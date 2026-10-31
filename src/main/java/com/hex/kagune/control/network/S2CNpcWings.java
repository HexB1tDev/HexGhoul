package com.hex.kagune.control.network;

import com.hex.kagune.control.client.NpcWingsClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.19: a companion's Ukaku wings (Touka's) as her clients draw them: out or going in and since when, crystal or not and since when,
 * what they aim at, the last spike's tick and wing (1 right, -1 left), and since when a volley has them curled forward to aim
 * ({@code Long.MIN_VALUE}: not). Game ticks.
 */
public record S2CNpcWings(int id, boolean out, long outAt, boolean crystal, long crystalAt, int prey, long shotAt, int shotSide, long volleyAt) {
    public static void encode(S2CNpcWings message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.id);
        buffer.writeBoolean(message.out);
        buffer.m_130103_(message.outAt);
        buffer.writeBoolean(message.crystal);
        buffer.m_130103_(message.crystalAt);
        buffer.writeInt(message.prey);
        buffer.m_130103_(message.shotAt);
        buffer.writeByte(message.shotSide);
        buffer.m_130103_(message.volleyAt);
    }

    public static S2CNpcWings decode(FriendlyByteBuf buffer) {
        return new S2CNpcWings(buffer.readInt(), buffer.readBoolean(), buffer.m_130258_(), buffer.readBoolean(), buffer.m_130258_(), buffer.readInt(),
            buffer.m_130258_(), buffer.readByte(), buffer.m_130258_());
    }

    public static void handle(S2CNpcWings message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcWingsClient.accept(message)));
        context.setPacketHandled(true);
    }
}
