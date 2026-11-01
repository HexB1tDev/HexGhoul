package com.hex.kagune.control;

/**
 * 1.3.35: a kagune drawn in its owner's colour when its own palette is fixed (pure, tested). The owner: "colors are fully randomly
 * generated, including crystal variants". Ukaku's wings, their crystal forms and every shard they fire are drawn in a red palette (reds
 * and crimsons at its heart, violet and navy in its depths, pink at its rims, white where it flashes); for a kagune of another colour
 * ({@link KaguneColours}, the one the owner rolled) every one of those colours is turned round the colour wheel by as much as the
 * owner's is from red, its saturation brought toward theirs, and its brightness kept, so the palette stays a palette (the depths still
 * deeper than the heart, the rims still lighter) in the new hue. The original's crimson leaves it as it is. The rare near-black kagune
 * draws its dark parts black and keeps its glow blood red; the bone white one draws in ash and bone.
 */
public final class KaguneTint {
    private KaguneTint() {
    }

    /** The hue the fixed palettes are built round (red). */
    public static final double RED = 0.995;

    /** {@code argb} (a colour of a red palette) as it is drawn for a kagune of {@code target} (0xRRGGBB). */
    public static int recolour(int argb, int target) {
        int rgb = target & 0xFFFFFF;
        if (rgb == KaguneColours.DEFAULT) {
            return argb;
        }
        int alpha = argb & 0xFF000000;
        double[] c = KaguneColours.toHsv(argb & 0xFFFFFF);
        double[] t = KaguneColours.toHsv(rgb);
        if (t[2] < 0.3 || t[1] < 0.25) {
            if (t[2] < 0.3) {
                // Black: the body black (its pale rims a dark smoke grey), only what glows hot and saturated still blood red.
                boolean glow = c[1] > 0.55 && c[2] > 0.8;
                return alpha | (glow ? KaguneColours.hsv(RED, c[1], c[2] * 0.9) : KaguneColours.hsv(c[0], c[1] * 0.08, c[2] * 0.26));
            }
            // White or grey: ash in the depths, bone where it is light, a faint warmth kept.
            double v = t[2] > 0.75 ? 0.3 + 0.7 * c[2] : c[2] * (0.4 + 0.6 * t[2]);
            return alpha | KaguneColours.hsv(0.08, c[1] * 0.12, Math.min(1.0, v));
        }
        double hue = c[0] + (t[0] - RED);
        double saturation = Math.min(1.0, c[1] * (0.3 + 0.7 * Math.min(1.0, t[1] / 0.9)));
        double value = Math.min(1.0, c[2] * (0.8 + 0.2 * Math.min(1.0, t[2] / 0.8)));
        return alpha | KaguneColours.hsv(hue, saturation, value);
    }

    /** All four corners of a quad recoloured (in place), and the array back. */
    public static int[] recolour(int[] argb, int target) {
        if ((target & 0xFFFFFF) == KaguneColours.DEFAULT) {
            return argb;
        }
        int[] out = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            out[i] = recolour(argb[i], target);
        }
        return out;
    }

    /**
     * A wild ghoul's kagune colour, from the seed it was born with (the same every time it is seen): as random as a player's, its
     * original crimson a chance among the rest.
     */
    public static int wild(int seed) {
        java.util.Random random = new java.util.Random(seed * 0x9E3779B97F4A7C15L + 77L);
        if (random.nextDouble() < 0.15) {
            return KaguneColours.DEFAULT;
        }
        return KaguneColours.roll(random::nextDouble).colour();
    }
}
