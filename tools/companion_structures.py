#!/usr/bin/env python3
"""1.3.24: Touka's and Ken's own places, from the owner's schematics, as 1.20.1 structure templates with each of them in theirs.

    python3 tools/companion_structures.py [--blocks blocks.json]

  art/anteiku.schem     -> src/main/resources/data/hexkagune/structures/anteiku.nbt      Touka behind the café's bar counter
  art/cyber_tower.schem -> src/main/resources/data/hexkagune/structures/cyber_tower.nbt  Ken on top of the tower, by its spire

Anteiku (saved by 1.15) is written whole, its air too (so nothing of the ground is left inside it); its walls' old sides (true/false)
become 1.20.1's (low/none) and every leaf is made persistent (its hedges have no logs: they would rot away).

The tower (saved by a much newer game) is 320 blocks tall: the whole height a 1.20.1 world has, so it can't stand on the ground as it
is. It is cut at its first full floor above the podium (schematic y 62: the twin towers' floors come every five blocks), and its spire's
last few decorative blocks (above 315) left off for margin, so it stands from the ground (y 62) to just under the sky's limit, Ken's roof
whole. The blocks 1.20.1 does not have (1.21's) are swapped for its nearest: the iron chain is 1.20.1's chain (only renamed), tuff's
slabs and stairs andesite's, polished tuff polished andesite; the trial spawner is left out (it would be a pig spawner). Its air is left
out (a template of seven million cells would not load): the structure's own terrain adaptation clears the ground in its box. A jigsaw
block at its middle (its start_jigsaw_name) centres it on the chunk it starts in: 151 blocks across, placed from a corner it would reach
past the eight chunks the game builds a structure's pieces in.

--blocks: PrismarineJS minecraft-data's 1.20 blocks.json, to check every block state written is one 1.20.1 has (it fails if not).
"""
import gzip
import io
import json
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from library_structure import BYTE, COMPOUND, DOUBLE, END, FLOAT, INT, LIST, STRING, Tag, _write, read, varints  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art"
OUT = ROOT / "src" / "main" / "resources" / "data" / "hexkagune" / "structures"
DATA_VERSION = 3465  # 1.20.1

PLACES = {
    "anteiku": {
        "source": "anteiku.schem",
        # Behind the bar counter on the café floor (the aisle between the counter and the coffee machines), facing the tables (south).
        "who": "hexkagune:touka", "at": (17, 10, 8), "yaw": 0.0,
        "keep_air": True, "crop": None, "anchor": None,
    },
    "cyber_tower": {
        "source": "cyber_tower.schem",
        # On the basalt terrace at the foot of the spire, the top of the tower (schematic coordinates), looking out over the roof (west).
        "who": "hexkagune:ken", "at": (74, 311, 87), "yaw": 90.0,
        "keep_air": False, "crop": (62, 315), "anchor": (75, 62, 75), "anchor_name": "hexkagune:tower_anchor",
    },
}

RENAMED = {
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


def build(key, place, registry):
    schem = read(ART / place["source"])
    if "Schematic" in schem:
        schem = schem["Schematic"].value
    width, height, length = schem["Width"].value, schem["Height"].value, schem["Length"].value
    by_index = {tag.value: state for state, tag in schem["Palette"].value.items()}
    cells = varints(schem["BlockData"].value)
    assert len(cells) == width * height * length, f"{key}: the block data does not fill the schematic"
    entities = {}
    if "BlockEntities" in schem:
        for raw in schem["BlockEntities"].value[1]:
            x, y, z = raw["Pos"].value
            entities[(x, y, z)] = block_entity(raw)
    y0, y1 = place["crop"] or (0, height - 1)
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
    for y in range(y0, y1 + 1):
        for z in range(length):
            row = (y * length + z) * width
            for x in range(width):
                c = converted[cells[row + x]]
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
                write_block(blocks, i, (x, y - y0, z), nbt)
                count += 1
    wx, wy, wz = place["at"]
    assert by_index[cells[(wy * length + wz) * width + wx]] in AIR and by_index[cells[((wy + 1) * length + wz) * width + wx]] in AIR, \
        f"{key}: {place['who']} would stand in a block"
    assert by_index[cells[((wy - 1) * length + wz) * width + wx]] not in AIR, f"{key}: {place['who']} would stand on nothing"
    assert y0 <= wy <= y1, f"{key}: {place['who']} is cut off"
    who = {"id": Tag(STRING, place["who"]), "PersistenceRequired": Tag(BYTE, 1), "Rotation": Tag(LIST, (FLOAT, [place["yaw"], 0.0])),
           "Motion": Tag(LIST, (DOUBLE, [0.0, 0.0, 0.0])), "OnGround": Tag(BYTE, 1)}
    figure = {"blockPos": Tag(LIST, (INT, [wx, wy - y0, wz])), "pos": Tag(LIST, (DOUBLE, [wx + 0.5, float(wy - y0), wz + 0.5])),
              "nbt": Tag(COMPOUND, who)}
    size = (width, y1 - y0 + 1, length)
    path = OUT / (key + ".nbt")
    write_template(path, size, palette, blocks.getvalue(), count, [figure])
    print(f"{path.relative_to(ROOT)}: {size[0]}x{size[1]}x{size[2]}, {len(palette)} states, {count} blocks, "
          f"{place['who']} at {(wx, wy - y0, wz)}, {path.stat().st_size // 1024} KiB")


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
    for key, place in PLACES.items():
        build(key, place, registry)


if __name__ == "__main__":
    main()
