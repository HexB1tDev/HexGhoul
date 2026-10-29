package com.hex.kagune.control.client.animation;

/**
 * 6.2.0: her own right arm as she sees it in first person (pure, tested), while the Mutated Arm has it pointed out ahead of her and
 * the flesh is bursting out of her hand; and both her arms through her strikes and the Big Punch ({@link #both}). In first person
 * the player model is never drawn and vanilla's hand is held low at the side of the view, nowhere near where the flesh comes out;
 * this is the arm posed exactly as the model draws it ({@link BodyFrame}), its skin's arm and sleeve layers mapped on as the game
 * maps them, seen as {@link EtoOwnLeg} sees her leg.
 */
public final class EtoOwnArm {
    /** The rows the arm is cut into along its length (pixels from the shoulder pivot): the elbow bends at the middle. */
    static final double[] ROWS = {-2.0, 1.0, 4.0, 7.0, 10.0};
    /** Where her right arm and its sleeve are on the skin (texture pixels). */
    static final double[] ARM_UV = {40.0, 16.0};
    static final double[] SLEEVE_UV = {40.0, 32.0};
    /** 6.2.0: where her left arm and its sleeve are (a 64x64 skin has its own left arm; older skins are converted to it on load). */
    static final double[] LEFT_ARM_UV = {32.0, 48.0};
    static final double[] LEFT_SLEEVE_UV = {48.0, 48.0};

    private EtoOwnArm() {
    }

    /** Her right arm on {@code body}, moved by {@code shift} (blocks), {@code slim} (three pixels wide) or not: the arm, then its sleeve. */
    public static void mesh(RigPose body, double[] shift, boolean slim, EtoOwnLeg.Sink sink) {
        double outer = slim ? -2.0 : -3.0;
        EtoOwnLeg.limb(body, "rightArm", shift, outer, 1.0, ROWS, 0.0, ARM_UV[0], ARM_UV[1], false, sink);
        EtoOwnLeg.limb(body, "rightArm", shift, outer, 1.0, ROWS, EtoOwnLeg.OVERLAY, SLEEVE_UV[0], SLEEVE_UV[1], true, sink);
    }

    /** 6.2.0: her left arm on {@code body}, as {@link #mesh} has her right (the model's box mirrored across: her outer side is to her left). */
    public static void left(RigPose body, double[] shift, boolean slim, EtoOwnLeg.Sink sink) {
        double outer = slim ? 2.0 : 3.0;
        EtoOwnLeg.limb(body, "leftArm", shift, -1.0, outer, ROWS, 0.0, LEFT_ARM_UV[0], LEFT_ARM_UV[1], false, sink);
        EtoOwnLeg.limb(body, "leftArm", shift, -1.0, outer, ROWS, EtoOwnLeg.OVERLAY, LEFT_SLEEVE_UV[0], LEFT_SLEEVE_UV[1], true, sink);
    }

    /** 6.2.0: both her arms (her Speed Blitz and Big Punch in first person, posed as third person draws them). */
    public static void both(RigPose body, double[] shift, boolean slim, EtoOwnLeg.Sink sink) {
        mesh(body, shift, slim, sink);
        left(body, shift, slim, sink);
    }
}
