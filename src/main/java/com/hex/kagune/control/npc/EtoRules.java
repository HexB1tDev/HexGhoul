package com.hex.kagune.control.npc;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.QuestLog;
import java.util.Set;

/**
 * 1.3.0: Eto, who lives in the Library (pure, tested). One of her, one bond with each player ({@link #KEY} in their quest log, a ghoul's
 * or a human's), kept as any ghoul NPC's is ({@link QuestLog.Bond}): her regard, the day they last chatted, flags (her lessons, how far
 * along her own errands are, whether they are dating), and in the bond's favour count her own word ({@link #count} for the errand on,
 * {@link #giftDay} for the last gift she took). She teaches a ghoul the eye-mode fight's three gates ({@link Lesson}), asks a player she
 * adores ({@link NpcBonds#MOST}) for three things ({@link Step}), and once they are dating she can make them one-eyed like her
 * ({@link #GRANT}). Her kakugan opens at night ({@link #night}) and when she fights.
 */
public final class EtoRules {
    /** Her bond's key in every player's quest log (never forgotten), and her name. */
    public static final String KEY = "eto";
    public static final String NAME = "Eto";
    /** Whether a player may wear one kakugan: kept in their profile root once she has made them like her (or they already were). */
    public static final String GRANT = "HexEtoGrant";

    /** 1.3.2: stacked: twice the health she had, and her skin hard as armour. */
    public static final float HEALTH = 300.0F;
    public static final double ARMOR = 14.0;
    /** 1.3.1: how much smaller than a player she is: drawn and boxed (1.3.2: smaller again, a head under most players). */
    public static final float SIZE = 0.86F;

    /** 1.3.1: her bust's bounce, a damped spring (per tick squared, per tick) lagging her body's height, and at most how far it lags. */
    public static final double JIGGLE_STIFF = 0.9;
    public static final double JIGGLE_DAMP = 0.35;
    public static final double JIGGLE_MOST = 0.075;

    /**
     * Her bust's bounce carried on over {@code ticks} as her body moved {@code moved} blocks up ({@code state}: how far it lags her body,
     * blocks, and how fast that changes): it stays where it was as she moves, then springs back, overshooting and settling. A move of
     * more than a block (a teleport) starts it still.
     */
    public static void jiggle(double[] state, double moved, double ticks) {
        if (Math.abs(moved) > 1.0 || !(ticks > 0.0)) {
            if (Math.abs(moved) > 1.0) {
                state[0] = 0.0;
                state[1] = 0.0;
            }
            return;
        }
        state[0] = Math.max(-JIGGLE_MOST, Math.min(JIGGLE_MOST, state[0] - moved));
        int steps = (int)Math.ceil(Math.min(ticks, 4.0) / 0.25);
        double h = Math.min(ticks, 4.0) / steps;
        for (int i = 0; i < steps; i++) {
            state[1] += (-JIGGLE_STIFF * state[0] - JIGGLE_DAMP * state[1]) * h;
            state[0] = Math.max(-JIGGLE_MOST, Math.min(JIGGLE_MOST, state[0] + state[1] * h));
        }
    }

    /** How far (model pixels, up) her bust's tip is drawn off its rest for a lag of {@code lag} blocks. */
    public static float jigglePixels(double lag) {
        return (float)(lag * 16.0);
    }
    /** How far from home she wanders (blocks), how far she chases before she goes back, how far off she is put back home. */
    public static final int HOME_RADIUS = 4;
    public static final int LEASH = HOME_RADIUS + 3;
    public static final double LOST = 16.0;

    /** Her eye: open through the night (the day's ticks), fading in and out over this many ticks. */
    public static final long NIGHT_FROM = 12542L;
    public static final long NIGHT_TO = 23460L;
    public static final float FADE_TICKS = 20.0F;

    /** What each thing does to her regard. */
    public static final int WHO = NpcBonds.WHO;
    public static final int CHAT = NpcBonds.CHAT;
    public static final int GIFT = 6;
    public static final int LESSON_DONE = 10;
    public static final int INSULT = NpcBonds.INSULT;
    public static final int BLOW = NpcBonds.BLOW;

