"""Generate the bloodlust overlay textures (stdlib only, deterministic). Run: python3 tools/gen_bloodlust_textures.py"""
import math, random, struct, zlib
from pathlib import Path

SIZE = 256
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hexkagune/textures/misc"


def write_png(path, pixels):
    raw = b"".join(b"\x00" + bytes(row) for row in pixels)
    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    path.write_bytes(png)


def edge_distance(x, y):
    """0 at the centre, 1 at the edge midpoints, larger in corners (squircle metric)."""
    nx = (x + 0.5) / SIZE * 2 - 1
    ny = (y + 0.5) / SIZE * 2 - 1
    return (abs(nx) ** 4 + abs(ny) ** 4) ** 0.25


def vignette():
    rows = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            d = edge_distance(x, y)
            t = max(0.0, min(1.0, (d - 0.62) / 0.4))
            a = t * t * (3 - 2 * t)
            row += [92, 2, 6, int(225 * a)]
        rows.append(row)
    return rows


def veins():
    rng = random.Random(1337)
    alpha = [[0.0] * SIZE for _ in range(SIZE)]
    core = [[0.0] * SIZE for _ in range(SIZE)]

    def stamp(px, py, radius, strength):
        r = int(radius + 2)
        for yy in range(int(py) - r, int(py) + r + 1):
            for xx in range(int(px) - r, int(px) + r + 1):
                if 0 <= xx < SIZE and 0 <= yy < SIZE:
                    dist = math.hypot(xx - px, yy - py)
                    glow = max(0.0, 1 - dist / (radius + 1.8)) * 0.35 * strength
                    body = max(0.0, min(1.0, radius + 0.5 - dist)) * strength
                    alpha[yy][xx] = max(alpha[yy][xx], body, glow)
                    core[yy][xx] = max(core[yy][xx], body)

    def grow(x, y, angle, width, length, depth):
        for _ in range(length):
            angle += rng.uniform(-0.28, 0.28)
            x += math.cos(angle) * 1.2
            y += math.sin(angle) * 1.2
            reach = 1 - edge_distance(x, y)
            if reach > 0.22 or not (0 <= x < SIZE and 0 <= y < SIZE):
                return
            fade = max(0.0, 1 - reach / 0.22)
            stamp(x, y, width, 0.35 + 0.65 * fade)
            width = max(0.35, width * 0.985)
            if depth < 4 and rng.random() < 0.035:
                grow(x, y, angle + rng.choice((-1, 1)) * rng.uniform(0.5, 1.0), width * 0.7, int(length * 0.6), depth + 1)

    for _ in range(46):
        side = rng.randrange(4)
        pos = rng.uniform(0, SIZE)
        start = [(pos, 0.0, math.pi / 2), (pos, SIZE - 1.0, -math.pi / 2), (0.0, pos, 0.0), (SIZE - 1.0, pos, math.pi)][side]
        grow(start[0], start[1], start[2] + rng.uniform(-0.6, 0.6), rng.uniform(1.4, 2.6), rng.randint(40, 90), 0)
    rows = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            a = alpha[y][x]
            c = core[y][x]
            row += [int(120 + 40 * (1 - c)), int(6 + 10 * (1 - c)), int(10 + 8 * (1 - c)), int(240 * min(1.0, a))]
        rows.append(row)
    return rows


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    write_png(OUT / "bloodlust_vignette.png", vignette())
    write_png(OUT / "bloodlust_veins.png", veins())
    print("wrote", OUT)
