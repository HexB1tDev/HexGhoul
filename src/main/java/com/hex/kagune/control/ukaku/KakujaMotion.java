package com.hex.kagune.control.ukaku;

/**
 * The life in the kakuja's tentacles (pure, tested): a spring chain per tentacle, like the Rinkaku path's tendrils, run on
 * the client in the world. Each tentacle's root is fixed to his back; every point out along it chases where the designed
 * pose wants it relative to the point before it, on a spring that loosens toward the claw and is a little under-damped.
 * So a step of the body runs out along each tentacle as a wave: they trail behind as he runs, swing round as he turns,
 * sag on the way up a jump and whip up and over at the top, overshoot and settle when he stops. The lag is handed back to
 * {@link KakujaRig} as offsets.
 */
public final class KakujaMotion {
    /** Angular frequency of the springs at the root and at the claw (radians a tick). */
    static final double OMEGA_ROOT = 0.78;
    static final double OMEGA_TIP = 0.34;
    /** Damping ratio: under 1, so they overshoot a little and settle (alive, not rubbery, not stiff). */
    static final double DAMPING = 0.42;
    /** How much each point is pulled toward its place in the pose itself, not just relative to the point before (no drift). */
    static final double ANCHOR = 0.22;
    /** How much of a segment's stretch the chain takes back each substep (it swings rather than stretches). */
    static final double STIFFNESS = 0.6;
    /** Longest lag of a point behind the pose, per block of tentacle out to it. */
    static final double MOST_LAG = 0.6;
    /** A body that moves further than this in one frame was teleported: start the chain again from the pose. */
    static final double TELEPORT = 6.0;
    /** Simulation substep (ticks) and the longest frame simulated (a hitch doesn't fling them). */
    static final double SUBSTEP = 0.5;
    static final double MOST_FRAME = 4.0;
    /** Ticks without a step after which the chains start again from the pose. */
    static final double STALE = 20.0;

    private double[][][] position;
    private double[][][] velocity;
    private double lastTime = Double.NaN;

    /**
     * Steps the chains to {@code time} (ticks) toward {@code target} (world space: per tentacle the root and each node, as
     * {@link KakujaRig#nodes}; a tentacle that is not out is null) and returns each point's lag behind it (world space, the
     * root's always zero).
     */
    public double[][][] step(double[][][] target, double time) {
        int count = target.length;
        double[][][] offsets = new double[count][][];
        // Not drawn for a while (out of sight, or the kakuja just came back): start again from the pose, not from stale points.
        boolean fresh = this.position == null || this.position.length != count || Double.isNaN(this.lastTime) || time < this.lastTime
            || time - this.lastTime > STALE;
        double dt = fresh ? 0.0 : Math.min(MOST_FRAME, time - this.lastTime);
        this.lastTime = time;
        if (fresh) {
            this.position = new double[count][][];
            this.velocity = new double[count][][];
        }
        for (int limb = 0; limb < count; limb++) {
            double[][] goal = target[limb];
            if (goal == null) {
                this.position[limb] = null;
                continue;
            }
            double[][] p = this.position[limb];
            if (p == null || p.length != goal.length || distance(p[0], goal[0]) > TELEPORT) {
                p = copy(goal);
                this.position[limb] = p;
                this.velocity[limb] = new double[goal.length][3];
            }
            simulate(p, this.velocity[limb], goal, dt);
            offsets[limb] = new double[goal.length][];
            for (int k = 0; k < goal.length; k++) {
                offsets[limb][k] = sub(p[k], goal[k]);
            }
        }
        return offsets;
    }

    /** Forget the chains (the next step starts again from the pose). */
    public void reset() {
        this.position = null;
        this.velocity = null;
        this.lastTime = Double.NaN;
    }

    static void simulate(double[][] p, double[][] v, double[][] goal, double dt) {
        int nodes = goal.length - 1;
        double[] segment = new double[goal.length];
        double reach = 0.0;
        double[] reachAt = new double[goal.length];
        for (int k = 1; k <= nodes; k++) {
            segment[k] = distance(goal[k], goal[k - 1]);
            reach += segment[k];
            reachAt[k] = reach;
        }
        int steps = dt <= 0.0 ? 0 : Math.max(1, (int)Math.ceil(dt / SUBSTEP));
        double h = steps == 0 ? 0.0 : dt / steps;
        for (int step = 0; step < steps; step++) {
            // The root is part of his back.
            p[0] = goal[0].clone();
            v[0] = new double[3];
            for (int k = 1; k <= nodes; k++) {
                double s = k / (double)nodes;
                double omega = OMEGA_ROOT + (OMEGA_TIP - OMEGA_ROOT) * s;
                // Where the pose wants it relative to the point before it, and a little where the pose itself has it.
                double[] relative = add(p[k - 1], sub(goal[k], goal[k - 1]));
                double[] want = add(mul(relative, 1.0 - ANCHOR), mul(goal[k], ANCHOR));
                double[] pull = mul(sub(want, p[k]), omega * omega);
                double[] drag = mul(v[k], 2.0 * DAMPING * omega);
                v[k] = add(v[k], mul(sub(pull, drag), h));
                p[k] = add(p[k], mul(v[k], h));
                // Keep the segment its length (softly): the tentacle swings, it doesn't stretch.
                double[] d = sub(p[k], p[k - 1]);
                double length = Math.sqrt(dot(d, d));
                if (length > 1.0E-6 && segment[k] > 1.0E-6) {
                    double fix = (segment[k] / length - 1.0) * STIFFNESS;
                    p[k] = add(p[k], mul(d, fix));
                }
            }
        }
        // Never further behind than a living limb would bend.
        for (int k = 1; k <= nodes; k++) {
            double[] lag = sub(p[k], goal[k]);
            double most = MOST_LAG * reachAt[k];
            double length = Math.sqrt(dot(lag, lag));
            if (length > most && length > 1.0E-9) {
                p[k] = add(goal[k], mul(lag, most / length));
                double[] dir = mul(lag, 1.0 / length);
                double out = dot(v[k], dir);
                if (out > 0.0) {
                    v[k] = sub(v[k], mul(dir, out));
                }
            }
            if (!Double.isFinite(p[k][0]) || !Double.isFinite(p[k][1]) || !Double.isFinite(p[k][2])) {
                p[k] = goal[k].clone();
                v[k] = new double[3];
            }
        }
    }

    static double[][] copy(double[][] points) {
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            out[i] = points[i].clone();
        }
        return out;
    }

    static double distance(double[] a, double[] b) {
        double[] d = sub(a, b);
        return Math.sqrt(dot(d, d));
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}
