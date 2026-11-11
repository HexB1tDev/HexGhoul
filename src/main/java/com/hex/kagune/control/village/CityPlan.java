package com.hex.kagune.control.village;

/**
 * 4.1.36 (the owner: "ALl villages get replaced with the city in my minecraft world that I sent named "Small-City" ... add
 * Hexkagune:small_city. This big structure is relatively rare (a bit rarer than a normal village)"; they chose the whole city): where the
 * Small City stands (pure). The city is 544 x 544, four 272 x 272 quarters ({@link SiteMaps}: small_city_0 north-west, 1 north-east, 2
 * south-west, 3 south-east, as the owner built it), and one structure only reaches 8 chunks from its start chunk, so it is built as four
 * starts of hexkagune:small_city, one per quarter, 17 chunks apart: the four slots of a city. Its set (structure_set/small_city.json, the
 * placement hexkagune:city) offers, in each region of its random spread ({@link DryLand#CITY_SPACING}), the region's start chunk (the
 * north-west slot's) and the three others beside it. Each quarter works out the same turn (from the world's seed and that chunk) and the
 * same ground (the whole city's), so the four are always built together, side by side, as one city, or none of them.
 */
public final class CityPlan {
    /** A quarter's side (blocks, chunks), the city's side, and the chunks from the north-west slot's start chunk to the city's middle. */
    public static final int QUARTER = 272;
    public static final int QUARTER_CHUNKS = 17;
    public static final int SIZE = 2 * QUARTER;
    public static final int MIDDLE = 9;
    /** The quarters' sites, by number. */
    public static final String[] QUARTERS = {"small_city_0", "small_city_1", "small_city_2", "small_city_3"};
    /** The most the city's ground may rise and fall (blocks, its highest less its lowest of {@link #SAMPLES} x {@link #SAMPLES}). */
    public static final int FLAT = 10;
    /** 4.1.42: 17 x 17 (every 34 blocks: 9 x 9 every 68 missed whole hillsides and valleys between them, and the city hung over them). */
    public static final int SAMPLES = 17;
    /**
     * 4.1.36 (the owner: "that city should be able to go over rivers. It doesn't really matter. Just make sure it blends well with the
     * terrain"): water no deeper than this (blocks over its bed) may lie under it: a river, a swamp's pools, a pond; not a lake or a sea.
     */
    public static final int DEEP = 8;
    /** 4.1.36: its streets' lowest (the first free height over its ground): over the sea's level (63), so no carved street fills with water. */
    public static final int ABOVE_SEA = 64;

    /**
     * 4.1.36: the city's ground (its first free height) from its samples' levels (the top of the ground, or of the water over it): their
     * middle (its terrain adaptation cuts the land over its streets inside it and fills the land up under its edges, so it meets the land
     * both ways), never under {@link #ABOVE_SEA}; or {@link Integer#MIN_VALUE} if they rise and fall more than {@link #FLAT}.
     */
    public static int ground(int[] levels) {
        if (levels.length == 0 || DryLand.spread(levels) > FLAT) {
            return Integer.MIN_VALUE;
        }
        return Math.max(ABOVE_SEA, DryLand.median(levels));
    }

    private CityPlan() {
    }

    /**
     * 4.1.42: the whole city's ground (its streets' first free height) on {@code ground} for the city whose least corner is ({@code minX},
     * {@code minZ}), or {@link Integer#MIN_VALUE} if it may not stand there: {@link #SAMPLES} x SAMPLES levels (the top of the ground or of
     * the water over it) rising and falling more than {@link #FLAT} ({@link #ground}), water deeper than {@link #DEEP} at any of them or at
     * any column every 32 blocks, or a sea's or a steep biome at any column every 16.
     */
    public static int land(DryLand.Ground ground, int minX, int minZ) {
        int n = SAMPLES;
        int[] levels = new int[n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = minX + 2 + (SIZE - 5) * i / (n - 1);
                int z = minZ + 2 + (SIZE - 5) * j / (n - 1);
                int floor = ground.floor(x, z);
                int surface = ground.surface(x, z);
                if (surface - floor > DEEP) {
                    return Integer.MIN_VALUE;
                }
                levels[i * n + j] = Math.max(surface, floor);
            }
        }
        int middle = ground(levels);
        if (middle == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        for (int[] column : DryLand.columns(minX, minZ, SIZE, SIZE, 16)) {
            if (ground.sea(column[0], middle, column[1]) || ground.steep(column[0], middle, column[1])) {
                return Integer.MIN_VALUE;
            }
        }
        for (int[] column : DryLand.columns(minX, minZ, SIZE, SIZE, 32)) {
            if (ground.surface(column[0], column[1]) - ground.floor(column[0], column[1]) > DEEP) {
                return Integer.MIN_VALUE;
            }
        }
        return middle;
    }

