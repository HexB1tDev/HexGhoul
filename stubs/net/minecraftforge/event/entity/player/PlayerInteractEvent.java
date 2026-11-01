// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event.entity.player;

public class PlayerInteractEvent extends PlayerEvent {
    public net.minecraft.world.InteractionHand getHand() { throw new UnsupportedOperationException(); }

    public void setCancellationResult(net.minecraft.world.InteractionResult result) { throw new UnsupportedOperationException(); }

    /** 1.3.25: an item used in the air (the quinque cases open on it). */
    public static class RightClickItem extends PlayerInteractEvent {
    }

    public static class EntityInteract extends PlayerInteractEvent {
        public net.minecraft.world.entity.Entity getTarget() { throw new UnsupportedOperationException(); }
    }
}
