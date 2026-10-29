package com.hex.kagune.control.ukaku;

import static com.hex.kagune.control.ukaku.UkakuModel.*;

/**
 * The gaseous Ukaku wing (at rest, kicks and bursts): a volume of RC fire pouring out of the shoulder blade.
 *
 * <p>Nothing here is a flat sheet. The wing is a bundle of flame tongues, each a curving, tapering 3D tube with an
 * elliptical cross-section, spread through space (up and out, and also back and forward of the wing's plane) so it
 * has depth from every side, including from above. Each tube glows like gas: where the camera looks straight through
 * it the fire is dense and hot; toward its silhouette it thins out into a soft lavender-pink glow (the shading is
 * worked out per vertex from the camera position). Inside the big tongues runs a white-hot inner core, a molten
 * fireball sits at the shoulder blade, sparks rise off the tips, and in the first appearance billowing 3D gas clouds
 * boil at the root.
 *
 * <p>Colour follows the anime: white-hot root, molten orange, red fire with dark streaks spiralling along each tongue,
 * dark crimson tips (season 1). From the √A era on (mastery 250 to 450) the fire past the root darkens into a starry
 * navy with red cores (the Cochlea frames and the :re coloured page). The fire licks: waves run from root to tip in two
 * directions, tongues flicker in length and width, the whole wing sways. It erupts out of the shoulder blade (fireball
 * first, then the tongues extrude) and dies back into it in a shower of sparks.
 */
public final class UkakuFlame {
    // Fire (anime season 1): white-hot root, molten orange, red, deep crimson, near-black tongue edges.
    static final int CORE = 0xFFF6D8;
    static final int MOLTEN = 0xFFB02A;
    static final int FIRE = 0xFF3C12;
    static final int RED = 0xE8121C;
    static final int CRIMSON = 0x8E0A1E;
    static final int EMBER = 0x2E0414;
    // √A / :re: the flame past the root turns into a starry navy night.
    static final int NAVY = 0x16134A;
    static final int ABYSS = 0x070626;
    // The lavender-pink glow at the edges of the fire.
    static final int RIM = 0xF2A2F0;
    static final int RIM_HOT = 0xFF86C4;
    // Gas clouds (the first appearance at Anteiku): white-hot hearts in red-pink clouds.
    static final int PUFF_CORE = 0xFFF2F2;
    static final int PUFF_BODY = 0xF8586A;

    /**
     * One flame tongue: direction in the wing plane (degrees from outward toward up), tilt out of the plane (degrees
     * toward the dorsal side), reach (wing lengths), half-width at its widest (wing lengths), thickness as a fraction
     * of the width, and its kind (the broad body of the fire, a main tongue, or a thin wisp).
     */
    record Tongue(double angle, double tilt, double reach, double width, double thickness, Kind kind) {
    }

    enum Kind {
        BODY, MAIN, WISP
    }

    /**
     * Season 1 / √A: a broad body of fire rising up and out, five fat main tongues and wisps, spread in depth behind
     * the wing plane (never toward the body), the most upright tongue still leaning out clear of the head.
     */
    static final Tongue[] TOKYO_GHOUL = {
        new Tongue(46, 10, 0.56, 0.250, 0.70, Kind.BODY),
        new Tongue(27, 2, 0.70, 0.150, 0.72, Kind.MAIN),
        new Tongue(37, 20, 0.88, 0.165, 0.72, Kind.MAIN),
        new Tongue(47, 8, 1.00, 0.170, 0.72, Kind.MAIN),
        new Tongue(54, 26, 0.92, 0.160, 0.72, Kind.MAIN),
        new Tongue(61, 6, 0.74, 0.145, 0.72, Kind.MAIN),
        new Tongue(32, 32, 0.62, 0.050, 0.9, Kind.WISP),
        new Tongue(42, 4, 0.84, 0.050, 0.9, Kind.WISP),
        new Tongue(52, 36, 0.88, 0.046, 0.9, Kind.WISP),
        new Tongue(58, 2, 0.70, 0.046, 0.9, Kind.WISP),
        new Tongue(50, 44, 0.66, 0.042, 0.9, Kind.WISP)};