    /** Slot {@code slot}'s start chunk from the north-west slot's ({x, z}): 0 north-west, 1 north-east, 2 south-west, 3 south-east. */
    public static int[] slotOffset(int slot) {
        return new int[] {(slot & 1) * QUARTER_CHUNKS, (slot >> 1) * QUARTER_CHUNKS};
    }

    /**
     * If chunk ({@code x}, {@code z}) is one of the four start chunks of a city {@code offers} offers: {slot, its north-west slot's chunk x,
     * z}; else null.
     */
    public static int[] slot(DryLand.Spread offers, int x, int z) {
        for (int s = 0; s < 4; s++) {
            int[] off = slotOffset(s);
            int[] base = offers.chunk(x - off[0], z - off[1]);
            if (base[0] + off[0] == x && base[1] + off[1] == z) {
                return new int[] {s, base[0], base[1]};
            }
        }
        return null;
    }

    /** The city's turn (quarter turns clockwise) at the offer whose north-west chunk is ({@code bx}, {@code bz}): the same for its four. */
    public static int turn(long seed, int bx, int bz) {
        long h = seed ^ (bx * 0x9E3779B97F4A7C15L) ^ (bz * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (int)(h & 3L);
    }

    /** The slot quarter {@code quarter} lands in when the city is turned {@code turns} quarter turns clockwise. */
    public static int slotOf(int quarter, int turns) {
        int qx = (quarter & 1) * QUARTER;
        int qz = (quarter >> 1) * QUARTER;
        int[] a = VillageRules.turn(qx, qz, turns);
        int[] b = VillageRules.turn(qx + QUARTER - 1, qz + QUARTER - 1, turns);
        int[] least = SiteMaps.turnedLeast(SIZE, SIZE, turns);
        int x = Math.min(a[0], b[0]) - least[0];
        int z = Math.min(a[1], b[1]) - least[1];
        return x / QUARTER + 2 * (z / QUARTER);
    }

    /** The quarter that stands in slot {@code slot} when the city is turned {@code turns}. */
    public static int quarterIn(int slot, int turns) {
        for (int k = 0; k < 4; k++) {
            if (slotOf(k, turns) == slot) {
                return k;
            }
        }
        throw new IllegalArgumentException("slot " + slot);
    }

    /** The city's least corner (block x, z) for the offer whose north-west chunk is ({@code bx}, {@code bz}). */
    public static int[] corner(int bx, int bz) {
        return new int[] {bx * 16 - 8 * 16, bz * 16 - 8 * 16};
    }

    /** The first and last chunk (either way) of the city whose north-west start chunk is {@code b}: 8 back, 8 on past the 17 of the far slot. */
    public static int firstChunk(int b) {
        return b - 8;
    }

    public static int lastChunk(int b) {
        return b + QUARTER_CHUNKS + 8;
    }

    /** True if chunk ({@code x}, {@code z}) is in the city whose north-west start chunk is ({@code bx}, {@code bz}), or within {@code margin} of it. */
    public static boolean covers(int bx, int bz, int x, int z, int margin) {
        return x >= firstChunk(bx) - margin && x <= lastChunk(bx) + margin && z >= firstChunk(bz) - margin && z <= lastChunk(bz) + margin;
    }

    /** Slot {@code slot}'s least corner (block x, z): all its start chunk reaches (8 chunks back, 8 on). */
    public static int[] slotCorner(int bx, int bz, int slot) {
        int[] c = corner(bx, bz);
        int[] off = slotOffset(slot);
        return new int[] {c[0] + off[0] * 16, c[1] + off[1] * 16};
    }
}
