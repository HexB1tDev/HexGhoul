package com.hex.kagune.control.family;

/**
 * 1.3.10: a couple's family as a talk sees it (pure): how far along she is ({@code carrying}: 0..1, -1 not carrying), whether they have
 * asked each other for a baby (the next night it comes), and how many children they have.
 */
public record FamilyView(double carrying, boolean wantsBaby, int children) {
    public static final FamilyView NONE = new FamilyView(-1.0, false, 0);

    public boolean pregnant() {
        return this.carrying >= 0.0;
    }

    public Pregnancy.Stage stage() {
        return Pregnancy.stage(Math.max(0.0, this.carrying));
    }
}
