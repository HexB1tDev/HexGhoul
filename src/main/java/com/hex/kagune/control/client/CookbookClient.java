package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Home Living (1.3.41): Touka's cookbook on the client: a right click with it in hand opens it ({@link CookbookScreen}, where it was last
 * left); its tooltip says whose it is.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class CookbookClient {
    private CookbookClient() {
    }

    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.m_91087_();
        if (player != minecraft.f_91074_ || player.m_9236_() == null || !player.m_9236_().f_46443_ || !ControlItems.cookbook(player.m_21205_())) {
            return;
        }
        minecraft.m_91152_(new CookbookScreen());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (ControlItems.cookbook(event.getItemStack())) {
            event.getToolTip().add(Component.m_237115_("item.hexkagune.ghoul_cookbook.about"));
        }
    }
}
