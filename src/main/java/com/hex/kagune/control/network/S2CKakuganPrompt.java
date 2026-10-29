package com.hex.kagune.control.network;

import com.hex.kagune.control.client.PathScreens;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to owner: "place your kakugan when you can" ({@code /kagune eyes}, or a chosen path whose eyes were never placed). */
public record S2CKakuganPrompt() {
    public static void encode(S2CKakuganPrompt message, FriendlyByteBuf buffer) {
    }

    public static S2CKakuganPrompt decode(FriendlyByteBuf buffer) {
        return new S2CKakuganPrompt();
    }

    public static void handle(S2CKakuganPrompt message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> PathScreens::promptKakugan));
        }
        context.setPacketHandled(true);
    }
}
