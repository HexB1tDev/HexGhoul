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
    record Script(double length, double dark, double wake, Object[][] cues, Object[][] captions, double wordsAt, double fade, double[] flashes,
                  boolean warm, int flashColour) {
        Script(double length, double dark, double wake, Object[][] cues, Object[][] captions, double wordsAt) {
            this(length, dark, wake, cues, captions, wordsAt, 0.8, new double[0]);
        }

        Script(double length, double dark, double wake, Object[][] cues, Object[][] captions, double wordsAt, double fade, double[] flashes) {
            this(length, dark, wake, cues, captions, wordsAt, fade, flashes, false, 0x8A0010);
        }
    }

    /** 4.1.15: how long each of the night's captions shows (seconds), and when the first comes. */
    static final double CAPTION_TIME = 2.7;
    static final double FIRST_CAPTION = 2.2;

    /**
     * 4.1.15 (the owner: "improve the making love scene"): the night as its captions have it (NightWords, chosen on the server for whoever it
     * is with): a candle's warm glow as the room goes dark and the candle goes out, the sheets, two heartbeats quickening and then slowing into
     * step, the rain on the window or the thunder if the captions have them, each caption in turn, the morning's words, and the dawn coming back
     * pale with the birds.
     */
    static Script night(java.util.List<String> captions) {
        int n = captions.size();
        double end = FIRST_CAPTION + n * CAPTION_TIME;
        double wordsAt = end + 0.3;
        double wake = wordsAt + 2.6;
        double length = wake + 2.4;
        java.util.List<Object[]> cues = new java.util.ArrayList<>();
        cues.add(new Object[] {1.6, "minecraft", "block.candle.extinguish", 0.8F, 1.0F});
        cues.add(new Object[] {2.6, "minecraft", "block.wool.step", 0.55F, 0.8F});
        // Two heartbeats quickening through the second and third captions, then slowing into one.
        double beat = FIRST_CAPTION + CAPTION_TIME;
        double gap = 0.7;
        for (int i = 0; i < 7 && beat < end - 0.4; i++) {
            cues.add(new Object[] {beat, "minecraft", "entity.warden.heartbeat", 0.4F + 0.02F * i, 1.2F + 0.06F * i});
            beat += gap;
            gap = Math.max(0.42, gap - 0.06);
        }
        cues.add(new Object[] {Math.max(beat, end - 1.6), "minecraft", "block.wool.step", 0.45F, 0.7F});
        cues.add(new Object[] {end - 0.6, "minecraft", "entity.warden.heartbeat", 0.3F, 0.95F});
        boolean rain = false;
        boolean thunder = false;
        for (String caption : captions) {
            String lower = caption.toLowerCase(java.util.Locale.ROOT);
            rain |= lower.contains("rain");
            thunder |= lower.contains("thunder") || lower.contains("lightning");
        }
        java.util.List<Double> flashes = new java.util.ArrayList<>();
        if (rain || thunder) {
            for (double at = 2.0; at < wake; at += 1.9) {
                cues.add(new Object[] {at, "minecraft", "weather.rain", 0.18F, 0.9F});
            }
        }
        if (thunder) {
            double at = FIRST_CAPTION + CAPTION_TIME + 0.5;
            flashes.add(at);
            cues.add(new Object[] {at + 0.3, "minecraft", "entity.lightning_bolt.thunder", 0.25F, 0.7F});
        }
        // Dawn: the rooster the night has always had (a vanilla sound the owner's pack keeps: SfxTests.KEPT_VANILLA).
        cues.add(new Object[] {wake + 0.2, "minecraft", "entity.chicken.ambient", 0.25F, 0.7F});
        cues.sort(java.util.Comparator.comparingDouble(cue -> (Double)cue[0]));
        Object[][] shown = new Object[n][];
        for (int i = 0; i < n; i++) {
            shown[i] = new Object[] {FIRST_CAPTION + i * CAPTION_TIME, FIRST_CAPTION + (i + 1) * CAPTION_TIME - 0.15, captions.get(i)};
        }
        double[] flashAt = new double[flashes.size()];
        for (int i = 0; i < flashAt.length; i++) {
            flashAt[i] = flashes.get(i);
        }
        return new Script(length, 1.6, wake, cues.toArray(new Object[0][]), shown, wordsAt, 0.7, flashAt, true, 0xDDE4FF);
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

    /**
     * 4.0.0 (the owner: "eventually unlock their Kakuja through a custom cinematic cutscene"): Ayato's lesson. A rooftop at night, the wind;
     * he throws you off it; your wings open too late; the fall, the crystal, his voice; the kakuja tears out of your back and wraps you, and
     * you land on your feet. A white flash at every break.
     */
    static final Script AYATO_KAKUJA = new Script(30.0, 1.4, 27.0, new Object[][] {
        {1.6, "minecraft", "entity.ender_dragon.flap", 0.4F, 1.8F},
        {3.2, "minecraft", "entity.warden.heartbeat", 0.6F, 0.9F},
        {5.6, "minecraft", "entity.player.attack.knockback", 1.0F, 0.8F},
        {6.4, "minecraft", "entity.ender_dragon.flap", 0.9F, 1.2F},
        {7.0, "minecraft", "entity.ender_dragon.flap", 0.9F, 1.4F},
        {8.6, "minecraft", "entity.warden.heartbeat", 0.8F, 1.3F},
        {9.4, "minecraft", "entity.warden.heartbeat", 0.8F, 1.5F},
        {10.4, "minecraft", "entity.ender_dragon.flap", 1.0F, 1.6F},
        {11.6, "minecraft", "block.amethyst_block.resonate", 1.0F, 0.9F},
        {13.2, "hexkagune", "manifest_rip", 1.0F, 0.8F},
        {13.6, "hexkagune", "bone_crack", 1.0F, 0.9F},
        {14.4, "minecraft", "block.amethyst_cluster.break", 1.0F, 0.7F},
        {15.4, "hexkagune", "sfx.writhe", 0.9F, 0.8F},
        {16.6, "minecraft", "block.amethyst_block.resonate", 1.0F, 0.6F},
        {17.6, "hexkagune", "manifest_rip", 1.0F, 0.6F},
        {18.2, "minecraft", "entity.ender_dragon.flap", 1.0F, 0.8F},
        {19.6, "minecraft", "block.amethyst_cluster.break", 1.0F, 0.5F},
        {20.8, "minecraft", "entity.warden.heartbeat", 0.7F, 1.0F},
        {22.4, "hexkagune", "bone_crush", 1.0F, 0.6F},
        {24.0, "minecraft", "block.amethyst_block.chime", 0.8F, 0.8F}}, new Object[][] {
        {1.6, 3.8, "A rooftop. Night. The wind."},
        {4.0, 5.6, "\"You want it? Then fall.\""},
        {5.8, 7.6, "He pushes you off the edge."},
        {7.8, 9.8, "Your wings open. Too slow. Too small."},
        {10.0, 12.2, "\"Stop holding back, idiot!\""},
        {12.4, 14.2, "Something tears out of your back."},
        {14.4, 16.4, "Crystal. Everywhere. Over your arms, your face."},
        {16.6, 18.6, "It doesn't stop at the wings."},
        {18.8, 21.0, "It wraps you like armour. Like a second body."},
        {21.2, 23.4, "You hit the ground on your feet. The street cracks."},
        {23.6, 25.6, "Above you, on the roof, he's grinning."}}, 25.8, 0.3,
        new double[] {5.6, 13.2, 14.4, 17.6, 19.6, 22.4});

    /**
     * 4.0.0: Noro's lessons: he doesn't explain; his worm does. It takes you whole, the dark inside it, being eaten and not dying, the worm in
     * you answering, and you climbing back out with it. A red flash at every bite.
     */
    static final Script NORO = new Script(26.0, 1.4, 23.0, new Object[][] {
        {1.8, "minecraft", "entity.warden.heartbeat", 0.5F, 0.7F},
        {3.4, "hexkagune", "sfx.writhe", 0.9F, 0.7F},
        {5.0, "hexkagune", "sfx.crunch", 1.0F, 0.7F},
        {5.6, "hexkagune", "bone_crush", 1.0F, 0.8F},
        {7.4, "hexkagune", "sfx.gush", 0.8F, 0.8F},
        {9.2, "minecraft", "entity.warden.heartbeat", 0.8F, 0.8F},
        {10.4, "hexkagune", "sfx.writhe", 1.0F, 0.6F},
        {12.0, "hexkagune", "sfx.crunch", 1.0F, 0.9F},
        {13.6, "hexkagune", "manifest_rip", 1.0F, 0.7F},
        {15.2, "hexkagune", "sfx.writhe", 1.0F, 0.5F},
        {16.8, "hexkagune", "bone_crack", 1.0F, 0.8F},
        {18.4, "minecraft", "entity.warden.heartbeat", 0.6F, 1.2F},
        {20.0, "hexkagune", "sfx.drips", 0.7F, 0.9F}}, new Object[][] {
        {1.6, 3.4, "He opens his coat."},
        {3.6, 5.4, "It takes you whole."},
        {5.6, 7.6, "Dark. Wet. Teeth."},
        {7.8, 9.6, "You should be dead."},
        {9.8, 11.8, "You're not."},
        {12.0, 13.6, "Something in you bites back."},
        {13.8, 15.8, "Your worm. Answering his."},
        {16.0, 18.0, "You eat your way out."},
        {18.2, 20.2, "He is waiting. He has not moved."}}, 20.6, 0.3,
        new double[] {5.0, 5.6, 12.0, 13.6, 16.8});

    private static Script playing;
    private static String words = "";
    private static long began = Long.MIN_VALUE;
    private static int played;

    private CutsceneOverlay() {
    }

    public static void begin(int script, String said) {
        playing = script == S2CCutscene.NIGHT ? NIGHT : script == S2CCutscene.BIRTH ? BIRTH : script == S2CCutscene.TORTURE ? TORTURE
            : script == S2CCutscene.AYATO_KAKUJA ? AYATO_KAKUJA : script == S2CCutscene.NORO ? NORO : SURGERY;
        words = said == null ? "" : said;
        if (script == S2CCutscene.NIGHT) {
            // 4.1.15: the night's own captions, if the server chose them (NightWords); the morning's words after them.
            java.util.List<String> captions = com.hex.kagune.control.npc.NightWords.captionsOf(said);
            words = com.hex.kagune.control.npc.NightWords.wordsOf(said);
            if (!captions.isEmpty()) {
                playing = night(captions);
            }
        }
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
        if (script.warm()) {
            // 4.1.15: a candle's amber as the room goes dark, the dawn's pale gold as it comes back.
            float glow = t < script.dark() ? (float)Math.sin(Math.PI * Math.min(1.0, t / script.dark())) : 0.0F;
            float dawn = t > script.wake() ? 1.0F - dark(script, t) : 0.0F;
            int amber = Math.round(80 * glow);
            if (amber > 0) {
                graphics.m_280509_(0, 0, width, height, amber << 24 | 0xFF9440);
            }
            int gold = Math.round(70 * (float)Math.sin(Math.PI * Math.min(1.0, dawn)));
            if (gold > 0) {
                graphics.m_280509_(0, 0, width, height, gold << 24 | 0xFFE2B8);
            }
        }
        int red = Math.round(150 * flash(script, t));
        if (red > 0) {
            graphics.m_280509_(0, 0, width, height, red << 24 | (script == AYATO_KAKUJA ? 0xBFD4FF : script.flashColour())); // 4.0.0: his crystal's white
        }
        int room = Math.max(120, width - 60);
        for (Object[] caption : script.captions()) {
            float shown = clear(t, (Double)caption[0], (Double)caption[1], script.fade());
            if (shown > 0.02F) {
                int a = Math.max(5, Math.round(255 * shown));
                // 4.1.15: a long caption wrapped to the screen, centred.
                java.util.List<String> lines = wrap(minecraft.f_91062_, (String)caption[2], room);
                for (int i = 0; i < lines.size(); i++) {
                    graphics.m_280137_(minecraft.f_91062_, lines.get(i), width / 2, height / 2 - 4 + (i * 2 - (lines.size() - 1)) * 6, a << 24 | 0xB8A8B0);
                }
            }
        }
        float said = clear(t, script.wordsAt(), script.length() - 0.3);
        if (said > 0.02F && !words.isEmpty()) {
            int a = Math.max(5, Math.round(255 * said));
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (String line : words.split("\n")) {
                lines.addAll(wrap(minecraft.f_91062_, line, room));
            }
            for (int i = 0; i < lines.size(); i++) {
                graphics.m_280137_(minecraft.f_91062_, lines.get(i), width / 2, height / 2 - 4 + (i * 2 - (lines.size() - 1)) * 6, a << 24 | 0xE8D8DC);
            }
        }
    }

    /** 4.1.15: {@code text} broken into lines no wider than {@code width} (at its spaces; a word longer than a line keeps its own). */
    static java.util.List<String> wrap(net.minecraft.client.gui.Font font, String text, int width) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String tried = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && font.m_92895_(tried) > width) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(tried);
            }
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
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
