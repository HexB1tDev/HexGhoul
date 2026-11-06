package com.hex.kagune.control;

import com.hex.kagune.control.family.FamilyView;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.Teaching;
import com.hex.kagune.control.npc.scene.Scene;
import com.hex.kagune.control.npc.scene.SceneBook;
import com.hex.kagune.control.npc.scene.SceneContext;
import com.hex.kagune.control.village.HumanPartnerTalk;
import com.hex.kagune.control.village.HumanPersona;
import com.hex.kagune.control.village.HumanRomance;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 3.1.4: dialogue must respect current relationship, mood, time and actual family, including when a menu becomes stale. */
public final class DialogueTests {
    private static int passed;

    private static void check(boolean ok, String why) {
        if (!ok) {
            throw new AssertionError(why);
        }
        passed++;
    }

    public static void main(String[] args) throws Exception {
        sceneConditions();
        humanConditions();
        babyRequests();
        System.out.println("Dialogue tests: " + passed + " checks passed");
    }

    private static SceneContext scene(String tier, String mood, Set<String> flags) {
        return new SceneContext(tier, mood, "", flags, Map.of(), 0);
    }

    private static void sceneConditions() throws Exception {
        for (String speaker : List.of("eto", "touka", "ken")) {
            SceneBook.Parsed book = SceneBook.parse(speaker,
                Files.readString(Path.of("src/main/resources/data/hexkagune/talk", speaker + ".txt")));
            check(book.errors().isEmpty(), speaker + ": additions parse: " + book.errors());
            List<Scene> added = book.scenes().stream().filter(s -> s.id().startsWith("more_")).toList();
            for (String id : List.of("more_bed_invitation", "more_bed_teasing", "more_private_night")) {
                Scene invitation = added.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
                for (String tier : List.of("wary", "friendly", "close", "partner", "wife", "child", "teen")) {
                    for (String mood : List.of("happy", "giddy", "affectionate", "sulky", "jealous", "sleepy", "wary")) {
                        for (Set<String> flags : List.of(Set.of("night", "home"), Set.of("day", "home"), Set.of("night"))) {
                            boolean appropriate = tier.equals("wife") && Set.of("happy", "giddy", "affectionate").contains(mood)
                                && flags.contains("night") && flags.contains("home");
                            check(invitation.fits(scene(tier, mood, flags)) == appropriate, speaker + "." + id + ": private invitation conditions");
                        }
                    }
                }
                check(invitation.first().options().get(0).does("night"), speaker + "." + id + ": uses the existing night action");
                Scene.Option decline = invitation.first().options().get(1);
                check(!decline.does("night") && decline.regard() >= 0, speaker + "." + id + ": gentle alternative, no intimacy or penalty");
            }
            for (String id : List.of("more_kids_home", "more_kids_bedtime", "more_kids_proud", "more_parent_team")) {
                Scene parent = added.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
                check(!parent.fits(scene("wife", "happy", Set.of("home", "night"))), speaker + "." + id + ": no fictional children");
                check(parent.when().contains("kids"), speaker + "." + id + ": requires real children");
            }
            for (String id : List.of("more_family_future", "more_baby_talk")) {
                Scene planning = added.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
                check(planning.fits(scene("wife", "happy", Set.of("home", "mixed"))), speaker + "." + id + ": eligible family planning");
                check(!planning.fits(scene("wife", "happy", Set.of("home", "mixed", "pregnant"))), speaker + "." + id + ": not while pregnant");
                check(planning.first().options().get(0).does("sweet"), speaker + "." + id + ": actual baby ask remains on the sweet page");
            }
            for (Scene addedScene : added) {
                check(addedScene.first().options().stream().allMatch(o -> o.regard() >= 0),
                    speaker + "." + addedScene.id() + ": the new replies do not drain affection");
            }
        }
    }

