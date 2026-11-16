package com.hex.kagune.control.network;

import com.hex.kagune.control.client.PregnancyClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.10: whether a woman is carrying a child, for every client that sees her: since which game tick ({@code Long.MIN_VALUE}: she is
 * not). Each client works out how far along she is (her belly) from its own clock.
 */
public record S2CPregnancy(UUID body, long conceived) {
    public static void encode(S2CPregnancy message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.body);
        buffer.m_130103_(message.conceived);
    }

    public static S2CPregnancy decode(FriendlyByteBuf buffer) {
        return new S2CPregnancy(buffer.m_130259_(), buffer.m_130258_());
    }

    public static void handle(S2CPregnancy message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PregnancyClient.accept(message)));
        context.setPacketHandled(true);
    }
}
