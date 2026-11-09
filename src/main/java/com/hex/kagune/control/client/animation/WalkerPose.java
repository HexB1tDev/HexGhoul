package com.hex.kagune.control.client.animation;

/**
 * 3.6.2 (the owner: "when the player walks his body doesn't move, but like he controls these tentacles that walk around"): The Walker's
 * body: his own legs hang still (none of the walk's swing), knees a little bent, swaying only slightly; he is lifted a little off the ground,
 * carried by the walking tentacles.
 */
public final class WalkerPose {
    /** How far he is lifted (model pixels). */
    static final float LIFT = 3.0F;

    private WalkerPose() {
    }

    /** Puts his legs hanging on {@code p} ({@code weight} of it: 1 all of it), {@code time} ticks into his clock. */
    public static void legs(RigPose p, double time, float weight) {
        if (weight <= 0.0F) {
            return;
        }
        float k = Math.min(1.0F, weight);
        p.rightLegX = lerp(p.rightLegX, (float)(-0.18 + 0.05 * Math.sin(time * 0.09)), k);
        p.leftLegX = lerp(p.leftLegX, (float)(0.08 + 0.05 * Math.sin(time * 0.09 + 2.0)), k);
        p.rightLegZ = lerp(p.rightLegZ, 0.06F, k);
        p.leftLegZ = lerp(p.leftLegZ, -0.06F, k);
        p.rightKnee = lerp(p.rightKnee, 0.35F, k);
        p.leftKnee = lerp(p.leftKnee, 0.25F, k);
        p.legHold = Math.max(p.legHold, k);
        p.drop -= LIFT * k;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
