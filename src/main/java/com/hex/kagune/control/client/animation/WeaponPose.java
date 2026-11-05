package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.Quinque;

/**
 * 3.0.0: how an investigator holds and swings its kagune-made weapon ({@link Arm}; pure, so the previews pose exactly what the game does).
 *
 * <p>Each is held as the quinque it takes after is ({@link #base}: the whip as the one-handed blade, the crystal spear as the spear, low at
 * the hip, the flesh hammer as the mace, on the right shoulder, the bone greatsword as the katana, in chudan), in the stance solved for that
 * quinque ({@link QuinqueHolds}), and drawn along that quinque's own line in the hand ({@link #meshToArm}): so the left hand is on its grip
 * wherever {@link QuinqueGrip#reach} puts it.
 *
 * <p>Every blow of its string and every move of its own is a run of keys, each saying where the right arm points and where the weapon
 * points (both in the chest's frame: {@link #solve} turns the arm to match), and how the body turns, leans, steps in, sinks and spins.
 * The keys are timed by the blow's own landing: the key at 0.5 is the moment it lands ({@code Arm.Strike.land}).
 */
public final class WeaponPose {
    private WeaponPose() {
    }

    /** The quinque each is held as. */
    public static Quinque base(Arm arm) {
        return switch (arm) {
            case WHIP -> Quinque.BLADE;
            case CRYSTAL_SPEAR -> Quinque.SPEAR;
            case FLESH_HAMMER -> Quinque.MACE;
            case BONE_SWORD -> Quinque.KATANA;
        };
    }

    // ---------------------------------------------------------------- where the weapon is in the hand

    /** Item model directions: along the quinque's line (its point, its head), across it in its plane, through it. */
    private static final double[] ALONG = {Math.sqrt(0.5), Math.sqrt(0.5), 0.0};
    private static final double[] ACROSS = {-Math.sqrt(0.5), Math.sqrt(0.5), 0.0};
    private static final double[] THROUGH = {0.0, 0.0, -1.0};

    private static final double[][][] PLACED = new double[Arm.values().length][][];

    /**
     * Where a point of {@code arm}'s mesh ({@code ArmMesh}: blocks, the hand's frame) is in the right arm's own frame (blocks, from the
     * arm's pivot, as {@code ModelPart.translateAndRotate} leaves it; a slim arm's half-pixel shift is the caller's): rows 0..2 the turn,
     * row 3 where the fist is. Put where the quinque it takes after is put, by the same chain (PlayerModel.translateToHand, ItemInHandLayer's
     * Rx(-90) Ry(180) and (1/16, 0.125, -0.625), its third person transform, the -0.5 shift), its line along that quinque's line from its
     * right grip, true size.
     */
    public static synchronized double[][] meshToArm(Arm arm) {
        if (PLACED[arm.ordinal()] != null) {
            return PLACED[arm.ordinal()];
        }
        QuinqueHolds.Hold h = QuinqueHolds.of(base(arm));
        double[][] rot = RigPose.multiply(RigPose.zyx(Math.toRadians(h.rotation()[0]), 0.0, 0.0),
            RigPose.multiply(RigPose.zyx(0.0, Math.toRadians(h.rotation()[1]), 0.0), RigPose.zyx(0.0, 0.0, Math.toRadians(h.rotation()[2]))));
        double[][] layer = RigPose.multiply(RigPose.zyx(Math.toRadians(-90.0), 0.0, 0.0), RigPose.zyx(0.0, Math.PI, 0.0));
        double[][] chain = RigPose.multiply(layer, rot);
        // The mesh's hand frame (x through, y down across, z back along) onto the item's lines: a proper turn.
        double[][] h3 = new double[3][3];
        for (int r = 0; r < 3; r++) {
            h3[r][0] = THROUGH[r];
            h3[r][1] = -ACROSS[r];
            h3[r][2] = -ALONG[r];
        }
        double[][] turn = RigPose.multiply(chain, h3);
        double s = h.scale();
        double[] grip = {s * (h.rightGrip()[0] / 16.0 - 0.5), s * (h.rightGrip()[1] / 16.0 - 0.5), s * (h.rightGrip()[2] / 16.0 - 0.5)};
        double[] g = QuinqueGrip.apply(rot, grip);
        double[] local = {1.0 / 16.0 + h.translation()[0] / 16.0 + g[0], 0.125 + h.translation()[1] / 16.0 + g[1],
            -0.625 + h.translation()[2] / 16.0 + g[2]};
        double[] fist = QuinqueGrip.apply(layer, local);
        double[][] out = {turn[0], turn[1], turn[2], fist};
        PLACED[arm.ordinal()] = out;
        return out;
    }

    /** {@code point} (the mesh's hand frame, blocks) in the right arm's frame (blocks). */
    public static double[] onArm(double[][] placed, double[] point) {
        double[] t = QuinqueGrip.apply(new double[][] {placed[0], placed[1], placed[2]}, point);
        return new double[] {placed[3][0] + t[0], placed[3][1] + t[1], placed[3][2] + t[2]};
    }

    /** {@code normal} (the mesh's hand frame) in the right arm's frame. */
    public static double[] turnOnArm(double[][] placed, double[] normal) {
        return QuinqueGrip.apply(new double[][] {placed[0], placed[1], placed[2]}, normal);
    }

    /** The weapon's line (its point's way: the mesh's -z) in the right arm's frame, unit. */
    static double[] line(Arm arm) {
        double[] d = turnOnArm(meshToArm(arm), new double[] {0.0, 0.0, -1.0});
        double n = QuinqueGrip.length(d);
        return new double[] {d[0] / n, d[1] / n, d[2] / n};
    }

    // ---------------------------------------------------------------- pointing the arm

    /** A way in the chest's frame (x to its left, y down, z back) from an azimuth ({@code az}: + to its left) and elevation (+ up), degrees. */
    public static double[] way(double az, double el) {
        double a = Math.toRadians(az);
        double e = Math.toRadians(el);
        return new double[] {Math.cos(e) * Math.sin(a), -Math.sin(e), -Math.cos(e) * Math.cos(a)};
    }

