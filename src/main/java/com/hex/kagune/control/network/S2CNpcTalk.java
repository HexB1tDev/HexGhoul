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
 * turns its page. 1.3.2: each answer may carry words of its own ({@code labels}, "" for the answer's own), so a page can offer any
 * reply (a topic, what to say back), and the NPC's line can run longer.
 */
public record S2CNpcTalk(int npc, String name, String temper, int regard, boolean ally, int skin, String line, int[] choices, String[] labels) {
    /** Longest text sent (UTF-8 bytes). */
    private static final int MAX_TEXT = 1024;
    /** Most answers on a page. */
    public static final int MOST = 12;

    public S2CNpcTalk(int npc, String name, String temper, int regard, boolean ally, int skin, String line, int[] choices) {
        this(npc, name, temper, regard, ally, skin, line, choices, new String[0]);
    }

    /** The words shown for answer {@code i}: its own, or "" for the answer's usual ones. */
    public String label(int i) {
        return i < this.labels.length && this.labels[i] != null ? this.labels[i] : "";
    }

    public static void encode(S2CNpcTalk message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.npc);
        text(buffer, message.name);
        text(buffer, message.temper);
        buffer.writeInt(message.regard);
        buffer.writeBoolean(message.ally);
        buffer.writeByte(message.skin);
        text(buffer, message.line);
        int count = Math.min(message.choices.length, MOST);
        buffer.writeByte(count);
        for (int i = 0; i < count; i++) {
            buffer.writeByte(message.choices[i]);
            text(buffer, message.label(i));
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
        int count = Math.max(0, Math.min(MOST, buffer.readByte()));
        int[] choices = new int[count];
        String[] labels = new String[count];
        for (int i = 0; i < count; i++) {
            choices[i] = buffer.readByte();
            labels[i] = text(buffer);
        }
        return new S2CNpcTalk(npc, name, temper, regard, ally, skin, line, choices, labels);
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
