#!/usr/bin/env python3
"""2.1.1: the Exhaustion effect's icon (the owner: "Final form (Peace or War) now have a 5 minute cooldown, reflected as the new custom effect
Exaustion, with a custom 2d image as well"): an 18x18 pixel icon in the style of the kitchen's four (a dark outline, flat colours, one
highlight): a kakugan half shut under a heavy, drooping lid, its red dimmed, a crack across the lid, a drop of RC running from it. Burnt out.

    python3 tools/exhaustion_icon.py   ->  src/main/resources/assets/hexkagune/textures/mob_effect/exhaustion.png
"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hexkagune/textures/mob_effect/exhaustion.png"
OUTLINE = (30, 18, 22, 255)
LID_HI = (128, 112, 124, 255)
LID = (98, 84, 96, 255)
LID_LOW = (70, 58, 70, 255)
LASH = (40, 26, 32, 255)
SCLERA = (24, 16, 20, 255)
IRIS = (150, 30, 40, 255)
IRIS_HI = (206, 58, 64, 255)
PUPIL = (52, 8, 14, 255)
CRACK = (226, 214, 220, 255)
TEAR = (164, 24, 36, 255)
TEAR_HI = (232, 88, 96, 255)


def main():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = img.load()
    filled = set()
    for y in range(18):
        for x in range(18):
            cx, cy = x + 0.5, y + 0.5
            half = 4.7 * (1.0 - ((cx - 9.0) / 8.3) ** 2)  # the almond of the eye
            if half <= 0.0 or abs(cy - 9.0) > half:
                continue
            filled.add((x, y))
            lid_edge = 7.2 + 1.1 * (1.0 - ((cx - 9.0) / 7.5) ** 2)  # the lid half shut, drooping lowest in the middle
            if cy < lid_edge:
                top = (cy - (9.0 - half)) / max(0.5, lid_edge - (9.0 - half))
                px[x, y] = LID_HI if top < 0.35 else LID if top < 0.8 else LID_LOW
                if lid_edge - cy < 1.0:
                    px[x, y] = LASH
                continue
            d = ((cx - 9.0) ** 2 + (cy - 10.4) ** 2) ** 0.5
            px[x, y] = PUPIL if d < 1.15 else IRIS if d < 2.9 else SCLERA
    px[7, 9] = IRIS_HI  # its one glint, dull
    # The crack across the lid.
    for x, y in [(12, 4), (12, 5), (11, 6), (11, 7)]:
        if (x, y) in filled and px[x, y] != LASH:
            px[x, y] = CRACK
    # A thin trail of RC from the corner of the eye, ending in a drop.
    for x, y, c in [(5, 13, TEAR), (5, 14, TEAR), (5, 15, TEAR), (4, 15, TEAR), (4, 16, TEAR), (5, 16, TEAR), (4, 15, TEAR_HI)]:
        px[x, y] = c
        filled.add((x, y))
    # The dark outline round everything.
    outline = []
    for y in range(18):
        for x in range(18):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                outline.append((x, y))
    for x, y in outline:
        px[x, y] = OUTLINE
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
