#!/usr/bin/env python3
"""1.3.24: Touka's and Ken's own places, from the owner's schematics, as 1.20.1 structure templates with each of them in theirs.

    python3 tools/companion_structures.py [--blocks blocks.json]

  art/anteiku.schem     -> src/main/resources/data/hexkagune/structures/anteiku.nbt      Touka behind the café's bar counter; 4.1.68: Ken
                                                                                          on its roof (the owner: "Ken's cybertower is now
                                                                                          fully removed, he is to spawn on top of Anteikou.")
  art/reaper/arima_mansion.schem -> src/main/resources/data/hexkagune/structures/reaper_mansion.nbt  4.1.24: Arima in its garden
  art/exploration/circus.schematic -> src/main/resources/data/hexkagune/structures/circus.nbt  4.1.48: four Clowns in the tent
  art/exploration/GasMask_hideout.schem -> src/main/resources/data/hexkagune/structures/gas_hideout.nbt  4.1.54: Banjou in the cottage, four Gas Masks

4.1.48: the circus tent is the old MCEdit "Alpha" format (numeric block ids and data in Blocks/Data: alpha() maps the few it has to 1.20.1's
states). It has no floor (it stood on its world's ground): a floor of dirt path is laid under everything inside its outer ring (floor()),
grass under its tufts and its flower; the anchor is the floor's middle. Several people can live in one place ("people").

4.1.68 (the owner: "Ken's cybertower is now fully removed, he is to spawn on top of Anteikou."): Ken's tower (4.0.9's Litematica file,
cut to fit 1.20.1's height) is gone; Ken is made with Anteiku, on its flat roof (the spruce deck, y 29), at its south edge. The Litematica
reader (litematic()) and the swaps for 1.21's blocks stay for any newer file.

Anteiku (saved by 1.15) is written whole, its air too (so nothing of the ground is left inside it); its walls' old sides (true/false)
become 1.20.1's (low/none) and every leaf is made persistent (its hedges have no logs: they would rot away).

--blocks: PrismarineJS minecraft-data's 1.20 blocks.json, to check every block state written is one 1.20.1 has (it fails if not).
"""
import gzip
import io
import json
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from library_structure import BYTE, COMPOUND, DOUBLE, END, FLOAT, INT, LIST, LONG, STRING, Tag, _write, read, varints  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art"
OUT = ROOT / "src" / "main" / "resources" / "data" / "hexkagune" / "structures"
DATA_VERSION = 3465  # 1.20.1

#: 4.1.48: the saved data of the circus's Clown wearing mask {@code mask} (1..5): GhoulNpcBrain.CIRCUS, CIRCUS_MASK, GANG_ID (the
#: circus's own gang number, -1: a roaming gang's is never under 1), kept with the world.
CIRCUS_GANG = -1


def CLOWN(mask):
    return {"HexCircus": ("byte", 1), "HexCircusMask": ("int", mask), "HexGang": ("int", CIRCUS_GANG)}


#: 4.1.54: the saved data of the Gas Mask hideout's Gas Mask wearing mask {@code mask} (1..5): GhoulNpcBrain.GAS_HIDEOUT, CIRCUS_MASK, GANG_ID
#: (the hideout's own gang number, -2: a roaming gang's is never under 1, the circus's is -1), kept with the world.
GAS_GANG = -2


def GAS_MASK(mask):
    return {"HexGasHideout": ("byte", 1), "HexCircusMask": ("int", mask), "HexGang": ("int", GAS_GANG)}


