package com.hex.kagune.control.npc;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.quest.Gate;
import java.util.List;
import java.util.function.Predicate;

/**
 * 1.3.14: what the companions teach (pure, tested). The best of each path is no longer reached by mastery and quests alone: the mastery
 * is still needed, then the one who knows it has to be found and asked. Each {@link Lesson} opens a {@link Gate} (the gate every key of
 * the game already checks), in order, for a ghoul on that gate's path. Touka teaches the wings (Ukaku): crystallizing them, the
 * Gatling, the first kakuja. Ken teaches what it costs to go further: the centipede (Rinkaku's first kakuja) and the Ukaku's final one
 * through what was done to him ({@link Kind#TRIAL}: the torture, lived through), the Full Kakuja, and the Dragon. Partner or not, anyone
 * the teacher is not an enemy of may learn.
 */
public final class Teaching {
    /** How a lesson is taught: told and shown, or lived through (the torture, a cutscene, before the gate opens). */
    public enum Kind {
        TALK, TRIAL
    }

    /** One lesson: its id, its teacher (a {@link Persona} key), the gate it opens, how it is taught, and its name. */
    public record Lesson(String id, String teacher, Gate gate, Kind kind, String name) {
        /** The mastery it needs: its gate's. */
        public int mastery() {
            return this.gate.mastery();
        }

        /** The path it is for. */
        public KagunePath path() {
            return this.gate.path();
        }
    }

    /** Why a teacher can't teach anything now. */
    public enum Refusal {
        /** A human: there are no wings or tails to teach. */
        HUMAN,
        /** Their kagune is not one this teacher knows. */
        WRONG_PATH,
        /** Not strong enough for the next one yet ({@link View#need} the mastery it needs). */
        WEAK,
        /** The step before it isn't theirs yet (a gate another way opens: {@link View#before}). */
        BEFORE,
        /** Everything this teacher knows, they know. */
        ALL_TAUGHT
    }

    /** What the teacher could do for them now: the lesson to teach (null if none), why not, the mastery needed, the gate missing before. */
    public record View(Lesson next, Refusal refusal, int need, Gate before) {
        public static final View NONE = new View(null, Refusal.ALL_TAUGHT, 0, null);

        /** True if a lesson can be taught right now. */
        public boolean ready() {
            return this.next != null && this.refusal == null;
        }

        /** True if this teacher has anything at all for them (taught now or later on their path). */
        public boolean teaches() {
            return this.next != null;
        }
    }

    public static final String TOUKA = "touka";
    public static final String KEN = "ken";
    /** 4.0.0: Ayato (Ukaku: his respect, then the first kakuja) and Noro (Bikaku: everything past the worm's first bites). */
    public static final String AYATO = "ayato";
    public static final String NORO = "noro";

