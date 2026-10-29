package com.hex.kagune.control.client;

import com.hex.kagune.control.SfxLimit;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 1.2.2: every one of the mod's sounds about to play on this client is let through or not by {@link SfxLimit} (no wall of noise). */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class SfxGuard {
    private static final SfxLimit LIMIT = new SfxLimit();

    private SfxGuard() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void play(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }
        ResourceLocation id = sound.m_7904_();
        if (id == null || !"hexkagune".equals(id.m_135827_())) {
            return;
        }
        if (!LIMIT.allow(id.m_135815_(), sound.m_7772_(), sound.m_7780_(), sound.m_7778_(), System.nanoTime())) {
            event.setSound(null);
        }
    }
}
