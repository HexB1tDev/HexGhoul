package com.hex.kagune.control.client.animation;

/**
 * 3.6.2 (the owner: "when the player walks his body doesn't move, but like he controls these tentacles that walk around"): The Walker's
 * body: his own legs hang still (none of the walk's swing), knees a little bent, swaying only slightly. (3.7.1: he is carried off the
 * ground for real, {@code WalkerLegs.HEIGHT} up: the model is no longer lifted.) 3.7.0 (the owner: "the arms should not swing at any point (they currently do)"): his arms hang still
 * too, none of Minecraft's own swing (walking, attacking, the bob), only the slight sway the legs have.
 */
public final class WalkerPose {
    /** 3.7.0: how far his arms hang out from his sides, and how bent his elbows are (radians). */
    static final float ARM_OUT = 0.1F;
    static final float ELBOW = 0.15F;

    private WalkerPose() {
    }

    /** Puts his legs hanging on {@code p} ({@code weight} of it: 1 all of it), {@code time} ticks into his clock. */
    public static void legs(RigPose p, double time, float weight) {
        legs(p, time, weight, null);
    }

    /**
     * As {@link #legs(RigPose, double, float)}; 4.0.1 (the owner: "feel like more of a wobble"): his body riding the steps
     * ({@code body}: {@code WalkerLegs.body}'s roll and drop, null for none): swaying over the foot he stands on, dipping as the other goes.
     */
    public static void legs(RigPose p, double time, float weight, double[] body) {
        if (weight <= 0.0F) {
            return;
        }
        float k = Math.min(1.0F, weight);
        if (body != null && body.length >= 2) {
            p.roll += (float)body[0] * k;
            p.drop += (float)body[1] * k;
        }
        p.rightLegX = lerp(p.rightLegX, (float)(-0.18 + 0.05 * Math.sin(time * 0.09)), k);
        p.leftLegX = lerp(p.leftLegX, (float)(0.08 + 0.05 * Math.sin(time * 0.09 + 2.0)), k);
        p.rightLegZ = lerp(p.rightLegZ, 0.06F, k);
        p.leftLegZ = lerp(p.leftLegZ, -0.06F, k);
        p.rightKnee = lerp(p.rightKnee, 0.35F, k);
        p.leftKnee = lerp(p.leftKnee, 0.25F, k);
        p.legHold = Math.max(p.legHold, k);
        // 3.7.0: the arms hang at his sides, a little out, elbows a little bent, swaying no more than the legs do.
        p.rightArmX = lerp(p.rightArmX, (float)(0.04 * Math.sin(time * 0.09 + 1.0)), k);
        p.leftArmX = lerp(p.leftArmX, (float)(0.04 * Math.sin(time * 0.09 + 3.0)), k);
        p.rightArmY = lerp(p.rightArmY, 0.0F, k);
        p.leftArmY = lerp(p.leftArmY, 0.0F, k);
        p.rightArmZ = lerp(p.rightArmZ, ARM_OUT, k);
        p.leftArmZ = lerp(p.leftArmZ, -ARM_OUT, k);
        p.rightElbow = lerp(p.rightElbow, ELBOW, k);
        p.leftElbow = lerp(p.leftElbow, ELBOW, k);
        p.armHold = Math.max(p.armHold, k);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
