package com.hex.kagune.control;

/**
 * 1.2.2: the mod's sounds never pile into noise (pure, tested). Moves layer a few sounds on a blow, and fast moves blow often: with the
 * user's pack (fuller, longer sounds than vanilla's), the same one landing on itself again and again, or dozens at once in a melee,
 * would be a wall of noise. Each of the mod's sounds about to play on a client is asked here first ({@link #allow}):
 * <ul>
 *   <li>the very same sound again within {@link #SAME_NANOS} near where it last began is let go (it would only double itself);</li>
 *   <li>no more than {@link #PER_SOUND} of one sound begin within {@link #WINDOW_NANOS} near each other;</li>
 *   <li>no more than {@link #ALL} of the mod's sounds begin within {@link #WINDOW_NANOS} anywhere.</li>
 * </ul>
 * Everything else plays.
 */
public final class SfxLimit {
    public static final long SAME_NANOS = 50_000_000L;
    public static final long WINDOW_NANOS = 200_000_000L;
    public static final int PER_SOUND = 3;
    public static final int ALL = 14;
    /** How near (blocks, squared) two plays of a sound count as the same place. */
    public static final double NEAR_SQR = 4.0 * 4.0;
    private static final int KEPT = 64;

    private final String[] keys = new String[KEPT];
    private final double[] xs = new double[KEPT];
    private final double[] ys = new double[KEPT];
    private final double[] zs = new double[KEPT];
    private final long[] at = new long[KEPT];
    private int next;

    /** True if {@code key} may begin at ({@code x, y, z}) {@code now} (nanoseconds); a play let through is remembered. */
    public boolean allow(String key, double x, double y, double z, long now) {
        int same = 0;
        int all = 0;
        for (int i = 0; i < KEPT; i++) {
            if (this.keys[i] == null || now - this.at[i] > WINDOW_NANOS || now < this.at[i]) {
                continue;
            }
            all++;
            if (!this.keys[i].equals(key)) {
                continue;
            }
            double dx = this.xs[i] - x;
            double dy = this.ys[i] - y;
            double dz = this.zs[i] - z;
            if (dx * dx + dy * dy + dz * dz > NEAR_SQR) {
                continue;
            }
            if (now - this.at[i] < SAME_NANOS) {
                return false;
            }
            same++;
        }
        if (same >= PER_SOUND || all >= ALL) {
            return false;
        }
        this.keys[this.next] = key;
        this.xs[this.next] = x;
        this.ys[this.next] = y;
        this.zs[this.next] = z;
        this.at[this.next] = now;
        this.next = (this.next + 1) % KEPT;
        return true;
    }
}
