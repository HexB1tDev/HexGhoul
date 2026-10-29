package com.hex.kagune.control.network;

import com.hex.kagune.control.client.NpcTalkClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 7.0.2: a page of a conversation with a ghoul NPC ({@code NpcTalk}): who is talking (the NPC's id, name, temper, its regard for the
 * player, whether it walks with them), what it says, and the answers ({@code NpcTalk.Choice} ordinals). Opens the talk screen, or
 * turns its page.
 */
public record S2CNpcTalk(int npc, String name, String temper, int regard, boolean ally, int skin, String line, int[] choices) {
    /** Longest text sent (UTF-8 bytes). */
    private static final int MAX_TEXT = 1024;

    public static void encode(S2CNpcTalk message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.npc);
        text(buffer, message.name);
        text(buffer, message.temper);
        buffer.writeInt(message.regard);
        buffer.writeBoolean(message.ally);
        buffer.writeByte(message.skin);
        text(buffer, message.line);
        buffer.writeByte(Math.min(message.choices.length, 12));
        for (int i = 0; i < Math.min(message.choices.length, 12); i++) {
            buffer.writeByte(message.choices[i]);
        }
    }

    public static S2CNpcTalk decode(FriendlyByteBuf buffer) {
        int npc = buffer.readInt();
        String name = text(buffer);
        String temper = text(buffer);
        int regard = buffer.readInt();
        boolean ally = buffer.readBoolean();
        int skin = buffer.readByte();
        String line = text(buffer);
        int count = Math.max(0, Math.min(12, buffer.readByte()));
        int[] choices = new int[count];
        for (int i = 0; i < count; i++) {
            choices[i] = buffer.readByte();
        }
        return new S2CNpcTalk(npc, name, temper, regard, ally, skin, line, choices);
    }

    static void text(FriendlyByteBuf buffer, String text) {
        byte[] bytes = (text == null ? "" : text).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int length = Math.min(bytes.length, MAX_TEXT);
        buffer.m_130130_(length);
        for (int i = 0; i < length; i++) {
            buffer.writeByte(bytes[i]);
        }
    }

    static String text(FriendlyByteBuf buffer) {
        int length = Math.max(0, Math.min(MAX_TEXT, buffer.m_130242_()));
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = buffer.readByte();
        }
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static void handle(S2CNpcTalk message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcTalkClient.accept(message)));
        context.setPacketHandled(true);
    }
}
