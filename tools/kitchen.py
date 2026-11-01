#!/usr/bin/env python3
"""Home Living (1.3.40): the ghoul kitchen, from one table to everything the game reads.

    python3 tools/kitchen.py              write it all into the mod
    python3 tools/kitchen.py --preview D  only the icons (and a contact sheet at 8x) into D, to look at

What a dish is lives here and only here (MENU below): its name, what it is made of, how many it makes, how much it fills, the effects it
gives and for how long, the chapter of the cookbook it is in, and a line about it. From that this writes:
  - its icon (tools/kitchen_art.py), textures/item/<id>.png, and its item model,
  - its crafting recipe (shapeless) in data/hexkagune/recipes, and the meats' cooking (furnace, smoker, campfire),
  - the item tags: hexkagune:dishes (every one; ghoul food takes them all), hexkagune:contains_ghoul_meat, hexkagune:contains_human_meat,
  - its name and its line in the lang file, the four kitchen effects' names, and their 18x18 icons,
  - src/main/java/com/hex/kagune/control/kitchen/Menu.java (generated): the same table for the game (DishItem, the effects, the
    cookbook, the partner's cooking).
How much ghoul meat is in a dish is worked out from its recipe (ghoul meat, seared ghoul meat and RC broth are one piece each); a
recipe with ghoul meat in it makes one serving, so every serving holds whole pieces. The same goes for human meat (any of it).
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import kitchen_art as art  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / "hexkagune"
DATA = RES / "data" / "hexkagune"
JAVA = ROOT / "src" / "main" / "java" / "com" / "hex" / "kagune" / "control" / "kitchen" / "Menu.java"
LANG = ASSETS / "lang" / "en_us.json"

M = "minecraft:"
H = "hexkagune:"

# The kitchen's own effects: (key, name, colour, what it does: for the lang file's description and the book).
EFFECTS = [
    ("refreshed", "Refreshed", 0x5AB4F0, "Your RC flows back on its own, a little every second."),
    ("replenished", "Replenished", 0xE63C50, "Wounds close slowly on their own, at no cost to the flesh reserve."),
    ("content", "Content", 0xFAD250, "Calm and happy: the frenzy ebbs, the urges quiet, and the ones you love warm to you faster."),
    ("clear_sight", "Clear Sight", 0x3C96DC, "Blindness, darkness and nausea fall away, and you see far through water, lava and snow."),
]
VANILLA_EFFECTS = {"speed", "haste", "strength", "jump_boost", "regeneration", "resistance", "fire_resistance", "water_breathing",
                   "night_vision", "absorption"}

# The kitchen's other lines (tooltips, The Human).
TEXT = {
    "entity.hexkagune.human": "Human",
    "tooltip.hexkagune.kitchen.ghoul_meat": "§4Contains ghoul meat",
    "tooltip.hexkagune.kitchen.ghoul_meat_pieces": "§4Contains ghoul meat (%s pieces)",
    "tooltip.hexkagune.kitchen.grows": "§8Devoured, you'd be at: strength %s, RC lasts %s",
    "tooltip.hexkagune.kitchen.human_meat": "§cContains human meat",
    "tooltip.hexkagune.kitchen.effect": "§9%s%s (%s)",
}

CHAPTERS = [
    ("basics", "The Basics"),
    ("breads", "Breads and Sandwiches"),
    ("bowls", "Bowls"),
    ("plates", "The Grill and the Plate"),
    ("sweets", "Sweets"),
    ("drinks", "Drinks"),
]


def dish(id, name, chapter, recipe, art_fn, food, effects=(), makes=1, kind="food", container=None, about=""):
    return dict(id=id, name=name, chapter=chapter, recipe=recipe, art=art_fn, food=food, effects=list(effects), makes=makes,
                kind=kind, container=container, about=about)


B = M + "bowl"
G = M + "glass_bottle"
HM, CHM, GM, CGM, MINCE, BLOOD, SUGARC, BROTH, SAUS = (H + "human_meat", H + "cooked_human_meat", H + "ghoul_meat",
                                                     H + "cooked_ghoul_meat", H + "human_mince", H + "blood_bottle",
                                                     H + "blood_sugar", H + "rc_broth", H + "blood_sausage")

# Effects: (key, seconds, level). Food: (nutrition, saturation modifier), as vanilla's (steak is 8, 0.8).
MENU = [
    # ------------------------------------------------------------------------------------------------ the basics
    dish("human_meat", "Human Meat", "basics", None, art.human_meat, (3, 0.3),
         about="Raw flesh, a strip of skin still on it. What a ghoul lives on."),
    dish("cooked_human_meat", "Seared Human Meat", "basics", "cook:" + HM, art.cooked_human_meat, (8, 0.8),
         about="Seared on the outside, still red within. The start of nearly every recipe here."),
    dish("cooked_ghoul_meat", "Seared Ghoul Meat", "basics", "cook:" + GM, art.cooked_ghoul_meat, (9, 0.9), [("strength", 30, 1)],
         about="Another ghoul's flesh, seared black. It keeps all of its RC, and so do you."),
    dish("human_mince", "Minced Human Meat", "basics", [HM], art.human_mince, (2, 0.2), makes=2,
         about="Chopped fine for buns, dumplings and anything rolled."),
    dish("blood_bottle", "Bottled Blood", "basics", [G, HM], art.blood_bottle, (3, 0.4), [("refreshed", 30, 1)], kind="drink",
         container=G, about="Pressed from the meat and corked. Cooks with it; the bottle comes back."),
    dish("blood_sugar", "Blood Sugar Cubes", "basics", [M + "sugar", BLOOD], art.blood_sugar, (1, 0.2), [("refreshed", 15, 1)],
         makes=4, kind="snack", about="Sugar set with blood. A ghoul's coffee is never served without them."),
    dish("blood_sausage", "Blood Sausage", "basics", [MINCE, MINCE, BLOOD], art.blood_sausage, (5, 0.6), [("regeneration", 5, 1)],
         makes=3, about="Mince and blood in a casing, cured until dark."),
    dish("rc_broth", "RC Broth", "basics", [B, GM, M + "water_bucket"], art.rc_broth, (6, 0.6), [("replenished", 60, 1)], kind="bowl",
         container=B, about="Ghoul meat simmered down until the water itself is dense with RC."),
    # ------------------------------------------------------------------------------------------------ breads and sandwiches
    dish("seared_sandwich", "Seared Sandwich", "breads", [M + "bread", CHM], art.seared_sandwich, (11, 0.8), [("content", 60, 1)],
         about="Bread round seared meat. Simple, and it fills."),
    dish("club_sandwich", "Club Sandwich", "breads", [M + "bread", M + "bread", CHM, M + "egg", M + "beetroot"], art.club_sandwich,
         (9, 0.8), [("content", 90, 1), ("refreshed", 60, 1)], makes=2, about="Stacked three high and cut in two, a pick through each."),
    dish("blood_toast", "Blood Toast", "breads", [M + "bread", BLOOD, M + "sugar"], art.blood_toast, (5, 0.6), [("refreshed", 60, 1)],
         makes=2, about="Toast spread thick with sweetened blood. Breakfast."),
    dish("meat_bun", "Steamed Meat Bun", "breads", [M + "wheat", M + "wheat", MINCE, M + "sugar"], art.meat_bun, (6, 0.6),
         [("replenished", 30, 1)], makes=2, about="Soft white dough round a sweet meat filling, steamed."),
    dish("anteiku_burger", "Anteiku Burger", "breads", [M + "bread", CHM, M + "beetroot", M + "potato"], art.anteiku_burger, (14, 0.9),
         [("content", 120, 1), ("strength", 45, 1)], about="The café's own: a thick patty, beet and greens in a toasted bun."),
    dish("sausage_roll", "Sausage Roll", "breads", [M + "bread", SAUS], art.sausage_roll, (10, 0.8), [("speed", 60, 1)],
         about="Blood sausage baked in a golden roll. Eaten on the run."),
    dish("ghoul_sandwich", "Ghoul Sandwich", "breads", [M + "bread", CGM], art.ghoul_sandwich, (13, 0.9),
         [("strength", 60, 1), ("content", 60, 1)], about="Seared ghoul meat between two slices. It tastes of iron and power."),
    dish("kakuja_burger", "Kakuja Burger", "breads", [M + "bread", CGM, CHM, M + "beetroot"], art.kakuja_burger, (18, 1.0),
         [("strength", 60, 2), ("resistance", 60, 1)], about="Two patties, one human, one ghoul. Not for the faint of heart."),
    dish("flatbread_wrap", "Flatbread Wrap", "breads", [M + "wheat", M + "wheat", M + "wheat", CHM, M + "sweet_berries"],
         art.flatbread_wrap, (8, 0.7), [("speed", 45, 1), ("content", 45, 1)], makes=2,
         about="Meat and berries rolled in a soft flatbread."),
    # ------------------------------------------------------------------------------------------------ bowls
    dish("human_stew", "Human Stew", "bowls", [B, CHM, M + "carrot", M + "potato", M + "brown_mushroom"], art.red_stew, (12, 0.9),
         [("replenished", 90, 1)], kind="bowl", container=B, about="A slow, thick stew. It warms the whole house."),
    dish("crimson_risotto", "Crimson Risotto", "bowls", [B, MINCE, M + "wheat", M + "red_mushroom", M + "beetroot"],
         art.crimson_risotto, (11, 0.8), [("content", 90, 1), ("clear_sight", 90, 1)], kind="bowl", container=B,
         about="Grain cooked down in beet and blood until it blushes."),
    dish("black_risotto", "Black Risotto", "bowls", [B, GM, M + "ink_sac", M + "wheat", M + "brown_mushroom"], art.black_risotto,
         (12, 0.9), [("night_vision", 180, 1), ("clear_sight", 120, 1)], kind="bowl", container=B,
         about="Squid ink and ghoul meat. The dark stops being dark."),
    dish("meatballs", "Meatballs in Red Sauce", "bowls", [B, MINCE, MINCE, M + "beetroot", M + "wheat"], art.meatballs, (12, 0.8),
         [("strength", 30, 1), ("replenished", 60, 1)], kind="bowl", container=B, about="Rolled by hand, simmered in a red sauce."),
    dish("blood_soup", "Beet and Blood Soup", "bowls", [B, M + "beetroot", M + "beetroot", BLOOD], art.blood_soup, (9, 0.7),
         [("refreshed", 90, 1), ("regeneration", 10, 1)], kind="bowl", container=B, about="Deep red, a swirl of cream, a sprig on top."),
    dish("midnight_ramen", "Midnight Ramen", "bowls", [B, M + "wheat", M + "egg", CHM, M + "dried_kelp"], art.midnight_ramen,
         (13, 0.9), [("refreshed", 90, 1), ("night_vision", 120, 1)], kind="bowl", container=B,
         about="Noodles, a soft egg and seared meat, for the hours when the city sleeps."),
    dish("rc_ramen", "RC Ramen", "bowls", [BROTH, M + "wheat", M + "egg", M + "dried_kelp"], art.rc_ramen, (15, 1.0),
         [("strength", 60, 1), ("refreshed", 120, 1), ("content", 60, 1)], kind="bowl", container=B,
         about="Noodles in RC broth. The bowl glows faintly red."),
    dish("crimson_curry", "Crimson Curry", "bowls", [B, CHM, M + "potato", M + "carrot", M + "blaze_powder"], art.crimson_curry,
         (13, 0.9), [("fire_resistance", 180, 1), ("strength", 30, 1)], kind="bowl", container=B,
         about="Hot enough that fire stops hurting for a while."),
    dish("centipede_soba", "Centipede Soba", "bowls", [B, M + "wheat", GM, M + "dried_kelp", M + "brown_mushroom"],
         art.centipede_soba, (13, 0.9), [("speed", 90, 1), ("clear_sight", 90, 1)], kind="bowl", container=B,
         about="Buckwheat noodles, a long strip of ghoul meat curled on top like something with too many legs."),
    dish("red_hunger_stew", "Red Hunger Stew", "bowls", [B, GM, HM, M + "beetroot", M + "potato"], art.red_hunger_stew, (15, 1.0),
         [("absorption", 60, 1), ("replenished", 120, 1)], kind="bowl", container=B,
         about="Both meats, raw, stewed together. It quiets even a kakuja's hunger."),
    dish("tide_broth", "Tide Broth", "bowls", [B, M + "dried_kelp", M + "cod", MINCE], art.tide_broth, (9, 0.7),
         [("water_breathing", 180, 1)], kind="bowl", container=B, about="Kelp, fish and meat. The sea lets you stay a while."),
    dish("pumpkin_soup", "Pumpkin Soup", "bowls", [B, M + "pumpkin", MINCE, M + "carrot"], art.pumpkin_soup, (10, 0.8),
         [("replenished", 60, 1), ("content", 60, 1)], kind="bowl", container=B, about="Smooth, sweet and orange, meat stirred through."),
    dish("forest_stew", "Forest Blood Stew", "bowls", [B, M + "brown_mushroom", M + "red_mushroom", BLOOD], art.forest_stew,
         (8, 0.7), [("refreshed", 60, 1), ("clear_sight", 60, 1)], kind="bowl", container=B,
         about="Mushrooms from the wood, simmered in blood."),
    dish("crimson_pudding", "Crimson Pudding", "bowls", [B, M + "milk_bucket", M + "egg", M + "sugar", BLOOD], art.crimson_pudding,
         (8, 0.8), [("content", 120, 1)], kind="bowl", container=B, about="A wobbling custard under a blood caramel."),
    # ------------------------------------------------------------------------------------------------ the grill and the plate
    dish("meat_skewer", "Meat Skewer", "plates", [M + "stick", CHM, M + "beetroot"], art.meat_skewer, (6, 0.7), [("speed", 60, 1)],
         makes=2, about="Seared meat and beet on a stick, off the grill."),
    dish("kagune_kebab", "Kagune Kebab", "plates", [M + "stick", CGM, M + "red_mushroom", MINCE], art.kagune_kebab, (12, 0.9),
         [("strength", 30, 2), ("speed", 60, 1)], about="Ghoul meat grilled hard on a skewer. It bites back."),
    dish("steak_dinner", "Steak Dinner", "plates", [CHM, M + "baked_potato", M + "carrot"], art.steak_dinner, (15, 1.0),
         [("strength", 60, 1), ("replenished", 60, 1)], about="A proper plate: the cut, a potato, a carrot."),
    dish("glazed_ribs", "Honey-Glazed Ribs", "plates", [CHM, M + "honey_bottle", M + "bone"], art.glazed_ribs, (12, 0.9),
         [("regeneration", 10, 1), ("content", 90, 1)], about="Sticky, sweet and falling off the bone."),
    dish("human_jerky", "Human Jerky", "plates", [HM, M + "sugar"], art.human_jerky, (3, 0.5), [("speed", 30, 1)], makes=3,
         kind="snack", about="Dried in strips with a little sugar. It keeps for weeks in a coat pocket."),
    dish("human_tartare", "Human Tartare", "plates", [HM, M + "egg", M + "beetroot"], art.human_tartare, (9, 0.7),
         [("clear_sight", 120, 1)], about="Raw, chopped fine, a yolk on top. For a ghoul who likes it honest."),
    dish("kakuhou_tartare", "Kakuhou Tartare", "plates", [GM, M + "egg", M + "beetroot", M + "red_mushroom"], art.kakuhou_tartare,
         (11, 0.9), [("strength", 45, 2), ("clear_sight", 120, 1)], about="Raw ghoul meat, the RC still alive in it."),
    dish("akai_sushi", "Akai Sushi", "plates", [M + "dried_kelp", HM, M + "wheat"], art.akai_sushi, (5, 0.6), [("clear_sight", 60, 1)],
         makes=2, about="Red meat rolled in grain and kelp, cut into rounds."),
    dish("kuro_sushi", "Kuro Sushi", "plates", [M + "dried_kelp", GM, M + "wheat", M + "ink_sac"], art.kuro_sushi, (9, 0.8),
         [("night_vision", 120, 1), ("clear_sight", 60, 1)], about="Ghoul meat draped over pressed grain, tied with kelp."),
    dish("dumplings", "Steamed Dumplings", "plates", [M + "wheat", MINCE, M + "carrot", M + "brown_mushroom"], art.dumplings, (4, 0.6),
         [("content", 45, 1)], makes=3, about="Pleated by hand, steamed, three to a plate."),
    dish("spring_rolls", "Spring Rolls", "plates", [M + "wheat", MINCE, M + "carrot", M + "kelp"], art.spring_rolls, (5, 0.6),
         [("speed", 45, 1)], makes=2, about="Crisp and golden, meat and carrot inside."),
    dish("croquettes", "Croquettes", "plates", [M + "potato", MINCE, M + "bread", M + "egg"], art.croquettes, (5, 0.6),
         [("content", 60, 1)], makes=3, about="Potato and meat, crumbed and fried."),
    dish("stuffed_potato", "Stuffed Potato", "plates", [M + "baked_potato", MINCE, M + "brown_mushroom"], art.stuffed_potato,
         (10, 0.8), [("replenished", 60, 1)], about="Baked, split, and filled high."),
    dish("sunday_omelette", "Sunday Omelette", "plates", [M + "egg", M + "egg", MINCE, M + "carrot"], art.sunday_omelette, (9, 0.8),
         [("content", 90, 1)], about="Folded soft over meat and carrot, slow on a quiet morning."),
    dish("owls_omelette", "Owl's Omelette", "plates", [M + "egg", M + "egg", GM, M + "brown_mushroom"], art.owls_omelette, (11, 0.9),
         [("clear_sight", 120, 1), ("strength", 45, 1)], about="An old recipe, they say, from a one-eyed ghoul's kitchen."),
    dish("morning_plate", "Morning Plate", "plates", [CHM, M + "egg", M + "bread", M + "potato"], art.morning_plate, (14, 0.9),
         [("refreshed", 90, 1), ("content", 60, 1)], about="An egg, strips of seared meat, toast. The day starts here."),
    dish("meatloaf", "Meatloaf", "plates", [MINCE, MINCE, M + "egg", M + "bread", M + "beetroot"], art.meatloaf, (8, 0.8),
         [("replenished", 60, 1)], makes=2, about="Baked in a tin with a red glaze, cut thick."),
    dish("roast_haunch", "Roast Haunch", "plates", [CHM, CHM, M + "apple", M + "sweet_berries"], art.roast_haunch, (18, 1.0),
         [("strength", 60, 1), ("absorption", 60, 1), ("content", 60, 1)], about="A whole roast with an apple and berries. A feast."),
    dish("meat_pie", "Meat Pie", "plates", [M + "wheat", M + "wheat", M + "wheat", M + "egg", MINCE], art.meat_pie, (12, 0.9),
         [("content", 60, 1), ("replenished", 60, 1)], about="Golden crust, rich filling, steam through the slits."),
    dish("ghouls_pie", "Ghoul's Pie", "plates", [M + "wheat", M + "wheat", M + "wheat", M + "egg", GM], art.ghouls_pie, (13, 0.9),
         [("strength", 60, 1), ("resistance", 60, 1)], about="The same crust, a darker filling. It glows through the slits."),
    # ------------------------------------------------------------------------------------------------ sweets
    dish("kakuja_cake", "Kakuja Cake", "sweets", [M + "wheat", M + "wheat", M + "sugar", M + "egg", M + "milk_bucket", GM],
         art.kakuja_cake, (10, 0.8), [("absorption", 90, 1), ("strength", 60, 1), ("content", 60, 1)],
         about="A layer of ghoul meat between the sponge. Birthdays only."),
    dish("velvet_cake", "Blood Velvet Cake", "sweets", [M + "wheat", M + "wheat", M + "sugar", M + "egg", M + "milk_bucket", BLOOD],
         art.velvet_cake, (7, 0.8), [("content", 120, 1), ("regeneration", 10, 1)], makes=2,
         about="Red as it sounds, white frosting, a berry on top."),
    dish("blood_cookies", "Blood Cookies", "sweets", [M + "wheat", M + "wheat", M + "cocoa_beans", BLOOD], art.blood_cookies, (2, 0.2),
         [("content", 30, 1)], makes=8, kind="snack", about="Cocoa and blood, baked by the tray."),
    dish("blood_jelly", "Blood Jelly", "sweets", [BLOOD, M + "slime_ball", M + "sugar"], art.blood_jelly, (5, 0.6),
         [("jump_boost", 90, 1)], about="It wobbles. So will you, a little higher than usual."),
    dish("ghoul_mochi", "Ghoul Mochi", "sweets", [M + "wheat", M + "sugar", GM], art.ghoul_mochi, (8, 0.7), [("speed", 90, 1)],
         kind="snack", about="Soft white rounds, a dark heart in each."),
    dish("berry_tart", "Berry Blood Tart", "sweets", [M + "wheat", M + "sweet_berries", M + "sweet_berries", M + "sugar", BLOOD],
         art.berry_tart, (6, 0.7), [("refreshed", 60, 1), ("content", 60, 1)], makes=2, about="A short crust, a blood curd, berries."),
    dish("blood_pumpkin_pie", "Blood Pumpkin Pie", "sweets", [M + "pumpkin", M + "sugar", M + "egg", BLOOD], art.blood_pumpkin_pie,
         (10, 0.8), [("content", 90, 1), ("replenished", 60, 1)], about="An autumn pie with a red swirl through it."),
    dish("blood_truffles", "Blood Truffles", "sweets", [M + "cocoa_beans", M + "cocoa_beans", M + "sugar", BLOOD], art.truffles,
         (3, 0.4), [("content", 60, 1), ("haste", 60, 1)], makes=4, kind="snack", about="Dark chocolate round a drop of blood."),
    dish("golden_heart", "Golden Heart", "sweets", [M + "golden_apple", GM, M + "honey_bottle"], art.golden_heart, (12, 1.2),
         [("absorption", 120, 2), ("regeneration", 10, 2), ("strength", 120, 1)],
         about="A ghoul's heart set in gold and honey. The finest thing in this book."),
    dish("glow_sorbet", "Glow Berry Sorbet", "sweets", [M + "glow_berries", M + "glow_berries", M + "snowball", BLOOD],
         art.glow_sorbet, (4, 0.5), [("night_vision", 120, 1), ("clear_sight", 120, 1)], makes=2,
         about="Cold, bright, and it lights the way home."),
    # ------------------------------------------------------------------------------------------------ drinks
    dish("anteiku_blend", "Anteiku Blend", "drinks", [G, M + "cocoa_beans", M + "cocoa_beans", SUGARC], art.anteiku_blend, (4, 0.5),
         [("refreshed", 120, 1), ("haste", 60, 1), ("content", 60, 1)], kind="drink", container=G,
         about="The café's coffee, a blood sugar cube in it. The one thing a ghoul can drink like anyone else."),
    dish("blood_latte", "Blood Latte", "drinks", [G, HM, M + "milk_bucket", M + "cocoa_beans"], art.blood_latte, (5, 0.6),
         [("refreshed", 90, 1), ("content", 90, 1)], kind="drink", container=G, about="Milk, coffee and blood, a heart on top."),
    dish("crimson_tea", "Crimson Tea", "drinks", [G, M + "sweet_berries", HM, M + "sugar"], art.crimson_tea, (4, 0.5),
         [("clear_sight", 120, 1), ("refreshed", 60, 1)], kind="drink", container=G, about="Berries steeped with a little blood."),
    dish("rc_tonic", "RC Tonic", "drinks", [G, GM, M + "glow_berries", M + "sugar"], art.rc_tonic, (6, 0.6),
         [("refreshed", 60, 2), ("speed", 30, 2)], kind="drink", container=G, about="Ghoul meat pressed into a bottle. It fizzes."),
    dish("kakuja_shot", "Kakuja Shot", "drinks", [G, GM, M + "blaze_powder"], art.kakuja_shot, (5, 0.5),
         [("strength", 30, 2), ("fire_resistance", 120, 1)], kind="drink", container=G, about="One swallow. It burns all the way down."),
]

# Pieces of ghoul meat in one of each (dishes made from these add theirs up).
GHOUL_PIECES = {GM: 1}
HUMAN = {HM}
FASTS = {"snack": 16, "drink": 32, "bowl": 32, "food": 32}


def worked_out():
    """Each dish's pieces of ghoul meat per serving, and whether it holds human meat: from what it is made of."""
    pieces = dict(GHOUL_PIECES)
    human = set(HUMAN)
    by_id = {H + d["id"]: d for d in MENU}
    changed = True
    while changed:
        changed = False
        for d in MENU:
            key = H + d["id"]
            r = d["recipe"]
            parts = [r[5:]] if isinstance(r, str) else (r or [])
            total = sum(pieces.get(p, 0) for p in parts)
            if total and total % d["makes"]:
                raise SystemExit(f"{d['id']}: {total} pieces of ghoul meat don't split into {d['makes']} servings")
            p = total // d["makes"] if total else 0
            if pieces.get(key, 0) != p and key not in GHOUL_PIECES:
                pieces[key] = p
                changed = True
            if any(x in human for x in parts) and key not in human:
                human.add(key)
                changed = True
    del by_id
    return pieces, human