    /** Her own flags, past {@link NpcBonds}'s (MET 1, BEFRIENDED 2, ASKED 4, ALLY 8). */
    public static final int TAUGHT_KICK = 16;
    public static final int TAUGHT_SOAR = 32;
    public static final int TAUGHT_WITNESS = 64;
    public static final int LINE_ON = 128;
    public static final int BOOKS_DONE = 256;
    public static final int HUNT_DONE = 512;
    public static final int FLOWER_DONE = 1024;
    public static final int DATING = 2048;
    /** 1.3.2: her last lesson taught (its quest, {@code eto_one_eye}, is open). */
    public static final int TAUGHT_TRIAL = 4096;

    /**
     * Her lessons, in the order she teaches them: the quest each opens and the gate it leads to (1.3.2: to anyone who isn't her enemy;
     * no friendship needed). The last, her trial, opens no gate: done, she makes them one-eyed.
     */
    public enum Lesson {
        KICK("eto_kick", Gate.EYES_KICK, TAUGHT_KICK),
        SOAR("eto_soar", Gate.EYES_SOAR, TAUGHT_SOAR),
        WITNESS("eto_witness", Gate.EYES_WITNESS, TAUGHT_WITNESS),
        TRIAL("eto_one_eye", null, TAUGHT_TRIAL);

        public final String quest;
        public final Gate gate;
        public final int flag;

        Lesson(String quest, Gate gate, int flag) {
            this.quest = quest;
            this.gate = gate;
            this.flag = flag;
        }

        /** The lesson before it, or null for the first. */
        public Lesson before() {
            return this.ordinal() == 0 ? null : values()[this.ordinal() - 1];
        }
    }

    /** Her errands at {@link NpcBonds#MOST}, in order, and how many of each: then the player may ask her out. */
    public enum Step {
        NONE(0), BOOKS(3), HUNT(10), FLOWER(1), ASK(0), DATING(0);

        public final int need;

        Step(int need) {
            this.need = need;
        }
    }

    /** What she makes of a book or a flower held out to her (by item id). 1.3.2: a written book is a letter ({@link #letter}), not a book. */
    static final Set<String> BOOKS = Set.of("minecraft:book", "minecraft:writable_book", "minecraft:enchanted_book",
        "minecraft:knowledge_book");
    static final Set<String> FLOWERS = Set.of("minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium",
        "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy",
        "minecraft:cornflower", "minecraft:lily_of_the_valley", "minecraft:wither_rose", "minecraft:torchflower", "minecraft:sunflower", "minecraft:lilac",
        "minecraft:rose_bush", "minecraft:peony", "minecraft:pitcher_plant", "minecraft:pink_petals");

    private EtoRules() {
    }

    // ---------------------------------------------------------------- her eye

    /** True through the night ({@code dayTime}: the level's day time, any day). */
    public static boolean night(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= NIGHT_FROM && time <= NIGHT_TO;
    }

    /** How open her eye is (0..1), {@code since} ticks after it was last told to open ({@code on}) or close, from {@code from}. */
    public static float eye(boolean on, float from, float since) {
        float step = Math.max(0.0F, since) / FADE_TICKS;
        return on ? Math.min(1.0F, from + step) : Math.max(0.0F, from - step);
    }

    /** She blinks once every this many ticks or so (a short shut, now and then twice), for this long. */
    public static final int BLINK_EVERY = 80;
    public static final int BLINK_TICKS = 3;

    /** True while her eyes are shut in a blink, {@code ticks} into her life ({@code seed}: hers, so no two blink together). */
    public static boolean blink(int seed, double ticks) {
        long cycle = (long)Math.floor(ticks / BLINK_EVERY);
        double into = ticks - cycle * BLINK_EVERY;
        int hash = (int)((cycle * 0x9E3779B1L + seed * 0x85EBCA6BL) >>> 7);
        int at = Math.floorMod(hash, BLINK_EVERY - 2 * BLINK_TICKS - 4);
        boolean twice = Math.floorMod(hash >>> 11, 5) == 0;
        return into >= at && into < at + BLINK_TICKS || twice && into >= at + BLINK_TICKS + 2 && into < at + 2 * BLINK_TICKS + 2;
    }

