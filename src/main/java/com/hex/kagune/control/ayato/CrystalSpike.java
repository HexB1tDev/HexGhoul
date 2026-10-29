package com.hex.kagune.control.ayato;

/**
 * A crystal spike of Ayato's crystal mode (pure, tested): one shard of the crystallized wing, fired. A long, slightly
 * flattened six-sided crystal: a short pyramid at the back, a prism, and a long point (the point a little off true and
 * every face a different size, like the wing's own quartz). Cut stone, lit by the same key light as the wing's crystal
 * ({@link UkakuCrystal#LIGHT}), with the same inner glow, white glints that move with the camera and a pink sheen where a
 * face turns edge-on; deep crimson at the back, red through the body, pale pink at the point. A thin red glow hangs around
 * it while it flies.
 *
 * <p>Stuck in a body, the point is inside it and only the back half shows. As its burst comes ({@code heat} 0 to 1) it
 * glows hotter, white cracks open along its faces and it trembles.
 *
 * <p>Coordinates are whatever space {@code tip}, {@code direction} and {@code eye} share (the renderer uses camera-relative
 * world blocks). {@code size} 1 is an M1 spike, {@link #LENGTH} blocks long.
 */
public final class CrystalSpike {
    /** Length of a size-1 spike, back to point (blocks), and its widest radius. */
    public static final double LENGTH = 0.95;
    public static final double RADIUS = 0.075;
    /** Share of the length that is the point, and the back pyramid. */
    static final double POINT = 0.55;
    static final double BACK = 0.12;
    /** Quads one spike costs at most (facets, the glow, the cracks), for budgets. */
    public static final int QUADS = 6 * 3 + 4 + 3;

    static final int BACK_COLOUR = 0x5A0A36;
    static final int BODY = 0xD40E2A;
    static final int CORE = 0xFF3A4A;

    private CrystalSpike() {
    }

