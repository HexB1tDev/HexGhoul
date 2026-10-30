#!/usr/bin/env python3
"""1.3.0: Eto's textures, from the user's skin (art/eto_skin.png, 128x128: the slim player layout at twice the pixels).

  eto.png              her skin, but her right eye (painted as a ghoul's) made the mirror of her left: both human
  eto_kakugan.png      her right eye turned, in the shape of her own eye (only these pixels), laid over the skin as she turns:
                       the sclera black, the iris red, thin veins under the eye
  eto_kakugan_glow.png the red iris alone, drawn full bright

    python3 tools/eto_textures.py
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art" / "eto_skin.png"
OUT = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "entity" / "eto"
# The face and its outer layer, at twice the pixels.
FACE = (16, 16)
HAT = (80, 16)
# The side of her head next to her right eye, and its outer layer.
SIDE = (0, 16)
HAT_SIDE = (64, 16)


def at(origin, x, y):
    return origin[0] + x, origin[1] + y


def rgb(code, alpha=255):
    return int(code[0:2], 16), int(code[2:4], 16), int(code[4:6], 16), alpha


def main():
    skin = Image.open(SOURCE).convert("RGBA")
    assert skin.size == (128, 128), "the skin is not 128x128"
    # Her right eye (the viewer's left) is painted as a ghoul's: it becomes the mirror of her left, on the face and its outer layer.
    assert skin.getpixel(at(FACE, 4, 11))[:3] == rgb("a43f46")[:3], "the right eye is not where it was"
    day = skin.copy()
    for origin in (FACE, HAT):
        for y in range(9, 14):
            for x in range(9, 15):
                day.putpixel(at(origin, 15 - x, y), skin.getpixel(at(origin, x, y)))

    # Turned, in the shape of that eye: its dark pixels black, the middle of it (where her catchlight is) the red iris.
    night = Image.new("RGBA", skin.size, (0, 0, 0, 0))
    sclera, iris, shine = rgb("0a0306"), rgb("d0101f"), rgb("ff3a48")
    for x, y in ((3, 10), (4, 10), (5, 10), (2, 11), (5, 11), (1, 12), (2, 12), (5, 12), (4, 9)):
        night.putpixel(at(FACE, x, y), sclera)
    for x, y in ((3, 11), (3, 12), (4, 12)):
        night.putpixel(at(FACE, x, y), iris)
    night.putpixel(at(FACE, 4, 11), shine)
    for x, y in ((4, 10), (5, 10), (5, 12)):
        night.putpixel(at(HAT, x, y), sclera)
    night.putpixel(at(HAT, 4, 12), iris)
    # Veins: the user's sketch (from the outer corner: up into the bangs, two out to the temple, one down the cheek) and two small
    # ones under the eye. A vein left of the face (x < 0) carries on round the side of her head; where her outer layer is painted over
    # a vein, it is drawn there too, or the hair would hide it.
    vein = rgb("7a0c1a")
    for x, y in ((2, 9), (1, 8), (1, 7), (2, 6),
                 (1, 10), (0, 10), (-1, 9), (-2, 9),
                 (0, 12), (-1, 12), (-2, 13),
                 (0, 14), (-1, 15),
                 (6, 12), (6, 13), (7, 14), (3, 14), (2, 15)):
        base, outer = (FACE, HAT) if x >= 0 else (SIDE, HAT_SIDE)
        column = x if x >= 0 else 16 + x
        night.putpixel(at(base, column, y), vein)
        if skin.getpixel(at(outer, column, y))[3] > 0:
            night.putpixel(at(outer, column, y), vein)

    glow = Image.new("RGBA", skin.size, (0, 0, 0, 0))
    bright = rgb("ff2a3a")
    for x, y in ((3, 11), (4, 11), (3, 12), (4, 12)):
        glow.putpixel(at(FACE, x, y), bright)
    glow.putpixel(at(HAT, 4, 12), bright)

    OUT.mkdir(parents=True, exist_ok=True)
    for name, image in (("eto.png", day), ("eto_kakugan.png", night), ("eto_kakugan_glow.png", glow)):
        image.save(OUT / name, optimize=True)
        print(OUT.relative_to(ROOT) / name)


if __name__ == "__main__":
    main()
