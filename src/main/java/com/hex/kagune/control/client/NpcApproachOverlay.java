package com.hex.kagune.control.client;

import com.hex.kagune.control.network.C2SNpcTalk;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CNpcApproach;
import com.hex.kagune.control.npc.NpcTalk;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.29: someone came up to the player with something to say ({@code S2CNpcApproach}): a card under the top of the screen, their name
 * and their words, and how to answer (the Answer key, O by default, or a right-click on them), for as long as they wait. A soft chime as
 * it comes. Answered, it opens the talk screen on what they came to say; the card goes once that screen is open, or they stop waiting.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class NpcApproachOverlay {
    /** The card's widest (pixels of the scaled GUI) and most lines of their words. */
    static final int WIDTH = 300;
    static final int LINES = 3;

    private static int npc = -1;
    private static String name = "";
    private static String colour = "";
    private static String line = "";
    private static long began = Long.MIN_VALUE;
    private static long until = Long.MIN_VALUE;

    private NpcApproachOverlay() {
    }

    public static void accept(S2CNpcApproach message) {
        if (message.ticks() <= 0) {
            if (message.npc() == npc) {
                clear();
            }
            return;
        }
        npc = message.npc();
        name = message.name();
        colour = message.colour();
        line = message.line();
        began = System.nanoTime();
        until = began + message.ticks() * 50_000_000L;
        CutsceneOverlay.play("minecraft", "block.note_block.chime", 0.6F, 1.5F);
        CutsceneOverlay.play("minecraft", "block.note_block.bell", 0.25F, 1.9F);
    }

    static void clear() {
        npc = -1;
        began = Long.MIN_VALUE;
        until = Long.MIN_VALUE;
    }

    /** True while someone waits on an answer. */
    static boolean waiting() {
        return npc >= 0 && System.nanoTime() < until;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (npc >= 0 && (System.nanoTime() >= until || minecraft.f_91074_ == null)) {
            clear();
        }
        if (minecraft.f_91080_ instanceof NpcTalkScreen screen && screen.npc == npc) {
            clear(); // answered (the key, or a right-click on them)
        }
        while (ControlKeys.ANSWER.m_90859_()) {
            if (waiting() && minecraft.f_91080_ == null) {
                ControlNetwork.CHANNEL.sendToServer(new C2SNpcTalk(npc, NpcTalk.Choice.ANSWER.ordinal()));
            }
        }
    }

    /** "O" for the Answer key bound to O ("key.keyboard.o"), "MOUSE 4" for a mouse button... */
    static String keyName() {
        String bound = ControlKeys.ANSWER.m_90865_();
        if (bound == null || bound.isEmpty() || bound.endsWith(".unknown")) {
            return "?";
        }
        String last = bound.substring(bound.lastIndexOf('.') + 1);
        if (bound.startsWith("key.mouse.")) {
            return "MOUSE " + last.toUpperCase(java.util.Locale.ROOT);
        }
        return last.toUpperCase(java.util.Locale.ROOT);
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!waiting() || !event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type())) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91080_ != null) {
            return;
        }
        long now = System.nanoTime();
        double age = (now - began) / 1.0E9;
        double left = (until - now) / 1.0E9;
        float shown = (float)Math.max(0.0, Math.min(1.0, Math.min(age / 0.3, left / 0.6)));
        if (shown <= 0.02F) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int screen = event.getWindow().m_85445_();
        List<String> wrapped = wrap(minecraft, "“" + line + "”", WIDTH - 20);
        int lines = Math.min(LINES, wrapped.size());
        int height = 16 + lines * 10 + 14;
        int x0 = screen / 2 - WIDTH / 2;
        int y0 = 34 + Math.round((1.0F - shown) * -12.0F);
        int alpha = Math.max(5, Math.round(255 * shown));
        // The card: dark, a rose rule down its left, a soft glow along its top that breathes.
        graphics.m_280509_(x0, y0, x0 + WIDTH, y0 + height, Math.round(190 * shown) << 24 | 0x1C0710);
        graphics.m_280509_(x0, y0, x0 + 2, y0 + height, alpha << 24 | 0xE0607E);
        int breathe = (int)Math.round(60 + 50 * Math.sin(age * 3.0));
        graphics.m_280509_(x0, y0, x0 + WIDTH, y0 + 1, Math.round(breathe * shown) << 24 | 0xFFB8C8);
        graphics.m_280056_(minecraft.f_91062_, colour + name + "§7 comes up to you", x0 + 10, y0 + 5, alpha << 24 | 0xFFFFFF, false);
        for (int i = 0; i < lines; i++) {
            String text = wrapped.get(i);
            if (i == lines - 1 && wrapped.size() > lines) {
                text = text + " ...";
            }
            graphics.m_280056_(minecraft.f_91062_, text, x0 + 10, y0 + 17 + i * 10, alpha << 24 | 0xF0DCE0, false);
        }
        String hint = "[" + keyName() + "] Answer   ·   or right-click them";
        int seconds = (int)Math.ceil(Math.max(0.0, left));
        String time = seconds + "s";
        int hintY = y0 + 17 + lines * 10 + 2;
        graphics.m_280056_(minecraft.f_91062_, hint, x0 + 10, hintY, alpha << 24 | 0xE0A0B4, false);
        graphics.m_280056_(minecraft.f_91062_, time, x0 + WIDTH - 8 - minecraft.f_91062_.m_92895_(time), hintY, alpha << 24 | 0x9A6A78, false);
    }

    /** {@code text} broken into lines no wider than {@code width}. */
    static List<String> wrap(Minecraft minecraft, String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String tried = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && minecraft.f_91062_.m_92895_(tried) > width) {
                out.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(tried);
            }
        }
        if (current.length() > 0) {
            out.add(current.toString());
        }
        return out;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
