package com.hex.kagune.control.ayato;

import com.hex.kagune.control.StrikePattern;

/**
 * How the Steel Blade is held and swung (pure, tested; 5.0.20). Everything here is the blade arm's whole turn in the chest's
 * own frame (model space before the chest's twist and lean: pixels, x to his left, y down, z backward), as a unit quaternion
 * {w, x, y, z}; {@link #euler} gives ModelPart's three angles for it.
 *
 * <p><b>The hold</b> ({@link #hold}; 6.0.4): relaxed, hanging down at his right side, out from the leg at about 58 degrees below
 * level, the edge forward, its point resting just off the ground. The blade runs about 2.8 blocks from the shoulder against
 * 1.4 blocks of shoulder height, so hanging there it draws in (about 1.6 blocks out of the shoulder,
 * {@link BladeCarry#fit}) and drives back out to its full length as a swing winds up ({@link #fit}). It breathes with him, his
 * weight shifts, it sways with his stride and swings back and up as he runs, crouched it hangs a little higher, and every few
 * seconds he idly turns the blade over in his hand and back.
 *
 * <p><b>The swings</b> ({@link #swing}): each a real cut. From wherever the blade is, it winds up (eased, a beat of stillness at
 * the top); then the cut, one continuous turn about the swing's own axis, edge leading, fastest exactly at the impact tick; it
 * carries through and slows to a stop; then it settles back into the hold. The chest coils into the wind-up and unwinds
 * through the cut, the head stays on the target, the free arm counters, the feet step into it.
 * <ul>
 *   <li>Steel Sweep: cocked back at his right, high; a flat cut across his front from right to left.</li>
 *   <li>Steel Return: wound across his chest to the left; the backhand out past his right.</li>
 *   <li>Steel Execution: raised behind his head; driven down across him on a slant into a lunge (the ground guard,
 *       {@link BladeCarry#guard}, stops it just above the ground).</li>
 * </ul>
 */
public final class SteelSwing {
    static final double[] RIGHT = {-1.0, 0.0, 0.0};
    static final double[] LEFT = {1.0, 0.0, 0.0};
    static final double[] UP = {0.0, -1.0, 0.0};
    static final double[] DOWN = {0.0, 1.0, 0.0};
    static final double[] FORWARD = {0.0, 0.0, -1.0};
    static final double[] BACK = {0.0, 0.0, 1.0};

    /**
     * The hold: how far behind straight out to his right the blade hangs, and how far below level (radians); how much further
     * back and how much higher it swings at a run.
     */
    static final double HOLD_TRAIL = Math.toRadians(6.0);
    static final double HOLD_DROP = Math.toRadians(58.0);
    static final double HOLD_RUN = Math.toRadians(17.0);
    static final double HOLD_RUN_LIFT = Math.toRadians(10.0);
    /** How much higher it hangs crouched (the shoulder is three pixels nearer the ground; even drawn in, 58 degrees would touch). */
    static final double HOLD_CROUCH = Math.toRadians(16.0);
    /** How often he turns the blade over in his hand (seconds), and how long it takes. */
    static final double FLOURISH_EVERY = 9.0;
    static final double FLOURISH_TAKES = 1.7;

    private SteelSwing() {
    }

    /** The body's part of a swing (radians, added to the body's own pose) and the blade arm's whole turn. */
    public static final class Pose {
        public float twist;
        public float torsoX;
        public float headX;
        public float headY;
        public float leftArmX;
        public float leftArmY;
        public float leftArmZ;
        public float leftArmBend;
        public float rightLegX;
        public float rightLegZ;
        public float leftLegX;
        public float leftLegZ;
        public float rightKnee;
        public float leftKnee;
        /** The blade arm's turn (quaternion, chest frame). */
        public double[] arm;
    }

    // ------------------------------------------------------------------ the hold

    /**
     * The relaxed hold at {@code seconds}, {@code walk} (0..1) how much he is walking at stride phase {@code stride} (radians),
     * {@code run} (0..1) how fast he runs.
     */
    public static double[] hold(double seconds, double walk, double stride, double run) {
        return hold(seconds, walk, stride, run, 0.0);
    }

