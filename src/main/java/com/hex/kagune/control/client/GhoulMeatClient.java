package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.GhoulMeat;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.1: Ghoul Meat on the client: how many pieces the player at the keys has devoured ({@code S2CGhoulMeat}), and the item's
 * tooltip: what it does, what they have from it so far, and what the next piece adds.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class GhoulMeatClient {
    private static int eaten;

    private GhoulMeatClient() {
    }

    public static void accept(int count) {
        eaten = Math.max(0, count);
    }

    public static int eaten() {
        return eaten;
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!ControlItems.ghoulMeat(event.getItemStack())) {
            return;
        }
        List<Component> lines = event.getToolTip();
        lines.add(Component.m_237115_("item.hexkagune.ghoul_meat.lore"));
        if (!KaguneClientState.self().unlocked()) {
            lines.add(Component.m_237115_("item.hexkagune.ghoul_meat.human"));
            return;
        }
        lines.add(Component.m_237110_("item.hexkagune.ghoul_meat.eaten", eaten, GhoulMeat.percent(GhoulMeat.strength(eaten)),
            GhoulMeat.times(GhoulMeat.endurance(eaten))));
        lines.add(Component.m_237110_("item.hexkagune.ghoul_meat.next", GhoulMeat.percent(GhoulMeat.strength(eaten + 1)),
            GhoulMeat.times(GhoulMeat.endurance(eaten + 1))));
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        eaten = 0;
    }
}
