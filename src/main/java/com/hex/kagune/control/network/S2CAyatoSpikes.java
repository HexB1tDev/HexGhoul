package com.hex.kagune.control.network;

import com.hex.kagune.control.client.fx.AyatoSpikes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Everything of Ayato's crystal that one body carries, for every client near it: the spikes stuck in it (in the body's own
 * frame: x to its right, y up from its feet, z forward; with the way each went in and when it bursts), how hard it bleeds
 * from them (stacks, and the tick the bleeding stops) and the body's yaw as the server has it (vanilla never syncs a body's
 * yaw, so a client's can drift from the frame the spikes were stuck in). Sent whole whenever it changes and once a second
 * while there is any. Purely visual: the server owns the spikes and the bleeding.
 */
public record S2CAyatoSpikes(int entityId, int bleedStacks, long bleedUntil, float bodyYaw, List<Spike> spikes) {
    /** Never more spikes than this in one message (the server caps a body well below it). */
    public static final int MOST = 64;

    /**
     * One stuck spike. 5.0.14: {@code limb} is the Kaneki tentacle it is stuck in (a guard stopped it; the offset is where it
     * struck the tentacle), -1 for the body itself.
     */
    public record Spike(int id, float ox, float oy, float oz, float dx, float dy, float dz, int kind, long stuckAt, long burstAt, int limb) {
    }

    public static void encode(S2CAyatoSpikes message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeByte(Math.max(0, Math.min(127, message.bleedStacks)));
        buffer.m_130103_(message.bleedUntil);
        buffer.writeFloat(message.bodyYaw);
        int count = Math.min(MOST, message.spikes.size());
        buffer.writeByte(count);
        for (int index = 0; index < count; index++) {
            Spike spike = message.spikes.get(index);
            buffer.writeInt(spike.id);
            buffer.writeFloat(spike.ox);
            buffer.writeFloat(spike.oy);
            buffer.writeFloat(spike.oz);
            buffer.writeFloat(spike.dx);
            buffer.writeFloat(spike.dy);
            buffer.writeFloat(spike.dz);
            buffer.writeByte(spike.kind);
            buffer.m_130103_(spike.stuckAt);
            buffer.m_130103_(spike.burstAt);
            buffer.writeByte(spike.limb);
        }
    }

    public static S2CAyatoSpikes decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        int stacks = buffer.readByte();
        long until = buffer.m_130258_();
        float yaw = buffer.readFloat();
        int count = Math.max(0, Math.min(MOST, buffer.readByte()));
        List<Spike> spikes = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            spikes.add(new Spike(buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readByte(), buffer.m_130258_(), buffer.m_130258_(), buffer.readByte()));
        }
        return new S2CAyatoSpikes(entityId, stacks, until, yaw, spikes);
    }

    public static void handle(S2CAyatoSpikes message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AyatoSpikes.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
