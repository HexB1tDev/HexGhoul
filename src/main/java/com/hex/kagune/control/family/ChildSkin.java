package com.hex.kagune.control.family;

import java.util.Random;
import java.util.function.Function;

/**
 * 1.3.10: a child's skin (pure), made the way edit_skin makes a player's (its parts are in {@code textures/entity/child}, credited in
 * CREDITS.txt): layer on layer, each a flat colour under its mask with its own shading drawn over it. Its skin is its own tone; its hair
 * one of the girls' long styles or the boys' short ones, in the blend of both parents' hair colours; its eyes a parent's colour (its
 * kakugan, when it shows, black and red in the eye or eyes it has); its clothes for its age (a baby in one soft colour like a onesie), their
 * colours from its seed. Then made a 64 x 64 skin (the parts are the old 64 x 32 layout: the left arm and leg mirrored from the right, as
 * Minecraft does). A child's clothes and body only: never the adult female body.
 */
public final class ChildSkin {
    public static final int SIZE = 64;
    /** Where the parts are, on the class path. */
    public static final String PARTS = "/assets/hexkagune/textures/entity/child/";
    static final int KAKUGAN_IRIS = 0xFFE0302A;
    static final int KAKUGAN_SCLERA = 0xFF140808;
    /** Long hair for girls; short for boys; a baby's wisp. */
    static final int[] GIRL_HAIR = {1, 4, 11};
    static final int[] BOY_HAIR = {3, 5, 10, 12};
    static final int BABY_HAIR = 12;
    static final int[] KID_TOPS = {2, 5, 6, 9, 10, 12, 13};
    static final int[] TEEN_TOPS = {1, 2, 4, 6, 9, 10, 11, 17};
    static final int[] KID_BOTTOMS = {3, 4, 5};
    static final int[] TEEN_BOTTOMS = {4, 5};
    /** Girls' eyes (lashes; the anime pair), boys' (plain). */
    static final int[] GIRL_EYES = {1, 4, 2, 5};
    static final int[] BOY_EYES = {2, 5, 6, 7};
    static final int[] MOUTHS = {1, 2, 3, 5, 7};
    static final int[] SHOES = {1, 2, 3};
    private static final int[] PASTELS = {0xF4C6D6, 0xC6DCF4, 0xD6F4C6, 0xF4E8C6, 0xE2CCF4, 0xC6F4EE};
    private static final int[] BRIGHT = {0xD8434F, 0x3F82C8, 0x3FA864, 0xF2C230, 0x9256C2, 0xEE8A3A, 0x2DA597, 0xE07AA0, 0xF2F2F2};
    private static final int[] DENIM = {0x3A4E7A, 0x2E3B55, 0x4B5E8C, 0x3A3A44, 0x55607A};
    private static final int[] DARK = {0x2B2B30, 0x3A4048, 0x4A3A32, 0x2E3A4A, 0x48304A, 0x5A5A5A};

    private ChildSkin() {
    }

