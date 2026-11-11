package com.hex.kagune.control.quinque;

/**
 * 4.1.20 (the owner, quoted exactly: "Gotta say that handle looks like shit, bro. The katana should be like every weapon in the game should
 * look like it's made from a kagune. That that's horrible. Not only that, but like I said, I don't want squares."): the quinques grown out
 * of kagune as smooth meshes (pure), in place of item models made of boxes. Built with {@link ArmMesh}'s builder in the weapon's own terms
 * ({@code u} along it from the guard, {@code v} across it toward its back, {@code w} through it; blocks) and stored standing in its item
 * model's pixels (x across toward its edge, y along it, z through it), as tools/quinque_models.py builds its boxes; drawn turned
 * {@link #DIAGONAL} about z through the model's middle as the baker turns them ({@link #turned}), the frame its holds were solved in
 * ({@code client.animation.QuinqueHolds}: its grips lie on that diagonal), so every grip, stance and swing holds as it was. Drawn by
 * {@code client.QuinqueItemRenderer} wherever the item is drawn.
 *
 * <p>The draw out of the case ({@link #drawn}): the weapon grows out of the fist toward its ends as tools/quinque_models.py telescoped its
 * boxes, a small case round the grip shrinking into it ({@link #caseBox}), continuous rather than seven frames.
 */
public final class QuinqueMesh {
    private QuinqueMesh() {
    }

    /** The item model's pixel the weapon's own terms start from ({@code u} = 0): the middle of the guard. */
    static final double ANCHOR = 6.35;
    /** The right hand's grip, standing (tools/quinque_models.py HAND): the weapon grows out of it as it comes out of its case. */
    public static final double HAND = 5.0;

    /** The baker's turn of a standing weapon (degrees about z through the model's middle, 8 8 8): the handheld diagonal. */
    public static final float DIAGONAL = -45.0F;

    /** A standing point (item pixels) where the item draws it: turned {@link #DIAGONAL} about z through the model's middle. */
    public static float[] turned(float[] standing) {
        double a = Math.toRadians(DIAGONAL);
        double x = standing[0] - 8.0;
        double y = standing[1] - 8.0;
        return new float[] {(float)(8.0 + x * Math.cos(a) - y * Math.sin(a)), (float)(8.0 + x * Math.sin(a) + y * Math.cos(a)), standing[2]};
    }

    /** Whether {@code q} is drawn as a mesh (the rest keep their item models for now). */
    public static boolean has(Quinque q) {
        return q == Quinque.KATANA;
    }

    private static final ArmMesh.Mesh[] BUILT = new ArmMesh.Mesh[Quinque.values().length];
    private static ArmMesh.Mesh box;

    /** {@code q}'s mesh (its item model's pixels), built the first time it is asked for; null if it has none. */
    public static synchronized ArmMesh.Mesh of(Quinque q) {
        if (!has(q)) {
            return null;
        }
        if (BUILT[q.ordinal()] == null) {
            ArmMesh.Builder b = new ArmMesh.Builder(QuinqueMesh::item, QuinqueMesh::itemWay);
            katana(b);
            BUILT[q.ordinal()] = b.done();
        }
        return BUILT[q.ordinal()];
    }

    /** The weapon's own terms (blocks) to its item model's pixels. */
    static double[] item(double[] uvw) {
        return new double[] {8.0 - 16.0 * uvw[1], ANCHOR + 16.0 * uvw[0], 8.0 + 16.0 * uvw[2]};
    }

    static double[] itemWay(double[] n) {
        return new double[] {-n[1], n[0], n[2]};
    }

    /** Its weapon frames' place on its model's "deploy" (tools/quinque_models.py FRAMES): the case open at 0.2, all the way out at 0.95. */
    static final float OPEN = 0.2F;
    static final float OUT = 0.95F;

