package com.hex.kagune.control.awakening;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Every Dragon currently awake, plus the short grace period after Kaneki emerges. */
public final class DragonSessions {
    /** Five seconds of untouchable footing after control returns. */
    static final long GRACE_TICKS = 100L;
    private static final Map<UUID, DragonSession> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> GRACE = new HashMap<>();

    private DragonSessions() {
    }

    /** True while the player is the Dragon and has no control. */
    public static boolean isAwakening(UUID player) {
        DragonSession session = ACTIVE.get(player);
        return session != null && !session.finished();
    }

    /** True while the player must not be harmed: during the Dragon and the grace period after it. */
    static boolean isProtected(ServerPlayer player) {
        if (isAwakening(player.getUUID())) return true;
        Long until = GRACE.get(player.getUUID());
        if (until == null) return false;
        if (player.serverLevel().getGameTime() < until) return true;
        GRACE.remove(player.getUUID());
        return false;
    }

    static int tickOf(UUID player) {
        DragonSession session = ACTIVE.get(player);
        return session == null ? -1 : session.tick();
    }

    /** Turns the player's death into the Dragon. Returns false if it could not begin. */
    static boolean start(ServerPlayer player) {
        // 1.0.11: Kaneki DNA's alone, whoever asks (the death, or the admin's trigger).
        if (ACTIVE.containsKey(player.getUUID()) || !AwakeningEvents.kanekiPath(player)) return false;
        ServerLevel level = player.serverLevel();
        if (player.getY() < level.getMinBuildHeight()) return false;
        // Kaneki's existing kagune and everything it holds are consumed by the transformation.
        KaguneServerController.release(player, false);
        KaguneProfile.setActive(player, false);
        KaguneRuntime.remove(player.getUUID());
        if (player.isPassenger()) player.stopRiding();
        if (player.isSleeping()) player.stopSleeping();
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        player.fallDistance = 0.0F;
        TerrainProbe probe = new TerrainProbe(level, player.getY());
        double surface = probe.surface(player.getX(), player.getZ());
        double height = player.getY() - surface;
        V3 anchor = new V3(player.getX(), height >= -2.0 && height <= 24.0 ? surface : player.getY(), player.getZ());
        AwakeningProfile.beginDragon(player, anchor);
        ACTIVE.put(player.getUUID(), new DragonSession(player, anchor));
        KaguneNetwork.broadcast(player);
        AwakeningNetwork.sendStatus(player);
        player.displayClientMessage(Component.literal("§4§lSomething is tearing its way out of you."), false);
        return true;
    }

    static void tick() {
        Iterator<Map.Entry<UUID, DragonSession>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            DragonSession session = iterator.next().getValue();
            boolean running;
            try {
                running = session.step();
            } catch (RuntimeException failure) {
                // Never leave a player frozen inside a broken Dragon: end it and give control back.
                KaguneMod.LOGGER.error("Dragon awakening failed; ending it early", failure);
                try {
                    session.finish(true);
                } catch (RuntimeException secondary) {
                    KaguneMod.LOGGER.error("Dragon awakening could not finish cleanly", secondary);
                    session.owner().setNoGravity(false);
                }
                running = false;
            }
            if (!running) {
                iterator.remove();
                afterFinish(session);
            }
        }
        TerrainScars.tickAll();
    }

    private static void afterFinish(DragonSession session) {
        ServerPlayer owner = session.owner();
        GRACE.put(session.ownerId, owner.serverLevel().getGameTime() + GRACE_TICKS);
        AwakeningProfile.clearDragon(owner);
        AwakeningNetwork.sendStatus(owner);
        KaguneNetwork.broadcast(owner);
    }

    /** Ends a player's awakening immediately (disconnect, dimension change, admin reset). */
    static void end(UUID player) {
        DragonSession session = ACTIVE.remove(player);
        if (session != null) {
            session.finish(true);
            afterFinish(session);
        }
    }

    static void endAll() {
        for (UUID player : new ArrayList<>(ACTIVE.keySet())) end(player);
    }

    static void endIn(ServerLevel level) {
        for (Map.Entry<UUID, DragonSession> entry : new ArrayList<>(ACTIVE.entrySet())) {
            if (entry.getValue().level == level) end(entry.getKey());
        }
    }

    /** A Dragon that was interrupted by a crash still awakens the final form on the next login. */
    static void recover(ServerPlayer player) {
        if (AwakeningProfile.stage(player) != AwakeningStage.DRAGON || ACTIVE.containsKey(player.getUUID())) return;
        player.setNoGravity(false);
        player.fallDistance = 0.0F;
        player.clearFire();
        GRACE.put(player.getUUID(), player.serverLevel().getGameTime() + GRACE_TICKS);
        AwakeningProfile.clearDragon(player);
        FinalKakuja.unlock(player);
    }

    static void forget(UUID player) {
        GRACE.remove(player);
    }
}
