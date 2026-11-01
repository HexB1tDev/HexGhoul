package com.hex.kagune.control.npc;

/**
 * 1.3.36: the arm blade's shape (pure): quads in the right arm's own frame (model pixels: x across the arm, toward the body +, y down
 * the arm from the shoulder's pivot, z toward the back +), as {@link BladeArm#WRAP_TICKS} wind it out ({@code wrap} 0..1).
 * <ul>
 *   <li>a fin of plates where it tears out of the shoulder blade;</li>
 *   <li>a spiral of hard plates round the arm, shoulder to wrist, each one tilted along the spiral, a light ridge down its back;</li>
 *   <li>a sleeve closed over the forearm where the spiral ends;</li>
 *   <li>the blade past the fist, along the arm: four tapering lengths, its edge on the thumb's side, a glowing seam down both flats, a
 *       point; two angled fins at its root, twisted like the spiral (a drill's look, a lance's line).</li>
 * </ul>
 * Colours are the kagune's ({@code colour}), its plates darker, their ridges and the edge lighter, the seam glowing (a quad whose
 * normal is straight up is drawn at full brightness: the renderers' convention for RC light; the seam is given one).
 */
public final class BladeArmModel {
    private BladeArmModel() {
    }

    /** Receives one quad: four corners (pixels, the arm's frame), its normal, and each corner's colour (ARGB). */
    public interface Sink {
        void quad(double[] a, double[] b, double[] c, double[] d, double[] n, int ca, int cb, int cc, int cd);
    }

    /** The straight-up normal that marks a quad as glowing (drawn full bright). */
    public static final double[] GLOW = {0.0, 1.0, 0.0};

