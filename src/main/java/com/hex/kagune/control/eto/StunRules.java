package com.hex.kagune.control.eto;

/**
 * 6.2.0: what Eto's fists do to a body's will to fight back. Every Speed Blitz strike that lands stuns what it hits: for
 * {@link #TICKS} (the heavy ones {@link #HEAVY_TICKS}) it can't hurt anything (its blows, its arrows, its slams all come to
 * nothing) and can't walk. Every hit renews it, so a combo keeps it helpless, but never further than {@link #CAP} ahead.
 */
public final class StunRules {
    /** How long a strike stuns (ticks): half a second. */
    public static final int TICKS = 10;
    /** How long the heavy strikes (the uppercut, the palms, the finishers) stun. */
    public static final int HEAVY_TICKS = 16;
    /** A stun never runs on further than this from now, however many hits pile up. */
    public static final int CAP = 30;
    /** The slowness that holds a stunned body still (amplifier: 7 levels of 15% is all of its own speed). */
    public static final int SLOW_AMPLIFIER = 6;

    private StunRules() {
    }

    /** When a stun that ran to {@code until} runs to after a hit of {@code ticks} now: renewed, never shortened, capped. */
    public static long renewed(long until, long now, int ticks) {
        long fresh = now + Math.max(0, ticks);
        long kept = Math.max(until, fresh);
        return Math.min(kept, now + CAP);
    }

    /** True while a stun that runs to {@code until} holds. */
    public static boolean holds(long until, long now) {
        return now < until;
    }

    /**
     * 7.0.2 (fair to players): the longest a player can be kept stunned without a break (ticks): hits that keep renewing it past this
     * no longer hold him, and he breaks free (out of anything that holds him by the stun too); then he can't be stunned again for
     * {@link #PLAYER_IMMUNE}. Mobs have no such limit.
     */
    public static final int PLAYER_CHAIN = 40;
    public static final int PLAYER_IMMUNE = 40;

    /** True if a player stunned without a break since {@code since} breaks free at {@code now}. */
    public static boolean breaksFree(long since, long now) {
        return now - since >= PLAYER_CHAIN;
    }
}
