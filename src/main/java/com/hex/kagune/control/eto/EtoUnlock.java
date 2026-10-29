package com.hex.kagune.control.eto;

/**
 * Eto's track (6.1.0; pure, tested): one row per step of her base form, in mastery order, each naming what that mastery brings,
 * and her kakuja, the Owl. The numbers are the gates the game checks ({@link EtoRules}), so the menu, the abilities board and the
 * "unlocks at mastery N" messages always agree with what a key does. 6.2.0: the Owl is her only kakuja (the sealed V2 is gone).
 */
public enum EtoUnlock {
    BASE_FORM(0, "Base form (Alt+R): one-eyed kakugan, rising steam"),
    BLITZ(0, "Speed Blitz (M1): jab, cross, hook, liver shot"),
    FLIPS(0, "Flips (right click + W A S D)"),
    BIG_PUNCH(0, "Big Punch! (hold Z, release)"),
    BLITZ_BLINK(EtoRules.BLITZ_BLINK_MASTERY, "Blitz: uppercut, blinking palm"),
    FLESH_SWIPE(EtoRules.SWIPE_MASTERY, "Flesh Swipe (tap J)"),
    SPEED_KICK(EtoRules.KICK_MASTERY, "Speed Kick (tap V)"),
    BLITZ_FINISH(EtoRules.BLITZ_FINISH_MASTERY, "Blitz: spinning backfist, twin palms"),
    FLESH_GRAB(EtoRules.GRAB_MASTERY, "Flesh Grab and Throw (hold J)"),
    MUTATION_POINT(EtoMutation.MILESTONES[0], "A mutation point (spend it in this menu)"),
    FLESH_HAMMER(EtoRules.HAMMER_MASTERY, "Flesh Hammer (hold V)"),
    WATCH_THIS(EtoRules.WATCH_MASTERY, "Watch This (B): two impaling tentacles"),
    MUTATED_ARM(ArmRules.MASTERY, "Mutated Arm (N): grab and drag; hold to crush in a sphere"),
    KAKUJA_V1(EtoRules.KAKUJA_V1_MASTERY, "The Owl, her kakuja (K): giant blades"),
    MUTATION_POINT_2(EtoMutation.MILESTONES[1], "A second mutation point (Up We Go)"),
    MUTATION_POINT_3(EtoMutation.MILESTONES[2], "A third mutation point (True Anonymity)"),
    MUTATION_POINT_4(EtoMutation.MILESTONES[3], "A fourth mutation point (Witness This)");

    private final int mastery;
    private final String text;

    EtoUnlock(int mastery, String text) {
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
        return mastery >= this.mastery;
    }

    /** The first step not reached yet at {@code mastery}, or null when every one is. */
    public static EtoUnlock next(int mastery) {
        for (EtoUnlock unlock : values()) {
            if (!unlock.reached(mastery)) {
                return unlock;
            }
        }
        return null;
    }

    /**
     * Her base form's stage names as it grows (the path card's strip shows the same, with the kakuja after them): 6.2.0, Owl DNA's
     * own, an experiment's, never a character's.
     */
    public static String stageName(int mastery) {
        return mastery >= 500 ? "One-Eyed Owl" : mastery >= 250 ? "Owl Strain" : "Test Subject";
    }

    /** True once {@code mastery} reaches {@code needs} (a gate any key can check). */
    public static boolean unlocked(int mastery, int needs) {
        return mastery >= needs;
    }
}
