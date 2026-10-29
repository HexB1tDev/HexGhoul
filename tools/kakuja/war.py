"""War: the One-Eyed King. Additive kakuja geometry on the player model, after the War reference sheet
(tools/kakuja/reference/war.webp). In order of visual weight: a huge crescent blade fused to the whole
right arm, a dense spiked mass around the head with the pale forehead ring, a wide ragged cape from the
shoulders to the ankles, a high jagged collar over spiked shoulders, and six thin angular rinkaku that
rise above the head and hook down like spider legs. Mostly black and charcoal, dark crimson plates and
only thin lines of glowing blood red."""
import math

import numpy as np

from geo import Model, Swatches, frame, humanoid, unit

SWATCHES = Swatches([
    "black", "black_plate", "charcoal", "crimson", "crimson_vein", "blood", "bone", "maroon",
    "maroon_stripe", "steel", "eye", "pupil", "ring", "black_vein", "crimson_plate", "ash",
])

MATERIALS = {
    "black": ("noise", ("#131012", 0.1), False),
    "black_plate": ("plate", ("#151214", "#362f33"), False),
    "charcoal": ("noise", ("#2b2729", 0.1), False),
    "crimson": ("noise", ("#5e0c14", 0.12), False),
    "crimson_vein": ("veins", ("#4a0a10", "#d8142a", 1), "veins"),
    "blood": ("noise", ("#e3122a", 0.08), True),
    "bone": ("noise", ("#f1ebe2", 0.04), True),
    "maroon": ("noise", ("#3d0a11", 0.1), False),
    "maroon_stripe": ("stripes", ("#3d0a11", "#1a0507"), False),
    "steel": ("plate", ("#77716f", "#a9a3a1"), False),
    "eye": ("noise", ("#f2ede6", 0.03), False),
    "pupil": ("noise", ("#140102", 0.05), False),
    "ring": ("ring", ("#151214", "#efe8de", "#2a2628"), False),
    "black_vein": ("veins", ("#141113", "#a80e1c", 1), "veins"),
    "crimson_plate": ("plate", ("#560a12", "#8e1220"), False),
    "ash": ("noise", ("#56504f", 0.08), False),
}


def spike(m, bone, base, direction, length, width, swatch, steps=3):
    direction = unit(direction)
    hint = np.cross(direction, (0.0, 1.0, 0.0))
    if np.linalg.norm(hint) < 1e-3:
        hint = np.array([1.0, 0.0, 0.0])
    m.taper(bone, base, np.asarray(base) + direction * length, width, 0.35, steps, 0.75, swatch, x_hint=hint)


