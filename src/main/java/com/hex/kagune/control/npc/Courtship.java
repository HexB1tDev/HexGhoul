package com.hex.kagune.control.npc;

import com.hex.kagune.control.quest.QuestLog;
import java.util.List;
import java.util.Set;

/**
 * 2.1.2 (pure, tested; the owner: "adapt the Ken, Eto and Touka quests for romance better, so that it's more immersive and less
 * repetitive, it's super boring at it stands"): each companion's own courtship, in place of the three errands they all shared (books,
 * ten kills at night, a flower). Five chapters each, every one asking for something only that one would ask (Eto wants a page you wrote,
 * a walk in the dark, ink, to watch you hunt, the Library at three in the morning; Touka beans for the cafe, the ward cleared of what
 * draws the Doves, somewhere high, company through exam night, a rabbit's foot; Ken a book, coffee taken slowly, one man-eater stopped,
 * white flowers for his door, one sunrise not alone), and every one closed by its moment: a scene of its own in the companion's script
 * ({@code moment <chapter id>} in {@code data/hexkagune/talk/<key>.txt}) that plays as soon as the task is done. After the fifth, they
 * may ask the companion out.
 *
 * <p>Kept in the bond as the errands were: {@link EtoRules#LINE_ON} once begun, the chapters closed in three bits of its flags
 * ({@link #SHIFT}), the task's progress in its favour count's low byte ({@link EtoRules#count}). A bond from before (the errands' flags)
 * is read as so many chapters closed, and the three errands all done as all five ({@link #migrate}).
 */
public final class Courtship {
    private Courtship() {
    }

    /** Every companion's courtship has this many chapters. */
    public static final int CHAPTERS = 5;
    /** It may begin once they are this close (a friend: the courtship carries them the rest of the way). */
    public static final int BEGIN_AT = NpcBonds.FRIEND;
    /** The chapters closed, in the bond's flags: three bits from here. */
    public static final int SHIFT = 13;
    static final int BITS = 7 << SHIFT;
    /** The errands' flags (before 2.1.2), read once and cleared. */
    static final int LEGACY = EtoRules.BOOKS_DONE | EtoRules.HUNT_DONE | EtoRules.FLOWER_DONE;
    /** A walk or a sit is counted in steps of this many seconds. */
    public static final int STEP_SECONDS = 10;
    /** High enough (block height) for "somewhere high". */
    public static final int HIGH = 100;
    /** Near enough (blocks) to be "with" them at dawn or in the night; to be sitting with them. */
    public static final double WITH = 10.0;
    public static final double SITTING = 6.0;

    /** What a chapter asks for. */
    public enum Task {
        /** Something handed over, {@code need} of it (held out on the talk screen: all of it at once). */
        GIVE,
        /** Monsters killed at night, anywhere. */
        KILLS,
        /** Monsters killed at night while the companion walks with them. */
        KILLS_WITH,
        /** Wild ghouls killed. */
        GHOUL,
        /** Walking with the companion following (steps of {@link #STEP_SECONDS}), after dark. */
        WALK_NIGHT,
        /** Sitting with them at their own place (near, not following them; steps of {@link #STEP_SECONDS}). */
        SIT,
        /** The companion following them up somewhere high ({@link #HIGH}). */
        HIGH,
        /** With them at dawn. */
        DAWN,
        /** With them at their own place in the dead of night. */
        DEEP;

        /** True if counted in steps of time (shown in minutes). */
        public boolean timed() {
            return this == WALK_NIGHT || this == SIT;
        }
    }

    /**
     * A chapter: its id (its moment's scene in the script), its title, what it asks for and how much ({@code items} the item ids a
     * {@link Task#GIVE} takes, {@code itemName} what to call them on the talk screen), and its words: {@code ask} as it begins (and again if
     * asked before anything is done), {@code progress} while it is under way, {@code journal} the task as the journal says it. In the words,
     * {@code {n}} is how much is done, {@code {need}} how much is needed ({@code {m}}, {@code {mm}}: the same in minutes).
     */
    public record Chapter(String id, String title, Task task, int need, Set<String> items, String itemName, String ask, String progress,
                          String journal) {
    }

