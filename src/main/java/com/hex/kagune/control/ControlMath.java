package com.hex.kagune.control;

/**
 * Pure numeric model of kagune control. Shared by the server (authority) and the client (visuals).
 *
 * <ul>
 *   <li>{@code control} 0..1 is learned skill: overall mastery, Restraint practice and calming mutations.</li>
 *   <li>{@code hunger} 0..1 is starvation derived from the vanilla food level.</li>
 *   <li>{@code frenzy} 0..1 is transient bloodlust from manifesting, fighting and being hurt.</li>
 * </ul>
 * Everything here is a smooth function so progression never jumps between arbitrary stages.
 */
public final class ControlMath {
    /** Restraint points needed for the full Restraint bonus. */
    public static final int RESTRAINT_FOR_FULL_BONUS = 250;
    /** Instinct below this never produces involuntary actions. */
    public static final float URGE_THRESHOLD = 0.22F;

    private ControlMath() {
    }

    public static float clamp01(float value) {
        return Float.isNaN(value) ? 0.0F : Math.max(0.0F, Math.min(1.0F, value));
    }

    public static float smoothstep(float value) {
        float t = clamp01(value);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Mastery 0..1000 to a slightly front-loaded 0..1 curve; mastery 1000 alone is full control. */
    public static float masteryControl(int mastery) {
        float m = clamp01(mastery / 1000.0F);
        return (float)Math.pow(m, 0.85);
    }

    /** Practising restraint (suppressing urges, enduring manifestation, feeding properly) adds up to +0.18. */
    public static float restraintBonus(int restraint) {
        return 0.18F * smoothstep(restraint / (float)RESTRAINT_FOR_FULL_BONUS);
    }

    public static float control(int mastery, int restraint, boolean reachMutation, boolean multiControlMutation) {
        float value = masteryControl(mastery) + restraintBonus(Math.max(0, restraint));
        if (reachMutation) {
            value += 0.05F; // "Reach & Control: calmer control"
        }
        if (multiControlMutation) {
            value += 0.03F;
        }
        return clamp01(value);
    }

    /** Vanilla food level 0..20 to starvation 0..1. A food level of 14 or more is considered fed. */
    public static float starvation(int foodLevel) {
        if (foodLevel >= 14) {
            return 0.0F;
        }
        return clamp01((float)Math.pow((14 - Math.max(0, foodLevel)) / 14.0F, 1.15));
    }

    /** How hard the kagune's instincts push against the player right now. */
    public static float instinct(float control, float hunger, float frenzy) {
        float pressure = 0.35F + 0.4F * clamp01(hunger) + 0.35F * clamp01(frenzy);
        return clamp01((float)Math.pow(1.0F - clamp01(control), 1.2) * pressure * 1.25F);
    }

    /** Intensity of visible bodily struggle (pain, tremor, spasms). Zero at full control. */
    public static float struggle(float control, float hunger, float frenzy) {
        float base = (float)Math.pow(1.0F - clamp01(control), 0.9);
        return clamp01(base * (0.72F + 0.22F * clamp01(hunger) + 0.3F * clamp01(frenzy)));
    }

    /** Strength of the red peripheral bloodlust overlay. */
    public static float bloodlust(float control, float hunger, float frenzy) {
        float c = clamp01(control);
        return clamp01(clamp01(frenzy) * (1.0F - 0.7F * c) + clamp01(hunger) * 0.35F * (1.0F - 0.5F * c));
    }

    /** Scale for the original manifestation pain pose: slightly stronger than before at mastery 0, faint when mastered. */
    public static float manifestPainScale(float control, float hunger) {
        float c = clamp01(control);
        return 0.12F + 1.0F * (float)Math.pow(1.0F - c, 1.1) + 0.12F * clamp01(hunger) * (1.0F - c);
    }

    /** Frenzy gained when the kagune tears out. */
    public static float manifestFrenzy(float control) {
        return 0.6F * (1.0F - 0.85F * clamp01(control));
    }

    public static float hitFrenzy(float control, int targetsHit) {
        return Math.min(3, targetsHit) * 0.05F * (1.0F - 0.75F * clamp01(control));
    }

    public static float hurtFrenzy(float control, float damage) {
        return Math.min(0.25F, 0.05F + Math.max(0.0F, damage) * 0.02F) * (1.0F - 0.6F * clamp01(control));
    }

    public static float killFrenzy(float control) {
        return 0.14F * (1.0F - 0.75F * clamp01(control));
    }

    /** Frenzy lost per tick. Calm, mastered ghouls settle faster. */
    public static float frenzyDecay(float control, boolean manifested) {
        return manifested ? 0.0035F + 0.0065F * clamp01(control) : 0.02F;
    }

    /** Starvation keeps a floor of bloodlust while the kagune is out. */
    public static float frenzyFloor(float control, float hunger) {
        return clamp01(hunger) * 0.4F * (1.0F - clamp01(control));
    }

    /** Chance per half-second check that an involuntary urge starts, before configuration scaling. */
    public static float urgeChance(float instinct, float contextWeight) {
        if (instinct <= URGE_THRESHOLD || contextWeight <= 0.0F) {
            return 0.0F;
        }
        float over = (instinct - URGE_THRESHOLD) / (1.0F - URGE_THRESHOLD);
        return clamp01((float)Math.pow(over, 1.4) * 0.22F * Math.min(1.0F, contextWeight));
    }

    /** Ticks before another urge may start. */
    public static int urgeCooldownTicks(float control, float random01) {
        return 100 + Math.round(140.0F * clamp01(control)) + Math.round(60.0F * clamp01(random01));
    }

    /** Chance that crouching successfully restrains an urge. */
    public static float restrainChance(float control, float hunger, float frenzy) {
        return clamp01(0.6F + 0.4F * clamp01(control) + 0.15F * (1.0F - clamp01(hunger)) - 0.2F * clamp01(frenzy));
    }

    /** Duration multiplier for kagune strikes: sluggish and heavy when untrained, crisp when mastered. */
    public static float strikeDurationScale(float control) {
        return 1.12F - 0.22F * clamp01(control);
    }

    /** Ticks after a strike ends during which the next click continues the combo. */
    public static int comboWindowTicks(float control) {
        return 8 + Math.round(10.0F * clamp01(control));
    }

    /** Maximum random yaw deviation (degrees) of an untrained strike. */
    public static float strikeDeviationDegrees(float control, boolean involuntary) {
        float base = 16.0F * (float)Math.pow(1.0F - clamp01(control), 1.5);
        return involuntary ? base * 1.5F : base;
    }

    /** Rotates the horizontal vector (x, z) around +Y. Server hit volumes and client strike paths share this. */
    public static double[] rotateYaw(double x, double z, float degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new double[] {x * cos - z * sin, x * sin + z * cos};
    }

    /** Food exhaustion added per tick while the kagune is manifested. */
    public static float manifestExhaustion(int limbs, float control) {
        return 0.0035F * (1.0F + 0.12F * Math.max(1, limbs)) * (1.0F - 0.35F * clamp01(control));
    }
}
