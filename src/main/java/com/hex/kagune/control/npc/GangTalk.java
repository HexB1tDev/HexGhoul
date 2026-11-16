package com.hex.kagune.control.npc;

/**
 * 4.1.52 (the owner: "high reputation means you're feared, they react to it"): what one of a player's own gang says when the player
 * talks to them, by the player's reputation in it (pure, tested): feared, respected, known, doubted, despised, unwanted. Gang-neutral (an
 * Aogiri member's, a Clown's); a different line by the seed.
 */
public final class GangTalk {
    // 4.2.8 (the owner: "So develop every single NPC even further, add more dialogue"): more of each band's.
    static final String[] FEARED = {"Y-yes? Whatever you need.", "*steps aside* After you.", "I didn't see anything. Honest.",
        "You want my spot? It's yours.", "Heard what you did to those doves. Nobody's crossing you.", "*straightens up* Here. Ready.",
        "The new ones ask about you. I tell them to keep their heads down.", "*goes quiet the moment you walk up* ...Sorry. Go on.",
        "Whatever you want done, consider it done. Twice.", "Someone said your name in the dark last night. Everyone stopped talking."};
    static final String[] RESPECTED = {"Good work out there. People are talking.", "You're all right, you know that?",
        "Need a hand with anything? Just say.", "The others listen when you talk now.", "Saved you a spot by the fire.",
        "If you're heading out, I'll come. Someone should watch your back.", "The boss mentioned you. Good things. Mostly.",
        "I told my brother about you. He wants to meet you. I said he'd have to earn it."};
    static final String[] KNOWN = {"Hey. Quiet day.", "What's up?", "Keep your head down out there.", "Got a minute? Not that I need one.",
        "Doves were sniffing around the east side. Just so you know.", "You eat yet? There's a little left.",
        "Same old. You?", "Heard the other gang's been moving at night. Could be nothing.", "*nods* Hey."};
    static final String[] DOUBTED = {"Hm. You again.", "Some of us aren't sure about you.", "Watch yourself around here.",
        "Prove you're one of us, then we'll talk.", "People talk. Not all of it's good.", "*looks you up and down* We'll see.",
        "Do something for us. Then maybe I'll remember your name."};
    static final String[] DESPISED = {"What do you want?", "Don't push your luck.", "You've got some nerve, talking to me.",
        "Hit one of us again and see what happens.", "Keep walking.", "*spits on the ground by your feet*",
        "Nobody here wants you. Not one of us."};
    static final String[] UNWANTED = {"Get out. Before it gets ugly.", "You're not one of us anymore. Leave.", "Walk away. Now.",
        "*several of them stand up at once*", "Last warning."};

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
