package com.hex.kagune.control.network;

import com.hex.kagune.control.server.FurutaServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 7.0.0: a Furuta DNA input the original channel has no action for (M1 still comes as the original's swipe). {@code targetId} is the
 * entity under the crosshair (0: none); the server checks it like any other hint.
 *
 * @param action one of the constants below
 * @param arg    per action: the dash's way, a key ({@code FurutaMove.Slot} ordinal), a stage
 */
public record C2SFurutaAction(int action, int arg, int targetId) {
    /** Right click with nothing to use: the dash, {@code arg} its way (0 ahead, 1 back, 2 left, 3 right). */
    public static final int STEP = 0;
    /** A move key, {@code arg} the {@code FurutaMove.Slot} it is (the server picks the move the stage has on it). */
    public static final int KEY = 1;
    /** K tapped: into the kakuja or out of it. */
    public static final int KAKUJA = 2;
    /** K held, or the menu: the base stage, {@code arg} its ordinal (-1: the next one unlocked). */
    public static final int STAGE = 3;
    /** The menu: unlock a mutation, {@code arg} its {@code FurutaMutation} ordinal (the server checks the points). */
    public static final int MUTATION = 4;

    public static void encode(C2SFurutaAction message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.action);
        buffer.writeByte(message.arg);
        buffer.writeInt(message.targetId);
    }

    public static C2SFurutaAction decode(FriendlyByteBuf buffer) {
        return new C2SFurutaAction(buffer.readByte(), buffer.readByte(), buffer.readInt());
    }

    public static void handle(C2SFurutaAction message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (!com.hex.kagune.control.DragonLock.locked(sender)) {
                    FurutaServer.onAction(sender, message.action, message.arg, message.targetId);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