    /** The hold, {@code crouch} (0..1) how far he is crouched: it hangs higher. */
    public static double[] hold(double seconds, double walk, double stride, double run, double crouch) {
        double breathe = Math.sin(seconds * 2.0 * Math.PI * 0.23);
        double shift = Math.sin(seconds * 2.0 * Math.PI * 0.11 + 1.1);
        double trail = HOLD_TRAIL + 0.05 * shift + 0.07 * walk * Math.sin(stride) + HOLD_RUN * run;
        double drop = HOLD_DROP + 0.03 * breathe - 0.03 * walk * Math.abs(Math.cos(stride)) - HOLD_RUN_LIFT * run - HOLD_CROUCH * crouch;
        double[] d = direction(trail, drop);
        // The edge forward, a little down.
        double[] q = aim(d, add(FORWARD, scale(DOWN, 0.3)));
        // Now and then he turns the blade over in his hand and back.
        double at = seconds % FLOURISH_EVERY;
        double roll = at < FLOURISH_TAKES ? 0.5 * Math.pow(Math.sin(Math.PI * at / FLOURISH_TAKES), 2.0) : 0.0;
        return multiply(axisAngle(d, roll * (1.0 - 0.8 * Math.min(1.0, walk + run))), q);
    }

    /** Straight out to his right turned {@code trail} toward his back and {@code drop} below level (unit). */
    static double[] direction(double trail, double drop) {
        double level = Math.cos(drop);
        return new double[] {RIGHT[0] * level * Math.cos(trail), Math.sin(drop), BACK[2] * level * Math.sin(trail)};
    }

    // ------------------------------------------------------------------ the swings

    /**
     * One swing's shape: where the wind-up leaves the blade (chest frame), the axis the cut turns it about and how far, how much
     * of that turn is done by the impact, and the body's keys {top of the wind-up, impact, end of the cut} for the chest's twist
     * and lean, the head's turn, the free arm and the legs.
     */
    record Cut(double[] windup, double[] via, double[] back, double[] axis, double angle, double lead, double share, float[] twist, float[] torso, float[] head, float[] leftX,
               float[] leftY, float[] leftZ, float[] rightLegX, float[] leftLegX, float[] leftKnee, float[] rightKnee) {
    }

    /** Out to his right turned {@code toward} degrees toward {@code side} and raised {@code up} degrees (unit). */
    static double[] heading(double[] from, double[] side, double toward, double up) {
        double a = Math.toRadians(toward);
        double e = Math.toRadians(up);
        return unit(add(scale(add(scale(from, Math.cos(a)), scale(side, Math.sin(a))), Math.cos(e)), scale(UP, Math.sin(e))));
    }

    static Cut cut(StrikePattern pattern) {
        return switch (pattern) {
            // Wound across his chest to the left; the backhand out past his right, the chest turning with it.
            case STEEL_RETURN -> new Cut(heading(LEFT, FORWARD, 42.0, 6.0), heading(FORWARD, RIGHT, 30.0, 12.0), null, DOWN, Math.toRadians(140.0), 0.45, 0.8,
                new float[] {-0.65F, 0.12F, 0.7F}, new float[] {0.06F, 0.16F, 0.12F}, new float[] {0.5F, -0.1F, -0.45F},
                new float[] {0.15F, 0.3F, 0.4F}, new float[] {0.0F, 0.0F, 0.0F}, new float[] {0.3F, -0.55F, -0.85F},
                new float[] {-0.15F, 0.25F, 0.3F}, new float[] {0.2F, -0.3F, -0.35F}, new float[] {0.0F, 0.2F, 0.25F},
                new float[] {0.15F, 0.0F, 0.0F});
            // Raised behind his head; driven down across him on a slant into a lunge.
            case STEEL_EXECUTION -> new Cut(unit(add(heading(UP, BACK, 25.0, 0.0), scale(RIGHT, 0.3))), null, heading(RIGHT, FORWARD, 55.0, -4.0), unit(add(LEFT, scale(UP, 0.3))),
                Math.toRadians(180.0), 0.72, 0.6,
                new float[] {0.3F, -0.25F, -0.4F}, new float[] {-0.3F, 0.42F, 0.5F}, new float[] {-0.2F, 0.1F, 0.16F},
                new float[] {-0.4F, 0.3F, 0.45F}, new float[] {0.0F, 0.0F, 0.0F}, new float[] {-0.5F, -0.65F, -0.7F},
                new float[] {0.15F, 0.45F, 0.5F}, new float[] {-0.1F, -0.55F, -0.6F}, new float[] {0.0F, 0.55F, 0.65F},
                new float[] {0.0F, 0.3F, 0.35F});
            // Cocked back at his right, a little raised; a flat cut across his front from right to left, the chest unwinding into it.
            default -> new Cut(heading(RIGHT, FORWARD, -10.0, 10.0), null, heading(RIGHT, FORWARD, 60.0, 0.0), UP, Math.toRadians(152.0), 0.6, 0.6,
                new float[] {0.7F, -0.15F, -0.75F}, new float[] {-0.06F, 0.15F, 0.2F}, new float[] {-0.5F, 0.1F, 0.5F},
                new float[] {-0.7F, -0.2F, 0.35F}, new float[] {-0.5F, 0.0F, 0.0F}, new float[] {-0.15F, -0.5F, -0.9F},
                new float[] {0.3F, -0.1F, -0.2F}, new float[] {-0.1F, -0.4F, -0.45F}, new float[] {0.0F, 0.25F, 0.3F},
                new float[] {0.2F, 0.0F, 0.0F});
        };
    }

