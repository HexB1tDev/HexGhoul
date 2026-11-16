package com.hex.kagune.control.family;

/**
 * 1.3.37: what a sleeper says, woken by a hand on the shoulder in the night (pure). The owner: "if you wake them up, like right click on
 * them, they just seem drowsy, just sleepy: what is it? what do you want?". A spouse half wakes, in their own way (Eto teasing even
 * asleep, Touka cross about the hour, Ken worried something's wrong); a child by its age (a baby sleeps on; a little one asks if it's
 * morning; a teen wants to know what you could possibly want). Then what they say back to "go back to sleep", a goodnight kiss, being
 * woken properly, a hand on their hair.
 */
public final class Drowsy {
    private Drowsy() {
    }

    private static String pick(String[] lines, int seed) {
        return lines[Math.floorMod(seed, lines.length)];
    }

    private static final String[] ETO = {
        "*squints at you over the covers* ...Mm? What is it... it's the middle of the night.",
        "*doesn't open her eyes* ...If this isn't important I'm writing you into a tragedy.",
        "Mmh... you're still up? ...What do you want?",
        "*rolls over, hair everywhere* ...Hm? What's wrong?",
    };
    private static final String[] TOUKA = {
        "*pulls the blanket over her head* ...Do you know what time it is?",
        "...What. *one eye open, glaring*",
        "Mm... what is it... I was asleep, idiot.",
        "*groans into the pillow* ...What do you want?",
    };
    private static final String[] KEN = {
        "*half-opens one eye* ...Hm? Is something wrong?",
        "...Mm. What is it? Did something happen?",
        "*sits up a little, blinking* ...You okay?",
        "*mumbles* ...what time is it...",
    };

    /** A spouse half woken in the night ({@code persona}: "eto", "touka", "ken"). */
    public static String spouse(String persona, int seed) {
        return switch (persona) {
            case "touka" -> pick(TOUKA, seed);
            case "ken" -> pick(KEN, seed);
            case "general" -> pick(new String[] {"*awake at once, a hand going to where a case would be* ...Report.", "...Mm? *already sitting up* Who's on the door?"}, seed);
            case "roma" -> pick(new String[] {"*awake at once, grinning in the dark* ...Is it a murder? Tell me it's a murder.", "...Mm? *one eye open* Who died?"}, seed);
            case "banjou" -> pick(new String[] {"*sits up too fast and bangs his head* ...Ow. What? Is it the Clowns?", "...Mm? *reaching for his mask* Who's there? I'm armed. I'm not armed."}, seed);
            case "ayato" -> pick(new String[] {"*one eye opens, red* ...What. Who's here?", "...Mm? *already half out of bed* Doves?",
                "*mumbles* ...if this isn't important I'm biting you."}, seed);
            case "noro" -> pick(new String[] {"*He was not asleep.*", "..."}, seed);
            default -> pick(ETO, seed);
        };
    }

    /** What they say to "Sorry. Go back to sleep.": they settle back. */
    public static String settle(String persona, int seed) {
        return switch (persona) {
            case "touka" -> pick(new String[] {"*already asleep again*", "...Idiot. *turns over*", "*a muffled grumble, then even breathing*"}, seed);
            case "ken" -> pick(new String[] {"...Okay. Goodnight. *settles back*", "*a sleepy smile* ...night.", "*nods, eyes already closing*"}, seed);
            case "general" -> pick(new String[] {"...As you were. *turns over, already asleep*", "*a grunt, and nothing more*"}, seed);
            case "roma" -> pick(new String[] {"*mumbles* ...wake me for the encore... *already gone*", "*turns over and steals the whole blanket*"}, seed);
            case "banjou" -> pick(new String[] {"...Okay. Night. *asleep before he finishes the word*", "*mumbles something about filters and turns over*"}, seed);
            case "ayato" -> pick(new String[] {"...Tch. *turns over, pulls the blanket with him*", "*already asleep again, scowling*"}, seed);
            case "noro" -> pick(new String[] {"*He doesn't move.*", "..."}, seed);
            default -> pick(new String[] {"*mumbles something about plot twists and burrows back in*", "...Mm. *already gone*",
                "*pats around for your hand, then gives up and sleeps*"}, seed);
        };
    }

