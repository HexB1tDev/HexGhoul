package com.hex.kagune.control.quinque;

/**
 * 2.0.31 (pure, tested): where Dawn's point passes through each of its three swings ({@code QuinquePose}'s Dawn keys), for the dark
 * ribbons it leaves (the owner's picture: the lance "swung with dark ribbons around it"). In the swinger's own frame from its feet:
 * {right, up, forward} in blocks. The first, a level sweep at chest height from out on the right round the front to the left; the
 * second, rising from low on the left to high on the right; the finisher, a straight thrust along its length.
 */
public final class DawnArc {
    private DawnArc() {
    }

    /** The part of a swing (its time, 0..1) the ribbon is laid along: the windup's end to the follow through's. */
    public static final double FROM = 0.3;
    public static final double TO = 0.68;

    /** How far along the ribbon's part of the swing {@code t} (0..1 of the swing) is: 0 before it, 1 after it. */
    public static double along(double t) {
        return Math.max(0.0, Math.min(1.0, (t - FROM) / (TO - FROM)));
    }

    /** Where the point is {@code u} (0..1) along the ribbon of its {@code step}th swing: {right, up, forward} from the feet. */
    public static double[] point(int step, double u) {
        double k = Math.max(0.0, Math.min(1.0, u));
        switch (Math.floorMod(step, 3)) {
            case 0: {
                double a = Math.toRadians(70.0 - 140.0 * k);
                return new double[] {Math.sin(a) * 2.7, 1.25, Math.cos(a) * 2.7};
            }
            case 1: {
                double a = Math.toRadians(-60.0 + 115.0 * k);
                return new double[] {Math.sin(a) * 2.5, 0.55 + 1.9 * k, Math.cos(a) * 2.5};
            }
            default:
                return new double[] {0.15, 1.35, 0.9 + 3.4 * k};
        }
    }
}
