package net.minecraft.world.entity;

import net.minecraft.world.level.Level;

public abstract class Mob extends LivingEntity {
    protected Mob(EntityType<? extends Mob> type, Level level) { super(type, level); }
    public boolean isNoAi() { throw null; }
    public void setNoAi(boolean noAi) { throw null; }
}
