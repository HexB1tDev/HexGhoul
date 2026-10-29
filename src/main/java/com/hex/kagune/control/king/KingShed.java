package com.hex.kagune.control.king;

/**
 * 1.0.19: War leaving him (pure): the kakuja dies on him and crumbles away. Every quad of it, as it was built the moment he took it off,
 * lets go in its own time, the farthest from his spine first (the rinkaku from their tips in), his chest last, a little scattered; then
 * over {@link #CRUMBLE} ticks it falls, tumbles, drifts off its face, shrinks to nothing and goes to ash.
 *
 * <p>Quads are as {@code KingLayer} keeps them: 20 floats, four corners (the part's own pixels: x left, y down from its pivot, z back),
 * the normal, four colours (ARGB as floats), and 1 for a glow.
 */
public final class KingShed {
    /** How long a piece takes to crumble away once it lets go (ticks). */
    public static final double CRUMBLE = 9.0;
    /** The first piece lets go this long after it was taken off, the last by this (before the scatter). */
    static final double FIRST = 2.0;
    static final double LAST = 20.0;
    /** How fast a falling piece gathers speed (pixels a tick, squared). */
    static final double FALL = 0.28;
    /** Ash: what every piece goes to. */
    static final int ASH = 0xFF161414;
    /** Where his spine is (body pixels): what lets go last is nearest it. */
    static final double[] SPINE = {0.0, 19.0, 0.5};

    private KingShed() {
    }

    /** When a piece whose middle is at body pixels {@code body} lets go (ticks after it was taken off); {@code scatter} 0..1. */
    public static double release(double[] body, double scatter) {
        double dx = body[0] - SPINE[0];
        double dy = body[1] - SPINE[1];
        double dz = body[2] - SPINE[2];
        double far = Math.min(1.0, Math.sqrt(dx * dx + dy * dy + dz * dz) / 32.0);
        return FIRST + (LAST - FIRST) * (1.0 - far) + 3.0 * scatter;
    }

    /**
     * Quad {@code q} of {@code part}, {@code age} ticks after War was taken off, as it is now, written into {@code out} (20 floats);
     * false when it has crumbled away.
     */
    public static boolean shed(float[] q, WarModel.Part part, double age, float[] out) {
        double cx = (q[0] + q[3] + q[6] + q[9]) * 0.25;
        double cy = (q[1] + q[4] + q[7] + q[10]) * 0.25;
        double cz = (q[2] + q[5] + q[8] + q[11]) * 0.25;
        double[] pivot = KingGrowth.pivot(part);
        int h = (int)Math.floor(cx * 2.0) * 73856093 ^ (int)Math.floor(cy * 2.0) * 19349663 ^ (int)Math.floor(cz * 2.0) * 83492791;
        double scatter = WarCloth.hash(h);
        double t0 = release(new double[] {pivot[0] + cx, pivot[1] - cy, pivot[2] + cz}, scatter);
        double t = age - t0;
        if (t >= CRUMBLE) {
            return false;
        }
        boolean glow = q[19] != 0.0F;
        if (t <= 0.0) {
            // Still on him, dying: darkening toward the moment it lets go (what burns dims).
            double dim = 0.45 * clamp(1.0 - (t0 - age) / 6.0);
            System.arraycopy(q, 0, out, 0, 20);
            for (int k = 0; k < 4; k++) {
                out[15 + k] = (float)(glow ? fade((int)q[15 + k], 1.0 - dim) : mix((int)q[15 + k], ASH, dim));
            }
            return true;
        }
        double s = t / CRUMBLE;
        double shrink = Math.pow(1.0 - s, 0.8);
        // It falls (down its part: y is down), drifts off its own face, and tumbles round its middle.
        double drop = 0.5 * FALL * t * t;
        double drift = 1.6 * s + 0.6 * (scatter - 0.5);
        double spin = (scatter - 0.5) * 3.2 * s;
        double cos = Math.cos(spin);
        double sin = Math.sin(spin);
        for (int k = 0; k < 4; k++) {
            double x = (q[k * 3] - cx) * shrink;
            double y = (q[k * 3 + 1] - cy) * shrink;
            double z = (q[k * 3 + 2] - cz) * shrink;
            double ry = y * cos - z * sin;
            double rz = y * sin + z * cos;
            out[k * 3] = (float)(cx + x + q[12] * drift);
            out[k * 3 + 1] = (float)(cy + ry + q[13] * drift + drop);
            out[k * 3 + 2] = (float)(cz + rz + q[14] * drift);
        }
        out[12] = q[12];
        out[13] = q[13];
        out[14] = q[14];
        double ash = clamp(0.45 + s * 1.4);
        for (int k = 0; k < 4; k++) {
            out[15 + k] = (float)(glow ? fade((int)q[15 + k], 0.55 * (1.0 - s)) : mix((int)q[15 + k], ASH, ash));
        }
        out[19] = q[19];
        return true;
    }

    /** True once every piece of it is gone, {@code age} ticks after it was taken off. */
    public static boolean gone(double age) {
        return age >= LAST + 3.0 + CRUMBLE;
    }

    static int mix(int a, int b, double t) {
        int r = (int)Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int)Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int)Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return a & 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** A glow's colour dimmed to {@code k} (it is added onto what is behind it). */
    static int fade(int argb, double k) {
        k = clamp(k);
        int r = (int)Math.round(((argb >> 16) & 0xFF) * k);
        int g = (int)Math.round(((argb >> 8) & 0xFF) * k);
        int b = (int)Math.round((argb & 0xFF) * k);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    static double clamp(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }
}
