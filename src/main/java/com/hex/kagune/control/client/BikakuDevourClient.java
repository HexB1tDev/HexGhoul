package com.hex.kagune.control.client;

import com.hex.kagune.control.bikaku.GrandDevour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.6.3, Grand Devouring on every client: a body being devoured is drawn until the jaws have shut on it and it is drawn up out of sight
 * ({@link GrandDevour#shown}), then not at all (it is inside the worm); a player being devoured sees no hands of his own (whatever was in
 * them was taken out) and his movement keys do nothing (he is held, stunned).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuDevourClient {
    private BikakuDevourClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void hideBody(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        double age = BikakuClient.devoured(entity.m_19879_(), PaleClient.clock(event.getPartialTick()));
        if (age >= 0.0 && !GrandDevour.shown(age)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hideHands(net.minecraftforge.client.event.RenderHandEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ != null && BikakuClient.devoured(minecraft.f_91074_.m_19879_(), PaleClient.clock(minecraft.m_91296_())) >= 0.0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer player && BikakuClient.devoured(player.m_19879_(), PaleClient.clock(0.0F)) >= 0.0) {
            Input input = event.getInput();
            input.f_108567_ = 0.0F;
            input.f_108566_ = 0.0F;
            input.f_108572_ = false;
            input.f_108573_ = false;
        }
    }
}
