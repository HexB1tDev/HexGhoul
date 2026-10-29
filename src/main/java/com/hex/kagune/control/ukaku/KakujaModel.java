package com.hex.kagune.control.ukaku;

/**
 * Ukaku's first kakuja as real 3D geometry (pure, tested), matched to the kakuja art and the anime frames: six armored
 * tentacle-wings, three from each shoulder blade, and the armor the kakuja grows over his body (the horned mask and the
 * plated chest).
 *
 * <p>A tentacle is a chain of overlapping armor plates (like a lobster's tail: each plate's lip rides over the one before
 * it), dark navy steel with a bright ridge along its back and a cold blue rim where each lip catches the light. Its belly is
 * open RC, a deep glowing magenta, and out of it grow two staggered rows of long magenta crystal spikes, raked toward the
 * tip, longest near the middle and shrinking toward the root and the point. The tip closes into a hooked claw. A faint
 * violet haze clings to the blade edges and a few spikes glint.
 *
 * <p>A tentacle is built along a {@link Spine} (its centre line with a frame at every sample: tangent, dorsal and the
 * leading edge). The spine is in any space the caller likes (the rig uses player-local blocks); armor pieces are built in the
 * player model's own pixels (x to the player's left, y down, z backward) for the head and the torso.
 */
public final class KakujaModel {
    // ---- Plates: dark navy steel, a pale ridge, a cold blue rim on each lip.
    static final int PLATE_DARK = 0x0B0D15;
    static final int PLATE = 0x1B2133;
    static final int PLATE_HI = 0x46527A;
    static final int RIDGE = 0x7A8CBA;
    static final int RIM = 0xA9D2FF;
    // ---- The open belly and the spikes: deep magenta RC, hot pink crystal.
    static final int BELLY = 0x3A0420;
    static final int BELLY_GLOW = 0xB0124E;
    static final int SPIKE_ROOT = 0x4A0222;
    static final int SPIKE = 0xB80E55;
    static final int SPIKE_TIP = 0xFF5FA6;
    static final int SPIKE_WHITE = 0xFFE4F2;
    static final int HAZE = 0xA23CFF;
    // ---- Armor on the body.
    static final int HORN_ROOT = 0x5A0810;
    static final int HORN = 0xC0121E;
    static final int HORN_TIP = 0xF2584A;
    static final int MASK = 0x101116;
    static final int MASK_HI = 0x343949;
    // ---- The final kakuja's half mask: black lacquer, steel rivets, vents glowing a dim red.
    static final int RIVET = 0x9AA3B8;
    static final int VENT = 0x040406;
    static final int VENT_GLOW = 0x8A0C1C;
    // ---- The final kakuja's fire sockets.
    static final int FIRE_SOCKET = 0xFF6A1E;
    static final int FIRE_SOCKET_CORE = 0xFFD9A0;

    /** Cross-section of a plate, unit half-width (x, across) and half-height (y, dorsal up): ridge, shoulders, blade edges, belly. */
    static final double[][] PROFILE = {
        {0.0, 1.15}, {0.42, 0.78}, {0.84, 0.3}, {1.0, 0.0}, {0.72, -0.38}, {0.28, -0.55},
        {-0.28, -0.55}, {-0.72, -0.38}, {-1.0, 0.0}, {-0.84, 0.3}, {-0.42, 0.78}};
    /** Profile points of the blade edges: on each plate's lip they jut out as spurs, a jagged armored edge. */
    static final int EDGE_RIGHT = 3;
    static final int EDGE_LEFT = 8;
    static final double SPUR = 1.32;
    /** Profile points that bound the open belly (facets between them glow). */
    static final int BELLY_FROM = 4;
    static final int BELLY_TO = 7;
    /** Thickness of a tentacle against its width. */
    static final double THICKNESS = 0.5;
    /** A plate's lip overlaps the plate before it by this share of a plate, flared out by {@link #LIP_FLARE}. */
    static final double LIP_BACK = 0.24;
    static final double LIP_FLARE = 1.16;
    static final double TAIL_TUCK = 0.92;

    /**
     * A tentacle's centre line: {@code samples + 1} points from the root (index 0) to the tip, each with its frame (unit
     * tangent toward the tip, dorsal "up" of the plates, and the leading edge), and the length in the spine's units.
     */
    public record Spine(double[][] p, double[][] t, double[][] n, double[][] b, double length) {
        public int samples() {
            return this.p.length - 1;
        }

        /** Interpolated frame at {@code s} (0 root .. 1 tip): {position, tangent, dorsal, edge}. */
        public double[][] at(double s) {
            int last = this.samples();
            double x = Math.max(0.0, Math.min(1.0, s)) * last;
            int i = Math.min(last - 1, (int)Math.floor(x));
            double f = x - i;
            double[] p = lerp(this.p[i], this.p[i + 1], f);
            double[] t = normalize(lerp(this.t[i], this.t[i + 1], f));
            double[] n = normalize(lerp(this.n[i], this.n[i + 1], f));
            // Keep the frame square after the blend.
            n = normalize(sub(n, mul(t, dot(n, t))));
            double[] b = normalize(lerp(this.b[i], this.b[i + 1], f));
            b = normalize(sub(sub(b, mul(t, dot(b, t))), mul(n, dot(b, n))));
            return new double[][] {p, t, n, b};
        }
    }

    /**
     * How one tentacle looks this frame.
     *
     * @param width   half-width at the root, in the spine's units
     * @param plates  armor plates along it
     * @param spikes  spikes per plate in each of the two rows (0 for none)
     * @param fine    full detail (the haze, the glints, the plate rims)
     * @param alpha   multiplies all opacity
     * @param time    seconds (the glints)
     * @param seed    varies the spikes and glints between tentacles
     * @param spikeScale multiplies spike length (0 retracted .. 1 full; above 1 bristling)
     * @param bristle    how hard a ripple runs out along the spike rows (0 none; the spikes lengthen as it passes)
     * @param bristlePhase where the ripple is (radians)
     */
    public record Look(double width, int plates, int spikes, boolean fine, double alpha, double time, int seed, double spikeScale, double bristle,
                       double bristlePhase) {
        public Look(double width, int plates, int spikes, boolean fine, double alpha, double time, int seed, double spikeScale) {
            this(width, plates, spikes, fine, alpha, time, seed, spikeScale, 0.0, 0.0);
        }
    }

    private KakujaModel() {
    }

    /** Half-width along the tentacle (share of the root's): thick at the root, a slight swell, then a long taper to the claw. */
    public static double widthAt(double s) {
        double c = Math.max(0.0, Math.min(1.0, s));
        return Math.pow(Math.max(0.0, 1.0 - Math.pow(c, 1.6)), 0.9) * (1.0 + 0.15 * Math.sin(Math.PI * c));
    }

