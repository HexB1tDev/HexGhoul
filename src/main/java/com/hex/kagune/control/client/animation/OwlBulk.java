package com.hex.kagune.control.client.animation;

/**
 * 6.2.0: her body bulked up as HexHaki's haki physique draws it (the Owl; pure, tested): a second chest over the top six pixels of
 * her torso, broader and deeper ({@link RigPose#BULK_CHEST}, {@link RigPose#BULK_CHEST_DEPTH} at the full bulk), in her own skin:
 * the torso's and the jacket layer's faces mapped on as the game maps them. (Her arms are pushed out by the pose itself and drawn
 * thicker by the model's own scale.)
 */
public final class OwlBulk {
    /** How far (pixels) the jacket layer stands off, as the skin's second layer does. */
    static final double OVERLAY = 0.25;
    /** How tall (pixels, down from the neck) the chest is. */
    public static final double HEIGHT = 6.0;

    private OwlBulk() {
    }

    /** The chest on {@code body} at its bulk ({@code body.bulk}): its skin, then the jacket layer over it. */
    public static void chest(RigPose body, PaleOwnLeg.Sink sink) {
        double b = Float.isFinite(body.bulk) ? Math.max(0.0, Math.min(1.0, body.bulk)) : 0.0;
        double sx = 1.0 + RigPose.BULK_CHEST * b;
        double sz = 1.0 + RigPose.BULK_CHEST_DEPTH * b;
        box(body, sx, sz, 0.0, 16.0, 16.0, false, sink);
        box(body, sx, sz, OVERLAY, 16.0, 32.0, true, sink);
    }

    /**
     * The chest's box grown {@code grow} pixels each way and scaled ({@code sx} across, {@code sz} front to back) about her middle,
     * its skin at ({@code u}, {@code v}) (a 64x64 skin; the torso's 8x12x4 box, its top six rows).
     */
    static void box(RigPose body, double sx, double sz, double grow, double u, double v, boolean jacket, PaleOwnLeg.Sink sink) {
        double x0 = (-4.0 - grow) * sx;
        double x1 = (4.0 + grow) * sx;
        double z0 = (-2.0 - grow) * sz;
        double z1 = (2.0 + grow) * sz;
        double y0 = -grow;
        double y1 = HEIGHT + grow;
        double w = 8.0;
        double d = 4.0;
        double va = v + d;
        double vb = v + d + HEIGHT;
        // Model pixels: x to her left, y down from the neck, z backward; the front is z0, her right side x0.
        face(body, sink, jacket, new double[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}},
            new double[] {u + d, va, u + d + w, va, u + d + w, vb, u + d, vb});
        face(body, sink, jacket, new double[][] {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}},
            new double[] {u + d + w, va, u + 2 * d + w, va, u + 2 * d + w, vb, u + d + w, vb});
        face(body, sink, jacket, new double[][] {{x1, y0, z1}, {x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}},
            new double[] {u + 2 * d + w, va, u + 2 * d + 2 * w, va, u + 2 * d + 2 * w, vb, u + 2 * d + w, vb});
        face(body, sink, jacket, new double[][] {{x0, y0, z1}, {x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}},
            new double[] {u, va, u + d, va, u + d, vb, u, vb});
        // The top (her shoulders), and the underside of the chest where it stands out over the waist.
        face(body, sink, jacket, new double[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}},
            new double[] {u + d, v, u + d + w, v, u + d + w, v + d, u + d, v + d});
        face(body, sink, jacket, new double[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}},
            new double[] {u + d + w, v, u + d + 2 * w, v, u + d + 2 * w, v + d, u + d + w, v + d});
    }

    /** One quad of the chest (torso pixels) posed into rig space; its normal away from the chest's middle; {@code uv} in texture pixels. */
    static void face(RigPose body, PaleOwnLeg.Sink sink, boolean jacket, double[][] local, double[] uv) {
        double[][] p = new double[4][];
        for (int i = 0; i < 4; i++) {
            p[i] = BodyFrame.world(body, BodyFrame.model(body, "torso", local[i]));
        }
        double[] e1 = {p[2][0] - p[0][0], p[2][1] - p[0][1], p[2][2] - p[0][2]};
        double[] e2 = {p[3][0] - p[1][0], p[3][1] - p[1][1], p[3][2] - p[1][2]};
        double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
        double[] middle = BodyFrame.world(body, BodyFrame.model(body, "torso", new double[] {0.0, HEIGHT / 2.0, 0.0}));
        double[] out = {(p[0][0] + p[2][0]) / 2.0 - middle[0], (p[0][1] + p[2][1]) / 2.0 - middle[1], (p[0][2] + p[2][2]) / 2.0 - middle[2]};
        if (n[0] * out[0] + n[1] * out[1] + n[2] * out[2] < 0.0) {
            n = new double[] {-n[0], -n[1], -n[2]};
        }
        double l = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        n = l < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : new double[] {n[0] / l, n[1] / l, n[2] / l};
        float[] skin = new float[8];
        for (int i = 0; i < 8; i++) {
            skin[i] = (float)(uv[i] / 64.0);
        }
        sink.quad(p[0], p[1], p[2], p[3], n, skin, jacket);
    }
}
