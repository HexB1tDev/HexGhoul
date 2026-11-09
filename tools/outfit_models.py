#!/usr/bin/env python3
"""4.0.0 (The Great Exploration): the owner's outfits and masks as the game draws them, and the masks a wild ghoul may wear.

    python3 tools/outfit_models.py [--preview <dir>]

Reads the owner's canonical geometry (art/outfits/: formal.model.json and crimson.model.json, the 24 masks' masks/<name>/<name>.model.json,
each with its 512 x 512 texture; Minecraft model units, Java Y-down, absolute positions in the player model's frame, each cube on one bone
with its own pivot and rotation and its box-UV offset), adds the generic masks (GENERIC, made here: a clown's, a gas mask, a skull, a fox,
an oni, a cloth wrap; no character's), and writes:

  src/main/resources/assets/hexkagune/models/outfits.bin       every model, baked (OutfitModels reads it)
  src/main/resources/assets/hexkagune/textures/outfit/<id>.png  every model's texture (the owner's as they are; the generic ones painted)
  src/main/resources/assets/hexkagune/textures/item/<item>.png  each item's icon: the model's own cubes seen from the front, 32 x 32
  src/main/resources/assets/hexkagune/models/item/<item>.json   each item's model (item/generated)

Box UV is the game's own (ModelPart.Cube: the faces' nets from the cube's offset, fractional sizes as they are), so a texture lands where
the owner's tools put it. Nothing in the owner's files is changed.
"""
import json
import math
import random
import struct
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art" / "outfits"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune"
BIN = ASSETS / "models" / "outfits.bin"
BONES = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg"]
SLOTS = ["helmet", "chestplate", "leggings", "boots"]
PIVOTS = {"head": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0), "right_leg": (-1.9, 12, 0),
          "left_leg": (1.9, 12, 0)}

# The owner's masks: (their folder, the item's id, its name in game). Kaneki's is "eyepatch" (the build keeps that name out of the jar).
MASKS = [
    ("kaneki", "mask_eyepatch", "Eyepatch Mask"), ("touka", "mask_touka", "Touka's Mask"), ("ayato", "mask_ayato", "Ayato's Mask"),
    ("noro", "mask_noro", "Noro's Mask"), ("yamori", "mask_yamori", "Hockey Mask"), ("eto", "mask_eto", "Eto's Mask"),
    ("tatara", "mask_tatara", "Red Muzzle Mask"), ("bin_brothers", "mask_bin_brothers", "Laughing Mask"),
    ("roma", "mask_roma", "Jester Mask"), ("shachi", "mask_shachi", "Fanged Mask"), ("rize", "mask_rize", "White Muzzle Mask"),
    ("uta", "mask_uta", "Crescent Mask"), ("itori", "mask_itori", "Red Vine Mask"), ("yomo", "mask_yomo", "Black Respirator"),
    ("yoshimura", "mask_yoshimura", "Dotted Mask"), ("nishiki", "mask_nishiki", "Black Grin Mask"),
    ("tsukiyama", "mask_tsukiyama", "Gourmet's Half Mask"), ("kanae", "mask_kanae", "Rose Filigree Mask"),
    ("donato", "mask_donato", "Priest's Mask"), ("koori", "mask_koori", "Slit Mask"), ("amon", "mask_amon", "Bolted Mask"),
    ("seidou", "mask_seidou", "Purple Grin Mask"), ("akira", "mask_akira", "Violet Mask"), ("arima", "mask_arima", "White Line Mask"),
]
# The outfits: (their file, each slot's item id and name).
OUTFITS = [
    ("formal", [("formal_helmet", "Formal Cap"), ("formal_chestplate", "Formal Overcoat"), ("formal_leggings", "Formal Trousers"),
                ("formal_boots", "Formal Shoes")]),
    ("crimson", [("crimson_helmet", "Crimson Hood"), ("crimson_chestplate", "Crimson Coat"), ("crimson_leggings", "Crimson Coat Tails"),
                 ("crimson_boots", "Crimson Boots")]),
]

# ---------------------------------------------------------------- the generic masks (a wild ghoul's), made here

COLOURS = {"white": (232, 230, 224), "bone": (221, 212, 190), "black": (24, 24, 28), "red": (178, 28, 36), "dark_red": (110, 18, 24),
           "blue": (60, 110, 200), "grey": (120, 122, 128), "dark_grey": (58, 60, 66), "olive": (92, 100, 58), "lens": (40, 70, 72),
           "gold": (212, 168, 62), "ivory": (236, 226, 196), "cloth": (46, 48, 60), "cloth_dark": (30, 31, 40), "strap": (34, 30, 28),
           "pink": (214, 120, 140)}


