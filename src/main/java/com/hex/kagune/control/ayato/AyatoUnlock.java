package com.hex.kagune.control.ayato;

import com.hex.kagune.control.AyatoStage;

/**
 * Ayato's abilities spread over his mastery (pure, tested): one row per step, in order, each naming what that mastery brings.
 * The numbers are the gates the game itself checks ({@link AyatoRules}, {@link AyatoStage}, {@link WingForm}), so the menu's
 * track and the "unlocks at mastery N" messages never disagree with what a key actually does. Mutation points (300, 500, 700,
 * 900) are {@link AyatoMutation}'s and shown in their own panel.
 */
public enum AyatoUnlock {
    START(0, "One wing: fire shots, Rabbit Hop, Flare"),
    TWIN_WINGS(AyatoStage.TWIN_WINGS.requiredMastery(), "Twin wings, Rising Flame"),
    WING_VORTEX(AyatoRules.VORTEX_MASTERY, "Wing Vortex (Z)"),
    CRYSTAL(AyatoRules.CRYSTAL_MASTERY, "Crystal mode (V): Lance, Spike Fan"),
    WING_BLADES(AyatoRules.WING_BLADES_MASTERY, "Wing blades, Fire Fan, Spike Rain"),
    RABBIT_DASH(AyatoRules.enderMastery(4), "Rabbit Dash, Spike Barrage"),
    GATLING(AyatoRules.GATLING_MASTERY, "Gatling (J, crystal mode)"),
    UKAKU_SPEED(450, "Ukaku speed II, hotter fire"),
    FULL_WINGS(AyatoStage.FULL_SIZE_MASTERY, "Full wings, Black Rabbit, Crystal Bloom"),
    KAKUJA(WingForm.KAKUJA.requiredMastery(), "First kakuja (K): Needles, Harrow, Line"),
    RUSHING_DEATH(AyatoRules.RUSH_MASTERY, "Rushing Death (Z), Thorn Fan"),
    NEEDLE_GATLING(AyatoRules.NEEDLE_GATLING_MASTERY, "Needle Gatling (J), Hail of Thorns"),
    SIX_TAIL(AyatoRules.enderMastery(4, true), "Six-Tail Barrage"),
    CRIMSON_BLOOM(AyatoRules.enderMastery(5, true), "Crimson Bloom"),
    FINAL_KAKUJA(AyatoStage.KAKUJA_II.requiredMastery(), "Final kakuja (hold K): needles + fire");

    private final int mastery;
    private final String text;

    AyatoUnlock(int mastery, String text) {
        this.mastery = mastery;
        this.text = text;
    }

    public int mastery() {
        return this.mastery;
    }

    public String text() {
        return this.text;
    }

    public boolean reached(int mastery) {
        return mastery >= this.mastery && !this.comingSoon();
    }

    /** True for a step that is not in the game yet (shown, never reached). */
    public boolean comingSoon() {
        return this == FINAL_KAKUJA && AyatoStage.KAKUJA_II.comingSoon();
    }

    /** The first step not reached yet at {@code mastery}, or null when every one is. */
    public static AyatoUnlock next(int mastery) {
        for (AyatoUnlock unlock : values()) {
            if (!unlock.reached(mastery)) {
                return unlock;
            }
        }
        return null;
    }

    /** The index of {@link #next} (the number of steps reached in order), {@code values().length} when all are. */
    public static int reachedCount(int mastery) {
        AyatoUnlock next = next(mastery);
        return next == null ? values().length : next.ordinal();
    }
}
