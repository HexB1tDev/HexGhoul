package com.hex.kagune.control.eto;

/**
 * Eto's Speed Blitz (M1; 6.1.0, pure): eight strikes chained click by click, each thrown with the whole body. The first four
 * are hers from the start, the uppercut and the blinking palm from {@link EtoRules#BLITZ_BLINK_MASTERY}, the spinning backfist
 * and the twin palm finisher from {@link EtoRules#BLITZ_FINISH_MASTERY}; past the last one she has, the blitz starts over.
 *
 * <p>Each strike: its length, impact tick and the tick the next strike may cut its recovery short from (the fighter's chain: the
 * last strike's extension is the next one's coil), damage multiplier, how hard and which way it throws the target (back, to her right
 * or left, up), its reach, whether it is thrown from the right hand, and whether she blinks (vanishes for an instant and strikes
 * from another angle) first.
 */
public enum EtoStrike {
    /** A snapping lead jab, the left fist straight from the guard. */
    JAB(7, 2, 4, 0.8F, 0.25, 0.0, 0.05, 3.2, false, Blink.NONE, 0),
    /** The right cross, hips and shoulders turning through. */
    CROSS(8, 3, 5, 0.9F, 0.3, 0.0, 0.05, 3.3, true, Blink.NONE, 0),
    /** A left hook round the side of the head: the target is knocked to her right. */
    HOOK(8, 3, 5, 1.0F, 0.2, 0.42, 0.08, 3.1, false, Blink.NONE, 0),
    /** Dipping under, a right hook into the body (the liver): it staggers. */
    LIVER(8, 3, 5, 1.0F, 0.32, 0.0, 0.02, 3.0, true, Blink.NONE, 0),
    /** Rising from the dip, a left uppercut that lifts the target off its feet. */
    UPPERCUT(9, 4, 6, 1.2F, 0.15, 0.0, 0.55, 3.1, false, Blink.NONE, EtoRules.BLITZ_BLINK_MASTERY),
    /** She vanishes and strikes from the target's side with a right palm. */
    BLINK_PALM(10, 4, 6, 1.25F, 0.25, -0.55, 0.12, 3.0, true, Blink.SIDE, EtoRules.BLITZ_BLINK_MASTERY),
    /** A full turn on the spot into a left backfist; it catches everything round her. */
    BACKFIST(10, 5, 7, 1.35F, 0.35, 0.55, 0.1, 3.4, false, Blink.NONE, EtoRules.BLITZ_FINISH_MASTERY),
    /** She vanishes, reappears square in front of the target and drives both palms into it. */
    TWIN_PALM(14, 6, 14, 1.8F, 1.55, 0.0, 0.3, 3.2, true, Blink.FRONT, EtoRules.BLITZ_FINISH_MASTERY);

    /** Where a blink puts her: nowhere (no blink), at the target's side, square in front of it. */
    public enum Blink {
        NONE, SIDE, FRONT
    }

    public final int duration;
    public final int impact;
    /** From this tick a queued strike starts at once, out of this one's hold (its recovery is never seen). */
    public final int chain;
    public final float damage;
    /** Knockback away from her, to her right (negative: left) and up (blocks a tick). */
    public final double back;
    public final double sideways;
    public final double up;
    public final double reach;
    public final boolean right;
    public final Blink blink;
    public final int mastery;

    EtoStrike(int duration, int impact, int chain, float damage, double back, double sideways, double up, double reach, boolean right, Blink blink,
              int mastery) {
        this.duration = duration;
        this.impact = impact;
        this.chain = chain;
        this.damage = damage;
        this.back = back;
        this.sideways = sideways;
        this.up = up;
        this.reach = reach;
        this.right = right;
        this.blink = blink;
        this.mastery = mastery;
    }

    /** The tick she blinks on (the strike's first, so the fist comes from the new angle). */
    public static final int BLINK_TICK = 1;

    /** The finisher: a recovery follows it. */
    public boolean finisher() {
        return this == TWIN_PALM;
    }

    /** RC a strike costs (before the original's own scaling by mastery). */
    public float cost() {
        return this.finisher() ? 1.2F : 0.3F + 0.08F * this.ordinal();
    }

    /** The strike after {@code previous} (null: the first) that {@code mastery} allows; after the last one, the first again. */
    public static EtoStrike next(EtoStrike previous, int mastery) {
        int from = previous == null ? 0 : previous.ordinal() + 1;
        EtoStrike[] all = values();
        if (from >= all.length || all[from].mastery > mastery) {
            return JAB;
        }
        return all[from];
    }

    public static EtoStrike byOrdinal(int ordinal) {
        EtoStrike[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : JAB;
    }

    /** How many strikes the blitz has at {@code mastery}. */
    public static int count(int mastery) {
        int count = 0;
        for (EtoStrike strike : values()) {
            if (strike.mastery <= mastery) {
                count++;
            }
        }
        return count;
    }
}
