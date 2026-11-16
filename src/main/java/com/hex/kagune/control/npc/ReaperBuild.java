package com.hex.kagune.control.npc;

/**
 * 2.0.45 (pure, tested; the owner: "adding some muscle to him ... The top part of the torso, should be just a little bit bigger than the
 * one in the bottom that connects the legs ... make the chest subside a little bit from the belly ... don't actually make muscle. Don't
 * design spheres or anything. Just simply increase the size of parts of the already existing player structure"): the White Reaper's
 * build. His torso's upper part is drawn again over it, a little bigger, in his own skin and its jacket layer: a chest broader across
 * the shoulders and lats ({@link #PROFILE}) and standing out in front of the belly, tapering back into the torso as it is at the waist,
 * so his upper body is wider than the hips under it (a V). His arms are set {@link #ARM_OUT} further out to clear it.
 *
 * <p>Model pixels of the torso, as vanilla's player model has them: x to his left, y down from the neck, z backward (his front is -z).
 * The torso is 8 x 12 x 4 (x -4..4, z -2..2); its skin at (16, 16), its jacket layer at (16, 32), on a 64 x 64 skin.
 */
public final class ReaperBuild {
    private ReaperBuild() {
    }

    /**
     * The chest's outline down the torso: each row {y, half its width, how far its front stands out, how far its back}. Broad at the
     * shoulders and the chest, out in front of the belly, then in to just inside the torso at the waist (where the torso takes over).
     */
    public static final double[][] PROFILE = {
        {-0.03, 4.5, 2.5, 2.2},
        {1.2, 4.68, 2.95, 2.35},
        {5.2, 4.62, 3.05, 2.35},
        {6.8, 4.32, 2.45, 2.15},
        {8.5, 3.97, 1.97, 1.97}};
    /** How far (pixels) the jacket layer stands off the chest, as the skin's second layer does on the torso. */
    public static final double JACKET = 0.25;
    /** How much further out (pixels) each arm hangs, clear of the broader chest. */
    public static final float ARM_OUT = 0.75F;

    /**
     * 2.2.5 (the owner: "the men's chest, the top chest, is a little too large. At least for the player. Keep the same for Arima"): a male
     * player's build, slimmer than the White Reaper's: the same shape ({@link #PROFILE}'s rows, the waist the same), but only
     * {@link #PLAYER_SHARE} of how far his stands out past the torso, across the shoulders and chest, in front and behind.
     */
    // 4.1.48 (the owner: "Reduce the player's muscle size to be the same as Noro."): Noro's, 0.52 (it was 0.6).
    public static final double PLAYER_SHARE = 0.52;
    /** A male player's chest's outline: {@link #PROFILE}, {@link #PLAYER_SHARE} as far out past the torso (its waist row as his). */
    public static final double[][] PLAYER = slimmer(PROFILE, PLAYER_SHARE);
    /** How much further out (pixels) a male player's arms hang, clear of his chest: {@link #armOut(double)} of his build (0.39). */
    public static final float PLAYER_ARM_OUT = (float)(ARM_OUT * PLAYER_SHARE);
    /**
     * 4.0.8 (the owner: "I sent you the new ayato skin, make him a little muscular but way less than the male player."): Ayato's build, the
     * same shape, {@link #AYATO_SHARE} of how far the White Reaper's stands out past the torso (a male player's is {@link #PLAYER_SHARE}).
     */
    // 4.1.48 (the owner: "ayato is meant to have muscles they're gone? Also give some to Noro, both less than the player, noro more than
    // ayato"): 0.42 (0.3 was lost under his black jacket in game); Noro's NORO_SHARE more, a male player's the same as Noro's.
    public static final double AYATO_SHARE = 0.42;
    public static final double[][] AYATO = slimmer(PROFILE, AYATO_SHARE);
    /** 4.1.48: Noro's build, more than Ayato's; a male player's the same (the owner: "the same as Noro"). */
    public static final double NORO_SHARE = PLAYER_SHARE;

