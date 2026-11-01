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
        for (String banned : new String[] {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"}) {
            check(!menu.contains(banned) && !lang.toLowerCase().contains(banned), "no " + banned + " in the kitchen");
        }
        String plains = read("src/main/resources/data/hexkagune/tags/worldgen/biome/has_structure/anteiku.json");
        String tower = read("src/main/resources/data/hexkagune/tags/worldgen/biome/has_structure/cyber_tower.json");
        String library = read("src/main/resources/data/hexkagune/tags/worldgen/biome/has_structure/library.json");
        check(plains.contains("minecraft:plains") && !plains.contains("forest") && !plains.contains("is_overworld"), "the cafe on plains only");
        check(tower.contains("minecraft:plains") && !tower.contains("forest") && !tower.contains("is_overworld"), "the tower on plains only");
        check(library.contains("minecraft:plains") && library.contains("minecraft:forest") && !library.contains("taiga")
            && !library.contains("savanna") && !library.contains("meadow"), "the library in forests and plains only");
    }

    static void rules() {
        check(Dishes.warmed(10) == 15 && Dishes.warmed(1) >= 1 && Dishes.warmed(-5) == -5 && Dishes.warmed(0) == 0, "content warms gains only");
        check(Dishes.replenishTicks(0) == Dishes.REPLENISH_TICKS && Dishes.replenishTicks(1) < Dishes.replenishTicks(0), "replenished: faster a level");
        check(Dishes.time(1200).equals("1:00") && Dishes.time(1800).equals("1:30") && Dishes.time(100).equals("0:05"), "time text");
        check(Dishes.level(0).isEmpty() && Dishes.level(1).equals(" II"), "level text");
        check(Dishes.CLEARED.length == 3 && Dishes.CLEAR_SIGHT_FAR > 1.0F, "clear sight clears");
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
        String screen = read("src/main/java/com/hex/kagune/control/client/CookbookScreen.java");
        java.util.regex.Matcher keys = java.util.regex.Pattern.compile("\"(book\\.hexkagune\\.cookbook\\.[a-z_]+)\"").matcher(screen);
        int found = 0;
        while (keys.find()) {
            found++;
            check(lang.contains("\"" + keys.group(1) + "\":"), "the book's words: " + keys.group(1));
        }
        check(found >= 10, "the screen's words checked: " + found);
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
