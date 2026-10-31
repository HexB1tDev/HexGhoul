package com.hex.kagune.control.npc;

import net.minecraft.world.phys.Vec3;

/**
 * 1.3.4: hand in hand with Eto (pure): where she is, glued at their right side, and how far out each of their hands hangs so they
 * meet. Their hand hangs at {@code 0.375 + 0.75 sin ARM} out from their middle (a wide arm's middle, its 12 pixels long), hers at
 * {@code (0.34 + 0.75 sin ARM) * SIZE} from hers (a slim one): {@link #OUT} apart, the two meet.
 */
public final class EtoHand {
    /** How far out from their sides both hands are turned (radians), and how far apart the two of them stand (blocks). */
    public static final float ARM = 0.1F;
    public static final double OUT = 0.375 + 0.75 * Math.sin(ARM) + (0.34 + 0.75 * Math.sin(ARM)) * EtoRules.SIZE;

    /** Where she stands for {@code x, y, z} facing {@code bodyYaw} (degrees): {@link #OUT} to their right. */
    public Vec3 spot(double x, double y, double z, float bodyYaw) {
        double yaw = Math.toRadians(bodyYaw);
        return new Vec3(x - Math.cos(yaw) * OUT, y, z - Math.sin(yaw) * OUT);
    }

    /** The same as offsets {x, z} (for a client gluing her there). */
    public static double[] offset(float bodyYaw) {
        double yaw = Math.toRadians(bodyYaw);
        return new double[] {-Math.cos(yaw) * OUT, -Math.sin(yaw) * OUT};
    }
}