    /** How far out of its case a weapon of "deploy" {@code shown} ({@code client.QuinqueClient.shown}) is drawn (0 .. 1). */
    public static float out(float shown) {
        return Math.max(0.0F, Math.min(1.0F, (shown - OPEN) / (OUT - OPEN)));
    }

    /**
     * Where a point of the weapon ({@code y}: its item model's pixel along it) is drawn {@code out} of the way out of its case (0 .. 1): drawn
     * in toward the fist, all of it when out.
     */
    public static float drawn(float y, float out) {
        float f = Math.max(0.0F, Math.min(1.0F, out));
        return (float)(HAND + (y - HAND) * f);
    }

    /**
     * The small case round the grip as the weapon comes out ({@code out} 0 .. 1): {x0, y0, z0, x1, y1, z1} (item pixels), as
     * tools/quinque_models.py telescoped it; null once out.
     */
    public static float[] caseBox(float out) {
        if (out >= 1.0F) {
            return null;
        }
        double k = 1.0 - Math.max(0.0F, out);
        double half = 2.4 + 3.6 * k;
        double tall = 2.0 + 4.0 * k;
        double depth = 1.2 + 0.8 * k;
        double y0 = HAND + 1.0;
        return new float[] {(float)(8 - half), (float)y0, (float)(8 - depth), (float)(8 + half), (float)(y0 + tall), (float)(8 + depth)};
    }

