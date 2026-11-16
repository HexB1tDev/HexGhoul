#!/usr/bin/env python3
"""4.1.51: pictures of a place's insides as the game would show them: every block in its own shape (slabs, stairs, carpets, trapdoors,
doors, panes and fences joined to their neighbours, lanterns, pots...), lit by the game's own light levels (each emitter's block light
spread a level a block, the sky's through openings and glass), seen from an eye inside a room or as an isometric cutaway of one floor.

    import interior_render as R
    scene = R.Scene(cells, size)              # cells: {(x, y, z): (name, {props})}, size (w, h, d); air need not be listed
    scene.iso("out.png", y0=8, y1=12, scale=12, side="se")      # one storey from above its south-east corner, everything from y1 up left off
    scene.eye("out.png", (20.5, 9.62, 18.5), yaw=180, pitch=10)    # a player's view (feet + 1.62), yaw as the game's (south 0, west 90)
    scene.dark(y0, y1)                         # the floor spots a monster could spawn on (block light 0, room to stand) between y0 and y1

Every block is cut into R x R x R small cells (R 8: a sixteenth of the game's shapes rounded to an eighth) and rays are walked through them
(numpy, every pixel at once). Light is the game's: block light from each emitter (glowstone 15, a lantern 15, a torch 14...), less one a
block through anything not a full solid block; sky light 15 straight down through open blocks, then less one a block sideways; the picture
uses the brighter of the two (day) or the block light alone (night: what keeps monsters away).
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import city_render as C  # noqa: E402

R = 8
SKY = np.array([150, 185, 225], dtype=np.float32)
EMIT = {"minecraft:glowstone": 15, "minecraft:sea_lantern": 15, "minecraft:lantern": 15, "minecraft:soul_lantern": 10,
        "minecraft:torch": 14, "minecraft:wall_torch": 14, "minecraft:end_rod": 14, "minecraft:shroomlight": 15,
        "minecraft:ochre_froglight": 15, "minecraft:verdant_froglight": 15, "minecraft:pearlescent_froglight": 15,
        "minecraft:jack_o_lantern": 15, "minecraft:beacon": 15, "minecraft:campfire": 15, "minecraft:redstone_lamp": 15,
        "minecraft:soul_torch": 10, "minecraft:soul_wall_torch": 10}
#: Seen through (a share of the colour behind kept per small cell crossed).
CLEAR = {"glass": 0.22, "glass_pane": 0.30, "iron_bars": 0.0}
DIRS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def box(x0, y0, z0, x1, y1, z1):
    return (x0, y0, z0, x1, y1, z1)


FULL = [box(0, 0, 0, 16, 16, 16)]


def short(name):
    return name.split(":", 1)[1]


def is_full_name(name):
    """True if the block is a whole cube by its name alone (what joins panes and fences, and stops light)."""
    n = short(name)
    if n in ("air", "cave_air", "void_air", "water", "lava", "barrier", "light"):
        return False
    partial = ("_slab", "_stairs", "_carpet", "_trapdoor", "_door", "_fence", "_fence_gate", "_wall", "_pane", "iron_bars", "ladder",
               "_sign", "lantern", "flower_pot", "potted_", "torch", "end_rod", "chain", "cauldron", "brewing_stand", "anvil",
               "lectern", "daylight_detector", "_bed", "candle", "_head", "_skull", "_banner", "_button", "_pressure_plate", "rail",
               "campfire", "stonecutter", "grindstone", "bell", "chest", "_plate", "scaffolding", "hopper", "composter", "snow",
               "grass", "fern", "flower", "dandelion", "poppy", "tulip", "_sapling", "vine", "lever", "redstone_wire", "repeater",
               "comparator", "cake", "bamboo", "azalea", "dripleaf", "pink_petals", "sea_pickle", "turtle_egg", "conduit", "skull",
               "enchanting_table", "end_portal_frame", "_pot", "item_frame", "tripwire", "cobweb", "lily_pad", "kelp", "seagrass")
    if n in ("glass",) or n.endswith("_glass"):
        return True
    return not any(p in n for p in partial)


def opaque(name):
    """Stops light (a whole solid cube, not glass, not leaves)."""
    n = short(name)
    return is_full_name(name) and not (n == "glass" or n.endswith("_glass") or n.endswith("_leaves") or n in ("glowstone", "sea_lantern"))


class Scene:
    def __init__(self, cells, size):
        self.cells = cells
        self.w, self.h, self.d = size
        self.states = ["minecraft:air"]
        self.props = [{}]
        self.index = {}
        grid = np.zeros((self.w, self.h, self.d), dtype=np.int32)
        for (x, y, z), (name, props) in cells.items():
            if not (0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d) or short(name) in ("air", "cave_air", "void_air"):
                continue
            key = (name, tuple(sorted(props.items())))
            if key not in self.index:
                self.index[key] = len(self.states)
                self.states.append(name)
                self.props.append(dict(props))
            grid[x, y, z] = self.index[key]
        self.grid = grid
        self._light()

    # ----- light -----
    def _light(self):
        pad = 3
        w, h, d = self.w + 2 * pad, self.h + pad, self.d + 2 * pad
        op = np.zeros((w, h, d), dtype=bool)
        emit = np.zeros((w, h, d), dtype=np.int16)
        is_opaque = np.array([opaque(n) if i else False for i, n in enumerate(self.states)])
        lights = np.array([EMIT.get(n, 0) for n in self.states], dtype=np.int16)
        op[pad:pad + self.w, :self.h, pad:pad + self.d] = is_opaque[self.grid]
        emit[pad:pad + self.w, :self.h, pad:pad + self.d] = lights[self.grid]
        block = emit.copy()
        for _ in range(15):
            block = np.maximum(block, self._spread(block, op))
        sky = np.zeros((w, h, d), dtype=np.int16)
        # Straight down from the top through open blocks.
        blocked = np.cumsum(op[:, ::-1, :], axis=1)[:, ::-1, :] > 0
        sky[~blocked] = 15
        sky[op] = 0
        for _ in range(15):
            sky = np.maximum(sky, self._spread(sky, op))
        self.block_light = block[pad:pad + self.w, :self.h, pad:pad + self.d]
        self.sky_light = sky[pad:pad + self.w, :self.h, pad:pad + self.d]

    @staticmethod
    def _spread(level, op):
        out = np.zeros_like(level)
        for axis in range(3):
            for s in (1, -1):
                shifted = np.roll(level, s, axis=axis)
                sl = [slice(None)] * 3
                sl[axis] = slice(0, 1) if s == 1 else slice(-1, None)
                shifted[tuple(sl)] = 0
                out = np.maximum(out, shifted - 1)
        out[op] = 0
        return np.maximum(out, 0)

    def dark(self, y0, y1):
        """Floor spots between y0 and y1 (the feet's layer) a monster could spawn on: a full block under, two open blocks, block light 0."""
        out = []
        for x in range(self.w):
            for z in range(self.d):
                for y in range(max(1, y0), min(self.h - 1, y1)):
                    under, here, over = self.grid[x, y - 1, z], self.grid[x, y, z], self.grid[x, y + 1, z]
                    if under and is_full_name(self.states[under]) and opaque(self.states[under]) and self._open(here) and self._open(over) \
                            and self.block_light[x, y, z] == 0:
                        out.append((x, y, z))
        return out

    def _open(self, i):
        if i == 0:
            return True
        n = short(self.states[i])
        return n.endswith("_carpet") or n.endswith("_pressure_plate") or n.endswith("_button") or n in ("rail",)

    # ----- shapes -----
    def _name_at(self, x, y, z):
        if 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d:
            return self.states[self.grid[x, y, z]]
        return "minecraft:air"

    def _joins(self, kind, other):
        n = short(other)
        if kind == "pane":
            return n.endswith("_pane") or n == "iron_bars" or n.endswith("glass") or (is_full_name(other) and n != "air")
        if kind == "fence":
            return n.endswith("_fence") or n.endswith("_fence_gate") or (is_full_name(other) and not n.endswith("glass"))
        if kind == "wall":
            return n.endswith("_wall") or n.endswith("_fence_gate") or n.endswith("_pane") or n == "iron_bars" or is_full_name(other)
        return False

    def boxes(self, i, x, y, z):
        name, props = self.states[i], self.props[i]
        n = short(name)
        f = props.get("facing", "north")
        half = props.get("half", props.get("type", "bottom"))
        if n.endswith("_slab"):
            return FULL if half == "double" else [box(0, 8, 0, 16, 16, 16)] if half == "top" else [box(0, 0, 0, 16, 8, 16)]
        if n.endswith("_stairs"):
            base = box(0, 8, 0, 16, 16, 16) if half == "top" else box(0, 0, 0, 16, 8, 16)
            ylo, yhi = (0, 8) if half == "top" else (8, 16)
            step = {"north": box(0, ylo, 0, 16, yhi, 8), "south": box(0, ylo, 8, 16, yhi, 16), "east": box(8, ylo, 0, 16, yhi, 16),
                    "west": box(0, ylo, 0, 8, yhi, 16)}[f]
            return [base, step]
        if n.endswith("_carpet") or n == "moss_carpet":
            return [box(0, 0, 0, 16, 1, 16)]
        if n.endswith("_pressure_plate"):
            return [box(1, 0, 1, 15, 1, 15)]
        if n.endswith("_trapdoor"):
            if props.get("open") == "true":
                return [{"north": box(0, 0, 13, 16, 16, 16), "south": box(0, 0, 0, 16, 16, 3), "west": box(13, 0, 0, 16, 16, 16),
                         "east": box(0, 0, 0, 3, 16, 16)}[f]]
            return [box(0, 13, 0, 16, 16, 16)] if half == "top" else [box(0, 0, 0, 16, 3, 16)]
        if n.endswith("_door"):
            closed = props.get("open") != "true"
            right = props.get("hinge") == "right"
            S, N, W, E = box(0, 0, 0, 16, 16, 3), box(0, 0, 13, 16, 16, 16), box(13, 0, 0, 16, 16, 16), box(0, 0, 0, 3, 16, 16)
            return [{"east": E if closed else (N if right else S), "south": S if closed else (E if right else W),
                     "west": W if closed else (S if right else N), "north": N if closed else (W if right else E)}[f]]
        if n == "ladder":
            return [{"north": box(0, 0, 13, 16, 16, 16), "south": box(0, 0, 0, 16, 16, 3), "west": box(13, 0, 0, 16, 16, 16),
                     "east": box(0, 0, 0, 3, 16, 16)}[f]]
        if n.endswith("_wall_banner"):
            return [{"north": box(0, 0, 14, 16, 16, 16), "south": box(0, 0, 0, 16, 16, 2), "west": box(14, 0, 0, 16, 16, 16),
                     "east": box(0, 0, 0, 2, 16, 16)}[f]]
        if n.endswith("_wall_sign"):
            return [{"north": box(0, 4, 14, 16, 12, 16), "south": box(0, 4, 0, 16, 12, 2), "west": box(14, 4, 0, 16, 12, 16),
                     "east": box(0, 4, 0, 2, 12, 16)}[f]]
        if n.endswith("_sign"):
            return [box(7, 0, 7, 9, 9, 9), box(0, 9, 7, 16, 16, 9)]
        if n.endswith("_pane") or n == "iron_bars":
            out = [box(7, 0, 7, 9, 16, 9)]
            for d, b in (("north", box(7, 0, 0, 9, 16, 7)), ("south", box(7, 0, 9, 9, 16, 16)), ("west", box(0, 0, 7, 7, 16, 9)),
                         ("east", box(9, 0, 7, 16, 16, 9))):
                dx, _, dz = DIRS[d]
                if self._joins("pane", self._name_at(x + dx, y, z + dz)):
                    out.append(b)
            return out
        if n.endswith("_fence"):
            out = [box(6, 0, 6, 10, 16, 10)]
            for d, b in (("north", box(7, 6, 0, 9, 15, 6)), ("south", box(7, 6, 10, 9, 15, 16)), ("west", box(0, 6, 7, 6, 15, 9)),
                         ("east", box(10, 6, 7, 16, 15, 9))):
                dx, _, dz = DIRS[d]
                if self._joins("fence", self._name_at(x + dx, y, z + dz)):
                    out.append(b)
            return out
        if n.endswith("_fence_gate"):
            if props.get("open") == "true":
                return [box(0, 5, 7, 2, 16, 9), box(14, 5, 7, 16, 16, 9)] if f in ("north", "south") else [box(7, 5, 0, 9, 16, 2), box(7, 5, 14, 9, 16, 16)]
            return [box(0, 5, 7, 16, 16, 9)] if f in ("north", "south") else [box(7, 5, 0, 9, 16, 16)]
        if n.endswith("_wall"):
            out = [box(4, 0, 4, 12, 16, 12)]
            for d, b in (("north", box(5, 0, 0, 11, 14, 4)), ("south", box(5, 0, 12, 11, 14, 16)), ("west", box(0, 0, 5, 4, 14, 11)),
                         ("east", box(12, 0, 5, 16, 14, 11))):
                dx, _, dz = DIRS[d]
                if self._joins("wall", self._name_at(x + dx, y, z + dz)):
                    out.append(b)
            return out
        if n in ("lantern", "soul_lantern"):
            return [box(5, 1, 5, 11, 8, 11), box(6, 8, 6, 10, 10, 10), box(7, 10, 7, 9, 16, 9)] if props.get("hanging") == "true" \
                else [box(5, 0, 5, 11, 7, 11), box(6, 7, 6, 10, 9, 10)]
        if n == "flower_pot" or n.startswith("potted_"):
            out = [box(5, 0, 5, 11, 6, 11)]
            if n.startswith("potted_"):
                tall = 16 if any(k in n for k in ("bamboo", "azalea", "cactus", "fern", "sapling")) else 12
                out.append(box(6, 6, 6, 10, tall, 10))
                if "azalea" in n:
                    out.append(box(3, 9, 3, 13, 15, 13))
            return out
        if n in ("torch", "soul_torch"):
            return [box(7, 0, 7, 9, 10, 9)]
        if n.endswith("wall_torch"):
            return [{"north": box(7, 3, 11, 9, 13, 16), "south": box(7, 3, 0, 9, 13, 5), "west": box(11, 3, 7, 16, 13, 9),
                     "east": box(0, 3, 7, 5, 13, 9)}[f]]
        if n == "end_rod":
            return [box(6, 0, 6, 10, 16, 10)] if f in ("up", "down") else [box(0, 6, 6, 16, 10, 10)] if f in ("east", "west") \
                else [box(6, 6, 0, 10, 10, 16)]
        if n == "chain":
            return [box(6, 0, 6, 10, 16, 10)]
        if n.endswith("cauldron"):
            return [box(0, 3, 0, 16, 16, 2), box(0, 3, 14, 16, 16, 16), box(0, 3, 0, 2, 16, 16), box(14, 3, 0, 16, 16, 16),
                    box(0, 3, 0, 16, 5, 16), box(0, 0, 0, 4, 3, 4), box(12, 0, 0, 16, 3, 4), box(0, 0, 12, 4, 3, 16), box(12, 0, 12, 16, 3, 16)] \
                + ([box(2, 5, 2, 14, 13, 14)] if n == "water_cauldron" else [])
        if n == "brewing_stand":
            return [box(1, 0, 1, 15, 2, 15), box(7, 0, 7, 9, 14, 9)]
        if n.endswith("anvil"):
            return [box(2, 0, 2, 14, 4, 14), box(4, 4, 3, 12, 5, 13), box(6, 5, 4, 10, 10, 12), box(3, 10, 0, 13, 16, 16)] if f in ("east", "west") \
                else [box(2, 0, 2, 14, 4, 14), box(3, 4, 4, 13, 5, 12), box(4, 5, 6, 12, 10, 10), box(0, 10, 3, 16, 16, 13)]
        if n == "lectern":
            return [box(0, 0, 0, 16, 2, 16), box(4, 2, 4, 12, 13, 12), box(0, 13, 0, 16, 15, 16)]
        if n.startswith("daylight_detector"):
            return [box(0, 0, 0, 16, 6, 16)]
        if n.endswith("_bed"):
            return [box(0, 3, 0, 16, 9, 16)]
        if n in ("chest", "trapped_chest", "ender_chest"):
            return [box(1, 0, 1, 15, 14, 15)]
        if n.endswith("_head") or n.endswith("_skull"):
            return [box(4, 0, 4, 12, 8, 12)]
        if n.endswith("_button"):
            return [box(5, 6, 5, 11, 10, 11)]
        if n in ("rail", "powered_rail", "detector_rail", "activator_rail"):
            return [box(0, 0, 0, 16, 1, 16)]
        if n in ("campfire", "soul_campfire"):
            return [box(0, 0, 0, 16, 7, 16)]
        if n == "stonecutter":
            return [box(0, 0, 0, 16, 9, 16)]
        if n == "grindstone":
            return [box(2, 4, 4, 14, 16, 12)]
        if "candle" in n:
            return [box(7, 0, 7, 9, 6, 9)]
        if n in ("grass", "fern", "dandelion", "poppy", "tall_grass", "large_fern") or "tulip" in n or "flower" in n and "pot" not in n:
            return [box(4, 0, 4, 12, 12, 12)]
        if n == "decorated_pot":
            return [box(1, 0, 1, 15, 16, 15)]
        if n == "hopper":
            return [box(0, 10, 0, 16, 16, 16), box(4, 4, 4, 12, 10, 12), box(6, 0, 6, 10, 4, 10)]
        if n == "composter":
            return FULL
        if n == "scaffolding":
            return [box(0, 14, 0, 16, 16, 16), box(0, 0, 0, 2, 16, 2), box(14, 0, 0, 16, 16, 2), box(0, 0, 14, 2, 16, 16), box(14, 0, 14, 16, 16, 16)]
        if n == "snow":
            return [box(0, 0, 0, 16, 2 * int(props.get("layers", "1")), 16)]
        return FULL

    # ----- colour -----
    def colour_table(self):
        """Per state, an R x R x R x 3 colour (its look in small cells: books in a shelf, seams in planks, specks in glowstone)."""
        rng = np.random.default_rng(7)
        tab = np.zeros((len(self.states), R, R, R, 3), dtype=np.float32)
        books = np.array([(120, 40, 35), (40, 70, 120), (50, 95, 50), (140, 110, 60), (90, 40, 90), (160, 150, 130), (60, 50, 40)], np.float32)
        ys = np.arange(R)[None, :, None]
        for i, name in enumerate(self.states):
            if i == 0:
                continue
            n = short(name)
            base = np.array(colour(name), dtype=np.float32)
            noise = 1.0 + rng.uniform(-0.05, 0.05, (R, R, R, 1)).astype(np.float32)
            c = np.broadcast_to(base, (R, R, R, 3)) * noise
            if n == "bookshelf" or n == "chiseled_bookshelf":
                wood = np.array(C.WOODS["oak"], np.float32)
                pick = books[rng.integers(0, len(books), (R, 2, R))]
                c = np.empty((R, R, R, 3), np.float32)
                c[:] = wood
                for row, (lo, hi) in enumerate(((1, 4), (5, 8))):
                    for yy in range(lo, min(hi, R)):
                        c[:, yy, :] = pick[:, row, :][:, None, :].repeat(1, axis=1)[:, 0, :][:, None, :] if False else pick[:, row, :]
                c[:, 0, :] = wood * 0.8
                c[:, 4, :] = wood * 0.8
                c[0, :, :] = c[R - 1, :, :] = wood * 0.85
                c[:, :, 0] = c[:, :, R - 1] = wood * 0.85
            elif n.endswith("_planks") or n.endswith("_slab") and any(n.startswith(wd) for wd in C.WOODS) \
                    or n.endswith("_stairs") and any(n.startswith(wd) for wd in C.WOODS):
                seam = np.where(ys % 4 == 0, 0.88, 1.0).astype(np.float32)[..., None]
                c = c * seam
            elif n == "glowstone" or n == "sea_lantern" or "froglight" in n or n == "shroomlight":
                c = np.broadcast_to(base, (R, R, R, 3)) * (1.0 + rng.uniform(-0.12, 0.12, (R, R, R, 1)).astype(np.float32))
            elif n.endswith("_leaves"):
                c = np.broadcast_to(base, (R, R, R, 3)) * (1.0 + rng.uniform(-0.25, 0.2, (R, R, R, 1)).astype(np.float32))
            elif n.startswith("potted_"):
                pot = np.array((150, 80, 55), np.float32)
                plant = np.array((70, 130, 50) if "bamboo" not in n else (95, 140, 40), np.float32)
                c = np.empty((R, R, R, 3), np.float32)
                c[:] = plant
                c[:, :3, :] = pot
            elif n in ("lantern", "soul_lantern"):
                c = np.empty((R, R, R, 3), np.float32)
                c[:] = (60, 60, 66)
                c[:, 1:3, :] = (255, 210, 120)
                if self.props[i].get("hanging") == "true":
                    c[:, :, :] = (60, 60, 66)
                    c[:, 1:4, :] = (255, 210, 120)
            tab[i] = c
        return np.clip(tab, 0, 255)

    # ----- the small cells -----
    def voxels(self, y0=0, y1=None, x0=0, x1=None, z0=0, z1=None):
        y1 = self.h if y1 is None else y1
        x1 = self.w if x1 is None else x1
        z1 = self.d if z1 is None else z1
        sub = np.zeros(((x1 - x0) * R, (y1 - y0) * R, (z1 - z0) * R), dtype=np.int32)
        cache = {}
        xs, ys, zs = np.nonzero(self.grid[x0:x1, y0:y1, z0:z1])
        for x, y, z in zip(xs, ys, zs):
            gx, gy, gz = x + x0, y + y0, z + z0
            i = self.grid[gx, gy, gz]
            bs = self.boxes(i, gx, gy, gz)
            key = (i, tuple(bs))
            m = cache.get(key)
            if m is None:
                m = np.zeros((R, R, R), dtype=bool)
                for (a, b, c, d, e, f) in bs:
                    lo = [int(math.floor(v * R / 16 + 1e-6)) for v in (a, b, c)]
                    hi = [max(l + 1, int(math.ceil(v * R / 16 - 1e-6))) for v, l in zip((d, e, f), lo)]
                    m[lo[0]:hi[0], lo[1]:hi[1], lo[2]:hi[2]] = True
                cache[key] = m
            view = sub[x * R:(x + 1) * R, y * R:(y + 1) * R, z * R:(z + 1) * R]
            view[m] = i
        return sub, (x0, y0, z0)

    def march(self, sub, offset, origins, dirs, night=False, limit=None):
        """Colours of the rays (block units, the scene's frame): through the small cells to the first solid one, through glass."""
        tab = self.colour_table()
        clear = np.zeros(len(self.states), np.float32)
        for i, name in enumerate(self.states):
            n = short(name)
            if n == "glass" or n.endswith("_glass"):
                clear[i] = 0.7
            elif n.endswith("_pane"):
                clear[i] = 0.55
        bright_self = np.array([EMIT.get(n, 0) for n in self.states], np.float32)
        is_solid = np.array([bool(i) and opaque(n) for i, n in enumerate(self.states)])
        shape = np.array(sub.shape)
        o = (origins - np.array(offset, np.float64)) * R
        dvec = dirs.astype(np.float64)
        n = len(o)
        # Clip each ray to the volume.
        inv = np.where(np.abs(dvec) < 1e-12, 1e12, 1.0 / np.where(np.abs(dvec) < 1e-12, 1e-12, dvec))
        t0 = (0 - o) * inv
        t1 = (shape - o) * inv
        tmin = np.max(np.minimum(t0, t1), axis=1)
        tmax = np.min(np.maximum(t0, t1), axis=1)
        tstart = np.maximum(tmin, 0) + 1e-6
        hit_any = tmax > tstart
        p = o + dvec * tstart[:, None]
        cell = np.clip(np.floor(p).astype(np.int64), 0, shape - 1)
        step = np.where(dvec > 0, 1, np.where(dvec < 0, -1, 0))
        tdelta = np.abs(inv)
        nxt = np.where(step > 0, cell + 1, cell).astype(np.float64)
        tm = np.where(step != 0, (nxt - o) * inv, 1e30)
        colour_out = np.tile(SKY, (n, 1)).astype(np.float32)
        trans = np.ones(n, np.float32)
        acc = np.zeros((n, 3), np.float32)
        last_axis = np.zeros(n, np.int64)
        active = np.nonzero(hit_any)[0]
        cell = cell[active]
        tm = tm[active]
        step_a = step[active]
        tdelta_a = tdelta[active]
        last_axis = np.argmin(tm, axis=1)
        prev = cell.copy()
        first = True
        maxsteps = int(limit or (shape.sum() * 2))
        light_day = np.maximum(self.block_light, 0 if night else self.sky_light).astype(np.float32)
        for _ in range(maxsteps):
            if len(active) == 0:
                break
            if not first:
                axis = np.argmin(tm, axis=1)
                prev = cell.copy()
                rows = np.arange(len(active))
                cell[rows, axis] += step_a[rows, axis]
                tm[rows, axis] += tdelta_a[rows, axis]
                last_axis = axis
            first = False
            inside = np.all((cell >= 0) & (cell < shape), axis=1)
            # Out of the volume: the sky (or what was gathered through glass).
            out = ~inside
            if out.any():
                idx = active[out]
                colour_out[idx] = acc[idx] + trans[idx, None] * SKY
            keep = inside
            active, cell, tm, step_a, tdelta_a, prev, last_axis = (active[keep], cell[keep], tm[keep], step_a[keep], tdelta_a[keep],
                                                                     prev[keep], last_axis[keep])
            if len(active) == 0:
                break
            v = sub[cell[:, 0], cell[:, 1], cell[:, 2]]
            solid = v != 0
            if not solid.any():
                continue
            sidx = np.nonzero(solid)[0]
            vs = v[sidx]
            c = cell[sidx]
            col = tab[vs, c[:, 0] % R, c[:, 1] % R, c[:, 2] % R]
            ax = last_axis[sidx]
            stp = step_a[sidx, :][np.arange(len(sidx)), ax]
            # The game's face shades: top 1, bottom 0.5, north/south 0.8, east/west 0.6.
            face = np.where(ax == 1, np.where(stp < 0, 1.0, 0.5), np.where(ax == 2, 0.8, 0.6)).astype(np.float32)
            # Light of the block the ray came from (the face's own open side).
            pb = prev[sidx] // R + np.array(offset)
            outside = np.any((pb < 0) | (pb >= np.array([self.w, self.h, self.d])), axis=1)
            pb = np.clip(pb, 0, np.array([self.w - 1, self.h - 1, self.d - 1]))
            lvl = light_day[pb[:, 0], pb[:, 1], pb[:, 2]]
            # Past the scene, or a solid block the cut left out (a cutaway's wall tops): lit as open sky.
            lvl = np.where(outside | is_solid[self.grid[pb[:, 0], pb[:, 1], pb[:, 2]]], 15.0, lvl)
            lvl = np.maximum(lvl, bright_self[vs])
            b = lvl / 15.0
            curve = b / (4.0 - 3.0 * b)
            curve = curve + (1 - (1 - curve) ** 4 - curve) * 0.5
            shade = face * (0.06 + 0.94 * curve)
            # Block edges drawn a touch darker, to read the blocks.
            inblock = c % R
            edge = np.zeros(len(sidx), bool)
            for a in range(3):
                on_face = ax == a
                lo_e = (inblock[:, a] == 0) | (inblock[:, a] == R - 1)
                edge |= (~on_face) & lo_e & ((c[:, a] % R == 0) | (c[:, a] % R == R - 1))
            # Only the outer cells of the block's own box count as its edge: approximate by the block's grid lines.
            shade = shade * np.where(edge & (self._edge_mask(vs)), 0.86, 1.0)
            al = clear[vs]
            idx = active[sidx]
            contrib = col * shade[:, None]
            glassy = al > 0
            # Glass: tint and go on; anything else ends the ray.
            acc[idx] += (trans[idx] * (1 - al))[:, None] * contrib
            trans[idx] *= np.where(glassy, al, 0.0)
            done = ~glassy
            colour_out[idx[done]] = acc[idx[done]]
            keep = np.ones(len(active), bool)
            keep[sidx[done]] = False
            active, cell, tm, step_a, tdelta_a, prev, last_axis = (active[keep], cell[keep], tm[keep], step_a[keep], tdelta_a[keep],
                                                                     prev[keep], last_axis[keep])
        return colour_out

    def _edge_mask(self, vs):
        full = np.array([is_full_name(n) for n in self.states])
        return full[vs]

    # ----- cameras -----
    def iso(self, out, y0=0, y1=None, scale=10, side="se", x0=0, x1=None, z0=0, z1=None, night=False, pitch=35.264, marks=()):
        sub, off = self.voxels(y0, y1, x0, x1, z0, z1)
        sx, sy, sz = (np.array(sub.shape) / R)
        az = {"se": 45.0, "sw": 135.0, "nw": 225.0, "ne": 315.0}[side]
        el = math.radians(pitch)
        a = math.radians(az)
        # Looking from the corner toward the middle, down.
        fwd = np.array([-math.cos(el) * math.sin(a), -math.sin(el), -math.cos(el) * math.cos(a)])
        right = np.cross(fwd, [0, 1, 0])
        right /= np.linalg.norm(right)
        up = np.cross(right, fwd)
        centre = np.array(off, np.float64) + np.array([sx, sy, sz]) / 2
        corners = np.array([[i, j, k] for i in (0, sx) for j in (0, sy) for k in (0, sz)]) + np.array(off)
        px = (corners - centre) @ right
        py = (corners - centre) @ up
        wpx = int((px.max() - px.min()) * scale) + 20
        hpx = int((py.max() - py.min()) * scale) + 20
        u = (np.arange(wpx) - wpx / 2) / scale
        v = (hpx / 2 - np.arange(hpx)) / scale
        U, V = np.meshgrid(u, v)
        origins = centre + U.ravel()[:, None] * right + V.ravel()[:, None] * up - fwd * (sx + sy + sz) * 2
        dirs = np.tile(fwd, (len(origins), 1))
        cols = self.march(sub, off, origins, dirs, night)
        img = Image.fromarray(np.clip(cols, 0, 255).astype(np.uint8).reshape(hpx, wpx, 3))
        if marks:
            from PIL import ImageDraw
            dr = ImageDraw.Draw(img)
            for (mx, my, mz), colr in marks:
                p = np.array([mx, my, mz], np.float64) - centre
                qx = wpx / 2 + (p @ right) * scale
                qy = hpx / 2 - (p @ up) * scale
                r = max(3, scale // 3)
                dr.ellipse((qx - r, qy - r, qx + r, qy + r), fill=colr, outline=(0, 0, 0))
        img.save(out)
        return img

    def eye(self, out, at, yaw=0.0, pitch=0.0, fov=70.0, size=(960, 540), night=False, reach=24):
        x, y, z = at
        x0, x1 = max(0, int(x - reach)), min(self.w, int(x + reach) + 1)
        z0, z1 = max(0, int(z - reach)), min(self.d, int(z + reach) + 1)
        y0, y1 = max(0, int(y - 8)), min(self.h, int(y + 8))
        sub, off = self.voxels(y0, y1, x0, x1, z0, z1)
        wpx, hpx = size
        yr, pr = math.radians(yaw), math.radians(pitch)
        fwd = np.array([-math.sin(yr) * math.cos(pr), -math.sin(pr), math.cos(yr) * math.cos(pr)])
        right = np.cross(fwd, [0, 1, 0])
        right /= np.linalg.norm(right)
        up = np.cross(right, fwd)
        f = 1.0 / math.tan(math.radians(fov) / 2)
        u = (np.arange(wpx) - wpx / 2 + 0.5) / (hpx / 2)
        v = (hpx / 2 - np.arange(hpx) - 0.5) / (hpx / 2)
        U, V = np.meshgrid(u, v)
        dirs = fwd * f + U.ravel()[:, None] * right + V.ravel()[:, None] * up
        dirs /= np.linalg.norm(dirs, axis=1)[:, None]
        origins = np.tile(np.array(at, np.float64), (len(dirs), 1))
        cols = self.march(sub, off, origins, dirs, night)
        img = Image.fromarray(np.clip(cols, 0, 255).astype(np.uint8).reshape(hpx, wpx, 3))
        img.save(out)
        return img


#: Colours the city's table lacks or gets wrong close up.
EXTRA = {"glowstone": (238, 200, 120), "sea_lantern": (210, 230, 225), "smooth_quartz": (236, 232, 226), "quartz": (236, 230, 223),
         "iron_trapdoor": (200, 200, 200), "iron_block": (220, 220, 220), "iron_door": (190, 190, 190), "white_concrete": (207, 213, 214),
         "light_gray_concrete": (125, 125, 115), "gray_concrete": (55, 58, 62), "black_concrete": (8, 10, 15), "ladder": (125, 95, 55),
         "brewing_stand": (120, 110, 90), "smithing_table": (60, 45, 45), "cartography_table": (110, 85, 60), "loom": (150, 120, 90),
         "crafting_table": (130, 95, 60), "flower_pot": (150, 80, 55), "lectern": (160, 130, 80), "cauldron": (70, 70, 74),
         "water_cauldron": (60, 90, 190), "polished_andesite": (132, 135, 134), "andesite_wall": (136, 136, 136), "tinted_glass": (50, 40, 55),
         "chain": (55, 60, 70), "barrel": (120, 88, 52), "black_wool": (20, 21, 26), "gray_wool": (62, 68, 71), "white_wool": (233, 236, 236),
         "red_wool": (161, 39, 34), "target": (225, 200, 190), "hay_block": (166, 136, 38), "jukebox": (95, 65, 50), "note_block": (88, 58, 40),
         "polished_blackstone": (53, 48, 56), "polished_deepslate": (72, 72, 73), "deepslate_tiles": (54, 54, 55), "smooth_stone": (158, 158, 158),
         "stripped_dark_oak_log": (72, 56, 36), "dark_oak_log": (60, 46, 26), "spruce_log": (58, 37, 16), "daylight_detector": (40, 45, 60)}


def colour(name):
    n = short(name)
    for k in sorted(EXTRA, key=len, reverse=True):
        if n == k or n.startswith(k + "_") and k in ("smooth_quartz", "quartz", "polished_andesite", "polished_blackstone", "polished_deepslate",
                                                      "deepslate_tile"):
            return EXTRA[k]
    if n in EXTRA:
        return EXTRA[n]
    if n.startswith("daylight_detector"):
        return EXTRA["daylight_detector"]
    return C.colour(name)
