package com.hex.kagune.control.client;

import com.hex.kagune.control.ukaku.UkakuWing;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry for the animated silhouettes on the path choice cards: a figure seen from behind, with Rinkaku's
 * Rinkaku tendrils from the lower back or Ukaku's Ukaku wings from the shoulder blades (the Ukaku path notes, "Look").
 *
 * <p>Colours are opaque RGB, already blended against the card background, because the GUI draws strokes as many
 * small overlapping fills and real alpha would darken every overlap. Every mark stays inside the box it was made
 * for (tested), so nothing bleeds out of a card at any GUI size.
 */
public final class PathSilhouette {
    /** Rinkaku path (Rinkaku): crimson scales with a darker core. */
    public static final int RINKAKU = 0xC4162E;
    public static final int RINKAKU_DARK = 0x6E0A1A;
    /** Ukaku path (Ukaku, canon vermilion): body, deep crimson core, hot crystallized edge. */
    public static final int UKAKU = 0xE0402A;
    public static final int UKAKU_CORE = 0x5A0A10;
    public static final int UKAKU_EDGE = 0xFFB08A;
    public static final int FIGURE = 0x2E2427;
    private static final float FIT_MARGIN = 2.5F;

    /** Something to draw. Marks are drawn in list order. */
    public sealed interface Mark permits Stroke, Disc, Shape {
    }

    /** A straight, tapered stroke (thickness {@code t0} at the start, {@code t1} at the end). */
    public record Stroke(float x0, float y0, float x1, float y1, float t0, float t1, int rgb) implements Mark {
    }

    /** A filled disc. */
    public record Disc(float cx, float cy, float r, int rgb) implements Mark {
    }

    /** A filled convex polygon (3 or 4 corners) in ARGB: translucent layers of the Ukaku wings. */
    public record Shape(float[] x, float[] y, int argb) implements Mark {
        @Override
        public boolean equals(Object other) {
            return other instanceof Shape shape && shape.argb == this.argb && java.util.Arrays.equals(shape.x, this.x)
                && java.util.Arrays.equals(shape.y, this.y);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * java.util.Arrays.hashCode(this.x) + java.util.Arrays.hashCode(this.y)) + this.argb;
        }

