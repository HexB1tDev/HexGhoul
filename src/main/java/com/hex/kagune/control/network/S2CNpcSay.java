package com.hex.kagune.control.network;

import com.hex.kagune.control.client.NpcSpeech;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.30: an NPC's words to the player it is sent to, shown in a bubble over the NPC's head ({@code NpcSpeech}) instead of the chat:
 * a companion's, a child's, an ally's. 1.3.52 (protocol 69): and how long its bubble stays up ({@code ticks}; 0: as long as it takes to
 * read): a line in a talk comes down as the reply goes up, so two bubbles in turn never overlap.
 */
public record S2CNpcSay(int npc, String text, int ticks) {
    public S2CNpcSay(int npc, String text) {
        this(npc, text, 0);
    }

    public static void encode(S2CNpcSay message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.npc);
        S2CNpcTalk.text(buffer, message.text);
        buffer.writeInt(message.ticks);
    }

    public static S2CNpcSay decode(FriendlyByteBuf buffer) {
        return new S2CNpcSay(buffer.readInt(), S2CNpcTalk.text(buffer), buffer.readInt());
    }

    public static void handle(S2CNpcSay message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcSpeech.accept(message.npc(), message.text(), message.ticks())));
        }
        context.setPacketHandled(true);
    }
}
