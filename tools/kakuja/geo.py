"""Small toolkit for writing Blockbench/GeckoLib .geo.json models from code.

Coordinates are Bedrock model coordinates as Blockbench shows them: pixels, feet at y = 0, the
model faces -z (north), the character's right side is -x. A cube can be given any orientation as a
frame (its local x, y, z axes); the toolkit converts that to the Euler angles GeckoLib expects, so
the same file renders identically in Blockbench, in GeckoLib and in the mod's own runtime.
"""
import json
import math
import random

import numpy as np

FLIP = np.diag([-1.0, 1.0, 1.0])


def unit(v):
    v = np.asarray(v, dtype=float)
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


def frame(y_axis, x_hint=(1.0, 0.0, 0.0)):
    """Orthonormal frame whose local +y points along y_axis, local +x as close to x_hint as possible."""
    y = unit(y_axis)
    x = np.asarray(x_hint, dtype=float)
    x = x - np.dot(x, y) * y
    if np.linalg.norm(x) < 1e-6:
        x = np.cross(y, (0.0, 0.0, 1.0)) if abs(y[2]) < 0.9 else np.cross(y, (1.0, 0.0, 0.0))
    x = unit(x)
    z = np.cross(x, y)
    return np.column_stack([x, y, z])


def rotation(axis, degrees):
    """Rotation matrix about an axis, in model coordinates."""
    a = unit(axis)
    t = math.radians(degrees)
    c, s = math.cos(t), math.sin(t)
    x, y, z = a
    return np.array([
        [c + x * x * (1 - c), x * y * (1 - c) - z * s, x * z * (1 - c) + y * s],
        [y * x * (1 - c) + z * s, c + y * y * (1 - c), y * z * (1 - c) - x * s],
        [z * x * (1 - c) - y * s, z * y * (1 - c) + x * s, c + z * z * (1 - c)]])


def euler(r):
    """Bedrock rotation (degrees) for a frame, under GeckoLib's x-mirrored Z*Y*X convention."""
    g = FLIP @ np.asarray(r, dtype=float) @ FLIP
    b = -math.asin(max(-1.0, min(1.0, g[2, 0])))
    if abs(g[2, 0]) > 1.0 - 1e-12:
        # Gimbal lock (local x along the model's z): put the whole turn on x, none on z.
        c = 0.0
        a = math.atan2(g[0, 1], g[1, 1]) if g[2, 0] < 0 else math.atan2(-g[0, 1], g[1, 1])
    else:
        a = math.atan2(g[2, 1], g[2, 2])
        c = math.atan2(g[1, 0], g[0, 0])
    angles = [-math.degrees(a), -math.degrees(b), math.degrees(c)]
    if np.max(np.abs(geckolib_matrix(angles) - np.asarray(r, dtype=float))) > 1e-6:
        raise ValueError("frame is not a rotation: %s" % r)
    return angles


def geckolib_matrix(angles):
    """What GeckoLib (and the mod) build from Bedrock angles, seen back in model coordinates."""
    a, b, c = math.radians(-angles[0]), math.radians(-angles[1]), math.radians(angles[2])
    rx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    rz = np.array([[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]])
    return FLIP @ (rz @ ry @ rx) @ FLIP


def r3(v):
    return [round(float(x), 4) for x in v]


