// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.event;

public abstract class ScreenEvent extends net.minecraftforge.eventbus.api.Event {
    public static class Opening extends ScreenEvent {
        public net.minecraft.client.gui.screens.Screen getNewScreen() { throw new UnsupportedOperationException(); }
        public void setNewScreen(net.minecraft.client.gui.screens.Screen newScreen) { throw new UnsupportedOperationException(); }
    }
}
