#!/usr/bin/env python3
"""4.1.51 (the owner: "New CCG schematic, map it out with sitting areas, offices and etc."): the CCG's new tower, from their file
(art/exploration/New_CCG.schematic, MCEdit "Alpha": numeric ids, 43 x 58 x 37), furnished floor by floor, written to
build/ccg/ccg_tower.schem (Sponge v3, with its signs and the owner's paintings) for the place pipeline (tools/exploration_structures.py,
site ccg_building). Their file is only read.

    python3 tools/ccg_tower.py [--render out_dir]

Their tower: a two-wing lobby (y 1-6, its floor y 0) behind a sandstone colonnade, its doors east and south; nine floors over it (their
floors y 7, 12, 17, 23, 28, 33, 39, 44, 49; the roof y 54-56), each an L of open floor round a ladder shaft (x 17-20, z 14-17, its way in
on the south), a band of black glass round it; on the 4th and 9th floors their own rooms (light grey terracotta, grey glass, doors).
Their signs in the shaft name each floor. What is added: furniture, lights (glowstone just under each ceiling, as their own 4th floor
has it), a few partitions in their style (light grey terracotta and grey glass) where a floor is mapped into rooms. Nothing of theirs is
taken away but air.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import library_structure as L  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art" / "exploration" / "New_CCG.schematic"
OUT = ROOT / "build" / "ccg"

STAIR_FACING = {0: "east", 1: "west", 2: "south", 3: "north"}
WALL_FACING = {2: "north", 3: "south", 4: "west", 5: "east"}
DOOR_FACING = {0: "east", 1: "south", 2: "west", 3: "north"}
RAIL = {6: "south_east", 7: "south_west", 8: "north_west", 9: "north_east"}
#: The paintings of their era by the names 1.20.1 gives them.
MOTIVES = {"Match": "match", "Bust": "bust", "Wither": "wither", "Wanderer": "wanderer", "SkullAndRoses": "skull_and_roses",
           "Stage": "stage", "Graham": "graham", "Void": "void", "Aztec": "aztec", "Kebab": "kebab"}


def alpha_state(b, d, below=None):
    """Their (id, data) as a 1.20.1 state: (name, props); None for air. {@code below}: the (id, data) under it (a door's top half)."""
    if b == 0:
        return None
    if b == 1:
        return "minecraft:stone", {}
    if b == 2:
        return "minecraft:grass_block", {"snowy": "false"}
    if b == 3:
        return "minecraft:dirt", {}
    if b == 5 and d == 1:
        return "minecraft:spruce_planks", {}
    if b == 17:
        wood = {0: "oak", 1: "spruce", 2: "birch", 3: "jungle"}[d & 3]
        return f"minecraft:{wood}_log", {"axis": {0: "y", 4: "x", 8: "z", 12: "y"}[d & 12]}
    if b == 24:
        return {0: ("minecraft:sandstone", {}), 1: ("minecraft:chiseled_sandstone", {}), 2: ("minecraft:cut_sandstone", {})}[d]
    if b == 43 and d == 0:
        return "minecraft:smooth_stone_slab", {"type": "double", "waterlogged": "false"}
    if b == 44:
        kind = {0: "smooth_stone", 1: "sandstone"}[d & 7]
        return f"minecraft:{kind}_slab", {"type": "top" if d & 8 else "bottom", "waterlogged": "false"}
    if b in (53, 128):
        kind = {53: "oak", 128: "sandstone"}[b]
        return f"minecraft:{kind}_stairs", {"facing": STAIR_FACING[d & 3], "half": "top" if d & 4 else "bottom", "shape": "straight",
                                            "waterlogged": "false"}
    if b == 64:
        if d & 8:
            lb, ld = below
            assert lb == 64 and not ld & 8, (b, d, below)
            return "minecraft:oak_door", {"half": "upper", "facing": DOOR_FACING[ld & 3], "open": "true" if ld & 4 else "false",
                                          "hinge": "right" if d & 1 else "left", "powered": "false"}
        return "minecraft:oak_door", {"half": "lower", "facing": DOOR_FACING[d & 3], "open": "true" if d & 4 else "false", "hinge": "left",
                                      "powered": "false"}
    if b == 65:
        return "minecraft:ladder", {"facing": WALL_FACING[d], "waterlogged": "false"}
    if b == 66:
        return "minecraft:rail", {"shape": RAIL[d], "waterlogged": "false"}
    if b == 68:
        return "minecraft:oak_wall_sign", {"facing": WALL_FACING[d], "waterlogged": "false"}
    if b == 89:
        return "minecraft:glowstone", {}
    if b == 95:
        return f"minecraft:{DYE[d]}_stained_glass", {}
    if b == 98 and d == 0:
        return "minecraft:stone_bricks", {}
    if b == 118:
        return "minecraft:cauldron", {}
    if b == 125:
        return f"minecraft:{WOOD[d]}_slab", {"type": "double", "waterlogged": "false"}
    if b == 126:
        return f"minecraft:{WOOD[d & 7]}_slab", {"type": "top" if d & 8 else "bottom", "waterlogged": "false"}
    if b == 140:
        return "minecraft:flower_pot", {}
    if b == 159:
        return f"minecraft:{DYE[d]}_terracotta", {}
    if b == 160:
        return f"minecraft:{DYE[d]}_stained_glass_pane", {"north": "false", "south": "false", "east": "false", "west": "false",
                                                          "waterlogged": "false"}
    if b == 161:
        return f"minecraft:{ {0: 'acacia', 1: 'dark_oak'}[d & 1]}_leaves", {"persistent": "true", "distance": "7", "waterlogged": "false"}
    if b == 171:
        return f"minecraft:{DYE[d]}_carpet", {}
    raise SystemExit(f"{SOURCE.name}: no 1.20.1 state for block {b}:{d}")


DYE = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown",
       "green", "red", "black"]
