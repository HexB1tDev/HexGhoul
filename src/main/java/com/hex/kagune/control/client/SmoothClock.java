package com.hex.kagune.control.client;

/**
 * 1.0.27: the clock the expansion draws by, in world ticks. The level's own game time is no good for it: the server sets it again
 * once a second (its time packet), a tick back or on whenever the client's count has drifted from the server's, and several ticks
 * back when the server lags, so whatever moves by it hitches (a stutter once a second, the kagune feeling as if run tick by tick).
 * This one counts the client's own ticks and is eased back onto the game time a little each tick, within the tick (never a jump):
 * between two ticks it runs from one to the next at a rate a little over or under one, so it stays on the server's stamps. Only
 * when it is far off (a new world, a long freeze) does it jump onto it.
 */
public final class SmoothClock {
    /** Of how far it is off the game time, the share made up each tick; the most a tick may be sped up or slowed (ticks); how far off it jumps. */
    static final double SLEW = 0.1;
    static final double MOST = 0.6;
    static final double SNAP = 40.0;

    private double base = Double.NaN;
    private double rate = 1.0;
    private long ticks;

    /** Where it starts again from: {@code game} the game time, {@code ticks} the client's own tick count. */
    public void reset(double game, long ticks) {
        this.base = game;
        this.rate = 1.0;
        this.ticks = ticks;
    }

    /** The client has counted {@code ticks} (its own count; it only goes up); {@code game} is the game time now. */
    public void at(double game, long ticks) {
        if (Double.isNaN(this.base)) {
            this.reset(game, ticks);
            return;
        }
        long passed = ticks - this.ticks;
        if (passed == 0L) {
            return;
        }
        if (passed < 0L || passed > SNAP) {
            this.reset(game, ticks);
            return;
        }
        this.base += this.rate * passed;
        this.ticks = ticks;
        double off = game - this.base;
        if (Math.abs(off) > SNAP) {
            this.reset(game, ticks);
            return;
        }
        this.rate = 1.0 + Math.max(-MOST, Math.min(MOST, off * SLEW));
    }

    /** The time {@code partial} of a tick into this one. */
    public double time(double partial) {
        return Double.isNaN(this.base) ? partial : this.base + partial * this.rate;
    }
}
