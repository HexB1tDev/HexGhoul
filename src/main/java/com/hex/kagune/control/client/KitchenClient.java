package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.GhoulMeat;
import com.hex.kagune.control.kitchen.Dish;
import com.hex.kagune.control.kitchen.Dishes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Home Living (1.3.40): the ghoul kitchen on the client. A dish's tooltip: its line about it; "Contains ghoul meat" (and how many pieces,
 * and for a ghoul what they will add, as Ghoul Meat's own tooltip says; for a human, that it can't be kept down) and "Contains human
 * meat"; and each effect it gives, with its level and how long. And Clear Sight's fog: under water, in lava or powder snow, the fog is
 * pushed back ({@link Dishes#CLEAR_SIGHT_FAR}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KitchenClient {
    private KitchenClient() {
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        Dish dish = ControlItems.dish(event.getItemStack());
        if (dish == null) {
            return;
        }
        List<Component> lines = event.getToolTip();
        lines.add(Component.m_237115_("item.hexkagune." + dish.id() + ".about"));
        if (dish.ghoulMeat() > 0) {
            lines.add(dish.ghoulMeat() == 1 ? Component.m_237115_("tooltip.hexkagune.kitchen.ghoul_meat")
                : Component.m_237110_("tooltip.hexkagune.kitchen.ghoul_meat_pieces", dish.ghoulMeat()));
            if (!KaguneClientState.self().unlocked()) {
                lines.add(Component.m_237115_("item.hexkagune.ghoul_meat.human"));
            } else {
                int eaten = GhoulMeatClient.eaten();
                lines.add(Component.m_237110_("tooltip.hexkagune.kitchen.grows", GhoulMeat.percent(GhoulMeat.strength(eaten + dish.ghoulMeat())),
                    GhoulMeat.times(GhoulMeat.endurance(eaten + dish.ghoulMeat()))));
            }
        }
        if (dish.humanMeat()) {
            lines.add(Component.m_237115_("tooltip.hexkagune.kitchen.human_meat"));
        }
        for (Dish.Effect effect : dish.effects()) {
            String key = (Dishes.kitchenEffect(effect.key()) ? "effect.hexkagune." : "effect.minecraft.") + effect.key();
            lines.add(Component.m_237110_("tooltip.hexkagune.kitchen.effect", Component.m_237115_(key), Dishes.level(effect.amplifier()),
                Dishes.time(effect.ticks())));
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        MobEffect clear = ControlItems.kitchenEffect("clear_sight");
        if (player == null || clear == null || !player.m_21023_(clear) || event.getType() == FogType.NONE) {
            return;
        }
        event.setFarPlaneDistance(event.getFarPlaneDistance() * Dishes.CLEAR_SIGHT_FAR);
        event.setCanceled(true);
    }
}
