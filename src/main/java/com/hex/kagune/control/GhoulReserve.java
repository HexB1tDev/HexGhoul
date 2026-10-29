package com.hex.kagune.control;

/**
 * A ghoul's flesh reserve, which replaces the food bar (pure, tested; 3.0.3; 7.0.0: and RC, {@link #rc}). Only raw flesh fills it; every point of health a
 * ghoul regenerates is paid from it (and the kagune out sips at it, as it used to sip at hunger); human food the body rejects
 * costs some. Empty, a ghoul does not regenerate, and below 14 of 20 the hunger behind bloodlust and instinct grows as the food
 * bar's did. Vanilla hunger is held still under it, high enough to sprint and low enough never to heal or starve.
 */
public final class GhoulReserve {
    public static final float MAX = 20.0F;
    /** Reserve per point of a raw flesh's nutrition: raw beef (3) gives 4.5, enough for 9 health. */
    public static final float PER_NUTRITION = 1.5F;
    /** Reserve spent per point of health regenerated: healing back from one heart costs about half the reserve. */
    public static final float PER_HEALTH = 0.5F;
    /** What human food the body throws back up costs. */
    public static final float REJECTED = 2.0F;
    /** The vanilla food level and saturation a ghoul is held at (sprinting needs above 6; food heals from 18; starving at 0). */
    public static final int FOOD_HELD = 17;
    public static final float SATURATION_HELD = 5.0F;

    /**
     * 7.0.0: one bar. RC is no longer a store of its own: it is this reserve read on its scale (full is 100 RC), so abilities,
     * healing and hunger all drain the one bar, and only eating fills it.
     */
    public static final float RC_PER_RESERVE = 5.0F;
    /** 7.0.0: the share of an ability's RC price the shared bar pays: it no longer refills on its own, so it drains slower. */
    public static final float RC_SPEND = 0.5F;

    private GhoulReserve() {
    }

    /** The reserve as RC (0..100). */
    public static float rc(float reserve) {
        return clamp(reserve) * RC_PER_RESERVE;
    }

    /** RC (0..100) as the reserve. */
    public static float fromRc(float rc) {
        return clamp(rc / RC_PER_RESERVE);
    }

    public static float clamp(float reserve) {
        return Math.max(0.0F, Math.min(MAX, reserve));
    }

    public static float eat(float reserve, int nutrition) {
        return clamp(reserve + Math.max(1, nutrition) * PER_NUTRITION);
    }

    /** How much of {@code wanted} health the reserve can pay for. */
    public static float healable(float reserve, float wanted) {
        return Math.max(0.0F, Math.min(wanted, clamp(reserve) / PER_HEALTH));
    }

    public static float afterHeal(float reserve, float healed) {
        return clamp(reserve - Math.max(0.0F, healed) * PER_HEALTH);
    }

    public static float reject(float reserve) {
        return clamp(reserve - REJECTED);
    }

    /** The reserve after {@code exhaustion} (vanilla units: four is one food point). */
    public static float drain(float reserve, float exhaustion) {
        return clamp(reserve - Math.max(0.0F, exhaustion) / 4.0F);
    }

    /** Starvation (0 to 1) as the food bar's was: none from 14 up. */
    public static float hunger(float reserve) {
        return ControlMath.starvation(Math.round(clamp(reserve)));
    }
}
