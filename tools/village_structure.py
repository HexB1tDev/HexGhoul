#!/usr/bin/env python3
"""2.0.1: the village, from the owner's schematic (art/village/japanesevillagev4.schem, Sponge v2, saved by 1.16.5), to 1.20.1 structure
templates (src/main/resources/data/hexkagune/structures/village/t_<i>_<j>.nbt: a 5 x 5 grid of tiles joined by jigsaw blocks), its
template pools, and the village's map for the game (src/main/java/com/hex/kagune/control/village/VillageMap.java: its streets, homes and
patrol, around its middle). (1.3.71 to 2.0.0 made the Brick Village from a litematic the same way, in one piece.)

The schematic is a whole hillside village (288 x 86 x 272, its rock and a lake under it): a structure reaches at most 128 blocks from its
middle (the jigsaw's own limit, any turn), so a 256 x 256 window of it is used (WINDOW: every house kept; a strip of the mountain's
cobblestone on the north and east and the lake's east end are left out, the lake's cut edge made bank). It is placed sunk so its ground
(GROUND, the commonest top of its ground) lies at the world's surface: the rock under its ground is left to the world's own (below
GROUND - KEEP), except what shows (a lake's bed, a cave's walls). Blocks 1.20.1 does not have (BYG's cherry, Pixelmon's temple) are
swapped for the nearest vanilla ones, Pixelmon's signs, barriers, command blocks and fire left out, signs and banners written as 1.20.1
reads them, containers empty. Each human stands inside a house (its home), each investigator on the patrol's loop; each faces south
(yaw 0) and carries its offset to the village's middle in the template's frame (ForgeData HexVillage).

    python3 tools/village_structure.py
"""
import collections
import json
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import library_structure as L  # noqa: E402  (its NBT reader and writer)

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art" / "village" / "japanesevillagev4.schem"
DATA = ROOT / "src" / "main" / "resources" / "data" / "hexkagune"
TILES = DATA / "structures" / "village"
POOLS = DATA / "worldgen" / "template_pool" / "village"
MAP = ROOT / "src" / "main" / "java" / "com" / "hex" / "kagune" / "control" / "village" / "VillageMap.java"
DATA_VERSION = 3465  # 1.20.1
# The window of the schematic used (its corner x, z; 256 a side), and the tiles' edges inside it (5 x 5, the middle one round 128).
WINDOW = (10, 8)
SIZE = 256
SPLITS = [0, 51, 102, 154, 205, 256]
MIDDLE_TILE = (2, 2)
# Natural rock this far under the ground is kept everywhere (the world's dips filled); deeper, only where it shows.
KEEP = 3
# Air is written this far over the ground (the world's bumps cleared off its streets), and inside its buildings.
CLEAR = 3
# How many investigators patrol it (2.0.1, the owner: "I only see two of them when there should be at least a little more").
AGENTS = 6
# How many houses have people (two each; the 26 villager looks, never two alike in a village: 13 houses).
HOUSES = 13

NATURAL = {"stone", "dirt", "granite", "podzol", "andesite", "diorite", "gravel", "grass_block", "coal_ore", "coarse_dirt", "sand",
           "bedrock", "nether_quartz_ore", "infested_cobblestone", "iron_ore", "clay", "snow_block", "white_concrete_powder",
           "green_concrete_powder", "dirt_path"}
GROUNDS = {"grass_block", "dirt", "podzol", "coarse_dirt", "dirt_path", "gravel", "sand"}
DROP = {"barrier", "command_block", "chain_command_block", "repeating_command_block", "structure_block", "structure_void", "jigsaw",
        "fire", "pixelmon:poke_center_sign", "pixelmon:poke_mart_sign"}
