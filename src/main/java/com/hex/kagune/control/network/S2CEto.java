package com.hex.kagune.control.network;

import com.hex.kagune.control.client.EtoClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.0: Eto as her clients draw her: her gesture ({@code EtoGesture}) and when it began, the move she is making in a fight
 * ({@code EtoRules.Move}) and when it began, her flip ({@code PaleRules.Flip}, -1 none) and when it took off, and whether her eye is open
 * for the fight ({@code turned}; at night it opens anyway). Game ticks throughout. 1.3.2: who she is holding on to ({@code with}: the
 * player she kisses, pulls in, hugs, clings to; 0 none), and the line over her head ({@code tag}: "Married to ...", "" none).
 */
public record S2CEto(int id, int gesture, long gestureAt, int move, long moveAt, int flip, long flipAt, boolean turned, int with, String tag) {
    public static void encode(S2CEto message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.id);
        buffer.writeByte(message.gesture);
        buffer.m_130103_(message.gestureAt);
        buffer.writeByte(message.move);
        buffer.m_130103_(message.moveAt);
        buffer.writeByte(message.flip);
        buffer.m_130103_(message.flipAt);
        buffer.writeBoolean(message.turned);
        buffer.writeInt(message.with);
        S2CNpcTalk.text(buffer, message.tag);
    }

    public static S2CEto decode(FriendlyByteBuf buffer) {
        return new S2CEto(buffer.readInt(), buffer.readByte(), buffer.m_130258_(), buffer.readByte(), buffer.m_130258_(), buffer.readByte(),
            buffer.m_130258_(), buffer.readBoolean(), buffer.readInt(), S2CNpcTalk.text(buffer));
    }

    public static void handle(S2CEto message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EtoClient.accept(message)));
        context.setPacketHandled(true);
    }
}
