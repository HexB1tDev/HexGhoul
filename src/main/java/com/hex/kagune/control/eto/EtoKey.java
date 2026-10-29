package com.hex.kagune.control.eto;

/**
 * One of Eto's keys timed on the client (6.1.0, pure, tested): what to tell the server each tick. A key either acts on its tap
 * and its hold differently (J: swipe or grab; V: kick or hammer; a hold is {@link EtoRules#HOLD_TICKS} down), or is held from
 * the moment it goes down (Z: the punch charges at once, a quick tap is a punch at the least charge). While held the server is
 * reminded every {@link EtoRules#HOLD_RESEND_TICKS} (it lets a hold go it hasn't heard of in a while); let go, it is told once.
 */
public final class EtoKey {
    public static final int NONE = 0;
    /** Tapped: the tap's action. */
    public static final int TAP = 1;
    /** Held long enough (or at once): the hold starts. */
    public static final int HOLD = 2;
    /** Still held: the reminder. */
    public static final int KEEP = 3;
    /** Let go after a hold. */
    public static final int LET_GO = 4;

    private final boolean holdAtOnce;
    /** Ticks it has been down (-1: up). */
    private int down = -1;
    private int resend;

    public EtoKey(boolean holdAtOnce) {
        this.holdAtOnce = holdAtOnce;
    }

    public void reset() {
        this.down = -1;
    }

    /** One client tick: {@code pressed} if a press came in since the last one, {@code isDown} if it is down now. */
    public int tick(boolean pressed, boolean isDown) {
        if (this.down < 0) {
            if (!pressed && !isDown) {
                return NONE;
            }
            this.down = 0;
            if (this.holdAtOnce) {
                this.resend = EtoRules.HOLD_RESEND_TICKS;
                return HOLD; // let go within the tick: the next one says so
            }
            if (!isDown) {
                this.down = -1;
                return TAP;
            }
            return NONE;
        }
        if (!isDown) {
            boolean held = this.holdAtOnce || this.down >= EtoRules.HOLD_TICKS;
            this.down = -1;
            return held ? LET_GO : TAP;
        }
        this.down++;
        if (!this.holdAtOnce && this.down == EtoRules.HOLD_TICKS) {
            this.resend = EtoRules.HOLD_RESEND_TICKS;
            return HOLD;
        }
        if ((this.holdAtOnce || this.down > EtoRules.HOLD_TICKS) && --this.resend <= 0) {
            this.resend = EtoRules.HOLD_RESEND_TICKS;
            return KEEP;
        }
        return NONE;
    }
}
