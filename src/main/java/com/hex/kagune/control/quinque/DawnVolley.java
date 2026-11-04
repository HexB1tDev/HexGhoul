package com.hex.kagune.control.quinque;

/**
 * 2.0.48 (pure, tested; the owner: "Arima should be able to shoot, like ... 5 at once in front of him. Like, all emerging from the same
 * point and then separating into, like, a circle"): the White Reaper's volley from Dawn's point. {@link #COUNT} lightning balls leave
 * its point together, open out into a ring about the line he aimed along ({@link #RADIUS} across, {@link #RADIUS_UP} up and down, one at the top and the rest evenly
 * round, {@link #OPEN} of the way there), then close in on the point he aimed at from every side at once (a pincer: standing still is
 * caught, a step well aside is not), crossing there and spreading again past it. Each carries {@link #SHARE} of a single shot.
 *
 * <p>Both the server (where they fly and hit) and every client (where they are drawn) follow {@link #at}, from the same point, way and
 * aimed-at distance, so they agree without anything more sent. Points are world coordinates in blocks.
 */
public final class DawnVolley {
    private DawnVolley() {
    }

    /** How many balls in a volley, how far out the ring opens (blocks), and by what share of the way to the aimed-at point. */
    public static final int COUNT = 5;
    public static final double RADIUS = 1.6;
    /** How far up and down it opens (blocks): less than across, so the lower balls keep off the ground from chest height. */
    public static final double RADIUS_UP = 1.0;
    public static final double OPEN = 0.35;
    /** Each ball's share of a single shot's damage (all five on one body: twice a shot). */
    public static final float SHARE = 0.4F;
    /** The nearest a volley's aimed-at point is taken to be (blocks): closer, the ring has no room to open. */
    public static final double NEAREST = 5.0;
    /** Past the aimed-at point the balls spread again, to this share of the ring. */
    public static final double AFTER = 0.6;

    /** How far out of the ring (0..1, negative once they have crossed) a ball is at {@code u} of the way to the aimed-at point. */
    public static double spread(double u) {
        if (!(u > 0.0)) {
            return 0.0;
        }
        if (u < OPEN) {
            return smooth(u / OPEN);
        }
        if (u < 1.0) {
            return 1.0 - smooth((u - OPEN) / (1.0 - OPEN));
        }
        return -AFTER * smooth((u - 1.0) / OPEN);
    }

    /** Ball {@code index}'s angle round the ring (radians): the first at the top, the rest evenly round. */
    public static double angle(int index) {
        return Math.PI / 2.0 + 2.0 * Math.PI * Math.floorMod(index, COUNT) / COUNT;
    }

    /**
     * Where ball {@code index} is once it has gone {@code travelled} blocks along {@code way} (unit) from {@code from}, the aimed-at point
     * {@code aimed} blocks along (at least {@link #NEAREST}).
     */
    public static double[] at(double[] from, double[] way, int index, double aimed, double travelled) {
        double[][] basis = basis(way);
        double u = Math.max(0.0, travelled) / Math.max(NEAREST, aimed);
        double open = spread(u);
        double a = angle(index);
        double c = Math.cos(a) * RADIUS * open;
        double s = Math.sin(a) * RADIUS_UP * open;
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            out[k] = from[k] + way[k] * travelled + basis[0][k] * c + basis[1][k] * s;
        }
        return out;
    }

    /** The ring's right and up about {@code way} (unit): right level, up square to both (straight up or down: east is right). */
    public static double[][] basis(double[] way) {
        double rx = -way[2];
        double rz = way[0];
        double l = Math.sqrt(rx * rx + rz * rz);
        double[] right = l < 1.0E-6 ? new double[] {1.0, 0.0, 0.0} : new double[] {rx / l, 0.0, rz / l};
        double[] up = {right[1] * way[2] - right[2] * way[1], right[2] * way[0] - right[0] * way[2], right[0] * way[1] - right[1] * way[0]};
        double lu = Math.sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2]);
        if (lu > 1.0E-9) {
            up = new double[] {up[0] / lu, up[1] / lu, up[2] / lu};
        }
        if (up[1] < 0.0) {
            up = new double[] {-up[0], -up[1], -up[2]};
        }
        return new double[][] {right, up};
    }

    static double smooth(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }
}
