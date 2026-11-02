#!/usr/bin/env python3
"""1.3.71: the Brick Village, from the owner's litematic (art/village/Brick_Village-from-abfielder.litematic, saved by 1.21.8) to a 1.20.1
structure template (src/main/resources/data/hexkagune/structures/brick_village.nbt), with its people in it, and the village's map for
the game (src/main/java/com/hex/kagune/control/village/BrickVillage.java: its streets and homes, around its middle).

The blocks 1.20.1 does not have are swapped for the nearest it does (the tuff family for andesite and stone bricks, the copper doors
and trapdoors for warped ones, a waxed copper trapdoor for an acacia one); the block entities are written as 1.20.1 reads them (blank
signs, plain banners, empty pots and chests); the armour stands keep their diamond armour (1.20.1's ArmorItems). Each human stands
inside a house (its home), each investigator on a street; each faces south (yaw 0) and carries its offset to the village's middle in
the template's frame (ForgeData HexVillage), so it can tell the village's turn from its own yaw once placed.

    python3 tools/village_structure.py
"""
import collections
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import library_structure as L  # noqa: E402  (its NBT reader and writer)

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art" / "village" / "Brick_Village-from-abfielder.litematic"
TARGET = ROOT / "src" / "main" / "resources" / "data" / "hexkagune" / "structures" / "brick_village.nbt"
MAP = ROOT / "src" / "main" / "java" / "com" / "hex" / "kagune" / "control" / "village" / "BrickVillage.java"
DATA_VERSION = 3465  # 1.20.1
# Up to this height every cell is written, air too (the streets stay clear of any hill); above it, only what is under a roof.
STREET_AIR = 12
# How many investigators patrol it.
AGENTS = 2

SWAPS = {
    "minecraft:polished_tuff": "minecraft:polished_andesite",
    "minecraft:polished_tuff_stairs": "minecraft:polished_andesite_stairs",
    "minecraft:polished_tuff_wall": "minecraft:andesite_wall",
    "minecraft:tuff_stairs": "minecraft:andesite_stairs",
    "minecraft:tuff_bricks": "minecraft:stone_bricks",
    "minecraft:tuff_brick_slab": "minecraft:stone_brick_slab",
    "minecraft:tuff_brick_stairs": "minecraft:stone_brick_stairs",
    "minecraft:tuff_brick_wall": "minecraft:stone_brick_wall",
    "minecraft:oxidized_copper_door": "minecraft:warped_door",
    "minecraft:oxidized_copper_trapdoor": "minecraft:warped_trapdoor",
    "minecraft:waxed_copper_trapdoor": "minecraft:acacia_trapdoor",
}
DOORS = ("minecraft:spruce_door", "minecraft:warped_door")
OPEN = ("minecraft:air", "minecraft:ladder", "minecraft:chain")
# Blocks a body walks through (their names' ends).
PASS = ("_carpet", "_button", "_pressure_plate", "_wall_banner", "_wall_sign", "minecraft:sweet_berry_bush")


def val(t):
    return t.value if isinstance(t, L.Tag) else t


def blocks_of(region, palette):
    size = val(region["Size"])
    sx, sy, sz = (abs(val(size[k])) for k in ("x", "y", "z"))
    bits = max(2, (len(palette) - 1).bit_length())
    longs = val(region["BlockStates"])
    mask = (1 << bits) - 1

    def at(i):
        start = i * bits
        li, off = start >> 6, start & 63
        v = (longs[li] & 0xFFFFFFFFFFFFFFFF) >> off
        if off + bits > 64:
            v |= (longs[li + 1] & 0xFFFFFFFFFFFFFFFF) << (64 - off)
        return v & mask

    cells = {}
    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                cells[(x, y, z)] = at((y * sz + z) * sx + x)
    return (sx, sy, sz), cells


def text(line):
    import json
    return json.dumps({"text": line}) if line else '""'


