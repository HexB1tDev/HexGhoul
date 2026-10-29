package com.hex.kagune.control.ukaku;

/**
 * Rinkaku's centipede legs in motion (pure, tested). Each leg takes a pose: control points in rig space (x to the player's right,
 * y up from the feet, z forward, blocks), fitted to the leg's length and resampled to {@link #POINTS} evenly spaced points. A
 * {@link Leg} follows its pose with critically damped springs on the player's own clock, so a leg never snaps, never overshoots
 * and never stretches: it rears, strikes, coils and settles back in one smooth line.
 *
 * <p>Rest poses are the approved designs: the first kakuja's right leg arched over his head like a scorpion and the left swept
 * out and hooked down; the final kakuja's two legs up and out like horns, fangs down at his sides. Every pose's "back" (its
 * dorsal side) faces out of the plane it curls in, so the claw rows splay round the curl.
 */
public final class CentipedeMotion {
    /** Points per leg after resampling (the renderer draws a spline through them). */
    public static final int POINTS = 12;
    /** The designs were drawn for legs this long (blocks); poses scale with the real length. */
    static final double DESIGN = 5.5;
    /** The designs' root (the right leg's; the left mirrors it). */
    static final double[] DESIGN_ROOT = {0.12, 1.05, -0.2};

    /** What a leg is doing. */
    public enum Act {
        /** Its rest pose, breathing, the head drifting where he looks. */
        REST,
        /** Rearing back over his shoulder toward a target, jaw opening, claws bristling. */
        LOCK,
        /** One arc, head first, onto the target. */
        STRIKE,
        /** Wrapped once round a held body, the jaw shut on it. */
        COIL,
        /** Swept round in front of him toward an attacker, claws out. */
        BLOCK,
        /** Up and over, down onto the target. */
        SLAM,
        /** Whipped out and forward, letting go. */
        THROW
    }

    /**
     * One leg this frame.
     *
     * @param side      +1 his right leg, -1 his left
     * @param finalForm the final kakuja (horns) rather than the first (scorpion and sweep)
     * @param length    the leg's length (blocks)
     * @param rootY     height of the leg's root above his feet (lower when he crouches)
     * @param act       what it does
     * @param target    the target or the held body's middle (rig space; ignored at rest)
     * @param radius    for a coil, the held body's half-width
     * @param grab      a strike that bites (jaw wide) rather than stabs
     * @param lookYaw   his head's turn from his body (radians, positive to his right)
     * @param lookPitch his look's pitch (radians, positive down)
     * @param speed     how fast he moves (blocks a tick)
     * @param time      ticks (the breathing)
     */
    public record Input(int side, boolean finalForm, double length, double rootY, Act act, double[] target, double radius, boolean grab,
                        double lookYaw, double lookPitch, double speed, double time) {
    }

    /**
     * A pose to follow: the points (rig space, root first), the back's direction, the jaw (0 shut, 1 wide), the claws' flare
     * (1 at rest) and how quickly to get there (ticks to cover most of the way).
     */
    public record Pose(double[][] points, double[] dorsal, double jaw, double flare, double response) {
    }

    // The approved designs (rig space, the right leg's root at DESIGN_ROOT).
    static final double[][] SCORPION = {{0.12, 1.05, -0.2}, {0.35, 1.2, -0.7}, {0.75, 1.9, -1.25}, {1.05, 3.0, -1.15}, {0.95, 3.85, -0.35},
        {0.6, 3.8, 0.55}, {0.35, 3.1, 1.0}};
    static final double[][] SWEEP = {{0.12, 1.05, -0.2}, {0.5, 1.25, -0.75}, {1.3, 1.95, -1.05}, {2.35, 2.5, -0.85}, {3.25, 2.35, -0.25},
        {3.7, 1.6, 0.3}};
    static final double[][] HORN = {{0.12, 1.05, -0.2}, {0.45, 1.15, -0.7}, {1.15, 1.45, -1.0}, {2.0, 1.8, -0.9}, {2.75, 2.0, -0.45},
        {3.2, 1.85, 0.15}, {3.25, 1.45, 0.75}, {2.95, 1.15, 1.2}};
    static final double[][] REAR = {{0.12, 1.05, -0.2}, {0.4, 1.4, -0.8}, {0.95, 2.5, -1.3}, {1.3, 3.6, -0.95}, {1.35, 4.25, -0.05},
        {1.3, 3.95, 0.75}};
    static final double[][] GUARD = {{0.12, 1.05, -0.2}, {0.35, 1.3, -0.6}, {0.95, 2.0, -0.35}, {1.05, 2.25, 0.75}, {0.25, 2.0, 1.35},
        {-0.55, 1.75, 1.25}};
    static final double[][] WHIP = {{0.12, 1.05, -0.2}, {0.4, 1.6, -0.6}, {1.0, 3.0, -0.2}, {1.8, 3.4, 1.2}, {2.6, 3.0, 2.4}, {3.0, 2.4, 3.0}};

