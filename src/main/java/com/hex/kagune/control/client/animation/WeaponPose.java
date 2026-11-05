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

    /**
     * A key: when (0..1), where the weapon points (exactly) and the side the arm is on (chest frame, degrees: {@link #point}), and the
     * body's twist, lean (radians), lunge, drop (pixels), spin (radians).
     */
    private static double[] k(double t, double armAz, double armEl, double weaponAz, double weaponEl, double twist, double lean, double lunge, double drop,
                              double spin) {
        return new double[] {t, armAz, armEl, weaponAz, weaponEl, twist, lean, lunge, drop, spin};
    }

    /** Its stance at {@code t} ({@code spin}: the body's turn, a whole turn ending as it began). */
    private static double[] rest(Arm arm, double t, double spin) {
        double[] w = stanceWays(arm);
        return k(t, w[0], w[1], w[2], w[3], 0.0, 0.0, 0.0, 0.0, spin);
    }

    private static double[] rest(Arm arm, double t) {
        return rest(arm, t, 0.0);
    }

    private static final double TURN = Math.PI * 2.0;

    /**
     * Its {@code step}th blow's keys, the blow landing at 0.5. Written in the chest's frame against each stance: a direction in the world
     * is the chest's azimuth less the chest's turn (the stance's and the key's twist; + twists the chest to its right).
     */
    static double[][] keys(Arm arm, int step) {
        int n = Math.floorMod(step, arm.length());
        return switch (arm) {
            case WHIP -> switch (n) {
                // Forehand: the arm out to its right and up, the handle laid back, then whipped across the front and on to its left.
                case 0 -> new double[][] {rest(arm, 0), k(0.3, -75, 10, -110, 20, 0.45, -0.05, -0.5, 0, 0), k(0.5, 10, -10, 30, 0, -0.15, 0.1, 1.5, 0.3, 0),
                    k(0.68, 60, -25, 80, -10, -0.4, 0.12, 1.2, 0.3, 0), rest(arm, 1)};
                // Backhand: across to its left first, then out to its right.
                case 1 -> new double[][] {rest(arm, 0), k(0.3, 60, 0, 100, 10, -0.4, -0.05, -0.5, 0, 0), k(0.5, -10, -10, -30, 0, 0.15, 0.08, 1.5, 0.3, 0),
                    k(0.68, -70, -20, -90, -10, 0.4, 0.1, 1.2, 0.3, 0), rest(arm, 1)};
                // Overhead: raised high and back over its head, cracked down in front.
                case 2 -> new double[][] {rest(arm, 0), k(0.3, 5, 125, 5, 150, 0.1, -0.15, -0.5, 0, 0), k(0.5, 5, 0, 5, -20, -0.05, 0.3, 2.5, 0.6, 0),
                    k(0.68, 5, -35, 5, -50, -0.08, 0.35, 2.5, 0.8, 0), rest(arm, 1)};
                // Low: sunk, the arm low out to its right, swept along the ground across to its left.
                case 3 -> new double[][] {rest(arm, 0), k(0.3, -80, -45, -110, -30, 0.45, 0.2, 0, 2.0, 0), k(0.5, 0, -40, 20, -35, -0.1, 0.3, 1.5, 3.0, 0),
                    k(0.68, 60, -40, 90, -30, -0.4, 0.3, 1.5, 3.0, 0), rest(arm, 1)};
                // Double: out to its right, across to its left, and back again.
                case 4 -> new double[][] {rest(arm, 0), k(0.2, -70, 5, -100, 15, 0.4, 0, 0, 0, 0), k(0.5, 50, -10, 80, 0, -0.35, 0.1, 1.2, 0.3, 0),
                    k(0.75, -50, 0, -80, 5, 0.35, 0.05, 1.0, 0.2, 0), rest(arm, 1)};
                // A whole turn of the body to its left, the arm held out to its right, the tendril trailing round.
                default -> new double[][] {rest(arm, 0), k(0.15, -85, 0, -95, 10, 0.1, 0, 0, 0.5, 0.15), k(0.5, -90, 5, -100, 10, 0, 0.05, 1.0, 0.8, Math.PI),
                    k(0.85, -85, 0, -95, 10, 0.1, 0, 0.5, 0.5, TURN - 0.15), rest(arm, 1, TURN)};
            };
            case CRYSTAL_SPEAR -> switch (n) {
                // A thrust along its own line: the chest turned back to draw it, then squared up behind it with a long step.
                case 0 -> new double[][] {rest(arm, 0), k(0.3, 70, -45, 77, -3, 0.35, -0.05, -1.5, 0, 0), k(0.5, 35, -10, 34, 4, -0.4, 0.15, 5, 0.3, 0),
                    k(0.68, 35, -10, 34, 4, -0.42, 0.16, 5, 0.3, 0), rest(arm, 1)};
                // Two: one at the middle, drawn back, one at the face.
                case 1 -> new double[][] {rest(arm, 0), k(0.25, 70, -45, 77, -3, 0.35, -0.05, -1.5, 0, 0), k(0.5, 35, -10, 34, 4, -0.4, 0.15, 5, 0.3, 0),
                    k(0.64, 65, -35, 74, 0, 0.3, 0, -1, 0, 0), k(0.82, 30, 0, 34, 6, -0.4, 0.12, 4.5, 0, 0), rest(arm, 1)};
                // Rising: the crystal swept up from low on its right across to high on its left, as a staff.
                case 2 -> new double[][] {rest(arm, 0), k(0.3, 30, -50, 20, -35, 0.4, 0.1, 0, 1.5, 0), k(0.5, 60, -20, 41, 15, -0.2, 0, 2.5, 0.5, 0),
                    k(0.68, 80, 10, 68, 40, -0.5, -0.08, 2.0, 0, 0), rest(arm, 1)};
                // A whole turn to its right, the butt round into them halfway (its back to them).
                case 3 -> new double[][] {rest(arm, 0), k(0.2, 60, -30, 57, 2, 0, 0.05, 0, 0.5, -0.5), k(0.5, 55, -25, 57, 10, 0, 0.1, 2, 0.8, -Math.PI),
                    k(0.8, 60, -30, 57, 2, 0, 0.05, 1, 0.5, -TURN + 0.5), rest(arm, 1, -TURN)};
                // Lunging: sunk, drawn far back, driven out its whole length.
                default -> new double[][] {rest(arm, 0), k(0.3, 75, -50, 86, -3, 0.5, -0.1, -2.5, 1.0, 0), k(0.5, 35, -8, 26, 2, -0.55, 0.3, 8, 0.8, 0),
                    k(0.68, 35, -8, 26, 2, -0.55, 0.32, 8, 0.9, 0), rest(arm, 1)};
            };
            case FLESH_HAMMER -> switch (n) {
                // Laid out to its right and swung across in front, the whole chest behind it.
                case 0 -> new double[][] {rest(arm, 0), k(0.3, -20, -25, -57, 5, 0.5, -0.05, -0.5, 0.5, 0), k(0.5, 30, -15, 3, 0, -0.2, 0.15, 2.0, 0.8, 0),
                    k(0.68, 60, -20, 60, -5, -0.6, 0.2, 2.0, 0.8, 0), rest(arm, 1)};
                // Back the other way.
                case 1 -> new double[][] {rest(arm, 0), k(0.3, 70, -20, 70, 5, -0.6, 0, 0, 0.5, 0), k(0.5, 20, -15, 20, 0, 0.1, 0.12, 1.5, 0.8, 0),
                    k(0.68, -30, -25, -31, -5, 0.6, 0.15, 1.5, 0.8, 0), rest(arm, 1)};
                // Up from the ground: its head down by its feet, swung up through them.
                case 2 -> new double[][] {rest(arm, 0), k(0.3, -10, -70, -30, -30, 0.2, 0.25, 0, 2.5, 0), k(0.5, 15, -10, 14, 30, 0, 0, 2.5, 0.5, 0),
                    k(0.68, 15, 30, 14, 80, -0.05, -0.12, 2.0, 0, 0), rest(arm, 1)};
                // Raised high over its head and brought down into the ground before it.
                default -> new double[][] {rest(arm, 0), k(0.3, 14, 120, 14, 150, 0, -0.2, -0.5, 0, 0), k(0.5, 14, -15, 14, -32, 0, 0.6, 2.5, 1.5, 0),
                    k(0.68, 14, -25, 14, -36, 0, 0.65, 2.5, 1.8, 0), rest(arm, 1)};
            };
            case BONE_SWORD -> switch (n) {
                // Falling: from over its right shoulder down across to its left hip.
                case 0 -> new double[][] {rest(arm, 0), k(0.3, -15, 55, -28, 70, 0.35, -0.05, -0.5, 0, 0), k(0.5, 25, -15, -10, -10, -0.3, 0.2, 2.5, 0.8, 0),
                    k(0.68, 45, -35, 25, -45, -0.45, 0.25, 2.2, 1.0, 0), rest(arm, 1)};
                // Rising: from low on its left up across to high on its right.
                case 1 -> new double[][] {rest(arm, 0), k(0.3, 45, -45, 33, -45, -0.4, 0.15, 0, 0.6, 0), k(0.5, 30, -5, 31, 20, 0.25, 0.05, 2.5, 0.2, 0),
                    k(0.68, 0, 40, -10, 65, 0.4, -0.05, 2.0, 0, 0), rest(arm, 1)};
                // Level: the blade flat out to its right, swept across.
                case 2 -> new double[][] {rest(arm, 0), k(0.3, -30, -20, -57, 0, 0.55, -0.05, -0.5, 0.5, 0), k(0.5, 25, -15, 6, 0, -0.1, 0.12, 2.0, 0.8, 0),
                    k(0.68, 60, -20, 62, -5, -0.6, 0.15, 1.8, 0.8, 0), rest(arm, 1)};
                // A thrust: drawn back at the hip, driven straight out.
                case 3 -> new double[][] {rest(arm, 0), k(0.3, 55, -45, 28, 5, 0.3, -0.05, -1.5, 0.3, 0), k(0.5, 15, -5, -8, 0, -0.35, 0.15, 5.5, 0.5, 0),
                    k(0.68, 15, -5, -8, 0, -0.36, 0.16, 5.5, 0.5, 0), rest(arm, 1)};
                // A whole turn to its left, the blade level out to its right.
                case 4 -> new double[][] {rest(arm, 0), k(0.2, -60, -15, -90, 0, 0.1, 0.05, 0, 1.0, 0.3), k(0.5, -60, -15, -90, 0, 0, 0.1, 1.5, 1.2, Math.PI),
                    k(0.8, -60, -15, -90, 0, 0.1, 0.05, 1.0, 1.0, TURN - 0.4), rest(arm, 1, TURN)};
                // Cleaving: raised behind its head, brought straight down the middle with a long step.
                default -> new double[][] {rest(arm, 0), k(0.3, 15, 125, 11.5, 155, 0, -0.15, -1, 0, 0), k(0.5, 15, -10, 11.5, -15, 0, 0.3, 4, 0.6, 0),
                    k(0.68, 15, -25, 11.5, -35, 0, 0.38, 4, 0.8, 0), rest(arm, 1)};
            };
        };
    }

    /** {@code ability}'s keys, over its whole length (0..1). */
    static double[][] keys(Arm arm, Arm.Ability ability) {
        double total = ability.total;
        return switch (ability) {
            // Thrown: raised back, cast at them; held taut, dragging (leaning back, braced); whipped down for the slam.
            case BIND -> new double[][] {rest(arm, 0), k(4 / total, -40, 60, -60, 90, 0.3, -0.1, -0.5, 0, 0), k(8 / total, 5, 0, 5, -10, -0.2, 0.15, 2, 0.3, 0),
                k(13 / total, -20, -40, -30, -20, 0.2, -0.15, -1, 1.5, 0), k(21 / total, -25, -25, -40, -10, 0.25, -0.2, -1.5, 1.5, 0),
                k(24 / total, 10, -60, 10, -80, -0.1, 0.35, 1.5, 2.0, 0), k(28 / total, 10, -55, 10, -70, -0.05, 0.3, 1.0, 1.8, 0), rest(arm, 1)};
            case FLAY -> flay(arm, ability);
            // Drawn back over the shoulder (one-handed: the left hand points the way), thrown, the arm following through.
            case JAVELIN -> new double[][] {rest(arm, 0), k(6 / total, -100, 60, 70, 10, 0.2, -0.1, -1, 0.5, 0), k(12 / total, -123, 45, 57, 15, 0.3, -0.15, -1.5, 0.5, 0),
                k(14 / total, 57, 20, 57, 5, -0.8, 0.25, 3, 0.5, 0), k(17 / total, 6, -25, 6, -20, -0.9, 0.3, 3, 0.8, 0),
                k(26 / total, 40, -30, 50, -5, -0.3, 0.1, 1, 0.3, 0), rest(arm, 1)};
            // Crouched, drawn back; then driven through them, the whole body behind it.
            case PIERCE -> new double[][] {rest(arm, 0), k(4 / total, 70, -45, 77, -3, 0.35, 0.2, -1.5, 3.0, 0), k(8 / total, 70, -45, 77, -3, 0.35, 0.25, -1.5, 3.0, 0),
                k(9.5 / total, 35, -8, 34, 2, -0.4, 0.35, 6, 1.5, 0), k(15 / total, 35, -8, 34, 2, -0.4, 0.35, 6, 1.5, 0), rest(arm, 1)};
            // Crouched with it raised, up off the ground, brought down on it.
            case QUAKE -> new double[][] {rest(arm, 0), k(3 / total, 14, 110, 14, 140, 0, 0.1, 0, 3.0, 0), k(9 / total, 14, 125, 14, 155, 0, -0.15, 0, 0, 0),
                k(14 / total, 14, -20, 14, -28, 0, 0.6, 2.5, 4.0, 0), k(19 / total, 14, -20, 14, -30, 0, 0.55, 2.5, 3.5, 0), rest(arm, 1)};
            // Two whole turns to its left, held out level to its right.
            case WHIRL -> new double[][] {rest(arm, 0), k(5 / total, -70, -5, -95, 0, 0.3, 0.05, 0, 1.0, 0), k(10 / total, -70, -5, -95, 0, 0.3, 0.05, 0.5, 1.2, 1.4),
                k(22 / total, -70, -5, -95, 0, 0.3, 0.05, 0.5, 1.2, 2.0 * TURN - 1.4), k(27 / total, -70, -5, -95, 0, 0.3, 0.05, 0, 1.0, 2.0 * TURN),
                rest(arm, 1, 2.0 * TURN)};
            // Crouched with it raised, the leap, raised high in the air, brought down where it lands.
            case LEAP -> new double[][] {rest(arm, 0), k(5 / total, 15, 110, 11, 150, 0, 0.15, 0, 3.5, 0), k(12 / total, 15, 125, 11, 160, 0, -0.1, 0, 0, 0),
                k(17 / total, 15, -15, 11, -25, 0, 0.4, 2, 3.0, 0), k(22 / total, 15, -20, 11, -30, 0, 0.35, 2, 2.5, 0), rest(arm, 1)};
            // Drawn far round to its right, the great sweep through everything in front, on round to its left.
            case CLEAVE -> new double[][] {rest(arm, 0), k(8 / total, -40, -10, -73, 10, 0.8, -0.05, -0.5, 1.0, 0), k(12 / total, 25, -15, 11.5, 0, 0, 0.15, 3, 1.5, 0),
                k(16 / total, 60, -20, 70, -5, -0.9, 0.2, 3, 1.5, 0), k(22 / total, 55, -20, 60, -5, -0.7, 0.15, 2, 1.0, 0), rest(arm, 1)};
        };
    }

    /** The Flay: a lash at each of its blows, across one way and back the other. */
    private static double[][] flay(Arm arm, Arm.Ability ability) {
        double total = ability.total;
        java.util.List<double[]> out = new java.util.ArrayList<>();
        out.add(rest(arm, 0));
        for (int i = 0; i < ability.hits.length; i++) {
            boolean back = i % 2 == 1;
            double side = back ? -1.0 : 1.0;
            double h = ability.hits[i];
            out.add(k((h - 2.5) / total, -60 * side, 5, -90 * side, 10, 0.35 * side, 0, 0.5, 0.5, 0));
            out.add(k(h / total, 45 * side, -10, 70 * side, -5, -0.3 * side, 0.1, 1.0, 0.8, 0));
        }
        out.add(rest(arm, 1));
        return out.toArray(new double[0][]);
    }

    /** {@code t} through a blow (0..1) as its keys run: its landing ({@code land}, 0..1) at 0.5. */
    static double timed(double t, double land) {
        double c = Math.max(0.0, Math.min(1.0, t));
        double l = Math.max(0.05, Math.min(0.95, land));
        return c < l ? 0.5 * c / l : 0.5 + 0.5 * (c - l) / (1.0 - l);
    }

    /** Its stance. */
    public static RigPose stance(Arm arm) {
        return QuinquePose.hold(base(arm));
    }

    /** Its {@code step}th blow, {@code age} ticks in. */
    public static RigPose strike(Arm arm, int step, double age) {
        Arm.Strike strike = arm.strike(step);
        return posed(arm, keys(arm, step), timed(age / strike.total(), strike.land() / (double)strike.total()));
    }

    /** {@code ability}, {@code age} ticks in. */
    public static RigPose ability(Arm arm, Arm.Ability ability, double age) {
        RigPose p = posed(arm, keys(arm, ability), age / ability.total);
        if (oneHanded(ability, age)) {
            // The javelin's throw: the left hand off the shaft, pointing the way (the chest is turned to its right: forward is to its left).
            QuinqueGrip.aimLeft(p, -1.35, -0.9, 0.0);
            p.leftArmHold = 1.0F;
        }
        return p;
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
        p.twist += (float)v[5];
        p.lean += (float)v[6];
        p.lunge += (float)v[7];
        p.drop += (float)v[8];
        p.spin += (float)v[9];
        QuinquePose.step(p, q, (float)v[7]);
        double[] arm1 = way(v[1], v[2]);
        double[] weapon = way(v[3], v[4]);
        // The keys' ways are the chest's as it turns, not as it leans: a level thrust stays level however far the body leans into it
        // (the arm is drawn lean . Ry(twist) . its own turn).
        double[][] lean = p.leanTurn();
        if (lean != null) {
            double[][] tw = RigPose.zyx(0.0, p.twist, 0.0);
            double[][] fix = RigPose.multiply(QuinqueGrip.transpose(tw), RigPose.multiply(QuinqueGrip.transpose(lean), tw));
            arm1 = QuinqueGrip.apply(fix, arm1);
            weapon = QuinqueGrip.apply(fix, weapon);
        }
        point(p, arm, arm1, weapon);
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
