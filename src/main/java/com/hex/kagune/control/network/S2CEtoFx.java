package com.hex.kagune.control.network;

import com.hex.kagune.control.client.fx.EtoFx;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 6.1.0: something of Eto's every client nearby should see for a moment: an impact (and how the body it hit reels), a blink's
 * afterimage, the hammer's shockwave, a body smashing through blocks. Purely visual: damage, knockback and broken blocks are the
 * server's.
 *
 * @param kind   one of the constants below
 * @param owner  entity id of the Eto player
 * @param target entity id of what was hit (0: none)
 * @param size   how hard (0..1 and over)
 * @param extra  per kind: a strike, a block's state id, a duration
 */
public record S2CEtoFx(int kind, int owner, int target, double x, double y, double z, float vx, float vy, float vz, float size, int extra) {
    /** A fist, palm, foot or the flesh landed at {@code x, y, z}, driving along {@code v} ({@code extra}: what landed, see EtoFx). */
    public static final int IMPACT = 0;
    /** She blinked: gone from {@code x, y, z} (an afterimage left there) toward {@code v} (where she reappears). */
    public static final int BLINK = 1;
    /** The hammer hit the ground at {@code x, y, z}: the shockwave, {@code size} its radius, {@code extra} the ground's block state id. */
    public static final int SHOCKWAVE = 2;
    /**
     * A launched body broke a block at {@code x, y, z} ({@code extra}: its state id), flying along {@code v}. 6.2.0: sent by a body
     * the Owl's punch drives, {@code size} the blocks it broke this tick: debris thrown on along its line, dust, the view jolted near.
     */
    public static final int SMASH = 3;
    /** A launched body slammed into something it could not break at {@code x, y, z}. */
    public static final int CRASH = 4;
    /** A broken block grew back at {@code x, y, z} ({@code extra}: its state id). */
    public static final int REGROW = 5;
    /**
     * 6.2.0: the Speed Kick's foot landed in the side of {@code target}'s head at {@code x, y, z}, launching it along {@code v}: it
     * snaps, tumbles, comes down and gets up (nothing else: no light, no vapour).
     */
    public static final int KICKED = 9;
    /**
     * 6.2.0: a Watch This beak went through {@code target}'s chest at {@code x, y, z}, going along {@code v}: blood out of its back,
     * a hole left in it ({@code size}: its height; {@code extra}: the tentacle's side).
     */
    public static final int IMPALED = 10;
    /**
     * 6.2.0: the Mutated Arm's hand closed on {@code target} at {@code x, y, z} (going along {@code v}); or ({@code target} 0) one
     * of its sphere's squeezes at {@code x, y, z}, {@code size} its radius, {@code extra} which squeeze: blood forced out of it.
     */
    public static final int CRUSHED = 11;

    /** An {@link #IMPACT}'s {@code extra} when it is not a strike of the blitz (those are their ordinals): Big Punch. */
    public static final int BIG_PUNCH = 100;
    /** An {@link #IMPACT}'s {@code extra} when the flesh landed (the swipe's rake, the grab's throw). */
    public static final int FLESH = 101;
    /** An {@link #IMPACT}'s {@code extra} when the swipe's blade cut: blood thrown the way it swung, and the cut's streak. */
    public static final int BONE = 104;
    /**
     * 6.2.0: an {@link #IMPACT}'s {@code extra} for the Owl's Big Punch: the air thrown level along the blow in rings marching out
     * along it, the ground torn up under it, the view of everyone near jolted.
     */
    public static final int OWL_PUNCH = 105;
    /** 6.2.0: an {@link #IMPACT}'s {@code extra} for the Owl's Reaping Whirl beginning: a flat ring of torn air thrown out round her, dust off the ground. */
    public static final int OWL_WHIRL = 106;

    public static void encode(S2CEtoFx message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.kind);
        buffer.writeInt(message.owner);
        buffer.writeInt(message.target);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.vx);
        buffer.writeFloat(message.vy);
        buffer.writeFloat(message.vz);
        buffer.writeFloat(message.size);
        buffer.writeInt(message.extra);
    }

    public static S2CEtoFx decode(FriendlyByteBuf buffer) {
        return new S2CEtoFx(buffer.readByte(), buffer.readInt(), buffer.readInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
            buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt());
    }

    public static void handle(S2CEtoFx message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EtoFx.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
