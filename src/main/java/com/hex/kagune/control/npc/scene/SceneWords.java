package com.hex.kagune.control.npc.scene;

import java.util.Set;

/**
 * 1.3.29: every word a scene script ({@code data/hexkagune/talk/*.txt}) may use (pure): who a scene is for, the moods and the moments
 * it fits, what an answer does, and the names it may put in. A word not here is a mistake in the script ({@link SceneBook} reports it,
 * and the tests refuse it), never something the game guesses at.
 *
 * <p>Two families of speaker: a companion (Eto, Touka, Ken: by who the player is to them, their mood, the hour, the weather, the
 * family the two of them have) and a child (by its age, its temperament, its mood, and who its parents are).
 */
public final class SceneWords {
    private SceneWords() {
    }

    /** The speakers with a script, and which family each is. */
    public static final Set<String> COMPANIONS = Set.of("eto", "touka", "ken");
    public static final String CHILD = "child";

    public static boolean child(String speaker) {
        return CHILD.equals(speaker);
    }

    // ---------------------------------------------------------------- who a scene is for

    /** A companion's tiers (EtoVoice: a stranger she barely likes, liked, a close friend, dating, married). */
    public static final Set<String> COMPANION_TIERS = Set.of("wary", "friendly", "close", "partner", "wife");
    /** A child's (ChildStage). */
    public static final Set<String> CHILD_TIERS = Set.of("baby", "toddler", "child", "teen");

    /** Short words for several tiers at once. */
    public static Set<String> tiers(String word) {
        return switch (word) {
            case "together" -> Set.of("partner", "wife");
            case "friend", "friends" -> Set.of("friendly", "close");
            case "anyone" -> Set.of("wary", "friendly", "close", "partner", "wife");
            case "little" -> Set.of("baby", "toddler");
            case "big" -> Set.of("child", "teen");
            case "walking" -> Set.of("toddler", "child", "teen");
            case "talking" -> Set.of("toddler", "child", "teen");
            default -> Set.of(word);
        };
    }

    // ---------------------------------------------------------------- moods

    /** A companion's moods (EtoMood's words). */
    public static final Set<String> COMPANION_MOODS = Set.of("happy", "giddy", "sulky", "jealous", "lonely", "clingy", "sleepy", "hungry",
        "affectionate", "bored", "wary");
    /** A child's (ChildTemper.Mood). */
    public static final Set<String> CHILD_MOODS = Set.of("happy", "sleepy", "scared", "missed", "bored", "proud", "sad", "hurt");
    /** A child's temperament (ChildTemper). */
    public static final Set<String> TEMPERS = Set.of("sunny", "shy", "bold", "curious", "mischief", "gentle", "serious");

    // ---------------------------------------------------------------- moments ("when:", all must hold; "!" for not)

    /** The day, the weather, the player (a ghoul or human, a woman or a man, hurt, holding something). */
    private static final Set<String> SHARED_FLAGS = Set.of("night", "day", "dawn", "dusk", "deep", "morning", "rain", "thunder", "hurt", "ghoul", "human",
        "woman", "man", "home", "following");
    /** A companion's: their family together, how long, how long apart, what the player holds, her own place. */
    public static final Set<String> COMPANION_FLAGS = union(SHARED_FLAGS, Set.of("pregnant", "kids", "baby", "toddler", "kid", "teen", "away", "new", "long",
        "meat", "book", "flower", "ownplace", "mixed", "taught"));
    /** A child's: a ghoul or not, a girl or a boy, which parent it talks to, the other parent a companion, brothers and sisters, carried. */
    public static final Set<String> CHILD_FLAGS = union(SHARED_FLAGS, Set.of("girl", "boy", "mother", "father", "siblings", "npcparent", "eto", "touka", "ken",
        "carried", "taught"));

    // ---------------------------------------------------------------- what an answer does

