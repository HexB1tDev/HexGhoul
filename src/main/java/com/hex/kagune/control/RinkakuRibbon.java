package com.hex.kagune.control;

/**
 * 2.0.39 (pure, tested): a Rinkaku tail's shape and colours as the owner's reference picture has it (drawn by
 * {@code client.render.RinkakuTube}): a broad, flat ribbon ({@link #WIDE} across its faces, {@link #THIN} through them, of its thickness),
 * swelling a little past the back then tapering to a point ({@link #profile}), crimson at the middle of each face darkening to deep maroon
 * at its edges, with dark smudges along it ({@link #shade}), and its markings' light colour; 2.0.39 (the owner: "we gotta adapt the
 * texture to the different colors"): every one of those the reference palette turned to the tail's own kagune colour ({@link KaguneTint}),
 * so a blue kagune is a blue ribbon with pale blue marks, the original crimson exactly the picture's.
 */
public final class RinkakuRibbon {
    private RinkakuRibbon() {
    }

    /** The reference palette (the original's crimson): the middle of a face, its deep edges, the light of the stripe and the marks. */
    public static final int HEART = 0xFFC81634; // (not bright enough to count as a glow: a black kagune's ribbon is black, KaguneTint)
    public static final int DEEP = 0xFF5C0816;
    public static final int LIGHT = 0xFFE47676;
    /** Its cross-section against its thickness: this wide across its faces, this thin through them. */
    public static final double WIDE = 1.35;
    public static final double THIN = 0.62;
    /** Round it, and along it: how many sides, and a ring every so far (blocks), at most this many. */
    public static final int SIDES = 18;
    public static final double STEP = 0.09;
    public static final int MOST_RINGS = 80;
    /** One tile of the markings every so far along it (blocks); one tile across each broad face (two round it). */
    public static final double TILE = 0.55;
    /** The markings stand this much outside the ribbon (of its size), so they never flicker into it. */
    public static final double OVER = 1.018;

    /**
     * A tail's mesh: each ring's points round it ({@link #SIDES} + 1, the last the first again) and their outward normals, each point's
     * colour (ARGB), how far along each ring is (blocks from the root), each ring's middle, and its markings' light colour; 2.0.41: each
     * ring's way across its faces and through them and its half-sizes those ways (for the veins on it), how clear its markings are at each
     * point (seen edge-on, they fade), and its glow's colour.
     */
    public record Mesh(double[][][] at, double[][][] way, int[][] tint, double[] along, double[][] centre, int light, double[][] wide,
                       double[][] thin, double[] half, double[] deep, int[][] marks, int glow) {
    }

    /**
     * 2.0.41 (the owner: "improve the kagune texture and material to be the same quality and beauty of ukaku, veins inside and etc"): the
     * rest of the palette: the glossy rim it shows toward its silhouette, the wet glint where the light catches it, and the RC glow of its
     * veins and its root.
     */
    public static final int RIM = 0xFFEE8C92;
    public static final int SHEEN = 0xFFFFDCE0;
    public static final int GLOW = 0xFFFF2A3E;
    /** The light it glints with (from above, a little behind and to the side), and how sharp the glint. */
    static final double[] SKY = normalize(new double[] {0.35, 0.88, 0.3});
    static final double GLINT = 22.0;
    /** How see-through its skin is at the most (seen edge-on): enough that the veins on its far side show faintly through. */
    public static final double CLEAREST = 0.78;
    /** Its veins: how many run along it, how far out from its middle (of its size: just over its skin, under its markings), how wide. */
    public static final int VEINS = 6;
    public static final double VEIN_LIFT = 1.009;
    public static final double VEIN_CORE = 0.011;
    public static final double VEIN_SOFT = 0.04;
    /** 2.0.50 (the owner: "More veins, and more glow"): the wide faint aura round the main veins and the spines, and its alpha. */
    public static final double VEIN_AURA = 0.085;
    public static final double AURA_ALPHA = 0.13;

    /** One quad of a vein: its corners and its colours (ARGB, glowing: drawn at full brightness). */
    public record Glow(double[][] corners, int[] argb) {
    }

