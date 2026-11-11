package com.hex.kagune.control.quinque;

/**
 * 4.1.29 (the owner: "a new weapon for the Reaper based on the images that I'm going to also send you ... It is a heavy weapon, dual-handed,
 * that deals massive damage and, yeah, just causes bleed whenever hit"): the White Reaper's second-phase blade, grown smooth (pure), after
 * the owner's art/reaper/Reaper_Quinque1.jpg and Reaper_quinque2.jpg: a long straight silver blade cracked with red veins, its point on the
 * edge's line; an organic silver guard swelling round its foot, a great red gem set in each face and small ones round it, spines along its
 * back and a comb of ribs under it; a long ribbed grip for both hands; a ring the rod runs through; a horn sweeping back from the guard along
 * the grip; the rod's end tipped red.
 *
 * <p>Its own frame, in pixels: u along it (the right fist's middle at 0, the point at {@link #TIP}), v across its flat toward its edge, w
 * through its flat. Drawn by the renderer in the right hand ({@code client.render.ReaperBladeLayer}), posed by
 * {@code client.animation.ReaperBladePose}; NPC only: no item, nothing to drop.
 */
public final class ReaperBladeMesh {
    private ReaperBladeMesh() {
    }

    /** Where the left hand holds (u: the grip behind the right fist), where the guard begins, the blade begins, its point. */
    public static final double LEFT_HAND = -4.2;
    static final double GUARD_FROM = 1.9;
    static final double BLADE_FROM = 9.6;
    public static final double TIP = 36.0;
    /** The grip's far end, the ring's middle, the rod's red tip. */
    static final double GRIP_TO = -9.0;
    static final double RING = -9.9;
    public static final double BUTT = -13.6;

    static final int SILVER = 0xFFC9CBCE;
    static final int SILVER_LIGHT = 0xFFE8E9EB;
    static final int SILVER_DARK = 0xFF8C8F94;
    static final int STEEL = 0xFF6E7176;
    static final int VEIN = 0xFF9A0C16;
    static final int VEIN_DARK = 0xFF5C060C;
    static final int GEM = 0xFFD2121E;
    static final int GEM_DARK = 0xFF7A0810;
    static final int INSET = 0xFF3A3C40;

    private static ArmMesh.Mesh built;

    /** 4.1.31: where the blade snaps (u): a hand's width past the guard. */
    public static final double SNAPS = BLADE_FROM + 3.6;

    private static ArmMesh.Mesh broken;

    /** 4.1.31 (the owner: "The blade shatters at the end, and it's just broken"): what is left in his hand: the hilt and a jagged stub. */
    public static synchronized ArmMesh.Mesh broken() {
        if (broken == null) {
            ArmMesh.Builder b = new ArmMesh.Builder(p -> p.clone(), n -> n.clone());
            blade(b, SNAPS);
            veins(b, SNAPS);
            guard(b);
            grip(b);
            broken = b.done();
        }
        return broken;
    }

    /** Its mesh, built the first time it is asked for. */
    public static synchronized ArmMesh.Mesh of() {
        if (built == null) {
            ArmMesh.Builder b = new ArmMesh.Builder(p -> p.clone(), n -> n.clone());
            blade(b);
            guard(b);
            grip(b);
            built = b.done();
        }
        return built;
    }

    /** A point of its frame (u, v, w). */
    static double[] at(double u, double v, double w) {
        return new double[] {u, v, w};
    }

    // ---------------------------------------------------------------- the blade

    /** The edge's side (v) at {@code u}: a straight line to the point. */
    public static double edge(double u) {
        return 2.2 - 0.55 * (u - BLADE_FROM) / (TIP - BLADE_FROM);
    }

    /** The back's side (v, negative) at {@code u}: straight, then curving down onto the edge's line at the point. */
    public static double spine(double u) {
        double straight = -2.5 + 0.75 * (u - BLADE_FROM) / (TIP - BLADE_FROM);
        double from = TIP - 6.5;
        if (u <= from) {
            return straight;
        }
        double t = Math.min(1.0, (u - from) / (TIP - from));
        // A clipped point: the back curves down to meet the edge's line.
        double curve = 1.0 - Math.sqrt(Math.max(0.0, 1.0 - t * t));
        return straight + (edge(TIP) - straight) * curve;
    }