def head(m):
    """The mask and the dense crown of shards around the whole head; the chin stays the player's own."""
    h = "head"
    rng = m.random
    # Shell over the head, slightly larger than the hat layer.
    m.box(h, (0, 33.0, 0.3), (9.6, 1.6, 9.6), swatch="black_plate")
    m.box(h, (0, 28.6, 4.75), (9.6, 9.0, 1.1), swatch="black")
    m.box(h, (4.75, 28.6, 0.3), (1.1, 9.0, 9.6), swatch="black_plate")
    m.box(h, (-4.75, 28.6, 0.3), (1.1, 9.0, 9.6), swatch="black")
    # Face: dark mask down to the mouth, a pale ring on the forehead, one white eye on the left.
    m.box(h, (0, 29.4, -4.65), (9.6, 7.4, 1.0), swatch="black_plate")
    m.box(h, (-3.2, 25.4, -4.55), (3.2, 1.2, 0.9), swatch="charcoal")
    m.box(h, (3.4, 25.4, -4.55), (2.8, 1.2, 0.9), swatch="black")
    # An octagonal ring (bars with the corners cut, plus corner steps) reads round at this size.
    for (cx, cy, sx, sy) in ((0, 32.1, 2.0, 0.8), (0, 28.5, 2.0, 0.8), (-1.75, 30.3, 0.8, 2.0), (1.75, 30.3, 0.8, 2.0),
                             (-1.25, 31.55, 0.8, 0.8), (1.25, 31.55, 0.8, 0.8), (-1.25, 29.05, 0.8, 0.8), (1.25, 29.05, 0.8, 0.8)):
        m.box(h, (cx - 0.6, cy, -5.3), (sx, sy, 0.5), swatch="bone")
    m.box(h, (-0.6, 30.3, -5.2), (2.2, 2.6, 0.3), swatch="charcoal")
    m.box(h, (2.1, 27.0, -5.2), (2.0, 1.4, 0.3), swatch="eye")
    m.box(h, (2.3, 26.95, -5.35), (0.9, 0.9, 0.2), swatch="pupil")
    m.box(h, (-2.4, 27.1, -5.2), (1.9, 0.6, 0.3), swatch="blood")
    m.rod(h, (-4.6, 25.9, -5.25), (-1.4, 28.4, -5.25), 0.4, 0.2, swatch="blood", x_hint=(0, 0, 1))
    # Crown of shards: longest on top and at the back, short at the temples, none over the face.
    centre = np.array([0.0, 28.0, 0.8])
    for count in range(46):
        face = ("top", "top", "top", "back", "back", "left", "right", "brow")[count % 8]
        if face == "top":
            p = np.array([rng.uniform(-4.6, 4.6), 33.7, rng.uniform(-4.2, 4.8)])
        elif face == "back":
            p = np.array([rng.uniform(-4.6, 4.6), rng.uniform(25.5, 33.4), 5.3])
        elif face == "brow":
            p = np.array([rng.uniform(-4.6, 4.6), rng.uniform(32.6, 33.6), -5.0])
        else:
            p = np.array([4.9 if face == "left" else -4.9, rng.uniform(26.5, 33.4), rng.uniform(-3.0, 4.8)])
        out = unit(p - centre)
        top = (p[1] - 25.0) / 8.6
        back = max(0.0, p[2]) / 4.8
        direction = unit(out + np.array([0.0, 0.55 + 0.4 * top, 0.25 + 0.2 * back]) + np.array([rng.uniform(-0.25, 0.25) for _ in range(3)]))
        length = 3.5 + 5.5 * max(top, back) + rng.uniform(-1.0, 1.5)
        width = rng.uniform(2.2, 3.4)
        swatch = ("black", "black_plate", "charcoal", "black", "crimson_plate", "black_vein")[count % 6]
        spike(m, h, p, direction, length, width, swatch)


def collar(m):
    """High jagged collar rising to the jaw, flaring over both shoulders."""
    b = "body"
    m.box(b, (0, 24.7, 0.2), (10.0, 1.6, 6.0), swatch="black_plate")
    rng = m.random
    for k in range(14):
        a = math.radians(-115 + 230 * k / 13.0)
        base = np.array([4.7 * math.sin(a), 24.9, 2.9 * math.cos(a) + 0.2])
        tall = 3.6 + 2.4 * (0.5 + 0.5 * math.cos(a)) + rng.uniform(-0.6, 0.8)
        direction = unit([math.sin(a) * 0.5, 1.0, math.cos(a) * 0.35])
        m.rod(b, base, base + direction * tall, 2.8, 0.9, "black_plate" if k % 3 else "crimson_plate", x_hint=(math.cos(a), 0, -math.sin(a)))
        spike(m, b, base + direction * tall * 0.8, direction + np.array([math.sin(a) * 0.4, 0, 0]), 2.2, 1.3, "black", 2)
    # Spiked flare over the left shoulder; the right one is part of the kakuja arm.
    for k in range(4):
        base = np.array([5.0 + k * 1.2, 24.6 - k * 0.5, -1.2 + k * 0.9])
        spike(m, b, base, (0.7 + k * 0.1, 1.0, 0.25), 3.0 + k * 0.6, 2.0, "black_plate" if k % 2 else "black")


