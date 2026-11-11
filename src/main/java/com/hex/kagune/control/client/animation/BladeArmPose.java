package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.BladeArm;

/**
 * 1.3.36: how a wild ghoul with an arm blade moves (pure): its guard (the bladed right side forward, the blade arm low before it, the
 * blade along the arm toward the ground ahead, the free arm up across the chest); its three cuts (a flat cut from the right across to the
 * left, a rising backhand, the lunging thrust of its point), on the same smooth curves as the quinques' ({@link QuinquePose#curve});
 * its kagune winding out (the right arm brought up across the chest, the left hand gripping its forearm as the plates spiral down it,
 * the head bowed to watch, then the blade flung out to the side and down into its guard); its sprint (pitched forward, the blade arm
 * swept back so the blade trails level behind it, the free arm pumping). Its arms are straight sticks (vanilla's model): the blade
 * along the forearm makes every arm swing a cut.
 */
public final class BladeArmPose {
    private BladeArmPose() {
    }

    /** The guard: the right arm's pitch, yaw and roll (QuinqueGrip.aim), the chest's turn (right shoulder forward) and lean. */
    static final float GUARD_PITCH = -1.12F;
    static final float GUARD_YAW = 0.22F;
    static final float GUARD_TWIST = -0.35F;
    static final float GUARD_LEAN = 0.12F;

    private static QuinquePose.Key k(double t, double pitch, double yaw, double roll, double twist, double lean, double lunge, double drop) {
        return new QuinquePose.Key((float)t, (float)pitch, (float)yaw, (float)roll, (float)twist, (float)lean, (float)lunge, (float)drop, 0.0F);
    }

    private static QuinquePose.Key guard(double t) {
        return k(t, GUARD_PITCH, GUARD_YAW, 0, GUARD_TWIST, GUARD_LEAN, 0, 0);
    }

    /** Its {@code step}th cut's keys (absolute: the arm's aim and the body), from the guard and back to it. */
    static QuinquePose.Key[] keys(int step) {
        return switch (step) {
            // Flat from the right across to the left: the arm out level to the right, swept round in front and on.
            case 0 -> new QuinquePose.Key[] {guard(0), k(0.3, -1.5, 1.35, 0, 0.3, 0.05, -0.5, 0), k(0.47, -1.52, 0.05, 0, -0.25, 0.15, 2.5, 0.3),
                k(0.64, -1.45, -1.0, 0, -0.6, 0.18, 2.5, 0.3), guard(1)};
            // A rising backhand: low on the left, up across to high on the right.
            case 1 -> new QuinquePose.Key[] {guard(0), k(0.3, -0.45, -0.95, 0, -0.5, 0.15, 0, 0.6), k(0.48, -1.6, 0.3, 0, 0.05, 0.08, 2.0, 0.2),
                k(0.66, -2.35, 0.75, 0, 0.35, -0.05, 1.5, 0), guard(1)};
            // The thrust: drawn back by the chest (the arm turned against it to stay on the line), then the hips and shoulder driven in
            // behind the point, a long lunge.
            default -> new QuinquePose.Key[] {guard(0), k(0.3, -1.45, -0.4, 0, 0.6, -0.05, -2.0, 0.8), k(0.52, -1.55, 0.55, 0, -0.6, 0.3, 7.5, 0.6),
                k(0.72, -1.55, 0.55, 0, -0.6, 0.32, 7.5, 0.6), guard(1)};
        };
    }

    /** The free arm up across the chest, guarding. */
    static void leftGuard(RigPose p, float k) {
        float[] a = QuinqueGrip.angles(QuinqueGrip.aim(-1.05 * k, 0.55 * k, 0.0));
        p.leftArmX = a[0];
        p.leftArmY = a[1];
        p.leftArmZ = a[2];
        p.leftArmHold = Math.max(p.leftArmHold, k);
    }

    /** Its guard. */
    public static RigPose guard() {
        return swing(0, 0.0);
    }

