package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.KakujaRig;
import com.hex.kagune.gameplay.TendrilMode;

/**
 * Maps the server-timed kakuja move a player is showing to what each tentacle does ({@link KakujaRig.Action}): how far it
 * is turned to fire (its spike rows on the target) and the kick of each needle leaving it. Purely visual. The final kakuja's
 * fire tendrils (the left three) can aim on their own: they whip, sweep and beat where the needles don't.
 */
final class KakujaMotions {
    /** While a volley chain is live the tentacles stay partly turned, ready for the next one. */
    static final float READY = 0.35F;

    private KakujaMotions() {
    }

    static KakujaRig.Action action(KaguneClientState.View view, ControlView control, double time, double[] aim, UkakuClient.UkakuView ukaku) {
        float gun = UkakuRules.gatlingAim(time, ukaku.gatling(), ukaku.gatlingChangedAt());
        if (gun > 0.0F) {
            return gatling(ukaku, time, aim, gun);
        }
        KakujaRig.Action cyclone = cycloneThrow(ukaku, time, aim);
        if (cyclone != null) {
            return cyclone;
        }
        float[] amount = new float[KakujaRig.TENTACLES];
        float[] kick = new float[KakujaRig.TENTACLES];
        boolean live = view.mode() == TendrilMode.SWIPE && control.describesSwipe(view.modeStarted());
        boolean volley = live && control.comboStyle() == ComboStyle.UKAKU_KAKUJA && control.stage().pattern() == StrikePattern.NEEDLE_VOLLEY;
        if (live && control.comboStyle() == ComboStyle.UKAKU_KAKUJA_ENDERS) {
            return ender(control, time, aim);
        }
        if (live && control.comboStyle() == ComboStyle.UKAKU_FINAL_ENDERS) {
            return finalEnder(control, time, aim);
        }
        if (live && control.comboStyle() == ComboStyle.UKAKU_FINAL_KAKUJA) {
            return finalShot(control, time, aim);
        }
        if (live && control.comboStyle() == ComboStyle.UKAKU_RUSHING_DEATH) {
            return rushingDeath(control, time, aim);
        }
        if (volley) {
            ComboStage stage = control.stage();
            int duration = Math.max(1, control.comboDuration());
            double age = time - control.comboStarted();
            float progress = (float)Math.max(0.0, Math.min(1.0, age / duration));
            int impactTick = stage.impactTick(duration);
            float impact = impactTick / (float)duration;
            // On the chain's first volley everything starts from rest; after that from the ready stance.
            float from = control.comboStage() > 0 ? READY : 0.0F;
            float aimed = UkakuMotions.aimCurve(progress, impact, from, READY);
            float waiting = from + (READY - from) * UkakuMotions.smooth(progress / 0.4F);
            float shot = 0.0F;
            for (int k = 0; k < UkakuRules.VOLLEY_NEEDLES; k++) {
                double since = age - (impactTick + k * UkakuRules.NEEDLE_STAGGER);
                if (since >= 0.0) {
                    shot = Math.max(shot, (float)Math.exp(-since * 0.6));
                }
            }
            for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                boolean firing = control.comboUsesLimb(20 + i);
                amount[i] = firing ? aimed : waiting;
                kick[i] = firing ? shot : 0.0F;
            }
        } else {
            float ready = ready(control, time);
            for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                amount[i] = ready;
            }
        }
        return new KakujaRig.Action(aim, amount, kick);
    }

    /**
     * One of the final kakuja's M1 shots: the tentacles that fire turn on the target (the rest wait half turned) and each kicks
     * as its needles or fire leave it ({@link UkakuRules#finalFireTicks}); the Fire Lash's tendril sweeps across left to right.
     */
    static KakujaRig.Action finalShot(ControlView control, double time, double[] aim) {
        float[] amount = new float[KakujaRig.TENTACLES];
        float[] kick = new float[KakujaRig.TENTACLES];
        ComboStage stage = control.stage();
        StrikePattern pattern = stage.pattern();
        int duration = Math.max(1, control.comboDuration());
        double age = time - control.comboStarted();
        float progress = (float)Math.max(0.0, Math.min(1.0, age / duration));
        int impactTick = stage.impactTick(duration);
        float impact = impactTick / (float)duration;
        float from = control.comboStage() > 0 ? READY : 0.0F;
        float aimed = UkakuMotions.aimCurve(progress, impact, from, READY);
        float waiting = from + (READY - from) * UkakuMotions.smooth(progress / 0.4F);
        int mask = 0;
        for (int i = 0; i < KakujaRig.TENTACLES; i++) {
            mask |= control.comboUsesLimb(20 + i) ? 1 << (20 + i) : 0;
        }
        for (int i = 0; i < KakujaRig.TENTACLES; i++) {
            int[] ticks = UkakuRules.finalFireTicks(pattern, i, mask);
            float shot = 0.0F;
            for (int tick : ticks) {
                double since = age - (impactTick + tick);
                if (since >= 0.0) {
                    shot = Math.max(shot, (float)Math.exp(-since * 0.6));
                }
            }
            amount[i] = ticks.length > 0 ? aimed : waiting;
            kick[i] = pattern == StrikePattern.FIREBALL ? shot : 0.85F * shot;
        }
        double[] left = null;
        if (pattern == StrikePattern.FIRE_LASH) {
            double sweep = Math.max(0.0, Math.min(1.0, (age - impactTick) / Math.max(1.0, UkakuRules.LASH_SHARDS - 1.0)));
            left = turned(aim, -UkakuRules.LASH_HALF_ANGLE + 2.0 * UkakuRules.LASH_HALF_ANGLE * sweep);
        }
        return new KakujaRig.Action(aim, amount, kick, 0.0F, left);
    }

    /** The tentacles through one of the final kakuja's enders. */
    static KakujaRig.Action finalEnder(ControlView control, double time, double[] aim) {
        float[] amount = new float[KakujaRig.TENTACLES];
        float[] kick = new float[KakujaRig.TENTACLES];
        ComboStage stage = control.stage();
        StrikePattern pattern = stage.pattern();
        int duration = Math.max(1, control.comboDuration());
        double age = time - control.comboStarted();
        float progress = (float)Math.max(0.0, Math.min(1.0, age / duration));
        int impactTick = stage.impactTick(duration);
        float impact = impactTick / (float)duration;
        double sinceImpact = age - impactTick;
        float hit = sinceImpact < 0.0 ? 0.0F : (float)Math.exp(-sinceImpact * 0.5);
        boolean afterShots = control.comboStage() > 0;
        float raise = UkakuMotions.aimCurve(progress, impact, afterShots ? READY : 0.0F, 0.0F);
        double[] look = aim;
        double[] left = null;
        switch (pattern) {
            case EMBER_HOP -> {
                // The fire tendrils beat down on the ground in front of him and throw him back; the needles stay out of it.
                left = normalize(new double[] {aim[0] * 0.2, -0.92, 0.38});
                for (int i = 3; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = raise;
                    kick[i] = hit;
                }
            }
            case TWIN_SERPENTS -> {
                // The needles sweep from his right across to his left while the fire sweeps the other way.
                double sweep = Math.max(0.0, Math.min(1.0, sinceImpact / Math.max(1, UkakuRules.SERPENT_SWEEP_TICKS)));
                look = turned(aim, UkakuRules.SERPENT_NEEDLE_HALF_ANGLE - 2.0 * UkakuRules.SERPENT_NEEDLE_HALF_ANGLE * sweep);
                left = turned(aim, -UkakuRules.SERPENT_FIRE_HALF_ANGLE + 2.0 * UkakuRules.SERPENT_FIRE_HALF_ANGLE * sweep);
                boolean sweeping = sinceImpact >= 0.0 && sinceImpact <= UkakuRules.SERPENT_SWEEP_TICKS + 2;
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = Math.max(raise, sweeping ? 0.9F : 0.0F);
                    kick[i] = sweeping ? 0.7F : 0.0F;
                }
            }
            case HELLRAIN -> {
                // Flung high: needles and fireballs both thrown up over him.
                look = new double[] {aim[0] * 0.5, 0.85, Math.max(0.2, aim[2] * 0.5)};
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = raise;
                    kick[i] = hit;
                }
            }
            case CRIMSON_PYRE -> {
                // Half turned, then every spike bristles and the tendrils flare as everything stuck bursts into flame.
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = 0.5F * raise;
                    kick[i] = Math.min(1.0F, 1.5F * hit);
                }
            }
            default -> {
                // Searing Harrow and the Pyre Lance: all six on the target, all kicking at once.
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = raise;
                    kick[i] = hit;
                }
            }
        }
        if (afterShots) {
            float carry = READY * (1.0F - UkakuMotions.smooth(progress / 0.25F));
            for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                amount[i] = Math.max(amount[i], carry);
            }
        }
        return new KakujaRig.Action(look, amount, kick, 0.0F, left);
    }

    /**
     * The final kakuja throwing its fire cyclone (Z): the fire tendrils coil up and back over his left shoulder through the
     * wind-up, then whip forward onto the target as it leaves them; null when no throw is under way.
     */
    static KakujaRig.Action cycloneThrow(UkakuClient.UkakuView ukaku, double time, double[] aim) {
        if (!ukaku.form().finalKakuja() || ukaku.vortexAt() == Long.MIN_VALUE || ukaku.vortexSide() >= 0) {
            return null;
        }
        double age = time - (ukaku.vortexAt() - UkakuRules.VORTEX_WINDUP);
        double total = UkakuClient.VORTEX_POSE_TICKS;
        if (age < 0.0 || age > total) {
            return null;
        }
        float progress = (float)(age / total);
        float release = UkakuRules.VORTEX_WINDUP / (float)total;
        float coil = UkakuMotions.smooth(progress / release) * (1.0F - UkakuMotions.smooth((progress - release) / 0.08F));
        float snap = UkakuMotions.smooth((progress - release + 0.05F) / 0.06F) * (1.0F - UkakuMotions.smooth((progress - 0.75F) / 0.25F));
        double since = age - UkakuRules.VORTEX_WINDUP;
        float kick = since < 0.0 ? 0.0F : (float)Math.exp(-since * 0.45);
        double[] back = {-0.5, 0.75, -0.42};
        double[] left = normalize(new double[] {back[0] + (aim[0] - back[0]) * snap, back[1] + (aim[1] - back[1]) * snap,
            back[2] + (aim[2] - back[2]) * snap});
        float[] amount = new float[KakujaRig.TENTACLES];
        float[] kicks = new float[KakujaRig.TENTACLES];
        for (int i = 3; i < KakujaRig.TENTACLES; i++) {
            amount[i] = Math.max(0.6F * coil, snap);
            kicks[i] = kick;
        }
        return new KakujaRig.Action(aim, amount, kicks, 0.0F, left);
    }

    /** {@code aim} (rig space) turned {@code degrees} about the vertical, positive to his right. */
    static double[] turned(double[] aim, double degrees) {
        double yaw = Math.toRadians(degrees);
        return new double[] {aim[0] * Math.cos(yaw) + aim[2] * Math.sin(yaw), aim[1], -aim[0] * Math.sin(yaw) + aim[2] * Math.cos(yaw)};
    }

    static double[] normalize(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return length < 1.0E-9 ? new double[] {0.0, 0.0, 1.0} : new double[] {v[0] / length, v[1] / length, v[2] / length};
    }

    /** The tentacles through one of the kakuja's enders. */
    static KakujaRig.Action ender(ControlView control, double time, double[] aim) {
        float[] amount = new float[KakujaRig.TENTACLES];
        float[] kick = new float[KakujaRig.TENTACLES];
        ComboStage stage = control.stage();
        StrikePattern pattern = stage.pattern();
        int duration = Math.max(1, control.comboDuration());
        double age = time - control.comboStarted();
        float progress = (float)Math.max(0.0, Math.min(1.0, age / duration));
        int impactTick = stage.impactTick(duration);
        float impact = impactTick / (float)duration;
        double sinceImpact = age - impactTick;
        float hit = sinceImpact < 0.0 ? 0.0F : (float)Math.exp(-sinceImpact * 0.5);
        // Enders are indexed by the volleys before them: past Harrow, the chain's ready stance is still up.
        boolean afterShots = control.comboStage() > 0;
        float raise = UkakuMotions.aimCurve(progress, impact, afterShots ? READY : 0.0F, 0.0F);
        double[] look = aim;
        switch (pattern) {
            case THORN_FAN -> {
                // The aim sweeps from his left across to his right as the fan leaves; each side's tentacles kick in their half.
                float half = UkakuRules.THORN_FAN_HALF_ANGLE;
                double sweep = Math.max(0.0, Math.min(1.0, sinceImpact / Math.max(1, UkakuRules.FAN_SWEEP_TICKS)));
                double yaw = Math.toRadians(-half + 2.0 * half * sweep);
                look = new double[] {aim[0] * Math.cos(yaw) + aim[2] * Math.sin(yaw), aim[1], -aim[0] * Math.sin(yaw) + aim[2] * Math.cos(yaw)};
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = raise;
                    boolean right = KakujaRig.sideOf(i) > 0;
                    kick[i] = sinceImpact < 0.0 || sinceImpact > UkakuRules.FAN_SWEEP_TICKS + 3 ? 0.0F : right == yaw >= 0.0 ? 0.8F : 0.15F;
                }
            }
            case HAIL_OF_THORNS -> {
                // Flung high: the rows turned up over him.
                look = new double[] {aim[0] * 0.5, 0.85, Math.max(0.2, aim[2] * 0.5)};
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = raise;
                    kick[i] = hit;
                }
            }
            case SIX_TAIL_BARRAGE -> {
                // All six held on the target from the impact, two needles a tick round the tentacles.
                float hang = UkakuMotions.smooth((progress - impact + 0.08F) / 0.08F) * (1.0F - UkakuMotions.smooth((progress - 0.82F) / 0.1F));
                double shots = Math.max(0.0, sinceImpact) * UkakuRules.SIX_TAIL_PER_TICK;
                int last = (int)Math.floor(shots);
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = Math.max(hang, raise);
                }
                if (sinceImpact >= 0.0 && hang > 0.0F) {
                    for (int back = 0; back < 4; back++) {
                        int shot = last - back;
                        if (shot < 0) {
                            break;
                        }
                        double ago = (shots - shot) / UkakuRules.SIX_TAIL_PER_TICK;
                        int index = UkakuRules.sixTailTentacle(shot);
                        kick[index] = Math.max(kick[index], hang * (float)Math.exp(-ago * 1.4));
                    }
                }
            }
            case CRIMSON_BLOOM -> {
                // Half turned, then every spike on them bristles as everything stuck bursts.
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = 0.5F * raise;
                    kick[i] = Math.min(1.0F, 1.5F * hit);
                }
            }
            default -> {
                // Harrow and the Piercing Line: all six on the target, all kicking at once.
                for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                    amount[i] = raise;
                    kick[i] = hit;
                }
            }
        }
        if (afterShots) {
            // They ease out of the chain's ready stance, never snap.
            float carry = READY * (1.0F - UkakuMotions.smooth(progress / 0.25F));
            for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                amount[i] = Math.max(amount[i], carry);
            }
        }
        return new KakujaRig.Action(look, amount, kick);
    }

    /**
     * The Needle Gatling: all six turned on the target ({@code gun} 0..1 as it spins up and down), salvos kicking one tentacle
     * at a time round the six in the order the server fires them (the final kakuja's Pyre Gatling: a needle tentacle, then a
     * fire tendril, in turn).
     */
    static KakujaRig.Action gatling(UkakuClient.UkakuView ukaku, double time, double[] aim, float gun) {
        float[] amount = new float[KakujaRig.TENTACLES];
        float[] kick = new float[KakujaRig.TENTACLES];
        for (int i = 0; i < KakujaRig.TENTACLES; i++) {
            amount[i] = gun;
        }
        if (ukaku.gatling() && ukaku.gatlingChangedAt() != Long.MIN_VALUE) {
            double held = time - ukaku.gatlingChangedAt();
            double rate = UkakuRules.needleGatlingRate((long)held);
            if (rate > 0.0) {
                double salvos = (held - UkakuRules.GATLING_SPINUP) * rate;
                int last = (int)Math.floor(salvos);
                for (int back = 0; back < 4; back++) {
                    int salvo = last - back;
                    if (salvo < 0) {
                        break;
                    }
                    double ago = (salvos - salvo) / rate;
                    int index = ukaku.form().finalKakuja() ? UkakuRules.pyreGatlingTentacle(salvo) : UkakuRules.sixTailTentacle(salvo);
                    kick[index] = Math.max(kick[index], gun * (float)Math.exp(-ago * 0.9));
                }
            }
        }
        return new KakujaRig.Action(aim, amount, kick);
    }

    /**
     * Rushing Death: pulled in tight through the backflip, then at the top all six snap forward onto the target and throw,
     * then ease back as he falls.
     */
    static KakujaRig.Action rushingDeath(ControlView control, double time, double[] aim) {
        float[] amount = new float[KakujaRig.TENTACLES];
        float[] kick = new float[KakujaRig.TENTACLES];
        int duration = Math.max(1, control.comboDuration());
        double age = time - control.comboStarted();
        float progress = (float)Math.max(0.0, Math.min(1.0, age / duration));
        int impactTick = control.stage().impactTick(duration);
        float impact = impactTick / (float)duration;
        float tuck = UkakuMotions.smooth((progress - 0.04F) / 0.08F) * (1.0F - UkakuMotions.smooth((progress - (impact - 0.1F)) / 0.06F));
        float snap = UkakuMotions.smooth((progress - (impact - 0.08F)) / 0.07F) * (1.0F - UkakuMotions.smooth((progress - 0.7F) / 0.25F));
        double since = age - impactTick;
        float hit = since < 0.0 ? 0.0F : (float)Math.exp(-since * 0.45);
        for (int i = 0; i < KakujaRig.TENTACLES; i++) {
            amount[i] = snap;
            kick[i] = hit;
        }
        return new KakujaRig.Action(aim, amount, kick, tuck);
    }

    /** The half-turned ready stance after a volley, while the chain can still go on (fading out over its last ticks). */
    static float ready(ControlView control, double time) {
        if (control.comboStyle() != ComboStyle.UKAKU_KAKUJA && control.comboStyle() != ComboStyle.UKAKU_FINAL_KAKUJA || !control.hasCombo()) {
            return 0.0F;
        }
        double since = time - (control.comboStarted() + control.comboDuration());
        if (since < 0.0 || since > UkakuRules.ENDER_WINDOW_TICKS) {
            return 0.0F;
        }
        return READY * (1.0F - UkakuMotions.smooth((float)((since - (UkakuRules.ENDER_WINDOW_TICKS - 8)) / 8.0)));
    }
}
