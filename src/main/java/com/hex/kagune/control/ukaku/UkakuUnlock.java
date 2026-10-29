package com.hex.kagune.control.ukaku;

import com.hex.kagune.control.UkakuStage;

/**
 * Ukaku's abilities spread over his mastery (pure, tested): one row per step, in order, each naming what that mastery brings.
 * The numbers are the gates the game itself checks ({@link UkakuRules}, {@link UkakuStage}, {@link WingForm}), so the menu's
 * track and the "unlocks at mastery N" messages never disagree with what a key actually does. Mutation points (300, 500, 700,
 * 900) are {@link UkakuMutation}'s and shown in their own panel.
 */
public enum UkakuUnlock {
    START(0, "One wing: fire shots, Rabbit Hop, Flare"),
    TWIN_WINGS(UkakuStage.TWIN_WINGS.requiredMastery(), "Twin wings, Rising Flame"),
    WING_VORTEX(UkakuRules.VORTEX_MASTERY, "Wing Vortex (Z)"),
    CRYSTAL(UkakuRules.CRYSTAL_MASTERY, "Crystal mode (V): Lance, Spike Fan"),
    WING_BLADES(UkakuRules.WING_BLADES_MASTERY, "Wing blades, Fire Fan, Spike Rain"),
    RABBIT_DASH(UkakuRules.enderMastery(4), "Rabbit Dash, Spike Barrage"),
    GATLING(UkakuRules.GATLING_MASTERY, "Gatling (J, crystal mode)"),
    UKAKU_SPEED(450, "Ukaku speed II, hotter fire"),
    FULL_WINGS(UkakuStage.FULL_SIZE_MASTERY, "Full wings, Black Rabbit, Crystal Bloom"),
    KAKUJA(WingForm.KAKUJA.requiredMastery(), "First kakuja (K): Needles, Harrow, Line"),
    RUSHING_DEATH(UkakuRules.RUSH_MASTERY, "Rushing Death (Z), Thorn Fan"),
    NEEDLE_GATLING(UkakuRules.NEEDLE_GATLING_MASTERY, "Needle Gatling (J), Hail of Thorns"),
    SIX_TAIL(UkakuRules.enderMastery(4, true), "Six-Tail Barrage"),
    CRIMSON_BLOOM(UkakuRules.enderMastery(5, true), "Crimson Bloom"),
    FINAL_KAKUJA(UkakuStage.KAKUJA_II.requiredMastery(), "Final kakuja (hold K): needles + fire");

    private final int mastery;
    private final String text;

    UkakuUnlock(int mastery, String text) {
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
        return this == FINAL_KAKUJA && UkakuStage.KAKUJA_II.comingSoon();
    }

    /** The first step not reached yet at {@code mastery}, or null when every one is. */
    public static UkakuUnlock next(int mastery) {
        for (UkakuUnlock unlock : values()) {
            if (!unlock.reached(mastery)) {
                return unlock;
            }
        }
        return null;
    }

    /** The index of {@link #next} (the number of steps reached in order), {@code values().length} when all are. */
    public static int reachedCount(int mastery) {
        UkakuUnlock next = next(mastery);
        return next == null ? values().length : next.ordinal();
    }
}
