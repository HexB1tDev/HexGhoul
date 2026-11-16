package com.hex.kagune.control;

import com.hex.kagune.control.family.FamilyView;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.EtoTopics;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.Script;
import com.hex.kagune.control.npc.Spoken;
import com.hex.kagune.control.npc.Teaching;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 4.2.4 (the owner, after 4.2.3, quoted exactly): "Currently, you can just spam one option and it'll give you relationship. It's a bug like that.
 * That shouldn't be able to pass. Plus, I don't know why you can ask twice the same person who they are. Like, I mean, if you're married to
 * someone, you can still ask them who the fuck they are. Like, how does that make sense? There should be such a thing as one-time options, like
 * one-time dialogue options." The talk's rules: who they are asked once (and never of one they are with); a choice's warmth once a day,
 * whatever its words say it is worth; one-time topics and replies; and the bookkeeping that holds them ({@link Spoken}).
 */
public final class TalkRulesTests {
    private static int checks;
    private static int failures;

    private TalkRulesTests() {
    }

    private static void check(boolean good, String message) {
        checks++;
        if (!good) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }

    public static void main(String[] args) throws Exception {
        spoken();
        who();
        once();
        gifts();
        server();
        topics();
        // 4.2.5 (the owner: "add as much text as you can to every single NPC, just some that can increase relationship, um, some that reduce it"):
        developed(Persona.BANJOU, 22, 13, 8);
        developed(Persona.ROMA, 20, 13, 9);
        // 4.2.6: the General, Ayato, Noro (Noro still terse: few words, small warmth).
        developed(Persona.GENERAL, 21, 12, 9);
        developed(Persona.AYATO, 21, 15, 9);
        developed(Persona.NORO, 11, 7, 3);
        // 4.2.7: Eto, Touka, Ken.
        developed(Persona.ETO, 25, 12, 7);
        developed(Persona.TOUKA, 21, 12, 6);
        developed(Persona.KEN, 19, 10, 6);
        generic(); // 4.2.8: the wild ghouls, gang members and village humans
        System.out.println("Talk rules tests: " + checks + " checks " + (failures == 0 ? "passed" : "run, " + failures + " FAILED"));
        if (failures > 0) {
            throw new AssertionError(failures + " talk rules checks failed");
        }
    }

    static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    /** A talk with one of them: {@code regard}, the bond's {@code flags}, how far along, meat in hand, a gift given today, what was said once. */
    static EtoTalk.Context context(int regard, int flags, Romance.Stage stage, boolean meat, boolean gifted, Spoken spoken) {
        return context(regard, flags, stage, meat, gifted, spoken, 7);
    }

    /** As above, with its {@code seed} (which of a line's variants is said). */
    static EtoTalk.Context context(int regard, int flags, Romance.Stage stage, boolean meat, boolean gifted, Spoken spoken, int seed) {
        return new EtoTalk.Context(regard, flags | (stage.together() ? EtoRules.DATING : 0), 0, true, false, gifted, false, false, null, true, false, seed,
            stage, null, false, 6000L, EtoMood.HAPPY, 3L, 0L, null, false, meat, false, EtoRules.Pact.NONE, 0, false, false, "Sam", false,
            FamilyView.NONE, Teaching.View.NONE, true, 0, spoken);
    }

    private static boolean offers(EtoTalk.Page page, NpcTalk.Choice choice) {
        return page.options().stream().anyMatch(option -> option.choice() == choice);
    }

