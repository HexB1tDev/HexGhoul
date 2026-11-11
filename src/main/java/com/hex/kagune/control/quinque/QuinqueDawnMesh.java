package com.hex.kagune.control.quinque;

/**
 * 4.1.26 (the owner: "His Quinque, Dawn, needs a major rework in look so it's smooth and not cubic. We need it to open properly when shooting
 * projectiles. Like, actually make it spread open like a fucking umbrella"): Dawn grown smooth (pure), built straight in its item model's
 * pixels (x along it from the butt to the point, y up, z through, its axis at y 8 z 8, as tools/quinque_models.py laid its boxes: its holds
 * hold as they were), {@code open} of the way open (0 shut .. 1 open):
 * <ul>
 *   <li>its flared guard: eight panels of black kagune, gold bands engraved round them, gold ribs along their seams; opening (the owner:
 *       "It just meant open. Like a reverse umbrella"), it turns inside out, its panels swinging over about where it meets the spike into a
 *       cup opening forward round the spike, its veined inside lit, the lightning gathering at the point;</li>
 *   <li>its long spike, smooth, six blades laid along each other, a gold line down each;</li>
 *   <li>its shaft and wound grip, its butt spike, the hooked knuckle guard under the hand.</li>
 * </ul>
 */
public final class QuinqueDawnMesh {
    private QuinqueDawnMesh() {
    }

    /** The right hand's grip (item pixels along it): the lance grows out of the fist from here (QuinqueHolds DAWN's right grip). */
    public static final double HAND = -5.7;
    /** The guard's rim and its hinge (item pixels along it), the canopy's panels. */
    static final double RIM = -2.8;
    static final double HINGE = 11.6;
    static final int PANELS = 8;
    /**
     * Opened, the guard turned inside out (the owner: "It just meant open. Like a reverse umbrella"): each panel swung past square about its
     * hinge, its rim ahead of it, the guard a cup opening toward what it aims at (radians from back along the shaft).
     */
    static final double REVERSE = Math.toRadians(145.0);
    /** Where the point splits, how far each quarter parts open (radians). */
    static final double SPLIT = 26.1;
    static final double PART = Math.toRadians(24.0);
    /** How many open steps are built (each built once). */
    public static final int STEPS = 8;

    static final int BLACK = 0xFF141114;
    static final int BLADE = 0xFF201C1C;
    static final int PLATE = 0xFF2E2828;
    static final int EDGE = 0xFF3A3030;
    static final int GOLD = 0xFFB08A3C;
    static final int GOLD_LIGHT = 0xFFE0B860;
    static final int ENGRAVING = 0xFF5A4424;
    static final int SHAFT = 0xFF1F1A18;
    static final int FLESH = 0xFF3A0A10;
    static final int FLESH_DARK = 0xFF1E0408;
    static final int LIGHT = 0xFFFFE460;
    static final int LIGHT_HOT = 0xFFFFF8D0;

    private static final ArmMesh.Mesh[] BUILT = new ArmMesh.Mesh[STEPS + 1];

    /** Dawn's mesh {@code open} of the way open (to the nearest of its {@link #STEPS}), built the first time it is asked for. */
    public static synchronized ArmMesh.Mesh of(float open) {
        int step = Math.max(0, Math.min(STEPS, Math.round(open * STEPS)));
        if (BUILT[step] == null) {
            ArmMesh.Builder b = new ArmMesh.Builder(p -> p.clone(), n -> n.clone());
            build(b, step / (double)STEPS);
            BUILT[step] = b.done();
        }
        return BUILT[step];
    }

    /** The guard's radius at {@code x} (item pixels): its rim, the flare, the taper into the spike's foot (Dawn's 2.0.30 profile, smoothed). */
    static double guard(double x) {
        double[][] p = {{-2.8, 3.04}, {-2.2, 2.93}, {-1.1, 2.79}, {-0.25, 2.62}, {0.5, 2.48}, {1.5, 2.22}, {2.15, 2.10}, {3.45, 1.91},
            {4.75, 1.72}, {6.05, 1.53}, {7.35, 1.35}, {8.675, 1.18}, {10.075, 1.04}, {11.65, 0.96}, {12.5, 0.94}};
        return profile(p, x);
    }

    /** The spike's radius at {@code x} (item pixels). */
    static double spike(double x) {
        double[][] p = {{12.2, 0.92}, {13.1, 0.84}, {13.6, 0.70}, {14.7, 0.70}, {15.3, 0.86}, {16.5, 0.95}, {17.8, 0.84}, {19.3, 0.70},
            {21.2, 0.58}, {23.5, 0.47}, {25.9, 0.36}, {28.1, 0.25}, {30.0, 0.14}, {31.8, 0.02}};
        return profile(p, x);
    }

