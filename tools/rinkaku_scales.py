#!/usr/bin/env python3
"""2.0.39: the Rinkaku tail's markings (the owner's reference picture: thin chevrons all over it, black and light, staggered, every one
pointing back toward the body, and a light stripe running the length of each face near one edge), as a tile laid over the tail by
RinkakuTube: u round the tail (one tile across each broad face), v along it (one tile every RinkakuTube.TILE blocks), v growing toward the
tip. White marks are tinted the tail's light colour as drawn; black stays black; the rest is clear.

    python3 tools/rinkaku_scales.py   ->  src/main/resources/assets/hexkagune/textures/misc/rinkaku_scales.png
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hexkagune/textures/misc/rinkaku_scales.png"
SIZE = 128
SS = 4  # drawn this much larger, then shrunk: smooth edges
STRIPE = (0.205, 0.265)  # across the face (u), the light stripe
COLS, ROWS = 5, 4


def main():
    big = SIZE * SS
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    rng = random.Random(1997)
    # The stripe: a band the whole length, soft at its sides.
    a, b = STRIPE[0] * big, STRIPE[1] * big
    for x in range(int(a) - 2 * SS, int(b) + 2 * SS):
        edge = min(x - a, b - x)
        alpha = max(0.0, min(1.0, 0.5 + edge / (2.0 * SS)))
        if alpha > 0:
            draw.line([(x, 0), (x, big)], fill=(255, 255, 255, int(220 * alpha)))
    # The chevrons: staggered rows, apex toward v = 0 (toward the body), arms a little curved, each its own size and lean.
    cell_w, cell_h = big / COLS, big / ROWS
    for r in range(ROWS):
        for c in range(COLS):
            cx = (c + 0.5 + 0.5 * (r % 2)) * cell_w + rng.uniform(-0.12, 0.12) * cell_w
            cy = (r + 0.5) * cell_h + rng.uniform(-0.12, 0.12) * cell_h
            u = (cx % big) / big
            if STRIPE[0] - 0.06 < u < STRIPE[1] + 0.06:
                continue  # none on the stripe
            dark = rng.random() < 0.5
            colour = (12, 2, 6, 255) if dark else (255, 255, 255, 255)
            span = rng.uniform(0.26, 0.34) * cell_w
            drop = rng.uniform(0.30, 0.40) * cell_h
            lean = rng.uniform(-0.15, 0.15) * span
            width = int(round(rng.uniform(1.8, 2.4) * SS))
            for side in (-1, 1):
                pts = []
                for k in range(9):
                    t = k / 8.0
                    # From the apex down and out, bowing outward a little (an arch, as drawn in the picture).
                    x = cx + lean * (1 - t) + side * span * (t ** 0.85)
                    y = cy - drop * 0.5 + drop * t - 0.12 * drop * math.sin(math.pi * t)
                    pts.append((x, y))
                for dx in (-big, 0, big):
                    for dy in (-big, 0, big):
                        draw.line([(x + dx, y + dy) for x, y in pts], fill=colour, width=width, joint="curve")
    small = img.resize((SIZE, SIZE), Image.LANCZOS)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    small.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