PLACES = {
    "anteiku": {
        "source": "anteiku.schem",
        # Behind the bar counter on the café floor (the aisle between the counter and the coffee machines), facing the tables (south).
        # 4.1.68 (the owner: "Ken's cybertower is now fully removed, he is to spawn on top of Anteikou."): Ken on the café's flat roof (its
        # spruce deck, y 29), just inside the south parapet, looking out over the street (south), where he sits (Persona.sitsAtHome).
        "people": [
            {"who": "hexkagune:touka", "at": (17, 10, 8), "yaw": 0.0},
            {"who": "hexkagune:ken", "at": (17, 30, 17), "yaw": 0.0},
        ],
        "keep_air": True, "crop": None, "anchor": None,
    },
    "reaper_mansion": {
        "source": "reaper/arima_mansion.schem",
        # 4.1.24 (the owner: "outside in the garden, like in this place"): Kishou Arima, the White Reaper, on the striped lawn north of the
        # house, facing it (south). A Rank 10 investigator with Dawn and the Reaper's look, alone, at home here (AgentBrain.wake).
        "who": "hexkagune:ccg_investigator", "at": (42, 7, 22), "yaw": 0.0,
        "who_data": {"HexCcgAgent": ("long", 9 | 5 << 8 | 1 << 16), "HexCcgAgentRankLevel": ("int", 10), "HexCcgAgentMansion": ("byte", 1)},
        # Its bottom layer is empty (cropped); its ground is one block thick at y 6 over nothing but its two pools' basins: what is under
        # the lawn is filled (dirt, then stone), or the ground would be left hollow under it. The anchor (on the lawn's layer, at the
        # middle) puts that layer on the ground and centres it on its chunk (121 blocks long).
        "keep_air": False, "crop": (1, 28), "fill_below": 6, "anchor": (39, 6, 60), "anchor_name": "hexkagune:mansion_anchor",
    },
    "circus": {
        "source": "exploration/circus.schematic",
        # 4.1.48 (the owner chose "One per world + clowns"): four of the Clowns inside the tent, each in a different mask, all of one gang
        # (HexGang: a fixed number, so they are each other's: GhoulNpcBrain.mates), kept with the world; each strolls about its spot.
        "people": [
            {"who": "hexkagune:ghoul", "at": (22, 0, 8), "yaw": 0.0, "who_data": CLOWN(1)},
            {"who": "hexkagune:ghoul", "at": (54, 0, 8), "yaw": 0.0, "who_data": CLOWN(2)},
            {"who": "hexkagune:ghoul", "at": (28, 0, 32), "yaw": 180.0, "who_data": CLOWN(3)},
            {"who": "hexkagune:ghoul", "at": (48, 0, 18), "yaw": 90.0, "who_data": CLOWN(4)},
            # 4.1.49 (the owner: Roma "spawns in this circus tent"): the ringmaster in the middle of the ring, facing the way in (south).
            {"who": "hexkagune:roma", "at": (38, 0, 24), "yaw": 0.0},
        ],
        "keep_air": False, "crop": None, "floor": "minecraft:dirt_path", "anchor": (38, -1, 24), "anchor_name": "hexkagune:circus_anchor",
    },
    "gas_hideout": {
        "source": "exploration/GasMask_hideout.schem",
        # 4.1.54 (the owner: Banjou "Lives in the small house in the schematic"; asked which, "The cottage": the main cherry house with the
        # pink roof): Banjou on the cottage's ground floor, facing its cherry doors (east). Four of the Gas Masks about it, each in a different
        # mask, all of one gang (HexGang), kept with the world: two outside the front door (south), one in the walled garden, one in its
        # little crimson-roofed garden house. Its bottom layer is its own ground (dirt, grass, moss): the anchor in its middle puts it on the
        # world's surface.
        "people": [
            {"who": "hexkagune:banjou", "at": (7, 2, 11), "yaw": -90.0},
            {"who": "hexkagune:ghoul", "at": (10, 1, 18), "yaw": 0.0, "who_data": GAS_MASK(1)},
            {"who": "hexkagune:ghoul", "at": (15, 1, 18), "yaw": 0.0, "who_data": GAS_MASK(2)},
            {"who": "hexkagune:ghoul", "at": (19, 2, 8), "yaw": 90.0, "who_data": GAS_MASK(3)},
            {"who": "hexkagune:ghoul", "at": (20, 2, 13), "yaw": 0.0, "who_data": GAS_MASK(4)},
        ],
        "keep_air": False, "crop": None, "anchor": (14, 0, 9), "anchor_name": "hexkagune:gas_hideout_anchor",
    },
}

