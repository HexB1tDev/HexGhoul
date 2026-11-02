#!/usr/bin/env python3
"""1.3.76: shut eyes for the children's skins (the owner: "they should all blink like Touka, Eto and all the kids do"), and through them
the villagers' (each villager's skin is a child's with other colours: the same eyes in the same place, the same skin tone).

    python3 tools/blink_skins.py [preview-dir]

For each child skin (the game's own processed copies, textures/entity/child/skins/<key>.png: never the owner's files) it writes
<key>_closed.png (and, 1.3.77, <key>_blush.png: pink on the cheeks under the eyes) beside it: only the eyes' pixels, laid over the skin while it blinks or sleeps: each eye grown over from the skin round
it (keeping its shading), a thin lash line across its lower part, sagging a little ("- -", as Eto's and the companions' shut eyes). Writes
src/main/java/com/hex/kagune/control/family/ChildBlinks.java: the keys that have one (a skin whose eyes are already shut, or hidden by
its hair, has none and never blinks).

Each eye is boxed by hand (BOXES, looked at face by face on a large grid): what is not skin inside its boxes is shut. (1.3.76's first try
found the eyes by colour and filled them with one flat tone: the owner marked a dozen faces where that showed; it is gone.)
"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import child_skins as cs  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SKINS = ROOT / "src/main/resources/assets/hexkagune/textures/entity/child/skins"
JAVA = ROOT / "src/main/java/com/hex/kagune/control/family/ChildBlinks.java"

# Eyes already shut, or hidden under the hair (or behind see-through hair): no blink.
NONE = {"daughter_02", "daughter_04", "daughter_06", "daughter_07", "daughter_10", "daughter_11", "daughter_12", "daughter_20", "daughter_21", "daughter_22",
        "son_14"}
# Each eye by hand (looked at on a large grid, face by face): rectangles (x0, y0, x1, y1) in face pixels (0..8 across and down; the right
# eye is on the viewer's left) holding all of the eye (its lashes, white and iris) and none of the hair. Only what is not skin inside them
# is shut. A skin not listed here does not blink.
BOXES = {
    "daughter_01": ([(1, 4, 3, 7)], [(5, 4, 7, 7)]),
    "daughter_03": ([(1, 5, 3, 7), (2, 4, 3, 5)], [(5, 5, 7, 7), (5, 4, 6, 5)]),
    "daughter_05": ([(1.75, 5.5, 3, 7)], [(5, 4.5, 7.75, 7)]),
    "daughter_08": ([(1, 5, 3, 7.25)], [(5, 5, 7, 7.25)]),
    "daughter_09": ([(1, 6, 2, 7), (2, 5.5, 3.25, 7)], [(5, 4, 7, 7), (7, 4.5, 8, 7)]),
    "daughter_13": ([(0.5, 4.5, 3, 7)], [(5, 4.5, 7.5, 7)]),
    "daughter_14": ([(0, 4.5, 3, 7)], [(5, 4.5, 8, 7)]),
    "daughter_15": ([(0.5, 5, 3, 7)], [(5, 4, 8, 7)]),
    "daughter_16": ([(0.75, 4, 3.25, 7.25)], [(4.75, 4, 7.25, 7.25)]),
    "daughter_17": ([(1, 6, 3, 7)], [(5, 4, 8, 7)]),
    "daughter_18": ([(1, 5, 3, 7)], [(5, 5, 7, 7)]),
    "daughter_19": ([(1, 5, 3, 7), (2, 4, 3, 5)], [(5, 5, 7, 7), (5, 4, 6, 5)]),
    "daughter_23": ([(1, 4, 3.5, 7)], [(4.75, 4, 7.5, 7)]),
    "son_01": ([(1, 5, 3, 7)], [(5, 5, 7, 7)]),
    "son_02": ([(1, 5, 2, 6), (2, 4, 3, 6)], [(5, 4, 7, 6)]),
    "son_03": ([(1, 4, 3, 6)], [(5, 4, 7, 6)]),
    "son_04": ([(1, 5, 3, 7), (2, 4, 3, 5)], [(5, 5, 7, 7), (5, 4, 6, 5)]),
    "son_05": ([(1, 5, 3, 7)], [(5, 5, 7, 7)]),
    "son_06": ([(1, 4, 3, 6)], [(5, 5, 6, 6), (6, 4, 7, 6)]),
    "son_07": ([(1, 4, 3, 6)], [(5, 4, 7, 6)]),
    "son_08": ([(1, 5, 3, 7), (2, 4, 3, 5)], [(5, 5, 7, 7), (5, 4, 6, 5)]),
    "son_09": ([(1, 5, 3, 7)], [(5, 5, 7, 7)]),
    "son_10": ([(1, 5, 3, 7)], [(5, 5, 7, 7)]),
    "son_11": ([(1, 5, 3, 6)], [(5, 4, 7, 6)]),
    "son_12": ([(1, 5, 3, 7)], [(5, 5, 7, 7)]),  # the blood running down past the right eye is kept (KEEP)
    "son_13": ([(1, 5, 3, 7)], [(5, 5, 7, 7)]),
    "son_15": ([(1, 5, 3, 7)], [(5, 5, 7, 7)]),
    "son_16": ([(1, 4, 3, 6)], [(5, 4, 7, 6)]),
}
LASH = (46, 28, 30)
# Where the skin's tone is read, if not the lower face (face rows): son_11's lower face is darker than the skin round its eyes.
TONE_ROWS = {"son_11": (2.5, 4.5)}
# What stays inside an eye's box: son_12's blood, pure red.
KEEP = {"son_12": lambda c: c[0] >= 120 and c[1] <= 40 and c[2] <= 40}


def band_tone(face, top, bottom):
    """The commonest colour in face rows {@code top}..{@code bottom} across its middle (x 2..6)."""
    from collections import Counter
    s = face.s
    found = Counter()
    for y in range(int(top * s), int(bottom * s)):
        for x in range(2 * s, 6 * s):
            c = face.seen[y][x]
            if c[3] >= 200:
                found[cs.quant(c)] += 1
    q = found.most_common(1)[0][0]
    near = [face.seen[y][x][:3] for y in range(int(top * s), int(bottom * s)) for x in range(2 * s, 6 * s) if cs.quant(face.seen[y][x]) == q]
    return tuple(int(sum(c[i] for c in near) / len(near)) for i in range(3))


def white(c, tone):
    """An eye's white: bright and greyer than the skin by a clear margin."""
    return cs.lum(c) >= 120 and cs.sat(c) <= min(16, cs.sat(tone) - 12)


