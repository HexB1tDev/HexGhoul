package com.hex.kagune.control.quinque;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.0.0: the higher ranks' quinques as meshes (pure): the crystal spear, the flesh hammer, the bone greatsword, and the whip's handle (its
 * tendril is drawn as a Rinkaku tail is, {@link WhipPath}). Built once, kept ({@link #of}), drawn in the investigator's right hand.
 *
 * <p>Built in the weapon's own terms: {@code u} along it (its point, its head, forward; the fist at 0), {@code v} up across it (a blade's
 * width), {@code w} through it (its thickness); stored in the hand's frame (blocks: x across, y down the arm, z back: the fist at the
 * origin), so held in a hanging arm it points forward and in an arm raised forward it points up, as a sword held so does.
 *
 * <p>Organic parts (flesh, bone, sinew) are smooth surfaces with their own lumps and veins (value noise, by a fixed seed: the same each
 * time); the crystal is faceted. A quad may glow ({@link Mesh#glow}): the veins, the crystal's light, the kakuhou eye: drawn full bright.
 */
public final class ArmMesh {
    private ArmMesh() {
    }

    /** Quads: four vertices each (position and normal in the hand's frame, colour ARGB), whether each glows. */
    public static final class Mesh {
        public final float[] pos;
        public final float[] normal;
        public final int[] argb;
        public final boolean[] glow;
        public final int quads;

        Mesh(float[] pos, float[] normal, int[] argb, boolean[] glow) {
            this.pos = pos;
            this.normal = normal;
            this.argb = argb;
            this.glow = glow;
            this.quads = glow.length;
        }

        /** Its extent along the weapon (blocks): from its back end to its point ({@code -z}). */
        public float[] span() {
            float least = Float.MAX_VALUE;
            float most = -Float.MAX_VALUE;
            for (int i = 2; i < this.pos.length; i += 3) {
                least = Math.min(least, -this.pos[i]);
                most = Math.max(most, -this.pos[i]);
            }
            return new float[] {least, most};
        }
    }

    private static final Mesh[] BUILT = new Mesh[Arm.values().length * 2];

    /** {@code arm}'s mesh ({@code masterwork}: grown bigger, darker, its veins brighter), built the first time it is asked for. */
    public static synchronized Mesh of(Arm arm, boolean masterwork) {
        int key = arm.ordinal() * 2 + (masterwork ? 1 : 0);
        if (BUILT[key] == null) {
            BUILT[key] = build(arm, masterwork);
        }
        return BUILT[key];
    }

    static Mesh build(Arm arm, boolean masterwork) {
        Builder b = new Builder();
        switch (arm) {
            case WHIP -> whipHandle(b, masterwork);
            case CRYSTAL_SPEAR -> spear(b, masterwork);
            case FLESH_HAMMER -> hammer(b, masterwork);
            case BONE_SWORD -> sword(b, masterwork);
        }
        return b.done();
    }

    // ---------------------------------------------------------------- the palette

    static final int BONE = 0xFFE4D8C2;
    static final int BONE_DARK = 0xFFA89478;
    static final int BONE_BLACK = 0xFF2E2224;
    static final int FLESH = 0xFF6E0E1C;
    static final int FLESH_DARK = 0xFF3A0610;
    static final int FLESH_LIGHT = 0xFFA4303C;
    static final int SINEW = 0xFF4E1618;
    static final int VEIN = 0xFFFF2B40;
    static final int VEIN_MASTER = 0xFFFF5A2A;
    static final int CRYSTAL = 0xE0E23A62;
    static final int CRYSTAL_EDGE = 0xF0FF9AB4;
    static final int CRYSTAL_MASTER = 0xE0B01844;
    static final int EYE = 0xFFFF3A2E;

    // ---------------------------------------------------------------- the crystal spear

    /** A shaft of dark bone, ringed; a lumpy collar of flesh; the head of crystal: a long blade and barbs round its foot; a butt spike. */
    static void spear(Builder b, boolean master) {
        double back = -1.0;
        double front = master ? 1.55 : 1.45;
        int n = 48;
        double[][] shaft = new double[n][];
        double[] radius = new double[n];
        int[] colour = new int[n];
        for (int i = 0; i < n; i++) {
            double s = i / (double)(n - 1);
            double u = back + (front - back) * s;
            shaft[i] = new double[] {u, 0.0, 0.0};
            // Rings of bone every 0.3 (a little swell), the grips worn smooth.
            double ring = Math.pow(Math.max(0.0, Math.cos((u - 0.05) / 0.3 * Math.PI * 2.0)), 18.0);
            radius[i] = 0.03 + 0.008 * ring;
            colour[i] = mix(master ? BONE_BLACK : 0xFF3E2024, BONE_DARK, 0.35 * ring);
        }
        b.tube(shaft, radius, 10, (row, side) -> colour[row], false, true);
        // The collar of flesh the crystal grows from.
        b.blob(new double[] {front + 0.04, 0.0, 0.0}, new double[] {0.07, 0.075, 0.075}, 11, 16, 7, 0.28,
            (along, around, lump) -> mix(FLESH_DARK, FLESH_LIGHT, lump), master);
        // The crystal: the long blade, its barbs round its foot, small shards between.
        double head = front + 0.06;
        double main = master ? 1.0 : 0.82;
        b.crystal(new double[] {head, 0.0, 0.0}, new double[] {1.0, 0.0, 0.0}, main, master ? 0.1 : 0.088, 6, 0.0, master);
        int barbs = master ? 7 : 5;
        for (int i = 0; i < barbs; i++) {
            double a = i * Math.PI * 2.0 / barbs + 0.4;
            double out = 0.55;
            double[] dir = norm(new double[] {Math.cos(out), Math.sin(out) * Math.cos(a), Math.sin(out) * Math.sin(a)});
            b.crystal(new double[] {head + 0.02, 0.03 * Math.cos(a), 0.03 * Math.sin(a)}, dir, (i % 2 == 0 ? 0.38 : 0.28) * (master ? 1.2 : 1.0),
                0.042, 5, a, master);
        }
        for (int i = 0; i < barbs; i++) {
            double a = (i + 0.5) * Math.PI * 2.0 / barbs + 0.4;
            double[] dir = norm(new double[] {Math.cos(0.3), Math.sin(0.3) * Math.cos(a), Math.sin(0.3) * Math.sin(a)});
            b.crystal(new double[] {head + 0.06, 0.02 * Math.cos(a), 0.02 * Math.sin(a)}, dir, 0.14, 0.02, 4, a, master);
        }
        // The butt: a short spike of crystal.
        b.crystal(new double[] {back - 0.01, 0.0, 0.0}, new double[] {-1.0, 0.0, 0.0}, 0.16, 0.03, 5, 0.0, master);
        // Veins of light up the last span of the shaft into the head.
        b.veins(front - 0.45, front + 0.02, 0.033, 3, 0.0, master ? VEIN_MASTER : VEIN);
    }

    // ---------------------------------------------------------------- the flesh hammer

    /**
     * A long handle of jointed bone, sinew bound tight round its two grips and tendons running up it, and for a head a great sledge of
     * swollen flesh (squared, lumpy, veined, its roots gripping the handle), bone spikes out of its two striking faces, an eye of RC in each
     * side.
     */
    static void hammer(Builder b, boolean master) {
        double back = -0.62;
        double headAt = master ? 1.42 : 1.32;
        int n = 54;
        double[][] shaft = new double[n][];
        double[] radius = new double[n];
        int[] colour = new int[n];
        for (int i = 0; i < n; i++) {
            double s = i / (double)(n - 1);
            double u = back + (headAt - 0.1 - back) * s;
            shaft[i] = new double[] {u, 0.0, 0.0};
            // A bone of joints: knuckles every 0.32, yellowed between them, darker toward the head.
            double joint = Math.pow(Math.max(0.0, Math.cos((u + 0.06) / 0.32 * Math.PI * 2.0)), 10.0);
            radius[i] = 0.038 + 0.014 * joint;
            colour[i] = mix(mix(BONE_DARK, BONE, 0.55 + 0.45 * joint), master ? BONE_BLACK : 0xFF6A5444, 0.45 * s * s);
        }
        b.tube(shaft, radius, 10, (row, side) -> colour[row], false, true);
        // Sinew bound tight round its two grips.
        for (double[] grip : new double[][] {{-0.5, -0.16}, {0.18, 0.5}}) {
            for (int k = 0; k < 2; k++) {
                int m = 60;
                double[][] wrap = new double[m][];
                double[] r = new double[m];
                for (int i = 0; i < m; i++) {
                    double s = i / (double)(m - 1);
                    double u = grip[0] + (grip[1] - grip[0]) * s;
                    double a = s * Math.PI * 2.0 * 6.0 + k * Math.PI;
                    wrap[i] = new double[] {u, 0.046 * Math.cos(a), 0.046 * Math.sin(a)};
                    r[i] = 0.012;
                }
                b.tube(wrap, r, 5, (row, side) -> SINEW, false, false);
            }
        }
        // Tendons up the handle from the head, bulging as they go.
        for (int k = 0; k < 3; k++) {
            double a = k * Math.PI * 2.0 / 3.0 + 0.3;
            int m = 20;
            double[][] tendon = new double[m][];
            double[] r = new double[m];
            for (int i = 0; i < m; i++) {
                double s = i / (double)(m - 1);
                double u = headAt - 0.02 - 0.75 * s;
                double out = 0.05 + 0.012 * Math.sin(Math.PI * s);
                tendon[i] = new double[] {u, out * Math.cos(a + 0.4 * s), out * Math.sin(a + 0.4 * s)};
                r[i] = 0.014 * (1.0 - 0.6 * s);
            }
            b.tube(tendon, r, 6, (row, side) -> mix(FLESH, FLESH_DARK, row / 19.0), false, true);
        }
        // The head: a squared sledge of flesh, wide across, lumpy, veined.
        double[] radii = master ? new double[] {0.29, 0.56, 0.3} : new double[] {0.25, 0.47, 0.26};
        double[] centre = {headAt + radii[0] * 0.75, 0.0, 0.0};
        b.blob(centre, radii, 20, 30, 13, 0.14, (along, around, lump) -> mix(master ? 0xFF2A030A : FLESH_DARK, master ? FLESH : FLESH_LIGHT, lump), master,
            3.2, master ? 9 : 6);
        // Its roots: flesh gripping the top of the handle.
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 2.0 / 5.0;
            int m = 12;
            double[][] root = new double[m][];
            double[] r = new double[m];
            for (int i = 0; i < m; i++) {
                double s = i / (double)(m - 1);
                double u = centre[0] - radii[0] * 0.7 - 0.3 * s;
                double out = 0.11 * (1.0 - s) + 0.045 * s;
                root[i] = new double[] {u, out * Math.cos(a + 0.6 * s), out * Math.sin(a + 0.6 * s)};
                r[i] = 0.03 * (1.0 - 0.75 * s);
            }
            b.tube(root, r, 7, (row, side) -> mix(FLESH_LIGHT, FLESH_DARK, row / 11.0), false, true);
        }
        // Bone spikes out of its two striking faces (a ring and one through the middle), a few out of its crown.
        int ring = master ? 7 : 5;
        for (int face = -1; face <= 1; face += 2) {
            for (int i = 0; i < ring; i++) {
                double a = i * Math.PI * 2.0 / ring + (face > 0 ? 0.0 : 0.3);
                double[] dir = norm(new double[] {0.35 * Math.cos(a), face * 1.0, 0.35 * Math.sin(a)});
                double[] base = {centre[0] + radii[0] * 0.5 * Math.cos(a), centre[1] + face * radii[1] * 0.86, radii[2] * 0.5 * Math.sin(a)};
                b.spike(base, dir, (master ? 0.2 : 0.15) * (1.0 - 0.2 * (i % 2)), 0.032, 6, BONE, BONE_BLACK);
            }
            b.spike(new double[] {centre[0], centre[1] + face * radii[1] * 0.95, 0.0}, new double[] {0.0, face, 0.0}, master ? 0.28 : 0.22, 0.05, 7, BONE,
                BONE_BLACK);
        }
        for (int i = 0; i < 3; i++) {
            double a = (i - 1) * 0.55;
            b.spike(new double[] {centre[0] + radii[0] * 0.85, Math.sin(a) * radii[1] * 0.45, 0.0}, norm(new double[] {1.0, Math.sin(a) * 0.5, 0.0}),
                0.14, 0.03, 6, BONE, BONE_BLACK);
        }
        // Its eyes: orbs of RC in each side, glowing.
        for (int side = -1; side <= 1; side += 2) {
            b.orb(new double[] {centre[0], 0.0, side * radii[2] * 1.0}, master ? 0.075 : 0.06, EYE);
        }
        // Veins of light down the handle's last span into the head.
        b.veins(headAt - 0.55, headAt + 0.05, 0.05, 4, 0.0, master ? VEIN_MASTER : VEIN);
    }

    // ---------------------------------------------------------------- the bone greatsword

    /** A grip bound in sinew, a knob of bone, a guard of ribs curling forward, a serrated blade of bone with a living spine of flesh. */
    static void sword(Builder b, boolean master) {
        // The grip and its pommel.
        int n = 24;
        double[][] grip = new double[n][];
        double[] radius = new double[n];
        int[] colour = new int[n];
        for (int i = 0; i < n; i++) {
            double s = i / (double)(n - 1);
            double u = -0.42 + 0.52 * s;
            grip[i] = new double[] {u, 0.0, 0.0};
            double band = Math.pow(Math.abs(Math.sin(u / 0.05 * Math.PI)), 6.0);
            radius[i] = 0.036 + 0.004 * band;
            colour[i] = mix(SINEW, 0xFF6A2422, band);
        }
        b.tube(grip, radius, 10, (row, side) -> colour[row], false, false);
        b.blob(new double[] {-0.46, 0.0, 0.0}, new double[] {0.06, 0.06, 0.06}, 9, 14, 3, 0.15, (along, around, lump) -> mix(BONE_DARK, BONE, lump), false);
        // The guard: a knot of flesh, an eye of RC in it, ribs of bone curling forward round the blade's foot.
        b.blob(new double[] {0.13, 0.0, 0.0}, new double[] {0.07, 0.13, 0.07}, 11, 18, 5, 0.3, (along, around, lump) -> mix(FLESH_DARK, FLESH_LIGHT, lump), master);
        b.orb(new double[] {0.13, 0.0, 0.066}, 0.035, EYE);
        b.orb(new double[] {0.13, 0.0, -0.066}, 0.035, EYE);
        int ribs = master ? 4 : 3;
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < ribs; k++) {
                int m = 16;
                double[][] rib = new double[m][];
                double[] r = new double[m];
                double spread = 0.16 + 0.07 * k;
                for (int i = 0; i < m; i++) {
                    double s = i / (double)(m - 1);
                    double a = s * Math.PI * 0.85;
                    rib[i] = new double[] {0.12 + 0.02 * k + spread * 0.55 * Math.sin(a) + 0.12 * s * s, side * (0.06 + spread * (1.0 - Math.cos(a)) * 0.6),
                        (k - (ribs - 1) / 2.0) * 0.018};
                    r[i] = 0.018 * (1.0 - 0.7 * s);
                }
                b.tube(rib, r, 6, (row, s2) -> mix(BONE, BONE_BLACK, row / 15.0 * 0.6), false, true);
            }
        }
        // The blade: lofted cross-sections of bone, a raised spine of flesh, the edges serrated.
        double from = 0.16;
        double length = master ? 2.15 : 1.85;
        int sections = 64;
        double width = master ? 0.15 : 0.13;
        double thick = 0.03;
        double[][][] grid = new double[sections][][];
        int[][] tint = new int[sections][];
        for (int i = 0; i < sections; i++) {
            double s = i / (double)(sections - 1);
            double u = from + length * s;
            double taper = s < 0.88 ? 1.0 - 0.35 * s : (1.0 - 0.35 * 0.88) * Math.max(0.0, (1.0 - s) / 0.12);
            double h = width * taper;
            double t = thick * (1.0 - 0.5 * s);
            double curve = 0.045 * Math.sin(Math.PI * s); // a slight sweep up toward its middle
            // Teeth: along the upper edge (a saw), the lower edge keen and plain; masterwork both.
            double saw = (u / 0.07) % 1.0;
            double teeth = s < 0.9 ? 0.22 * (saw < 0.75 ? saw / 0.75 : (1.0 - saw) / 0.25) : 0.0;
            double top = h * (1.0 + teeth);
            double bottom = h * (1.0 + (master ? teeth * 0.7 : 0.0));
            double spine = t * (1.7 - 0.4 * s);
            grid[i] = new double[][] {
                {u, curve + top, 0.0}, {u, curve + h * 0.5, t}, {u, curve + h * 0.12, spine}, {u, curve, spine * 1.05}, {u, curve - h * 0.12, spine},
                {u, curve - h * 0.5, t}, {u, curve - bottom, 0.0}, {u, curve - h * 0.5, -t}, {u, curve - h * 0.12, -spine}, {u, curve, -spine * 1.05},
                {u, curve + h * 0.12, -spine}, {u, curve + h * 0.5, -t}};
            double grain = noise(u * 9.0, 1.3, 0.2);
            int bone = mix(master ? 0xFFC8B89E : BONE, BONE_DARK, 0.25 * grain + 0.2 * s);
            int edge = mix(bone, 0xFFF4EEE2, 0.6);
            int flesh = mix(FLESH_DARK, FLESH_LIGHT, 0.4 + 0.4 * noise(u * 14.0, 7.7, 0.0));
            tint[i] = new int[] {edge, bone, flesh, flesh, flesh, bone, edge, bone, flesh, flesh, flesh, bone};
        }
        b.loft(grid, tint, true);
        // Veins of light along the spine, both faces.
        for (int face = -1; face <= 1; face += 2) {
            int m = 80;
            for (int i = 0; i < m - 1; i++) {
                double s0 = i / (double)(m - 1);
                double s1 = (i + 1) / (double)(m - 1);
                if (noise(s0 * 20.0, face * 3.1, 9.0) < (master ? 0.25 : 0.42)) {
                    continue;
                }
                double u0 = from + length * s0 * 0.85;
                double u1 = from + length * s1 * 0.85;
                double c0 = 0.045 * Math.sin(Math.PI * s0) + 0.02 * Math.sin(s0 * 31.0);
                double c1 = 0.045 * Math.sin(Math.PI * s1) + 0.02 * Math.sin(s1 * 31.0);
                double w0 = face * (thick * (1.7 - 0.4 * s0) * 1.05 + 0.002);
                double w1 = face * (thick * (1.7 - 0.4 * s1) * 1.05 + 0.002);
                b.strip(new double[] {u0, c0, w0}, new double[] {u1, c1, w1}, 0.007, face, master ? VEIN_MASTER : VEIN);
            }
        }
    }

    // ---------------------------------------------------------------- the whip's handle

    /** A short handle of bone, the knot of flesh at its end the tendril grows out of. */
    static void whipHandle(Builder b, boolean master) {
        int n = 18;
        double[][] grip = new double[n][];
        double[] radius = new double[n];
        int[] colour = new int[n];
        for (int i = 0; i < n; i++) {
            double s = i / (double)(n - 1);
            double u = -0.16 + 0.36 * s;
            grip[i] = new double[] {u, 0.0, 0.0};
            double band = Math.pow(Math.abs(Math.sin(u / 0.045 * Math.PI)), 6.0);
            radius[i] = 0.034 + 0.006 * band;
            colour[i] = mix(BONE_DARK, master ? BONE_BLACK : SINEW, band);
        }
        b.tube(grip, radius, 10, (row, side) -> colour[row], false, true);
        b.blob(new double[] {0.24, 0.0, 0.0}, new double[] {0.075, 0.075, 0.075}, 11, 16, 5, 0.32, (along, around, lump) -> mix(FLESH_DARK, FLESH_LIGHT, lump),
            master);
        b.orb(new double[] {0.26, 0.0, 0.07}, 0.025, EYE);
        b.orb(new double[] {0.26, 0.0, -0.07}, 0.025, EYE);
    }

    /** Where the whip's tendril leaves its handle (the weapon's own terms: along it). */
    public static final double WHIP_ROOT = 0.3;

    // ---------------------------------------------------------------- building

    interface Tint {
        int at(int row, int side);
    }

    interface Skin {
        int at(double along, double around, double lump);
    }

    static final class Builder {
        final List<double[]> pos = new ArrayList<>();
        final List<double[]> nrm = new ArrayList<>();
        final List<Integer> col = new ArrayList<>();
        final List<Boolean> glow = new ArrayList<>();

        /** A quad (the weapon's own terms), its normals given per vertex. */
        void quad(double[][] p, double[][] n, int[] c, boolean glows) {
            for (int i = 0; i < 4; i++) {
                this.pos.add(hand(p[i]));
                this.nrm.add(hand(n[i] == null ? faceNormal(p) : normOr(n[i], faceNormal(p))));
                this.col.add(c[i]);
            }
            this.glow.add(glows);
        }

        /** A smooth grid ({@code rows} x {@code cols} points, the cols round and closed if {@code closed}), its normals from its neighbours. */
        void grid(double[][][] g, int[][] c, boolean closed, boolean glows) {
            int rows = g.length;
            int cols = g[0].length;
            double[][][] n = new double[rows][cols][];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    double[] a = g[Math.min(rows - 1, i + 1)][j];
                    double[] bb = g[Math.max(0, i - 1)][j];
                    int jn = closed ? (j + 1) % cols : Math.min(cols - 1, j + 1);
                    int jp = closed ? Math.floorMod(j - 1, cols) : Math.max(0, j - 1);
                    double[] d1 = sub(a, bb);
                    double[] d2 = sub(g[i][jn], g[i][jp]);
                    double[] x = cross(d1, d2);
                    n[i][j] = dot(x, x) < 1.0E-18 ? null : norm(x); // a pole: its quads' own faces

                }
            }
            int last = closed ? cols : cols - 1;
            for (int i = 0; i < rows - 1; i++) {
                for (int j = 0; j < last; j++) {
                    int k = (j + 1) % cols;
                    this.quad(new double[][] {g[i][j], g[i + 1][j], g[i + 1][k], g[i][k]}, new double[][] {n[i][j], n[i + 1][j], n[i + 1][k], n[i][k]},
                        new int[] {c[i][j], c[i + 1][j], c[i + 1][k], c[i][k]}, glows);
                }
            }
        }

        /** A tube along {@code path} (its own radius at each point), {@code sides} round, its ends capped if asked. */
        void tube(double[][] path, double[] radius, int sides, Tint tint, boolean glows, boolean caps) {
            int n = path.length;
            double[][][] g = new double[n][sides][];
            int[][] c = new int[n][sides];
            double[] prevSide = null;
            for (int i = 0; i < n; i++) {
                double[] t = norm(sub(path[Math.min(n - 1, i + 1)], path[Math.max(0, i - 1)]));
                double[] side = prevSide == null ? anyPerp(t) : norm(sub(prevSide, scale(t, dot(prevSide, t))));
                double[] up = cross(t, side);
                prevSide = side;
                for (int j = 0; j < sides; j++) {
                    double a = j * Math.PI * 2.0 / sides;
                    double[] off = add(scale(side, Math.cos(a) * radius[i]), scale(up, Math.sin(a) * radius[i]));
                    g[i][j] = add(path[i], off);
                    c[i][j] = tint.at(i, j);
                }
            }
            // Wound the right way out: the grid's normal (along x round) points out of the tube.
            if (dot(cross(sub(g[1][0], g[0][0]), sub(g[0][1], g[0][0])), sub(g[0][0], path[0])) < 0.0) {
                for (int i = 0; i < n; i++) {
                    reverse(g[i]);
                    reverseInts(c[i]);
                }
            }
            this.grid(g, c, true, glows);
            if (caps) {
                this.cap(path[0], g[0], c[0][0], true, glows);
                this.cap(path[n - 1], g[n - 1], c[n - 1][0], false, glows);
            }
        }

        /** A fan closing a tube's end ({@code start}: its first, facing back). */
        void cap(double[] centre, double[][] ring, int colour, boolean start, boolean glows) {
            int n = ring.length;
            for (int j = 0; j < n; j += 2) {
                double[] a = ring[j];
                double[] b1 = ring[(j + 1) % n];
                double[] b2 = ring[(j + 2) % n];
                double[][] p = start ? new double[][] {centre, b2, b1, a} : new double[][] {centre, a, b1, b2};
                double[] f = faceNormal(p);
                this.quad(p, new double[][] {f, f, f, f}, new int[] {colour, colour, colour, colour}, glows);
            }
        }

        /** A lumpy mass of flesh: an ellipsoid ({@code radii} along, up, through) pushed out and in by its lumps, its veins glowing. */
        void blob(double[] centre, double[] radii, int rings, int segs, int seed, double lumpiness, Skin skin, boolean master) {
            this.blob(centre, radii, rings, segs, seed, lumpiness, skin, master, 2.0, master ? 7 : 4);
        }

        /**
         * A lumpy mass of flesh: a superellipsoid ({@code radii} along, up, through; {@code squareness} 2 round, more boxier) pushed out and
         * in by its lumps; {@code veins} glowing veins wandering over it (a masterwork's brighter).
         */
        void blob(double[] centre, double[] radii, int rings, int segs, int seed, double lumpiness, Skin skin, boolean master, double squareness, int veins) {
            double[][][] g = new double[rings + 1][segs][];
            int[][] c = new int[rings + 1][segs];
            for (int i = 0; i <= rings; i++) {
                double th = Math.PI * i / rings;
                for (int j = 0; j < segs; j++) {
                    double ph = Math.PI * 2.0 * j / segs;
                    double[] d = {Math.cos(th), Math.sin(th) * Math.cos(ph), Math.sin(th) * Math.sin(ph)};
                    double lump = lump(d, seed);
                    g[i][j] = surface(centre, radii, d, seed, lumpiness, squareness);
                    c[i][j] = skin.at(i / (double)rings, j / (double)segs, lump);
                }
            }
            this.grid(g, c, true, false);
            int vein = master ? VEIN_MASTER : VEIN;
            for (int k = 0; k < veins; k++) {
                this.wander(centre, radii, seed, lumpiness, squareness, seed * 31 + k * 7, master ? 26 : 20, vein);
            }
        }

        /** A vein wandering over a blob's surface from a point its seed picks: a glowing thread, branching once. */
        void wander(double[] centre, double[] radii, int seed, double lumpiness, double squareness, int veinSeed, int steps, int colour) {
            double[] d = norm(new double[] {hash(veinSeed, 1, 2) - 0.5, hash(veinSeed, 3, 4) - 0.5, hash(veinSeed, 5, 6) - 0.5});
            double[] heading = anyPerp(d);
            double turn = 0.0;
            double[] last = null;
            for (int i = 0; i < steps; i++) {
                double[] at = surface(centre, radii, d, seed, lumpiness, squareness);
                double[] out = norm(sub(at, centre));
                double[] lifted = add(at, scale(out, 0.005));
                if (last != null) {
                    double width = 0.012 * (1.0 - 0.6 * i / steps);
                    this.strip(last, lifted, width, out, colour);
                }
                if (i == steps / 2 && last != null) {
                    // A branch off its middle.
                    this.branch(centre, radii, seed, lumpiness, squareness, d, cross(d, heading), veinSeed + 99, steps / 3, colour);
                }
                last = lifted;
                turn += (noise(i * 0.7, veinSeed * 0.31, 2.0) - 0.5) * 0.9;
                double[] side = cross(d, heading);
                heading = norm(add(scale(heading, Math.cos(turn * 0.35)), scale(side, Math.sin(turn * 0.35))));
                d = norm(add(d, scale(heading, 0.11)));
                heading = norm(sub(heading, scale(d, dot(heading, d))));
            }
        }

        void branch(double[] centre, double[] radii, int seed, double lumpiness, double squareness, double[] from, double[] heading, int veinSeed, int steps,
                    int colour) {
            double[] d = from;
            double[] last = null;
            for (int i = 0; i < steps; i++) {
                double[] at = surface(centre, radii, d, seed, lumpiness, squareness);
                double[] out = norm(sub(at, centre));
                double[] lifted = add(at, scale(out, 0.005));
                if (last != null) {
                    this.strip(last, lifted, 0.007 * (1.0 - 0.6 * i / steps), out, colour);
                }
                last = lifted;
                double bend = (noise(i * 0.9, veinSeed * 0.17, 5.0) - 0.5) * 0.6;
                double[] side = cross(d, heading);
                heading = norm(add(heading, scale(side, bend)));
                d = norm(add(d, scale(heading, 0.1)));
                heading = norm(sub(heading, scale(d, dot(heading, d))));
            }
        }

        /** A glowing orb (an eye of RC), set into flesh. */
        void orb(double[] centre, double radius, int colour) {
            int rings = 6;
            int segs = 10;
            double[][][] g = new double[rings + 1][segs][];
            int[][] c = new int[rings + 1][segs];
            for (int i = 0; i <= rings; i++) {
                double th = Math.PI * i / rings;
                for (int j = 0; j < segs; j++) {
                    double ph = Math.PI * 2.0 * j / segs;
                    g[i][j] = new double[] {centre[0] + radius * Math.cos(th), centre[1] + radius * Math.sin(th) * Math.cos(ph),
                        centre[2] + radius * Math.sin(th) * Math.sin(ph)};
                    double rim = Math.sin(th);
                    c[i][j] = mix(colour, 0xFFFFE0C0, 0.5 * (1.0 - rim));
                }
            }
            this.grid(g, c, true, true);
        }

        /** A spike of bone (a cone, {@code sides} round) from {@code base} out along {@code dir}, darkening to its point. */
        void spike(double[] base, double[] dir, double length, double radius, int sides, int root, int tip) {
            int n = 8;
            double[][] path = new double[n][];
            double[] r = new double[n];
            int[] colour = new int[n];
            double[] d = norm(dir);
            for (int i = 0; i < n; i++) {
                double s = i / (double)(n - 1);
                path[i] = add(base, scale(d, length * s));
                r[i] = Math.max(0.0015, radius * Math.pow(1.0 - s, 1.25));
                colour[i] = mix(root, tip, s * s);
            }
            this.tube(path, r, sides, (row, side) -> colour[row], false, true);
        }

        /**
         * A crystal: a short foot, a prism and a long point ({@code sides} faces), from {@code base} along {@code dir}: flat faces, its edges
         * lighter, glowing; turned {@code twist} about its length.
         */
        void crystal(double[] base, double[] dir, double length, double radius, int sides, double twist, boolean master) {
            double[] d = norm(dir);
            double[] side = anyPerp(d);
            double[] up = cross(d, side);
            double foot = length * 0.12;
            double prism = length * 0.38;
            int body = master ? CRYSTAL_MASTER : CRYSTAL;
            double[][] lower = new double[sides][];
            double[][] upper = new double[sides][];
            for (int j = 0; j < sides; j++) {
                double a = j * Math.PI * 2.0 / sides + twist;
                double[] off = add(scale(side, Math.cos(a) * radius), scale(up, Math.sin(a) * radius));
                lower[j] = add(add(base, scale(d, foot)), off);
                upper[j] = add(add(base, scale(d, foot + prism)), scale(off, 0.92));
            }
            double[] bottom = add(base, scale(d, -foot * 0.2));
            double[] point = add(base, scale(d, length));
            double[] axis = add(base, scale(d, length * 0.4));
            int deep = master ? 0xF0700A26 : 0xF0901838;
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                int shade = j % 2 == 0 ? body : mix(body, CRYSTAL_EDGE, 0.3);
                // Deep crimson at its foot, its own colour up the prism, pale toward its point.
                this.facet(new double[][] {bottom, lower[k], lower[j], bottom}, new int[] {deep, mix(deep, shade, 0.5), mix(deep, shade, 0.5), deep}, axis);
                this.facet(new double[][] {lower[j], lower[k], upper[k], upper[j]}, new int[] {mix(deep, shade, 0.5), mix(deep, shade, 0.5), shade, shade}, axis);
                int tip = mix(shade, CRYSTAL_EDGE, 0.75);
                this.facet(new double[][] {upper[j], upper[k], point, point}, new int[] {shade, shade, tip, tip}, axis);
            }
        }

        /** A flat glowing face (a crystal's), facing out from {@code inside}, its corners' colours. */
        void facet(double[][] p, int[] colours, double[] inside) {
            double[] f = faceNormal(p);
            if (dot(f, sub(centroid(p), inside)) < 0.0) {
                f = scale(f, -1.0);
            }
            this.quad(p, new double[][] {f, f, f, f}, colours, true);
        }

        /** A thin glowing thread from {@code a} to {@code b}, lying flat on a surface facing {@code out} ({@code face}: +1 or -1 through). */
        void strip(double[] a, double[] b, double width, double face, int colour) {
            this.strip(a, b, width, new double[] {0.0, 0.0, face}, colour);
        }

        void strip(double[] a, double[] b, double width, double[] out, int colour) {
            double[] along = norm(sub(b, a));
            double[] across = norm(cross(out, along));
            double[] h = scale(across, width * 0.5);
            double[][] p = {sub(a, h), add(a, h), add(b, h), sub(b, h)};
            double[] n = norm(out);
            if (dot(faceNormal(p), n) < 0.0) {
                p = new double[][] {p[3], p[2], p[1], p[0]};
            }
            this.quad(p, new double[][] {n, n, n, n}, new int[] {colour, colour, colour, colour}, true);
        }

        /** Veins of light along a shaft from {@code from} to {@code to} (along it), {@code count} of them round it, wandering. */
        void veins(double from, double to, double radius, int count, double seed, int colour) {
            int m = 30;
            for (int k = 0; k < count; k++) {
                double a0 = k * Math.PI * 2.0 / count + seed;
                for (int i = 0; i < m - 1; i++) {
                    double s0 = i / (double)(m - 1);
                    double s1 = (i + 1) / (double)(m - 1);
                    double a = a0 + 0.5 * Math.sin(s0 * 9.0 + k);
                    double b2 = a0 + 0.5 * Math.sin(s1 * 9.0 + k);
                    double[] p0 = {from + (to - from) * s0, radius * Math.cos(a), radius * Math.sin(a)};
                    double[] p1 = {from + (to - from) * s1, radius * Math.cos(b2), radius * Math.sin(b2)};
                    this.strip(p0, p1, 0.006, new double[] {0.0, Math.cos(a), Math.sin(a)}, colour);
                }
            }
        }

        /** Cross-sections ({@code grid[i]}: the points round one, closed) lofted into a surface, its point closed off. */
        void loft(double[][][] g, int[][] c, boolean closed) {
            // Wound outward: the first section's normal against its middle.
            double[] mid = centroid(g[g.length / 2]);
            double[][][] probe = g;
            double[] n0 = cross(sub(probe[g.length / 2 + 1][0], probe[g.length / 2][0]), sub(probe[g.length / 2][1], probe[g.length / 2][0]));
            if (dot(n0, sub(probe[g.length / 2][0], mid)) < 0.0) {
                for (int i = 0; i < g.length; i++) {
                    reverse(g[i]);
                    reverseInts(c[i]);
                }
            }
            this.grid(g, c, closed, false);
        }

        Mesh done() {
            int v = this.pos.size();
            float[] p = new float[v * 3];
            float[] n = new float[v * 3];
            int[] c = new int[v];
            for (int i = 0; i < v; i++) {
                for (int k = 0; k < 3; k++) {
                    p[i * 3 + k] = (float)this.pos.get(i)[k];
                    n[i * 3 + k] = (float)this.nrm.get(i)[k];
                }
                c[i] = this.col.get(i);
            }
            boolean[] g = new boolean[this.glow.size()];
            for (int i = 0; i < g.length; i++) {
                g[i] = this.glow.get(i);
            }
            return new Mesh(p, n, c, g);
        }
    }

    // ---------------------------------------------------------------- small maths

    /** A blob's lumps at unit way {@code d} (0..1). */
    static double lump(double[] d, int seed) {
        return noise(d[0] * 2.6 + seed, d[1] * 2.6, d[2] * 2.6) * 0.65 + noise(d[0] * 6.0, d[1] * 6.0 + seed, d[2] * 6.0) * 0.35;
    }

    /** A blob's surface out along unit way {@code d}: its superellipsoid, its lumps on it. */
    static double[] surface(double[] centre, double[] radii, double[] d, int seed, double lumpiness, double squareness) {
        double e = Math.pow(Math.abs(d[0]), squareness) + Math.pow(Math.abs(d[1]), squareness) + Math.pow(Math.abs(d[2]), squareness);
        double box = Math.pow(Math.max(1.0E-9, e), -1.0 / squareness);
        double r = box * (1.0 + lumpiness * (lump(d, seed) - 0.5) * 2.0);
        return new double[] {centre[0] + d[0] * radii[0] * r, centre[1] + d[1] * radii[1] * r, centre[2] + d[2] * radii[2] * r};
    }

    /** The weapon's own terms (along, up, through) into the hand's frame (across, down the arm, back). */
    static double[] hand(double[] uvw) {
        return new double[] {uvw[2], -uvw[1], -uvw[0]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] scale(double[] a, double s) {
        return new double[] {a[0] * s, a[1] * s, a[2] * s};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] norm(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : scale(a, 1.0 / l);
    }

    static double[] normOr(double[] a, double[] fallback) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-9 || Double.isNaN(l) ? fallback : scale(a, 1.0 / l);
    }

    static double[] midpoint(double[] a, double[] b) {
        return scale(add(a, b), 0.5);
    }

    static double[] centroid(double[][] ring) {
        double[] c = {0.0, 0.0, 0.0};
        for (double[] p : ring) {
            c = add(c, p);
        }
        return scale(c, 1.0 / ring.length);
    }

    /** A quad's face normal (its two diagonals' cross; a degenerate corner tolerated). */
    static double[] faceNormal(double[][] p) {
        return norm(cross(sub(p[2], p[0]), sub(p[3], p[1])));
    }

    static double[] anyPerp(double[] t) {
        double[] any = Math.abs(t[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        return norm(cross(t, any));
    }

    static void reverse(double[][] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            double[] t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    static void reverseInts(int[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    /** Two ARGB colours mixed ({@code t}: 0 the first, 1 the second). */
    static int mix(int a, int b, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int x = a >>> shift & 0xFF;
            int y = b >>> shift & 0xFF;
            out |= (int)Math.round(x + (y - x) * k) << shift;
        }
        return out;
    }

    /** Smooth value noise in [0, 1] (a fixed lattice: the same every time). */
    static double noise(double x, double y, double z) {
        int xi = (int)Math.floor(x);
        int yi = (int)Math.floor(y);
        int zi = (int)Math.floor(z);
        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;
        double u = xf * xf * (3.0 - 2.0 * xf);
        double v = yf * yf * (3.0 - 2.0 * yf);
        double w = zf * zf * (3.0 - 2.0 * zf);
        double x00 = lerp(hash(xi, yi, zi), hash(xi + 1, yi, zi), u);
        double x10 = lerp(hash(xi, yi + 1, zi), hash(xi + 1, yi + 1, zi), u);
        double x01 = lerp(hash(xi, yi, zi + 1), hash(xi + 1, yi, zi + 1), u);
        double x11 = lerp(hash(xi, yi + 1, zi + 1), hash(xi + 1, yi + 1, zi + 1), u);
        return lerp(lerp(x00, x10, v), lerp(x01, x11, v), w);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double hash(int x, int y, int z) {
        long h = x * 374761393L + y * 668265263L + z * 1274126177L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double)0x1000000;
    }
}