    /** True for the patterns swung here. */
    public static boolean swings(StrikePattern pattern) {
        return pattern == StrikePattern.STEEL_SWEEP || pattern == StrikePattern.STEEL_RETURN || pattern == StrikePattern.STEEL_EXECUTION;
    }

    /**
     * 6.0.4: how much the blade is drawn in to rest off the ground ({@link BladeCarry#fit}; 0 full length, 1 drawn in as far as it
     * must) through the swing {@code pattern} at {@code progress}, from {@code from} as it began: it drives out to full length
     * over the wind-up, stays out through the cut, and draws back in as it settles into the hold.
     */
    public static double fit(StrikePattern pattern, float progress, float impact, double from) {
        Cut cut = cut(pattern);
        double p = Float.isFinite(progress) ? Math.max(0.0, Math.min(1.0, progress)) : 0.0;
        double w = windupEnd(impact, cut.share());
        double f = cutEnd(impact, cut.share());
        if (p < w) {
            return Math.max(0.0, Math.min(1.0, from)) * (1.0 - smoother(p / w));
        }
        return p < f ? 0.0 : smoother((p - f) / (1.0 - f));
    }

    /** Where the wind-up ends, for the cut that hits at {@code impact} (share of the swing): {@code share} of the way to it. */
    static double windupEnd(float impact, double share) {
        return share * clampImpact(impact);
    }

    /** Where the cut ends: as far past the impact as the wind-up's end is before it, so the impact is half way through the cut. */
    static double cutEnd(float impact, double share) {
        double i = clampImpact(impact);
        return Math.min(0.9, 2.0 * i - windupEnd(impact, share));
    }

    static double clampImpact(float impact) {
        return Float.isFinite(impact) ? Math.max(0.2, Math.min(0.6, impact)) : 0.4;
    }

    /**
     * How far through its turn a cut is, {@code u} of the way from the top of the wind-up to its end: still at both ends,
     * speeding up into the impact (half way in time, {@code lead} of the way round), fastest exactly there, then slowing to a
     * stop (an ease-in and an ease-out meeting at the impact at the same speed).
     */
    static double through(double u, double lead) {
        double x = Math.max(0.0, Math.min(1.0, u));
        double n = 2.2;
        double m = lead * n / (1.0 - lead);
        if (x < 0.5) {
            return lead * Math.pow(2.0 * x, n);
        }
        return lead + (1.0 - lead) * (1.0 - Math.pow(1.0 - (2.0 * x - 1.0), m));
    }

