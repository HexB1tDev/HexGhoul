package com.hex.kagune.control.king;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.0.17: War's black cloth, simulated: the tattered cape in two layers down his back (the upper rows one piece, torn into strips
 * below) and the coat's split skirt round his hips. Each strip is a ribbon of particles two wide, hung from points on his torso and
 * moved by Verlet integration out in the world, so all he does reaches it as it would real cloth: it trails when he runs, swings
 * out when he turns, billows with the air going past it (drag on each face, against the wind), falls over his shoulders when he
 * crouches, is pushed by his legs and arms and never passes through his body or the ground.
 *
 * <p>Pure (no game classes): the caller says where his body parts are each frame ({@link Pose}: each part's own pixels out in the
 * world) and steps it; the layer draws it, the previewer and the tests run the same thing.
 */
public final class WarCloth {
    /** Where a body part's own pixels (x his left, y down from its pivot, z back) are in the world (blocks) at this moment. */
    public interface Frame {
        double[] at(double x, double y, double z);
    }

    /** His body this step: its parts, the ground under him ({@link Double#NaN}: none) and how many blocks a pixel is. */
    public record Pose(Frame torso, Frame rightArm, Frame leftArm, Frame rightLeg, Frame leftLeg, double ground, double scale) {
    }

    /** One quad of cloth drawn (world blocks), its strip's garment ({@link #CAPE}, {@link #OVER}, {@link #SKIRT}) and colour. */
    public interface Out {
        void quad(double[] a, double[] b, double[] c, double[] d, int colour);
    }

    public static final int CAPE = 0;
    public static final int OVER = 1;
    public static final int SKIRT = 2;

    /** Its substep (seconds): four to a tick. */
    public static final double DT = 0.0125;
    public static final int SUBSTEPS_PER_TICK = 4;
    /** Gravity (blocks per second squared), the air's hold on each face, what each substep keeps of the speed. */
    static final double GRAVITY = 15.0;
    static final double DRAG = 0.42;
    static final double KEEP = 0.992;
    static final int ITERATIONS = 10;
    /** How far a particle stays off his body and the ground (pixels): the cloth's own thickness. */
    static final double SKIN = 0.35;

    /** The black of it: the deepest, the usual and its sheen. */
    public static final int CLOTH_DEEP = 0xFF0D0B0C;
    public static final int CLOTH = 0xFF161314;
    public static final int CLOTH_SHEEN = 0xFF221E1F;

    /** A strip: where its two top corners hang (torso pixels), its rows, its length (pixels), its garment and the strip it is welded to. */
    record Strip(double[] left, double[] right, int rows, double length, int garment, int seed, int weldRows, int weldTo, double[] out) {
    }

    /**
     * The colliders: a capsule on a part, from {@code a} to {@code b} (the part's pixels), {@code radius} pixels round, for the
     * garments in {@code mask} (a bit each).
     */
    record Capsule(int part, double[] a, double[] b, double radius, int mask) {
    }

    static final int ALL = 7;
    /** The arms push the cape and its layer over it, not the skirt: a coat's sides hang between the arms and the hips. */
    static final int CAPES = 1 << CAPE | 1 << OVER;

    static final int TORSO = 0;
    static final int RIGHT_ARM = 1;
    static final int LEFT_ARM = 2;
    static final int RIGHT_LEG = 3;
    static final int LEFT_LEG = 4;

    static final Capsule[] BODY = {
        new Capsule(TORSO, v(-2.0, 2.4, 0.2), v(2.0, 2.4, 0.2), 3.0, ALL),
        new Capsule(TORSO, v(-1.8, 7.0, 0.1), v(1.8, 7.0, 0.1), 2.9, ALL),
        new Capsule(TORSO, v(-1.7, 10.9, 0.0), v(1.7, 10.9, 0.0), 2.75, ALL),
        new Capsule(RIGHT_ARM, v(-1.0, -1.0, 0.0), v(-1.0, 9.5, 0.0), 2.45, CAPES),
        new Capsule(LEFT_ARM, v(1.0, -1.0, 0.0), v(1.0, 9.5, 0.0), 2.35, CAPES),
        new Capsule(RIGHT_LEG, v(0.0, 0.0, 0.0), v(0.0, 11.4, 0.0), 2.45, ALL),
        new Capsule(LEFT_LEG, v(0.0, 0.0, 0.0), v(0.0, 11.4, 0.0), 2.45, ALL),
    };

    static final List<Strip> STRIPS = strips();

