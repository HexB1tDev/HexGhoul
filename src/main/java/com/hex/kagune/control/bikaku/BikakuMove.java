package com.hex.kagune.control.bikaku;

/**
 * 3.5.0: the Bikaku path's moves (pure): which key each is on (the original's kagune keys), the mastery it unlocks at (the owner, after
 * 3.4.9: "These abilities are not going to be taught by anyone currently in the mod ... It's going to be a custom NPC": mastery alone
 * opens them, no lesson or quest), how long it lasts, when it bites, its cooldown and its RC. Their motion is {@link MawMotion}'s.
 */
public enum BikakuMove {
    NONE("", Slot.NONE, 0, 0, 0, 0, 0.0F),
    /**
     * Devouring Maw (the owner's spec): thrown far out at what he aims at, its mouth opened extremely wide, snapped shut round it; the
     * teeth clamp on, it is pulled up before him and violently shaken, then flung away to the side. Bleed, a brief stun, a little healing.
     */
    MAW("Devouring Maw", Slot.G, BikakuRules.MAW, 42, 9, 160, 3.0F),
    /**
     * Predatory Drag: along the ground to what he aims at, mouth open; it bites on and drags it back across the ground to him, lifts it
     * high and slams it down before letting go. A grab and a slam: stun, bleed.
     */
    DRAG("Predatory Drag", Slot.J, BikakuRules.DRAG, 46, 11, 200, 4.0F);

    /** The original's kagune keys, as the move keys of this path. */
    public enum Slot {
        NONE, Z, J, V, B, N, H, G, C
    }

    private final String displayName;
    private final Slot slot;
    private final int mastery;
    private final int ticks;
    private final int land;
    private final int cooldown;
    private final float cost;

    BikakuMove(String displayName, Slot slot, int mastery, int ticks, int land, int cooldown, float cost) {
        this.displayName = displayName;
        this.slot = slot;
        this.mastery = mastery;
        this.ticks = ticks;
        this.land = land;
        this.cooldown = cooldown;
        this.cost = cost;
    }

    public String displayName() {
        return this.displayName;
    }

    public Slot slot() {
        return this.slot;
    }

    /** The mastery it unlocks at. */
    public int mastery() {
        return this.mastery;
    }

    /** How long it lasts when it catches (ticks). */
    public int ticks() {
        return this.ticks;
    }

    /** The tick its mouth first shuts. */
    public int land() {
        return this.land;
    }

    /** Ticks before it can be used again (from when it began). */
    public int cooldown() {
        return this.cooldown;
    }

    public float cost() {
        return this.cost;
    }

    /** How long it lasts when its bite misses (it draws back from there). */
    public int missedTicks() {
        return this.land + 15;
    }

    /** True if {@code mastery} has opened it. */
    public boolean unlocked(int mastery) {
        return this != NONE && mastery >= this.mastery;
    }

    /** The move on {@code slot} (none if nothing is on it). */
    public static BikakuMove of(Slot slot) {
        for (BikakuMove move : values()) {
            if (move != NONE && move.slot == slot) {
                return move;
            }
        }
        return NONE;
    }

    public static BikakuMove byOrdinal(int ordinal) {
        BikakuMove[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    // ---------------------------------------------------------------- Devouring Maw's beats

    /** Drawn back, mouth opening wide, until this tick; the lunge after it shuts at {@link #land()}. */
    public static final int MAW_WINDUP = 5;
    /** Pulled up before him by this tick; shaken until {@link #MAW_SHAKE_END}; flung over the next ticks, let go at {@link #MAW_RELEASE}. */
    public static final int MAW_PULLED = 14;
    public static final int MAW_SHAKE_END = 27;
    public static final int MAW_RELEASE = 30;

    // ---------------------------------------------------------------- Predatory Drag's beats

    /** Down to the ground before him by this tick; along the ground until it bites at {@link #land()}. */
    public static final int DRAG_DOWN = 4;
    /** Dragged back across the ground to him by this tick; lifted high by {@link #DRAG_LIFTED}; slammed down at {@link #DRAG_SLAM}. */
    public static final int DRAG_HOME = 24;
    public static final int DRAG_LIFTED = 31;
    public static final int DRAG_SLAM = 34;
}