    /** A smooth profile through {@code p} ((x, r) pairs): Catmull-Rom between them, held past the ends. */
    static double profile(double[][] p, double x) {
        if (x <= p[0][0]) {
            return p[0][1];
        }
        for (int i = 0; i < p.length - 1; i++) {
            if (x <= p[i + 1][0]) {
                double t = (x - p[i][0]) / (p[i + 1][0] - p[i][0]);
                double r0 = p[Math.max(0, i - 1)][1];
                double r1 = p[i][1];
                double r2 = p[i + 1][1];
                double r3 = p[Math.min(p.length - 1, i + 2)][1];
                double t2 = t * t;
                double t3 = t2 * t;
                return 0.5 * (2 * r1 + (-r0 + r2) * t + (2 * r0 - 5 * r1 + 4 * r2 - r3) * t2 + (-r0 + 3 * r1 - 3 * r2 + r3) * t3);
            }
        }
        return p[p.length - 1][1];
    }

    static double[] at(double x, double r, double a) {
        return new double[] {x, 8.0 + r * Math.sin(a), 8.0 + r * Math.cos(a)};
    }

    /** {@code p} turned {@code angle} about the line through {@code o} along unit {@code k}. */
    static double[] turn(double[] p, double[] o, double[] k, double angle) {
        double[] v = ArmMesh.sub(p, o);
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double[] kv = ArmMesh.cross(k, v);
        double kd = ArmMesh.dot(k, v);
        return new double[] {o[0] + v[0] * c + kv[0] * s + k[0] * kd * (1 - c), o[1] + v[1] * c + kv[1] * s + k[1] * kd * (1 - c),
            o[2] + v[2] * c + kv[2] * s + k[2] * kd * (1 - c)};
    }

    static void build(ArmMesh.Builder b, double open) {
        double ease = open * open * (3.0 - 2.0 * open);
        canopy(b, ease);
        spike(b, 0.0);
        shaft(b);
        if (ease > 0.01) {
            // The lightning gathering at its point, where it leaves (Quinque.DAWN_POINT), and glowing in the cup's veins.
            b.orb(new double[] {32.4, 8.0, 8.0}, 0.25 + 0.6 * ease, LIGHT_HOT);
        }
    }

    /**
     * The guard: its panels, each a shell of black kagune (gold bands round it, plated) over living kagune, opened {@code ease}: turned inside
     * out as an umbrella in a gale, each panel keeping its share of the circle while it swings over about the hinge (its point's slant from
     * the hinge kept) to {@link #REVERSE}, the cup it makes opening forward round the spike, its veined inside shown; a gold rib along every
     * seam.
     */
    static void canopy(ArmMesh.Builder b, double ease) {
        int rows = 30;
        int cols = 9;
        double half = Math.PI / PANELS - 0.01;
        double hr = guard(HINGE);
        // The slant from the hinge to each row along the guard's profile (its length as the cone opens).
        double[] slant = new double[rows];
        double[] xs = new double[rows];
        for (int i = rows - 1; i >= 0; i--) {
            xs[i] = RIM + (HINGE - RIM) * i / (rows - 1.0);
            slant[i] = i == rows - 1 ? 0.0 : slant[i + 1] + Math.hypot(xs[i + 1] - xs[i], guard(xs[i + 1]) - guard(xs[i]));
        }
        for (int k = 0; k < PANELS; k++) {
            double centre = Math.PI * 2.0 * k / PANELS + Math.PI / PANELS;
            double[][][] outer = new double[rows][cols][];
            double[][][] inner = new double[rows][cols][];
            int[][] oc = new int[rows][cols];
            int[][] ic = new int[rows][cols];
            for (int i = 0; i < rows; i++) {
                double x = xs[i];
                double r = guard(x);
                double thick = 0.08 + 0.18 * (1.0 - i / (rows - 1.0));
                // The chord's angle from the axis now, and opened.
                double closed = Math.atan2(r - hr, HINGE - x);
                double opened = closed + (REVERSE - closed) * ease;
                double along = (Math.cos(opened) - Math.cos(closed)) * slant[i];
                double out = (Math.sin(opened) - Math.sin(closed)) * slant[i];
                for (int j = 0; j < cols; j++) {
                    double f = j / (cols - 1.0);
                    double a = centre - half + 2.0 * half * f;
                    double sag = 1.0 - 0.07 * ease * Math.sin(Math.PI * f) * slant[i] / Math.max(1.0, slant[0]);
                    double ro = (r + out) * sag;
                    double ri = (r - thick + out) * sag;
                    outer[i][j] = at(x - along, ro, a);
                    inner[i][j] = at(x - along - thick * Math.sin(opened) * 0.5, ri, a);
                    oc[i][j] = canopyColour(x, j, cols);
                    double vein = Math.pow(Math.abs(Math.cos((f - 0.5) * 5.0 + x * 0.9)), 18.0);
                    ic[i][j] = ArmMesh.mix(FLESH_DARK, FLESH, 0.4 + 0.4 * Math.sin(x * 1.7 + k));
                    if (vein > 0.6 && ease > 0.05) {
                        ic[i][j] = ArmMesh.mix(ic[i][j], GOLD_LIGHT, 0.7);
                    }
                }
            }
            b.grid(outer, oc, false, false);
            b.grid(inner, ic, false, false);
            // Its rim, a gold lip, closing the shell; its sides.
            for (int j = 0; j < cols - 1; j++) {
                b.quad(new double[][] {outer[0][j], outer[0][j + 1], inner[0][j + 1], inner[0][j]}, new double[4][], new int[] {GOLD, GOLD, GOLD, GOLD},
                    false);
            }
            for (int side : new int[] {0, cols - 1}) {
                for (int i = 0; i < rows - 1; i++) {
                    b.quad(new double[][] {outer[i][side], outer[i + 1][side], inner[i + 1][side], inner[i][side]}, new double[4][],
                        new int[] {EDGE, EDGE, EDGE, EDGE}, false);
                }
            }
            // A rib along its seam with the next (an umbrella's rib), gold.
            for (int i = 0; i < rows - 1; i++) {
                double[] p0 = outer[i][0];
                double[] p1 = outer[i + 1][0];
                double[] out0 = ArmMesh.norm(ArmMesh.sub(p0, inner[i][0]));
                b.strip(ArmMesh.add(p0, ArmMesh.scale(out0, 0.05)), ArmMesh.add(p1, ArmMesh.scale(out0, 0.05)), 0.14, out0, i % 7 == 0 ? GOLD_LIGHT : GOLD);
            }
        }
        // The collar at the canopy's top, where the panels hang from (never moves).
        int n = 24;
        double[][] collar = new double[n][];
        double[] cr = new double[n];
        for (int i = 0; i < n; i++) {
            double s = i / (n - 1.0);
            double x = HINGE - 0.6 + 1.4 * s;
            collar[i] = new double[] {x, 8.0, 8.0};
            cr[i] = guard(x) + 0.06 + 0.05 * Math.sin(Math.PI * s);
        }
        b.tube(collar, cr, 24, (row, side) -> row % 6 < 2 ? GOLD : BLACK, false, false);
    }

