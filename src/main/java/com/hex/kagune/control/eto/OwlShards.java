package com.hex.kagune.control.eto;

import com.hex.kagune.control.ayato.UkakuModel;

/**
 * 6.2.0, the Owl's spikes (6.2.4: J held in it; pure, tested, the same on the server and every client): its head lowered, its crest
 * flared, it fires crystal spikes off its crest at what she looks at, a burst every {@link #STORM_EVERY} ticks for as long as it is
 * held (and her RC lasts), out to {@link #STORM_MOST}. They fly hard and straight (dropping a little), run through what they hit and
 * carry it along their flight until they strike a wall or the ground ({@link #PIN_CARRY} at the most) and pin it there ({@link
 * #PINNED} ticks); one that hits nothing sticks where it lands, then crumbles.
 *
 * <p>Points are in her frame (blocks to her right, up from her feet, ahead of her) where the server places the bursts; the spikes
 * themselves fly in the world.
 */
public final class OwlShards {
    /** When the first two bursts let go (ticks into the move: its head lowered under its flaring crest first). */
    public static final int FIRST_RIGHT = 6;
    public static final int FIRST_LEFT = 8;
    /** Held, a throw every so many ticks after those, the arms turn about; the last no later than this. */
    public static final int STORM_EVERY = 3;
    public static final int STORM_MOST = 60;
    /** After the last throw, back into her stance (ticks). */
    public static final int RECOVER = 9;
    /** Spears a throw: the tap's two, then each of the storm's. */
    public static final int TAP_SPEARS = 3;
    public static final int STORM_SPEARS = 3;
    /** Radians between the spears of one throw. */
    public static final double FAN = 0.07;
    /** Its flight: blocks a tick, the drop (blocks a tick, each tick), and the longest it flies (ticks) before it falls spent. */
    public static final double SPEED = 3.0;
    public static final double GRAVITY = 0.02;
    public static final int LIFE = 14;
    /** How far (blocks) a spear carries what it runs through before it pins it (to a wall sooner; to the ground at the most). */
    public static final double PIN_CARRY = 5.0;
    /** Ticks what it pins is held there; ticks a spear stays stuck where it hit before it crumbles. */
    public static final int PINNED = 30;
    public static final int STUCK = 60;
    /** Each spear's hurt, times a fist; the RC each throw of the storm costs after the first two. */
    public static final float DAMAGE = 0.8F;
    public static final float STORM_COST = 1.5F;
    /** A spear's length (blocks). */
    public static final double LENGTH = 2.2;
    /** The farthest (blocks) she aims at: where she looks, or this far along it. */
    public static final double AIM = 24.0;

    private OwlShards() {
    }

    /** When throw {@code k} (from 0) lets go: the right arm's, the left's, then turn about every {@link #STORM_EVERY}. */
    public static int throwAt(int k) {
        return k <= 0 ? FIRST_RIGHT : k == 1 ? FIRST_LEFT : FIRST_LEFT + (k - 1) * STORM_EVERY;
    }

    /** Which arm makes throw {@code k} (1 her right, -1 her left). */
    public static int side(int k) {
        return (k & 1) == 0 ? 1 : -1;
    }

    /** How many spears throw {@code k} hurls. */
    public static int spears(int k) {
        return k < 2 ? TAP_SPEARS : STORM_SPEARS;
    }

    /**
     * How many throws are made in all: a tap's two; held, every one that lets go before she lets go ({@code letGo} ticks in; 0 while
     * still held: as many as there could be), never past {@link #STORM_MOST}.
     */
    public static int throwsMade(boolean held, double letGo) {
        if (!held) {
            return 2;
        }
        int k = 2;
        while (throwAt(k) <= STORM_MOST && (letGo <= 0.0 || throwAt(k) < letGo)) {
            k++;
        }
        return k;
    }

    /** When the move is over (ticks in): its last throw, and back into her stance after it. */
    public static int ends(boolean held, double letGo) {
        return throwAt(throwsMade(held, letGo) - 1) + RECOVER;
    }

    /** Where burst {@code k} leaves (her frame): off the creature's crest, high on its back behind its head, from across it in turn. */
    public static double[] release(int k) {
        double[] across = {-0.6, 0.6, 0.0, -0.35, 0.35};
        return new double[] {across[Math.floorMod(k, across.length)], 4.6, -0.5};
    }

    /**
     * The way spear {@code j} of throw {@code k} goes (the world), thrown at {@code at} from {@code from}: fanned across (about the
     * upright), a hair up (it drops), the storm's a little scattered.
     */
    public static double[] direction(double[] from, double[] at, int k, int j) {
        double[] d = normalize(new double[] {at[0] - from[0], at[1] - from[1], at[2] - from[2]});
        int n = spears(k);
        double across = FAN * (j - (n - 1) * 0.5);
        double lift = 0.025;
        if (k >= 2) {
            // The storm's scatter: the same on every side (from the throw and the spear alone).
            across += 0.05 * Math.sin(k * 2.39 + j * 1.71);
            lift += 0.03 * Math.sin(k * 1.93 + j * 2.87);
        }
        double c = Math.cos(across);
        double s = Math.sin(across);
        double[] turned = {d[0] * c + d[2] * s, d[1], -d[0] * s + d[2] * c};
        turned[1] += lift;
        return normalize(turned);
    }

