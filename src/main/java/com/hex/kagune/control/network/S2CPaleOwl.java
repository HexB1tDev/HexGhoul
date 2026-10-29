package com.hex.kagune.control.network;

import com.hex.kagune.control.client.OwlClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.2.0: an Pale player's Owl (her first kakuja), for her and everyone tracking her: whether she is growing it or turning back
 * ({@code on}), since which game tick.
 */
public record S2CPaleOwl(UUID playerId, boolean on, long at) {
    public static void encode(S2CPaleOwl message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeBoolean(message.on);
        buffer.m_130103_(message.at);
    }

    public static S2CPaleOwl decode(FriendlyByteBuf buffer) {
        return new S2CPaleOwl(buffer.m_130259_(), buffer.readBoolean(), buffer.m_130258_());
    }

    public static void handle(S2CPaleOwl message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> OwlClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
