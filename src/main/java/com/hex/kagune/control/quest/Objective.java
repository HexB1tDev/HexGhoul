package com.hex.kagune.control.quest;

import java.util.Set;

/**
 * 7.0.1: one thing a quest asks for (pure): {@code count} of {@code signal}, counting only events carrying every one of
 * {@code all} and none of {@code none}. {@code best}: the objective keeps the highest amount seen (a rampage's length, the
 * mastery reached) instead of adding them up. {@code oneLife}: progress is lost when the player dies.
 */
public record Objective(String text, Signal signal, int count, Set<String> all, Set<String> none, boolean best, boolean oneLife) {
    public Objective {
        all = all == null ? Set.of() : Set.copyOf(all);
        none = none == null ? Set.of() : Set.copyOf(none);
        count = Math.max(1, count);
    }

    /** {@code count} of {@code signal} carrying every tag in {@code all}. */
    public static Objective of(String text, Signal signal, int count, String... all) {
        return new Objective(text, signal, count, Set.of(all), Set.of(), signal == Signal.RAMPAGE || signal == Signal.MASTERY, false);
    }

    /** As {@link #of}, never carrying any of {@code none}. */
    public Objective without(String... none) {
        return new Objective(this.text, this.signal, this.count, this.all, Set.of(none), this.best, this.oneLife);
    }

    /** As this, lost on death. */
    public Objective inOneLife() {
        return new Objective(this.text, this.signal, this.count, this.all, this.none, this.best, true);
    }

    public boolean matches(QuestEvent event) {
        if (event.signal() != this.signal || event.amount() <= 0) {
            return false;
        }
        for (String tag : this.all) {
            if (!event.has(tag)) {
                return false;
            }
        }
        for (String tag : this.none) {
            if (event.has(tag)) {
                return false;
            }
        }
        return true;
    }

    /** Progress {@code before} after {@code event} (which matches), never past {@link #count}. */
    public int advance(int before, QuestEvent event) {
        int after = this.best ? Math.max(before, event.amount()) : before + event.amount();
        return Math.min(this.count, Math.max(before, after));
    }
}
