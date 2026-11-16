package com.hex.kagune.control.bikaku;

/**
 * 3.6.3: the Bikaku path's mutations (pure, tested). Not opened by mastery (the owner: "these mutations are NOT unlocked through mastery
 * they're gonna be unlocked through an NPC's quest introduced later today"): a quest unlocks each ({@code BikakuDevour.grant}); until it
 * is in, an operator can ({@code /kagune bikaku mutation <id> on|off}). Unlocked mutations are a bitmask in the profile
 * ({@code BikakuMutations}), so death keeps them and {@code /kagune reset} clears them. The owner's words for both are in
 * {@code docs/handoff/owner-words-mutations.md}.
 */
public enum BikakuMutation {
    /**
     * Grand Devouring: a body within reach at one heart or less is fed on by one of the worms on its own ({@link GrandDevour}): it swells,
     * swallows it whole, and the bulge of it runs down to his back, refilling his bar. That worm does nothing else until it is done.
     */
    GRAND_DEVOURING("grand_devouring", "Grand Devouring",
        "A body within 10 blocks at 1 heart or less: a worm swallows it whole and your bar refills. 60 s cooldown.", true),
    /**
     * Potential Unlock: three times the energy, his bar purple (3.6.7); torn apart, he pulls himself back together while his energy lasts
     * (3.7.3: BikakuPotential).
     */
    POTENTIAL_UNLOCK("potential_unlock", "Potential Unlock",
        "Three times the energy; your bar turns purple. Torn apart, you pull yourself back together while energy lasts.", true);

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

    /** The one called {@code id} (its {@link #id()}), or null. */
    public static BikakuMutation byId(String id) {
        for (BikakuMutation mutation : values()) {
            if (mutation.id.equals(id)) {
                return mutation;
            }
        }
        return null;
    }

    public static BikakuMutation byOrdinal(int ordinal) {
        BikakuMutation[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
