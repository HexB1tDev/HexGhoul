package com.hex.kagune.control.awakening;

import com.hex.kagune.control.awakening.client.DragonClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * A discrete moment of the awakening that clients turn into camera shake, flashes and debris. 1.0.11: {@code PRELUDE}, again and again
 * while Rinkaku turns (its magnitude: how many ticks in), and {@code BURST}, each violent burst out of him then (how hard, 0..1).
 */
public record S2CDragonEvent(UUID owner, Kind kind, double x, double y, double z, float magnitude) {
    public enum Kind { ERUPTION, BITE, LASH, SLAM, GRAB, CRACK, SHATTER, EMERGE, END, PRELUDE, BURST }

    public static void encode(S2CDragonEvent message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.owner);
        buffer.writeEnum(message.kind);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.magnitude);
    }

    public static S2CDragonEvent decode(FriendlyByteBuf buffer) {
        return new S2CDragonEvent(buffer.readUUID(), buffer.readEnum(Kind.class), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat());
    }

    public static void handle(S2CDragonEvent message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DragonClient.acceptEvent(message)));
        context.setPacketHandled(true);
    }
}