def check():
    ids = [d["id"] for d in MENU]
    assert len(ids) == len(set(ids)), "a dish twice"
    seen = {}
    known = {H + i for i in ids} | {GM}
    for d in MENU:
        r = d["recipe"]
        if isinstance(r, list):
            assert 1 <= len(r) <= 9, d["id"]
            for part in r:
                assert part.startswith(M) or part in known, (d["id"], part)
            key = tuple(sorted(r))
            assert key not in seen, f"{d['id']} and {seen.get(key)} have the same recipe"
            seen[key] = d["id"]
            if d["kind"] == "bowl":
                assert B in r or BROTH in r, d["id"] + ": a bowl dish without a bowl"
            if d["kind"] == "drink":
                assert G in r, d["id"] + ": a drink without its bottle"
                assert BLOOD not in r, d["id"] + ": a bottled drink from bottled blood would make a bottle"
        for (key, seconds, level) in d["effects"]:
            assert key in VANILLA_EFFECTS or key in {e[0] for e in EFFECTS}, (d["id"], key)
            assert 1 <= level <= 3 and 1 <= seconds <= 600, d["id"]
        assert d["kind"] in FASTS
        assert (d["kind"] in ("bowl", "drink")) == (d["container"] is not None), d["id"]
    for word in ("kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"):
        for d in MENU:
            assert word not in (d["id"] + d["name"] + d["about"]).lower(), (d["id"], word)