    /**
     * 4.1.5 (the owner: "give him some muscles also less than the player"): the General's build, between Ayato's and a male player's.
     */
    public static final double GENERAL_SHARE = 0.45;

    /** 4.0.8: a build {@code share} of the White Reaper's: its outline. */
    public static double[][] of(double share) {
        return share >= 1.0 ? PROFILE : share == PLAYER_SHARE ? PLAYER : share == AYATO_SHARE ? AYATO : slimmer(PROFILE, share);
    }

    /** 4.0.8: how much further out (pixels) the arms of a build {@code share} of the Reaper's hang (his 0.75; a male player's 0.39). */
    public static float armOut(double share) {
        return (float)(ARM_OUT * share);
    }
    /**
     * 2.3.3 (the owner: "Whenever you transform into any form, your chest ... vanishes ... The chest should adapt to the form and vice
     * versa"): how much a form's armour fitted to the bare torso (Ukaku's kakuja plates, Graft's hide, War's and Peace's shells) is
     * widened ({@code X}) and deepened ({@code Z}) about the torso's middle to sit over a male player's chest, jacket layer and all.
     */
    public static final float PLAYER_FIT_X = (float)((widest(PLAYER) + JACKET) / (4.0 + JACKET));
    public static final float PLAYER_FIT_Z = (float)((deepest(PLAYER) + JACKET) / (2.0 + JACKET));

    static double widest(double[][] rows) {
        double most = 0.0;
        for (double[] r : rows) {
            most = Math.max(most, r[1]);
        }
        return most;
    }

    static double deepest(double[][] rows) {
        double most = 0.0;
        for (double[] r : rows) {
            most = Math.max(most, Math.max(r[2], r[3]));
        }
        return most;
    }

    /** {@code rows} with only {@code share} of how far each stands out past the torso (4 a side, 2 front and back); inside it, as is. */
    static double[][] slimmer(double[][] rows, double share) {
        double[][] out = new double[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            double[] r = rows[i];
            out[i] = new double[] {r[0], past(r[1], 4.0, share), past(r[2], 2.0, share), past(r[3], 2.0, share)};
        }
        return out;
    }

    private static double past(double value, double torso, double share) {
        return value > torso ? torso + (value - torso) * share : value;
    }

    /**
     * 2.0.46: how far back (pixels, +z) the build's back stands at {@code y} down the torso ({@code jacket}: its jacket layer's), or 0 where
     * there is none (above the shoulders or below the waist): what is laid on a man's back (the Rinkaku mark) sits on it.
     */
    public static double back(double y, boolean jacket) {
        return back(PROFILE, y, jacket);
    }

    /** 2.2.5: {@link #back(double, boolean)} for the build whose outline is {@code profile} ({@link #PROFILE} or {@link #PLAYER}). */
    public static double back(double[][] profile, double y, boolean jacket) {
        int rows = profile.length;
        if (!(y >= profile[0][0]) || y > profile[rows - 1][0]) {
            return 0.0;
        }
        for (int i = 0; i + 1 < rows; i++) {
            double[] a = profile[i];
            double[] b = profile[i + 1];
            if (y <= b[0]) {
                double t = (y - a[0]) / (b[0] - a[0]);
                return a[3] + (b[3] - a[3]) * t + (jacket ? JACKET : 0.0);
            }
        }
        return 0.0;
    }

    /** Where each quad goes: its four corners (torso pixels), their places on the skin (texture pixels, u v four times), its normal. */
    public interface Sink {
        void quad(double[][] corners, double[] uv, double[] normal);
    }

    /** The chest ({@code jacket}: its jacket layer, standing {@link #JACKET} off it), quad by quad: front, back, sides, and the top. */
    public static void chest(boolean jacket, Sink sink) {
        chest(PROFILE, jacket, sink);
    }