    /** Its {@code step}th cut {@code t} through (0..1 over BladeArm.ticks(step)). */
    public static RigPose swing(int step, double t) {
        float[] v = QuinquePose.curve(keys(Math.floorMod(step, BladeArm.STEPS)), t);
        RigPose p = new RigPose();
        QuinqueGrip.aimRight(p, v[0], v[1], v[2]);
        p.rightArmHold = 1.0F;
        p.twist = v[3];
        p.lean = v[4];
        p.lunge = Math.min(7.5F, v[5]); // (the curve overshoots its keys a little)
        p.drop = v[6];
        leftGuard(p, 1.0F);
        // (the free arm flung back a little with the big blows, for balance)
        if (step == BladeArm.FINISHER) {
            float back = QuinquePose.ease((t - 0.3) / 0.2) * (1.0F - QuinquePose.ease((t - 0.72) / 0.28));
            float[] a = QuinqueGrip.angles(QuinqueGrip.aim(-1.05 + 1.5 * back, 0.55 - 0.8 * back, 0.0));
            p.leftArmX = a[0];
            p.leftArmY = a[1];
            p.leftArmZ = a[2];
        }
        QuinquePose.step(p, com.hex.kagune.control.quinque.Quinque.KATANA, v[5]);
        return p;
    }

    /**
     * Its kagune winding out ({@code wrap} 0..1 over BladeArm.WRAP_TICKS): the right arm up across the chest, the left hand on its
     * forearm, the head bowed to it; as the blade forms, the arm flung out to the right and down into its guard.
     */
    public static RigPose wrap(double wrap) {
        double w = Math.max(0.0, Math.min(1.0, wrap));
        float up = QuinquePose.ease(w / 0.18);
        float fling = QuinquePose.ease((w - 0.78) / 0.12);
        float settle = QuinquePose.ease((w - 0.9) / 0.1);
        RigPose p = new RigPose();
        // Up across the chest, flung out to the right, then down into the guard.
        double pitch = -0.31 + (-1.35 + 0.31) * up;
        double yaw = -0.5 * up;
        pitch += (-1.45 - pitch) * fling;
        yaw += (1.1 - yaw) * fling;
        pitch += (GUARD_PITCH - pitch) * settle;
        yaw += (GUARD_YAW - yaw) * settle;
        // A tremor as the plates bite down the arm.
        double tremor = w > 0.18 && w < 0.78 ? 0.035 * Math.sin(w * 90.0) : 0.0;
        QuinqueGrip.aimRight(p, pitch + tremor, yaw, 0.0);
        p.rightArmHold = 1.0F;
        // The left hand on the right forearm while it winds, then over to its guard as the blade is flung out.
        QuinqueGrip.reachLeftTo(p, new double[] {-1.0, 6.5, -0.5}, 1.0F);
        float[] held = {p.leftArmX, p.leftArmY, p.leftArmZ};
        float[] guard = QuinqueGrip.angles(QuinqueGrip.aim(-1.05, 0.55, 0.0));
        float k = Math.max(up, fling);
        p.leftArmX = (held[0] + (guard[0] - held[0]) * fling) * k;
        p.leftArmY = (held[1] + (guard[1] - held[1]) * fling) * k;
        p.leftArmZ = (held[2] + (guard[2] - held[2]) * fling) * k;
        p.leftArmHold = k;
        p.headX = 0.38F * up * (1.0F - fling);
        p.headY = -0.3F * up * (1.0F - fling);
        p.headHold = 0.6F * up * (1.0F - fling);
        p.drop = 0.8F * up * (1.0F - settle);
        p.lean = 0.15F * up + (GUARD_LEAN - 0.15F) * settle;
        p.twist = GUARD_TWIST * settle + 0.2F * fling * (1.0F - settle);
        return p;
    }

    /** Its sprint ({@code amount} 0..1 of it, {@code phase} its stride): the investigators' run, the blade arm swept back to trail it. */
    public static RigPose run(double phase, float amount) {
        RigPose p = AgentPose.run(phase, amount, AgentPose.Carry.CASE);
        float k = Math.max(0.0F, Math.min(1.0F, amount));
        QuinqueGrip.aimRight(p, 1.15 * k, 0.3 * k, 0.0);
        p.rightArmHold = Math.max(p.rightArmHold, k);
        p.lean += 0.15F * k;
        return p;
    }
}
