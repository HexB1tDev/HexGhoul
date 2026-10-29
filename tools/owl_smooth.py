#!/usr/bin/env python3
"""
6.2.4: the One-Eyed Owl baked smooth out of the user's Blockbench model (art/one_eyed_owl.bbmodel), into what the mod draws:
src/main/resources/assets/hexkagune/models/one_eyed_owl.bin (OWL4) and textures/entity/one_eyed_owl.png and _glow.png.

The model is the user's, as they built it: nothing of its design changes, only its surface. Its cubes are laid in shells and
stacks (a face of thin plates, a hood of thin slats, curves stepped out of rotated blocks), so each part is first made solid
(every shell closed round its inside filled), then given one skin over all of it, smoothed (no steps, no seams between the cubes
it was built of: the face, the skull, the horns and the eye one head), and painted from the user's own cubes (each point of the
skin the colour of the cube it lies on, blended a little where two meet). The kagune blades are left out: the mod grows its own
there (their bones are kept, where they grow from).

Each part (a group of the model's bones made one, or one bone of a group cut out of it) is meshed on its own grid (the head's
finest), decimated to its share of the budget the phone can draw, its triangles paired into quads, each quad given its own patch
of the texture.

Needs numpy, scipy, scikit-image, pymeshlab and Pillow (pip install numpy scipy scikit-image pymeshlab pillow; pymeshlab's
meshing filters need libOpenGL: apt-get install libopengl0). Run: python3 tools/owl_smooth.py [model.bbmodel]
"""

import base64
import gzip
import io
import json
import math
import os
import struct
import sys

import numpy as np
from PIL import Image
from scipy import ndimage
from skimage import measure

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL = os.path.join(ROOT, "art", "one_eyed_owl.bbmodel")
OUT_BIN = os.path.join(ROOT, "src", "main", "resources", "assets", "hexkagune", "models", "one_eyed_owl.bin")
OUT_TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "hexkagune", "textures", "entity", "one_eyed_owl.png")
OUT_GLOW = os.path.join(ROOT, "src", "main", "resources", "assets", "hexkagune", "textures", "entity", "one_eyed_owl_glow.png")

FACES = ("east", "west", "up", "down", "south", "north")  # local +x, -x, +y, -y, +z, -z


# ---------------------------------------------------------------------------------------------------------------- the model

def rotation(deg):
    """A cube's turn as Blockbench turns it (x, then y, then z)."""
    x, y, z = (math.radians(v) for v in deg)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rz @ ry @ rx


class Cube:
    __slots__ = ("bone", "c", "r", "h", "col", "lo", "hi")

    def __init__(self, bone, c, r, h, col):
        self.bone = bone
        self.c = c
        self.r = r
        self.h = h
        self.col = col
        ext = np.abs(r) @ h
        self.lo = c - ext
        self.hi = c + ext


