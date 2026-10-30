package com.hex.kagune.control.network;

import com.hex.kagune.control.client.KakuganClient;
import com.hex.kagune.control.kakugan.KakuganEyes;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Where one player's eyes are on their skin (for their kakugan), to that player and everyone tracking them; 1.3.0: and whether they may
 * wear one kakugan (Eto made them like her).
 */
public record S2CKakuganState(UUID playerId, KakuganEyes eyes, boolean placed, boolean granted) {
    public static void encode(S2CKakuganState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeInt(message.eyes.pack());
        buffer.writeBoolean(message.placed);
        buffer.writeBoolean(message.granted);
    }

    public static S2CKakuganState decode(FriendlyByteBuf buffer) {
        return new S2CKakuganState(buffer.m_130259_(), KakuganEyes.unpack(buffer.readInt()), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(S2CKakuganState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KakuganClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
