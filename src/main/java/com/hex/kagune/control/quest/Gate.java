package com.hex.kagune.control.quest;

import com.hex.kagune.control.KagunePath;

/**
 * 7.0.1: an unlock that mastery alone no longer gives (pure): a transformation, a move or a mutation that also needs one of the
 * quests that open it ({@link QuestBook#openers}). The mastery it always needed still counts; the gate is on top of it. Saved by
 * id: a renamed gate keeps its id.
 */
public enum Gate {
    // The shared eye-mode fight (any path).
    EYES_KICK("eyes_kick", null, "Speed Kick", 250, false),
    EYES_SOAR("eyes_soar", null, "Up We Go (soaring flips)", 400, false),
    EYES_WITNESS("eyes_witness", null, "Witness This (the full Big Punch's combo)", 600, false),
    // Kaneki DNA.
    KANEKI_TWIN("kaneki_twin", KagunePath.KANEKI, "Twin Rinkaku", 250, false),
    KANEKI_FOUR("kaneki_four", KagunePath.KANEKI, "Four-Tail Rinkaku", 500, false),
    KANEKI_CENTIPEDE("kaneki_centipede", KagunePath.KANEKI, "Centipede Form (first kakuja)", 750, false),
    KANEKI_FULL_KAKUJA("kaneki_full_kakuja", KagunePath.KANEKI, "Full Kakuja", 1000, false),
    /** 1.0.4: Kakuja V2, the Dragon: open, the next death wakes it instead (control/awakening). Never grandfathered. */
    KANEKI_DRAGON("kaneki_dragon", KagunePath.KANEKI, "Kakuja V2: the Dragon", 1000, false),
    /**
     * 1.0.41: You Thought You'd Get Rid of Me That Easily?, the Kaneki path's one passive (no mutations of its own any more): its quest
     * is all it takes. A mutation's gate as far as saves go (never opened for an old save's mastery).
     */
    KANEKI_NOT_THAT_EASILY("kaneki_not_that_easily", KagunePath.KANEKI, "You Thought You'd Get Rid of Me That Easily? (passive)", 0, true),
    // Ayato DNA.
    AYATO_GATLING("ayato_gatling", KagunePath.AYATO, "Gatling", 400, false),
    AYATO_KAKUJA("ayato_kakuja", KagunePath.AYATO, "First Kakuja", 750, false),
    AYATO_FINAL_KAKUJA("ayato_final_kakuja", KagunePath.AYATO, "Final Kakuja", 1000, false),
    AYATO_BURSTING_SHARDS("ayato_bursting_shards", KagunePath.AYATO, "Bursting Shards (mutation)", 0, true),
    /** 1.0.41: Furuta's revive as Ayato's fifth mutation, behind the same quest. */
    AYATO_NOT_THAT_EASILY("ayato_not_that_easily", KagunePath.AYATO, "You Thought You'd Get Rid of Me That Easily? (mutation)", 0, true),
    // Furuta DNA.
    FURUTA_CENTIPEDE("furuta_centipede", KagunePath.FURUTA, "Centipede Rinkaku", 250, false),
    FURUTA_THORNS("furuta_thorns", KagunePath.FURUTA, "Four Thorns", 450, false),
    FURUTA_DRAGON("furuta_dragon", KagunePath.FURUTA, "Many-Eyed Kagune", 700, false),
    FURUTA_KAKUJA("furuta_kakuja", KagunePath.FURUTA, "Kakuja", 900, false),
    FURUTA_APPETITE("furuta_appetite", KagunePath.FURUTA, "Rize's Appetite (mutation)", 0, true),
    FURUTA_NOT_THAT_EASILY("furuta_not_that_easily", KagunePath.FURUTA, "You Thought You'd Get Rid of Me That Easily? (mutation)", 0, true);

    private final String id;
    private final KagunePath path;
    private final String displayName;
    private final int mastery;
    private final boolean mutation;

    Gate(String id, KagunePath path, String displayName, int mastery, boolean mutation) {
        this.id = id;
        this.path = path;
        this.displayName = displayName;
        this.mastery = mastery;
        this.mutation = mutation;
    }

    /** A mutation's gate: the mutation's own mastery and points still apply; an old save keeps it if the mutation was taken. */
    public boolean mutation() {
        return this.mutation;
    }

    public String id() {
        return this.id;
    }

    /** The path it belongs to; null for the shared eye-mode fight. */
    public KagunePath path() {
        return this.path;
    }

    public String displayName() {
        return this.displayName;
    }

    /** The mastery it needed before quests (still needed; 0 for a mutation's); grandfathered saves past it keep it open. */
    public int mastery() {
        return this.mastery;
    }

    /** True for a gate a player on {@code path} meets. */
    public boolean on(KagunePath path) {
        return this.path == null || this.path == path;
    }

    public static Gate fromId(String id) {
        for (Gate gate : values()) {
            if (gate.id.equals(id)) {
                return gate;
            }
        }
        return null;
    }
}