    /**
     * The arm blade {@code wrap} of the way out on a {@code slim} (3 pixel) or wide (4 pixel) arm, in {@code colour} (0xRRGGBB), into
     * {@code sink}.
     */
    public static void build(double wrap, boolean slim, int colour, Sink sink) {
        if (wrap <= 0.0) {
            return;
        }
        double cx = slim ? -0.5 : -1.0;
        double half = slim ? 1.5 : 2.0;
        int plate = shade(colour, 0.62);
        int ridge = shade(colour, 1.12);
        int dark = shade(colour, 0.32);
        int glow = 0xFF000000 | brighten(colour);
        // Where it tears out: a fin of three plates on the shoulder blade, pointing up and back.
        double fin = BladeArm.plate(0, wrap);
        if (fin > 0.0) {
            for (int k = 0; k < 3; k++) {
                double s = fin * (1.0 - 0.22 * k);
                double[] at = {cx + half * 0.6 + 0.4 * k, -1.6 - 1.2 * k * s, half + 0.6 + 0.5 * k};
                box(sink, at, axis(0.0, -0.55, 0.8), axis(1.0, 0.0, 0.0), axis(0.0, 0.8, 0.55), 1.4 * s, (2.6 - 0.5 * k) * s, 0.7 * s,
                    k == 0 ? plate : ridge, dark);
            }
        }
        // The spiral, shoulder to wrist.
        double radius = half + 0.55;
        double rise = 10.6 / (BladeArm.PLATES - 1.0);
        double turn = 2.0 * Math.PI * BladeArm.TURNS / (BladeArm.PLATES - 1.0);
        double tilt = Math.atan2(rise, turn * radius);
        for (int i = 0; i < BladeArm.PLATES; i++) {
            double s = BladeArm.plate(i, wrap);
            if (s <= 0.0) {
                continue;
            }
            double t = i / (BladeArm.PLATES - 1.0);
            double phi = Math.PI / 2.0 + 2.0 * Math.PI * BladeArm.TURNS * t;
            double[] radial = {Math.cos(phi), 0.0, Math.sin(phi)};
            double[] tangent = {-Math.sin(phi), 0.0, Math.cos(phi)};
            // Along the spiral: the tangent tipped down the arm by the spiral's own slope.
            double[] along = {tangent[0] * Math.cos(tilt), Math.sin(tilt), tangent[2] * Math.cos(tilt)};
            double[] across = {-tangent[0] * Math.sin(tilt), Math.cos(tilt), -tangent[2] * Math.sin(tilt)};
            double y = -1.0 + 10.6 * t;
            double r = radius + 0.25 * (1.0 - s);
            double[] at = {cx + radial[0] * r, y, radial[2] * r};
            double size = 0.55 + 0.45 * s;
            box(sink, at, along, across, radial, 3.2 * size, 2.3 * size, 0.85 * size, plate, dark);
            // Its ridge down the back of the plate, a little proud of it.
            double[] proud = {at[0] + radial[0] * 0.5 * size, at[1], at[2] + radial[2] * 0.5 * size};
            box(sink, proud, along, across, radial, 2.6 * size, 0.55 * size, 0.3 * size, ridge, plate);
        }
        // The sleeve over the forearm, as the spiral closes there.
        double sleeve = BladeArm.plate(BladeArm.PLATES - 3, wrap);
        if (sleeve > 0.0) {
            double grow = 0.35 + 0.65 * sleeve;
            box(sink, new double[] {cx, 8.35, 0.0}, axis(0, 0, 1), axis(0, 1, 0), axis(1, 0, 0), (half + 0.45) * 2.0 * grow, 3.9, (half + 0.45) * 2.0 * grow,
                plate, dark);
        }
        // The blade.
        double out = BladeArm.blade(wrap);
        if (out <= 0.0) {
            return;
        }
        double length = BladeArm.BLADE_LENGTH * out;
        double root = 10.0;
        double[] widths = {3.6, 3.2, 2.5, 1.6};
        double[] thick = {1.1, 0.95, 0.8, 0.6};
        double front = -0.35; // a little to the thumb's side (z -)
        for (int k = 0; k < 4; k++) {
            double y0 = root + length * k / 4.0;
            double y1 = root + length * (k + 1) / 4.0;
            double w = widths[k] * (0.5 + 0.5 * out);
            double[] mid = {cx, (y0 + y1) / 2.0, front};
            box(sink, mid, axis(0, 0, 1), axis(0, 1, 0), axis(1, 0, 0), w, y1 - y0 + 0.05, thick[k], plate, dark);
            // The edge, on the thumb's side.
            box(sink, new double[] {cx, (y0 + y1) / 2.0, front - w / 2.0 - 0.15}, axis(0, 0, 1), axis(0, 1, 0), axis(1, 0, 0), 0.4, y1 - y0 + 0.05,
                thick[k] * 0.55, ridge, plate);
            // The glowing seam down both flats.
            for (double side : new double[] {-1.0, 1.0}) {
                double[] seam = {cx + side * (thick[k] / 2.0 + 0.04), (y0 + y1) / 2.0, front + 0.2};
                flat(sink, seam, axis(0, 0, 1), axis(0, 1, 0), new double[] {side, 0, 0}, 0.45 * (1.0 - 0.15 * k), y1 - y0, glow);
            }
        }
        // The point: a narrow wedge past the last length.
        double tip = root + length;
        box(sink, new double[] {cx, tip + 1.0 * out, front + 0.3}, axis(0, 0, 1), axis(0, 1, 0), axis(1, 0, 0), 0.8, 2.0 * out, 0.45, ridge, plate);
        // Two fins at its root, turned like the spiral.
        for (double side : new double[] {-1.0, 1.0}) {
            double[] at = {cx + side * 1.3, root + 1.2, 0.6};
            box(sink, at, axis(side * 0.35, 0.94, 0.0), axis(0, 0, 1), axis(side, 0.0, 0.0), 0.7, 3.0 * out, 1.6, ridge, dark);
        }
    }

    // ---------------------------------------------------------------- boxes

    static double[] axis(double x, double y, double z) {
        double n = Math.sqrt(x * x + y * y + z * z);
        return n < 1.0E-9 ? new double[] {0, 1, 0} : new double[] {x / n, y / n, z / n};
    }

