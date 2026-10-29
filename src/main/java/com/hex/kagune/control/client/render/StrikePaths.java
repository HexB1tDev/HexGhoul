package com.hex.kagune.control.client.render;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.render.TendrilChain;
import com.hex.kagune.control.BodyClearance;
import com.hex.kagune.control.PathSmoother;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.ControlView;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.TendrilMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Bytecode redirect targets inside {@code TendrilChain}. Only SWIPE strikes that the server tagged with
 * combo data are changed; every other mode (and any swipe without combo data, such as Threat Auto Attack)
 * falls straight through to the original logic.
 */
public final class StrikePaths {
    private StrikePaths() {
    }

    /** Replaces the five {@code this.isActionLimb(view)} calls: lets several limbs strike together. */
    public static boolean isActionLimb(TendrilChain chain, KaguneClientState.View view, AbstractClientPlayer player) {
        if (view.mode() == TendrilMode.SWIPE) {
            ControlView control = ControlClient.get(player);
            if (control.describesSwipe(view.modeStarted())) {
                int limb = chain.kaguneLimbIndex();
                return limb < Math.max(1, view.manifestedLimbCount()) && control.comboUsesLimb(limb);
            }
        }
        return chain.isActionLimb(view);
    }

    /** Replaces both {@code thrustTarget(...)} calls in {@code TendrilChain.actionTarget} (SWIPE and RAPID_STRIKE). */
    public static Vec3 strikeTarget(Vec3 root, Vec3 rest, Vec3 target, Vec3 forward, Vec3 right, float progress, double sideSign,
                                    AbstractClientPlayer player, KaguneClientState.View view, TendrilChain chain) {
        if (view.mode() == TendrilMode.SWIPE) {
            ControlView control = ControlClient.get(player);
            int limb = chain.kaguneLimbIndex();
            if (control.describesSwipe(view.modeStarted()) && control.comboUsesLimb(limb)) {
                return StrikeShapes.path(control, view, limb, root, rest, target, forward, right, progress, sideSign);
            }
        }
        return TendrilChain.thrustTarget(root, rest, target, forward, right, progress, sideSign);
    }

    /**
     * 3.0.0: runs on every drawn limb path ({@code TendrilChain.renderPath}, right after it is resampled) and keeps it out of
     * its owner's body ({@link BodyClearance}): an M1 at something in front of him no longer draws the tentacle through his
     * torso. The original's own routing for held and grappling limbs still runs after this. 3.1.0: Kaneki's tentacles are also
     * smoothed here first ({@link PathSmoother}): quick in a strike, calm at rest.
     */
    /** Ticks to settle most of the way: calm at rest, quick enough in a strike that a hit still lands with the tip. */
    static final double IDLE_RESPONSE = 5.0;
    static final double ACTION_RESPONSE = 0.6;
    /** At rest only this share of the original's sway is kept round the calm shape (PathSmoother). */
    static final double IDLE_WIGGLE = 0.25;
    private static final java.util.Map<TendrilChain, PathSmoother> SMOOTHERS = new java.util.WeakHashMap<>();

