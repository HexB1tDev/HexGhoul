"""Generate Eto's steam wisp texture (stdlib only, deterministic). Run: python3 tools/gen_steam_texture.py

A 64 x 64 soft wisp: white, the shape all in the alpha. A radial falloff broken up by domain-warped value noise so the
edge is ragged and curling, never a disc, with a few thinner tendrils of vapour trailing off it. The steam particle
(client/fx/EtoSteam) draws it on three crossed planes at very low opacity, so what reads in game is a soft, lumpy volume.
"""
import math
import random
import struct
import zlib
from pathlib import Path

SIZE = 64
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hexkagune/textures/particle/steam.png"

rng = random.Random(6100)
GRID = 9
LATTICE = [[rng.random() for _ in range(GRID + 1)] for _ in range(GRID + 1)]


def smooth(t):
    return t * t * (3.0 - 2.0 * t)


def value_noise(x, y):
    """Periodic value noise on a 9 x 9 lattice over the unit square (x, y wrap at 1, so octaves join without a seam)."""
    gx = (x % 1.0) * GRID
    gy = (y % 1.0) * GRID
    ix, iy = int(gx) % GRID, int(gy) % GRID
    jx, jy = (ix + 1) % GRID, (iy + 1) % GRID
    fx, fy = smooth(gx - int(gx)), smooth(gy - int(gy))
    a = LATTICE[iy][ix] + (LATTICE[iy][jx] - LATTICE[iy][ix]) * fx
    b = LATTICE[jy][ix] + (LATTICE[jy][jx] - LATTICE[jy][ix]) * fx
    return a + (b - a) * fy


def fbm(x, y):
    total, amplitude, frequency = 0.0, 0.55, 1.0
    for _ in range(4):
        total += amplitude * value_noise(x * frequency, y * frequency)
        amplitude *= 0.5
        frequency *= 2.0
    return total


def main():
    rows = []
    for py in range(SIZE):
        row = []
        for px in range(SIZE):
            u = (px + 0.5) / SIZE
            v = (py + 0.5) / SIZE
            # Domain warp: the shape curls.
            wx = u + 0.16 * (fbm(u * 0.9 + 0.13, v * 0.9 + 0.71) - 0.5)
            wy = v + 0.16 * (fbm(u * 0.9 + 0.57, v * 0.9 + 0.29) - 0.5)
            dx = (wx - 0.5) * 2.0
            dy = (wy - 0.5) * 2.0
            r = math.hypot(dx, dy * 1.08)
            body = max(0.0, 1.0 - r)
            body = body ** 1.35
            # A ragged, breathing edge and a few thinner wisps inside.
            n = fbm(wx * 1.3, wy * 1.3)
            density = body * (0.55 + 0.75 * n)
            swirl = math.sin(math.atan2(dy, dx) * 3.0 + r * 7.0 + n * 4.0)
            density *= 0.8 + 0.2 * swirl
            alpha = max(0.0, min(1.0, (density - 0.06) / 0.62))
            alpha = alpha * alpha * (3.0 - 2.0 * alpha)
            # Fade hard to nothing at the square's rim, whatever the noise did.
            rim = min(u, v, 1.0 - u, 1.0 - v) * 2.0 / 0.25
            alpha *= max(0.0, min(1.0, rim))
            row += [255, 255, 255, int(round(alpha * 255))]
        rows.append(row)
    raw = b"".join(b"\x00" + bytes(r) for r in rows)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    OUT.write_bytes(png)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
