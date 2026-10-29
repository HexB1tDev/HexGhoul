package com.hex.kagune.control.client.animation;

/**
 * 6.2.0: her own right leg as she sees it in first person (pure, tested). In first person the player model is never drawn, so a
 * move that swings the leg up in front of her (the Flesh Hammer's raise) would show nothing of it; this is the leg, posed and bent
 * at the knee exactly as the model draws it ({@link BodyFrame}), its skin's leg and trouser layers mapped on as the game maps them.
 *
 * <p>The camera stays where her eyes are when she stands, however her body tips; the leg (and what grows round it) is drawn as she
 * would see it from where her head has gone ({@link #shift}): tipped right back under a leg raised nearly upright, the camera
 * would otherwise sit right on her foot.
 */
public final class EtoOwnLeg {
    /** Where her eyes are on her head (head pixels: halfway up it, on its middle line). */
    static final double[] EYES = {0.0, -4.0, 0.0};
    /** How far (pixels) the trouser layer stands off the leg, as the skin's second layer does. */
    static final double OVERLAY = 0.25;
    /** The rows the leg is cut into along its length (pixels from the hip): the knee bends at the middle. */
    static final double[] ROWS = {0.0, 3.0, 6.0, 9.0, 12.0};

    private EtoOwnLeg() {
    }

    /** One face of the leg: corners in rig space (blocks), its outward normal, and each corner's place on the skin (0..1). */
    public interface Sink {
        void quad(double[] a, double[] b, double[] c, double[] d, double[] normal, float[] uv, boolean trousers);
    }

    /**
     * What to add (rig space, blocks) to anything drawn on {@code body} to see it from where its head has gone, the camera being
     * where her eyes are when she stands: her head's way from there, taken off.
     */
    public static double[] shift(RigPose body) {
        double[] standing = BodyFrame.world(new RigPose(), BodyFrame.model(new RigPose(), "head", EYES));
        double[] posed = BodyFrame.world(body, BodyFrame.model(body, "head", EYES));
        return new double[] {standing[0] - posed[0], standing[1] - posed[1], standing[2] - posed[2]};
    }

    /** Her right leg on {@code body}, moved by {@code shift} (blocks): the leg, then its trouser layer over it. */
    public static void mesh(RigPose body, double[] shift, Sink sink) {
        box(body, shift, 0.0, 0.0, 16.0, false, sink);
        box(body, shift, OVERLAY, 0.0, 32.0, true, sink);
    }

    /**
     * The leg's box grown {@code grow} pixels each way, its skin at ({@code u}, {@code v}) (texture pixels, a 64x64 skin), as
     * vanilla's model lays a box's six faces out: the hip end and the sole along the top, then round the sides (her outer side,
     * the front, the inner side, the back).
     */
    static void box(RigPose body, double[] shift, double grow, double u, double v, boolean trousers, Sink sink) {
        limb(body, "rightLeg", shift, -2.0, 2.0, ROWS, grow, u, v, trousers, sink);
    }

