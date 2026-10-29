// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level.block.state;

public abstract class BlockBehaviour {
    public abstract static class BlockStateBase {
        /** isAir */ public boolean m_60795_() { throw new UnsupportedOperationException(); }
        /** getDestroySpeed(level, pos) */ public float m_60800_(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) { throw new UnsupportedOperationException(); }
        /** is(tag) */ public boolean m_204336_(net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag) { throw new UnsupportedOperationException(); }
        /** requiresCorrectToolForDrops */ public boolean m_60834_() { throw new UnsupportedOperationException(); }
        /** getBlock */ public net.minecraft.world.level.block.Block m_60734_() { throw new UnsupportedOperationException(); }
        /** getFluidState */ public net.minecraft.world.level.material.FluidState m_60819_() { throw new UnsupportedOperationException(); }
        /** getCollisionShape(level, pos) */ public net.minecraft.world.phys.shapes.VoxelShape m_60812_(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) { throw new UnsupportedOperationException(); }
    }
}
