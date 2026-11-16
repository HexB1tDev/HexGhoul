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
     * torso. The original's own routing for held and grappling limbs still runs after this. 3.1.0: Rinkaku's tentacles are also
     * smoothed here first ({@link PathSmoother}): quick in a strike, calm at rest.
     */
    /** Ticks to settle most of the way: calm at rest, quick enough in a strike that a hit still lands with the tip. */
    static final double IDLE_RESPONSE = 5.0;
    static final double ACTION_RESPONSE = 0.6;
    /** At rest only this share of the original's sway is kept round the calm shape (PathSmoother). */
    static final double IDLE_WIGGLE = 0.25;
    /** 3.0.5: the tail holding someone: followed calmly, only this share of the original's sway kept. */
    static final double HOLD_RESPONSE = 2.0;
    static final double HOLD_WIGGLE = 0.35;

    /** 3.0.5: the modes in which a tail holds someone (the others at rest meanwhile). */
    public static boolean holding(TendrilMode mode) {
        return mode == TendrilMode.HOLD || mode == TendrilMode.LIFT || mode == TendrilMode.CRUSH;
    }
    private static final java.util.Map<TendrilChain, PathSmoother> SMOOTHERS = new java.util.WeakHashMap<>();
    /** 3.1.5: how far each tail drawn this frame has its end drawn to a point (an Impale); none: not at all. */
    private static final java.util.Map<TendrilChain, Double> SHARP = new java.util.WeakHashMap<>();

    /** 3.1.5: how far {@code chain}'s tail, as last laid out, is drawn to a point (0 .. 1; {@code CentipedeRenderer.emitTube}). */
    public static double sharp(TendrilChain chain) {
        Double sharp = SHARP.get(chain);
        return sharp == null ? 0.0 : sharp;
    }

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
     * ({@link com.hex.kagune.control.client.animation.GraftPose#ride}: the root in his back, the carry fading out along the limb);
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
        double[][] carried = com.hex.kagune.control.client.animation.GraftPose.ride(pose, rig);
        if (carried == rig) {
            return false;
        }
        for (int i = 0; i < path.length; i++) {
            double[] q = carried[i];
            path[i] = new double[] {feet[0] + q[0] * rx + q[2] * fx, feet[1] + q[1], feet[2] + q[0] * rz + q[2] * fz};
        }
        return true;
    }

    /** 2.0.38: {@code path} (world) through {@link com.hex.kagune.control.RinkakuRoot#attach} in the owner's frame ({@code feet}, {@code yaw}). */
    static double[][] attachRoot(double[][] path, int limb, double fanSide, double[] feet, double yaw, double height) {
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
        double[][] grown = com.hex.kagune.control.RinkakuRoot.attach(rig, limb, fanSide, height);
        double[][] out = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            double[] q = grown[i];
            out[i] = new double[] {feet[0] + q[0] * rx + q[2] * fx, feet[1] + q[1], feet[2] + q[0] * rz + q[2] * fz};
        }
        return out;
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
        // 2.3.2: BodyClearance's side (its +1 is his left): the tail carried round its own side of him, never round the other.
        double side = com.hex.kagune.control.RinkakuRoot.clearance(limb, layout.fanSide());
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
            // 3.0.5 (the owner: "While Rinkaku is holding a target ... the tails are constantly shaking"): holding someone, the tails not
            // holding them are at rest (they took the original's twitching, coiled motion at full strength), and the one holding is
            // calmed too (its end is wrapped round them by the original after this).
            boolean holds = holding(view.mode()) && view.heldEntityId(limb) != 0;
            boolean idle = view.mode() == TendrilMode.IDLE || holding(view.mode()) && !holds;
            path = smooth(chain, path, yaw, idle ? IDLE_RESPONSE : holds ? HOLD_RESPONSE : ACTION_RESPONSE,
                idle ? IDLE_WIGGLE : holds ? HOLD_WIGGLE : 1.0, com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
        }
        // 2.0.51: at rest, the shape the tails were drawn with (RinkakuIdle), not the original chain's coiled, twitching rest.
        if (!layout.segmented()) {
            double[][] rested = com.hex.kagune.control.client.RinkakuIdleClient.apply(path, limb, player, view, feet, yaw,
                com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
            if (rested != null) {
                path = rested;
            }
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
        // 2.0.42: Rinkaku's block: the tails swung round in front of him (RinkakuBlock), or thrown back by a broken one.
        if (!layout.segmented()) {
            double[][] blocking = com.hex.kagune.control.client.RinkakuBlockClient.apply(path, limb, player, view, feet, yaw,
                com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
            if (blocking != null) {
                path = blocking;
                guarding = true;
            }
        }
        // 2.4.5: Thy Death: the tail lunging at its own foe, over its own motion.
        if (!layout.segmented()) {
            double[][] lunging = com.hex.kagune.control.client.ThyDeathClient.apply(path, limb, player, layout.fanSide(), yaw,
                com.hex.kagune.control.client.ControlClient.worldTime(partialTick), partialTick);
            if (lunging != null) {
                path = lunging;
                guarding = true;
            }
        }
        // 3.1.5: the Impale: the tail up over its shoulder and straight out along its line, its end drawn to a point (sharp, for the mesh).
        if (!layout.segmented()) {
            double[][] impaling = com.hex.kagune.control.client.ImpaleClient.apply(path, limb, player, layout.fanSide(), feet, yaw,
                com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
            if (impaling != null) {
                path = impaling;
                guarding = true;
                SHARP.put(chain, com.hex.kagune.control.client.ImpaleClient.sharp());
            } else {
                SHARP.remove(chain);
            }
        }
        // 3.1.6: the Rubble Hurl: the tails gripping its chunk, then whipping after it as it is thrown.
        if (!layout.segmented()) {
            double[][] gripping = com.hex.kagune.control.client.HurlClient.apply(path, limb, player, layout.fanSide(), feet, yaw,
                com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
            if (gripping != null) {
                path = gripping;
                guarding = true;
            }
        }
        boolean moved = BodyClearance.clear(path, feet, -Math.sin(yaw), Math.cos(yaw), player.m_20205_(), player.m_20206_(), radius, side);
        // 2.0.38: the tail grown out of his lower back (RinkakuRoot): its root sunk in it, its base leaving it, then its own motion.
        if (!layout.segmented()) {
            path = attachRoot(path, limb, layout.fanSide(), feet, yaw, player.m_20206_());
            moved = true;
        }
        // 1.0.24: the roots ride his body as it is drawn (a lean, a lunge, a drop, a flip, a roll), as Graft's do: before, a body
        // animation left them hanging where his hips would be standing still.
        boolean ridden = ride(path, player, feet, yaw, partialTick);
        // 5.0.14: kept, so spikes stuck in this tentacle ride it (TendrilPaths, UkakuSpikes.place).
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
