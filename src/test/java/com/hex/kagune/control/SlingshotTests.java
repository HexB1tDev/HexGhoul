package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** Rinkaku's grapple flight (3.0.0): up, over and down onto the top of the grabbed block, ending exactly on it. */
public final class SlingshotTests {
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
        double[][][] cases = {
            {{0.0, 64.0, 0.0}, {0.5, 65.0, 12.5}},   // a block a step up, 12 ahead
            {{0.0, 64.0, 0.0}, {-8.5, 80.0, 3.5}},   // the top of a tree
            {{0.0, 90.0, 0.0}, {6.5, 70.0, -18.5}},  // down onto a ledge far below
            {{0.0, 64.0, 0.0}, {0.5, 64.0, 2.5}},    // right next to him
            {{0.0, 64.0, 0.0}, {0.5, 88.0, 3.5}}};   // up a wall just ahead
        for (double[][] c : cases) {
            double[] from = c[0];
            double[] to = c[1];
            String label = java.util.Arrays.toString(from) + " -> " + java.util.Arrays.toString(to);
            SlingshotPath path = SlingshotPath.plan(from, to);
            check(path.ticks() >= SlingshotPath.MIN_TICKS && path.ticks() <= SlingshotPath.MAX_TICKS, label + ": flight time in bounds " + path.ticks());
            double[] start = path.at(0);
            double[] end = path.at(path.ticks());
            check(dist(start, from) < 1.0E-9, label + ": starts at his feet");
            check(dist(end, to) < 1.0E-9, label + ": lands exactly on the top of the block");
            // Summing the per-tick velocities lands him there too (the server drives him with them).
            double[] p = from.clone();
            double fastest = 0.0;
            for (int tick = 0; tick < path.ticks(); tick++) {
                double[] v = path.velocity(tick);
                p[0] += v[0];
                p[1] += v[1];
                p[2] += v[2];
                fastest = Math.max(fastest, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]));
            }
            check(dist(p, to) < 1.0E-9, label + ": the velocities add up to the landing");
            check(fastest < 4.0, label + ": never faster than 4 blocks a tick (" + fastest + ")");
            double[] last = path.velocity(path.ticks() - 1);
            check(Math.sqrt(last[0] * last[0] + last[1] * last[1] + last[2] * last[2]) < 0.6, label + ": a soft landing");
            check(last[1] <= 1.0E-9, label + ": comes down onto the top, never up into it from below");
            // Nearing the block he is already above its top (he never clips its edge or the wall it is in).
            boolean above = true;
            for (int tick = 0; tick <= path.ticks(); tick++) {
                above &= path.across(tick) < 0.7 || path.at(tick)[1] >= to[1] - 1.0E-9;
            }
            check(above, label + ": comes over the block from above");
            // He clears the higher end before crossing over.
            double apex = Double.NEGATIVE_INFINITY;
            for (int tick = 0; tick <= path.ticks(); tick++) {
                apex = Math.max(apex, path.at(tick)[1]);
            }
            check(apex >= Math.max(from[1], to[1]) + 1.0, label + ": rises clear over the edge (" + apex + ")");
        }
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAIL: " + failure));
            System.exit(1);
        }
        System.out.println("Slingshot tests: " + passed + " checks passed");
    }

    private static double dist(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }
}
