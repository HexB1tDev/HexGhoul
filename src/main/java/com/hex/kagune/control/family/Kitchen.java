package com.hex.kagune.control.family;

import com.hex.kagune.control.kitchen.Dish;
import com.hex.kagune.control.kitchen.Dishes;
import com.hex.kagune.control.kitchen.Menu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.3.31: the spouse's real help about the house (pure, tested; the world's side is {@code HomeKitchen}): what raw food cooks into, what a
 * furnace smelts what into, what burns and for how long, which crops are ripe and what they give, and the meal it keeps for its spouse
 * (cooked for a human; for a ghoul, who can't keep human food down, the raw cuts set aside), handed over when they come home.
 */
public final class Kitchen {
    /** How much it cooks or smelts at a time, at most; how many portions of a meal it keeps for them, at most. */
    public static final int BATCH = 8;
    public static final int MEAL = 6;
    /** What one item takes to cook or smelt, in halves of a coal's worth per eight (a coal does eight: sixteen halves, two an item). */
    public static final int PER_ITEM = 2;

    private static final Map<String, String> COOKED = Map.ofEntries(
        Map.entry("minecraft:beef", "minecraft:cooked_beef"), Map.entry("minecraft:porkchop", "minecraft:cooked_porkchop"),
        Map.entry("minecraft:chicken", "minecraft:cooked_chicken"), Map.entry("minecraft:mutton", "minecraft:cooked_mutton"),
        Map.entry("minecraft:rabbit", "minecraft:cooked_rabbit"), Map.entry("minecraft:cod", "minecraft:cooked_cod"),
        Map.entry("minecraft:salmon", "minecraft:cooked_salmon"), Map.entry("minecraft:potato", "minecraft:baked_potato"),
        Map.entry("minecraft:kelp", "minecraft:dried_kelp"));
    private static final Map<String, String> SMELTED = Map.ofEntries(
        Map.entry("minecraft:raw_iron", "minecraft:iron_ingot"), Map.entry("minecraft:raw_gold", "minecraft:gold_ingot"),
        Map.entry("minecraft:raw_copper", "minecraft:copper_ingot"), Map.entry("minecraft:iron_ore", "minecraft:iron_ingot"),
        Map.entry("minecraft:deepslate_iron_ore", "minecraft:iron_ingot"), Map.entry("minecraft:gold_ore", "minecraft:gold_ingot"),
        Map.entry("minecraft:deepslate_gold_ore", "minecraft:gold_ingot"), Map.entry("minecraft:copper_ore", "minecraft:copper_ingot"),
        Map.entry("minecraft:deepslate_copper_ore", "minecraft:copper_ingot"), Map.entry("minecraft:ancient_debris", "minecraft:netherite_scrap"),
        Map.entry("minecraft:sand", "minecraft:glass"), Map.entry("minecraft:red_sand", "minecraft:glass"),
        Map.entry("minecraft:cobblestone", "minecraft:stone"), Map.entry("minecraft:cobbled_deepslate", "minecraft:deepslate"),
        Map.entry("minecraft:clay_ball", "minecraft:brick"), Map.entry("minecraft:netherrack", "minecraft:nether_brick"),
        Map.entry("minecraft:wet_sponge", "minecraft:sponge"), Map.entry("minecraft:cactus", "minecraft:green_dye"));
    /** Raw cuts a ghoul eats (the mod's own Ghoul Meat too). */
    private static final java.util.Set<String> FLESH = java.util.Set.of("minecraft:beef", "minecraft:porkchop", "minecraft:chicken", "minecraft:mutton",
        "minecraft:rabbit", "minecraft:cod", "minecraft:salmon", "hexkagune:ghoul_meat");

    private Kitchen() {
    }

    /** What {@code id} (an item's registry name) cooks into, or null if it isn't raw food that cooks. */
    public static String cooked(String id) {
        return id == null ? null : COOKED.get(id);
    }

    /** What a furnace smelts {@code id} into (ores, sand, cobblestone, logs into charcoal...), or null. */
    public static String smelted(String id) {
        if (id == null) {
            return null;
        }
        String out = SMELTED.get(id);
        if (out != null) {
            return out;
        }
        String path = id.substring(id.indexOf(':') + 1);
        return id.startsWith("minecraft:") && (path.endsWith("_log") || path.endsWith("_wood")) && !path.startsWith("stripped_") && !path.contains("crimson")
            && !path.contains("warped") ? "minecraft:charcoal" : null;
    }

    /** True for raw flesh a ghoul eats (its meal is set aside raw). */
    public static boolean flesh(String id) {
        return id != null && FLESH.contains(id);
    }

    /** How much {@code id} burns for, in halves ({@link #PER_ITEM} an item): coal 16, a log or planks 3, a stick 1; 0 if it doesn't burn. */
    public static int fuel(String id) {
        if (id == null) {
            return 0;
        }
        String path = id.substring(id.indexOf(':') + 1);
        return switch (path) {
            case "coal", "charcoal" -> 16;
            case "coal_block" -> 160;
            case "blaze_rod" -> 24;
            case "dried_kelp_block" -> 40;
            case "stick" -> 1;
            default -> (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_planks")) && !path.contains("crimson") && !path.contains("warped")
                ? 3 : 0;
        };
    }

    /** True for a station that cooks ({@code smelts} false) or smelts: a furnace both; a smoker or campfire food only; a blast furnace ores. */
    public static boolean station(String blockText, boolean smelts) {
        String path = Chores.path(blockText);
        if (smelts) {
            return path.equals("furnace") || path.equals("blast_furnace");
        }
        return path.equals("furnace") || path.equals("smoker") || path.endsWith("campfire");
    }

    /** True if {@code blockText} burns without fuel (a campfire). */
    public static boolean free(String blockText) {
        return Chores.path(blockText).endsWith("campfire");
    }

    /** True if a crop's state ({@code stateText}: "Block{minecraft:wheat}[age=7]") is ripe. */
    public static boolean ripe(String stateText) {
        if (stateText == null) {
            return false;
        }
        String path = Chores.path(stateText);
        int age = stateText.indexOf("age=");
        if (age < 0) {
            return false;
        }
        int end = age + 4;
        while (end < stateText.length() && Character.isDigit(stateText.charAt(end))) {
            end++;
        }
        if (end == age + 4) {
            return false;
        }
        int value = Integer.parseInt(stateText.substring(age + 4, end));
        return switch (path) {
            case "wheat", "carrots", "potatoes" -> value >= 7;
            case "beetroots" -> value >= 3;
            default -> false;
        };
    }

    /** What a ripe crop gives once it is picked and the plant left to grow again ({@code roll}, {@code roll2} in 0..1: how much). */
    public static List<Chores.Pile> harvest(String blockText, double roll, double roll2) {
        String path = Chores.path(blockText);
        List<Chores.Pile> out = new ArrayList<>();
        int extra = (int)Math.floor(Math.max(0.0, Math.min(0.999, roll)) * 3.0);
        switch (path) {
            case "wheat" -> {
                out.add(new Chores.Pile("minecraft:wheat", 1, 64));
                if (extra > 0) {
                    out.add(new Chores.Pile("minecraft:wheat_seeds", extra, 64));
                }
            }
            case "carrots" -> out.add(new Chores.Pile("minecraft:carrot", 1 + extra, 64));
            case "potatoes" -> {
                out.add(new Chores.Pile("minecraft:potato", 1 + extra, 64));
                if (roll2 < 0.02) {
                    out.add(new Chores.Pile("minecraft:poisonous_potato", 1, 64));
                }
            }
            case "beetroots" -> {
                out.add(new Chores.Pile("minecraft:beetroot", 1, 64));
                if (extra > 0) {
                    out.add(new Chores.Pile("minecraft:beetroot_seeds", extra, 64));
                }
            }
            default -> {
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- the meal kept for them

    /** A meal as kept in its saved data ("minecraft:cooked_beef*3;minecraft:baked_potato*2"). */
    public static String save(List<Chores.Pile> meal) {
        StringBuilder out = new StringBuilder();
        for (Chores.Pile pile : meal) {
            if (pile != null && pile.count() > 0 && pile.key() != null && !pile.key().contains(";") && !pile.key().contains("*")) {
                out.append(out.length() == 0 ? "" : ";").append(pile.key()).append('*').append(pile.count());
            }
        }
        return out.toString();
    }

    /** The meal back from its saved text (a bad part left out). */
    public static List<Chores.Pile> load(String text) {
        List<Chores.Pile> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        for (String part : text.split(";")) {
            int star = part.lastIndexOf('*');
            if (star <= 0) {
                continue;
            }
            try {
                int count = Integer.parseInt(part.substring(star + 1));
                if (count > 0) {
                    out.add(new Chores.Pile(part.substring(0, star), Math.min(64, count), 64));
                }
            } catch (NumberFormatException bad) {
                // left out
            }
        }
        return out;
    }

    /** How many portions {@code meal} holds. */
    public static int portions(List<Chores.Pile> meal) {
        int n = 0;
        for (Chores.Pile pile : meal) {
            n += pile.count();
        }
        return n;
    }

    /** {@code meal} with {@code count} of {@code id} added, never past {@link #MEAL} portions in all; how many fit is the second value. */
    public static List<Chores.Pile> add(List<Chores.Pile> meal, String id, int count, int[] took) {
        List<Chores.Pile> out = new ArrayList<>(meal);
        int room = Math.max(0, MEAL - portions(meal));
        int put = Math.max(0, Math.min(room, count));
        took[0] = put;
        if (put == 0) {
            return out;
        }
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).key().equals(id)) {
                out.set(i, new Chores.Pile(id, out.get(i).count() + put, 64));
                return out;
            }
        }
        out.add(new Chores.Pile(id, put, 64));
        return out;
    }

    /** "cooked beef" for "minecraft:cooked_beef"; a dish of the cookbook by its own name ("Crimson Curry", 1.3.42). */
    public static String name(String id) {
        Dish dish = Dishes.byItem(id);
        if (dish != null) {
            return Dishes.name(dish.id());
        }
        String path = id == null ? "" : id.substring(id.indexOf(':') + 1);
        return path.replace('_', ' ');
    }

    // ---------------------------------------------------------------- 1.3.42: the cookbook's dishes, cooked at home

    /** At most this many of the cookbook's dishes a day, and at least this long between two (ticks): meals, not a factory. */
    public static final int DISHES_A_DAY = 4;
    public static final long DISH_GAP = 2400L;
    /** How far back it goes making what a recipe needs first: a sausage roll's sausage (1), and that sausage's mince and blood (2). */
    public static final int PREP_DEPTH = 2;

    /**
     * A dish it can make from what the chests hold: the dish; everything it makes on the way, in order (what a recipe needs first, the
     * dish last); what it takes out of the chests (item id to count); what comes of it all (the dish times what its recipe makes, any
     * prep left over, the buckets and bottles a recipe gives back); how many things are seared at a stove on the way.
     */
    public record Cooking(Dish dish, List<Dish> steps, Map<String, Integer> uses, Map<String, Integer> yields, int seared) {
        /** The dish's item id ("hexkagune:crimson_curry"). */
        public String item() {
            return "hexkagune:" + this.dish.id();
        }

        /**
         * True if it takes the last of something out of the chests ({@code stock}: what they held) that another of the cookbook's
         * recipes needs too: what the player may have put by for that one.
         */
        public boolean empties(Map<String, Integer> stock) {
            for (Map.Entry<String, Integer> use : this.uses.entrySet()) {
                if (stock.getOrDefault(use.getKey(), 0) <= use.getValue() && neededElsewhere(use.getKey(), this.steps)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * What a recipe gives back once {@code id} is used in it, as vanilla's crafting does: a bucket for a bucket of water, milk or lava; a
     * glass bottle for honey, dragon's breath, or one of the kitchen's bottled drinks (Bottled Blood). Null for nothing.
     */
    public static String remainder(String id) {
        if (id == null) {
            return null;
        }
        switch (id) {
            case "minecraft:water_bucket", "minecraft:milk_bucket", "minecraft:lava_bucket" -> {
                return "minecraft:bucket";
            }
            case "minecraft:honey_bottle", "minecraft:dragon_breath" -> {
                return "minecraft:glass_bottle";
            }
            default -> {
                Dish dish = Dishes.byItem(id);
                return dish != null && dish.kind() == Dish.Kind.DRINK && dish.container() != null ? dish.container() : null;
            }
        }
    }

    /**
     * How {@code dish} could be made from {@code stock} (what the chests hold: item id to count), or null if it can't: each ingredient
     * taken from what it has made on the way first, then from the chests, else made first from its own recipe (up to {@link #PREP_DEPTH}
     * deep: Minced Human Meat from Human Meat for the buns, Bottled Blood for the toast). A cooking recipe (a meat seared) needs the
     * stove: at most {@code searable} things can be seared (0: no stove, or no fuel for one).
     */
    public static Cooking cooking(Dish dish, Map<String, Integer> stock, int searable) {
        if (dish == null || dish.recipe() == null || stock == null) {
            return null;
        }
        Work work = new Work(stock);
        if (!work.make(dish, PREP_DEPTH) || work.seared > Math.max(0, searable)) {
            return null;
        }
        Map<String, Integer> uses = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> held : stock.entrySet()) {
            int used = held.getValue() - work.have.getOrDefault(held.getKey(), 0);
            if (used > 0) {
                uses.put(held.getKey(), used);
            }
        }
        Map<String, Integer> yields = new LinkedHashMap<>();
        String item = "hexkagune:" + dish.id();
        yields.put(item, work.made.getOrDefault(item, 0));
        work.made.forEach((id, n) -> {
            if (n > 0 && !id.equals(item)) {
                yields.merge(id, n, Integer::sum);
            }
        });
        work.back.forEach((id, n) -> yields.merge(id, n, Integer::sum));
        return new Cooking(dish, List.copyOf(work.steps), uses, yields, work.seared);
    }

    /** The making of one dish, as it goes: what the chests have left, what has been made (and not used yet), what came back, the steps. */
    private static final class Work {
        final Map<String, Integer> have;
        final Map<String, Integer> made = new LinkedHashMap<>();
        final Map<String, Integer> back = new LinkedHashMap<>();
        final List<Dish> steps = new ArrayList<>();
        int seared;

        Work(Map<String, Integer> stock) {
            this.have = new HashMap<>(stock);
        }

        boolean make(Dish dish, int depth) {
            if (dish.recipe() == null) {
                return false;
            }
            for (String part : dish.recipe().ingredients()) {
                if (!use(part, depth)) {
                    return false;
                }
                String rest = dish.recipe().cooking() ? null : remainder(part);
                if (rest != null) {
                    this.back.merge(rest, 1, Integer::sum);
                }
            }
            if (dish.recipe().cooking()) {
                this.seared++;
            }
            this.steps.add(dish);
            this.made.merge("hexkagune:" + dish.id(), dish.makes(), Integer::sum);
            return true;
        }

        boolean use(String part, int depth) {
            if (this.made.getOrDefault(part, 0) > 0) {
                this.made.merge(part, -1, Integer::sum);
                return true;
            }
            if (this.have.getOrDefault(part, 0) > 0) {
                this.have.merge(part, -1, Integer::sum);
                return true;
            }
            Dish prep = depth > 0 ? Dishes.byItem(part) : null;
            if (prep != null && prep.recipe() != null && this.make(prep, depth - 1)) {
                this.made.merge(part, -1, Integer::sum);
                return true;
            }
            return false;
        }
    }

    /** True if a recipe other than those in {@code steps} needs {@code id} too. */
    static boolean neededElsewhere(String id, List<Dish> steps) {
        for (Dish dish : Menu.DISHES) {
            if (dish.recipe() == null || steps.contains(dish)) {
                continue;
            }
            for (String part : dish.recipe().ingredients()) {
                if (part.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * True if it would cook {@code dish} for its spouse at all. Only for a ghoul ({@code ghoul}): every dish of the cookbook has ghoul
     * meat in it (a human would be sick on it, and the meat wasted) or human meat (it won't feed its human spouse a person); a human
     * gets the plain cooking. A dish that does nothing but fill (the mince) is only ever made on the way to another; searing raw meat
     * is cooking, so it counts.
     */
    public static boolean cooks(Dish dish, boolean ghoul) {
        if (!ghoul || dish == null || dish.recipe() == null) {
            return false;
        }
        return dish.effects().length > 0 || dish.recipe().cooking();
    }

    /** How much it would like to make {@code dish}: dishes that do something first (the more effects, the longer and stronger, the better), then the filling ones. */
    public static double worth(Dish dish) {
        double score = dish.effects().length * 100.0 + dish.nutrition();
        for (Dish.Effect effect : dish.effects()) {
            score += effect.ticks() / 20.0 * (effect.amplifier() + 1) / 10.0;
        }
        return score;
    }

    /**
     * Which of the cookbook's dishes it makes from {@code stock} now, or null for none: of the ones it {@link #cooks} and can make, the
     * best three by {@link #worth} (never the one it made last time while there is another), picked by {@code roll} (0..1, the best
     * likeliest: 3, 2, 1). {@code emptiedLast}: what it made last time took the last of something another recipe needed: this time it
     * leaves the rest alone (never twice in a row), so a dish that would do it again isn't made.
     */
    public static Cooking choose(Map<String, Integer> stock, int searable, boolean ghoul, String last, boolean emptiedLast, double roll) {
        List<Cooking> can = new ArrayList<>();
        for (Dish dish : Menu.DISHES) {
            if (!cooks(dish, ghoul)) {
                continue;
            }
            Cooking cooking = cooking(dish, stock, searable);
            if (cooking != null && !(emptiedLast && cooking.empties(stock))) {
                can.add(cooking);
            }
        }
        if (can.size() > 1 && last != null) {
            can.removeIf(c -> c.dish().id().equals(last));
        }
        if (can.isEmpty()) {
            return null;
        }
        can.sort(Comparator.comparingDouble((Cooking c) -> -worth(c.dish())).thenComparing(c -> c.dish().id()));
        int n = Math.min(3, can.size());
        int[] weights = {3, 2, 1};
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += weights[i];
        }
        double at = Math.max(0.0, Math.min(0.999999, roll)) * total;
        for (int i = 0; i < n; i++) {
            at -= weights[i];
            if (at < 0.0) {
                return can.get(i);
            }
        }
        return can.get(n - 1);
    }

    /**
     * True if this meal's turn passes without a dish only because what it made last time took the last of something ({@code
     * emptiedLast}) and every dish it could make now would too: the turn counts as a meal (none is made), and at the next one the last
     * of anything is fair game again. So it never takes the last of something twice in a row, yet a home stocked one recipe at a time
     * is still cooked for.
     */
    public static boolean passes(Map<String, Integer> stock, int searable, boolean ghoul, String last, boolean emptiedLast) {
        return emptiedLast && choose(stock, searable, ghoul, last, true, 0.0) == null && choose(stock, searable, ghoul, last, false, 0.0) != null;
    }

    /** True if it may cook another dish now: fewer than {@link #DISHES_A_DAY} today ({@code today}), {@link #DISH_GAP} since the last ({@code lastAt}). */
    public static boolean dishDue(int today, long lastAt, long now) {
        return today < DISHES_A_DAY && (lastAt == Long.MIN_VALUE || now < lastAt || now - lastAt >= DISH_GAP);
    }

    // ---------------------------------------------------------------- its words

    /** What it says handing over the meal it kept ({@code ghoul}: their spouse is a ghoul: the raw cuts; {@code what}: the main thing). */
    public static String served(String key, boolean ghoul, String what, int seed) {
        String[] lines = switch (key) {
            case "touka" -> ghoul ? new String[] {"*shoves a wrapped package at you* " + cap(what) + ". Raw. Don't make it weird. ...Eat.",
                "I put the good cuts aside for you. " + cap(what) + ". Don't tell me I never do anything."}
                : new String[] {"*pushes a plate into your hands* I can't taste any of it, so if it's bad, lie. " + cap(what) + ".",
                "You're late. I made " + what + ". It got cold waiting. ...Eat it anyway."};
            case "ken" -> ghoul ? new String[] {"Welcome home. I kept some " + what + " for you. Raw, the way you need it.",
                "*hands you a wrapped bundle* " + cap(what) + ". I know it's hard. Eat anyway. For me."}
                : new String[] {"Welcome home. I made " + what + ". I can't eat it with you, but I like watching you eat.",
                "*sets a plate in front of you* " + cap(what) + ". I learned it from a book. Tell me if it's good."};
            default -> ghoul ? new String[] {"Welcome home, love. I saved you " + what + ". Raw. Romantic, isn't it?",
                "*presses a bundle into your hands* " + cap(what) + ", fresh. The best cuts went to you. You owe me."}
                : new String[] {"Welcome home. I cooked. " + cap(what) + ". Don't make that face, I followed the recipe. Mostly.",
                "*hands you a plate* " + cap(what) + ". I can't taste a thing, so you're my critic. Be kind."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /**
     * 1.3.42: what it says having made one of the cookbook's dishes ({@code what}: its name) with its spouse there to hear: {@code kept}
     * it is keeping it for them (handed over now); else it is in the chest (the meal it keeps for them is full).
     */
    public static String made(String key, String what, boolean kept, int seed) {
        String[] lines = switch (key) {
            case "touka" -> kept ? new String[] {"*pushes a plate at you* " + what + ". I made it from the book. Eat it while it's hot.",
                "I made " + what + ". Don't look at me like that. Just eat."}
                : new String[] {"I made " + what + ". It's in the chest. ...You're welcome.", "There's " + what + " in the chest. Don't let it go to waste."};
            case "ken" -> kept ? new String[] {"*sets it down in front of you* " + what + ". I followed the recipe exactly. I hope it's good.",
                "I made " + what + " for you. Sit down a minute and eat."}
                : new String[] {"I made " + what + ". It's in the chest, for whenever you're hungry.", "There's " + what + " in the chest. I made extra."};
            default -> kept ? new String[] {"*presents it with a flourish* " + what + ". Straight out of the cookbook. Eat.",
                "I made " + what + ". You will eat all of it and praise me. Those are the rules."}
                : new String[] {"I made " + what + ". It's in the chest. Admire it before you eat it.", "There's " + what + " in the chest. I'm a terrifyingly good cook."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** 1.3.42: what it says handing over a dish of the cookbook it kept for them ({@code more}: it put more in the chest, too). */
    public static String servedDish(String key, String what, boolean more, int seed) {
        String rest = more ? switch (key) {
            case "touka" -> " There's more in the chest.";
            case "ken" -> " I put the rest in the chest.";
            default -> " The rest is in the chest. I spoil you.";
        } : "";
        String[] lines = switch (key) {
            case "touka" -> new String[] {"You're back. I made " + what + ". It's still warm. ...Mostly.",
                "*hands you a plate* " + what + ". Out of the book. Don't tell me if it's bad."};
            case "ken" -> new String[] {"Welcome home. I made " + what + " while you were out. I kept it warm for you.",
                "*sets a plate in front of you* " + what + ". I've been waiting to give you this."};
            default -> new String[] {"Welcome home. I cooked. " + what + ", from the cookbook. Worship me.",
                "*hands you a plate* " + what + ". I got bored waiting, so I cooked. You're welcome."};
        };
        return lines[Math.floorMod(seed, lines.length)] + rest;
    }

    /** 1.3.42: what it says when they come home and it put what it made in the chest (it kept no meal for them this time). */
    public static String madeWhileOut(String key, String what, int seed) {
        String[] lines = switch (key) {
            case "touka" -> new String[] {"You're back. I made " + what + ". It's in the chest.", "Took you long enough. There's " + what + " in the chest."};
            case "ken" -> new String[] {"Welcome home. I made " + what + " while you were out. It's in the chest.",
                "You're back. There's " + what + " in the chest, if you're hungry."};
            default -> new String[] {"Welcome home. I made " + what + ". It's in the chest, being magnificent.",
                "You're back! I cooked " + what + ". The chest. Go. Admire."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** What it says when there's nothing to burn for the stove. */
    public static String noFuel(String key, int seed) {
        String[] lines = switch (key) {
            case "touka" -> new String[] {"We're out of coal. Again. I'm not burning the furniture.", "No fuel. Get coal or eat it raw."};
            case "ken" -> new String[] {"We're out of coal. ...Could you bring some home?", "The furnace is cold. We need coal, or wood."};
            default -> new String[] {"No coal, no cooking. Bring me fuel and I'll make you something wonderful.", "The stove's empty. I refuse to rub sticks together."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    private static String cap(String text) {
        return text == null || text.isEmpty() ? "" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