    /**
     * The swing {@code pattern} at {@code progress} (0..1) with its impact at {@code impact}: from {@code from} (where the blade
     * was as it began) back to {@code to} (the hold now).
     */
    public static Pose swing(StrikePattern pattern, float progress, float impact, double[] from, double[] to) {
        Cut cut = cut(pattern);
        double p = Float.isFinite(progress) ? Math.max(0.0, Math.min(1.0, progress)) : 0.0;
        double w = windupEnd(impact, cut.share());
        double f = cutEnd(impact, cut.share());
        // At the top the edge faces the way the cut will take it: it leads all the way round.
        double[] top = aim(cut.windup(), cross(cut.axis(), cut.windup()));
        double[] end = multiply(axisAngle(cut.axis(), cut.angle()), top);
        Pose pose = new Pose();
        if (p < w) {
            // Up to the top of the wind-up (by way of the cut's via: the backhand goes round in front of him, not down through
            // his legs).
            pose.arm = path(from, cut.via(), top, smoother(p / w));
        } else if (p < f) {
            double u = (p - w) / (f - w);
            pose.arm = multiply(axisAngle(cut.axis(), cut.angle() * through(u, cut.lead())), top);
        } else {
            // Back to the hold, by way of the cut's way back (round in front of him, at his side).
            pose.arm = path(end, cut.back(), to, smoother((p - f) / (1.0 - f)));
        }
        // The body: coiled by the top of the wind-up, at rest again by the end; it unwinds round the impact (half way there) over
        // longer than the blade's cut, leading it in and carrying on after: the hips and chest drive the arm.
        double in = p < w ? smoother(p / w) : 1.0;
        double out = p < f ? 1.0 : 1.0 - smoother((p - f) / (1.0 - f));
        double i = clampImpact(impact);
        double reach = Math.max(0.02, Math.min(Math.min(i, 1.0 - i) - 0.02, 1.8 * (i - w)));
        double turn = through((p - (i - reach)) / (2.0 * reach), 0.5);
        pose.twist = key(cut.twist(), in, turn, out);
        pose.torsoX = key(cut.torso(), in, turn, out);
        pose.headY = key(cut.head(), in, turn, out);
        pose.headX = -0.6F * pose.torsoX;
        pose.leftArmX = key(cut.leftX(), in, turn, out);
        pose.leftArmY = key(cut.leftY(), in, turn, out);
        pose.leftArmZ = key(cut.leftZ(), in, turn, out);
        pose.leftArmBend = 0.35F * (float)(in * out);
        pose.rightLegX = key(cut.rightLegX(), in, turn, out);
        pose.leftLegX = key(cut.leftLegX(), in, turn, out);
        pose.leftKnee = key(cut.leftKnee(), in, turn, out);
        pose.rightKnee = key(cut.rightKnee(), in, turn, out);
        return pose;
    }

    /** A body channel's keys {at the top of the wind-up, at the impact, at the end of the cut}, eased in, through and out. */
    private static float key(float[] keys, double in, double through, double out) {
        double v;
        if (through <= 0.5) {
            v = keys[0] + (keys[1] - keys[0]) * (through * 2.0);
        } else {
            v = keys[1] + (keys[2] - keys[1]) * ((through - 0.5) * 2.0);
        }
        return (float)(v * in * out);
    }

    /**
     * From the turn {@code a} to {@code b}, {@code t} of the way: the blade's direction along a smooth curve (by way of
     * {@code via} when there is one; along the shortest arc otherwise), its edge turning evenly from one to the other. Exactly
     * {@code a} and {@code b} at the ends.
     */
    static double[] path(double[] a, double[] via, double[] b, double t) {
        if (t <= 0.0) {
            return a;
        }
        if (t >= 1.0) {
            return b;
        }
        double[] da = pointing(a);
        double[] db = pointing(b);
        // 6.0.2: the edge is carried along the way the blade actually goes (it never turns about the blade by itself), and the one
        // turn left over to meet the end's edge (under half a turn, the short way) is spread evenly along it. Until 6.0.2 each
        // end's edge was carried straight to the blade's direction and the turn between them taken afresh every frame, which
        // flipped from one way to the other (the edge jumped half over in a frame) when they came out opposite.
        double[] end = carriedAlong(edge(a), da, via, db, 1.0);
        double[] eb = edge(b);
        double roll = Math.atan2(dot(cross(end, eb), db), dot(end, eb));
        double[] d = direction(da, via, db, t);
        double[] ea = carriedAlong(edge(a), da, via, db, t);
        return aim(d, add(scale(ea, Math.cos(roll * t)), scale(cross(d, ea), Math.sin(roll * t))));
    }