    /** What they do with a goodnight kiss. */
    public static String kissed(String persona, int seed) {
        return switch (persona) {
            case "touka" -> pick(new String[] {"*her ears go red in the dark* ...G'night.", "...Mm. *pretends she's still asleep, smiling*"}, seed);
            case "ken" -> pick(new String[] {"*smiles without opening his eyes* ...Night.", "*squeezes your hand* ...Love you. Night."}, seed);
            case "general" -> pick(new String[] {"*an arm comes round you without his eyes opening* ...Stay.", "...Goodnight. *almost a smile*"}, seed);
            case "roma" -> pick(new String[] {"*pulls you in without opening her eyes* ...Stay. Ringmaster's orders.", "...Night. *a tiny sleepy giggle*"}, seed);
            case "banjou" -> pick(new String[] {"*an arm comes round you without his eyes opening* ...Stay. Please.", "...Night. *a sleepy, embarrassed smile*"}, seed);
            case "ayato" -> pick(new String[] {"*pulls you down next to him without opening his eyes* ...Stay.", "...Mm. Night. Don't tell anyone."}, seed);
            default -> pick(new String[] {"*smiles without opening her eyes* ...Again tomorrow.", "...Mm. That's a good ending. Night."}, seed);
        };
    }

    /** Woken properly ("Wake up. I need you."): up, on their feet, listening. */
    public static String roused(String persona, int seed) {
        return switch (persona) {
            case "touka" -> pick(new String[] {"*sits up, all at once awake* ...What happened?", "...Fine. I'm up. What is it?"}, seed);
            case "ken" -> pick(new String[] {"*already on his feet* What is it? What's wrong?", "...I'm up. Tell me."}, seed);
            case "general" -> pick(new String[] {"*on his feet, coat half on* Where? How many?", "...I'm up. Report."}, seed);
            case "roma" -> pick(new String[] {"*on her feet, tendrils half out* Who? Where? Ooh, is it a fight?", "...I'm up! Who do I kick?"}, seed);
            case "banjou" -> pick(new String[] {"*on his feet, mask half on, wing half out* Where? How many? ...How many is too many?", "...I'm up! I'm up. Who do I stand in front of?"}, seed);
            case "ayato" -> pick(new String[] {"*on his feet, wings half out* Who? Where?", "...I'm up. Who do I kill?"}, seed);
            case "noro" -> pick(new String[] {"*He is standing. You didn't see him get up.*"}, seed);
            default -> pick(new String[] {"*sits up, rubbing her eyes* ...Okay. I'm up. What's wrong?", "...Alright, alright. I'm awake. Tell me."}, seed);
        };
    }

    /** A child half woken, by its stage. */
    public static String child(ChildStage stage, int seed) {
        return switch (stage) {
            case BABY -> pick(new String[] {"*fast asleep, a tiny fist by its face*", "*sighs in its sleep and doesn't wake*"}, seed);
            case TODDLER -> pick(new String[] {"*rubs its eyes* ...is it morning?", "*stirs* ...mm... stay...", "*sleepy babbling, then quiet*"}, seed);
            case CHILD -> pick(new String[] {"Mmh... I was dreaming about... *yawns*", "*blinks up at you* ...is it time to get up?",
                "...can you leave the door open?"}, seed);
            default -> pick(new String[] {"*from under the blanket* ...It's like two in the morning. What.", "...Mm. What do you want?",
                "*doesn't move* ...I'm asleep. Go away."}, seed);
        };
    }

    /** A child told to go back to sleep, or stroked on the hair. */
    public static String childSettles(ChildStage stage, boolean stroked, int seed) {
        if (stage == ChildStage.BABY) {
            return stroked ? "*a tiny sigh, sleeping on*" : "*sleeping on*";
        }
        if (stroked) {
            return pick(new String[] {"*leans into your hand, already asleep again*", "*a sleepy little smile* ...night...",
                "*curls up a little tighter, breathing slow*"}, seed);
        }
        return stage.older() ? pick(new String[] {"*rolls over without a word*", "...night."}, seed)
            : pick(new String[] {"...okay... *eyes closing*", "*yawns and snuggles in*"}, seed);
    }
}
