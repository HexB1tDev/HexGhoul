#!/usr/bin/env python3
"""4.1.51 (the owner: "New CCG schematic, map it out with sitting areas, offices and etc."; then, of their first tower: "I'll get you a new
one fully furnished"): the CCG's new building, their furnished tower (art/exploration/CCG.schematic, MCEdit/WorldEdit "Alpha": numeric
ids, 31 x 65 x 46), as 1.20.1 has it, written to build/ccg/ccg_tower.schem (Sponge v3, with its signs, banners, pots and furnaces, and
its paintings and item frames as entities) for the place pipeline (tools/exploration_structures.py, site ccg_building). Their file is only
read; their first tower (New_CCG.schematic, unfurnished) is not used.

    python3 tools/ccg_tower.py

Their tower: three layers of dirt under it; its ground y 3 (a pavement round it, a glowstone floor under the lobby's carpet); the lobby
(y 4-7: a restaurant, a waiting room, a kitchen), then nine office floors one every 6 blocks (feet y 10, 16 ... 58: eight offices, two
bathrooms, a kitchen, a meeting room each), a quartz roof. Its door is the gap in the north front (x 15). Between floors only a lift: a
3 x 3 cabin over the shaft (x 14-16, z 14-16) on every floor, a stone button inside, a sign over it ("Current Floor: 1 / Dest: 2"); its
command blocks were never in the file, so the game's lift (server/CcgLift) does what those signs say.

The owner's answers (4.1.51): its signs say CCG things in the game ("Rename to CCG"): the business park's names become the CCG's (SIGNS);
their stray private-use characters (U+F700, U+F701) are dropped from every line. Its four villagers (the lobby restaurant's) become the
mod's humans ("Restaurant staff humans"): their spots are kept (STAFF), and exploration_structures places hexkagune:human there.
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import library_structure as L  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art" / "exploration" / "CCG.schematic"
OUT = ROOT / "build" / "ccg"
PATH = OUT / "ccg_tower.schem"

DYE = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown",
       "green", "red", "black"]
WOOD = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
STAIRS = {53: "oak", 67: "cobblestone", 108: "brick", 109: "stone_brick", 114: "nether_brick", 128: "sandstone", 134: "spruce",
          135: "birch", 136: "jungle", 156: "quartz", 163: "acacia", 164: "dark_oak", 180: "red_sandstone", 203: "purpur"}
DOORS = {64: "oak", 193: "spruce", 194: "birch", 195: "jungle", 196: "acacia", 197: "dark_oak", 71: "iron"}
STAIR_FACING = {0: "east", 1: "west", 2: "south", 3: "north"}
WALL_FACING = {2: "north", 3: "south", 4: "west", 5: "east"}
DOOR_FACING = {0: "east", 1: "south", 2: "west", 3: "north"}
TRAPDOOR_FACING = {0: "north", 1: "south", 2: "west", 3: "east"}
HOOK_FACING = {0: "south", 1: "west", 2: "north", 3: "east"}
PISTON_FACING = {0: "down", 1: "up", 2: "north", 3: "south", 4: "west", 5: "east"}
#: An old button's data: where it is stuck (face) and which way it looks.
BUTTON = {0: ("ceiling", "north"), 1: ("wall", "east"), 2: ("wall", "west"), 3: ("wall", "south"), 4: ("wall", "north"), 5: ("floor", "north")}
SLABS = {0: "smooth_stone", 1: "sandstone", 2: "petrified_oak", 3: "cobblestone", 4: "brick", 5: "stone_brick", 6: "nether_brick",
         7: "quartz"}
FALSE = "false"


def stairs(kind, d):
    return f"minecraft:{kind}_stairs", {"facing": STAIR_FACING[d & 3], "half": "top" if d & 4 else "bottom", "shape": "straight",
                                        "waterlogged": FALSE}


def state(b, d, below):
    """Their (id, data) as a 1.20.1 state (name, props), or None (air). {@code below}: the (id, data) under it (a door's top half)."""
    if b == 0:
        return None
    plain = {1: "stone", 2: "grass_block", 3: "dirt", 20: "glass", 24: "sandstone", 42: "iron_block", 47: "bookshelf", 58: "crafting_table",
             82: "clay", 89: "glowstone", 152: "redstone_block"}
    if b in plain and d == 0:
        n = "minecraft:" + plain[b]
        return (n, {"snowy": FALSE}) if b == 2 else (n, {})
    if b == 5:
        return f"minecraft:{WOOD[d]}_planks", {}
    if b == 17:
        return f"minecraft:{WOOD[d & 3]}_log", {"axis": {0: "y", 4: "x", 8: "z", 12: "y"}[d & 12]}
    if b in (18, 161):
        kind = WOOD[d & 3] if b == 18 else WOOD[4 + (d & 1)]
        # Their leaves never decay (bit 4) or are the game's own check (bit 8): kept for good either way (no logs hold them).
        return f"minecraft:{kind}_leaves", {"persistent": "true", "distance": "7", "waterlogged": FALSE}
    if b == 25:
        return "minecraft:note_block", {"instrument": "harp", "note": "0", "powered": FALSE}
    if b == 29 or b == 33:
        return "minecraft:sticky_piston" if b == 29 else "minecraft:piston", {"extended": "true" if d & 8 else FALSE,
                                                                              "facing": PISTON_FACING[d & 7]}
    if b == 34:
        return "minecraft:piston_head", {"facing": PISTON_FACING[d & 7], "short": FALSE, "type": "sticky" if d & 8 else "normal"}
    if b == 35:
        return f"minecraft:{DYE[d]}_wool", {}
    if b == 43:
        return f"minecraft:{SLABS[d]}_slab" if d != 2 else "minecraft:petrified_oak_slab", {"type": "double", "waterlogged": FALSE}
    if b == 44:
        return f"minecraft:{SLABS[d & 7]}_slab", {"type": "top" if d & 8 else "bottom", "waterlogged": FALSE}
    if b in STAIRS:
        return stairs(STAIRS[b], d)
    if b == 61 or b == 62:
        return "minecraft:furnace", {"facing": WALL_FACING[d], "lit": "true" if b == 62 else FALSE}
    if b == 68:
        return "minecraft:oak_wall_sign", {"facing": WALL_FACING[d], "waterlogged": FALSE}
    if b in (77, 143):
        face, facing = BUTTON[d & 7]
        return "minecraft:stone_button" if b == 77 else "minecraft:oak_button", {"face": face, "facing": facing, "powered": FALSE}
    if b in (95, 160):
        if b == 95:
            return f"minecraft:{DYE[d]}_stained_glass", {}
        return f"minecraft:{DYE[d]}_stained_glass_pane", {k: FALSE for k in ("north", "south", "east", "west", "waterlogged")}
    if b == 96:
        return "minecraft:oak_trapdoor", {"facing": TRAPDOOR_FACING[d & 3], "open": "true" if d & 4 else FALSE,
                                          "half": "top" if d & 8 else "bottom", "powered": FALSE, "waterlogged": FALSE}
    if b == 102:
        return "minecraft:glass_pane", {k: FALSE for k in ("north", "south", "east", "west", "waterlogged")}
    if b == 118:
        return ("minecraft:water_cauldron", {"level": str(d)}) if d else ("minecraft:cauldron", {})
    if b == 131:
        return "minecraft:tripwire_hook", {"facing": HOOK_FACING[d & 3], "attached": "true" if d & 4 else FALSE,
                                           "powered": "true" if d & 8 else FALSE}
    if b == 140:
        return "minecraft:flower_pot", {}  # its plant: from its block entity (POTTED)
    if b == 155:
        return ("minecraft:quartz_block", {}) if d == 0 else ("minecraft:chiseled_quartz_block", {}) if d == 1 else \
            ("minecraft:quartz_pillar", {"axis": {2: "y", 3: "x", 4: "z"}[d]})
    if b == 159:
        return f"minecraft:{DYE[d]}_terracotta", {}
    if b == 171:
        return f"minecraft:{DYE[d]}_carpet", {}
    if b == 177:
        return "minecraft:white_wall_banner", {"facing": WALL_FACING[d]}  # its colour: from its block entity (Base)
    if b in DOORS:
        kind = DOORS[b]
        if d & 8:
            lb, ld = below
            assert lb == b and not ld & 8, (b, d, below)
            return f"minecraft:{kind}_door", {"half": "upper", "facing": DOOR_FACING[ld & 3], "open": "true" if ld & 4 else FALSE,
                                              "hinge": "right" if d & 1 else "left", "powered": FALSE}
        # The lower half's hinge is the upper half's (the game reads both halves as one).
        return f"minecraft:{kind}_door", {"half": "lower", "facing": DOOR_FACING[d & 3], "open": "true" if d & 4 else FALSE,
                                          "hinge": "left", "powered": FALSE}
    raise SystemExit(f"{SOURCE.name}: no 1.20.1 state for block {b}:{d}")


#: Their flower pots' plants (old id and data) as 1.20.1's potted blocks; an empty pot stays a pot.
POTTED = {("minecraft:sapling", 0): "potted_oak_sapling", ("minecraft:sapling", 1): "potted_spruce_sapling",
          ("minecraft:sapling", 2): "potted_birch_sapling", ("minecraft:sapling", 3): "potted_jungle_sapling",
          ("minecraft:sapling", 4): "potted_acacia_sapling", ("minecraft:sapling", 5): "potted_dark_oak_sapling",
          ("minecraft:cactus", 0): "potted_cactus", ("minecraft:yellow_flower", 0): "potted_dandelion",
          ("minecraft:red_flower", 0): "potted_poppy", ("minecraft:red_flower", 1): "potted_blue_orchid",
          ("minecraft:red_flower", 2): "potted_allium", ("minecraft:red_flower", 3): "potted_azure_bluet",
          ("minecraft:red_flower", 4): "potted_red_tulip", ("minecraft:red_flower", 5): "potted_orange_tulip",
          ("minecraft:red_flower", 6): "potted_white_tulip", ("minecraft:red_flower", 7): "potted_pink_tulip",
          ("minecraft:red_flower", 8): "potted_oxeye_daisy", ("minecraft:tallgrass", 2): "potted_fern",
          ("minecraft:deadbush", 0): "potted_dead_bush", ("minecraft:red_mushroom", 0): "potted_red_mushroom",
          ("minecraft:brown_mushroom", 0): "potted_brown_mushroom"}

#: 4.1.51 (the owner chose "Rename to CCG"): the business park's sign texts as the CCG's (whole signs, four lines each, after the stray
#: characters are dropped); every other sign as it is (the restaurant, the bathrooms, the kitchen, the lift, the boss's two rules).
SIGNS = {
    ("Apollo House", "Business Park", "Units 1-56", ""): ("CCG", "Commission of", "Counter Ghoul", "Headquarters"),
    ("Lifts", "to", "Business", "Centres"): ("Lifts", "to", "CCG", "Offices"),
}
ORDINALS = ["First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth"]
for _k, _o in enumerate(ORDINALS):
    for _form in ((f"{_o} Floor", "Business", "Centre", ""), (_o, "Floor", "Business", "Centre")):
        SIGNS[_form] = (f"{_o} Floor", "CCG", "Offices", "")
for _n in range(1, 9):
    SIGNS[("", f"Unit {_n}", "", "")] = ("", f"Office {_n}", "", "")

#: Paintings of their era by the names 1.20.1 gives them.
MOTIVES = {"Kebab": "kebab", "Aztec": "aztec", "Alban": "alban", "Aztec2": "aztec2", "Bomb": "bomb", "Plant": "plant",
           "Wasteland": "wasteland", "Pool": "pool", "Courbet": "courbet", "Sea": "sea", "Sunset": "sunset", "Creebet": "creebet",
           "Wanderer": "wanderer", "Graham": "graham", "Match": "match", "Bust": "bust", "Stage": "stage", "Void": "void",
           "SkullAndRoses": "skull_and_roses", "Wither": "wither", "Fighters": "fighters", "Pointer": "pointer", "Pigscene": "pigscene",
           "BurningSkull": "burning_skull", "Skeleton": "skeleton", "DonkeyKong": "donkey_kong"}
#: Their item frames' items by 1.20.1's ids (and a potion's contents).
ITEMS = {("minecraft:cooked_fish", 0): "minecraft:cooked_cod", ("minecraft:cooked_fish", 1): "minecraft:cooked_salmon",
         ("minecraft:speckled_melon", 0): "minecraft:glistering_melon_slice"}
#: Their era's potions by damage value: what 1.20.1 calls the bottle and its contents (0: water; 16428: a splash potion of harming II).
POTIONS = {0: ("minecraft:potion", "minecraft:water"), 16428: ("minecraft:splash_potion", "minecraft:strong_harming")}
#: 2D facing (0 south, 1 west, 2 north, 3 east) as 1.20.1's 3D data value (an item frame's Facing).
FACE3D = {0: 3, 1: 4, 2: 2, 3: 5}


def val(t):
    return t.value if isinstance(t, L.Tag) else t


def plain(t):
    v = val(t)
    if isinstance(v, dict):
        return {k: plain(x) for k, x in v.items()}
    if isinstance(v, tuple):
        return [plain(e) for e in v[1]]
    return v


def clean(s):
    return "".join(ch for ch in s if not 0xF700 <= ord(ch) <= 0xF7FF)


def flat(j):
    if isinstance(j, str):
        return j
    if isinstance(j, list):
        return "".join(flat(e) for e in j)
    if isinstance(j, dict):
        return j.get("text", "") + "".join(flat(e) for e in j.get("extra", []))
    return ""


def stripped(j):
    if isinstance(j, str):
        return clean(j)
    if isinstance(j, list):
        return [stripped(e) for e in j]
    if isinstance(j, dict):
        return {k: stripped(v) for k, v in j.items()}
    return j


def sign_lines(t):
    """A sign's four lines as 1.20.1 JSON texts: renamed if it is one of the business park's (SIGNS), else as they were (colours kept)."""
    raw = []
    for k in range(1, 5):
        s = t.get(f"Text{k}", "")
        try:
            j = json.loads(s) if s else ""
        except json.JSONDecodeError:
            j = s
        raw.append(stripped(j))
    texts = tuple(flat(j).strip() for j in raw)
    if texts in SIGNS:
        return [json.dumps({"text": line}) for line in SIGNS[texts]]
    return [json.dumps(j if isinstance(j, dict) else {"text": flat(j)}) for j in raw]


def read_tower():
    """Their tower in 1.20.1's terms: cells {(x, y, z): (name, props)}, size (w, h, d), block entities {(x, y, z): (id, {key: Tag})},
    hanging things [(kind, x, y, z, nbt {key: Tag})] (paintings, item frames: their block), and the staff's spots [(x, y, z)]."""
    root = {k: val(v) for k, v in val(L.read(SOURCE)).items()}
    w, h, d = root["Width"], root["Height"], root["Length"]
    ids, data = root["Blocks"], root["Data"]
    ox, oy, oz = root["WEOriginX"], root["WEOriginY"], root["WEOriginZ"]
    cells = {}
    for y in range(h):
        for z in range(d):
            for x in range(w):
                i = (y * d + z) * w + x
                below = None
                if y > 0:
                    j = ((y - 1) * d + z) * w + x
                    below = (ids[j] & 0xFF, data[j] & 0x0F)
                s = state(ids[i] & 0xFF, data[i] & 0x0F, below)
                if s is not None:
                    cells[(x, y, z)] = s
    # The doors' lower halves take their upper halves' hinge.
    for (x, y, z), (n, p) in list(cells.items()):
        if n.endswith("_door") and p["half"] == "upper" and (x, y - 1, z) in cells:
            ln, lp = cells[(x, y - 1, z)]
            cells[(x, y - 1, z)] = (ln, dict(lp, hinge=p["hinge"]))
    entities = {}
    for t in plain(root["TileEntities"]):
        p = (t["x"], t["y"], t["z"])
        kind = t["id"]
        if kind == "Sign":
            lines = sign_lines(t)
            empty = [json.dumps({"text": ""})] * 4
            side = lambda msgs: L.compound(messages=L.strings(*msgs), color=L.s("black"), has_glowing_text=L.Tag(L.BYTE, 0))  # noqa: E731
            entities[p] = ("minecraft:sign", {"front_text": side(lines), "back_text": side(empty), "is_waxed": L.Tag(L.BYTE, 0)})
        elif kind == "Banner":
            # Their era's banner colours are the dyes' damage values (0 black .. 15 white): 1.20.1's are the other way round.
            base = DYE[15 - t["Base"]]
            n, props = cells[p]
            cells[p] = (n.replace("white", base), props)
            patterns = [{"Pattern": L.s(q["Pattern"]), "Color": L.Tag(L.INT, 15 - q["Color"])} for q in t.get("Patterns", [])]
            entities[p] = ("minecraft:banner", {"Patterns": L.Tag(L.LIST, (L.COMPOUND, patterns))} if patterns else {})
        elif kind == "FlowerPot":
            item = t.get("Item", "")
            if item:
                cells[p] = ("minecraft:" + POTTED[(item, t.get("Data", 0))], {})
        elif kind == "Furnace":
            entities[p] = ("minecraft:furnace", {})
        elif kind == "Music":
            pass  # a note block keeps its note in its state
        else:
            raise SystemExit(f"{SOURCE.name}: no 1.20.1 block entity for {kind}")
    hanging, staff = [], []
    for e in plain(root["Entities"]):
        kind = e["id"]
        if kind == "Villager":
            # 4.1.51 (the owner chose "Restaurant staff humans"): where each stood, the mod's human instead.
            px, py, pz = e["Pos"]
            staff.append((int(px // 1) - ox, int(round(py)) - oy, int(pz // 1) - oz))
            continue
        tile = (e["TileX"] - ox, e["TileY"] - oy, e["TileZ"] - oz)
        if kind == "Painting":
            nbt = {"id": L.s("minecraft:painting"), "variant": L.s("minecraft:" + MOTIVES[e["Motive"]]),
                   "facing": L.Tag(L.BYTE, e["Facing"])}
        elif kind == "ItemFrame":
            it = e["Item"]
            key = (it["id"], it.get("Damage", 0))
            item = {"Count": L.Tag(L.BYTE, it.get("Count", 1))}
            if it["id"] == "minecraft:potion":
                pid, contents = POTIONS[it.get("Damage", 0)]
                item["id"] = L.s(pid)
                item["tag"] = L.compound(Potion=L.s(contents))
            else:
                item["id"] = L.s(ITEMS.get(key, it["id"]))
            nbt = {"id": L.s("minecraft:item_frame"), "Facing": L.Tag(L.BYTE, FACE3D[e["Facing"]]),
                   "ItemRotation": L.Tag(L.BYTE, e.get("ItemRotation", 0)), "Item": L.compound(**item),
                   "ItemDropChance": L.Tag(L.FLOAT, 1.0)}
        else:
            raise SystemExit(f"{SOURCE.name}: no 1.20.1 entity for {kind}")
        assert 0 <= tile[0] < w and 0 <= tile[1] < h and 0 <= tile[2] < d, (kind, tile)
        hanging.append((kind, *tile, nbt))
    return cells, (w, h, d), entities, hanging, staff


def write(cells, size, entities, hanging):
    """build/ccg/ccg_tower.schem: Sponge v3 as tools/city_structure.py writes it, its hanging things in an Entities list (Pos: their
    block's middle, so the game hangs each in the block it was in)."""
    w, h, d = size
    used = {"minecraft:air": 0}
    data = bytearray()

    def varint(v):
        out = bytearray()
        while True:
            b = v & 0x7F
            v >>= 7
            if v:
                out.append(b | 0x80)
            else:
                out.append(b)
                return out

    for y in range(h):
        for z in range(d):
            for x in range(w):
                c = cells.get((x, y, z))
                s = "minecraft:air" if c is None else c[0] + ("[" + ",".join(f"{k}={v}" for k, v in sorted(c[1].items())) + "]" if c[1] else "")
                if s not in used:
                    used[s] = len(used)
                data += varint(used[s])
    bes = [{"Id": L.s(kind), "Pos": L.Tag(L.INTS, list(p)), "Data": L.compound(**inner)} for p, (kind, inner) in sorted(entities.items())]
    ents = [{"Id": nbt["id"], "Pos": L.doubles(x + 0.5, y + 0.5, z + 0.5), "Data": L.compound(**{k: v for k, v in nbt.items() if k != "id"})}
            for _, x, y, z, nbt in hanging]
    root = {
        "Version": L.Tag(L.INT, 3),
        "DataVersion": L.Tag(L.INT, 3465),
        "Width": L.Tag(L.SHORT, w),
        "Height": L.Tag(L.SHORT, h),
        "Length": L.Tag(L.SHORT, d),
        "Blocks": L.compound(Palette=L.compound(**{s: L.Tag(L.INT, i) for s, i in used.items()}),
                             Data=L.Tag(L.BYTES, bytearray(data)), BlockEntities=L.Tag(L.LIST, (L.COMPOUND, bes))),
        "Entities": L.Tag(L.LIST, (L.COMPOUND, ents)),
    }
    OUT.mkdir(parents=True, exist_ok=True)
    L.write(PATH, {"Schematic": L.compound(**root)})
    return len(used)


def make():
    """The tower's schematic (written once a run); the staff's spots."""
    cells, size, entities, hanging, staff = read_tower()
    write(cells, size, entities, hanging)
    return staff


if __name__ == "__main__":
    c, size, ents, hang, staff = read_tower()
    kinds = write(c, size, ents, hang)
    print(f"{PATH}: {size}, {len(c)} blocks, {kinds} states, {len(ents)} block entities, {len(hang)} hanging, staff at {staff}")