# ---------------------------------------------------------------------------------------------------- writers

def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def icons(items, effects):
    items.mkdir(parents=True, exist_ok=True)
    effects.mkdir(parents=True, exist_ok=True)
    for d in MENU:
        d["art"]().save(items / f"{d['id']}.png")
    for (key, *_rest) in EFFECTS:
        art.effect_icon(key).save(effects / f"{key}.png")


def recipes():
    out = DATA / "recipes" / "kitchen"
    if out.exists():
        for old in out.glob("*.json"):
            old.unlink()
    for d in MENU:
        r = d["recipe"]
        if r is None:
            continue
        if isinstance(r, str):
            source = r[5:]
            for (kind, time, suffix) in (("smelting", 200, ""), ("smoking", 100, "_from_smoking"), ("campfire_cooking", 600, "_from_campfire")):
                write_json(out / f"{d['id']}{suffix}.json", {"type": M + kind, "category": "food", "ingredient": {"item": source},
                                                             "result": H + d["id"], "experience": 0.35, "cookingtime": time})
            continue
        write_json(out / f"{d['id']}.json", {"type": M + "crafting_shapeless", "category": "misc",
                                            "ingredients": [{"item": p} for p in r],
                                            "result": {"item": H + d["id"], "count": d["makes"]}})


