package com.hex.kagune.control;

/**
 * 6.0.4: how mastery is earned (pure, tested). Gameplay earns raw points (the original's: 4 a kill, 1 a hit, a grapple, a guard
 * held, a hunt; Ukaku's shots and the restraint the same way), and each point of mastery costs more raw the higher it is: the
 * first costs one, a point at 100 two, at 500 six, at 900 ten. Reaching 1000 takes 5995 raw where it took 1000: the first
 * kills come as fast as ever, the kakuja take hundreds. Progress toward the next point carries over (in thousandths, exactly).
 */
public final class MasteryCurve {
    public static final int MAX = 1000;
    /** Thousandths of a raw point. */
    public static final int MILLI = 1000;

    private MasteryCurve() {
    }

    /** What the point from {@code mastery} to the next costs (thousandths of a raw point): 1000 at 0, 2000 at 100, 11000 at 1000. */
    public static int cost(int mastery) {
        return MILLI + 10 * Math.max(0, Math.min(MAX, mastery));
    }

    /**
     * {mastery after, thousandths carried toward the next point} for {@code raw} points earned at {@code mastery} with
     * {@code carry} already toward the next. At the top nothing more is carried.
     */
    public static int[] earn(int mastery, int carry, int raw) {
        int m = Math.max(0, Math.min(MAX, mastery));
        long progress = Math.max(0, carry) + (long)Math.max(0, raw) * MILLI;
        while (m < MAX && progress >= cost(m)) {
            progress -= cost(m);
            m++;
        }
        return new int[] {m, m >= MAX ? 0 : (int)progress};
    }

    /** Raw points it takes to climb from 0 to {@code mastery}. */
    public static long rawTo(int mastery) {
        long milli = 0L;
        for (int m = 0; m < Math.max(0, Math.min(MAX, mastery)); m++) {
            milli += cost(m);
        }
        return milli / MILLI;
    }
}
