package com.hex.kagune.control;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Called from the end of {@code KaguneMod}'s constructor (bytecode hook), next to the original network init. */
public final class KaguneControlBootstrap {
    private KaguneControlBootstrap() {
    }

    public static void init(FMLJavaModLoadingContext context) {
        try {
            ControlConfig.register();
        } catch (Throwable failure) {
            // Gameplay keeps working with defaults; only file-based tuning is lost.
            KaguneMod.LOGGER.error("HexKagune control configuration could not be registered; using defaults", failure);
        }
        ControlNetwork.init();
        // 7.0.1: the expansion's own items (Ghoul Meat).
        ControlItems.register(context.getModEventBus());
        // 7.0.2: the expansion's own entities (the ghoul NPC).
        ControlEntities.register(context.getModEventBus());
        KaguneMod.LOGGER.info("HexKagune kagune control, instinct and combat expansion loaded");
    }
}
