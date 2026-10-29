package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.client.EtoCamera;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.eto.EtoMove;
import com.hex.kagune.control.eto.OwlSpikes;
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
 * 6.2.0, the Owl's bone spikes seen bursting out: as each tears out of her back, blood and chips of bone thrown off where it tore
 * out and flung along the way it shoots; with the first, the ground blown out round her feet in a ring of dust and the view of
 * anyone close jolted; as they draw back in, a last spatter. All from what every client already knows (her move and its seed): no
 * packets.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class OwlSpikeFx {
    /** Beyond this (blocks from the eye) nothing is thrown. */
    static final double RANGE = 64.0;
    static final int DUST = 0xC8D2C6B4;
    static final int DUST_FADE = 0x00B4A894;
    static final int BONE = 0xFFEDE6D8;
    static final int BONE_FADE = 0xFFB9B0A0;
    static final int BLOOD = 0xE09A0A18;
    static final int BLOOD_FADE = 0x00500408;

    /** One burst being followed: which (when it began) and how far in it was last tick. */
    private static final class Seen {
        long started;
        double last = -1.0;
        long tick;
    }

    private static final Map<UUID, Seen> SEEN = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private OwlSpikeFx() {
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
        long now = level.m_46467_();
        double clock = EtoClient.clock(0.0F);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        for (AbstractClientPlayer player : level.m_6907_()) {
            EtoClient.EtoView view = EtoClient.get(player);
            if (view.move() != EtoMove.OWL_SPIKES || !player.m_6084_() || player.m_20182_().m_82557_(camera) > RANGE * RANGE) {
                continue;
            }
            Seen seen = SEEN.computeIfAbsent(player.m_20148_(), id -> new Seen());
            if (seen.started != view.startedAt()) {
                seen.started = view.startedAt();
                seen.last = -1.0;
            }
            seen.tick = now;
            double age = view.age(clock);
            burst(minecraft, player, view, seen.last, age, clock);
            seen.last = age;
        }
        if (!SEEN.isEmpty()) {
            SEEN.values().removeIf(seen -> seen.tick != now);
        }
    }

    /** What this tick of the burst throws, from {@code last} ticks in to {@code age}. */
    private static void burst(Minecraft minecraft, AbstractClientPlayer player, EtoClient.EtoView view, double last, double age, double clock) {
        double yaw = Math.toRadians(view.yaw());
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 feet = player.m_20182_();
        OwlSpikes.Spike[] spikes = OwlSpikes.of(view.detail());
        double first = Double.MAX_VALUE;
        for (OwlSpikes.Spike spike : spikes) {
            first = Math.min(first, spike.erupt());
        }
        if (last < first && age >= first) {
            // The first tears out: the ground blown out round her feet, everyone close jolted.
            RcFx.at(RcFx.RING, false, 10, feet.m_82520_(0.0, 0.08, 0.0)).colors(DUST, DUST_FADE).size(0.4F, 3.2F).bloom(0.0F).spawn();
            for (int i = RcFx.count(18.0F); i > 0; i--) {
                double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                Vec3 out = new Vec3(Math.cos(angle), 0.02, Math.sin(angle)).m_82490_(0.12 + 0.12 * RANDOM.nextDouble());
                RcFx.at(RcFx.PUFF, false, 18 + RANDOM.nextInt(10), feet.m_82520_(Math.cos(angle) * 0.4, 0.1, Math.sin(angle) * 0.4)).velocity(out)
                    .drag(0.88F).colors(DUST, DUST_FADE).size(0.35F, 1.2F).spawn();
            }
            if (minecraft.f_91074_ != null) {
                double away = minecraft.f_91074_.m_20182_().m_82554_(feet);
                if (away < 10.0) {
                    EtoCamera.kick((float)(0.7 * (1.0 - away / 10.0)), clock);
                }
            }
        }
        for (OwlSpikes.Spike spike : spikes) {
            if (last >= spike.erupt() || age < spike.erupt()) {
                continue;
            }
            double[] r = OwlSpikes.root(spike);
            Vec3 root = feet.m_82549_(right.m_82490_(r[0])).m_82549_(facing.m_82490_(r[2])).m_82520_(0.0, r[1], 0.0);
            double[] d = spike.dir();
            Vec3 way = right.m_82490_(d[0]).m_82549_(facing.m_82490_(d[2])).m_82520_(0.0, d[1], 0.0);
            // Torn out of her: blood and a breath of red mist where it came out.
            for (int i = RcFx.count(5.0F); i > 0; i--) {
                Vec3 spray = way.m_82490_(0.06 + 0.1 * RANDOM.nextDouble()).m_82520_(RANDOM.nextGaussian() * 0.05, 0.04 + 0.05 * RANDOM.nextDouble(),
                    RANDOM.nextGaussian() * 0.05);
                RcFx.at(RcFx.MOTE, false, 12 + RANDOM.nextInt(8), root).velocity(spray).fall(0.04F).drag(0.92F).colors(0xFFB0101E, 0xFF5A0610)
                    .size(0.07F, 0.04F).spawn();
            }
            RcFx.at(RcFx.PUFF, false, 14 + RANDOM.nextInt(6), root).velocity(way.m_82490_(0.03)).drag(0.86F).colors(BLOOD, BLOOD_FADE).size(0.25F, 0.7F)
                .spawn();
            // Chips of bone flung along the way it shot out, a streak racing out with its point.
            for (int i = RcFx.count(3.0F); i > 0; i--) {
                Vec3 chip = way.m_82490_(0.25 + 0.25 * RANDOM.nextDouble()).m_82520_(RANDOM.nextGaussian() * 0.06, RANDOM.nextGaussian() * 0.06,
                    RANDOM.nextGaussian() * 0.06);
                RcFx.at(RcFx.MOTE, false, 10 + RANDOM.nextInt(6), root).velocity(chip).fall(0.03F).drag(0.9F).colors(BONE, BONE_FADE).size(0.06F, 0.04F)
                    .spawn();
            }
            RcFx.at(RcFx.STREAK, true, 4, root.m_82549_(way.m_82490_(spike.length() * 0.5))).velocity(way.m_82490_(0.6)).drag(0.6F)
                .colors(0xCCFFFFFF, RcFx.clear(0xFFF2EDE6)).size(0.12F, 0.03F).bloom(0.0F).spawn();
        }
        if (view.charge() > 0.0F) {
            double letGo = view.charge();
            if (last < letGo && age >= letGo) {
                // Drawn back in: a last spatter off her back.
                Vec3 back = feet.m_82549_(facing.m_82490_(0.2)).m_82520_(0.0, 1.2, 0.0);
                for (int i = RcFx.count(8.0F); i > 0; i--) {
                    Vec3 spray = new Vec3(RANDOM.nextGaussian() * 0.08, 0.06 + 0.06 * RANDOM.nextDouble(), RANDOM.nextGaussian() * 0.08);
                    RcFx.at(RcFx.MOTE, false, 12 + RANDOM.nextInt(8), back).velocity(spray).fall(0.04F).drag(0.92F).colors(0xFFB0101E, 0xFF5A0610)
                        .size(0.07F, 0.04F).spawn();
                }
            }
        }
    }
}
