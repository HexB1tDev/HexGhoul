package com.hex.kagune.control.family;

/**
 * 1.3.10: what a pregnancy brings to her partner's ears, for any woman (pure; {@code name} hers): told privately as her time goes on, and
 * what the cutscene says at the birth. An NPC may say far more in her own words (Eto's are {@code EtoLines}).
 */
public final class FamilyLines {
    private FamilyLines() {
    }

    /** The private word to her partner at alert {@code index} ({@link Pregnancy#ALERTS}), after her name (the banner puts it first). */
    public static String alert(int index, int seed) {
        return switch (index) {
            case 0 -> pick(seed, "felt the baby kick. Go and feel it.", "has something to show you. Your hand. Here. Feel that?");
            case 1 -> pick(seed, "is tired today. The baby is heavy.", "is craving something strange. Bring her food.");
            default -> pick(seed, "'s time is close. Stay near.", "says it's soon. Very soon.");
        };
    }

    /** The cutscene's words after the birth: who was born, and to whom. */
    public static String born(String mother, boolean girl, String child) {
        return (girl ? "A girl. " : "A boy. ") + child + ".\n" + mother + " won't let go of " + (girl ? "her" : "him") + ".";
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
