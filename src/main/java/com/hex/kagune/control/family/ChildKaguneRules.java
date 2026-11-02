package com.hex.kagune.control.family;

/**
 * 1.3.43: how a ghoul child fights with the kagune it inherited (pure, tested; the world's side is {@code ChildKagune}). 1.3.44 (the
 * owner): its Rinkaku is one tentacle, a Rinkaku player's (the original's tube, the first tentacle's curl over the head at rest), lashing
 * on the beats of the lash string; its wing or wings shoot as the wild ghouls' do (one wing: every shot from that side); all of it at its
 * body's size; with a tentacle and a wing it uses both, the tentacle up close, the wing from further off. It only ever fights what
 * threatens its family (as before).
 */
public final class ChildKaguneRules {
    /** How close it keeps when it only shoots, how far it lets its prey get, how far a shot flies, ticks between shots. */
    public static final double SHOOT_NEAREST = 4.0;
    public static final double SHOOT_FARTHEST = 10.0;
    public static final double SHOT_RANGE = 14.0;
    public static final int SHOT_EVERY = 10;
    public static final double SHOT_SPEED = 1.3;
    /** How long it waits between strings and volleys (ticks), at least and at most. */
    public static final int PAUSE_LEAST = 20;
    public static final int PAUSE_MOST = 42;
    /** A lash or a shot's damage against its punch's (a child's kagune is lighter than a grown ghoul's). */
    public static final float KAGUNE_SHARE = 0.45F;

    /** What it does this tick: lash with its tails, shoot with its wing(s), or close in (it has nothing that reaches yet). */
    public enum Use {
        TAILS, WINGS, CLOSE_IN, NONE
    }

    private ChildKaguneRules() {
    }

    /** How big its kagune is against a grown ghoul's: as big as it is ({@link ChildStage#draw}). */
    public static double scale(ChildStage stage) {
        return stage == null ? 1.0 : stage.draw;
    }

    /**
     * The mastery its tentacle strikes at (the lash string's beats, {@code GraftMove.beats}): 1.3.44 (the owner: "it's meant to be one
     * tentacle"): one, a teen's a little longer, always under the second tail's mastery. By its stage alone: its clients draw it from what
     * they know of it, and the server strikes from the same.
     */
    public static int tailMastery(ChildStage stage) {
        return stage == ChildStage.TEEN ? 60 : 0;
    }

    /** 1.3.44: how many Rinkaku tentacles it has: one (with or without its wing). */
    public static final int TENTACLES = 1;
    /** 1.3.44: the tentacle's thickness, as a Rinkaku player's first tentacle is (the original's 0.18 at mastery 0), before its size. */
    public static final float TENTACLE_THICKNESS = 0.18F;
    /** 1.3.44: the original seed tentacle's layout (KaguneForm.RINKAKU_SEED: fan to the side, height, back, its sway). */
    public static final double SEED_FAN = 0.15;
    public static final double SEED_HEIGHT = 0.44;
    public static final double SEED_BACK = 0.62;
    public static final double SEED_WIGGLE = 0.16;
    /** 1.3.44: where it leaves the body (rig space: its pelvis, as a Rinkaku player's, TendrilChain.pelvisAnchor's height and back). */
    public static final double[] TENTACLE_ROOT = {0.0, 1.8 * 0.43, -0.18};

    /** 1.3.44: how much of its length its tentacle shows at rest (the rest curled over; a lash straightens it out to the whole). */
    public static final double REST_SHARE = 0.85;

