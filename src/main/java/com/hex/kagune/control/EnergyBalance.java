package com.hex.kagune.control;

/**
 * RC, made to last at high mastery (pure, tested; 6.1.0; 1.0.33: far more, and on everything). The bar holds 100 whatever the
 * mastery (the original's HUD, rest-time recovery and checks are built on that), so mastery makes what it pays for cheaper
 * instead: every drain on it (moves, a kakuja's upkeep, healing, the kakugan, Graft's and Pale's drains) costs
 * {@link #costScale} of its price, so a full bar goes {@link #capacity} times as far: twice and a quarter at 500, five times at 1000.
 * The original takes 30% off a move's cost with mastery of its own; {@link #undoOriginal} gives that back, so a move scales by
 * mastery the same as everything else.
 */
public final class EnergyBalance {
    /** What a move costs at mastery 1000 in the original, of its base. */
    public static final float ORIGINAL_MASTERED = 0.7F;
    /** How many times as far a full bar goes at mastery 1000. */
    public static final float MASTERED_CAPACITY = 5.0F;

    private EnergyBalance() {
    }

    private static float share(int mastery) {
        return Math.max(0, Math.min(1000, mastery)) / 1000.0F;
    }

    /** How many times as far a full bar goes at {@code mastery}: 1 for a new ghoul, rising faster the nearer 1000 (5 there). */
    public static float capacity(int mastery) {
        float t = share(mastery);
        return 1.0F + t + (MASTERED_CAPACITY - 2.0F) * t * t;
    }

    /** Anything drawn from the bar costs this of its price at {@code mastery}: 1 for a new ghoul, a fifth at 1000. */
    public static float costScale(int mastery) {
        return 1.0F / capacity(mastery);
    }

    /** The original's own mastery cut on a move's cost, undone (it multiplies by 1 at mastery 0 up to 1 / 0.7 at 1000). */
    public static float undoOriginal(int mastery) {
        return 1.0F / (1.0F - (1.0F - ORIGINAL_MASTERED) * share(mastery));
    }
}
