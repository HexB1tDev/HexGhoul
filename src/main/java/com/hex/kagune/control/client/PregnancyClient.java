package com.hex.kagune.control.client;

import com.hex.kagune.control.family.Pregnancy;
import com.hex.kagune.control.network.S2CPregnancy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 1.3.10: every woman this client knows to be carrying a child (by her UUID), and how much her belly shows. */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PregnancyClient {
    private static final Map<UUID, Long> CONCEIVED = new HashMap<>();

    private PregnancyClient() {
    }

    public static void accept(S2CPregnancy message) {
        if (message.conceived() == Long.MIN_VALUE) {
            CONCEIVED.remove(message.body());
        } else {
            CONCEIVED.put(message.body(), message.conceived());
        }
    }

    /** How much of her belly's fullest swell shows (0..1) this frame; 0 if she isn't carrying. */
    public static float belly(Entity her, float partial) {
        Long conceived = her == null ? null : CONCEIVED.get(her.m_20148_());
        Minecraft minecraft = Minecraft.m_91087_();
        if (conceived == null || minecraft.f_91073_ == null) {
            return 0.0F;
        }
        return Pregnancy.belly(Pregnancy.progress(conceived, minecraft.f_91073_.m_46467_()));
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CONCEIVED.clear();
    }
}
