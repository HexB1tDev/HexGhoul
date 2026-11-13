#!/usr/bin/env python3
"""4.1.35: an isometric picture of a tiled place as the game gets it (4.1.72: any site's tiles, data/hexkagune/structures/<site>/t_i_j.nbt,
the hotel by default; the Small City it was written for is gone), put back side by side, every block a small coloured cube seen from the
south-east, lit from above (tops brightest, the south faces darker, the east faces darkest), painter's order through a depth buffer.

    python3 tools/city_render.py out.png [--site ID] [--scale N] [--box x0 z0 x1 z1] [--turn 0..3] [--people] [--grass | --land]
                                         [--roof Y] [--cut south|east]

--box renders only that part of it (x, z from its north-west corner); --scale N makes each block N times bigger; --turn turns it a quarter
at a time (as the game turns a structure) before it is drawn; --people marks where its people stand; --grass sets it on a stretch of grass,
--land on rolling land with a river (fitted roughly as its terrain adaptation fits it); --roof Y leaves out everything from its layer Y up
(see inside a storey); --cut south leaves out the southern half (east: the eastern half).
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import library_structure as L  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src" / "main" / "resources" / "data" / "hexkagune" / "structures"

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


def load_site(site="hotel", turn=0):
    """
    A tiled site as a (x, y, z) array of indices into names (0: air), x and z from its north-west corner; and its people (kind, x, y, z).
    Each tile t_i_j sits after the tiles before it in its row and column (their sizes read from the tiles themselves).
    """
    folder = DATA / site
    tiles = {}
    for f in folder.glob("t_*_*.nbt"):
        i, j = (int(v) for v in f.stem.split("_")[1:])
        root = L.read(f)
        tiles[i, j] = root.value if isinstance(root, L.Tag) else root
    if not tiles:
        raise SystemExit(f"no tiles in {folder}")
    across = 1 + max(i for i, _ in tiles)
    down = 1 + max(j for _, j in tiles)
    size = {k: [_v(v) for v in _v(r["size"])] for k, r in tiles.items()}
    xs = [0]
    for i in range(across):
        xs.append(xs[-1] + size[i, 0][0])
    zs = [0]
    for j in range(down):
        zs.append(zs[-1] + size[0, j][2])
    height = max(v[1] for v in size.values())
    names = ["minecraft:air"]
    index = {"minecraft:air": 0}
    grid = np.zeros((xs[-1], height, zs[-1]), dtype=np.uint16)
    people = []
    for (i, j), root in tiles.items():
        pal = []
        for p in _v(root["palette"]):
            n = _v(p["Name"])
            if n not in index:
                index[n] = len(names)
                names.append(n)
            pal.append(index[n])
        for b in _v(root["blocks"]):
            x, y, z = (_v(v) for v in _v(b["pos"]))
            grid[xs[i] + x, y, zs[j] + z] = pal[_v(b["state"])]
        for e in _v(root["entities"]):
            x, y, z = (_v(v) for v in _v(e["blockPos"]))
            people.append((_v(_v(e["nbt"])["id"]), xs[i] + x, y, zs[j] + z))
    w, d = grid.shape[0], grid.shape[2]
    for _ in range(turn % 4):
        # A quarter turn clockwise seen from above (north up), as the game turns a template: x' = (d - 1) - z, z' = x.
        grid = turned(grid, 1)
        people = [(kind, d - 1 - z, y, x) for kind, x, y, z in people]
        w, d = d, w
    return grid, names, people


def turned(grid, t):
    """A (x, y, z) grid turned t quarter turns clockwise as the game turns a template (x' = -z, z' = x), its least corner at 0."""
    t %= 4
    if t == 1:
        return np.ascontiguousarray(grid[:, :, ::-1].transpose(2, 1, 0))
    if t == 2:
        return np.ascontiguousarray(grid[::-1, :, ::-1])
    if t == 3:
        return np.ascontiguousarray(grid[::-1, :, :].transpose(2, 1, 0))
    return grid


def on_grass(grid, names, margin=40, ground=6):
    """The city with a stretch of grass round it (and under its open ground), as if the world's ground met it."""
    w, h, d = grid.shape
    if "minecraft:grass_block" not in names:
        names.append("minecraft:grass_block")
    if "minecraft:dirt" not in names:
        names.append("minecraft:dirt")
    grass, dirt = names.index("minecraft:grass_block"), names.index("minecraft:dirt")
    out = np.zeros((w + 2 * margin, h, d + 2 * margin), dtype=grid.dtype)
    out[:, ground - 2:ground, :] = dirt
    out[:, ground, :] = grass
    inner = out[margin:margin + w, :, margin:margin + d]
    inner[grid != 0] = grid[grid != 0]
    return out


