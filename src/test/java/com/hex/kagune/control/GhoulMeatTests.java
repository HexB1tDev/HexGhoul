package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** 7.0.1: Ghoul Meat: every piece adds strength and RC endurance, less each time but never nothing; 1.3.40: with no limit. */
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
        tank();
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
        for (int n = 1; n <= 4000; n++) {
            float strength = GhoulMeat.strength(n);
            float endurance = GhoulMeat.endurance(n);
            check(strength >= lastStrength && endurance > lastEndurance, "piece " + n + " adds to both");
            double step = GhoulMeat.growth(n) - GhoulMeat.growth(n - 1);
            check(step > 0.0 && step < lastStep, "piece " + n + " adds something, and less than the one before");
            lastStrength = strength;
            lastEndurance = endurance;
            lastStep = step;
        }
        // 1.3.40: endless, gentle: no ceiling, a first piece barely shows, a hundred a real edge, a thousand twice the RC.
        check(GhoulMeat.strength(1) > 1.004F && GhoulMeat.strength(1) < 1.01F, "one piece: under +1% strength: " + GhoulMeat.strength(1));
        check(GhoulMeat.strength(100) > 1.20F && GhoulMeat.strength(100) < 1.23F, "a hundred pieces: about +21%: " + GhoulMeat.strength(100));
        check(GhoulMeat.endurance(100) > 1.43F && GhoulMeat.endurance(100) < 1.47F, "a hundred pieces: RC x1.45: " + GhoulMeat.endurance(100));
        check(GhoulMeat.strength(1000) > 1.45F && GhoulMeat.endurance(1000) > 1.95F, "a thousand: about +47% and x1.98");
        check(GhoulMeat.strength(100000) > GhoulMeat.strength(10000) && GhoulMeat.strength(100000) > 2.0F, "it never stops growing");
        check(GhoulMeat.strength(40) < 1.0F + 0.4F * (1.0F - (float)Math.exp(-1.0)), "gentler than before 1.3.40 at forty pieces");
        // A mutant (Eto's one-eyed): harder blows, RC lasting longer, more from every bite.
        check(Mutant.strength(true, 1.0F) == Mutant.STRENGTH && Mutant.strength(false, 1.1F) == 1.1F, "a mutant hits harder");
        check(Math.abs(Mutant.endurance(true, GhoulMeat.endurance(100)) - GhoulMeat.endurance(100) * Mutant.ENDURANCE) < 1.0E-5F,
            "a mutant's RC lasts longer, on top of the meat");
        check(Mutant.feed(true, 8) == 12 && Mutant.feed(false, 8) == 8, "a mutant gets more from a meal");
        check(Mutant.STRENGTH > 1.0F && Mutant.ENDURANCE >= 1.5F && Mutant.FEED > 1.0F, "every mutant bonus is a bonus");
    }

    static void food() {
        check(FoodClassifier.classify(GhoulMeat.ID, true) == FoodClassifier.Diet.GHOUL_FOOD, "a ghoul can eat it (raw flesh)");
        check(GhoulMeat.NUTRITION * GhoulReserve.PER_NUTRITION >= GhoulReserve.MAX * 0.5F, "it fills at least half the flesh reserve");
    }

    static void text() {
        check(GhoulMeat.percent(1.0F).equals("+0%") && GhoulMeat.percent(1.123F).equals("+12%"), "percent text");
        check(GhoulMeat.times(1.0F).equals("x1.00") && GhoulMeat.times(1.2249F).equals("x1.22"), "times text");
    }

    /** 2.0: a ghoul is a living tank: mastery, meat and form each cut a blow, together to about a quarter; a new ghoul takes it all. */
    private static void tank() {
        check(com.hex.kagune.control.GhoulTank.taken(0, 0, 0.0) == 1.0, "a new ghoul with its kagune in takes a blow whole");
        double most = com.hex.kagune.control.GhoulTank.taken(1000, 100000, com.hex.kagune.control.GhoulTank.form(true, 5, true, true));
        check(most > 0.15 && most < 0.22, "a seasoned ghoul in a final kakuja takes about a fifth, before its quests (2.3.6; " + most + ")");
        double prev = 1.0;
        boolean falls = true;
        for (int m = 0; m <= 1000; m += 100) {
            double t = com.hex.kagune.control.GhoulTank.taken(m, 0, 0.0);
            falls &= t <= prev;
            prev = t;
        }
        check(falls && com.hex.kagune.control.GhoulTank.taken(1000, 0, 0.0) < 0.7, "more mastery, less taken");
        check(Math.abs(com.hex.kagune.control.GhoulTank.meat(15) - 0.5) < 1.0E-9 && com.hex.kagune.control.GhoulTank.meat(0) == 0.0
            && com.hex.kagune.control.GhoulTank.meat(200) > 0.9, "meat: half its share at 15 pieces, more for ever");
        // 2.3.6: an endgame ghoul (mastery 1000, every unlock, 100 ghoul meat) is far tougher: about 40 health, under 30% of a blow with
        // its kagune out, about a sixth in a final kakuja.
        double endgameOut = com.hex.kagune.control.GhoulTank.taken(1000, 100, 1.0, com.hex.kagune.control.GhoulTank.form(true, 0, false, false));
        double endgameFinal = com.hex.kagune.control.GhoulTank.taken(1000, 100, 1.0, com.hex.kagune.control.GhoulTank.form(true, 0, true, true));
        double extra = com.hex.kagune.control.GhoulTank.health(1000, 100, 1.0);
        check(endgameOut < 0.3 && endgameFinal < 0.17 && extra > 18.0 && extra <= 20.0, "endgame: " + endgameOut + " of a blow, " + endgameFinal
            + " in a final kakuja, +" + extra + " health");
        check(com.hex.kagune.control.GhoulTank.health(0, 0, 0.0) == 0.0 && com.hex.kagune.control.GhoulTank.taken(0, 0, 0.0, 0.0) == 1.0,
            "a new ghoul: a player's health, every blow whole");
        check((20.0 + extra) / endgameOut > 3.0 * (20.0 / 0.46), "about three times as hard to bring down as before 2.3.6");
        double out = com.hex.kagune.control.GhoulTank.form(true, 0, false, false);
        double quad = com.hex.kagune.control.GhoulTank.form(true, 2, false, false);
        double kakuja = com.hex.kagune.control.GhoulTank.form(true, 5, true, false);
        double fin = com.hex.kagune.control.GhoulTank.form(true, 5, true, true);
        check(com.hex.kagune.control.GhoulTank.form(false, 5, false, false) == 0.0 && out > 0.0 && quad > out && kakuja > quad && fin > kakuja,
            "the better the form, the more it takes off");
        check(com.hex.kagune.control.GhoulTank.bypasses("outOfWorld") && com.hex.kagune.control.GhoulTank.bypasses("genericKill")
            && !com.hex.kagune.control.GhoulTank.bypasses("player"), "the void and a kill command are never softened");
    }
}