        @Override
        public String toString() {
            return "Shape[x=" + java.util.Arrays.toString(this.x) + ", y=" + java.util.Arrays.toString(this.y) + ", argb="
                + Integer.toHexString(this.argb) + "]";
        }
    }

    private PathSilhouette() {
    }

    /** Body proportions for a figure that fits {@code box}. */
    private record Figure(float cx, float headY, float headR, float shoulderY, float shoulderHalf, float waistY, float footY, float size) {
        static Figure in(PathChoiceLayout.Box box) {
            return in(box, 0.64F); // small figure: the kagune is the point
        }

        static Figure in(PathChoiceLayout.Box box, float share) {
            float size = box.height() * share;
            float footY = box.bottom() - FIT_MARGIN - 1.0F;
            float top = footY - size;
            float headR = Math.max(1.5F, size * 0.075F);
            return new Figure(box.x() + box.width() / 2.0F, top + headR + 1.0F, headR, top + size * 0.22F,
                size * 0.13F, top + size * 0.56F, footY, size);
        }
    }

    private static void figure(List<Mark> marks, Figure f, int bg, float intensity) {
        int body = blend(bg, FIGURE, 0.55F + 0.45F * intensity);
        float limb = Math.max(1.0F, f.size * 0.06F);
        marks.add(new Disc(f.cx, f.headY, f.headR, body));
        marks.add(new Stroke(f.cx, f.headY + f.headR, f.cx, f.shoulderY, f.headR * 0.8F, f.headR * 0.8F, body));
        marks.add(new Stroke(f.cx, f.shoulderY + 1.0F, f.cx, f.waistY, f.shoulderHalf * 2.0F, f.size * 0.17F, body));
        marks.add(new Stroke(f.cx - f.shoulderHalf, f.shoulderY + 1.0F, f.cx + f.shoulderHalf, f.shoulderY + 1.0F, limb, limb, body));
        for (int side = -1; side <= 1; side += 2) {
            marks.add(new Stroke(f.cx + side * f.shoulderHalf, f.shoulderY + 1.0F, f.cx + side * (f.shoulderHalf + f.size * 0.04F),
                f.waistY + f.size * 0.06F, limb, limb * 0.8F, body));
            marks.add(new Stroke(f.cx + side * f.size * 0.05F, f.waistY, f.cx + side * f.size * 0.07F, f.footY,
                limb * 1.3F, limb * 0.9F, body));
        }
    }

    /** Rinkaku path card art: four Rinkaku tendrils from the lower back, writhing. {@code intensity} 0..1 (dim → selected). */
    public static List<Mark> rinkaku(PathChoiceLayout.Box box, double time, float intensity, int bg) {
        List<Mark> marks = new ArrayList<>();
        Figure f = Figure.in(box);
        figure(marks, f, bg, intensity);
        float rootX = f.cx;
        float rootY = f.waistY - f.size * 0.06F;
        double speed = 1.2 + 1.4 * intensity;
        // Unit-length curves first, so the longest length that keeps every tendril inside the box can be solved for.
        float[][][] curves = new float[4][][];
        for (int i = 0; i < 4; i++) {
            int side = i < 2 ? -1 : 1;
            double angle = Math.toRadians(i % 2 == 0 ? 22.0 : 58.0);
            double phase = time * speed + i * 1.3;
            curves[i] = tendril(side, angle, phase);
        }
        // Fit against the whole writhe cycle (sampled), so the tendrils keep their length from frame to frame.
        float[][][] envelope = new float[4 * 16][][];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                envelope[i * 16 + j] = tendril(i < 2 ? -1 : 1, Math.toRadians(i % 2 == 0 ? 22.0 : 58.0), j * Math.PI / 8.0);
            }
        }
        float length = fit(box, rootX, rootY, Math.min(box.width() * 0.48F, box.height() * 0.9F), 0.05F, 1.0F, envelope);
        for (int i = 0; i < 4; i++) {
            float[][] points = curves[i];
            float root = Math.max(2.0F, length * 0.10F);
            for (int s = 0; s + 1 < points.length; s++) {
                float a = s / (float)(points.length - 1);
                float b = (s + 1) / (float)(points.length - 1);
                int scale = s % 2 == 0 ? RINKAKU : RINKAKU_DARK; // segmented, scale-like Rinkaku look
                marks.add(new Stroke(rootX + points[s][0] * length, rootY + points[s][1] * length,
                    rootX + points[s + 1][0] * length, rootY + points[s + 1][1] * length,
                    root + (0.8F - root) * a, root + (0.8F - root) * b, blend(bg, scale, 0.45F + 0.55F * intensity)));
            }
        }
        return marks;
    }

    /**
     * 4.1.58: one of Rinkaku's forms for the menu's preview, as it is: {@code tails} tails from the lower back (1, 2 or 4), and with
     * {@code legs} two centipede legs too, thick, segmented, armoured and spiked, rearing higher (the Centipede Form: the legs alone; the
     * Full Kakuja: the legs and four tentacles). The figure seen from behind, the tails writhing.
     */
    public static List<Mark> rinkakuForm(PathChoiceLayout.Box box, double time, float intensity, int tails, boolean legs, int bg) {
        List<Mark> marks = new ArrayList<>();
        Figure f = Figure.in(box, legs ? 0.5F : 0.6F);
        float rootX = f.cx;
        float rootY = f.waistY - f.size * 0.06F;
        double speed = 1.2 + 1.4 * intensity;
        java.util.List<float[][]> curves = new java.util.ArrayList<>();
        java.util.List<double[]> kinds = new java.util.ArrayList<>(); // {side, angle, leg (1) or tail (0)}
        double[][] tailAngles = tails <= 1 ? new double[][] {{1, 40.0}} : tails == 2 ? new double[][] {{-1, 38.0}, {1, 38.0}}
            : new double[][] {{-1, 18.0}, {-1, 50.0}, {1, 18.0}, {1, 50.0}};
        if (legs) {
            kinds.add(new double[] {-1, 70.0, 1});
            kinds.add(new double[] {1, 70.0, 1});
        }
        if (!legs || tails > 2) {
            for (double[] a : tailAngles) {
                kinds.add(new double[] {a[0], a[1], 0});
            }
        }
        float[][][] envelope = new float[kinds.size() * 16][][];
        for (int i = 0; i < kinds.size(); i++) {
            double[] k = kinds.get(i);
            curves.add(tendril((int)k[0], Math.toRadians(k[1]), time * (k[2] > 0 ? speed * 0.5 : speed) + i * 1.3));
            for (int j = 0; j < 16; j++) {
                envelope[i * 16 + j] = tendril((int)k[0], Math.toRadians(k[1]), j * Math.PI / 8.0);
            }
        }
        float length = fit(box, rootX, rootY, Math.min(box.width() * 0.48F, box.height() * 0.9F), 0.05F, 1.0F, envelope);
        float fade = 0.45F + 0.55F * Math.max(0.0F, Math.min(1.0F, intensity));
        for (int i = 0; i < kinds.size(); i++) {
            boolean leg = kinds.get(i)[2] > 0;
            float[][] points = curves.get(i);
            float reach = leg ? length : length * 0.82F;
            float root = Math.max(2.0F, reach * (leg ? 0.15F : 0.10F));
            for (int s = 0; s + 1 < points.length; s++) {
                float a = s / (float)(points.length - 1);
                float b = (s + 1) / (float)(points.length - 1);
                float x0 = rootX + points[s][0] * reach;
                float y0 = rootY + points[s][1] * reach;
                float x1 = rootX + points[s + 1][0] * reach;
                float y1 = rootY + points[s + 1][1] * reach;
                float t0 = root + (0.8F - root) * a;
                float t1 = root + (0.8F - root) * b;
                if (!leg) {
                    marks.add(new Stroke(x0, y0, x1, y1, t0, t1, blend(bg, s % 2 == 0 ? RINKAKU : RINKAKU_DARK, fade)));
                    continue;
                }
                // A centipede leg: dark armour plates, a red seam at each joint, a pale spike off each plate.
                marks.add(new Stroke(x0, y0, x1, y1, t0, t1, blend(bg, KAKUJA_PLATE, fade)));
                marks.add(new Stroke(x0, y0, x1, y1, Math.max(0.6F, t0 * 0.3F), Math.max(0.6F, t1 * 0.3F), blend(bg, RINKAKU, fade * 0.8F)));
                if (s % 2 == 1 && s < points.length - 2) {
                    float dx = x1 - x0;
                    float dy = y1 - y0;
                    float d = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
                    float spike = t1 * 0.9F + 1.0F;
                    marks.add(new Stroke(x1, y1, x1 - dy / d * spike, y1 + dx / d * spike, 1.2F, 0.6F, blend(bg, KAKUJA_NEEDLE, fade)));
                    marks.add(new Stroke(x1, y1, x1 + dy / d * spike, y1 - dx / d * spike, 1.2F, 0.6F, blend(bg, KAKUJA_NEEDLE, fade)));
                }
            }
        }
        figure(marks, f, bg, intensity);
        return marks;
    }

    /** A unit-length S-curve (cubic Bézier, 14 segments) heading outward and up from the root. */
    private static float[][] tendril(int side, double angle, double phase) {
        double dx = side * Math.cos(angle);
        double dy = -Math.sin(angle);
        double px = -dy * side;
        double py = dx * side;
        double[] p1 = {dx * 0.35 + px * 0.18 * Math.sin(phase), dy * 0.35 + py * 0.18 * Math.sin(phase)};
        double[] p2 = {dx * 0.70 - px * 0.20 * Math.sin(phase + 1.0), dy * 0.70 - py * 0.20 * Math.sin(phase + 1.0)};
        double[] p3 = {dx + px * 0.08 * Math.sin(phase + 2.0), dy + py * 0.08 * Math.sin(phase + 2.0)};
        float[][] points = new float[15][];
        for (int s = 0; s <= 14; s++) {
            double t = s / 14.0;
            double u = 1.0 - t;
            double b1 = 3 * u * u * t;
            double b2 = 3 * u * t * t;
            double b3 = t * t * t;
            points[s] = new float[] {(float)(b1 * p1[0] + b2 * p2[0] + b3 * p3[0]), (float)(b1 * p1[1] + b2 * p2[1] + b3 * p3[1])};
        }
        return points;
    }

    /**
     * Ukaku path card art: the same Ukaku wings the game draws ({@link UkakuWing}), seen from the front like the anime:
     * both wings behind the figure, flickering flames while gaseous, crystallized blades when the card is selected.
     */
    public static List<Mark> ukaku(PathChoiceLayout.Box box, double time, float intensity, boolean crystallized, int bg) {
        List<Mark> marks = new ArrayList<>();
        Figure f = Figure.in(box, 0.52F); // Ukaku's wings dwarf him, like the anime frame
        float[] envelope = UkakuWing.envelope(7);
        double rootY = f.shoulderY + f.size * 0.04;
        double rootOffset = f.size * 0.09;
        double right = f.cx + rootOffset;
        double margin = FIT_MARGIN;
        double length = Math.min(box.height() * 1.2, box.width());
        length = Math.min(length, (box.right() - margin - right) / envelope[2]);
        length = Math.min(length, (right - box.x() - margin) / -envelope[0]);
        length = Math.min(length, (rootY - box.y() - margin) / envelope[3]);
        length = Math.min(length, (box.bottom() - margin - rootY) / -envelope[1]);
        length = Math.max(0.0, length);
        float opacity = 0.45F + 0.55F * Math.max(0.0F, Math.min(1.0F, intensity));
        float crystal = crystallized ? 1.0F : 0.0F;
        for (int side = -1; side <= 1; side += 2) {
            double root = f.cx + side * rootOffset;
            List<UkakuWing.Poly> polys = UkakuWing.wing(time + (side > 0 ? 0.0 : 1.7), crystal, 1.0F, 7, 0.6F);
            for (int layer = 0; layer < UkakuWing.LAYERS; layer++) {
                for (UkakuWing.Poly poly : polys) {
                    if (poly.layer() != layer) {
                        continue;
                    }
                    int alpha = Math.round(Math.max(0.0F, Math.min(1.0F, poly.alpha() * opacity)) * 255.0F);
                    if (alpha <= 0) {
                        continue;
                    }
                    float[] x = new float[poly.corners()];
                    float[] y = new float[poly.corners()];
                    for (int i = 0; i < poly.corners(); i++) {
                        x[i] = (float)(root + side * poly.x()[i] * length);
                        y[i] = (float)(rootY - poly.y()[i] * length);
                    }
                    marks.add(new Shape(x, y, alpha << 24 | poly.rgb()));
                }
            }
        }
        figure(marks, f, bg, intensity);
        return marks;
    }

    /**
     * The largest length (up to {@code wanted}) that keeps every unit-curve point, scaled from the root, inside the box,
     * including stroke thickness: every mark is at most {@code unitHalf * length + fixedHalf} thick on each side.
     */
    /** 4.1.56: the kakuja's armour (dark plates), its seams, the final kakuja's needles and its fire. */
    public static final int KAKUJA_PLATE = 0x641C24;
    public static final int KAKUJA_SEAM = 0xC8402A;
    public static final int KAKUJA_NEEDLE = 0xE8D2C8;
    public static final int KAKUJA_FIRE = 0xFF8A2A;
    public static final int KAKUJA_FIRE_CORE = 0xFFD86A;

    /**
     * 4.1.56: Ukaku's kakuja for the menu's preview (his first: "six armored tentacles grow from his shoulder blades"; the final: "three of them
     * as needle tentacles and ... the other three into fire tendrils"): seen from the front, three tentacles from each shoulder blade fanned
     * up and out, writhing, in armour plates with vermilion seams; the final's needle tentacles on his right (the viewer's left) ending in
     * pale needles, its fire tendrils on his left flickering orange to gold.
     */
    public static List<Mark> ukakuKakuja(PathChoiceLayout.Box box, double time, float intensity, boolean finalForm, int bg) {
        List<Mark> marks = new ArrayList<>();
        Figure f = Figure.in(box, 0.52F);
        float rootY = f.shoulderY + f.size * 0.05F;
        double[] angles = {12.0, 42.0, 72.0};
        float[][][] curves = new float[6][][];
        float[][][] envelope = new float[6 * 16][][];
        for (int i = 0; i < 6; i++) {
            int side = i < 3 ? -1 : 1;
            double angle = Math.toRadians(angles[i % 3]);
            curves[i] = tendril(side, angle, time * (finalForm && side > 0 ? 3.2 : 1.4) + i * 1.1);
            for (int j = 0; j < 16; j++) {
                envelope[i * 16 + j] = tendril(side, angle, j * Math.PI / 8.0);
            }
        }
        float rootOffset = f.shoulderHalf * 0.55F;
        float length = Math.min(fit(box, f.cx - rootOffset, rootY, box.width() * 0.5F, 0.05F, 1.0F, java.util.Arrays.copyOfRange(envelope, 0, 48)),
            fit(box, f.cx + rootOffset, rootY, box.width() * 0.5F, 0.05F, 1.0F, java.util.Arrays.copyOfRange(envelope, 48, 96)));
        float fade = 0.45F + 0.55F * Math.max(0.0F, Math.min(1.0F, intensity));
        for (int i = 0; i < 6; i++) {
            boolean left = i < 3;
            float rootX = f.cx + (left ? -rootOffset : rootOffset);
            boolean fire = finalForm && !left;
            boolean needle = finalForm && left;
            float[][] points = curves[i];
            float root = Math.max(2.0F, length * (fire ? 0.08F : 0.11F));
            for (int s = 0; s + 1 < points.length; s++) {
                float a = s / (float)(points.length - 1);
                float b = (s + 1) / (float)(points.length - 1);
                float x0 = rootX + points[s][0] * length;
                float y0 = rootY + points[s][1] * length;
                float x1 = rootX + points[s + 1][0] * length;
                float y1 = rootY + points[s + 1][1] * length;
                float t0 = root + (0.8F - root) * a;
                float t1 = root + (0.8F - root) * b;
                if (fire) {
                    float flicker = 0.5F + 0.5F * (float)Math.sin(time * 9.0 + s * 0.9 + i);
                    marks.add(new Stroke(x0, y0, x1, y1, t0 * (1.0F + 0.25F * flicker), t1 * (1.0F + 0.25F * flicker),
                        blend(bg, blend(KAKUJA_FIRE, KAKUJA_FIRE_CORE, a * 0.8F * flicker), fade)));
                    continue;
                }
                // Armour plates with a seam of vermilion between them (every other segment), a needle's pale point at the end.
                int plate = needle && s >= points.length - 4 ? KAKUJA_NEEDLE : KAKUJA_PLATE;
                marks.add(new Stroke(x0, y0, x1, y1, t0, t1, blend(bg, plate, fade)));
                if (s % 3 == 2 && plate == KAKUJA_PLATE) {
                    // A seam: a thin vermilion band across the plate's end.
                    float dx = x1 - x0;
                    float dy = y1 - y0;
                    float d = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
                    float half = t1 * 0.5F;
                    marks.add(new Stroke(x1 - dy / d * half, y1 + dx / d * half, x1 + dy / d * half, y1 - dx / d * half, 1.2F, 1.2F,
                        blend(bg, KAKUJA_SEAM, fade)));
                }
                if (plate == KAKUJA_PLATE) {
                    // The plate's lit ridge down its middle.
                    marks.add(new Stroke(x0, y0, x1, y1, Math.max(0.6F, t0 * 0.25F), Math.max(0.6F, t1 * 0.25F), blend(bg, KAKUJA_SEAM, fade * 0.7F)));
                }
            }
        }
        figure(marks, f, bg, intensity);
        return marks;
    }

    private static float fit(PathChoiceLayout.Box box, float rootX, float rootY, float wanted, float unitHalf, float fixedHalf,
                             float[][][] curves) {
        float pad = FIT_MARGIN + fixedHalf;
        float[] room = {box.right() - pad - rootX, rootX - box.x() - pad, box.bottom() - pad - rootY, rootY - box.y() - pad};
        float length = Math.max(0.0F, wanted);
        for (float[][] curve : curves) {
            for (float[] point : curve) {
                float[] reach = {Math.max(point[0], 0.0F), Math.max(-point[0], 0.0F), Math.max(point[1], 0.0F), Math.max(-point[1], 0.0F)};
                for (int side = 0; side < 4; side++) {
                    length = Math.min(length, Math.max(0.0F, room[side]) / (reach[side] + unitHalf));
                }
            }
        }
        return Math.max(0.0F, length);
    }

    /** Pale path (6.1.0): pale living flesh, its folds, a protrusion's darker tip, and the faint steam off her skin. */
    public static final int FLESH = 0xEADFD5;
    public static final int FLESH_FOLD = 0xBFA99D;
    public static final int FLESH_DEEP = 0x8F7469;
    public static final int STEAM = 0xDCE0E2;

    /**
     * Pale path card art (6.1.0): her from behind, and out of her upper back a lumpy mass of pale flesh rearing over her right
     * shoulder and curling down into a malformed hand, bulging and folding as it breathes, bony spurs along its back; faint steam
     * rises off her shoulders. Selected, the flesh rears higher and the hand opens.
     */
    public static List<Mark> pale(PathChoiceLayout.Box box, double time, float intensity, boolean selected, int bg) {
        List<Mark> marks = new ArrayList<>();
        Figure f = Figure.in(box, 0.56F);
        figure(marks, f, bg, intensity);
        float rootX = f.cx + f.shoulderHalf * 0.45F;
        float rootY = f.shoulderY + f.size * 0.06F;
        double rear = selected ? 1.0 : 0.65 + 0.1 * Math.sin(time * 0.9);
        float[][] curve = fleshCurve(rear, time);
        // The longest mass that stays inside the box at its fattest, through the whole breath.
        float[][][] envelope = {fleshCurve(1.0, 0.0), fleshCurve(1.0, Math.PI), fleshCurve(0.55, 0.0), curve};
        // Beyond its line the mass reaches at most a swollen radius with its underside fold (~0.25), a spur (~0.28) or a finger
        // (~0.19) in unit terms: 0.3 covers every mark.
        float length = fit(box, rootX, rootY, Math.min(box.width() * 0.46F, box.height() * 0.82F), 0.3F, 1.0F, envelope);
        if (length <= 0.5F) {
            return marks;
        }
        float strength = 0.5F + 0.5F * intensity;
        int n = curve.length;
        for (int i = 0; i < n; i++) {
            float s = i / (float)(n - 1);
            double bulge = 1.0 + 0.22 * Math.sin(s * 19.0 - time * 2.3) + 0.14 * Math.sin(s * 7.0 + time * 1.4);
            float radius = (float)(fleshRadius(s) * bulge) * length;
            float x = rootX + curve[i][0] * length;
            float y = rootY + curve[i][1] * length;
            // A darker fold on the underside first, then the flesh, then a lit ridge on top: lumpy, not a tube.
            marks.add(new Disc(x + radius * 0.18F, y + radius * 0.22F, radius, blend(bg, FLESH_FOLD, strength)));
            boolean fold = Math.sin(s * 23.0 + time * 1.7) > 0.55;
            marks.add(new Disc(x, y, radius * 0.92F, blend(bg, fold ? FLESH_FOLD : FLESH, strength)));
            if (i % 3 == 1) {
                marks.add(new Disc(x - radius * 0.25F, y - radius * 0.3F, radius * 0.38F, blend(bg, FLESH, strength)));
            }
            if (i > 3 && i < n - 4 && i % 5 == 2) {
                // A bony spur off its back.
                double[] normal = normal(curve, i);
                float spur = radius * 1.5F;
                marks.add(new Stroke(x - (float)normal[0] * radius * 0.6F, y - (float)normal[1] * radius * 0.6F,
                    x - (float)normal[0] * spur, y - (float)normal[1] * spur, Math.max(0.8F, radius * 0.35F), 0.6F, blend(bg, FLESH_DEEP, strength)));
            }
        }
        // The malformed hand: three thick, uneven fingers off the end, open when selected.
        float[] tip = curve[n - 1];
        float tipX = rootX + tip[0] * length;
        float tipY = rootY + tip[1] * length;
        float spread = selected ? 0.75F : 0.35F;
        for (int k = -1; k <= 1; k++) {
            double angle = Math.PI * 0.5 + k * spread + 0.15 * Math.sin(time * 1.9 + k);
            float finger = length * (0.13F + 0.03F * k * k);
            float fx = tipX + (float)Math.cos(angle) * finger;
            float fy = tipY + (float)Math.sin(angle) * finger;
            marks.add(new Stroke(tipX, tipY, fx, fy, length * 0.05F, length * 0.022F, blend(bg, k == 0 ? FLESH_FOLD : FLESH, strength)));
        }
        // Steam off her shoulders: faint, rising, spreading, fading.
        for (int k = 0; k < 6; k++) {
            double cycle = (time * 0.35 + k * 0.37) % 1.0;
            float side = k % 2 == 0 ? -1.0F : 1.0F;
            float sx = f.cx + side * f.shoulderHalf * (0.6F + 0.3F * (k % 3)) + (float)Math.sin(time * 1.3 + k * 2.0) * f.size * 0.03F;
            float sy = f.shoulderY - (float)cycle * f.size * 0.32F;
            float sr = Math.max(0.8F, f.size * (0.025F + 0.05F * (float)cycle));
            sx = Math.max(box.x() + FIT_MARGIN + sr, Math.min(box.right() - FIT_MARGIN - sr, sx));
            sy = Math.max(box.y() + FIT_MARGIN + sr, Math.min(box.bottom() - FIT_MARGIN - sr, sy));
            marks.add(new Disc(sx, sy, sr, blend(bg, STEAM, (0.08F + 0.1F * intensity) * (float)(1.0 - cycle))));
        }
        return marks;
    }

    /** The flesh's radius along its length (unit-length terms): thin where it leaves the back, swollen past the shoulder. */
    private static final float FLESH_UNIT_RADIUS = 0.11F;

    private static float fleshRadius(float s) {
        return FLESH_UNIT_RADIUS * (0.35F + 0.65F * (float)Math.pow(Math.sin(Math.PI * Math.min(1.0F, s * 1.15F)), 0.6))
            + FLESH_UNIT_RADIUS * 0.25F * s;
    }

    /** The mass's line (unit length, 24 points): up over her right shoulder, arching out and curling down. */
    private static float[][] fleshCurve(double rear, double time) {
        double sway = 0.06 * Math.sin(time * 1.1);
        double[] p1 = {0.12, -0.55 * rear};
        double[] p2 = {0.72 + sway, -0.78 * rear};
        double[] p3 = {0.92 + sway, -0.12 - 0.25 * (1.0 - rear)};
        float[][] points = new float[24][];
        for (int s = 0; s < 24; s++) {
            double t = s / 23.0;
            double u = 1.0 - t;
            double b1 = 3 * u * u * t;
            double b2 = 3 * u * t * t;
            double b3 = t * t * t;
            points[s] = new float[] {(float)(b1 * p1[0] + b2 * p2[0] + b3 * p3[0]), (float)(b1 * p1[1] + b2 * p2[1] + b3 * p3[1])};
        }
        return points;
    }

    /** The outward side of the curve at point {@code i} (unit). */
    private static double[] normal(float[][] curve, int i) {
        float[] a = curve[Math.max(0, i - 1)];
        float[] b = curve[Math.min(curve.length - 1, i + 1)];
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double length = Math.max(1.0E-6, Math.hypot(dx, dy));
        return new double[] {dy / length, -dx / length};
    }

    /** Graft DNA (7.0.0): black-crimson rinkaku muscle, its dark bands, and the glow of the centipede's seams and mouth. */
    public static final int GRAFT = 0xA8142A;
    public static final int GRAFT_DARK = 0x3A0710;
    public static final int GRAFT_GLOW = 0xFF4A3C;

    /**
     * Graft DNA card art (7.0.0): him from behind, two thick banded tails from the lower back swinging low and wide; selected, a
     * third, a centipede, rears up between them over his head with its toothed mouth open.
     */
    public static List<Mark> graft(PathChoiceLayout.Box box, double time, float intensity, boolean selected, int bg) {
        List<Mark> marks = new ArrayList<>();
        Figure f = Figure.in(box, 0.58F);
        figure(marks, f, bg, intensity);
        float rootX = f.cx;
        float rootY = f.waistY - f.size * 0.04F;
        double speed = 0.9 + 1.1 * intensity;
        float[][][] envelope = new float[3 * 16][][];
        for (int j = 0; j < 16; j++) {
            envelope[j] = tendril(-1, Math.toRadians(14.0), j * Math.PI / 8.0);
            envelope[16 + j] = tendril(1, Math.toRadians(14.0), j * Math.PI / 8.0);
            envelope[32 + j] = rearing(j * Math.PI / 8.0);
        }
        // Beyond its line a tail reaches at most its root's half-width (~0.09) or the open jaws (~0.16) in unit terms.
        float length = fit(box, rootX, rootY, Math.min(box.width() * 0.46F, box.height() * 0.85F), 0.17F, 1.0F, envelope);
        if (length <= 0.5F) {
            return marks;
        }
        float strength = 0.45F + 0.55F * intensity;
        for (int side = -1; side <= 1; side += 2) {
            float[][] curve = tendril(side, Math.toRadians(14.0), time * speed + (side + 1) * 1.6);
            band(marks, curve, rootX, rootY, length, length * 0.17F, length * 0.05F, bg, strength, false);
        }
        if (selected) {
            float[][] curve = rearing(time * speed * 0.8);
            band(marks, curve, rootX, rootY, length, length * 0.14F, length * 0.07F, bg, strength, true);
            // The open mouth: two hooked jaws off the head, teeth between them.
            float[] head = curve[curve.length - 1];
            double[] normal = normal(curve, curve.length - 1);
            float[] before = curve[curve.length - 2];
            double dx = head[0] - before[0];
            double dy = head[1] - before[1];
            double unit = Math.max(1.0E-6, Math.hypot(dx, dy));
            float hx = rootX + head[0] * length;
            float hy = rootY + head[1] * length;
            float open = (float)(0.55 + 0.25 * Math.sin(time * 2.4));
            for (int k = -1; k <= 1; k += 2) {
                float jx = hx + (float)(dx / unit * 0.12 + normal[0] * k * 0.07 * open) * length;
                float jy = hy + (float)(dy / unit * 0.12 + normal[1] * k * 0.07 * open) * length;
                marks.add(new Stroke(hx, hy, jx, jy, length * 0.045F, length * 0.012F, blend(bg, GRAFT_DARK, strength)));
            }
            marks.add(new Disc(hx + (float)(dx / unit) * length * 0.03F, hy + (float)(dy / unit) * length * 0.03F, length * 0.03F * open,
                blend(bg, GRAFT_GLOW, strength)));
        }
        return marks;
    }

    /** A tail drawn along {@code curve}: tapered, banded dark and crimson; a centipede's bands carry a glowing seam. */
    private static void band(List<Mark> marks, float[][] curve, float rootX, float rootY, float length, float root, float tip, int bg, float strength,
                             boolean seams) {
        int n = curve.length;
        for (int s = 0; s + 1 < n; s++) {
            float a = s / (float)(n - 1);
            float b = (s + 1) / (float)(n - 1);
            float x0 = rootX + curve[s][0] * length;
            float y0 = rootY + curve[s][1] * length;
            float x1 = rootX + curve[s + 1][0] * length;
            float y1 = rootY + curve[s + 1][1] * length;
            int colour = s % 2 == 0 ? GRAFT : GRAFT_DARK;
            marks.add(new Stroke(x0, y0, x1, y1, root + (tip - root) * a, root + (tip - root) * b, blend(bg, colour, strength)));
            if (seams && s % 2 == 1) {
                marks.add(new Disc(x1, y1, Math.max(0.6F, (root + (tip - root) * b) * 0.22F), blend(bg, GRAFT_GLOW, strength * 0.9F)));
            }
        }
    }

    /** The centipede's line (unit length, 15 points): straight up out of the lower back, over the head, the head dipping forward. */
    private static float[][] rearing(double phase) {
        double sway = 0.12 * Math.sin(phase);
        double[] p1 = {0.10 + sway, -0.45};
        double[] p2 = {-0.18 - sway, -0.80};
        double[] p3 = {0.22 + sway * 0.5, -0.92};
        float[][] points = new float[15][];
        for (int s = 0; s <= 14; s++) {
            double t = s / 14.0;
            double u = 1.0 - t;
            double b1 = 3 * u * u * t;
            double b2 = 3 * u * t * t;
            double b3 = t * t * t;
            points[s] = new float[] {(float)(b1 * p1[0] + b2 * p2[0] + b3 * p3[0]), (float)(b1 * p1[1] + b2 * p2[1] + b3 * p3[1])};
        }
        return points;
    }

    /**
     * 4.1.60: one of Graft DNA's stages for the menu's preview, as the game draws it: every limb's resting line is the game's own
     * ({@link com.hex.kagune.control.graft.GraftRig#pose}, breathing as it does in the world), seen from behind him and drawn to scale
     * with him, so the thorns dwarf him and the many-eyed mass towers to his right; each limb in its stage's look (the
     * {@link com.hex.kagune.control.graft.GraftModel} limbs): Rize's muscular tails with a vein of light; armoured centipedes, spiked
     * down both flanks, the first with its mouth; thorned giants ringed in black ridges; crescents of black flesh full of white eyes that
     * blink; the kakuja's colossi with their heads and mandibles, and him in its black hide, his crown of spikes and the two burning
     * sockets in his back. Limbs reaching forward round him pass behind him.
     */
    public static List<Mark> graftForm(PathChoiceLayout.Box box, double time, float intensity, com.hex.kagune.control.graft.GraftStage stage, int mastery,
                                       int bg) {
        return graftForm(box, time, intensity, stage, mastery, bg, GRAFT_YAW);
    }

    /** {@link #graftForm} seen from {@code yaw} round from straight behind him (toward his left; for previews of the angle). */
    public static List<Mark> graftForm(PathChoiceLayout.Box box, double time, float intensity, com.hex.kagune.control.graft.GraftStage stage, int mastery,
                                       int bg, double yaw) {
        List<Mark> marks = new ArrayList<>();
        int n = stage.limbs(mastery);
        double length = stage.length(mastery);
        double half = stage.width(mastery);
        double ticks = time * 20.0;
        // The scale: every limb at rest over a stretch of its breathing, with its girth, and him; so the scene keeps its size.
        double minX = -0.5;
        double maxX = 0.5;
        double minY = 0.0;
        double maxY = 2.1;
        for (int sample = 0; sample < 6; sample++) {
            for (int limb = 0; limb < n; limb++) {
                for (double[] p : graftLimb(stage, n, limb, length, sample * 37.0)) {
                    double sx = graftX(p, yaw);
                    minX = Math.min(minX, sx - half * 1.6);
                    maxX = Math.max(maxX, sx + half * 1.6);
                    minY = Math.min(minY, p[1] - half * 1.6);
                    maxY = Math.max(maxY, p[1] + half * 1.6);
                }
            }
        }
        float room = FIT_MARGIN + 2.0F;
        double scale = Math.min((box.width() - 2.0 * room) / (maxX - minX), (box.height() - 2.0 * room) / (maxY - minY)) * 0.94;
        if (scale <= 0.5) {
            return marks;
        }
        float originX = (float)(box.x() + box.width() / 2.0 - (minX + maxX) / 2.0 * scale);
        float originY = (float)(box.y() + box.height() / 2.0 + (minY + maxY) / 2.0 * scale);
        float fade = 0.45F + 0.55F * Math.max(0.0F, Math.min(1.0F, intensity));
        // Each limb as runs of segments behind him (reaching forward round him) or in front of him (toward the viewer).
        List<float[]> order = new ArrayList<>(); // {depth, run index}
        List<List<Mark>> runs = new ArrayList<>();
        for (int limb = 0; limb < n; limb++) {
            double[][] spine = smoothLine(graftLimb(stage, n, limb, length, ticks), 6);
            int count = spine.length;
            float[] x = new float[count];
            float[] y = new float[count];
            float[] w = new float[count];
            float[] s = new float[count];
            double total = 0.0;
            double[] along = new double[count];
            for (int i = 1; i < count; i++) {
                total += Math.hypot(Math.hypot(spine[i][0] - spine[i - 1][0], spine[i][1] - spine[i - 1][1]), spine[i][2] - spine[i - 1][2]);
                along[i] = total;
            }
            for (int i = 0; i < count; i++) {
                s[i] = (float)(along[i] / Math.max(1.0E-6, total));
                x[i] = originX + (float)(graftX(spine[i], yaw) * scale);
                y[i] = originY - (float)(spine[i][1] * scale);
                w[i] = (float)(half * graftWidth(stage, s[i]) * scale);
            }
            int start = 0;
            while (start < count - 1) {
                boolean behind = graftDepth(spine[start], spine[start + 1], yaw) > 0.2;
                int end = start + 1;
                while (end < count - 1 && (graftDepth(spine[end], spine[end + 1], yaw) > 0.2) == behind) {
                    end++;
                }
                List<Mark> run = new ArrayList<>();
                graftRun(run, stage, limb, x, y, w, s, start, end, time, bg, fade);
                double depth = 0.0;
                for (int i = start; i <= end; i++) {
                    depth += graftDepth(spine[i], spine[i], yaw);
                }
                order.add(new float[] {(float)(depth / (end - start + 1)) + (behind ? 10.0F : 0.0F), runs.size()});
                runs.add(run);
                start = end;
            }
        }
        // His body, to scale: feet on the ground line, two blocks tall.
        float size = (float)(2.0 * scale);
        float footY = originY;
        float top = footY - size;
        float headR = Math.max(1.5F, size * 0.075F);
        Figure f = new Figure(originX, top + headR + 1.0F, headR, top + size * 0.22F, size * 0.13F, top + size * 0.56F, footY, size);
        List<Mark> body = new ArrayList<>();
        if (stage.kakuja()) {
            kakujaHide(body, f, time, bg, fade);
        } else {
            figure(body, f, bg, intensity);
        }
        order.add(new float[] {5.0F, runs.size()});
        runs.add(body);
        order.sort((a, b) -> Float.compare(b[0], a[0]));
        for (float[] o : order) {
            marks.addAll(runs.get((int)o[1]));
        }
        return marks;
    }

    /** A limb's resting line this frame (rig space, root first), from the game's rig. */
    private static double[][] graftLimb(com.hex.kagune.control.graft.GraftStage stage, int limbs, int limb, double length, double ticks) {
        com.hex.kagune.control.graft.GraftRig.Body still = new com.hex.kagune.control.graft.GraftRig.Body(0.0, false, 0.0, true, 0.0, 0.0, ticks);
        return com.hex.kagune.control.graft.GraftRig.pose(new com.hex.kagune.control.graft.GraftRig.Input(stage, limbs, limb, length, still,
            com.hex.kagune.control.graft.GraftMove.NONE, 0.0, null, null, null, 0.3)).points();
    }

    /** Seen from behind him and a little to his left: rig x (his right) across, a touch of depth so what curls forward shows. */
    private static final double GRAFT_YAW = 0.7;

    private static double graftX(double[] p, double yaw) {
        return p[0] * Math.cos(yaw) + p[2] * Math.sin(yaw);
    }

    /** How far in front of him (away from the viewer) the middle of a segment is. */
    private static double graftDepth(double[] a, double[] b, double yaw) {
        double x = (a[0] + b[0]) / 2.0;
        double z = (a[2] + b[2]) / 2.0;
        return z * Math.cos(yaw) - x * Math.sin(yaw);
    }

    /** A limb's half-width along it (share of the thickest), as the game's model shapes it. */
    private static double graftWidth(com.hex.kagune.control.graft.GraftStage stage, double s) {
        return switch (stage) {
            case TRANSPLANT -> com.hex.kagune.control.graft.GraftModel.rinkakuWidth(s);
            case CENTIPEDE -> com.hex.kagune.control.graft.GraftModel.centipedeWidth(s) * (s > 0.9 ? Math.max(0.15, (1.0 - s) / 0.1) : 1.0);
            case THORNS -> com.hex.kagune.control.graft.GraftModel.thornWidth(s) * (s > 0.82 ? Math.max(0.05, (1.0 - s) / 0.18) : 1.0);
            case DRAGON -> com.hex.kagune.control.graft.GraftModel.crescentWidth(s);
            case KAKUJA -> com.hex.kagune.control.graft.GraftModel.colossusWidth(s) * (s > 0.97 ? 0.7 : 1.0);
        };
    }

    /** A Catmull-Rom line through {@code points}, {@code per} samples between each two. */
    private static double[][] smoothLine(double[][] points, int per) {
        int n = points.length;
        if (n < 2) {
            return points;
        }
        double[][] out = new double[(n - 1) * per + 1][];
        int k = 0;
        for (int i = 0; i + 1 < n; i++) {
            double[] p0 = points[Math.max(0, i - 1)];
            double[] p1 = points[i];
            double[] p2 = points[i + 1];
            double[] p3 = points[Math.min(n - 1, i + 2)];
            for (int j = 0; j < per; j++) {
                double t = j / (double)per;
                double t2 = t * t;
                double t3 = t2 * t;
                double[] p = new double[3];
                for (int d = 0; d < 3; d++) {
                    p[d] = 0.5 * (2.0 * p1[d] + (-p0[d] + p2[d]) * t + (2.0 * p0[d] - 5.0 * p1[d] + 4.0 * p2[d] - p3[d]) * t2
                        + (-p0[d] + 3.0 * p1[d] - 3.0 * p2[d] + p3[d]) * t3);
                }
                out[k++] = p;
            }
        }
        out[k] = points[n - 1].clone();
        return out;
    }

    // Graft DNA's colours for the 2D preview, the game model's (GraftModel), lifted a little so they read against the tile's black.
    private static final int G_FLESH_DEEP = 0x30040A;
    private static final int G_FLESH = 0x6C0C16;
    private static final int G_FLESH_HI = 0x98202A;
    private static final int G_FLESH_SHEEN = 0xD04A50;
    private static final int G_FLESH_EDGE = 0x1A0206;
    private static final int G_VEIN = 0xFF3A30;
    private static final int G_SHELL_DARK = 0x120608;
    private static final int G_SHELL = 0x2A1016;
    private static final int G_SHELL_HI = 0x4E1C24;
    private static final int G_SHELL_SHEEN = 0x8A3440;
    private static final int G_SHELL_EDGE = 0x5A1820;
    private static final int G_SPIKE = 0x9A3038;
    private static final int G_SPIKE_TIP = 0x3A0E14;
    private static final int G_MASS = 0x140508;
    private static final int G_MASS_HI = 0x2E0A10;
    private static final int G_CREASE = 0x6A0C18;
    private static final int G_EYE = 0xF3EEE2;
    private static final int G_IRIS = 0xB0101C;
    private static final int G_TOOTH = 0xE8DECC;
    /** The light falls from the upper left of the tile. */
    private static final float LIGHT_X = -0.6F;
    private static final float LIGHT_Y = -0.8F;

    /**
     * Segments {@code from}..{@code to} of one limb, in its stage's look: drawn as a lit tube (an outline round the whole run, the
     * dark of its far side, its body, and a sheen on the side the light falls on), then what its stage carries.
     */
    private static void graftRun(List<Mark> marks, com.hex.kagune.control.graft.GraftStage stage, int limb, float[] x, float[] y, float[] w, float[] s,
                                 int from, int to, double time, int bg, float fade) {
        boolean maw = com.hex.kagune.control.graft.GraftRig.maw(stage, limb);
        int last = x.length - 1;
        boolean colossus = stage.kakuja();
        int edge;
        int dark;
        int body;
        int sheen;
        switch (stage) {
            case TRANSPLANT -> {
                edge = G_FLESH_EDGE;
                dark = G_FLESH_DEEP;
                body = G_FLESH;
                sheen = G_FLESH_SHEEN;
            }
            case THORNS -> {
                edge = G_FLESH_EDGE;
                dark = G_FLESH_DEEP;
                body = G_FLESH_HI;
                sheen = G_FLESH_SHEEN;
            }
            case DRAGON -> {
                edge = G_CREASE;
                dark = G_MASS;
                body = G_MASS_HI;
                sheen = 0x4A0E16;
            }
            default -> {
                edge = G_SHELL_EDGE;
                dark = G_SHELL_DARK;
                body = G_SHELL;
                sheen = G_SHELL_SHEEN;
            }
        }
        // The tube as chains of discs, round everywhere: a thick stroke is drawn in squares, whose corners show where a limb comes at the
        // viewer or a run ends.
        for (int i = from; i < to; i++) {
            tube(marks, x[i], y[i], x[i + 1], y[i + 1], w[i] + 1.0F, w[i + 1] + 1.0F, blend(bg, edge, fade), i + 1 == to);
        }
        for (int i = from; i < to; i++) {
            tube(marks, x[i], y[i], x[i + 1], y[i + 1], w[i], w[i + 1], blend(bg, dark, fade), i + 1 == to);
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int i = from; i < to; i++) {
                float dx = x[i + 1] - x[i];
                float dy = y[i + 1] - y[i];
                float d = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
                float nx = -dy / d;
                float ny = dx / d;
                float lit = nx * LIGHT_X + ny * LIGHT_Y; // how far the light side lies along the normal
                float shift = pass == 0 ? 0.22F : 0.5F;
                float share = pass == 0 ? 0.68F : 0.22F;
                float ox0 = nx * lit * w[i] * shift;
                float oy0 = ny * lit * w[i] * shift;
                float ox1 = nx * lit * w[i + 1] * shift;
                float oy1 = ny * lit * w[i + 1] * shift;
                int colour = pass == 0 ? body : sheen;
                if (stage == com.hex.kagune.control.graft.GraftStage.TRANSPLANT && pass == 0) {
                    // Bundles of muscle swelling along it.
                    colour = blend(G_FLESH, G_FLESH_HI, (float)(0.5 + 0.5 * Math.sin(s[i] * 30.0 + limb)) * 0.7F);
                }
                if (stage == com.hex.kagune.control.graft.GraftStage.THORNS && s[i] > com.hex.kagune.control.graft.GraftModel.POINT) {
                    colour = pass == 0 ? G_SHELL : G_SHELL_SHEEN; // the point: black, honed
                }
                if (stage == com.hex.kagune.control.graft.GraftStage.TRANSPLANT && s[i] > 0.78F) {
                    colour = blend(colour, G_FLESH_EDGE, Math.min(1.0F, (s[i] - 0.78F) / 0.2F) * 0.75F); // the point darkens, honed
                }
                float min = pass == 0 ? 0.5F : 0.3F;
                tube(marks, x[i] + ox0, y[i] + oy0, x[i + 1] + ox1, y[i + 1] + oy1, Math.max(min, w[i] * share), Math.max(min, w[i + 1] * share),
                    blend(bg, colour, fade), i + 1 == to);
            }
        }
        for (int i = from; i < to; i++) {
            float dx = x[i + 1] - x[i];
            float dy = y[i + 1] - y[i];
            float d = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
            float nx = -dy / d;
            float ny = dx / d;
            float hw = w[i + 1];
            switch (stage) {
                case CENTIPEDE, KAKUJA -> {
                    boolean head = colossus && s[i] > com.hex.kagune.control.graft.GraftModel.COLOSSUS_HEAD;
                    if (i % 3 != 2 || head || s[i] < 0.08F || s[i] > 0.93F) {
                        break;
                    }
                    // Where two plates overlap: a dark line across, red light in the seam.
                    float glow = 0.7F + 0.3F * (float)Math.sin(time * 3.0 + i * 0.7);
                    marks.add(new Stroke(x[i + 1] - nx * hw, y[i + 1] - ny * hw, x[i + 1] + nx * hw, y[i + 1] + ny * hw, 1.4F, 1.4F,
                        blend(bg, G_SHELL_DARK, fade)));
                    marks.add(new Stroke(x[i + 1] - nx * hw * 0.8F + dx / d, y[i + 1] - ny * hw * 0.8F + dy / d, x[i + 1] + nx * hw * 0.8F + dx / d,
                        y[i + 1] + ny * hw * 0.8F + dy / d, 0.8F, 0.8F, blend(bg, GRAFT_GLOW, fade * glow * 0.8F)));
                    // A spike off each flank, raked back toward the root (a colossus's twice as long).
                    float spike = hw * (colossus ? 0.95F : 0.7F) + 2.0F;
                    for (int side = -1; side <= 1; side += 2) {
                        float bx = x[i + 1] + side * nx * hw;
                        float by = y[i + 1] + side * ny * hw;
                        marks.add(new Stroke(bx, by, bx + side * nx * spike - dx / d * spike * 0.45F, by + side * ny * spike - dy / d * spike * 0.45F,
                            Math.max(1.0F, hw * 0.28F), 0.6F, blend(bg, G_SPIKE, fade)));
                    }
                }
                case THORNS -> {
                    if (i % 4 != 1 || s[i] < 0.06F || s[i] > com.hex.kagune.control.graft.GraftModel.POINT) {
                        break;
                    }
                    // A black armoured ridge ringing the core, its rim lit, a long thorn off each side curving toward the point.
                    float ring = hw * 1.16F;
                    marks.add(new Stroke(x[i + 1] - nx * ring, y[i + 1] - ny * ring, x[i + 1] + nx * ring, y[i + 1] + ny * ring, 2.6F, 2.6F,
                        blend(bg, G_SHELL_DARK, fade)));
                    marks.add(new Stroke(x[i + 1] - nx * ring - dx / d, y[i + 1] - ny * ring - dy / d, x[i + 1] + nx * ring - dx / d,
                        y[i + 1] + ny * ring - dy / d, 0.8F, 0.8F, blend(bg, G_SHELL_EDGE, fade)));
                    // A thorn off every other ridge, sides taking turns: few, long and hooked, never a centipede's legs.
                    if ((i / 4) % 2 == 1) {
                        break;
                    }
                    float thorn = hw * 1.7F + 5.0F;
                    for (int side = -1; side <= 1; side += 2) {
                        if (side != ((i / 8) % 2 == 0 ? 1 : -1)) {
                            continue;
                        }
                        float lean = 1.0F;
                        float bx = x[i + 1] + side * nx * ring;
                        float by = y[i + 1] + side * ny * ring;
                        float mx = bx + side * nx * thorn * 0.62F * lean + dx / d * thorn * 0.3F;
                        float my = by + side * ny * thorn * 0.62F * lean + dy / d * thorn * 0.3F;
                        float tx = mx + dx / d * thorn * 0.5F + side * nx * thorn * 0.12F;
                        float ty = my + dy / d * thorn * 0.5F + side * ny * thorn * 0.12F;
                        marks.add(new Stroke(bx, by, mx, my, Math.max(2.0F, hw * 0.6F), Math.max(1.2F, hw * 0.3F), blend(bg, G_SHELL_EDGE, fade)));
                        marks.add(new Stroke(mx, my, tx, ty, Math.max(1.2F, hw * 0.3F), 0.6F, blend(bg, G_SHELL_SHEEN, fade)));
                    }
                }
                case DRAGON -> {
                    // Its crease, crimson along the top of the crescent.
                    float up = ny < 0 ? 1.0F : -1.0F;
                    marks.add(new Stroke(x[i] + up * nx * w[i] * 0.72F, y[i] + up * ny * w[i] * 0.72F, x[i + 1] + up * nx * hw * 0.72F,
                        y[i + 1] + up * ny * hw * 0.72F, 1.0F, 1.0F, blend(bg, G_CREASE, fade)));
                }
                case TRANSPLANT -> {
                    if (s[i] < 0.08F || s[i] > 0.76F) {
                        break;
                    }
                    // The striations between its bundles of muscle, running along it.
                    for (float k : new float[] {-0.52F, 0.4F}) {
                        marks.add(new Stroke(x[i] + nx * w[i] * k, y[i] + ny * w[i] * k, x[i + 1] + nx * hw * k, y[i + 1] + ny * hw * k, 1.0F, 1.0F,
                            blend(bg, blend(G_FLESH, G_FLESH_DEEP, 0.75F), fade)));
                    }
                }
            }
            if (stage == com.hex.kagune.control.graft.GraftStage.THORNS && s[i] > com.hex.kagune.control.graft.GraftModel.POINT && i % 4 == 0
                && s[i] < 0.96F) {
                // The point's serrations, a few teeth down each edge.
                for (int side = -1; side <= 1; side += 2) {
                    float bx = x[i] + side * nx * w[i];
                    float by = y[i] + side * ny * w[i];
                    marks.add(new Stroke(bx, by, bx + side * nx * 2.0F - dx / d * 1.5F, by + side * ny * 2.0F - dy / d * 1.5F, 1.2F, 0.6F,
                        blend(bg, G_SHELL_EDGE, fade)));
                }
            }
        }
        // The veins of light along the transplant's tails.
        if (stage == com.hex.kagune.control.graft.GraftStage.TRANSPLANT) {
            float pulse = 0.6F + 0.4F * (float)Math.sin(time * 2.6 + limb);
            for (int i = from; i < to; i++) {
                if (s[i] < 0.06 || s[i] > 0.8) {
                    continue;
                }
                float off = 0.3F * (float)Math.sin(s[i] * 9.0 + limb);
                float dx = x[i + 1] - x[i];
                float dy = y[i + 1] - y[i];
                float d = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
                marks.add(new Stroke(x[i] - dy / d * w[i] * off, y[i] + dx / d * w[i] * off, x[i + 1] - dy / d * w[i + 1] * off, y[i + 1] + dx / d * w[i + 1] * off,
                    1.0F, 1.0F, blend(bg, G_VEIN, fade * pulse)));
            }
        }
        // The many eyes, along every crescent, blinking each in its own time.
        if (stage == com.hex.kagune.control.graft.GraftStage.DRAGON) {
            float[] at = {0.3F, 0.52F, 0.72F};
            for (int e = 0; e < at.length; e++) {
                int i = index(s, at[e]);
                if (i < from || i >= to || w[i] < 2.5F) {
                    continue;
                }
                double seed = Math.sin((limb * 7 + e * 3 + 1) * 12.9898) * 43758.5453;
                seed -= Math.floor(seed);
                double cycle = (time + seed * 6.0) % (3.5 + seed * 3.0);
                float open = cycle < 0.18 ? (float)Math.abs(cycle - 0.09) / 0.09F : 1.0F;
                almond(marks, x, y, w, i, open, time, seed, bg, fade);
            }
        }
        // The heads: the centipede's mouth and the colossi's.
        if (to == last && maw) {
            mouth(marks, x, y, w, last, stage.kakuja(), time, bg, fade);
        } else if (to == last && stage == com.hex.kagune.control.graft.GraftStage.CENTIPEDE) {
            // The barbed stinger.
            float dx = x[last] - x[last - 2];
            float dy = y[last] - y[last - 2];
            float d = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
            for (int side = -1; side <= 1; side += 2) {
                marks.add(new Stroke(x[last], y[last], x[last] - dx / d * 4.0F - dy / d * side * 3.0F, y[last] - dy / d * 4.0F + dx / d * side * 3.0F, 1.4F, 0.6F,
                    blend(bg, G_SPIKE, fade)));
            }
        }
    }

    /** One length of a tube as discs close enough to read as one round shape (radius {@code r0} at its start, {@code r1} at its end). */
    private static void tube(List<Mark> marks, float x0, float y0, float x1, float y1, float r0, float r1, int rgb, boolean end) {
        if (Math.max(r0, r1) < 1.0F) {
            marks.add(new Stroke(x0, y0, x1, y1, r0 * 2.0F, r1 * 2.0F, rgb));
            return;
        }
        float length = (float)Math.hypot(x1 - x0, y1 - y0);
        int steps = Math.max(1, (int)Math.ceil(length / Math.max(0.75F, Math.min(r0, r1) * 0.5F)));
        for (int k = 0; k < (end ? steps + 1 : steps); k++) {
            float t = k / (float)steps;
            marks.add(new Disc(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, Math.max(0.6F, r0 + (r1 - r0) * t), rgb));
        }
    }

    /** The sample nearest {@code s} along a limb. */
    private static int index(float[] s, float at) {
        int best = 0;
        for (int i = 0; i < s.length; i++) {
            if (Math.abs(s[i] - at) < Math.abs(s[best] - at)) {
                best = i;
            }
        }
        return Math.min(best, s.length - 2);
    }

    /** A great almond eye on a crescent: a black rim, the white ball, the pupil ringed in red; shut, a black line. */
    private static void almond(List<Mark> marks, float[] x, float[] y, float[] w, int i, float open, double time, double seed, int bg, float fade) {
        float dx = x[i + 1] - x[i];
        float dy = y[i + 1] - y[i];
        float d = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
        float ux = dx / d;
        float uy = dy / d;
        float cx = x[i];
        float cy = y[i];
        float halfLong = w[i] * 1.1F;
        float tall = Math.max(1.0F, w[i] * 0.9F * open);
        marks.add(new Stroke(cx, cy, cx - ux * (halfLong + 1.5F), cy - uy * (halfLong + 1.5F), tall + 2.5F, 1.0F, blend(bg, 0x050203, fade)));
        marks.add(new Stroke(cx, cy, cx + ux * (halfLong + 1.5F), cy + uy * (halfLong + 1.5F), tall + 2.5F, 1.0F, blend(bg, 0x050203, fade)));
        if (open < 0.3F) {
            return;
        }
        marks.add(new Stroke(cx, cy, cx - ux * halfLong, cy - uy * halfLong, tall, 0.6F, blend(bg, G_EYE, fade)));
        marks.add(new Stroke(cx, cy, cx + ux * halfLong, cy + uy * halfLong, tall, 0.6F, blend(bg, G_EYE, fade)));
        // The pupil rolls along the eye, looking about.
        float look = (float)Math.sin(time * 0.7 + seed * 10.0) * halfLong * 0.35F;
        float r = Math.max(1.0F, tall * 0.42F);
        marks.add(new Disc(cx + ux * look, cy + uy * look, r, blend(bg, G_IRIS, fade)));
        marks.add(new Disc(cx + ux * look, cy + uy * look, Math.max(0.6F, r * 0.55F), blend(bg, 0x050203, fade)));
    }

    /** A mouth at a limb's end: two hooked jaws opening and closing, pale teeth, the red of its throat. */
    private static void mouth(List<Mark> marks, float[] x, float[] y, float[] w, int last, boolean colossus, double time, int bg, float fade) {
        float dx = x[last] - x[last - 3];
        float dy = y[last] - y[last - 3];
        float d = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
        float ux = dx / d;
        float uy = dy / d;
        float hw = Math.max(2.0F, w[last - 3]);
        float open = (float)(0.55 + 0.35 * Math.sin(time * 2.2));
        float hx = x[last];
        float hy = y[last];
        marks.add(new Disc(hx, hy, hw * 0.55F * open + 0.8F, blend(bg, GRAFT_GLOW, fade)));
        for (int side = -1; side <= 1; side += 2) {
            float bx = hx - uy * side * hw * 0.7F;
            float by = hy + ux * side * hw * 0.7F;
            float reach = hw * (colossus ? 1.5F : 1.1F);
            float mx = bx + ux * reach - uy * side * reach * 0.35F * open;
            float my = by + uy * reach + ux * side * reach * 0.35F * open;
            marks.add(new Stroke(bx, by, mx, my, Math.max(1.4F, hw * 0.45F), Math.max(1.0F, hw * 0.25F), blend(bg, G_SHELL_EDGE, fade)));
            marks.add(new Stroke(mx, my, mx + uy * side * reach * 0.45F + ux * reach * 0.15F, my - ux * side * reach * 0.45F + uy * reach * 0.15F,
                Math.max(1.0F, hw * 0.25F), 0.6F, blend(bg, G_SPIKE, fade)));
            marks.add(new Disc(bx + ux * reach * 0.35F, by + uy * reach * 0.35F, Math.max(0.6F, hw * 0.14F), blend(bg, G_TOOTH, fade)));
        }
        if (colossus) {
            // Its oversized eyes, glowing, behind the jaws.
            for (int side = -1; side <= 1; side += 2) {
                float ex = hx - ux * hw * 1.2F - uy * side * hw * 0.55F;
                float ey = hy - uy * hw * 1.2F + ux * side * hw * 0.55F;
                marks.add(new Disc(ex, ey, Math.max(1.0F, hw * 0.28F), blend(bg, 0xFF3B2A, fade)));
            }
        }
    }

    /**
     * Him in the kakuja's black hide, from behind: red whorls over it, a wild crown of black spikes off his head, the two sockets in
     * his lower back the colossi grow out of, burning.
     */
    private static void kakujaHide(List<Mark> marks, Figure f, double time, int bg, float fade) {
        int hide = blend(bg, 0x1E0C12, fade);
        int whorl = blend(bg, 0x6E0C16, fade);
        float limb = Math.max(1.0F, f.size * 0.07F);
        marks.add(new Disc(f.cx, f.headY, f.headR * 1.1F, hide));
        marks.add(new Stroke(f.cx, f.headY + f.headR, f.cx, f.shoulderY, f.headR * 0.9F, f.headR * 0.9F, hide));
        marks.add(new Stroke(f.cx, f.shoulderY + 1.0F, f.cx, f.waistY, f.shoulderHalf * 2.2F, f.size * 0.19F, hide));
        marks.add(new Stroke(f.cx - f.shoulderHalf, f.shoulderY + 1.0F, f.cx + f.shoulderHalf, f.shoulderY + 1.0F, limb, limb, hide));
        for (int side = -1; side <= 1; side += 2) {
            marks.add(new Stroke(f.cx + side * f.shoulderHalf, f.shoulderY + 1.0F, f.cx + side * (f.shoulderHalf + f.size * 0.05F),
                f.waistY + f.size * 0.06F, limb, limb * 0.9F, hide));
            // Claws over the hands.
            float hx = f.cx + side * (f.shoulderHalf + f.size * 0.05F);
            float hy = f.waistY + f.size * 0.06F;
            marks.add(new Stroke(hx, hy, hx + side * 1.0F, hy + f.size * 0.06F, 1.2F, 0.6F, blend(bg, G_SPIKE, fade)));
            marks.add(new Stroke(f.cx + side * f.size * 0.05F, f.waistY, f.cx + side * f.size * 0.07F, f.footY, limb * 1.4F, limb, hide));
            // A whorl on each shoulder blade.
            marks.add(new Stroke(f.cx + side * f.shoulderHalf * 0.2F, f.shoulderY + f.size * 0.08F, f.cx + side * f.shoulderHalf * 0.75F,
                f.shoulderY + f.size * 0.14F, 1.0F, 1.0F, whorl));
            // The sockets the colossi grow out of.
            float glow = 0.75F + 0.25F * (float)Math.sin(time * 4.0 + side);
            marks.add(new Disc(f.cx + side * f.size * 0.045F, f.waistY - f.size * 0.03F, Math.max(1.0F, f.size * 0.035F), blend(bg, GRAFT_GLOW, fade * glow)));
        }
        // The crown of black spikes sweeping back off his head.
        for (int k = -2; k <= 2; k++) {
            float bx = f.cx + k * f.headR * 0.4F;
            float by = f.headY - f.headR * 0.5F;
            marks.add(new Stroke(bx, by, bx + k * f.headR * 0.5F, by - f.headR * (1.3F - Math.abs(k) * 0.25F), Math.max(1.0F, f.headR * 0.45F), 0.6F,
                blend(bg, k % 2 == 0 ? 0x2A0E14 : G_SHELL_EDGE, fade)));
        }
    }

    /** 3.4.9, Bikaku: the worm's crimson-purple flesh, its dark folds, its red veins and its grey fangs. */
    public static final int BIKAKU = 0x8A1A3E;
    public static final int BIKAKU_DARK = 0x3C0716;
    public static final int BIKAKU_VEIN = 0xFF3040;
    public static final int BIKAKU_FANG = 0xC8C2B8;

    /**
     * Bikaku card art (3.4.9): him from behind, and out of his lower back one thick worm climbing in zigzags high over his head and
     * round over his right shoulder, its round mouth of fangs at the end, a wave running up its coils. Selected, the mouth gapes.
     */
    public static List<Mark> bikaku(PathChoiceLayout.Box box, double time, float intensity, boolean selected, int bg) {
        List<Mark> marks = new ArrayList<>();
        Figure f = Figure.in(box, 0.5F);
        float rootX = f.cx;
        float rootY = f.waistY - f.size * 0.04F;
        double speed = 0.8 + 1.0 * intensity;
        float[][][] envelope = new float[16][][];
        for (int j = 0; j < 16; j++) {
            envelope[j] = worm(j * Math.PI / 8.0);
        }
        // Beyond its line the worm is at most its body's half-width (~0.06) or the open mouth's fangs (~0.11) in unit terms.
        float length = fit(box, rootX, rootY, Math.min(box.width() * 0.9F, box.height() * 0.95F), 0.11F, 1.0F, envelope);
        if (length <= 0.5F) {
            figure(marks, f, bg, intensity);
            return marks;
        }
        float strength = 0.45F + 0.55F * intensity;
        float[][] curve = worm(time * speed);
        int n = curve.length;
        for (int s = 0; s + 1 < n; s++) {
            float a = s / (float)(n - 1);
            float b = (s + 1) / (float)(n - 1);
            float x0 = rootX + curve[s][0] * length;
            float y0 = rootY + curve[s][1] * length;
            float x1 = rootX + curve[s + 1][0] * length;
            float y1 = rootY + curve[s + 1][1] * length;
            float w0 = length * wormRadius(a);
            float w1 = length * wormRadius(b);
            // Ridged: dark folds between the bands, a vein glowing along it here and there.
            marks.add(new Stroke(x0, y0, x1, y1, w0 * 2.0F, w1 * 2.0F, blend(bg, s % 3 == 0 ? BIKAKU_DARK : BIKAKU, strength)));
            if (s % 3 == 1 && s > 1 && s < n - 4) {
                marks.add(new Stroke(x0, y0, x1, y1, Math.max(0.6F, w0 * 0.25F), Math.max(0.6F, w1 * 0.25F), blend(bg, BIKAKU_VEIN, strength * 0.8F)));
            }
        }
        // He stands in front of its coils (it rises from his back); its head is over his shoulder, in front of everything.
        figure(marks, f, bg, intensity);
        // The mouth: a dark hole ringed by its lip, a crown of fangs round it (shut: leaning in; selected: thrown wide).
        float[] head = curve[n - 1];
        float[] before = curve[n - 3];
        double dx = head[0] - before[0];
        double dy = head[1] - before[1];
        double unit = Math.max(1.0E-6, Math.hypot(dx, dy));
        dx /= unit;
        dy /= unit;
        float hx = rootX + head[0] * length;
        float hy = rootY + head[1] * length;
        float open = selected ? (float)(0.55 + 0.4 * Math.max(0.0, Math.sin(time * 1.6))) : 0.15F;
        float lip = length * wormRadius(1.0F) * (1.0F + 0.35F * open);
        marks.add(new Disc(hx, hy, lip, blend(bg, BIKAKU, strength)));
        marks.add(new Disc(hx + (float)dx * lip * 0.25F, hy + (float)dy * lip * 0.25F, lip * (0.25F + 0.55F * open), blend(bg, 0x120205, strength)));
        for (int k = 0; k < 6; k++) {
            double angle = Math.PI * 2.0 * k / 6.0 + 0.4;
            double cx = Math.cos(angle);
            double sy = Math.sin(angle);
            // Round the mouth (its ring seen a little from the side) and out ahead of it, splaying as it opens.
            double px = -dy * cx * 0.9;
            double py = dx * cx * 0.9;
            float bx = hx + (float)(px * lip + dx * lip * 0.2 * sy);
            float by = hy + (float)(py * lip + dy * lip * 0.2 * sy);
            double ahead = 0.9 - 0.3 * open;
            double out = -0.35 + 0.95 * open;
            float tx = bx + (float)((dx * ahead + px * out) * lip * 1.2);
            float ty = by + (float)((dy * ahead + py * out) * lip * 1.2);
            marks.add(new Stroke(bx, by, tx, ty, Math.max(0.8F, lip * 0.32F), 0.5F, blend(bg, BIKAKU_FANG, strength)));
        }
        return marks;
    }

    /**
     * 4.1.59: one of Bikaku's forms for the menu's preview: {@code worms} worms out of the one root (the first over his right shoulder as the
     * card's worm, the second mirrored over his left, the third rearing higher behind his head), each with its mouth of fangs; with
     * {@code walker} The Walker's two walking tentacles out of the same root carrying him up off the ground, his own legs hanging still.
     */
    public static List<Mark> bikakuForm(PathChoiceLayout.Box box, double time, float intensity, boolean selected, int worms, boolean walker, int bg) {
        List<Mark> marks = new ArrayList<>();
        float lift = walker ? box.height() * 0.24F : 0.0F;
        PathChoiceLayout.Box body = new PathChoiceLayout.Box(box.x(), box.y(), box.width(), Math.max(10, Math.round(box.height() - lift)));
        Figure f = Figure.in(body, walker ? 0.42F : 0.46F);
        float rootX = f.cx;
        float rootY = f.waistY - f.size * 0.04F;
        // Each worm's lane: how far it is turned from the first (radians, toward the middle), mirrored or not, and its own phase.
        double[][] lanes = worms <= 1 ? new double[][] {{0.0, 1, 0.0}} : worms == 2 ? new double[][] {{-0.2, 1, 0.0}, {-0.2, -1, 1.9}}
            : new double[][] {{-0.35, 1, 0.0}, {-0.35, -1, 1.9}, {0.45, 1, 3.4}};
        float thin = worms <= 1 ? 1.0F : worms == 2 ? 0.72F : 0.58F;
        float[][][] envelope = new float[lanes.length * 16][][];
        for (int i = 0; i < lanes.length; i++) {
            for (int j = 0; j < 16; j++) {
                envelope[i * 16 + j] = lane(worm(j * Math.PI / 8.0), lanes[i]);
            }
        }
        float length = fit(box, rootX, rootY, Math.min(box.width() * 0.9F, body.height() * 0.95F), 0.11F, 1.0F, envelope);
        if (length <= 0.5F) {
            figure(marks, f, bg, intensity);
            return marks;
        }
        float strength = 0.45F + 0.55F * intensity;
        if (walker) {
            // The two walking tentacles: out of the root, bowed out to each side and down to the ground, banded, their tips planted.
            for (int side = -1; side <= 1; side += 2) {
                float footX = rootX + side * box.width() * 0.3F;
                float footY = box.bottom() - 2.0F;
                float kneeX = rootX + side * box.width() * 0.38F;
                float kneeY = rootY - (footY - rootY) * 0.15F;
                float px = rootX;
                float py = rootY;
                for (int k = 1; k <= 14; k++) {
                    float t = k / 14.0F;
                    float u = 1.0F - t;
                    float nx = u * u * rootX + 2 * u * t * kneeX + t * t * footX;
                    float ny = u * u * rootY + 2 * u * t * kneeY + t * t * footY;
                    float w = length * (0.09F - 0.06F * t);
                    marks.add(new Stroke(px, py, nx, ny, w + 0.6F, w, blend(bg, k % 3 == 0 ? BIKAKU_DARK : BIKAKU, strength)));
                    px = nx;
                    py = ny;
                }
            }
        }
        double speed = 0.8 + 1.0 * intensity;
        List<float[][]> curves = new ArrayList<>();
        for (int i = 0; i < lanes.length; i++) {
            float[][] curve = lane(worm(time * speed + lanes[i][2]), lanes[i]);
            curves.add(curve);
            int n = curve.length;
            for (int s = 0; s + 1 < n; s++) {
                float a = s / (float)(n - 1);
                float b = (s + 1) / (float)(n - 1);
                float x0 = rootX + curve[s][0] * length;
                float y0 = rootY + curve[s][1] * length;
                float x1 = rootX + curve[s + 1][0] * length;
                float y1 = rootY + curve[s + 1][1] * length;
                float w0 = length * wormRadius(a) * thin;
                float w1 = length * wormRadius(b) * thin;
                marks.add(new Stroke(x0, y0, x1, y1, w0 * 2.0F, w1 * 2.0F, blend(bg, s % 3 == 0 ? BIKAKU_DARK : BIKAKU, strength)));
                if (s % 3 == 1 && s > 1 && s < n - 4) {
                    marks.add(new Stroke(x0, y0, x1, y1, Math.max(0.6F, w0 * 0.25F), Math.max(0.6F, w1 * 0.25F), blend(bg, BIKAKU_VEIN, strength * 0.8F)));
                }
            }
        }
        figure(marks, f, bg, intensity);
        for (float[][] curve : curves) {
            int n = curve.length;
            float[] head = curve[n - 1];
            float[] before = curve[n - 3];
            double dx = head[0] - before[0];
            double dy = head[1] - before[1];
            double unit = Math.max(1.0E-6, Math.hypot(dx, dy));
            dx /= unit;
            dy /= unit;
            float hx = rootX + head[0] * length;
            float hy = rootY + head[1] * length;
            float open = selected ? (float)(0.55 + 0.4 * Math.max(0.0, Math.sin(time * 1.6))) : 0.15F;
            float lip = length * wormRadius(1.0F) * Math.max(0.75F, thin) * (1.0F + 0.35F * open);
            marks.add(new Disc(hx, hy, lip, blend(bg, BIKAKU, strength)));
            marks.add(new Disc(hx + (float)dx * lip * 0.25F, hy + (float)dy * lip * 0.25F, lip * (0.25F + 0.55F * open), blend(bg, 0x120205, strength)));
            for (int k = 0; k < 6; k++) {
                double angle = Math.PI * 2.0 * k / 6.0 + 0.4;
                double cx = Math.cos(angle);
                double sy = Math.sin(angle);
                double px = -dy * cx * 0.9;
                double py = dx * cx * 0.9;
                float bx = hx + (float)(px * lip + dx * lip * 0.2 * sy);
                float by = hy + (float)(py * lip + dy * lip * 0.2 * sy);
                double ahead = 0.9 - 0.3 * open;
                double out = -0.35 + 0.95 * open;
                float tx = bx + (float)((dx * ahead + px * out) * lip * 1.2);
                float ty = by + (float)((dy * ahead + py * out) * lip * 1.2);
                marks.add(new Stroke(bx, by, tx, ty, Math.max(0.8F, lip * 0.32F), 0.5F, blend(bg, BIKAKU_FANG, strength)));
            }
        }
        return marks;
    }

    /** A worm's line turned {@code lane[0]} radians toward the middle and mirrored if {@code lane[1]} is -1 (unit, about its root). */
    private static float[][] lane(float[][] curve, double[] lane) {
        double turn = -lane[0];
        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        float[][] out = new float[curve.length][];
        for (int i = 0; i < curve.length; i++) {
            double x = curve[i][0] * cos - curve[i][1] * sin;
            double y = curve[i][0] * sin + curve[i][1] * cos;
            out[i] = new float[] {(float)(x * lane[1]), (float)y};
        }
        return out;
    }

    /** The worm's thickness along it (unit, {@code a} 0 at his back .. 1 its mouth): thin out of the back, thick, a swollen head. */
    private static float wormRadius(float a) {
        float body = 0.02F + 0.035F * Math.min(1.0F, a * 6.0F);
        float head = a > 0.88F ? 0.02F * (float)Math.sin(Math.PI * (a - 0.88F) / 0.24F) : 0.0F;
        return body + head;
    }

    /** The worm's line (unit, 40 points): up out of his back in zigzag coils high over his head, round over his right shoulder. */
    private static float[][] worm(double phase) {
        double[][] knots = {{0.0, 0.0}, {0.05, -0.08}, {0.32, -0.2}, {-0.3, -0.36}, {0.34, -0.52}, {-0.24, -0.68}, {0.22, -0.84}, {0.52, -0.78},
            {0.62, -0.6}};
        int count = 40;
        float[][] points = new float[count][];
        for (int i = 0; i < count; i++) {
            double t = i / (double)(count - 1) * (knots.length - 1);
            int k = Math.min(knots.length - 2, (int)Math.floor(t));
            double u = t - k;
            double[] p0 = knots[Math.max(0, k - 1)];
            double[] p1 = knots[k];
            double[] p2 = knots[k + 1];
            double[] p3 = knots[Math.min(knots.length - 1, k + 2)];
            double x = catmull(p0[0], p1[0], p2[0], p3[0], u);
            double y = catmull(p0[1], p1[1], p2[1], p3[1], u);
            // A wave running up its coils (it slithers), none at the root.
            double along = i / (double)(count - 1);
            x += 0.06 * Math.sin(phase - along * 7.0) * Math.min(1.0, along * 4.0);
            y += 0.02 * Math.sin(phase * 0.8 - along * 5.0) * Math.min(1.0, along * 4.0);
            points[i] = new float[] {(float)x, (float)y};
        }
        return points;
    }

    private static double catmull(double p0, double p1, double p2, double p3, double u) {
        return 0.5 * (2.0 * p1 + (-p0 + p2) * u + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * u * u + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * u * u * u);
    }

    /** Opaque mix of {@code fg} over {@code bg} at {@code alpha} (0..1). */
    public static int blend(int bg, int fg, float alpha) {
        float a = Math.max(0.0F, Math.min(1.0F, alpha));
        int r = Math.round(((bg >> 16) & 0xFF) * (1.0F - a) + ((fg >> 16) & 0xFF) * a);
        int g = Math.round(((bg >> 8) & 0xFF) * (1.0F - a) + ((fg >> 8) & 0xFF) * a);
        int b = Math.round((bg & 0xFF) * (1.0F - a) + (fg & 0xFF) * a);
        return r << 16 | g << 8 | b;
    }
}
