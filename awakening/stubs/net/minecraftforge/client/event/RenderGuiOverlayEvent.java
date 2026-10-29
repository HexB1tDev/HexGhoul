package net.minecraftforge.client.event;

import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.eventbus.api.Event;

public class RenderGuiOverlayEvent extends Event {
    public NamedGuiOverlay getOverlay() { throw null; }

    public static class Pre extends RenderGuiOverlayEvent {}
}
