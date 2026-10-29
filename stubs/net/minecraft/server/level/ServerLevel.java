// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.server.level;

public class ServerLevel extends net.minecraft.world.level.Level {
    /** getServer */ public net.minecraft.server.MinecraftServer m_7654_() { throw new UnsupportedOperationException(); }
    /** getDataStorage */ public net.minecraft.world.level.storage.DimensionDataStorage m_8895_() { throw new UnsupportedOperationException(); }
    /** players */ public java.util.List<ServerPlayer> m_6907_() { throw new UnsupportedOperationException(); }
    /** getBlockState */ public net.minecraft.world.level.block.state.BlockState m_8055_(net.minecraft.core.BlockPos pos) { throw new UnsupportedOperationException(); }
    /** noCollision(entity, box) (CollisionGetter) */ public boolean m_45756_(net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.AABB box) { throw new UnsupportedOperationException(); }
    /** getEntity(int) */ public net.minecraft.world.entity.Entity m_6815_(int id) { throw new UnsupportedOperationException(); }
    /** getEntitiesOfClass */ public <T extends net.minecraft.world.entity.Entity> java.util.List<T> m_6443_(Class<T> type,
        net.minecraft.world.phys.AABB area, java.util.function.Predicate<? super T> filter) { throw new UnsupportedOperationException(); }
    /** sendParticles */ public <T extends net.minecraft.core.particles.ParticleOptions> int m_8767_(T type, double x, double y, double z,
        int count, double dx, double dy, double dz, double speed) { throw new UnsupportedOperationException(); }
}
