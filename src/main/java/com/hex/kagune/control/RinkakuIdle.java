package com.hex.kagune.control;

/**
 * 2.0.51 (pure, tested; the owner, of the tails in game: "sometimes it twitches and snaps back into weird positions and seems short ... It
 * also does not match the shape you shown me, your render made it look way better, that's what I want"): a Rinkaku's tails at rest, as
 * drawn in the showcase rather than as the original's chain lets them fall (coiled, short, twitching as its links settle). Each pair has its
 * shape ({@link #keys}): the first arcs up off the back and over, its end high behind the shoulder; the second fans out wide to the side
 * and up; the third sweeps back and up. Each is scaled to the tail's own length (its spread more than its height), and alive: swaying a
 * little, each out of step with the others ({@link #sway}), trailing back as he runs and lagging as he turns.
 *
 * <p>Points are in the owner's own frame from his feet (x right, y up, z forward, blocks), as {@link RinkakuRoot}'s; a body {@code height}
 * tall (a player's 1.8).
 */
public final class RinkakuIdle {
    private RinkakuIdle() {
    }

    /** The length of the drawn shapes (blocks, at a player's size) before they are scaled to a tail's own. */
    public static final double DESIGNED = 3.4;
    /** Of a scale past the designed length, how much goes up rather than out (a long tail spreads, it doesn't tower). */
    public static final double RISE = 0.3;
    /** How far its keys sway (blocks, the end's), and over how long (ticks). */
    public static final double SWAY = 0.16;
    public static final double SWAY_PERIOD = 84.0;
    /** Running full tilt (blocks a tick) it trails this far back and down at its end, and turning (radians a tick) it lags this far. */
    public static final double FULL_RUN = 0.28;
    public static final double TRAIL_BACK = 1.0;
    public static final double TRAIL_DOWN = 0.45;
    public static final double TURN_LAG = 0.55;
    /** How fast it settles into the rest (a share a tick) and leaves it for a move (a share a tick: quickly, so a blow isn't late). */
    public static final double SETTLE = 0.12;
    public static final double LEAVE = 0.45;

    /** The shape of a tail of pair {@code pair} on {@code side} (+1 right, -1 left) at a player's size: its keys past its base. */
    static double[][] keys(int pair, double side) {
        return switch (Math.max(0, Math.min(2, pair))) {
            case 0 -> new double[][] {{side * 0.75, 1.55, -0.85}, {side * 1.05, 2.35, -1.0}, {side * 1.0, 2.85, -0.55}, {side * 0.8, 2.95, 0.05}};
            case 1 -> new double[][] {{side * 1.0, 0.95, -0.7}, {side * 1.75, 1.15, -0.85}, {side * 2.25, 1.65, -0.6}, {side * 2.4, 2.05, -0.15}};
            default -> new double[][] {{side * 0.6, 0.75, -1.0}, {side * 1.05, 0.95, -1.7}, {side * 1.4, 1.55, -2.1}, {side * 1.5, 2.15, -1.9}};
        };
    }

    /** The pair tail {@code limb} of {@code tails} rests with: one or two tails rest as the first pair, the rest by twos. */
    public static int pair(int limb, int tails) {
        return tails <= 2 ? 0 : Math.min(2, Math.max(0, limb) / 2);
    }

    /**
     * How far (blocks) key {@code key} of {@code count} sways at {@code time} (ticks), tail {@code seed}: {x, y, z}, the further out the
     * more.
     */
    public static double[] sway(int key, int count, double time, int seed) {
        double out = Math.pow((key + 1.0) / count, 1.3);
        double phase = (seed & 1023) * 0.77 + key * 0.9;
        double w = 2.0 * Math.PI / SWAY_PERIOD;
        return new double[] {SWAY * out * Math.sin(time * w + phase), 0.7 * SWAY * out * Math.sin(time * w * 1.31 + phase * 1.7),
            0.5 * SWAY * out * Math.sin(time * w * 0.83 + phase * 2.3)};
    }

    /**
     * Tail {@code limb} of {@code tails} at rest, {@code n} points from its root (owner's frame), for a body {@code height} tall and a tail
     * {@code length} long, at {@code time} (ticks; NaN: still), tail {@code seed}, he moving {@code forward} and {@code sideways} (blocks a
     * tick, his frame) and turning {@code turn} (radians a tick, + to his left). Evenly spaced.
     */
    public static double[][] pose(int limb, int tails, double fanSide, int n, double height, double length, double time, int seed,
                                  double forward, double sideways, double turn) {
        double size = height / 1.8;
        double side = RinkakuRoot.side(limb, fanSide);
        double[] root = RinkakuRoot.root(limb, side, height);
        double[] way = RinkakuRoot.direction(limb, side);
        double scale = Math.max(0.3, length / (DESIGNED * size));
        double up = 1.0 + (scale - 1.0) * RISE;
        double[][] shape = keys(pair(limb, tails), side);
        int count = shape.length;
        double[][] keys = new double[count + 2][];
        keys[0] = root;
        keys[1] = new double[] {root[0] + way[0] * 0.5 * size, root[1] + way[1] * 0.5 * size + 0.04 * size, root[2] + way[2] * 0.5 * size};
        double run = Math.max(-1.0, Math.min(1.0, forward / FULL_RUN));
        double drift = Math.max(-1.0, Math.min(1.0, sideways / FULL_RUN));
        double lag = Math.max(-1.0, Math.min(1.0, turn * 6.0));
        for (int i = 0; i < count; i++) {
            double[] k = shape[i];
            // From the base out: spread by its scale, raised by less.
            double x = keys[1][0] + (k[0] * size - keys[1][0]) * scale;
            double y = keys[1][1] + (k[1] * size - keys[1][1]) * up;
            double dz = k[2] * size - keys[1][2];
            double z = keys[1][2] + dz * (dz > 0.0 ? Math.min(1.0, scale) : scale); // never further forward over him for being longer
            double out = (i + 1.0) / count;
            if (Double.isFinite(time)) {
                double[] s = sway(i, count, time, seed);
                x += s[0] * scale;
                y += s[1] * scale;
                z += s[2] * scale;
            }
            // Running it trails back and down; stepping aside it leans away; turning it lags the other way.
            z -= TRAIL_BACK * run * out * scale;
            y -= TRAIL_DOWN * Math.abs(run) * out * scale;
            x -= TRAIL_BACK * 0.6 * drift * out * scale;
            x += TURN_LAG * lag * out * scale;
            keys[i + 2] = new double[] {x, y, z};
        }
        return RinkakuBlock.resample(RinkakuBlock.spline(keys, 12), n);
    }

    /** How far into its rest a tail is after a tick that wants it there ({@code rest}) or not, from {@code weight} (0..1), {@code dt} ticks on. */
    public static double settle(double weight, boolean rest, double dt) {
        double t = Math.max(0.0, Math.min(5.0, Double.isFinite(dt) ? dt : 0.0));
        double w = Math.max(0.0, Math.min(1.0, Double.isFinite(weight) ? weight : 0.0));
        return rest ? Math.min(1.0, w + SETTLE * t) : Math.max(0.0, w - LEAVE * t);
    }
}
