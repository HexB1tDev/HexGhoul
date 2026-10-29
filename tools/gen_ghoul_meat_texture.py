#!/usr/bin/env python3
"""7.0.1: draws the Ghoul Meat item texture (16x16, vanilla item scale) by hand, pixel by pixel.

A torn hunk of ghoul flesh: dark crimson muscle lit from the top left like vanilla's raw meats, pale sinew, a strip of the
ghoul's plum-black hide along the lower right, and RC veins glowing the kakugan's red (the mod's palette: AbilityGuide EYES
0xFF3347, RINKAKU 0xE5394F, KAKUJA_PLUM 0xB27FD6). Usage: gen_ghoul_meat_texture.py <out.png> [preview.png]
"""
import sys
from PIL import Image

PALETTE = {
    '.': None,
    'K': (0x1c, 0x05, 0x0a),  # outline
    'd': (0x55, 0x0b, 0x18),  # flesh in shadow
    'm': (0x7c, 0x13, 0x25),  # flesh
    'h': (0xa3, 0x22, 0x37),  # flesh lit
    'l': (0xc8, 0x48, 0x5a),  # flesh catching the light
    's': (0xe3, 0x9c, 0xa3),  # sinew
    'p': (0x2a, 0x0c, 0x22),  # hide
    'q': (0x4a, 0x18, 0x3e),  # hide lit
    'r': (0x6e, 0x2c, 0x5c),  # hide sheen
    'v': (0xe5, 0x39, 0x4f),  # RC vein
    'V': (0xff, 0x6a, 0x7a),  # vein glowing
    'W': (0xff, 0xc2, 0xc8),  # vein core
}

ART = [
    "................",
    "..........KK....",
    ".......KK.KhK...",
    ".....KKlhKKmmK..",
    "....KlhsdhmmmdK.",
    "...KlhsmdmvmmmdK",
    "..KlhsmmdvVmdmdK",
    "..KhmmdmvmWvmddK",
    ".KhhmdmvmdmmvdqK",
    ".KhmdmvmmdmmmqrK",
    "KhsmdVvmmdmmdqpK",
    "KhmdmmvmmdmddqpK",
    ".KhmmdmvmddmqrpK",
    ".KKdmmdmddqqppK.",
    "...KKdddqppppK..",
    ".....KKKKKKKK...",
]


def main():
    assert len(ART) == 16 and all(len(row) == 16 for row in ART), [len(row) for row in ART]
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ART):
        for x, c in enumerate(row):
            colour = PALETTE[c]
            if colour is not None:
                image.putpixel((x, y), colour + (255,))
    image.save(sys.argv[1])
    if len(sys.argv) > 2:
        # A preview: the texture blown up, on a dark and a light slot, as an inventory shows it.
        big = image.resize((256, 256), Image.NEAREST)
        sheet = Image.new('RGBA', (560, 280), (0x1b, 0x1d, 0x24, 255))
        sheet.paste((0x8b, 0x8b, 0x8b, 255), (292, 12, 548, 268))
        sheet.alpha_composite(big, (12, 12))
        sheet.alpha_composite(big, (292, 12))
        sheet.save(sys.argv[2])


main()