    // ---------------------------------------------------------------- a kiss

    /** She asks for one of her own at most this often per player (game ticks: twenty minutes), on a roll every few seconds. */
    public static final long KISS_ASK_EVERY = 24000L;
    public static final double KISS_ASK_CHANCE = 0.12;
    public static final double KISS_ASK_NEAR = 6.0;
    /** She leans in and waits this long; a shift shorter than {@link #KISS_TAP} is a peck, longer she blushes and holds it. */
    public static final int KISS_WAIT = 160;
    public static final int KISS_TAP = 8;
    public static final int KISS_HOLD_MOST = 600;
    /** 1.3.2: held this long (ticks), she pulls them in: her arms round their head. */
    public static final int KISS_PULL = 50;
    /** She stops this close to them; farther off than {@link #KISS_AWAY} while she waits, they left her hanging. */
    public static final double KISS_NEAR = 1.3;
    public static final double KISS_AWAY = 4.0;
    /** Left hanging, she ignores them this long (game ticks: five minutes). */
    public static final long IGNORE_TICKS = 6000L;

    /** Where a kiss is. */
    public enum Kiss {
        WAIT, BLUSH, KISSED, HANGING, PULL
    }

    /**
     * The kiss one tick on: {@code waited} ticks since she leaned in, the shift key down now and held {@code held} ticks so far,
     * {@code released} the ticks it was held if it came up this tick (0: it did not), {@code distance} how far off they are.
     */
    public static Kiss kiss(Kiss now, long waited, boolean shift, int held, int released, double distance) {
        if (now == Kiss.WAIT) {
            if (released > 0) {
                return Kiss.KISSED;
            }
            if (shift && held >= KISS_TAP) {
                return Kiss.BLUSH;
            }
            return distance > KISS_AWAY || waited >= KISS_WAIT && !shift ? Kiss.HANGING : Kiss.WAIT;
        }
        if (now == Kiss.BLUSH || now == Kiss.PULL) {
            if (!shift) {
                return Kiss.KISSED;
            }
            if (distance > KISS_AWAY) {
                return Kiss.HANGING;
            }
            if (waited >= KISS_HOLD_MOST) {
                return Kiss.KISSED;
            }
            return now == Kiss.BLUSH && held >= KISS_PULL ? Kiss.PULL : now;
        }
        return now;
    }

    // ---------------------------------------------------------------- her lessons

    /** The lesson whose quest is {@code quest}, or null. */
    public static Lesson lesson(String quest) {
        for (Lesson lesson : Lesson.values()) {
            if (lesson.quest.equals(quest)) {
                return lesson;
            }
        }
        return null;
    }

    /** The flag her bond must carry before {@code quest} opens: 0 for a quest that is not one of her lessons. */
    public static int lessonFlag(String quest) {
        Lesson lesson = lesson(quest);
        return lesson == null ? 0 : lesson.flag;
    }

    /** The next lesson she has to teach (null: all taught). */
    public static Lesson next(QuestLog.Bond bond) {
        return next(bond, lesson -> false);
    }

    /**
     * 1.3.2: the next lesson she has to teach, skipping any already known some other way ({@code known}: its quest done, or the gate
     * it leads to already open): nobody is sent on a quest they have already finished.
     */
    public static Lesson next(QuestLog.Bond bond, java.util.function.Predicate<Lesson> known) {
        for (Lesson lesson : Lesson.values()) {
            if (!bond.has(lesson.flag) && !known.test(lesson)) {
                return lesson;
            }
        }
        return null;
    }

