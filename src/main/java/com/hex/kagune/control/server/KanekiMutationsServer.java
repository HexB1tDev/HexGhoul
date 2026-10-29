package com.hex.kagune.control.server;

import com.hex.kagune.control.KanekiMutations;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.MutationNode;
import com.hex.kagune.network.KaguneNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Kaneki's cut mutations on the server ({@link KanekiMutations}, 3.0.3): {@code KaguneProfile.hasMutation} is a trampoline
 * here, so a cut one never counts (and the guard is everyone's), and a save that still carries cut ones has them stripped
 * when the player logs in and every few seconds after (a stash coming back from the Ayato path included), so every client's
 * view of the mutations agrees.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KanekiMutationsServer {
    /** The profile's own key for the mutation bits (KaguneProfile). */
    private static final String MUTATIONS = "Mutations";
    private static final long CHECK_TICKS = 40L;

    private KanekiMutationsServer() {
    }

    /** Trampoline target of the original's {@code KaguneProfile.hasMutation}. */
    public static boolean hasMutation(ServerPlayer player, MutationNode node) {
        // 7.0.1: Centipede Body and the Projectile Reflex Organ also need their quests.
        com.hex.kagune.control.quest.Gate gate = com.hex.kagune.control.quest.Gates.kaneki(node);
        return KanekiMutations.has(KaguneProfile.mutationMask(player), node) && (gate == null || QuestServer.open(player, gate));
    }

    /** Takes any cut mutation out of this player's profile; true if there was one. */
    static boolean strip(ServerPlayer player) {
        long mask = KaguneProfile.mutationMask(player);
        long kept = KanekiMutations.strip(mask);
        if (kept == mask) {
            return false;
        }
        CompoundTag root = KaguneProfile.root(player);
        root.m_128356_(MUTATIONS, kept);
        KaguneNetwork.broadcast(player);
        return true;
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            strip(player);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.m_9236_().m_46467_() % CHECK_TICKS == 0L) {
            strip(player);
        }
    }
}
