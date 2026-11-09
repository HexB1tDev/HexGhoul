package com.hex.kagune.control.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.7.3, Potential Unlock: the parts of a Bikaku player torn off and being pulled back, for him and everyone tracking him (all of them;
 * none: he is whole). Each: which part ({@code PotentialRules.Part}), how it was thrown ({@code seed}), the way the blow went (his body's
 * frame then: right, ahead), where he stood and the way his body faced then (it lies where it fell), how far below his feet the ground
 * was, how far along it is (ticks) and whether it is waiting for energy. Every client plays the rest ({@code Reattach}).
 */
public record S2CPotential(UUID playerId, Torn[] torn) {
    /** The most parts at once (every part there is). */
    public static final int MAX = 5;

    public record Torn(int part, long seed, float blowX, float blowZ, double x, double y, double z, float yaw, float ground, float age, boolean paused) {
    }

    public static void encode(S2CPotential message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        int count = Math.min(MAX, message.torn.length);
        buffer.writeByte(count);
        for (int i = 0; i < count; i++) {
            Torn t = message.torn[i];
            buffer.writeByte(t.part());
            buffer.m_130103_(t.seed());
            buffer.writeFloat(t.blowX());
            buffer.writeFloat(t.blowZ());
            buffer.writeDouble(t.x());
            buffer.writeDouble(t.y());
            buffer.writeDouble(t.z());
            buffer.writeFloat(t.yaw());
            buffer.writeFloat(t.ground());
            buffer.writeFloat(t.age());
            buffer.writeBoolean(t.paused());
        }
    }

    public static S2CPotential decode(FriendlyByteBuf buffer) {
        UUID id = buffer.m_130259_();
        int count = Math.max(0, Math.min(MAX, buffer.readByte()));
        Torn[] torn = new Torn[count];
        for (int i = 0; i < count; i++) {
            torn[i] = new Torn(buffer.readByte(), buffer.m_130258_(), buffer.readFloat(), buffer.readFloat(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
        }
        return new S2CPotential(id, torn);
    }

    public static void handle(S2CPotential message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.PotentialClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
