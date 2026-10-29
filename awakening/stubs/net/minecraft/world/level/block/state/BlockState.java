package net.minecraft.world.level.block.state;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockState extends BlockBehaviour.BlockStateBase {
    public boolean isAir() { throw null; }
    public FluidState getFluidState() { throw null; }
    public boolean hasBlockEntity() { throw null; }
    public float getDestroySpeed(BlockGetter level, BlockPos pos) { throw null; }
    public Block getBlock() { throw null; }
    public VoxelShape getCollisionShape(BlockGetter level, BlockPos pos) { throw null; }
    public boolean ignitedByLava() { throw null; }
}
