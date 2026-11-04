package com.hex.kagune.control.npc.scene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.3.29: a speaker's script, read (pure): {@code data/hexkagune/talk/<speaker>.txt}, a plain text file of scenes, so there can be
 * thousands of lines and each stays easy to read and to add to. The form:
 *
 * <pre>
 * # a comment
 * scene rain_window                      (something it comes up to them with; "topic rain" for something they can bring up;
 *                                         2.1.2: "moment eto_letter" a courtship chapter's, "date stargazing" a date's)
 * prompt: What's the rain like?           (a topic's: what the player says to bring it up)
 * who: together                           (who it is for: tiers, or together / friend / anyone; a child's: baby toddler child teen...)
 * mood: clingy affectionate happy         (any of these moods; none: any mood)
 * temper: shy gentle                      (a child's: any of these temperaments)
 * when: rain !thunder                     (every one of these moments, none of the "!" ones)
 * weight: 2                               (how often, against the others: 1 if not said)
 * say: It's raining. Sit with me.         (what it says first: one of these, picked)
 * say: Rain. Perfect.
 * > Sit with her. | +2 cling              (an answer: its words, then what it does: regard, gestures, deeds, gift:item, -> a page)
 *   - Mm. Good. Now shh.                  (what it says back: one of these, picked)
 * > Ask what's wrong. | +1 -> why
 * page why                                (a page an answer leads to)
 * say: ...
 * > ...
 * ignored: Fine. Be like that.            (an approach left unanswered: what it says, one of these)
 * </pre>
 *
 * Every word is checked against {@link SceneWords}; {@link #parse} returns the scenes and every mistake (with its line), and the tests
 * refuse a script with any.
 */
public final class SceneBook {
    /** Longest line a speaker says, longest answer (it shares a row with another when a page has many), most answers on a page. */
    public static final int LINE_MOST = 420;
    public static final int LABEL_MOST = 50;
    public static final int OPTIONS_MOST = 7;

    private static final Pattern NAME = Pattern.compile("\\{([a-z]+)}");

    private SceneBook() {
    }

    /** A script read: its scenes, and its mistakes ("line 12: ..."). */
    public record Parsed(List<Scene> scenes, List<String> errors) {
    }

    // ---------------------------------------------------------------- reading

    private static final class SceneDraft {
        String id;
        Scene.Kind kind;
        String prompt;
        final Set<String> who = new LinkedHashSet<>();
        final Set<String> moods = new LinkedHashSet<>();
        final Set<String> tempers = new LinkedHashSet<>();
        final List<String> when = new ArrayList<>();
        int weight = 1;
        final List<PageDraft> pages = new ArrayList<>();
        final List<String> ignored = new ArrayList<>();
        int line;
    }

    private static final class PageDraft {
        String id;
        final List<String> lines = new ArrayList<>();
        final List<OptionDraft> options = new ArrayList<>();
        int line;
    }

    private static final class OptionDraft {
        String label;
        int regard;
        final List<String> effects = new ArrayList<>();
        String next;
        final List<String> replies = new ArrayList<>();
        int line;
    }

    /** {@code speaker}'s script {@code text} read. */
    public static Parsed parse(String speaker, String text) {
        List<String> errors = new ArrayList<>();
        List<SceneDraft> drafts = new ArrayList<>();
        SceneDraft scene = null;
        PageDraft page = null;
        OptionDraft option = null;
        String[] lines = text.split("\r?\n", -1);
        for (int n = 0; n < lines.length; n++) {
            String raw = lines[n];
            String line = raw.strip();
            int at = n + 1;
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("scene ") || line.startsWith("topic ") || line.startsWith("moment ") || line.startsWith("date ")) {
                scene = new SceneDraft();
                int space = line.indexOf(' ');
                scene.id = line.substring(space + 1).strip();
                scene.kind = switch (line.substring(0, space)) {
                    case "scene" -> Scene.Kind.APPROACH;
                    case "moment" -> Scene.Kind.MOMENT;
                    case "date" -> Scene.Kind.DATE;
                    default -> Scene.Kind.TOPIC;
                };
                scene.line = at;
                if (!scene.id.matches("[a-z0-9_]+")) {
                    errors.add("line " + at + ": a scene's name is lower case letters, digits and _: " + scene.id);
                }
                page = new PageDraft();
                page.id = "start";
                page.line = at;
                scene.pages.add(page);
                option = null;
                drafts.add(scene);
                continue;
            }
            if (scene == null) {
                errors.add("line " + at + ": outside any scene: " + line);
                continue;
            }
            if (line.startsWith("page ")) {
                page = new PageDraft();
                page.id = line.substring(5).strip();
                page.line = at;
                if (!page.id.matches("[a-z0-9_]+") || "start".equals(page.id)) {
                    errors.add("line " + at + ": a page's name is lower case letters, digits and _ (not 'start'): " + page.id);
                }
                scene.pages.add(page);
                option = null;
                continue;
            }
            if (line.startsWith(">")) {
                option = new OptionDraft();
                option.line = at;
                String body = line.substring(1).strip();
                int bar = body.indexOf('|');
                option.label = (bar < 0 ? body : body.substring(0, bar)).strip();
                if (bar >= 0) {
                    effects(speaker, body.substring(bar + 1).strip(), option, errors, at);
                }
                if (option.label.isEmpty()) {
                    errors.add("line " + at + ": an answer with no words");
                }
                page.options.add(option);
                continue;
            }
            if (line.startsWith("- ")) {
                if (option == null) {
                    errors.add("line " + at + ": a reply with no answer before it");
                } else {
                    option.replies.add(line.substring(2).strip());
                }
                continue;
            }
            int colon = line.indexOf(':');
            String key = colon < 0 ? "" : line.substring(0, colon).strip();
            String value = colon < 0 ? "" : line.substring(colon + 1).strip();
            SceneDraft current = scene;
            PageDraft currentPage = page;
            switch (key) {
                case "say" -> {
                    if (!currentPage.options.isEmpty()) {
                        errors.add("line " + at + ": 'say' after the answers (say comes first on a page)");
                    }
                    currentPage.lines.add(value);
                }
                case "prompt" -> current.prompt = value;
                case "who" -> words(value, w -> {
                    Set<String> tiers = SceneWords.tiers(w);
                    for (String tier : tiers) {
                        if (!SceneWords.tiersOf(speaker).contains(tier)) {
                            errors.add("line " + at + ": not one of " + speaker + "'s tiers: " + w);
                        }
                    }
                    current.who.addAll(tiers);
                });
                case "mood" -> words(value, w -> {
                    if (!SceneWords.moodsOf(speaker).contains(w)) {
                        errors.add("line " + at + ": not one of " + speaker + "'s moods: " + w);
                    }
                    current.moods.add(w);
                });
                case "temper" -> words(value, w -> {
                    if (!SceneWords.child(speaker) || !SceneWords.TEMPERS.contains(w)) {
                        errors.add("line " + at + ": not a child's temperament: " + w);
                    }
                    current.tempers.add(w);
                });
                case "when" -> words(value, w -> {
                    String flag = w.startsWith("!") ? w.substring(1) : w;
                    if (!SceneWords.flagsOf(speaker).contains(flag)) {
                        errors.add("line " + at + ": not one of " + speaker + "'s moments: " + w);
                    }
                    current.when.add(w);
                });
                case "weight" -> {
                    try {
                        current.weight = Math.max(1, Math.min(20, Integer.parseInt(value)));
                    } catch (NumberFormatException bad) {
                        errors.add("line " + at + ": weight is a number: " + value);
                    }
                }
                case "ignored" -> current.ignored.add(value);
                default -> errors.add("line " + at + ": not understood: " + line);
            }
        }
        List<Scene> scenes = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (SceneDraft draft : drafts) {
            if (!ids.add(draft.id)) {
                errors.add("line " + draft.line + ": a second scene named " + draft.id);
            }
            Scene built = build(speaker, draft, errors);
            if (built != null) {
                scenes.add(built);
            }
        }
        return new Parsed(List.copyOf(scenes), List.copyOf(errors));
    }

    private static void words(String value, java.util.function.Consumer<String> each) {
        for (String w : value.split("[\\s,]+")) {
            if (!w.isEmpty()) {
                each.accept(w);
            }
        }
    }

    private static void effects(String speaker, String text, OptionDraft option, List<String> errors, int at) {
        String[] tokens = text.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isEmpty()) {
                continue;
            }
            if (token.matches("[+-]?[0-9]+")) {
                option.regard += Integer.parseInt(token);
            } else if (token.equals("->")) {
                if (i + 1 < tokens.length) {
                    option.next = tokens[++i];
                } else {
                    errors.add("line " + at + ": '->' leads nowhere");
                }
            } else if (token.startsWith("->")) {
                option.next = token.substring(2);
            } else if (SceneWords.effect(speaker, token)) {
                option.effects.add(token);
            } else {
                errors.add("line " + at + ": not something " + speaker + " can do: " + token);
            }
        }
    }

    private static Scene build(String speaker, SceneDraft draft, List<String> errors) {
        String where = "scene " + draft.id + " (line " + draft.line + ")";
        if ((draft.kind == Scene.Kind.TOPIC || draft.kind == Scene.Kind.DATE) && (draft.prompt == null || draft.prompt.isEmpty())) {
            errors.add(where + ": a " + (draft.kind == Scene.Kind.DATE ? "date" : "topic") + " needs a prompt");
        }
        if (draft.kind == Scene.Kind.DATE && (draft.who.isEmpty() || !SceneWords.tiers("together").containsAll(draft.who))) {
            errors.add(where + ": a date is for a couple alone (who: together)");
        }
        if ((draft.kind == Scene.Kind.MOMENT || draft.kind == Scene.Kind.DATE) && !SceneWords.COMPANIONS.contains(speaker)) {
            errors.add(where + ": only a companion has moments and dates");
        }
        if (draft.prompt != null && draft.prompt.length() > LABEL_MOST) {
            errors.add(where + ": its prompt is longer than " + LABEL_MOST + ": " + draft.prompt);
        }
        Map<String, Scene.Page> pages = new LinkedHashMap<>();
        Set<String> pageIds = new LinkedHashSet<>();
        for (PageDraft page : draft.pages) {
            if (!pageIds.add(page.id)) {
                errors.add(where + ": a second page named " + page.id);
            }
        }
        boolean together = !draft.who.isEmpty() && SceneWords.tiers("together").containsAll(draft.who);
        Set<String> needs = new LinkedHashSet<>();
        for (String flag : draft.when) {
            if (!flag.startsWith("!")) {
                needs.add(flag);
            }
        }
        for (PageDraft page : draft.pages) {
            if (page.lines.isEmpty()) {
                errors.add(where + ": page " + page.id + " says nothing (a 'say:' line)");
            }
            if (page.options.size() > OPTIONS_MOST) {
                errors.add(where + ": page " + page.id + " has more than " + OPTIONS_MOST + " answers");
            }
            if ((draft.kind == Scene.Kind.APPROACH || draft.kind == Scene.Kind.MOMENT) && page.id.equals("start") && page.options.size() < 2) {
                errors.add(where + ": something it comes up with needs at least two answers");
            }
            for (String line : page.lines) {
                text(speaker, line, LINE_MOST, needs, where, errors);
            }
            List<Scene.Option> options = new ArrayList<>();
            for (OptionDraft option : page.options) {
                text(speaker, option.label, LABEL_MOST, needs, where + " line " + option.line, errors);
                for (String reply : option.replies) {
                    text(speaker, reply, LINE_MOST, needs, where + " line " + option.line, errors);
                }
                if (option.replies.isEmpty() && option.next == null) {
                    errors.add(where + " line " + option.line + ": an answer with no reply and no page after it");
                }
                if (option.next != null && !pageIds.contains(option.next)) {
                    errors.add(where + " line " + option.line + ": leads to a page it doesn't have: " + option.next);
                }
                if (SceneWords.COMPANIONS.contains(speaker) && !together) {
                    for (String effect : option.effects) {
                        if (SceneWords.TOGETHER_ONLY.contains(effect)) {
                            errors.add(where + " line " + option.line + ": '" + effect + "' is for a couple alone (who: partner/wife/together)");
                        }
                    }
                }
                options.add(new Scene.Option(option.label, option.regard, List.copyOf(option.effects), option.next, List.copyOf(option.replies)));
            }
            pages.put(page.id, new Scene.Page(page.id, List.copyOf(page.lines), List.copyOf(options)));
        }
        for (String line : draft.ignored) {
            text(speaker, line, LINE_MOST, needs, where, errors);
        }
        if (draft.pages.isEmpty()) {
            return null;
        }
        Scene.Page first = pages.get("start");
        Map<String, Scene.Page> rest = new LinkedHashMap<>(pages);
        rest.remove("start");
        return new Scene(speaker, draft.id, draft.kind, draft.prompt, Set.copyOf(draft.who), Set.copyOf(draft.moods), Set.copyOf(draft.tempers),
            List.copyOf(draft.when), draft.weight, first, Map.copyOf(rest), List.copyOf(draft.ignored));
    }

    /** A line or label checked: not too long, its names known to the speaker's family and their moments needed by the scene. */
    private static void text(String speaker, String text, int most, Set<String> needs, String where, List<String> errors) {
        if (text.isEmpty()) {
            errors.add(where + ": an empty line");
        }
        if (text.length() > most) {
            errors.add(where + ": longer than " + most + " (" + text.length() + "): " + text);
        }
        if (count(text, '{') != count(text, '}')) {
            errors.add(where + ": unbalanced braces: " + text);
        }
        Matcher m = NAME.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!SceneWords.namesOf(speaker).contains(name)) {
                errors.add(where + ": not a name " + speaker + "'s lines can hold: {" + name + "}");
            }
            String need = SceneWords.needs(name);
            if (need != null && !"claimed".equals(need) && !needs.contains(need)) {
                errors.add(where + ": {" + name + "} needs 'when: " + need + "'");
            }
        }
    }

    private static int count(String text, char c) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- using it

    /** {@code line} with its names put in ({@code {name}}, {@code {call}}...); a name it has no value for is left out cleanly. */
    public static String fill(String line, SceneContext c) {
        Matcher m = NAME.matcher(line);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = c.names().get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value));
        }
        m.appendTail(out);
        return out.toString().replace("  ", " ").replace(" ,", ",").replace(" .", ".");
    }

    /** One of {@code variants}, by {@code seed}. */
    public static String one(List<String> variants, int seed) {
        return variants.isEmpty() ? "" : variants.get(Math.floorMod(seed, variants.size()));
    }

    /** Every scene of {@code kind} in {@code scenes} that fits {@code c}. */
    public static List<Scene> fitting(Collection<Scene> scenes, Scene.Kind kind, SceneContext c) {
        List<Scene> out = new ArrayList<>();
        for (Scene scene : scenes) {
            if (scene.kind() == kind && scene.fits(c)) {
                out.add(scene);
            }
        }
        return out;
    }

    /**
     * The scene to come up with: one of {@code kind} that fits {@code c}, not among the {@code recent} ones (unless nothing else
     * fits), picked by {@code roll} in [0, 1) by weight; a particular one (it needs rain, the player hurt, a baby on the way...) counts
     * for more, so it comes up while it can. Null if none fits.
     */
    public static Scene pick(Collection<Scene> scenes, Scene.Kind kind, SceneContext c, double roll, Collection<String> recent) {
        List<Scene> fit = fitting(scenes, kind, c);
        List<Scene> fresh = new ArrayList<>();
        for (Scene scene : fit) {
            if (!recent.contains(scene.key())) {
                fresh.add(scene);
            }
        }
        List<Scene> from = fresh.isEmpty() ? fit : fresh;
        if (from.isEmpty()) {
            return null;
        }
        double total = 0.0;
        for (Scene scene : from) {
            total += weight(scene);
        }
        double at = Math.max(0.0, Math.min(0.999999, roll)) * total;
        for (Scene scene : from) {
            at -= weight(scene);
            if (at < 0.0) {
                return scene;
            }
        }
        return from.get(from.size() - 1);
    }

    /** Its weight in a pick: its own, more for each moment it needs. */
    public static double weight(Scene scene) {
        return scene.weight() * (1.0 + 1.5 * scene.particular());
    }

    /** How many lines a script holds: every line said, every answer, every reply. */
    public static int lines(Collection<Scene> scenes) {
        int n = 0;
        for (Scene scene : scenes) {
            n += scene.ignored().size() + (scene.prompt() == null ? 0 : 1);
            List<Scene.Page> all = new ArrayList<>(scene.pages().values());
            all.add(scene.first());
            for (Scene.Page page : all) {
                n += page.lines().size();
                for (Scene.Option option : page.options()) {
                    n += 1 + option.replies().size();
                }
            }
        }
        return n;
    }
}
