"""4.1.82 (the owner: "clients are spawning in areas that are not infront of the reception"): Anteiku's floor seen from above, at Touka's feet: where the shift's regulars sit (JoinTrial.counter + JoinServer.counterSeat,
the same rules done here over the template), next to where 4.1.79's random ring put them.

    python3 -I tools/cafe_seats.py out.png       (from the repo root)
"""
import math, random, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import os
os.chdir(Path(__file__).resolve().parent.parent)
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import structure_render as S
import city_render as C

grid, names, people = S.load("anteiku")
air = [n == "minecraft:air" or n.endswith("cave_air") for n in names]
fluid = [("water" in n or "lava" in n) and "cauldron" not in n for n in names]
W, H, D = grid.shape
touka = [p for p in people if p[0] == "hexkagune:touka"][0]
tx, ty, tz = touka[1], touka[2], touka[3]

def isair(x, y, z):
    if not (0 <= x < W and 0 <= y < H and 0 <= z < D):
        return True
    return air[grid[x, y, z]]

def standable(x, y, z):
    g = (x, y - 1, z)
    return (not isair(*g)) and not fluid[grid[g]] and isair(x, y, z) and isair(x, y + 1, z)

def roofed(x, y, z):
    return any(not isair(x, y + up, z) for up in range(2, 11))

def counter(yaw, near=2, far=6, side_n=3):
    fx, fz = -math.sin(math.radians(yaw)), math.cos(math.radians(yaw))
    out, seen = [], set()
    for ahead in range(near, far + 1):
        for side in range(-side_n, side_n + 1):
            dx = round(fx * ahead - fz * side)
            dz = round(fz * ahead + fx * side)
            if (dx, dz) not in seen:
                seen.add((dx, dz)); out.append((dx, dz))
    return out

cands, seats = [], []
for dx, dz in counter(0.0):
    pick = None
    for dy in (0, -1, 1):
        x, y, z = tx + dx, ty + dy, tz + dz
        if standable(x, y, z) and roofed(x, y, z):
            pick = (x, y, z); break
    cands.append(((tx + dx, tz + dz), pick))
    if pick:
        seats.append(pick)
    if len(seats) >= 6:
        break
allc = [(tx + dx, tz + dz) for dx, dz in counter(0.0)]
print("Touka", tx, ty, tz, "seats", seats)
print("candidates looked at", len(cands), "of", len(allc))
# 4.1.79's old ring: floor(level, anchor, SHIFT_NEAR, SHIFT_FAR, true, false) and the 1.0..SHIFT_FAR outdoors fallback.
src = Path("src/main/java/com/hex/kagune/control/npc/JoinTrial.java").read_text()
import re
near = float(re.search(r"SHIFT_NEAR = ([\d.]+)", src).group(1)); far = float(re.search(r"SHIFT_FAR = ([\d.]+)", src).group(1))
print("old ring", near, far)


# Draw (cropped to the café floor): the floor (feet level air, solid under) light; anything at feet level (walls, the counter, tables,
# chairs) in its own colour, framed; outside the walls dark.
X0, X1, Z0, Z1 = 8, 28, 2, 21
cell = 34
pad = 30
img = Image.new("RGB", ((X1 - X0) * cell + pad * 2 + 360, (Z1 - Z0) * cell + pad * 2), (24, 24, 28))
dr = ImageDraw.Draw(img)
font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 15)
small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 14)
big = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 19)
def px(x, z):
    return pad + (x - X0) * cell, pad + (z - Z0) * cell
for x in range(X0, X1):
    for z in range(Z0, Z1):
        X, Z = px(x, z)
        feet, under = (x, ty, z), (x, ty - 1, z)
        if not isair(*feet):
            c = C.colour(names[grid[feet]])[:3]
            c = tuple(int(v * 0.8) for v in c)
            dr.rectangle([X, Z, X + cell - 1, Z + cell - 1], fill=c, outline=(15, 15, 15), width=2)
        elif not isair(*under):
            c = C.colour(names[grid[under]])[:3]
            c = tuple(int(70 + v * 0.55) for v in c)
            dr.rectangle([X, Z, X + cell - 1, Z + cell - 1], fill=c, outline=tuple(max(0, v - 18) for v in c))
            if not roofed(x, ty, z):
                dr.line([X + 2, Z + 2, X + cell - 3, Z + cell - 3], fill=(90, 140, 220), width=2)
        else:
            dr.rectangle([X, Z, X + cell - 1, Z + cell - 1], fill=(36, 36, 42))
