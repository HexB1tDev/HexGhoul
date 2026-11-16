#!/usr/bin/env python3
"""4.1.73 (the owner: "Make sure NPCS dont spawn in the ground at any point, especially official NPCS."): every NPC written into a structure
template (data/hexkagune/structures/: one file each, or a site's tiles put back together) checked where it stands: its body's box (its
width and height, the official NPCs' by their size) touches no block's collision shape, and something under its feet holds it up.

    python3 tools/npc_spots.py            # every NPC of every template, one line each that is wrong, a line per template
    python3 tools/npc_spots.py --check    # the same, ending 1 if any is wrong (NpcTests runs this)

What a template leaves unwritten is the world's after it is built: over its floor (its first layer, or a tiled site's ground layer) its site
is cleared to air (SitePrep), at and under its floor the world's ground is filled up to it. Shapes are interior_render's (the game's own,
to a sixteenth), but for what the game lets a body pass through (plants, torches, signs, banners, buttons, plates, rails, redstone, vines).
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import city_render as C  # noqa: E402
import interior_render as R  # noqa: E402
import library_structure as L  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/hexkagune/structures"
#: No collision in the game (noCollission): a body stands in them.
PASS = ("grass", "fern", "tall_grass", "large_fern", "dead_bush", "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "oxeye_daisy",
        "cornflower", "lily_of_the_valley", "wither_rose", "sunflower", "lilac", "rose_bush", "peony", "torch", "wall_torch", "soul_torch",
        "soul_wall_torch", "redstone_torch", "redstone_wall_torch", "redstone_wire", "lever", "tripwire", "tripwire_hook", "vine", "glow_lichen",
        "sugar_cane", "kelp", "kelp_plant", "seagrass", "tall_seagrass", "water", "lava", "light", "structure_void", "cobweb", "nether_portal",
        "pink_petals", "spore_blossom", "hanging_roots", "cave_vines", "cave_vines_plant", "twisting_vines", "weeping_vines", "big_dripleaf_stem",
        "small_dripleaf", "sweet_berry_bush", "fire", "soul_fire")
PASS_ENDS = ("_sapling", "_tulip", "_sign", "_banner", "_button", "_pressure_plate", "rail", "_mushroom", "_roots", "_fungus", "_coral",
             "_coral_fan", "_coral_wall_fan")
#: Each NPC's body (width, height): the game's (ControlEntities, the official ones by their size).
BODY = {"hexkagune:general": (0.69, 2.07), "hexkagune:flesh_thrall": (0.6, 1.95)}
DEFAULT = (0.6, 1.8)
EPS = 1e-4


def passes(name):
    n = R.short(name)
    return n in ("air", "cave_air", "void_air") or n in PASS or n.endswith(PASS_ENDS) or "flower" in n and "pot" not in n


def site_ground(site):
    text = (ROOT / "src/main/java/com/hex/kagune/control/village/SiteMaps.java").read_text()
    found = re.search(r'new Site\("' + re.escape(site) + r'", \d+, \d+, \d+, \d+, (\d+),', text)
    return int(found.group(1)) if found else 0


def load(path):
    """A template (or a site's tiles) as cells {(x, y, z): (name, props)}, its size, its NPCs [(id, x, y, z)] and its floor layer."""
    files = sorted(path.glob("t_*_*.nbt")) if path.is_dir() else [path]
    roots = {}
    for f in files:
        root = L.read(f)
        root = root.value if isinstance(root, L.Tag) else root
        ij = tuple(int(v) for v in f.stem.split("_")[1:]) if path.is_dir() else (0, 0)
        roots[ij] = root
    size = {k: [C._v(v) for v in C._v(r["size"])] for k, r in roots.items()}
    across = 1 + max(i for i, _ in roots)
    down = 1 + max(j for _, j in roots)
    xs, zs = [0], [0]
    for i in range(across):
        xs.append(xs[-1] + size[i, 0][0])
    for j in range(down):
        zs.append(zs[-1] + size[0, j][2])
    cells, people = {}, []
    for (i, j), root in roots.items():
        palette = []
        for p in C._v(root["palette"]):
            props = {k: C._v(v) for k, v in C._v(p["Properties"]).items()} if "Properties" in C._v(p) else {}
            palette.append((C._v(p["Name"]), props))
        for b in C._v(root["blocks"]):
            x, y, z = (C._v(v) for v in C._v(b["pos"]))
            cells[xs[i] + x, y, zs[j] + z] = palette[C._v(b["state"])]
        for e in C._v(root["entities"]):
            nbt = C._v(e["nbt"])
            kind = C._v(nbt["id"])
            if kind.startswith("hexkagune:"):
                x, y, z = (C._v(v) for v in C._v(e["pos"]))
                forge = C._v(nbt["ForgeData"]) if "ForgeData" in nbt else {}
                # An investigator at a desk (HqRules.SEAT): made in its chair's block, sat on the chair's step on its first tick (AgentDesk).
                seat = kind == "hexkagune:ccg_investigator" and bool(C._v(forge["HexCcgSeat"])) if "HexCcgSeat" in forge else False
                people.append((kind, xs[i] + x, y, zs[j] + z, seat))
    height = max(s[1] for s in size.values())
    floor = site_ground(path.name) if path.is_dir() else 0
    return cells, (xs[-1], height, zs[-1]), people, floor


class Unlit(R.Scene):
    """interior_render's scene for its shapes alone (its light, which a big site takes long over, left out)."""

    def _light(self):
        pass


class Shapes:
    """Each cell's collision boxes (blocks, 0..1), the template's written cells, else the world's: air over its floor, ground at and under."""

    def __init__(self, cells, size, floor):
        self.cells, self.size, self.floor = cells, size, floor
        solid = {k: v for k, v in cells.items() if not passes(v[0])}
        self.scene = Unlit(solid, size)
        self.memo = {}

    def boxes(self, x, y, z):
        key = (x, y, z)
        if key in self.memo:
            return self.memo[key]
        cell = self.cells.get(key)
        if cell is None:
            out = [(0, 0, 0, 1, 1, 1)] if y <= self.floor else []
        elif passes(cell[0]):
            out = []
        else:
            i = self.scene.grid[x, y, z] if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2] else 0
            raw = self.scene.boxes(i, x, y, z) if i else R.FULL
            # Fences, walls and fence gates (shut) stand 1.5 high to a body (their collision).
            n = R.short(cell[0])
            tall = n.endswith(("_fence", "_wall")) or n.endswith("_fence_gate") and cell[1].get("open") != "true"
            out = [(b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, (24 if tall and b[4] == 16 else b[4]) / 16, b[5] / 16) for b in raw]
        self.memo[key] = out
        return out


