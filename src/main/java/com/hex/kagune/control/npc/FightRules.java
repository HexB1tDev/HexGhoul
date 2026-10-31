package com.hex.kagune.control.npc;

/**
 * 1.3.24: when an NPC (a companion: Eto, Touka, Ken; a child that has been taught to fight) takes on a hostile mob and when it lets it
 * go (pure). The owner: "NPCs will now attack hostile mobs in a 5 block radius, once starting to attack a hostile mob, they will not stop
 * until it's dead, or gone far away enough" (they used to notice one only within three blocks and drop it a few steps off their home).
 * Kept until dead, or {@link #gone} far off it, or chased too far from home, or {@link #STUCK} without getting at it (out of reach up a
 * cliff, behind a wall: never stood staring at it for ever).
 */
public final class FightRules {
    /** A hostile mob this close (blocks) is fought. */
    public static final double NOTICE = 5.0;
    /** A companion lets it go once it is this far off (blocks). */
    public static final double GONE = 24.0;
    /** A child, smaller and mortal, lets it go sooner (blocks). */
    public static final double CHILD_GONE = 16.0;
    /** How far from its home (or the one it follows) a companion chases (blocks); its leash while it fights. */
    public static final double CHASE = 32.0;
    /** How long without getting at it (reaching it, or landing a blow) it keeps trying (ticks: 30 s). */
    public static final long STUCK = 600L;
    /** Close enough to count as having got at it (blocks). */
    public static final double REACHED = 4.0;

    private FightRules() {
    }

    /** True if a mob {@code distance} off is close enough to be taken on. */
    public static boolean notices(double distance) {
        return distance <= NOTICE;
    }

    /** How far off a fighter lets it go ({@code child}: a child's shorter chase). */
    public static double gone(boolean child) {
        return child ? CHILD_GONE : GONE;
    }

    /**
     * True if it keeps fighting what it fights: {@code distance} off it, {@code fromHome} from where it chases from (negative: no
     * home to keep to), {@code sinceReached} ticks since it last got at it (Long.MAX_VALUE: never).
     */
    public static boolean keeps(double distance, double fromHome, long sinceReached, boolean child) {
        return distance <= gone(child) && (fromHome < 0.0 || fromHome <= CHASE) && sinceReached >= 0L && sinceReached <= STUCK;
    }

    /** Ticks since {@code at} (Long.MIN_VALUE: never), never overflowing: Long.MAX_VALUE for never, 0 for a time ahead of now. */
    public static long since(long now, long at) {
        return at == Long.MIN_VALUE ? Long.MAX_VALUE : Math.max(0L, now - at);
    }
}
