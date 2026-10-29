package com.hex.kagune.control.client.animation;

/**
 * 1.0.26: the final kakujas' runs (pure, so the previewer poses them as the game does), driven by his own stride ({@link #phase}: a
 * whole turn is two steps, run on as far as he really goes, so the faster he is the faster his legs) rather than vanilla's walk,
 * which tops out long before a final kakuja does. Each is a walk at {@code sprint} 0 and its run at 1.
 * <ul>
 *   <li>Peace runs as Naoya Zen'in does: tipped far forward from the hips, the head up and level, both arms swept straight back and a
 *       little out behind him, held there, the legs cutting long fast strides with the knees driven high, barely bobbing: a blade
 *       thrown along the ground.</li>
 *   <li>War runs on the warpath: hunched low, the head thrust out and glaring up from under the brow, the shoulders rolling, the arms
 *       wide and low pumping like claws raking the ground ahead, every footfall a stomp the whole body drops into, lurching side to
 *       side.</li>
 * </ul>
 * The legs are always the run's while he runs (whatever he strikes with, {@code KaguneBodyAnimation} lets no blow touch them on the
 * ground); the upper body is the run's only as far as {@code free} (a blow takes the arms, and most of the lean).
 */
public final class KingRun {
    /** Blocks a whole stride (two steps) at a full run; walking, strides are shorter (down to {@link #SHORT} of it). */
    public static final double STRIDE = 3.1;
    static final double SHORT = 0.55;
    /** Blocks a tick at which a stride is at its longest. */
    static final double FULL = 0.45;
    /**
     * How much of the run's lean a blow leaves him (the arms are the blow's): War most of it; Peace little, its blades striking along his
     * look as the server cuts them, so a lean tipping them down would draw them off where they really cut.
     */
    static final float LEAN_UNDER_BLOWS = 0.6F;
    static final float PEACE_LEAN_UNDER_BLOWS = 0.25F;

    private KingRun() {
    }

    /** How far (radians) his stride goes on for {@code speed} blocks over the ground. */
    public static double phase(double speed) {
        double stride = STRIDE * (SHORT + (1.0 - SHORT) * Math.min(1.0, speed / FULL));
        return 2.0 * Math.PI * speed / stride;
    }

    /** How hard he is running from {@code speed} blocks a tick (0: standing, 1: a full run's worth of stride). */
    public static float amount(double speed) {
        return (float)Math.max(0.0, Math.min(1.0, speed / 0.16));
    }

    /**
     * Peace's run onto {@code p}: stride {@code phase}, running {@code run} (0..1), sprinting {@code sprint} (0..1), its upper body as
     * free as {@code free} (0: a blow has it), at {@code time} ticks.
     */
    public static void peace(RigPose p, double phase, float run, float sprint, float free, double time) {
        if (run <= 0.0F) {
            return;
        }
        double w = run;
        double s = sprint;
        double lean = w * (free + (1.0F - free) * PEACE_LEAN_UNDER_BLOWS);
        double up = w * free;
        // Tipped far forward, the head kept up and level (his eyes on what he runs at).
        p.lean += (float)(lean * (0.1 + 0.52 * s));
        p.headX -= (float)(lean * (0.08 + 0.44 * s));
        // Both arms swept straight back, a little out, held: only a breath of the stride in them.
        double flutter = 0.05 * Math.sin(phase * 2.0 + 0.3);
        p.rightArmX += (float)(up * (0.18 + 0.98 * s + flutter));
        p.leftArmX += (float)(up * (0.18 + 0.98 * s - flutter));
        p.rightArmZ += (float)(up * (0.08 + 0.17 * s));
        p.leftArmZ -= (float)(up * (0.08 + 0.17 * s));
        p.rightElbow -= (float)(up * 0.05 * s);
        p.leftElbow -= (float)(up * 0.05 * s);
        p.armHold += (float)(up * (0.25 + 0.75 * s));
        p.twist += (float)(up * 0.08 * Math.sin(phase));
        // Long fast strides, the knees driven high on the way through; barely a bob.
        legs(p, phase, w, 0.5 + 0.45 * s, 0.75 + 1.15 * s, 0.0);
        p.drop += (float)(w * (0.25 + 0.35 * s) * (0.5 + 0.5 * Math.cos(2.0 * phase)));
    }