    // Particles: where they are, where they were (Verlet), where they were a substep ago (what is drawn between), pinned or free.
    final int count;
    final double[] x, y, z, px, py, pz, lx, ly, lz, ax, ay, az;
    final boolean[] pinned;
    /** Each pinned particle's point on the torso (pixels); null for a free one. */
    final double[][] hang;
    /** Constraints: the two ends, the rest length (pixels), how hard; and the far limit from the top of its column (pixels). */
    final int[] ca, cb;
    final double[] rest, stiff;
    final int[] top;
    final double[] reach;
    final int[] stripBase;
    /** Each particle's garment bit, for the colliders' masks; its strip; and each strip's way out from his body this step. */
    final int[] bit;
    final int[] stripOf;
    final double[][] outs;
    boolean ready;

    public WarCloth() {
        int n = 0;
        this.stripBase = new int[STRIPS.size()];
        for (int s = 0; s < STRIPS.size(); s++) {
            this.stripBase[s] = n;
            n += STRIPS.get(s).rows() * 2;
        }
        this.count = n;
        this.x = new double[n];
        this.y = new double[n];
        this.z = new double[n];
        this.px = new double[n];
        this.py = new double[n];
        this.pz = new double[n];
        this.lx = new double[n];
        this.ly = new double[n];
        this.lz = new double[n];
        this.ax = new double[n];
        this.ay = new double[n];
        this.az = new double[n];
        this.pinned = new boolean[n];
        this.hang = new double[n][];
        this.top = new int[n];
        this.reach = new double[n];
        this.bit = new int[n];
        this.stripOf = new int[n];
        this.outs = new double[STRIPS.size()][];
        for (int s = 0; s < STRIPS.size(); s++) {
            this.outs[s] = new double[] {0.0, 0.0, 1.0};
        }
        List<int[]> pairs = new ArrayList<>();
        List<double[]> lengths = new ArrayList<>();
        for (int s = 0; s < STRIPS.size(); s++) {
            Strip strip = STRIPS.get(s);
            int base = this.stripBase[s];
            double seg = strip.length() / (strip.rows() - 1);
            double width = dist(strip.left(), strip.right());
            for (int r = 0; r < strip.rows(); r++) {
                for (int c = 0; c < 2; c++) {
                    int i = base + r * 2 + c;
                    this.top[i] = base + c;
                    this.bit[i] = 1 << strip.garment();
                    this.stripOf[i] = s;
                    this.reach[i] = r * seg * 1.04;
                    if (r == 0) {
                        this.pinned[i] = true;
                        this.hang[i] = c == 0 ? strip.left() : strip.right();
                    }
                }
                double w = width * (1.0 + 0.14 * r / strip.rows());
                pairs.add(new int[] {base + r * 2, base + r * 2 + 1});
                lengths.add(new double[] {w, 1.0});
                if (r + 1 < strip.rows()) {
                    for (int c = 0; c < 2; c++) {
                        pairs.add(new int[] {base + r * 2 + c, base + (r + 1) * 2 + c});
                        lengths.add(new double[] {seg, 1.0});
                    }
                    double diag = Math.sqrt(seg * seg + w * w);
                    pairs.add(new int[] {base + r * 2, base + (r + 1) * 2 + 1});
                    lengths.add(new double[] {diag, 0.7});
                    pairs.add(new int[] {base + r * 2 + 1, base + (r + 1) * 2});
                    lengths.add(new double[] {diag, 0.7});
                }
                if (r + 2 < strip.rows()) {
                    for (int c = 0; c < 2; c++) {
                        pairs.add(new int[] {base + r * 2 + c, base + (r + 2) * 2 + c});
                        lengths.add(new double[] {seg * 2.0, 0.22});
                    }
                }
            }
            if (strip.weldTo() >= 0) {
                // The upper rows one piece: this strip's right edge held to the next one's left, letting go as they fan out.
                int other = this.stripBase[strip.weldTo()];
                double gap = dist(strip.right(), STRIPS.get(strip.weldTo()).left());
                for (int r = 1; r < strip.weldRows(); r++) {
                    pairs.add(new int[] {base + r * 2 + 1, other + r * 2});
                    // The cape held close as one piece; the skirt's panels let fan open over the legs as they part.
                    boolean skirt = strip.garment() == SKIRT;
                    lengths.add(new double[] {gap + (skirt ? 0.3 : 0.1) * r, skirt ? 0.85 : 0.9});
                }
            }
        }
        this.ca = new int[pairs.size()];
        this.cb = new int[pairs.size()];
        this.rest = new double[pairs.size()];
        this.stiff = new double[pairs.size()];
        for (int k = 0; k < pairs.size(); k++) {
            this.ca[k] = pairs.get(k)[0];
            this.cb[k] = pairs.get(k)[1];
            this.rest[k] = lengths.get(k)[0];
            this.stiff[k] = lengths.get(k)[1];
        }
    }

