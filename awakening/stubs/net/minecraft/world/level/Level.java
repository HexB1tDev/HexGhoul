package net.minecraft.world.level;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public abstract class Level implements LevelAccessor, BlockAndTintGetter {
    public long getGameTime() { throw null; }
    public void playSound(Player player, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch) { throw null; }
    public boolean mayInteract(Player player, BlockPos pos) { throw null; }
    public boolean setBlock(BlockPos pos, BlockState state, int flags) { throw null; }
    @Override public BlockState getBlockState(BlockPos pos) { throw null; }
    @Override public BlockHitResult clip(ClipContext context) { throw null; }
    public boolean isLoaded(BlockPos pos) { throw null; }
    public void levelEvent(Player player, int type, BlockPos pos, int data) { throw null; }
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type, AABB area, Predicate<? super T> filter) { throw null; }
    public int getMinBuildHeight() { throw null; }
    public int getMaxBuildHeight() { throw null; }
    public void addParticle(ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) { throw null; }
}
