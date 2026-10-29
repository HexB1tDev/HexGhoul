package com.hex.kagune.control.client;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.UrgeType;
import com.hex.kagune.gameplay.TendrilMode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bloodlust at the edge of vision: a soft dark-red vignette plus veins that creep in from the edges and pulse with a
 * heartbeat. Drawn below every other HUD element; the centre of the screen is never tinted.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BloodlustOverlay implements IGuiOverlay {
    private static final ResourceLocation VIGNETTE = KaguneMod.id("textures/misc/bloodlust_vignette.png");
    private static final ResourceLocation VEINS = KaguneMod.id("textures/misc/bloodlust_veins.png");
    private static float shown;
    private static float beatPhase;
    private static long lastFrame;

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("kagune_bloodlust", new BloodlustOverlay());
    }

    /** "Lub-dub": two pulses per beat, 0..1. */
    static float heartbeat(float phase) {
        float t = phase - (float)Math.floor(phase);
        float lub = (float)Math.exp(-Math.pow((t - 0.08) / 0.05, 2));
        float dub = 0.7F * (float)Math.exp(-Math.pow((t - 0.3) / 0.06, 2));
        return Math.min(1.0F, lub + dub);
    }

    private static float target(Minecraft minecraft) {
        KaguneClientState.View state = KaguneClientState.self();
        if (!ControlConfig.bloodlustOverlay() || minecraft.f_91074_ == null || !state.active() || !minecraft.f_91074_.m_6084_()
            || minecraft.f_91074_.m_5833_()) {
            return 0.0F;
        }
        ControlView control = ControlClient.self();
        float intensity = control.bloodlust();
        if (state.mode() == TendrilMode.MANIFEST) {
            intensity += 0.35F * (1.0F - control.control());
        }
        if (control.urge() != UrgeType.NONE && !control.urgeRestrained()
            && control.urgeAge(ControlClient.worldTime(minecraft.m_91296_())) >= 0.0F) {
            intensity += 0.25F * control.urgeStrength();
        }
        return Math.min(1.0F, intensity) * ControlConfig.overlayOpacity();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.m_91087_();
        long frame = System.nanoTime();
        float dt = lastFrame == 0L ? 0.0F : Math.min(0.1F, (frame - lastFrame) / 1.0E9F);
        lastFrame = frame;
        float goal = target(minecraft);
        shown += (goal - shown) * (1.0F - (float)Math.exp(-dt * (goal > shown ? 4.0F : 1.2F)));
        if (shown <= 0.01F) {
            return;
        }
        beatPhase += dt * (60.0F + 80.0F * shown) / 60.0F;
        beatPhase -= (float)Math.floor(beatPhase);
        float beat = heartbeat(beatPhase);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, shown * (0.55F + 0.2F * beat)));
        graphics.m_280411_(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
        // Weak bloodlust keeps the veins pushed off-screen; strong bloodlust lets them creep in.
        float scale = 1.35F - 0.35F * shown;
        int veinWidth = Math.round(width * scale);
        int veinHeight = Math.round(height * scale);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, (float)Math.pow(shown, 1.2) * (0.45F + 0.5F * beat)));
        graphics.m_280411_(VEINS, (width - veinWidth) / 2, (height - veinHeight) / 2, veinWidth, veinHeight, 0.0F, 0.0F, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