    /** :re: larger and more controlled: seven long, evenly spaced tongues swept up and out, with wisps. */
    static final Tongue[] RE = {
        new Tongue(43, 10, 0.50, 0.220, 0.70, Kind.BODY),
        new Tongue(20, 2, 0.72, 0.120, 0.70, Kind.MAIN),
        new Tongue(28, 18, 0.84, 0.128, 0.70, Kind.MAIN),
        new Tongue(36, 6, 0.95, 0.134, 0.70, Kind.MAIN),
        new Tongue(44, 20, 1.00, 0.136, 0.70, Kind.MAIN),
        new Tongue(52, 4, 0.95, 0.134, 0.70, Kind.MAIN),
        new Tongue(56, 22, 0.84, 0.128, 0.70, Kind.MAIN),
        new Tongue(61, 8, 0.72, 0.118, 0.70, Kind.MAIN),
        new Tongue(24, 34, 0.62, 0.040, 0.9, Kind.WISP),
        new Tongue(40, 2, 0.84, 0.040, 0.9, Kind.WISP),
        new Tongue(48, 36, 0.88, 0.040, 0.9, Kind.WISP),
        new Tongue(58, 2, 0.70, 0.036, 0.9, Kind.WISP)};

    /** Every tongue leaves the shoulder blade as a narrow jet this wide (blocks, each side) and widens at this rate. */
    static final double ROOT_HALF_WIDTH = 0.055;
    static final double SPREAD = 0.38;
    /** Red veins running through the fire (blocks, each side of the line). */
    static final double VEIN_HALF_WIDTH = 0.016;
    static final int VEIN = 0xFF2236;
    static final int VEIN_HOT = 0xFF5040;

    /** How far above the crystal front the fire glows white-hot pink (share of the tongue), and that colour. */
    static final double SEAM = 0.09;
    static final int SEAM_HOT = 0xFFC2E0;
    /** Once the crystal front has climbed this far, the fire left beyond it is too small to draw. */
    static final double GONE = 0.985;
    /** Past this front the molten fireball and the gas clouds at the root are gone (frozen into the crystal). */
    static final double ROOT_FROZEN = 0.08;

    /** Where the camera is assumed to be when none is given (rig space: behind the player, a little above). */
    static final double[] DEFAULT_EYE = {0.6, 2.4, -5.0};

    private UkakuFlame() {
    }

    static Tongue[] layout(boolean reEra) {
        return reEra ? RE : TOKYO_GHOUL;
    }

    /**
     * Everything about this wing's fire for one frame. {@code from} is the crystal front: the fire only burns beyond it
     * (0 the whole tongue .. 1 none).
     */
    record Look(double time, double phase, boolean fine, double dusk, double sway, double heat, double kick, double alpha,
                double weight, double deploy, boolean dissolving, double early, double[] eye, double from) {
    }

    /** One gaseous wing, faded and condensed toward the root by {@code weight} (0..1, the share still gaseous). */
    public static void wing(Sink sink, Frame frame, Params p, double weight) {
        wing(sink, frame, p, weight, 0.0);
    }

    /**
     * One gaseous wing, faded by {@code weight}, burning only beyond {@code from} along each tongue (the crystal front: the
     * crystal has frozen everything below it).
     */
    public static void wing(Sink sink, Frame frame, Params p, double weight, double from) {
        double deploy = clamp01((double)p.deploy());
        double alpha = clamp01((double)p.alpha()) * clamp01(weight);
        if (deploy <= 0.001 || alpha <= 0.003) {
            return;
        }
        double kick = clamp01((double)p.kick());
        // The wing flares hot while it erupts and when it thrusts.
        double erupting = p.dissolving() || deploy >= 1.0 ? 0.0 : Math.sin(Math.PI * Math.min(1.0, deploy / 0.8));
        Look look = new Look(p.time(), p.seed() * 2.3, p.detail() >= 0.6F, smooth((p.mastery() - 250) / 200.0), p.reEra() ? 0.8 : 1.0,
            Math.max(erupting, kick * 0.8), kick, alpha, clamp01(weight), deploy, p.dissolving(), 1.0 - smooth((p.mastery() - 40) / 160.0),
            p.eye() != null ? p.eye() : DEFAULT_EYE, clamp01(from));
        if (look.from() >= GONE) {
            return;
        }
        Tongue[] tongues = layout(p.reEra());
        for (int i = 0; i < tongues.length; i++) {
            tongue(sink, frame, tongues[i], i, look);
        }
        if (look.from() < ROOT_FROZEN) {
            fireball(sink, frame, look);
        }
        if (look.fine()) {
            if (look.from() < ROOT_FROZEN) {
                puffs(sink, frame, tongues, look);
            }
            sparks(sink, frame, tongues, look);
        }
    }

