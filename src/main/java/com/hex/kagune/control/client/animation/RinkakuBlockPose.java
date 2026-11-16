package com.hex.kagune.control.client.animation;

/**
 * 2.0.42 (pure; the owner: "You're going to assume a defensive position with your hands, like putting, covering your face or something,
 * like in a battle ready position"): Rinkaku's block on the body: both forearms up in front of his face, elbows in, chin down behind
 * them, knees bent and the weight a little forward, ready ({@link #guard}); a blow on the guard rocks him back a little, a perfect block
 * drives him into it, a broken guard throws his arms open and him back ({@link #apply}).
 */
public final class RinkakuBlockPose {
    private RinkakuBlockPose() {
    }

    /** The guard, whole. */
    public static RigPose guard() {
        RigPose p = new RigPose();
        p.headX = 0.16F;
        p.lean = 0.14F;
        p.drop = 0.6F;
        p.rightArmX = -1.2F;
        p.rightArmY = -0.62F;
        p.rightArmZ = 0.22F;
        p.rightElbow = 2.15F;
        p.leftArmX = -1.32F;
        p.leftArmY = 0.6F;
        p.leftArmZ = -0.2F;
        p.leftElbow = 2.05F;
        p.rightLegX = -0.34F;
        p.rightLegZ = 0.1F;
        p.rightKnee = 0.62F;
        p.leftLegX = -0.12F;
        p.leftLegZ = -0.1F;
        p.leftKnee = 0.5F;
        p.armHold = 1.0F;
        return p;
    }

    /**
     * {@code p} with the guard on it at {@code amount} (0..1), {@code jolt} the guard's press ({@code -} a blow pushing it back,
     * {@code +} a perfect block driving it out, blocks) and {@code thrown} a broken guard's throw (0..1).
     */
    public static void apply(RigPose p, float amount, float jolt, float thrown) {
        if (amount > 0.0F) {
            p.add(guard(), amount);
            // A blow rocks him back on his heels, a perfect block drives him into it.
            p.lean += 1.1F * jolt * amount;
            p.headX += 0.8F * jolt * amount;
            p.rightArmX -= 1.4F * jolt * amount;
            p.leftArmX -= 1.4F * jolt * amount;
        }
        if (thrown > 0.0F) {
            // Broken: the arms thrown open and up, the body rocked back.
            p.lean -= 0.3F * thrown;
            p.headX -= 0.32F * thrown;
            p.rightArmX -= 0.35F * thrown;
            p.leftArmX -= 0.35F * thrown;
            p.rightArmZ += 1.05F * thrown;
            p.leftArmZ -= 1.05F * thrown;
            p.rightElbow += 0.35F * thrown;
            p.leftElbow += 0.35F * thrown;
            p.rightLegX -= 0.25F * thrown;
            p.rightKnee += 0.3F * thrown;
            p.armHold += 0.8F * thrown;
        }
    }
}
