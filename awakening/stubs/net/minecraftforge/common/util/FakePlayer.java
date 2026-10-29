package net.minecraftforge.common.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class FakePlayer extends ServerPlayer {
    protected FakePlayer(Level level) { super(level); }
}