    private CentipedeMotion() {
    }

    /** A leg's length at {@code mastery} (blocks): 6.5 for the first kakuja at 750, growing to 7.5 at 1000. */
    public static double length(int mastery) {
        return 6.5 + Math.max(0.0, Math.min(1.0, (mastery - 750) / 250.0));
    }

    /** A leg's half-width where it is thickest, for its length. */
    public static double width(double length) {
        return 0.3 + 0.04 * (length - 6.5);
    }

    /** Plates for a leg of {@code length} at {@code width}: each about as long as the leg is wide. */
    public static int plates(double length, double width) {
        return Math.max(5, (int)Math.round(length / (2.67 * width)));
    }

    private static final java.util.Map<Long, Double> REACH = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * How far a leg of {@code length} reaches from his body in a strike (blocks, horizontally, to a body's middle at chest height,
     * out to the front and side): the hunt goes after nothing farther, so the head always lands where the hit does.
     */
    public static double reach(double length) {
        long key = Math.round(length * 20.0);
        return REACH.computeIfAbsent(key, k -> {
            double l = k / 20.0;
            double low = 1.0;
            double high = l;
            for (int i = 0; i < 14; i++) {
                double mid = 0.5 * (low + high);
                double[] target = {Math.sin(0.45) * mid, 1.1, Math.cos(0.45) * mid};
                double[][] points = pose(new Input(1, false, l, 0.99, Act.STRIKE, target, 0.3, false, 0.0, 0.0, 0.0, 0.0)).points();
                if (distance(points[POINTS - 1], target) < 0.35) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            return low;
        });
    }

    /**
     * Where a caught body's middle is held (rig space): out to the leg's own side, a little ahead, at about his head's height,
     * where he sees it and the leg's coil round it fits the leg's length.
     */
    public static double[] holdPoint(int side, double length) {
        double k = length / 6.5;
        return new double[] {(side >= 0 ? 1 : -1) * 2.0 * k, 2.1, 1.35 * k};
    }

    /** The root of the leg on {@code side} (rig space). */    /** The root of the leg on {@code side} (rig space). */
    public static double[] root(int side, double rootY) {
        return new double[] {DESIGN_ROOT[0] * side, rootY, DESIGN_ROOT[2]};
    }

    /** The pose for {@code in}. */
    public static Pose pose(Input in) {
        int s = in.side() >= 0 ? 1 : -1;
        double k = in.length() / DESIGN;
        double[] root = root(s, in.rootY());
        return switch (in.act()) {
            case REST -> rest(in, s, k, root);
            case LOCK -> {
                double turn = bearing(root, in.target()) * 0.6;
                double[][] points = turn(whole(REAR, s, in.length(), root), root, turn);
                yield new Pose(fit(points, in.length(), 1, points.length - 1), turned(new double[] {s, 0.0, 0.0}, turn), in.grab() ? 0.75 : 0.35,
                    1.35, 1.6);
            }
            case STRIKE -> {
                double[] t = in.target();
                double[] p1 = design(new double[] {0.38, 1.35, -0.62}, s, k, root);
                double[] p2 = design(new double[] {0.9, 2.35, -0.62}, s, k, root);
                double[] p3 = add(lerp(p2, t, 0.4), new double[] {0.0, 1.0 * k, 0.0});
                double[] p4 = add(lerp(p2, t, 0.78), new double[] {0.0, 0.45 * k, 0.0});
                double[][] points = {root, p1, p2, p3, p4, t.clone()};
                double turn = bearing(root, t);
                yield new Pose(fit(points, in.length(), 2, 4), turned(new double[] {s, 0.0, 0.0}, turn * 0.6), in.grab() ? 1.0 : 0.3, 1.25, 1.1);
            }
            case COIL -> coil(in, s, k, root);
            case BLOCK -> {
                double turn = bearing(root, in.target()) * 0.8;
                double[][] points = turn(whole(GUARD, s, in.length(), root), root, turn);
                yield new Pose(fit(points, in.length(), 1, points.length - 1), new double[] {0.0, 1.0, 0.0}, 0.5, 1.4, 1.2);
            }
            case SLAM -> {
                double[] t = in.target();
                double[] p1 = design(new double[] {0.4, 1.6, -0.6}, s, k, root);
                double[] p2 = design(new double[] {0.95, 3.25, -0.4}, s, k, root);
                double[] p3 = design(new double[] {1.35, 3.95, 1.05}, s, k, root);
                double[] p4 = add(lerp(p3, t, 0.6), new double[] {0.0, 0.4 * k, 0.0});
                double[][] points = {root, p1, p2, p3, p4, t.clone()};
                yield new Pose(fit(points, in.length(), 4, 4), new double[] {s, 0.0, 0.0}, 0.6, 1.2, 1.0);
            }
            case THROW -> {
                double turn = bearing(root, in.target()) * 0.7;
                double[][] points = turn(whole(WHIP, s, in.length(), root), root, turn);
                yield new Pose(fit(points, in.length(), 1, points.length - 1), turned(new double[] {s, 0.0, 0.0}, turn), 1.0, 1.3, 1.0);
            }
        };
    }

    /** At rest: the design, breathing, the far end drifting where he looks, trailing a little as he runs. */
    static Pose rest(Input in, int s, double k, double[] root) {
        double[][] design = in.finalForm() ? HORN : s > 0 ? SCORPION : SWEEP;
        double[] dorsal = in.finalForm() ? new double[] {0.0, 1.0, 0.0} : s < 0 ? new double[] {0.0, 0.0, -1.0} : new double[] {1.0, 0.0, 0.0};
        double[][] points = whole(design, s, in.length(), root);
        double look = Math.max(-1.0, Math.min(1.0, in.lookYaw()));
        double pitch = Math.max(-1.0, Math.min(1.0, in.lookPitch()));
        double run = Math.max(0.0, Math.min(1.0, in.speed() / 0.28));
        double offset = s > 0 ? 0.0 : 1.7;
        for (int i = 1; i < points.length; i++) {
            double f = i / (double)(points.length - 1);
            double[] p = points[i];
            p[1] += k * (0.08 * f * Math.sin(in.time() * 0.07 + offset + f * 1.6) - 0.45 * pitch * f * f * f + 0.12 * run * f);
            p[0] += k * s * 0.06 * f * f * Math.sin(in.time() * 0.045 + 1.7 + offset);
            p[2] -= k * 0.4 * run * f;
            points[i] = turn(new double[][] {p}, root, look * 0.35 * f * f)[0];
        }
        return new Pose(fit(points, in.length(), 1, points.length - 1), dorsal, 0.0, 1.0, 6.0);
    }

    /** Wrapped once round the held body: over his shoulder, down behind it, then round it, the head shut on its front. */
    static Pose coil(Input in, int s, double k, double[] root) {
        double[] c = in.target();
        double r = Math.max(0.25, in.radius());
        double toward = Math.atan2(root[0] - c[0], root[2] - c[2]);
        double[] p1 = design(new double[] {0.4, 1.4, -0.7}, s, k, root);
        double[] p2 = design(new double[] {0.95, 2.45, -0.6}, s, k, root);
        double a0 = toward + 0.25 * s;
        double[] above = {c[0] + Math.sin(a0 - 0.6 * s) * r * 1.15, c[1] + 1.0, c[2] + Math.cos(a0 - 0.6 * s) * r * 1.15};
        int turns = 7;
        double[][] points = new double[4 + turns][];
        points[0] = root;
        points[1] = p1;
        points[2] = p2;
        points[3] = above;
        double sweep = 1.75 * Math.PI * s;
        for (int i = 0; i < turns; i++) {
            double f = (i + 1) / (double)turns;
            double a = a0 + sweep * f;
            points[4 + i] = new double[] {c[0] + Math.sin(a) * r, c[1] + 0.45 - 0.75 * f, c[2] + Math.cos(a) * r};
        }
        return new Pose(fit(points, in.length(), 2, 3), new double[] {0.0, 1.0, 0.0}, 0.0, 0.55, 2.0);
    }

    /** A whole design for {@code side}, scaled about its root to be {@code length} long and moved to {@code root}. */
    static double[][] whole(double[][] points, int side, double length, double[] root) {
        return design(points, side, length / curveLength(points), root);
    }

    /** A design (right leg's, root at DESIGN_ROOT) for {@code side}, scaled by {@code k} about the root and moved to {@code root}. */
    static double[][] design(double[][] points, int side, double k, double[] root) {
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            out[i] = design(points[i], side, k, root);
        }
        out[0] = root.clone();
        return out;
    }

