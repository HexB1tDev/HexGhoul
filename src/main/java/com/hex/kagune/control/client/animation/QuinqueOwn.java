package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.Quinque;
import com.hex.kagune.control.quinque.WhipPath;

/** 3.1.3: camera-space quinque attacks, shared by the renderer and offline camera checks. Camera +x right, +y up, -z forward. */
public final class QuinqueOwn {
    private QuinqueOwn() {
    }

    /** Vanilla swing observations, whether made by the body layer or directly by the first-person renderer. */
    public static final class Combo {
        private boolean swinging;
        private int last = Integer.MAX_VALUE;
        private int step = -1;
        private double began = -1.0E9;

        public void observe(boolean active, int swingTime, double clock) {
            if (clock < this.began) {
                this.swinging = false;
                this.step = -1;
            }
            if (active && (!this.swinging || swingTime < this.last)) {
                this.step = Quinque.next(this.step, (long)(clock - this.began));
                this.began = clock;
            }
            this.swinging = active;
            this.last = swingTime;
        }

        public int step() { return this.step; }
        public double began() { return this.began; }
        public double age(double clock) { return clock - this.began; }
    }

    /** Yaw, pitch, roll (degrees), then offsets from the ready hand (blocks). */
    public static double[] arm(Arm arm, int step, Arm.Ability ability, double age) {
        if (step >= 0) {
            Arm.Strike s = arm.strike(step);
            int kind = s.blow() == Arm.Strike.STAB ? 2 : arm == Arm.WHIP ? (step == 2 ? 1 : 0)
                : s.arc() >= 80.0 ? 0 : 1;
            return motion(kind, step % 2 == 1, s.total(), s.land(), age, 1);
        }
        if (ability == null) {
            return new double[6];
        }
        int kind = switch (ability) { case JAVELIN, PIERCE -> 2; case QUAKE, LEAP -> 1; default -> 0; };
        int repeats = ability == Arm.Ability.FLAY || ability == Arm.Ability.WHIRL ? ability.hits.length : 1;
        return motion(kind, false, ability.total, ability.hits[ability.hits.length - 1], age, repeats);
    }

    public static double[] legacy(Quinque q, int step, double age) {
        int kind = q == Quinque.SPEAR || q == Quinque.LAUNCHER || q == Quinque.DAWN && step == Quinque.FINISHER ? 2
            : step == Quinque.FINISHER || q == Quinque.MACE && step == 0 ? 1 : 0;
        return motion(kind, step % 2 == 1, q.swing, q.swing * 0.5, age, 1);
    }

    private static double[] motion(int kind, boolean back, double total, double land, double age, int repeats) {
        double[] out = new double[6];
        if (!Double.isFinite(age) || age < 0.0 || age >= total) {
            return out;
        }
        double a;
        if (repeats > 1) {
            double in = ease(age / 2.0);
            double off = age <= land ? 1.0 : 1.0 - ease((age - land) / (total - land));
            a = Math.sin(age / land * repeats * Math.PI - Math.PI * 0.5) * in * off;
        } else if (age < 0.6 * land) {
            a = -ease(age / (0.6 * land));
        } else if (age < land) {
            a = -1.0 + 2.0 * ease((age - 0.6 * land) / (0.4 * land));
        } else {
            a = 1.0 - ease((age - land) / (total - land));
        }
        switch (kind) {
            case 0 -> {
                out[0] = (back ? -1.0 : 1.0) * (a < 0 ? 25.0 : 35.0) * a;
                out[2] = -18.0 * a;
                out[3] = (back ? 0.15 : -0.30) * a;
            }
            case 1 -> {
                out[1] = (a < 0 ? -25.0 : -45.0) * a;
                out[4] = -0.08 * a;
            }
            default -> {
                out[5] = (a < 0 ? -0.12 : -0.45) * a;
                out[1] = -8.0 * Math.max(0, a);
            }
        }
        return out;
    }

    public static float scale(Arm arm) {
        return arm == Arm.CRYSTAL_SPEAR ? 0.55F : arm == Arm.WHIP ? 0.9F : 0.68F;
    }

    /** The exact rotation chain used by the hand renderer, including the kagune mesh's ready orientation. */
    public static double[][] turn(Arm arm, double[] m) {
        double[][] r = RigPose.multiply(RigPose.zyx(0, Math.toRadians(m[0]), 0),
            RigPose.multiply(RigPose.zyx(Math.toRadians(m[1]), 0, 0), RigPose.zyx(0, 0, Math.toRadians(m[2]))));
        if (arm != null) {
            r = RigPose.multiply(r, RigPose.multiply(RigPose.zyx(0, 0, Math.toRadians(15)),
                RigPose.multiply(RigPose.zyx(Math.toRadians(-30), 0, 0),
                RigPose.multiply(RigPose.zyx(0, Math.toRadians(-50), 0), RigPose.zyx(Math.PI * 0.5, 0, 0)))));
        }
        return r;
    }

    public static double[] point(Arm arm, double[] motion, double[] raw, double grown, double equip) {
        double[] p = QuinqueGrip.apply(turn(arm, motion), raw);
        double scale = arm == null ? grown : scale(arm) * grown;
        return new double[] {0.42 + motion[3] + p[0] * scale, -0.43 + motion[4] - 0.6 * equip + p[1] * scale,
            -0.95 + motion[5] + p[2] * scale};
    }

    /** The whip's complete visible path, attached to the exact knot used to draw its handle. */
    public static double[][] whip(double[] motion, int step, Arm.Ability ability, double age, boolean master, double shown, double equip, double time) {
        double[] root = point(Arm.WHIP, motion, new double[] {0, 0, -ArmMesh.WHIP_ROOT}, shown, equip);
        double length = WhipPath.length(master);
        double[][] rest = WhipPath.rest(length, Math.max(0.2, 1.62 + root[1]), time);
        double[][] active = rest;
        double weight = 0;
        if (step >= 0 && age >= 0 && age < Arm.WHIP.strike(step).total() + 4) {
            Arm.Strike s = Arm.WHIP.strike(step);
            active = WhipPath.lash(step, Math.min(1, age / s.total()), s.land() / (double)s.total(), length, time);
            weight = weight(age, s.total());
        } else if (ability == Arm.Ability.FLAY && age >= 0 && age < ability.total) {
            active = WhipPath.flay(age, length, time);
            weight = weight(age, ability.total - 4);
        } else if (ability == Arm.Ability.BIND && age >= 0 && age < ability.total) {
            active = WhipPath.bind(age, new double[] {4, 0, 0}, 0.35, length, time);
            weight = weight(age, ability.total - 4);
        }
        double[][] out = new double[rest.length][];
        for (int i = 0; i < out.length; i++) {
            double fwd = rest[i][0] + (active[i][0] - rest[i][0]) * weight;
            double left = rest[i][1] + (active[i][1] - rest[i][1]) * weight;
            double up = rest[i][2] + (active[i][2] - rest[i][2]) * weight;
            out[i] = new double[] {root[0] - left * shown, root[1] + up * shown, root[2] - fwd * shown};
        }
        out[0] = root;
        return out;
    }

    private static double weight(double age, double total) {
        return ease(Math.min(Math.min(1, age / 2), age <= total ? 1 : 1 - (age - total) / 4));
    }

    private static double ease(double t) {
        double c = Math.max(0, Math.min(1, t));
        return c * c * (3 - 2 * c);
    }
}
