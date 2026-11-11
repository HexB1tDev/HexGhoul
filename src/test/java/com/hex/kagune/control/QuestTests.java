package com.hex.kagune.control;

import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Hunts;
import com.hex.kagune.control.quest.Objective;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestEvent;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** 7.0.1: the quests: the book holds together, a log moves through its states, saves round-trip, hunts are drawn and rebuilt. */
public final class QuestTests {
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
        book();
        states();
        exclusive();
        oneLife();
        saves();
        migration();
        hunts();
        gates();
        givers();
        taughtAlready();
        romance();
        forGood();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Quest tests: " + passed + " checks passed");
    }

    /**
     * 4.1.33 (the owner: "Quests should only be done once ... once you did all of them /quest complete, all of them should be finished. Even if
     * you pick another kagune, this should still be finished because you finished all of them. You don't need to be typing it again."):
     * finished once, every path's and every ghoul's quest is done for good, at any mastery, kept through a save; nothing opens again.
     */
    static void forGood() {
        QuestLog log = new QuestLog();
        Quest anteiku = QuestBook.get("anteiku_way");
        if (anteiku != null) {
            log.complete(QuestBook.get("pack_way") != null ? QuestBook.get("pack_way") : anteiku);
        }
        log.finishAll();
        QuestLog back = QuestLog.load(log.save());
        check(back.finished(), "finished for good, through a save");
        for (QuestLog l : new QuestLog[] {log, back}) {
            for (com.hex.kagune.control.KagunePath path : com.hex.kagune.control.KagunePath.values()) {
                for (int mastery : new int[] {0, 300, 750, 1000, 5000}) {
                    for (Quest quest : QuestBook.all()) {
                        if (quest.line() != Quest.Line.PATH && quest.line() != Quest.Line.GHOUL) {
                            continue;
                        }
                        QuestLog.State state = l.state(quest, path, mastery);
                        check(state == QuestLog.State.DONE || state == QuestLog.State.CLOSED,
                            quest.id() + " on " + path + " at mastery " + mastery + ": never open again (" + state + ")");
                    }
                }
            }
        }
        // A quest a later version adds is done as it comes: state() says so without a stored end.
        Quest any = null;
        for (Quest quest : QuestBook.all()) {
            if (quest.line() == Quest.Line.PATH) {
                any = quest;
                break;
            }
        }
        QuestLog fresh = QuestLog.load("v=" + QuestLog.VERSION + ";all=1");
        check(any != null && fresh.state(any, any.path() == null ? com.hex.kagune.control.KagunePath.RINKAKU : any.path(), 0)
            == QuestLog.State.DONE, "any quest, stored or not, is done once all are");
        // The hunts and favours still come, a night at a time.
        Hunts.Spec spec = new Hunts.Spec(0, 4, 3, Hunts.PAY_MEAT, 1);
        Quest hunt = Hunts.quest(spec);
        check(back.state(hunt, com.hex.kagune.control.KagunePath.RINKAKU, 500) != QuestLog.State.DONE, "a hunt is still its own night's work");
        back.reset();
        check(!back.finished(), "a reset (the command) starts them over");
    }

    /** Every quest is sound, every gate has a way in on its own path, and every path has its own line. */
    static void book() {
        Set<String> ids = new HashSet<>();
        for (Quest quest : QuestBook.all()) {
            check(ids.add(quest.id()), quest.id() + ": one quest per id");
            check(!quest.title().isBlank() && !quest.story().isBlank() && !quest.objectives().isEmpty(), quest.id() + ": a title, a story, objectives");
            for (String before : quest.after()) {
                Quest prior = QuestBook.get(before);
                check(prior != null && prior.mastery() <= quest.mastery(), quest.id() + ": comes after a real, earlier quest " + before);
            }
            for (Objective objective : quest.objectives()) {
                check(!objective.text().isBlank() && objective.count() > 0, quest.id() + ": objective has words and a count");
            }
            for (Gate gate : quest.reward().gates()) {
                check(gate.path() == null || gate.path() == quest.path(), quest.id() + ": opens only its own path's gate " + gate.id());
            }
            if (quest.exclusive() != null) {
                long rivals = QuestBook.all().stream().filter(other -> quest.exclusive().equals(other.exclusive())).count();
                check(rivals >= 2, quest.id() + ": an exclusive group has a rival");
            }
        }
        for (Gate gate : Gate.values()) {
            List<Quest> openers = QuestBook.openers(gate);
            // 1.3.14: a gate a companion teaches has no quest opening it: its teacher does.
            boolean taught = com.hex.kagune.control.npc.Teaching.taught(gate);
            check(taught ? openers.isEmpty() : !openers.isEmpty(), gate.id() + ": some quest opens it, or its teacher does (never both)");
            for (Quest opener : openers) {
                check(opener.path() == gate.path() || opener.path() == null, gate.id() + ": opened on its own path by " + opener.id());
                check(gate.mutation() ? gate.mastery() == 0 : opener.mastery() <= gate.mastery(), gate.id() + ": " + opener.id() + " starts before the gate's mastery");
            }
            check(taught || !QuestBook.openersText(gate).isBlank(), gate.id() + ": names its quests");
            check(Gate.fromId(gate.id()) == gate, gate.id() + ": found by id");
        }
        for (KagunePath path : new KagunePath[] {KagunePath.RINKAKU, KagunePath.UKAKU, KagunePath.GRAFT}) {
            long own = QuestBook.all().stream().filter(quest -> quest.path() == path).count();
            check(own >= 6, path + ": a line of its own (" + own + " quests)");
            long gates = java.util.Arrays.stream(Gate.values()).filter(gate -> gate.path() == path).count();
            check(gates >= 4, path + ": at least four gates of its own");
        }
        // Rival ways: most gates of the paths have two quests opening them.
        int two = 0;
        int pathGates = 0;
        for (Gate gate : Gate.values()) {
            if (gate.path() != null && !gate.mutation() && !com.hex.kagune.control.npc.Teaching.taught(gate)) {
                pathGates++;
                two += QuestBook.openers(gate).size() >= 2 ? 1 : 0;
            }
        }
        check(two * 2 >= pathGates, "most path gates opened by quests have two ways in (" + two + " of " + pathGates + ")");
    }

    /** Locked until the path and mastery fit, open while counting, met when every objective is, done once paid. */
    static void states() {
        QuestLog log = new QuestLog();
        Quest gift = QuestBook.get("rizes_gift");
        check(log.state(gift, KagunePath.UKAKU, 1000) == QuestLog.State.LOCKED, "another path's quest is locked");
        check(log.state(gift, KagunePath.RINKAKU, gift.mastery() - 1) == QuestLog.State.LOCKED, "locked below its mastery");
        check(log.state(gift, KagunePath.RINKAKU, gift.mastery()) == QuestLog.State.OPEN, "open at its mastery");
        // Locked quests do not count.
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.RINKAKU, 0);
        check(log.progress(gift, 0) == 0, "a locked quest doesn't count");
        for (int i = 0; i < 20; i++) {
            log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE, Tags.HOSTILE), KagunePath.RINKAKU, 300);
        }
        check(log.progress(gift, 0) == gift.objectives().get(0).count(), "counts up to its objective and no further");
        check(!log.met(gift), "not met with an objective left");
        log.apply(QuestEvent.of(Signal.EAT, 1, Tags.item("minecraft:bread")), KagunePath.RINKAKU, 300);
        check(log.progress(gift, 1) == 0, "an event without the tag doesn't count");
        for (int i = 0; i < 5; i++) {
            log.apply(QuestEvent.of(Signal.EAT, 1, Tags.FLESH), KagunePath.RINKAKU, 300);
        }
        check(log.met(gift), "met with every objective");
        check(!log.open(Gate.RINKAKU_TWIN), "the gate stays shut until it is paid");
        log.complete(gift);
        check(log.state(gift, KagunePath.RINKAKU, 300) == QuestLog.State.DONE && log.open(Gate.RINKAKU_TWIN), "done, and its gate open");
        int before = log.progress(gift, 0);
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.RINKAKU, 300);
        check(log.progress(gift, 0) == before, "a done quest counts no more");
        // A quest that comes after another waits for it (1.3.29: the ghouls' chain, once a ghoul has been talked to).
        log.bond("a-ghoul-of-the-night", QuestLog.Bond.NONE.with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        Quest errands = QuestBook.get("errands");
        check(log.state(errands, KagunePath.RINKAKU, 1000) == QuestLog.State.LOCKED, "waits for the quest it comes after");
        log.complete(QuestBook.get("a_friend"));
        check(log.state(errands, KagunePath.RINKAKU, 1000) == QuestLog.State.OPEN, "open once that one is done");
        // Best-of objectives keep the highest amount.
        Quest rampage = QuestBook.get("rampage");
        log.apply(QuestEvent.of(Signal.RAMPAGE, 3), KagunePath.RINKAKU, 1000);
        log.apply(QuestEvent.of(Signal.RAMPAGE, 2), KagunePath.RINKAKU, 1000);
        check(log.progress(rampage, 0) == 3, "a rampage objective keeps its best streak");
        log.apply(QuestEvent.of(Signal.RAMPAGE, 9), KagunePath.RINKAKU, 1000);
        check(log.progress(rampage, 0) == 5 && log.met(rampage), "and caps at its count");
        // Points and sides.
        log.complete(QuestBook.get("die_hard"));
        check(log.points() == 1, "a quest's mutation point is kept");
    }

    /** The first of two rival ways closes the other for good. */
    static void exclusive() {
        QuestLog log = new QuestLog();
        Quest anteiku = QuestBook.get("anteiku");
        Quest pack = QuestBook.get("pack");
        List<Quest> closed = log.complete(pack);
        check(closed.contains(anteiku), "taking the Pack's way closes Anteiku's");
        check(log.state(anteiku, KagunePath.RINKAKU, 1000) == QuestLog.State.CLOSED, "closed for good");
        check(log.side(QuestBook.PACK) == 1 && log.side(QuestBook.ANTEIKU) == 0, "the side taken is remembered");
        log.apply(QuestEvent.of(Signal.RESTRAIN, 1), KagunePath.RINKAKU, 1000);
        check(log.progress(anteiku, 0) == 0, "a closed quest counts no more");
    }

    static void oneLife() {
        Objective o = Objective.of("x", Signal.KILL, 3).inOneLife();
        check(o.oneLife() && !Objective.of("x", Signal.KILL, 3).oneLife(), "one-life objectives are marked");
        check(o.advance(1, QuestEvent.of(Signal.KILL, 1)) == 2, "advance adds");
        check(!Objective.of("x", Signal.KILL, 1, Tags.EYES).without(Tags.KAKUJA).matches(QuestEvent.of(Signal.KILL, 1, Tags.EYES, Tags.KAKUJA)),
            "a forbidden tag stops it matching");
        check(!Objective.of("x", Signal.KILL, 1).matches(QuestEvent.of(Signal.KILL, 0)), "nothing happened, nothing counts");
    }

    static void saves() {
        QuestLog log = new QuestLog();
        log.migrate(gate -> gate == Gate.EYES_KICK);
        for (int i = 0; i < 7; i++) {
            log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.RINKAKU, 300);
        }
        log.complete(QuestBook.get("pack"));
        log.complete(QuestBook.get("cannibal"));
        Hunts.Spec spec = new Hunts.Spec(0, 4, 3, Hunts.PAY_MEAT, 1);
        log.startHunt(spec, 12345L);
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.mob("minecraft:witch")), KagunePath.RINKAKU, 300);
        log.nextHuntAt(99L);
        QuestLog back = QuestLog.load(log.save());
        Quest gift = QuestBook.get("rizes_gift");
        check(back.progress(gift, 0) == 7, "progress survives the save");
        check(back.state(QuestBook.get("pack"), KagunePath.RINKAKU, 1000) == QuestLog.State.DONE, "done survives");
        check(back.state(QuestBook.get("anteiku"), KagunePath.RINKAKU, 1000) == QuestLog.State.CLOSED, "closed survives");
        check(back.open(Gate.EYES_KICK) && back.open(Gate.RINKAKU_FOUR), "gates survive");
        check(back.points() == 1 && back.side(QuestBook.PACK) == 1, "points and sides survive");
        check(back.migrated() == QuestLog.VERSION && back.nextHuntAt() == 99L, "the version and the hunt clock survive");
        check(back.hunts().size() == 1 && back.hunts().get(0).endsAt() == 12345L, "the hunt survives");
        check(back.progress(back.hunts().get(0).quest(), 0) == 1, "and its progress");
        check(back.save().equals(log.save()), "a second save is the same string");
        check(QuestLog.load("").migrated() == 0 && QuestLog.load(null).points() == 0, "nothing saved is a new log");
        QuestLog junk = QuestLog.load("v=1;gate=nope,eyes_kick;done=nothing;p.nothing=4;p.rizes_gift=99,x;h.0=bad");
        check(junk.open(Gate.EYES_KICK) && junk.gates().size() == 1, "an unknown gate is dropped");
        check(junk.progress(gift, 0) == gift.objectives().get(0).count() && junk.progress(gift, 1) == 0, "bad numbers are clamped or zero");
        check(junk.hunts().isEmpty(), "a bad hunt is dropped");
    }

    static void migration() {
        QuestLog log = new QuestLog();
        log.migrate(gate -> !gate.mutation() && gate.mastery() <= 500);
        check(log.open(Gate.RINKAKU_TWIN) && log.open(Gate.RINKAKU_FOUR) && !log.open(Gate.RINKAKU_CENTIPEDE), "what was reached stays open");
        log.migrate(gate -> true);
        check(!log.open(Gate.RINKAKU_CENTIPEDE), "migration happens once");
        log.reset();
        check(log.gates().isEmpty() && log.migrated() == QuestLog.VERSION, "a reset forgets everything and never migrates again");
    }

    /** Which gate each form, stage and mutation is behind, and the stages a gate leaves usable. */
    static void gates() {
        check(com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED) == null, "the seed is never gated");
        check(com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.RINKAKU_DUAL) == Gate.RINKAKU_TWIN
            && com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD) == Gate.RINKAKU_FOUR
            && com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.CENTIPEDE) == Gate.RINKAKU_CENTIPEDE
            && com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA) == Gate.RINKAKU_FULL_KAKUJA,
            "every Rinkaku form past the seed has its gate");
        check(com.hex.kagune.control.quest.Gates.graft(com.hex.kagune.control.graft.GraftStage.TRANSPLANT) == null, "the transplant is never gated");
        for (com.hex.kagune.control.graft.GraftStage stage : com.hex.kagune.control.graft.GraftStage.values()) {
            Gate gate = com.hex.kagune.control.quest.Gates.graft(stage);
            check(stage == com.hex.kagune.control.graft.GraftStage.TRANSPLANT || gate != null && gate.path() == KagunePath.GRAFT
                && gate.mastery() == stage.mastery(), stage + ": its gate, at its mastery");
        }
        java.util.function.Predicate<Gate> onlyCentipede = gate -> gate == Gate.GRAFT_CENTIPEDE;
        check(com.hex.kagune.control.quest.Gates.graftUsable(com.hex.kagune.control.graft.GraftStage.DRAGON, 1000, onlyCentipede)
            == com.hex.kagune.control.graft.GraftStage.CENTIPEDE, "a stage whose quest isn't done falls back to the highest that is open");
        check(com.hex.kagune.control.quest.Gates.graftUsable(com.hex.kagune.control.graft.GraftStage.THORNS, 1000, gate -> false)
            == com.hex.kagune.control.graft.GraftStage.TRANSPLANT, "nothing open: the transplant");
        check(com.hex.kagune.control.quest.Gates.graftUsable(com.hex.kagune.control.graft.GraftStage.DRAGON, 1000, gate -> true)
            == com.hex.kagune.control.graft.GraftStage.DRAGON, "everything open: what was picked");
        check(com.hex.kagune.control.quest.Gates.graftNext(com.hex.kagune.control.graft.GraftStage.CENTIPEDE, 1000, onlyCentipede)
            == com.hex.kagune.control.graft.GraftStage.TRANSPLANT, "K held skips the stages waiting for quests");
        check(com.hex.kagune.control.quest.Gates.graft(com.hex.kagune.control.graft.GraftMutation.RIZES_APPETITE) == Gate.GRAFT_APPETITE
            && com.hex.kagune.control.quest.Gates.graft(com.hex.kagune.control.graft.GraftMutation.WASHUU_BLOOD) == null
            && com.hex.kagune.control.quest.Gates.ukaku(com.hex.kagune.control.ukaku.UkakuMutation.BURSTING_SHARDS) == Gate.UKAKU_BURSTING_SHARDS
            && com.hex.kagune.control.quest.Gates.ukaku(com.hex.kagune.control.ukaku.UkakuMutation.NOT_THAT_EASILY) == Gate.UKAKU_NOT_THAT_EASILY
            && com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.MutationNode.CENTIPEDE_BODY) == null,
            "the gated mutations (1.0.41: the Rinkaku path has none left)");
        // 1.0.41: Graft's revive on every path that has it, behind the same quest: the same objectives, the same mastery.
        Quest graft = QuestBook.get("not_that_easily");
        for (String id : new String[] {"rinkaku_not_that_easily", "ukaku_not_that_easily"}) {
            Quest copy = QuestBook.get(id);
            check(copy != null && copy.mastery() == graft.mastery() && copy.title().equals(graft.title())
                && copy.objectives().equals(graft.objectives()), id + ": Graft's quest, objective for objective");
        }
        check(QuestBook.get("arrow_eater") == null && QuestBook.openers(Gate.RINKAKU_NOT_THAT_EASILY).size() == 1
            && QuestBook.openers(Gate.UKAKU_NOT_THAT_EASILY).size() == 1, "the Rinkaku path's mutation quest is gone; the revive's quests open it");
        // Quest points add to the milestones' (and never go below zero).
        check(com.hex.kagune.control.graft.GraftMutation.available(0, 0, 2) == 2
            && com.hex.kagune.control.graft.GraftMutation.available(500, 0, 0) == 1
            && com.hex.kagune.control.ukaku.UkakuMutation.available(0, 0, -3) == 0, "quest points count with the milestones'");
    }

    static void hunts() {
        Random random = new Random(42L);
        Set<Integer> templates = new HashSet<>();
        for (int i = 0; i < 400; i++) {
            Hunts.Spec spec = Hunts.draw(random, false);
            templates.add(spec.template());
            Quest quest = Hunts.quest(spec);
            check(quest.line() == Quest.Line.HUNT && quest.objectives().size() == 1 && quest.objectives().get(0).count() == spec.count(),
                "a hunt is a one-objective quest: " + spec.save());
            check(spec.equals(Hunts.Spec.load(spec.save())), "a hunt's spec round-trips: " + spec.save());
            check(Hunts.quest(spec).id().equals(quest.id()), "the same spec is the same hunt");
            check(!Hunts.payText(spec).isBlank(), "a hunt says what it pays");
            check(!quest.title().contains("own") || spec.template() != 7, "no ghoul hunt while no ghouls roam");
        }
        check(templates.size() >= 6, "hunts vary: " + templates.size() + " kinds drawn");
        Random a = new Random(7L);
        Random b = new Random(7L);
        check(Hunts.draw(a, true).equals(Hunts.draw(b, true)), "the same roll, the same hunt");
        QuestLog log = new QuestLog();
        Hunts.Spec spec = new Hunts.Spec(1, 0, 2, Hunts.PAY_MASTERY, 20);
        log.startHunt(spec, 100L);
        check(log.expire(99L).isEmpty() && log.hunts().size() == 1, "on until its time");
        check(log.expire(100L).size() == 1 && log.hunts().isEmpty(), "gone at dawn");
    }

    /** 1.3.29: no quest comes by itself unless the path needs it; a teacher's only from the teacher; feats show once done. */
    static void givers() {
        check(QuestBook.giver(QuestBook.get("rizes_gift")) == QuestBook.Giver.PATH && QuestBook.giver(QuestBook.get("anteiku")) == QuestBook.Giver.PATH,
            "the path's own ways on are the path's");
        for (String id : new String[] {"light_feet", "up_we_go", "witness_this"}) {
            check(QuestBook.giver(QuestBook.get(id)) == QuestBook.Giver.RETIRED, id + " is retired: Eto's lesson teaches the same");
        }
        for (String id : new String[] {"first_meal", "night_stalker", "the_brink", "rampage", "die_hard", "cannibal", "ghoul_eater"}) {
            check(QuestBook.giver(QuestBook.get(id)) == QuestBook.Giver.FEAT, id + " is a feat");
        }
        for (String id : new String[] {"among_our_own", "a_friend", "cold_blood", "errands", "side_by_side"}) {
            check(QuestBook.giver(QuestBook.get(id)) == QuestBook.Giver.GHOULS, id + " is the ghouls'");
        }
        for (com.hex.kagune.control.npc.EtoRules.Lesson lesson : com.hex.kagune.control.npc.EtoRules.Lesson.values()) {
            check(QuestBook.giver(QuestBook.get(lesson.quest)) == QuestBook.Giver.ETO, lesson + "'s quest is Eto's to give");
        }
        check(QuestBook.giver(Hunts.quest(new Hunts.Spec(0, 4, 3, Hunts.PAY_MEAT, 1))) == QuestBook.Giver.ASKED, "a hunt is given when asked for");
        int teachers = 0;
        for (Quest quest : QuestBook.all()) {
            QuestBook.Giver giver = QuestBook.giver(quest);
            check(!QuestBook.from(quest).isEmpty() || giver == QuestBook.Giver.PATH, quest.id() + ": the journal says who it is from");
            if (giver == QuestBook.Giver.TEACHER) {
                teachers++;
                com.hex.kagune.control.npc.Teaching.Lesson lesson = QuestBook.lesson(quest);
                check(lesson != null && !quest.opens(lesson.gate()) && QuestBook.from(quest).contains(com.hex.kagune.control.npc.Persona.of(lesson.teacher()).name()),
                    quest.id() + ": a teacher's practice, after their lesson (which opens the gate, not the quest)");
            }
            if (giver == QuestBook.Giver.PATH) {
                check(new QuestLog().given(quest), quest.id() + ": the path's own quests need no one to give them (the way on is never hidden)");
            }
        }
        check(teachers >= 8 && QuestBook.lesson(QuestBook.get("crystal_rain")) == com.hex.kagune.control.npc.Teaching.byId("touka_gatling")
            && QuestBook.lesson(QuestBook.get("thousand_minus_seven")) == com.hex.kagune.control.npc.Teaching.byId("ken_centipede"),
            "the quests whose gates the companions teach are theirs");
        QuestLog log = new QuestLog();
        Quest feet = QuestBook.get("light_feet");
        check(log.state(feet, KagunePath.RINKAKU, 1000) == QuestLog.State.LOCKED && !log.shown(feet, QuestLog.State.LOCKED),
            "Light on Your Feet never comes by itself any more");
        log.apply(QuestEvent.of(Signal.FLIP, 1), KagunePath.RINKAKU, 1000);
        check(log.progress(feet, 1) == 0, "nor counts");
        Quest kick = QuestBook.get("eto_kick");
        check(log.state(kick, KagunePath.RINKAKU, 1000) == QuestLog.State.LOCKED && !log.shown(kick, QuestLog.State.LOCKED), "Eto's waits for her, unlisted");
        log.bond(com.hex.kagune.control.npc.EtoRules.KEY, QuestLog.Bond.NONE.with(com.hex.kagune.control.npc.EtoRules.TAUGHT_KICK, true));
        check(log.state(kick, KagunePath.RINKAKU, 0) == QuestLog.State.OPEN && log.shown(kick, QuestLog.State.OPEN) && kick.title().equals("Light on Your Feet"),
            "asked of Eto, Light on Your Feet is given (hers)");
        check(QuestBook.openersText(Gate.EYES_KICK).split("Light on Your Feet", -1).length == 2, "the gate's ways name the lesson once (not the retired one)");
        Quest meal = QuestBook.get("first_meal");
        check(log.state(meal, KagunePath.RINKAKU, 0) == QuestLog.State.OPEN && !log.shown(meal, QuestLog.State.OPEN), "a feat counts unlisted");
        log.complete(meal);
        check(log.shown(meal, log.state(meal, KagunePath.RINKAKU, 0)), "and shows once done");
        Quest among = QuestBook.get("among_our_own");
        log.bond("eto", log.bond("eto").with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        check(log.state(among, KagunePath.RINKAKU, 0) == QuestLog.State.LOCKED, "the ghouls' quests wait for a ghoul (a companion met is not one)");
        log.bond("9f3c2a7e-0000-4000-8000-000000000001", QuestLog.Bond.NONE.with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        check(log.state(among, KagunePath.RINKAKU, 0) == QuestLog.State.OPEN, "talked to a ghoul of the night, they give theirs");
        Quest rain = QuestBook.get("crystal_rain");
        QuestLog ukaku = new QuestLog();
        check(ukaku.state(rain, KagunePath.UKAKU, 1000) == QuestLog.State.LOCKED, "a teacher's practice waits for the teacher");
        ukaku.grant(Gate.UKAKU_GATLING);
        check(ukaku.state(rain, KagunePath.UKAKU, 1000) == QuestLog.State.OPEN, "taught, the practice is given");
        QuestLog old = QuestLog.load("v=2;p.light_feet=12,3");
        check(old.state(feet, KagunePath.RINKAKU, 1000) == QuestLog.State.OPEN && old.shown(feet, QuestLog.State.OPEN),
            "a retired quest already begun is kept going (no save loses its work)");
    }

    /** 1.3.29: asked for what they know already, a teacher says so and moves on. */
    static void taughtAlready() {
        java.util.function.Predicate<Gate> open = gate -> gate == Gate.UKAKU_CRYSTAL || gate == Gate.UKAKU_GATLING;
        List<com.hex.kagune.control.npc.Teaching.Lesson> known = com.hex.kagune.control.npc.Teaching.knownElsewhere("touka", KagunePath.UKAKU, open, 0L);
        check(known.size() == 2 && known.get(0).id().equals("touka_crystal"), "the gates open some other way are the ones known");
        long told = com.hex.kagune.control.npc.Teaching.bit(known.get(0));
        check(com.hex.kagune.control.npc.Teaching.knownElsewhere("touka", KagunePath.UKAKU, open, told).size() == 1, "told once, never told again");
        check(com.hex.kagune.control.npc.Teaching.knownElsewhere("touka", KagunePath.RINKAKU, open, 0L).isEmpty(), "only the lessons for their path");
        Set<Long> bits = new HashSet<>();
        for (com.hex.kagune.control.npc.Teaching.Lesson lesson : com.hex.kagune.control.npc.Teaching.ALL) {
            long bit = com.hex.kagune.control.npc.Teaching.bit(lesson);
            check(bit != 0L, lesson.id() + " has a bit");
            bits.add(bit * 31L + lesson.teacher().hashCode());
        }
        check(bits.size() == com.hex.kagune.control.npc.Teaching.ALL.size(), "each lesson its own bit (per teacher)");
        check(com.hex.kagune.control.npc.Teaching.names(List.of("A")).equals("A") && com.hex.kagune.control.npc.Teaching.names(List.of("A", "B")).equals("A and B")
            && com.hex.kagune.control.npc.Teaching.names(List.of("A", "B", "C")).equals("A, B and C"), "names run on as a sentence");
        for (String teacher : new String[] {"eto", "touka", "ken"}) {
            for (int seed = 0; seed < 4; seed++) {
                String line = com.hex.kagune.control.npc.Teaching.alreadyKnown(teacher, List.of("Gatling"), seed);
                check(line.contains("Gatling") && line.toLowerCase(java.util.Locale.ROOT).contains("already"), teacher + " says it is already known");
            }
        }
    }

    /** 1.3.29: the journal's Romance tab: each companion's card, sent with the log. */
    static void romance() {
        List<com.hex.kagune.control.npc.RomanceCard> cards = new ArrayList<>();
        com.hex.kagune.control.npc.Romance.Stage[] stages = com.hex.kagune.control.npc.Romance.Stage.values();
        com.hex.kagune.control.npc.EtoRules.Step[] steps = com.hex.kagune.control.npc.EtoRules.Step.values();
        String[] banned = {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"};
        Random random = new Random(1329);
        for (int i = 0; i < 400; i++) {
            String key = new String[] {"eto", "touka", "ken"}[i % 3];
            com.hex.kagune.control.npc.RomanceCard card = new com.hex.kagune.control.npc.RomanceCard(key, random.nextInt(5) > 0, stages[random.nextInt(stages.length)],
                random.nextInt(201) - 100, random.nextBoolean(), random.nextInt(4) == 0 ? "Somebody" : null, steps[random.nextInt(steps.length)],
                random.nextInt(11), random.nextInt(9), random.nextInt(4), random.nextBoolean(), random.nextBoolean(), random.nextInt(6));
            cards.add(card);
            String next = card.next();
            String stage = card.stageText();
            check(next != null && !next.isBlank() && stage != null && !stage.isBlank() && !card.errands().isBlank(), key + ": every card says where it stands and what's next");
            check(card.regardShare() >= 0.0F && card.regardShare() <= 1.0F, "the regard bar stays in its box");
            for (String name : banned) {
                check(!(next + stage + card.errands()).toLowerCase(java.util.Locale.ROOT).contains(name), "a card never names " + name);
            }
            if (!card.met()) {
                check(next.contains(card.persona().home()), "not met: where to find them");
            }
            if (card.met() && card.stage() == com.hex.kagune.control.npc.Romance.Stage.MARRIED && !card.home()) {
                check(next.contains("This is our home now."), "married with no home: how to make one");
            }
        }
        List<com.hex.kagune.control.npc.RomanceCard> back = com.hex.kagune.control.npc.RomanceCard.load(com.hex.kagune.control.npc.RomanceCard.save(cards));
        check(back.equals(cards), "the cards come through the packet as they were");
        check(com.hex.kagune.control.npc.RomanceCard.load("bad\nnobody\t1\n").isEmpty() && com.hex.kagune.control.npc.RomanceCard.load(null).isEmpty(),
            "a bad card is left out");
    }
}