def judge(shapes, kind, x, y, z, seat=False):
    """What is wrong with an NPC standing at (x, y, z), or None. A seated investigator's own chair (the stair in its block) is its seat."""
    w, h = BODY.get(kind, DEFAULT)
    x0, x1, z0, z1 = x - w / 2, x + w / 2, z - w / 2, z + w / 2
    inside = []
    support = False
    for bx in range(int(x0 // 1), int(x1 // 1) + 1):
        for bz in range(int(z0 // 1), int(z1 // 1) + 1):
            for by in range(int((y - 1) // 1), int((y + h) // 1) + 1):
                for b in shapes.boxes(bx, by, bz):
                    ax0, ay0, az0, ax1, ay1, az1 = bx + b[0], by + b[1], bz + b[2], bx + b[3], by + b[4], bz + b[5]
                    if ax1 <= x0 + EPS or ax0 >= x1 - EPS or az1 <= z0 + EPS or az0 >= z1 - EPS:
                        continue
                    chair = seat and (bx, by, bz) == (int(x // 1), int(y // 1), int(z // 1)) \
                        and R.short(shapes.cells.get((bx, by, bz), ("air", {}))[0]).endswith("_stairs")
                    if chair:
                        support = True
                        continue
                    if ay1 > y + EPS and ay0 < y + h - EPS:
                        name = shapes.cells.get((bx, by, bz), ("world ground" if by <= shapes.floor else "air", {}))[0]
                        inside.append(f"{R.short(name) if ':' in name else name} at {bx},{by},{bz}")
                    elif abs(ay1 - y) <= 0.0625:
                        support = True
    if inside:
        return "in " + "; ".join(sorted(set(inside))[:3])
    if not support:
        return "nothing under its feet"
    return None


def main():
    wrong = 0
    total = 0
    for path in sorted(DATA.iterdir()):
        if path.name == "clearing.nbt" or not (path.is_dir() or path.suffix == ".nbt"):
            continue
        cells, size, people, floor = load(path)
        if not people:
            continue
        shapes = Shapes(cells, size, floor)
        bad = []
        seated = 0
        for kind, x, y, z, seat in people:
            total += 1
            seated += 1 if seat else 0
            why = judge(shapes, kind, x, y, z, seat)
            if why:
                bad.append(f"  {kind} at {x:.2f}, {y:.2f}, {z:.2f}: {why}")
        wrong += len(bad)
        print(f"{path.name}: {len(people)} NPCs ({seated} at their desks' chairs), {len(bad)} wrong")
        for line in bad:
            print(line)
    print(f"{total} NPCs in the templates, {wrong} wrong")
    if "--check" in sys.argv and wrong:
        sys.exit(1)


if __name__ == "__main__":
    main()