    /** A companion's gestures (EtoGesture, lower case) an answer may set her doing. */
    public static final Set<String> GESTURES = Set.of("wave", "stretch", "twirl", "hop", "peace", "lean", "giggle", "blush", "read", "write", "sweep", "hair",
        "hum", "look", "dance", "pout", "yawn", "lick", "pet", "laugh", "cross", "tap", "huff", "sip", "wipe", "kicks", "scold", "shy", "chin", "crack", "bow",
        "angry", "ring");
    /** A companion's deeds: walking up for a hug, an arm, a hand, a kiss; following or staying; on to her topics or her sweet page. */
    public static final Set<String> COMPANION_EFFECTS = Set.of("hug", "cling", "hand", "kiss", "peck", "follow", "stay", "talk", "sweet", "forgive", "night",
        "close", "sulk", "giddy", "hearts");
    /** Of those, the ones only for a couple (a scene that does them is for partners and spouses alone). */
    public static final Set<String> TOGETHER_ONLY = Set.of("cling", "hand", "kiss", "peck", "sweet", "night");
    /** A child's moments (ChildMoment, lower case) and deeds: picked up, carried on shoulders, by the hand, following, staying, to bed. */
    public static final Set<String> CHILD_EFFECTS = Set.of("hug", "pat", "high_five", "tickle", "peekaboo", "toss", "spin", "arms_up", "wave", "clap",
        "flower", "up", "down", "shoulders", "hand", "follow", "stay", "bed", "home", "teach", "sad", "proud", "close", "play");
    /** What may be given to the player ({@code gift:<item>}): small things, a ghoul's dinner for a ghoul. */
    public static final Set<String> GIFTS = Set.of("poppy", "dandelion", "cornflower", "blue_orchid", "allium", "oxeye_daisy", "lily_of_the_valley",
        "azure_bluet", "red_tulip", "pink_tulip", "sunflower", "cookie", "bread", "apple", "sweet_berries", "glow_berries", "honey_bottle", "pumpkin_pie",
        "cake", "book", "paper", "feather", "amethyst_shard", "cocoa_beans", "string", "flint", "cobblestone", "stick", "egg", "bone", "ghoul_meat",
        "writable_book", "candle", "lantern", "music_disc_cat", "name_tag", "rabbit_foot", "snowball", "slime_ball", "clay_ball", "pointed_dripstone",
        "glow_ink_sac", "nautilus_shell", "scute", "prismarine_shard", "rose_bush", "peony", "lilac");

    // ---------------------------------------------------------------- what may be put in a line

    /** Names a companion's lines may hold: the player, a child of theirs, the children, who she belongs to. */
    public static final Set<String> COMPANION_NAMES = Set.of("name", "kid", "kids", "partner", "she", "her", "self");
    /** A child's: what it calls the one it talks to, the other parent (what it calls them, their name), its own name, a brother or sister. */
    public static final Set<String> CHILD_NAMES = Set.of("call", "other", "othername", "me", "sib", "name");
    /** A name that needs a moment to hold (else there is no one to name). */
    public static String needs(String name) {
        return switch (name) {
            case "kid", "kids" -> "kids";
            case "sib" -> "siblings";
            case "partner" -> "claimed";
            default -> null;
        };
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        java.util.HashSet<String> out = new java.util.HashSet<>(a);
        out.addAll(b);
        return Set.copyOf(out);
    }

    /** The tiers, moods, flags, effects and names of {@code speaker}'s family. */
    public static Set<String> tiersOf(String speaker) {
        return child(speaker) ? CHILD_TIERS : COMPANION_TIERS;
    }

    public static Set<String> moodsOf(String speaker) {
        return child(speaker) ? CHILD_MOODS : COMPANION_MOODS;
    }

    public static Set<String> flagsOf(String speaker) {
        return child(speaker) ? CHILD_FLAGS : COMPANION_FLAGS;
    }

    public static Set<String> namesOf(String speaker) {
        return child(speaker) ? CHILD_NAMES : COMPANION_NAMES;
    }

    /** True if {@code effect} (a token after an answer's "|") is one {@code speaker}'s family may do. */
    public static boolean effect(String speaker, String effect) {
        if (effect.startsWith("gift:")) {
            return GIFTS.contains(effect.substring(5));
        }
        return child(speaker) ? CHILD_EFFECTS.contains(effect) : COMPANION_EFFECTS.contains(effect) || GESTURES.contains(effect);
    }
}
