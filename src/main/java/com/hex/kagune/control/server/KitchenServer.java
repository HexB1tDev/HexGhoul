package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.kitchen.Dish;
import com.hex.kagune.control.kitchen.Dishes;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Home Living (1.3.40): the ghoul kitchen on the server. A dish swallowed ({@link #finished}): the ghoul meat in it devoured, piece by
 * piece, for good ({@link GhoulMeatServer#devour}: a human who eats it is sick instead, and gets nothing else from it); its effects
 * given; its bowl or bottle handed back. And what the kitchen's own effects do, every tick ({@link #tick}): Refreshed lets RC flow back,
 * Replenished closes wounds at no cost to the flesh reserve, Content lets the frenzy ebb (and the ones you love warm to you faster:
 * {@link #warmth}, in QuestServer), Clear Sight takes off blindness, darkness and nausea (and the client clears the fog).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KitchenServer {
    private KitchenServer() {
    }

    @SubscribeEvent
    public static void finished(LivingEntityUseItemEvent.Finish event) {
        Dish dish = ControlItems.dish(event.getItem());
        if (dish == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean kept = dish.ghoulMeat() <= 0 || GhoulMeatServer.devour(player, dish.ghoulMeat());
        if (kept) {
            give(player, dish);
        }
        if (dish.container() != null && !player.m_7500_()) {
            Item vessel = ControlItems.vanilla(dish.container());
            if (vessel != null) {
                ItemStack back = new ItemStack(vessel, 1);
                if (event.getResultStack().m_41619_()) {
                    event.setResultStack(back);
                } else if (!player.m_36356_(back)) {
                    player.m_36176_(back, false);
                }
            }
        }
    }

    /** {@code dish}'s effects on {@code eater}. */
    public static void give(LivingEntity eater, Dish dish) {
        for (Dish.Effect effect : dish.effects()) {
            MobEffect kind = ControlItems.effect(effect.key());
            if (kind != null) {
                eater.m_147207_(new MobEffectInstance(kind, effect.ticks(), effect.amplifier(), false, true, true), eater);
            }
        }
    }

    /** The amplifier of the kitchen effect {@code key} on {@code body}, or -1 if it hasn't it. */
    static int level(LivingEntity body, String key) {
        MobEffect effect = ControlItems.kitchenEffect(key);
        if (effect == null || !body.m_21023_(effect)) {
            return -1;
        }
        MobEffectInstance instance = body.m_21124_(effect);
        return instance == null ? -1 : Math.max(0, instance.m_19564_());
    }

    /** True while {@code player} is Content (their bonds warm faster). */
    public static boolean content(ServerPlayer player) {
        return level(player, "content") >= 0;
    }

    /** A regard gain of {@code gain} as it lands on a player who may be Content ({@link Dishes#warmed}). */
    public static int warmth(ServerPlayer player, int gain) {
        return gain > 0 && content(player) ? Dishes.warmed(gain) : gain;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        if (level(player, "clear_sight") >= 0) {
            for (String key : Dishes.CLEARED) {
                MobEffect bad = ControlItems.effect(key);
                if (bad != null && player.m_21023_(bad)) {
                    player.m_21195_(bad);
                }
            }
        }
        int replenished = level(player, "replenished");
        if (replenished >= 0 && now % Dishes.replenishTicks(replenished) == 0L && player.m_21223_() < player.m_21233_()) {
            player.m_5634_(1.0F);
        }
        if (now % 20L != 0L) {
            return;
        }
        int refreshed = level(player, "refreshed");
        if (refreshed >= 0 && KaguneProfile.isUnlocked(player) && !player.m_7500_()) {
            float energy = SharedBar.energy(player);
            if (energy < KaguneProfile.MAX_ENERGY) {
                SharedBar.setEnergy(player, Math.min(KaguneProfile.MAX_ENERGY, energy + Dishes.REFRESH_RC * (refreshed + 1)));
            }
        }
        int content = level(player, "content");
        if (content >= 0 && KaguneProfile.isUnlocked(player)) {
            ControlServer.calm(player, Dishes.CONTENT_CALM * (content + 1));
        }
    }
}