    // ------------------------------------------------------------------ tongues

    /** How far a tongue has grown out of the shoulder blade (the body first, the long tongues last). */
    static double grow(Tongue tongue, int index, Look look) {
        if (look.dissolving()) {
            return smooth(look.deploy() / 0.9);
        }
        double delay = tongue.kind() == Kind.BODY ? 0.05 : 0.15 + 0.25 * tongue.reach() + 0.03 * (index % 3);
        return smooth((look.deploy() - delay) / 0.5);
    }

    /** A tongue's length flicker: two beating waves plus an occasional lick upward. */
    static double flicker(int index, Look look) {
        double t = look.time() * look.sway();
        double ph = index * 1.37 + look.phase();
        double lick = Math.pow(Math.max(0.0, Math.sin(t * 1.9 + ph * 2.1)), 8.0);
        return 1.0 + 0.07 * Math.sin(t * 6.3 + ph) + 0.05 * Math.sin(t * 9.7 + ph * 1.7) + 0.1 * lick;
    }

    /** Half-width profile along a tongue (t 0 root .. 1 tip): swelling out of the root, broad, then tapering to a point. */
    static double profile(double t, Kind kind) {
        double swell = kind == Kind.WISP ? 0.7 + 0.3 * smooth(t / 0.3) : 0.45 + 0.55 * smooth(t / 0.3);
        double taper = t < 0.32 ? 1.0 : Math.pow(Math.max(0.0, (1.0 - t) / 0.68), kind == Kind.BODY ? 0.6 : 0.8);
        return swell * taper;
    }

    /** How far a tongue's licking waves swing (fractions of its length): the body barely, wisps the most. */
    static double licking(Kind kind) {
        return switch (kind) {
            case BODY -> 0.035;
            case MAIN -> 0.06;
            case WISP -> 0.11;
        };
    }

    /** The spine of a tongue at {@code t}, wing-local: out along its direction, licking in two directions, rising. */
    static double[] spine(Tongue tongue, int index, double length, double t, Look look, double[] dir, double[] side, double[] lift) {
        double time = look.time() * look.sway();
        double ph = index * 2.13 + look.phase();
        double wave = licking(tongue.kind()) * look.sway() * (1.0 + 0.6 * look.kick());
        double reach = length * t;
        double s1 = wave * Math.pow(t, 1.3) * (Math.sin(time * 5.1 - t * 6.5 + ph) + 0.35 * Math.sin(time * 8.3 - t * 11.0 + ph * 1.7));
        double s2 = wave * 0.8 * Math.pow(t, 1.3) * Math.sin(time * 4.3 - t * 5.5 + ph * 0.7 + 1.1);
        // Flames rise: the outer part curls upward; a thrust streams it back.
        double rise = 0.12 * t * t;
        double back = look.kick() * 0.35 * t * t;
        return new double[] {
            dir[0] * reach + (side[0] * s1 + lift[0] * s2) * length,
            dir[1] * reach + (side[1] * s1 + lift[1] * s2) * length + rise * length,
            dir[2] * reach + (side[2] * s1 + lift[2] * s2) * length + back * length};
    }