    /**
     * Why she would not teach {@code lesson} now, or null if she will. {@code beforeDone}: the lesson before it practised (its quest
     * done, or known already). 1.3.2: no friendship is asked for: anyone she talks to can learn.
     */
    public static Refusal refusal(Lesson lesson, boolean ghoul, boolean beforeDone) {
        if (lesson == null) {
            return Refusal.ALL_TAUGHT;
        }
        if (!ghoul) {
            return Refusal.HUMAN;
        }
        return lesson.before() != null && !beforeDone ? Refusal.PRACTISE : null;
    }

    /** Why a lesson is refused. */
    public enum Refusal {
        ALL_TAUGHT, HUMAN, PRACTISE
    }

    // ---------------------------------------------------------------- 1.3.2: a human's way to one eye

    /** What she asks of a human who wants to be like her, in order, and how many of each: then the surgery. */
    public enum Pact {
        NONE(0), HUNT(12), GHOUL(1), MEAT(1), READY(0);

        public final int need;

        Pact(int need) {
            this.need = need;
        }

        public static Pact byOrdinal(long ordinal) {
            Pact[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[(int)ordinal] : NONE;
        }

        /** The step after this one is done. */
        public Pact then() {
            return this == READY ? READY : values()[this.ordinal() + 1];
        }
    }

    /** {@code amount} more of {@code step} with {@code count} done: the step and count after (the next step, its count from 0, once met). */
    public static long[] pact(Pact step, int count, Pact doing, int amount) {
        if (step != doing || step.need <= 0 || amount <= 0) {
            return new long[] {step.ordinal(), count};
        }
        int done = count + amount;
        return done >= step.need ? new long[] {step.then().ordinal(), 0} : new long[] {step.ordinal(), done};
    }

    // ---------------------------------------------------------------- her errands

    /** Where the player is in her errands. */
    public static Step step(QuestLog.Bond bond) {
        if (bond.has(DATING)) {
            return Step.DATING;
        }
        if (!bond.has(LINE_ON)) {
            return Step.NONE;
        }
        if (!bond.has(BOOKS_DONE)) {
            return Step.BOOKS;
        }
        if (!bond.has(HUNT_DONE)) {
            return Step.HUNT;
        }
        return bond.has(FLOWER_DONE) ? Step.ASK : Step.FLOWER;
    }

    /** How much of the errand on is done. */
    public static int count(QuestLog.Bond bond) {
        return bond.favours() & 0xFF;
    }

    /** The game day she last took a gift (-1: never; days wrap after 2^23, which is years of play). */
    public static long giftDay(QuestLog.Bond bond) {
        int day = bond.favours() >>> 8;
        return day == 0 ? -1L : day - 1L;
    }

    /** {@code bond} with {@code count} done on the errand on. */
    public static QuestLog.Bond count(QuestLog.Bond bond, int count) {
        return word(bond, Math.max(0, Math.min(0xFF, count)), bond.favours() >>> 8);
    }

    /** {@code bond} with a gift taken on game day {@code day}. */
    public static QuestLog.Bond gifted(QuestLog.Bond bond, long day) {
        return word(bond, count(bond), (int)(Math.floorMod(day, 0x7FFFFFL) + 1L));
    }

    private static QuestLog.Bond word(QuestLog.Bond bond, int count, int day) {
        return new QuestLog.Bond(bond.regard(), bond.chatted(), bond.flags(), (day & 0x7FFFFF) << 8 | count);
    }

    /** True if she took a gift from them on game day {@code day} already. */
    public static boolean giftedOn(QuestLog.Bond bond, long day) {
        return giftDay(bond) == Math.floorMod(day, 0x7FFFFFL);
    }

    /** The errands begun (at {@link NpcBonds#MOST}): books first. */
    public static QuestLog.Bond begin(QuestLog.Bond bond) {
        return count(bond.with(LINE_ON, true), 0);
    }

