package com.hex.kagune.control.npc;

/**
 * 1.3.13: hand in hand, alive (pure, tested). The one whose hand is held ({@code her}) is drawn on a soft spring from the spot at their
 * side ({@link EtoHand}): as they walk she trails a little behind, pulled along by the hand (further the faster they go), swings a
 * step ahead as they stop (her own momentum) and settles; as they turn she swings out wide on the outside of the turn and in on the
 * inside. Both held arms reach along that gap to meet ({@link #reach}), and the two hands sway together on their own slow clock
 * ({@link #sway}), never with the walk's stride (the arms stay held: none of vanilla's walking swing).
 *
 * <p>One instance per pair, stepped once a tick by the client drawing them ({@code step}), read between ticks ({@code trail(partial)}).
 */
public final class HandSway {
    /** How far she trails behind per block a tick they walk forward, at most (blocks), and how far ahead she can swing at most. */
    public static final double TRAIL_PER_SPEED = 1.9;
    public static final double MAX_TRAIL = 0.6;
    public static final double MAX_AHEAD = 0.22;
    /** The spring: its pull each tick per block off where she wants to be, and how much of its speed is kept each tick. */
    public static final double PULL = 0.16;
    public static final double KEEP = 0.78;
    /** Turning, how far she swings out (blocks) per degree a tick their body turns, at most. */
    public static final double OUT_PER_DEGREE = 0.014;
    public static final double MAX_OUT = 0.25;
    /** A player's arm, shoulder to hand (12 model pixels). */
    public static final double ARM = 0.75;
    /** The sway of the joined hands: how far forward and back (radians), and out and in, on two slow clocks (ticks a cycle). */
    public static final double SWAY = 0.13;
    public static final double SWAY_OUT = 0.04;
    static final double SLOW = 46.0;
    static final double QUICK = 17.0;
    static final double SIDE = 29.0;
    /** Where the slow swing is furthest forward (ticks). */
    public static final double SLOW_QUARTER = SLOW / 4.0;

    /** How far behind the spot at their side she is drawn (blocks; negative: ahead), and its speed; the same, outward. */
    public double trail;
    public double trailSpeed;
    public double out;
    public double outSpeed;
    /** Each, a tick ago (to draw between ticks). */
    public double lastTrail;
    public double lastOut;
    /** Ticks it has been stepped (the sway's clock, its own: never the walk's). */
    public long ticks;

    /**
     * One tick: the one holding her hand moved {@code forward} blocks along the way their body faces (negative: backing up) and turned
     * {@code turn} degrees (positive: to the right, away from her at their right... the turn's outside is to their left, so she, at their
     * right, is on its inside).
     */
    public void step(double forward, double turn) {
        this.lastTrail = this.trail;
        this.lastOut = this.out;
        this.ticks++;
        if (!Double.isFinite(forward) || !Double.isFinite(turn)) {
            return;
        }
        double wantTrail = clamp(forward * TRAIL_PER_SPEED, -MAX_AHEAD, MAX_TRAIL);
        this.trailSpeed = (this.trailSpeed + (wantTrail - this.trail) * PULL) * KEEP;
        this.trail = clamp(this.trail + this.trailSpeed, -MAX_AHEAD - 0.1, MAX_TRAIL + 0.15);
        // She is at their right: turning right she is on the inside (pulled in), turning left on the outside (swung out).
        double wantOut = clamp(-turn * OUT_PER_DEGREE, -MAX_OUT, MAX_OUT);
        this.outSpeed = (this.outSpeed + (wantOut - this.out) * PULL) * KEEP;
        this.out = clamp(this.out + this.outSpeed, -MAX_OUT - 0.08, MAX_OUT + 0.1);
    }

    /** How far behind she is drawn, {@code partial} of the way through this tick. */
    public double trail(float partial) {
        return this.lastTrail + (this.trail - this.lastTrail) * partial;
    }

    /** How far further out than the spot at their side she is drawn, {@code partial} through this tick. */
    public double out(float partial) {
        return this.lastOut + (this.out - this.lastOut) * partial;
    }

    /**
     * How far each held arm turns (radians) to reach its half of a gap {@code gap} blocks long, for an arm {@code size} times a player's:
     * the holder's swings back by it, hers forward.
     */
    public static double reach(double gap, double size) {
        double half = gap * 0.5 / (ARM * Math.max(0.1, size));
        return Math.asin(clamp(half, -0.9, 0.9));
    }

    /**
     * The joined hands' sway forward (radians; both arms together, the same way) at {@code clock} ticks: a slow swing with a quicker one
     * over it, never still for long and never with the stride.
     */
    public static double sway(double clock) {
        return SWAY * (0.75 * Math.sin(clock * Math.PI * 2.0 / SLOW) + 0.25 * Math.sin(clock * Math.PI * 2.0 / QUICK + 1.1));
    }

    /** The joined hands' drift outward (radians, both arms out together) at {@code clock} ticks. */
    public static double swayOut(double clock) {
        return SWAY_OUT * Math.sin(clock * Math.PI * 2.0 / SIDE + 0.4);
    }

    /** How much her upper body leans forward (radians) trailing {@code trail} behind: pulled along by the hand. */
    public static double pulled(double trail) {
        return 0.16 * clamp(trail / MAX_TRAIL, 0.0, 1.0);
    }

    /**
     * Everything one held arm does this frame: {@code x} added to the arm's forward turn (negative: forward, as the model's), {@code z}
     * added to its outward turn (positive: out). {@code holder}: the one walking (their arm swings back toward her as she trails);
     * otherwise her (hers swings forward to them). {@code size}: the arm's against a player's.
     */
    public static float[] arm(double trail, double out, double clock, boolean holder, double size) {
        double reach = reach(trail, size);
        double sway = sway(clock);
        double x = (holder ? reach : -reach) - sway;
        double z = reach(out, size) + swayOut(clock);
        return new float[] {(float)x, (float)z};
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
