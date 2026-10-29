package com.hex.kagune.control.network;

import com.hex.kagune.control.server.PaleServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.1.0: an Pale input the original channel has no action for. Her M1 still comes as the original's swipe; everything else is
 * here. {@code targetId} is the entity under her crosshair (0: none); the server checks it like any other hint.
 *
 * @param action one of the constants below
 * @param arg    the flip ({@code PaleRules.Flip} ordinal); for the held keys 1 while held (sent every few ticks), 0 when let go
 */
public record C2SPaleAction(int action, int arg, int targetId) {
    /** Right click: a flip, {@code arg} the direction. */
    public static final int FLIP = 0;
    /** Z (the original's Rapid Strike key) held for Big Punch: 1 held, 0 let go. */
    public static final int PUNCH = 1;
    /** J (the original's Slingshot key) tapped: the Flesh Swipe. */
    public static final int SWIPE = 2;
    /** J held: the flesh reaches for {@code targetId} (1 held, 0 let go: the throw). */
    public static final int GRAB = 3;
    /** V (the original's Lift key) tapped: the Speed Kick at {@code targetId}. */
    public static final int KICK = 4;
    /** V held: the Flesh Hammer (1 held, 0 let go: the slam). */
    public static final int HAMMER = 5;
    /** K: her kakuja, not in the game yet (the server says so). */
    public static final int KAKUJA = 6;
    /** B (the original's Slam key): Watch This. */
    public static final int WATCH = 7;
    /** N (the original's Throw key): the Mutated Arm, tapped... */
    public static final int ARM_TAP = 8;
    /** ...or held (1 down, 0 let go). */
    public static final int ARM = 9;
    /** 6.2.0: unlock a mutation from her menu: {@code arg} is the mutation's ordinal (the server checks there is a point for it). */
    public static final int MUTATION = 10;
    /** 6.2.2: her menu's Instant Big Punch switch (Witness This): {@code arg} 1 on, 0 off. */
    public static final int INSTANT_PUNCH = 11;
    /** 7.0.1: an M1 of the eyes alone (the shared eye-mode fight): {@code targetId} the body under the crosshair, 0 for none. */
    public static final int STRIKE = 12;

    public static void encode(C2SPaleAction message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.action);
        buffer.writeByte(message.arg);
        buffer.writeInt(message.targetId);
    }

    public static C2SPaleAction decode(FriendlyByteBuf buffer) {
        return new C2SPaleAction(buffer.readByte(), buffer.readByte(), buffer.readInt());
    }

    public static void handle(C2SPaleAction message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (!com.hex.kagune.control.DragonLock.locked(sender)) {
                    PaleServer.onAction(sender, message.action, message.arg, message.targetId);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
