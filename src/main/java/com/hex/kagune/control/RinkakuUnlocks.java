package com.hex.kagune.control;

/**
 * 6.0.4: the mastery each of Rinkaku's tendril moves unlocks at (pure, tested), as Ukaku's moves unlock one by one. The seed
 * starts with its M1 whips and the guard; then the grapple, Rapid Strike, the grab and throw with the twin tails, crushing,
 * lifting and slamming what they hold with the four. The kakuja (750 and 1000) have everything.
 */
public final class RinkakuUnlocks {
    /** J: the grapple leap. */
    public static final int GRAPPLE = 50;
    /** Z: Rapid Strike (Ukaku's Z, the Wing Vortex, comes at 150 too). */
    public static final int RAPID_STRIKE = 150;
    /** G and N: grabbing with a tail and throwing what it holds (the Twin Rinkaku's mastery). */
    public static final int GRAB = 250;
    /** H: crushing what a tail holds. */
    public static final int CRUSH = 350;
    /** V: lifting what a tail holds. */
    public static final int LIFT = 450;
    /** B: slamming what a tail holds (the Four-Tail's mastery). */
    public static final int SLAM = 500;

    private RinkakuUnlocks() {
    }

    /** True once {@code mastery} reaches {@code needs}. */
    public static boolean unlocked(int mastery, int needs) {
        return mastery >= needs;
    }
}
