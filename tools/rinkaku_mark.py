#!/usr/bin/env python3
"""2.0.40: where a Rinkaku's tails tear out of the lower back (the owner's reference picture: a glowing red opening with a torn rim, red
veins branching out from it across the skin), laid over the back of the torso by RinkakuMarkLayer while the kagune is out. Two tiles, in
greys so the kagune's own colour tints them (RinkakuMarkLayer):

  rinkaku_mark.png       drawn over the skin as it is lit: the opening (light) inside its torn dark rim, the veins (light, dark-edged)
  rinkaku_mark_glow.png  added at full brightness: the opening's glowing heart and the veins' glowing cores (black: nothing)

The tile covers the back from 2 px under the shoulders to the belt (8 x 10 px of the skin, 64 x 80 here); the opening sits where the
tails' roots are (RinkakuRoot.ROOT_Y: 9.6 px down the back).

    python3 tools/rinkaku_mark.py
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hexkagune/textures/misc"
W, H = 64, 80
SS = 4
CENTRE = (0.5, 0.74)  # the opening (of the tile)


def veins(rng):
    """Branching lines out from the opening's rim: (points, width) each, in the big drawing's pixels."""
    cx, cy = CENTRE[0] * W * SS, CENTRE[1] * H * SS
    out = []
    starts = [(-150, 0.9), (-110, 0.75), (-70, 0.75), (-30, 0.9), (5, 1.0), (30, 1.0), (55, 1.0), (80, 1.0), (100, 1.0), (125, 1.0),
              (150, 1.0), (175, 0.9), (-175, 0.9)]
    for angle, strength in starts:
        a = math.radians(angle + rng.uniform(-8, 8))
        # Down the screen is +y: most veins run out sideways and down, a few up the back.
        r0x, r0y = 0.28 * W * SS, 0.10 * H * SS
        x, y = cx + math.cos(a) * r0x, cy + math.sin(a) * r0y
        grow(rng, out, x, y, a, 1.7 * SS * strength, 0.42 * W * SS * strength, 0)
    return out


def grow(rng, out, x, y, a, width, length, depth):
    pts = [(x, y)]
    step = 3.0 * SS
    travelled = 0.0
    while travelled < length and width > 0.5 * SS:
        a += rng.uniform(-0.45, 0.45)
        x += math.cos(a) * step
        y += math.sin(a) * step * 1.15
        pts.append((x, y))
        travelled += step
        width *= 0.97
        if depth < 3 and rng.random() < 0.24:
            grow(rng, out, x, y, a + rng.choice((-1, 1)) * rng.uniform(0.45, 0.95), width * 0.7, (length - travelled) * 0.75, depth + 1)
    out.append((pts, width))


def opening(draw, rng, fill, rim, grow_by=0.0):
    """The torn opening: an ellipse with a jagged rim (the skin split round it)."""
    cx, cy = CENTRE[0] * W * SS, CENTRE[1] * H * SS
    rx, ry = (0.27 + grow_by) * W * SS, (0.085 + grow_by * 0.6) * H * SS
    pts = []
    for k in range(48):
        t = 2 * math.pi * k / 48
        jag = 1.0 + (0.22 if k % 3 == 0 else 0.0) * rng.uniform(0.4, 1.0) - (0.08 if k % 3 == 1 else 0.0)
        pts.append((cx + math.cos(t) * rx * jag, cy + math.sin(t) * ry * jag))
    draw.polygon(pts, fill=fill, outline=rim)
    return pts


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # The lit tile: dark-edged light veins, the opening light inside a dark torn rim.
    lit = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(lit)
    rng = random.Random(4207)
    lines = veins(rng)
    for pts, width in lines:
        d.line(pts, fill=(60, 60, 60, 255), width=int(width + 1.2 * SS), joint="curve")
    for pts, width in lines:
        d.line(pts, fill=(235, 235, 235, 255), width=int(max(SS, width)), joint="curve")
    rim = opening(d, random.Random(9), (70, 70, 70, 255), None, 0.03)
    opening(d, random.Random(9), (255, 255, 255, 255), None, 0.0)
    lit = lit.resize((W, H), Image.LANCZOS)
    lit.save(OUT / "rinkaku_mark.png")
    # The glow tile: the opening's heart (soft) and the veins' cores, on black.
    glow = Image.new("RGB", (W * SS, H * SS), (0, 0, 0))
    g = ImageDraw.Draw(glow)
    for pts, width in lines:
        g.line(pts, fill=(150, 150, 150), width=int(max(SS * 0.75, width * 0.55)), joint="curve")
    opening(g, random.Random(9), (255, 255, 255), None, 0.0)
    glow = glow.filter(ImageFilter.GaussianBlur(1.2 * SS)).resize((W, H), Image.LANCZOS)
    glow.save(OUT / "rinkaku_mark_glow.png")
    print("wrote", OUT / "rinkaku_mark.png", OUT / "rinkaku_mark_glow.png")


if __name__ == "__main__":
    main()
