package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.1.0: Pale's own view. When she blinks to a body's side the view swings round to it over a few frames (never snapped), through
 * the same turn the mouse makes, so moving the mouse still works; and her blows (and blows that land on her) kick the view.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PaleCamera {
    /** Degrees still to turn, and how fast (degrees a second). */
    private static double turnLeft;
    private static double turnRate;
    private static long lastFrame;
    /** The last kick: how hard, and when (PaleClient's clock). */
    private static float kick;
    private static double kickAt = Double.NEGATIVE_INFINITY;
    /** 1.0.27: the last rumble: how hard, from when, for how long (PaleClient's clock, ticks). */
    private static float rumble;
    private static double rumbleAt = Double.NEGATIVE_INFINITY;
    private static double rumbleFor;

    private PaleCamera() {
    }

    /** Swing the view {@code degrees} (+ to the right) over {@code ticks}. */
    public static void turnBy(double degrees, double ticks) {
        turnLeft = degrees;
        turnRate = Math.abs(degrees) / Math.max(0.05, ticks / 20.0);
    }

    /** A kick to the view, {@code strength} about 1 for a hard blow. */
    public static void kick(float strength, double time) {
        float left = kick * (float)Math.exp(-(time - kickAt) * 0.45);
        kick = Math.max(left, strength);
        kickAt = time;
    }

    /** 1.0.27: the ground shaking under the view, {@code strength} about 1 at the heart of it, fading out over {@code ticks}. */
    public static void rumble(float strength, double ticks, double time) {
        double since = time - rumbleAt;
        float left = since >= 0.0 && since < rumbleFor ? rumble * (float)(1.0 - since / rumbleFor) : 0.0F;
        if (strength >= left) {
            rumble = strength;
            rumbleAt = time;
            rumbleFor = Math.max(1.0, ticks);
        }
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        long frame = System.nanoTime();
        double dt = lastFrame == 0L ? 0.0 : Math.min(0.1, (frame - lastFrame) / 1.0E9);
        lastFrame = frame;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player != null && dt > 0.0) {
            lockOn(player, dt);
            kingLock(player, dt);
        }
        if (player == null || Math.abs(turnLeft) < 1.0E-3 || dt <= 0.0) {
            return;
        }
        double step = Math.signum(turnLeft) * Math.min(Math.abs(turnLeft), turnRate * dt);
        turnLeft -= step;
        player.m_19884_(step / 0.15, 0.0);
    }

    /** 6.2.0: how quickly (a second) the view settles onto the head she kicks, and how long after the blow it is held there. */
    static final double LOCK_RATE = 9.0;
    static final double LOCK_AFTER = 1.5;

    /**
     * 6.2.0: through the Speed Kick until just after the blow, her view eases onto what she kicks (6.2.0: its middle; it is held still
     * for it), so in first person she sees her own leg drive into it; through the same turn the mouse makes, so the mouse still works.
     */
    private static void lockOn(LocalPlayer player, double dt) {
        PaleClient.PaleView view = PaleClient.get(player);
        // (6.2.0: the Owl's kick too, until just after its heel lands.)
        boolean owl = view.move() == com.hex.kagune.control.pale.PaleMove.OWL_KICK;
        // (6.2.0: the Speed Kick's blow comes a lunge and a dash in, and her view is on its middle, where her foot goes.)
        // (6.2.0: Witness This too, all the way down to the crater.)
        boolean witness = view.move() == com.hex.kagune.control.pale.PaleMove.WITNESS;
        double until = owl ? view.detail() + com.hex.kagune.control.pale.OwlRules.KICK_BLOW
            : witness ? com.hex.kagune.control.pale.WitnessRules.IMPACT - LOCK_AFTER
            : com.hex.kagune.control.pale.PaleRules.kickArrive(view.detail()) + com.hex.kagune.control.pale.PaleRules.KICK_CONTACT;
        if (view.move() != com.hex.kagune.control.pale.PaleMove.KICK && !owl && !witness || view.targetId() == 0
            || view.age(PaleClient.clock(0.0F)) > until + LOCK_AFTER) {
            return;
        }
        net.minecraft.world.entity.Entity target = player.m_9236_().m_6815_(view.targetId());
        if (target == null) {
            return;
        }
        net.minecraft.world.phys.Vec3 eye = player.m_146892_();
        net.minecraft.world.phys.Vec3 head = owl ? target.m_146892_()
            : target.m_20182_().m_82520_(0.0, target.m_20206_() * com.hex.kagune.control.pale.PaleRules.KICK_AIM, 0.0);
        double dx = head.f_82479_ - eye.f_82479_;
        double dy = head.f_82480_ - eye.f_82480_;
        double dz = head.f_82481_ - eye.f_82481_;
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double turn = yaw - player.m_146908_();
        turn -= 360.0 * Math.floor((turn + 180.0) / 360.0);
        double k = 1.0 - Math.exp(-LOCK_RATE * dt);
        player.m_19884_(turn * k / 0.15, (pitch - player.m_146909_()) * k / 0.15);
    }

    /**
     * 1.0.27: through War's blitz (King's Execution at a body) and Black Sun, his view eases onto what he is tearing into (its middle)
     * until just after the last blow, wherever the blinks put him; through the same turn the mouse makes, so the mouse still works.
     */
    private static void kingLock(LocalPlayer player, double dt) {
        KingClient.View view = KingClient.get(player);
        if (view == null || view.target() == 0) {
            return;
        }
        double time = ControlClient.worldTime(Minecraft.m_91087_().m_91296_());
        com.hex.kagune.control.king.KingMove move = view.moveAt(time);
        double until = move == com.hex.kagune.control.king.KingMove.BLITZ ? com.hex.kagune.control.king.WarRules.BLITZ_GUT + 3.0
            : move == com.hex.kagune.control.king.KingMove.SUNDER ? com.hex.kagune.control.king.WarRules.SUNDER_PUNCH : -1.0; // 1.0.33: his view his own from the punch on
        if (until < 0.0 || time - view.moveAt() > until) {
            return;
        }
        net.minecraft.world.entity.Entity target = player.m_9236_().m_6815_(view.target());
        if (target == null) {
            return;
        }
        net.minecraft.world.phys.Vec3 eye = player.m_146892_();
        net.minecraft.world.phys.Vec3 middle = target.m_20182_().m_82520_(0.0, target.m_20206_() * 0.5, 0.0);
        double dx = middle.f_82479_ - eye.f_82479_;
        double dy = middle.f_82480_ - eye.f_82480_;
        double dz = middle.f_82481_ - eye.f_82481_;
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double turn = yaw - player.m_146908_();
        turn -= 360.0 * Math.floor((turn + 180.0) / 360.0);
        double k = 1.0 - Math.exp(-LOCK_RATE * 1.4 * dt);
        player.m_19884_(turn * k / 0.15, (pitch - player.m_146909_()) * k / 0.15);
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (kick <= 0.0F && rumble <= 0.0F || !ControlConfig.cameraEffects()) {
            return;
        }
        double time = PaleClient.clock((float)event.getPartialTick());
        float scale = ControlConfig.cameraEffectStrength();
        double since = time - kickAt;
        if (kick > 0.0F && since >= 0.0 && since <= 12.0) {
            float fade = kick * (float)Math.exp(-since * 0.45);
            // A short, sharp jolt: down and to one side, ringing out over half a second.
            float jolt = fade * (float)Math.cos(since * 2.3);
            event.setPitch(event.getPitch() + 1.6F * jolt * scale);
            event.setRoll(event.getRoll() + 1.1F * fade * (float)Math.sin(since * 2.9) * scale);
        }
        double shaking = time - rumbleAt;
        if (rumble > 0.0F && shaking >= 0.0 && shaking < rumbleFor) {
            // 1.0.27: the ground going: an uneven shudder (three sines that never line up), dying away to nothing.
            float fade = rumble * (float)Math.pow(1.0 - shaking / rumbleFor, 1.5);
            double t = shaking;
            float pitch = (float)(Math.sin(t * 4.1) + 0.6 * Math.sin(t * 9.7 + 1.3) + 0.3 * Math.sin(t * 17.3 + 0.4));
            float roll = (float)(Math.sin(t * 3.3 + 2.1) + 0.5 * Math.sin(t * 11.9 + 0.7));
            event.setPitch(event.getPitch() + 1.2F * fade * pitch * scale);
            event.setRoll(event.getRoll() + 0.9F * fade * roll * scale);
        }
    }
}
