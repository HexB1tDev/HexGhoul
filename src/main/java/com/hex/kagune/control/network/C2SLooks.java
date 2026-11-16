package com.hex.kagune.control.network;

import com.hex.kagune.control.server.Looks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.10: the main colour of the top of this player's head and their skin tone, read off their own skin by their client (-1: none could be
 * told); a child of theirs gets each, blended with its other parent's.
 */
public record C2SLooks(int hair, int skin) {
    public static void encode(C2SLooks message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.hair);
        buffer.writeInt(message.skin);
    }

    public static C2SLooks decode(FriendlyByteBuf buffer) {
        return new C2SLooks(buffer.readInt(), buffer.readInt());
    }

    public static void handle(C2SLooks message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> Looks.set(sender, message.hair, message.skin));
        }
        context.setPacketHandled(true);
    }
}
