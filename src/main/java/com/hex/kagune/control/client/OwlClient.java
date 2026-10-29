package com.hex.kagune.control.client;

import com.hex.kagune.control.eto.OwlRules;
import com.hex.kagune.control.network.S2CEtoOwl;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: the client's store of every Eto player's Owl (fed only by the server, {@link S2CEtoOwl}), timed on {@link EtoClient#clock}:
 * whether she is growing it or turning back, and since when.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class OwlClient {
    /** Growing it ({@code on}) or turning back, since {@code began} (the clock); whether its flesh has burst out of her yet. */
    public static final class Owl {
        private final boolean on;
        private final double began;
        boolean burst;

        Owl(boolean on, double began) {
            this.on = on;
            this.began = began;
        }

        public boolean on() {
            return this.on;
        }

        public double age(double clock) {
            return Math.max(0.0, clock - this.began);
        }

        /** How grown it is (0..1). */
        public double grown(double clock) {
            return OwlRules.grown(this.on, this.age(clock));
        }

        /** Whether anything of it is to be seen or felt: grown, growing, or still turning back. */
        public boolean shown(double clock) {
            return this.on || OwlRules.changing(false, this.age(clock));
        }
    }

    private static final Map<UUID, Owl> OWLS = new HashMap<>();
    /** 6.2.4: grown out past this (0..1), the creature is drawn in her place and her own body is not. */
    public static final double SHOWN = 0.01;

    private OwlClient() {
    }

    public static void accept(S2CEtoOwl message) {
        Minecraft minecraft = Minecraft.m_91087_();
        long now = minecraft.f_91073_ == null ? message.at() : minecraft.f_91073_.m_46467_();
        long behind = now - message.at();
        Owl before = OWLS.get(message.playerId());
        if (!message.on() && before == null) {
            return; // never seen in it
        }
        // Coming into view of her in it (long since grown): already grown, not growing all over again.
        double began = EtoClient.clock(0.0F) - (behind > EtoClient.JUST_BEGUN || behind < 0L ? behind : 0L);
        OWLS.put(message.playerId(), new Owl(message.on(), began));
    }

    /** {@code player}'s Owl, or null when she has none (nor is turning back from it). */
    public static Owl get(Player player, double clock) {
        if (player == null) {
            return null;
        }
        Owl owl = OWLS.get(player.m_36316_().getId());
        if (owl != null && !owl.shown(clock)) {
            OWLS.remove(player.m_36316_().getId());
            return null;
        }
        return owl;
    }

    /** 6.2.4: whether {@code player} is the Owl's creature at {@code clock} (burst out of her, not yet shrunk back into her). */
    public static boolean creature(Player player, double clock) {
        Owl owl = get(player, clock);
        return owl != null && owl.grown(clock) > SHOWN;
    }

    /**
     * 6.2.4: her own body is not drawn while the creature is, in its place (EtoFleshRenderer draws it): first of anything, so nothing
     * else that would change her model for this frame ever runs on it.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void hideBody(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof net.minecraft.client.player.AbstractClientPlayer player && creature(player, EtoClient.clock(event.getPartialTick()))) {
            event.setCanceled(true);
        }
    }

    /** 6.2.4: nor her own hands in first person (she sees out of its eye; its own arms are drawn). */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void hideHands(net.minecraftforge.client.event.RenderHandEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ != null && creature(minecraft.f_91074_, EtoClient.clock(minecraft.m_91296_()))) {
            event.setCanceled(true);
        }
    }

    /**
     * As it grows, the moment it bursts out of her: blood out of both shoulders, and (6.2.4) the crest igniting at the nape of her
     * neck: a burst of burning red shards flung out all round her back and a ring of red light.
     */
    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (event.phase != TickEvent.Phase.END || OWLS.isEmpty() || minecraft.f_91073_ == null || minecraft.m_91104_()) {
            return;
        }
        double clock = EtoClient.clock(0.0F);
        for (net.minecraft.client.player.AbstractClientPlayer player : minecraft.f_91073_.m_6907_()) {
            Owl owl = OWLS.get(player.m_36316_().getId());
            if (owl == null || !owl.on || owl.burst || owl.age(clock) < OwlRules.BURST) {
                continue;
            }
            owl.burst = true;
            if (owl.age(clock) > OwlRules.BURST + 4.0 || player.m_20145_()) {
                continue; // long since (she came into view in it)
            }
            double yaw = Math.toRadians(player.f_20883_);
            Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
            Vec3 back = new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
            for (int side : new int[] {1, -1}) {
                Vec3 at = player.m_20182_().m_82520_(0.0, 1.35, 0.0).m_82549_(right.m_82490_(0.42 * side)).m_82549_(back.m_82490_(0.1));
                com.hex.kagune.control.client.fx.AyatoBlood.spurt(minecraft.f_91073_, at, right.m_82490_(side).m_82520_(0.0, 0.9, 0.0).m_82549_(back.m_82490_(0.4)),
                    1.1F, player);
            }
            Vec3 nape = player.m_20182_().m_82520_(0.0, 1.28, 0.0).m_82549_(back.m_82490_(0.24));
            java.util.Random random = new java.util.Random(player.m_19879_() * 7919L + (long)clock);
            int shards = 6 + (int)(26 * com.hex.kagune.control.client.fx.RcFx.density());
            for (int i = 0; i < shards; i++) {
                double phi = Math.PI * 2.0 * i / shards + random.nextDouble() * 0.3;
                double across = Math.sin(phi);
                double up = Math.max(-0.35, Math.cos(phi));
                double out = 0.55 + random.nextDouble() * 0.7;
                double length = Math.sqrt(across * across + up * up + out * out);
                double speed = (0.3 + random.nextDouble() * 0.35) / length;
                Vec3 way = right.m_82490_(across * speed).m_82520_(0.0, up * speed, 0.0).m_82549_(back.m_82490_(out * speed));
                com.hex.kagune.control.client.fx.RcFx.at(com.hex.kagune.control.client.fx.RcFx.STREAK, true, 7 + random.nextInt(6), nape).velocity(way)
                    .drag(0.8F).bloom(0).colors(com.hex.kagune.control.client.fx.RcFx.CORE, com.hex.kagune.control.client.fx.RcFx.clear(
                        com.hex.kagune.control.client.fx.RcFx.RED)).size(0.26F, 0.04F).spawn();
                if (i % 2 == 0) {
                    com.hex.kagune.control.client.fx.RcFx.at(com.hex.kagune.control.client.fx.RcFx.EMBER, true, 14 + random.nextInt(10), nape)
                        .velocity(way.m_82490_(0.6)).drag(0.9F).fall(-0.004F).wander(0.01F).bloom(0).colors(com.hex.kagune.control.client.fx.RcFx.FIRE,
                            com.hex.kagune.control.client.fx.RcFx.clear(com.hex.kagune.control.client.fx.RcFx.CRIMSON)).size(0.12F, 0.02F).spawn();
                }
            }
            com.hex.kagune.control.client.fx.RcFx.at(com.hex.kagune.control.client.fx.RcFx.RING, true, 9, nape).bloom(0)
                .colors(0xFFFF4A3A, com.hex.kagune.control.client.fx.RcFx.clear(com.hex.kagune.control.client.fx.RcFx.CRIMSON)).size(0.3F, 2.6F).spawn();
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        OWLS.clear();
    }
}