    /** The blade's direction {@code t} of the way from {@code da} to {@code db} (by way of {@code via} when there is one). */
    static double[] direction(double[] da, double[] via, double[] db, double t) {
        return via == null ? arc(da, db, t) : arc(arc(da, unit(via), t), arc(unit(via), db, t), t);
    }

    /** The edge {@code e} (square to {@code da}) carried along the blade's way to {@code t} of it, in {@link #ROLL_STEPS} steps. */
    static double[] carriedAlong(double[] e, double[] da, double[] via, double[] db, double t) {
        double[] at = da;
        double[] out = e;
        for (int k = 1; k <= ROLL_STEPS; k++) {
            double[] next = direction(da, via, db, t * k / ROLL_STEPS);
            out = carried(out, at, next);
            at = next;
        }
        return out;
    }

    /** How finely {@link #path} carries the edge along the way. */
    static final int ROLL_STEPS = 24;

    /** {@code e} (square to {@code from}) carried along the shortest turn from {@code from} to {@code to}: square to {@code to}. */
    static double[] carried(double[] e, double[] from, double[] to) {
        double[] axis = cross(from, to);
        double s = Math.sqrt(dot(axis, axis));
        double c = dot(from, to);
        double[] out;
        if (s < 1.0E-9) {
            out = c > 0.0 ? e : scale(e, -1.0);
        } else {
            out = rotate(axisAngle(axis, Math.atan2(s, c)), e);
        }
        return unit(sub(out, scale(to, dot(out, to))));
    }

    /** The unit vector {@code t} of the way along the shortest arc from {@code a} to {@code b} (both unit). */
    static double[] arc(double[] a, double[] b, double t) {
        double c = Math.max(-1.0, Math.min(1.0, dot(a, b)));
        double angle = Math.acos(c);
        if (angle < 1.0E-6) {
            return a;
        }
        if (Math.PI - angle < 1.0E-4) {
            // Opposite: round by any square way.
            double[] side = unit(sub(UP, scale(a, dot(UP, a))));
            if (dot(side, side) < 0.5) {
                side = unit(sub(FORWARD, scale(a, dot(FORWARD, a))));
            }
            return unit(add(scale(a, Math.cos(Math.PI * t)), scale(side, Math.sin(Math.PI * t))));
        }
        double s = Math.sin(angle);
        return unit(add(scale(a, Math.sin((1.0 - t) * angle) / s), scale(b, Math.sin(t * angle) / s)));
    }

    /** The blade's direction (unit, chest frame) for the turn {@code q}. */
    public static double[] pointing(double[] q) {
        return rotate(q, DOWN);
    }

    /** Which way its edge faces (unit, chest frame) for the turn {@code q}. */
    public static double[] edge(double[] q) {
        return rotate(q, LEFT);
    }

    // ------------------------------------------------------------------ turns

    /**
     * The arm's turn that points the blade along {@code d} (the arm's own +y, down its length) with the edge (the arm's +x) as
     * near {@code e} as it can be, square to {@code d}.
     */
    public static double[] aim(double[] d, double[] e) {
        double[] y = unit(d);
        double[] x = sub(e, scale(y, dot(e, y)));
        if (dot(x, x) < 1.0E-9) {
            x = sub(LEFT, scale(y, dot(LEFT, y)));
            if (dot(x, x) < 1.0E-9) {
                x = sub(BACK, scale(y, dot(BACK, y)));
            }
        }
        x = unit(x);
        double[] z = cross(x, y);
        return fromMatrix(new double[][] {{x[0], y[0], z[0]}, {x[1], y[1], z[1]}, {x[2], y[2], z[2]}});
    }

    /** ModelPart's three angles (x, y, z, applied Rz Ry Rx) for the turn {@code q}. */
    public static float[] euler(double[] q) {
        return BladeCarry.euler(toMatrix(q));
    }

    /** The turn for ModelPart's three angles. */
    public static double[] fromEuler(float x, float y, float z) {
        return fromMatrix(BladeCarry.zyx(x, y, z));
    }

