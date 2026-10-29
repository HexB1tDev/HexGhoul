package com.hex.kagune.control.network;

import com.hex.kagune.control.client.OwlLanceClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.2.0: one of the Owl's Watch This lances ({@code OwlLances}), for her and everyone tracking her, sent each time it starts
 * something new: which ({@code index}), what it is doing ({@code OwlLances.Phase} ordinal) and the game tick it began, the body it
 * dives on, pins or hoists (0: none), how many ticks its dive takes, and where its point was as a slam or a miss began (the world).
 * Every client moves the lance from these alone.
 */
public record S2COwlLance(UUID playerId, int index, int phase, long startedAt, int targetId, int dive, double x, double y, double z) {
    public static void encode(S2COwlLance message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.index);
        buffer.writeByte(message.phase);
        buffer.m_130103_(message.startedAt);
        buffer.writeInt(message.targetId);
        buffer.writeByte(message.dive);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
    }

    public static S2COwlLance decode(FriendlyByteBuf buffer) {
        return new S2COwlLance(buffer.m_130259_(), buffer.readByte(), buffer.readByte(), buffer.m_130258_(), buffer.readInt(), buffer.readByte(),
            buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(S2COwlLance message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> OwlLanceClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
