#!/usr/bin/env python3
"""2.0.30: a block/item model JSON drawn off-game, so its shape can be looked at closely (no game, no OpenGL).

    python3 tools/model_preview.py <model.json> <texture.png> <out.png> [--size 900] [--view side|top|end|three] [--zoom x0,x1]

Every element is a box (from/to in model pixels, its one rotation about x/y/z through its origin, "rescale" ignored); each face is
filled with the mean colour of its uv patch on the texture, shaded by its normal against a fixed light, and drawn back to front
(painter's order). --zoom draws only the stretch x0..x1 of the model along x (the lance's length), scaled up to the image.
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

FACES = {
    "north": ((0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), (0, 0, -1)),
    "south": ((0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1), (0, 0, 1)),
    "west": ((0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0), (-1, 0, 0)),
    "east": ((1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0), (1, 0, 0)),
    "down": ((0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1), (0, -1, 0)),
    "up": ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1), (0, 1, 0)),
}


def turn(p, rot):
    if not rot:
        return p
    a = math.radians(rot["angle"])
    o = rot["origin"]
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    c, s = math.cos(a), math.sin(a)
    if rot["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    elif rot["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def mean(tex, uv, size):
    k = tex.width / 16.0
    x0, y0, x1, y1 = uv
    box = (int(min(x0, x1) * k), int(min(y0, y1) * k), max(int(min(x0, x1) * k) + 1, int(max(x0, x1) * k)),
           max(int(min(y0, y1) * k) + 1, int(max(y0, y1) * k)))
    px = [tex.getpixel((x, y)) for x in range(box[0], box[2]) for y in range(box[1], box[3])]
    px = [p for p in px if p[3] > 0] or px
    return tuple(sum(p[i] for p in px) / len(px) for i in range(3))


def view_matrix(view):
    # Returns a function: model point -> (screen x, screen y, depth). Model x is the lance's length.
    if view == "top":
        return lambda p: (p[0], -p[2], p[1])
    if view == "end":
        return lambda p: (p[2], -p[1], p[0])
    if view == "three":
        ya, pa = math.radians(-28), math.radians(22)

        def f(p):
            x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
            x, z = x * math.cos(ya) + z * math.sin(ya), -x * math.sin(ya) + z * math.cos(ya)
            y, z = y * math.cos(pa) - z * math.sin(pa), y * math.sin(pa) + z * math.cos(pa)
            return (x, -y, z)
        return f
    return lambda p: (p[0], -p[1], p[2])


def render(model, tex, out, size=900, view="side", zoom=None):
    light = (0.35, 0.8, 0.5)
    n = math.sqrt(sum(v * v for v in light))
    light = tuple(v / n for v in light)
    project = view_matrix(view)
    polys = []
    for el in model["elements"]:
        if zoom is not None and (max(el["from"][0], el["to"][0]) < zoom[0] or min(el["from"][0], el["to"][0]) > zoom[1]):
            continue
        a, b = el["from"], el["to"]
        rot = el.get("rotation")
        for name, face in el.get("faces", {}).items():
            c0, c1, c2, c3, normal = FACES[name]
            pts = [turn(tuple(a[i] + (b[i] - a[i]) * c[i] for i in range(3)), rot) for c in (c0, c1, c2, c3)]
            nrm = turn(normal, {"angle": rot["angle"], "axis": rot["axis"], "origin": (0, 0, 0)}) if rot else normal
            colour = mean(tex, face.get("uv", [0, 0, 16, 16]), tex.width)
            shade = 0.45 + 0.55 * max(0.0, sum(nrm[i] * light[i] for i in range(3)))
            scr = [project(p) for p in pts]
            depth = sum(s[2] for s in scr) / 4
            polys.append((depth, [(s[0], s[1]) for s in scr], tuple(int(min(255, c * shade)) for c in colour)))
    xs = [x for _, pts, _ in polys for x, _ in pts]
    ys = [y for _, pts, _ in polys for _, y in pts]
    w, h = max(xs) - min(xs), max(ys) - min(ys)
    scale = 0.92 * size / max(w, h, 1e-6)
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    img = Image.new("RGB", (size, size), (196, 204, 214))
    draw = ImageDraw.Draw(img)
    for depth, pts, colour in sorted(polys, key=lambda t: t[0]):
        draw.polygon([((x - cx) * scale + size / 2, (y - cy) * scale + size / 2) for x, y in pts], fill=colour, outline=None)
    img.save(out)
    return len(polys)


def main():
    args = sys.argv[1:]
    model_path, tex_path, out = args[0], args[1], args[2]
    size = int(args[args.index("--size") + 1]) if "--size" in args else 900
    view = args[args.index("--view") + 1] if "--view" in args else "side"
    zoom = tuple(float(v) for v in args[args.index("--zoom") + 1].split(",")) if "--zoom" in args else None
    model = json.loads(Path(model_path).read_text())
    tex = Image.open(tex_path).convert("RGBA")
    print(render(model, tex, out, size, view, zoom), "faces")


if __name__ == "__main__":
    main()
