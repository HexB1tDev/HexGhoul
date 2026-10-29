package com.hex.kagune.control.ukaku;

/**
 * 5.0.15: the crystal thorns, Ukaku's answer to Rinkaku's grab (pure, tested). An Ukaku in the crystal mode held by a Rinkaku's
 * tails (or turning the crystal mode on while they hold him) grows dozens of long crystal spikes out of every side of his body,
 * through the tails wrapped round him, and {@link #RELEASE_TICKS} later the kagune holding him lets go. The thorns stay out a
 * moment after he is free ({@link #LINGER_TICKS}): a grab that lands on them then lets go after only {@link #REGRAB_TICKS}.
 * When the moment is over they sink back into him; if the crystal thaws (V again, the kagune going in) they shatter.
 *
 * <p>{@link Eruption} is the server's timing. {@link #thorns} lays the spikes out on his body for the clients, in the body's own
 * frame (x to his right, y up from the feet, z forward) for a body {@link #HEIGHT} tall; each is drawn as a {@link CrystalSpike}.
 */
public final class CrystalThorns {
    /** The thorns are out this long before the kagune holding him lets go: one second. */
    public static final int RELEASE_TICKS = 20;
    /** Grabbed again while the thorns are still out, the kagune lets go this soon. */
    public static final int REGRAB_TICKS = 6;
    /** They stay out this long after he is free. */
    public static final int LINGER_TICKS = 16;
    /** Each thorn snaps out over this long, the last starting up to {@link #STAGGER_TICKS} after the first. */
    public static final int GROW_TICKS = 4;
    public static final int STAGGER_TICKS = 4;
    /** They sink back in over this long. */
    public static final int SINK_TICKS = 6;
    /** How many: many. */
    public static final int COUNT = 44;
    /** The body the layout is for (blocks tall): a standing player. Scale by the body's own height. */
    public static final double HEIGHT = 1.8;
    /** How long a thorn is, as a {@link CrystalSpike} size (1 is an M1 spike, {@link CrystalSpike#LENGTH} blocks). */
    public static final double MIN_SIZE = 1.6;
    public static final double MAX_SIZE = 2.8;
    /** The back of each thorn stays this far inside him (blocks), so none shows out of his other side. */
    public static final double INSET = 0.16;
    /** The lowest a thorn points (the y of its direction): none goes straight down into the ground under him. */
    public static final double LOWEST = -0.5;
    /** The eye in the body frame, and how far the wearer's own thorns keep from it to be drawn in first person (blocks). */
    public static final double EYE_HEIGHT = 1.62;
    public static final double EYE_CLEARANCE = 0.45;
    /** How far a thorn snaps past full length before it settles (share of its length). */
    static final double OVERSHOOT = 1.2;

    private CrystalThorns() {
    }

    /** What one server tick of an {@link Eruption} does. */
    public enum Step {
        NONE,
        /** The thorns come out. */
        ERUPT,
        /** Whatever holds him lets go. */
        RELEASE,
        /** He has been free a moment: they sink back in. */
        SINK,
        /** The crystal thawed (or the kagune went in, or he died): they shatter. */
        SHATTER
    }

    /** One Ukaku's thorns on the server: when they came out, when the holder lets go, and since when he has been free. */
    public static final class Eruption {
        private long eruptedAt = Long.MIN_VALUE;
        private long releaseAt = Long.MIN_VALUE;
        private long freedAt = Long.MIN_VALUE;

        /** True while the thorns are out. */
        public boolean out() {
            return this.eruptedAt != Long.MIN_VALUE;
        }

        /**
         * One server tick at game time {@code now}: {@code crystal} the crystal mode is on (with the wings out and him alive),
         * {@code held} some kagune holds him.
         */
        public Step tick(boolean crystal, boolean held, long now) {
            if (!crystal) {
                if (out()) {
                    clear();
                    return Step.SHATTER;
                }
                return Step.NONE;
            }
            if (held) {
                Step step = Step.NONE;
                if (!out()) {
                    this.eruptedAt = now;
                    this.releaseAt = now + RELEASE_TICKS;
                    this.freedAt = Long.MIN_VALUE;
                    step = Step.ERUPT;
                } else if (this.freedAt != Long.MIN_VALUE) {
                    // Grabbed again while they are still out: the grab closes on the thorns.
                    this.freedAt = Long.MIN_VALUE;
                    this.releaseAt = now + REGRAB_TICKS;
                }
                if (step == Step.NONE && now >= this.releaseAt) {
                    this.freedAt = now;
                    return Step.RELEASE;
                }
                return step;
            }
            if (out()) {
                if (this.freedAt == Long.MIN_VALUE) {
                    this.freedAt = now; // let go some other way
                }
                if (now - this.freedAt >= LINGER_TICKS) {
                    clear();
                    return Step.SINK;
                }
            }
            return Step.NONE;
        }

        private void clear() {
            this.eruptedAt = Long.MIN_VALUE;
            this.releaseAt = Long.MIN_VALUE;
            this.freedAt = Long.MIN_VALUE;
        }
    }

    /**
     * One thorn in the body's frame: where it leaves his body, the way it points (unit), its {@link CrystalSpike} size, how
     * many ticks after the eruption it starts out, and a seed for its cut.
     */
    public record Thorn(double[] root, double[] direction, double size, int delay, int seed) {
    }

