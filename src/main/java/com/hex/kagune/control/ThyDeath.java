package com.hex.kagune.control;

/**
 * 2.4.5 (pure, tested; the owner: "Rapid Strike -> Becomes 'Thy Death' - All active tendrils auto seek out targets and attack independently
 * while the move button is held"): Rinkaku's Z. While it is held, every tail out hunts on its own: each picks a foe within {@link #range} (a
 * different one from its siblings where there are enough), lunges at it from its own side ({@link #strike}: out, the point through it at
 * {@link #HIT_AT}, back by {@link #STRIKE}), and goes again; each tail a little behind the one before so they never strike as one.
 */
public final class ThyDeath {
    private ThyDeath() {
    }

    /** One lunge (ticks): out and back; its point is through the foe at {@link #HIT_AT}. */
    public static final int STRIKE = 12;
    public static final int HIT_AT = 6;
    /** How far behind the tail before it each tail starts (ticks), and the pause after a lunge before its next. */
    public static final int STAGGER = 3;
    public static final int REST = 2;
    /** How long a press or a held ping keeps it going (ticks): the client pings every 4 while it is held. */
    public static final int HELD = 10;

    /** How far (blocks) the tails reach for foes: 7 at mastery 0, 10 at 1000. */
    public static double range(int mastery) {
        return 7.0 + 3.0 * clamp01(mastery / 1000.0);
    }

    /** What one lunge does: 4 at mastery 0, 10 at 1000. */
    public static float damage(int mastery) {
        return (float)(4.0 + 6.0 * clamp01(mastery / 1000.0));
    }

    /** When tail {@code limb} may first lunge after Thy Death begins at {@code start}. */
    public static long first(long start, int limb) {
        return start + (long)STAGGER * Math.max(0, limb);
    }

    /** How far out a lunge {@code age} ticks in is (0 at rest, 1 with its point through the foe), smooth out and back. */
    public static double reach(double age) {
        if (age <= 0.0 || age >= STRIKE) {
            return 0.0;
        }
        if (age <= HIT_AT) {
            double t = age / HIT_AT;
            return t * t * (3.0 - 2.0 * t);
        }
        double t = (age - HIT_AT) / (STRIKE - HIT_AT);
        return 1.0 - t * t * (3.0 - 2.0 * t);
    }

    /**
     * {@code path} (the tail as it is, {@code path[0]} its root) bent toward {@code target} as a lunge {@code age} ticks in: each point
     * pulled toward its place on an arc from the root to the target ({@code arch} blocks up and out to the tail's {@code side} at its
     * middle, {@code sideX}/{@code sideZ} the owner's right), by {@link #reach}, the root kept. A new array; the input untouched.
     */
    public static double[][] strike(double[][] path, double[] target, double age, double side, double sideX, double sideZ, double arch) {
        double w = reach(age);
        double[][] out = new double[path.length][];
        double[] root = path[0];
        int n = path.length;
        for (int i = 0; i < n; i++) {
            double s = n == 1 ? 1.0 : i / (double)(n - 1);
            double bow = Math.sin(Math.PI * s) * arch;
            double[] on = {root[0] + (target[0] - root[0]) * s + sideX * side * bow, root[1] + (target[1] - root[1]) * s + bow * 0.8,
                root[2] + (target[2] - root[2]) * s + sideZ * side * bow};
            double k = i == 0 ? 0.0 : w;
            out[i] = new double[] {path[i][0] + (on[0] - path[i][0]) * k, path[i][1] + (on[1] - path[i][1]) * k, path[i][2] + (on[2] - path[i][2]) * k};
        }
        return out;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
