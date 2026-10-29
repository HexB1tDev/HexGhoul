package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.AwakeningStage;
import com.hex.kagune.control.awakening.DragonRig;
import com.hex.kagune.control.awakening.DragonTimeline;
import com.hex.kagune.control.awakening.S2CAwakeningStatus;
import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client wiring: total loss of control, the hidden human body, screen effects and the quest readout. */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class AwakeningClientEvents {
    private AwakeningClientEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            DragonCamera.tick();
            // 1.0.11: and the movement keys too (his body stays where it is held, seen or not).
            com.hex.kagune.control.client.TurningClient.hold(DragonCamera.active());
            if (DragonCamera.active()) drainInput(Minecraft.getInstance().options);
        } else {
            DragonClient.tick();
            DragonClient.Dragon local = DragonClient.local();
            if (local != null && local.current != null) {
                DragonPose pose = local.pose(1.0F);
                for (int h = 0; h < pose.headStates.length; h++) {
                    if (pose.headStates[h] == DragonRig.HeadState.STRIKE.ordinal()) DragonCamera.hot(pose.world(pose.headTips[h]), 1.0);
                }
            }
        }
    }

    /**
     * Swallows every gameplay key, including the kagune keys, before Minecraft or the mod reads them. (1.0.33: as War's small Dragon,
     * M1 and RMB are its bite and slam: taken here all the same, and handed to its driving, {@code KingInput.wyrmClicks}.)
     */
    private static void drainInput(Options options) {
        if (DragonCamera.chase()) {
            boolean bite = false;
            boolean slam = false;
            while (options.keyAttack.consumeClick()) bite = true;
            while (options.keyUse.consumeClick()) slam = true;
            com.hex.kagune.control.client.KingInput.wyrmClicks(bite, slam);
        }
        KeyMapping[] keys = {options.keyAttack, options.keyUse, options.keyPickItem, options.keyDrop, options.keySwapOffhand, options.keyInventory,
                options.keyTogglePerspective, KaguneKeys.TOGGLE, KaguneKeys.MENU, KaguneKeys.GRAB, KaguneKeys.THROW, KaguneKeys.RAPID_STRIKE,
                KaguneKeys.SLINGSHOT, KaguneKeys.CYCLE_TENDRILS, KaguneKeys.CRUSH, KaguneKeys.LIFT, KaguneKeys.SLAM, KaguneKeys.GUARD,
                com.hex.kagune.control.client.ControlKeys.UNLEASH, com.hex.kagune.control.client.ControlKeys.QUESTS};
        for (KeyMapping key : keys) {
            while (key.consumeClick()) {
            }
        }
        for (KeyMapping key : options.keyHotbarSlots) {
            while (key.consumeClick()) {
            }
        }
        options.keyAttack.setDown(false);
        options.keyUse.setDown(false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (DragonCamera.active()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) DragonCamera.frame(event.renderTickTime);
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles event) {
        double shake = DragonClient.shake;
        if (shake <= 0.0) return;
        double t = System.nanoTime() / 1.0E9;
        event.setYaw((float) (event.getYaw() + Math.sin(t * 37.0) * 2.4 * shake));
        event.setPitch((float) (event.getPitch() + Math.sin(t * 29.0 + 1.0) * 1.8 * shake));
        event.setRoll((float) (event.getRoll() + Math.sin(t * 23.0 + 2.0) * 3.0 * shake));
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (!DragonCamera.active() || DragonCamera.chase()) return;
        event.setFOV(event.getFOV() + 10.0 * DragonCamera.bars() + DragonClient.fovPunch);
    }

    @SubscribeEvent
    public static void hideHand(RenderHandEvent event) {
        if (DragonCamera.active()) event.setCanceled(true);
    }

    /**
     * 1.0.11: what blacked the view out now and then: Minecraft paints the block (or water, or fire) round the player's own head over
     * the whole screen, and the player's body is held in the ground under the Dragon while the camera is far off. The camera never
     * sits in a block itself ({@code DragonCamera.safe}), so while it has the view none of that is drawn.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noHeadInTheWall(RenderBlockScreenEffectEvent event) {
        if (DragonCamera.active()) event.setCanceled(true);
    }

    /** Kaneki's human form is gone while the Dragon exists, for every viewer. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideBody(RenderPlayerEvent.Pre event) {
        if (DragonClient.hidden(event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void hideHud(RenderGuiOverlayEvent.Pre event) {
        // (1.0.13: not under War's small Dragon: his abilities board and his health stay.)
        if (DragonCamera.active() && !DragonCamera.chase() && !event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post event) {
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        flash(graphics, width, height, DragonClient.redFlash, 0x8A0010);
        flash(graphics, width, height, DragonClient.whiteFlash, 0xFFF4EC);
        if (DragonCamera.active() && !DragonCamera.chase()) {
            cinematic(graphics, width, height);
        } else if (!DragonCamera.active()) {
            quest(graphics);
        }
    }

    private static void flash(GuiGraphics graphics, int width, int height, double strength, int rgb) {
        int alpha = (int) Math.min(255.0, strength * 255.0);
        if (alpha >= 4) graphics.fill(0, 0, width, height, alpha << 24 | rgb);
    }

    private static void cinematic(GuiGraphics graphics, int width, int height) {
        int bar = (int) Math.round(height * 0.11 * DragonCamera.bars());
        if (bar > 0) {
            graphics.fill(0, 0, width, bar, 0xFF000000);
            graphics.fill(0, height - bar, width, height, 0xFF000000);
        }
        DragonClient.Dragon dragon = DragonClient.local();
        if (dragon == null || dragon.current == null) return;
        int tick = dragon.tick();
        double appear = DragonTimeline.smooth((tick - 8) / 14.0) * (1.0 - DragonTimeline.smooth((tick - 70) / 20.0));
        if (appear > 0.02) {
            Minecraft minecraft = Minecraft.getInstance();
            int alpha = (int) (appear * 255.0);
            graphics.pose().pushPose();
            graphics.pose().scale(4.0F, 4.0F, 1.0F);
            graphics.drawCenteredString(minecraft.font, "D R A G O N", width / 8, height / 8 - 8, alpha << 24 | 0xC0101A);
            graphics.pose().popPose();
            graphics.drawCenteredString(minecraft.font, "K A K U J A   V 2", width / 2, height / 2 + 12, alpha << 24 | 0xB8B0A8);
        }
        if (tick >= DragonTimeline.RAMPAGE_START && tick < DragonTimeline.RAMPAGE_END && bar > 10) {
            int seconds = (DragonTimeline.RAMPAGE_END - tick + 19) / 20;
            graphics.drawCenteredString(Minecraft.getInstance().font, String.valueOf(seconds), width / 2, height - bar / 2 - 4, 0xFF8E0F1A);
        }
    }

    private static void quest(GuiGraphics graphics) {
        S2CAwakeningStatus status = DragonClient.status;
        if (status.stage() != AwakeningStage.ARMED) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !KaguneClientState.self().unlocked()) return;
        boolean recent = DragonClient.ticks - DragonClient.statusChangedAt < 200;
        if (!recent && !KaguneClientState.self().active()) return;
        int x = 6;
        int y = 6;
        // The quest's own progress is in the journal; this only reminds an armed Kaneki what his next death means.
        graphics.drawString(minecraft.font, "\u00a7c\u00a7lTHE DRAGON SLEEPS", x, y, 0xFFFFFFFF, true);
        graphics.drawString(minecraft.font, "\u00a77Your next death will awaken it.", x, y + 11, 0xFFFFFFFF, true);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DragonCamera.reset();
        DragonClient.clear();
    }
}
