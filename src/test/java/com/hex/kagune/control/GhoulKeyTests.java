package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** 3.0.0: R is ghoul mode (eyes only); 1.2.0: from the kagune, R goes back to kakugan mode and Ctrl + R puts everything away. */
public final class GhoulKeyTests {
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
        GhoulKey r = GhoulKey.press(false, false, false);
        check(!r.toggleKagune() && r.ghoul(), "R opens the kakugan without the kagune (kakugan mode)");
        r = GhoulKey.press(false, true, false);
        check(!r.toggleKagune() && !r.ghoul(), "R again closes it");
        r = GhoulKey.press(false, false, true);
        check(r.toggleKagune() && r.ghoul(), "Ctrl + R unleashes the kagune, eyes and all");
        r = GhoulKey.press(false, true, true);
        check(r.toggleKagune() && r.ghoul(), "Ctrl + R from kakugan mode unleashes the kagune");
        for (boolean ghoul : new boolean[] {false, true}) {
            // 1.2.0: R with the kagune out goes back to kakugan mode; Ctrl + R puts everything away.
            r = GhoulKey.press(true, ghoul, false);
            check(r.toggleKagune() && r.ghoul(), "R with the kagune out puts the kagune away and keeps the eyes open");
            r = GhoulKey.press(true, ghoul, true);
            check(r.toggleKagune() && !r.ghoul(), "Ctrl + R with the kagune out puts everything away");
        }
        // Round trips: nothing, R, Ctrl + R, R, R: eyes, kagune, eyes, nothing.
        boolean out = false;
        boolean eyes = false;
        String[] steps = {"R", "Ctrl+R", "R", "R"};
        boolean[][] expect = {{false, true}, {true, true}, {false, true}, {false, false}};
        for (int i = 0; i < steps.length; i++) {
            GhoulKey step = GhoulKey.press(out, eyes, steps[i].startsWith("Ctrl"));
            out = step.toggleKagune() != out;
            eyes = step.ghoul();
            check(out == expect[i][0] && eyes == expect[i][1], "after " + steps[i] + " (step " + i + "): kagune " + out + ", eyes " + eyes);
        }
        // 6.1.0: the open eyes drain RC slowly: the kagune's rest recovery is held back and a little more is taken.
        float novice = GhoulBuffs.eyesCost(1.0F);
        float master = GhoulBuffs.eyesCost(1.0F - 0.58F);
        check(novice > GhoulBuffs.KAGUNE_REST_RECOVERY && master > GhoulBuffs.KAGUNE_REST_RECOVERY, "the eyes always drain, never recover");
        check(master < novice, "mastery makes the eyes cheaper");
        float perSecond = (novice - GhoulBuffs.KAGUNE_REST_RECOVERY) * 20.0F;
        check(perSecond > 0.2F && perSecond < 1.0F, "a slow drain: " + perSecond + " RC a second at mastery 0");
        float lasts = 100.0F / perSecond;
        check(lasts > 120.0F && lasts < 300.0F, "a full reserve keeps the eyes open two to five minutes: " + lasts + " s");
        check(GhoulBuffs.eyesCost(Float.NaN) == novice && GhoulBuffs.eyesCost(7.0F) == novice, "odd efficiencies are clamped");
        check(GhoulBuffs.EYES_RESISTANCE == 0 && GhoulBuffs.EYES_STRENGTH == 0, "a small Resistance and a modest Strength (level I)");
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAIL: " + failure));
            System.exit(1);
        }
        System.out.println("Ghoul key tests: " + passed + " checks passed");
    }
}