    // ---------------------------------------------------------------- the chapters

    static final Set<String> WRITTEN = Set.of("minecraft:written_book");
    static final Set<String> INK = Set.of("minecraft:ink_sac", "minecraft:glow_ink_sac");
    static final Set<String> COCOA = Set.of("minecraft:cocoa_beans");
    static final Set<String> RABBIT_FOOT = Set.of("minecraft:rabbit_foot");
    static final Set<String> BOOKS = Set.of("minecraft:book", "minecraft:writable_book", "minecraft:enchanted_book", "minecraft:written_book",
        "minecraft:knowledge_book");
    static final Set<String> WHITE_FLOWERS = Set.of("minecraft:white_tulip", "minecraft:oxeye_daisy", "minecraft:lily_of_the_valley",
        "minecraft:azure_bluet");

    static final List<Chapter> ETO = List.of(
        new Chapter("eto_letter", "Write me something", Task.GIVE, 1, WRITTEN, "the book you wrote",
            "You want to do something for me? Write me something. A page, a poem, a grocery list, I don't care. Put it in a book, sign it, bring "
                + "it here. I want to see how your head works when nobody's watching it.",
            "Still waiting on my page. A book and quill, write, sign it. It doesn't have to be good. ...It has to be yours.",
            "Write Eto something in a book and quill, sign it, and hand it to her"),
        new Chapter("eto_night_walk", "A walk after dark", Task.WALK_NIGHT, 18, Set.of(), "",
            "Walk with me tonight. Not to anywhere. Just out, in the dark, while the city pretends it's asleep. Once it's dark, ask me to follow "
                + "you, and don't let me get bored.",
            "We've walked {m} of {mm} minutes. The night's still young. So am I. Relatively.",
            "After dark, have Eto follow you and walk together: {m} of {mm} minutes"),
        new Chapter("eto_ink", "Ink for a monster", Task.GIVE, 4, INK, "the ink sacs",
            "My pen's run dry, and I'm in the middle of something terrible. Bring me ink. Squid ink, four sacs. The glowing kind is fine too, "
                + "I'll write something that glows.",
            "Ink. {n} of four. Squids live in water, in case you were wondering where.",
            "Bring Eto ink sacs (glowing ones count too): {n} of {need}"),
        new Chapter("eto_watch", "Let me watch you", Task.KILLS_WITH, 6, Set.of(), "",
            "I want to watch you hunt. Not because I'm hungry. Because people show you who they are with blood on their hands. Take me out "
                + "after dark and kill six things while I watch.",
            "{n} of six. Keep going. I'm taking notes. In my head. Mostly.",
            "After dark, with Eto following you, kill monsters while she watches: {n} of {need}"),
        new Chapter("eto_deep", "The Library at three in the morning", Task.DEEP, 1, Set.of(), "",
            "Last thing. Come to the Library in the dead of night, when even the books are asleep. Stay with me there. I want to show you "
                + "something no one's ever seen.",
            "The dead of night. The Library. Me. You know where. Don't be late. I hate waiting almost as much as I hate being early.",
            "In the dead of night, be with Eto at the Library"));

