package net.minecraft.world.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class AbstractArrow extends Projectile {
    protected AbstractArrow(EntityType<? extends AbstractArrow> type, Level level) { super(type, level); }
}
