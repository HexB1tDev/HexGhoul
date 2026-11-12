package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 4.1.15 (pure, tested; the owner: "improve the making love scene"): the night together's captions, chosen for whoever it is with. Who
 * they are (each companion their own way into the dark), the weather on the window, their mood (a sulk that doesn't survive the night, a
 * jealous one holding on), the first night or a long-married one, the children asleep down the hall; and the right pronoun (1.3.10's
 * captions said "her" whoever it was). Never more than the dark shows: it fades out on a candle and comes back on the morning.
 *
 * <p>They travel with the morning words in the cutscene's one string ({@link #pack}): each caption ended by {@link #CAPTION}, the morning
 * after {@link #WORDS}.
 */
public final class NightWords {
    /** Ends each caption in the packed string. */
    public static final char CAPTION = '\u0001';
    /** Ends the captions; what follows is said in the morning. */
    public static final char WORDS = '\u0002';
    /** The most captions a night shows. */
    public static final int MOST = 5;

    private NightWords() {
    }

    /**
     * The night's captions with {@code key} (a persona's key; anyone else: the plain ones), {@code female} for the pronoun, their {@code mood}
     * (EtoMood's word), the moment's flags (SceneServer's: rain, thunder, kids, pregnant, new, long, home...), {@code first} if it is their
     * first night, {@code married} if they are wed; {@code seed} picks among the ways of saying each.
     */
    public static List<String> captions(String key, boolean female, String mood, Set<String> flags, boolean first, boolean married, int seed) {
        String she = female ? "She" : "He";
        String her = female ? "her" : "his";
        String herself = female ? "herself" : "himself";
        List<String> out = new ArrayList<>();
        out.add(pick(opening(key, she, her, herself), seed));
        if (flags.contains("thunder")) {
            out.add(pick(new String[] {"Thunder rolls somewhere far off. Neither of you notices.", "Lightning, and for a second the room is white. Then dark again."},
                seed >> 3));
        } else if (flags.contains("rain")) {
            out.add(pick(new String[] {"Rain taps on the window, soft and steady.", "The rain picks up. The room feels smaller. Warmer."}, seed >> 3));
        } else {
            out.add(pick(new String[] {"The candle goes out.", "The last light in the room goes out.", "Somewhere outside, the city goes quiet."}, seed >> 3));
        }
        out.add(heart(mood, she, her, first, married, flags, seed >> 6));
        if (flags.contains("kids") && !flags.contains("away")) {
            out.add(pick(new String[] {"Down the hall, a small voice mumbles in its sleep. You both freeze. Then laugh without a sound.",
                "A door creaks down the hall. Footsteps, then nothing. Back to bed, little one."}, seed >> 9));
        } else if (flags.contains("pregnant")) {
            out.add(pick(new String[] {"A hand rests on the curve of a belly, and stays there.", "Three heartbeats in the room now. You count them twice."}, seed >> 9));
        }
        out.add(pick(new String[] {"...Later.", "...Much later.", "...The candle's long gone cold."}, seed >> 12));
        return out.size() > MOST ? out.subList(0, MOST) : out;
    }

    /** Each one's way into the dark. */
    static String[] opening(String key, String she, String her, String herself) {
        return switch (key == null ? "" : key) {
            case "eto" -> new String[] {"She closes her notebook. For once, she isn't writing anything down.",
                "\"No narration tonight,\" she whispers, and blows out the candle herself.",
                "She laughs at something only she heard. Then, very quietly, she stops laughing."};
            case "touka" -> new String[] {"She grumbles that the coffee went cold. She doesn't let go of your hand.",
                "She turns off the light herself. \"Don't look at me like that, idiot.\"",
                "She kicks off her shoes, steals the blanket, and holds the corner open for you."};
            case "ken" -> new String[] {"He sets his book face down, the page unmarked. He doesn't care where he stops.",
                "He hesitates, as if asking. You answer. He smiles.",
                "He pours two cups of coffee, and forgets both of them."};
            case "ayato" -> new String[] {"He shrugs off his jacket and pretends he isn't nervous. He's very bad at pretending.",
                "\"Don't get the wrong idea,\" he mutters, and pulls you closer.",
                "His wings fold away. For once, nothing in him is ready for a fight."};
            case "general" -> new String[] {"He folds his uniform with military precision. Then forgets to finish.",
                "He sets his cap on the dresser and, for the first time all day, stops giving orders.",
                "He clears his throat as if to say something formal. He doesn't."};
            case "roma" -> new String[] {"She takes her mask off last and sets it face up on the pillow, grinning at the ceiling.",
                "She does a little bow before getting into bed. Nobody claps. She does it anyway.",
                "For once she doesn't say anything funny. She just reaches for you."};
            case "banjou" -> new String[] {"He takes the gas mask off last, sets it on the dresser, and looks at it a long moment before he looks at you.", "He checks the door twice, then the window, then the door again. Then he laughs at himself.", "He's very big and very careful, and he asks twice if you're sure."};
            case "noro" -> new String[] {"The mask comes off last. He sets it face down, carefully.",
                "He says nothing at all. He doesn't need to."};
            default -> new String[] {she + " takes your hand and leads the way.", she + " laughs softly and closes the door behind " + herself + ".",
                she + " lets " + herself + " be held."};
        };
    }

    /** The night's heart: its mood, or what kind of night it is. */
    static String heart(String mood, String she, String her, boolean first, boolean married, Set<String> flags, int seed) {
        String lower = she.toLowerCase(java.util.Locale.ROOT);
        String[] lines = switch (mood == null ? "" : mood) {
            case "sulky" -> new String[] {"Whatever the argument was, it doesn't survive the night.", "Somewhere in the dark, the sulk gives up."};
            case "jealous" -> new String[] {she + " holds on like you might leave. You don't.", "\"Mine,\" " + lower + " breathes. You don't argue."};
            case "lonely" -> new String[] {she + " doesn't let go. Not once.", "All the empty nights before this one stop counting."};
            case "sleepy" -> new String[] {she + " falls asleep first, mid-sentence, " + her + " hand still in yours."};
            case "giddy" -> new String[] {she + " can't stop laughing. Then can't stop smiling.", "Giggling, shushing each other, giggling again."};
            case "clingy", "affectionate" -> new String[] {she + " traces slow circles on the back of your hand.", "Forehead to forehead, the dark goes very quiet."};
            default -> null;
        };
        if (lines != null) {
            return pick(lines, seed);
        }
        if (first) {
            return pick(new String[] {"The first time. Neither of you is in any hurry.", "Your hands are shaking. So are " + ("her".equals(her) ? "hers" : "his") + ". That's all right."}, seed);
        }
        if (married && flags.contains("long")) {
            return pick(new String[] {"It's familiar now. Somehow, it's still new.", "Years of nights like this one, and you still lose count of the time."}, seed);
        }
        return pick(new String[] {she + " finds your hand in the dark.", "Two heartbeats, slowly falling into step.",
            "The world shrinks to the size of one room."}, seed);
    }

    /** What {@code key} does on waking (an EtoGesture's name), by the mood they went to sleep in. */
    public static String waking(String mood) {
        return switch (mood == null ? "" : mood) {
            case "sleepy" -> "YAWN";
            case "giddy" -> "GIGGLE";
            case "clingy", "lonely", "jealous", "affectionate" -> "LEAN";
            default -> "STRETCH";
        };
    }

    /** The captions and the morning's words in one string ({@link #CAPTION} after each caption, {@link #WORDS} before the morning). */
    public static String pack(List<String> captions, String morning) {
        StringBuilder out = new StringBuilder();
        for (String caption : captions) {
            out.append(caption.replace(CAPTION, ' ').replace(WORDS, ' ')).append(CAPTION);
        }
        return out.append(WORDS).append(morning == null ? "" : morning).toString();
    }

    /** The captions of a packed string (none in an old one: only morning words). */
    public static List<String> captionsOf(String packed) {
        List<String> out = new ArrayList<>();
        int end = packed == null ? -1 : packed.indexOf(WORDS);
        if (end < 0) {
            return out;
        }
        int from = 0;
        for (int i = 0; i < end; i++) {
            if (packed.charAt(i) == CAPTION) {
                out.add(packed.substring(from, i));
                from = i + 1;
            }
        }
        return out;
    }

    /** The morning's words of a packed string (all of an old one). */
    public static String wordsOf(String packed) {
        if (packed == null) {
            return "";
        }
        int at = packed.indexOf(WORDS);
        return at < 0 ? packed : packed.substring(at + 1);
    }

    private static String pick(String[] lines, int seed) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
