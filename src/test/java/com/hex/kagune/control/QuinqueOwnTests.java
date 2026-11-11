package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.QuinqueOwn;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.Quinque;

/** 3.1.3: real weapon geometry at the attack's impact, projected into the first-person camera, plus the independent swing clock. */
public final class QuinqueOwnTests {
    private static int passed;
    private static void check(boolean yes, String message) {
        if (!yes) { throw new AssertionError(message); }
        passed++;
    }
    public static void main(String[] args) {
        combos();
        for (Arm arm : Arm.values()) {
            for (int step = 0; step < arm.length(); step++) {
                Arm.Strike strike = arm.strike(step);
                double[] impact = QuinqueOwn.arm(arm, step, null, strike.land());
                check(magnitude(impact) > 0.2, arm + ": real motion at every impact");
                for (boolean master : new boolean[] {false, true}) {
                    ArmMesh.Mesh mesh = ArmMesh.of(arm, master);
                    for (double fov : new double[] {55, 70, 100}) {
                        int seen = 0;
                        for (int i = 0; i < mesh.pos.length; i += 3) {
                            double[] p = QuinqueOwn.point(arm, impact, new double[] {mesh.pos[i], mesh.pos[i + 1], mesh.pos[i + 2]}, 1, 0);
                            if (visible(p, fov)) { seen++; }
                        }
                        check(seen >= 12, arm + "/" + step + ": weapon visible at impact, FOV " + fov + ", points " + seen);
                    }
                }
                check(magnitude(QuinqueOwn.arm(arm, step, null, 0)) == 0, "starts from ready without a jump");
                check(magnitude(QuinqueOwn.arm(arm, step, null, strike.total())) == 0, "ends in ready");
                check(magnitude(QuinqueOwn.arm(arm, step, null, strike.total() - 0.0001)) < 0.001, "settles continuously");
                for (double age = 0; age <= strike.total(); age += 0.5) {
                    for (double value : QuinqueOwn.arm(arm, step, null, age)) { check(Double.isFinite(value), "finite attack"); }
                }
                if (arm == Arm.WHIP) {
                    whip(step, null, strike.land(), impact);
                }
            }
            for (Arm.Ability ability : arm.abilities()) {
                for (double age = 0; age <= ability.total; age += 0.5) {
                    double[] motion = QuinqueOwn.arm(arm, -1, ability, age);
                    for (double value : motion) { check(Double.isFinite(value), "finite ability"); }
                    if (arm == Arm.WHIP) { whip(-1, ability, age, motion); }
                }
                check(magnitude(QuinqueOwn.arm(arm, -1, ability, ability.total - 0.0001)) < 0.001, "repeated ability settles continuously");
            }
        }
        for (Quinque q : Quinque.values()) {
            for (int step = 0; step < q.steps(); step++) { // 4.1.17: each its own string, each blow its own length
                check(magnitude(QuinqueOwn.legacy(q, step, q.swing(step) * 0.5)) > 0.2, q + ": each legacy M1 moves the hand");
                check(magnitude(QuinqueOwn.legacy(q, step, q.swing(step))) == 0, q + ": no old swing after it finishes");
            }
        }
        System.out.println("Quinque own-view tests: " + passed + " checks passed");
    }
    private static void combos() {
        QuinqueOwn.Combo c = new QuinqueOwn.Combo();
        c.observe(true, 0, 10);
        check(c.step() == 0 && c.age(10) == 0, "first-person observation starts the first hit without a body-layer tick");
        c.observe(true, 0, 10);
        check(c.step() == 0 && c.began() == 10, "a second rendering pass cannot restart the swing");
        c.observe(true, 4, 14);
        check(c.age(14) == 4, "swing clock keeps running");
        c.observe(true, 0, 15);
        check(c.step() == 1 && c.began() == 15, "vanilla swing wrap advances the string");
        c.observe(false, 0, 20); c.observe(true, 0, 21);
        check(c.step() == 2, "third click reaches the finisher");
        c.observe(false, 0, 70); c.observe(true, 0, 71);
        check(c.step() == 0, "after the combo timeout starts fresh");
        c.observe(true, 0, 0);
        check(c.step() == 0 && c.age(0) == 0, "clock rollback starts cleanly");
    }
    private static void whip(int step, Arm.Ability ability, double age, double[] motion) {
        double[][] path = QuinqueOwn.whip(motion, step, ability, age, false, 1, 0, 40);
        double[] root = QuinqueOwn.point(Arm.WHIP, motion, new double[] {0, 0, -ArmMesh.WHIP_ROOT}, 1, 0);
        check(java.util.Arrays.equals(path[0], root), "whip stays attached to its handle");
        int seen = 0;
        for (double[] p : path) {
            for (double v : p) { check(Double.isFinite(v), "finite whip geometry"); }
            if (visible(p, 70)) { seen++; }
        }
        check(seen >= 2, "whip's tendril is visible, not just its handle: " + step + "/" + ability + " age " + age);
    }
    private static boolean visible(double[] p, double fov) {
        double depth = -p[2];
        double h = depth * Math.tan(Math.toRadians(fov * 0.5));
        return depth > 0.05 && Math.abs(p[1]) < h && Math.abs(p[0]) < h * (16.0 / 9.0);
    }
    private static double magnitude(double[] a) { double n = 0; for (double v : a) { n += Math.abs(v); } return n; }
}
