package com.hex.kagune.control.network;

import com.hex.kagune.control.client.GhoulNpcClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 7.0.2: a ghoul NPC as its clients draw it: who it is ({@code identity}, {@code NpcIdentity.pack}), its kagune out or not and since
 * when, the rinkaku lash it is making (a {@code FurutaMove}, when it began, the point it was aimed at), what it goes for, and the
 * ukaku's volley (when its wings curled to aim, its last shot and the wing it came from). Game ticks throughout. 1.0.41: and its flip
 * (an {@code EtoRules.Flip}, -1 none, and when it took off).
 */
public record S2CGhoulNpc(int id, long identity, boolean out, long outAt, int move, long moveAt, double x, double y, double z, int prey, long volleyAt,
                          long shotAt, int shotSide, int flip, long flipAt) {
    public static void encode(S2CGhoulNpc message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.id);
        buffer.m_130103_(message.identity);
        buffer.writeBoolean(message.out);
        buffer.m_130103_(message.outAt);
        buffer.writeInt(message.move);
        buffer.m_130103_(message.moveAt);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeInt(message.prey);
        buffer.m_130103_(message.volleyAt);
        buffer.m_130103_(message.shotAt);
        buffer.writeInt(message.shotSide);
        buffer.writeByte(message.flip);
        buffer.m_130103_(message.flipAt);
    }

    public static S2CGhoulNpc decode(FriendlyByteBuf buffer) {
        return new S2CGhoulNpc(buffer.readInt(), buffer.m_130258_(), buffer.readBoolean(), buffer.m_130258_(), buffer.readInt(), buffer.m_130258_(),
            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readInt(), buffer.m_130258_(), buffer.m_130258_(), buffer.readInt(),
            buffer.readByte(), buffer.m_130258_());
    }

    public static void handle(S2CGhoulNpc message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GhoulNpcClient.accept(message)));
        context.setPacketHandled(true);
    }
}
