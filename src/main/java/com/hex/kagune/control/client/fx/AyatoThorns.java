package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.ayato.CrystalThorns;
import com.hex.kagune.control.client.AyatoClient;
import com.hex.kagune.control.network.S2CAyatoFx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 5.0.15: the crystal thorns as every client sees them ({@link CrystalThorns}; the server decides when, {@code AyatoThorns} on
 * the server). Which Ayatos have thorns out, laid out from the server's seed so everyone sees the same ones, and the crystal
 * they throw off: a crack of glints and red haze as they erupt, crystal and blood bursting from round him when the tails
 * holding him let go, chips as they shatter. The thorns themselves are drawn by {@code AyatoWingRenderer}, each a
 * {@code CrystalSpike} riding his body as it moves and turns.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class AyatoThorns {
    /** One Ayato's thorns: their layout, when they erupted, and when they started sinking back (NaN while they are out). */
    public record Eruption(CrystalThorns.Thorn[] thorns, long eruptedAt, double sunkAt) {
    }

    /** Thorns out longer than this (ticks) lost their end on the way: they go. */
    private static final long LONGEST = 1200L;
    private static final Map<Integer, Eruption> ERUPTIONS = new HashMap<>();

    private AyatoThorns() {
    }

    /** The Ayatos (by entity id) with thorns out. */
    public static Map<Integer, Eruption> eruptions() {
        return ERUPTIONS;
    }

    /** {@link S2CAyatoFx#THORNS}, {@link S2CAyatoFx#THORNS_PIERCE} and {@link S2CAyatoFx#THORNS_END}. */
    static void accept(ClientLevel level, S2CAyatoFx message, Vec3 at, long now) {
        Entity body = level.m_6815_(message.shooter());
        boolean near = RcFx.near(at, AyatoFx.VISIBLE);
        switch (message.kind()) {
            case S2CAyatoFx.THORNS -> {
                CrystalThorns.Thorn[] thorns = CrystalThorns.thorns(message.id(), Math.min(64, Math.max(0, message.extra())));
                ERUPTIONS.put(message.shooter(), new Eruption(thorns, now, Double.NaN));
                if (near && body instanceof LivingEntity living) {
                    erupting(living, thorns);
                }
            }
            case S2CAyatoFx.THORNS_PIERCE -> {
                if (near) {
                    pierce(level, at, body);
                }
            }
            case S2CAyatoFx.THORNS_END -> {
                Eruption eruption = ERUPTIONS.get(message.shooter());
                if (eruption == null) {
                    return;
                }
                if (message.extra() == 1) {
                    ERUPTIONS.remove(message.shooter());
                    if (near && body instanceof LivingEntity living) {
                        shatter(living, eruption, now);
                    }
                } else if (Double.isNaN(eruption.sunkAt())) {
                    ERUPTIONS.put(message.shooter(), new Eruption(eruption.thorns(), eruption.eruptedAt(), now));
                }
            }
            default -> {
            }
        }
    }

    /**
     * {@code local} (the thorns' body frame: x his right, y up from the feet, z forward) in the world, on {@code body} as drawn at
     * {@code partial}; {@code turn} only rotates it (for directions). Sized by the body's height.
     */
    public static Vec3 world(LivingEntity body, double[] local, float partial, boolean turn) {
        float previous = body.f_20884_;
        float delta = ((body.f_20883_ - previous) % 360.0F + 540.0F) % 360.0F - 180.0F;
        double yaw = Math.toRadians(previous + delta * partial);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double scale = turn ? 1.0 : body.m_20206_() / CrystalThorns.HEIGHT;
        double x = rightX * local[0] * scale + forwardX * local[2] * scale;
        double y = local[1] * scale;
        double z = rightZ * local[0] * scale + forwardZ * local[2] * scale;
        if (turn) {
            return new Vec3(x, y, z);
        }
        double feetX = body.f_19790_ + (body.m_20185_() - body.f_19790_) * partial;
        double feetY = body.f_19791_ + (body.m_20186_() - body.f_19791_) * partial;
        double feetZ = body.f_19792_ + (body.m_20189_() - body.f_19792_) * partial;
        return new Vec3(feetX + x, feetY + y, feetZ + z);
    }

    /** The thorns breaking out: glints racing out along each, red haze round him, a ring of crystal light. */
    private static void erupting(LivingEntity body, CrystalThorns.Thorn[] thorns) {
        Vec3 chest = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.55, 0.0);
        RcFx.at(RcFx.RING, true, 8, chest).bloom(0).colors(0xFFFF6A8A, RcFx.clear(AyatoFx.GEM_CRIMSON)).size(0.4F, 4.2F).spawn();
        for (int i = 0; i < thorns.length; i++) {
            CrystalThorns.Thorn thorn = thorns[i];
            Vec3 root = world(body, thorn.root(), 1.0F, false);
            Vec3 out = world(body, thorn.direction(), 1.0F, true);
            if (i % 2 == 0) {
                AyatoFx.glint(root, out.m_82490_(0.25 + 0.1 * Math.random()), 0.14F, 4 + (int)(Math.random() * 4));
            } else {
                AyatoFx.chip(root, out.m_82490_(0.12 + 0.12 * Math.random()).m_82520_(0.0, 0.04, 0.0), 0.1F, 10 + (int)(Math.random() * 8));
            }
        }
        AyatoFx.haze(chest, Vec3.f_82478_, 1.3F, 10);
    }

    /** The tails let go, pierced: crystal and their blood bursting out from round him, a white flash, a red ring. */
    private static void pierce(ClientLevel level, Vec3 chest, Entity body) {
        RcFx.at(RcFx.MOTE, true, 4, chest).bloom(0).colors(0xFFFFFFFF, RcFx.clear(AyatoFx.GEM_POINT)).size(1.2F, 2.0F).spawn();
        RcFx.at(RcFx.RING, true, 9, chest).bloom(0).colors(0xE0FF2C58, RcFx.clear(AyatoFx.GEM_CRIMSON)).size(0.4F, 3.6F).spawn();
        for (int i = RcFx.count(24); i > 0; i--) {
            Vec3 way = AyatoFx.sphere();
            AyatoFx.chip(chest.m_82549_(way.m_82490_(0.5)), way.m_82490_(0.2 + 0.25 * Math.random()).m_82520_(0.0, 0.06, 0.0), 0.13F,
                12 + (int)(Math.random() * 10));
        }
        for (int i = 0; i < 4; i++) {
            Vec3 way = AyatoFx.sphere();
            AyatoBlood.spurt(level, chest.m_82549_(way.m_82490_(0.8)), way, 0.8F, body);
        }
        AyatoFx.haze(chest, Vec3.f_82478_, 1.6F, 12);
    }

    /** The crystal thawed: every thorn crumbles where it stands. */
    private static void shatter(LivingEntity body, Eruption eruption, long now) {
        for (CrystalThorns.Thorn thorn : eruption.thorns()) {
            double out = CrystalThorns.out(thorn, now - eruption.eruptedAt(), Double.NaN);
            if (out <= 0.05) {
                continue;
            }
            Vec3 middle = world(body, CrystalThorns.tip(thorn, out * 0.5), 1.0F, false);
            Vec3 way = world(body, thorn.direction(), 1.0F, true);
            AyatoFx.crumble(middle, way, 0.5F);
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ERUPTIONS.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            ERUPTIONS.clear();
            return;
        }
        long now = level.m_46467_();
        for (Iterator<Map.Entry<Integer, Eruption>> iterator = ERUPTIONS.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, Eruption> entry = iterator.next();
            Eruption eruption = entry.getValue();
            Entity body = level.m_6815_(entry.getKey());
            // The crystal mode's state may land a moment after the thorns (V pressed while held): give it that moment.
            boolean thawed = now - eruption.eruptedAt() > 10L && body instanceof Player player && !AyatoClient.crystalMode(player);
            boolean gone = !(body instanceof Player) || !body.m_6084_() || thawed;
            boolean sunk = !Double.isNaN(eruption.sunkAt()) && now - eruption.sunkAt() > CrystalThorns.SINK_TICKS + 1;
            if (gone || sunk || now - eruption.eruptedAt() > LONGEST || now < eruption.eruptedAt() - 20L) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ERUPTIONS.clear();
    }
}
