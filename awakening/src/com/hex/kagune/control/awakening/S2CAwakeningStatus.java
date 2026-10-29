package com.hex.kagune.control.awakening;

import com.hex.kagune.control.awakening.client.DragonClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** The local player's own awakening stage and the final-form unlock flag (the quest itself is in the journal). */
public record S2CAwakeningStatus(AwakeningStage stage, boolean finalKakuja) {
    public static void encode(S2CAwakeningStatus message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.stage);
        buffer.writeBoolean(message.finalKakuja);
    }

    public static S2CAwakeningStatus decode(FriendlyByteBuf buffer) {
        return new S2CAwakeningStatus(buffer.readEnum(AwakeningStage.class), buffer.readBoolean());
    }

    public static void handle(S2CAwakeningStatus message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DragonClient.acceptStatus(message)));
        context.setPacketHandled(true);
    }
}
