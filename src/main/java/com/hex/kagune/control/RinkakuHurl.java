package com.hex.kagune.control;

/**
 * 3.1.6 (pure, tested): Rinkaku's Rubble Hurl, on the Throw key (N) whenever no tail holds anyone. The owner asked for an idea for the
 * Throw key with nothing to throw, and chose this one: "N always throws something: a held body, or, with none, the ground itself."
 *
 * <p>Pressed, up to two free tails ({@link #TAILS}; never a centipede leg) dig into the block he looks at within {@link #DIG_REACH} (or
 * the ground just before him) and tear a chunk of it out; it rises ({@link #RISE} ticks) to hang before and above him in the tails'
 * grip ({@link #hover}). Held, it grows ({@link #charge}, {@link #size}: bigger and heavier, a little slower); let go (or after
 * {@link #MOST_HELD} ticks) the tails whip it at what he looks at ({@link #aim}: a lob that lands where he aims), in the final kakuja
 * as {@link #KAKUJA_PIECES} pieces. Where it lands it bursts ({@link #radius}, {@link #falloff}): hurt, knocked back and a moment
 * stunned, and a full one craters the ground.
 */
public final class RinkakuHurl {
    private RinkakuHurl() {
    }

    /** How far he can tear a chunk from (blocks from his eyes), and how far before his feet the ground is taken otherwise. */
    public static final double DIG_REACH = 7.0;
    public static final double GROUND_AHEAD = 1.8;
    /** At most this many tails grip it. */
    public static final int TAILS = 2;
    /** Ticks it takes to come up out of the ground into his tails' grip; a tap still lifts it this long before it flies. */
    public static final int RISE = 6;
    /** Ticks held to tear the biggest chunk. */
    public static final int CHARGE = 30;
    /** It goes by itself after this many ticks held. */
    public static final int MOST_HELD = 80;
    /** How long a press or a held ping keeps it held (ticks): the client pings every 4 while the key is down. */
    public static final int HELD = 10;
    /** Pulled down this much a tick in flight (blocks a tick, per tick). */
    public static final double GRAVITY = 0.045;
    /** It bursts by itself after this many ticks in flight. */
    public static final int FLIGHT = 60;
    /** It is aimed at what he looks at up to this far. */
    public static final double AIM_RANGE = 48.0;
    /** RC it costs, and ticks before the next (from when the last piece bursts; the original's cooldown scaling on top). */
    public static final float COST = 1.6F;
    public static final int COOLDOWN = 44;
    /** The final kakuja tears it into this many pieces, thrown this far apart (degrees). */
    public static final int KAKUJA_PIECES = 3;
    public static final double SPREAD = 8.0;
    /** Nothing harder than this is taken out of the world (stone 1.5, deepslate and ores 3; obsidian 50 is not); a harder block still gives a chunk. */
    public static final float HARDEST = 3.0F;
    /** A burst this charged or more also craters the ground where it lands. */
    public static final double CRATERS = 0.75;

    /** How charged it is {@code held} ticks after it was torn out (0 .. 1). */
    public static double charge(double held) {
        return clamp01(held / CHARGE);
    }

    /** How big it is across (blocks) at {@code charge}: 0.85 to 2; {@code pieces} pieces each a little smaller. */
    public static double size(double charge, int pieces) {
        double whole = 0.85 + 1.15 * clamp01(charge);
        return pieces > 1 ? whole * 0.75 : whole;
    }

    /** How fast it is thrown (blocks a tick): a big one a little slower. */
    public static double speed(double charge) {
        return 1.9 - 0.5 * clamp01(charge);
    }

    /** How far its burst reaches (blocks), {@code size} across. */
    public static double radius(double size) {
        return 1.4 + 1.1 * Math.max(0.0, size);
    }

    /** What its burst does at its middle, of the tail's swipe damage, at {@code charge}. */
    public static double power(double charge) {
        return 1.0 + 1.0 * clamp01(charge);
    }

    /** How much of its burst reaches {@code distance} from its middle, {@code radius} its reach: all at the middle, 40% at the edge, none past. */
    public static double falloff(double distance, double radius) {
        if (!(distance >= 0.0) || distance > radius) {
            return 0.0;
        }
        return 1.0 - 0.6 * (distance / Math.max(1.0E-9, radius));
    }

    /** How hard its burst throws a body (knockback strength) and how high, and how long it stuns (ticks), at {@code charge}. */
    public static double knock(double charge) {
        return 0.55 + 0.65 * clamp01(charge);
    }

    public static double lift(double charge) {
        return 0.25 + 0.15 * clamp01(charge);
    }

