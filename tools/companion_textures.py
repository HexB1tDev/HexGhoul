#!/usr/bin/env python3
"""1.3.14: the companions' textures (Touka, Ken), from the owner's skins, in Eto's way (tools/eto_textures.py).

    python3 tools/companion_textures.py [preview-dir]

For each companion it reads art/<key>_skin.png (a 64x64 player skin, or 128x128 at twice the pixels) and writes, under
src/main/resources/assets/hexkagune/textures/entity/<key>/:

  <key>.png               the skin as it is
  <key>_kakugan.png       the eyes turned (only these pixels, laid over the skin as they turn): each eye's white black, its iris red,
                          a thin vein at the outer corner. Touka turns both eyes; Ken only his left (the one-eyed king)
  <key>_kakugan_glow.png  the red irises alone, drawn full bright
  <key>_closed.png        the eyes shut for blinks and kisses: the skin's lashes kept, the row under them in the skin's own colour, a black
                          line one pixel lower, the rest of the eye skin too (1.3.30, as the owner marked it)
  <key>_blush.png         pink across the cheeks, under each eye

The eyes are found on the face (columns 8-15, rows 8-15 of a 64px skin): in its middle rows, the pixels that are neither skin nor hair;
the bright, grey ones are the whites, the rest the irises; the left half is the character's right eye. A skin whose eyes are not found
that way can name them in EYES below. With no skin in art/ yet, a stand-in is painted (art/placeholder/<key>_skin.png) so the mod
builds and shows something; drop the real skin in and run this again.
"""
import os
import sys
from collections import Counter
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art"
OUT = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "entity"

# Which eyes turn: the character's right (the viewer's left on the face), left, or both.
COMPANIONS = {
    # 1.3.17: her skin's right eye is painted as a ghoul's: by day it is the mirror of her left (as Eto's is), turning only as a kakugan.
    "touka": {"turns": ("right", "left"), "slim": True, "mirror": True},
    "ken": {"turns": ("left",), "slim": True},
    # 4.0.0: Ayato (the owner's skin, art/exploration/Ayato_skin.png): both eyes turn; Noro (Noro_skin.webp): his face is his stitched
    # mask, its two dark eye-holes turn red.
    # 4.1.48 (the owner: "Ayato and Noro should both be Alex models"): both on the slim arms. Ayato's skin is painted for them already
    # (its arms' fourth columns are clear); Noro's is painted for the wide arms: the game's copy has its arms narrowed (narrow_arms).
    "ayato": {"turns": ("right", "left"), "slim": True},  # 4.0.8: the owner's new skin (art/exploration/Ayato_skin_2.png)
    "noro": {"turns": ("right", "left"), "slim": True, "narrow": True},
    # 4.1.5: the General (the owner's skin, sent with "use this skin for the general": art/exploration/General_skin.png, slim arms). A
    # human: no eye of his ever turns (his kakugan textures are empty).
    "general": {"turns": (), "slim": True},
}
# Eye pixels by hand, if detection is wrong for a skin: {key: {"right": [(x, y), ...], "left": [...], "white": [...]}} in 64px face
# coordinates (0..7 across the face front, 0..7 down).
EYES = {
    # 1.3.17: the owner's second Touka skin: her right eye painted dark (no white to find), her left lavender with a white corner.
    "touka": {"right": [(1, 5), (2, 5), (1, 6), (2, 6)], "left": [(5, 5), (6, 5), (5, 6), (6, 6)], "white": [(1, 5), (1, 6), (6, 5), (6, 6)],
              "lash": [(1, 4), (2, 4), (5, 4), (6, 4)]},
    # 1.3.22: the owner: "Kaneki's eye's iris is wrong". Detection read his eyes by rows (the grey top row as iris, the pale bottom row
    # as white: a red bar over a black one). They are columns, as Touka's: the inner one his grey iris (dark grey over light grey), the
    # outer one the white (shadowed by the lid over bright white); the near-black pixel over the iris, under his bangs, is the lash.
    "ken": {"right": [(1, 5), (2, 5), (1, 6), (2, 6)], "left": [(5, 5), (6, 5), (5, 6), (6, 6)], "white": [(1, 5), (1, 6), (6, 5), (6, 6)],
            "lash": [(2, 4), (5, 4)]},
}