    /**
     * {@code amount} more done on {@code step} (it must be the errand on): the bond as it then is, the errand finished and the next one
     * on (its count from 0) once {@link Step#need} is reached.
     */
    public static QuestLog.Bond advance(QuestLog.Bond bond, Step step, int amount) {
        if (step(bond) != step || step.need <= 0 || amount <= 0) {
            return bond;
        }
        int done = count(bond) + amount;
        if (done < step.need) {
            return count(bond, done);
        }
        int flag = switch (step) {
            case BOOKS -> BOOKS_DONE;
            case HUNT -> HUNT_DONE;
            default -> FLOWER_DONE;
        };
        return count(bond.with(flag, true), 0);
    }

    /** Asked out once every errand is done: they are dating. */
    public static QuestLog.Bond date(QuestLog.Bond bond) {
        return step(bond) == Step.ASK ? bond.with(DATING, true) : bond;
    }

    /** The lesson flags, kept whatever else is forgotten (what was taught stays taught). */
    static final int TAUGHT = TAUGHT_KICK | TAUGHT_SOAR | TAUGHT_WITNESS | TAUGHT_TRIAL;

    /**
     * Killed by them: she remembers nothing kind of them, only that they are her enemy (her errands and any dating with it). 1.3.2: what
     * she taught stays taught: a lesson is never asked for twice.
     */
    public static QuestLog.Bond killedBy(QuestLog.Bond bond) {
        return new QuestLog.Bond(NpcBonds.LEAST, bond.chatted(), bond.flags() & (NpcBonds.MET | NpcBonds.ASKED | TAUGHT), 0);
    }

    // ---------------------------------------------------------------- what she is given

    public static boolean book(String item) {
        return item != null && BOOKS.contains(item);
    }

    public static boolean flower(String item) {
        return item != null && FLOWERS.contains(item);
    }

    /** 1.3.2: a letter (a written book): an apology, or just a letter. */
    public static boolean letter(String item) {
        return "minecraft:written_book".equals(item);
    }

    // ---------------------------------------------------------------- one eye

    /** True on a path whose kakugan is one eye by nature (Pale's, Graft DNA's): it never needed her. */
    public static boolean oneEyedPath(KagunePath path) {
        return path == KagunePath.PALE || path == KagunePath.GRAFT;
    }

    /** True if a player first seen under 1.3.0 keeps one eye: they had already placed it so, on a path where it was a choice. */
    public static boolean grandfathered(boolean placed, KakuganEyes eyes, KagunePath path) {
        return placed && eyes.oneEyed() && !oneEyedPath(path);
    }

    /** The eyes a player is allowed: one kakugan only once she has granted it (or their path is one-eyed by nature): both otherwise. */
    public static KakuganEyes allowed(KakuganEyes eyes, boolean granted, KagunePath path) {
        return granted || oneEyedPath(path) || !eyes.oneEyed() ? eyes : eyes.withGhoul(KakuganEyes.BOTH);
    }

    // ---------------------------------------------------------------- her fight (1.3.4: the Owl DNA kit, the player's own moves)

    /**
     * What she does next in a fight: her Owl DNA moves, the very ones a Pale player throws ({@code PaleMove}): the Speed Blitz's
     * strikes chained (blinks, the backfist, the twin palm finisher), the Big Punch charged and let go (her blink in front of it; at the
     * full charge, landed, Witness This), the Speed Kick (gone, there, the heel through it), and her flips.
     */
    public enum Act {
        /** Run at it. */
        CHASE,
        /** Close enough to step in and strike. */
        STEP_IN,
        /** The next strike of the blitz. */
        STRIKE,
        /** Wind up the Big Punch. */
        PUNCH,
        /** The Speed Kick. */
        KICK,
        /** A front flip in over the gap, or a back flip clear. */
        FLIP_IN,
        FLIP_BACK,
        /** 1.3.6: the flesh out of her back raking across everything in front of her. */
        SWIPE,
        /** 1.3.6: the flesh reaching for it, holding it up, hauling it over her and flinging it. */
        GRAB,
        /** 1.3.6: the Flesh Hammer: the club grown round her raised leg, the axe kick slamming it down. */
        HAMMER,
        /** 1.3.6: Watch This: her arms out in a cross, two tentacles out of her back hunting on their own for ten seconds. */
        WATCH,
        /** 1.3.7: the Mutated Arm: her right arm turned to flesh and shot out at it, catching what it meets. */
        ARM,
        /** 1.3.8: the Owl, her kakuja, grown out of her: her last resort. */
        OWL,
        /** 1.3.8, in the Owl: its Rend, the four cuts of the giant blades, the last its jaws. */
        OWL_REND,
        /** 1.3.8, in the Owl: its Reaping Whirl, a whole turn with both giant arms out. */
        OWL_WHIRL,
        /** 1.3.8, in the Owl: its Pounce, the bound and both blades driven into the ground as it lands. */
        OWL_LEAP
    }

