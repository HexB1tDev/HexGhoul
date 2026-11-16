package com.hex.kagune.control;

import com.hex.kagune.control.kitchen.Dish;
import com.hex.kagune.control.kitchen.Dishes;
import com.hex.kagune.control.kitchen.Menu;
import com.hex.kagune.control.npc.HumanRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Home Living (1.3.40): the ghoul kitchen. Every dish in the generated menu has its icon, model, name, line and recipe on disk, matching
 * the table; no two recipes are the same; the ghoul meat and human meat in each are what its recipe says; bowls and bottles come back;
 * the effects are real ones; the kitchen's own effects have icons and names; ghouls can eat every dish; and The Human's rules.
 */
public final class KitchenTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();
    static final Set<String> VANILLA_EFFECTS = Set.of("speed", "haste", "strength", "jump_boost", "regeneration", "resistance",
        "fire_resistance", "water_breathing", "night_vision", "absorption");

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) throws IOException {
        table();
        files();
        rules();
        human();
        cookbook();
        homeCooking();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Kitchen tests: " + passed + " checks passed");
    }

    static void table() {
        check(Menu.DISHES.length >= 60, "a ton of food: " + Menu.DISHES.length);
        Set<String> ids = new HashSet<>();
        Map<String, String> recipes = new HashMap<>();
        Map<String, Integer> ghoul = new HashMap<>();
        ghoul.put("hexkagune:ghoul_meat", 1);
        Set<String> human = new HashSet<>(Set.of("hexkagune:human_meat"));
        Set<String> chapters = new HashSet<>(Arrays.asList(Menu.CHAPTERS));
        for (Dish dish : Menu.DISHES) {
            check(ids.add(dish.id()), dish.id() + " once");
            check(Dishes.byId(dish.id()) == dish, dish.id() + " looked up");
            check(chapters.contains(dish.chapter()), dish.id() + " in a chapter of the book");
            check(dish.nutrition() >= 1 && dish.nutrition() <= 20 && dish.saturation() > 0.0F && dish.saturation() <= 1.2F, dish.id() + " fills sanely");
            check(dish.makes() >= 1 && dish.makes() <= 8, dish.id() + " makes a sane number");
            check((dish.kind() == Dish.Kind.BOWL) == "minecraft:bowl".equals(dish.container()), dish.id() + ": a bowl dish comes back as a bowl");
            check((dish.kind() == Dish.Kind.DRINK) == "minecraft:glass_bottle".equals(dish.container()), dish.id() + ": a drink gives its bottle back");
            for (Dish.Effect effect : dish.effects()) {
                check(Dishes.kitchenEffect(effect.key()) || VANILLA_EFFECTS.contains(effect.key()), dish.id() + ": " + effect.key() + " is an effect");
                check(effect.ticks() >= 20 && effect.ticks() <= 12000 && effect.amplifier() >= 0 && effect.amplifier() <= 2, dish.id() + ": sane effect");
            }
            String own = "hexkagune:" + dish.id();
            if (dish.recipe() == null) {
                check(dish.id().equals("human_meat"), dish.id() + ": only human meat has no recipe (it comes off a human)");
            } else {
                String[] parts = dish.recipe().ingredients();
                check(parts.length >= 1 && parts.length <= 9, dish.id() + ": fits a crafting grid");
                String[] sorted = parts.clone();
                Arrays.sort(sorted);
                String key = String.join(",", sorted) + (dish.recipe().cooking() ? "@fire" : "");
                check(!recipes.containsKey(key), dish.id() + " and " + recipes.get(key) + " share a recipe");
                recipes.put(key, dish.id());
                int pieces = 0;
                boolean anyHuman = false;
                for (String part : parts) {
                    check(part.startsWith("minecraft:") || ids.contains(part.substring("hexkagune:".length())) || part.equals("hexkagune:ghoul_meat"),
                        dish.id() + ": " + part + " is made before it in the book (or vanilla's)");
                    pieces += ghoul.getOrDefault(part, 0);
                    anyHuman |= human.contains(part);
                }
                check(pieces % dish.makes() == 0, dish.id() + ": every serving holds whole pieces of ghoul meat");
                check(dish.ghoulMeat() == pieces / dish.makes(), dish.id() + ": ghoul meat as the recipe says (" + dish.ghoulMeat() + ")");
                check(dish.humanMeat() == anyHuman, dish.id() + ": human meat as the recipe says");
                if (dish.kind() == Dish.Kind.DRINK) {
                    check(!Arrays.asList(parts).contains("hexkagune:blood_bottle"), dish.id() + ": no bottle made from a bottle that comes back");
                }
            }
            if (dish.ghoulMeat() > 0) {
                ghoul.put(own, dish.ghoulMeat());
            }
            if (dish.humanMeat()) {
                human.add(own);
            }
            check(dish.feedsGrowth() == dish.ghoulMeat() > 0, dish.id() + ": only ghoul meat grows a ghoul");
        }
        check(!Dishes.byId("human_meat").feedsGrowth() && !Dishes.byId("cooked_human_meat").feedsGrowth(), "human meat gives no growth");
        check(Dishes.byId("cooked_ghoul_meat").ghoulMeat() == 1 && Dishes.byId("rc_ramen").ghoulMeat() == 1, "ghoul meat counted through broth");
        check(Dishes.byId("kakuja_burger").ghoulMeat() == 1 && Dishes.byId("kakuja_burger").humanMeat(), "both meats in the burger");
        int withGhoul = 0;
        for (Dish dish : Menu.DISHES) {
            withGhoul += dish.ghoulMeat() > 0 ? 1 : 0;
        }
        check(withGhoul >= 15, "plenty made with ghoul meat: " + withGhoul);
        for (String chapter : Menu.CHAPTERS) {
            check(Dishes.chapter(chapter).size() >= 5, chapter + " has a few dishes");
        }
    }

    static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    static void files() throws IOException {
        String lang = read("src/main/resources/assets/hexkagune/lang/en_us.json");
        String dishes = read("src/main/resources/data/hexkagune/tags/items/dishes.json");
        String ghoulTag = read("src/main/resources/data/hexkagune/tags/items/contains_ghoul_meat.json");
        String humanTag = read("src/main/resources/data/hexkagune/tags/items/contains_human_meat.json");
        String ghoulFood = read("src/main/resources/data/hexkagune/tags/items/ghoul_food.json");
        check(ghoulFood.contains("\"#hexkagune:dishes\""), "a ghoul can eat every dish");
        for (Dish dish : Menu.DISHES) {
            String own = "\"hexkagune:" + dish.id() + "\"";
            check(Files.isRegularFile(Path.of("src/main/resources/assets/hexkagune/textures/item/" + dish.id() + ".png")), dish.id() + " has its icon");
            String model = read("src/main/resources/assets/hexkagune/models/item/" + dish.id() + ".json");
            check(model.contains("\"hexkagune:item/" + dish.id() + "\""), dish.id() + "'s model shows its icon");
            check(lang.contains("\"item.hexkagune." + dish.id() + "\":") && lang.contains("\"item.hexkagune." + dish.id() + ".about\":"),
                dish.id() + " has a name and a line");
            check(dishes.contains(own), dish.id() + " is a dish");
            check(ghoulTag.contains(own) == dish.ghoulMeat() > 0, dish.id() + ": tagged with ghoul meat as it holds it");
            check(humanTag.contains(own) == dish.humanMeat(), dish.id() + ": tagged with human meat as it holds it");
            if (dish.recipe() != null) {
                String recipe = read("src/main/resources/data/hexkagune/recipes/kitchen/" + dish.id() + ".json");
                check(recipe.contains("\"hexkagune:" + dish.id() + "\""), dish.id() + "'s recipe makes it");
                for (String part : dish.recipe().ingredients()) {
                    check(recipe.contains("\"" + part + "\""), dish.id() + "'s recipe takes " + part);
                }
            }
        }
        for (Menu.Kitchen effect : Menu.EFFECTS) {
            check(Files.isRegularFile(Path.of("src/main/resources/assets/hexkagune/textures/mob_effect/" + effect.key() + ".png")), effect.key() + " has its icon");
            check(lang.contains("\"effect.hexkagune." + effect.key() + "\":"), effect.key() + " has a name");
        }
        check(lang.contains("\"entity.hexkagune.human\":"), "The Human has a name");
        String menu = read("src/main/java/com/hex/kagune/control/kitchen/Menu.java").toLowerCase();
        // 4.0.0: the owner asked for Ayato Kirishima and Aogiri Tree by name (The Great Exploration): no longer kept out.
        for (String banned : new String[] {"kaneki", "furuta", "takatsuki", "yoshimura"}) {
            check(!menu.contains(banned) && !lang.toLowerCase().contains(banned), "no " + banned + " in the kitchen");
        }
        // 2.0.2 (the owner: "stop limiting it to plains ... including some from biomes o plenty"); 4.1.9 (the owner: "it should be able to
        // spawn literally in any biome ... It should also be fully compatible with Biomes of Plenty"): the cafe, the tower, the library and
        // the villages stand in any biome of the overworld, a mod's too, and never in a wet one (judged in code: NpcTests.dryLand).
        for (String place : new String[] {"anteiku", "library", "hotel"}) { // 4.1.72: the hotel, the Small City gone
            String json = read("src/main/resources/data/hexkagune/worldgen/structure/" + place + ".json");
            check(json.contains("\"biomes\": \"#hexkagune:overworld\""), place + ": any biome of the overworld, Biomes O' Plenty's too");
        }
    }

    static void rules() {
        check(Dishes.warmed(10) == 15 && Dishes.warmed(1) >= 1 && Dishes.warmed(-5) == -5 && Dishes.warmed(0) == 0, "content warms gains only");
        check(Dishes.replenishTicks(0) == Dishes.REPLENISH_TICKS && Dishes.replenishTicks(1) < Dishes.replenishTicks(0), "replenished: faster a level");
        check(Dishes.time(1200).equals("1:00") && Dishes.time(1800).equals("1:30") && Dishes.time(100).equals("0:05"), "time text");
        check(Dishes.level(0).isEmpty() && Dishes.level(1).equals(" II"), "level text");
        check(Dishes.CLEARED.length == 3 && Dishes.CLEAR_SIGHT_FAR > 1.0F, "clear sight clears");
    }

    /**
     * 1.3.42: the partner cooking the cookbook's dishes at home: every recipe can be made from exactly what it lists; nothing short is
     * made; what a recipe needs first is made on the way (two deep); searing needs the stove; buckets and bottles come back; which dish
     * it picks (dishes with effects first, never the last one twice while there is another, never the last of something twice in a
     * row); a few a day, a while apart; its words; the dishes' names.
     */
    static void homeCooking() throws IOException {
        int all = Integer.MAX_VALUE;
        check(com.hex.kagune.control.family.Kitchen.remainder("minecraft:water_bucket").equals("minecraft:bucket")
            && com.hex.kagune.control.family.Kitchen.remainder("minecraft:milk_bucket").equals("minecraft:bucket")
            && com.hex.kagune.control.family.Kitchen.remainder("minecraft:honey_bottle").equals("minecraft:glass_bottle")
            && "minecraft:glass_bottle".equals(com.hex.kagune.control.family.Kitchen.remainder("hexkagune:blood_bottle"))
            && com.hex.kagune.control.family.Kitchen.remainder("hexkagune:rc_broth") == null
            && com.hex.kagune.control.family.Kitchen.remainder("minecraft:bread") == null
            && com.hex.kagune.control.family.Kitchen.remainder(null) == null, "buckets and bottles come back as crafting gives them back");
        int recipes = 0;
        for (Dish dish : Menu.DISHES) {
            if (dish.recipe() == null) {
                check(com.hex.kagune.control.family.Kitchen.cooking(dish, Map.of(), all) == null, dish.id() + ": no recipe, never cooked");
                continue;
            }
            recipes++;
            Map<String, Integer> stock = new HashMap<>();
            for (String part : dish.recipe().ingredients()) {
                stock.merge(part, 1, Integer::sum);
            }
            com.hex.kagune.control.family.Kitchen.Cooking c = com.hex.kagune.control.family.Kitchen.cooking(dish, stock, all);
            check(c != null && c.uses().equals(stock) && c.dish() == dish && c.steps().size() == 1
                && c.yields().getOrDefault("hexkagune:" + dish.id(), 0) == dish.makes() && c.seared() == (dish.recipe().cooking() ? 1 : 0),
                dish.id() + ": made from exactly its recipe, " + dish.makes() + " of it");
            int back = 0;
            for (String part : dish.recipe().ingredients()) {
                back += !dish.recipe().cooking() && com.hex.kagune.control.family.Kitchen.remainder(part) != null ? 1 : 0;
            }
            int yields = 0;
            for (int n : c == null ? List.<Integer>of() : c.yields().values()) {
                yields += n;
            }
            check(c != null && yields == dish.makes() + back, dish.id() + ": what comes of it is the dish and what the recipe gives back");
            // One short of anything it can't make first: nothing.
            String first = dish.recipe().ingredients()[0];
            Map<String, Integer> less = new HashMap<>(stock);
            less.merge(first, -1, Integer::sum);
            Dish prep = Dishes.byItem(first);
            if (prep == null || prep.recipe() == null) {
                check(com.hex.kagune.control.family.Kitchen.cooking(dish, less, all) == null, dish.id() + ": one " + first + " short, not made");
            }
            if (dish.recipe().cooking()) {
                check(com.hex.kagune.control.family.Kitchen.cooking(dish, stock, 0) == null, dish.id() + ": searing needs the stove");
            }
        }
        check(recipes >= 60, "the partner can make the cookbook's recipes: " + recipes);
        // Prep two deep: a sausage roll from bread, human meat and a bottle (the mince, the bottled blood, the sausage made on the way).
        Map<String, Integer> raw = Map.of("minecraft:bread", 1, "hexkagune:human_meat", 2, "minecraft:glass_bottle", 1);
        com.hex.kagune.control.family.Kitchen.Cooking roll = com.hex.kagune.control.family.Kitchen.cooking(Dishes.byId("sausage_roll"), raw, 0);
        check(roll != null && roll.uses().equals(raw) && roll.steps().size() == 4 && roll.steps().get(3).id().equals("sausage_roll")
            && roll.yields().get("hexkagune:sausage_roll") == 1 && roll.yields().get("hexkagune:blood_sausage") == 2
            && roll.yields().get("minecraft:glass_bottle") == 1 && roll.yields().size() == 3 && roll.seared() == 0,
            "a sausage roll from raw stock: mince, blood and sausage made first; two sausages and the bottle left over: " + (roll == null ? null : roll.yields()));
        check(com.hex.kagune.control.family.Kitchen.cooking(Dishes.byId("sausage_roll"), Map.of("minecraft:bread", 1, "hexkagune:human_meat", 1,
            "minecraft:glass_bottle", 1), 0) == null, "not enough human meat for the mince and the blood: no sausage roll");
        Map<String, Integer> burger = Map.of("minecraft:bread", 1, "hexkagune:ghoul_meat", 1, "hexkagune:human_meat", 1, "minecraft:beetroot", 1);
        com.hex.kagune.control.family.Kitchen.Cooking kakuja = com.hex.kagune.control.family.Kitchen.cooking(Dishes.byId("kakuja_burger"), burger, 2);
        check(kakuja != null && kakuja.seared() == 2 && kakuja.uses().equals(burger)
            && com.hex.kagune.control.family.Kitchen.cooking(Dishes.byId("kakuja_burger"), burger, 1) == null
            && com.hex.kagune.control.family.Kitchen.cooking(Dishes.byId("kakuja_burger"), burger, 0) == null,
            "both patties seared at the stove first; not without fuel for both");
        Map<String, Integer> some = new HashMap<>(Map.of("minecraft:bread", 1, "hexkagune:cooked_human_meat", 2));
        com.hex.kagune.control.family.Kitchen.Cooking sandwich = com.hex.kagune.control.family.Kitchen.cooking(Dishes.byId("seared_sandwich"), some, 0);
        check(sandwich != null && sandwich.uses().equals(Map.of("minecraft:bread", 1, "hexkagune:cooked_human_meat", 1)), "takes only what it needs");
        // Whom it cooks for, and what.
        check(!com.hex.kagune.control.family.Kitchen.cooks(Dishes.byId("crimson_curry"), false)
            && !com.hex.kagune.control.family.Kitchen.cooks(Dishes.byId("seared_sandwich"), false)
            && com.hex.kagune.control.family.Kitchen.cooks(Dishes.byId("crimson_curry"), true)
            && com.hex.kagune.control.family.Kitchen.cooks(Dishes.byId("cooked_human_meat"), true)
            && !com.hex.kagune.control.family.Kitchen.cooks(Dishes.byId("human_mince"), true)
            && !com.hex.kagune.control.family.Kitchen.cooks(Dishes.byId("human_meat"), true),
            "the cookbook only for a ghoul spouse; the mince only on the way to another dish; raw meat isn't a recipe");
        check(com.hex.kagune.control.family.Kitchen.worth(Dishes.byId("blood_bottle")) > com.hex.kagune.control.family.Kitchen.worth(Dishes.byId("cooked_human_meat"))
            && com.hex.kagune.control.family.Kitchen.worth(Dishes.byId("rc_ramen")) > com.hex.kagune.control.family.Kitchen.worth(Dishes.byId("seared_sandwich")),
            "a dish that does something beats one that only fills; more effects beat fewer");
        // Choosing.
        Map<String, Integer> meat = Map.of("hexkagune:human_meat", 3, "minecraft:glass_bottle", 2);
        com.hex.kagune.control.family.Kitchen.Cooking best = com.hex.kagune.control.family.Kitchen.choose(meat, 4, true, null, false, 0.0);
        check(best != null && best.dish().id().equals("blood_bottle"), "the best first: bottled blood before plain seared meat: " + (best == null ? null : best.dish().id()));
        check(com.hex.kagune.control.family.Kitchen.choose(meat, 4, false, null, false, 0.0) == null, "nothing from the cookbook for a human spouse");
        com.hex.kagune.control.family.Kitchen.Cooking again = com.hex.kagune.control.family.Kitchen.choose(meat, 4, true, "blood_bottle", false, 0.0);
        check(again != null && !again.dish().id().equals("blood_bottle"), "not the same dish twice in a row while there's another");
        Map<String, Integer> one = Map.of("minecraft:bread", 1, "hexkagune:cooked_human_meat", 1);
        com.hex.kagune.control.family.Kitchen.Cooking only = com.hex.kagune.control.family.Kitchen.choose(one, 0, true, "seared_sandwich", false, 0.5);
        check(only != null && only.dish().id().equals("seared_sandwich"), "the same dish again when it's the only one there is");
        check(only != null && only.empties(one) && com.hex.kagune.control.family.Kitchen.choose(one, 0, true, null, true, 0.5) == null,
            "it took the last of something last time: not the last of anything again this time");
        check(com.hex.kagune.control.family.Kitchen.passes(one, 0, true, null, true) && !com.hex.kagune.control.family.Kitchen.passes(one, 0, true, null, false)
            && !com.hex.kagune.control.family.Kitchen.passes(Map.of(), 0, true, null, true),
            "a meal's turn passes only when every dish would take the last of something again (then the next turn may)");
        Map<String, Integer> two = Map.of("minecraft:bread", 2, "hexkagune:cooked_human_meat", 2);
        com.hex.kagune.control.family.Kitchen.Cooking more = com.hex.kagune.control.family.Kitchen.choose(two, 0, true, null, true, 0.5);
        check(more != null && !more.empties(two) && !com.hex.kagune.control.family.Kitchen.passes(two, 0, true, null, true),
            "leaving some behind is fine after taking the last of something");
        boolean variety = false;
        Map<String, Integer> pantry = new HashMap<>();
        for (Dish dish : Menu.DISHES) {
            if (dish.recipe() != null) {
                for (String part : dish.recipe().ingredients()) {
                    pantry.merge(part, 4, Integer::sum);
                }
            }
        }
        String seen = null;
        for (double r = 0.0; r < 1.0; r += 0.1) {
            com.hex.kagune.control.family.Kitchen.Cooking c = com.hex.kagune.control.family.Kitchen.choose(pantry, all, true, null, false, r);
            check(c != null && c.dish().effects().length > 0, "a full pantry: a dish with effects");
            if (c != null && seen != null && !seen.equals(c.dish().id())) {
                variety = true;
            }
            seen = c == null ? seen : c.dish().id();
        }
        check(variety, "a full pantry: not always the same dish");
        // A few a day, a while apart.
        long now = 50000L;
        check(com.hex.kagune.control.family.Kitchen.dishDue(0, Long.MIN_VALUE, now)
            && !com.hex.kagune.control.family.Kitchen.dishDue(com.hex.kagune.control.family.Kitchen.DISHES_A_DAY, Long.MIN_VALUE, now)
            && !com.hex.kagune.control.family.Kitchen.dishDue(1, now - 100L, now)
            && com.hex.kagune.control.family.Kitchen.dishDue(1, now - com.hex.kagune.control.family.Kitchen.DISH_GAP, now)
            && com.hex.kagune.control.family.Kitchen.dishDue(1, now + 500L, now), "a few a day, a while apart (time set back: due again)");
        // The meal it keeps can hold a dish.
        int[] took = new int[1];
        List<com.hex.kagune.control.family.Chores.Pile> meal = com.hex.kagune.control.family.Kitchen.add(new ArrayList<>(), "hexkagune:crimson_curry", 2, took);
        check(took[0] == 2 && com.hex.kagune.control.family.Kitchen.load(com.hex.kagune.control.family.Kitchen.save(meal)).equals(meal), "a dish kept as the meal saves and loads");
        // Names and words.
        check(com.hex.kagune.control.family.Kitchen.name("hexkagune:crimson_curry").equals("Crimson Curry")
            && com.hex.kagune.control.family.Kitchen.name("minecraft:cooked_beef").equals("cooked beef")
            && com.hex.kagune.control.family.Kitchen.name("hexkagune:ghoul_cookbook").equals("ghoul cookbook")
            && Dishes.byItem("minecraft:crimson_curry") == null && Dishes.byItem("hexkagune:crimson_curry") == Dishes.byId("crimson_curry"),
            "a dish by its own name; anything else by its id");
        check(Menu.NAMES.length == Menu.DISHES.length, "every dish has its name");
        String lang = Files.readString(Path.of("src/main/resources/assets/hexkagune/lang/en_us.json"));
        for (Dish dish : Menu.DISHES) {
            check(lang.contains("\"item.hexkagune." + dish.id() + "\": \"" + Dishes.name(dish.id()) + "\""), dish.id() + ": its name as the lang file has it");
        }
        for (String key : new String[] {"touka", "ken", "eto"}) {
            for (int seed = 0; seed < 4; seed++) {
                String[] lines = {com.hex.kagune.control.family.Kitchen.made(key, "Crimson Curry", true, seed),
                    com.hex.kagune.control.family.Kitchen.made(key, "Crimson Curry", false, seed),
                    com.hex.kagune.control.family.Kitchen.servedDish(key, "Crimson Curry", seed % 2 == 0, seed),
                    com.hex.kagune.control.family.Kitchen.madeWhileOut(key, "Crimson Curry", seed)};
                for (String line : lines) {
                    String low = line.toLowerCase(java.util.Locale.ROOT);
                    check(line.contains("Crimson Curry") && !low.contains("kaneki") && !low.contains("takatsuki") && !low.contains("yoshimura")
                        && !low.contains("aogiri"), key + ": says what it made: " + line);
                }
                check(com.hex.kagune.control.family.Kitchen.made(key, "X", false, seed).contains("chest")
                    && com.hex.kagune.control.family.Kitchen.madeWhileOut(key, "X", seed).contains("chest")
                    && com.hex.kagune.control.family.Kitchen.servedDish(key, "X", true, seed).contains("chest")
                    && !com.hex.kagune.control.family.Kitchen.servedDish(key, "X", false, seed).contains("chest"), key + ": says where the rest is");
            }
        }
    }

    /** 1.3.41: Touka's cookbook: its spreads, its pictures, its words. */
    static void cookbook() throws IOException {
        List<com.hex.kagune.control.kitchen.Cookbook.Spread> spreads = com.hex.kagune.control.kitchen.Cookbook.spreads();
        check(spreads.size() == 2 + Menu.CHAPTERS.length + Menu.DISHES.length, "a cover, a spread a chapter, one a dish, the effects");
        check(spreads.get(0).kind() == com.hex.kagune.control.kitchen.Cookbook.Kind.COVER, "it opens on its cover");
        check(spreads.get(spreads.size() - 1).kind() == com.hex.kagune.control.kitchen.Cookbook.Kind.EFFECTS, "the effects at the back");
        for (String chapter : Menu.CHAPTERS) {
            int at = com.hex.kagune.control.kitchen.Cookbook.chapterAt(chapter);
            check(at > 0 && spreads.get(at).chapter().equals(chapter) && spreads.get(at).kind() == com.hex.kagune.control.kitchen.Cookbook.Kind.CHAPTER,
                chapter + " has its spread");
            check(Files.isRegularFile(Path.of("src/main/resources/assets/hexkagune/textures/gui/cookbook/" + chapter + ".png")), chapter + " has its picture");
        }
        for (Dish dish : Menu.DISHES) {
            int at = com.hex.kagune.control.kitchen.Cookbook.dishAt(dish.id());
            check(at > com.hex.kagune.control.kitchen.Cookbook.chapterAt(dish.chapter()) && spreads.get(at).dish() == dish, dish.id() + " has its page in its chapter");
        }
        check(com.hex.kagune.control.kitchen.Cookbook.dishAt("nothing") == -1, "no page for no dish");
        check(com.hex.kagune.control.kitchen.Cookbook.clamp(-4) == 0 && com.hex.kagune.control.kitchen.Cookbook.clamp(9999) == spreads.size() - 1, "kept in the book");
        check(com.hex.kagune.control.kitchen.Cookbook.pageNumber(0) == 1 && com.hex.kagune.control.kitchen.Cookbook.pageNumber(3) == 7, "page numbers");
        check(com.hex.kagune.control.kitchen.Cookbook.affordable(20, 2) && !com.hex.kagune.control.kitchen.Cookbook.affordable(19, 64)
            && !com.hex.kagune.control.kitchen.Cookbook.affordable(100, 1), "two cocoa beans, once she likes you");
        List<String> lines = com.hex.kagune.control.kitchen.Cookbook.wrap("one two three four five six", 9, String::length);
        check(lines.equals(List.of("one two", "three", "four five", "six")), "wrapped at the spaces: " + lines);
        check(com.hex.kagune.control.kitchen.Cookbook.wrap("enormousword small", 4, String::length).get(0).equals("enormousword"), "a long word alone");
        check(com.hex.kagune.control.kitchen.Cookbook.wrap("", 10, String::length).isEmpty(), "nothing to wrap");
        for (int seed = 0; seed < 6; seed++) {
            check(!com.hex.kagune.control.kitchen.Cookbook.price(seed).isEmpty() && !com.hex.kagune.control.kitchen.Cookbook.given(seed).isEmpty(), "her lines");
        }
        for (String picture : new String[] {"cover", "backdrop"}) {
            check(Files.isRegularFile(Path.of("src/main/resources/assets/hexkagune/textures/gui/cookbook/" + picture + ".png")), picture + " drawn");
        }
        check(Files.isRegularFile(Path.of("src/main/resources/assets/hexkagune/textures/gui/cookbook.png")), "the book drawn");
        check(Files.isRegularFile(Path.of("src/main/resources/assets/hexkagune/textures/item/ghoul_cookbook.png")), "the book's icon");
        String lang = read("src/main/resources/assets/hexkagune/lang/en_us.json");
        // 4.1.66: the book's words are its view's now (CookbookView, the quest menu's look).
        String screen = read("src/main/java/com/hex/kagune/control/client/CookbookView.java");
        java.util.regex.Matcher keys = java.util.regex.Pattern.compile("\"(book\\.hexkagune\\.cookbook\\.[a-z_]+)\"").matcher(screen);
        int found = 0;
        while (keys.find()) {
            found++;
            check(lang.contains("\"" + keys.group(1) + "\":"), "the book's words: " + keys.group(1));
        }
        check(found >= 10, "the book's words checked: " + found);
        for (String chapter : Menu.CHAPTERS) {
            check(lang.contains("\"book.hexkagune.cookbook.chapter." + chapter + ".intro\":"), chapter + " has its few words");
        }
        for (Menu.Kitchen effect : Menu.EFFECTS) {
            check(lang.contains("\"effect.hexkagune." + effect.key() + ".description\":"), effect.key() + " explained");
        }
        String server = read("src/main/java/com/hex/kagune/control/server/EtoServer.java");
        check(server.contains("withCookbook(player, npc, page)") && server.contains("\"cookbook\""), "Touka offers it on her first page");
        check(server.contains("Persona.TOUKA") && server.contains("Cookbook.affordable("), "only Touka, only for the beans");
    }

    static void human() {
        check(HumanRules.LOOKS.length == 9, "vanilla's nine default skins");
        Set<String> skins = new HashSet<>();
        for (int i = 0; i < HumanRules.LOOKS.length * 2; i++) {
            skins.add(HumanRules.skin(i));
            check(HumanRules.skin(i).startsWith("textures/entity/player/" + (HumanRules.slim(i) ? "slim/" : "wide/")), "skin " + i + " on its arms");
        }
        check(skins.size() == 18, "eighteen looks");
        for (int looting = 0; looting <= 3; looting++) {
            check(HumanRules.meat(0.0, looting) == 1 + looting && HumanRules.meat(0.999, looting) == 3 + looting, "meat with looting " + looting);
        }
        check(HumanRules.notices(15.0, true) && !HumanRules.notices(15.0, false) && HumanRules.notices(5.0, false), "it knows a ghoul that shows itself");
        // It runs at about a player's walk: a sprinting ghoul catches it (a mob's speed goes as the square of it).
        double run = Math.pow(HumanRules.SPEED * HumanRules.RUN, 2.0);
        double walk = 0.98 * 0.1;
        check(run > walk * 0.85 && run < walk * 1.3 * 0.98, "it runs about as fast as a player walks, slower than a sprint: " + run);
        double[] to = HumanRules.fleeStep(0.0, 0.0, 5.0, 0.0, 0.0, 10.0);
        check(Math.abs(to[0] + 10.0) < 1.0E-9 && Math.abs(to[1]) < 1.0E-9, "it runs straight away");
        double[] round = HumanRules.fleeStep(0.0, 0.0, 5.0, 0.0, Math.PI / 2.0, 10.0);
        check(Math.abs(round[0]) < 1.0E-9 && Math.abs(Math.abs(round[1]) - 10.0) < 1.0E-9, "and round when it must");
        double[] same = HumanRules.fleeStep(3.0, 3.0, 3.0, 3.0, 0.0, 10.0);
        check(Double.isFinite(same[0]) && Double.isFinite(same[1]), "even from right on top of it");
    }
}
