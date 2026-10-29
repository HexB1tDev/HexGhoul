package net.minecraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.Entity;

public class Minecraft {
    public LocalPlayer player;
    public ClientLevel level;
    public Options options;
    public GameRenderer gameRenderer;
    public Screen screen;
    public Font font;

    public static Minecraft getInstance() { throw null; }
    public void setScreen(Screen screen) { throw null; }
    public RenderBuffers renderBuffers() { throw null; }
    public void setCameraEntity(Entity entity) { throw null; }
}
