package com.hex.kagune.control.network;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.server.PathServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** "I choose this path", sent once by the starting screen. The server decides whether to accept it. */
public record C2SChoosePath(KagunePath path) {
    public static void encode(C2SChoosePath message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.path.ordinal());
    }

    public static C2SChoosePath decode(FriendlyByteBuf buffer) {
        return new C2SChoosePath(KagunePath.byOrdinal(buffer.readByte()));
    }

    public static void handle(C2SChoosePath message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (!com.hex.kagune.control.DragonLock.locked(sender)) {
                    PathServer.choose(sender, message.path);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
