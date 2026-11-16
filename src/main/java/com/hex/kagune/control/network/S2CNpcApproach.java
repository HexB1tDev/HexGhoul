package com.hex.kagune.control.network;

import com.hex.kagune.control.client.NpcApproachOverlay;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.29: someone comes up to the player with something to say ({@code SceneServer}): who ({@code npc}, its name and colour code), what
 * they say, and how long they wait for an answer (ticks; 0 takes the prompt down: answered, or gone). The player answers with the Answer
 * key or by right-clicking them; the talk screen then opens on what they came to say.
 */
public record S2CNpcApproach(int npc, String name, String colour, String line, int ticks) {
    public static void encode(S2CNpcApproach message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.npc);
        S2CNpcTalk.text(buffer, message.name);
        S2CNpcTalk.text(buffer, message.colour);
        S2CNpcTalk.text(buffer, message.line);
        buffer.writeInt(message.ticks);
    }

    public static S2CNpcApproach decode(FriendlyByteBuf buffer) {
        return new S2CNpcApproach(buffer.readInt(), S2CNpcTalk.text(buffer), S2CNpcTalk.text(buffer), S2CNpcTalk.text(buffer), buffer.readInt());
    }

    public static void handle(S2CNpcApproach message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcApproachOverlay.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