class Swatches:
    """A texture made of 16x16 swatches. Each face samples a patch the size of the face."""

    def __init__(self, names, columns=8, size=16):
        self.names = list(names)
        self.columns = columns
        self.size = size
        rows = (len(self.names) + columns - 1) // columns
        self.width = columns * size
        self.height = max(1, rows) * size
        self.width = self.height = max(self.width, self.height)

    def origin(self, name):
        i = self.names.index(name)
        return (i % self.columns) * self.size, (i // self.columns) * self.size


class Model:
    def __init__(self, identifier, swatches, seed=1):
        self.identifier = identifier
        self.swatches = swatches
        self.bones = []
        self.by_name = {}
        self.random = random.Random(seed)

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        if name in self.by_name:
            raise ValueError("duplicate bone " + name)
        if parent is not None and parent not in self.by_name:
            raise ValueError("unknown parent " + parent)
        bone = {"name": name, "pivot": r3(pivot)}
        if parent is not None:
            bone["parent"] = parent
        if rotation is not None:
            bone["rotation"] = r3(rotation)
        bone["cubes"] = []
        self.bones.append(bone)
        self.by_name[name] = bone
        return name

    def box(self, bone, center, size, orient=None, swatch="black", inflate=None):
        """A cube centred on `center` with `size` along its own axes, turned by the frame `orient`."""
        center = np.asarray(center, dtype=float)
        size = np.asarray(size, dtype=float)
        if np.any(size < 0):
            raise ValueError("negative size")
        cube = {"origin": r3(center - size / 2.0), "size": r3(size)}
        if orient is not None:
            angles = euler(orient)
            if any(abs(a) > 1e-4 for a in angles):
                cube["rotation"] = r3(angles)
                cube["pivot"] = r3(center)
        if inflate:
            cube["inflate"] = inflate
        cube["uv"] = self.faces(size, swatch)
        self.by_name[bone]["cubes"].append(cube)

    def faces(self, size, swatch):
        sx, sy, sz = (max(1.0, min(16.0, round(float(v)))) for v in size)
        u0, v0 = self.swatches.origin(swatch)
        out = {}
        for face, (w, h) in {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy),
                             "up": (sx, sz), "down": (sx, sz)}.items():
            du = self.random.randint(0, int(16 - w)) if w < 16 else 0
            dv = self.random.randint(0, int(16 - h)) if h < 16 else 0
            out[face] = {"uv": [u0 + du, v0 + dv], "uv_size": [w, h]}
        return out

    def rod(self, bone, a, b, width, depth=None, swatch="black", x_hint=(1, 0, 0), overlap=0.3):
        """A cube running from point a to point b, `width` across along x_hint and `depth` thick."""
        a = np.asarray(a, dtype=float)
        b = np.asarray(b, dtype=float)
        length = float(np.linalg.norm(b - a))
        if length < 1e-6:
            return
        f = frame(b - a, x_hint)
        self.box(bone, (a + b) / 2.0, (width, length + overlap, width if depth is None else depth), f, swatch)

    def taper(self, bone, a, b, w0, w1, steps=3, depth_ratio=1.0, swatch="black", x_hint=(1, 0, 0)):
        """A tapering spike from a (width w0) to b (width w1) as `steps` shrinking rods."""
        a = np.asarray(a, dtype=float)
        b = np.asarray(b, dtype=float)
        for k in range(steps):
            t0, t1 = k / steps, (k + 1) / steps
            w = w0 + (w1 - w0) * (k + 0.5) / steps
            self.rod(bone, a + (b - a) * t0, a + (b - a) * t1, max(0.4, w), max(0.4, w * depth_ratio), swatch, x_hint)

    def chain(self, prefix, parent, points, widths, swatch="black", x_hint=(1, 0, 0), depth_ratio=1.0, extra=None):
        """A bone per segment of a polyline, each pivoting at its first point; returns the bone names."""
        names = []
        previous = parent
        for k in range(len(points) - 1):
            name = self.bone(f"{prefix}{k + 1}", previous, points[k])
            w0 = widths[k]
            w1 = widths[k + 1] if k + 1 < len(widths) else widths[k] * 0.6
            self.taper(name, points[k], points[k + 1], w0, w1, 2, depth_ratio, swatch, x_hint)
            if extra is not None:
                extra(name, k, np.asarray(points[k], dtype=float), np.asarray(points[k + 1], dtype=float))
            names.append(name)
            previous = name
        return names

    def cube_count(self):
        return sum(len(b["cubes"]) for b in self.bones)

    def write(self, path):
        geometry = {
            "description": {
                "identifier": self.identifier,
                "texture_width": self.swatches.width,
                "texture_height": self.swatches.height,
                "visible_bounds_width": 8,
                "visible_bounds_height": 5,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": [{k: v for k, v in b.items() if k != "cubes" or v} for b in self.bones],
        }
        with open(path, "w") as out:
            json.dump({"format_version": "1.12.0", "minecraft:geometry": [geometry]}, out, indent=1)
            out.write("\n")


def humanoid(model):
    """The player-model proxy bones the render layer drives from the vanilla player model."""
    model.bone("root", None, (0, 0, 0))
    model.bone("body", "root", (0, 24, 0))
    model.bone("head", "root", (0, 24, 0))
    model.bone("right_arm", "root", (-5, 22, 0))
    model.bone("left_arm", "root", (5, 22, 0))
    model.bone("right_leg", "root", (-1.9, 12, 0))
    model.bone("left_leg", "root", (1.9, 12, 0))
