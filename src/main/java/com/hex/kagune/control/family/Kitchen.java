package com.hex.kagune.control.family;

import java.util.ArrayList;
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

    /** "cooked beef" for "minecraft:cooked_beef". */
    public static String name(String id) {
        String path = id == null ? "" : id.substring(id.indexOf(':') + 1);
        return path.replace('_', ' ');
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
