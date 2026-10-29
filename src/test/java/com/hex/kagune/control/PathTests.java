package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dependency-free tests for the kagune path model: stored ids, network ordinals and how old saves are classified. */
public final class PathTests {
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
        ids();
        ordinals();
        unmarkedProfiles();
        owed();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Path tests: " + passed + " checks passed");
    }

    private static void ids() {
        Set<String> seen = new HashSet<>();
        for (KagunePath path : KagunePath.values()) {
            check(seen.add(path.id()), "path ids are unique: " + path.id());
            check(KagunePath.fromId(path.id()) == path.active(), "stored id round-trips: " + path);
            check(KagunePath.fromId(" " + path.id().toUpperCase() + " ") == path.active(), "stored ids are read leniently: " + path);
            check(path.id().matches("[a-z]+"), "ids are plain lowercase words (NBT and command friendly): " + path.id());
        }
        // 7.0.0: Owl DNA is dormant; every way of naming it lands on Furuta DNA, and nothing lists it.
        check(KagunePath.ETO.dormant() && KagunePath.ETO.active() == KagunePath.FURUTA && !KagunePath.FURUTA.dormant(),
            "Owl DNA is dormant, Furuta DNA replaces it");
        check(KagunePath.fromId("owl") == KagunePath.FURUTA && KagunePath.fromId("eto") == KagunePath.FURUTA
            && KagunePath.fromId("furuta") == KagunePath.FURUTA, "Owl DNA's saved ids read as Furuta DNA");
        check(!KagunePath.choosable().contains(KagunePath.ETO) && KagunePath.choosable().contains(KagunePath.FURUTA),
            "the starting screen offers Furuta DNA, never Owl DNA");
        check(KagunePath.fromId("kaneki") == KagunePath.KANEKI && KagunePath.fromId("ayato") == KagunePath.AYATO,
            "the saved ids never change (old worlds depend on them)");
        check(KagunePath.fromId(null) == KagunePath.NONE, "a missing id means undecided");
        check(KagunePath.fromId("") == KagunePath.NONE, "an empty tag means undecided");
        check(KagunePath.fromId("touka") == KagunePath.NONE, "an id from a newer version asks the player to choose again");
        check(!KagunePath.NONE.chosen() && KagunePath.KANEKI.chosen() && KagunePath.AYATO.chosen(), "only real paths count as chosen");
    }

    private static void ordinals() {
        for (KagunePath path : KagunePath.values()) {
            check(KagunePath.byOrdinal(path.ordinal()) == path.active(), "network ordinal round-trips: " + path);
            check(path.ordinal() < 128, "ordinal fits the signed byte on the wire: " + path);
        }
        check(KagunePath.byOrdinal(-1) == KagunePath.NONE && KagunePath.byOrdinal(99) == KagunePath.NONE,
            "a corrupt ordinal decodes to undecided instead of crashing the client");
    }

    private static void owed() {
        check(KagunePath.choiceOwed(true, KagunePath.NONE), "an unlocked, undecided kagune owes the starting screen");
        check(!KagunePath.choiceOwed(false, KagunePath.NONE), "no kagune, no screen");
        check(!KagunePath.choiceOwed(true, KagunePath.KANEKI) && !KagunePath.choiceOwed(true, KagunePath.AYATO),
            "a chosen path is never asked again");
        check(!KagunePath.choiceOwed(false, KagunePath.AYATO), "a locked kagune with a path owes nothing");
        // Legacy saves: the unmarked-profile rule and the owed rule agree that veterans are never blocked.
        check(!KagunePath.choiceOwed(true, KagunePath.forUnmarkedProfile(true, 300, 0L, false)),
            "a 1.1 veteran can still manifest after updating");
        check(KagunePath.choiceOwed(true, KagunePath.forUnmarkedProfile(true, 0, 0L, true)),
            "a fresh unlock is asked to choose before the first manifestation");
    }

    private static void unmarkedProfiles() {
        check(KagunePath.forUnmarkedProfile(true, 0, 0L, true) == KagunePath.NONE,
            "an unlocked but untouched kagune gets the starting screen");
        check(KagunePath.forUnmarkedProfile(false, 0, 0L, true) == KagunePath.NONE,
            "a player without a kagune chooses once it is unlocked");
        check(KagunePath.forUnmarkedProfile(true, 1, 0L, true) == KagunePath.KANEKI, "any mastery keeps the original path");
        check(KagunePath.forUnmarkedProfile(true, 0, 1L << 5, true) == KagunePath.KANEKI, "any mutation keeps the original path");
        check(KagunePath.forUnmarkedProfile(true, 0, 0L, false) == KagunePath.KANEKI, "a grown form keeps the original path");
        check(KagunePath.forUnmarkedProfile(true, 1000, -1L, false) == KagunePath.KANEKI, "a veteran is never asked");
        check(KagunePath.forUnmarkedProfile(false, 1000, -1L, false) == KagunePath.NONE,
            "a locked profile is never auto-assigned; it chooses when unlocked again");
    }
}
