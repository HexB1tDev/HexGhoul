package com.hex.kagune.control.network;

import com.hex.kagune.control.server.PlayerArms;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.0.6: a player with a kagune-made weapon out ({@code Arm}) asks for {@link #STRIKE}, the next blow of its string (M1), or one of its
 * two moves, {@link #FIRST} (right click) or {@link #SECOND} (Shift + right click), with the body under the crosshair ({@code target}, an
 * entity id; 0: none). The server checks everything ({@code PlayerArms}).
 */
public record C2SArmAction(int kind, int target) {
    public static final int STRIKE = 0;
    public static final int FIRST = 1;
    public static final int SECOND = 2;

    public static void encode(C2SArmAction message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.kind);
        buffer.writeInt(message.target);
    }

    public static C2SArmAction decode(FriendlyByteBuf buffer) {
        return new C2SArmAction(buffer.readByte(), buffer.readInt());
    }

    public static void handle(C2SArmAction message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> PlayerArms.request(sender, message.kind, message.target));
        }
        context.setPacketHandled(true);
    }
}
