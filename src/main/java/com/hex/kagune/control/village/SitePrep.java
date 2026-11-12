package com.hex.kagune.control.village;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.57 (the owner: "FIX THE FUCKING STRUCTURE SPAWNING PLACEMENT, i asked multiple times to fix it, CLEAR THE FUCKING AREA OF SPAWN, CLEAR
 * ALL BLOCKS OR TREES, AND THEN PLACE IT DIRECTLY ON THE FLOOR, NOT FLYING, NOT HOVERING OVER THE FLOOR, I SAID ON IT."): a place's site made
 * ready before the place is built on it (pure, tested). The floor it stands on is its own footprint's ground (the middle of its heights:
 * never the highest, which left it standing on a plinth over its low side); everything over that floor across its footprint and
 * {@link #MARGIN} round it is cleared, trees and hills alike, by air tiles {@link #CLEAR} on a side laid before the place itself (the world's
 * own pieces, saved and loaded as any); under it, the ground is filled up to the floor (the apron round it too).
 */
public final class SitePrep {
    private SitePrep() {
    }

    /** The clearing template's size (a cube of air: data/hexkagune/structures/clearing.nbt). */
    public static final int CLEAR = 16;
    /**
     * How far round a place's footprint its site is graded (blocks): flat for {@link #FLAT}, then a bank of one block up or down a block
     * out to the land round it (cut into a hill, built up over a dip), never a cliff or a ledge at its edge.
     */
    public static final int MARGIN = 8;
    /** The flat apron round a place, before its bank. */
    public static final int FLAT = 2;
    /** How far over the highest ground of the site it is cleared (the tallest trees, a jungle's, reach about this). */
    public static final int ABOVE = 32;
    /** How far a structure's pieces may reach from its start chunk's least corner, back and on (its eight chunks either way). */
    public static final int REACH_BACK = 128;
    public static final int REACH_ON = 143;
    /** How far apart the ground is sampled under a footprint for its floor and its highest ground. */
    public static final int STEP = 4;
    /** About how many samples a side a footprint's are at most. */
    public static final int ACROSS = 24;

    /** A clearing tile: its least corner. */
    public record Tile(int x, int y, int z) {
    }

