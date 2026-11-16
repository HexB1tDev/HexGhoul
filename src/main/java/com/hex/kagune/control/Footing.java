package com.hex.kagune.control;

/**
 * 4.1.73 (the owner: "Make sure NPCS dont spawn in the ground at any point, especially official NPCS."): where a body put down near a height
 * really stands (pure, tested). Every home of the mod's NPCs is kept as a whole block (the block over their feet), so a body brought back,
 * sent home or put by its player was set at a whole height: half into a slab or a stair it was made on, into a carpet, or into whatever was
 * built there since. Here the column is looked at as the game sees it: the top of each block's collision (a slab's half, a carpet's
 * sixteenth, a full block's top) is a floor; the nearest floor to the height wanted (a little down, a few blocks up) where the whole body
 * has room is where it stands.
 */
public final class Footing {
    /** How many blocks over the height wanted a body may be stood (out of the ground it was put into, onto what was built there). */
    public static final int UP = 3;
    /** How many blocks under it (onto a floor a little lower: a home made over a slab since broken). */
    public static final int DOWN = 2;
    /** How deep (blocks) a body is in the blocks round it before it counts as in them (Homeward's: a crowd's push never does). */
    public static final double INSIDE = 0.15;

    private Footing() {
    }

    /** A column as the game has it at one spot: each block's collision top, and whether a body has room. */
    public interface Column {
        /** The top (0 to 1.5 over the block's foot: a fence's is 1.5) of block {@code y}'s collision there, or NaN if it has none. */
        double top(int y);

        /** True if the whole body has room standing with its feet at {@code feet} (no block in it). */
        boolean free(double feet);
    }

    /**
     * The height a body put down at {@code want} stands at: the floor (a collision top) nearest it, from {@link #DOWN} under to {@link #UP}
     * over, where the body has room; on a tie, the higher. NaN if there is none (it is left where it was put: the game's own push and the
     * guard over it, NpcFooting, see to it).
     */
    public static double stand(double want, Column column) {
        int base = (int)Math.floor(want);
        double best = Double.NaN;
        double bestOff = Double.MAX_VALUE;
        for (int y = base - DOWN - 1; y <= base + UP; y++) {
            double top = column.top(y);
            if (Double.isNaN(top) || top <= 0.0) {
                continue;
            }
            double feet = y + top;
            double off = Math.abs(feet - want);
            if (feet < want - DOWN - 1.0E-6 || feet > want + UP + 1.0E-6) {
                continue;
            }
            if (off < bestOff - 1.0E-9 || Math.abs(off - bestOff) <= 1.0E-9 && feet > best) {
                if (column.free(feet)) {
                    best = feet;
                    bestOff = off;
                }
            }
        }
        return best;
    }

    /** How many checks in a row a body must be found in the blocks before it is stood out of them (a door shut on it, a shove: not). */
    public static final int SEEN = 2;

    /** True if a body found in the blocks {@code times} checks running is to be stood out of them now. */
    public static boolean stuck(int times) {
        return times >= SEEN;
    }
}