    /** Its thickness (half, w) at {@code u}: thick at its foot, thinning to the point. */
    static double thick(double u) {
        double t = Math.max(0.0, Math.min(1.0, (u - BLADE_FROM) / (TIP - BLADE_FROM)));
        return 0.46 - 0.30 * t;
    }

    /**
     * The blade: a flat back, two faces bevelled to an edge, each face's ridge a third of the way from its back; silver, a little brighter
     * along the bevel. Then its red veins over both faces.
     */
    static void blade(ArmMesh.Builder b) {
        blade(b, TIP);
        veins(b, TIP);
    }

    /**
     * The blade up to {@code end} (its point: {@link #TIP}; less: broken off there, its end jagged across and closed, 4.1.31). Its veins
     * separately ({@link #veins}).
     */
    static void blade(ArmMesh.Builder b, double end) {
        boolean broken = end < TIP - 0.5;
        int rows = broken ? 20 : 72;
        // Round the section: back (two corners), face down to the ridge, the bevel to the edge, the other side back.
        double[][] section = {{0.0, 1.0}, {0.18, 1.0}, {0.36, 1.0}, {0.52, 0.86}, {0.68, 0.6}, {0.84, 0.32}, {1.0, 0.0}, {0.84, -0.32},
            {0.68, -0.6}, {0.52, -0.86}, {0.36, -1.0}, {0.18, -1.0}, {0.0, -1.0}, {0.0, 0.0}};
        int cols = section.length;
        double[][][] g = new double[rows][cols][];
        int[][] c = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            double t = i / (rows - 1.0);
            double u0 = BLADE_FROM - 0.6 + (end - BLADE_FROM + 0.6) * t;
            double h0 = thick(u0) * (u0 > TIP - 0.4 ? Math.max(0.05, (TIP - u0) / 0.4) : 1.0);
            for (int j = 0; j < cols; j++) {
                double f = section[j][0];
                // Broken: its last row jagged across, as it snapped.
                double u = broken && i == rows - 1 ? u0 + 0.9 * Math.sin(f * 9.0 + 0.7) - 0.6 * Math.cos(f * 23.0) : u0;
                double s = spine(u);
                double e = edge(u);
                double h = broken ? thick(u) : h0;
                double v = s + (e - s) * f;
                double w = h * section[j][1] * (f >= 0.999 ? 0.0 : 1.0);
                g[i][j] = at(u, v, j == cols - 1 ? 0.0 : w);
                if (j == cols - 1) {
                    g[i][j] = at(u, s - 0.02, 0.0); // the back's middle, closing the section
                }
                boolean bevel = f > 0.52;
                c[i][j] = bevel ? ArmMesh.mix(SILVER, SILVER_LIGHT, 0.55 + 0.25 * f) : ArmMesh.mix(SILVER, SILVER_DARK, 0.18 + 0.1 * Math.sin(u * 0.7));
            }
        }
        b.grid(g, c, true, false);
        if (broken) {
            b.cap(at(end, (spine(end) + edge(end)) * 0.5, 0.0), g[rows - 1], SILVER_DARK, false, false);
        }
    }

    /**
     * Its red veins: cracks branching over both faces from its foot toward the point, thick near the guard and thinner and fewer toward the
     * point (a fixed seed: the same blade every time), laid a hair off the face.
     */
    static void veins(ArmMesh.Builder b, double end) {
        java.util.Random r = new java.util.Random(0x5EA9E1L);
        for (int side : new int[] {1, -1}) {
            for (int crack = 0; crack < 24; crack++) {
                double u = BLADE_FROM + 0.4 + Math.pow(r.nextDouble(), 1.6) * (TIP - BLADE_FROM - 4.0);
                double f = 0.1 + 0.75 * r.nextDouble();
                double heading = (r.nextDouble() - 0.5) * 1.6;
                int steps = 4 + r.nextInt(5);
                double width = 0.15 * (1.0 - 0.5 * (u - BLADE_FROM) / (TIP - BLADE_FROM));
                walk(b, r, side, u, f, heading, steps, width, 0, end);
            }
        }
    }

    /** One crack: a walk of short steps across the face (f: 0 its back, 1 its edge), branching now and then. */
    static void walk(ArmMesh.Builder b, java.util.Random r, int side, double u, double f, double heading, int steps, double width, int depth,
                     double end) {
        for (int k = 0; k < steps; k++) {
            double step = 0.7 + 0.6 * r.nextDouble();
            double nu = u + Math.cos(heading) * step;
            double nf = f + Math.sin(heading) * step / 4.2;
            if (nu > Math.min(TIP - 3.0, end - 1.2) || nu < BLADE_FROM + 0.2 || nf < 0.04 || nf > 0.94) {
                return;
            }
            double[] a = onFace(u, f, side, 0.025);
            double[] e = onFace(nu, nf, side, 0.025);
            b.strip(a, e, width, new double[] {0.0, 0.0, side}, k % 3 == 0 ? VEIN_DARK : VEIN);
            if (depth < 2 && r.nextDouble() < 0.28) {
                walk(b, r, side, nu, nf, heading + (r.nextBoolean() ? 1.0 : -1.0) * (0.6 + 0.5 * r.nextDouble()), 2 + r.nextInt(3), width * 0.7,
                    depth + 1, end);
            }
            u = nu;
            f = nf;
            heading += (r.nextDouble() - 0.5) * 0.9;
        }
    }

    /** A point on the face ({@code side} +1 or -1 in w) at {@code u}, {@code f} across from its back to its edge, {@code lift} off it. */
    static double[] onFace(double u, double f, int side, double lift) {
        double s = spine(u);
        double e = edge(u);
        double h = thick(u);
        // The face's height across the section (as blade() lays it).
        double w = f <= 0.36 ? h : f >= 1.0 ? 0.0 : f <= 0.52 ? h * (1.0 - 0.14 * (f - 0.36) / 0.16)
            : h * (0.86 - 0.86 * (f - 0.52) / 0.48);
        return at(u, s + (e - s) * f, side * (w + lift));
    }

    // ---------------------------------------------------------------- the guard

    /** The guard's reach toward its back (v, negative), toward its edge, and its half thickness, at {@code u}. */
    static double[] guardSize(double u) {
        double t = (u - GUARD_FROM) / (BLADE_FROM + 1.6 - GUARD_FROM);
        t = Math.max(0.0, Math.min(1.0, t));
        // A long leaf: out of the grip, at its broadest a third of the way along (its back higher than its edge side is low), drawn back
        // into the blade's own width and thickness at its end (no neck).
        double end = BLADE_FROM + 1.6;
        double back = QuinqueDawnMesh.profile(new double[][] {{0.0, -0.75}, {0.14, -2.5}, {0.36, -4.05}, {0.58, -3.6}, {0.8, -2.95},
            {1.0, spine(end) - 0.05}}, t);
        double edge = QuinqueDawnMesh.profile(new double[][] {{0.0, 0.75}, {0.14, 1.95}, {0.36, 2.95}, {0.58, 2.75}, {0.8, 2.4},
            {1.0, edge(end) + 0.02}}, t);
        double half = QuinqueDawnMesh.profile(new double[][] {{0.0, 0.6}, {0.18, 1.2}, {0.36, 1.45}, {0.62, 1.05}, {0.84, 0.7},
            {1.0, thick(end) + 0.04}}, t);
        return new double[] {back, edge, half};
    }

    /** The guard: an organic silver swell round the blade's foot, its gems, the spines along its back, the ribs under it, its horn. */
    static void guard(ArmMesh.Builder b) {
        int rows = 40;
        int cols = 28;
        double[][][] g = new double[rows][cols][];
        int[][] c = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            double t = i / (rows - 1.0);
            double u = GUARD_FROM + (BLADE_FROM + 1.6 - GUARD_FROM) * t;
            double[] size = guardSize(u);
            double mid = (size[0] + size[1]) * 0.5;
            double half = (size[1] - size[0]) * 0.5;
            for (int j = 0; j < cols; j++) {
                double a = Math.PI * 2.0 * j / cols;
                // A rounded lens: wide across its flat, its faces full; a ripple of grown ridges over it.
                double ripple = 1.0 + 0.06 * Math.sin(a * 5.0 + u * 1.3) * Math.sin(Math.PI * t);
                double v = mid + half * Math.cos(a) * ripple;
                double w = size[2] * Math.sin(a) * Math.pow(Math.abs(Math.sin(a)), -0.35) * ripple;
                g[i][j] = at(u, v, Double.isFinite(w) ? w : 0.0);
                double shade = 0.5 + 0.5 * Math.sin(a * 3.0 + u * 0.9);
                c[i][j] = ArmMesh.mix(SILVER_DARK, SILVER_LIGHT, 0.35 + 0.5 * shade * (0.6 + 0.4 * Math.abs(Math.sin(a))));
            }
        }
        b.grid(g, c, true, false);
        // The gems: a great oval one in each face, a ring of small ones round it, one more on the blade's foot.
        for (int side : new int[] {1, -1}) {
            double gu = 4.9;
            double[] size = guardSize(gu);
            double face = size[2] * 0.98;
            gem(b, at(gu, (size[0] + size[1]) * 0.5 - 0.1, side * face), 1.75, 0.85, side);
            double[][] small = {{3.0, -2.2}, {3.4, 1.4}, {7.3, -1.7}, {7.1, 1.0}, {2.4, 0.2}, {8.4, -0.5}, {6.1, -2.6}};
            for (double[] s : small) {
                double[] sz = guardSize(s[0]);
                gem(b, at(s[0], s[1], side * sz[2] * Math.sqrt(Math.max(0.15, 1.0 - Math.pow((s[1] - (sz[0] + sz[1]) * 0.5) / ((sz[1] - sz[0]) * 0.5), 2)))
                    * 0.97), 0.32, 0.32, side);
            }
            // A dark eye set in the blade's foot.
            inset(b, BLADE_FROM + 2.8, side);
        }
        // Spines along its back, swept back toward the grip.
        for (int k = 0; k < 5; k++) {
            double u = GUARD_FROM + 1.0 + k * 1.35;
            double[] size = guardSize(u);
            double len = 1.4 + 1.0 * Math.sin(Math.PI * (k + 0.5) / 5.0);
            double[][] path = new double[8][];
            double[] radius = new double[8];
            for (int s = 0; s < 8; s++) {
                double t = s / 7.0;
                path[s] = at(u - len * 0.75 * t, size[0] + 0.3 - len * t * (1.0 - 0.2 * t), 0.0);
                radius[s] = 0.42 * (1.0 - t) + 0.03;
            }
            b.tube(path, radius, 8, (row, side) -> ArmMesh.mix(SILVER, SILVER_LIGHT, row / 8.0), false, true);
        }
        // A comb of ribs under it, along its edge's side.
        for (int k = 0; k < 8; k++) {
            double u = GUARD_FROM + 0.9 + k * 0.62;
            double[] size = guardSize(u);
            double[][] path = {at(u, size[1] - 0.3, 0.0), at(u - 0.15, size[1] + 0.45, 0.0), at(u - 0.4, size[1] + 0.8, 0.0)};
            b.tube(path, new double[] {0.2, 0.16, 0.05}, 6, (row, side) -> SILVER_DARK, false, true);
        }
        // Its horn: up out of the guard's back, swept back along the grip, past the ring.
        int n = 22;
        double[][] horn = new double[n][];
        double[] hr = new double[n];
        for (int s = 0; s < n; s++) {
            double t = s / (n - 1.0);
            double u = GUARD_FROM + 2.8 - (GUARD_FROM + 2.8 - (RING - 3.2)) * t;
            double v = -2.6 - 1.6 * Math.sin(Math.PI * 0.5 * Math.min(1.0, t * 1.6)) - 0.5 * t * t;
            horn[s] = at(u, v, 0.0);
            hr[s] = 0.62 * Math.pow(1.0 - t, 0.8) + 0.04;
        }
        b.tube(horn, hr, 10, (row, side) -> ArmMesh.mix(SILVER_LIGHT, SILVER_DARK, 0.3 + 0.3 * Math.sin(row * 0.6)), false, true);
    }

    /** A gem: a flattened oval of red, {@code long} along u, {@code wide} across, set in the face {@code side}. */
    static void gem(ArmMesh.Builder b, double[] centre, double along, double wide, int side) {
        int rings = 7;
        int segs = 14;
        double[][][] g = new double[rings + 1][segs][];
        int[][] c = new int[rings + 1][segs];
        double depth = 0.35 * Math.min(along, wide);
        for (int i = 0; i <= rings; i++) {
            double th = Math.PI * 0.5 * i / rings; // from its crown to its rim
            for (int j = 0; j < segs; j++) {
                double ph = Math.PI * 2.0 * j / segs;
                double rr = Math.sin(th);
                g[i][j] = at(centre[0] + along * rr * Math.cos(ph), centre[1] + wide * rr * Math.sin(ph), centre[2] + side * depth * Math.cos(th));
                c[i][j] = ArmMesh.mix(GEM, i < 2 ? 0xFFFF8A90 : GEM_DARK, i < 2 ? 0.5 : (double)i / rings * 0.6);
            }
        }
        if (side < 0) {
            for (double[][] row : g) {
                java.util.Collections.reverse(java.util.Arrays.asList(row));
            }
        }
        b.grid(g, c, true, false);
        // Its silver bezel.
        double[][] ring = new double[segs + 1][];
        for (int j = 0; j <= segs; j++) {
            double ph = Math.PI * 2.0 * j / segs;
            ring[j] = at(centre[0] + (along + 0.12) * Math.cos(ph), centre[1] + (wide + 0.12) * Math.sin(ph), centre[2]);
        }
        double[] rr = new double[segs + 1];
        java.util.Arrays.fill(rr, 0.11);
        b.tube(ring, rr, 6, (row, s) -> SILVER_LIGHT, false, false);
    }

    /** The dark eye in the blade's foot, on face {@code side}. */
    static void inset(ArmMesh.Builder b, double u0, int side) {
        int segs = 16;
        double[][][] g = new double[2][segs][];
        int[][] c = new int[2][segs];
        for (int j = 0; j < segs; j++) {
            double ph = Math.PI * 2.0 * j / segs;
            double u = u0 + 1.4 * Math.cos(ph);
            double f = 0.42 + 0.2 * Math.sin(ph) * Math.pow(Math.abs(Math.cos(ph * 0.5)), 0.2);
            g[0][j] = onFace(u, f, side, 0.04);
            g[1][j] = onFace(u0, 0.42, side, 0.04);
            c[0][j] = INSET;
            c[1][j] = 0xFF55585E;
        }
        if (side < 0) {
            for (double[][] row : g) {
                java.util.Collections.reverse(java.util.Arrays.asList(row));
            }
        }
        b.grid(g, c, true, false);
    }

    // ---------------------------------------------------------------- 4.1.29: the golden case

    static final int GOLD = 0xFFD4AE3A;
    static final int GOLD_LIGHT = 0xFFF4DA7C;
    static final int GOLD_DARK = 0xFF8E6E1A;

    static final int LINING = 0xFF5A0A12;
    static final int LINING_DARK = 0xFF2E0408;

    /** 4.1.29: the case's size in its own frame (pixels): its top under the handle, its bottom (its hinge), half its length, half its depth. */
    public static final double CASE_TOP = 1.6;
    public static final double CASE_BOTTOM = 10.8;
    static final double CASE_HALF = 5.3;
    static final double CASE_DEPTH = 1.85;

    private static final ArmMesh.Mesh[] HALVES = new ArmMesh.Mesh[2];
    private static ArmMesh.Mesh handle;

    /**
     * 4.1.29 (the owner: "pulls out a random briefcase from his back like a golden looking briefcase", "the briefcase just splits apart open
     * like a normal fucking briefcase and releases the weapon"): one half of his blade's case ({@code side} +1: its x+ half, with its latches;
     * -1 the other), gold, in its own frame (pixels: the handle's grip at 0, x across its depth, y down from the handle, z along it): its
     * corners rounded, its seam's edge dark, its open side lined in red (seen when it splits open on its hinge, its bottom edge).
     */
    public static synchronized ArmMesh.Mesh goldHalf(int side) {
        int key = side > 0 ? 0 : 1;
        if (HALVES[key] == null) {
            ArmMesh.Builder b = new ArmMesh.Builder(p -> p.clone(), n -> n.clone());
            int n = 44;
            double cy = (CASE_TOP + CASE_BOTTOM) * 0.5;
            double hh = (CASE_BOTTOM - CASE_TOP) * 0.5;
            double[][] loop = new double[n][];
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0 * i / n;
                double c = Math.cos(a);
                double sn = Math.sin(a);
                loop[i] = new double[] {Math.signum(c) * Math.pow(Math.abs(c), 0.22) * CASE_HALF, cy + Math.signum(sn) * Math.pow(Math.abs(sn), 0.22) * hh};
            }
            // Across its depth from the seam out to its face: the seam's dark lip, the side, the face's bevel.
            double[] xs = {0.03, 0.18, CASE_DEPTH - 0.18, CASE_DEPTH};
            double[] grow = {0.985, 1.0, 1.0, 0.93};
            int[] tone = {GOLD_DARK, GOLD, GOLD, GOLD_LIGHT};
            double[][][] g = new double[xs.length][n][];
            int[][] c = new int[xs.length][n];
            for (int r = 0; r < xs.length; r++) {
                for (int i = 0; i < n; i++) {
                    double z = loop[i][0] * (grow[r] < 0.95 ? 0.97 : 1.0);
                    g[r][i] = new double[] {side * xs[r], cy + (loop[i][1] - cy) * grow[r], z};
                    c[r][i] = r == 0 ? tone[r] : ArmMesh.mix(tone[r], GOLD_LIGHT, 0.15 + 0.2 * Math.sin(i * 0.45 + r));
                }
            }
            if (side < 0) {
                for (double[][] row : g) {
                    java.util.Collections.reverse(java.util.Arrays.asList(row));
                }
                for (int[] row : c) {
                    for (int i = 0, j = row.length - 1; i < j; i++, j--) {
                        int t = row[i];
                        row[i] = row[j];
                        row[j] = t;
                    }
                }
            }
            b.grid(g, c, true, false);
            // Its face (gold, a raised panel's rim) and its open side (the red lining, darker toward its middle).
            b.cap(new double[] {side * CASE_DEPTH, cy, 0.0}, g[xs.length - 1], GOLD, side < 0, false);
            b.cap(new double[] {side * 0.03, cy, 0.0}, g[0], LINING, side > 0, false);
            double[][] lining = new double[n][];
            for (int i = 0; i < n; i++) {
                lining[i] = new double[] {side * 0.06, cy + (g[0][i][1] - cy) * 0.6, g[0][i][2] * 0.75};
            }
            b.cap(new double[] {side * 0.06, cy, 0.0}, lining, LINING_DARK, side > 0, false);
            // A raised rim round its face.
            double[][] rim = new double[n + 1][];
            double[] rr = new double[n + 1];
            for (int i = 0; i <= n; i++) {
                double[] q = g[xs.length - 1][i % n];
                rim[i] = new double[] {side * (CASE_DEPTH + 0.05), cy + (q[1] - cy) * 0.82, q[2] * 0.85};
                rr[i] = 0.14;
            }
            b.tube(rim, rr, 6, (row, s2) -> GOLD_LIGHT, false, false);
            if (side > 0) {
                // Its latches on its top, either side of the handle.
                for (double z : new double[] {-2.8, 2.8}) {
                    double[][] latch = {{0.3, CASE_TOP - 0.2, z - 0.5}, {0.3, CASE_TOP - 0.2, z + 0.5}};
                    b.tube(latch, new double[] {0.45, 0.45}, 6, (row, s2) -> GOLD_DARK, false, true);
                }
            }
            HALVES[key] = b.done();
        }
        return HALVES[key];
    }

    /** 4.1.29: the case's handle, a loop over its top, the fist round its middle (its own frame). */
    public static synchronized ArmMesh.Mesh goldHandle() {
        if (handle == null) {
            ArmMesh.Builder b = new ArmMesh.Builder(p -> p.clone(), n -> n.clone());
            int k = 14;
            double[][] path = new double[k][];
            double[] hr = new double[k];
            for (int i = 0; i < k; i++) {
                double a = Math.PI * i / (k - 1.0);
                path[i] = new double[] {0.0, CASE_TOP - 0.1 - 1.6 * Math.sin(a), -2.0 * Math.cos(a)};
                hr[i] = 0.34;
            }
            b.tube(path, hr, 8, (row, s2) -> GOLD_DARK, false, true);
            handle = b.done();
        }
        return handle;
    }

    /** 4.1.29: a point of a half of the case swung {@code angle} open on its hinge (its bottom edge; {@code side} +1 or -1). */
    public static double[] opened(double[] p, int side, double angle) {
        double a = side * angle;
        double dy = p[1] - CASE_BOTTOM;
        return new double[] {Math.cos(a) * p[0] - Math.sin(a) * dy, CASE_BOTTOM + Math.sin(a) * p[0] + Math.cos(a) * dy, p[2]};
    }

    /** 4.1.29: the blade slid {@code out} of the way out of its case: its blade and its rod drawn in toward the guard and the fist. */
    public static double slid(double u, double out) {
        if (u > BLADE_FROM) {
            return BLADE_FROM + (u - BLADE_FROM) * out;
        }
        if (u < -1.6) {
            return -1.6 + (u + 1.6) * out;
        }
        return u;
    }

    // ---------------------------------------------------------------- the grip, its ring, the rod

    /** The long ribbed grip for both hands, the collar into the guard, the ring the rod runs through, the rod, its red tip. */
    static void grip(ArmMesh.Builder b) {
        int n = 40;
        double[][] path = new double[n][];
        double[] radius = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (n - 1.0);
            double u = GRIP_TO + (GUARD_FROM + 0.4 - GRIP_TO) * t;
            path[i] = at(u, 0.0, 0.0);
            radius[i] = 0.6 + 0.035 * Math.cos(u * 2.6) + (u > GUARD_FROM - 0.8 ? 0.22 * (u - GUARD_FROM + 0.8) : 0.0);
        }
        b.tube(path, radius, 12, (row, side) -> row % 3 == 0 ? SILVER_LIGHT : ArmMesh.mix(STEEL, SILVER, 0.55), false, true);
        // The ring, in the blade's plane, the rod through its middle.
        int segs = 28;
        double[][] ring = new double[segs + 1][];
        double[] rr = new double[segs + 1];
        for (int j = 0; j <= segs; j++) {
            double ph = Math.PI * 2.0 * j / segs;
            ring[j] = at(RING + 1.5 * Math.cos(ph), 1.5 * Math.sin(ph), 0.0);
            rr[j] = 0.3;
        }
        b.tube(ring, rr, 8, (row, s) -> row % 4 == 0 ? SILVER_DARK : SILVER_LIGHT, false, false);
        // The rod, a thread wound down it, out past the ring to its red tip.
        int m = 20;
        double[][] rod = new double[m][];
        double[] rodR = new double[m];
        for (int i = 0; i < m; i++) {
            double t = i / (m - 1.0);
            rod[i] = at(GRIP_TO + 0.3 - (GRIP_TO + 0.3 - (BUTT + 0.6)) * t, 0.0, 0.0);
            rodR[i] = 0.3 + 0.05 * Math.sin(i * 2.2);
        }
        b.tube(rod, rodR, 8, (row, s) -> row % 2 == 0 ? SILVER : STEEL, false, true);
        double[][] tip = {at(BUTT + 0.7, 0.0, 0.0), at(BUTT + 0.3, 0.0, 0.0), at(BUTT - 0.2, 0.0, 0.0), at(BUTT - 0.7, 0.0, 0.0)};
        b.tube(tip, new double[] {0.32, 0.42, 0.32, 0.04}, 10, (row, s) -> row < 1 ? SILVER : GEM, false, true);
        // The hooked prong under the ring: a claw curling back toward the guard.
        int k = 12;
        double[][] claw = new double[k][];
        double[] cr = new double[k];
        for (int i = 0; i < k; i++) {
            double t = i / (k - 1.0);
            double a = Math.PI * 0.85 * t;
            claw[i] = at(RING + 0.2 + 2.6 * Math.sin(a) * 0.9, 1.4 + 1.6 * (1.0 - Math.cos(a)) * 0.8, 0.0);
            cr[i] = 0.26 * (1.0 - t) + 0.03;
        }
        b.tube(claw, cr, 6, (row, s) -> SILVER_LIGHT, false, true);
    }
}
