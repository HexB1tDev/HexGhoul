package com.hex.kagune.control.npc;

/**
 * 1.3.36: the wild ghouls' arm blade (pure, tested): a Koukaku no player can take. It tears out of the right shoulder blade and winds
 * down the arm in a spiral of hard plates, closing over the forearm into a long, ridged blade past the fist (a lance-like Koukaku, as
 * the heavy, armoured kagune of the shoulder is). The ghoul fights with that one arm: 4.1.47 four slashes to a string ({@link #STEPS}),
 * each landing partway through, then it circles in its stance. (4.1.47: the look is BladeArmModel's: broad ribbons wound round the arm
 * into one great curved blade, its edge facing down.)
 *
 * <p>Ticks are game ticks. The string is chained: a cut begun within {@link #CHAIN} of the last one's end is the next of the string;
 * after the last, a breath ({@link #pause}).
 */
public final class BladeArm {
    private BladeArm() {
    }

    /** How long the kagune takes to wind down the arm into its blade (ticks). */
    public static final int WRAP_TICKS = 18;
    /** The plates of its spiral, shoulder to wrist, and how many turns they make round the arm. */
    public static final int PLATES = 11;
    public static final double TURNS = 1.35;
    /** How long its blade is past the wrist, at its full length (model pixels). 4.1.47: 26 (it was 15: "way too small"). */
    public static final double BLADE_LENGTH = 26.0;
    /**
     * The cuts of a string. 4.1.47 (the owner: "also give it a proper moveset, slash attacks and a proper ambient stand"): four slashes, a
     * chop down from over the right shoulder, a rising backhand, a wide sweep across (it cuts everyone before it), then the finisher, the
     * blade raised high behind the head and brought down in a lunging cleave.
     */
    public static final int STEPS = 4;
    public static final int SWEEP = 2;
    public static final int FINISHER = 3;
    /** The next cut within this long of the last one's end goes on with the string (ticks). */
    public static final long CHAIN = 12L;

    /** How long a cut takes, start to recovered (ticks): the thrust longer. */
    public static int ticks(int step) {
        return switch (step) {
            case 0, 1 -> 10;
            case SWEEP -> 12;
            default -> 17;
        };
    }

    /** The tick of a cut on which it lands (it can be met before then: KaguneClash). */
    public static int lands(int step) {
        return switch (step) {
            case 0, 1 -> 5;
            case SWEEP -> 6;
            default -> 11;
        };
    }

    /** How far it reaches (blocks from its feet to the prey's): the arm and the blade; a stronger one's blade is longer. */
    public static double reach(int tier) {
        return 3.2 + 0.25 * Math.max(0, Math.min(2, tier)); // 4.1.47: the great blade (it was 3.0)
    }

    /** The sweep reaches a little further, the cleave (with its lunge) further still. */
    public static double reach(int tier, int step) {
        return reach(tier) + (step == FINISHER ? 1.2 : step == SWEEP ? 0.4 : 0.0);
    }

    /** How hard each cut lands, on the tier's damage (NpcRules.damage). */
    public static float weight(int step) {
        return switch (step) {
            case 0 -> 1.0F;
            case 1 -> 1.1F;
            case SWEEP -> 1.2F;
            default -> 2.0F;
        };
    }

    /** How hard it steps in with each cut (blocks a tick): the thrust drives it in. */
    public static double lunge(int step) {
        return step == FINISHER ? 0.85 : step == SWEEP ? 0.2 : 0.35;
    }

    /** 4.1.47: true if cut {@code step} cuts everyone before it in reach (the sweep), not only its prey. */
    public static boolean sweeps(int step) {
        return step == SWEEP;
    }

    /** The next cut: the first of a string, or the one after {@code last} ({@code -1} none) if it ended {@code since} ticks ago. */
    public static int next(int last, long since) {
        return last < 0 || last >= FINISHER || since < 0L || since > CHAIN ? 0 : last + 1;
    }

    /** The breath after a string (ticks), from a number in 0..1. */
    public static int pause(double roll) {
        return 16 + (int)(Math.max(0.0, Math.min(1.0, roll)) * 22.0);
    }

    /** How far the kagune has wound down the arm ({@code wrap} 0..1) for plate {@code i} (0 at the shoulder): 0 not yet, 1 grown. */
    public static double plate(int i, double wrap) {
        double start = 0.04 + 0.56 * i / (PLATES - 1.0);
        return Math.max(0.0, Math.min(1.0, (wrap - start) / 0.14));
    }

    /** How far the blade is out past the wrist (0..1): it grows once the spiral has reached the wrist. */
    public static double blade(double wrap) {
        double t = Math.max(0.0, Math.min(1.0, (wrap - 0.62) / 0.38));
        return t * t * (3.0 - 2.0 * t);
    }
}
