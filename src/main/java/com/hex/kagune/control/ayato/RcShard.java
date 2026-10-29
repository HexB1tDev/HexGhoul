package com.hex.kagune.control.ayato;

/**
 * A shard of Ayato's RC fire in flight (pure, tested): what an M1 shot or an ender's fireball looks like. It is a piece of
 * the wing torn off and thrown, built from the wing's own flame: a white-hot spindle at the head, then five licking flame
 * tongues streaming behind it from molten orange through red to crimson, a red vein through the middle tongue, and the
 * pink aura around it all. Everything glows (full bright, upward normals) and faces the camera like the wing's fire.
 *
 * <p>Coordinates are whatever space {@code head}, {@code direction} and {@code eye} share (the renderer uses
 * camera-relative world blocks). {@code size} 1 is an M1 shot (about 0.8 blocks long); the Flare's fireball is larger.
 */
public final class RcShard {
    /** One quad: four corners and four ARGB colours. */
    public interface Sink {
        void quad(double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd);
    }

    static final int CORE = 0xFFF6D8;
    static final int MOLTEN = 0xFFB02A;
    static final int FIRE = 0xFF3C12;
    static final int RED = 0xE8121C;
    static final int CRIMSON = 0x8E0A1E;
    static final int AURA = 0xF2A2F0;
    static final int VEIN = 0xFF2236;
    /** Length of a size-1 shard, head to the end of its longest tongue (blocks). */
    public static final double LENGTH = 0.85;
    /** Quads one shard costs (for budgets). */
    public static final int QUADS = 2 * 4 + 5 * 6 + 6 + 6;

    private RcShard() {
    }

