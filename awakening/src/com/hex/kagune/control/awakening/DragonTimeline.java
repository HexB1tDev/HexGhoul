package com.hex.kagune.control.awakening;

/**
 * Fixed schedule of the Dragon Kakuja awakening, in server ticks (20 per second).
 *
 * <pre>
 *   0 ─ ERUPTION ─ 40 ─────────── RAMPAGE (exactly 1200 = 60 s) ─────────── 1240 ─ DESTABILIZE ─ 1310 ─ EMERGENCE ─ 1360
 *                                                                                                  ^ shatter, final form unlocked
 * </pre>
 *
 * The player has no control from tick 0 until {@link #END_TICK}.
 */
public final class DragonTimeline {
    public static final int TICKS_PER_SECOND = 20;
    /** Rinkaku's body erupts into the expanding mass. */
    public static final int ERUPTION_TICKS = 40;
    /** The autonomous rampage: exactly sixty seconds. */
    public static final int RAMPAGE_TICKS = 60 * TICKS_PER_SECOND;
    /** The body cracks, sags and collapses before it shatters. */
    public static final int DESTABILIZE_TICKS = 70;
    /** Fragments clear and Rinkaku stands in the final form before control returns. */
    public static final int EMERGENCE_TICKS = 50;

    public static final int RAMPAGE_START = ERUPTION_TICKS;
    public static final int RAMPAGE_END = RAMPAGE_START + RAMPAGE_TICKS;
    public static final int SHATTER_TICK = RAMPAGE_END + DESTABILIZE_TICKS;
    public static final int END_TICK = SHATTER_TICK + EMERGENCE_TICKS;

    public enum Phase { ERUPTION, RAMPAGE, DESTABILIZE, EMERGENCE, DONE }

    private DragonTimeline() {
    }

    public static Phase phaseAt(int tick) {
        if (tick < RAMPAGE_START) return Phase.ERUPTION;
        if (tick < RAMPAGE_END) return Phase.RAMPAGE;
        if (tick < SHATTER_TICK) return Phase.DESTABILIZE;
        if (tick < END_TICK) return Phase.EMERGENCE;
        return Phase.DONE;
    }

    /** Body girth multiplier: a rapidly expanding mass that reaches full size eight seconds in. */
    public static double girth(int tick) {
        if (tick >= SHATTER_TICK) return 0.0;
        double erupt = smooth(tick / (double) ERUPTION_TICKS);
        double swell = smooth(tick / 160.0);
        return 0.18 + 0.42 * erupt + 0.40 * swell;
    }

    /** 0 while the Dragon is stable, rising to 1 at the moment it shatters. */
    public static double instability(int tick) {
        if (tick < RAMPAGE_END) return 0.0;
        if (tick >= SHATTER_TICK) return 1.0;
        return smooth((tick - RAMPAGE_END) / (double) DESTABILIZE_TICKS);
    }

    /** True while heads and tentacles may start new attacks. */
    public static boolean attacking(int tick) {
        return tick >= 12 && tick < RAMPAGE_END;
    }

    public static double smooth(double t) {
        double x = Math.max(0.0, Math.min(1.0, t));
        return x * x * (3.0 - 2.0 * x);
    }
}