EYES.update({
    # 4.0.8: Ayato's new skin: his eyes are the face's rows 5 and 6 (the white outside on row 6, the violet iris inside), the lashes row 4.
    # 4.1.16: repainted (REPAINT): the white the outer column, both rows.
    "ayato": {"right": [(1, 5), (2, 5), (1, 6), (2, 6)], "left": [(5, 5), (6, 5), (5, 6), (6, 6)], "white": [(1, 5), (1, 6), (6, 5), (6, 6)],
              "lash": [(1, 4), (2, 4), (5, 4), (6, 4)]},
    # 4.0.0: Noro's mask's eye-holes (row 5, the dark stitches' ends).
    "noro": {"right": [(1, 5), (2, 5)], "left": [(5, 5), (6, 5)], "white": [], "lash": []},
    # 4.1.5: the General's eyes are one row (the face's row 4: the white outside, the blue iris inside), under his brows (row 3).
    "general": {"right": [(1, 4), (2, 4)], "left": [(5, 4), (6, 4)], "white": [(1, 4), (6, 4)], "lash": []},
})

# 4.1.16 (the owner: "adapt all eyes to make sure that they all look like eyes ... Some eyes are clearly a little fucked on the skins", and
# Ayato's first): face pixels repainted in the game's copy of a skin (the owner's file in art/ is never changed) so each eye reads as an
# eye. Ayato's skin painted his iris in his hair's own violet (60579f) under a near-black lid, a single white pixel at its lower outer
# corner: in game the eye was a black block over hair. Now each eye is a white outer column and a blue iris inner one (dark at the top,
# light under it), under his lashes.
REPAINT = {
    "ayato": {(1, 5): "f5f6f7", (1, 6): "e8eaf0", (2, 5): "2a3488", (2, 6): "6a82dc",
              (6, 5): "f5f6f7", (6, 6): "e8eaf0", (5, 5): "2a3488", (5, 6): "6a82dc"},
}

BLACK = (12, 4, 8, 255)
LID = (14, 12, 16, 255)  # 1.3.30: the shut lid's line, black
DEEP = (150, 10, 24, 255)
RED = (206, 18, 34, 255)
LIGHT = (240, 52, 62, 255)
VEIN = (120, 6, 20, 200)
GLOW = (255, 60, 70, 255)
BLUSH = (255, 118, 140, 120)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def sat(c):
    return max(c[:3]) - min(c[:3])


def dist(a, b):
    return sum((a[i] - b[i]) ** 2 for i in range(3)) ** 0.5


class Face:
    """The face front of a skin at its scale: get and put by face pixel (0..7), each a scale x scale block."""

    def __init__(self, image):
        self.image = image
        self.s = image.size[0] // 64

    def at(self, x, y, layer=0):
        u0 = 8 if layer == 0 else 40
        return self.image.getpixel(((u0 + x) * self.s, (8 + y) * self.s))

    def put(self, target, x, y, colour):
        for dy in range(self.s):
            for dx in range(self.s):
                target.putpixel(((8 + x) * self.s + dx, (8 + y) * self.s + dy), colour)


