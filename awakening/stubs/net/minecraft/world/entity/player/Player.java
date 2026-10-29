package net.minecraft.world.entity.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public abstract class Player extends LivingEntity {
    protected Player(Level level) { super(null, level); }
    public boolean isCreative() { throw null; }
    public GameProfile getGameProfile() { throw null; }
    public void displayClientMessage(Component message, boolean actionBar) { throw null; }
}
