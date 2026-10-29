package net.minecraftforge.network;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class PacketDistributor<T> {
    public static final PacketDistributor<ServerPlayer> PLAYER = null;
    public static final PacketDistributor<Entity> TRACKING_ENTITY_AND_SELF = null;

    public PacketTarget with(Supplier<T> input) { throw null; }

    public static class PacketTarget {}
}
