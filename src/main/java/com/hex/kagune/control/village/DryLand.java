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

        /** 4.1.36: true if the biome there is a sea (rivers, beaches and swamps are not); as {@link #wet} where not told apart. */
        default boolean sea(int x, int y, int z) {
            return this.wet(x, y, z);
        }
    }

    private DryLand() {
    }

    /** 4.1.17: how far apart (blocks) the ground is sampled under a place's footprint for {@link #lift}. */
    public static final int LIFT_STEP = 4;

    /**
     * 4.1.17 (the owner: "they are spawning under the ground, which cannot happen, bro. This is causing major fucking chunks to be missing. It
     * should be always spawning above ground, always at the top max area"): how many blocks a place started over ({@code x}, {@code z}) must
     * rise so it stands on the highest ground of its footprint ({@code minX}..{@code maxX}, {@code minZ}..{@code maxZ}, sampled every
     * {@link #LIFT_STEP} and at its edges), never cut into a hillside under it (its terrain adaptation then builds up under its low side
     * instead of carving the hill out round it). 0 if the ground there is already the highest.
     */
    public static int lift(Ground ground, int x, int z, int minX, int minZ, int maxX, int maxZ) {
        int here = ground.surface(x, z);
        int top = here;
        for (int sx = minX; ; sx = Math.min(maxX, sx + LIFT_STEP)) {
            for (int sz = minZ; ; sz = Math.min(maxZ, sz + LIFT_STEP)) {
                top = Math.max(top, ground.surface(sx, sz));
                if (sz >= maxZ) {
                    break;
                }
            }
            if (sx >= maxX) {
                break;
            }
        }
        return Math.max(0, top - here);
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
        return offers(seed, SALT, 0, REGIONS, false);
    }

    /**
     * 4.1.6 (the owner: "I cant get the aogiri hideout to spawn or the ccg building at all"): the hideout's further offers, weighed only
     * when none of the near ones ({@link #hideoutOffers}) is dry: every region's start chunk from {@link #REGIONS} out to {@link #FAR}
     * regions either way of the world's middle, nearest first.
     */
    public static int[][] hideoutFurther(long seed) {
        return offers(seed, SALT, REGIONS, FAR, false);
    }

    /** 4.1.6: how many regions either way of the world's middle the further offers reach. */
    public static final int FAR = 8;

    /**
     * The start chunks the game offers (a random spread of {@link #SPACING} with {@code salt}) in the regions from {@code inner} out to
     * {@code outer} either way of the world's middle (the square {@code -outer .. outer - 1} less {@code -inner .. inner - 1}), none
     * nearer it than {@link #NEAREST} chunks, nearest first (then by x, then z); for the CCG building ({@code apart}), less those near a
     * spot of the hideout's or a village's.
     */
    static int[][] offers(long seed, int salt, int inner, int outer, boolean apart) {
        // 4.1.36: the villages' spots are gone; the Small City is kept clear of where it is really built (ControlStructures.inCity).
        Spread[] keep = apart ? new Spread[] {fixed(seed, SPACING, SEPARATION, SALT)} : new Spread[0];
        return offers(fixed(seed, SPACING, SEPARATION, salt), inner, outer, keep, CCG_APART);
    }

    /**
     * 4.1.9: a structure set's random spread as the game has it: the start chunk it offers in the region holding any chunk. In the game it
     * is the set's own placement, read from the world (RandomSpreadStructurePlacement.getPotentialStructureChunk, its spacing): a mod that
     * changes every set's spacing (the owner's pack has Structure Essentials, which can) changes the offers here too, so the one chosen is
     * always one the game asks about. {@link #fixed} is the set's file's numbers.
     */
    public interface Spread {
        /** Chunks along a side of one of its regions. */
        int spacing();

        /** The start chunk ({x, z}) it offers in the region holding chunk ({@code x}, {@code z}). */
        int[] chunk(int x, int z);
    }

    /** 4.1.9: a linear random spread with these numbers ({@link #spreadChunk}), as the game makes it from a set's file. */
    public static Spread fixed(long seed, int spacing, int separation, int salt) {
        return new Spread() {
            @Override
            public int spacing() {
                return spacing;
            }

            @Override
            public int[] chunk(int x, int z) {
                return spreadChunk(seed, Math.floorDiv(x, spacing), Math.floorDiv(z, spacing), spacing, separation, salt);
            }
        };
    }

    /**
     * The start chunks {@code spread} offers in the regions from {@code inner} out to {@code outer} either way of the world's middle (the
     * square {@code -outer .. outer - 1} less {@code -inner .. inner - 1}), none nearer it than {@link #NEAREST} chunks, nor within
     * {@code apart} chunks of an offer of any of {@code keep}; each once, nearest first (then by x, then z).
     */
    public static int[][] offers(Spread spread, int inner, int outer, Spread[] keep, int apart) {
        int spacing = Math.max(1, spread.spacing());
        List<int[]> offers = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int rx = -outer; rx < outer; rx++) {
            for (int rz = -outer; rz < outer; rz++) {
                if (rx >= -inner && rx < inner && rz >= -inner && rz < inner) {
                    continue;
                }
                int[] chunk = spread.chunk(rx * spacing, rz * spacing);
                if ((long)chunk[0] * chunk[0] + (long)chunk[1] * chunk[1] < (long)NEAREST * NEAREST || !seen.add((long)chunk[0] << 32 ^ (chunk[1] & 0xFFFFFFFFL))) {
                    continue;
                }
                boolean clear = true;
                for (Spread other : keep) {
                    clear &= !near(other, chunk[0], chunk[1], apart);
                }
                if (clear) {
                    offers.add(chunk);
                }
            }
        }
        offers.sort(Comparator.<int[]>comparingLong(c -> (long)c[0] * c[0] + (long)c[1] * c[1]).thenComparingInt(c -> c[0]).thenComparingInt(c -> c[1]));
        return offers.toArray(new int[0][]);
    }

    /**
     * 4.1.4 (the owner: "CCG building ... like the others only 1 of them spawns"): the CCG building's structure set
     * (data/hexkagune/worldgen/structure_set/ccg_building.json), a random spread of the hideout's spacing with its own salt. The world's
     * one is built at the first of its offers on dry land ({@link #choose}) that is no nearer than {@link #CCG_APART} chunks to any spot
     * the hideout or a village may be built at (their own sets' offers: the mansion and the towns are up to 272 blocks wide), so it never
     * stands in one of them.
     */
    public static final int CCG_SALT = 1873602245;
    public static final int CCG_APART = 12;
    /**
     * 4.1.36: the Small City's set (data/hexkagune/worldgen/structure_set/small_city.json, its own placement hexkagune:city with these
     * numbers: {@link CityPlan}). It replaces the villages' (34, 20). A region of 48 chunks holds at most one city (34 chunks wide, its
     * offer moved at most 12): no two ever touch; one a region at most, on dry, fairly flat land only: rarer than a vanilla village (34).
     */
    public static final int CITY_SPACING = 48;
    public static final int CITY_SEPARATION = 36;
    public static final int CITY_SALT = 1873602250;
    /**
     * 4.1.36: how far (chunks) a one-of-a-kind place's start chunk is kept outside a Small City that is built (its square of chunks): the
     * widest place's half (the hideout's, 9) and one more.
     */
    public static final int CITY_MARGIN = 10;

    /** 4.1.4: true if the start chunk ({@code x}, {@code z}) is within {@code apart} chunks of an offer of the random spread with these numbers. */
    public static boolean near(long seed, int x, int z, int apart, int spacing, int separation, int salt) {
        return near(fixed(seed, spacing, separation, salt), x, z, apart);
    }

    /** 4.1.9: true if the start chunk ({@code x}, {@code z}) is within {@code apart} chunks of an offer of {@code spread}. */
    public static boolean near(Spread spread, int x, int z, int apart) {
        int spacing = Math.max(1, spread.spacing());
        int rx = Math.floorDiv(x, spacing);
        int rz = Math.floorDiv(z, spacing);
        int reach = apart / spacing + 1;
        for (int i = rx - reach; i <= rx + reach; i++) {
            for (int j = rz - reach; j <= rz + reach; j++) {
                int[] other = spread.chunk(i * spacing, j * spacing);
                long dx = other[0] - (long)x;
                long dz = other[1] - (long)z;
                if (dx * dx + dz * dz < (long)apart * apart) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 4.1.4: the CCG building's offers in the order they are weighed: as the hideout's ({@link #hideoutOffers}: every region's start chunk
     * {@link #REGIONS} regions either way of the world's middle, no nearer it than {@link #NEAREST} chunks, nearest first), with its own
     * salt, less those near a spot of the hideout's or a village's.
     */
    public static int[][] ccgOffers(long seed) {
        return offers(seed, CCG_SALT, 0, REGIONS, true);
    }

    /** 4.1.6: the CCG building's further offers (as {@link #hideoutFurther}), weighed only when none of the near ones is dry. */
    public static int[][] ccgFurther(long seed) {
        return offers(seed, CCG_SALT, REGIONS, FAR, true);
    }

    /**
     * 4.1.9 (the owner: "it should be able to spawn literally in any biome as long as there's only one of each structure aside from the
     * villages"): Anteiku, the library and Ken's tower are the world's one each too (they were three each, rings of three), chosen in this
     * order after the hideout and the CCG building. Each has a random spread of the hideout's spacing with its own salt
     * (data/hexkagune/worldgen/structure_set/&lt;id&gt;.json); its one is built at the first of its offers, nearest the world's middle,
     * where the place itself stands once laid out and moved onto dry land as it always is ({@link #firstPlaced}), at least
     * {@link #PLACE_APART} chunks from every village offer and from the middle of every one-of-a-kind place chosen before it.
     */
    public static final String[] PLACES = {"anteiku", "library", "cyber_tower", "reaper_mansion"};
    public static final int ANTEIKU_SALT = 1324066190;
    public static final int LIBRARY_SALT = 1300741852;
    public static final int TOWER_SALT = 1324066191;
    /** 4.1.24 (the owner: "I think we need a structure for him ... there's always only one of them also in the map"): Arima's mansion. */
    public static final int MANSION_SALT = 1324066192;
    /** 4.1.9: how far (chunks) a one-of-a-kind place's middle keeps from a village's offer and from the places chosen before it. */
    public static final int PLACE_APART = 12;
    /** 4.1.9: how many offers that look like land are laid out in full before a world goes without the place (none on land). */
    public static final int PLACE_TRIES = 24;

    /** 4.1.9: the salt of the one-of-a-kind jigsaw place {@code id}'s set, or -1 if it is not one of {@link #PLACES}. */
    public static int placeSalt(String id) {
        if (id == null) {
            return -1;
        }
        return switch (id) {
            case "anteiku" -> ANTEIKU_SALT;
            case "library" -> LIBRARY_SALT;
            case "cyber_tower" -> TOWER_SALT;
            case "reaper_mansion" -> MANSION_SALT;
            default -> -1;
        };
    }

    /** 4.1.9: the place {@code id}'s offers in the order they are weighed (as {@link #hideoutOffers}, with its own salt). */
    public static int[][] placeOffers(long seed, String id) {
        return offers(seed, placeSalt(id), 0, REGIONS, false);
    }

    /** 4.1.9: the place {@code id}'s further offers (as {@link #hideoutFurther}), weighed only once the near ones are all tried. */
    public static int[][] placeFurther(long seed, String id) {
        return offers(seed, placeSalt(id), REGIONS, FAR, false);
    }

    /** 4.1.9: how a one-of-a-kind jigsaw place is tried at one of its offers ({@link #firstPlaced}). */
    public interface Probe {
        /** True if the offer's start chunk looks like land at all (no water and no wet biome at its middle): worth laying out. */
        boolean land(int chunkX, int chunkZ);

        /**
         * The chunk ({x, z}) the place's middle stands in once laid out from this offer as the world would lay it (moved onto dry land),
         * or null if it would not stand there.
         */
        int[] placed(int chunkX, int chunkZ);
    }

    /**
     * 4.1.9: the first of {@code near}, then of {@code further} (offers, {x, z} start chunks, in order) that looks like land, stands when
     * laid out, and stands where {@code clear} allows; as {offer x, offer z, its middle's chunk x, z}, or null. No more than {@code most}
     * offers are laid out (each costs a whole layout), so a world of sea gives up quickly.
     */
    public static int[] firstPlaced(int[][] near, int[][] further, Probe probe, java.util.function.Predicate<int[]> clear, int most) {
        int tried = 0;
        for (int[][] offers : new int[][][] {near, further}) {
            for (int[] offer : offers) {
                if (!probe.land(offer[0], offer[1])) {
                    continue;
                }
                int[] at = probe.placed(offer[0], offer[1]);
                if (at != null && clear.test(at)) {
                    return new int[] {offer[0], offer[1], at[0], at[1]};
                }
                if (++tried >= most) {
                    return null;
                }
            }
        }
        return null;
    }

    /** 4.1.9: true if chunk ({@code x}, {@code z}) is at least {@code chunks} from every one of {@code others} ({x, z} chunks). */
    public static boolean apart(int x, int z, List<int[]> others, int chunks) {
        for (int[] other : others) {
            long dx = other[0] - (long)x;
            long dz = other[1] - (long)z;
            if (dx * dx + dz * dz < (long)chunks * chunks) {
                return false;
            }
        }
        return true;
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

        /**
         * 4.1.6: {wetness, its ground's height} ({@link DryLand#wetness}, counted no further than {@code most}), or null if its middle is
         * not dry land in a biome it may stand in, or too little of it is dry land to stand on.
         */
        int[] wetness(int chunkX, int chunkZ, int most);
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

    /**
     * 4.1.6 (the owner: "I cant get the aogiri hideout to spawn or the ccg building at all"): the world's one hideout (or CCG building),
     * always somewhere if any of its offers is land: the first near offer that is dry ({@link #choose}); failing that, the first further
     * one that is; failing that, the driest near one ({@link #driest}: its middle on dry land, the least water under it), then the driest
     * further one. A world whose near offers held a dry one gets the same spot as before 4.1.6; one whose had none (no hideout before)
     * now gets one. Null only if none of them stands on land in a biome it may stand in.
     */
    public static int[] pick(int[][] near, int[][] further, Judge judge, int flat) {
        int[] chosen = choose(near, judge, flat);
        if (chosen == null) {
            chosen = choose(further, judge, flat);
        }
        if (chosen == null) {
            chosen = driest(near, judge);
        }
        if (chosen == null) {
            chosen = driest(further, judge);
        }
        return chosen;
    }

    /**
     * 4.1.6: of {@code offers}, the one with the least water under it ({@link Judge#wetness}: its middle on dry land in a biome it may
     * stand in), the first of them on a tie; as {x, z, its ground's height}, or null if none stands on land.
     */
    public static int[] driest(int[][] offers, Judge judge) {
        int[] best = null;
        int least = Integer.MAX_VALUE;
        for (int[] offer : offers) {
            int[] wet = judge.wetness(offer[0], offer[1], least - 1);
            if (wet == null || wet[0] >= least) {
                continue;
            }
            least = wet[0];
            best = new int[] {offer[0], offer[1], wet[1]};
            if (least == 0) {
                break;
            }
        }
        return best;
    }

    /**
     * 4.1.6: how much water lies under a footprint and {@link #MARGIN} round it, as {@link #dry} looks (a wet biome every
     * {@link #BIOME_STEP} blocks counts 1, water over the ground every {@link #STEP} counts {@code (STEP / BIOME_STEP)^2}: the ground
     * each stands for); it stops counting once past {@code most} (and says so by returning more than it).
     */
    public static int wetness(Ground ground, int y, int minX, int minZ, int sizeX, int sizeZ, int most) {
        int wet = 0;
        for (int[] column : columns(minX, minZ, sizeX, sizeZ, BIOME_STEP)) {
            if (ground.wet(column[0], y, column[1]) && ++wet > most) {
                return wet;
            }
        }
        int weight = (STEP / BIOME_STEP) * (STEP / BIOME_STEP);
        for (int[] column : columns(minX, minZ, sizeX, sizeZ, STEP)) {
            if (ground.surface(column[0], column[1]) > ground.floor(column[0], column[1])) {
                wet += weight;
                if (wet > most) {
                    return wet;
                }
            }
        }
        return wet;
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