    /**
     * The spike with its point at {@code tip}, pointing along {@code direction} (any length), {@code size} as above, seen from
     * {@code eye}. {@code glow} (0..1) is the flight's red glow, {@code heat} (0..1) how close a stuck spike is to bursting,
     * {@code time} seconds (for the tremble), {@code seed} to vary its cut, {@code fine} false for the cheap far version,
     * {@code fade} (0..1) dims it.
     */
    public static void build(RcShard.Sink sink, double[] tip, double[] direction, double size, double[] eye, double glow, double heat, double time,
                             int seed, boolean fine, double fade) {
        double alpha = clamp01(fade);
        if (alpha <= 0.003 || size <= 0.0) {
            return;
        }
        double[] axis = normalize(direction);
        double hot = clamp01(heat);
        if (hot > 0.0) {
            // About to burst: a tremble that quickens.
            double shake = 0.012 * size * hot * hot;
            tip = add(tip, new double[] {Math.sin(time * 61.0 + seed) * shake, Math.sin(time * 53.0 + seed * 1.3) * shake,
                Math.sin(time * 67.0 + seed * 0.7) * shake});
        }
        double length = LENGTH * size;
        double radius = RADIUS * Math.sqrt(size) * (0.9 + 0.2 * hash(seed + 1));
        double[] helper = Math.abs(axis[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        double[] a1 = normalize(cross(axis, helper));
        double[] a2 = cross(axis, a1);
        int sides = fine ? 6 : 4;
        double twist = hash(seed + 2) * Math.PI;
        double flat = 0.7 + 0.25 * hash(seed + 3);
        double[] back = add(tip, scale(axis, -length));
        double[] shoulder = add(tip, scale(axis, -length * POINT));
        double[] heel = add(back, scale(axis, length * BACK));
        // The point a little off true: never two spikes alike.
        double[] point = add(tip, add(scale(a1, (hash(seed + 4) - 0.5) * radius * 0.6), scale(a2, (hash(seed + 5) - 0.5) * radius * 0.6)));
        double[][] front = new double[sides][];
        double[][] rear = new double[sides][];
        for (int s = 0; s < sides; s++) {
            double angle = twist + 2.0 * Math.PI * s / sides;
            double[] ring = add(scale(a1, Math.cos(angle) * radius), scale(a2, Math.sin(angle) * radius * flat));
            double uneven = 1.0 + 0.3 * (hash(seed * 31 + s + 11) - 0.5);
            front[s] = add(add(shoulder, scale(axis, length * 0.04 * (uneven - 1.0))), scale(ring, uneven));
            rear[s] = add(heel, scale(ring, 0.92));
        }
        double vary = 0.85 + 0.3 * hash(seed + 6);
        double burning = 0.25 * clamp01(glow) + 0.9 * hot;
        for (int s = 0; s < sides; s++) {
            int n = (s + 1) % sides;
            facet(sink, front[s], front[n], point, point, mix(BODY, UkakuCrystal.POINT, 0.55), mix(UkakuCrystal.POINT, 0xFFFFFF, 0.3), eye, vary,
                burning, alpha);
            facet(sink, rear[s], rear[n], front[n], front[s], mix(BACK_COLOUR, BODY, 0.6), BODY, eye, vary, burning, alpha);
            facet(sink, back, back, rear[n], rear[s], BACK_COLOUR, mix(BACK_COLOUR, BODY, 0.5), eye, vary, burning, alpha);
        }
        double[] view = normalize(sub(eye, add(tip, scale(axis, -length * 0.5))));
        double[] across = cross(axis, view);
        if (dot(across, across) < 1.0E-8) {
            across = a1;
        }
        across = normalize(across);
        if (glow > 0.01 || hot > 0.01) {
            // The red glow around it: two soft camera-facing diamonds, the hot core along its line.
            double g = Math.max(clamp01(glow) * 0.5, hot * 0.8);
            double width = radius * (3.2 + 2.5 * hot);
            double[] middle = add(tip, scale(axis, -length * 0.45));
            int aura = argb(hot > 0.5 ? mix(UkakuCrystal.SHEEN, 0xFFFFFF, (hot - 0.5) * 0.8) : UkakuCrystal.SHEEN, alpha * g * 0.5);
            int none = argb(UkakuCrystal.SHEEN, 0.0);
            double[] l = add(middle, scale(across, width));
            double[] r = add(middle, scale(across, -width));
            double[] ahead = add(tip, scale(axis, length * 0.15));
            double[] behind = add(back, scale(axis, -length * 0.35));
            sink.quad(ahead, l, middle, r, none, none, aura, none);
            sink.quad(middle, l, behind, r, aura, none, none, none);
            int core = argb(mix(CORE, 0xFFFFFF, hot), alpha * (0.5 * clamp01(glow) + 0.7 * hot));
            double w = radius * 0.35;
            sink.quad(add(back, scale(across, w)), add(tip, scale(across, w * 0.3)), add(tip, scale(across, -w * 0.3)), add(back, scale(across, -w)),
                core, core, core, core);
            sink.quad(add(heel, scale(across, w * 1.6)), add(shoulder, scale(across, w * 1.4)), add(shoulder, scale(across, -w * 1.4)),
                add(heel, scale(across, -w * 1.6)), argb(CORE, 0.0), argb(CORE, alpha * g * 0.4), argb(CORE, alpha * g * 0.4), argb(CORE, 0.0));
        }
        if (fine && hot > 0.25) {
            // Cracks opening along the faces, white-hot, longer the nearer the burst.
            double open = (hot - 0.25) / 0.75;
            for (int k = 0; k < 3; k++) {
                int s = (int)(hash(seed * 7 + k) * sides) % sides;
                double[] from = lerp(rear[s], front[s], 0.1 + 0.2 * hash(seed + 20 + k));
                double[] to = lerp(front[s], point, 0.2 + 0.6 * open * hash(seed + 30 + k));
                double[] lift = scale(normalize(sub(lerp(from, to, 0.5), add(tip, scale(axis, -length * 0.5)))), radius * 0.08);
                double w = radius * 0.12;
                int white = argb(0xFFFFFF, alpha * open);
                int clear = argb(0xFFE0E8, 0.0);
                sink.quad(add(add(from, lift), scale(across, w)), add(add(to, lift), scale(across, w * 0.3)), add(add(to, lift), scale(across, -w * 0.3)),
                    add(add(from, lift), scale(across, -w)), white, clear, clear, white);
            }
        }
    }

    /** One face: the crystal's key light and inner glow, a glint from the camera, a pink sheen when edge-on, hotter as it burns. */
    private static void facet(RcShard.Sink sink, double[] a, double[] b, double[] c, double[] d, int low, int high, double[] eye, double vary,
                              double burning, double alpha) {
        double[] normal = normalize(cross(sub(c, a), sub(d, b)));
        if (dot(normal, normal) < 0.5) {
            normal = normalize(cross(sub(b, a), sub(c, a)));
        }
        double[] centre = scale(add(add(a, b), add(c, d)), 0.25);
        double[] view = normalize(sub(eye, centre));
        if (dot(normal, view) < 0.0) {
            normal = scale(normal, -1.0);
        }
        double[] light = UkakuCrystal.LIGHT;
        double diffuse = (UkakuCrystal.INNER_GLOW + 0.85 * Math.max(0.0, dot(normal, light)) + 0.25 * Math.max(0.0, -dot(normal, light))) * vary;
        double[] half = normalize(add(light, view));
        double glint = Math.min(1.0, 1.6 * Math.pow(Math.max(0.0, dot(normal, half)), 22.0));
        double sheen = Math.pow(1.0 - Math.abs(dot(normal, view)), 2.0);
        int lo = lit(low, diffuse, glint, sheen, burning);
        int hi = lit(high, diffuse, glint, sheen, burning);
        int ca = argb(lo, alpha * 0.85);
        int cc = argb(hi, alpha * 0.92);
        sink.quad(a, b, c, d, ca, ca, cc, cc);
    }

    private static int lit(int colour, double diffuse, double glint, double sheen, double burning) {
        int c = UkakuCrystal.scaleRgb(colour, diffuse + 0.6 * burning);
        c = mix(c, UkakuCrystal.SHEEN, 0.5 * sheen);
        c = mix(c, 0xFFF0F4, Math.min(1.0, 0.4 * burning));
        return mix(c, UkakuCrystal.GLINT, glint);
    }

    // ------------------------------------------------------------------ helpers

    static double hash(int n) {
        return UkakuModel.hash(n);
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

    private static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] scale(double[] a, double k) {
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
        return length < 1.0E-12 ? new double[] {0.0, 0.0, 1.0} : scale(a, 1.0 / length);
    }
}