    static double radius(double[] p) {
        return Math.hypot(p[1] - 8.0, p[2] - 8.0);
    }

    /** The canopy's skin at {@code x}: Dawn's gold bands and engraving where they were, plated black kagune between. */
    static int canopyColour(double x, int j, int cols) {
        if (x < -2.45) {
            return EDGE;
        }
        if (x >= -2.2 && x < -2.03 || x >= -1.14 && x < -0.97 || x >= 0.45 && x < 0.60 || x >= 1.35 && x < 1.5) {
            return GOLD;
        }
        if (x >= -2.03 && x < -1.14 || x >= 0.60 && x < 1.35) {
            double scroll = Math.sin(x * 9.0 + j * 1.7) * Math.cos(x * 4.0 - j * 0.9);
            return scroll > 0.35 ? GOLD_LIGHT : ENGRAVING;
        }
        double plate = (x - 1.5) / 1.35;
        double frac = plate - Math.floor(plate);
        return frac > 0.88 ? PLATE : ArmMesh.mix(BLACK, BLADE, 0.5 + 0.5 * Math.sin(x * 0.6 + j));
    }

    /** Where the spike splits into its blades (item pixels), how many, how far each splays open (radians). */
    static final double FOOT = 13.3;
    static final int BLADES = 6;
    static final double SPLAY = Math.toRadians(34.0);

