package com.hex.kagune.control.network;

import com.hex.kagune.control.client.NpcAlertOverlay;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.2: a private word to one player from an NPC they are together with ({@code Romance.Want}): she wants a kiss, a hug, them. Only
 * they see it (a banner at the top of the screen) and hear it (a chime of its own kind), so an absent-minded partner is warned before
 * she is left hanging.
 */
public record S2CNpcAlert(String who, int want, String text) {
    public static void encode(S2CNpcAlert message, FriendlyByteBuf buffer) {
        S2CNpcTalk.text(buffer, message.who);
        buffer.writeByte(message.want);
        S2CNpcTalk.text(buffer, message.text);
    }

    public static S2CNpcAlert decode(FriendlyByteBuf buffer) {
        return new S2CNpcAlert(S2CNpcTalk.text(buffer), buffer.readByte(), S2CNpcTalk.text(buffer));
    }

    public static void handle(S2CNpcAlert message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcAlertOverlay.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
