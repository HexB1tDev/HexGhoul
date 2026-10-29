package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.UrgeType;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.TendrilMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Local-player only, and only ever visual or the player's own input: first-person camera feedback (render angles,
 * never the real rotation), the instinct view pull (a resistible turn toward the prey) and a slight movement drag away
 * from it. The server never trusts any of this; it decides urges and resolves all damage itself.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ControlCamera {
    private static long lastPull;

    private ControlCamera() {
    }

    private static float wave(double time, double frequency, double phase) {
        return (float)(Math.sin(time * frequency + phase) * 0.6 + Math.sin(time * frequency * 1.73 + phase * 2.1) * 0.4);
    }

    static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        return wrapped >= 180.0 ? wrapped - 360.0 : wrapped < -180.0 ? wrapped + 360.0 : wrapped;
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || !ControlConfig.cameraEffects() || !minecraft.f_91066_.m_92176_().m_90612_()) {
            return;
        }
        KaguneClientState.View view = KaguneClientState.self();
        if (!view.active()) {
            return;
        }
        double distortion = minecraft.f_91066_.m_231924_().m_231551_();
        float scale = ControlConfig.cameraEffectStrength() * (float)Math.max(0.0, Math.min(1.0, distortion));
        if (scale <= 0.0F) {
            return;
        }
        ControlView control = ControlClient.self();
        double time = ControlClient.worldTime((float)event.getPartialTick());
        float low = 1.0F - control.control();
        float roll = 0.6F * control.struggle() * control.frenzy() * wave(time, 0.9, 0.3);
        float pitch = 0.4F * control.struggle() * control.frenzy() * wave(time, 1.1, 2.0);
        double age = time - view.modeStarted();
        float progress = view.modeDuration() <= 0 ? 1.0F : (float)Math.max(0.0, Math.min(1.0, age / view.modeDuration()));
        if (view.mode() == TendrilMode.MANIFEST) {
            float decay = 1.0F - progress;
            float jolt = age >= 0.0 && age < Math.max(1, view.transitionToLimbCount()) * 7.0 ? (float)Math.exp(-(age % 7.0) * 0.7) : 0.0F;
            roll += low * decay * (2.2F * wave(time, 2.4, 0.0) + 2.5F * jolt);
            pitch += low * (1.2F * decay * wave(time, 2.9, 1.0) + 2.0F * jolt);
        } else if (view.mode() == TendrilMode.SWIPE && control.describesSwipe(view.modeStarted())) {
            float impact = control.stage().impactFraction();
            float weight = control.stage().weight();
            float side = strikeSide(view, control);
            if (progress < impact) {
                roll += side * (1.0F + low) * smooth(progress / impact) * (0.8F + 1.2F * weight);
            } else {
                float after = (progress - impact) * Math.max(1, view.modeDuration());
                float kick = (float)Math.exp(-after * (0.9F - 0.4F * low));
                pitch += (0.6F + 1.0F * weight) * (1.0F + 0.5F * low) * kick;
                roll += side * (1.0F + low) * (0.8F + 1.2F * weight) * kick * 0.6F;
            }
        }
        float urgeAge = control.urgeAge(time);
        if (control.urge() != UrgeType.NONE && urgeAge >= 0.0F) {
            float twitch = (float)Math.exp(-urgeAge * 0.5);
            float shudder = control.urgeRestrained() ? 1.2F * wave(time, 3.5, 0.7) : 0.0F;
            roll += 1.5F * twitch * wave(time, 4.0, 0.0) + shudder;
            pitch += 0.8F * twitch + shudder * 0.4F;
        }
        event.setRoll(event.getRoll() + roll * scale);
        event.setPitch(event.getPitch() + pitch * scale);
    }

    /** Pulls the real view toward the prey during an urge, frame-rate independent; mouse input still wins if fought. */
    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        long frame = System.nanoTime();
        double dt = lastPull == 0L ? 0.0 : Math.min(0.1, (frame - lastPull) / 1.0E9);
        lastPull = frame;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || dt <= 0.0 || minecraft.f_91080_ != null || minecraft.m_91104_() || !ControlConfig.urgeViewPull()
            || !player.m_6084_() || player.m_5833_()) {
            return;
        }
        ControlView control = ControlClient.self();
        float age = control.urgeAge(ControlClient.worldTime(event.renderTickTime));
        Entity target = activeTarget(minecraft, control, age);
        if (target == null) {
            return;
        }
        Vec3 eye = player.m_20299_(event.renderTickTime);
        Vec3 aim = target.m_20182_().m_82520_(0.0, target.m_20206_() * 0.6, 0.0);
        double dx = aim.f_82479_ - eye.f_82479_;
        double dy = aim.f_82480_ - eye.f_82480_;
        double dz = aim.f_82481_ - eye.f_82481_;
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double yawDelta = wrapDegrees(yaw - player.m_146908_());
        double pitchDelta = pitch - player.m_146909_();
        double distance = Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
        if (distance < 4.0) {
            return;
        }
        float length = Math.max(1, control.urgeDuration());
        double ease = Math.sin(Math.PI * Math.max(0.0, Math.min(1.0, age / length)));
        double step = Math.min(distance - 4.0, (60.0 + 120.0 * control.urgeStrength()) * ease * dt);
        if (step > 0.0) {
            player.m_19884_(yawDelta / distance * step / 0.15, pitchDelta / distance * step / 0.15);
        }
    }

    /** Damp movement away from the prey and lean slightly into it; crouching (restraint) cancels the drag. */
    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || event.getEntity() != player || player.m_20159_() || player.m_21255_()) {
            return;
        }
        Input input = event.getInput();
        ControlView control = ControlClient.self();
        float age = control.urgeAge(ControlClient.worldTime(minecraft.m_91296_()));
        Entity target = activeTarget(minecraft, control, age);
        if (target == null || input.f_108573_) {
            return;
        }
        float strength = control.urgeStrength() * (control.urge() == UrgeType.GLANCE ? 0.35F : 1.0F);
        double yaw = Math.toRadians(player.m_146908_());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        // Input in world space: forward = (-sin, cos), left = (cos, sin).
        double wx = -sin * input.f_108567_ + cos * input.f_108566_;
        double wz = cos * input.f_108567_ + sin * input.f_108566_;
        Vec3 from = player.m_20182_();
        Vec3 to = target.m_20182_();
        double tx = to.f_82479_ - from.f_82479_;
        double tz = to.f_82481_ - from.f_82481_;
        double length = Math.sqrt(tx * tx + tz * tz);
        if (length < 1.0E-3) {
            return;
        }
        tx /= length;
        tz /= length;
        double along = wx * tx + wz * tz;
        if (along < 0.0) {
            wx -= tx * along * 0.45 * strength;
            wz -= tz * along * 0.45 * strength;
        }
        wx += tx * 0.25 * strength;
        wz += tz * 0.25 * strength;
        input.f_108567_ = (float)Math.max(-1.0, Math.min(1.0, -sin * wx + cos * wz));
        input.f_108566_ = (float)Math.max(-1.0, Math.min(1.0, cos * wx + sin * wz));
    }

    private static Entity activeTarget(Minecraft minecraft, ControlView control, float age) {
        if (control.urge() == UrgeType.NONE || control.urgeRestrained() || age < 0.0F || age > control.urgeDuration() || minecraft.f_91073_ == null) {
            return null;
        }
        Entity target = minecraft.f_91073_.m_6815_(control.urgeTargetId());
        return target != null && target.m_6084_() ? target : null;
    }

    private static float strikeSide(KaguneClientState.View view, ControlView control) {
        KaguneForm form = view.form();
        double sum = 0.0;
        for (int limb = 0; limb < form.limbCount(); limb++) {
            if (control.comboUsesLimb(limb)) {
                double fan = form.limb(limb).fanSide();
                sum += Math.abs(fan) < 0.01 ? (limb % 2 == 0 ? 1.0 : -1.0) : -Math.signum(fan);
            }
        }
        return sum >= 0.0 ? 1.0F : -1.0F;
    }

    private static float smooth(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }
}
