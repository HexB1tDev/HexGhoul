package com.hex.kagune.control.family;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.3.10: the main colour of the top of a player's head, read off their skin (pure): the head's top face (and the hat layer over it where
 * it is opaque) and the two rows of the face's front under it (a fringe); the commonest colour, near colours counted together, averaged.
 */
public final class HairColour {
    private HairColour() {
    }

    /** {@code pixels} a 64 x 64 skin (ARGB, row by row); the hair's colour (RGB), or -1 if nothing opaque is there. */
    public static int of(int[] pixels, int size) {
        if (pixels == null || size < 64 || pixels.length < size * size) {
            return -1;
        }
        Map<Integer, long[]> buckets = new HashMap<>();
        // The top of the head (8..16, 0..8); the hat layer's top (40..48, 0..8) where opaque, over it; the face's first two rows.
        for (int v = 0; v < 8; v++) {
            for (int u = 8; u < 16; u++) {
                int hat = pixels[v * size + u + 32];
                count(buckets, (hat >>> 24) > 128 ? hat : pixels[v * size + u], 2);
            }
        }
        for (int v = 8; v < 10; v++) {
            for (int u = 8; u < 16; u++) {
                int hat = pixels[v * size + u + 32];
                count(buckets, (hat >>> 24) > 128 ? hat : pixels[v * size + u], 1);
            }
        }
        long[] best = null;
        for (long[] bucket : buckets.values()) {
            if (best == null || bucket[0] > best[0]) {
                best = bucket;
            }
        }
        if (best == null || best[0] == 0) {
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
        int key = (r >> 5) << 6 | (g >> 5) << 3 | (b >> 5);
        long[] bucket = buckets.computeIfAbsent(key, ignored -> new long[4]);
        bucket[0] += weight;
        bucket[1] += (long)r * weight;
        bucket[2] += (long)g * weight;
        bucket[3] += (long)b * weight;
    }
}
