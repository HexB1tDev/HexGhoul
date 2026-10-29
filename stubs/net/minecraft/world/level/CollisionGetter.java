// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level;

/** Real hierarchy: LevelReader -> CollisionGetter -> BlockGetter. */
public interface CollisionGetter extends BlockGetter {
    /** noCollision(entity) */ default boolean m_45786_(net.minecraft.world.entity.Entity entity) { throw new UnsupportedOperationException(); }
}
