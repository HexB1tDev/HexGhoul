package com.hex.kagune.control.network;

import com.hex.kagune.control.client.ReserveHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** The owner's flesh reserve (0 to 20), or below 0 when they live on the food bar instead (3.0.3). */
public record S2CGhoulReserve(float reserve) {
    public static void encode(S2CGhoulReserve message, FriendlyByteBuf buffer) {
        buffer.writeFloat(message.reserve);
    }

    public static S2CGhoulReserve decode(FriendlyByteBuf buffer) {
        return new S2CGhoulReserve(buffer.readFloat());
    }

    public static void handle(S2CGhoulReserve message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ReserveHud.accept(message.reserve())));
        context.setPacketHandled(true);
    }
}
