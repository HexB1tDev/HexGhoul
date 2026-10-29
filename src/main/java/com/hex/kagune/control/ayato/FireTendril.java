package com.hex.kagune.control.ayato;

/**
 * One of the final kakuja's fire tendrils (pure, tested): the wing half of the form, three slim tendrils of RC fire where the
 * first kakuja has armored tentacles, riding the same living spines ({@link KakujaRig}) so they sway, trail, stretch and
 * keep clear of each other exactly as the tentacles do.
 *
 * <p>A tendril is the :re wing's fire drawn along a spine: red-hot out of the shoulder blade, red through its first third
 * with dark streaks spiralling along it, then the starry navy night deepening to the abyss toward the tip; it thins at its
 * silhouette into a lavender-pink glow and is densest where the camera looks straight through it (shaded per vertex from the
 * camera, as the wings' fire is). A molten core glows through its middle, jagged red veins crackle along its surface, and
 * pointed flame tongues tear off it all along its length, leaning out toward the tip and rising, red at the root and dark at
 * their points, so its outline is ragged fire and never a smooth tube. The tip forks into three tongues. Everything boils:
 * the width pulses in waves running out along it and every tongue flickers in length.
 */
public final class FireTendril {
    /** Half-width at the root, against the armored tentacle's at the same tier: these are the slim half. */
    public static final double SLIM = 0.58;
    /** Flame tongues tearing off a tendril (fine detail; half far away). */
    static final int TONGUES = 11;
    /** Red veins crackling along a tendril's surface (fine detail only). */
    static final int VEINS = 3;
    /** A vein's half-width (blocks). */
    static final double VEIN_HALF_WIDTH = 0.012;
    /** World up in the spine's space (rig space): flames rise. */
    static final double[] UP = {0.0, 1.0, 0.0};

    private FireTendril() {
    }

    /** Half-width along a tendril (share of the root's): a narrow jet, a slight swell, then a long taper into the forked tip. */
    public static double widthAt(double s) {
        double c = Math.max(0.0, Math.min(1.0, s));
        return (0.72 + 0.28 * UkakuModel.smooth(c / 0.18)) * Math.pow(Math.max(0.0, 1.0 - Math.pow(c, 1.35)), 0.75) * (1.0 + 0.2 * Math.sin(Math.PI * c));
    }

    /**
     * One fire tendril along {@code spine}. {@code look.width()} is the armored tentacle's root half-width at this tier (the
     * tendril takes {@link #SLIM} of it); {@code eye} the camera in the spine's space (null: shaded as if seen half on).
     */
    public static void tendril(UkakuModel.Sink sink, KakujaModel.Spine spine, KakujaModel.Look look, double[] eye) {
        double alpha = UkakuModel.clamp01(look.alpha());
        double width = look.width() * SLIM;
        if (alpha <= 0.003 || spine.length() <= 1.0E-4 || width <= 1.0E-5) {
            return;
        }
        boolean fine = look.fine();
        int stations = fine ? 20 : 11;
        int sides = fine ? 7 : 5;
        tube(sink, spine, look, eye, width, stations, sides, false, alpha);
        tube(sink, spine, look, eye, width * 0.45, Math.max(6, stations - 6), Math.max(4, sides - 2), true, alpha);
        if (fine) {
            for (int v = 0; v < VEINS; v++) {
                vein(sink, spine, look, width, v, alpha);
            }
        }
        int tongues = fine ? TONGUES : TONGUES / 2;
        for (int i = 0; i < tongues; i++) {
            tongue(sink, spine, look, eye, width, i, tongues, alpha);
        }
        for (int prong = 0; prong < 3; prong++) {
            prong(sink, spine, look, eye, width, prong, fine, alpha);
        }
        if (fine) {
            stars(sink, spine, look, width, alpha);
        }
        ember(sink, spine, look, width, alpha);
    }

    /** The boil: the tube's width pulsing in waves that run out toward the tip. */
    static double boil(double s, double time, int seed) {
        return 1.0 + 0.12 * Math.sin(time * 6.5 - s * 14.0 + seed * 1.7) * UkakuModel.smooth(s / 0.15) + 0.06 * Math.sin(time * 11.0 - s * 23.0 + seed);
    }

