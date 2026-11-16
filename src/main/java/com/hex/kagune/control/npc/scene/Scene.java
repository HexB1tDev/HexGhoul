package com.hex.kagune.control.npc.scene;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.3.29: one scene of a speaker's script (pure): something it comes up to the player with ({@link Kind#APPROACH}: it walks up, a prompt
 * pops up, and the player answers), or something the player can bring up ({@link Kind#TOPIC}: on its talk page). Who it is for
 * ({@link #who}), the moods it fits ({@link #moods}; a child's temperaments, {@link #tempers}), the moments it needs ({@link #when}),
 * its pages ({@link #first} and any it leads on to), and (an approach) what it says if left unanswered ({@link #ignored}).
 */
public record Scene(String speaker, String id, Kind kind, String prompt, Set<String> who, Set<String> moods, Set<String> tempers, List<String> when,
                    int weight, Page first, Map<String, Page> pages, List<String> ignored) {
    public enum Kind {
        APPROACH, TOPIC,
        /**
         * 2.1.2: a courtship chapter's moment ({@code moment <chapter id>}), played when its task is done and never picked otherwise; and
         * a date ({@code date <id>}), offered to a partner as one of the places to go together (its {@link #prompt} the offer).
         */
        MOMENT, DATE
    }

    /** A page: what it says (one of these, picked), and the answers. */
    public record Page(String id, List<String> lines, List<Option> options) {
    }

    /**
     * An answer: its words, what it does to the speaker's regard, what happens ({@link SceneWords}: a gesture, a deed, a gift), the page it
     * leads on to ({@code next}, or null), and what the speaker says to it (one of these, picked; none: the next page's own words).
     */
    public record Option(String label, int regard, List<String> effects, String next, List<String> replies) {
        public boolean does(String effect) {
            return this.effects.contains(effect);
        }

        /** The item it gives the player ({@code gift:<item>}), or null. */
        public String gift() {
            for (String effect : this.effects) {
                if (effect.startsWith("gift:")) {
                    return effect.substring(5);
                }
            }
            return null;
        }
    }

    /** Its key, unique across every script: "eto.rain_window". */
    public String key() {
        return this.speaker + "." + this.id;
    }

    /** The page {@code id} ({@code ""} or null: the first). */
    public Page page(String id) {
        if (id == null || id.isEmpty() || id.equals(this.first.id())) {
            return this.first;
        }
        return this.pages.get(id);
    }

    /** True if it fits {@code c}: who the player is to the speaker, its mood, its temperament, every moment it needs. */
    public boolean fits(SceneContext c) {
        if (!this.who.isEmpty() && !this.who.contains(c.tier())) {
            return false;
        }
        if (!this.moods.isEmpty() && !this.moods.contains(c.mood())) {
            return false;
        }
        if (!this.tempers.isEmpty() && !this.tempers.contains(c.temper())) {
            return false;
        }
        for (String flag : this.when) {
            boolean not = flag.startsWith("!");
            if (c.has(not ? flag.substring(1) : flag) == not) {
                return false;
            }
        }
        return true;
    }

    /** How many moments it needs (not counting what it needs not to be): the more particular, the likelier it comes up when it can. */
    public int particular() {
        int n = 0;
        for (String flag : this.when) {
            if (!flag.startsWith("!")) {
                n++;
            }
        }
        return n;
    }
}
