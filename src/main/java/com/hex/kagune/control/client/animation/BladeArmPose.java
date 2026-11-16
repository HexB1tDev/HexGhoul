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
    // 4.1.47: held up and out (the great blade clear of the ground, its point ahead and to the side; it was the arm low before it).
    static final float GUARD_PITCH = -1.42F;
    static final float GUARD_YAW = 0.42F;
    static final float GUARD_TWIST = -0.35F;
    static final float GUARD_LEAN = 0.12F;
    // (4.1.47: the keys' spin, the whole body turned about its middle, is none in these cuts but kept for one that would.)

    private static QuinquePose.Key k(double t, double pitch, double yaw, double roll, double twist, double lean, double lunge, double drop) {
        return new QuinquePose.Key((float)t, (float)pitch, (float)yaw, (float)roll, (float)twist, (float)lean, (float)lunge, (float)drop, 0.0F);
    }

    private static QuinquePose.Key guard(double t) {
        return k(t, GUARD_PITCH, GUARD_YAW, 0, GUARD_TWIST, GUARD_LEAN, 0, 0);
    }

    private static QuinquePose.Key k(double t, double pitch, double yaw, double roll, double twist, double lean, double lunge, double drop,
                                     double spin) {
        return new QuinquePose.Key((float)t, (float)pitch, (float)yaw, (float)roll, (float)twist, (float)lean, (float)lunge, (float)drop, (float)spin);
    }

    /**
     * Its {@code step}th cut's keys (absolute: the arm's aim and its roll, the body), from the guard and back to it. 4.1.47 (the owner: "also
     * give it a proper moveset, slash attacks"): four slashes, the edge leading each (the blade's edge is on the back of the arm: the arm
     * rolled so it faces the way the blade goes).
     */
    static QuinquePose.Key[] keys(int step) {
        return switch (step) {
            // A chop down from over the right shoulder to low on the left: wound up high, the chest turned back with it; the edge (facing
            // forward with the arm up) leads down.
            case 0 -> new QuinquePose.Key[] {guard(0), k(0.34, -2.75, 0.8, 0, 0.5, -0.1, -0.6, 0), k(0.5, -1.65, 0.05, 0, -0.1, 0.12, 2.5, 0.5),
                k(0.68, -1.12, -0.85, 0, -0.55, 0.2, 2.5, 0.7), guard(1)};
            // A rising backhand from low on the left up across to high on the right, the arm rolled over so the edge faces up and leads.
            case 1 -> new QuinquePose.Key[] {guard(0), k(0.3, -1.2, -0.95, 2.9, -0.55, 0.15, 0.0, 0.5), k(0.5, -1.85, 0.25, 3.05, 0.05, 0.05, 2.0, 0.2),
                k(0.7, -2.65, 0.85, 3.05, 0.45, -0.1, 1.5, 0), guard(1)};
            // The sweep: drawn far round to the right, the chest wound with it, then cut level round in front to the far left, the arm
            // rolled a quarter so the edge leads across; low in the knees.
            case BladeArm.SWEEP -> new QuinquePose.Key[] {guard(0), k(0.3, -1.62, 1.55, 1.5, 0.65, 0.0, -0.4, 0.6),
                k(0.5, -1.62, 0.0, 1.5, -0.1, 0.12, 1.6, 0.9), k(0.72, -1.55, -1.45, 1.5, -0.75, 0.12, 1.6, 0.9), guard(1)};
            // The finisher: raised high behind the head, held a breath, then driven down in a lunging cleave, the body down behind it.
            default -> new QuinquePose.Key[] {guard(0), k(0.32, -3.3, 0.2, 0, 0.3, -0.2, -1.6, 0, 0), k(0.55, -3.15, 0.15, 0, 0.25, -0.16, -1.2, 0, 0),
                k(0.65, -1.78, -0.22, 0, -0.2, 0.34, 7.0, 1.0, 0), k(0.82, -1.66, -0.24, 0, -0.25, 0.36, 7.0, 1.0, 0), guard(1)};
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

    /** Its guard (still: the stance's own middle). */
    public static RigPose guard() {
        return swing(0, 0.0);
    }

    /**
     * 4.1.47 (the owner: "a proper ambient stand"): its stance between cuts, alive, {@code time} ticks in ({@code seed}: its own, so no two
     * breathe together): the guard, breathing (the chest rising, a slight sway), the great blade drifting about its line and now and then
     * turned over on the wrist and back (its edge flashing), the head a little down, watching; the free arm loose before it.
     */
    public static RigPose stand(double time, int seed) {
        return stand(time, seed, 1.0F);
    }

    /** As {@link #stand}, its feet set in it as far as it stands {@code still} (0 walking: its legs its walk's, 1 standing). */
    public static RigPose stand(double time, int seed, float still) {
        double phase = Math.floorMod(seed, 997) * 0.37;
        double breath = Math.sin(time * 2.0 * Math.PI / 64.0 + phase);
        double drift = Math.sin(time * 2.0 * Math.PI / 97.0 + phase * 1.7);
        double sway = Math.sin(time * 2.0 * Math.PI / 131.0 + phase * 0.6);
        // Every so often (about every eight seconds) the blade turned over on the wrist and back, over a second.
        double cycle = (time + phase * 40.0) % STAND_TURN_EVERY;
        double turn = cycle < STAND_TURN_TICKS ? Math.sin(cycle / STAND_TURN_TICKS * Math.PI) : 0.0;
        RigPose p = new RigPose();
        QuinqueGrip.aimRight(p, GUARD_PITCH + 0.06 * drift + 0.03 * breath, GUARD_YAW + 0.07 * sway, -1.15 * turn * turn * (3.0 - 2.0 * turn));
        p.rightArmHold = 1.0F;
        p.twist = (float)(GUARD_TWIST + 0.05 * sway);
        p.lean = (float)(GUARD_LEAN + 0.03 * breath);
        p.drop = (float)(0.35 + 0.2 * breath);
        float[] a = QuinqueGrip.angles(QuinqueGrip.aim(-0.85 + 0.05 * breath, 0.35 - 0.04 * sway, 0.0));
        p.leftArmX = a[0];
        p.leftArmY = a[1];
        p.leftArmZ = a[2];
        p.leftArmHold = 1.0F;
        p.headX = (float)(0.12 + 0.02 * breath);
        p.headHold = 0.5F;
        // Its feet set apart, the right one (the blade's side) forward, the left back and out, its weight down in them.
        float k = Math.max(0.0F, Math.min(1.0F, still));
        p.rightLegX = -0.32F * k;
        p.leftLegX = 0.3F * k;
        p.rightLegZ = 0.06F * k;
        p.leftLegZ = -0.14F * k;
        p.legHold = k;
        p.drop += 0.9F * k;
        return p;
    }

    /** 4.1.47: how often its stance turns the blade over on the wrist, and over how long (ticks). */
    static final double STAND_TURN_EVERY = 160.0;
    static final double STAND_TURN_TICKS = 22.0;

    /** Its {@code step}th cut {@code t} through (0..1 over BladeArm.ticks(step)). */
    public static RigPose swing(int step, double t) {
        float[] v = QuinquePose.curve(keys(Math.floorMod(step, BladeArm.STEPS)), t);
        RigPose p = new RigPose();
        QuinqueGrip.aimRight(p, v[0], v[1], v[2]);
        p.rightArmHold = 1.0F;
        p.spin = v[7];
        p.twist = v[3];
        p.lean = v[4];
        p.lunge = Math.min(7.5F, v[5]); // (the curve overshoots its keys a little)
        p.drop = v[6];
        leftGuard(p, 1.0F);
        // (the free arm flung back a little with the big blows, for balance)
        if (step == BladeArm.FINISHER || step == BladeArm.SWEEP) {
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
