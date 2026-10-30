package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcTalk;
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

/** 1.3.0: Eto: her eye's night, her blinks, her lessons, her errands, a kiss, her talk, the one-eye grant, her fight. */
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
            check(quest != null && quest.opens(lesson.gate) && EtoRules.lessonFlag(lesson.quest) == lesson.flag, lesson + ": its quest opens its gate");
            check(log.state(quest, KagunePath.RINKAKU, 1000) == QuestLog.State.LOCKED, lesson + ": locked until she teaches it, whatever the mastery");
        }
        check(EtoRules.lessonFlag("light_feet") == 0 && QuestBook.openers(Gate.EYES_KICK).size() >= 3, "the old ways in stay; hers is one more");
        QuestLog.Bond bond = QuestLog.Bond.NONE;
        check(EtoRules.next(bond) == EtoRules.Lesson.KICK, "the Speed Kick first");
        check(EtoRules.refusal(EtoRules.Lesson.KICK, 19, true, true) == EtoRules.Refusal.NOT_CLOSE
            && EtoRules.refusal(EtoRules.Lesson.KICK, 20, true, true) == null, "taught at 20");
        check(EtoRules.refusal(EtoRules.Lesson.KICK, 100, false, true) == EtoRules.Refusal.HUMAN, "never to a human");
        check(EtoRules.refusal(EtoRules.Lesson.SOAR, 39, true, true) == EtoRules.Refusal.NOT_CLOSE && EtoRules.refusal(EtoRules.Lesson.SOAR, 40, true, true) == null
            && EtoRules.refusal(EtoRules.Lesson.WITNESS, 60, true, true) == null, "then at 40 and 60");
        check(EtoRules.refusal(EtoRules.Lesson.SOAR, 100, true, false) == EtoRules.Refusal.PRACTISE, "not before the last one is practised");
        check(EtoRules.refusal(null, 100, true, true) == EtoRules.Refusal.ALL_TAUGHT, "three lessons, no more");
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
        check(EtoRules.book("minecraft:book") && EtoRules.book("minecraft:written_book") && !EtoRules.book("minecraft:paper") && !EtoRules.book(null),
            "books are books");
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
        check(EtoRules.kiss(EtoRules.Kiss.BLUSH, 100, true, 100, 0, 1.0) == EtoRules.Kiss.BLUSH, "and stays while it is held");
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
            && !choices.contains(NpcTalk.Choice.LESSON) && !choices.contains(NpcTalk.Choice.KISS) && !choices.contains(NpcTalk.Choice.FAVOUR),
            "a human stranger: who, chat, leave; no lessons, no kisses, no errands");
        EtoTalk.Context ghoul = context(25, 0, 0, true, true, false, EtoRules.Lesson.KICK, false);
        check(EtoTalk.choices(ghoul).contains(NpcTalk.Choice.LESSON) && EtoTalk.choices(ghoul).contains(NpcTalk.Choice.BOOK), "a ghoul with a book");
        check(EtoTalk.answer(ghoul, NpcTalk.Choice.LESSON).effect() == EtoTalk.Effect.TEACH, "she teaches at 25");
        check(EtoTalk.answer(context(10, 0, 0, true, false, false, EtoRules.Lesson.KICK, false), NpcTalk.Choice.LESSON).effect() == EtoTalk.Effect.NONE,
            "not at 10");
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
        check(dated.contains(NpcTalk.Choice.KISS) && dated.contains(NpcTalk.Choice.HUG) && dated.contains(NpcTalk.Choice.SURGERY)
            && !dated.contains(NpcTalk.Choice.INSULT) && dated.size() <= 12, "dating: kiss, hug, the surgery");
        check(EtoTalk.answer(date, NpcTalk.Choice.KISS).effect() == EtoTalk.Effect.KISS && EtoTalk.answer(date, NpcTalk.Choice.HUG).effect() == EtoTalk.Effect.HUG,
            "a kiss and a hug from the menu");
        check(EtoTalk.answer(date, NpcTalk.Choice.SURGERY).effect() == EtoTalk.Effect.OFFER && EtoTalk.offer().contains(NpcTalk.Choice.ACCEPT)
            && EtoTalk.answer(date, NpcTalk.Choice.ACCEPT).effect() == EtoTalk.Effect.SURGERY, "the surgery is offered, then done");
        EtoTalk.Context done = context(100, dating, 0, true, false, false, null, true);
        check(!EtoTalk.choices(done).contains(NpcTalk.Choice.SURGERY) && EtoTalk.answer(done, NpcTalk.Choice.ACCEPT).effect() == EtoTalk.Effect.NONE,
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
        check(NpcTalk.Choice.byOrdinal(11) == NpcTalk.Choice.LEAVE && NpcTalk.Choice.values().length == 19, "her choices are at the end, the old ordinals kept");
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
        EtoRules.Ready ready = EtoRules.Ready.NOW;
        check(EtoRules.choose(9.0, 100, ready, true, true, 1.0F, false, 0.5) == EtoRules.Move.FLIP_FORWARD, "far: a front flip in");
        check(EtoRules.choose(9.0, 100, ready, true, false, 1.0F, false, 0.5) == EtoRules.Move.NONE, "not at what she cannot see");
        check(EtoRules.choose(5.0, 100, ready, true, true, 1.0F, false, 0.5) == EtoRules.Move.BLITZ, "blitz range: the Speed Blitz");
        check(EtoRules.choose(2.0, 100, ready, true, true, 1.0F, true, 0.9) == EtoRules.Move.PUNCH, "up close on something tough: the Big Punch");
        EtoRules.Ready punched = ready.used(EtoRules.Move.PUNCH, 100);
        check(EtoRules.choose(2.0, 101, punched, true, true, 1.0F, true, 0.9) == EtoRules.Move.NONE, "nothing while the punch plays out");
        check(EtoRules.choose(2.0, 100 + EtoRules.PUNCH_TICKS + EtoRules.STRIKE_EVERY, punched, true, true, 1.0F, true, 0.5) == EtoRules.Move.KICK,
            "the punch spent: the Speed Kick");
        EtoRules.Ready spent = punched.used(EtoRules.Move.KICK, 140);
        check(EtoRules.choose(2.0, 160, spent, true, true, 1.0F, true, 0.9) == EtoRules.Move.STRIKE, "both spent: plain strikes");
        check(EtoRules.choose(2.0, 100, ready, true, true, 0.3F, false, 0.1) == EtoRules.Move.FLIP_BACK, "hurt and pressed: a flip clear");
        check(EtoRules.damage(10.0F, 0.4F) > EtoRules.damage(10.0F, 0.9F), "transformed below half, she hits harder");
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
        check(kiss.flip < -0.2F && Math.abs(kiss.rightLegX - kiss.flip) < 1.0E-4F && kiss.rightArmX > 0.3F && kiss.leftArmX > 0.3F,
            "the kiss: her top half leans in, the legs stay under her, hands behind her back");
        check(EtoPose.gesture(EtoGesture.WAVE, 0).rightArmX == 0.0F && EtoPose.gesture(EtoGesture.WAVE, EtoGesture.WAVE.ticks).rightArmX == 0.0F,
            "an idle one eases in and out");
        for (EtoRules.Move move : EtoRules.Move.values()) {
            for (int t = 0; t < 24; t++) {
                RigPose p = EtoPose.move(move, t);
                check(p == null || Float.isFinite(p.rightArmX) && Float.isFinite(p.rightLegX), move + " at " + t);
            }
        }
    }
}
