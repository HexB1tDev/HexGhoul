package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** Dependency-free tests for 6.0.4's mastery curve and Kaneki's unlocks. */
public final class MasteryTests {
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
        curve();
        unlocks();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Mastery tests: " + passed + " checks passed");
    }

    /** Each point costs more the higher it is; progress carries over exactly; the top stops it. */
    private static void curve() {
        check(MasteryCurve.cost(0) == 1000 && MasteryCurve.cost(100) == 2000 && MasteryCurve.cost(500) == 6000 && MasteryCurve.cost(900) == 10000,
            "a point costs 1 raw at the start, 2 at 100, 6 at 500, 10 at 900");
        for (int m = 1; m <= MasteryCurve.MAX; m++) {
            check(MasteryCurve.cost(m) > MasteryCurve.cost(m - 1), "it gets harder at every point (" + m + ")");
        }
        // Two kills and their hits (31 raw) at the start come as fast as ever; the same fight at 500 earns a fraction of it.
        int[] fresh = MasteryCurve.earn(0, 0, 31);
        int[] seasoned = MasteryCurve.earn(500, 0, 31);
        check(fresh[0] >= 25 && fresh[0] <= 31, "at the start two kills still earn about as much (" + fresh[0] + ")");
        check(seasoned[0] - 500 == 5, "at 500 the same fight earns five (" + (seasoned[0] - 500) + ")");
        // Earned a point at a time or all at once, the same mastery and the same carry.
        int mastery = 0;
        int carry = 0;
        for (int i = 0; i < 2000; i++) {
            int[] after = MasteryCurve.earn(mastery, carry, 1);
            mastery = after[0];
            carry = after[1];
        }
        int[] once = MasteryCurve.earn(0, 0, 2000);
        check(mastery == once[0] && carry == once[1], "earning a point at a time or all at once comes to the same");
        check(MasteryCurve.rawTo(1000) == 5995 && MasteryCurve.rawTo(100) == 149 && MasteryCurve.rawTo(500) == 1747,
            "1000 takes 5995 raw (it was 1000); 100 takes 149; 500 takes 1747");
        int[] top = MasteryCurve.earn(999, 0, 100000);
        check(top[0] == MasteryCurve.MAX && top[1] == 0, "the top stops it and carries nothing");
        int[] none = MasteryCurve.earn(300, 1500, 0);
        check(none[0] == 300 && none[1] == 1500 && MasteryCurve.earn(300, 1500, -5)[0] == 300, "nothing earned, nothing changes");
        int[] carried = MasteryCurve.earn(100, 1500, 1);
        check(carried[0] == 101 && carried[1] == 500, "progress toward the next point carries over (1.5 + 1 at 100: one point, 0.5 left)");
    }

    /** Kaneki's moves unlock one by one, in a sensible order, all before the kakuja. */
    private static void unlocks() {
        int[] order = {KanekiUnlocks.GRAPPLE, KanekiUnlocks.RAPID_STRIKE, KanekiUnlocks.GRAB, KanekiUnlocks.CRUSH, KanekiUnlocks.LIFT,
            KanekiUnlocks.SLAM};
        check(order[0] > 0, "the seed doesn't start with them");
        for (int i = 1; i < order.length; i++) {
            check(order[i] > order[i - 1], "each unlocks after the one before (" + order[i] + ")");
        }
        check(KanekiUnlocks.SLAM <= 500, "the four tails have every move, long before the kakuja");
        check(!KanekiUnlocks.unlocked(KanekiUnlocks.GRAB - 1, KanekiUnlocks.GRAB) && KanekiUnlocks.unlocked(KanekiUnlocks.GRAB, KanekiUnlocks.GRAB),
            "a move unlocks exactly at its mastery");
    }
}
