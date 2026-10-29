package com.hex.kagune.control.king;

/**
 * 1.0.20: the final kakujas' fighting grammar (pure, tested), the same for War and Peace, each with blows of its own.
 * <ul>
 *   <li>M1: the chain. Holding jump on a blow after {@link #LAUNCH_AFTER} chained ones throws the launcher instead (the body goes up,
 *       and he goes up after it). In the air every M1 is an air blow: the body is kept floating in front of him and he hangs there with
 *       it; the {@link #AIR_HITS}th after the first ends it with the spike (driven down into the ground, he plunging after it, the ground
 *       breaking where it lands).</li>
 *   <li>RMB: the heavy. Tapped, a slow blow that breaks a guard; held {@link #CHARGE_TICKS} and let go (or held to {@link #CHARGE_MAX}),
 *       the charged one, far bigger; right after a blow ({@link #ENDER_WINDOW}), the ender, worth more for every blow chained before it;
 *       in the air, the spike.</li>
 * </ul>
 */
public final class KingCombo {
    /** What the client says with a blow: the jump key held; off the ground; (a heavy) pressed, not let go. */
    public static final int JUMP = 1;
    public static final int AIR = 2;
    public static final int PRESS = 4;

    /** A launcher needs this many blows chained before it. */
    public static final int LAUNCH_AFTER = 2;
    /** Air blows before the spike. */
    public static final int AIR_HITS = 3;
    /** A heavy held this long (ticks) is charged; held this long it goes off on its own. */
    public static final int CHARGE_TICKS = 12;
    public static final int CHARGE_MAX = 40;
    /** A heavy this soon after a blow ended (ticks), or during one, is the ender; worth this much more for each blow chained before it. */
    public static final int ENDER_WINDOW = 14;
    public static final double ENDER_PER_HIT = 0.15;
    /** After a launch, how long (ticks) his M1s are air blows even if he has not left the ground yet. */
    public static final int CHASE_TICKS = 30;

    /** The launcher: how hard the body goes up (blocks a tick), and he after it. */
    public static final double LAUNCH_UP = 1.15;
    public static final double CHASE_UP = 1.02;
    /** An air blow: the body floated up a little (so it stays in front of him), he hung where he is. */
    public static final double FLOAT_UP = 0.3;
    public static final double HANG_UP = 0.12;
    /** The spike: how hard the body is driven down, how hard he plunges after it; what its landing does round it (blocks, damage). */
    public static final double SPIKE_DOWN = 1.7;
    public static final double PLUNGE_DOWN = 1.25;
    public static final double CRATER = 3.0;
    public static final float CRATER_DAMAGE = 6.0F;
    /** How long a body is held in the air by blows (stun ticks) and after a launch. */
    public static final int AIR_STUN = 14;
    public static final int LAUNCH_STUN = 30;

    private KingCombo() {
    }

    /** What an M1 is, by what came before it and how it was clicked. */
    public enum Blow {
        /** The next blow of the chain on the ground. */
        CHAIN,
        /** The launcher. */
        LAUNCH,
        /** An air blow. */
        AIR,
        /** The spike that ends the air blows. */
        SPIKE
    }

    /**
     * The M1 thrown, {@code chained} blows into the chain (0: a fresh one), {@code airHits} air blows into the air chain, with
     * {@code flags} as the client clicked it, {@code air} as the server sees him (off the ground, or chasing what he launched).
     */
    public static Blow blow(int chained, int airHits, int flags, boolean air) {
        if (air) {
            return airHits >= AIR_HITS ? Blow.SPIKE : Blow.AIR;
        }
        if ((flags & JUMP) != 0 && chained >= LAUNCH_AFTER) {
            return Blow.LAUNCH;
        }
        return Blow.CHAIN;
    }

    /** What a heavy is. */
    public enum Heavy {
        /** Tapped: the guard breaker. */
        TAP,
        /** Held and let go: the charged one. */
        CHARGED,
        /** Right after a blow: the ender. */
        ENDER,
        /** In the air: the spike. */
        SPIKE,
        /** Pressed on the ground with nothing before it: begin charging (what it is is known when it is let go). */
        CHARGING
    }

    /**
     * The heavy pressed ({@code press}) or let go after being held {@code held} ticks: in the air the spike; pressed {@code sinceBlow}
     * ticks after a blow (or during one, 0) with {@code chained} blows before it, the ender; else it charges, and is the tap or the
     * charged one when let go.
     */
    public static Heavy heavy(boolean press, boolean air, int chained, long sinceBlow, long held) {
        if (!press) {
            return held >= CHARGE_TICKS ? Heavy.CHARGED : Heavy.TAP;
        }
        if (air) {
            return Heavy.SPIKE;
        }
        if (chained >= 1 && sinceBlow >= 0 && sinceBlow <= ENDER_WINDOW) {
            return Heavy.ENDER;
        }
        return Heavy.CHARGING;
    }

    /** What the ender is worth after {@code chained} blows (times its own damage). */
    public static double ender(int chained) {
        return 1.0 + ENDER_PER_HIT * Math.max(0, Math.min(8, chained));
    }

    /** How far through the charge a heavy held {@code held} ticks is (0..1). */
    public static double charge(double held) {
        return Math.max(0.0, Math.min(1.0, held / CHARGE_TICKS));
    }
}
