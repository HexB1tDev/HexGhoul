package com.hex.kagune.control.server;

import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.TendrilMode;
import com.hex.kagune.network.KaguneNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rinkaku's first kakuja is the kagune's, not his (5.0.10): its centipede legs hunt, block and fight on their own
 * ({@link CentipedeHunt}, the original's centipede block), and he can only walk. Every move key does nothing in it: M1, Rapid
 * Strike, the grapple, grab, throw, crush, lift, slam and guard ({@link #refuse}, called by each of their trampolines). K, R,
 * Alt + R and the menu still work, so he can leave it. Whatever his tails were doing when it came out is let go
 * ({@link #tick}): bodies he held are dropped, a grapple lets go and his guard comes down.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class RinkakuAutonomy {
    /** A refused key says why at most this often. */
    private static final long NOTICE_TICKS = 60L;
    private static final Map<UUID, Long> NOTICED = new HashMap<>();

    private RinkakuAutonomy() {
    }

    /** True while this player is a Rinkaku in the first kakuja. */
    public static boolean active(ServerPlayer player) {
        return !UkakuServer.isUkaku(player) && RinkakuForms.autonomous(KaguneProfile.form(player));
    }

    /** True when a move key must do nothing because he is in the first kakuja; says so on the action bar every few seconds. */
    static boolean refuse(ServerPlayer player) {
        if (!active(player)) {
            return false;
        }
        long now = player.m_9236_().m_46467_();
        Long last = NOTICED.get(player.m_20148_());
        if (last == null || now - last >= NOTICE_TICKS || now < last) {
            NOTICED.put(player.m_20148_(), now);
            player.m_5661_(Component.m_237115_("message.hexkagune.kakuja_autonomous"), true);
        }
        return true;
    }

    /** Each server tick after the original controller: lets go of what his tails were doing when the first kakuja took over. */
    static void tick(ServerPlayer player, KaguneRuntime.State state, long now) {
        if (!KaguneProfile.isActive(player) || !active(player)) {
            return;
        }
        boolean changed = false;
        if (state.guarding) {
            state.guarding = false;
            changed = true;
        }
        if (state.hasManualHeldEntities()) {
            // Only what he grabbed: a body the legs caught themselves stays theirs.
            ServerLevel level = player.m_284548_();
            for (int slot = 0; slot < state.heldEntityIds.length; slot++) {
                if (state.heldEntityIds[slot] != 0 && !state.autonomousCentipedeHeld[slot]) {
                    KaguneServerController.releaseHeldSlot(level, state, slot);
                }
            }
            state.pendingGrabSlot = -1;
            state.selectedHeldSlot = -1;
            changed = true;
        }
        if (handling(state.mode)) {
            KaguneServerController.returnToHoldingOrRetract(player, state, now);
            changed = true;
        }
        if (changed) {
            KaguneNetwork.broadcast(player);
        }
    }

    /**
     * The modes he drives that would wait on him: his grab and what follows it (reaching, holding, crushing, lifting, slamming)
     * and the grapple (its hold waits for J to launch, which he can't press in the first kakuja).
     */
    static boolean handling(TendrilMode mode) {
        return mode == TendrilMode.REACH_ENTITY || mode == TendrilMode.REACH_ITEM || mode == TendrilMode.REACH_BLOCK || mode == TendrilMode.HOLD
            || mode == TendrilMode.CRUSH || mode == TendrilMode.LIFT || mode == TendrilMode.SLAM || mode == TendrilMode.SLINGSHOT_REACH
            || mode == TendrilMode.SLINGSHOT_HOLD;
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        NOTICED.remove(event.getEntity().m_20148_());
    }
}