    /** Every lesson, in the order each teacher teaches them. */
    public static final List<Lesson> ALL = List.of(
        new Lesson("touka_crystal", TOUKA, Gate.UKAKU_CRYSTAL, Kind.TALK, "Crystal Wings"),
        new Lesson("touka_gatling", TOUKA, Gate.UKAKU_GATLING, Kind.TALK, "Gatling"),
        // 4.0.0: the first kakuja is Ayato's to teach now (the owner); Touka's lesson of it is gone (it was her last: her others keep their bits).
        new Lesson("ken_centipede", KEN, Gate.RINKAKU_CENTIPEDE, Kind.TRIAL, "The Centipede"),
        new Lesson("ken_king", KEN, Gate.RINKAKU_FULL_KAKUJA, Kind.TALK, "The Full Kakuja"),
        new Lesson("ken_dragon", KEN, Gate.RINKAKU_DRAGON, Kind.TALK, "The Dragon"),
        new Lesson("ken_storm", KEN, Gate.UKAKU_FINAL_KAKUJA, Kind.TRIAL, "The Final Kakuja"),
        // 3.3.1 (the owner: Ken teaches the Impale and the Rubble Hurl, with proofs). Appended: each lesson's saved bit is its place
        // among its teacher's, so the old ones keep theirs; he still teaches them first ({@link #view} goes by mastery).
        new Lesson("ken_hurl", KEN, Gate.RINKAKU_HURL, Kind.TALK, "Rubble Hurl"),
        new Lesson("ken_impale", KEN, Gate.RINKAKU_IMPALE, Kind.TALK, "Impale"),
        // 4.0.0 (the owner: "players must earn his respect, complete meaningful challenges, and eventually unlock their Kakuja through a
        // custom cinematic cutscene"): Ayato's road, each step a proof; the last lived through (its cutscene).
        new Lesson("ayato_doves", AYATO, Gate.UKAKU_AYATO_DOVES, Kind.TALK, "Respect: Doves"),
        new Lesson("ayato_hunt", AYATO, Gate.UKAKU_AYATO_HUNT, Kind.TALK, "Respect: the Hunt"),
        new Lesson("ayato_kakuja", AYATO, Gate.UKAKU_KAKUJA, Kind.TRIAL, "The First Kakuja"),
        // 4.0.0 (the owner: Noro teaches "its normal form, Half-Kakuja, Full-Kakuja, and unlockable mutations ... through dedicated quests,
        // training, and progression challenges"): each a proof; The Walker and the kakuja lived through (their cutscenes).
        new Lesson("noro_worms", NORO, Gate.BIKAKU_WORMS, Kind.TALK, "The Worms"),
        new Lesson("noro_walker", NORO, Gate.BIKAKU_WALKER, Kind.TRIAL, "The Walker"),
        new Lesson("noro_kakuja", NORO, Gate.BIKAKU_KAKUJA, Kind.TRIAL, "The Kakuja"),
        new Lesson("noro_devouring", NORO, Gate.BIKAKU_DEVOURING, Kind.TALK, "Grand Devouring"),
        new Lesson("noro_potential", NORO, Gate.BIKAKU_POTENTIAL, Kind.TRIAL, "Potential Unlock"));

    /**
     * What has to be open before a lesson's gate can be taught (the step before it on the path, whoever opened it), or null. Ken's
     * centipede follows the four tails (opened by their quests); Touka's crystals come first of all.
     */
    public static Gate before(Gate gate) {
        return switch (gate) {
            case UKAKU_GATLING -> Gate.UKAKU_CRYSTAL;
            case UKAKU_AYATO_DOVES -> Gate.UKAKU_GATLING;
            case UKAKU_AYATO_HUNT -> Gate.UKAKU_AYATO_DOVES;
            case UKAKU_KAKUJA -> Gate.UKAKU_AYATO_HUNT;
            case BIKAKU_WALKER -> Gate.BIKAKU_WORMS;
            case BIKAKU_KAKUJA -> Gate.BIKAKU_WALKER;
            case BIKAKU_DEVOURING -> Gate.BIKAKU_KAKUJA;
            case BIKAKU_POTENTIAL -> Gate.BIKAKU_DEVOURING;
            case UKAKU_FINAL_KAKUJA -> Gate.UKAKU_KAKUJA;
            case RINKAKU_CENTIPEDE -> Gate.RINKAKU_FOUR;
            case RINKAKU_FULL_KAKUJA -> Gate.RINKAKU_CENTIPEDE;
            case RINKAKU_DRAGON -> Gate.RINKAKU_FULL_KAKUJA;
            case RINKAKU_IMPALE -> Gate.RINKAKU_HURL;
            default -> null;
        };
    }

    private Teaching() {
    }

    /** The lessons {@code teacher} teaches, in the order they were added (each one's saved bit: {@link #bit}). */
    public static List<Lesson> of(String teacher) {
        return ALL.stream().filter(lesson -> lesson.teacher().equals(teacher)).toList();
    }

