package com.hex.kagune.control.furuta;

import static com.hex.kagune.control.furuta.FurutaMesh.add;
import static com.hex.kagune.control.furuta.FurutaMesh.argb;
import static com.hex.kagune.control.furuta.FurutaMesh.clamp01;
import static com.hex.kagune.control.furuta.FurutaMesh.facet;
import static com.hex.kagune.control.furuta.FurutaMesh.glow;
import static com.hex.kagune.control.furuta.FurutaMesh.hash;
import static com.hex.kagune.control.furuta.FurutaMesh.mix;
import static com.hex.kagune.control.furuta.FurutaMesh.mul;
import static com.hex.kagune.control.furuta.FurutaMesh.normalize;
import static com.hex.kagune.control.furuta.FurutaMesh.smooth;
import static com.hex.kagune.control.furuta.FurutaMesh.sub;

import com.hex.kagune.control.ayato.UkakuModel;

/**
 * The kakuja's body (pure, tested; 7.0.0), after the anime's and the manga's final form: his whole body swallowed by a black hide
 * of kagune ({@link FurutaSkin}: red whorls like fingerprints, three burning slashes down the abdomen) and, built here, what stands
 * out of it: black claws over the hands, the two glowing sockets in his lower back the colossi grow out of, and the head.
 *
 * <p>The head is his own head gone under the hide: a wild crown of black spikes sweeping back off the skull and falling over the
 * brow like hair, one blazing white star where his right eye was, and a grin too wide for the face, corner to corner across the
 * cheeks, two rows of great square teeth; the jaw drops open in an attack, the lower row going with it, the mouth black and red
 * inside.
 *
 * <p>Part pixels as every layer: x to the player's left, y down, z backward; the face at z = -4 (the hide stands {@code 0.6} off it).
 */
public final class FurutaArmor {
    static final int BLACK = 0x07040A;
    static final int BLACK_HI = 0x2A0E14;
    static final int CRIMSON = 0x6E0C16;
    /** The hair: black, the deepest crimson only at the roots. */
    static final int HAIR_ROOT = 0x2A070C;
    static final int TOOTH = 0xF2EEE4;
    static final int TOOTH_SHADE = 0xBDB3A6;

    /** Which body part a piece goes on. */
    public enum Part {
        HEAD, TORSO, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG
    }

    private FurutaArmor() {
    }

    /** How far the hide has spread at {@code grow} over what grows in at {@code order} (0 the sockets .. 1 the crown of the head). */
    public static double reveal(double grow, double order) {
        return smooth((grow - order * 0.78) / 0.22);
    }

    /** What stands out of the hide on {@code part} (the hide itself is {@link FurutaSkin}). */
    public static void build(UkakuModel.Sink sink, Part part, boolean slim, double grow, double time, boolean fine) {
        if (grow <= 0.002) {
            return;
        }
        switch (part) {
            case TORSO -> {
                double k = reveal(grow, 0.0);
                for (int side = -1; side <= 1; side += 2) {
                    socket(sink, new double[] {1.3 * side, 9.4, 2.45}, new double[] {0.0, 0.0, 1.0}, 1.2 * k, time, fine);
                }
            }
            case RIGHT_ARM -> claws(sink, true, slim, reveal(grow, 0.7), fine);
            case LEFT_ARM -> claws(sink, false, slim, reveal(grow, 0.7), fine);
            default -> {
            }
        }
    }

