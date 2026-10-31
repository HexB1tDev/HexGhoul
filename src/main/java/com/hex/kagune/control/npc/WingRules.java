package com.hex.kagune.control.npc;

/**
 * 1.3.19: Touka's wings in a fight (pure): her own little thing, nowhere near Ken or Eto. A lesser pair (Ukaku about halfway grown) out
 * of her back the moment a fight finds her, turned to crystal, throwing crystal spikes: a volley of three, a fan of five, now and then
 * the barrage (both wings in turn, fast). Up close she fights with what her eyes give her (strikes, the kick, flips).
 */
public final class WingRules {
    private WingRules() {
    }

    /** How grown her wings are drawn (Ukaku mastery): about halfway, a lesser pair. */
    public static final int MASTERY = 380;
    /** Out over, crystal after (ticks). */
    public static final int DEPLOY_TICKS = 10;
    public static final int CRYSTAL_AFTER = 12;
    /** Closer than this (blocks), her fists; farther than this, she closes in before she throws. */
    public static final double NEAR = 3.2;
    public static final double FAR = 20.0;
    /** The distance she keeps while she throws (she backs off inside it). */
    public static final double KEEP = 6.0;
    /** A spike: how fast (blocks a tick), how far, how wide it catches. */
    public static final double SPEED = 2.0;
    public static final double RANGE = 26.0;
    public static final double RADIUS = 0.35;
    /** What each kind hurts: the volley's, the fan's, the barrage's. */
    public static final float SHOT = 4.0F;
    public static final float FAN = 3.0F;
    public static final float BARRAGE = 2.0F;
    /** The volley: every how many ticks a spike, how many, the rest after. */
    public static final int SHOT_EVERY = 7;
    public static final int VOLLEY = 3;
    public static final int VOLLEY_REST = 26;
    /** The fan: how many spikes, how wide (degrees each side), how often. */
    public static final int FAN_SPIKES = 5;
    public static final double FAN_SPREAD = 14.0;
    public static final int FAN_EVERY = 140;
    /** The barrage: how many spikes, one every how many ticks, how often. */
    public static final int BARRAGE_SPIKES = 14;
    public static final int BARRAGE_EVERY_TICK = 2;
    public static final int BARRAGE_EVERY = 240;
    /** How long out of a fight before her wings go back in. */
    public static final long PUT_AWAY = 100L;

    /** What her wings do now. */
    public enum Act {
        /** Too close, or nothing to throw at: her fists and her feet (or a chase). */
        NONE,
        /** The next spike of a volley. */
        VOLLEY,
        /** A fan of spikes at once. */
        FAN,
        /** The barrage. */
        BARRAGE,
        /** Waiting on the next spike, keeping her distance. */
        HOLD
    }

    /** What she does with her wings at {@code gap} blocks off it, seeing it or not, out of what is ready; {@code roll} in [0, 1). */
    public static Act choose(double gap, boolean sees, long now, long nextShot, long fanReady, long barrageReady, double roll) {
        if (gap < NEAR || !sees || gap > FAR) {
            return Act.NONE;
        }
        if (barrageReady <= now && roll < 0.35) {
            return Act.BARRAGE;
        }
        if (fanReady <= now && roll < 0.55) {
            return Act.FAN;
        }
        return nextShot <= now ? Act.VOLLEY : Act.HOLD;
    }

    /** How far a fan's {@code i}th of {@code count} spikes is turned off its aim (degrees). */
    public static double fan(int i, int count) {
        return count <= 1 ? 0.0 : FAN_SPREAD * (2.0 * i / (count - 1) - 1.0);
    }
}
