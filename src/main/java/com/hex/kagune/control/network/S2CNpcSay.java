package com.hex.kagune.control.network;

import com.hex.kagune.control.client.NpcSpeech;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.30: an NPC's words to the player it is sent to, shown in a bubble over the NPC's head ({@code NpcSpeech}) instead of the chat:
 * a companion's, a child's, an ally's.
 */
public record S2CNpcSay(int npc, String text) {
    public static void encode(S2CNpcSay message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.npc);
        S2CNpcTalk.text(buffer, message.text);
    }

    public static S2CNpcSay decode(FriendlyByteBuf buffer) {
        return new S2CNpcSay(buffer.readInt(), S2CNpcTalk.text(buffer));
    }

    public static void handle(S2CNpcSay message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcSpeech.accept(message.npc(), message.text())));
        }
        context.setPacketHandled(true);
    }
}
