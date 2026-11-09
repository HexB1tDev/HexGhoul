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
    GRAFT_NOT_THAT_EASILY("graft_not_that_easily", KagunePath.GRAFT, "You Thought You'd Get Rid of Me That Easily? (mutation)", 0, true),
    /** 1.3.14: Crystal mode (V crystallizes the wings): Touka teaches it ({@code Teaching}); kept for saves already past its mastery. */
    UKAKU_CRYSTAL("ukaku_crystal", KagunePath.UKAKU, "Crystal Wings (crystal mode)", com.hex.kagune.control.ukaku.UkakuRules.CRYSTAL_MASTERY, false),
    /** 3.3.1: Rinkaku's Rubble Hurl (N held, nothing held): Ken teaches it, after a proof ({@code Teaching}). Never grandfathered. */
    RINKAKU_HURL("rinkaku_hurl", KagunePath.RINKAKU, "Rubble Hurl", com.hex.kagune.control.RinkakuUnlocks.HURL, false),
    /** 3.3.1: Rinkaku's Impale (H held, nothing held): Ken teaches it, after the Hurl and a proof. Never grandfathered. */
    RINKAKU_IMPALE("rinkaku_impale", KagunePath.RINKAKU, "Impale", com.hex.kagune.control.RinkakuUnlocks.IMPALE, false),
    /**
     * 4.0.0 (the owner: Ayato "the main NPC responsible for unlocking the Ukaku Kakuja progression ... players must earn his respect, complete
     * meaningful challenges"): his respect, earned in two steps before he teaches the first kakuja ({@code Teaching}). No move of their
     * own: the road to the kakuja. Never grandfathered.
     */
    UKAKU_AYATO_DOVES("ukaku_ayato_doves", KagunePath.UKAKU, "Ayato's Respect: Doves", 600, false),
    UKAKU_AYATO_HUNT("ukaku_ayato_hunt", KagunePath.UKAKU, "Ayato's Respect: the Hunt", 680, false),
    /**
     * 4.0.0 (the owner: Noro "the primary progression NPC for the new Bikaku path, teaching players the abilities associated with its normal
     * form, Half-Kakuja, Full-Kakuja, and unlockable mutations"): the worms' forms and their later moves (Twin and Three Worms, Ravenous
     * Pursuit, Carrion Eruption, Monstrous Regeneration), The Walker (the half kakuja), the kakuja, and the two mutations. A save already past
     * a move's mastery keeps it (it had it); the mutations' only if it has them already.
     */
    BIKAKU_WORMS("bikaku_worms", KagunePath.BIKAKU, "Bikaku: the Worms (forms, Pursuit, Eruption, Regeneration)", 250, false),
    BIKAKU_WALKER("bikaku_walker", KagunePath.BIKAKU, "Bikaku: The Walker (half kakuja)", 750, false),
    BIKAKU_KAKUJA("bikaku_kakuja", KagunePath.BIKAKU, "Bikaku: the Kakuja", 1000, false),
    BIKAKU_DEVOURING("bikaku_devouring", KagunePath.BIKAKU, "Grand Devouring (mutation)", 0, true),
    BIKAKU_POTENTIAL("bikaku_potential", KagunePath.BIKAKU, "Potential Unlock (mutation)", 0, true);

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