def models():
    for d in MENU:
        write_json(ASSETS / "models" / "item" / f"{d['id']}.json",
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"hexkagune:item/{d['id']}"}})


def tags(pieces, human):
    items = DATA / "tags" / "items"
    write_json(items / "dishes.json", {"replace": False, "values": [H + d["id"] for d in MENU]})
    write_json(items / "contains_ghoul_meat.json", {"replace": False, "values": [GM] + [H + d["id"] for d in MENU if pieces.get(H + d["id"])]})
    write_json(items / "contains_human_meat.json", {"replace": False, "values": [H + d["id"] for d in MENU if H + d["id"] in human]})


def lang():
    data = json.loads(LANG.read_text(encoding="utf-8"))
    for d in MENU:
        data[f"item.hexkagune.{d['id']}"] = d["name"]
        data[f"item.hexkagune.{d['id']}.about"] = "§7§o" + d["about"]
    for (key, name, _colour, what) in EFFECTS:
        data[f"effect.hexkagune.{key}"] = name
        data[f"effect.hexkagune.{key}.description"] = what
    for (key, name) in CHAPTERS:
        data[f"book.hexkagune.cookbook.chapter.{key}"] = name
    data.update(TEXT)
    LANG.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def java(pieces, human):
    lines = []
    for d in MENU:
        r = d["recipe"]
        if r is None:
            recipe = "null"
        elif isinstance(r, str):
            recipe = 'Dish.cooked("' + r[5:] + '")'
        else:
            recipe = "Dish.of(" + ", ".join('"' + p + '"' for p in r) + ")"
        effects = ", ".join(f'new Dish.Effect("{k}", {s * 20}, {lvl - 1})' for (k, s, lvl) in d["effects"])
        container = "null" if d["container"] is None else '"' + d["container"] + '"'
        lines.append(f'        new Dish("{d["id"]}", "{d["chapter"]}", Dish.Kind.{d["kind"].upper()}, {d["food"][0]}, {d["food"][1]}F, '
                     f'{pieces.get(H + d["id"], 0)}, {str(H + d["id"] in human).lower()}, {container}, {d["makes"]}, {recipe},\n'
                     f'            new Dish.Effect[] {{{effects}}}),')
    chapters = ", ".join('"' + c[0] + '"' for c in CHAPTERS)
    effects = ", ".join(f'new Kitchen("{k}", 0x{c:06X})' for (k, _n, c, _w) in EFFECTS)
    text = f'''package com.hex.kagune.control.kitchen;

/**
 * GENERATED by tools/kitchen.py from its MENU table: do not edit by hand (run {{@code python3 tools/kitchen.py}}). Home Living (1.3.40):
 * every dish of the ghoul kitchen, in the cookbook's order: what it is, how much it fills, the pieces of ghoul meat in a serving and
 * whether it holds human meat (worked out from its recipe), what it is served in, how many a recipe makes, the recipe, the effects.
 */
public final class Menu {{
    private Menu() {{
    }}

    /** The cookbook's chapters, in order. */
    public static final String[] CHAPTERS = {{{chapters}}};

    /** The kitchen's own effects (their keys and colours). */
    public static final Kitchen[] EFFECTS = {{{effects}}};

    public static final Dish[] DISHES = {{
{chr(10).join(lines)}
    }};

    /** One of the kitchen's own effects. */
    public record Kitchen(String key, int colour) {{
    }}
}}
'''
    JAVA.parent.mkdir(parents=True, exist_ok=True)
    JAVA.write_text(text, encoding="utf-8")