def torso(m):
    b = "body"
    m.box(b, (0, 18.5, -2.45), (8.8, 11.0, 0.9), swatch="black_plate")
    m.box(b, (0, 18.0, 2.45), (8.8, 12.0, 0.9), swatch="black")
    m.box(b, (-4.45, 18.5, 0), (0.9, 11.0, 4.8), swatch="charcoal")
    m.box(b, (4.45, 18.5, 0), (0.9, 11.0, 4.8), swatch="charcoal")
    # Crossed dark crimson straps, a thin glowing seam on one, and the glowing cross on the belt.
    m.rod(b, (-4.3, 23.6, -3.0), (4.3, 14.6, -3.0), 1.3, 0.45, swatch="crimson_plate", x_hint=(0, 0, 1))
    m.rod(b, (4.3, 23.6, -3.0), (-4.3, 14.6, -3.0), 1.3, 0.45, swatch="crimson_plate", x_hint=(0, 0, 1))
    m.rod(b, (3.9, 23.0, -3.25), (-3.9, 15.2, -3.25), 0.3, 0.15, swatch="blood", x_hint=(0, 0, 1))
    m.box(b, (0, 12.7, 0), (9.2, 1.6, 5.2), swatch="black_plate")
    m.box(b, (0, 13.9, -2.95), (0.9, 3.2, 0.4), swatch="blood")
    m.box(b, (0, 14.5, -2.95), (2.8, 0.9, 0.4), swatch="blood")


def blade_normal(points, k):
    """Spine direction of segment k and the direction its width runs in: from the outer spine towards
    the body and a little back, so the flat of the blade faces the front and the outside."""
    d = unit(points[k + 1] - points[k])
    towards = np.array([1.0, 0.1, 0.45])
    return d, unit(towards - np.dot(towards, d) * d)


def right_arm(m):
    """The kakuja arm: a massive spiked pauldron, layered plates, and the crescent blade along the arm."""
    a = "right_arm"
    rng = m.random
    # Pauldron: stacked plates over the shoulder, with shards up and out.
    m.box(a, (-6.6, 24.8, 0.1), (6.6, 1.8, 6.2), swatch="black_plate")
    m.box(a, (-7.4, 23.2, 0.0), (5.4, 2.0, 5.8), swatch="crimson_plate")
    m.box(a, (-8.0, 21.6, 0.0), (4.6, 1.8, 5.4), swatch="black_plate")
    for k in range(7):
        base = np.array([-5.0 - k * 1.05, 25.6 - k * 0.55, -2.0 + (k % 3) * 1.8])
        spike(m, a, base, (-0.65 - k * 0.08, 1.0, 0.15 * ((k % 3) - 1)), 4.5 + rng.uniform(0, 3.0), 2.4, "black_plate" if k % 2 else "black")
    # Layered plates down the arm, offset like overlapping scales.
    for k in range(5):
        y = 19.8 - k * 1.9
        m.box(a, (-6.1 - 0.3 * (k % 2), y, 0.15 * (-1) ** k), (5.0 + 0.4 * (k % 2), 1.9, 5.0), swatch="black_plate" if k % 2 else "charcoal")
    # The blade: from the top of the shoulder, bulging out and down, then curving forwards to the ground.
    spine = [np.array(p, dtype=float) for p in ((-9.5, 25.0, 0.8), (-15.5, 18.0, 0.2), (-18.0, 10.5, -1.5), (-16.5, 4.0, -4.5), (-12.0, 0.8, -8.5))]
    widths = (5.5, 8.5, 9.0, 6.5, 1.0)
    parent = a
    for k in range(4):
        name = m.bone(f"blade{k + 1}", parent, spine[k])
        d, inward = blade_normal(spine, k)
        thick = unit(np.cross(inward, d))
        f = np.column_stack([inward, d, thick])
        p0, p1 = spine[k], spine[k + 1]
        for s in range(3):
            t0, t1 = s / 3.0, (s + 1) / 3.0
            q0, q1 = p0 + (p1 - p0) * t0, p0 + (p1 - p0) * t1
            mid = (q0 + q1) / 2.0
            w = widths[k] + (widths[k + 1] - widths[k]) * (s + 0.5) / 3.0
            length = float(np.linalg.norm(q1 - q0)) + 0.4
            # Body of the blade, its pale cutting face, the glowing edge and the bare steel edge.
            m.box(name, mid + inward * (w / 2.0 - 1.2), (w, length, 2.2), f, "black_plate" if (k + s) % 2 else "black")
            m.box(name, mid + inward * (w * 0.62 - 0.6) - thick * 0.0, (w * 0.42, length * 0.92, 2.5), f, "ash" if (k + s) % 2 else "steel")
            m.box(name, mid + inward * (w - 0.9), (0.8, length, 2.7), f, "blood")
            m.box(name, mid + inward * (w - 0.2), (0.7, length, 1.2), f, "steel")
            # Raised kakuja plates along the spine and serrations on the outer edge.
            m.box(name, mid - inward * 0.9, (1.8, length * 0.85, 3.2), f, "crimson_plate" if s == 1 else "black_plate")
            if k < 3:
                spike(m, name, mid - inward * 1.6, -inward + d * 0.6, 2.2 + rng.uniform(0, 1.2), 1.4, "black", 2)
        parent = name


