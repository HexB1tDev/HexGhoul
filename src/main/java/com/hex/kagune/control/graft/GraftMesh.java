package com.hex.kagune.control.graft;

import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * The shapes Graft DNA's kagune is built of (pure, tested; 7.0.0): organic tubes along a spine with a radius and a colour for every
 * point of their surface, curved horns and spikes, embedded eyes with lids, sockets and a pupil that looks somewhere, and teeth.
 * Quads go to a {@link UkakuModel.Sink} as every kagune model in the mod does: a shaded quad carries its true normal turned out of
 * the shape; a glowing one (RC light) carries the straight-up normal the renderers draw at full brightness.
 */
public final class GraftMesh {
    private GraftMesh() {
    }

    /** A tube's surface: its radius and colour at {@code s} along the spine (0 root .. 1 tip) and angle {@code a} round it. */
    public interface Surface {
        double radius(double s, double a);

        int colour(double s, double a);
    }

    /**
     * A tube along {@code spine} from {@code s0} to {@code s1}: {@code stations} rings of {@code sides} points. Angle 0 is the
     * spine's edge (its side), a quarter turn its back.
     */
    public static void tube(UkakuModel.Sink sink, KakujaModel.Spine spine, double s0, double s1, int stations, int sides, Surface surface,
                            double alpha) {
        double[][][] rings = new double[stations + 1][][];
        int[][] colours = new int[stations + 1][];
        double[][] centres = new double[stations + 1][];
        for (int i = 0; i <= stations; i++) {
            double s = s0 + (s1 - s0) * i / stations;
            double[][] f = spine.at(s);
            centres[i] = f[0];
            rings[i] = new double[sides][];
            colours[i] = new int[sides];
            for (int j = 0; j < sides; j++) {
                double a = Math.PI * 2.0 * j / sides;
                double r = Math.max(0.0, surface.radius(s, a));
                rings[i][j] = add(f[0], add(mul(f[3], Math.cos(a) * r), mul(f[2], Math.sin(a) * r)));
                colours[i][j] = argb(surface.colour(s, a), alpha);
            }
        }
        for (int i = 0; i < stations; i++) {
            double[] centre = lerp(centres[i], centres[i + 1], 0.5);
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                facet(sink, rings[i][j], rings[i + 1][j], rings[i + 1][k], rings[i][k], centre, colours[i][j], colours[i + 1][j],
                    colours[i + 1][k], colours[i][k]);
            }
        }
    }

    /** The point of {@code spine}'s surface at {@code s}, angle {@code a}, radius {@code r}, and the outward direction there. */
    public static double[][] onSurface(KakujaModel.Spine spine, double s, double a, double r) {
        double[][] f = spine.at(s);
        double[] out = add(mul(f[3], Math.cos(a)), mul(f[2], Math.sin(a)));
        return new double[][] {add(f[0], mul(out, r)), out, f[1]};
    }

    /**
     * A curved horn (a spike, a thorn, a claw, a tooth): from {@code base} out along {@code dir}, bending toward {@code bend} by
     * its end, {@code length} long and {@code radius} thick at the base, tapering to a point; {@code root} colour at the base,
     * {@code tip} at the point.
     */
    public static void horn(UkakuModel.Sink sink, double[] base, double[] dir, double[] bend, double length, double radius, int sides, int stations,
                            int root, int tip, double alpha) {
        if (length < 1.0E-4 || radius < 1.0E-5) {
            return;
        }
        double[] d = normalize(dir);
        double[] b = sub(bend, mul(d, dot(bend, d)));
        b = dot(b, b) < 1.0E-10 ? perpendicular(d) : normalize(b);
        double[][] c = {base, add(base, mul(d, length * 0.4)), add(add(base, mul(d, length * 0.75)), mul(b, length * 0.15)),
            add(add(base, mul(d, length * 0.92)), mul(b, length * 0.42))};
        double[][][] rings = new double[stations + 1][][];
        int[] colour = new int[stations + 1];
        double[] u0 = perpendicular(d);
        double[] v0 = cross(d, u0);
        for (int i = 0; i <= stations; i++) {
            double u = i / (double)stations;
            double[] p = bezier(c, u);
            double r = radius * Math.pow(1.0 - u, 0.9);
            rings[i] = new double[sides][];
            double[] t = normalize(sub(bezier(c, Math.min(1.0, u + 0.03)), bezier(c, Math.max(0.0, u - 0.03))));
            double[] uu = normalize(sub(u0, mul(t, dot(u0, t))));
            double[] vv = cross(t, uu);
            for (int j = 0; j < sides; j++) {
                double a = Math.PI * 2.0 * j / sides;
                rings[i][j] = add(p, add(mul(uu, Math.cos(a) * r), mul(vv, Math.sin(a) * r)));
            }
            colour[i] = argb(mix(root, tip, Math.pow(u, 0.8)), alpha);
        }
        for (int i = 0; i < stations; i++) {
            double[] centre = bezier(c, (i + 0.5) / stations);
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                facet(sink, rings[i][j], rings[i + 1][j], rings[i + 1][k], rings[i][k], centre, colour[i], colour[i + 1], colour[i + 1], colour[i]);
            }
        }
    }

    /** A straight spike (a horn that does not bend). */
    public static void spike(UkakuModel.Sink sink, double[] base, double[] dir, double length, double radius, int sides, int root, int tip, double alpha) {
        horn(sink, base, dir, perpendicular(normalize(dir)), length, radius, sides, 2, root, tip, alpha);
    }

    /** A rough ball: {@code bands} x {@code segments}, its radius at each point jittered by {@code lump} (share) from {@code seed}. */
    public static void blob(UkakuModel.Sink sink, double[] centre, double[] axisX, double[] axisY, double[] axisZ, double rx, double ry, double rz,
                            int bands, int segments, double lump, int seed, java.util.function.DoubleUnaryOperator shade, int light, int dark, double alpha) {
        double[][][] grid = new double[bands + 1][segments][];
        int[][] colours = new int[bands + 1][segments];
        for (int i = 0; i <= bands; i++) {
            double theta = Math.PI * i / bands;
            for (int j = 0; j < segments; j++) {
                double phi = Math.PI * 2.0 * j / segments;
                double n = 1.0 + lump * (noise(seed, i * 0.9, j * 0.9) - 0.5) * 2.0 * Math.sin(theta);
                double x = Math.sin(theta) * Math.cos(phi) * rx * n;
                double y = Math.cos(theta) * ry * n;
                double z = Math.sin(theta) * Math.sin(phi) * rz * n;
                grid[i][j] = add(centre, add(add(mul(axisX, x), mul(axisY, y)), mul(axisZ, z)));
                double light01 = shade.applyAsDouble(Math.cos(theta)) * (0.75 + 0.5 * noise(seed + 7, i * 1.3, j * 1.7));
                colours[i][j] = argb(mix(dark, light, light01), alpha);
            }
        }
        for (int i = 0; i < bands; i++) {
            for (int j = 0; j < segments; j++) {
                int k = (j + 1) % segments;
                facet(sink, grid[i][j], grid[i + 1][j], grid[i + 1][k], grid[i][k], centre, colours[i][j], colours[i + 1][j], colours[i + 1][k],
                    colours[i][k]);
            }
        }
    }

    /**
     * An eye embedded in flesh, looking out along {@code out} at the surface point {@code at}: the ball sunk into the flesh, a
     * glowing red iris round a black slit pupil turned toward {@code look} (a world direction; null to stare straight out), lids
     * of the flesh closing over it as {@code open} goes to 0 (a blink), a thick ring of muscle round it and red veins running off
     * into the flesh. {@code up} orients the slit. {@code stare} widens it (the lids draw back, the pupil narrows).
     */
    public static void eye(UkakuModel.Sink sink, double[] at, double[] out, double[] up, double radius, double[] look, double open, double stare,
                           int flesh, int fleshDark, boolean fine, double alpha) {
        double[] n = normalize(out);
        double[] u = sub(up, mul(n, dot(up, n)));
        u = dot(u, u) < 1.0E-10 ? perpendicular(n) : normalize(u);
        double[] v = cross(n, u);
        double r = radius;
        double[] centre = sub(at, mul(n, r * 0.35));
        int bands = fine ? 8 : 5;
        int segments = fine ? 12 : 8;
        // The gaze: straight out, pulled toward where it looks (never more than about 40 degrees round).
        double[] gaze = n;
        if (look != null && dot(look, look) > 1.0E-8) {
            double[] l = normalize(look);
            double[] side = sub(l, mul(n, dot(l, n)));
            gaze = normalize(add(n, mul(side, 0.85)));
        }
        double[] gu = sub(u, mul(gaze, dot(u, gaze)));
        gu = dot(gu, gu) < 1.0E-10 ? perpendicular(gaze) : normalize(gu);
        double[] gv = cross(gaze, gu);
        double iris = 0.62;
        double pupilWide = 0.2 * (1.0 - 0.6 * clamp01(stare));
        // The ball: black, its iris glowing where it faces the gaze, the slit pupil black in it.
        double[][][] grid = new double[bands + 1][segments][];
        for (int i = 0; i <= bands; i++) {
            double theta = Math.PI * i / bands;
            for (int j = 0; j < segments; j++) {
                double phi = Math.PI * 2.0 * j / segments;
                double[] dir = add(mul(gaze, Math.cos(theta)), add(mul(gu, Math.sin(theta) * Math.cos(phi)), mul(gv, Math.sin(theta) * Math.sin(phi))));
                grid[i][j] = add(centre, mul(dir, r));
            }
        }
        for (int i = 0; i < bands; i++) {
            double t0 = Math.PI * i / bands;
            double t1 = Math.PI * (i + 1) / bands;
            for (int j = 0; j < segments; j++) {
                int k = (j + 1) % segments;
                double phi = Math.PI * 2.0 * (j + 0.5) / segments;
                double tm = (t0 + t1) * 0.5;
                if (tm < iris) {
                    // Inside the iris: the pupil is a slit along the gaze's up.
                    double across = Math.abs(Math.sin(tm) * Math.sin(phi)) / Math.max(1.0E-3, Math.sin(iris));
                    boolean pupil = across < pupilWide / Math.max(0.2, Math.sin(iris)) * 0.9 + 0.03;
                    int c = pupil ? argb(0x050002, alpha) : argb(mix(0xFF3B2A, 0xB00A16, tm / iris), alpha);
                    if (pupil) {
                        facet(sink, grid[i][j], grid[i + 1][j], grid[i + 1][k], grid[i][k], centre, c, c, c, c);
                    } else {
                        glow(sink, grid[i][j], grid[i + 1][j], grid[i + 1][k], grid[i][k], c, c, c, c);
                    }
                } else {
                    int c = argb(mix(0x16060A, 0x050203, (tm - iris) / Math.PI), alpha);
                    facet(sink, grid[i][j], grid[i + 1][j], grid[i + 1][k], grid[i][k], centre, c, c, c, c);
                }
            }
        }
        // The lids: two shells of flesh just over the ball, from its top and bottom toward its middle; open (and staring) they draw
        // back to the socket's rim, shut they meet over the pupil.
        double opening = clamp01(open) * (0.75 + 0.25 * clamp01(stare));
        double reach = Math.PI * 0.5 * (1.0 - 0.72 * opening);
        lid(sink, centre, n, u, v, r * 1.08, reach, fine, flesh, fleshDark, alpha);
        lid(sink, centre, n, mul(u, -1.0), mul(v, -1.0), r * 1.08, reach * 0.85, fine, flesh, fleshDark, alpha);
        // The socket: a thick ring of muscle round it, standing out of the flesh.
        int ring = fine ? 12 : 8;
        int tubeSides = fine ? 5 : 4;
        double ringRadius = r * 1.12;
        double thick = r * 0.32;
        double[] ringCentre = add(at, mul(n, -r * 0.18));
        double[][][] torus = new double[ring][tubeSides][];
        int[][] tones = new int[ring][tubeSides];
        for (int i = 0; i < ring; i++) {
            double a = Math.PI * 2.0 * i / ring;
            double[] radial = add(mul(u, Math.cos(a)), mul(v, Math.sin(a)));
            double bulge = 1.0 + 0.18 * Math.sin(a * 3.0 + 1.3);
            double[] c = add(ringCentre, mul(radial, ringRadius));
            for (int j = 0; j < tubeSides; j++) {
                double b = Math.PI * 2.0 * j / tubeSides;
                double[] d = add(mul(radial, Math.cos(b)), mul(n, Math.sin(b)));
                torus[i][j] = add(c, mul(d, thick * bulge));
                tones[i][j] = argb(mix(fleshDark, flesh, 0.5 + 0.5 * Math.sin(b)), alpha);
            }
        }
        for (int i = 0; i < ring; i++) {
            int i2 = (i + 1) % ring;
            for (int j = 0; j < tubeSides; j++) {
                int j2 = (j + 1) % tubeSides;
                facet(sink, torus[i][j], torus[i2][j], torus[i2][j2], torus[i][j2], add(ringCentre, mul(add(mul(u, Math.cos(Math.PI * 2.0 * (i + 0.5) / ring)),
                    mul(v, Math.sin(Math.PI * 2.0 * (i + 0.5) / ring))), ringRadius)), tones[i][j], tones[i2][j], tones[i2][j2], tones[i][j2]);
            }
        }
        if (fine) {
            // Veins: thin glowing threads running out of the socket into the flesh.
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 0.5 * i + 0.4;
                double[] radial = add(mul(u, Math.cos(a)), mul(v, Math.sin(a)));
                double[] side = add(mul(u, -Math.sin(a)), mul(v, Math.cos(a)));
                double[] p0 = add(ringCentre, mul(radial, ringRadius + thick * 0.9));
                double[] p1 = add(add(p0, mul(radial, r * 0.9)), mul(side, r * 0.25 * (i % 2 == 0 ? 1 : -1)));
                double w = r * 0.05;
                int c0 = argb(0xD0141E, alpha);
                int c1 = argb(0x5A0408, alpha);
                glow(sink, add(p0, mul(side, w)), add(p1, mul(side, w * 0.4)), add(p1, mul(side, -w * 0.4)), add(p0, mul(side, -w)), c0, c1, c1, c0);
            }
        }
    }

    /**
     * A great almond eye lying on a broad face of flesh, as the many-eyed kagune's in the manga: its long axis along {@code along},
     * {@code length} long and {@code height} across, bulging out along {@code out}; a stark white ball (lit as if from within) with a
     * black oval pupil ringed in red that rolls toward {@code look}; a thick black rim of flesh round it; lids closing over it from
     * both sides as {@code open} goes to 0. {@code stare} shrinks the pupil and draws the rims back.
     */
    public static void almond(UkakuModel.Sink sink, double[] at, double[] out, double[] along, double length, double height, double[] look, double open,
                              double stare, int rim, int rimInner, boolean fine, double alpha) {
        double[] n = normalize(out);
        double[] t = sub(along, mul(n, dot(along, n)));
        t = dot(t, t) < 1.0E-10 ? perpendicular(n) : normalize(t);
        double[] b = cross(n, t);
        double half = length * 0.5;
        double tall = height * 0.5;
        double bulge = height * 0.32;
        int cols = fine ? 12 : 7;
        int rows = fine ? 6 : 4;
        double opening = clamp01(open) * (0.85 + 0.15 * clamp01(stare));
        // Where the pupil is (u along, v across, -1..1): rolled toward what it looks at.
        double pu = 0.0;
        double pv = 0.0;
        if (look != null && dot(look, look) > 1.0E-8) {
            double[] l = normalize(look);
            pu = Math.max(-0.5, Math.min(0.5, dot(l, t) * 0.7));
            pv = Math.max(-0.35, Math.min(0.35, dot(l, b) * 0.5));
        }
        double pr = 0.36 * (1.0 - 0.35 * clamp01(stare));
        double[][][] grid = new double[cols + 1][rows + 1][];
        double[][] vmax = new double[cols + 1][];
        for (int i = 0; i <= cols; i++) {
            double u = -1.0 + 2.0 * i / cols;
            double edge = Math.pow(Math.max(0.0, 1.0 - u * u), 0.75);
            vmax[i] = new double[] {edge};
            for (int j = 0; j <= rows; j++) {
                double v = (-1.0 + 2.0 * j / rows) * edge * opening;
                double dome = (1.0 - u * u) * (1.0 - (edge < 1.0E-6 ? 1.0 : (v / edge) * (v / edge)));
                grid[i][j] = add(at, add(add(mul(t, u * half), mul(b, v * tall)), mul(n, bulge * Math.max(0.0, dome) + 0.04 * height)));
            }
        }
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                double uc = -1.0 + 2.0 * (i + 0.5) / cols;
                double vc = (-1.0 + 2.0 * (j + 0.5) / rows) * opening * Math.pow(Math.max(0.0, 1.0 - uc * uc), 0.75);
                int c = argb(mix(0xF3EEE2, 0xBDB3A2, Math.abs(vc) * 0.9 + 0.25 * uc * uc), alpha);
                glow(sink, grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1], c, c, c, c);
            }
        }
        // The pupil: a black oval ringed in red, lying just over the ball where it looks, never past the lids.
        if (opening > 0.12) {
            int ring = fine ? 16 : 10;
            double ru = pr * 0.8;
            double rv = Math.min(pr, opening * 0.9);
            double lift = bulge * Math.max(0.0, (1.0 - pu * pu) * (1.0 - pv * pv)) + 0.07 * height;
            double[] centre = add(at, add(add(mul(t, pu * half), mul(b, pv * tall)), mul(n, lift)));
            int red = argb(0xB0101C, alpha);
            int black = argb(0x020001, alpha);
            for (int k = 0; k < ring; k++) {
                double a0 = Math.PI * 2.0 * k / ring;
                double a1 = Math.PI * 2.0 * (k + 1) / ring;
                double[] o0 = add(centre, add(mul(t, Math.cos(a0) * ru * half * 1.35), mul(b, Math.sin(a0) * rv * tall * 1.35)));
                double[] o1 = add(centre, add(mul(t, Math.cos(a1) * ru * half * 1.35), mul(b, Math.sin(a1) * rv * tall * 1.35)));
                double[] i0 = add(add(centre, add(mul(t, Math.cos(a0) * ru * half), mul(b, Math.sin(a0) * rv * tall))), mul(n, 0.01 * height));
                double[] i1 = add(add(centre, add(mul(t, Math.cos(a1) * ru * half), mul(b, Math.sin(a1) * rv * tall))), mul(n, 0.01 * height));
                double[] mid = add(centre, mul(n, 0.012 * height));
                glow(sink, o0, o1, i1, i0, red, red, red, red);
                facet(sink, mid, mid, i1, i0, sub(at, mul(n, height)), black, black, black, black);
            }
        }
        // The rim: a thick lip of black flesh standing round the opening, its inner edge red-raw; past the opening, the lids.
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < cols; i++) {
                double u0 = -1.0 + 2.0 * i / cols;
                double u1 = -1.0 + 2.0 * (i + 1) / cols;
                double e0 = vmax[i][0];
                double e1 = vmax[i + 1][0];
                double[] inner0 = add(at, add(add(mul(t, u0 * half), mul(b, side * e0 * opening * tall)), mul(n, 0.06 * height)));
                double[] inner1 = add(at, add(add(mul(t, u1 * half), mul(b, side * e1 * opening * tall)), mul(n, 0.06 * height)));
                double[] lid0 = add(at, add(add(mul(t, u0 * half * 1.04), mul(b, side * e0 * tall * 1.02)), mul(n, 0.28 * height)));
                double[] lid1 = add(at, add(add(mul(t, u1 * half * 1.04), mul(b, side * e1 * tall * 1.02)), mul(n, 0.28 * height)));
                double[] outer0 = add(at, add(add(mul(t, u0 * half * 1.18), mul(b, side * (e0 * tall * 1.25 + 0.08 * height))), mul(n, 0.0)));
                double[] outer1 = add(at, add(add(mul(t, u1 * half * 1.18), mul(b, side * (e1 * tall * 1.25 + 0.08 * height))), mul(n, 0.0)));
                double[] centre = sub(at, mul(n, height));
                facet(sink, inner0, inner1, lid1, lid0, centre, argb(rimInner, alpha), argb(rimInner, alpha), argb(rim, alpha), argb(rim, alpha));
                facet(sink, lid0, lid1, outer1, outer0, centre, argb(rim, alpha), argb(rim, alpha), argb(mix(rim, 0x000000, 0.4), alpha),
                    argb(mix(rim, 0x000000, 0.4), alpha));
            }
        }
    }

    /** One lid: a shell of flesh round the ball's {@code u} pole, reaching {@code reach} radians down toward its middle. */
    static void lid(UkakuModel.Sink sink, double[] centre, double[] n, double[] u, double[] v, double r, double reach, boolean fine, int flesh, int dark,
                    double alpha) {
        int rows = fine ? 4 : 2;
        int cols = fine ? 8 : 5;
        // In the ball's frame with u as the pole: theta from the pole, phi round it, the visible half (toward n) only.
        double[][][] grid = new double[rows + 1][cols + 1][];
        for (int i = 0; i <= rows; i++) {
            double theta = reach * i / rows;
            for (int j = 0; j <= cols; j++) {
                double phi = -Math.PI * 0.5 + Math.PI * j / cols;
                double[] dir = add(mul(u, Math.cos(theta)), add(mul(n, Math.sin(theta) * Math.cos(phi)), mul(v, Math.sin(theta) * Math.sin(phi))));
                grid[i][j] = add(centre, mul(dir, r * (1.0 + 0.05 * (rows - i) / rows)));
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int c0 = argb(mix(dark, flesh, 0.35 + 0.5 * i / rows), alpha);
                int c1 = argb(mix(dark, flesh, 0.35 + 0.5 * (i + 1) / rows), alpha);
                facet(sink, grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1], centre, c0, c1, c1, c0);
            }
        }
    }

    /** A row of jagged teeth from {@code a} to {@code b}, pointing along {@code toward}, {@code count} of them, lengths varied by seed. */
    public static void teeth(UkakuModel.Sink sink, double[] a, double[] b, double[] toward, int count, double length, double radius, int seed, double alpha) {
        for (int i = 0; i < count; i++) {
            double f = (i + 0.5) / count;
            double[] base = lerp(a, b, f);
            double vary = 0.65 + 0.7 * hash(seed + i * 13);
            double lean = (hash(seed + i * 7 + 3) - 0.5) * 0.5;
            double[] dir = normalize(add(toward, mul(normalize(sub(b, a)), lean)));
            horn(sink, base, dir, sub(b, a), length * vary, radius * (0.8 + 0.4 * hash(seed + i)), 4, 2, 0xB9A992, 0xF4EDE0, alpha);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** A shaded quad, its true normal turned away from {@code centre}. */
    public static void facet(UkakuModel.Sink sink, double[] a, double[] b, double[] c, double[] d, double[] centre, int ca, int cb, int cc, int cd) {
        double[] e1 = sub(c, a);
        double[] e2 = sub(d, b);
        double[] n = crossRaw(e1, e2);
        double length = Math.sqrt(dot(n, n));
        if (length < 1.0E-12) {
            n = crossRaw(sub(b, a), sub(d, a));
            length = Math.sqrt(dot(n, n));
            if (length < 1.0E-12) {
                n = crossRaw(sub(c, b), sub(d, b));
                length = Math.sqrt(dot(n, n));
                if (length < 1.0E-12) {
                    return;
                }
            }
        }
        n = mul(n, 1.0 / length);
        double[] mid = {(a[0] + b[0] + c[0] + d[0]) / 4.0 - centre[0], (a[1] + b[1] + c[1] + d[1]) / 4.0 - centre[1],
            (a[2] + b[2] + c[2] + d[2]) / 4.0 - centre[2]};
        if (dot(mid, n) < 0.0) {
            n = mul(n, -1.0);
        }
        // Never exactly straight up: that is the glow convention.
        if (n[0] == 0.0 && n[2] == 0.0) {
            n = normalize(new double[] {1.0E-4, n[1], 0.0});
        }
        sink.quad(a, b, c, d, n, ca, cb, cc, cd);
    }

    /** A glowing quad (RC light, drawn at full brightness). */
    public static void glow(UkakuModel.Sink sink, double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
        sink.quad(a, b, c, d, new double[] {0.0, 1.0, 0.0}, ca, cb, cc, cd);
    }

    /** A smooth value noise in 0..1. */
    public static double noise(int seed, double x, double y) {
        int x0 = (int)Math.floor(x);
        int y0 = (int)Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        double sx = fx * fx * (3.0 - 2.0 * fx);
        double sy = fy * fy * (3.0 - 2.0 * fy);
        double a = hash(seed * 7919 + x0 * 374761 + y0 * 668265);
        double b = hash(seed * 7919 + (x0 + 1) * 374761 + y0 * 668265);
        double c = hash(seed * 7919 + x0 * 374761 + (y0 + 1) * 668265);
        double d = hash(seed * 7919 + (x0 + 1) * 374761 + (y0 + 1) * 668265);
        return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy;
    }

    public static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double)(1L << 53);
    }

    public static int argb(int rgb, double alpha) {
        int a = (int)Math.round(clamp01(alpha) * 255.0);
        return a << 24 | (rgb & 0xFFFFFF);
    }

    public static int mix(int from, int to, double amount) {
        double t = clamp01(amount);
        int r = (int)Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int)Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int)Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return r << 16 | g << 8 | b;
    }

    public static double smooth(double value) {
        double t = clamp01(value);
        return t * t * (3.0 - 2.0 * t);
    }

    public static double clamp01(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }

    static double[] bezier(double[][] c, double u) {
        double w = 1.0 - u;
        double a = w * w * w;
        double b = 3.0 * w * w * u;
        double d = 3.0 * w * u * u;
        double e = u * u * u;
        return new double[] {a * c[0][0] + b * c[1][0] + d * c[2][0] + e * c[3][0], a * c[0][1] + b * c[1][1] + d * c[2][1] + e * c[3][1],
            a * c[0][2] + b * c[1][2] + d * c[2][2] + e * c[3][2]};
    }

    public static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    public static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    public static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    public static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    public static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static double length3(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    static double[] crossRaw(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    public static double[] cross(double[] a, double[] b) {
        return normalize(crossRaw(a, b));
    }

    public static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / length);
    }

    public static double[] perpendicular(double[] t) {
        double[] axis = Math.abs(t[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        return normalize(sub(axis, mul(t, dot(axis, t))));
    }

    /** {@code v} turned about the unit {@code axis} by {@code angle} (Rodrigues). */
    public static double[] rotate(double[] v, double[] axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double[] k = normalize(axis);
        return add(add(mul(v, c), mul(crossRaw(k, v), s)), mul(k, dot(k, v) * (1.0 - c)));
    }
}
