package com.hex.kagune.control.family;

/**
 * 1.3.29: who a child is and how it feels (pure). Its temperament is its own for life: worked out from its id, so nothing new is saved,
 * leaning toward its companion parent's (Eto's children curious and mischievous, Touka's bold and serious, Ken's gentle and curious);
 * its mood is worked out fresh each time from what is going on (hurt, brushed off, a storm or the dark with something out there, a
 * parent back after a long while, a lesson today, bedtime, nobody about).
 */
public enum ChildTemper {
    /** Cheerful, loud, loves everyone. */
    SUNNY,
    /** Quiet, clingy, hides behind legs, braver than it looks. */
    SHY,
    /** Fearless, wants to fight everything, climbs everything. */
    BOLD,
    /** Questions, questions, questions; collects things; reads. */
    CURIOUS,
    /** Pranks, giggles, sticky hands, a born liar (badly). */
    MISCHIEF,
    /** Soft-hearted, worries about everyone, brings you flowers. */
    GENTLE,
    /** A little adult: frowns, keeps score, takes its lessons to heart. */
    SERIOUS;

    public String word() {
        return this.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** A child's mood now (its scenes' "mood:"). */
    public enum Mood {
        HAPPY, SLEEPY, SCARED, MISSED, BORED, PROUD, SAD, HURT;

        public String word() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** How strongly each companion's children lean toward theirs (out of their weights below). */
    static int[] weights(String mother, String father) {
        int[] w = {3, 3, 3, 3, 3, 3, 3};
        for (String parent : new String[] {mother, father}) {
            if (parent == null) {
                continue;
            }
            switch (parent) {
                case "npc:eto" -> {
                    w[CURIOUS.ordinal()] += 4;
                    w[MISCHIEF.ordinal()] += 4;
                    w[SERIOUS.ordinal()] += 1;
                }
                case "npc:touka" -> {
                    w[BOLD.ordinal()] += 4;
                    w[SERIOUS.ordinal()] += 3;
                    w[SHY.ordinal()] += 2;
                }
                case "npc:ken" -> {
                    w[GENTLE.ordinal()] += 4;
                    w[CURIOUS.ordinal()] += 3;
                    w[SHY.ordinal()] += 2;
                }
                default -> {
                }
            }
        }
        return w;
    }

    /** Its temperament: from its id (always the same), leaning toward a companion parent's ({@code mother}, {@code father}: their keys). */
    public static ChildTemper of(String id, String mother, String father) {
        int[] w = weights(mother, father);
        int total = 0;
        for (int v : w) {
            total += v;
        }
        long h = 1125899906842597L;
        for (int i = 0; i < (id == null ? 0 : id.length()); i++) {
            h = 31L * h + id.charAt(i);
        }
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        int at = (int)Math.floorMod(h, (long)total);
        for (ChildTemper t : values()) {
            at -= w[t.ordinal()];
            if (at < 0) {
                return t;
            }
        }
        return SUNNY;
    }

    /**
     * Its mood: {@code hurt} it is hurt; {@code sad} it was brushed off a little while ago; {@code storm} thunder; {@code danger}
     * something hostile near in the dark; {@code missed} a parent back after a long while; {@code taught} a lesson today; {@code night}
     * and {@code late} (deep night or dawn) the hour; {@code lonely} nobody about for long; {@code roll} in [0, 1) for its whims.
     */
    public static Mood mood(boolean hurt, boolean sad, boolean storm, boolean danger, boolean missed, boolean taught, boolean night, boolean late,
                            boolean lonely, double roll) {
        if (hurt) {
            return Mood.HURT;
        }
        if (sad) {
            return Mood.SAD;
        }
        if (storm || danger && night) {
            return Mood.SCARED;
        }
        if (missed) {
            return Mood.MISSED;
        }
        if (taught && roll < 0.6) {
            return Mood.PROUD;
        }
        if (late && roll < 0.7 || night && roll < 0.35) {
            return Mood.SLEEPY;
        }
        if (lonely && roll < 0.6) {
            return Mood.BORED;
        }
        return roll > 0.85 ? Mood.BORED : Mood.HAPPY;
    }
}