RENAMED = {
    # 4.1.24: the mansion's grass (1.20.3 renamed it).
    "minecraft:short_grass": "minecraft:grass",
    # 4.0.9: the new tower's tuff (1.20.3's and 1.21's) as 1.20.1's nearest.
    "minecraft:chiseled_tuff_bricks": "minecraft:chiseled_stone_bricks",
    "minecraft:polished_tuff_wall": "minecraft:andesite_wall",
    "minecraft:iron_chain": "minecraft:chain",
    "minecraft:polished_tuff_slab": "minecraft:polished_andesite_slab",
    "minecraft:tuff_slab": "minecraft:andesite_slab",
    "minecraft:tuff_stairs": "minecraft:andesite_stairs",
    "minecraft:polished_tuff": "minecraft:polished_andesite",
    "minecraft:polished_tuff_stairs": "minecraft:polished_andesite_stairs",
}
LEFT_OUT = {"minecraft:trial_spawner"}
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def parse(state):
    if "[" in state:
        name, props = state[:-1].split("[", 1)
        return name, dict(kv.split("=", 1) for kv in props.split(","))
    return state, {}


def convert(state):
    """(name, props) for 1.20.1, or None for air (and what is left out)."""
    name, props = parse(state)
    if name in AIR or name in LEFT_OUT:
        return None
    name = RENAMED.get(name, name)
    if name.endswith("_wall") and any(props.get(side) in ("true", "false") for side in ("north", "south", "east", "west")):
        # 1.15's wall sides (connected or not) are 1.16+'s heights.
        for side in ("north", "south", "east", "west"):
            if side in props:
                props[side] = "low" if props[side] == "true" else "none"
    if name.endswith("_leaves"):
        props["persistent"] = "true"
    return name, props


def text(state_name, props):
    return state_name + ("[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]" if props else "")


class Registry:
    """1.20.1's block states (minecraft-data's blocks.json), to check what is written."""

    def __init__(self, path):
        self.blocks = {}
        for b in json.loads(Path(path).read_text()):
            props = {}
            for st in b.get("states", []):
                if st["type"] == "bool":
                    values = ["true", "false"]
                elif st["type"] == "int":
                    values = [str(v) for v in st["values"]] if "values" in st else [str(i) for i in range(st["num_values"])]
                else:
                    values = st["values"]
                props[st["name"]] = set(values)
            self.blocks["minecraft:" + b["name"]] = props

    def check(self, name, props):
        assert name in self.blocks, f"1.20.1 has no {name}"
        for k, v in props.items():
            assert k in self.blocks[name], f"1.20.1's {name} has no {k}"
            assert v in self.blocks[name][k], f"1.20.1's {name} has no {k}={v}"


def block_entity(raw):
    """A Sponge schematic's block entity (Id, Pos, the rest) as a template's (id, the rest)."""
    out = {}
    for key, tag in raw.items():
        if key == "Pos":
            continue
        out["id" if key == "Id" else key] = tag
    return out


def unpack(longs, bits, count):
    """A Litematica region's block states: {@code count} entries of {@code bits} each, packed tight across its longs."""
    mask = (1 << bits) - 1
    u = [v & 0xFFFFFFFFFFFFFFFF for v in longs]
    out = []
    for i in range(count):
        start = i * bits
        li, off = start >> 6, start & 63
        value = u[li] >> off
        if off + bits > 64:
            value |= u[li + 1] << (64 - off)
        out.append(value & mask)
    return out


