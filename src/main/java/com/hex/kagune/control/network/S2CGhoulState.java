package com.hex.kagune.control.network;

import com.hex.kagune.control.client.KakuganClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Whether a player is in ghoul mode (their kakugan open without the kagune), for them and everyone tracking them. */
public record S2CGhoulState(UUID playerId, boolean ghoul) {
    public static void encode(S2CGhoulState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeBoolean(message.ghoul);
    }

    public static S2CGhoulState decode(FriendlyByteBuf buffer) {
        return new S2CGhoulState(buffer.m_130259_(), buffer.readBoolean());
    }

    public static void handle(S2CGhoulState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KakuganClient.acceptGhoul(message)));
        context.setPacketHandled(true);
    }
}
