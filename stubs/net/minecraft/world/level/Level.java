// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level;

public abstract class Level implements LevelAccessor {
    /** OVERWORLD */ public static final net.minecraft.resources.ResourceKey<Level> f_46428_ = null;
    /** dimension */ public net.minecraft.resources.ResourceKey<Level> m_46472_() { throw new UnsupportedOperationException(); }
    /** getBlockEntity(pos) */ public net.minecraft.world.level.block.entity.BlockEntity m_7702_(net.minecraft.core.BlockPos pos) { throw new UnsupportedOperationException(); }
    /** setBlock(pos, state, flags) */ public boolean m_7731_(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, int flags) { throw new UnsupportedOperationException(); }
    /** mayInteract(player, pos): not spawn-protected for them */ public boolean m_7966_(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) { throw new UnsupportedOperationException(); }
    /** playSound */ public void m_6263_(net.minecraft.world.entity.player.Player player, double x, double y, double z, net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) { throw new UnsupportedOperationException(); }
    /** getGameTime */ public long m_46467_() { throw new UnsupportedOperationException(); }
    /** getDayTime */ public long m_46468_() { throw new UnsupportedOperationException(); }
    /** getEntity(int) */ public net.minecraft.world.entity.Entity m_6815_(int id) { throw new UnsupportedOperationException(); }
    /** clip */ public net.minecraft.world.phys.BlockHitResult m_45547_(ClipContext context) { throw new UnsupportedOperationException(); }
    /** addFreshEntity */ public boolean m_7967_(net.minecraft.world.entity.Entity entity) { throw new UnsupportedOperationException(); }
    /** getEntitiesOfClass */ public <T extends net.minecraft.world.entity.Entity> java.util.List<T> m_6443_(Class<T> type, net.minecraft.world.phys.AABB box, java.util.function.Predicate<? super T> filter) { throw new UnsupportedOperationException(); }
}