def find_eyes(face):
    """{"right": [(x, y)], "left": [(x, y)], "white": set of (x, y), "lash": [(x, y)]} on the face, by colour.

    The whites are bright and greyer than the skin (pale skin is nearly white itself) and never the hair's colour (white hair is as white
    as any eye); the irises are the coloured pixels in the rows of the whites and the rows next to them; the lashes (the dark row over an
    eye) are found too, but are never turned: only the blink paints over them. Columns 0 and 7 are the face's edge (hair at its sides).
    """
    lower = [face.at(x, y) for y in range(5, 8) for x in range(1, 7) if face.at(x, y)[3] > 0]
    skin = Counter(c[:3] for c in lower).most_common(1)[0][0]
    top = [face.at(x, 0) for x in range(8) if face.at(x, 0)[3] > 0]
    hair = Counter(c[:3] for c in top).most_common(1)[0][0] if top else (0, 0, 0)

    def white(c):
        return lum(c) > 150 and sat(c) < 24 and sat(c) < sat(skin) * 0.5 and dist(c, hair) > 30

    def coloured(c):
        return dist(c, skin) >= 38 and dist(c, hair) >= 26

    found = {"right": [], "left": [], "white": set(), "lash": []}
    for side, columns in (("right", range(1, 4)), ("left", range(4, 7))):
        whites = [(x, y) for y in range(2, 8) for x in columns if face.at(x, y)[3] > 0 and white(face.at(x, y))]
        if not whites:
            # No whites at all: the eye is its coloured pixels in the middle rows.
            pixels = [(x, y) for y in range(3, 7) for x in columns if face.at(x, y)[3] > 0 and coloured(face.at(x, y))
                      and lum(face.at(x, y)) >= 60]
            found[side] = pixels
            continue
        rows = sorted({y for x, y in whites})
        span = range(rows[0] - 1, rows[-1] + 2)
        cols = {x for x, y in whites}
        near = set(cols) | {x + 1 for x in cols} | {x - 1 for x in cols}
        pixels = set(whites)
        for y in span:
            for x in columns:
                if x not in near or not (0 <= y < 8) or (x, y) in pixels:
                    continue
                c = face.at(x, y)
                # Inside the eye's own rows, hair is no rival: an off-white as grey as white hair is still the eye.
                pale = lum(c) > 150 and sat(c) < 24 and sat(c) < sat(skin) * 0.5
                if c[3] > 0 and (pale or dist(c, skin) >= 38 and lum(c) >= 60):
                    pixels.add((x, y))
                    if pale:
                        found["white"].add((x, y))
        found[side] = sorted(pixels)
        found["white"].update(p for p in pixels if white(face.at(*p)))
        # The lashes: the dark row just over the eye.
        topmost = min(y for x, y in pixels)
        if topmost >= 1:
            for x in sorted({x for x, y in pixels}):
                c = face.at(x, topmost - 1)
                if c[3] > 0 and lum(c) < 60:
                    found["lash"].append((x, topmost - 1))
    return found, skin


# 4.1.48: each arm's boxes in a 64px skin (its corner u, v): the right arm and its outer layer, the left arm and its outer layer.
ARM_BOXES = [(40, 16), (40, 32), (32, 48), (48, 48)]


def narrow_arms(skin):
    """4.1.48: a skin painted for the wide (4 pixel) arms laid out for the slim (3 pixel) ones, as a slim model reads it: each arm face 4
    wide is made 3 by leaving out one middle column (the front's third, the back's second: the same side of the arm), the sides as they
    are; the slim layout's spare columns cleared. A 128px skin at twice the pixels."""
    k = skin.size[0] // 64
    out = skin.copy()
    src = skin.load()
    dst = out.load()

    def face(u, v, w, h, keep, to_u):
        for y in range(h * k):
            for i, col in enumerate(keep):
                for dx in range(k):
                    dst[(to_u + i) * k + dx, v * k + y] = src[(u + col) * k + dx, v * k + y]

    for u, v in ARM_BOXES:
        # The box's whole area (16 x 16) cleared, then each face laid out again at the slim positions.
        for y in range(16 * k):
            for x in range(16 * k):
                dst[u * k + x, v * k + y] = (0, 0, 0, 0)
        front_keep = [0, 1, 3]
        back_keep = [0, 2, 3]
        face(u + 4, v, 4, 4, front_keep, u + 4)          # top (4 wide -> 3)
        face(u + 8, v, 4, 4, front_keep, u + 7)          # bottom
        face(u, v + 4, 4, 12, [0, 1, 2, 3], u)           # the outer side
        face(u + 4, v + 4, 4, 12, front_keep, u + 4)     # the front
        face(u + 8, v + 4, 4, 12, [0, 1, 2, 3], u + 7)   # the inner side
        face(u + 12, v + 4, 4, 12, back_keep, u + 11)    # the back
    return out