def strap():
    return [((-4.38, -4.1, -3.9), (0.3, 1.1, 8.2), "strap", None, "side_strap"), ((4.08, -4.1, -3.9), (0.3, 1.1, 8.2), "strap", None, "side_strap"),
            ((-4.38, -4.1, 4.08), (8.76, 1.1, 0.25), "strap", None, "rear_strap")]


def pair(pos, size, mat, rot=None):
    """A cube and its mirror across the face's middle (x)."""
    x, y, z = pos
    w = size[0]
    out = [(pos, size, mat) + ((rot,) if rot else ())]
    mirrored = None if rot is None else (rot[0], -rot[1], -rot[2])
    out.append(((-x - w, y, z), size, mat) + ((mirrored,) if mirrored else ()))
    return out


GENERIC = {
    "mask_clown": ("Clown Mask", strap() + [
        ((-4.25, -8.1, -4.45), (8.5, 8.2, 0.35), "white"),
        ((-0.75, -4.2, -5.0), (1.5, 1.5, 0.6), "red"),
        *pair((-3.1, -5.9, -4.52), (1.4, 1.4, 0.1), "black", (0, 0, 45)),
        *pair((-3.4, -7.2, -4.52), (2.2, 0.35, 0.1), "black", (0, 0, -12)),
        *pair((-2.9, -4.3, -4.52), (0.6, 0.9, 0.1), "blue"),
        ((-2.6, -2.3, -4.52), (5.2, 0.7, 0.1), "red"),
        *pair((-3.4, -2.9, -4.52), (1.0, 0.6, 0.1), "red", (0, 0, 30)),
        ((-2.0, -1.65, -4.53), (4.0, 0.35, 0.1), "black"),
        *pair((-3.9, -3.4, -4.51), (1.1, 1.1, 0.1), "pink"),
    ]),
    "mask_gas": ("Gas Mask", strap() + [
        ((-4.2, -7.0, -4.5), (8.4, 7.2, 0.45), "black"),
        *pair((-3.4, -6.0, -4.8), (2.5, 2.3, 0.35), "grey"),
        *pair((-3.1, -5.7, -4.86), (1.9, 1.7, 0.1), "lens"),
        ((-1.3, -3.4, -5.7), (2.6, 2.6, 1.3), "olive"),
        ((-1.5, -3.6, -5.05), (3.0, 3.0, 0.5), "dark_grey"),
        ((-0.9, -3.0, -5.75), (1.8, 1.8, 0.1), "black"),
        *pair((-4.0, -2.8, -4.4), (1.2, 1.6, 1.2), "olive"),
        ((-3.4, -0.6, -4.55), (6.8, 0.6, 0.6), "black"),
    ]),
    "mask_skull": ("Skull Mask", strap() + [
        ((-4.2, -8.0, -4.45), (8.4, 8.1, 0.35), "bone"),
        *pair((-3.3, -6.0, -4.52), (2.3, 2.1, 0.1), "black"),
        ((-0.6, -4.0, -4.52), (1.2, 1.1, 0.1), "black"),
        ((-2.7, -2.1, -4.52), (5.4, 1.4, 0.1), "black"),
        *[((-2.5 + i * 0.9, -2.05, -4.56), (0.7, 1.3, 0.1), "bone") for i in range(6)],
        *pair((-4.0, -4.2, -4.5), (1.4, 0.4, 0.1), "grey"),
        ((-0.2, -7.9, -4.51), (0.4, 1.6, 0.1), "grey"),
    ]),
    "mask_fox": ("Fox Mask", strap() + [
        ((-4.2, -7.6, -4.45), (8.4, 7.0, 0.35), "white"),
        ((-1.6, -3.9, -5.6), (3.2, 2.4, 1.2), "white"),
        ((-0.5, -3.1, -5.72), (1.0, 0.6, 0.15), "black"),
        *pair((-4.0, -10.2, -4.3), (2.2, 2.7, 0.6), "white"),
        *pair((-3.6, -9.6, -4.36), (1.4, 1.9, 0.1), "red"),
        *pair((-3.5, -5.6, -4.52), (2.5, 0.4, 0.1), "red", (0, 0, 18)),
        *pair((-3.3, -5.25, -4.53), (1.7, 0.35, 0.1), "black", (0, 0, 18)),
        ((-0.35, -7.4, -4.51), (0.7, 1.4, 0.1), "red"),
        *pair((-3.8, -2.5, -4.52), (1.6, 0.3, 0.1), "red", (0, 0, -15)),
    ]),
    "mask_oni": ("Oni Mask", strap() + [
        ((-4.25, -8.0, -4.45), (8.5, 8.1, 0.4), "red"),
        *pair((-3.7, -10.8, -3.6), (1.1, 3.0, 1.1), "ivory", (0, 0, 10)),
        ((-3.9, -6.7, -4.9), (7.8, 0.75, 0.5), "dark_red"),
        *pair((-3.1, -5.7, -4.53), (2.0, 1.1, 0.1), "gold"),
        *pair((-2.5, -5.55, -4.56), (0.7, 0.8, 0.1), "black"),
        ((-0.85, -4.3, -5.0), (1.7, 1.5, 0.6), "dark_red"),
        ((-2.9, -2.3, -4.55), (5.8, 1.3, 0.1), "black"),
        *pair((-2.5, -2.7, -4.6), (0.6, 1.6, 0.15), "ivory"),
        *pair((-1.3, -1.25, -4.6), (0.5, 0.4, 0.12), "ivory"),
    ]),
    "mask_cloth": ("Cloth Mask", [
        ((-4.32, -4.4, -4.55), (8.64, 4.5, 0.45), "cloth"),
        ((-4.32, -4.4, -4.2), (0.35, 4.5, 8.5), "cloth"),
        ((3.97, -4.4, -4.2), (0.35, 4.5, 8.5), "cloth"),
        ((-4.32, -4.4, 4.0), (8.64, 2.0, 0.35), "cloth"),
        ((-4.36, -2.9, -4.62), (8.72, 0.25, 0.1), "cloth_dark"),
        ((-4.36, -1.5, -4.62), (8.72, 0.25, 0.1), "cloth_dark"),
        ((-1.1, -4.3, 4.3), (2.2, 1.7, 0.8), "cloth_dark"),
        ((-0.9, -2.7, 4.4), (0.8, 3.2, 0.25), "cloth", (12, 0, 8)),
        ((0.2, -2.7, 4.4), (0.8, 2.8, 0.25), "cloth", (12, 0, -10)),
    ]),
}