def main():
    root = L.read(SOURCE)
    region = val(val(root["Regions"])["Unnamed"])
    raw = [{k: val(v) for k, v in val(p).items()} for p in val(region["BlockStatePalette"])[1]]
    palette = []
    for p in raw:
        props = {k: val(v) for k, v in p.get("Properties", {}).items()}
        palette.append((SWAPS.get(p["Name"], p["Name"]), props))
    (sx, sy, sz), cells = blocks_of(region, raw)
    name = lambda pos: palette[cells[pos]][0] if pos in cells else "minecraft:air"
    top = max(y for (x, y, z), i in cells.items() if palette[i][0] != "minecraft:air")
    height = top + 1
    roof = {}
    for (x, y, z), i in cells.items():
        if palette[i][0] != "minecraft:air":
            roof[(x, z)] = max(roof.get((x, z), -1), y)

    def roofed(x, y, z):
        return any(name((x, yy, z)) not in OPEN for yy in range(y + 2, min(height, y + 9)))

    def free(x, y, z):
        n = name((x, y, z))
        return n in OPEN or any(n.endswith(k) for k in PASS)

    def stands(x, y, z):
        # The template's lowest layer is the walking level: the ground under it is the world's own.
        under = "minecraft:dirt" if y == 0 else name((x, y - 1, z))
        return 0 <= x < sx and 0 <= z < sz and (under not in OPEN or y == 0) and free(x, y, z) and free(x, y + 1, z)

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
                if not (0 <= n[0] < sx and 0 <= n[1] < sz):
                    return False
                if stands(n[0], y, n[1]):
                    seen.add(n)
                    todo.append(n)
        return True

    # Homes: inside each door that opens from the street into a house (what is walkable on its inner side is all roofed).
    homes = []
    for (x, y, z), i in cells.items():
        n, props = palette[i]
        if n not in DOORS or props.get("half") != "lower":
            continue
        f = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[props["facing"]]
        for inside, outside in (((x - f[0], z - f[1]), (x + f[0], z + f[1])), ((x + f[0], z + f[1]), (x - f[0], z - f[1]))):
            if stands(inside[0], y, inside[1]) and enclosed(inside, y, (x, z)) and stands(outside[0], y, outside[1]) \
                    and not enclosed(outside, y, (x, z)):
                deeper = (2 * inside[0] - x, 2 * inside[1] - z)
                spot = deeper if stands(deeper[0], y, deeper[1]) else inside
                homes.append((spot[0], y, spot[1], x, z))
                break
    homes = sorted(set(homes))
    centre = (sx // 2, 0, sz // 2)
    # Streets: open-sky ground a body stands on, every 4th cell either way (the patrols' and traders' spots).
    streets = []
    for x in range(0, sx, 4):
        for z in range(0, sz, 4):
            for y in (0, 1):  # on the ground (a step up at most): never a roof or a balcony
                if stands(x, y, z) and not roofed(x, y, z):
                    streets.append((x, y, z))
                    break
    # The template: every cell up to the street height, above it only what is solid or under a roof.
    out_palette, index, blocks = [], {}, []
    te = {(val(t["x"]), val(t["y"]), val(t["z"])): val(t) for t in val(region["TileEntities"])[1]}
    for y in range(height):
        for z in range(sz):
            for x in range(sx):
                n, props = palette[cells[(x, y, z)]]
                if n == "minecraft:air" and y > STREET_AIR and roof.get((x, z), -1) <= y:
                    continue
                key = (n, tuple(sorted(props.items())))
                if key not in index:
                    index[key] = len(out_palette)
                    entry = {"Name": L.s(n)}
                    if props:
                        entry["Properties"] = L.compound(**{k: L.s(v) for k, v in sorted(props.items())})
                    out_palette.append(entry)
                block = {"state": L.Tag(L.INT, index[key]), "pos": L.ints(x, y, z)}
                entity = te.get((x, y, z))
                if entity is not None:
                    block["nbt"] = block_entity(entity, n)
                blocks.append(block)
    entities = [armour_stand(val(e), region) for e in val(region["Entities"])[1]]
    people = []
    for k, (hx, hy, hz, dx, dz) in enumerate(homes):
        people.append(("hexkagune:human", (hx, hy, hz), k))
    # 2.0 (the owner: "Add more humans ... per village"): a second in every house, its own number after the first ones' (each number is
    # the villager's slot: its look and its name are the village's own shuffle at that slot, so no two in a village share either).
    for k, (hx, hy, hz, dx, dz) in enumerate(homes):
        people.append(("hexkagune:human", (hx, hy, hz), len(homes) + k))
    near = sorted(streets, key=lambda p: (p[0] - centre[0]) ** 2 + (p[2] - centre[2]) ** 2)
    for k in range(AGENTS):
        people.append(("hexkagune:ccg_investigator", near[3 + k * 5], k))
    for kind, (px, py, pz), k in people:
        data = L.compound(HexVillage=L.compound(CentreX=L.Tag(L.INT, centre[0] - px), CentreY=L.Tag(L.INT, centre[1] - py),
                                                CentreZ=L.Tag(L.INT, centre[2] - pz), Home=L.Tag(L.INT, k if kind == "hexkagune:human" else -1)))
        nbt = L.compound(id=L.s(kind), PersistenceRequired=L.Tag(L.BYTE, 1), Rotation=L.floats(0.0, 0.0), Motion=L.doubles(0.0, 0.0, 0.0),
                         OnGround=L.Tag(L.BYTE, 1), ForgeData=data)
        entities.append({"blockPos": L.ints(px, py, pz), "pos": L.doubles(px + 0.5, float(py), pz + 0.5), "nbt": nbt})
    template = {
        "DataVersion": L.Tag(L.INT, DATA_VERSION),
        "size": L.ints(sx, height, sz),
        "palette": L.Tag(L.LIST, (L.COMPOUND, out_palette)),
        "blocks": L.Tag(L.LIST, (L.COMPOUND, blocks)),
        "entities": L.Tag(L.LIST, (L.COMPOUND, entities)),
    }
    L.write(TARGET, template)
    write_map(centre, homes, streets, sx, sz)
    print(f"{TARGET.relative_to(ROOT)}: {sx}x{height}x{sz}, {len(out_palette)} states, {len(blocks)} blocks, {len(entities)} entities "
          f"({len(homes)} homes, {AGENTS} investigators), centre {centre}, {len(streets)} street spots")


def block_entity(t, block):
    """A 1.21 block entity as 1.20.1 reads it: its id and nothing of 1.21's components; signs' blank text as JSON."""
    out = {"id": L.s(val(t["id"]))}
    if val(t["id"]) == "minecraft:sign":
        for side in ("front_text", "back_text"):
            s = val(t[side])
            lines = [val(m) for m in val(s["messages"])[1]]
            out[side] = L.compound(messages=L.strings(*[text(m if isinstance(m, str) else "") for m in lines]),
                                   color=L.s(val(s.get("color", "black"))), has_glowing_text=L.Tag(L.BYTE, val(s.get("has_glowing_text", 0))))
        out["is_waxed"] = L.Tag(L.BYTE, val(t.get("is_waxed", 0)))
    if val(t["id"]) in ("minecraft:chest", "minecraft:barrel"):
        out["Items"] = L.Tag(L.LIST, (L.COMPOUND, []))
    return L.compound(**out)


def armour_stand(e, region):
    """An armour stand where the litematic has it (relative to the region's corner), its armour as 1.20.1 keeps it."""
    pos = [val(p) for p in val(e["Pos"])[1]]
    at = val(region["Position"])
    # Relative to the region's anchor (its Position); the template's corner is the region's lowest corner (y 0, z 0 here).
    x, y, z = pos[0] + val(at["x"]), pos[1] + val(at["y"]), pos[2] + val(at["z"])
    eq = {k: val(v) for k, v in val(e.get("equipment", L.Tag(L.COMPOUND, {}))).items()}

    def item(slot):
        if slot not in eq:
            return {}
        return {"id": L.s(val(eq[slot]["id"])), "Count": L.Tag(L.BYTE, 1)}
    rot = [val(r) for r in val(e["Rotation"])[1]]
    nbt = L.compound(id=L.s("minecraft:armor_stand"), ArmorItems=L.Tag(L.LIST, (L.COMPOUND, [item("feet"), item("legs"), item("chest"), item("head")])),
                     HandItems=L.Tag(L.LIST, (L.COMPOUND, [{}, {}])), Rotation=L.floats(rot[0], 0.0),
                     ShowArms=L.Tag(L.BYTE, val(e.get("ShowArms", 0))), NoBasePlate=L.Tag(L.BYTE, val(e.get("NoBasePlate", 0))))
    return {"blockPos": L.ints(int(x // 1), int(y // 1), int(z // 1)), "pos": L.doubles(x, y, z), "nbt": nbt}


def write_map(middle, homes, streets, sx, sz):
    """The map, relative to {@code middle} (the village's middle at street level; its bell hangs on a tower)."""
    def rows(points):
        return ",\n".join("        {%d, %d, %d}" % p for p in points)
    import math
    rel_streets = [(x - middle[0], y - middle[1], z - middle[2]) for x, y, z in streets]
    rel_homes = [(x - middle[0], y - middle[1], z - middle[2]) for x, y, z, _, _ in homes]
    # The patrol: a ring of street spots round the middle (between 14 and 34 blocks out), in order round it, one every 20 degrees or so.
    ring = [p for p in rel_streets if 14 <= math.hypot(p[0], p[2]) <= 34]
    route, last = [], None
    for p in sorted(ring, key=lambda p: math.atan2(p[2], p[0])):
        a = math.degrees(math.atan2(p[2], p[0]))
        if last is None or a - last >= 20.0:
            route.append(p)
            last = a
    radius = max(max(abs(x) for x, _, _ in rel_streets), max(abs(z) for _, _, z in rel_streets)) + 2
    MAP.parent.mkdir(parents=True, exist_ok=True)
    MAP.write_text(f'''package com.hex.kagune.control.village;

/**
 * 1.3.71: the Brick Village's map (pure; written by {{@code tools/village_structure.py}} from the owner's litematic: do not edit by hand),
 * in the template's frame relative to its middle at street level (x east, y up, z south, as the template lies unturned): where its people may stand on
 * its streets (open sky, every fourth block), each home (a spot inside each door that opens into a house), and how far out its
 * perimeter is.
 */
public final class BrickVillage {{
    private BrickVillage() {{
    }}

    /** Its perimeter: blocks from its middle, either way along x and z, past which its people never go. */
    public static final int PERIMETER = {radius};

    /** Spots on its streets ({{dx, dy, dz}} from its middle). */
    public static final int[][] STREETS = {{
{rows(rel_streets)}
    }};

    /** The investigators' patrol: street spots in order round the middle ({{dx, dy, dz}} from it), walked as a loop. */
    public static final int[][] ROUTE = {{
{rows(route)}
    }};

    /** Each home's spot ({{dx, dy, dz}} from its middle), by number. */
    public static final int[][] HOMES = {{
{rows(rel_homes)}
    }};
}}
''')


if __name__ == "__main__":
    main()
