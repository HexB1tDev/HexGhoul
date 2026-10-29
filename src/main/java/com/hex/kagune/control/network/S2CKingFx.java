package com.hex.kagune.control.network;

import com.hex.kagune.control.client.KingFx;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.0.13: something one of War's abilities does out in the world, for every client near to draw ({@link KingFx}): its {@code kind}
 * (the constants here), the bodies it concerns ({@code ids}) and where ({@code data}, as each kind reads it).
 */
public record S2CKingFx(UUID playerId, int kind, int[] ids, float[] data) {
    /** King's Execution: the centipedes out of his arm at {@code ids[0]} (0: at nothing, {@code data} where they lash). */
    public static final int GRASP = 0;
    /** King's Execution: what they hold is crushed. */
    public static final int CRUSH = 1;
    /** Black Reaper: the dash, {@code data} from x, y, z to x, y, z. */
    public static final int DASH = 2;
    /** Black Reaper: the cleave, {@code data} at x, y, z facing x, z; {@code ids} what it killed (cut in half). */
    public static final int CLEAVE = 3;
    /** Spire Wave: {@code data} from x, y, z along x, z, then the ground's height under each of its steps. */
    public static final int SPIRE = 4;
    /** King's Defiance: the blink, {@code data} from x, y, z to x, y, z. */
    public static final int BLINK = 5;
    /** King's Defiance: the cocoon bursts, {@code data} at x, y, z. */
    public static final int BURST = 6;
    /** Black Sun (1.0.15): the ground cracks open at {@code data} x, y, z. */
    public static final int SUN = 7;
    /** 1.0.13's rain (no longer sent). */
    public static final int RAIN = 8;
    /**
     * 1.0.14: one of War's M1 blows landing (or cutting air): {@code ids[0]} which ({@code WarStrike}), the rest the bodies it hit;
     * {@code data} his chest x, y, z, the way he struck x, z, then where each body was hit, x, y, z.
     */
    public static final int STRIKE = 9;
    /**
     * 1.0.14: one of War's M1 blows begun: {@code ids[0]} which ({@code WarStrike}), {@code ids[1]} at whom (0: no one); {@code data} the
     * way he faces x, y, z. Every client draws the blade's arc with him, timed to land on the impact tick.
     */
    public static final int SWING = 10;
    /** 1.0.15: Black Sun: the centipedes up out of the ground at {@code data} x, y, z, one for each of {@code ids}, which they lift. */
    public static final int SEIZE = 11;
    /** 1.0.15: Black Sun: {@code ids[0]} slammed down into the ground at {@code data} x, y, z. */
    public static final int SLAM = 12;
    /** 1.0.16: Peace: blade {@code ids[0]} sent out on its own at {@code ids[1]} (the Thousand Cuts). 1.0.34: no longer sent (kept, unused). */
    public static final int PEACE_CUT = 13;
    /** 1.0.16: Peace: its blades met {@code ids} at {@code data} (x, y, z each). */
    public static final int PEACE_HIT = 14;
    /** 1.0.16: Peace: The Final Cross met at {@code data} x, y, z, its wave going on along x, z. 1.0.34: no longer sent (kept, unused). */
    public static final int PEACE_CROSS = 15;
    /** 1.0.19: a mark a blow left (KingMarkFx): ids {style, the body it was on or 0}; data {where, which way, how strong (x)}. */
    public static final int MARK = 16;
    /**
     * 1.0.27: spikes breaking the ground ({@code data}: per spike its base x, y, z, which way it leans x, z, how tall, how thick, how many
     * ticks after the effect it comes up).
     */
    public static final int SPIKES = 17;
    /** 1.0.27: the blitz's dash or blink from one point to the next ({@code data}: from, to), the owner flashing out of sight on the way. */
    public static final int BLITZ = 18;
    /** 1.0.27: a body driven into the ground: the crater, the cracks, the shaking ({@code data}: where, {radius, how far it shakes, 0}). */
    public static final int CRATER = 19;
    /**
     * 1.0.27: Black Sun's punch: the ring of air thrown out from it ({@code data}: where, {radius, ticks, how far it shakes}; 1.0.33: and
     * {the punch's way x, z, the ground's height}: the ring stands up square to it, down to the ground).
     */
    public static final int AIR_RING = 20;
    /** 1.0.28: King's Defiance: a spike out of the cocoon at what struck it (ids: what; data: where it comes out of it, its point). */
    public static final int DEFY_SPIKE = 21;
    /**
     * 1.0.28: the Spire Wave let go (ids: its seed; data: from, its way, its reach, the mass's size, its speed, the charge, how many
     * centipedes): every client runs it from these by the same clock as the server ({@code WarRules.spireFront}).
     */
    public static final int SPIRE_WAVE = 22;
    // 1.0.34: Peace's reworked six (PeaceFx draws them).
    /** A blade driven into a body and left there (ids: the blade, the body, how: 0 the Waltz, 1 the Horizon, 2 the Horizon's uppercut). */
    public static final int PEACE_IMPALE = 23;
    /** The blades in a body let go (ids: the body, how: 0 torn out of it every way, 1 drawn out). */
    public static final int PEACE_TEAR = 24;
    /** Aegis of :Re's shields out (ids: 1) or gone (ids: 0). */
    public static final int PEACE_AEGIS = 25;
    /** One of its shields at work (ids: which, what: PeaceRules.SHIELD_*; data: where it goes). */
    public static final int PEACE_SHIELD = 26;
    /** White Wing Ascent's two tendrils (ids: what now: 0 lifting, 1 wrenching him down, 2 letting him down, 3 gone; data: their feet). */
    public static final int PEACE_LIFT = 27;
    /** The flesh torn up out of the ground (ids: its seed; data: where, its radius, how many, then each tendril's ground height, NaN: none). */
    public static final int PEACE_ERUPT = 28;
    /** One of the eruption's tendrils lashing at a body (ids: its seed, which tendril, the body). */
    public static final int PEACE_LASH = 29;
    /**
     * Piercing Salvation's lance (ids: what now: 0 gathering, 1 driven out, 2 through a body, 3 pinned, 4 called back; the body; data:
     * from, its way, its length, its speed).
     */
    public static final int PEACE_LANCE = 30;
    /** 1.0.34: one of Rebirth Instinct's creatures (ids: its entity, what: 0 born (its life), 1 its blades out (at whom), 2 in again, 3 a
     *  swing (at whom), 4 melting). */
    public static final int PEACE_THRALL = 31;
    private static final int MAX = 2048; // 1.0.27: room for a cone of spikes

    public static void encode(S2CKingFx message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.kind);
        buffer.writeShort(Math.min(MAX, message.ids.length));
        for (int i = 0; i < Math.min(MAX, message.ids.length); i++) {
            buffer.writeInt(message.ids[i]);
        }
        buffer.writeShort(Math.min(MAX, message.data.length));
        for (int i = 0; i < Math.min(MAX, message.data.length); i++) {
            buffer.writeFloat(message.data[i]);
        }
    }

    public static S2CKingFx decode(FriendlyByteBuf buffer) {
        UUID id = buffer.m_130259_();
        int kind = buffer.readByte();
        int[] ids = new int[Math.min(MAX, Math.max(0, buffer.readShort()))];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = buffer.readInt();
        }
        float[] data = new float[Math.min(MAX, Math.max(0, buffer.readShort()))];
        for (int i = 0; i < data.length; i++) {
            data[i] = buffer.readFloat();
        }
        return new S2CKingFx(id, kind, ids, data);
    }

    public static void handle(S2CKingFx message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KingFx.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