    private static void humanConditions() {
        HumanPartnerTalk.Context dating = new HumanPartnerTalk.Context(false, true, false, false);
        HumanPartnerTalk.Context night = new HumanPartnerTalk.Context(true, true, false, false);
        HumanPartnerTalk.Context day = new HumanPartnerTalk.Context(true, false, false, false);
        HumanPartnerTalk.Context hurt = new HumanPartnerTalk.Context(true, true, true, false);
        HumanPartnerTalk.Context angry = new HumanPartnerTalk.Context(true, true, false, true);
        for (HumanRomance.Partner topic : HumanRomance.Partner.values()) {
            check(HumanPartnerTalk.available(topic, dating) == (topic != HumanRomance.Partner.BABY && topic != HumanRomance.Partner.BED),
                topic + ": private/family topics excluded before marriage");
            check(HumanPartnerTalk.available(topic, night), topic + ": spouse topic available");
        }
        for (HumanPersona.Temper temper : HumanPersona.Temper.values()) {
            Set<String> bedtime = new HashSet<>();
            Set<String> babies = new HashSet<>();
            for (int seed : new int[] {Integer.MIN_VALUE, -3, -2, -1, 0, 1, 2, 3, Integer.MAX_VALUE}) {
                String invite = HumanRomance.partner(temper, HumanRomance.Partner.BED, HumanPersona.Taste.LOYAL, "a quiet home", night, seed);
                bedtime.add(invite);
                babies.add(HumanRomance.partner(temper, HumanRomance.Partner.BABY, HumanPersona.Taste.LOYAL, "a quiet home", night, seed));
                check(!invite.equals(HumanPartnerTalk.reply(temper, HumanRomance.Partner.BED, day, seed)), temper + ": private night differs from daylight");
                check(!invite.equals(HumanPartnerTalk.reply(temper, HumanRomance.Partner.BED, dating, seed)), temper + ": stale marriage status rechecked");
                check(HumanPartnerTalk.reply(temper, HumanRomance.Partner.BED, angry, seed)
                    .equals(HumanPartnerTalk.reply(temper, HumanRomance.Partner.FEELINGS, angry, seed)), temper + ": listens to upset partner before intimacy");
                check(HumanPartnerTalk.reply(temper, HumanRomance.Partner.BABY, hurt, seed)
                    .equals(HumanPartnerTalk.reply(temper, HumanRomance.Partner.FEELINGS, hurt, seed)), temper + ": recovery before family planning");
                for (HumanPartnerTalk.Context context : List.of(night, day, hurt, angry, dating)) {
                    for (HumanRomance.Partner topic : List.of(HumanRomance.Partner.FEELINGS, HumanRomance.Partner.BABY, HumanRomance.Partner.BED)) {
                        String line = HumanPartnerTalk.reply(temper, topic, context, seed);
                        check(!line.isBlank() && line.length() <= SceneBook.LINE_MOST && !SceneTests.FORBIDDEN.matcher(line).find(),
                            temper + "/" + topic + ": renderable, named correctly");
                    }
                }
            }
            check(bedtime.size() >= 2 && babies.size() >= 2, temper + ": varied bedtime/family replies");
        }
    }

    private static EtoTalk.Context context(Romance.Stage stage, FamilyView family, boolean mixed, int seed) {
        return new EtoTalk.Context(100, stage.together() ? EtoRules.DATING : 0, 0, true, false, false, false, false, null, true, false, seed,
            stage, null, false, 18000L, EtoMood.AFFECTIONATE, 14L, 0L, null, false, false, false, EtoRules.Pact.NONE, 0, false, false,
            "Sam", false, family, Teaching.View.NONE, mixed);
    }

    private static void babyRequests() {
        for (Persona persona : List.of(Persona.ETO, Persona.TOUKA, Persona.KEN)) {
            Set<String> first = new HashSet<>();
            Set<String> another = new HashSet<>();
            Set<String> tonight = new HashSet<>();
            for (int seed = -12; seed < 24; seed++) {
                EtoTalk.Context fresh = context(Romance.Stage.MARRIED, FamilyView.NONE, true, seed);
                EtoTalk.Context parent = context(Romance.Stage.MARRIED, new FamilyView(-1, false, 1, FamilyView.BABY_WAIT), true, seed);
                EtoTalk.Reply answer = persona.script().sweetly(fresh, "baby");
                EtoTalk.Reply again = persona.script().sweetly(parent, "baby");
                check(answer.effect() == EtoTalk.Effect.BABY && again.effect() == EtoTalk.Effect.BABY, persona + ": real baby action retained");
                first.add(answer.line());
                another.add(again.line());
                for (FamilyView unavailable : List.of(new FamilyView(0.2, false, 1), new FamilyView(-1, true, 1),
                    new FamilyView(-1, false, 1, FamilyView.BABY_WAIT - 1))) {
                    EtoTalk.Reply blocked = persona.script().sweetly(context(Romance.Stage.MARRIED, unavailable, true, seed), "baby");
                    check(blocked.effect() != EtoTalk.Effect.BABY, persona + ": pregnant, already asked or recovering never creates another request");
                    if (unavailable.wantsBaby()) {
                        tonight.add(blocked.line());
                    }
                }
                check(EtoTalk.babyOption(fresh) != null && EtoTalk.babyOption(parent).label().contains("another"),
                    persona + ": first/another baby option retained");
                check(EtoTalk.babyOption(context(Romance.Stage.DATING, FamilyView.NONE, true, seed)) == null
                    && EtoTalk.babyOption(context(Romance.Stage.MARRIED, FamilyView.NONE, false, seed)) == null,
                    persona + ": actual request eligibility retained");
            }
            check(first.size() >= 6 && another.size() >= 7 && tonight.size() >= 6, persona + ": expanded actual baby response pools");
        }
    }
}
