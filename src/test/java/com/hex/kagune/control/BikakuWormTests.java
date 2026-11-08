package com.hex.kagune.control;

import com.hex.kagune.control.bikaku.MawMotion;
import com.hex.kagune.control.bikaku.MawWorm;
import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * Work in progress (after 3.4.8): the Bikaku path's worm (MawWorm, MawMotion), not wired into the game yet. Pure, off-game checks of its
 * shape, its mouth, its colours and how it moves.
 */
public final class BikakuWormTests {
    private static int passed;

    private static void check(boolean good, String message) {
        if (!good) {
            throw new AssertionError(message);
        }
        passed++;
    }

    public static void main(String[] args) {
        shape();
        mouth();
        colours();
        motion();
        System.out.println("Bikaku worm tests: " + passed + " checks passed");
    }

    /** At rest it is long (the owner: "longer than all the others ... zigzags"), rooted in the lower back, every quad finite. */
    private static void shape() {
        for (double time : new double[] {0, 37, 120, 333}) {
            MawMotion.Shape s = MawMotion.idle(time, 7);
            KakujaModel.Spine spine = MawWorm.spine(s.path(), s.dorsal(), 60);
            check(spine.length() > 7.0, "the worm is about eight blocks long at rest (" + spine.length() + ")");
            double[] root = s.path()[0];
            check(Math.abs(root[0] - MawMotion.ROOT[0]) < 1.0E-9 && Math.abs(root[1] - MawMotion.ROOT[1]) < 1.0E-9
                && Math.abs(root[2] - MawMotion.ROOT[2]) < 1.0E-9, "it always leaves the body at the lower back");
            // It zigzags: its coils swing from side to side (x crosses back and forth).
            int crossings = 0;
            for (int i = 3; i < s.path().length - 3; i++) {
                if (Math.signum(s.path()[i][0]) != Math.signum(s.path()[i + 1][0])) {
                    crossings++;
                }
            }
            check(crossings >= 3, "its coils zigzag from side to side behind him");
            int[] quads = {0};
            boolean[] finite = {true};
            UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
                quads[0]++;
                for (double[] p : new double[][] {a, b, c, d, n}) {
                    finite[0] &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
                }
            };
            MawWorm.build(sink, new MawWorm.Pose(s.path(), s.dorsal(), 1.0, s.aperture(), 1.0, 7, time), 0xA50F37, true);
            check(finite[0] && quads[0] > 1500 && quads[0] < 12000, "the worm is built of finite quads, a sane number (" + quads[0] + ")");
        }
        int[] none = {0};
        MawMotion.Shape s = MawMotion.idle(0, 7);
        MawWorm.build((a, b, c, d, n, ca, cb, cc, cd) -> none[0]++, new MawWorm.Pose(s.path(), s.dorsal(), 0.0, 0.0, 1.0, 7, 0), 0xA50F37, true);
        check(none[0] == 0, "nothing is drawn of a worm not grown out at all");
    }

    /** The mouth: its hole grows as it opens, the head flares, a shut one is still a mouth (a small hole). */
    private static void mouth() {
        double last = -1.0;
        boolean grows = true;
        for (double open = 0.0; open <= 1.0001; open += 0.05) {
            double hole = MawWorm.holeRadius(open);
            grows &= hole > last;
            last = hole;
        }
        check(grows, "the mouth's hole grows steadily as it opens");
        check(MawWorm.holeRadius(0.0) > 0.15 * MawWorm.lipRadius(0.0), "shut, the lip still puckers round a hole (it reads as a mouth)");
        check(MawWorm.lipRadius(1.0) > MawWorm.lipRadius(0.0) * 1.3, "thrown wide, the head flares");
        boolean gapes = false;
        boolean mostlyShut = true;
        for (int t = 0; t < 600; t++) {
            double m = MawMotion.idleMouth(t, 7);
            gapes |= m > 0.7;
            mostlyShut &= m >= 0.0 && m <= 1.0;
        }
        check(gapes && mostlyShut, "at rest it now and then gapes, showing its teeth");
    }

    /** The flesh takes the owner's kagune colour; the teeth, spurs and throat keep theirs. */
    private static void colours() {
        MawMotion.Shape s = MawMotion.idle(14, 7);
        java.util.Set<Integer> crimson = colours(s, 0xA50F37);
        java.util.Set<Integer> blue = colours(s, 0x2350D8);
        java.util.Set<Integer> shared = new java.util.HashSet<>(crimson);
        shared.retainAll(blue);
        check(!shared.isEmpty() && shared.size() < crimson.size(), "recoloured, its flesh changes and its bone and throat stay as they are");
        boolean bluish = false;
        for (int c : blue) {
            int r = c >> 16 & 255;
            int b = c & 255;
            bluish |= b > r + 30;
        }
        check(bluish, "a blue kagune's worm is drawn blue");
    }

    private static java.util.Set<Integer> colours(MawMotion.Shape s, int tint) {
        java.util.Set<Integer> out = new java.util.HashSet<>();
        MawWorm.build((a, b, c, d, n, ca, cb, cc, cd) -> {
            out.add(ca & 0xFFFFFF);
            out.add(cc & 0xFFFFFF);
        }, new MawWorm.Pose(s.path(), s.dorsal(), 1.0, 0.8, 1.0, 7, 14), tint, false);
        return out;
    }

    /** A bite reaches its target as it lands, its mouth thrown wide just before and snapped shut on it, and comes back to rest. */
    private static void motion() {
        double[] target = {0.1, 1.2, 3.5};
        MawMotion.Shape before = MawMotion.bite(40, 7, target, 4.5, 12, 6);
        MawMotion.Shape landed = MawMotion.bite(41.5, 7, target, 6.0, 12, 6);
        double[] head = landed.path()[landed.path().length - 1];
        double miss = Math.sqrt((head[0] - target[0]) * (head[0] - target[0]) + (head[1] - target[1]) * (head[1] - target[1])
            + (head[2] - target[2]) * (head[2] - target[2]));
        check(miss < 0.2, "a bite's mouth is on its target as it lands (" + miss + " blocks off)");
        check(before.aperture() > 0.85 && landed.aperture() < 0.1, "the mouth gapes on the way in and is snapped shut as it lands");
        MawMotion.Shape end = MawMotion.bite(52, 7, target, 12.0, 12, 6);
        MawMotion.Shape rest = MawMotion.idle(52, 7);
        double[] a = end.path()[end.path().length - 1];
        double[] b = rest.path()[rest.path().length - 1];
        check(Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]) < 1.0E-6, "a bite ends back at rest");
    }
}
