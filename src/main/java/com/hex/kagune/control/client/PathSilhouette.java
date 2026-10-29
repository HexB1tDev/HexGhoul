package com.hex.kagune.control.client;

import com.hex.kagune.control.ayato.UkakuWing;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry for the animated silhouettes on the path choice cards: a figure seen from behind, with Kaneki's
 * Rinkaku tendrils from the lower back or Ayato's Ukaku wings from the shoulder blades (docs/AYATO_PATH.md, "Look").
 *
 * <p>Colours are opaque RGB, already blended against the card background, because the GUI draws strokes as many
 * small overlapping fills and real alpha would darken every overlap. Every mark stays inside the box it was made
 * for (tested), so nothing bleeds out of a card at any GUI size.
 */
public final class PathSilhouette {
    /** Kaneki path (Rinkaku): crimson scales with a darker core. */
    public static final int RINKAKU = 0xC4162E;
    public static final int RINKAKU_DARK = 0x6E0A1A;
    /** Ayato path (Ukaku, canon vermilion): body, deep crimson core, hot crystallized edge. */
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

    /** Kaneki path card art: four Rinkaku tendrils from the lower back, writhing. {@code intensity} 0..1 (dim → selected). */
    public static List<Mark> kaneki(PathChoiceLayout.Box box, double time, float intensity, int bg) {
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
     * Ayato path card art: the same Ukaku wings the game draws ({@link UkakuWing}), seen from the front like the anime:
     * both wings behind the figure, flickering flames while gaseous, crystallized blades when the card is selected.
     */
    public static List<Mark> ayato(PathChoiceLayout.Box box, double time, float intensity, boolean crystallized, int bg) {
        List<Mark> marks = new ArrayList<>();
        Figure f = Figure.in(box, 0.52F); // Ayato's wings dwarf him, like the anime frame
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

    /** Eto path (6.1.0): pale living flesh, its folds, a protrusion's darker tip, and the faint steam off her skin. */
    public static final int FLESH = 0xEADFD5;
    public static final int FLESH_FOLD = 0xBFA99D;
    public static final int FLESH_DEEP = 0x8F7469;
    public static final int STEAM = 0xDCE0E2;

    /**
     * Eto path card art (6.1.0): her from behind, and out of her upper back a lumpy mass of pale flesh rearing over her right
     * shoulder and curling down into a malformed hand, bulging and folding as it breathes, bony spurs along its back; faint steam
     * rises off her shoulders. Selected, the flesh rears higher and the hand opens.
     */
    public static List<Mark> eto(PathChoiceLayout.Box box, double time, float intensity, boolean selected, int bg) {
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

    /** Furuta DNA (7.0.0): black-crimson rinkaku muscle, its dark bands, and the glow of the centipede's seams and mouth. */
    public static final int FURUTA = 0xA8142A;
    public static final int FURUTA_DARK = 0x3A0710;
    public static final int FURUTA_GLOW = 0xFF4A3C;

    /**
     * Furuta DNA card art (7.0.0): him from behind, two thick banded tails from the lower back swinging low and wide; selected, a
     * third, a centipede, rears up between them over his head with its toothed mouth open.
     */
    public static List<Mark> furuta(PathChoiceLayout.Box box, double time, float intensity, boolean selected, int bg) {
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
                marks.add(new Stroke(hx, hy, jx, jy, length * 0.045F, length * 0.012F, blend(bg, FURUTA_DARK, strength)));
            }
            marks.add(new Disc(hx + (float)(dx / unit) * length * 0.03F, hy + (float)(dy / unit) * length * 0.03F, length * 0.03F * open,
                blend(bg, FURUTA_GLOW, strength)));
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
            int colour = s % 2 == 0 ? FURUTA : FURUTA_DARK;
            marks.add(new Stroke(x0, y0, x1, y1, root + (tip - root) * a, root + (tip - root) * b, blend(bg, colour, strength)));
            if (seams && s % 2 == 1) {
                marks.add(new Disc(x1, y1, Math.max(0.6F, (root + (tip - root) * b) * 0.22F), blend(bg, FURUTA_GLOW, strength * 0.9F)));
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

    /** Opaque mix of {@code fg} over {@code bg} at {@code alpha} (0..1). */
    public static int blend(int bg, int fg, float alpha) {
        float a = Math.max(0.0F, Math.min(1.0F, alpha));
        int r = Math.round(((bg >> 16) & 0xFF) * (1.0F - a) + ((fg >> 16) & 0xFF) * a);
        int g = Math.round(((bg >> 8) & 0xFF) * (1.0F - a) + ((fg >> 8) & 0xFF) * a);
        int b = Math.round((bg & 0xFF) * (1.0F - a) + (fg & 0xFF) * a);
        return r << 16 | g << 8 | b;
    }
}
