package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.ukaku.UkakuBody;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.WingRig;
import com.hex.kagune.gameplay.TendrilMode;

/**
 * Maps the server-timed Ukaku combo stage a player is showing to what the wings do: a {@link WingRig.Motion} plus, for the
 * fire, how far each wing is curled over its shoulder aiming (0..1) and the kick of a shot just leaving it. Purely visual.
 */
final class UkakuMotions {
    /** Half-raised: while an M1 chain is live the wings stay partly curled, ready for the next shot. */
    static final float READY = 0.35F;

    /**
     * The motion, its 0..1 progress and impact fraction, which wings crystallize for it, each wing's aim and shot kick, a
     * sideways sweep of the aim (degrees, the Fire Fan) and the body's backflip (radians, the Black Rabbit).
     */
    record Strike(WingRig.Motion motion, float progress, float impact, boolean crystalRight, boolean crystalLeft, float aimRight, float aimLeft,
                  float kickRight, float kickLeft, float aimYaw, float flip) {
        static final Strike NONE = new Strike(WingRig.Motion.NONE, 0.0F, 0.4F, false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private UkakuMotions() {
    }

    static Strike strike(KaguneClientState.View view, ControlView control, double time) {
        if (view.mode() != TendrilMode.SWIPE || !control.describesSwipe(view.modeStarted()) || !control.comboStyle().ukaku()) {
            float ready = ready(control, time);
            return ready <= 0.0F ? Strike.NONE
                : new Strike(WingRig.Motion.NONE, 0.0F, 0.4F, false, false, ready, ready, 0.0F, 0.0F, 0.0F, 0.0F);
        }
        ComboStage stage = control.stage();
        int duration = Math.max(1, control.comboDuration());
        double age = time - control.comboStarted();
        float progress = (float)Math.max(0.0, Math.min(1.0, age / duration));
        float impact = stage.impactTick(duration) / (float)duration;
        boolean right = control.comboUsesLimb(0);
        boolean left = control.comboUsesLimb(1);
        double sinceImpact = age - stage.impactTick(duration);
        float kick = sinceImpact < 0.0 ? 0.0F : (float)Math.exp(-sinceImpact * 0.55);
        StrikePattern pattern = stage.pattern();
        WingRig.Motion motion = switch (pattern) {
            case KICK -> WingRig.Motion.KICK;
            case WING_SLASH -> right ? WingRig.Motion.SLASH_RIGHT : WingRig.Motion.SLASH_LEFT;
            case WING_CROSS -> WingRig.Motion.CROSS;
            case SHARD_BURST -> WingRig.Motion.BURST;
            case ONE_WING_SLASH -> WingRig.Motion.ONE_SLASH;
            case ONE_WING_RETURN -> WingRig.Motion.ONE_RETURN;
            case BEHEADING -> WingRig.Motion.ONE_BEHEAD;
            case RABBIT_DASH -> WingRig.Motion.DASH;
            case RABBIT_HOP -> WingRig.Motion.HOP;
            case BLACK_RABBIT -> WingRig.Motion.FLIP;
            case RISING_FLAME, SPIKE_RAIN -> WingRig.Motion.RISE;
            case CRYSTAL_BLOOM -> WingRig.Motion.BURST;
            default -> WingRig.Motion.NONE;
        };
        float aimRight = 0.0F;
        float aimLeft = 0.0F;
        float kickRight = 0.0F;
        float kickLeft = 0.0F;
        float yaw = 0.0F;
        float flip = 0.0F;
        // Enders are indexed by the shots before them: past the hop, the chain's ready stance is still up.
        boolean afterShots = control.comboStyle().ukakuEnderSet() && control.comboStage() > 0;
        switch (pattern) {
            case FLAME_SHOT, CRYSTAL_SHOT -> {
                // The wing on the hand's side curls on from where it is to aim, fires at the impact, holds, then eases back
                // to ready; the other wing waits half-curled. On the chain's first shot both start from rest.
                float from = control.comboStage() > 0 ? READY : 0.0F;
                float aim = aimCurve(progress, impact, from, READY);
                float waiting = from + (READY - from) * smooth(progress / 0.4F);
                aimRight = right ? aim : waiting;
                aimLeft = right ? waiting : aim;
                kickRight = right ? kick : 0.0F;
                kickLeft = right ? 0.0F : kick;
            }
            case FLARE, CRYSTAL_LANCE -> {
                float aim = aimCurve(progress, impact, afterShots ? READY : 0.0F, 0.0F);
                aimRight = aim;
                aimLeft = aim;
                kickRight = kick;
                kickLeft = kick;
            }
            case FIRE_FAN, SPIKE_FAN -> {
                float aim = aimCurve(progress, impact, afterShots ? READY : 0.0F, 0.0F);
                aimRight = aim;
                aimLeft = aim;
                kickRight = kick;
                kickLeft = kick;
                // The aim sweeps across the front as the fan sprays.
                float half = pattern == StrikePattern.SPIKE_FAN ? UkakuRules.SPIKE_FAN_HALF_ANGLE : UkakuRules.FAN_HALF_ANGLE;
                float sweep = (float)Math.max(0.0, Math.min(1.0, (progress - (impact - 0.12F)) / 0.3F));
                yaw = -half + 2.0F * half * sweep;
            }
            case SPIKE_BARRAGE -> {
                // Both wings curled on the target from the impact, machine-gunning: one spike a tick, alternating.
                float hang = smooth((progress - impact + 0.08F) / 0.08F) * (1.0F - smooth((progress - 0.82F) / 0.1F));
                float raise = aimCurve(progress, impact, afterShots ? READY : 0.0F, 0.0F);
                aimRight = Math.max(hang, raise);
                aimLeft = Math.max(hang, raise);
                double tick = age - Math.floor(age);
                boolean even = ((long)Math.floor(age) & 1L) == 0L;
                float pulse = hang * (float)Math.exp(-tick * 2.0);
                kickRight = even ? pulse : 0.0F;
                kickLeft = even ? 0.0F : pulse;
            }
            case RUSHING_DEATH -> flip = UkakuBody.pose(StrikePattern.RUSHING_DEATH, true, true, progress, impact).flip;
            case BLACK_RABBIT -> {
                flip = UkakuBody.pose(StrikePattern.BLACK_RABBIT, true, true, progress, impact).flip;
                // Hanging in the air, both wings rain fire: one shot a tick, alternating.
                float hang = smooth((progress - 0.36F) / 0.05F) * (1.0F - smooth((progress - 0.74F) / 0.06F));
                aimRight = hang;
                aimLeft = hang;
                double tick = age - Math.floor(age);
                boolean even = ((long)Math.floor(age) & 1L) == 0L;
                float pulse = hang * (float)Math.exp(-tick * 2.0);
                kickRight = even ? pulse : 0.0F;
                kickLeft = even ? 0.0F : pulse;
            }
            default -> {
            }
        }
        // An ender after shots starts with the wings still half-curled from the chain: they ease out of it, never snap.
        if (afterShots) {
            float carry = READY * (1.0F - smooth(progress / 0.25F));
            aimRight = Math.max(aimRight, carry);
            aimLeft = Math.max(aimLeft, carry);
        }
        // Wing blades are crystallized RC, and so are the shards of a point-blank burst; the fire stays fire.
        boolean burst = motion == WingRig.Motion.BURST;
        boolean crystalRight = burst && right || motion == WingRig.Motion.SLASH_RIGHT || motion == WingRig.Motion.CROSS;
        boolean crystalLeft = burst && left || motion == WingRig.Motion.SLASH_LEFT || motion == WingRig.Motion.CROSS;
        return new Strike(motion, progress, impact, crystalRight, crystalLeft, aimRight, aimLeft, kickRight, kickLeft, yaw, flip);
    }

    /** Curl from {@code from} to a full aim before the impact, hold through the shot, then ease back to {@code rest}. */
    static float aimCurve(float progress, float impact, float from, float rest) {
        float raise = smooth(progress / Math.max(0.05F, impact * 0.85F));
        float lower = smooth((progress - 0.68F) / 0.32F);
        float held = from + (1.0F - from) * raise;
        return held + (rest - held) * lower;
    }

    /** The half-raised ready stance after a shot, while the chain can still go on (fading out over its last ticks). */
    static float ready(ControlView control, double time) {
        if (!control.comboStyle().ukakuShots() || !control.hasCombo()) {
            return 0.0F;
        }
        double since = time - (control.comboStarted() + control.comboDuration());
        if (since < 0.0 || since > UkakuRules.ENDER_WINDOW_TICKS) {
            return 0.0F;
        }
        return READY * (1.0F - smooth((float)((since - (UkakuRules.ENDER_WINDOW_TICKS - 8)) / 8.0)));
    }

    static float smooth(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }
}
