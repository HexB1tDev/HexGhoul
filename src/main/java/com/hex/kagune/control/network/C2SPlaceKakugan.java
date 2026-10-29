package com.hex.kagune.control.network;

import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.server.KakuganServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** "My eyes are here", from the kakugan screen. The server validates the positions (they can only ever land on the face). */
public record C2SPlaceKakugan(KakuganEyes eyes) {
    public static void encode(C2SPlaceKakugan message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.eyes.pack());
    }

    public static C2SPlaceKakugan decode(FriendlyByteBuf buffer) {
        return new C2SPlaceKakugan(KakuganEyes.unpack(buffer.readInt()));
    }

    public static void handle(C2SPlaceKakugan message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (!com.hex.kagune.control.DragonLock.locked(sender)) {
                    KakuganServer.place(sender, message.eyes);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
