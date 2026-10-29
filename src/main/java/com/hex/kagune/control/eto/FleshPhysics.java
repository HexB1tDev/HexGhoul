package com.hex.kagune.control.eto;

/**
 * 6.2.0: how a length of Eto's flesh carries its own weight (pure, tested). Every move gives where its flesh should be (a path
 * from its root to its tip); the flesh itself follows that path the way a heavy living limb would, not as a rigid thing set there
 * each frame: a {@link Chain} of points, the one at the root held where the root is, every other one pulled toward its place by a
 * spring and slowed by its own drag, looser and heavier toward the tip. So the thin flesh near the root moves first and bends
 * ahead, the heavy end lags behind it, keeps going when the root stops or turns, overshoots and swings back (a whip, follow
 * through, recoil); and since the chain lives in the world, when she moves the flesh trails after her. Its segments keep their
 * length (it bends, it does not stretch like rubber), and a tip that holds something can be held where that is.
 */
public final class FleshPhysics {
    /** The longest step (ticks) the springs take at once: short enough for a whip that crosses blocks in a tick. */
    static final double STEP = 0.125;
    /** The longest gap (ticks) between two frames that is carried through; longer (a stall, the game paused), it starts afresh. */
    static final double LONGEST = 3.0;
    /** How far (blocks) the root may jump in one frame before the chain starts afresh where it is (a blink, a teleport). */
    static final double JUMP = 4.0;
    /** How much (of its length) a segment may stretch or shrink while it whips. */
    static final double GIVE = 0.12;

    private FleshPhysics() {
    }

    /**
     * How a length of flesh carries itself: how stiff its springs are at the root and at the tip (radians a tick: how quickly each
     * point follows; the tip's lower, it is heavier), how damped (under 1 it overshoots and swings back), whether the tip is held
     * on what it holds ({@code hold}: 0 free, 1 held right there; between, drawn onto it that much), and how many points it is
     * followed by.
     */
    public record Feel(double rootStiffness, double tipStiffness, double damping, double hold, int points) {
        /** The same, its tip held (it has hold of something) or let go. */
        public Feel held(boolean held) {
            return this.held(held ? 1.0 : 0.0);
        }

        /** The same, its tip drawn {@code hold} (0..1) of the way onto where its path ends (a beak closing on what it strikes). */
        public Feel held(double hold) {
            return new Feel(this.rootStiffness, this.tipStiffness, this.damping, Math.max(0.0, Math.min(1.0, hold)), this.points);
        }

        /** Whether its tip is held at all. */
        public boolean tipHeld() {
            return this.hold > 0.0;
        }
    }

    /** The swipe: thin quick flesh with a heavy bone at its end that trails, then whips through and swings back. */
    public static final Feel WHIP = new Feel(2.6, 0.62, 0.36, 0.0, 18);
    /** The grab reaching out, holding, throwing: a heavy knot at the end. */
    public static final Feel REACH = new Feel(2.4, 0.9, 0.5, 0.0, 18);
    /** The hammer's club: stiff (it is mostly bone and muscle round the leg) but heavy at its head. */
    public static final Feel CLUB = new Feel(3.2, 1.25, 0.55, 0.0, 12);
    /** The tentacles of Watch This: quick and predatory, a sharp beak at the end. */
    public static final Feel TENTACLE = new Feel(2.8, 0.85, 0.42, 0.0, 22);
    /** The mutated arm: long, elastic, its great extremity heavy. */
    public static final Feel ARM = new Feel(2.2, 0.55, 0.5, 0.0, 40);

    /** A length of flesh following its path over time, in whatever space its paths are given in (the world, in game). */
    public static final class Chain {
        private double[][] p;
        private double[][] v;
        private double[][] lastTarget;

        /** Whether it has a shape yet. */
        public boolean live() {
            return this.p != null;
        }

        /** Forgets its shape: it starts again where the next path is. */
        public void reset() {
            this.p = null;
            this.v = null;
            this.lastTarget = null;
        }

        /**
         * One frame: {@code dt} ticks on, its path is {@code path} (root first; null when none is out): the chain follows it and is
         * returned (its points, root first). The first path, a long gap or a jump of the root sets it straight onto the path.
         */
        public double[][] step(double[][] path, double dt, Feel feel) {
            if (path == null || path.length < 2) {
                this.reset();
                return null;
            }
            double[][] target = resample(path, feel.points());
            if (this.p == null || this.p.length != target.length || dt > LONGEST || dt < 0.0
                || distance(target[0], this.lastTarget[0]) > JUMP) {
                this.p = copy(target);
                this.v = new double[target.length][3];
                this.lastTarget = target;
                return copy(this.p);
            }
            if (dt < 1.0E-9) {
                return copy(this.p);
            }
            int steps = Math.max(1, (int)Math.ceil(dt / STEP));
            double h = dt / steps;
            for (int s = 1; s <= steps; s++) {
                // The path moves on smoothly through the frame.
                double[][] goal = lerp(this.lastTarget, target, s / (double)steps);
                this.advance(goal, h, feel);
            }
            this.lastTarget = target;
            return copy(this.p);
        }

