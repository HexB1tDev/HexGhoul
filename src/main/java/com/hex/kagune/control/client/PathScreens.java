package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.screen.KaguneScreen;
import com.hex.kagune.control.KagunePath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Opens the path choice screen: when the server prompts (unlock, login, a blocked manifest attempt) and, until a
 * path is chosen, whenever the HexKagune menu key would open the kagune menu. Opens the kakugan screen when the server
 * asks (right after a path is chosen, or {@code /kagune eyes}). A prompt never interrupts another screen (chat,
 * inventory, pause); it waits until the player is back in the world.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PathScreens {
    /** Ticks a server prompt may wait for the player to close other screens (and for their kagune state to arrive). */
    private static final int PROMPT_PATIENCE_TICKS = 20 * 60;
    private static int pendingTicks = -1;
    private static int pendingKakuganTicks = -1;

    private PathScreens() {
    }

    /** Server prompt: show the screen now if the player is free, otherwise as soon as they are. */
    public static void prompt() {
        pendingTicks = 0;
        tryOpen();
    }

    /** Server prompt: show the kakugan screen now if the player is free, otherwise as soon as they are. */
    public static void promptKakugan() {
        pendingKakuganTicks = 0;
        tryOpenKakugan();
    }

    private static void tryOpenKakugan() {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ != null && minecraft.f_91080_ == null && !choiceOwed()) {
            pendingKakuganTicks = -1;
            minecraft.m_91152_(new KakuganScreen());
        }
    }

    private static void tryOpen() {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ != null && minecraft.f_91080_ == null && choiceOwed()) {
            pendingTicks = -1;
            minecraft.m_91152_(new PathChoiceScreen());
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (pendingKakuganTicks >= 0) {
            if (++pendingKakuganTicks > PROMPT_PATIENCE_TICKS) {
                pendingKakuganTicks = -1;
            } else {
                tryOpenKakugan();
            }
        }
        if (pendingTicks < 0) {
            return;
        }
        if (++pendingTicks > PROMPT_PATIENCE_TICKS || PathClient.self().chosen()) {
            pendingTicks = -1; // the next login or manifest attempt prompts again
        } else {
            tryOpen();
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        pendingTicks = -1;
        pendingKakuganTicks = -1;
    }

    /** True while this client's kagune is unlocked but no path has been chosen. */
    public static boolean choiceOwed() {
        return KagunePath.choiceOwed(KaguneClientState.self().unlocked(), PathClient.self());
    }

    /** The menu key: the starting screen while a path is owed, otherwise the path's own menu (if it has one). */
    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof KaguneScreen)) {
            return;
        }
        if (choiceOwed()) {
            event.setNewScreen(new PathChoiceScreen());
            return;
        }
        Screen menu = PathMenus.menuFor(PathClient.self());
        if (menu != null) {
            event.setNewScreen(menu);
        }
    }
}
