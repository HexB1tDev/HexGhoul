package com.hex.kagune.control.client;

import com.hex.kagune.control.client.fx.OwlShardFx;
import com.hex.kagune.control.eto.OwlShards;
import com.hex.kagune.control.network.S2COwlShard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: the client's store of every Owl's volley spears ({@link S2COwlShard}): each flown from where and when it was thrown and how
 * fast (on {@link EtoClient#clock}, as every client flies it alike) until the server says where it stuck, then held there until it
 * crumbles.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class OwlShardClient {
    /** One spear: thrown from {@code from} at {@code speed} when the clock read {@code began}; stuck (its point, the way it points), or not. */
    public static final class Shard {
        public final int id;
        final double[] from;
        final double[] speed;
        final double began;
        double[] stuckAt;
        double[] stuckWay;
        double stuckSince;

        Shard(int id, double[] from, double[] speed, double began) {
            this.id = id;
            this.from = from;
            this.speed = speed;
            this.began = began;
        }

        /** Where its point is at {@code clock} (the world). */
        public double[] point(double clock) {
            if (this.stuckAt != null) {
                return this.stuckAt;
            }
            return OwlShards.at(this.from, this.speed, Math.max(0.0, Math.min(OwlShards.LIFE + 4.0, clock - this.began)));
        }

        /** The way it points at {@code clock} (the world, unit). */
        public double[] way(double clock) {
            double[] v = this.stuckWay != null ? this.stuckWay
                : OwlShards.velocity(this.speed, Math.max(0.0, Math.min(OwlShards.LIFE + 4.0, clock - this.began)));
            double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            return l < 1.0E-9 ? new double[] {0.0, 0.0, 1.0} : new double[] {v[0] / l, v[1] / l, v[2] / l};
        }

        /** Whether it has stuck in something. */
        public boolean stuck() {
            return this.stuckAt != null;
        }
    }

    /** By player, each of her spears by its id. */
    private static final Map<UUID, Map<Integer, Shard>> SHARDS = new HashMap<>();

    private OwlShardClient() {
    }

    public static void accept(S2COwlShard message) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (message.kind() == S2COwlShard.CLEAR) {
            SHARDS.remove(message.playerId());
            return;
        }
        long behind = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_() - message.tick();
        double clock = EtoClient.clock(0.0F);
        double began = clock - (Math.abs(behind) > EtoClient.JUST_BEGUN || behind < 0L ? behind : 0L);
        Map<Integer, Shard> hers = SHARDS.computeIfAbsent(message.playerId(), ignored -> new LinkedHashMap<>());
        double[] at = {message.x(), message.y(), message.z()};
        double[] d = {message.dx(), message.dy(), message.dz()};
        switch (message.kind()) {
            case S2COwlShard.THROWN -> {
                hers.put(message.id(), new Shard(message.id(), at, d, began));
                OwlShardFx.ripped(at, d);
            }
            case S2COwlShard.STUCK -> {
                Shard shard = hers.computeIfAbsent(message.id(), id -> new Shard(id, at, d, began));
                shard.stuckAt = at;
                shard.stuckWay = d;
                shard.stuckSince = clock;
                OwlShardFx.stuck(at, d, message.body() != 0);
            }
            case S2COwlShard.GONE -> {
                Shard shard = hers.remove(message.id());
                if (shard != null) {
                    OwlShardFx.crumbled(shard.point(clock), shard.way(clock));
                }
                if (hers.isEmpty()) {
                    SHARDS.remove(message.playerId());
                }
            }
            default -> {
            }
        }
    }

    /** Every spear out, of every player. */
    public static List<Shard> all() {
        List<Shard> out = new ArrayList<>();
        for (Map<Integer, Shard> hers : SHARDS.values()) {
            out.addAll(hers.values());
        }
        return out;
    }

    public static boolean any() {
        return !SHARDS.isEmpty();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SHARDS.clear();
    }
}
