package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** 3.0.0: drawn kagune limbs never pass through their owner's body (Rinkaku's M1s drew tentacles through his torso). */
public final class BodyClearanceTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();
    private static final double WIDTH = 0.6;
    private static final double HEIGHT = 1.8;

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        // The body: inside the torso and head, outside beyond them.
        check(BodyClearance.distance(0.0, 1.0, 0.0, WIDTH, HEIGHT) < 0.0, "the chest is inside");
        check(BodyClearance.distance(0.0, 1.6, 0.0, WIDTH, HEIGHT) < 0.0, "the head is inside");
        check(BodyClearance.distance(0.0, 1.0, 0.5, WIDTH, HEIGHT) > 0.3, "half a block in front is outside");
        check(BodyClearance.distance(0.9, 1.0, 0.0, WIDTH, HEIGHT) > 0.3, "beside the arm is outside");
        check(Math.abs(BodyClearance.distance(0.0, 2.3, 0.0, WIDTH, HEIGHT) - (2.3 - HEIGHT - 0.03)) < 1.0E-9, "above the head by the gap");

        double radius = 0.16;
        for (double yawDegrees : new double[] {0.0, 37.0, 90.0, 180.0, 251.0}) {
            double yaw = Math.toRadians(yawDegrees);
            double fx = -Math.sin(yaw);
            double fz = Math.cos(yaw);
            double rx = fz;
            double rz = -fx;
            double[] feet = {10.0, 64.0, -4.0};
            for (double side : new double[] {-1.0, 1.0}) {
                // Root on the lower back (as TendrilChain's pelvis anchor), the strike goal ahead at chest height, the limb
                // drawn straight between them: through the torso.
                double[] root = {feet[0] - fx * 0.18 + rx * side * 0.1, feet[1] + HEIGHT * 0.43, feet[2] - fz * 0.18 + rz * side * 0.1};
                for (double[] goalLocal : new double[][] {{0.0, 1.2, 3.0}, {side * 0.4, 0.9, 1.6}, {-side * 1.5, 1.6, 2.5}, {0.0, 0.5, 4.0}}) {
                    double[] goal = {feet[0] + rx * goalLocal[0] + fx * goalLocal[2], feet[1] + goalLocal[1], feet[2] + rz * goalLocal[0] + fz * goalLocal[2]};
                    double[][] path = new double[42][];
                    for (int i = 0; i < path.length; i++) {
                        double t = i / (double)(path.length - 1);
                        path[i] = new double[] {root[0] + (goal[0] - root[0]) * t, root[1] + (goal[1] - root[1]) * t, root[2] + (goal[2] - root[2]) * t};
                    }
                    double before = longestStep(path);
                    String label = "yaw " + yawDegrees + " side " + side + " goal " + java.util.Arrays.toString(goalLocal);
                    boolean moved = BodyClearance.clear(path, feet, fx, fz, WIDTH, HEIGHT, radius, side);
                    check(moved, label + ": a limb drawn through him is moved");
                    check(path[0][0] == root[0] && path[0][1] == root[1] && path[0][2] == root[2], label + ": the root stays on his back");
                    check(dist(path[path.length - 1], goal) < 1.0E-9, label + ": the tip still lands on the goal");
                    double arc = 0.0;
                    boolean clear = true;
                    double worst = Double.MAX_VALUE;
                    for (int i = 1; i < path.length; i++) {
                        arc += dist(path[i], path[i - 1]);
                        double x = path[i][0] - feet[0];
                        double z = path[i][2] - feet[2];
                        double d = BodyClearance.distance(x * rx + z * rz, path[i][1] - feet[1], x * fx + z * fz, WIDTH, HEIGHT);
                        double need = BodyClearance.needed(arc, radius);
                        clear &= d >= need - 1.0E-6;
                        worst = Math.min(worst, d - need);
                    }
                    check(clear, label + ": the drawn limb clears his body (worst " + worst + ")");
                    check(longestStep(path) < Math.max(0.45, before * 4.0), label + ": the path stays smooth (" + longestStep(path) + ")");
                }
            }
            // A limb that never comes near him is left alone.
            double[][] away = new double[20][];
            for (int i = 0; i < away.length; i++) {
                double t = i / 19.0;
                away[i] = new double[] {feet[0] - fx * (0.3 + 3.0 * t) + rx * t, feet[1] + 0.8 + t, feet[2] - fz * (0.3 + 3.0 * t) + rz * t};
            }
            double[][] copy = new double[away.length][];
            for (int i = 0; i < away.length; i++) {
                copy[i] = away[i].clone();
            }
            check(!BodyClearance.clear(away, feet, fx, fz, WIDTH, HEIGHT, radius, 1.0), "yaw " + yawDegrees + ": a limb behind him is untouched");
            boolean same = true;
            for (int i = 0; i < away.length; i++) {
                same &= dist(away[i], copy[i]) == 0.0;
            }
            check(same, "yaw " + yawDegrees + ": untouched means not a hair moved");
        }
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAIL: " + failure));
            System.exit(1);
        }
        System.out.println("Body clearance tests: " + passed + " checks passed");
    }

    private static double longestStep(double[][] path) {
        double longest = 0.0;
        for (int i = 1; i < path.length; i++) {
            longest = Math.max(longest, dist(path[i], path[i - 1]));
        }
        return longest;
    }

    private static double dist(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }
}
