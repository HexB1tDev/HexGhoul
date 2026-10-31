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
        if (fight != null) {
            float bodyYaw = npc.f_20884_ + wrap(npc.f_20883_ - npc.f_20884_) * partial;
            float turn = (float)Math.toRadians(wrap(fight.yaw() - bodyYaw));
            body.animator.apply(p, fight, clock, age, body.landed, turn);
        }
        body.animator.shown(p);
        return under == null && p.isZero() ? null : p;
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
