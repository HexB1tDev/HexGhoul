package com.hex.kagune.control.graft;

import com.hex.kagune.control.ukaku.CentipedeMotion;

/**
 * Graft DNA's limbs in motion (pure, tested; 7.0.0). Each limb follows its {@link GraftRig.Pose} with critically damped springs
 * on the player's own clock (solved exactly per step, so any frame rate is stable), as Rinkaku's centipede legs do: it never snaps,
 * never overshoots, and its far end trails its root, so a strike whips out and a turn drags the limb round after him. Unlike them a
 * limb starts as nothing but its root in his back and is drawn out of it, so the first pose it is given is grown into, not jumped
 * to; and its length is followed too (a strike stretched past its rest length eases out and back).
 */
public final class GraftMotion {
    public static final int POINTS = CentipedeMotion.POINTS;

    private GraftMotion() {
    }

    /** One limb following its poses. */
    public static final class Limb {
        private final double[][] p = new double[POINTS][3];
        private final double[][] v = new double[POINTS][3];
        private final double[] dorsal = {0.0, 1.0, 0.0};
        private final double[] dorsalV = new double[3];
        private final double[] jaw = new double[2];
        private final double[] flare = {1.0, 0.0};
        private final double[] stare = new double[2];
        private final double[] length = new double[2];
        private double last = Double.NaN;

        /** Follow {@code pose} up to {@code time} (ticks). The first call (or after a long gap) starts it coiled in its root. */
        public void follow(GraftRig.Pose pose, double time) {
            double[][] target = pose.points();
            if (Double.isNaN(this.last) || time < this.last - 1.0E-6 || time - this.last > 20.0) {
                for (int i = 0; i < POINTS; i++) {
                    // Every point starts in the root: the limb unfolds out of his back along the springs.
                    double f = i / (double)(POINTS - 1);
                    double[] start = lerp(target[0], target[Math.min(POINTS - 1, 1)], 0.2 * f);
                    System.arraycopy(start, 0, this.p[i], 0, 3);
                    java.util.Arrays.fill(this.v[i], 0.0);
                }
                System.arraycopy(pose.dorsal(), 0, this.dorsal, 0, 3);
                java.util.Arrays.fill(this.dorsalV, 0.0);
                this.jaw[0] = pose.jaw();
                this.flare[0] = pose.flare();
                this.stare[0] = pose.stare();
                this.length[0] = pose.length();
                this.last = time;
                return;
            }
            double dt = Math.min(4.0, time - this.last);
            this.last = time;
            double omega = 2.6 / Math.max(0.2, pose.response());
            for (int i = 0; i < POINTS; i++) {
                if (i == 0) {
                    System.arraycopy(target[0], 0, this.p[0], 0, 3);
                    java.util.Arrays.fill(this.v[0], 0.0);
                    continue;
                }
                // Stiff near the root, looser toward the point: the far end lags and trails.
                double f = i / (double)(POINTS - 1);
                double w = omega * (i == 1 ? 3.0 : 1.35 - 0.45 * f);
                for (int axis = 0; axis < 3; axis++) {
                    double[] next = spring(this.p[i][axis], this.v[i][axis], target[i][axis], w, dt);
                    this.p[i][axis] = next[0];
                    this.v[i][axis] = next[1];
                }
            }
            for (int axis = 0; axis < 3; axis++) {
                double[] next = spring(this.dorsal[axis], this.dorsalV[axis], pose.dorsal()[axis], omega, dt);
                this.dorsal[axis] = next[0];
                this.dorsalV[axis] = next[1];
            }
            step(this.jaw, pose.jaw(), omega * 1.6, dt);
            step(this.flare, pose.flare(), omega, dt);
            step(this.stare, pose.stare(), omega * 1.2, dt);
            step(this.length, pose.length(), omega, dt);
        }

        private static void step(double[] state, double target, double omega, double dt) {
            double[] next = spring(state[0], state[1], target, omega, dt);
            state[0] = next[0];
            state[1] = next[1];
        }

        /** The limb's points now (rig space, root first). */
        public double[][] points() {
            double[][] out = new double[POINTS][];
            for (int i = 0; i < POINTS; i++) {
                out[i] = this.p[i].clone();
            }
            return out;
        }

        public double[] dorsal() {
            double l = Math.sqrt(this.dorsal[0] * this.dorsal[0] + this.dorsal[1] * this.dorsal[1] + this.dorsal[2] * this.dorsal[2]);
            return l < 1.0E-6 ? new double[] {0.0, 1.0, 0.0} : new double[] {this.dorsal[0] / l, this.dorsal[1] / l, this.dorsal[2] / l};
        }

        public double jaw() {
            return Math.max(0.0, Math.min(1.0, this.jaw[0]));
        }

        public double flare() {
            return Math.max(0.0, Math.min(2.0, this.flare[0]));
        }

        public double stare() {
            return Math.max(0.0, Math.min(1.0, this.stare[0]));
        }

        /** How long it is drawn out now (blocks). */
        public double length() {
            return Math.max(0.1, this.length[0]);
        }
    }

    /** One critically damped spring step, solved exactly: {value, velocity} after {@code dt} toward {@code target}. */
    static double[] spring(double value, double velocity, double target, double omega, double dt) {
        double y = value - target;
        double e = Math.exp(-omega * dt);
        double temp = (velocity + omega * y) * dt;
        return new double[] {target + (y + temp) * e, (velocity - omega * temp) * e};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }
}