    /** One flame tongue as a glowing tube, with a white-hot core inside the big ones. */
    private static void tongue(Sink sink, Frame frame, Tongue tongue, int index, Look look) {
        double grow = grow(tongue, index, look);
        if (grow <= 0.01) {
            return;
        }
        // As the crystal climbs, the fire left above it shrinks a little toward the tip, eaten by the crystal.
        double length = tongue.reach() * flicker(index, look) * grow * (1.0 + 0.25 * look.kick()) * (0.55 + 0.45 * look.weight())
            * (1.0 - 0.15 * look.from());
        double a = Math.toRadians(tongue.angle());
        double b = Math.toRadians(tongue.tilt());
        double[] dir = {Math.cos(a) * Math.cos(b), Math.sin(a) * Math.cos(b), Math.sin(b)};
        double[] side = {-Math.sin(a), Math.cos(a), 0.0};
        double[] lift = normalize(cross(dir, side));
        int stations = look.fine() ? (tongue.kind() == Kind.WISP ? 7 : 11) : (tongue.kind() == Kind.WISP ? 4 : 6);
        int sides = look.fine() ? (tongue.kind() == Kind.WISP ? 5 : tongue.kind() == Kind.BODY ? 9 : 7) : (tongue.kind() == Kind.WISP ? 3 : 5);
        double width = tongue.width() * (0.4 + 0.6 * grow);
        // Above the crystal front only: fewer rings for what is left, never fewer than a few.
        double left = 1.0 - look.from();
        int remaining = Math.max(3, (int)Math.ceil(stations * left));
        tube(sink, frame, tongue, index, length, width, 1.0, remaining, sides, dir, side, lift, look, false);
        if (look.fine() && tongue.kind() != Kind.WISP && look.from() < 0.62 - 0.05) {
            tube(sink, frame, tongue, index, length * 0.62, width * 0.42, 0.62, Math.max(3, (int)Math.ceil(6 * (0.62 - look.from()) / 0.62)), 5, dir,
                side, lift, look, true);
        }
    }