    /** True once it has been hung on a body. */
    public boolean ready() {
        return this.ready;
    }

    /** Hangs it fresh on {@code pose}: every strip straight down from where it hangs, then let settle. */
    public void reset(Pose pose) {
        for (int s = 0; s < STRIPS.size(); s++) {
            Strip strip = STRIPS.get(s);
            int base = this.stripBase[s];
            double seg = strip.length() / (strip.rows() - 1);
            double[] out = sub(pose.torso().at(strip.out()[0], strip.out()[1], strip.out()[2]), pose.torso().at(0.0, strip.out()[1], 0.0));
            out = norm(new double[] {out[0], 0.0, out[2]});
            for (int c = 0; c < 2; c++) {
                double[] h = c == 0 ? strip.left() : strip.right();
                double[] at = pose.torso().at(h[0], h[1], h[2]);
                for (int r = 0; r < strip.rows(); r++) {
                    int i = base + r * 2 + c;
                    double drop = r * seg * pose.scale();
                    double lean = r * 0.25 * pose.scale();
                    this.x[i] = at[0] + out[0] * lean;
                    this.y[i] = at[1] - drop;
                    this.z[i] = at[2] + out[2] * lean;
                }
            }
        }
        this.settle();
        this.ready = true;
        for (int k = 0; k < 60; k++) {
            this.step(pose, pose, 1, Wind.STILL);
        }
    }

    private void settle() {
        System.arraycopy(this.x, 0, this.px, 0, this.count);
        System.arraycopy(this.y, 0, this.py, 0, this.count);
        System.arraycopy(this.z, 0, this.pz, 0, this.count);
        System.arraycopy(this.x, 0, this.lx, 0, this.count);
        System.arraycopy(this.y, 0, this.ly, 0, this.count);
        System.arraycopy(this.z, 0, this.lz, 0, this.count);
    }

    /** The air: which way and how hard it blows (blocks per second). */
    public record Wind(double x, double y, double z) {
        public static final Wind STILL = new Wind(0.0, 0.0, 0.0);

        /** A breeze that wanders and gusts with {@code time} (ticks). */
        public static Wind breeze(double time) {
            double turn = time * 0.0021 + Math.sin(time * 0.0007) * 2.0;
            double gust = 0.55 + 0.35 * Math.sin(time * 0.013) + 0.25 * Math.sin(time * 0.041 + 1.3) * Math.sin(time * 0.0091);
            return new Wind(Math.cos(turn) * gust, 0.0, Math.sin(turn) * gust);
        }
    }

    /**
     * {@code substeps} steps from {@code from} to {@code to}: the hanging points and the body move evenly between the two over them.
     */
    public void step(Pose from, Pose to, int substeps, Wind wind) {
        if (substeps <= 0) {
            return;
        }
        int pins = 0;
        double[][] hangFrom = new double[this.count][];
        double[][] hangTo = new double[this.count][];
        for (int i = 0; i < this.count; i++) {
            if (this.pinned[i]) {
                hangFrom[i] = from.torso().at(this.hang[i][0], this.hang[i][1], this.hang[i][2]);
                hangTo[i] = to.torso().at(this.hang[i][0], this.hang[i][1], this.hang[i][2]);
                if (!finite(hangFrom[i]) || !finite(hangTo[i])) {
                    return;
                }
                pins++;
            }
        }
        if (pins == 0) {
            return;
        }
        double[][][] capsFrom = capsules(from);
        double[][][] capsTo = capsules(to);
        for (int s = 0; s < STRIPS.size(); s++) {
            double[] o = STRIPS.get(s).out();
            double[] d = sub(to.torso().at(o[0], o[1], o[2]), to.torso().at(0.0, o[1], 0.0));
            this.outs[s] = norm(new double[] {d[0], 0.0, d[2]});
        }
        double scale = to.scale();
        double dt2 = DT * DT;
        for (int k = 1; k <= substeps; k++) {
            double f = (double)k / substeps;
            double ground = Double.isNaN(to.ground()) ? Double.NaN : to.ground() + SKIN * scale;
            System.arraycopy(this.x, 0, this.lx, 0, this.count);
            System.arraycopy(this.y, 0, this.ly, 0, this.count);
            System.arraycopy(this.z, 0, this.lz, 0, this.count);
            this.air(wind);
            for (int i = 0; i < this.count; i++) {
                if (this.pinned[i]) {
                    double[] h = lerp(hangFrom[i], hangTo[i], f);
                    this.px[i] = this.x[i];
                    this.py[i] = this.y[i];
                    this.pz[i] = this.z[i];
                    this.x[i] = h[0];
                    this.y[i] = h[1];
                    this.z[i] = h[2];
                    continue;
                }
                double vx = (this.x[i] - this.px[i]) * KEEP;
                double vy = (this.y[i] - this.py[i]) * KEEP;
                double vz = (this.z[i] - this.pz[i]) * KEEP;
                this.px[i] = this.x[i];
                this.py[i] = this.y[i];
                this.pz[i] = this.z[i];
                this.x[i] += vx + this.ax[i] * dt2;
                this.y[i] += vy + (this.ay[i] - GRAVITY) * dt2;
                this.z[i] += vz + this.az[i] * dt2;
            }
            double[][][] caps = new double[capsTo.length][][];
            for (int c = 0; c < caps.length; c++) {
                caps[c] = new double[][] {lerp(capsFrom[c][0], capsTo[c][0], f), lerp(capsFrom[c][1], capsTo[c][1], f), capsTo[c][2]};
            }
            for (int it = 0; it < ITERATIONS; it++) {
                this.solve(scale);
                this.collide(caps, ground, scale);
            }
        }
    }

