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
    /**
     * 4.0.0: every speaker whose script may hold courtship moments and dates: the three, and Ayato (his script, talk/ayato.txt, is his
     * courtship's moments, his dates and a few scenes of his own: not yet every mood of every tier as theirs are).
     */
    public static final Set<String> ROMANCE_SCRIPTS = Set.of("eto", "touka", "ken", "ayato", "general"); // 4.1.5: and the General
    public static final String CHILD = "child";
    /** The wild ghouls who walk with a player (any of them: by its temper). */
    public static final String ALLY = "ally";

    public static boolean child(String speaker) {
        return CHILD.equals(speaker);
    }

    public static boolean ally(String speaker) {
        return ALLY.equals(speaker);
    }

    // ---------------------------------------------------------------- who a scene is for

    /** A companion's tiers (EtoVoice: a stranger she barely likes, liked, a close friend, dating, married). */
    public static final Set<String> COMPANION_TIERS = Set.of("wary", "friendly", "close", "partner", "wife");
    /** A child's (ChildStage). */
    public static final Set<String> CHILD_TIERS = Set.of("baby", "toddler", "child", "teen");
    /** An ally's: its temper (the ones that walk with a player). */
    public static final Set<String> ALLY_TIERS = Set.of("anteiku", "pack", "drifter", "scavenger");

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
    /** An ally's moods: fine, restless, hungry (at night), hurt, worn out (dawn). */
    public static final Set<String> ALLY_MOODS = Set.of("happy", "bored", "hungry", "hurt", "tired");

    // ---------------------------------------------------------------- moments ("when:", all must hold; "!" for not)

    /** The day, the weather, the player (a ghoul or human, a woman or a man, hurt, holding something). */
    private static final Set<String> SHARED_FLAGS = Set.of("night", "day", "dawn", "dusk", "deep", "morning", "rain", "thunder", "hurt", "ghoul", "human",
        "woman", "man", "home", "following");
    /** A companion's: their family together, how long, how long apart, what the player holds, her own place. */
    public static final Set<String> COMPANION_FLAGS = union(SHARED_FLAGS, Set.of("pregnant", "kids", "baby", "toddler", "kid", "teen", "away", "new", "long",
        "meat", "book", "flower", "ownplace", "mixed", "taught", "cowife",
        // 2.2.3: never set by the moment itself: a scene with it only comes up when asked for by name (making up after a sulk, after jealousy)
        "makeup",
        // 3.2.1: the player just brought a child of theirs home ({kid} is that one): "found" if it had gone out on its own (else an outing
        // the two of them came back from); "carrying" if the player carries a child of theirs (in arms, on shoulders, by the hand)
        "brought", "found", "carrying",
        // 3.3.8: and who that one is: its temper and its age (Homecoming.flags)
        "sunnykid", "shykid", "boldkid", "curiouskid", "mischiefkid", "gentlekid", "seriouskid", "babykid", "littlekid", "bigkid",
        // 4.1.15: who else the player is with (SceneServer.relations): dating or married to, married to, a parent with, each of the others;
        // "taken" if with any of them
        "taken"), relationFlags());
    /**
     * 4.1.15: the companions whose ties to the player every other one can see (with_, wed_, kidswith_ each), by persona key, and what their
     * flags call them ({@link #tieName}: the black rabbit's by his mask, as the companions' scripts never name him).
     */
    public static final Set<String> RELATED = Set.of("eto", "touka", "ken", "ayato", "general", "noro");

    /** 4.1.15: the word a tie to persona {@code key} goes by in the flags ({@code with_<word>}). */
    public static String tieName(String key) {
        return "ayato".equals(key) ? "blackrabbit" : key;
    }

    private static Set<String> relationFlags() {
        Set<String> out = new java.util.HashSet<>();
        for (String key : Set.of("eto", "touka", "ken", "ayato", "general", "noro")) {
            out.add("with_" + tieName(key));
            out.add("wed_" + tieName(key));
            out.add("kidswith_" + tieName(key));
        }
        return out;
    }
    /** A child's: a ghoul or not, a girl or a boy, which parent it talks to, the other parent a companion, brothers and sisters, carried. */
    public static final Set<String> CHILD_FLAGS = union(SHARED_FLAGS, Set.of("girl", "boy", "mother", "father", "siblings", "halfsib", "npcparent", "eto", "touka", "ken",
        "carried", "taught",
        // 3.2.1: the one it talks to just brought it home; "found" if it had gone out on its own (else they went out together)
        "back", "found"));
    /** An ally's: a boy or a girl, a friend of the player's (regard), not just someone who walks with them. */
    public static final Set<String> ALLY_FLAGS = union(SHARED_FLAGS, Set.of("boy", "girl", "friend"));

    // ---------------------------------------------------------------- what an answer does

    /** A companion's gestures (EtoGesture, lower case) an answer may set her doing. */
    public static final Set<String> GESTURES = Set.of("wave", "stretch", "twirl", "hop", "peace", "lean", "giggle", "blush", "read", "write", "sweep", "hair",
        "hum", "look", "dance", "pout", "yawn", "lick", "pet", "laugh", "cross", "tap", "huff", "sip", "wipe", "kicks", "scold", "shy", "chin", "crack", "bow",
        "angry", "ring");
    /** A companion's deeds: walking up for a hug, an arm, a hand, a kiss; following or staying; on to her topics or her sweet page. */
    public static final Set<String> COMPANION_EFFECTS = Set.of("hug", "cling", "hand", "kiss", "peck", "steal", "follow", "stay", "talk", "sweet", "forgive",
        "night", "close", "sulk", "giddy", "hearts",
        // 3.3.7: talked it out: the sulk, the grudge, the jealousy and any make-up owed all over, whatever else the answer does
        "mend");
    /** Of those, the ones only for a couple (a scene that does them is for partners and spouses alone). */
    public static final Set<String> TOGETHER_ONLY = Set.of("cling", "hand", "kiss", "peck", "steal", "sweet", "night");
    /** A child's moments (ChildMoment, lower case) and deeds: picked up, carried on shoulders, by the hand, following, staying, to bed. */
    public static final Set<String> CHILD_EFFECTS = Set.of("hug", "pat", "high_five", "tickle", "peekaboo", "toss", "spin", "arms_up", "wave", "clap",
        "nuzzle", "boop", "fist_bump", "patty_cake", "dance", // 4.1.15: the new moments (ChildMoment)
        "flower", "up", "down", "shoulders", "hand", "follow", "stay", "bed", "home", "teach", "sad", "proud", "close", "play");
    /** An ally's deeds: following, waiting, done talking. */
    public static final Set<String> ALLY_EFFECTS = Set.of("follow", "stay", "close");
    /** What may be given to the player ({@code gift:<item>}): small things, a ghoul's dinner for a ghoul. */
    public static final Set<String> GIFTS = Set.of("poppy", "dandelion", "cornflower", "blue_orchid", "allium", "oxeye_daisy", "lily_of_the_valley",
        "azure_bluet", "red_tulip", "pink_tulip", "sunflower", "cookie", "bread", "apple", "sweet_berries", "glow_berries", "honey_bottle", "pumpkin_pie",
        "cake", "book", "paper", "feather", "amethyst_shard", "cocoa_beans", "string", "flint", "cobblestone", "stick", "egg", "bone", "ghoul_meat",
        "writable_book", "candle", "lantern", "music_disc_cat", "name_tag", "rabbit_foot", "snowball", "slime_ball", "clay_ball", "pointed_dripstone",
        "glow_ink_sac", "nautilus_shell", "scute", "prismarine_shard", "rose_bush", "peony", "lilac");

    // ---------------------------------------------------------------- what may be put in a line

    /** Names a companion's lines may hold: the player, a child of theirs, the children, who she belongs to. */
    public static final Set<String> COMPANION_NAMES = Set.of("name", "kid", "kids", "partner", "she", "her", "self", "cowife",
        "kidwith"); // 4.1.15: a child the player has with another of them (SceneServer.relations)
    /** A child's: what it calls the one it talks to, the other parent (what it calls them, their name), its own name, a brother or sister. */
    public static final Set<String> CHILD_NAMES = Set.of("call", "other", "othername", "me", "sib", "name", "halfsib", "halfmom");
    /** An ally's: the player's name, its own. */
    public static final Set<String> ALLY_NAMES = Set.of("name", "me");
    /** A name that needs a moment to hold (else there is no one to name). */
    public static String needs(String name) {
        return switch (name) {
            case "kid", "kids" -> "kids";
            case "sib" -> "siblings";
            case "partner" -> "claimed";
            case "cowife" -> "cowife"; // 3.1.9: another of the player's partners
            case "halfsib", "halfmom" -> "halfsib"; // 3.1.9: a brother or sister by another mother or father, and that one's
            default -> null;
        };
    }

    @SafeVarargs
    private static Set<String> union(Set<String> a, Set<String>... more) {
        java.util.HashSet<String> out = new java.util.HashSet<>(a);
        for (Set<String> b : more) {
            out.addAll(b);
        }
        return Set.copyOf(out);
    }

    /** The tiers, moods, flags, effects and names of {@code speaker}'s family. */
    public static Set<String> tiersOf(String speaker) {
        return child(speaker) ? CHILD_TIERS : ally(speaker) ? ALLY_TIERS : COMPANION_TIERS;
    }

    public static Set<String> moodsOf(String speaker) {
        return child(speaker) ? CHILD_MOODS : ally(speaker) ? ALLY_MOODS : COMPANION_MOODS;
    }

    public static Set<String> flagsOf(String speaker) {
        return child(speaker) ? CHILD_FLAGS : ally(speaker) ? ALLY_FLAGS : COMPANION_FLAGS;
    }

    public static Set<String> namesOf(String speaker) {
        return child(speaker) ? CHILD_NAMES : ally(speaker) ? ALLY_NAMES : COMPANION_NAMES;
    }

    /** True if {@code effect} (a token after an answer's "|") is one {@code speaker}'s family may do. */
    public static boolean effect(String speaker, String effect) {
        if (effect.startsWith("gift:")) {
            return GIFTS.contains(effect.substring(5));
        }
        if (ally(speaker)) {
            return ALLY_EFFECTS.contains(effect);
        }
        return child(speaker) ? CHILD_EFFECTS.contains(effect) : COMPANION_EFFECTS.contains(effect) || GESTURES.contains(effect);
    }
}