    /**
     * Its colour at a point: {@code base} (its heart to its deep edge, already shaded) {@code t} along it (0 root .. 1 point), the hotter
     * root and the darker point; seen {@code facing} (1 face on .. 0 edge-on, or negative: not known) the glossy rim toward its silhouette
     * and {@code glint} of the light's sheen; and how clear its skin is there. ARGB.
     */
    public static int surface(int base, int[] colours, double t, double facing, double glint) {
        int c = mix(base, colours[4], 0.55 * (1.0 - smooth(t / 0.22))); // 2.0.50: a hotter root, glowing further out
        c = mix(c, colours[1], 0.4 * smooth((t - 0.78) / 0.22));
        double alpha = 1.0;
        if (facing >= 0.0) {
            c = mix(c, colours[3], 0.55 * (1.0 - smooth(facing / 0.5)));
            c = mix(c, colours[5], 0.5 * Math.max(0.0, Math.min(1.0, glint)));
            alpha = CLEAREST + (1.0 - CLEAREST) * smooth(facing / 0.45);
        }
        return (int)Math.round(alpha * 255.0) << 24 | (c & 0xFFFFFF);
    }

    /** How clear a mark is at a point seen {@code facing} (marks seen edge-on fade, never shimmering at the silhouette): 0..255. */
    public static int markAlpha(double facing) {
        return facing < 0.0 ? 255 : (int)Math.round(255.0 * smooth((facing - 0.08) / 0.3));
    }

    /**
     * The tail's veins at {@code time} (ticks): {@link #VEINS} glowing lines wandering along it just over its skin round to every side of
     * it (the far ones show through its clear skin), each with a fork, a soft glow round a bright core, thin and fading at both ends, a
     * pulse of RC flowing out along them from the root to the point.
     */
    public static java.util.List<Glow> veins(Mesh mesh, int seed, double time) {
        java.util.List<Glow> out = new java.util.ArrayList<>();
        int rings = mesh.at().length;
        double total = Math.max(1.0E-6, mesh.along()[rings - 1]);
        for (int v = 0; v < VEINS; v++) {
            double start = 2.0 * Math.PI * v / VEINS + 0.4 + (seed % 7) * 0.13;
            vein(out, mesh, seed * 13 + v, start, 0.03, 0.92 - 0.04 * (v % 3), 0.0, total, time, true);
            double fork = 0.22 + 0.1 * v;
            vein(out, mesh, seed * 13 + v + 50, start, fork, Math.min(0.9, fork + 0.34), v % 2 == 0 ? 1.1 : -1.1, total, time, false);
            if (v % 2 == 0) {
                // 2.0.50: half of them fork again further out.
                double second = Math.min(0.75, fork + 0.24);
                vein(out, mesh, seed * 13 + v + 90, start, second, Math.min(0.93, second + 0.22), v % 4 == 0 ? -1.3 : 1.3, total, time, false);
            }
        }
        // 2.0.50: a glowing spine down the middle of each broad face, wandering little (the light from inside it).
        for (int face = 0; face < 2; face++) {
            vein(out, mesh, seed * 13 + 200 + face, face == 0 ? Math.PI / 2.0 : -Math.PI / 2.0, 0.02, 0.95, 0.0, total, time, true);
        }
        return out;
    }

