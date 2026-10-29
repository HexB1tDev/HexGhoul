package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public interface BlockGetter {
    BlockState getBlockState(BlockPos pos);
    BlockHitResult clip(ClipContext context);
}
