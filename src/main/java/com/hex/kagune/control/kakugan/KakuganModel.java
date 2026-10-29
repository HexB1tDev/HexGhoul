package com.hex.kagune.control.kakugan;

import java.util.ArrayList;
import java.util.List;

/**
 * The kakugan as geometry on the head (pure, tested): the whites of the eyes turn black, the irises turn red with a
 * dark rim, thin red cracks radiate from the iris across the black, and dark veins crawl out of the eyes over the
 * skin, tapering and forking like the anime's (mostly down the cheek). Only the eyes and the skin right around them
 * change; the rest of every player's own skin is untouched. Everything is built in the head model's own pixels, the
 * space the head's skin is drawn in: x -4..4 left to right as the face is seen from the front (texture column
 * {@code u} sits at {@code x = u - 4}), y -8 at the top of the head .. 0 at the chin (row {@code v} at
 * {@code y = v - 8}), and the face at z -4 (smaller is further out). The renderer places it with the head's own
 * transform, so it follows every head turn and body animation.
 *
 * <p>Two kinds of quads: solid ones (lit by the world, faded in by alpha) and glowing ones ({@link Quad#glow()}: drawn
 * additively at full brightness, so they shine in the dark; their colour is their strength). As the kakugan wakes
 * ({@code intensity} 0..1) the eyes darken and the veins grow outward; the veins pulse with a heartbeat. {@code seed}
 * makes every player's veins their own.
 */
public final class KakuganModel {
    /**
     * One quad: four corners (x, y, z in head pixels; triangles repeat a corner), a normal, four ARGB colours. {@code outer}:
     * it lies on the skin's outer (second) layer, the head box half a pixel out (every coordinate 9/8 of the inner one's
     * about the head's centre), where that layer covers an eye pixel.
     */
    public record Quad(float[] xyz, float nx, float ny, float nz, int[] argb, boolean glow, boolean outer) {
    }

    /** The outer layer's box is the head's, half a pixel out each way: 9/8 of it about the head's centre (0, -4, 0). */
    public static final double OUTER_SCALE = 9.0 / 8.0;

    /** The whites turn pure black (canon): nothing glowing is ever drawn over them except the thin cracks. */
    static final int SCLERA = 0x000000;
    static final int IRIS = 0xC8101C;
    static final int IRIS_RIM = 0x4C0008;
    /** Cracks across the black: bright red, like the anime's. */
    static final int CRACK = 0xFF2A24;
    /** Veins in the skin: a soft dark bruise around a dark crimson core with a brighter line near the root. */
    static final int VEIN_SHADOW = 0x3A0208;
    static final int VEIN = 0x8A0A18;
    static final int VEIN_LIGHT = 0xD0161E;
    /** The bruised, sunken look around a kakugan: a soft dark ring on the skin around each eye. */
    static final int SOCKET = 0x2C0612;
    /** Overall vein thickness (the root widths below are multiplied by it). */
    static final double VEIN_SCALE = 1.15;
    /** 6.0.7: the extra cracks across a one-eyed ghoul's black (its veins grow four more roots, see {@link #veins}). */
    static final int ONE_EYED_CRACKS = 3;
    /** Glow colours (added to whatever is behind, so their brightness is their strength). 5.0.11: all brighter. */
    static final int GLOW = 0xFF1006;
    static final int CORE = 0xFF2410;
    static final int VEIN_GLOW = 0xD80E0A;
    static final int CRACK_GLOW = 0xF0140E;
    /** 5.0.12: the cracks across the black glow as hard as the veins: a hot core inside a soft glow this many times their width. */
    static final int CRACK_CORE = 0xFF3424;
    static final double CRACK_BLOOM = 3.0;
    /** How much of the iris the core glow covers (the inset from its edges, pixels), and the halo's strength and reach. */
    static final double CORE_INSET = 0.1;
    static final double HALO_STRENGTH = 0.85;
    static final double HALO_BEAT = 0.2;
    static final double HALO_REACH_U = 1.8;
    static final double HALO_REACH_V = 1.1;
    static final double HALO_REACH_PER_ROW = 0.4;

    /**
     * Depth order on the skin, in steps of {@code layer} pixels (see {@link #build}): the kakugan lies on the inner
     * skin layer itself, replacing its pixels, not floating over it like the outer layer. A step is only as thick as
     * the depth buffer needs to keep the parts from flickering (the renderer sizes it by camera distance), so even edge-on
     * nothing of the old eyes shows under it.
     */
    static final int SOCKET_LIFT = 1;
    static final int BASE_LIFT = 2;
    static final int SHADOW_LIFT = 2;
    static final int VEIN_LIFT = 3;
    static final int CRACK_LIFT = 3;
    static final int RIM_LIFT = 3;
    static final int LIGHT_LIFT = 4;
    static final int GLOW_LIFT = 5;
    /** Thickness of one depth step when the caller has no camera distance (pixels). */
    public static final double DEFAULT_LAYER = 0.003;