def generic_model(cubes):
    """Cubes on the head bone, packed into a 128 x 128 atlas (each net at its own offset), painted by material."""
    size = 128
    out, x, y, row = [], 2, 2, 0
    for c in cubes:
        pos, dims, mat = c[0], c[1], c[2]
        rot = c[3] if len(c) > 3 and c[3] else (0, 0, 0)
        w, h, d = dims
        nw = math.ceil(2 * (w + d)) + 2
        nh = math.ceil(d + h) + 2
        if x + nw > size:
            x, y, row = 2, y + row, 0
        out.append({"name": c[4] if len(c) > 4 else mat, "bone": "head", "slot": "helmet", "pos": list(pos), "size": list(dims), "mat": mat, "uv": [x, y], "rot": list(rot),
                    "pivot": [pos[0] + w / 2, pos[1] + h / 2, pos[2] + d / 2] if any(rot) else list(pos)})
        x += nw
        row = max(row, nh)
    assert y + row <= size, "atlas too small"
    return {"texture_width": size, "texture_height": size, "pivots": PIVOTS, "cubes": out}


def paint(model, seed):
    """Each cube's net filled with its material's colour, a little grain, its edges a shade darker."""
    size = model["texture_width"]
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(seed)
    for c in model["cubes"]:
        u, v = c["uv"]
        w, h, d = c["size"]
        base = COLOURS[c["mat"]]
        x1, y1 = math.ceil(u + 2 * (w + d)), math.ceil(v + d + h)
        for x in range(int(u), min(size, x1 + 1)):
            for y in range(int(v), min(size, y1 + 1)):
                g = rng.uniform(-7, 7)
                edge = x in (int(u), x1) or y in (int(v), y1)
                f = 0.86 if edge else 1.0
                px[x, y] = tuple(max(0, min(255, int(ch * f + g))) for ch in base) + (255,)
    return img


# ---------------------------------------------------------------- box UV (the game's own) and the icons

