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
        // 4.1.69 (the owner: "Ken's sitting animation is broken, one of his legs is just in the air."): into or out of sitting on the
        // floor, a straight blend took the hips down (or up) as fast as the legs swung, the feet through the floor halfway. The hips go no
        // lower than the legs let them (EtoPose.sitDrop, on the more upright leg: its foot is the lower), past what the other pose has
        // of its own; the same pose at both ends of the fade.
        float sat = Math.max(EtoPose.seated(a) ? 1.0F - k : 0.0F, b != null && EtoPose.seated(b) ? k : 0.0F);
        if (sat > 0.0F && sat < 1.0F) {
            RigPose other = EtoPose.seated(a) ? b : a;
            float forward = Math.max(0.0F, Math.min(EtoPose.SIT_LEVEL, Math.min(-out.rightLegX, -out.leftLegX)));
            out.drop = Math.min(out.drop, EtoPose.sitDrop(forward) + (other == null ? 0.0F : Math.max(0.0F, other.drop)) * (1.0F - sat));
        }
        return out;
    }
}
