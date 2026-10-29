package com.hex.kagune.control.ukaku;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure 2D geometry of Ukaku's Ukaku kagune, matched to the anime (the Ukaku path notes, "Look"):
 * <ul>
 *   <li><b>Gaseous wing:</b> a fan of jagged, dark crimson/purple flame tongues spreading up and out from the shoulder
 *       blade, a molten orange-red core near the root, red veins, and a large soft pink-white glow around it all.</li>
 *   <li><b>Crystallized wing:</b> the flames gather into one long, notched blade: a dark, starry purple-black interior,
 *       red glowing edges and base, and a tight pink rim.</li>
 *   <li><b>:re One-Winged Kagune:</b> one huge crescent blade of faceted red crystal with dark inclusions and thorny
 *       serrations along its outer curve.</li>
 * </ul>
 * Shapes are lists of small convex polygons (3 or 4 corners) in wing-local units: for a wing, x points away from the
 * body and y up, with the root at the origin and a nominal length of 1; for the One-Winged Kagune, u points forward
 * and v up. Polygons are listed back to front by {@link Poly#layer()}; renderers draw both faces with nested layers
 * offset so they never z-fight. Everything is deterministic for a given time (no per-frame randomness), so a
 * crystallized wing holds perfectly still.
 */
public final class UkakuWing {
    /** Depth layers, back to front. */
    public static final int HALO = 0;
    public static final int RIM = 1;
    public static final int BODY = 2;
    public static final int GLOW = 3;
    public static final int CORE = 4;
    public static final int DETAIL = 5;
    public static final int LAYERS = 6;

    /** Palette sampled from the anime frames. */
    public static final int AURA = 0xF6D2EE;
    public static final int FLAME_DARK = 0x2C0A21;
    public static final int FLAME_DARK_TIP = 0x150611;
    public static final int FLAME_RED = 0xE3161C;
    public static final int MOLTEN = 0xFF5A14;
    public static final int MOLTEN_HOT = 0xFFC24A;
    public static final int CRYSTAL_DARK = 0x1B1031;
    public static final int CRYSTAL_EDGE = 0xFF2A24;
    public static final int CRYSTAL_RIM = 0xF5C4E6;
    public static final int STAR = 0xA9C0FF;
    public static final int BLADE_RED = 0xD5121E;
    public static final int BLADE_LIGHT = 0xFF3B30;
    public static final int BLADE_DEEP = 0x8E0A16;
    public static final int BLADE_EDGE = 0xFF7A5C;
    public static final int BLADE_INCLUSION = 0x2C0A3A;

    /** Resting direction of the crystallized blade, degrees above "straight out from the body". */
    public static final double BLADE_ANGLE = 67.0;

    /** A small convex polygon (3 or 4 corners) with a colour, an opacity (0..1) and a depth layer. */
    public record Poly(float[] x, float[] y, int rgb, float alpha, int layer) {
        public int corners() {
            return this.x.length;
        }
    }

    private UkakuWing() {
    }

    // ------------------------------------------------------------------ shared helpers

    private static float clamp01(double value) {
        return (float)Math.max(0.0, Math.min(1.0, value));
    }

    private static double smooth(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }

    /** Opaque mix of two RGB colours. */
    public static int mix(int from, int to, double amount) {
        double a = Math.max(0.0, Math.min(1.0, amount));
        int r = (int)Math.round(((from >> 16) & 0xFF) * (1.0 - a) + ((to >> 16) & 0xFF) * a);
        int g = (int)Math.round(((from >> 8) & 0xFF) * (1.0 - a) + ((to >> 8) & 0xFF) * a);
        int b = (int)Math.round((from & 0xFF) * (1.0 - a) + (to & 0xFF) * a);
        return r << 16 | g << 8 | b;
    }

    /** Sharp sawtooth in [-1, 1] that rises toward the tip (flame licks and notches). */
    private static double saw(double value) {
        double frac = value - Math.floor(value);
        return frac * 2.0 - 1.0;
    }

    /** Deterministic hash in [0, 1). */
    private static double hash(int a, int b) {
        long h = a * 0x9E3779B97F4A7C15L ^ (b + 0x632BE59BD9B4E019L) * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) / (double)(1L << 53);
    }

    private static void quad(List<Poly> out, double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy,
                             int rgb, float alpha, int layer) {
        if (alpha <= 0.003F) {
            return;
        }
        out.add(new Poly(new float[] {(float)ax, (float)bx, (float)cx, (float)dx}, new float[] {(float)ay, (float)by, (float)cy, (float)dy},
            rgb, Math.min(1.0F, alpha), layer));
    }

    private static void triangle(List<Poly> out, double ax, double ay, double bx, double by, double cx, double cy, int rgb, float alpha,
                                 int layer) {
        if (alpha <= 0.003F) {
            return;
        }
        out.add(new Poly(new float[] {(float)ax, (float)bx, (float)cx}, new float[] {(float)ay, (float)by, (float)cy}, rgb,
            Math.min(1.0F, alpha), layer));
    }

    /** A ribbon around a sampled centre line: {@code left}/{@code right} are half-widths per sample; one quad per segment. */
    private static void ribbon(List<Poly> out, double[] cx, double[] cy, double[] nx, double[] ny, double[] left, double[] right,
                               int fromRgb, int toRgb, float fromAlpha, float toAlpha, int layer) {
        int n = cx.length;
        for (int s = 0; s + 1 < n; s++) {
            double t = (s + 0.5) / (n - 1);
            int rgb = mix(fromRgb, toRgb, t);
            float alpha = (float)(fromAlpha + (toAlpha - fromAlpha) * t);
            quad(out,
                cx[s] + nx[s] * left[s], cy[s] + ny[s] * left[s],
                cx[s + 1] + nx[s + 1] * left[s + 1], cy[s + 1] + ny[s + 1] * left[s + 1],
                cx[s + 1] - nx[s + 1] * right[s + 1], cy[s + 1] - ny[s + 1] * right[s + 1],
                cx[s] - nx[s] * right[s], cy[s] - ny[s] * right[s],
                rgb, alpha, layer);
        }
    }

    /** A filled ellipse as a fan of triangles. */
    private static void ellipse(List<Poly> out, double x, double y, double rx, double ry, int rgb, float alpha, int layer, int sides) {
        for (int k = 0; k < sides; k++) {
            double a0 = Math.PI * 2.0 * k / sides;
            double a1 = Math.PI * 2.0 * (k + 1) / sides;
            triangle(out, x, y, x + Math.cos(a0) * rx, y + Math.sin(a0) * ry, x + Math.cos(a1) * rx, y + Math.sin(a1) * ry, rgb, alpha, layer);
        }
    }

    // ------------------------------------------------------------------ twin-wing kagune (one wing)

    /** A sampled, curved centre line from the root toward {@code angleDeg} with length {@code length}. */
    private record Spine(double[] x, double[] y, double[] nx, double[] ny) {
        static Spine of(double angleDeg, double length, double bend, int samples, double start) {
            double a = Math.toRadians(angleDeg);
            double dx = Math.cos(a);
            double dy = Math.sin(a);
            double px = -dy;
            double py = dx;
            double p0x = dx * start * length;
            double p0y = dy * start * length;
            double p1x = dx * 0.5 * length + px * bend * length * 0.5;
            double p1y = dy * 0.5 * length + py * bend * length * 0.5;
            double p2x = dx * length + px * bend * length;
            double p2y = dy * length + py * bend * length;
            double[] x = new double[samples];
            double[] y = new double[samples];
            double[] nx = new double[samples];
            double[] ny = new double[samples];
            for (int s = 0; s < samples; s++) {
                double t = s / (samples - 1.0);
                double u = 1.0 - t;
                x[s] = u * u * p0x + 2 * u * t * p1x + t * t * p2x;
                y[s] = u * u * p0y + 2 * u * t * p1y + t * t * p2y;
                double tx = 2 * u * (p1x - p0x) + 2 * t * (p2x - p1x);
                double ty = 2 * u * (p1y - p0y) + 2 * t * (p2y - p1y);
                double len = Math.max(1.0E-9, Math.sqrt(tx * tx + ty * ty));
                nx[s] = -ty / len;
                ny[s] = tx / len;
            }
            return new Spine(x, y, nx, ny);
        }
    }

    /**
     * One Ukaku wing at unit length. {@code crystal} 0..1 cross-fades the gaseous flames into the crystallized blade (the
     * flames gather toward the blade while they fade); {@code deploy} 0..1 grows the wing out of the shoulder blade;
     * {@code tongues} is 7 for Tokyo Ghoul / √A Ukaku and 9 for the fuller :re wings; {@code detail} 0..1 is the level
     * of detail (segments, glow shells, specks).
     */
    public static List<Poly> wing(double time, float crystal, float deploy, int tongues, float detail) {
        List<Poly> out = new ArrayList<>();
        float c = clamp01(crystal);
        float grow = clamp01(deploy);
        if (grow <= 0.001F) {
            return out;
        }
        int samples = Math.max(5, Math.round(6 + 8 * clamp01(detail)));
        boolean halo = detail >= 0.35F;
        float gaseous = 1.0F - c;
        if (gaseous > 0.003F) {
            gaseousWing(out, time, gaseous, c, grow, Math.max(3, tongues), samples, halo, detail >= 0.6F);
        }
        if (c > 0.003F) {
            crystalBlade(out, c, grow, samples + 4, halo, detail >= 0.6F);
        }
        return out;
    }

    private static double bell(double f, double centre, double width) {
        double z = (f - centre) / width;
        return Math.exp(-z * z);
    }

    /** Main wing body: a broad flame mass sweeping out and up from the shoulder blade (angle, length, bend). */
    private static final double BODY_ANGLE = 40.0;
    private static final double BODY_LENGTH = 0.9;
    private static final double BODY_BEND = 0.08;

    /** Half-width of the gaseous wing body at {@code t}: broad through the middle, narrowing toward the tip. */
    static double bodyWidth(double t) {
        return 0.28 * (0.55 + 0.45 * Math.sin(Math.PI * Math.min(1.0, t * 1.2))) * (1.0 - 0.6 * t);
    }

    /**
     * Flame fingers off the wing body, as {where along the body (0..1), edge (+1 upper, -1 lower, 0 tip), angle in degrees,
     * length, half-width}. Upper fingers lick upward and outward, the tip ones reach out, the lower ones droop at the far end.
     */
    private static final double[][] FINGERS = {
        {0.15, +1, 88.0, 0.42, 0.10}, {0.33, +1, 78.0, 0.55, 0.11}, {0.52, +1, 66.0, 0.52, 0.11}, {0.72, +1, 54.0, 0.42, 0.10},
        {0.92, 0, 36.0, 0.38, 0.09}, {1.0, 0, 16.0, 0.33, 0.085},
        {0.50, -1, -14.0, 0.24, 0.08}, {0.68, -1, -26.0, 0.31, 0.085}, {0.86, -1, -6.0, 0.27, 0.08}};

    /** The :re wings carry two more fingers (fuller, larger flames). */
    private static final double[][] RE_FINGERS = {{0.42, +1, 84.0, 0.5, 0.1}, {0.78, -1, -18.0, 0.28, 0.08}};

    private static void gaseousWing(List<Poly> out, double time, float opacity, float crystal, float deploy, int tongues, int samples,
                                    boolean halo, boolean veins) {
        double grow = 1.0 - Math.pow(1.0 - deploy, 3.0);
        double flicker = 1.0 - crystal;
        double bodyAngle = BODY_ANGLE + (BLADE_ANGLE - BODY_ANGLE) * crystal + 3.0 * Math.sin(time * 1.9) * flicker;
        double bodyLength = BODY_LENGTH * grow;
        Spine body = Spine.of(bodyAngle, bodyLength, BODY_BEND, samples, 0.0);
        double[] width = new double[samples];
        double[] upper = new double[samples];
        double[] lower = new double[samples];
        for (int s = 0; s < samples; s++) {
            double t = s / (samples - 1.0);
            width[s] = bodyWidth(t) * grow;
            double phase = time * 1.6 * flicker;
            upper[s] = width[s] * (1.0 + 0.18 * saw(t * 5.0 + phase));
            lower[s] = width[s] * (1.0 + 0.18 * saw(t * 4.0 + phase + 0.5));
        }
        // The glow around everything: a broad soft halo and a brighter pink-white rim.
        if (halo) {
            ribbon(out, body.x, body.y, body.nx, body.ny, hugged(width, 2.1, 0.09 * grow), hugged(width, 2.1, 0.09 * grow),
                AURA, AURA, 0.16F * opacity, 0.07F * opacity, HALO);
        }
        ribbon(out, body.x, body.y, body.nx, body.ny, hugged(width, 1.5, 0.05 * grow), hugged(width, 1.5, 0.05 * grow),
            AURA, AURA, 0.34F * opacity, 0.22F * opacity, RIM);
        // Dark flame mass, then the molten red-orange heart of the wing.
        ribbon(out, body.x, body.y, body.nx, body.ny, upper, lower, FLAME_DARK, FLAME_DARK_TIP, 0.93F * opacity, 0.9F * opacity, BODY);
        int inner = Math.max(4, samples * 4 / 5);
        Spine heart = Spine.of(bodyAngle, bodyLength * 0.78, BODY_BEND, inner, 0.0);
        double[] heartUpper = new double[inner];
        double[] heartLower = new double[inner];
        double[] hotUpper = new double[inner];
        double[] hotLower = new double[inner];
        for (int s = 0; s < inner; s++) {
            double t = s / (inner - 1.0);
            double w = bodyWidth(t * 0.78) * grow * 0.66;
            double phase = time * 2.1 * flicker;
            heartUpper[s] = w * (1.0 + 0.22 * saw(t * 3.5 + phase + 0.2));
            heartLower[s] = w * (1.0 + 0.22 * saw(t * 3.0 + phase + 0.7));
            double hot = Math.max(0.0, 1.0 - t * 1.6);
            hotUpper[s] = w * 0.55 * hot * (1.0 + 0.2 * saw(t * 4.0 + phase));
            hotLower[s] = w * 0.55 * hot * (1.0 + 0.2 * saw(t * 4.0 + phase + 0.5));
        }
        // Ragged red heart with a hot orange-yellow inside, burning out toward the dark flames.
        ribbon(out, heart.x, heart.y, heart.nx, heart.ny, heartUpper, heartLower, MOLTEN, FLAME_RED, 0.95F * opacity, 0.88F * opacity, GLOW);
        ribbon(out, heart.x, heart.y, heart.nx, heart.ny, hotUpper, hotLower, MOLTEN_HOT, MOLTEN, 0.9F * opacity, 0.6F * opacity, CORE);

        int fingerCount = tongues > 7 ? FINGERS.length + RE_FINGERS.length : FINGERS.length;
        int fingerSamples = Math.max(4, samples * 2 / 3);
        for (int i = 0; i < fingerCount; i++) {
            double[] finger = i < FINGERS.length ? FINGERS[i] : RE_FINGERS[i - FINGERS.length];
            double local = clamp01((deploy - 0.35 * finger[0]) / 0.65);
            if (local <= 0.001) {
                continue;
            }
            double fingerGrow = 1.0 - Math.pow(1.0 - local, 3.0);
            int at = (int)Math.round(finger[0] * (samples - 1));
            double edge = finger[1];
            double half = edge > 0 ? upper[at] : edge < 0 ? lower[at] : 0.0;
            double startX = body.x[at] + body.nx[at] * half * edge * 0.8;
            double startY = body.y[at] + body.ny[at] * half * edge * 0.8;
            double sway = (6.0 * Math.sin(time * 2.4 + i * 1.7) + 2.5 * Math.sin(time * 5.3 + i * 0.7)) * flicker;
            double angle = finger[2] + (bodyAngle - BODY_ANGLE) + sway;
            angle += (BLADE_ANGLE + (finger[0] - 0.5) * 8.0 - angle) * crystal;
            double length = finger[3] * fingerGrow * (1.0 + 0.08 * Math.sin(time * 3.7 + i * 2.2) * flicker);
            double bend = edge >= 0 ? -0.15 : 0.2;
            Spine spine = Spine.of(angle, length, bend, fingerSamples, 0.0);
            double[] fingerLeft = new double[fingerSamples];
            double[] fingerRight = new double[fingerSamples];
            double[] fingerBase = new double[fingerSamples];
            for (int s = 0; s < fingerSamples; s++) {
                double t = s / (fingerSamples - 1.0);
                double w = finger[4] * fingerGrow * Math.pow(1.0 - t, 0.85) * (0.8 + 0.4 * Math.sin(Math.PI * Math.min(1.0, t * 1.6)));
                fingerBase[s] = w;
                double phase = time * 1.8 * flicker + i * 0.9;
                fingerLeft[s] = w * (1.0 + 0.32 * saw(t * 4.0 + phase));
                fingerRight[s] = w * (1.0 + 0.32 * saw(t * 4.0 + phase + 0.5));
            }
            for (int s = 0; s < fingerSamples; s++) {
                spine.x[s] += startX;
                spine.y[s] += startY;
            }
            ribbon(out, spine.x, spine.y, spine.nx, spine.ny, scaled(fingerBase, 1.9, 0.05 * length), scaled(fingerBase, 1.9, 0.05 * length),
                AURA, AURA, 0.3F * opacity, 0.12F * opacity, RIM);
            ribbon(out, spine.x, spine.y, spine.nx, spine.ny, fingerLeft, fingerRight, FLAME_DARK, FLAME_DARK_TIP,
                0.93F * opacity, 0.88F * opacity, BODY);
            if (veins) {
                double[] vein = new double[fingerSamples];
                for (int s = 0; s < fingerSamples; s++) {
                    double t = s / (fingerSamples - 1.0);
                    vein[s] = t < 0.15 || t > 0.8 ? 0.0 : 0.014 * Math.max(0.3, length);
                }
                ribbon(out, spine.x, spine.y, spine.nx, spine.ny, vein, vein, FLAME_RED, FLAME_RED, 0.85F * opacity, 0.55F * opacity, DETAIL);
            }
        }
        // Molten core at the shoulder blade.
        ellipse(out, 0.09 * grow, 0.03 * grow, 0.16 * grow, 0.13 * grow, MOLTEN, 0.92F * opacity, CORE, 10);
        ellipse(out, 0.09 * grow, 0.03 * grow, 0.085 * grow, 0.07 * grow, MOLTEN_HOT, opacity, CORE, 8);
    }

    /** Like {@link #scaled} but the extra margin is even along the length (no bulge at the root). */
    private static double[] hugged(double[] values, double factor, double extra) {
        double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            double t = i / (double)Math.max(1, values.length - 1);
            out[i] = values[i] * factor + extra * Math.sin(Math.PI * Math.min(1.0, 0.15 + t));
        }
        return out;
    }

    private static double[] scaled(double[] values, double factor, double extra) {
        double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i] * factor + extra * (1.0 - i / (double)Math.max(1, values.length - 1));
        }
        return out;
    }

    /** Half-width of the crystallized blade at {@code t} (unit length). */
    static double bladeWidth(double t) {
        return t < 0.3 ? 0.07 + 0.21 * (t / 0.3) : 0.28 * Math.pow(Math.max(0.0, 1.0 - (t - 0.3) / 0.7), 1.05);
    }

    /** Length and bend of the crystallized blade's spine (unit wing length). */
    private static final double BLADE_LENGTH = 1.05;
    private static final double BLADE_BEND = -0.05;

    /**
     * Wing-local point on a fully deployed crystallized blade's spine at {@code t} (0 at the shoulder blade, 1 at the tip):
     * where a Shard Volley's shards leave the wing. Same curve as the blade the renderer draws.
     */
    public static double[] bladePoint(double t) {
        double clamped = Double.isFinite(t) ? Math.max(0.0, Math.min(1.0, t)) : 0.0;
        double a = Math.toRadians(BLADE_ANGLE);
        double dx = Math.cos(a);
        double dy = Math.sin(a);
        double p1x = dx * 0.5 * BLADE_LENGTH - dy * BLADE_BEND * BLADE_LENGTH * 0.5;
        double p1y = dy * 0.5 * BLADE_LENGTH + dx * BLADE_BEND * BLADE_LENGTH * 0.5;
        double p2x = dx * BLADE_LENGTH - dy * BLADE_BEND * BLADE_LENGTH;
        double p2y = dy * BLADE_LENGTH + dx * BLADE_BEND * BLADE_LENGTH;
        double u = 1.0 - clamped;
        return new double[] {2 * u * clamped * p1x + clamped * clamped * p2x, 2 * u * clamped * p1y + clamped * clamped * p2y};
    }

    private static void crystalBlade(List<Poly> out, float opacity, float deploy, int samples, boolean halo, boolean stars) {
        double grow = 1.0 - Math.pow(1.0 - deploy, 3.0);
        double length = BLADE_LENGTH * grow;
        Spine spine = Spine.of(BLADE_ANGLE, length, BLADE_BEND, samples, 0.0);
        double[] base = new double[samples];
        double[] outer = new double[samples];
        double[] inner = new double[samples];
        for (int s = 0; s < samples; s++) {
            double t = s / (samples - 1.0);
            base[s] = bladeWidth(t) * grow;
            // Notches: deep on the outer edge, shallow on the inner one.
            outer[s] = base[s] * (1.0 + 0.4 * saw(t * 6.0));
            inner[s] = base[s] * 0.7 * (1.0 + 0.12 * saw(t * 3.0 + 0.4));
        }
        if (halo) {
            ribbon(out, spine.x, spine.y, spine.nx, spine.ny, scaled(base, 1.6, 0.08 * length), scaled(base, 1.6, 0.08 * length),
                CRYSTAL_RIM, CRYSTAL_RIM, 0.17F * opacity, 0.08F * opacity, HALO);
        }
        ribbon(out, spine.x, spine.y, spine.nx, spine.ny, scaled(outer, 1.22, 0.035 * length), scaled(inner, 1.22, 0.035 * length),
            CRYSTAL_RIM, CRYSTAL_RIM, 0.58F * opacity, 0.5F * opacity, RIM);
        ribbon(out, spine.x, spine.y, spine.nx, spine.ny, outer, inner, CRYSTAL_DARK, CRYSTAL_DARK, 0.97F * opacity, 0.97F * opacity, BODY);
        // Red glowing bands just inside both edges, brightest at the base.
        double[] outerIn = new double[samples];
        double[] innerIn = new double[samples];
        for (int s = 0; s < samples; s++) {
            outerIn[s] = outer[s] * 0.76;
            innerIn[s] = inner[s] * 0.76;
        }
        edgeBand(out, spine, outer, outerIn, +1, opacity);
        edgeBand(out, spine, inner, innerIn, -1, opacity);
        // Base glow: the lower third burns red-orange into the dark crystal.
        int baseSamples = Math.max(3, samples * 2 / 5);
        double[] bx = new double[baseSamples];
        double[] by = new double[baseSamples];
        double[] bnx = new double[baseSamples];
        double[] bny = new double[baseSamples];
        double[] bw = new double[baseSamples];
        for (int s = 0; s < baseSamples; s++) {
            bx[s] = spine.x[s];
            by[s] = spine.y[s];
            bnx[s] = spine.nx[s];
            bny[s] = spine.ny[s];
            bw[s] = base[s] * 0.9;
        }
        ribbon(out, bx, by, bnx, bny, bw, bw, MOLTEN, CRYSTAL_EDGE, 0.9F * opacity, 0.05F * opacity, GLOW);
        if (stars) {
            for (int k = 0; k < 16; k++) {
                double t = 0.25 + 0.65 * hash(k, 7);
                double side = (hash(k, 13) * 2.0 - 1.0) * 0.55;
                int s = (int)Math.round(t * (samples - 1));
                double half = (side > 0 ? outer[s] : inner[s]) * Math.abs(side);
                double x = spine.x[s] + spine.nx[s] * half * Math.signum(side);
                double y = spine.y[s] + spine.ny[s] * half * Math.signum(side);
                double size = (0.008 + 0.01 * hash(k, 21)) * grow;
                int rgb = hash(k, 31) < 0.3 ? 0xFFFFFF : STAR;
                quad(out, x - size, y, x, y + size, x + size, y, x, y - size, rgb, 0.92F * opacity, DETAIL);
            }
        }
    }

    private static void edgeBand(List<Poly> out, Spine spine, double[] edge, double[] inside, int side, float opacity) {
        int n = spine.x.length;
        for (int s = 0; s + 1 < n; s++) {
            double t = (s + 0.5) / (n - 1);
            int rgb = mix(MOLTEN, CRYSTAL_EDGE, Math.min(1.0, t * 2.5));
            float alpha = (float)(0.95 - 0.25 * t) * opacity;
            quad(out,
                spine.x[s] + spine.nx[s] * edge[s] * side, spine.y[s] + spine.ny[s] * edge[s] * side,
                spine.x[s + 1] + spine.nx[s + 1] * edge[s + 1] * side, spine.y[s + 1] + spine.ny[s + 1] * edge[s + 1] * side,
                spine.x[s + 1] + spine.nx[s + 1] * inside[s + 1] * side, spine.y[s + 1] + spine.ny[s + 1] * inside[s + 1] * side,
                spine.x[s] + spine.nx[s] * inside[s] * side, spine.y[s] + spine.ny[s] * inside[s] * side,
                rgb, alpha, GLOW);
        }
    }

    // ------------------------------------------------------------------ :re One-Winged Kagune

    /** Crescent centre line control points (u forward, v up), unit scale. */
    private static final double[][] CRESCENT = {{0.0, 0.0}, {-0.25, 0.70}, {0.55, 1.15}, {1.30, 0.30}};

    private static double[] crescentPoint(double t) {
        double u = 1.0 - t;
        double b0 = u * u * u;
        double b1 = 3 * u * u * t;
        double b2 = 3 * u * t * t;
        double b3 = t * t * t;
        return new double[] {
            b0 * CRESCENT[0][0] + b1 * CRESCENT[1][0] + b2 * CRESCENT[2][0] + b3 * CRESCENT[3][0],
            b0 * CRESCENT[0][1] + b1 * CRESCENT[1][1] + b2 * CRESCENT[2][1] + b3 * CRESCENT[3][1]};
    }

    /** Half-width of the crescent at {@code t}: thin at the root, broad through the arc, a sharp point at the tip. */
    static double crescentWidth(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return (0.05 + 0.13 * Math.pow(Math.sin(Math.PI * Math.min(c, 0.999)), 0.85)) * Math.pow(1.0 - c, 0.35);
    }

    /**
     * The :re One-Winged Kagune in (u forward, v up) units: a crystallized crescent that rises from the shoulder blade,
     * arcs over the head and sweeps forward to a point. {@code deploy} grows it from the root to the tip.
     */
    public static List<Poly> oneWing(double time, float deploy, float detail) {
        List<Poly> out = new ArrayList<>();
        float grow = clamp01(deploy);
        if (grow <= 0.001F) {
            return out;
        }
        int samples = Math.max(10, Math.round(12 + 14 * clamp01(detail)));
        double reach = 1.0 - Math.pow(1.0 - grow, 2.5);
        double[] x = new double[samples];
        double[] y = new double[samples];
        double[] nx = new double[samples];
        double[] ny = new double[samples];
        double[] w = new double[samples];
        for (int s = 0; s < samples; s++) {
            double t = reach * s / (samples - 1.0);
            double[] p = crescentPoint(t);
            double[] q = crescentPoint(Math.min(1.0, t + 0.001));
            double[] r = crescentPoint(Math.max(0.0, t - 0.001));
            double tx = q[0] - r[0];
            double ty = q[1] - r[1];
            double len = Math.max(1.0E-9, Math.sqrt(tx * tx + ty * ty));
            x[s] = p[0];
            y[s] = p[1];
            nx[s] = -ty / len; // the convex (outer) side of the crescent
            ny[s] = tx / len;
            w[s] = crescentWidth(t) * (s == samples - 1 && reach < 0.999 ? 0.35 : 1.0);
        }
        boolean halo = detail >= 0.35F;
        if (halo) {
            ribbon(out, x, y, nx, ny, scaled(w, 1.5, 0.05), scaled(w, 1.5, 0.05), CRYSTAL_RIM, CRYSTAL_RIM, 0.16F, 0.08F, HALO);
        }
        ribbon(out, x, y, nx, ny, scaled(w, 1.18, 0.02), scaled(w, 1.18, 0.02), CRYSTAL_RIM, CRYSTAL_RIM, 0.32F, 0.22F, RIM);
        // Faceted crystal: every segment splits into two triangles of different shades.
        int[] shades = {BLADE_RED, BLADE_DEEP, BLADE_RED, BLADE_LIGHT, BLADE_DEEP, BLADE_RED, BLADE_INCLUSION, BLADE_DEEP};
        for (int s = 0; s + 1 < samples; s++) {
            double ox0 = x[s] + nx[s] * w[s];
            double oy0 = y[s] + ny[s] * w[s];
            double ox1 = x[s + 1] + nx[s + 1] * w[s + 1];
            double oy1 = y[s + 1] + ny[s + 1] * w[s + 1];
            double ix0 = x[s] - nx[s] * w[s] * 0.8;
            double iy0 = y[s] - ny[s] * w[s] * 0.8;
            double ix1 = x[s + 1] - nx[s + 1] * w[s + 1] * 0.8;
            double iy1 = y[s + 1] - ny[s + 1] * w[s + 1] * 0.8;
            int a = shades[(int)(hash(s, 3) * shades.length)];
            int b = shades[(int)(hash(s, 5) * shades.length)];
            triangle(out, ox0, oy0, ox1, oy1, ix1, iy1, a, 0.97F, BODY);
            triangle(out, ox0, oy0, ix1, iy1, ix0, iy0, b, 0.97F, BODY);
            // Bright outer edge.
            double ex0 = x[s] + nx[s] * w[s] * 0.86;
            double ey0 = y[s] + ny[s] * w[s] * 0.86;
            double ex1 = x[s + 1] + nx[s + 1] * w[s + 1] * 0.86;
            double ey1 = y[s + 1] + ny[s + 1] * w[s + 1] * 0.86;
            quad(out, ox0, oy0, ox1, oy1, ex1, ey1, ex0, ey0, BLADE_EDGE, 0.9F, GLOW);
        }
        // Thorns along the outer curve, leaning back toward the root.
        for (int k = 0; k < 13; k++) {
            double t = 0.1 + k * 0.065;
            if (t > reach - 0.04) {
                break;
            }
            int s = (int)Math.round(t / reach * (samples - 1));
            int lo = Math.max(0, s - 1);
            int hi = Math.min(samples - 1, s + 1);
            double tx = x[hi] - x[lo];
            double ty = y[hi] - y[lo];
            double tl = Math.max(1.0E-9, Math.sqrt(tx * tx + ty * ty));
            double spike = 0.06 * (0.8 + 0.4 * hash(k, 11));
            double apexX = x[s] + nx[s] * (w[s] + spike) - tx / tl * 0.04;
            double apexY = y[s] + ny[s] * (w[s] + spike) - ty / tl * 0.04;
            triangle(out, x[lo] + nx[lo] * w[lo] * 0.95, y[lo] + ny[lo] * w[lo] * 0.95, x[hi] + nx[hi] * w[hi] * 0.95,
                y[hi] + ny[hi] * w[hi] * 0.95, apexX, apexY, BLADE_DEEP, 0.95F, BODY);
        }
        // Dark inclusions inside the crystal.
        if (detail >= 0.35F) {
            for (int k = 0; k < 12; k++) {
                double t = 0.15 + 0.7 * hash(k, 17);
                if (t > reach - 0.05) {
                    continue;
                }
                int s = (int)Math.round(t / reach * (samples - 1));
                double side = (hash(k, 19) * 2.0 - 1.0) * 0.5;
                double cx = x[s] + nx[s] * w[s] * side;
                double cy = y[s] + ny[s] * w[s] * side;
                double size = w[s] * (0.18 + 0.2 * hash(k, 23));
                triangle(out, cx - size, cy - size * 0.4, cx + size * 0.7, cy - size * 0.2, cx + size * 0.1, cy + size * 0.8,
                    BLADE_INCLUSION, 0.85F, DETAIL);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers for fitting and tests

    /** {minX, minY, maxX, maxY} over every corner, or all zeros for an empty list. */
    public static float[] bounds(List<Poly> polys) {
        if (polys.isEmpty()) {
            return new float[4];
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (Poly poly : polys) {
            for (int i = 0; i < poly.corners(); i++) {
                minX = Math.min(minX, poly.x()[i]);
                minY = Math.min(minY, poly.y()[i]);
                maxX = Math.max(maxX, poly.x()[i]);
                maxY = Math.max(maxY, poly.y()[i]);
            }
        }
        return new float[] {minX, minY, maxX, maxY};
    }

    /**
     * The largest extent any wing pose can reach, in wing units, for fitting a wing into a fixed box: every flicker phase,
     * every crystallization step and every level of detail at full deployment, with a 0.06 margin. Measured offline by
     * dense sampling (the same sweep the tests repeat), so fitting never has to compute it at runtime. {minX, minY, maxX, maxY}.
     */
    private static final float[] ENVELOPE = {-0.51F, -0.35F, 1.13F, 1.27F};

    public static float[] envelope(int tongues) {
        return ENVELOPE.clone();
    }
}
