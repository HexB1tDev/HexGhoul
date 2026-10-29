package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.client.EtoCamera;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.OwlLanceClient;
import com.hex.kagune.control.eto.OwlLances;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0, the Owl's Watch This lances seen tearing out of her: as each bursts out of her back, blood and chips of bone thrown off
 * where it tore out and up the way it goes, and the view of anyone close jolted; as they draw back in, a last spatter off her back.
 * All from what every client already knows (each lance's phase as the server said): no packets of its own.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class OwlLanceFx {
    /** Beyond this (blocks from the eye) nothing is thrown. */
    static final double RANGE = 64.0;
    static final int BONE = 0xFFEDE6D8;
    static final int BONE_FADE = 0xFFB9B0A0;
    static final int BLOOD = 0xE09A0A18;
    static final int BLOOD_FADE = 0x00500408;

    /** Each player's lances as last seen: what each was doing, and since when (the server's tick it began). */
    private static final Map<UUID, long[]> SEEN = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private OwlLanceFx() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (event.phase != TickEvent.Phase.END || level == null || minecraft.m_91104_() || minecraft.f_91063_ == null) {
            if (level == null) {
                SEEN.clear();
            }
            return;
        }
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        double clock = EtoClient.clock(0.0F);
        for (AbstractClientPlayer player : level.m_6907_()) {
            if (!OwlLanceClient.out(player) || !player.m_6084_() || player.m_20182_().m_82557_(camera) > RANGE * RANGE) {
                SEEN.remove(player.m_20148_());
                continue;
            }
            long[] seen = SEEN.computeIfAbsent(player.m_20148_(), id -> new long[OwlLances.COUNT * 2]);
            boolean burst = false;
            for (int i = 0; i < OwlLances.COUNT; i++) {
                OwlLanceClient.Lance lance = OwlLanceClient.get(player, i);
                long phase = lance.phase().ordinal();
                if (seen[i * 2] == phase && seen[i * 2 + 1] == lance.startedAt()) {
                    continue;
                }
                seen[i * 2] = phase;
                seen[i * 2 + 1] = lance.startedAt();
                if (lance.phase() == OwlLances.Phase.OUT) {
                    torn(player, i, false);
                    burst = true;
                } else if (lance.phase() == OwlLances.Phase.IN) {
                    torn(player, i, true);
                }
            }
            if (burst && minecraft.f_91074_ != null) {
                double away = minecraft.f_91074_.m_20182_().m_82554_(player.m_20182_());
                if (away < 10.0) {
                    EtoCamera.kick((float)(0.6 * (1.0 - away / 10.0)), clock);
                }
            }
        }
    }

    /** Where lance {@code i} tears out of {@code player}'s back (the world), and the way it goes out: blood, bone chips (or, going in, a spatter). */
    private static void torn(AbstractClientPlayer player, int i, boolean goingIn) {
        double yaw = Math.toRadians(player.f_20883_);
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-ahead.f_82481_, 0.0, ahead.f_82479_);
        double[] px = OwlLances.px(i);
        // Her back as she stands in the Owl (torso pixels to blocks: x to her left, y down from her shoulders, z back).
        Vec3 root = player.m_20182_().m_82549_(right.m_82490_(-px[0] / 16.0 * 1.2)).m_82520_(0.0, 1.62 - px[1] / 16.0 * 1.2, 0.0)
            .m_82549_(ahead.m_82490_(-px[2] / 16.0 * 1.2 - 0.05));
        double b = OwlLances.bearing(i);
        Vec3 way = right.m_82490_(Math.sin(b) * 0.6).m_82549_(ahead.m_82490_(Math.cos(b) * 0.6 - 0.5)).m_82520_(0.0, 1.0, 0.0).m_82541_();
        int spray = RcFx.count(goingIn ? 4.0F : 7.0F);
        for (int k = 0; k < spray; k++) {
            Vec3 v = way.m_82490_(0.08 + 0.12 * RANDOM.nextDouble()).m_82520_(RANDOM.nextGaussian() * 0.05, 0.03 * RANDOM.nextDouble(), RANDOM.nextGaussian() * 0.05);
            RcFx.at(RcFx.MOTE, false, 12 + RANDOM.nextInt(8), root).velocity(v).fall(0.04F).drag(0.92F).colors(0xFFB0101E, 0xFF5A0610).size(0.07F, 0.04F)
                .spawn();
        }
        if (goingIn) {
            return;
        }
        RcFx.at(RcFx.PUFF, false, 14 + RANDOM.nextInt(6), root).velocity(way.m_82490_(0.04)).drag(0.86F).colors(BLOOD, BLOOD_FADE).size(0.25F, 0.75F).spawn();
        for (int k = RcFx.count(3.0F); k > 0; k--) {
            Vec3 chip = way.m_82490_(0.25 + 0.25 * RANDOM.nextDouble()).m_82520_(RANDOM.nextGaussian() * 0.06, RANDOM.nextGaussian() * 0.06, RANDOM.nextGaussian() * 0.06);
            RcFx.at(RcFx.MOTE, false, 10 + RANDOM.nextInt(6), root).velocity(chip).fall(0.03F).drag(0.9F).colors(BONE, BONE_FADE).size(0.06F, 0.04F).spawn();
        }
        RcFx.at(RcFx.STREAK, true, 4, root.m_82549_(way.m_82490_(1.0))).velocity(way.m_82490_(0.6)).drag(0.6F).colors(0xCCFFFFFF, RcFx.clear(0xFFF2EDE6))
            .size(0.12F, 0.03F).bloom(0.0F).spawn();
    }
}