    /** One depth step (pixels) for a camera this far away (squared blocks): thin up close, a little thicker far away. */
    public static double layerFor(double distanceSqr) {
        return Math.max(DEFAULT_LAYER, Math.min(0.06, 4.0E-5 * distanceSqr));
    }

    /** The depth step for the head being built (pure, one thread per caller). */
    private static final ThreadLocal<Double> LAYER = ThreadLocal.withInitial(() -> DEFAULT_LAYER);

    private KakuganModel() {
    }

    /** All quads for one head. {@code fine} false (far away) skips the finest detail; {@code layer} is one depth step in pixels. */
    public static List<Quad> build(KakuganEyes eyes, int seed, double time, float intensity, boolean fine, double layer) {
        return build(eyes, seed, time, intensity, fine, layer, 0L);
    }

    /**
     * {@link #build(KakuganEyes, int, double, float, boolean, double)} for a skin whose outer layer covers the face cells in
     * {@code covered} with opaque pixels (eyes painted on the second layer; glass is left out, the renderer draws it over the
     * kakugan instead): every eye pixel under them is drawn on the outer layer too, which itself is left whole.
     */
    public static List<Quad> build(KakuganEyes eyes, int seed, double time, float intensity, boolean fine, double layer, long covered) {
        List<Quad> out = new ArrayList<>();
        LAYER.set(layer);
        double strength = clamp01(intensity);
        if (strength <= 0.003) {
            return out;
        }
        KakuganEyes e = eyes.validated();
        double grow = smooth((strength - 0.1) / 0.75);
        double beat = heartbeat(time);
        for (boolean right : new boolean[] {true, false}) {
            if (!e.ghoulEye(right)) {
                // One eyed (3.1.0): this eye stays human.
                continue;
            }
            eye(out, e, right, seed, strength, grow, beat, time, fine);
            if (covered != 0L) {
                outerEye(out, e, right, strength, time, seed, covered);
            }
        }
        return out;
    }

    /**
     * The eye's pixels the outer layer covers, drawn again on it: the black white and the red iris (with its rim and glowing
     * core) pixel for pixel, aligned to the outer layer's own pixels. Nothing else (the veins stay on the skin under it).
     */
    private static void outerEye(List<Quad> out, KakuganEyes e, boolean right, double strength, double time, int seed, long covered) {
        int h = e.height();
        int top = e.row(right);
        int sclera = e.scleraColumn(right);
        int iris = e.irisColumn(right);
        double alpha = smooth(strength * 3.0);
        double flicker = 0.92 + 0.08 * Math.sin(time * 7.3 + seed * 31 + (right ? 17 : 71));
        double rim = 0.13;
        for (int row = top; row < top + h; row++) {
            if ((covered & KakuganSkin.bit(sclera, row)) != 0L) {
                outerRect(out, sclera, row, sclera + 1, row + 1, BASE_LIFT, colour(SCLERA, alpha, false), false);
            }
            if ((covered & KakuganSkin.bit(iris, row)) != 0L) {
                outerRect(out, iris, row, iris + 1, row + 1, BASE_LIFT, colour(IRIS, alpha, false), false);
                int rimColour = colour(IRIS_RIM, alpha, false);
                if (row == top) {
                    outerRect(out, iris, row, iris + 1, row + rim, RIM_LIFT, rimColour, false);
                }
                if (row == top + h - 1) {
                    outerRect(out, iris, row + 1 - rim, iris + 1, row + 1, RIM_LIFT, rimColour, false);
                }
                outerRect(out, iris, row, iris + rim, row + 1, RIM_LIFT, rimColour, false);
                outerRect(out, iris + 1 - rim, row, iris + 1, row + 1, RIM_LIFT, rimColour, false);
                double inset = CORE_INSET;
                outerRect(out, iris + inset, row + (row == top ? inset : 0.0), iris + 1 - inset, row + 1 - (row == top + h - 1 ? inset : 0.0), GLOW_LIFT,
                    colour(CORE, strength * flicker, true), true);
            }
        }
    }