    /** 3.3.1: the lessons {@code teacher} teaches in the order they are taught: by the mastery they need (a stable sort: ties as added). */
    public static List<Lesson> inOrder(String teacher) {
        return of(teacher).stream().sorted(java.util.Comparator.comparingInt(Lesson::mastery)).toList();
    }

    /** The lesson that opens {@code gate}, or null when no one teaches it (it is opened by its quests, as before). */
    public static Lesson teaching(Gate gate) {
        for (Lesson lesson : ALL) {
            if (lesson.gate() == gate) {
                return lesson;
            }
        }
        return null;
    }

    /** True if {@code gate} is taught by someone (its quests no longer open it). */
    public static boolean taught(Gate gate) {
        return teaching(gate) != null;
    }

    public static Lesson byId(String id) {
        for (Lesson lesson : ALL) {
            if (lesson.id().equals(id)) {
                return lesson;
            }
        }
        return null;
    }

    /**
     * What {@code teacher} can do for a player: {@code ghoul} or not, on {@code path}, at {@code mastery}, {@code open} the gates they
     * have. The first lesson of theirs on the player's path not yet open is the next; it is ready once the mastery and the step before it
     * are theirs.
     */
    public static View view(String teacher, boolean ghoul, KagunePath path, int mastery, Predicate<Gate> open) {
        if (!ghoul) {
            return new View(null, Refusal.HUMAN, 0, null);
        }
        boolean knows = false;
        for (Lesson lesson : inOrder(teacher)) {
            if (lesson.path() != path) {
                continue;
            }
            knows = true;
            if (open.test(lesson.gate())) {
                continue;
            }
            Gate before = before(lesson.gate());
            if (before != null && !open.test(before)) {
                return new View(lesson, Refusal.BEFORE, lesson.mastery(), before);
            }
            if (mastery < lesson.mastery()) {
                return new View(lesson, Refusal.WEAK, lesson.mastery(), null);
            }
            return new View(lesson, null, lesson.mastery(), null);
        }
        return new View(null, knows ? Refusal.ALL_TAUGHT : Refusal.WRONG_PATH, 0, null);
    }

    /** 1.3.29: {@code lesson}'s bit among its teacher's (kept per player: taught, or told it was known). */
    public static long bit(Lesson lesson) {
        int index = of(lesson.teacher()).indexOf(lesson);
        return index < 0 ? 0L : 1L << index;
    }

    /**
     * 1.3.29: the lessons of {@code teacher} for a ghoul on {@code path} who has their gate open already ({@code open}) but was never taught
     * them by this teacher nor told so ({@code told}: their bits): known some other way (a command, an old quest). In order.
     */
    public static List<Lesson> knownElsewhere(String teacher, KagunePath path, Predicate<Gate> open, long told) {
        return of(teacher).stream().filter(lesson -> lesson.path() == path && open.test(lesson.gate()) && (told & bit(lesson)) == 0L).toList();
    }

