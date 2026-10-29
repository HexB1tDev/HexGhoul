package com.hex.kagune.control.quest;

import java.util.Set;

/** 7.0.1: something that happened to a player, for the quest log (pure): {@code amount} of {@code signal}, tagged ({@link Tags}). */
public record QuestEvent(Signal signal, int amount, Set<String> tags) {
    public QuestEvent {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public static QuestEvent of(Signal signal, int amount, String... tags) {
        return new QuestEvent(signal, amount, Set.of(tags));
    }

    public boolean has(String tag) {
        return this.tags.contains(tag);
    }
}
