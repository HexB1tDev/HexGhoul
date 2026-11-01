package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 1.3.33: both hands on a two-handed quinque (pure). The weapon is drawn in the right hand (vanilla's chain: PlayerModel.translateToHand,
 * ItemInHandLayer's Rx(-90) Ry(180) and (1/16, 0.125, -0.625), its model's third person transform, the -0.5 shift); the left arm is
 * pointed every frame at the left grip that chain puts the weapon's left grip at ({@link #reach}), so the left hand stays on it however
 * the right arm, the chest and the body move (a swing, a parry, a kick, a run). The owner: "both hands staying on two-handed weapons".
 *
 * <p>An arm is a straight stick from its shoulder (an investigator has no elbows): its fist is where it is, so a grip nearer or further
 * than its length is met as near as the arm gets, the shoulder reaching a pixel or so toward it (it protracts and draws back, as a real
 * one does). The stances (QuinqueHolds) are solved so that it never has to in them.
 *
 * <p>Frames: model pixels, x to the body's left, y down, z to its back (the chest's frame: the same with the chest's twist and the upper
 * body's lean taken out). An arm's turn as RigPose writes it (ModelPart's Rz Ry Rx) or as where it points ({@link #aim}).
 */
public final class QuinqueGrip {
    private QuinqueGrip() {
    }

    /** The fist's middle a pixel above the end of an arm (its own frame: a wide arm's middle across, a slim one's). */
    static final float FIST_Y = 9.0F;
    /** How far a shoulder reaches toward a grip the stick can't (pixels, each way). */
    static final float SHOULDER_GIVE = 1.5F;

    /** Ry(yaw) Rx(pitch) Ry(roll): an arm turned about its own length, pitched forward (-) or back, swung round to its left (-). */
    public static double[][] aim(double pitch, double yaw, double roll) {
        return RigPose.multiply(RigPose.zyx(0.0, yaw, 0.0), RigPose.multiply(RigPose.zyx(pitch, 0.0, 0.0), RigPose.zyx(0.0, roll, 0.0)));
    }

    /** ModelPart's three angles (Rz Ry Rx) of a turn. */
    public static float[] angles(double[][] m) {
        double sinY = Math.max(-1.0, Math.min(1.0, -m[2][0]));
        double ry = Math.asin(sinY);
        if (Math.abs(sinY) < 0.999999) {
            return new float[] {(float)Math.atan2(m[2][1], m[2][2]), (float)ry, (float)Math.atan2(m[1][0], m[0][0])};
        }
        return new float[] {(float)Math.atan2(-m[1][2], m[1][1]), (float)ry, 0.0F};
    }

    /** {@code p}'s right arm pointed so (in the chest's frame: held, its own turn not kept). */
    public static void aimRight(RigPose p, double pitch, double yaw, double roll) {
        float[] a = angles(aim(pitch, yaw, roll));
        p.rightArmX = a[0];
        p.rightArmY = a[1];
        p.rightArmZ = a[2];
    }

    /** {@code p}'s left arm pointed so. */
    public static void aimLeft(RigPose p, double pitch, double yaw, double roll) {
        float[] a = angles(aim(pitch, yaw, roll));
        p.leftArmX = a[0];
        p.leftArmY = a[1];
        p.leftArmZ = a[2];
    }

    static double[][] transpose(double[][] m) {
        return new double[][] {{m[0][0], m[1][0], m[2][0]}, {m[0][1], m[1][1], m[2][1]}, {m[0][2], m[1][2], m[2][2]}};
    }

    static double[] apply(double[][] m, double[] v) {
        return new double[] {m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    /** An arm's turn as drawn (lean and twist in) for {@code p}, and its pivot (pixels). */
    static double[][] armTurn(RigPose p, boolean right) {
        float[] a = p.transform(right ? "rightArm" : "leftArm", RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        return RigPose.zyx(a[0], a[1], a[2]);
    }

    static double[] pivot(RigPose p, boolean right) {
        float[] at = p.transform(right ? "rightArm" : "leftArm", RigPose.POSITION, right ? -5.0F : 5.0F, 2.0F, 0.0F);
        return new double[] {at[0], at[1], at[2]};
    }

    /** The middle of an arm's fist as drawn (pixels). */
    public static double[] fist(RigPose p, boolean right, boolean slim) {
        double[] o = pivot(p, right);
        double across = (slim ? 0.5 : 1.0) * (right ? -1.0 : 1.0);
        double[] d = apply(armTurn(p, right), new double[] {across, FIST_Y, 0.0});
        return new double[] {o[0] + d[0], o[1] + d[1], o[2] + d[2]};
    }

    /** Where a point of the quinque's model ({@code point}: model pixels) is drawn, held in the right hand of {@code p} (pixels). */
    public static double[] held(RigPose p, QuinqueHolds.Hold h, boolean slim, float[] point) {
        double[][] r = RigPose.multiply(RigPose.zyx(Math.toRadians(h.rotation()[0]), 0.0, 0.0),
            RigPose.multiply(RigPose.zyx(0.0, Math.toRadians(h.rotation()[1]), 0.0), RigPose.zyx(0.0, 0.0, Math.toRadians(h.rotation()[2]))));
        double s = h.scale();
        double[] m = apply(r, new double[] {s * (point[0] / 16.0 - 0.5), s * (point[1] / 16.0 - 0.5), s * (point[2] / 16.0 - 0.5)});
        double[] local = {1.0 / 16.0 + h.translation()[0] / 16.0 + m[0], 0.125 + h.translation()[1] / 16.0 + m[1],
            -0.625 + h.translation()[2] / 16.0 + m[2]};
        // ItemInHandLayer: Rx(-90) then Ry(180).
        double[] layer = apply(RigPose.multiply(RigPose.zyx(Math.toRadians(-90.0), 0.0, 0.0), RigPose.zyx(0.0, Math.PI, 0.0)), local);
        double[] turned = apply(armTurn(p, true), layer);
        double[] o = pivot(p, true);
        // PlayerModel.translateToHand: a slim arm's pivot half a pixel out for the item.
        return new double[] {o[0] + (slim ? 0.5 : 0.0) + 16.0 * turned[0], o[1] + 16.0 * turned[1], o[2] + 16.0 * turned[2]};
    }

    /** Where {@code q}'s left grip is drawn for {@code p} (pixels), or null (one-handed). */
    public static double[] leftGrip(RigPose p, Quinque q, boolean slim) {
        QuinqueHolds.Hold h = q == null ? null : QuinqueHolds.of(q);
        return h == null ? null : held(p, h, slim, h.leftGrip());
    }

    /**
     * {@code p}'s left arm pointed at {@code q}'s left grip (held: its own turn not kept), keeping as much of the arm's roll as the turn
     * it had allows; its shoulder reaching up to {@link #SHOULDER_GIVE} toward a grip the stick doesn't meet. Returns how far the fist
     * still misses (pixels; 0 for a one-handed quinque).
     */
    public static float reach(RigPose p, Quinque q, boolean slim) {
        QuinqueHolds.Hold h = q == null ? null : QuinqueHolds.of(q);
        if (h == null) {
            return 0.0F;
        }
        p.leftArmHold = 1.0F;
        double[] fist = {slim ? 0.5 : 1.0, FIST_Y, 0.0};
        double arm = length(fist);
        double[][] lean = p.leanTurn();
        double[][] unlean = lean == null ? null : transpose(lean);
        double miss = 0.0;
        for (int pass = 0; pass < 2; pass++) {
            double[] from = pivot(p, false);
            double[] grip = slide(held(p, h, slim, h.leftGrip()), held(p, h, slim, h.slideFrom()), held(p, h, slim, h.slideTo()), from, arm);
            double[] to = {grip[0] - from[0], grip[1] - from[1], grip[2] - from[2]};
            double far = length(to);
            if (!(far > 1.0E-6)) {
                return 0.0F;
            }
            miss = far - arm;
            if (pass == 0 && Math.abs(miss) > 0.05) {
                // The shoulder reaches toward it (its shrug is in the chest's frame before the lean).
                double give = Math.max(-SHOULDER_GIVE, Math.min(SHOULDER_GIVE, miss));
                double[] way = {to[0] / far, to[1] / far, to[2] / far};
                if (unlean != null) {
                    way = apply(unlean, way);
                }
                p.leftShoulderY += (float)(way[1] * give);
                p.leftShoulderZ += (float)(way[2] * give);
                continue;
            }
            double[][] had = armTurn(p, false);
            double[] d0 = apply(had, fist);
            double n0 = length(d0);
            double[] a = {d0[0] / n0, d0[1] / n0, d0[2] / n0};
            double[] b = {to[0] / far, to[1] / far, to[2] / far};
            double[][] turn = RigPose.multiply(between(a, b), had);
            // Back into RigPose's terms: the drawn turn is lean . Ry(twist) . own.
            double[][] chest = unlean == null ? turn : RigPose.multiply(unlean, turn);
            float[] own = angles(RigPose.multiply(RigPose.zyx(0.0, -p.twist, 0.0), chest));
            p.leftArmX = own[0];
            p.leftArmY = own[1];
            p.leftArmZ = own[2];
        }
        return (float)miss;
    }

    /**
     * Where along the stretch {@code a}..{@code b} (drawn) the left hand takes hold: at {@code grip} if the arm's {@code arm} reaches it
     * from {@code from}, otherwise the nearest point along it to there that it does reach (the spear slides through the front hand as it
     * is thrust), or the one nearest to reaching if none does.
     */
    static double[] slide(double[] grip, double[] a, double[] b, double[] from, double arm) {
        double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double len2 = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        if (len2 < 1.0E-6) {
            return grip;
        }
        double[] fa = {a[0] - from[0], a[1] - from[1], a[2] - from[2]};
        double[] fg = {grip[0] - from[0], grip[1] - from[1], grip[2] - from[2]};
        double base = Math.max(0.0, Math.min(1.0, ((grip[0] - a[0]) * ab[0] + (grip[1] - a[1]) * ab[1] + (grip[2] - a[2]) * ab[2]) / len2));
        if (Math.abs(length(fg) - arm) < 0.05) {
            return grip;
        }
        // |fa + t ab| = arm: len2 t^2 + 2 (fa.ab) t + |fa|^2 - arm^2 = 0.
        double half = fa[0] * ab[0] + fa[1] * ab[1] + fa[2] * ab[2];
        double c = fa[0] * fa[0] + fa[1] * fa[1] + fa[2] * fa[2] - arm * arm;
        double disc = half * half - len2 * c;
        double t;
        if (disc >= 0.0) {
            double r = Math.sqrt(disc);
            double t1 = (-half - r) / len2;
            double t2 = (-half + r) / len2;
            double best = Double.NaN;
            for (double root : new double[] {t1, t2}) {
                if (root >= 0.0 && root <= 1.0 && (Double.isNaN(best) || Math.abs(root - base) < Math.abs(best - base))) {
                    best = root;
                }
            }
            if (Double.isNaN(best)) {
                // Both ways past an end: the end nearer to reaching.
                best = Math.abs(t1 - Math.max(0.0, Math.min(1.0, t1))) < Math.abs(t2 - Math.max(0.0, Math.min(1.0, t2)))
                    ? Math.max(0.0, Math.min(1.0, t1)) : Math.max(0.0, Math.min(1.0, t2));
            }
            t = best;
        } else {
            // Out of reach all along it: its nearest point.
            t = Math.max(0.0, Math.min(1.0, -half / len2));
        }
        return new double[] {a[0] + ab[0] * t, a[1] + ab[1] * t, a[2] + ab[2] * t};
    }

    /** A turn of {@code angle} about the unit {@code axis} (right handed). */
    public static double[][] about(double[] axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double t = 1.0 - c;
        double x = axis[0];
        double y = axis[1];
        double z = axis[2];
        return new double[][] {{c + x * x * t, x * y * t - z * s, x * z * t + y * s}, {y * x * t + z * s, c + y * y * t, y * z * t - x * s},
            {z * x * t - y * s, z * y * t + x * s, c + z * z * t}};
    }

    /**
     * 1.3.36: {@code p}'s left arm pointed so its fist takes hold of a point on the right arm ({@code onRight}: the right arm's own frame,
     * pixels), {@code weight} of the way from where it was (a hand on the other forearm).
     */
    public static void reachLeftTo(RigPose p, double[] onRight, float weight) {
        if (weight <= 0.0F) {
            return;
        }
        double[] o = pivot(p, true);
        double[] d = apply(armTurn(p, true), onRight);
        double[] target = {o[0] + d[0], o[1] + d[1], o[2] + d[2]};
        float[] was = {p.leftArmX, p.leftArmY, p.leftArmZ};
        float hold = p.leftArmHold;
        p.leftArmHold = 1.0F;
        double[] fist = {1.0, FIST_Y, 0.0};
        double[] from = pivot(p, false);
        double[] to = {target[0] - from[0], target[1] - from[1], target[2] - from[2]};
        double far = length(to);
        if (!(far > 1.0E-6)) {
            p.leftArmHold = hold;
            return;
        }
        double[][] had = armTurn(p, false);
        double[] d0 = apply(had, fist);
        double n0 = length(d0);
        double[][] turn = RigPose.multiply(between(new double[] {d0[0] / n0, d0[1] / n0, d0[2] / n0}, new double[] {to[0] / far, to[1] / far, to[2] / far}),
            had);
        double[][] lean = p.leanTurn();
        double[][] chest = lean == null ? turn : RigPose.multiply(transpose(lean), turn);
        float[] own = angles(RigPose.multiply(RigPose.zyx(0.0, -p.twist, 0.0), chest));
        p.leftArmX = was[0] + (own[0] - was[0]) * weight;
        p.leftArmY = was[1] + (own[1] - was[1]) * weight;
        p.leftArmZ = was[2] + (own[2] - was[2]) * weight;
        p.leftArmHold = Math.max(hold, weight);
    }

    /** The least turn taking unit {@code a} to unit {@code b}. */
    static double[][] between(double[] a, double[] b) {
        double[] axis = {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
        double sin = length(axis);
        double cos = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        if (sin < 1.0E-9) {
            if (cos > 0.0) {
                return new double[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
            }
            // Opposite: half a turn about any axis square to it.
            double[] any = Math.abs(a[0]) < 0.9 ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
            axis = new double[] {a[1] * any[2] - a[2] * any[1], a[2] * any[0] - a[0] * any[2], a[0] * any[1] - a[1] * any[0]};
            sin = 0.0;
            cos = -1.0;
            double n = length(axis);
            axis = new double[] {axis[0] / n, axis[1] / n, axis[2] / n};
        } else {
            axis = new double[] {axis[0] / sin, axis[1] / sin, axis[2] / sin};
        }
        double t = 1.0 - cos;
        double x = axis[0];
        double y = axis[1];
        double z = axis[2];
        return new double[][] {{cos + x * x * t, x * y * t - z * sin, x * z * t + y * sin}, {y * x * t + z * sin, cos + y * y * t, y * z * t - x * sin},
            {z * x * t - y * sin, z * y * t + x * sin, cos + z * z * t}};
    }
}