    /** Spike length along the tentacle (share of the longest): none at the root, growing in past it, longest past the middle, tiny at the claw. */
    public static double spikeAt(double s) {
        double c = Math.max(0.0, Math.min(1.0, s));
        double rise = smooth((c - 0.1) / 0.36);
        double fall = 1.0 - 0.8 * smooth((c - 0.5) / 0.48);
        return rise * fall;
    }

    /** One armored tentacle along {@code spine}. */
    public static void tentacle(UkakuModel.Sink sink, Spine spine, Look look) {
        double alpha = clamp01(look.alpha());
        if (alpha <= 0.003 || spine.length() <= 1.0E-4 || look.width() <= 1.0E-5) {
            return;
        }
        int plates = Math.max(2, look.plates());
        double step = 1.0 / plates;
        for (int i = 0; i < plates; i++) {
            double s0 = i * step;
            double s1 = (i + 1) * step;
            boolean last = i == plates - 1;
            double[][] lip = ring(spine, look, Math.max(0.0, s0 - LIP_BACK * step), i == 0 ? 1.0 : LIP_FLARE);
            if (i > 0) {
                spur(spine, look, lip, Math.max(0.0, s0 - LIP_BACK * step));
            }
            double[][] body = ring(spine, look, s0 + 0.18 * step, i == 0 ? 1.0 : LIP_FLARE * 0.97);
            double[][] tail = last ? claw(spine, look) : ring(spine, look, s1, TAIL_TUCK);
            double[] centre = spine.at(s0 + 0.5 * step)[0];
            band(sink, lip, body, centre, 0, alpha);
            band(sink, body, tail, centre, 1, alpha);
            if (i > 0 && look.fine()) {
                rim(sink, spine, look, lip, s0, centre, alpha);
            }
        }
        if (look.spikes() > 0 && look.spikeScale() > 0.01) {
            spikes(sink, spine, look, alpha);
        }
        if (look.fine()) {
            haze(sink, spine, look, alpha);
        }
    }

    /** The plate cross-section at {@code s}, {@code scale} times the tentacle's width there. */
    static double[][] ring(Spine spine, Look look, double s, double scale) {
        double[][] f = spine.at(s);
        double w = look.width() * widthAt(s) * scale;
        double h = w * THICKNESS;
        double[][] out = new double[PROFILE.length][];
        for (int k = 0; k < PROFILE.length; k++) {
            out[k] = add(f[0], add(mul(f[3], PROFILE[k][0] * w), mul(f[2], PROFILE[k][1] * h)));
        }
        return out;
    }

    /** The lip's blade edges pushed out and raked back toward the root: each plate ends in a spur on both edges. */
    static void spur(Spine spine, Look look, double[][] lip, double s) {
        double[][] f = spine.at(s);
        double w = look.width() * widthAt(s) * LIP_FLARE;
        for (int side = -1; side <= 1; side += 2) {
            int k = side > 0 ? EDGE_RIGHT : EDGE_LEFT;
            lip[k] = add(lip[k], add(mul(f[3], side * (SPUR - 1.0) * w), mul(f[1], -0.22 * w)));
        }
    }

    /** The last plate closes into a claw: every profile point meets at the point, hooked a little toward the belly. */
    static double[][] claw(Spine spine, Look look) {
        double[][] f = spine.at(1.0);
        double hook = look.width() * 0.22;
        double[] point = add(add(f[0], mul(f[1], look.width() * 0.25)), mul(f[2], -hook));
        double[][] out = new double[PROFILE.length][];
        for (int k = 0; k < PROFILE.length; k++) {
            out[k] = point;
        }
        return out;
    }

    /** Facets between two rings: steel on the back and flanks (lit), glowing RC on the belly. {@code part} 0 lip, 1 plate. */
    static void band(UkakuModel.Sink sink, double[][] from, double[][] to, double[] centre, int part, double alpha) {
        int count = PROFILE.length;
        for (int k = 0; k < count; k++) {
            int j = (k + 1) % count;
            boolean belly = k >= BELLY_FROM && k < BELLY_TO;
            if (belly) {
                int edge = argb(BELLY, alpha);
                int core = argb(BELLY_GLOW, alpha);
                glow(sink, from[k], to[k], to[j], from[j], k == BELLY_FROM ? edge : core, k == BELLY_FROM ? edge : core,
                    j == BELLY_TO ? edge : core, j == BELLY_TO ? edge : core);
                continue;
            }
            facet(sink, from[k], to[k], to[j], from[j], centre, argb(steel(k, part, true), alpha), argb(steel(k, part, false), alpha),
                argb(steel(j, part, false), alpha), argb(steel(j, part, true), alpha));
        }
    }

    /** Steel colour of profile point {@code k}: the ridge pale, the shoulders lit, the blade edges rimmed on the lip, the flanks dark. */
    static int steel(int k, int part, boolean front) {
        int base = switch (k) {
            case 0 -> RIDGE;
            case 1, 10 -> mix(PLATE, PLATE_HI, 0.75);
            case 2, 9 -> PLATE;
            case 3, 8 -> mix(PLATE, PLATE_DARK, 0.3);
            default -> PLATE_DARK;
        };
        if (part == 0 && front) {
            // The leading lip of each plate catches the light: a cold blue line along every plate's edge.
            return mix(base, RIM, k == 0 ? 0.55 : k == 3 || k == 8 ? 0.7 : k <= 2 || k >= 9 ? 0.45 : 0.15);
        }
        if (part == 1 && !front) {
            // The back of a plate sits in the shadow of the next plate's lip.
            return mix(base, PLATE_DARK, 0.45);
        }
        return base;
    }

    /** The face of a lip turned toward the root: a thin bright rim where one plate rides over the last. */
    static void rim(UkakuModel.Sink sink, Spine spine, Look look, double[][] lip, double s, double[] centre, double alpha) {
        double[][] inner = ring(spine, look, Math.max(0.0, s - LIP_BACK / Math.max(2, look.plates())), TAIL_TUCK * 0.98);
        for (int k = 0; k < PROFILE.length; k++) {
            int j = (k + 1) % PROFILE.length;
            if (k >= BELLY_FROM && k < BELLY_TO) {
                continue;
            }
            facet(sink, inner[k], lip[k], lip[j], inner[j], centre, argb(PLATE_DARK, alpha), argb(mix(PLATE_HI, RIM, 0.5), alpha),
                argb(mix(PLATE_HI, RIM, 0.5), alpha), argb(PLATE_DARK, alpha));
        }
    }

