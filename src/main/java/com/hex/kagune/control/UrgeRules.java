package com.hex.kagune.control;

/** Pure decision rules for involuntary urges; the server feeds in world facts and random rolls. */
public final class UrgeRules {
    /** What drew the kagune's attention. */
    public enum PreyKind {
        /** A hostile mob or anything that recently hurt the host. Every urge is possible. */
        THREAT,
        /** A huntable animal while hungry or frenzied. Every urge is possible. */
        HUNT,
        /** A human-like scent (players, villagers). Only glances and steps; never forced attacks. */
        SCENT
    }

    private UrgeRules() {
    }

    public static float contextWeight(PreyKind kind, float hunger, float frenzy) {
        if (kind == null) {
            return 0.0F;
        }
        return switch (kind) {
            case THREAT -> 1.0F;
            case HUNT -> hunger >= 0.25F || frenzy >= 0.5F ? 0.35F + 0.65F * Math.max(hunger, frenzy * 0.6F) : 0.0F;
            case SCENT -> hunger >= 0.35F || frenzy >= 0.6F ? 0.25F + 0.4F * hunger : 0.0F;
        };
    }

    /**
     * @param canStrike the server confirmed a kagune swipe could legally start at this prey right now
     * @param roll01    uniform random in [0,1)
     */
    public static UrgeType choose(float instinct, PreyKind kind, boolean onGround, double distance, boolean canStrike, float roll01) {
        float glance = 1.0F;
        float step = onGround && distance > 2.0 ? 0.6F * instinct : 0.0F;
        boolean aggressive = kind != PreyKind.SCENT;
        float lunge = aggressive && onGround && distance >= 3.0 && distance <= 9.0 ? Math.max(0.0F, instinct - 0.45F) * 1.2F : 0.0F;
        float strike = aggressive && canStrike ? Math.max(0.0F, instinct - 0.35F) * 1.4F : 0.0F;
        float total = glance + step + lunge + strike;
        float pick = Math.max(0.0F, Math.min(0.9999F, roll01)) * total;
        if ((pick -= strike) < 0.0F) {
            return UrgeType.STRIKE;
        }
        if ((pick -= lunge) < 0.0F) {
            return UrgeType.LUNGE;
        }
        if ((pick -= step) < 0.0F) {
            return UrgeType.STEP;
        }
        return UrgeType.GLANCE;
    }

    /** Whole urge length in ticks. For STRIKE this is the tell before the kagune lashes out. */
    public static int durationTicks(UrgeType type, float instinct) {
        float i = ControlMath.clamp01(instinct);
        return switch (type) {
            case GLANCE -> 8 + Math.round(10.0F * i);
            case STEP, LUNGE -> 10;
            case STRIKE -> 7 + Math.round(4.0F * i);
            case NONE -> 0;
        };
    }

    /** How strongly the client pulls the view and resists movement away from the prey. */
    public static float strength(float instinct) {
        return 0.35F + 0.65F * ControlMath.clamp01(instinct);
    }

    /** Horizontal push in blocks/tick applied by the server for STEP/LUNGE. */
    public static double pushSpeed(UrgeType type, float instinct) {
        return switch (type) {
            case STEP -> 0.22 + 0.12 * ControlMath.clamp01(instinct);
            case LUNGE -> 0.42 + 0.2 * ControlMath.clamp01(instinct);
            default -> 0.0;
        };
    }
}