    /** The bookkeeping: a reply paid once a day, a one-time mark kept, the names it keeps them under fit the memory's. */
    static void spoken() {
        long day = 120L;
        long stored = Spoken.pay(0L, day, 3);
        check(!Spoken.pays(stored, day, 3) && Spoken.pays(stored, day, 2) && Spoken.pays(stored, day + 1, 3), "a reply paid once a day, each its own");
        long both = Spoken.pay(stored, day, 5);
        check(!Spoken.pays(both, day, 3) && !Spoken.pays(both, day, 5), "two replies of one topic paid the same day, each kept");
        check(Spoken.paid(Spoken.pay(both, day + 1, 0), day + 1) == 1L, "a new day starts afresh");
        check(!Spoken.pays(0L, day, -1) && !Spoken.pays(0L, day, Spoken.MOST_FOLLOWS), "nothing out of its range pays");
        Spoken said = new Spoken(Map.of(Spoken.onceKey("scar"), Spoken.mark(Spoken.mark(0L, 2), Spoken.TOPIC_BIT)));
        check(said.said("scar", 2) && !said.said("scar", 1) && said.opened("scar") && !said.opened("other") && !Spoken.NONE.said("scar", 2),
            "a one-time reply and a one-time topic kept as said");
        check(Spoken.warmth(3, true) == 3 && Spoken.warmth(3, false) == 0 && Spoken.warmth(-4, false) == -4, "warmth paid once; what hurts always hurts");
        check(Spoken.choicePays(-1L, day) && !Spoken.choicePays(day, day) && Spoken.choicePays(day - 1, day), "a choice pays once a day");
        for (NpcTalk.Choice choice : NpcTalk.Choice.values()) {
            check(clean(Spoken.choiceKey(choice.name())), "a choice's memory name is a clean word: " + Spoken.choiceKey(choice.name()));
        }
        for (String act : new String[] {"sleep_kiss", "cookbook", "reassure", "sorry"}) {
            check(clean(Spoken.choiceKey(act)), "a keyed act's memory name is a clean word: " + act);
        }
    }

    /** As the memory takes them: a lower-case word of letters, digits and _, 24 at most. */
    static boolean clean(String word) {
        return word.length() <= 24 && word.matches("[a-z0-9_]+");
    }

    /** "Who are you?": asked once, never of one they are with (dating or married), for every one of them; a wild ghoul's once too. */
    static void who() {
        for (Persona p : Persona.ALL) {
            Script script = p.script();
            check(offers(script.main(context(0, 0, Romance.Stage.NONE, false, false, Spoken.NONE), ""), NpcTalk.Choice.WHO),
                p.key() + ": a stranger may ask who they are");
            check(!offers(script.main(context(0, NpcBonds.ASKED, Romance.Stage.NONE, false, false, Spoken.NONE), ""), NpcTalk.Choice.WHO),
                p.key() + ": asked once, never again");
            for (Romance.Stage stage : new Romance.Stage[] {Romance.Stage.DATING, Romance.Stage.MARRIED}) {
                check(!offers(script.main(context(NpcBonds.MOST, 0, stage, false, false, Spoken.NONE), ""), NpcTalk.Choice.WHO),
                    p.key() + ": never asked of one they are " + (stage == Romance.Stage.MARRIED ? "married to" : "dating") + " (never asked before)");
            }
        }
        com.hex.kagune.control.npc.NpcIdentity ghoul = new com.hex.kagune.control.npc.NpcIdentity(com.hex.kagune.control.npc.NpcIdentity.Kind.UKAKU,
            com.hex.kagune.control.npc.NpcTemper.SCAVENGER, 2, 0, 3, 1234);
        for (boolean asked : new boolean[] {false, true}) {
            NpcTalk.Context c = new NpcTalk.Context(ghoul, 0, true, null, false, false, false, asked, false, false, true, null, true);
            check(NpcTalk.choices(c).contains(NpcTalk.Choice.WHO) != asked, "a wild ghoul: who it is asked " + (asked ? "once, never again" : "the first time"));
        }
    }

