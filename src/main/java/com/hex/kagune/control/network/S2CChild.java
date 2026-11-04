package com.hex.kagune.control.network;

import com.hex.kagune.control.client.ChildClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.10: a child as its clients draw it: its name, what it inherited ({@code Genome.save}: its looks), how grown it is ({@code ChildStage}
 * ordinal), who carries it (an entity id, 0 no one), and whether its kakugan shows (it fights). 1.3.20: how it is held ({@code ChildRules}
 * RIDE_*), and the moment it is having with a parent ({@code ChildMoment} ordinal), since when (game ticks), with whom (an entity id).
 */
public record S2CChild(int id, String name, String genome, int stage, int carrier, boolean turned, int ride, int moment, long momentAt, int partner,
                       int cue, long cueAt, float mother) {
    /** 2.3.0 (protocol 84): and how big its mother is (a grown daughter is her height, give or take). */
    public S2CChild(int id, String name, String genome, int stage, int carrier, boolean turned, int ride, int moment, long momentAt, int partner,
                    int cue, long cueAt) {
        this(id, name, genome, stage, carrier, turned, ride, moment, momentAt, partner, cue, cueAt, com.hex.kagune.control.FemaleBody.SIZE);
    }

    // 1.3.57 (protocol 70): and what it does with its body as it says a line in a talk ({@code ChildCue} ordinal), since when.
    public static void encode(S2CChild message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.id);
        S2CNpcTalk.text(buffer, message.name);
        S2CNpcTalk.text(buffer, message.genome);
        buffer.writeByte(message.stage);
        buffer.writeInt(message.carrier);
        buffer.writeBoolean(message.turned);
        buffer.writeByte(message.ride);
        buffer.writeByte(message.moment);
        buffer.m_130103_(message.momentAt);
        buffer.writeInt(message.partner);
        buffer.writeByte(message.cue);
        buffer.m_130103_(message.cueAt);
        buffer.writeFloat(message.mother);
    }

    public static S2CChild decode(FriendlyByteBuf buffer) {
        return new S2CChild(buffer.readInt(), S2CNpcTalk.text(buffer), S2CNpcTalk.text(buffer), buffer.readByte(), buffer.readInt(), buffer.readBoolean(),
            buffer.readByte(), buffer.readByte(), buffer.m_130258_(), buffer.readInt(), buffer.readByte(), buffer.m_130258_(), buffer.readFloat());
    }

    public static void handle(S2CChild message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChildClient.accept(message)));
        context.setPacketHandled(true);
    }
}
