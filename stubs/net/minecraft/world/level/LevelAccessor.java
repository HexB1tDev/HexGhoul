// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level;

/** Real hierarchy: LevelAccessor -> CommonLevelAccessor -> LevelReader, LevelSimulatedRW -> LevelWriter. */
public interface LevelAccessor extends LevelReader, LevelWriter {
    /** levelEvent(type, pos, data) */ default void m_46796_(int type, net.minecraft.core.BlockPos pos, int data) { throw new UnsupportedOperationException(); }
}
