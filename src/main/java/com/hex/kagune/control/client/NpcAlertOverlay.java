package com.hex.kagune.control.client;

import com.hex.kagune.control.network.S2CNpcAlert;
import com.hex.kagune.control.npc.Romance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.2: a private word from an NPC the player is together with ({@code S2CNpcAlert}): a banner at the top of the screen, a heart and
 * her name, for a few seconds, and a chime of its own kind (a kiss rings bright, the night low, a fight sharp), heard only by them.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class NpcAlertOverlay {
    static final double SHOWN = 6.0;

    private static String line = "";
    private static long began = Long.MIN_VALUE;

    private NpcAlertOverlay() {
    }

    public static void accept(S2CNpcAlert message) {
        Romance.Want want = Romance.Want.values()[Math.floorMod(message.want(), Romance.Want.values().length)];
        line = "❤ " + message.who() + " " + (message.text().isEmpty() ? want.text() : message.text());
        began = System.nanoTime();
        switch (want) {
            case KISS -> {
                CutsceneOverlay.play("minecraft", "block.amethyst_block.chime", 1.0F, 1.4F);
                CutsceneOverlay.play("minecraft", "entity.experience_orb.pickup", 0.5F, 1.6F);
            }
            case HUG, MISSED -> CutsceneOverlay.play("minecraft", "block.note_block.chime", 0.8F, 1.2F);
            case NIGHT -> {
                CutsceneOverlay.play("minecraft", "block.note_block.harp", 0.7F, 0.7F);
                CutsceneOverlay.play("minecraft", "block.note_block.harp", 0.5F, 0.94F);
            }
            case HELP -> CutsceneOverlay.play("minecraft", "block.note_block.bell", 1.0F, 0.6F);
            default -> CutsceneOverlay.play("minecraft", "block.note_block.chime", 0.7F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (began == Long.MIN_VALUE || !event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type())) {
            return;
        }
        double t = (System.nanoTime() - began) / 1.0E9;
        if (t >= SHOWN) {
            began = Long.MIN_VALUE;
            return;
        }
        float shown = (float)Math.min(1.0, Math.min(t / 0.35, (SHOWN - t) / 0.8));
        if (shown <= 0.02F) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().m_85445_();
        int textWidth = minecraft.f_91062_.m_92895_(line);
        int y = 14 + Math.round((1.0F - shown) * -10.0F);
        int alpha = Math.max(5, Math.round(255 * shown));
        graphics.m_280509_(width / 2 - textWidth / 2 - 8, y - 5, width / 2 + textWidth / 2 + 8, y + 13, Math.round(170 * shown) << 24 | 0x2A0A14);
        graphics.m_280509_(width / 2 - textWidth / 2 - 8, y + 12, width / 2 + textWidth / 2 + 8, y + 13, alpha << 24 | 0xE0607E);
        graphics.m_280137_(minecraft.f_91062_, line, width / 2, y, alpha << 24 | 0xFFC8D8);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        began = Long.MIN_VALUE;
    }
}
