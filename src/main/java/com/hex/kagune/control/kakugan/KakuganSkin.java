package com.hex.kagune.control.kakugan;

/**
 * What a player's skin says about their eyes (pure, tested; 3.0.0), read from its pixels: where the eyes are on the face, and
 * which face pixels the skin's outer (second) layer covers, and which of those are glass (see-through, like lenses). Glass over
 * the eyes is drawn after the kakugan so the ghoul eyes show through it; eyes painted fully opaque on the outer layer get the
 * kakugan drawn on them ({@link KakuganModel}). The outer layer is never cut away.
 *
 * <p>Pixels are ARGB, {@code size} by {@code size} (a 64 by 64 skin, or any multiple of it: every skin pixel is then a square
 * of them). The face is texture square (8, 8)..(16, 16), the outer layer's face (40, 8)..(48, 16).
 */
public final class KakuganSkin {
    /** An outer-layer pixel at least this opaque covers what is under it. */
    public static final int COVERING_ALPHA = 64;
    /** An outer-layer pixel this opaque is solid; one between the two is glass (tinted, see-through). */
    public static final int SOLID_ALPHA = 250;
    /** Below this score no pair of eyes stands out on the face (a full mask, a blank face): keep the placement as it was. */
    static final double CONFIDENT = 250.0;

    private KakuganSkin() {
    }

    /** The bit for face cell ({@code column}, {@code row}) in a {@link #covered} mask. */
    public static long bit(int column, int row) {
        return 1L << (row * 8 + column);
    }

    /** The face cells both eyes take up. */
    public static long eyeCells(KakuganEyes eyes) {
        KakuganEyes e = eyes.validated();
        long mask = 0L;
        for (boolean right : new boolean[] {true, false}) {
            for (int row = e.row(right); row < e.row(right) + e.height(); row++) {
                mask |= bit(e.column(right), row) | bit(e.column(right) + 1, row);
            }
        }
        return mask;
    }

