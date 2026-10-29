package com.hex.kagune.control.server;

import com.hex.kagune.control.EnergyBalance;
import com.hex.kagune.control.GhoulReserve;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.0: RC, healing and food are one bar. Trampoline targets for the profile's {@code energy}, {@code setEnergy},
 * {@code consumeEnergy} and {@code restoreEnergy} (bytecode hooks): while a ghoul lives on the flesh reserve
 * ({@link GhoulReserveServer#applies}) RC is the reserve read on its scale ({@link GhoulReserve#rc}), an ability pays
 * {@link GhoulReserve#RC_SPEND} of its price from it, and nothing refills it but eating (the original's rest-time recovery, and
 * any form's own, is ignored). Without the diet (server config, creative) RC is the original's own store, as before.
 * 1.0.33: mastery takes off everything the bar pays for ({@link EnergyBalance#costScale}: a fifth of the price at 1000), and in
 * creative it pays for nothing: it reads full and stays full.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class SharedBar {
    private SharedBar() {
    }

    public static float energy(ServerPlayer player) {
        if (player.m_7500_()) {
            return KaguneProfile.MAX_ENERGY;
        }
        return GhoulReserveServer.applies(player) ? GhoulReserve.rc(GhoulReserveServer.get(player)) : KaguneProfile.energyLegacy(player);
    }

    public static void setEnergy(ServerPlayer player, float energy) {
        if (GhoulReserveServer.applies(player)) {
            GhoulReserveServer.set(player, GhoulReserve.fromRc(energy));
        } else {
            KaguneProfile.setEnergyLegacy(player, energy);
        }
    }

    public static boolean consumeEnergy(ServerPlayer player, float cost) {
        if (player.m_7500_()) {
            return true;
        }
        // 7.0.1: every piece of Ghoul Meat eaten makes the same RC last longer (GhoulMeat.endurance).
        float paid = price(player, cost);
        return KaguneProfile.consumeEnergyLegacy(player, GhoulReserveServer.applies(player) ? paid * GhoulReserve.RC_SPEND : paid);
    }

    /** Rest-time and form recovery: only the original's own store recovers; the shared bar fills by eating alone. */
    public static void restoreEnergy(ServerPlayer player, float amount) {
        // 1.0.23: nor while a final kakuja is worn: it burns RC (KingRules.UPKEEP) and none comes back until it is off.
        if (!GhoulReserveServer.applies(player) && !KingServer.locks(player.m_20148_())) {
            KaguneProfile.restoreEnergyLegacy(player, amount);
        }
    }

    /** Gives back what an ability that never happened was charged (its share, on the shared bar). */
    public static void refund(ServerPlayer player, float cost) {
        if (cost <= 0.0F) {
            return;
        }
        if (player.m_7500_()) {
            return;
        }
        float paid = price(player, cost); // (what consumeEnergy really took)
        if (GhoulReserveServer.applies(player)) {
            setEnergy(player, energy(player) + paid * GhoulReserve.RC_SPEND);
        } else {
            KaguneProfile.restoreEnergyLegacy(player, paid);
        }
    }

    /** What {@code cost} really takes from the bar: less with mastery, and with every piece of Ghoul Meat eaten. */
    private static float price(ServerPlayer player, float cost) {
        return cost * EnergyBalance.costScale(KaguneProfile.mastery(player)) / GhoulMeatServer.endurance(player);
    }

    /** 1.0.33: in creative the original's store is kept full too, so its HUD and anything reading it straight see a full bar. */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.m_7500_()
            && KaguneProfile.energyLegacy(player) < KaguneProfile.MAX_ENERGY) {
            KaguneProfile.setEnergyLegacy(player, KaguneProfile.MAX_ENERGY);
        }
    }

    /** Flesh torn from prey and swallowed: RC on either store (on the shared bar, it is eating). */
    public static void feed(ServerPlayer player, float energy) {
        if (energy > 0.0F) {
            setEnergy(player, energy(player) + energy);
        }
    }
}
