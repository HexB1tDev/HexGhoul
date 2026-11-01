package com.hex.kagune.control;

import java.util.function.DoubleSupplier;

/**
 * 1.3.34: a Rinkaku kagune's colour, rolled at random (pure, tested). The owner: "the normal tendrils that are by default red to be a
 * random color, literally a random color... whenever the player spawns, you get assigned a random color and a command to re-roll it".
 *
 * <p>The colour is the original's own (KaguneProfile's appearance: a primary colour the tendrils, the mask and the shards are drawn in,
 * and a vein colour the dark ribbing between their plates), so everything already drawn in it follows. A roll is any hue, kept clear
 * of muddy greys and of black-on-black (saturated enough, light enough); its veins are the same hue, much darker. Now and then it is one
 * of the rare ones: near black with blood-red veins, or bone white with ash veins.
 */
public final class KaguneColours {
    private KaguneColours() {
    }

    /** The original's crimson and its veins (KaguneProfile.DEFAULT_COLOR, DEFAULT_VEIN_COLOR). */
    public static final int DEFAULT = 0xA50F37;
    public static final int DEFAULT_VEIN = 0x18030B;
    /** The rare ones: their chance each, and their colours. */
    public static final double RARE = 0.04;
    public static final int BLACK = 0x17141A;
    public static final int BLACK_VEIN = 0xA0101C;
    public static final int BONE = 0xE6DFD2;
    public static final int BONE_VEIN = 0x5A4F47;

    /** A colour and its veins (0xRRGGBB each). */
    public record Pick(int colour, int vein) {
    }

    /** A new colour from four numbers in 0..1. */
    public static Pick roll(DoubleSupplier random) {
        double rare = random.getAsDouble();
        if (rare < RARE) {
            return new Pick(BLACK, BLACK_VEIN);
        }
        if (rare < RARE * 2.0) {
            return new Pick(BONE, BONE_VEIN);
        }
        double hue = random.getAsDouble();
        double saturation = 0.55 + 0.45 * random.getAsDouble();
        double value = 0.42 + 0.5 * random.getAsDouble();
        return new Pick(hsv(hue, saturation, value), hsv(hue, Math.min(1.0, saturation + 0.1), Math.max(0.06, value * 0.17)));
    }

    /** 0xRRGGBB from a hue (0..1, round), saturation and value (0..1). */
    public static int hsv(double hue, double saturation, double value) {
        double h = (hue - Math.floor(hue)) * 6.0;
        double s = Math.max(0.0, Math.min(1.0, saturation));
        double v = Math.max(0.0, Math.min(1.0, value));
        int sector = (int)Math.floor(h) % 6;
        double f = h - Math.floor(h);
        double p = v * (1.0 - s);
        double q = v * (1.0 - s * f);
        double t = v * (1.0 - s * (1.0 - f));
        double[] rgb = switch (sector) {
            case 0 -> new double[] {v, t, p};
            case 1 -> new double[] {q, v, p};
            case 2 -> new double[] {p, v, t};
            case 3 -> new double[] {p, q, v};
            case 4 -> new double[] {t, p, v};
            default -> new double[] {v, p, q};
        };
        return channel(rgb[0]) << 16 | channel(rgb[1]) << 8 | channel(rgb[2]);
    }

    private static int channel(double c) {
        return (int)Math.round(Math.max(0.0, Math.min(1.0, c)) * 255.0);
    }

    /** {@code rgb}'s hue (0..1), saturation and value. */
    public static double[] toHsv(int rgb) {
        double r = (rgb >> 16 & 255) / 255.0;
        double g = (rgb >> 8 & 255) / 255.0;
        double b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double d = max - min;
        double h;
        if (d < 1.0E-9) {
            h = 0.0;
        } else if (max == r) {
            h = ((g - b) / d) / 6.0;
        } else if (max == g) {
            h = ((b - r) / d + 2.0) / 6.0;
        } else {
            h = ((r - g) / d + 4.0) / 6.0;
        }
        return new double[] {h - Math.floor(h), max < 1.0E-9 ? 0.0 : d / max, max};
    }

    private static final String[] HUES = {"red", "scarlet", "orange", "amber", "gold", "yellow", "lime", "green", "emerald", "jade", "teal", "cyan",
        "azure", "blue", "cobalt", "indigo", "violet", "purple", "magenta", "pink", "rose", "crimson"};

    /** What to call {@code rgb} in a line of chat: its hue, lightened or darkened in words ("dark teal", "pale violet", "bone white"). */
    public static String name(int rgb) {
        if (rgb == BLACK) {
            return "near-black";
        }
        if (rgb == BONE) {
            return "bone white";
        }
        double[] hsv = toHsv(rgb);
        if (hsv[1] < 0.18) {
            return hsv[2] < 0.25 ? "black" : hsv[2] > 0.8 ? "white" : "grey";
        }
        String hue = HUES[(int)Math.floor(hsv[0] * HUES.length + 0.5) % HUES.length];
        if (hsv[2] < 0.5) {
            return "dark " + hue;
        }
        if (hsv[1] < 0.65 && hsv[2] > 0.75) {
            return "pale " + hue;
        }
        return hsv[2] > 0.85 && hsv[1] > 0.85 ? "bright " + hue : hue;
    }

    /** "#RRGGBB". */
    public static String hex(int rgb) {
        return String.format(java.util.Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }
}