def faces(cube, texw, texh):
    """The cube's six faces as the game makes them: ([4 corners], [4 uvs], outward normal), in the model's frame, rotated about its pivot."""
    x0, y0, z0 = cube["pos"]
    w, h, d = cube["size"]
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    u, v = cube["uv"]
    f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    f10, f11, f12 = v, v + d, v + d + h
    v0, v1, v2, v3 = (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)
    v4, v5, v6, v7 = (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)

    def poly(vs, ua, va, ub, vb, n):
        return vs, [(ub / texw, va / texh), (ua / texw, va / texh), (ua / texw, vb / texh), (ub / texw, vb / texh)], n

    out = [poly([v5, v1, v2, v6], f6, f11, f8, f12, (1, 0, 0)),     # east
           poly([v0, v4, v7, v3], f4, f11, f5, f12, (-1, 0, 0)),    # west
           poly([v5, v4, v0, v1], f5, f10, f6, f11, (0, -1, 0)),    # down (y up in the game's frame; the model's -y)
           poly([v2, v3, v7, v6], f6, f11, f7, f10, (0, 1, 0)),     # up
           poly([v1, v0, v3, v2], f5, f11, f6, f12, (0, 0, -1)),    # north (the front)
           poly([v4, v5, v6, v7], f8, f11, f9, f12, (0, 0, 1))]     # south
    rx, ry, rz = (math.radians(a) for a in cube["rot"])
    if rx or ry or rz:
        p = cube["pivot"]

        def turn(q):
            x, y, z = q[0] - p[0], q[1] - p[1], q[2] - p[2]
            # The game's ModelPart: rotationZYX(z, y, x): x first, then y, then z.
            y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
            x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
            x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
            return (x + p[0], y + p[1], z + p[2])

        out = [([turn(q) for q in vs], uvs, turn((n[0] + p[0], n[1] + p[1], n[2] + p[2]))) for vs, uvs, n in out]
        out = [(vs, uvs, (n[0] - p[0], n[1] - p[1], n[2] - p[2])) for vs, uvs, n in out]
    return out


def icon(model, texture, slot, size=32, yaw=-28.0, pitch=0.0):
    """The cubes of {@code slot} seen from the front (a little turned), on a transparent square: an inventory icon."""
    tex = texture.convert("RGBA")
    tw, th = model["texture_width"], model["texture_height"]
    tpx = tex.load()
    # A mask's straps round the head are left out of its icon (seen from the front they stick out sideways).
    cubes = [c for c in model["cubes"] if c.get("slot", "helmet") == slot and "strap" not in c.get("name", "")]
    pts = []
    a = math.radians(yaw)
    ca, sa = math.cos(a), math.sin(a)
    b = math.radians(pitch)
    cb, sb = math.cos(b), math.sin(b)

    def view(q):
        x, y, z = q
        x, z = x * ca + z * sa, -x * sa + z * ca
        # Seen from a little above: the model's y is down, so its top tips toward the viewer.
        return (x, y * cb - z * sb, y * sb + z * cb)

    polys = []
    for c in cubes:
        for vs, uvs, n in faces(c, tw, th):
            vv = [view(q) for q in vs]
            nn = view(n)
            if nn[2] >= -1e-4:  # facing away (the viewer looks along +z from z = -inf)
                continue
            polys.append((vv, uvs, nn))
            pts.extend(vv)
    if not pts:
        return Image.new("RGBA", (size, size), (0, 0, 0, 0))
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    span = max(max(xs) - min(xs), max(ys) - min(ys))
    big = size * 4
    scale = (big - 8) / span
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    cpx = canvas.load()
    depth = [[1e9] * big for _ in range(big)]
    light = (0.35, -0.55, -0.75)
    ln = math.sqrt(sum(c * c for c in light))
    for vv, uvs, nn in polys:
        shade = 0.62 + 0.38 * max(0.0, -(nn[0] * light[0] + nn[1] * light[1] + nn[2] * light[2]) / ln)
        steps_u = max(2, int(math.dist(vv[0], vv[1]) * scale) + 2)
        steps_v = max(2, int(math.dist(vv[1], vv[2]) * scale) + 2)
        for i in range(steps_u + 1):
            s = i / steps_u
            for j in range(steps_v + 1):
                t = j / steps_v
                # Bilinear over the quad (corners 0..3 in order) and its uvs.
                p = [(1 - s) * (1 - t) * vv[0][k] + s * (1 - t) * vv[1][k] + s * t * vv[2][k] + (1 - s) * t * vv[3][k] for k in range(3)]
                uv = [(1 - s) * (1 - t) * uvs[0][k] + s * (1 - t) * uvs[1][k] + s * t * uvs[2][k] + (1 - s) * t * uvs[3][k] for k in range(2)]
                X = int((p[0] - cx) * scale + big / 2)
                Y = int((p[1] - cy) * scale + big / 2)
                if not (0 <= X < big and 0 <= Y < big) or p[2] >= depth[Y][X]:
                    continue
                tx = min(tw - 1, max(0, int(uv[0] * tw)))
                ty = min(th - 1, max(0, int(uv[1] * th)))
                col = tpx[tx, ty]
                if col[3] < 16:
                    continue
                depth[Y][X] = p[2]
                cpx[X, Y] = (int(col[0] * shade), int(col[1] * shade), int(col[2] * shade), 255)
    return canvas.resize((size, size), Image.LANCZOS)


