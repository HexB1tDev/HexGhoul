package com.hex.kagune.control.client.animation;

/**
 * 1.0.26: the final kakujas' runs (pure, so the previewer poses them as the game does), driven by his own stride ({@link #phase}: a
 * whole turn is two steps, run on as far as he really goes, so the faster he is the faster his legs) rather than vanilla's walk,
 * which tops out long before a final kakuja does. Each is a walk at {@code sprint} 0 and its run at 1. 1.3.66 (the owner: "new more
 * dramatic running animations for both War and Peace"):
 * <ul>
 *   <li>Peace glides: thrown so far forward he is nearly flat, his head held dead level, his eyes on what he runs at; both arms flung back
 *       and spread wide like swept wings, lifting a little at the top of every bound; long bounding strides, the knee driven high, a hang
 *       in the air between them and down light onto the next, banking a little into each.</li>
 *   <li>War charges like a beast: thrown forward almost flat, the head thrust out ahead and glaring along the ground; between footfalls
 *       both claws reach far out ahead together, and as each foot comes down they rake back, so he drags himself on with every stomp;
 *       the whole body rolling and lurching onto the side it lands on, every stomp a drop the head jolts with. Walking, the claws pump
 *       wide and low as before.</li>
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
     * free as {@code free} (0: a blow has it), at {@code time} ticks. 1.3.66: the glide (see the class).
     */
    public static void peace(RigPose p, double phase, float run, float sprint, float free, double time) {
        if (run <= 0.0F) {
            return;
        }
        double w = run;
        double s = sprint;
        double lean = w * (free + (1.0F - free) * PEACE_LEAN_UNDER_BLOWS);
        double up = w * free;
        // Between footfalls (0 as a foot comes down, 1 halfway to the next): the bound's hang.
        double air = Math.pow(Math.sin(phase), 2.0);
        double land = Math.pow(0.5 + 0.5 * Math.cos(2.0 * phase), 4.0);
        // Nearly flat at a full sprint (the upper body as far over as it goes, the whole of him tipped after it), the head held dead level.
        p.lean += (float)(lean * (0.12 + 0.7 * s)); // (not more: the blades on his back would dip into the ground before him)
        p.headX -= (float)(lean * (0.1 + 0.85 * s));
        // Both arms flung back and spread like swept wings, lifting a little at the top of every bound.
        double beat = 0.16 * s * (air - 0.5);
        p.rightArmX += (float)(up * (0.2 + 0.2 * s + beat));
        p.leftArmX += (float)(up * (0.2 + 0.2 * s + beat));
        p.rightArmZ += (float)(up * (0.1 + 0.75 * s));
        p.leftArmZ -= (float)(up * (0.1 + 0.75 * s));
        p.rightArmY -= (float)(up * 0.2 * s);
        p.leftArmY += (float)(up * 0.2 * s);
        p.armHold += (float)(up * (0.25 + 0.75 * s));
        // Banking a little into each bound.
        p.roll += (float)(up * 0.07 * s * Math.sin(phase));
        p.twist += (float)(up * 0.06 * Math.sin(phase));
        // Long bounding strides, the knee driven high; a hang in the air between them, down light onto the next.
        legs(p, phase, w, 0.55 + 0.5 * s, 0.8 + 1.2 * s, 0.0);
        p.drop += (float)(w * ((0.25 + 0.45 * s) * land - (0.2 + 2.8 * s) * air));
    }

    /**
     * War's run onto {@code p}, as {@link #peace}; {@code seed} his own (the head's jolts at the footfalls are his). 1.3.66: the beast's
     * charge (see the class).
     */
    public static void war(RigPose p, double phase, float run, float sprint, float free, double time, int seed) {
        if (run <= 0.0F) {
            return;
        }
        double w = run;
        double s = sprint;
        double lean = w * (free + (1.0F - free) * LEAN_UNDER_BLOWS);
        double up = w * free;
        // Every footfall a stomp: sharp, the whole body dropping into it; the claws raked back as it lands (rake 1), reaching between (-1).
        double foot = Math.pow(0.5 + 0.5 * Math.cos(2.0 * phase), 4.0);
        double rake = Math.cos(2.0 * phase);
        // Thrown forward almost flat (the upper body as far over as it goes, the whole of him tipped after it), the head thrust out
        // ahead, glaring along the ground; jolted by each stomp.
        p.lean += (float)(lean * (0.24 + 0.76 * s));
        p.flip -= (float)(lean * 0.15 * s);
        p.headX -= (float)(lean * (0.18 + 0.85 * s) - up * 0.16 * foot);
        p.headZ += (float)(up * (0.06 + 0.1 * s) * Math.sin(phase + seed));
        // The whole body rolling and lurching onto the side it lands on.
        p.twist += (float)(up * (0.12 + 0.22 * s) * Math.sin(phase));
        p.side += (float)(lean * (0.05 + 0.1 * s) * Math.sin(phase));
        p.roll += (float)(up * 0.1 * s * Math.sin(phase));
        // Walking: the claws pumping wide and low, one against the other. Charging: both reach far out ahead together between footfalls
        // and rake back as each foot comes down, the elbows bent as they pull.
        double pump = (0.45 + 0.2 * s) * Math.cos(phase) * (1.0 - s);
        double claw = s * (-1.25 + 0.9 * rake);
        p.rightArmX += (float)(up * (pump + claw));
        p.leftArmX += (float)(up * (-pump + claw));
        p.rightArmZ += (float)(up * (0.25 + 0.3 * s));
        p.leftArmZ -= (float)(up * (0.25 + 0.3 * s));
        double pull = 0.5 + 0.5 * rake;
        p.rightElbow += (float)(up * (0.45 * (1.0 - s) + s * (0.15 + 0.85 * pull)));
        p.leftElbow += (float)(up * (0.45 * (1.0 - s) + s * (0.15 + 0.85 * pull)));
        p.armHold += (float)(up * (0.4 + 0.6 * s));
        // Heavy strides, a little wide.
        legs(p, phase, w, 0.55 + 0.45 * s, 0.6 + 1.0 * s, 0.09);
        p.drop += (float)(w * (0.4 + 1.8 * s) * foot);
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