    /** The case as a rounded box in a unit frame (-1 .. 1 each way), its trim and its red core seams, drawn into {@link #caseBox}. */
    public static synchronized ArmMesh.Mesh caseMesh() {
        if (box == null) {
            ArmMesh.Builder b = new ArmMesh.Builder(p -> p.clone(), n -> n.clone());
            // Its shell: gunmetal, rounded at the corners (a superellipsoid), lighter toward its top.
            b.blob(new double[] {0.0, 0.0, 0.0}, new double[] {1.0, 1.0, 1.0}, 10, 20, 3, 0.0,
                (along, around, lump) -> ArmMesh.mix(0xFF2A2C33, 0xFF4A4D57, 0.5 + 0.5 * Math.cos(Math.PI * along)), false, 7.0, 0);
            // The seam round its middle, glowing red where it splits for the weapon.
            int n = 40;
            double[][] seam = new double[n][];
            double[] r = new double[n];
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0 * i / (n - 1);
                double c = Math.cos(a);
                double s = Math.sin(a);
                double e = Math.pow(Math.pow(Math.abs(c), 7.0) + Math.pow(Math.abs(s), 7.0), -1.0 / 7.0);
                seam[i] = new double[] {0.0, c * e * 1.02, s * e * 1.02};
                r[i] = 0.06;
            }
            b.tube(seam, r, 6, (row, side) -> 0xFFC0182A, true, false);
            box = b.done();
        }
        return box;
    }

    // ---------------------------------------------------------------- the palette (hardened kagune, flesh, sinew, RC)

    static final int BLACK = 0xFF120306;
    static final int KAGUNE_DARK = 0xFF22050A;
    static final int KAGUNE = 0xFF4A0B15;
    static final int KAGUNE_LIGHT = 0xFF7E1424;
    static final int RIDGE = 0xFF9E2232;
    static final int HAMON = 0xFFD8203A;
    static final int GLOW = 0xFFFF3A4E;
    static final int EDGE = 0xFFFF7468;
    static final int FLESH = 0xFF5E0C18;
    static final int FLESH_DARK = 0xFF2E050C;
    static final int SINEW = 0xFF5A1418;
    static final int SINEW_LIGHT = 0xFFA8382E;

    // ---------------------------------------------------------------- the katana

    /** Where the blade begins and ends along it (blocks from the guard), as the 4.1.18 katana (item pixels 7.9 .. 31.9). */
    static final double BLADE_FROM = (7.6 - ANCHOR) / 16.0;
    static final double BLADE_TO = (31.9 - ANCHOR) / 16.0;
    /** Where the hilt ends (item pixel -4.85: the 4.1.18 hilt's end, where its holds put the left hand below). */
    static final double HILT_END = (-4.85 - ANCHOR) / 16.0;

    /**
     * A katana grown of kagune: the blade hardened kagune laid in plates like a Koukaku's (dark crimson, a seam and a ridge where each
     * begins), curving as a katana does (the point drawn back toward its back), its edge bevel crimson under a glowing temper line that
     * wanders as a hamon does; a collar of living flesh at its foot, an eye of RC on each face; the guard two Rinkaku tendrils grown out of
     * the collar and wound round the blade's foot, their tips hooked forward; the hilt a ribbed core like vertebrae bound in two strands
     * of sinew crossing as a wrap does (diamonds, no squares), a collar of black at each end; the pommel a knot of flesh, an eye at its end.
     */
    static void katana(ArmMesh.Builder b) {
        blade(b);
        guard(b);
        hilt(b);
    }

    /** The blade: lofted sections, back to edge; its edge bevels glowing under the temper line. */
    static void blade(ArmMesh.Builder b) {
        int sections = 96;
        double length = BLADE_TO - BLADE_FROM;
        // Across (0 its back .. 1 its edge) and through (of its thickness) round each section: the back's flat, the ridge (shinogi), the flat
        // to the bevel, the edge, and back up the other face.
        double[][] ring = {{0.0, 0.55}, {0.3, 1.0}, {0.62, 0.62}, {1.0, 0.0}, {0.62, -0.62}, {0.3, -1.0}, {0.0, -0.55}};
        int[] role = {0, 1, 2, 3, 2, 1, 0};
        double[][][] g = new double[sections][ring.length][];
        int[][] tint = new int[sections][ring.length];
        double[] backAt = new double[sections];
        double[] width = new double[sections];
        double[] thick = new double[sections];
        for (int i = 0; i < sections; i++) {
            double f = i / (sections - 1.0);
            double u = BLADE_FROM + length * f;
            double sori = 0.072 * f * f;                                   // the curve: its back drawn back (+v) toward the point
            double back = 0.0625 + sori;
            double h = 0.131 - 0.026 * f;                                  // back to edge
            if (f > 0.86) {
                // The kissaki: the edge sweeps up to meet the back at the point.
                double k = (f - 0.86) / 0.14;
                h *= Math.sqrt(Math.max(0.0, 1.0 - k * k));
            }
            h = Math.max(0.002, h);
            double t = Math.max(0.0025, 0.027 * (1.0 - 0.35 * f) * (f > 0.86 ? 0.35 + 0.65 * Math.sqrt(Math.max(0.0, 1.0 - Math.pow((f - 0.86) / 0.14, 2.0))) : 1.0));
            backAt[i] = back;
            width[i] = h;
            thick[i] = t;
            double grain = ArmMesh.noise(u * 9.0, 1.3, 0.7);
            for (int j = 0; j < ring.length; j++) {
                double across = ring[j][0];
                // Plates laid toward the point (a Koukaku's): each begins in a raised crimson lip, darkens along it to a black seam,
                // their ends swept back at the edge as scales lie.
                double plate = ((f + 0.045 * across) / PLATE) % 1.0;
                boolean plated = f < 0.86;
                double ridge = plated ? 1.0 + 0.22 * Math.pow(1.0 - plate, 3.0) : 1.0;
                double lip = plated ? Math.pow(1.0 - plate, 3.0) : 0.0;
                boolean seam = plated && plate < 0.06;
                // The plates' ridges raise its back and its shinogi only: the edge bevel stays one smooth face under its temper line.
                g[i][j] = new double[] {u, back - across * h, ring[j][1] * t * (role[j] <= 1 ? ridge : 1.0)};
                int colour = switch (role[j]) {
                    case 0 -> ArmMesh.mix(BLACK, KAGUNE_DARK, 0.3 + 0.4 * grain);                       // the back
                    case 1 -> ArmMesh.mix(ArmMesh.mix(KAGUNE_DARK, KAGUNE, 0.35 + 0.4 * grain), RIDGE, 0.9 * lip);  // the ridge
                    case 2 -> ArmMesh.mix(ArmMesh.mix(KAGUNE_DARK, KAGUNE, 0.2 + 0.4 * grain), KAGUNE_LIGHT, 0.85 * lip); // the flat
                    default -> HAMON;                                                                   // the edge
                };
                tint[i][j] = seam && role[j] != 3 ? BLACK : colour;
            }
        }
        b.faceted(g, tint);
        // The edge: each bevel glowing from the temper line (wandering as a hamon does) down to the edge, deep red to bright.
        for (int face = -1; face <= 1; face += 2) {
            for (int i = 0; i < sections - 1; i++) {
                double f0 = i / (sections - 1.0);
                double f1 = (i + 1) / (sections - 1.0);
                double[] a0 = hamon(f0, i, face, backAt, width, thick, length, temper(f0));
                double[] a1 = hamon(f1, i + 1, face, backAt, width, thick, length, temper(f1));
                double[] e0 = hamon(f0, i, face, backAt, width, thick, length, 1.0);
                double[] e1 = hamon(f1, i + 1, face, backAt, width, thick, length, 1.0);
                double[] n = ArmMesh.normOr(ArmMesh.cross(ArmMesh.sub(a1, a0), ArmMesh.sub(e0, a0)), new double[] {0.0, 0.0, face});
                if (n[2] * face < 0.0) {
                    n = ArmMesh.scale(n, -1.0);
                }
                int deep = ArmMesh.mix(0xFF9A0E22, 0xFFC0182C, 0.5 + 0.5 * Math.sin(f0 * 37.0));
                b.quad(new double[][] {a0, a1, e1, e0}, new double[][] {n, n, n, n}, new int[] {deep, deep, EDGE, EDGE}, true);
            }
        }
    }

    /** How long each plate of the blade is (of its length). */
    static final double PLATE = 0.075;

    /** Where the temper line lies across the edge bevel at {@code f} along the blade (0 the bevel's top .. 1 the edge): a hamon's waves. */
    static double temper(double f) {
        return 0.3 + 0.2 * Math.sin(f * 58.0) * Math.sin(f * 7.0 + 0.6) + 0.08 * Math.sin(f * 23.0 + 1.3);
    }

    /** A point on face {@code face}'s edge bevel at {@code f} along the blade (section {@code i}), {@code s} of the way from its top to the edge, just off it. */
    static double[] hamon(double f, int i, int face, double[] backAt, double[] width, double[] thick, double length, double s) {
        double across = 0.62 + 0.38 * s;
        double through = 0.62 * (1.0 - s);
        double lift = 0.0012;
        return new double[] {BLADE_FROM + length * f, backAt[i] - across * width[i], face * (through * thick[i] + lift)};
    }

    /**
     * The collar of flesh at the blade's foot (its two eyes) and the guard: a tsuba grown of hardened kagune (a lumpy oval disc, veins of
     * light in it), two tendrils grown out of the collar and coiled over its rim, their tips hooked forward up the blade.
     */
    static void guard(ArmMesh.Builder b) {
        double collar = 0.075;
        b.blob(new double[] {collar, 0.0, 0.0}, new double[] {0.042, 0.088, 0.046}, 10, 18, 41, 0.22,
            (along, around, lump) -> ArmMesh.mix(FLESH_DARK, FLESH, 0.25 + 0.6 * lump), false, 2.2, 2);
        b.orb(new double[] {collar + 0.004, 0.005, 0.046}, 0.017, ArmMesh.EYE);
        b.orb(new double[] {collar + 0.004, 0.005, -0.046}, 0.017, ArmMesh.EYE);
        // The tsuba: an oval disc, its rim thicker (squared a little), dark kagune lit by its veins.
        b.blob(new double[] {0.0, -0.012, 0.0}, new double[] {0.022, 0.16, 0.1}, 8, 30, 57, 0.12,
            (along, around, lump) -> ArmMesh.mix(BLACK, KAGUNE, 0.2 + 0.55 * lump + 0.25 * Math.pow(Math.sin(Math.PI * along), 8.0)), false, 2.6, 5);
        for (int t = 0; t < 2; t++) {
            int n = 60;
            double[][] path = new double[n][];
            double[] r = new double[n];
            double start = t * Math.PI + 0.5;
            for (int i = 0; i < n; i++) {
                double s = i / (n - 1.0);
                // Out of the collar onto the tsuba's face, round over its rim (the tendril's own coil about the rim), and up toward the blade.
                double a = start + s * Math.PI * 1.25;
                double reach = 0.07 + 0.095 * Math.min(1.0, s / 0.3);
                double coil = Math.sin(s * Math.PI * 5.0 + t) * 0.022 * Math.min(1.0, s / 0.2);
                double across = reach * Math.cos(a) - 0.012;
                double through = (reach * 0.63 + 0.004) * Math.sin(a);
                double along = 0.012 + coil + (s > 0.78 ? 0.13 * Math.pow((s - 0.78) / 0.22, 1.7) : 0.0);
                path[i] = new double[] {along, across, through};
                // Thick at the root, swelling in scales like a Rinkaku's, tapering to a hooked point.
                double scale = 1.0 + 0.22 * Math.pow(Math.abs(Math.sin(s * Math.PI * 10.0)), 4.0);
                r[i] = Math.max(0.002, 0.03 * Math.pow(1.0 - s, 0.75) * scale);
            }
            b.tube(path, r, 9, (row, side) -> ArmMesh.mix(KAGUNE_DARK, row % 6 < 2 ? RIDGE : KAGUNE_LIGHT, 0.35 + 0.5 * row / (double)n), false, true);
            // A vein glowing along its outside.
            for (int i = 3; i < n - 10; i++) {
                double[] out0 = ArmMesh.norm(new double[] {0.0, path[i][1], path[i][2]});
                double[] p0 = ArmMesh.add(path[i], ArmMesh.scale(out0, r[i] * 1.0));
                double[] out1 = ArmMesh.norm(new double[] {0.0, path[i + 1][1], path[i + 1][2]});
                double[] p1 = ArmMesh.add(path[i + 1], ArmMesh.scale(out1, r[i + 1] * 1.0));
                b.strip(p0, p1, 0.006, out0, GLOW);
            }
        }
    }

    /** The hilt: its ribbed core, the two strands of sinew crossing round it, a black collar each end, the pommel and its eye. */
    static void hilt(ArmMesh.Builder b) {
        double from = HILT_END;
        double to = -0.012;
        int n = 60;
        double[][] core = new double[n][];
        double[] cr = new double[n];
        int[] cc = new int[n];
        for (int i = 0; i < n; i++) {
            double s = i / (n - 1.0);
            double u = from + (to - from) * s;
            core[i] = new double[] {u, 0.0, 0.0};
            double rib = Math.pow(Math.abs(Math.sin((u - from) / 0.058 * Math.PI)), 5.0);
            // A little fuller in the middle (a hilt swells between the hands), ribbed like a spine.
            cr[i] = 0.039 + 0.004 * Math.sin(Math.PI * s) + 0.005 * rib;
            cc[i] = ArmMesh.mix(BLACK, FLESH_DARK, 0.35 + 0.55 * rib);
        }
        b.tube(core, cr, 12, (row, side) -> cc[row], false, false);
        // The wrap: two flat bands of sinew wound the two ways round it, crossing in diamonds (a tsuka-ito's, grown), the core's black
        // showing in each diamond; each band ridged at its middle, lighter at its edges.
        int turns = 8;
        for (int strand = 0; strand < 2; strand++) {
            int m = 140;
            double way = strand == 0 ? 1.0 : -1.0;
            double[][] mid = new double[m][];
            double[][] out = new double[m][];
            double[][] along = new double[m][];
            for (int i = 0; i < m; i++) {
                double s = i / (m - 1.0);
                double u = from + 0.014 + (to - from - 0.032) * s;
                // Half a turn apart, so the two cross on the hilt's faces (where a wrap's diamonds are), not at its sides.
                double a = way * s * Math.PI * 2.0 * turns + strand * Math.PI;
                // Clear of the core's ribs (at most 0.048), the first band over the second.
                double radius = 0.0505 + 0.004 * Math.sin(Math.PI * s) + (strand == 0 ? 0.0012 : 0.0);
                out[i] = new double[] {0.0, Math.cos(a), Math.sin(a)};
                mid[i] = new double[] {u, Math.cos(a) * radius, Math.sin(a) * radius};
            }
            for (int i = 0; i < m; i++) {
                along[i] = ArmMesh.norm(ArmMesh.sub(mid[Math.min(m - 1, i + 1)], mid[Math.max(0, i - 1)]));
            }
            double half = 0.0135;
            for (int i = 0; i < m - 1; i++) {
                double[] side0 = ArmMesh.scale(ArmMesh.norm(ArmMesh.cross(out[i], along[i])), half);
                double[] side1 = ArmMesh.scale(ArmMesh.norm(ArmMesh.cross(out[i + 1], along[i + 1])), half);
                double[] crest0 = ArmMesh.add(mid[i], ArmMesh.scale(out[i], 0.0035));
                double[] crest1 = ArmMesh.add(mid[i + 1], ArmMesh.scale(out[i + 1], 0.0035));
                int band = ArmMesh.mix(SINEW, SINEW_LIGHT, 0.35 + 0.3 * Math.sin(i * 0.9 + strand));
                int rim = ArmMesh.mix(band, KAGUNE_LIGHT, 0.45);
                // Two facets, each from a band's edge up to its crest.
                double[] l0 = ArmMesh.add(mid[i], side0);
                double[] l1 = ArmMesh.add(mid[i + 1], side1);
                double[] r0 = ArmMesh.sub(mid[i], side0);
                double[] r1 = ArmMesh.sub(mid[i + 1], side1);
                b.quad(new double[][] {l0, l1, crest1, crest0}, new double[][] {out[i], out[i + 1], out[i + 1], out[i]}, new int[] {rim, rim, band, band}, false);
                b.quad(new double[][] {crest0, crest1, r1, r0}, new double[][] {out[i], out[i + 1], out[i + 1], out[i]}, new int[] {band, band, rim, rim}, false);
            }
        }
        // The collars: a ring of black kagune at the guard's foot and above the pommel.
        for (double at : new double[] {to - 0.004, from + 0.006}) {
            int m = 28;
            double[][] ring = new double[m][];
            double[] rr = new double[m];
            for (int i = 0; i < m; i++) {
                double a = Math.PI * 2.0 * i / (m - 1);
                ring[i] = new double[] {at, Math.cos(a) * 0.05, Math.sin(a) * 0.044};
                rr[i] = 0.011;
            }
            b.tube(ring, rr, 6, (row, side) -> ArmMesh.mix(BLACK, KAGUNE, 0.3 + 0.3 * Math.cos(side)), false, false);
        }
        // The pommel: a knot of flesh, veined, an eye of RC at its end.
        double pu = from - 0.03;
        b.blob(new double[] {pu, 0.0, 0.0}, new double[] {0.04, 0.055, 0.05}, 10, 16, 23, 0.25,
            (along, around, lump) -> ArmMesh.mix(FLESH_DARK, FLESH, 0.2 + 0.6 * lump), false, 2.3, 2);
        b.orb(new double[] {pu - 0.037, 0.0, 0.0}, 0.017, ArmMesh.EYE);
    }
}
