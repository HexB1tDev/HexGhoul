package com.hex.kagune.control.client;

import com.hex.kagune.control.pale.WatchRules;
import com.hex.kagune.control.network.S2CPaleWatch;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: the client's store of every Pale player's Watch This tentacles (fed only by the server, {@link S2CPaleWatch}), each
 * timed from when it began on {@link PaleClient#clock}, so every client moves them alike.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class WatchClient {
    /**
     * One tentacle as the server last said: what it is doing, when it began here (PaleClient's clock), what it strikes or carries,
     * its lunge, and its point (a carry: where the chest went on the beak in her frame then, {@code yaw}; otherwise: the world).
     */
    public record Tentacle(WatchRules.Phase phase, double began, long startedAt, int targetId, int lunge, float yaw, double x, double y, double z) {
        public static final Tentacle NONE = new Tentacle(WatchRules.Phase.NONE, 0.0, Long.MIN_VALUE, 0, 0, 0.0F, 0.0, 0.0, 0.0);

        /** Ticks into it at {@code clock} (0 before it begins). */
        public double age(double clock) {
            return Math.max(0.0, clock - this.began);
        }
    }

    /** By player, the right tentacle then the left. */
    private static final Map<UUID, Tentacle[]> TENTACLES = new HashMap<>();

    private WatchClient() {
    }

    public static void accept(S2CPaleWatch message) {
        Minecraft minecraft = Minecraft.m_91087_();
        // As a move is: from now, or that far in when it began a while before (or later: the burst, a few ticks on).
        long behind = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_() - message.startedAt();
        double began = PaleClient.clock(0.0F) - (Math.abs(behind) > PaleClient.JUST_BEGUN || behind < 0L ? behind : 0L);
        Tentacle[] both = TENTACLES.computeIfAbsent(message.playerId(), ignored -> new Tentacle[] {Tentacle.NONE, Tentacle.NONE});
        int index = message.side() > 0 ? 0 : 1;
        WatchRules.Phase phase = WatchRules.Phase.byOrdinal(message.phase());
        both[index] = new Tentacle(phase, began, message.startedAt(), message.targetId(), message.lunge(), message.yaw(), message.x(),
            message.y(), message.z());
        if (both[0].phase() == WatchRules.Phase.NONE && both[1].phase() == WatchRules.Phase.NONE) {
            TENTACLES.remove(message.playerId());
        }
    }

    /**
     * {@code player}'s tentacle on {@code side} (1 her right, -1 her left). 1.3.6: anyone's, by their UUID (a player's is their
     * profile's; an NPC with the kit, Eto, is sent by hers).
     */
    public static Tentacle get(Entity player, int side) {
        Tentacle[] both = player == null ? null : TENTACLES.get(player.m_20148_());
        return both == null ? Tentacle.NONE : both[side > 0 ? 0 : 1];
    }

    /** Whether {@code player} has her tentacles out at all. */
    public static boolean out(Entity player) {
        return player != null && TENTACLES.containsKey(player.m_20148_());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TENTACLES.clear();
    }
}
