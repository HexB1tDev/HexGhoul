package com.hex.kagune.control.network;

import com.hex.kagune.control.client.GhoulMeatClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** 7.0.1: how many pieces of Ghoul Meat the owner has devoured (the tooltip shows what they did and what the next one adds). */
public record S2CGhoulMeat(int eaten) {
    public static void encode(S2CGhoulMeat message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.eaten);
    }

    public static S2CGhoulMeat decode(FriendlyByteBuf buffer) {
        return new S2CGhoulMeat(buffer.readInt());
    }

    public static void handle(S2CGhoulMeat message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GhoulMeatClient.accept(message.eaten())));
        context.setPacketHandled(true);
    }
}
