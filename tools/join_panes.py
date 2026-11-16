#!/usr/bin/env python3
"""4.1.57 (the owner: "Be careful with the glass placement on the structures, the CCG building has all glass pannels unconnected and not
attached to eachother!"): every glass pane and iron bar in the mod's templates joined to what it touches, as the game itself joins them
(IronBarsBlock: to another pane or bar, to a wall, to any block with a full face that way; never to leaves, a barrier, a pumpkin, a melon
or a shulker box). A template is placed without updating its blocks' shapes, so a pane written with no sides stays a lone post: the CCG
tower's converter wrote every one of its 3134 so. Tiled places (a directory of t_I_J.nbt) are read as one, so a pane at a tile's edge
joins the next tile's. Everything else in each file is written back exactly as it was.

    python3 tools/join_panes.py            # every template under src/main/resources/data/hexkagune/structures
    python3 tools/join_panes.py --check    # only says how many would change (exit 1 if any)
"""
import gzip
import io
import re
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STRUCTURES = ROOT / "src/main/resources/data/hexkagune/structures"

# ---------------------------------------------------------------- typed NBT, read and written back exactly


def _read(buf, kind):
    if kind == 1:
        return struct.unpack(">b", buf.read(1))[0]
    if kind == 2:
        return struct.unpack(">h", buf.read(2))[0]
    if kind == 3:
        return struct.unpack(">i", buf.read(4))[0]
    if kind == 4:
        return struct.unpack(">q", buf.read(8))[0]
    if kind == 5:
        return struct.unpack(">f", buf.read(4))[0]
    if kind == 6:
        return struct.unpack(">d", buf.read(8))[0]
    if kind == 7:
        n = struct.unpack(">i", buf.read(4))[0]
        return buf.read(n)
    if kind == 8:
        n = struct.unpack(">H", buf.read(2))[0]
        return buf.read(n).decode("utf-8")
    if kind == 9:
        inner = buf.read(1)[0]
        n = struct.unpack(">i", buf.read(4))[0]
        return (inner, [_read(buf, inner) for _ in range(n)])
    if kind == 10:
        out = {}
        while True:
            t = buf.read(1)[0]
            if t == 0:
                return out
            n = struct.unpack(">H", buf.read(2))[0]
            name = buf.read(n).decode("utf-8")
            out[name] = (t, _read(buf, t))
    if kind == 11:
        n = struct.unpack(">i", buf.read(4))[0]
        return list(struct.unpack(f">{n}i", buf.read(4 * n)))
    if kind == 12:
        n = struct.unpack(">i", buf.read(4))[0]
        return list(struct.unpack(f">{n}q", buf.read(8 * n)))
    raise ValueError(f"tag {kind}")


def _write(out, kind, value):
    if kind == 1:
        out.write(struct.pack(">b", value))
    elif kind == 2:
        out.write(struct.pack(">h", value))
    elif kind == 3:
        out.write(struct.pack(">i", value))
    elif kind == 4:
        out.write(struct.pack(">q", value))
    elif kind == 5:
        out.write(struct.pack(">f", value))
    elif kind == 6:
        out.write(struct.pack(">d", value))
    elif kind == 7:
        out.write(struct.pack(">i", len(value)) + value)
    elif kind == 8:
        data = value.encode("utf-8")
        out.write(struct.pack(">H", len(data)) + data)
    elif kind == 9:
        inner, items = value
        out.write(bytes([inner if items or inner else 0]) + struct.pack(">i", len(items)))
        for item in items:
            _write(out, inner, item)
    elif kind == 10:
        for name, (t, v) in value.items():
            data = name.encode("utf-8")
            out.write(bytes([t]) + struct.pack(">H", len(data)) + data)
            _write(out, t, v)
        out.write(b"\x00")
    elif kind == 11:
        out.write(struct.pack(">i", len(value)) + struct.pack(f">{len(value)}i", *value))
    elif kind == 12:
        out.write(struct.pack(">i", len(value)) + struct.pack(f">{len(value)}q", *value))
    else:
        raise ValueError(f"tag {kind}")


