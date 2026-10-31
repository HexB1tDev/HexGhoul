package com.hex.kagune.control.npc;

import net.minecraft.world.phys.Vec3;

/**
 * 1.3.4: hand in hand with Eto (pure): where she is, glued at their right side, and how far out each of their hands hangs so they
 * meet. Their hand hangs at {@code 0.375 + 0.75 sin ARM} out from their middle (a wide arm's middle, its 12 pixels long), hers at
 * {@code (0.34 + 0.75 sin ARM) * SIZE} from hers (a slim one): {@link #OUT} apart, the two meet.
 */
public final class EtoHand {
    /**
     * How far out from their sides both hands are turned (radians), how far forward (radians), and how far apart the two of them stand
     * (blocks). 1.3.5: stretched out further (they stood too close), about 15 degrees each, a block apart.
     */
    public static final float ARM = 0.26F;
    public static final float FORWARD = 0.12F;
    public static final double OUT = 0.375 + 0.75 * Math.sin(ARM) + (0.34 + 0.75 * Math.sin(ARM)) * EtoRules.SIZE;

    /**
     * 1.3.9: as far apart as a woman's hand (the one female body's: a slim arm, her size) and hers hang out: a woman holding her hand stands
     * that much closer.
     */
    public static final double OUT_FEMALE = (0.34 + 0.75 * Math.sin(ARM)) * EtoRules.SIZE * 2.0;

    /** How far apart they stand: {@link #OUT}, or {@link #OUT_FEMALE} when the one whose hand she holds is a woman. */
    public static double out(boolean woman) {
        return woman ? OUT_FEMALE : OUT;
    }

    /** Where she stands for {@code x, y, z} facing {@code bodyYaw} (degrees): {@link #out} to their right. */
    public Vec3 spot(double x, double y, double z, float bodyYaw, boolean woman) {
        double yaw = Math.toRadians(bodyYaw);
        double out = out(woman);
        return new Vec3(x - Math.cos(yaw) * out, y, z - Math.sin(yaw) * out);
    }

    /** The same as offsets {x, z} (for a client gluing her there). */
    public static double[] offset(float bodyYaw, boolean woman) {
        double yaw = Math.toRadians(bodyYaw);
        double out = out(woman);
        return new double[] {-Math.cos(yaw) * out, -Math.sin(yaw) * out};
    }
}
