package com.hex.kagune.control.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 4.2.2 (the owner: "bind a new key for all Kagunes that is called regeneration, except for the Bikaku ... You need to hold the key down"): the
 * regeneration key down ({@code down}: as it goes down and every half second while held) or up. The server regenerates a ghoul only while it
 * is held (GhoulRegen).
 */
public record C2SRegen(boolean down) {
    public static void encode(C2SRegen message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.down);
    }

    public static C2SRegen decode(FriendlyByteBuf buffer) {
        return new C2SRegen(buffer.readBoolean());
    }

    public static void handle(C2SRegen message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> com.hex.kagune.control.server.GhoulRegen.held(sender, message.down));
        }
        context.setPacketHandled(true);
    }
}
