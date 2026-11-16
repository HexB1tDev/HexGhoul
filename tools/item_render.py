"""4.1.17: a textured look at an item model (the quinques' real faces, their patches per face), as the game draws its elements.

    python3 tools/item_render.py <model.json> <texture.png> <out.png>

Render a Minecraft item/block model JSON (elements, one-axis rotations, per-face UVs) with its texture, orthographic, painter's sort."""
import json, math, sys
from PIL import Image, ImageDraw

def rot(p, axis, ang, origin):
    a = math.radians(ang); c, s = math.cos(a), math.sin(a)
    x, y, z = (p[i] - origin[i] for i in range(3))
    if axis == 'x': y, z = y * c - z * s, y * s + z * c
    elif axis == 'y': x, z = x * c + z * s, -x * s + z * c
    else: x, y = x * c - y * s, x * s + y * c
    return (x + origin[0], y + origin[1], z + origin[2])

FACES = {  # corners (as from/to picks) in MC's order with uv corners
    'north': [('t','t','f'), ('f','t','f'), ('f','f','f'), ('t','f','f')],
    'south': [('f','t','t'), ('t','t','t'), ('t','f','t'), ('f','f','t')],
    'east':  [('t','t','t'), ('t','t','f'), ('t','f','f'), ('t','f','t')],
    'west':  [('f','t','f'), ('f','t','t'), ('f','f','t'), ('f','f','f')],
    'up':    [('f','t','f'), ('t','t','f'), ('t','t','t'), ('f','t','t')],
    'down':  [('f','f','t'), ('t','f','t'), ('t','f','f'), ('f','f','f')],
}

def render(model_path, tex_path, view, size=600, scale=None, centre=None):
    m = json.load(open(model_path)); tex = Image.open(tex_path).convert('RGBA'); tw = tex.width
    yaw, pitch = view
    polys = []
    for el in m['elements']:
        f, t = el['from'], el['to']
        for name, corners in FACES.items():
            face = el['faces'].get(name)
            if not face: continue
            pts = [tuple((f if c[i] == 'f' else t)[i] for i in range(3)) for c in corners]
            if 'rotation' in el:
                r = el['rotation']; pts = [rot(p, r['axis'], r['angle'], r['origin']) for p in pts]
            u0, v0, u1, v1 = face['uv']
            # sample the face's texture at a grid
            polys.append((pts, (u0, v0, u1, v1), name))
    def view_pt(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        a = math.radians(yaw); b = math.radians(pitch)
        x, z = x * math.cos(a) - z * math.sin(a), x * math.sin(a) + z * math.cos(a)
        y, z = y * math.cos(b) - z * math.sin(b), y * math.sin(b) + z * math.cos(b)
        return x, y, z
    allp = [view_pt(p) for poly in polys for p in poly[0]]
    minx = min(p[0] for p in allp); maxx = max(p[0] for p in allp); miny = min(p[1] for p in allp); maxy = max(p[1] for p in allp)
    sc = scale or (size * 0.9) / max(maxx - minx, maxy - miny)
    cx, cy = centre or ((minx + maxx) / 2, (miny + maxy) / 2)
    img = Image.new('RGBA', (size, size), (52, 58, 68, 255))
    tris = []
    light = {'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6, 'up': 1.0, 'down': 0.5}
    for pts, uv, name in polys:
        vp = [view_pt(p) for p in pts]
        # back-face cull
        ax, ay = vp[1][0] - vp[0][0], vp[1][1] - vp[0][1]; bx, by = vp[3][0] - vp[0][0], vp[3][1] - vp[0][1]
        n = 6  # subdivide the face into n x n cells, each coloured from the texture
        u0, v0, u1, v1 = uv
        for i in range(n):
            for j in range(n):
                def at(a, b):
                    # bilinear across the quad corners 0..3 (0 top-left u0v0, 1 top-right, 2 bottom-right, 3 bottom-left)
                    top = [vp[0][k] + (vp[1][k] - vp[0][k]) * a for k in range(3)]
                    bot = [vp[3][k] + (vp[2][k] - vp[3][k]) * a for k in range(3)]
                    return [top[k] + (bot[k] - top[k]) * b for k in range(3)]
                q = [at(i / n, j / n), at((i + 1) / n, j / n), at((i + 1) / n, (j + 1) / n), at(i / n, (j + 1) / n)]
                uu = u0 + (u1 - u0) * (i + 0.5) / n; vv = v0 + (v1 - v0) * (j + 0.5) / n
                px = tex.getpixel((min(tw - 1, max(0, int(uu / 16 * tw))), min(tw - 1, max(0, int(vv / 16 * tw)))))
                if px[3] < 10: continue
                depth = sum(p[2] for p in q) / 4
                col = tuple(int(c * light[name]) for c in px[:3])
                tris.append((depth, q, col))
    tris.sort(key=lambda t: -t[0])
    d = ImageDraw.Draw(img)
    for depth, q, col in tris:
        d.polygon([(size / 2 + (p[0] - cx) * sc, size / 2 - (p[1] - cy) * sc) for p in q], fill=col)
    return img

if __name__ == '__main__':
    model, tex, out = sys.argv[1:4]
    views = [(0, 0), (180, 0), (35, 25), (90, 0)]
    imgs = [render(model, tex, v, 500) for v in views]
    sheet = Image.new('RGB', (500 * len(imgs), 500))
    for i, im in enumerate(imgs): sheet.paste(im, (500 * i, 0))
    sheet.save(out)
