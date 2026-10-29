package com.hex.kagune.control.client.render;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.client.ControlView;
import com.hex.kagune.gameplay.KaguneForm;
import net.minecraft.world.phys.Vec3;

/**
 * Per-pattern tendril strike paths for SWIPE limbs that carry server combo data. Gathers the limb, stage and control
 * inputs and hands the curve math to {@link StrikeCurve}.
 */
final class StrikeShapes {
    /**
     * Roughly how many ticks a limb tip trails its goal (verlet/FABRIK tendrils; segmented legs blend more slowly).
     * The goal is sampled this far ahead so the tip itself, not just the goal, lands on the server's impact tick.
     */
    private static final double TENDRIL_LAG_TICKS = 0.7;
    private static final double SEGMENTED_LAG_TICKS = 1.0;
    private static final ThreadLocal<StrikeCurve> CURVES = ThreadLocal.withInitial(StrikeCurve::new);

    private StrikeShapes() {
    }

    static Vec3 path(ControlView control, KaguneClientState.View view, int limbIndex, Vec3 root, Vec3 rest, Vec3 target,
                     Vec3 forward, Vec3 right, float progress, double sideSign) {
        KaguneForm form = view.form();
        KaguneForm.LimbLayout layout = form.limb(limbIndex);
        ComboStage stage = control.stage();
        int duration = view.modeDuration();

        int count = 0;
        int rank = 0;
        for (int limb = 0; limb < form.limbCount(); limb++) {
            if (control.comboUsesLimb(limb)) {
                if (limb < limbIndex) {
                    rank++;
                }
                count++;
            }
        }

        StrikeCurve curve = CURVES.get();
        curve.rootX = root.f_82479_;
        curve.rootY = root.f_82480_;
        curve.rootZ = root.f_82481_;
        curve.restX = rest.f_82479_;
        curve.restY = rest.f_82480_;
        curve.restZ = rest.f_82481_;
        curve.targetX = target.f_82479_;
        curve.targetY = target.f_82480_;
        curve.targetZ = target.f_82481_;
        curve.forwardX = forward.f_82479_;
        curve.forwardZ = forward.f_82481_;
        curve.sideSign = sideSign;
        curve.lane = StrikeCurve.laneOf(layout.fanHeight());
        curve.size = StrikeCurve.sizeScale(view.limbLength(layout));
        // The server resolves damage on a whole tick: aim for exactly that tick, not the nominal fraction.
        curve.stage(stage, duration > 0 ? stage.impactTick(duration) / (double)duration : stage.impactFraction());
        curve.control = control.control();
        curve.involuntary = control.comboInvoluntary();
        curve.deviation = control.comboDeviation();
        curve.strikeSeed = control.comboStarted();
        curve.limbIndex = limbIndex;
        curve.rank = rank;
        curve.count = Math.max(1, count);

        double lead = duration > 0 ? (layout.segmented() ? SEGMENTED_LAG_TICKS : TENDRIL_LAG_TICKS) / duration : 0.0;
        curve.sample(Math.min(1.0, progress + lead));
        return new Vec3(curve.x, curve.y, curve.z);
    }
}
