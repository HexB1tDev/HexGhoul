package net.minecraft.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

public abstract class LivingEntity extends Entity {
    protected LivingEntity(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }
    @Override public boolean hurt(DamageSource source, float amount) { throw null; }
    public float getHealth() { throw null; }
    public float getMaxHealth() { throw null; }
    public void setHealth(float health) { throw null; }
    public boolean isSleeping() { throw null; }
    public void stopSleeping() { throw null; }
}
