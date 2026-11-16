#!/usr/bin/env python3
"""4.1.82: floor plans of a structure template (data/hexkagune/structures/<name>.nbt) from above, one panel a floor, at the feet's height
given: the floor (air at the feet, something under) light, anything at the feet (walls, furniture, beds) in its own colour and framed;
its people as the template puts them (a dot and the way they face), and any extra spots marked.

    python3 -I tools/floor_plan.py <name> out.png --floor Y "title" [--floor Y "title" ...] [--crop X0 X1 Z0 Z1]
        [--mark X Y Z "label"] [--cell N] [--section Z "title"]

A spot marked or a person is drawn on the panel whose floor is its own (its Y), with its label beside it. 4.1.83: --section Z adds a
panel seen from the south, cut through row Z (x across, y up, the cropped x and every layer): who stands in that row, on what.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

import city_render as C  # noqa: E402
import structure_render as S  # noqa: E402

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def main():
    a = sys.argv[1:]
    name, out = a[0], a[1]
    floors, marks, crop, cell, section = [], [], None, 30, None
    i = 2
    while i < len(a):
        if a[i] == "--floor":
            floors.append((int(a[i + 1]), a[i + 2]))
            i += 3
        elif a[i] == "--mark":
            marks.append((int(a[i + 1]), int(a[i + 2]), int(a[i + 3]), a[i + 4]))
            i += 5
        elif a[i] == "--crop":
            crop = tuple(int(v) for v in a[i + 1:i + 5])
            i += 5
        elif a[i] == "--section":
            section = (int(a[i + 1]), a[i + 2])
            i += 3
        elif a[i] == "--cell":
            cell = int(a[i + 1])
            i += 2
        else:
            sys.exit("unknown " + a[i])
    grid, names, people = S.load(name)
    w, h, d = grid.shape
    x0, x1, z0, z1 = crop or (0, w, 0, d)
    air = [n.endswith(":air") or n.endswith("cave_air") for n in names]

    def isair(x, y, z):
        return not (0 <= x < w and 0 <= y < h and 0 <= z < d) or air[grid[x, y, z]]

    font, bold, big = ImageFont.truetype(FONT, 14), ImageFont.truetype(BOLD, 13), ImageFont.truetype(BOLD, 17)
    pad, top = 24, 34
    pw, ph = (x1 - x0) * cell, (z1 - z0) * cell
    sh = h * cell if section else 0
    img = Image.new("RGB", (max(len(floors) * (pw + pad) + pad, pw + 2 * pad), ph + top + pad + 8 + (sh + top + pad if section else 0)), (24, 24, 28))
    dr = ImageDraw.Draw(img)
    for f, (y, title) in enumerate(floors):
        ox, oz = pad + f * (pw + pad), top

        def px(x, z):
            return ox + (x - x0) * cell, oz + (z - z0) * cell
        dr.text((ox, 8), title, fill=(240, 240, 240), font=big)
        for x in range(x0, x1):
            for z in range(z0, z1):
                X, Z = px(x, z)
                if not isair(x, y, z):
                    c = tuple(int(v * 0.8) for v in C.colour(names[grid[x, y, z]])[:3])
                    dr.rectangle([X, Z, X + cell - 1, Z + cell - 1], fill=c, outline=(12, 12, 12), width=2)
                elif not isair(x, y - 1, z):
                    c = tuple(int(70 + v * 0.55) for v in C.colour(names[grid[x, y - 1, z]])[:3])
                    dr.rectangle([X, Z, X + cell - 1, Z + cell - 1], fill=c, outline=tuple(max(0, v - 18) for v in c))
                else:
                    dr.rectangle([X, Z, X + cell - 1, Z + cell - 1], fill=(36, 36, 42))
        spots = [(px_, py_, pz_, lab, (160, 90, 210), yaw) for (who, px_, py_, pz_), yaw in
                 ((p[:4], p[4] if len(p) > 4 else None) for p in people) for lab in [who.split(":")[1]]]
        spots += [(mx, my, mz, lab, (70, 200, 90), None) for mx, my, mz, lab in marks]
        for sx, sy, sz, lab, colour, yaw in spots:
            if sy != y or not (x0 <= sx < x1 and z0 <= sz < z1):
                continue
            X, Z = px(sx, sz)
            dr.ellipse([X + 3, Z + 3, X + cell - 4, Z + cell - 4], fill=colour, outline=(255, 255, 255), width=2)
            box = dr.textbbox((X + cell + 4, Z + cell / 2), lab, font=bold, anchor="lm")
            dr.rectangle([box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2], fill=(20, 20, 24))
            dr.text((X + cell + 4, Z + cell / 2), lab, fill=(255, 235, 160), font=bold, anchor="lm")
    dr.text((pad, ph + top + 6), "north up; a square a block" if pw < 420 else "north is up; one square a block; framed: something at feet height",
            fill=(170, 170, 170), font=font)
    if section:
        sz, title = section
        ox, oz = pad, ph + top + pad + 8 + top
        dr.text((ox, oz - top + 8), title, fill=(240, 240, 240), font=big)
        for x in range(x0, x1):
            for y in range(h):
                X, Y = ox + (x - x0) * cell, oz + (h - 1 - y) * cell
                if not isair(x, y, sz):
                    c = tuple(int(v * 0.85) for v in C.colour(names[grid[x, y, sz]])[:3])
                    dr.rectangle([X, Y, X + cell - 1, Y + cell - 1], fill=c, outline=(12, 12, 12))
                else:
                    behind = next((names[grid[x, y, zz]] for zz in range(sz - 1, -1, -1) if not isair(x, y, zz)), None)
                    c = tuple(int(30 + v * 0.25) for v in C.colour(behind)[:3]) if behind else (36, 36, 42)
                    dr.rectangle([X, Y, X + cell - 1, Y + cell - 1], fill=c)
        for who, px_, py_, pz_ in people:
            if pz_ == sz and x0 <= px_ < x1:
                X, Y = ox + (px_ - x0) * cell, oz + (h - 1 - py_) * cell
                dr.rectangle([X + 6, Y - cell + 4, X + cell - 7, Y + cell - 2], fill=(160, 90, 210), outline=(255, 255, 255), width=2)
                lab = who.split(":")[1]
                box = dr.textbbox((X + cell + 4, Y), lab, font=bold, anchor="lm")
                dr.rectangle([box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2], fill=(20, 20, 24))
                dr.text((X + cell + 4, Y), lab, fill=(255, 235, 160), font=bold, anchor="lm")
        dr.text((ox, oz + h * cell + 6), f"seen from the south, cut through row z {sz}; up is up; dim: what is behind the cut",
                fill=(170, 170, 170), font=font)
    img.save(out)
    print("wrote", out, img.size)


if __name__ == "__main__":
    main()
