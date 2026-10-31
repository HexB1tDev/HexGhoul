package com.hex.kagune.control;

/**
 * 1.3.9: the one female body (pure), Eto's figure pulled out so every woman shares it: Eto, female players and every female NPC to come.
 * <ul>
 *   <li>Her size: {@link #SIZE} of a player's, drawn and boxed (her eyes as much lower).</li>
 *   <li>Slim arms (a wide-armed skin's arms are drawn {@link #ARMS} as wide).</li>
 *   <li>Her legs drawn {@link #HIPS} as wide and {@link #THIGHS} as deep, their pivots out at {@link #LEG_X} so they meet at the middle:
 *       her hips wider than her waist.</li>
 *   <li>The bust: a wedge on the front of the torso ({@link #BUST_TOP} down to {@link #BUST_BOTTOM}, out to {@link #BUST_OUT} at
 *       {@link #BUST_TIP}), bouncing as she jumps and lands ({@link #jiggle}).</li>
 * </ul>
 * Adults only: children are never drawn with any of it.
 */
public final class FemaleBody {
    /** How much smaller than a player a woman is: drawn and boxed (a head under most players). */
    public static final float SIZE = 0.86F;
    /** A wide-armed skin's arms drawn this wide (to a slim arm's 3 pixels). */
    public static final float ARMS = 0.75F;
    /** How much wider (across) and deeper (front to back) her legs are drawn. */
    public static final float HIPS = 1.25F;
    public static final float THIGHS = 1.1F;
    /** Each leg's pivot moved out so the wider legs meet at the middle (as vanilla's do, 0.1 pixel over) rather than overlap. */
    public static final float LEG_X = 2.0F * HIPS - 0.1F;
    /** Vanilla's leg pivot (model pixels out from the middle). */
    public static final float VANILLA_LEG_X = 1.9F;

    /** The bust (model pixels on the torso: x across -4..4, y down from the neck, z back): the torso's whole width. */
    public static final float BUST_TOP = 1.5F;
    public static final float BUST_TIP = 4.5F;
    public static final float BUST_BOTTOM = 6.0F;
    public static final float BUST_FRONT = -2.0F;
    public static final float BUST_OUT = -3.6F;

    /** Her bust's bounce, a damped spring (per tick squared, per tick) lagging her body's height, and at most how far it lags. */
    public static final double JIGGLE_STIFF = 0.9;
    public static final double JIGGLE_DAMP = 0.35;
    public static final double JIGGLE_MOST = 0.075;

    private FemaleBody() {
    }

    /**
     * Her bust's bounce carried on over {@code ticks} as her body moved {@code moved} blocks up ({@code state}: how far it lags her body,
     * blocks, and how fast that changes): it stays where it was as she moves, then springs back, overshooting and settling. A move of
     * more than a block (a teleport) starts it still.
     */
    public static void jiggle(double[] state, double moved, double ticks) {
        if (Math.abs(moved) > 1.0 || !(ticks > 0.0)) {
            if (Math.abs(moved) > 1.0) {
                state[0] = 0.0;
                state[1] = 0.0;
            }
            return;
        }
        state[0] = Math.max(-JIGGLE_MOST, Math.min(JIGGLE_MOST, state[0] - moved));
        int steps = (int)Math.ceil(Math.min(ticks, 4.0) / 0.25);
        double h = Math.min(ticks, 4.0) / steps;
        for (int i = 0; i < steps; i++) {
            state[1] += (-JIGGLE_STIFF * state[0] - JIGGLE_DAMP * state[1]) * h;
            state[0] = Math.max(-JIGGLE_MOST, Math.min(JIGGLE_MOST, state[0] + state[1] * h));
        }
    }

    /** How far (model pixels, up) her bust's tip is drawn off its rest for a lag of {@code lag} blocks. */
    public static float jigglePixels(double lag) {
        return (float)(lag * 16.0);
    }

    /** How far out (model pixels) each leg's pivot is moved past vanilla's, right leg -, left leg +. */
    public static float legShift() {
        return LEG_X - VANILLA_LEG_X;
    }
}
