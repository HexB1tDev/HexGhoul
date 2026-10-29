package net.minecraft.world.entity.projectile;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class Projectile extends Entity {
    protected Projectile(EntityType<? extends Projectile> type, Level level) { super(type, level); }
}
