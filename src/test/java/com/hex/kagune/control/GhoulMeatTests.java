package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** 7.0.1: Ghoul Meat: every piece adds strength and RC endurance, less each time but never nothing, toward a fixed limit. */
public final class GhoulMeatTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        curves();
        food();
        text();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Ghoul meat tests: " + passed + " checks passed");
    }

    static void curves() {
        check(GhoulMeat.strength(0) == 1.0F && GhoulMeat.endurance(0) == 1.0F, "none eaten: nothing changes");
        check(GhoulMeat.strength(-5) == 1.0F && GhoulMeat.endurance(-5) == 1.0F, "a bad count counts as none");
        float lastStrength = 1.0F;
        float lastEndurance = 1.0F;
        double lastStep = Double.MAX_VALUE;
        for (int n = 1; n <= 400; n++) {
            float strength = GhoulMeat.strength(n);
            float endurance = GhoulMeat.endurance(n);
            check(strength > lastStrength && endurance > lastEndurance, "piece " + n + " adds to both");
            double step = GhoulMeat.share(n) - GhoulMeat.share(n - 1);
            check(step < lastStep, "piece " + n + " adds less than the one before");
            check(strength < 1.0F + GhoulMeat.STRENGTH_MAX + 1.0E-6F && endurance < 1.0F + GhoulMeat.ENDURANCE_MAX + 1.0E-6F, "piece " + n + " stays under the limit");
            lastStrength = strength;
            lastEndurance = endurance;
            lastStep = step;
        }
        // The pace: a first kill shows; ten are a real edge; a long hunt nears the limit.
        check(GhoulMeat.strength(1) >= 1.009F, "one piece: about +1% strength");
        check(GhoulMeat.strength(10) > 1.08F && GhoulMeat.strength(10) < 1.10F, "ten pieces: about +9% strength: " + GhoulMeat.strength(10));
        check(GhoulMeat.endurance(10) > 1.2F && GhoulMeat.endurance(10) < 1.25F, "ten pieces: RC lasts about x1.22: " + GhoulMeat.endurance(10));
        check(GhoulMeat.strength(100) > 1.36F && GhoulMeat.endurance(100) > 1.9F, "a hundred pieces: near +40% and x2");
    }

    static void food() {
        check(FoodClassifier.classify(GhoulMeat.ID, true) == FoodClassifier.Diet.GHOUL_FOOD, "a ghoul can eat it (raw flesh)");
        check(GhoulMeat.NUTRITION * GhoulReserve.PER_NUTRITION >= GhoulReserve.MAX * 0.5F, "it fills at least half the flesh reserve");
    }

    static void text() {
        check(GhoulMeat.percent(1.0F).equals("+0%") && GhoulMeat.percent(1.123F).equals("+12%"), "percent text");
        check(GhoulMeat.times(1.0F).equals("x1.00") && GhoulMeat.times(1.2249F).equals("x1.22"), "times text");
    }
}