    static final List<Chapter> TOUKA = List.of(
        new Chapter("touka_beans", "The cafe's out of beans", Task.GIVE, 8, COCOA, "the cocoa beans",
            "...Fine. Since you won't leave me alone. The cafe's out of good beans. Bring me eight. Cocoa beans, the brown pods off jungle trees. "
                + "Don't ask. And don't eat any.",
            "Beans. {n} of eight. It's not hard. Jungle. Trees. Pods. Go.",
            "Bring Touka cocoa beans for the cafe: {n} of {need}"),
        new Chapter("touka_patrol", "Doves in the ward", Task.KILLS, 8, Set.of(), "",
            "Doves have been sniffing round the ward. Every time something ugly crawls out at night and eats someone, they come looking, and "
                + "they find us instead. Clear the night out. Eight of them. I'll know if you lie.",
            "{n} of eight. The night's not going to clean itself.",
            "Kill monsters at night to keep the Doves away from the ward: {n} of {need}"),
        new Chapter("touka_roof", "Somewhere high", Task.HIGH, 1, Set.of(), "",
            "Take me somewhere high. Higher than the rooftops. A mountain, a tower, I don't care. Somewhere the whole world's under us and "
                + "nobody's looking up. Ask me to follow you, and climb.",
            "Still on the ground. Higher. Way higher. Somewhere the clouds are close.",
            "With Touka following you, climb somewhere high (height " + HIGH + " or more)"),
        new Chapter("touka_exam", "Exam night", Task.SIT, 12, Set.of(), "",
            "I have an exam. A real one, human school, don't laugh. Sit with me while I study. You don't have to say anything. Just... be there, "
                + "so I don't throw the book at the wall.",
            "Sit. Stay. Don't talk. You've lasted {m} of {mm} minutes. That's a record, for you.",
            "Sit with Touka at the cafe while she studies: {m} of {mm} minutes"),
        new Chapter("touka_rabbit", "A rabbit's foot", Task.GIVE, 1, RABBIT_FOOT, "the rabbit's foot",
            "Last thing. Bring me a rabbit's foot. ...Don't ask why. Just do it. And if you make one joke about my mask I'll put you through a "
                + "wall.",
            "A rabbit's foot. One. You know what a rabbit is.",
            "Bring Touka a rabbit's foot"));

    static final List<Chapter> KEN = List.of(
        new Chapter("ken_book", "A book he hasn't read", Task.GIVE, 1, BOOKS, "the book",
            "You'd do something for me? ...Then bring me a book. One I haven't read. That's all. That's all I've ever really wanted from anyone.",
            "A book. Any book. ...Well, not any. One I haven't read. I've read a lot. Sorry.",
            "Bring Ken a book he hasn't read"),
        new Chapter("ken_coffee", "Coffee, slowly", Task.SIT, 12, Set.of(), "",
            "Have coffee with me. ...That's it. That's the favour. Sit with me a while, here, and don't hurry. I'm bad at asking people to stay.",
            "Stay a little longer. {m} of {mm} minutes. The coffee's still warm.",
            "Sit with Ken at his place over coffee: {m} of {mm} minutes"),
        new Chapter("ken_ghoul", "The one who eats people", Task.GHOUL, 1, Set.of(), "",
            "There's a ghoul out there eating people who can't fight back. Not hunting. Eating. I've seen what's left. I can't be everywhere. "
                + "...Stop one. Please. And come back alive.",
            "Not yet? ...Be careful. A wild ghoul. They're out there. They always are.",
            "Kill a wild ghoul that preys on people"),
        new Chapter("ken_flowers", "White flowers by the door", Task.GIVE, 3, WHITE_FLOWERS, "the white flowers",
            "I'd like to plant something by the door. White flowers. Three. Daisies, white tulips, lilies of the valley... anything white. I used "
                + "to dream of red ones. I'd like to stop.",
            "{n} of three white flowers. ...Thank you for not asking about the red ones.",
            "Bring Ken white flowers (white tulips, oxeye daisies, lilies of the valley, azure bluets): {n} of {need}"),
        new Chapter("ken_sunrise", "One sunrise", Task.DAWN, 1, Set.of(), "",
            "I don't sleep well. I haven't, for a long time. Stay up with me, once. Until the sun comes up. I'd like to see one sunrise with "
                + "someone, instead of alone.",
            "Dawn. Be with me when the sun comes up. That's all.",
            "Be with Ken when the sun comes up (at dawn)"));

