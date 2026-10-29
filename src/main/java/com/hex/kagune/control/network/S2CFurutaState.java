package com.hex.kagune.control.network;

import com.hex.kagune.control.client.FurutaClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 7.0.0: what a Furuta DNA player's kagune is doing, for them and everyone tracking them: the base stage it is in ({@code FurutaStage}
 * ordinal), the kakuja (and the game tick it last went on or off), the move ({@code FurutaMove} ordinal) and the tick it began, the
 * way it was thrown (yaw, degrees), its point (a target's middle, the ground it slams, the block it grips), the body each limb goes
 * after (0: none), the body held (0: none), his mutations; 7.0.2: the job running
 * alongside (a {@code FurutaMove} that keeps its own clock: the crawl, the mouth dragging), when it began, and what it holds. Every
 * client draws the limbs from these alone.
 */
public record S2CFurutaState(UUID playerId, int stage, boolean kakuja, long kakujaAt, int move, long startedAt, float yaw, double x, double y,
                             double z, int[] targets, int heldId, int mutations, int job, long jobAt, int jobHeld) {
    public static final int TARGETS = 5;

    public static void encode(S2CFurutaState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.stage);
        buffer.writeBoolean(message.kakuja);
        buffer.m_130103_(message.kakujaAt);
        buffer.writeByte(message.move);
        buffer.m_130103_(message.startedAt);
        buffer.writeFloat(message.yaw);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        for (int i = 0; i < TARGETS; i++) {
            buffer.writeInt(message.targets != null && i < message.targets.length ? message.targets[i] : 0);
        }
        buffer.writeInt(message.heldId);
        buffer.writeInt(message.mutations);
        buffer.writeByte(message.job);
        buffer.m_130103_(message.jobAt);
        buffer.writeInt(message.jobHeld);
    }

    public static S2CFurutaState decode(FriendlyByteBuf buffer) {
        UUID id = buffer.m_130259_();
        int stage = buffer.readByte();
        boolean kakuja = buffer.readBoolean();
        long kakujaAt = buffer.m_130258_();
        int move = buffer.readByte();
        long startedAt = buffer.m_130258_();
        float yaw = buffer.readFloat();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        int[] targets = new int[TARGETS];
        for (int i = 0; i < TARGETS; i++) {
            targets[i] = buffer.readInt();
        }
        int held = buffer.readInt();
        int mutations = buffer.readInt();
        int job = buffer.readByte();
        long jobAt = buffer.m_130258_();
        return new S2CFurutaState(id, stage, kakuja, kakujaAt, move, startedAt, yaw, x, y, z, targets, held, mutations, job, jobAt,
            buffer.readInt());
    }

    public static void handle(S2CFurutaState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FurutaClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