    /** The shell's half-width at {@code s} (the core is a slimmer tube inside it). */
    static double radius(double width, double s, KakujaModel.Look look) {
        return width * widthAt(s) * boil(s, look.time(), look.seed());
    }

    /** The shell of the fire ({@code core} false), or the molten core inside it. */
    static void tube(UkakuModel.Sink sink, KakujaModel.Spine spine, KakujaModel.Look look, double[] eye, double width, int stations, int sides,
                     boolean core, double alpha) {
        double[][][] point = new double[stations + 1][sides][];
        int[][] colour = new int[stations + 1][sides];
        double end = core ? 0.86 : 1.0;
        for (int k = 0; k <= stations; k++) {
            double s = end * k / (double)stations;
            double[][] f = spine.at(s);
            double half = radius(width, s, look);
            if (core) {
                half *= 1.0 - 0.6 * UkakuModel.smooth((s - 0.5) / 0.36);
            }
            // A flame is rounder than a plate: a little flattened, its broad side to the dorsal.
            double deep = half * (core ? 0.9 : 0.8);
            for (int j = 0; j < sides; j++) {
                double theta = 2.0 * Math.PI * j / sides + (core ? 0.4 : 0.0);
                double c = Math.cos(theta);
                double sn = Math.sin(theta);
                double[] local = add(f[0], add(mul(f[3], c * half), mul(f[2], sn * deep)));
                double[] normal = normalize(add(mul(f[3], c * deep), mul(f[2], sn * half)));
                double facing = facing(local, normal, eye);
                point[k][j] = local;
                colour[k][j] = core ? coreColour(s, facing, alpha) : shellColour(s, theta, facing, look, alpha);
            }
        }
        for (int k = 0; k < stations; k++) {
            for (int j = 0; j < sides; j++) {
                int n = (j + 1) % sides;
                KakujaModel.glow(sink, point[k][j], point[k][n], point[k + 1][n], point[k + 1][j], colour[k][j], colour[k][n], colour[k + 1][n],
                    colour[k + 1][j]);
            }
        }
    }

    /**
     * The fire at {@code s} along the tendril: the wing's own ramp, white-hot to red out of the shoulder blade, red through its
     * first third, then the :re night.
     */
    static int fire(double s) {
        int c = UkakuFlame.fireRamp(0.06 + s * 1.1);
        int night = UkakuModel.mixInt(UkakuFlame.NAVY, UkakuFlame.ABYSS, UkakuModel.smooth((s - 0.62) / 0.38));
        return UkakuModel.mixInt(c, night, UkakuModel.smooth((s - 0.28) / 0.3));
    }

    /** The shell's fire at {@code s} along the tendril, round it at {@code theta}. */
    static int shellColour(double s, double theta, double facing, KakujaModel.Look look, double alpha) {
        int c = fire(s);
        // Streaks spiralling out along it: dark ones in the red, red ones in the night.
        double streak = Math.pow(Math.max(0.0, Math.sin(2.0 * theta + 9.0 * s - look.time() * 1.6 + look.seed())), 5.0);
        double night = UkakuModel.smooth((s - 0.28) / 0.3);
        c = UkakuModel.mixInt(c, UkakuFlame.EMBER, 0.5 * streak * (1.0 - night) * UkakuModel.smooth((s - 0.06) / 0.1));
        c = UkakuModel.mixInt(c, UkakuFlame.RED, 0.7 * streak * night * (1.0 - UkakuModel.smooth((s - 0.82) / 0.18)));
        // The silhouette thins into lavender-pink.
        double edge = 1.0 - UkakuModel.smooth(facing / 0.55);
        c = UkakuModel.mixInt(c, UkakuModel.mixInt(UkakuFlame.RIM_HOT, UkakuFlame.RIM, 0.3 + 0.7 * UkakuModel.smooth(s / 0.4)), 0.8 * edge);
        double opacity = 0.84 * (0.16 + 0.84 * UkakuModel.smooth(facing / 0.6)) * (1.0 - 0.6 * UkakuModel.smooth((s - 0.75) / 0.25));
        return UkakuModel.argb(c, opacity * alpha);
    }

