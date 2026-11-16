package com.hex.kagune.control.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 4.1.50: a player's gang ({@code GangPlayer}: 0 none, 1 Aogiri Tree, 2 the Clowns), told to every client: their name over their head and
 * in chat carries its emblem ({@code GangServer.name}).
 */
public record S2CGangName(UUID player, int gang) {
    public static void encode(S2CGangName message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.player);
        buffer.writeByte(message.gang);
    }

    public static S2CGangName decode(FriendlyByteBuf buffer) {
        return new S2CGangName(buffer.m_130259_(), buffer.readByte());
    }

    public static void handle(S2CGangName message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.GangClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
