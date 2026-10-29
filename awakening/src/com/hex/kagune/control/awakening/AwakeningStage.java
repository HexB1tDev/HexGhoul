package com.hex.kagune.control.awakening;

/** Persistent progress of the Kaneki path towards its final evolution. Ordinals are stored in NBT. */
public enum AwakeningStage {
    /** Below the mastery threshold. */
    DORMANT,
    /** The awakening quest is underway. */
    QUEST,
    /** The quest is complete: the next death awakens the Dragon instead of killing Kaneki. */
    ARMED,
    /** The Dragon is rampaging right now. */
    DRAGON,
    /** The Dragon has shattered and the final evolution is permanently unlocked. */
    FINAL;

    public static AwakeningStage of(int ordinal) {
        AwakeningStage[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DORMANT;
    }
}