    /** {@code path} (world) smoothed in the player's own frame about its root, so his walking and turning carry it unsmoothed. */
    static double[][] smooth(TendrilChain chain, double[][] path, double yaw, double response, double wiggle, double time) {
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double[] root = path[0];
        double[][] local = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            double dx = path[i][0] - root[0];
            double dz = path[i][2] - root[2];
            local[i] = new double[] {dx * rightX + dz * rightZ, path[i][1] - root[1], dx * forwardX + dz * forwardZ};
        }
        double[][] eased = SMOOTHERS.computeIfAbsent(chain, ignored -> new PathSmoother()).follow(local, time, response, wiggle);
        double[][] out = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            out[i] = new double[] {root[0] + rightX * eased[i][0] + forwardX * eased[i][2], root[1] + eased[i][1],
                root[2] + rightZ * eased[i][0] + forwardZ * eased[i][2]};
        }
        return out;
    }

    /**
     * 1.0.24: a limb's drawn path (world, root first) carried with {@code player}'s torso as the body layer drew it this frame
     * ({@link com.hex.kagune.control.client.animation.FurutaPose#ride}: the root in his back, the carry fading out along the limb);
     * false, untouched, when nothing poses him (the model not drawn lately: first person).
     */
    static boolean ride(double[][] path, AbstractClientPlayer player, double[] feet, double yaw, float partialTick) {
        com.hex.kagune.control.client.animation.RigPose pose = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(player,
            ControlClient.worldTime(partialTick));
        if (pose == null) {
            return false;
        }
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double[][] rig = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            double dx = path[i][0] - feet[0];
            double dz = path[i][2] - feet[2];
            rig[i] = new double[] {dx * rx + dz * rz, path[i][1] - feet[1], dx * fx + dz * fz};
        }
        double[][] carried = com.hex.kagune.control.client.animation.FurutaPose.ride(pose, rig);
        if (carried == rig) {
            return false;
        }
        for (int i = 0; i < path.length; i++) {
            double[] q = carried[i];
            path[i] = new double[] {feet[0] + q[0] * rx + q[2] * fx, feet[1] + q[1], feet[2] + q[0] * rz + q[2] * fz};
        }
        return true;
    }

    public static Vec3[] clearOwner(Vec3[] points, TendrilChain chain, AbstractClientPlayer player, KaguneClientState.View view, float partialTick) {
        if (points == null || points.length < 3 || player == null || view == null) {
            return points;
        }
        int limb = chain.kaguneLimbIndex();
        KaguneForm.LimbLayout layout = view.form().limb(limb);
        double[] feet = {player.f_19790_ + (player.m_20185_() - player.f_19790_) * partialTick,
            player.f_19791_ + (player.m_20186_() - player.f_19791_) * partialTick,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partialTick};
        float turn = player.f_20883_ - player.f_20884_;
        turn -= 360.0F * Math.round(turn / 360.0F);
        double yaw = Math.toRadians(player.f_20884_ + turn * partialTick);
        double side = Math.abs(layout.fanSide()) < 0.01 ? (limb % 2 == 0 ? -1.0 : 1.0) : Math.signum(layout.fanSide());
        float thickness = view.limbThickness(layout);
        // Centipede limbs are drawn fatter than their thickness says (KaguneWorldRenderer).
        double radius = layout.segmented() ? Math.max(0.18, thickness * 0.81) : thickness;
        double[][] path = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            path[i] = new double[] {points[i].f_82479_, points[i].f_82480_, points[i].f_82481_};
        }
        // 3.1.0: the tentacles' own motion smoothed (PathSmoother), before the clearance so that still has the last word.
        boolean smoothed = !layout.segmented();
        if (smoothed) {
            boolean idle = view.mode() == TendrilMode.IDLE;
            path = smooth(chain, path, yaw, idle ? IDLE_RESPONSE : ACTION_RESPONSE, idle ? IDLE_WIGGLE : 1.0,
                com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
        }
        // 5.0.14: a tentacle guarding him against incoming shots (Auto Projectile) swings into its guard.
        boolean guarding = false;
        if (!layout.segmented()) {
            double[][] guarded = com.hex.kagune.control.client.TendrilShieldClient.apply(path, limb, player, feet, yaw,
                com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
            if (guarded != null) {
                path = guarded;
                guarding = true;
            }
        }
        boolean moved = BodyClearance.clear(path, feet, -Math.sin(yaw), Math.cos(yaw), player.m_20205_(), player.m_20206_(), radius, side);
        // 1.0.24: the roots ride his body as it is drawn (a lean, a lunge, a drop, a flip, a roll), as Furuta's do: before, a body
        // animation left them hanging where his hips would be standing still.
        boolean ridden = ride(path, player, feet, yaw, partialTick);
        // 5.0.14: kept, so spikes stuck in this tentacle ride it (TendrilPaths, AyatoSpikes.place).
        TendrilPaths.record(player.m_36316_().getId(), limb, path, com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
        if (!moved && !smoothed && !guarding && !ridden) {
            return points;
        }
        Vec3[] drawn = new Vec3[points.length];
        for (int i = 0; i < points.length; i++) {
            drawn[i] = new Vec3(path[i][0], path[i][1], path[i][2]);
        }
        return drawn;
    }
}
