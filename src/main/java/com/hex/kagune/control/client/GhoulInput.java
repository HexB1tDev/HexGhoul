package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.network.C2SGhoulKey;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The kagune key (R) since 3.0.0: R alone is ghoul mode (the kakugan opens without the kagune; R again closes it; 1.2.0: with the
 * kagune out, back to kakugan mode, the eyes left open), Ctrl + R unleashes the kagune or, out, puts everything away (Ctrl is the
 * Kagune key since 1.2.0, Alt before; rebindable in Controls since 5.0.0). Runs before the original's key handling and takes the presses first, so the original never sees them; the server
 * decides ({@code GhoulServer}).
 *
 * <p>5.0.3: the key press itself is read from the keyboard ({@link #key}), not only from the key binding's clicks. With Alt held,
 * Forge hands an R press to whatever is bound to Alt + R (another mod's key, a binding in Controls) and the kagune key never
 * hears it: Alt + R did nothing, while Ctrl + Alt + R worked (Forge then looks for Ctrl + R instead). The binding's own clicks
 * still count for a kagune key bound to a mouse button; a press both ways is sent once.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class GhoulInput {
    /** GLFW_PRESS: a key going down (not held, not released). */
    private static final int PRESS = 1;
    /** Presses already sent from the keyboard this tick, so the binding's click for the same press isn't sent again. */
    private static int sent;

    private GhoulInput() {
    }

    /** A key went down: the kagune key, read straight from the keyboard whatever else is bound to it with a modifier. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void key(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (event.getAction() != PRESS || minecraft.f_91074_ == null || minecraft.f_91080_ != null || !KaguneClientState.self().unlocked()
            || !KaguneKeys.TOGGLE.m_90832_(event.getKey(), event.getScanCode())) {
            return;
        }
        ControlNetwork.CHANNEL.sendToServer(new C2SGhoulKey(ControlKeys.unleashHeld()));
        sent++;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        // A human's presses are left to the original (it discards them); so is anything while a screen is open.
        if (minecraft.f_91074_ == null || minecraft.f_91080_ != null || !KaguneClientState.self().unlocked()) {
            sent = 0;
            return;
        }
        while (KaguneKeys.TOGGLE.m_90859_()) {
            if (sent > 0) {
                sent--;
            } else {
                ControlNetwork.CHANNEL.sendToServer(new C2SGhoulKey(ControlKeys.unleashHeld()));
            }
        }
        sent = 0;
    }
}
