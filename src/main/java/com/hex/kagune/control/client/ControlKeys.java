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
 * key (R) is pressed, the kagune comes out (or, out, everything goes away) rather than the kakugan opening or the kagune going
 * back to kakugan mode. 1.2.0: Left Ctrl by default (Left Alt before), under a new name so every player gets the new default
 * (the old binding, saved as {@code key.hexkagune.unleash}, is left behind); rebind it in Controls.
 */
public final class ControlKeys {
    /** GLFW_KEY_LEFT_CONTROL. */
    private static final int LEFT_CONTROL = 341;
    public static final KeyMapping UNLEASH = new KeyMapping("key.hexkagune.kagune_out", InputConstants.Type.KEYSYM, LEFT_CONTROL, "key.categories.hexkagune");
    /** 7.0.1: GLFW_KEY_U; the quest journal ({@link QuestScreen}). */
    private static final int KEY_U = 85;
    public static final KeyMapping QUESTS = new KeyMapping("key.hexkagune.quests", InputConstants.Type.KEYSYM, KEY_U, "key.categories.hexkagune");

    private ControlKeys() {
    }

    /**
     * True while the unleash key is held. Bound to Ctrl (the default, 1.2.0) or Alt, either Ctrl (or Alt) is read straight from the
     * keyboard, whatever else is held with it: the key binding's own pressed state missed a lone modifier for players (5.0.1, Alt
     * took Ctrl + Alt). Any other key it is bound to counts while the binding sees it down.
     */
    public static boolean unleashHeld() {
        String bound = UNLEASH.m_90865_();
        if ("key.keyboard.left.control".equals(bound) || "key.keyboard.right.control".equals(bound)) {
            return Screen.m_96637_() || UNLEASH.m_90857_();
        }
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