    /**
     * The shard with its head at {@code head}, flying along {@code direction} (any length), {@code time} in seconds for the
     * flicker, {@code seed} to vary it, seen from {@code eye}. {@code fade} (0..1) dims it (bursting, burning out).
     */
    public static void build(Sink sink, double[] head, double[] direction, double size, double time, int seed, double[] eye, double fade) {
        double[] dir = normalize(direction);
        double alpha = Math.max(0.0, Math.min(1.0, fade));
        if (alpha <= 0.003 || size <= 0.0) {
            return;
        }
        double length = LENGTH * size;
        double[] view = normalize(sub(eye, head));
        // Two axes across the flight line: one facing the camera (the ribbons' width), one up out of that plane.
        double[] across = cross(dir, view);
        if (dot(across, across) < 1.0E-8) {
            across = cross(dir, new double[] {0.0, 1.0, 0.0});
            if (dot(across, across) < 1.0E-8) {
                across = new double[] {1.0, 0.0, 0.0};
            }
        }
        across = normalize(across);
        double[] lift = normalize(cross(across, dir));

        // The head: a white-hot spindle in two crossed planes, pointed at the front.
        double headLength = 0.28 * size;
        double headWidth = 0.09 * size;
        for (double[] side : new double[][] {across, lift}) {
            double[] tip = add(head, mul(dir, headLength * 0.45));
            double[] tail = add(head, mul(dir, -headLength * 0.55));
            double[] l = add(head, mul(side, headWidth));
            double[] r = add(head, mul(side, -headWidth));
            int hot = argb(CORE, alpha);
            int edge = argb(MOLTEN, 0.0);
            sink.quad(tip, l, head, r, argb(CORE, alpha * 0.9), edge, hot, edge);
            sink.quad(head, l, tail, r, hot, edge, argb(MOLTEN, alpha * 0.6), edge);
            // A soft molten halo around the head.
            double[] l2 = add(head, mul(side, headWidth * 2.2));
            double[] r2 = add(head, mul(side, -headWidth * 2.2));
            double[] front = add(head, mul(dir, headLength * 0.8));
            double[] back = add(head, mul(dir, -headLength));
            int glow = argb(FIRE, alpha * 0.45);
            int clear = argb(RED, 0.0);
            sink.quad(front, l2, head, r2, clear, clear, glow, clear);
            sink.quad(head, l2, back, r2, glow, clear, clear, clear);
        }

        // Five flame tongues streaming back from the head, licking from side to side.
        int stations = 6;
        for (int tongue = 0; tongue < 5; tongue++) {
            double phase = seed * 1.7 + tongue * 2.39;
            double reach = length * (tongue == 2 ? 1.0 : 0.55 + 0.3 * hash(seed * 31 + tongue));
            double spread = (tongue - 2) * 0.11;
            double width = headWidth * (tongue == 2 ? 1.25 : 0.85);
            double[] previousCentre = null;
            double[] previousLeft = null;
            double[] previousRight = null;
            int previousColour = 0;
            for (int station = 0; station <= stations; station++) {
                double t = station / (double)stations;
                double back = reach * t;
                double wave = Math.sin(time * 9.0 - t * 5.0 + phase) * 0.1 * t + spread * t;
                double rise = Math.sin(time * 6.5 - t * 4.0 + phase * 1.3) * 0.12 * t;
                double[] centre = add(add(add(head, mul(dir, -back - headLength * 0.3)), mul(across, wave * length)), mul(lift, rise * length));
                double half = width * (t < 0.2 ? 0.7 + 1.5 * t : (1.0 - t) * 1.25);
                double[] left = add(centre, mul(across, half));
                double[] right = add(centre, mul(across, -half));
                int colour = argb(t < 0.25 ? mix(MOLTEN, FIRE, t / 0.25) : t < 0.6 ? mix(FIRE, RED, (t - 0.25) / 0.35) : mix(RED, CRIMSON, (t - 0.6) / 0.4),
                    alpha * (1.0 - t * t) * (tongue == 2 ? 0.95 : 0.75));
                if (previousCentre != null) {
                    sink.quad(previousLeft, left, right, previousRight, previousColour, colour, colour, previousColour);
                }
                previousCentre = centre;
                previousLeft = left;
                previousRight = right;
                previousColour = colour;
            }
        }

        // A red vein along the middle tongue, lifted toward the camera so it isn't cut into dashes.
        double[] previous = null;
        for (int station = 0; station <= 6; station++) {
            double t = station / 6.0;
            double back = length * 0.85 * t;
            double wave = Math.sin(time * 9.0 - t * 5.0 + seed * 1.7 + 2 * 2.39) * 0.1 * t;
            double[] centre = add(add(add(head, mul(dir, -back - headLength * 0.3)), mul(across, wave * length)), mul(view, 0.03 * size));
            if (previous != null) {
                double w = 0.012 * size;
                int c = argb(VEIN, alpha * (1.0 - t));
                sink.quad(add(previous, mul(across, w)), add(centre, mul(across, w)), add(centre, mul(across, -w)), add(previous, mul(across, -w)),
                    c, c, c, c);
            }
            previous = centre;
        }

        // The pink aura: one soft glow around the whole shard.
        double auraBack = length * 0.7;
        double auraWidth = headWidth * 3.2;
        double[] front = add(head, mul(dir, headLength));
        double[] middle = add(head, mul(dir, -auraBack * 0.35));
        double[] end = add(head, mul(dir, -auraBack));
        int aura = argb(AURA, alpha * 0.22);
        int none = argb(AURA, 0.0);
        double[] l = add(middle, mul(across, auraWidth));
        double[] r = add(middle, mul(across, -auraWidth));
        sink.quad(front, l, middle, r, none, none, aura, none);
        sink.quad(middle, l, end, r, aura, none, none, none);
        double[] lu = add(middle, mul(lift, auraWidth));
        double[] ld = add(middle, mul(lift, -auraWidth));
        sink.quad(front, lu, middle, ld, none, none, aura, none);
        sink.quad(middle, lu, end, ld, aura, none, none, none);
        double[] hl = add(head, mul(across, auraWidth * 0.6));
        double[] hr = add(head, mul(across, -auraWidth * 0.6));
        int warm = argb(FIRE, alpha * 0.25);
        sink.quad(front, hl, head, hr, none, none, warm, none);
        sink.quad(head, hl, middle, hr, warm, none, none, none);
    }

    static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double)(1L << 53);
    }

    static int argb(int rgb, double alpha) {
        int a = (int)Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0);
        return a << 24 | (rgb & 0xFFFFFF);
    }

    static int mix(int from, int to, double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        int r = (int)Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int)Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int)Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return r << 16 | g << 8 | b;
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-12 ? new double[] {0.0, 0.0, 1.0} : mul(a, 1.0 / length);
    }
}
