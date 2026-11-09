package com.hex.kagune.control.bikaku;

/**
 * 3.7.3, Potential Unlock, torn apart and pulled back together (pure; the owner's words in {@code docs/handoff/owner-words-mutations.md}:
 * "While the player still has energy remaining, they gain the ability to survive devastating injuries that would ordinarily cause death,
 * including the physical destruction or separation of body parts"; "dismemberment must NOT happen frequently"; "Every detached limb takes
 * approximately 5 seconds to completely reattach"; "Every time a severed body part successfully reattaches, the player receives a small
 * health boost"; "If the player's energy runs out during regeneration, the process must be interrupted until sufficient energy becomes
 * available"). The parts that come off whole, the timeline of each (ticks from the blow; the drawing is {@code Reattach}), what it costs,
 * and which part a blow takes. The server keeps each part's age, paused while the bar is empty during the pull.
 */
public final class PotentialRules {
    /** A part torn off whole: the head at the neck, an arm at the shoulder, a leg at the hip. */
    public enum Part {
        HEAD, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG;

        public int bit() {
            return 1 << this.ordinal();
        }

        public static Part byOrdinal(int ordinal) {
            Part[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
        }
    }

    /** The strands come out of the wound; they reach it; they have hold of it and haul; it is in place; flesh closed, strands in: whole. */
    public static final int REACH_AT = 14;
    public static final int GRIP_AT = 40;
    public static final int HAUL_AT = 48;
    public static final int HOME_AT = 88;
    public static final int WHOLE = 100;

    /** The health he is left with when a blow that would have killed him doesn't (one heart), and the energy that costs. */
    public static final float SURVIVE_HEALTH = 2.0F;
    public static final float SURVIVE_COST = 20.0F;
    /** The energy each part's strands draw a tick while they reach and haul (from {@link #REACH_AT} to {@link #HOME_AT}). */
    public static final float PULL_DRAIN = 0.15F;
    /** A single blow this much of his full health or more tears a part off (rare: never an ordinary blow). */
    public static final float HEAVY_BLOW = 0.5F;
    /** The small heal when a part is back on (one heart), only once it is whole. */
    public static final float HEAL = 2.0F;
    /** How likely the head is to be the part a blow takes: a blow that would have killed him, and a heavy one that wouldn't. */
    public static final double HEAD_LETHAL = 0.2;
    public static final double HEAD_HEAVY = 0.05;

    private PotentialRules() {
    }

    /** True while a part's strands reach and haul (they draw energy; with none, the part waits where it is). */
    public static boolean pulling(double age) {
        return age >= REACH_AT && age < HOME_AT;
    }

    /**
     * Which part a blow takes: none already off ({@code missing}, {@link Part#bit} bits); the head now and then ({@link #HEAD_LETHAL} of a
     * blow that would have killed him, {@link #HEAD_HEAVY} of another); else an arm (twice as likely) or a leg, on the side the blow came
     * from ({@code side} -1 his left .. 1 his right) more often. {@code roll} and {@code pick} are two numbers in [0, 1). Null if every part is
     * already off.
     */
    public static Part pick(int missing, boolean lethal, double side, double roll, double pick) {
        boolean head = (missing & Part.HEAD.bit()) == 0;
        if (head && roll < (lethal ? HEAD_LETHAL : HEAD_HEAVY)) {
            return Part.HEAD;
        }
        double right = 0.5 + 0.3 * Math.max(-1.0, Math.min(1.0, side));
        Part[] limbs = {Part.RIGHT_ARM, Part.LEFT_ARM, Part.RIGHT_LEG, Part.LEFT_LEG};
        double[] weight = {2.0 * right, 2.0 * (1.0 - right), right, 1.0 - right};
        double total = 0.0;
        for (int i = 0; i < limbs.length; i++) {
            if ((missing & limbs[i].bit()) != 0) {
                weight[i] = 0.0;
            }
            total += weight[i];
        }
        if (total <= 0.0) {
            return head ? Part.HEAD : null;
        }
        double at = pick * total;
        for (int i = 0; i < limbs.length; i++) {
            at -= weight[i];
            if (at < 0.0 && weight[i] > 0.0) {
                return limbs[i];
            }
        }
        for (int i = limbs.length - 1; i >= 0; i--) {
            if (weight[i] > 0.0) {
                return limbs[i];
            }
        }
        return null;
    }
}
