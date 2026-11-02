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
        return GhoulTank.taken(KaguneProfile.mastery(player), GhoulMeatServer.eaten(player), form);
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
