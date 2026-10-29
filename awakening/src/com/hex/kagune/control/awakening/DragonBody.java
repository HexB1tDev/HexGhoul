package com.hex.kagune.control.awakening;

/**
 * Frame of the Dragon's body at a segment, and where heads and tentacles sprout from it. The server
 * rig and the client pose both call these, so what is drawn grows from exactly where it strikes.
 *
 * <p>The side vector comes from the horizontal direction of travel, never from the 3D forward
 * crossed with up, so it cannot degenerate or flip when part of the body rears up steeply.
 */
public final class DragonBody {
    /** How far the spine rides above the ground, as a fraction of the local radius; the rest is buried. */
    public static final double RIDE = 0.62;
    private static final V3 FALLBACK = new V3(1.0, 0.0, 0.0);

    private DragonBody() {
    }

    /** Direction of travel at a segment, towards the head. */
    public static V3 forward(V3[] segments, int index) {
        int last = segments.length - 1;
        V3 forward = segments[Math.max(0, index - 1)].sub(segments[Math.min(last, index + 1)]).normalize();
        return forward.length() < 1.0E-6 ? flatForward(segments, index) : forward;
    }

    /** Horizontal direction of travel; widens the window until the segments are apart. */
    public static V3 flatForward(V3[] segments, int index) {
        int last = segments.length - 1;
        for (int span = 1; span <= 16; span *= 2) {
            V3 d = segments[Math.max(0, index - span)].sub(segments[Math.min(last, index + span)]).withY(0.0);
            if (d.horizontalLength() > 1.0E-3) return d.normalize();
        }
        return FALLBACK;
    }

    /** Horizontal unit vector to the body's right-hand flank (positive side). */
    public static V3 side(V3[] segments, int index) {
        V3 f = flatForward(segments, index);
        return new V3(-f.z(), 0.0, f.x());
    }

    /** Back of the body: perpendicular to the forward direction, tilting with it as the body rears. */
    public static V3 up(V3[] segments, int index) {
        V3 up = side(segments, index).cross(forward(segments, index)).normalize();
        return up.length() < 1.0E-6 ? V3.UP : up;
    }

    /** +1 or -1: which flank a side head grows from. Head 0 is the front of the body. */
    public static double headSide(int head) {
        return head % 2 == 0 ? 1.0 : -1.0;
    }

    public static double tentacleSide(int tentacle) {
        return tentacle % 2 == 0 ? -1.0 : 1.0;
    }

    /** Head 0 is the tip of the body; the others grow from the upper flanks. */
    public static V3 headRoot(V3[] segments, double[] radii, int head) {
        if (head == 0) return segments[0];
        return flankRoot(segments, radii, DragonRig.HEAD_ROOTS[head], headSide(head));
    }

    public static V3 tentacleRoot(V3[] segments, double[] radii, int tentacle) {
        return flankRoot(segments, radii, DragonRig.TENTACLE_ROOTS[tentacle], tentacleSide(tentacle));
    }

    private static V3 flankRoot(V3[] segments, double[] radii, int root, double sign) {
        double r = radii[root];
        return segments[root].add(up(segments, root).scale(r * 0.5)).add(side(segments, root).scale(sign * r * 0.55));
    }
}
