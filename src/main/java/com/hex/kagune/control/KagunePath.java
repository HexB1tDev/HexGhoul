package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Which canon ghoul a player's kagune follows. Chosen once, on the starting screen, after the kagune is
 * unlocked. Stored by id in the profile NBT, so the enum order is free to change.
 *
 * <p>This is the list of kagunes. Adding one means: a constant here (its id is saved: a renamed id keeps reading its old one, see
 * {@link #fromId}), its card
 * on the starting screen ({@code client/PathCards}), its menu ({@code client/PathMenus}) and its own server
 * behaviour. Everything shared (the starting screen, the kakugan eyes, the path commands and sync) picks it up.
 */
public enum KagunePath {
    /** Unlocked (or not yet unlocked) and not chosen yet: the starting screen is still owed. */
    NONE("none", "Undecided", 1.0F),
    /** Rinkaku → Centipede → Kakuja: the original HexKagune progression, unchanged. */
    KANEKI("kaneki", "Kaneki DNA", 0.85F),
    /** Ukaku wings, "Black Rabbit" (see docs/AYATO_PATH.md). */
    AYATO("ayato", "Ayato DNA", 1.3F),
    /**
     * 6.1.0, Owl DNA (6.2.0: renamed from its first name, its id "eto" to "owl"): the One-Eyed Owl strain, a natural one-eyed ghoul
     * (only one eye ever turns), absurdly fast and playful, whose kagune is a mass of pale living flesh from the upper back; its
     * kakuja is the Owl. (The constant keeps its first name: only the code sees it.)
     *
     * <p>7.0.0: dormant. Furuta DNA took its place; every line of it (moves, the Owl, its models, mutations, menu) is kept for a
     * later update, but no player can be on it: its ids read as {@link #FURUTA} ({@link #RENAMED}), it is never choosable, and the
     * server turns a request for it into Furuta DNA ({@link #active}). Its save-data keys are left untouched.
     */
    ETO("owl", "Owl DNA", 1.12F),
    /**
     * 7.0.0, Furuta DNA: Kichimura Washuu's artificial one-eyed ghoul body built round Rize's transplanted kakuhou. A rinkaku
     * that grows from one or two muscular tails to toothed centipedes, four thorned giants, a multi-eyed mass and a kakuja of
     * two colossal centipedes over living armor; regeneration is its heart (see {@code control/furuta}). It replaces Owl DNA.
     */
    FURUTA("furuta", "Furuta DNA", 0.78F);

    /**
     * Ids a path was once saved under, still read as it (6.2.0: the Owl DNA path was "eto"). 7.0.0: Owl DNA is dormant, so both of
     * its ids now read as Furuta DNA, the path that replaced it: an old save, a cached selection or a command naming it lands there.
     */
    private static final java.util.Map<String, String> RENAMED = java.util.Map.of("eto", "furuta", "owl", "furuta");

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

    /** 7.0.0: a path kept in the code for a later update that no player can be on (Owl DNA). */
    public boolean dormant() {
        return this == ETO;
    }

    /** The path a player is really put on when {@code this} is asked for: a dormant path's replacement, else itself. */
    public KagunePath active() {
        return this == ETO ? FURUTA : this;
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
            key = RENAMED.getOrDefault(key, key);
            for (KagunePath path : values()) {
                if (path.id.equals(key)) {
                    return path.active();
                }
            }
        }
        return NONE;
    }

    /** Safe decode of a network ordinal (a dormant path's decodes as its replacement). */
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
        return unlocked && progressed ? KANEKI : NONE;
    }
}
