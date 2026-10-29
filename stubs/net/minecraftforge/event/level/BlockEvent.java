// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event.level;

public class BlockEvent extends net.minecraftforge.eventbus.api.Event {
    public static class BreakEvent extends BlockEvent {
        public BreakEvent(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.entity.player.Player player) { throw new UnsupportedOperationException(); }
    }
}
