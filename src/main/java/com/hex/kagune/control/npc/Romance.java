package com.hex.kagune.control.npc;

/**
 * 1.3.2: love with the one-of-a-kind NPCs (pure, tested), the same for every one of them (Eto first; any NPC made later uses it as
 * it is). An NPC has at most one partner in the whole world, and a player at most one spouse: dating, then married with a ring. Once
 * begun it is for good, no breaking up, no divorce; only the partner killing the NPC ends it. A partner is never fought back (the NPC
 * is hurt, cross, and sulks instead), and gets the NPC's warm talk; every other player gets the cold, claimed one. Love for another
 * NPC behind a partner's back makes the partner jealous ({@link #jealousy}).
 */
public final class Romance {
    /** How one player and one NPC stand. */
    public enum Stage {
        NONE, DATING, MARRIED;

        public static Stage byOrdinal(int ordinal) {
            Stage[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }

        public boolean together() {
            return this != NONE;
        }
    }

    /** What the player can be warned of, privately: the NPC wants something of them (a sound and a line only they get). */
    public enum Want {
        KISS("wants a kiss. Hold Shift when she leans in."),
        HUG("wants a hug. Stay close."),
        TALK("wants to talk to you."),
        NIGHT("is waiting for you. It's late..."),
        HELP("is fighting. Hurry."),
        MISSED("missed you.");

        private final String text;

        Want(String text) {
            this.text = text;
        }

        public String text() {
            return this.text;
        }
    }

    /** At least this regard, and dating at least this many game days, before a ring is taken. */
    public static final int PROPOSE_REGARD = 90;
    public static final long PROPOSE_DAYS = 1L;
    /** What a partner's jealousy costs: love shown to another NPC, more if seen, more again once married. */
    public static final int JEALOUS_HEARD = -6;
    public static final int JEALOUS_SEEN = -15;
    public static final int MARRIED_TWICE = 2;
    /** A partner's hit: her regard lost (she never hits back). */
    public static final int PARTNER_BLOW = -8;
    /** How long a partner hit or left hanging is ignored (game ticks: two minutes). */
    public static final long PARTNER_SULK = 2400L;

    private Romance() {
    }

    /** Why a ring would not be taken, or null if it will. */
    public enum Refusal {
        NOT_DATING, CLAIMED, MARRIED_ELSEWHERE, TOO_SOON, NOT_CLOSE, ALREADY
    }

    /**
     * Whether a proposal is taken: {@code stage} how they stand, {@code claimed} the NPC is someone else's, {@code spouseElsewhere} the
     * player is married to another NPC, {@code days} dating so far, {@code regard} the NPC's.
     */
    public static Refusal propose(Stage stage, boolean claimed, boolean spouseElsewhere, long days, int regard) {
        if (stage == Stage.MARRIED) {
            return Refusal.ALREADY;
        }
        if (claimed) {
            return Refusal.CLAIMED;
        }
        if (stage != Stage.DATING) {
            return Refusal.NOT_DATING;
        }
        if (spouseElsewhere) {
            return Refusal.MARRIED_ELSEWHERE;
        }
        if (days < PROPOSE_DAYS) {
            return Refusal.TOO_SOON;
        }
        return regard < PROPOSE_REGARD ? Refusal.NOT_CLOSE : null;
    }

    /** True if a player may begin dating an NPC: it has no partner and they are married to no one else. */
    public static boolean mayDate(boolean claimed, boolean spouseElsewhere) {
        return !claimed && !spouseElsewhere;
    }

    /** What a partner loses for love shown to another NPC: {@code seen} with their own eyes, {@code stage} theirs. */
    public static int jealousy(Stage stage, boolean seen) {
        if (!stage.together()) {
            return 0;
        }
        int base = seen ? JEALOUS_SEEN : JEALOUS_HEARD;
        return stage == Stage.MARRIED ? base * MARRIED_TWICE : base;
    }

    /** The line over the NPC's head: "Dating Steve", "Married to Steve" (empty: no partner). */
    public static String tag(Stage stage, String partner) {
        if (!stage.together() || partner == null || partner.isEmpty()) {
            return "";
        }
        return stage == Stage.MARRIED ? "Married to " + partner : "Dating " + partner;
    }

    /** Whole game days since {@code day} (0 the same day; never below 0). */
    public static long days(long day, long today) {
        return day < 0L ? 0L : Math.max(0L, today - day);
    }
}