    /** A glowing socket the colossi grow out of: a ring of torn black flesh round a red-lit hole. */
    static void socket(UkakuModel.Sink sink, double[] at, double[] out, double radius, double time, boolean fine) {
        if (radius < 0.02) {
            return;
        }
        int n = fine ? 10 : 6;
        double[] u = {1.0, 0.0, 0.0};
        double[] v = {0.0, 1.0, 0.0};
        double pulse = 0.7 + 0.3 * Math.sin(time * 0.25);
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2.0 * k / n;
            double a1 = Math.PI * 2.0 * (k + 1) / n;
            double[] e0 = add(mul(u, Math.cos(a0)), mul(v, Math.sin(a0)));
            double[] e1 = add(mul(u, Math.cos(a1)), mul(v, Math.sin(a1)));
            double[] in0 = add(at, mul(e0, radius * 0.55));
            double[] in1 = add(at, mul(e1, radius * 0.55));
            double[] rim0 = add(add(at, mul(e0, radius)), mul(out, 0.45 * radius));
            double[] rim1 = add(add(at, mul(e1, radius)), mul(out, 0.45 * radius));
            double[] base0 = add(at, mul(e0, radius * 1.35));
            double[] base1 = add(at, mul(e1, radius * 1.35));
            int hot = argb(mix(0x7A0612, 0xFF3A2A, pulse), 1.0);
            glow(sink, at, at, in1, in0, hot, hot, hot, hot);
            facet(sink, in0, in1, rim1, rim0, sub(at, out), argb(CRIMSON, 1.0), argb(CRIMSON, 1.0), argb(BLACK_HI, 1.0), argb(BLACK_HI, 1.0));
            facet(sink, rim0, rim1, base1, base0, sub(at, out), argb(BLACK_HI, 1.0), argb(BLACK_HI, 1.0), argb(BLACK, 1.0), argb(BLACK, 1.0));
        }
    }

    /** Four black claws curving forward and down off the hand, grown {@code k} of the way. */
    static void claws(UkakuModel.Sink sink, boolean right, boolean slim, double k, boolean fine) {
        if (k < 0.02) {
            return;
        }
        double inner = right ? 1.0 : -1.0;
        double outer = right ? (slim ? -2.0 : -3.0) : (slim ? 2.0 : 3.0);
        for (int i = 0; i < 4; i++) {
            double f = (i + 0.5) / 4.0;
            double x = inner + (outer - inner) * f;
            double[] base = {x, 10.2, -2.1};
            double length = (2.6 + 0.5 * Math.sin(Math.PI * f)) * k;
            FurutaMesh.horn(sink, base, new double[] {(outer - inner) * 0.04, 1.0, -0.55}, new double[] {0.0, 0.0, 1.0}, length, 0.38 * k, fine ? 5 : 4,
                fine ? 3 : 2, BLACK_HI, 0x020102, 1.0);
        }
    }

    // ---------------------------------------------------------------- the head

    /** The face of the hide (head pixels, z): the head box's front pushed out by the hide's stand-off. */
    static final double FACE = -4.0 - 0.6;

    /**
     * The transformed head in head pixels. {@code grow}: how far the hide has closed over it (0..1, the head goes under last);
     * {@code jaw}: the mouth (0 the grin shut .. 1 the jaw dropped); {@code eyeX, eyeY}: the right eye's centre on the face (pixels,
     * from the player's own eye placement); {@code stare}: the star blazing wider in an attack; {@code time}: ticks.
     */
    public static void head(UkakuModel.Sink sink, double grow, double jaw, double eyeX, double eyeY, double stare, double time, boolean fine) {
        double k = reveal(grow, 0.9);
        if (k < 0.02) {
            return;
        }
        crown(sink, k, eyeX, eyeY, time, fine);
        grin(sink, k, jaw, fine);
        double eye = smooth((k - 0.4) / 0.6);
        if (eye > 0.02) {
            // The one eye: a great white star blazing in the middle of the face over the grin, dead centre whatever his eyes.
            star(sink, STAR_X, STAR_Y, eye, stare, time, fine);
        }
    }

    /**
     * The crown: black spikes sweeping up and back off the skull, longer at the back, a few hanging down over the brow like hair
     * (never over the eye), red at the roots.
     */
    static void crown(UkakuModel.Sink sink, double k, double eyeX, double eyeY, double time, boolean fine) {
        double top = -8.0 - 0.6;
        double back = 4.0 + 0.6;
        double side = 4.0 + 0.6;
        int count = fine ? 40 : 18;
        for (int i = 0; i < count; i++) {
            double a = hash(i * 17 + 3);
            double b = hash(i * 29 + 5);
            double c = hash(i * 41 + 7);
            double[] base;
            double[] dir;
            double length;
            double thick;
            int kind = i % 4;
            if (kind <= 1) {
                // Off the top of the skull: great spikes thrown up and back, fanning out wider than the head, the wildest at the back.
                double x = (a - 0.5) * 8.6;
                double z = -3.8 + b * 8.0;
                base = new double[] {x, top - 0.05, z};
                double sweep = 0.2 + 0.8 * b;
                dir = normalize(new double[] {x * 0.22 + (c - 0.5) * 0.9, -1.0 + 0.35 * sweep, 0.25 + 1.0 * sweep});
                length = 7.0 + 9.0 * (0.45 + 0.55 * sweep) * (0.6 + 0.5 * c);
                thick = 1.2 + 0.9 * c;
            } else if (kind == 2) {
                // Off the back of the head: a mane falling back and down past the neck.
                double x = (a - 0.5) * 8.2;
                double y = -8.2 + b * 6.0;
                base = new double[] {x, y, back + 0.05};
                dir = normalize(new double[] {x * 0.12 + (c - 0.5) * 0.6, 0.45 + 0.55 * b, 1.0});
                length = 6.0 + 7.0 * c;
                thick = 1.1 + 0.7 * c;
            } else {
                // Off the sides above the ears: swept out and back, so the head is a spiked crown from the front too.
                double s = a < 0.5 ? -1.0 : 1.0;
                double y = -8.0 + b * 3.4;
                base = new double[] {s * (side + 0.05), y, -1.5 + c * 5.0};
                dir = normalize(new double[] {s * 1.0, -0.55 - 0.3 * b, 0.6 + 0.4 * c});
                length = 5.5 + 6.5 * c;
                thick = 1.0 + 0.6 * b;
            }
            double sway = 0.06 * Math.sin(time * 0.06 + i);
            dir = normalize(add(dir, new double[] {sway, 0.0, 0.0}));
            FurutaMesh.horn(sink, base, dir, new double[] {0.0, 1.0, 0.5}, length * (0.25 + 0.75 * k), thick * k, fine ? 5 : 4, fine ? 3 : 2,
                HAIR_ROOT, BLACK, 1.0);
        }
        // Thick strands hanging down at both temples, outside the star's points (never in front of it).
        double[][] fringe = {{4.0, 0.3, 5.0}, {3.6, 0.15, 4.0}, {-4.0, -0.3, 5.2}, {-3.6, -0.15, 4.2}};
        for (double[] strand : fringe) {
            double[] base = {strand[0], top + 0.2, FACE + 0.3};
            double[] dir = normalize(new double[] {strand[1], 1.0, -0.12});
            FurutaMesh.horn(sink, base, dir, new double[] {0.0, 0.0, 1.0}, strand[2] * k, 0.95 * k, fine ? 5 : 4, fine ? 3 : 2, BLACK_HI, BLACK, 1.0);
        }
    }

    /** The grin's half-width (pixels): the whole face, corner to corner. */
    static final double GRIN = 4.4;

    /** The grin's depth (head z): laid on the face of the hide (the mouth's inside painted dark just over it, the teeth proud of it). */
    static double grinZ(double x) {
        return FACE - 0.04;
    }

    /** The upper lip (head y) at {@code x}: a smile, its corners pulled up high. */
    static double upperLip(double x) {
        double f = x / GRIN;
        return -2.2 - 1.0 * f * f;
    }

    /** The mouth's height at {@code x} when shut (pixels), tallest in the middle. */
    static double mouthHeight(double x) {
        double f = Math.min(1.0, Math.abs(x) / GRIN);
        return 2.1 * Math.sqrt(Math.max(0.0, 1.0 - f * f));
    }

    /**
     * The grin: two rows of great square teeth from corner to corner, black lips, the inside of the mouth black and red; open, the
     * lower row drops with the jaw (most at the front) and the mouth gapes.
     */
    static void grin(UkakuModel.Sink sink, double k, double jaw, boolean fine) {
        double open = clamp01(jaw);
        int teeth = fine ? 12 : 8;
        double gap = 0.12;
        // The inside: a dark wall behind the teeth, from the upper lip to the lower one.
        int cols = fine ? 16 : 9;
        for (int i = 0; i < cols; i++) {
            double x0 = -GRIN + 2.0 * GRIN * i / cols;
            double x1 = -GRIN + 2.0 * GRIN * (i + 1) / cols;
            double[] a = {x0, upperLip(x0), grinZ(x0)};
            double[] b = {x1, upperLip(x1), grinZ(x1)};
            double[] c = {x1, upperLip(x1) + mouthHeight(x1) + drop(x1, open), grinZ(x1)};
            double[] d = {x0, upperLip(x0) + mouthHeight(x0) + drop(x0, open), grinZ(x0)};
            int top = argb(0x2A040A, k);
            int deep = argb(0x050103, k);
            facet(sink, a, b, c, d, new double[] {0.0, -2.0, 4.0}, top, top, deep, deep);
        }
        // The teeth: square blocks with a little depth, the upper row hanging from the lip, the lower standing on the jaw.
        for (int i = 0; i < teeth; i++) {
            double x0 = -GRIN * 0.97 + 2.0 * GRIN * 0.97 * i / teeth + gap * 0.5;
            double x1 = -GRIN * 0.97 + 2.0 * GRIN * 0.97 * (i + 1) / teeth - gap * 0.5;
            double xm = (x0 + x1) * 0.5;
            double half = mouthHeight(xm) * 0.5;
            if (half < 0.15) {
                continue;
            }
            double vary = 0.88 + 0.2 * hash(i * 13 + 1);
            double top = upperLip(xm);
            tooth(sink, x0, x1, top, top + half * vary - 0.04, grinZ(xm) - 0.4, k, fine);
            double low = upperLip(xm) + 2.0 * half + drop(xm, open);
            tooth(sink, x0, x1, low - half * (0.92 + 0.16 * hash(i * 7 + 2)) + 0.04, low, grinZ(xm) - 0.4, k, fine);
        }
        // Black lips round it, and the cuts running up from the corners into the cheeks.
        int segs = fine ? 14 : 8;
        for (int i = 0; i < segs; i++) {
            double x0 = -GRIN + 2.0 * GRIN * i / segs;
            double x1 = -GRIN + 2.0 * GRIN * (i + 1) / segs;
            lip(sink, x0, upperLip(x0) - 0.28, x1, upperLip(x1) - 0.28, 0.32, k);
            lip(sink, x0, upperLip(x0) + mouthHeight(x0) + drop(x0, open) + 0.02, x1, upperLip(x1) + mouthHeight(x1) + drop(x1, open) + 0.02, 0.32, k);
        }
        for (int side = -1; side <= 1; side += 2) {
            double x = side * GRIN;
            lip(sink, x, upperLip(x) - 0.1, x + side * 0.35, upperLip(x) - 1.0, 0.18, k);
        }
    }

    /** How far the lower lip drops at {@code x} with the jaw {@code open} (pixels): most at the front. */
    static double drop(double x, double open) {
        double f = Math.min(1.0, Math.abs(x) / GRIN);
        return open * 3.0 * Math.sqrt(Math.max(0.0, 1.0 - f * f));
    }

    /** One square tooth from {@code x0} to {@code x1}, {@code y0} to {@code y1} (head pixels), its face at {@code z}. */
    static void tooth(UkakuModel.Sink sink, double x0, double x1, double y0, double y1, double z, double k, boolean fine) {
        double depth = 0.35;
        double[] a = {x0, y0, z};
        double[] b = {x1, y0, z};
        double[] c = {x1, y1, z};
        double[] d = {x0, y1, z};
        int lit = argb(TOOTH, k);
        int shade = argb(TOOTH_SHADE, k);
        double[] inside = {(x0 + x1) * 0.5, (y0 + y1) * 0.5, z + 2.0};
        facet(sink, a, b, c, d, inside, lit, lit, shade, shade);
        if (fine) {
            double[] a2 = {x0, y0, z + depth};
            double[] b2 = {x1, y0, z + depth};
            double[] c2 = {x1, y1, z + depth};
            double[] d2 = {x0, y1, z + depth};
            facet(sink, a, a2, d2, d, inside, shade, shade, shade, shade);
            facet(sink, b, c, c2, b2, inside, shade, shade, shade, shade);
            facet(sink, d, d2, c2, c, inside, shade, shade, shade, shade);
        }
    }

    /** A strip of black lip from one point of the mouth's edge to another, {@code width} pixels wide, just proud of the face. */
    static void lip(UkakuModel.Sink sink, double x0, double y0, double x1, double y1, double width, double k) {
        double z0 = grinZ(x0) - 0.45;
        double z1 = grinZ(x1) - 0.45;
        double[] a = {x0, y0 - width * 0.5, z0};
        double[] b = {x1, y1 - width * 0.5, z1};
        double[] c = {x1, y1 + width * 0.5, z1};
        double[] d = {x0, y0 + width * 0.5, z0};
        int black = argb(BLACK, k);
        facet(sink, a, b, c, d, new double[] {(x0 + x1) * 0.5, (y0 + y1) * 0.5, 2.0}, black, black, black, black);
    }

    /** Where the star burns on the face (head pixels): the middle of the face, its points from the brow to just over the grin. */
    static final double STAR_X = 0.0;
    static final double STAR_Y = -5.0;
    /** How far in front of the face it stands (pixels): clear of every swell of the hide over the face. */
    static final double STAR_OUT = 0.45;
    /** A classic five-pointed star: its points this far out (pixels), its inner corners this share of that. */
    static final double STAR_RADIUS = 3.2;
    static final double STAR_INNER = 0.42;

    /**
     * The one eye: a solid blazing white star (one shape, never a spray of strokes), its light spilling round it in layers of
     * bloom (white: anything coloured over it would tint it). All of it is light (drawn added on at full brightness), so it burns in
     * the dark. It blazes up through an attack ({@code stare}).
     */
    static void star(UkakuModel.Sink sink, double x, double y, double grow, double stare, double time, boolean fine) {
        double z = FACE - STAR_OUT;
        double flicker = 1.0 + 0.05 * Math.sin(time * 0.9) + 0.03 * Math.sin(time * 2.3);
        double size = grow * flicker * (1.0 + 0.2 * clamp01(stare));
        double blaze = clamp01(0.75 + 0.25 * stare);
        int steps = fine ? 6 : 3;
        // The star itself first, solid and white hot (what is drawn behind it never tints it).
        starShape(sink, x, y, z, size, steps, argb(0xFFFFFF, grow), argb(0xFFFAF8, grow));
        // The bloom: the star again, larger and fainter each time, its light spilling out over the face.
        double[][] bloom = {{1.18, 0.6, 0xFFFFFF}, {1.4, 0.3, 0xFFF4F2}, {1.75, 0.13, 0xFFE8E4}};
        for (int i = 0; i < bloom.length; i++) {
            starShape(sink, x, y, z + 0.18 + 0.08 * i, size * bloom[i][0], steps, argb((int)bloom[i][2], bloom[i][1] * grow * blaze),
                argb((int)bloom[i][2], 0.0));
        }
    }

    /**
     * The star's shape at {@code scale}, flat on the face at {@code z}: five points (one straight up), straight edges in to its inner
     * corners, filled as a fan from its middle, {@code inner} at the middle, {@code outer} at the outline.
     */
    static void starShape(UkakuModel.Sink sink, double x, double y, double z, double scale, int steps, int inner, int outer) {
        double[] centre = {x, y, z};
        double[][] corners = new double[10][];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * i / 5.0;
            double r = (i % 2 == 0 ? STAR_RADIUS : STAR_RADIUS * STAR_INNER) * scale;
            // Up on the face is -y.
            corners[i] = new double[] {x + Math.sin(a) * r, y - Math.cos(a) * r, z};
        }
        // Each point a solid kite: the middle, the inner corner before it, its tip, the inner corner after it.
        for (int k = 0; k < 5; k++) {
            double[] before = corners[(2 * k + 9) % 10];
            double[] tip = corners[2 * k];
            double[] after = corners[2 * k + 1];
            glow(sink, centre, before, tip, after, inner, inner, outer, inner);
        }
    }

    /** A flat glowing disc on the face (z), {@code inner} colour at its centre fading to {@code outer} at its rim. */
    static void disc(UkakuModel.Sink sink, double x, double y, double z, double radius, int segments, int inner, int outer) {
        double[] c = {x, y, z};
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            double[] p0 = {x + Math.cos(a0) * radius, y + Math.sin(a0) * radius, z};
            double[] p1 = {x + Math.cos(a1) * radius, y + Math.sin(a1) * radius, z};
            glow(sink, c, c, p1, p0, inner, inner, outer, outer);
        }
    }
}
