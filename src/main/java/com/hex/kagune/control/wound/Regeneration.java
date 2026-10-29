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

    private Regeneration() {
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
