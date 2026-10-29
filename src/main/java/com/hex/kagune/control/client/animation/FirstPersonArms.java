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

    /** 1.2.4: no shape of War's rinkaku with a corner closer to the eyes than this (blocks): only what reaches out ahead is seen. */
    public static final double RINKAKU_CLEAR = 1.25;

    private FirstPersonArms() {
    }

    /** 1.2.4: whether every corner of a shape is at least {@code clearance} from {@code eye} (all in blocks). */
    public static boolean clear(double[] eye, double clearance, double[]... corners) {
        for (double[] c : corners) {
            double dx = c[0] - eye[0];
            double dy = c[1] - eye[1];
            double dz = c[2] - eye[2];
            if (!(dx * dx + dy * dy + dz * dz >= clearance * clearance)) {
                return false;
            }
        }
        return true;
    }

    /** 1.2.4: how many ticks before a hand's blow lands vanilla's swing starts, to reach its full stretch as the blow lands. */
    public static final int SWING_LEAD = 2;

    /**
     * 1.2.4: when War's own hand swings in first person through {@code move} at {@code level} (ticks after it began), or -1 for a move
     * none of his hands throws: each M1 blow of a fist or blade (not the kicks), the hand thrown out to grasp, Black Sun's punch. His
     * arms, posed as third person poses them and pulled in before the eyes, filled the view; vanilla's own hand, his plates on it, swings
     * instead.
     */
    public static int swingAt(com.hex.kagune.control.king.KingMove move, int level) {
        if (move == com.hex.kagune.control.king.KingMove.STRIKE) {
            com.hex.kagune.control.king.WarStrike blow = com.hex.kagune.control.king.WarStrike.byOrdinal(level);
            return blow.kind == com.hex.kagune.control.king.WarStrike.Kind.KICK ? -1 : Math.max(0, blow.impact - SWING_LEAD);
        }
        if (move == com.hex.kagune.control.king.KingMove.GRASP) {
            return 0;
        }
        if (move == com.hex.kagune.control.king.KingMove.SUNDER) {
            return Math.max(0, com.hex.kagune.control.king.WarRules.SUNDER_PUNCH - SWING_LEAD);
        }
        return -1;
    }

    /**
     * 1.2.4: what to add (rig space, blocks) to anything drawn on {@code body} so its neck is where a standing body's is: as the
     * camera, which never leans or bobs with the pose. Taken at the neck, not the eyes ({@code EtoOwnLeg#shift}), so the head's own
     * turns (War's twitching) do not shake everything else in view.
     */
    public static double[] neckShift(RigPose body) {
        double[] neck = {0.0, 0.0, 0.0};
        double[] standing = BodyFrame.world(new RigPose(), BodyFrame.model(new RigPose(), "head", neck));
        double[] posed = BodyFrame.world(body, BodyFrame.model(body, "head", neck));
        return new double[] {standing[0] - posed[0], standing[1] - posed[1], standing[2] - posed[2]};
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
