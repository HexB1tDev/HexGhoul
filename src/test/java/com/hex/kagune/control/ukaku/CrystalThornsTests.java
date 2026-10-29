package com.hex.kagune.control.ukaku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Dependency-free tests for the crystal thorns (5.0.15): when they come out and the grab lets go, and how they sit on him. */
public final class CrystalThornsTests {
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
        timing();
        layout();
        growth();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Crystal thorns tests: " + passed + " checks passed");
    }

    /** Runs {@code ticks} ticks from {@code from} with the same inputs; returns the step of each. */
    private static CrystalThorns.Step[] run(CrystalThorns.Eruption eruption, long from, int ticks, boolean crystal, boolean held) {
        CrystalThorns.Step[] steps = new CrystalThorns.Step[ticks];
        for (int i = 0; i < ticks; i++) {
            steps[i] = eruption.tick(crystal, held, from + i);
        }
        return steps;
    }

    private static boolean only(CrystalThorns.Step[] steps, CrystalThorns.Step step) {
        return Arrays.stream(steps).allMatch(s -> s == step);
    }

    private static void timing() {
        check(CrystalThorns.RELEASE_TICKS == 20, "the kagune lets go one second after the thorns come out");
        // Held in the crystal mode: out at once, let go a second later, then out a moment longer before they sink back.
        CrystalThorns.Eruption e = new CrystalThorns.Eruption();
        check(e.tick(true, true, 100L) == CrystalThorns.Step.ERUPT && e.out(), "held in the crystal mode, the thorns erupt at once");
        check(only(run(e, 101L, CrystalThorns.RELEASE_TICKS - 1, true, true), CrystalThorns.Step.NONE), "and hold for the second");
        check(e.tick(true, true, 100L + CrystalThorns.RELEASE_TICKS) == CrystalThorns.Step.RELEASE, "then the kagune lets go");
        long free = 101L + CrystalThorns.RELEASE_TICKS;
        check(only(run(e, free, CrystalThorns.LINGER_TICKS - 1, true, false), CrystalThorns.Step.NONE) && e.out(), "free, the thorns stay out a moment");
        check(e.tick(true, false, free + CrystalThorns.LINGER_TICKS - 1) == CrystalThorns.Step.SINK && !e.out(), "then sink back in");

        // Turning the crystal mode on while held: the same, from the tick it comes on.
        e = new CrystalThorns.Eruption();
        check(only(run(e, 0L, 40, false, true), CrystalThorns.Step.NONE) && !e.out(), "held out of the crystal mode, nothing");
        check(e.tick(true, true, 40L) == CrystalThorns.Step.ERUPT, "V while held: the thorns erupt");
        check(only(run(e, 41L, CrystalThorns.RELEASE_TICKS - 1, true, true), CrystalThorns.Step.NONE)
            && e.tick(true, true, 40L + CrystalThorns.RELEASE_TICKS) == CrystalThorns.Step.RELEASE, "and the grab lets go a second later");

        // Grabbed again while they are still out: the grab closes on the thorns and lets go much sooner.
        long regrab = 41L + CrystalThorns.RELEASE_TICKS + 5L;
        run(e, 41L + CrystalThorns.RELEASE_TICKS, 5, true, false);
        check(only(run(e, regrab, CrystalThorns.REGRAB_TICKS, true, true), CrystalThorns.Step.NONE)
            && e.tick(true, true, regrab + CrystalThorns.REGRAB_TICKS) == CrystalThorns.Step.RELEASE, "a grab on the thorns lets go sooner");
        check(CrystalThorns.REGRAB_TICKS < CrystalThorns.RELEASE_TICKS / 2, "much sooner");
        // After they have sunk back, a new grab has to wait the full second again.
        long later = regrab + CrystalThorns.REGRAB_TICKS + 1L;
        run(e, later, CrystalThorns.LINGER_TICKS, true, false);
        check(!e.out() && e.tick(true, true, later + 100L) == CrystalThorns.Step.ERUPT, "sunk back, a new grab starts them again");

        // Still held when it should have let go (something else holds him): it tries again soon, never stalls.
        e = new CrystalThorns.Eruption();
        e.tick(true, true, 0L);
        run(e, 1L, CrystalThorns.RELEASE_TICKS, true, true);
        check(only(run(e, CrystalThorns.RELEASE_TICKS + 1L, CrystalThorns.REGRAB_TICKS, true, true), CrystalThorns.Step.NONE)
            && e.tick(true, true, CrystalThorns.RELEASE_TICKS + 1L + CrystalThorns.REGRAB_TICKS) == CrystalThorns.Step.RELEASE, "it tries again");

        // Let go before the second is up (the Rinkaku let go, or threw him): no release, they linger and sink.
        e = new CrystalThorns.Eruption();
        e.tick(true, true, 0L);
        run(e, 1L, 5, true, true);
        CrystalThorns.Step[] after = run(e, 6L, CrystalThorns.LINGER_TICKS + 1, true, false);
        check(Arrays.stream(after).noneMatch(s -> s == CrystalThorns.Step.RELEASE) && after[CrystalThorns.LINGER_TICKS] == CrystalThorns.Step.SINK,
            "let go early, they linger and sink without a release");

        // The crystal thawing (V again, the kagune in, death) shatters them at once, held or not.
        for (boolean held : new boolean[] {true, false}) {
            e = new CrystalThorns.Eruption();
            e.tick(true, true, 0L);
            run(e, 1L, 3, true, held);
            check(e.tick(false, held, 4L) == CrystalThorns.Step.SHATTER && !e.out() && e.tick(false, held, 5L) == CrystalThorns.Step.NONE,
                "thawed (held " + held + "): they shatter, once");
        }
        check(only(run(new CrystalThorns.Eruption(), 0L, 60, true, false), CrystalThorns.Step.NONE), "never held, never out");
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static void layout() {
        check(CrystalThorns.COUNT >= 36, "many thorns (" + CrystalThorns.COUNT + ")");
        check(CrystalThorns.MIN_SIZE * CrystalSpike.LENGTH >= 1.5 && CrystalThorns.MAX_SIZE * CrystalSpike.LENGTH <= 2.8,
            "long: 1.5 to 2.8 blocks out of him");
        CrystalThorns.Thorn[] a = CrystalThorns.thorns(1234, CrystalThorns.COUNT);
        CrystalThorns.Thorn[] b = CrystalThorns.thorns(1234, CrystalThorns.COUNT);
        CrystalThorns.Thorn[] c = CrystalThorns.thorns(1235, CrystalThorns.COUNT);
        boolean same = a.length == CrystalThorns.COUNT;
        boolean differ = false;
        for (int i = 0; i < a.length; i++) {
            same &= Arrays.equals(a[i].root(), b[i].root()) && Arrays.equals(a[i].direction(), b[i].direction()) && a[i].size() == b[i].size()
                && a[i].delay() == b[i].delay();
            differ |= !Arrays.equals(a[i].direction(), c[i].direction());
        }
        check(same, "every client lays out the same thorns from the seed");
        check(differ, "each eruption is its own");
        for (int seed : new int[] {0, 1, 77, 4096, 65535}) {
            CrystalThorns.Thorn[] thorns = CrystalThorns.thorns(seed, CrystalThorns.COUNT);
            double upHeight = 0.0;
            int up = 0;
            double downHeight = 0.0;
            int down = 0;
            int firstPerson = 0;
            for (CrystalThorns.Thorn thorn : thorns) {
                double[] d = thorn.direction();
                double[] r = thorn.root();
                check(Math.abs(dot(d, d) - 1.0) < 1.0E-9, "a thorn points one way (" + seed + ")");
                check(d[1] >= CrystalThorns.LOWEST - 1.0E-9 && d[1] <= 0.97 + 1.0E-9, "none points into the ground under him (" + seed + ")");
                check(Math.abs(r[0]) <= 0.26 && Math.abs(r[2]) <= 0.23 && r[1] >= 0.25 && r[1] <= 1.75, "it leaves his body (" + seed + ")");
                // Outward from where it leaves him (not back across his body).
                double h = Math.hypot(r[0], r[2]);
                check(h < 1.0E-9 || (r[0] * d[0] + r[2] * d[2]) / h > 0.0, "it grows outward (" + seed + ")");
                // Its hidden back stays inside him: INSET back from the root, never out of his other side.
                double[] back = {r[0] - d[0] * CrystalThorns.INSET, r[1] - d[1] * CrystalThorns.INSET, r[2] - d[2] * CrystalThorns.INSET};
                double[] half = CrystalThorns.halfBody(r[1]);
                check(Math.abs(back[0]) <= half[0] + 1.0E-6 && Math.abs(back[2]) <= Math.max(half[1], 0.16) + 1.0E-6, "its back is inside him (" + seed + ")");
                check(thorn.delay() >= 0 && thorn.delay() <= CrystalThorns.STAGGER_TICKS, "it starts within the stagger");
                check(thorn.size() >= CrystalThorns.MIN_SIZE && thorn.size() <= CrystalThorns.MAX_SIZE, "its size is in range");
                if (d[1] > 0.5) {
                    upHeight += r[1];
                    up++;
                } else if (d[1] < -0.2) {
                    downHeight += r[1];
                    down++;
                }
                if (CrystalThorns.clearOfEye(thorn)) {
                    firstPerson++;
                }
            }
            check(up > 0 && down > 0 && upHeight / up > downHeight / down + 0.5, "up from the shoulders and head, down from the legs (" + seed + ")");
            check(firstPerson >= thorns.length / 3 && firstPerson < thorns.length, "in first person he sees many of his own, not those at his eyes ("
                + firstPerson + ")");
            // All round him: no direction (above the lowest) is more than 40 degrees from a thorn.
            double worst = 1.0;
            for (int yaw = 0; yaw < 360; yaw += 15) {
                for (double rise = CrystalThorns.LOWEST + 0.05; rise <= 0.95; rise += 0.1) {
                    double ring = Math.sqrt(1.0 - rise * rise);
                    double[] look = {Math.sin(Math.toRadians(yaw)) * ring, rise, Math.cos(Math.toRadians(yaw)) * ring};
                    double best = -1.0;
                    for (CrystalThorns.Thorn thorn : thorns) {
                        best = Math.max(best, dot(look, thorn.direction()));
                    }
                    worst = Math.min(worst, best);
                }
            }
            check(worst >= Math.cos(Math.toRadians(40.0)), "all round him (" + seed + ": " + Math.toDegrees(Math.acos(worst)) + " degrees)");
        }
        CrystalThorns.Thorn crown = new CrystalThorns.Thorn(new double[] {0.0, 1.72, 0.0}, new double[] {0.0, 1.0, 0.0}, 2.0, 0, 0);
        check(!CrystalThorns.clearOfEye(crown), "the one out of his crown is not drawn in his own first person view");
    }

    private static void growth() {
        CrystalThorns.Thorn[] thorns = CrystalThorns.thorns(9, CrystalThorns.COUNT);
        double highest = 0.0;
        for (CrystalThorns.Thorn thorn : thorns) {
            check(CrystalThorns.out(thorn, thorn.delay(), Double.NaN) == 0.0 && CrystalThorns.out(thorn, -3.0, Double.NaN) == 0.0, "in until it starts");
            double full = thorn.delay() + CrystalThorns.GROW_TICKS;
            check(Math.abs(CrystalThorns.out(thorn, full, Double.NaN) - 1.0) < 1.0E-9 && CrystalThorns.out(thorn, 200.0, Double.NaN) == 1.0, "then out");
            check(full <= CrystalThorns.RELEASE_TICKS / 2.0, "every thorn is out well before the grab lets go");
            for (double age = thorn.delay(); age <= full; age += 0.25) {
                double out = CrystalThorns.out(thorn, age, Double.NaN);
                check(out >= 0.0, "never inside out");
                highest = Math.max(highest, out);
            }
            check(CrystalThorns.out(thorn, 50.0, 0.0) == 1.0 && CrystalThorns.out(thorn, 50.0, CrystalThorns.SINK_TICKS) == 0.0
                && CrystalThorns.out(thorn, 50.0, CrystalThorns.SINK_TICKS / 2.0) > 0.0, "it sinks back over the sink time");
            double[] tip = CrystalThorns.tip(thorn, 1.0);
            double reach = Math.sqrt(Math.pow(tip[0] - thorn.root()[0], 2) + Math.pow(tip[1] - thorn.root()[1], 2) + Math.pow(tip[2] - thorn.root()[2], 2));
            check(Math.abs(reach - CrystalSpike.LENGTH * thorn.size()) < 1.0E-9, "out, it reaches its full length");
            // Drawn from the tip back: its back is INSET inside the root at every stage.
            for (double out : new double[] {0.05, 0.5, 1.0, 1.05}) {
                double drawn = CrystalThorns.drawnSize(thorn, out) * CrystalSpike.LENGTH;
                double shown = CrystalSpike.LENGTH * thorn.size() * out;
                check(Math.abs(drawn - shown - CrystalThorns.INSET) < 1.0E-9, "its back stays inside him as it grows");
            }
        }
        check(highest > 1.0 && highest < 1.1, "they snap out a little past full (" + highest + ")");
    }
}
