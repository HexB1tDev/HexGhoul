package net.minecraftforge.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class TickEvent extends Event {
    public final Phase phase = null;

    public enum Phase { START, END }

    public static class ServerTickEvent extends TickEvent {}

    public static class ClientTickEvent extends TickEvent {}

    public static class PlayerTickEvent extends TickEvent {
        public final Player player = null;
    }

    public static class RenderTickEvent extends TickEvent {
        public final float renderTickTime;

        public RenderTickEvent(float renderTickTime) {
            this.renderTickTime = renderTickTime;
        }
    }
}