    /**
     * A tube along a tongue's spine: rings of an ellipse (wide in the wing plane, {@code thickness} as deep), shaded per
     * vertex from the camera: dense fire where it faces the camera, a soft lavender glow toward its silhouette.
     * {@code span} is the share of the tongue the tube covers (the core runs part of the way); it starts at the crystal
     * front ({@code look.from()}), so the fire burns on out of the crystal.
     */
    private static void tube(Sink sink, Frame frame, Tongue tongue, int index, double length, double width, double span, int stations, int sides,
                             double[] dir, double[] side, double[] lift, Look look, boolean core) {
        double[][][] point = new double[stations + 1][sides][];
        int[][] colour = new int[stations + 1][sides];
        double[][] centres = new double[stations + 1][];
        double[][] axes1 = new double[stations + 1][];
        double[][] axes2 = new double[stations + 1][];
        double[] halves = new double[stations + 1];
        double[] deeps = new double[stations + 1];
        double full = length / span;
        double unit = 1.0 / frame.scale(); // one block, in wing lengths
        double time = look.time() * look.sway();
        double ph = index * 1.91 + look.phase();
        double from = Math.min(span, look.from());
        for (int k = 0; k <= stations; k++) {
            double t = k / (double)stations;
            double along = from + t * (span - from); // where on the whole tongue this ring sits
            double[] centre = spine(tongue, index, full, along, look, dir, side, lift);
            double[] tangent = normalize(sub(spine(tongue, index, full, Math.min(1.0, along + 0.02), look, dir, side, lift),
                spine(tongue, index, full, Math.max(0.0, along - 0.02), look, dir, side, lift)));
            double[] n1 = normalize(sub(side, scale(tangent, dot(side, tangent))));
            double[] n2 = cross(tangent, n1);
            double boil = 1.0 + 0.14 * Math.sin(time * 7.0 + along * 9.0 + ph) * Math.min(1.0, along * 3.0);
            double tc = along / span; // how far along the core itself
            double half = width * profile(core ? Math.min(1.0, tc * 0.9 + 0.1 * tc * tc) : along, tongue.kind()) * boil;
            // Out of the shoulder blade as a narrow jet, widening only as it travels (never engulfing the host).
            half = Math.min(half, ROOT_HALF_WIDTH * unit + along * full * SPREAD);
            double deep = half * tongue.thickness();
            centres[k] = centre;
            axes1[k] = n1;
            axes2[k] = n2;
            halves[k] = half;
            deeps[k] = deep;
            double g = along * full; // distance from the root along the fire, wing lengths
            for (int j = 0; j < sides; j++) {
                double theta = 2.0 * Math.PI * j / sides + (core ? 0.3 : 0.0);
                double c = Math.cos(theta);
                double s = Math.sin(theta);
                double[] local = {centre[0] + n1[0] * c * half + n2[0] * s * deep, centre[1] + n1[1] * c * half + n2[1] * s * deep,
                    centre[2] + n1[2] * c * half + n2[2] * s * deep};
                double[] normal = normalize(new double[] {n1[0] * c * deep + n2[0] * s * half, n1[1] * c * deep + n2[1] * s * half,
                    n1[2] * c * deep + n2[2] * s * half});
                double facing = facing(frame, frame.at(local[0], local[1], local[2]), normal, look.eye());
                double streak = Math.pow(Math.max(0.0, Math.sin(3.0 * theta + 2.4 * along + ph + 0.4 * time)), 4.0);
                colour[k][j] = core ? coreColour(g, facing, look) : shellColour(g, along, facing, streak, tongue.kind(), look);
                if (!core && look.from() > 0.02) {
                    // Where the crystal is freezing it, the fire burns white-hot pink: the seam climbing the wing.
                    double seam = 1.0 - smooth((along - look.from()) / SEAM);
                    if (seam > 0.0) {
                        int was = colour[k][j];
                        double opacity = (was >>> 24) / 255.0;
                        colour[k][j] = argb(mixInt(was & 0xFFFFFF, mixInt(SEAM_HOT, CORE, 0.35 + 0.25 * facing), 0.75 * seam),
                            Math.min(look.alpha(), opacity + 0.35 * seam * look.alpha()));
                    }
                }
                point[k][j] = local;
            }
        }
        for (int k = 0; k < stations; k++) {
            for (int j = 0; j < sides; j++) {
                int n = (j + 1) % sides;
                glowQuad(sink, frame, point[k][j], point[k][n], point[k + 1][n], point[k + 1][j], colour[k][j], colour[k][n], colour[k + 1][n],
                    colour[k + 1][j]);
            }
        }
        if (!core && look.fine() && tongue.kind() != Kind.WISP) {
            int count = tongue.kind() == Kind.BODY ? 4 : 3;
            for (int vein = 0; vein < count; vein++) {
                double start = 2.0 * Math.PI * vein / count + index * 0.9;
                vein(sink, frame, centres, axes1, axes2, halves, deeps, span, index * 7 + vein, start, 0.1, 0.92, 0.0, look);
                if (tongue.kind() == Kind.MAIN && vein < 2) {
                    // A fork: a shorter vein splitting off and drifting around the tongue.
                    double fork = 0.32 + 0.26 * vein;
                    vein(sink, frame, centres, axes1, axes2, halves, deeps, span, index * 7 + vein, start, fork, Math.min(0.9, fork + 0.3),
                        vein == 0 ? 1.1 : -1.1, look);
                }
            }
        }
    }

