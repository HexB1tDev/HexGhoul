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

    /**
     * What they won't take now: a kiss ({@code kiss} true) or a hug, in {@code mood}; null if they'll take it. 2.2.4 (the owner: "If they're
     * angry and you ask for one, they'll give you an angry one"): a kiss is never turned down now, only given as the mood has it
     * ({@link MoodKiss}); a hug still is, sulking or jealous.
     */
    public static String refuse(String key, EtoMood mood, boolean kiss, int seed) {
        if (kiss) {
            return null;
        }
        return switch (mood) {
            case SULKY -> pick(seed, switch (key) {
                case "eto" -> new String[] {"No. I'm sulking. It's an art. Don't interrupt it.", "You can hug me when I've decided you can. Not before."};
                case "touka" -> new String[] {"Don't. I'm still mad.", "Touch me and lose a hand."};
                case "ken" -> new String[] {"...Not right now. I'm sorry. Give me a little while.", "I need a moment first. Please."};
                case "general" -> new String[] {"Not now. I'm in no temper for it. ...Give me a quarter of an hour.", "Stand off a moment. I'm still angry."};
                case "roma" -> new String[] {"Ah-ah. No touching the angry clown. ...Give me ten minutes. I'll be hilarious again.", "Not now. I'm sulking. It's a whole act. Very dramatic."};
                case "banjou" -> new String[] {"Not now. I'm angry. ...I think. Give me ten minutes to be sure.", "Don't. I'm trying to stay mad and you're making it hard."};
                case "ayato" -> new String[] {"Don't touch me. I'm mad. ...Still mad.", "Back off. Give me five minutes. Ten."};
                default -> new String[] {"Not now."};
            });
            case JEALOUS -> pick(seed, switch (key) {
                case "eto" -> new String[] {"Hug them instead. The other one.", "Oh, now you remember me? How sweet."};
                case "touka" -> new String[] {"Don't. I saw you.", "Who was that? ...Don't answer. Just don't touch me right now."};
                case "ken" -> new String[] {"...I saw you with someone else. I know it's stupid. I just need you to tell me it's me.",
                    "Can you tell me first? That it's me. Then yes."};
                case "general" -> new String[] {"I saw who you were with. Don't. Not until I've stopped seeing it.", "Go and stand with them, then."};
                case "roma" -> new String[] {"Who was THAT? ...No, really, I want a name. For my scrapbook.", "Go hug them, then. I'll be over here sharpening something. Kidding! ...Am I?"};
                case "banjou" -> new String[] {"I saw who you were with. I'm not- I'm fine. I just need you to tell me it's me.", "Go and stand with them, then. ...No. Don't. Stay."};
                case "ayato" -> new String[] {"Go hug whoever that was.", "I saw you. Don't. Not until you tell me who's yours."};
                default -> new String[] {"No."};
            });
            default -> null;
        };
    }

    /** What the jealous one says, made sure again ("It's only you."). */
    public static String reassured(String key, int seed) {
        return pick(seed, switch (key) {
            case "eto" -> new String[] {"...Say it again. Slower. ...Fine. I believe you. This time.", "Only me? Good. I was sharpening something. I'll put it away."};
            case "touka" -> new String[] {"...You'd better mean it. ...You mean it. Okay. Come here, idiot.", "Only me. Right. ...Right. Okay. I'm fine now."};
            case "ken" -> new String[] {"...Thank you. I needed to hear that more than I'd like to admit.", "Only me. ...I'm sorry I needed to ask."};
            case "general" -> new String[] {"...Only me. ...Good. I'm too old to share and too proud to ask twice.", "Say it again. ...Yes. All right. Come here."};
            case "roma" -> new String[] {"...Only me? Say it in front of the whole circus. ...Okay. Okay. I'm smiling. Shut up.", "Good. I'd hate to make someone disappear. Ta-da."};
            case "banjou" -> new String[] {"...Only me? Really? ...Okay. I believe you. I always believe you, that's my problem.", "Good. I'd hate to have to fight someone. I'd hate losing even more."};
            case "ayato" -> new String[] {"...Good. Say it again. ...Okay. Come here, idiot.", "Only me. Right. ...If they come near you again, they lose an arm."};
            case "noro" -> new String[] {"*A slow nod.*"};
            default -> new String[] {"Okay."};
        });
    }

    /** The answer that makes sure of them again, offered while they're jealous. */
    public static final String REASSURE = "It's only you. It's always been you.";

    static String pick(int seed, String[] lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
