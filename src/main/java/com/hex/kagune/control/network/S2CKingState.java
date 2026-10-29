package com.hex.kagune.control.network;

import com.hex.kagune.control.client.KingClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.0.5: a ghoul's final kakuja, for them and everyone tracking them: their crown ({@code crown}: -1 before the Dragon, else the
 * {@code KingForm} ordinal chosen, NONE while it waits to be chosen), the form worn ({@code KingForm} ordinal; NONE when none),
 * whether it is on or going, and the game tick it last came or went. {@code preview}: put on by the admin preview command (the look
 * only). 1.0.6: what its body is doing ({@code KingMove} ordinal), since when, at what level, the body it is aimed at (0: none), and
 * when each of its six abilities is ready again (game ticks).
 */
public record S2CKingState(UUID playerId, int crown, int form, boolean on, long since, boolean preview, int move, long moveAt, int level,
                           int target, long[] ready) {
    public static final int ABILITIES = 6;

    public static void encode(S2CKingState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.crown);
        buffer.writeByte(message.form);
        buffer.writeBoolean(message.on);
        buffer.m_130103_(message.since);
        buffer.writeBoolean(message.preview);
        buffer.writeByte(message.move);
        buffer.m_130103_(message.moveAt);
        buffer.writeByte(message.level);
        buffer.writeInt(message.target);
        for (int i = 0; i < ABILITIES; i++) {
            buffer.m_130103_(message.ready != null && i < message.ready.length ? message.ready[i] : 0L);
        }
    }

    public static S2CKingState decode(FriendlyByteBuf buffer) {
        UUID id = buffer.m_130259_();
        int crown = buffer.readByte();
        int form = buffer.readByte();
        boolean on = buffer.readBoolean();
        long since = buffer.m_130258_();
        boolean preview = buffer.readBoolean();
        int move = buffer.readByte();
        long moveAt = buffer.m_130258_();
        int level = buffer.readByte();
        int target = buffer.readInt();
        long[] ready = new long[ABILITIES];
        for (int i = 0; i < ABILITIES; i++) {
            ready[i] = buffer.m_130258_();
        }
        return new S2CKingState(id, crown, form, on, since, preview, move, moveAt, level, target, ready);
    }

    public static void handle(S2CKingState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KingClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
