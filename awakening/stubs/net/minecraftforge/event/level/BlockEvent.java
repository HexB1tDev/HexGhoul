package net.minecraftforge.event.level;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class BlockEvent extends Event {
    public static class BreakEvent extends BlockEvent {
        public Player getPlayer() { throw null; }
    }
}
