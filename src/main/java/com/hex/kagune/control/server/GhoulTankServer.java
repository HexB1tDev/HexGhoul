package com.hex.kagune.control.server;

import com.hex.kagune.control.GhoulTank;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.0: a ghoul player is a living tank ({@link GhoulTank}): every blow it takes is cut by its mastery, the ghoul meat it has devoured and
 * the form it is in, last of everything that changes a blow (so nothing scales it back up). Mastery and forms no longer give it health.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulTankServer {
    private GhoulTankServer() {
    }

    /** What {@code player} takes of a blow now (1 for a human). */
    public static double taken(ServerPlayer player) {
        if (!KaguneProfile.isUnlocked(player)) {
            return 1.0;
        }
        boolean out = KaguneProfile.isActive(player);
        double form = GhoulTank.form(out, KaguneProfile.form(player).ordinal(), Sightings.kakuja(player),
            Sightings.finalOf(player) != com.hex.kagune.control.npc.FinalSight.NONE);
        return GhoulTank.taken(KaguneProfile.mastery(player), GhoulMeatServer.eaten(player), unlocked(player), form);
    }

    /** 2.3.6: the share of its path's unlocks (and the shared eye fight's) {@code player} has opened. */
    static double unlocked(ServerPlayer player) {
        com.hex.kagune.control.KagunePath path = PathServer.path(player);
        com.hex.kagune.control.quest.QuestLog log = QuestServer.log(player);
        int all = 0;
        int open = 0;
        for (com.hex.kagune.control.quest.Gate gate : com.hex.kagune.control.quest.Gate.values()) {
            if (gate.path() == null || gate.path() == path) {
                all++;
                open += log.open(gate) ? 1 : 0;
            }
        }
        return all == 0 ? 0.0 : open / (double)all;
    }

    /** 2.3.6: the extra health, kept on its max health while it is a ghoul (looked at every two seconds). */
    static final java.util.UUID HEALTH = java.util.UUID.fromString("6b0d2f5e-91c4-4b8e-a7d3-2f1c9e4a5b77");
    private static final java.util.Map<java.util.UUID, Double> GIVEN = new java.util.HashMap<>();

    @SubscribeEvent
    public static void playerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
            || (player.f_19797_ + player.m_19879_()) % 40 != 0) {
            return;
        }
        net.minecraft.world.entity.ai.attributes.AttributeInstance health = player.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22276_);
        if (health == null) {
            return;
        }
        double extra = KaguneProfile.isUnlocked(player)
            ? Math.round(GhoulTank.health(KaguneProfile.mastery(player), GhoulMeatServer.eaten(player), unlocked(player))) : 0.0;
        Double before = GIVEN.get(player.m_20148_());
        if (before != null && before == extra && (extra == 0.0 || health.m_22111_(HEALTH) != null)) {
            return;
        }
        health.m_22120_(HEALTH);
        if (extra > 0.0) {
            health.m_22118_(new net.minecraft.world.entity.ai.attributes.AttributeModifier(HEALTH, "Ghoul's health", extra,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        }
        GIVEN.put(player.m_20148_(), extra);
        if (player.m_21223_() > player.m_21233_()) {
            player.m_21153_(player.m_21233_());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F
            || GhoulTank.bypasses(event.getSource().m_19385_())) {
            return;
        }
        double taken = taken(player);
        if (taken < 1.0) {
            event.setAmount((float)(event.getAmount() * taken));
        }
    }
}