    /** The molten core: white-orange at the root, red out along it, seen through the middle of the fire and gone at its edges. */
    static int coreColour(double s, double facing, double alpha) {
        int hot = s < 0.1 ? UkakuModel.mixInt(UkakuFlame.CORE, UkakuFlame.MOLTEN, s / 0.1)
            : UkakuModel.mixInt(UkakuModel.mixInt(UkakuFlame.MOLTEN, UkakuFlame.FIRE, UkakuModel.smooth((s - 0.1) / 0.2)), UkakuFlame.RED,
                UkakuModel.smooth((s - 0.3) / 0.3));
        double opacity = 0.9 * Math.pow(facing, 1.3) * (1.0 - UkakuModel.smooth((s - 0.6) / 0.26));
        return UkakuModel.argb(hot, opacity * alpha);
    }

    /**
     * Red vein {@code index}: a jagged line wandering along the shell's surface from near the root to the tip's night, lifted
     * just off it (so the fire's own faces never cut it into dashes), thin and fading at both ends.
     */
    static void vein(UkakuModel.Sink sink, KakujaModel.Spine spine, KakujaModel.Look look, double width, int index, double alpha) {
        int seed = look.seed() * 37 + index * 11;
        double from = 0.06 + 0.06 * UkakuModel.hash(seed);
        double to = 0.78 + 0.12 * UkakuModel.hash(seed + 1);
        double start = 2.0 * Math.PI * index / VEINS + look.seed() * 0.7;
        int steps = 18;
        double[] previousLeft = null;
        double[] previousRight = null;
        double previousFade = 0.0;
        for (int step = 0; step <= steps; step++) {
            double t = step / (double)steps;
            double s = from + (to - from) * t;
            double[][] f = spine.at(s);
            double half = radius(width, s, look) * 1.08;
            double theta = start + 0.6 * Math.sin(s * 11.0 + seed * 1.3) + 0.35 * Math.sin(s * 31.0 + seed) + 0.15 * Math.sin(look.time() * 1.3 + seed)
                + 1.4 * t;
            double c = Math.cos(theta);
            double sn = Math.sin(theta);
            double[] p = add(f[0], add(mul(f[3], c * half), mul(f[2], sn * half * 0.8)));
            double[] across = normalize(add(mul(f[3], -sn), mul(f[2], c)));
            double fade = UkakuModel.smooth(t / 0.15) * (1.0 - UkakuModel.smooth((t - 0.75) / 0.25));
            double w = VEIN_HALF_WIDTH * (0.4 + 0.6 * fade);
            double[] left = add(p, mul(across, w));
            double[] right = add(p, mul(across, -w));
            if (previousLeft != null) {
                int colour = UkakuModel.mixInt(UkakuFlame.VEIN_HOT, UkakuFlame.VEIN, UkakuModel.smooth(s / 0.4));
                int line0 = UkakuModel.argb(colour, 0.95 * previousFade * alpha);
                int line1 = UkakuModel.argb(colour, 0.95 * fade * alpha);
                KakujaModel.glow(sink, previousLeft, left, right, previousRight, line0, line1, line1, line0);
            }
            previousLeft = left;
            previousRight = right;
            previousFade = fade;
        }
    }

    /**
     * Flame tongue {@code index}: a pointed tongue tearing off the tendril, leaning out toward the tip and rising as flames
     * do, flickering in length: the tendril's fire at its root, dark at its point, its silhouette glowing lavender.
     */
    static void tongue(UkakuModel.Sink sink, KakujaModel.Spine spine, KakujaModel.Look look, double[] eye, double width, int index, int count,
                       double alpha) {
        int seed = look.seed() * 131 + index * 17;
        double s = 0.1 + 0.72 * (index + 0.6 * UkakuModel.hash(seed)) / count;
        double[][] f = spine.at(s);
        double local = radius(width, s, look);
        // Round the tube in turn, never twice on the same side.
        double around = 2.0 * Math.PI * (index * 0.382 + 0.2 * UkakuModel.hash(seed + 1));
        double[] out = normalize(add(mul(f[2], Math.cos(around)), mul(f[3], Math.sin(around))));
        double flicker = 0.7 + 0.3 * Math.sin(look.time() * (5.0 + 2.5 * UkakuModel.hash(seed + 2)) + seed);
        double length = local * (2.6 + 1.6 * UkakuModel.hash(seed + 3)) * flicker * (1.0 + 0.4 * (1.0 - s));
        double[] dir = normalize(add(add(mul(out, 0.75), mul(f[1], 0.75)), mul(UP, 0.25)));
        double[] base = add(f[0], mul(out, local * 0.5));
        double[] rise = normalize(add(mul(f[1], 0.6), mul(UP, 0.8)));
        flame(sink, base, dir, rise, local * 0.62, length, s, eye, look.fine() ? 5 : 3, look.fine() ? 4 : 3, alpha);
    }

