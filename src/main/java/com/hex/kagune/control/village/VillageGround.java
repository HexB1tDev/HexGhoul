package com.hex.kagune.control.village;

/**
 * 2.0.1: where a village may start (pure, tested). The owner: "the villages are spawning in big bodies of water when they should look for
 * dry land". The ground is looked at in a 5 x 5 grid over its 256 blocks ({@link #SAMPLES}, from its middle): no water over the ground at
 * any of them (the world's surface, water counted, no higher than its floor), and the ground no more than {@link #FLAT} blocks from
 * highest to lowest (it is placed at its middle's height: the world's bumps and dips stay off its streets).
 */
public final class VillageGround {
    /** Offsets {dx, dz} from the village's middle where the ground is looked at. */
    public static final int[][] SAMPLES;
    /** The highest and lowest ground under it at most this far apart. */
    public static final int FLAT = 8;

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
        for (int i = 0; i < surface.length; i++) {
            if (surface[i] > floor[i]) {
                return false;
            }
            lo = Math.min(lo, floor[i]);
            hi = Math.max(hi, floor[i]);
        }
        return surface.length > 0 && hi - lo <= FLAT;
    }
}
