package com.hex.kagune.control.king;

/**
 * 1.0.17: how a final kakuja comes over him (pure; the server's sounds and blast, every client's body, model and effects all go by
 * it, from the tick it was put on).
 *
 * <ul>
 *   <li>War: violent. The darkness gathers into him for {@link #WAR_BURST} ticks, his body wrenched and shaking with it, his heart
 *       loud; then it bursts: the kakuja tears out of his chest all at once (out past itself, then back), blood thrown wide, a black
 *       shock running out over the ground, all near flung back and blinded, his cape whipped out.</li>
 *   <li>Peace: graceful. He rises a little, arms opening; the kagune unfurls from his back as wings, spread wide and high, then
 *       folds into its crown; the white armour grows over him from his heart outward like muscle swelling under the skin, a little
 *       past itself and settling; a soft white light at the end, feathers drifting down.</li>
 * </ul>
 * 1.0.19: and how it leaves him (taken off):
 * <ul>
 *   <li>War: it dies on him. A shudder; then the kakuja hardens and crumbles to ash, piece by piece ({@link KingShed}), the rinkaku
 *       from their tips in first, his chest last, falling away as black flakes and smoke while he straightens, spent.</li>
 *   <li>Peace: it is drawn back into him. The blades fold in to his back and sink into it, the wings close and wither, shedding
 *       feathers, and the white armour recedes from his hands and feet back into his heart; a last soft light there, and gone.</li>
 * </ul>
 */
public final class KingTransform {
    /** War: the gathering, then the burst; how long it all runs. */
    public static final int WAR_BURST = 16;
    public static final int WAR_TICKS = 40;
    /** How far (blocks) War's burst throws what is near, and blinds it. */
    public static final double WAR_BLAST = 6.0;
    /** Peace: the wings unfold (from .. to), the armour grows (from .. to), the light; how long it all runs. */
    public static final int PEACE_WINGS_FROM = 4;
    public static final int PEACE_WINGS_TO = 44;
    public static final int PEACE_ARMOUR_FROM = 10;
    public static final int PEACE_ARMOUR_TO = 42;
    public static final int PEACE_LIGHT = 46;
    public static final int PEACE_TICKS = 56;
    /** 1.0.19: how long each takes to leave him; Peace's blades fold in until the first, its wings wither from the second, its armour
     *  recedes over the third and fourth. */
    public static final int WAR_LEAVE = 32;
    public static final int PEACE_LEAVE = 36;
    public static final int PEACE_FOLD = 14;
    public static final int PEACE_WITHER = 8;
    public static final int PEACE_RECEDE_FROM = 10;
    public static final int PEACE_RECEDE_TO = 32;
    /** How far out (body pixels) the growth has reached from his heart at its end: all of him. */
    public static final double REACH = 26.0;

    private KingTransform() {
    }

    /** How long {@code form} takes to come over him (ticks). */
    public static int ticks(KingForm form) {
        return switch (form) {
            case WAR -> WAR_TICKS;
            case PEACE -> PEACE_TICKS;
            default -> KingRules.GROW_TICKS;
        };
    }

    /** 1.0.19: how long {@code form} takes to leave him (ticks). */
    public static int leave(KingForm form) {
        return switch (form) {
            case WAR -> WAR_LEAVE;
            case PEACE -> PEACE_LEAVE;
            default -> KingRules.GROW_TICKS;
        };
    }

    /** 1.0.19: true while {@code form}, taken off ({@code !on}) {@code age} ticks ago, is still leaving him. */
    public static boolean going(KingForm form, boolean on, double age) {
        return !on && (form == KingForm.WAR || form == KingForm.PEACE) && age >= 0.0 && age < leave(form);
    }

    /** True while {@code form}, put on ({@code on}) {@code age} ticks ago, is still coming over him. */
    public static boolean coming(KingForm form, boolean on, double age) {
        return on && (form == KingForm.WAR || form == KingForm.PEACE) && age >= 0.0 && age < ticks(form);
    }