    /**
     * The spike: smooth along its profile, its foot a ring of gold-banded black; from {@link #FOOT} it is {@link #BLADES} blades lying shut
     * along each other (their seams fine lines), each with a gold line down its back. Opened {@code ease}, as an umbrella turned inside out
     * (the owner: "It just meant open. Like a reverse umbrella"): each blade swings out from its foot, its point leading outward and
     * forward, the whole spike opening like a flower toward what it aims at, the lightning gathering between them.
     */
    static void spike(ArmMesh.Builder b, double ease) {
        int sides = 24;
        // Its foot, fixed: from the collar to where it splits.
        int frows = 8;
        double[][][] f = new double[frows][sides][];
        int[][] fc = new int[frows][sides];
        for (int i = 0; i < frows; i++) {
            double x = 12.2 + (FOOT - 12.2) * i / (frows - 1.0);
            for (int j = 0; j < sides; j++) {
                double a = Math.PI * 2.0 * j / sides;
                f[i][j] = at(x, spike(x) + 0.02, a);
                fc[i][j] = i == 2 || i == frows - 2 ? GOLD : BLACK;
            }
        }
        b.grid(f, fc, true, false);
        int rows = 44;
        int cols = 6;
        double half = Math.PI / BLADES - 0.008;
        for (int k = 0; k < BLADES; k++) {
            double centre = Math.PI * 2.0 * k / BLADES;
            double[] hinge = at(FOOT, spike(FOOT) * 0.55, centre);
            double[] axis = {0.0, Math.cos(centre), -Math.sin(centre)};
            double[] tip = at(31.8, 0.3, centre);
            double sign = radius(turn(tip, hinge, axis, 0.3)) > radius(tip) ? 1.0 : -1.0;
            double angle = sign * SPLAY * ease;
            double[][][] outer = new double[rows][cols][];
            double[][][] inner = new double[rows][cols][];
            int[][] oc = new int[rows][cols];
            int[][] ic = new int[rows][cols];
            for (int i = 0; i < rows; i++) {
                double x = FOOT + (31.8 - FOOT) * i / (rows - 1.0);
                double r = spike(x);
                for (int j = 0; j < cols; j++) {
                    double t = j / (cols - 1.0);
                    double a = centre - half + 2.0 * half * t;
                    outer[i][j] = turn(at(x, r, a), hinge, axis, angle);
                    // Its inside: a ridge down its middle toward the axis (a blade's back, not a hollow shell).
                    double in = r * (0.15 + 0.5 * Math.abs(t - 0.5) * 2.0);
                    inner[i][j] = turn(at(x, in, a), hinge, axis, angle);
                    oc[i][j] = Math.abs(t - 0.5) < 0.12 && i % 9 > 1 ? GOLD : ArmMesh.mix(BLACK, BLADE, 0.4 + 0.5 * Math.cos((t - 0.5) * Math.PI));
                    ic[i][j] = ease > 0.05 ? ArmMesh.mix(ENGRAVING, GOLD_LIGHT, 0.3 + 0.6 * (1.0 - Math.abs(t - 0.5) * 2.0)) : BLACK;
                }
            }
            b.grid(outer, oc, false, false);
            b.grid(inner, ic, false, false);
        }
    }

    /** The shaft through it, its wound grip, the butt spike, the hooked knuckle guard under the hand. */
    static void shaft(ArmMesh.Builder b) {
        int n = 40;
        double[][] rod = new double[n][];
        double[] rr = new double[n];
        for (int i = 0; i < n; i++) {
            double x = -14.8 + (12.4 + 14.8) * i / (n - 1.0);
            rod[i] = new double[] {x, 8.0, 8.0};
            double butt = x < -9.4 ? profile(new double[][] {{-14.8, 0.04}, {-14.0, 0.12}, {-12.7, 0.28}, {-11.0, 0.42}, {-10.2, 0.56}, {-9.4, 0.58}}, x)
                : 0.44;
            rr[i] = butt;
        }
        b.tube(rod, rr, 14, (row, side) -> row < 8 ? ArmMesh.mix(BLACK, GOLD, row == 7 ? 0.6 : 0.0) : SHAFT, false, true);
        // The grip's wire: a thin gold spiral wound round the shaft under the hand.
        int m = 120;
        double[][] wire = new double[m][];
        double[] wr = new double[m];
        for (int i = 0; i < m; i++) {
            double s = i / (m - 1.0);
            double x = -9.2 + 6.2 * s;
            double a = s * Math.PI * 2.0 * 9.0;
            wire[i] = new double[] {x, 8.0 + 0.47 * Math.sin(a), 8.0 + 0.47 * Math.cos(a)};
            wr[i] = 0.06;
        }
        b.tube(wire, wr, 5, (row, side) -> GOLD, false, false);
        // The hooked knuckle guard: from under the canopy's rim, round below the hand, back to the shaft by the butt.
        double[][] hook = {{-2.6, 6.25, 8.0}, {-3.6, 6.15, 8.0}, {-5.0, 6.2, 8.0}, {-6.4, 6.42, 8.0}, {-7.6, 6.85, 8.0}, {-8.5, 7.3, 8.0},
            {-9.1, 7.62, 8.0}};
        int h = 28;
        double[][] path = new double[h][];
        double[] hr = new double[h];
        for (int i = 0; i < h; i++) {
            double s = i * (hook.length - 1.0) / (h - 1.0);
            int a = Math.min(hook.length - 2, (int)s);
            double t = s - a;
            path[i] = new double[] {hook[a][0] + (hook[a + 1][0] - hook[a][0]) * t, hook[a][1] + (hook[a + 1][1] - hook[a][1]) * t, 8.0};
            hr[i] = 0.3 - 0.16 * i / (h - 1.0);
        }
        b.tube(path, hr, 10, (row, side) -> side == 0 ? GOLD : BLACK, false, true);
    }
}
