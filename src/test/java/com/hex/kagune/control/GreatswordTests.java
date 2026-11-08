package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.QuinqueGrip;
import com.hex.kagune.control.client.animation.QuinqueHolds;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.animation.WeaponPose;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import java.util.ArrayList;
import java.util.List;

/**
 * 3.4.8 (the owner, of the heavy sword's sheet: "the moves are ridiculously stupidly broken"): the Bone Greatsword held as a two-handed
 * sword is, every blow and move of it. Pure, off-game checks of WeaponPose's hold.
 */
public final class GreatswordTests {
    private static final Arm SWORD = Arm.BONE_SWORD;
    private static int passed;

    private static void check(boolean good, String message) {
        if (!good) {
            throw new AssertionError(message);
        }
        passed++;
    }

    public static void main(String[] args) {
        stance();
        everyFrame();
        System.out.println("Greatsword tests: " + passed + " checks passed");
    }

    /** Its stance is the one it had (nothing turned in the fist): standing and walking with it are as they were. */
    private static void stance() {
        RigPose st = WeaponPose.stance(SWORD);
        check(st.weaponX == 0.0F && st.weaponY == 0.0F && st.weaponZ == 0.0F, "3.4.8: the greatsword's stance turns nothing in the fist");
        for (Arm other : new Arm[] {Arm.WHIP, Arm.FLESH_HAMMER, Arm.CRYSTAL_SPEAR}) {
            boolean none = true;
            for (int step = 0; step < other.length(); step++) {
                for (double age = 0.0; age <= other.strike(step).total(); age += 0.5) {
                    RigPose p = WeaponPose.strike(other, step, age);
                    none &= p.weaponX == 0.0F && p.weaponY == 0.0F && p.weaponZ == 0.0F;
                }
            }
            check(none, "3.4.8: the " + other + " is held as it was (the owner: the hammer and the whip are good)");
        }
    }

    private static List<Object> moves() {
        List<Object> all = new ArrayList<>();
        for (int step = 0; step < SWORD.length(); step++) {
            all.add(step);
        }
        all.addAll(List.of(SWORD.abilities()));
        all.add(SWORD.signature());
        return all;
    }

    /**
     * Every frame of every blow and move: the left hand on the grip, the grip drawn where that hand takes it, the blade never through the
     * chest, never jumping between frames, and back in the stance at its end.
     */
    private static void everyFrame() {
        QuinqueHolds.Hold h = QuinqueHolds.of(WeaponPose.base(SWORD));
        float[] r = h.rightGrip();
        float[] l = h.leftGrip();
        double gap = h.scale() * Math.sqrt((r[0] - l[0]) * (r[0] - l[0]) + (r[1] - l[1]) * (r[1] - l[1]) + (r[2] - l[2]) * (r[2] - l[2])) / 16.0;
        float[] span = ArmMesh.of(SWORD, false).span();
        for (Object move : moves()) {
            double total = move instanceof Integer step ? SWORD.strike(step).total() : ((Arm.Ability)move).total;
            String name = move instanceof Integer step ? SWORD.strike(step).name() : ((Arm.Ability)move).title;
            double worstMiss = 0.0;
            double worstDrawn = 0.0;
            double worstTurn = 0.0;
            boolean chest = false;
            double[] last = null;
            for (double age = 0.0; age <= total + 1.0E-9; age += 0.25) {
                RigPose p = move instanceof Integer step ? WeaponPose.strike(SWORD, step, age) : WeaponPose.ability(SWORD, (Arm.Ability)move, age);
                for (boolean slim : new boolean[] {false, true}) {
                    RigPose q = new RigPose().add(p, 1.0F);
                    worstMiss = Math.max(worstMiss, Math.abs(QuinqueGrip.reach(q, WeaponPose.base(SWORD), slim)));
                }
                RigPose q = new RigPose().add(p, 1.0F);
                QuinqueGrip.reach(q, WeaponPose.base(SWORD), false);
                double[] grip = QuinqueGrip.leftGrip(q, WeaponPose.base(SWORD), false);
                worstDrawn = Math.max(worstDrawn, distance(grip, WeaponPose.alongLine(q, SWORD, -gap)));
                double[] from = WeaponPose.alongLine(q, SWORD, 0.0);
                double[] tip = WeaponPose.alongLine(q, SWORD, span[1]);
                for (double u = 0.3; u <= span[1]; u += 0.05) {
                    chest |= inChest(q, WeaponPose.alongLine(q, SWORD, u));
                }
                double[] way = unit(new double[] {tip[0] - from[0], tip[1] - from[1], tip[2] - from[2]});
                if (last != null) {
                    worstTurn = Math.max(worstTurn, Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, way[0] * last[0] + way[1] * last[1] + way[2] * last[2])))));
                }
                last = way;
            }
            check(worstMiss < 0.35, "3.4.8: " + name + ": both hands stay on the grip (the left misses it by " + worstMiss + " px at worst)");
            check(worstDrawn < 0.05, "3.4.8: " + name + ": the grip is drawn where the left hand takes it (" + worstDrawn + " px off)");
            check(!chest, "3.4.8: " + name + ": the blade never passes through the chest");
            check(worstTurn < 35.0, "3.4.8: " + name + ": the blade never jumps between frames (" + worstTurn + " degrees in a quarter tick at most)");
            RigPose end = move instanceof Integer step ? WeaponPose.strike(SWORD, step, total) : WeaponPose.ability(SWORD, (Arm.Ability)move, total);
            check(Math.abs(end.weaponX) < 1.0E-5 && Math.abs(end.weaponY) < 1.0E-5 && Math.abs(end.weaponZ) < 1.0E-5,
                "3.4.8: " + name + ": ends with nothing turned in the fist (its stance)");
        }
    }

    /** True if {@code q} (model pixels) is inside the chest as {@code p} poses it (half a pixel in from its faces). */
    private static boolean inChest(RigPose p, double[] q) {
        float[] at = p.transform("torso", RigPose.POSITION, 0.0F, 0.0F, 0.0F);
        float[] turn = p.transform("torso", RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        double[][] m = zyx(turn[0], turn[1], turn[2]);
        double[] d = {q[0] - at[0], q[1] - at[1], q[2] - at[2]};
        double x = m[0][0] * d[0] + m[1][0] * d[1] + m[2][0] * d[2];
        double y = m[0][1] * d[0] + m[1][1] * d[1] + m[2][1] * d[2];
        double z = m[0][2] * d[0] + m[1][2] * d[1] + m[2][2] * d[2];
        return Math.abs(x) < 3.5 && y > 0.5 && y < 11.5 && Math.abs(z) < 1.5;
    }

    private static double[][] zyx(double x, double y, double z) {
        double cx = Math.cos(x);
        double sx = Math.sin(x);
        double cy = Math.cos(y);
        double sy = Math.sin(y);
        double cz = Math.cos(z);
        double sz = Math.sin(z);
        double[][] rx = {{1, 0, 0}, {0, cx, -sx}, {0, sx, cx}};
        double[][] ry = {{cy, 0, sy}, {0, 1, 0}, {-sy, 0, cy}};
        double[][] rz = {{cz, -sz, 0}, {sz, cz, 0}, {0, 0, 1}};
        return multiply(rz, multiply(ry, rx));
    }

    private static double[][] multiply(double[][] a, double[][] b) {
        double[][] out = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                out[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return out;
    }

    private static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    private static double[] unit(double[] v) {
        double n = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / n, v[1] / n, v[2] / n};
    }
}
