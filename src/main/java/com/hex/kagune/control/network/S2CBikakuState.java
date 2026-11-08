package com.hex.kagune.control.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.4.9: what a Bikaku player's worm is doing, for them and everyone tracking them: the bite of the M1 string it is making
 * ({@code BikakuRules.Bite} ordinal, 0 none) and the game tick it began, where it is thrown (a point in the world: the target's middle when
 * it began, or the air ahead), and the body it goes after (an entity id, 0 none: the clients follow it as it moves). Every client draws
 * the worm from these alone.
 */
public record S2CBikakuState(UUID playerId, int bite, long startedAt, double x, double y, double z, int targetId) {
    public static void encode(S2CBikakuState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.bite);
        buffer.m_130103_(message.startedAt);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeInt(message.targetId);
    }

    public static S2CBikakuState decode(FriendlyByteBuf buffer) {
        return new S2CBikakuState(buffer.m_130259_(), buffer.readByte(), buffer.m_130258_(), buffer.readDouble(), buffer.readDouble(),
            buffer.readDouble(), buffer.readInt());
    }

    public static void handle(S2CBikakuState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.BikakuClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
