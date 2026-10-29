package com.hex.kagune.control.network;

import com.hex.kagune.control.client.ReviveClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.2.0: an Pale player's own mutations, for her menu: which she has unlocked (a bitmask of {@code PaleMutation}) and the game tick
 * her death that does not take is ready again.
 */
public record S2CPaleMutations(int mask, long reviveReadyAt) {
    public static void encode(S2CPaleMutations message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.mask);
        buffer.m_130103_(message.reviveReadyAt);
    }

    public static S2CPaleMutations decode(FriendlyByteBuf buffer) {
        return new S2CPaleMutations(buffer.readInt(), buffer.m_130258_());
    }

    public static void handle(S2CPaleMutations message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ReviveClient.mutations(message)));
        }
        context.setPacketHandled(true);
    }
}
