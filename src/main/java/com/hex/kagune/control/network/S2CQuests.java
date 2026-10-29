package com.hex.kagune.control.network;

import com.hex.kagune.control.client.QuestClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** 7.0.1: the owner's quest log, as saved ({@code QuestLog.save}): the journal and the menus' quest locks read it. */
public record S2CQuests(String saved) {
    /** Longest log sent (UTF-8 bytes); a log is a few hundred. */
    private static final int MAX_BYTES = 65536;

    public static void encode(S2CQuests message, FriendlyByteBuf buffer) {
        byte[] bytes = message.saved.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int length = Math.min(bytes.length, MAX_BYTES);
        buffer.m_130130_(length);
        for (int i = 0; i < length; i++) {
            buffer.writeByte(bytes[i]);
        }
    }

    public static S2CQuests decode(FriendlyByteBuf buffer) {
        int length = Math.max(0, Math.min(MAX_BYTES, buffer.m_130242_()));
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = buffer.readByte();
        }
        return new S2CQuests(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void handle(S2CQuests message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> QuestClient.accept(message.saved())));
        context.setPacketHandled(true);
    }
}
