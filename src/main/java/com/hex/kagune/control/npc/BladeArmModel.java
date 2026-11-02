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
                wedge(sink, at, axis(0.0, -0.55, 0.8), axis(1.0, 0.0, 0.0), (3.4 - 0.6 * k) * s, 1.5 * s, 0.75 * s, k == 0 ? plate : ridge, ridge);
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
            // 1.3.69 (the owner: "sharper and less cubic"): a pointed scale laid along the spiral, its point down the arm and a little out,
            // its keel a light ridge (it was a box with a box on it).
            double[] base = {at[0] - along[0] * 1.5 * size, at[1] - along[1] * 1.5 * size, at[2] - along[2] * 1.5 * size};
            double[] lean = axis(along[0] + radial[0] * 0.18 + 0.0, along[1] + 0.12, along[2] + radial[2] * 0.18);
            wedge(sink, base, lean, across, 3.9 * size, 2.4 * size, 0.85 * size, plate, ridge);
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
        double front = -0.35; // a little to the thumb's side (z -)
        sabre(sink, cx, root, front, length, 3.6 * (0.5 + 0.5 * out), 1.15, plate, ridge, dark, glow);
        // Two fins at its root, turned like the spiral.
        for (double side : new double[] {-1.0, 1.0}) {
            double[] at = {cx + side * 1.1, root - 0.2, 0.6};
            wedge(sink, at, axis(side * 0.4, 0.92, 0.12), axis(0, 0, 1), 3.6 * out, 1.5, 0.6, ridge, ridge);
        }
    }

    /** 1.3.69: how many sections the blade is drawn in, root to point. */
    static final int SECTIONS = 12;

    /**
     * 1.3.69 (the owner: "make the kagune arm blade for ghouls sharper and less cubic"; it was four boxes and a box for its point): one
     * blade, root to point: a diamond across (both edges sharp, thickest down its middle), widest a little past the root and narrowing in a
     * long curve to a needle point, swept a little forward (toward the thumb, z -) like a sabre; the edge side lighter, a glowing seam down
     * both flats. {@code width} and {@code thick} at the root, {@code length} past {@code root} down the arm.
     */
    static void sabre(Sink sink, double cx, double root, double front, double length, double width, double thick, int plate, int edge, int dark,
                      int glow) {
        double[][][] ring = new double[SECTIONS + 1][][];
        for (int i = 0; i <= SECTIONS; i++) {
            double u = (double)i / SECTIONS;
            double y = root + length * u;
            // Swept forward toward the point, a sabre's curve.
            double z = front - 1.3 * u * u;
            // Widest a little past the root, then a long curve to a point (0 at the end).
            double w = width * (0.82 + 0.5 * Math.sin(Math.min(1.0, u * 4.0) * Math.PI / 2.0) * (1.0 - u * 0.2)) * Math.pow(1.0 - u, 0.75) * 0.75;
            double t = thick * Math.pow(1.0 - u, 0.6) * 0.5;
            // The diamond: the forward edge (z -), one flat's middle (x +), the back edge (z +, blunter), the other flat (x -).
            ring[i] = new double[][] {{cx, y, z - w * 0.55}, {cx + t, y, z}, {cx, y, z + w * 0.45}, {cx - t, y, z}};
        }
        for (int i = 0; i < SECTIONS; i++) {
            double[][] a = ring[i];
            double[][] b = ring[i + 1];
            double fade = (double)i / SECTIONS;
            int flat = mix(plate, edge, 0.15 + 0.25 * fade);
            for (int k = 0; k < 4; k++) {
                int k2 = (k + 1) % 4;
                double[] mid = {cx, (a[k][1] + b[k][1]) / 2.0, (a[0][2] + a[2][2] + b[0][2] + b[2][2]) / 4.0};
                double[] n = outward(new double[][] {a[k], a[k2], b[k2], b[k]}, mid);
                // The faces toward the forward edge (k 3 -> 0 -> 1) lighter: the cutting edge catching the light.
                int near = 0xFF000000 | (k == 0 || k == 3 ? edge : flat) & 0xFFFFFF;
                int far = 0xFF000000 | (k == 0 || k == 3 ? flat : mix(plate, dark, 0.3)) & 0xFFFFFF;
                sink.quad(a[k], a[k2], b[k2], b[k], n, near, far, far, near);
            }
            // The glowing seam down both flats (not the last length: it runs out before the point).
            if (i < SECTIONS - 2) {
                for (int side = 1; side <= 3; side += 2) {
                    double[] pa = a[side];
                    double[] pb = b[side];
                    double out = side == 1 ? 0.04 : -0.04;
                    double h = 0.2 * (1.0 - fade);
                    sink.quad(new double[] {pa[0] + out, pa[1], pa[2] - h}, new double[] {pa[0] + out, pa[1], pa[2] + h},
                        new double[] {pb[0] + out, pb[1], pb[2] + h * 0.8}, new double[] {pb[0] + out, pb[1], pb[2] - h * 0.8}, GLOW, glow, glow, glow, glow);
                }
            }
        }
    }

    /**
     * 1.3.69: a pointed plate: a {@code width} by {@code thick} base at {@code base}, coming to a point {@code length} along {@code dir}
     * ({@code across}: its width's way), its keel ({@code keel} colour) along the top; four sloping faces and the base.
     */
    static void wedge(Sink sink, double[] base, double[] dir, double[] across, double length, double width, double thick, int colour, int keel) {
        double[] up = axis(dir[1] * across[2] - dir[2] * across[1], dir[2] * across[0] - dir[0] * across[2], dir[0] * across[1] - dir[1] * across[0]);
        double[] tip = add(base, scale(dir, length));
        double[] l = add(base, scale(across, -width / 2.0));
        double[] r = add(base, scale(across, width / 2.0));
        double[] top = add(add(base, scale(up, thick / 2.0)), scale(dir, length * 0.18));
        double[] bottom = add(base, scale(up, -thick / 2.0));
        int c = 0xFF000000 | colour & 0xFFFFFF;
        int k = 0xFF000000 | keel & 0xFFFFFF;
        int under = 0xFF000000 | mix(colour, 0, 0.35) & 0xFFFFFF;
        double[] mid = add(base, scale(dir, length * 0.3));
        sink.quad(l, top, tip, tip, outward(new double[][] {l, top, tip}, mid), c, k, c, c);
        sink.quad(top, r, tip, tip, outward(new double[][] {top, r, tip}, mid), k, c, c, c);
        sink.quad(r, bottom, tip, tip, outward(new double[][] {r, bottom, tip}, mid), under, under, under, under);
        sink.quad(bottom, l, tip, tip, outward(new double[][] {bottom, l, tip}, mid), under, under, under, under);
        sink.quad(l, bottom, r, top, outward(new double[][] {l, bottom, r, top}, mid), under, under, under, c);
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    /**
     * The unit normal of the face through {@code corners}, turned to face away from {@code inside} (the drawing is two-sided, so the
     * winding never matters, only the light). Never exactly straight up: that is the mark of a glowing quad.
     */
    static double[] outward(double[][] corners, double[] inside) {
        double[] n = normal(corners[0], corners[1], corners[2]);
        double[] c = {0.0, 0.0, 0.0};
        for (double[] p : corners) {
            c = add(c, scale(p, 1.0 / corners.length));
        }
        double[] away = {c[0] - inside[0], c[1] - inside[1], c[2] - inside[2]};
        if (n[0] * away[0] + n[1] * away[1] + n[2] * away[2] < 0.0) {
            n = scale(n, -1.0);
        }
        return n[1] > 0.9999 ? axis(0.01, 1.0, 0.0) : n;
    }

    /** The unit normal of the triangle {@code a b c} (the way its winding faces). */
    static double[] normal(double[] a, double[] b, double[] c) {
        double ux = b[0] - a[0];
        double uy = b[1] - a[1];
        double uz = b[2] - a[2];
        double vx = c[0] - a[0];
        double vy = c[1] - a[1];
        double vz = c[2] - a[2];
        return axis(uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx);
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
