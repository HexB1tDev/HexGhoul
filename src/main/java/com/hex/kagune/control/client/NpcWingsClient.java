package com.hex.kagune.control.client;

import com.hex.kagune.control.network.S2CNpcWings;
import com.hex.kagune.control.npc.WingRules;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * 1.3.19: each companion's Ukaku wings (Touka's) as this client draws them ({@link S2CNpcWings}), on this client's clock: how far out,
 * how crystal, how curled forward to aim, each wing's kick as a spike leaves it.
 */
public final class NpcWingsClient {
    /** Her wings as the server last said; {@code ...Began} are client clock times. */
    public record View(boolean out, double outBegan, boolean crystal, double crystalBegan, int prey, long shotAt, double shotBegan, int shotSide,
                       long volleyAt, double volleyBegan) {
    }

    private static final Map<Integer, View> VIEWS = new HashMap<>();

    private NpcWingsClient() {
    }

    public static void accept(S2CNpcWings message) {
        Minecraft minecraft = Minecraft.m_91087_();
        long world = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        double clock = PaleClient.clock(0.0F);
        VIEWS.put(message.id(), new View(message.out(), began(world, clock, message.outAt()), message.crystal(), began(world, clock, message.crystalAt()),
            message.prey(), message.shotAt(), began(world, clock, message.shotAt()), message.shotSide(), message.volleyAt(),
            began(world, clock, message.volleyAt())));
    }

    private static double began(long world, double clock, long at) {
        if (at == Long.MIN_VALUE) {
            return -1.0E6;
        }
        return clock - Math.max(0L, Math.min(world - at, 10_000L));
    }

    public static View get(int id) {
        return VIEWS.get(id);
    }

    /** How far out her wings are (0..1), eased in as they come out and out as they go in. */
    public static float deploy(View view, double clock) {
        if (view == null) {
            return 0.0F;
        }
        double t = Math.max(0.0, Math.min(1.0, (clock - view.outBegan()) / WingRules.DEPLOY_TICKS));
        double eased = t * t * (3.0 - 2.0 * t);
        return (float)(view.out() ? eased : 1.0 - eased);
    }

    /** How crystal they are (0..1). */
    public static float crystal(View view, double clock) {
        if (view == null) {
            return 0.0F;
        }
        double t = Math.max(0.0, Math.min(1.0, (clock - view.crystalBegan()) / 8.0));
        return (float)(view.crystal() ? t : 1.0 - t);
    }

    /** How curled forward to aim (0..1): through a volley, and a while after its last spike. */
    public static float aim(View view, double clock) {
        if (view == null) {
            return 0.0F;
        }
        if (view.volleyAt() != Long.MIN_VALUE) {
            return (float)Math.max(0.0, Math.min(1.0, (clock - view.volleyBegan()) / 4.0));
        }
        double since = clock - view.shotBegan();
        return (float)Math.max(0.0, Math.min(1.0, 1.0 - (since - 4.0) / 10.0));
    }

    /** A wing's kick as a spike leaves it ({@code side} 1 right, -1 left): a jolt that dies away. */
    public static float kick(View view, int side, double clock) {
        if (view == null || view.shotAt() == Long.MIN_VALUE || view.shotSide() != side) {
            return 0.0F;
        }
        double since = clock - view.shotBegan();
        return since < 0.0 ? 0.0F : (float)Math.exp(-since * 0.55);
    }

    /** Every companion this client knows wings of (entity ids; a copy). */
    public static int[] ids() {
        return VIEWS.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    /** Forgets one gone (or everything, leaving the world). */
    public static void forget(int id) {
        VIEWS.remove(id);
    }

    public static void clear() {
        VIEWS.clear();
    }
}
