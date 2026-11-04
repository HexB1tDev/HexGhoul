package com.hex.kagune.control.network;

import com.hex.kagune.control.client.RinkakuBlockClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 2.0.42: a Rinkaku player's block ({@code RinkakuBlock}), to them and everyone tracking them: it went up or down, took a blow, a
 * perfect block, or broke ({@code ticks}: the cooldown it left); and, to the player alone, a press refused ({@code ticks}: the cooldown
 * still to run). Purely visual: the client draws the tails and the body from it.
 */
public record S2CRinkakuBlock(UUID playerId, int event, int ticks) {
    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int HIT = 2;
    public static final int PERFECT = 3;
    public static final int BREAK = 4;
    public static final int REFUSED = 5;

    public static void encode(S2CRinkakuBlock message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.event);
        buffer.writeShort(Math.max(0, Math.min(Short.MAX_VALUE, message.ticks)));
    }

    public static S2CRinkakuBlock decode(FriendlyByteBuf buffer) {
        return new S2CRinkakuBlock(buffer.m_130259_(), buffer.readByte(), buffer.readShort());
    }

    public static void handle(S2CRinkakuBlock message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RinkakuBlockClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
