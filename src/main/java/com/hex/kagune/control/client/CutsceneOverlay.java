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
    record Script(double length, double dark, double wake, Object[][] cues, Object[][] captions, double wordsAt, double fade, double[] flashes) {
        Script(double length, double dark, double wake, Object[][] cues, Object[][] captions, double wordsAt) {
            this(length, dark, wake, cues, captions, wordsAt, 0.8, new double[0]);
        }
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

    /**
     * 1.3.14: Ken's trial: what was done to him, done to them. Ten days in the dark: the pliers, the fingers growing back, the count down
     * from a thousand by sevens, the thing in the ear, the choice; then what grows out of it. A red flash at every break.
     */
    static final Script TORTURE = new Script(46.0, 1.6, 43.0, new Object[][] {
        {2.2, "minecraft", "entity.warden.heartbeat", 0.6F, 0.85F},
        {3.4, "minecraft", "entity.warden.heartbeat", 0.6F, 0.85F},
        {5.0, "hexkagune", "sfx.crunch", 1.0F, 0.9F},
        {5.3, "hexkagune", "bone_crack", 0.9F, 1.1F},
        {7.4, "hexkagune", "sfx.writhe", 0.7F, 1.2F},
        {9.8, "hexkagune", "sfx.crunch", 1.0F, 0.85F},
        {10.4, "hexkagune", "sfx.crunch", 1.0F, 0.95F},
        {10.9, "hexkagune", "bone_crush", 0.9F, 1.0F},
        {12.0, "hexkagune", "sfx.crunch", 1.0F, 0.9F},
        {12.5, "hexkagune", "bone_crack", 1.0F, 1.2F},
        {14.0, "minecraft", "entity.warden.heartbeat", 0.7F, 1.0F},
        {16.3, "minecraft", "entity.warden.heartbeat", 0.7F, 1.1F},
        {17.1, "minecraft", "entity.warden.heartbeat", 0.7F, 1.2F},
        {17.9, "hexkagune", "sfx.crunch", 0.8F, 1.0F},
        {18.7, "minecraft", "entity.warden.heartbeat", 0.75F, 1.3F},
        {19.4, "minecraft", "entity.warden.heartbeat", 0.75F, 1.4F},
        {20.0, "hexkagune", "bone_crack", 0.9F, 1.3F},
        {20.6, "minecraft", "entity.warden.heartbeat", 0.8F, 1.5F},
        {21.2, "minecraft", "entity.warden.heartbeat", 0.8F, 1.55F},
        {21.7, "minecraft", "entity.warden.heartbeat", 0.85F, 1.6F},
        {22.2, "hexkagune", "sfx.crunch", 1.0F, 0.8F},
        {23.4, "hexkagune", "sfx.writhe", 0.9F, 1.5F},
        {24.1, "hexkagune", "sfx.drips", 0.7F, 1.4F},
        {24.8, "hexkagune", "sfx.writhe", 0.9F, 1.7F},
        {26.8, "hexkagune", "sfx.stab", 1.0F, 0.9F},
        {27.2, "hexkagune", "sfx.gush", 0.9F, 1.0F},
        {29.5, "minecraft", "entity.warden.heartbeat", 0.5F, 0.8F},
        {31.0, "minecraft", "entity.warden.heartbeat", 0.45F, 0.75F},
        {32.6, "minecraft", "entity.warden.heartbeat", 0.4F, 0.7F},
        {34.4, "hexkagune", "manifest_rip", 1.0F, 0.8F},
        {35.0, "hexkagune", "bone_crack", 1.0F, 0.7F},
        {35.4, "hexkagune", "sfx.gush_long", 0.8F, 0.9F},
        {38.0, "hexkagune", "bone_crack", 0.6F, 1.5F}}, new Object[][] {
        {1.8, 4.0, "Ten days."},
        {4.2, 6.6, "Pliers. A finger."},
        {6.8, 8.8, "It grows back."},
        {9.0, 11.2, "So he does it again."},
        {11.4, 13.2, "And again."},
        {13.4, 16.0, "\"What's a thousand minus seven?\""},
        {16.2, 17.0, "993"}, {17.0, 17.8, "986"}, {17.8, 18.6, "979"}, {18.6, 19.4, "972"}, {19.4, 20.0, "965"}, {20.0, 20.6, "958"},
        {20.6, 21.2, "951"}, {21.2, 21.7, "944"}, {21.7, 22.2, "937"}, {22.2, 22.8, "930..."},
        {23.0, 25.6, "Something crawls into your ear."},
        {25.8, 28.6, "You choose who dies. Either way, someone does."},
        {28.8, 31.4, "It's not the one who is killed who's in the wrong."},
        {31.6, 33.6, "It's the one who kills."},
        {33.8, 36.4, "Something white grows out of the pain."},
        {36.6, 39.4, "You crack a finger with your thumb. You don't remember starting."}}, 39.8, 0.3,
        new double[] {5.0, 9.8, 10.4, 10.9, 12.0, 17.9, 22.2, 26.8, 34.4});

    private static Script playing;
    private static String words = "";
    private static long began = Long.MIN_VALUE;
    private static int played;

    private CutsceneOverlay() {
    }

    public static void begin(int script, String said) {
        playing = script == S2CCutscene.NIGHT ? NIGHT : script == S2CCutscene.BIRTH ? BIRTH : script == S2CCutscene.TORTURE ? TORTURE : SURGERY;
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
        return clear(t, from, to, 0.8);
    }

    /** 1.3.14: the same, fading in and out over {@code fade} seconds. */
    static float clear(double t, double from, double to, double fade) {
        if (t < from || t >= to) {
            return 0.0F;
        }
        return (float)Math.min(1.0, Math.min((t - from) / fade, (to - t) / fade));
    }

    /** 1.3.14: how red the screen flashes (0..1) {@code t} seconds into {@code script}: a pulse at each of its flashes, gone in a third of a second. */
    static float flash(Script script, double t) {
        float most = 0.0F;
        for (double at : script.flashes()) {
            double since = t - at;
            if (since >= 0.0 && since < 0.35) {
                most = Math.max(most, (float)(1.0 - since / 0.35));
            }
        }
        return most;
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
        int red = Math.round(150 * flash(script, t));
        if (red > 0) {
            graphics.m_280509_(0, 0, width, height, red << 24 | 0x8A0010);
        }
        for (Object[] caption : script.captions()) {
            float shown = clear(t, (Double)caption[0], (Double)caption[1], script.fade());
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
