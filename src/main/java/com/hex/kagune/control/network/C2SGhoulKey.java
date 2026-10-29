package com.hex.kagune.control.network;

import com.hex.kagune.control.server.GhoulServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * The kagune key (R) was pressed, with Alt held ({@code unleash}: the kagune comes out) or not (ghoul mode, or everything in);
 * 1.0.22: in a final kakuja, it takes it off.
 */
public record C2SGhoulKey(boolean unleash) {
    public static void encode(C2SGhoulKey message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.unleash);
    }

    public static C2SGhoulKey decode(FriendlyByteBuf buffer) {
        return new C2SGhoulKey(buffer.readBoolean());
    }

    public static void handle(C2SGhoulKey message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (com.hex.kagune.control.DragonLock.dragon(sender)) {
                    return;
                }
                if (com.hex.kagune.control.server.KingServer.locks(sender.m_20148_())) {
                    // 1.0.22: R takes a final kakuja off, as it puts any other form away.
                    com.hex.kagune.control.server.KingServer.off(sender, true);
                    return;
                }
                GhoulServer.press(sender, message.unleash);
            });
        }
        context.setPacketHandled(true);
    }
}