    /**
     * A red vein along a tube's surface from {@code from} to {@code to} (fractions of the tube), wandering around it in
     * a jagged line; {@code veer} turns a fork away from its parent vein. A bright line over a soft red glow, lying on
     * the surface (just outside it, so the fire never hides the near ones and the far ones show through the fire).
     */
    private static void vein(Sink sink, Frame frame, double[][] centres, double[][] axes1, double[][] axes2, double[] halves, double[] deeps,
                             double span, int seed, double start, double from, double to, double veer, Look look) {
        int stations = centres.length - 1;
        double unit = 1.0 / frame.scale();
        double[] previousLeft = null;
        double[] previousRight = null;
        double previousFade = 0.0;
        int steps = Math.max(3, (int)Math.round((to - from) * stations * 1.3));
        for (int step = 0; step <= steps; step++) {
            double t = from + (to - from) * step / steps;
            double x = t * stations;
            int k = Math.min(stations - 1, (int)Math.floor(x));
            double f = x - k;
            double[] centre = lerp3(centres[k], centres[k + 1], f);
            double[] n1 = lerp3(axes1[k], axes1[k + 1], f);
            double[] n2 = lerp3(axes2[k], axes2[k + 1], f);
            double half = halves[k] + (halves[k + 1] - halves[k]) * f;
            double deep = deeps[k] + (deeps[k + 1] - deeps[k]) * f;
            double along = (t - from) / Math.max(1.0E-6, to - from);
            double theta = start + veer * along + 0.55 * Math.sin(t * span * 8.0 + seed * 1.3) + 0.3 * Math.sin(t * span * 27.0 + seed)
                + 0.15 * Math.sin(look.time() * 1.3 + seed);
            double c = Math.cos(theta);
            double sn = Math.sin(theta);
            double lift = 1.1; // off the surface, so the fire's own faces never cut the line into dashes
            double[] p = {centre[0] + (n1[0] * c * half + n2[0] * sn * deep) * lift, centre[1] + (n1[1] * c * half + n2[1] * sn * deep) * lift,
                centre[2] + (n1[2] * c * half + n2[2] * sn * deep) * lift};
            double[] across = normalize(new double[] {-n1[0] * sn * half + n2[0] * c * deep, -n1[1] * sn * half + n2[1] * c * deep,
                -n1[2] * sn * half + n2[2] * c * deep});
            // Thin and fading at both ends; a fork is thinner than its parent.
            double fade = smooth(along / 0.15) * (1.0 - smooth((along - 0.8) / 0.2));
            double width = VEIN_HALF_WIDTH * unit * (veer != 0.0 ? 0.75 : 1.0) * (0.4 + 0.6 * fade);
            double[] left = add3(p, scale(across, width));
            double[] right = add3(p, scale(across, -width));
            if (previousLeft != null) {
                int colour = mixInt(VEIN, VEIN_HOT, 0.3 * (1.0 - look.dusk()));
                int line0 = argb(colour, 0.95 * previousFade * look.alpha());
                int line1 = argb(colour, 0.95 * fade * look.alpha());
                glowQuad(sink, frame, previousLeft, left, right, previousRight, line0, line1, line1, line0);
            }
            previousLeft = left;
            previousRight = right;
            previousFade = fade;
        }
    }

