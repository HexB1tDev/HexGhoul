package com.hex.kagune.control.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 2.4.5: one of a Rinkaku player's tails lunging at a foe in Thy Death ({@code ThyDeath}): whose ({@code player}, an entity id), which tail
 * ({@code limb}), at what ({@code target}, an entity id; 0 for Thy Death over: every tail back), since when ({@code start}, game ticks).
 */
public record S2CThyDeath(int player, int limb, int target, long start) {
    public static void encode(S2CThyDeath message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.player);
        buffer.writeByte(message.limb);
        buffer.writeInt(message.target);
        buffer.m_130103_(message.start);
    }

    public static S2CThyDeath decode(FriendlyByteBuf buffer) {
        return new S2CThyDeath(buffer.readInt(), buffer.readByte(), buffer.readInt(), buffer.m_130258_());
    }

    public static void handle(S2CThyDeath message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.ThyDeathClient.accept(message)));
        context.setPacketHandled(true);
    }
}