    /**
     * The two rows of crystal spikes along the belly: staggered, splayed a little outward from the centre line and raked
     * toward the tip, glowing magenta from root to a pale pink point.
     */
    static void spikes(UkakuModel.Sink sink, Spine spine, Look look, double alpha) {
        int plates = Math.max(2, look.plates());
        int perRow = Math.max(1, look.spikes());
        double longest = look.width() * 1.9 * look.spikeScale();
        int index = 0;
        for (int row = -1; row <= 1; row += 2) {
            int count = plates * perRow;
            for (int i = 0; i < count; i++) {
                double s = (i + (row > 0 ? 0.3 : 0.8)) / count;
                if (s > 0.94) {
                    continue;
                }
                double reach = spikeAt(s);
                if (reach < 0.06) {
                    continue;
                }
                int seed = look.seed() * 977 + index++ * 31 + (row > 0 ? 7 : 0);
                double ripple = 1.0 + look.bristle() * Math.max(0.0, Math.sin(look.bristlePhase() - s * 9.0 - (row > 0 ? 0.0 : 0.5)));
                double length = longest * reach * ripple * (0.82 + 0.3 * UkakuModel.hash(seed));
                double[][] f = spine.at(s);
                double w = look.width() * widthAt(s);
                double[] base = add(f[0], add(mul(f[3], row * 0.3 * w), mul(f[2], -0.5 * w * THICKNESS)));
                double splay = Math.toRadians(16.0 + 10.0 * UkakuModel.hash(seed + 1));
                double rake = Math.toRadians(30.0 + 12.0 * UkakuModel.hash(seed + 2));
                double[] down = normalize(add(mul(f[2], -Math.cos(splay)), mul(f[3], row * Math.sin(splay))));
                double[] dir = normalize(add(mul(down, Math.cos(rake)), mul(f[1], Math.sin(rake))));
                double radius = Math.min(0.3 * w, 0.13 * length);
                spike(sink, base, dir, f[1], length, radius, alpha);
            }
        }
        if (look.fine()) {
            glints(sink, spine, look, longest, alpha);
        }
    }

    /** Nominal splay of a spike out from the belly's centre line and rake toward the claw (degrees), as drawn on average. */
    static final double SPLAY = 21.0;
    static final double RAKE = 36.0;

    /**
     * The point of a spike {@code s} along {@code spine} in row {@code row} (+1 or -1) of a tentacle {@code width} wide at the
     * root, as drawn on average (no per-spike variation): {point, direction it points}.
     */
    public static double[][] spikeTip(Spine spine, double width, int row, double s) {
        double[][] f = spine.at(s);
        double w = width * widthAt(s);
        double[] base = add(f[0], add(mul(f[3], row * 0.3 * w), mul(f[2], -0.5 * w * THICKNESS)));
        double splay = Math.toRadians(SPLAY);
        double rake = Math.toRadians(RAKE);
        double[] down = normalize(add(mul(f[2], -Math.cos(splay)), mul(f[3], row * Math.sin(splay))));
        double[] dir = normalize(add(mul(down, Math.cos(rake)), mul(f[1], Math.sin(rake))));
        double length = width * 1.9 * spikeAt(s);
        return new double[][] {add(base, mul(dir, length)), dir};
    }

    /** One spike: a four-sided crystal, its base a diamond drawn out along the tentacle. */
    static void spike(UkakuModel.Sink sink, double[] base, double[] dir, double[] along, double length, double radius, double alpha) {
        double[] a1 = normalize(sub(along, mul(dir, dot(along, dir))));
        double[] a2 = cross(dir, a1);
        double[] tip = add(base, mul(dir, length));
        // The base sits a little inside the belly, so no collar is needed: four long faces, dark at the root, pale at the point.
        double[] root = add(base, mul(dir, -0.04 * length));
        double[][] corners = {
            add(root, mul(a1, radius * 1.35)), add(root, mul(a2, radius * 0.75)), add(root, mul(a1, -radius * 1.35)), add(root, mul(a2, -radius * 0.75))};
        int dark = argb(SPIKE_ROOT, alpha);
        int body = argb(SPIKE, alpha);
        int point = argb(SPIKE_TIP, alpha);
        for (int k = 0; k < 4; k++) {
            double[] c0 = corners[k];
            double[] c1 = corners[(k + 1) % 4];
            boolean lit = k == 0 || k == 1;
            int foot = lit ? mix2(dark, body, 0.6) : dark;
            glow(sink, c0, tip, tip, c1, foot, argb(lit ? SPIKE_WHITE : SPIKE_TIP, alpha), point, foot);
        }
    }

    /** A few spike points glint, twinkling in turn. */
    static void glints(UkakuModel.Sink sink, Spine spine, Look look, double longest, double alpha) {
        for (int g = 0; g < 2; g++) {
            double phase = look.time() * (1.7 + 0.4 * g) + look.seed() * 2.3 + g * 3.1;
            double twinkle = Math.pow(Math.max(0.0, Math.sin(phase)), 10.0);
            if (twinkle < 0.02) {
                continue;
            }
            double s = 0.3 + 0.45 * UkakuModel.hash(look.seed() * 13 + g + (int)Math.floor(phase / (2.0 * Math.PI)) * 7);
            double[][] f = spine.at(s);
            double w = look.width() * widthAt(s);
            double[] at = add(f[0], mul(f[2], -0.5 * w * THICKNESS - longest * spikeAt(s) * 0.75));
            star(sink, at, longest * 0.45 * twinkle, f[1], f[3], twinkle * alpha);
        }
    }

    /** The faint violet haze along both blade edges. */
    static void haze(UkakuModel.Sink sink, Spine spine, Look look, double alpha) {
        int steps = Math.max(4, look.plates() * 2);
        for (int side = -1; side <= 1; side += 2) {
            double[] prevIn = null;
            double[] prevOut = null;
            double prevStrength = 0.0;
            for (int i = 0; i <= steps; i++) {
                double s = i / (double)steps * 0.97;
                double[][] f = spine.at(s);
                double w = look.width() * widthAt(s);
                double[] in = add(f[0], mul(f[3], side * w));
                double[] out = add(in, add(mul(f[3], side * (0.45 * w + 0.04)), mul(f[2], 0.12 * w)));
                double strength = 0.32 * smooth(s / 0.12) * (1.0 - smooth((s - 0.75) / 0.25));
                if (prevIn != null) {
                    glow(sink, prevIn, in, out, prevOut, argb(HAZE, prevStrength * alpha), argb(HAZE, strength * alpha), argb(HAZE, 0.0),
                        argb(HAZE, 0.0));
                }
                prevIn = in;
                prevOut = out;
                prevStrength = strength;
            }
        }
    }