    static double[] lerp3(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double[] add3(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    /** 0 at the silhouette (the surface seen edge-on) .. 1 facing the camera. */
    static double facing(Frame frame, double[] world, double[] localNormal, double[] eye) {
        double[] n = {frame.out()[0] * localNormal[0] + frame.up()[0] * localNormal[1] + frame.back()[0] * localNormal[2],
            frame.out()[1] * localNormal[0] + frame.up()[1] * localNormal[1] + frame.back()[1] * localNormal[2],
            frame.out()[2] * localNormal[0] + frame.up()[2] * localNormal[1] + frame.back()[2] * localNormal[2]};
        double[] v = normalize(new double[] {eye[0] - world[0], eye[1] - world[1], eye[2] - world[2]});
        return Math.min(1.0, Math.abs(n[0] * v[0] + n[1] * v[1] + n[2] * v[2]));
    }

    /** The outer fire of a tongue at {@code g} wing lengths from the root, {@code t} along the tongue. */
    static int shellColour(double g, double t, double facing, double streak, Kind kind, Look look) {
        int c = fireRamp(g);
        double body = smooth((g - 0.12) / 0.22);
        if (look.dusk() > 0.0) {
            // The Cochlea frames keep the fire red through nearly half the wing before it turns to night.
            int night = mixInt(NAVY, ABYSS, smooth((g - 0.6) / 0.4));
            c = mixInt(c, night, look.dusk() * smooth((g - 0.32) / 0.26));
            c = mixInt(c, RED, look.dusk() * streak * 0.8 * body * (1.0 - smooth((g - 0.85) / 0.15)));
        }
        c = mixInt(c, EMBER, (1.0 - look.dusk()) * streak * 0.6 * body);
        // Dark licking tips.
        c = mixInt(c, look.dusk() > 0.5 ? ABYSS : EMBER, 0.45 * smooth((t - 0.7) / 0.3));
        c = mixInt(c, CORE, look.heat() * 0.7 * (1.0 - smooth(g / 0.3)));
        // The silhouette thins into a lavender-pink glow.
        double edge = 1.0 - smooth(facing / 0.55);
        int rim = mixInt(RIM_HOT, RIM, 0.35 + 0.65 * look.dusk());
        c = mixInt(c, rim, edge * (kind == Kind.WISP ? 0.6 : 0.8));
        double alpha = (kind == Kind.WISP ? 0.7 : 0.86) * (0.16 + 0.84 * smooth(facing / 0.6)) * (1.0 - 0.35 * smooth((t - 0.75) / 0.25));
        return argb(c, alpha * look.alpha());
    }

    /** The white-hot core inside a big tongue: bright through the middle of the fire, invisible at its edges. */
    static int coreColour(double g, double facing, Look look) {
        int hot = g < 0.12 ? mixInt(CORE, MOLTEN, g / 0.12) : mixInt(MOLTEN, FIRE, smooth((g - 0.12) / 0.4));
        if (look.dusk() > 0.0) {
            hot = mixInt(hot, mixInt(FIRE, RED, smooth((g - 0.2) / 0.3)), look.dusk() * smooth((g - 0.1) / 0.2));
        }
        double alpha = 0.75 * Math.pow(facing, 1.5) * (1.0 - smooth((g - 0.35) / 0.3) * 0.5);
        return argb(hot, alpha * look.alpha());
    }

    static int fireRamp(double g) {
        if (g < 0.06) {
            return mixInt(CORE, MOLTEN, g / 0.06);
        } else if (g < 0.16) {
            return mixInt(MOLTEN, FIRE, (g - 0.06) / 0.10);
        } else if (g < 0.36) {
            return mixInt(FIRE, RED, (g - 0.16) / 0.20);
        } else if (g < 0.66) {
            return mixInt(RED, CRIMSON, (g - 0.36) / 0.30);
        }
        return mixInt(CRIMSON, EMBER, (g - 0.66) / 0.34);
    }

    // ------------------------------------------------------------------ root, clouds and sparks

    /** The molten fireball at the shoulder blade, and its glow. */
    private static void fireball(Sink sink, Frame frame, Look look) {
        // Sized in blocks, not wing lengths: it stays a fist-sized fireball on the shoulder blade on any wing.
        double unit = 1.0 / frame.scale();
        double size = (0.055 + 0.035 * look.heat()) * unit * Math.min(1.0, look.deploy() * 3.0) * (0.6 + 0.4 * look.weight());
        if (size <= 0.002 * unit) {
            return;
        }
        int rings = look.fine() ? 5 : 3;
        int segments = look.fine() ? 7 : 5;
        double[] centre = {0.0, 0.0, 0.05 * unit};
        // Molten rather than white-hot, so the two roots read as fire welling out, not as a pair of glowing eyes.
        sphere(sink, frame, centre, size, rings, segments, look, mixInt(CORE, MOLTEN, 0.45), FIRE, 0.85);
        sphere(sink, frame, centre, size * 1.8, rings, segments, look, FIRE, RED, 0.35);
    }

    /**
     * A glowing ball: {@code centre} colour where it faces the camera, {@code edge} colour toward its silhouette, fading
     * out at the silhouette (so overlapping balls blend into one soft mass).
     */
    private static void sphere(Sink sink, Frame frame, double[] c, double r, int rings, int segments, Look look, int centre, int edge, double opacity) {
        double[][][] point = new double[rings + 1][segments][];
        int[][] colour = new int[rings + 1][segments];
        for (int i = 0; i <= rings; i++) {
            double lat = Math.PI * i / rings - Math.PI / 2.0;
            for (int j = 0; j < segments; j++) {
                double lon = 2.0 * Math.PI * j / segments;
                double[] n = {Math.cos(lat) * Math.cos(lon), Math.sin(lat), Math.cos(lat) * Math.sin(lon)};
                double[] local = {c[0] + n[0] * r, c[1] + n[1] * r, c[2] + n[2] * r};
                double f = facing(frame, frame.at(local[0], local[1], local[2]), n, look.eye());
                colour[i][j] = argb(mixInt(edge, centre, smooth(f / 0.8)), opacity * Math.pow(f, 1.2) * look.alpha());
                point[i][j] = local;
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segments; j++) {
                int n = (j + 1) % segments;
                glowQuad(sink, frame, point[i][j], point[i][n], point[i + 1][n], point[i + 1][j], colour[i][j], colour[i][n], colour[i + 1][n],
                    colour[i + 1][j]);
            }
        }
    }

    /** Billowing gas clouds at the root: big in the first appearance (Anteiku), a few small ones later. */
    private static void puffs(Sink sink, Frame frame, Tongue[] tongues, Look look) {
        double strength = (0.55 + 0.45 * look.early()) * Math.min(1.0, look.deploy() * 2.0) * (0.8 + 0.2 * look.heat());
        int count = 2 + (int)Math.round(5 * look.early());
        for (int puff = 0; puff < count; puff++) {
            double t = look.time() * 0.4 + puff * 0.31 + look.phase() * 0.1;
            double cycle = t - Math.floor(t); // each cloud boils out along the fire and fades, then boils up again
            double angle = Math.toRadians(tongues[0].angle() + (hash(puff * 5 + 2) - 0.5) * 64.0);
            // Behind the wing plane only, and sized in blocks: the clouds billow off the wing, never around the host.
            double tilt = Math.toRadians(8.0 + 40.0 * hash(puff * 7 + 1));
            double unit = 1.0 / frame.scale();
            double reach = 0.3 * unit + (0.12 + 0.3 * look.early()) * (0.25 + 0.75 * cycle);
            double[] c = {Math.cos(angle) * Math.cos(tilt) * reach, Math.sin(angle) * Math.cos(tilt) * reach, Math.sin(tilt) * reach};
            double radius = (0.08 + 0.14 * look.early() + 0.04 * hash(puff * 11)) * unit * (0.75 + 0.45 * cycle);
            double fade = Math.min(1.0, 2.5 * Math.sin(Math.PI * cycle)) * strength;
            if (fade > 0.02) {
                sphere(sink, frame, c, radius, 4, 7, look, mixInt(PUFF_BODY, PUFF_CORE, 0.55), PUFF_BODY, 0.75 * fade);
            }
        }
    }

    /** Sparks rising off the fire (a burst of them while it dies back into the shoulder blade). */
    private static void sparks(Sink sink, Frame frame, Tongue[] tongues, Look look) {
        boolean dying = look.dissolving() && look.deploy() < 0.999;
        int count = dying ? 16 : 6;
        double burst = dying ? Math.sin(Math.PI * Math.min(1.0, (1.0 - look.deploy()) * 1.2)) : 1.0;
        for (int spark = 0; spark < count; spark++) {
            double t = look.time() * (0.7 + 0.3 * hash(spark * 3)) + hash(spark * 17 + 5);
            double cycle = t - Math.floor(t);
            Tongue from = tongues[1 + (int)(hash(spark * 13 + (int)Math.floor(t)) * (tongues.length - 1))];
            double a = Math.toRadians(from.angle());
            double b = Math.toRadians(from.tilt());
            // Off the fire only, never out of the crystal below the front.
            double reach = from.reach() * Math.max(look.from() * 1.05, 0.5 + 0.4 * hash(spark * 5 + 1)) * (dying ? 1.0 - 0.5 * (1.0 - look.deploy()) : 1.0);
            double cx = Math.cos(a) * Math.cos(b) * reach;
            double cy = Math.sin(a) * Math.cos(b) * reach + cycle * 0.35;
            double cz = Math.sin(b) * reach + (hash(spark * 7) - 0.5) * 0.1;
            double size = 0.01 + 0.01 * hash(spark);
            double fade = Math.sin(Math.PI * cycle) * burst * look.alpha();
            if (fade <= 0.02) {
                continue;
            }
            int hot = argb(spark % 3 == 0 ? RIM_HOT : MOLTEN, 0.95 * fade);
            int clear = argb(FIRE, 0.0);
            glowQuad(sink, frame, new double[] {cx - size, cy, cz}, new double[] {cx, cy + size * 2.5, cz}, new double[] {cx + size, cy, cz},
                new double[] {cx, cy - size, cz}, hot, clear, hot, hot);
            glowQuad(sink, frame, new double[] {cx, cy, cz - size}, new double[] {cx, cy + size * 2.5, cz}, new double[] {cx, cy, cz + size},
                new double[] {cx, cy - size, cz}, hot, clear, hot, hot);
        }
    }

    // ------------------------------------------------------------------ vectors

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] scale(double[] a, double s) {
        return new double[] {a[0] * s, a[1] * s, a[2] * s};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
