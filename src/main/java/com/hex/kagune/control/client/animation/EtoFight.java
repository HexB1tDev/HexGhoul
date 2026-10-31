package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.npc.EtoNpc;
import java.util.HashMap;
import java.util.Map;

/**
 * 1.3.4: Eto's fight on her body: her Owl DNA moves posed by the very animator a Pale player's are ({@link PaleAnimator}: the strikes,
 * the Big Punch's wind-up and blow, Witness This, the Speed Kick, the flips, each blended out of the pose before it and back into
 * her guard after), over whatever her gesture was. One animator for each Eto drawn, with her landings followed as a player's are.
 */
public final class EtoFight {
    private static final class Body {
        final PaleAnimator animator = new PaleAnimator();
        boolean airborne;
        float landed = -1000.0F;
        int seen;
    }

    private static final Map<Integer, Body> BODIES = new HashMap<>();

    private EtoFight() {
    }

    /**
     * Her pose this frame: her fight's move ({@code fight}, from the server) put on {@code under} (her gesture's pose, or null), as a
     * Pale player's would be; null when nothing poses her at all.
     */
    public static RigPose pose(EtoNpc npc, PaleClient.PaleView fight, RigPose under, double clock, float partial) {
        Body body = BODIES.computeIfAbsent(npc.m_19879_(), id -> new Body());
        float age = npc.f_19797_ + partial;
        boolean air = !npc.m_20096_();
        if (body.airborne && !air) {
            body.landed = age;
        }
        body.airborne = air;
        body.seen = npc.f_19797_;
        RigPose p = under == null ? new RigPose() : new RigPose().add(under, 1.0F);
        float bodyYaw = npc.f_20884_ + wrap(npc.f_20883_ - npc.f_20884_) * partial;
        // 1.3.19: Peace worn (Ken): its stance, its coming and going, its moves' gestures, as a player's (KaguneBodyAnimation.war); her
        // Owl DNA moves (his flips, his strikes) over it.
        peace(npc, p, partial, bodyYaw, air);
        // 1.3.8: the Owl growing on her, in it, or going: under its own moves, over anything else (as a player's, KaguneBodyAnimation).
        com.hex.kagune.control.client.OwlClient.Owl owl = com.hex.kagune.control.client.OwlClient.get(npc, clock);
        boolean owlMove = fight != null && (fight.move().owl() || body.animator.owlPosing(clock));
        if (owl != null && owlMove) {
            OwlPose.body(p, owl.on(), owl.age(clock), clock);
        }
        if (fight != null) {
            float turn = (float)Math.toRadians(wrap(fight.yaw() - bodyYaw));
            body.animator.apply(p, fight, clock, age, body.landed, turn);
        }
        if (owl != null && !owlMove) {
            OwlPose.body(p, owl.on(), owl.age(clock), clock);
        }
        if (owl != null) {
            float headYaw = npc.f_20886_ + wrap(npc.f_20885_ - npc.f_20886_) * partial;
            OwlPose.neck(p, Math.toRadians(wrap(headYaw - bodyYaw)), Math.toRadians(npc.m_146909_()), OwlPose.weight(owl.on(), owl.age(clock)));
        }
        // 1.3.7: her Mutated Arm's weight on her, whatever she is doing (as a player's, KaguneBodyAnimation).
        com.hex.kagune.control.client.ArmClient.Arm arm = com.hex.kagune.control.client.ArmClient.get(npc);
        if (arm != null) {
            double[] look = arm.aim(partial);
            double yaw = Math.toRadians(bodyYaw);
            double[] aim = {-look[0] * Math.cos(yaw) - look[2] * Math.sin(yaw), look[1], -look[0] * Math.sin(yaw) + look[2] * Math.cos(yaw)};
            ArmPose.body(p, arm.phase, arm.age(clock), arm.reach(clock), aim, arm.afterSphere(), clock);
        }
        body.animator.shown(p);
        return under == null && p.isZero() ? null : p;
    }

    /** 1.3.19: Peace on {@code npc}'s body, as on a player's (its run left to the legs' own walk). */
    private static void peace(EtoNpc npc, RigPose p, float partial, float bodyYaw, boolean air) {
        com.hex.kagune.control.client.KingClient.View worn = com.hex.kagune.control.client.KingClient.get(npc);
        if (worn == null || worn.form() != com.hex.kagune.control.king.KingForm.PEACE) {
            return;
        }
        double time = com.hex.kagune.control.client.ControlClient.worldTime(partial);
        double since = time - worn.since();
        if (com.hex.kagune.control.king.KingTransform.coming(worn.form(), worn.on(), since)) {
            double ticks = com.hex.kagune.control.king.KingTransform.ticks(worn.form());
            float stance = (float)Math.max(0.0, Math.min(1.0, (since - (ticks - 10.0)) / 10.0));
            WarPose.peace(p, time, stance);
            RigPose coming = new RigPose();
            WarPose.coming(coming, false, since, time);
            p.add(coming, 1.0F - stance * stance);
            return;
        }
        if (com.hex.kagune.control.king.KingTransform.going(worn.form(), worn.on(), since)) {
            WarPose.peace(p, time, (float)Math.max(0.0, 1.0 - since / 4.0));
            WarPose.going(p, false, since, time);
            return;
        }
        if (!worn.on()) {
            return;
        }
        float run = Math.min(1.0F, npc.f_267362_.m_267711_(partial) * 1.4F);
        WarPose.peace(p, time, 1.0F - 0.7F * run);
        com.hex.kagune.control.king.KingMove move = worn.moveAt(time);
        if (move != com.hex.kagune.control.king.KingMove.NONE) {
            RigPose blow = new RigPose();
            PeacePose.act(blow, move, time - worn.moveAt(), worn.level(), 1.0F);
            float headYaw = npc.f_20886_ + wrap(npc.f_20885_ - npc.f_20886_) * partial;
            blow.spin -= (float)Math.toRadians(wrap(headYaw - bodyYaw));
            p.add(blow.legs(air ? 1.0F : 0.0F, KaguneBodyAnimation.BLOW_TWIST), 1.0F);
        }
    }

    /** True while a move (or its guard after) has her body: her gestures wait. */
    public static boolean busy(EtoNpc npc, double clock) {
        Body body = BODIES.get(npc.m_19879_());
        return body != null && body.animator.busy(clock);
    }

    /** Forgets every Eto no longer about (her animator goes with her). */
    public static void prune(java.util.function.IntPredicate gone) {
        BODIES.keySet().removeIf(gone::test);
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
