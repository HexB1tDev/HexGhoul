package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.client.WoundClient;
import com.hex.kagune.control.client.render.TendrilPaths;
import com.hex.kagune.control.network.S2CAyatoSpikes;
import com.hex.kagune.control.wound.WoundBook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What this client knows of Ayato's crystal spikes at rest: the ones stuck in bodies ({@link S2CAyatoSpikes}, drawn by the
 * wing renderer on the body as it moves and turns), the bleeding they cause (blood dripping from each spike, and from the
 * body, by {@link AyatoBlood}), and spikes that hit a block, which stay planted in it for a moment before they crumble.
 * A spike stays in a body for fifteen seconds before it bursts.
 * Purely visual.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class AyatoSpikes {
    /**
     * One body's crystal: its spikes, its bleeding, and how far (degrees) this client's idea of the body's yaw was off the
     * server's when last told (the spikes are turned by it so they sit where they went in).
     */
    public record Body(int bleedStacks, long bleedUntil, List<S2CAyatoSpikes.Spike> spikes, float yawOffset) {
    }

    /** A spike stuck in a block. */
    public record Planted(Vec3 tip, Vec3 direction, float size, int kind, long born, long until, int seed) {
    }

    private static final Map<Integer, Body> BODIES = new HashMap<>();
    private static final List<Planted> PLANTED = new ArrayList<>();
    /** The share of a spike's length buried in the block it hit. */
    static final double PLANTED_DEPTH = 0.45;
    /** Ticks a spike stays planted in a block before it crumbles. */
    static final int PLANTED_TICKS = 40;
    private static final int MOST_PLANTED = 160;
    private static final double VISIBLE = 64.0;

    private AyatoSpikes() {
    }

    public static void accept(S2CAyatoSpikes message) {
        if (message.spikes().isEmpty() && message.bleedStacks() <= 0) {
            BODIES.remove(message.entityId());
            return;
        }
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        float offset = 0.0F;
        if (level != null && level.m_6815_(message.entityId()) instanceof LivingEntity living) {
            offset = wrap(message.bodyYaw() - living.f_20883_);
        }
        Body before = BODIES.put(message.entityId(), new Body(message.bleedStacks(), message.bleedUntil(), List.copyOf(message.spikes()), offset));
        if (before != null && level != null) {
            punctures(level.m_6815_(message.entityId()), before.spikes(), message.spikes());
        }
    }

    /**
     * 3.0.0: every spike that has left a player's body (burst or pulled out) leaves a puncture where it sat
     * ({@code WoundClient}).
     */
    static void punctures(Entity entity, List<S2CAyatoSpikes.Spike> before, List<S2CAyatoSpikes.Spike> now) {
        if (!(entity instanceof AbstractClientPlayer player) || !player.m_6084_()) {
            return;
        }
        boolean slim = "slim".equals(player.m_108564_());
        for (S2CAyatoSpikes.Spike spike : before) {
            if (spike.limb() < 0 && now.stream().noneMatch(kept -> kept.id() == spike.id())) { // one in a tentacle leaves no wound
                WoundBook.Spot spot = WoundBook.spot(spike.ox(), spike.oy(), spike.oz(), spike.dx(), spike.dy(), spike.dz(), slim);
                WoundClient.puncture(player, spot.part(), spot.face(), spot.u(), spot.v(), spike.id());
            }
        }
    }

    private static float wrap(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        } else if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    /** Bodies with spikes in them or bleeding, by entity id. */
    public static Map<Integer, Body> bodies() {
        return BODIES;
    }

    public static List<Planted> planted() {
        return PLANTED;
    }

    /** A spike that hit a block: it stays in it a moment. */
    static void plant(Vec3 tip, Vec3 direction, float size, int kind, long now, int seed) {
        if (PLANTED.size() >= MOST_PLANTED) {
            PLANTED.remove(0);
        }
        PLANTED.add(new Planted(tip, direction, size, kind, now, now + PLANTED_TICKS + seed % 12, seed));
    }

    /** Where a stuck spike's point is in the world, and which way it points, for the body as it stands this frame. */
    public static Vec3[] place(LivingEntity body, S2CAyatoSpikes.Spike spike, double partial) {
        Body known = BODIES.get(body.m_19879_());
        return place(body, spike, partial, known == null ? 0.0F : known.yawOffset());
    }

    /** {@link #place}, with this client's yaw for the body corrected by {@code yawOffset} degrees. */
    static Vec3[] place(LivingEntity body, S2CAyatoSpikes.Spike spike, double partial, float yawOffset) {
        double x = body.f_19790_ + (body.m_20185_() - body.f_19790_) * partial;
        double y = body.f_19791_ + (body.m_20186_() - body.f_19791_) * partial;
        double z = body.f_19792_ + (body.m_20189_() - body.f_19792_) * partial;
        float delta = ((body.f_20883_ - body.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
        double yaw = Math.toRadians(body.f_20884_ + delta * partial + yawOffset);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        Vec3 at = new Vec3(x + rightX * spike.ox() + forwardX * spike.oz(), y + spike.oy(), z + rightZ * spike.ox() + forwardZ * spike.oz());
        Vec3 dir = new Vec3(rightX * spike.dx() + forwardX * spike.dz(), spike.dy(), rightZ * spike.dx() + forwardZ * spike.dz());
        if (spike.limb() >= 0 && body instanceof AbstractClientPlayer player) {
            // 5.0.14: stuck in a tentacle a guard put in its way: it rides the tentacle, at the point where it went in (found once,
            // on the tentacle as drawn when it arrived), its point inside the tentacle.
            double[][] path = TendrilPaths.path(player.m_36316_().getId(), spike.limb(), com.hex.kagune.control.client.ControlClient.worldTime((float)partial));
            if (path != null) {
                Double along = ALONG.get(spike.id());
                if (along == null) {
                    along = TendrilPaths.nearest(path, at);
                    ALONG.put(spike.id(), along);
                }
                Vec3 heading = dir.m_82556_() < 1.0E-8 ? dir : dir.m_82541_();
                return new Vec3[] {TendrilPaths.at(path, along).m_82546_(heading.m_82490_(TENTACLE_SURFACE)), dir};
            }
        }
        return new Vec3[] {at, dir};
    }

    /** 5.0.14: where along its tentacle each spike stuck in one sits (0 root .. 1 tip), by spike id (pruned every tick). */
    private static final Map<Integer, Double> ALONG = new HashMap<>();
    /** How far in front of a tentacle's centre line a spike stuck in it goes in (about its thickness). */
    static final double TENTACLE_SURFACE = 0.14;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.m_91104_()) {
            return;
        }
        long now = level.m_46467_();
        for (Iterator<Planted> iterator = PLANTED.iterator(); iterator.hasNext(); ) {
            Planted planted = iterator.next();
            if (now >= planted.until()) {
                iterator.remove();
                if (RcFx.near(planted.tip(), VISIBLE)) {
                    AyatoFx.crumble(planted.tip(), planted.direction(), planted.size());
                }
            }
        }
        java.util.Set<Integer> inTentacles = new java.util.HashSet<>();
        for (Iterator<Map.Entry<Integer, Body>> iterator = BODIES.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, Body> entry = iterator.next();
            Body body = entry.getValue();
            for (S2CAyatoSpikes.Spike spike : body.spikes()) {
                if (spike.limb() >= 0) {
                    inTentacles.add(spike.id());
                }
            }
            Entity entity = level.m_6815_(entry.getKey());
            boolean bleeding = body.bleedStacks() > 0 && now < body.bleedUntil();
            // Spikes long overdue (a lost message) and bleeding that has stopped are forgotten.
            List<S2CAyatoSpikes.Spike> live = body.spikes().stream().filter(spike -> now < spike.burstAt() + 40L).toList();
            if (live.size() != body.spikes().size()) {
                punctures(entity, body.spikes(), live);
            }
            if (entity == null || !entity.m_6084_() || live.isEmpty() && !bleeding) {
                iterator.remove();
                continue;
            }
            if (live.size() != body.spikes().size()) {
                entry.setValue(new Body(body.bleedStacks(), body.bleedUntil(), live, body.yawOffset()));
            }
            if (bleeding && entity instanceof LivingEntity living && RcFx.near(living.m_20182_(), VISIBLE)) {
                List<Vec3> wounds = new ArrayList<>();
                for (S2CAyatoSpikes.Spike spike : live) {
                    wounds.add(place(living, spike, 1.0)[0]);
                }
                AyatoBlood.bleed(level, living, body.bleedStacks(), wounds);
            }
        }
        ALONG.keySet().retainAll(inTentacles);
        AyatoBlood.tick(level);
        BladeBloodFx.tick(level);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BODIES.clear();
        PLANTED.clear();
        ALONG.clear();
        AyatoBlood.clear();
        BladeBloodFx.clear();
    }
}
