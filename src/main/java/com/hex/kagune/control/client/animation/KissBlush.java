package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.MoodKiss;

/**
 * 4.1.16 (pure, tested; the owner: "Confirm that all females when kissing have a blush, sort of ... So every time [they] blush, it depends
 * on their mood"): one companion's blush, how strong this frame (0..1). A woman blushes through the whole of a kiss (leaning in for it,
 * held, pulled in, a peck, a stolen one, just after), and anyone whose way it is to blush at a gesture (Persona.blushes) does too. In a
 * kiss it is as deep as the kiss's mood has it ({@link MoodKiss#blush}); it stays a while once the kiss is over ({@link #LINGER}), then
 * fades; it comes up and goes down smoothly, never flickering off between the kiss's phases.
 */
public final class KissBlush {
    /** Ticks the colour stays once the kiss is over, before it fades. */
    public static final double LINGER = 100.0;
    /** Ticks to come up fully. */
    public static final double RISE = 8.0;
    /** Ticks to fade out fully. */
    public static final double FADE = 40.0;

    private float shown;
    private double last = Double.NaN;
    private double kissedAt = Double.NEGATIVE_INFINITY;
    private float strength;

    /** True if {@code gesture} is part of a kiss. */
    public static boolean kissing(EtoGesture gesture) {
        return gesture == EtoGesture.KISS || gesture == EtoGesture.BLUSH || gesture == EtoGesture.PULL || gesture == EtoGesture.PECK
            || gesture == EtoGesture.STEAL || gesture == EtoGesture.KISSED;
    }

    /**
     * The blush at {@code clock} for a {@code female} companion (or not) doing {@code gesture}, {@code blushes} if it is their way to blush
     * at it, in a kiss of {@code style} (the kiss's mood; sweet when none).
     */
    public float at(boolean female, boolean blushes, EtoGesture gesture, MoodKiss.Style style, double clock) {
        double dt = Double.isNaN(this.last) ? 0.0 : Math.max(0.0, Math.min(5.0, clock - this.last));
        this.last = clock;
        float target = 0.0F;
        if (gesture != null && kissing(gesture) && (female || blushes)) {
            if (gesture == EtoGesture.KISS || gesture == EtoGesture.BLUSH || gesture == EtoGesture.PULL) {
                this.strength = MoodKiss.blush(style); // the kiss's own mood (it is sent while the kiss lasts)
            } else if (clock - this.kissedAt > LINGER) {
                this.strength = MoodKiss.blush(MoodKiss.Style.SWEET); // a peck or a stolen kiss on its own
            }
            this.kissedAt = clock;
            target = this.strength;
        } else if (blushes) {
            target = 1.0F;
        } else if (clock - this.kissedAt <= LINGER) {
            target = this.strength;
        }
        if (target > this.shown) {
            this.shown = (float)Math.min(target, this.shown + dt / RISE);
        } else {
            this.shown = (float)Math.max(target, this.shown - dt / FADE);
        }
        return this.shown;
    }
}