cx, cz = px(tx + 0.5, tz + 0.5)
for r in (near, far):
    dr.ellipse([cx - r * cell, cz - r * cell, cx + r * cell, cz + r * cell], outline=(235, 125, 60), width=3)
for (x, z) in allc:
    X, Z = px(x, z)
    dr.rectangle([X + 5, Z + 5, X + cell - 6, Z + cell - 6], outline=(245, 245, 245), width=1)
for (x, z), pick in cands:
    X, Z = px(x, z)
    if pick is None:
        dr.line([X + 7, Z + 7, X + cell - 8, Z + cell - 8], fill=(230, 50, 50), width=3)
        dr.line([X + 7, Z + cell - 8, X + cell - 8, Z + 7], fill=(230, 50, 50), width=3)
for i, (x, y, z) in enumerate(seats):
    X, Z = px(x, z)
    dr.ellipse([X + 4, Z + 4, X + cell - 5, Z + cell - 5], fill=(70, 200, 90), outline=(255, 255, 255), width=2)
    dr.text((X + cell / 2, Z + cell / 2), str(i + 1), fill=(0, 0, 0), font=small, anchor="mm")
X, Z = px(tx, tz)
dr.ellipse([X + 3, Z + 3, X + cell - 4, Z + cell - 4], fill=(160, 90, 210), outline=(255, 255, 255), width=2)
dr.polygon([(X + cell / 2 - 6, Z + cell - 4), (X + cell / 2 + 6, Z + cell - 4), (X + cell / 2, Z + cell + 6)], fill=(255, 255, 255))
# the counter's label (the row of solid blocks between her and the seats)
lx_, lz_ = px(15, 9)
def label(xy, text):
    box = dr.textbbox(xy, text, font=small, anchor="rm")
    dr.rectangle([box[0] - 4, box[1] - 3, box[2] + 4, box[3] + 3], fill=(20, 20, 24))
    dr.text(xy, text, fill=(255, 235, 160), font=small, anchor="rm")
label((px(12, 9)[0] - 4, lz_ + cell / 2), "counter")
dr.line([px(12, 9)[0], lz_ + cell / 2, lx_ + 4, lz_ + cell / 2], fill=(255, 235, 160), width=2)
dr.polygon([(lx_ + 6, lz_ + cell / 2), (lx_ - 4, lz_ + cell / 2 - 5), (lx_ - 4, lz_ + cell / 2 + 5)], fill=(255, 235, 160))
label((px(14, 15)[0] - 4, px(14, 15)[1] + cell / 2), "tables")
lx = pad * 2 + (X1 - X0) * cell
ly = pad
dr.text((lx, ly), "Anteiku from above (4.1.82)", fill=(240, 240, 240), font=big); ly += 30
dr.text((lx, ly), f"its template, at Touka's feet (y {ty})", fill=(200, 200, 200), font=font); ly += 34
items = [((160, 90, 210), "Touka behind the counter,\nfacing south (yaw 0)"),
         ((70, 200, 90), "the regulars' seats now:\nthe nearest row, 1-6"),
         ((230, 50, 50), "looked at, no room (a post)"),
         ((245, 245, 245), "the 35 in front of her:\n2 to 6 out, 3 to a side"),
         ((235, 125, 60), f"4.1.79's ring: any floor\n{near:g} to {far:g} round her\n(behind the counter too)")]
for colr, text in items:
    dr.rectangle([lx, ly + 2, lx + 16, ly + 18], fill=colr)
    dr.multiline_text((lx + 26, ly), text, fill=(230, 230, 230), font=font, spacing=3); ly += 22 * (text.count("\n") + 1) + 14
dr.multiline_text((lx, ly + 6), "framed squares: something at\nfeet height (walls, counter,\ntables, chairs); north is up,\none square a block", fill=(170, 170, 170), font=font, spacing=3)
out = sys.argv[1] if len(sys.argv) > 1 else "cafe.png"
img.save(out)
print("wrote", out, img.size)