    private static void vein(java.util.List<Glow> out, Mesh mesh, int seed, double start, double from, double to, double veer, double total,
                             double time, boolean aura) {
        int rings = mesh.at().length;
        int steps = Math.max(3, (int)Math.round((to - from) * (rings - 1) * 1.2));
        double[][] previous = null;
        int[] previousColour = null;
        for (int step = 0; step <= steps; step++) {
            double t = from + (to - from) * step / steps;
            double x = t * (rings - 1);
            int k = Math.min(rings - 2, (int)Math.floor(x));
            double f = x - k;
            double[] centre = lerp(mesh.centre()[k], mesh.centre()[k + 1], f);
            double[] w = lerp(mesh.wide()[k], mesh.wide()[k + 1], f);
            double[] n = lerp(mesh.thin()[k], mesh.thin()[k + 1], f);
            double half = mesh.half()[k] + (mesh.half()[k + 1] - mesh.half()[k]) * f;
            double deep = mesh.deep()[k] + (mesh.deep()[k + 1] - mesh.deep()[k]) * f;
            double along = (t - from) / Math.max(1.0E-6, to - from);
            double s = t * total;
            // The spines (straight down a broad face's middle) wander little; the others more.
            double wander = Math.abs(Math.abs(start) - Math.PI / 2.0) < 1.0E-9 && veer == 0.0 ? 0.12 : 0.45;
            double theta = start + veer * along + wander * Math.sin(s * 4.5 + seed * 1.3) + 0.1 * Math.sin(s * 13.0 + seed);
            double cw = Math.cos(theta);
            double sn = Math.sin(theta);
            double[] p = {centre[0] + (w[0] * cw * half + n[0] * sn * deep) * VEIN_LIFT, centre[1] + (w[1] * cw * half + n[1] * sn * deep) * VEIN_LIFT,
                centre[2] + (w[2] * cw * half + n[2] * sn * deep) * VEIN_LIFT};
            double[] across = normalize(new double[] {-w[0] * sn * half + n[0] * cw * deep, -w[1] * sn * half + n[1] * cw * deep,
                -w[2] * sn * half + n[2] * cw * deep});
            double fade = smooth(along / 0.12) * (1.0 - smooth((along - 0.8) / 0.2)) * (veer != 0.0 ? 0.8 : 1.0);
            // A pulse of RC flowing out from the root: a bright band travelling along every vein (2.0.50: brighter).
            double pulse = 0.72 + 0.5 * Math.pow(Math.max(0.0, Math.sin(s * 3.2 - time * 0.42)), 3.0);
            double size = Math.max(0.25, Math.min(1.0, half * 6.0));
            double soft = VEIN_SOFT * size * (0.4 + 0.6 * fade);
            double core = VEIN_CORE * size * (0.4 + 0.6 * fade);
            double wide = VEIN_AURA * size * (0.4 + 0.6 * fade);
            double[][] now = {add(p, across, soft), add(p, across, core), add(p, across, -core), add(p, across, -soft), add(p, across, wide),
                add(p, across, -wide)};
            int glow = mesh.glow();
            int[] colour = {argb(glow, Math.min(1.0, 0.34 * fade * pulse)), argb(mix(glow, 0xFFFFFFFF, Math.min(1.0, 0.3 * pulse)),
                Math.min(1.0, 0.95 * fade * pulse)), argb(glow, Math.min(1.0, AURA_ALPHA * fade * pulse))};
            if (previous != null) {
                if (aura) {
                    // 2.0.50: the wide faint aura first, under the rest.
                    out.add(new Glow(new double[][] {previous[4], now[4], now[5], previous[5]},
                        new int[] {previousColour[2], colour[2], colour[2], previousColour[2]}));
                }
                // The soft glow either side, then the bright core over it.
                out.add(new Glow(new double[][] {previous[0], now[0], now[3], previous[3]},
                    new int[] {previousColour[0], colour[0], colour[0], previousColour[0]}));
                out.add(new Glow(new double[][] {previous[1], now[1], now[2], previous[2]},
                    new int[] {previousColour[1], colour[1], colour[1], previousColour[1]}));
            }
            previous = now;
            previousColour = colour;
        }
    }

