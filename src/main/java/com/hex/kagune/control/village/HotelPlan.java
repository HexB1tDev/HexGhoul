package com.hex.kagune.control.village;

/**
 * 4.1.72 (the owner: "from now on the small city is gone, erased. / From now on close to a village will be an hotel where humans work, live
 * and walk around doing their own thing. / CCG patrols around the hotel at all times."; asked how many, they chose "Beside some villages"):
 * where the hotels stand (pure). A village stands at its set's offer in a region (the world's own village placement, so a pack that moves
 * villages moves hotels with them); one village in {@link #ONE_IN} has a hotel ({@link #chosen}), on one side of it (north, east, south or
 * west: {@link #side}), its start chunk {@link #DISTANCE} chunks out from the village's: the hotel's footprint, its cleared margin and the
 * ground its terrain fit moves never meet the village's pieces. Its side is the first, in an order the world's seed gives the village, whose
 * spot no other village's offer comes near ({@link #CLEAR_OF}): never a hotel on a neighbouring village. Of two hotels that would overlap,
 * the one beside the village first in (x, then z) order stands ({@link #hotelOf}). The hotel is turned so that its forecourt (its file's
 * south side) faces its village ({@link #turns}).
 */
public final class HotelPlan {
    /** One village in this many has a hotel beside it. */
    public static final int ONE_IN = 4;
    /**
     * How far (chunks) a hotel's start chunk is from its village's, on its side. Its near edge is then 16 x 14 - 105 = 119 blocks out of the
     * village's start corner at the least (the north and west sides: the footprint centres on its chunk's middle); its clearing reaches 8
     * further in ({@link SitePrep#MARGIN}), its terrain fit 12 more; a village's pieces reach 80 from its start piece's middle, that piece
     * up to some 16 off the corner: 96.
     */
    public static final int DISTANCE = 14;
    /** How far (chunks) a hotel reaches from its start chunk either way (its 196 blocks, half of them, and a little). */
    public static final int REACH = 7;
    /** How far (chunks) another place's start chunk is kept outside a hotel's reach. */
    public static final int MARGIN = 6;
    /** How far (chunks) a village's pieces may reach from its start chunk (96 blocks: 80 from its start piece's middle, and that piece). */
    public static final int VILLAGE_REACH = 6;
    /** How far (chunks) from a hotel's start chunk no other village's offer may be: its reach, a village's, and one between. */
    public static final int CLEAR_OF = REACH + VILLAGE_REACH + 1;
    /** How far apart (chunks, either way) two hotels' start chunks are at the least: their reaches never meet. */
    public static final int APART = 2 * REACH + 1;
    /** The most chunks between two offsets of a {@link #scan}. */
    public static final int SCAN_GAP = 6;
    /** How far apart (blocks) the biome is looked at over a hotel's footprint and round it ({@link #onPlains}). */
    public static final int PLAINS_STEP = 16;

    /**
     * 4.1.72 (the owner: "Only spawns on plains or biomes o plenty equivallent biomes, we cant have that shit spawning on mountains"): true
     * if every column looked at over a hotel's footprint ({@code minX}, {@code minZ}, {@code sizeX} x {@code sizeZ}) and a little round it
     * ({@link DryLand#columns}, every {@link #PLAINS_STEP} blocks (a chunk), its edges too) is a plains biome ({@code plains}: hexkagune:plains, vanilla's
     * plains and Biomes O' Plenty's open grasslands): never half on a mountain, a forest or anything else.
     */
    public static boolean onPlains(int minX, int minZ, int sizeX, int sizeZ, java.util.function.BiPredicate<Integer, Integer> plains) {
        for (int[] column : DryLand.columns(minX, minZ, sizeX, sizeZ, PLAINS_STEP)) {
            if (!plains.test(column[0], column[1])) {
                return false;
            }
        }
        return true;
    }

    /** The village biome tags (vanilla's has_structure/village_*: where each kind of village may stand). */
    public static final String[] VILLAGE_TAGS = {"village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga"};
    /** Each side's step from the village (chunks, x then z): north, east, south, west. */
    private static final int[][] STEP = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private HotelPlan() {
    }

    /** The villages' offers: the start chunk (x, z) its spread offers in the region holding chunk ({@code x}, {@code z}). */
    @FunctionalInterface
    public interface Offers {
        int[] at(int x, int z);
    }

    private static long mix(long seed, int x, int z) {
        long h = seed ^ 0x5DEECE66DL ^ (long)x * 341873128712L ^ (long)z * 132897987541L ^ 0x48_4F_54_45_4CL;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        return h ^ h >>> 33;
    }

    /** True if the village at chunk ({@code x}, {@code z}) is one with a hotel beside it (one in {@link #ONE_IN}, by the world's seed). */
    public static boolean chosen(long seed, int x, int z) {
        return Math.floorMod(mix(seed, x, z), ONE_IN) == 0;
    }

    /** The side the village at chunk ({@code x}, {@code z}) tries first for its hotel (0 north, 1 east, 2 south, 3 west), by the seed. */
    public static int first(long seed, int x, int z) {
        return (int)Math.floorMod(mix(seed, x, z) >>> 8, 4L);
    }

    /**
     * The side of the village at chunk ({@code x}, {@code z}) its hotel stands on: the first, from {@link #first} round clockwise, whose
     * spot has no other village's offer within {@link #CLEAR_OF} chunks ({@link #neighbours}); -1 if every side has one.
     */
    public static int side(long seed, Offers offers, int x, int z) {
        int start = first(seed, x, z);
        for (int k = 0; k < 4; k++) {
            int side = (start + k) % 4;
            int[] h = hotel(x, z, side);
            if (neighbours(offers, h[0], h[1], x, z).length == 0) {
                return side;
            }
        }
        return -1;
    }

