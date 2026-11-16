#!/usr/bin/env python3
"""4.1.72: pictures of the hotel (data/hexkagune/structures/hotel/, the owner's humanhotel.schem) to judge it by.

    python3 tools/hotel_render.py village out.png     # a plan of a village with its hotel on each of its four sides, turned to face it
    python3 tools/hotel_render.py street out.png      # its street level cut away (layer 18 up left off), its people and its patrol

village: north up, a block a pixel and a quarter, the world's chunks drawn; each panel one side (HotelPlan: the hotel's start chunk
DISTANCE chunks out of the village's, its footprint round that chunk's middle as ControlStructures.Site.hotelAt lays it, turned by
HotelPlan.turns), the hotel seen from above (each column's top block, lit from the north-west), its cleared margin (SitePrep.MARGIN) and its
terrain fit (12) dotted round it; the village's start chunk and how far its pieces may reach (96 blocks: 80 from its start piece's middle,
that piece up to 16 off its chunk's corner).

street: city_render's picture of everything under layer 18 (the slab, the basement under it, the ground floor and the forecourt), from the
south-east, the humans in yellow, the investigators in blue, the patrol's stops white and joined in order round it.
"""
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parent))
import city_render as C  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
PLAN = (ROOT / "src/main/java/com/hex/kagune/control/village/HotelPlan.java").read_text()
DISTANCE = int(re.search(r"int DISTANCE = (\d+);", PLAN).group(1))
MARGIN = int(re.search(r"int MARGIN = (\d+);", (ROOT / "src/main/java/com/hex/kagune/control/village/SitePrep.java").read_text()).group(1))
BEARD = 12
VILLAGE_REACH = 96
STEP = [(0, -1), (1, 0), (0, 1), (-1, 0)]
SIDES = ["north", "east", "south", "west"]
FACES = ["south", "west", "north", "east"]


def font(size, bold=False):
    try:
        return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans" + ("-Bold" if bold else "") + ".ttf", size)
    except OSError:
        return ImageFont.load_default()


def site_numbers():
    """The hotel's middle, ground and its patrol (relative to its middle), from SiteMaps.java."""
    text = (ROOT / "src/main/java/com/hex/kagune/control/village/SiteMaps.java").read_text()
    start = text.index('new Site("hotel"')
    block = text[start:text.index("new Site(", start + 10) if "new Site(" in text[start + 10:] else len(text)]
    head = re.match(r'new Site\("hotel", (\d+), (\d+), (\d+), (\d+), (\d+), (\d+), (\d+),', block)
    ground, mx, mz = int(head.group(5)), int(head.group(6)), int(head.group(7))
    pts = re.findall(r'points\("([^"]*)"\)', block)
    route = [tuple(int(v) for v in p.split(",")) for p in pts[1].split(";") if p]
    return ground, (mx, mz), route


def top_down(grid, names):
    """Each column's top block's colour, lit from the north-west by its height (x, z)."""
    w, h, d = grid.shape
    filled = grid != 0
    top = np.where(filled.any(axis=1), h - 1 - np.argmax(filled[:, ::-1, :], axis=1), -1)
    ids = np.take_along_axis(grid, np.clip(top, 0, h - 1)[:, None, :], axis=1)[:, 0, :]
    cols = np.array([C.colour(n) for n in names], dtype=np.float32)[ids]
    nw = np.pad(top, ((1, 0), (1, 0)), mode="edge")[:-1, :-1]
    light = np.clip(1.0 + 0.05 * (top - nw), 0.6, 1.25)[..., None]
    height = (0.72 + 0.28 * np.clip(top / 194.0, 0, 1))[..., None]
    out = np.clip(cols * light * height, 0, 255)
    out[top < 0] = (0, 0, 0)
    return out.astype(np.uint8), top >= 0


def dashed_rect(dr, box, colour, dash=6, width=2):
    x0, y0, x1, y1 = box
    for a, b, c, e in ((x0, y0, x1, y0), (x1, y0, x1, y1), (x1, y1, x0, y1), (x0, y1, x0, y0)):
        length = max(abs(c - a), abs(e - b))
        for k in range(0, int(length), 2 * dash):
            t0, t1 = k / length, min(1.0, (k + dash) / length)
            dr.line((a + (c - a) * t0, b + (e - b) * t0, a + (c - a) * t1, b + (e - b) * t1), fill=colour, width=width)


def arrow(dr, p, q, colour, width=4):
    dr.line((*p, *q), fill=colour, width=width)
    v = np.array(q, float) - np.array(p, float)
    v /= np.linalg.norm(v)
    n = np.array([-v[1], v[0]])
    tip = np.array(q, float)
    dr.polygon([tuple(tip), tuple(tip - 16 * v + 8 * n), tuple(tip - 16 * v - 8 * n)], fill=colour)


