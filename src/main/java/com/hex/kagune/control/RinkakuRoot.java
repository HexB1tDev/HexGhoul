package com.hex.kagune.control;

/**
 * 2.0.38 (pure, tested; the owner: "attach properly to the body, like actually attach to it, not just glide behind it. Like it has to be
 * actually part of the body"): a Rinkaku tail grown out of its owner's lower back. The original anchors every tail at a point hanging just
 * behind the hips and lets it fall from there like a rope; here each tail's root is sunk inside the lower back ({@link #ROOT_Y},
 * {@link #ROOT_Z}: the tube's start hidden in the body), and its first {@link #BASE} blocks leave the back as a rigid base, fanned out to
 * its own side and lane ({@link #direction}), before it blends into the path its motion gave it ({@link #attach}).
 *
 * <p>Points are in the owner's own frame from his feet: {x right, y up, z forward}, blocks, at a player's size ({@code scale} for others).
 */
public final class RinkakuRoot {
    private RinkakuRoot() {
    }

    /** The root: this high up the body (of its height: the lower back, above the belt) and this far behind its middle (inside the torso). */
    public static final double ROOT_Y = 0.5;
    public static final double ROOT_Z = -0.05;
    /** The back's surface (half the torso's depth behind its middle). */
    public static final double BACK = -0.125;
    /** How long the rigid base is (blocks), and where the tail is wholly its own motion's again (in bases). */
    public static final double BASE = 0.42;
    public static final double FREE = 1.7;
    /** How far round to its side a tail leaves the back (radians, the first pair), and how much more each pair after. */
    public static final double OUT = Math.toRadians(24.0);
    public static final double OUT_STEP = Math.toRadians(10.0);

    /** The side ({@code +1} right, {@code -1} left) of tail {@code limb}: its layout's own ({@code fanSide}), else alternating. */
    public static double side(int limb, double fanSide) {
        return Math.abs(fanSide) >= 0.01 ? Math.signum(fanSide) : limb % 2 == 0 ? -1.0 : 1.0;
    }

    /**
     * The way tail {@code limb} leaves the back (unit, owner's frame): backwards, round to its {@code side}, the first pair rising a little,
     * the next level, the next falling (up to six tails), each pair a little further round.
     */
    public static double[] direction(int limb, double side) {
        int pair = Math.max(0, limb) / 2;
        double out = OUT + OUT_STEP * pair;
        double rise = Math.toRadians(new double[] {16.0, -4.0, -20.0}[Math.min(2, pair)]);
        double c = Math.cos(rise);
        return new double[] {side * Math.sin(out) * c, Math.sin(rise), -Math.cos(out) * c};
    }

    /** Where tail {@code limb}'s root is sunk (owner's frame) for a body {@code height} tall: a little to its side, inside the lower back. */
    public static double[] root(int limb, double side, double height) {
        int pair = Math.max(0, limb) / 2;
        double scale = height / 1.8;
        return new double[] {side * (0.03 + 0.015 * pair) * scale, height * ROOT_Y - 0.03 * pair * scale, ROOT_Z * scale};
    }

    /**
     * {@code path} (owner's frame, root first) grown out of the lower back: its root moved to {@link #root} (the move fading out along the
     * tail, so its far part stays where its motion put it), and its first part laid along the rigid base ({@link #direction}) from there,
     * blended back into the path past it. A new array; {@code path} is untouched.
     */
    public static double[][] attach(double[][] path, int limb, double fanSide, double height) {
        int n = path.length;
        if (n < 2) {
            return path;
        }
        double scale = height / 1.8;
        double side = side(limb, fanSide);
        double[] root = root(limb, side, height);
        double[] way = direction(limb, side);
        double base = BASE * scale;
        double[] arc = new double[n];
        for (int i = 1; i < n; i++) {
            arc[i] = arc[i - 1] + distance(path[i], path[i - 1]);
        }
        double total = Math.max(1.0E-6, arc[n - 1]);
        double[] shift = {root[0] - path[0][0], root[1] - path[0][1], root[2] - path[0][2]};
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            double carry = 1.0 - smooth(arc[i] / (total * 0.55));
            double[] moved = {path[i][0] + shift[0] * carry, path[i][1] + shift[1] * carry, path[i][2] + shift[2] * carry};
            double s = Math.min(arc[i], base * FREE);
            double[] laid = {root[0] + way[0] * s, root[1] + way[1] * s, root[2] + way[2] * s};
            double hold = 1.0 - smooth((arc[i] - base * 0.5) / (base * (FREE - 0.5)));
            out[i] = new double[] {moved[0] + (laid[0] - moved[0]) * hold, moved[1] + (laid[1] - moved[1]) * hold,
                moved[2] + (laid[2] - moved[2]) * hold};
        }
        return out;
    }

    static double smooth(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }

    static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
