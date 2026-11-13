package com.hex.kagune.control.npc;

/**
 * 4.1.80: what the companions say through the courtship's new chapters (pure, tested), in their own voices: Ayato through the spar he
 * asked for (sore, cocky, and quieter about it than in Aogiri's), Touka through her homework (prickly, then pleased with herself), Roma at
 * each firework, Banjou at each thing planted, his boys at their supper. The chapters' asks and the moments that close them are
 * {@link Courtship}'s and the talk scripts'.
 */
public final class CourtVoice {
    private CourtVoice() {
    }

    /**
     * Ayato's spar at {@code moment}: "start", "won", "lost" (they went under a quarter), "draw" (its time out), "fled" (off its ground),
     * "died", "hurt" (they came to it under half their health). Picked by {@code seed}; never empty.
     */
    public static String spar(String moment, int seed) {
        String[] lines = switch (moment) {
            case "start" -> new String[] {"*He rolls his shoulders and doesn't smile.* Don't go easy on me because you like me. ...Three... two...",
                "*He cracks his neck.* If you hold back I'll know. And I'll be insulted. Three... two..."};
            case "won" -> new String[] {"*He's on one knee, laughing, which he never does.* ...Okay. Okay. You win. Don't you dare look smug.",
                "*He drops his hands and just looks at you, breathing hard.* ...Nobody's put me down like that. Not since... Tch. Come here."};
            case "lost" -> new String[] {"*He catches you before you hit the floor, and lets go too fast.* ...Get up. You're not done. Come back when "
                + "you can stand.", "That's it? ...Eat something. Then come back. I'm not finished with you."};
            case "draw" -> new String[] {"*He drops his hands.* We could do this all night. ...Don't make that face. Again, later. Properly.",
                "Time. You lasted. I want to WIN, though. Again, when you've got something."};
            case "fled" -> new String[] {"Where are you going? ...Tch. Come back when you're done running."};
            case "hurt" -> new String[] {"*He looks you up and down.* You're half dead already. I'm not fighting that. Eat. Then ask."};
            default -> new String[] {"...Hey. Come back. That wasn't the deal."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /**
     * Touka through her homework at {@code moment}: "begin" (the first question), "next" (one right, the next), "wrong" (a wrong answer:
     * done for today), "today" (asked again after one), "done" (three right). Picked by {@code seed}; never empty.
     */
    public static String homework(String moment, int seed) {
        String[] lines = switch (moment) {
            case "begin" -> new String[] {"*She slaps the textbook on the table between you and doesn't look up.* Okay. First one. Don't rush.",
                "*She opens her notebook to a page covered in crossings-out.* Right. Number one. ...If you laugh I'm leaving."};
            case "next" -> new String[] {"*She checks the back of the book, then checks it again.* ...Right. Huh. Next one.",
                "That's... right. *She writes it down, very neatly.* Okay. Next.", "*She narrows her eyes at you.* Lucky. Next."};
            case "wrong" -> new String[] {"*She checks the back of the book and shuts it.* ...No. That's not it either. Great. We're BOTH stupid. "
                + "Tomorrow.", "*She stares at you.* That's what I put. And I got it wrong. ...We're done for today. Tomorrow, and actually study."};
            case "today" -> new String[] {"Not again today. My head's full. Tomorrow.", "We already did this today. It went badly. Tomorrow."};
            default -> new String[] {"*She closes the book slowly.* ...Three. In a row."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** Roma at the {@code n}th of {@code need} fireworks (the last one: her delight). */
    public static String firework(int n, int need, int seed) {
        String[] lines = n >= need ? new String[] {"ENCORE! No! No encore! That was PERFECT! Don't you dare top it!"}
            : new String[] {"Oooh!", "*She claps with her whole body.* AGAIN!", "Ahhh! Red! My favourite! That's a lie, they're all my favourite!",
                "*She's standing on a crate to see better.* Higher!", "The crowd goes WILD! (The crowd is me.)"};
        return lines[Math.floorMod(seed + n, lines.length)];
    }

    /** Banjou at the {@code n}th of {@code need} things planted (the last one: done). */
    public static String planted(int n, int need, int seed) {
        String[] lines = n >= need ? new String[] {"*He kneels and very carefully pats the earth flat round it.* ...That's all of them. They're all in."}
            : new String[] {"*He holds his breath until it's in.* ...Is it okay? It looks okay.", "Not too deep. Not too shallow. ...I don't know "
                + "what I'm talking about.", "*He's counting under his breath.* That's another one. Wow.", "Careful. That one's- they're all that one."};
        return lines[Math.floorMod(seed + n, lines.length)];
    }

    /** One of Banjou's boys handed a bowl (the {@code n}th of {@code need}). */
    public static String served(int n, int need, int seed) {
        String[] lines = {"*pushes the gas mask up and digs in* ...Hot. It's HOT. Who made this?", "*takes the bowl in both hands* ...Banjou sent you? "
            + "...Thanks. Really.", "*sniffs it through the filter, then tears the mask off* Oh. Oh, that's good.",
            "*sits down right where he stands and eats without a word*"};
        return lines[Math.floorMod(seed + n, lines.length)] + (n >= need ? " §7(that's all three: go and see Banjou)" : "");
    }

    /** One of Banjou's boys handed a bowl a second time. */
    public static final String SERVED_TWICE = "*He waves you off, mouth full.* I had mine. Feed the others.";
}
