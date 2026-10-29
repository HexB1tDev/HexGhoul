// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event;

public class TickEvent extends net.minecraftforge.eventbus.api.Event {
    public final Phase phase = null;

    public enum Phase {
        START, END
    }

    public static class ClientTickEvent extends TickEvent {
    }

    public static class PlayerTickEvent extends TickEvent {
        public final net.minecraft.world.entity.player.Player player = null;
    }

    public static class LevelTickEvent extends TickEvent {
        public final net.minecraft.world.level.Level level = null;
    }
    public static class RenderTickEvent extends TickEvent {
        public float renderTickTime;
    }
}
