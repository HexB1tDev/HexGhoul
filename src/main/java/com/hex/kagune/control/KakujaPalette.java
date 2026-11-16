package com.hex.kagune.control;

/**
 * 2.1.0 (pure, tested; the owner: "Kakuja mask and centipede legs should better adapt their colors to match the kagune color, make it
 * blend nicely but make sure they're distinct. Segments should always be Grey, Black, white, not primary colors, thats for the inner parts
 * of the centipede"): the colours of a Rinkaku kakuja's centipede legs and mask for a kagune of a given colour. Was a fixed plum.
 *
 * <ul>
 *   <li>The plates (the legs' segments, the mask's shell) are always greys: charcoal and black with pale grey and white rims, or (for a
 *       kagune too dark to stand apart from them) bone white over greys. Only a breath of the kagune's own hue is in them
 *       ({@link #UNDERTONE}), so they sit with it without ever being it.</li>
 *   <li>The flesh between the plates (the legs' glowing joints, the mask's deepest folds) is the kagune's own colour
 *       ({@link KaguneTint#recolour} of the original red flesh).</li>
 * </ul>
 */
public final class KakujaPalette {
    private KakujaPalette() {
    }

    /** How much of the kagune's hue the greys take (a breath: they stay greys). */
    public static final double UNDERTONE = 0.1;
    /** A kagune this dark (HSV value) or darker gets the bone-white plates, so they stand apart from it. */
    public static final double DARK_KAGUNE = 0.3;

    /** The flesh as it was drawn (the original red), turned to the kagune's colour as it is drawn. */
    static final int FLESH_DARK = 0xFF6E1016;
    static final int FLESH = 0xFFC82A28;
    static final int FLESH_HOT = 0xFFF2493A;

    /**
     * A kakuja's colours: the plates from darkest to lightest ({@code shadow, dark, plate, hi, streak}), the rim and its shade (the lit lip
     * of each plate), the gloss, the claws ({@code claw, sheen, edge}), and the flesh ({@code dark, flesh, hot}). All 0xRRGGBB.
     */
    public record Palette(int shadow, int dark, int plate, int hi, int streak, int rim, int rimShade, int gloss, int claw, int clawSheen,
                          int clawEdge, int fleshDark, int flesh, int fleshHot) {
    }

    /** True when a kagune of {@code colour} (0xRRGGBB) is dark enough to take the bone-white plates. */
    public static boolean bone(int colour) {
        return KaguneColours.toHsv(colour & 0xFFFFFF)[2] <= DARK_KAGUNE;
    }

    /** The palette for a kagune of {@code colour} (0xRRGGBB). */
    public static Palette of(int colour) {
        int rgb = colour & 0xFFFFFF;
        double[] hsv = KaguneColours.toHsv(rgb);
        // The kagune's hue at a grey's low saturation: what the greys lean toward.
        int lean = KaguneColours.hsv(hsv[0], Math.min(1.0, hsv[1]) * 0.6, 0.6);
        boolean bone = hsv[2] <= DARK_KAGUNE;
        int shadow;
        int dark;
        int plate;
        int hi;
        int streak;
        int rim;
        int rimShade;
        int gloss;
        if (bone) {
            shadow = 0x6A6866;
            dark = 0x8E8B88;
            plate = 0xB8B4AE;
            hi = 0xD6D2CB;
            streak = 0xE8E4DD;
            rim = 0xF6F3EE;
            rimShade = 0xA9A49D;
            gloss = 0xFFFFFF;
        } else {
            shadow = 0x0E0E10;
            dark = 0x1C1C1F;
            plate = 0x2E2E32;
            hi = 0x46464B;
            streak = 0x6E6E74;
            rim = 0xD8D8DC;
            rimShade = 0x8E8E94;
            gloss = 0xF0F0F2;
        }
        int[] tone = {shadow, dark, plate, hi, streak, rim, rimShade};
        for (int i = 0; i < tone.length; i++) {
            tone[i] = mix(tone[i], lean, UNDERTONE * (i < 5 ? 1.0 : 0.6));
        }
        int fleshDark = KaguneTint.recolour(FLESH_DARK, rgb) & 0xFFFFFF;
        int flesh = KaguneTint.recolour(FLESH, rgb) & 0xFFFFFF;
        int fleshHot = KaguneTint.recolour(FLESH_HOT, rgb) & 0xFFFFFF;
        int claw = bone ? 0x2A2826 : 0x0A0A0C;
        int clawSheen = bone ? 0x56524E : 0x3A3A40;
        int clawEdge = bone ? 0xE0DCD4 : 0xB4B4BA;
        return new Palette(tone[0], tone[1], tone[2], tone[3], tone[4], tone[5], tone[6], gloss, claw, clawSheen, clawEdge, fleshDark, flesh,
            fleshHot);
    }

    /** How far from grey {@code rgb} is: the spread of its channels (0 a grey, black or white; 1 a pure colour). */
    public static double chroma(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        return (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b))) / 255.0;
    }

    static int mix(int from, int to, double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        int r = (int)Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = (int)Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = (int)Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return r << 16 | g << 8 | b;
    }
}