    /** The hotel's start chunk for a village at chunk ({@code x}, {@code z}), on {@code side}. */
    public static int[] hotel(int x, int z, int side) {
        return new int[] {x + STEP[side][0] * DISTANCE, z + STEP[side][1] * DISTANCE};
    }

    /** The village chunk a hotel started at chunk ({@code x}, {@code z}) on {@code side} would stand beside. */
    public static int[] village(int x, int z, int side) {
        return new int[] {x - STEP[side][0] * DISTANCE, z - STEP[side][1] * DISTANCE};
    }

    /**
     * The hotel beside the village at chunk ({@code x}, {@code z}): {start chunk x, z, side}, or null if it has none: not {@link #chosen},
     * no side clear of the other villages ({@link #side}), or a hotel of a village before it in (x, then z) order within {@link #APART} of
     * it (the first one stands: two never overlap). Whether a village really stands there (its biome) and the hotel's land are the world's
     * to judge (ControlStructures.Site.hotel).
     */
    public static int[] hotelOf(long seed, Offers offers, int x, int z) {
        if (!chosen(seed, x, z)) {
            return null;
        }
        int side = side(seed, offers, x, z);
        if (side < 0) {
            return null;
        }
        int[] h = hotel(x, z, side);
        for (int[] other : offersNear(offers, h[0], h[1], APART - 1 + DISTANCE)) {
            boolean before = other[0] < x || other[0] == x && other[1] < z;
            if (!before || !chosen(seed, other[0], other[1])) {
                continue;
            }
            int theirs = side(seed, offers, other[0], other[1]);
            if (theirs < 0) {
                continue;
            }
            int[] g = hotel(other[0], other[1], theirs);
            if (Math.abs(g[0] - h[0]) < APART && Math.abs(g[1] - h[1]) < APART) {
                return null;
            }
        }
        return new int[] {h[0], h[1], side};
    }

    /** The offsets -{@code reach} to {@code reach}, both ends in, evenly spread no more than {@link #SCAN_GAP} apart. */
    public static int[] scan(int reach) {
        int steps = Math.max(1, (2 * reach + SCAN_GAP - 1) / SCAN_GAP);
        int[] out = new int[steps + 1];
        for (int i = 0; i <= steps; i++) {
            out[i] = -reach + 2 * reach * i / steps;
        }
        return out;
    }

    /**
     * Every offer within {@code reach} chunks (either way) of chunk ({@code x}, {@code z}), each once: the regions are asked every
     * {@link #SCAN_GAP} chunks or closer, both ends of the square too, so none of a spread at least that wide is missed.
     */
    public static int[][] offersNear(Offers offers, int x, int z, int reach) {
        java.util.Map<Long, int[]> found = new java.util.LinkedHashMap<>();
        int[] scan = scan(reach);
        for (int dx : scan) {
            for (int dz : scan) {
                int[] offer = offers.at(x + dx, z + dz);
                if (Math.abs(offer[0] - x) <= reach && Math.abs(offer[1] - z) <= reach) {
                    found.putIfAbsent((long)offer[0] << 32 ^ offer[1] & 0xFFFFFFFFL, offer);
                }
            }
        }
        return found.values().toArray(new int[0][]);
    }

    /** The village offers but its own ({@code vx}, {@code vz}) within {@link #CLEAR_OF} chunks of a hotel started at ({@code hx}, {@code hz}). */
    public static int[][] neighbours(Offers offers, int hx, int hz, int vx, int vz) {
        return java.util.Arrays.stream(offersNear(offers, hx, hz, CLEAR_OF)).filter(o -> o[0] != vx || o[1] != vz).toArray(int[][]::new);
    }

    /**
     * True if a hotel stands within {@link #MARGIN} chunks of start chunk ({@code x}, {@code z}) by the villages' offers alone ({@link
     * #hotelOf}: whether a village really stands there or not, which costs no biome; ControlStructures.inHotel keeps every other place clear
     * of them so).
     */
    public static boolean near(long seed, Offers offers, int x, int z) {
        for (int[] v : offersNear(offers, x, z, DISTANCE + REACH + MARGIN)) {
            int[] h = hotelOf(seed, offers, v[0], v[1]);
            if (h != null && covers(h[0], h[1], x, z, MARGIN)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The quarter turns (ControlStructures.Site's: 0 none, 1 clockwise, 2 half, 3 counter-clockwise) that face the hotel's forecourt (its
     * file's south, +z) toward its village: north of it, south; east of it, west; and so on.
     */
    public static int turns(int side) {
        return side;
    }

    /** Where +z of the file faces once turned {@code turns} (x, z): as a template turns (clockwise: x, z to -z, x). */
    public static int[] front(int turns) {
        int[] f = {0, 1};
        for (int i = 0; i < turns; i++) {
            f = new int[] {-f[1], f[0]};
        }
        return f;
    }

    /** True if a hotel started at chunk ({@code hx}, {@code hz}) reaches chunk ({@code x}, {@code z}) with {@code margin} chunks to spare. */
    public static boolean covers(int hx, int hz, int x, int z, int margin) {
        return Math.abs(x - hx) <= REACH + margin && Math.abs(z - hz) <= REACH + margin;
    }
}