    /** One-time replies and topics: offered until said, then never; the others as before. */
    static void once() {
        EtoTopics.Follow always = new EtoTopics.Follow("Again?", c -> true, c -> new EtoTalk.Reply("Again.", 1, EtoTalk.Effect.NONE));
        EtoTopics.Follow once = EtoTopics.Follow.once("Where's it from?", c -> new EtoTalk.Reply("A long story.", 2, EtoTalk.Effect.NONE));
        EtoTopics.Topic scar = new EtoTopics.Topic("scar", c -> true, c -> "That scar?", c -> "This?", List.of(always, once), true);
        EtoTalk.Context fresh = context(0, 0, Romance.Stage.NONE, false, false, Spoken.NONE);
        check(scar.offered(fresh) && scar.open(fresh).size() == 2, "a one-time topic and reply offered until said");
        Spoken said = new Spoken(Map.of(Spoken.onceKey("scar"), Spoken.mark(0L, scar.place(once))));
        EtoTalk.Context after = context(0, 0, Romance.Stage.NONE, false, false, said);
        check(scar.open(after).size() == 1 && scar.open(after).get(0) == always, "a one-time reply said: never offered again; the others still are");
        check(scar.place(once) == 1 && scar.place(always) == 0, "a reply's place among all of them (its mark)");
        Spoken opened = new Spoken(Map.of(Spoken.onceKey("scar"), Spoken.mark(0L, Spoken.TOPIC_BIT)));
        check(!scar.offered(context(0, 0, Romance.Stage.NONE, false, false, opened)), "a one-time topic opened: gone from the list");
        EtoTopics.Topic plain = new EtoTopics.Topic("weather", c -> true, c -> "Weather?", c -> "Rain.", List.of(always));
        check(!plain.once() && plain.offered(context(0, 0, Romance.Stage.NONE, false, false, opened)), "a topic not marked once stays");
        // 4.2.5: a topic of one-time replies only, each said: nothing left to bring it up for.
        EtoTopics.Follow vow = EtoTopics.Follow.once("Promise?", c -> new EtoTalk.Reply("Promise.", 4, EtoTalk.Effect.NONE));
        EtoTopics.Topic promise = new EtoTopics.Topic("promise", c -> true, c -> "Promise me.", c -> "Anything.", List.of(vow));
        check(promise.offered(fresh), "a topic of promises offered while one is left");
        check(!promise.offered(context(0, 0, Romance.Stage.NONE, false, false, new Spoken(Map.of(Spoken.onceKey("promise"), Spoken.mark(0L, 0))))),
            "every promise made: the topic is gone");
        check(scar.offered(after), "a topic with a reply that can be said again stays");
    }

    /** Dinner (Ghoul Meat) given: taken, and warm once a day, for every one of them who takes it (before 4.2.4 Roma's and Banjou's: +3 a click, kept). */
    static void gifts() {
        for (Persona p : Persona.ALL) {
            EtoTalk.Reply first = p.script().answer(context(NpcBonds.FRIEND, NpcBonds.ASKED, Romance.Stage.NONE, true, false, Spoken.NONE), NpcTalk.Choice.GIFT);
            EtoTalk.Reply again = p.script().answer(context(NpcBonds.FRIEND, NpcBonds.ASKED, Romance.Stage.NONE, true, true, Spoken.NONE), NpcTalk.Choice.GIFT);
            if (first.regard() > 0) {
                check(first.effect() == EtoTalk.Effect.MEAT, p.key() + ": dinner warms them only as it is taken from your hand");
                check(again.regard() <= 0, p.key() + ": a second dinner the same day buys nothing");
            }
        }
    }

