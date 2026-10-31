#!/usr/bin/env python3
"""1.3.18: the children's skins, from the owner's own (art/children/sons, art/children/daughters: HD skins, 64 to 1024 pixels across).

    python3 tools/child_skins.py [preview-dir]

The owner picked these skins for the sons and daughters only. Each is kept as it is drawn; only what Minecraft itself does to a player's skin
is done to it: an old 2:1 skin is made square (its left arm and leg mirrored from the right), the base layer made opaque where vanilla makes
it opaque. For each, under src/main/resources/assets/hexkagune/textures/entity/child/skins/:

  <key>.png            the skin
  <key>_kr.png         its right eye turned (the character's right: the viewer's left on the face): only the eye's pixels, laid over the
                       skin as it turns: the white black, the iris red (as light or dark as it was), lashes, outlines and highlights kept
  <key>_kl.png         its left eye turned the same way
  <key>_gr.png         the right eye's red iris alone, drawn full bright
  <key>_gl.png         the left eye's

and src/main/java/com/hex/kagune/control/family/ChildSkins.java: every skin's key, girl or boy, slim arms or wide, and its skin tone (read off
its face), which a child's is picked by (its tone from its parents', ChildSkins.pick).

Eyes are found on the face front (base and hat as they are seen) at the skin's own resolution: in the eye rows, what is neither skin nor hair
nor blush; whites are bright and grey, small whites inside the iris are highlights (kept), the darkest pixels are lashes and outlines (kept),
the rest is iris. A skin whose eyes come out wrong gets its eye box in BOXES below.
"""
import os
import sys
from collections import Counter
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art" / "children"
OUT = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "entity" / "child" / "skins"
JAVA = ROOT / "src" / "main" / "java" / "com" / "hex" / "kagune" / "control" / "family" / "ChildSkins.java"

GROUPS = (("sons", "son", False), ("daughters", "daughter", True))

# Each eye by hand (looked at up close on every skin): its white ("w") and its iris ("i") as rectangles (x0, y0, x1, y1) in face pixels
# (0..8 across the face front, 0..8 down; fractions for HD skins; the right eye is on the viewer's left). In the white, what is pale turns
# black; in the iris, everything (but skin, blush and pure black) turns red, shaded by its own light and dark, the top of it a little darker
# (the lid's shadow), a small white wholly inside it kept as a glint. Lashes, outlines and hair outside the rectangles are never touched.
def E(w=(), i=()):
    return {"w": list(w), "i": list(i)}


