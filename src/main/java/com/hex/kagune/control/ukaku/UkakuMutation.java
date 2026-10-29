package com.hex.kagune.control.ukaku;

/**
 * Ukaku's mutations (pure, tested): five (1.0.41: the fifth is Graft's revive), all of which he can have. Each mastery milestone
 * ({@link #MILESTONES}) earns one mutation point, spent in the Ukaku menu (P) on whichever mutation he wants next.
 * Unlocked mutations are a bitmask in the profile ({@code UkakuMutations}), so death keeps them and {@code /kagune reset}
 * clears them.
 */
public enum UkakuMutation {
    /** In ghoul form (the kagune out): a big jump and speed boost. */
    AGILITY_BOOST("agility_boost", "Agility Boost", "Kagune out: twice as fast on your feet and a far higher jump."),
    /** The right-click hop throws him back further; W held with it, he dashes forward through everything instead. */
    FIRE_DASH("fire_dash", "Fire Dash", "Rabbit Hop throws you back further; hold W and right click to dash forward instead."),
    /** The Wing Vortex's fire cyclone is far bigger and hits harder. */
    FIRE_DANCE("fire_dance", "Fire Dance", "The Wing Vortex's fire cyclone is far bigger and hits much harder."),
    /** Stuck spikes and needles explode when they burst, and fire shards explode where they hit. */
    BURSTING_SHARDS("bursting_shards", "Bursting Shards", "Spikes and needles explode when they burst; fire shards explode where they hit."),
    /**
     * 1.0.41: You Thought You'd Get Rid of Me That Easily? (Graft's, the same death and the same knitting back together,
     * {@code PaleRevive}): once every five minutes what would kill him blows him apart instead, and he pulls himself back together.
     */
    NOT_THAT_EASILY("not_that_easily", "You Thought You'd Get Rid of Me That Easily?",
        "Every 5 min, death blows you apart and all forget you; you knit back together, slowly, and stand up whole.");

    /** Mastery milestones: each one reached earns a mutation point (1.0.41: a fifth at 1000, for the fifth mutation). */
    public static final int[] MILESTONES = {300, 500, 700, 900, 1000};

    private final String id;
    private final String displayName;
    private final String description;

    UkakuMutation(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public String description() {
        return this.description;
    }

    /** This mutation's bit in the unlocked mask. */
    public int bit() {
        return 1 << this.ordinal();
    }

    /** True if {@code mask} has this mutation. */
    public boolean in(int mask) {
        return (mask & this.bit()) != 0;
    }

    /** Mutation points earned by {@code mastery}: one for each milestone reached. */
    public static int earned(int mastery) {
        int points = 0;
        for (int milestone : MILESTONES) {
            if (mastery >= milestone) {
                points++;
            }
        }
        return points;
    }

    /** Mutations unlocked in {@code mask} (only the real bits count). */
    public static int unlocked(int mask) {
        return Integer.bitCount(mask & all());
    }

    /** Points left to spend at {@code mastery} with {@code mask} unlocked (never below zero). */
    public static int available(int mastery, int mask) {
        return Math.max(0, earned(mastery) - unlocked(mask));
    }

    /** True if {@code mutation} can be unlocked now: not yet had, and a point to spend. */
    public static boolean canUnlock(int mastery, int mask, UkakuMutation mutation) {
        return canUnlock(mastery, mask, mutation, 0);
    }

    /** 7.0.1: as above with {@code bonus} points earned from quests besides the milestones'. */
    public static boolean canUnlock(int mastery, int mask, UkakuMutation mutation, int bonus) {
        return mutation != null && !mutation.in(mask) && available(mastery, mask, bonus) > 0;
    }

    /** 7.0.1: points left to spend with {@code bonus} earned from quests besides the milestones'. */
    public static int available(int mastery, int mask, int bonus) {
        return Math.max(0, earned(mastery) + Math.max(0, bonus) - unlocked(mask));
    }

    /** The mastery at which the next point comes, or -1 when every milestone is reached. */
    public static int nextMilestone(int mastery) {
        for (int milestone : MILESTONES) {
            if (mastery < milestone) {
                return milestone;
            }
        }
        return -1;
    }

    /** Every mutation's bit. */
    public static int all() {
        int mask = 0;
        for (UkakuMutation mutation : values()) {
            mask |= mutation.bit();
        }
        return mask;
    }

    public static UkakuMutation byOrdinal(int ordinal) {
        UkakuMutation[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
