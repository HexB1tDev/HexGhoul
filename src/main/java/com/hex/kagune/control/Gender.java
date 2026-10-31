package com.hex.kagune.control;

/**
 * 1.3.9: a player's (or an NPC's) gender, picked once they are a ghoul (after their eyes, the gender screen), kept in their profile root
 * ({@link #KEY}). Female bodies are drawn and boxed as {@link FemaleBody}; unpicked is drawn as it always was. Sent by ordinal: new
 * values go at the end.
 */
public enum Gender {
    UNSET,
    MALE,
    FEMALE;

    /** Where a player's gender is kept (their profile root). */
    public static final String KEY = "HexGender";

    public static Gender byOrdinal(int ordinal) {
        Gender[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : UNSET;
    }

    public boolean female() {
        return this == FEMALE;
    }

    /** What a player may pick on the screen (unpicked is not a choice). */
    public boolean choice() {
        return this != UNSET;
    }
}
