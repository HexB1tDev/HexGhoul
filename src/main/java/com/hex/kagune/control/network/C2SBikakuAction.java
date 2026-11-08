package com.hex.kagune.control.network;

import com.hex.kagune.control.server.BikakuServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.5.0: a Bikaku input the original channel has no action for (M1 still comes as the original's swipe). {@code targetId} is the entity
 * under the crosshair (0: none); the server checks it like any other hint.
 *
 * @param action one of the constants below
 * @param arg    per action: a key ({@code BikakuMove.Slot} ordinal)
 */
public record C2SBikakuAction(int action, int arg, int targetId) {
    /** A move key, {@code arg} the {@code BikakuMove.Slot} it is (the server picks the move on it). */
    public static final int KEY = 1;

    public static void encode(C2SBikakuAction message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.action);
        buffer.writeByte(message.arg);
        buffer.writeInt(message.targetId);
    }

    public static C2SBikakuAction decode(FriendlyByteBuf buffer) {
        return new C2SBikakuAction(buffer.readByte(), buffer.readByte(), buffer.readInt());
    }

    public static void handle(C2SBikakuAction message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (!com.hex.kagune.control.DragonLock.locked(sender)) {
                    BikakuServer.onAction(sender, message.action, message.arg, message.targetId);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
