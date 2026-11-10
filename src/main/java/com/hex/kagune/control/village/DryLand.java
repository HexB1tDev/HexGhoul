package com.hex.kagune.control.village;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 4.1.1 (the owner: "bug, make sure NONE of the structures spawns in water or collides with water bodies, the mansion is currently happily
 * resting at the very bottom of the ocean..."): where a place may stand (pure, tested). Over the place's whole footprint and
 * {@link #MARGIN} blocks out round it there may be no water: no wet biome (a sea, a river, a beach, a swamp: data/hexkagune/tags/worldgen/
 * biome/wet.json) at any of the columns looked at every {@link #BIOME_STEP} blocks (the biomes' own grain), and no water over the ground
 * (a lake, a pond: the world's ground counted with water and without) at any of those every {@link #STEP} blocks. The coarse columns are
 * looked at first, so a sea is seen at once.
 */
public final class DryLand {
    /** Blocks between two columns whose ground is looked at (water over it), and between two whose biome is. */
    public static final int STEP = 16;
    public static final int BIOME_STEP = 4;
    /** How far out round the footprint it must be dry too (no water at its walls). */
    public static final int MARGIN = 8;
    /** How far a structure's pieces may reach from its start chunk's corner (8 chunks either way: the game places no further). */
    public static final int REACH_BACK = 128;
    public static final int REACH_ON = 143;

    /**
     * The hideout's structure set (data/hexkagune/worldgen/structure_set/aogiri_hideout.json, a random spread): one start chunk offered in
     * every {@link #SPACING} x {@link #SPACING} chunks, as the game picks it ({@link #spreadChunk}). Of those {@link #CHOICES} nearest the
     * world's middle (no nearer than {@link #NEAREST} chunks), the world's one hideout is built at the first on dry land.
     */
    public static final int SPACING = 40;
    public static final int SEPARATION = 20;
    public static final int SALT = 1873602243;
    /** Regions either way of the world's middle whose offers are weighed. */
    public static final int REGIONS = 4;
    public static final int NEAREST = 32;

    /** The world as the game will make it: at a column, its first free height counting water as ground, and not (its two WG heightmaps). */
    public interface Ground {
        int surface(int x, int z);

        int floor(int x, int z);

        /** True if the biome at block ({@code x}, {@code y}, {@code z}) is a wet one (a sea, a river, a beach, a swamp). */
        boolean wet(int x, int y, int z);
    }

    private DryLand() {
    }

    /**
     * The columns ({x, z}) looked at for a footprint {@code sizeX} x {@code sizeZ} from ({@code minX}, {@code minZ}): every {@code step}
     * blocks from {@link #MARGIN} out on one side to {@link #MARGIN} out on the other (its far edges always among them), the coarse ones
     * first (every eighth, then every fourth, every second, then the rest).
     */
    public static int[][] columns(int minX, int minZ, int sizeX, int sizeZ, int step) {
        int[] xs = line(minX, sizeX, step);
        int[] zs = line(minZ, sizeZ, step);
        List<int[]> out = new ArrayList<>(xs.length * zs.length);
        boolean[][] taken = new boolean[xs.length][zs.length];
        for (int every = 8; every >= 1; every /= 2) {
            for (int i = 0; i < xs.length; i++) {
                for (int j = 0; j < zs.length; j++) {
                    boolean on = (i % every == 0 || i == xs.length - 1) && (j % every == 0 || j == zs.length - 1);
                    if (on && !taken[i][j]) {
                        taken[i][j] = true;
                        out.add(new int[] {xs[i], zs[j]});
                    }
                }
            }
        }
        return out.toArray(new int[0][]);
    }

    /** The coordinates looked at along one side: from {@link #MARGIN} before {@code min} to {@link #MARGIN} past its last block. */
    static int[] line(int min, int size, int step) {
        int from = min - MARGIN;
        int to = min + Math.max(1, size) - 1 + MARGIN;
        int n = (to - from + step - 1) / step;
        int[] at = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            at[i] = Math.min(to, from + i * step);
        }
        return at;
    }

    /**
     * True if there is no water under the footprint or round it: no wet biome at the height {@code y} (its ground's) every
     * {@link #BIOME_STEP} blocks, then no water over the ground every {@link #STEP} (stops at the first wet column).
     */
    public static boolean dry(Ground ground, int y, int minX, int minZ, int sizeX, int sizeZ) {
        for (int[] column : columns(minX, minZ, sizeX, sizeZ, BIOME_STEP)) {
            if (ground.wet(column[0], y, column[1])) {
                return false;
            }
        }
        for (int[] column : columns(minX, minZ, sizeX, sizeZ, STEP)) {
            if (ground.surface(column[0], column[1]) > ground.floor(column[0], column[1])) {
                return false;
            }
        }
        return true;
    }

    /**
     * The start chunk ({x, z}) the game offers in region ({@code rx}, {@code rz}) of a random spread (linear) with these numbers: its
     * RandomSpreadStructurePlacement.getPotentialStructureChunk (a legacy random, java.util.Random's own, seeded by the region and the salt).
     */
    public static int[] spreadChunk(long seed, int rx, int rz, int spacing, int separation, int salt) {
        java.util.Random random = new java.util.Random((long)rx * 341873128712L + (long)rz * 132897987541L + seed + (long)salt);
        int range = spacing - separation;
        int x = random.nextInt(range);
        int z = random.nextInt(range);
        return new int[] {rx * spacing + x, rz * spacing + z};
    }

    /**
     * The hideout's offers in the order they are weighed: every region's start chunk {@link #REGIONS} regions either way of the world's
     * middle, no nearer it than {@link #NEAREST} chunks, nearest first (then by x, then z).
     */
    public static int[][] hideoutOffers(long seed) {
        List<int[]> offers = new ArrayList<>();
        for (int rx = -REGIONS; rx < REGIONS; rx++) {
            for (int rz = -REGIONS; rz < REGIONS; rz++) {
                int[] chunk = spreadChunk(seed, rx, rz, SPACING, SEPARATION, SALT);
                if ((long)chunk[0] * chunk[0] + (long)chunk[1] * chunk[1] >= (long)NEAREST * NEAREST) {
                    offers.add(chunk);
                }
            }
        }
        offers.sort(Comparator.<int[]>comparingLong(c -> (long)c[0] * c[0] + (long)c[1] * c[1]).thenComparingInt(c -> c[0]).thenComparingInt(c -> c[1]));
        return offers.toArray(new int[0][]);
    }

    /**
     * Chunk offsets ({dx, dz}) a start may be moved by to find dry land, up to {@code most} either way: none first, then the nearest
     * (then by x, then z).
     */
    public static int[][] shifts(int most) {
        List<int[]> out = new ArrayList<>();
        for (int dx = -most; dx <= most; dx++) {
            for (int dz = -most; dz <= most; dz++) {
                out.add(new int[] {dx, dz});
            }
        }
        out.sort(Comparator.<int[]>comparingInt(d -> d[0] * d[0] + d[1] * d[1]).thenComparingInt(d -> d[0]).thenComparingInt(d -> d[1]));
        return out.toArray(new int[0][]);
    }

    /**
     * The least corners along one side a run of {@code size} blocks may take from a start chunk whose corner is {@code corner}, staying in
     * its reach: {@code want} first, then every {@code step} out from it either way, nearest first.
     */
    public static int[] runs(int want, int corner, int size, int step) {
        int lo = corner - REACH_BACK;
        int hi = corner + REACH_ON - size + 1;
        if (hi < lo) {
            return new int[] {lo};
        }
        int first = Math.max(lo, Math.min(hi, want));
        List<Integer> out = new ArrayList<>();
        out.add(first);
        for (int d = step; first - d >= lo || first + d <= hi; d += step) {
            if (first - d >= lo) {
                out.add(first - d);
            }
            if (first + d <= hi) {
                out.add(first + d);
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Up to {@code most} least corners ({x, z}) to try a footprint at, from the runs along each side ({@link #runs}, each nearest its
     * first): the first pair first, then the nearest to it (then by x, then z).
     */
    public static int[][] spots(int[] xs, int[] zs, int most) {
        List<int[]> out = new ArrayList<>(xs.length * zs.length);
        for (int x : xs) {
            for (int z : zs) {
                out.add(new int[] {x, z});
            }
        }
        int x0 = xs[0];
        int z0 = zs[0];
        out.sort(Comparator.<int[]>comparingLong(c -> (long)(c[0] - x0) * (c[0] - x0) + (long)(c[1] - z0) * (c[1] - z0))
            .thenComparingInt(c -> c[0]).thenComparingInt(c -> c[1]));
        return out.subList(0, Math.min(most, out.size())).toArray(new int[0][]);
    }

    /** True if a footprint from ({@code minX}, {@code minZ}) to ({@code maxX}, {@code maxZ}) stays in the reach of the chunk cornered there. */
    public static boolean reachable(int cornerX, int cornerZ, int minX, int minZ, int maxX, int maxZ) {
        return minX >= cornerX - REACH_BACK && maxX <= cornerX + REACH_ON && minZ >= cornerZ - REACH_BACK && maxZ <= cornerZ + REACH_ON;
    }

    /** How the ground at one of the hideout's offers (its start chunk) is judged ({@link #choose}). */
    public interface Judge {
        /** The dry ground's heights on a 5 x 5 grid over all its start chunk reaches, or null if there is water over any of them. */
        int[] grid(int chunkX, int chunkZ);

        /** True if it may stand in the biome at its start (the middle of its start chunk, at height {@code y}). */
        boolean biome(int chunkX, int chunkZ, int y);

        /** True if there is no water under all its start chunk reaches or round it ({@link #dry}). */
        boolean dry(int chunkX, int chunkZ, int y);
    }

    /**
     * The first of {@code offers} ({x, z} start chunks, in order) on dry land in a biome it may stand in, its ground no more than
     * {@code flat} blocks from highest to lowest; failing that, the first dry one however steep; as {x, z, its ground's height}, or null
     * if there is none.
     */
    public static int[] choose(int[][] offers, Judge judge, int flat) {
        List<int[]> steep = new ArrayList<>();
        for (int[] offer : offers) {
            int[] floor = judge.grid(offer[0], offer[1]);
            if (floor == null || floor.length == 0) {
                continue;
            }
            int middle = median(floor);
            if (!judge.biome(offer[0], offer[1], middle)) {
                continue;
            }
            if (spread(floor) > flat) {
                steep.add(new int[] {offer[0], offer[1], middle});
            } else if (judge.dry(offer[0], offer[1], middle)) {
                return new int[] {offer[0], offer[1], middle};
            }
        }
        for (int[] offer : steep) {
            if (judge.dry(offer[0], offer[1], offer[2])) {
                return offer;
            }
        }
        return null;
    }

    /** How far apart {@code heights}' highest and lowest are. */
    public static int spread(int[] heights) {
        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        for (int at : heights) {
            lo = Math.min(lo, at);
            hi = Math.max(hi, at);
        }
        return heights.length == 0 ? 0 : hi - lo;
    }

    /** The middle (rounded down) of {@code heights}, sorted. */
    public static int median(int[] heights) {
        int[] sorted = heights.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }
}