    /** {@code w}'s azimuth and elevation (degrees). */
    public static double[] angles(double[] w) {
        double n = QuinqueGrip.length(w);
        double el = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, -w[1] / n))));
        double az = Math.toDegrees(Math.atan2(w[0], -w[2]));
        return new double[] {az, el};
    }

    /**
     * The right arm's turn (chest frame) pointing it along {@code arm} with {@code arm}'s weapon pointing as near {@code weapon} as it can:
     * the arm's length laid on its way, then turned about it to bring the weapon round.
     */
    public static double[][] solve(Arm held, double[] arm, double[] weapon) {
        double[] a = unit(arm);
        double[][] r1 = QuinqueGrip.between(new double[] {0.0, 1.0, 0.0}, a);
        double[] d = QuinqueGrip.apply(r1, line(held));
        double[] u1 = flat(d, a);
        double[] u2 = flat(unit(weapon), a);
        if (QuinqueGrip.length(u1) < 1.0E-6 || QuinqueGrip.length(u2) < 1.0E-6) {
            return r1;
        }
        u1 = unit(u1);
        u2 = unit(u2);
        double cos = Math.max(-1.0, Math.min(1.0, u1[0] * u2[0] + u1[1] * u2[1] + u1[2] * u2[2]));
        double[] c = {u1[1] * u2[2] - u1[2] * u2[1], u1[2] * u2[0] - u1[0] * u2[2], u1[0] * u2[1] - u1[1] * u2[0]};
        double sign = c[0] * a[0] + c[1] * a[1] + c[2] * a[2] < 0.0 ? -1.0 : 1.0;
        return RigPose.multiply(QuinqueGrip.about(a, sign * Math.acos(cos)), r1);
    }

    /**
     * {@code p}'s right arm turned so {@code held} points along {@code weapon} exactly, the arm as near {@code arm} as it can be (it holds
     * the weapon at a set angle to itself: the arm lies on that cone about the weapon's line, on the side {@code arm} is).
     */
    public static void point(RigPose p, Arm held, double[] arm, double[] weapon) {
        double[] w = unit(weapon);
        double[] line = line(held);
        double cone = Math.acos(Math.max(-1.0, Math.min(1.0, line[1])));
        double[] side = flat(unit(arm), w);
        if (QuinqueGrip.length(side) < 1.0E-6) {
            side = flat(Math.abs(w[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0}, w);
        }
        side = unit(side);
        double[] lay = {Math.cos(cone) * w[0] + Math.sin(cone) * side[0], Math.cos(cone) * w[1] + Math.sin(cone) * side[1],
            Math.cos(cone) * w[2] + Math.sin(cone) * side[2]};
        float[] a = QuinqueGrip.angles(solve(held, lay, w));
        p.rightArmX = a[0];
        p.rightArmY = a[1];
        p.rightArmZ = a[2];
        p.rightArmHold = 1.0F;
    }

    /** Where the arm and the weapon point in {@code arm}'s stance: {arm az, arm el, weapon az, weapon el} (degrees). */
    public static double[] stanceWays(Arm arm) {
        QuinqueHolds.Hold h = QuinqueHolds.of(base(arm));
        double[][] r = QuinqueGrip.aim(h.pitch(), h.yaw(), h.roll());
        double[] a = angles(QuinqueGrip.apply(r, new double[] {0.0, 1.0, 0.0}));
        double[] w = angles(QuinqueGrip.apply(r, line(arm)));
        return new double[] {a[0], a[1], w[0], w[1]};
    }

    // ---------------------------------------------------------------- its blows and its moves

    /*
     * 3.0.1 (the owner, of the overhead smash: "How does animations like this pass?"): rebuilt. Every key says where the weapon and the
     * arm point relative to the foe (azimuth: + to its left; elevation: + up; degrees, turned with the body's own spin), how the chest
     * twists against the hips, how far the body leans (forward, sideways: never more than a man leans) and spins, and its feet: a stride
     * (the front foot out, the back one planted where it stood: the body carried forward over it and lowered only as far as the stride
     * lowers it, so no foot sinks or slides) and how wide they stand. The head stays on the foe. A one-handed weapon's free arm swings
     * against the blow. Each blow is an anticipation, a windup, the blow quickening, the impact, the follow-through, settling, and back.
     */

    private static final int T = 0;
    private static final int W_AZ = 1;
    private static final int W_EL = 2;
    private static final int A_AZ = 3;
    private static final int A_EL = 4;
    private static final int TWIST = 5;
    private static final int LEAN = 6;
    private static final int SIDE = 7;
    private static final int SPIN = 8;
    private static final int STEP = 9;
    private static final int SPREAD = 10;
    private static final int L_AZ = 11;
    private static final int L_EL = 12;
    /** Which foot is planted in its stride: +1 the back one (the body carried forward over the front), -1 the front (its weight rocked back). */
    private static final int SHIFT = 13;
    private static final int CHANNELS = 14;
    /** The most a body leans forward or back, and to a side (radians): further, and straight legs read as folding at the hips. */
    static final double MOST_LEAN = 0.26;
    static final double MOST_SIDE = 0.2;
    /** Its stance's feet: the left a little forward, a little apart (radians). */
    static final double STANCE_STEP = 0.15;
    static final double STANCE_SPREAD = 0.07;
    /** A one-handed weapon's free arm at rest (azimuth, elevation). */
    static final double[] FREE_ARM = {25.0, -72.0};

    /** One key, its unset channels NaN until it is filled in from its stance. */
    static final class K {
        final double[] v = new double[CHANNELS];

        K(double t) {
            java.util.Arrays.fill(this.v, Double.NaN);
            this.v[T] = t;
        }

        K w(double az, double el) {
            this.v[W_AZ] = az;
            this.v[W_EL] = el;
            return this;
        }

        K a(double az, double el) {
            this.v[A_AZ] = az;
            this.v[A_EL] = el;
            return this;
        }

        K tw(double x) {
            this.v[TWIST] = x;
            return this;
        }

        K ln(double x) {
            this.v[LEAN] = x;
            return this;
        }

        K sd(double x) {
            this.v[SIDE] = x;
            return this;
        }

        K sp(double x) {
            this.v[SPIN] = x;
            return this;
        }

        K st(double x) {
            this.v[STEP] = x;
            return this;
        }

        K spr(double x) {
            this.v[SPREAD] = x;
            return this;
        }

        K l(double az, double el) {
            this.v[L_AZ] = az;
            this.v[L_EL] = el;
            return this;
        }

        /** Its weight rocked back onto its back foot (the front one planted): drawn back before a blow. */
        K bk() {
            this.v[SHIFT] = -1.0;
            return this;
        }
    }

    private static K k(double t) {
        return new K(t);
    }

    /** Its stance as a key at {@code t} (spun {@code spin}: a whole turn ends as it began). */
    private static K rest(double t, double spin) {
        return k(t).sp(spin);
    }

    private static K rest(double t) {
        return k(t);
    }

    /** {@code arm}'s stance: where its weapon and arm point relative to the foe {weapon az, el, arm az, el}. */
    static double[] stanceToFoe(Arm arm) {
        double[] w = stanceWays(arm);
        double turn = Math.toDegrees(QuinqueHolds.of(base(arm)).twist());
        return new double[] {w[2] - turn, w[3], w[0] - turn, w[1]};
    }

    /** {@code keys} filled in: every unset channel its stance's. */
    static double[][] filled(Arm arm, K... keys) {
        double[] stance = stanceToFoe(arm);
        double[] defaults = new double[CHANNELS];
        defaults[W_AZ] = stance[0];
        defaults[W_EL] = stance[1];
        defaults[A_AZ] = stance[2];
        defaults[A_EL] = stance[3];
        defaults[STEP] = STANCE_STEP;
        defaults[SPREAD] = STANCE_SPREAD;
        defaults[L_AZ] = FREE_ARM[0];
        defaults[L_EL] = FREE_ARM[1];
        defaults[SHIFT] = 1.0;
        double[][] out = new double[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            out[i] = keys[i].v.clone();
            for (int c = 1; c < CHANNELS; c++) {
                if (Double.isNaN(out[i][c])) {
                    out[i][c] = defaults[c];
                }
            }
        }
        return out;
    }

    private static final double TURN = Math.PI * 2.0;
    private static final double HALF = Math.PI;

    /** Its {@code step}th blow's keys, the blow landing at 0.5. */
    static double[][] keys(Arm arm, int step) {
        int n = Math.floorMod(step, arm.length());
        return switch (arm) {
            case WHIP -> switch (n) {
                // Forehand: the arm drawn back up to its right, whipped across in front with a stride, on round to its left.
                case 0 -> filled(arm, rest(0), k(0.12).w(30, 10).a(25, -45).tw(-0.1).l(-20, -60),
                    k(0.3).w(-120, 25).a(-80, 15).tw(0.55).ln(-0.08).sd(0.05).st(0.1).l(40, -30),
                    k(0.42).w(-60, 12).a(-45, 0).tw(0.3).ln(0.02).st(0.2).l(10, -45),
                    k(0.5).w(10, 0).a(5, -5).tw(-0.15).ln(0.12).st(0.3).l(-50, -40),
                    k(0.6).w(70, -10).a(55, -20).tw(-0.45).ln(0.15).sd(-0.05).st(0.3).l(-40, -55),
                    k(0.75).w(50, -5).a(40, -35).tw(-0.25).ln(0.08).st(0.25).l(-10, -65), rest(1));
                // Backhand: from across its body on its left, out to its right.
                case 1 -> filled(arm, rest(0), k(0.12).w(-10, 10).a(5, -45).tw(0.1),
                    k(0.3).w(110, 15).a(65, 5).tw(-0.55).ln(-0.05).sd(-0.05).st(0.12).l(-30, -40),
                    k(0.42).w(60, 8).a(40, 0).tw(-0.3).st(0.2).l(0, -50),
                    k(0.5).w(-10, 0).a(-10, -5).tw(0.15).ln(0.1).st(0.25).l(30, -50),
                    k(0.6).w(-75, -8).a(-55, -15).tw(0.45).ln(0.12).sd(0.05).st(0.25).l(45, -45),
                    k(0.75).w(-45, -5).a(-35, -30).tw(0.25).ln(0.06).st(0.2).l(30, -60), rest(1));
                // Overhead: a dip, raised high and back over its head, cracked down in front with a long stride.
                case 2 -> filled(arm, rest(0), k(0.12).w(10, -20).a(10, -60).ln(0.05),
                    k(0.3).w(0, 150).a(0, 140).tw(0.1).ln(-0.15).st(0.2).bk().l(10, -20),
                    k(0.42).w(0, 70).a(0, 90).ln(-0.05).st(0.15).l(0, -40),
                    k(0.5).w(0, -25).a(0, -5).ln(0.22).st(0.32).l(-20, -70),
                    k(0.6).w(0, -55).a(0, -30).ln(0.25).st(0.32).l(-15, -70),
                    k(0.75).w(5, -30).a(5, -45).ln(0.12).st(0.25), rest(1));
                // Low: sunk in a wide stride, swept along the ground from its right across to its left.
                case 3 -> filled(arm, rest(0), k(0.12).w(20, -5).a(15, -50),
                    k(0.3).w(-110, -25).a(-80, -35).tw(0.5).ln(0.2).sd(0.08).st(0.25).spr(0.2).l(40, -20),
                    k(0.42).w(-50, -30).a(-40, -40).tw(0.25).ln(0.25).st(0.3).spr(0.22).l(20, -30),
                    k(0.5).w(15, -35).a(10, -45).tw(-0.1).ln(0.28).st(0.32).spr(0.22).l(-20, -35),
                    k(0.6).w(75, -30).a(55, -40).tw(-0.45).ln(0.25).sd(-0.06).st(0.3).spr(0.2).l(-40, -40),
                    k(0.75).w(50, -20).a(40, -45).tw(-0.25).ln(0.12).st(0.25).spr(0.12), rest(1));
                // Double: across to its left, wrapped round, and straight back across.
                case 4 -> filled(arm, rest(0), k(0.15).w(-100, 20).a(-70, 10).tw(0.45).ln(-0.05).st(0.12).l(40, -35),
                    k(0.35).w(-40, 10).a(-30, 0).tw(0.2).st(0.2), k(0.5).w(40, 0).a(30, -10).tw(-0.3).ln(0.1).st(0.25).l(-40, -45),
                    k(0.62).w(95, 10).a(60, 0).tw(-0.5).ln(0.05).st(0.25).l(-40, -40),
                    k(0.8).w(-30, 0).a(-25, -10).tw(0.3).ln(0.12).st(0.3).l(40, -45),
                    k(0.9).w(-20, -10).a(-10, -35).tw(0.15).ln(0.06).st(0.2), rest(1));
                // A whole turn of the body to its left, the arm out to its right, the tendril trailing round.
                default -> filled(arm, rest(0), k(0.1).w(-40, 10).a(-50, -20).tw(0.3).st(0.1).sp(-0.3).l(30, -40),
                    k(0.25).w(-95, 5).a(-90, -5).tw(0.15).sp(0.5).st(0.15).spr(0.1).l(60, -20),
                    k(0.5).w(-95, 5).a(-90, 0).ln(0.05).sp(HALF).st(0.15).spr(0.12).l(70, -15),
                    k(0.7).w(-95, 5).a(-90, 0).sp(1.6 * Math.PI).st(0.15).spr(0.12).l(70, -15),
                    k(0.85).w(-60, 0).a(-60, -20).sp(TURN - 0.2).st(0.15).l(40, -40), rest(1, TURN));
            };
            case CRYSTAL_SPEAR -> switch (n) {
                // A thrust along its own line: drawn back (the chest coiling, weight back), driven out with a stride, recovered.
                case 0 -> filled(arm, rest(0), k(0.12).w(0, -8).a(5, -40).tw(0.1),
                    k(0.3).w(0, -3).a(15, -55).tw(0.65).ln(-0.08).st(0.28).bk(),
                    k(0.42).w(0, 2).a(5, -25).tw(0.0).st(0.25),
                    k(0.5).w(0, 5).a(0, -5).tw(-0.45).ln(0.2).st(0.4),
                    k(0.6).w(0, 5).a(0, -5).tw(-0.48).ln(0.22).st(0.4),
                    k(0.75).w(0, 2).a(2, -20).tw(-0.2).ln(0.1).st(0.3), rest(1));
                // Two: at the middle, drawn half back, at the face.
                case 1 -> filled(arm, rest(0), k(0.25).w(0, -3).a(15, -55).tw(0.65).ln(-0.08).st(0.28).bk(),
                    k(0.5).w(0, 3).a(0, -5).tw(-0.45).ln(0.2).st(0.4),
                    k(0.62).w(0, 5).a(10, -40).tw(0.4).ln(-0.02).st(0.3).bk(),
                    k(0.8).w(0, 14).a(0, 5).tw(-0.45).ln(0.15).st(0.42),
                    k(0.9).w(0, 12).a(2, -5).tw(-0.4).ln(0.12).st(0.4), rest(1));
                // Rising: its head swept from low on its right up across to high on its left, as a staff is.
                case 2 -> filled(arm, rest(0), k(0.12).w(-10, -10).a(0, -40).tw(0.15),
                    k(0.3).w(-80, -35).a(-30, -50).tw(0.55).ln(0.12).st(0.2).spr(0.12),
                    k(0.42).w(-40, -15).a(-10, -35).tw(0.25).ln(0.08).st(0.28),
                    k(0.5).w(10, 20).a(10, -15).tw(-0.2).st(0.32),
                    k(0.6).w(55, 45).a(25, 5).tw(-0.5).ln(-0.08).st(0.3),
                    k(0.75).w(30, 25).a(15, -15).tw(-0.25).st(0.25), rest(1));
                // A whole turn to its right: its back to them halfway, the butt round into them.
                case 3 -> filled(arm, rest(0), k(0.12).w(10, 0).tw(-0.15).sp(0.2),
                    k(0.3).w(0, 3).tw(0.2).st(0.15).spr(0.1).sp(-1.2),
                    k(0.5).w(0, 10).tw(0.1).ln(0.05).st(0.2).spr(0.12).sp(-HALF),
                    k(0.7).w(0, 3).st(0.2).spr(0.1).sp(-4.8),
                    k(0.85).st(0.15).sp(-TURN + 0.3), rest(1, -TURN));
                // Lunging: a deep coil, weight back, then out its whole length with the longest stride, held, recovered.
                default -> filled(arm, rest(0), k(0.12).w(0, -5).a(10, -45).tw(0.15).st(0.05),
                    k(0.3).w(0, -4).a(20, -60).tw(0.8).ln(-0.1).st(0.32).spr(0.1).bk(),
                    k(0.42).w(0, 0).a(5, -30).tw(0.1).ln(0.1).st(0.3),
                    k(0.5).w(0, 3).a(0, -3).tw(-0.6).ln(0.24).st(0.55).spr(0.1),
                    k(0.65).w(0, 3).a(0, -3).tw(-0.62).ln(0.24).st(0.55).spr(0.1),
                    k(0.8).w(0, 0).a(5, -20).tw(-0.25).ln(0.12).st(0.4), rest(1));
            };
            case FLESH_HAMMER -> switch (n) {
                // Wound far back to its right, its head behind it, then round in front at chest height with a stride.
                case 0 -> filled(arm, rest(0), k(0.12).w(15, 70).a(30, -10).tw(-0.1),
                    k(0.3).w(-110, 20).a(-50, -15).tw(0.65).ln(-0.05).sd(0.06).st(0.1).spr(0.1),
                    k(0.42).w(-60, 8).a(-25, -15).tw(0.35).st(0.25).spr(0.12),
                    k(0.5).w(0, 0).a(10, -15).tw(-0.1).ln(0.12).sd(-0.04).st(0.35).spr(0.12),
                    k(0.6).w(70, -5).a(45, -20).tw(-0.55).ln(0.15).sd(-0.06).st(0.35).spr(0.12),
                    k(0.75).w(45, 10).a(35, -20).tw(-0.35).ln(0.08).st(0.28), rest(1));
                // Back the other way.
                case 1 -> filled(arm, rest(0), k(0.12).w(-5, 65).a(25, -15).tw(0.05),
                    k(0.3).w(100, 20).a(55, -15).tw(-0.65).ln(-0.04).sd(-0.05).st(0.12).spr(0.1),
                    k(0.42).w(55, 8).a(35, -15).tw(-0.35).st(0.25),
                    k(0.5).w(0, 0).a(10, -15).tw(0.1).ln(0.1).sd(0.04).st(0.33).spr(0.12),
                    k(0.6).w(-75, -5).a(-40, -20).tw(0.55).ln(0.12).sd(0.06).st(0.33),
                    k(0.75).w(-45, 10).a(-20, -20).tw(0.35).ln(0.06).st(0.25), rest(1));
                // Up from the ground: sunk, its head low by its right foot, driven up through them, the body rising behind it.
                case 2 -> filled(arm, rest(0), k(0.12).w(10, 40).a(25, -20).tw(0.05).ln(0.05),
                    k(0.3).w(-35, -30).a(-20, -45).tw(0.4).ln(0.22).st(0.2).spr(0.18),
                    k(0.42).w(-15, 0).a(0, -35).tw(0.2).ln(0.15).st(0.28).spr(0.18),
                    k(0.5).w(0, 45).a(5, -10).tw(-0.05).ln(-0.05).st(0.35).spr(0.1),
                    k(0.6).w(0, 85).a(5, 20).tw(-0.1).ln(-0.15).st(0.32),
                    k(0.75).w(5, 70).a(15, -5).tw(-0.05).ln(-0.05).st(0.25), rest(1));
                // A dip, raised high over and behind its head, brought down onto the ground in front with a long stride.
                default -> filled(arm, rest(0), k(0.12).w(0, 40).a(25, -25).ln(0.05),
                    k(0.3).w(0, 155).a(5, 140).tw(0.05).ln(-0.18).st(0.25).bk(),
                    k(0.42).w(0, 90).a(5, 110).ln(-0.05).st(0.3),
                    k(0.5).w(0, -36).a(0, -30).ln(0.22).st(0.48).spr(0.18),
                    k(0.6).w(0, -38).a(0, -32).ln(0.22).st(0.48).spr(0.18),
                    k(0.75).w(0, 0).a(10, -30).ln(0.12).st(0.32).spr(0.1), rest(1));
            };
            case BONE_SWORD -> switch (n) {
                // Falling: raised over its right shoulder, cut down across to its left hip with a stride.
                case 0 -> filled(arm, rest(0), k(0.12).w(5, 50).a(30, -15).tw(-0.05),
                    k(0.3).w(-50, 75).a(-25, 55).tw(0.45).ln(-0.08).st(0.08),
                    k(0.42).w(-25, 30).a(-5, 25).tw(0.15).ln(0.05).st(0.22),
                    k(0.5).w(10, -15).a(15, -10).tw(-0.3).ln(0.18).sd(-0.04).st(0.32),
                    k(0.6).w(40, -45).a(30, -30).tw(-0.5).ln(0.22).st(0.32),
                    k(0.75).w(25, -10).a(30, -25).tw(-0.25).ln(0.1).st(0.25), rest(1));
                // Rising: from low on its left up across to high on its right.
                case 1 -> filled(arm, rest(0), k(0.12).w(10, 20).a(30, -30).tw(0.05),
                    k(0.3).w(45, -45).a(35, -40).tw(-0.45).ln(0.15).st(0.12).spr(0.1),
                    k(0.42).w(25, -15).a(20, -25).tw(-0.2).ln(0.1).st(0.25),
                    k(0.5).w(-5, 25).a(5, -5).tw(0.25).ln(0.02).st(0.32),
                    k(0.6).w(-40, 65).a(-15, 30).tw(0.45).ln(-0.1).st(0.3),
                    k(0.75).w(-20, 50).a(5, 0).tw(0.25).ln(-0.05).st(0.25), rest(1));
                // Level: wound far round to its right, swept flat through them and on round to its left.
                case 2 -> filled(arm, rest(0), k(0.12).w(10, 30).a(30, -20),
                    k(0.3).w(-110, 5).a(-45, -10).tw(0.6).ln(-0.05).sd(0.06).st(0.12).spr(0.12),
                    k(0.42).w(-55, 2).a(-20, -12).tw(0.3).st(0.25).spr(0.14),
                    k(0.5).w(0, 0).a(10, -15).tw(-0.1).ln(0.12).st(0.33).spr(0.14),
                    k(0.6).w(75, -5).a(45, -20).tw(-0.6).ln(0.14).sd(-0.06).st(0.33),
                    k(0.75).w(45, 10).a(35, -20).tw(-0.3).ln(0.06).st(0.25), rest(1));
                // A thrust: drawn back to its right hip, its point on them, driven straight out with a long stride.
                case 3 -> filled(arm, rest(0), k(0.12).w(0, 25).a(30, -25).tw(0.05),
                    k(0.3).w(0, 5).a(45, -45).tw(0.55).ln(-0.08).st(0.25).bk(),
                    k(0.42).w(0, 3).a(25, -25).tw(0.1).st(0.3),
                    k(0.5).w(0, 2).a(5, -5).tw(-0.35).ln(0.22).st(0.5),
                    k(0.6).w(0, 2).a(5, -5).tw(-0.37).ln(0.23).st(0.5),
                    k(0.75).w(0, 10).a(15, -20).tw(-0.15).ln(0.1).st(0.35), rest(1));
                // A whole turn to its left, the blade level out to its right.
                case 4 -> filled(arm, rest(0), k(0.12).w(-30, 15).a(-5, -20).tw(0.25).sp(-0.25),
                    k(0.3).w(-95, 5).a(-70, -10).tw(0.15).sp(0.6).st(0.15).spr(0.14),
                    k(0.5).w(-95, 0).a(-70, -10).tw(0.05).ln(0.05).sp(HALF).st(0.2).spr(0.14),
                    k(0.7).w(-95, 0).a(-70, -10).sp(5.0).st(0.2).spr(0.14),
                    k(0.85).w(-50, 15).a(-20, -15).sp(TURN - 0.2).st(0.15), rest(1, TURN));
                // Cleaving: a dip, raised behind its head, brought straight down the middle with a long stride.
                default -> filled(arm, rest(0), k(0.12).w(0, 50).a(25, -15).ln(0.04),
                    k(0.3).w(0, 160).a(5, 130).ln(-0.16).st(0.22).bk(),
                    k(0.42).w(0, 95).a(5, 100).ln(-0.05).st(0.3),
                    k(0.5).w(0, -22).a(5, -14).ln(0.22).st(0.48).spr(0.12),
                    k(0.6).w(0, -32).a(5, -24).ln(0.22).st(0.48).spr(0.12),
                    k(0.75).w(0, 0).a(15, -25).ln(0.15).st(0.32), rest(1));
            };
        };
    }

    /** {@code ability}'s keys, over its whole length (0..1). */
    static double[][] keys(Arm arm, Arm.Ability ability) {
        double t = ability.total;
        return switch (ability) {
            // Raised back and cast; hauled on with both hands, leaning back, braced wide; raised; whipped down for the slam.
            case BIND -> filled(arm, rest(0), k(3 / t).w(-30, 120).a(-20, 110).tw(0.3).ln(-0.12).st(0.05).l(30, -10),
                k(8 / t).w(0, 5).a(0, 5).tw(-0.2).ln(0.15).st(0.3).l(-30, -60),
                k(12 / t).w(0, -5).a(-10, -25).tw(0.1).ln(-0.15).st(0.28).bk().spr(0.12).l(0, -20),
                k(18 / t).w(-10, -10).a(-25, -35).tw(0.25).ln(-0.2).st(0.32).bk().spr(0.16).l(10, -30),
                k(22 / t).w(-20, 30).a(-15, 60).tw(0.2).ln(-0.15).st(0.25).bk().spr(0.12).l(20, -40),
                k(24 / t).w(0, -50).a(0, -40).tw(-0.1).ln(0.3).st(0.35).spr(0.12).l(-20, -60),
                k(28 / t).w(0, -30).a(5, -45).ln(0.18).st(0.3), rest(1));
            case FLAY -> flay(arm, ability);
            // Drawn back over its right shoulder (the left hand off the shaft, pointing at them), thrown with a stride, followed through.
            case JAVELIN -> filled(arm, rest(0), k(5 / t).w(0, 20).a(-160, 50).tw(0.5).ln(-0.1).st(0.1).l(5, 0),
                k(12 / t).w(0, 12).a(-175, 40).tw(0.7).ln(-0.15).st(0.3).bk().spr(0.1).l(0, 5),
                k(14 / t).w(0, 5).a(0, 25).tw(-0.5).ln(0.2).st(0.45).l(-30, -40),
                k(17 / t).w(10, -30).a(10, -30).tw(-0.7).ln(0.28).st(0.45).l(-20, -60),
                k(26 / t).w(0, 0).a(5, -30).tw(-0.2).ln(0.1).st(0.3), rest(1));
            // Crouched and coiled; then a sprint through them, the spear driven out ahead (its legs running).
            case PIERCE -> filled(arm, rest(0), k(4 / t).w(0, -2).a(15, -55).tw(0.6).ln(0.2).st(0.3).bk().spr(0.2),
                k(8 / t).w(0, -2).a(15, -55).tw(0.6).ln(0.22).st(0.3).bk().spr(0.2),
                k(9.5 / t).w(0, 4).a(0, -5).tw(-0.45).ln(0.3).st(0.5),
                k(11 / t).w(0, 4).a(0, -5).tw(-0.45).ln(0.3).st(-0.45),
                k(12.5 / t).w(0, 4).a(0, -5).tw(-0.45).ln(0.3).st(0.45),
                k(14 / t).w(0, 4).a(0, -5).tw(-0.45).ln(0.3).st(-0.45),
                k(15.5 / t).w(0, 4).a(0, -5).tw(-0.45).ln(0.28).st(0.4),
                k(20 / t).w(0, 3).a(5, -20).tw(-0.2).ln(0.12).st(0.3), rest(1));
            // Sunk wide with it raised; up, raised high behind the head; brought down on the ground in front.
            case QUAKE -> filled(arm, rest(0), k(3 / t).w(0, 120).a(10, 100).ln(0.12).st(0.1).spr(0.22),
                k(7 / t).w(0, 160).a(5, 140).ln(-0.18).st(0.0).spr(0.05),
                k(11 / t).w(0, 100).a(5, 110).ln(0.0).st(0.15).spr(0.1),
                k(14 / t).w(0, -36).a(0, -30).ln(0.22).st(0.42).spr(0.22),
                k(19 / t).w(0, -38).a(0, -32).ln(0.22).st(0.42).spr(0.22),
                k(25 / t).w(0, 10).a(15, -25).ln(0.15).st(0.25).spr(0.12), rest(1));
            // Coiled to its right, then two whole turns to its left held out level, braced wide.
            case WHIRL -> filled(arm, rest(0), k(5 / t).w(-80, 10).a(-50, -10).tw(0.35).sp(-0.3).st(0.12).spr(0.15),
                k(10 / t).w(-95, 0).a(-70, -8).tw(0.2).sp(2.0).st(0.15).spr(0.18),
                k(16 / t).w(-95, 0).a(-70, -8).tw(0.2).sp(5.6).st(0.15).spr(0.18),
                k(22 / t).w(-95, 0).a(-70, -8).tw(0.2).sp(9.4).st(0.15).spr(0.18),
                k(27 / t).w(-80, 5).a(-60, -8).tw(0.25).sp(2.0 * TURN).st(0.15).spr(0.16),
                k(31 / t).w(-30, 30).a(-10, -15).sp(2.0 * TURN).st(0.15), rest(1, 2.0 * TURN));
            // Sunk with it raised, the leap, raised high in the air, cut down where it lands with a stride.
            case LEAP -> filled(arm, rest(0), k(4 / t).w(0, 100).a(10, 90).ln(0.15).st(0.1).spr(0.2),
                k(6 / t).w(0, 140).a(5, 125).ln(-0.1).st(0.0).spr(0.05),
                k(12 / t).w(0, 165).a(5, 140).ln(-0.2).st(0.2).spr(0.05),
                k(15 / t).w(0, 80).a(5, 100).ln(0.0).st(0.3),
                k(17 / t).w(0, -26).a(5, -18).ln(0.22).st(0.48).spr(0.15),
                k(22 / t).w(0, -28).a(5, -20).ln(0.22).st(0.48).spr(0.15),
                k(26 / t).w(0, 10).a(15, -25).ln(0.12).st(0.3), rest(1));
            // Raised, drawn far round to its right (the blade behind it), the great sweep through everything in front, on round to its left.
            case CLEAVE -> filled(arm, rest(0), k(5 / t).w(-60, 60).a(-30, 20).tw(0.5).ln(-0.08).st(0.1),
                k(9 / t).w(-150, 15).a(-60, -5).tw(0.95).ln(-0.05).sd(0.08).st(0.25).bk().spr(0.15),
                k(12 / t).w(0, 0).a(10, -15).tw(-0.05).ln(0.15).st(0.42).spr(0.16),
                k(15 / t).w(110, -5).a(55, -20).tw(-0.95).ln(0.18).sd(-0.08).st(0.42).spr(0.16),
                k(20 / t).w(80, 10).a(45, -20).tw(-0.7).ln(0.1).st(0.35), rest(1));
        };
    }

    /** The Flay: a lash at each of its blows, across one way and back the other, a short stride into each. */
    private static double[][] flay(Arm arm, Arm.Ability ability) {
        double total = ability.total;
        java.util.List<K> out = new java.util.ArrayList<>();
        out.add(rest(0));
        for (int i = 0; i < ability.hits.length; i++) {
            double side = i % 2 == 1 ? -1.0 : 1.0;
            double h = ability.hits[i];
            out.add(k((h - 2.5) / total).w(-95 * side, 15).a(-60 * side, 5).tw(0.45 * side).ln(-0.03).st(0.18).l(40 * side, -35));
            out.add(k(h / total).w(55 * side, -5).a(40 * side, -15).tw(-0.35 * side).ln(0.12).st(0.28).l(-40 * side, -45));
        }
        out.add(k((ability.total - 3.0) / total).w(20, -10).a(15, -40).ln(0.05).st(0.2));
        out.add(rest(1));
        return filled(arm, out.toArray(new K[0]));
    }

    /** {@code t} through a blow (0..1) as its keys run: its landing ({@code land}, 0..1) at 0.5. */
    static double timed(double t, double land) {
        double c = Math.max(0.0, Math.min(1.0, t));
        double l = Math.max(0.05, Math.min(0.95, land));
        return c < l ? 0.5 * c / l : 0.5 + 0.5 * (c - l) / (1.0 - l);
    }

    /** Its stance: the quinque it takes after's, its feet set. */
    public static RigPose stance(Arm arm) {
        return posed(arm, filled(arm, rest(0), rest(1)), 0.0);
    }

    /** Its {@code step}th blow, {@code age} ticks in. */
    public static RigPose strike(Arm arm, int step, double age) {
        Arm.Strike strike = arm.strike(step);
        return posed(arm, keys(arm, step), timed(age / strike.total(), strike.land() / (double)strike.total()));
    }

    /** {@code ability}, {@code age} ticks in. */
    public static RigPose ability(Arm arm, Arm.Ability ability, double age) {
        return posed(arm, keys(arm, ability), age / ability.total);
    }

    /** True while the left hand is off its grip ({@code ability} {@code age} ticks in): the javelin thrown one-handed, until it has grown back. */
    public static boolean oneHanded(Arm.Ability ability, double age) {
        return ability == Arm.Ability.JAVELIN && age >= 2.0 && age < 26.0;
    }

    /** How much of it is in its hand ({@code ability} {@code age} ticks in): the javelin gone as it is thrown, another growing back. */
    public static float grown(Arm.Ability ability, double age) {
        if (ability != Arm.Ability.JAVELIN || age < ability.hits[0]) {
            return 1.0F;
        }
        return QuinquePose.ease((age - ability.hits[0] - 2.0) / 12.0);
    }

    /** The pose at {@code t} along {@code keys}. */
    static RigPose posed(Arm arm, double[][] keys, double t) {
        double[] v = curve(keys, t);
        Quinque q = base(arm);
        RigPose p = QuinquePose.hold(q);
        double lean = Math.max(-MOST_LEAN, Math.min(MOST_LEAN, v[LEAN]));
        double side = Math.max(-MOST_SIDE, Math.min(MOST_SIDE, v[SIDE]));
        p.twist += (float)v[TWIST];
        p.lean += (float)lean;
        p.side += (float)side;
        p.spin += (float)v[SPIN];
        // Its feet: a stride (the left foot forward for +, the right for -), the back foot where it stood; how wide apart.
        double stride = v[STEP];
        double spread = Math.max(0.0, v[SPREAD]);
        p.leftLegX = (float)-stride;
        p.rightLegX = (float)stride;
        p.rightLegZ = (float)spread;
        p.leftLegZ = (float)-spread;
        p.legHold = 1.0F;
        p.lunge += (float)(12.0 * Math.sin(Math.abs(stride)) * Math.max(-1.0, Math.min(1.0, v[SHIFT])));
        p.drop += (float)(12.0 * (1.0 - Math.cos(stride) * Math.cos(spread)));
        // The head on the foe: up against the lean, back against a part turn (a whole turn takes it round).
        p.headX -= (float)(0.65 * lean);
        double spin = v[SPIN];
        p.headY -= (float)(spin * Math.max(0.0, 1.0 - Math.abs(spin) / 2.2) * 0.85);
        // Its ways are relative to the foe: into the chest's frame (its twist, the stance's and the key's), then out of its lean.
        double turn = Math.toDegrees(QuinqueHolds.of(q).twist() + v[TWIST]);
        double[] armWay = way(v[A_AZ] + turn, v[A_EL]);
        double[] weapon = way(v[W_AZ] + turn, v[W_EL]);
        double[] free = way(v[L_AZ] + turn, v[L_EL]);
        double[][] leanTurn = p.leanTurn();
        if (leanTurn != null) {
            double[][] tw = RigPose.zyx(0.0, p.twist, 0.0);
            double[][] fix = RigPose.multiply(QuinqueGrip.transpose(tw), RigPose.multiply(QuinqueGrip.transpose(leanTurn), tw));
            armWay = QuinqueGrip.apply(fix, armWay);
            weapon = QuinqueGrip.apply(fix, weapon);
            free = QuinqueGrip.apply(fix, free);
        }
        point(p, arm, armWay, weapon);
        if (!arm.twoHanded || v[L_AZ] != FREE_ARM[0] || v[L_EL] != FREE_ARM[1]) {
            // A one-handed weapon's free arm swings against the blow (a two-handed one's is put on its grip after, but the javelin's).
            float[] a = QuinqueGrip.angles(QuinqueGrip.between(new double[] {0.0, 1.0, 0.0}, unit(free)));
            p.leftArmX = a[0];
            p.leftArmY = a[1];
            p.leftArmZ = a[2];
            p.leftArmHold = 1.0F;
        }
        return p;
    }

    /** The keys' values at {@code t} on a smooth curve through them (Catmull-Rom, still at the first and last). */
    static double[] curve(double[][] keys, double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        int i = 0;
        while (i < keys.length - 2 && c > keys[i + 1][0]) {
            i++;
        }
        double[] a = keys[i];
        double[] b = keys[i + 1];
        double span = Math.max(1.0E-6, b[0] - a[0]);
        double u = Math.max(0.0, Math.min(1.0, (c - a[0]) / span));
        double[] out = new double[a.length];
        out[0] = c;
        for (int j = 1; j < a.length; j++) {
            double ma = tangent(keys, i, j) * span;
            double mb = tangent(keys, i + 1, j) * span;
            double u2 = u * u;
            double u3 = u2 * u;
            out[j] = (2 * u3 - 3 * u2 + 1) * a[j] + (u3 - 2 * u2 + u) * ma + (-2 * u3 + 3 * u2) * b[j] + (u3 - u2) * mb;
        }
        return out;
    }

    private static double tangent(double[][] keys, int i, int j) {
        if (i == 0 || i == keys.length - 1) {
            return 0.0;
        }
        return (keys[i + 1][j] - keys[i - 1][j]) / Math.max(1.0E-6, keys[i + 1][0] - keys[i - 1][0]);
    }

    /** Where {@code arm} points as {@code p}'s right arm holds it, in its chest's frame (its own turn: no twist, no lean), unit. */
    public static double[] held(RigPose p, Arm arm) {
        return QuinqueGrip.apply(RigPose.zyx(p.rightArmX, p.rightArmY, p.rightArmZ), line(arm));
    }

    /** Where {@code p}'s right arm points (from its shoulder to its fist), in its chest's frame, unit. */
    public static double[] armWay(RigPose p) {
        return QuinqueGrip.apply(RigPose.zyx(p.rightArmX, p.rightArmY, p.rightArmZ), new double[] {0.0, 1.0, 0.0});
    }

    // ---------------------------------------------------------------- the body's frame (the whip's tendril)

    /**
     * A point on the right arm ({@code onArm}: its own frame, blocks) in the model's frame (blocks: x to its left, y down, z back; its feet
     * 1.501 down), the arm part where it is ({@code px..pz} pixels, its turn {@code xRot..zRot}, as ModelPart keeps them; a slim arm's
     * half pixel out, as PlayerModel.translateToHand has it).
     */
    public static double[] onBody(float px, float py, float pz, float xRot, float yRot, float zRot, boolean slim, double[] onArm) {
        double[] t = QuinqueGrip.apply(RigPose.zyx(xRot, yRot, zRot), onArm);
        return new double[] {(px + (slim ? 0.5 : 0.0)) / 16.0 + t[0], py / 16.0 + t[1], pz / 16.0 + t[2]};
    }

    /**
     * A point in the world ({@code rel}: blocks from the investigator's feet) in its model's frame, as its renderer draws it: turned by
     * its body's yaw ({@code yaw} degrees), drawn {@code wide} across and {@code size} tall, upside down, its feet 1.501 down. (Its flips
     * and spins aside.)
     */
    public static double[] toModel(double[] rel, double yaw, double wide, double size) {
        double x = rel[0] / wide;
        double y = rel[1] / size;
        double z = rel[2] / wide;
        double a = -Math.toRadians(180.0 - yaw);
        double c = Math.cos(a);
        double s = Math.sin(a);
        double rx = c * x + s * z;
        double rz = -s * x + c * z;
        return new double[] {-rx, -y + 1.501, rz};
    }

    /** {@code bodyPoint} ({forward, left, up} from {@code from}) in the model's frame. */
    public static double[] fromBody(double[] from, double[] bodyPoint) {
        return new double[] {from[0] + bodyPoint[1], from[1] - bodyPoint[2], from[2] - bodyPoint[0]};
    }

    /** {@code modelPoint} as {forward, left, up} from {@code from} (both in the model's frame). */
    public static double[] toBody(double[] from, double[] modelPoint) {
        return new double[] {from[2] - modelPoint[2], modelPoint[0] - from[0], from[1] - modelPoint[1]};
    }

    private static double[] unit(double[] v) {
        double n = QuinqueGrip.length(v);
        return n < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : new double[] {v[0] / n, v[1] / n, v[2] / n};
    }

    private static double[] flat(double[] v, double[] a) {
        double k = v[0] * a[0] + v[1] * a[1] + v[2] * a[2];
        return new double[] {v[0] - a[0] * k, v[1] - a[1] * k, v[2] - a[2] * k};
    }
}
