package com.hex.kagune.control.client;

import com.hex.kagune.control.network.C2SRegen;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.2.2 (the owner: "as long as you're holding down that key, you start regenerating and your wounds are going to close. But if you don't, the
 * wounds are going to remain present on your body"): the regeneration key ({@link ControlKeys#REGENERATE}, X by default) told to the server as
 * it goes down, every half second while it stays down (so a lost word never leaves it held) and as it comes up. Whatever the form: the eyes
 * shut or open, the kagune out, any kakuja, War, Peace (the abilities panel shows it in each: AbilityGuide.withRegen).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class RegenClient {
    /** How often a held key is told again (ticks). */
    static final int REPEAT = 10;
    private static boolean was;
    private static int ticks;

    private RegenClient() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        boolean down = minecraft.f_91074_ != null && minecraft.f_91080_ == null && ControlKeys.REGENERATE.m_90857_();
        if (minecraft.f_91074_ == null) {
            was = false;
            return;
        }
        ticks++;
        if (down != was || down && ticks % REPEAT == 0) {
            ControlNetwork.CHANNEL.sendToServer(new C2SRegen(down));
        }
        was = down;
    }
}