    /** The air on each face: what of its own speed through the air goes along the face's normal is held back. */
    private void air(Wind wind) {
        java.util.Arrays.fill(this.ax, 0.0);
        java.util.Arrays.fill(this.ay, 0.0);
        java.util.Arrays.fill(this.az, 0.0);
        for (int s = 0; s < STRIPS.size(); s++) {
            int base = this.stripBase[s];
            int rows = STRIPS.get(s).rows();
            for (int r = 0; r + 1 < rows; r++) {
                int a = base + r * 2;
                int b = a + 1;
                int c = a + 3;
                int d = a + 2;
                double[] e1 = {this.x[c] - this.x[a], this.y[c] - this.y[a], this.z[c] - this.z[a]};
                double[] e2 = {this.x[d] - this.x[b], this.y[d] - this.y[b], this.z[d] - this.z[b]};
                double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
                double area = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]) * 0.5;
                if (area < 1.0E-12) {
                    continue;
                }
                n[0] /= area * 2.0;
                n[1] /= area * 2.0;
                n[2] /= area * 2.0;
                double vx = (this.x[a] + this.x[b] + this.x[c] + this.x[d] - this.px[a] - this.px[b] - this.px[c] - this.px[d]) / (4.0 * DT) - wind.x();
                double vy = (this.y[a] + this.y[b] + this.y[c] + this.y[d] - this.py[a] - this.py[b] - this.py[c] - this.py[d]) / (4.0 * DT) - wind.y();
                double vz = (this.z[a] + this.z[b] + this.z[c] + this.z[d] - this.pz[a] - this.pz[b] - this.pz[c] - this.pz[d]) / (4.0 * DT) - wind.z();
                double along = vx * n[0] + vy * n[1] + vz * n[2];
                // Per pixel of cloth (its area in square pixels over the particles' mass): the faster through the air, the harder.
                double push = -DRAG * along * Math.abs(along) * 0.35 - DRAG * along;
                double fx = n[0] * push;
                double fy = n[1] * push;
                double fz = n[2] * push;
                this.ax[a] += fx;
                this.ay[a] += fy;
                this.az[a] += fz;
                this.ax[b] += fx;
                this.ay[b] += fy;
                this.az[b] += fz;
                this.ax[c] += fx;
                this.ay[c] += fy;
                this.az[c] += fz;
                this.ax[d] += fx;
                this.ay[d] += fy;
                this.az[d] += fz;
            }
        }
    }

    private void solve(double scale) {
        for (int k = 0; k < this.ca.length; k++) {
            int a = this.ca[k];
            int b = this.cb[k];
            double dx = this.x[b] - this.x[a];
            double dy = this.y[b] - this.y[a];
            double dz = this.z[b] - this.z[a];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < 1.0E-9) {
                continue;
            }
            double want = this.rest[k] * scale;
            double diff = (d - want) / d * this.stiff[k];
            boolean pa = this.pinned[a];
            boolean pb = this.pinned[b];
            if (pa && pb) {
                continue;
            }
            double wa = pa ? 0.0 : pb ? 1.0 : 0.5;
            double wb = pb ? 0.0 : pa ? 1.0 : 0.5;
            this.x[a] += dx * diff * wa;
            this.y[a] += dy * diff * wa;
            this.z[a] += dz * diff * wa;
            this.x[b] -= dx * diff * wb;
            this.y[b] -= dy * diff * wb;
            this.z[b] -= dz * diff * wb;
        }
        // Never farther from the top of its column than the cloth between them: no rubber.
        for (int i = 0; i < this.count; i++) {
            if (this.pinned[i]) {
                continue;
            }
            int t = this.top[i];
            double dx = this.x[i] - this.x[t];
            double dy = this.y[i] - this.y[t];
            double dz = this.z[i] - this.z[t];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double far = this.reach[i] * scale;
            if (d > far && d > 1.0E-9) {
                double k = far / d;
                this.x[i] = this.x[t] + dx * k;
                this.y[i] = this.y[t] + dy * k;
                this.z[i] = this.z[t] + dz * k;
            }
        }
    }

    private void collide(double[][][] caps, double ground, double scale) {
        // Each capsule once: its ends, its radius out to the cloth, its middle and how far round it anything can touch it.
        int nc = caps.length;
        double[] c = this.capData.length == nc * 10 ? this.capData : (this.capData = new double[nc * 10]);
        for (int k = 0; k < nc; k++) {
            double[][] cap = caps[k];
            int o = k * 10;
            c[o] = cap[0][0];
            c[o + 1] = cap[0][1];
            c[o + 2] = cap[0][2];
            c[o + 3] = cap[1][0] - cap[0][0];
            c[o + 4] = cap[1][1] - cap[0][1];
            c[o + 5] = cap[1][2] - cap[0][2];
            c[o + 6] = (cap[2][0] + SKIN) * scale;
            c[o + 7] = c[o + 3] * c[o + 3] + c[o + 4] * c[o + 4] + c[o + 5] * c[o + 5];
            double half = Math.sqrt(c[o + 7]) * 0.5;
            c[o + 8] = half + c[o + 6]; // bounding reach from its middle
            c[o + 9] = half;
        }
        for (int i = 0; i < this.count; i++) {
            if (this.pinned[i]) {
                continue;
            }
            double px0 = this.x[i];
            double py0 = this.y[i];
            double pz0 = this.z[i];
            boolean hit = false;
            for (int k = 0; k < nc; k++) {
                if ((BODY[k].mask() & this.bit[i]) == 0) {
                    continue;
                }
                int o = k * 10;
                double mx = c[o] + c[o + 3] * 0.5 - px0;
                double my = c[o + 1] + c[o + 4] * 0.5 - py0;
                double mz = c[o + 2] + c[o + 5] * 0.5 - pz0;
                if (mx * mx + my * my + mz * mz > c[o + 8] * c[o + 8]) {
                    continue;
                }
                double t = c[o + 7] < 1.0E-12 ? 0.0 : ((px0 - c[o]) * c[o + 3] + (py0 - c[o + 1]) * c[o + 4] + (pz0 - c[o + 2]) * c[o + 5]) / c[o + 7];
                t = t < 0.0 ? 0.0 : t > 1.0 ? 1.0 : t;
                double qx = c[o] + c[o + 3] * t;
                double qy = c[o + 1] + c[o + 4] * t;
                double qz = c[o + 2] + c[o + 5] * t;
                double dx = px0 - qx;
                double dy = py0 - qy;
                double dz = pz0 - qz;
                double d2 = dx * dx + dy * dy + dz * dz;
                double r = c[o + 6];
                if (d2 >= r * r) {
                    continue;
                }
                double d = Math.sqrt(d2);
                if (d < 1.0E-9) {
                    dx = 0.0;
                    dy = 0.0;
                    dz = 1.0;
                    d = 1.0;
                }
                double[] n = outward(dx / d, dy / d, dz / d, caps[k], this.outs[this.stripOf[i]]);
                px0 = qx + n[0] * r;
                py0 = qy + n[1] * r;
                pz0 = qz + n[2] * r;
                hit = true;
            }
            if (!Double.isNaN(ground) && py0 < ground) {
                py0 = ground;
                hit = true;
            }
            if (hit) {
                // Cloth on a body drags on it: some of what it slid by is lost.
                this.px[i] += (px0 - this.px[i]) * 0.25;
                this.py[i] += (py0 - this.py[i]) * 0.25;
                this.pz[i] += (pz0 - this.pz[i]) * 0.25;
                this.x[i] = px0;
                this.y[i] = py0;
                this.z[i] = pz0;
            }
        }
        // Each strip's edge across it as well: a leg must not slip between its two sides (they are wider apart than a leg is round).
        for (int s = 0; s < STRIPS.size(); s++) {
            int base = this.stripBase[s];
            int rows = STRIPS.get(s).rows();
            for (int r = 1; r < rows; r++) {
                this.edge(base + r * 2, base + r * 2 + 1, caps, c);
            }
        }
    }

    /** Each capsule as {@link #collide} keeps it this substep (reused). */
    private double[] capData = new double[0];

    /** The edge from particle {@code a} to {@code b} pushed out of every capsule it passes through, both ends as far as it takes. */
    private void edge(int a, int b, double[][][] caps, double[] c) {
        for (int k = 0; k < caps.length; k++) {
            if ((BODY[k].mask() & this.bit[a]) == 0) {
                continue;
            }
            int o = k * 10;
            double ax0 = this.x[a];
            double ay0 = this.y[a];
            double az0 = this.z[a];
            double ex = this.x[b] - ax0;
            double ey = this.y[b] - ay0;
            double ez = this.z[b] - az0;
            // Far from the capsule altogether: nothing to do.
            double mx = c[o] + c[o + 3] * 0.5 - (ax0 + ex * 0.5);
            double my = c[o + 1] + c[o + 4] * 0.5 - (ay0 + ey * 0.5);
            double mz = c[o + 2] + c[o + 5] * 0.5 - (az0 + ez * 0.5);
            double reach = c[o + 8] + Math.sqrt(ex * ex + ey * ey + ez * ez) * 0.5;
            if (mx * mx + my * my + mz * mz > reach * reach) {
                continue;
            }
            // The closest points of the edge and the capsule's axis (Ericson's segment-segment).
            double dx2 = c[o + 3];
            double dy2 = c[o + 4];
            double dz2 = c[o + 5];
            double rx = ax0 - c[o];
            double ry = ay0 - c[o + 1];
            double rz = az0 - c[o + 2];
            double aa = ex * ex + ey * ey + ez * ez;
            double ee = c[o + 7];
            double ff = dx2 * rx + dy2 * ry + dz2 * rz;
            if (aa < 1.0E-12 || ee < 1.0E-12) {
                continue;
            }
            double cc = ex * rx + ey * ry + ez * rz;
            double bb = ex * dx2 + ey * dy2 + ez * dz2;
            double denom = aa * ee - bb * bb;
            double sP = denom > 1.0E-12 ? clamp((bb * ff - cc * ee) / denom) : 0.0;
            double tP = (bb * sP + ff) / ee;
            if (tP < 0.0) {
                tP = 0.0;
                sP = clamp(-cc / aa);
            } else if (tP > 1.0) {
                tP = 1.0;
                sP = clamp((bb - cc) / aa);
            }
            if (sP <= 0.02 || sP >= 0.98) {
                continue; // its ends are the particles' own (pushed above)
            }
            double px0 = ax0 + ex * sP;
            double py0 = ay0 + ey * sP;
            double pz0 = az0 + ez * sP;
            double qx = c[o] + dx2 * tP;
            double qy = c[o + 1] + dy2 * tP;
            double qz = c[o + 2] + dz2 * tP;
            double dx = px0 - qx;
            double dy = py0 - qy;
            double dz = pz0 - qz;
            double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double r = c[o + 6];
            if (l >= r || l < 1.0E-9) {
                continue;
            }
            double[] out = outward(dx / l, dy / l, dz / l, caps[k], this.outs[this.stripOf[a]]);
            double depth = out[0] * dx + out[1] * dy + out[2] * dz < 0.0 ? r + l : r - l; // through to its own side, if it is on the wrong one
            double wa = this.pinned[a] ? 0.0 : 1.0 - sP;
            double wb = this.pinned[b] ? 0.0 : sP;
            double sum = wa * wa + wb * wb;
            if (sum < 1.0E-9) {
                continue;
            }
            double k2 = depth / sum;
            this.x[a] += out[0] * k2 * wa;
            this.y[a] += out[1] * k2 * wa;
            this.z[a] += out[2] * k2 * wa;
            this.x[b] += out[0] * k2 * wb;
            this.y[b] += out[1] * k2 * wb;
            this.z[b] += out[2] * k2 * wb;
        }
    }

    /**
     * Which way a capsule pushes cloth out of it: straight out from its axis ({@code nx, ny, nz}), unless that is the wrong side for
     * the strip (a strip of the cape caught in front of a leg, the skirt behind one): then out on the strip's own side ({@code out}).
     */
    static double[] outward(double nx, double ny, double nz, double[][] cap, double[] out) {
        if (nx * out[0] + ny * out[1] + nz * out[2] >= -0.15) {
            return new double[] {nx, ny, nz};
        }
        double[] axis = norm(sub(cap[1], cap[0]));
        double along = dot(out, axis);
        double[] side = norm(new double[] {out[0] - axis[0] * along, out[1] - axis[1] * along, out[2] - axis[2] * along});
        return side[0] == 0.0 && side[1] == 0.0 && side[2] == 0.0 ? new double[] {nx, ny, nz} : side;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /** Each collider out in the world: its two ends and (in a third array) its radius in pixels. */
    static double[][][] capsules(Pose pose) {
        Frame[] parts = {pose.torso(), pose.rightArm(), pose.leftArm(), pose.rightLeg(), pose.leftLeg()};
        double[][][] out = new double[BODY.length][][];
        for (int c = 0; c < BODY.length; c++) {
            Capsule cap = BODY[c];
            Frame part = parts[cap.part()];
            out[c] = new double[][] {part.at(cap.a()[0], cap.a()[1], cap.a()[2]), part.at(cap.b()[0], cap.b()[1], cap.b()[2]), {cap.radius()}};
        }
        return out;
    }

    /**
     * Throws every free particle out from {@code centre} (world) at {@code speed} blocks a second, and up a little: the cloth
     * thrown wide by what bursts out of him.
     */
    public void burst(double[] centre, double speed) {
        for (int i = 0; i < this.count; i++) {
            if (this.pinned[i]) {
                continue;
            }
            double[] d = norm(new double[] {this.x[i] - centre[0], 0.0, this.z[i] - centre[2]});
            double kick = speed * (0.7 + 0.3 * hash(i * 13 + 5));
            this.px[i] = this.x[i] - (d[0] * kick) * DT;
            this.py[i] = this.y[i] - (0.55 * kick) * DT;
            this.pz[i] = this.z[i] - (d[2] * kick) * DT;
        }
    }

    /**
     * The cloth as it is drawn: each particle between where it was a substep ago and where it is ({@code alpha} 0..1), moved by
     * {@code shift} (what the body moved since, so the cloth keeps up with it to the frame); {@code out} 0..1 how far it is out of
     * him (taken off, it draws back up to where it hangs).
     */
    public void draw(double alpha, double[] shift, double out, Out sink) {
        if (!this.ready) {
            return;
        }
        double[][] p = new double[this.count][];
        for (int i = 0; i < this.count; i++) {
            double[] at = {this.lx[i] + (this.x[i] - this.lx[i]) * alpha + shift[0], this.ly[i] + (this.y[i] - this.ly[i]) * alpha + shift[1],
                this.lz[i] + (this.z[i] - this.lz[i]) * alpha + shift[2]};
            p[i] = at;
        }
        if (out < 1.0) {
            for (int i = 0; i < this.count; i++) {
                double[] t = p[this.top[i]];
                p[i] = lerp(t, p[i], Math.max(0.0, out));
            }
        }
        for (int s = 0; s < STRIPS.size(); s++) {
            Strip strip = STRIPS.get(s);
            int base = this.stripBase[s];
            int rows = strip.rows();
            for (int r = 0; r + 1 < rows; r++) {
                double[] a = p[base + r * 2];
                double[] b = p[base + r * 2 + 1];
                double[] c = p[base + (r + 1) * 2 + 1];
                double[] d = p[base + (r + 1) * 2];
                if (r + 2 == rows) {
                    // The torn end: its last edge drawn in toward a ragged point.
                    double pull = 0.45 + 0.4 * hash(strip.seed() * 7 + 1);
                    double lean = hash(strip.seed() * 3 + 2);
                    double[] mid = lerp(d, c, lean);
                    c = lerp(c, mid, pull);
                    d = lerp(d, mid, pull);
                }
                sink.quad(a, b, c, d, colour(strip, (double)r / (rows - 1)));
            }
        }
    }

    /** A strip's black, a little different strip to strip, deepest at the torn end. */
    static int colour(Strip strip, double u) {
        double sheen = hash(strip.seed() * 11 + 4) * 0.6 * (1.0 - u);
        int c = mix(CLOTH, CLOTH_SHEEN, sheen);
        return mix(c, CLOTH_DEEP, Math.max(0.0, u - 0.55) / 0.45 * 0.8);
    }

    /** Every particle, world blocks (for checks): {x, y, z} each. */
    public double[][] points() {
        double[][] out = new double[this.count][];
        for (int i = 0; i < this.count; i++) {
            out[i] = new double[] {this.x[i], this.y[i], this.z[i]};
        }
        return out;
    }

    /** True for each particle that hangs (pinned to the body). */
    public boolean pinned(int i) {
        return this.pinned[i];
    }

    /** How far particle {@code i} is from the one above it in its column, over what the cloth between them is (1: as it hangs). */
    public double stretch(int i, double scale) {
        int s = strip(i);
        int base = this.stripBase[s];
        int r = (i - base) / 2;
        if (r == 0) {
            return 1.0;
        }
        int above = i - 2;
        double seg = STRIPS.get(s).length() / (STRIPS.get(s).rows() - 1) * scale;
        return dist(new double[] {this.x[i], this.y[i], this.z[i]}, new double[] {this.x[above], this.y[above], this.z[above]}) / seg;
    }

    /** The garment particle {@code i} is of. */
    public int garment(int i) {
        return STRIPS.get(strip(i)).garment();
    }

    /** How many particles. */
    public int count() {
        return this.count;
    }

    private int strip(int i) {
        int s = 0;
        while (s + 1 < this.stripBase.length && this.stripBase[s + 1] <= i) {
            s++;
        }
        return s;
    }

    /** How deep (pixels) point {@code p} (world) of {@code garment} is inside what of his body pushes it on {@code pose} (0: clear). */
    public static double inside(Pose pose, double[] p, int garment) {
        double deepest = 0.0;
        double[][][] caps = capsules(pose);
        for (int k = 0; k < caps.length; k++) {
            if ((BODY[k].mask() & 1 << garment) == 0) {
                continue;
            }
            double[][] cap = caps[k];
            double r = cap[2][0] * pose.scale();
            double d = dist(p, closest(p, cap[0], cap[1]));
            deepest = Math.max(deepest, (r - d) / pose.scale());
        }
        return deepest;
    }

    // ------------------------------------------------------------------ the garments

    static List<Strip> strips() {
        List<Strip> out = new ArrayList<>();
        // The cape: ten long strips over his back to the ankles, then eight shorter over them; each layer one piece down most of its
        // length, torn into strips only toward the hem.
        cape(out, 10, 1.55, 0.25, 1.6, 3.85, 0.85, 17.6, 3.0, 11, 8, CAPE, 0);
        cape(out, 8, 1.85, 0.3, 1.0, 4.45, 0.9, 9.8, 5.0, 7, 5, OVER, 100);
        // The coat's skirt from the belt to the knees, split at the front and at each side, open at the back under the cape.
        double[][] panels = {{-80.0, -7.0}, {7.0, 80.0}, {-142.0, -82.0}, {82.0, 142.0}};
        for (int k = 0; k < panels.length; k++) {
            int first = out.size();
            for (int j = 0; j < 3; j++) {
                double a0 = panels[k][0] + (panels[k][1] - panels[k][0]) * j / 3.0 - 2.0;
                double a1 = panels[k][0] + (panels[k][1] - panels[k][0]) * (j + 1) / 3.0 + 2.0;
                double length = 6.6 + hash(k * 9 + j + 300) * 1.6;
                int weld = j < 2 ? first + j + 1 : -1;
                out.add(new Strip(belt(a0), belt(a1), 6, length, SKIRT, 300 + k * 3 + j, 3, weld, belt((a0 + a1) * 0.5)));
            }
        }
        return out;
    }

    private static void cape(List<Strip> out, int n, double width, double overlap, double y, double back, double curve, double length,
                             double spread, int rows, int weld, int garment, int seed) {
        double total = n * width - (n - 1) * overlap;
        int first = out.size();
        for (int i = 0; i < n; i++) {
            double x0 = -total * 0.5 + i * (width - overlap);
            double x1 = x0 + width;
            double edge = Math.abs((x0 + x1) * 0.5) / (total * 0.5);
            double len = length + hash(seed + i * 7) * spread - 1.6 * edge * edge;
            int weldTo = i + 1 < n ? first + i + 1 : -1;
            out.add(new Strip(v(x0, y, capeZ(x0, total, back, curve)), v(x1, y, capeZ(x1, total, back, curve)), rows, len, garment, seed + i, weld, weldTo,
                v((x0 + x1) * 0.5, y, capeZ((x0 + x1) * 0.5, total, back, curve) + 1.0)));
        }
    }

    private static double capeZ(double x, double total, double back, double curve) {
        double u = x / (total * 0.5);
        return back - curve * u * u;
    }

    /** A point round the belt, {@code degrees} from the front (to his left positive), torso pixels. */
    private static double[] belt(double degrees) {
        double a = Math.toRadians(degrees);
        return v(5.3 * Math.sin(a), 11.6, -3.45 * Math.cos(a));
    }

    // ------------------------------------------------------------------ vectors

    static double[] v(double x, double y, double z) {
        return new double[] {x, y, z};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double dist(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static double[] norm(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-12 ? new double[] {0.0, 0.0, 0.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }

    static boolean finite(double[] a) {
        return Double.isFinite(a[0]) && Double.isFinite(a[1]) && Double.isFinite(a[2]);
    }

    static double[] closest(double[] p, double[] a, double[] b) {
        double[] ab = sub(b, a);
        double l = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        double t = l < 1.0E-12 ? 0.0 : ((p[0] - a[0]) * ab[0] + (p[1] - a[1]) * ab[1] + (p[2] - a[2]) * ab[2]) / l;
        t = Math.max(0.0, Math.min(1.0, t));
        return new double[] {a[0] + ab[0] * t, a[1] + ab[1] * t, a[2] + ab[2] * t};
    }

    static double hash(int n) {
        int h = n * 0x27D4EB2D;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFF) / 65535.0;
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r = (int)Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int)Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int)Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
