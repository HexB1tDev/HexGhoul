package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.NpcMemory;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.2: each player's {@link NpcMemory}, kept in their profile (so it goes with them through death) and read once while it is
 * unchanged there.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class NpcMemoryServer {
    static final String KEY = "HexNpcMemory";

    private static final class Cached {
        String saved;
        NpcMemory memory;
    }

    private static final Map<UUID, Cached> CACHE = new HashMap<>();

    private NpcMemoryServer() {
    }

    /** {@code player}'s memory: changes to it are kept by {@link #save}. */
    public static NpcMemory of(ServerPlayer player) {
        Cached cached = CACHE.computeIfAbsent(player.m_20148_(), id -> new Cached());
        String stored = KaguneProfile.root(player).m_128461_(KEY);
        if (cached.memory == null || !stored.equals(cached.saved)) {
            cached.memory = NpcMemory.load(stored);
            cached.saved = stored;
        }
        return cached.memory;
    }

    public static void save(ServerPlayer player) {
        Cached cached = CACHE.get(player.m_20148_());
        if (cached == null || cached.memory == null) {
            return;
        }
        cached.saved = cached.memory.save();
        KaguneProfile.root(player).m_128359_(KEY, cached.saved);
    }

    public static long get(ServerPlayer player, String npc, String name, long otherwise) {
        return of(player).get(npc, name, otherwise);
    }

    public static void set(ServerPlayer player, String npc, String name, long value) {
        of(player).set(npc, name, value);
        save(player);
    }

    public static void add(ServerPlayer player, String npc, String name, long amount) {
        of(player).add(npc, name, amount);
        save(player);
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        CACHE.remove(event.getEntity().m_20148_());
    }
}