SWAPS = {
    "grass_path": "dirt_path",
    "bedrock": "stone",
    "byg:pink_cherry_leaves": "cherry_leaves",
    "byg:cherry_log": "cherry_log",
    "byg:pink_orchid": "pink_tulip",
    "byg:pink_daffodil": "pink_petals",
    "byg:pink_cherry_foliage": "pink_petals",
    "byg:glowstone_lantern": "lantern",
    "pixelmon:temple_block": "stone_bricks",
    "pixelmon:temple_brick": "stone_bricks",
    "pixelmon:temple_brick_stairs": "stone_brick_stairs",
    "pixelmon:temple_stairs": "stone_brick_stairs",
    "pixelmon:temple_pillar": "chiseled_stone_bricks",
    "pixelmon:temple_pillar_fractured": "cracked_stone_bricks",
}
# The properties each swapped block keeps (the rest are the modded block's own).
KEEPS = {"cherry_leaves": ("distance",), "cherry_log": ("axis",), "lantern": ("hanging",),
         "stone_brick_stairs": ("facing", "half", "shape", "waterlogged")}
OPEN = ("minecraft:air", "minecraft:ladder", "minecraft:chain", "minecraft:water")
# Blocks a body walks through (their names' ends).
PASS = ("_carpet", "_button", "_pressure_plate", "_wall_banner", "_wall_sign", "minecraft:sweet_berry_bush", "minecraft:grass",
        "minecraft:fern", "minecraft:pink_petals", "minecraft:pink_tulip", "minecraft:dandelion", "minecraft:poppy", "_torch", "minecraft:snow")
EMPTY = "minecraft:empty"
# The signs kept (by a word on them): the rest were the old server's (shops, warps, gym leaders, players' books) and are left blank.
SIGNS_KEPT = ("Tea House", "Apothecary", "Police Station", "Officer", "Temple", "Hall | Of | Records", "Cherry-", "Crown of")


def val(t):
    return t.value if isinstance(t, L.Tag) else t


def short(name):
    return name[len("minecraft:"):] if name.startswith("minecraft:") else name


def parse_state(text):
    if "[" not in text:
        return text, {}
    name, rest = text.split("[", 1)
    return name, dict(kv.split("=", 1) for kv in rest.rstrip("]").split(","))


def state_text(name, props):
    return name + ("[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]" if props else "")


def convert(name, props, x, z):
    """A 1.16.5 (+ BYG, Pixelmon) block as 1.20.1 has it: (name, props), or None for air."""
    key = short(name)
    if key in DROP or name in DROP or key in ("air", "cave_air", "void_air"):
        return None
    if key in SWAPS or name in SWAPS:
        new = SWAPS.get(name, SWAPS.get(key))
        kept = {k: v for k, v in props.items() if k in KEEPS.get(new, ())}
        if new == "pink_petals":
            kept = {"facing": ("north", "east", "south", "west")[(x * 7 + z * 13) % 4], "flower_amount": str(2 + (x * 3 + z * 5) % 3)}
        name, props, key = "minecraft:" + new, kept, new
    if not name.startswith("minecraft:"):
        raise SystemExit(f"a modded block with no swap: {name}")
    if key.endswith("_leaves"):
        props = dict(props, persistent="true")
    return name, props


