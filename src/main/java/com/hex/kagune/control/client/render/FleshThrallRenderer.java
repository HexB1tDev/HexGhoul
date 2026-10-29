package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.0.34: Peace's flesh creatures are shapes of their own, not a model: {@code PeaceFx} draws every one near after entities with the
 * rest of Peace's flesh. 1.0.38: the game is given vanilla's own renderer that draws nothing for them (a renderer of ours failed to
 * be made in some setups, and a creature with no renderer at all crashes the game the moment it is in view). 1.2.1: it still had none in
 * some setups: it is registered with the ghoul NPC's too, and mended as one joins the world if there is still none ({@link ThrallRenderGuard}).
 */
public final class FleshThrallRenderer {
    private FleshThrallRenderer() {
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ControlEntities.THRALL.get(), context -> new NoopRenderer<>(context));
            // 1.2.1: said in the log, so a setup without one can be told from one where this never ran (ThrallRenderGuard mends it).
            com.hex.kagune.KaguneMod.LOGGER.info("HexKagune: registered the renderer of Peace's flesh creatures");
        }
    }
}
