package com.hex.kagune.control;

/**
 * Ukaku path transformation by mastery (shared with the Rinkaku path, so the pacing lines up with the original forms):
 * one small wing at first, two small wings from {@link #TWIN_WINGS}, growing steadily until they reach full size (and
 * the full :re shape) at {@link #FULL_WINGS}. Two kakuja forms follow: the first (a tap of K transforms into it) at
 * {@link #KAKUJA_I}, the final one (K held) at {@link #KAKUJA_II}.
 */
public enum UkakuStage {
    /** A new Ukaku: one small wing (the left, Touka's side). */
    ONE_WING("One Wing", "One", 0, false),
    /** Two small wings, which keep growing from here. */
    TWIN_WINGS("Twin Wings", "Twin", 100, false),
    /** The wings at full size, in the full :re shape. */
    FULL_WINGS("Full Wings", "Full", 500, false),
    /** First kakuja form: six armored tentacles (a tap of K transforms into it). */
    KAKUJA_I("Kakuja I", "Kakuja I", 750, false),
    /** The final kakuja: three needle tentacles, three fire tendrils and the half mask (K held). */
    KAKUJA_II("Kakuja II", "Kakuja II", 1000, false);

    /** Mastery at which the wings stop growing. */
    public static final int FULL_SIZE_MASTERY = 500;

    private final String displayName;
    private final String shortName;
    private final int requiredMastery;
    private final boolean comingSoon;

    UkakuStage(String displayName, String shortName, int requiredMastery, boolean comingSoon) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.requiredMastery = requiredMastery;
        this.comingSoon = comingSoon;
    }

    public String displayName() {
        return this.displayName;
    }

    public String shortName() {
        return this.shortName;
    }

    public int requiredMastery() {
        return this.requiredMastery;
    }

    /** Not built yet: shown in the menus as "coming soon", never reached in play. */
    public boolean comingSoon() {
        return this.comingSoon;
    }

    /** True from full size on: the wings take the full :re shape. */
    public boolean reEra() {
        return this.compareTo(FULL_WINGS) >= 0;
    }

    /** The highest built stage reached at {@code mastery} (coming-soon stages are never reached). */
    public static UkakuStage forMastery(int mastery) {
        UkakuStage reached = ONE_WING;
        for (UkakuStage stage : values()) {
            if (!stage.comingSoon && mastery >= stage.requiredMastery) {
                reached = stage;
            }
        }
        return reached;
    }

    /** How grown the wings are (0 at the twin wings' start .. 1 at full size). */
    public static float growth(int mastery) {
        float t = (mastery - TWIN_WINGS.requiredMastery) / (float)(FULL_SIZE_MASTERY - TWIN_WINGS.requiredMastery);
        return Math.max(0.0F, Math.min(1.0F, t));
    }
}