def build(key, config, preview):
    source = ART / (key + "_skin.png")
    if not source.exists():
        source = ART / "placeholder" / (key + "_skin.png")
        paint_placeholder(key, source)
        print(f"{key}: no art/{key}_skin.png yet; using the stand-in {source.relative_to(ROOT)}")
    skin = Image.open(source).convert("RGBA")
    assert skin.size in ((64, 64), (128, 128)), f"{source} is {skin.size}, not a 64x64 (or 128x128) skin"
    if config.get("mirror"):
        # The right eye (the viewer's left, columns 1-2) made the mirror of the left (columns 5-6), lashes and all, on the face only
        # (the outer layer's hair stays as it is drawn).
        day = skin.copy()
        source_face = Face(skin)
        for y in range(3, 8):
            for x in (5, 6):
                source_face.put(day, 7 - x, y, source_face.at(x, y))
        skin = day
    if config.get("narrow"):
        skin = narrow_arms(skin)
    if key in REPAINT:
        skin = skin.copy()
        painter = Face(skin)
        for (x, y), code in REPAINT[key].items():
            painter.put(skin, x, y, (int(code[0:2], 16), int(code[2:4], 16), int(code[4:6], 16), 255))
    face = Face(skin)
    if key in EYES:
        eyes = {"right": EYES[key]["right"], "left": EYES[key]["left"], "white": set(EYES[key].get("white", [])), "lash": EYES[key].get("lash", [])}
        lower = [face.at(x, y) for y in range(5, 8) for x in range(1, 7)]
        tone = Counter(c[:3] for c in lower).most_common(1)[0][0]
    else:
        eyes, tone = find_eyes(face)
    assert eyes["right"] and eyes["left"], f"{key}: no eyes found on the face; name them in EYES"
    out = OUT / key
    out.mkdir(parents=True, exist_ok=True)
    skin.save(out / f"{key}.png")
    blank = lambda: Image.new("RGBA", skin.size, (0, 0, 0, 0))
    kakugan, glow, closed, blush = blank(), blank(), blank(), blank()
    for side in ("right", "left"):
        pixels = sorted(eyes[side], key=lambda p: (p[1], p[0]))
        if side in config["turns"]:
            irises = [p for p in pixels if p not in eyes["white"]]
            top = min(y for x, y in pixels)
            for x, y in pixels:
                if (x, y) in eyes["white"]:
                    face.put(kakugan, x, y, BLACK)
                else:
                    colour = DEEP if y == top and len({yy for xx, yy in irises}) > 1 else RED
                    face.put(kakugan, x, y, colour)
                    face.put(glow, x, y, GLOW if colour is RED else LIGHT)
            # A thin vein down from the outer corner.
            outer = min(x for x, y in pixels) if side == "right" else max(x for x, y in pixels)
            bottom = max(y for x, y in pixels)
            vx = max(0, outer - 1) if side == "right" else min(7, outer + 1)
            if bottom + 1 <= 7:
                face.put(kakugan, outer, bottom + 1, VEIN)
            face.put(kakugan, vx, bottom, VEIN)
        # Shut (1.3.30, where the owner marked it): the skin's own lashes kept as they are, the row under them skin, and one black line a
        # pixel lower (the shut lid); the rest of the eye painted over in skin.
        bottom = max(y for x, y in pixels)
        top = min(y for x, y in pixels)
        line = min(top + 1, bottom)
        for x, y in pixels:
            face.put(closed, x, y, LID if y == line else tone + (255,))
        # Blush: just under the eye.
        cheek = min(7, bottom + 1)
        for x in sorted({x for x, y in pixels}):
            face.put(blush, x, cheek, BLUSH)
    # 4.1.16: nothing under the hair (a face pixel its outer layer paints over, opaque): the game draws that layer half a pixel out and 9/8
    # the size, so red or a lid line under it peeks out round the hair in slivers (Touka's eye under her fringe turned, her cheek streaked).
    for overlay in (kakugan, glow, closed, blush):
        for y in range(8):
            for x in range(8):
                if face.at(x, y, 1)[3] >= 255:
                    face.put(overlay, x, y, (0, 0, 0, 0))
    kakugan.save(out / f"{key}_kakugan.png")
    glow.save(out / f"{key}_kakugan_glow.png")
    closed.save(out / f"{key}_closed.png")
    blush.save(out / f"{key}_blush.png")
    print(f"{key}: eyes right {sorted(eyes['right'])} left {sorted(eyes['left'])} (whites {sorted(eyes['white'])}, lashes {eyes['lash']}); "
          f"textures in "
          f"{out.relative_to(ROOT)}")
    if preview:
        sheet(key, skin, [kakugan, glow, closed, blush], preview)


def sheet(key, skin, overlays, folder):
    """Each face, big: the skin, turned, glowing, shut, blushing."""
    s = skin.size[0] // 64
    crop = lambda im: im.crop((8 * s, 8 * s, 16 * s, 16 * s))
    base = crop(skin)
    hat = skin.crop((40 * s, 8 * s, 48 * s, 16 * s))
    frames = [Image.alpha_composite(base, hat)]
    for overlay in overlays:
        frames.append(Image.alpha_composite(Image.alpha_composite(base, crop(overlay)), hat))
    big = 24 // s
    out = Image.new("RGBA", (len(frames) * 8 * s * big + (len(frames) + 1) * 8, 8 * s * big + 16), (40, 40, 46, 255))
    for i, frame in enumerate(frames):
        out.paste(frame.resize((8 * s * big, 8 * s * big), Image.NEAREST), (8 + i * (8 * s * big + 8), 8))
    os.makedirs(folder, exist_ok=True)
    out.save(os.path.join(folder, f"{key}-faces.png"))


