package com.hex.kagune.control.family;

/**
 * 1.3.20: a moment between a child and one of its parents (pure): a player, or its NPC parent (Eto, Touka, Ken). Each is drawn on both
 * bodies (the child's and the parent's, {@code ChildMomentPose}) for {@link #ticks}; the child stands {@link #stand} blocks off its parent
 * (a player's size) facing them, or is held in their arms ({@link #held}). Some are the child's own (no partner pose: {@link #solo}): a wave,
 * a clap, arms up to be picked up, a flower shown and given. Sent by ordinal: new ones only at the end.
 */
public enum ChildMoment {
    NONE(0, 0, false, false, 0.0),
    /** It runs into their arms; they bend down to it (a little one) or simply hold it (a teen). */
    HUG(70, mask(ChildStage.TODDLER, ChildStage.CHILD, ChildStage.TEEN, ChildStage.ADULT), false, false, 0.5),
    /** A hand on its head, ruffling its hair; it beams up at them. */
    PAT(50, mask(ChildStage.BABY, ChildStage.TODDLER, ChildStage.CHILD, ChildStage.TEEN, ChildStage.ADULT), false, false, 0.72),
    /** Hands up, slapped together. */
    HIGH_FIVE(28, mask(ChildStage.CHILD, ChildStage.TEEN, ChildStage.ADULT), false, false, 0.95),
    /** Fingers at its sides; it squirms and giggles. */
    TICKLE(60, mask(ChildStage.BABY, ChildStage.TODDLER, ChildStage.CHILD), false, false, 0.58),
    /** Hands over their face, opened: peekaboo! It bounces and claps. */
    PEEKABOO(72, mask(ChildStage.BABY, ChildStage.TODDLER), false, false, 0.9),
    /** Thrown up out of their arms and caught, three times. */
    TOSS(66, mask(ChildStage.BABY, ChildStage.TODDLER), true, false, 0.0),
    /** Held out at arm's length and spun round, its legs flying out. */
    SPIN(64, mask(ChildStage.TODDLER, ChildStage.CHILD), true, false, 0.0),
    /** A toddler's arms up: pick me up! */
    ARMS_UP(60, mask(ChildStage.TODDLER), false, true, 0.8),
    /** A wave at them. */
    WAVE(36, mask(ChildStage.TODDLER, ChildStage.CHILD, ChildStage.TEEN, ChildStage.ADULT), false, true, 0.0),
    /** A happy clap (a baby's, a toddler's). */
    CLAP(36, mask(ChildStage.BABY, ChildStage.TODDLER), false, true, 0.0),
    /** A flower it found, held up to them, and given. */
    FLOWER(56, mask(ChildStage.TODDLER, ChildStage.CHILD), false, true, 0.85),
    /** 1.3.21: tucked into its bed: they lean over it, smooth its covers, a hand on its head. */
    TUCK(80, mask(ChildStage.BABY, ChildStage.TODDLER, ChildStage.CHILD, ChildStage.TEEN, ChildStage.ADULT), false, false, 0.0),
    /** 1.3.21: fed from their hand: it eats, munching. */
    FEED(60, mask(ChildStage.BABY, ChildStage.TODDLER, ChildStage.CHILD, ChildStage.TEEN, ChildStage.ADULT), false, false, 0.85),
    // 4.1.15 (the owner: "add more kid interactions"): appended (sent by ordinal).
    /** 4.1.15: bent down (or in) to nuzzle the top of its head; it ducks its head for it, then beams up at them. */
    NUZZLE(48, mask(ChildStage.BABY, ChildStage.TODDLER, ChildStage.CHILD, ChildStage.TEEN, ChildStage.ADULT), false, false, 0.62),
    /** 4.1.15: a finger to the tip of its nose: boop. It jerks back, startled, then giggles and wobbles. */
    BOOP(34, mask(ChildStage.BABY, ChildStage.TODDLER, ChildStage.CHILD), false, false, 0.72),
    /** 4.1.15: fists bumped, then both blown apart: a little explosion. */
    FIST_BUMP(30, mask(ChildStage.CHILD, ChildStage.TEEN, ChildStage.ADULT), false, false, 0.9),
    /** 4.1.15: patty-cake: their own hands clapped, then each other's, four times over. */
    PATTY_CAKE(80, mask(ChildStage.TODDLER, ChildStage.CHILD), false, false, 0.62),
    /** 4.1.15: dancing together: both its hands in theirs, swaying side to side, stepping. */
    DANCE(90, mask(ChildStage.TODDLER, ChildStage.CHILD, ChildStage.TEEN), false, false, 0.85);

    public final int ticks;
    private final int stages;
    /** True if it is done in their arms (the child held, or lifted up for it). */
    public final boolean held;
    /** True if it is the child's alone (its parent's body is not posed for it). */
    public final boolean solo;
    /** How far off them it stands (blocks, a player's size); 0: wherever it is. */
    public final double stand;

    ChildMoment(int ticks, int stages, boolean held, boolean solo, double stand) {
        this.ticks = ticks;
        this.stages = stages;
        this.held = held;
        this.solo = solo;
        this.stand = stand;
    }

    private static int mask(ChildStage... stages) {
        int out = 0;
        for (ChildStage stage : stages) {
            out |= 1 << stage.ordinal();
        }
        return out;
    }

    /** True if a child at {@code stage} can have this moment. */
    public boolean fits(ChildStage stage) {
        return this != NONE && (this.stages & 1 << stage.ordinal()) != 0;
    }

