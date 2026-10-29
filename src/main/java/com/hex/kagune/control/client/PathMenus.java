package com.hex.kagune.control.client;

import com.hex.kagune.control.KagunePath;
import net.minecraft.client.gui.screens.Screen;

/** Each kagune's own menu (the HexKagune menu key). A path without its own menu keeps the original kagune menu. */
public final class PathMenus {
    private PathMenus() {
    }

    /** The menu for {@code path}, or null to keep the original menu (before a path is chosen). */
    public static Screen menuFor(KagunePath path) {
        return switch (path) {
            case UKAKU -> new UkakuMenuScreen();
            case RINKAKU -> new RinkakuMenuScreen();
            case PALE, GRAFT -> new GraftMenuScreen(); // 7.0.0: Owl DNA is dormant (its old players are Graft DNA)
            case NONE -> null;
        };
    }
}
