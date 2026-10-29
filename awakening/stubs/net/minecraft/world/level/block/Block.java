package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class Block extends BlockBehaviour {
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_KNOWN_SHAPE = 16;

    public BlockState defaultBlockState() { throw null; }
    public static int getId(BlockState state) { throw null; }
}
