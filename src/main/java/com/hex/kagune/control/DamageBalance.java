package com.hex.kagune.control;

/**
 * The kagune's damage, brought down (pure, tested; 3.0.3). Both paths had climbed far past anything else in the game: at
 * mastery 1000 a Full Kakuja swing hit for about 29 (a player has 20 health) and Ayato's first kakuja poured out about 69
 * damage a second from needles alone. Every kagune hit is now scaled by {@link #scale}: three quarters of what it was for a new
 * ghoul, down to a little over half at mastery 1000, where the form, stage and mastery multipliers had stacked up. Ayato's M1
 * shots, which fire several projectiles a beat, are trimmed further in their combo stages. At 1000 a sustained M1 comes to
 * about 18 to 24 damage a second (a Sharpness V netherite sword is about 18); the enders stay bursty.
 */
public final class DamageBalance {
    /** The scale for a new ghoul and at mastery 1000. */
    public static final float NEW = 0.75F;
    public static final float MASTERED = 0.55F;

    private DamageBalance() {
    }

    /** What every kagune hit is multiplied by at {@code mastery}. */
    public static float scale(int mastery) {
        float t = Math.max(0, Math.min(1000, mastery)) / 1000.0F;
        return NEW + (MASTERED - NEW) * t;
    }
}
