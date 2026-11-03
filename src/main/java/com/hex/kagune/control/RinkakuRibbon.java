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
     * colour (ARGB), how far along each ring is (blocks from the root), each ring's middle, and its markings' light colour.
     */
    public record Mesh(double[][][] at, double[][][] way, int[][] tint, double[] along, double[][] centre, int light) {
    }

    /** Its width (of its widest) {@code t} along it (0 its root .. 1 its point): a swell past the back, a long taper to a point. */
    public static double profile(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        double swell = 0.84 + 0.22 * Math.sin(Math.PI * Math.min(1.0, c / 0.55) * 0.5);
        double taper = c < 0.5 ? 1.0 : 1.0 - Math.pow((c - 0.5) / 0.5, 1.7);
        return Math.max(0.0, swell * taper);
    }

    /**
     * How dark it is at {@code across} (-1 one edge .. 1 the other, through the middle of a face), on its {@code back} face or not,
     * {@code along} blocks from its root ({@code seed}: where its smudges fall): 0 its heart's colour .. 1 its deep edge's.
     */
    public static double shade(double across, boolean back, double along, int seed) {
        double edge = Math.pow(Math.min(1.0, Math.abs(across)), 2.2);
        double smudge = Math.max(0.0, Math.sin(along * 4.3 + seed * 0.37) * Math.sin(along * 1.7 + across * 2.0 + seed * 0.11));
        return Math.min(1.0, edge * 0.85 + smudge * 0.35 + (back ? 0.15 : 0.0));
    }

    /** The palette of a tail of kagune colour {@code colour} (0xRRGGBB): {heart, deep, light}. */
    public static int[] palette(int colour) {
        int[] out = {KaguneTint.recolour(HEART, colour), KaguneTint.recolour(DEEP, colour), KaguneTint.recolour(LIGHT, colour)};
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
        for (int r = 0; r < rings; r++) {
            double[] n = thin[r];
            double[] w = cross(tangent[r], n); // across its faces
            double size = thickness * profile(r / (double)(rings - 1));
            double a = size * WIDE;
            double c = size * THIN;
            for (int k = 0; k <= SIDES; k++) {
                double phi = 2.0 * Math.PI * (k % SIDES) / SIDES;
                double cw = Math.cos(phi);
                double sn = Math.sin(phi);
                at[r][k] = new double[] {centre[r][0] + w[0] * a * cw + n[0] * c * sn, centre[r][1] + w[1] * a * cw + n[1] * c * sn,
                    centre[r][2] + w[2] * a * cw + n[2] * c * sn};
                way[r][k] = unit(new double[] {w[0] * c * cw + n[0] * a * sn, w[1] * c * cw + n[1] * a * sn, w[2] * c * cw + n[2] * a * sn}, n);
                tint[r][k] = mix(colours[0], colours[1], shade(cw, sn < 0.0, along[r], seed));
            }
        }
        return new Mesh(at, way, tint, along, centre, colours[2]);
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