    /** {@code key}'s chapters, in order (none for one who has none). */
    public static List<Chapter> of(String key) {
        return switch (key == null ? "" : key) {
            case "eto" -> ETO;
            case "touka" -> TOUKA;
            case "ken" -> KEN;
            default -> List.of();
        };
    }

    // ---------------------------------------------------------------- where they are

    /** How many chapters are closed (0 to all of them) in {@code bond}, for a courtship of {@code total} chapters. */
    public static int closed(QuestLog.Bond bond, int total) {
        int bits = (bond.flags() & BITS) >>> SHIFT;
        if (bits == 0 && (bond.flags() & LEGACY) != 0) {
            // The errands of before: all three done, they were waiting to ask; else one chapter for each done.
            if (bond.has(EtoRules.FLOWER_DONE)) {
                return total;
            }
            bits = (bond.has(EtoRules.BOOKS_DONE) ? 1 : 0) + (bond.has(EtoRules.HUNT_DONE) ? 1 : 0);
        }
        return Math.max(0, Math.min(total, bits));
    }

    /** {@code bond} as this courtship keeps it: the errands' flags read as chapters (their count, meaningless now, from 0). */
    public static QuestLog.Bond migrate(QuestLog.Bond bond, int total) {
        if ((bond.flags() & LEGACY) == 0) {
            return bond;
        }
        int closed = closed(bond, total);
        QuestLog.Bond cleared = new QuestLog.Bond(bond.regard(), bond.chatted(), bond.flags() & ~LEGACY & ~BITS | Math.min(7, closed) << SHIFT,
            bond.favours());
        return EtoRules.count(cleared, 0);
    }

    /** True once begun and not every chapter is closed. */
    public static boolean on(QuestLog.Bond bond, int total) {
        return bond.has(EtoRules.LINE_ON) && !bond.has(EtoRules.DATING) && closed(bond, total) < total;
    }

    /** The chapter on for {@code key} (null: not begun, or all closed). */
    public static Chapter chapter(String key, QuestLog.Bond bond) {
        List<Chapter> all = of(key);
        if (!on(bond, all.size())) {
            return null;
        }
        return all.get(closed(bond, all.size()));
    }

    /** True if the chapter on is done and its moment waits to be played. */
    public static boolean ready(String key, QuestLog.Bond bond) {
        Chapter chapter = chapter(key, bond);
        return chapter != null && EtoRules.count(bond) >= chapter.need();
    }

    /** Begun: the first chapter on, nothing done of it. */
    public static QuestLog.Bond begin(QuestLog.Bond bond) {
        return EtoRules.count(new QuestLog.Bond(bond.regard(), bond.chatted(), (bond.flags() | EtoRules.LINE_ON) & ~LEGACY & ~BITS, bond.favours()), 0);
    }

    /** {@code amount} more done on the chapter on for {@code key}, if it is a {@code task} (never past what it needs). */
    public static QuestLog.Bond progress(String key, QuestLog.Bond bond, Task task, int amount) {
        Chapter chapter = chapter(key, bond);
        if (chapter == null || chapter.task() != task || amount <= 0) {
            return bond;
        }
        return EtoRules.count(bond, Math.min(chapter.need(), EtoRules.count(bond) + amount));
    }

    /** Its moment played: the chapter closed, the next on (nothing done of it). Only a ready chapter closes. */
    public static QuestLog.Bond close(String key, QuestLog.Bond bond) {
        List<Chapter> all = of(key);
        if (!ready(key, bond)) {
            return bond;
        }
        int closed = closed(bond, all.size()) + 1;
        return EtoRules.count(new QuestLog.Bond(bond.regard(), bond.chatted(), bond.flags() & ~LEGACY & ~BITS | Math.min(7, closed) << SHIFT,
            bond.favours()), 0);
    }

    /** How many of a {@link Task#GIVE}'s items a hand-over of {@code held} takes now (0: it isn't one, or it is done). */
    public static int takes(String key, QuestLog.Bond bond, int held) {
        Chapter chapter = chapter(key, bond);
        if (chapter == null || chapter.task() != Task.GIVE || held <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(held, chapter.need() - EtoRules.count(bond)));
    }

