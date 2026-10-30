package com.hex.kagune.control.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.0: Eto's surgery, as the one under the knife lives it: the screen fades to black, the cutting is heard in the dark (the mod's own
 * stab, writhing flesh, the gush, the tear; a heartbeat, which the sound pack has none of, is vanilla's), then her voice ("hey are you awake
 * honey?") and the world fades back. Ten seconds; nothing else is drawn over it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class EtoSurgeryOverlay {
    static final double LENGTH = 10.0;
    static final double DARK = 1.5;
    static final double WAKE = 8.5;
    static final String WORDS = "hey are you awake honey?";
    /** What is heard, and when (seconds in): the sound's namespace and path, its volume and pitch. */
    private static final Object[][] CUES = {
        {1.6, "hexkagune", "sfx.stab", 0.9F, 0.9F},
        {2.1, "minecraft", "entity.warden.heartbeat", 1.0F, 1.0F},
        {2.5, "hexkagune", "sfx.writhe", 0.8F, 1.0F},
        {2.9, "minecraft", "entity.warden.heartbeat", 1.0F, 1.1F},
        {3.3, "hexkagune", "sfx.gush", 0.8F, 1.0F},
        {3.7, "minecraft", "entity.warden.heartbeat", 1.0F, 1.2F},
        {4.1, "hexkagune", "manifest_rip", 0.8F, 0.9F},
        {4.5, "minecraft", "entity.warden.heartbeat", 1.0F, 1.2F},
        {5.0, "hexkagune", "sfx.drips", 0.7F, 1.0F},
        {5.6, "minecraft", "entity.warden.heartbeat", 0.9F, 1.0F},
        {6.6, "minecraft", "entity.warden.heartbeat", 0.7F, 0.9F}};

    private static long began = Long.MIN_VALUE;
    private static int played;

    private EtoSurgeryOverlay() {
    }

    public static void begin(boolean ghoul) {
        began = System.nanoTime();
        played = 0;
        Minecraft.m_91087_().m_91152_(null);
    }

    /** How dark the screen is (0..1) {@code t} seconds in. */
    static float dark(double t) {
        if (t < 0.0 || t >= LENGTH) {
            return 0.0F;
        }
        if (t < DARK) {
            return (float)(t / DARK);
        }
        return t < WAKE ? 1.0F : (float)(1.0 - (t - WAKE) / (LENGTH - WAKE));
    }

    /** How clear her words are (0..1) {@code t} seconds in. */
    static float words(double t) {
        if (t < 6.4 || t >= LENGTH - 0.3) {
            return 0.0F;
        }
        return (float)Math.min(1.0, Math.min((t - 6.4) / 0.8, (LENGTH - 0.3 - t) / 0.8));
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (began == Long.MIN_VALUE || !event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type())) {
            return;
        }
        double t = (System.nanoTime() - began) / 1.0E9;
        if (t >= LENGTH) {
            began = Long.MIN_VALUE;
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        while (played < CUES.length && t >= (double)CUES[played][0]) {
            Object[] cue = CUES[played++];
            sound(minecraft, (String)cue[1], (String)cue[2], (Float)cue[3], (Float)cue[4]);
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().m_85445_();
        int height = event.getWindow().m_85446_();
        int alpha = Math.round(255 * dark(t));
        if (alpha > 0) {
            graphics.m_280509_(0, 0, width, height, alpha << 24);
        }
        float clear = words(t);
        if (clear > 0.02F) {
            int a = Math.max(5, Math.round(255 * clear));
            graphics.m_280137_(minecraft.f_91062_, WORDS, width / 2, height / 2 - 4, a << 24 | 0xE8D8DC);
        }
    }

    private static void sound(Minecraft minecraft, String namespace, String path, float volume, float pitch) {
        ResourceLocation id = ResourceLocation.m_214293_(namespace, path);
        if (id == null || minecraft.f_91074_ == null) {
            return;
        }
        minecraft.m_91106_().m_120367_(new Cue(SoundEvent.m_262824_(id), volume, pitch, minecraft.f_91074_.m_20185_(), minecraft.f_91074_.m_20186_() + 1.0,
            minecraft.f_91074_.m_20189_()));
    }

    /** A one-shot where the player lies. */
    private static final class Cue extends AbstractTickableSoundInstance {
        private Cue(SoundEvent sound, float volume, float pitch, double x, double y, double z) {
            super(sound, SoundSource.PLAYERS, SoundInstance.m_235150_());
            this.f_119578_ = false;
            this.f_119579_ = 0;
            this.f_119573_ = volume;
            this.f_119574_ = pitch;
            this.f_119580_ = SoundInstance.Attenuation.LINEAR;
            this.f_119575_ = x;
            this.f_119576_ = y;
            this.f_119577_ = z;
        }

        @Override
        public void m_7788_() {
        }
    }
}
