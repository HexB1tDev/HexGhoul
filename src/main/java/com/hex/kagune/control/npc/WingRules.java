package com.hex.kagune.control.npc;

/**
 * 1.3.19: Touka's wings in a fight (pure): her own little thing, nowhere near Ken or Eto. 1.3.29: the owner: "only one wing, not two, and
 * it should be smaller": one small wing ({@link #SIDE}, Ukaku not long grown, {@link #MASTERY}) out of her back the moment a fight finds
 * her, turned to crystal, throwing crystal spikes from it: a volley of three, a fan of five, now and then the barrage. Up close she
 * fights with what her eyes give her (strikes, the kick, flips).
 */
public final class WingRules {
    private WingRules() {
    }

    /** How grown her wing is drawn (Ukaku mastery): just past the first wings, small. */
    public static final int MASTERY = 170;
    /** Her one wing: on her right (+1); every spike leaves from it. */
    public static final int SIDE = 1;
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

    /**
     * 1.3.26: a companion's wings (Touka's) as the rig draws them at {@link #MASTERY}, crystal as she throws, aiming as she shoots. 1.3.29:
     * one wing alone, on {@link #SIDE} (the other shoulder blade's grown to nothing: the rig draws nothing there).
     */
    public static com.hex.kagune.control.ukaku.WingRig.Input companion(float deploy, double time, float crystal, boolean dissolving, float aim,
                                                                       double[] at, float kickRight, float kickLeft, float detail) {
        return new com.hex.kagune.control.ukaku.WingRig.Input(com.hex.kagune.control.ukaku.WingForm.TWIN, MASTERY, deploy, time, crystal, crystal,
            com.hex.kagune.control.ukaku.WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, detail, 1.0F, dissolving, aim, aim, at, kickRight, kickLeft,
            0.0F, 0.0F, SIDE > 0 ? 1.0F : 0.0F, SIDE > 0 ? 0.0F : 1.0F);
    }

    /**
     * 1.3.43: a ghoul child's wing or wings (Ukaku inherited) as the rig draws them at {@code mastery} (its stage's): the pair, or one
     * alone on {@code side} (1 its right, -1 its left; the other grown to nothing), aiming as it shoots.
     */
    public static com.hex.kagune.control.ukaku.WingRig.Input child(int mastery, int wings, int side, float deploy, double time, boolean dissolving,
                                                                   float aim, double[] at, float kickRight, float kickLeft, float detail) {
        return child(mastery, wings, side, deploy, time, dissolving, aim, at, kickRight, kickLeft, detail, 0.0F);
    }

    /** 1.3.48: the same, turned to crystal as far as {@code crystal} (0..1). */
    public static com.hex.kagune.control.ukaku.WingRig.Input child(int mastery, int wings, int side, float deploy, double time, boolean dissolving,
                                                                   float aim, double[] at, float kickRight, float kickLeft, float detail, float crystal) {
        float right = wings >= 2 || side > 0 ? 1.0F : 0.0F;
        float left = wings >= 2 || side < 0 ? 1.0F : 0.0F;
        return new com.hex.kagune.control.ukaku.WingRig.Input(com.hex.kagune.control.ukaku.WingForm.TWIN, mastery, deploy, time, crystal, crystal,
            com.hex.kagune.control.ukaku.WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, detail, 1.0F, dissolving, aim, aim, at, kickRight, kickLeft,
            0.0F, 0.0F, right, left);
    }
}