def on_land(grid, names, margin=64, seed=3):
    """
    4.1.36: the city on rolling land with a river running under it, fitted the way its terrain adaptation (beard_box) fits it, roughly:
    its streets on the middle of the land's levels (the river's top counted, never under the sea's level), the land cut off over its
    ground and filled up under it inside its footprint, and the land round it eased to its ground over 12 blocks. An approximation of the
    game's ground, to judge how the city meets the land.
    """
    import math
    w, h, d = grid.shape
    for n in ("minecraft:grass_block", "minecraft:dirt", "minecraft:water", "minecraft:sand"):
        if n not in names:
            names.append(n)
    grass, dirt, water, sand = (names.index(n) for n in ("minecraft:grass_block", "minecraft:dirt", "minecraft:water", "minecraft:sand"))
    W, D = w + 2 * margin, d + 2 * margin
    xs = np.arange(W)[:, None]
    zs = np.arange(D)[None, :]
    land = 68 + 4.0 * np.sin(xs / 61.0 + seed) + 3.0 * np.cos(zs / 47.0) + 2.0 * np.sin((xs + zs) / 29.0)
    # A river crossing from the north-west to the south-east, its top at the sea's level (62), its bed 4 under.
    path = D * 0.3 + xs * 0.55 + 22.0 * np.sin(xs / 90.0)
    dist = np.abs(zs - path)
    river = dist < 7
    bank = (dist >= 7) & (dist < 14)
    land = np.where(river, 58.0, np.where(bank, 62.0 + (dist - 7) / 7.0 * (land - 62.0), land))
    level = np.where(river, 63.0, land)
    inside = np.zeros((W, D), dtype=bool)
    inside[margin:margin + w, margin:margin + d] = True
    samples = [level[margin + 2 + (w - 5) * i // 8, margin + 2 + (d - 5) * j // 8] for i in range(9) for j in range(9)]
    street = max(64, int(round(sorted(samples)[len(samples) // 2])))
    ground = street - 1
    # Eased to the city's ground within 12 blocks of its footprint (Chebyshev), as the beard does.
    ex = np.maximum(0, np.maximum(margin - xs, xs - (margin + w - 1)))
    ez = np.maximum(0, np.maximum(margin - zs, zs - (margin + d - 1)))
    edge = np.maximum(ex, ez)
    ease = np.clip(edge / 12.0, 0, 1)
    fitted = np.where(inside, ground, ground + (land - ground) * ease)
    top = np.floor(fitted).astype(int)
    lift = ground - 6  # the world's y of the grid's layer 0
    H = max(h, int(top.max()) - lift + 2)
    out = np.zeros((W, H, D), dtype=grid.dtype)
    for x in range(W):
        for z in range(D):
            t = top[x, z] - lift
            if t >= 0:
                out[x, :t, z] = dirt
                out[x, t, z] = sand if (river[x, z] or bank[x, z]) and not inside[x, z] else grass
            if (river[x, z] or bank[x, z]) and not inside[x, z]:
                # Water stands wherever the eased ground is under the sea's level (the game's water fills there).
                for y in range(max(0, t + 1), 62 - lift + 1):
                    out[x, y, z] = water
    inner = out[margin:margin + w, :h, margin:margin + d]
    inner[:, 6:, :] = 0
    inner[grid != 0] = grid[grid != 0]
    print(f"its streets at y {street} (the land's middle; the river's top counted)")
    return out


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


def ground_of(site):
    """The site's street level (its slab's or ground's top layer: SiteMaps' ground), read from SiteMaps.java."""
    import re
    text = (ROOT / "src/main/java/com/hex/kagune/control/village/SiteMaps.java").read_text()
    found = re.search(r'new Site\("' + re.escape(site) + r'", \d+, \d+, \d+, \d+, (\d+),', text)
    return int(found.group(1)) if found else 6


def main():
    args = sys.argv[1:]
    out = args[0]
    site = args[args.index("--site") + 1] if "--site" in args else "hotel"
    scale = int(args[args.index("--scale") + 1]) if "--scale" in args else 1
    box = tuple(int(v) for v in args[args.index("--box") + 1:args.index("--box") + 5]) if "--box" in args else None
    turn = int(args[args.index("--turn") + 1]) if "--turn" in args else 0
    grid, names, people = load_site(site, turn)
    if "--roof" in args:
        grid[:, int(args[args.index("--roof") + 1]):, :] = 0
    if "--cut" in args:
        cut = args[args.index("--cut") + 1]
        if cut == "south":
            grid[:, :, grid.shape[2] // 2:] = 0
        else:
            grid[grid.shape[0] // 2:, :, :] = 0
    margin = 0
    if "--grass" in args:
        margin = 40
        grid = on_grass(grid, names, margin, ground_of(site))
    elif "--land" in args:
        margin = 64
        grid = on_land(grid, names, margin)
    people = [(k, x + margin, y, z + margin) for k, x, y, z in people]
    render(grid, names, out, scale, box, people if "--people" in args else ())
    print(out)


if __name__ == "__main__":
    main()
