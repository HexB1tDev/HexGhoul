#!/usr/bin/env python3
"""4.0.0 (The Great Exploration): the owner's new places (4.1.4: a fourth, the CCG building), from their schematics (art/exploration/, saved by newer games, Sponge v2
and v3), to 1.20.1 structure templates cut into tiles, with the people who live in each, and the maps the game walks them by.

    python3 tools/exploration_structures.py --blocks blocks.json

  art/city/small-city.zip               -> hexkagune:small_city      4.1.35/4.1.36: the owner's Small City, four quarters
                                                                       (small_city_0 .. 3, tools/city_structure.py); it replaced
                                                                       village_1 (the human town) and village_2 (the CCG settlement)
  art/exploration/aogiri_mansion.schem  -> hexkagune:aogiri_hideout  the Aogiri Tree hideout (4.0.2: the owner's mansion, replacing
                                                                       hideout_ougiri.schem: its guards, its patrol, its members, Ayato
                                                                       and Noro in rooms of their own)
  art/exploration/ccg_building.schem    -> hexkagune:ccg_building    4.1.4: the CCG's building (guards at its door and in its yard, a
                                                                       patrol round it, investigators at the desks and in the halls of
                                                                       every floor)

Each place is written as tiles (data/hexkagune/structures/<site>/t_<i>_<j>.nbt, at most TILE blocks a side) that the expansion's own
structure type (ControlStructures.Site) places side by side, turned as a whole, from its start chunk; its numbers and its map are written to
src/main/java/com/hex/kagune/control/village/SiteMaps.java. The blocks 1.20.1 does not have are swapped for its nearest (the list in
SWAPS); a property 1.20.1's block does not have is dropped; barriers, structure voids and jigsaws are left out; leaves are made persistent;
block entities are written as 1.20.1 reads them (signs' text as JSON, containers empty, some of them given a loot table). Natural ground
more than KEEP blocks under the place's own ground is left to the world's (the structure's terrain adaptation, beard_box, fills under it
and clears the world's ground over it). Nothing in the owner's files is changed.

--blocks: PrismarineJS minecraft-data's 1.20 blocks.json, to check every block state written is one 1.20.1 has (it fails if not).
"""
import collections
import json
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import library_structure as L  # noqa: E402
from companion_structures import Registry  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art" / "exploration"
DATA = ROOT / "src" / "main" / "resources" / "data" / "hexkagune"
JAVA = ROOT / "src" / "main" / "java" / "com" / "hex" / "kagune" / "control" / "village" / "SiteMaps.java"
DATA_VERSION = 3465  # 1.20.1
TILE = 64
KEEP = 2

SITES = [
    # 4.1.36 (the owner: "Delete all ids hexkagune:village_x ( and anything related)"): the human town (village_1, map 3) and the CCG
    # settlement (village_2, map 4) are gone; the Small City (below) stands in their place.
    # 4.0.2 (the owner: "we're gonna replace the Ougiri hideout with another schematic that I got ... I give them a mansion because I just
    # thought the Aogiri Tree deserves it"): 320 x 263, its grounds walled round. 272 is all a structure's start chunk reaches, so a band of
    # 24 columns is left out either side (the inner edge of the side wall's trees and the plain paving beside it: whole repeats of the
    # paving's 6, so no seam); its walls, trees, building, ponds and helipad are as they are. Guards stand at its two gates (north and
    # south, either side of the path; one at each asks a newcomer their business), a patrol walks round the building, the rest are spread
    # through every floor and the grounds; Noro has a suite on the top floor (west), Ayato the great lounge on the first (east).
    {"id": "aogiri_hideout", "source": "aogiri_mansion.schem", "map": 5, "crop_x": None, "cut_x": [(11, 35), (285, 309)], "kind": "aogiri",
     "houses": 0, "agents": 0, "members": 12, "patrol": 6, "ring": (60, 130), "loot_every": 3, "ground": 0,
     "posts": [(152, 10, True), (168, 10, False), (152, 252, True), (168, 252, False)],
     "loop": (30, 290, 55, 195), "building": (42, 278, 72, 180), "top_floor": 27,
     "rooms": {"hexkagune:noro": (118, 26, 88), "hexkagune:ayato": (262, 14, 105)}},
    # 4.1.51 (the owner: "New CCG schematic, map it out with sitting areas, offices and etc."; then "I'll get you a new one fully
    # furnished"): their furnished tower (art/exploration/CCG.schematic, MCEdit Alpha; tools/ccg_tower.py writes it as build/ccg/
    # ccg_tower.schem, its signs the CCG's, its paintings and item frames kept), 31 x 65 x 46: a lobby (feet y 4: the reception counter,
    # the restaurant, its kitchen) and nine office floors over it, one every 6 blocks (feet y 10 .. 58), a lift between them (server/
    # CcgLift). Its ground y 3 (a pavement round it); its door the gap in its north front (x 15). Two guards outside the door and two either
    # side of the lift in the lobby; the two receptionists at the counter; a patrol of six round it (the walk between its hedges and its
    # glass, then the world's ground two blocks out, which its terrain adaptation levels); on every office floor three at their desks
    # (their chairs: a stair with nothing over it, a desk or a table before it) and one by the lift; the restaurant's staff, the mod's
    # humans where its four villagers stood (the owner chose "Restaurant staff humans"), keeping indoors; the General in the top floor's
    # boss's office (the one with "Rule 1: Boss is always right" over its chair). Its map is 11: the old building's people (map 6) keep
    # theirs (village/RetiredSites).
    {"id": "ccg_building", "source": "ccg_tower", "map": 11, "crop_x": None, "kind": "ccg_hq", "houses": 0, "agents": 0,
     "ring": (0, 0), "loot_every": 0, "ground": 3, "storey": 6, "floors": 10, "seats": 3, "hanging": True,
     "posts": [(14, 1, 180.0), (16, 1, 180.0)],
     "lobby_posts": [(14, 12, 180.0), (16, 12, 180.0)],
     "lobby_seats": [(21, 5), (21, 7)],
     "loop": [(-2, -2), (8, -2), (15, -2), (22, -2), (32, -2), (32, 8), (32, 18), (32, 28), (32, 38), (32, 47), (22, 47), (15, 47),
              (8, 47), (-2, 47), (-2, 38), (-2, 28), (-2, 18), (-2, 8)],
     "patrol": [(2, 9), (2, 24), (2, 39), (28, 9), (28, 24), (28, 39)], "hall": (12, 10),
     # The General, standing before the boss's desk on the top floor (x, feet y, z); no investigator takes a chair of that office.
     "rooms": {"hexkagune:general": (23, 58, 25)}, "boss_office": (22, 24, 26, 31)},
]