    /**
     * The starts of tiles {@link #CLEAR} long covering {@code min}..{@code max} (both in): every {@link #CLEAR} from {@code min}, the last
     * pulled back to end on {@code max} (it may overlap the one before; one tile if the run is shorter than a tile).
     */
    static int[] starts(int min, int max) {
        if (max < min) {
            return new int[0];
        }
        List<Integer> out = new ArrayList<>();
        for (int at = min; ; at += CLEAR) {
            if (at + CLEAR - 1 >= max) {
                out.add(Math.max(min, max - CLEAR + 1));
                break;
            }
            out.add(at);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The tiles covering every block from ({@code minX}, {@code fromY}, {@code minZ}) to ({@code maxX}, {@code topY}, {@code maxZ}). */
    public static List<Tile> tiles(int minX, int minZ, int maxX, int maxZ, int fromY, int topY) {
        List<Tile> out = new ArrayList<>();
        for (int y : starts(fromY, topY)) {
            for (int x : starts(minX, maxX)) {
                for (int z : starts(minZ, maxZ)) {
                    out.add(new Tile(x, y, z));
                }
            }
        }
        return out;
    }

    /** {@code min}..{@code max} kept within what a start chunk whose least corner is {@code corner} reaches: {low, high}. */
    public static int[] reach(int min, int max, int corner) {
        return new int[] {Math.max(min, corner - REACH_BACK), Math.min(max, corner + REACH_ON)};
    }

    /**
     * The columns sampled over a footprint: every {@link #STEP} blocks from its least corner (wider apart over a big one: about
     * {@link #ACROSS} samples a side, so the hideout's costs no more than a small place's), its far edges always among them.
     */
    public static int[][] samples(int minX, int minZ, int maxX, int maxZ) {
        int step = Math.max(STEP, (Math.max(maxX - minX, maxZ - minZ) + 1) / ACROSS);
        List<int[]> out = new ArrayList<>();
        for (int x = minX; ; x = Math.min(maxX, x + step)) {
            for (int z = minZ; ; z = Math.min(maxZ, z + step)) {
                out.add(new int[] {x, z});
                if (z >= maxZ) {
                    break;
                }
            }
            if (x >= maxX) {
                break;
            }
        }
        return out.toArray(new int[0][]);
    }

    /**
     * The floor a place stands on: the middle of its footprint's ground (each sample's first free height: the floor is its ground block, one
     * under). Never the highest (a plinth over the low side) nor the lowest (half of it dug in): the high side is cleared down to it, the
     * low side filled up to it.
     */
    public static int floor(DryLand.Ground ground, int minX, int minZ, int maxX, int maxZ) {
        int[][] at = samples(minX, minZ, maxX, maxZ);
        int[] heights = new int[at.length];
        for (int i = 0; i < at.length; i++) {
            heights[i] = ground.surface(at[i][0], at[i][1]);
        }
        return DryLand.median(heights);
    }

    /** The highest ground (first free height) over a footprint, at its samples. */
    public static int highest(DryLand.Ground ground, int minX, int minZ, int maxX, int maxZ) {
        int top = Integer.MIN_VALUE;
        for (int[] at : samples(minX, minZ, maxX, maxZ)) {
            top = Math.max(top, ground.surface(at[0], at[1]));
        }
        return top;
    }

    /**
     * How high the site is cleared: {@link #ABOVE} over its highest ground (what trees on it reach), and at least to the place's own top
     * (nothing of the world left inside it), never past the world's top ({@code ceiling}, its highest block).
     */
    public static int top(int highestGround, int placeTop, int ceiling) {
        return Math.min(ceiling, Math.max(highestGround + ABOVE, placeTop));
    }

    /** How far the ground at {@code d} blocks out from a footprint may stand over or under its floor: 0 on the apron, then a block a block. */
    public static int rise(int d) {
        return Math.max(0, d - FLAT);
    }

    /** The highest the bank reaches over the floor (at the margin's edge); the clearing's tiles take everything over the floor plus this. */
    public static int bank() {
        return rise(MARGIN);
    }

    /**
     * The tiles that clear a site, kept within its start's reach ({@code cornerX}, {@code cornerZ}: the start chunk's least corner): over its
     * footprint ({@code minX}..{@code maxX}, {@code minZ}..{@code maxZ}) everything from the first height over its floor ({@code floor}: its
     * ground block), and over the whole site with its {@link #MARGIN} everything from over its bank ({@link #bank}) up to {@code top}; the
     * bank between is cut and built after, a block at a time ({@link #cut}, {@link #fill}).
     */
    public static List<Tile> clearing(int minX, int minZ, int maxX, int maxZ, int cornerX, int cornerZ, int floor, int top) {
        int[] fx = reach(minX, maxX, cornerX);
        int[] fz = reach(minZ, maxZ, cornerZ);
        int[] xs = reach(minX - MARGIN, maxX + MARGIN, cornerX);
        int[] zs = reach(minZ - MARGIN, maxZ + MARGIN, cornerZ);
        List<Tile> out = new ArrayList<>(tiles(fx[0], fz[0], fx[1], fz[1], floor + 1, Math.min(top, floor + bank())));
        out.addAll(tiles(xs[0], zs[0], xs[1], zs[1], floor + 1 + bank(), top));
        return out;
    }

    /**
     * A clearing tile's ground height over its foot (a pool piece's ground delta): its ground is the place's floor's first free height for
     * every tile (0 for the tiles from it, less for the tiles over the bank's top), so the game's beard sees the whole site's ground at the
     * floor: it carves over it and builds under it, never up to a high tile's foot.
     */
    public static int groundDelta(int floor, int tileY) {
        return floor + 1 - tileY;
    }

    /** How far out of a footprint ({@code minX}..{@code maxX}, {@code minZ}..{@code maxZ}) column ({@code x}, {@code z}) is: 0 in it. */
    public static int distance(int x, int z, int minX, int minZ, int maxX, int maxZ) {
        int dx = x < minX ? minX - x : x > maxX ? x - maxX : 0;
        int dz = z < minZ ? minZ - z : z > maxZ ? z - maxZ : 0;
        return Math.max(dx, dz);
    }

    /** The lowest height cut away in a column {@code d} out (all of it over this, up to the bank's top, goes): the floor + 1 + its rise. */
    public static int cut(int floor, int d) {
        return floor + 1 + rise(d);
    }

    /** The highest height the ground is built up to in a column {@code d} out: the floor less its rise. */
    public static int fill(int floor, int d) {
        return floor - rise(d);
    }

    /**
     * The fill in a column {@code d} out (0: in the footprint, where the place has nothing at its floor): the heights from over its
     * {@code ground} (its first solid block that is no tree) up to {@link #fill}, ({from, to}, both in), or null if it stands that high
     * already or its ground is more than {@code deepest} under the floor (a ravine is left as it is).
     */
    public static int[] apron(int floor, int d, int ground, int deepest) {
        int to = fill(floor, d);
        if (ground >= to || floor - ground > deepest) {
            return null;
        }
        return new int[] {ground + 1, to};
    }
}
