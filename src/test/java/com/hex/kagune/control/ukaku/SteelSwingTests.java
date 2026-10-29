package com.hex.kagune.control.ukaku;

import com.hex.kagune.control.StrikePattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free tests for the Steel Blade's hold and swings (5.0.20): real cuts (the edge leads, the impact is dead ahead and
 * the fastest moment, they flow from wherever the blade is and settle back into the hold), the arm never through his own body,
 * a relaxed hold, and all of it smooth frame to frame.
 */
public final class SteelSwingTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();
    private static final StrikePattern[] SWINGS = {StrikePattern.STEEL_SWEEP, StrikePattern.STEEL_RETURN, StrikePattern.STEEL_EXECUTION};

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        turns();
        hold();
        swings();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Steel swing tests: " + passed + " checks passed");
    }

    /** The chest's twist turning a direction (as RigPose turns the blade arm: Ry about the model's vertical). */
    static double[] twisted(double twist, double[] v) {
        double c = Math.cos(twist);
        double s = Math.sin(twist);
        return new double[] {c * v[0] + s * v[2], v[1], -s * v[0] + c * v[2]};
    }

    private static void turns() {
        // ModelPart's angles and back give the same turn; aim points the blade and its edge where asked.
        boolean round = true;
        for (double a = -3.0; a <= 3.0; a += 0.37) {
            for (double b = -1.4; b <= 1.4; b += 0.31) {
                for (double c = -3.0; c <= 3.0; c += 0.41) {
                    double[] q = SteelSwing.fromEuler((float)a, (float)b, (float)c);
                    float[] e = SteelSwing.euler(q);
                    round &= SteelSwing.between(q, SteelSwing.fromEuler(e[0], e[1], e[2])) < 1.0E-4;
                }
            }
        }
        check(round, "ModelPart's three angles and the turn round-trip");
        double[] d = SteelSwing.unit(new double[] {-0.3, 0.2, -0.9});
        double[] q = SteelSwing.aim(d, SteelSwing.UP);
        double[] p = SteelSwing.pointing(q);
        double[] e = SteelSwing.edge(q);
        check(SteelSwing.dot(p, d) > 0.9999 && Math.abs(SteelSwing.dot(e, d)) < 1.0E-9 && SteelSwing.dot(e, SteelSwing.UP) > 0.9,
            "aim points the blade and turns its edge as asked");
    }

    private static void hold() {
        double[] still = BladeCarry.STILL;
        double last = Double.NaN;
        double most = 0.0;
        double[] previous = null;
        for (double t = 0.0; t < 30.0; t += 1.0 / 60.0) {
            double[] q = SteelSwing.hold(t, 0.0, 0.0, 0.0);
            double[] d = SteelSwing.pointing(q);
            if (previous != null) {
                most = Math.max(most, SteelSwing.between(previous, q));
            }
            previous = q;
            last = SteelSwing.dot(d, still);
        }
        check(most < 0.02, "the hold lives gently (" + most + " rad a frame at most)");
        double[] q = SteelSwing.hold(0.0, 0.0, 0.0, 0.0);
        double[] d = SteelSwing.pointing(q);
        double[] e = SteelSwing.edge(q);
        // Model space: x to his left, y down, z back.
        // 6.0.4: hanging down at his right side, out from the leg, steeply (about 58 degrees below level): neither standing out
        // level (the T-pose of 6.0.1) nor swept behind him (6.0.2).
        check(d[1] > 0.8 && d[1] < 0.9, "relaxed: hanging steeply down (" + d[1] + ")");
        check(d[0] < -0.45 && Math.abs(d[2]) < 0.2, "at his right side, out from the leg, neither in front nor behind (" + d[0] + ", " + d[2] + ")");
        check(e[2] < -0.8, "its edge turned forward");
        check(last < 0.71, "well away from the ground guard's still point (over 45 degrees)");
        // Walking it sways, running it swings back and up.
        double[] running = SteelSwing.pointing(SteelSwing.hold(0.0, 1.0, 0.0, 1.0));
        check(running[2] > d[2] + 0.1 && running[1] < d[1] && running[0] < 0.0, "running, it swings further back (still at his right) and up");
        double[] crouched = SteelSwing.pointing(SteelSwing.hold(0.0, 0.0, 0.0, 0.0, 1.0));
        check(crouched[1] < d[1] - 0.1 && crouched[0] < -0.6, "crouched, it hangs higher, still at his side");
        // It drives out over a swing's wind-up, stays out through the cut, and draws back in as it settles; smoothly.
        for (StrikePattern pattern : SWINGS) {
            check(Math.abs(SteelSwing.fit(pattern, 0.0F, 0.4F, 1.0) - 1.0) < 1.0E-9 && SteelSwing.fit(pattern, 0.4F, 0.4F, 1.0) == 0.0
                && Math.abs(SteelSwing.fit(pattern, 1.0F, 0.4F, 1.0) - 1.0) < 1.0E-9 && Math.abs(SteelSwing.fit(pattern, 0.0F, 0.4F, 0.3) - 0.3) < 1.0E-9,
                pattern + ": the blade drives out for the cut and draws back in after, from wherever it was");
            double jump = 0.0;
            for (int frame = 1; frame <= 60; frame++) {
                jump = Math.max(jump, Math.abs(SteelSwing.fit(pattern, frame / 60.0F, 0.4F, 1.0) - SteelSwing.fit(pattern, (frame - 1) / 60.0F, 0.4F, 1.0)));
            }
            check(jump < 0.15, pattern + ": its length changes smoothly (" + jump + " a frame at most)");
        }
        // Now and then he turns it over in his hand: a roll about the blade, its direction kept.
        double[] turning = SteelSwing.hold(SteelSwing.FLOURISH_TAKES * 0.5, 0.0, 0.0, 0.0);
        double[] calm = SteelSwing.hold(SteelSwing.FLOURISH_EVERY * 0.5, 0.0, 0.0, 0.0);
        check(SteelSwing.dot(SteelSwing.edge(turning), SteelSwing.edge(calm)) < 0.95, "he turns the blade over in his hand now and then");
    }

    private static void swings() {
        double[] hold = SteelSwing.hold(0.0, 0.0, 0.0, 0.0);
        double[] elsewhere = SteelSwing.aim(SteelSwing.unit(new double[] {-0.4, -0.2, -0.9}), SteelSwing.LEFT);
        for (StrikePattern pattern : SWINGS) {
            check(SteelSwing.swings(pattern), "swung here: " + pattern);
            for (float impact : new float[] {0.4F, 0.45F}) {
                for (double[] from : new double[][] {hold, elsewhere}) {
                    SteelSwing.Pose start = SteelSwing.swing(pattern, 0.0F, impact, from, hold);
                    SteelSwing.Pose end = SteelSwing.swing(pattern, 1.0F, impact, from, hold);
                    check(SteelSwing.between(start.arm, from) < 1.0E-6 && SteelSwing.between(end.arm, hold) < 1.0E-6,
                        pattern + ": starts where the blade was, ends in the hold");
                    check(Math.abs(start.twist) < 1.0E-6 && Math.abs(end.twist) < 1.0E-6 && Math.abs(end.torsoX) < 1.0E-6,
                        pattern + ": the body starts and ends at rest");
                }
                // The impact: dead ahead of him (with the chest's twist), the edge leading, the fastest stretch of the cut.
                SteelSwing.Pose at = SteelSwing.swing(pattern, impact, impact, hold, hold);
                SteelSwing.Pose next = SteelSwing.swing(pattern, impact + 0.01F, impact, hold, hold);
                double[] d = twisted(at.twist, SteelSwing.pointing(at.arm));
                double[] moving = SteelSwing.sub(twisted(next.twist, SteelSwing.pointing(next.arm)), d);
                double[] edge = twisted(at.twist, SteelSwing.edge(at.arm));
                check(-d[2] > 0.85, pattern + ": the impact is dead ahead (" + -d[2] + ")");
                check(SteelSwing.dot(SteelSwing.unit(moving), edge) > 0.8, pattern + ": the edge leads into the impact");
                if (pattern != StrikePattern.STEEL_EXECUTION) {
                    check(Math.abs(d[1]) < 0.3, pattern + ": a level cut, at his chest");
                } else {
                    check(d[1] > -0.1, pattern + ": the execution comes down");
                }
                double fastest = 0.0;
                double atImpact = 0.0;
                double windingUp = 0.0;
                double body = 0.0;
                double[] previous = hold;
                SteelSwing.Pose was = SteelSwing.swing(pattern, 0.0F, impact, hold, hold);
                boolean clear = true;
                for (int frame = 1; frame <= 30; frame++) {
                    float progress = frame / 30.0F;
                    SteelSwing.Pose pose = SteelSwing.swing(pattern, progress, impact, hold, hold);
                    double step = SteelSwing.between(previous, pose.arm);
                    fastest = Math.max(fastest, step);
                    if (Math.abs(progress - impact) <= 0.034F) {
                        atImpact = Math.max(atImpact, step);
                    }
                    if (progress < impact - 0.15F) {
                        windingUp = Math.max(windingUp, step);
                    }
                    body = Math.max(body, Math.max(Math.abs(pose.twist - was.twist), Math.abs(pose.torsoX - was.torsoX)));
                    // Never through his own body: across to his left only in front of his chest.
                    double[] chest = SteelSwing.pointing(pose.arm);
                    clear &= chest[0] < 0.2 || -chest[2] > 0.3;
                    previous = pose.arm;
                    was = pose;
                }
                check(atImpact >= 0.75 * fastest, pattern + ": the impact is in the cut's fastest stretch (" + atImpact + " of " + fastest + ")");
                check(fastest < 1.2, pattern + ": fast but never a jump (" + fastest + " rad a frame at 30 frames a swing)");
                // 6.0.2: from the low hold behind him too, the wind-up never outpaces the cut.
                check(windingUp < atImpact, pattern + ": the wind-up is slower than the cut (" + windingUp + " vs " + atImpact + ")");
                check(body < 0.4, pattern + ": the body moves smoothly (" + body + ")");
                check(clear, pattern + ": the arm never passes through his body");
            }
        }
        // One swing flows into the next: started from mid-recovery, nothing jumps.
        SteelSwing.Pose mid = SteelSwing.swing(StrikePattern.STEEL_SWEEP, 0.7F, 0.4F, hold, hold);
        SteelSwing.Pose next = SteelSwing.swing(StrikePattern.STEEL_RETURN, 0.0F, 0.4F, mid.arm, hold);
        SteelSwing.Pose after = SteelSwing.swing(StrikePattern.STEEL_RETURN, 0.03F, 0.4F, mid.arm, hold);
        check(SteelSwing.between(mid.arm, next.arm) < 1.0E-6 && SteelSwing.between(next.arm, after.arm) < 0.2, "one swing flows into the next");
    }
}