    /** 1.3.29: "A", "A and B", "A, B and C". */
    public static String names(List<String> names) {
        if (names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    /**
     * 1.3.29: what a teacher ({@code teacher}: a companion's key; Eto too) says when asked for something they already know ({@code names}),
     * before going on to the next thing (taught, or why not).
     */
    public static String alreadyKnown(String teacher, List<String> names, int seed) {
        String what = names(names);
        boolean one = names.size() <= 1;
        String[] lines = switch (teacher) {
            case AYATO -> new String[] {"You already know " + what + ". What, you want a medal? Next.",
                what + "? You've got " + (one ? "that" : "those") + ". Don't make me repeat myself, I hate it."};
            case NORO -> new String[] {"...Known.", "..."};
            case TOUKA -> new String[] {"You already know " + what + ". Don't waste my time. ...Fine. Next.",
                what + "? You've got " + (one ? "that one" : "those") + " already. I'm not saying it twice."};
            case KEN -> new String[] {"You already know " + what + ". You don't need me for " + (one ? "that one" : "those") + ". Let's move on.",
                "You know " + what + " already. ...You learned it somewhere harder than here, didn't you."};
            default -> new String[] {"You already know " + what + ". Don't look so smug. Next.",
                what + "? You know " + (one ? "that one" : "those") + " already. I don't repeat myself. Moving on."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    // ---------------------------------------------------------------- 2.1.8: proof before the final kakujas

    /** What a proof asks for: ghoul meat eaten, Doves of a rank killed, wild ghouls killed in the first kakuja (Ukaku's). */
    public enum Deed {
        EAT_GHOUL, DOVES, GHOULS_IN_KAKUJA,
        /** 3.3.1: bodies thrown from the tails (N, someone held); bodies crushed in them (H, someone held). */
        THROWS, CRUSHES,
        /** 4.0.0: wild ghouls of a rank (the proof's {@code rank}: D 0 .. SS 5) killed; Aogiri's own too. */
        GHOUL_RANK,
        /** 4.0.0: bodies brought down with the Bikaku's worm out (it eats them). */
        DEVOURED
    }

    /**
     * 2.1.8 (the owner: "Make Ken's quests a little harder specifically the ones involving the final kakuja variant"): what Ken asks to see
     * done before he teaches a final kakuja ({@code need} of {@code deed}; {@code rank} the Doves' least), in his words ({@code ask} as he sets
     * it, {@code progress} while it is under way: {n} done, {need} needed).
     */
    public record Proof(Deed deed, int need, int rank, String ask, String progress) {
        /** What the game says on the action bar as one is done. */
        public String bar(int done) {
            return bar("Ken", done);
        }

        /** 4.0.0: the same, for any teacher ({@code teacher}: their name). */
        public String bar(String teacher, int done) {
            String what = switch (this.deed) {
                case EAT_GHOUL -> "ghoul meat eaten";
                case DOVES -> "Doves of Rank " + this.rank + "+ killed";
                case GHOULS_IN_KAKUJA -> "ghouls killed in your kakuja";
                case THROWS -> "bodies thrown from your tails";
                case CRUSHES -> "bodies crushed in your tails";
                case GHOUL_RANK -> "ghouls of " + com.hex.kagune.control.npc.NpcRules.rankName(this.rank) + "-rank or above killed";
                case DEVOURED -> "bodies brought down by your worm";
            };
            return teacher + "'s proof: " + Math.min(done, this.need) + " of " + this.need + " " + what + (done >= this.need ? ". Go and see him." : "");
        }
    }

    /** The proof {@code lesson} needs before it is taught, or null (Touka's lessons and the centipede: none; 3.3.1: Ken's Hurl and Impale have one). */
    public static Proof proof(Lesson lesson) {
        if (lesson == null) {
            return null;
        }
        return switch (lesson.id()) {
            case "ken_king" -> new Proof(Deed.EAT_GHOUL, 5, 0,
                "The Full Kakuja isn't taught. It's eaten. You have to eat your own until there's nothing human left to complain. Eat ghoul meat. "
                    + "Five. Then come back, and I'll show you how to keep your mind while you wear it.",
                "{n} of five. Keep eating. I know what it tastes like. I'm sorry.");
            case "ken_dragon" -> new Proof(Deed.DOVES, 3, 6,
                "The Dragon only wakes in someone who's stood at the edge and come back. Kill three Doves who are worth fearing. Rank 6 or above. "
                    + "Then come back alive, and we'll talk about dying.",
                "{n} of three. Rank 6 or above. They'll find you. They always do.");
            // 3.3.1: the Hurl is a throw with nothing in the tails but the ground; the Impale, a crush turned outward. He wants the plain
            // ones first, done until they're not a thought any more.
            case "ken_hurl" -> new Proof(Deed.THROWS, 5, 0,
                "Before you throw the ground at anyone, throw something that fights back. Grab someone with your tails and throw them. Five "
                    + "times. You have to know how the weight leaves you before there's no one in your grip but stone.",
                "{n} of five. Grab, and throw. Don't think about where they land. That comes later.");
            case "ken_impale" -> new Proof(Deed.CRUSHES, 5, 0,
                "The Impale is a crush that doesn't wait for you to be close. You have to know what a tail feels like closing on someone "
                    + "first. Crush five, in your tails. ...I know. I'm sorry. It's the only way it holds still long enough to learn.",
                "{n} of five. In your tails. I'll be here. I'm not going anywhere.");
            // 4.0.0: Ayato's: he doesn't teach anyone he can't respect. Doves first; then ghouls worth fearing; the kakuja last, after an S.
            case "ayato_doves" -> new Proof(Deed.DOVES, 3, 3,
                "You want something from me? Earn it. Three Doves. Rank 3 or higher, not the paper-pushers. Kill them and come back. If you "
                    + "come back.",
                "{n} of three. Rank 3 or up. Stop wasting my time and go.");
            case "ayato_hunt" -> new Proof(Deed.GHOUL_RANK, 3, com.hex.kagune.control.npc.NpcRules.A,
                "Doves are easy. Our own kind isn't. Three wild ghouls, A-rank or worse. The ones that actually fight back. Show me your wings "
                    + "can take a real hit.",
                "{n} of three A-ranks. Don't come crying to me if one of them eats you.");
            case "ayato_kakuja" -> new Proof(Deed.GHOUL_RANK, 1, com.hex.kagune.control.npc.NpcRules.S,
                "Fine. You're not completely useless. Last thing. Find an S-rank. Or an SS, if you've got a death wish. Kill it. Then we'll see "
                    + "if your wings have more in them.",
                "Still nothing? An S-rank. They're out there. Hunt.");
            // 4.0.0: Noro's: few words. Feed the worm; then the hunt; then the strong.
            case "noro_worms" -> new Proof(Deed.EAT_GHOUL, 3, 0, "...Eat. Three. Ghoul.", "...{n}.");
            case "noro_walker" -> new Proof(Deed.GHOUL_RANK, 3, com.hex.kagune.control.npc.NpcRules.B, "...Hunt. Three. Strong ones.", "...{n}.");
            case "noro_kakuja" -> new Proof(Deed.GHOUL_RANK, 1, com.hex.kagune.control.npc.NpcRules.S, "...One. The strongest you find.", "...Not yet.");
            case "noro_devouring" -> new Proof(Deed.DEVOURED, 10, 0, "...Let it eat. Ten. Worm out.", "...{n}. More.");
            case "noro_potential" -> new Proof(Deed.DOVES, 3, 6, "...Three Doves. Rank six. Come back in pieces. Or don't.", "...{n}.");
            case "ken_storm" -> new Proof(Deed.GHOULS_IN_KAKUJA, 10, 0,
                "Your wings can go further. But the storm has to be fed first. Put on your kakuja and hunt your own: ten wild ghouls, with your "
                    + "first kakuja's wings out. Then come back, and sit down. It won't be gentle.",
                "{n} of ten, in your kakuja. Feed the storm. It's hungrier than you are.");
            default -> null;
        };
    }

    /** {@code text} with a proof's counts put in. */
    public static String fill(String text, Proof proof, int done) {
        return text.replace("{n}", Integer.toString(Math.min(done, proof.need()))).replace("{need}", Integer.toString(proof.need()));
    }

    /** "Touka teaches it (mastery 400)": what the game says when a taught gate stands in the way. */
    public static String gatedText(Gate gate, String teacherName) {
        Lesson lesson = teaching(gate);
        if (lesson == null) {
            return "";
        }
        Gate before = before(gate);
        return gate.displayName() + ": " + teacherName + " teaches it, at mastery " + lesson.mastery()
            + (before == null ? "" : ", after " + before.displayName()) + ". Find them and ask.";
    }
}
