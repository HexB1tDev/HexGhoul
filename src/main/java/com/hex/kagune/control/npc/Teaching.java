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

    /** Every lesson, in the order each teacher teaches them. */
    public static final List<Lesson> ALL = List.of(
        new Lesson("touka_crystal", TOUKA, Gate.UKAKU_CRYSTAL, Kind.TALK, "Crystal Wings"),
        new Lesson("touka_gatling", TOUKA, Gate.UKAKU_GATLING, Kind.TALK, "Gatling"),
        new Lesson("touka_kakuja", TOUKA, Gate.UKAKU_KAKUJA, Kind.TALK, "The First Kakuja"),
        new Lesson("ken_centipede", KEN, Gate.RINKAKU_CENTIPEDE, Kind.TRIAL, "The Centipede"),
        new Lesson("ken_king", KEN, Gate.RINKAKU_FULL_KAKUJA, Kind.TALK, "The Full Kakuja"),
        new Lesson("ken_dragon", KEN, Gate.RINKAKU_DRAGON, Kind.TALK, "The Dragon"),
        new Lesson("ken_storm", KEN, Gate.UKAKU_FINAL_KAKUJA, Kind.TRIAL, "The Final Kakuja"));

    /**
     * What has to be open before a lesson's gate can be taught (the step before it on the path, whoever opened it), or null. Ken's
     * centipede follows the four tails (opened by their quests); Touka's crystals come first of all.
     */
    public static Gate before(Gate gate) {
        return switch (gate) {
            case UKAKU_GATLING -> Gate.UKAKU_CRYSTAL;
            case UKAKU_KAKUJA -> Gate.UKAKU_GATLING;
            case UKAKU_FINAL_KAKUJA -> Gate.UKAKU_KAKUJA;
            case RINKAKU_CENTIPEDE -> Gate.RINKAKU_FOUR;
            case RINKAKU_FULL_KAKUJA -> Gate.RINKAKU_CENTIPEDE;
            case RINKAKU_DRAGON -> Gate.RINKAKU_FULL_KAKUJA;
            default -> null;
        };
    }

    private Teaching() {
    }

    /** The lessons {@code teacher} teaches, in order. */
    public static List<Lesson> of(String teacher) {
        return ALL.stream().filter(lesson -> lesson.teacher().equals(teacher)).toList();
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
        for (Lesson lesson : of(teacher)) {
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