    /** True if {@code item} (an item id) is what the chapter on for {@code key} asks to be handed over. */
    public static boolean wants(String key, QuestLog.Bond bond, String item) {
        Chapter chapter = chapter(key, bond);
        return chapter != null && chapter.task() == Task.GIVE && item != null && chapter.items().contains(item);
    }

    // ---------------------------------------------------------------- its words

    /** {@code text} with the chapter's counts in it ({@code done} so far). */
    public static String fill(String text, Chapter chapter, int done) {
        int step = STEP_SECONDS;
        return text.replace("{n}", Integer.toString(done)).replace("{need}", Integer.toString(chapter.need()))
            .replace("{mm}", minutes(chapter.need() * step)).replace("{m}", minutes(done * step));
    }

    /** Seconds as minutes ("1", "1.5", "2"). */
    static String minutes(int seconds) {
        int halves = Math.round(seconds / 30.0F);
        return halves % 2 == 0 ? Integer.toString(halves / 2) : (halves / 2) + ".5";
    }

    /** What the companion says asked about the chapter on: its ask if nothing is done yet, else how far along. */
    public static String said(Chapter chapter, int done) {
        return fill(done <= 0 || chapter.task() == Task.HIGH || chapter.task() == Task.DAWN || chapter.task() == Task.DEEP ? chapter.ask()
            : chapter.progress(), chapter, done);
    }

    /** The journal's name for the chapter on: "Chapter 2 of 5: A walk after dark" ("" past the last). */
    public static String journal(String key, int closed, int done) {
        List<Chapter> all = of(key);
        if (closed < 0 || closed >= all.size()) {
            return "";
        }
        Chapter chapter = all.get(closed);
        return "Chapter " + (closed + 1) + " of " + all.size() + ": " + chapter.title() + (done >= chapter.need() ? " (done)" : "");
    }

    /** The journal's line for what the chapter on asks, done so far (and once it's done, to go and see them). */
    public static String task(String key, int closed, int done) {
        List<Chapter> all = of(key);
        if (closed < 0 || closed >= all.size()) {
            return "";
        }
        Chapter chapter = all.get(closed);
        return done >= chapter.need() ? "Done: go and see " + Persona.of(key).her() + "." : fill(chapter.journal(), chapter, done) + ".";
    }

    /** The talk screen's words for handing over the chapter's items. */
    public static String handOver(Chapter chapter) {
        return "Here. What you asked for. (" + chapter.itemName() + " in your hand)";
    }

    // ---------------------------------------------------------------- dates (once they are together)

    /** The sweet page's answer that opens the dates. */
    public static final String DATES = "Let's go somewhere. (a date)";

