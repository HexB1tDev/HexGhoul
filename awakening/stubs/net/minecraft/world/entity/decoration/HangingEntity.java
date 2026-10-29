package net.minecraft.world.entity.decoration;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class HangingEntity extends Entity {
    protected HangingEntity(EntityType<? extends HangingEntity> type, Level level) { super(type, level); }
}