    /** What the server does with them: every first page's choice and keyed act paid once a day, replies by their topic, one-time marks kept. */
    static void server() throws Exception {
        String eto = read("src/main/java/com/hex/kagune/control/server/EtoServer.java");
        check(eto.contains("carry(player, npc, c, choice, once(player, npc, level, choice.name(), script.answer(c, choice)), after);"),
            "every answer of the first page paid once a day (a tease, a dinner, a home...)");
        check(eto.contains("if (reply.regard() <= 0) {\n            return reply;\n        }\n        return choiceFirst(player, npc, level, what) ? reply : unpaid(reply);"),
            "warmth once a day; what hurts always hurts");
        check(eto.contains("boolean first = com.hex.kagune.control.npc.Spoken.choicePays(memory.get(npc.persona().key(), name, -1L), today);")
            && eto.contains("memory.set(npc.persona().key(), name, today);"), "a choice's day kept in their memory");
        check(eto.contains("once(player, npc, level, \"sleep_kiss\",") && eto.contains("once(player, npc, level, \"cookbook\",")
            && eto.contains("choiceFirst(player, npc, level, \"reassure\")") && eto.contains("choiceFirst(player, npc, level, \"sorry\")"),
            "the keyed acts too: a kiss half asleep, the cookbook, a reassurance, an apology");
        check(eto.contains("once(player, npc, level, choice.name(), new EtoTalk.Reply(known + \" \" + reply.line(), reply.regard(),"),
            "a lesson already known too");
        check(eto.contains("boolean first = paysToday(player, npc, level, topic.id(), place);")
            && eto.contains("carry(player, npc, c, choice, first ? reply : unpaid(reply), Kind.TALK);")
            && !eto.contains("Math.min(13, script.topicIndex(topic))"), "a topic's replies paid once a day each, kept by its id (no 14-topic limit)");
        check(eto.contains("if (said != null && said.once()) {\n                spoken(player, npc, topic.id(), place);"), "a one-time reply marked said");
        check(eto.contains("if (topic.once()) {\n                    spoken(player, npc, topic.id(), com.hex.kagune.control.npc.Spoken.TOPIC_BIT);"),
            "a one-time topic marked opened");
        check(eto.contains("new com.hex.kagune.control.npc.Spoken(memory.starting(persona.key(), \"o_\"))"), "the talk knows what was said once");
        String ghouls = read("src/main/java/com/hex/kagune/control/server/GhoulNpcTalk.java");
        check(ghouls.contains("if (Long.valueOf(today).equals(GIFTED.get(both))) {\n                reply = new NpcTalk.Reply(reply.line(), 0, reply.effect());")
            && ghouls.contains("GIFTED.put(both, today);") && ghouls.contains("private static final Map<String, Long> GIFTED = WorldTimes.map();"),
            "a wild ghoul warmed by meat once a day");
    }

