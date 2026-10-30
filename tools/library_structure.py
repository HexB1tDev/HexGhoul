#!/usr/bin/env python3
"""1.3.0: the Library, from the user's schematic (art/library.schem, a Sponge schematic saved by a newer game) to a 1.20.1
structure template (src/main/resources/data/hexkagune/structures/library.nbt), with Eto standing in it.

The blocks 1.20.1 does not have are swapped for the nearest it does (pale oak for stripped birch, the shelves for chiseled
bookshelves, the copper trapdoor for a warped one, the wool slabs and stairs of the awning for quartz and mangrove); the block
entities are written as 1.20.1 reads them (the shelves' books, the signs' text, the lectern's book, the pot's sherds).

    python3 tools/library_structure.py
"""
import gzip
import io
import random
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art" / "library.schem"
TARGET = ROOT / "src" / "main" / "resources" / "data" / "hexkagune" / "structures" / "library.nbt"
DATA_VERSION = 3465  # 1.20.1
# Where Eto stands (the ground floor, clear of the desk), template coordinates.
ETO = (6, 3, 5)
# A lone grass block left floating off the roof's corner when the schematic was cut: not part of the building.
STRAY = {(0, 15, 8)}

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTES, STRING, LIST, COMPOUND, INTS, LONGS = range(13)


class Tag:
    def __init__(self, kind, value):
        self.kind, self.value = kind, value


def _payload(f, kind):
    if kind == BYTE:
        return struct.unpack(">b", f.read(1))[0]
    if kind == SHORT:
        return struct.unpack(">h", f.read(2))[0]
    if kind == INT:
        return struct.unpack(">i", f.read(4))[0]
    if kind == LONG:
        return struct.unpack(">q", f.read(8))[0]
    if kind == FLOAT:
        return struct.unpack(">f", f.read(4))[0]
    if kind == DOUBLE:
        return struct.unpack(">d", f.read(8))[0]
    if kind == BYTES:
        return bytearray(f.read(struct.unpack(">i", f.read(4))[0]))
    if kind == STRING:
        return f.read(struct.unpack(">H", f.read(2))[0]).decode("utf-8")
    if kind == LIST:
        inner = f.read(1)[0]
        return inner, [_payload(f, inner) for _ in range(struct.unpack(">i", f.read(4))[0])]
    if kind == COMPOUND:
        out = {}
        while True:
            inner = f.read(1)[0]
            if inner == END:
                return out
            name = f.read(struct.unpack(">H", f.read(2))[0]).decode("utf-8")
            out[name] = Tag(inner, _payload(f, inner))
    if kind == INTS:
        n = struct.unpack(">i", f.read(4))[0]
        return list(struct.unpack(">%di" % n, f.read(4 * n)))
    if kind == LONGS:
        n = struct.unpack(">i", f.read(4))[0]
        return list(struct.unpack(">%dq" % n, f.read(8 * n)))
    raise ValueError(kind)


def read(path):
    f = io.BytesIO(gzip.decompress(path.read_bytes()))
    f.read(1)
    f.read(struct.unpack(">H", f.read(2))[0])
    return _payload(f, COMPOUND)


def _write(f, kind, value):
    if kind == BYTE:
        f.write(struct.pack(">b", value))
    elif kind == SHORT:
        f.write(struct.pack(">h", value))
    elif kind == INT:
        f.write(struct.pack(">i", value))
    elif kind == LONG:
        f.write(struct.pack(">q", value))
    elif kind == FLOAT:
        f.write(struct.pack(">f", value))
    elif kind == DOUBLE:
        f.write(struct.pack(">d", value))
    elif kind == STRING:
        data = value.encode("utf-8")
        f.write(struct.pack(">H", len(data)))
        f.write(data)
    elif kind == LIST:
        inner, items = value
        f.write(bytes([inner if items else END]))
        f.write(struct.pack(">i", len(items)))
        for item in items:
            _write(f, inner, item)
    elif kind == COMPOUND:
        for name, tag in value.items():
            f.write(bytes([tag.kind]))
            data = name.encode("utf-8")
            f.write(struct.pack(">H", len(data)))
            f.write(data)
            _write(f, tag.kind, tag.value)
        f.write(bytes([END]))
    else:
        raise ValueError(kind)


