package com.hex.kagune.control.network;

import com.hex.kagune.control.client.EtoSurgeryOverlay;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** 1.3.0: Eto's surgery begins on this player: their screen goes black, it is heard, and she wakes them. */
public record S2CEtoSurgery(boolean ghoul) {
    public static void encode(S2CEtoSurgery message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.ghoul);
    }

    public static S2CEtoSurgery decode(FriendlyByteBuf buffer) {
        return new S2CEtoSurgery(buffer.readBoolean());
    }

    public static void handle(S2CEtoSurgery message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EtoSurgeryOverlay.begin(message.ghoul())));
        }
        context.setPacketHandled(true);
    }
}