    /** A spear thrown from {@code p} at {@code v} (blocks a tick), {@code t} ticks on. */
    public static double[] at(double[] p, double[] v, double t) {
        return new double[] {p[0] + v[0] * t, p[1] + v[1] * t - 0.5 * GRAVITY * t * t, p[2] + v[2] * t};
    }

    /** Its speed then (blocks a tick). */
    public static double[] velocity(double[] v, double t) {
        return new double[] {v[0], v[1] - GRAVITY * t, v[2]};
    }

    // ------------------------------------------------------------------ the arms throwing

    /** A throw's arm, about its letting go (ticks from it): reared back over its shoulder, whipped through, followed through, ready. */
    public static final double WIND_FROM = -3.0;
    public static final double WOUND = -1.5;
    public static final double THROUGH = 1.2;
    public static final double READY = 3.0;

    /**
     * The throw {@code side}'s arm is in at {@code age} ticks into the move (held, let go at {@code letGo}): its index, or -1 when it
     * is between throws (ready). An arm's throws are {@code 2 * STORM_EVERY} apart: the windows never overlap.
     */
    public static int throwOf(int side, double age, boolean held, double letGo) {
        int made = throwsMade(held, letGo);
        for (int k = side > 0 ? 0 : 1; k < made; k += 2) {
            double p = age - throwAt(k);
            if (p >= WIND_FROM && p < READY) {
                return k;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ how they look

    /**
     * One spike into {@code sink}: from its butt {@code tail} (rig space) along {@code dir}, {@code whole} of it there (1 whole; less as
     * it crumbles): a crystal off the crest, four-sided, widest a third of the way along, its butt burning red, its point white.
     */
    public static void mesh(UkakuModel.Sink sink, double[] tail, double[] dir, double whole, int seed) {
        double w = Math.max(0.0, Math.min(1.0, whole));
        if (w <= 0.02) {
            return;
        }
        double[] d = normalize(dir);
        double[] a = normalize(Math.abs(d[1]) < 0.9 ? OwlLances.cross(d, new double[] {0.0, 1.0, 0.0}) : OwlLances.cross(d, new double[] {1.0, 0.0, 0.0}));
        double[] b = OwlLances.cross(d, a);
        double length = LENGTH * w;
        double turn = (seed % 7) * 0.4;
        double[][] ring0 = ring(tail, a, b, 0.07 * w, turn);
        double[] mid = {tail[0] + d[0] * length * 0.32, tail[1] + d[1] * length * 0.32, tail[2] + d[2] * length * 0.32};
        double[][] ring1 = ring(mid, a, b, 0.2 * w, turn);
        double[] tip = {tail[0] + d[0] * length, tail[1] + d[1] * length, tail[2] + d[2] * length};
        int red = OwlModel.glow(0xFFE81E30);
        int deep = OwlModel.glow(0xFFB0101E);
        int white = 0xFFF2E6E8;
        int pale = 0xFFD8C2C6;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            double[] n0 = normalize(add(ring1[i], ring1[j], mid, -2.0));
            sink.quad(ring0[i], ring0[j], ring1[j], ring1[i], n0, deep, deep, red, red);
            double[] n1 = normalize(add(ring1[i], ring1[j], tip, -2.0));
            int c = i % 2 == 0 ? white : pale;
            sink.quad(ring1[i], ring1[j], tip, tip, n1, red, red, c, c);
        }
    }

    /** Four corners round {@code c} across {@code a} and {@code b}, {@code r} out, turned {@code turn}. */
    static double[][] ring(double[] c, double[] a, double[] b, double r, double turn) {
        double[][] out = new double[4][];
        for (int i = 0; i < 4; i++) {
            double t = turn + i * Math.PI / 2.0;
            double ca = Math.cos(t) * r;
            double cb = Math.sin(t) * r;
            out[i] = new double[] {c[0] + a[0] * ca + b[0] * cb, c[1] + a[1] * ca + b[1] * cb, c[2] + a[2] * ca + b[2] * cb};
        }
        return out;
    }

    /** {@code p + q + k * c} (the way out of the face between {@code p} and {@code q}, from {@code c}: {@code k} -2). */
    static double[] add(double[] p, double[] q, double[] c, double k) {
        return new double[] {p[0] + q[0] + k * c[0], p[1] + q[1] + k * c[1], p[2] + q[2] + k * c[2]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-9 ? new double[] {0.0, 0.0, 1.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
