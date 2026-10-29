package com.hex.kagune.control.client.animation;

/**
 * 1.0.15: where arms posed for third person are drawn in first person (pure, tested). Posed as the model is, a blow lands out to the
 * side of and below the eyes, outside the view, so it is never seen: in first person each point of the arms is drawn pulled in toward
 * the line of the eyes and a little ahead, and turned with the look up or down (as vanilla's own hand is), so what the arms do is in
 * front of the player. In rig space (blocks): x across, y up, z ahead. 1.0.33: the shoulders were right against the eyes, so the arms
 * filled the view as blocks and a punch, reaching barely a third of a block ahead of them, never got anywhere: now they are carried
 * well ahead and a little up, and every move along the look made {@link #REACH} times longer, so a punch drives out into the middle
 * of the view and a wind-up pulls back out of it, as the arms do seen from outside.
 */
public final class FirstPersonArms {
    /** How much of the way across to the line of the eyes, how far ahead, how far up, how much longer along the look, and how much of the look's pitch. */
    static final double INWARD = 0.35;
    static final double AHEAD = 0.85;
    static final double UP = 0.1;
    static final double REACH = 1.6;
    static final double PITCH = 0.85;

    private FirstPersonArms() {
    }

    /** {@code rig} (a point of the arms, seen from {@code eye}) as first person draws it, the look {@code pitch} radians down. */
    public static double[] adjust(double[] rig, double[] eye, double pitch) {
        double x = (rig[0] - eye[0]) * (1.0 - INWARD);
        double y = rig[1] - eye[1] + UP;
        double z = (rig[2] - eye[2]) * REACH + AHEAD;
        double a = pitch * PITCH;
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[] {eye[0] + x, eye[1] + y * c - z * s, eye[2] + y * s + z * c};
    }

    /** A direction (a face's normal) turned as {@link #adjust} turns the points. */
    public static double[] turn(double[] n, double pitch) {
        double a = pitch * PITCH;
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[] {n[0], n[1] * c - n[2] * s, n[1] * s + n[2] * c};
    }
}
