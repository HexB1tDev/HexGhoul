#!/usr/bin/env python3
"""2.0.50 (the owner: "Make the scales on the rinkaku more realistic"): the Rinkaku tail's scales, as a tile laid over the tail by RinkakuTube
(u round the tail, one tile across each broad face; v along it, one tile every RinkakuRibbon.TILE blocks, v growing toward the tip).
Was: thin chevrons, black and white (2.0.39).

Overlapping keeled scales, as a reptile's: staggered rows, each scale's rounded free edge toward the tip, laid over the base of the next
(drawn from the tip back, so each lies on the one behind it). Each in greys over the tail's own colour (white is the tail's light colour as
drawn, black stays black, the alpha how much shows): its body a little darker toward its base where it tucks under, a raised keel down its
middle, a lit rim along its free edge and, just past it, the dark line of the shadow that edge casts. A faint light stripe still runs the
length of each face near one edge (the reference's).

The tile is stretched about 1.8 times as tall on the tail as it is wide (a face ~0.3 blocks across, a tile 0.55 along), so the scales
are drawn that much squatter here to come out round.

    python3 tools/rinkaku_scales.py   ->  src/main/resources/assets/hexkagune/textures/misc/rinkaku_scales.png
"""
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hexkagune/textures/misc/rinkaku_scales.png"
SIZE = 128
SS = 4
COLS = 4           # scales across a face (big enough to read as scales a few blocks off)
ROWS = 10          # rows along a tile (even: the stagger carries on across the tile's seam)
REACH = 1.55       # how many rows long a scale is (the rest lies under the next)
STRIPE = (0.205, 0.255)


def scale_at(sx, sy):
    """A scale's own shading at (sx across -1..1, sy along 0 base..1 free edge), as (grey 0..255, alpha 0..1), or None outside it."""
    edge = 0.52 + 0.48 * math.sqrt(max(0.0, 1.0 - sx * sx))  # its rounded free edge
    if sy < 0.0 or sy > edge or abs(sx) > 1.0:
        return None
    k = sy / edge  # 0 its base .. 1 its free edge
    side = abs(sx)
    # Its body: darker toward its base where it tucks under the one before it, a touch darker toward its sides (rounded).
    grey = 150 + 70 * k - 40 * side * side
    alpha = 0.30 + 0.12 * k
    # The keel down its middle, lit on one side.
    keel = math.exp(-((sx + 0.06) / 0.11) ** 2) * (0.35 + 0.65 * k)
    grey += 60 * keel
    alpha += 0.18 * keel
    # The lit rim just inside its free edge.
    rim = max(0.0, 1.0 - (edge - sy) / 0.13)
    grey += 80 * rim
    alpha += 0.32 * rim
    # The outline at its very edge (a thin dark line round it).
    if edge - sy < 0.045 or 1.0 - side < 0.05:
        return 25, 0.8
    return max(0, min(255, int(grey))), max(0.0, min(1.0, alpha))


def main():
    big = SIZE * SS
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(1997)
    cw = big / COLS
    rh = big / ROWS
    # Each scale's own small variation: a lean, its size and its shade.
    jitter = {(r, c): (rng.uniform(-0.08, 0.08), rng.uniform(0.92, 1.06), rng.uniform(-14, 10)) for r in range(ROWS) for c in range(COLS)}
    for y in range(big):
        for x in range(big):
            u = x + 0.5
            v = y + 0.5
            # The scales whose bodies may cover this texel: its own row's and those up to REACH rows back toward the root. A scale
            # nearer the root lies over the base of the next (its free edge on it), so the one furthest back that covers it is seen.
            row_here = int(math.floor(v / rh))
            found = None
            for back in range(int(math.ceil(REACH)), -1, -1):
                r = row_here - back
                rr = r % ROWS
                offset = 0.5 * cw if r % 2 else 0.0
                c = int(math.floor((u - offset) / cw))
                for cc in (c - 1, c, c + 1):
                    lean, size, tone = jitter[(rr, cc % COLS)]
                    cx = offset + (cc + 0.5) * cw
                    base = r * rh
                    sy = (v - base) / (REACH * rh * size)
                    sx = (u - cx) / (0.5 * cw * 1.04 * size) - lean * sy
                    shade = scale_at(sx, sy)
                    if shade is not None:
                        found = (int(shade[0] + (tone if shade[0] > 40 else 0)), shade[1])
                        break
                if found is not None:
                    break
            if found is None:
                continue
            grey, alpha = found
            grey = max(0, min(255, grey))
            px[x, y] = (grey, grey, grey, int(255 * alpha))
    # The shadow each free edge casts on the scale under it: a soft dark band just past every edge.
    shadow = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    sp = shadow.load()
    for y in range(big):
        for x in range(big):
            r, g, b, a = px[x, y]
            yb = (y - int(0.035 * rh)) % big
            r2, g2, b2, a2 = px[x, yb]
            if a2 >= 200 and r2 < 40 and not (a >= 200 and r < 40):
                sp[x, y] = (0, 0, 0, 110)
    img = Image.alpha_composite(img, shadow)
    # The faint light stripe the length of each face near one edge.
    stripe = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    st = stripe.load()
    a0, b0 = STRIPE[0] * big, STRIPE[1] * big
    for x in range(int(a0) - 2 * SS, int(b0) + 2 * SS):
        edge = min(x - a0, b0 - x)
        alpha = max(0.0, min(1.0, 0.5 + edge / (2.0 * SS)))
        for y in range(big):
            st[x % big, y] = (255, 255, 255, int(70 * alpha))
    img = Image.alpha_composite(img, stripe)
    small = img.resize((SIZE, SIZE), Image.LANCZOS)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    small.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