    /**
     * True if {@code pixels} can be a real skin as read back: Minecraft keeps the face's inner layer fully opaque on every skin,
     * so a read that left any of it see-through (blank, as a driver that cannot read textures back leaves it, or scrambled) is
     * not to be trusted.
     */
    public static boolean readable(int[] pixels, int size) {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                if ((pixel(pixels, size, 8 + column, 8 + row) >>> 24) != 255) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Which face cells the outer layer covers (bit {@code row * 8 + column}). */
    public static long covered(int[] pixels, int size) {
        long mask = 0L;
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                if ((pixel(pixels, size, 40 + column, 8 + row) >>> 24) >= COVERING_ALPHA) {
                    mask |= bit(column, row);
                }
            }
        }
        return mask;
    }

    /** Which covered face cells are see-through (a lens: under {@link #SOLID_ALPHA}), bit {@code row * 8 + column}. */
    public static long glass(int[] pixels, int size) {
        long mask = 0L;
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                int alpha = pixel(pixels, size, 40 + column, 8 + row) >>> 24;
                if (alpha >= COVERING_ALPHA && alpha < SOLID_ALPHA) {
                    mask |= bit(column, row);
                }
            }
        }
        return mask;
    }

    /** The face as others see it: the outer layer laid over the inner one by its alpha. {@code [row][column]}, RGB. */
    public static int[][] visibleFace(int[] pixels, int size) {
        int[][] face = new int[8][8];
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                int inner = pixel(pixels, size, 8 + column, 8 + row);
                int outer = pixel(pixels, size, 40 + column, 8 + row);
                face[row][column] = mix(inner & 0xFFFFFF, outer & 0xFFFFFF, (outer >>> 24) / 255.0);
            }
        }
        return face;
    }

    /**
     * Where the eyes are on this skin: the pair of two-pixel eyes that stands out most, on the inner face (where the kakugan
     * goes; glasses on the second layer can hide them from view) or on the face as it is seen (eyes painted on the second
     * layer), whichever is clearer. Null when nothing stands out enough to trust.
     */
    public static KakuganEyes detect(int[] pixels, int size) {
        int[][] inner = new int[8][8];
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                inner[row][column] = pixel(pixels, size, 8 + column, 8 + row) & 0xFFFFFF;
            }
        }
        Found under = best(inner);
        Found seen = best(visibleFace(pixels, size));
        Found found = seen != null && (under == null || seen.score() > under.score()) ? seen : under;
        return found == null || found.score() < CONFIDENT ? null : found.eyes();
    }

    /** A placement and how much it stands out. */
    record Found(KakuganEyes eyes, double score) {
    }

    /**
     * The pair of eyes standing out most on an 8 by 8 face ({@code [row][column]}, RGB): each eye a whitish pixel beside a
     * darker or coloured one, unlike the skin above and below, the two eyes level, mirrored and alike, most skins' places
     * preferred a little.
     */
    static Found best(int[][] face) {
        Found best = null;
        for (int height = 1; height <= 2; height++) {
            for (int ry = 1; ry <= 8 - height - 1; ry++) {
                for (int rx = 0; rx <= 4; rx++) {
                    double right = eye(face, rx, ry, height);
                    if (right <= 0.0) {
                        continue;
                    }
                    for (int ly = Math.max(1, ry - 1); ly <= Math.min(8 - height - 1, ry + 1); ly++) {
                        for (int lx = rx + 2; lx <= 6; lx++) {
                            double left = eye(face, lx, ly, height);
                            if (left <= 0.0) {
                                continue;
                            }
                            double score = right + left;
                            score += ry == ly ? 40.0 : -30.0;
                            score += lx == 6 - rx ? 60.0 : -15.0 * Math.abs(lx - (6 - rx));
                            // The two eyes alike, mirrored: right eye's outer pixel against the left eye's outer pixel.
                            score -= 0.2 * (distance(face[ry][rx], face[ly][lx + 1]) + distance(face[ry][rx + 1], face[ly][lx]));
                            // Their whites on mirrored sides.
                            boolean rightWhiteOutside = whiteness(face[ry][rx]) >= whiteness(face[ry][rx + 1]);
                            boolean leftWhiteOutside = whiteness(face[ly][lx + 1]) >= whiteness(face[ly][lx]);
                            score += rightWhiteOutside == leftWhiteOutside ? 25.0 : -40.0;
                            // Where most skins have them: one pixel in from the sides, rows 3 to 5.
                            score += rx == 1 ? 30.0 : rx == 2 ? 10.0 : -10.0;
                            score += ry >= 3 && ry <= 5 ? 15.0 : 0.0;
                            if (best == null || score > best.score()) {
                                // The white outside means the iris is the inner pixel.
                                best = new Found(new KakuganEyes(rx, ry, lx, ly, height, rightWhiteOutside, false).validated(), score);
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    /**
     * How much a two-pixel eye at ({@code x}, {@code y}), {@code height} tall, stands out (0 or less: not an eye): a whitish
     * pixel and a darker one, the same way round on every row, the white unlike the skin above and below it, and the dark one
     * not just a strip of hair running on above and below.
     */
    static double eye(int[][] face, int x, int y, int height) {
        double contrast = 0.0;
        double white = 0.0;
        boolean whiteLeft = whiteness(face[y][x]) >= whiteness(face[y][x + 1]);
        for (int row = y; row < y + height; row++) {
            int a = face[row][x];
            int b = face[row][x + 1];
            if ((whiteness(a) >= whiteness(b)) != whiteLeft || distance(a, b) < 40.0) {
                return 0.0;
            }
            contrast += Math.min(distance(a, b), 260.0);
            white += Math.max(whiteness(a), whiteness(b));
        }
        contrast /= height;
        white /= height;
        int sclera = whiteLeft ? x : x + 1;
        int iris = whiteLeft ? x + 1 : x;
        int aboveWhite = face[y - 1][sclera];
        int belowWhite = face[y + height][sclera];
        double apart = Double.MAX_VALUE;
        for (int row = y; row < y + height; row++) {
            apart = Math.min(apart, Math.min(distance(face[row][sclera], aboveWhite), distance(face[row][sclera], belowWhite)));
        }
        double score = contrast * 0.5 + Math.min(apart, 200.0) * 0.6 + 140.0 * white;
        // A dark pixel with the same dark above and below it is hair down the side of the face, not an iris.
        int irisTop = face[y][iris];
        int irisBottom = face[y + height - 1][iris];
        if (distance(irisTop, face[y - 1][iris]) < 80.0 && distance(irisBottom, face[y + height][iris]) < 80.0) {
            score -= 120.0;
        }
        // Nor is one that runs on into the pixel beyond it: that is skin (or hair) beside a white.
        int beyond = iris == x + 1 ? x + 2 : x - 1;
        if (beyond >= 0 && beyond < 8) {
            boolean runs = true;
            for (int row = y; row < y + height; row++) {
                runs &= distance(face[row][iris], face[row][beyond]) < 50.0;
            }
            if (runs) {
                score -= 100.0;
            }
        }
        return score + (height == 2 ? 20.0 : 0.0);
    }

    /** 0 .. 1: how much a colour looks like the white of an eye (bright and unsaturated). */
    static double whiteness(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        double light = (r * 0.299 + g * 0.587 + b * 0.114) / 255.0;
        double saturation = max == 0 ? 0.0 : (max - min) / (double)max;
        return Math.max(0.0, Math.min(1.0, (light - 0.55) / 0.4)) * (1.0 - saturation);
    }

    static double distance(int a, int b) {
        double dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
        double dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
        double db = (a & 0xFF) - (b & 0xFF);
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    static int mix(int a, int b, double t) {
        double s = Math.max(0.0, Math.min(1.0, t));
        int r = (int)Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * s);
        int g = (int)Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * s);
        int bl = (int)Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * s);
        return r << 16 | g << 8 | bl;
    }

    /** Skin pixel ({@code u}, {@code v}) of a 64-wide layout, from a texture {@code size} wide (its top-left sample). */
    static int pixel(int[] pixels, int size, int u, int v) {
        int scale = Math.max(1, size / 64);
        int x = u * scale;
        int y = v * scale;
        return x < size && y < size && y * size + x < pixels.length ? pixels[y * size + x] : 0;
    }
}
