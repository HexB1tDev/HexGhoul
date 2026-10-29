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
        // 7.0.0: Owl DNA is dormant; 1.2.5: and Graft DNA (never pickable, the user's ask): naming either reads as none (the player
        // chooses again), and the starting screen lists neither.
        check(KagunePath.PALE.dormant() && KagunePath.GRAFT.dormant() && KagunePath.PALE.active() == KagunePath.NONE
            && KagunePath.GRAFT.active() == KagunePath.NONE && !KagunePath.RINKAKU.dormant() && !KagunePath.UKAKU.dormant(),
            "Owl DNA and Graft DNA are dormant");
        check(KagunePath.fromId("owl") == KagunePath.NONE && KagunePath.fromId("graft") == KagunePath.NONE, "a dormant path's id reads as none");
        check(KagunePath.choosable().equals(java.util.List.of(KagunePath.RINKAKU, KagunePath.UKAKU)),
            "the starting screen offers Rinkaku and Ukaku, never a dormant path");
        check(KagunePath.fromId("rinkaku") == KagunePath.RINKAKU && KagunePath.fromId("ukaku") == KagunePath.UKAKU,
            "the saved ids read back (1.2.5: new ids; older saves choose again)");
        check(KagunePath.fromId(null) == KagunePath.NONE, "a missing id means undecided");
        check(KagunePath.fromId("") == KagunePath.NONE, "an empty tag means undecided");
        check(KagunePath.fromId("touka") == KagunePath.NONE, "an id from a newer version asks the player to choose again");
        check(!KagunePath.NONE.chosen() && KagunePath.RINKAKU.chosen() && KagunePath.UKAKU.chosen(), "only real paths count as chosen");
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
        check(!KagunePath.choiceOwed(true, KagunePath.RINKAKU) && !KagunePath.choiceOwed(true, KagunePath.UKAKU),
            "a chosen path is never asked again");
        check(!KagunePath.choiceOwed(false, KagunePath.UKAKU), "a locked kagune with a path owes nothing");
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
        check(KagunePath.forUnmarkedProfile(true, 1, 0L, true) == KagunePath.RINKAKU, "any mastery keeps the original path");
        check(KagunePath.forUnmarkedProfile(true, 0, 1L << 5, true) == KagunePath.RINKAKU, "any mutation keeps the original path");
        check(KagunePath.forUnmarkedProfile(true, 0, 0L, false) == KagunePath.RINKAKU, "a grown form keeps the original path");
        check(KagunePath.forUnmarkedProfile(true, 1000, -1L, false) == KagunePath.RINKAKU, "a veteran is never asked");
        check(KagunePath.forUnmarkedProfile(false, 1000, -1L, false) == KagunePath.NONE,
            "a locked profile is never auto-assigned; it chooses when unlocked again");
    }
}