    /** A rectangle of face cells ({@code u}, {@code v}) on the outer layer, {@code level} depth steps out from it. */
    private static void outerRect(List<Quad> out, double u0, double v0, double u1, double v1, int level, int colour, boolean glow) {
        float[] xyz = new float[12];
        double[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        double lift = level * LAYER.get();
        for (int i = 0; i < 4; i++) {
            double[] p = place(uv[i][0], uv[i][1], 0.0);
            xyz[i * 3] = (float)(p[0] * OUTER_SCALE);
            xyz[i * 3 + 1] = (float)(-4.0 + (p[1] + 4.0) * OUTER_SCALE);
            xyz[i * 3 + 2] = (float)(-4.0 * OUTER_SCALE - lift);
        }
        out.add(new Quad(xyz, 0.0F, 0.0F, -1.0F, new int[] {colour, colour, colour, colour}, glow, true));
    }

    private static void eye(List<Quad> out, KakuganEyes e, boolean right, int seed, double strength, double grow, double beat, double time,
                            boolean fine) {
        int h = e.height();
        int top = e.row(right);
        int sclera = e.scleraColumn(right);
        int iris = e.irisColumn(right);
        // The eyes themselves turn fast (the old eye colours never linger); the veins take their time growing.
        double alpha = smooth(strength * 3.0);
        int eyeSeed = seed * 31 + (right ? 17 : 71);
        // A soft dark ring sinks the eye into the face, under everything else.
        socket(out, e.column(right) + 1.0, top + h / 2.0, h, fine ? 16 : 8, alpha * grow);
        // The white turns black; the iris turns red, with a dark rim. A hair larger than the pixels they cover, so the
        // skin's own eye never peeks out at the edges from an angle.
        double bleed = 0.06;
        double sLeft = sclera < iris ? bleed : 0.0;
        double sRight = sclera < iris ? 0.0 : bleed;
        rect(out, sclera - sLeft, top - bleed, sclera + 1 + sRight, top + h + bleed, BASE_LIFT, SCLERA, alpha, false);
        rect(out, iris - sRight, top - bleed, iris + 1 + sLeft, top + h + bleed, BASE_LIFT, IRIS, alpha, false);
        double rim = 0.13;
        rect(out, iris, top, iris + 1, top + rim, RIM_LIFT, IRIS_RIM, alpha, false);
        rect(out, iris, top + h - rim, iris + 1, top + h, RIM_LIFT, IRIS_RIM, alpha, false);
        rect(out, iris, top + rim, iris + rim, top + h - rim, RIM_LIFT, IRIS_RIM, alpha, false);
        rect(out, iris + 1 - rim, top + rim, iris + 1, top + h - rim, RIM_LIFT, IRIS_RIM, alpha, false);
        // Cracks radiating from the iris across the black, then the veins crawling out into the skin.
        for (Vein crack : cracks(e, right, eyeSeed)) {
            ribbon(out, crack, crack.width(), grow, CRACK_LIFT, CRACK, alpha, false, 0.0);
            // A soft glow round each crack and a hot core along it, at any distance (they are only a few quads).
            ribbon(out, crack, crack.width() * CRACK_BLOOM, grow, GLOW_LIFT, CRACK_GLOW, strength * (0.6 + 0.2 * beat), true, 0.0);
            ribbon(out, crack, crack.width() * 0.9, grow, GLOW_LIFT, CRACK_CORE, strength * (0.85 + 0.15 * beat), true, 0.0);
        }
        for (Vein vein : veins(e, right, eyeSeed)) {
            double depthFade = vein.depth() == 0 ? 1.0 : vein.depth() == 1 ? 0.88 : 0.72;
            ribbon(out, vein, vein.width() * 2.2, grow, SHADOW_LIFT, VEIN_SHADOW, alpha * 0.35 * depthFade, false, 0.0);
            ribbon(out, vein, vein.width(), grow, VEIN_LIFT, VEIN, alpha * 0.95 * depthFade, false, 0.0);
            if (vein.depth() <= 1) {
                ribbon(out, vein, vein.width() * 0.42, grow, LIGHT_LIFT, VEIN_LIGHT, alpha * 0.7 * depthFade, false, 1.0);
            }
            if (fine) {
                ribbon(out, vein, vein.width() * 1.3, grow, GLOW_LIFT, VEIN_GLOW, strength * depthFade * (0.55 + 0.45 * beat), true, 0.0);
            }
        }
        // The iris glows: a hot core and a soft red halo spilling over the skin around it.
        double flicker = 0.92 + 0.08 * Math.sin(time * 7.3 + eyeSeed);
        rect(out, iris + CORE_INSET, top + CORE_INSET, iris + 1 - CORE_INSET, top + h - CORE_INSET, GLOW_LIFT, CORE, strength * flicker, true);
        double[] eyeBox = {e.column(right), top, e.column(right) + 2.0, top + h};
        halo(out, iris + 0.5, top + h / 2.0, HALO_REACH_U, HALO_REACH_V + HALO_REACH_PER_ROW * h, eyeBox, fine ? 0.25 : 0.5,
            strength * (HALO_STRENGTH + HALO_BEAT * beat));
    }

    // ------------------------------------------------------------------ veins

    /**
     * One vein or crack: a smooth polyline in face pixels with a width at every point (tapering to its tip), and its
     * depth in the tree (0 a main vein, 1 a fork, 2 a fork of a fork).
     */
    record Vein(List<double[]> points, double[] widths, int depth) {
        double width() {
            return this.widths[0];
        }
    }

    /** The eye's own box, and the box a vein must stay in (the face, minus the eyes). */
    private record Bounds(double[][] eyes, double minU, double maxU, double minV, double maxV) {
        boolean allows(double u, double v, int own) {
            if (u < this.minU || u > this.maxU || v < this.minV || v > this.maxV) {
                return false;
            }
            for (int i = 0; i < this.eyes.length; i++) {
                double[] box = this.eyes[i];
                double margin = i == own ? 0.0 : 0.45;
                if (u > box[0] - margin && u < box[2] + margin && v > box[1] - margin && v < box[3] + margin) {
                    return false;
                }
            }
            return true;
        }
    }

    private static Bounds faceBounds(KakuganEyes e) {
        int h = e.height();
        double[][] eyes = {{e.rightX(), e.rightY(), e.rightX() + 2, e.rightY() + h}, {e.leftX(), e.leftY(), e.leftX() + 2, e.leftY() + h}};
        return new Bounds(eyes, 0.08, 7.92, 0.08, 7.92);
    }

    /**
     * Thin red cracks across the blackened white, radiating from the iris (as in the anime, where they spread from the
     * pupil over the black) and stopping at the edge of the white. 6.0.8: they run jagged, like lightning, each segment
     * kinked off its course, and the longer ones split once.
     */
    static List<Vein> cracks(KakuganEyes e, boolean right, int seed) {
        List<Vein> cracks = new ArrayList<>();
        int h = e.height();
        int top = e.row(right);
        int sclera = e.scleraColumn(right);
        int iris = e.irisColumn(right);
        double edge = Math.max(sclera, iris); // the line between the iris and the white
        double away = sclera < iris ? 180.0 : 0.0; // heading from the iris into the white
        double turn = away == 0.0 ? 1.0 : -1.0;    // positive angles lean down the face on either side
        double[] box = {sclera + 0.04, top + 0.04, sclera + 0.96, top + h - 0.04};
        int count = h == 2 ? 4 : 3;
        for (int i = 0; i < count; i++) {
            double v = top + h * (i + 0.5 + 0.5 * (hash(seed + i * 7) - 0.5)) / count;
            double spread = (v - (top + h / 2.0)) / (h / 2.0); // fan out: the top cracks lean up, the bottom ones down
            double heading = away + turn * (spread * 38.0 + 16.0 * (hash(seed + i * 7 + 1) - 0.5));
            crack(cracks, box, edge, v, heading, turn, 0.8 + 0.35 * hash(seed + i * 7 + 2), 0.14, seed * 3 + i * 37);
        }
        if (e.oneEyed()) {
            // 6.0.7: a one-eyed ghoul's single kakugan is the stronger one: more cracks, between the others, a little wider.
            for (int i = 0; i < ONE_EYED_CRACKS; i++) {
                double v = top + h * (i + 0.15 + 0.7 * hash(seed + 90 + i * 5)) / ONE_EYED_CRACKS;
                double spread = (v - (top + h / 2.0)) / (h / 2.0);
                double heading = away + turn * (spread * 52.0 + 24.0 * (hash(seed + 91 + i * 5) - 0.5));
                crack(cracks, box, edge, v, heading, turn, 0.95 + 0.3 * hash(seed + 92 + i * 5), 0.16, seed * 3 + 500 + i * 37);
            }
        }
        return cracks;
    }

    /**
     * One jagged crack from (u, v): its course drifts, and every segment kinks off it by up to about 35 degrees, alternating
     * sides more often than not, so it zigzags like a lightning bolt. Past a third of its length a longer one splits once
     * into a thinner, shorter crack. It stops at the edge of the white.
     */
    private static void crack(List<Vein> cracks, double[] box, double u, double v, double heading, double turn, double length, double root,
                              int seed) {
        int steps = 6;
        double step = length / steps;
        List<double[]> points = new ArrayList<>();
        points.add(new double[] {u, v});
        double course = heading;
        double side = hash(seed) < 0.5 ? -1.0 : 1.0;
        int splitAt = length > 0.85 && hash(seed + 1) < 0.7 ? 3 + (int)(hash(seed + 2) * 3.0) : -1;
        for (int k = 1; k <= steps; k++) {
            course += turn * 10.0 * (hash(seed + 10 + k) - 0.5);
            side = hash(seed + 30 + k) < 0.75 ? -side : side;
            double a = Math.toRadians(course + side * (12.0 + 24.0 * hash(seed + 50 + k)));
            double nu = u + Math.cos(a) * step;
            double nv = v + Math.sin(a) * step;
            if (nu < box[0] || nu > box[2] || nv < box[1] || nv > box[3]) {
                break;
            }
            u = nu;
            v = nv;
            points.add(new double[] {u, v});
            if (k == splitAt) {
                double off = course + (hash(seed + 3) < 0.5 ? -1.0 : 1.0) * (28.0 + 18.0 * hash(seed + 4));
                double rest = length * (1.0 - (double)k / steps) * (0.6 + 0.3 * hash(seed + 5));
                crack(cracks, box, u, v, off, turn, Math.max(0.25, rest), root * 0.65, seed * 7 + 11);
            }
        }
        if (points.size() >= 3) {
            cracks.add(new Vein(points, taper(points.size(), root, root * 0.3, 1.0), 0));
        }
    }

    /**
     * The veins crawling out of one eye into the skin, all from its outer side (away from the nose), the way the user
     * drew them: three out of the outer edge (one rising, one level, one dropping), and two down from under the outer
     * pixel (one straight down the cheek, one down and out). Nothing grows from under the iris or the inner corner, so
     * the veins never gather between the eyes. Each is a smooth, tapering curve that forks once or twice; forks fork
     * again, thinner. They never cross the eyes or leave the face.
     */
    static List<Vein> veins(KakuganEyes e, boolean right, int seed) {
        List<Vein> veins = new ArrayList<>();
        Bounds bounds = faceBounds(e);
        int own = right ? 0 : 1;
        int h = e.height();
        int top = e.row(right);
        int x = e.column(right);
        double out = right ? 180.0 : 0.0;   // straight outward, away from the nose
        double down = right ? -1.0 : 1.0;   // adding this many degrees to "out" turns it down the face
        double outer = right ? x : x + 2.0; // the outer edge of the eye
        double outerPixel = right ? x + 0.5 : x + 1.5;
        // {u, v, heading (degrees, 0 = +u, 90 = down the face), length, width}
        double[][] roots = {
            {outer, top + h * 0.18, out + down * (-22.0 + 10.0 * hash(seed + 41)), 1.3 + 0.5 * hash(seed + 42), 0.15},
            {outer, top + h * 0.55, out + down * (4.0 + 10.0 * hash(seed + 43)), 1.5 + 0.6 * hash(seed + 44), 0.18},
            {outer, top + h * 0.92, out + down * (34.0 + 12.0 * hash(seed + 45)), 1.5 + 0.6 * hash(seed + 46), 0.17},
            {outerPixel - (right ? 0.25 : -0.25), top + h, out + down * (62.0 + 10.0 * hash(seed + 47)), 1.6 + 0.6 * hash(seed + 48), 0.18},
            {outerPixel + (right ? 0.3 : -0.3), top + h, 90.0 - down * (8.0 + 10.0 * hash(seed + 49)), 1.7 + 0.6 * hash(seed + 50), 0.17}};
        for (int i = 0; i < roots.length; i++) {
            double[] r = roots[i];
            branch(veins, bounds, own, r[0], r[1], r[2], r[3], r[4] * VEIN_SCALE, 0, seed * 7 + i * 101);
        }
        if (e.oneEyed()) {
            // 6.0.7: a one-eyed ghoul's veins spread further, still only from the outer side: up over the brow, long out
            // along the temple, and long down the cheek, thicker than the others.
            double[][] more = {
                {outerPixel, top, out + down * (-72.0 + 12.0 * hash(seed + 61)), 2.1 + 0.6 * hash(seed + 62), 0.17},
                {outer, top + h * 0.35, out + down * (-10.0 + 12.0 * hash(seed + 63)), 2.8 + 0.7 * hash(seed + 64), 0.2},
                {outer, top + h * 0.75, out + down * (18.0 + 12.0 * hash(seed + 65)), 2.6 + 0.7 * hash(seed + 66), 0.19},
                {outerPixel, top + h, out + down * (48.0 + 12.0 * hash(seed + 67)), 2.8 + 0.7 * hash(seed + 68), 0.2}};
            for (int i = 0; i < more.length; i++) {
                double[] r = more[i];
                branch(veins, bounds, own, r[0], r[1], r[2], r[3], r[4] * VEIN_SCALE, 0, seed * 7 + (roots.length + i) * 101);
            }
        }
        return veins;
    }

    /**
     * Grows one branch from (u, v), tapering to a hair-thin tip and forking off thinner, shorter branches part of the way
     * along. 6.0.8: it winds like a real vein instead of running ruler-straight. It arches (a C, or for most an S that bends
     * back the other way part of the way along), meanders in a long slow wave with a short tighter one on top, and kinks a
     * little at random; it never turns more than about 70 degrees off its course, so the veins still spread outward. It stops
     * where the face ends or an eye begins.
     */
    private static void branch(List<Vein> veins, Bounds bounds, int own, double u, double v, double heading, double length, double width, int depth,
                               int seed) {
        int steps = Math.max(5, (int)Math.round(length / 0.17));
        double step = length / steps;
        double arch = (hash(seed) < 0.5 ? -1.0 : 1.0) * (55.0 + 55.0 * hash(seed + 9)) / Math.max(0.6, length); // degrees per pixel
        double flipAt = hash(seed + 10) < 0.65 ? 0.35 + 0.3 * hash(seed + 11) : 2.0; // where an S bends back (past 1: a C)
        double slow = 60.0 + 50.0 * hash(seed + 1);   // degrees per pixel, a wave about 2 pixels long
        double slowFreq = 2.4 + 1.4 * hash(seed + 12);
        double fast = 80.0 + 60.0 * hash(seed + 13);  // a wave about a pixel long
        double fastFreq = 5.5 + 3.0 * hash(seed + 14);
        double slowPhase = hash(seed + 2) * 6.283;
        double fastPhase = hash(seed + 15) * 6.283;
        List<double[]> points = new ArrayList<>();
        points.add(new double[] {u, v});
        double h = heading;
        int forkAt = depth >= 2 ? -1 : (int)Math.round(steps * (0.3 + 0.25 * hash(seed + 3)));
        int secondFork = depth == 0 && hash(seed + 4) < 0.6 ? (int)Math.round(steps * (0.6 + 0.15 * hash(seed + 5))) : -1;
        for (int k = 1; k <= steps; k++) {
            double t = (double)k / steps;
            double along = k * step;
            double bendHere = t < flipAt ? arch : -1.3 * arch;
            h += step * (bendHere + slow * Math.sin(slowPhase + along * slowFreq) + fast * Math.sin(fastPhase + along * fastFreq))
                + 8.0 * (hash(seed + 200 + k) - 0.5);
            double off = h - heading;
            if (Math.abs(off) > 70.0) {
                h = heading + Math.signum(off) * 70.0;
            }
            // However it winds, it never heads in toward the nose (the right eye's nose side is +u, the left eye's -u).
            double fromNose = h - (own == 0 ? 0.0 : 180.0);
            fromNose = ((fromNose + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
            if (Math.abs(fromNose) < 82.0) {
                h += (fromNose < 0.0 ? -82.0 : 82.0) - fromNose;
            }
            double a = Math.toRadians(h);
            double nu = u + Math.cos(a) * step;
            double nv = v + Math.sin(a) * step;
            if (!bounds.allows(nu, nv, own)) {
                break;
            }
            u = nu;
            v = nv;
            points.add(new double[] {u, v});
            if (k == forkAt || k == secondFork) {
                double side = hash(seed + 6 + k) < 0.5 ? -1.0 : 1.0;
                double angle = h + side * (24.0 + 20.0 * hash(seed + 7 + k));
                double rest = length * (1.0 - (double)k / steps);
                double widthHere = width * Math.pow(1.0 - (double)k / steps, 0.8);
                branch(veins, bounds, own, u, v, angle, Math.max(0.35, rest * (0.55 + 0.3 * hash(seed + 8 + k))), widthHere * 0.72, depth + 1,
                    seed * 13 + k * 7 + 3);
            }
        }
        if (points.size() >= 3) {
            veins.add(new Vein(points, taper(points.size(), width, 0.025, 0.85), depth));
        }
    }

    /** Widths along a branch: {@code root} at the start thinning to {@code tip}. */
    private static double[] taper(int count, double root, double tip, double power) {
        double[] widths = new double[count];
        for (int i = 0; i < count; i++) {
            double t = count == 1 ? 0.0 : (double)i / (count - 1);
            widths[i] = tip + (root - tip) * Math.pow(1.0 - t, power);
        }
        return widths;
    }

    /**
     * A vein drawn as one continuous ribbon (joined corners, no gaps at the bends), {@code scale} times its width, grown
     * to {@code reach} (0..1) of its length. With {@code fadeAfter} above 0, its strength fades out past that share of its
     * length (the bright core line only shows near the root).
     */
    private static void ribbon(List<Quad> out, Vein vein, double scale, double reach, int lift, int rgb, double strength, boolean glow,
                               double fadeAfter) {
        List<double[]> points = vein.points();
        int n = points.size();
        double shown = clamp01(reach) * (n - 1);
        if (shown <= 0.0 || n < 2) {
            return;
        }
        int last = Math.min(n - 1, (int)Math.ceil(shown));
        double[][] p = new double[last + 1][];
        double[] w = new double[last + 1];
        for (int i = 0; i <= last; i++) {
            p[i] = points.get(i);
            w[i] = vein.widths()[i] * scale;
        }
        double part = shown - (last - 1);
        if (part < 1.0) {
            double[] a = points.get(last - 1);
            double[] b = points.get(last);
            p[last] = new double[] {a[0] + (b[0] - a[0]) * part, a[1] + (b[1] - a[1]) * part};
        }
        double[][] normals = new double[last + 1][];
        for (int i = 0; i <= last; i++) {
            double[] before = i > 0 ? p[i - 1] : p[i];
            double[] after = i < last ? p[i + 1] : p[i];
            double du = after[0] - before[0];
            double dv = after[1] - before[1];
            double length = Math.sqrt(du * du + dv * dv);
            normals[i] = length < 1.0E-9 ? new double[] {0.0, 1.0} : new double[] {-dv / length, du / length};
        }
        for (int i = 0; i < last; i++) {
            double t0 = (double)i / (n - 1);
            double t1 = (double)(i + 1) / (n - 1);
            double s0 = fadeAfter > 0.0 ? strength * (1.0 - smooth(t0 / fadeAfter)) : strength;
            double s1 = fadeAfter > 0.0 ? strength * (1.0 - smooth(t1 / fadeAfter)) : strength;
            if (s0 <= 0.002 && s1 <= 0.002) {
                continue;
            }
            int c0 = colour(rgb, s0, glow);
            int c1 = colour(rgb, s1, glow);
            double[][] corners = {
                {p[i][0] + normals[i][0] * w[i] / 2, p[i][1] + normals[i][1] * w[i] / 2},
                {p[i + 1][0] + normals[i + 1][0] * w[i + 1] / 2, p[i + 1][1] + normals[i + 1][1] * w[i + 1] / 2},
                {p[i + 1][0] - normals[i + 1][0] * w[i + 1] / 2, p[i + 1][1] - normals[i + 1][1] * w[i + 1] / 2},
                {p[i][0] - normals[i][0] * w[i] / 2, p[i][1] - normals[i][1] * w[i] / 2}};
            quad(out, corners, lift, new int[] {c0, c1, c1, c0}, glow);
        }
    }

    // ------------------------------------------------------------------ primitives

    /** Where a face-pixel point lands on the head (on the face, floating {@code lift} pixels off it). */
    static double[] place(double u, double v, double lift) {
        return new double[] {Math.max(0.0, Math.min(8.0, u)) - 4.0, Math.max(0.0, Math.min(8.0, v)) - 8.0, -4.0 - lift};
    }

    private static void rect(List<Quad> out, double u0, double v0, double u1, double v1, int lift, int rgb, double strength, boolean glow) {
        int colour = colour(rgb, strength, glow);
        quad(out, new double[][] {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}}, lift, new int[] {colour, colour, colour, colour}, glow);
    }

    /** A soft dark ring around an eye: darkest at the eye's edge, fading out over the skin. */
    private static void socket(List<Quad> out, double cu, double cv, int h, int segments, double strength) {
        if (strength <= 0.002) {
            return;
        }
        int dark = colour(SOCKET, 0.45 * strength, false);
        int clear = colour(SOCKET, 0.0, false);
        double innerU = 1.0;
        double innerV = h / 2.0;
        double outerU = 1.95;
        double outerV = h / 2.0 + 0.85;
        for (int s = 0; s < segments; s++) {
            double a0 = 2.0 * Math.PI * s / segments;
            double a1 = 2.0 * Math.PI * (s + 1) / segments;
            double[][] corners = {
                {cu + innerU * squarish(Math.cos(a0)), cv + innerV * squarish(Math.sin(a0))},
                {cu + outerU * Math.cos(a0), cv + outerV * Math.sin(a0)},
                {cu + outerU * Math.cos(a1), cv + outerV * Math.sin(a1)},
                {cu + innerU * squarish(Math.cos(a1)), cv + innerV * squarish(Math.sin(a1))}};
            quad(out, corners, SOCKET_LIFT, new int[] {dark, clear, clear, dark}, false);
        }
    }

    /** Pushes a point on a circle out toward the enclosing square, so the ring's inside hugs the rectangular eye. */
    private static double squarish(double c) {
        return Math.signum(c) * Math.min(1.0, Math.abs(c) * 1.42);
    }

    /**
     * The iris's glow spilling onto the skin around the eye: a soft red ellipse of added light, laid as a fine grid
     * aligned to the skin pixels so it can leave out the eye itself (the black stays pure black; the iris has its
     * own core glow) and the space beyond the face.
     */
    private static void halo(List<Quad> out, double cu, double cv, double ru, double rv, double[] eye, double cell, double strength) {
        double u0 = Math.floor((cu - ru) / cell) * cell;
        double v0 = Math.floor((cv - rv) / cell) * cell;
        for (double u = u0; u < cu + ru; u += cell) {
            for (double v = v0; v < cv + rv; v += cell) {
                double u1 = u + cell;
                double v1 = v + cell;
                boolean inEye = u >= eye[0] - 1.0E-6 && u1 <= eye[2] + 1.0E-6 && v >= eye[1] - 1.0E-6 && v1 <= eye[3] + 1.0E-6;
                if (inEye || u < 0.0 || u1 > 8.0 || v < 0.0 || v1 > 8.0) {
                    continue;
                }
                double[][] corners = {{u, v}, {u1, v}, {u1, v1}, {u, v1}};
                int[] argb = new int[4];
                boolean lit = false;
                for (int i = 0; i < 4; i++) {
                    double du = (corners[i][0] - cu) / ru;
                    double dv = (corners[i][1] - cv) / rv;
                    double fall = Math.max(0.0, 1.0 - Math.sqrt(du * du + dv * dv));
                    // Falls off as fall^1.5 (5.0.11: was fall^2): the red spreads further over the skin before it fades.
                    double glow = strength * fall * Math.sqrt(fall);
                    lit |= glow > 0.004;
                    argb[i] = colour(GLOW, glow, true);
                }
                if (lit) {
                    quad(out, corners, GLOW_LIFT, argb, true);
                }
            }
        }
    }

    private static void quad(List<Quad> out, double[][] uv, int level, int[] argb, boolean glow) {
        float[] xyz = new float[12];
        double lift = level * LAYER.get();
        for (int i = 0; i < 4; i++) {
            double[] p = place(uv[i][0], uv[i][1], lift);
            xyz[i * 3] = (float)p[0];
            xyz[i * 3 + 1] = (float)p[1];
            xyz[i * 3 + 2] = (float)p[2];
        }
        out.add(new Quad(xyz, 0.0F, 0.0F, -1.0F, argb, glow, false));
    }

    /** Bloodlust at which the eyes start to turn on their own, and at which they are fully turned. */
    public static final float FLARE_FROM = 0.45F;
    public static final float FLARE_FULL = 0.8F;

    /** How awake a kakugan should be: fully with the kagune out, otherwise as much as the bloodlust (hunger, frenzy) says. */
    public static float awake(boolean kaguneOut, float bloodlust) {
        float flare = Math.max(0.0F, Math.min(1.0F, (bloodlust - FLARE_FROM) / (FLARE_FULL - FLARE_FROM)));
        return kaguneOut ? 1.0F : flare * flare * (3.0F - 2.0F * flare);
    }

    /** Solid colours fade in by alpha; glowing colours (added light) by brightness. */
    static int colour(int rgb, double strength, boolean glow) {
        double s = clamp01(strength);
        if (glow) {
            int r = (int)Math.round(((rgb >> 16) & 0xFF) * s);
            int g = (int)Math.round(((rgb >> 8) & 0xFF) * s);
            int b = (int)Math.round((rgb & 0xFF) * s);
            return 0xFF000000 | r << 16 | g << 8 | b;
        }
        return (int)Math.round(s * 255.0) << 24 | (rgb & 0xFFFFFF);
    }

    /** A heartbeat (0..1): a strong thump, a softer second one, then rest, about 70 beats a minute. */
    static double heartbeat(double time) {
        double t = time / 0.86;
        double phase = t - Math.floor(t);
        double first = Math.exp(-Math.pow((phase - 0.05) / 0.06, 2.0));
        double second = 0.6 * Math.exp(-Math.pow((phase - 0.26) / 0.06, 2.0));
        return clamp01(first + second);
    }

    static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double)(1L << 53);
    }

    static double smooth(double value) {
        double t = clamp01(value);
        return t * t * (3.0 - 2.0 * t);
    }

    static double clamp01(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }
}
