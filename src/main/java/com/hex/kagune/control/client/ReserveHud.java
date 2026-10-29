package com.hex.kagune.control.client;

import com.hex.kagune.control.FoodClassifier;
import com.hex.kagune.control.GhoulReserve;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.server.GhoulDiet;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Draws a ghoul's flesh reserve ({@link ReserveBarView}) where the food bar was (3.0.3). The food overlay is not cancelled
 * (the original RC bar and the control line draw on its post event): its icons are pushed far off screen as it renders and put
 * back right after, and the reserve is drawn in their place. AppleSkin's food icons (exhaustion underlay, saturation outline,
 * meal preview) draw on the same overlay's events, outside that push, so they are cancelled while the reserve shows (6.0.6).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ReserveHud {
    private static final long GHOST_NANOS = 700_000_000L;
    private static final long FLASH_NANOS = 450_000_000L;
    private static float reserve = -1.0F;
    private static float ghost;
    private static long ghostAt;
    private static long flashAt;
    private static boolean hidden;
    /** AppleSkin's own cancelable HUD events that draw over the food bar (its health preview is left alone). */
    private static final String[] APPLESKIN_FOOD_EVENTS = {
        "squeek.appleskin.api.event.HUDOverlayEvent$Exhaustion",
        "squeek.appleskin.api.event.HUDOverlayEvent$Saturation",
        "squeek.appleskin.api.event.HUDOverlayEvent$HungerRestored",
    };

    private ReserveHud() {
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            for (String name : APPLESKIN_FOOD_EVENTS) {
                Class<? extends Event> type;
                try {
                    type = Class.forName(name).asSubclass(Event.class);
                } catch (ClassNotFoundException | ClassCastException | LinkageError missing) {
                    continue;
                }
                MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, type, ReserveHud::hideAppleSkin);
            }
        }
    }

    private static void hideAppleSkin(Event event) {
        if (showing(Minecraft.m_91087_())) {
            event.setCanceled(true);
        }
    }

    /**
     * A ghoul cannot start eating human food (the vanilla food bar is held below full, so the game would always let them): the
     * server refuses too ({@code GhoulDiet.refuseHumanFood}); cancelling it here keeps the client from playing a meal it will not get.
     */
    @SubscribeEvent
    public static void refuseHumanFood(LivingEntityUseItemEvent.Start event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (reserve < 0.0F || event.getEntity() != minecraft.f_91074_ || !event.getItem().m_41614_()) {
            return;
        }
        if (GhoulDiet.classify(event.getItem(), event.getEntity()) == FoodClassifier.Diet.HUMAN_FOOD) {
            event.setCanceled(true);
        }
    }

    /** The owner's reserve from the server, below 0 when they live on the food bar. */
    public static void accept(float value) {
        long now = System.nanoTime();
        if (value >= 0.0F && reserve >= 0.0F) {
            if (value < reserve - 0.2F) {
                // A heal (or a rejected meal) drew on it: the ghost starts from what it was, or keeps an older, higher one.
                boolean fading = now - ghostAt < GHOST_NANOS;
                ghost = fading ? Math.max(ghost, reserve) : reserve;
                ghostAt = now;
            } else if (value > reserve + 0.5F) {
                flashAt = now;
            }
        }
        reserve = value;
    }

    /**
     * 7.0.0: the original RC bar's listener (bytecode hook, {@code KaguneEnergyHud.render}): RC is the shared bar now, so its own
     * bar is not drawn while this one shows; without the diet (or in creative) it draws as it always did.
     */
    public static void energyHud(RenderGuiOverlayEvent.Post event) {
        if (!showing(Minecraft.m_91087_())) {
            com.hex.kagune.client.KaguneEnergyHud.renderLegacy(event);
        }
    }

    private static boolean showing(Minecraft minecraft) {
        LocalPlayer player = minecraft.f_91074_;
        return reserve >= 0.0F && player != null && !player.m_7500_() && !player.m_5833_() && !player.m_20159_();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hideFood(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().equals(VanillaGuiOverlay.FOOD_LEVEL.type()) || !showing(Minecraft.m_91087_())) {
            return;
        }
        PoseStack pose = event.getGuiGraphics().m_280168_();
        pose.m_85836_();
        pose.m_85837_(0.0, 100000.0, 0.0);
        hidden = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().equals(VanillaGuiOverlay.FOOD_LEVEL.type())) {
            return;
        }
        if (hidden) {
            hidden = false;
            event.getGuiGraphics().m_280168_().m_85849_();
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (!showing(minecraft)) {
            return;
        }
        long now = System.nanoTime();
        float fill = reserve / GhoulReserve.MAX;
        float ghostAlpha = Math.max(0.0F, 1.0F - (now - ghostAt) / (float)GHOST_NANOS);
        float flash = Math.max(0.0F, 1.0F - (now - flashAt) / (float)FLASH_NANOS);
        ReserveBarView.Frame frame = new ReserveBarView.Frame(fill, ghostAlpha > 0.0F ? ghost / GhoulReserve.MAX : fill, ghostAlpha, flash,
            GhoulReserve.hunger(reserve), ControlClient.worldTime(minecraft.m_91296_()));
        int x = event.getWindow().m_85445_() / 2 + 10;
        int y = event.getWindow().m_85446_() - 39;
        ReserveBarView.draw(new GuiCanvas(event.getGuiGraphics(), minecraft.f_91062_, null), x, y, frame);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reserve = -1.0F;
        hidden = false;
        ghostAt = 0L;
        flashAt = 0L;
    }
}