def load(path):
    raw = gzip.decompress(path.read_bytes())
    buf = io.BytesIO(raw)
    kind = buf.read(1)[0]
    n = struct.unpack(">H", buf.read(2))[0]
    name = buf.read(n).decode("utf-8")
    return name, _read(buf, kind), raw


def save(path, name, root):
    out = io.BytesIO()
    data = name.encode("utf-8")
    out.write(bytes([10]) + struct.pack(">H", len(data)) + data)
    _write(out, 10, root)
    path.write_bytes(gzip.compress(out.getvalue(), compresslevel=9, mtime=0))


# ---------------------------------------------------------------- the game's rule


def connecting(name):
    """A pane or a bar (vanilla's IronBarsBlock and its kinds)."""
    return name.endswith("_pane") or name.endswith("iron_bars") or name.endswith("copper_bars")


EXCEPTIONS = ("barrier", "pumpkin", "carved_pumpkin", "jack_o_lantern", "melon", "shulker_box")
EXCEPTION_ENDS = ("_leaves", "_shulker_box")
#: Blocks whose faces are not whole, by exact name (a substring would take light_blue_stained_glass for light, sea_lantern for lantern).
NOT_FULL = {"air", "cave_air", "void_air", "water", "lava", "light", "cobweb", "snow", "lever", "bell", "cake", "conduit", "scaffolding",
            "string", "tripwire", "dragon_egg", "lectern", "hopper", "cauldron", "water_cauldron", "lava_cauldron", "powder_snow_cauldron",
            "anvil", "chipped_anvil", "damaged_anvil", "bamboo", "bamboo_sapling", "cactus", "sugar_cane", "kelp", "kelp_plant", "seagrass",
            "tall_seagrass", "sea_pickle", "farmland", "dirt_path", "soul_sand", "mud", "honey_block", "grindstone", "stonecutter",
            "brewing_stand", "enchanting_table", "campfire", "soul_campfire", "chain", "lantern", "soul_lantern", "end_rod", "lightning_rod",
            "flower_pot", "decorated_pot", "daylight_detector", "repeater", "comparator", "redstone_wire", "ladder", "vine", "glow_lichen",
            "sculk_vein", "pointed_dripstone", "dead_bush", "grass", "short_grass", "tall_grass", "fern", "large_fern", "sweet_berry_bush",
            "wheat", "carrots", "potatoes", "beetroots", "nether_wart", "structure_void", "jigsaw", "moss_carpet", "pink_petals",
            "big_dripleaf", "big_dripleaf_stem", "small_dripleaf", "azalea", "flowering_azalea", "spore_blossom", "hanging_roots",
            "mangrove_propagule", "frogspawn", "turtle_egg", "sniffer_egg", "chest", "trapped_chest", "ender_chest", "candle", "poppy",
            "dandelion", "allium", "azure_bluet", "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose", "sunflower", "lilac",
            "rose_bush", "peony", "torchflower", "pitcher_plant", "torch", "soul_torch", "redstone_torch", "rail", "end_portal_frame",
            "piston_head", "moving_piston", "composter", "cocoa", "lily_pad", "nether_sprouts", "warped_roots", "crimson_roots",
            "red_mushroom", "brown_mushroom", "crimson_fungus", "warped_fungus", "weeping_vines", "twisting_vines", "cave_vines",
            "item_frame", "glow_item_frame", "slime_block_never"}
#: ... and by the end of their names.
NOT_FULL_ENDS = ("_stairs", "_door", "_trapdoor", "_sign", "_button", "_pressure_plate", "_carpet", "_torch", "_banner", "_bed", "_rail",
                 "_fence", "_fence_gate", "_candle", "_skull", "_head", "_sapling", "_tulip", "_orchid", "_coral", "_coral_fan",
                 "_cluster", "_bud", "_cake", "_plant", "_vines", "_pane", "_bars", "_rod", "_lantern_never")


def full(name):
    """True for a block whose faces are whole (a full cube): every block but those named above (a slab only double: the caller's)."""
    short = name.split(":", 1)[-1]
    if short in NOT_FULL or short.startswith("potted_"):
        return False
    return not short.endswith(NOT_FULL_ENDS)