    /**
     * {@code count} thorns for the eruption {@code seed}: spread evenly round him (a Fibonacci sphere cut off below
     * {@link #LOWEST}, turned and jittered by the seed), each leaving his body where it faces: the ones pointing up from his
     * shoulders and head, the level ones from his chest and back, the low ones from his hips and legs.
     */
    public static Thorn[] thorns(int seed, int count) {
        Thorn[] thorns = new Thorn[Math.max(0, count)];
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        double spin = hash(seed) * Math.PI * 2.0;
        for (int i = 0; i < thorns.length; i++) {
            int key = seed * 131 + i * 7;
            double rise = 1.0 - (i + 0.5) / thorns.length * (1.0 - LOWEST);
            rise = clamp(rise + (hash(key + 1) - 0.5) * 0.18, LOWEST, 0.97);
            double angle = spin + i * golden + (hash(key + 2) - 0.5) * 0.45;
            double ring = Math.sqrt(1.0 - rise * rise);
            double[] direction = {Math.sin(angle) * ring, rise, Math.cos(angle) * ring};
            double height = clamp(1.05 + rise * 0.7 + (hash(key + 3) - 0.5) * 0.3, 0.25, 1.72);
            double[] root;
            if (ring < 0.25) {
                root = new double[] {0.0, Math.max(height, 1.7), 0.0}; // straight up out of the crown
            } else {
                double ux = direction[0] / ring;
                double uz = direction[2] / ring;
                double[] half = halfBody(height);
                double s = 1.0 / Math.sqrt(ux * ux / (half[0] * half[0]) + uz * uz / (half[1] * half[1]));
                root = new double[] {ux * s, height, uz * s};
            }
            double size = MIN_SIZE + (MAX_SIZE - MIN_SIZE) * hash(key + 4);
            int delay = Math.min(STAGGER_TICKS, (int)(hash(key + 5) * (STAGGER_TICKS + 1)));
            thorns[i] = new Thorn(root, direction, size, delay, (seed * 31 + i) & 0xFFFF);
        }
        return thorns;
    }

    /** His body's half width (x) and half depth (z) at {@code height}: the legs, the chest, the head. */
    static double[] halfBody(double height) {
        if (height > 1.45) {
            return new double[] {0.22, 0.22};
        }
        if (height < 0.75) {
            return new double[] {0.22, 0.12};
        }
        return new double[] {0.25, 0.14};
    }

    /**
     * How far out a thorn is (0 in, 1 full; a little past 1 as it snaps out), {@code age} ticks after the eruption (partial
     * included) and {@code sunk} ticks after they started sinking back (NaN while they are out).
     */
    public static double out(Thorn thorn, double age, double sunk) {
        double t = (age - thorn.delay()) / GROW_TICKS;
        if (t <= 0.0) {
            return 0.0;
        }
        double amount = 1.0;
        if (t < 1.0) {
            double u = t - 1.0;
            amount = 1.0 + (OVERSHOOT + 1.0) * u * u * u + OVERSHOOT * u * u;
        }
        if (!Double.isNaN(sunk)) {
            double back = clamp(sunk / SINK_TICKS, 0.0, 1.0);
            amount *= 1.0 - back * back * (3.0 - 2.0 * back);
        }
        return Math.max(0.0, amount);
    }

    /** The thorn's point (body frame) when {@code out} of the way out. */
    public static double[] tip(Thorn thorn, double out) {
        double reach = CrystalSpike.LENGTH * thorn.size() * Math.max(0.0, out);
        double[] r = thorn.root();
        double[] d = thorn.direction();
        return new double[] {r[0] + d[0] * reach, r[1] + d[1] * reach, r[2] + d[2] * reach};
    }

    /** The {@link CrystalSpike} size to draw the thorn at when {@code out}: what shows of it, and {@link #INSET} inside him. */
    public static double drawnSize(Thorn thorn, double out) {
        return (CrystalSpike.LENGTH * thorn.size() * Math.max(0.0, out) + INSET) / CrystalSpike.LENGTH;
    }

    /**
     * True if the thorn, fully out (and its overshoot), stays {@link #EYE_CLEARANCE} from the eye: only those of the wearer's
     * own thorns are drawn in first person, so none fills his view.
     */
    public static boolean clearOfEye(Thorn thorn) {
        double[] a = thorn.root();
        double[] b = tip(thorn, 1.1);
        double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double[] ae = {-a[0], EYE_HEIGHT - a[1], -a[2]};
        double length = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        double t = length < 1.0E-9 ? 0.0 : clamp((ae[0] * ab[0] + ae[1] * ab[1] + ae[2] * ab[2]) / length, 0.0, 1.0);
        double dx = a[0] + ab[0] * t;
        double dy = a[1] + ab[1] * t - EYE_HEIGHT;
        double dz = a[2] + ab[2] * t;
        return dx * dx + dy * dy + dz * dz >= EYE_CLEARANCE * EYE_CLEARANCE;
    }

    static double clamp(double value, double low, double high) {
        return value < low ? low : value > high ? high : value;
    }

    /** A stable pseudo-random number in [0, 1) for {@code n}. */
    static double hash(int n) {
        int h = n * 0x27D4EB2D;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFFFF) / (double)0x1000000;
    }
}
