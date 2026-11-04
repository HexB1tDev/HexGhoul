package com.hex.kagune.control.npc;

/**
 * 2.2.4 (pure, tested; the owner: "I want different kiss scenes with the same principle as the one already in place, but I want different
 * kiss scenes depending on the mood. If they're angry and you ask for one, they'll give you an angry one. And whatsoever, like, make up new
 * ones"): the kiss as the mood has it. The same as ever (they lean in and wait; a tap of shift is a quick one, held they blush, held longer
 * they pull them in: {@link EtoRules#kiss}), but each mood its own: how they wait ({@link #waits}), what they say, how they're drawn
 * ({@code MoodKissPose}), and what they do after ({@link #after}).
 */
public final class MoodKiss {
    private MoodKiss() {
    }

    /** The kinds of kiss. Sent by ordinal (append only). */
    public enum Style {
        /** Happy: as it always was. */
        SWEET,
        /** Sulking: grabbed by the collar, quick and hard, shoved away. It ends the sulk. */
        ANGRY,
        /** Jealous: pulled in at once, held too long. "Mine." It ends the jealousy. */
        POSSESSIVE,
        /** Giddy: up on their toes, bouncing, a burst of little ones, a spin. */
        GIDDY,
        /** Affectionate or clingy: hands on their face, slow, forehead to forehead after. */
        TENDER,
        /** Lonely: thrown at them, arms round them, won't let go. */
        DESPERATE,
        /** Sleepy: lazy, drooping, their head on the shoulder after. */
        SLEEPY,
        /** Starving: the eye open, a bite at the end. */
        HUNGRY,
        /** Bored: dodges the first try, laughing, then a real one, then off. */
        TEASING
    }

    /** The kiss for {@code mood}. */
    public static Style of(EtoMood mood) {
        return switch (mood) {
            case SULKY -> Style.ANGRY;
            case JEALOUS -> Style.POSSESSIVE;
            case GIDDY -> Style.GIDDY;
            case AFFECTIONATE, CLINGY -> Style.TENDER;
            case LONELY -> Style.DESPERATE;
            case SLEEPY -> Style.SLEEPY;
            case BLOODTHIRSTY -> Style.HUNGRY;
            case BORED -> Style.TEASING;
            default -> Style.SWEET;
        };
    }

    /** How long they lean in and wait (ticks): the angry won't wait long; the sleepy forget to stop. */
    public static int waits(Style style) {
        return switch (style) {
            case ANGRY -> 70;
            case SLEEPY -> 240;
            case DESPERATE -> 220;
            default -> EtoRules.KISS_WAIT;
        };
    }

    /** True if, teasing, the first tap is dodged (they lean back laughing, then lean in again). */
    public static boolean dodges(Style style) {
        return style == Style.TEASING;
    }

    /** What they do once it's over: the gesture after the kiss ({@code EtoGesture}), or null to keep the usual. */
    public static EtoGesture after(Style style) {
        return switch (style) {
            case ANGRY -> EtoGesture.ANGRY;
            case POSSESSIVE -> EtoGesture.CLING;
            case DESPERATE -> EtoGesture.HUG;
            case SLEEPY -> EtoGesture.LEAN;
            case HUNGRY -> EtoGesture.LICK;
            case TEASING -> EtoGesture.HOP;
            default -> null;
        };
    }

    /** How many hearts it's worth, against the usual. */
    public static double hearts(Style style) {
        return switch (style) {
            case ANGRY -> 0.4;
            case GIDDY, TENDER -> 1.6;
            case DESPERATE, POSSESSIVE -> 1.3;
            default -> 1.0;
        };
    }

    // ---------------------------------------------------------------- their words

    /** What the screen says while they wait ({@code name}: what they're called; {@code pos}: her or his). */
    public static String waiting(Style style, String name, String pos) {
        return switch (style) {
            case SWEET -> name + " leans in, eyes closed...";
            case ANGRY -> name + " grabs your collar, scowling. \"Well? Hurry up.\"";
            case POSSESSIVE -> name + " pulls you down by the collar and doesn't let go...";
            case GIDDY -> name + " bounces up on " + pos + " toes, grinning, eyes squeezed shut...";
            case TENDER -> name + " cups your face in both hands, slowly...";
            case DESPERATE -> name + " throws " + pos + " arms round you and holds on...";
            case SLEEPY -> name + " leans in, half asleep, swaying...";
            case HUNGRY -> name + " leans in, kakugan glowing, lips parted...";
            case TEASING -> name + " leans in with a sly little smile...";
        };
    }