    /**
     * 6.2.0: a right arm or leg's box ({@code part}, its own pixels from {@code left} to {@code right} across, 4 deep, cut into
     * {@code rows} along its length, bent at the middle as the model bends it) grown {@code grow} pixels each way, its skin at
     * ({@code u}, {@code v}) (texture pixels, a 64x64 skin), as vanilla's model lays a box's six faces out.
     */
    static void limb(RigPose body, String part, double[] shift, double left, double right, double[] rows, double grow, double u, double v,
                     boolean trousers, Sink sink) {
        double x0 = left - grow;
        double x1 = right + grow;
        double z0 = -2.0 - grow;
        double z1 = 2.0 + grow;
        double w = right - left;
        double d = 4.0;
        double first = rows[0];
        double middle = (left + right) / 2.0;
        // Model pixels: x to her left, y down from the hip, z backward; the front is z0, her outer (right) side x0.
        for (int r = 0; r + 1 < rows.length; r++) {
            double ya = r == 0 ? rows[r] - grow : rows[r];
            double yb = r + 2 == rows.length ? rows[r + 1] + grow : rows[r + 1];
            double va = v + d + rows[r] - first;
            double vb = v + d + rows[r + 1] - first;
            // The front: her right edge at u + d.
            face(body, part, middle, shift, sink, trousers, new double[][] {{x0, ya, z0}, {x1, ya, z0}, {x1, yb, z0}, {x0, yb, z0}},
                new double[] {u + d, va, u + d + w, va, u + d + w, vb, u + d, vb});
            // Her inner (left) side: from the front edge round to the back.
            face(body, part, middle, shift, sink, trousers, new double[][] {{x1, ya, z0}, {x1, ya, z1}, {x1, yb, z1}, {x1, yb, z0}},
                new double[] {u + d + w, va, u + 2 * d + w, va, u + 2 * d + w, vb, u + d + w, vb});
            // The back: from her left edge round to her right.
            face(body, part, middle, shift, sink, trousers, new double[][] {{x1, ya, z1}, {x0, ya, z1}, {x0, yb, z1}, {x1, yb, z1}},
                new double[] {u + 2 * d + w, va, u + 2 * d + 2 * w, va, u + 2 * d + 2 * w, vb, u + 2 * d + w, vb});
            // Her outer (right) side: from the back edge round to the front.
            face(body, part, middle, shift, sink, trousers, new double[][] {{x0, ya, z1}, {x0, ya, z0}, {x0, yb, z0}, {x0, yb, z1}},
                new double[] {u, va, u + d, va, u + d, vb, u, vb});
        }
        double top = rows[0] - grow;
        double sole = rows[rows.length - 1] + grow;
        // The hip (or shoulder) end (the back of it toward v) and the sole (or the hand's end).
        face(body, part, middle, shift, sink, trousers, new double[][] {{x0, top, z1}, {x1, top, z1}, {x1, top, z0}, {x0, top, z0}},
            new double[] {u + d, v, u + d + w, v, u + d + w, v + d, u + d, v + d});
        face(body, part, middle, shift, sink, trousers, new double[][] {{x0, sole, z0}, {x1, sole, z0}, {x1, sole, z1}, {x0, sole, z1}},
            new double[] {u + d + w, v, u + d + 2 * w, v, u + d + 2 * w, v + d, u + d + w, v + d});
    }

    /**
     * One quad of the limb (its own pixels, unbent, its middle line at {@code middle} across) posed into rig space and moved; {@code uv}
     * in texture pixels.
     */
    static void face(RigPose body, String part, double middle, double[] shift, Sink sink, boolean trousers, double[][] local, double[] uv) {
        double[][] p = new double[4][];
        for (int i = 0; i < 4; i++) {
            double[] q = BodyFrame.world(body, BodyFrame.model(body, part, local[i]));
            p[i] = new double[] {q[0] + shift[0], q[1] + shift[1], q[2] + shift[2]};
        }
        double[] e1 = {p[2][0] - p[0][0], p[2][1] - p[0][1], p[2][2] - p[0][2]};
        double[] e2 = {p[3][0] - p[1][0], p[3][1] - p[1][1], p[3][2] - p[1][2]};
        double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
        // Outward: away from the leg's own middle line.
        double[] mid = BodyFrame.world(body, BodyFrame.model(body, part, new double[] {middle, (local[0][1] + local[2][1]) / 2.0, 0.0}));
        double[] centre = {(p[0][0] + p[2][0]) / 2.0 - shift[0] - mid[0], (p[0][1] + p[2][1]) / 2.0 - shift[1] - mid[1],
            (p[0][2] + p[2][2]) / 2.0 - shift[2] - mid[2]};
        if (n[0] * centre[0] + n[1] * centre[1] + n[2] * centre[2] < 0.0) {
            n = new double[] {-n[0], -n[1], -n[2]};
        }
        double l = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        n = l < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : new double[] {n[0] / l, n[1] / l, n[2] / l};
        float[] skin = new float[8];
        for (int i = 0; i < 8; i++) {
            skin[i] = (float)(uv[i] / 64.0);
        }
        sink.quad(p[0], p[1], p[2], p[3], n, skin, trousers);
    }
}
