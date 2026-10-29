package com.hex.kagune.control.network;

import com.hex.kagune.control.server.KingServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.0.5: the final kakuja's requests: {@link #CHOOSE} a form ({@code arg}: its ordinal) from the menu, once; {@link #TOGGLE} it on or
 * off (its key). 1.0.6: {@link #ABILITY}, one of its keys ({@code arg}: the ability's slot times 4 plus what the key did, {@code
 * WarRules} PRESS, RELEASE or CROUCH_PRESS), with the body under the crosshair ({@code target}, 0: none). The server checks everything
 * ({@code KingServer}). Never during the Dragon. 1.0.14: {@link #STRIKE}, an M1 at the body under the crosshair ({@code target}, 0:
 * none).
 */
public record C2SKingAction(int kind, int arg, int target) {
    public static final int CHOOSE = 0;
    public static final int TOGGLE = 1;
    public static final int ABILITY = 2;
    public static final int STRIKE = 3;
    /** 1.0.20: the heavy (RMB): arg is KingCombo's flags, PRESS when pressed (else let go). */
    public static final int HEAVY = 4;
    /** 1.0.29: Centipede Dominion driven: arg is the keys held this tick ({@code WarRules.WYRM_*} bits); 1.0.39: target is his aim ({@code WarRules.wyrmAim}). */
    public static final int WYRM = 5;

    public C2SKingAction(int kind, int arg) {
        this(kind, arg, 0);
    }

    public static void encode(C2SKingAction message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.kind);
        buffer.writeInt(message.arg);
        buffer.writeInt(message.target);
    }

    public static C2SKingAction decode(FriendlyByteBuf buffer) {
        return new C2SKingAction(buffer.readByte(), buffer.readInt(), buffer.readInt());
    }

    public static void handle(C2SKingAction message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (!com.hex.kagune.control.DragonLock.dragon(sender)) {
                    KingServer.request(sender, message.kind, message.arg, message.target);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
