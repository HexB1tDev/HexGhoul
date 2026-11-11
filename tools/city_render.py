#!/usr/bin/env python3
"""4.1.35: an isometric picture of the Small City as the game gets it: its four quarters' 1.20.1 tiles
(data/hexkagune/structures/small_city_<k>/), put back side by side, every block a small coloured cube seen from the south-east, lit from
above (tops brightest, the south faces darker, the east faces darkest), painter's order through a depth buffer.

    python3 tools/city_render.py out.png [--scale N] [--box x0 z0 x1 z1] [--turn 0..3] [--people]

--box renders only that part of the city (x, z from its north-west corner, 0 .. 544); --scale N makes each block N times bigger; --turn
turns the city a quarter at a time (as the game turns a structure) before it is drawn; --people marks where its people stand.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import library_structure as L  # noqa: E402
from exploration_structures import splits  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src" / "main" / "resources" / "data" / "hexkagune" / "structures"
QUARTER = 272
SIZE = 2 * QUARTER

DYES = {"white": (233, 236, 236), "orange": (240, 118, 19), "magenta": (189, 68, 179), "light_blue": (58, 175, 217), "yellow": (248, 197, 39),
        "lime": (112, 185, 25), "pink": (237, 141, 172), "gray": (62, 68, 71), "light_gray": (142, 142, 134), "cyan": (21, 137, 145),
        "purple": (121, 42, 172), "blue": (53, 57, 157), "brown": (114, 71, 40), "green": (84, 109, 27), "red": (161, 39, 34),
        "black": (20, 21, 25)}
TERRACOTTA = {"white": (209, 178, 161), "orange": (161, 83, 37), "pink": (161, 78, 78), "light_blue": (113, 108, 137), "yellow": (186, 133, 35),
              "lime": (103, 117, 52), "cyan": (86, 91, 91), "green": (76, 83, 42), "light_gray": (135, 106, 97), "gray": (57, 42, 35),
              "brown": (77, 51, 35), "red": (143, 61, 46), "black": (37, 22, 16), "blue": (74, 59, 91), "purple": (118, 70, 86),
              "magenta": (149, 88, 108)}
WOODS = {"oak": (162, 130, 78), "spruce": (114, 84, 48), "birch": (192, 175, 121), "dark_oak": (66, 43, 20), "jungle": (160, 115, 80),
         "acacia": (168, 90, 50), "cherry": (226, 178, 172), "mangrove": (117, 54, 48), "crimson": (101, 48, 70), "warped": (43, 104, 99),
         "petrified_oak": (162, 130, 78)}
NAMED = {"stone": (125, 125, 125), "smooth_stone": (158, 158, 158), "stone_brick": (122, 121, 122), "mossy_stone_brick": (115, 121, 105),
         "cobblestone": (127, 127, 127), "andesite": (136, 136, 136), "diorite": (188, 188, 188), "granite": (149, 103, 85),
         "quartz": (236, 230, 223), "sandstone": (216, 203, 155), "brick": (150, 97, 83), "end_stone_brick": (218, 224, 162),
         "deepslate": (80, 80, 82), "blackstone": (42, 36, 41), "prismarine": (51, 91, 75), "dirt": (134, 96, 67), "coarse_dirt": (119, 85, 59),
         "podzol": (91, 63, 24), "grass_block": (95, 159, 53), "dirt_path": (148, 122, 65), "gravel": (131, 127, 126), "sand": (219, 207, 163),
         "water": (52, 90, 200), "ice": (145, 183, 253), "snow": (249, 254, 254), "sea_lantern": (172, 199, 190), "glowstone": (171, 131, 84),
         "end_rod": (220, 210, 200), "iron": (210, 210, 210), "lantern": (90, 80, 70), "chain": (60, 64, 74), "bookshelf": (117, 94, 59),
         "hay": (166, 136, 38), "anvil": (68, 68, 68), "barrel": (134, 100, 58), "glass": (200, 220, 225), "soul_sand": (81, 62, 50),
         "beacon": (117, 220, 215), "daylight_detector": (130, 116, 94), "cauldron": (74, 74, 78), "note_block": (88, 58, 40),
         "coal_ore": (105, 105, 105), "leaves": (60, 120, 40), "spruce_leaves": (55, 90, 55), "birch_leaves": (110, 150, 70),
         "cherry_leaves": (229, 172, 194), "campfire": (90, 70, 50), "smoker": (90, 85, 80), "furnace": (110, 110, 110), "rail": (125, 110, 90)}
FLOWERS = {"dandelion": (250, 220, 40), "poppy": (220, 30, 30), "rose_bush": (200, 30, 40), "peony": (230, 170, 220), "oxeye_daisy": (240, 240, 230),
           "cornflower": (70, 110, 230), "azure_bluet": (220, 230, 240), "red_tulip": (220, 40, 40), "sunflower": (250, 210, 40),
           "pink_petals": (240, 170, 200)}
#: Drawn as a thin layer on the block under them (flat or small things).
THIN = ("_carpet", "_button", "_pressure_plate", "_wall_banner", "_banner", "_wall_sign", "_sign", "minecraft:grass", "minecraft:fern",
        "tall_grass", "large_fern", "_torch", "minecraft:snow", "rail", "minecraft:lever", "redstone_wire", "dandelion", "poppy", "_tulip",
        "pink_petals", "oxeye_daisy", "cornflower", "azure_bluet", "rose_bush", "peony", "sunflower", "potted_", "flower_pot", "kelp",
        "dripleaf", "azalea", "bubble_column", "_trapdoor", "daylight_detector", "_bed", "minecraft:chain", "lantern", "end_rod")


def colour(name):
    n = name.split(":", 1)[1]
    for f, c in FLOWERS.items():
        if f in n:
            return c
    if n in ("grass", "tall_grass", "fern", "large_fern", "kelp_plant", "big_dripleaf", "flowering_azalea"):
        return (90, 150, 50)
    for dye in sorted(DYES, key=len, reverse=True):
        if n.startswith(dye + "_"):
            rest = n[len(dye) + 1:]
            if "terracotta" in rest:
                return TERRACOTTA.get(dye, DYES[dye])
            base = DYES[dye]
            if "glass" in rest:
                return tuple(int(c * 0.6 + 255 * 0.4) for c in base)
            return base
    if n == "terracotta":
        return (152, 94, 67)
    for key in ("spruce_leaves", "birch_leaves", "cherry_leaves"):
        if key in n:
            return NAMED[key]
    if n.endswith("_leaves"):
        return NAMED["leaves"]
    for wood in sorted(WOODS, key=len, reverse=True):
        if n.startswith(wood + "_") or n.startswith("stripped_" + wood + "_"):
            c = WOODS[wood]
            return tuple(int(v * 0.8) for v in c) if n.endswith("_log") and not n.startswith("stripped") else c
    n = n.replace("infested_", "").replace("polished_", "").replace("chiseled_", "").replace("cut_", "").replace("smooth_quartz", "quartz")
    for key in sorted(NAMED, key=len, reverse=True):
        if key in n:
            return NAMED[key]
    return (170, 160, 150)


def thin(name):
    return any(t in name for t in THIN)


def _v(tag):
    """A tag's value; a list's items (a list tag holds its kind and its items)."""
    v = tag.value if isinstance(tag, L.Tag) else tag
    return v[1] if isinstance(v, tuple) and len(v) == 2 and isinstance(v[1], list) else v


def load_city(turn=0):
    """The city as a (x, y, z) array of indices into names (0: air), x and z from its north-west corner; and its people."""
    names = ["minecraft:air"]
    index = {"minecraft:air": 0}
    height = None
    grid = None
    people = []
    for k in range(4):
        folder = DATA / f"small_city_{k}"
        qx, qz = (k % 2) * QUARTER, (k // 2) * QUARTER
        xs, zs = splits(QUARTER), splits(QUARTER)
        for i in range(len(xs) - 1):
            for j in range(len(zs) - 1):
                root = L.read(folder / f"t_{i}_{j}.nbt")
                root = root.value if isinstance(root, L.Tag) else root
                size = [_v(v) for v in _v(root["size"])]
                if grid is None:
                    height = size[1]
                    grid = np.zeros((SIZE, height, SIZE), dtype=np.uint16)
                pal = []
                for p in _v(root["palette"]):
                    n = _v(p["Name"])
                    if n not in index:
                        index[n] = len(names)
                        names.append(n)
                    pal.append(index[n])
                for b in _v(root["blocks"]):
                    x, y, z = (_v(v) for v in _v(b["pos"]))
                    grid[qx + xs[i] + x, y, qz + zs[j] + z] = pal[_v(b["state"])]
                for e in _v(root["entities"]):
                    x, y, z = (_v(v) for v in _v(e["blockPos"]))
                    people.append((_v(_v(e["nbt"])["id"]), qx + xs[i] + x, y, qz + zs[j] + z))
    for _ in range(turn % 4):
        # A quarter turn clockwise seen from above (north up): x' = SIZE - 1 - z, z' = x.
        grid = np.ascontiguousarray(np.rot90(grid, k=-1, axes=(0, 2)))
        people = [(kind, SIZE - 1 - z, y, x) for kind, x, y, z in people]
    return grid, names, people


# A block's picture (its anchor: the top corner of its top face): (dx, dy, shade) per pixel at scale 1.
TOP = [(-1, 0), (0, 0), (-2, 1), (-1, 1), (0, 1), (1, 1)]
SOUTH = [(-2, 2), (-1, 2), (-2, 3), (-1, 3)]
EAST = [(0, 2), (1, 2), (0, 3), (1, 3)]


def render(grid, names, out, scale=1, box=None, people=(), sky=(132, 166, 204)):
    if box:
        x0, z0, x1, z1 = box
        grid = grid[x0:x1, :, z0:z1]
        people = [(k, x - x0, y, z - z0) for k, x, y, z in people if x0 <= x < x1 and z0 <= z < z1]
    w, h, d = grid.shape
    cols = np.array([colour(n) for n in names], dtype=np.float32)
    is_thin = np.array([thin(n) for n in names])
    is_thin[0] = False
    solid = (grid != 0) & ~is_thin[grid]
    pad = np.zeros((w + 1, h + 1, d + 1), dtype=bool)
    pad[:w, :h, :d] = solid
    open_top = ~pad[:w, 1:h + 1, :d]
    open_east = ~pad[1:w + 1, :h, :d]
    open_south = ~pad[:w, :h, 1:d + 1]
    draw_solid = solid & (open_top | open_east | open_south)
    draw_thin = (grid != 0) & is_thin[grid]
    width_px = (w + d) * 2 * scale + 8 * scale
    height_px = (w + d + 2 * h) * scale + 8 * scale
    ox = d * 2 * scale + 4 * scale
    oy = 2 * h * scale + 4 * scale
    zbuf = np.full(width_px * height_px, -1e9, dtype=np.float64)
    img = np.zeros((width_px * height_px, 3), dtype=np.float32)
    img[:] = sky

    def splat(mask, faces, lift):
        xs, ys, zs = np.nonzero(mask)
        if len(xs) == 0:
            return
        c = cols[grid[xs, ys, zs]]
        depth = (xs + zs + ys).astype(np.float64) + lift * 0.1
        ax = (xs - zs) * 2 * scale + ox
        ay = (xs + zs - 2 * (ys + 1)) * scale + oy + (2 * scale if lift else 0)
        for pixels, shade, face in faces:
            for dx, dy in pixels:
                for a in range(scale):
                    for b in range(scale):
                        px = ax + dx * scale + a
                        py = ay + dy * scale + b
                        ok = face if face is not None else np.ones(len(xs), dtype=bool)
                        ok = ok & (px >= 0) & (px < width_px) & (py >= 0) & (py < height_px)
                        pix = (py * width_px + px)[ok]
                        dep = depth[ok]
                        order = np.lexsort((dep, pix))
                        pix, dep = pix[order], dep[order]
                        last = np.r_[pix[1:] != pix[:-1], True]
                        pix, dep, sel = pix[last], dep[last], np.nonzero(ok)[0][order][last]
                        win = dep >= zbuf[pix]
                        zbuf[pix[win]] = dep[win]
                        img[pix[win]] = c[sel[win]] * shade

    xs, ys, zs = np.nonzero(draw_solid)
    top_face = open_top[xs, ys, zs]
    mask_faces = [(TOP, 1.0, None), (SOUTH, 0.78, None), (EAST, 0.62, None)]
    splat(draw_solid, mask_faces, 0)
    splat(draw_thin, [(TOP, 1.0, None)], 1)
    del top_face
    picture = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8).reshape(height_px, width_px, 3))
    if people:
        dr = ImageDraw.Draw(picture)
        marks = {"hexkagune:human": (255, 220, 60), "hexkagune:ccg_investigator": (40, 120, 255)}
        for kind, x, y, z in people:
            sx = (x - z) * 2 * scale + ox
            sy = (x + z - 2 * (y + 1)) * scale + oy - 4 * scale
            r = max(2, scale + 1)
            dr.ellipse((sx - r, sy - r, sx + r, sy + r), fill=marks.get(kind, (255, 255, 255)), outline=(0, 0, 0))
    picture.save(out)
    return picture


def main():
    args = sys.argv[1:]
    out = args[0]
    scale = int(args[args.index("--scale") + 1]) if "--scale" in args else 1
    box = tuple(int(v) for v in args[args.index("--box") + 1:args.index("--box") + 5]) if "--box" in args else None
    turn = int(args[args.index("--turn") + 1]) if "--turn" in args else 0
    grid, names, people = load_city(turn)
    render(grid, names, out, scale, box, people if "--people" in args else ())
    print(out)


if __name__ == "__main__":
    main()
