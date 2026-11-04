package com.hex.kagune.control.npc;

/**
 * 2.2.3 (pure, tested; the owner: "MOOD of partner effects their actions, if they're mad for example they dont go up to you kissing you,
 * etc, but after a while they come to apologise ... make scenarios for everything and every mood"): what a companion's mood
 * ({@link EtoMood}) does to what passes between them and their partner, beyond what it says and the scenes it comes up with (those already
 * follow it): how readily they come for a kiss ({@link #kissing}); whether they take one, or a hug, when it's offered ({@link #refuse}); and
 * the words of the jealous made sure again ({@link #reassured}). Cross ones come to make up once it has passed (their scripts' {@code make_up}
 * and {@code jealous_over}).
 */
public final class MoodActs {
    private MoodActs() {
    }

    /** How readily they come for a kiss themselves in {@code mood}, against their usual (0: never). */
    public static double kissing(EtoMood mood) {
        return switch (mood) {
            case SULKY, JEALOUS, CLAIMED, WARY -> 0.0;
            case LONELY -> 2.2;
            case AFFECTIONATE -> 2.0;
            case CLINGY -> 1.8;
            case GIDDY -> 1.6;
            case BORED -> 1.2;
            case HAPPY -> 1.0;
            case SLEEPY -> 0.5;
            case BLOODTHIRSTY -> 0.4;
        };
    }

    /** What they won't take now: a kiss ({@code kiss} true) or a hug, in {@code mood}; null if they'll take it. */
    public static String refuse(String key, EtoMood mood, boolean kiss, int seed) {
        return switch (mood) {
            case SULKY -> pick(seed, switch (key) {
                case "eto" -> new String[] {"No. I'm sulking. It's an art. Don't interrupt it.", "You can kiss me when I've decided you can. Not before."};
                case "touka" -> new String[] {"Don't. I'm still mad.", "Touch me and lose a hand."};
                case "ken" -> new String[] {"...Not right now. I'm sorry. Give me a little while.", "I need a moment first. Please."};
                default -> new String[] {"Not now."};
            });
            case JEALOUS -> pick(seed, switch (key) {
                case "eto" -> new String[] {kiss ? "Kiss whoever you were with earlier. I'm sure they'd love it." : "Hug them instead. The other one.",
                    "Oh, now you remember me? How sweet."};
                case "touka" -> new String[] {kiss ? "Go kiss them, then. Since you like them so much." : "Don't. I saw you.",
                    "Who was that? ...Don't answer. Just don't touch me right now."};
                case "ken" -> new String[] {"...I saw you with someone else. I know it's stupid. I just need you to tell me it's me.",
                    "Can you tell me first? That it's me. Then yes."};
                default -> new String[] {"No."};
            });
            case BLOODTHIRSTY -> kiss ? pick(seed, switch (key) {
                case "eto" -> new String[] {"If I kiss you now I'll bite. Hard. Feed me first, darling.", "Not with my eye open like this. I'm starving."};
                case "touka" -> new String[] {"Back off. I'm hungry and you smell good. That's not a compliment.", "Feed me first. Then we'll see."};
                case "ken" -> new String[] {"Not now. I'm too hungry. I'd hurt you. Please.", "I can't. Not while I'm like this. Let's hunt first."};
                default -> new String[] {"I'm hungry."};
            }) : null;
            default -> null;
        };
    }

    /** What the jealous one says, made sure again ("It's only you."). */
    public static String reassured(String key, int seed) {
        return pick(seed, switch (key) {
            case "eto" -> new String[] {"...Say it again. Slower. ...Fine. I believe you. This time.", "Only me? Good. I was sharpening something. I'll put it away."};
            case "touka" -> new String[] {"...You'd better mean it. ...You mean it. Okay. Come here, idiot.", "Only me. Right. ...Right. Okay. I'm fine now."};
            case "ken" -> new String[] {"...Thank you. I needed to hear that more than I'd like to admit.", "Only me. ...I'm sorry I needed to ask."};
            default -> new String[] {"Okay."};
        });
    }

    /** The answer that makes sure of them again, offered while they're jealous. */
    public static final String REASSURE = "It's only you. It's always been you.";

    static String pick(int seed, String[] lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
