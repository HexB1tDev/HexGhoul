package com.hex.kagune.control.quest;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcIdentity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 7.0.1: one ghoul's quests (pure, tested): each quest's progress, which are done or closed for good, the gates opened, mutation
 * points earned, the side taken, and the hunts on. The server feeds it {@link QuestEvent}s ({@link #apply}) and stores it in the
 * profile root as one string ({@link #save}, {@link #load}), so death carries it and {@code /kagune reset} clears it.
 *
 * <p>A quest's state is worked out, not stored, until it ends: {@link State#LOCKED} until the ghoul is on its path with the
 * mastery and the quests it comes after done, then {@link State#OPEN} (its objectives count), then {@link State#DONE} (paid, its
 * gates open) or {@link State#CLOSED} (a rival quest in its exclusive group was done first; a hunt that ran out).
 */
public final class QuestLog {
    /** A kill this soon (ticks) after the last one carries the rampage on. */
    public static final int RAMPAGE_TICKS = 200;
    /** The newest save format; {@link #migrated} below it means the old progression was never looked at. */
    public static final int VERSION = 1;

    public enum State {
        LOCKED, OPEN, DONE, CLOSED
    }

    /** One step forward: {@code quest}'s objective {@code objective} from {@code before} to {@code after}. */
    public record Step(Quest quest, int objective, int before, int after) {
        public boolean finished() {
            return this.after >= this.quest.objectives().get(this.objective).count();
        }
    }

    /** A hunt on: its quest, and the game time it ends. */
    public record Hunt(Hunts.Spec spec, Quest quest, long endsAt) {
    }

    /** 7.0.2: a favour on: what it asks, the ghoul who asked (its UUID and who it is), the how-manyth it asked, its quest. */
    public record Favour(Favours.Spec spec, String giver, long identity, int number, Quest quest) {
    }

    /**
     * 7.0.2: how the ghoul stands with one ghoul NPC: its {@code regard} ({@link NpcBonds}), the day they last chatted (game days),
     * flags ({@link NpcBonds#MET}, {@link NpcBonds#BEFRIENDED}, {@link NpcBonds#ASKED}, {@link NpcBonds#ALLY}) and the favours done for it.
     */
    public record Bond(int regard, long chatted, int flags, int favours) {
        public static final Bond NONE = new Bond(0, Long.MIN_VALUE, 0, 0);

        public boolean has(int flag) {
            return (this.flags & flag) != 0;
        }

        public Bond regard(int regard) {
            return new Bond(NpcBonds.clamp(regard), this.chatted, this.flags, this.favours);
        }

        public Bond chatted(long day) {
            return new Bond(this.regard, day, this.flags, this.favours);
        }

        public Bond with(int flag, boolean on) {
            return new Bond(this.regard, this.chatted, on ? this.flags | flag : this.flags & ~flag, this.favours);
        }

        public Bond favourDone() {
            return new Bond(this.regard, this.chatted, this.flags, this.favours + 1);
        }
    }

    private final Map<String, int[]> progress = new LinkedHashMap<>();
    private final Map<String, State> ended = new LinkedHashMap<>();
    private final EnumSet<Gate> gates = EnumSet.noneOf(Gate.class);
    private final Map<String, Integer> sides = new LinkedHashMap<>();
    private final List<Hunt> hunts = new ArrayList<>();
    private final List<Favour> favours = new ArrayList<>();
    private final Map<String, Bond> bonds = new LinkedHashMap<>();
    private int points;
    private int migrated;
    /** The game time the next hunt may be offered (0: as soon as it is dusk). */
    private long nextHuntAt;

    public QuestLog() {
    }

    // ------------------------------------------------------------------ reading

    public State state(Quest quest, KagunePath path, int mastery) {
        State stored = this.ended.get(quest.id());
        if (stored != null) {
            return stored;
        }
        if (!quest.on(path) || mastery < quest.mastery()) {
            return State.LOCKED;
        }
        // 1.3.0: one of Eto's lessons waits for her to teach it.
        int lesson = com.hex.kagune.control.npc.EtoRules.lessonFlag(quest.id());
        if (lesson != 0 && !this.bond(com.hex.kagune.control.npc.EtoRules.KEY).has(lesson)) {
            return State.LOCKED;
        }
        for (String before : quest.after()) {
            if (this.ended.get(before) != State.DONE) {
                return State.LOCKED;
            }
        }
        return State.OPEN;
    }

    /** Progress on {@code quest}'s objective {@code index}. */
    public int progress(Quest quest, int index) {
        int[] values = this.progress.get(quest.id());
        return values == null || index >= values.length ? 0 : values[index];
    }

    public boolean open(Gate gate) {
        return this.gates.contains(gate);
    }

    public Set<Gate> gates() {
        return EnumSet.copyOf(this.gates);
    }

    /** Mutation points earned from quests (spent alongside the mastery milestones'). */
    public int points() {
        return this.points;
    }

    /** How far the ghoul is on {@code side} (each quest for it adds one). */
    public int side(String side) {
        return this.sides.getOrDefault(side, 0);
    }

    public List<Hunt> hunts() {
        return List.copyOf(this.hunts);
    }

    /** 7.0.2: the favours on. */
    public List<Favour> favours() {
        return List.copyOf(this.favours);
    }

    /** The favour {@code quest} is, or null. */
    public Favour favour(Quest quest) {
        for (Favour favour : this.favours) {
            if (favour.quest().id().equals(quest.id())) {
                return favour;
            }
        }
        return null;
    }

    /** The favour on for the ghoul {@code giver}, or null. */
    public Favour favourFrom(String giver) {
        for (Favour favour : this.favours) {
            if (favour.giver().equals(giver)) {
                return favour;
            }
        }
        return null;
    }

    /** 7.0.2: how the ghoul stands with the ghoul NPC {@code npc} (its UUID). */
    public Bond bond(String npc) {
        return this.bonds.getOrDefault(npc, Bond.NONE);
    }

    /** Every bond remembered (the NPCs' UUIDs). */
    public Map<String, Bond> bonds() {
        return java.util.Collections.unmodifiableMap(this.bonds);
    }

    /** How many ghoul NPCs walk with this ghoul. */
    public int allies() {
        return (int)this.bonds.values().stream().filter(bond -> bond.has(NpcBonds.ALLY)).count();
    }

    public long nextHuntAt() {
        return this.nextHuntAt;
    }

    public void nextHuntAt(long time) {
        this.nextHuntAt = time;
    }

    public int migrated() {
        return this.migrated;
    }

    /** Every quest that counts for this ghoul now: the book's and the hunts on. */
    public List<Quest> quests() {
        List<Quest> all = new ArrayList<>(QuestBook.all());
        for (Hunt hunt : this.hunts) {
            all.add(hunt.quest());
        }
        for (Favour favour : this.favours) {
            all.add(favour.quest());
        }
        return all;
    }

    // ------------------------------------------------------------------ changing

    /**
     * {@code event} counted on every open quest's matching objectives; the steps it made, in order. A quest whose objectives
     * are all met is not finished here: the server pays it ({@link #complete}) so it can tell the player.
     */
    public List<Step> apply(QuestEvent event, KagunePath path, int mastery) {
        List<Step> steps = new ArrayList<>();
        for (Quest quest : this.quests()) {
            if (this.state(quest, path, mastery) != State.OPEN) {
                continue;
            }
            List<Objective> objectives = quest.objectives();
            for (int i = 0; i < objectives.size(); i++) {
                Objective objective = objectives.get(i);
                if (!objective.matches(event)) {
                    continue;
                }
                int before = this.progress(quest, i);
                int after = objective.advance(before, event);
                if (after != before) {
                    this.set(quest, i, after);
                    steps.add(new Step(quest, i, before, after));
                }
            }
        }
        return steps;
    }

    /** True once every objective of {@code quest} is met. */
    public boolean met(Quest quest) {
        List<Objective> objectives = quest.objectives();
        for (int i = 0; i < objectives.size(); i++) {
            if (this.progress(quest, i) < objectives.get(i).count()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ends {@code quest} as done: its gates open, its points and side are counted, the rival quests in its exclusive group close
     * for good (returned, so the player can be told), a hunt is taken off. What else it pays (mastery, Ghoul Meat) is the
     * server's to give.
     */
    public List<Quest> complete(Quest quest) {
        this.ended.put(quest.id(), State.DONE);
        this.gates.addAll(quest.reward().gates());
        this.points += Math.max(0, quest.reward().points());
        if (quest.reward().side() != null) {
            this.sides.merge(quest.reward().side(), 1, Integer::sum);
        }
        this.hunts.removeIf(hunt -> hunt.quest().id().equals(quest.id()));
        Favour favour = this.favour(quest);
        if (favour != null) {
            // Done for a ghoul: it thinks better of whoever did it, and remembers it asked.
            this.favours.remove(favour);
            Bond bond = this.bond(favour.giver());
            this.bond(favour.giver(), bond.regard(bond.regard() + NpcBonds.FAVOUR_DONE).favourDone());
        }
        if (com.hex.kagune.control.npc.EtoRules.lesson(quest.id()) != null) {
            // 1.3.0: a lesson of Eto's practised: she thinks the better of them for it.
            Bond eto = this.bond(com.hex.kagune.control.npc.EtoRules.KEY);
            this.bond(com.hex.kagune.control.npc.EtoRules.KEY, eto.regard(eto.regard() + com.hex.kagune.control.npc.EtoRules.LESSON_DONE));
        }
        List<Quest> closed = new ArrayList<>();
        if (quest.exclusive() != null) {
            for (Quest rival : QuestBook.all()) {
                if (rival != quest && quest.exclusive().equals(rival.exclusive()) && !this.ended.containsKey(rival.id())) {
                    this.ended.put(rival.id(), State.CLOSED);
                    closed.add(rival);
                }
            }
        }
        return closed;
    }

    /** Death: every objective that must be met in one life starts over. */
    public void died() {
        for (Quest quest : this.quests()) {
            int[] values = this.progress.get(quest.id());
            if (values == null || this.ended.containsKey(quest.id())) {
                continue;
            }
            for (int i = 0; i < values.length && i < quest.objectives().size(); i++) {
                if (quest.objectives().get(i).oneLife()) {
                    values[i] = 0;
                }
            }
        }
    }

    /** A hunt offered: on until {@code endsAt}. */
    public void startHunt(Hunts.Spec spec, long endsAt) {
        Quest quest = Hunts.quest(spec);
        this.hunts.removeIf(hunt -> hunt.quest().id().equals(quest.id()));
        this.progress.remove(quest.id());
        this.ended.remove(quest.id());
        this.hunts.add(new Hunt(spec, quest, endsAt));
    }

    /**
     * 7.0.2: a favour taken on for the ghoul {@code giver} ({@code identity}: who it is). False if one is already on for it or
     * {@link NpcBonds#MOST_FAVOURS} are.
     */
    public boolean startFavour(Favours.Spec spec, String giver, NpcIdentity identity) {
        if (this.favourFrom(giver) != null || this.favours.size() >= NpcBonds.MOST_FAVOURS) {
            return false;
        }
        int number = this.bond(giver).favours();
        Quest quest = Favours.quest(spec, giver, identity, number);
        this.progress.remove(quest.id());
        this.ended.remove(quest.id());
        this.favours.add(new Favour(spec, giver, identity.pack(), number, quest));
        return true;
    }

    /**
     * 7.0.2: how the ghoul stands with {@code npc} now (forgetting the faintest bond, never an ally's, past {@link NpcBonds#REMEMBERED});
     * 1.3.0: nor Eto's.
     */
    public void bond(String npc, Bond bond) {
        this.bonds.put(npc, bond);
        while (this.bonds.size() > NpcBonds.REMEMBERED) {
            String faintest = null;
            int least = Integer.MAX_VALUE;
            for (Map.Entry<String, Bond> entry : this.bonds.entrySet()) {
                int strength = Math.abs(entry.getValue().regard());
                if (!entry.getValue().has(NpcBonds.ALLY) && !entry.getKey().equals(npc) && !entry.getKey().equals(com.hex.kagune.control.npc.EtoRules.KEY)
                    && strength < least) {
                    least = strength;
                    faintest = entry.getKey();
                }
            }
            if (faintest == null) {
                break;
            }
            this.bonds.remove(faintest);
        }
    }

    /** The hunts whose time ran out at {@code now}, taken off (their progress forgotten). */
    public List<Quest> expire(long now) {
        List<Quest> gone = new ArrayList<>();
        this.hunts.removeIf(hunt -> {
            if (now < hunt.endsAt()) {
                return false;
            }
            gone.add(hunt.quest());
            this.progress.remove(hunt.quest().id());
            return true;
        });
        return gone;
    }

    /**
     * The first time a save is seen with quests (7.0.1): whatever the ghoul already had stays theirs. {@code had} says which
     * gates they reached under the old progression (the server: mastery past a transformation's, a mutation already taken).
     */
    public void migrate(Predicate<Gate> had) {
        if (this.migrated >= VERSION) {
            return;
        }
        for (Gate gate : Gate.values()) {
            if (had.test(gate)) {
                this.gates.add(gate);
            }
        }
        this.migrated = VERSION;
    }

    /** Opens {@code gate} outright (a command). */
    public void grant(Gate gate) {
        this.gates.add(gate);
    }

    /** Everything forgotten (a command; the save is migrated, so nothing is grandfathered back). */
    public void reset() {
        this.progress.clear();
        this.ended.clear();
        this.gates.clear();
        this.sides.clear();
        this.hunts.clear();
        this.favours.clear();
        this.bonds.clear();
        this.points = 0;
        this.nextHuntAt = 0L;
        this.migrated = VERSION;
    }

    private void set(Quest quest, int index, int value) {
        int[] values = this.progress.get(quest.id());
        if (values == null || values.length < quest.objectives().size()) {
            int[] grown = new int[quest.objectives().size()];
            if (values != null) {
                System.arraycopy(values, 0, grown, 0, Math.min(values.length, grown.length));
            }
            values = grown;
            this.progress.put(quest.id(), values);
        }
        values[index] = value;
    }

    // ------------------------------------------------------------------ the save: one string

    /**
     * "v=1;pts=2;hunt@=123;side.anteiku=1;gate=eyes_kick,rinkaku_twin;done=first_meal,...;closed=pack;p.light_feet=12,5;
     * h.0=kill_mob:3:5:120000:1". Unknown quests and gates in an old save are dropped quietly.
     */
    public String save() {
        StringBuilder out = new StringBuilder();
        out.append("v=").append(this.migrated);
        out.append(";pts=").append(this.points);
        out.append(";hunt@=").append(this.nextHuntAt);
        for (Map.Entry<String, Integer> side : this.sides.entrySet()) {
            out.append(";side.").append(side.getKey()).append('=').append(side.getValue());
        }
        out.append(";gate=").append(join(this.gates.stream().map(Gate::id).toList()));
        List<String> done = new ArrayList<>();
        List<String> closed = new ArrayList<>();
        for (Map.Entry<String, State> end : this.ended.entrySet()) {
            (end.getValue() == State.DONE ? done : closed).add(end.getKey());
        }
        out.append(";done=").append(join(done));
        out.append(";closed=").append(join(closed));
        for (Map.Entry<String, int[]> entry : this.progress.entrySet()) {
            if (this.ended.containsKey(entry.getKey())) {
                continue;
            }
            StringBuilder values = new StringBuilder();
            for (int i = 0; i < entry.getValue().length; i++) {
                values.append(i == 0 ? "" : ",").append(entry.getValue()[i]);
            }
            out.append(";p.").append(entry.getKey()).append('=').append(values);
        }
        for (int i = 0; i < this.hunts.size(); i++) {
            Hunt hunt = this.hunts.get(i);
            out.append(";h.").append(i).append('=').append(hunt.spec().save()).append(':').append(hunt.endsAt());
        }
        for (int i = 0; i < this.favours.size(); i++) {
            Favour favour = this.favours.get(i);
            out.append(";f.").append(i).append('=').append(favour.spec().save()).append(':').append(favour.number()).append(':')
                .append(favour.identity()).append(':').append(favour.giver());
        }
        for (Map.Entry<String, Bond> entry : this.bonds.entrySet()) {
            Bond bond = entry.getValue();
            out.append(";b.").append(entry.getKey()).append('=').append(bond.regard()).append(',').append(bond.chatted()).append(',')
                .append(bond.flags()).append(',').append(bond.favours());
        }
        return out.toString();
    }

    public static QuestLog load(String saved) {
        QuestLog log = new QuestLog();
        if (saved == null || saved.isEmpty()) {
            return log;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (String part : saved.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                fields.put(part.substring(0, eq), part.substring(eq + 1));
            }
        }
        log.migrated = parse(fields.get("v"), 0);
        log.points = Math.max(0, parse(fields.get("pts"), 0));
        log.nextHuntAt = parseLong(fields.get("hunt@"), 0L);
        // Hunts first: their progress is read below like any quest's.
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (!field.getKey().startsWith("h.")) {
                continue;
            }
            String value = field.getValue();
            int colon = value.lastIndexOf(':');
            Hunts.Spec spec = colon < 0 ? null : Hunts.Spec.load(value.substring(0, colon));
            if (spec != null) {
                log.hunts.add(new Hunt(spec, Hunts.quest(spec), parseLong(value.substring(colon + 1), 0L)));
            }
        }
        // Favours too: "t:p:c:pay:amount:number:identity:giver".
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (!field.getKey().startsWith("f.")) {
                continue;
            }
            String[] parts = field.getValue().split(":");
            if (parts.length != 8) {
                continue;
            }
            Favours.Spec spec = Favours.Spec.load(String.join(":", java.util.Arrays.copyOf(parts, 5)));
            if (spec != null && !parts[7].isEmpty()) {
                NpcIdentity identity = NpcIdentity.unpack(parseLong(parts[6], 0L));
                int number = Math.max(0, parse(parts[5], 0));
                log.favours.add(new Favour(spec, parts[7], identity.pack(), number, Favours.quest(spec, parts[7], identity, number)));
            }
        }
        for (Map.Entry<String, String> field : fields.entrySet()) {
            String key = field.getKey();
            String value = field.getValue();
            if (key.startsWith("b.")) {
                List<String> values = split(value);
                if (values.size() == 4) {
                    log.bonds.put(key.substring(2), new Bond(NpcBonds.clamp(parse(values.get(0), 0)), parseLong(values.get(1), Long.MIN_VALUE),
                        parse(values.get(2), 0), Math.max(0, parse(values.get(3), 0))));
                }
            } else if (key.startsWith("side.")) {
                log.sides.put(key.substring(5), Math.max(0, parse(value, 0)));
            } else if (key.equals("gate")) {
                for (String id : split(value)) {
                    Gate gate = Gate.fromId(id);
                    if (gate != null) {
                        log.gates.add(gate);
                    }
                }
            } else if (key.equals("done") || key.equals("closed")) {
                for (String id : split(value)) {
                    if (QuestBook.get(id) != null) {
                        log.ended.put(id, key.equals("done") ? State.DONE : State.CLOSED);
                    }
                }
            } else if (key.startsWith("p.")) {
                String id = key.substring(2);
                Quest quest = log.find(id);
                if (quest == null) {
                    continue;
                }
                List<String> values = split(value);
                int[] read = new int[quest.objectives().size()];
                for (int i = 0; i < read.length && i < values.size(); i++) {
                    read[i] = Math.max(0, Math.min(quest.objectives().get(i).count(), parse(values.get(i), 0)));
                }
                log.progress.put(id, read);
            }
        }
        return log;
    }

    private Quest find(String id) {
        Quest quest = QuestBook.get(id);
        if (quest != null) {
            return quest;
        }
        for (Hunt hunt : this.hunts) {
            if (hunt.quest().id().equals(id)) {
                return hunt.quest();
            }
        }
        for (Favour favour : this.favours) {
            if (favour.quest().id().equals(id)) {
                return favour.quest();
            }
        }
        return null;
    }

    private static String join(List<String> values) {
        return String.join(",", values);
    }

    private static List<String> split(String value) {
        List<String> out = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return out;
        }
        for (String part : value.split(",")) {
            if (!part.isEmpty()) {
                out.add(part);
            }
        }
        return out;
    }

    private static int parse(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException bad) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return value == null ? fallback : Long.parseLong(value.trim());
        } catch (NumberFormatException bad) {
            return fallback;
        }
    }
}
