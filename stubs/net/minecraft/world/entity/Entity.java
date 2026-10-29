// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.entity;

public abstract class Entity {
    /** xo, zo (the position a tick ago; 1.2.2) */ public double f_19854_;
    public double f_19856_;
    /** spawnAtLocation(stack) (7.0.2) */ public net.minecraft.world.entity.item.ItemEntity m_19983_(net.minecraft.world.item.ItemStack stack) { throw new UnsupportedOperationException(); }
    /** setCustomName (7.0.2) */ public void m_6593_(net.minecraft.network.chat.Component name) { throw new UnsupportedOperationException(); }
    /** moveTo(x, y, z, yaw, pitch) (7.0.2) */ public void m_7678_(double x, double y, double z, float yaw, float pitch) { throw new UnsupportedOperationException(); }
    /** getPersistentData (Forge's IForgeEntity, 7.0.2) */ public net.minecraft.nbt.CompoundTag getPersistentData() { throw new UnsupportedOperationException(); }
    /** getType */ public EntityType<?> m_6095_() { throw new UnsupportedOperationException(); }
    /** invulnerableTime */ public int f_19802_;
    /** setSecondsOnFire */ public void m_20254_(int seconds) { throw new UnsupportedOperationException(); }
    /** hurtMarked */ public boolean f_19864_;
    /** hasImpulse */ public boolean f_19812_;
    /** fallDistance */ public float f_19789_;
    /** teleportTo(x, y, z) (a ServerPlayer sends it to its client) */ public void m_6021_(double x, double y, double z) { throw new UnsupportedOperationException(); }
    /** setDeltaMovement(x, y, z) */ public void m_20334_(double x, double y, double z) { throw new UnsupportedOperationException(); }
    /** xo */ public double f_19790_;
    /** yo */ public double f_19791_;
    /** zo */ public double f_19792_;
    /** getUUID */ public java.util.UUID m_20148_() { throw new UnsupportedOperationException(); }
    /** getId */ public int m_19879_() { throw new UnsupportedOperationException(); }
    /** level */ public net.minecraft.world.level.Level m_9236_() { throw new UnsupportedOperationException(); }
    /** position */ public net.minecraft.world.phys.Vec3 m_20182_() { throw new UnsupportedOperationException(); }
    /** getX */ public double m_20185_() { throw new UnsupportedOperationException(); }
    /** getY */ public double m_20186_() { throw new UnsupportedOperationException(); }
    /** getZ */ public double m_20189_() { throw new UnsupportedOperationException(); }
    /** getEyePosition */ public net.minecraft.world.phys.Vec3 m_146892_() { throw new UnsupportedOperationException(); }
    /** getEyePosition(partialTick) */ public net.minecraft.world.phys.Vec3 m_20299_(float partialTick) { throw new UnsupportedOperationException(); }
    /** getLookAngle */ public net.minecraft.world.phys.Vec3 m_20154_() { throw new UnsupportedOperationException(); }
    /** getViewVector */ public net.minecraft.world.phys.Vec3 m_20252_(float partialTick) { throw new UnsupportedOperationException(); }
    /** distanceToSqr */ public double m_20280_(Entity other) { throw new UnsupportedOperationException(); }
    /** distanceToSqr(point) */ public double m_20238_(net.minecraft.world.phys.Vec3 point) { throw new UnsupportedOperationException(); }
    /** getBoundingBox */ public net.minecraft.world.phys.AABB m_20191_() { throw new UnsupportedOperationException(); }
    /** getBbWidth */ public float m_20205_() { throw new UnsupportedOperationException(); }
    /** isRemoved */ public boolean m_213877_() { throw new UnsupportedOperationException(); }
    /** getBbHeight */ public float m_20206_() { throw new UnsupportedOperationException(); }
    /** tickCount */ public int f_19797_;
    /** isOnFire */ public boolean m_6060_() { throw new UnsupportedOperationException(); }
    /** isAlive */ public boolean m_6084_() { throw new UnsupportedOperationException(); }
    /** isSpectator */ public boolean m_5833_() { throw new UnsupportedOperationException(); }
    /** isInvisible */ public boolean m_20145_() { throw new UnsupportedOperationException(); }
    /** getDeltaMovement */ public net.minecraft.world.phys.Vec3 m_20184_() { throw new UnsupportedOperationException(); }
    /** setDeltaMovement */ public void m_20256_(net.minecraft.world.phys.Vec3 motion) { throw new UnsupportedOperationException(); }
    /** setPos(Vec3) */ public final void m_146884_(net.minecraft.world.phys.Vec3 position) { throw new UnsupportedOperationException(); }
    /** onGround */ public boolean m_20096_() { throw new UnsupportedOperationException(); }
    /** isInWater */ public boolean m_20072_() { throw new UnsupportedOperationException(); }
    /** discard */ public void m_146870_() { throw new UnsupportedOperationException(); }
    /** isInWater (the touching-water check vanilla's sprint particles use) */ public boolean m_20069_() { throw new UnsupportedOperationException(); }
    /** isSwimming */ public boolean m_6069_() { throw new UnsupportedOperationException(); }
    /** isSprinting */ public boolean m_20142_() { throw new UnsupportedOperationException(); }
    /** isCrouching */ public boolean m_6047_() { throw new UnsupportedOperationException(); }
    /** isPassenger */ public boolean m_20159_() { throw new UnsupportedOperationException(); }
    /** isShiftKeyDown */ public boolean m_6144_() { throw new UnsupportedOperationException(); }
    /** isAlliedTo */ public boolean m_7307_(Entity other) { throw new UnsupportedOperationException(); }
    /** hasCustomName */ public boolean m_8077_() { throw new UnsupportedOperationException(); }
    /** hurt */ public boolean m_6469_(net.minecraft.world.damagesource.DamageSource source, float amount) { throw new UnsupportedOperationException(); }
    /** damageSources */ public net.minecraft.world.damagesource.DamageSources m_269291_() { throw new UnsupportedOperationException(); }
    /** getYRot */ public float m_146908_() { throw new UnsupportedOperationException(); }
    /** yRotO */ public float f_19859_;
    /** getXRot */ public float m_146909_() { throw new UnsupportedOperationException(); }
    /** setXRot */ public void m_146926_(float pitch) { throw new UnsupportedOperationException(); }
    /** setYRot (1.0.41: a ghoul turned to face its prey as it flips) */ public void m_146922_(float yaw) { throw new UnsupportedOperationException(); }
    /** xRotO */ public float f_19860_;
    /** turn(yaw, pitch) in mouse units (x0.15 degrees) */ public void m_19884_(double yaw, double pitch) { throw new UnsupportedOperationException(); }
    /** setInvulnerable (6.2.0) */ public void m_20331_(boolean invulnerable) { throw new UnsupportedOperationException(); }
    /** isInvulnerable (6.2.0) */ public boolean m_20147_() { throw new UnsupportedOperationException(); }
}
