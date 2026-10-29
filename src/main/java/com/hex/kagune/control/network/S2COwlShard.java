package com.hex.kagune.control.network;

import com.hex.kagune.control.client.OwlShardClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.2.0: one of the Owl's volley spears ({@code OwlShards}), for her and everyone tracking her: {@link #THROWN} (where from, the game
 * tick, how fast: every client flies it from these alone), {@link #STUCK} (where its point stuck, the way it points, what it pins, 0:
 * nothing), {@link #GONE} (crumbled), or {@link #CLEAR} (all of hers gone at once).
 */
public record S2COwlShard(UUID playerId, int id, int kind, long tick, double x, double y, double z, float dx, float dy, float dz, int body) {
    public static final int THROWN = 0;
    public static final int STUCK = 1;
    public static final int GONE = 2;
    public static final int CLEAR = 3;

    public static void encode(S2COwlShard message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeInt(message.id);
        buffer.writeByte(message.kind);
        buffer.m_130103_(message.tick);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.dx);
        buffer.writeFloat(message.dy);
        buffer.writeFloat(message.dz);
        buffer.writeInt(message.body);
    }

    public static S2COwlShard decode(FriendlyByteBuf buffer) {
        return new S2COwlShard(buffer.m_130259_(), buffer.readInt(), buffer.readByte(), buffer.m_130258_(), buffer.readDouble(), buffer.readDouble(),
            buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt());
    }

    public static void handle(S2COwlShard message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> OwlShardClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
