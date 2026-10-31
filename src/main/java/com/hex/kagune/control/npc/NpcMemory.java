package com.hex.kagune.control.npc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.3.2: what one player and the one-of-a-kind NPCs remember of each other (pure, tested), past the regard and flags of their bond
 * ({@code QuestLog.Bond}): numbers by name, per NPC key ({@code "eto"}...), such as the day they began dating, the day they married,
 * when they were last together, how many kisses, how far along her trial a human is. Kept in the player's profile as one string
 * ({@link #save}, {@link #load}), so it goes with them through death. Any NPC made later keeps its own under its own key.
 */
public final class NpcMemory {
    /** The names every romanced NPC uses (each may add its own). */
    public static final String SINCE = "since";
    public static final String WED = "wed";
    public static final String SEEN = "seen";
    public static final String KISSES = "kisses";
    public static final String NIGHTS = "nights";
    /** 1.3.10: they asked each other for a baby (1): the next night it comes. */
    public static final String BABY = "baby";
    /** 1.3.10: how many children they have together. */
    public static final String CHILDREN = "children";
    public static final String NIGHT_DAY = "nightday";
    public static final String JEALOUS = "jealous";
    public static final String PUPPY_DAY = "pupday";
    public static final String TRIAL = "trial";
    public static final String TRIAL_COUNT = "tcount";
    public static final String TALKED_DAY = "tday";
    public static final String TOPICS = "topics";

    private final Map<String, Map<String, Long>> values = new LinkedHashMap<>();

    /** What is kept of {@code npc} under {@code name}, or {@code otherwise}. */
    public long get(String npc, String name, long otherwise) {
        Map<String, Long> of = this.values.get(npc);
        Long value = of == null ? null : of.get(name);
        return value == null ? otherwise : value;
    }

    public NpcMemory set(String npc, String name, long value) {
        if (!clean(npc) || !clean(name)) {
            return this;
        }
        this.values.computeIfAbsent(npc, ignored -> new LinkedHashMap<>()).put(name, value);
        return this;
    }

    public NpcMemory add(String npc, String name, long amount) {
        return this.set(npc, name, this.get(npc, name, 0L) + amount);
    }

    public NpcMemory forget(String npc, String name) {
        Map<String, Long> of = this.values.get(npc);
        if (of != null) {
            of.remove(name);
            if (of.isEmpty()) {
                this.values.remove(npc);
            }
        }
        return this;
    }

    /** Everything of {@code npc} forgotten (it was killed by them: whatever was between them is over). */
    public NpcMemory forgetAll(String npc) {
        this.values.remove(npc);
        return this;
    }

    public boolean knows(String npc, String name) {
        Map<String, Long> of = this.values.get(npc);
        return of != null && of.containsKey(name);
    }

    /** "eto{since=12,kisses=3};toka{...}": NPC keys and names are lower-case words. */
    public String save() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Map<String, Long>> npc : this.values.entrySet()) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append(npc.getKey()).append('{');
            boolean first = true;
            for (Map.Entry<String, Long> value : npc.getValue().entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(value.getKey()).append('=').append(value.getValue());
            }
            out.append('}');
        }
        return out.toString();
    }

    /** A memory read back ({@link #save}); whatever cannot be read is dropped quietly. */
    public static NpcMemory load(String saved) {
        NpcMemory memory = new NpcMemory();
        if (saved == null || saved.isEmpty()) {
            return memory;
        }
        for (String part : saved.split(";")) {
            int open = part.indexOf('{');
            if (open <= 0 || !part.endsWith("}")) {
                continue;
            }
            String npc = part.substring(0, open);
            String body = part.substring(open + 1, part.length() - 1);
            for (String pair : body.split(",")) {
                int equals = pair.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                try {
                    memory.set(npc, pair.substring(0, equals), Long.parseLong(pair.substring(equals + 1)));
                } catch (NumberFormatException ignored) {
                    // dropped
                }
            }
        }
        return memory;
    }

    private static boolean clean(String word) {
        if (word == null || word.isEmpty() || word.length() > 24) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) {
                return false;
            }
        }
        return true;
    }
}