def litematic(path):
    """(width, height, length, palette by index (state text), cells x-fastest then z then y): a one-region Litematica file."""
    root = read(path)
    (region,) = root["Regions"].value.values()
    r = region.value
    width, height, length = (abs(r["Size"].value[k].value) for k in ("x", "y", "z"))
    by_index = {}
    for i, entry in enumerate(r["BlockStatePalette"].value[1]):
        props = {k: v.value for k, v in entry["Properties"].value.items()} if "Properties" in entry else {}
        by_index[i] = text(entry["Name"].value, props)
    bits = max(2, (len(by_index) - 1).bit_length())
    cells = unpack(r["BlockStates"].value, bits, width * height * length)
    # Litematica's order is x fastest, then z, then y: as a Sponge schematic's.
    return width, height, length, by_index, cells


#: 4.1.48: the circus tent's blocks (id, data) as 1.20.1's states (an Alpha schematic's numbers; every other air).
ALPHA = {
    (35, 0): "minecraft:white_wool",
    (35, 14): "minecraft:red_wool",
    (89, 0): "minecraft:glowstone",
    (17, 1): "minecraft:spruce_log[axis=y]",  # spruce (1), upright (the axis bits 0)
    (31, 1): "minecraft:grass",               # tall grass's grass: 1.20.1's grass
    (37, 0): "minecraft:dandelion",
}
PLANTS = {"minecraft:grass", "minecraft:dandelion"}


def alpha(path):
    """4.1.48: (width, height, length, palette by index (state text), cells x-fastest then z then y): an MCEdit Alpha schematic."""
    root = read(path)
    width, height, length = root["Width"].value, root["Height"].value, root["Length"].value
    ids, data = root["Blocks"].value, root["Data"].value
    by_index, index, cells = {0: "minecraft:air"}, {}, []
    for i in range(width * height * length):
        b, d = ids[i] & 0xFF, data[i] & 0x0F
        if b == 0:
            cells.append(0)
            continue
        assert (b, d) in ALPHA, f"{path.name}: no 1.20.1 state for block {b}:{d}"
        if (b, d) not in index:
            index[(b, d)] = len(by_index)
            by_index[len(by_index)] = ALPHA[(b, d)]
        cells.append(index[(b, d)])
    return width, height, length, by_index, cells


def floor(width, height, length, by_index, cells):
    """
    4.1.48: the cells (x, z) a floor is laid under: everything the tent covers (any of its blocks but its plants, at any height, seen from
    above) and everything those close in (what can't be reached from the edge of the schematic without crossing it).
    """
    covered = set()
    for y in range(height):
        for z in range(length):
            row = (y * length + z) * width
            for x in range(width):
                state = by_index[cells[row + x]]
                if state not in AIR and state not in PLANTS:
                    covered.add((x, z))
    outside, todo = set(), [(x, z) for x in range(width) for z in (0, length - 1)] + [(x, z) for z in range(length) for x in (0, width - 1)]
    while todo:
        x, z = todo.pop()
        if not (0 <= x < width and 0 <= z < length) or (x, z) in outside or (x, z) in covered:
            continue
        outside.add((x, z))
        todo += [(x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)]
    return {(x, z) for x in range(width) for z in range(length) if (x, z) not in outside}


