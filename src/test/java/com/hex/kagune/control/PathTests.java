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
        colours();
        tints();
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

    /**
     * 1.3.34: a Rinkaku kagune's random colour: any hue, never a muddy grey or too dark to see (the rare black and bone excepted), its
     * veins its own hue and much darker; the original's crimson is the default it replaces; names and hex read right.
     */
    private static void colours() {
        java.util.Random random = new java.util.Random(7L);
        int[] hues = new int[12];
        int rare = 0;
        for (int i = 0; i < 4000; i++) {
            KaguneColours.Pick pick = KaguneColours.roll(random::nextDouble);
            if (pick.colour() == KaguneColours.BLACK || pick.colour() == KaguneColours.BONE) {
                rare++;
                continue;
            }
            double[] hsv = KaguneColours.toHsv(pick.colour());
            double[] vein = KaguneColours.toHsv(pick.vein());
            check(hsv[1] >= 0.38 && hsv[2] >= 0.34, "a rolled colour is vivid and light enough to see: " + KaguneColours.hex(pick.colour()));
            double apart = Math.abs(vein[0] - hsv[0]);
            check(vein[2] < hsv[2] * 0.3 && (apart < 0.05 || apart > 0.95),
                "its veins are its own hue, much darker: " + KaguneColours.hex(pick.vein()));
            hues[(int)(hsv[0] * 12) % 12]++;
        }
        for (int i = 0; i < 12; i++) {
            check(hues[i] > 150, "every hue comes up (" + i + ": " + hues[i] + ")");
        }
        check(rare > 150 && rare < 500, "the rare black and bone come up now and then (" + rare + " of 4000)");
        check(KaguneColours.DEFAULT == com.hex.kagune.data.KaguneProfile.DEFAULT_COLOR, "the default it replaces is the original's crimson");
        check(KaguneColours.hex(0x0A0B0C).equals("#0A0B0C") && KaguneColours.name(KaguneColours.hsv(0.455, 0.8, 0.7)).endsWith("teal")
            && KaguneColours.name(KaguneColours.hsv(0.75, 0.9, 0.9)).contains("violet") && KaguneColours.name(KaguneColours.BONE).equals("bone white"),
            "a colour's name and hex");
        check(KaguneColours.hsv(0.0, 1.0, 1.0) == 0xFF0000 && KaguneColours.hsv(1.0 / 3.0, 1.0, 1.0) == 0x00FF00, "hsv round trips");
    }

    /**
     * 1.3.35: Ukaku's red palette turned to a kagune's colour: the crimson leaves it as it is; another turns its heart to that hue and
     * keeps its depths deeper and its rims lighter; the near-black is black with a red glow; alpha kept; a wild ghoul's colour is fixed
     * by its seed, varied across seeds, sometimes the crimson.
     */
    private static void tints() {
        int heart = 0xE3161C;   // the wings' red
        int depth = 0x1B1031;   // the crystal's dark
        int rim = 0xF5C4E6;     // a rim
        check(KaguneTint.recolour(0x80123456, KaguneColours.DEFAULT) == 0x80123456, "the crimson draws the palette as it always was");
        for (int target : new int[] {0x1F8A7A, 0x6A2BC9, 0xD9A21A, 0x2E5FD6, 0x3FAF3A}) {
            int h = KaguneTint.recolour(0xFF000000 | heart, target);
            double[] got = KaguneColours.toHsv(h & 0xFFFFFF);
            double[] want = KaguneColours.toHsv(target);
            double apart = Math.abs(got[0] - want[0]);
            check(Math.min(apart, 1.0 - apart) < 0.03, "the heart turns to " + KaguneColours.hex(target) + " (" + KaguneColours.hex(h) + ")");
            check((h >>> 24) == 0xFF && (KaguneTint.recolour(0x40000000 | heart, target) >>> 24) == 0x40, "alpha kept");
            double d = KaguneColours.toHsv(KaguneTint.recolour(depth, target) & 0xFFFFFF)[2];
            double r = KaguneColours.toHsv(KaguneTint.recolour(rim, target) & 0xFFFFFF)[2];
            check(d < got[2] && r > d, "the depths stay deeper than the heart, the rims lighter (" + KaguneColours.hex(target) + ")");
        }
        double[] black = KaguneColours.toHsv(KaguneTint.recolour(depth, KaguneColours.BLACK) & 0xFFFFFF);
        double[] blackRim = KaguneColours.toHsv(KaguneTint.recolour(rim, KaguneColours.BLACK) & 0xFFFFFF);
        double[] blackGlow = KaguneColours.toHsv(KaguneTint.recolour(heart, KaguneColours.BLACK) & 0xFFFFFF);
        check(black[2] < 0.1 && blackRim[2] < 0.3 && blackGlow[1] > 0.6 && blackGlow[2] > 0.7, "the near-black: black, its glow still red");
        double[] bone = KaguneColours.toHsv(KaguneTint.recolour(heart, KaguneColours.BONE) & 0xFFFFFF);
        check(bone[1] < 0.2 && bone[2] > 0.6, "bone white: pale and nearly colourless");
        check(KaguneTint.wild(1234) == KaguneTint.wild(1234), "a wild ghoul's colour is its own, every time");
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        int crimson = 0;
        for (int seed = 0; seed < 400; seed++) {
            int c = KaguneTint.wild(seed);
            seen.add(c);
            crimson += c == KaguneColours.DEFAULT ? 1 : 0;
        }
        check(seen.size() > 300 && crimson > 25 && crimson < 110, "wild ghouls vary, the crimson now and then (" + seen.size() + ", " + crimson + ")");
    }
}
