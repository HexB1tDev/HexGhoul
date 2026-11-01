package com.hex.kagune.control;

/**
 * 7.0.1: Ghoul Meat (pure, tested). Another ghoul's flesh, devoured, stays in the body for good: every piece makes the ghoul hit
 * harder ({@link #strength}: every blow it lands, kagune, fists or blade) and its RC last longer ({@link #endurance}: every RC
 * cost, each move and the kagune's and the eyes' upkeep, is divided by it, so the same bar holds more). A human's body can't take it.
 * Home Living (1.3.40): the growth never stops ({@link #growth}: the log of the pieces, so each one adds a little less than the one
 * before, but there is no ceiling: a hundred pieces is about +21% and x1.45, a thousand +47% and x1.98), and it is gentler than it was
 * at the start. Any dish with ghoul meat in it counts its pieces ({@code Dish.ghoulMeat}); human meat, however it is cooked, counts
 * for nothing.
 */
public final class GhoulMeat {
    /** The item's registry path, and where the count lives in the profile root. */
    public static final String ID = "ghoul_meat";
    public static final String KEY = "GhoulMeatEaten";
    /** As food: rich in RC, it fills most of a ghoul's flesh reserve. */
    public static final int NUTRITION = 8;
    public static final float SATURATION = 0.8F;
    /** 1.3.40: what one unit of growth adds: +12% to every blow, RC lasting a quarter longer. */
    public static final double STRENGTH_PER = 0.12;
    public static final double ENDURANCE_PER = 0.25;
    /** 1.3.40: the pieces for the first unit of growth to be most of the way (e - 1 times this). */
    public static final double SCALE = 20.0;
    /** A human who eats it: sick for this long (ticks). */
    public static final int SICK_TICKS = 300;

    private GhoulMeat() {
    }

    /** 1.3.40: how far {@code eaten} pieces have grown a ghoul: 0 for none, rising for ever, every piece a little less than the last. */
    public static double growth(int eaten) {
        return Math.log1p(Math.max(0, eaten) / SCALE);
    }

    /** What every blow is multiplied by after {@code eaten} pieces. */
    public static float strength(int eaten) {
        return (float)(1.0 + STRENGTH_PER * growth(eaten));
    }

    /** What every RC cost is divided by after {@code eaten} pieces. */
    public static float endurance(int eaten) {
        return (float)(1.0 + ENDURANCE_PER * growth(eaten));
    }

    /** {@code multiplier} as the bonus it is, in whole percent ("+12%"). */
    public static String percent(float multiplier) {
        return "+" + Math.round((multiplier - 1.0F) * 100.0F) + "%";
    }

    /** {@code multiplier} with two decimals ("x1.25"). */
    public static String times(float multiplier) {
        return String.format(java.util.Locale.ROOT, "x%.2f", multiplier);
    }
}
