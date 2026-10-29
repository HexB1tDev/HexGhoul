// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.event;

public abstract class RenderGuiOverlayEvent extends net.minecraftforge.eventbus.api.Event {
    public com.mojang.blaze3d.platform.Window getWindow() { throw new UnsupportedOperationException(); }
    public net.minecraft.client.gui.GuiGraphics getGuiGraphics() { throw new UnsupportedOperationException(); }
    public net.minecraftforge.client.gui.overlay.NamedGuiOverlay getOverlay() { throw new UnsupportedOperationException(); }

    public static class Pre extends RenderGuiOverlayEvent {
    }

    public static class Post extends RenderGuiOverlayEvent {
    }
}