    /**
     * 1.3.44: its tentacle at rest, in the shape the original lays a Rinkaku player's first tentacle out in (the cubic from the pelvis
     * through its two controls to its rest above and a little ahead of the head: up behind the back and curling over), drawn out about
     * its root to {@link #REST_SHARE} of its {@code length} (rig space, a grown body's: as long at rest as it is in a lash, so it never
     * stretches), swaying a little on {@code time} (ticks) by its {@code seed}; {@code points} evenly along the curve, root first.
     */
    public static double[][] tentacleRest(double length, double time, int seed, int points) {
        double[] root = TENTACLE_ROOT;
        double arch = length * 0.09;
        double[] rest = {SEED_FAN, root[1] + SEED_HEIGHT + arch, root[2] + length * (0.03 + SEED_BACK * 0.04)};
        double[] a = {SEED_FAN * 0.16, root[1] + SEED_HEIGHT * 0.42 + length * 0.15, root[2] - length * 0.22};
        double[] b = {rest[0] + SEED_FAN * 0.05, rest[1] + length * 0.12, rest[2] - length * 0.16};
        int n = Math.max(2, points);
        double[][] curve = new double[n][];
        double run = 0.0;
        for (int i = 0; i < n; i++) {
            double t = i / (double)(n - 1);
            double u = 1.0 - t;
            double[] p = new double[3];
            for (int k = 0; k < 3; k++) {
                p[k] = u * u * u * root[k] + 3.0 * u * u * t * a[k] + 3.0 * u * t * t * b[k] + t * t * t * rest[k];
            }
            curve[i] = p;
            if (i > 0) {
                run += Math.sqrt(Math.pow(p[0] - curve[i - 1][0], 2) + Math.pow(p[1] - curve[i - 1][1], 2) + Math.pow(p[2] - curve[i - 1][2], 2));
            }
        }
        double grow = run < 1.0E-6 ? 1.0 : REST_SHARE * length / run;
        double phase = time / 20.0 + (seed & 255) * 0.0245;
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            double t = i / (double)(n - 1);
            double[] p = new double[3];
            for (int k = 0; k < 3; k++) {
                p[k] = root[k] + (curve[i][k] - root[k]) * grow;
            }
            // The sway: nothing at the root, the most at the point, slow and a little out of step along it.
            double sway = SEED_WIGGLE * t * t;
            p[0] += sway * Math.sin(phase * 1.7 + t * 2.1);
            p[1] += sway * 0.6 * Math.sin(phase * 1.3 + t * 1.4 + 1.1);
            p[2] += sway * 0.5 * Math.cos(phase * 1.1 + t * 1.8);
            out[i] = p;
        }
        return out;
    }

    /** The mastery its wing or wings are drawn at (and shoot from the tip of): a child's small, a teen's grown; by its stage alone. */
    public static int wingMastery(ChildStage stage) {
        return stage == ChildStage.TEEN ? 220 : 110;
    }

    /** One unit of a lash's power, or one shot: a share of its punch ({@link ChildRules#damage}), as strong as it was born. */
    public static float damage(ChildStage stage, int skill, float might) {
        return ChildRules.damage(stage, skill, true, might) * KAGUNE_SHARE;
    }

    /** How many lashes a string, or shots a volley, before it pauses: two, up to four the better it was taught. */
    public static int string(int skill) {
        return 2 + Math.max(0, Math.min(2, skill / 2));
    }

    /**
     * What it does with kagune {@code bits} against prey {@code distance} away that it {@code sees}: its tails within their reach
     * ({@code tailReach}, already at its size); its wing(s) beyond it (or always, with no tails); closer if it can't reach at all.
     */
    public static Use use(int bits, double distance, boolean sees, double tailReach) {
        boolean tails = KaguneGenes.tails(bits);
        boolean wings = KaguneGenes.wings(bits) > 0;
        if (!tails && !wings) {
            return Use.NONE;
        }
        if (tails && distance <= tailReach - 0.3 && sees) {
            return Use.TAILS;
        }
        if (wings && sees && distance <= SHOT_RANGE) {
            return tails && distance <= tailReach + 1.5 ? Use.CLOSE_IN : Use.WINGS;
        }
        return Use.CLOSE_IN;
    }

    /** The wing its next shot leaves from (1 its right, -1 its left): its one wing's side, or turn about with the pair ({@code last}). */
    public static int side(int bits, long seed, int last) {
        if (KaguneGenes.wings(bits) == 1) {
            return KaguneGenes.side(seed);
        }
        return last >= 0 ? -1 : 1;
    }

    /** Its pause after a string or volley ({@code roll} in 0..1). */
    public static int pause(double roll) {
        return PAUSE_LEAST + (int)Math.round(Math.max(0.0, Math.min(1.0, roll)) * (PAUSE_MOST - PAUSE_LEAST));
    }
}
