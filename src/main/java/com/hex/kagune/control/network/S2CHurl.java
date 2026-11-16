package com.hex.kagune.control.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.1.6: a Rinkaku player's Rubble Hurl ({@code RinkakuHurl}) as it is now, every tick: whose ({@code player}, an entity id), which piece
 * ({@code piece}: 0 while it is one chunk), what it is doing ({@code phase}: {@link #HOLD} torn out and in his tails' grip, {@link #FLY}
 * thrown, {@link #BURST} this piece burst, {@link #GONE} all of it over), where it is ({@code x}, {@code y}, {@code z}, its middle), how big
 * ({@code size}, blocks across), how charged ({@code charge}), the tails gripping it ({@code limbA}, {@code limbB}; -1 for none) and what
 * it was torn from ({@code item}: the block's item, to draw it).
 */
public record S2CHurl(int player, int piece, int phase, float x, float y, float z, float size, float charge, int limbA, int limbB, String item) {
    public static final int GONE = 0;
    public static final int HOLD = 1;
    public static final int FLY = 2;
    public static final int BURST = 3;

    public static void encode(S2CHurl message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.player);
        buffer.writeByte(message.piece);
        buffer.writeByte(message.phase);
        buffer.writeFloat(message.x);
        buffer.writeFloat(message.y);
        buffer.writeFloat(message.z);
        buffer.writeFloat(message.size);
        buffer.writeFloat(message.charge);
        buffer.writeByte(message.limbA);
        buffer.writeByte(message.limbB);
        S2CNpcTalk.text(buffer, message.item == null ? "" : message.item);
    }

    public static S2CHurl decode(FriendlyByteBuf buffer) {
        return new S2CHurl(buffer.readInt(), buffer.readByte(), buffer.readByte(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
            buffer.readFloat(), buffer.readFloat(), buffer.readByte(), buffer.readByte(), S2CNpcTalk.text(buffer));
    }

    public static void handle(S2CHurl message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.HurlClient.accept(message)));
        context.setPacketHandled(true);
    }
}