def left_arm(m):
    a = "left_arm"
    m.box(a, (6.1, 24.5, 0), (5.0, 1.4, 5.0), swatch="black_plate")
    m.box(a, (6.2, 19.2, 0), (4.7, 1.8, 4.7), swatch="charcoal")
    m.box(a, (6.2, 15.6, 0), (4.7, 1.4, 4.7), swatch="black")


def tendrils(m):
    """Six thin angular rinkaku: they leave the shoulder blades, rise past the head, knee outwards and
    hook down to sharp tips at waist height. The upper pair is the highest, the lower pair the widest."""
    shapes = [
        [(1.0, 4.0, 3.5), (5.5, 39.0, 6.0), (10.5, 47.5, 4.5), (16.0, 44.0, 2.0), (19.0, 35.0, 0.0), (20.0, 26.0, -2.0)],
        [(1.5, 3.0, 3.5), (8.0, 33.5, 6.5), (14.5, 39.0, 5.0), (20.0, 34.5, 2.5), (22.5, 26.0, 0.5), (23.0, 18.5, -1.5)],
        [(1.5, 2.5, 3.5), (7.5, 26.5, 6.5), (13.5, 30.0, 5.5), (18.0, 26.5, 3.5), (20.0, 19.0, 2.0), (20.5, 12.0, 0.5)],
    ]
    widths = [2.4, 2.2, 2.0, 1.7, 1.3, 0.9]
    names = []
    for side in (-1, 1):
        for level, shape in enumerate(shapes):
            root = np.array([side * 2.4, 21.5 - 2.2 * level, 2.9])
            points = [root, root + np.array([side * shape[0][0], shape[0][1], shape[0][2]])]
            for (x, y, z) in shape[1:]:
                points.append(np.array([side * x, y, z]))

            def detail(bone, k, p0, p1, side=side):
                if k in (1, 2, 3):
                    m.box(bone, p0, (widths[k] + 0.7, widths[k] + 0.7, widths[k] + 0.7), frame(p1 - p0), "black_plate")
                if k >= 2:
                    # A thin glowing line along the outside of the leg.
                    out = unit(np.cross(p1 - p0, (0.0, 0.0, 1.0)) * side)
                    m.rod(bone, p0 + out * widths[k] * 0.42, p1 + out * widths[k] * 0.32, 0.35, 0.35, "blood", x_hint=(0, 0, 1))

            chain = m.chain(f"tendril_{'l' if side > 0 else 'r'}{level + 1}_", "body", points, widths,
                            "crimson" if level == 1 else "black_vein", depth_ratio=1.0, extra=detail)
            spike(m, chain[-1], points[-1], points[-1] - points[-2], 3.0, 1.0, "crimson_plate", 2)
            names.append(chain)
    return names