    /**
     * The skin (ARGB, row by row, 64 x 64) of a child with {@code genome} at {@code stage}; {@code kakugan}: its ghoul eye(s) showing;
     * {@code parts} reads a part by name ({@code "hair_1"}: 64 x 32 ARGB), null if it isn't there.
     */
    public static int[] paint(Genome genome, ChildStage stage, boolean kakugan, Function<String, int[]> parts) {
        Random random = new Random(genome.seed());
        boolean girl = genome.gender().female();
        int[] px = new int[SIZE * 32];
        layer(px, parts, "base_1", 0xFF000000 | genome.skin());
        int eye = girl ? GIRL_EYES[random.nextInt(GIRL_EYES.length)] : BOY_EYES[random.nextInt(BOY_EYES.length)];
        int mouth = MOUTHS[random.nextInt(MOUTHS.length)];
        int hair;
        int top;
        int bottom;
        int topColour;
        int bottomColour;
        int shoes = -1;
        switch (stage) {
            case BABY -> {
                hair = BABY_HAIR;
                top = 2;
                bottom = 3;
                topColour = PASTELS[random.nextInt(PASTELS.length)];
                bottomColour = topColour;
            }
            case TEEN -> {
                hair = girl ? GIRL_HAIR[random.nextInt(GIRL_HAIR.length)] : BOY_HAIR[random.nextInt(BOY_HAIR.length)];
                top = TEEN_TOPS[random.nextInt(TEEN_TOPS.length)];
                bottom = TEEN_BOTTOMS[random.nextInt(TEEN_BOTTOMS.length)];
                topColour = random.nextBoolean() ? DARK[random.nextInt(DARK.length)] : BRIGHT[random.nextInt(BRIGHT.length)];
                bottomColour = DENIM[random.nextInt(DENIM.length)];
                shoes = SHOES[random.nextInt(SHOES.length)];
            }
            default -> {
                hair = girl ? GIRL_HAIR[random.nextInt(GIRL_HAIR.length)] : BOY_HAIR[random.nextInt(BOY_HAIR.length)];
                top = KID_TOPS[random.nextInt(KID_TOPS.length)];
                bottom = KID_BOTTOMS[random.nextInt(KID_BOTTOMS.length)];
                topColour = BRIGHT[random.nextInt(BRIGHT.length)];
                bottomColour = random.nextInt(3) == 0 ? PASTELS[random.nextInt(PASTELS.length)] : DENIM[random.nextInt(DENIM.length)];
                shoes = SHOES[random.nextInt(SHOES.length)];
            }
        }
        if (shoes > 0) {
            layer(px, parts, "footwear_" + shoes, 0);
        }
        layer(px, parts, "eye_" + eye, 0);
        eyes(px, parts.apply("eye_" + eye), genome, kakugan);
        layer(px, parts, "mouth_" + mouth, 0);
        layer(px, parts, "bottom_" + bottom, 0xFF000000 | bottomColour);
        layer(px, parts, "top_" + top, 0xFF000000 | topColour);
        int[] under = px.clone();
        layer(px, parts, "hair_" + hair, 0xFF000000 | genome.hair());
        clearChin(px, under);
        return wide(px);
    }

    /**
     * A child has no beard: whatever the hair laid on the middle of the lower face (rows 12..15, columns 9..14, and the same on the hat
     * layer's front) is taken back off; the strands framing the face (its edge columns) stay.
     */
    static void clearChin(int[] px, int[] under) {
        for (int v = 12; v < 16; v++) {
            for (int u = 9; u < 15; u++) {
                px[v * SIZE + u] = under[v * SIZE + u];
                px[v * SIZE + u + 32] = under[v * SIZE + u + 32];
            }
        }
    }

    /** One part laid over {@code px}: its mask in {@code colour} (0: none), then the part itself, each over what is there. */
    static void layer(int[] px, Function<String, int[]> parts, String name, int colour) {
        int[] part = parts.apply(name);
        if (part == null) {
            return;
        }
        int[] mask = colour == 0 ? null : parts.apply(name + "_mask");
        for (int i = 0; i < px.length && i < part.length; i++) {
            if (mask != null && i < mask.length && (mask[i] >>> 24) != 0) {
                px[i] = over(px[i], (mask[i] >>> 24) << 24 | colour & 0xFFFFFF);
            }
            px[i] = over(px[i], part[i]);
        }
    }

    /**
     * Its eyes (on the face's front: its right eye 8..11, its left 12..15, rows 8..16), told from the eye part itself ({@code eye}): its
     * coloured pixels (not white, not a lash, not a pink blush) turned to {@code genome}'s eye colour, as light or dark as they were; its
     * kakugan, if showing, black where the part's whites are and red where its colour is.
     */
    static void eyes(int[] px, int[] eye, Genome genome, boolean kakugan) {
        if (eye == null) {
            return;
        }
        int base = genome.eyes();
        // The rows each eye's white is on (its right eye's, its left's): a pupil on them turns red in a kakugan, a lash above them doesn't.
        boolean[][] whiteRows = new boolean[2][16];
        for (int v = 8; v < 16; v++) {
            for (int u = 8; u < 16; u++) {
                int p = eye[v * SIZE + u];
                if ((p >>> 24) >= 200 && Math.min((p >> 16) & 255, Math.min((p >> 8) & 255, p & 255)) > 190) {
                    whiteRows[u < 12 ? 0 : 1][v] = true;
                }
            }
        }
        for (int v = 8; v < 16; v++) {
            for (int u = 8; u < 16; u++) {
                int i = v * SIZE + u;
                int p = eye[i];
                if ((p >>> 24) < 200) {
                    continue;
                }
                boolean right = u < 12;
                boolean ghoulEye = kakugan && (genome.kakugan() == Genome.BOTH || right && genome.kakugan() == Genome.RIGHT
                    || !right && genome.kakugan() == Genome.LEFT);
                int r = (p >> 16) & 255;
                int g = (p >> 8) & 255;
                int b = p & 255;
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                boolean white = min > 190;
                boolean blush = r > b + 40 && max > 160;
                boolean iris = !white && !blush && max >= 40 && max - min > 30;
                // An eye with no white at all (two dots): the dot is its pupil.
                boolean dots = !hasWhite(whiteRows[right ? 0 : 1]);
                boolean pupil = !white && !blush && (whiteRows[right ? 0 : 1][v] || dots);
                if (ghoulEye && white) {
                    px[i] = KAKUGAN_SCLERA;
                } else if (ghoulEye && pupil) {
                    px[i] = KAKUGAN_IRIS;
                } else if (iris) {
                    px[i] = ghoulEye ? KAKUGAN_IRIS : 0xFF000000 | scale(base, Math.max(0.55F, Math.min(1.5F, max / 160.0F)));
                }
            }
        }
    }