def skin(c, tone):
    return cs.skinlike(c, tone) or cs.blushlike(c, tone)


def eye_mask(face, tone, rects, keep=None):
    """The eye's native pixels: everything inside its hand-drawn rectangles (they hold the eye and no hair; any skin in them is grown back
    from the skin round it), but what {@code keep} says stays (son_12's blood running past its eye)."""
    s = face.s
    mask = set()
    for (x0, y0, x1, y1) in rects:
        for y in range(int(round(y0 * s)), int(round(y1 * s))):
            for x in range(int(round(x0 * s)), int(round(x1 * s))):
                c = face.seen[y][x]
                if 0 <= x < face.n and 0 <= y < face.n and c[3] >= 128 and not (keep and keep(c)):
                    mask.add((x, y))
    return mask


def inpaint(face, tone, mask):
    """{(x, y): (r, g, b)}: the eye grown over from the skin round it, a ring at a time (each pixel the mean of its filled or skin
    neighbours), so the lid keeps the skin's own shading; only where no skin reaches, from whatever is round it."""
    n = face.n
    filled = {}

    def grow(known):
        todo = set(p for p in mask if p not in filled)
        while todo:
            ring = {}
            for (x, y) in todo:
                got = []
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        q = (x + dx, y + dy)
                        if q == (x, y) or not (0 <= q[0] < n and 0 <= q[1] < n):
                            continue
                        if q in filled:
                            got.append(filled[q])
                        elif q not in mask and known(face.seen[q[1]][q[0]]):
                            got.append(face.seen[q[1]][q[0]][:3])
                if got:
                    ring[(x, y)] = tuple(int(sum(c[i] for c in got) / len(got)) for i in range(3))
            if not ring:
                return
            filled.update(ring)
            todo -= set(ring)

    grow(lambda c: c[3] >= 128 and cs.skinlike(c, tone) and not white(c, tone))
    grow(lambda c: c[3] >= 128)
    for p in mask:
        filled.setdefault(p, tuple(tone))
    return filled


