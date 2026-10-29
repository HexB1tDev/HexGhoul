package com.hex.kagune.control.king;

/**
 * 1.0.5: the final kakujas (pure), the King's two answers once the Dragon is spent: War, the One-Eyed King, and Peace, Kaneki :Re. A
 * final kakuja is its own form, never one of Kaneki's kakujas ({@code KanekiForms}, his half kakujas since 1.0.10): its own section
 * in the menu, and K held puts it on (1.0.10; a tap is the strongest half kakuja). A ghoul chooses one, once, on the two cards.
 * Peace is shown and can't be chosen until it is built.
 */
public enum KingForm {
    NONE("none", "None", false),
    WAR("war", "War", true),
    PEACE("peace", "Peace", true);

    private final String id;
    private final String displayName;
    private final boolean built;

    KingForm(String id, String displayName, boolean built) {
        this.id = id;
        this.displayName = displayName;
        this.built = built;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    /** What the menu calls it. */
    public String title() {
        return this == NONE ? "None" : this.displayName + " (Final Kakuja)";
    }

    /** True when it can be chosen and worn: War from 1.0.5, Peace not yet. */
    public boolean built() {
        return this.built;
    }

    public static KingForm byId(String id) {
        for (KingForm form : values()) {
            if (form.id.equals(id)) {
                return form;
            }
        }
        return NONE;
    }

    public static KingForm byOrdinal(int ordinal) {
        KingForm[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }
}
