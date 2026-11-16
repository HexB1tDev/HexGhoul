package com.hex.kagune.control;

/**
 * Home Living (1.3.40): a mutant (pure): a ghoul Eto has made one-eyed like her ({@code KakuganServer.granted}). Its body is built
 * differently: its blows land harder ({@link #STRENGTH}, on top of the ghoul meat it has eaten), its RC drains far slower (every cost
 * divided by {@link #ENDURANCE}: the same bar holds that much more), and it gets more out of every meal (each bite of flesh fills its
 * reserve {@link #FEED} times as far).
 */
public final class Mutant {
    public static final float STRENGTH = 1.25F;
    public static final float ENDURANCE = 1.6F;
    public static final float FEED = 1.5F;

    private Mutant() {
    }

    /** What a mutant's (or anyone's) blows are multiplied by, with the ghoul meat's {@code meat} on top. */
    public static float strength(boolean mutant, float meat) {
        return mutant ? meat * STRENGTH : meat;
    }

    /** What its RC costs are divided by, with the ghoul meat's {@code meat} on top. */
    public static float endurance(boolean mutant, float meat) {
        return mutant ? meat * ENDURANCE : meat;
    }

    /** The nutrition a bite of {@code nutrition} counts as for its reserve. */
    public static int feed(boolean mutant, int nutrition) {
        return mutant ? Math.round(nutrition * FEED) : nutrition;
    }
}
