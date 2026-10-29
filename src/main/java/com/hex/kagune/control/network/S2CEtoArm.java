package com.hex.kagune.control.network;

import com.hex.kagune.control.client.ArmClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.2.0: an Eto player's Mutated Arm, for her and everyone tracking her, sent each time it starts something new (or catches
 * something): what it is doing ({@code ArmRules.Phase} ordinal) since which game tick, when N went down (its charge counts from
 * there), how far out it was as this began, its charge, the way it points, the sphere's middle and size, and the bodies it holds.
 */
public record S2CEtoArm(UUID playerId, int phase, long startedAt, long downAt, float length, float charge, float aimX, float aimY, float aimZ,
                        double x, double y, double z, float radius, int[] caught) {
    public static void encode(S2CEtoArm message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.phase);
        buffer.m_130103_(message.startedAt);
        buffer.m_130103_(message.downAt);
        buffer.writeFloat(message.length);
        buffer.writeFloat(message.charge);
        buffer.writeFloat(message.aimX);
        buffer.writeFloat(message.aimY);
        buffer.writeFloat(message.aimZ);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.radius);
        buffer.writeByte(message.caught.length);
        for (int id : message.caught) {
            buffer.writeInt(id);
        }
    }

    public static S2CEtoArm decode(FriendlyByteBuf buffer) {
        UUID id = buffer.m_130259_();
        int phase = buffer.readByte();
        long startedAt = buffer.m_130258_();
        long downAt = buffer.m_130258_();
        float length = buffer.readFloat();
        float charge = buffer.readFloat();
        float ax = buffer.readFloat();
        float ay = buffer.readFloat();
        float az = buffer.readFloat();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        float radius = buffer.readFloat();
        int[] caught = new int[Math.max(0, Math.min(16, buffer.readByte()))];
        for (int i = 0; i < caught.length; i++) {
            caught[i] = buffer.readInt();
        }
        return new S2CEtoArm(id, phase, startedAt, downAt, length, charge, ax, ay, az, x, y, z, radius, caught);
    }

    public static void handle(S2CEtoArm message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ArmClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
