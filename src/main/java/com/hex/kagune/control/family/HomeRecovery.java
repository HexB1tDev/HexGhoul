package com.hex.kagune.control.family;

/** 3.0.9: recovery observations for one family NPC (sampled every ten ticks), independent of the game. */
public final class HomeRecovery {
    public enum Reason {NONE, WALLED, STALLED}

    private int walled;
    private double best = Double.MAX_VALUE;
    private long since = Long.MIN_VALUE;
    private long last = Long.MIN_VALUE;

    /**
     * A follower, carried child, held hand or fighter is busy: never recover it to a moving home or carry old wall observations
     * through that activity. Otherwise only sustained block trapping or the existing two-minute no-headway rule permits recovery.
     */
    public Reason sample(boolean insideBlocks, boolean away, boolean busy, double distance, long now) {
        if (busy || this.last != Long.MIN_VALUE && now < this.last) {
            this.walled = 0;
            this.best = Double.MAX_VALUE;
            this.since = Long.MIN_VALUE;
        }
        this.last = now;
        if (busy) {
            return Reason.NONE;
        }
        this.walled = insideBlocks ? this.walled + 1 : 0;
        if (this.walled >= HomeRules.WALLED) {
            return Reason.WALLED;
        }
        if (!away) {
            this.best = Double.MAX_VALUE;
            this.since = Long.MIN_VALUE;
            return Reason.NONE;
        }
        if (this.since == Long.MIN_VALUE || distance < this.best - 2.0) {
            this.best = distance;
            this.since = now;
            return Reason.NONE;
        }
        return HomeRules.noHeadway(this.best, distance, this.since, now) ? Reason.STALLED : Reason.NONE;
    }
}
