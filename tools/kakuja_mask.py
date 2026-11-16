#!/usr/bin/env python3
"""The final kakuja's mask from the owner's model (1.3.1): their Forge OBJ (Y up, front -Z, blocks, the neck's pivot at the origin)
packed into the mod as a small binary mesh in the head's own pixels, and its texture copied in.

Usage: python3 tools/kakuja_mask.py <model.obj> <texture.png>

Writes src/main/resources/assets/hexkagune/models/kakuja_mask.bin and src/main/resources/assets/hexkagune/textures/entity/kakuja_mask.png.

The mesh (big-endian, read by client/render/KakujaMaskMesh):
  int  magic 0x484B4D31 ("HKM1"), int vertices, int triangles
  per vertex: short x, y, z (head pixels * 1024: x to the player's left, y down, z backward, as the head part draws), short u, v
              (0..65535, v flipped as Forge's flip_v), byte nx, ny, nz (* 127)
  per triangle: three vertex indices (unsigned short while there are fewer than 65536 vertices, else int)
"""
import os
import shutil
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_MESH = os.path.join(ROOT, "src/main/resources/assets/hexkagune/models/kakuja_mask.bin")
OUT_TEXTURE = os.path.join(ROOT, "src/main/resources/assets/hexkagune/textures/entity/kakuja_mask.png")


def read_obj(path):
    vs, vts, vns, tris = [], [], [], []
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("v "):
                vs.append(tuple(float(x) for x in line.split()[1:4]))
            elif line.startswith("vt "):
                vts.append(tuple(float(x) for x in line.split()[1:3]))
            elif line.startswith("vn "):
                vns.append(tuple(float(x) for x in line.split()[1:4]))
            elif line.startswith("f "):
                corners = []
                for token in line.split()[1:]:
                    parts = token.split("/")
                    v = int(parts[0]) - 1
                    t = int(parts[1]) - 1 if len(parts) > 1 and parts[1] else -1
                    n = int(parts[2]) - 1 if len(parts) > 2 and parts[2] else -1
                    corners.append((v, t, n))
                for k in range(1, len(corners) - 1):
                    tris.append((corners[0], corners[k], corners[k + 1]))
    return vs, vts, vns, tris


def packed(vertex):
    px, py, pz, u, w, nx, ny, nz = vertex
    return (int(round(clamp(px * 1024, -32767, 32767))), int(round(clamp(py * 1024, -32767, 32767))), int(round(clamp(pz * 1024, -32767, 32767))),
            int(round(clamp(u, 0.0, 1.0) * 65535)), int(round(clamp(w, 0.0, 1.0) * 65535)), int(round(clamp(nx, -1, 1) * 127)),
            int(round(clamp(ny, -1, 1) * 127)), int(round(clamp(nz, -1, 1) * 127)))


def clamp(x, lo, hi):
    return lo if x < lo else hi if x > hi else x


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    vs, vts, vns, tris = read_obj(sys.argv[1])
    index = {}
    vertices = []
    triangles = []
    for tri in tris:
        ids = []
        for corner in tri:
            v, t, n = corner
            x, y, z = vs[v]
            u, w = vts[t] if t >= 0 else (0.0, 0.0)
            nx, ny, nz = vns[n] if n >= 0 else (0.0, 1.0, 0.0)
            # The head part's space: x to the player's left (the model's +x is its right), y down, z backward (front -z, as the
            # model's), pixels; a half turn about z, so no mirroring and the winding stays as it was.
            vertex = (-x * 16.0, -y * 16.0, z * 16.0, u, 1.0 - w, -nx, -ny, nz)
            # Corners alike once packed are one vertex (the OBJ gives every corner its own texture and normal index).
            key = packed(vertex)
            if key not in index:
                index[key] = len(vertices)
                vertices.append(vertex)
            ids.append(index[key])
        triangles.append(ids)
    wide = len(vertices) >= 65536
    os.makedirs(os.path.dirname(OUT_MESH), exist_ok=True)
    with open(OUT_MESH, "wb") as out:
        out.write(struct.pack(">iii", 0x484B4D31, len(vertices), len(triangles)))
        for vertex in vertices:
            out.write(struct.pack(">hhhHHbbb", *packed(vertex)))
        for ids in triangles:
            out.write(struct.pack(">iii" if wide else ">HHH", *ids))
    os.makedirs(os.path.dirname(OUT_TEXTURE), exist_ok=True)
    shutil.copyfile(sys.argv[2], OUT_TEXTURE)
    xs = [v[0] for v in vertices]
    ys = [v[1] for v in vertices]
    zs = [v[2] for v in vertices]
    print("kakuja mask: %d vertices, %d triangles, %d bytes; x %.2f..%.2f y %.2f..%.2f z %.2f..%.2f (head pixels)" % (
        len(vertices), len(triangles), os.path.getsize(OUT_MESH), min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))


if __name__ == "__main__":
    main()