    public static int stun(double charge) {
        return (int)Math.round(10 + 18 * clamp01(charge));
    }

    /**
     * Where it hangs in his tails' grip: before and above his eyes ({@code eye}), {@code forwardX}/{@code forwardZ} his level facing (a
     * unit vector), {@code size} across: in his own view the top of his sight.
     */
    public static double[] hover(double[] eye, double forwardX, double forwardZ, double size) {
        double up = 0.55 + 0.45 * size;
        double out = 1.0 + 0.5 * size;
        return new double[] {eye[0] + forwardX * out, eye[1] + up, eye[2] + forwardZ * out};
    }

    /** Where it is {@code age} ticks after it was torn out from {@code from}, rising to {@code to}: eased, a little arc over the way. */
    public static double[] rising(double[] from, double[] to, double age) {
        double t = clamp01(age / RISE);
        double e = t * t * (3.0 - 2.0 * t);
        double arc = Math.sin(Math.PI * t) * 0.5;
        return new double[] {from[0] + (to[0] - from[0]) * e, from[1] + (to[1] - from[1]) * e + arc, from[2] + (to[2] - from[2]) * e};
    }

    /**
     * Its velocity (blocks a tick) thrown from {@code from} at about {@code speed} so that, falling as {@link #step} has it, a tick of
     * its flight ends exactly on {@code to}: straight at it, raised just enough to make up for the fall on the way.
     */
    public static double[] aim(double[] from, double[] to, double speed) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        // A whole number of ticks, so that a tick of its flight ends exactly on the mark.
        double ticks = Math.max(1.0, Math.round(distance / Math.max(1.0E-3, speed)));
        return new double[] {dx / ticks, dy / ticks + GRAVITY * (ticks - 1.0) / 2.0, dz / ticks};
    }

    /** {@code velocity} turned {@code degrees} about the vertical (a piece of the kakuja's throw); a new array. */
    public static double[] spread(double[] velocity, double degrees) {
        double a = Math.toRadians(degrees);
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[] {velocity[0] * c - velocity[2] * s, velocity[1], velocity[0] * s + velocity[2] * c};
    }

    /** A tick of flight: {@code pos} on by {@code velocity}, then {@code velocity} pulled down by {@link #GRAVITY} (both updated in place). */
    public static void step(double[] pos, double[] velocity) {
        pos[0] += velocity[0];
        pos[1] += velocity[1];
        pos[2] += velocity[2];
        velocity[1] -= GRAVITY;
    }

    /**
     * True if it, {@code radius} round, going from {@code a} to {@code b} this tick, meets a body standing at {@code x}/{@code z} from
     * {@code y0} (its feet) to {@code y1} (its head), {@code half} its half width.
     */
    public static boolean meets(double[] a, double[] b, double x, double y0, double y1, double z, double half, double radius) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int)Math.ceil(length / 0.2));
        double reach = half + radius;
        for (int i = 0; i <= steps; i++) {
            double f = i / (double)steps;
            double px = a[0] + dx * f;
            double py = a[1] + dy * f;
            double pz = a[2] + dz * f;
            if (Math.abs(px - x) <= reach && Math.abs(pz - z) <= reach && py >= y0 - radius && py <= y1 + radius) {
                return true;
            }
        }
        return false;
    }

    /**
     * The rubble as drawn, {@code seed} its own: cubes of the block, each {x, y, z, scale, turn} of its size across (the first its big core,
     * then smaller lumps stuck round it), {@code count} of them.
     */
    public static double[][] pieces(int seed, int count) {
        int n = Math.max(1, count);
        double[][] out = new double[n][];
        out[0] = new double[] {0.0, 0.0, 0.0, 0.74, 0.0};
        long s = seed * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        for (int i = 1; i < n; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            double a = ((s >>> 11) & 0xFFFF) / 65536.0 * Math.PI * 2.0;
            s = s * 6364136223846793005L + 1442695040888963407L;
            double b = ((s >>> 11) & 0xFFFF) / 65536.0 * 2.0 - 1.0;
            s = s * 6364136223846793005L + 1442695040888963407L;
            double k = ((s >>> 11) & 0xFFFF) / 65536.0;
            double flat = Math.sqrt(Math.max(0.0, 1.0 - b * b));
            double reach = 0.34;
            out[i] = new double[] {Math.cos(a) * flat * reach, b * reach * 0.8, Math.sin(a) * flat * reach, 0.34 + 0.16 * k, k * 90.0};
        }
        return out;
    }

    /** How many lumps a chunk {@code size} across is drawn with: more the bigger. */
    public static int lumps(double size) {
        return 4 + (int)Math.floor(Math.max(0.0, size - 0.85) * 2.0);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
