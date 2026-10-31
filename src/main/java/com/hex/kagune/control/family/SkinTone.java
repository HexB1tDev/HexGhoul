package com.hex.kagune.control.family;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.3.10: a player's skin tone, read off their own skin (pure): the lower face (its cheeks, chin and jaw: rows 12..16 of the face's front,
 * where the second layer doesn't cover it) and both hands' fronts; the commonest colour there, near colours counted together, averaged.
 * A child's skin is the blend of both its parents' tones, never anything else ({@link Genome}).
 */
public final class SkinTone {
    private SkinTone() {
    }

    /** {@code pixels} a 64 x 64 skin (ARGB, row by row); its skin tone (RGB), or -1 if none can be told. */
    public static int of(int[] pixels, int size) {
        if (pixels == null || size < 64 || pixels.length < size * size) {
            return -1;
        }
        Map<Integer, long[]> buckets = new HashMap<>();
        for (int v = 12; v < 16; v++) {
            for (int u = 8; u < 16; u++) {
                int hat = pixels[v * size + u + 32];
                if ((hat >>> 24) < 128) {
                    count(buckets, pixels[v * size + u], 2);
                }
            }
        }
        // The hands: the bottom rows of the right arm's front (44..48, 29..32) and the left's (36..40, 61..64).
        for (int v = 29; v < 32; v++) {
            for (int u = 44; u < 47; u++) {
                count(buckets, pixels[v * size + u], 1);
            }
        }
        for (int v = 61; v < 64; v++) {
            for (int u = 36; u < 39; u++) {
                count(buckets, pixels[v * size + u], 1);
            }
        }
        long[] best = null;
        for (long[] bucket : buckets.values()) {
            if (best == null || bucket[0] > best[0]) {
                best = bucket;
            }
        }
        if (best == null || best[0] < 3) {
            return -1;
        }
        return (int)(best[1] / best[0]) << 16 | (int)(best[2] / best[0]) << 8 | (int)(best[3] / best[0]);
    }

    private static void count(Map<Integer, long[]> buckets, int argb, int weight) {
        if ((argb >>> 24) < 128) {
            return;
        }
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        // Not a skin colour at all (a mask, a lash, a mouth's dark): left out. Skin runs warm: red at least its blue.
        if (r < 40 || r < b - 10 || Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 150) {
            return;
        }
        int key = (r >> 5) << 6 | (g >> 5) << 3 | (b >> 5);
        long[] bucket = buckets.computeIfAbsent(key, ignored -> new long[4]);
        bucket[0] += weight;
        bucket[1] += (long)r * weight;
        bucket[2] += (long)g * weight;
        bucket[3] += (long)b * weight;
    }
}
