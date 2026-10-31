package com.hex.kagune.control.npc;

import com.hex.kagune.control.family.ChildStage;

/** 1.3.11: what ghoul NPCs and children say when a player shows what they are ({@link Display}), pure. Eto's own are {@code EtoLines.saw}. */
public final class ReactionLines {
    private ReactionLines() {
    }

    /** A ghoul NPC that doesn't know them: afraid of a kakuja and worse, wary of a kagune. */
    public static String stranger(Display shown, int seed) {
        return switch (shown) {
            case DREAD -> pick(seed, "Wh— what ARE you?!", "That's not a ghoul. Run. RUN!", "No, no, no, no—");
            case KAKUJA -> pick(seed, "A kakuja?! Stay back!", "You've been eating your own... Get away from me!", "I'm not dying here. Not to that.");
            case KAGUNE -> pick(seed, "Easy. Easy. I don't want trouble.", "Put that away, ghoul.", "Hah. Big kagune. Doesn't scare me. ...Much.");
            default -> pick(seed, "Red eyes. Hungry, are we?", "One of us, huh.");
        };
    }

    /** A ghoul NPC that follows them: impressed. */
    public static String ally(Display shown, int seed) {
        return switch (shown) {
            case DREAD -> pick(seed, "...I'm glad I'm on your side.", "Remind me never to cross you.");
            case KAKUJA -> pick(seed, "That's a real kakuja. Incredible.", "Now we're talking. Let's hunt.");
            case KAGUNE -> pick(seed, "Nice kagune. Let's go.", "Ready when you are.");
            default -> pick(seed, "Eyes up. I'm with you.", "Hungry? Me too.");
        };
    }

    /** One of their own children, by how grown it is. */
    public static String child(ChildStage stage, String parent, Display shown, int seed) {
        boolean scary = shown.frightening();
        return switch (stage) {
            case BABY -> scary ? "*cries*" : "*stares at your eyes, fascinated*";
            case TODDLER -> scary ? pick(seed, parent + " scary! " + parent + " scary!", "*hides its face*") : pick(seed, "Ooh! Pretty eyes!", parent + " glowy!");
            case CHILD -> scary ? pick(seed, parent + "...? Is that still you?", "Whoa... you're HUGE.") : pick(seed, "Whoa! Teach me that!", "So cool!");
            case TEEN -> scary ? pick(seed, "...Okay, that's actually terrifying. Nice.", "Just... come back as you, okay?")
                : pick(seed, "Show-off.", "That's... actually kind of cool.");
        };
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
