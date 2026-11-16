package com.hex.kagune.control.ukaku;

/**
 * 4.2.9 (the owner: "Blade Arm Control + right click (does a high speed dash, only leaving behind after images)"): the Steel Blade's flash
 * (pure, tested). With the blade out, Ctrl + right click throws him along his look, flat and very fast, for {@link #TICKS} ticks
 * ({@link #SPEED} blocks a tick, about ten blocks); for as long he is not seen at all, only the afterimages he leaves every {@link #GHOST_EVERY}
 * blocks along the way. Everything living within {@link #RADIUS} of the line he crossed is cut once, as hard as a Steel Blade cut times
 * {@link #DAMAGE}. Then {@link #COOLDOWN} ticks before the next.
 */
public final class SteelFlash {
    /** How long it throws him (ticks) and how fast (blocks a tick): two dashes' worth, in the time of one Reaper's dash. */
    public static final int TICKS = 4;
    public static final double SPEED = 2.6;
    /** Ticks after it before the next one. */
    public static final int COOLDOWN = 80;
    /** How long after the throw his crossings are still read (ticks): a client's move reaches the server a tick or two late. */
    public static final int SWEEP_AFTER = 4;
    /** How far from the line he crossed a body is still cut (blocks, to its middle, less its own half width). */
    public static final double RADIUS = 1.3;
    /** Its cut against a Steel Blade cut's. */
    public static final float DAMAGE = 1.5F;
    /** At most how many it cuts. */
    public static final int MOST = 8;
    /** One afterimage about every this far along his path (blocks: a body apart, each its own figure, not a smear), and at most this many a tick. */
    public static final double GHOST_EVERY = 1.6;
    public static final int GHOSTS_A_TICK = 6;

    private SteelFlash() {
    }

    /** How far the throw carries him over open ground (blocks). */
    public static double distance() {
        return SPEED * TICKS;
    }

    /**
     * True when a body centred at {@code p} ({@code halfWidth} its half width, {@code halfHeight} its half height) lies within {@link #RADIUS}
     * of the segment {@code a} to {@code b} he crossed (feet to feet: the line is taken through his middle, a block up).
     */
    public static boolean crossed(double[] a, double[] b, double[] p, double halfWidth, double halfHeight) {
        double ax = a[0];
        double ay = a[1] + 1.0;
        double az = a[2];
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        double length = dx * dx + dy * dy + dz * dz;
        double t = length < 1.0E-9 ? 0.0 : ((p[0] - ax) * dx + (p[1] - ay) * dy + (p[2] - az) * dz) / length;
        t = t < 0.0 ? 0.0 : t > 1.0 ? 1.0 : t;
        double cx = ax + dx * t - p[0];
        double cy = ay + dy * t - p[1];
        double cz = az + dz * t - p[2];
        // Across: the line against its width; up and down: against its height (a tall body is cut wherever the line passes it).
        double across = Math.sqrt(cx * cx + cz * cz) - halfWidth;
        double up = Math.abs(cy) - halfHeight;
        return across <= RADIUS && up <= RADIUS;
    }

    /** How many afterimages a stretch of {@code blocks} leaves (one every {@link #GHOST_EVERY}, at most {@link #GHOSTS_A_TICK}). */
    public static int ghosts(double blocks) {
        if (!(blocks > 1.0E-3)) {
            return 0;
        }
        return (int)Math.max(1, Math.min(GHOSTS_A_TICK, Math.round(blocks / GHOST_EVERY)));
    }
}
