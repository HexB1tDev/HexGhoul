package com.hex.kagune.control.king;

import com.hex.kagune.control.ayato.UkakuModel;

/**
 * 1.0.17: a final kakuja growing out of his skin ({@link KingTransform#growth}): every corner of what is built on a body part is let
 * out from the nearest point of that part's own skin as far as the growth has reached it, so plates rise out of him and swell, and
 * what reaches far (the rinkaku, the scythe) comes out of him from where it roots. The growth runs out from his heart: a corner whose
 * root is farther from it comes later. War's tears out shuddering (each corner thrown about a little as it comes); Peace's swells.
 */
public final class KingGrowth {
    /** His heart (body pixels), where the growth begins. */
    static final double[] HEART = {0.8, 19.0, -1.2};

    private KingGrowth() {
    }

    /** {@code out} with every corner of part {@code part} let out as far as growth {@code g} ({front, swell, jitter}) has reached. */
    public static UkakuModel.Sink wrap(UkakuModel.Sink out, WarModel.Part part, double[] g) {
        if (g == null) {
            return out;
        }
        double[] pivot = pivot(part);
        double[][] box = box(part);
        return (a, b, c, d, n, ca, cb, cc, cd) -> {
            double[] s = new double[1];
            double[] qa = corner(a, pivot, box, g, s);
            double[] qb = corner(b, pivot, box, g, s);
            double[] qc = corner(c, pivot, box, g, s);
            double[] qd = corner(d, pivot, box, g, s);
            if (s[0] < 0.03) {
                return; // still all in his skin: nothing of it yet (and nothing flat on the skin to flicker)
            }
            out.quad(qa, qb, qc, qd, n, ca, cb, cc, cd);
        };
    }

    /** Corner {@code p} (the part's pixels: x left, y down from its pivot, z back) as far out of the skin as the growth has it. */
    static double[] corner(double[] p, double[] pivot, double[][] box, double[] g, double[] most) {
        double[] root = {clamp(p[0], box[0][0], box[1][0]), clamp(p[1], box[0][1], box[1][1]), clamp(p[2], box[0][2], box[1][2])};
        double[] body = {pivot[0] + root[0], pivot[1] - root[1], pivot[2] + root[2]};
        double dx = body[0] - HEART[0];
        double dy = body[1] - HEART[1];
        double dz = body[2] - HEART[2];
        double s = KingTransform.out(Math.sqrt(dx * dx + dy * dy + dz * dz), g);
        boolean onSkin = root[0] == p[0] && root[1] == p[1] && root[2] == p[2];
        most[0] = Math.max(most[0], onSkin ? 0.0 : s);
        double[] q = {root[0] + (p[0] - root[0]) * s, root[1] + (p[1] - root[1]) * s, root[2] + (p[2] - root[2]) * s};
        if (g[2] > 0.0) {
            // Torn out, not grown: each corner shaken a little about where it is going, by how far out of him it is.
            double reach = Math.sqrt((p[0] - root[0]) * (p[0] - root[0]) + (p[1] - root[1]) * (p[1] - root[1]) + (p[2] - root[2]) * (p[2] - root[2]));
            double k = g[2] * Math.min(2.5, 0.35 * reach) * s;
            int h = (int)Math.floor(p[0] * 3.0) * 73856093 ^ (int)Math.floor(p[1] * 3.0) * 19349663 ^ (int)Math.floor(p[2] * 3.0) * 83492791;
            q[0] += (WarCloth.hash(h) - 0.5) * k;
            q[1] += (WarCloth.hash(h + 1) - 0.5) * k;
            q[2] += (WarCloth.hash(h + 2) - 0.5) * k;
        }
        return q;
    }

    /** Where part {@code part}'s pivot is (body pixels: x left, y up from the feet, z back). */
    static double[] pivot(WarModel.Part part) {
        return switch (part) {
            case HEAD, TORSO -> new double[] {0.0, 24.0, 0.0};
            case RIGHT_ARM -> new double[] {-5.0, 22.0, 0.0};
            case LEFT_ARM -> new double[] {5.0, 22.0, 0.0};
            case RIGHT_LEG -> new double[] {-1.9, 12.0, 0.0};
            case LEFT_LEG -> new double[] {1.9, 12.0, 0.0};
        };
    }

    /** Part {@code part}'s own skin, its box (its pixels): {min, max}. */
    static double[][] box(WarModel.Part part) {
        return switch (part) {
            case HEAD -> new double[][] {{-4, -8, -4}, {4, 0, 4}};
            case TORSO -> new double[][] {{-4, 0, -2}, {4, 12, 2}};
            case RIGHT_ARM -> new double[][] {{-3, -2, -2}, {1, 10, 2}};
            case LEFT_ARM -> new double[][] {{-1, -2, -2}, {3, 10, 2}};
            case RIGHT_LEG, LEFT_LEG -> new double[][] {{-2, 0, -2}, {2, 12, 2}};
        };
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
