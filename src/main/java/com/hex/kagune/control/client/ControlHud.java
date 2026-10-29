package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.ControlMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A one-pixel kagune-control line tucked under the original RC bar (same anchor as KaguneEnergyHud):
 * crimson when the kagune controls you, bone-white when you control it. It flickers while instinct is
 * strong enough to break through, so players can read their own state at a glance.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ControlHud {
    private static final int WIDTH = 81;

    private ControlHud() {
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().equals(VanillaGuiOverlay.FOOD_LEVEL.type()) || !ControlConfig.controlIndicator()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        KaguneClientState.View state = KaguneClientState.self();
        if (minecraft.f_91074_ == null || !state.active() || !minecraft.f_91074_.m_6084_()) {
            return;
        }
        ControlView control = ControlClient.self();
        GuiGraphics graphics = event.getGuiGraphics();
        int x = event.getWindow().m_85445_() / 2 + 10;
        int y = event.getWindow().m_85446_() - 52 + 8;
        float ratio = ControlMath.clamp01(control.control());
        int fill = Math.round((WIDTH - 2) * ratio);
        graphics.m_280509_(x - 1, y, x + WIDTH + 1, y + 3, 0xC8000000);
        if (fill > 0) {
            graphics.m_280509_(x + 1, y + 1, x + 1 + fill, y + 2, blend(0xFF7A0A12, 0xFFE8DCC8, ratio));
        }
        float instinct = control.instinct();
        if (instinct > ControlMath.URGE_THRESHOLD) {
            double time = ControlClient.worldTime(minecraft.m_91296_());
            float flicker = 0.5F + 0.5F * (float)Math.sin(time * (0.35 + instinct * 0.5));
            int alpha = Math.round(255.0F * flicker * Math.min(1.0F, (instinct - ControlMath.URGE_THRESHOLD) * 2.5F));
            if (alpha > 8) {
                graphics.m_280509_(x + 1 + fill, y + 1, x + WIDTH - 1, y + 2, alpha << 24 | 0xB0141E);
            }
        }
    }

    private static int blend(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
