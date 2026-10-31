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
        poses();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Companion tests: " + passed + " checks passed");
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
        check(EtoGesture.CROSS.ordinal() == EtoGesture.HAND.ordinal() + 1 && EtoGesture.HAND.ordinal() > EtoGesture.LAUGH.ordinal() && EtoGesture.PECK.ordinal() == EtoGesture.values().length - 1,
            "new gestures only ever at the end (sent by ordinal)");
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
}
