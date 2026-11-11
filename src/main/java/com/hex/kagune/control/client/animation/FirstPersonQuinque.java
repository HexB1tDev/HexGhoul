package com.hex.kagune.control.client.animation;

/**
 * 4.1.19 (the owner: "don't forget the the first person animation, like first person view to match the animation that they got, because
 * they currently don't have that"): where first person draws a melee quinque (pure, tested). The weapon is placed exactly as third person
 * places it on the posed body ({@link QuinqueGrip#held}: the posed right arm, ItemInHandLayer, the item's own transform), its body's step
 * in (the lunge and the drop) carrying it, the body's whole turn left out (the camera never spins); then seen from {@link #EYE}, a point
 * behind and a little above and left of the head, looking {@link #PITCH} down, the view zoomed {@link #ZOOM} about its middle. So every cut
 * crosses the screen the way the body swings it, and lands in front of the crosshair.
 *
 * <p>Found on renders (tools/FirstPersonPreview.java). Tried and dropped: the true eye (a katana in chudan points along the view, the cuts
 * land below the screen: the hands work a block under the eyes); the eye twelve pixels back with no look down (the cuts land on the bottom
 * edge, the hilt swells into the view on the follow through); the grip's path squeezed into view with the blade's angle kept (lifting the
 * hands turns the hilt onto the eye: a rigid view never does that); the posed arms drawn as well (from any point behind the shoulders they
 * wall off the view; vanilla draws no arm under a held sword either).
 */
public final class FirstPersonQuinque {
    private FirstPersonQuinque() {
    }

    /** Where the view is taken from: model pixels from the neck (x to the left, y down, z back). */
    public static final float[] EYE = {4.5F, -1.0F, 24.0F};
    /** How far the view looks down (radians): the hands work below the eyes, so the look is tipped toward them. */
    public static final float PITCH = (float)Math.toRadians(9.0);
    /** The view zoomed about its middle (camera x and y, not depth): the weapon seen from behind the head at the size of one held. */
    public static final float ZOOM = 1.7F;
    /** The player model's scale (LivingEntityRenderer: 0.9375 for a player). */
    public static final float MODEL = 0.9375F;
    /** The hand's view in first person (GameRenderer.renderItemInHand: 70 degrees, whatever the FOV setting), and a 16:9 screen. */
    public static final double FOV = Math.toRadians(70.0);
    public static final double ASPECT = 16.0 / 9.0;

    /** {@code p} as first person poses the arm: its whole turn left out, every hold kept (RigPose.add leaves the right arm's hold out). */
    public static RigPose still(RigPose p) {
        RigPose s = new RigPose().add(p, 1.0F);
        s.spin = 0.0F;
        s.rightArmHold = p.rightArmHold;
        return s;
    }

    /**
     * A point of the posed model ({@code model}: pixels, as {@link QuinqueGrip#held} gives it for {@link #still}) in the camera's frame
     * (blocks: x right, y up, z back, the camera at 0 looking down -z), carried by {@code body}'s step as PlayerRendererMixin carries
     * the model (its drop and lunge, in blocks).
     */
    public static double[] view(RigPose body, double[] model) {
        double s = MODEL / 16.0;
        double x = -(model[0] - EYE[0]) * s;
        double y = -(model[1] - EYE[1]) * s - body.drop / 16.0;
        double z = (model[2] - EYE[2]) * s - body.lunge / 16.0;
        double c = Math.cos(PITCH);
        double n = Math.sin(PITCH);
        return new double[] {x * ZOOM, (y * c - z * n) * ZOOM, y * n + z * c};
    }

    /** A point of the held quinque's own model (its pixels) as first person draws it for {@code body}. */
    public static double[] held(RigPose body, QuinqueHolds.Hold h, boolean slim, float[] point) {
        return view(body, QuinqueGrip.held(still(body), h, slim, point));
    }

    /** Whether a camera point is on the screen (in front, inside the hand's view). */
    public static boolean seen(double[] c) {
        if (!(c[2] < -0.05)) {
            return false;
        }
        double half = Math.tan(FOV / 2.0);
        return Math.abs(c[1] / -c[2]) <= half && Math.abs(c[0] / -c[2]) <= half * ASPECT;
    }
}