def sheet(directory, out):
    from PIL import Image, ImageDraw
    files = [directory / f"{d['id']}.png" for d in MENU] + sorted(directory.glob("effect_*.png"))
    scale, cols = 6, 8
    cell = 16 * scale + 34
    rows = (len(files) + cols - 1) // cols
    img = Image.new("RGB", (cols * cell, rows * (cell + 6)), (139, 139, 139))
    draw = ImageDraw.Draw(img)
    for k, f in enumerate(files):
        icon = Image.open(f).convert("RGBA")
        big = icon.resize((icon.width * scale, icon.height * scale), Image.NEAREST)
        x = (k % cols) * cell + 14
        y = (k // cols) * (cell + 6) + 6
        draw.rectangle([x - 4, y - 4, x + 16 * scale + 4, y + 16 * scale + 4], fill=(198, 198, 198))
        img.paste(big, (x, y), big)
        draw.text((x - 6, y + 16 * scale + 8), f.stem[:20], fill=(0, 0, 0))
    img.save(out)


def main():
    check()
    pieces, human = worked_out()
    if len(sys.argv) > 2 and sys.argv[1] == "--preview":
        directory = Path(sys.argv[2])
        icons(directory, directory)
        for (key, *_r) in EFFECTS:
            (directory / f"{key}.png").rename(directory / f"effect_{key}.png")
        sheet(directory, directory / "sheet.png")
        print(f"{len(MENU)} icons and a sheet in {directory}")
        return
    icons(ASSETS / "textures" / "item", ASSETS / "textures" / "mob_effect")
    recipes()
    models()
    tags(pieces, human)
    lang()
    java(pieces, human)
    print(f"{len(MENU)} dishes ({sum(1 for d in MENU if pieces.get(H + d['id']))} with ghoul meat, "
          f"{sum(1 for d in MENU if H + d['id'] in human)} with human meat), {len(EFFECTS)} effects")


if __name__ == "__main__":
    main()
