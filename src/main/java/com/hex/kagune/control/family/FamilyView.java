package com.hex.kagune.control.family;

/**
 * 1.3.10: a couple's family as a talk sees it (pure): how far along she is ({@code carrying}: 0..1, -1 not carrying), whether they have
 * asked each other for a baby (the next night it comes), and how many children they have. 2.5.7: and how long since the last of them was
 * born ({@code sinceBorn}, ticks; {@link Long#MAX_VALUE} none yet): another is only asked for {@link #BABY_WAIT} after it.
 */
public record FamilyView(double carrying, boolean wantsBaby, int children, long sinceBorn) {
    public static final FamilyView NONE = new FamilyView(-1.0, false, 0, Long.MAX_VALUE);
    /** 2.5.7 (the owner: "perhaps a cooldown of 5 mc days"): how long after a child is born before another is asked for (ticks). */
    public static final long BABY_WAIT = 5L * 24000L;

    public FamilyView(double carrying, boolean wantsBaby, int children) {
        this(carrying, wantsBaby, children, Long.MAX_VALUE);
    }

    public boolean pregnant() {
        return this.carrying >= 0.0;
    }

    public Pregnancy.Stage stage() {
        return Pregnancy.stage(Math.max(0.0, this.carrying));
    }

    /** 2.5.7: true while their last child is too new for another to be asked for. */
    public boolean waiting() {
        return this.sinceBorn >= 0L && this.sinceBorn < BABY_WAIT;
    }

    /** 2.5.7: the days still to wait (at least one while {@link #waiting}, else 0). */
    public int daysToWait() {
        return this.waiting() ? (int)Math.max(1L, (BABY_WAIT - this.sinceBorn + 23999L) / 24000L) : 0;
    }
}
