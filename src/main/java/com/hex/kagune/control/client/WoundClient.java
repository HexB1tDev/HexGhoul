package com.hex.kagune.control.client;

import com.hex.kagune.control.wound.Wound;
import com.hex.kagune.control.wound.WoundBook;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Every player's wounds on this client ({@link WoundBook}), following their health as it is synced: hits tear wounds open,
 * regeneration closes them (3.0.0). Drawn by {@code WoundLayer}.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class WoundClient {
    private static final Map<UUID, WoundBook> BOOKS = new HashMap<>();

    private WoundClient() {
    }

    /** A player's wounds, or null. */
    public static WoundBook book(AbstractClientPlayer player) {
        return player == null ? null : BOOKS.get(player.m_36316_().getId());
    }

    /** A puncture left by one of Ayato's spikes on a player (see {@code AyatoSpikes}). */
    public static void puncture(AbstractClientPlayer player, Wound.Part part, Wound.Face face, double u, double v, int seed) {
        WoundBook book = book(player);
        if (book != null) {
            book.puncture(part, face, u, v, seed);
        }
    }

    /**
     * A blow from Kaneki's tails is landing on {@code player} (3.0.3): the health it costs opens wounds of its shape, the first at
     * {@code spot} (a slash's gash at {@code angle}).
     */
    public static void blow(AbstractClientPlayer player, WoundBook.Blow blow, WoundBook.Spot spot, double angle) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (player == null || level == null) {
            return;
        }
        WoundBook book = BOOKS.computeIfAbsent(player.m_36316_().getId(), ignored -> new WoundBook());
        book.blow(blow, spot, angle, level.m_46467_());
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            BOOKS.clear();
            return;
        }
        long now = level.m_46467_();
        Set<UUID> seen = new HashSet<>();
        for (AbstractClientPlayer player : level.m_6907_()) {
            UUID id = player.m_36316_().getId();
            seen.add(id);
            WoundBook book = BOOKS.computeIfAbsent(id, ignored -> new WoundBook());
            book.slim("slim".equals(player.m_108564_()));
            book.health(player.m_21223_(), player.m_21233_(), player.m_19879_() * 7919 + (int)now, now);
        }
        if (now % 40L == 0L) {
            BOOKS.keySet().retainAll(seen);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BOOKS.clear();
    }
}
