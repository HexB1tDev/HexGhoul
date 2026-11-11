package com.hex.kagune.control.client.animation;

/**
 * 4.1.15 (pure, tested; the owner: "better the animation"): one companion's pose carried from one gesture into the next. Each gesture's
 * pose eases in from nothing (standing straight), so a change of gesture (a kiss over and the blush after it, a wave cut short by a hug)
 * dropped the body back upright for a moment before the next began; this blends from the pose last drawn into the new one over
 * {@link #TICKS}, whatever the two are.
 */
public final class PoseFade {
    /** Ticks to go from the last gesture's pose into the next one's. */
    public static final float TICKS = 5.0F;

    private RigPose last;
    private long at = Long.MIN_VALUE;
    private RigPose from;
    private double since;

    /**
     * The pose to draw for {@code now} (null: none), the gesture's of {@code gestureAt} (when it began: a new value is a new gesture),
     * {@code clock} ticks.
     */
    public RigPose through(RigPose now, long gestureAt, double clock) {
        if (gestureAt != this.at) {
            this.from = this.last;
            this.since = clock;
            this.at = gestureAt;
        }
        RigPose out = now;
        if (this.from != null) {
            float k = EtoPose.ease((float)((clock - this.since) / TICKS));
            if (k >= 1.0F || !Double.isFinite(clock - this.since)) {
                this.from = null;
            } else {
                out = blend(this.from, now, k);
            }
        }
        this.last = out == null ? null : blend(out, null, 0.0F);
        return out;
    }

    /** {@code a} turned {@code k} of the way into {@code b} (null: standing straight), its held arms too. */
    static RigPose blend(RigPose a, RigPose b, float k) {
        RigPose out = new RigPose().add(a, 1.0F - k);
        if (b != null) {
            out.add(b, k);
        }
        out.rightArmHold = a.rightArmHold * (1.0F - k) + (b == null ? 0.0F : b.rightArmHold * k);
        return out;
    }
}