def build(key, place, registry):
    source = ART / place["source"]
    entities = {}
    if source.suffix == ".schematic":
        width, height, length, by_index, cells = alpha(source)
    elif source.suffix == ".litematic":
        width, height, length, by_index, cells = litematic(source)
    else:
        schem = read(source)
        if "Schematic" in schem:
            schem = schem["Schematic"].value
        width, height, length = schem["Width"].value, schem["Height"].value, schem["Length"].value
        by_index = {tag.value: state for state, tag in schem["Palette"].value.items()}
        cells = varints(schem["BlockData"].value)
        if "BlockEntities" in schem:
            for raw in schem["BlockEntities"].value[1]:
                x, y, z = raw["Pos"].value
                entities[(x, y, z)] = block_entity(raw)
    assert len(cells) == width * height * length, f"{key}: the block data does not fill the schematic"
    y0, y1 = place["crop"] or (0, height - 1)
    # 4.0.9: the layers kept, bottom up (what is cropped and dropped left out), each written where it now stands.
    layers = [y for y in range(y0, y1 + 1) if not any(a <= y < b for a, b in place.get("drop", ()))]
    converted = {i: convert(state) for i, state in by_index.items()}

    palette, index = [], {}

    def state_index(name, props):
        k = (name, tuple(sorted(props.items())))
        if k not in index:
            if registry:
                registry.check(name, props)
            index[k] = len(palette)
            palette.append((name, props))
        return index[k]

    air = state_index("minecraft:air", {}) if place["keep_air"] else None
    anchor = place.get("anchor")
    blocks = io.BytesIO()
    count = 0
    lift = 0
    if place.get("floor"):
        # 4.1.48: the floor, a layer of its own under the schematic's bottom one (grass under a plant: it would not stand on a path).
        under = floor(width, height, length, by_index, cells)
        plants = {(x, z) for z in range(length) for x in range(width) if by_index[cells[z * width + x]] in PLANTS}
        for z in range(length):
            for x in range(width):
                if (x, z) not in under and (x, z) not in plants:
                    continue
                c = ("minecraft:grass_block", {"snowy": "false"}) if (x, z) in plants else (place["floor"], {})
                nbt = None
                if anchor and (x, -1, z) == anchor:
                    final = text(*c)
                    c = ("minecraft:jigsaw", {"orientation": "up_north"})
                    nbt = {"id": Tag(STRING, "minecraft:jigsaw"), "name": Tag(STRING, place["anchor_name"]), "target": Tag(STRING, "minecraft:empty"),
                           "pool": Tag(STRING, "minecraft:empty"), "final_state": Tag(STRING, final), "joint": Tag(STRING, "rollable")}
                write_block(blocks, state_index(*c), (x, 0, z), nbt)
                count += 1
        assert anchor is None or (anchor[0], anchor[2]) in under, f"{key}: the anchor is off the floor"
        lift = 1
    for out_y, y in enumerate(layers, start=lift):
        for z in range(length):
            row = (y * length + z) * width
            for x in range(width):
                c = converted[cells[row + x]]
                if c is None and y < place.get("fill_below", -1):
                    # 4.1.24: under the ground, earth, not a hollow.
                    c = ("minecraft:dirt", {}) if y >= place["fill_below"] - 2 else ("minecraft:stone", {})
                nbt = None
                if anchor and (x, y, z) == anchor:
                    # The anchor: a jigsaw block that becomes what was here once the structure is placed.
                    final = text(*c) if c else "minecraft:air"
                    c = ("minecraft:jigsaw", {"orientation": "up_north"})
                    nbt = {"id": Tag(STRING, "minecraft:jigsaw"), "name": Tag(STRING, place["anchor_name"]), "target": Tag(STRING, "minecraft:empty"),
                           "pool": Tag(STRING, "minecraft:empty"), "final_state": Tag(STRING, final), "joint": Tag(STRING, "rollable")}
                if c is None:
                    if air is None:
                        continue
                    i = air
                else:
                    i = state_index(*c)
                    if nbt is None and (x, y, z) in entities:
                        nbt = entities[(x, y, z)]
                write_block(blocks, i, (x, out_y, z), nbt)
                count += 1
    figures = []
    for person in place.get("people", [place]):
        wx, wy, wz = person["at"]
        assert by_index[cells[(wy * length + wz) * width + wx]] in AIR and by_index[cells[((wy + 1) * length + wz) * width + wx]] in AIR, \
            f"{key}: {person['who']} would stand in a block"
        if wy == 0 and place.get("floor"):
            assert (wx, wz) in under, f"{key}: {person['who']} at {person['at']} would stand off the floor"  # 4.1.48
        else:
            assert by_index[cells[((wy - 1) * length + wz) * width + wx]] not in AIR, f"{key}: {person['who']} would stand on nothing"
        assert wy in layers, f"{key}: {person['who']} is cut off"
        wy_out = layers.index(wy) + lift
        who = {"id": Tag(STRING, person["who"]), "PersistenceRequired": Tag(BYTE, 1), "Rotation": Tag(LIST, (FLOAT, [person["yaw"], 0.0])),
               "Motion": Tag(LIST, (DOUBLE, [0.0, 0.0, 0.0])), "OnGround": Tag(BYTE, 1)}
        if "who_data" in person:
            # 4.1.24: who they are, in Forge's saved data (Entity.getPersistentData: "ForgeData").
            kinds = {"long": LONG, "int": INT, "byte": BYTE}
            who["ForgeData"] = Tag(COMPOUND, {k: Tag(kinds[kind], v) for k, (kind, v) in person["who_data"].items()})
        figures.append({"blockPos": Tag(LIST, (INT, [wx, wy_out, wz])), "pos": Tag(LIST, (DOUBLE, [wx + 0.5, float(wy_out), wz + 0.5])),
                        "nbt": Tag(COMPOUND, who)})
    size = (width, len(layers) + lift, length)
    path = OUT / (key + ".nbt")
    write_template(path, size, palette, blocks.getvalue(), count, figures)
    print(f"{path.relative_to(ROOT)}: {size[0]}x{size[1]}x{size[2]}, {len(palette)} states, {count} blocks, "
          + ", ".join(f"{_who(f)} at {tuple(f['blockPos'].value[1])}" for f in figures) + f", {path.stat().st_size // 1024} KiB")


