package com.hex.kagune.control.npc;

/**
 * 1.3.2: who the player is to Eto, which decides how she talks to them (pure): someone else's partner gets the cold, claimed voice;
 * her own partner the warm one, warmer once married; anyone else by how she regards them. Every line she says is picked by this
 * first ({@link #pick}), then by her mood and the hour.
 */
public enum EtoVoice {
    /** She belongs to another player: lessons, if they must. Nothing more. */
    CLAIMED,
    /** A stranger, or someone she has little time for. */
    WARY,
    /** She likes them. */
    FRIENDLY,
    /** A friend, close to her. */
    CLOSE,
    /** Dating. */
    PARTNER,
    /** Married. */
    WIFE;

    public static EtoVoice of(EtoTalk.Context c) {
        if (c.claimed()) {
            return CLAIMED;
        }
        if (c.married()) {
            return WIFE;
        }
        if (c.dating()) {
            return PARTNER;
        }
        if (c.regard() >= NpcBonds.FRIEND) {
            return CLOSE;
        }
        return c.regard() >= NpcBonds.FRIENDLY ? FRIENDLY : WARY;
    }

    public boolean together() {
        return this == PARTNER || this == WIFE;
    }

    /**
     * One of the lines for this voice, picked by {@code seed}: {@code lines} in voice order (claimed, wary, friendly, close, partner,
     * wife), a null falling back to the one before it (wife to partner, close to friendly...; claimed to wary).
     */
    public String pick(int seed, String[]... lines) {
        int at = this.ordinal();
        while (at > 0 && (at >= lines.length || lines[at] == null || lines[at].length == 0)) {
            at = at == 1 ? 0 : at - 1;
        }
        if (at >= lines.length || lines[at] == null || lines[at].length == 0) {
            at = 1;
        }
        String[] from = lines[at];
        return from[Math.floorMod(seed, from.length)];
    }
}