    /** The mastery her moves are thrown at (the most there is: her fists as hard as a master's). */
    public static final int MASTERY = 1000;
    /** Close enough to strike (blocks, centre to centre, less half its width); the front flip's range in; the kick's nearest. */
    public static final double STRIKE_REACH = 2.6;
    public static final double FLIP_IN_FROM = 8.0;
    public static final double FLIP_IN_TO = 13.0;
    public static final double KICK_FROM = 4.0;
    /** How often a flip, the Big Punch and the Speed Kick come back (ticks, before her phase cuts them down). */
    public static final int FLIP_EVERY = 50;
    public static final int PUNCH_EVERY = 70;
    public static final int KICK_EVERY = 80;

    /**
     * Her pick (pure). {@code distance}: to its near side; {@code sees} it; {@code grounded}; {@code health} her share left; when each
     * of her big moves is ready again; {@code roll} in [0, 1) so she is never quite predictable. The Speed Kick from range when it is
     * ready (always, once she is serious), the Big Punch from mid range or up close, a flip in over a gap, the blitz up close (a flip
     * clear now and then once she is serious), a step in from just out of reach, else she runs at it.
     */
    public static Act choose(double distance, boolean sees, boolean grounded, float health, long now, long punchReady, long kickReady, long flipReady,
                             double roll) {
        return choose(distance, sees, grounded, health, now, Ready.only(punchReady, kickReady, flipReady), roll);
    }

    /** 1.3.6: how often her flesh moves and Watch This come back (ticks, before her phase cuts them down). */
    public static final int SWIPE_EVERY = 70;
    public static final int GRAB_EVERY = 150;
    public static final int HAMMER_EVERY = 120;
    public static final int WATCH_EVERY = 520;
    /** 1.3.6: Watch This once something is this close (blocks); the grab from this far to as far as the flesh reaches on her; the hammer this close. */
    public static final double WATCH_FROM = 8.0;
    public static final double GRAB_FROM = 4.5;
    public static final double GRAB_TO = com.hex.kagune.control.pale.PaleRules.GRAB_RANGE * SIZE;
    public static final double HAMMER_TO = 3.2;
    /** 1.3.6: the swipe's reach on her (blocks): the flesh is as much smaller as she is. */
    public static final double SWIPE_TO = com.hex.kagune.control.pale.PaleRules.SWIPE_REACH * SIZE - 1.0;

    /** 1.3.6: when each of her moves is ready again (game ticks; {@code Long.MAX_VALUE}: not hers to throw). */
    public record Ready(long punch, long kick, long flip, long swipe, long grab, long hammer, long watch, long arm) {
        public static Ready only(long punch, long kick, long flip) {
            return new Ready(punch, kick, flip, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        }
    }

    /** 1.3.7: how often the Mutated Arm comes back (ticks, before her phase cuts it down), and from how far to how far she throws it. */
    public static final int ARM_EVERY = 180;
    public static final double ARM_FROM = 6.0;
    public static final double ARM_TO = com.hex.kagune.control.pale.ArmRules.TAP_REACH * SIZE * 0.9;