# ---------------------------------------------------------------- writing

def bake(models):
    out = bytearray(b"HXOF")
    out += struct.pack(">HH", 1, len(models))
    for mid, model in models:
        name = mid.encode("utf-8")
        out += struct.pack(">H", len(name)) + name
        out += struct.pack(">HHH", model["texture_width"], model["texture_height"], len(model["cubes"]))
        for c in model["cubes"]:
            out += struct.pack(">BB", BONES.index(c["bone"]), SLOTS.index(c.get("slot", "helmet")))
            out += struct.pack(">14f", *c["pos"], *c["size"], *c["pivot"], *c["rot"], *c["uv"])
    BIN.parent.mkdir(parents=True, exist_ok=True)
    BIN.write_bytes(bytes(out))


def item_model(item):
    path = ASSETS / "models" / "item" / f"{item}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"hexkagune:item/{item}"}}, indent=2) + "\n")


def main():
    models = []
    names = {}
    textures = ASSETS / "textures" / "outfit"
    items = ASSETS / "textures" / "item"
    textures.mkdir(parents=True, exist_ok=True)
    items.mkdir(parents=True, exist_ok=True)
    previews = []
    for style, pieces in OUTFITS:
        model = json.loads((ART / f"{style}.model.json").read_text())
        tex = Image.open(ART / f"{style}.png")
        tex.save(textures / f"{style}.png")
        models.append((style, model))
        for slot, (item, label) in zip(SLOTS, pieces):
            names[item] = label
            img = icon(model, tex, slot, yaw=-30.0, pitch=28.0 if slot in ("helmet", "boots") else 0.0)
            img.save(items / f"{item}.png")
            item_model(item)
            previews.append((item, img))
    for folder, item, label in MASKS:
        model = json.loads((ART / "masks" / folder / f"{folder}.model.json").read_text())
        tex = Image.open(ART / "masks" / folder / f"{folder}.png")
        tex.save(textures / f"{item}.png")
        models.append((item, model))
        names[item] = label
        img = icon(model, tex, "helmet", yaw=0.0)
        img.save(items / f"{item}.png")
        item_model(item)
        previews.append((item, img))
    for i, (item, (label, cubes)) in enumerate(GENERIC.items()):
        model = generic_model(cubes)
        tex = paint(model, i)
        tex.save(textures / f"{item}.png")
        models.append((item, model))
        names[item] = label
        img = icon(model, tex, "helmet", yaw=0.0)
        img.save(items / f"{item}.png")
        item_model(item)
        previews.append((item, img))
    bake(models)
    print(f"{len(models)} models, {sum(len(m['cubes']) for _, m in models)} cubes, {BIN.stat().st_size} bytes")
    print(json.dumps(names))
    if "--preview" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--preview") + 1])
        out.mkdir(parents=True, exist_ok=True)
        cols = 8
        sheet = Image.new("RGBA", (cols * 136, math.ceil(len(previews) / cols) * 136), (40, 42, 48, 255))
        for k, (item, img) in enumerate(previews):
            big = img.resize((128, 128), Image.NEAREST)
            sheet.paste(big, ((k % cols) * 136 + 4, (k // cols) * 136 + 4), big)
        sheet.save(out / "outfit-icons.png")
        for item, (label, cubes) in GENERIC.items():
            model = generic_model(cubes)
            icon(model, paint(model, 0), "helmet", size=160, yaw=-25.0).save(out / f"{item}-big.png")


if __name__ == "__main__":
    main()