    /**
     * War's run onto {@code p}, as {@link #peace}; {@code seed} his own (the head's jolts at the footfalls are his).
     */
    public static void war(RigPose p, double phase, float run, float sprint, float free, double time, int seed) {
        if (run <= 0.0F) {
            return;
        }
        double w = run;
        double s = sprint;
        double lean = w * (free + (1.0F - free) * LEAN_UNDER_BLOWS);
        double up = w * free;
        // Every footfall a stomp: sharp, the whole body dropping into it.
        double foot = Math.pow(0.5 + 0.5 * Math.cos(2.0 * phase), 4.0);
        // Hunched low, the head thrust out and glaring up from under the brow; jolted by each stomp.
        p.lean += (float)(lean * (0.22 + 0.55 * s));
        p.headX -= (float)(lean * (0.18 + 0.5 * s) - up * 0.12 * foot);
        p.headZ += (float)(up * 0.08 * Math.sin(phase + seed));
        // The shoulders rolling, the body lurching to each side it lands on.
        p.twist += (float)(up * (0.12 + 0.24 * s) * Math.sin(phase));
        p.side += (float)(lean * (0.05 + 0.08 * s) * Math.sin(phase));
        // The arms wide and low, pumping against the legs like claws raking the ground ahead, the elbows bent most reaching forward.
        double pump = (0.45 + 0.65 * s) * Math.cos(phase);
        p.rightArmX += (float)(up * (0.22 * s + pump));
        p.leftArmX += (float)(up * (0.22 * s - pump));
        p.rightArmZ += (float)(up * (0.25 + 0.28 * s));
        p.leftArmZ -= (float)(up * (0.25 + 0.28 * s));
        p.rightElbow += (float)(up * (0.45 + 0.75 * s * (0.5 - 0.5 * Math.cos(phase))));
        p.leftElbow += (float)(up * (0.45 + 0.75 * s * (0.5 + 0.5 * Math.cos(phase))));
        p.armHold += (float)(up * (0.4 + 0.6 * s));
        // Heavy strides, a little wide.
        legs(p, phase, w, 0.55 + 0.4 * s, 0.6 + 0.95 * s, 0.07);
        p.drop += (float)(w * (0.4 + 1.3 * s) * foot);
    }

    /**
     * Both legs through a stride at {@code phase} (the right's; the left's half a turn on), {@code w} of them: each swung forward to
     * strike the ground straight ({@code swing} radians either side), driven back under him, then folded high on the way through
     * ({@code fold}, the knee); {@code wide} out to its side. They are all the run's: no vanilla swing under them.
     */
    static void legs(RigPose p, double phase, double w, double swing, double fold, double wide) {
        p.rightLegX += (float)(w * thigh(phase, swing));
        p.leftLegX += (float)(w * thigh(phase + Math.PI, swing));
        p.rightKnee += (float)(w * knee(phase, fold));
        p.leftKnee += (float)(w * knee(phase + Math.PI, fold));
        p.rightLegZ += (float)(w * wide);
        p.leftLegZ -= (float)(w * wide);
        p.legHold += (float)w;
    }

    /** The thigh at {@code phase}: forward to strike the ground at 0, back under him through to a half turn, forward again. */
    static double thigh(double phase, double swing) {
        return -0.08 - swing * Math.cos(phase);
    }

    /** The knee at {@code phase}: near straight on the ground, folded high on the way through (the second half of the turn). */
    static double knee(double phase, double fold) {
        double a = phase - 2.0 * Math.PI * Math.floor(phase / (2.0 * Math.PI));
        if (a <= Math.PI) {
            return 0.15 * Math.sin(a);
        }
        double u = (a - Math.PI) / Math.PI;
        return fold * Math.sin(Math.PI * Math.pow(u, 0.7));
    }
}