    /**
     * 1.3.8: the Owl, her kakuja and her last resort: below this share of her health she grows it (when it is back); in it she takes
     * this share of every blow; out of a fight this long (ticks) she sheds it; it comes back this long after she shed it.
     */
    public static final float OWL_AT = 0.25F;
    public static final float OWL_TAKEN = 0.5F;
    public static final long OWL_SHED_AFTER = 120L;
    public static final int OWL_EVERY = 1200;
    /** 1.3.8: how often its Reaping Whirl and its Pounce come back (ticks); the Pounce from this far to this far. */
    public static final int WHIRL_EVERY = 60;
    public static final int LEAP_EVERY = 50;
    public static final double LEAP_FROM = 4.5;
    public static final double LEAP_TO = 12.0;

    /** 1.3.8: whether she grows the Owl now: hurt far enough, not in it already, and it is back. */
    public static boolean owlNow(float health, boolean inOwl, long now, long owlReady) {
        return !inOwl && health < OWL_AT && now >= owlReady;
    }

    /**
     * 1.3.8: her pick in the Owl (pure): the Pounce onto it from mid range, the Reaping Whirl with more than one about her (or now and
     * then), the Rend up close (its four cuts chained), else after it.
     */
    public static Act chooseOwl(double distance, boolean sees, boolean grounded, long now, long whirlReady, long leapReady, int around, double roll) {
        if (sees && grounded && now >= leapReady && distance >= LEAP_FROM && distance <= LEAP_TO) {
            return Act.OWL_LEAP;
        }
        if (grounded && now >= whirlReady && distance <= com.hex.kagune.control.pale.OwlRules.WHIRL_REACH && (around >= 2 || roll < 0.25)) {
            return Act.OWL_WHIRL;
        }
        if (distance <= com.hex.kagune.control.pale.OwlRules.REND_REACH[0] - 0.4) {
            return Act.OWL_REND;
        }
        return Act.CHASE;
    }

    /** 1.3.7: whether she holds the Mutated Arm out to charge it (the maniac, mostly: whatever it caught crushed in the sphere). */
    public static boolean armHeld(Phase phase, double roll) {
        return phase == Phase.MANIAC ? roll < 0.75 : phase == Phase.SERIOUS && roll < 0.25;
    }

    /**
     * 1.3.6: her pick with the whole Owl DNA kit (pure). Once she is serious: Watch This the moment it is back with something close
     * (the tentacles hunt on their own while she fights on); the grab from mid range; the Flesh Hammer up close (the maniac most of
     * all). The swipe at any time, now and then when calm, often once serious. Otherwise as before: the kick, the Big Punch, the flips,
     * the blitz, a step in, the chase.
     */
    public static Act choose(double distance, boolean sees, boolean grounded, float health, long now, Ready ready, double roll) {
        Phase phase = Phase.of(health);
        boolean serious = phase != Phase.CALM;
        boolean maniac = phase == Phase.MANIAC;
        if (serious && sees && grounded && now >= ready.watch() && distance <= WATCH_FROM) {
            return Act.WATCH;
        }
        if (sees && grounded && now >= ready.kick() && distance >= KICK_FROM && distance <= com.hex.kagune.control.pale.PaleRules.KICK_RANGE
            && (serious || roll < 0.55)) {
            return Act.KICK;
        }
        if (serious && sees && grounded && now >= ready.grab() && distance >= GRAB_FROM && distance <= GRAB_TO && roll < (maniac ? 0.8 : 0.5)) {
            return Act.GRAB;
        }
        if (serious && sees && now >= ready.arm() && distance >= ARM_FROM && distance <= ARM_TO && roll < (maniac ? 0.5 : 0.3)) {
            return Act.ARM;
        }
        if (serious && sees && grounded && now >= ready.hammer() && distance <= HAMMER_TO && roll < (maniac ? 0.6 : 0.35)) {
            return Act.HAMMER;
        }
        if (sees && grounded && now >= ready.swipe() && distance >= 1.0 && distance <= SWIPE_TO && roll < (serious ? 0.45 : 0.15)) {
            return Act.SWIPE;
        }
        long punchReady = ready.punch();
        long flipReady = ready.flip();
        if (sees && grounded && now >= punchReady && (distance >= 2.0 && distance <= 10.0 && (serious || roll < 0.45)
            || distance < 2.0 && roll < (serious ? 0.5 : 0.3))) {
            return Act.PUNCH;
        }
        boolean flip = grounded && now >= flipReady;
        if (flip && sees && distance > FLIP_IN_FROM && distance < FLIP_IN_TO) {
            return Act.FLIP_IN;
        }
        if (distance <= STRIKE_REACH) {
            return flip && serious && roll > 0.9 ? Act.FLIP_BACK : Act.STRIKE;
        }
        return sees && distance <= com.hex.kagune.control.pale.PaleRules.STEP_IN_RANGE ? Act.STEP_IN : Act.CHASE;
    }

