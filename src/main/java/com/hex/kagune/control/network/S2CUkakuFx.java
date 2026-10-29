package com.hex.kagune.control.network;

import com.hex.kagune.control.client.fx.UkakuFx;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Something of Ukaku's fire or crystal that every client nearby should see: a shard or spike leaving a wing (with its id,
 * start, velocity and life, so clients fly it themselves), where it ended, a Rising Flame pillar, the dash and hop bursts,
 * embers left burning, a stuck spike bursting, blood thrown from a wound. Purely visual: all damage is the server's.
 *
 * @param kind    {@link #BOLT} + the bolt kind (0 shot, 1 flare, 2 fan, 3 barrage), or one of the other kinds below
 * @param shooter entity id of the Ukaku player
 * @param id      the bolt's id (bolt kinds, {@link #IMPACT}, {@link #FIZZLE}), else 0
 * @param size    the bolt's size; for an impact 1 if it hit something; the pillar's radius
 * @param extra   the bolt's life in ticks; for an impact or fizzle the bolt kind; the pillar's catch count; durations
 */
public record S2CUkakuFx(int kind, int shooter, int id, double x, double y, double z, float vx, float vy, float vz, float size, int extra) {
    /** Bolt kinds occupy BOLT .. BOLT + 3. */
    public static final int BOLT = 0;
    public static final int IMPACT = 8;
    public static final int FIZZLE = 9;
    public static final int PILLAR = 10;
    public static final int HOP = 11;
    public static final int DASH = 12;
    public static final int EMBERS = 13;
    // ---- the crystal mode. Spike kinds occupy SPIKE .. SPIKE + 4 (shot, lance, fan, rain, barrage), flown like bolts.
    public static final int SPIKE = 16;
    /** A spike stopped: {@code size} 1 if it stuck in someone (it is drawn there from now on), else it stuck in a block. */
    public static final int SPIKE_IMPACT = 21;
    public static final int SPIKE_FIZZLE = 22;
    /** A stuck spike bursting at {@code x, y, z} out along {@code v}: crystal shards and blood. */
    public static final int SPIKE_BURST = 23;
    /** The Crystal Bloom going off around the shooter ({@code extra}: how many spikes it burst). */
    public static final int BLOOM = 24;
    /** Blood thrown out of a wound at {@code x, y, z} along {@code v}, as hard as {@code size}. */
    public static final int BLOOD = 25;
    /** A bursting spike exploding (the Bursting Shards mutation), {@code size} its radius. */
    public static final int BURST_BLAST = 26;
    /** The wings crystallizing ({@code extra} 1) or thawing (0): a shimmer at the shoulder blades. */
    public static final int CRYSTALLIZE = 27;
    // ---- the Wing Vortex (Z).
    /** A wing thrown as a vortex: its id, start, velocity, {@code size} (the wing's size) and life, flown by the clients. */
    public static final int VORTEX = 28;
    /** The vortex catching a body at {@code x, y, z}, flying along {@code v}. */
    public static final int VORTEX_HIT = 29;
    /** The vortex ending (its range or a wall): it unwinds and fades over {@code extra} ticks. */
    public static final int VORTEX_END = 30;
    /** A kakuja needle leaves a tentacle's spike row (size, life in {@code extra}). */
    public static final int NEEDLE = 31;
    /** The final kakuja: a needle wrapped in flame (a NEEDLE that burns what it hits, drawn trailing fire). */
    public static final int FLAME_NEEDLE = 32;
    /**
     * 3.0.3: one of Rinkaku's tails hit a body ({@code id}) at {@code x, y, z}, going into it along {@code v}: blood, and on a
     * player wounds shaped by the blow ({@code extra}: {@code WoundBook.Blow} ordinal + 16 x (the slash's angle in degrees + 180));
     * {@code size} is how hard.
     */
    public static final int RINKAKU_HIT = 33;
    // ---- 5.0.15: the crystal thorns (an Ukaku in the crystal mode held by a Rinkaku's tails).
    /** Thorns erupt from the shooter's body: {@code id} the layout's seed, {@code extra} how many ({@code CrystalThorns.thorns}). */
    public static final int THORNS = 34;
    /** The thorns went through what held him and it let go: crystal and blood bursting out round his chest at {@code x, y, z}. */
    public static final int THORNS_PIERCE = 35;
    /** The thorns end: they sink back into him ({@code extra} 0) or shatter (1, the crystal thawed). */
    public static final int THORNS_END = 36;
    /**
     * 5.0.17: any other hit on a player ({@code id}; {@code shooter} whoever dealt it) at {@code x, y, z} going into him along
     * {@code v} ((0, 0, 0): nowhere in particular): the wounds it opens take its shape there, as {@link #RINKAKU_HIT}'s do
     * ({@code extra} the same: the {@code WoundBook.Blow} + 16 x (the swing's angle in degrees + 180); {@code size} how hard).
     */
    public static final int HIT = 37;
    /**
     * 5.0.20: the Steel Blade cut a body ({@code id}; {@code shooter} the swordsman) at {@code x, y, z}, the blade going along
     * {@code v}: blood flung along the cut, and the blade bloodied; {@code size} how hard, {@code extra} 1 when it killed (a burst,
     * gushes from the body, a pool).
     */
    public static final int BLADE_HIT = 38;
    /** 6.0.0: the kakuja's passive: he ({@code shooter}) vanishes in a gust of wind at {@code x, y, z}. */
    public static final int VANISH = 39;
    /** 6.0.0: he ({@code shooter}) comes back out of the wind at {@code x, y, z}. */
    public static final int REVEAL = 40;
    /** 6.0.0: the kakuja's dash: a rush of wind from {@code x, y, z} along {@code v}, {@code size} how strong. */
    public static final int GUST = 41;

    public static void encode(S2CUkakuFx message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.kind);
        buffer.writeInt(message.shooter);
        buffer.writeInt(message.id);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.vx);
        buffer.writeFloat(message.vy);
        buffer.writeFloat(message.vz);
        buffer.writeFloat(message.size);
        buffer.writeShort(Math.max(0, Math.min(32767, message.extra)));
    }

    public static S2CUkakuFx decode(FriendlyByteBuf buffer) {
        return new S2CUkakuFx(buffer.readByte(), buffer.readInt(), buffer.readInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
            buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readShort());
    }

    public static void handle(S2CUkakuFx message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> UkakuFx.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
