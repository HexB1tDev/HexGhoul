package com.hex.kagune.control.network;

import com.hex.kagune.control.client.KingClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.0.7: one of War's rinkaku struck on its own ({@code WarKit}'s instinct): which ({@code tendril}, 0..5), at what body
 * ({@code target}), and whether it lashed out at it or fired a spike. Every client bends that rinkaku at it for a moment.
 */
public record S2CKingLash(UUID playerId, int tendril, int target, boolean spike) {
    public static void encode(S2CKingLash message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.tendril);
        buffer.writeInt(message.target);
        buffer.writeBoolean(message.spike);
    }

    public static S2CKingLash decode(FriendlyByteBuf buffer) {
        return new S2CKingLash(buffer.m_130259_(), buffer.readByte(), buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(S2CKingLash message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KingClient.lash(message)));
        }
        context.setPacketHandled(true);
    }
}