    /** What {@code key} says offering where to go. */
    public static String datesAsked(String key, int seed) {
        String[] lines = switch (key == null ? "" : key) {
            case "eto" -> new String[] {"A date? With me? ...Where are you taking me, then? Choose well. I'll write about it.",
                "Out? Together? In public? How scandalous. Pick somewhere.", "Mm. Somewhere with you. Anywhere's fine. Pick, before I pick for us."};
            case "touka" -> new String[] {"A date. Right. Fine. Where? ...Don't say the cafe, I work there.",
                "You want to go out? With me? ...Okay. Where. Quickly, before I change my mind.", "Fine. One date. Where are we going?"};
            case "ken" -> new String[] {"A date? I'd love that. Where would you like to go?", "Somewhere with you? ...Yes. Anywhere. You choose.",
                "I'm not very good at dates. But I'd like to try. With you."};
            default -> new String[] {"Where to?"};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** What {@code key} says asked for a second date in a day. */
    public static String datedToday(String key, int seed) {
        String[] lines = switch (key == null ? "" : key) {
            case "eto" -> new String[] {"We already went out today. Greedy. ...Tomorrow. I'll think of somewhere worse.",
                "One date a day, or I'll run out of things to write about you."};
            case "touka" -> new String[] {"We went out already. Twice in one day? People will talk. ...Tomorrow.", "Tomorrow. I have a shift. And homework. And you."};
            case "ken" -> new String[] {"We already went out today. ...Tomorrow? I'll be looking forward to it all night.",
                "Tomorrow. I want to have something to look forward to."};
            default -> new String[] {"Tomorrow."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    // ---------------------------------------------------------------- on the talk screen

    /**
     * The first page's courtship answers for {@code key} ({@code romantic}: whether it would date them at all): before it begins, once
     * they are close enough, "Need anything done?"; on a chapter, what they can say about it (the items held out, the task done, or how
     * it's going); every chapter closed, "Go out with me."
     */
    public static void options(String key, EtoTalk.Context c, boolean romantic, List<EtoTalk.Option> out) {
        if (EtoVoice.of(c).together() || !c.mayDate() || !romantic) {
            return;
        }
        switch (c.step()) {
            case NONE -> {
                if (c.regard() >= BEGIN_AT) {
                    out.add(EtoTalk.Option.of(NpcTalk.Choice.FAVOUR));
                }
            }
            case COURTING -> {
                Chapter chapter = c.chapter(key);
                if (chapter != null) {
                    int done = c.count();
                    String label = done >= chapter.need() ? "I did what you asked."
                        : chapter.task() == Task.GIVE && c.held() > 0 ? handOver(chapter) : done > 0 ? "About what you asked..." : "";
                    out.add(new EtoTalk.Option(NpcTalk.Choice.FAVOUR, label, ""));
                }
            }
            case ASK -> out.add(EtoTalk.Option.of(NpcTalk.Choice.ASK_OUT));
            default -> {
            }
        }
    }

    /**
     * Its answer to "Need anything done?" (and what is said about a chapter after): the first chapter's ask as the courtship begins
     * ({@link EtoTalk.Effect#BEGIN}); the items held out taken, or the task done, and its moment played ({@link EtoTalk.Effect#COURT}: the
     * line here is what it says if the hand-over leaves some still to bring); how it's going; the rest in its own words ({@code notYet}
     * before they are close enough, {@code asking} once every chapter is closed, {@code dating}, {@code taken} by someone else,
     * {@code married} to someone else).
     */
    public static EtoTalk.Reply favour(String key, EtoTalk.Context c, String notYet, String asking, String dating, String taken, String married) {
        if (!c.mayDate()) {
            return new EtoTalk.Reply(c.claimed() ? EtoLines.fill(c, taken) : married, 0, EtoTalk.Effect.NONE);
        }
        List<Chapter> all = of(key);
        return switch (c.step()) {
            case NONE -> c.regard() >= BEGIN_AT && !all.isEmpty() ? new EtoTalk.Reply(fill(all.get(0).ask(), all.get(0), 0), 0, EtoTalk.Effect.BEGIN)
                : new EtoTalk.Reply(notYet, 0, EtoTalk.Effect.NONE);
            case ASK -> new EtoTalk.Reply(asking, 0, EtoTalk.Effect.NONE);
            case DATING -> new EtoTalk.Reply(dating, 0, EtoTalk.Effect.NONE);
            default -> {
                Chapter chapter = c.chapter(key);
                if (chapter == null) {
                    yield new EtoTalk.Reply(asking, 0, EtoTalk.Effect.NONE);
                }
                int done = c.count();
                if (done >= chapter.need()) {
                    yield new EtoTalk.Reply("", 0, EtoTalk.Effect.COURT);
                }
                if (chapter.task() == Task.GIVE && c.held() > 0) {
                    int after = done + Math.min(c.held(), chapter.need() - done);
                    yield new EtoTalk.Reply(fill(chapter.progress(), chapter, after), 0, EtoTalk.Effect.COURT);
                }
                yield new EtoTalk.Reply(said(chapter, done), 0, EtoTalk.Effect.NONE);
            }
        };
    }
}
