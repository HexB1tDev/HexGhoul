package com.hex.kagune.control.npc;

/**
 * 4.1.52 (the owner: "high reputation means you're feared, they react to it"): what one of a player's own gang says when the player
 * talks to them, by the player's reputation in it (pure, tested): feared, respected, known, doubted, despised, unwanted. Gang-neutral (an
 * Aogiri member's, a Clown's); a different line by the seed.
 */
public final class GangTalk {
    static final String[] FEARED = {"Y-yes? Whatever you need.", "*steps aside* After you.", "I didn't see anything. Honest.",
        "You want my spot? It's yours.", "Heard what you did to those doves. Nobody's crossing you.", "*straightens up* Here. Ready."};
    static final String[] RESPECTED = {"Good work out there. People are talking.", "You're all right, you know that?",
        "Need a hand with anything? Just say.", "The others listen when you talk now."};
    static final String[] KNOWN = {"Hey. Quiet day.", "What's up?", "Keep your head down out there.", "Got a minute? Not that I need one."};
    static final String[] DOUBTED = {"Hm. You again.", "Some of us aren't sure about you.", "Watch yourself around here.",
        "Prove you're one of us, then we'll talk."};
    static final String[] DESPISED = {"What do you want?", "Don't push your luck.", "You've got some nerve, talking to me.",
        "Hit one of us again and see what happens."};
    static final String[] UNWANTED = {"Get out. Before it gets ugly.", "You're not one of us anymore. Leave.", "Walk away. Now."};

    private GangTalk() {
    }

    /** What one of the player's own gang says to them with reputation {@code reputation} (seed {@code seed}: which line). */
    public static String greet(int reputation, int seed) {
        String[] lines = lines(reputation);
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** The lines for reputation {@code reputation} (by its band: {@link GangStanding#band}). */
    public static String[] lines(int reputation) {
        return switch (GangStanding.band(reputation)) {
            case "Feared" -> FEARED;
            case "Respected" -> RESPECTED;
            case "Known" -> KNOWN;
            case "Doubted" -> DOUBTED;
            case "Despised" -> DESPISED;
            default -> UNWANTED;
        };
    }
}