def shut(face, tone, mask):
    """{(x, y): colour}: the eye filled with the skin round it, a lash line across it in its lower part, sagging a little in the middle
    and thinner at its ends ("- -", as Eto's and the companions' shut eyes)."""
    s = face.s
    if not mask:
        return {}
    out = {p: c + (255,) for p, c in inpaint(face, tone, mask).items()}
    x0, x1 = min(p[0] for p in mask), max(p[0] for p in mask)
    y0, y1 = min(p[1] for p in mask), max(p[1] for p in mask)
    dark = min((face.seen[p[1]][p[0]] for p in mask), key=cs.lum)
    # As dark as the eye's own darkest (its lashes), but never its colour (a dark green or red iris): a neutral, warm dark.
    deep = min(cs.lum(dark), cs.lum(LASH))
    lash = (int(max(12, deep * 1.15)), int(max(8, deep * 0.7)), int(max(9, deep * 0.75)))
    width = x1 - x0 + 1
    height = y1 - y0 + 1
    thick = max(1.0, 0.36 * s)
    middle = y0 + 0.68 * height
    sag = 0.12 * s if s >= 8 else 0.0  # a skin of 4 pixels or fewer a face pixel: straight (a one-pixel step looks crooked)
    for x in range(x0, x1 + 1):
        u = (x - x0 + 0.5) / width
        t = thick * (0.65 + 0.35 * math.sin(math.pi * u)) if s >= 8 else thick
        centre = middle + sag * math.sin(math.pi * u)
        top = int(round(centre - t / 2.0))
        for y in range(top, top + max(1, int(round(t)))):
            if y0 <= y <= y1:
                out[(x, y)] = lash + (255,)
    return out


def blush(face, tone, mask):
    """{(x, y): colour}: a soft pink oval on the cheek under the eye (1.3.77: villagers blush), only where it is the skin."""
    s = face.s
    if not mask:
        return {}
    x0, x1 = min(p[0] for p in mask), max(p[0] for p in mask)
    y1 = max(p[1] for p in mask)
    cx = (x0 + x1 + 1) / 2.0
    cy = y1 + 1 + 0.45 * s
    rx = max(0.42 * (x1 - x0 + 1), 1.0)
    ry = max(0.34 * s, 0.6)
    out = {}
    for y in range(int(cy - ry - 1), int(cy + ry + 2)):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            if not (0 <= x < face.n and 0 <= y < face.n):
                continue
            d = ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2
            c = face.seen[y][x]
            if d >= 1.0 or c[3] < 128 or not cs.skinlike(c, tone) and not cs.blushlike(c, tone):
                continue
            out[(x, y)] = (255, 104, 128, int(170 * (1.0 - d) ** 0.6))
    return out