    static int argb(int rgb, double alpha) {
        return (int)Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0) << 24 | (rgb & 0xFFFFFF);
    }

    static double smooth(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double[] add(double[] p, double[] d, double k) {
        return new double[] {p[0] + d[0] * k, p[1] + d[1] * k, p[2] + d[2] * k};
    }

    static double[] normalize(double[] v) {
        return unit(v, new double[] {0.0, 1.0, 0.0});
    }

    /**
     * 2.0.50 (the owner: "Make the rinkaku shape more thin closer to the emergence area", with a drawing: a leaf, a point where it leaves
     * the body, broadest near its far end): how wide it is {@code t} along it (0 its root .. 1 its end), of its thickness: {@link #ROOT_WIDTH}
     * of its broadest where it leaves the body, widening all the way to {@link #BROADEST} along, then a short rounded end.
     */
    public static double profile(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        if (c <= BROADEST) {
            return PEAK * (ROOT_WIDTH + (1.0 - ROOT_WIDTH) * Math.pow(c / BROADEST, 0.9));
        }
        double x = (c - BROADEST) / (1.0 - BROADEST);
        return PEAK * Math.sqrt(Math.max(0.0, 1.0 - x * x));
    }

    /** 2.0.50: where it is broadest (of its length), how broad (of its thickness), and how much of that it has at its root. */
    public static final double BROADEST = 0.78;
    public static final double PEAK = 1.15;
    public static final double ROOT_WIDTH = 0.25;

    /**
     * How dark it is at {@code across} (-1 one edge .. 1 the other, through the middle of a face), on its {@code back} face or not,
     * {@code along} blocks from its root ({@code seed}: where its smudges fall): 0 its heart's colour .. 1 its deep edge's.
     */
    public static double shade(double across, boolean back, double along, int seed) {
        double edge = Math.pow(Math.min(1.0, Math.abs(across)), 2.2);
        double smudge = Math.max(0.0, Math.sin(along * 4.3 + seed * 0.37) * Math.sin(along * 1.7 + across * 2.0 + seed * 0.11));
        return Math.min(1.0, edge * 0.85 + smudge * 0.35 + (back ? 0.15 : 0.0));
    }

    /** The palette of a tail of kagune colour {@code colour} (0xRRGGBB): {heart, deep, light, rim, glow, sheen}. */
    public static int[] palette(int colour) {
        int[] out = {KaguneTint.recolour(HEART, colour), KaguneTint.recolour(DEEP, colour), KaguneTint.recolour(LIGHT, colour),
            KaguneTint.recolour(RIM, colour), KaguneTint.recolour(GLOW, colour), KaguneTint.recolour(SHEEN, colour)};
        if (KaguneColours.toHsv(colour & 0xFFFFFF)[2] < 0.3) {
            out[2] = 0xFF6E686C; // a black kagune: its stripe and light marks smoke grey, so they still show on it
        }
        return out;
    }

    /** The markings' u at side {@code k} (one tile across each broad face) and v {@code along} blocks from the root. */
    public static float u(int k) {
        return (float)(2.0 * k / SIDES);
    }

    public static float v(double along) {
        return (float)(along / TILE);
    }

    /**
     * The mesh of a tail along {@code points} (root first, any frame), {@code thickness} its radius, {@code colour} its kagune colour; null
     * if too short to draw. Its frame is carried along it without twisting: its thin way starts level (across its owner's back, its broad
     * faces upright), so a tail spread out to the side shows its broad face, as in the picture.
     */
    public static Mesh build(double[][] points, double thickness, int colour, int seed) {
        return build(points, thickness, colour, seed, null);
    }

    /** 2.0.41: the same, seen from {@code eye} (the same frame as the points; null: unknown): its rim, glint and clearness by it. */
    public static Mesh build(double[][] points, double thickness, int colour, int seed, double[] eye) {
        if (points == null || points.length < 2 || !(thickness > 0.0)) {
            return null;
        }
        double[] arc = new double[points.length];
        for (int i = 1; i < points.length; i++) {
            arc[i] = arc[i - 1] + Math.sqrt(sq(points[i][0] - points[i - 1][0]) + sq(points[i][1] - points[i - 1][1]) + sq(points[i][2] - points[i - 1][2]));
        }
        double total = arc[points.length - 1];
        if (!(total > 1.0E-4)) {
            return null;
        }
        int rings = Math.max(4, Math.min(MOST_RINGS, (int)Math.ceil(total / STEP) + 1));
        double[][] centre = new double[rings][];
        double[] along = new double[rings];
        int j = 0;
        for (int r = 0; r < rings; r++) {
            double want = total * r / (rings - 1);
            while (j < points.length - 2 && arc[j + 1] < want) {
                j++;
            }
            double span = arc[j + 1] - arc[j];
            double f = span < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (want - arc[j]) / span));
            double[] a = points[j];
            double[] b = points[j + 1];
            centre[r] = new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
            along[r] = want;
        }
        double[][] tangent = new double[rings][];
        for (int r = 0; r < rings; r++) {
            double[] p = centre[Math.max(0, r - 1)];
            double[] q = centre[Math.min(rings - 1, r + 1)];
            tangent[r] = unit(new double[] {q[0] - p[0], q[1] - p[1], q[2] - p[2]}, new double[] {0.0, 0.0, 1.0});
        }
        double[][] thin = new double[rings][];
        thin[0] = unit(cross(new double[] {0.0, 1.0, 0.0}, tangent[0]), perpendicular(tangent[0]));
        for (int r = 1; r < rings; r++) {
            double[] n = thin[r - 1];
            double d = dot(n, tangent[r]);
            thin[r] = unit(new double[] {n[0] - tangent[r][0] * d, n[1] - tangent[r][1] * d, n[2] - tangent[r][2] * d}, perpendicular(tangent[r]));
        }
        int[] colours = palette(colour);
        double[][][] at = new double[rings][SIDES + 1][];
        double[][][] way = new double[rings][SIDES + 1][];
        int[][] tint = new int[rings][SIDES + 1];
        int[][] marks = new int[rings][SIDES + 1];
        double[][] wide = new double[rings][];
        double[] halfs = new double[rings];
        double[] deeps = new double[rings];
        for (int r = 0; r < rings; r++) {
            double[] n = thin[r];
            double[] w = cross(tangent[r], n); // across its faces
            double size = thickness * profile(r / (double)(rings - 1));
            double a = size * WIDE;
            double c = size * THIN;
            wide[r] = w;
            halfs[r] = a;
            deeps[r] = c;
            for (int k = 0; k <= SIDES; k++) {
                double phi = 2.0 * Math.PI * (k % SIDES) / SIDES;
                double cw = Math.cos(phi);
                double sn = Math.sin(phi);
                at[r][k] = new double[] {centre[r][0] + w[0] * a * cw + n[0] * c * sn, centre[r][1] + w[1] * a * cw + n[1] * c * sn,
                    centre[r][2] + w[2] * a * cw + n[2] * c * sn};
                way[r][k] = unit(new double[] {w[0] * c * cw + n[0] * a * sn, w[1] * c * cw + n[1] * a * sn, w[2] * c * cw + n[2] * a * sn}, n);
                double facing = -1.0;
                double glint = 0.0;
                if (eye != null) {
                    double[] p = at[r][k];
                    double[] view = unit(new double[] {eye[0] - p[0], eye[1] - p[1], eye[2] - p[2]}, way[r][k]);
                    double[] nn = way[r][k];
                    facing = Math.min(1.0, Math.abs(dot(nn, view)));
                    double d = 2.0 * dot(SKY, nn);
                    double[] mirror = {d * nn[0] - SKY[0], d * nn[1] - SKY[1], d * nn[2] - SKY[2]};
                    glint = Math.pow(Math.max(0.0, dot(mirror, view)), GLINT);
                }
                tint[r][k] = surface(mix(colours[0], colours[1], shade(cw, sn < 0.0, along[r], seed)), colours, r / (double)(rings - 1),
                    facing, glint);
                marks[r][k] = markAlpha(facing);
            }
        }
        return new Mesh(at, way, tint, along, centre, colours[2], wide, thin, halfs, deeps, marks, colours[4]);
    }

    /** Its markings' points: each of the mesh's a hair further out from its ring's middle ({@link #OVER}). */
    public static double[][][] over(Mesh mesh) {
        double[][][] out = new double[mesh.at().length][][];
        for (int r = 0; r < out.length; r++) {
            double[] c = mesh.centre()[r];
            out[r] = new double[mesh.at()[r].length][];
            for (int k = 0; k < out[r].length; k++) {
                double[] p = mesh.at()[r][k];
                out[r][k] = new double[] {c[0] + (p[0] - c[0]) * OVER, c[1] + (p[1] - c[1]) * OVER, c[2] + (p[2] - c[2]) * OVER};
            }
        }
        return out;
    }

    public static int mix(int a, int b, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int)Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * k);
        int g = (int)Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * k);
        int bl = (int)Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * k);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    static double sq(double v) {
        return v * v;
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] unit(double[] v, double[] otherwise) {
        double length = Math.sqrt(dot(v, v));
        return length < 1.0E-9 ? otherwise : new double[] {v[0] / length, v[1] / length, v[2] / length};
    }

    static double[] perpendicular(double[] t) {
        return unit(cross(Math.abs(t[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0}, t), new double[] {1.0, 0.0, 0.0});
    }
}