# 4.1.35 (the owner: "ALl villages get replaced with the city in my minecraft world that I sent named "Small-City""): the city, 544 x 544,
# cut by tools/city_structure.py into four 272 x 272 quarters (0 north-west, 1 north-east, 2 south-west, 3 south-east: what one structure can
# place) on the city's one ground (its schematics' layer 6, the world's y 64), placed side by side by ControlStructures. Its people: humans
# at home and investigators on patrol in every quarter.
import city_structure  # noqa: E402

CITY_GROUND = 6
for _k in range(4):
    SITES.append({"id": f"small_city_{_k}", "source": str(city_structure.OUT / f"small_city_{_k}.schem"), "map": 7 + _k, "crop_x": None,
                  "kind": "human", "houses": 12, "agents": 2, "ring": (24, 110), "loot_every": 7, "ground": CITY_GROUND, "city": True})

# 1.20.1 has none of these (1.20.3 to 1.21.9): each one's nearest, and the properties it keeps.
SWAPS = {
    "minecraft:short_grass": ("minecraft:grass", ()),
    "minecraft:iron_chain": ("minecraft:chain", ("axis", "waterlogged")),
    "minecraft:waxed_weathered_copper_lantern": ("minecraft:lantern", ("hanging", "waterlogged")),
    "minecraft:pale_oak_trapdoor": ("minecraft:birch_trapdoor", ("facing", "half", "open", "powered", "waterlogged")),
    "minecraft:pale_oak_stairs": ("minecraft:birch_stairs", ("facing", "half", "shape", "waterlogged")),
    "minecraft:pale_oak_slab": ("minecraft:birch_slab", ("type", "waterlogged")),
    "minecraft:stripped_pale_oak_log": ("minecraft:stripped_birch_log", ("axis",)),
    "minecraft:pale_oak_button": ("minecraft:birch_button", ("face", "facing", "powered")),
    "minecraft:pale_oak_fence_gate": ("minecraft:birch_fence_gate", ("facing", "in_wall", "open", "powered")),
    "minecraft:bush": ("minecraft:fern", ()),
    "minecraft:waxed_exposed_lightning_rod": ("minecraft:lightning_rod", ("facing", "powered", "waterlogged")),
    "minecraft:exposed_lightning_rod": ("minecraft:lightning_rod", ("facing", "powered", "waterlogged")),
    "minecraft:weathered_lightning_rod": ("minecraft:lightning_rod", ("facing", "powered", "waterlogged")),
    "minecraft:copper_grate": ("minecraft:cut_copper", ()),
    "minecraft:waxed_copper_grate": ("minecraft:waxed_cut_copper", ()),
    "minecraft:waxed_weathered_copper_trapdoor": ("minecraft:warped_trapdoor", ("facing", "half", "open", "powered", "waterlogged")),
    # The heavy core (1.21): a small dark weight on the floor; a polished blackstone pressure plate lies there as low and as dark.
    "minecraft:heavy_core": ("minecraft:polished_blackstone_pressure_plate", ()),
    "minecraft:bedrock": ("minecraft:stone", ()),
    # 4.0.2, the mansion's (1.21 to 1.21.9).
    "minecraft:stripped_pale_oak_wood": ("minecraft:stripped_birch_wood", ("axis",)),
    "minecraft:pale_oak_planks": ("minecraft:birch_planks", ()),
    "minecraft:pale_oak_fence": ("minecraft:birch_fence", ("east", "north", "south", "waterlogged", "west")),
    "minecraft:pale_oak_door": ("minecraft:birch_door", ("facing", "half", "hinge", "open", "powered")),
    "minecraft:waxed_copper_bars": ("minecraft:iron_bars", ("east", "north", "south", "waterlogged", "west")),
    "minecraft:oxidized_copper_bars": ("minecraft:iron_bars", ("east", "north", "south", "waterlogged", "west")),
    "minecraft:polished_tuff": ("minecraft:tuff", ()),
    "minecraft:polished_tuff_wall": ("minecraft:andesite_wall", ("east", "north", "south", "up", "waterlogged", "west")),
    "minecraft:polished_tuff_slab": ("minecraft:polished_andesite_slab", ("type", "waterlogged")),
    "minecraft:resin_bricks": ("minecraft:smooth_red_sandstone", ()),
    "minecraft:resin_brick_wall": ("minecraft:red_sandstone_wall", ("east", "north", "south", "up", "waterlogged", "west")),
    "minecraft:waxed_copper_lantern": ("minecraft:lantern", ("hanging", "waterlogged")),
    "minecraft:waxed_copper_trapdoor": ("minecraft:acacia_trapdoor", ("facing", "half", "open", "powered", "waterlogged")),
    "minecraft:copper_trapdoor": ("minecraft:acacia_trapdoor", ("facing", "half", "open", "powered", "waterlogged")),
    "minecraft:exposed_copper_trapdoor": ("minecraft:acacia_trapdoor", ("facing", "half", "open", "powered", "waterlogged")),
    "minecraft:waxed_copper_bulb": ("minecraft:shroomlight", ()),
    "minecraft:waxed_copper_golem_statue": ("minecraft:decorated_pot", ("facing", "waterlogged")),
    # A shelf (1.21.9): its wood's trapdoor laid shut at the top of the block, a board on the wall.
    "minecraft:pale_oak_shelf": ("minecraft:birch_trapdoor", ("facing", "waterlogged"), {"half": "top", "open": "false"}),
    "minecraft:spruce_shelf": ("minecraft:spruce_trapdoor", ("facing", "waterlogged"), {"half": "top", "open": "false"}),
    "minecraft:jungle_shelf": ("minecraft:jungle_trapdoor", ("facing", "waterlogged"), {"half": "top", "open": "false"}),
    "minecraft:cherry_shelf": ("minecraft:cherry_trapdoor", ("facing", "waterlogged"), {"half": "top", "open": "false"}),
    "minecraft:acacia_shelf": ("minecraft:acacia_trapdoor", ("facing", "waterlogged"), {"half": "top", "open": "false"}),
    "minecraft:oak_shelf": ("minecraft:oak_trapdoor", ("facing", "waterlogged"), {"half": "top", "open": "false"}),
    "minecraft:dark_oak_shelf": ("minecraft:dark_oak_trapdoor", ("facing", "waterlogged"), {"half": "top", "open": "false"}),
    "minecraft:mangrove_shelf": ("minecraft:mangrove_trapdoor", ("facing", "waterlogged"), {"half": "top", "open": "false"}),
    # 4.1.35, the Small City's (1.21.4 to 1.21.5): a pale oak sign on birch, tuff bricks as stone bricks, wildflowers (a carpet of small
    # flowers over the ground, one to four a block) as pink petals, laid the same way.
    "minecraft:pale_oak_wall_sign": ("minecraft:birch_wall_sign", ("facing", "waterlogged")),
    "minecraft:tuff_bricks": ("minecraft:stone_bricks", ()),
    "minecraft:wildflowers": ("minecraft:pink_petals", ("facing", "flower_amount")),
    # Infested blocks look like the plain ones and let silverfish out when broken: the city's (and the town's one) are plain.
    "minecraft:infested_stone_bricks": ("minecraft:stone_bricks", ()),
    "minecraft:infested_stone": ("minecraft:stone", ()),
}
# 4.1.35: block entities 1.20.1 has no kind of (their block swapped for one without any): left out of the city (the older places keep theirs
# as they were: the game skips them).
NO_ENTITY = {"minecraft:shelf"}
# 4.1.35: a banner's patterns as 1.20.1 keeps them (Patterns: a short code and a dye number; a newer game writes patterns: an id and a
# colour's name). Only the ones the owner's files use.
BANNER_CODES = {"minecraft:gradient_up": "gru", "minecraft:border": "bo", "minecraft:stripe_downright": "drs",
                "minecraft:stripe_downleft": "dls", "minecraft:rhombus": "mr"}
DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown",
        "green", "red", "black"]
DROP = {"minecraft:barrier", "minecraft:structure_void", "minecraft:jigsaw", "minecraft:structure_block", "minecraft:command_block",
        "minecraft:light", "minecraft:cactus_flower"}
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}
NATURAL = {"minecraft:stone", "minecraft:dirt", "minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:granite", "minecraft:andesite",
           "minecraft:diorite", "minecraft:gravel", "minecraft:sand", "minecraft:podzol", "minecraft:clay", "minecraft:rooted_dirt"}
GROUNDS = {"minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:dirt_path", "minecraft:gravel",
           "minecraft:sand"}
PASS = ("_carpet", "_button", "_pressure_plate", "_wall_banner", "_wall_sign", "minecraft:grass", "minecraft:fern", "minecraft:tall_grass",
        "_torch", "minecraft:snow", "minecraft:rail", "minecraft:tripwire", "minecraft:lever", "_flower", "minecraft:dandelion",
        "minecraft:poppy", "minecraft:pink_petals")
OPEN = ("minecraft:air", "minecraft:ladder", "minecraft:chain", "minecraft:water")
CONTAINERS = ("minecraft:chest", "minecraft:barrel", "minecraft:trapped_chest")
LOOT = {"human": "minecraft:chests/village/village_plains_house", "aogiri": "hexkagune:chests/aogiri_hideout"}


def val(t):
    return t.value if isinstance(t, L.Tag) else t


def parse(state):
    if "[" not in state:
        return state, {}
    name, rest = state.split("[", 1)
    return name, dict(kv.split("=", 1) for kv in rest.rstrip("]").split(","))


def text(name, props):
    return name + ("[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]" if props else "")


def convert(state, registry):
    """A newer game's block state as 1.20.1 has it: (name, props), or None (air, or left out)."""
    name, props = parse(state)
    if name in AIR or name in DROP:
        return None
    if name in SWAPS:
        new, keeps, *fixed = SWAPS[name]
        props = {k: v for k, v in props.items() if k in keeps}
        if fixed:
            props.update(fixed[0])
        name = new
    if name.endswith("_leaves"):
        props["persistent"] = "true"
    if registry is not None:
        allowed = registry.blocks.get(name)
        if allowed is None:
            raise SystemExit(f"1.20.1 has no {name} (swap it)")
        props = {k: v for k, v in props.items() if k in allowed and v in allowed[k]}
        registry.check(name, props)
    return name, props


def load(path):
    root = {k: val(v) for k, v in val(L.read(path)).items()}
    if "Schematic" in root:
        root = {k: val(v) for k, v in root["Schematic"].items()}
    if "Blocks" in root:
        blocks = {k: val(v) for k, v in root["Blocks"].items()}
        palette, data, entities = blocks["Palette"], blocks["Data"], blocks.get("BlockEntities", (L.COMPOUND, []))
    else:
        palette, data, entities = root["Palette"], root["BlockData"], root.get("BlockEntities", (L.COMPOUND, []))
    by_index = {val(v): k for k, v in palette.items()}
    ids = L.varints(bytes(b & 0xFF for b in data))
    w, h, d = root["Width"], root["Height"], root["Length"]
    assert len(ids) == w * h * d, path
    raw_entities = {}
    for t in val(entities)[1] if isinstance(entities, L.Tag) else entities[1]:
        t = {k: v for k, v in val(t).items()}
        x, y, z = val(t["Pos"])
        inner = {k: v for k, v in val(t["Data"]).items()} if "Data" in t else t
        raw_entities[(x, y, z)] = (val(t["Id"]), inner)
    return w, h, d, by_index, ids, raw_entities


def sign_line(line):
    """A sign's line as 1.20.1 keeps it (a JSON text): a newer game's plain string, or JSON as it is."""
    if isinstance(line, str) and (line.startswith("{") or line.startswith('"') or line.startswith("[")):
        return line
    return json.dumps({"text": line if isinstance(line, str) else ""})


