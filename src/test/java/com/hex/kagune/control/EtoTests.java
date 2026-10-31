package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoLines;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.EtoTopics;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcMemory;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.UniqueNpc;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestEvent;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 1.3.0: Eto: her eye's night, her blinks, her lessons, her errands, a kiss, her talk, the one-eye grant, her fight. 1.3.2: whose she is,
 * her pact and trial, her topics, love and rings, what players and NPCs remember, one of her in a world, her moods, her fight's phases.
 */
public final class EtoTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        night();
        blink();
        lessons();
        errands();
        kiss();
        talk();
        grant();
        fight();
        poses();
        romance();
        memory();
        unique();
        moods();
        phases();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Eto tests: " + passed + " checks passed");
    }

    static void night() {
        check(!EtoRules.night(6000L) && !EtoRules.night(12000L) && !EtoRules.night(EtoRules.NIGHT_FROM - 1), "her eye stays shut by day and at dusk");
        check(EtoRules.night(EtoRules.NIGHT_FROM) && EtoRules.night(18000L) && EtoRules.night(EtoRules.NIGHT_TO), "and opens through the night");
        check(!EtoRules.night(EtoRules.NIGHT_TO + 1) && EtoRules.night(24000L * 7 + 18000L) && !EtoRules.night(-12000L), "any day, dawn shuts it");
        double[] bounce = {0.0, 0.0};
        double most = 0.0;
        double up = 0.42;
        for (int tick = 0; tick < 12; tick++) {
            EtoRules.jiggle(bounce, up, 1.0);
            up -= 0.08;
            most = Math.max(most, Math.abs(bounce[0]));
        }
        EtoRules.jiggle(bounce, 0.0, 1.0);
        check(most > 0.02 && most <= EtoRules.JIGGLE_MOST + 1.0E-9, "her bust lags a jump, never past its most");
        for (int tick = 0; tick < 60; tick++) {
            EtoRules.jiggle(bounce, 0.0, 1.0);
        }
        check(Math.abs(bounce[0]) < 0.002 && Math.abs(bounce[1]) < 0.002, "and settles once she stands still");
        EtoRules.jiggle(bounce, 30.0, 1.0);
        check(bounce[0] == 0.0 && bounce[1] == 0.0, "a teleport starts it still");
        check(EtoRules.eye(true, 0.0F, 0.0F) == 0.0F && EtoRules.eye(true, 0.0F, EtoRules.FADE_TICKS) == 1.0F
            && EtoRules.eye(true, 0.0F, EtoRules.FADE_TICKS / 2) > 0.4F && EtoRules.eye(true, 0.0F, EtoRules.FADE_TICKS / 2) < 0.6F,
            "it fades in over about a second");
        check(EtoRules.eye(false, 1.0F, EtoRules.FADE_TICKS) == 0.0F && EtoRules.eye(false, 0.5F, 0.0F) == 0.5F, "and out from wherever it was");
    }

    static void blink() {
        for (int seed : new int[] {0, 7, 12345, -99}) {
            int shut = 0;
            int runs = 0;
            boolean was = false;
            for (int t = 0; t < 20 * 60; t++) {
                boolean now = EtoRules.blink(seed, t);
                shut += now ? 1 : 0;
                runs += now && !was ? 1 : 0;
                was = now;
            }
            check(runs >= 12 && runs <= 25 && shut < 20 * 60 / 8, "she blinks now and then, briefly (seed " + seed + "): " + runs + " blinks, " + shut + " ticks shut");
        }
    }

    static void lessons() {
        QuestLog log = new QuestLog();
        for (EtoRules.Lesson lesson : EtoRules.Lesson.values()) {
            Quest quest = QuestBook.get(lesson.quest);
            check(quest != null && (lesson.gate == null ? quest.reward().gates().isEmpty() : quest.opens(lesson.gate))
                && EtoRules.lessonFlag(lesson.quest) == lesson.flag, lesson + ": its quest opens its gate (her trial, none)");
            check(log.state(quest, KagunePath.RINKAKU, 1000) == QuestLog.State.LOCKED, lesson + ": locked until she teaches it, whatever the mastery");
        }
        check(EtoRules.lessonFlag("light_feet") == 0 && QuestBook.openers(Gate.EYES_KICK).size() >= 3, "the old ways in stay; hers is one more");
        QuestLog.Bond bond = QuestLog.Bond.NONE;
        check(EtoRules.next(bond) == EtoRules.Lesson.KICK, "the Speed Kick first");
        check(EtoRules.refusal(EtoRules.Lesson.KICK, true, true) == null && EtoRules.refusal(EtoRules.Lesson.WITNESS, true, true) == null,
            "taught to anyone she talks to: no friendship needed");
        check(EtoRules.refusal(EtoRules.Lesson.KICK, false, true) == EtoRules.Refusal.HUMAN, "never to a human (a human has her pact)");
        check(EtoRules.refusal(EtoRules.Lesson.SOAR, true, false) == EtoRules.Refusal.PRACTISE, "not before the last one is practised");
        check(EtoRules.refusal(null, true, true) == EtoRules.Refusal.ALL_TAUGHT, "four lessons, no more");
        check(EtoRules.next(bond, lesson -> lesson == EtoRules.Lesson.KICK) == EtoRules.Lesson.SOAR
            && EtoRules.next(bond, lesson -> lesson != EtoRules.Lesson.TRIAL) == EtoRules.Lesson.TRIAL && EtoRules.next(bond, lesson -> true) == null,
            "a lesson already known some other way is never taught again");
        QuestLog elsewhere = new QuestLog();
        Quest other = null;
        for (Quest opener : QuestBook.openers(Gate.EYES_KICK)) {
            if (EtoRules.lesson(opener.id()) == null && opener.on(KagunePath.RINKAKU)) {
                other = opener;
            }
        }
        check(other != null, "another way to the Speed Kick");
        if (other != null) {
            elsewhere.complete(other);
            check(elsewhere.state(QuestBook.get("eto_kick"), KagunePath.RINKAKU, 0) == QuestLog.State.DONE,
                "its gate open another way, her lesson's quest is done: never asked of them");
            elsewhere.bond(EtoRules.KEY, QuestLog.Bond.NONE.with(EtoRules.TAUGHT_SOAR, true));
            check(elsewhere.state(QuestBook.get("eto_soar"), KagunePath.RINKAKU, 0) == QuestLog.State.OPEN, "and the one after it opens as if it were done");
        }
        log.bond(EtoRules.KEY, bond.with(EtoRules.TAUGHT_KICK, true).regard(20));
        Quest kick = QuestBook.get("eto_kick");
        Quest soar = QuestBook.get("eto_soar");
        check(log.state(kick, KagunePath.RINKAKU, 0) == QuestLog.State.OPEN, "taught, the lesson opens at any mastery");
        check(log.state(soar, KagunePath.RINKAKU, 0) == QuestLog.State.LOCKED, "the next stays locked");
        check(EtoRules.next(log.bond(EtoRules.KEY)) == EtoRules.Lesson.SOAR, "the next to teach is the soaring flips");
        for (int i = 0; i < 40; i++) {
            log.apply(new QuestEvent(Signal.STRIKE, 1, Set.of(Tags.EYES)), KagunePath.RINKAKU, 0);
        }
        for (int i = 0; i < 12; i++) {
            log.apply(new QuestEvent(Signal.FLIP, 1, Set.of()), KagunePath.RINKAKU, 0);
        }
        check(log.met(kick), "struck and flipped enough");
        log.complete(kick);
        check(log.open(Gate.EYES_KICK) && log.bond(EtoRules.KEY).regard() == 20 + EtoRules.LESSON_DONE, "done: the gate opens and she thinks better of them");
        for (int i = 0; i < 60; i++) {
            log.bond("npc-" + i, new QuestLog.Bond(1, 0L, 0, 0));
        }
        check(log.bond(EtoRules.KEY).regard() == 20 + EtoRules.LESSON_DONE && log.bonds().size() == NpcBonds.REMEMBERED, "her bond is never forgotten");
        QuestLog back = QuestLog.load(log.save());
        check(back.bond(EtoRules.KEY).has(EtoRules.TAUGHT_KICK) && back.state(kick, KagunePath.RINKAKU, 0) == QuestLog.State.DONE, "kept in the save");
    }

    static void errands() {
        QuestLog.Bond bond = new QuestLog.Bond(100, 0L, NpcBonds.MET, 0);
        check(EtoRules.step(bond) == EtoRules.Step.NONE, "nothing asked yet");
        bond = EtoRules.begin(bond);
        check(EtoRules.step(bond) == EtoRules.Step.BOOKS && EtoRules.count(bond) == 0, "books first");
        bond = EtoRules.advance(bond, EtoRules.Step.HUNT, 5);
        check(EtoRules.step(bond) == EtoRules.Step.BOOKS && EtoRules.count(bond) == 0, "only the errand on counts");
        bond = EtoRules.advance(EtoRules.gifted(bond, 12L), EtoRules.Step.BOOKS, 1);
        bond = EtoRules.advance(bond, EtoRules.Step.BOOKS, 1);
        check(EtoRules.step(bond) == EtoRules.Step.BOOKS && EtoRules.count(bond) == 2 && EtoRules.giftedOn(bond, 12L) && !EtoRules.giftedOn(bond, 13L),
            "two books, the gift day kept beside them");
        bond = EtoRules.advance(bond, EtoRules.Step.BOOKS, 1);
        check(EtoRules.step(bond) == EtoRules.Step.HUNT && EtoRules.count(bond) == 0 && EtoRules.giftedOn(bond, 12L), "three: then the hunt");
        for (int i = 0; i < 9; i++) {
            bond = EtoRules.advance(bond, EtoRules.Step.HUNT, 1);
        }
        check(EtoRules.step(bond) == EtoRules.Step.HUNT && EtoRules.count(bond) == 9, "nine kills");
        bond = EtoRules.advance(bond, EtoRules.Step.HUNT, 1);
        check(EtoRules.step(bond) == EtoRules.Step.FLOWER, "ten: then a flower");
        check(EtoRules.date(bond) == bond, "no asking her out before the flower");
        bond = EtoRules.advance(bond, EtoRules.Step.FLOWER, 1);
        check(EtoRules.step(bond) == EtoRules.Step.ASK, "then ask her");
        bond = EtoRules.date(bond);
        check(EtoRules.step(bond) == EtoRules.Step.DATING && bond.has(EtoRules.DATING) && bond.regard() == 100, "dating");
        QuestLog.Bond back = QuestLog.load(saved(bond)).bond(EtoRules.KEY);
        check(back.equals(bond), "her word survives the save");
        QuestLog.Bond killed = EtoRules.killedBy(bond);
        check(killed.regard() <= NpcBonds.ENEMY && !killed.has(EtoRules.DATING) && !killed.has(EtoRules.LINE_ON) && killed.has(NpcBonds.MET)
            && !EtoTalk.talks(killed.regard()), "killed by them: her enemy, all of it gone");
        check(EtoRules.killedBy(bond.with(EtoRules.TAUGHT_KICK, true).with(EtoRules.TAUGHT_SOAR, true)).has(EtoRules.TAUGHT_SOAR),
            "what she taught stays taught: no lesson asked for twice");
        check(EtoRules.book("minecraft:book") && !EtoRules.book("minecraft:written_book") && !EtoRules.book("minecraft:paper") && !EtoRules.book(null),
            "books are books");
        check(EtoRules.letter("minecraft:written_book") && !EtoRules.letter("minecraft:book"), "a written book is a letter");
        check(EtoRules.flower("minecraft:poppy") && EtoRules.flower("minecraft:cornflower") && !EtoRules.flower("minecraft:oak_sapling"), "flowers are flowers");
        long far = 9_000_000L;
        check(EtoRules.giftedOn(EtoRules.gifted(QuestLog.Bond.NONE, far), far) && EtoRules.giftDay(QuestLog.Bond.NONE) == -1L, "any day fits");
    }

    private static String saved(QuestLog.Bond bond) {
        QuestLog log = new QuestLog();
        log.bond(EtoRules.KEY, bond);
        return log.save();
    }

    static void kiss() {
        EtoRules.Kiss wait = EtoRules.Kiss.WAIT;
        check(EtoRules.kiss(wait, 5, false, 0, 0, 1.0) == EtoRules.Kiss.WAIT, "she waits");
        check(EtoRules.kiss(wait, 5, false, 0, 3, 1.0) == EtoRules.Kiss.KISSED, "a tap of shift: a kiss");
        check(EtoRules.kiss(wait, 5, true, EtoRules.KISS_TAP - 1, 0, 1.0) == EtoRules.Kiss.WAIT, "a short hold is still a tap so far");
        check(EtoRules.kiss(wait, 5, true, EtoRules.KISS_TAP, 0, 1.0) == EtoRules.Kiss.BLUSH, "held: she blushes");
        check(EtoRules.kiss(EtoRules.Kiss.BLUSH, 100, true, EtoRules.KISS_PULL - 1, 0, 1.0) == EtoRules.Kiss.BLUSH, "and stays while it is held");
        check(EtoRules.kiss(EtoRules.Kiss.BLUSH, 100, true, EtoRules.KISS_PULL, 0, 1.0) == EtoRules.Kiss.PULL
            && EtoRules.kiss(EtoRules.Kiss.PULL, 200, true, 300, 0, 1.0) == EtoRules.Kiss.PULL, "held long enough, she pulls them in, and holds on");
        check(EtoRules.kiss(EtoRules.Kiss.PULL, 200, false, 0, 300, 1.0) == EtoRules.Kiss.KISSED
            && EtoRules.kiss(EtoRules.Kiss.PULL, EtoRules.KISS_HOLD_MOST, true, 900, 0, 1.0) == EtoRules.Kiss.KISSED
            && EtoRules.kiss(EtoRules.Kiss.PULL, 200, true, 300, 0, EtoRules.KISS_AWAY + 1.0) == EtoRules.Kiss.HANGING, "let go, or too long: kissed; gone: hanging");
        check(EtoRules.kiss(EtoRules.Kiss.BLUSH, 100, false, 0, 100, 1.0) == EtoRules.Kiss.KISSED, "let go: kissed");
        check(EtoRules.kiss(EtoRules.Kiss.BLUSH, EtoRules.KISS_HOLD_MOST, true, 600, 0, 1.0) == EtoRules.Kiss.KISSED, "never forever");
        check(EtoRules.kiss(wait, EtoRules.KISS_WAIT, false, 0, 0, 1.0) == EtoRules.Kiss.HANGING, "nothing: left hanging");
        check(EtoRules.kiss(wait, 10, false, 0, 0, EtoRules.KISS_AWAY + 0.5) == EtoRules.Kiss.HANGING, "walked off: left hanging");
        check(EtoRules.KISS_ASK_EVERY == 24000L && EtoRules.IGNORE_TICKS > 0L, "she asks herself at most every twenty minutes");
        check(EtoGesture.KISS.eyesClosed && EtoGesture.KISS.held() && EtoGesture.BLUSH.blush && !EtoGesture.ANGRY.eyesClosed && !EtoGesture.WAVE.held()
            && EtoGesture.WAVE.over(EtoGesture.WAVE.ticks) && !EtoGesture.KISS.over(100000), "the kiss is held until it ends; the idle ones run out");
    }

    static EtoTalk.Context context(int regard, int flags, int count, boolean ghoul, boolean book, boolean flower, EtoRules.Lesson next, boolean granted) {
        return new EtoTalk.Context(regard, flags, count, ghoul, false, false, book, flower, next, true, granted, 3);
    }

    static void talk() {
        EtoTalk.Context stranger = context(0, 0, 0, false, false, false, EtoRules.Lesson.KICK, false);
        List<NpcTalk.Choice> choices = EtoTalk.choices(stranger);
        check(choices.contains(NpcTalk.Choice.WHO) && choices.contains(NpcTalk.Choice.CHAT) && choices.contains(NpcTalk.Choice.LEAVE)
            && choices.contains(NpcTalk.Choice.LESSON) && !choices.contains(NpcTalk.Choice.SWEET) && !choices.contains(NpcTalk.Choice.FAVOUR),
            "a human stranger: who, chat, her pact, leave; nothing sweet, no errands");
        check(EtoTalk.answer(stranger, NpcTalk.Choice.LESSON).effect() == EtoTalk.Effect.PACT, "a human asking to learn is told her price");
        EtoTalk.Context ghoul = context(25, 0, 0, true, true, false, EtoRules.Lesson.KICK, false);
        check(EtoTalk.choices(ghoul).contains(NpcTalk.Choice.LESSON) && EtoTalk.choices(ghoul).contains(NpcTalk.Choice.BOOK), "a ghoul with a book");
        check(EtoTalk.answer(ghoul, NpcTalk.Choice.LESSON).effect() == EtoTalk.Effect.TEACH, "she teaches at 25");
        check(EtoTalk.answer(context(10, 0, 0, true, false, false, EtoRules.Lesson.KICK, false), NpcTalk.Choice.LESSON).effect() == EtoTalk.Effect.TEACH,
            "and at 10: no friendship needed");
        check(EtoTalk.answer(ghoul, NpcTalk.Choice.BOOK).regard() == EtoRules.GIFT && EtoTalk.answer(ghoul, NpcTalk.Choice.BOOK).effect() == EtoTalk.Effect.BOOK,
            "a book raises her regard");
        EtoTalk.Context gifted = new EtoTalk.Context(25, 0, 0, true, true, true, true, false, null, true, false, 3);
        check(EtoTalk.answer(gifted, NpcTalk.Choice.BOOK).effect() == EtoTalk.Effect.NONE && EtoTalk.answer(gifted, NpcTalk.Choice.CHAT).regard() == 0,
            "one gift and one chat a day");
        EtoTalk.Context adored = context(100, 0, 0, false, false, false, null, false);
        check(EtoTalk.choices(adored).contains(NpcTalk.Choice.FAVOUR) && EtoTalk.answer(adored, NpcTalk.Choice.FAVOUR).effect() == EtoTalk.Effect.BEGIN,
            "at 100 she asks for her errands (a human too)");
        EtoTalk.Context books = context(100, EtoRules.LINE_ON, 2, false, true, false, null, false);
        check(EtoTalk.answer(books, NpcTalk.Choice.BOOK).effect() == EtoTalk.Effect.BOOK, "the third book counts even on a gifted day");
        EtoTalk.Context ask = context(100, EtoRules.LINE_ON | EtoRules.BOOKS_DONE | EtoRules.HUNT_DONE | EtoRules.FLOWER_DONE, 0, false, false, false, null, false);
        check(EtoTalk.choices(ask).contains(NpcTalk.Choice.ASK_OUT) && EtoTalk.answer(ask, NpcTalk.Choice.ASK_OUT).effect() == EtoTalk.Effect.DATE,
            "every errand done: ask her out");
        int dating = EtoRules.LINE_ON | EtoRules.BOOKS_DONE | EtoRules.HUNT_DONE | EtoRules.FLOWER_DONE | EtoRules.DATING;
        EtoTalk.Context date = context(100, dating, 0, false, false, false, null, false);
        List<NpcTalk.Choice> dated = EtoTalk.choices(date);
        List<NpcTalk.Choice> sweet = EtoTalk.sweet(date, "").choices();
        check(dated.contains(NpcTalk.Choice.SWEET) && !dated.contains(NpcTalk.Choice.INSULT) && dated.contains(NpcTalk.Choice.TEASE) && dated.size() <= 12
            && sweet.contains(NpcTalk.Choice.KISS) && sweet.contains(NpcTalk.Choice.HUG) && sweet.contains(NpcTalk.Choice.SURGERY) && sweet.size() <= 12,
            "dating: her sweet side (kiss, hug, the surgery) on a page of its own");
        check(EtoTalk.answer(date, NpcTalk.Choice.KISS).effect() == EtoTalk.Effect.KISS && EtoTalk.answer(date, NpcTalk.Choice.HUG).effect() == EtoTalk.Effect.HUG,
            "a kiss and a hug from the menu");
        check(EtoTalk.answer(date, NpcTalk.Choice.SURGERY).effect() == EtoTalk.Effect.OFFER && EtoTalk.offer().contains(NpcTalk.Choice.ACCEPT)
            && EtoTalk.answer(date, NpcTalk.Choice.ACCEPT).effect() == EtoTalk.Effect.SURGERY, "the surgery is offered, then done");
        EtoTalk.Context done = context(100, dating, 0, true, false, false, null, true);
        check(!EtoTalk.sweet(done, "").choices().contains(NpcTalk.Choice.SURGERY) && EtoTalk.answer(done, NpcTalk.Choice.SURGERY).effect() == EtoTalk.Effect.NONE,
            "once only");
        check(EtoTalk.answer(context(-30, 0, 0, false, false, false, null, false), NpcTalk.Choice.INSULT).effect() == EtoTalk.Effect.ANGER
            && EtoTalk.answer(stranger, NpcTalk.Choice.INSULT).effect() == EtoTalk.Effect.CLOSE, "an insult: cold, or a fight");
        check(!EtoTalk.talks(NpcBonds.ENEMY) && EtoTalk.talks(NpcBonds.ENEMY + 1), "an enemy is not talked to");
        int most = 0;
        for (int flags : new int[] {0, dating}) {
            for (boolean book : new boolean[] {false, true}) {
                most = Math.max(most, EtoTalk.choices(new EtoTalk.Context(100, flags, 0, true, false, false, book, book, EtoRules.Lesson.KICK, true, false, 0))
                    .size());
            }
        }
        check(most <= 12, "never more answers than the talk screen sends: " + most);
        for (NpcTalk.Choice choice : NpcTalk.Choice.values()) {
            check(EtoTalk.answer(date, choice) != null, "an answer to everything: " + choice);
        }
        check(NpcTalk.Choice.byOrdinal(11) == NpcTalk.Choice.LEAVE && NpcTalk.Choice.byOrdinal(18) == NpcTalk.Choice.SURGERY
            && NpcTalk.Choice.values().length == 35 && NpcTalk.Choice.SAY_3.say() == 2 && NpcTalk.Choice.LEAVE.say() == -1,
            "new choices at the end, the old ordinals kept");
        claimed();
        student();
        topics();
        typing();
    }

    /** A full context, as the server makes one. */
    static EtoTalk.Context full(int regard, int flags, boolean ghoul, Romance.Stage stage, String claimedBy, long dayTime, EtoMood mood, boolean ring,
                                EtoRules.Pact pact, boolean trialDone, boolean granted, EtoRules.Lesson next, int seed) {
        return new EtoTalk.Context(regard, flags, 0, ghoul, false, false, true, true, next, true, granted, seed, stage, claimedBy, false, dayTime, mood, 3L,
            0L, "wolf", ring, true, true, pact, 0, trialDone, false, "Steve", false);
    }

    static void claimed() {
        EtoTalk.Context other = full(100, NpcBonds.MET, true, Romance.Stage.NONE, "Alex", 6000L, EtoMood.CLAIMED, true, EtoRules.Pact.NONE, false, false,
            EtoRules.Lesson.KICK, 4);
        List<NpcTalk.Choice> cold = EtoTalk.choices(other);
        check(cold.contains(NpcTalk.Choice.LESSON) && !cold.contains(NpcTalk.Choice.BOOK) && !cold.contains(NpcTalk.Choice.FLOWER)
            && !cold.contains(NpcTalk.Choice.PUPPY) && !cold.contains(NpcTalk.Choice.SWEET) && !cold.contains(NpcTalk.Choice.FAVOUR)
            && !cold.contains(NpcTalk.Choice.FOLLOW) && !cold.contains(NpcTalk.Choice.SAY_8), "someone else's: lessons only, no gifts, nothing sweet");
        check(EtoTalk.greeting(other).contains("Alex") || EtoTalk.greeting(other.reseed(5)).contains("Alex") || EtoTalk.greeting(other.reseed(6)).contains("Alex"),
            "she says whose she is");
        check(EtoTalk.answer(other, NpcTalk.Choice.ASK_OUT).effect() == EtoTalk.Effect.NONE && EtoTalk.answer(other, NpcTalk.Choice.FAVOUR).effect()
            == EtoTalk.Effect.NONE, "and is not asked out");
        check(EtoTalk.answer(other, NpcTalk.Choice.LESSON).effect() == EtoTalk.Effect.TEACH, "but still teaches");
        EtoTalk.Context wife = full(100, EtoRules.DATING, true, Romance.Stage.MARRIED, null, 18000L, EtoMood.HAPPY, true, EtoRules.Pact.NONE, false, false,
            null, 2);
        check(!EtoTalk.sweet(wife, "").choices().contains(NpcTalk.Choice.PROPOSE) && EtoTalk.sweet(wife, "").choices().contains(NpcTalk.Choice.NIGHT)
            && EtoTalk.answer(wife, NpcTalk.Choice.NIGHT).effect() == EtoTalk.Effect.NIGHT, "married: no ring twice; the night, at night");
        EtoTalk.Context dating = full(100, EtoRules.DATING, true, Romance.Stage.DATING, null, 6000L, EtoMood.HAPPY, true, EtoRules.Pact.NONE, false, false,
            null, 2);
        EtoTalk.Option hand = EtoTalk.sweet(dating, "").options().stream().filter(o -> o.key().equals("sweet:hand")).findFirst().orElse(null);
        check(hand != null && hand.label().equals("Hold my hand.") && EtoTalk.sweetly(dating, "hand").effect() == EtoTalk.Effect.HAND
            && EtoTalk.sweetly(dating, "cling").effect() == EtoTalk.Effect.CLING && EtoTalk.sweetly(dating, "cling").gesture() == EtoGesture.NONE,
            "her hand, and her arm round theirs, asked for (she comes to them for it)");
        check(EtoTalk.sweet(dating, "").choices().contains(NpcTalk.Choice.PROPOSE) && !EtoTalk.sweet(dating, "").choices().contains(NpcTalk.Choice.NIGHT)
            && EtoTalk.answer(dating, NpcTalk.Choice.NIGHT).effect() == EtoTalk.Effect.NONE, "dating with a ring: the proposal; by day, no night");
        check(EtoTalk.choices(dating).contains(NpcTalk.Choice.SAY_8), "many gifts at once: a page of their own");
        check(!EtoTalk.choices(full(100, NpcBonds.ASKED, true, Romance.Stage.NONE, null, 6000L, EtoMood.HAPPY, false, EtoRules.Pact.NONE, false, false,
            null, 1)).contains(NpcTalk.Choice.WHO), "'Who are you?' once: she's told them");
        check(!EtoLines.surgeryWords(dating).equals(EtoLines.surgeryWords(other)) && EtoLines.surgeryWords(other.reseed(0)).contains("crying"),
            "the surgery: warm for hers, cold for anyone else");
        for (EtoMood mood : EtoMood.values()) {
            for (int seed = 0; seed < 8; seed++) {
                for (Romance.Stage stage : Romance.Stage.values()) {
                    EtoTalk.Context c = full(stage.together() ? 100 : 30, 0, seed % 2 == 0, stage, null, seed * 3000L, mood, false, EtoRules.Pact.NONE, false,
                        false, null, seed);
                    String hello = EtoTalk.greeting(c);
                    check(!hello.isEmpty() && !hello.contains("{") && hello.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 1024,
                        "a greeting for every mood and stage: " + mood + " " + stage);
                    check(!EtoLines.approach(c).contains("{") && !EtoLines.welcome(c).contains("{"), "her words come with names: " + mood);
                }
            }
        }
    }

    static void student() {
        EtoTalk.Context human = full(0, 0, false, Romance.Stage.NONE, null, 6000L, EtoMood.WARY, false, EtoRules.Pact.READY, false, false, null, 1);
        check(EtoTalk.choices(human).contains(NpcTalk.Choice.TRIAL) && EtoTalk.answer(human, NpcTalk.Choice.TRIAL).effect() == EtoTalk.Effect.OFFER,
            "a human who paid her price: the one eye offered");
        EtoTalk.Context ghoul = full(0, 0, true, Romance.Stage.NONE, null, 6000L, EtoMood.WARY, false, EtoRules.Pact.NONE, true, false, null, 1);
        check(EtoTalk.choices(ghoul).contains(NpcTalk.Choice.TRIAL) && EtoTalk.answer(ghoul, NpcTalk.Choice.TRIAL).effect() == EtoTalk.Effect.OFFER,
            "a ghoul who passed her trial: the one eye offered");
        check(!EtoTalk.choices(full(0, 0, true, Romance.Stage.NONE, null, 6000L, EtoMood.WARY, false, EtoRules.Pact.NONE, true, true, null, 1))
            .contains(NpcTalk.Choice.TRIAL), "once made one-eyed, never again");
        long[] step = EtoRules.pact(EtoRules.Pact.HUNT, 11, EtoRules.Pact.HUNT, 1);
        check(step[0] == EtoRules.Pact.GHOUL.ordinal() && step[1] == 0, "twelve at night: then a ghoul");
        check(EtoRules.pact(EtoRules.Pact.HUNT, 3, EtoRules.Pact.GHOUL, 1)[1] == 3 && EtoRules.pact(EtoRules.Pact.MEAT, 0, EtoRules.Pact.MEAT, 1)[0]
            == EtoRules.Pact.READY.ordinal() && EtoRules.Pact.READY.then() == EtoRules.Pact.READY, "only the step on counts; then she's ready for them");
        Quest trial = QuestBook.get("eto_one_eye");
        check(trial != null && trial.after().contains("eto_witness") && EtoRules.lessonFlag("eto_one_eye") == EtoRules.TAUGHT_TRIAL,
            "her last lesson comes after the third, taught like the rest");
    }

    static void typing() {
        String line = "Hi. There, you... come here.";
        double all = com.hex.kagune.control.client.NpcTalkView.duration(line);
        int last = -1;
        boolean onward = true;
        for (double s = 0.0; s <= all + 0.1; s += 0.01) {
            int now = com.hex.kagune.control.client.NpcTalkView.revealed(line, s);
            onward &= now >= last;
            last = now;
        }
        check(onward && com.hex.kagune.control.client.NpcTalkView.revealed(line, 0.0) == 0
            && com.hex.kagune.control.client.NpcTalkView.revealed(line, all + 0.01) == line.length()
            && com.hex.kagune.control.client.NpcTalkView.revealed(line, Double.POSITIVE_INFINITY) == line.length(), "her words come out in order, all of them");
        check(com.hex.kagune.control.client.NpcTalkView.duration("Yes. No.") > com.hex.kagune.control.client.NpcTalkView.duration("Yes, No.")
            && com.hex.kagune.control.client.NpcTalkView.duration("Yes, No.") > com.hex.kagune.control.client.NpcTalkView.duration("Yes  No.")
            && all < 3.0, "a pause at a comma, a longer one at a sentence's end; never long");
    }

    static void topics() {
        for (Romance.Stage stage : Romance.Stage.values()) {
            for (EtoMood mood : EtoMood.values()) {
                for (int seed = 0; seed < 6; seed++) {
                    for (boolean ghoul : new boolean[] {false, true}) {
                        String claimedBy = seed == 5 && !stage.together() ? "Alex" : null;
                        EtoTalk.Context c = full(stage.together() ? 100 : seed * 20, 0, ghoul, stage, claimedBy, 3000L + seed * 4000L, mood, false,
                            EtoRules.Pact.NONE, false, false, null, seed);
                        EtoTalk.Page talk = EtoTalk.talk(c, "");
                        check(talk.options().size() <= 12 && talk.options().size() >= 4, "a handful of topics to talk about: " + stage + " " + mood);
                        for (EtoTopics.Topic topic : EtoTopics.open(c)) {
                            EtoTalk.Page page = EtoTalk.topic(c, topic);
                            check(!page.line().isEmpty() && !page.line().contains("{") && page.options().size() <= 12 && page.options().size() >= 2,
                                topic.id() + ": an answer and something to say back (" + stage + " " + mood + ")");
                            for (int i = 0; i < topic.open(c).size(); i++) {
                                EtoTalk.Reply reply = EtoTalk.follow(c, topic, i);
                                check(reply != null && !reply.line().isEmpty() && !reply.line().contains("{")
                                    && reply.line().getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 1024, topic.id() + " reply " + i);
                            }
                        }
                        if (stage.together()) {
                            for (String[] say : EtoTalk.SWEET_SAYS) {
                                check(!EtoTalk.sweetly(c, say[1]).line().isEmpty(), "a sweet word answered: " + say[1]);
                            }
                        }
                    }
                }
            }
        }
    }

    static void romance() {
        check(Romance.propose(Romance.Stage.NONE, false, false, 9, 100) == Romance.Refusal.NOT_DATING
            && Romance.propose(Romance.Stage.DATING, false, false, 0, 100) == Romance.Refusal.TOO_SOON
            && Romance.propose(Romance.Stage.DATING, false, false, 1, 80) == Romance.Refusal.NOT_CLOSE
            && Romance.propose(Romance.Stage.DATING, false, false, 1, 95) == null
            && Romance.propose(Romance.Stage.DATING, false, true, 5, 100) == Romance.Refusal.MARRIED_ELSEWHERE
            && Romance.propose(Romance.Stage.NONE, true, false, 5, 100) == Romance.Refusal.CLAIMED
            && Romance.propose(Romance.Stage.MARRIED, false, false, 5, 100) == Romance.Refusal.ALREADY, "a ring is taken only when it should be");
        check(Romance.tag(Romance.Stage.DATING, "Steve").equals("Dating Steve") && Romance.tag(Romance.Stage.MARRIED, "Steve").equals("Married to Steve")
            && Romance.tag(Romance.Stage.NONE, "Steve").isEmpty(), "the line over her head");
        check(Romance.jealousy(Romance.Stage.NONE, true) == 0 && Romance.jealousy(Romance.Stage.DATING, false) < 0
            && Romance.jealousy(Romance.Stage.DATING, true) < Romance.jealousy(Romance.Stage.DATING, false)
            && Romance.jealousy(Romance.Stage.MARRIED, true) < Romance.jealousy(Romance.Stage.DATING, true), "jealousy: worse seen, worse married");
        check(!Romance.mayDate(true, false) && !Romance.mayDate(false, true) && Romance.mayDate(false, false), "one partner each, for good");
        check(Romance.days(3, 10) == 7 && Romance.days(-1, 10) == 0 && Romance.days(12, 10) == 0, "days together");
    }

    static void memory() {
        NpcMemory memory = new NpcMemory().set("eto", NpcMemory.SINCE, 12).add("eto", NpcMemory.KISSES, 3).add("eto", NpcMemory.KISSES, 2)
            .set("toka", NpcMemory.SEEN, -5).set("Bad Key", "x", 1);
        NpcMemory back = NpcMemory.load(memory.save());
        check(back.get("eto", NpcMemory.SINCE, 0) == 12 && back.get("eto", NpcMemory.KISSES, 0) == 5 && back.get("toka", NpcMemory.SEEN, 0) == -5
            && !back.knows("Bad Key", "x") && back.get("eto", "nothing", 7) == 7, "what a player and the NPCs remember of each other, saved");
        check(NpcMemory.load("eto{a=1,b=x,=3};{c=1};junk").get("eto", "a", 0) == 1 && NpcMemory.load(null).save().isEmpty(), "a broken save reads what it can");
        back.forget("eto", NpcMemory.SINCE).forgetAll("toka");
        check(!back.knows("eto", NpcMemory.SINCE) && back.knows("eto", NpcMemory.KISSES) && !back.knows("toka", NpcMemory.SEEN), "forgotten when it should be");
    }

    static void unique() {
        UniqueNpc eto = new UniqueNpc();
        java.util.UUID first = new java.util.UUID(1, 1);
        java.util.UUID second = new java.util.UUID(2, 2);
        check(eto.claim(first, 100) && !eto.claim(second, 200) && eto.claim(first, 300), "only one of her lives; the same body ticks on");
        check(eto.claim(second, 300 + UniqueNpc.GONE_QUIET), "gone quiet for days, a new body may take her place");
        eto.homeX = 5;
        eto.homeY = 64;
        eto.homeZ = -7;
        eto.homed = true;
        eto.died(first, 10);
        check(eto.alive, "the old body dying changes nothing");
        eto.died(second, 1000);
        check(!eto.alive && !eto.dueBack(1000 + UniqueNpc.COMES_BACK - 1) && eto.dueBack(1000 + UniqueNpc.COMES_BACK), "dead: back home a couple of days later");
        java.util.UUID steve = new java.util.UUID(3, 3);
        eto.pair(steve, "Steve", Romance.Stage.MARRIED);
        check(eto.stageOf(steve) == Romance.Stage.MARRIED && eto.stageOf(first) == Romance.Stage.NONE && eto.claimedFrom(first) && !eto.claimedFrom(steve)
            && eto.tag().equals("Married to Steve"), "her partner, and everyone else");
        java.util.Map<String, UniqueNpc> all = new java.util.LinkedHashMap<>();
        all.put("eto", eto);
        UniqueNpc back = UniqueNpc.loadAll(UniqueNpc.saveAll(all)).get("eto");
        check(back != null && back.stageOf(steve) == Romance.Stage.MARRIED && back.homeZ == -7 && back.homed && !back.alive && back.diedAt == 1000
            && second.equals(back.body), "kept with the world");
        eto.pair(null, "", Romance.Stage.NONE);
        check(eto.tag().isEmpty() && !eto.claimedFrom(steve), "free again (only when her partner kills her)");
        check(UniqueNpc.loadAll("eto|x|y\nbad").isEmpty(), "a broken line is dropped");
        UniqueNpc home = new UniqueNpc();
        home.claim(first, 0);
        home.originX = 10;
        home.originY = 70;
        home.originZ = 20;
        home.origined = true;
        home.homeX = 400;
        home.homeY = 64;
        home.homeZ = 400;
        home.homed = true;
        home.away = true;
        home.company = 1000;
        check(!home.dueHome(1000 + UniqueNpc.GOES_HOME - 1) && home.dueHome(1000 + UniqueNpc.GOES_HOME), "left away a day with no one about: she goes home");
        home.away = false;
        check(!home.dueHome(9_999_999L), "at home, she stays");
        check(home.spawn()[0] == 10 && home.spawn()[2] == 20, "she comes back where she belongs, not where she was left");
        home.died(first, 5000);
        check(home.dueBack(5000 + UniqueNpc.COMES_BACK) && UniqueNpc.COMES_BACK <= 24000L, "and a day after she dies");
        all.put("eto", home);
        UniqueNpc kept = UniqueNpc.loadAll(UniqueNpc.saveAll(all)).get("eto");
        check(kept != null && kept.origined && kept.originY == 70 && kept.company == 1000, "her home kept with the world");
        check(UniqueNpc.loadAll("eto|-|1|5|-9223372036854775808|1,2,3|-|-|0").get("eto").homeX == 1, "a save from before her own home reads");
    }

    static void moods() {
        check(EtoMood.of(Romance.Stage.MARRIED, false, 100, 6000L, 0L, true, false, false, false, 0.5) == EtoMood.SULKY, "cross first");
        check(EtoMood.of(Romance.Stage.NONE, true, 100, 6000L, 0L, false, false, false, false, 0.5) == EtoMood.CLAIMED, "someone else's");
        check(EtoMood.of(Romance.Stage.DATING, false, 100, 6000L, 0L, false, true, false, false, 0.5) == EtoMood.JEALOUS
            && EtoMood.of(Romance.Stage.DATING, false, 100, 6000L, 0L, false, false, true, false, 0.5) == EtoMood.GIDDY
            && EtoMood.of(Romance.Stage.DATING, false, 100, 6000L, 3L, false, false, false, false, 0.5) == EtoMood.LONELY
            && EtoMood.of(Romance.Stage.MARRIED, false, 100, 18000L, 0L, false, false, false, false, 0.1) == EtoMood.AFFECTIONATE,
            "her partner: jealous, giddy, lonely, late at night wanting them");
        check(EtoMood.of(Romance.Stage.NONE, false, -10, 6000L, 0L, false, false, false, false, 0.5) == EtoMood.WARY
            && EtoMood.of(Romance.Stage.NONE, false, 50, 6000L, 9L, false, false, false, false, 0.5) == EtoMood.BORED, "anyone else: wary, or bored of waiting");
        for (EtoMood mood : EtoMood.values()) {
            check(mood.idles().length > 0 && !mood.word().isEmpty() && mood.seeks() >= 0L, mood + ": something to do with her hands");
        }
    }

    static void phases() {
        check(EtoRules.Phase.of(1.0F) == EtoRules.Phase.CALM && EtoRules.Phase.of(0.5F) == EtoRules.Phase.SERIOUS && EtoRules.Phase.of(0.2F)
            == EtoRules.Phase.MANIAC, "calm, serious, maniac as her health goes");
        check(EtoRules.damage(10.0F, 0.2F) > EtoRules.damage(10.0F, 0.5F) && EtoRules.damage(10.0F, 0.5F) > EtoRules.damage(10.0F, 1.0F),
            "the worse it gets for her, the harder she hits");
        check(EtoRules.cooldown(EtoRules.PUNCH_EVERY, EtoRules.Phase.MANIAC) < EtoRules.cooldown(EtoRules.PUNCH_EVERY, EtoRules.Phase.CALM)
            && EtoRules.Phase.MANIAC.resistance > EtoRules.Phase.SERIOUS.resistance
            && EtoRules.Phase.SERIOUS.speed >= 0 && EtoRules.Phase.CALM.speed < 0, "her moves come back sooner, and she gets faster and harder to hurt");
        check(EtoRules.HEALTH >= 300.0F && EtoRules.SIZE < 0.9F, "stacked, and a head shorter than most");
    }

    static void grant() {
        KakuganEyes one = KakuganEyes.DEFAULT.withGhoul(KakuganEyes.RIGHT);
        check(!EtoRules.allowed(one, false, KagunePath.RINKAKU).oneEyed(), "one eye only once she grants it");
        check(EtoRules.allowed(one, true, KagunePath.RINKAKU).oneEyed(), "granted: one eye");
        check(EtoRules.allowed(one, false, KagunePath.PALE).oneEyed() && EtoRules.allowed(one, false, KagunePath.GRAFT).oneEyed(),
            "a one-eyed path never needed her");
        check(!EtoRules.allowed(KakuganEyes.DEFAULT, true, KagunePath.UKAKU).oneEyed(), "both eyes are always allowed where both are");
        check(EtoRules.grandfathered(true, one, KagunePath.RINKAKU) && !EtoRules.grandfathered(false, one, KagunePath.RINKAKU)
            && !EtoRules.grandfathered(true, KakuganEyes.DEFAULT, KagunePath.RINKAKU) && !EtoRules.grandfathered(true, one, KagunePath.PALE),
            "already one-eyed by choice: kept");
    }

    static void fight() {
        long never = Long.MIN_VALUE;
        long later = 1000L;
        check(EtoRules.choose(10.0, true, true, 1.0F, 100, never, never, never, 0.1) == EtoRules.Act.KICK, "in sight from range: the Speed Kick");
        check(EtoRules.choose(10.0, false, true, 1.0F, 100, never, never, never, 0.1) == EtoRules.Act.CHASE, "nothing thrown at what she cannot see");
        check(EtoRules.choose(10.0, true, true, 0.5F, 100, never, never, never, 0.99) == EtoRules.Act.KICK, "serious, the kick whenever it is back");
        check(EtoRules.choose(6.0, true, true, 1.0F, 100, never, later, never, 0.2) == EtoRules.Act.PUNCH, "mid range: the Big Punch (its blink closes in)");
        check(EtoRules.choose(10.0, true, true, 1.0F, 100, later, later, never, 0.5) == EtoRules.Act.FLIP_IN, "both spent, a gap: a front flip in");
        check(EtoRules.choose(2.0, true, true, 1.0F, 100, later, later, later, 0.6) == EtoRules.Act.STRIKE, "up close: the blitz");
        check(EtoRules.choose(2.0, true, true, 0.4F, 100, later, later, never, 0.95) == EtoRules.Act.FLIP_BACK, "serious and pressed: now and then a flip clear");
        check(EtoRules.choose(5.0, true, true, 1.0F, 100, later, later, later, 0.5) == EtoRules.Act.STEP_IN, "just out of reach: a step in");
        check(EtoRules.choose(30.0, true, true, 1.0F, 100, never, never, never, 0.1) == EtoRules.Act.CHASE, "too far for anything: she runs at it");
        check(EtoRules.choose(10.0, true, false, 1.0F, 100, never, never, never, 0.1) != EtoRules.Act.KICK, "never a kick from the air");
        check(EtoRules.punchHold(EtoRules.Phase.MANIAC, 0.9) == com.hex.kagune.control.pale.PaleRules.PUNCH_CHARGE_TICKS
            && EtoRules.punchHold(EtoRules.Phase.SERIOUS, 0.1) == com.hex.kagune.control.pale.PaleRules.PUNCH_CHARGE_TICKS
            && EtoRules.punchHold(EtoRules.Phase.CALM, 0.9) < com.hex.kagune.control.pale.PaleRules.PUNCH_CHARGE_TICKS,
            "the Big Punch at the full charge (Witness This when it lands): always maniac, mostly serious, now and then calm");
        int full = 0;
        for (int i = 0; i < 100; i++) {
            full += EtoRules.punchHold(EtoRules.Phase.CALM, i / 100.0) >= com.hex.kagune.control.pale.PaleRules.PUNCH_CHARGE_TICKS ? 1 : 0;
        }
        check(full > 10 && full < 50, "calm, a full charge now and then: " + full + "%");
        check(com.hex.kagune.control.pale.PaleRules.fist(EtoRules.MASTERY) >= 4.4F, "her fists are a master's");
        check(EtoRules.damage(10.0F, 0.4F) > EtoRules.damage(10.0F, 0.9F), "serious, she hits harder");
        check(EtoRules.LEASH == EtoRules.HOME_RADIUS + 3 && EtoRules.LOST > EtoRules.LEASH, "she chases a little past home, never far");
    }

    static void poses() {
        for (EtoGesture gesture : EtoGesture.values()) {
            int length = gesture.held() ? 200 : gesture.ticks;
            for (int t = 0; t <= length; t += 3) {
                RigPose p = EtoPose.gesture(gesture, t);
                check(gesture == EtoGesture.NONE ? p == null : p != null && Float.isFinite(p.rightArmX) && Float.isFinite(p.flip) && Math.abs(p.flip) < 0.6F
                    && Math.abs(p.drop) < 4.0F, gesture + " at " + t + " is a sane pose");
            }
        }
        RigPose kiss = EtoPose.gesture(EtoGesture.KISS, 30);
        check(kiss.lean > 0.2F && kiss.flip == 0.0F && kiss.rightLegX == 0.0F && kiss.leftLegX == 0.0F && kiss.drop == 0.0F && kiss.rightArmX > 0.3F
            && kiss.leftArmX > 0.3F, "the kiss: her top half bends in at the hips, her feet flat where they stand, hands behind her back");
        RigPose pull = EtoPose.gesture(EtoGesture.PULL, 30);
        check(pull.rightArmX + pull.lean > -1.75F && pull.rightArmX + pull.lean < -1.2F && Math.abs(pull.rightArmY) < 0.45F && pull.drop == 0.0F,
            "pulling them in: her arms level with their shoulders (not closing on their face), her feet on the ground");
        RigPose hand = EtoPose.gesture(EtoGesture.HAND, 40);
        check(EtoGesture.HAND.held() && hand.leftArmZ < -0.05F && Math.abs(hand.rightArmZ) < 0.2F && hand.lean == 0.0F,
            "hand in hand: her left hand out to the one at her side, held until she lets go");
        double theirs = 0.375 + 0.75 * Math.sin(com.hex.kagune.control.npc.EtoHand.ARM);
        double hers = (0.34 + 0.75 * Math.sin(com.hex.kagune.control.npc.EtoHand.ARM)) * EtoRules.SIZE;
        check(Math.abs(theirs + hers - com.hex.kagune.control.npc.EtoHand.OUT) < 1.0E-9 && com.hex.kagune.control.npc.EtoHand.OUT > 0.6
            && com.hex.kagune.control.npc.EtoHand.OUT < 0.95, "their hands meet: she stands just as far off as both hands hang out");
        RigPose write = EtoPose.gesture(EtoGesture.WRITE, 20);
        RigPose writing = EtoPose.gesture(EtoGesture.WRITE, 21);
        check(write.leftArmX == writing.leftArmX && write.rightArmX != writing.rightArmX && EtoGesture.WRITE.mainHand.endsWith("feather")
            && EtoGesture.WRITE.offHand.endsWith("writable_book"), "writing: the quill hand moves, the book hand holds still");
        float turn = (float)(Math.PI * 2.0);
        boolean onward = true;
        for (int t = 24; t < EtoGesture.TWIRL.ticks; t++) {
            onward &= Math.abs(EtoPose.gesture(EtoGesture.TWIRL, t).spin - turn) < 1.0E-3F;
        }
        check(onward, "her twirl ends a whole turn round, never spun back as it fades");
        check(EtoPose.gesture(EtoGesture.WAVE, 0).rightArmX == 0.0F && EtoPose.gesture(EtoGesture.WAVE, EtoGesture.WAVE.ticks).rightArmX == 0.0F,
            "an idle one eases in and out");
    }
}