    /** One of the three prongs the tip forks into: the middle one straight on, the others splaying a little. */
    static void prong(UkakuModel.Sink sink, KakujaModel.Spine spine, KakujaModel.Look look, double[] eye, double width, int prong, boolean fine,
                      double alpha) {
        double s = prong == 1 ? 0.8 : 0.72;
        double[][] f = spine.at(s);
        double local = radius(width, s, look);
        double splay = (prong - 1) * 0.45;
        double twist = look.seed() * 0.9;
        double[] side = normalize(add(mul(f[3], Math.cos(twist)), mul(f[2], Math.sin(twist))));
        double[] dir = normalize(add(f[1], mul(side, splay)));
        double flicker = 0.8 + 0.2 * Math.sin(look.time() * (6.0 + prong) + look.seed() * 2.0 + prong);
        double length = spine.length() * (prong == 1 ? 0.3 : 0.22) * flicker;
        double[] rise = normalize(add(f[1], mul(UP, 0.4)));
        flame(sink, f[0], dir, rise, Math.max(local, width * 0.25) * 0.9, length, s, eye, fine ? 6 : 3, fine ? 4 : 3, alpha);
    }

    /**
     * A pointed flame from {@code base} along {@code dir}, curling toward {@code curl} as it goes: {@code half} wide at its
     * base, tapering to a point, its fire starting at the tendril's at {@code s} and darkening to its point.
     */
    static void flame(UkakuModel.Sink sink, double[] base, double[] dir, double[] curl, double half, double length, double s, double[] eye,
                      int stations, int sides, double alpha) {
        if (length <= 1.0E-4 || half <= 1.0E-5) {
            return;
        }
        double[][][] point = new double[stations + 1][sides][];
        int[][] colour = new int[stations + 1][sides];
        double[] p = base;
        double[] d = dir;
        double[] side0 = normalize(cross(dir, UP));
        if (dot(side0, side0) < 0.5) {
            side0 = normalize(cross(dir, new double[] {1.0, 0.0, 0.0}));
        }
        int root = fire(s);
        for (int k = 0; k <= stations; k++) {
            double t = k / (double)stations;
            double w = half * Math.pow(1.0 - t, 0.85) * (0.75 + 0.45 * Math.sin(Math.PI * Math.min(1.0, t * 1.4)));
            double[] a1 = normalize(sub(side0, mul(d, dot(side0, d))));
            double[] a2 = cross(d, a1);
            for (int j = 0; j < sides; j++) {
                double theta = 2.0 * Math.PI * j / sides + 0.3;
                double c = Math.cos(theta);
                double sn = Math.sin(theta);
                double[] v = add(p, add(mul(a1, c * w), mul(a2, sn * w * 0.7)));
                double facing = facing(v, normalize(add(mul(a1, c), mul(a2, sn))), eye);
                double edge = 1.0 - UkakuModel.smooth(facing / 0.55);
                int body = UkakuModel.mixInt(root, fire(Math.min(1.0, s + 0.5)), UkakuModel.smooth(t / 0.7));
                int tint = UkakuModel.mixInt(body, UkakuModel.mixInt(UkakuFlame.RIM_HOT, UkakuFlame.RIM, t), 0.7 * edge);
                point[k][j] = v;
                colour[k][j] = UkakuModel.argb(tint, 0.82 * (0.2 + 0.8 * UkakuModel.smooth(facing / 0.6)) * (1.0 - 0.7 * t * t) * alpha);
            }
            d = normalize(add(d, mul(curl, 0.22)));
            p = add(p, mul(d, length / stations));
        }
        for (int k = 0; k < stations; k++) {
            for (int j = 0; j < sides; j++) {
                int n = (j + 1) % sides;
                KakujaModel.glow(sink, point[k][j], point[k][n], point[k + 1][n], point[k + 1][j], colour[k][j], colour[k][n], colour[k + 1][n],
                    colour[k + 1][j]);
            }
        }
    }