def main():
    preview = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    keys = sorted(p.stem for p in SKINS.glob("*.png") if p.stem.split("_")[-1].isdigit())
    made = []
    tiles = []
    for key in keys:
        target = SKINS / (key + "_closed.png")
        if key in NONE or key not in BOXES:
            for old in (target, SKINS / (key + "_blush.png")):
                if old.exists():
                    old.unlink()
            continue
        image = Image.open(SKINS / (key + ".png")).convert("RGBA")
        w = image.size[0]
        s = w // 64
        face = cs.Face(image)
        tone = cs.tone_of(face)
        if key in TONE_ROWS:
            tone = band_tone(face, *TONE_ROWS[key])
        overlay = Image.new("RGBA", (w, w), (0, 0, 0, 0))
        cheeks = Image.new("RGBA", (w, w), (0, 0, 0, 0))
        found = 0
        for side in (0, 1):
            mask = eye_mask(face, tone, BOXES[key][side], KEEP.get(key))
            for (x, y), colour in shut(face, tone, mask).items():
                on_hat = face.hat[y][x][3] > 0  # a see-through hat pixel too: it would tint the shut eye
                overlay.putpixel(((40 if on_hat else 8) * s + x, 8 * s + y), colour)
            for (x, y), colour in blush(face, tone, mask).items():
                cheeks.putpixel(((40 if face.hat[y][x][3] > 0 else 8) * s + x, 8 * s + y), colour)
            found += bool(mask)
        if found == 0:
            if target.exists():
                target.unlink()
            print(f"{key}: no eyes found")
            continue
        overlay.save(target, optimize=True)
        cheeks.save(SKINS / (key + "_blush.png"), optimize=True)
        made.append(key)
        if preview is not None:
            def face_img(img):
                front = img.crop((8 * s, 8 * s, 16 * s, 16 * s))
                front.alpha_composite(img.crop((40 * s, 8 * s, 48 * s, 16 * s)))
                return front.resize((96, 96), Image.NEAREST)
            closed = image.copy()
            closed.alpha_composite(overlay)
            tiles.append((key, face_img(image), face_img(closed)))
        print(f"{key}: {found} eye(s)")
    rows = ",\n".join(f'        "{k}"' for k in made)
    JAVA.write_text(f'''package com.hex.kagune.control.family;

/**
 * 1.3.76: the children's skins that can shut their eyes (pure; written by {{@code tools/blink_skins.py}}: do not edit by hand): each has
 * {{@code <key>_closed.png}} beside it. A skin whose eyes are already shut, or hidden by its hair, has none.
 */
public final class ChildBlinks {{
    private ChildBlinks() {{
    }}

    private static final java.util.Set<String> KEYS = java.util.Set.of(
{rows});

    /** True if skin {{@code key}} has its eyes-shut overlay. */
    public static boolean has(String key) {{
        return key != null && KEYS.contains(key);
    }}

    /** 1.3.77: its blush (pink on the cheeks under the eyes), for a skin that {{@link #has}} shut eyes. */
    public static String blush(String key) {{
        return "textures/entity/child/skins/" + key + "_blush.png";
    }}

    /** The overlay's texture path (the mod's), for a skin that {{@link #has}} one. */
    public static String closed(String key) {{
        return "textures/entity/child/skins/" + key + "_closed.png";
    }}
}}
''')
    print(f"{len(made)} of {len(keys)} skins blink")
    if preview is not None:
        from PIL import ImageDraw
        preview.mkdir(parents=True, exist_ok=True)
        cols = 8
        sheet = Image.new("RGBA", (cols * 200, ((len(tiles) + cols - 1) // cols) * 112), (50, 50, 60, 255))
        d = ImageDraw.Draw(sheet)
        for i, (key, a, b) in enumerate(tiles):
            x, y = (i % cols) * 200, (i // cols) * 112
            sheet.alpha_composite(a, (x + 2, y + 2))
            sheet.alpha_composite(b, (x + 100, y + 2))
            d.text((x + 4, y + 99), key, fill=(255, 255, 255, 255))
        sheet.save(preview / "blinks.png")


if __name__ == "__main__":
    main()
