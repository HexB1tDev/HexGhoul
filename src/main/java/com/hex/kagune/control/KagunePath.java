package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Which canon ghoul a player's kagune follows. Chosen once, on the starting screen, after the kagune is
 * unlocked. Stored by id in the profile NBT, so the enum order is free to change.
 *
 * <p>This is the list of kagunes. Adding one means: a constant here (its id is saved), its card
 * on the starting screen ({@code client/PathCards}), its menu ({@code client/PathMenus}) and its own server
 * behaviour. Everything shared (the starting screen, the kakugan eyes, the path commands and sync) picks it up.
 */
public enum KagunePath {
    /** Unlocked (or not yet unlocked) and not chosen yet: the starting screen is still owed. */
    NONE("none", "Undecided", 1.0F),
    /** Rinkaku → Centipede → Kakuja: the original HexKagune progression, unchanged. */
    RINKAKU("rinkaku", "Rinkaku", 0.85F),
    /** Ukaku wings, "Black Rabbit". */
    UKAKU("ukaku", "Ukaku", 1.3F),
    /**
     * 6.1.0, Owl DNA: a natural one-eyed ghoul (only one eye ever turns), absurdly fast and playful, whose kagune is a mass of pale
     * living flesh from the upper back; its kakuja is the Owl. 7.0.0: dormant, and 1.2.5: never pickable (the user's ask): no player is
     * ever on it, but its code stays, and what other paths use of it stays theirs (the eye-mode fight every ghoul has with only the
     * kakugan out, the death that does not take).
     */
    PALE("owl", "Owl DNA", 1.12F),
    /**
     * 7.0.0, Graft DNA: an artificial one-eyed ghoul body built round a transplanted kakuhou, a rinkaku that grows from one or two
     * muscular tails to toothed centipedes, four thorned giants, a multi-eyed mass and a kakuja of two colossal centipedes over
     * living armor (see {@code control/graft}). 1.2.5: dormant, never pickable (the user's ask): its code stays, and what others use
     * of it stays theirs (the wild ghouls' tails and lashes, the revive passive the other paths took from it).
     */
    GRAFT("graft", "Graft DNA", 0.78F);

    private final String id;
    private final String displayName;
    private final float awakenPitch;

    KagunePath(String id, String displayName, float awakenPitch) {
        this.id = id;
        this.displayName = displayName;
        this.awakenPitch = awakenPitch;
    }

    /** Every path a player can choose, in the order the starting screen shows them (never a dormant one). */
    public static List<KagunePath> choosable() {
        List<KagunePath> paths = new ArrayList<>();
        for (KagunePath path : values()) {
            if (path.chosen() && !path.dormant()) {
                paths.add(path);
            }
        }
        return paths;
    }

    /** 7.0.0: a path kept in the code that no player can be on (Owl DNA; 1.2.5: and Graft DNA). */
    public boolean dormant() {
        return this == PALE || this == GRAFT;
    }

    /** The path a player is really on when {@code this} is read: none for a dormant one (they choose again), else itself. */
    public KagunePath active() {
        return this.dormant() ? NONE : this;
    }

    /** Pitch of the tearing sound when a player awakens on this path. */
    public float awakenPitch() {
        return this.awakenPitch;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public boolean chosen() {
        return this != NONE;
    }

    /** True while the kagune is unlocked but has no path: manifesting is blocked and the starting screen is owed. */
    public static boolean choiceOwed(boolean unlocked, KagunePath path) {
        return unlocked && !path.chosen();
    }

    /** Unknown or missing ids (a corrupted tag, a path from a newer version) read as {@link #NONE}, so the player chooses again. */
    public static KagunePath fromId(String id) {
        if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);
            for (KagunePath path : values()) {
                if (path.id.equals(key)) {
                    return path.active();
                }
            }
        }
        return NONE;
    }

    /** Safe decode of a network ordinal (a dormant path's decodes as none). */
    public static KagunePath byOrdinal(int ordinal) {
        KagunePath[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal].active() : NONE;
    }

    /**
     * The path for a profile that has never stored one (first load under 1.2.0, or after {@code /kagune reset}).
     * Anyone who already grew their kagune keeps the original progression without being asked; an untouched
     * profile gets the starting screen.
     */
    public static KagunePath forUnmarkedProfile(boolean unlocked, int mastery, long mutationMask, boolean startingForm) {
        boolean progressed = mastery > 0 || mutationMask != 0L || !startingForm;
        return unlocked && progressed ? RINKAKU : NONE;
    }
}
