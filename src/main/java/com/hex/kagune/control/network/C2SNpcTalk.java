package com.hex.kagune.control.network;

import com.hex.kagune.control.server.GhoulNpcTalk;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** 7.0.2: the player's answer to a ghoul NPC ({@code NpcTalk.Choice} ordinal); the server checks it was one they were offered. */
public record C2SNpcTalk(int npc, int choice) {
    public static void encode(C2SNpcTalk message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.npc);
        buffer.writeByte(message.choice);
    }

    public static C2SNpcTalk decode(FriendlyByteBuf buffer) {
        return new C2SNpcTalk(buffer.readInt(), buffer.readByte());
    }

    public static void handle(C2SNpcTalk message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (!com.hex.kagune.control.DragonLock.locked(sender)) {
                    GhoulNpcTalk.heard(sender, message.npc, message.choice);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