    /**
     * 4.1.15 (the owner: "there's no way a son, like starting from being a teen, there's no way he would dance with his fucking dad or mom.
     * It's embarrassing ... stop the touching with your fucking son. Like, they don't like to be touched"): true if a child at {@code stage}
     * can have this moment, {@code girl} or a boy: a son from his teens on has no hugs, pats on the head, nuzzles, dances or tickles (a fist
     * bump, a high five, food handed over, a wave: all fine).
     */
    public boolean fits(ChildStage stage, boolean girl) {
        return this.fits(stage) && (!this.touchy() || touched(stage, girl));
    }

    /** 4.1.15: true if this is the parent's hands on the child (a hug, a pat, a nuzzle, a boop, tickles, a dance): what a teenage son won't have. */
    public boolean touchy() {
        return this == HUG || this == PAT || this == NUZZLE || this == BOOP || this == TICKLE || this == DANCE;
    }

    /** 4.1.15: true if a child at {@code stage} ({@code girl}, or a boy) is hugged, patted, held by the hand: anyone but a son from his teens on. */
    public static boolean touched(ChildStage stage, boolean girl) {
        return girl || !stage.older();
    }

    /** True while it runs ({@code age} ticks in). */
    public boolean running(double age) {
        return this != NONE && age >= 0.0 && age < this.ticks;
    }

    public static ChildMoment byOrdinal(int ordinal) {
        ChildMoment[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    /** What a parent may start from the child's play page, in the order shown. */
    public static final ChildMoment[] PLAY = {HUG, PAT, HIGH_FIVE, TICKLE, PEEKABOO, TOSS, SPIN, NUZZLE, BOOP, FIST_BUMP, PATTY_CAKE, DANCE};
    /** What a child starts on its own, near a parent, now and then. */
    public static final ChildMoment[] OWN = {ARMS_UP, WAVE, CLAP, FLOWER, HUG};
    /** What an NPC parent starts with its child on its own (not in its arms: in its arms, it tosses it). */
    public static final ChildMoment[] NPC = {HUG, PAT, TICKLE, PEEKABOO, SPIN, HIGH_FIVE, FEED, NUZZLE, BOOP, PATTY_CAKE, DANCE};

    /** The play page's words for it. */
    public String label() {
        return switch (this) {
            case HUG -> "Come here, hug!";
            case PAT -> "*pat their head*";
            case HIGH_FIVE -> "High five!";
            case TICKLE -> "*tickle them*";
            case PEEKABOO -> "Peekaboo!";
            case TOSS -> "Up you go! *toss them*";
            case SPIN -> "Airplane! *spin them round*";
            case NUZZLE -> "*nuzzle the top of their head*";
            case BOOP -> "Boop! *tap their nose*";
            case FIST_BUMP -> "Fist bump!";
            case PATTY_CAKE -> "Patty-cake!";
            case DANCE -> "Dance with me!";
            default -> "";
        };
    }

    // ---------------------------------------------------------------- shapes over time (shared by both sides' poses)

    /** TOSS: how high above their arms it is at {@code age} (blocks, a player's size): three throws and catches. */
    public static double tossLift(double age) {
        double each = TOSS.ticks / 3.0;
        double t = age % each / each;
        if (age < 0.0 || age >= TOSS.ticks) {
            return 0.0;
        }
        // Up and back down (a throw's arc), its peak about the middle; a little dip as it is caught.
        double arc = 4.0 * t * (1.0 - t);
        return Math.max(-0.06, 0.95 * arc - 0.06 * Math.max(0.0, Math.sin(t * Math.PI * 2.0)));
    }

    /** TOSS: how far their arms are up throwing (0 cradling .. 1 up high). */
    public static double tossArms(double age) {
        double each = TOSS.ticks / 3.0;
        double t = age % each / each;
        return age < 0.0 || age >= TOSS.ticks ? 0.0 : Math.max(0.0, 1.0 - Math.abs(t - 0.12) / 0.3) * 0.9 + Math.max(0.0, 1.0 - Math.abs(t - 0.95) / 0.12) * 0.5;
    }

    /** SPIN: how far round they have turned (radians, + to their right), eased in and out: two turns. */
    public static double spinAngle(double age) {
        double t = Math.max(0.0, Math.min(1.0, age / SPIN.ticks));
        double eased = t * t * (3.0 - 2.0 * t);
        return eased * Math.PI * 4.0;
    }

    /**
     * 1.3.24: SPIN: which way round they face now (radians, + to their right, -pi..pi): {@link #spinAngle} wrapped. Two whole turns end
     * facing the way they began, so as the moment eases out nothing unwinds (the owner: it "spins all the way back"), and a body's spin
     * never reaches the pose's one-turn limit (RigPose.settle) and stops halfway.
     */
    public static double spinTurn(double age) {
        double a = spinAngle(age);
        return Math.atan2(Math.sin(a), Math.cos(a));
    }

    /** How far into it a pose has come (0..1): eased in over the first few ticks and out over the last few. */
    public static double weight(ChildMoment moment, double age) {
        if (!moment.running(age)) {
            return 0.0;
        }
        double in = Math.min(1.0, age / 6.0);
        double out = Math.min(1.0, (moment.ticks - age) / 6.0);
        double w = Math.min(in, out);
        return w * w * (3.0 - 2.0 * w);
    }
}
