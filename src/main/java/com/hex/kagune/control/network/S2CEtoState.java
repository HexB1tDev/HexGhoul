package com.hex.kagune.control.network;

import com.hex.kagune.control.client.EtoClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.1.0: what an Eto player's body is doing, for her and everyone tracking her: the move ({@code EtoMove} ordinal), its detail
 * (the strike, the flip), the game tick it began, the entity it is aimed at (0: none), the charge it was let go at, the yaw she
 * faced as it began, and a point (where she blinked to, where the flesh reaches, where the hammer lands). Every client animates
 * the move from these alone.
 */
public record S2CEtoState(UUID playerId, int move, int detail, long startedAt, int targetId, float charge, float yaw, double x, double y,
                          double z) {
    public static void encode(S2CEtoState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.move);
        buffer.writeByte(message.detail);
        buffer.m_130103_(message.startedAt);
        buffer.writeInt(message.targetId);
        buffer.writeFloat(message.charge);
        buffer.writeFloat(message.yaw);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
    }

    public static S2CEtoState decode(FriendlyByteBuf buffer) {
        return new S2CEtoState(buffer.m_130259_(), buffer.readByte(), buffer.readByte(), buffer.m_130258_(), buffer.readInt(), buffer.readFloat(),
            buffer.readFloat(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(S2CEtoState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EtoClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
