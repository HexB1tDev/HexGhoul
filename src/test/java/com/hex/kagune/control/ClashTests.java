package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** 1.0.40: two players' M1s meeting ({@link ClashRules}): when they meet, and when they don't. */
public final class ClashTests {
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
        // The other blow on its way: from the tick it set out to the tick it lands, and no later.
        check(ClashRules.onItsWay(100, 106, 100) && ClashRules.onItsWay(100, 106, 103) && ClashRules.onItsWay(100, 106, 106),
            "a blow is on its way from its start to its landing");
        check(!ClashRules.onItsWay(100, 106, 99) && !ClashRules.onItsWay(100, 106, 107), "not before it set out, nor after it landed");
        // Facing: straight at him, a little off, side on, away.
        check(ClashRules.faces(0.0, 1.0, 0.0, 3.0), "looking straight at him");
        check(ClashRules.faces(Math.sin(Math.toRadians(50.0)), Math.cos(Math.toRadians(50.0)), 0.0, 3.0), "50 degrees off still meets");
        check(!ClashRules.faces(1.0, 0.0, 0.0, 3.0), "side on does not");
        check(!ClashRules.faces(0.0, -1.0, 0.0, 3.0), "looking away does not");
        check(ClashRules.faces(0.0, 1.0, 0.0, 0.0), "on top of each other always meets");
        check(!ClashRules.faces(0.0, 0.0, 0.0, 3.0), "looking straight up or down does not");
        // Reach: within the other blow's reach and a little past it.
        check(ClashRules.reaches(4.0, 3.0) && !ClashRules.reaches(4.6, 3.0), "within its reach and the slack past it, no further");
        // All of it.
        check(ClashRules.meets(100, 106, 104, 0.0, 1.0, 0.0, 3.0, 3.0, 3.5), "a blow on its way, aimed at him, reaching him: they meet");
        check(!ClashRules.meets(100, 106, 108, 0.0, 1.0, 0.0, 3.0, 3.0, 3.5), "the other already landed: it does not");
        check(!ClashRules.meets(100, 106, 104, 1.0, 0.0, 0.0, 3.0, 3.0, 3.5), "the other aimed elsewhere: it does not");
        check(!ClashRules.meets(100, 106, 104, 0.0, 1.0, 0.0, 9.0, 9.0, 3.5), "the other out of its reach: it does not");
        check(ClashRules.PUSH > 0.0 && ClashRules.LIFT > 0.0 && ClashRules.STAGGER > 0, "both are thrown apart and kept a moment from striking again");
        shards();
        grabs();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Clash tests: " + passed + " checks passed");
    }

    /** 1.0.42: an M1 on its way meets a shard flying at the one swinging it, in front of him and within reach; nothing else. */
    static void shards() {
        double[] chest = {0.0, 1.1, 0.0};
        // He looks along +z; a shard comes in down -z from 6 blocks off, crossing 2 blocks in front of him this tick.
        double[] from = {0.0, 1.2, 6.0};
        double[] to = {0.0, 1.2, 2.0};
        double t = ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, from, to, 3.0);
        check(t >= 0.0 && t <= 1.0, "a shard coming straight at him through his blow's reach is met: " + t);
        double z = from[2] + (to[2] - from[2]) * t;
        check(Math.abs(Math.sqrt(z * z + 0.01) - (3.0 + ClashRules.SHARD_SLACK)) < 1.0E-6, "met as it comes into his reach: " + z);
        check(ClashRules.swats(100, 106, 107, 0.0, 1.0, chest, from, to, 3.0) < 0.0, "not once his blow has landed");
        check(ClashRules.swats(100, 106, 99, 0.0, 1.0, chest, from, to, 3.0) < 0.0, "not before it set out");
        check(ClashRules.swats(100, 106, 103, 0.0, -1.0, chest, from, to, 3.0) < 0.0, "not with his back to it");
        check(ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, new double[] {0.0, 1.2, 14.0}, new double[] {0.0, 1.2, 10.0}, 3.0) < 0.0,
            "not while it is still out of reach");
        check(ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, new double[] {0.0, 1.2, 2.0}, new double[] {0.0, 1.2, 6.0}, 3.0) < 0.0,
            "not one flying away from him");
        check(ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, new double[] {6.0, 1.2, 3.0}, new double[] {-6.0, 1.2, 3.0}, 3.0) >= 0.0,
            "one crossing in front of him, in reach, is met");
        double through = ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, new double[] {0.0, 1.2, 4.0}, new double[] {0.0, 1.2, -4.0}, 3.0);
        check(through >= 0.0 && through < 0.5, "a fast one flying right through him is met before it reaches him: " + through);
        check(ClashRules.SHARDS_PER_BLOW > 0 && ClashRules.SHARD_SLACK >= 0.0, "a blow meets a few shards");
        // 1.0.43: a blow that met shards is spent on them: from when it set out to just after it lands.
        check(ClashRules.spent(100, 106, 100) && ClashRules.spent(100, 106, 107), "a blow that swatted lands on nothing");
        check(!ClashRules.spent(100, 106, 99) && !ClashRules.spent(100, 106, 109), "but only that blow");
    }

    /** 1.0.42: Rinkaku's grab, slower and reeling in only at its end, broken by a swing since it set out. */
    static void grabs() {
        check(GrabRules.reach(9) == 18, "the grab takes twice the original's 9 ticks: " + GrabRules.reach(9));
        check(GrabRules.reelFrom(18) == 12, "it reels in over its last 6 ticks: " + GrabRules.reelFrom(18));
        check(GrabRules.reelFrom(9) == 3 && GrabRules.reelFrom(2) == 3, "a reach left as the original's reels in as the original's did");
        for (int original = 2; original <= 20; original++) { // (the original's cooldown() never goes under 2)
            int reach = GrabRules.reach(original);
            check(reach > original && GrabRules.reelFrom(reach) < reach, "slower, and caught before it ends: " + original);
        }
        check(GrabRules.counts(100, 104, 110), "a swing after it set out is judged");
        check(GrabRules.counts(100, 99, 110), "a swing a moment before it set out is judged too (mashing is caught out)");
        check(!GrabRules.counts(100, 90, 110), "an old one is not");
        check(!GrabRules.counts(100, 111, 110), "one yet to come is not");
        // 1.0.43: the window is the last 8 ticks before it holds.
        check(GrabRules.windowOpens(18) == 10 && GrabRules.windowOpens(4) == 0, "the window opens 8 ticks before it holds: " + GrabRules.windowOpens(18));
        check(!GrabRules.inWindow(100, 18, 100) && !GrabRules.inWindow(100, 18, 109), "a swing as it sets out, or too early, is not in it");
        check(GrabRules.inWindow(100, 18, 110) && GrabRules.inWindow(100, 18, 117), "one in its last 8 ticks is");
        check(GrabRules.windowOpens(18) <= GrabRules.reelFrom(18), "the window is open before it starts reeling in");
        check(GrabRules.WINDOW >= 6, "the window is long enough to react in (" + GrabRules.WINDOW + " ticks)");
    }
}
