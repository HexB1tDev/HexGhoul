package com.hex.kagune.control.network;

import com.hex.kagune.control.client.ControlClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server-authoritative control snapshot for one player, sent to that player and everyone tracking them.
 * It only drives visuals and the owner's local view-pull/input-drag; it never carries damage or positions.
 */
public record S2CControlState(
    UUID playerId,
    float control,
    float hunger,
    float frenzy,
    int urgeType,
    long urgeStarted,
    int urgeDuration,
    int urgeTargetId,
    float urgeStrength,
    boolean urgeRestrained,
    int comboStyle,
    int comboStage,
    long comboStarted,
    int comboDuration,
    int comboLimbMask,
    float comboDeviation,
    boolean comboInvoluntary
) {
    public static void encode(S2CControlState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeFloat(message.control);
        buffer.writeFloat(message.hunger);
        buffer.writeFloat(message.frenzy);
        buffer.writeByte(message.urgeType);
        buffer.m_130103_(message.urgeStarted);
        buffer.m_130130_(Math.max(0, message.urgeDuration));
        buffer.writeInt(message.urgeTargetId);
        buffer.writeFloat(message.urgeStrength);
        buffer.writeBoolean(message.urgeRestrained);
        buffer.writeByte(message.comboStyle);
        buffer.writeByte(message.comboStage);
        buffer.m_130103_(message.comboStarted);
        buffer.m_130130_(Math.max(0, message.comboDuration));
        buffer.writeInt(message.comboLimbMask);
        buffer.writeFloat(message.comboDeviation);
        buffer.writeBoolean(message.comboInvoluntary);
    }

    public static S2CControlState decode(FriendlyByteBuf buffer) {
        return new S2CControlState(
            buffer.m_130259_(),
            buffer.readFloat(),
            buffer.readFloat(),
            buffer.readFloat(),
            buffer.readByte(),
            buffer.m_130258_(),
            buffer.m_130242_(),
            buffer.readInt(),
            buffer.readFloat(),
            buffer.readBoolean(),
            buffer.readByte(),
            buffer.readByte(),
            buffer.m_130258_(),
            buffer.m_130242_(),
            buffer.readInt(),
            buffer.readFloat(),
            buffer.readBoolean()
        );
    }

    public static void handle(S2CControlState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ControlClient.accept(message)));
        context.setPacketHandled(true);
    }
}