def block_entity(kind, inner, loot, seed):
    out = {"id": L.s(kind)}
    if kind in ("minecraft:sign", "minecraft:hanging_sign"):
        for side in ("front_text", "back_text"):
            t = {k: val(v) for k, v in val(inner[side]).items()} if side in inner else {}
            lines = [sign_line(val(m)) for m in (val(t["messages"])[1] if "messages" in t else ["", "", "", ""])]
            out[side] = L.compound(messages=L.strings(*lines), color=L.s(val(t.get("color", "black"))),
                                   has_glowing_text=L.Tag(L.BYTE, val(t.get("has_glowing_text", 0))))
        out["is_waxed"] = L.Tag(L.BYTE, 0)
    elif kind == "minecraft:banner" and "patterns" in inner:
        patterns = []
        for q in val(inner["patterns"])[1]:
            q = {k: val(v) for k, v in val(q).items()}
            patterns.append({"Pattern": L.s(BANNER_CODES[q["pattern"]]), "Color": L.Tag(L.INT, DYES.index(q["color"]))})
        out["Patterns"] = L.Tag(L.LIST, (L.COMPOUND, patterns))
    elif kind in CONTAINERS or kind in ("minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:hopper",
                                        "minecraft:dispenser", "minecraft:dropper", "minecraft:campfire", "minecraft:soul_campfire"):
        out["Items"] = L.Tag(L.LIST, (L.COMPOUND, []))
        if loot and kind in CONTAINERS:
            out["LootTable"] = L.s(loot)
            out["LootTableSeed"] = L.Tag(L.LONG, seed)
    return L.compound(**out)


def load_hanging(path):
    """4.1.51: the things hung on its walls (paintings, item frames) a Sponge v3 schematic keeps in its Entities: [(their block, nbt)]."""
    root = {k: val(v) for k, v in val(L.read(path)).items()}
    if "Schematic" in root:
        root = {k: val(v) for k, v in root["Schematic"].items()}
    out = []
    for t in val(root.get("Entities", (L.COMPOUND, [])))[1]:
        t = {k: v for k, v in val(t).items()}
        x, y, z = (int(math.floor(c)) for c in val(t["Pos"])[1])
        out.append(((x, y, z), dict(id=t["Id"], **{k: v for k, v in val(t["Data"]).items()})))
    return out


