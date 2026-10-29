package com.hex.kagune.control;

import java.util.Arrays;

/**
 * One M1 combo step. Timing values are in ticks before mastery/mutation scaling.
 *
 * @param name           readable name used in debug output and the command readout
 * @param pattern        tendril path and body pose family
 * @param shape          authoritative server hit volume
 * @param shapeParam     arc degrees (SWEEP), radius blocks (PIERCE/SLAM) or range fraction (SPIN)
 * @param baseDuration   whole strike length in ticks
 * @param impactFraction fraction of the strike at which damage resolves (and the limb visually connects)
 * @param damageMul      multiplier on {@code KaguneProfile.swipeDamage}
 * @param knockbackMul   multiplier on the original swipe knockback
 * @param rangeMul       multiplier on {@code KaguneProfile.attackRange}
 * @param maxTargets     maximum entities damaged by this stage
 * @param energyMul      multiplier on the original 0.85 RC swipe cost
 * @param limbs          form limb indices that strike; empty means "rotate through limbs like the original swipe"
 * @param weight         0 light and whippy .. 1 heavy and committed; drives body animation weight
 */
public record ComboStage(
    String name,
    StrikePattern pattern,
    HitShape shape,
    float shapeParam,
    int baseDuration,
    float impactFraction,
    float damageMul,
    float knockbackMul,
    float rangeMul,
    int maxTargets,
    float energyMul,
    int[] limbs,
    float weight
) {
    public ComboStage {
        limbs = limbs == null ? new int[0] : limbs.clone();
    }

    @Override
    public int[] limbs() {
        return this.limbs.clone();
    }

    public boolean usesLimb(int limb) {
        for (int candidate : this.limbs) {
            if (candidate == limb) {
                return true;
            }
        }
        return false;
    }

    public int impactTick(int duration) {
        return Math.max(2, Math.min(duration - 2, Math.round(duration * this.impactFraction)));
    }

    @Override
    public String toString() {
        return "ComboStage[" + this.name + " " + this.pattern + "/" + this.shape + " limbs=" + Arrays.toString(this.limbs) + "]";
    }
}