EYES = {
    # daughters
    "3d-girl-hd-skin.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "anime-cactus-girl-hd.png": (E(i=[(1.25, 5.5, 2.75, 6.75)]), E(i=[(5.25, 5.5, 6.75, 6.75)])),
    "anime-cat-girl-hd-not-mine-copy.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "anime-hd-girl-green.png": (E(w=[(1.1, 6.45, 2.0, 7.0)], i=[(2.0, 6.4, 2.95, 7.0)]), E(i=[(5.0, 6.4, 5.95, 7.0)], w=[(5.95, 6.45, 6.9, 7.0)])),
    "cool-hd-girl-for-me.png": (E(i=[(2, 5.5, 2.75, 6.25)]), E(i=[(5.5, 5, 6.5, 6.25)], w=[(5, 5, 5.5, 5.5), (6, 6.25, 7, 7)])),
    "hd-anime-girl.png": (E(w=[(1.1, 6.4, 2.0, 7.0)], i=[(2.0, 6.35, 2.95, 7.0)]), E(i=[(5.0, 6.35, 5.95, 7.0)], w=[(5.95, 6.4, 6.9, 7.0)])),
    "hd-cat-girl.png": (E(i=[(1, 5, 3, 6), (2, 6, 3, 6.5)], w=[(1, 6, 2, 7)]), E(i=[(5, 5, 7, 6), (5, 6, 6, 6.5)], w=[(6, 6, 7, 7)])),
    "hd-cute-girl.png": (E(i=[(1.75, 5.75, 3, 7)]), E(i=[(5.5, 5, 6.5, 6.25), (5, 6.25, 6, 7)], w=[(5, 5, 5.5, 5.5), (6, 6, 7, 7)])),
    "hd-cute-white-demon-girl.png": (E(w=[(1, 6, 2.5, 7)], i=[(1.5, 6, 2, 7)]), E(w=[(5, 6, 6.5, 7)], i=[(5.5, 6, 6, 7)])),
    "hd-egirl-skin.png": (E(i=[(1.5, 5.25, 2.75, 6.25)]), E(i=[(5.25, 5.25, 6.5, 6.25)])),
    "hd-maid-cat-girl-so-cute-love.png": (E(i=[(1.5, 5.75, 3, 6.5), (2, 6.5, 3, 7)], w=[(1, 6.5, 2, 7)]),
                                          E(i=[(5, 5.75, 6.5, 6.5), (5, 6.5, 6, 7)], w=[(6, 6.5, 7, 7)])),
    "hd-new-wedding-dress.png": (E(w=[(1, 6.25, 2, 7)], i=[(2, 6.25, 3, 7)]), E(i=[(5, 6.25, 6, 7)], w=[(6, 6.25, 7, 7)])),
    "hd-wolf-girl(1).png": (E(w=[(1, 6, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5.5, 7, 7)])),
    "hd-wolf-girl-fixed.png": (E(i=[(1, 5, 2.5, 6), (2, 6, 2.5, 7)], w=[(1, 6, 2, 7)]), E(i=[(5.5, 5, 6.75, 6), (5.5, 6, 6, 7)], w=[(6, 6, 7, 7)])),
    "hd-wolf-girl.png": (E(w=[(2, 6.5, 2.5, 7)], i=[(2.5, 6, 3, 7)]), E(i=[(5, 5.5, 6, 7), (5.5, 5, 6, 5.5)], w=[(5, 5, 5.5, 5.5), (6, 5, 7, 7)])),
    "kitty-cat-girl-alex-hd.png": (E(w=[(1, 6, 2, 7)], i=[(2, 6, 3, 7)]), E(i=[(5, 6, 6, 7)], w=[(6, 6, 7, 7)])),
    "melanie-martinez-hd.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "other-one-hd.png": (E(i=[(2, 5.5, 3, 6.5)], w=[(1, 6.25, 2.5, 7)]), E(i=[(5, 5.5, 6, 6.5)], w=[(5.5, 6.25, 7, 7)])),
    "shinobu-kocho-hd.png": (E(w=[(1, 6.5, 2, 7)], i=[(2, 6.5, 3, 7)]), E(i=[(5, 6.5, 6, 7)], w=[(6, 6.5, 7, 7)])),
    "the-hd-skin-girl-lol.png": (E(w=[(1.25, 5.25, 2, 6.75)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5.25, 6.75, 6.75)])),
    # sons
    "boy-hd-1.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "boy-hd-123.png": (E(w=[(1, 5, 2, 6)], i=[(2, 4, 3, 6)]), E(i=[(5, 4, 6, 6)], w=[(6, 4, 7, 6)])),
    "boy-hd-skin-armor.png": (E(w=[(1, 4, 2, 6)], i=[(2, 4, 3, 6)]), E(i=[(5, 4, 6, 6)], w=[(6, 4, 7, 6)])),
    "cute-boy-hd.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "dark-boy-hd.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "gamer-boy-hd.png": (E(w=[(1, 4, 2, 6)], i=[(2, 4, 3, 6)]), E(i=[(5, 5, 6, 6)], w=[(6, 4, 7, 6)])),
    "hd-alpha-wolf-boy.png": (E(w=[(1, 4, 2, 6)], i=[(2, 4, 3, 6)]), E(i=[(5, 4, 6, 6)], w=[(6, 4, 7, 6)])),
    "hd-boy-cute.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "hd-boy-pro.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "hd-boy-skin.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "hd-boy.png": (E(w=[(1, 5, 2, 6)], i=[(2, 5, 3, 6)]), E(i=[(5, 4.5, 6, 6)], w=[(6, 5, 7, 6)])),
    "hd-fox-boy.png": (E(w=[(0.9, 5.4, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5.25, 7.1, 7)])),
    "hd-green-boy.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "hd-skin-ite.png": (E(w=[(1.0, 6.4, 2.0, 7.0)], i=[(2.0, 6.0, 2.95, 7.0)]), E(i=[(5.0, 6.0, 5.95, 7.0)], w=[(6.0, 6.4, 7.0, 7.0)])),
    "skin-boy-hd.png": (E(w=[(1, 5, 2, 7)], i=[(2, 5, 3, 7)]), E(i=[(5, 5, 6, 7)], w=[(6, 5, 7, 7)])),
    "skin-hd-boy.png": (E(w=[(1, 5, 2, 6)], i=[(2, 4, 3, 6)]), E(i=[(5, 4, 6, 6)], w=[(6, 5, 7, 6)])),
}
# Skins whose eyes can't be seen (hair over them, or only lashes drawn): they show no kakugan rather than one painted over their hair.
HIDDEN = {"hd-black-and-white-cloud-girl.png", "hd-emo-girl.png", "rainbow-aesthetic-girl-hd-3.png"}

