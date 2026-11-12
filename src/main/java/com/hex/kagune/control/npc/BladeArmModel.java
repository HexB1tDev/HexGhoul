package com.hex.kagune.control.npc;

/**
 * 1.3.36: the arm blade's shape (pure): quads in the right arm's own frame (model pixels: x across the arm, toward the body +, y down
 * the arm from the shoulder's pivot, z toward the back +), as {@link BladeArm#WRAP_TICKS} wind it out ({@code wrap} 0..1).
 *
 * <p>4.1.47 (the owner: "Improve severely the kagune arm blade of the ghouls, it looks way too small, not sharp and horribly made compared
 * to literally every other kagune", then a picture: "it should look like this": broad smooth bands wound round the whole arm, red veins
 * across them, flowing into one huge, smooth, single-edged blade curving to its point, a bright gleam all down its edge):
 * <ul>
 *   <li>two broad ribbons wound round the arm from the shoulder to the wrist ({@link #RIBBON_TURNS} turns each, half a turn apart), flaring
 *       out toward the wrist where they flow into the blade, red veins branching across them;</li>
 *   <li>the blade, grown through the forearm and out past the fist ({@link BladeArm#BLADE_LENGTH}): a wedge across (a razor edge on the
 *       back of the arm, z +, so it faces down with the arm held out: the owner chose "B: blade turned over"; a thick spine on the front),
 *       widening toward its far end, its edge a long convex sweep up to the point at the spine's line, the spine
 *       curving back a little (a falchion's line); lighter toward the edge, a glowing gleam down the edge itself, veins at its root.</li>
 * </ul>
 * Colours are the kagune's ({@code colour}); a quad whose normal is straight up is drawn at full brightness (the renderers' convention for
 * RC light: the gleam).
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
        int body = colour & 0xFFFFFF;
        int deep = shade(colour, 0.55);
        int light = shade(colour, 1.28);
        int vein = veins(colour);
        int gleam = 0xFF000000 | shade(brighten(colour), 1.55);
        // The ribbons, from the shoulder down as it winds out.
        double reach = Math.max(0.0, Math.min(1.0, (wrap - 0.04) / 0.6));
        for (int r = 0; r < RIBBONS; r++) {
            ribbon(sink, cx, half, r, reach, body, deep, light, vein);
        }
        double out = BladeArm.blade(wrap);
        if (out > 0.0) {
            // 4.1.47 (the owner, shown both: "B: blade turned over"): its edge and its point on the back of the arm (z +), so with the arm
            // held out before it they face down.
            blade(turned(sink), cx, half, out, body, deep, light, vein, gleam);
        }
    }

    /** 4.1.47: {@code sink} with every point and normal mirrored front to back (z), a glowing quad still glowing. */
    static Sink turned(Sink sink) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(flipZ(a), flipZ(d), flipZ(c), flipZ(b), n == GLOW ? GLOW : flipZ(n), ca, cd, cc, cb);
    }

    static double[] flipZ(double[] p) {
        return new double[] {p[0], p[1], -p[2]};
    }

    /** 4.1.47: the ribbons wound round the arm, how many turns each makes, how broad each is (pixels), how thick. */
    static final int RIBBONS = 2;
    static final double RIBBON_TURNS = 1.3;
    static final double RIBBON_WIDTH = 3.1;
    static final double RIBBON_THICK = 0.7;
    /** 4.1.47: how many lengths a ribbon is drawn in. */
    static final int RIBBON_STEPS = 40;
    /** 4.1.47: where the ribbons start (the shoulder) and end (the wrist), down the arm. */
    static final double RIBBON_TOP = -1.6;
    static final double RIBBON_BOTTOM = 10.4;

    /**
     * 4.1.47: ribbon {@code r} (of {@link #RIBBONS}, each half a turn round from the last), {@code reach} of it grown from the shoulder: a
     * broad smooth band on the arm, a little off it (further toward the wrist, where it flares into the blade), its outer face lit, its
     * inner face and edges darker, veins branching across its outer face.
     */
    static void ribbon(Sink sink, double cx, double half, int r, double reach, int body, int deep, int light, int vein) {
        int steps = (int)Math.ceil(RIBBON_STEPS * reach);
        if (steps <= 0) {
            return;
        }
        double[][] outer0 = null;
        double[][] inner0 = null;
        for (int i = 0; i <= steps; i++) {
            double s = Math.min(reach, (double)i / RIBBON_STEPS);
            double[][][] cut = ribbonCut(cx, half, r, s);
            double[][] outer = cut[0];
            double[][] inner = cut[1];
            if (outer0 != null) {
                double[] centre = {cx, (outer[0][1] + outer0[0][1]) / 2.0, 0.0};
                double lit = 0.5 + 0.5 * Math.cos(s * Math.PI * 2.0 * RIBBON_TURNS + r * Math.PI);
                int face = 0xFF000000 | mix(body, light, 0.35 * lit);
                int under = 0xFF000000 | deep;
                // Its outer face, its inner face, its two edges.
                sink.quad(outer0[0], outer0[1], outer[1], outer[0], outward(new double[][] {outer0[0], outer0[1], outer[1], outer[0]}, centre),
                    face, face, face, face);
                sink.quad(inner0[0], inner0[1], inner[1], inner[0], inward(new double[][] {inner0[0], inner0[1], inner[1], inner[0]}, centre),
                    under, under, under, under);
                int side = 0xFF000000 | mix(body, deep, 0.5);
                sink.quad(outer0[0], outer[0], inner[0], inner0[0], outward(new double[][] {outer0[0], outer[0], inner[0], inner0[0]}, centre),
                    side, side, side, side);
                sink.quad(outer0[1], outer[1], inner[1], inner0[1], outward(new double[][] {outer0[1], outer[1], inner[1], inner0[1]}, centre),
                    side, side, side, side);
            }
            outer0 = outer;
            inner0 = inner;
        }
        // Veins across it: a few cracks, each from one edge toward the other with a branch, where the ribbon has grown.
        java.util.Random random = new java.util.Random(91L + r * 7919L);
        int cracks = 11;
        for (int c = 0; c < cracks; c++) {
            double s0 = (c + 0.25 + 0.5 * random.nextDouble()) / cracks;
            double from = random.nextBoolean() ? 0.08 : 0.92;
            double run = 0.05 + 0.05 * random.nextDouble();
            double towards = from < 0.5 ? 0.65 + 0.25 * random.nextDouble() : 0.35 - 0.25 * random.nextDouble();
            if (s0 + run > reach) {
                continue;
            }
            double[] a = onRibbon(cx, half, r, s0, from);
            double[] m = onRibbon(cx, half, r, s0 + run * 0.5, (from + towards) / 2.0 + 0.12 * (random.nextDouble() - 0.5));
            double[] e = onRibbon(cx, half, r, s0 + run, towards);
            crack(sink, cx, a, m, 0.36, vein);
            crack(sink, cx, m, e, 0.26, vein);
            double[] fork = onRibbon(cx, half, r, s0 + run * (0.7 + 0.2 * random.nextDouble()), from < 0.5 ? 0.3 : 0.7);
            crack(sink, cx, m, fork, 0.2, vein);
        }
    }

    /** 4.1.47: how far ribbon {@code r}'s middle is round the arm and down it, {@code s} of its way (0 shoulder, 1 wrist). */
    static double ribbonAngle(int r, double s) {
        return Math.PI * 0.35 + r * Math.PI + 2.0 * Math.PI * RIBBON_TURNS * s;
    }

    /** 4.1.47: how far off the arm's middle ribbon's face is at {@code s}: close at the shoulder, swelling a little, flared at the wrist. */
    static double ribbonRadius(double half, double s) {
        return half + 0.6 + 0.55 * Math.sin(s * Math.PI) + 1.5 * Math.pow(Math.max(0.0, (s - 0.68) / 0.32), 2.0);
    }

    /** 4.1.47: a point on ribbon {@code r}'s outer face, {@code s} of its way down, {@code across} 0..1 of its breadth. */
    static double[] onRibbon(double cx, double half, int r, double s, double across) {
        double[][][] cut = ribbonCut(cx, half, r, s);
        double[] p = lerp(cut[0][0], cut[0][1], across);
        double[] n = axis(p[0] - cx, 0.0, p[2]);
        return add(p, scale(n, 0.05));
    }

    /** 4.1.47: ribbon {@code r}'s cross-cut at {@code s}: its outer face's two corners, then its inner face's. */
    static double[][][] ribbonCut(double cx, double half, int r, double s) {
        double phi = ribbonAngle(r, s);
        double radius = ribbonRadius(half, s);
        double y = RIBBON_TOP + (RIBBON_BOTTOM - RIBBON_TOP) * s;
        double[] radial = {Math.cos(phi), 0.0, Math.sin(phi)};
        // Its way along (round and down), and across it on the arm's surface (square to that).
        double dphi = 2.0 * Math.PI * RIBBON_TURNS;
        double[] along = axis(-Math.sin(phi) * radius * dphi, RIBBON_BOTTOM - RIBBON_TOP, Math.cos(phi) * radius * dphi);
        double[] across = axis(radial[1] * along[2] - radial[2] * along[1], radial[2] * along[0] - radial[0] * along[2], radial[0] * along[1] - radial[1] * along[0]);
        if (across[1] < 0.0) {
            across = scale(across, -1.0);
        }
        double w = RIBBON_WIDTH * (1.0 + 0.25 * Math.max(0.0, (s - 0.65) / 0.35)) / 2.0;
        double[] mid = {cx + radial[0] * radius, y, radial[2] * radius};
        double[] in = scale(radial, -RIBBON_THICK);
        double[] o0 = add(mid, scale(across, -w));
        double[] o1 = add(mid, scale(across, w));
        return new double[][][] {{o0, o1}, {add(o0, in), add(o1, in)}};
    }

    /** 4.1.47: a vein from {@code a} to {@code b}, {@code width} across, lying on the face it was put on (just off it). */
    static void crack(Sink sink, double cx, double[] a, double[] b, double width, int vein) {
        double[] d = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double[] n = axis((a[0] + b[0]) / 2.0 - cx, 0.0, (a[2] + b[2]) / 2.0);
        double[] side = axis(d[1] * n[2] - d[2] * n[1], d[2] * n[0] - d[0] * n[2], d[0] * n[1] - d[1] * n[0]);
        double[] h = scale(side, width / 2.0);
        int c = 0xFF000000 | vein;
        sink.quad(add(a, h), add(b, scale(h, 0.6)), add(b, scale(h, -0.6)), add(a, scale(h, -1.0)), n, c, c, c, c);
    }

    /** 4.1.47: how many lengths the blade is drawn in, root to point. */
    static final int SECTIONS = 30;
    /** 4.1.47: where the blade grows out of the forearm (model pixels down the arm), inside the ribbons' flare. */
    static final double ROOT = 5.5;

    /**
     * 4.1.47: the blade, {@code out} of the way grown: root to point, a wedge across (the edge in front, the spine behind and thickest),
     * the edge a long convex sweep (widening toward the far end, then curving up to the point on the spine's line), the spine curving back a
     * little; lighter toward the edge, the gleam glowing down the edge, veins at its root where the ribbons flow into it.
     */
    static void blade(Sink sink, double cx, double half, double out, int body, int deep, int light, int vein, int gleam) {
        double length = (BladeArm.BLADE_LENGTH + (10.0 - ROOT)) * out;
        double grow = 0.4 + 0.6 * out;
        double[][][] ring = new double[SECTIONS + 1][][];
        for (int i = 0; i <= SECTIONS; i++) {
            double u = (double)i / SECTIONS;
            double y = ROOT + length * u;
            double back = spineAt(u, half) * grow;
            double front = edgeAt(u, half, back / grow) * grow;
            double thick = (1.0 + 0.55 * (1.0 - u)) * Math.min(1.0, (1.0 - u) * 14.0 + 0.05) * grow;
            double mid = front + (back - front) * 0.42;
            ring[i] = new double[][] {
                {cx, y, front},                          // 0: the edge
                {cx + thick * 0.62, y, mid},             // 1: the face's swell, this side
                {cx + thick * 0.5, y, back - 0.25},      // 2: the spine's corner, this side
                {cx - thick * 0.5, y, back - 0.25},      // 3: the spine's corner, the other
                {cx - thick * 0.62, y, mid}};            // 4: the face's swell, the other side
        }
        int nearEdge = 0xFF000000 | mix(light, 0xFFFFFF, 0.18);
        int face = 0xFF000000 | mix(body, light, 0.4);
        int toSpine = 0xFF000000 | body;
        int spine = 0xFF000000 | mix(body, deep, 0.6);
        for (int i = 0; i < SECTIONS; i++) {
            double[][] a = ring[i];
            double[][] b = ring[i + 1];
            double[] centre = {cx, (a[0][1] + b[0][1]) / 2.0, (a[0][2] + a[2][2] + b[0][2] + b[2][2]) / 4.0};
            int[][] colours = {{nearEdge, face}, {face, toSpine}, {spine, spine}, {toSpine, face}, {face, nearEdge}};
            for (int k = 0; k < 5; k++) {
                int k2 = (k + 1) % 5;
                double[] n = outward(new double[][] {a[k], a[k2], b[k2], b[k]}, centre);
                sink.quad(a[k], a[k2], b[k2], b[k], n, colours[k][0], colours[k][1], colours[k][1], colours[k][0]);
            }
            // The gleam down the edge, each side: a glowing band from the edge a little way up its face.
            for (int side = 0; side < 2; side++) {
                int top = side == 0 ? 1 : 4;
                double lift = side == 0 ? 0.04 : -0.04;
                double[] ea = a[0];
                double[] eb = b[0];
                double[] fa = lerp(a[0], a[top], 0.16);
                double[] fb = lerp(b[0], b[top], 0.16);
                sink.quad(new double[] {ea[0] + lift * 0.2, ea[1], ea[2]}, new double[] {fa[0] + lift, fa[1], fa[2]},
                    new double[] {fb[0] + lift, fb[1], fb[2]}, new double[] {eb[0] + lift * 0.2, eb[1], eb[2]}, GLOW, gleam, gleam, gleam, gleam);
            }
        }
        // Veins at its root, where the ribbons flow into it, running out along its face.
        java.util.Random random = new java.util.Random(4147L);
        for (int side = 0; side < 2; side++) {
            for (int c = 0; c < 4; c++) {
                double u0 = 0.02 + 0.05 * c;
                double u1 = u0 + 0.12 + 0.1 * random.nextDouble();
                double v0 = 0.35 + 0.4 * random.nextDouble();
                double v1 = Math.max(0.15, Math.min(0.85, v0 + 0.3 * (random.nextDouble() - 0.5)));
                double[] p0 = onBlade(ring, side, u0, v0);
                double[] p1 = onBlade(ring, side, (u0 + u1) / 2.0, (v0 + v1) / 2.0 + 0.08 * (random.nextDouble() - 0.5));
                double[] p2 = onBlade(ring, side, u1, v1);
                bladeCrack(sink, p0, p1, side, 0.34, vein);
                bladeCrack(sink, p1, p2, side, 0.22, vein);
            }
        }
    }

    /** 4.1.47: how far the spine is behind the arm's middle {@code u} along the blade: behind the arm, curving back a little toward the point. */
    static double spineAt(double u, double half) {
        double back = half + 0.9 + 1.4 * u * u;
        // Over its last stretch the spine turns in toward the edge a little, so the two meet in a point.
        return u <= 0.84 ? back : back - 1.6 * Math.pow((u - 0.84) / 0.16, 2.0);
    }

    /**
     * 4.1.47: where the edge is ({@code back}: the spine there): in front of the arm at the root, sweeping forward (widening) toward the far
     * end, then curving round to meet the spine's line at the point (a convex sweep, no corner).
     */
    static double edgeAt(double u, double half, double back) {
        double widest = -(half + 4.6);
        double root = -(half + 1.2);
        if (u <= 0.74) {
            double t = u / 0.74;
            return root + (widest - root) * (t * (2.0 - t));
        }
        // Sweeping up ever faster to meet the spine: a sharp point, not a rounded end.
        double t = (u - 0.74) / 0.26;
        double end = back - 0.3;
        return widest + (end - widest) * Math.pow(t, 1.7);
    }

    /** 4.1.47: a point on the blade's face ({@code side} 0 this, 1 the other), {@code u} along it, {@code v} from the edge (0) to the spine (1). */
    static double[] onBlade(double[][][] ring, int side, double u, double v) {
        double at = u * (ring.length - 1);
        int i = Math.min(ring.length - 2, (int)Math.floor(at));
        double f = at - i;
        int swell = side == 0 ? 1 : 4;
        int corner = side == 0 ? 2 : 3;
        double[] a = v < 0.42 ? lerp(ring[i][0], ring[i][swell], v / 0.42) : lerp(ring[i][swell], ring[i][corner], (v - 0.42) / 0.58);
        double[] b = v < 0.42 ? lerp(ring[i + 1][0], ring[i + 1][swell], v / 0.42) : lerp(ring[i + 1][swell], ring[i + 1][corner], (v - 0.42) / 0.58);
        double[] p = lerp(a, b, f);
        return new double[] {p[0] + (side == 0 ? 0.05 : -0.05), p[1], p[2]};
    }

    /** 4.1.47: a vein on the blade's face from {@code a} to {@code b} ({@code side}: which face). */
    static void bladeCrack(Sink sink, double[] a, double[] b, int side, double width, int vein) {
        double[] n = {side == 0 ? 1.0 : -1.0, 0.0, 0.0};
        double[] d = axis(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        double[] across = axis(d[1] * n[2] - d[2] * n[1], d[2] * n[0] - d[0] * n[2], d[0] * n[1] - d[1] * n[0]);
        double[] h = scale(across, width / 2.0);
        int c = 0xFF000000 | vein;
        sink.quad(add(a, h), add(b, scale(h, 0.6)), add(b, scale(h, -0.6)), add(a, scale(h, -1.0)), n, c, c, c, c);
    }

    /** 4.1.47: the veins' colour: a deep red, unless the kagune is red itself (then darker than it, as cracks). */
    static int veins(int colour) {
        int r = colour >> 16 & 255;
        int g = colour >> 8 & 255;
        int b = colour & 255;
        boolean red = r > 110 && r > g * 1.6 && r > b * 1.6;
        return red ? shade(colour, 0.38) : 0xC4142C;
    }

    /** 4.1.47: as {@link #outward}, but facing in toward {@code inside} (a ribbon's underside). */
    static double[] inward(double[][] corners, double[] inside) {
        double[] n = outward(corners, inside);
        double[] flipped = scale(n, -1.0);
        return flipped[1] > 0.9999 ? axis(0.01, 1.0, 0.0) : flipped;
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
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
