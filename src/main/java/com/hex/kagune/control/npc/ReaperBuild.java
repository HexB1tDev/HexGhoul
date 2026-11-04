package com.hex.kagune.control.npc;

/**
 * 2.0.45 (pure, tested; the owner: "adding some muscle to him ... The top part of the torso, should be just a little bit bigger than the
 * one in the bottom that connects the legs ... make the chest subside a little bit from the belly ... don't actually make muscle. Don't
 * design spheres or anything. Just simply increase the size of parts of the already existing player structure"): the White Reaper's
 * build. His torso's upper part is drawn again over it, a little bigger, in his own skin and its jacket layer: a chest broader across
 * the shoulders and lats ({@link #PROFILE}) and standing out in front of the belly, tapering back into the torso as it is at the waist,
 * so his upper body is wider than the hips under it (a V). His arms are set {@link #ARM_OUT} further out to clear it.
 *
 * <p>Model pixels of the torso, as vanilla's player model has them: x to his left, y down from the neck, z backward (his front is -z).
 * The torso is 8 x 12 x 4 (x -4..4, z -2..2); its skin at (16, 16), its jacket layer at (16, 32), on a 64 x 64 skin.
 */
public final class ReaperBuild {
    private ReaperBuild() {
    }

    /**
     * The chest's outline down the torso: each row {y, half its width, how far its front stands out, how far its back}. Broad at the
     * shoulders and the chest, out in front of the belly, then in to just inside the torso at the waist (where the torso takes over).
     */
    public static final double[][] PROFILE = {
        {-0.03, 4.5, 2.5, 2.2},
        {1.2, 4.68, 2.95, 2.35},
        {5.2, 4.62, 3.05, 2.35},
        {6.8, 4.32, 2.45, 2.15},
        {8.5, 3.97, 1.97, 1.97}};
    /** How far (pixels) the jacket layer stands off the chest, as the skin's second layer does on the torso. */
    public static final double JACKET = 0.25;
    /** How much further out (pixels) each arm hangs, clear of the broader chest. */
    public static final float ARM_OUT = 0.75F;

    /**
     * 2.0.46: how far back (pixels, +z) the build's back stands at {@code y} down the torso ({@code jacket}: its jacket layer's), or 0 where
     * there is none (above the shoulders or below the waist): what is laid on a man's back (the Rinkaku mark) sits on it.
     */
    public static double back(double y, boolean jacket) {
        int rows = PROFILE.length;
        if (!(y >= PROFILE[0][0]) || y > PROFILE[rows - 1][0]) {
            return 0.0;
        }
        for (int i = 0; i + 1 < rows; i++) {
            double[] a = PROFILE[i];
            double[] b = PROFILE[i + 1];
            if (y <= b[0]) {
                double t = (y - a[0]) / (b[0] - a[0]);
                return a[3] + (b[3] - a[3]) * t + (jacket ? JACKET : 0.0);
            }
        }
        return 0.0;
    }

    /** Where each quad goes: its four corners (torso pixels), their places on the skin (texture pixels, u v four times), its normal. */
    public interface Sink {
        void quad(double[][] corners, double[] uv, double[] normal);
    }

    /** The chest ({@code jacket}: its jacket layer, standing {@link #JACKET} off it), quad by quad: front, back, sides, and the top. */
    public static void chest(boolean jacket, Sink sink) {
        double grow = jacket ? JACKET : 0.0;
        double v0 = jacket ? 32.0 : 16.0;
        int rows = PROFILE.length;
        for (int i = 0; i + 1 < rows; i++) {
            double[] a = PROFILE[i];
            double[] b = PROFILE[i + 1];
            double ya = a[0] - (i == 0 ? grow : 0.0);
            double yb = b[0];
            double wa = a[1] + grow;
            double wb = b[1] + grow;
            double fa = -(a[2] + grow);
            double fb = -(b[2] + grow);
            double ba = a[3] + grow;
            double bb = b[3] + grow;
            // The front, seen from in front: his right (-x) on the left of the skin's front.
            emit(sink, v0, new double[][] {{-wa, ya, fa}, {wa, ya, fa}, {wb, yb, fb}, {-wb, yb, fb}}, Face.FRONT);
            emit(sink, v0, new double[][] {{wa, ya, ba}, {-wa, ya, ba}, {-wb, yb, bb}, {wb, yb, bb}}, Face.BACK);
            emit(sink, v0, new double[][] {{wa, ya, fa}, {wa, ya, ba}, {wb, yb, bb}, {wb, yb, fb}}, Face.LEFT);
            emit(sink, v0, new double[][] {{-wa, ya, ba}, {-wa, ya, fa}, {-wb, yb, fb}, {-wb, yb, bb}}, Face.RIGHT);
        }
        double[] top = PROFILE[0];
        double y = top[0] - grow;
        double w = top[1] + grow;
        double f = -(top[2] + grow);
        double back = top[3] + grow;
        emit(sink, v0, new double[][] {{-w, y, back}, {w, y, back}, {w, y, f}, {-w, y, f}}, Face.TOP);
    }

    enum Face {
        FRONT, BACK, LEFT, RIGHT, TOP
    }

    /** One quad, each corner painted with the torso's texel for where it is (clamped onto the torso's own faces), its normal outward. */
    static void emit(Sink sink, double v0, double[][] c, Face face) {
        double[] uv = new double[8];
        for (int k = 0; k < 4; k++) {
            double[] t = texel(c[k], face, v0);
            uv[2 * k] = t[0];
            uv[2 * k + 1] = t[1];
        }
        double[] e1 = {c[2][0] - c[0][0], c[2][1] - c[0][1], c[2][2] - c[0][2]};
        double[] e2 = {c[3][0] - c[1][0], c[3][1] - c[1][1], c[3][2] - c[1][2]};
        double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
        double[] mid = {(c[0][0] + c[2][0]) * 0.5, (c[0][1] + c[2][1]) * 0.5, (c[0][2] + c[2][2]) * 0.5};
        double[] out = {mid[0], face == Face.TOP ? -1.0 : 0.0, mid[2]};
        if (n[0] * out[0] + n[1] * out[1] + n[2] * out[2] < 0.0) {
            n = new double[] {-n[0], -n[1], -n[2]};
        }
        double l = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        sink.quad(c, uv, l < 1.0E-9 ? new double[] {0.0, -1.0, 0.0} : new double[] {n[0] / l, n[1] / l, n[2] / l});
    }

    /**
     * Where on the skin (texture pixels) a corner at {@code p} of a {@code face} is painted from: the torso's own face's texel at the
     * same place, its x clamped to the torso's width and its z to its depth (a quarter pixel in from an edge, so a side stays on its
     * own column).
     */
    static double[] texel(double[] p, Face face, double v0) {
        double x = Math.max(-3.75, Math.min(3.75, p[0]));
        double z = Math.max(-1.75, Math.min(1.75, p[2]));
        double y = Math.max(0.0, Math.min(12.0, p[1]));
        double v = v0 + 4.0 + y;
        return switch (face) {
            case FRONT -> new double[] {20.0 + 4.0 + x, v};
            case BACK -> new double[] {32.0 + 4.0 - x, v};
            case LEFT -> new double[] {28.0 + 2.0 + z, v};
            case RIGHT -> new double[] {16.0 + 2.0 - z, v};
            case TOP -> new double[] {20.0 + 4.0 + x, v0 + 2.0 - z};
        };
    }
}
