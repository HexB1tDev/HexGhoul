package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoInitiative;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.EtoTopics;
import com.hex.kagune.control.npc.EtoVoice;
import com.hex.kagune.control.npc.HandSway;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.Script;
import com.hex.kagune.control.npc.Teaching;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestLog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 1.3.13 on: the companions (pure rules, no game): hands held alive; the personas every one-of-a-kind NPC is built from (Eto, Touka,
 * Ken), their lines, their lessons.
 */
public final class CompanionTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        hands();
        personas();
        lessons();
        talk();
        courtship();
        proofs();
        tolerance();
        moods();
        moodKisses();
        wed();
        poses();
        ken();
        touka();
        toukaWingsShow();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Companion tests: " + passed + " checks passed");
    }

    /** 1.3.19: Ken fights in Peace, every one of its moves his, with Eto's fists, kick, flips and a dash; Touka and Eto never wear it. */
    static void ken() {
        check(com.hex.kagune.control.npc.Persona.KEN.finalKakuja() == com.hex.kagune.control.king.KingForm.PEACE, "Ken fights in Peace");
        check(com.hex.kagune.control.npc.Persona.ETO.finalKakuja() == com.hex.kagune.control.king.KingForm.NONE
            && com.hex.kagune.control.npc.Persona.TOUKA.finalKakuja() == com.hex.kagune.control.king.KingForm.NONE, "nobody else wears a final kakuja");
        check(com.hex.kagune.control.npc.Persona.KEN.health() > com.hex.kagune.control.npc.Persona.ETO.health(), "Ken is the strongest of them");
        for (int slot = 0; slot < 6; slot++) {
            com.hex.kagune.control.npc.KenRules.Act act = com.hex.kagune.control.npc.KenRules.ability(slot);
            check(act != null && com.hex.kagune.control.npc.KenRules.slot(act) == slot, "Peace's key " + slot + " is one of his acts and back");
            int ticks = com.hex.kagune.control.king.PeaceRules.Ability.bySlot(slot).cooldown();
            check(com.hex.kagune.control.npc.KenRules.cooldown(ticks) < ticks, "his wait for key " + slot + " is shorter than a player's");
        }
        // Over many fights, every move of his comes up.
        java.util.EnumSet<com.hex.kagune.control.npc.KenRules.Act> seen = java.util.EnumSet.noneOf(com.hex.kagune.control.npc.KenRules.Act.class);
        java.util.Random random = new java.util.Random(19);
        long now = 10_000L;
        for (int i = 0; i < 20_000; i++) {
            long[] w = new long[11];
            for (int k = 0; k < w.length; k++) {
                w[k] = random.nextDouble() < 0.5 ? 0L : now + 100L;
            }
            com.hex.kagune.control.npc.KenRules.Ready ready = new com.hex.kagune.control.npc.KenRules.Ready(w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7],
                w[8], w[9], w[10]);
            double gap = random.nextDouble() * 24.0;
            com.hex.kagune.control.npc.KenRules.Act act = com.hex.kagune.control.npc.KenRules.choose(gap, random.nextDouble() < 0.9,
                random.nextDouble() < 0.85, (float)random.nextDouble(), random.nextBoolean(), random.nextInt(4), now, ready, random.nextDouble());
            seen.add(act);
            int slot = com.hex.kagune.control.npc.KenRules.slot(act);
            check(slot < 0 || w[slot] <= now, "he never throws an ability that isn't ready (" + act + ")");
            check(act != com.hex.kagune.control.npc.KenRules.Act.STRIKE || gap <= com.hex.kagune.control.npc.KenRules.REACH,
                "his blades' chain only in reach");
            check(act != com.hex.kagune.control.npc.KenRules.Act.HORIZON || gap <= com.hex.kagune.control.king.PeaceRules.HORIZON_RANGE,
                "the Final Horizon only on what is in its reach");
            check(act != com.hex.kagune.control.npc.KenRules.Act.WALTZ || gap <= com.hex.kagune.control.king.PeaceRules.WALTZ_RANGE,
                "the Waltz only on what is in its reach");
        }
        check(seen.size() == com.hex.kagune.control.npc.KenRules.Act.values().length, "every one of his moves comes up in his fights (" + seen + ")");
        // Peace comes over him at once: the very first thing in a fight, whatever the distance (his server side wears it before any act).
        check(com.hex.kagune.control.npc.KenRules.OFF_AFTER >= 100L, "it stays on him a while after a fight, never flickering off between blows");
    }

    /** 1.3.19: Touka's wings: a lesser pair, crystal spikes (a volley, a fan, the barrage), her fists up close; only hers. */
    static void touka() {
        check(com.hex.kagune.control.npc.Persona.TOUKA.wings() && !com.hex.kagune.control.npc.Persona.ETO.wings()
            && !com.hex.kagune.control.npc.Persona.KEN.wings(), "only Touka fights with wings of her own");
        check(com.hex.kagune.control.npc.WingRules.MASTERY < com.hex.kagune.control.ukaku.WingForm.KAKUJA.requiredMastery()
            && com.hex.kagune.control.npc.WingRules.MASTERY > com.hex.kagune.control.ukaku.WingForm.TWIN.requiredMastery(), "her wing is a lesser one");
        check(com.hex.kagune.control.npc.WingRules.SHOT < 6.0F && com.hex.kagune.control.npc.WingRules.BARRAGE < com.hex.kagune.control.npc.WingRules.SHOT,
            "her spikes sting; nowhere near Ken's blades");
        java.util.EnumSet<com.hex.kagune.control.npc.WingRules.Act> seen = java.util.EnumSet.noneOf(com.hex.kagune.control.npc.WingRules.Act.class);
        java.util.Random random = new java.util.Random(7);
        long now = 5_000L;
        for (int i = 0; i < 5_000; i++) {
            double gap = random.nextDouble() * 26.0;
            boolean sees = random.nextDouble() < 0.9;
            long shot = random.nextBoolean() ? 0L : now + 5L;
            long fan = random.nextBoolean() ? 0L : now + 50L;
            long barrage = random.nextBoolean() ? 0L : now + 90L;
            com.hex.kagune.control.npc.WingRules.Act act = com.hex.kagune.control.npc.WingRules.choose(gap, sees, now, shot, fan, barrage, random.nextDouble());
            seen.add(act);
            check(act == com.hex.kagune.control.npc.WingRules.Act.NONE || gap >= com.hex.kagune.control.npc.WingRules.NEAR && sees,
                "her spikes only at what she sees, never point blank (her fists there)");
            check(act != com.hex.kagune.control.npc.WingRules.Act.BARRAGE || barrage <= now, "the barrage only when it is ready");
            check(act != com.hex.kagune.control.npc.WingRules.Act.FAN || fan <= now, "the fan only when it is ready");
        }
        check(seen.size() == com.hex.kagune.control.npc.WingRules.Act.values().length, "every one of her wing moves comes up (" + seen + ")");
        check(Math.abs(com.hex.kagune.control.npc.WingRules.fan(0, 5) + com.hex.kagune.control.npc.WingRules.FAN_SPREAD) < 1.0E-9
            && Math.abs(com.hex.kagune.control.npc.WingRules.fan(2, 5)) < 1.0E-9, "the fan spreads evenly about her aim");
    }

    static void hands() {
        // Standing still: she rests at their side, the hands sway anyway (never still for long).
        HandSway still = new HandSway();
        for (int i = 0; i < 200; i++) {
            still.step(0.0, 0.0);
        }
        check(Math.abs(still.trail) < 1.0E-6 && Math.abs(still.out) < 1.0E-6, "standing still, she rests right at their side");
        double low = Double.MAX_VALUE;
        double high = -Double.MAX_VALUE;
        for (int t = 0; t < 400; t++) {
            double sway = HandSway.sway(t);
            low = Math.min(low, sway);
            high = Math.max(high, sway);
        }
        check(high - low > 0.15 && high <= HandSway.SWAY + 1.0E-9 && low >= -HandSway.SWAY - 1.0E-9,
            "the joined hands sway forward and back, about a dozen degrees all told, never past their bound: " + low + ".." + high);

        // Walking: she trails behind, settled, about a third of a block; sprinting further; never past the bound.
        HandSway walk = new HandSway();
        double most = 0.0;
        for (int i = 0; i < 200; i++) {
            walk.step(0.216, 0.0);
            most = Math.max(most, walk.trail);
        }
        check(walk.trail > 0.3 && walk.trail < 0.5, "walking, she trails a little behind, pulled by the hand: " + walk.trail);
        check(most <= HandSway.MAX_TRAIL + 0.15, "the spring never throws her past its bound: " + most);
        HandSway run = new HandSway();
        for (int i = 0; i < 200; i++) {
            run.step(0.28, 0.0);
        }
        check(run.trail > walk.trail && run.trail <= HandSway.MAX_TRAIL + 1.0E-9, "sprinting, a little further behind, still within reach: " + run.trail);

        // Stopping: her own momentum carries her a touch ahead, then she settles back at their side.
        double ahead = 0.0;
        for (int i = 0; i < 120; i++) {
            walk.step(0.0, 0.0);
            ahead = Math.min(ahead, walk.trail);
        }
        check(ahead < -0.01, "stopping, she swings a step ahead before she settles (her momentum): " + ahead);
        check(Math.abs(walk.trail) < 0.01, "and settles back at their side: " + walk.trail);

        // Turning: at their right she swings out turning left (the outside) and in turning right.
        HandSway left = new HandSway();
        HandSway right = new HandSway();
        for (int i = 0; i < 12; i++) {
            left.step(0.2, -9.0);
            right.step(0.2, 9.0);
        }
        check(left.out > 0.05 && right.out < -0.05, "turning away from her she swings out wide, toward her she comes in: " + left.out + " / " + right.out);

        // The arms: the holder's swings back as she trails, hers forward, both by the same reach (each half the gap), same sway.
        float[] holder = HandSway.arm(0.4, 0.0, 0.0, true, 1.0);
        float[] her = HandSway.arm(0.4, 0.0, 0.0, false, 1.0);
        check(holder[0] > 0.2F && her[0] < -0.2F && Math.abs(holder[0] + her[0] + 2.0 * HandSway.sway(0.0)) < 1.0E-5,
            "trailing, the holder's arm reaches back and hers forward, meeting halfway: " + holder[0] + " / " + her[0]);
        float[] smaller = HandSway.arm(0.4, 0.0, 0.0, false, 0.9);
        check(smaller[0] < her[0], "a shorter arm turns further to reach its half");
        float[] swayed = HandSway.arm(0.0, 0.0, HandSway.SLOW_QUARTER, true, 1.0);
        float[] swayedHer = HandSway.arm(0.0, 0.0, HandSway.SLOW_QUARTER, false, 1.0);
        check(swayed[0] < -0.05F && Math.abs(swayed[0] - swayedHer[0]) < 1.0E-5, "the sway takes both hands forward together, the same way");
        check(HandSway.pulled(0.6) > HandSway.pulled(0.3) && HandSway.pulled(-0.2) == 0.0 && HandSway.pulled(5.0) <= 0.16 + 1.0E-9,
            "pulled along she leans in a little, more the further she trails, never when ahead");
        HandSway bad = new HandSway();
        bad.step(Double.NaN, 4.0);
        bad.step(0.1, Double.POSITIVE_INFINITY);
        check(Double.isFinite(bad.trail) && Double.isFinite(bad.out), "nothing unreadable ever reaches the spring");
        check(Math.abs(HandSway.reach(50.0, 1.0)) < Math.PI / 2.0, "a gap too long for the arm turns it most of the way, never wraps round");
    }

    /** The build's own name filter (build.py check_names): none of these may reach the jar. */
    static final Pattern FORBIDDEN = Pattern.compile("Kaneki|KANEKI|kaneki|Ayato|AYATO|ayato|Furuta|FURUTA|furuta");

    static void personas() {
        Set<String> keys = new HashSet<>();
        for (Persona p : Persona.ALL) {
            check(keys.add(p.key()) && Persona.of(p.key()) == p && Persona.known(p.key()), p.key() + ": one key, found by it");
            check(!FORBIDDEN.matcher(p.name()).find() && !FORBIDDEN.matcher(p.key()).find(), p.key() + ": its name passes the name filter");
            check(p.size() > 0.7F && p.size() <= 1.0F && p.health() > 100.0F && p.armor() > 0.0, p.key() + ": a sane body");
            for (EtoMood mood : EtoMood.values()) {
                check(p.idles(mood).length > 0, p.key() + ": something to do " + mood);
            }
            check(p.anyIdle().length >= 5 && p.script() != null && p.colour().startsWith("\u00a7"), p.key() + ": idles, words, a colour");
        }
        check(Persona.ALL.get(0) == Persona.ETO && Persona.ALL.indexOf(Persona.TOUKA) == 1 && Persona.ALL.indexOf(Persona.KEN) == 2,
            "the order the talk screen's faces are sent by (-1, -2, -3) never changes");
        check(Persona.of("nobody") == Persona.ETO && !Persona.known("nobody"), "an unknown key falls back to Eto and is not a companion");
        check(Persona.ETO.owlKit() && !Persona.TOUKA.owlKit() && !Persona.KEN.owlKit(), "only Eto fights with the Owl DNA kit");
        check(Persona.ETO.curves() == 1.0F && Persona.ETO.build() == 1.0F && Persona.TOUKA.curves() > 1.2F && Persona.TOUKA.curves() < 1.6F
            && Persona.TOUKA.build() == 1.0F, "Touka on the same female body as Eto, curvier (1.3.17: legs and chest bigger); Eto as she was");
        check(Persona.ETO.female() && Persona.TOUKA.female() && !Persona.KEN.female() && Persona.TOUKA.slim() && Persona.KEN.slim(),
            "Eto and Touka on the female body, Ken a man's; all on the slim arms their skins are made for");
        check(!Persona.ETO.pecks() && Persona.TOUKA.pecks() && Persona.KEN.pecks(), "Touka and Ken give a quick kiss of their own; Eto waits for hers");
        check(Persona.KEN.leads() && !Persona.TOUKA.leads() && !Persona.ETO.leads(), "Ken leans down into a kiss; the women lean up");
        check(Persona.KEN.likes(true) && !Persona.KEN.likes(false) && Persona.TOUKA.likes(false) && Persona.ETO.likes(true),
            "Ken is only interested in women; Eto and Touka in anyone");
        check(Persona.TOUKA.kissing() < Persona.ETO.kissing() && Persona.TOUKA.forward() < 1.0 && Persona.KEN.forward() > 1.0,
            "Touka would rather die than ask first; Ken reaches for his partner more than anyone");
        check(Persona.TOUKA.blushes(EtoGesture.KISS) && Persona.TOUKA.blushes(EtoGesture.HAND) && !Persona.ETO.blushes(EtoGesture.KISS)
            && Persona.ETO.blushes(EtoGesture.BLUSH) && !Persona.TOUKA.blushes(EtoGesture.CROSS), "Touka goes pink at anything sweet, not at a glare");
        check(Persona.TOUKA.errandItem("minecraft:cocoa_beans") && !Persona.TOUKA.errandItem("minecraft:book") && Persona.KEN.errandItem("minecraft:book")
            && Persona.ETO.errandItem("minecraft:book"), "their first errands: beans for Touka, books for Ken and Eto");
        check(Persona.TOUKA.flower("minecraft:rabbit_foot") && !Persona.TOUKA.flower("minecraft:poppy") && Persona.KEN.flower("minecraft:white_tulip")
            && !Persona.KEN.flower("minecraft:poppy") && Persona.ETO.flower("minecraft:poppy"), "their last gifts: a rabbit's foot, a white flower, any flower");
        check(EtoGesture.CROSS.ordinal() == EtoGesture.HAND.ordinal() + 1 && EtoGesture.HAND.ordinal() > EtoGesture.LAUGH.ordinal()
            && EtoGesture.STEAL.ordinal() == EtoGesture.PECK.ordinal() + 1 && EtoGesture.TIDY.ordinal() == EtoGesture.COOK.ordinal() - 1
            && EtoGesture.FLINCH.ordinal() == EtoGesture.COOK.ordinal() + 1 && EtoGesture.AWE.ordinal() == EtoGesture.values().length - 1,
            "new gestures only ever at the end (sent by ordinal)");
        // 1.3.29: Touka's own ways: never round an arm; a kiss on the cheek or a kiss stolen instead; her own poses, not Eto's.
        check(Persona.ETO.clings() && Persona.KEN.clings() && !Persona.TOUKA.clings(), "Touka never winds round an arm");
        check(Persona.TOUKA.unasked(EtoGesture.CLING) == EtoGesture.PECK && Persona.TOUKA.unasked(EtoGesture.DANCE) == EtoGesture.STEAL
            && Persona.ETO.unasked(EtoGesture.CLING) == EtoGesture.CLING, "unasked, Touka kisses a cheek or steals a kiss; Eto clings");
        check(Persona.ETO.style() == 0 && Persona.KEN.style() == 1 && Persona.TOUKA.style() == 2, "each holds someone their own way");
        // 1.3.30: a child in the arms is cradled the whole time it is held (it faded out with its gesture's time, then came back).
        for (double held = 5.0; held < 4000.0; held += 37.0) {
            com.hex.kagune.control.client.animation.RigPose cradle = com.hex.kagune.control.client.animation.EtoPose.cradle(held, held);
            check(cradle.rightArmX < -0.9F && cradle.leftArmX < -0.9F, "still cradling the child after " + held + " ticks");
        }
        check(com.hex.kagune.control.client.animation.EtoPose.cradle(Double.MAX_VALUE, 3.0).rightArmX < -0.9F, "cradling a child picked up long ago");
        check(com.hex.kagune.control.client.animation.EtoPose.cradle(0.0, 0.0).rightArmX > -0.1F, "picked up: the arms ease into it");
        for (int variant = 0; variant < 3; variant++) {
            // 1.3.30 (the owner): waiting for the kiss she only leans in, her arms as they are; the hand comes up once she is kissed.
            com.hex.kagune.control.client.animation.RigPose waiting = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.KISS, 20.0, 2, variant);
            check(waiting != null && waiting.lean > 0.3F && Math.abs(waiting.rightArmX) < 0.3F && Math.abs(waiting.leftArmX) < 0.3F
                && waiting.rightArmHold == 0.0F && waiting.leftArmHold == 0.0F, "Touka waiting for a kiss: leaning in, no arm thrown forward (" + variant + ")");
            com.hex.kagune.control.client.animation.RigPose kissed = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.BLUSH, 20.0, 2, variant);
            check(kissed != null && kissed.rightArmX < -1.5F && kissed.leftArmX > -0.5F && kissed.lean > 0.3F,
                "Touka kissed: one hand up to them, the other at her side, still leaning in (" + variant + ")");
            com.hex.kagune.control.client.animation.RigPose first = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.BLUSH, 0.0, 2, variant);
            check(first != null && first.rightArmX > -0.3F && first.lean >= 0.4F, "the moment she is kissed: her lean kept, her hand not up yet (" + variant + ")");
            com.hex.kagune.control.client.animation.RigPose eto = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.KISS, 20.0, 0, variant);
            check(eto != null && eto.rightArmX > 0.0F && eto.leftArmX > 0.0F, "Eto's kiss: hands behind her back (" + variant + ")");
        }
        com.hex.kagune.control.client.animation.RigPose a = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.BLUSH, 20.0, 2, 0);
        com.hex.kagune.control.client.animation.RigPose b = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.BLUSH, 20.0, 2, 1);
        check(a.rightArmX != b.rightArmX, "her kiss not the same every time");
        com.hex.kagune.control.client.animation.RigPose stealIn = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.STEAL, 12.0, 2, 0);
        com.hex.kagune.control.client.animation.RigPose stealOut = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.STEAL, 50.0, 2, 0);
        check(stealIn.lean > 0.3F && stealOut.rightArmZ > 0.6F && stealOut.leftArmZ < -0.6F && Math.abs(stealOut.headY) > 0.4F,
            "a kiss stolen: in close, then arms crossed, looking away");
    }

    static void lessons() {
        Set<String> ids = new HashSet<>();
        for (Teaching.Lesson lesson : Teaching.ALL) {
            check(ids.add(lesson.id()) && Teaching.byId(lesson.id()) == lesson, lesson.id() + ": one id");
            check(Persona.known(lesson.teacher()) && lesson.path() != null, lesson.id() + ": a teacher and a path");
            check(QuestBook.openers(lesson.gate()).isEmpty(), lesson.gate().id() + ": no quest opens it any more, its teacher does");
            check(Teaching.taught(lesson.gate()) && Teaching.teaching(lesson.gate()) == lesson, lesson.gate().id() + ": taught by one lesson");
            check(!Teaching.gatedText(lesson.gate(), Persona.of(lesson.teacher()).name()).isBlank(), lesson.id() + ": the game says who teaches it");
        }
        for (Quest quest : QuestBook.all()) {
            for (Gate gate : quest.reward().gates()) {
                check(!Teaching.taught(gate), quest.id() + ": opens no gate a companion teaches (" + gate.id() + ")");
            }
        }
        check(Teaching.of("touka").size() == 3 && Teaching.of("ken").size() == 4 && Teaching.of("eto").isEmpty(), "Touka three lessons, Ken four");
        check(Teaching.of("touka").stream().allMatch(l -> l.path() == KagunePath.UKAKU), "Touka teaches the wings only");
        check(Gate.fromId("ukaku_crystal") == Gate.UKAKU_CRYSTAL && Gate.UKAKU_CRYSTAL.mastery() == com.hex.kagune.control.ukaku.UkakuRules.CRYSTAL_MASTERY,
            "Crystal Wings is a gate, at crystal mode's own mastery");
        Set<Gate> open = new HashSet<>();
        Teaching.View human = Teaching.view("touka", false, KagunePath.UKAKU, 1000, open::contains);
        check(!human.ready() && human.refusal() == Teaching.Refusal.HUMAN, "a human has nothing to learn");
        Teaching.View rinkaku = Teaching.view("touka", true, KagunePath.RINKAKU, 1000, open::contains);
        check(!rinkaku.ready() && rinkaku.refusal() == Teaching.Refusal.WRONG_PATH, "Touka knows nothing of tails");
        Teaching.View weak = Teaching.view("touka", true, KagunePath.UKAKU, 100, open::contains);
        check(!weak.ready() && weak.refusal() == Teaching.Refusal.WEAK && weak.next().gate() == Gate.UKAKU_CRYSTAL && weak.need() == 200,
            "too weak: the next lesson named, and the mastery it needs");
        Teaching.View crystal = Teaching.view("touka", true, KagunePath.UKAKU, 250, open::contains);
        check(crystal.ready() && crystal.next().gate() == Gate.UKAKU_CRYSTAL && crystal.next().kind() == Teaching.Kind.TALK, "crystals first, told");
        open.add(Gate.UKAKU_CRYSTAL);
        Teaching.View gatling = Teaching.view("touka", true, KagunePath.UKAKU, 250, open::contains);
        check(!gatling.ready() && gatling.next().gate() == Gate.UKAKU_GATLING && gatling.refusal() == Teaching.Refusal.WEAK, "then the Gatling, at 400");
        open.add(Gate.UKAKU_GATLING);
        open.add(Gate.UKAKU_KAKUJA);
        Teaching.View all = Teaching.view("touka", true, KagunePath.UKAKU, 1000, open::contains);
        check(!all.ready() && all.refusal() == Teaching.Refusal.ALL_TAUGHT && all.next() == null, "all she knows, taught");
        Teaching.View storm = Teaching.view("ken", true, KagunePath.UKAKU, 1000, open::contains);
        check(storm.ready() && storm.next().gate() == Gate.UKAKU_FINAL_KAKUJA && storm.next().kind() == Teaching.Kind.TRIAL,
            "Ken takes the wings the last of the way: lived through");
        Set<Gate> tails = new HashSet<>();
        Teaching.View before = Teaching.view("ken", true, KagunePath.RINKAKU, 800, tails::contains);
        check(!before.ready() && before.refusal() == Teaching.Refusal.BEFORE && before.before() == Gate.RINKAKU_FOUR,
            "the centipede only after the four tails (their quests)");
        tails.add(Gate.RINKAKU_FOUR);
        Teaching.View centipede = Teaching.view("ken", true, KagunePath.RINKAKU, 800, tails::contains);
        check(centipede.ready() && centipede.next().gate() == Gate.RINKAKU_CENTIPEDE && centipede.next().kind() == Teaching.Kind.TRIAL,
            "the centipede: the torture, lived through");
        tails.add(Gate.RINKAKU_CENTIPEDE);
        tails.add(Gate.RINKAKU_FULL_KAKUJA);
        Teaching.View dragon = Teaching.view("ken", true, KagunePath.RINKAKU, 1000, tails::contains);
        check(dragon.ready() && dragon.next().gate() == Gate.RINKAKU_DRAGON, "and last, the Dragon");
    }

    /** 2.2.3: a partner's mood shapes what they do: kisses asked for, kisses and hugs taken. */
    static void moods() {
        for (EtoMood mood : EtoMood.values()) {
            double k = com.hex.kagune.control.npc.MoodActs.kissing(mood);
            check(k >= 0.0 && k <= 3.0, mood + ": a sane share");
        }
        check(com.hex.kagune.control.npc.MoodActs.kissing(EtoMood.SULKY) == 0.0 && com.hex.kagune.control.npc.MoodActs.kissing(EtoMood.JEALOUS) == 0.0
            && com.hex.kagune.control.npc.MoodActs.kissing(EtoMood.LONELY) > 1.5 && com.hex.kagune.control.npc.MoodActs.kissing(EtoMood.SLEEPY) < 1.0,
            "mad or jealous: no kisses; lonely: far more; sleepy: fewer");
        for (Persona p : Persona.ALL) {
            for (int seed = 0; seed < 4; seed++) {
                check(com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.SULKY, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.SULKY, false, seed) != null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.JEALOUS, false, seed) != null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.JEALOUS, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.BLOODTHIRSTY, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.BLOODTHIRSTY, false, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.HAPPY, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.AFFECTIONATE, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.reassured(p.key(), seed).length() > 5, p.key() + ": takes what the mood allows");
            }
        }
    }

    /** 2.3.5: two poses that differ somewhere (arms, head, lean). */
    static boolean differ(RigPose a, RigPose b) {
        float d = Math.abs(a.rightArmX - b.rightArmX) + Math.abs(a.leftArmX - b.leftArmX) + Math.abs(a.rightArmZ - b.rightArmZ)
            + Math.abs(a.leftArmZ - b.leftArmZ) + Math.abs(a.rightArmY - b.rightArmY) + Math.abs(a.headY - b.headY) + Math.abs(a.headZ - b.headZ)
            + Math.abs(a.lean - b.lean) + Math.abs(a.drop - b.drop);
        return d > 0.15F;
    }

    /** 2.2.7: married by command: everything about it full (Courtship.wed). */
    static void wed() {
        com.hex.kagune.control.quest.QuestLog.Bond[] starts = {new com.hex.kagune.control.quest.QuestLog.Bond(0, -1L, 0, 0),
            new com.hex.kagune.control.quest.QuestLog.Bond(-80, 3L, EtoRules.BOOKS_DONE | EtoRules.TAUGHT_SOAR, EtoRules.gifted(
                new com.hex.kagune.control.quest.QuestLog.Bond(0, 0L, 0, 0), 9L).favours() | 2),
            EtoRules.begin(new com.hex.kagune.control.quest.QuestLog.Bond(70, 5L, com.hex.kagune.control.npc.NpcBonds.MET, 0))};
        for (com.hex.kagune.control.quest.QuestLog.Bond start : starts) {
            com.hex.kagune.control.quest.QuestLog.Bond wed = com.hex.kagune.control.npc.Courtship.wed(start);
            check(wed.regard() == com.hex.kagune.control.npc.NpcBonds.MOST, "wed: regard at the most");
            check(wed.has(EtoRules.DATING) && EtoRules.step(wed) == EtoRules.Step.DATING, "wed: past dating's step (the world keeps them married)");
            check(com.hex.kagune.control.npc.Courtship.closed(wed, com.hex.kagune.control.npc.Courtship.CHAPTERS) == com.hex.kagune.control.npc.Courtship.CHAPTERS
                && !com.hex.kagune.control.npc.Courtship.on(wed, com.hex.kagune.control.npc.Courtship.CHAPTERS) && EtoRules.count(wed) == 0,
                "wed: every chapter closed, nothing owing");
            check(wed.has(com.hex.kagune.control.npc.NpcBonds.MET) && wed.has(com.hex.kagune.control.npc.NpcBonds.BEFRIENDED)
                && !wed.has(EtoRules.BOOKS_DONE), "wed: met, befriended, the old errands gone");
            check(wed.has(EtoRules.TAUGHT_SOAR) == start.has(EtoRules.TAUGHT_SOAR) && EtoRules.giftDay(wed) == EtoRules.giftDay(start)
                && wed.chatted() == start.chatted(), "wed: what was taught kept, and the last gift's day");
            check(com.hex.kagune.control.npc.Courtship.wed(wed).equals(wed), "wed twice: the same");
        }
    }

    /** 2.2.4: a kiss for every mood: the same lean-in and hold, each mood its own words, wait, body and after. */
    static void moodKisses() {
        com.hex.kagune.control.npc.MoodKiss.Style[] styles = com.hex.kagune.control.npc.MoodKiss.Style.values();
        check(styles[0] == com.hex.kagune.control.npc.MoodKiss.Style.SWEET && styles.length == 9, "the kinds of kiss (sent by ordinal: sweet first)");
        check(com.hex.kagune.control.npc.MoodKiss.of(EtoMood.SULKY) == com.hex.kagune.control.npc.MoodKiss.Style.ANGRY
            && com.hex.kagune.control.npc.MoodKiss.of(EtoMood.JEALOUS) == com.hex.kagune.control.npc.MoodKiss.Style.POSSESSIVE
            && com.hex.kagune.control.npc.MoodKiss.of(EtoMood.HAPPY) == com.hex.kagune.control.npc.MoodKiss.Style.SWEET
            && com.hex.kagune.control.npc.MoodKiss.of(EtoMood.BORED) == com.hex.kagune.control.npc.MoodKiss.Style.TEASING,
            "angry when sulking, possessive when jealous, sweet when happy, teasing when bored");
        Set<com.hex.kagune.control.npc.MoodKiss.Style> used = new HashSet<>();
        for (EtoMood mood : EtoMood.values()) {
            used.add(com.hex.kagune.control.npc.MoodKiss.of(mood));
        }
        check(used.size() == styles.length, "every kind of kiss comes from some mood");
        for (com.hex.kagune.control.npc.MoodKiss.Style style : styles) {
            int waits = com.hex.kagune.control.npc.MoodKiss.waits(style);
            check(waits >= 40 && waits <= 400, style + ": a sane wait");
            check(com.hex.kagune.control.npc.MoodKiss.dodges(style) == (style == com.hex.kagune.control.npc.MoodKiss.Style.TEASING), style + ": only teasing dodges");
            double hearts = com.hex.kagune.control.npc.MoodKiss.hearts(style);
            check(hearts > 0.0 && hearts <= 2.0, style + ": hearts");
            String waiting = com.hex.kagune.control.npc.MoodKiss.waiting(style, "Touka", "her");
            check(waiting.startsWith("Touka") && waiting.length() > 15, style + ": what the screen says while they wait");
            for (Persona p : Persona.ALL) {
                for (int seed = 0; seed < 4; seed++) {
                    for (int h = 0; h < 3; h++) {
                        String line = com.hex.kagune.control.npc.MoodKiss.kissed(p.key(), style, h == 1, h == 2, seed);
                        check(style == com.hex.kagune.control.npc.MoodKiss.Style.SWEET ? line == null : line != null && line.length() > 10,
                            p.key() + " " + style + ": their words after (sweet keeps the script's)");
                    }
                    check(com.hex.kagune.control.npc.MoodKiss.dodged(p.key(), seed).length() > 10, p.key() + ": a dodge");
                }
            }
            for (EtoGesture gesture : new EtoGesture[] {EtoGesture.KISS, EtoGesture.BLUSH, EtoGesture.PULL}) {
                for (int persona = 0; persona < 3; persona++) {
                    for (double age : new double[] {0.0, 7.0, 20.0, 45.0, 90.0, 200.0}) {
                        RigPose usual = EtoPose.gesture(gesture, age, persona, 0);
                        RigPose moody = com.hex.kagune.control.client.animation.MoodKissPose.apply(usual, style, gesture, age, persona);
                        if (style == com.hex.kagune.control.npc.MoodKiss.Style.SWEET) {
                            check(moody == usual, gesture + ": sweet is the usual kiss");
                            continue;
                        }
                        check(moody != null && moody != usual && Float.isFinite(moody.headX) && Math.abs(moody.headX) < 1.1F,
                            style + " " + gesture + " " + persona + ": a pose");
                        // 2.3.5: never an arm raised to face height turned in across the middle (the partner's face, their camera).
                        check(com.hex.kagune.control.client.animation.MoodKissPose.clear(moody), style + " " + gesture + " persona " + persona
                            + " at " + age + ": the partner's face kept clear (arms " + moody.rightArmX + "/" + moody.rightArmY + ", " + moody.leftArmX + "/"
                            + moody.leftArmY + ")");
                    }
                    RigPose held = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, gesture, 40.0, persona);
                    if (style != com.hex.kagune.control.npc.MoodKiss.Style.SWEET && gesture == EtoGesture.KISS) {
                        check(held.lean > 0.3F && held.lean < 0.75F, style + " " + persona + ": leaning in");
                    }
                }
                if (style != com.hex.kagune.control.npc.MoodKiss.Style.SWEET) {
                    // 2.3.5: each companion their own way, not one copied to all three.
                    RigPose eto = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, gesture, 40.0, 0);
                    RigPose ken = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, gesture, 40.0, 1);
                    RigPose touka = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, gesture, 40.0, 2);
                    if (gesture == EtoGesture.KISS) {
                        check(differ(eto, ken) && differ(eto, touka) && differ(ken, touka), style + ": Eto's, Ken's and Touka's each their own");
                    }
                }
            }
            RigPose other = EtoPose.gesture(EtoGesture.WAVE, 20.0, 0, 0);
            check(com.hex.kagune.control.client.animation.MoodKissPose.apply(other, style, EtoGesture.WAVE, 20.0, 0) == other, style + ": only the kiss is changed");
        }
        // 2.3.5: angry looks angry: the face turned away while waiting, on every one of them; Eto stamps, Touka's fists are clenched.
        for (int persona = 0; persona < 3; persona++) {
            RigPose angry = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, com.hex.kagune.control.npc.MoodKiss.Style.ANGRY,
                EtoGesture.KISS, 30.0, persona);
            check(Math.abs(angry.headY) > 0.3F && angry.lean < 0.45F, persona + ": angry, the face turned away, leaning in grudgingly");
        }
        check(com.hex.kagune.control.client.animation.MoodKissPose.apply(null, com.hex.kagune.control.npc.MoodKiss.Style.ANGRY, EtoGesture.KISS, 2.0, 0)
            .rightLegX < 0.0F, "Eto stamps her foot");
        check(com.hex.kagune.control.npc.MoodKiss.waits(com.hex.kagune.control.npc.MoodKiss.Style.ANGRY) < EtoRules.KISS_WAIT
            && com.hex.kagune.control.npc.MoodKiss.waits(com.hex.kagune.control.npc.MoodKiss.Style.SLEEPY) > EtoRules.KISS_WAIT,
            "the angry won't wait long; the sleepy forget to stop");
        check(com.hex.kagune.control.npc.MoodKiss.after(com.hex.kagune.control.npc.MoodKiss.Style.ANGRY) == EtoGesture.ANGRY
            && com.hex.kagune.control.npc.MoodKiss.after(com.hex.kagune.control.npc.MoodKiss.Style.SWEET) == null, "after: arms crossed when angry; the usual when sweet");
    }

    /** 2.2.0: a partner lets go of a first blow or a missed kiss; only more, close together, put them out; sorry always helps. */
    static void tolerance() {
        com.hex.kagune.control.npc.Tolerance.Reaction shrug = com.hex.kagune.control.npc.Tolerance.Reaction.SHRUG;
        check(com.hex.kagune.control.npc.Tolerance.hit(0, false) == shrug && com.hex.kagune.control.npc.Tolerance.hit(1, true) == shrug
            && com.hex.kagune.control.npc.Tolerance.hit(1, false) == com.hex.kagune.control.npc.Tolerance.Reaction.PUT_OUT
            && com.hex.kagune.control.npc.Tolerance.hit(2, false) == com.hex.kagune.control.npc.Tolerance.Reaction.CROSS,
            "a first blow (a second mid-fight) brushed off; then put out; then cross");
        check(com.hex.kagune.control.npc.Tolerance.missed(0) == shrug && com.hex.kagune.control.npc.Tolerance.missed(1)
            == com.hex.kagune.control.npc.Tolerance.Reaction.PUT_OUT, "a first missed kiss shrugged off");
        for (com.hex.kagune.control.npc.Tolerance.Reaction r : com.hex.kagune.control.npc.Tolerance.Reaction.values()) {
            check(r.sulk <= 1200L && r.regard >= -6, r + ": never the five minutes of before, never a big blow to regard");
        }
        check(com.hex.kagune.control.npc.Tolerance.offences(3, 100L, 100L + com.hex.kagune.control.npc.Tolerance.FORGETS) == 0
            && com.hex.kagune.control.npc.Tolerance.offences(3, 100L, 200L) == 3 && com.hex.kagune.control.npc.Tolerance.offences(3, Long.MIN_VALUE, 0L) == 0,
            "what they hold fades after three minutes");
        check(com.hex.kagune.control.npc.Tolerance.apology(1) == com.hex.kagune.control.npc.Tolerance.Apology.FORGIVEN
            && com.hex.kagune.control.npc.Tolerance.apology(5) == com.hex.kagune.control.npc.Tolerance.Apology.A_MOMENT, "sorry forgives (a moment first, if it keeps happening)");
        for (Persona p : Persona.ALL) {
            for (int seed = 0; seed < 6; seed++) {
                for (String line : new String[] {com.hex.kagune.control.npc.Tolerance.shrug(p.key(), seed), com.hex.kagune.control.npc.Tolerance.shrugKiss(p.key(), seed),
                    com.hex.kagune.control.npc.Tolerance.forgiven(p.key(), seed % 2 == 0, seed), com.hex.kagune.control.npc.Tolerance.putOut(p.key(), seed)}) {
                    check(line != null && line.length() > 3, p.key() + " says it");
                }
            }
        }
    }

    /** 2.1.8: Ken's final kakujas need their proof first (Teaching.proof); everything else is taught as before. */
    static void proofs() {
        for (Teaching.Lesson lesson : Teaching.ALL) {
            Teaching.Proof proof = Teaching.proof(lesson);
            boolean finalOne = lesson.gate() == Gate.RINKAKU_FULL_KAKUJA || lesson.gate() == Gate.RINKAKU_DRAGON || lesson.gate() == Gate.UKAKU_FINAL_KAKUJA;
            check(finalOne == (proof != null), lesson.id() + ": a proof first only for a final kakuja");
            if (proof != null) {
                check(proof.need() >= 3 && !Teaching.fill(proof.ask(), proof, 0).contains("{") && Teaching.fill(proof.progress(), proof, 2).contains("2")
                    && proof.bar(proof.need()).contains("Go and see him") && !proof.bar(1).contains("Go and see"), lesson.id() + ": its proof said cleanly");
            }
        }
        check(Teaching.proof(Teaching.byId("ken_dragon")).rank() >= 6, "the Dragon wants Doves worth fearing");
    }

    /**
     * 2.1.2: each companion's own courtship (Courtship): five chapters asking for different things (no two companions alike), each in its
     * own words; a bond from before read as its chapters; what is handed over taken whole; the first page's words for where they are.
     */
    static void courtship() {
        Set<String> ids = new HashSet<>();
        Set<String> asks = new HashSet<>();
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura", java.util.regex.Pattern.CASE_INSENSITIVE);
        for (Persona p : Persona.ALL) {
            List<com.hex.kagune.control.npc.Courtship.Chapter> chapters = com.hex.kagune.control.npc.Courtship.of(p.key());
            check(chapters.size() == com.hex.kagune.control.npc.Courtship.CHAPTERS, p.key() + " has five chapters");
            Set<com.hex.kagune.control.npc.Courtship.Task> tasks = new HashSet<>();
            for (com.hex.kagune.control.npc.Courtship.Chapter chapter : chapters) {
                check(ids.add(chapter.id()) && chapter.id().startsWith(p.key() + "_"), chapter.id() + ": its own");
                check(asks.add(chapter.ask()), chapter.id() + ": asked in its own words");
                tasks.add(chapter.task());
                check(chapter.need() >= 1 && chapter.need() <= 255, chapter.id() + ": a count the bond can keep");
                check(chapter.task() != com.hex.kagune.control.npc.Courtship.Task.GIVE || !chapter.items().isEmpty() && !chapter.itemName().isEmpty(),
                    chapter.id() + ": what to hand over, named");
                for (String text : new String[] {chapter.title(), chapter.ask(), chapter.progress(), chapter.journal()}) {
                    check(!text.isBlank() && text.length() <= com.hex.kagune.control.npc.scene.SceneBook.LINE_MOST && !banned.matcher(text).find(),
                        chapter.id() + ": a clean line: " + text);
                    String filled = com.hex.kagune.control.npc.Courtship.fill(text, chapter, 1);
                    check(!filled.contains("{"), chapter.id() + ": every count put in: " + filled);
                }
            }
            check(tasks.size() >= 4, p.key() + "'s chapters ask for " + tasks.size() + " kinds of thing");
            // 2.1.5: out of the mod's own world: nothing of the old shared errands' kind (kills of any monster, a sit, ink, beans...).
            for (com.hex.kagune.control.npc.Courtship.Chapter chapter : chapters) {
                check(chapter.task() != com.hex.kagune.control.npc.Courtship.Task.KILLS && chapter.task() != com.hex.kagune.control.npc.Courtship.Task.KILLS_WITH
                    && chapter.task() != com.hex.kagune.control.npc.Courtship.Task.SIT && chapter.task() != com.hex.kagune.control.npc.Courtship.Task.WALK_NIGHT
                    && (chapter.task() != com.hex.kagune.control.npc.Courtship.Task.GIVE || chapter.items().stream().allMatch(i -> i.startsWith("hexkagune:"))),
                    chapter.id() + ": asks for something of the mod's own world");
            }
            // The road: begun, a hand-over taken whole (never past what it needs), its moment closing it, the next on, then asking out.
            QuestLog.Bond bond = com.hex.kagune.control.npc.Courtship.begin(new QuestLog.Bond(70, 0L, NpcBonds.MET, 0));
            check(com.hex.kagune.control.npc.Courtship.chapter(p.key(), bond) == chapters.get(0) && EtoRules.step(bond) == EtoRules.Step.COURTING,
                p.key() + ": begun on chapter one");
            for (int i = 0; i < chapters.size(); i++) {
                com.hex.kagune.control.npc.Courtship.Chapter chapter = chapters.get(i);
                check(com.hex.kagune.control.npc.Courtship.close(p.key(), bond) == bond, chapter.id() + ": nothing closes before its task is done");
                if (chapter.task() == com.hex.kagune.control.npc.Courtship.Task.GIVE) {
                    String item = chapter.items().iterator().next();
                    check(com.hex.kagune.control.npc.Courtship.wants(p.key(), bond, item) && !com.hex.kagune.control.npc.Courtship.wants(p.key(), bond,
                        "minecraft:dirt"), chapter.id() + ": wants what it asks for, only that");
                    check(com.hex.kagune.control.npc.Courtship.takes(p.key(), bond, 64) == chapter.need(), chapter.id() + ": a stack held out: as much as it needs");
                }
                bond = com.hex.kagune.control.npc.Courtship.progress(p.key(), bond, chapter.task(), 300);
                check(EtoRules.count(bond) == chapter.need() && com.hex.kagune.control.npc.Courtship.ready(p.key(), bond), chapter.id() + ": done");
                bond = com.hex.kagune.control.npc.Courtship.close(p.key(), bond);
                check(com.hex.kagune.control.npc.Courtship.closed(bond, chapters.size()) == i + 1 && EtoRules.count(bond) == 0, chapter.id() + ": closed");
            }
            check(EtoRules.step(bond) == EtoRules.Step.ASK && com.hex.kagune.control.npc.Courtship.chapter(p.key(), bond) == null, p.key() + ": then ask");
            // 2.1.5: a Dove below the chapter's rank doesn't count; one at it does.
            for (int i = 0; i < chapters.size(); i++) {
                com.hex.kagune.control.npc.Courtship.Chapter chapter = chapters.get(i);
                if (chapter.rank() > 0) {
                    QuestLog.Bond on = new QuestLog.Bond(70, 0L, NpcBonds.MET | EtoRules.LINE_ON | i << com.hex.kagune.control.npc.Courtship.SHIFT
                        | com.hex.kagune.control.npc.Courtship.FROM_215, 0);
                    check(EtoRules.count(com.hex.kagune.control.npc.Courtship.progress(p.key(), on, chapter.task(), 1, chapter.rank() - 1)) == 0
                        && EtoRules.count(com.hex.kagune.control.npc.Courtship.progress(p.key(), on, chapter.task(), 1, chapter.rank())) == 1,
                        chapter.id() + ": only a Dove of Rank " + chapter.rank() + " or above");
                }
            }
            // 2.1.5: a chapter on from before 2.1.5 starts its count again (it counted something else).
            QuestLog.Bond before = new QuestLog.Bond(70, 0L, NpcBonds.MET | EtoRules.LINE_ON | 1 << com.hex.kagune.control.npc.Courtship.SHIFT, 6);
            QuestLog.Bond now = com.hex.kagune.control.npc.Courtship.migrate(before, 5);
            check(EtoRules.count(now) == 0 && com.hex.kagune.control.npc.Courtship.closed(now, 5) == 1 && com.hex.kagune.control.npc.Courtship.migrate(now, 5) == now,
                p.key() + ": a chapter from before 2.1.5 starts again, once");
            // A bond from before 2.1.2: the errands read as chapters.
            QuestLog.Bond twoDone = new QuestLog.Bond(100, 0L, NpcBonds.MET | EtoRules.LINE_ON | EtoRules.BOOKS_DONE | EtoRules.HUNT_DONE, 7);
            QuestLog.Bond migrated = com.hex.kagune.control.npc.Courtship.migrate(twoDone, 5);
            check(com.hex.kagune.control.npc.Courtship.closed(migrated, 5) == 2 && EtoRules.count(migrated) == 0 && !migrated.has(EtoRules.BOOKS_DONE)
                && EtoRules.step(migrated) == EtoRules.Step.COURTING, p.key() + ": two errands done before: chapter three, from nothing");
            QuestLog.Bond allDone = new QuestLog.Bond(100, 0L, NpcBonds.MET | EtoRules.LINE_ON | EtoRules.BOOKS_DONE | EtoRules.HUNT_DONE
                | EtoRules.FLOWER_DONE, 0);
            check(EtoRules.step(allDone) == EtoRules.Step.ASK && EtoRules.step(com.hex.kagune.control.npc.Courtship.migrate(allDone, 5)) == EtoRules.Step.ASK,
                p.key() + ": every errand done before: still waiting to be asked");
            check(com.hex.kagune.control.npc.Courtship.migrate(bond, 5) == bond, p.key() + ": a bond already kept the new way is left alone");
            // The first page: the courtship's answer, labelled for where they are.
            boolean mixed = true;
            EtoTalk.Context close = context(com.hex.kagune.control.npc.Courtship.BEGIN_AT, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1,
                Teaching.View.NONE, mixed, 6000L);
            check(p.script().main(close, "").choices().contains(NpcTalk.Choice.FAVOUR) && p.script().answer(close, NpcTalk.Choice.FAVOUR).effect()
                == EtoTalk.Effect.BEGIN && p.script().answer(close, NpcTalk.Choice.FAVOUR).line().equals(chapters.get(0).ask()),
                p.key() + ": close enough, the courtship begins in its first chapter's words");
            EtoTalk.Context holding = courting(p, 0, 0, 5);
            EtoTalk.Option give = option(p.script().main(holding, ""), NpcTalk.Choice.FAVOUR);
            boolean firstGives = chapters.get(0).task() == com.hex.kagune.control.npc.Courtship.Task.GIVE;
            check(give != null && (!firstGives || give.label().contains(chapters.get(0).itemName())), p.key() + ": what's held is offered by name");
            check(!firstGives || p.script().answer(holding, NpcTalk.Choice.FAVOUR).effect() == EtoTalk.Effect.COURT, p.key() + ": held out, handed over");
            EtoTalk.Context done = courting(p, 1, chapters.get(1).need(), 0);
            check(option(p.script().main(done, ""), NpcTalk.Choice.FAVOUR).label().equals("I did what you asked.")
                && p.script().answer(done, NpcTalk.Choice.FAVOUR).effect() == EtoTalk.Effect.COURT, p.key() + ": a task done, its moment");
            EtoTalk.Context dating = context(90, Romance.Stage.DATING, null, true, EtoMood.HAPPY, 2, Teaching.View.NONE, mixed, 6000L);
            check(!p.script().main(dating, "").choices().contains(NpcTalk.Choice.FAVOUR), p.key() + ": dating, no chores");
            EtoTalk.Option dates = null;
            for (EtoTalk.Option o : p.script().sweet(dating, "").options()) {
                if (o.key().equals("dates")) {
                    dates = o;
                }
            }
            check(dates != null && p.script().sweet(dating, "").options().size() <= 12, p.key() + ": a partner can ask for a date");
        }
    }

    /** {@code p}'s courtship on chapter {@code closed} + 1 with {@code done} of it done and {@code held} of what it asks for in hand. */
    static EtoTalk.Context courting(Persona p, int closed, int done, int held) {
        int flags = NpcBonds.MET | EtoRules.LINE_ON | closed << com.hex.kagune.control.npc.Courtship.SHIFT;
        return new EtoTalk.Context(80, flags, done, true, false, false, false, false, null, true, false, 4, Romance.Stage.NONE, null, false, 6000L,
            EtoMood.HAPPY, 0L, 0L, null, false, false, false, EtoRules.Pact.NONE, 0, false, false, "Sam", false,
            com.hex.kagune.control.family.FamilyView.NONE, Teaching.View.NONE, true, held);
    }

    static EtoTalk.Option option(EtoTalk.Page page, NpcTalk.Choice choice) {
        for (EtoTalk.Option o : page.options()) {
            if (o.choice() == choice) {
                return o;
            }
        }
        return null;
    }

    /** A talk with {@code persona}: who they are to it, everything else ordinary. */
    static EtoTalk.Context context(int regard, Romance.Stage stage, String claimedBy, boolean ghoul, EtoMood mood, int seed, Teaching.View teaching,
                                   boolean mixed, long dayTime) {
        return new EtoTalk.Context(regard, stage.together() ? EtoRules.DATING : 0, 0, ghoul, false, false, true, true, null, true, false, seed,
            stage, claimedBy, false, dayTime, mood, 3L, 0L, "wolf", true, true, true, EtoRules.Pact.NONE, 0, false, false, "Sam", false,
            com.hex.kagune.control.family.FamilyView.NONE, teaching, mixed);
    }

    static void talk() {
        for (Persona p : new Persona[] {Persona.TOUKA, Persona.KEN}) {
            Script script = p.script();
            Set<String> topicIds = new HashSet<>();
            int words = 0;
            for (int voice = 0; voice < 6; voice++) {
                int regard = new int[] {0, 0, NpcBonds.FRIENDLY, NpcBonds.FRIEND, NpcBonds.MOST, NpcBonds.MOST}[voice];
                Romance.Stage stage = voice == 4 ? Romance.Stage.DATING : voice == 5 ? Romance.Stage.MARRIED : Romance.Stage.NONE;
                String claimed = voice == 0 ? "Alex" : null;
                for (EtoMood mood : EtoMood.values()) {
                    for (int seed = 0; seed < 12; seed++) {
                        for (long time : new long[] {6000L, 18000L}) {
                            EtoTalk.Context c = context(regard, stage, claimed, true, mood, seed, Teaching.view(p.key(), true, KagunePath.UKAKU, 500,
                                gate -> false), true, time);
                            for (String line : new String[] {script.greeting(c), script.approach(c), script.welcome(c), script.title(c),
                                script.kissed(seed, voice == 5), script.hugged(seed), script.handTaken(seed), script.peck(seed), script.babyNight(c)}) {
                                check(line != null && !line.isBlank() && !line.contains("{name}") && !line.contains("{partner}") && !FORBIDDEN.matcher(line).find(),
                                    p.key() + ": a line for every voice and mood (" + voice + ", " + mood + "): " + line);
                                words++;
                            }
                            EtoTalk.Page main = script.main(c, "hi");
                            check(main.options().size() <= com.hex.kagune.control.network.S2CNpcTalk.MOST && main.choices().contains(NpcTalk.Choice.LEAVE),
                                p.key() + ": the first page fits the screen and has a way out");
                            for (NpcTalk.Choice choice : main.choices()) {
                                EtoTalk.Reply reply = script.answer(c, choice);
                                check(reply != null && reply.line() != null && !FORBIDDEN.matcher(reply.line()).find(),
                                    p.key() + ": an answer to " + choice);
                            }
                            EtoTalk.Page topics = script.talk(c, "go on");
                            for (EtoTalk.Option option : topics.options()) {
                                if (option.key().startsWith("topic:")) {
                                    EtoTopics.Topic topic = script.topicOf(option.key().substring(6));
                                    check(topic != null && script.topicIndex(topic) >= 0 && script.topicIndex(topic) < 14,
                                        p.key() + ": a topic offered is found (" + option.key() + ")");
                                    EtoTalk.Page page = script.topic(c, topic);
                                    check(!page.line().isBlank() && !FORBIDDEN.matcher(page.line()).find(), p.key() + ": a topic's answer");
                                    for (int i = 0; i < topic.open(c).size(); i++) {
                                        EtoTalk.Reply follow = script.follow(c, topic, i);
                                        check(!follow.line().isBlank() && !FORBIDDEN.matcher(follow.line()).find(), p.key() + ": a reply on " + topic.id());
                                    }
                                    topicIds.add(topic.id());
                                }
                            }
                            if (stage.together()) {
                                EtoTalk.Page sweet = script.sweet(c, "mm");
                                for (EtoTalk.Option option : sweet.options()) {
                                    if (option.key().startsWith("sweet:")) {
                                        check(!script.sweetly(c, option.key().substring(6)).line().isBlank(), p.key() + ": sweet " + option.key());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            check(topicIds.size() >= 6, p.key() + ": plenty to talk about (" + topicIds + ")");
            for (com.hex.kagune.control.npc.Display shown : com.hex.kagune.control.npc.Display.values()) {
                for (EtoVoice voice : EtoVoice.values()) {
                    String line = script.saw(shown, voice, false, 3);
                    check(shown == com.hex.kagune.control.npc.Display.NONE ? line.isEmpty() : !line.isBlank(), p.key() + ": a word on seeing " + shown);
                }
            }
            for (EtoInitiative.Act act : EtoInitiative.Act.values()) {
                check(act == EtoInitiative.Act.NONE || !script.initiative(act, 5).isBlank(), p.key() + ": a word as it starts " + act);
            }
            for (EtoInitiative.Moment moment : EtoInitiative.Moment.values()) {
                check(!script.moment(moment, true, 2).isBlank() && !script.moment(moment, false, 2).isBlank(), p.key() + ": a word at " + moment);
            }
            check(words > 1000, p.key() + ": many lines checked (" + words + ")");
        }
        // A lesson asked for: taught when ready, refused with the reason when not.
        Teaching.View ready = Teaching.view("touka", true, KagunePath.UKAKU, 250, gate -> false);
        EtoTalk.Reply learn = Persona.TOUKA.script().answer(context(20, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, ready, true, 6000L),
            NpcTalk.Choice.LESSON);
        check(learn.effect() == EtoTalk.Effect.LEARN && learn.line().contains("Crystal"), "Touka teaches the crystals: " + learn.line());
        Teaching.View trial = Teaching.view("ken", true, KagunePath.RINKAKU, 800, gate -> gate == Gate.RINKAKU_FOUR);
        EtoTalk.Reply torture = Persona.KEN.script().answer(context(20, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, trial, true, 6000L),
            NpcTalk.Choice.LESSON);
        check(torture.effect() == EtoTalk.Effect.TORTURE, "Ken's centipede is lived through, not told");
        Teaching.View weak = Teaching.view("ken", true, KagunePath.RINKAKU, 300, gate -> gate == Gate.RINKAKU_FOUR);
        EtoTalk.Reply refused = Persona.KEN.script().answer(context(20, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, weak, true, 6000L),
            NpcTalk.Choice.LESSON);
        check(refused.effect() == EtoTalk.Effect.NONE && refused.line().contains("750"), "too weak: told the mastery it takes: " + refused.line());
        check(!Persona.KEN.script().trialWords(Teaching.byId("ken_centipede"),
            context(20, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, trial, true, 6000L)).isBlank(), "Ken's words as the trial ends");
        // Ken only for women: a man at his most regard is never offered the errands or a date.
        EtoTalk.Context man = context(NpcBonds.MOST, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, false, 6000L);
        EtoTalk.Context woman = context(NpcBonds.MOST, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, true, 6000L);
        check(!Persona.KEN.script().main(man, "").choices().contains(NpcTalk.Choice.FAVOUR)
            && Persona.KEN.script().main(woman, "").choices().contains(NpcTalk.Choice.FAVOUR)
            && Persona.KEN.script().answer(man, NpcTalk.Choice.ASK_OUT).effect() != EtoTalk.Effect.DATE,
            "Ken: a friend to a man, a date for a woman");
        check(Persona.TOUKA.script().main(man, "").choices().contains(NpcTalk.Choice.FAVOUR), "Touka would date anyone who earns it");
        // A baby: only a woman and a man, married, not already on the way.
        EtoTalk.Context wed = context(NpcBonds.MOST, Romance.Stage.MARRIED, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, true, 6000L);
        EtoTalk.Context same = context(NpcBonds.MOST, Romance.Stage.MARRIED, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, false, 6000L);
        check(wed.babyOpen() && !same.babyOpen(), "a baby can be asked for by a married woman and man, not two women or two men");
        check(Persona.ETO.script().greeting(wed).equals(EtoTalk.greeting(wed)) && Persona.ETO.script().kissed(3, true).equals(com.hex.kagune.control.npc.EtoLines.kissed(3, true)), "Eto's script is Eto's own words, unchanged");
    }

    static void poses() {
        for (boolean leads : new boolean[] {false, true}) {
            for (EtoGesture gesture : EtoGesture.values()) {
                int length = gesture.held() ? 200 : gesture.ticks;
                for (int t = 0; t <= length; t += 3) {
                    RigPose p = EtoPose.gesture(gesture, t, leads);
                    check(gesture == EtoGesture.NONE ? p == null : p != null && Float.isFinite(p.rightArmX) && Float.isFinite(p.lean) && Math.abs(p.flip) < 0.6F
                        && Math.abs(p.drop) < 4.0F && Math.abs(p.lean) < 0.7F, gesture + (leads ? " (led)" : "") + " at " + t + " is a sane pose");
                }
            }
        }
        RigPose kiss = EtoPose.gesture(EtoGesture.KISS, 30, true);
        check(kiss.rightArmX < -1.4F && kiss.leftArmX < -0.5F && kiss.lean > 0.1F, "Ken's kiss: a hand to their cheek, one at their waist, leaning down");
        RigPose her = EtoPose.gesture(EtoGesture.KISS, 30, false);
        check(her.rightArmX > 0.3F, "hers: hands behind her back, as always");
        RigPose peck = EtoPose.gesture(EtoGesture.PECK, EtoGesture.PECK.ticks / 2.0, true);
        check(peck.headX > 0.3F && peck.lean > 0.2F, "Ken's quick kiss: down to their forehead");
        RigPose hand = EtoPose.gesture(EtoGesture.HAND, 40, true);
        check(hand.leftArmHold >= 0.999F, "holding hands is the same for everyone (no led pose of its own)");
    }

    /** 1.3.26: the owner: Touka "attacks properly but she's not manifesting at all": her wings drew nothing (both passed as thrown). */
    static void toukaWingsShow() {
        double[] eye = {0.0, -1.5, -4.0};
        for (float deploy : new float[] {0.3F, 1.0F}) {
            for (float crystal : new float[] {0.0F, 1.0F}) {
                int quads = com.hex.kagune.control.ukaku.WingRig.build(com.hex.kagune.control.npc.WingRules.companion(deploy, 5.0, crystal, false, 0.0F,
                    com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F), eye).size();
                check(quads > 50, "Touka's wings are drawn as they come out (deploy " + deploy + ", crystal " + crystal + "): " + quads + " quads");
            }
        }
        check(com.hex.kagune.control.ukaku.WingRig.build(com.hex.kagune.control.npc.WingRules.companion(0.0F, 5.0, 0.0F, true, 0.0F,
            com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F), eye).isEmpty(), "put away, none are drawn");
        // 1.3.29: the owner: "only one wing, not two, and it should be smaller".
        com.hex.kagune.control.ukaku.WingRig.Input one = com.hex.kagune.control.npc.WingRules.companion(1.0F, 5.0, 1.0F, false, 0.0F,
            com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F);
        com.hex.kagune.control.ukaku.WingRig.Input two = new com.hex.kagune.control.ukaku.WingRig.Input(one.form(), one.mastery(), one.deploy(), one.time(),
            one.crystalRight(), one.crystalLeft(), one.motion(), one.progress(), one.impact(), one.volley(), one.recoil(), one.crouching(), one.detail(),
            one.alpha(), one.dissolving(), one.aimRight(), one.aimLeft(), one.aim(), one.kickRight(), one.kickLeft(), 0.0F, 0.0F, 1.0F, 1.0F);
        int alone = com.hex.kagune.control.ukaku.WingRig.build(one, eye).size();
        int pair = com.hex.kagune.control.ukaku.WingRig.build(two, eye).size();
        check(alone > 50 && alone < pair * 0.65, "Touka's kagune: one wing, not two (" + alone + " quads against a pair's " + pair + ")");
        check(one.growRight() + one.growLeft() == 1.0F && com.hex.kagune.control.npc.WingRules.SIDE != 0, "one shoulder blade, every spike from it");
        check(com.hex.kagune.control.ukaku.UkakuRules.wingLength(com.hex.kagune.control.npc.WingRules.MASTERY, com.hex.kagune.control.ukaku.WingForm.TWIN)
            < com.hex.kagune.control.ukaku.UkakuRules.wingLength(380, com.hex.kagune.control.ukaku.WingForm.TWIN), "and smaller than it was");
    }
}
