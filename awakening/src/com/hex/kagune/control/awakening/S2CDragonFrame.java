package com.hex.kagune.control.awakening;

import com.hex.kagune.control.awakening.client.DragonClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * One server tick of the Dragon's pose. Positions are relative to the anchor so floats keep
 * sub-millimetre precision anywhere in the world. Clients render exactly this state.
 * 1.0.13: {@code mini}: War's small Dragon (Centipede Dominion), drawn the same, filmed from behind it instead of the great one's
 * cinematic.
 */
public record S2CDragonFrame(
        UUID owner,
        int tick,
        double anchorX,
        double anchorY,
        double anchorZ,
        float[] segments,
        float[] radii,
        float[] heads,
        byte[] headStates,
        float[] tentacles,
        byte[] tentacleStates,
        int[] held,
        float[] emergence,
        boolean mini) {
    public static final int SEGMENT_FLOATS = DragonRig.SEGMENTS * 3;
    public static final int HEAD_FLOATS = DragonRig.HEADS * 4;
    public static final int TENTACLE_FLOATS = DragonRig.TENTACLES * 3;

    public static S2CDragonFrame capture(UUID owner, DragonRig rig, V3 emergence) {
        V3 a = rig.anchor;
        float[] segments = new float[SEGMENT_FLOATS];
        float[] radii = new float[DragonRig.SEGMENTS];
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            V3 s = rig.segment(i);
            segments[i * 3] = (float) (s.x() - a.x());
            segments[i * 3 + 1] = (float) (s.y() - a.y());
            segments[i * 3 + 2] = (float) (s.z() - a.z());
            radii[i] = (float) rig.radius(i);
        }
        float[] heads = new float[HEAD_FLOATS];
        byte[] headStates = new byte[DragonRig.HEADS];
        for (int i = 0; i < DragonRig.HEADS; i++) {
            DragonRig.Head h = rig.heads[i];
            heads[i * 4] = (float) (h.tip.x() - a.x());
            heads[i * 4 + 1] = (float) (h.tip.y() - a.y());
            heads[i * 4 + 2] = (float) (h.tip.z() - a.z());
            heads[i * 4 + 3] = (float) h.jaw;
            headStates[i] = (byte) h.state.ordinal();
        }
        float[] tentacles = new float[TENTACLE_FLOATS];
        byte[] tentacleStates = new byte[DragonRig.TENTACLES];
        int[] held = new int[DragonRig.TENTACLES];
        for (int i = 0; i < DragonRig.TENTACLES; i++) {
            DragonRig.Tentacle t = rig.tentacles[i];
            tentacles[i * 3] = (float) (t.tip.x() - a.x());
            tentacles[i * 3 + 1] = (float) (t.tip.y() - a.y());
            tentacles[i * 3 + 2] = (float) (t.tip.z() - a.z());
            tentacleStates[i] = (byte) t.state.ordinal();
            held[i] = t.heldId;
        }
        V3 e = emergence == null ? a : emergence;
        float[] emerge = {(float) (e.x() - a.x()), (float) (e.y() - a.y()), (float) (e.z() - a.z())};
        return new S2CDragonFrame(owner, rig.tick(), a.x(), a.y(), a.z(), segments, radii, heads, headStates, tentacles, tentacleStates, held, emerge, false);
    }

    public static void encode(S2CDragonFrame message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.owner);
        buffer.writeVarInt(message.tick);
        buffer.writeDouble(message.anchorX);
        buffer.writeDouble(message.anchorY);
        buffer.writeDouble(message.anchorZ);
        for (float value : message.segments) buffer.writeFloat(value);
        for (float value : message.radii) buffer.writeFloat(value);
        for (float value : message.heads) buffer.writeFloat(value);
        for (byte value : message.headStates) buffer.writeByte(value);
        for (float value : message.tentacles) buffer.writeFloat(value);
        for (byte value : message.tentacleStates) buffer.writeByte(value);
        for (int value : message.held) buffer.writeVarInt(value);
        for (float value : message.emergence) buffer.writeFloat(value);
        buffer.writeBoolean(message.mini);
    }

    public static S2CDragonFrame decode(FriendlyByteBuf buffer) {
        UUID owner = buffer.readUUID();
        int tick = buffer.readVarInt();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        float[] segments = floats(buffer, SEGMENT_FLOATS);
        float[] radii = floats(buffer, DragonRig.SEGMENTS);
        float[] heads = floats(buffer, HEAD_FLOATS);
        byte[] headStates = bytes(buffer, DragonRig.HEADS);
        float[] tentacles = floats(buffer, TENTACLE_FLOATS);
        byte[] tentacleStates = bytes(buffer, DragonRig.TENTACLES);
        int[] held = new int[DragonRig.TENTACLES];
        for (int i = 0; i < held.length; i++) held[i] = buffer.readVarInt();
        float[] emergence = floats(buffer, 3);
        boolean mini = buffer.readBoolean();
        return new S2CDragonFrame(owner, tick, x, y, z, segments, radii, heads, headStates, tentacles, tentacleStates, held, emergence, mini);
    }

    private static float[] floats(FriendlyByteBuf buffer, int count) {
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            float value = buffer.readFloat();
            values[i] = Float.isFinite(value) ? value : 0.0F;
        }
        return values;
    }

    private static byte[] bytes(FriendlyByteBuf buffer, int count) {
        byte[] values = new byte[count];
        for (int i = 0; i < count; i++) values[i] = buffer.readByte();
        return values;
    }

    public static void handle(S2CDragonFrame message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DragonClient.acceptFrame(message)));
        context.setPacketHandled(true);
    }
}
