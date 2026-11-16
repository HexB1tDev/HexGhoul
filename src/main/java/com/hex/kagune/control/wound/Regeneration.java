package com.hex.kagune.control.wound;

/**
 * A ghoul's regeneration (pure, tested; 3.0.0): after a while without being hurt their wounds knit and their health returns,
 * all of it, over a short stretch; any hit starts the wait again. Mastery shortens both: at mastery 0 the wounds start closing
 * after 15 seconds and are healed 5 seconds later; at 1000 they start after 5 and are healed within 7 seconds of the last hit.
 * Food no longer heals a ghoul at all (only this does).
 */
public final class Regeneration {
    /** The wait before the wounds start closing, at mastery 0 and 1000 (ticks). */
    public static final int WAIT_NOVICE = 300;
    public static final int WAIT_MASTER = 100;
    /** How long they take to close from there, to full health (ticks). */
    public static final int HEAL_NOVICE = 100;
    public static final int HEAL_MASTER = 40;
    /** The Regenerative Reservoir mutation shortens the wait by this share. */
    public static final double RESERVOIR = 0.3;

    /**
     * 4.2.2 (the owner: "regeneration is consuming way too much energy, and no longer is it automatic ... To regenerate, you need to press that
     * key ... You need to hold the key down"; "just slow down the regeneration in general. Even when holding the key down, make sure it's slower
     * than what it currently is"): no longer by itself after a wait. While the regeneration key is held, health comes back at a steady rate:
     * {@link #HELD_SPAN} points over the old heal's span ({@link #healTicks}: 5 seconds at mastery 0, 2 at 1000), so half a point a second at
     * mastery 0 and a point and a quarter at 1000. The old heal brought ALL that was missing back over that span (two hearts a second after a
     * creeper's blast): the held rate is never faster than it for any wound of {@link #HELD_SPAN} or more, at any mastery, and a full bar (20)
     * takes 40 seconds held (16 at 1000) where it took 20 (7) with the wait.
     */
    public static final float HELD_SPAN = 2.5F;

    /** 4.2.2: the Regenerative Reservoir mutation (it shortened the wait, which is gone): its holder regenerates this much faster. */
    public static final double RESERVOIR_SPEED = 1.3;
    /**
     * 4.2.2 (the owner: "I just need to be hit by a creeper and my fucking energy is gone"): what a point of health costs, a fifth of before: the
     * flesh reserve's {@code GhoulReserve.REGEN_PER_HEALTH} (was 0.5 of its 20), Graft DNA's RC for its faster healing the same share.
     */
    public static final float PRICE_SHARE = 0.2F;

    private Regeneration() {
    }

    /** 4.2.2: health back a tick while the key is held ({@code speed}: Graft DNA's faster healing with its kagune out, 1 for everyone else). */
    public static float heldPerTick(int mastery, boolean reservoir, double speed) {
        double rate = HELD_SPAN / (double)healTicks(mastery);
        return (float)(rate * (reservoir ? RESERVOIR_SPEED : 1.0) * Math.max(0.0, speed));
    }

    static double master(int mastery) {
        return Math.max(0.0, Math.min(1.0, mastery / 1000.0));
    }

    /** Ticks after the last hit before regeneration starts. */
    public static int waitTicks(int mastery, boolean reservoir) {
        double wait = WAIT_NOVICE + (WAIT_MASTER - WAIT_NOVICE) * master(mastery);
        return (int)Math.round(wait * (reservoir ? 1.0 - RESERVOIR : 1.0));
    }

    /** Ticks regeneration takes to bring the health missing when it started back to full. */
    public static int healTicks(int mastery) {
        return (int)Math.round(HEAL_NOVICE + (HEAL_MASTER - HEAL_NOVICE) * master(mastery));
    }

    /**
     * How much to heal this tick: {@code since} ticks after the last hit, {@code missingAtStart} health missing when regeneration
     * began; 0 while still waiting.
     */
    public static float healThisTick(long since, int mastery, boolean reservoir, float missingAtStart) {
        if (since < waitTicks(mastery, reservoir) || missingAtStart <= 0.0F) {
            return 0.0F;
        }
        return missingAtStart / healTicks(mastery);
    }

    /**
     * True if a heal of {@code amount} reaching a ghoul is the kind this replaces: food's natural regeneration (a point at a
     * time), not a potion of healing (several points), nor a regeneration effect, nor this.
     */
    public static boolean natural(float amount, boolean regenerationEffect, boolean ours) {
        return !ours && !regenerationEffect && amount <= 1.5001F;
    }
}
