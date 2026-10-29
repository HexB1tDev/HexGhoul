package com.hex.kagune.control.ayato;

import com.hex.kagune.control.kakugan.CentipedeMask;
import com.hex.kagune.control.kakugan.KakuganEyes;
import java.util.ArrayList;
import java.util.List;

/** Kaneki's centipede legs (3.1.0): poses keep their length, reach and wrap; the springs are smooth at any frame rate; the budget. */
public final class CentipedeTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        lengths();
        poses();
        springs();
        hunt();
        budget();
        mask();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            throw new AssertionError(FAILURES.size() + " centipede checks failed");
        }
        System.out.println("Centipede tests: " + passed + " checks passed");
    }

    static void lengths() {
        check(Math.abs(CentipedeMotion.length(750) - 6.5) < 1.0E-9, "6.5 blocks at 750");
        check(Math.abs(CentipedeMotion.length(1000) - 7.5) < 1.0E-9, "7.5 blocks at 1000");
        check(Math.abs(CentipedeMotion.length(0) - 6.5) < 1.0E-9 && Math.abs(CentipedeMotion.length(5000) - 7.5) < 1.0E-9, "length clamped");
        for (int mastery = 750; mastery <= 1000; mastery += 50) {
            double length = CentipedeMotion.length(mastery);
            int plates = CentipedeMotion.plates(length, CentipedeMotion.width(length));
            check(plates >= 7 && plates <= 10, "plates at " + mastery + ": " + plates);
        }
    }

    static CentipedeMotion.Input input(int side, boolean finalForm, CentipedeMotion.Act act, double[] target, double time) {
        return new CentipedeMotion.Input(side, finalForm, 7.0, 0.99, act, target, 0.3, false, 0.0, 0.0, 0.0, time);
    }

    static double polyline(double[][] points) {
        double total = 0.0;
        for (int i = 1; i < points.length; i++) {
            total += CentipedeMotion.distance(points[i], points[i - 1]);
        }
        return total;
    }

    static void poses() {
        for (boolean finalForm : new boolean[] {false, true}) {
            for (int side = -1; side <= 1; side += 2) {
                for (double time = 0.0; time < 400.0; time += 37.0) {
                    String label = (finalForm ? "final" : "first") + " side " + side + " t " + time;
                    CentipedeMotion.Pose rest = CentipedeMotion.pose(input(side, finalForm, CentipedeMotion.Act.REST, null, time));
                    double[][] p = rest.points();
                    check(p.length == CentipedeMotion.POINTS, label + ": point count");
                    check(CentipedeMotion.distance(p[0], CentipedeMotion.root(side, 0.99)) < 1.0E-9, label + ": rooted in his back");
                    double length = polyline(p);
                    check(length > 7.0 * 0.95 && length < 7.0 * 1.001, label + ": rest keeps the leg's length " + length);
                    for (int i = 3; i < p.length; i++) {
                        boolean inside = Math.hypot(p[i][0], p[i][2]) < 0.45 && p[i][1] > 0.0 && p[i][1] < 2.0;
                        check(!inside, label + ": point " + i + " clear of his body");
                    }
                    check(p[p.length - 1][0] * side > 0.0, label + ": stays on its own side");
                }
            }
            // The first kakuja's right leg is the scorpion over his head.
            double top = 0.0;
            for (double[] point : CentipedeMotion.pose(input(1, false, CentipedeMotion.Act.REST, null, 0.0)).points()) {
                top = Math.max(top, point[1]);
            }
            check(top > 3.5, "the scorpion arches over his head: " + top);
        }
        // The final kakuja's legs sweep out of his hips between the tentacle pairs (the upper ones end about 3.1 up, the lower
        // ones about 0.15): never up over the upper pair, never down among the lower, and out past both.
        for (double length : new double[] {CentipedeMotion.length(750), CentipedeMotion.length(1000)}) {
            for (int side = -1; side <= 1; side += 2) {
                for (double time = 0.0; time < 400.0; time += 37.0) {
                    double[][] p = CentipedeMotion.pose(new CentipedeMotion.Input(side, true, length, 0.99, CentipedeMotion.Act.REST, null, 0.3, false,
                        0.0, 0.0, 0.0, time)).points();
                    double high = 0.0;
                    double low = Double.MAX_VALUE;
                    double out = 0.0;
                    for (double[] point : p) {
                        high = Math.max(high, point[1]);
                        low = Math.min(low, point[1]);
                        out = Math.max(out, point[0] * side);
                    }
                    String label = "final " + length + " side " + side + " t " + time;
                    check(high < 2.7 && low > 0.6, label + ": between the tentacle pairs (" + low + " to " + high + ")");
                    check(out > 3.0, label + ": out past the tentacles' tips " + out);
                }
            }
        }
        // A strike reaches a target in reach, and stops short (never stretches) of one out of reach.
        double[] near = {1.3, 1.0, 3.3};
        CentipedeMotion.Pose strike = CentipedeMotion.pose(input(1, false, CentipedeMotion.Act.STRIKE, near, 0.0));
        double[][] s = strike.points();
        check(CentipedeMotion.distance(s[s.length - 1], near) < 0.3, "the strike's head lands on a near target: " + CentipedeMotion.distance(s[s.length - 1], near));
        check(Math.abs(polyline(s) - 7.0) < 0.35, "the strike keeps the leg's length " + polyline(s));
        double[] far = {0.0, 1.0, 30.0};
        double[][] f = CentipedeMotion.pose(input(1, false, CentipedeMotion.Act.STRIKE, far, 0.0)).points();
        check(polyline(f) < 7.0 * 1.001, "a far strike never stretches the leg: " + polyline(f));
        check(f[f.length - 1][2] > 2.5, "a far strike still reaches out toward it");
        // A coil wraps the held body: its last points circle the body's axis.
        double[] held = {2.0, 2.15, 1.75};
        CentipedeMotion.Input coil = new CentipedeMotion.Input(1, false, 7.0, 0.99, CentipedeMotion.Act.COIL, held, 0.55, false, 0.0, 0.0, 0.0, 0.0);
        double[][] c = CentipedeMotion.pose(coil).points();
        double minAngle = Double.MAX_VALUE;
        double maxAngle = -Double.MAX_VALUE;
        double previous = Double.NaN;
        double unwrapped = 0.0;
        for (int i = c.length - 4; i < c.length; i++) {
            double r = Math.hypot(c[i][0] - held[0], c[i][2] - held[2]);
            check(r > 0.3 && r < 0.95, "coil point " + i + " hugs the body: " + r);
            double a = Math.atan2(c[i][0] - held[0], c[i][2] - held[2]);
            if (!Double.isNaN(previous)) {
                double d = a - previous;
                d = Math.atan2(Math.sin(d), Math.cos(d));
                unwrapped += d;
            }
            previous = a;
            minAngle = Math.min(minAngle, unwrapped);
            maxAngle = Math.max(maxAngle, unwrapped);
        }
        check(maxAngle - minAngle > 1.5, "the coil goes round the body: " + (maxAngle - minAngle));
        // Every act gives a whole pose with its numbers in range.
        for (CentipedeMotion.Act act : CentipedeMotion.Act.values()) {
            CentipedeMotion.Pose pose = CentipedeMotion.pose(input(-1, true, act, near, 5.0));
            check(pose.points().length == CentipedeMotion.POINTS && pose.jaw() >= 0.0 && pose.jaw() <= 1.0 && pose.flare() > 0.0, act + ": a whole pose");
            for (double[] point : pose.points()) {
                check(Double.isFinite(point[0]) && Double.isFinite(point[1]) && Double.isFinite(point[2]), act + ": finite points");
            }
        }
    }

    static void springs() {
        CentipedeMotion.Pose rest = CentipedeMotion.pose(input(1, false, CentipedeMotion.Act.REST, null, 0.0));
        CentipedeMotion.Pose lock = CentipedeMotion.pose(input(1, false, CentipedeMotion.Act.LOCK, new double[] {1.3, 1.0, 3.3}, 0.0));
        // The same follow at 20 and at 160 frames a second ends in the same place (each step is solved exactly).
        CentipedeMotion.Leg coarse = new CentipedeMotion.Leg();
        CentipedeMotion.Leg fine = new CentipedeMotion.Leg();
        coarse.follow(rest, 0.0, 0.0);
        fine.follow(rest, 0.0, 0.0);
        for (int tick = 1; tick <= 6; tick++) {
            coarse.follow(lock, tick, 0.0);
        }
        for (int frame = 1; frame <= 48; frame++) {
            fine.follow(lock, frame * 0.125, 0.0);
        }
        double drift = 0.0;
        for (int i = 0; i < CentipedeMotion.POINTS; i++) {
            drift = Math.max(drift, CentipedeMotion.distance(coarse.points()[i], fine.points()[i]));
        }
        check(drift < 1.0E-6, "frame rate doesn't change the motion: " + drift);
        // Critically damped: from rest it closes on the new pose without ever passing it, and gets there.
        CentipedeMotion.Leg leg = new CentipedeMotion.Leg();
        leg.follow(rest, 0.0, 0.0);
        double[] head = lock.points()[CentipedeMotion.POINTS - 1];
        double last = CentipedeMotion.distance(leg.points()[CentipedeMotion.POINTS - 1], head);
        boolean monotone = true;
        for (double t = 0.5; t <= 40.0; t += 0.5) {
            leg.follow(lock, t, 0.0);
            double now = CentipedeMotion.distance(leg.points()[CentipedeMotion.POINTS - 1], head);
            monotone &= now <= last + 1.0E-9;
            last = now;
        }
        check(monotone, "no overshoot");
        check(last < 1.0E-3, "settles on the pose: " + last);
        check(CentipedeMotion.distance(leg.points()[0], lock.points()[0]) < 1.0E-12, "the root never leaves his back");
        // A long gap (paused game, chunk reload) snaps to the pose rather than flinging across the world.
        leg.follow(rest, 500.0, 0.0);
        check(CentipedeMotion.distance(leg.points()[CentipedeMotion.POINTS - 1], rest.points()[CentipedeMotion.POINTS - 1]) < 1.0E-9,
            "snaps after a long gap");
    }

    static void hunt() {
        double short_ = CentipedeMotion.reach(6.5);
        double long_ = CentipedeMotion.reach(7.5);
        check(short_ > 3.5 && short_ < 6.5, "a 6.5-block leg reaches 3.5..6.5 blocks: " + short_);
        check(long_ > short_, "a longer leg reaches farther: " + long_);
        for (double length : new double[] {6.5, 7.5}) {
            double reach = CentipedeMotion.reach(length);
            // Anything the hunt picks (within reach) is really struck: the head lands on it, in front or out to the side.
            for (double bearing = -1.0; bearing <= 1.0; bearing += 0.25) {
                for (double y : new double[] {0.6, 1.1, 1.8}) {
                    int side = bearing >= 0.0 ? 1 : -1;
                    double[] target = {Math.sin(bearing) * reach * 0.95, y, Math.cos(bearing) * reach * 0.95};
                    double[][] p = CentipedeMotion.pose(new CentipedeMotion.Input(side, false, length, 0.99, CentipedeMotion.Act.STRIKE, target, 0.3, false,
                        0.0, 0.0, 0.0, 0.0)).points();
                    check(CentipedeMotion.distance(p[p.length - 1], target) < 0.5, "length " + length + " bearing " + bearing + " height " + y
                        + ": the head lands on what the hunt picked (" + CentipedeMotion.distance(p[p.length - 1], target) + ")");
                }
            }
            for (int side = -1; side <= 1; side += 2) {
                double[] held = CentipedeMotion.holdPoint(side, length);
                String label = "length " + length + " side " + side;
                check(held[0] * side > 1.2 && held[2] > 0.5, label + ": held out to his side, ahead, where he sees it");
                double[][] c = CentipedeMotion.pose(new CentipedeMotion.Input(side, false, length, 0.99, CentipedeMotion.Act.COIL, held, 0.3 + 0.3 * 0.85,
                    true, 0.0, 0.0, 0.0, 0.0)).points();
                double turned = 0.0;
                for (int i = c.length - 4; i < c.length; i++) {
                    double r = Math.hypot(c[i][0] - held[0], c[i][2] - held[2]);
                    check(r > 0.3 && r < 1.0, label + ": coil point " + i + " hugs the body " + r);
                    if (i > c.length - 4) {
                        double a = Math.atan2(c[i][0] - held[0], c[i][2] - held[2]) - Math.atan2(c[i - 1][0] - held[0], c[i - 1][2] - held[2]);
                        turned += Math.abs(Math.atan2(Math.sin(a), Math.cos(a)));
                    }
                }
                check(turned > 1.5, label + ": the coil goes round (not cut short) " + turned);
                for (int i = 2; i < c.length; i++) {
                    boolean inside = Math.hypot(c[i][0], c[i][2]) < 0.45 && c[i][1] > 0.0 && c[i][1] < 2.0;
                    check(!inside, label + ": coil point " + i + " clear of his body");
                }
            }
        }
    }

    /** One eyed on the right: the same mask, mirrored, so the bare window is round the right eye (the ghoul one). */
    static void mask() {
        List<double[]> both = new ArrayList<>();
        List<double[]> right = new ArrayList<>();
        CentipedeMask.build((a, b, c, d, n, ca, cb, cc, cd) -> both.add(new double[] {a[0], a[1], a[2], n[0], n[1], n[2]}), KakuganEyes.DEFAULT);
        CentipedeMask.build((a, b, c, d, n, ca, cb, cc, cd) -> right.add(new double[] {d[0], d[1], d[2], n[0], n[1], n[2]}),
            KakuganEyes.DEFAULT.withGhoul(KakuganEyes.RIGHT));
        check(!both.isEmpty() && both.size() == right.size(), "the mirrored mask has every piece");
        boolean mirrored = both.size() == right.size();
        for (int i = 0; mirrored && i < both.size(); i++) {
            double[] p = both.get(i);
            double[] q = right.get(i);
            mirrored = Math.abs(p[0] + q[0]) < 1.0E-9 && Math.abs(p[1] - q[1]) < 1.0E-9 && Math.abs(p[2] - q[2]) < 1.0E-9
                && Math.abs(p[3] + q[3]) < 1.0E-9 && Math.abs(p[4] - q[4]) < 1.0E-9 && Math.abs(p[5] - q[5]) < 1.0E-9;
        }
        check(mirrored, "one eyed on the right wears the mask mirrored (Steve's eyes are symmetrical)");
        int[] left = {0};
        CentipedeMask.build((a, b, c, d, n, ca, cb, cc, cd) -> left[0]++, KakuganEyes.DEFAULT.withGhoul(KakuganEyes.LEFT));
        check(left[0] == both.size(), "one eyed on the left wears it as it is");
    }

    static void budget() {
        for (boolean fine : new boolean[] {true, false}) {
            int[] quads = {0};
            UkakuModel.Sink count = (a, b, c, d, n, ca, cb, cc, cd) -> quads[0]++;
            for (int side = -1; side <= 1; side += 2) {
                CentipedeMotion.Leg leg = new CentipedeMotion.Leg();
                double length = CentipedeMotion.length(1000);
                leg.follow(CentipedeMotion.pose(new CentipedeMotion.Input(side, true, length, 0.99, CentipedeMotion.Act.REST, null, 0.3, false, 0.0,
                    0.0, 0.0, 0.0)), 0.0, 0.0);
                KakujaModel.Spine spine = CentipedeTail.through(leg.points(), fine ? 64 : 40, side, leg.dorsal());
                double width = CentipedeMotion.width(length);
                CentipedeTail.tail(count, spine, new CentipedeTail.Look(width, CentipedeMotion.plates(length, width), fine, 1.0, side, 1.0, 1.3, 0.35,
                    0.0));
            }
            int legs = quads[0];
            CentipedeMask.build(count, KakuganEyes.DEFAULT);
            check(quads[0] <= (fine ? 3200 : 2400), (fine ? "fine" : "far") + " legs + mask within budget: " + legs + " + " + (quads[0] - legs));
        }
    }
}
