"""4.1.57: the CCG building's glass from its template, before and after tools/join_panes.py: its front seen from outside and an office
floor seen from above, each pane drawn as the game draws it (a post, and glass out to each side it is joined on).

    git show <old>:src/main/resources/data/hexkagune/structures/ccg_building/t_0_0.nbt > before.nbt
    python3 tools/glass_render.py before.nbt src/main/resources/data/hexkagune/structures/ccg_building/t_0_0.nbt out.png
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import join_panes as J  # noqa: E402
from PIL import Image, ImageDraw, ImageFont
from collections import Counter

def world(path):
    name, root, raw = J.load(Path(path))
    pal = root['palette'][1][1]
    states = [(e['Name'][1], {k: v[1] for k, v in e.get('Properties', (10, {}))[1].items()}) for e in pal]
    w = {}
    for b in root['blocks'][1][1]:
        p = b['pos'][1][1]
        w[tuple(p)] = states[b['state'][1]]
    return w, root['size'][1][1]

before, size = world(sys.argv[1])
after, _ = world(sys.argv[2])
out = sys.argv[3]
# The facade with the most panes, and an office floor's height with many.
panes = [p for p, (n, _) in after.items() if J.connecting(n)]
zs = Counter(p[2] for p in panes)
face_z = min(zs, key=lambda z: (-zs[z], z))
ys = Counter(p[1] for p in panes)
floor_y = max(ys, key=lambda y: (ys[y], -y))
CELL = 16
def colour(n):
    if 'glass' in n: return (150, 190, 215)
    if 'quartz' in n: return (230, 226, 216)
    if 'concrete' in n or 'stone' in n: return (140, 140, 140)
    if 'planks' in n or 'log' in n: return (150, 115, 70)
    return (175, 170, 160)
def pane(d, x0, y0, props, top_view):
    # top view: a post in the middle and an arm to each joined side; front view: the glass across to each joined side (east/west)
    c = (70, 120, 160)
    m = CELL // 2
    t = 3
    d.rectangle([x0 + m - t, y0 + m - t, x0 + m + t, y0 + m + t], fill=c)
    arms = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)} if top_view else {'west': (-1, 0), 'east': (1, 0)}
    for side, (dx, dy) in arms.items():
        if props.get(side) == 'true':
            if dx:
                xa, xb = (x0, x0 + m) if dx < 0 else (x0 + m, x0 + CELL)
                d.rectangle([xa, y0 + m - (t if top_view else m), xb, y0 + m + (t if top_view else m - 1)], fill=c)
            else:
                ya, yb = (y0, y0 + m) if dy < 0 else (y0 + m, y0 + CELL)
                d.rectangle([x0 + m - t, ya, x0 + m + t, yb], fill=c)
    if not top_view:
        d.rectangle([x0 + m - 1, y0, x0 + m + 1, y0 + CELL - 1], fill=c)

def draw_top(d, w, ox, oy):
    for x in range(size[0]):
        for z in range(size[2]):
            st = w.get((x, floor_y, z))
            x0, y0 = ox + x * CELL, oy + z * CELL
            d.rectangle([x0, y0, x0 + CELL - 1, y0 + CELL - 1], fill=(238, 242, 246))
            if st is None or st[0].endswith(':air'):
                continue
            if J.connecting(st[0]):
                pane(d, x0, y0, st[1], True)
            else:
                d.rectangle([x0, y0, x0 + CELL - 1, y0 + CELL - 1], fill=colour(st[0]))

def draw_front(d, w, ox, oy, ys_shown):
    for i, y in enumerate(ys_shown):
        for x in range(size[0]):
            st = w.get((x, y, face_z))
            x0, y0 = ox + x * CELL, oy + i * CELL
            d.rectangle([x0, y0, x0 + CELL - 1, y0 + CELL - 1], fill=(238, 242, 246))
            if st is None or st[0].endswith(':air'):
                continue
            if J.connecting(st[0]):
                pane(d, x0, y0, st[1], False)
            else:
                d.rectangle([x0, y0, x0 + CELL - 1, y0 + CELL - 1], fill=colour(st[0]))

ys_shown = list(range(min(floor_y + 8, size[1] - 1), max(floor_y - 10, 0), -1))
W = size[0] * CELL
Hf = len(ys_shown) * CELL
Ht = size[2] * CELL
img = Image.new('RGB', (W * 2 + 90, Hf + Ht + 160), (24, 30, 40))
d = ImageDraw.Draw(img)
try:
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 20)
    small = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 15)
except Exception:
    font = small = ImageFont.load_default()
d.text((20, 12), 'The CCG building\'s glass, from its template: before (4.1.56) and after (4.1.57)', fill=(240, 240, 240), font=font)
for k, (w, label) in enumerate(((before, '4.1.56: every pane a lone post (3134 of 3134)'), (after, '4.1.57: each joined to what it touches'))):
    ox = 30 + k * (W + 30)
    d.text((ox, 46), label, fill=(240, 120, 110) if k == 0 else (130, 220, 150), font=small)
    d.text((ox, 70), f'its front (z {face_z}), seen from outside', fill=(200, 200, 200), font=small)
    draw_front(d, w, ox, 92, ys_shown)
    d.text((ox, 100 + Hf), f'an office floor seen from above (y {floor_y})', fill=(200, 200, 200), font=small)
    draw_top(d, w, ox, 124 + Hf)
img.save(out)
print('wrote', out, 'face z', face_z, 'floor y', floor_y)
