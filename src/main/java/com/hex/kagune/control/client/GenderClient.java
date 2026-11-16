package com.hex.kagune.control.client;

import com.hex.kagune.control.Gender;
import com.hex.kagune.control.Genders;
import com.hex.kagune.control.network.S2CGender;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 1.3.9: every player's gender as the server says (it changes how they are drawn and boxed), and this player's own. */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class GenderClient {
    private GenderClient() {
    }

    public static void accept(S2CGender message) {
        Genders.put(message.player(), Gender.byOrdinal(message.gender()));
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return;
        }
        for (Player player : minecraft.f_91073_.m_6907_()) {
            if (player.m_20148_().equals(message.player())) {
                player.m_6210_(); // her box and eyes remade here too (her own client moves her own box)
            }
        }
    }

    /** This player's own gender. */
    public static Gender self() {
        Minecraft minecraft = Minecraft.m_91087_();
        return minecraft.f_91074_ == null ? Gender.UNSET : Genders.of(minecraft.f_91074_.m_20148_());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Genders.clear();
    }
}
