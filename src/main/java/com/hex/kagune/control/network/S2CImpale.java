package com.hex.kagune.control.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.1.5: a Rinkaku player's Impale ({@code RinkakuImpale}) as it is now, every tick it is out: whose ({@code player}, an entity id), which
 * tail ({@code limb}), how far it has gone ({@code length}, blocks from the shoulder), which way ({@code x}, {@code y}, {@code z}: a unit
 * vector), and what it is doing ({@code phase}: {@link #OUT} going out or held there, {@link #BACK} tearing back out (sent once, as it
 * starts back), {@link #GONE} home).
 */
public record S2CImpale(int player, int limb, float length, float x, float y, float z, int phase) {
    public static final int GONE = 0;
    public static final int OUT = 1;
    public static final int BACK = 2;

    public static void encode(S2CImpale message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.player);
        buffer.writeByte(message.limb);
        buffer.writeFloat(message.length);
        buffer.writeFloat(message.x);
        buffer.writeFloat(message.y);
        buffer.writeFloat(message.z);
        buffer.writeByte(message.phase);
    }

    public static S2CImpale decode(FriendlyByteBuf buffer) {
        return new S2CImpale(buffer.readInt(), buffer.readByte(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
            buffer.readByte());
    }

    public static void handle(S2CImpale message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.ImpaleClient.accept(message)));
        context.setPacketHandled(true);
    }
}
