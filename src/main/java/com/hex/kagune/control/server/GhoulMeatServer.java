package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.GhoulMeat;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGhoulMeat;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.1: Ghoul Meat on the server ({@link GhoulMeat}). How many pieces a ghoul has devoured is kept in its profile root (so death
 * carries it; a kagune reset clears it). Each one eaten makes every blow it lands harder ({@link #hurt}) and its RC last longer
 * ({@link #endurance}, divided out of every cost in {@link SharedBar}). The owner's client hears the count for the tooltip.
 * A human eats it and is sick.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulMeatServer {
    private GhoulMeatServer() {
    }

    /** Pieces devoured. */
    public static int eaten(ServerPlayer player) {
        return Math.max(0, KaguneProfile.root(player).m_128451_(GhoulMeat.KEY));
    }

    static void setEaten(ServerPlayer player, int eaten) {
        KaguneProfile.root(player).m_128405_(GhoulMeat.KEY, Math.max(0, eaten));
        sync(player);
    }

    /** What every RC cost of this player's is divided by (1 for a human, or a ghoul who has eaten none). */
    public static float endurance(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) ? GhoulMeat.endurance(eaten(player)) : 1.0F;
    }

    /** What every blow of this player's is multiplied by. */
    public static float strength(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) ? GhoulMeat.strength(eaten(player)) : 1.0F;
    }

    static void sync(ServerPlayer player) {
        ControlNetwork.sendTo(player, new S2CGhoulMeat(eaten(player)));
    }

    /** Swallowed: a ghoul's body keeps it for good; a human's can't keep it down. */
    @SubscribeEvent
    public static void finished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ControlItems.ghoulMeat(event.getItem())) {
            return;
        }
        if (!KaguneProfile.isUnlocked(player)) {
            player.m_147207_(new MobEffectInstance(MobEffects.f_19604_, GhoulMeat.SICK_TICKS, 0, false, true, true), player);
            player.m_147207_(new MobEffectInstance(MobEffects.f_19614_, GhoulMeat.SICK_TICKS / 3, 0, false, true, true), player);
            player.m_147207_(new MobEffectInstance(MobEffects.f_19612_, GhoulMeat.SICK_TICKS, 1, false, true, true), player);
            player.m_5661_(Component.m_237115_("message.hexkagune.ghoul_meat.human"), true);
            return;
        }
        int eaten = eaten(player) + 1;
        setEaten(player, eaten);
        player.m_5661_(Component.m_237110_("message.hexkagune.ghoul_meat.eaten", eaten, GhoulMeat.percent(GhoulMeat.strength(eaten)),
            GhoulMeat.times(GhoulMeat.endurance(eaten))), true);
        // The RC taking hold: a heartbeat through the body.
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.warden.heartbeat", 0.9F, 0.85F);
    }

    /** Every blow a ghoul lands, harder with every piece it has eaten. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hurt(LivingHurtEvent event) {
        if (event.getSource().m_7639_() instanceof ServerPlayer attacker && attacker != event.getEntity() && event.getAmount() > 0.0F) {
            float strength = strength(attacker);
            if (strength > 1.0F) {
                event.setAmount(event.getAmount() * strength);
            }
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
