package com.hex.kagune.control.ukaku;

/**
 * A needle of Ukaku's first kakuja (pure, tested): one of the spikes on a tentacle's belly, fired. Much thinner and
 * sharper than the crystal mode's spikes ({@link CrystalSpike}) and in the kakuja's own magenta: a four-sided shaft drawn
 * out into a long point, a short dark heel, the point white-hot pink. In flight a faint streak runs behind it. Stuck in a
 * body it glows hotter as its burst comes ({@code heat}) and trembles.
 *
 * <p>Coordinates are whatever space {@code tip}, {@code direction} and {@code eye} share (the renderer uses camera-relative
 * world blocks). {@code size} 1 is {@link #LENGTH} blocks long.
 */
public final class KakujaNeedle {
    /** Length of a size-1 needle, heel to point (blocks), and its widest radius. */
    public static final double LENGTH = 0.62;
    public static final double RADIUS = 0.03;
    /** Share of the length that is the point, and the heel. */
    static final double POINT = 0.68;
    static final double HEEL = 0.1;
    /** Quads one needle costs at most (the four faces of each part, the streak, the glow), for budgets. */
    public static final int QUADS = 4 * 3 + 2 + 2;

    static final int HEEL_COLOUR = 0x3A021C;
    static final int SHAFT = 0xB80E55;
    static final int POINT_COLOUR = 0xFF5FA6;
    static final int WHITE = 0xFFE4F2;

    private KakujaNeedle() {
    }

    /**
     * The needle with its point at {@code tip}, pointing along {@code direction} (any length), {@code size} as above, seen
     * from {@code eye}. {@code streak} (0..1) is the flight's trail, {@code heat} (0..1) how close a stuck needle is to
     * bursting, {@code time} seconds (the tremble), {@code seed} varies it, {@code fine} false for the cheap far version,
     * {@code fade} (0..1) dims it.
     */
    public static void build(RcShard.Sink sink, double[] tip, double[] direction, double size, double[] eye, double streak, double heat, double time,
                             int seed, boolean fine, double fade) {
        double alpha = clamp01(fade);
        if (alpha <= 0.003 || size <= 0.0) {
            return;
        }
        double[] axis = normalize(direction);
        double hot = clamp01(heat);
        if (hot > 0.0) {
            double shake = 0.008 * size * hot * hot;
            tip = add(tip, new double[] {Math.sin(time * 71.0 + seed) * shake, Math.sin(time * 59.0 + seed * 1.3) * shake,
                Math.sin(time * 67.0 + seed * 0.7) * shake});
        }
        double length = LENGTH * size;
        double radius = RADIUS * Math.sqrt(size) * (0.9 + 0.2 * UkakuModel.hash(seed + 1));
        double[] helper = Math.abs(axis[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        double[] a1 = normalize(cross(axis, helper));
        double[] a2 = cross(axis, a1);
        double twist = UkakuModel.hash(seed + 2) * Math.PI;
        double[] back = add(tip, mul(axis, -length));
        double[] shoulder = add(tip, mul(axis, -length * POINT));
        double[] heel = add(back, mul(axis, length * HEEL));
        double[][] ring = new double[4][];
        double[][] rear = new double[4][];
        for (int k = 0; k < 4; k++) {
            double angle = twist + Math.PI / 2.0 * k;
            // A flattened diamond: wider one way than the other, like a blade.
            double[] offset = add(mul(a1, Math.cos(angle) * radius), mul(a2, Math.sin(angle) * radius * 0.62));
            ring[k] = add(shoulder, offset);
            rear[k] = add(heel, mul(offset, 0.85));
        }
        int heelColour = argb(mix(HEEL_COLOUR, 0xFFFFFF, 0.5 * hot), alpha);
        int shaft = argb(mix(SHAFT, 0xFFFFFF, 0.45 * hot), alpha);
        int point = argb(mix(POINT_COLOUR, 0xFFFFFF, 0.5 * hot), alpha);
        int white = argb(WHITE, alpha);
        for (int k = 0; k < 4; k++) {
            int n = (k + 1) % 4;
            // Two faces catch the light: the point goes white on them.
            boolean lit = k == 0 || k == 1;
            sink.quad(ring[k], ring[n], tip, tip, shaft, shaft, lit ? white : point, lit ? white : point);
            sink.quad(rear[k], rear[n], ring[n], ring[k], heelColour, heelColour, shaft, shaft);
            if (fine) {
                sink.quad(back, back, rear[n], rear[k], heelColour, heelColour, heelColour, heelColour);
            }
        }
        if (streak > 0.01) {
            // A faint streak behind it, flat to the camera.
            double[] view = normalize(sub(eye, add(tip, mul(axis, -length * 0.5))));
            double[] across = cross(axis, view);
            if (dot(across, across) < 1.0E-8) {
                across = a1;
            }
            across = normalize(across);
            double w = radius * 1.6;
            double[] end = add(back, mul(axis, -length * 1.6 * clamp01(streak)));
            int glow = argb(SHAFT, alpha * 0.4 * clamp01(streak));
            int clear = argb(SHAFT, 0.0);
            sink.quad(add(back, mul(across, w)), add(end, mul(across, w * 0.2)), add(end, mul(across, -w * 0.2)), add(back, mul(across, -w)), glow,
                clear, clear, glow);
            int core = argb(POINT_COLOUR, alpha * 0.35 * clamp01(streak));
            sink.quad(add(shoulder, mul(across, w * 0.5)), add(back, mul(across, w * 0.35)), add(back, mul(across, -w * 0.35)),
                add(shoulder, mul(across, -w * 0.5)), core, clear, clear, core);
        }
    }

    static int argb(int rgb, double alpha) {
        return UkakuModel.argb(rgb, alpha);
    }

    static int mix(int from, int to, double amount) {
        return UkakuModel.mixInt(from, to, amount);
    }

    static double clamp01(double value) {
        return UkakuModel.clamp01(value);
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-12 ? new double[] {0.0, 0.0, 1.0} : mul(a, 1.0 / length);
    }
}