    /** A four-point star flash in the plane of {@code u} and {@code v}, and across it. */
    static void star(UkakuModel.Sink sink, double[] at, double size, double[] u, double[] v, double strength) {
        int core = argb(SPIKE_WHITE, Math.min(1.0, strength));
        int clear = argb(SPIKE_TIP, 0.0);
        double[] w = cross(u, v);
        double[][][] planes = {{u, v}, {u, w}};
        for (double[][] plane : planes) {
            for (int arm = 0; arm < 4; arm++) {
                double angle = Math.PI / 2.0 * arm;
                double reach = size * (arm % 2 == 0 ? 1.0 : 0.6);
                double[] dir = add(mul(plane[0], Math.cos(angle)), mul(plane[1], Math.sin(angle)));
                double[] side = add(mul(plane[0], Math.cos(angle + Math.PI / 2.0)), mul(plane[1], Math.sin(angle + Math.PI / 2.0)));
                double[] tip = add(at, mul(dir, reach));
                glow(sink, at, add(at, mul(side, size * 0.12)), tip, add(at, mul(side, -size * 0.12)), core, core, clear, core);
            }
        }
    }

    // ------------------------------------------------------------------ armor on the body (player model pixels)

    /**
     * The kakuja's horns, in head pixels (x to the player's left, y down from the top of the neck, z backward; the head box
     * is -4..4, -8..0, -4..4): two red horns from the top corners of the brow, rising and sweeping back, a little outward.
     * {@code grow} 0..1 brings them out.
     */
    public static void horns(UkakuModel.Sink sink, double grow, double alpha) {
        double g = smooth(grow);
        if (g <= 0.01 || alpha <= 0.003) {
            return;
        }
        for (int side = -1; side <= 1; side += 2) {
            double[] root = {side * 2.55, -7.35, -2.9};
            double[] t = normalize(new double[] {side * 0.42, -1.0, -0.12});
            double[] bendTo = normalize(new double[] {side * 0.1, 0.0, 1.0});
            int segments = 6;
            int sides = 6;
            double length = 5.8 * g;
            double radius = 1.05;
            double[][] prev = null;
            double[] p = root;
            double[] dir = t;
            for (int i = 0; i <= segments; i++) {
                double s = i / (double)segments;
                double r = radius * Math.pow(1.0 - s, 0.85) * (0.4 + 0.6 * g);
                double[] a1 = normalize(cross(dir, new double[] {0.0, 0.0, 1.0}));
                double[] a2 = cross(dir, a1);
                double[][] ring = new double[sides][];
                for (int k = 0; k < sides; k++) {
                    double angle = 2.0 * Math.PI * k / sides;
                    ring[k] = add(p, add(mul(a1, Math.cos(angle) * r), mul(a2, Math.sin(angle) * r)));
                }
                if (prev != null) {
                    int c0 = argb(mix(HORN_ROOT, HORN, Math.min(1.0, (s - 1.0 / segments) * 2.2)), alpha);
                    int c1 = argb(s > 0.55 ? mix(HORN, HORN_TIP, (s - 0.55) / 0.45) : mix(HORN_ROOT, HORN, Math.min(1.0, s * 2.2)), alpha);
                    double[] centre = mul(add(prev[0], ring[sides / 2]), 0.5);
                    for (int k = 0; k < sides; k++) {
                        int j = (k + 1) % sides;
                        facet(sink, prev[k], ring[k], ring[j], prev[j], centre, c0, c1, c1, c0);
                    }
                }
                prev = ring;
                // Curve back as it rises.
                dir = normalize(add(dir, mul(bendTo, 0.16)));
                p = add(p, mul(dir, length / segments));
            }
        }
    }

    /**
     * The kakuja's mask over the face, in head pixels: a dark angular faceplate with a ridge down the middle, slanted eye
     * openings where the eyes are ({@code eyes}: right eye column, row, left eye column, row on the 8x8 face; each eye two
     * pixels wide and tall, as {@code KakuganEyes}), and a grille over the mouth. {@code grow} 0..1 spreads it over the face.
     */
    public static void mask(UkakuModel.Sink sink, int[] eyes, double grow, double alpha) {
        double g = smooth(grow);
        if (g <= 0.01 || alpha <= 0.003) {
            return;
        }
        int cells = 16;
        double cell = 8.8 / cells;
        double[] centre = {0.0, -4.0, 0.0};
        for (int cx = 0; cx < cells; cx++) {
            for (int cy = 0; cy < cells; cy++) {
                double x0 = -4.4 + cx * cell;
                double x1 = x0 + cell;
                double y0 = -8.2 + cy * cell;
                double y1 = y0 + cell;
                double mx = (x0 + x1) / 2.0;
                double my = (y0 + y1) / 2.0;
                // It spreads out from the nose bridge.
                if (Math.hypot(mx / 4.4, (my + 3.6) / 4.4) > g * 1.5) {
                    continue;
                }
                if (eyeHole(eyes, mx, my)) {
                    continue;
                }
                double[] a = maskPoint(x0, y0);
                double[] b = maskPoint(x1, y0);
                double[] c = maskPoint(x1, y1);
                double[] d = maskPoint(x0, y1);
                boolean grille = my > -1.7 && Math.abs(mx) < 2.6 && (cx % 2 == 0);
                int colour = grille ? mix(MASK, 0x000000, 0.6) : mix(MASK, MASK_HI, Math.max(0.0, 1.0 - Math.abs(mx) / 1.2) * 0.8);
                int col = argb(colour, alpha);
                facet(sink, a, b, c, d, centre, col, col, col, col);
            }
        }
        // Wrap the plate's edges back over the sides of the head.
        for (int side = -1; side <= 1; side += 2) {
            double x = side * 4.4;
            for (int cy = 0; cy < cells; cy++) {
                double y0 = -8.2 + cy * cell;
                double y1 = y0 + cell;
                if (Math.hypot(1.0, ((y0 + y1) / 2.0 + 3.6) / 4.4) > g * 1.5) {
                    continue;
                }
                double[] front0 = maskPoint(x, y0);
                double[] front1 = maskPoint(x, y1);
                double[] back0 = {side * 4.56, y0, -2.2};
                double[] back1 = {side * 4.56, y1, -2.2};
                int col = argb(MASK, alpha);
                facet(sink, front0, back0, back1, front1, centre, col, col, col, col);
            }
        }
    }

    /** True inside a slanted eye opening (a pixel of margin round each eye, the inner top corner cut lower: a glare). */
    static boolean eyeHole(int[] eyes, double x, double y) {
        for (int e = 0; e < 2; e++) {
            int column = eyes[e * 2];
            int row = eyes[e * 2 + 1];
            // Face column c (seen from the front, 0 on the viewer's left = the player's right) spans model x from c - 4.
            double left = column - 4.0 - 0.35;
            double right = column - 4.0 + 2.0 + 0.35;
            double top = row - 8.0 - 0.45;
            double bottom = row - 8.0 + 2.0 + 0.25;
            if (x < left || x > right || y > bottom) {
                continue;
            }
            // Toward the nose the brow comes down a little.
            double inner = e == 0 ? (x - left) / (right - left) : (right - x) / (right - left);
            if (y >= top + 0.55 * inner) {
                return true;
            }
        }
        return false;
    }

