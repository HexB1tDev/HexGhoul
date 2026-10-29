package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;

public class PlayerEvent extends LivingEvent {
    @Override public Player getEntity() { throw null; }

    public static class Clone extends PlayerEvent {
        public Player getOriginal() { throw null; }
    }

    public static class PlayerLoggedInEvent extends PlayerEvent {}

    public static class PlayerLoggedOutEvent extends PlayerEvent {}

    public static class PlayerRespawnEvent extends PlayerEvent {}

    public static class PlayerChangedDimensionEvent extends PlayerEvent {}
}
