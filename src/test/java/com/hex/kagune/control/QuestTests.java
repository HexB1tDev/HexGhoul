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
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Quest tests: " + passed + " checks passed");
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
            check(!openers.isEmpty(), gate.id() + ": some quest opens it");
            for (Quest opener : openers) {
                check(opener.path() == gate.path() || opener.path() == null, gate.id() + ": opened on its own path by " + opener.id());
                check(gate.mutation() ? gate.mastery() == 0 : opener.mastery() <= gate.mastery(), gate.id() + ": " + opener.id() + " starts before the gate's mastery");
            }
            check(!QuestBook.openersText(gate).isBlank(), gate.id() + ": names its quests");
            check(Gate.fromId(gate.id()) == gate, gate.id() + ": found by id");
        }
        for (KagunePath path : new KagunePath[] {KagunePath.KANEKI, KagunePath.AYATO, KagunePath.FURUTA}) {
            long own = QuestBook.all().stream().filter(quest -> quest.path() == path).count();
            check(own >= 6, path + ": a line of its own (" + own + " quests)");
            long gates = java.util.Arrays.stream(Gate.values()).filter(gate -> gate.path() == path).count();
            check(gates >= 4, path + ": at least four gates of its own");
        }
        // Rival ways: most gates of the paths have two quests opening them.
        int two = 0;
        int pathGates = 0;
        for (Gate gate : Gate.values()) {
            if (gate.path() != null) {
                pathGates++;
                two += QuestBook.openers(gate).size() >= 2 ? 1 : 0;
            }
        }
        check(two * 2 >= pathGates, "most path gates have two ways in (" + two + " of " + pathGates + ")");
    }

    /** Locked until the path and mastery fit, open while counting, met when every objective is, done once paid. */
    static void states() {
        QuestLog log = new QuestLog();
        Quest gift = QuestBook.get("rizes_gift");
        check(log.state(gift, KagunePath.AYATO, 1000) == QuestLog.State.LOCKED, "another path's quest is locked");
        check(log.state(gift, KagunePath.KANEKI, gift.mastery() - 1) == QuestLog.State.LOCKED, "locked below its mastery");
        check(log.state(gift, KagunePath.KANEKI, gift.mastery()) == QuestLog.State.OPEN, "open at its mastery");
        // Locked quests do not count.
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.KANEKI, 0);
        check(log.progress(gift, 0) == 0, "a locked quest doesn't count");
        for (int i = 0; i < 20; i++) {
            log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE, Tags.HOSTILE), KagunePath.KANEKI, 300);
        }
        check(log.progress(gift, 0) == gift.objectives().get(0).count(), "counts up to its objective and no further");
        check(!log.met(gift), "not met with an objective left");
        log.apply(QuestEvent.of(Signal.EAT, 1, Tags.item("minecraft:bread")), KagunePath.KANEKI, 300);
        check(log.progress(gift, 1) == 0, "an event without the tag doesn't count");
        for (int i = 0; i < 5; i++) {
            log.apply(QuestEvent.of(Signal.EAT, 1, Tags.FLESH), KagunePath.KANEKI, 300);
        }
        check(log.met(gift), "met with every objective");
        check(!log.open(Gate.KANEKI_TWIN), "the gate stays shut until it is paid");
        log.complete(gift);
        check(log.state(gift, KagunePath.KANEKI, 300) == QuestLog.State.DONE && log.open(Gate.KANEKI_TWIN), "done, and its gate open");
        int before = log.progress(gift, 0);
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.KANEKI, 300);
        check(log.progress(gift, 0) == before, "a done quest counts no more");
        // A quest that comes after another waits for it.
        Quest soar = QuestBook.get("up_we_go");
        check(log.state(soar, KagunePath.KANEKI, 1000) == QuestLog.State.LOCKED, "waits for the quest it comes after");
        log.complete(QuestBook.get("light_feet"));
        check(log.state(soar, KagunePath.KANEKI, 1000) == QuestLog.State.OPEN, "open once that one is done");
        // Best-of objectives keep the highest amount.
        Quest rampage = QuestBook.get("rampage");
        log.apply(QuestEvent.of(Signal.RAMPAGE, 3), KagunePath.KANEKI, 1000);
        log.apply(QuestEvent.of(Signal.RAMPAGE, 2), KagunePath.KANEKI, 1000);
        check(log.progress(rampage, 0) == 3, "a rampage objective keeps its best streak");
        log.apply(QuestEvent.of(Signal.RAMPAGE, 9), KagunePath.KANEKI, 1000);
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
        check(log.state(anteiku, KagunePath.KANEKI, 1000) == QuestLog.State.CLOSED, "closed for good");
        check(log.side(QuestBook.PACK) == 1 && log.side(QuestBook.ANTEIKU) == 0, "the side taken is remembered");
        log.apply(QuestEvent.of(Signal.RESTRAIN, 1), KagunePath.KANEKI, 1000);
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
            log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.KANEKI, 300);
        }
        log.complete(QuestBook.get("pack"));
        log.complete(QuestBook.get("cannibal"));
        Hunts.Spec spec = new Hunts.Spec(0, 4, 3, Hunts.PAY_MEAT, 1);
        log.startHunt(spec, 12345L);
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.mob("minecraft:witch")), KagunePath.KANEKI, 300);
        log.nextHuntAt(99L);
        QuestLog back = QuestLog.load(log.save());
        Quest gift = QuestBook.get("rizes_gift");
        check(back.progress(gift, 0) == 7, "progress survives the save");
        check(back.state(QuestBook.get("pack"), KagunePath.KANEKI, 1000) == QuestLog.State.DONE, "done survives");
        check(back.state(QuestBook.get("anteiku"), KagunePath.KANEKI, 1000) == QuestLog.State.CLOSED, "closed survives");
        check(back.open(Gate.EYES_KICK) && back.open(Gate.KANEKI_FOUR), "gates survive");
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
        check(log.open(Gate.KANEKI_TWIN) && log.open(Gate.KANEKI_FOUR) && !log.open(Gate.KANEKI_CENTIPEDE), "what was reached stays open");
        log.migrate(gate -> true);
        check(!log.open(Gate.KANEKI_CENTIPEDE), "migration happens once");
        log.reset();
        check(log.gates().isEmpty() && log.migrated() == QuestLog.VERSION, "a reset forgets everything and never migrates again");
    }

    /** Which gate each form, stage and mutation is behind, and the stages a gate leaves usable. */
    static void gates() {
        check(com.hex.kagune.control.quest.Gates.kaneki(com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED) == null, "the seed is never gated");
        check(com.hex.kagune.control.quest.Gates.kaneki(com.hex.kagune.gameplay.KaguneForm.RINKAKU_DUAL) == Gate.KANEKI_TWIN
            && com.hex.kagune.control.quest.Gates.kaneki(com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD) == Gate.KANEKI_FOUR
            && com.hex.kagune.control.quest.Gates.kaneki(com.hex.kagune.gameplay.KaguneForm.CENTIPEDE) == Gate.KANEKI_CENTIPEDE
            && com.hex.kagune.control.quest.Gates.kaneki(com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA) == Gate.KANEKI_FULL_KAKUJA,
            "every Kaneki form past the seed has its gate");
        check(com.hex.kagune.control.quest.Gates.furuta(com.hex.kagune.control.furuta.FurutaStage.TRANSPLANT) == null, "the transplant is never gated");
        for (com.hex.kagune.control.furuta.FurutaStage stage : com.hex.kagune.control.furuta.FurutaStage.values()) {
            Gate gate = com.hex.kagune.control.quest.Gates.furuta(stage);
            check(stage == com.hex.kagune.control.furuta.FurutaStage.TRANSPLANT || gate != null && gate.path() == KagunePath.FURUTA
                && gate.mastery() == stage.mastery(), stage + ": its gate, at its mastery");
        }
        java.util.function.Predicate<Gate> onlyCentipede = gate -> gate == Gate.FURUTA_CENTIPEDE;
        check(com.hex.kagune.control.quest.Gates.furutaUsable(com.hex.kagune.control.furuta.FurutaStage.DRAGON, 1000, onlyCentipede)
            == com.hex.kagune.control.furuta.FurutaStage.CENTIPEDE, "a stage whose quest isn't done falls back to the highest that is open");
        check(com.hex.kagune.control.quest.Gates.furutaUsable(com.hex.kagune.control.furuta.FurutaStage.THORNS, 1000, gate -> false)
            == com.hex.kagune.control.furuta.FurutaStage.TRANSPLANT, "nothing open: the transplant");
        check(com.hex.kagune.control.quest.Gates.furutaUsable(com.hex.kagune.control.furuta.FurutaStage.DRAGON, 1000, gate -> true)
            == com.hex.kagune.control.furuta.FurutaStage.DRAGON, "everything open: what was picked");
        check(com.hex.kagune.control.quest.Gates.furutaNext(com.hex.kagune.control.furuta.FurutaStage.CENTIPEDE, 1000, onlyCentipede)
            == com.hex.kagune.control.furuta.FurutaStage.TRANSPLANT, "K held skips the stages waiting for quests");
        check(com.hex.kagune.control.quest.Gates.furuta(com.hex.kagune.control.furuta.FurutaMutation.RIZES_APPETITE) == Gate.FURUTA_APPETITE
            && com.hex.kagune.control.quest.Gates.furuta(com.hex.kagune.control.furuta.FurutaMutation.WASHUU_BLOOD) == null
            && com.hex.kagune.control.quest.Gates.ayato(com.hex.kagune.control.ayato.AyatoMutation.BURSTING_SHARDS) == Gate.AYATO_BURSTING_SHARDS
            && com.hex.kagune.control.quest.Gates.ayato(com.hex.kagune.control.ayato.AyatoMutation.NOT_THAT_EASILY) == Gate.AYATO_NOT_THAT_EASILY
            && com.hex.kagune.control.quest.Gates.kaneki(com.hex.kagune.gameplay.MutationNode.CENTIPEDE_BODY) == null,
            "the gated mutations (1.0.41: the Kaneki path has none left)");
        // 1.0.41: Furuta's revive on every path that has it, behind the same quest: the same objectives, the same mastery.
        Quest furuta = QuestBook.get("not_that_easily");
        for (String id : new String[] {"kaneki_not_that_easily", "ayato_not_that_easily"}) {
            Quest copy = QuestBook.get(id);
            check(copy != null && copy.mastery() == furuta.mastery() && copy.title().equals(furuta.title())
                && copy.objectives().equals(furuta.objectives()), id + ": Furuta's quest, objective for objective");
        }
        check(QuestBook.get("arrow_eater") == null && QuestBook.openers(Gate.KANEKI_NOT_THAT_EASILY).size() == 1
            && QuestBook.openers(Gate.AYATO_NOT_THAT_EASILY).size() == 1, "the Kaneki path's mutation quest is gone; the revive's quests open it");
        // Quest points add to the milestones' (and never go below zero).
        check(com.hex.kagune.control.furuta.FurutaMutation.available(0, 0, 2) == 2
            && com.hex.kagune.control.furuta.FurutaMutation.available(500, 0, 0) == 1
            && com.hex.kagune.control.ayato.AyatoMutation.available(0, 0, -3) == 0, "quest points count with the milestones'");
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
}
