package com.hex.kagune.control.awakening.client;

/** Black and crimson kagune of Rinkaku's Dragon. Colours are 0xRRGGBB. */
final class DragonPalette {
    static final int SHELL = 0x0E0B0D;
    static final int SHELL_EDGE = 0x2A1A1F;
    static final int GROOVE = 0x4A0911;
    static final int CRIMSON = 0x8E0F1A;
    static final int VEIN = 0xC0182A;
    static final int FLESH = 0x3B0A10;
    static final int BELLY = 0x1C090C;
    static final int GUM = 0x6B1420;
    static final int THROAT = 0x160204;
    static final int TOOTH = 0xE6DECF;
    static final int TOOTH_ROOT = 0xB9AE9C;
    static final int SCLERA = 0x050304;
    static final int PALE_EYE = 0xE4DED2;
    static final int IRIS = 0xFF1C22;
    static final int IRIS_CORE = 0xFF6A4A;
    static final int PUPIL = 0x020101;
    static final int CRACK = 0xFF4A1E;
    static final int HAIR = 0x120A0C;
    // 1.0.31: the Dragon as the source has it: a full centipede, Rinkaku's centipede kagune's plates (CentipedeTail) in dark red with
    // the flesh between them light red and glowing; its heads and tentacles the same dark red flesh.
    /** Its flesh, where it swells into the light, deep in its folds, under it; the light red flesh in its seams. */
    static final int MEAT = 0x5A1018;
    static final int MEAT_HI = 0x7C2028;
    static final int MEAT_DEEP = 0x34070C;
    static final int MEAT_BELLY = 0x280509;
    static final int SEAM = 0xD8484A;
    /** The plates: their darkest shadow and their lightest rims; the flesh between them, deep and hot. */
    static final int PLATE_SHADOW = 0x1E0306;
    static final int PLATE_LIGHT = 0x8E2A30;
    static final int INNER_DEEP = 0xB8343A;
    static final int INNER_HOT = 0xFF8A80;
    /** Its legs: the thigh, the shank, the claw at the foot, the joints. */
    static final int LEG = 0x4E0C12;
    static final int LEG_SHANK = 0x641419;
    static final int LEG_CLAW = 0x1A0306;
    static final int JOINT = 0xC23A3E;
    /** Its eyes: the thick rim round each, the glowing iris and the darker ring round it, the pupil, the glint in it. */
    static final int EYE_RIM = 0x22142A;
    static final int EYE_IRIS = 0xFF4E9A;
    static final int EYE_RING = 0xA8185A;
    static final int EYE_PUPIL = 0x26040F;
    static final int EYE_GLINT = 0xFFF0F6;

    private DragonPalette() {
    }

    static int mix(int a, int b, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1.0 - k) + ((b >> 16) & 0xFF) * k);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1.0 - k) + ((b >> 8) & 0xFF) * k);
        int bl = (int) Math.round((a & 0xFF) * (1.0 - k) + (b & 0xFF) * k);
        return r << 16 | g << 8 | bl;
    }

    /** Small deterministic hash in [0, 1) for stable per-segment detail. */
    static double hash(int a, int b) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) / (double) (1L << 53);
    }
}