BLACK = (14, 4, 8)
DEEP = (92, 0, 12)
BRIGHT = (236, 30, 44)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def sat(c):
    return max(c[:3]) - min(c[:3])


def dist(a, b):
    return sum((a[i] - b[i]) ** 2 for i in range(3)) ** 0.5


def over(a, b):
    """b laid over a (RGBA tuples)."""
    ba = b[3] / 255.0
    if ba >= 1.0:
        return b
    if ba <= 0.0:
        return a
    aa = a[3] / 255.0 * (1.0 - ba)
    out = ba + aa
    if out <= 0.0:
        return (0, 0, 0, 0)
    return tuple(int(round((b[i] * ba + a[i] * aa) / out)) for i in range(3)) + (int(round(out * 255)),)


# ------------------------------------------------------------------ Minecraft's own handling of a player's skin

def square(image):
    """An old 64 x 32 layout (at any scale) made 64 x 64 as Minecraft makes it: the left leg and arm copied from the right, mirrored."""
    w, h = image.size
    if h == w:
        return image
    s = w // 64
    out = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    out.paste(image, (0, 0))

    def copy(x, y, dx, dy, cw, ch):
        region = image.crop((x * s, y * s, (x + cw) * s, (y + ch) * s)).transpose(Image.FLIP_LEFT_RIGHT)
        out.paste(region, ((x + dx) * s, (y + dy) * s))

    copy(4, 16, 16, 32, 4, 4)
    copy(8, 16, 16, 32, 4, 4)
    copy(0, 20, 24, 32, 4, 12)
    copy(4, 20, 16, 32, 4, 12)
    copy(8, 20, 8, 32, 4, 12)
    copy(12, 20, 16, 32, 4, 12)
    copy(44, 16, -8, 32, 4, 4)
    copy(48, 16, -8, 32, 4, 4)
    copy(40, 20, 0, 32, 4, 12)
    copy(44, 20, -8, 32, 4, 12)
    copy(48, 20, -16, 32, 4, 12)
    copy(52, 20, -8, 32, 4, 12)
    return out


def slim(image):
    """Slim arms: the right arm's outer column (64px x 54..55, rows 20..31) left empty, as an Alex-style skin has it."""
    s = image.size[0] // 64
    px = image.load()
    empty = total = 0
    for y in range(20 * s, 32 * s):
        for x in range(54 * s, 56 * s):
            total += 1
            if px[x, y][3] == 0:
                empty += 1
    return empty >= total * 0.9


