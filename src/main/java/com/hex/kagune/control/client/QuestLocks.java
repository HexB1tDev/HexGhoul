package com.hex.kagune.control.client;

import com.hex.kagune.control.quest.Gate;
import java.util.function.Predicate;

/**
 * 7.0.1: what a kagune menu needs to know of the quests: which gates are open and how many mutation points they paid. {@link #ALL}
 * (every gate open, no points) is what a menu drawn without quests (a preview) shows.
 */
public record QuestLocks(Predicate<Gate> open, int points) {
    public static final QuestLocks ALL = new QuestLocks(gate -> true, 0);

    /** The player at the keys' ({@link QuestClient}). */
    public static QuestLocks live() {
        return new QuestLocks(QuestClient::open, QuestClient.points());
    }

    public boolean passes(Gate gate) {
        return gate == null || this.open.test(gate);
    }
}
