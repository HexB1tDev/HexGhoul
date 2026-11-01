package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 1.3.27: how a CCG investigator moves (pure): its own run, the same for every one of them; its parry; its dash. Its swings, its stance
 * and its case opening are the quinque's ({@link QuinquePose}), its flips a player's ({@code PaleMotions.flip}).
 *
 * <p>The run: an investigator's, trained: the body pitched forward from the hips, the head up against it to keep its eyes on what it
 * runs at, a long stride with the back foot kicked high, the shoulders turning against the hips, a small bounce at each footfall. The
 * free arm pumps, close in; the other carries what it holds: the case low at its side (heavy: it swings little), a one-handed blade
 * trailed low behind it and out from the body, a two-handed quinque kept in its stance a little lowered (its legs and body still the
 * run's).
 */
public final class AgentPose {
    private AgentPose() {
    }

    /** What the right hand carries as it runs. */
    public enum Carry {
        /** Its case shut (or nothing). */
        CASE,
        /** A one-handed quinque out (the blade). */
        BLADE,
        /** A two-handed one in its stance (the spear, the katana, the mace, the launcher). */
        STANCE
    }

    /** The run's lean forward, its head's lift against it, its stride, its arm pump, its bounce (model pixels). */
    static final float LEAN = 0.3F;
    static final float HEAD = -0.24F;
    static final float STRIDE = 1.1F;
    static final float KICK = 0.18F;
    static final float PUMP = 1.0F;
    static final float BOUNCE = 0.6F;
    static final float SHOULDERS = 0.14F;
    /** How far a two-handed stance is lowered as it runs (radians on both arms). */
    static final float STANCE_LOWER = 0.35F;

    /**
     * Its run {@code amount} of the way in (0: the vanilla walk, 1: all the run), {@code phase} into its stride (radians: the walk's own
     * limb swing times vanilla's 0.6662, so the feet keep pace with the ground), carrying {@code carry}.
     */
    public static RigPose run(double phase, float amount, Carry carry) {
        RigPose p = new RigPose();
        float k = Math.max(0.0F, Math.min(1.0F, amount));
        if (k <= 0.0F) {
            return p;
        }
        float s = (float)Math.sin(phase);
        float c = (float)Math.cos(phase);
        // The legs: forward is X-; the back leg kicked a little higher behind than the front one reaches.
        p.rightLegX = (STRIDE * s + KICK * Math.max(0.0F, s)) * k;
        p.leftLegX = (-STRIDE * s + KICK * Math.max(0.0F, -s)) * k;
        p.rightLegZ = 0.04F * k;
        p.leftLegZ = -0.04F * k;
        p.legHold = k;
        // The arms: each pumps with the opposite leg, a little out from the body, further forward than back.
        p.leftArmX = (PUMP * s - 0.12F * Math.abs(s) - 0.1F) * k;
        p.leftArmZ = -0.1F * k;
        p.rightArmX = (-PUMP * s - 0.12F * Math.abs(s) - 0.1F) * k;
        p.rightArmZ = 0.1F * k;
        switch (carry) {
            case CASE -> {
                p.rightArmX = (0.12F - 0.22F * s) * k;
                p.rightArmZ = 0.08F * k;
            }
            case BLADE -> {
                p.rightArmX = (0.6F + 0.08F * s) * k;
                p.rightArmY = 0.15F * k;
                p.rightArmZ = 0.32F * k;
            }
            case STANCE -> {
                // (the stance's arms are put on by whoever asked: see stanceRun)
            }
        }
        p.armHold = k;
        // The body: pitched forward, the head lifted against it, the shoulders turning against the stride, a bounce at each footfall.
        p.lean = LEAN * k;
        p.headX = HEAD * k;
        p.twist = SHOULDERS * s * k;
        p.drop = BOUNCE * Math.abs(c) * k;
        return p;
    }

    /** A two-handed stance carried on the run: both arms its stance's, a little lowered, bobbing with each stride. */
    public static void stanceRun(RigPose run, RigPose stance, double phase, float amount) {
        if (stance == null || amount <= 0.0F) {
            return;
        }
        float k = Math.max(0.0F, Math.min(1.0F, amount));
        float bob = 0.06F * (float)Math.sin(phase * 2.0) * k;
        run.rightArmX = stance.rightArmX + STANCE_LOWER * k + bob;
        run.rightArmY = stance.rightArmY;
        run.rightArmZ = stance.rightArmZ;
        run.leftArmX = stance.leftArmX + STANCE_LOWER * k + bob;
        run.leftArmY = stance.leftArmY;
        run.leftArmZ = stance.leftArmZ;
        run.armHold = 1.0F;
        run.twist *= 0.4F; // the weapon steadies the shoulders
    }

    /** How long its parry is drawn (ticks). */
    public static final int PARRY_TICKS = 9;

    /** Its weight through a quick move {@code t} (0..1): in fast, held, out. */
    static float snap(double t) {
        if (t < 0.0 || t >= 1.0) {
            return 0.0F;
        }
        if (t < 0.28) {
            return QuinquePose.ease(t / 0.28);
        }
        if (t < 0.6) {
            return 1.0F;
        }
        return 1.0F - QuinquePose.ease((t - 0.6) / 0.4);
    }

    /**
     * Its parry {@code t} through (0..1 over {@link #PARRY_TICKS}): from its stance (or a held blade) the weapon whipped up and across
     * the body to meet the blow (both arms for a two-handed one), the shoulders turning into it.
     */
    public static RigPose parry(Quinque q, double t) {
        float k = snap(t);
        RigPose base = QuinquePose.hold(q);
        RigPose p = new RigPose();
        float[] right = base == null ? QuinquePose.HELD : new float[] {base.rightArmX, base.rightArmY, base.rightArmZ};
        p.rightArmX = right[0] - 0.85F * k;
        p.rightArmY = right[1] - 0.6F * k;
        p.rightArmZ = right[2] - 0.2F * k;
        p.rightArmHold = 1.0F;
        if (base != null) {
            p.leftArmX = base.leftArmX - 0.85F * k;
            p.leftArmY = base.leftArmY - 0.6F * k;
            p.leftArmZ = base.leftArmZ;
            p.leftArmHold = 1.0F;
        }
        p.twist = -0.35F * k;
        p.lean = -0.08F * k;
        return p;
    }

    /** How long a dash is drawn (ticks): its push, and the body coming back up. */
    public static final int DASH_TICKS = 11;

    /**
     * A dash ({@code way}: 0 forward, 1 back, 2 left, 3 right) {@code t} through (0..1 over {@link #DASH_TICKS}) put on {@code p}: low,
     * the legs split along it, the body leaning into it.
     */
    public static void dash(RigPose p, int way, double t) {
        float k = snap(t);
        if (k <= 0.0F) {
            return;
        }
        float legs = Math.max(p.legHold, k);
        switch (way) {
            case 0 -> {
                p.lean += 0.42F * k;
                p.rightLegX = p.rightLegX * (1.0F - k) - 0.75F * k;
                p.leftLegX = p.leftLegX * (1.0F - k) + 0.8F * k;
            }
            case 1 -> {
                p.lean -= 0.15F * k;
                p.rightLegX = p.rightLegX * (1.0F - k) + 0.7F * k;
                p.leftLegX = p.leftLegX * (1.0F - k) - 0.55F * k;
            }
            default -> {
                float side = way == 2 ? 1.0F : -1.0F;
                p.side += 0.3F * side * k;
                p.rightLegX *= 1.0F - k;
                p.leftLegX *= 1.0F - k;
                p.rightLegZ = p.rightLegZ * (1.0F - k) + 0.42F * k;
                p.leftLegZ = p.leftLegZ * (1.0F - k) - 0.42F * k;
            }
        }
        p.legHold = legs;
        p.drop += 2.2F * k;
        p.headX -= 0.1F * k;
    }

    /** How much of its run it is in at {@code speed} (blocks a tick along the ground): none at a walk, all of it at a sprint. */
    public static float running(double speed) {
        return (float)Math.max(0.0, Math.min(1.0, (speed - 0.16) / 0.12));
    }
}
