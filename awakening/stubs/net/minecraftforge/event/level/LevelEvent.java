package net.minecraftforge.event.level;

import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;

public class LevelEvent extends Event {
    public LevelAccessor getLevel() { throw null; }

    public static class Unload extends LevelEvent {}
}
