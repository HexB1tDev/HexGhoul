package com.hex.kagune.control.network;

import com.hex.kagune.control.client.TendrilShieldClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 5.0.14: the tentacles a Rinkaku player holds up as a guard against incoming shots ({@code TendrilShield}): for each, the limb,
 * the world direction the shots come from (yaw and pitch, degrees) and the game tick it went up. Sent to the player and everyone
 * tracking them whenever a guard goes up or comes down; an empty list folds them all back. Purely visual.
 */
public record S2CTendrilShield(UUID playerId, int[] limbs, float[] yaws, float[] pitches, long[] raisedAt) {
    /** More can never be up at once (a form has at most this many limbs). */
    static final int MAX = 16;

    public static void encode(S2CTendrilShield message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        int count = Math.min(MAX, message.limbs.length);
        buffer.writeByte(count);
        for (int i = 0; i < count; i++) {
            buffer.writeByte(message.limbs[i]);
            buffer.writeFloat(message.yaws[i]);
            buffer.writeFloat(message.pitches[i]);
            buffer.m_130103_(message.raisedAt[i]);
        }
    }

    public static S2CTendrilShield decode(FriendlyByteBuf buffer) {
        UUID id = buffer.m_130259_();
        int count = Math.max(0, Math.min(MAX, buffer.readByte()));
        int[] limbs = new int[count];
        float[] yaws = new float[count];
        float[] pitches = new float[count];
        long[] raisedAt = new long[count];
        for (int i = 0; i < count; i++) {
            limbs[i] = buffer.readByte();
            yaws[i] = buffer.readFloat();
            pitches[i] = buffer.readFloat();
            raisedAt[i] = buffer.m_130258_();
        }
        return new S2CTendrilShield(id, limbs, yaws, pitches, raisedAt);
    }

    public static void handle(S2CTendrilShield message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TendrilShieldClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