    /** A few stars glinting in the navy, twinkling in turn. */
    static void stars(UkakuModel.Sink sink, KakujaModel.Spine spine, KakujaModel.Look look, double width, double alpha) {
        for (int g = 0; g < 4; g++) {
            double phase = look.time() * (1.3 + 0.35 * g) + look.seed() * 1.9 + g * 2.2;
            double twinkle = Math.pow(Math.max(0.0, Math.sin(phase)), 6.0);
            if (twinkle < 0.03) {
                continue;
            }
            int cycle = (int)Math.floor(phase / (2.0 * Math.PI));
            double s = 0.25 + 0.55 * UkakuModel.hash(look.seed() * 29 + g * 5 + cycle * 11);
            double around = 2.0 * Math.PI * UkakuModel.hash(look.seed() * 31 + g * 7 + cycle * 13);
            double[][] f = spine.at(s);
            double r = width * widthAt(s) * 0.7;
            double[] at = add(f[0], add(mul(f[3], Math.cos(around) * r), mul(f[2], Math.sin(around) * r)));
            star(sink, at, width * 0.55 * twinkle, f[1], f[3], twinkle * alpha);
        }
    }

    /** The hot glow where the tendril leaves the shoulder blade. */
    static void ember(UkakuModel.Sink sink, KakujaModel.Spine spine, KakujaModel.Look look, double width, double alpha) {
        double[][] f = spine.at(0.02);
        double r = width * 1.1 * (1.0 + 0.1 * Math.sin(look.time() * 8.0 + look.seed()));
        int hot = UkakuModel.argb(UkakuFlame.MOLTEN, 0.55 * alpha);
        int clear = UkakuModel.argb(UkakuFlame.RED, 0.0);
        int segments = 8;
        for (int k = 0; k < segments; k++) {
            double a0 = 2.0 * Math.PI * k / segments;
            double a1 = 2.0 * Math.PI * (k + 1) / segments;
            double[] p0 = add(f[0], add(mul(f[3], Math.cos(a0) * r), mul(f[2], Math.sin(a0) * r)));
            double[] p1 = add(f[0], add(mul(f[3], Math.cos(a1) * r), mul(f[2], Math.sin(a1) * r)));
            KakujaModel.glow(sink, f[0], f[0], p1, p0, hot, hot, clear, clear);
        }
    }

    /** A four-point star: white-blue at the heart, in the plane of {@code u} and {@code v} and across it. */
    static void star(UkakuModel.Sink sink, double[] at, double size, double[] u, double[] v, double strength) {
        int core = UkakuModel.argb(0xF2F4FF, Math.min(1.0, strength));
        int clear = UkakuModel.argb(0xB8C4FF, 0.0);
        double[] w = cross(u, v);
        double[][][] planes = {{u, v}, {v, w}};
        for (double[][] plane : planes) {
            for (int arm = 0; arm < 4; arm++) {
                double angle = Math.PI / 2.0 * arm;
                double[] dir = add(mul(plane[0], Math.cos(angle)), mul(plane[1], Math.sin(angle)));
                double[] across = add(mul(plane[0], -Math.sin(angle)), mul(plane[1], Math.cos(angle)));
                double reach = size * (arm % 2 == 0 ? 1.0 : 0.7);
                KakujaModel.glow(sink, at, add(at, mul(across, size * 0.1)), add(at, mul(dir, reach)), add(at, mul(across, -size * 0.1)), core, core,
                    clear, core);
            }
        }
    }

    /** 0 at the silhouette (seen edge-on) .. 1 facing the camera; 0.55 without a camera. */
    static double facing(double[] at, double[] normal, double[] eye) {
        if (eye == null) {
            return 0.55;
        }
        double[] v = normalize(sub(eye, at));
        return Math.min(1.0, Math.abs(dot(normal, v)));
    }

    static double[] add(double[] a, double[] b) {
        return KakujaModel.add(a, b);
    }

    static double[] sub(double[] a, double[] b) {
        return KakujaModel.sub(a, b);
    }

    static double[] mul(double[] a, double k) {
        return KakujaModel.mul(a, k);
    }

    static double dot(double[] a, double[] b) {
        return KakujaModel.dot(a, b);
    }

    static double[] cross(double[] a, double[] b) {
        return KakujaModel.cross(a, b);
    }

    static double[] normalize(double[] a) {
        return KakujaModel.normalize(a);
    }
}