    /**
     * 4.2.5 on: {@code p} developed: at least {@code topics} topics, {@code lowers} replies that cost them regard, {@code onces} one-time replies;
     * every reply's worth between -8 and +5 (+5 the most any reply had before: "I love you."; a gift or a lesson is worth more, never a word), and every line said.
     */
    static void developed(Persona p, int topics, int lowers, int onces) {
        List<EtoTopics.Topic> all = p.script().allTopics();
        // The house rule on names in visible text (3.3.5 on): never Takatsuki or Yoshimura; Aogiri only from its own (Ayato, Noro).
        java.util.regex.Pattern names = p == Persona.AYATO || p == Persona.NORO ? java.util.regex.Pattern.compile("Takatsuki|Yoshimura")
            : java.util.regex.Pattern.compile("Takatsuki|Yoshimura|Aogiri");
        check(all.size() >= topics, p.key() + ": " + all.size() + " topics (at least " + topics + ")");
        int lower = 0;
        int raise = 0;
        int once = 0;
        // Every variant of every line (a line picked by the seed has up to six): before 4.2.7's amend one seed was looked at, and a name in a
        // line's other variant went unseen (the sabotage list caught it).
        List<EtoTalk.Context> seeded = new java.util.ArrayList<>();
        for (int seed = 0; seed < 12; seed++) {
            seeded.add(context(NpcBonds.MOST, NpcBonds.ASKED, Romance.Stage.MARRIED, false, false, Spoken.NONE, seed));
            seeded.add(context(NpcBonds.FRIEND, NpcBonds.ASKED, Romance.Stage.NONE, false, false, Spoken.NONE, seed));
            seeded.add(context(NpcBonds.FRIENDLY, 0, Romance.Stage.NONE, false, false, Spoken.NONE, seed));
        }
        EtoTalk.Context[] cs = seeded.toArray(new EtoTalk.Context[0]);
        for (EtoTopics.Topic topic : all) {
            if (topic.id().equals("baby") || topic.id().equals("family")) {
                continue; // a family's own (a baby carried, a child wanted), looked at by FamilyTests
            }
            for (EtoTalk.Context c : cs) {
                if (topic.when().test(c)) {
                    String answer = topic.answer().apply(c);
                    check(answer != null && !answer.isBlank() && !CompanionTests.FORBIDDEN.matcher(answer + topic.prompt().apply(c)).find()
                        && !names.matcher(answer + topic.prompt().apply(c)).find(),
                        p.key() + ": " + topic.id() + " is answered");
                }
            }
            for (EtoTopics.Follow follow : topic.follows()) {
                once += follow.once() ? 1 : 0;
                check(!CompanionTests.FORBIDDEN.matcher(follow.prompt()).find() && !names.matcher(follow.prompt()).find(),
                    p.key() + ": " + topic.id() + ": a reply's words");
                int least = Integer.MAX_VALUE;
                int most = Integer.MIN_VALUE;
                for (EtoTalk.Context c : cs) {
                    if (!follow.when().test(c)) {
                        continue;
                    }
                    EtoTalk.Reply reply = follow.reply().apply(c);
                    check(reply.line() != null && !reply.line().isBlank() && !CompanionTests.FORBIDDEN.matcher(reply.line()).find()
                        && !names.matcher(reply.line()).find(),
                        p.key() + ": " + topic.id() + ": \"" + follow.prompt() + "\" is answered");
                    check(reply.regard() >= -8 && reply.regard() <= 5, p.key() + ": " + topic.id() + ": \"" + follow.prompt() + "\" is worth " + reply.regard());
                    least = Math.min(least, reply.regard());
                    most = Math.max(most, reply.regard());
                }
                lower += least < 0 && least != Integer.MAX_VALUE ? 1 : 0;
                raise += most > 0 && most != Integer.MIN_VALUE ? 1 : 0;
            }
        }
        check(lower >= lowers, p.key() + ": " + lower + " replies that cost regard (at least " + lowers + ")");
        check(raise > lower, p.key() + ": " + raise + " replies that win it, more than the " + lower + " that cost it");
        check(once >= onces, p.key() + ": " + once + " one-time replies (at least " + onces + ")");
    }

