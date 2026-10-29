package com.hex.kagune.control.ayato;

/**
 * K, timed on either path (pure, tested; 3.0.0): a tap is the first kakuja, holding it the final kakuja. Fed once a tick with whether
 * K was pressed since the last tick and whether it is down now, it says what to send, once per press. Before the final kakuja
 * is unlocked there is nothing to wait for, so a press is the first kakuja straight away.
 */
public final class KakujaKey {
    /** Nothing to send this tick. */
    public static final int NONE = 0;
    /** A tap: into the first kakuja, or out of either. */
    public static final int TAP = 1;
    /** A hold: into the final kakuja, or out of it. */
    public static final int HOLD = 2;
    /** Held this many ticks (0.4 s) it is a hold; let go sooner, a tap. */
    public static final int HOLD_TICKS = 8;

    /** Ticks K has been down since the press being timed, or -1 when none is. */
    private int held = -1;
    private boolean sent;

    /** One tick: {@code pressed} since the last, {@code down} now, and whether the final kakuja is unlocked. */
    public int tick(boolean pressed, boolean down, boolean finalUnlocked) {
        if (pressed && this.held < 0) {
            if (!finalUnlocked) {
                return TAP;
            }
            this.held = 0;
            this.sent = false;
        }
        if (this.held < 0) {
            return NONE;
        }
        if (down) {
            if (++this.held >= HOLD_TICKS && !this.sent) {
                this.sent = true;
                return HOLD;
            }
            return NONE;
        }
        boolean tap = !this.sent;
        this.held = -1;
        return tap ? TAP : NONE;
    }

    /** Forget a press being timed (no player, a screen opened). */
    public void reset() {
        this.held = -1;
        this.sent = false;
    }
}
