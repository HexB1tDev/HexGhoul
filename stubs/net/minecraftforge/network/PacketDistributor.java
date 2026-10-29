// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.network;

public class PacketDistributor<T> {
    public static final PacketDistributor<net.minecraft.server.level.ServerPlayer> PLAYER = null;
    public static final PacketDistributor<net.minecraft.world.entity.Entity> TRACKING_ENTITY_AND_SELF = null;

    public PacketTarget with(java.util.function.Supplier<T> input) { throw new UnsupportedOperationException(); }

    public static class PacketTarget {
    }
}