def main():
    root = L.read(SOURCE)
    r = {k: val(v) for k, v in val(root).items()}
    width, height, length = r["Width"], r["Height"], r["Length"]
    states = {v: k for k, v in ((k, val(v)) for k, v in r["Palette"].items())}
    ids = L.varints(bytes(b & 0xFF for b in r["BlockData"]))
    assert len(ids) == width * height * length
    x0, z0 = WINDOW
    raw = {i: parse_state(s) for i, s in states.items()}

    # The window's cells, converted: (x, y, z) -> (name, props), air left out.
    cells = {}
    for y in range(height):
        for z in range(SIZE):
            for x in range(SIZE):
                name, props = raw[ids[(y * length + z + z0) * width + x + x0]]
                c = convert(name, props, x, z)
                if c is not None:
                    cells[(x, y, z)] = c
    blocks_entities = {}
    for t in val(r["BlockEntities"])[1]:
        t = {k: val(v) for k, v in val(t).items()}
        px, py, pz = t["Pos"]
        p = (px - x0, py, pz - z0)
        if p in cells:
            blocks_entities[p] = t

    def name(p):
        c = cells.get(p)
        return c[0] if c else "minecraft:air"

    def water(p):
        return name(p) == "minecraft:water" or cells.get(p, ("", {}))[1].get("waterlogged") == "true"

    # The lake's cut edge: water on the window's rim becomes its bank (dirt, grass on top), so none runs out into the world.
    for (x, y, z), c in list(cells.items()):
        if c[0] == "minecraft:water" and (x in (0, SIZE - 1) or z in (0, SIZE - 1)):
            cells[(x, y, z)] = ("minecraft:grass_block" if name((x, y + 1, z)) == "minecraft:air" else "minecraft:dirt", {})

    top = {}
    for (x, y, z) in cells:
        if y > top.get((x, z), -1):
            top[(x, z)] = y
    # The ground: the commonest top of natural ground round the middle.
    tops = collections.Counter(top[(x, z)] for x in range(32, SIZE - 32) for z in range(32, SIZE - 32)
                               if (x, z) in top and short(name((x, top[(x, z)], z))) in GROUNDS)
    ground = tops.most_common(1)[0][0]
    street = ground + 1

    def natural(p):
        return short(name(p)) in NATURAL

    def shows(p):
        x, y, z = p
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            q = (x + d[0], y + d[1], z + d[2])
            if 0 <= q[0] < SIZE and 0 <= q[2] < SIZE and q[1] >= 0 and not natural(q):
                return True
        return False

    def roofed(x, y, z):
        return any(name((x, yy, z)) not in OPEN for yy in range(y + 2, min(height, y + 9)))

    def free(x, y, z):
        n = name((x, y, z))
        return n == "minecraft:air" or n in ("minecraft:ladder", "minecraft:chain") or any(n.endswith(k) for k in PASS)

    def stands(x, y, z):
        if not (0 <= x < SIZE and 0 <= z < SIZE and y > 0):
            return False
        under = name((x, y - 1, z))
        return under != "minecraft:air" and not free(x, y - 1, z) and not water((x, y - 1, z)) and free(x, y, z) and free(x, y + 1, z) \
            and not water((x, y, z))

    def enclosed(start, y, door):
        """True if what can be walked to from {@code start} at {@code y}, not through {@code door}, is all under a roof (a house)."""
        seen, todo = {start}, [start]
        while todo:
            c = todo.pop()
            if not roofed(c[0], y, c[1]) or len(seen) > 600:
                return False
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (c[0] + d[0], c[1] + d[1])
                if n in seen or n == door:
                    continue
                if not (0 <= n[0] < SIZE and 0 <= n[1] < SIZE):
                    return False
                if stands(n[0], y, n[1]):
                    seen.add(n)
                    todo.append(n)
        return True

    middle = (SIZE // 2, street, SIZE // 2)
    # Homes: inside each door (any but iron) that opens from outside into a house.
    homes = []
    for (x, y, z), (n, props) in cells.items():
        if not n.endswith("_door") or n == "minecraft:iron_door" or props.get("half") != "lower":
            continue
        f = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[props["facing"]]
        for inside, outside in (((x - f[0], z - f[1]), (x + f[0], z + f[1])), ((x + f[0], z + f[1]), (x - f[0], z - f[1]))):
            if stands(inside[0], y, inside[1]) and stands(outside[0], y, outside[1]) and enclosed(inside, y, (x, z)) \
                    and not enclosed(outside, y, (x, z)):
                deeper = (2 * inside[0] - x, 2 * inside[1] - z)
                spot = deeper if stands(deeper[0], y, deeper[1]) else inside
                homes.append((spot[0], y, spot[1]))
                break
    homes = sorted(set(homes))
    found = len(homes)
    # HOUSES of them, spread out (each next the farthest from those taken), starting nearest the middle.
    homes = [h for h in homes if abs(h[1] - street) <= 12 and math.hypot(h[0] - middle[0], h[2] - middle[2]) <= 112]
    taken = [min(homes, key=lambda h: (h[0] - middle[0]) ** 2 + (h[2] - middle[2]) ** 2)]
    while len(taken) < min(HOUSES, len(homes)):
        taken.append(max((h for h in homes if h not in taken),
                         key=lambda h: min((h[0] - t[0]) ** 2 + (h[2] - t[2]) ** 2 for t in taken)))
    homes = taken
    # Streets: open-sky ground a body stands on near the ground's height, every 4th cell either way.
    streets = []
    for x in range(2, SIZE - 2, 4):
        for z in range(2, SIZE - 2, 4):
            if math.hypot(x - middle[0], z - middle[2]) > 116:
                continue
            for y in range(street - 3, street + 7):
                if stands(x, y, z) and not roofed(x, y, z):
                    streets.append((x, y, z))
                    break
    rel = lambda p: (p[0] - middle[0], p[1] - middle[1], p[2] - middle[2])  # noqa: E731
    # The patrol: a ring of street spots round the middle (18 to 36 blocks out), in order round it, one every 20 degrees or so.
    ring = [p for p in streets if 18 <= math.hypot(p[0] - middle[0], p[2] - middle[2]) <= 36 and abs(p[1] - street) <= 4]
    route, last = [], None
    for p in sorted(ring, key=lambda p: math.atan2(p[2] - middle[2], p[0] - middle[0])):
        a = math.degrees(math.atan2(p[2] - middle[2], p[0] - middle[0]))
        if last is None or a - last >= 20.0:
            route.append(p)
            last = a

    # What goes in the templates: everything at or over the ground less KEEP, under it only what shows; air over the ground (CLEAR)
    # where the village's own ground is that high, and inside its buildings.
    out = {}
    for p, c in cells.items():
        if p[1] >= ground - KEEP or not natural(p) or shows(p):
            out[p] = c
    air = ("minecraft:air", {})
    for x in range(SIZE):
        for z in range(SIZE):
            t = top.get((x, z), -1)
            for y in range(ground + 1, height):
                if (x, y, z) in cells:
                    continue
                if (y <= ground + CLEAR and t >= ground) or (y <= t and roofed(x, y, z)):
                    out[(x, y, z)] = air

    people = []
    for k, h in enumerate(homes):
        people.append(("hexkagune:human", h, k))
    # 2.0: a second in every house, its own number after the first ones' (its look and name are the village's own shuffle at it).
    for k, h in enumerate(homes):
        people.append(("hexkagune:human", h, len(homes) + k))
    for k in range(AGENTS):
        people.append(("hexkagune:ccg_investigator", route[(k * len(route)) // AGENTS], k))

    write_tiles(out, blocks_entities, people, middle, height)
    # The structure starts sunk by its ground's height, so its ground lies at the world's surface (JigsawPlacement: the start piece's
    # lowest layer + 1 at start_height + the first free height there).
    structure = DATA / "worldgen" / "structure" / "village.json"
    spec = json.loads(structure.read_text())
    spec["start_height"] = {"absolute": -ground}
    structure.write_text(json.dumps(spec, indent=2) + "\n")
    write_map(middle, homes, streets, route)
    signs = [sign_text(t) for t in blocks_entities.values() if val(t["Id"]) == "minecraft:sign"]
    kinds = collections.Counter("air" if c[0] == "minecraft:air" else "natural" if short(c[0]) in NATURAL else "built" for c in out.values())
    print("kept:", dict(kinds))
    print(f"{len(out)} blocks in {len(SPLITS) - 1}x{len(SPLITS) - 1} tiles, height {height}, ground {ground} (sunk {ground}), "
          f"{found} homes found, {len(homes)} used, {AGENTS} investigators, {len(streets)} street spots, {len(route)} patrol spots, "
          f"middle {middle}")
    print("signs kept:", sorted(set(s for s in signs if any(k in s for k in SIGNS_KEPT))))
    preview(cells, top, middle, homes, streets, route, people)


def sign_text(t):
    out = []
    for i in range(1, 5):
        line = val(t.get(f"Text{i}", '""'))
        try:
            out.append(json.loads(line).get("text", "") if line.startswith("{") else "")
        except ValueError:
            out.append("")
    return " | ".join(out)


def tile_of(v):
    for i in range(len(SPLITS) - 1):
        if SPLITS[i] <= v < SPLITS[i + 1]:
            return i
    raise ValueError(v)


def tree():
    """The tiles' jigsaw tree from the middle one: along its row both ways, then up and down every column. {child: parent}."""
    mi, mj = MIDDLE_TILE
    n = len(SPLITS) - 1
    parent = {}
    for i in range(n):
        if i != mi:
            parent[(i, mj)] = (i + (1 if i < mi else -1), mj)
    for i in range(n):
        for j in range(n):
            if j != mj:
                parent[(i, j)] = (i, j + (1 if j < mj else -1))
    return parent


def block_entity(t, block):
    """A 1.16.5 block entity as 1.20.1 reads it (signs' text on their front; banners' patterns; containers empty)."""
    kind = val(t["Id"])
    out = {"id": L.s(kind)}
    if kind == "minecraft:sign":
        lines = []
        for i in range(1, 5):
            line = val(t.get(f"Text{i}", '""'))
            lines.append(line if line.startswith("{") or line.startswith('"') else json.dumps({"text": line}))
        # Only the village's own words: its places and its two verses (the old server's shops, warps, gym records and books blanked).
        if not any(k in sign_text(t) for k in SIGNS_KEPT):
            lines = ['""'] * 4
        color = val(t.get("Color", "black"))
        out["front_text"] = L.compound(messages=L.strings(*lines), color=L.s(color),
                                       has_glowing_text=L.Tag(L.BYTE, val(t.get("GlowingText", 0))))
        out["back_text"] = L.compound(messages=L.strings('""', '""', '""', '""'), color=L.s("black"), has_glowing_text=L.Tag(L.BYTE, 0))
        out["is_waxed"] = L.Tag(L.BYTE, 0)
    elif kind == "minecraft:banner":
        patterns = val(t.get("Patterns", L.Tag(L.LIST, (L.COMPOUND, []))))
        items = [{"Pattern": L.s(val(val(p)["Pattern"])), "Color": L.Tag(L.INT, val(val(p)["Color"]))} for p in patterns[1]]
        if items:
            out["Patterns"] = L.Tag(L.LIST, (L.COMPOUND, items))
    elif kind in ("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:dispenser", "minecraft:dropper",
                  "minecraft:hopper"):
        out["Items"] = L.Tag(L.LIST, (L.COMPOUND, []))
    return L.compound(**out)


def jigsaw(orientation, name, target, pool, final):
    state = ("minecraft:jigsaw", {"orientation": orientation})
    nbt = L.compound(id=L.s("minecraft:jigsaw"), name=L.s(name), target=L.s(target), pool=L.s(pool), final_state=L.s(final),
                     joint=L.s("aligned" if orientation.split("_")[0] not in ("up", "down") else "rollable"))
    return state, nbt


def write_tiles(out, entities_at, people, middle, height):
    n = len(SPLITS) - 1
    parent = tree()
    pieces = {(i, j): {} for i in range(n) for j in range(n)}
    nbts = {(i, j): {} for i in range(n) for j in range(n)}
    for (x, y, z), c in out.items():
        key = (tile_of(x), tile_of(z))
        local = (x - SPLITS[key[0]], y, z - SPLITS[key[1]])
        pieces[key][local] = c
        if (x, y, z) in entities_at and c[0] != "minecraft:air":
            nbts[key][local] = block_entity(entities_at[(x, y, z)], c[0])

    def final(key, local):
        c = pieces[key].get(local)
        return state_text(*c) if c else "minecraft:air"

    def place(key, local, orientation, name, target, pool):
        state, nbt = jigsaw(orientation, name, target, pool, final(key, local))
        pieces[key][local] = state
        nbts[key][local] = nbt

    def free_y(key_a, at_a, key_b, at_b):
        """The lowest y where neither cell holds a block entity (a jigsaw block takes the cell's place)."""
        for y in range(height):
            if (at_a[0], y, at_a[1]) not in nbts[key_a] and (at_b[0], y, at_b[1]) not in nbts[key_b]:
                return y
        raise SystemExit("no room for a jigsaw block")

    for child, par in sorted(parent.items()):
        ci, cj = child
        pi, pj = par
        wp, dp = SPLITS[pi + 1] - SPLITS[pi], SPLITS[pj + 1] - SPLITS[pj]
        wc, dc = SPLITS[ci + 1] - SPLITS[ci], SPLITS[cj + 1] - SPLITS[cj]
        if cj == pj:  # along the row: the same z inside both
            mid = dp // 2
            if ci > pi:
                a, b, fa, fb = (wp - 1, mid), (0, mid), "east", "west"
            else:
                a, b, fa, fb = (0, mid), (wc - 1, mid), "west", "east"
        else:  # along the column: the same x inside both
            mid = wp // 2
            if cj > pj:
                a, b, fa, fb = (mid, dp - 1), (mid, 0), "south", "north"
            else:
                a, b, fa, fb = (mid, 0), (mid, dc - 1), "north", "south"
        y = free_y(par, a, child, b)
        tag = f"hexkagune:village_{ci}_{cj}"
        place(par, (a[0], y, a[1]), fa + "_up", "minecraft:empty", tag, f"hexkagune:village/{ci}_{cj}")
        place(child, (b[0], y, b[1]), fb + "_up", tag, EMPTY, EMPTY)
    # The anchor: the village's middle, where the structure's start is (start_jigsaw_name).
    mt = MIDDLE_TILE
    at = (middle[0] - SPLITS[mt[0]], 0, middle[2] - SPLITS[mt[1]])
    place(mt, at, "down_south", "hexkagune:village_middle", EMPTY, EMPTY)

    entities = {(i, j): [] for i in range(n) for j in range(n)}
    for kind, (px, py, pz), k in people:
        key = (tile_of(px), tile_of(pz))
        lx, lz = px - SPLITS[key[0]], pz - SPLITS[key[1]]
        data = L.compound(HexVillage=L.compound(CentreX=L.Tag(L.INT, middle[0] - px), CentreY=L.Tag(L.INT, middle[1] - py),
                                                CentreZ=L.Tag(L.INT, middle[2] - pz), Home=L.Tag(L.INT, k if kind == "hexkagune:human" else -1)))
        nbt = L.compound(id=L.s(kind), PersistenceRequired=L.Tag(L.BYTE, 1), Rotation=L.floats(0.0, 0.0), Motion=L.doubles(0.0, 0.0, 0.0),
                         OnGround=L.Tag(L.BYTE, 1), ForgeData=data)
        entities[key].append({"blockPos": L.ints(lx, py, lz), "pos": L.doubles(lx + 0.5, float(py), lz + 0.5), "nbt": nbt})

    TILES.mkdir(parents=True, exist_ok=True)
    POOLS.mkdir(parents=True, exist_ok=True)
    for old in list(TILES.glob("*.nbt")) + list(POOLS.glob("*.json")):
        old.unlink()
    for (i, j), cellmap in sorted(pieces.items()):
        w, d = SPLITS[i + 1] - SPLITS[i], SPLITS[j + 1] - SPLITS[j]
        palette, index, blocks = [], {}, []
        for (x, y, z), (bn, props) in sorted(cellmap.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (bn, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": L.s(bn)}
                if props:
                    entry["Properties"] = L.compound(**{k: L.s(v) for k, v in sorted(props.items())})
                palette.append(entry)
            block = {"state": L.Tag(L.INT, index[key]), "pos": L.ints(x, y, z)}
            if (x, y, z) in nbts[(i, j)]:
                block["nbt"] = nbts[(i, j)][(x, y, z)]
            blocks.append(block)
        L.write(TILES / f"t_{i}_{j}.nbt", {
            "DataVersion": L.Tag(L.INT, DATA_VERSION),
            "size": L.ints(w, height, d),
            "palette": L.Tag(L.LIST, (L.COMPOUND, palette)),
            "blocks": L.Tag(L.LIST, (L.COMPOUND, blocks)),
            "entities": L.Tag(L.LIST, (L.COMPOUND, entities[(i, j)])),
        })
        pool = {"name": f"hexkagune:village/{i}_{j}", "fallback": EMPTY, "elements": [{"weight": 1, "element": {
            "element_type": "minecraft:single_pool_element", "location": f"hexkagune:village/t_{i}_{j}", "processors": "minecraft:empty",
            "projection": "rigid"}}]}
        (POOLS / f"{i}_{j}.json").write_text(json.dumps(pool, indent=2) + "\n")


def write_map(middle, homes, streets, route):
    """The map, relative to {@code middle} (the village's middle at street level), as strings (a big int[][] would not fit a class)."""
    def rows(points):
        return ";".join("%d,%d,%d" % (p[0] - middle[0], p[1] - middle[1], p[2] - middle[2]) for p in points)
    rel_streets = [(x - middle[0], z - middle[2]) for x, _, z in streets]
    radius = max(max(abs(x) for x, _ in rel_streets), max(abs(z) for _, z in rel_streets)) + 2
    MAP.parent.mkdir(parents=True, exist_ok=True)
    MAP.write_text(f'''package com.hex.kagune.control.village;

/**
 * 2.0.1: the village's map (pure; written by {{@code tools/village_structure.py}} from the owner's schematic: do not edit by hand), in the
 * template's frame relative to its middle at street level (x east, y up, z south, as the template lies unturned): where its people may
 * stand on its streets (open sky, every fourth block), each home (a spot inside a house's door), the investigators' patrol, and how far out
 * its perimeter is. (1.3.71 to 2.0.0: the Brick Village's, BrickVillage.)
 */
public final class VillageMap {{
    private VillageMap() {{
    }}

    /** Its perimeter: blocks from its middle, either way along x and z, past which its people never go. */
    public static final int PERIMETER = {radius};

    /** Spots on its streets ({{dx, dy, dz}} from its middle). */
    public static final int[][] STREETS = points("{rows(streets)}");

    /** The investigators' patrol: street spots in order round the middle ({{dx, dy, dz}} from it), walked as a loop. */
    public static final int[][] ROUTE = points("{rows(route)}");

    /** Each home's spot ({{dx, dy, dz}} from its middle), by number. */
    public static final int[][] HOMES = points("{rows(homes)}");

    private static int[][] points(String text) {{
        String[] parts = text.split(";");
        int[][] out = new int[parts.length][];
        for (int i = 0; i < parts.length; i++) {{
            String[] p = parts[i].split(",");
            out[i] = new int[] {{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])}};
        }}
        return out;
    }}
}}
''')



def preview(cells, top, middle, homes, streets, route, people):
    """docs/handoff/village-2.0.1.png: the window from above (ground by height, water, buildings), the middle, homes, streets, patrol."""
    from PIL import Image, ImageDraw
    scale = 3
    img = Image.new("RGB", (SIZE * scale, SIZE * scale))
    px = img.load()
    for x in range(SIZE):
        for z in range(SIZE):
            t = top.get((x, z), -1)
            n = short(cells[(x, t, z)][0]) if t >= 0 else "air"
            v = max(0, min(255, 60 + t * 3))
            c = (40, 70, 200) if n == "water" else (v // 2, v, v // 3) if n in NATURAL or n.endswith(("leaves", "grass", "fern")) \
                else (215, 160, 90)
            for dx in range(scale):
                for dz in range(scale):
                    px[x * scale + dx, z * scale + dz] = c
    d = ImageDraw.Draw(img)
    for x, _, z in streets:
        d.point((x * scale + 1, z * scale + 1), fill=(255, 255, 0))
    for k, (x, _, z) in enumerate(route):
        a = route[(k + 1) % len(route)]
        d.line((x * scale + 1, z * scale + 1, a[0] * scale + 1, a[2] * scale + 1), fill=(0, 200, 255), width=2)
    for x, _, z in homes:
        d.ellipse((x * scale - 4, z * scale - 4, x * scale + 5, z * scale + 5), outline=(255, 0, 0), width=2)
    for kind, (x, _, z), _ in people:
        if kind.endswith("investigator"):
            d.rectangle((x * scale - 3, z * scale - 3, x * scale + 4, z * scale + 4), fill=(20, 20, 20), outline=(255, 255, 255))
    m = middle
    d.line((m[0] * scale - 8, m[2] * scale, m[0] * scale + 8, m[2] * scale), fill=(255, 0, 255), width=2)
    d.line((m[0] * scale, m[2] * scale - 8, m[0] * scale, m[2] * scale + 8), fill=(255, 0, 255), width=2)
    for s in SPLITS[1:-1]:
        d.line((s * scale, 0, s * scale, SIZE * scale), fill=(255, 255, 255))
        d.line((0, s * scale, SIZE * scale, s * scale), fill=(255, 255, 255))
    img.save(ROOT / "docs" / "handoff" / "village-2.0.1.png")


if __name__ == "__main__":
    main()
