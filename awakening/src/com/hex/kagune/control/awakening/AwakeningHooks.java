package com.hex.kagune.control.awakening;

import com.hex.kagune.control.DragonLock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Startup of the Dragon (Kakuja V2): its own network channel, and the lock that the original kagune actions and every action
 * packet of the expansion ask while it rampages ({@link DragonLock}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AwakeningHooks {
    private AwakeningHooks() {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        AwakeningNetwork.init();
        DragonLock.install(DragonSessions::isAwakening);
        // 1.0.6: War's abilities, for the final kakuja (KingServer can't name the awakening's classes).
        com.hex.kagune.control.server.KingServer.installKit(WarKit.INSTANCE);
        com.hex.kagune.control.server.KingServer.installKit(com.hex.kagune.control.king.KingForm.PEACE, PeaceKit.INSTANCE); // 1.0.16
    }
}
