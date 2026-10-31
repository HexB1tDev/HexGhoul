package com.hex.kagune.control.network;

import com.hex.kagune.control.client.GenderClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** 1.3.9: a player's gender ({@code Gender} ordinal), for them and every client that sees them (it changes how they are drawn and boxed). */
public record S2CGender(UUID player, int gender) {
    public static void encode(S2CGender message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.player);
        buffer.writeByte(message.gender);
    }

    public static S2CGender decode(FriendlyByteBuf buffer) {
        return new S2CGender(buffer.m_130259_(), buffer.readByte());
    }

    public static void handle(S2CGender message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GenderClient.accept(message)));
        context.setPacketHandled(true);
    }
}