    /** A point on the faceplate: in front of the face (and the skin's outer layer), with a ridge down the middle. */
    static double[] maskPoint(double x, double y) {
        double ridge = 0.55 * Math.max(0.0, 1.0 - Math.abs(x) / 2.2);
        double brow = y < -4.6 ? 0.25 * smooth((-4.6 - y) / 1.2) * (1.0 - smooth((-7.2 - y) / 1.0)) : 0.0;
        double cheek = 0.3 * smooth((Math.abs(x) - 2.6) / 1.8);
        return new double[] {x, y, -4.62 - ridge - brow + cheek};
    }

    /**
     * The final kakuja's half mask, in head pixels (as {@link #mask}): Ukaku's own mask, over the lower face only, leaving
     * the eyes (and the kakugan in them) bare. Black lacquer drawn to a sharp beak down the middle, its ridge broken into
     * stepped lips; the cheeks wrap back along the jaw, and from each a pointed wing sweeps back and up past the ear; three
     * slanted vents on each side glow a dim red; steel rivets on the beak and by the wings. Its top edge sits just under
     * the eyes wherever they are ({@code eyes} as {@link #mask}) and rises to a point over the nose bridge between them.
     * {@code grow} 0..1 spreads it out from the beak; the wings come last. Fitted over the skin's outer layer as vanilla draws it.
     */
    public static void halfMask(UkakuModel.Sink sink, int[] eyes, double grow, double alpha) {
        halfMask(sink, eyes, grow, alpha, OUTER_LAYER);
    }

    /**
     * 6.0.2: the half mask fitted over {@code shell} (head pixels out from the head's centre line, to the front and the sides): the
     * face itself ({@link #FACE}), the skin's outer layer ({@link #OUTER_LAYER}) or 3D Skin Layers' voxels ({@link #VOXEL_LAYER}),
     * whichever is really under it ({@link #halfMaskShell}). Its surface lies {@link #MASK_GAP} off that, front and sides, turning
     * the corner of the face round a small radius; nothing of it is scaled. Until 6.0.2 it stood out past the outer layer at any
     * face (its sides 4.75 to 4.9 pixels out, half a pixel wider than the head and its outer layer each side), and under 3D Skin
     * Layers it was stretched 1.18 wide and 1.3 deep about the head's middle: a band floating two pixels off the face.
     */
    public static void halfMask(UkakuModel.Sink sink, int[] eyes, double grow, double alpha, double shell) {
        double g = smooth(grow);
        if (g <= 0.01 || alpha <= 0.003) {
            return;
        }
        double[] edge = halfMaskEdge(eyes);
        double top = edge[0];
        double half = shell + MASK_GAP;
        double[] centre = {0.0, -4.0, 0.0};
        double[] xs = halfMaskColumns(half);
        int columns = xs.length - 1;
        int rows = 12;
        double[][][] grid = new double[columns + 1][rows + 1][];
        for (int cx = 0; cx <= columns; cx++) {
            for (int cy = 0; cy <= rows; cy++) {
                grid[cx][cy] = halfMaskPoint(edge, xs[cx], cy / (double)rows, shell);
            }
        }
        for (int cx = 0; cx < columns; cx++) {
            for (int cy = 0; cy < rows; cy++) {
                double mx = 0.5 * (xs[cx] + xs[cx + 1]);
                double t = (cy + 0.5) / rows;
                // It spreads out from the beak.
                if (Math.hypot(mx / half, t - 0.4) > g * 1.6) {
                    continue;
                }
                double ridge = Math.max(0.0, 1.0 - Math.abs(mx) / HALF_MASK_BEAK);
                boolean lip = ridge > 0.0 && (cy + 1) % 3 == 0 && cy < rows - 1;
                int colour = mix(MASK, MASK_HI, 0.75 * Math.pow(ridge, 3.0) + (lip ? 0.45 * ridge : 0.0));
                int col = argb(colour, alpha);
                facet(sink, grid[cx][cy], grid[cx + 1][cy], grid[cx + 1][cy + 1], grid[cx][cy + 1], centre, col, col, col, col);
            }
        }
        double wings = smooth((g - 0.55) / 0.45);
        for (int side = -1; side <= 1; side += 2) {
            jaw(sink, edge, side, g, alpha, shell);
            vents(sink, edge, side, g, alpha, shell);
            if (wings > 0.01) {
                cheekWing(sink, top, side, wings, alpha, half);
            }
            rivet(sink, new double[] {side * half, top + 0.75, -2.5}, new double[] {side, 0.0, 0.0}, 0.2 * g, alpha);
            rivet(sink, new double[] {side * half, top + 1.75, -1.75}, new double[] {side, 0.0, 0.0}, 0.2 * g, alpha);
        }
        // Rivets down the beak, under the nose point.
        for (int k = 0; k < 2; k++) {
            double t = 0.18 + 0.3 * k;
            double[] at = halfMaskPoint(edge, edge[1], t, shell);
            rivet(sink, at, new double[] {0.0, 0.0, -1.0}, 0.17 * g, alpha);
        }
    }

    /**
     * 6.0.2: what the half mask can be fitted over, head pixels out from the head's centre line: the face (the 8-pixel head box),
     * the skin's outer layer as vanilla draws it (half a pixel out), and 3D Skin Layers' voxels (that layer as one-pixel voxels on
     * the head box, scaled 1.18 about the head's centre, so their outer faces stand 4.72 out).
     */
    public static final double FACE = 4.0;
    public static final double OUTER_LAYER = 4.5;
    public static final double VOXEL_LAYER = 4.72;
    /** How far the half mask's surface lies off what it is fitted over. */
    public static final double MASK_GAP = 0.12;
    /** The radius the half mask turns the corner of the face round, front to side (inside {@code 2 * MASK_GAP / (2 - sqrt 2)}). */
    static final double MASK_CORNER = 0.3;

    /**
     * The half mask's columns across its front, {@code half} out each side: even across the flat of the face, and round each
     * corner by even turns of its radius, so no facet cuts the corner it turns (a straight one would leave the head's own corner
     * standing through it).
     */
    static double[] halfMaskColumns(double half) {
        int flat = 16;
        int arc = 4;
        double inner = half - MASK_CORNER;
        double[] xs = new double[flat + 1 + 2 * arc];
        int k = 0;
        for (int a = arc; a >= 1; a--) {
            xs[k++] = -(inner + MASK_CORNER * Math.sin(a * Math.PI / 2.0 / arc));
        }
        for (int f = 0; f <= flat; f++) {
            xs[k++] = -inner + 2.0 * inner * f / flat;
        }
        for (int a = 1; a <= arc; a++) {
            xs[k++] = inner + MASK_CORNER * Math.sin(a * Math.PI / 2.0 / arc);
        }
        return xs;
    }

