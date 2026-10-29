// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event.entity.player;

public class PlayerEvent extends net.minecraftforge.event.entity.living.LivingEvent {
    public net.minecraft.world.entity.player.Player getEntity() { throw new UnsupportedOperationException(); }

    public static class PlayerLoggedInEvent extends PlayerEvent {
    }

    public static class PlayerLoggedOutEvent extends PlayerEvent {
    }

    public static class PlayerRespawnEvent extends PlayerEvent {
    }

    public static class PlayerChangedDimensionEvent extends PlayerEvent {
    }

    public static class StartTracking extends PlayerEvent {
        public net.minecraft.world.entity.Entity getTarget() { throw new UnsupportedOperationException(); }
    }
}
