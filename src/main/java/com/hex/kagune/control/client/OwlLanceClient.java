package com.hex.kagune.control.client;

import com.hex.kagune.control.eto.OwlLances;
import com.hex.kagune.control.network.S2COwlLance;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: the client's store of every Owl's Watch This lances (fed only by the server, {@link S2COwlLance}), each timed from when it
 * began on {@link EtoClient#clock}, so every client moves them alike.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class OwlLanceClient {
    /** One lance as the server last said: what it is doing, when it began here (EtoClient's clock), at what, its dive, its point (the world). */
    public record Lance(OwlLances.Phase phase, double began, long startedAt, int targetId, int dive, double x, double y, double z) {
        public static final Lance NONE = new Lance(OwlLances.Phase.NONE, 0.0, Long.MIN_VALUE, 0, 0, 0.0, 0.0, 0.0);

        /** Ticks into it at {@code clock} (0 before it begins). */
        public double age(double clock) {
            return Math.max(0.0, clock - this.began);
        }
    }

    /** By player, each lance by its index. */
    private static final Map<UUID, Lance[]> LANCES = new HashMap<>();

    private OwlLanceClient() {
    }

    public static void accept(S2COwlLance message) {
        if (message.index() < 0 || message.index() >= OwlLances.COUNT) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        // As a move is: from now, or that far in when it began a while before (or later: the burst, a few ticks on).
        long behind = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_() - message.startedAt();
        double began = EtoClient.clock(0.0F) - (Math.abs(behind) > EtoClient.JUST_BEGUN || behind < 0L ? behind : 0L);
        Lance[] all = LANCES.computeIfAbsent(message.playerId(), ignored -> {
            Lance[] none = new Lance[OwlLances.COUNT];
            java.util.Arrays.fill(none, Lance.NONE);
            return none;
        });
        all[message.index()] = new Lance(OwlLances.Phase.byOrdinal(message.phase()), began, message.startedAt(), message.targetId(), message.dive(),
            message.x(), message.y(), message.z());
        for (Lance lance : all) {
            if (lance.phase() != OwlLances.Phase.NONE) {
                return;
            }
        }
        LANCES.remove(message.playerId());
    }

    /** {@code player}'s lance {@code index}. */
    public static Lance get(Player player, int index) {
        Lance[] all = player == null ? null : LANCES.get(player.m_36316_().getId());
        return all == null ? Lance.NONE : all[index];
    }

    /** Whether {@code player} has her lances out at all. */
    public static boolean out(Player player) {
        return player != null && LANCES.containsKey(player.m_36316_().getId());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LANCES.clear();
    }
}
