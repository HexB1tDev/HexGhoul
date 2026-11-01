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

    static {
        for (Dish dish : Menu.DISHES) {
            BY_ID.put(dish.id(), dish);
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
