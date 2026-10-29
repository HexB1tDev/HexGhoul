package net.minecraft.world.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeEntity;

public abstract class Entity implements IForgeEntity {
    public double xo;
    public double yo;
    public double zo;
    public double xOld;
    public double yOld;
    public double zOld;
    public float yRotO;
    public float xRotO;
    public float fallDistance;
    public int invulnerableTime;
    public boolean hurtMarked;
    public boolean hasImpulse;
    public int tickCount;

    public Entity(EntityType<?> type, Level level) {}
    public UUID getUUID() { throw null; }
    public int getId() { throw null; }
    public double getX() { throw null; }
    public double getY() { throw null; }
    public double getZ() { throw null; }
    public boolean isRemoved() { throw null; }
    public boolean isPassenger() { throw null; }
    public void stopRiding() { throw null; }
    public void setNoGravity(boolean noGravity) { throw null; }
    public boolean isNoGravity() { throw null; }
    public void setDeltaMovement(Vec3 motion) { throw null; }
    public Vec3 getDeltaMovement() { throw null; }
    public void setDeltaMovement(double x, double y, double z) { throw null; }
    public void clearFire() { throw null; }
    public boolean isInLava() { throw null; }
    public void teleportTo(double x, double y, double z) { throw null; }
    public float getBbWidth() { throw null; }
    public float getBbHeight() { throw null; }
    public boolean isAlive() { throw null; }
    public boolean isSpectator() { throw null; }
    public boolean isInvulnerable() { throw null; }
    public Level level() { throw null; }
    public void move(MoverType type, Vec3 motion) { throw null; }
    public DamageSources damageSources() { throw null; }
    public boolean hurt(DamageSource source, float amount) { throw null; }
    public Vec3 getEyePosition(float partialTick) { throw null; }
    public Vec3 getViewVector(float partialTick) { throw null; }
    public float getYRot() { throw null; }
    public float getXRot() { throw null; }
    public void setYRot(float yaw) { throw null; }
    public void setXRot(float pitch) { throw null; }
    public void setPos(double x, double y, double z) { throw null; }
    @Override public CompoundTag getPersistentData() { throw null; }
}
