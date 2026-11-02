package com.hex.kagune.control.village;

/**
 * 2.0.1: where a village may start (pure, tested). The owner: "the villages are spawning in big bodies of water when they should look for
 * dry land". The ground is looked at in a 5 x 5 grid over its 256 blocks ({@link #SAMPLES}, from its middle): no water over the ground at
 * the 9 middle ones, at most {@link #WET_EDGE} of the 16 round the edge (a river or a pond at its rim, never the sea), and the dry ground
 * no more than {@link #FLAT} blocks from highest to lowest (it is placed at its middle's height). 2.0.2 (the owner: "currently cant even
 * find a village with locate structure"): FLAT 12 (was 8) and a little water at the rim allowed (was none anywhere).
 */
public final class VillageGround {
    /** Offsets {dx, dz} from the village's middle where the ground is looked at. */
    public static final int[][] SAMPLES;
    /** The highest and lowest dry ground under it at most this far apart. */
    public static final int FLAT = 12;
    /** At most this many of the samples round its edge may have water over the ground; the middle ones none. */
    public static final int WET_EDGE = 2;
    /** Samples this far out (either way) are its edge's. */
    static final int EDGE = 112;

    static {
        int[] at = {-112, -56, 0, 56, 112};
        SAMPLES = new int[at.length * at.length][];
        for (int i = 0; i < at.length; i++) {
            for (int j = 0; j < at.length; j++) {
                SAMPLES[i * at.length + j] = new int[] {at[i], at[j]};
            }
        }
    }

    private VillageGround() {
    }

    /**
     * True if a village may stand here: {@code surface[i]} the world's first free height at sample i counting water as ground,
     * {@code floor[i]} the same not counting it (water over the ground: the surface higher than the floor).
     */
    public static boolean suits(int[] surface, int[] floor) {
        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        int wet = 0;
        for (int i = 0; i < surface.length; i++) {
            if (surface[i] > floor[i]) {
                if (!edge(i) || ++wet > WET_EDGE) {
                    return false;
                }
                continue;
            }
            lo = Math.min(lo, floor[i]);
            hi = Math.max(hi, floor[i]);
        }
        return surface.length > 0 && hi >= lo && hi - lo <= FLAT;
    }

    /** True if sample {@code i} is one round the edge. */
    static boolean edge(int i) {
        return i < SAMPLES.length && (Math.abs(SAMPLES[i][0]) == EDGE || Math.abs(SAMPLES[i][1]) == EDGE);
    }
}
