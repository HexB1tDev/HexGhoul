package com.hex.kagune.control.client;

import com.hex.kagune.control.TendrilShield;
import com.hex.kagune.control.network.S2CTendrilShield;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 5.0.14: Rinkaku's tentacles guarding him against incoming shots, as every client draws them (the server decides,
 * {@code TendrilShieldServer}; the rules and the pose are {@link TendrilShield}'s). Each guarding tentacle swings out of its
 * usual path into the guard over {@link TendrilShield#RAISE_TICKS}, holds it across the line the shots come in on, and folds
 * back over {@link TendrilShield#LOWER_TICKS} when the server lowers it. Applied to the tentacle's drawn path in
 * {@code StrikePaths.clearOwner}, after the smoothing and before the body clearance.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class TendrilShieldClient {
    /** One tentacle in (or leaving) its guard: the direction the shots come from, when it went up, and when it started down. */
    private record Guard(int limb, double[] dir, long raisedAt, double loweredAt) {
    }

    private static final Map<UUID, List<Guard>> GUARDS = new HashMap<>();

    private TendrilShieldClient() {
    }

    public static void accept(S2CTendrilShield message) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        double now = level == null ? 0.0 : level.m_46467_();
        List<Guard> before = GUARDS.getOrDefault(message.playerId(), List.of());
        List<Guard> after = new ArrayList<>();
        for (int i = 0; i < message.limbs().length; i++) {
            after.add(new Guard(message.limbs()[i], TendrilShield.direction(message.yaws()[i], message.pitches()[i]), message.raisedAt()[i],
                Double.NaN));
        }
        for (Guard old : before) {
            boolean still = false;
            for (Guard guard : after) {
                still |= guard.limb() == old.limb();
            }
            if (!still) {
                // Lowered by the server: it folds back from where it is.
                after.add(Double.isNaN(old.loweredAt()) ? new Guard(old.limb(), old.dir(), old.raisedAt(), now) : old);
            }
        }
        after.removeIf(guard -> !Double.isNaN(guard.loweredAt()) && now - guard.loweredAt() > TendrilShield.LOWER_TICKS + 1);
        if (after.isEmpty()) {
            GUARDS.remove(message.playerId());
        } else {
            GUARDS.put(message.playerId(), after);
        }
    }

    /**
     * {@code path} (world, from the tentacle's root) of {@code player}'s {@code limb} at {@code time} (ticks, partial included),
     * moved into its guard as far as it is raised; null when that tentacle isn't guarding. {@code feet} is where the player
     * stands and {@code yaw} the body's facing (radians).
     */
    public static double[][] apply(double[][] path, int limb, AbstractClientPlayer player, double[] feet, double yaw, double time) {
        List<Guard> guards = GUARDS.get(player.m_36316_().getId());
        if (guards == null || path.length < 3) {
            return null;
        }
        for (Guard guard : guards) {
            if (guard.limb() != limb) {
                continue;
            }
            float amount = TendrilShield.amount(time - guard.raisedAt(), Double.isNaN(guard.loweredAt()) ? Double.NaN : time - guard.loweredAt());
            if (amount <= 0.001F) {
                return null;
            }
            double[] centre = {feet[0], feet[1] + player.m_20206_() * 0.55, feet[2]};
            double[] forward = {-Math.sin(yaw), 0.0, Math.cos(yaw)};
            double[][] pose = TendrilShield.pose(path[0], centre, guard.dir(), forward, path.length);
            return TendrilShield.blend(path, pose, amount);
        }
        return null;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GUARDS.clear();
        com.hex.kagune.control.client.render.TendrilPaths.clear();
    }
}
