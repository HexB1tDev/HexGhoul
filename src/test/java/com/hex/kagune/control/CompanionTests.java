package com.hex.kagune.control;

import com.hex.kagune.control.npc.HandSway;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.13 on: the companions (pure rules, no game): hands held alive; the personas every one-of-a-kind NPC is built from (Eto, Touka,
 * Ken), their lines, their lessons.
 */
public final class CompanionTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        hands();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Companion tests: " + passed + " checks passed");
    }

    static void hands() {
        // Standing still: she rests at their side, the hands sway anyway (never still for long).
        HandSway still = new HandSway();
        for (int i = 0; i < 200; i++) {
            still.step(0.0, 0.0);
        }
        check(Math.abs(still.trail) < 1.0E-6 && Math.abs(still.out) < 1.0E-6, "standing still, she rests right at their side");
        double low = Double.MAX_VALUE;
        double high = -Double.MAX_VALUE;
        for (int t = 0; t < 400; t++) {
            double sway = HandSway.sway(t);
            low = Math.min(low, sway);
            high = Math.max(high, sway);
        }
        check(high - low > 0.15 && high <= HandSway.SWAY + 1.0E-9 && low >= -HandSway.SWAY - 1.0E-9,
            "the joined hands sway forward and back, about a dozen degrees all told, never past their bound: " + low + ".." + high);

        // Walking: she trails behind, settled, about a third of a block; sprinting further; never past the bound.
        HandSway walk = new HandSway();
        double most = 0.0;
        for (int i = 0; i < 200; i++) {
            walk.step(0.216, 0.0);
            most = Math.max(most, walk.trail);
        }
        check(walk.trail > 0.3 && walk.trail < 0.5, "walking, she trails a little behind, pulled by the hand: " + walk.trail);
        check(most <= HandSway.MAX_TRAIL + 0.15, "the spring never throws her past its bound: " + most);
        HandSway run = new HandSway();
        for (int i = 0; i < 200; i++) {
            run.step(0.28, 0.0);
        }
        check(run.trail > walk.trail && run.trail <= HandSway.MAX_TRAIL + 1.0E-9, "sprinting, a little further behind, still within reach: " + run.trail);

        // Stopping: her own momentum carries her a touch ahead, then she settles back at their side.
        double ahead = 0.0;
        for (int i = 0; i < 120; i++) {
            walk.step(0.0, 0.0);
            ahead = Math.min(ahead, walk.trail);
        }
        check(ahead < -0.01, "stopping, she swings a step ahead before she settles (her momentum): " + ahead);
        check(Math.abs(walk.trail) < 0.01, "and settles back at their side: " + walk.trail);

        // Turning: at their right she swings out turning left (the outside) and in turning right.
        HandSway left = new HandSway();
        HandSway right = new HandSway();
        for (int i = 0; i < 12; i++) {
            left.step(0.2, -9.0);
            right.step(0.2, 9.0);
        }
        check(left.out > 0.05 && right.out < -0.05, "turning away from her she swings out wide, toward her she comes in: " + left.out + " / " + right.out);

        // The arms: the holder's swings back as she trails, hers forward, both by the same reach (each half the gap), same sway.
        float[] holder = HandSway.arm(0.4, 0.0, 0.0, true, 1.0);
        float[] her = HandSway.arm(0.4, 0.0, 0.0, false, 1.0);
        check(holder[0] > 0.2F && her[0] < -0.2F && Math.abs(holder[0] + her[0] + 2.0 * HandSway.sway(0.0)) < 1.0E-5,
            "trailing, the holder's arm reaches back and hers forward, meeting halfway: " + holder[0] + " / " + her[0]);
        float[] smaller = HandSway.arm(0.4, 0.0, 0.0, false, 0.9);
        check(smaller[0] < her[0], "a shorter arm turns further to reach its half");
        float[] swayed = HandSway.arm(0.0, 0.0, HandSway.SLOW_QUARTER, true, 1.0);
        float[] swayedHer = HandSway.arm(0.0, 0.0, HandSway.SLOW_QUARTER, false, 1.0);
        check(swayed[0] < -0.05F && Math.abs(swayed[0] - swayedHer[0]) < 1.0E-5, "the sway takes both hands forward together, the same way");
        check(HandSway.pulled(0.6) > HandSway.pulled(0.3) && HandSway.pulled(-0.2) == 0.0 && HandSway.pulled(5.0) <= 0.16 + 1.0E-9,
            "pulled along she leans in a little, more the further she trails, never when ahead");
        HandSway bad = new HandSway();
        bad.step(Double.NaN, 4.0);
        bad.step(0.1, Double.POSITIVE_INFINITY);
        check(Double.isFinite(bad.trail) && Double.isFinite(bad.out), "nothing unreadable ever reaches the spring");
        check(Math.abs(HandSway.reach(50.0, 1.0)) < Math.PI / 2.0, "a gap too long for the arm turns it most of the way, never wraps round");
    }
}
