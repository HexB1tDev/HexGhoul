package com.hex.kagune.control.npc;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 4.1.84, romance, step 3 (the owner's to-do, of the plan shown, "whatever you think is best": "gifts each companion loves or hates"): what
 * each companion makes of a present (pure, tested): anything in hand that has no gift of its own (their errand's item, their flower, Ghoul
 * Meat, a letter) may be given, and each of them loves some things, hates others, and takes the rest politely. Their own lines, by name.
 *
 * <p>The ghouls hate human food (it tastes of nothing but rot to them, and Touka has had to swallow it at school); the General, the one
 * human, hates what ghouls eat and wear. Each loves a few things of their own life: Eto her ink and her owl, Touka the café's coffee and
 * her rabbit, Ken books and coffee (and hates centipedes and anything like Yamori's pliers), Ayato what's sharp and black (and flowers, all
 * but the wither rose, insult him), Noro what a ghoul eats, the General order and the CCG's own, Roma colour and noise (and hates
 * homework), Banjou anything that grows, and the suppers of the cookbook he feeds his boys.
 */
public final class Tastes {
    private Tastes() {
    }

    /** What a present is to them. */
    public enum Taste {
        /** Taken with delight: {@link #LOVED_REGARD}. */
        LOVED,
        /** Taken politely: {@link #LIKED_REGARD}. */
        LIKED,
        /** Pushed back, offended: {@link #HATED_REGARD}; never taken. */
        HATED
    }

    /** Regard a present brings (once a day, as every gift: EtoRules.gifted). */
    public static final int LOVED_REGARD = EtoRules.GIFT;
    public static final int LIKED_REGARD = 2;
    public static final int HATED_REGARD = -4;

    /** Human food, as a ghoul's tongue finds it: every food of Minecraft's a human eats. */
    public static final Set<String> HUMAN_FOOD = Set.of("minecraft:bread", "minecraft:cake", "minecraft:cookie", "minecraft:pumpkin_pie",
        "minecraft:apple", "minecraft:golden_apple", "minecraft:baked_potato", "minecraft:potato", "minecraft:carrot", "minecraft:golden_carrot",
        "minecraft:beetroot", "minecraft:melon_slice", "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:cooked_beef",
        "minecraft:cooked_porkchop", "minecraft:cooked_chicken", "minecraft:cooked_mutton", "minecraft:cooked_rabbit", "minecraft:cooked_cod",
        "minecraft:cooked_salmon", "minecraft:mushroom_stew", "minecraft:rabbit_stew", "minecraft:beetroot_soup", "minecraft:suspicious_stew",
        "minecraft:honey_bottle", "minecraft:dried_kelp", "minecraft:beef", "minecraft:porkchop", "minecraft:chicken", "minecraft:mutton",
        "minecraft:rabbit", "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish");

    /** What each loves (an id, or a prefix ending in '_' or ':' for a whole kind: "minecraft:music_disc_"). */
    public static final Map<String, List<String>> LOVES = Map.of(
        "eto", List.of("hexkagune:anteiku_blend", "minecraft:writable_book", "minecraft:ink_sac", "minecraft:glow_ink_sac", "minecraft:feather",
            "hexkagune:mask_eto", "hexkagune:owls_omelette", "minecraft:candle"),
        "touka", List.of("hexkagune:anteiku_blend", "hexkagune:blood_latte", "hexkagune:mask_touka", "minecraft:rabbit_hide", "hexkagune:velvet_cake",
            "minecraft:writable_book"),
        "ken", List.of("hexkagune:anteiku_blend", "hexkagune:mask_eyepatch", "minecraft:writable_book", "minecraft:lantern", "hexkagune:crimson_tea"),
        "ayato", List.of("minecraft:black_dye", "hexkagune:mask_ayato", "minecraft:iron_sword", "minecraft:diamond_sword", "minecraft:netherite_sword",
            "hexkagune:rc_tonic", "hexkagune:kakuja_shot"),
        "noro", List.of("hexkagune:cooked_ghoul_meat", "hexkagune:kakuhou_tartare", "hexkagune:mask_noro", "minecraft:rotten_flesh"),
        "general", List.of("minecraft:writable_book", "minecraft:clock", "minecraft:cooked_beef", "minecraft:bread", "minecraft:shield",
            "hexkagune:quinque_"),
        "roma", List.of("minecraft:firework_star", "minecraft:slime_ball", "minecraft:jack_o_lantern", "minecraft:carved_pumpkin",
            "minecraft:purple_dye", "minecraft:magenta_dye", "minecraft:pink_dye", "hexkagune:mask_clown", "hexkagune:mask_roma", "minecraft:music_disc_"),
        "banjou", List.of("minecraft:bone_meal", "minecraft:flower_pot", "hexkagune:mask_gas", "minecraft:oak_sapling", "minecraft:birch_sapling",
            "minecraft:spruce_sapling", "minecraft:jungle_sapling", "minecraft:acacia_sapling", "minecraft:dark_oak_sapling",
            "minecraft:cherry_sapling", "minecraft:mangrove_propagule", "minecraft:azalea", "minecraft:flowering_azalea"));

    /** What each hates, besides human food for the ghouls (as {@link #LOVES}). */
    public static final Map<String, List<String>> HATES = Map.of(
        "eto", List.of("hexkagune:quinque_", "hexkagune:mask_arima"),
        "touka", List.of("hexkagune:quinque_", "hexkagune:mask_amon"),
        "ken", List.of("minecraft:spider_eye", "minecraft:fermented_spider_eye", "hexkagune:centipede_soba", "hexkagune:mask_yamori", "minecraft:shears"),
        "ayato", List.of("minecraft:pink_dye", "hexkagune:mask_touka"),
        "noro", List.of(),
        "general", List.of("hexkagune:ghoul_meat", "hexkagune:cooked_ghoul_meat", "hexkagune:human_", "hexkagune:mask_", "hexkagune:anteiku_blend",
            "hexkagune:blood_", "minecraft:rotten_flesh"),
        "roma", List.of("minecraft:book", "minecraft:writable_book", "minecraft:paper"),
        "banjou", List.of("minecraft:wither_rose", "minecraft:tnt"));

    /** True if {@code item} is one of {@code those} (an id, or a prefix ending in '_' or ':'). */
    static boolean among(List<String> those, String item) {
        for (String one : those) {
            boolean prefix = one.endsWith("_") || one.endsWith(":");
            if (prefix ? item.startsWith(one) : item.equals(one)) {
                return true;
            }
        }
        return false;
    }

    /**
     * What official {@code key} makes of {@code item} given as a present: hated first (an insult outweighs), then loved, else liked. The
     * ghouls hate human food; Ayato every flower but the wither rose; Banjou loves every flower (his garden).
     */
    public static Taste of(String key, String item) {
        if (item == null || key == null) {
            return Taste.LIKED;
        }
        Persona persona = Persona.of(key);
        boolean ghoul = persona != null && !persona.human();
        if (ghoul && HUMAN_FOOD.contains(item) || among(HATES.getOrDefault(key, List.of()), item)
            || "ayato".equals(key) && flower(item) && !"minecraft:wither_rose".equals(item)) {
            return Taste.HATED;
        }
        if (among(LOVES.getOrDefault(key, List.of()), item) || "banjou".equals(key) && Courtship.plant(item)) {
            return Taste.LOVED;
        }
        return Taste.LIKED;
    }

    /** How {@code key} takes a present she {@code taste}s (her gesture as she answers). */
    public static EtoGesture gesture(String key, Taste taste) {
        if (taste == Taste.LIKED) {
            return EtoGesture.NONE;
        }
        boolean loved = taste == Taste.LOVED;
        return switch (key == null ? "" : key) {
            case "eto" -> loved ? EtoGesture.GIGGLE : EtoGesture.POUT;
            case "touka" -> loved ? EtoGesture.SHY : EtoGesture.HUFF;
            case "ken" -> loved ? EtoGesture.BOW : EtoGesture.FLINCH;
            case "ayato" -> loved ? EtoGesture.SHRUG : EtoGesture.CROSS;
            case "noro" -> loved ? EtoGesture.TILT : EtoGesture.NONE;
            case "general" -> loved ? EtoGesture.SHY : EtoGesture.CROSS;
            case "roma" -> loved ? EtoGesture.TWIRL : EtoGesture.POUT;
            case "banjou" -> loved ? EtoGesture.AWE : EtoGesture.SHRUG;
            default -> EtoGesture.NONE;
        };
    }

    /** A flower (what a garden takes, but a sapling, an azalea or a propagule). */
    static boolean flower(String item) {
        return Courtship.plant(item) && !item.endsWith("_sapling") && !item.endsWith("azalea") && !item.endsWith("propagule");
    }

    /** Regard {@code taste} brings. */
    public static int regard(Taste taste) {
        return switch (taste) {
            case LOVED -> LOVED_REGARD;
            case LIKED -> LIKED_REGARD;
            case HATED -> HATED_REGARD;
        };
    }

    /** Names the game's ids don't make well ("writable_book": a book and quill). */
    private static final Map<String, String> NAMES = Map.ofEntries(
        Map.entry("writable_book", "book and quill"),
        Map.entry("rabbit_hide", "rabbit hide"),
        Map.entry("ink_sac", "ink sac"),
        Map.entry("glow_ink_sac", "glow ink sac"),
        Map.entry("anteiku_blend", "Anteiku Blend"),
        Map.entry("mask_eyepatch", "eyepatch mask"),
        Map.entry("jack_o_lantern", "jack o'lantern"),
        Map.entry("mangrove_propagule", "mangrove propagule"),
        Map.entry("rc_tonic", "RC tonic"),
        Map.entry("rc_broth", "RC broth"),
        Map.entry("rc_ramen", "RC ramen"));

    /** {@code item} as said aloud: "minecraft:writable_book" a "book and quill", "hexkagune:mask_touka" a "Touka mask". */
    public static String name(String item) {
        if (item == null || item.isEmpty()) {
            return "nothing";
        }
        String path = item.substring(item.indexOf(':') + 1);
        if (NAMES.containsKey(path)) {
            return NAMES.get(path);
        }
        if (path.startsWith("mask_")) {
            String who = path.substring(5).replace('_', ' ');
            return Character.toUpperCase(who.charAt(0)) + who.substring(1) + " mask";
        }
        if (path.startsWith("music_disc_")) {
            return "record";
        }
        if (path.startsWith("quinque_")) {
            return "quinque";
        }
        return path.replace('_', ' ');
    }

    /**
     * {@code name} with its article: "a clock", "an ink sac", "an RC tonic" (an initialism said with a vowel), "some bread" (what isn't
     * counted: bread, meat, flesh, a meal, beef, dye); {@code capital}: "A clock", "An ink sac", "Some bread".
     */
    public static String an(String name, boolean capital) {
        String first = name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name;
        if (name.equals("bread") || name.endsWith(" dye") || name.endsWith(" meat") || name.endsWith(" flesh") || name.endsWith(" meal")
            || name.endsWith(" beef")) {
            return (capital ? "Some " : "some ") + name;
        }
        boolean initials = first.length() >= 2 && first.equals(first.toUpperCase(java.util.Locale.ROOT)) && Character.isLetter(first.charAt(0));
        boolean vowel = !name.isEmpty() && (initials ? "AEFHILMNORSX" : "aeiouAEIOU").indexOf(name.charAt(0)) >= 0;
        return (capital ? (vowel ? "An " : "A ") : (vowel ? "an " : "a ")) + name;
    }

    /** The option on the gifts page: "Here. I want you to have this. (the book and quill in your hand)". */
    public static String offer(String item) {
        return "Here. I want you to have this. (the " + name(item) + " in your hand)";
    }

    /** One of {@code lines} by {@code seed}. */
    private static String one(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** What {@code key} says of a present ({@code item}) she {@code taste}s; {@code together}: dating or married. */
    public static String said(String key, Taste taste, String item, int seed, boolean together) {
        String it = name(item);
        return switch (key == null ? "" : key) {
            case "eto" -> switch (taste) {
                case LOVED -> one(seed, an(it, true) + "! Oh, you've been reading my mind. Or my manuscripts. Either way: mine now.",
                    "*She turns the " + it + " over in both hands, delighted.* For me? You strange, wonderful thing. I'll use it tonight.");
                case HATED -> HUMAN_FOOD.contains(item) ? one(seed, "*She sniffs it and recoils.* Human food. You're testing whether I'll be polite. I won't.",
                    "Do you know what that tastes like to us? Wet paper and rot. Take it back, darling.")
                    : "*Her smile goes very thin.* The Doves' handiwork. You'd hand me that? ...Put it away before I decide what it means.";
                case LIKED -> one(seed, an(it, true) + "? How odd. How lovely. I'll put it somewhere I'll find it again, eventually.",
                    together ? "You bring me the strangest things. Keep doing it." : "Thank you. I think? I'll find a use for it in a story.");
            };
            case "touka" -> switch (taste) {
                case LOVED -> one(seed, "*She takes the " + it + " a little too fast.* ...It's not like I wanted one. Thanks.",
                    an(it, true) + "? ...You remembered. Don't look at me like that. Thank you, okay?");
                case HATED -> HUMAN_FOOD.contains(item) ? one(seed, "I chewed that for a whole lunch break once, smiling, so nobody would notice. Never again. Take it back.",
                    "Are you trying to make me throw up? That's human food. Get it away from me.")
                    : "That's a Dove thing. Are you stupid? Don't carry that around near me.";
                case LIKED -> one(seed, "..." + an(it, true) + ". Okay. Thanks, I guess.", together ? "Weirdo. ...I'll keep it." : "Why would you give me this? ...Fine. Thanks.");
            };
            case "ken" -> switch (taste) {
                case LOVED -> one(seed, an(it, true) + "... *He smiles, small and real.* You didn't have to. Thank you. Really.",
                    "*He holds the " + it + " carefully, like it might break.* This is exactly the kind of thing I like. How did you know?");
                case HATED -> HUMAN_FOOD.contains(item) ? "I can't. I tried, once, the first week. ...I'm sorry. I know you meant well."
                    : one(seed, "*He goes very still. His hand twitches toward his ear.* ...Please put that away. Please.",
                        "*A thousand minus seven. Nine hundred and ninety-three.* ...Sorry. Don't give me that again.");
                case LIKED -> one(seed, an(it, true) + "? Thank you. I'll find somewhere for it.", together ? "You think of me. I notice. Thank you." : "That's kind of you. Thanks.");
            };
            case "ayato" -> switch (taste) {
                case LOVED -> one(seed, "*He snatches the " + it + " and looks it over.* ...Not bad. Don't make a thing of it.",
                    "Tch. " + an(it, true) + ". ...Fine. I'm keeping it. Say thank you for me and I'll kill you.");
                case HATED -> HUMAN_FOOD.contains(item) ? "You want me to eat that? I'll feed it to you through your nose."
                    : flower(item) ? one(seed, "Flowers. For me. Do I look like I want flowers? Do I look like Touka?", "Get that soft garbage out of my face.")
                    : "*He stares at it, then at you.* Is this a joke? Do I look like I'm laughing?";
                case LIKED -> one(seed, "...What am I supposed to do with " + an(it, false) + "? ...Whatever. Leave it.", "Tch. Fine. Thanks or whatever.");
            };
            case "noro" -> switch (taste) {
                case LOVED -> one(seed, "*He takes the " + it + ". He looks at it for a long time.* ...Good.", "*The mask tilts. He keeps it.* ...More.");
                case HATED -> "*He lets it fall from his hand to the floor and does not look at it again.*";
                case LIKED -> one(seed, "*He takes it.* ...", "*The mask tilts, slowly.*");
            };
            case "general" -> switch (taste) {
                case LOVED -> one(seed, an(it, true) + ". *He clears his throat.* That's... practical. Very practical. Thank you.",
                    "*He tucks the " + it + " away quickly, ears going red.* I'll make good use of it. That's all.");
                case HATED -> item.startsWith("hexkagune:mask_") ? "A ghoul's mask. You think this is funny? I've buried men who saw that face last."
                    : one(seed, "Is that... no. Get it away from me. I know what that's made of.", "I won't touch that. Don't ask me again.");
                case LIKED -> one(seed, an(it, true) + "? ...Thank you. I'll find a place for it.", together ? "You keep bringing me things. I'm not complaining." : "Noted. Thank you.");
            };
            case "roma" -> switch (taste) {
                case LOVED -> one(seed, "Ooh! " + an(it, true) + "! For me? For ME? This is going straight into the act!",
                    "*She spins with the " + it + " held over her head.* Best audience ever! Encore! Bring me another!");
                case HATED -> HUMAN_FOOD.contains(item) ? "Bleh! Human food! I served that stuff for months pretending to be a waitress. Never again, sweetheart."
                    : one(seed, "Homework?! Boooring! Take it back before it bores me to death!", "Eww, paper. Paper is for contracts and tickets. Take it back!");
                case LIKED -> one(seed, an(it, true) + "? Hehe. Weird! I love weird. Thanks!", together ? "You're so funny. Bringing me junk. ...I'm keeping it." : "Oh! Um. Thanks!");
            };
            case "banjou" -> switch (taste) {
                case LOVED -> one(seed, "*His big hands close round the " + it + " so carefully.* For the garden? ...Oh, this is good. Thank you. Really.",
                    an(it, true) + "! I know just where it goes. You'll have to come see it once it's settled.");
                case HATED -> HUMAN_FOOD.contains(item) ? "Ah... that's human food. I can't. It's kind, though. The thought is kind."
                    : one(seed, "That kills everything round it. ...I'll put it far from the others. Maybe in the river.", "No, no, not near the garden. Please. Take it back.");
                case LIKED -> one(seed, an(it, true) + "? Thank you. I'll find somewhere good for it.", together ? "You always think of me. ...Thank you." : "That's kind of you. Thank you.");
            };
            default -> taste == Taste.HATED ? "No, thank you." : "Thank you.";
        };
    }

    /**
     * 4.1.85 (romance's last step of the plan: "a small daily 'what do you want today?' once together"): what each may wish for on a day,
     * things they love that can be had (none behind a boss or a deep mine), their wish of the day picked from them by the day. Not Noro's:
     * no one is ever together with him (Persona: not romanceable).
     */
    public static final Map<String, List<String>> WISHES = Map.of(
        "eto", List.of("hexkagune:anteiku_blend", "minecraft:ink_sac", "minecraft:feather", "minecraft:candle", "minecraft:writable_book"),
        "touka", List.of("hexkagune:anteiku_blend", "minecraft:rabbit_hide", "minecraft:writable_book", "hexkagune:blood_latte"),
        "ken", List.of("hexkagune:anteiku_blend", "minecraft:writable_book", "minecraft:lantern", "hexkagune:crimson_tea"),
        "ayato", List.of("minecraft:black_dye", "minecraft:iron_sword", "hexkagune:rc_tonic"),
        "general", List.of("minecraft:clock", "minecraft:bread", "minecraft:cooked_beef", "minecraft:writable_book", "minecraft:shield"),
        "roma", List.of("minecraft:firework_star", "minecraft:slime_ball", "minecraft:jack_o_lantern", "minecraft:purple_dye", "minecraft:pink_dye"),
        "banjou", List.of("minecraft:bone_meal", "minecraft:oak_sapling", "minecraft:flower_pot", "minecraft:cherry_sapling", "minecraft:poppy"));

    /** 4.1.85: regard a wish granted brings on top of the present's own (a loved one: LOVED_REGARD). */
    public static final int WISH_REGARD = 4;
    /** 4.1.85: what the main page asks them, together. */
    public static final String WISH_ASK = "What do you want today?";

    /** 4.1.85: {@code key}'s wish on game day {@code day} (the same all that day, another most days after), or null for none of theirs. */
    public static String wish(String key, long day) {
        List<String> wishes = WISHES.get(key);
        if (wishes == null || wishes.isEmpty()) {
            return null;
        }
        long mixed = day * 0x9E3779B97F4A7C15L + key.hashCode();
        return wishes.get((int)Math.floorMod(mixed ^ mixed >>> 29, (long)wishes.size()));
    }

    /** 4.1.85: what {@code key} says, asked what they want today: {@code item}, in their words. */
    public static String wishSaid(String key, String item, int seed) {
        String it = name(item);
        return switch (key == null ? "" : key) {
            case "eto" -> one(seed, "What do I want? Mm... " + an(it, false) + ". Bring me one and I'll put you in a chapter. A nice one.",
                "Today? " + an(it, true) + ". Don't ask why. Writers have their reasons, and most of them are bad.");
            case "touka" -> one(seed, "...Why are you asking? ...Fine. " + an(it, true) + ". Not that I'll be waiting for it.",
                "Huh? Nothing. ...Okay, " + an(it, false) + ". Happy now?");
            case "ken" -> one(seed, "Today? I'd like " + an(it, false) + ", I think. Only if it's no trouble.", an(it, true) + ". ...Sorry. It's silly. You asked.");
            case "ayato" -> one(seed, "Tch. " + an(it, true) + ". Don't make it weird.", "Why do you care? ...Fine. " + an(it, true) + ". And hurry up.");
            case "general" -> one(seed, "Today? ...I could use " + an(it, false) + ". That's an observation, not a request.",
                an(it, true) + ". *He clears his throat.* If you happen to have it on you. On your way. No detours.");
            case "roma" -> one(seed, "Ooh, wishes! I wish for " + an(it, false) + "! The shiniest you can find! Go, go, go!", an(it, true) + "! Bring me that and I'll do a trick. A good one!");
            case "banjou" -> one(seed, "Me? Oh... " + an(it, false) + " would be nice. For the garden. If you're passing that way.",
                "I've been wanting " + an(it, false) + ". Don't go out of your way. ...Unless you want to.");
            default -> "Nothing, really.";
        };
    }

    /** 4.1.85: what {@code key} says when the present is today's wish. */
    public static String granted(String key, String item, int seed) {
        String it = name(item);
        return switch (key == null ? "" : key) {
            case "eto" -> "*She laughs, delighted, holding the " + it + " up to the light.* You actually listened. Do you know how rare that is?";
            case "touka" -> "...You actually brought it. *She looks away, going red.* Idiot. ...Thanks.";
            case "ken" -> "You remembered. *He holds the " + it + " close.* ...Thank you. You don't know what that means to me.";
            case "ayato" -> "...You remembered. *He tries not to look pleased, and fails.* Whatever. ...Thanks.";
            case "general" -> "You... remembered. *He clears his throat twice.* That's very thoughtful. Very.";
            case "roma" -> "My wish came true! Encore! Encore! You're my favourite audience in the WHOLE world!";
            case "banjou" -> "You brought it! *His big face splits into a smile.* It's going in the garden today. You'll come and see?";
            default -> "You remembered.";
        };
    }

    /** What {@code key} says when they've had a present from you today already (nothing taken). */
    public static String again(String key, int seed) {
        return switch (key == null ? "" : key) {
            case "eto" -> "Another? Darling, you'll spoil me. Tomorrow.";
            case "touka" -> "You already gave me something today. Don't overdo it. It's embarrassing.";
            case "ken" -> "You've already given me something today. Keep it. Tomorrow, maybe.";
            case "ayato" -> "Again? What are you, my aunt? Tomorrow.";
            case "noro" -> "*The mask turns away.*";
            case "general" -> "One a day is plenty. Any more and people will talk.";
            case "roma" -> one(seed, "Greedy, greedy! One a day, sweetheart, or the act gets boring.", "Save it! Save it for tomorrow's show!");
            case "banjou" -> "You've given me something today already. That's plenty. Truly.";
            default -> "Tomorrow.";
        };
    }
}
