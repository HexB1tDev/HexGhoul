// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level;

/** Real hierarchy: LevelReader -> CollisionGetter -> BlockGetter. */
public interface CollisionGetter extends BlockGetter {
    /** noCollision(entity) */ default boolean m_45786_(net.minecraft.world.entity.Entity entity) { throw new UnsupportedOperationException(); }
    /** getBlockCollisions(entity, box) */ default java.lang.Iterable<net.minecraft.world.phys.shapes.VoxelShape> m_186434_(net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.AABB box) { throw new UnsupportedOperationException(); }
}
