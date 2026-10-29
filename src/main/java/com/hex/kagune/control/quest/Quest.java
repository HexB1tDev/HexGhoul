package com.hex.kagune.control.quest;

import com.hex.kagune.control.KagunePath;
import java.util.List;

/**
 * 7.0.1: one quest (pure). {@code path} null: any ghoul's. It opens once mastery reaches {@code mastery} and every quest in
 * {@code after} is done; from then on its {@link Objective}s count, and when all are met it is done and pays {@link Reward}
 * (and opens its gates). Quests sharing an {@code exclusive} group are rival ways: the first done closes the others for good.
 * {@code line}: where the journal lists it.
 */
public record Quest(String id, Line line, KagunePath path, String title, String story, int mastery, List<String> after, String exclusive,
                    List<Objective> objectives, Reward reward) {
    /** Where a quest comes from. */
    public enum Line {
        /** The path's own progression (its gates). */
        PATH,
        /** Any ghoul's: the eye-mode fight and the ghoul's life. */
        GHOUL,
        /** 7.0.1: a hunt drawn at dusk, gone at dawn ({@link Hunts}). */
        HUNT,
        /** 7.0.1 (NPCs): asked by a ghoul, taken in conversation. */
        FAVOUR
    }

    public Quest {
        after = after == null ? List.of() : List.copyOf(after);
        objectives = List.copyOf(objectives);
        reward = reward == null ? Reward.NONE : reward;
    }

    /** True if a player on {@code path} can ever have it. */
    public boolean on(KagunePath path) {
        return this.path == null || this.path == path;
    }

    /** True if it opens {@code gate}. */
    public boolean opens(Gate gate) {
        return this.reward.gates().contains(gate);
    }
}