    /** How far grown over him ({@code on}) or gone back ({@code !on}) {@code form} is, {@code age} ticks in: 0..1. */
    public static double grow(KingForm form, boolean on, double age) {
        if (form != KingForm.WAR && form != KingForm.PEACE) {
            return KingRules.grow(on, age);
        }
        if (!on) {
            // 1.0.19: all of it there while it leaves (crumbling, folding, receding: its own ways), then none.
            return age < leave(form) ? 1.0 : 0.0;
        }
        if (form == KingForm.WAR) {
            return age < WAR_BURST ? 0.0 : smooth((age - WAR_BURST) / 2.0);
        }
        return smooth((age - PEACE_WINGS_FROM) / (double)(PEACE_ARMOUR_TO - PEACE_WINGS_FROM));
    }

    /** True for the tick War's burst goes off. */
    public static boolean burstNow(KingForm form, long age) {
        return form == KingForm.WAR && age == WAR_BURST;
    }

    /** True in the moments just after War's burst (a client seeing it come out of him throws its cape out). */
    public static boolean bursting(KingForm form, double age) {
        return form == KingForm.WAR && age >= WAR_BURST && age < WAR_BURST + 6.0;
    }

    /**
     * How far the growth has reached out from his heart (body pixels), and how swollen what it has reached is: {front, swell, jitter}
     * (null: no growth to show, it is all there). War's tears out in three ticks past itself and shudders back; Peace's swells out over
     * the whole of its armour's time.
     */
    public static double[] growth(KingForm form, boolean on, double age) {
        if (form == KingForm.PEACE && going(form, on, age)) {
            // 1.0.19: Peace's armour drawn back from his hands and feet into his heart, a last swell as it goes.
            double u = clamp((age - PEACE_RECEDE_FROM) / (double)(PEACE_RECEDE_TO - PEACE_RECEDE_FROM));
            return new double[] {REACH * (1.0 - smooth(u)), 0.08 * Math.sin(Math.PI * u), 0.0};
        }
        if (!coming(form, on, age)) {
            return null;
        }
        if (form == KingForm.WAR) {
            double t = age - WAR_BURST;
            if (t < 0.0) {
                return new double[] {0.0, 0.0, 0.0};
            }
            double front = REACH * 1.4 * smooth(t / 3.0);
            double swell = 0.38 * Math.exp(-Math.max(0.0, t - 1.0) / 3.0);
            double jitter = 0.7 * Math.exp(-t / 4.0);
            return t > 18.0 ? null : new double[] {front, swell, jitter};
        }
        double u = (age - PEACE_ARMOUR_FROM) / (double)(PEACE_ARMOUR_TO - PEACE_ARMOUR_FROM);
        if (u > 1.4) {
            return null;
        }
        double front = REACH * smooth(Math.min(1.0, u));
        // Like muscle: swollen as it grows, settling after; one breath of it again at the end.
        double swell = 0.12 * Math.sin(Math.PI * clamp(u)) + 0.05 * Math.max(0.0, Math.sin(Math.PI * (u - 1.0) / 0.4));
        return new double[] {front, swell, 0.0};
    }

    /** How much of a vertex {@code d} pixels from his heart is out of his skin, the growth at {@code g} ({@link #growth}): 0..1+. */
    public static double out(double d, double[] g) {
        double band = 5.0;
        double s = smooth((g[0] - d) / band);
        return s * (1.0 + g[1] * Math.sin(Math.PI * Math.min(1.0, s * 1.3)) + g[1] * 0.4 * s);
    }

    /**
     * Peace's kagune unfolding as wings, {@code age} ticks in: -1 once done (or not Peace), else 0..1 how far through (spread widest
     * in the middle, folded into the crown at the end).
     */
    public static double unfold(KingForm form, boolean on, double age) {
        if (form != KingForm.PEACE || !coming(form, on, age)) {
            return -1.0;
        }
        return clamp((age - PEACE_WINGS_FROM) / (double)(PEACE_WINGS_TO - PEACE_WINGS_FROM));
    }

    /** The soft white light at Peace's end: 0..1, and gone. */
    public static double light(KingForm form, boolean on, double age) {
        if (form != KingForm.PEACE || !on) {
            return 0.0;
        }
        double t = age - PEACE_LIGHT;
        return t < 0.0 || t > 10.0 ? 0.0 : Math.sin(Math.PI * t / 10.0);
    }

    static double smooth(double t) {
        t = clamp(t);
        return t * t * (3.0 - 2.0 * t);
    }

    static double clamp(double t) {
        return Math.max(0.0, Math.min(1.0, t));
    }
}
