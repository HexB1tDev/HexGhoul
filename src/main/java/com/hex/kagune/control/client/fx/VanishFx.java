package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.network.S2CAyatoFx;
import java.util.Random;
import net.minecraft.world.phys.Vec3;

/**
 * 6.0.0: the kakuja's wind ({@link S2CAyatoFx#VANISH}, {@link S2CAyatoFx#REVEAL}, {@link S2CAyatoFx#GUST}): he vanishes in a
 * whirl of wind spiralling up round him, comes back out of a smaller one, and every dash tears a rush of wind behind him.
 */
final class VanishFx {
    private static final Random RANDOM = new Random();
    private static final int WIND = 0xE6ECF0;

    private VanishFx() {
    }

    static void accept(S2CAyatoFx message, Vec3 at, Vec3 velocity) {
        if (message.kind() == S2CAyatoFx.GUST) {
            gust(at, velocity, message.size());
        } else {
            whirl(at, message.kind() == S2CAyatoFx.VANISH ? 1.0F : 0.55F);
        }
    }

    /** A whirl of wind spiralling up round his body. */
    private static void whirl(Vec3 at, float power) {
        int rings = 3;
        int around = RcFx.count(18 * power);
        for (int ring = 0; ring < rings; ring++) {
            for (int i = 0; i < around; i++) {
                double angle = Math.PI * 2.0 * i / around + ring * 0.7 + RANDOM.nextDouble() * 0.2;
                double radius = 0.55 + 0.18 * ring;
                double c = Math.cos(angle);
                double s = Math.sin(angle);
                Vec3 from = at.m_82520_(c * radius, 0.15 + 0.62 * ring + RANDOM.nextDouble() * 0.2, s * radius);
                Vec3 spin = new Vec3(-s * 0.28 * power + c * 0.04, 0.05 + 0.04 * ring, c * 0.28 * power + s * 0.04);
                int life = 12 + RANDOM.nextInt(8);
                RcFx.at(RcFx.PUFF, false, life, from).velocity(spin).drag(0.88F).fall(-0.004F).wander(0.01F).bloom(0)
                    .colors(0xB0000000 | WIND, RcFx.clear(WIND)).size(0.3F, 0.85F).spawn();
                if (i % 3 == 0) {
                    RcFx.at(RcFx.STREAK, false, 8 + RANDOM.nextInt(4), from).velocity(spin.m_82490_(1.6)).drag(0.85F).bloom(0)
                        .colors(0xD0000000 | WIND, RcFx.clear(WIND)).size(0.25F, 0.1F).spawn();
                }
            }
        }
    }

    /** A rush of wind left behind a dash along {@code velocity}. */
    private static void gust(Vec3 at, Vec3 velocity, float power) {
        if (velocity.m_82556_() < 1.0E-6) {
            return;
        }
        Vec3 way = velocity.m_82541_();
        Vec3 across = new Vec3(-way.f_82481_, 0.0, way.f_82479_);
        for (int i = RcFx.count(16 * power); i > 0; i--) {
            Vec3 from = at.m_82549_(across.m_82490_((RANDOM.nextDouble() - 0.5) * 0.9)).m_82520_(0.0, 0.2 + RANDOM.nextDouble() * 1.4, 0.0);
            Vec3 drift = way.m_82490_(-0.12 - RANDOM.nextDouble() * 0.15).m_82520_(0.0, 0.02, 0.0);
            RcFx.at(RcFx.STREAK, false, 7 + RANDOM.nextInt(5), from).velocity(drift).drag(0.86F).bloom(0)
                .colors(0xC8000000 | WIND, RcFx.clear(WIND)).size(0.3F * power + 0.1F, 0.08F).spawn();
            if (i % 2 == 0) {
                RcFx.at(RcFx.PUFF, false, 10 + RANDOM.nextInt(6), from).velocity(drift.m_82490_(0.6)).drag(0.9F).fall(-0.002F).bloom(0)
                    .colors(0x80000000 | WIND, RcFx.clear(WIND)).size(0.25F, 0.7F).spawn();
            }
        }
    }
}
