package net.minecraftforge.event.entity.player;

public class PlayerInteractEvent extends PlayerEvent {
    public static class EntityInteractSpecific extends PlayerInteractEvent {}

    public static class EntityInteract extends PlayerInteractEvent {}

    public static class RightClickBlock extends PlayerInteractEvent {}

    public static class RightClickItem extends PlayerInteractEvent {}

    public static class LeftClickBlock extends PlayerInteractEvent {}
}
