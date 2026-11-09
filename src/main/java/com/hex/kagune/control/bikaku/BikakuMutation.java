package com.hex.kagune.control.bikaku;

/**
 * 3.6.3: the Bikaku path's mutations (pure, tested), as Pale's ({@code PaleMutation}): each mastery milestone ({@link #MILESTONES}) earns
 * one mutation point, spent in the menu on whichever mutation is wanted. Unlocked mutations are a bitmask in the profile
 * ({@code BikakuMutations}), so death keeps them and {@code /kagune reset} clears them. The owner's words for both are in
 * {@code docs/handoff/owner-words-mutations.md}.
 */
public enum BikakuMutation {
    /**
     * Grand Devouring: a body within reach at one heart or less is fed on by one of the worms on its own ({@link GrandDevour}): it swells,
     * swallows it whole, and the bulge of it runs down to his back, refilling his bar. That worm does nothing else until it is done.
     */
    GRAND_DEVOURING("grand_devouring", "Grand Devouring", "A body near you at 1 heart or less: a worm swallows it whole; your bar refills.", true),
    /** Potential Unlock: torn apart, he pulls himself back together while his RC lasts; three times the RC. Not built yet. */
    POTENTIAL_UNLOCK("potential_unlock", "Potential Unlock", "Torn apart, you pull yourself back together while RC lasts; triple RC.", false);

    /** Mastery milestones: each one reached earns a mutation point. */
    public static final int[] MILESTONES = {600, 900};

    private final String id;
    private final String displayName;
    private final String description;
    private final boolean built;

    BikakuMutation(String id, String displayName, String description, boolean built) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.built = built;
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

    /** False for a mutation still to come (shown, not picked). */
    public boolean built() {
        return this.built;
    }

    public int bit() {
        return 1 << this.ordinal();
    }

    /** True if {@code mask} has it. */
    public boolean in(int mask) {
        return (mask & this.bit()) != 0;
    }

    /** Every mutation's bit. */
    public static int all() {
        return (1 << values().length) - 1;
    }

    /** Points earned by {@code mastery}. */
    public static int points(int mastery) {
        int points = 0;
        for (int milestone : MILESTONES) {
            if (mastery >= milestone) {
                points++;
            }
        }
        return points;
    }

    /** Points spent on {@code mask}. */
    public static int spent(int mask) {
        return Integer.bitCount(mask & all());
    }

    /** Points left to spend. */
    public static int left(int mask, int mastery) {
        return Math.max(0, points(mastery) - spent(mask));
    }

    /** True if {@code mutation} can be picked now: built, not had, a point left. */
    public static boolean canPick(BikakuMutation mutation, int mask, int mastery) {
        return mutation != null && mutation.built && !mutation.in(mask) && left(mask, mastery) > 0;
    }

    public static BikakuMutation byOrdinal(int ordinal) {
        BikakuMutation[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
