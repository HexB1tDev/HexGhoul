package com.hex.kagune.control.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.0.6: a player's kagune-made weapon swung, as an investigator's is told ({@code CcgAgent.swing}): whose ({@code player}, an entity id),
 * what ({@code code}: {@code ArmRules.stepCode} a blow of its string, {@code ArmRules.abilityCode} one of its moves, -1 nothing), since when
 * ({@code at}, game ticks), and whom its whip has bound ({@code bound}, an entity id; 0 none).
 */
public record S2CArmSwing(int player, int code, long at, int bound) {
    public static void encode(S2CArmSwing message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.player);
        buffer.writeByte(message.code);
        buffer.m_130103_(message.at);
        buffer.writeInt(message.bound);
    }

    public static S2CArmSwing decode(FriendlyByteBuf buffer) {
        return new S2CArmSwing(buffer.readInt(), buffer.readByte(), buffer.m_130258_(), buffer.readInt());
    }

    public static void handle(S2CArmSwing message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.WeaponClient.accept(message)));
        context.setPacketHandled(true);
    }
}
