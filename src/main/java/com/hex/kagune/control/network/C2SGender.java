package com.hex.kagune.control.network;

import com.hex.kagune.control.Gender;
import com.hex.kagune.control.server.GenderServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** 1.3.9: the gender the player picked on the gender screen ({@link Gender} ordinal); the server checks it may be. */
public record C2SGender(int gender) {
    public static void encode(C2SGender message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.gender);
    }

    public static C2SGender decode(FriendlyByteBuf buffer) {
        return new C2SGender(buffer.readByte());
    }

    public static void handle(C2SGender message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> GenderServer.choose(sender, Gender.byOrdinal(message.gender)));
        }
        context.setPacketHandled(true);
    }
}