def load(path):
    """Its bones (name: parent, pivot, in the outliner's order) and cubes (each its bone, middle, turn, half size, face colours)."""
    m = json.load(open(path))
    src = m["textures"][0]["source"]
    tex = np.array(Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")).astype(float)
    uw = m["textures"][0].get("uv_width", m["resolution"]["width"])
    uh = m["textures"][0].get("uv_height", m["resolution"]["height"])
    groups = {g["uuid"]: g for g in m["groups"]}
    bones = {}
    order = []
    bone_of = {}

    def walk(node, parent):
        if isinstance(node, str):
            bone_of[node] = parent
            return
        g = groups[node["uuid"]]
        name = g["name"]
        bones[name] = (parent, np.array(g.get("origin", [0, 0, 0]), float))
        order.append(name)
        for k in node.get("children", []):
            walk(k, name)

    for n in m["outliner"]:
        walk(n, None)
    cubes = []
    for e in m["elements"]:
        f = np.array(e["from"], float)
        t = np.array(e["to"], float)
        o = np.array(e.get("origin", [0, 0, 0]), float)
        r = rotation(e.get("rotation", [0, 0, 0]))
        c = r @ ((f + t) / 2 - o) + o
        h = np.abs(t - f) / 2 + e.get("inflate", 0.0)
        col = np.zeros((6, 3))
        have = np.zeros(6, bool)
        for k, name in enumerate(FACES):
            face = e["faces"].get(name)
            if not face or face.get("texture") is None:
                continue
            u1, v1, u2, v2 = face["uv"]
            x0, x1 = sorted((u1 / uw * tex.shape[1], u2 / uw * tex.shape[1]))
            y0, y1 = sorted((v1 / uh * tex.shape[0], v2 / uh * tex.shape[0]))
            reg = tex[int(y0):max(int(math.ceil(y1)), int(y0) + 1), int(x0):max(int(math.ceil(x1)), int(x0) + 1)].reshape(-1, 4)
            reg = reg[reg[:, 3] > 16]
            if len(reg):
                col[k] = reg[:, :3].mean(axis=0)
                have[k] = True
        if not have.any():
            continue
        for k in range(6):
            if not have[k]:
                col[k] = col[have][0]
        cubes.append(Cube(bone_of[e["uuid"]], c, r, h, col))
    return bones, order, cubes


# ---------------------------------------------------------------------------------------------------------------- the parts

def part_specs(order):
    """
    The groups made solid together (a grid each: its cell, how far it is smoothed, all in the model's pixels), and in each the
    parts cut out of it (the bone each is drawn on, the bones whose cubes it is made of, its share of the quads). A group's parts
    overlap a little where they meet, so none parts from its neighbour as it moves.
    """
    crest = [b for b in order if b.startswith("crown_primary") or b.startswith("crown_red_branch")]
    groups = [
        # The head: the skull, the face, the eye, the horns, the upper teeth one head (no seam between them); the jaw and the tongue
        # theirs (they move).
        dict(name="head", cell=0.25, smooth=0.32, thick=0.3, floor=False, cut=False, texel=0.3,
             parts=[("head", ["mask_cranium", "mask_face_plates", "mask_single_eye", "mask_horns", "mask_upper_teeth"], 1700)]),
        # (Its teeth stand half a pixel apart in front of the black of its mouth: smoothed less than that, they stay apart.)
        dict(name="jaw", cell=0.16, smooth=0.12, thick=0.16, floor=False, cut=False, texel=0.22, parts=[("mask_jaw", ["mask_jaw"], 340)]),
        dict(name="tongue", cell=0.25, smooth=0.25, thick=0.25, floor=False, cut=False, texel=0.3, parts=[("mask_tongue", ["mask_tongue"], 40)]),
        # Its body: filled solid all together (its cloak closed by the ground under it and the body inside it), each bone's part of it
        # cut out (the faces between them, inside it, dropped).
        dict(name="body", cell=0.5, smooth=0.5, thick=0.6, floor=True, cut=True, texel=0.7,
             parts=[("torso", ["torso"], 180), ("neck_cowl", ["neck_cowl"], 620), ("dorsal_hump", ["dorsal_hump"], 260),
                    ("mantle", [b for b in order if b.startswith("mantle")], 1250), ("right_shoulder", ["right_shoulder"], 320),
                    ("left_shoulder", ["left_shoulder"], 320)]),
        # The crest: every spike its own (each stirs and flares on its own).
        dict(name="crest", cell=0.25, smooth=0.2, thick=0.25, floor=False, cut=True, texel=0.45,
             parts=[(b, [b], 46 if b.startswith("crown_primary") else 34) for b in crest]),
    ]
    for side in ("right", "left"):
        groups.append(dict(name=side + " arm", cell=0.25, smooth=0.25, thick=0.35, floor=False, cut=False, texel=0.5,
                           parts=[(side + "_upper_arm", [side + "_upper_arm"], 130), (side + "_forearm", [side + "_forearm"], 130),
                                  (side + "_hand", [side + "_hand"], 70)]
                           + [(side + "_finger_" + str(k), [side + "_finger_" + str(k)], 44) for k in range(1, 5)]
                           + [(side + "_thumb", [side + "_thumb"], 36)]))
    return groups


# ---------------------------------------------------------------------------------------------------------------- solid

def boxes_on_grid(cubes, origin, shape, cell, thin):
    """
    Over the grid ({@code origin}, {@code shape}, {@code cell}): which points are inside a cube (each cube at least {@code thin}
    thick each way, so a shell of thin plates is closed on the grid), the cube nearest each point near any (its index), and how near.
    """
    inside = np.zeros(shape, bool)
    near = np.full(shape, np.inf)
    which = np.full(shape, -1, np.int32)
    margin = 2.5 * cell
    for i, cb in enumerate(cubes):
        lo = np.maximum(np.floor((cb.lo - margin - origin) / cell).astype(int), 0)
        hi = np.minimum(np.ceil((cb.hi + margin - origin) / cell).astype(int) + 1, shape)
        if np.any(hi <= lo):
            continue
        xs = origin[0] + np.arange(lo[0], hi[0]) * cell
        ys = origin[1] + np.arange(lo[1], hi[1]) * cell
        zs = origin[2] + np.arange(lo[2], hi[2]) * cell
        p = np.stack(np.meshgrid(xs, ys, zs, indexing="ij"), axis=-1) - cb.c
        q = p @ cb.r  # (the cube's own axes)
        h = np.maximum(cb.h, thin)
        d = np.abs(q) - h
        out = np.linalg.norm(np.maximum(d, 0.0), axis=-1) + np.minimum(d.max(axis=-1), 0.0)
        sl = (slice(lo[0], hi[0]), slice(lo[1], hi[1]), slice(lo[2], hi[2]))
        inside[sl] |= out <= 0.0
        a = np.abs(out)
        closer = a < near[sl]
        near[sl] = np.where(closer, a, near[sl])
        which[sl] = np.where(closer, i, which[sl])
    return inside, near, which


def nearest_everywhere(which):
    """Each grid point's nearest cube (off the nearest point that has one)."""
    known = which >= 0
    idx = ndimage.distance_transform_edt(~known, return_distances=False, return_indices=True)
    return which[tuple(idx)]


class Painter:
    """A group's cubes and its grid of nearest cubes: the colour (and the bone) of the cube under any point of its skin."""

    def __init__(self, cubes, nearest, origin, cell):
        self.c = np.array([c.c for c in cubes])
        self.r = np.array([c.r for c in cubes])
        self.h = np.array([c.h for c in cubes])
        self.col = np.array([c.col for c in cubes])
        self.bone = np.array([c.bone for c in cubes])
        self.nearest = nearest
        self.origin = origin
        self.cell = cell
        self.hash = None

    def cube(self, pts):
        g = np.clip(np.round((pts - self.origin) / self.cell).astype(int), 0, np.array(self.nearest.shape) - 1)
        return self.nearest[g[:, 0], g[:, 1], g[:, 2]]

    def paint(self, pts, i=None):
        """The colour at each point: the face of the cube under it (or cube {@code i}) that it lies nearest."""
        if i is None:
            i = self.cube(pts)
        q = np.einsum("nk,nkj->nj", pts - self.c[i], self.r[i])
        rel = np.abs(q) - self.h[i]
        axis = rel.argmax(axis=1)
        sign = np.take_along_axis(q, axis[:, None], axis=1)[:, 0] >= 0
        face = axis * 2 + np.where(sign, 0, 1)
        return self.col[i, face], self.bone[i]

    def seen(self, pts, normals, reach=2.0):
        """
        The colour each point of the skin shows: what the user's model shows there, looked at from outside along the skin's normal
        (the first of its cubes the look meets, and the face of it it comes in through); where it meets none, the cube it lies
        nearest. (Every cube near the point is tried, each exactly: a thin layer of paint in front of another is never missed.)
        """
        if self.hash is None:
            self.build_hash()
        out_col = np.zeros((len(pts), 3))
        out_bone = np.empty(len(pts), dtype=self.bone.dtype)
        for k0 in range(0, len(pts), 4096):
            sl = slice(k0, k0 + 4096)
            P = pts[sl]
            N = normals[sl]
            A = P + N * reach
            D = -N * (2.0 * reach)
            cell = np.clip(np.floor((P - self.hash_origin) / HASH).astype(int), 0, np.array(self.hash.shape[:3]) - 1)
            cand = self.hash[cell[:, 0], cell[:, 1], cell[:, 2]]  # (n, k)
            valid = cand >= 0
            ci = np.where(valid, cand, 0)
            o = np.einsum("nkj,nkji->nki", (A[:, None, :] - self.c[ci]), self.r[ci])
            d = np.einsum("nj,nkji->nki", D, self.r[ci])
            h = self.h[ci] + 0.01
            with np.errstate(divide="ignore", invalid="ignore"):
                inv = 1.0 / np.where(np.abs(d) < 1e-12, 1e-12, d)
                t1 = (-h - o) * inv
                t2 = (h - o) * inv
            near = np.minimum(t1, t2)
            far = np.maximum(t1, t2)
            tmin = near.max(axis=2)
            tmax = far.min(axis=2)
            hit = valid & (tmax >= np.maximum(tmin, 0.0)) & (tmin <= 1.0)
            # (Coming in at the same depth as another, the thinner wins: a layer of paint on what is under it.)
            vol = np.prod(self.h[ci], axis=2)
            key = np.where(hit, np.maximum(tmin, 0.0) + vol * 1e-7, np.inf)
            best = key.argmin(axis=1)
            got = np.isfinite(key[np.arange(len(P)), best])
            idx = ci[np.arange(len(P)), best]
            entry = np.clip(tmin[np.arange(len(P)), best], 0.0, 1.0)
            at = A + D * entry[:, None]
            col = np.zeros((len(P), 3))
            bone = np.empty(len(P), dtype=self.bone.dtype)
            if got.any():
                # The face it comes in through: the side of the axis it last crossed onto.
                ax = near[np.arange(len(P)), best].argmax(axis=1)
                dd = d[np.arange(len(P)), best, ax]
                face = ax * 2 + np.where(dd > 0, 1, 0)  # coming in going +: through the - face
                inside = tmin[np.arange(len(P)), best] < 0.0
                cc, bb = self.paint(at[got], idx[got])
                face_col = self.col[idx, face]
                col[got] = np.where(inside[got][:, None], cc, face_col[got])
                bone[got] = self.bone[idx[got]]
            if (~got).any():
                col[~got], bone[~got] = self.paint(P[~got])
            out_col[sl] = col
            out_bone[sl] = bone
        return out_col, out_bone

    def build_hash(self):
        """Every cube listed in the cells of a coarse grid it lies within reach of (for each, the cubes a look from it can meet)."""
        lo = self.c - np.einsum("nij,nj->ni", np.abs(self.r), self.h)
        hi = self.c + np.einsum("nij,nj->ni", np.abs(self.r), self.h)
        self.hash_origin = lo.min(axis=0) - 4.0
        shape = tuple(np.ceil((hi.max(axis=0) + 4.0 - self.hash_origin) / HASH).astype(int) + 1)
        lists = {}
        for i in range(len(self.c)):
            a = np.floor((lo[i] - 2.5 - self.hash_origin) / HASH).astype(int)
            b = np.floor((hi[i] + 2.5 - self.hash_origin) / HASH).astype(int)
            for x in range(a[0], b[0] + 1):
                for y in range(a[1], b[1] + 1):
                    for z in range(a[2], b[2] + 1):
                        lists.setdefault((x, y, z), []).append(i)
        k = max(len(v) for v in lists.values())
        table = -np.ones(shape + (k,), np.int32)
        for (x, y, z), v in lists.items():
            if 0 <= x < shape[0] and 0 <= y < shape[1] and 0 <= z < shape[2]:
                table[x, y, z, :len(v)] = v
        self.hash = table


HASH = 1.5


def cropped(mask, pad):
    """The box round {@code mask}'s points, {@code pad} points out each way (clipped to its grid): slices."""
    idx = np.nonzero(mask)
    return tuple(slice(max(int(a.min()) - pad, 0), min(int(a.max()) + pad + 1, n)) for a, n in zip(idx, mask.shape))


def signed(solid, cell, smooth):
    """The signed distance (pixels; outside +) of a solid on its grid, smoothed {@code smooth} pixels."""
    out = ndimage.distance_transform_edt(~solid) * cell
    ins = ndimage.distance_transform_edt(solid) * cell
    d = np.where(solid, -(ins - 0.5 * cell), out - 0.5 * cell)
    return ndimage.gaussian_filter(d, smooth / cell) if smooth > 0 else d


# ---------------------------------------------------------------------------------------------------------------- meshed

def mesh_of(d, origin, cell):
    """The skin (level 0) of the signed distance {@code d}: its points (model pixels) and triangles, outward."""
    if d.min() >= 0 or d.max() <= 0:
        return np.zeros((0, 3)), np.zeros((0, 3), int)
    v, f, _, _ = measure.marching_cubes(d, 0.0, spacing=(cell, cell, cell))
    v = v + origin
    return v, f[:, ::-1].copy()  # (marching cubes winds them inward for a field positive outside)


def decimated(v, f, target):
    """{@code v}, {@code f} brought down to about {@code target} triangles (quadric edge collapse), smoothed a little first."""
    import pymeshlab
    ms = pymeshlab.MeshSet()
    ms.add_mesh(pymeshlab.Mesh(vertex_matrix=v, face_matrix=f))
    ms.meshing_remove_duplicate_vertices()
    ms.meshing_remove_unreferenced_vertices()
    ms.apply_coord_taubin_smoothing(stepsmoothnum=6)
    if ms.current_mesh().face_number() > target:
        ms.meshing_decimation_quadric_edge_collapse(targetfacenum=int(target), preservenormal=True, preservetopology=False, optimalplacement=True,
                                                   planarquadric=True, qualitythr=0.4, autoclean=True)
    mesh = ms.current_mesh()
    return mesh.vertex_matrix().copy(), mesh.face_matrix().copy()


def quads_of(v, f):
    """
    The triangles paired into quads where two make a good one (flat enough, convex, near square), each quad (a, b, c, d) split as
    the game splits it (a b c, a c d) along the edge they shared; a triangle left over is a quad with its last corner twice.
    """
    n = np.cross(v[f[:, 1]] - v[f[:, 0]], v[f[:, 2]] - v[f[:, 0]])
    area = np.linalg.norm(n, axis=1)
    n = n / np.maximum(area, 1e-12)[:, None]
    edges = {}
    for t in range(len(f)):
        for k in range(3):
            a, b = f[t, k], f[t, (k + 1) % 3]
            edges.setdefault((min(a, b), max(a, b)), []).append((t, k))
    cand = []
    for key, users in edges.items():
        if len(users) != 2:
            continue
        (t1, k1), (t2, k2) = users
        if np.dot(n[t1], n[t2]) < 0.94:
            continue
        p = f[t1, (k1 + 2) % 3]  # the corner of t1 off the shared edge
        q, r = f[t1, k1], f[t1, (k1 + 1) % 3]
        s = f[t2, (k2 + 2) % 3]
        quad = (q, s, r, p)
        P = v[list(quad)]
        # Convex: each corner turns the same way.
        nn = n[t1] + n[t2]
        ok = True
        score = 0.0
        for i in range(4):
            e1 = P[(i + 1) % 4] - P[i]
            e2 = P[(i + 3) % 4] - P[i]
            if np.dot(np.cross(-e2, e1), nn) <= 0:
                ok = False
                break
            cosang = np.dot(e1, e2) / max(np.linalg.norm(e1) * np.linalg.norm(e2), 1e-12)
            score += abs(cosang)
        if ok:
            cand.append((score + (1.0 - np.dot(n[t1], n[t2])) * 4.0, t1, t2, quad))
    cand.sort(key=lambda c: c[0])
    used = np.zeros(len(f), bool)
    quads = []
    for score, t1, t2, quad in cand:
        if used[t1] or used[t2]:
            continue
        used[t1] = used[t2] = True
        quads.append(quad)
    for t in np.nonzero(~used)[0]:
        quads.append((f[t, 0], f[t, 1], f[t, 2], f[t, 2]))
    return np.array(quads, int)


def sample(grid, origin, cell, pts, order=1):
    """{@code grid} (its last axis the values, or none) at the points {@code pts} (model pixels), trilinear."""
    g = (pts - origin) / cell
    coords = [g[:, 0], g[:, 1], g[:, 2]]
    if grid.ndim == 3:
        return ndimage.map_coordinates(grid, coords, order=order, mode="nearest")
    return np.stack([ndimage.map_coordinates(grid[..., k], coords, order=order, mode="nearest") for k in range(grid.shape[-1])], axis=-1)


# ---------------------------------------------------------------------------------------------------------------- glow

def glow_of(bone, rgb):
    """How much of a point burns (0..1): the red of its crest and its eye at full, the red slashes of its face a little."""
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    red = (r > 120) & (g < 70) & (b < 80)
    bright = (r > 200) & (g < 60)
    out = np.zeros(r.shape)
    if bone.startswith("crown"):
        out = np.where(bright, 1.0, np.where(red, 0.6, 0.0))
    elif bone == "eye":
        out = np.where(red, 1.0, 0.0)
    elif bone == "face":
        out = np.where(red, 0.3, 0.0)
    return out


# ---------------------------------------------------------------------------------------------------------------- baked

def bake(path=MODEL):
    bones, order, cubes = load(path)
    print(f"{len(cubes)} cubes, {len(order)} bones", flush=True)
    names = list(order)
    # Where the eye is (its iris, the red of it): for first person and its glow.
    eye = [c for c in cubes if c.bone == "mask_single_eye"]
    reds = [c for c in eye if c.col.max(axis=0)[0] > 150 and c.col.min(axis=0)[1] < 60]
    iris = np.mean([c.c for c in (reds or eye)], axis=0) if eye else np.array([0.0, 56.0, -33.0])
    out_quads = []  # (bone index, corners 4x3, normals 4x3, glow tag, sample fn index)
    samplers = []
    for spec in part_specs(order):
        members = {b for _, bs, _ in spec["parts"] for b in bs}
        cs = [c for c in cubes if c.bone in members]
        if not cs:
            continue
        cell = spec["cell"]
        lo = np.min([c.lo for c in cs], axis=0) - 4 * cell - spec["smooth"] * 3
        hi = np.max([c.hi for c in cs], axis=0) + 4 * cell + spec["smooth"] * 3
        shape = tuple(np.ceil((hi - lo) / cell).astype(int) + 1)
        inside, near, which = boxes_on_grid(cs, lo, shape, cell, max(0.75 * cell, spec["thick"]))
        # Filled: whatever the outside cannot reach is inside it (the ground under it closes its cloak).
        empty = ~inside
        walls = np.zeros(shape, bool)
        if spec["floor"]:
            ground = int(np.floor((0.5 - lo[1]) / cell))
            walls[:, :max(ground, 1), :] = True  # (below the ground the outside does not reach in)
        lab, _ = ndimage.label(empty & ~walls)
        border = np.zeros(shape, bool)
        border[0, :, :] = border[-1, :, :] = True
        border[:, -1, :] = True
        border[:, :, 0] = border[:, :, -1] = True
        if not spec["floor"]:
            border[:, 0, :] = True
        seeds = np.unique(lab[border & (lab > 0)])
        outside = np.isin(lab, seeds) | (walls & empty)
        solid = ~outside
        nearest = nearest_everywhere(which)
        painter = Painter(cs, nearest, lo, cell)
        samplers.append(painter)
        si = len(samplers) - 1
        owner = painter.bone[nearest]
        whole = signed(solid, cell, spec["smooth"]) if spec["cut"] else None
        pad = int(math.ceil((2.0 + spec["smooth"] * 3) / cell)) + 4
        for bone, bs, budget in spec["parts"]:
            mine = np.isin(owner, bs) & solid
            if not mine.any():
                continue
            if len(spec["parts"]) > 1:
                # Its own share of the solid, and a little past it into its neighbours' (so they never part as it moves).
                box = cropped(mine, pad + int(math.ceil(2.0 / cell)))
                part = solid[box] & ndimage.binary_dilation(mine[box], iterations=int(math.ceil(2.0 / cell)))
            else:
                box = cropped(solid, pad)
                part = solid[box]
            origin = lo + np.array([b.start for b in box]) * cell
            d = signed(part, cell, spec["smooth"])
            v, f = mesh_of(d, origin, cell)
            if len(f) == 0:
                continue
            drop = np.zeros(len(f), bool)
            if spec["cut"] and len(spec["parts"]) > 1:
                # The faces cut between it and its neighbours, inside the whole: dropped.
                depth = sample(whole, lo, cell, v)
                drop |= np.all(depth[f] < -0.45, axis=1)
            if spec["floor"]:
                # Its underside, flat on the ground: never seen.
                drop |= np.all(v[f][:, :, 1] < 1.0, axis=1)
            f = f[~drop]
            used = np.unique(f)
            remap = -np.ones(len(v), int)
            remap[used] = np.arange(len(used))
            v = v[used]
            f = remap[f]
            if len(f) == 0:
                continue
            v, f = decimated(v, f.astype(np.int32), budget * 1.7)
            q = quads_of(v, f)
            grad = np.stack(np.gradient(d, cell), axis=-1)
            nv = sample(grad, origin, cell, v)
            nv = nv / np.maximum(np.linalg.norm(nv, axis=1), 1e-9)[:, None]
            bi = names.index(bone)
            for quad in q:
                out_quads.append((bi, v[quad], nv[quad], si, bone))
            print(f"  {spec['name']:10s} {bone:24s} {len(q):5d} quads", flush=True)
    print(f"{len(out_quads)} quads in all", flush=True)
    # The eye's bone (no quads of its own): where the iris is, on the head.
    names.append("mask_iris")
    bones["mask_iris"] = ("head", iris)
    write(names, bones, out_quads, samplers, cubes, iris)


# ---------------------------------------------------------------------------------------------------------------- written

TEXEL = {"head": 0.18, "mask_jaw": 0.18, "mask_tongue": 0.3}
BLEND = np.array([[0, 0, 0], [1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]], float)


def write(names, bones, quads, samplers, cubes, iris):
    # Each quad its patch of the texture, as many texels each way as it is long over its texel (2 .. 14).
    patches = []
    for bi, p, n, si, bone in quads:
        texel = TEXEL.get(bone, 0.45 if bone.startswith("crown") else 0.5 if "_arm" in bone or "finger" in bone or "thumb" in bone or "hand" in bone
                          or "forearm" in bone else 0.7)
        u = max(np.linalg.norm(p[1] - p[0]), np.linalg.norm(p[2] - p[3]))
        w = max(np.linalg.norm(p[3] - p[0]), np.linalg.norm(p[2] - p[1]))
        patches.append((int(np.clip(round(u / texel), 2, 14)), int(np.clip(round(w / texel), 2, 14))))
    # Packed in shelves, tallest first.
    size = 256
    while True:
        at = [None] * len(patches)
        x = y = shelf = 0
        ok = True
        for i in sorted(range(len(patches)), key=lambda i: -patches[i][1]):
            pw, ph = patches[i]
            if x + pw > size:
                x = 0
                y += shelf
                shelf = 0
            if y + ph > size:
                ok = False
                break
            at[i] = (x, y)
            x += pw
            shelf = max(shelf, ph)
        if ok:
            break
        size *= 2
    print(f"texture {size}x{size}", flush=True)
    tex = np.zeros((size, size, 4), np.uint8)
    glow = np.zeros((size, size, 4), np.uint8)
    uvs = []
    for i, (bi, p, n, si, bone) in enumerate(quads):
        pw, ph = patches[i]
        x0, y0 = at[i]
        # Each texel where its middle is drawn: the game splits the quad a b c, a c d, each mapped flat.
        ii, jj = np.meshgrid(np.arange(pw), np.arange(ph), indexing="xy")
        s = ((ii + 0.5 - 0.02) / (pw - 0.04)).ravel()
        t = ((jj + 0.5 - 0.02) / (ph - 0.04)).ravel()
        lower = s >= t
        pts = np.where(lower[:, None], p[0] + s[:, None] * (p[1] - p[0]) + t[:, None] * (p[2] - p[1]),
                       p[0] + s[:, None] * (p[2] - p[3]) + t[:, None] * (p[3] - p[0]))
        nrm = np.where(lower[:, None], n[0] + s[:, None] * (n[1] - n[0]) + t[:, None] * (n[2] - n[1]),
                       n[0] + s[:, None] * (n[2] - n[3]) + t[:, None] * (n[3] - n[0]))
        nrm = nrm / np.maximum(np.linalg.norm(nrm, axis=1), 1e-9)[:, None]
        painter = samplers[si]
        # (Each texel the colours round its middle blended: where two of the cubes meet, they run into each other.)
        span = 0.35 * max(np.linalg.norm(p[1] - p[0]) / pw, np.linalg.norm(p[3] - p[0]) / ph)
        rgb = np.zeros((len(pts), 3))
        for o in BLEND:
            c, src0 = painter.seen(pts + o * span, nrm)
            rgb += c
            if not np.any(o):
                src = src0
        rgb /= len(BLEND)
        kind = np.array(["eye" if b == "mask_single_eye" else "face" if b == "mask_face_plates" else b for b in src])
        amount = np.zeros(len(pts))
        for k in np.unique(kind):
            sel = kind == k
            amount[sel] = glow_of(k, rgb[sel])
        rgb = np.clip(rgb, 0, 255).reshape(ph, pw, 3)
        tex[y0:y0 + ph, x0:x0 + pw, :3] = rgb.astype(np.uint8)
        tex[y0:y0 + ph, x0:x0 + pw, 3] = 255
        a = amount.reshape(ph, pw)
        glow[y0:y0 + ph, x0:x0 + pw, :3] = (rgb * a[..., None]).astype(np.uint8)
        glow[y0:y0 + ph, x0:x0 + pw, 3] = np.where(a > 0, 255, 0).astype(np.uint8)
        corner = [(0, 0), (1, 0), (1, 1), (0, 1)]
        uvs.append([((x0 + 0.02 + cs * (pw - 0.04)) / size, (y0 + 0.02 + ct * (ph - 0.04)) / size) for cs, ct in corner])
        quads[i] = (bi, p, n, si, bone, bool(amount.max() > 0))
    os.makedirs(os.path.dirname(OUT_TEX), exist_ok=True)
    Image.fromarray(tex).save(OUT_TEX)
    Image.fromarray(glow).save(OUT_GLOW)
    # The mesh: "OWL4": its bones (name, parent, pivot, rest turn), none grown elsewhere, its quads (bone, four corners, a normal at each
    # corner, each corner's place in the texture, whether any of it burns). Gzipped.
    index = {n: i for i, n in enumerate(names)}
    buf = io.BytesIO()
    w = buf.write
    w(struct.pack(">i", 0x4F574C34))
    w(struct.pack(">i", len(names)))
    for name in names:
        parent, pivot = bones[name]
        data = name.encode("utf-8")
        w(struct.pack(">H", len(data)) + data)
        w(struct.pack(">i", index.get(parent, -1) if parent else -1))
        w(struct.pack(">3f", *pivot))
        w(struct.pack(">3f", 0.0, 0.0, 0.0))
    w(struct.pack(">i", 0))
    w(struct.pack(">i", len(quads)))
    for (bi, p, n, si, bone, burns), uv in zip(quads, uvs):
        w(struct.pack(">h", bi))
        w(struct.pack(">12f", *p.ravel()))
        w(struct.pack(">12f", *n.ravel()))
        w(struct.pack(">8f", *[c for xy in uv for c in xy]))
        w(struct.pack(">?", burns))
    os.makedirs(os.path.dirname(OUT_BIN), exist_ok=True)
    with gzip.open(OUT_BIN, "wb") as o:
        o.write(buf.getvalue())
    print(f"wrote {OUT_BIN} ({len(quads)} quads), {OUT_TEX}", flush=True)


if __name__ == "__main__":
    bake(sys.argv[1] if len(sys.argv) > 1 else MODEL)
