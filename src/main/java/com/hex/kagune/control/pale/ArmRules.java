package com.hex.kagune.control.pale;

/**
 * 6.2.0, the Mutated Arm (N; pure, tested): Pale's right arm turns to her pale flesh and shoots out where she looks. Tapped, it
 * shoots out {@link #TAP_REACH}, grabs what its hand meets (the hurt), drags it back to her and flings it. Held, it keeps on
 * swelling, thicker and more monstrous, reaching out as far as {@link #MAX_REACH}, following where she looks with the lag of its
 * weight, carrying whatever its hand meets; at the full charge what it caught is gathered at its end and the flesh there folds in
 * over it into a sphere that crushes it ({@link #SQUEEZES} times), held out on the stretched arm a moment; then it opens and the arm
 * draws slowly back into her, the mutation going back down. She walks the whole time.
 */
public final class ArmRules {
    /** What the arm is doing (sent by ordinal: new ones go at the end). */
    public enum Phase {
        /** In her: her own arm. */
        NONE,
        /** Shooting out where she looks. */
        SHOOT,
        /** Held out, swelling, reaching further, carrying what it caught. */
        HOLD,
        /** Hauled back to her with what it caught, flung at the end. */
        DRAG,
        /** The sphere: folding in over what it caught, crushing it, held, opening. */
        SPHERE,
        /** Drawing back into her, the mutation going back down. */
        RETRACT;

        public static Phase byOrdinal(int ordinal) {
            Phase[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }
    }

    public static final int MASTERY = 700;
    /** How far it shoots on a tap, and the furthest it reaches held at the full charge. */
    public static final double TAP_REACH = 20.0;
    public static final double MAX_REACH = 35.0;
    /**
     * How long it takes to shoot out (ticks; 6.2.0: twice as long, the user's "flesh arm travels slower": a heavy thing hurled, not a
     * beam), and how long it is held before the full charge.
     */
    public static final int SHOOT_TICKS = 12;
    public static final int CHARGE_TICKS = 60;
    /** The drag back (6.2.0: slower, as the shot), and where it stops short of her (blocks; further for a swollen hand, {@link #dragTo}). */
    public static final int DRAG_TICKS = 14;
    public static final double DRAG_TO = 2.6;
    /** The sphere: folding in, crushing ({@link #SQUEEZES} squeezes), held shut, opening. */
    public static final int SPHERE_FORM = 8;
    public static final int SPHERE_CRUSH = 15;
    public static final int SQUEEZES = 3;
    public static final int SPHERE_HELD = 10;
    public static final int SPHERE_OPEN = 5;
    /** Drawing back in: slowly after the sphere, quickly after a drag. */
    public static final int RETRACT_TICKS = 30;
    public static final int RETRACT_QUICK = 10;
    /** How near its hand (blocks, past half a body's width) a body is caught. */
    public static final double CATCH = 0.9;
    /** How quickly (of the way a tick) it turns after where she looks: its weight lags it. */
    public static final double FOLLOW = 0.3;
    /** Damage (times a fist): the hand closing on it; each of the sphere's squeezes. */
    public static final float GRAB_DAMAGE = 1.3F;
    public static final float CRUSH_DAMAGE = 2.2F;
    /** The fling at the end of a drag (blocks a tick, out to her side and up). */
    public static final double FLING = 1.3;
    public static final double FLING_UP = 0.45;
    public static final float COST = 8.0F;
    public static final float SPHERE_COST = 12.0F;
    public static final int COOLDOWN = 50;
    /** The most it catches at once. */
    public static final int MOST = 6;

    private ArmRules() {
    }

    /** How charged it is, held {@code held} ticks (0..1). */
    public static double charge(double held) {
        return clamp01(held / CHARGE_TICKS);
    }

    /** How far it reaches at {@code charge}: a tap's, out to the full charge's. */
    public static double reach(double charge) {
        return TAP_REACH + (MAX_REACH - TAP_REACH) * clamp01(charge);
    }

    /** Its thickest (blocks, a radius) at {@code charge}: a swollen arm's at once, swelling as it is held to well over twice that. */
    public static double girth(double charge) {
        return 0.16 + 0.26 * (0.15 + 0.85 * clamp01(charge));
    }

    /** How far out it is {@code age} ticks into shooting out to {@code to}: fast, slowing as it gets there. */
    /**
     * 6.2.0: where along the arm (blocks from the shoulder, between {@code near} and {@code far}) its hand passes closest to
     * {@code centre}, the arm going out of {@code shoulder} along {@code aim} (unit): the hand sweeps everything between where it was
     * last tick and where it is now, so nothing it went past (close to her, between two ticks' reach) is missed.
     */
    public static double along(double[] shoulder, double[] aim, double[] centre, double near, double far) {
        double t = (centre[0] - shoulder[0]) * aim[0] + (centre[1] - shoulder[1]) * aim[1] + (centre[2] - shoulder[2]) * aim[2];
        return Math.max(Math.min(near, far), Math.min(Math.max(near, far), t));
    }

