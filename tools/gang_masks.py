#!/usr/bin/env python3
"""4.1.45: the roaming gangs' masks (npc.Gang): each gang's own mask (mask_clown, mask_gas: the owner's, left as they are) repainted into
four more, so every member of a gang wears a different one (mask_clown_v2 .. v5, mask_gas_v2 .. v5). Each texel is sorted into its
material by its colour (the clown's white face, red, pink cheeks, blue diamonds; the gas mask's dark body, teal lenses, olive filters, grey
metal) and given the variant's colour for it, its own light kept as shading; the black of the eyes and outlines stays. Same model
(Outfits.geometryOf), new paint.

Usage: python3 tools/gang_masks.py   (writes src/main/resources/assets/hexkagune/textures/outfit/mask_{clown,gas}_v{2..5}.png)
"""
import colorsys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/hexkagune/textures/outfit"

# Each variant: the colour each material takes (r, g, b), at the light of the base's own for that material.
CLOWN = {
    "v2": {"face": (236, 232, 240), "red": (110, 30, 150), "pink": (222, 178, 58), "blue": (40, 160, 160)},   # jester
    "v3": {"face": (188, 198, 180), "red": (44, 96, 40), "pink": (196, 186, 74), "blue": (112, 22, 22)},      # creep
    "v4": {"face": (204, 218, 238), "red": (32, 44, 116), "pink": (140, 172, 224), "blue": (70, 140, 236)},   # sad
    "v5": {"face": (246, 240, 244), "red": (226, 58, 148), "pink": (118, 220, 188), "blue": (240, 140, 40)},  # candy
}
GAS = {
    "v2": {"body": (78, 88, 46), "lens": (204, 142, 40), "filter": (34, 34, 34), "metal": (150, 146, 120)},     # military
    "v3": {"body": (214, 182, 40), "lens": (30, 42, 62), "filter": (52, 52, 58), "metal": (120, 120, 126)},    # hazmat
    "v4": {"body": (66, 74, 86), "lens": (178, 36, 36), "filter": (40, 40, 46), "metal": (162, 162, 170)},    # riot
    "v5": {"body": (98, 62, 36), "lens": (64, 152, 84), "filter": (172, 142, 62), "metal": (130, 110, 86)},    # rust
}
# The base's own light for each material (its middle shade), so a variant keeps the base's shading round it.
CLOWN_LIGHT = {"face": 0.86, "red": 0.36, "pink": 0.63, "blue": 0.45}
GAS_LIGHT = {"body": 0.13, "lens": 0.25, "filter": 0.26, "metal": 0.56}


def clown_material(r, g, b):
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    if l < 0.2:
        return None  # eyes, outlines: as they are
    if s > 0.45 and (h < 0.04 or h > 0.93) and l < 0.55:
        return "red"
    if s > 0.3 and (h > 0.9 or h < 0.04) and l >= 0.55 and l < 0.8:
        return "pink"
    if s > 0.3 and 0.5 < h < 0.75:
        return "blue"
    if l >= 0.6:
        return "face"
    return None


def gas_material(r, g, b):
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    if s > 0.15 and 0.42 < h < 0.58:
        return "lens"
    if s > 0.2 and 0.14 < h < 0.3 and l > 0.15:
        return "filter"
    if l > 0.45 and s < 0.15:
        return "metal"
    if l < 0.3:
        return "body"
    return None


def paint(base, out, variant, material, light):
    image = Image.open(OUT / (base + ".png")).convert("RGBA")
    pixels = image.load()
    width, height = image.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            kind = material(r, g, b)
            if kind is None:
                continue
            _, l, _ = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            k = max(0.35, min(1.6, l / light[kind]))
            tr, tg, tb = variant[kind]
            pixels[x, y] = (min(255, int(tr * k)), min(255, int(tg * k)), min(255, int(tb * k)), a)
    image.save(OUT / (out + ".png"))
    print("wrote", (OUT / (out + ".png")).relative_to(ROOT))


def main():
    for name, variant in CLOWN.items():
        paint("mask_clown", "mask_clown_" + name, variant, clown_material, CLOWN_LIGHT)
    for name, variant in GAS.items():
        paint("mask_gas", "mask_gas_" + name, variant, gas_material, GAS_LIGHT)


if __name__ == "__main__":
    main()
