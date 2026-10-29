package com.hex.kagune.control.network;

import com.hex.kagune.control.client.ReviveClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.2.0, You Thought You'd Get Rid of Me That Easily?: an Pale player's death that did not take, for her and everyone tracking her:
 * since which game tick her body is in pieces, how it came apart ({@code seed}), where she stood (her feet), the way her body
 * faced (degrees) and how far below her feet the ground is. Every client plays the rest from that ({@code ReviveRules},
 * {@code Remains}). {@code over} ends it at once (she left, changed world).
 */
public record S2CPaleRevive(UUID playerId, long startedAt, long seed, double x, double y, double z, float yaw, float ground, boolean over) {
    public static void encode(S2CPaleRevive message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.m_130103_(message.startedAt);
        buffer.m_130103_(message.seed);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.yaw);
        buffer.writeFloat(message.ground);
        buffer.writeBoolean(message.over);
    }

    public static S2CPaleRevive decode(FriendlyByteBuf buffer) {
        return new S2CPaleRevive(buffer.m_130259_(), buffer.m_130258_(), buffer.m_130258_(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
            buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(S2CPaleRevive message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ReviveClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
