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

    public static final float HEALTH = 150.0F;
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

    /** The three lessons, in the order she teaches them: the quest each opens, the gate it leads to, the regard it waits for. */
    public enum Lesson {
        KICK("eto_kick", Gate.EYES_KICK, 20, TAUGHT_KICK),
        SOAR("eto_soar", Gate.EYES_SOAR, 40, TAUGHT_SOAR),
        WITNESS("eto_witness", Gate.EYES_WITNESS, 60, TAUGHT_WITNESS);

        public final String quest;
        public final Gate gate;
        public final int regard;
        public final int flag;

        Lesson(String quest, Gate gate, int regard, int flag) {
            this.quest = quest;
            this.gate = gate;
            this.regard = regard;
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

    /** What she makes of a book or a flower held out to her (by item id). */
    static final Set<String> BOOKS = Set.of("minecraft:book", "minecraft:writable_book", "minecraft:written_book", "minecraft:enchanted_book",
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
    /** She stops this close to them; farther off than {@link #KISS_AWAY} while she waits, they left her hanging. */
    public static final double KISS_NEAR = 1.3;
    public static final double KISS_AWAY = 4.0;
    /** Left hanging, she ignores them this long (game ticks: five minutes). */
    public static final long IGNORE_TICKS = 6000L;

    /** Where a kiss is. */
    public enum Kiss {
        WAIT, BLUSH, KISSED, HANGING
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
        if (now == Kiss.BLUSH) {
            if (!shift) {
                return Kiss.KISSED;
            }
            if (distance > KISS_AWAY) {
                return Kiss.HANGING;
            }
            return waited >= KISS_HOLD_MOST ? Kiss.KISSED : Kiss.BLUSH;
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

    /** The next lesson she has to teach (null: all three taught). */
    public static Lesson next(QuestLog.Bond bond) {
        for (Lesson lesson : Lesson.values()) {
            if (!bond.has(lesson.flag)) {
                return lesson;
            }
        }
        return null;
    }

    /** Why she would not teach {@code lesson} now, or null if she will. {@code beforeDone}: the lesson before it practised (its quest done). */
    public static Refusal refusal(Lesson lesson, int regard, boolean ghoul, boolean beforeDone) {
        if (lesson == null) {
            return Refusal.ALL_TAUGHT;
        }
        if (!ghoul) {
            return Refusal.HUMAN;
        }
        if (lesson.before() != null && !beforeDone) {
            return Refusal.PRACTISE;
        }
        return regard < lesson.regard ? Refusal.NOT_CLOSE : null;
    }

    /** Why a lesson is refused. */
    public enum Refusal {
        ALL_TAUGHT, HUMAN, PRACTISE, NOT_CLOSE
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

    /** Killed by them: she remembers nothing kind of them, only that they are her enemy (her lessons and errands with it). */
    public static QuestLog.Bond killedBy(QuestLog.Bond bond) {
        return new QuestLog.Bond(NpcBonds.LEAST, bond.chatted(), bond.flags() & NpcBonds.MET, 0);
    }

    // ---------------------------------------------------------------- what she is given

    public static boolean book(String item) {
        return item != null && BOOKS.contains(item);
    }

    public static boolean flower(String item) {
        return item != null && FLOWERS.contains(item);
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

    // ---------------------------------------------------------------- her fight

    /** What she does next in a fight. */
    public enum Move {
        NONE, STRIKE, BLITZ, PUNCH, KICK, FLIP_FORWARD, FLIP_BACK;

        public static Move byOrdinal(int ordinal) {
            Move[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }
    }

    /** Reach and timing (ticks) of each move; damage before she transforms. */
    public static final double STRIKE_REACH = 2.0;
    public static final double BLITZ_FROM = 3.0;
    public static final double BLITZ_TO = 7.0;
    public static final double CLOSE_REACH = 2.6;
    public static final double FLIP_IN_FROM = 7.0;
    public static final double FLIP_IN_TO = 12.0;
    public static final int STRIKE_EVERY = 12;
    public static final int BLITZ_EVERY = 50;
    public static final int PUNCH_EVERY = 110;
    public static final int KICK_EVERY = 70;
    public static final int FLIP_EVERY = 60;
    public static final int BLITZ_TICKS = 6;
    public static final int PUNCH_WINDUP = 12;
    public static final int PUNCH_TICKS = 20;
    public static final int KICK_TICKS = 10;
    public static final float STRIKE_DAMAGE = 5.0F;
    public static final float BLITZ_DAMAGE = 7.0F;
    public static final float PUNCH_DAMAGE = 14.0F;
    public static final float KICK_DAMAGE = 8.0F;
    /** Below this share of her health she transforms: her eye open, faster, harder. */
    public static final float TRANSFORM_AT = 0.5F;
    public static final float TRANSFORM_SPEED = 1.3F;
    public static final float TRANSFORM_POWER = 1.35F;
    /** Out of a fight this long (ticks), she heals this much a second. */
    public static final long CALM_TICKS = 100L;
    public static final float HEAL_PER_SECOND = 2.0F;

    /** When each move is ready again (game ticks; anything at or before {@code now} is ready). */
    public record Ready(long strike, long blitz, long punch, long kick, long flip) {
        public static final Ready NOW = new Ready(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);

        /** As now, {@code move} used at {@code now}. */
        public Ready used(Move move, long now) {
            return switch (move) {
                case STRIKE -> new Ready(now + STRIKE_EVERY, this.blitz, this.punch, this.kick, this.flip);
                case BLITZ -> new Ready(now + STRIKE_EVERY, now + BLITZ_EVERY, this.punch, this.kick, this.flip);
                case PUNCH -> new Ready(now + STRIKE_EVERY + PUNCH_TICKS, this.blitz, now + PUNCH_EVERY, this.kick, this.flip);
                case KICK -> new Ready(now + STRIKE_EVERY, this.blitz, this.punch, now + KICK_EVERY, this.flip);
                case FLIP_FORWARD, FLIP_BACK -> new Ready(this.strike, this.blitz, this.punch, this.kick, now + FLIP_EVERY);
                default -> this;
            };
        }
    }

    /**
     * Her pick (pure): far but in sight, a front flip in; at blitz range, the Speed Blitz; up close the Big Punch on something tough
     * or when she is transformed, the Speed Kick to launch it, else a plain strike, and hurt and pressed, a back flip clear. {@code roll}
     * in [0, 1) keeps her from being too predictable; {@code tough}: the target has a lot of health left.
     */
    public static Move choose(double distance, long now, Ready ready, boolean grounded, boolean sees, float health, boolean tough, double roll) {
        boolean flip = grounded && now >= ready.flip();
        if (distance > FLIP_IN_FROM && distance < FLIP_IN_TO && flip && sees) {
            return Move.FLIP_FORWARD;
        }
        if (distance >= BLITZ_FROM && distance <= BLITZ_TO && now >= ready.blitz() && sees) {
            return Move.BLITZ;
        }
        if (distance > CLOSE_REACH) {
            return Move.NONE;
        }
        boolean transformed = health < TRANSFORM_AT;
        if (flip && transformed && roll < 0.2) {
            return Move.FLIP_BACK;
        }
        if (now >= ready.punch() && (tough || transformed || roll < 0.35)) {
            return Move.PUNCH;
        }
        if (now >= ready.kick() && roll < 0.7) {
            return Move.KICK;
        }
        return distance <= STRIKE_REACH + 0.3 && now >= ready.strike() ? Move.STRIKE : Move.NONE;
    }

    /** What a blow of hers deals: {@code base}, more once she has transformed. */
    public static float damage(float base, float health) {
        return health < TRANSFORM_AT ? base * TRANSFORM_POWER : base;
    }
}
