package com.hex.kagune.control.pale;

/**
 * 7.0.1: the shared eye-mode fight (pure). Any ghoul, whatever their path, with only the kakugan open (R; the kagune put away)
 * fights with the Owl DNA body: the Speed Blitz on M1 ({@link PaleStrike}), the flips on an empty right click, the Big Punch on Z
 * and the Speed Kick on a tap of V. Nothing of the flesh (no swipe, grab, hammer, Mutated Arm, Watch This or Owl): those were her
 * kagune. The kagune out (Alt + R) ends it at once and the path's own kit is back.
 *
 * <p>The moves and their timings are hers, unchanged; how hard they land grows with mastery ({@link #power}), on top of what her
 * own numbers already grow with it (the blitz's longer chain, the Speed Kick at {@link PaleRules#KICK_MASTERY}).
 */
public final class EyeRules {
    /** How hard the eyes alone hit against her base form: this at mastery 0 ... */
    public static final float POWER_NEW = 0.7F;
    /** ... and this at {@link #MASTERED}. */
    public static final float POWER_MASTERED = 1.0F;
    public static final int MASTERED = 1000;
    /**
     * A target this close to the crosshair (blocks from the eye) and within {@link #AIM_COS} of it turns an M1 on a block into a
     * strike instead of mining (a fight near the ground or a wall never starts digging).
     */
    public static final double AIM_RANGE = 4.5;
    public static final double AIM_COS = 0.9;

    private EyeRules() {
    }

    /** The multiplier on every blow of the eyes alone at {@code mastery}: {@link #POWER_NEW} rising evenly to {@link #POWER_MASTERED}. */
    public static float power(int mastery) {
        float t = Math.max(0.0F, Math.min(1.0F, mastery / (float)MASTERED));
        return POWER_NEW + (POWER_MASTERED - POWER_NEW) * t;
    }

    /**
     * Whether an M1 on a block goes to the fight: a living target {@code distance} blocks from the eye, {@code facing} the cosine
     * between the look and the way to it.
     */
    public static boolean strikesOverBlock(double distance, double facing) {
        return distance <= AIM_RANGE && facing >= AIM_COS;
    }
}
