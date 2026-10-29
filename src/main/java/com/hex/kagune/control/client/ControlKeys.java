package com.hex.kagune.control.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The expansion's own keys, in Minecraft's Controls under the HexKagune category (5.0.0). {@link #UNLEASH}: held while the kagune
 * key (R) is pressed, the kagune is unleashed rather than the kakugan opened. Left Alt by default; rebind it in Controls.
 */
public final class ControlKeys {
    /** GLFW_KEY_LEFT_ALT. */
    private static final int LEFT_ALT = 342;
    public static final KeyMapping UNLEASH = new KeyMapping("key.hexkagune.unleash", InputConstants.Type.KEYSYM, LEFT_ALT, "key.categories.hexkagune");
    /** 7.0.1: GLFW_KEY_U; the quest journal ({@link QuestScreen}). */
    private static final int KEY_U = 85;
    public static final KeyMapping QUESTS = new KeyMapping("key.hexkagune.quests", InputConstants.Type.KEYSYM, KEY_U, "key.categories.hexkagune");

    private ControlKeys() {
    }

    /**
     * True while the unleash key is held. Bound to Alt (the default), either Alt is read straight from the keyboard, whatever else
     * is held with it: the key binding's own pressed state missed a lone Alt for players (5.0.1, it took Ctrl + Alt). Any other
     * key it is bound to counts while the binding sees it down.
     */
    public static boolean unleashHeld() {
        String bound = UNLEASH.m_90865_();
        if ("key.keyboard.left.alt".equals(bound) || "key.keyboard.right.alt".equals(bound)) {
            return Screen.m_96639_() || UNLEASH.m_90857_();
        }
        return UNLEASH.m_90857_();
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(UNLEASH);
            event.register(QUESTS);
        }
    }
}