    /**
     * A box centred on {@code at}, {@code a} long along {@code u}, {@code b} along {@code v}, {@code c} along {@code w}: its faces in
     * {@code colour}, the edges toward {@code -v}'s end shading toward {@code low} (a plate darker where it tucks under the next).
     */
    static void box(Sink sink, double[] at, double[] u, double[] v, double[] w, double a, double b, double c, int colour, int low) {
        double[][] corner = new double[8][];
        for (int i = 0; i < 8; i++) {
            double su = ((i & 1) == 0 ? -0.5 : 0.5) * a;
            double sv = ((i & 2) == 0 ? -0.5 : 0.5) * b;
            double sw = ((i & 4) == 0 ? -0.5 : 0.5) * c;
            corner[i] = new double[] {at[0] + u[0] * su + v[0] * sv + w[0] * sw, at[1] + u[1] * su + v[1] * sv + w[1] * sw,
                at[2] + u[2] * su + v[2] * sv + w[2] * sw};
        }
        int base = 0xFF000000 | colour & 0xFFFFFF;
        int under = 0xFF000000 | mix(colour, low, 0.55) & 0xFFFFFF;
        int[][] faces = {{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}};
        double[][] normals = {neg(u), u, neg(v), v, neg(w), w};
        for (int f = 0; f < 6; f++) {
            int[] q = faces[f];
            int[] c4 = new int[4];
            for (int k = 0; k < 4; k++) {
                // The corners at the -v end of the box a little darker.
                c4[k] = (q[k] & 2) == 0 ? under : base;
            }
            sink.quad(corner[q[0]], corner[q[1]], corner[q[2]], corner[q[3]], normals[f], c4[0], c4[1], c4[2], c4[3]);
        }
    }

    /** A single flat quad (a glowing seam), {@code a} along {@code u}, {@code b} along {@code v}, facing {@code n}. */
    static void flat(Sink sink, double[] at, double[] u, double[] v, double[] n, double a, double b, int argb) {
        double[][] c = new double[4][];
        double[][] signs = {{-0.5, -0.5}, {0.5, -0.5}, {0.5, 0.5}, {-0.5, 0.5}};
        for (int k = 0; k < 4; k++) {
            c[k] = new double[] {at[0] + u[0] * signs[k][0] * a + v[0] * signs[k][1] * b, at[1] + u[1] * signs[k][0] * a + v[1] * signs[k][1] * b,
                at[2] + u[2] * signs[k][0] * a + v[2] * signs[k][1] * b};
        }
        // Glowing: drawn full bright (the up normal is the renderers' mark for it).
        sink.quad(c[0], c[1], c[2], c[3], GLOW, argb, argb, argb, argb);
    }

    static double[] neg(double[] v) {
        return new double[] {-v[0], -v[1], -v[2]};
    }

    /** {@code rgb} scaled in brightness by {@code k} (above 1 lightens toward white a little). */
    static int shade(int rgb, double k) {
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        if (k > 1.0) {
            double t = Math.min(1.0, k - 1.0);
            r = (int)(r + (255 - r) * t);
            g = (int)(g + (255 - g) * t);
            b = (int)(b + (255 - b) * t);
            return r << 16 | g << 8 | b;
        }
        return (int)(r * k) << 16 | (int)(g * k) << 8 | (int)(b * k);
    }

    /** The colour at full brightness and a touch paler: its glow. */
    static int brighten(int rgb) {
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        int most = Math.max(1, Math.max(r, Math.max(g, b)));
        double k = 255.0 / most;
        r = (int)Math.min(255, r * k * 0.85 + 38);
        g = (int)Math.min(255, g * k * 0.85 + 38);
        b = (int)Math.min(255, b * k * 0.85 + 38);
        return r << 16 | g << 8 | b;
    }

    static int mix(int a, int b, double t) {
        int r = (int)((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int)((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int)((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }
}