    /** 6.2.0: how far {@code centre} is from the arm's hand {@code t} blocks out of {@code shoulder} along {@code aim}. */
    public static double off(double[] shoulder, double[] aim, double[] centre, double t) {
        double dx = centre[0] - shoulder[0] - aim[0] * t;
        double dy = centre[1] - shoulder[1] - aim[1] * t;
        double dz = centre[2] - shoulder[2] - aim[2] * t;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double shot(double age, double to) {
        double u = clamp01(age / SHOOT_TICKS);
        return ARM + (to - ARM) * (1.0 - (1.0 - u) * (1.0 - u));
    }

    /**
     * How far it shoots: a tap's reach; held ({@code heldBefore} ticks down as it shot), as far as the charge will reach as it gets
     * there, so the hold carries straight on.
     */
    public static double shootTo(double heldBefore) {
        return heldBefore > 0.0 ? reach(charge(heldBefore + SHOOT_TICKS)) : TAP_REACH;
    }

    /** How far out it is {@code age} ticks into the drag back from {@code from} at {@code charge}: hauled, heavy then fast. */
    public static double dragged(double age, double from, double charge) {
        double u = clamp01(age / DRAG_TICKS);
        double to = Math.min(from, dragTo(charge));
        return from + (to - from) * u * u * (3.0 - 2.0 * u);
    }

    /** Where the drag stops (the middle of its hand, blocks from her shoulder) at {@code charge}: its hand, however swollen, short of her. */
    public static double dragTo(double charge) {
        return Math.max(DRAG_TO, HAND_CLEAR + HAND_FITS * girth(charge));
    }

    /**
     * How far behind the middle of its hand (blocks, along the arm) the arm itself ends, {@code girth} thick: its knot of flesh and
     * the grip of its claws (as the hand is drawn, {@code PaleFleshModel.armHand}).
     */
    public static double handBack(double girth) {
        double knot = girth * 1.5;
        return knot * 1.55 + (girth * 1.7 + 0.2) * 0.8;
    }

    /** The middle of its hand this far (blocks) from her shoulder, and then this much more for each block of its girth, keeps it off her. */
    static final double HAND_CLEAR = 0.85;
    static final double HAND_FITS = 4.9;

    /**
     * The thickest the arm and its hand may be with the middle of its hand {@code out} blocks from her shoulder: shooting out of her
     * and drawing back into her, its hand grows out of hers and goes back into it, never swelling into her.
     */
    public static double fits(double out) {
        return Math.max(0.1, (out - HAND_CLEAR) / HAND_FITS);
    }

    /** How far out it is {@code age} ticks into drawing back in from {@code from} over {@code ticks}. */
    public static double retracted(double age, double from, double ticks) {
        double u = clamp01(age / ticks);
        return from + (ARM - from) * u * u * (3.0 - 2.0 * u);
    }

    /** Her own arm's length (blocks): where it starts and ends. */
    public static final double ARM = 0.7;

    /** The mutation at {@code age} ticks into drawing back in over {@code ticks}, from {@code from}: going back down to her arm. */
    public static double unswollen(double age, double from, double ticks) {
        double u = clamp01(age / ticks);
        return from + (girth(0.0) * 0.6 - from) * u;
    }

    /** The sphere {@code age} ticks in: how shut it is (0 open .. 1 shut) and how hard it squeezes now (0..1). */
    public static double[] sphere(double age) {
        double shut;
        if (age < SPHERE_FORM) {
            shut = smooth(age / SPHERE_FORM);
        } else if (age < SPHERE_FORM + SPHERE_CRUSH + SPHERE_HELD) {
            shut = 1.0;
        } else {
            shut = 1.0 - smooth((age - SPHERE_FORM - SPHERE_CRUSH - SPHERE_HELD) / SPHERE_OPEN);
        }
        double squeeze = 0.0;
        double crush = age - SPHERE_FORM;
        if (crush >= 0.0 && crush < SPHERE_CRUSH) {
            double each = SPHERE_CRUSH / (double)SQUEEZES;
            double into = (crush % each) / each;
            // Slammed shut, then easing off for the next.
            squeeze = into < 0.25 ? smooth(into / 0.25) : 1.0 - smooth((into - 0.25) / 0.75);
        }
        return new double[] {shut, squeeze};
    }

    /** The ticks into the sphere each squeeze lands on (its hurt). */
    public static int squeezeTick(int index) {
        return SPHERE_FORM + (int)Math.round(index * SPHERE_CRUSH / (double)SQUEEZES + SPHERE_CRUSH / (double)SQUEEZES * 0.25);
    }

    /** All of the sphere: formed, crushed, held, opened. */
    public static final int SPHERE_TICKS = SPHERE_FORM + SPHERE_CRUSH + SPHERE_HELD + SPHERE_OPEN;

    /** How big the sphere is round bodies this tall (blocks, its radius). */
    public static double sphereRadius(double tallest) {
        return Math.max(0.9, Math.min(1.8, tallest * 0.62));
    }

    /** How far out (blocks, its middle from her shoulder) the sphere is, the arm out {@code out}: never closer than its size and an arm. */
    public static double sphereOut(double out, double radius) {
        return Math.max(out, radius + 1.2);
    }

    /** {@code d} turned toward {@code look} by {@link #FOLLOW} (both unit vectors): one tick of its lag. */
    public static double[] follow(double[] d, double[] look) {
        double[] n = {d[0] + (look[0] - d[0]) * FOLLOW, d[1] + (look[1] - d[1]) * FOLLOW, d[2] + (look[2] - d[2]) * FOLLOW};
        double l = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        return l < 1.0E-9 ? look.clone() : new double[] {n[0] / l, n[1] / l, n[2] / l};
    }

    static double clamp01(double u) {
        return Math.max(0.0, Math.min(1.0, u));
    }

    static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3.0 - 2.0 * c);
    }
}
