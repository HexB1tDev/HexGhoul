package com.hex.kagune.control.furuta;

/**
 * 7.0.0: Furuta DNA's mutations (pure, tested), as every path has its own: each mastery milestone ({@link #MILESTONES}) earns a
 * point, spent in his menu on whichever he wants next. Unlocked mutations are a bitmask in the profile ({@code FurutaMutations}),
 * so death keeps them and {@code /kagune reset} clears them.
 */
public enum FurutaMutation {
    /**
     * You Thought You'd Get Rid of Me That Easily? (Owl DNA's, carried over): once every five minutes what would kill him blows his
     * body apart instead and everything forgets him, as if he had died; he lies in pieces a long while before flesh lines run out of
     * what is left of his chest and haul him back together, whole, in his kagune ({@code ReviveRules}, slower since 7.0.0).
     */
    NOT_THAT_EASILY("not_that_easily", "You Thought You'd Get Rid of Me That Easily?",
        "Every 5 min, death blows you apart and all forget you; you knit back together, slowly, and stand up whole."),
    /**
     * Rize's Appetite: what his kagune bites feeds him (every bite that lands fills the bar a little, a kill a lot) and heals him.
     */
    RIZES_APPETITE("rizes_appetite", "Rize's Appetite", "Every bite that lands feeds you; a kill fills the bar far more and heals you."),
    /** Kakuhou Overdrive: his wounds start closing sooner and close faster, and heavy blows grow back more of what they took. */
    KAKUHOU_OVERDRIVE("kakuhou_overdrive", "Kakuhou Overdrive", "Wounds close sooner and faster; heavy blows grow back far more of what they took."),
    /** Washuu Blood: the kakuja comes cheaper, burns RC half as fast and hits harder. */
    WASHUU_BLOOD("washuu_blood", "Washuu Blood", "The kakuja costs less, burns RC half as fast and its blows land harder.");

    /** Mastery milestones: each one reached earns a mutation point. */
    public static final int[] MILESTONES = {500, 800, 900, 1000};
    /** Rize's Appetite: RC a landed bite feeds, a kill's (and the health it gives back). */
    public static final float APPETITE_BITE = 2.0F;
    public static final float APPETITE_KILL = 12.0F;
    public static final float APPETITE_HEAL = 4.0F;
    /** Kakuhou Overdrive: the share of the wait and the healing time left, and how much more a heavy blow grows back. */
    public static final double OVERDRIVE_SHARE = 0.7;
    public static final float OVERDRIVE_RECOVER = 0.2F;
    /** Washuu Blood: the kakuja's price, the share of its drain, its blows' extra weight. */
    public static final float BLOOD_KAKUJA_COST = 15.0F;
    public static final float BLOOD_DRAIN = 0.5F;
    public static final float BLOOD_DAMAGE = 1.15F;

    private final String id;
    private final String displayName;
    private final String description;

    FurutaMutation(String id, String displayName, String description) {
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
    public static boolean canUnlock(int mastery, int mask, FurutaMutation mutation) {
        return canUnlock(mastery, mask, mutation, 0);
    }

    /** 7.0.1: as above with {@code bonus} points earned from quests besides the milestones'. */
    public static boolean canUnlock(int mastery, int mask, FurutaMutation mutation, int bonus) {
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
        for (FurutaMutation mutation : values()) {
            mask |= mutation.bit();
        }
        return mask;
    }

    public static FurutaMutation byOrdinal(int ordinal) {
        FurutaMutation[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    public static FurutaMutation fromId(String id) {
        for (FurutaMutation mutation : values()) {
            if (mutation.id.equals(id)) {
                return mutation;
            }
        }
        return null;
    }
}
