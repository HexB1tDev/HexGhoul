package com.hex.kagune.control;

import com.hex.kagune.control.family.ChildMoment;
import com.hex.kagune.control.family.ChildTemper;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.scene.Scene;
import com.hex.kagune.control.npc.scene.SceneBook;
import com.hex.kagune.control.npc.scene.SceneContext;
import com.hex.kagune.control.npc.scene.SceneWords;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 1.3.29: the scenes (SceneBook, ChildTemper, the scripts in data/hexkagune/talk): the scripts read with no mistake in them; every
 * companion has something to come up with for every tier and every mood it can be in, by day and by night, and something to talk about;
 * every child for every age, temperament and mood; nothing in them names anyone the mod never names; picking avoids repeats and favours
 * a scene that fits the moment; a child's temperament is its own for life and leans toward its companion parent's.
 */
public final class SceneTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();
    static final Path TALK = Path.of("src/main/resources/data/hexkagune/talk");
    /** Never in the jar (build.py) nor anywhere a player can read (PaleTests). */
    static final Pattern FORBIDDEN = Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura", Pattern.CASE_INSENSITIVE);

    /** At least this many scenes fit every companion's tier and mood (by day and by night), every child's age, temperament and mood. */
    static final int COMPANION_FIT = 3;
    static final int CHILD_FIT = 2;
    /** At least this many topics for every companion's tier, every child's age. */
    static final int COMPANION_TOPICS = 8;
    static final int CHILD_TOPICS = 6;
    /** At least this many lines in all the scripts together. */
    static final int LINES = 3000;

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) throws IOException {
        parsing();
        Map<String, List<Scene>> books = scripts();
        companions(books);
        children(books.getOrDefault("child", List.of()));
        homecomings(books);
        allies(books.getOrDefault("ally", List.of()));
        picking();
        tempers();
        words();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Scene tests: " + passed + " checks passed");
    }

    /** The form read, and every kind of mistake caught. */
    static void parsing() {
        String good = String.join("\n",
            "# comment",
            "scene hello",
            "who: together",
            "mood: happy giddy",
            "when: night !rain",
            "weight: 2",
            "say: Hi, {name}.",
            "say: Hello.",
            "> Hug her. | +2 hug",
            "  - Mm.",
            "> Why? | +1 -> why",
            "page why",
            "say: Because.",
            "> Okay. | pout",
            "  - Okay.",
            "ignored: Fine.",
            "topic weather",
            "prompt: Nice weather?",
            "say: Is it?",
            "> Yes. | +1",
            "  - It is.");
        SceneBook.Parsed parsed = SceneBook.parse("eto", good);
        check(parsed.errors().isEmpty(), "a good script reads cleanly: " + parsed.errors());
        check(parsed.scenes().size() == 2, "two scenes");
        if (parsed.scenes().size() == 2) {
            Scene hello = parsed.scenes().get(0);
            check(hello.kind() == Scene.Kind.APPROACH && hello.who().equals(Set.of("partner", "wife")) && hello.moods().equals(Set.of("happy", "giddy")),
                "who and mood read");
            check(hello.when().equals(List.of("night", "!rain")) && hello.weight() == 2 && hello.particular() == 1, "when and weight read");
            check(hello.first().lines().size() == 2 && hello.first().options().size() == 2, "its lines and answers");
            Scene.Option hug = hello.first().options().get(0);
            check(hug.regard() == 2 && hug.does("hug") && hug.replies().equals(List.of("Mm.")), "an answer's regard, deed and reply");
            check("why".equals(hello.first().options().get(1).next()) && hello.page("why") != null, "an answer leads on to its page");
            check(hello.ignored().equals(List.of("Fine.")), "what it says left unanswered");
            Scene weather = parsed.scenes().get(1);
            check(weather.kind() == Scene.Kind.TOPIC && "Nice weather?".equals(weather.prompt()), "a topic and its prompt");
            SceneContext c = new SceneContext("wife", "happy", "", Set.of("night"), Map.of("name", "Alex"), 1);
            check(hello.fits(c), "fits a wife, happy, at night, dry");
            check(!hello.fits(new SceneContext("wife", "happy", "", Set.of("night", "rain"), Map.of(), 1)), "not in the rain");
            check(!hello.fits(new SceneContext("close", "happy", "", Set.of("night"), Map.of(), 1)), "not a friend");
            check(!hello.fits(new SceneContext("wife", "sulky", "", Set.of("night"), Map.of(), 1)), "not sulking");
            check(SceneBook.fill("Hi, {name}.", c).equals("Hi, Alex."), "a name put in");
        }
        String[][] bad = {
            {"scene x\nmood: furious\nsay: a\n> b | +1\n  - c\n> d\n  - e", "not one of eto's moods"},
            {"scene x\nwhen: raining\nsay: a\n> b\n  - c\n> d\n  - e", "not one of eto's moments"},
            {"scene x\nsay: a\n> b | fly\n  - c\n> d\n  - e", "not something eto can do"},
            {"scene x\nsay: a\n> b | -> nowhere\n> d\n  - e", "leads to a page it doesn't have"},
            {"scene x\nwho: friend\nsay: a\n> b | kiss\n  - c\n> d\n  - e", "for a couple alone"},
            {"scene x\nsay: Hi {kid}.\n> b\n  - c\n> d\n  - e", "needs 'when: kids'"},
            {"scene x\nsay: Hi {call}.\n> b\n  - c\n> d\n  - e", "not a name"},
            {"scene x\nsay: a\n> b\n  - c", "at least two answers"},
            {"scene x\nsay: a\n> b\n> d\n  - e", "no reply and no page"},
            {"topic x\nsay: a\n> b\n  - c", "a topic needs a prompt"},
            {"scene x\nsay: " + "a".repeat(SceneBook.LINE_MOST + 1) + "\n> b\n  - c\n> d\n  - e", "longer than"},
            {"scene x\nsay: a\n> b\n  - c\n> d\n  - e\nscene x\nsay: a\n> b\n  - c\n> d\n  - e", "a second scene named"},
            {"say: a", "outside any scene"},
        };
        for (String[] each : bad) {
            List<String> errors = SceneBook.parse("eto", each[0]).errors();
            check(errors.stream().anyMatch(e -> e.contains(each[1])), "caught: " + each[1] + " (got " + errors + ")");
        }
        List<String> childErrors = SceneBook.parse("child", "scene x\nwho: teen\ntemper: shy\nmood: proud\nwhen: siblings\nsay: {sib}!\n> a | high_five\n  - b\n> c | gift:poppy\n  - d").errors();
        check(childErrors.isEmpty(), "a child's words read: " + childErrors);
        check(!SceneBook.parse("child", "scene x\ntemper: grumpy\nsay: a\n> b\n  - c\n> d\n  - e").errors().isEmpty(), "a temperament no child has");
        check(!SceneBook.parse("eto", "scene x\nsay: a\n> b | gift:diamond\n  - c\n> d\n  - e").errors().isEmpty(), "a gift not on the list");
    }

    /** Every script read, with no mistake in it, and nothing in it that names anyone the mod never names. */
    static Map<String, List<Scene>> scripts() throws IOException {
        Map<String, List<Scene>> books = new HashMap<>();
        int lines = 0;
        Set<String> speakers = new HashSet<>(SceneWords.COMPANIONS);
        speakers.add(SceneWords.CHILD);
        speakers.add(SceneWords.ALLY);
        for (String speaker : speakers) {
            Path file = TALK.resolve(speaker + ".txt");
            check(Files.exists(file), speaker + " has a script");
            if (!Files.exists(file)) {
                continue;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            SceneBook.Parsed parsed = SceneBook.parse(speaker, text);
            for (String error : parsed.errors()) {
                check(false, speaker + ".txt: " + error);
            }
            check(!FORBIDDEN.matcher(text).find(), speaker + ".txt names no one the mod never names");
            books.put(speaker, parsed.scenes());
            int n = SceneBook.lines(parsed.scenes());
            lines += n;
            System.out.println("  " + speaker + ": " + parsed.scenes().size() + " scenes, " + n + " lines");
        }
        check(lines >= LINES, "thousands of lines between them: " + lines);
        return books;
    }

    /** The moods each companion tier can be in (EtoMood.of), and whether each is a night's only. */
    static final Map<String, List<String>> MOODS = Map.of(
        "friendly", List.of("happy", "sulky", "bored", "sleepy", "hungry"),
        "close", List.of("happy", "sulky", "bored", "sleepy", "hungry"),
        "partner", List.of("happy", "giddy", "sulky", "jealous", "lonely", "clingy", "sleepy", "hungry", "affectionate"),
        "wife", List.of("happy", "giddy", "sulky", "jealous", "lonely", "clingy", "sleepy", "hungry", "affectionate"));
    static final Set<String> NIGHT_ONLY = Set.of("hungry", "affectionate");

    /** Every companion: something to come up with for every tier and mood, day and night, for anyone; things to talk about. */
    static void companions(Map<String, List<Scene>> books) {
        for (String speaker : SceneWords.COMPANIONS) {
            List<Scene> book = books.getOrDefault(speaker, List.of());
            for (Map.Entry<String, List<String>> tier : MOODS.entrySet()) {
                for (String mood : tier.getValue()) {
                    for (String hour : new String[] {"day", "night"}) {
                        if (hour.equals("day") && NIGHT_ONLY.contains(mood)) {
                            continue;
                        }
                        for (String[] who : new String[][] {{"human", "man"}, {"ghoul", "woman"}}) {
                            SceneContext c = new SceneContext(tier.getKey(), mood, "", Set.of(hour, who[0], who[1]), names(), 7);
                            int fit = SceneBook.fitting(book, Scene.Kind.APPROACH, c).size();
                            check(fit >= COMPANION_FIT, speaker + " comes up to a " + tier.getKey() + " (" + mood + ", " + hour + ", " + who[0] + " " + who[1]
                                + ") with " + fit + " scenes");
                        }
                    }
                }
                SceneContext talk = new SceneContext(tier.getKey(), tier.getValue().get(0), "", Set.of("day", "human", "man"), names(), 3);
                int topics = SceneBook.fitting(book, Scene.Kind.TOPIC, talk).size();
                check(topics >= COMPANION_TOPICS, speaker + " has " + topics + " things to talk about with a " + tier.getKey());
            }
            // Particular moments are covered too: rain, the player hurt, a baby on the way, the children, days apart.
            for (String[] moment : new String[][] {{"rain"}, {"hurt"}, {"pregnant"}, {"kids", "kid"}, {"kids", "baby"}, {"away"}}) {
                Set<String> flags = new HashSet<>(Set.of("day", "human", "man", "mixed"));
                flags.addAll(List.of(moment));
                SceneContext c = new SceneContext("wife", "happy", "", flags, names(), 1);
                long particular = SceneBook.fitting(book, Scene.Kind.APPROACH, c).stream().filter(s -> s.when().contains(moment[moment.length - 1])).count();
                check(particular >= 1, speaker + " has something particular for '" + String.join("+", moment) + "': " + particular);
            }
            // 2.1.2: every chapter of the courtship closes with its moment; a couple has dates to go on, by day and by night.
            for (com.hex.kagune.control.npc.Courtship.Chapter chapter : com.hex.kagune.control.npc.Courtship.of(speaker)) {
                Scene moment = book.stream().filter(scene -> scene.id().equals(chapter.id())).findFirst().orElse(null);
                check(moment != null && moment.kind() == Scene.Kind.MOMENT, speaker + " has the moment of " + chapter.id());
                if (moment != null) {
                    check(moment.who().isEmpty() && moment.when().isEmpty(), chapter.id() + " plays whenever its task is done (nothing it waits on)");
                }
            }
            // 2.2.3: coming to make up after a sulk and after jealousy: there, and never come up with but by name.
            for (String id : new String[] {"make_up", "jealous_over"}) {
                Scene made = book.stream().filter(scene -> scene.id().equals(id)).findFirst().orElse(null);
                check(made != null && made.when().contains("makeup") && !made.fits(new SceneContext("partner", "happy", "", Set.of("day", "human", "man"),
                    names(), 1)), speaker + " has " + id + ", only ever asked for by name");
            }
            long dates = book.stream().filter(scene -> scene.kind() == Scene.Kind.DATE).count();
            check(dates >= 6, speaker + " has " + dates + " dates");
            for (String tier : new String[] {"partner", "wife"}) {
                for (String hour : new String[] {"day", "night"}) {
                    SceneContext c = new SceneContext(tier, "happy", "", Set.of(hour, "human", "man"), names(), 5);
                    int fit = SceneBook.fitting(book, Scene.Kind.DATE, c).size();
                    check(fit >= 3, speaker + " has " + fit + " dates for a " + tier + " by " + hour);
                }
            }
            for (Scene scene : book) {
                check(scene.kind() == Scene.Kind.TOPIC || scene.kind() == Scene.Kind.DATE || !scene.ignored().isEmpty(),
                    speaker + "." + scene.id() + " says something if left unanswered");
            }
        }
    }

    static Map<String, String> names() {
        return Map.of("name", "Alex", "kid", "Mio", "kids", "the kids", "self", "Her", "she", "she", "her", "her");
    }

    /** Every child: for every age, temperament and mood, something to come up with; things to talk about at every age. */
    static void children(List<Scene> book) {
        String[] tiers = {"baby", "toddler", "child", "teen"};
        for (String tier : tiers) {
            for (String temper : SceneWords.TEMPERS) {
                for (String mood : SceneWords.CHILD_MOODS) {
                    for (String[] who : new String[][] {{"day", "mother", "girl"}, {"night", "father", "boy"}}) {
                        SceneContext c = new SceneContext(tier, mood, temper, Set.of(who[0], who[1], who[2], "human"), Map.of("call", "Mama", "me", "Mio"), 5);
                        int fit = SceneBook.fitting(book, Scene.Kind.APPROACH, c).size();
                        check(fit >= CHILD_FIT, "a " + temper + " " + tier + " (" + mood + ", " + who[0] + ", to its " + who[1] + ") comes up with " + fit
                            + " scenes");
                    }
                }
            }
            SceneContext talk = new SceneContext(tier, "happy", "sunny", Set.of("day", "mother", "girl", "human"), Map.of(), 2);
            int topics = SceneBook.fitting(book, Scene.Kind.TOPIC, talk).size();
            check(topics >= CHILD_TOPICS, "a " + tier + " has " + topics + " things to talk about");
        }
        for (Scene scene : book) {
            check(scene.kind() == Scene.Kind.TOPIC || !scene.ignored().isEmpty(), "child." + scene.id() + " says something if left unanswered");
            // A moment an answer starts fits at least one of the ages the scene is for (or it would never happen).
            List<Scene.Page> pages = new ArrayList<>(scene.pages().values());
            pages.add(scene.first());
            for (Scene.Page page : pages) {
                for (Scene.Option option : page.options()) {
                    for (String effect : option.effects()) {
                        ChildMoment moment;
                        try {
                            moment = ChildMoment.valueOf(effect.toUpperCase(java.util.Locale.ROOT));
                        } catch (IllegalArgumentException notMoment) {
                            continue;
                        }
                        boolean fits = false;
                        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
                            String word = stage == com.hex.kagune.control.family.ChildStage.CHILD ? "child" : stage.name().toLowerCase(java.util.Locale.ROOT);
                            if ((scene.who().isEmpty() || scene.who().contains(word)) && moment.fits(stage)) {
                                fits = true;
                            }
                        }
                        check(fits, "child." + scene.id() + ": '" + effect + "' fits one of its ages " + scene.who());
                    }
                }
            }
        }
    }

    /**
     * 3.2.1: bringing a child home. Every companion meets the player at the door ("brought") whoever they are to each other, in every mood,
     * found or back from an outing, carrying the child or not, by day and by night; every child of every age and temper has its own word
     * ("back"), found or not, by day and by night. Nothing about a homecoming comes up but at one: "found" only with "brought" or "back".
     */
    static void homecomings(Map<String, List<Scene>> books) {
        for (String speaker : SceneWords.COMPANIONS) {
            List<Scene> book = books.getOrDefault(speaker, List.of());
            List<Scene> brought = book.stream().filter(scene -> scene.when().contains("brought")).toList();
            check(brought.size() >= 10, speaker + " has " + brought.size() + " scenes for a child brought home");
            for (Map.Entry<String, List<String>> tier : MOODS.entrySet()) {
                for (String mood : tier.getValue()) {
                    for (String hour : new String[] {"day", "night"}) {
                        if (hour.equals("day") && NIGHT_ONLY.contains(mood)) {
                            continue;
                        }
                        for (boolean found : new boolean[] {false, true}) {
                            for (boolean carrying : new boolean[] {false, true}) {
                                Set<String> flags = new HashSet<>(Set.of(hour, "human", "man", "kids", "home", "brought"));
                                if (found) {
                                    flags.add("found");
                                }
                                if (carrying) {
                                    flags.add("carrying");
                                }
                                int fit = SceneBook.fitting(brought, Scene.Kind.APPROACH, new SceneContext(tier.getKey(), mood, "", flags, names(), 7)).size();
                                check(fit >= 1, speaker + " meets a " + tier.getKey() + " (" + mood + ", " + hour + (found ? ", found" : ", an outing")
                                    + (carrying ? ", carrying" : "") + ") bringing the child home with " + fit + " scenes");
                            }
                        }
                    }
                }
            }
            for (Scene scene : brought) {
                check(scene.kind() == Scene.Kind.APPROACH && !scene.fits(new SceneContext("wife", "happy", "", Set.of("day", "human", "man", "kids", "home"),
                    names(), 1)), speaker + "." + scene.id() + " comes up only with a child just brought home");
            }
        }
        List<Scene> child = books.getOrDefault("child", List.of());
        List<Scene> back = child.stream().filter(scene -> scene.when().contains("back")).toList();
        check(back.size() >= 50, "children have " + back.size() + " scenes for being brought home");
        for (String tier : new String[] {"baby", "toddler", "child", "teen"}) {
            for (String temper : SceneWords.TEMPERS) {
                for (String mood : SceneWords.CHILD_MOODS) {
                    for (String hour : new String[] {"day", "night"}) {
                        for (boolean found : new boolean[] {false, true}) {
                            if (tier.equals("baby") && found) {
                                continue; // a baby goes nowhere on its own
                            }
                            Set<String> flags = new HashSet<>(Set.of(hour, "mother", "girl", "human", "home", "back"));
                            if (found) {
                                flags.add("found");
                            }
                            SceneContext c = new SceneContext(tier, mood, temper, flags, Map.of("call", "Mama", "me", "Mio", "other", "Papa"), 5);
                            int fit = SceneBook.fitting(back, Scene.Kind.APPROACH, c).size();
                            int want = tier.equals("baby") ? 1 : 2;
                            check(fit >= want, "a " + temper + " " + tier + " (" + mood + ", " + hour + (found ? ", found" : ", an outing") + ") brought home has "
                                + fit + " scenes");
                            if (!tier.equals("baby")) {
                                long own = SceneBook.fitting(back, Scene.Kind.APPROACH, c).stream().filter(scene -> scene.tempers().contains(temper)).count();
                                check(own >= 1, "a " + temper + " " + tier + " brought home (" + (found ? "found" : "an outing") + ") has a scene of its own temper");
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, List<Scene>> book : books.entrySet()) {
            for (Scene scene : book.getValue()) {
                boolean aboutFound = scene.when().contains("found") || scene.when().contains("!found");
                check(!aboutFound || scene.when().contains("back") || scene.when().contains("brought"),
                    book.getKey() + "." + scene.id() + ": 'found' only means something with a child brought home");
            }
        }
    }

    /** Every ally temper: something to come up with in every mood, by day and by night (hungry: at night); things to talk about. */
    static void allies(List<Scene> book) {
        for (String tier : SceneWords.ALLY_TIERS) {
            for (String mood : SceneWords.ALLY_MOODS) {
                for (String hour : new String[] {"day", "night"}) {
                    if (mood.equals("hungry") && hour.equals("day")) {
                        continue;
                    }
                    SceneContext c = new SceneContext(tier, mood, "", Set.of(hour, "ghoul", "man", "boy", "following"), Map.of("name", "Alex", "me", "Rin"), 4);
                    int fit = SceneBook.fitting(book, Scene.Kind.APPROACH, c).size();
                    check(fit >= 2, "a " + tier + " ally (" + mood + ", " + hour + ") comes up with " + fit + " scenes");
                }
            }
            SceneContext talk = new SceneContext(tier, "happy", "", Set.of("day", "ghoul", "man", "boy"), Map.of(), 2);
            int topics = SceneBook.fitting(book, Scene.Kind.TOPIC, talk).size();
            check(topics >= 6, "a " + tier + " ally has " + topics + " things to talk about");
        }
        for (Scene scene : book) {
            check(scene.kind() == Scene.Kind.TOPIC || !scene.ignored().isEmpty(), "ally." + scene.id() + " says something if left unanswered");
        }
    }

    /** A pick: a scene that fits, never one said lately (unless nothing else fits), a particular one favoured. */
    static void picking() {
        String script = String.join("\n",
            "scene plain", "say: a", "> b", "  - c", "> d", "  - e", "ignored: f",
            "scene rainy", "when: rain", "say: a", "> b", "  - c", "> d", "  - e", "ignored: f",
            "scene never", "who: wife", "say: a", "> b", "  - c", "> d", "  - e", "ignored: f");
        List<Scene> book = SceneBook.parse("eto", script).scenes();
        SceneContext dry = new SceneContext("close", "happy", "", Set.of("day"), Map.of(), 1);
        SceneContext wet = new SceneContext("close", "happy", "", Set.of("day", "rain"), Map.of(), 1);
        for (double roll = 0.0; roll < 1.0; roll += 0.05) {
            Scene a = SceneBook.pick(book, Scene.Kind.APPROACH, dry, roll, List.of());
            check(a != null && a.id().equals("plain"), "only what fits (" + roll + ")");
            Scene b = SceneBook.pick(book, Scene.Kind.APPROACH, wet, roll, List.of("eto.rainy"));
            check(b != null && b.id().equals("plain"), "not what was said lately (" + roll + ")");
            Scene c = SceneBook.pick(book, Scene.Kind.APPROACH, dry, roll, List.of("eto.plain"));
            check(c != null && c.id().equals("plain"), "what was said lately, if nothing else fits (" + roll + ")");
        }
        int rainy = 0;
        for (double roll = 0.0; roll < 1.0; roll += 0.01) {
            Scene s = SceneBook.pick(book, Scene.Kind.APPROACH, wet, roll, List.of());
            rainy += s != null && s.id().equals("rainy") ? 1 : 0;
        }
        check(rainy > 55, "the rain's own scene comes up more in the rain: " + rainy + " of 100");
        check(SceneBook.pick(book, Scene.Kind.TOPIC, dry, 0.5, List.of()) == null, "no topic: none");
        check(SceneBook.fill("{call}, {missing} look", new SceneContext("toddler", "happy", "shy", Set.of(), Map.of("call", "Papa"), 1)).equals("Papa, look"),
            "a name with no value is left out cleanly");
    }

    /** A child's temperament: always the same for it, every one of them turns up, a companion parent's leaning shows; its mood. */
    static void tempers() {
        check(ChildTemper.of("abc", "player:x", "player:y") == ChildTemper.of("abc", "player:x", "player:y"), "always the same for the same child");
        Map<ChildTemper, Integer> any = new EnumMap<>(ChildTemper.class);
        Map<ChildTemper, Integer> touka = new EnumMap<>(ChildTemper.class);
        Map<ChildTemper, Integer> ken = new EnumMap<>(ChildTemper.class);
        Map<ChildTemper, Integer> eto = new EnumMap<>(ChildTemper.class);
        for (int i = 0; i < 7000; i++) {
            String id = "child-" + i;
            any.merge(ChildTemper.of(id, "player:a", "player:b"), 1, Integer::sum);
            touka.merge(ChildTemper.of(id, "npc:touka", "player:b"), 1, Integer::sum);
            ken.merge(ChildTemper.of(id, "player:a", "npc:ken"), 1, Integer::sum);
            eto.merge(ChildTemper.of(id, "npc:eto", "player:b"), 1, Integer::sum);
        }
        for (ChildTemper t : ChildTemper.values()) {
            check(any.getOrDefault(t, 0) > 700, t + " turns up: " + any.get(t));
            check(touka.getOrDefault(t, 0) > 200 && ken.getOrDefault(t, 0) > 200 && eto.getOrDefault(t, 0) > 200, t + " turns up in every family");
        }
        check(touka.get(ChildTemper.BOLD) > any.get(ChildTemper.BOLD) * 1.4 && touka.get(ChildTemper.SERIOUS) > any.get(ChildTemper.SERIOUS) * 1.3,
            "Touka's children lean bold and serious");
        check(ken.get(ChildTemper.GENTLE) > any.get(ChildTemper.GENTLE) * 1.4, "Ken's lean gentle");
        check(eto.get(ChildTemper.CURIOUS) > any.get(ChildTemper.CURIOUS) * 1.4 && eto.get(ChildTemper.MISCHIEF) > any.get(ChildTemper.MISCHIEF) * 1.4,
            "Eto's lean curious and mischievous");
        check(ChildTemper.mood(true, true, true, true, true, true, true, true, true, 0.1) == ChildTemper.Mood.HURT, "hurt first");
        check(ChildTemper.mood(false, true, true, false, false, false, false, false, false, 0.1) == ChildTemper.Mood.SAD, "brushed off: sad");
        check(ChildTemper.mood(false, false, true, false, false, false, false, false, false, 0.1) == ChildTemper.Mood.SCARED, "a storm: scared");
        check(ChildTemper.mood(false, false, false, true, false, false, true, false, false, 0.1) == ChildTemper.Mood.SCARED, "something out there in the dark");
        check(ChildTemper.mood(false, false, false, true, false, false, false, false, false, 0.1) != ChildTemper.Mood.SCARED, "by day, braver");
        check(ChildTemper.mood(false, false, false, false, true, false, false, false, false, 0.1) == ChildTemper.Mood.MISSED, "back after a long while: missed");
        check(ChildTemper.mood(false, false, false, false, false, true, false, false, false, 0.1) == ChildTemper.Mood.PROUD, "a lesson today: proud");
        check(ChildTemper.mood(false, false, false, false, false, false, true, true, false, 0.1) == ChildTemper.Mood.SLEEPY, "late: sleepy");
        check(ChildTemper.mood(false, false, false, false, false, false, false, false, false, 0.1) == ChildTemper.Mood.HAPPY, "otherwise happy");
        for (ChildTemper.Mood mood : ChildTemper.Mood.values()) {
            check(SceneWords.CHILD_MOODS.contains(mood.word()), mood + " is a word the scripts know");
        }
        for (ChildTemper t : ChildTemper.values()) {
            check(SceneWords.TEMPERS.contains(t.word()), t + " is a word the scripts know");
        }
    }

    /** Every word the scripts may use is one the game does something with. */
    static void words() {
        for (String gesture : SceneWords.GESTURES) {
            boolean known;
            try {
                EtoGesture.valueOf(gesture.toUpperCase(java.util.Locale.ROOT));
                known = true;
            } catch (IllegalArgumentException unknown) {
                known = false;
            }
            check(known, "the gesture " + gesture + " is one she can do");
        }
        Set<String> deeds = Set.of("up", "down", "shoulders", "hand", "follow", "stay", "bed", "home", "teach", "sad", "proud", "close", "play");
        for (String effect : SceneWords.CHILD_EFFECTS) {
            boolean known = deeds.contains(effect);
            if (!known) {
                try {
                    ChildMoment.valueOf(effect.toUpperCase(java.util.Locale.ROOT));
                    known = true;
                } catch (IllegalArgumentException unknown) {
                    known = false;
                }
            }
            check(known, "the child's " + effect + " is something it can do");
        }
        for (String gift : SceneWords.GIFTS) {
            check(gift.matches("[a-z_]+"), "the gift " + gift + " is an item's name");
        }
    }
}
