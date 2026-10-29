package net.minecraft.client.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class AbstractClientPlayer extends Player {
    protected AbstractClientPlayer(Level level) { super(level); }
}
