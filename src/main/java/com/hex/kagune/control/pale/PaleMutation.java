package com.hex.kagune.control.pale;

/**
 * 6.2.0: Pale's mutations (pure, tested), as the other paths have theirs: each mastery milestone ({@link #MILESTONES}) earns one
 * mutation point, spent in her menu on whichever mutation she wants next. Unlocked mutations are a bitmask in the profile
 * ({@code PaleMutations}), so death keeps them and {@code /kagune reset} clears them.
 */
public enum PaleMutation {
    /**
     * You Thought You'd Get Rid of Me That Easily?: once every {@link ReviveRules#COOLDOWN} ticks, what would kill her blows her
     * body apart instead and everything forgets her, as if she had died; five seconds later flesh lines run out of what is left of
     * her chest to every piece of her and pull her back together, whole, in her kagune, a gust throwing everything near her back
     * ({@link ReviveRules}, {@link Remains}).
     */
    NOT_THAT_EASILY("not_that_easily", "You Thought You'd Get Rid of Me That Easily?",
        "Every 5 min, death blows you apart and all forget you; 5 s later you stand up whole, a gust throwing all back."),
    /**
     * Up We Go: her flips (right click) launch her far higher and further up, the launch built up over a few ticks (real momentum,
     * smooth acceleration, never a jolt), turning over slower across the longer climb ({@link PaleRules#soar}).
     */
    UP_WE_GO("up_we_go", "Up We Go", "Your flips launch you far higher and further up, picking up speed: real momentum, no jolt."),
    /**
     * 6.2.0, True Anonymity: the Flesh Hammer's club (V held) grows on past its full size ({@link PaleRules#HAMMER_HUGE}), bristling
     * with bone spikes all over the flesh; its stomp reaches further, its shockwave is wider and its crater bigger, and everything it
     * catches bleeds.
     */
    TRUE_ANONYMITY("true_anonymity", "True Anonymity", "Flesh Hammer: a far bigger club bristling with bone spikes; wider, further stomp; all it hits bleeds."),
    /**
     * 6.2.0, Witness This: a fully charged Big Punch that lands goes on into a combo ({@code WitnessRules}): she is behind it,
     * uppercuts it high into the air, follows it up and blitzes it from every side, then is above it and kicks it down into a
     * crater. It is frozen all through it.
     */
    WITNESS_THIS("witness_this", "Witness This", "A full-charge Big Punch goes on: uppercut, a midair blitz from every side, a kick down into a crater.");

    /**
     * 6.2.2: her Witness This setting, carried in the synced mask above every mutation's bit: Instant Big Punch, Z tapped is the
     * full-charge punch at once (and so the combo).
     */
    public static final int INSTANT_PUNCH = 1 << 30;

    /** Mastery milestones: each one reached earns a mutation point (6.2.0: a second at 800; 6.2.0: a third at 900, a fourth at 1000). */
    public static final int[] MILESTONES = {500, 800, 900, 1000};

    private final String id;
    private final String displayName;
    private final String description;

    PaleMutation(String id, String displayName, String description) {
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
    public static boolean canUnlock(int mastery, int mask, PaleMutation mutation) {
        return mutation != null && !mutation.in(mask) && available(mastery, mask) > 0;
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
        for (PaleMutation mutation : values()) {
            mask |= mutation.bit();
        }
        return mask;
    }

    public static PaleMutation byOrdinal(int ordinal) {
        PaleMutation[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
