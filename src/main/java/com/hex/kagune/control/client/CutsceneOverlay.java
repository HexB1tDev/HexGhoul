package com.hex.kagune.control.client;

import com.hex.kagune.control.network.S2CCutscene;
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
 * 1.3.2 (1.3.0's surgery, made any NPC's): a cutscene as the player lives it ({@code S2CCutscene}). The screen fades to black, what
 * happens is heard in the dark (each script's cues) and told (its captions), then the NPC's own words, and the world fades back.
 * <ul>
 *   <li>The surgery: the cutting (the mod's stab, writhing flesh, the gush, the tear; vanilla's heartbeat), then her words, warm to a
 *       partner, cold to anyone else.</li>
 *   <li>The night: a candle put out, two heartbeats quickening, the dark, and what she says in the morning.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class CutsceneOverlay {
    /** One script: how long it runs (seconds), when it is all black and when it starts to clear, its cues and its captions. */
    record Script(double length, double dark, double wake, Object[][] cues, Object[][] captions, double wordsAt) {
    }

    static final Script SURGERY = new Script(10.0, 1.5, 8.5, new Object[][] {
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
        {6.6, "minecraft", "entity.warden.heartbeat", 0.7F, 0.9F}}, new Object[0][], 6.4);

    static final Script NIGHT = new Script(14.0, 1.6, 12.0, new Object[][] {
        {1.8, "minecraft", "block.candle.extinguish", 0.8F, 1.0F},
        {3.2, "minecraft", "block.wool.step", 0.6F, 0.8F},
        {4.0, "minecraft", "entity.warden.heartbeat", 0.45F, 1.25F},
        {4.6, "minecraft", "entity.warden.heartbeat", 0.45F, 1.35F},
        {5.1, "minecraft", "entity.warden.heartbeat", 0.5F, 1.45F},
        {5.5, "minecraft", "entity.warden.heartbeat", 0.5F, 1.55F},
        {5.9, "minecraft", "entity.warden.heartbeat", 0.5F, 1.6F},
        {7.4, "minecraft", "block.wool.step", 0.5F, 0.7F},
        {8.6, "minecraft", "entity.warden.heartbeat", 0.35F, 1.0F},
        {9.8, "minecraft", "entity.chicken.ambient", 0.25F, 0.7F}}, new Object[][] {
        {2.0, 4.4, "The candle goes out."},
        {4.6, 7.0, "Her hand finds yours in the dark."},
        {7.2, 9.4, "...Later."}}, 9.6);

    /** 1.3.10: a birth: the dark, "9 months later...", a heartbeat, a small cry, and who was born. */
    static final Script BIRTH = new Script(13.0, 1.4, 11.0, new Object[][] {
        {2.0, "minecraft", "entity.warden.heartbeat", 0.4F, 0.9F},
        {3.0, "minecraft", "entity.warden.heartbeat", 0.4F, 0.95F},
        {4.6, "minecraft", "block.wool.step", 0.5F, 0.8F},
        {5.8, "minecraft", "entity.warden.heartbeat", 0.45F, 1.3F},
        {6.3, "minecraft", "entity.warden.heartbeat", 0.45F, 1.4F},
        {7.2, "minecraft", "entity.cat.ambient", 0.35F, 1.9F},
        {7.9, "minecraft", "block.amethyst_block.chime", 0.6F, 1.2F}}, new Object[][] {
        {1.6, 4.6, "9 months later..."},
        {5.0, 7.6, "A small cry in the dark."}}, 8.2);

    private static Script playing;
    private static String words = "";
    private static long began = Long.MIN_VALUE;
    private static int played;

    private CutsceneOverlay() {
    }

    public static void begin(int script, String said) {
        playing = script == S2CCutscene.NIGHT ? NIGHT : script == S2CCutscene.BIRTH ? BIRTH : SURGERY;
        words = said == null ? "" : said;
        began = System.nanoTime();
        played = 0;
        Minecraft.m_91087_().m_91152_(null);
    }

    /** How dark the screen is (0..1) {@code t} seconds into {@code script}. */
    static float dark(Script script, double t) {
        if (t < 0.0 || t >= script.length()) {
            return 0.0F;
        }
        if (t < script.dark()) {
            return (float)(t / script.dark());
        }
        return t < script.wake() ? 1.0F : (float)(1.0 - (t - script.wake()) / (script.length() - script.wake()));
    }

    /** How clear a line shown from {@code from} to {@code to} is (0..1) {@code t} seconds in. */
    static float clear(double t, double from, double to) {
        if (t < from || t >= to) {
            return 0.0F;
        }
        return (float)Math.min(1.0, Math.min((t - from) / 0.8, (to - t) / 0.8));
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (began == Long.MIN_VALUE || playing == null || !event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type())) {
            return;
        }
        Script script = playing;
        double t = (System.nanoTime() - began) / 1.0E9;
        if (t >= script.length()) {
            began = Long.MIN_VALUE;
            playing = null;
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        while (played < script.cues().length && t >= (double)script.cues()[played][0]) {
            Object[] cue = script.cues()[played++];
            sound(minecraft, (String)cue[1], (String)cue[2], (Float)cue[3], (Float)cue[4]);
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().m_85445_();
        int height = event.getWindow().m_85446_();
        int alpha = Math.round(255 * dark(script, t));
        if (alpha > 0) {
            graphics.m_280509_(0, 0, width, height, alpha << 24);
        }
        for (Object[] caption : script.captions()) {
            float shown = clear(t, (Double)caption[0], (Double)caption[1]);
            if (shown > 0.02F) {
                int a = Math.max(5, Math.round(255 * shown));
                graphics.m_280137_(minecraft.f_91062_, (String)caption[2], width / 2, height / 2 - 4, a << 24 | 0xB8A8B0);
            }
        }
        float said = clear(t, script.wordsAt(), script.length() - 0.3);
        if (said > 0.02F && !words.isEmpty()) {
            int a = Math.max(5, Math.round(255 * said));
            String[] lines = words.split("\n");
            for (int i = 0; i < lines.length; i++) {
                graphics.m_280137_(minecraft.f_91062_, lines[i], width / 2, height / 2 - 4 + (i - (lines.length - 1) / 2) * 12, a << 24 | 0xE8D8DC);
            }
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

    /** A one-shot where the player is. */
    static final class Cue extends AbstractTickableSoundInstance {
        Cue(SoundEvent sound, float volume, float pitch, double x, double y, double z) {
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

    /** Plays {@code namespace:path} where the player is (the alerts' chimes too). */
    static void play(String namespace, String path, float volume, float pitch) {
        sound(Minecraft.m_91087_(), namespace, path, volume, pitch);
    }
}
