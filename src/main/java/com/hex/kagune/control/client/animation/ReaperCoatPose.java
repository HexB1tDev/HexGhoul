package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.AgentRules;

/**
 * 4.1.37 (the owner: "when he takes off his jacket, I feel like he should actually like take off an actual jacket, before switching to the
 * other skin"; then: "You should take off an actual jacket. Like, you can use the armor that we got ... just make sure he takes it off, bro.
 * And you can see him taking it off"): the White Reaper's coat as a thing of its own while he sheds it (pure). It is the Formal Overcoat's
 * model (its body, its sleeves, its tails: client.render.ReaperCoatLayer draws them, white), worn exactly where his parts are at first
 * (his look turns bare under it at {@link AgentRules#REAPER_SHED_SWAP}, hidden); as he pulls it open and sweeps his arms back it goes back
 * off his shoulders ({@link #PEEL}) and its sleeves slide down his arms and off his hands ({@link #SLEEVES}); free of him ({@link #OFF}) it
 * drops behind him and falls flat, empty, on its back on the ground ({@link #LANDS}), lies there while he draws his blade, and sinks away
 * before his change ends ({@link #SINKS}).
 *
 * <p>Each piece's frame is {M, T}: a point of the piece (its bone's frame, sixteenths, y down, as {@code OutfitModels} gives its corners)
 * is {@code T + M p} in the model's frame (sixteenths, y down, z to his back). Once it is off, the frame is his model's at {@link #OFF}
 * (the renderer keeps it where it fell, whichever way he turns after).
 */
public final class ReaperCoatPose {
    private ReaperCoatPose() {
    }

    /** The pieces: the coat's body, its right and left sleeves, its right and left tails. */
    public static final int BODY = 0;
    public static final int RIGHT_SLEEVE = 1;
    public static final int LEFT_SLEEVE = 2;
    public static final int RIGHT_TAIL = 3;
    public static final int LEFT_TAIL = 4;
    public static final int PIECES = 5;

    /** Ticks into his shed: back off his shoulders from, its sleeves down his arms from, free of him, flat on the ground. */
    public static final double PEEL = 14.0;
    public static final double SLEEVES = 17.0;
    public static final double OFF = 26.5;
    public static final double LANDS = OFF + 7.0;
    /** Ticks into his whole change (jacket and draw) it sinks away over, ending as the change ends. */
    public static final double SINKS = 12.0;
    /** How flat the empty coat lies (its depth's share). */
    public static final double FLAT = 0.3;
    /** Where it lies: how far behind his heels its collar is (sixteenths). */
    public static final double BEHIND = 9.0;

    /** Each piece's bone ({@code OutfitModels}) and the slot its cubes are worn in: the overcoat's body and sleeves, the trousers' tails. */
    public static final int[] BONE = {1, 2, 3, 8, 9};
    public static final int[] SLOT = {1, 1, 1, 2, 2};

    /** {@code piece}'s frame {M (3 x 3), T} in the model's frame {@code age} ticks into his shed, his model on the ground at {@code ground}. */
    public static double[][][] frame(int piece, double age, double ground) {
        if (age < OFF) {
            return peeled(piece, AgentPose.shed(age), age);
        }
        double[][][] from = peeled(piece, AgentPose.shed(OFF), OFF);
        double[][][] to = lying(piece, ground);
        double u = Math.max(0.0, Math.min(1.0, (age - OFF) / (LANDS - OFF)));
        // Across at an even pace, down faster and faster (it falls); turned over as it goes; flattened as it lands (nothing in it).
        double[] a0 = from[1][0];
        double[] b0 = to[1][0];
        double[] t = {a0[0] + (b0[0] - a0[0]) * u, a0[1] + (b0[1] - a0[1]) * u * u, a0[2] + (b0[2] - a0[2]) * u};
        float[] a = QuinqueGrip.angles(from[0]);
        float[] b = QuinqueGrip.angles(rotation(to[0]));
        float[] m = QuinqueGrip.blendTurn(a[0], a[1], a[2], b[0], b[1], b[2], (float)ReaperBladePose.smooth(u));
        double flat = 1.0 - (1.0 - FLAT) * ReaperBladePose.smooth((u - 0.6) / 0.4);
        double[][] turn = squash(RigPose.zyx(m[0], m[1], m[2]), flat);
        double sinking = sunk(age);
        return new double[][][] {turn, {{t[0], t[1] + sinking, t[2]}}};
    }

    /** How far it has sunk into the ground ({@code age} ticks into his shed): from {@link #SINKS} before his change ends, out of sight by then. */
    public static double sunk(double age) {
        double from = AgentRules.REAPER_CHANGE_TICKS - SINKS;
        return Math.max(0.0, age - from) * 0.6;
    }

    /** Whether the coat is drawn at all {@code age} ticks into his change (from the start until it has sunk). */
    public static boolean drawn(double age) {
        return age >= 0.0 && age < AgentRules.REAPER_CHANGE_TICKS;
    }

    /** Its depth (the piece's own z) flattened to {@code flat}: {@code m} times diag(1, 1, flat). */
    static double[][] squash(double[][] m, double flat) {
        return new double[][] {{m[0][0], m[0][1], m[0][2] * flat}, {m[1][0], m[1][1], m[1][2] * flat}, {m[2][0], m[2][1], m[2][2] * flat}};
    }