def _who(figure):
    return figure["nbt"].value["id"].value


def name_bytes(name):
    data = name.encode("utf-8")
    return struct.pack(">H", len(data)) + data


STATE = bytes([INT]) + name_bytes("state")
POS = bytes([LIST]) + name_bytes("pos") + bytes([INT]) + struct.pack(">i", 3)
NBT = bytes([COMPOUND]) + name_bytes("nbt")


def write_block(f, state, pos, nbt):
    f.write(STATE + struct.pack(">i", state))
    f.write(POS + struct.pack(">iii", *pos))
    if nbt is not None:
        f.write(NBT)
        _write(f, COMPOUND, nbt)
    f.write(bytes([END]))


def write_template(path, size, palette, blocks, count, entities):
    f = io.BytesIO()
    f.write(bytes([COMPOUND]) + name_bytes(""))
    f.write(bytes([INT]) + name_bytes("DataVersion") + struct.pack(">i", DATA_VERSION))
    f.write(bytes([LIST]) + name_bytes("size") + bytes([INT]) + struct.pack(">i", 3) + struct.pack(">iii", *size))
    f.write(bytes([LIST]) + name_bytes("palette") + bytes([COMPOUND]) + struct.pack(">i", len(palette)))
    for name, props in palette:
        entry = {"Name": Tag(STRING, name)}
        if props:
            entry["Properties"] = Tag(COMPOUND, {k: Tag(STRING, v) for k, v in sorted(props.items())})
        _write(f, COMPOUND, entry)
    f.write(bytes([LIST]) + name_bytes("blocks") + bytes([COMPOUND]) + struct.pack(">i", count))
    f.write(blocks)
    f.write(bytes([LIST]) + name_bytes("entities") + bytes([COMPOUND if entities else END]) + struct.pack(">i", len(entities)))
    for e in entities:
        _write(f, COMPOUND, e)
    f.write(bytes([END]))
    path.parent.mkdir(parents=True, exist_ok=True)
    # mtime 0: the same schematic always makes the same bytes.
    path.write_bytes(gzip.compress(f.getvalue(), compresslevel=9, mtime=0))


def main():
    registry = None
    if "--blocks" in sys.argv:
        registry = Registry(sys.argv[sys.argv.index("--blocks") + 1])
    only = sys.argv[sys.argv.index("--only") + 1].split(",") if "--only" in sys.argv else None  # 4.1.48: some of them
    for key, place in PLACES.items():
        if only is None or key in only:
            build(key, place, registry)


if __name__ == "__main__":
    main()