        private void advance(double[][] goal, double h, Feel feel) {
            int n = goal.length;
            double[][] before = copy(this.p);
            // The root is wherever the root is.
            this.p[0] = goal[0].clone();
            for (int k = 1; k < n; k++) {
                double u = k / (double)(n - 1);
                double w = feel.rootStiffness() + (feel.tipStiffness() - feel.rootStiffness()) * Math.pow(u, 1.3);
                double c = 2.0 * feel.damping() * w;
                for (int a = 0; a < 3; a++) {
                    double accel = w * w * (goal[k][a] - this.p[k][a]) - c * this.v[k][a];
                    this.v[k][a] += accel * h;
                    this.p[k][a] += this.v[k][a] * h;
                }
            }
            // Held: drawn that share of the way onto it each tick (all the way, held right there).
            double hold = feel.hold() >= 1.0 ? 1.0 : 1.0 - Math.pow(1.0 - feel.hold(), h);
            if (hold > 0.0) {
                this.p[n - 1] = lerpPoint(this.p[n - 1], goal[n - 1], hold);
            }
            // Each segment keeps (near enough) its length: it bends, it does not stretch.
            for (int k = 1; k < n; k++) {
                double want = distance(goal[k], goal[k - 1]);
                double[] d = sub(this.p[k], this.p[k - 1]);
                double len = Math.sqrt(dot(d, d));
                if (len < 1.0E-9) {
                    continue;
                }
                double kept = Math.max(want * (1.0 - GIVE), Math.min(want * (1.0 + GIVE), len));
                if (kept != len) {
                    this.p[k] = add(this.p[k - 1], mul(d, kept / len));
                }
            }
            if (hold > 0.0) {
                // Held at both ends: the last few points ease back onto what it holds.
                for (int k = n - 1; k > 0 && k > n - 4; k--) {
                    double t = (k - (n - 4)) / 3.0;
                    this.p[k] = lerpPoint(this.p[k], goal[k], t * hold);
                }
            }
            for (int k = 0; k < n; k++) {
                this.v[k] = mul(sub(this.p[k], before[k]), 1.0 / h);
            }
        }
    }

    /**
     * Where a chain following the paths {@code pathAt} gives (ticks from 0) is at {@code age}, the paths in a still frame (for
     * the previews and the tests: the game steps its chains frame by frame instead).
     */
    public static double[][] simulate(java.util.function.DoubleFunction<double[][]> pathAt, double age, Feel feel) {
        Chain chain = new Chain();
        double[][] out = chain.step(pathAt.apply(0.0), 0.0, feel);
        double t = 0.0;
        while (t < age - 1.0E-9) {
            double next = Math.min(age, t + 0.25);
            out = chain.step(pathAt.apply(next), next - t, feel);
            t = next;
        }
        return out;
    }

    /** {@code path} as {@code n} points evenly spaced along its length (root and tip kept). */
    public static double[][] resample(double[][] path, int n) {
        double[] lengths = new double[path.length];
        for (int i = 1; i < path.length; i++) {
            lengths[i] = lengths[i - 1] + distance(path[i], path[i - 1]);
        }
        double total = lengths[path.length - 1];
        double[][] out = new double[n][];
        int j = 1;
        for (int k = 0; k < n; k++) {
            double want = total * k / (n - 1);
            while (j < path.length - 1 && lengths[j] < want) {
                j++;
            }
            double span = Math.max(1.0E-12, lengths[j] - lengths[j - 1]);
            double t = Math.max(0.0, Math.min(1.0, (want - lengths[j - 1]) / span));
            out[k] = lerpPoint(path[j - 1], path[j], t);
        }
        return out;
    }

    static double[][] copy(double[][] a) {
        double[][] out = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            out[i] = a[i].clone();
        }
        return out;
    }

    static double[][] lerp(double[][] a, double[][] b, double t) {
        double[][] out = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            out[i] = lerpPoint(a[i], b[i], t);
        }
        return out;
    }

    static double[] lerpPoint(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
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
