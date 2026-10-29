package com.hex.kagune.control;

/**
 * Rinkaku's grapple flight (J, 3.0.0; pure, tested): the tendril hauls him up and over onto the top of the block it grabbed,
 * and sets him down on it. Three overlapping moves: he rises fast out of the launch to an apex clear above both ends, crosses
 * over once he is high enough not to clip what is in front of him, and comes down onto the top from straight above, easing
 * into the landing. Each move takes a share of the flight as long as its stretch of the way.
 */
public final class SlingshotPath {
    /** Blocks above the higher of the two ends the apex clears, plus a little per block of reach. */
    public static final double CLEARANCE = 1.4;
    public static final double CLEARANCE_PER_BLOCK = 0.06;
    /** Average flight speed (blocks per tick) and the flight's bounds in ticks. */
    public static final double SPEED = 1.35;
    public static final int MIN_TICKS = 8;
    public static final int MAX_TICKS = 36;
    /** The descent onto the block lasts at least this many ticks (a soft landing). */
    static final int DESCENT_TICKS = 4;

    private final double[] from;
    private final double[] to;
    private final double apex;
    private final double peakAt;
    private final double crossFrom;
    private final double crossTo;
    private final int ticks;

    private SlingshotPath(double[] from, double[] to, double apex, double peakAt, double crossFrom, double crossTo, int ticks) {
        this.from = from;
        this.to = to;
        this.apex = apex;
        this.peakAt = peakAt;
        this.crossFrom = crossFrom;
        this.crossTo = crossTo;
        this.ticks = ticks;
    }

    /** The flight from {@code from} (his feet) to {@code to} (his feet on top of the block), both {x, y, z}. */
    public static SlingshotPath plan(double[] from, double[] to) {
        double dx = to[0] - from[0];
        double dz = to[2] - from[2];
        double reach = Math.sqrt(dx * dx + dz * dz);
        double apex = Math.max(from[1], to[1]) + CLEARANCE + CLEARANCE_PER_BLOCK * reach;
        double rise = apex - from[1];
        double drop = apex - to[1];
        double length = rise + reach + drop;
        int ticks = (int)Math.max(MIN_TICKS, Math.min(MAX_TICKS, Math.round(length / SPEED)));
        double peakAt = Math.max(0.2, Math.min(1.0 - DESCENT_TICKS / (double)ticks, (rise + 0.5 * reach) / length));
        double crossFrom = 0.6 * peakAt;
        double crossTo = peakAt + 0.75 * (1.0 - peakAt);
        return new SlingshotPath(from.clone(), to.clone(), apex, peakAt, crossFrom, crossTo, ticks);
    }

    /** How many ticks the flight takes. */
    public int ticks() {
        return this.ticks;
    }

    /** Where his feet are after {@code tick} ticks of the flight (0 the launch .. {@link #ticks()} the landing). */
    public double[] at(int tick) {
        double t = Math.max(0.0, Math.min(1.0, tick / (double)this.ticks));
        double y = t <= this.peakAt ? this.from[1] + (this.apex - this.from[1]) * (1.0 - Math.pow(1.0 - t / this.peakAt, 1.5))
            : this.apex - (this.apex - this.to[1]) * smooth((t - this.peakAt) / (1.0 - this.peakAt));
        double across = smooth((t - this.crossFrom) / (this.crossTo - this.crossFrom));
        if (tick >= this.ticks) {
            return this.to.clone();
        }
        return new double[] {this.from[0] + (this.to[0] - this.from[0]) * across, y, this.from[2] + (this.to[2] - this.from[2]) * across};
    }

    /** The velocity that carries him from where tick {@code tick} left him to where tick {@code tick + 1} wants him. */
    public double[] velocity(int tick) {
        double[] a = this.at(tick);
        double[] b = this.at(tick + 1);
        return new double[] {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
    }

    /** How far across he is at {@code tick} (0 above the launch spot .. 1 above the landing). */
    public double across(int tick) {
        return smooth((Math.max(0.0, Math.min(1.0, tick / (double)this.ticks)) - this.crossFrom) / (this.crossTo - this.crossFrom));
    }

    static double smooth(double s) {
        double c = Math.max(0.0, Math.min(1.0, s));
        return c * c * (3.0 - 2.0 * c);
    }
}