    static double[] axisAngle(double[] axis, double angle) {
        double[] a = unit(axis);
        double s = Math.sin(angle * 0.5);
        return new double[] {Math.cos(angle * 0.5), a[0] * s, a[1] * s, a[2] * s};
    }

    static double[] multiply(double[] a, double[] b) {
        return new double[] {a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3], a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
            a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1], a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]};
    }

    /** The shortest turn from {@code a} to {@code b}, {@code t} of the way (both unit). */
    public static double[] slerp(double[] a, double[] b, double t) {
        double c = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
        double[] to = b;
        if (c < 0.0) {
            c = -c;
            to = new double[] {-b[0], -b[1], -b[2], -b[3]};
        }
        double ka;
        double kb;
        if (c > 0.9995) {
            ka = 1.0 - t;
            kb = t;
        } else {
            double angle = Math.acos(Math.min(1.0, c));
            double s = Math.sin(angle);
            ka = Math.sin((1.0 - t) * angle) / s;
            kb = Math.sin(t * angle) / s;
        }
        double[] out = {a[0] * ka + to[0] * kb, a[1] * ka + to[1] * kb, a[2] * ka + to[2] * kb, a[3] * ka + to[3] * kb};
        double l = Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2] + out[3] * out[3]);
        return new double[] {out[0] / l, out[1] / l, out[2] / l, out[3] / l};
    }

    /** The angle between two turns (radians, 0..pi). */
    public static double between(double[] a, double[] b) {
        double c = Math.abs(a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3]);
        return 2.0 * Math.acos(Math.min(1.0, c));
    }

    static double[] rotate(double[] q, double[] v) {
        double[][] m = toMatrix(q);
        return new double[] {m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    static double[][] toMatrix(double[] q) {
        double w = q[0];
        double x = q[1];
        double y = q[2];
        double z = q[3];
        return new double[][] {{1 - 2 * (y * y + z * z), 2 * (x * y - w * z), 2 * (x * z + w * y)},
            {2 * (x * y + w * z), 1 - 2 * (x * x + z * z), 2 * (y * z - w * x)}, {2 * (x * z - w * y), 2 * (y * z + w * x), 1 - 2 * (x * x + y * y)}};
    }

    static double[] fromMatrix(double[][] m) {
        double trace = m[0][0] + m[1][1] + m[2][2];
        double w;
        double x;
        double y;
        double z;
        if (trace > 0.0) {
            double s = Math.sqrt(trace + 1.0) * 2.0;
            w = 0.25 * s;
            x = (m[2][1] - m[1][2]) / s;
            y = (m[0][2] - m[2][0]) / s;
            z = (m[1][0] - m[0][1]) / s;
        } else if (m[0][0] > m[1][1] && m[0][0] > m[2][2]) {
            double s = Math.sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]) * 2.0;
            w = (m[2][1] - m[1][2]) / s;
            x = 0.25 * s;
            y = (m[0][1] + m[1][0]) / s;
            z = (m[0][2] + m[2][0]) / s;
        } else if (m[1][1] > m[2][2]) {
            double s = Math.sqrt(1.0 + m[1][1] - m[0][0] - m[2][2]) * 2.0;
            w = (m[0][2] - m[2][0]) / s;
            x = (m[0][1] + m[1][0]) / s;
            y = 0.25 * s;
            z = (m[1][2] + m[2][1]) / s;
        } else {
            double s = Math.sqrt(1.0 + m[2][2] - m[0][0] - m[1][1]) * 2.0;
            w = (m[1][0] - m[0][1]) / s;
            x = (m[0][2] + m[2][0]) / s;
            y = (m[1][2] + m[2][1]) / s;
            z = 0.25 * s;
        }
        double l = Math.sqrt(w * w + x * x + y * y + z * z);
        return new double[] {w / l, x / l, y / l, z / l};
    }

    // ------------------------------------------------------------------ vectors

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] unit(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : scale(a, 1.0 / l);
    }

    static double smoother(double t) {
        double x = t < 0.0 ? 0.0 : t > 1.0 ? 1.0 : t;
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }
}
