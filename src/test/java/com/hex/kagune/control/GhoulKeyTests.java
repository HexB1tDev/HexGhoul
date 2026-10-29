package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** 3.0.0: R is ghoul mode (eyes only) or everything in; Alt + R unleashes the kagune. */
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
        check(!r.toggleKagune() && r.ghoul(), "R opens the kakugan without the kagune");
        r = GhoulKey.press(false, true, false);
        check(!r.toggleKagune() && !r.ghoul(), "R again closes it");
        r = GhoulKey.press(false, false, true);
        check(r.toggleKagune() && r.ghoul(), "Alt + R unleashes the kagune, eyes and all");
        r = GhoulKey.press(false, true, true);
        check(r.toggleKagune() && r.ghoul(), "Alt + R from ghoul mode unleashes the kagune");
        for (boolean ghoul : new boolean[] {false, true}) {
            r = GhoulKey.press(true, ghoul, false);
            check(r.toggleKagune() && !r.ghoul(), "R with the kagune out puts everything away");
            r = GhoulKey.press(true, ghoul, true);
            check(!r.toggleKagune() && r.ghoul() == ghoul, "Alt + R with the kagune out changes nothing");
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
