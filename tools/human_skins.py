#!/usr/bin/env python3
"""1.3.75: the villagers' looks (the owner: "make the humans as cute looking as possible you can also adapt skins I gave you for the kids as
long as you make sure they are differently looking from the kids, such as adapting hair color, clothes"): ten of the children's skins
(the game's own copies, textures/entity/child/skins: never the owner's files) with their hair, clothes and eyes turned to other colours
(each its own turn of the hue, and its own lightness), the skin, the blush, the whites and the blacks left as they are.

    python3 tools/human_skins.py

Writes src/main/resources/assets/hexkagune/textures/entity/human/human_NN.png and
src/main/java/com/hex/kagune/control/village/HumanSkins.java (each skin's path, girl or boy, slim arms or wide).
"""
import colorsys
import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
CHILD = ROOT / "src/main/resources/assets/hexkagune/textures/entity/child/skins"
OUT = ROOT / "src/main/resources/assets/hexkagune/textures/entity/human"
TABLE = ROOT / "src/main/java/com/hex/kagune/control/village/HumanSkins.java"
SKINS = ROOT / "src/main/java/com/hex/kagune/control/family/ChildSkins.java"
# (child skin, hue turn in degrees, lightness factor for what is turned)
PICKS = [("daughter_21", 150, 1.0), ("daughter_08", 220, 1.05), ("daughter_18", 300, 1.0), ("daughter_19", 90, 0.95), ("daughter_13", 40, 1.1),
         ("son_04", 200, 0.95), ("son_08", 120, 1.0), ("son_12", 260, 0.9), ("son_06", 330, 1.0), ("son_13", 60, 1.05)]


def skin_like(h, s, v, tone, face):
    """Skin (its own tone, lit or shaded) and a blush on it: kept as they are; hair, clothes and eyes are not. On the face (the head's
    own layer) a wider match: its shading strays further from the tone, and a turned cheek shows at once."""
    th, ts, tv = tone
    dh = min(abs(h - th), 1.0 - abs(h - th)) * 360.0
    blush = (h * 360.0 >= 340.0 or h * 360.0 <= 12.0) and abs(s - ts) <= 0.25 and v >= tv * 0.7
    if face:
        return dh <= 24.0 and abs(s - ts) <= 0.24 and v >= tv * 0.4 or blush and dh <= 30.0
    return dh <= 14.0 and abs(s - ts) <= 0.16 and v >= tv * 0.45 or blush and dh <= 30.0


def turn(img, degrees, light, tone, greys=0.0):
    px = img.load()
    w, h = img.size
    k = w // 64
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            face = x < 32 * k and y < 16 * k
            if greys > 0.0 and (s < 0.12 or s < 0.3 and not skin_like(hh, s, v, tone, face)) and 0.25 <= v <= 0.8 \
                    and not skin_like(hh, s, v, tone, face):
                # 2.0: a mid grey or a dull colour (a grey or blue-grey hoodie, grey hair) tinted with the new colour; whites, blacks and eye
                # whites stay
                nr, ng, nb = colorsys.hsv_to_rgb((degrees / 360.0) % 1.0, greys, min(1.0, v * light))
                px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
                continue
            if s < 0.12 or skin_like(hh, s, v, tone, face):
                continue  # whites, blacks and greys, and skin, kept
            hh = (hh + degrees / 360.0) % 1.0
            v = min(1.0, v * light)
            nr, ng, nb = colorsys.hsv_to_rgb(hh, s, v)
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return img


def changed(a, b):
    """The share of the skin's drawn pixels that differ between {@code a} and {@code b} (by more than a little)."""
    pa, pb = a.load(), b.load()
    w, h = a.size
    drawn = differ = 0
    for y in range(0, h, max(1, w // 64)):
        for x in range(0, w, max(1, w // 64)):
            c, d = pa[x, y], pb[x, y]
            if c[3] < 128:
                continue
            drawn += 1
            differ += abs(c[0] - d[0]) + abs(c[1] - d[1]) + abs(c[2] - d[2]) > 60
    return differ / max(1, drawn)


# 2.0 (the owner: "Add more humans ... make sure there are no 2 of the same look per village"): more looks, from every child skin that
# blinks (ChildBlinks: so every new villager blinks), two turns of each, kept only if at least this share of the skin changed (never
# a child of the player's in all but name).
MORE_TURNS = 2
DIFFERENT = 0.30
# Looked at on the previews and dropped: daughter_01's second (keeps the child's mint hair), son_08's (son_04's skin with a smile: the
# same as son_04's new ones), son_13's second (the same teal hoodie as human_09).
DROP = {("daughter_01", 1), ("son_08", 0), ("son_08", 1), ("son_13", 1)}  # (2.0: mid greys tinted too, so a grey hoodie becomes a coloured one)


def blinking():
    java = (ROOT / "src/main/java/com/hex/kagune/control/family/ChildBlinks.java").read_text()
    return re.findall(r'"((?:daughter|son)_\d\d)"', java)


def main():
    table = {m.group(1): (m.group(2) == "true", m.group(3) == "true", int(m.group(4), 16))
             for m in re.finditer(r'new Skin\("(\w+)", (true|false), (true|false), 0x([0-9A-Fa-f]{6})', SKINS.read_text())}
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("human_*.png"):
        old.unlink()
    picks = list(PICKS)
    for n, key in enumerate(blinking()):
        first = (97 + 61 * n) % 360
        for k in range(MORE_TURNS):
            if (key, k) in DROP:
                continue
            degrees = (first + 150 * k) % 360
            if degrees < 40 or degrees > 320:
                degrees = (degrees + 90) % 360  # too near no turn at all
            picks.append((key, degrees, (0.95, 1.05, 1.0, 0.9)[(n + k) % 4]))
    rows = []
    made = 0
    for key, degrees, light in picks:
        source = Image.open(CHILD / f"{key}.png").convert("RGBA")
        girl, slim, rgb = table[key]
        tone = colorsys.rgb_to_hsv((rgb >> 16 & 255) / 255.0, (rgb >> 8 & 255) / 255.0, (rgb & 255) / 255.0)
        img = turn(source.copy(), degrees, light, tone, 0.0 if made < len(PICKS) else 0.42)
        if made >= len(PICKS) and changed(source, img) < DIFFERENT:
            print(f"skipped {key} +{degrees}: only {changed(source, img):.0%} changed")
            continue
        img.save(OUT / f"human_{made:02d}.png")
        rows.append(f'        new Skin("textures/entity/human/human_{made:02d}.png", {str(girl).lower()}, {str(slim).lower()}, "{key}"), // hue +{degrees}')
        made += 1
    body = "\n".join(rows)
    TABLE.write_text(f'''package com.hex.kagune.control.village;

/**
 * 1.3.75: the villagers' own skins (pure; written by {{@code tools/human_skins.py}}: do not edit by hand): the children's skins with their
 * hair, clothes and eyes turned to other colours, so no villager looks like a child of the player's.
 */
public final class HumanSkins {{
    private HumanSkins() {{
    }}

    /**
     * One skin: its texture (the mod's), girl or boy, slim arms or wide, and the child's skin it was made from (1.3.76: its eyes are in
     * the same place, so it blinks with that skin's shut eyes, {{@code ChildBlinks}}).
     */
    public record Skin(String path, boolean girl, boolean slim, String child) {{
    }}

    public static final Skin[] ALL = {{
{body}
    }};
}}
''')
    print(f"{made} skins in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
