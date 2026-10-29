package net.minecraft.world.entity.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public class Creeper extends Mob {
    public Creeper(EntityType<? extends Creeper> type, Level level) { super(type, level); }
    public void setSwellDir(int direction) { throw null; }
}