def cape(m):
    """A wide ragged cape in two layers of torn strips, from the shoulders to the ankles, flaring out and
    back; three bones per strip so it can flutter. Crimson strands also hang off the left hip."""
    rng = m.random
    strips = []
    for k in range(9):
        x = -6.0 + k * 1.5
        strips.append(dict(top=(x, 24.3, 2.8), spread=1.9, back=0.22, widths=(3.0, 3.8, 4.4), lengths=(7.5, 8.0, 6.5),
                           swatch=("black", "maroon_stripe", "black", "maroon")[k % 4]))
    for k in range(8):
        x = -5.25 + k * 1.5
        strips.append(dict(top=(x, 23.6, 3.5), spread=2.1, back=0.3, widths=(2.6, 3.2, 3.8), lengths=(7.0, 7.5, 5.5),
                           swatch=("crimson", "black_plate", "crimson_plate", "black")[k % 4]))
    for side, count in ((1, 4), (-1, 2)):
        for k in range(count):
            strips.append(dict(top=(side * (4.6 + 0.5 * k), 23.0 - k * 1.2, 1.8 - k * 1.0), spread=None, side=side,
                               back=0.1, widths=(2.0, 2.2, 2.0), lengths=(6.5, 6.5, 5.0),
                               swatch=("crimson", "maroon_stripe", "crimson_plate", "black")[k % 4]))
    names = []
    for i, strip in enumerate(strips):
        top = np.array(strip["top"], dtype=float)
        points = [top]
        for j, length in enumerate(strip["lengths"]):
            length += rng.uniform(-0.8, 1.2)
            if strip.get("spread") is not None:
                out = np.array([top[0] * (strip["spread"] - 1.0) / 22.0, 0.0, strip["back"]])
            else:
                out = np.array([strip["side"] * (0.28 + 0.1 * j), 0.0, strip["back"]])
            points.append(points[-1] + unit(np.array([0.0, -1.0, 0.0]) + out) * length)
        hint = (1, 0, 0) if strip.get("spread") is not None else (0, 0, 1)
        parent = "body"
        chain = []
        for j in range(3):
            name = m.bone(f"cape{i + 1}_{j + 1}", parent, points[j])
            w = strip["widths"][j]
            m.rod(name, points[j], points[j + 1], w, 0.75, strip["swatch"], x_hint=hint)
            if j == 2:
                d = unit(points[3] - points[2])
                side = np.array(hint, dtype=float)
                for t, (offset, scale) in enumerate(((-0.3, 0.45), (0.25, 0.35), (0.0, 0.2))):
                    end = points[3] + side * w * offset
                    m.rod(name, end, end + d * rng.uniform(0.8, 3.2), w * scale, 0.65, strip["swatch"] if t else "black", x_hint=hint)
            chain.append(name)
            parent = name
        names.append(chain)
    return names


def legs(m):
    for side, bone in ((-1, "right_leg"), (1, "left_leg")):
        x = side * 1.95
        m.box(bone, (x, 1.6, -0.1), (4.7, 3.2, 4.9), swatch="black_plate")
        m.box(bone, (x, 3.6, -0.1), (4.9, 0.8, 5.1), swatch="black")
        m.box(bone, (x, 7.6, -2.3), (3.6, 2.4, 0.6), swatch="charcoal")


def build():
    m = Model("geometry.hexkagune.war", SWATCHES, seed=3)
    humanoid(m)
    head(m)
    collar(m)
    torso(m)
    right_arm(m)
    left_arm(m)
    legs(m)
    tendril_chains = tendrils(m)
    cape_chains = cape(m)
    return m, tendril_chains, cape_chains