    static double[] design(double[] p, int side, double k, double[] root) {
        return new double[] {root[0] + (p[0] - DESIGN_ROOT[0]) * k * side, root[1] + (p[1] - DESIGN_ROOT[1]) * k, root[2] + (p[2] - DESIGN_ROOT[2]) * k};
    }

    /** The heading from {@code root} to {@code target} (radians: 0 straight ahead, positive to his right), within +-1.2. */
    static double bearing(double[] root, double[] target) {
        if (target == null) {
            return 0.0;
        }
        double b = Math.atan2(target[0] - root[0], target[2] - root[2]);
        return Math.max(-1.2, Math.min(1.2, b));
    }

    /** Points turned about the vertical through {@code about} by {@code angle} (positive toward his right). */
    static double[][] turn(double[][] points, double[] about, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double ax = about[0];
        double az = about[2];
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double x = points[i][0] - ax;
            double z = points[i][2] - az;
            out[i] = new double[] {ax + x * c + z * s, points[i][1], az - x * s + z * c};
        }
        return out;
    }

    /** A direction turned about the vertical by {@code angle} (positive toward his right). */
    static double[] turned(double[] direction, double angle) {
        return turn(new double[][] {direction}, new double[] {0.0, 0.0, 0.0}, angle)[0];
    }

    /**
     * {@code points} made {@code length} long and resampled to {@link #POINTS} evenly spaced points. The points {@code from}..{@code to}
     * are the free ones (the points either side of them stay put): too short and they bow upward until it is long enough; too
     * long and they straighten toward the line between their neighbours, only as far as needed; if even straight it is too long,
     * the target is out of reach and the head stops short of it (the leg never stretches).
     */
    public static double[][] fit(double[][] points, double length, int from, int to) {
        double[][] shape = points;
        double current = curveLength(points);
        if (current > length) {
            double[][] straight = straighten(points, from, to, 1.0);
            if (curveLength(straight) >= length) {
                shape = straight;
            } else {
                double low = 0.0;
                double high = 1.0;
                for (int i = 0; i < 18; i++) {
                    double mid = 0.5 * (low + high);
                    if (curveLength(straighten(points, from, to, mid)) > length) {
                        low = mid;
                    } else {
                        high = mid;
                    }
                }
                shape = straighten(points, from, to, high);
            }
        } else if (current < length) {
            double low = 0.0;
            double high = 1.5 * length;
            for (int i = 0; i < 18; i++) {
                double mid = 0.5 * (low + high);
                if (curveLength(bow(points, from, to, mid)) < length) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            shape = bow(points, from, to, high);
        }
        return resample(shape, length);
    }

    /** The points {@code from}..{@code to} moved {@code amount} of the way to the straight line between their neighbours. */
    static double[][] straighten(double[][] points, int from, int to, double amount) {
        double[][] out = new double[points.length][];
        double[] a = points[Math.max(0, from - 1)];
        double[] b = points[Math.min(points.length - 1, to + 1)];
        for (int i = 0; i < points.length; i++) {
            out[i] = points[i].clone();
            if (i >= from && i <= to) {
                double f = (i - from + 1) / (double)(to - from + 2);
                out[i] = lerp(points[i], lerp(a, b, f), amount);
            }
        }
        return out;
    }

    /** The points {@code from}..{@code to} raised by {@code height} in an arch (the ends of the range barely move). */
    static double[][] bow(double[][] points, int from, int to, double height) {
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            out[i] = points[i].clone();
            if (i >= from && i <= to) {
                double f = (i - from + 1) / (double)(to - from + 2);
                out[i][1] += height * Math.sin(Math.PI * f);
            }
        }
        return out;
    }

    static final int SAMPLES = 96;

    /** The length of the spline through {@code points}. */
    public static double curveLength(double[][] points) {
        double total = 0.0;
        double[] previous = CentipedeTail.catmullRom(points, 0.0);
        for (int i = 1; i <= SAMPLES; i++) {
            double[] p = CentipedeTail.catmullRom(points, i / (double)SAMPLES);
            total += distance(p, previous);
            previous = p;
        }
        return total;
    }

    /** {@link #POINTS} points along the spline through {@code points}, evenly spaced over its first {@code length} blocks. */
    public static double[][] resample(double[][] points, double length) {
        double[][] dense = new double[SAMPLES + 1][];
        double[] along = new double[SAMPLES + 1];
        for (int i = 0; i <= SAMPLES; i++) {
            dense[i] = CentipedeTail.catmullRom(points, i / (double)SAMPLES);
            along[i] = i == 0 ? 0.0 : along[i - 1] + distance(dense[i], dense[i - 1]);
        }
        double total = Math.min(length, along[SAMPLES]);
        double[][] out = new double[POINTS][];
        int j = 0;
        for (int i = 0; i < POINTS; i++) {
            double want = total * i / (POINTS - 1);
            while (j < SAMPLES - 1 && along[j + 1] < want) {
                j++;
            }
            double span = along[j + 1] - along[j];
            double f = span < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (want - along[j]) / span));
            out[i] = lerp(dense[j], dense[j + 1], f);
        }
        return out;
    }

    /**
     * One leg following its poses: critically damped springs per point (solved exactly for each step, so any frame rate is
     * stable), the root held to his back, the next point stiffer so the base never wobbles; the back's direction, the jaw and the
     * claws follow the same way; the claws' walking wave runs on.
     */
    public static final class Leg {
        private final double[][] p = new double[POINTS][3];
        private final double[][] v = new double[POINTS][3];
        private final double[] dorsal = new double[3];
        private final double[] dorsalV = new double[3];
        private final double[] jaw = new double[2];
        private final double[] flare = new double[2];
        private double phase;
        private double last = Double.NaN;

        /** Follow {@code pose} up to {@code time} (ticks), moving at {@code speed} (blocks a tick, for the claws' wave). */
        public void follow(Pose pose, double time, double speed) {
            double[][] target = pose.points();
            if (Double.isNaN(this.last) || time < this.last - 1.0E-6 || time - this.last > 20.0) {
                for (int i = 0; i < POINTS; i++) {
                    System.arraycopy(target[i], 0, this.p[i], 0, 3);
                    java.util.Arrays.fill(this.v[i], 0.0);
                }
                System.arraycopy(pose.dorsal(), 0, this.dorsal, 0, 3);
                java.util.Arrays.fill(this.dorsalV, 0.0);
                this.jaw[0] = pose.jaw();
                this.jaw[1] = 0.0;
                this.flare[0] = pose.flare();
                this.flare[1] = 0.0;
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
                double w = i == 1 ? omega * 3.0 : omega;
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
            double[] j = spring(this.jaw[0], this.jaw[1], pose.jaw(), omega * 1.4, dt);
            this.jaw[0] = j[0];
            this.jaw[1] = j[1];
            double[] f = spring(this.flare[0], this.flare[1], pose.flare(), omega, dt);
            this.flare[0] = f[0];
            this.flare[1] = f[1];
            this.phase += dt * (0.09 + 0.3 * Math.max(0.0, Math.min(1.0, speed / 0.28)));
        }

        /** The leg's points now (rig space, root first). */
        public double[][] points() {
            double[][] out = new double[POINTS][];
            for (int i = 0; i < POINTS; i++) {
                out[i] = this.p[i].clone();
            }
            return out;
        }

        /** The back's direction now (unit, rig space). */
        public double[] dorsal() {
            double length = Math.sqrt(this.dorsal[0] * this.dorsal[0] + this.dorsal[1] * this.dorsal[1] + this.dorsal[2] * this.dorsal[2]);
            return length < 1.0E-6 ? new double[] {0.0, 1.0, 0.0} : new double[] {this.dorsal[0] / length, this.dorsal[1] / length,
                this.dorsal[2] / length};
        }

        public double jaw() {
            return Math.max(0.0, Math.min(1.0, this.jaw[0]));
        }

        public double flare() {
            return Math.max(0.0, Math.min(1.6, this.flare[0]));
        }

        /** Where the claws' walking wave is (radians). */
        public double phase() {
            return this.phase;
        }
    }

    /** One critically damped spring step, solved exactly: {value, velocity} after {@code dt} toward {@code target}. */
    static double[] spring(double value, double velocity, double target, double omega, double dt) {
        double y = value - target;
        double e = Math.exp(-omega * dt);
        double temp = (velocity + omega * y) * dt;
        return new double[] {target + (y + temp) * e, (velocity - omega * temp) * e};
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