    /**
     * 4.2.8: the others developed: a wild ghoul (each temper that talks) has nine things to say (a Gourmet seven); a gang member at least five for
     * every standing; the ghouls who walk with you twenty topics, twelve answers that cost; and a village human two more topics, answered in
     * every temper, their flags clear of every other flag a human's bond keeps.
     */
    static void generic() throws Exception {
        com.hex.kagune.control.npc.NpcTemper[] talkers = {com.hex.kagune.control.npc.NpcTemper.ANTEIKU, com.hex.kagune.control.npc.NpcTemper.PACK,
            com.hex.kagune.control.npc.NpcTemper.SCAVENGER, com.hex.kagune.control.npc.NpcTemper.GOURMET, com.hex.kagune.control.npc.NpcTemper.DRIFTER};
        for (com.hex.kagune.control.npc.NpcTemper temper : talkers) {
            Set<String> said = new HashSet<>();
            for (int regard = 0; regard < 40; regard++) {
                com.hex.kagune.control.npc.NpcIdentity who = new com.hex.kagune.control.npc.NpcIdentity(com.hex.kagune.control.npc.NpcIdentity.Kind.UKAKU,
                    temper, 2, 0, 3, 1000);
                NpcTalk.Reply reply = NpcTalk.answer(new NpcTalk.Context(who, regard, true, null, false, false, false, true, false, false, true, null, true),
                    NpcTalk.Choice.CHAT);
                said.add(reply.line());
            }
            int least = temper == com.hex.kagune.control.npc.NpcTemper.GOURMET ? 7 : 9;
            check(said.size() >= least, temper + ": " + said.size() + " things to say to a stranger (at least " + least + ")");
        }
        for (int reputation = -100; reputation <= 100; reputation += 5) {
            check(com.hex.kagune.control.npc.GangTalk.lines(reputation).length >= 5, "a gang member at standing " + reputation + " has five things to say");
        }
        String ally = read("src/main/resources/data/hexkagune/talk/ally.txt");
        com.hex.kagune.control.npc.scene.SceneBook.Parsed parsed = com.hex.kagune.control.npc.scene.SceneBook.parse("ally", ally);
        check(parsed.errors().isEmpty(), "the walking ghouls' script reads cleanly: " + parsed.errors());
        int topics = 0;
        int costly = 0;
        for (com.hex.kagune.control.npc.scene.Scene scene : parsed.scenes()) {
            if (scene.kind() != com.hex.kagune.control.npc.scene.Scene.Kind.TOPIC) {
                continue;
            }
            topics++;
            Set<com.hex.kagune.control.npc.scene.Scene.Page> pages = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            pages.addAll(scene.pages().values());
            pages.add(scene.first());
            for (com.hex.kagune.control.npc.scene.Scene.Page page : pages) {
                for (com.hex.kagune.control.npc.scene.Scene.Option option : page.options()) {
                    costly += option.regard() < 0 ? 1 : 0;
                }
            }
        }
        check(topics >= 20, "a ghoul who walks with you: " + topics + " topics (at least 20)");
        check(costly >= 12, "a ghoul who walks with you: " + costly + " answers that cost regard (at least 12)");
        com.hex.kagune.control.village.HumanVoice.Topic strange = com.hex.kagune.control.village.HumanVoice.Topic.STRANGE;
        com.hex.kagune.control.village.HumanVoice.Topic me = com.hex.kagune.control.village.HumanVoice.Topic.ME;
        check(!strange.personal() && !me.personal() && me.warmth == com.hex.kagune.control.village.HumanPersona.WARM && strange.warmth == 0,
            "a human's two new topics are talk (never paid), what they think of you asked once they're warm");
        int others = com.hex.kagune.control.village.HumanPersona.CONFESSED | com.hex.kagune.control.village.HumanPersona.PARTNER
            | com.hex.kagune.control.village.HumanPersona.NOT_TYPE | com.hex.kagune.control.village.HumanPersona.MARRIED;
        int seen = 0;
        for (com.hex.kagune.control.village.HumanVoice.Topic topic : com.hex.kagune.control.village.HumanVoice.Topic.values()) {
            check(topic.flag() > 0 && (topic.flag() & others) == 0 && (topic.flag() & seen) == 0, "a human topic's flag is its own: " + topic);
            seen |= topic.flag();
        }
        com.hex.kagune.control.village.HumanLife.Life life = com.hex.kagune.control.village.HumanLife.of(12345L,
            com.hex.kagune.control.village.HumanLife.Job.values()[0]);
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 6; seed++) {
                for (com.hex.kagune.control.village.HumanVoice.Topic topic : new com.hex.kagune.control.village.HumanVoice.Topic[] {strange, me}) {
                    String said = com.hex.kagune.control.village.HumanVoice.answer(temper, life, topic, seed, null);
                    check(said != null && !said.isBlank() && !said.contains("null") && !CompanionTests.FORBIDDEN.matcher(said).find(),
                        temper + " answers " + topic);
                }
            }
        }
    }

    /** Every one's topics: ids that fit the memory, none twice, at most a page of replies each. */
    static void topics() {
        int all = 0;
        for (Persona p : Persona.ALL) {
            Set<String> ids = new HashSet<>();
            for (EtoTopics.Topic topic : p.script().allTopics()) {
                all++;
                check(ids.add(topic.id()), p.key() + ": topic " + topic.id() + " once");
                check(clean(Spoken.onceKey(topic.id())) && clean(Spoken.paidKey(topic.id())), p.key() + ": topic " + topic.id() + "'s marks fit the memory");
                check(topic.follows().size() <= Spoken.MOST_FOLLOWS, p.key() + ": topic " + topic.id() + " has a page of replies at most");
            }
        }
        check(all >= 60, "every one's topics looked at (" + all + ")");
    }
}
