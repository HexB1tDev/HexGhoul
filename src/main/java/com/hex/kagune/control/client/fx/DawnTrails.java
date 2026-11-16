package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.quinque.DawnArc;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 2.0.31: the dark ribbons Dawn leaves as it is swung (the owner's picture: the lance "swung with dark ribbons around it"): as a swing
 * passes through its fast part, dark smoke is laid all along the way its point went ({@link DawnArc}), each puff edged with a faint dark
 * red glow, gone in about a third of a second. Fed every frame by whoever draws the swing (an investigator's pose, a player's); nothing
 * is sent.
 */
public final class DawnTrails {
    /** A puff every so far along the way (blocks), and none this close to the viewer's eye (first person). */
    static final double STEP = 0.3;
    static final double CLEARANCE = 1.0;
    private static final int SMOKE = 0xD8120C0F;
    private static final int SMOKE_FADE = 0x00200609;
    private static final int EDGE = 0x8C7A1220;
    private static final int EDGE_FADE = 0x00300008;
    /** Each body's swing as last laid: {its step, when it began, how far along its ribbon is laid}. */
    private static final Map<Integer, double[]> LAID = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private DawnTrails() {
    }

    /** {@code body}'s {@code step}th swing (begun at {@code began} on its own clock) is {@code t} through (0..1) this frame. */
    public static void swing(LivingEntity body, int step, double began, double t, float partial) {
        try {
            lay(body, step, began, t, partial);
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "Dawn's ribbons");
        }
    }

    private static void lay(LivingEntity body, int step, double began, double t, float partial) {
        double u = DawnArc.along(t);
        if (LAID.size() > 256) {
            LAID.clear();
        }
        double[] laid = LAID.get(body.m_19879_());
        if (laid == null || laid[0] != step || laid[1] != began) {
            laid = new double[] {step, began, 0.0};
            LAID.put(body.m_19879_(), laid);
        }
        double from = laid[2];
        if (u <= from + 1.0E-4) {
            return;
        }
        laid[2] = u;
        double feetX = body.f_19790_ + (body.m_20185_() - body.f_19790_) * partial;
        double feetY = body.f_19791_ + (body.m_20186_() - body.f_19791_) * partial;
        double feetZ = body.f_19792_ + (body.m_20189_() - body.f_19792_) * partial;
        Vec3 feet = new Vec3(feetX, feetY, feetZ);
        if (!RcFx.near(feet, 48.0)) {
            return;
        }
        float delta = ((body.f_20883_ - body.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
        double yaw = Math.toRadians(body.f_20884_ + delta * partial);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.m_91087_();
        Vec3 eye = minecraft.f_91063_.m_109153_().m_90583_();
        double[] last = DawnArc.point(step, from);
        double travelled = 0.0;
        int puffs = 0;
        for (double k = from; k < u && puffs < 24; ) {
            k = Math.min(u, k + 0.02);
            double[] p = DawnArc.point(step, k);
            travelled += Math.sqrt((p[0] - last[0]) * (p[0] - last[0]) + (p[1] - last[1]) * (p[1] - last[1]) + (p[2] - last[2]) * (p[2] - last[2]));
            last = p;
            if (travelled < STEP / Math.max(0.25, RcFx.density())) {
                continue;
            }
            travelled = 0.0;
            Vec3 at = new Vec3(feetX + rx * p[0] + fx * p[2], feetY + p[1], feetZ + rz * p[0] + fz * p[2]);
            if (at.m_82557_(eye) < CLEARANCE * CLEARANCE) {
                continue;
            }
            puffs++;
            Vec3 drift = new Vec3((RANDOM.nextDouble() - 0.5) * 0.02, 0.01 + RANDOM.nextDouble() * 0.01, (RANDOM.nextDouble() - 0.5) * 0.02);
            RcFx.at(RcFx.PUFF, false, 6 + RANDOM.nextInt(4), at).velocity(drift).drag(0.8F).colors(SMOKE, SMOKE_FADE).size(0.42F, 0.16F).spawn();
            RcFx.at(RcFx.STREAK, true, 4 + RANDOM.nextInt(3), at).drag(0.8F).colors(EDGE, EDGE_FADE).size(0.3F, 0.06F).spawn();
        }
    }
}
