package com.hex.kagune.control.kitchen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Home Living (1.3.40): the ghoul kitchen's dishes looked up (pure, tested): by their item's path, by chapter, what a recipe is made of.
 * What the kitchen's own effects do, in numbers, is here too ({@link #REFRESH_RC} and the rest), for the server and the tests alike.
 */
public final class Dishes {
    private static final Map<String, Dish> BY_ID = new HashMap<>();
    private static final Map<String, String> NAMES = new HashMap<>();

    static {
        for (int i = 0; i < Menu.DISHES.length; i++) {
            Dish dish = Menu.DISHES[i];
            BY_ID.put(dish.id(), dish);
            NAMES.put(dish.id(), i < Menu.NAMES.length ? Menu.NAMES[i] : dish.id());
        }
    }

    /** Refreshed: RC flowing back a second, per level (the bar holds 100). */
    public static final float REFRESH_RC = 0.25F;
    /** Replenished: one heart's half closed every this many ticks at the first level (halved at the second). */
    public static final int REPLENISH_TICKS = 80;
    /** Content: how much frenzy ebbs a second, per level, and how much faster the ones you love warm to you. */
    public static final float CONTENT_CALM = 0.02F;
    public static final float CONTENT_WARMTH = 1.5F;
    /** Clear Sight: how much further a body sees through water, lava and powder snow. */
    public static final float CLEAR_SIGHT_FAR = 3.0F;
    /** The effects Clear Sight takes off (vanilla's paths). */
    public static final String[] CLEARED = {"blindness", "darkness", "nausea"};

    private Dishes() {
    }

    /** The dish whose item path is {@code id}, or null. */
    public static Dish byId(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** 1.3.42: the dish an item id names ("hexkagune:crimson_curry"), or null (another mod's item, a vanilla one, no such dish). */
    public static Dish byItem(String itemId) {
        return itemId != null && itemId.startsWith("hexkagune:") ? BY_ID.get(itemId.substring("hexkagune:".length())) : null;
    }

    /** 1.3.42: the dish's name as the lang file has it ("Crimson Curry"), or its id if it isn't one. */
    public static String name(String id) {
        return id == null ? "" : NAMES.getOrDefault(id, id);
    }

    /**
     * 4.2.1 (the owner: "check all the other quests"; of Anteiku's first: "the player has no idea how to craft the coffee ... You can just
     * like tell the player how it's done"): how the dish an item id names is made, in words, from its recipe here ("craft 1 glass bottle, 2
     * cocoa beans and 1 blood sugar together"), with each of the kitchen's own ingredients' recipes after it, all the way down ("Blood Sugar Cubes:
     * craft 1 sugar and 1 Bottled Blood together"); empty for anything that is no dish. What a companion wishes for, or a chapter asks, says it.
     */
    public static String how(String itemId) {
        Dish dish = byItem(itemId);
        if (dish == null || dish.recipe() == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(made(dish));
        java.util.Set<String> told = new java.util.LinkedHashSet<>();
        told.add(dish.id());
        java.util.ArrayDeque<String> next = new java.util.ArrayDeque<>(java.util.List.of(dish.recipe().ingredients()));
        // The kitchen's own ingredients all the way down (the blend's blood sugar, the blood sugar's bottled blood), each told once.
        for (int guard = 0; !next.isEmpty() && guard < 32; guard++) {
            String ingredient = next.poll();
            Dish inner = byItem(ingredient);
            if (inner != null && inner.recipe() != null && told.add(inner.id())) {
                out.append("; ").append(words(ingredient)).append(": ").append(made(inner));
                next.addAll(java.util.List.of(inner.recipe().ingredients()));
            }
        }
        return out.toString();
    }

    /** 4.2.1: one dish's own recipe in words: crafted ("craft 1 x, 2 y and 1 z together") or cooked ("cook x"). */
    static String made(Dish dish) {
        String[] ingredients = dish.recipe().ingredients();
        if (dish.recipe().cooking()) {
            return "cook " + words(ingredients[0]) + " (a furnace, a smoker or a campfire)";
        }
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String ingredient : ingredients) {
            counts.merge(ingredient, 1, Integer::sum);
        }
        StringBuilder out = new StringBuilder("craft ");
        int i = 0;
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            out.append(i == 0 ? "" : i == counts.size() - 1 ? " and " : ", ").append(entry.getValue()).append(' ').append(words(entry.getKey()));
            i++;
        }
        return out.append(" together").toString();
    }

    /** 4.2.1: an item id as words ("minecraft:cocoa_beans" "cocoa beans"; a dish its own name, lower case). */
    static String words(String itemId) {
        Dish dish = byItem(itemId);
        if (dish != null) {
            return name(dish.id()); // the kitchen's own as the game names it ("Blood Sugar Cubes")
        }
        String path = itemId.indexOf(':') >= 0 ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        return path.replace('_', ' ');
    }

    /** Every dish in {@code chapter}, in the book's order. */
    public static List<Dish> chapter(String chapter) {
        List<Dish> out = new ArrayList<>();
        for (Dish dish : Menu.DISHES) {
            if (dish.chapter().equals(chapter)) {
                out.add(dish);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** True if {@code key} is one of the kitchen's own effects (else a vanilla one, by its path). */
    public static boolean kitchenEffect(String key) {
        for (Menu.Kitchen effect : Menu.EFFECTS) {
            if (effect.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** A regard gain of {@code gain} while content ({@link #CONTENT_WARMTH} of it, rounded; never less than it was). */
    public static int warmed(int gain) {
        return gain <= 0 ? gain : Math.max(gain, Math.round(gain * CONTENT_WARMTH));
    }

    /** Ticks between Replenished's heals at {@code amplifier}. */
    public static int replenishTicks(int amplifier) {
        return Math.max(10, REPLENISH_TICKS / (1 + Math.max(0, amplifier)));
    }

    /** "1:30" for {@code ticks}. */
    public static String time(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
    }

    /** " II" for amplifier 1 (nothing for 0), as vanilla's potion names. */
    public static String level(int amplifier) {
        return switch (amplifier) {
            case 0 -> "";
            case 1 -> " II";
            case 2 -> " III";
            case 3 -> " IV";
            default -> " " + (amplifier + 1);
        };
    }
}
