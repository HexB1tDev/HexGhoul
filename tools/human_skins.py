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


def turn(img, degrees, light, tone):
    px = img.load()
    w, h = img.size
    k = w // 64
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            if s < 0.12 or skin_like(hh, s, v, tone, x < 32 * k and y < 16 * k):
                continue  # whites, blacks and greys, and skin, kept
            hh = (hh + degrees / 360.0) % 1.0
            v = min(1.0, v * light)
            nr, ng, nb = colorsys.hsv_to_rgb(hh, s, v)
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return img


def main():
    table = {m.group(1): (m.group(2) == "true", m.group(3) == "true", int(m.group(4), 16))
             for m in re.finditer(r'new Skin\("(\w+)", (true|false), (true|false), 0x([0-9A-Fa-f]{6})', SKINS.read_text())}
    OUT.mkdir(parents=True, exist_ok=True)
    rows = []
    for i, (key, degrees, light) in enumerate(PICKS):
        img = Image.open(CHILD / f"{key}.png").convert("RGBA")
        girl, slim, rgb = table[key]
        tone = colorsys.rgb_to_hsv((rgb >> 16 & 255) / 255.0, (rgb >> 8 & 255) / 255.0, (rgb & 255) / 255.0)
        turn(img, degrees, light, tone).save(OUT / f"human_{i:02d}.png")
        rows.append(f'        new Skin("textures/entity/human/human_{i:02d}.png", {str(girl).lower()}, {str(slim).lower()}, "{key}"), // hue +{degrees}')
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
    print(f"{len(PICKS)} skins in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
