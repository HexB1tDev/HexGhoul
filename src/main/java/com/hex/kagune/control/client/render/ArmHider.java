package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.ArmClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: while her Mutated Arm is out, her right hand in first person is not vanilla's (held low at the side of the view): her own
 * arm is drawn pointed out where the flesh goes, the flesh coming out of her hand ({@link EtoFleshRenderer}). Seen from outside her
 * arm is always drawn: the flesh bursts out of it. 6.2.0: nor are either of vanilla's hands drawn while her own arms are, through
 * her Speed Blitz and the Big Punch. 1.0.15: nor while War's own arms are drawn in first person through his moves.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ArmHider {
    private ArmHider() {
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && ArmClient.out(Minecraft.m_91087_().f_91074_)) {
            event.setCanceled(true);
        }
        // 6.2.0: through her strikes and the Big Punch her own two arms are drawn as third person poses them: no hand of vanilla's.
        if (EtoFleshRenderer.ownArmsShown()) {
            event.setCanceled(true);
        }
    }
}