    /**
     * 6.0.2: what the half mask is fitted over for these eyes: the face itself when the skin's outer layer covers none of the
     * face rows it lies on ({@code covered}, bit {@code row * 8 + column}, as {@code KakuganSkin.covered}); otherwise that layer,
     * as 3D Skin Layers draws it ({@code voxels}) or as vanilla does. A skin not read yet ({@code read} false) counts as covered.
     */
    public static double halfMaskShell(int[] eyes, long covered, boolean read, boolean voxels) {
        double[] edge = halfMaskEdge(eyes);
        int from = Math.max(0, Math.min(7, (int)Math.floor(edge[0] - edge[3] + 8.0)));
        long rows = 0L;
        for (int row = from; row < 8; row++) {
            rows |= 0xFFL << (row * 8);
        }
        if (read && (covered & rows) == 0L) {
            return FACE;
        }
        return voxels ? VOXEL_LAYER : OUTER_LAYER;
    }

    /** Half-width of the half mask's beak, and how far its bottom edge comes below the chin of a bare face (head pixels). */
    static final double HALF_MASK_BEAK = 2.4;
    static final double HALF_MASK_BOTTOM = 0.45;
    /** How far down the half mask (top 0, bottom edge 1) it reaches the chin of what it covers: its last row of facets. */
    static final double HALF_MASK_CHIN = 11.0 / 12.0;

    /**
     * The half mask's top edge for these eyes: {top under the eyes, x of the nose point between them, its half-width, how
     * far it rises}.
     */
    static double[] halfMaskEdge(int[] eyes) {
        double bottom = -8.0;
        for (int e = 0; e < 2; e++) {
            bottom = Math.max(bottom, eyes[e * 2 + 1] - 8.0 + 2.0);
        }
        double top = Math.min(-0.6, bottom + 0.3);
        // Between the eyes: the inner edge of the eye nearer the viewer's left to the inner edge of the other.
        double inner0 = Math.min(eyes[0], eyes[2]) - 4.0 + 2.0;
        double inner1 = Math.max(eyes[0], eyes[2]) - 4.0;
        double half = Math.max(0.0, (inner1 - inner0) / 2.0 - 0.25);
        double rise = half >= 0.3 ? 1.3 : 0.0;
        return new double[] {top, (inner0 + inner1) / 2.0, half, rise};
    }

    /** The top edge's height at {@code x}: under the eyes, rising to the nose point. */
    static double halfMaskTop(double[] edge, double x) {
        double half = edge[2];
        double point = half > 0.0 ? Math.max(0.0, 1.0 - Math.abs(x - edge[1]) / half) : 0.0;
        return edge[0] - edge[3] * point;
    }

    /** {@link #halfMaskPoint(double[], double, double, double)} over the skin's outer layer. */
    static double[] halfMaskPoint(double[] edge, double x, double t) {
        return halfMaskPoint(edge, x, t, OUTER_LAYER);
    }

    /**
     * A point on the half mask's front at {@code x}, {@code t} from its top edge (0) to its bottom (1), fitted over {@code shell}:
     * {@link #MASK_GAP} in front of it, drawn out into the beak down the middle (sharp, stepped into lips), turning the corner of
     * the face round {@link #MASK_CORNER} at the cheeks, and pulled in under the chin (never into what it covers).
     */
    static double[] halfMaskPoint(double[] edge, double x, double t, double shell) {
        double half = shell + MASK_GAP;
        double top = halfMaskTop(edge, x);
        // Down to the chin of what it covers by HALF_MASK_CHIN of the way, then a lip tucked in under it to its bottom edge (the
        // outer layer's chin comes half a pixel lower than the face's): the tuck stays wholly below that chin, so it never cuts it.
        double chinY = shell - FACE;
        double y = t <= HALF_MASK_CHIN ? top + (chinY - top) * t / HALF_MASK_CHIN
            : chinY + (Math.max(HALF_MASK_BOTTOM, chinY + MASK_GAP) - chinY) * (t - HALF_MASK_CHIN) / (1.0 - HALF_MASK_CHIN);
        double ridge = Math.max(0.0, 1.0 - Math.abs(x) / HALF_MASK_BEAK);
        double profile = 0.35 + 0.65 * smooth(t / 0.4) * (1.0 - 0.55 * smooth((t - 0.6) / 0.4));
        // The beak's ridge breaks into stepped lips, like a raptor's.
        double steps = 0.14 * ((t * 4.0) % 1.0) * smooth(t / 0.2);
        double beak = ridge * (1.7 * profile + steps);
        double z = -half - beak + 0.3 * smooth((t - HALF_MASK_CHIN) / (1.0 - HALF_MASK_CHIN));
        // The cheeks turn the corner of the face, front to side, round a small radius.
        double inner = half - MASK_CORNER;
        double out = Math.abs(x) - inner;
        if (out > 0.0) {
            z = Math.max(z, -inner - Math.sqrt(Math.max(0.0, MASK_CORNER * MASK_CORNER - out * out)));
        }
        // Over the face (above the chin of what it covers), never closer to it than a hair.
        if (y < chinY && Math.abs(x) < shell) {
            z = Math.min(z, -shell - 0.03);
        }
        return new double[] {x, y, z};
    }

    /** The side of the half mask, from its front edge back along the jaw, {@link #MASK_GAP} off the side of what it covers. */
    static void jaw(UkakuModel.Sink sink, double[] edge, int side, double grow, double alpha, double shell) {
        int rows = 8;
        double half = shell + MASK_GAP;
        double[] centre = {0.0, -4.0, 0.0};
        int col = argb(mix(MASK, MASK_HI, 0.2), alpha);
        double front = -(half - MASK_CORNER);
        double back = front + (-1.1 - front) * smooth((grow - 0.3) / 0.5);
        for (int k = 0; k < rows; k++) {
            double t0 = k / (double)rows;
            double t1 = (k + 1) / (double)rows;
            double[] f0 = halfMaskPoint(edge, side * half, t0, shell);
            double[] f1 = halfMaskPoint(edge, side * half, t1, shell);
            // The jaw line slopes up toward the ear.
            double[] b0 = {side * half, f0[1] - 0.3 * t0, Math.max(f0[2], back)};
            double[] b1 = {side * half, f1[1] - 0.3 * t1 - (k == rows - 1 ? 0.6 : 0.0), Math.max(f1[2], back)};
            facet(sink, f0, b0, b1, f1, centre, col, col, col, col);
        }
    }

