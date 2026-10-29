package net.minecraftforge.client.event;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.eventbus.api.Event;

public class RenderGuiEvent extends Event {
    public Window getWindow() { throw null; }
    public GuiGraphics getGuiGraphics() { throw null; }

    public static class Post extends RenderGuiEvent {}
}
