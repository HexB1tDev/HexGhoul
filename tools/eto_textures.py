#!/usr/bin/env python3
"""1.3.0: Eto's textures, from the user's skin (art/eto_skin.png, 128x128: the slim player layout at twice the pixels).

  eto.png              her skin, but her right eye (painted as a ghoul's) made the mirror of her left: both human
  eto_kakugan.png      her right eye turned, in the shape of her own eye (only these pixels), laid over the skin as she turns:
                       the sclera black, the iris red, thin veins under the eye
  eto_kakugan_glow.png the red iris alone, drawn full bright
  eto_closed.png       her eyes shut, "- -" (a blink, a kiss), over the skin and the lashes of its outer layer
  eto_blush.png        pink on her cheeks
  eto_angry.png        the anger mark drawn over her head when she is left hanging (16x16)

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

    # Turned: the same eye as her other one, only its colours changed (the user's marks). Her lashes and the flick over the eye stay as
    # they are; the iris (columns 4-5 down rows 11-13, column 3 down rows 12-13) goes red, shaded as
    # her grey iris is (darkest at the top, her catchlight a bright highlight), on the face and on the outer layer over it; the two whites at the outer corner go black.
    night = Image.new("RGBA", skin.size, (0, 0, 0, 0))
    black, deep, red, light, shine = rgb("0a0306"), rgb("9a0c18"), rgb("c81020"), rgb("e8303c"), rgb("ff6a76")
    IRIS = ((4, 11), (5, 11), (4, 12), (5, 12), (4, 13), (5, 13), (3, 12), (3, 13))
    shade = {(4, 11): shine, (5, 11): deep, (3, 12): red, (4, 12): red, (5, 12): red, (3, 13): light, (4, 13): light, (5, 13): light}
    for (x, y), colour in shade.items():
        night.putpixel(at(FACE, x, y), colour)
        if skin.getpixel(at(HAT, x, y))[3] > 0:
            night.putpixel(at(HAT, x, y), colour)
    for x, y in ((2, 12), (2, 13)):
        night.putpixel(at(FACE, x, y), black)
    # Veins: the user's sketch (from the outer corner: up into the bangs, two out to the temple, one down the cheek) and one small
    # one under the eye (the user had the one by its inner corner taken out). A vein left of the face (x < 0) carries on round the side of her head; where her outer layer is painted over
    # a vein, it is drawn there too, or the hair would hide it.
    vein = rgb("7a0c1a")
    for x, y in ((2, 9), (1, 8), (1, 7), (2, 6),
                 (1, 10), (0, 10), (-1, 9), (-2, 9),
                 (0, 12), (-1, 12), (-2, 13),
                 (0, 14), (-1, 15),
                 (3, 14), (2, 15)):
        base, outer = (FACE, HAT) if x >= 0 else (SIDE, HAT_SIDE)
        column = x if x >= 0 else 16 + x
        night.putpixel(at(base, column, y), vein)
        if skin.getpixel(at(outer, column, y))[3] > 0:
            night.putpixel(at(outer, column, y), vein)

    glow = Image.new("RGBA", skin.size, (0, 0, 0, 0))
    bright = rgb("ff2a3a")
    for x, y in IRIS:
        glow.putpixel(at(FACE, x, y), bright)
    for x, y in ((4, 12), (5, 12), (4, 13), (5, 13)):
        glow.putpixel(at(HAT, x, y), bright)

    # Her eyes shut, "- -": over each eye (and the lashes her outer layer paints on it) her skin, one dark line where the lids meet.
    closed = Image.new("RGBA", skin.size, (0, 0, 0, 0))
    lid, line, hair = rgb("ebe0e0"), rgb("333a3e"), rgb("598a8c")
    for mirror in (False, True):
        def put(origin, x, y, colour):
            closed.putpixel(at(origin, 15 - x if mirror else x, y), colour)
        for y, xs in ((9, range(3, 6)), (10, range(3, 6)), (11, range(2, 6)), (13, range(2, 6))):
            for x in xs:
                put(FACE, x, y, lid)
        for x in range(1, 6):
            put(FACE, x, 12, line)
        put(FACE, 1, 13, hair)
        for x in (4, 5):
            put(HAT, x, 10, lid)
            put(HAT, x, 12, line)
            put(HAT, x, 13, lid)

    # Her blush: pink under both shut eyes, two little strokes on each cheek.
    blush = Image.new("RGBA", skin.size, (0, 0, 0, 0))
    soft, warm, stroke = rgb("f4a0b2", 140), rgb("ec8098", 190), rgb("d85c7c", 220)
    for mirror in (False, True):
        def put(x, y, colour):
            blush.putpixel(at(FACE, 15 - x if mirror else x, y), colour)
        for x in range(1, 5):
            put(x, 13, soft)
            put(x, 14, warm)
        put(2, 13, stroke)
        put(4, 13, stroke)

    # The anger mark over her head: four red brackets, their corners toward the middle, outlined dark.
    angry = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    red, edge, shine = rgb("e8202e"), rgb("6a0010"), rgb("ff7080")
    body = set()
    for x in range(1, 6):
        body.update({(x, 4), (x, 5)})
    for y in range(1, 6):
        body.update({(4, y), (5, y)})
    body -= {(1, 5), (5, 1)}
    marks = set()
    for x, y in body:
        marks.update({(x, y), (15 - x, y), (x, 15 - y), (15 - x, 15 - y)})
    for x, y in marks:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in marks:
                angry.putpixel((nx, ny), edge)
    for x, y in marks:
        angry.putpixel((x, y), red)
    for x, y in ((2, 4), (4, 2), (13, 4), (11, 2), (2, 11), (13, 11)):
        angry.putpixel((x, y), shine)

    OUT.mkdir(parents=True, exist_ok=True)
    for name, image in (("eto.png", day), ("eto_kakugan.png", night), ("eto_kakugan_glow.png", glow), ("eto_closed.png", closed),
                        ("eto_blush.png", blush), ("eto_angry.png", angry)):
        image.save(OUT / name, optimize=True)
        print(OUT.relative_to(ROOT) / name)


if __name__ == "__main__":
    main()
