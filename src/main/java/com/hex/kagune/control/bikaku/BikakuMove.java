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
    DRAG("Predatory Drag", Slot.J, BikakuRules.DRAG, 46, 11, 200, 4.0F),
    /**
     * 3.5.1, Ravenous Pursuit: the worm splits into three thinner mouth-bearing worms out of the same root, each going after its own body
     * near him in fast, snaking lunges from its own side (one over each shoulder, one rearing high behind his head); against one body, all
     * three bite it one after another. Bleed, several hits.
     */
    PURSUIT("Ravenous Pursuit", Slot.Z, BikakuRules.PURSUIT, 44, 12, 240, 5.0F),
    /**
     * 3.5.1, Carrion Eruption: driven into the ground before him, it tunnels under the surface (the ground heaving and breaking above it)
     * and bursts up under what he aims at with its jaws thrown wide, biting it and throwing it up into the air; it sinks back underground
     * and comes back out of the ground to him. A knock-up, bleeding.
     */
    ERUPTION("Carrion Eruption", Slot.B, BikakuRules.ERUPTION, 54, 24, 280, 6.0F),
    /**
     * 3.5.3, Monstrous Regeneration: for a while ({@link #ticks()}) his body regenerates far faster than any ghoul's, strands of kagune
     * tissue crawling over it and knitting it back. It does not make him invincible (nothing it takes off a blow) and it cools down long.
     * It does not take the worm: it bites and moves on meanwhile.
     */
    REGENERATION("Monstrous Regeneration", Slot.C, BikakuRules.REGENERATION, 160, 1, 900, 15.0F),
    /**
     * 3.5.7, the kakuja's (the owner's spec), Thousand Jaws: every mouthed tentacle thrown out at the bodies round it, each biting again and
     * again on its own rhythm from its own side; the grinning mouths of its body snapping at whatever is close to it (a big bite: a brief
     * stun). Several hits, bleeding. On the worm's Z.
     */
    JAWS("Thousand Jaws", Slot.Z, BikakuRules.KAKUJA, 64, 6, 400, 12.0F),
    /**
     * 3.5.7, the kakuja's Endless Hunger: three of its mouths and a tentacle that coils round it go after one body; held, it is bitten from
     * every side, then pulled into the great maw, which shuts on it: a crushing last bite, then it is let go. Every bite heals him. On G.
     */
    HUNGER("Endless Hunger", Slot.G, BikakuRules.KAKUJA, 62, 10, 360, 10.0F),
    /**
     * 3.5.7, the kakuja's Flesh Cataclysm: the whole mass draws in on itself, then bursts out, its tentacles flung out straight and swept
     * round it while every mouth on it roars: what is close is hurled away, what the tentacles strike is hit hard. Area damage, knockback,
     * a stun. On B.
     */
    CATACLYSM("Flesh Cataclysm", Slot.B, BikakuRules.KAKUJA, 50, 16, 500, 14.0F),
    /**
     * 3.5.7, the kakuja's Undying Abomination: for a while it regenerates at an extraordinary rate, strands of new flesh crawling over its
     * body and tentacles, and takes still less from every blow; a heavy blow slows the regeneration for a moment, never stops it. It runs
     * alongside the rest. On C.
     */
    UNDYING("Undying Abomination", Slot.C, BikakuRules.KAKUJA, 200, 1, 1200, 20.0F);

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

    /** How long it lasts when its bite misses (a grab draws back from there; 3.5.1: the others run their whole course). */
    public int missedTicks() {
        return this.grabs() ? this.land + 15 : this.ticks;
    }

    /** 3.5.3: true if it is something the worm does (the worm is busy until it ends); false: Monstrous Regeneration runs alongside. */
    public boolean occupies() {
        return this != NONE && this != REGENERATION && this != UNDYING;
    }

    /** 3.5.1: a grab (it holds what it catches). 3.5.7: Endless Hunger too. */
    public boolean grabs() {
        return this == MAW || this == DRAG || this == HUNGER;
    }

    /** 3.5.8: its reach on a tap (held, it goes further: {@link BikakuRules#heldReach}); 0 for a move that isn't thrown. */
    public double baseReach() {
        return switch (this) {
            case MAW -> BikakuRules.REACH;
            case DRAG -> BikakuRules.DRAG_REACH;
            case PURSUIT -> BikakuRules.PURSUIT_REACH;
            case ERUPTION -> BikakuRules.ERUPTION_REACH;
            default -> 0.0;
        };
    }

    /** 3.5.8: true for a move held to throw it further (its key's press winds it up, letting go throws it). */
    public boolean held() {
        return this.baseReach() > 0.0;
    }

    /** 3.5.7: one of the kakuja's own moves (made only in it; the worm's are made only out of it). */
    public boolean kakuja() {
        return this == JAWS || this == HUNGER || this == CATACLYSM || this == UNDYING;
    }

    /** True if {@code mastery} has opened it. */
    public boolean unlocked(int mastery) {
        return this != NONE && mastery >= this.mastery;
    }

    /** The worm's move on {@code slot} (none if nothing is on it). */
    public static BikakuMove of(Slot slot) {
        return of(slot, false);
    }

    /** 3.5.7: the move on {@code slot} in the kakuja ({@code kakuja}) or out of it (none if nothing is on it). */
    public static BikakuMove of(Slot slot, boolean kakuja) {
        for (BikakuMove move : values()) {
            if (move != NONE && move.slot == slot && move.kakuja() == kakuja) {
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

    // ---------------------------------------------------------------- 3.5.1: Ravenous Pursuit's beats

    /** Split into three by this tick (merged back into one from {@link #PURSUIT_MERGE} over the last ticks). */
    public static final int PURSUIT_SPLIT = 6;
    public static final int PURSUIT_MERGE = 36;
    /** The first worm bites at {@link #land()}, each of the others this many ticks after the one before. */
    public static final int PURSUIT_EVERY = 5;
    /** How many worms it splits into. */
    public static final int PURSUIT_WORMS = 3;
    /** Each worm's own bite: drawn back this many ticks before it shuts, back at rest this many after. */
    public static final int PURSUIT_LEAD = 7;
    public static final int PURSUIT_AFTER = 10;

    /** The tick worm {@code i} of Ravenous Pursuit bites. */
    public static int pursuitLands(int i) {
        return PURSUIT.land + PURSUIT_EVERY * i;
    }

    // ---------------------------------------------------------------- 3.5.1: Carrion Eruption's beats

    /** Into the ground by this tick; tunnelled under its mark by {@link #ERUPT_RISE}; burst up and shut at {@link #land()}. */
    public static final int ERUPT_DIVE = 8;
    public static final int ERUPT_RISE = 20;
    /** Held up, shaking, until this tick; sunk back under the ground by {@link #ERUPT_SUNK}; back under him by {@link #ERUPT_BACK}. */
    public static final int ERUPT_HOLD = 32;
    public static final int ERUPT_SUNK = 40;
    public static final int ERUPT_BACK = 46;

    // ---------------------------------------------------------------- 3.5.7: the kakuja's moves' beats

    /** Thousand Jaws: each mouthed tentacle's bite comes round this often, shutting this far into it; when each starts (by tentacle). */
    public static final int JAWS_CYCLE = 12;
    public static final int JAWS_BITE = 6;
    static final int[] JAWS_START = {0, 4, 8, -1, 2, 6};
    /** Thousand Jaws: the grinning mouths of its body snap shut at these ticks (opening over the ticks before). */
    public static final int[] JAWS_SNAPS = {14, 30, 46};

    /** The ticks Thousand Jaws' tentacle {@code i} begins a bite (none for the mouthless one). */
    public static java.util.List<Integer> jawsStarts(int i) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        int first = i >= 0 && i < JAWS_START.length ? JAWS_START[i] : -1;
        if (first < 0) {
            return out;
        }
        for (int at = first; at + JAWS_CYCLE <= JAWS.ticks; at += JAWS_CYCLE) {
            out.add(at);
        }
        return out;
    }

    /** True if Thousand Jaws' tentacle {@code i} shuts on something {@code age} ticks in. */
    public static boolean jawsBites(int i, long age) {
        for (int start : jawsStarts(i)) {
            if (age == start + JAWS_BITE) {
                return true;
            }
        }
        return false;
    }

    /** Endless Hunger: coiled round its catch by this tick; its three mouths bite at these ticks (in turn); pulled into the maw from
     *  {@link #HUNGER_PULL}, the maw shutting on it at {@link #HUNGER_CRUSH}; let go at {@link #HUNGER_RELEASE}. */
    public static final int HUNGER_COILED = 14;
    public static final int[] HUNGER_BITES = {14, 18, 22, 26, 30, 34};
    public static final int HUNGER_PULL = 36;
    public static final int HUNGER_CRUSH = 46;
    public static final int HUNGER_RELEASE = 52;

    /** Flesh Cataclysm: drawn in by this tick; burst out at {@link #land()}; swept round until {@link #CATA_SWEPT}; back by its end. */
    public static final int CATA_DRAWN = 13;
    public static final int CATA_SWEPT = 34;
}
