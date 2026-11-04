package com.hex.kagune.control.network;

import com.hex.kagune.control.server.UkakuServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * An Ukaku input the original channel has no action for: {@link #ENDER} is a right click with the wings out (the server
 * picks the ender from the shots fired so far), {@link #GATLING} the Gatling's trigger. {@code targetId} is the entity under the crosshair, or 0; the server
 * checks it like any other hint.
 */
public record C2SUkakuAction(int action, int targetId) {
    public static final int ENDER = 0;
    /** The Gatling's trigger (J): {@code targetId} 1 while held (sent every few ticks), 0 when let go. */
    public static final int GATLING = 1;
    /** A right click with W held (Fire Dash dashes forward instead of hopping back); {@code targetId} as for {@link #ENDER}. */
    public static final int ENDER_FORWARD = 2;
    /** Unlock a mutation from the Ukaku menu: {@code targetId} is the mutation's ordinal. */
    public static final int MUTATE = 3;
    /** K tapped (Ukaku): into the first kakuja, or out of either. */
    public static final int KAKUJA = 4;
    /** K held (Ukaku, mastery 1000): into the final kakuja, or out of it. */
    public static final int FINAL_KAKUJA = 5;
    /**
     * 6.0.0: right click with the kakuja's passive (Ukaku's Steel Blade, Rinkaku's Full Kakuja): a dash, {@code targetId} 1
     * forward (W or no direction), -1 back (S).
     */
    public static final int DASH = 6;
    /**
     * 2.0.42: Rinkaku's block on the right click: {@code targetId} 2 when the button goes down, 1 every few ticks while it is held, 0 when
     * it is let go ({@code RinkakuBlockServer}).
     */
    public static final int BLOCK = 7;
    /**
     * 2.1.7: the White Reaper's dash in Ukaku's final kakuja (the sprint key tapped twice): {@code targetId} the way the player was moving,
     * in tenths of a degree of world yaw ({@code ReaperDash.encode}).
     */
    public static final int REAPER_DASH = 8;

    public static void encode(C2SUkakuAction message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.action);
        buffer.writeInt(message.targetId);
    }

    public static C2SUkakuAction decode(FriendlyByteBuf buffer) {
        return new C2SUkakuAction(buffer.readByte(), buffer.readInt());
    }

    public static void handle(C2SUkakuAction message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                // 1.0.10: K still answers under a final kakuja (it takes it off); only the Dragon keeps the body from it.
                boolean kakuja = message.action == KAKUJA || message.action == FINAL_KAKUJA;
                if (kakuja ? !com.hex.kagune.control.DragonLock.dragon(sender) : !com.hex.kagune.control.DragonLock.locked(sender)) {
                    UkakuServer.onAction(sender, message.action, message.targetId);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