    /**
     * How long she winds the Big Punch up (ticks): calm, mostly a quick one, now and then the full charge; serious, the full charge
     * more often than not; maniac, always (a full charge that lands is Witness This).
     */
    public static int punchHold(Phase phase, double roll) {
        int full = com.hex.kagune.control.pale.PaleRules.PUNCH_CHARGE_TICKS;
        return switch (phase) {
            case MANIAC -> full;
            case SERIOUS -> roll < 0.65 ? full : 14 + (int)(roll * 12.0);
            default -> roll < 0.25 ? full : 8 + (int)(roll * 14.0);
        };
    }

    /**
     * 1.3.6: how long she lets the Flesh Hammer's club grow round her raised leg (ticks): anything from the leg just up to the club
     * fully grown; the maniac always lets it grow all the way (hers is True Anonymity's, grown on past that).
     */
    public static int hammerHold(Phase phase, double roll) {
        int raise = com.hex.kagune.control.pale.PaleRules.HAMMER_RAISE_TICKS;
        int grow = com.hex.kagune.control.pale.PaleRules.HAMMER_GROW_TICKS;
        return phase == Phase.MANIAC ? grow : raise + (int)Math.round(Math.max(0.0, Math.min(1.0, roll)) * (grow - raise));
    }

    /** A cooldown of {@code ticks}, cut down as she gets serious. */
    public static long cooldown(int ticks, Phase phase) {
        return Math.max(4L, Math.round(ticks * phase.cooldowns));
    }

    /** Below this share of her health she gets serious: her eye open, faster, harder ({@link Phase}). */
    public static final float TRANSFORM_AT = 0.6F;
    public static final float MANIAC_AT = 0.3F;

    /**
     * 1.3.2: how seriously she takes a fight, by what is left of her health. Calm, she plays with them. Serious, her eye opens, she
     * is faster, harder to hurt, and her moves come back sooner. Maniac, below a third, she laughs and stops holding anything back.
     */
    public enum Phase {
        CALM(1.0F, 1.0F, -1, -1, 1.1),
        SERIOUS(1.4F, 0.6F, 0, 0, 1.35),
        MANIAC(1.8F, 0.35F, 1, 1, 1.55);

        /** What her blows deal, times. */
        public final float power;
        /** Her moves' cooldowns, times. */
        public final float cooldowns;
        /** The speed and resistance she has (effect levels; -1 none). */
        public final int speed;
        public final int resistance;
        /** How fast she chases. */
        public final double chase;

        Phase(float power, float cooldowns, int speed, int resistance, double chase) {
            this.power = power;
            this.cooldowns = cooldowns;
            this.speed = speed;
            this.resistance = resistance;
            this.chase = chase;
        }

        public static Phase of(float health) {
            return health < MANIAC_AT ? MANIAC : health < TRANSFORM_AT ? SERIOUS : CALM;
        }
    }

    /** Out of a fight this long (ticks), she heals this much a second. */
    public static final long CALM_TICKS = 100L;
    public static final float HEAL_PER_SECOND = 2.0F;

    /** What a blow of hers deals: {@code base} (a Pale player's at her mastery), more the more serious she is ({@link Phase#power}). */
    public static float damage(float base, float health) {
        return base * Phase.of(health).power;
    }
}
