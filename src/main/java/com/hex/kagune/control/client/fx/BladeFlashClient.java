package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.UkakuClient;
import com.hex.kagune.control.client.animation.BladeArmPose;
import com.hex.kagune.control.client.animation.KaguneBodyAnimation;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.render.KingAfterimages;
import com.hex.kagune.control.ukaku.SteelFlash;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.2.9 (the owner: "Blade Arm Control + right click (does a high speed dash, only leaving behind after images)"): every client's side of the
 * Steel Blade's flash ({@code BladeFlash}). The server hides him for it (the vanish's own invisibility: nothing of him drawn); here, each tick
 * of it, his afterimages are laid along the stretch he crossed since the last, one every {@link SteelFlash#GHOST_EVERY} blocks, in the stride
 * he is in, pale as steel, the blade on each one's arm ({@link KingAfterimages}), with a thin white streak of wind through them.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BladeFlashClient {
    /** One flash under way on this client: until when (world ticks) its stretch is read, and where he was last tick. */
    private static final class Flash {
        final double until;
        Vec3 last;
        /** Afterimages laid so far: each a quarter stride on from the one before. */
        int laid;

        Flash(double until, Vec3 last) {
            this.until = until;
            this.last = last;
        }
    }

    private static final Map<Integer, Flash> FLASHES = new HashMap<>();
    /** How far on in the run each afterimage is from the one before (radians of its stride: a quarter). */
    static final double STRIDE = Math.PI / 2.0;
    /** A stretch longer than this in one tick is a teleport, not the flash (blocks). */
    private static final double JUMP = 12.0;

    private BladeFlashClient() {
    }

    /** The server said player {@code shooter} flashes for {@code ticks} ticks from {@code at}. */
    public static void start(int shooter, int ticks, Vec3 at) {
        if (FLASHES.size() > 32) {
            FLASHES.clear();
        }
        FLASHES.put(shooter, new Flash(ControlClient.worldTime(0.0F) + Math.max(1, ticks) + SteelFlash.SWEEP_AFTER, at));
    }

    /** True while player {@code id}'s flash throws him (its ticks, not the stretch read after): his body is in its run. */
    public static boolean flashing(int id, double time) {
        Flash flash = FLASHES.isEmpty() ? null : FLASHES.get(id);
        return flash != null && time <= flash.until - SteelFlash.SWEEP_AFTER + 1.0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || FLASHES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            FLASHES.clear();
            return;
        }
        double now = ControlClient.worldTime(0.0F);
        for (Iterator<Map.Entry<Integer, Flash>> it = FLASHES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Flash> entry = it.next();
            Flash flash = entry.getValue();
            if (now > flash.until || !(minecraft.f_91073_.m_6815_(entry.getKey()) instanceof AbstractClientPlayer player) || !player.m_6084_()) {
                it.remove();
                continue;
            }
            Vec3 feet = player.m_20182_();
            double stretch = feet.m_82554_(flash.last);
            if (stretch < JUMP) {
                lay(player, flash, feet, stretch, now);
            }
            flash.last = feet;
        }
    }

    /**
     * His afterimages along the stretch from where {@code flash} last saw him to {@code to} ({@code stretch} blocks), the farther back the older:
     * each frozen in the blade's own run, flat out ({@link BladeArmPose#run}), a quarter stride on from the one before, so the trail runs
     * through a whole sprint (legs apart, passing, apart) rather than his body's pose of the moment (still turning from his stance to it).
     */
    private static void lay(AbstractClientPlayer player, Flash flash, Vec3 to, double stretch, double now) {
        int count = SteelFlash.ghosts(stretch);
        if (count <= 0) {
            return;
        }
        Vec3 from = flash.last;
        double formed = UkakuClient.blade(player, now);
        double[] blade = formed > 0.05 ? new double[] {formed, KaguneBodyAnimation.bladeReach(player), "slim".equals(player.m_108564_()) ? 1.0 : 0.0}
            : null;
        for (int k = 1; k <= count; k++) {
            double t = k / (double)count;
            Vec3 at = from.m_82549_(to.m_82546_(from).m_82490_(t));
            // Every other one carries the blade (it is heavy to draw); the newest always does.
            RigPose stride = BladeArmPose.run(flash.laid++ * STRIDE, 1.0F);
            stride.settle(1.0F);
            KingAfterimages.add(player, stride, at, player.f_20883_, now - (1.0 - t), false, false, k == count || k % 2 == 0 ? blade : null);
        }
        WarPathFx.streak(from.m_82520_(0.0, 1.0, 0.0), to.m_82520_(0.0, 1.0, 0.0), false, (float)Math.min(6.0, stretch));
    }
}