    private static boolean hasWhite(boolean[] rows) {
        for (boolean row : rows) {
            if (row) {
                return true;
            }
        }
        return false;
    }

    /** {@code a} with {@code b} laid over it (by {@code b}'s alpha). */
    static int over(int a, int b) {
        int ba = b >>> 24;
        if (ba == 255) {
            return b;
        }
        if (ba == 0) {
            return a;
        }
        int aa = a >>> 24;
        float fb = ba / 255.0F;
        float fa = aa / 255.0F * (1.0F - fb);
        float out = fb + fa;
        int r = Math.round((((b >> 16) & 255) * fb + ((a >> 16) & 255) * fa) / out);
        int g = Math.round((((b >> 8) & 255) * fb + ((a >> 8) & 255) * fa) / out);
        int bl = Math.round(((b & 255) * fb + (a & 255) * fa) / out);
        return Math.round(out * 255.0F) << 24 | r << 16 | g << 8 | bl;
    }

    static int scale(int rgb, float k) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * k));
        int b = Math.min(255, Math.round((rgb & 255) * k));
        return r << 16 | g << 8 | b;
    }

    /** A 64 x 32 skin made 64 x 64: the left leg and arm copied from the right, each face mirrored (as Minecraft reads an old skin). */
    static int[] wide(int[] old) {
        int[] px = new int[SIZE * SIZE];
        System.arraycopy(old, 0, px, 0, Math.min(old.length, SIZE * 32));
        copy(px, 4, 16, 16, 32, 4, 4);
        copy(px, 8, 16, 16, 32, 4, 4);
        copy(px, 0, 20, 24, 32, 4, 12);
        copy(px, 4, 20, 16, 32, 4, 12);
        copy(px, 8, 20, 8, 32, 4, 12);
        copy(px, 12, 20, 16, 32, 4, 12);
        copy(px, 44, 16, -8, 32, 4, 4);
        copy(px, 48, 16, -8, 32, 4, 4);
        copy(px, 40, 20, 0, 32, 4, 12);
        copy(px, 44, 20, -8, 32, 4, 12);
        copy(px, 48, 20, -16, 32, 4, 12);
        copy(px, 52, 20, -8, 32, 4, 12);
        return px;
    }

    /** Minecraft's copyRect(x, y, dx, dy, w, h, mirror x, not y). */
    private static void copy(int[] px, int x, int y, int dx, int dy, int w, int h) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int from = (y + j) * SIZE + x + w - 1 - i;
                int to = (y + j + dy) * SIZE + x + i + dx;
                px[to] = px[from];
            }
        }
    }

    /** Reads a part off the class path (64 x 32 ARGB), or null. */
    public static int[] part(String name) {
        try (java.io.InputStream in = ChildSkin.class.getResourceAsStream(PARTS + name + ".png")) {
            if (in == null) {
                return null;
            }
            Png.Image image = Png.read(in);
            return image.width() == SIZE && image.height() == 32 ? image.pixels() : null;
        } catch (java.io.IOException unreadable) {
            return null;
        }
    }
}
