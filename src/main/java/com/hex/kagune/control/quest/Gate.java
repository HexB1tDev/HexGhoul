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
    // Rinkaku.
    RINKAKU_TWIN("rinkaku_twin", KagunePath.RINKAKU, "Twin Rinkaku", 250, false),
    RINKAKU_FOUR("rinkaku_four", KagunePath.RINKAKU, "Four-Tail Rinkaku", 500, false),
    RINKAKU_CENTIPEDE("rinkaku_centipede", KagunePath.RINKAKU, "Centipede Form (first kakuja)", 750, false),
    RINKAKU_FULL_KAKUJA("rinkaku_full_kakuja", KagunePath.RINKAKU, "Full Kakuja", 1000, false),
    /** 1.0.4: Kakuja V2, the Dragon: open, the next death wakes it instead (control/awakening). Never grandfathered. */
    RINKAKU_DRAGON("rinkaku_dragon", KagunePath.RINKAKU, "Kakuja V2: the Dragon", 1000, false),
    /**
     * 1.0.41: You Thought You'd Get Rid of Me That Easily?, the Rinkaku path's one passive (no mutations of its own any more): its quest
     * is all it takes. A mutation's gate as far as saves go (never opened for an old save's mastery).
     */
    RINKAKU_NOT_THAT_EASILY("rinkaku_not_that_easily", KagunePath.RINKAKU, "You Thought You'd Get Rid of Me That Easily? (passive)", 0, true),
    // Ukaku.
    UKAKU_GATLING("ukaku_gatling", KagunePath.UKAKU, "Gatling", 400, false),
    UKAKU_KAKUJA("ukaku_kakuja", KagunePath.UKAKU, "First Kakuja", 750, false),
    UKAKU_FINAL_KAKUJA("ukaku_final_kakuja", KagunePath.UKAKU, "Final Kakuja", 1000, false),
    UKAKU_BURSTING_SHARDS("ukaku_bursting_shards", KagunePath.UKAKU, "Bursting Shards (mutation)", 0, true),
    /** 1.0.41: Graft's revive as Ukaku's fifth mutation, behind the same quest. */
    UKAKU_NOT_THAT_EASILY("ukaku_not_that_easily", KagunePath.UKAKU, "You Thought You'd Get Rid of Me That Easily? (mutation)", 0, true),
    // Graft DNA.
    GRAFT_CENTIPEDE("graft_centipede", KagunePath.GRAFT, "Centipede Rinkaku", 250, false),
    GRAFT_THORNS("graft_thorns", KagunePath.GRAFT, "Four Thorns", 450, false),
    GRAFT_DRAGON("graft_dragon", KagunePath.GRAFT, "Many-Eyed Kagune", 700, false),
    GRAFT_KAKUJA("graft_kakuja", KagunePath.GRAFT, "Kakuja", 900, false),
    GRAFT_APPETITE("graft_appetite", KagunePath.GRAFT, "Rize's Appetite (mutation)", 0, true),
    GRAFT_NOT_THAT_EASILY("graft_not_that_easily", KagunePath.GRAFT, "You Thought You'd Get Rid of Me That Easily? (mutation)", 0, true);

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
