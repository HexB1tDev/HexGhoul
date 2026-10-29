package com.hex.kagune.control;

import com.hex.kagune.control.kakugan.CentipedeMask;
import com.hex.kagune.control.kakugan.KakuganEyes;
import java.util.ArrayList;
import java.util.List;

/** Rinkaku's kakuja bursting out and shattering (5.0.0): the timing curves, the leg's extrusion, the mask's growth, the pieces. */
public final class KakujaBurstTests {
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
        legs();
        tentacles();
        extrusion();
        mask();
        shards();
        head();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            throw new AssertionError(FAILURES.size() + " kakuja burst checks failed");
        }
        System.out.println("Kakuja burst tests: " + passed + " checks passed");
    }

    static void legs() {
        for (int order = 0; order < 2; order++) {
            double start = KakujaBurst.legStart(order);
            check(KakujaBurst.legOut(start - 0.5, order) == 0.0, "leg " + order + " still inside before it tears");
            check(KakujaBurst.legOut(start + KakujaBurst.LEG_TICKS, order) == 1.0, "leg " + order + " all out after LEG_TICKS");
            double previous = 0.0;
            for (double age = 0.0; age <= 30.0; age += 0.25) {
                double out = KakujaBurst.legOut(age, order);
                check(out >= previous - 1.0E-12 && out >= 0.0 && out <= 1.0, "leg out grows monotonically at " + age);
                previous = out;
                double stretch = KakujaBurst.legStretch(age, order);
                check(stretch >= 1.0 && stretch <= 1.1 + 1.0E-9, "stretch within a tenth at " + age);
                double swell = KakujaBurst.legSwell(age, order);
                check(swell >= 1.0 && swell <= 1.35 + 1.0E-9, "swell bounded at " + age);
                double jaw = KakujaBurst.legJaw(age, order);
                check(jaw >= 0.0 && jaw <= 1.0, "jaw bounded at " + age);
            }
            // Fast out of the wound: over half out in the first third.
            check(KakujaBurst.legOut(start + KakujaBurst.LEG_TICKS / 3.0, order) > 0.6, "leg erupts fast");
            check(KakujaBurst.legStretch(KakujaBurst.burstEnd(2), order) == 1.0, "stretch settled by the burst's end");
            check(Math.abs(KakujaBurst.legSwell(KakujaBurst.burstEnd(2), order) - 1.0) < 0.02, "swell settled by the burst's end");
            check(KakujaBurst.legJaw(KakujaBurst.burstEnd(2), order) < 0.05, "jaw closed by the burst's end");
        }
        check(KakujaBurst.legsBursting(0.0, 2) && !KakujaBurst.legsBursting(KakujaBurst.burstEnd(2), 2), "burst window");
        check(!KakujaBurst.legsBursting(-1.0, 2), "no burst before it starts");
        check(KakujaBurst.legStart(1) > KakujaBurst.legStart(0), "the legs tear out one after the other");
    }

    static void tentacles() {
        for (int order = 0; order < 4; order++) {
            check(KakujaBurst.tentacleOut(KakujaBurst.tentacleStart(order) - 0.1, order) == 0.0F, "tentacle " + order + " waits");
            check(KakujaBurst.tentacleOut(KakujaBurst.tentacleStart(order) + KakujaBurst.TENTACLE_TICKS, order) == 1.0F, "tentacle " + order + " out");
            float previous = 0.0F;
            for (double age = 0.0; age < 20.0; age += 0.25) {
                float out = KakujaBurst.tentacleOut(age, order);
                check(out >= previous && out <= 1.0F, "tentacle out monotonic at " + age);
                previous = out;
            }
            check(KakujaBurst.tentacleOut(KakujaBurst.tentacleStart(order) + KakujaBurst.TENTACLE_TICKS / 4.0, order) > 0.7F, "tentacle erupts");
            check(KakujaBurst.regrowOut(0.0, order) == 0.0F, "regrow waits for the shards");
            check(KakujaBurst.regrowOut(100.0, order) == 1.0F, "regrown");
        }
        check(!KakujaBurst.regrown(KakujaBurst.REGROW_DELAY, 4), "not regrown at once");
        for (int limbs = 1; limbs <= 6; limbs++) {
            double done = KakujaBurst.REGROW_DELAY + (limbs - 1) * KakujaBurst.REGROW_STAGGER + KakujaBurst.REGROW_TICKS;
            check(KakujaBurst.regrown(done, limbs), "regrown after the last of " + limbs);
            for (int order = 0; order < limbs; order++) {
                check(KakujaBurst.regrowOut(done, order) == 1.0F, "tentacle " + order + " of " + limbs + " fully out when regrown");
            }
        }
    }

    static void extrusion() {
        double[][] points = new double[12][];
        for (int i = 0; i < 12; i++) {
            double t = i / 11.0;
            points[i] = new double[] {0.3 + 2.0 * t, 1.0 + 3.0 * t - 1.5 * t * t, -0.2 - 1.2 * t};
        }
        double full = polyline(points);
        double[][] same = KakujaBurst.extrude(points, 1.0, 1.0);
        check(same.length == points.length, "extrude keeps the point count");
        check(near(same[0], points[0]) && near(same[11], points[11]), "all out, unstretched: the same leg");
        // Resampled points lie on the leg; the chords between them cut its bends by under a percent.
        check(Math.abs(polyline(same) - full) < full * 0.01, "all out keeps the length");
        for (double out = 0.05; out <= 1.0; out += 0.05) {
            double[][] part = KakujaBurst.extrude(points, out, 1.0);
            check(near(part[0], points[0]), "the root stays in his back at " + out);
            check(Math.abs(polyline(part) - full * out) < full * out * 0.01, "a part " + out + " of the length is out");
            double[][] stretched = KakujaBurst.extrude(points, out, 1.1);
            check(Math.abs(polyline(stretched) - full * out * 1.1) < full * out * 1.1 * 0.01, "the overshoot stretches it at " + out);
        }
        double[][] none = KakujaBurst.extrude(points, 0.0, 1.0);
        check(polyline(none) < 1.0E-9, "nothing out is a point at the root");
    }

    static void mask() {
        check(KakujaBurst.maskFront(0.0) == 0.0, "the mask starts from nothing");
        check(KakujaBurst.maskFront(KakujaBurst.MASK_TICKS) >= KakujaBurst.MASK_REACH + KakujaBurst.MASK_EDGE - 1.0E-9, "the mask's edge passes its farthest point");
        double previous = 0.0;
        for (double age = 0.0; age <= KakujaBurst.MASK_TICKS + 5.0; age += 1.0) {
            double front = KakujaBurst.maskFront(age);
            check(front >= previous, "the mask only grows at " + age);
            previous = front;
        }
        check(KakujaBurst.maskFront(KakujaBurst.MASK_TICKS * 0.2) < KakujaBurst.MASK_REACH * 0.25, "the mask takes hold slowly");
        check(KakujaBurst.maskGrown(5.0, 4.0) == 0.0 && KakujaBurst.maskGrown(1.0, 10.0) == 1.0, "grown is clamped");
        // Every quad of the mask, on any face, is reached by the end, and the lens is where it starts.
        KakuganEyes e = KakuganEyes.DEFAULT;
        KakuganEyes[] faces = {e, new KakuganEyes(e.rightX(), e.rightY(), e.leftX(), e.leftY(), e.height(), e.irisInside(), e.clearOuter(), KakuganEyes.LEFT),
            new KakuganEyes(e.rightX(), e.rightY(), e.leftX(), e.leftY(), e.height(), e.irisInside(), e.clearOuter(), KakuganEyes.RIGHT)};
        for (KakuganEyes eyes : faces) {
            double[] lens = CentipedeMask.lens(eyes);
            check(lens[0] > -4.0 && lens[0] < 4.0 && lens[1] > -8.0 && lens[1] < 0.0, "the lens is on the face");
            double[] farthest = {0.0};
            int[] count = {0};
            CentipedeMask.build((a, b, c, d, n, ca, cb, cc, cd) -> {
                double x = (a[0] + b[0] + c[0] + d[0]) * 0.25;
                double y = (a[1] + b[1] + c[1] + d[1]) * 0.25;
                farthest[0] = Math.max(farthest[0], Math.hypot(x - lens[0], y - lens[1]));
                count[0]++;
            }, eyes);
            check(count[0] > 50, "the mask has its quads");
            check(farthest[0] < KakujaBurst.MASK_REACH, "the whole mask is covered by the end (farthest " + farthest[0] + ")");
        }
    }

    static void shards() {
        List<KakujaBurst.Quad> quads = new ArrayList<>();
        // A leg-like tube of quads sweeping up and out to his right.
        for (int i = 0; i < 80; i++) {
            double t = i / 80.0;
            double[] at = {0.4 + 4.0 * t, 1.1 + 2.0 * t, -0.3 - t};
            double s = 0.12;
            quads.add(new KakujaBurst.Quad(new double[] {at[0] - s, at[1], at[2]}, new double[] {at[0] + s, at[1], at[2]},
                new double[] {at[0] + s, at[1] + s, at[2]}, new double[] {at[0] - s, at[1] + s, at[2]}, new double[] {0.0, 0.0, -1.0},
                0xFF563A5E, 0xFF563A5E, 0xFF563A5E, 0xFF563A5E, i % 7 == 0));
        }
        double[] body = {0.0, 1.0, 0.0};
        double floor = 0.0;
        List<KakujaBurst.Shard> shards = KakujaBurst.shatter(quads, 0.55, body, floor, 1.0, 42L);
        int total = 0;
        for (KakujaBurst.Shard shard : shards) {
            total += shard.quads();
        }
        check(total == quads.size(), "every quad goes into a piece");
        check(shards.size() >= 6 && shards.size() < quads.size(), "it breaks into several pieces, not one or dust: " + shards.size());
        List<KakujaBurst.Shard> again = KakujaBurst.shatter(quads, 0.55, body, floor, 1.0, 42L);
        check(again.size() == shards.size(), "the same seed breaks the same way");
        boolean same = true;
        for (int i = 0; i < shards.size(); i++) {
            same &= near(shards.get(i).velocity(), again.get(i).velocity());
        }
        check(same, "the same seed throws the same way");
        for (KakujaBurst.Shard shard : shards) {
            double[] start = shard.position(1.0);
            double before = Math.hypot(start[0] - body[0], start[2] - body[2]);
            check(shard.fade(0.0) == 1.0, "a fresh piece is whole");
            check(shard.velocity()[1] > 0.0, "every piece is thrown up a little");
            for (int tick = 0; tick < 6; tick++) {
                shard.step();
            }
            double[] at = shard.position(1.0);
            check(Math.hypot(at[0] - body[0], at[2] - body[2]) > before, "a piece flies away from his body");
            while (!shard.dead()) {
                shard.step();
                check(shard.position(1.0)[1] >= floor - 1.0E-9, "a piece never sinks through the ground");
            }
            check(shard.age == KakujaBurst.SHARD_LIFE && shard.fade(0.0) == 0.0, "a piece is gone at the end of its life");
            double[] rotation = shard.rotation(0.5);
            double[] n = shard.normal(0, rotation);
            check(Math.abs(Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]) - 1.0) < 1.0E-9, "a turned normal stays a unit");
        }
        check(KakujaBurst.shatter(new ArrayList<>(), 0.5, body, floor, 1.0, 1L).isEmpty(), "nothing drawn, nothing broken");
    }

    static void head() {
        double[] neck = KakujaBurst.head(new double[] {0.0, 0.0, 0.0}, 0.0, 0.0, 0.0, false);
        check(Math.abs(neck[1] - 1.501 * 0.9375) < 1.0E-9 && Math.abs(neck[0]) < 1.0E-9 && Math.abs(neck[2]) < 1.0E-9, "the neck over the feet");
        double[] top = KakujaBurst.head(new double[] {0.0, -8.0, 0.0}, 0.0, 0.0, 0.0, false);
        check(top[1] > neck[1] + 0.45, "the top of the head is above the neck");
        // Yaw 0 faces +z (south); the face is at z = -4 head pixels.
        double[] face = KakujaBurst.head(new double[] {0.0, -4.0, -4.0}, 0.0, 0.0, 0.0, false);
        check(face[2] > 0.2 && Math.abs(face[0]) < 1.0E-9, "the face looks south at yaw 0");
        double[] west = KakujaBurst.head(new double[] {0.0, -4.0, -4.0}, 90.0, 0.0, 0.0, false);
        check(west[0] < -0.2 && Math.abs(west[2]) < 1.0E-9, "the face looks west at yaw 90");
        double[] turned = KakujaBurst.head(new double[] {0.0, -4.0, -4.0}, 0.0, 90.0, 0.0, false);
        check(turned[0] < -0.2, "a head turned 90 relative to the body looks where yaw 90 does");
        // His left (+x head pixels) is east (+x) when he faces south.
        double[] left = KakujaBurst.head(new double[] {4.0, -4.0, 0.0}, 0.0, 0.0, 0.0, false);
        check(left[0] > 0.2, "his left is east when he faces south");
        double[] down = KakujaBurst.head(new double[] {0.0, -4.0, -4.0}, 0.0, 0.0, 60.0, false);
        check(down[1] < face[1], "looking down lowers the face");
        double[] crouched = KakujaBurst.head(new double[] {0.0, 0.0, 0.0}, 0.0, 0.0, 0.0, true);
        check(crouched[1] < neck[1], "crouching lowers the head");
    }

    static double polyline(double[][] points) {
        double total = 0.0;
        for (int i = 1; i < points.length; i++) {
            total += KakujaBurst.distance(points[i - 1], points[i]);
        }
        return total;
    }

    static boolean near(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < 1.0E-9 && Math.abs(a[1] - b[1]) < 1.0E-9 && Math.abs(a[2] - b[2]) < 1.0E-9;
    }
}
