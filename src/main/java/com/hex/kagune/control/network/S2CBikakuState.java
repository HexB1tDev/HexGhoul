package com.hex.kagune.control.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.4.9: what a Bikaku player's worm is doing, for them and everyone tracking them: the bite of the M1 string it is making
 * ({@code BikakuRules.Bite} ordinal, 0 none) and the game tick it began, where it is thrown (a point in the world: the target's middle when
 * it began, or the air ahead), and the body it goes after (an entity id, 0 none: the clients follow it as it moves). 3.5.0: and the move
 * it is making ({@code BikakuMove} ordinal, 0 none), the tick it began, its point (the world), the bodies it goes after ({@link #TARGETS},
 * 0 none), the body it holds (0 none: missed, or let go) and whether its bite caught anything; 3.5.3: when Monstrous Regeneration last began; 3.5.4: whether it is in the kakuja and when it last went in or out. Every client draws the worm from these alone.
 */
public record S2CBikakuState(UUID playerId, int bite, long startedAt, double x, double y, double z, int targetId, int move, long moveAt,
                             double mx, double my, double mz, int[] targets, int heldId, boolean caught, long regenAt,
                             boolean kakuja, long kakujaAt) {
    /** How many bodies a move can go after at once. */
    public static final int TARGETS = 3;

    public static void encode(S2CBikakuState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.bite);
        buffer.m_130103_(message.startedAt);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeInt(message.targetId);
        buffer.writeByte(message.move);
        buffer.m_130103_(message.moveAt);
        buffer.writeDouble(message.mx);
        buffer.writeDouble(message.my);
        buffer.writeDouble(message.mz);
        for (int i = 0; i < TARGETS; i++) {
            buffer.writeInt(message.targets != null && i < message.targets.length ? message.targets[i] : 0);
        }
        buffer.writeInt(message.heldId);
        buffer.writeBoolean(message.caught);
        buffer.m_130103_(message.regenAt);
        buffer.writeBoolean(message.kakuja);
        buffer.m_130103_(message.kakujaAt);
    }

    public static S2CBikakuState decode(FriendlyByteBuf buffer) {
        UUID id = buffer.m_130259_();
        int bite = buffer.readByte();
        long startedAt = buffer.m_130258_();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        int targetId = buffer.readInt();
        int move = buffer.readByte();
        long moveAt = buffer.m_130258_();
        double mx = buffer.readDouble();
        double my = buffer.readDouble();
        double mz = buffer.readDouble();
        int[] targets = new int[TARGETS];
        for (int i = 0; i < TARGETS; i++) {
            targets[i] = buffer.readInt();
        }
        int heldId = buffer.readInt();
        boolean caught = buffer.readBoolean();
        long regenAt = buffer.m_130258_();
        boolean kakuja = buffer.readBoolean();
        return new S2CBikakuState(id, bite, startedAt, x, y, z, targetId, move, moveAt, mx, my, mz, targets, heldId, caught, regenAt, kakuja,
            buffer.m_130258_());
    }

    public static void handle(S2CBikakuState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.BikakuClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