    /** What {@code key} says after a quick one ({@code held}: held a while; {@code pulled}: pulled all the way in). */
    public static String kissed(String key, Style style, boolean held, boolean pulled, int seed) {
        String[] lines = switch (style) {
            case SWEET -> null;
            case ANGRY -> switch (key) {
                case "eto" -> new String[] {"*shoves you back* There. Happy? I'm still annoyed. ...Less annoyed.", "Hmph. That counts as an apology. Yours, not mine."};
                case "touka" -> new String[] {"*shoves you away* ...Don't get used to it. I'm still mad. A little.", "There. Now shut up. ...Okay, I'm not mad anymore. Idiot."};
                default -> new String[] {"*pulls back, flushed* ...Okay. I can't stay angry when you do that.", "That wasn't fair. ...I'm not angry anymore."};
            };
            case POSSESSIVE -> switch (key) {
                case "eto" -> new String[] {"Mine. Every page of you. Remember that.", "*doesn't let go* Tell whoever that was that you're taken."};
                case "touka" -> new String[] {"Mine. Got it? Mine.", "*keeps hold of your collar* ...Next time someone looks at you, I'm biting them."};
                default -> new String[] {"...I'm sorry. I needed to know you were still mine.", "*holds you a little too tight* Mine. ...Sorry. Mine."};
            };
            case GIDDY -> switch (key) {
                case "eto" -> new String[] {"Again! No— again! *laughs* I've lost count. Again.", "*spins on the spot* I'm writing this down in capital letters."};
                case "touka" -> new String[] {"*can't stop grinning* ...Shut up. I'm not smiling. I'm NOT.", "*hops on the spot, then catches herself* ...You saw nothing."};
                default -> new String[] {"*laughs, surprised at himself* I don't know what's wrong with me today. Again?", "*grinning like an idiot* ...Sorry. I'm just happy."};
            };
            case TENDER -> switch (key) {
                case "eto" -> new String[] {"*rests her forehead on yours* ...Stay like this. Just a minute. Don't say anything.", "Mm. That's the one I'll remember."};
                case "touka" -> new String[] {"*forehead to yours, eyes closed* ...Okay. That one was nice. Really nice.", "*softly* Don't move yet."};
                default -> new String[] {"*touches his forehead to yours* ...I love you. That's all.", "*keeps his hands on your face a moment longer* Thank you."};
            };
            case DESPERATE -> switch (key) {
                case "eto" -> new String[] {"Don't go away that long again. I read the same page forty times waiting.", "*won't let go* ...I missed you. Horribly. Don't."};
                case "touka" -> new String[] {"*face in your shoulder* Where were you? ...Don't answer. Just stay.", "Idiot. You were gone ages. ...Stay."};
                default -> new String[] {"*holds on* I kept thinking something happened to you. Please don't disappear like that.", "I missed you. So much. Stay?"};
            };
            case SLEEPY -> switch (key) {
                case "eto" -> new String[] {"*yawns against your mouth* ...Mm. Sorry. Carry me to bed.", "*head on your shoulder* Five more minutes. Of this."};
                case "touka" -> new String[] {"*leans her whole weight on you* ...I'm not asleep. I'm resting my eyes. On you.", "Mm. G'night. ...It's day? Don't care."};
                default -> new String[] {"*rests his head on your shoulder* ...Sorry. I'm so tired. This is nice.", "*half asleep* Stay. Just a little."};
            };
            case HUNGRY -> switch (key) {
                case "eto" -> new String[] {"*bites your lip, just a little* Sorry. Not sorry. You taste like dinner.", "*licks her lips, eye glowing* Mm. Feed me, and I'll do that again."};
                case "touka" -> new String[] {"*nips your lip* ...Sorry. I'm starving. That's not an excuse. It's a warning.", "*kakugan flickering* Don't look at me like that, I'm hungry."};
                default -> new String[] {"*pulls back fast, kakugan bright* I'm sorry. I almost bit. I need to eat.", "*a little breathless* ...Hunt with me later? Please."};
            };
            case TEASING -> switch (key) {
                case "eto" -> new String[] {"Caught me. Barely. *runs off giggling* Catch me again!", "*skips back out of reach* That's one. Want two? Earn it."};
                case "touka" -> new String[] {"Ha! Took you long enough. *hops back* Bet you can't do it again.", "*sticks her tongue out and backs away* Slowpoke."};
                default -> new String[] {"*laughs and steps back* You were faster than I thought.", "*grins* Okay, okay. You win. This time."};
            };
        };
        if (lines == null) {
            return null;
        }
        String line = lines[Math.floorMod(seed + (held ? 1 : 0) + (pulled ? 1 : 0), lines.length)];
        return line;
    }

    /** What {@code key} says dodging the first try (teasing). */
    public static String dodged(String key, int seed) {
        String[] lines = switch (key) {
            case "eto" -> new String[] {"*leans back at the last second* Too slow! Try again. I'll wait. Maybe.", "Ah-ah. Ask nicely. ...That was nicely. Again."};
            case "touka" -> new String[] {"*ducks away* Ha! Missed. Try again, slowpoke.", "*leans back, smirking* Nope. Again."};
            default -> new String[] {"*steps back, laughing* Sorry, sorry. I couldn't help it. Again?", "*dodges, grinning* One more try."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }
}
