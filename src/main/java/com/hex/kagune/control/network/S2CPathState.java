package com.hex.kagune.control.network;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.PathClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** One player's chosen kagune path, sent to that player and everyone tracking them (renderers need it). */
public record S2CPathState(UUID playerId, KagunePath path) {
    public static void encode(S2CPathState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.path.ordinal());
    }

    public static S2CPathState decode(FriendlyByteBuf buffer) {
        return new S2CPathState(buffer.m_130259_(), KagunePath.byOrdinal(buffer.readByte()));
    }

    public static void handle(S2CPathState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server, never from another player's client
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PathClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
