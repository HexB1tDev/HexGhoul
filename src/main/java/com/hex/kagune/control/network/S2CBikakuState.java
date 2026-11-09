package com.hex.kagune.control.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 3.4.9: what a Bikaku player's worm is doing, for them and everyone tracking them: the bite of the M1 string it is making
 * ({@code BikakuRules.Bite} ordinal, 0 none) and the game tick it began, where it is thrown (a point in the world: the target's middle when
 * it began, or the air ahead), and the body it goes after (an entity id, 0 none: the clients follow it as it moves). 3.5.0: and the move
 * it is making ({@code BikakuMove} ordinal, 0 none), the tick it began, its point (the world), the bodies it goes after ({@link #TARGETS},
 * 0 none), the body it holds (0 none: missed, or let go) and whether its bite caught anything; 3.5.3: when Monstrous Regeneration last began; 3.5.4: whether it is in the kakuja and when it last went in or out. 3.6.0: his form ({@code BikakuForm} ordinal). 3.6.1: a worm move made by several worms: how many, whom each holds, which caught, each one's mark (the world). 3.6.3: his mutations ({@code BikakuMutation} bits), and Grand Devouring: the body it feeds on (0 none), the worm making it, when it began, where its victim was caught (its feet, the world) and its size, and when it can feed again (its cooldown, world time). 3.5.9: the ways round blocks (the world; none for a straight line). 3.5.8: how far the bite and the move out were thrown, and a key held to wind one up ({@code charge}: 9 M1, else its {@code BikakuMove.Slot} ordinal; -1 none) since when. 3.5.7: {@link #TARGETS} is one body for each of the kakuja's tentacles (Thousand Jaws; Ravenous Pursuit uses the first three); when Undying Abomination last began; for each tentacle, when its last bite of its own began (Autonomous Predation) and the body it went after. Every client draws the worm from these alone.
 */
public record S2CBikakuState(UUID playerId, int bite, long startedAt, double x, double y, double z, int targetId, int move, long moveAt,
                             double mx, double my, double mz, int[] targets, int heldId, boolean caught, long regenAt,
                             boolean kakuja, long kakujaAt, long undyingAt, long[] autoAt, int[] autoTarget, float reach, float moveReach, int charge,
                             long chargeAt, double[][][] routes, int form, int moveWorms, int[] held, int caughtBits, double[][] points,
                             int mutations, int devourVictim, int devourWorm, long devourAt, double[] devourSpot, float devourWidth, float devourHeight,
                             long devourReadyAt) {
    /** 3.5.9: how many ways round blocks it carries (the bite's, then the move's: one for each of Ravenous Pursuit's worms), and their points. */
    public static final int ROUTES = 4;
    public static final int ROUTE_POINTS = 8;
    /** How many bodies a move can go after at once (3.5.7: one for each of the kakuja's tentacles). */
    public static final int TARGETS = 6;
    /** 3.5.7: how many of the kakuja's tentacles can bite on their own (one entry each). */
    public static final int AUTOS = 6;

    public static void encode(S2CBikakuState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.bite);
        buffer.m_130103_(message.startedAt);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeInt(message.targetId);
        buffer.writeByte(message.move);
        buffer.m_130103_(message.moveAt);
        buffer.writeDouble(message.mx);
        buffer.writeDouble(message.my);
        buffer.writeDouble(message.mz);
        for (int i = 0; i < TARGETS; i++) {
            buffer.writeInt(message.targets != null && i < message.targets.length ? message.targets[i] : 0);
        }
        buffer.writeInt(message.heldId);
        buffer.writeBoolean(message.caught);
        buffer.m_130103_(message.regenAt);
        buffer.writeBoolean(message.kakuja);
        buffer.m_130103_(message.kakujaAt);
        buffer.m_130103_(message.undyingAt);
        for (int i = 0; i < AUTOS; i++) {
            buffer.m_130103_(message.autoAt != null && i < message.autoAt.length ? message.autoAt[i] : Long.MIN_VALUE);
            buffer.writeInt(message.autoTarget != null && i < message.autoTarget.length ? message.autoTarget[i] : 0);
        }
        buffer.writeFloat(message.reach);
        buffer.writeFloat(message.moveReach);
        buffer.writeByte(message.charge);
        buffer.m_130103_(message.chargeAt);
        for (int r = 0; r < ROUTES; r++) {
            double[][] route = message.routes != null && r < message.routes.length ? message.routes[r] : null;
            int count = route == null ? 0 : Math.min(ROUTE_POINTS, route.length);
            buffer.writeByte(count);
            for (int i = 0; i < count; i++) {
                buffer.writeDouble(route[i][0]);
                buffer.writeDouble(route[i][1]);
                buffer.writeDouble(route[i][2]);
            }
        }
        buffer.writeByte(message.form);
        buffer.writeByte(message.moveWorms);
        for (int i = 0; i < 3; i++) {
            buffer.writeInt(message.held != null && i < message.held.length ? message.held[i] : 0);
            double[] p = message.points != null && i < message.points.length ? message.points[i] : null;
            buffer.writeBoolean(p != null);
            if (p != null) {
                buffer.writeDouble(p[0]);
                buffer.writeDouble(p[1]);
                buffer.writeDouble(p[2]);
            }
        }
        buffer.writeByte(message.caughtBits);
        // 3.6.3
        buffer.writeInt(message.mutations);
        buffer.writeInt(message.devourVictim);
        buffer.writeByte(message.devourWorm);
        buffer.m_130103_(message.devourAt);
        double[] spot = message.devourSpot == null ? new double[3] : message.devourSpot;
        buffer.writeDouble(spot[0]);
        buffer.writeDouble(spot[1]);
        buffer.writeDouble(spot[2]);
        buffer.writeFloat(message.devourWidth);
        buffer.writeFloat(message.devourHeight);
        buffer.m_130103_(message.devourReadyAt);
    }

    public static S2CBikakuState decode(FriendlyByteBuf buffer) {
        UUID id = buffer.m_130259_();
        int bite = buffer.readByte();
        long startedAt = buffer.m_130258_();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        int targetId = buffer.readInt();
        int move = buffer.readByte();
        long moveAt = buffer.m_130258_();
        double mx = buffer.readDouble();
        double my = buffer.readDouble();
        double mz = buffer.readDouble();
        int[] targets = new int[TARGETS];
        for (int i = 0; i < TARGETS; i++) {
            targets[i] = buffer.readInt();
        }
        int heldId = buffer.readInt();
        boolean caught = buffer.readBoolean();
        long regenAt = buffer.m_130258_();
        boolean kakuja = buffer.readBoolean();
        long kakujaAt = buffer.m_130258_();
        long undyingAt = buffer.m_130258_();
        long[] autoAt = new long[AUTOS];
        int[] autoTarget = new int[AUTOS];
        for (int i = 0; i < AUTOS; i++) {
            autoAt[i] = buffer.m_130258_();
            autoTarget[i] = buffer.readInt();
        }
        float reach = buffer.readFloat();
        float moveReach = buffer.readFloat();
        int charge = buffer.readByte();
        long chargeAt = buffer.m_130258_();
        double[][][] routes = new double[ROUTES][][];
        for (int r = 0; r < ROUTES; r++) {
            int count = Math.max(0, Math.min(ROUTE_POINTS, buffer.readByte()));
            if (count == 0) {
                continue;
            }
            routes[r] = new double[count][];
            for (int i = 0; i < count; i++) {
                routes[r][i] = new double[] {buffer.readDouble(), buffer.readDouble(), buffer.readDouble()};
            }
        }
        int form = buffer.readByte();
        int moveWorms = buffer.readByte();
        int[] held = new int[3];
        double[][] points = new double[3][];
        for (int i = 0; i < 3; i++) {
            held[i] = buffer.readInt();
            if (buffer.readBoolean()) {
                points[i] = new double[] {buffer.readDouble(), buffer.readDouble(), buffer.readDouble()};
            }
        }
        int caughtBits = buffer.readByte();
        int mutations = buffer.readInt();
        int devourVictim = buffer.readInt();
        int devourWorm = buffer.readByte();
        long devourAt = buffer.m_130258_();
        double[] devourSpot = {buffer.readDouble(), buffer.readDouble(), buffer.readDouble()};
        float devourWidth = buffer.readFloat();
        float devourHeight = buffer.readFloat();
        long devourReadyAt = buffer.m_130258_();
        return new S2CBikakuState(id, bite, startedAt, x, y, z, targetId, move, moveAt, mx, my, mz, targets, heldId, caught, regenAt, kakuja, kakujaAt,
            undyingAt, autoAt, autoTarget, reach, moveReach, charge, chargeAt, routes, form, moveWorms, held, caughtBits, points, mutations, devourVictim,
            devourWorm, devourAt, devourSpot, devourWidth, devourHeight, devourReadyAt);
    }

    public static void handle(S2CBikakuState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hex.kagune.control.client.BikakuClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
