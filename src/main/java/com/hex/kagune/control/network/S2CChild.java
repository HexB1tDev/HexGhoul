package com.hex.kagune.control.network;

import com.hex.kagune.control.client.ChildClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.10: a child as its clients draw it: its name, what it inherited ({@code Genome.save}: its looks), how grown it is ({@code ChildStage}
 * ordinal), who carries it (an entity id, 0 no one), and whether its kakugan shows (it fights).
 */
public record S2CChild(int id, String name, String genome, int stage, int carrier, boolean turned) {
    public static void encode(S2CChild message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.id);
        S2CNpcTalk.text(buffer, message.name);
        S2CNpcTalk.text(buffer, message.genome);
        buffer.writeByte(message.stage);
        buffer.writeInt(message.carrier);
        buffer.writeBoolean(message.turned);
    }

    public static S2CChild decode(FriendlyByteBuf buffer) {
        return new S2CChild(buffer.readInt(), S2CNpcTalk.text(buffer), S2CNpcTalk.text(buffer), buffer.readByte(), buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(S2CChild message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChildClient.accept(message)));
        context.setPacketHandled(true);
    }
}
