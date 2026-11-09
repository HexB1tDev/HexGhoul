package com.hex.kagune.control.network;

import com.hex.kagune.control.server.BikakuServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.5.0: a Bikaku input the original channel has no action for (3.5.8: M1 too, held and let go, out of the kakuja; in it, the original's swipe). {@code targetId} is the entity
 * under the crosshair (0: none); the server checks it like any other hint.
 *
 * @param action one of the constants below
 * @param arg    per action: a key ({@code BikakuMove.Slot} ordinal)
 */
public record C2SBikakuAction(int action, int arg, int targetId) {
    /** A move key, {@code arg} the {@code BikakuMove.Slot} it is (the server picks the move on it). 3.5.8: pressed (a thrown move winds up). */
    public static final int KEY = 1;
    /** 3.5.8: that move key let go ({@code arg} its slot): a wound-up move is thrown as far as it was held for. */
    public static final int KEY_UP = 2;
    /** 3.5.8: M1 pressed (the worm winds up a bite) and let go (thrown as far as it was held for); {@code targetId} under the crosshair. */
    public static final int ATTACK = 3;
    public static final int ATTACK_UP = 4;
    /** 3.6.0: a form picked in the menu ({@code arg} its {@code BikakuForm} ordinal). */
    public static final int FORM = 5;
    /** 3.6.3: a mutation picked in the menu ({@code slot}: its {@code BikakuMutation} ordinal), paid with a mutation point. */
    public static final int MUTATION = 6;

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
