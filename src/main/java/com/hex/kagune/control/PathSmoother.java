package com.hex.kagune.control;

/**
 * Rinkaku's tentacles drawn smooth and controlled (3.1.0, pure, tested). The original simulates each tentacle (inertia, reach,
 * constraint passes, block collisions, pushes out of his body) and draws the result as it comes, so every correction shows as
 * a jolt and every swing wobbles on. This follows the simulated path with critically damped springs per point, in the player's
 * own frame (root at the origin, x to his right, z forward): his walking and turning carry the tentacles exactly as before, only
 * their own motion is smoothed: quick when they strike, slow and calm at rest. Each step is solved exactly, so the frame rate
 * never changes the motion.
 */
public final class PathSmoother {
    /** Ticks the calm shape takes to follow where the tentacle rests (walking, looking up or down). */
    static final double MEAN_TICKS = 40.0;
    /** Ticks to change between keeping the whole motion (a strike) and the calmed one (at rest). */
    static final double KEEP_TICKS = 4.0;
    private double[][] p;
    private double[][] v;
    private double[][] mean;
    private double keep = 1.0;
    private double last = Double.NaN;

    public double[][] follow(double[][] target, double time, double response) {
        return this.follow(target, time, response, 1.0);
    }

    /**
     * The smoothed path for {@code target} (local frame, root first) at {@code time} (ticks), closing most of the way in about
     * {@code response} ticks. {@code wiggle} below 1 calms the motion at rest: the springs follow a slow average of the shape
     * plus only that share of the original's sway round it (the average holds still while {@code wiggle} is 1, in a strike, so
     * the tentacle glides back to where it rested instead of snapping). A new shape (another point count), a long gap or time
     * running backwards starts afresh.
     */
    public double[][] follow(double[][] target, double time, double response, double wiggle) {
        int n = target.length;
        if (this.p == null || this.p.length != n || Double.isNaN(this.last) || time < this.last - 1.0E-6 || time - this.last > 10.0) {
            this.p = new double[n][3];
            this.v = new double[n][3];
            this.mean = new double[n][3];
            for (int i = 0; i < n; i++) {
                System.arraycopy(target[i], 0, this.p[i], 0, 3);
                System.arraycopy(target[i], 0, this.mean[i], 0, 3);
            }
            this.keep = wiggle;
            this.last = time;
            return copy(this.p);
        }
        double dt = time - this.last;
        this.last = time;
        this.keep = wiggle + (this.keep - wiggle) * Math.exp(-dt / KEEP_TICKS);
        double drift = wiggle < 0.999 ? 1.0 - Math.exp(-dt / MEAN_TICKS) : 0.0;
        double[][] goal = new double[n][3];
        for (int i = 0; i < n; i++) {
            for (int axis = 0; axis < 3; axis++) {
                this.mean[i][axis] += (target[i][axis] - this.mean[i][axis]) * drift;
                goal[i][axis] = this.mean[i][axis] + this.keep * (target[i][axis] - this.mean[i][axis]);
            }
        }
        target = goal;
        double omega = 2.6 / Math.max(0.1, response);
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                System.arraycopy(target[0], 0, this.p[0], 0, 3);
                java.util.Arrays.fill(this.v[0], 0.0);
                continue;
            }
            // The base follows tighter than the tip, so the tentacle never swings loose at the root.
            double w = omega * (1.0 + 1.5 * (1.0 - i / (double)(n - 1)));
            for (int axis = 0; axis < 3; axis++) {
                double y = this.p[i][axis] - target[i][axis];
                double e = Math.exp(-w * dt);
                double temp = (this.v[i][axis] + w * y) * dt;
                this.p[i][axis] = target[i][axis] + (y + temp) * e;
                this.v[i][axis] = (this.v[i][axis] - w * temp) * e;
            }
        }
        return copy(this.p);
    }

    private static double[][] copy(double[][] points) {
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            out[i] = points[i].clone();
        }
        return out;
    }
}
