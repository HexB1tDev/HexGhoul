package com.hex.kagune.control.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * The kagune tearing out, played by every client for every player it sees (the server's old single rip is gone). Rinkaku's
 * tendrils each play the tear as they come out, one every few ticks, and each new one cuts the last off; Ukaku's wings
 * ignite once for both. Driven by the original's own per-limb manifestation pulse ({@code KaguneManifestationEffects}).
 */
public final class ManifestSounds {
    private static final SoundEvent RINKAKU = SoundEvent.m_262824_(ResourceLocation.m_214293_("hexkagune", "manifest_rinkaku"));
    private static final SoundEvent UKAKU = SoundEvent.m_262824_(ResourceLocation.m_214293_("hexkagune", "manifest_ukaku"));
    private static final Map<UUID, Tear> PLAYING = new HashMap<>();

    private ManifestSounds() {
    }

    /** 6.1.0: the kakugan turning in Pale's one eye as her base form wakes (nothing tears out of her). */
    private static final SoundEvent EYE_OPEN = SoundEvent.m_262824_(ResourceLocation.m_214293_("hexkagune", "eye_open"));

    /** 6.1.0: Pale's base form wakes: her eye turns, with the ghoul mode's own sound, a little softer and lower. */
    public static void pale(AbstractClientPlayer player) {
        PLAYING.values().removeIf(Tear::m_7801_);
        UUID id = player.m_36316_().getId();
        Tear previous = PLAYING.remove(id);
        if (previous != null) {
            previous.end();
        }
        Tear tear = new Tear(player, EYE_OPEN, 0.8F, 0.9F);
        PLAYING.put(id, tear);
        Minecraft.m_91087_().m_91106_().m_120367_(tear);
    }

    /** One limb came out ({@code opening}: the first of this manifestation). */
    public static void limb(AbstractClientPlayer player, boolean opening, boolean ukaku) {
        if (ukaku && !opening) {
            return;
        }
        PLAYING.values().removeIf(Tear::m_7801_);
        UUID id = player.m_36316_().getId();
        Tear previous = PLAYING.remove(id);
        if (previous != null) {
            previous.end();
        }
        // A little pitch drift so a fast run of tendrils doesn't sound like one sample on repeat.
        float pitch = ukaku ? 1.0F : 0.94F + 0.12F * player.m_217043_().m_188501_();
        Tear tear = new Tear(player, ukaku ? UKAKU : RINKAKU, ukaku ? 1.0F : 0.95F, pitch);
        PLAYING.put(id, tear);
        Minecraft.m_91087_().m_91106_().m_120367_(tear);
    }

    /** A one-shot that follows the player and can be cut off. */
    private static final class Tear extends AbstractTickableSoundInstance {
        private final AbstractClientPlayer player;

        private Tear(AbstractClientPlayer player, SoundEvent sound, float volume, float pitch) {
            super(sound, SoundSource.PLAYERS, SoundInstance.m_235150_());
            this.player = player;
            this.f_119578_ = false;
            this.f_119579_ = 0;
            this.f_119573_ = volume;
            this.f_119574_ = pitch;
            this.f_119580_ = SoundInstance.Attenuation.LINEAR;
            this.follow();
        }

        private void follow() {
            this.f_119575_ = this.player.m_20185_();
            this.f_119576_ = this.player.m_20186_() + this.player.m_20206_() * 0.6;
            this.f_119577_ = this.player.m_20189_();
        }

        private void end() {
            this.m_119609_();
        }

        @Override
        public void m_7788_() {
            if (this.player.m_213877_() || this.player.m_9236_() != Minecraft.m_91087_().f_91073_) {
                this.m_119609_();
                return;
            }
            this.follow();
        }
    }
}
