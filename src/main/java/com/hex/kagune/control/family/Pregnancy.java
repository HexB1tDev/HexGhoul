package com.hex.kagune.control.family;

/**
 * 1.3.10: a pregnancy (pure), for any woman: a female player or any female NPC. Two Minecraft days from the night she conceived to the
 * birth ({@link #TERM}); her belly shows from a fifth of the way and swells gently to its fullest at the end ({@link #belly}); how far
 * along she is ({@link Stage}) colours what she says; her partner is told at {@link #ALERTS}.
 */
public final class Pregnancy {
    /** Conception to birth (game ticks): two Minecraft days. */
    public static final long TERM = 48000L;
    /** How far along before her belly starts to show. */
    public static final double SHOWS = 0.2;
    /** How far out (model pixels) her belly is at its fullest, in front of the torso. */
    public static final float BELLY_OUT = 1.7F;
    /** How far along her partner is told something (the first kick, the weight of it, her time close). */
    public static final double[] ALERTS = {0.35, 0.7, 0.93};

    /** Early (nothing shows), along (it shows), late (heavy, tired), due. */
    public enum Stage {
        EARLY,
        ALONG,
        LATE,
        DUE
    }

    private Pregnancy() {
    }

    /** How far along (0..1) at {@code now}, conceived at {@code conceived}. */
    public static double progress(long conceived, long now) {
        if (conceived == Long.MIN_VALUE) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (now - conceived) / (double)TERM));
    }

    public static boolean due(long conceived, long now) {
        return conceived != Long.MIN_VALUE && now - conceived >= TERM;
    }

    /** How much of her belly's fullest swell shows (0..1): none before {@link #SHOWS}, then easing in to all of it at the end. */
    public static float belly(double progress) {
        double t = Math.max(0.0, Math.min(1.0, (progress - SHOWS) / (1.0 - SHOWS)));
        return (float)(t * t * (3.0 - 2.0 * t));
    }

    public static Stage stage(double progress) {
        if (progress >= 1.0) {
            return Stage.DUE;
        }
        return progress < SHOWS ? Stage.EARLY : progress < 0.75 ? Stage.ALONG : Stage.LATE;
    }

    /** Which alerts ({@link #ALERTS} by index, as bits) going from {@code before} to {@code after} crosses. */
    public static int crossed(double before, double after) {
        int bits = 0;
        for (int i = 0; i < ALERTS.length; i++) {
            if (before < ALERTS[i] && after >= ALERTS[i]) {
                bits |= 1 << i;
            }
        }
        return bits;
    }
}