    /** Three slanted vents on one side of the beak: dark slits, a dim red glow inside each. */
    static void vents(UkakuModel.Sink sink, double[] edge, int side, double grow, double alpha, double shell) {
        if (grow < 0.35) {
            return;
        }
        double show = smooth((grow - 0.35) / 0.3) * alpha;
        for (int v = 0; v < 3; v++) {
            double t0 = 0.42 + 0.14 * v;
            double t1 = t0 + 0.06;
            double x0 = side * 2.55;
            double x1 = side * 3.75;
            // Slanted down toward the back, along the jaw.
            double[] a = lift(halfMaskPoint(edge, x0, t0, shell));
            double[] b = lift(halfMaskPoint(edge, x1, t0 + 0.1, shell));
            double[] c = lift(halfMaskPoint(edge, x1, t1 + 0.1, shell));
            double[] d = lift(halfMaskPoint(edge, x0, t1, shell));
            int slit = argb(VENT, show);
            glow(sink, a, b, c, d, slit, slit, slit, slit);
            double[] ma = lerp(a, d, 0.35);
            double[] mb = lerp(b, c, 0.35);
            double[] mc = lerp(b, c, 0.65);
            double[] md = lerp(a, d, 0.65);
            int hot = argb(VENT_GLOW, 0.85 * show);
            int dim = argb(VENT_GLOW, 0.35 * show);
            glow(sink, lift(ma), lift(mb), lift(mc), lift(md), hot, dim, dim, hot);
        }
    }

    /** A point lifted a hair off the mask's front, so a detail on it never fights the surface. */
    static double[] lift(double[] p) {
        return new double[] {p[0], p[1], p[2] - 0.04};
    }

    /** The pointed wing sweeping back and up from one cheek past the ear, with a short spur under it; rooted on the jaw at {@code half}. */
    static void cheekWing(UkakuModel.Sink sink, double top, int side, double grow, double alpha, double half) {
        double[] root0 = {side * half, top + 0.15, -2.75};
        double[] root1 = {side * half, top + 1.9, -1.35};
        double[] tip = {side * (half + 1.7 * grow), top + 0.15 - 2.9 * grow, -2.0 + 4.3 * grow};
        blade(sink, root0, root1, tip, side, 0.28, alpha, true);
        double[] spur0 = {side * half, top + 1.5, -1.9};
        double[] spur1 = {side * half, top + 2.4, -1.15};
        double[] spurTip = {side * (half + 1.0 * grow), top + 1.2 - 0.8 * grow, -1.4 + 2.6 * grow};
        blade(sink, spur0, spur1, spurTip, side, 0.2, alpha, false);
    }

    /** A thin pointed blade from the root edge {@code r0}-{@code r1} to {@code tip}, {@code thick} pixels thick outward. */
    static void blade(UkakuModel.Sink sink, double[] r0, double[] r1, double[] tip, int side, double thick, double alpha, boolean edgeLit) {
        double[] out = {side * thick, 0.0, 0.0};
        double[] o0 = add(r0, out);
        double[] o1 = add(r1, out);
        double[] ot = add(tip, mul(out, 0.3));
        double[] mid = mul(add(add(r0, r1), tip), 1.0 / 3.0);
        double[] centre = add(mid, mul(out, 0.5));
        int face = argb(mix(MASK, MASK_HI, 0.35), alpha);
        int dark = argb(MASK, alpha);
        int lit = argb(edgeLit ? mix(MASK_HI, RIVET, 0.45) : MASK_HI, alpha);
        // Outer and inner faces, then the two long edges (the upper one catching the light).
        facet(sink, o0, o1, ot, ot, centre, face, face, lit, lit);
        facet(sink, r0, r1, tip, tip, centre, dark, dark, dark, dark);
        facet(sink, r0, o0, ot, tip, centre, lit, lit, lit, lit);
        facet(sink, r1, o1, ot, tip, centre, dark, dark, face, face);
    }

    /** A steel rivet: a small four-sided stud standing out along {@code normal}. */
    static void rivet(UkakuModel.Sink sink, double[] at, double[] normal, double size, double alpha) {
        if (size <= 0.01) {
            return;
        }
        double[] n = normalize(normal);
        double[] u = Math.abs(n[1]) < 0.9 ? cross(n, new double[] {0.0, 1.0, 0.0}) : cross(n, new double[] {1.0, 0.0, 0.0});
        double[] v = cross(n, u);
        double[] peak = add(at, mul(n, size * 0.9));
        double[] base = add(at, mul(n, -0.05));
        double[][] corners = {add(base, mul(u, size)), add(base, mul(v, size)), add(base, mul(u, -size)), add(base, mul(v, -size))};
        double[] centre = add(at, mul(n, -1.0));
        int hi = argb(RIVET, alpha);
        int lo = argb(mix(RIVET, MASK, 0.55), alpha);
        for (int k = 0; k < 4; k++) {
            facet(sink, corners[k], corners[(k + 1) % 4], peak, peak, centre, k < 2 ? hi : lo, k < 2 ? hi : lo, hi, hi);
        }
    }

    /**
     * The kakuja's chest armor, in torso pixels (x to the player's left, y down from the neck, z backward; the torso box is
     * -4..4, 0..12, -2..2): pectoral plates and three rows of stacked plates over the stomach in front, angled rib plates
     * down the sides, and over the shoulder blades a spine ridge with the six sockets the tentacles grow from, glowing.
     */
    public static void chest(UkakuModel.Sink sink, double grow, double alpha) {
        chest(sink, grow, alpha, false);
    }

    /** {@link #chest(UkakuModel.Sink, double, double)}; in the final kakuja the left sockets burn with the fire tendrils' heat. */
    public static void chest(UkakuModel.Sink sink, double grow, double alpha, boolean finalForm) {
        double g = smooth(grow);
        if (g <= 0.01 || alpha <= 0.003) {
            return;
        }
        double lift = 0.15 + 0.5 * g;
        for (int side = -1; side <= 1; side += 2) {
            // Pectorals: the lower edge angled up toward the arms.
            plate(sink, new double[][] {{side * 0.25, 0.5}, {side * 3.95, 0.5}, {side * 3.95, 3.6}, {side * 0.25, 4.3}}, -2.0, -lift - 0.25, 0.35, alpha,
                side);
            for (int row = 0; row < 3; row++) {
                double y0 = 4.65 + row * 2.2;
                plate(sink, new double[][] {{side * 0.3, y0}, {side * (2.75 - 0.15 * row), y0}, {side * (2.6 - 0.15 * row), y0 + 1.85},
                    {side * 0.3, y0 + 1.85}}, -2.0, -lift - 0.1, 0.3, alpha, side);
            }
            // Ribs down the flanks of the front.
            for (int rib = 0; rib < 3; rib++) {
                double y0 = 4.8 + rib * 2.1;
                plate(sink, new double[][] {{side * 3.0, y0 + 0.4}, {side * 3.95, y0}, {side * 3.95, y0 + 1.4}, {side * 3.0, y0 + 1.75}}, -2.0,
                    -lift * 0.7, 0.25, alpha, side);
            }
        }
        // Back: the spine ridge, and a glowing socket round each tentacle's root.
        plateBack(sink, new double[][] {{-0.7, 0.4}, {0.7, 0.4}, {0.6, 9.5}, {-0.6, 9.5}}, 2.0, 2.0 + lift + 0.35, 0.3, alpha);
        for (int side = -1; side <= 1; side += 2) {
            for (int tier = 0; tier < 3; tier++) {
                double[] root = KakujaRig.rootPixels(side, tier);
                socket(sink, root[0], root[1], 2.0 + lift * 0.6, 1.25 - 0.12 * tier, g, alpha, finalForm && side < 0);
            }
        }
    }

