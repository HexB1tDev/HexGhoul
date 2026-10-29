package com.hex.kagune.control.network;

import com.hex.kagune.control.client.PathScreens;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to owner: "your kagune has no path yet; show the starting screen when you can". */
public record S2CPathPrompt() {
    public static void encode(S2CPathPrompt message, FriendlyByteBuf buffer) {
    }

    public static S2CPathPrompt decode(FriendlyByteBuf buffer) {
        return new S2CPathPrompt();
    }

    public static void handle(S2CPathPrompt message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> PathScreens::prompt));
        }
        context.setPacketHandled(true);
    }
}
