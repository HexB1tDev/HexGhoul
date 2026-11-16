package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.RinkakuBlock;
import com.hex.kagune.control.RinkakuIdle;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.TendrilMode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.0.51: a Rinkaku's tails at rest as every client draws them ({@link RinkakuIdle}: the designed shape, swaying, trailing as he runs),
 * in place of the original chain's own rest (coiled, short, twitching). Each tail settles into it once the kagune is at rest and leaves it
 * at once for a move ({@link RinkakuIdle#settle}), blended over the path its motion gave it; his way and turn are smoothed so the trail
 * never twitches with them. Applied in {@code StrikePaths.clearOwner}, after the smoothing and before the guards and the body clearance.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class RinkakuIdleClient {
    /** Each tail's weight in its rest and when it was last drawn, by entity id and limb. */
    private static final Map<Long, double[]> WEIGHTS = new HashMap<>();
    /** Each player's smoothed way (forward, sideways: blocks a tick) and turn (radians a tick), and when last read. */
    private static final Map<Integer, double[]> MOTION = new HashMap<>();
    /** How quickly the smoothed way follows his own (a share a tick). */
    private static final double FOLLOW = 0.18;

    private RinkakuIdleClient() {
    }

    /**
     * {@code path} (world, root first) of {@code player}'s tail {@code limb} at {@code time} (ticks, partial included) moved as far into its
     * rest as it has settled; null while it is wholly its motion's. {@code feet} is where he stands, {@code yaw} his body's facing (radians).
     */
    public static double[][] apply(double[][] path, int limb, AbstractClientPlayer player, KaguneClientState.View view, double[] feet, double yaw,
                                   double time) {
        if (path.length < 3) {
            return null;
        }
        if (WEIGHTS.size() > 1024) {
            WEIGHTS.clear();
            MOTION.clear();
        }
        long key = (long)player.m_19879_() * 32L + limb;
        double[] state = WEIGHTS.computeIfAbsent(key, k -> new double[] {0.0, time});
        // 3.0.5: holding someone, the tails not holding them rest as they do at peace (not the original's twitching coil).
        boolean rest = view.active() && (view.mode() == TendrilMode.IDLE
            || com.hex.kagune.control.client.render.StrikePaths.holding(view.mode()) && view.heldEntityId(limb) == 0);
        double dt = time - state[1];
        if (!(dt >= 0.0) || dt > 40.0) {
            state[0] = rest ? 1.0 : 0.0; // back after a long while (out of sight): as it is now
            dt = 0.0;
        }
        state[0] = RinkakuIdle.settle(state[0], rest, dt);
        state[1] = time;
        double weight = smooth(state[0]);
        if (weight <= 0.001) {
            return null;
        }
        KaguneForm form = view.form();
        int tails = 0;
        int index = -1;
        for (int i = 0; i < form.limbCount(); i++) {
            if (!form.limb(i).segmented()) {
                if (i == limb) {
                    index = tails;
                }
                tails++;
            }
        }
        if (index < 0) {
            return null;
        }
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double[] moving = motion(player, fx, fz, rx, rz, time);
        double length = view.limbLength(form.limb(limb));
        int seed = player.m_20148_().hashCode() + limb * 131;
        // 2.1.6: its side as the original has it, by its own place among the limbs when its layout gives none.
        double fan = Math.abs(form.limb(limb).fanSide()) >= 0.01 ? form.limb(limb).fanSide() : limb % 2 == 0 ? -1.0 : 1.0;
        double[][] pose = RinkakuIdle.pose(index, tails, fan, path.length, player.m_20206_(), length, time, seed,
            moving[0], moving[1], moving[2]);
        double[][] world = new double[pose.length][];
        for (int i = 0; i < pose.length; i++) {
            double[] q = pose[i];
            world[i] = new double[] {feet[0] + q[0] * rx + q[2] * fx, feet[1] + q[1], feet[2] + q[0] * rz + q[2] * fz};
        }
        return RinkakuBlock.blend(path, world, weight);
    }

    /** His way and turn, smoothed: {forward, sideways, turn (+ to his left)}. */
    private static double[] motion(AbstractClientPlayer player, double fx, double fz, double rx, double rz, double time) {
        double dx = player.m_20185_() - player.f_19790_;
        double dz = player.m_20189_() - player.f_19792_;
        float turned = player.f_20883_ - player.f_20884_;
        turned -= 360.0F * Math.round(turned / 360.0F);
        double[] now = {dx * fx + dz * fz, dx * rx + dz * rz, -Math.toRadians(turned)};
        double[] kept = MOTION.computeIfAbsent(player.m_19879_(), id -> new double[] {now[0], now[1], now[2], time});
        double dt = time - kept[3];
        if (!(dt >= 0.0) || dt > 40.0) {
            System.arraycopy(now, 0, kept, 0, 3);
        } else if (dt > 0.0) {
            double k = 1.0 - Math.pow(1.0 - FOLLOW, dt);
            for (int i = 0; i < 3; i++) {
                kept[i] += (now[i] - kept[i]) * k;
            }
        }
        kept[3] = time;
        return new double[] {kept[0], kept[1], kept[2]};
    }

    private static double smooth(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WEIGHTS.clear();
        MOTION.clear();
    }
}
