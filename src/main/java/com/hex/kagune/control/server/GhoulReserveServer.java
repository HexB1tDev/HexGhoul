package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.ControlMath;
import com.hex.kagune.control.GhoulReserve;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGhoulReserve;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The flesh reserve on the server ({@link GhoulReserve}, 3.0.3): kept in the ghoul's profile (full for a new ghoul and after
 * every respawn), vanilla hunger held still under it every tick, and the owner told whenever it moves (the client draws it where
 * the food bar was, {@code ReserveHud}). With the ghoul diet off (server config) nothing changes and the food bar stays.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulReserveServer {
    /** In ten-thousandths, so the kagune's small per-tick sips add up. */
    private static final String KEY = "GhoulReserve";
    private static final float SCALE = 10000.0F;
    private static final Map<UUID, Float> SENT = new HashMap<>();

    private GhoulReserveServer() {
    }

    /** Whether this player lives on the reserve (a ghoul, the diet on, not in creative). */
    public static boolean applies(ServerPlayer player) {
        return ControlConfig.ghoulDiet() && KaguneProfile.isUnlocked(player) && !player.m_7500_();
    }

    public static float get(ServerPlayer player) {
        CompoundTag root = KaguneProfile.root(player);
        return root.m_128441_(KEY) ? GhoulReserve.clamp(root.m_128451_(KEY) / SCALE) : GhoulReserve.MAX;
    }

    public static void set(ServerPlayer player, float reserve) {
        KaguneProfile.root(player).m_128405_(KEY, Math.round(GhoulReserve.clamp(reserve) * SCALE));
    }

    /** Starvation for bloodlust and instinct: from the reserve, or the food bar where the reserve does not apply. */
    public static float hunger(ServerPlayer player) {
        return applies(player) ? GhoulReserve.hunger(get(player)) : ControlMath.starvation(player.m_36324_().m_38702_());
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        boolean on = applies(player);
        if (on) {
            FoodData food = player.m_36324_();
            if (food.m_38702_() != GhoulReserve.FOOD_HELD) {
                food.m_38705_(GhoulReserve.FOOD_HELD);
            }
            if (food.m_38722_() != GhoulReserve.SATURATION_HELD) {
                food.m_38717_(GhoulReserve.SATURATION_HELD);
            }
        }
        float value = on ? get(player) : -1.0F;
        Float sent = SENT.get(player.m_20148_());
        if (sent == null || Math.abs(sent - value) >= 0.05F || value <= 0.0F != sent <= 0.0F || value >= GhoulReserve.MAX != sent >= GhoulReserve.MAX) {
            SENT.put(player.m_20148_(), value);
            ControlNetwork.sendTo(player, new S2CGhoulReserve(value));
            if (on) {
                // 7.0.0: RC is this bar: the profile's RC (menus, HUD) goes with it.
                com.hex.kagune.network.KaguneNetwork.sendTo(player, player);
            }
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            set(player, GhoulReserve.MAX);
            SENT.remove(player.m_20148_());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        SENT.remove(event.getEntity().m_20148_());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().m_20148_());
    }
}
