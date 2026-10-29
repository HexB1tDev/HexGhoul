package com.hex.kagune.control.network;

import com.hex.kagune.control.client.WatchClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.2.0: one of an Eto player's Watch This tentacles, for her and everyone tracking her, sent each time it starts something new:
 * which one ({@code side}: 1 over her right shoulder, -1 the left), what it is doing ({@code WatchRules.Phase} ordinal) and the
 * game tick it began, the body it strikes or carries (0: none), how many ticks its lunge takes, and a point: for a carry and the
 * follow-through after it, where the body went on the beak (blocks to her right, up, ahead, in the frame she faced, {@code yaw});
 * for a miss, where the beak got to (the world). Every client moves the tentacle from these alone.
 */
public record S2CEtoWatch(UUID playerId, int side, int phase, long startedAt, int targetId, int lunge, float yaw, double x, double y, double z) {
    public static void encode(S2CEtoWatch message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.side);
        buffer.writeByte(message.phase);
        buffer.m_130103_(message.startedAt);
        buffer.writeInt(message.targetId);
        buffer.writeByte(message.lunge);
        buffer.writeFloat(message.yaw);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
    }

    public static S2CEtoWatch decode(FriendlyByteBuf buffer) {
        return new S2CEtoWatch(buffer.m_130259_(), buffer.readByte(), buffer.readByte(), buffer.m_130258_(), buffer.readInt(), buffer.readByte(),
            buffer.readFloat(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(S2CEtoWatch message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WatchClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