# ---------------------------------------------------------------------------------------------------- stand-ins until the real skins

def box(image, u0, v0, w, h, d, paint):
    """Every pixel of a box's six faces, painted by paint(face, x, y, w_face, h_face) -> colour or None."""
    faces = {"top": (u0 + d, v0, w, d), "bottom": (u0 + d + w, v0, w, d), "right": (u0, v0 + d, d, h), "front": (u0 + d, v0 + d, w, h),
             "left": (u0 + d + w, v0 + d, d, h), "back": (u0 + 2 * d + w, v0 + d, w, h)}
    for name, (fu, fv, fw, fh) in faces.items():
        for y in range(fh):
            for x in range(fw):
                c = paint(name, x, y, fw, fh)
                if c is not None:
                    image.putpixel((fu + x, fv + y), tuple(c) + ((255,) if len(c) == 3 else ()))


def paint_placeholder(key, path):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    if key == "touka":
        skin, hair, shine, eye = (241, 220, 207), (44, 35, 64), (70, 56, 98), (62, 92, 146)
        shirt, vest, skirt, tights, shoe = (232, 232, 238), (30, 30, 36), (36, 34, 46), (32, 28, 36), (18, 18, 22)
        arm = 3
    else:
        skin, hair, shine, eye = (238, 217, 203), (226, 226, 232), (196, 196, 206), (78, 76, 86)
        shirt, vest, skirt, tights, shoe = (28, 28, 32), (24, 24, 28), (52, 52, 58), (52, 52, 58), (16, 16, 18)
        arm = 4

    def head(face, x, y, w, h):
        if face == "top":
            return shine if (x + y) % 5 == 0 else hair
        if face == "bottom":
            return skin
        if face == "back":
            return shine if x % 3 == 1 and y < 3 else hair
        if face in ("right", "left"):
            return hair if y < (7 if key == "touka" else 5) or x > 4 else skin
        # front
        if y <= 1:
            return shine if x % 3 == 0 else hair
        if key == "touka" and x <= 2 and y <= 3:
            return hair  # her bangs swept down over her right side
        if y in (2, 3) and x in (0, 7):
            return hair
        if y == 3 and x in (1, 2, 5, 6):
            return tuple(int(v * 0.55) for v in hair)  # brows and lashes
        if y == 4:
            return {1: (236, 236, 240), 2: eye, 5: eye, 6: (236, 236, 240)}.get(x, skin)
        if y == 5 and key == "touka":
            return {1: (236, 236, 240), 2: tuple(min(255, v + 40) for v in eye), 5: tuple(min(255, v + 40) for v in eye),
                    6: (236, 236, 240)}.get(x, skin)
        if y == 6 and x in (3, 4):
            return (210, 150, 150)
        return skin

    def body(face, x, y, w, h):
        if face in ("top", "bottom"):
            return vest
        if face == "front" and key == "touka":
            return shirt if 3 <= x <= 4 or y < 2 else vest
        if face == "front":
            return (60, 60, 66) if x == 3 and y > 1 else shirt
        return vest

    def arms(face, x, y, w, h):
        if face == "bottom":
            return skin
        if face == "top":
            return shirt if key == "touka" else vest
        if y >= 10:
            return skin
        return shirt if key == "touka" else vest

    def legs(face, x, y, w, h):
        if face == "bottom" or y >= 11:
            return shoe
        if key == "touka" and y < 4:
            return skirt
        return tights if face != "top" else skirt

    box(img, 0, 0, 8, 8, 8, head)
    box(img, 16, 16, 8, 12, 4, body)
    box(img, 40, 16, arm, 12, 4, arms)
    box(img, 32, 48, arm, 12, 4, arms)
    box(img, 0, 16, 4, 12, 4, legs)
    box(img, 16, 48, 4, 12, 4, legs)
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def main():
    preview = sys.argv[1] if len(sys.argv) > 1 else None
    for key, config in COMPANIONS.items():
        build(key, config, preview)


if __name__ == "__main__":
    main()