def no_alpha(image):
    """Vanilla's setNoAlpha over the base layer (head 0..32 x 0..16, body and right limbs 0..64 x 16..32, left limbs 16..48 x 48..64)."""
    s = image.size[0] // 64
    px = image.load()
    for x0, y0, x1, y1 in ((0, 0, 32, 16), (0, 16, 64, 32), (16, 48, 48, 64)):
        for y in range(y0 * s, y1 * s):
            for x in range(x0 * s, x1 * s):
                c = px[x, y]
                if c[3] != 255:
                    px[x, y] = (c[0], c[1], c[2], 255)


# ------------------------------------------------------------------ reading the face

class Face:
    """The face front as seen (base with the hat over it), at the skin's own pixels."""

    def __init__(self, image):
        self.s = image.size[0] // 64
        self.n = 8 * self.s
        px = image.load()
        self.base = [[px[8 * self.s + x, 8 * self.s + y] for x in range(self.n)] for y in range(self.n)]
        self.hat = [[px[40 * self.s + x, 8 * self.s + y] for x in range(self.n)] for y in range(self.n)]
        self.seen = [[over(self.base[y][x], self.hat[y][x]) for x in range(self.n)] for y in range(self.n)]


def quant(c, step=12):
    return tuple(int(v) // step * step + step // 2 for v in c[:3])


def tone_of(face):
    """The skin's tone: the commonest colour of the lower face's middle on the base layer (under any hair the hat lays over it)."""
    n, s = face.n, face.s
    bins = Counter()
    members = {}
    for y in range(int(5.5 * s), n):
        for x in range(int(2 * s), n - int(2 * s)):
            c = face.base[y][x]
            if c[3] < 200 or lum(c) < 60:
                continue
            q = quant(c)
            bins[q] += 1
            members.setdefault(q, []).append(c)
    if not bins:
        return (230, 190, 160)
    best = bins.most_common(1)[0][0]
    cs = members[best]
    return tuple(int(sum(c[i] for c in cs) / len(cs)) for i in range(3))


def hair_of(face):
    """The hair's colours: what covers the top of the face (its first row and a half), clustered."""
    s = face.s
    found = Counter()
    for y in range(0, max(1, int(1.5 * s))):
        for x in range(face.n):
            c = face.seen[y][x]
            if c[3] >= 128:
                found[quant(c, 20)] += 1
    total = sum(found.values()) or 1
    return [q for q, k in found.items() if k >= total * 0.04]


def skinlike(c, tone):
    """The skin, lit or shaded: the same hue, from a little darker to a little lighter."""
    if dist(c, tone) < 34:
        return True
    st = sum(tone) or 1
    sc = sum(c[:3]) or 1
    chroma = sum(abs(c[i] / sc - tone[i] / st) for i in range(3))
    k = lum(c) / max(1.0, lum(tone))
    return chroma < 0.05 and 0.6 < k < 1.2


def blushlike(c, tone):
    return c[0] > c[2] + 25 and c[0] > c[1] + 35 and lum(c) > 120 and dist(c, tone) < 110


def inside(x, y, s, rects):
    """Native pixel (x, y) of the face (scale s) in any of the rectangles (face pixels)."""
    cx, cy = (x + 0.5) / s, (y + 0.5) / s
    return any(r[0] <= cx < r[2] and r[1] <= cy < r[3] for r in rects)


def eye_roles(face, tone, spec):
    """{(x, y): "white" | "iris" | "glint"} for one eye's spec (its white and iris rectangles)."""
    s = face.s
    roles = {}
    for y in range(face.n):
        for x in range(face.n):
            in_w = inside(x, y, s, spec["w"])
            in_i = inside(x, y, s, spec["i"])
            if not in_w and not in_i:
                continue
            c = face.seen[y][x]
            # Inside a rectangle drawn round the eye only the skin's own colour is skin (on a pale skin a grey white is not skin).
            if c[3] < 128 or dist(c, tone) < 22 or in_w and not in_i and blushlike(c, tone) and dist(c, tone) < 60:
                continue
            l = lum(c)
            if in_i:
                if l < 22:
                    continue  # a pure black pupil or outline stays black
                roles[(x, y)] = "white" if (l >= 200 and sat(c) <= 18) else "iris"
            elif l >= 120 and sat(c) <= 70:
                roles[(x, y)] = "white"
            elif l >= 50 and sat(c) > 70:
                roles[(x, y)] = "iris"
    # A white wholly inside the iris (nothing but iris or black round it), small against the iris, is a glint: kept.
    irises = sum(1 for k in roles.values() if k == "iris")
    done = set()
    for p, k in list(roles.items()):
        if k != "white" or p in done or not inside(p[0], p[1], s, spec["i"]):
            continue
        comp, stack = [], [p]
        done.add(p)
        while stack:
            q = stack.pop()
            comp.append(q)
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    r = (q[0] + dx, q[1] + dy)
                    if roles.get(r) == "white" and r not in done and inside(r[0], r[1], s, spec["i"]):
                        done.add(r)
                        stack.append(r)
        enclosed = True
        for q in comp:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    r = (q[0] + dx, q[1] + dy)
                    if r in comp:
                        continue
                    if not (0 <= r[0] < face.n and 0 <= r[1] < face.n):
                        enclosed = False
                        continue
                    if roles.get(r) == "iris" or lum(face.seen[r[1]][r[0]]) < 22 or roles.get(r) == "white" and not inside(r[0], r[1], s, spec["w"]):
                        continue
                    if not inside(r[0], r[1], s, spec["i"]):
                        continue  # the iris rectangle's edge: whatever is past it decides nothing
                    enclosed = False
        if enclosed and len(comp) <= max(2, 0.35 * irises):
            for q in comp:
                roles[q] = "glint"
    return roles


def paint_eye(face, roles):
    """Each turned pixel's colour (RGBA): the white black (its own light kept a little), the iris red by its own light and dark."""
    irises = [(p, face.seen[p[1]][p[0]]) for p, k in roles.items() if k == "iris"]
    if irises:
        lo = min(lum(c) for p, c in irises)
        hi = max(lum(c) for p, c in irises)
        top = min(p[1] for p, c in irises)
        bottom = max(p[1] for p, c in irises)
    out = {}
    for p, kind in roles.items():
        c = face.seen[p[1]][p[0]]
        if kind == "white":
            k = 0.75 + 0.5 * (lum(c) / 255.0)
            out[p] = tuple(min(255, int(v * k)) for v in BLACK) + (255,)
        elif kind == "iris":
            t = 0.5 if hi - lo < 1 else (lum(c) - lo) / (hi - lo)
            t = 0.25 + 0.75 * t ** 0.8
            colour = [DEEP[i] + (BRIGHT[i] - DEEP[i]) * t for i in range(3)]
            if bottom > top and (p[1] - top) / (bottom - top + 1) < 0.25:
                colour = [v * 0.72 for v in colour]  # the lid's shadow over the top of the iris
            out[p] = tuple(int(v) for v in colour) + (255,)
    return out


def build_one(path, key, girl, preview):
    raw = Image.open(path).convert("RGBA")
    image = square(raw)
    is_slim = slim(image)
    no_alpha(image)
    w = image.size[0]
    s = w // 64
    face = Face(image)
    tone = tone_of(face)
    hair = hair_of(face)
    spec = EYES.get(path.name)
    if path.name in HIDDEN or spec is None:
        eyes = {"r": {}, "l": {}}
    else:
        eyes = {"r": eye_roles(face, tone, spec[0]), "l": eye_roles(face, tone, spec[1])}
    OUT.mkdir(parents=True, exist_ok=True)
    image.save(OUT / (key + ".png"), optimize=True)
    found = {}
    for side, eye in eyes.items():
        turn = Image.new("RGBA", (w, w), (0, 0, 0, 0))
        glow = Image.new("RGBA", (w, w), (0, 0, 0, 0))
        colours = paint_eye(face, eye)
        for (x, y), colour in colours.items():
            on_hat = face.hat[y][x][3] >= 128
            u = (40 if on_hat else 8) * s + x
            v = 8 * s + y
            turn.putpixel((u, v), colour)
            if eye[(x, y)] == "iris":
                glow.putpixel((u, v), colour)
        turn.save(OUT / (key + "_k" + side + ".png"), optimize=True)
        glow.save(OUT / (key + "_g" + side + ".png"), optimize=True)
        found[side] = (sum(1 for k in eye.values() if k == "white"), sum(1 for k in eye.values() if k == "iris"))
        if preview is not None:
            eyes[side] = (eye, turn)
    if preview is not None:
        sheet = Image.new("RGBA", (3 * 192 + 16, 192 + 8), (40, 40, 48, 255))
        n = face.n

        def face_img(img):
            front = img.crop((8 * s, 8 * s, 16 * s, 16 * s))
            hat = img.crop((40 * s, 8 * s, 48 * s, 16 * s))
            front.alpha_composite(hat)
            return front.resize((192, 192), Image.NEAREST)

        both = image.copy()
        for side in ("r", "l"):
            both.alpha_composite(eyes[side][1])
        right_only = image.copy()
        right_only.alpha_composite(eyes["r"][1])
        sheet.alpha_composite(face_img(image), (4, 4))
        sheet.alpha_composite(face_img(both), (200, 4))
        sheet.alpha_composite(face_img(right_only), (396, 4))
        sheet.save(Path(preview) / (key + ".png"))
    return {"key": key, "girl": girl, "slim": is_slim, "tone": tone, "source": path.name, "eyes": found, "size": raw.size}


def write_java(skins):
    lines = []
    for sk in skins:
        r, g, b = sk["tone"]
        lines.append('        new Skin("%s", %s, %s, 0x%02X%02X%02X), // %s, %dx%d' % (
            sk["key"], "true" if sk["girl"] else "false", "true" if sk["slim"] else "false", r, g, b, sk["source"], sk["size"][0], sk["size"][1]))
    body = "\n".join(lines)
    java = JAVA.read_text() if JAVA.exists() else None
    start = "    // <generated by tools/child_skins.py>\n"
    end = "    // </generated>\n"
    table = start + "    public static final Skin[] ALL = {\n" + body + "\n    };\n" + end
    if java is None or start not in java:
        raise SystemExit("ChildSkins.java has no generated block: " + str(JAVA))
    head, rest = java.split(start, 1)
    _, tail = rest.split(end, 1)
    JAVA.write_text(head + table + tail)


def main():
    preview = sys.argv[1] if len(sys.argv) > 1 else None
    if preview:
        os.makedirs(preview, exist_ok=True)
    for old in OUT.glob("*.png") if OUT.exists() else []:
        old.unlink()
    skins = []
    for folder, prefix, girl in GROUPS:
        files = sorted(p for p in (ART / folder).glob("*.png"))
        for i, path in enumerate(files):
            key = "%s_%02d" % (prefix, i + 1)
            sk = build_one(path, key, girl, preview)
            skins.append(sk)
            print("%-12s %-44s %4dx%-4d slim=%-5s tone=%s eyes r=%s l=%s" % (key, path.name, sk["size"][0], sk["size"][1], sk["slim"],
                                                                              "#%02x%02x%02x" % sk["tone"], sk["eyes"]["r"], sk["eyes"]["l"]))
    write_java(skins)
    if preview:
        tiles = sorted(Path(preview).glob("*_[0-9][0-9].png"))
        cols = 3
        sheet = Image.new("RGBA", (cols * 600, ((len(tiles) + cols - 1) // cols) * 216), (20, 20, 24, 255))
        from PIL import ImageDraw
        d = ImageDraw.Draw(sheet)
        for i, t in enumerate(tiles):
            x, y = (i % cols) * 600, (i // cols) * 216
            sheet.alpha_composite(Image.open(t), (x, y))
            d.text((x + 4, y + 200), t.stem, fill=(255, 255, 255, 255))
        sheet.save(Path(preview) / "all.png")


if __name__ == "__main__":
    main()
