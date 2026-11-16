package com.hex.kagune.control.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 4.1.99: {@code /kagune lag} told to the asker's game: measure the mod's side here for {@code seconds} and print it in the chat
 * ({@code log} false); or, {@code log} true, the minute by minute log on ({@code seconds} its window) or off (0).
 */
public record S2CLag(int seconds, boolean log) {
    public static void encode(S2CLag message, FriendlyByteBuf buffer) {
        buffer.writeShort(message.seconds);
        buffer.writeBoolean(message.log);
    }

    public static S2CLag decode(FriendlyByteBuf buffer) {
        return new S2CLag(buffer.readShort(), buffer.readBoolean());
    }

    public static void handle(S2CLag message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.LagClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
