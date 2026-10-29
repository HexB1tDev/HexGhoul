package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.eto.OwlShards;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.0, the Owl's volley spears seen: ripped off the giant blade (blood, chips of bone, a white streak away along its throw),
 * striking a wall or the ground (grit thrown back off it; blood too when it pins something there), and crumbling to bone dust when
 * they go.
 */
public final class OwlShardFx {
    static final int BONE = 0xFFEDE6D8;
    static final int BONE_FADE = 0xFFB9B0A0;
    static final int DUST = 0xC8D2C6B4;
    static final int DUST_FADE = 0x00B4A894;
    private static final java.util.Random RANDOM = new java.util.Random();

    private OwlShardFx() {
    }

    /** Torn off the blade at {@code at}, thrown at {@code speed}. */
    public static void ripped(double[] at, double[] speed) {
        Vec3 p = new Vec3(at[0], at[1], at[2]);
        Vec3 v = new Vec3(speed[0], speed[1], speed[2]);
        Vec3 way = v.m_82553_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : v.m_82541_();
        for (int i = RcFx.count(3.0F); i > 0; i--) {
            Vec3 spray = way.m_82490_(0.05 + 0.08 * RANDOM.nextDouble()).m_82520_(RANDOM.nextGaussian() * 0.05, 0.04 * RANDOM.nextDouble(), RANDOM.nextGaussian() * 0.05);
            RcFx.at(RcFx.MOTE, false, 10 + RANDOM.nextInt(6), p).velocity(spray).fall(0.04F).drag(0.92F).colors(0xFFB0101E, 0xFF5A0610).size(0.06F, 0.035F)
                .spawn();
        }
        for (int i = RcFx.count(2.0F); i > 0; i--) {
            Vec3 chip = way.m_82490_(0.2 + 0.2 * RANDOM.nextDouble()).m_82520_(RANDOM.nextGaussian() * 0.08, RANDOM.nextGaussian() * 0.08, RANDOM.nextGaussian() * 0.08);
            RcFx.at(RcFx.MOTE, false, 8 + RANDOM.nextInt(5), p).velocity(chip).fall(0.03F).drag(0.9F).colors(BONE, BONE_FADE).size(0.05F, 0.03F).spawn();
        }
        RcFx.at(RcFx.STREAK, true, 4, p.m_82549_(way.m_82490_(1.2))).velocity(way.m_82490_(0.8)).drag(0.6F).colors(0xCCFFFFFF, RcFx.clear(0xFFF2EDE6))
            .size(0.11F, 0.025F).bloom(0.0F).spawn();
    }

    /** Its point stuck at {@code at}, going {@code way}; {@code pinned}: through something into it. */
    public static void stuck(double[] at, double[] way, boolean pinned) {
        Vec3 p = new Vec3(at[0], at[1], at[2]);
        Vec3 back = new Vec3(-way[0], -way[1], -way[2]);
        for (int i = RcFx.count(5.0F); i > 0; i--) {
            Vec3 v = back.m_82490_(0.08 + 0.1 * RANDOM.nextDouble()).m_82520_(RANDOM.nextGaussian() * 0.07, 0.06 * RANDOM.nextDouble(), RANDOM.nextGaussian() * 0.07);
            RcFx.at(RcFx.PUFF, false, 12 + RANDOM.nextInt(8), p).velocity(v).drag(0.85F).colors(DUST, DUST_FADE).size(0.18F, 0.5F).spawn();
        }
        if (pinned) {
            for (int i = RcFx.count(6.0F); i > 0; i--) {
                Vec3 v = new Vec3(RANDOM.nextGaussian() * 0.08, 0.05 + 0.05 * RANDOM.nextDouble(), RANDOM.nextGaussian() * 0.08);
                RcFx.at(RcFx.MOTE, false, 12 + RANDOM.nextInt(8), p.m_82549_(back.m_82490_(0.5))).velocity(v).fall(0.04F).drag(0.92F)
                    .colors(0xFFB0101E, 0xFF5A0610).size(0.07F, 0.04F).spawn();
            }
        }
    }

    /** Crumbled where it was ({@code at} its point, {@code way} the way it pointed): bone dust down its length. */
    public static void crumbled(double[] at, double[] way) {
        for (int i = RcFx.count(6.0F); i > 0; i--) {
            double u = RANDOM.nextDouble() * OwlShards.LENGTH;
            Vec3 p = new Vec3(at[0] - way[0] * u, at[1] - way[1] * u, at[2] - way[2] * u);
            RcFx.at(RcFx.PUFF, false, 14 + RANDOM.nextInt(8), p).velocity(new Vec3(RANDOM.nextGaussian() * 0.02, 0.01, RANDOM.nextGaussian() * 0.02)).drag(0.9F)
                .colors(0xB0EDE6D8, 0x00B9B0A0).size(0.14F, 0.35F).spawn();
        }
    }
}
