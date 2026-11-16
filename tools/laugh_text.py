#!/usr/bin/env python3
"""4.3.0 (the owner: "can you revert it but add like "HAHAHAHAHA" text over the head", then "no, white and with a cool letter type, not that
shit"): the mastered Ukaku's laugh, one "HA" as manga sound-effect lettering: white, heavy and slanted (Inter Black Italic, SIL Open Font
License), a thick black outline round it, on transparency. LaughPullOutClient draws it over the head again and again, "HA" by "HA".

Usage: python3 tools/laugh_text.py    (writes src/main/resources/assets/hexkagune/textures/misc/laugh_ha.png)
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/hexkagune/textures/misc/laugh_ha.png")
FONT = "/usr/share/fonts/opentype/inter/Inter-BlackItalic.otf"
W, H = 256, 128
SIZE = 104
STROKE = 11


def main():
    if not os.path.isfile(FONT):
        sys.exit("needs " + FONT)
    font = ImageFont.truetype(FONT, SIZE)
    image = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    left, top, right, bottom = draw.textbbox((0, 0), "HA", font=font, stroke_width=STROKE)
    x = (W - (right - left)) / 2 - left
    y = (H - (bottom - top)) / 2 - top
    draw.text((x, y), "HA", font=font, fill=(255, 255, 255, 255), stroke_width=STROKE, stroke_fill=(10, 8, 10, 255))
    image.save(OUT)
    print("wrote", OUT, image.size)


if __name__ == "__main__":
    main()
