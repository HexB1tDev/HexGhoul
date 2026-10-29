// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level;

/** Real hierarchy: LevelReader -> BlockAndTintGetter, CollisionGetter, ... -> BlockGetter. */
public interface LevelReader extends BlockGetter, CollisionGetter {
    /** getHeight(type, x, z) */ default int m_6924_(net.minecraft.world.level.levelgen.Heightmap.Types type, int x, int z) { throw new UnsupportedOperationException(); }
    /** holderLookup(registry) */ default <T> net.minecraft.core.HolderLookup<T> m_246945_(
        net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends T>> registry) { throw new UnsupportedOperationException(); }
    /** hasChunkAt(pos) */ default boolean m_46805_(net.minecraft.core.BlockPos pos) { throw new UnsupportedOperationException(); }
}
