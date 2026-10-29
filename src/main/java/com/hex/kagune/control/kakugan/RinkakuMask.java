package com.hex.kagune.control.kakugan;

import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * Rinkaku's kakuja mask (the Centipede and both kakujas) in head pixels (pure, tested; 3.0.0): the original's eye patch over his
 * right eye and the beak drooping from it, the very same shapes and colours, moved off the world (where the original drew
 * them from his head's yaw, pitch and eye height, so they drifted off his face whenever an animation moved the head model)
 * onto the head itself. Head pixels: x to the player's left, y down from the neck, z backward (the face at z = -4, the skin's
 * outer layer at -4.5). The patch is centred on the player's own right eye ({@link KakuganEyes}) and laid just over the
 * outer layer.
 */
public final class RinkakuMask {
    /** Blocks to head pixels (the player model is drawn at 15/16). */
    public static final double PIXELS = 16.0 / 0.9375;
    /** The original's mask sat this far in front of the eye (blocks); here that depth lies just outside the skin's outer layer. */
    static final double FACE_DEPTH = 0.326;
    public static final double OUTER_LAYER = 4.62;
    /** The eye's height on the head (pixels above the neck), the point the original measured the mask from. */
    static final double EYE_Y = -3.63;

    private RinkakuMask() {
    }

    /** The mask's quads, its colours from the kagune's ({@code color}, {@code veinColor}) as the original mixed them. */
    public static void build(UkakuModel.Sink sink, KakuganEyes eyes, int color, int veinColor) {
        // The default right eye's centre is (-2, -3): move the mask with the player's own.
        double shiftX = eyes.column(true) - KakuganEyes.DEFAULT.column(true);
        double shiftY = eyes.row(true) - KakuganEyes.DEFAULT.row(true) + (eyes.height() - KakuganEyes.DEFAULT.height()) * 0.5;
        int nearBlack = mix(veinColor, 591117, 0.78F);
        int purple = mix(color, 3412039, 0.58F);
        int ridgeColor = mix(purple, 1115929, 0.42F);
        // The eye patch (the original's offsets from the eye, in blocks: right, up, forward).
        double[] patch = {0.105, 0.018, 0.326};
        quad(sink, nearBlack, shiftX, shiftY, at(patch, -0.105, -0.09, 0.01), at(patch, 0.145, -0.055, 0.004), at(patch, 0.125, 0.115, -0.002),
            at(patch, -0.075, 0.095, 0.008));
        // The beak drooping from it.
        double[] beak = {0.04, -0.055, 0.324};
        double[] upperInner = at(beak, -0.1, 0.07, 0.012);
        double[] upperOuter = at(beak, 0.145, 0.05, 0.006);
        double[] lowerInner = at(beak, -0.07, -0.125, 0.03);
        double[] lowerOuter = at(beak, 0.125, -0.135, 0.02);
        double[] ridge = at(beak, 0.025, -0.08, 0.175);
        double[] tip = at(beak, 0.045, -0.435, 0.105);
        quad(sink, purple, shiftX, shiftY, upperInner, upperOuter, ridge, ridge);
        quad(sink, ridgeColor, shiftX, shiftY, upperInner, ridge, tip, lowerInner);
        quad(sink, nearBlack, shiftX, shiftY, ridge, upperOuter, lowerOuter, tip);
        quad(sink, purple, shiftX, shiftY, lowerInner, tip, lowerOuter, lowerOuter);
    }

    /** A point of the original's mask ({@code centre} plus {@code side}, {@code height}, {@code depth}, blocks from the eye). */
    private static double[] at(double[] centre, double side, double height, double depth) {
        return new double[] {centre[0] + side, centre[1] + height, centre[2] + depth};
    }

    /** That point in head pixels, its depth laid onto the face. */
    static double[] pixels(double[] blocks, double shiftX, double shiftY) {
        return new double[] {-blocks[0] * PIXELS + shiftX, EYE_Y - blocks[1] * PIXELS + shiftY, -(OUTER_LAYER + (blocks[2] - FACE_DEPTH) * PIXELS)};
    }

    private static void quad(UkakuModel.Sink sink, int colour, double shiftX, double shiftY, double[] a, double[] b, double[] c, double[] d) {
        // The original's per-corner shading, at its alpha.
        float[] shade = {0.82F, 0.9F, 0.96F, 0.86F};
        int[] argb = new int[4];
        for (int corner = 0; corner < 4; corner++) {
            argb[corner] = 246 << 24 | channel(colour >> 16, shade[corner]) << 16 | channel(colour >> 8, shade[corner]) << 8 | channel(colour, shade[corner]);
        }
        sink.quad(pixels(a, shiftX, shiftY), pixels(b, shiftX, shiftY), pixels(c, shiftX, shiftY), pixels(d, shiftX, shiftY), new double[] {0.0, 0.0, -1.0},
            argb[0], argb[1], argb[2], argb[3]);
    }

    private static int channel(int value, float shade) {
        return Math.max(0, Math.min(255, (int)((value & 0xFF) * shade)));
    }

    /** The original's colour mix. */
    static int mix(int a, int b, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int r = Math.round((a >> 16 & 0xFF) + t * ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)));
        int g = Math.round((a >> 8 & 0xFF) + t * ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)));
        int blue = Math.round((a & 0xFF) + t * ((b & 0xFF) - (a & 0xFF)));
        return r << 16 | g << 8 | blue;
    }
}