    /** {@code m} with its depth's flattening undone (its columns made unit again). */
    static double[][] rotation(double[][] m) {
        double[][] r = new double[3][3];
        for (int c = 0; c < 3; c++) {
            double l = Math.sqrt(m[0][c] * m[0][c] + m[1][c] * m[1][c] + m[2][c] * m[2][c]);
            for (int k = 0; k < 3; k++) {
                r[k][c] = m[k][c] / l;
            }
        }
        return r;
    }

    /** The part {@code name} of his model posed as {@code p} has it (AgentModel's own: PlayerAnimator's transform from vanilla's place). */
    static double[][][] part(RigPose p, String name, float x, float y, float z) {
        float[] at = p.transform(name, RigPose.POSITION, x, y, z);
        float[] turn = p.transform(name, RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        double out = name.equals("rightArm") ? -com.hex.kagune.control.npc.ReaperBuild.ARM_OUT
            : name.equals("leftArm") ? com.hex.kagune.control.npc.ReaperBuild.ARM_OUT : 0.0;
        return new double[][][] {RigPose.zyx(turn[0], turn[1], turn[2]), {{at[0] + out, at[1], at[2]}}};
    }

    /** {@code piece} on him ({@code p}, {@code age} ticks in), worn or on its way off. */
    static double[][][] peeled(int piece, RigPose p, double age) {
        double back = ReaperBladePose.smooth((age - PEEL) / (OFF - PEEL));
        double down = ReaperBladePose.smooth((age - SLEEVES) / (OFF - SLEEVES));
        switch (piece) {
            case BODY -> {
                // Off his shoulders: tipped back about its hem (its collar goes back and down first), and the whole of it back and down.
                double[][][] torso = part(p, "torso", 0.0F, 0.0F, 0.0F);
                double[][] tip = RigPose.zyx(-0.75 * back, 0.0, 0.0);
                double[] hem = {0.0, 12.0, 0.0};
                double[] shift = {0.0, 6.0 * back, 4.5 * back};
                // p' = torso.M (tip (p - hem) + hem + shift) + torso.T  =  (torso.M tip) p + torso.M (hem - tip hem + shift) + torso.T
                double[][] m = RigPose.multiply(torso[0], tip);
                double[] local = add(sub(hem, apply(tip, hem)), shift);
                return new double[][][] {m, {add(torso[1][0], apply(torso[0], local))}};
            }
            case RIGHT_SLEEVE, LEFT_SLEEVE -> {
                // Down his arm towards his hand and off it.
                boolean right = piece == RIGHT_SLEEVE;
                double[][][] arm = part(p, right ? "rightArm" : "leftArm", right ? -5.0F : 5.0F, 2.0F, 0.0F);
                return new double[][][] {arm[0], {add(arm[1][0], apply(arm[0], new double[] {0.0, 13.0 * down, 0.0}))}};
            }
            default -> {
                // The tails go with the coat's hem: back and down with it, swung back from the hip as it goes (never into the ground).
                boolean right = piece == RIGHT_TAIL;
                double[][][] leg = part(p, right ? "rightLeg" : "leftLeg", right ? -1.9F : 1.9F, 12.0F, 0.0F);
                double[][] swing = RigPose.zyx(1.15 * back, 0.0, 0.0);
                return new double[][][] {RigPose.multiply(swing, leg[0]), {add(leg[1][0], new double[] {0.0, 6.0 * back, 4.5 * back})}};
            }
        }
    }

    /**
     * {@code piece} lying on the ground ({@code ground}: the model's y of it) behind him: the coat on its back, collar towards his heels, hem
     * away; its sleeves out to each side and down along it; its tails on from its hem; all of it flattened ({@link #FLAT}).
     */
    static double[][][] lying(int piece, double ground) {
        // Its own across to his right (x -), its own down along the ground away from him (z +), its own back down onto the ground (y +).
        double[][] body = {{-1.0, 0.0, 0.0}, {0.0, 0.0, 1.0}, {0.0, 1.0, 0.0}};
        double[] at = {0.0, ground - 2.6 * FLAT - 0.05, BEHIND};
        switch (piece) {
            case BODY -> {
                return new double[][][] {squash(body, FLAT), {at}};
            }
            case RIGHT_SLEEVE, LEFT_SLEEVE -> {
                boolean right = piece == RIGHT_SLEEVE;
                double[] shoulder = add(at, apply(body, new double[] {right ? -5.0 : 5.0, 2.0, 0.0}));
                double side = right ? 1.0 : -1.0; // its right sleeve lies on its right: his left, it being on its back
                double[] along = {0.72 * side, 0.0, 0.69};
                double[] flat = {0.0, 1.0, 0.0};
                double[] across = cross(along, flat);
                double[][] m = {{across[0], along[0], flat[0]}, {across[1], along[1], flat[1]}, {across[2], along[2], flat[2]}};
                return new double[][][] {squash(m, FLAT), {{shoulder[0], ground - 2.6 * FLAT - 0.05, shoulder[2]}}};
            }
            default -> {
                boolean right = piece == RIGHT_TAIL;
                double[] hip = add(at, apply(body, new double[] {right ? -1.9 : 1.9, 12.0, 0.0}));
                return new double[][][] {squash(body, FLAT), {hip}};
            }
        }
    }

    static double[] apply(double[][] m, double[] v) {
        return new double[] {m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /** A point {@code p} of a piece placed by {@code frame} (the model's frame, sixteenths). */
    public static double[] place(double[][][] frame, double[] p) {
        return add(frame[1][0], apply(frame[0], p));
    }
}