def write(path, root):
    f = io.BytesIO()
    f.write(bytes([COMPOUND]))
    f.write(struct.pack(">H", 0))
    _write(f, COMPOUND, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    # mtime 0: the same schematic always makes the same bytes.
    path.write_bytes(gzip.compress(f.getvalue(), mtime=0))


def s(text):
    return Tag(STRING, text)


def compound(**fields):
    return Tag(COMPOUND, fields)


def ints(*values):
    return Tag(LIST, (INT, list(values)))


def doubles(*values):
    return Tag(LIST, (DOUBLE, list(values)))


def floats(*values):
    return Tag(LIST, (FLOAT, list(values)))


def strings(*values):
    return Tag(LIST, (STRING, list(values)))


# ---------------------------------------------------------------- blocks

def parse(state):
    """'minecraft:x[a=b,c=d]' -> ('minecraft:x', {'a': 'b', 'c': 'd'})"""
    if "[" not in state:
        return state, {}
    name, rest = state.split("[", 1)
    props = dict(part.split("=", 1) for part in rest.rstrip("]").split(",") if part)
    return name, props


RENAMED = {
    "minecraft:stripped_pale_oak_log": "minecraft:stripped_birch_log",
    "minecraft:stripped_pale_oak_wood": "minecraft:stripped_birch_wood",
    "minecraft:pale_oak_planks": "minecraft:birch_planks",
    "minecraft:oxidized_copper_trapdoor": "minecraft:warped_trapdoor",
    "minecraft:white_wool_slab": "minecraft:smooth_quartz_slab",
    "minecraft:white_wool_stairs": "minecraft:smooth_quartz_stairs",
    "minecraft:red_wool_slab": "minecraft:mangrove_slab",
    "minecraft:red_wool_stairs": "minecraft:mangrove_stairs",
}
# Properties 1.20.1 does not know on blocks it does.
DROPPED = {"minecraft:decorated_pot": {"cracked"}}


def convert(state, books):
    """A block of the newer game as 1.20.1 has it: its name, its properties, and its block entity's data (or None)."""
    name, props = parse(state)
    if name == "minecraft:oak_shelf":
        # A shelf of books: a chiseled bookshelf facing the same way, some of its six slots filled.
        slots = books()
        props = {"facing": props.get("facing", "north")}
        for i in range(6):
            props[f"slot_{i}_occupied"] = "true" if i in slots else "false"
        return "minecraft:chiseled_bookshelf", props, shelf(slots)
    name = RENAMED.get(name, name)
    for prop in DROPPED.get(name, ()):
        props.pop(prop, None)
    if name == "minecraft:chiseled_bookshelf":
        slots = [i for i in range(6) if props.get(f"slot_{i}_occupied") == "true"]
        return name, props, shelf(slots)
    if name in ("minecraft:warped_hanging_sign", "minecraft:warped_wall_hanging_sign"):
        lines = ("", "LIBRARY", "quiet, please", "") if name.endswith("wall_hanging_sign") else ("", "", "", "")
        return name, props, sign(lines)
    if name == "minecraft:lectern":
        return name, props, lectern() if props.get("has_book") == "true" else compound(id=s("minecraft:lectern"))
    if name == "minecraft:decorated_pot":
        return name, props, compound(id=s("minecraft:decorated_pot"), sherds=strings(*["minecraft:brick"] * 4))
    if name == "minecraft:enchanting_table":
        return name, props, compound(id=s("minecraft:enchanting_table"))
    return name, props, None


def shelf(slots):
    items = [{"Slot": Tag(BYTE, i), "id": s("minecraft:book"), "Count": Tag(BYTE, 1)} for i in slots]
    return compound(id=s("minecraft:chiseled_bookshelf"), Items=Tag(LIST, (COMPOUND, items)), last_interacted_slot=Tag(INT, -1))


def text(line):
    return '{"text":"' + line.replace('"', '\\"') + '"}'


def sign(lines):
    def side(rows):
        return compound(messages=strings(*[text(row) for row in rows]), color=s("black"), has_glowing_text=Tag(BYTE, 0))
    return compound(id=s("minecraft:hanging_sign"), front_text=side(lines), back_text=side(("", "", "", "")), is_waxed=Tag(BYTE, 0))


def lectern():
    pages = [
        "Library rules:\n\n1. Quiet.\n2. No eating in the stacks.\n3. Whatever is eating in the stacks is not your business.",
        "If you are reading this at night, my eye is not a trick of the light.\n\nSay hi anyway. I get lonely.",
    ]
    book = {"id": s("minecraft:written_book"), "Count": Tag(BYTE, 1), "tag": compound(
        title=s("Library Rules"), author=s("the librarian"), resolved=Tag(BYTE, 1), pages=strings(*[text(page) for page in pages]))}
    return compound(id=s("minecraft:lectern"), Book=Tag(COMPOUND, book), Page=Tag(INT, 0))


# ---------------------------------------------------------------- the template

def varints(data):
    out, i = [], 0
    while i < len(data):
        value = shift = 0
        while True:
            byte = data[i]
            i += 1
            value |= (byte & 0x7F) << shift
            shift += 7
            if not byte & 0x80:
                break
        out.append(value)
    return out


def main():
    schem = read(SOURCE)
    if "Schematic" in schem:
        schem = schem["Schematic"].value
    width, height, length = schem["Width"].value, schem["Height"].value, schem["Length"].value
    by_index = {tag.value: state for state, tag in schem["Palette"].value.items()}
    cells = varints(schem["BlockData"].value)
    assert len(cells) == width * height * length, "the block data does not fill the schematic"
    rng = random.Random(1300)

    def books():
        return sorted(rng.sample(range(6), rng.randint(2, 6)))

    palette, index, blocks = [], {}, []
    for y in range(height):
        for z in range(length):
            for x in range(width):
                state = "minecraft:air" if (x, y, z) in STRAY else by_index[cells[(y * length + z) * width + x]]
                name, props, entity = convert(state, books)
                key = (name, tuple(sorted(props.items())))
                if key not in index:
                    index[key] = len(palette)
                    entry = {"Name": s(name)}
                    if props:
                        entry["Properties"] = compound(**{k: s(v) for k, v in sorted(props.items())})
                    palette.append(entry)
                block = {"state": Tag(INT, index[key]), "pos": ints(x, y, z)}
                if entity is not None:
                    block["nbt"] = entity
                blocks.append(block)
    ex, ey, ez = ETO
    assert by_index[cells[(ey * length + ez) * width + ex]] == "minecraft:air", "Eto would stand in a block"
    eto = compound(id=s("hexkagune:eto"), PersistenceRequired=Tag(BYTE, 1), Rotation=floats(90.0, 0.0), Motion=doubles(0.0, 0.0, 0.0),
                   OnGround=Tag(BYTE, 1))
    entities = [{"blockPos": ints(ex, ey, ez), "pos": doubles(ex + 0.5, float(ey), ez + 0.5), "nbt": eto}]
    root = {
        "DataVersion": Tag(INT, DATA_VERSION),
        "size": ints(width, height, length),
        "palette": Tag(LIST, (COMPOUND, palette)),
        "blocks": Tag(LIST, (COMPOUND, blocks)),
        "entities": Tag(LIST, (COMPOUND, entities)),
    }
    write(TARGET, root)
    print(f"{TARGET.relative_to(ROOT)}: {width}x{height}x{length}, {len(palette)} states, {len(blocks)} blocks, Eto at {ETO}")


if __name__ == "__main__":
    main()
