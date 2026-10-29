package com.hex.kagune.control;

/**
 * Short involuntary actions the kagune forces on an untrained, hungry or frenzied host.
 * Each lasts well under a second and can be resisted (crouch to restrain, or simply steer away).
 */
public enum UrgeType {
    NONE(0, false),
    /** Head and eyes snap toward the prey; the client gently pulls the view. */
    GLANCE(1, false),
    /** An aggressive half-step toward the prey. */
    STEP(2, true),
    /** A short lunge toward the prey. */
    LUNGE(3, true),
    /** After a brief tell, the kagune lashes at the prey on its own. */
    STRIKE(4, true);

    private final int severity;
    private final boolean restrainable;

    UrgeType(int severity, boolean restrainable) {
        this.severity = severity;
        this.restrainable = restrainable;
    }

    public int severity() {
        return this.severity;
    }

    /** Crouching during the urge can cancel it (and is rewarded with Restraint practice). */
    public boolean restrainable() {
        return this.restrainable;
    }

    public static UrgeType byOrdinal(int ordinal) {
        UrgeType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }
}
