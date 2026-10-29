package com.hex.kagune.control.eto;

/**
 * 6.2.0, You Thought You'd Get Rid of Me That Easily? (pure, tested): the timings of Eto's death that does not take. The blow that
 * would kill her blows her body apart instead ({@link Remains}: her limbs and head torn off, her torso torn in two, her guts out,
 * blood everywhere); she lies in pieces for {@link #SCATTER} ticks; then flesh lines run out of what is left of her chest to every
 * piece of her ({@link #LINES}), and they haul everything back ({@link #PULL}: what is left of her chest stands first, the rest is
 * dragged in to it piece by piece, the head last, each turned upright on the way and slammed home) and she stands there whole, at
 * full health, the moment the last piece is in. Ready again {@link #COOLDOWN} ticks (five minutes) after it took.
 *
 * <p>7.0.0 (Furuta DNA's mutation now): it takes far longer: eleven seconds in pieces, the lines a second and a half running out,
 * the pull over a second and a quarter (was five, under one, and under one).
 */
public final class ReviveRules {
    /** Ticks she lies in pieces, the lines running out to every piece, the pull back together. */
    public static final int SCATTER = 220;
    public static final int LINES = 30;
    public static final int PULL = 26;
    /** Ticks from the blow until she stands there whole again. */
    public static final int WHOLE = SCATTER + LINES + PULL;
    /** Ticks (five minutes) before it can take again. */
    public static final long COOLDOWN = 6000L;
    /** How long (ticks) each line takes to reach its piece: the last sets out just in time to be there as the pull begins. */
    static final double REACH = 16.0;

    private ReviveRules() {
    }

    /** Whether it is ready at {@code now} (game time), ready since {@code readyAt}: a ready time further off than it can be is forgotten. */
    public static boolean ready(long now, long readyAt) {
        return now >= readyAt || readyAt - now > COOLDOWN;
    }

    /** How far out (0..1) the line to piece {@code index} (of {@code count}) has run, {@code age} ticks after the blow. */
    public static double line(double age, int index, int count) {
        double start = SCATTER + (LINES - REACH) * index / Math.max(1, count - 1);
        return smooth((age - start) / REACH);
    }

    /**
     * How far (0..1) the pull has brought everything back, {@code age} ticks after the blow: the lines take the strain, then they
     * haul, faster and faster, and it all slams back together.
     */
    public static double pull(double age) {
        double u = clamp01((age - SCATTER - LINES) / PULL);
        return Math.pow(u, 2.2);
    }

    /**
     * 6.2.0: how far (0..1) one piece has been hauled home {@code age} ticks after the blow, hauled from {@code start} to {@code end}
     * (fractions of the pull): faster and faster, so it slams in; 1 from {@code end} on (it is home).
     */
    public static double haul(double age, double start, double end) {
        double u = clamp01((age - SCATTER - LINES - start * PULL) / Math.max(1.0E-6, (end - start) * PULL));
        return u > 1.0 - 1.0E-9 ? 1.0 : Math.pow(u, 1.6);
    }

    /** 6.2.0: how far (0..1) a piece hauled from {@code start} to {@code end} has turned back upright: upright before it is home. */
    public static double upright(double age, double start, double end) {
        double u = clamp01((age - SCATTER - LINES - start * PULL) / Math.max(1.0E-6, (end - start) * PULL * UPRIGHT));
        return u > 1.0 - 1.0E-9 ? 1.0 : smooth(u);
    }

    /** How much of its way a hauled piece takes to turn upright (it arrives upright, never turning as it slots in). */
    static final double UPRIGHT = 0.7;

    /** Whether she is in pieces at {@code age} ticks after the blow (her body not drawn; the pieces are). */
    public static boolean apart(double age) {
        return age >= 0.0 && age < WHOLE;
    }

    static double clamp01(double u) {
        return Math.max(0.0, Math.min(1.0, u));
    }

    static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3.0 - 2.0 * c);
    }
}
