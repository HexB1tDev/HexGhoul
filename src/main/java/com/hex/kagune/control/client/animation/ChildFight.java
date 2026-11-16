package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.client.PaleClient;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.3.56: Eto's child's fight on its body (the last handoff: "the child's body doesn't animate: it stands still while it rakes or
 * hammers"): her flesh's rake, grab, throw and club posed by the very animator hers and a Pale player's are ({@link PaleAnimator}: each
 * move blended out of the pose before it and back into its guard after), and her Mutated Arm's weight on it when it is out
 * ({@link ArmPose}), as {@link EtoFight} does for her; never the Owl (a child has no kakuja). One animator for each child drawn, its
 * landings followed; forgotten once it has not been drawn for a while.
 */
public final class ChildFight {
    private static final class Body {
        final PaleAnimator animator = new PaleAnimator();
        boolean airborne;
        float landed = -1000.0F;
        long seen;
    }

    /** Ticks a child may go undrawn before its animator is let go. */
    static final int FORGET = 200;

    private static final Map<Integer, Body> BODIES = new HashMap<>();

    private ChildFight() {
    }

    /**
     * Its pose this frame from its fight (its move from the server under its UUID, its arm), in a player's rig (the child's model is a
     * player's, scaled); null when nothing of its fight poses it.
     */
    public static RigPose pose(LivingEntity child, double clock, float partial) {
        PaleClient.PaleView fight = PaleClient.get(child.m_20148_());
        com.hex.kagune.control.client.ArmClient.Arm arm = com.hex.kagune.control.client.ArmClient.get(child);
        Body body = BODIES.get(child.m_19879_());
        boolean moving = fight != null && fight.move() != com.hex.kagune.control.pale.PaleMove.NONE;
        if (!moving && arm == null && (body == null || !body.animator.busy(clock))) {
            return null;
        }
        if (body == null) {
            body = new Body();
            forget(child.m_9236_().m_46467_());
            BODIES.put(child.m_19879_(), body);
        }
        float age = child.f_19797_ + partial;
        boolean air = !child.m_20096_();
        if (body.airborne && !air) {
            body.landed = age;
        }
        body.airborne = air;
        body.seen = child.m_9236_().m_46467_();
        RigPose p = new RigPose();
        float bodyYaw = child.f_20884_ + wrap(child.f_20883_ - child.f_20884_) * partial;
        if (fight != null) {
            body.animator.apply(p, fight, clock, age, body.landed, (float)Math.toRadians(wrap(fight.yaw() - bodyYaw)));
        }
        if (arm != null) {
            double[] look = arm.aim(partial);
            double yaw = Math.toRadians(bodyYaw);
            double[] aim = {-look[0] * Math.cos(yaw) - look[2] * Math.sin(yaw), look[1], -look[0] * Math.sin(yaw) + look[2] * Math.cos(yaw)};
            ArmPose.body(p, arm.phase, arm.age(clock), arm.reach(clock), aim, arm.afterSphere(), clock);
        }
        body.animator.shown(p);
        return p.isZero() ? null : p;
    }

    /** Children not drawn for {@link #FORGET} ticks let go (their animators with them). */
    private static void forget(long now) {
        BODIES.values().removeIf(body -> Math.abs(now - body.seen) > FORGET);
    }

    private static float wrap(float degrees) {
        float d = degrees % 360.0F;
        if (d >= 180.0F) {
            d -= 360.0F;
        }
        if (d < -180.0F) {
            d += 360.0F;
        }
        return d;
    }
}