def build(site, registry):
    staff = []
    path = ART / site["source"]
    if site["source"] == "ccg_tower":
        # 4.1.51: the CCG's tower, written from the owner's Alpha schematic first; where its restaurant's staff stand.
        import ccg_tower
        staff = ccg_tower.make()
        path = ccg_tower.PATH
    w, h, d, by_index, ids, raw_entities = load(path)
    hanging = load_hanging(path) if site.get("hanging") else []
    if "top" in site:
        # 4.1.4: the layers over its roof (empty in the owner's file) left off: the file's layers are y, then z, then x.
        assert all(by_index[i].split("[")[0] in AIR for i in ids[site["top"] * d * w:]), site["id"]
        h = site["top"]
        ids = ids[:h * d * w]
    x0, x1 = site["crop_x"] or (0, w)
    # 4.0.2: the columns kept (the crop's, less any bands cut out of it), and where each lands.
    kept = [x for x in range(x0, x1) if not any(a <= x < b for a, b in site.get("cut_x", ()))]
    at_x = {x: i for i, x in enumerate(kept)}
    width = len(kept)
    converted = {i: convert(s, registry) for i, s in by_index.items()}
    cells = {}
    for y in range(h):
        for z in range(d):
            row = (y * d + z) * w
            for x in kept:
                c = converted[ids[row + x]]
                if c is not None:
                    cells[(at_x[x], y, z)] = c
    entities = {(at_x[p[0]], p[1], p[2]): e for p, e in raw_entities.items() if p[0] in at_x and (at_x[p[0]], p[1], p[2]) in cells}

    def placed(x):
        """An x of the owner's schematic where it lands (a cut column: the nearest kept one)."""
        return at_x[min(kept, key=lambda k: abs(k - x))]

    def name(p):
        c = cells.get(p)
        return c[0] if c else "minecraft:air"

    def free(x, y, z):
        n = name((x, y, z))
        return n == "minecraft:air" or n in ("minecraft:ladder", "minecraft:chain") or any(n.endswith(k) or n == k for k in PASS)

    def water(p):
        c = cells.get(p)
        return c is not None and (c[0] == "minecraft:water" or c[1].get("waterlogged") == "true")

    def stands(x, y, z):
        if not (1 <= x < width - 1 and 1 <= z < d - 1 and 0 < y < h - 2):
            return False
        return not free(x, y - 1, z) and not water((x, y - 1, z)) and free(x, y, z) and free(x, y + 1, z) and not water((x, y, z)) \
            and name((x, y - 1, z)) not in ("minecraft:lava", "minecraft:campfire", "minecraft:magma_block")

    def roofed(x, y, z):
        return any(name((x, yy, z)) not in OPEN for yy in range(y + 2, min(h, y + 9)))

    top = {}
    for (x, y, z) in cells:
        if y > top.get((x, z), -1):
            top[(x, z)] = y
    # Its ground: the commonest top of its natural ground (round its middle; the hideout's over all of it: its middle is a hall on a mound).
    m = 0 if site.get("ground_all") else 6
    tops = collections.Counter(top[(x, z)] for x in range(width // m if m else 0, width - (width // m if m else 0))
                               for z in range(d // m if m else 0, d - (d // m if m else 0))
                               if (x, z) in top and name((x, top[(x, z)], z)) in GROUNDS)
    ground = site["ground"] if "ground" in site else tops.most_common(1)[0][0]
    street = ground + 1
    middle = (width // 2, street, d // 2)

    # What goes in the tiles: all but the natural ground more than KEEP under the place's own ground.
    out = {p: c for p, c in cells.items() if p[1] >= ground - KEEP or c[0] not in NATURAL}
    # Air at or under its ground where it is open from above (a sunken yard, a pool's basin, a pit): the world's ground, filled up to the
    # place's own ground under it, must not fill these.
    for x in range(width):
        for z in range(d):
            for y in range(ground + 1, ground - KEEP - 1, -1):
                if y < 0 or (x, y, z) in cells:
                    break
                out[(x, y, z)] = ("minecraft:air", {})

    # Its people.
    streets = []
    for x in range(2, width - 2, 3):
        for z in range(2, d - 2, 3):
            for y in range(max(1, street - 3), min(h - 2, street + 5)):
                if stands(x, y, z) and not roofed(x, y, z):
                    streets.append((x, y, z))
                    break
    rel = lambda p: math.hypot(p[0] - middle[0], p[2] - middle[2])  # noqa: E731
    lo, hi = site["ring"]
    ring = [p for p in streets if lo <= rel(p) <= hi and abs(p[1] - street) <= 3]
    route, last = [], None
    for p in sorted(ring, key=lambda p: math.atan2(p[2] - middle[2], p[0] - middle[0])):
        a = math.degrees(math.atan2(p[2] - middle[2], p[0] - middle[0]))
        if last is None or a - last >= 18.0:
            route.append(p)
            last = a
    homes = houses(cells, name, stands, roofed, width, d, h, street, middle) if site["houses"] else []
    if homes:
        taken = [min(homes, key=lambda p: rel(p))]
        while len(taken) < min(site["houses"], len(homes)):
            taken.append(max((p for p in homes if p not in taken), key=lambda p: min((p[0] - t[0]) ** 2 + (p[2] - t[2]) ** 2 for t in taken)))
        homes = taken
    people = []
    for k, home in enumerate(homes):
        people.append(("hexkagune:human", home, k, {}))
    for k, home in enumerate(homes):
        people.append(("hexkagune:human", home, len(homes) + k, {}))
    for k in range(site["agents"]):
        if k % 2 == 0 or site["kind"] != "ccg":
            spot = route[(k * len(route)) // max(1, site["agents"])]
        else:
            # A post: a street spot well spread from the others.
            spot = spread(streets, [p[1] for p in people], middle, hi + 8)
        people.append(("hexkagune:ccg_investigator", spot, -1, {}))
    if site["kind"] == "ccg_hq":
        people += headquarters(site, cells, stands, width, d, h, street, staff)
        route = [(x, street, z) for x, z in site["loop"]]
    elif site["kind"] == "aogiri" and "posts" in site:
        people += mansion(site, cells, stands, roofed, streets, placed, width, d, h, street, middle)
        route = site_loop(site, streets, placed, middle)
    elif site["kind"] == "aogiri":
        inside = [p for p in streets if rel(p) <= hi + 6]
        for k in range(site["members"]):
            spot = spread(inside, [p[1] for p in people], middle, hi + 6)
            people.append(("hexkagune:ghoul", spot, -1, {"HexAogiri": L.Tag(L.BYTE, 1)}))
        hall = sorted((p for p in floor_spots(stands, roofed, width, d, h, street) if rel(p) <= 14), key=rel)
        noro = hall[0] if hall else middle
        people.append(("hexkagune:noro", noro, -1, {}))
        front = sorted((p for p in streets if 6 <= rel(p) <= 22), key=lambda p: (abs(p[0] - middle[0]), -(p[2] - middle[2])))
        people.append(("hexkagune:ayato", front[0] if front else (middle[0] + 4, street, middle[2] + 4), -1, {}))

    rng = random.Random(site["id"])
    loot = LOOT.get(site["kind"])
    tiles = write_tiles(site, out, entities, people, middle, width, d, h, loot, rng, hanging)
    return {"site": site, "width": width, "depth": d, "height": h, "ground": ground, "middle": middle, "streets": streets, "route": route,
            "homes": homes, "tiles": tiles, "people": people, "blocks": sum(1 for c in out.values()), "cells": cells, "top": top}


def houses(cells, name, stands, roofed, width, d, h, street, middle):
    """A spot inside each door (any but iron) that opens from outside into a room with a roof over all of it."""
    def enclosed(start, y, door):
        seen, todo = {start}, [start]
        while todo:
            c = todo.pop()
            if not roofed(c[0], y, c[1]) or len(seen) > 500:
                return False
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (c[0] + dx, c[1] + dz)
                if n in seen or n == door:
                    continue
                if stands(n[0], y, n[1]):
                    seen.add(n)
                    todo.append(n)
        return True

    homes = []
    for (x, y, z), (n, props) in cells.items():
        if not n.endswith("_door") or n == "minecraft:iron_door" or props.get("half") != "lower" or abs(y - street) > 12:
            continue
        f = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[props["facing"]]
        for inside, outside in (((x - f[0], z - f[1]), (x + f[0], z + f[1])), ((x + f[0], z + f[1]), (x - f[0], z - f[1]))):
            if stands(inside[0], y, inside[1]) and stands(outside[0], y, outside[1]) and enclosed(inside, y, (x, z)) \
                    and not enclosed(outside, y, (x, z)):
                deeper = (2 * inside[0] - x, 2 * inside[1] - z)
                spot = deeper if stands(deeper[0], y, deeper[1]) else inside
                homes.append((spot[0], y, spot[1]))
                break
    return sorted(set(homes))


def standing(stands, x, y, z, reach=8):
    """The spot nearest ({@code x}, {@code y}, {@code z}) one can stand on (searching up and down, then out a little), or None."""
    for r in range(0, reach + 1):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if max(abs(dx), abs(dz)) != r:
                    continue
                for dy in sorted(range(-6, 7), key=abs):
                    if stands(x + dx, y + dy, z + dz):
                        return (x + dx, y + dy, z + dz)
    return None


def mansion(site, cells, stands, roofed, streets, placed, width, d, h, street, middle):
    """4.0.2: the mansion's people: its guards at their posts, its patrol, its members spread through it, Noro and Ayato in their rooms."""
    people = []
    taken = []
    for x, z, asks in site["posts"]:
        spot = standing(stands, placed(x), street, z)
        tags = {"HexAogiri": L.Tag(L.BYTE, 1), "HexAogiriPost": L.Tag(L.BYTE, 1)}
        if asks:
            tags["HexAogiriAsks"] = L.Tag(L.BYTE, 1)
        people.append(("hexkagune:ghoul", spot, -1, tags))
        taken.append(spot)
    for kind, (x, y, z) in site["rooms"].items():
        spot = standing(stands, placed(x), y, z)
        people.append((kind, spot, -1, {}))
        taken.append(spot)
    # Every spot one can stand on, every third block: inside on every floor (under a roof), outside on the ground (never on a tree or
    # the roof); the members spread over them, each as far as can be from the rest, most of them inside.
    def leafy(x, y, z):
        c = cells.get((x, y - 1, z))
        return c is not None and c[0].endswith("_leaves")
    inside, outside = [], []
    bx0, bx1, bz0, bz1 = site["building"]
    bx0, bx1 = placed(bx0), placed(bx1)
    for x in range(2, width - 2, 3):
        for z in range(2, d - 2, 3):
            for y in range(1, h - 2):
                if not stands(x, y, z) or leafy(x, y, z):
                    continue
                if roofed(x, y, z):
                    if bx0 <= x <= bx1 and bz0 <= z <= bz1 and y <= site["top_floor"] + 2:
                        inside.append((x, y, z))
                elif y <= street + 2:
                    outside.append((x, y, z))

    def far(pool):
        return max((p for p in pool if p not in taken), key=lambda p: min((p[0] - t[0]) ** 2 + 36 * (p[1] - t[1]) ** 2 + (p[2] - t[2]) ** 2
                                                                        for t in taken))
    loop = site_loop(site, streets, placed, middle)
    for k in range(site["patrol"]):
        spot = loop[(k * len(loop)) // site["patrol"]]
        people.append(("hexkagune:ghoul", spot, -1, {"HexAogiri": L.Tag(L.BYTE, 1), "HexAogiriPatrol": L.Tag(L.BYTE, 1)}))
        taken.append(spot)
    for k in range(site["members"]):
        spot = far(inside if k % 3 != 2 else outside)
        people.append(("hexkagune:ghoul", spot, -1, {"HexAogiri": L.Tag(L.BYTE, 1)}))
        taken.append(spot)
    return people


def headquarters(site, cells, stands, width, d, h, street, staff=()):
    """4.1.4: the CCG building's people: the guards at their posts (each looking the way given), the patrol on its loop, and on every floor
    investigators at desks (HexCcgSeat: the chair's block is where they are placed; HexCcgYaw the way they sit facing) and one by the lift.
    4.1.51: the new tower's: the lobby's two at the lift and two at the reception counter, its restaurant's staff (the mod's humans), and
    the General in the top floor's boss's office (none of his chairs taken)."""
    people, taken = [], []

    def agent(spot, **tags):
        assert spot is not None and spot not in taken, (spot, tags)
        people.append(("hexkagune:ccg_investigator", spot, -1, tags))
        taken.append(spot)

    def exact(x, y, z):
        spot = standing(stands, x, y, z, 0)
        assert spot == (x, y, z), ("no room to stand", (x, y, z), spot)
        return spot

    for kind, (x, y, z) in site.get("rooms", {}).items():
        spot = exact(x, y, z)
        people.append((kind, spot, -1, {}))
        taken.append(spot)
    for x, z, yaw in site["posts"] + site.get("lobby_posts", []):
        agent(exact(x, street, z), HexCcgPost=L.Tag(L.BYTE, 1), HexCcgYaw=L.Tag(L.INT, int(yaw)))
    for x, z in site["patrol"]:
        # Placed out of doors, by the loop (the loop itself goes round on the world's ground beside the building).
        agent(exact(x, street, z))

    # The way someone sat on a stair looks: away from its tall side (south is yaw 0, west 90, north 180, east -90).
    looks = {"north": 0.0, "east": 90.0, "south": 180.0, "west": -90.0}
    step = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
    table = ("_planks", "_slab", "_carpet", "_fence", "minecraft:piston_head")

    def chair(x, y, z):
        """The way one sits on the stair at (x, y, z), or None: a stair's low step with nothing over it, room before it (open floor, a
        carpet, a desk or a table: the legs go under it), a floor under it."""
        c = cells.get((x, y, z))
        if c is None or not c[0].endswith("_stairs") or c[1].get("half") != "bottom" or c[1].get("shape") != "straight":
            return None
        over = cells.get((x, y + 1, z))
        if over is not None and not (over[0].endswith("_trapdoor") and over[1].get("open") == "true") or (x, y + 2, z) in cells:
            return None
        f = step[c[1]["facing"]]
        front = cells.get((x - f[0], y, z - f[1]))
        if front is not None and not any(front[0].endswith(t) for t in table):
            return None
        if (x, y - 1, z) not in cells:
            return None
        return looks[c[1]["facing"]]

    for x, z in site.get("lobby_seats", []):
        yaw = chair(x, street, z)
        assert yaw is not None, ("not a chair", (x, street, z))
        agent((x, street, z), HexCcgSeat=L.Tag(L.BYTE, 1), HexCcgYaw=L.Tag(L.INT, int(yaw)))
    boss = site.get("boss_office")
    for floor in range(1, site["floors"]):
        y = street + floor * site["storey"]
        seats = []
        for (x, yy, z) in sorted(cells):
            if yy != y:
                continue
            yaw = chair(x, y, z)
            if yaw is None:
                continue
            if boss and floor == site["floors"] - 1 and boss[0] <= x <= boss[2] and boss[1] <= z <= boss[3]:
                continue
            # A desk's chair first: a stair with a desk (planks) one or two blocks before it.
            f = step[cells[(x, y, z)][1]["facing"]]
            desk = any(cells.get((x - k * f[0], y, z - k * f[1]), ("", {}))[0].endswith("_planks") for k in (1, 2))
            seats.append(((x, y, z), yaw, desk))
        desks = [s for s in seats if s[2]] or seats
        # Each floor its own three (the floors are alike): the first by the floor's number, then each as far as can be from those taken.
        chosen = [desks[(floor * 7) % len(desks)]]
        while len(chosen) < min(site["seats"], len(desks)):
            free = [s for s in desks if s not in chosen]
            chosen.append(max(free, key=lambda s: min((s[0][0] - c[0][0]) ** 2 + (s[0][2] - c[0][2]) ** 2 for c in chosen)))
        for spot, yaw, _ in chosen:
            agent(spot, HexCcgSeat=L.Tag(L.BYTE, 1), HexCcgYaw=L.Tag(L.INT, int(yaw)))
        # By the lift, looking down the hall (south).
        agent(exact(site["hall"][0], y, site["hall"][1]), HexCcgPost=L.Tag(L.BYTE, 1), HexCcgYaw=L.Tag(L.INT, 0))
    # 4.1.51 (the owner chose "Restaurant staff humans"): where the restaurant's four villagers stood, the mod's humans (each its own
    # home number: its own look and name); they keep indoors (server/HumanBrain, by the building's map).
    for k, (x, y, z) in enumerate(staff):
        spot = standing(stands, x, y, z, 1)
        assert spot is not None and spot[1] == y and spot not in taken, ("the staff's spot", (x, y, z), spot)
        people.append(("hexkagune:human", spot, k, {}))
        taken.append(spot)
    return people


def site_loop(site, streets, placed, middle):
    """4.0.2: the patrol round the mansion: its loop (the owner's x, z) walked every 14 blocks, each stop the nearest open spot to it."""
    x0, x1, z0, z1 = site["loop"]
    x0, x1 = placed(x0), placed(x1)
    corners = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
    stops = []
    for (ax, az), (bx, bz) in zip(corners, corners[1:] + corners[:1]):
        n = max(1, round(math.hypot(bx - ax, bz - az) / 14))
        for k in range(n):
            stops.append((ax + (bx - ax) * k / n, az + (bz - az) * k / n))
    route = []
    for sx, sz in stops:
        near = min(streets, key=lambda p: (p[0] - sx) ** 2 + (p[2] - sz) ** 2)
        if (near[0] - sx) ** 2 + (near[2] - sz) ** 2 <= 64 and near not in route:
            route.append(near)
    return route


def floor_spots(stands, roofed, width, d, h, street):
    out = []
    for x in range(1, width - 1):
        for z in range(1, d - 1):
            for y in range(max(1, street - 2), min(h - 2, street + 3)):
                if stands(x, y, z) and roofed(x, y, z):
                    out.append((x, y, z))
                    break
    return out


def spread(spots, taken, middle, radius):
    """The spot within {@code radius} of the middle farthest from every one taken (the nearest the middle first)."""
    near = [p for p in spots if math.hypot(p[0] - middle[0], p[2] - middle[2]) <= radius and p not in taken]
    if not near:
        near = [p for p in spots if p not in taken] or [middle]
    if not taken:
        return min(near, key=lambda p: math.hypot(p[0] - middle[0], p[2] - middle[2]))
    return max(near, key=lambda p: min((p[0] - t[0]) ** 2 + (p[2] - t[2]) ** 2 for t in taken))


def splits(n):
    count = max(1, math.ceil(n / TILE))
    return [round(i * n / count) for i in range(count + 1)]


def write_tiles(site, out, entities, people, middle, width, depth, height, loot, rng, hanging=()):
    xs, zs = splits(width), splits(depth)
    folder = DATA / "structures" / site["id"]
    folder.mkdir(parents=True, exist_ok=True)
    for old in folder.glob("*.nbt"):
        old.unlink()

    def tile_of(v, edges):
        for i in range(len(edges) - 1):
            if edges[i] <= v < edges[i + 1]:
                return i
        raise ValueError(v)

    pieces = {(i, j): {} for i in range(len(xs) - 1) for j in range(len(zs) - 1)}
    nbts = {k: {} for k in pieces}
    counter = collections.Counter()
    for (x, y, z), c in out.items():
        key = (tile_of(x, xs), tile_of(z, zs))
        local = (x - xs[key[0]], y, z - zs[key[1]])
        pieces[key][local] = c
        if (x, y, z) in entities and not (site.get("city") and entities[(x, y, z)][0] in NO_ENTITY):
            kind, inner = entities[(x, y, z)]
            given = None
            if loot and kind in CONTAINERS:
                counter[kind] += 1
                if counter[kind] % site["loot_every"] == 1 or site["loot_every"] == 1:
                    given = loot
            nbts[key][local] = block_entity(kind, inner, given, rng.getrandbits(62))
    ents = {k: [] for k in pieces}
    for kind, (px, py, pz), home, extra in people:
        key = (tile_of(px, xs), tile_of(pz, zs))
        lx, lz = px - xs[key[0]], pz - zs[key[1]]
        forge = {"HexVillage": L.compound(CentreX=L.Tag(L.INT, middle[0] - px), CentreY=L.Tag(L.INT, middle[1] - py),
                                          CentreZ=L.Tag(L.INT, middle[2] - pz), Home=L.Tag(L.INT, home), Map=L.Tag(L.INT, site["map"]))}
        forge.update(extra)
        nbt = L.compound(id=L.s(kind), PersistenceRequired=L.Tag(L.BYTE, 1), Rotation=L.floats(0.0, 0.0), Motion=L.doubles(0.0, 0.0, 0.0),
                         OnGround=L.Tag(L.BYTE, 1), ForgeData=L.compound(**forge))
        ents[key].append({"blockPos": L.ints(lx, py, lz), "pos": L.doubles(lx + 0.5, float(py), lz + 0.5), "nbt": nbt})
    # 4.1.51: what hangs on its walls, each at its block's middle (the game hangs a painting or a frame in the block its position is in).
    for (hx, hy, hz), nbt in hanging:
        key = (tile_of(hx, xs), tile_of(hz, zs))
        lx, lz = hx - xs[key[0]], hz - zs[key[1]]
        tag = dict(nbt, TileX=L.Tag(L.INT, lx), TileY=L.Tag(L.INT, hy), TileZ=L.Tag(L.INT, lz))
        ents[key].append({"blockPos": L.ints(lx, hy, lz), "pos": L.doubles(lx + 0.5, hy + 0.5, lz + 0.5), "nbt": L.compound(**tag)})
    for (i, j), cellmap in sorted(pieces.items()):
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
        if not palette:
            palette.append({"Name": L.s("minecraft:air")})
        L.write(folder / f"t_{i}_{j}.nbt", {
            "DataVersion": L.Tag(L.INT, DATA_VERSION),
            "size": L.ints(xs[i + 1] - xs[i], height, zs[j + 1] - zs[j]),
            "palette": L.Tag(L.LIST, (L.COMPOUND, palette)),
            "blocks": L.Tag(L.LIST, (L.COMPOUND, blocks)),
            "entities": L.Tag(L.LIST, (L.COMPOUND, ents[(i, j)])),
        })
    return xs, zs


def java(results):
    def rows(points, middle):
        return ";".join("%d,%d,%d" % (p[0] - middle[0], p[1] - middle[1], p[2] - middle[2]) for p in points) or "0,0,0"

    blocks = []
    for r in results:
        s, middle = r["site"], r["middle"]
        rel = [(x - middle[0], z - middle[2]) for x, _, z in r["streets"] + r["route"]] or [(0, 0)]
        perimeter = max(max(abs(x) for x, _ in rel), max(abs(z) for _, z in rel)) + 2
        xs, zs = r["tiles"]
        blocks.append(f'''        new Site("{s["id"]}", {s["map"]}, {r["width"]}, {r["depth"]}, {r["height"]}, {r["ground"]}, {middle[0]}, {middle[2]},
            edges("{",".join(map(str, xs))}"), edges("{",".join(map(str, zs))}"), {perimeter},
            points("{rows(r["streets"], middle)}"),
            points("{rows(r["route"], middle)}"),
            points("{rows(r["homes"], middle)}"))''')
    joined = ",\n".join(blocks)
    JAVA.write_text(f'''package com.hex.kagune.control.village;

/**
 * 4.0.0 (The Great Exploration): the owner's new places (4.1.4: and the CCG building; pure; written by
 * {{@code tools/exploration_structures.py}} from their schematics: do not edit by hand). Each one's size and ground (the template's frame: x east, y up, z south, as it lies unturned), the
 * edges of its tiles (data/hexkagune/structures/&lt;id&gt;/t_&lt;i&gt;_&lt;j&gt;.nbt), its middle at street level, and its map relative to that
 * middle: where its people may stand in the open (every third block), its patrol (in order round the middle), its homes (by number), and
 * how far out its perimeter is. Its people carry its {{@link Site#map}} number in their HexVillage data.
 */
public final class SiteMaps {{
    /** One place. */
    public record Site(String id, int map, int width, int depth, int height, int ground, int middleX, int middleZ, int[] xEdges, int[] zEdges,
                       int perimeter, int[][] streets, int[][] route, int[][] homes) {{
        /** Its tiles across (x) and down (z). */
        public int across() {{
            return this.xEdges.length - 1;
        }}

        public int down() {{
            return this.zEdges.length - 1;
        }}

        /** Its tile (i, j)'s template id. */
        public String tile(int i, int j) {{
            return "hexkagune:" + this.id + "/t_" + i + "_" + j;
        }}
    }}

    public static final Site[] ALL = {{
{joined}
    }};

    private SiteMaps() {{
    }}

    /** The place with {{@code id}}, or null. */
    public static Site of(String id) {{
        for (Site site : ALL) {{
            if (site.id().equals(id)) {{
                return site;
            }}
        }}
        return null;
    }}

    /** The place whose people carry map number {{@code map}}, or null. */
    public static Site byMap(int map) {{
        for (Site site : ALL) {{
            if (site.map() == map) {{
                return site;
            }}
        }}
        return RetiredSites.byMap(map); // 4.1.40: the old towns' people, in worlds made before 4.1.36
    }}

    /**
     * The least corner ({{x, z}}) of a {{@code width}} x {{@code depth}} footprint (x 0 .. width - 1, z 0 .. depth - 1) turned {{@code turns}}
     * quarter turns clockwise about its corner, as the game turns a template ({{@link VillageRules#turn}}).
     */
    public static int[] turnedLeast(int width, int depth, int turns) {{
        return switch (Math.floorMod(turns, 4)) {{
            case 1 -> new int[] {{-(depth - 1), 0}};
            case 2 -> new int[] {{-(width - 1), -(depth - 1)}};
            case 3 -> new int[] {{0, -(width - 1)}};
            default -> new int[] {{0, 0}};
        }};
    }}

    private static int[] edges(String text) {{
        String[] parts = text.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {{
            out[i] = Integer.parseInt(parts[i]);
        }}
        return out;
    }}

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


def preview(r, path):
    from PIL import Image, ImageDraw
    w, d = r["width"], r["depth"]
    scale = 3
    img = Image.new("RGB", (w * scale, d * scale), (20, 20, 20))
    px = img.load()
    for (x, z), y in r["top"].items():
        n = r["cells"][(x, y, z)][0]
        base = (70, 140, 60) if n in GROUNDS or "grass" in n else (60, 90, 200) if "water" in n else (160, 150, 140)
        f = 0.55 + 0.45 * y / r["height"]
        for a in range(scale):
            for b in range(scale):
                px[x * scale + a, z * scale + b] = tuple(int(c * f) for c in base)
    draw = ImageDraw.Draw(img)
    for p in r["streets"]:
        draw.point((p[0] * scale + 1, p[2] * scale + 1), fill=(240, 240, 120))
    for i, p in enumerate(r["route"]):
        draw.ellipse((p[0] * scale - 3, p[2] * scale - 3, p[0] * scale + 3, p[2] * scale + 3), outline=(80, 160, 255))
    colours = {"hexkagune:human": (255, 210, 90), "hexkagune:ccg_investigator": (90, 160, 255), "hexkagune:ghoul": (220, 40, 40),
               "hexkagune:noro": (240, 240, 240), "hexkagune:ayato": (140, 90, 220)}
    for kind, p, _, _ in r["people"]:
        draw.rectangle((p[0] * scale - 3, p[2] * scale - 3, p[0] * scale + 3, p[2] * scale + 3), fill=colours.get(kind, (255, 255, 255)))
    m = r["middle"]
    draw.line((m[0] * scale - 6, m[2] * scale, m[0] * scale + 6, m[2] * scale), fill=(255, 255, 255))
    draw.line((m[0] * scale, m[2] * scale - 6, m[0] * scale, m[2] * scale + 6), fill=(255, 255, 255))
    img.save(path)


def main():
    registry = None
    if "--blocks" in sys.argv:
        registry = Registry(sys.argv[sys.argv.index("--blocks") + 1])
    ground, y0 = city_structure.make()
    assert ground - y0 == CITY_GROUND, (ground, y0)
    results = [build(site, registry) for site in SITES]
    java(results)
    for r in results:
        people = collections.Counter(p[0] for p in r["people"])
        print(f'{r["site"]["id"]}: {r["width"]} x {r["height"]} x {r["depth"]}, ground {r["ground"]}, {r["blocks"]} blocks, tiles '
              f'{len(r["tiles"][0]) - 1}x{len(r["tiles"][1]) - 1}, {len(r["streets"])} street spots, {len(r["route"])} patrol spots, '
              f'{len(r["homes"])} homes, people {dict(people)}')
        if "--preview" in sys.argv:
            out = Path(sys.argv[sys.argv.index("--preview") + 1])
            out.mkdir(parents=True, exist_ok=True)
            preview(r, out / f'{r["site"]["id"]}.png')


if __name__ == "__main__":
    main()
