package com.hex.kagune.control.server;

import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.0.0: a kagune run dry. When its energy (RC) reaches 0 the kagune goes back in and can't be brought out again for ten
 * seconds: only the eyes stay. {@link PathServer#toggle} asks {@link #locked} before letting it out.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KaguneLock {
    /** How long a spent kagune stays in (ticks). */
    public static final int TICKS = 200;

    private static final Map<UUID, Long> UNTIL = new HashMap<>();
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private KaguneLock() {
    }

    /** Seconds left before {@code player}'s kagune can come out again (0: it can). */
    public static int locked(ServerPlayer player) {
        Long until = UNTIL.get(player.m_20148_());
        long now = player.m_9236_().m_46467_();
        if (until == null || now >= until) {
            return 0;
        }
        return (int)Math.ceil((until - now) / 20.0);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !KaguneProfile.isUnlocked(player)) {
            return;
        }
        UUID id = player.m_20148_();
        boolean active = KaguneProfile.isActive(player);
        boolean wasActive = active ? !ACTIVE.add(id) : ACTIVE.remove(id);
        if (!(KaguneProfile.energy(player) <= 0.001F) || !(active || wasActive) || locked(player) > 0) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        UNTIL.put(id, now + TICKS);
        if (active) {
            KaguneServerController.toggleLegacy(player); // the kagune goes back in
            ACTIVE.remove(id);
        }
        player.m_5661_(Component.m_237110_("message.hexkagune.kagune_spent", TICKS / 20), true);
    }

    /** 1.0.23: run dry some other way (a final kakuja falling away): the same wait before anything comes out again. */
    public static void spend(ServerPlayer player) {
        UNTIL.put(player.m_20148_(), player.m_9236_().m_46467_() + TICKS);
    }

    /** 6.2.0: the wait is over now (Eto standing up in her kagune after her death that does not take). */
    static void clear(ServerPlayer player) {
        UNTIL.remove(player.m_20148_());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().m_20148_());
    }
}
