package com.hex.kagune.control.network;

import com.hex.kagune.control.client.CutsceneOverlay;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.3.2 (1.3.0's surgery, made any NPC's): a cutscene begins on this player ({@code CutsceneOverlay}'s scripts: the screen goes dark,
 * what happens is heard and told, and the world comes back), with the words said at its end ({@code words}: the NPC's, so each says it
 * her own way, warm to a partner, cold to anyone else).
 */
public record S2CCutscene(int script, String words) {
    public static final int SURGERY = 0;
    public static final int NIGHT = 1;
    /** 1.3.10: a birth: "9 months later...", a cry in the dark, who was born. */
    public static final int BIRTH = 2;
    /** 1.3.14: Ken's trial: the torture he lived through, lived through ({@code CutsceneOverlay.TORTURE}). */
    public static final int TORTURE = 3;

    public static void encode(S2CCutscene message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.script);
        S2CNpcTalk.text(buffer, message.words);
    }

    public static S2CCutscene decode(FriendlyByteBuf buffer) {
        return new S2CCutscene(buffer.readByte(), S2CNpcTalk.text(buffer));
    }

    public static void handle(S2CCutscene message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CutsceneOverlay.begin(message.script(), message.words())));
        }
        context.setPacketHandled(true);
    }
}