WOOD = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
#: Their flower pots' plants (the old pot's Item id) as 1.20.1's potted blocks.
POTTED = {37: "minecraft:potted_dandelion", 38: "minecraft:potted_poppy", 6: "minecraft:potted_oak_sapling", 31: "minecraft:potted_fern"}


def val(t):
    return t.value if isinstance(t, L.Tag) else t


def read_tower():
    """Their tower: {(x, y, z): (name, props)}, its size, its signs' texts {(x, y, z): [4 lines]}, its paintings [(variant, facing, x, y, z)]."""
    root = {k: val(v) for k, v in val(L.read(SOURCE)).items()}
    w, h, d = root["Width"], root["Height"], root["Length"]
    ids, data = root["Blocks"], root["Data"]
    cells = {}
    for y in range(h):
        for z in range(d):
            for x in range(w):
                i = (y * d + z) * w + x
                b, dd = ids[i] & 0xFF, data[i] & 0x0F
                below = None
                if y > 0:
                    j = ((y - 1) * d + z) * w + x
                    below = (ids[j] & 0xFF, data[j] & 0x0F)
                s = alpha_state(b, dd, below)
                if s is not None:
                    cells[(x, y, z)] = s
    signs = {}
    for t in val(root["TileEntities"])[1]:
        t = {k: val(v) for k, v in val(t).items()}
        p = (t["x"], t["y"], t["z"])
        if t["id"] == "Sign":
            signs[p] = [t.get(f"Text{k}", "") for k in range(1, 5)]
        elif t["id"] == "FlowerPot" and t.get("Item") in POTTED:
            cells[p] = (POTTED[t["Item"]], {})
    paintings = []
    for e in val(root["Entities"])[1]:
        e = {k: val(v) for k, v in val(e).items()}
        if e["id"] == "Painting":
            # Their era's Direction: 0 south, 1 west, 2 north, 3 east (the side the picture faces), as 1.20.1's "facing".
            paintings.append((MOTIVES[e["Motive"]], e["Direction"], e["TileX"], e["TileY"], e["TileZ"]))
    return cells, (w, h, d), signs, paintings


if __name__ == "__main__":
    c, size, s, p = read_tower()
    print(size, len(c), "blocks", len(s), "signs", len(p), "paintings")