def village(out):
    grid, names, _ = C.load_site("hotel")
    w, d = grid.shape[0], grid.shape[2]
    s = 1.25
    half = 250
    panel = int(2 * half * s)
    pad, title = 16, 54
    img = Image.new("RGB", (2 * panel + 3 * pad, 2 * (panel + title) + 3 * pad + 70), (236, 232, 222))
    dr = ImageDraw.Draw(img)
    big, small = font(20, True), font(15)
    foot = 2 * (panel + title) + 2 * pad + 6
    dr.text((pad, foot), f"One side only per village: the first of the four (from one the seed gives it) with no other village's start within reach. "
            f"Start chunk {DISTANCE} chunks out;", fill=(40, 40, 40), font=small)
    dr.text((pad, foot + 20), "its forecourt (the file's south: fountain, porte-cochere, taxis) turned to the village (red arrow). Tan: where a "
            f"village's pieces may reach ({VILLAGE_REACH} blocks);", fill=(40, 40, 40), font=small)
    dr.text((pad, foot + 40), f"white dashes: the hotel's cleared margin ({MARGIN}); pale dashes: its terrain fit ({BEARD} more). "
            "North is up; the grid is the world's chunks.", fill=(40, 40, 40), font=small)
    for side in range(4):
        sx, sz = STEP[side]
        turns = side
        g = C.turned(grid, turns)
        colours, solid = top_down(g, names)
        sizeX, sizeZ = (d, w) if turns % 2 else (w, d)
        hx, hz = sx * DISTANCE * 16, sz * DISTANCE * 16
        minX, minZ = hx + 8 - sizeX // 2, hz + 8 - sizeZ // 2
        cx, cz = 8 + sx * DISTANCE * 8, 8 + sz * DISTANCE * 8
        px0 = pad + (side % 2) * (panel + pad)
        py0 = pad + (side // 2) * (panel + title + pad) + title

        def to(x, z):
            return px0 + (x - cx + half) * s, py0 + (z - cz + half) * s

        canvas = Image.new("RGB", (panel, panel), (104, 150, 70))
        cd = ImageDraw.Draw(canvas)
        for k in range(-40, 40):
            gx = (k * 16 - cx + half) * s
            gz = (k * 16 - cz + half) * s
            if 0 <= gx < panel:
                cd.line((gx, 0, gx, panel), fill=(96, 140, 64))
            if 0 <= gz < panel:
                cd.line((0, gz, panel, gz), fill=(96, 140, 64))
        img.paste(canvas, (px0, py0))
        # The village: how far its pieces may reach, its start chunk.
        vx0, vz0 = to(-VILLAGE_REACH, -VILLAGE_REACH)
        vx1, vz1 = to(VILLAGE_REACH, VILLAGE_REACH)
        over = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(over).rectangle((vx0, vz0, vx1, vz1), fill=(222, 196, 140, 120))
        img.paste(Image.alpha_composite(img.convert("RGBA"), over).convert("RGB"))
        dr = ImageDraw.Draw(img)
        dashed_rect(dr, (vx0, vz0, vx1, vz1), (150, 110, 50))
        c0, c1 = to(0, 0), to(16, 16)
        dr.rectangle((*c0, *c1), fill=(150, 100, 50), outline=(70, 45, 20))
        dr.text((vx0 + 6, vz0 + 4), "village", fill=(70, 45, 20), font=big)
        dr.text((vx0 + 6, vz0 + 28), f"pieces within {VILLAGE_REACH}", fill=(70, 45, 20), font=small)
        # The hotel, its margin and its terrain fit.
        pic = Image.fromarray(colours.transpose(1, 0, 2))
        mask = Image.fromarray((solid.T * 255).astype(np.uint8))
        pic = pic.resize((int(sizeX * s), int(sizeZ * s)), Image.NEAREST)
        mask = mask.resize(pic.size, Image.NEAREST)
        hx0, hz0 = to(minX, minZ)
        img.paste(pic, (int(round(hx0)), int(round(hz0))), mask)
        dr = ImageDraw.Draw(img)
        hx1, hz1 = to(minX + sizeX, minZ + sizeZ)
        dr.rectangle((hx0, hz0, hx1, hz1), outline=(30, 30, 30), width=2)
        m0, m1 = to(minX - MARGIN, minZ - MARGIN), to(minX + sizeX + MARGIN, minZ + sizeZ + MARGIN)
        dashed_rect(dr, (*m0, *m1), (250, 250, 250), dash=4, width=1)
        b0, b1 = to(minX - MARGIN - BEARD, minZ - MARGIN - BEARD), to(minX + sizeX + MARGIN + BEARD, minZ + sizeZ + MARGIN + BEARD)
        dashed_rect(dr, (*b0, *b1), (200, 220, 200), dash=3, width=1)
        # Its forecourt toward the village.
        f = [(0, 1), (-1, 0), (0, -1), (1, 0)][turns]  # HotelPlan.front: the file's south, turned
        mid = (minX + sizeX / 2, minZ + sizeZ / 2)
        edge = (mid[0] + f[0] * sizeX / 2, mid[1] + f[1] * sizeZ / 2)
        arrow(dr, to(edge[0] - f[0] * 6, edge[1] - f[1] * 6), to(edge[0] + f[0] * 34, edge[1] + f[1] * 34), (220, 40, 40))
        # How far apart: the hotel's near edge and the village's reach (blocks).
        if side == 0:
            gap = 0 - (minZ + sizeZ)
        elif side == 1:
            gap = minX - 16
        elif side == 2:
            gap = minZ - 16
        else:
            gap = 0 - (minX + sizeX)
        dr.rectangle((px0, py0 - title, px0 + panel, py0), fill=(236, 232, 222))
        dr.text((px0, py0 - title + 2), f"{SIDES[side].capitalize()} of its village (turn {turns}): forecourt to the {FACES[side]}",
                fill=(20, 20, 20), font=big)
        dr.text((px0, py0 - title + 30), f"near edge {gap} blocks out of the village's start chunk", fill=(60, 60, 60), font=small)
        dr.rectangle((px0, py0, px0 + panel - 1, py0 + panel - 1), outline=(80, 80, 80))
    img.save(out)
    print(out)


def project(x, y, z, shape, scale):
    """city_render.render's place on the picture for block (x, y, z) of a grid of this shape (its marks' anchor)."""
    w, h, d = shape
    ox = d * 2 * scale + 4 * scale
    oy = 2 * h * scale + 4 * scale
    return (x - z) * 2 * scale + ox, (x + z - 2 * (y + 1)) * scale + oy - 4 * scale


def level(grid, names, people, route, middle, ground, cut, margin, scale, keep):
    """One storey's picture: everything from layer {@code cut} up left off, the people {@code keep} takes marked, the patrol drawn."""
    grid = grid.copy()
    grid[:, cut:, :] = 0
    if margin:
        grid = C.on_grass(grid, names, margin, ground)  # the world's ground round it (under the street the slab itself is shown)
    tmp = Path("/dev/shm" if Path("/dev/shm").is_dir() else ".") / "hotel_level.tmp.png"
    picture = C.render(grid, names, str(tmp), scale, None, ())
    tmp.unlink()
    dr = ImageDraw.Draw(picture)
    if route:
        stops = [project(middle[0] + dx + margin, ground + 1 + dy, middle[1] + dz + margin, grid.shape, scale) for dx, dy, dz in route]
        for a, b in zip(stops, stops[1:] + stops[:1]):
            dr.line((*a, *b), fill=(255, 255, 255), width=3)
        for p in stops:
            dr.ellipse((p[0] - 4, p[1] - 4, p[0] + 4, p[1] + 4), fill=(255, 255, 255), outline=(0, 0, 0))
    marks = {"hexkagune:human": (255, 214, 40), "hexkagune:ccg_investigator": (30, 110, 255)}
    for kind, x, y, z in people:
        if keep(y):
            sx, sy = project(x + margin, y, z + margin, grid.shape, scale)
            dr.ellipse((sx - 7, sy - 7, sx + 7, sy + 7), fill=marks.get(kind, (255, 255, 255)), outline=(0, 0, 0), width=2)
    body = np.any(np.abs(np.array(picture).astype(int) - np.array([132, 166, 204])) > 6, axis=2)
    ys, xs = np.nonzero(body)
    return picture.crop((max(0, xs.min() - 16), max(0, ys.min() - 16), min(picture.width, xs.max() + 16), min(picture.height, ys.max() + 16)))


def street(out):
    grid, names, people = C.load_site("hotel")
    ground, middle, route = site_numbers()
    scale = 2
    under = level(grid, names, people, (), middle, ground, ground, 0, scale, lambda y: y <= ground)
    floor = level(grid, names, people, route, middle, ground, 18, 24, scale, lambda y: y > ground)
    head = 130
    img = Image.new("RGB", (under.width + floor.width + 24, max(under.height, floor.height) + head), (236, 232, 222))
    img.paste(under, (0, head + (floor.height - under.height) // 2 if floor.height > under.height else head))
    img.paste(floor, (under.width + 24, head))
    dr = ImageDraw.Draw(img)
    big, small = font(24, True), font(19)
    humans = [y for k, x, y, z in people if k == "hexkagune:human"]
    agents = sum(1 for k, *_ in people if k == "hexkagune:ccg_investigator")
    below = sum(1 for y in humans if y <= ground)
    dr.text((24, 10), "The hotel at street level (layer " + str(ground) + " its slab's top). Left: under the street, its working floor cut into "
            "the slab. Right: the ground floor and forecourt (layer 18 up left off).", fill=(20, 20, 20), font=big)
    y = 46
    for colour, text in (((255, 214, 40), f"humans: {len(humans)}, two at each of its 10 homes ({below // 2} in the rooms under the street, "
                                          f"{(len(humans) - below) // 2} on the ground floor)"),
                         ((30, 110, 255), f"CCG investigators: {agents}, on its patrol round it"),
                         ((255, 255, 255), f"the patrol: {len(route)} stops on its roads and forecourt, joined in order")):
        dr.ellipse((24, y + 4, 42, y + 22), fill=colour, outline=(0, 0, 0), width=2)
        dr.text((52, y), text, fill=(20, 20, 20), font=small)
        y += 26
    img.save(out)
    print(out)


if __name__ == "__main__":
    mode, target = sys.argv[1], sys.argv[2]
    {"village": village, "street": street}[mode](target)
