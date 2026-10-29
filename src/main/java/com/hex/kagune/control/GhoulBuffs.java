package com.hex.kagune.control;

/**
 * What a ghoul keeps of the kagune's buffs with only the kakugan open (ghoul mode, R; pure, tested; 3.0.3): the movement buffs at
 * half their level (never below level I while the kagune would give any), and none of the defensive ones (the Centipede forms'
 * Resistance and Health Boost belong to the kagune's armor). Ayato's Speed and Jump Boost (with Agility Boost's on top) and
 * the Centipede forms' Speed carry over this way.
 *
 * <p>6.1.0: the open eyes are a form of their own. They give a small Resistance and a modest melee boost (Resistance I and
 * Strength I, refreshed like the kagune's form buffs and taken away the moment the eyes close or the kagune comes out), and they
 * burn RC slowly: the kagune's rest-time recovery stops and the eyes drain the reserve on top, less the higher the mastery (the
 * same efficiency the manifested kagune's upkeep gets). Run dry, the eyes close.
 */
public final class GhoulBuffs {
    /** Resistance amplifier while the eyes are open (0: Resistance I, a fifth off incoming damage). */
    public static final int EYES_RESISTANCE = 0;
    /** Strength amplifier while the eyes are open (0: Strength I). */
    public static final int EYES_STRENGTH = 0;
    /** RC the open eyes burn a tick at mastery 0 (0.6 a second: a full reserve lasts under three minutes). */
    public static final float EYES_DRAIN_PER_TICK = 0.03F;
    /** What the original gives back each tick while the kagune is in (its rest-time recovery), held back while the eyes are open. */
    public static final float KAGUNE_REST_RECOVERY = 0.16F;

    private GhoulBuffs() {
    }

    /**
     * The effect amplifier in ghoul mode for one the kagune gives at {@code kagune} (an amplifier: 0 is level I), or -1 for none
     * ({@code kagune} below 0).
     */
    public static int eyes(int kagune) {
        if (kagune < 0) {
            return -1;
        }
        int level = kagune + 1;
        return Math.max(1, level / 2) - 1;
    }

    /**
     * RC taken from the reserve each tick the eyes are open: the original's rest-time recovery cancelled, plus the eyes' own drain
     * scaled by {@code efficiency} (the original's {@code energyDrainEfficiency}, 1 at mastery 0 down to 0.42 at 1000).
     */
    public static float eyesCost(float efficiency) {
        float scale = Float.isFinite(efficiency) ? Math.max(0.0F, Math.min(1.0F, efficiency)) : 1.0F;
        return KAGUNE_REST_RECOVERY + EYES_DRAIN_PER_TICK * scale;
    }
}
