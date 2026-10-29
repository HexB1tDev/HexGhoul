package com.hex.kagune.control;

/**
 * 7.0.1: Ghoul Meat (pure, tested). Another ghoul's flesh, devoured, stays in the body for good: every piece makes the ghoul hit
 * harder ({@link #strength}: every blow it lands, kagune, fists or blade) and its RC last longer ({@link #endurance}: every RC
 * cost, each move and the kagune's and the eyes' upkeep, is divided by it, so the same bar holds more). Each piece adds less
 * than the one before, but never nothing; past {@link #SCALE} pieces most of the way is gone. A human's body can't take it.
 */
public final class GhoulMeat {
    /** The item's registry path, and where the count lives in the profile root. */
    public static final String ID = "ghoul_meat";
    public static final String KEY = "GhoulMeatEaten";
    /** As food: rich in RC, it fills most of a ghoul's flesh reserve. */
    public static final int NUTRITION = 8;
    public static final float SATURATION = 0.8F;
    /** At the limit: +40% to every blow, and RC lasting twice as long. */
    public static final float STRENGTH_MAX = 0.4F;
    public static final float ENDURANCE_MAX = 1.0F;
    /** Pieces for about two thirds of the way to the limit. */
    public static final double SCALE = 40.0;
    /** A human who eats it: sick for this long (ticks). */
    public static final int SICK_TICKS = 300;

    private GhoulMeat() {
    }

    /** How far toward the limit {@code eaten} pieces have gone (0 to 1, never quite 1). */
    public static double share(int eaten) {
        return 1.0 - Math.exp(-Math.max(0, eaten) / SCALE);
    }

    /** What every blow is multiplied by after {@code eaten} pieces. */
    public static float strength(int eaten) {
        return (float)(1.0 + STRENGTH_MAX * share(eaten));
    }

    /** What every RC cost is divided by after {@code eaten} pieces. */
    public static float endurance(int eaten) {
        return (float)(1.0 + ENDURANCE_MAX * share(eaten));
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