    /** A raised front plate: the outline {@code corners} (x, y) at depth {@code base}, its face at {@code top}, bevelled by {@code bevel}. */
    static void plate(UkakuModel.Sink sink, double[][] corners, double base, double top, double bevel, double alpha, int side) {
        double[] cx = {0.0, 0.0};
        for (double[] c : corners) {
            cx[0] += c[0] / corners.length;
            cx[1] += c[1] / corners.length;
        }
        double out = Math.signum(top);
        double[] centre = {cx[0], cx[1], base - 3.0 * out};
        double[][] face = new double[corners.length][];
        double[][] foot = new double[corners.length][];
        for (int k = 0; k < corners.length; k++) {
            double dx = cx[0] - corners[k][0];
            double dy = cx[1] - corners[k][1];
            double l = Math.max(1.0E-6, Math.hypot(dx, dy));
            face[k] = new double[] {corners[k][0] + dx / l * bevel, corners[k][1] + dy / l * bevel, base + top};
            foot[k] = new double[] {corners[k][0], corners[k][1], base + 0.02 * out};
        }
        int lit = argb(mix(PLATE, PLATE_HI, 0.35), alpha);
        int edgeHi = argb(mix(PLATE_HI, RIM, 0.35), alpha);
        int dark = argb(PLATE_DARK, alpha);
        facet(sink, face[0], face[1], face[2], face[3], centre, edgeHi, lit, argb(PLATE, alpha), lit);
        for (int k = 0; k < corners.length; k++) {
            int j = (k + 1) % corners.length;
            boolean upper = k == 0;
            facet(sink, foot[k], foot[j], face[j], face[k], centre, dark, dark, upper ? edgeHi : lit, upper ? edgeHi : lit);
        }
    }

    /** The same raised plate on the back of the torso. */
    static void plateBack(UkakuModel.Sink sink, double[][] corners, double base, double top, double bevel, double alpha) {
        double[][] mirrored = new double[corners.length][];
        for (int k = 0; k < corners.length; k++) {
            mirrored[k] = corners[corners.length - 1 - k];
        }
        plate(sink, mirrored, base, top - base, bevel, alpha, 0);
    }

    /** A collar of armor round a tentacle's root on the back, glowing magenta inside. */
    static void socket(UkakuModel.Sink sink, double x, double y, double z, double radius, double grow, double alpha, boolean fire) {
        int rim = fire ? FIRE_SOCKET : SPIKE;
        int heart = fire ? FIRE_SOCKET_CORE : BELLY_GLOW;
        int sides = 8;
        double[] centre = {x, y, 0.0};
        for (int k = 0; k < sides; k++) {
            double a0 = 2.0 * Math.PI * k / sides;
            double a1 = 2.0 * Math.PI * (k + 1) / sides;
            double[] o0 = {x + Math.cos(a0) * radius, y + Math.sin(a0) * radius, 2.02};
            double[] o1 = {x + Math.cos(a1) * radius, y + Math.sin(a1) * radius, 2.02};
            double[] i0 = {x + Math.cos(a0) * radius * 0.62, y + Math.sin(a0) * radius * 0.62, z};
            double[] i1 = {x + Math.cos(a1) * radius * 0.62, y + Math.sin(a1) * radius * 0.62, z};
            facet(sink, o0, o1, i1, i0, centre, argb(PLATE_DARK, alpha), argb(PLATE_DARK, alpha), argb(PLATE_HI, alpha), argb(PLATE_HI, alpha));
            double[] core = {x, y, z - 0.1};
            int hot = argb(heart, alpha * grow);
            glow(sink, i0, i1, core, core, argb(rim, alpha * grow), argb(rim, alpha * grow), hot, hot);
        }
    }

    // ------------------------------------------------------------------ primitives

    /** A shaded facet: true normal, turned away from {@code centre}. */
    static void facet(UkakuModel.Sink sink, double[] a, double[] b, double[] c, double[] d, double[] centre, int ca, int cb, int cc, int cd) {
        double[] e1 = sub(c, a);
        double[] e2 = sub(d, b);
        double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
        double length = Math.sqrt(dot(n, n));
        if (length < 1.0E-12) {
            // Degenerate across one diagonal (a claw point): use the other triangle.
            double[] f1 = sub(b, a);
            double[] f2 = sub(d, a);
            n = new double[] {f1[1] * f2[2] - f1[2] * f2[1], f1[2] * f2[0] - f1[0] * f2[2], f1[0] * f2[1] - f1[1] * f2[0]};
            length = Math.sqrt(dot(n, n));
            if (length < 1.0E-12) {
                return;
            }
        }
        n = mul(n, 1.0 / length);
        double[] mid = {(a[0] + b[0] + c[0] + d[0]) / 4.0 - centre[0], (a[1] + b[1] + c[1] + d[1]) / 4.0 - centre[1],
            (a[2] + b[2] + c[2] + d[2]) / 4.0 - centre[2]};
        if (dot(mid, n) < 0.0) {
            n = mul(n, -1.0);
        }
        sink.quad(a, b, c, d, n, ca, cb, cc, cd);
    }

    /** A glowing quad (lit from straight above, so the entity lighting leaves it at full brightness). */
    static void glow(UkakuModel.Sink sink, double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
        sink.quad(a, b, c, d, new double[] {0.0, 1.0, 0.0}, ca, cb, cc, cd);
    }

    static int argb(int rgb, double alpha) {
        return UkakuModel.argb(rgb, alpha);
    }

    static int mix(int from, int to, double amount) {
        return UkakuModel.mixInt(from, to, amount);
    }

    /** Mixes two ARGB colours, alpha included. */
    static int mix2(int from, int to, double amount) {
        int a = (int)Math.round((from >>> 24) * (1.0 - amount) + (to >>> 24) * amount);
        return a << 24 | UkakuModel.mixInt(from & 0xFFFFFF, to & 0xFFFFFF, amount);
    }

    static double smooth(double value) {
        return UkakuModel.smooth(value);
    }

    static double clamp01(double value) {
        return UkakuModel.clamp01(value);
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
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
        return normalize(new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]});
    }

    static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / length);
    }
}