    /** 2.2.5: {@link #chest(boolean, Sink)} for the build whose outline is {@code profile} ({@link #PROFILE} or {@link #PLAYER}). */
    public static void chest(double[][] profile, boolean jacket, Sink sink) {
        double grow = jacket ? JACKET : 0.0;
        double v0 = jacket ? 32.0 : 16.0;
        int rows = profile.length;
        for (int i = 0; i + 1 < rows; i++) {
            double[] a = profile[i];
            double[] b = profile[i + 1];
            double ya = a[0] - (i == 0 ? grow : 0.0);
            double yb = b[0];
            double wa = a[1] + grow;
            double wb = b[1] + grow;
            double fa = -(a[2] + grow);
            double fb = -(b[2] + grow);
            double ba = a[3] + grow;
            double bb = b[3] + grow;
            // The front, seen from in front: his right (-x) on the left of the skin's front.
            emit(sink, v0, new double[][] {{-wa, ya, fa}, {wa, ya, fa}, {wb, yb, fb}, {-wb, yb, fb}}, Face.FRONT);
            emit(sink, v0, new double[][] {{wa, ya, ba}, {-wa, ya, ba}, {-wb, yb, bb}, {wb, yb, bb}}, Face.BACK);
            emit(sink, v0, new double[][] {{wa, ya, fa}, {wa, ya, ba}, {wb, yb, bb}, {wb, yb, fb}}, Face.LEFT);
            emit(sink, v0, new double[][] {{-wa, ya, ba}, {-wa, ya, fa}, {-wb, yb, fb}, {-wb, yb, bb}}, Face.RIGHT);
        }
        double[] top = profile[0];
        double y = top[0] - grow;
        double w = top[1] + grow;
        double f = -(top[2] + grow);
        double back = top[3] + grow;
        emit(sink, v0, new double[][] {{-w, y, back}, {w, y, back}, {w, y, f}, {-w, y, f}}, Face.TOP);
    }

    enum Face {
        FRONT, BACK, LEFT, RIGHT, TOP
    }

    /** One quad, each corner painted with the torso's texel for where it is (clamped onto the torso's own faces), its normal outward. */
    static void emit(Sink sink, double v0, double[][] c, Face face) {
        double[] uv = new double[8];
        for (int k = 0; k < 4; k++) {
            double[] t = texel(c[k], face, v0);
            uv[2 * k] = t[0];
            uv[2 * k + 1] = t[1];
        }
        double[] e1 = {c[2][0] - c[0][0], c[2][1] - c[0][1], c[2][2] - c[0][2]};
        double[] e2 = {c[3][0] - c[1][0], c[3][1] - c[1][1], c[3][2] - c[1][2]};
        double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
        double[] mid = {(c[0][0] + c[2][0]) * 0.5, (c[0][1] + c[2][1]) * 0.5, (c[0][2] + c[2][2]) * 0.5};
        double[] out = {mid[0], face == Face.TOP ? -1.0 : 0.0, mid[2]};
        if (n[0] * out[0] + n[1] * out[1] + n[2] * out[2] < 0.0) {
            n = new double[] {-n[0], -n[1], -n[2]};
        }
        double l = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        sink.quad(c, uv, l < 1.0E-9 ? new double[] {0.0, -1.0, 0.0} : new double[] {n[0] / l, n[1] / l, n[2] / l});
    }

    /**
     * Where on the skin (texture pixels) a corner at {@code p} of a {@code face} is painted from: the torso's own face's texel at the
     * same place, its x clamped to the torso's width and its z to its depth (a quarter pixel in from an edge, so a side stays on its
     * own column).
     */
    static double[] texel(double[] p, Face face, double v0) {
        double x = Math.max(-3.75, Math.min(3.75, p[0]));
        double z = Math.max(-1.75, Math.min(1.75, p[2]));
        double y = Math.max(0.0, Math.min(12.0, p[1]));
        double v = v0 + 4.0 + y;
        return switch (face) {
            case FRONT -> new double[] {20.0 + 4.0 + x, v};
            case BACK -> new double[] {32.0 + 4.0 - x, v};
            case LEFT -> new double[] {28.0 + 2.0 + z, v};
            case RIGHT -> new double[] {16.0 + 2.0 - z, v};
            case TOP -> new double[] {20.0 + 4.0 + x, v0 + 2.0 - z};
        };
    }
}