#: Blocks whose face that way is whole or not by how they are turned (a stair's back, a trapdoor shut against it): left as the template has them.
TURNED_ENDS = ("_stairs", "_trapdoor")


def attaches(name, props):
    """The game's IronBarsBlock.attachsTo for a neighbour: a pane or bar, a wall, or a full face that is no exception; None where it
    depends on how the neighbour is turned (the template's own side kept)."""
    if name is None:
        return False
    if name.split(":", 1)[-1].endswith(TURNED_ENDS):
        return None
    short = name.split(":", 1)[-1]
    if connecting(name):
        return True
    if short.endswith("_wall") and "sign" not in short and "torch" not in short and "banner" not in short and "fan" not in short:
        return True
    if short in EXCEPTIONS or short.endswith(EXCEPTION_ENDS):
        return False
    if short.endswith("_slab"):
        return props.get("type") == "double"
    return full(name)


SIDES = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}

# ---------------------------------------------------------------- a structure read as one


def tiles_of(path):
    """[(file, x offset, z offset)] for a single template or a directory of t_I_J tiles laid edge to edge."""
    if path.is_file():
        return [(path, 0, 0)]
    tiles = {}
    for f in path.glob("t_*_*.nbt"):
        m = re.match(r"t_(\d+)_(\d+)\.nbt", f.name)
        tiles[(int(m.group(1)), int(m.group(2)))] = f
    sizes = {}
    for key, f in tiles.items():
        _, root, _ = load(f)
        sizes[key] = root["size"][1][1]
    xs = {}
    zs = {}
    for (i, j), size in sizes.items():
        xs[i] = size[0]
        zs[j] = size[2]
    out = []
    for (i, j), f in sorted(tiles.items()):
        out.append((f, sum(xs[k] for k in range(i)), sum(zs[k] for k in range(j))))
    return out


def join(path, check):
    parts = []
    world = {}
    for f, ox, oz in tiles_of(path):
        name, root, raw = load(f)
        palette = root["palette"][1][1]
        states = []
        for entry in palette:
            props = {k: v[1] for k, v in entry.get("Properties", (10, {}))[1].items()}
            states.append((entry["Name"][1], props))
        for block in root["blocks"][1][1]:
            pos = block["pos"][1][1]
            world[(pos[0] + ox, pos[1], pos[2] + oz)] = states[block["state"][1]]
        parts.append((f, ox, oz, name, root, raw, states))
    changed = 0
    total = 0
    for f, ox, oz, name, root, raw, states in parts:
        palette = root["palette"][1][1]
        index = {}
        for i, (n, p) in enumerate(states):
            index[(n, tuple(sorted(p.items())))] = i
        moved = 0
        for block in root["blocks"][1][1]:
            n, props = states[block["state"][1]]
            if not connecting(n):
                continue
            total += 1
            pos = block["pos"][1][1]
            want = dict(props)
            for side, (dx, dy, dz) in SIDES.items():
                there = world.get((pos[0] + ox + dx, pos[1] + dy, pos[2] + oz + dz))
                joined = attaches(there[0], there[1]) if there is not None else False
                if joined is not None:
                    want[side] = "true" if joined else "false"
            if want == props:
                continue
            key = (n, tuple(sorted(want.items())))
            if key not in index:
                index[key] = len(states)
                states.append((n, want))
                palette.append({"Name": (8, n), "Properties": (10, {k: (8, v) for k, v in sorted(want.items())})})
            block["state"] = (3, index[key])
            moved += 1
        changed += moved
        if moved and not check:
            save(f, name, root)
    return changed, total


def main():
    check = "--check" in sys.argv
    targets = sorted([p for p in STRUCTURES.iterdir() if p.suffix == ".nbt" or p.is_dir()])
    all_changed = 0
    for target in targets:
        if target.name == "clearing.nbt":
            continue
        changed, total = join(target, check)
        if total:
            print(f"{target.relative_to(ROOT)}: {total} panes and bars, {changed} {'to join' if check else 'joined'}")
        all_changed += changed
    if check and all_changed:
        sys.exit(1)


if __name__ == "__main__":
    main()
