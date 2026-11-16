package com.hex.kagune.control;

/**
 * 3.1.5 (pure, tested; the owner: "When someone is not being grabbed by the Kagune ... one of them is going to be impale, which is one
 * random Kagune ... stretches further than ... all the others can ... dashes forward in the line, and like with its end sharpened, and
 * impales whoever or whatever ... was in the way. This ability should drag mobs and players. It is a fast ability as well. And yeah,
 * it's also a held, so ... the longer you hold it, the longer it goes"): Rinkaku's Impale, on the Crush key (H) whenever no tail holds
 * anyone.
 *
 * <p>One random tail ({@link #pick}: never a centipede leg) rises over its own shoulder ({@link #launch}) and shoots out straight where
 * he looks, its end drawn to a point. While the key is held it keeps going ({@link #SPEED} blocks a tick, up to {@link #reach}, far past
 * any grab or strike); a tap still runs it out {@link #TAP}. Whatever its point runs through is run through and carried on it, newest
 * nearest the point ({@link #slot}). If the point meets a wall it stops there, and whoever it carries is pinned against it. Let go (or
 * after {@link #MOST_OUT} ticks) it tears back out of them ({@link #RETRACT} ticks), and they drop where they are.
 */
public final class RinkakuImpale {
    private RinkakuImpale() {
    }

    /** How fast its point goes out (blocks a tick): the whole reach in well under a second. */
    public static final double SPEED = 2.1;
    /** How far a tap still runs it out (blocks from the shoulder). */
    public static final double TAP = 6.0;
    /** The most ticks it stays out, held or not, before it tears back out. */
    public static final int MOST_OUT = 70;
    /** Ticks it stays at its full length after it is let go before it comes back (a beat to see what it caught). */
    public static final int LINGER = 3;
    /** Ticks it takes to come back. */
    public static final int RETRACT = 6;
    /** How long a press or a held ping keeps it going (ticks): the client pings every 4 while the key is held. */
    public static final int HELD = 10;
    /** Ticks before it can go again, from the moment it is back (the original's cooldown scaling applies on top). */
    public static final int COOLDOWN = 36;
    /** RC it costs to send out. */
    public static final float COST = 1.4F;
    /** How far its point reaches either side of its line to run something through (blocks, added to the body's half width). */
    public static final double POINT = 0.35;
    /** Where the bodies it carries sit: the newest this far behind its point, each one before it this much further up its shaft. */
    public static final double TIP_BACK = 0.55;
    public static final double SPACING = 0.95;
    /** Never carried closer to him than this (blocks from the shoulder). */
    public static final double NEAREST = 1.6;
    /** A body held back this far from its place on the shaft (caught on the ground, a ledge) tears free. */
    public static final double TEARS_FREE = 2.6;
    /** How fast its line can follow his look (degrees a tick): a heavy spear, not a laser. */
    public static final double TURN = 9.0;
    /** The most bodies it carries at once (more are still run through, but slide off). */
    public static final int MOST_CARRIED = 5;

    /** How far it can go at {@code mastery}: 16 blocks at 0, 30 at 1000 (a grab reaches 8.5 to 15). */
    public static double reach(int mastery) {
        return 16.0 + 14.0 * clamp01(mastery / 1000.0);
    }

    /** What running a body through does, of the tail's swipe damage. A wall it is pinned to adds {@link #PIN}, tearing out {@link #TEAR}. */
    public static final float THROUGH = 1.5F;
    public static final float PIN = 0.6F;
    public static final float TEAR = 0.4F;

    /**
     * Its length a tick on from {@code length}: out by {@link #SPEED} while the key is {@code held} (and, let go, until it has run a tap's
     * {@link #TAP}), never past {@code reach}; otherwise as it is.
     */
    public static double step(double length, boolean held, double reach) {
        double now = Math.max(0.0, length);
        if (held || now < Math.min(TAP, reach)) {
            return Math.min(reach, now + SPEED);
        }
        return Math.min(reach, now);
    }

    /** True once it has gone as far as it will: let go (not {@code held}) and past a tap, or at its {@code reach}. */
    public static boolean spent(double length, boolean held, double reach) {
        return length >= reach - 1.0E-9 || !held && length >= Math.min(TAP, reach) - 1.0E-9;
    }

    /** The share of its length left {@code age} ticks into coming back (1 as it starts back, 0 home), smooth. */
    public static double back(double age) {
        if (!(age > 0.0)) {
            return 1.0;
        }
        if (age >= RETRACT) {
            return 0.0;
        }
        double t = age / RETRACT;
        return 1.0 - t * t * (3.0 - 2.0 * t);
    }

    /**
     * Where the {@code k}th of {@code count} bodies it carries sits along it (blocks from the shoulder), its point {@code length} out: the
     * first caught furthest up its shaft, the newest nearest its point.
     */
    public static double slot(int k, int count, double length) {
        double along = length - TIP_BACK - SPACING * (count - 1 - k);
        return Math.max(Math.min(NEAREST, length), along);
    }

    /**
     * Which tail goes: one of those {@code free} (true for a tail that is out, is a tail and not a centipede leg, and is doing nothing
     * else) chosen by {@code roll} (0 inclusive .. 1 exclusive); -1 if none is free.
     */
    public static int pick(boolean[] free, double roll) {
        int count = 0;
        for (boolean f : free) {
            count += f ? 1 : 0;
        }
        if (count == 0) {
            return -1;
        }
        int want = Math.min(count - 1, Math.max(0, (int)Math.floor(roll * count)));
        for (int i = 0; i < free.length; i++) {
            if (free[i] && want-- == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Where it leaves over his shoulder, from his feet ({@code feet}), {@code height} tall, the tail's {@code side} (RinkakuRoot.side: its
     * own side times his right, {@code rightX}/{@code rightZ}; {@code forwardX}/{@code forwardZ} his facing), {@code own} in his own first
     * person (lower, out of his eyes).
     */
    public static double[] launch(double[] feet, double height, double side, double rightX, double rightZ, double forwardX, double forwardZ,
                                  boolean own) {
        double s = side >= 0.0 ? 1.0 : -1.0;
        // Just over the top of the shoulder, beside the head (a player's shoulders are 1.5 of his 1.8, his head 0.25 either side).
        double out = own ? 0.62 : 0.52;
        double up = height * (own ? 0.74 : 0.87);
        return new double[] {feet[0] + rightX * s * out + forwardX * 0.18, feet[1] + up, feet[2] + rightZ * s * out + forwardZ * 0.18};
    }

    /** {@code from} turned toward {@code to} (both unit vectors) by at most {@code degrees}; a unit vector. */
    public static double[] turn(double[] from, double[] to, double degrees) {
        double[] a = unit(from);
        double[] b = unit(to);
        double cos = Math.max(-1.0, Math.min(1.0, a[0] * b[0] + a[1] * b[1] + a[2] * b[2]));
        double angle = Math.acos(cos);
        double most = Math.toRadians(Math.max(0.0, degrees));
        if (angle <= most || angle < 1.0E-9) {
            return b;
        }
        // Slerp a share of the way.
        double t = most / angle;
        double sin = Math.sin(angle);
        double wa = Math.sin((1.0 - t) * angle) / sin;
        double wb = Math.sin(t * angle) / sin;
        return unit(new double[] {a[0] * wa + b[0] * wb, a[1] * wa + b[1] * wb, a[2] * wa + b[2] * wb});
    }

    /**
     * True if its point, going from {@code a} to {@code b} this tick, runs through a body standing at {@code x}/{@code z} from {@code y0}
     * (its feet) to {@code y1} (its head), {@code half} its half width.
     */
    public static boolean through(double[] a, double[] b, double x, double y0, double y1, double z, double half) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int)Math.ceil(length / 0.2));
        double reach = half + POINT;
        for (int i = 0; i <= steps; i++) {
            double f = i / (double)steps;
            double px = a[0] + dx * f;
            double py = a[1] + dy * f;
            double pz = a[2] + dz * f;
            if (Math.abs(px - x) <= reach && Math.abs(pz - z) <= reach && py >= y0 - POINT && py <= y1 + POINT) {
                return true;
            }
        }
        return false;
    }

    /**
     * The tail as drawn while it impales: {@code n} points from its {@code root} (where it leaves his back) rising up and out over his
     * shoulder to {@code launch}, then straight along {@code way} (a unit vector) {@code length} blocks to its point; {@code side} its side
     * times his right ({@code rightX}/{@code rightZ}), {@code forwardX}/{@code forwardZ} his facing. Its points are spread by length.
     */
    public static double[][] spear(double[] root, double[] launch, double[] way, double length, int n, double side, double rightX, double rightZ,
                                   double forwardX, double forwardZ) {
        int count = Math.max(4, n);
        double s = side >= 0.0 ? 1.0 : -1.0;
        // The rise: a curve up behind the shoulder, then over it (a quadratic: root, a point above and behind, the launch).
        double[] bend = {root[0] * 0.35 + launch[0] * 0.65 + rightX * s * 0.18 - forwardX * 0.35, Math.max(root[1], launch[1]) + 0.3,
            root[2] * 0.35 + launch[2] * 0.65 + rightZ * s * 0.18 - forwardZ * 0.35};
        double rise = distance(root, bend) + distance(bend, launch);
        double run = Math.max(0.0, length);
        double total = rise + run;
        double[][] out = new double[count][];
        for (int i = 0; i < count; i++) {
            double at = total * i / (count - 1);
            if (at <= rise || run <= 1.0E-9) {
                double t = rise <= 1.0E-9 ? 1.0 : Math.min(1.0, at / rise);
                double u = 1.0 - t;
                out[i] = new double[] {u * u * root[0] + 2.0 * u * t * bend[0] + t * t * launch[0],
                    u * u * root[1] + 2.0 * u * t * bend[1] + t * t * launch[1], u * u * root[2] + 2.0 * u * t * bend[2] + t * t * launch[2]};
            } else {
                double d = at - rise;
                out[i] = new double[] {launch[0] + way[0] * d, launch[1] + way[1] * d, launch[2] + way[2] * d};
            }
        }
        out[0] = root.clone();
        return out;
    }

    /** {@code path} blended toward {@code to} by {@code w} (0 .. 1), its root kept; a new array. */
    public static double[][] blend(double[][] path, double[][] to, double w) {
        double k = clamp01(w);
        double[][] out = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            double[] p = path[i];
            double[] q = to[Math.min(i, to.length - 1)];
            double m = i == 0 ? 0.0 : k;
            out[i] = new double[] {p[0] + (q[0] - p[0]) * m, p[1] + (q[1] - p[1]) * m, p[2] + (q[2] - p[2]) * m};
        }
        return out;
    }

    // ---------------------------------------------------------------- its sharpened end

    /** How long its point is (blocks): from where it is broadest to nothing. */
    public static final double POINT_LENGTH = 1.5;
    /** How far from its root it is full width (blocks): the rest of its stretched length one broad blade. */
    public static final double SHAFT = 2.4;
    /** How much broader it swells just behind its point (a spearhead), of its full width. */
    public static final double HEAD = 1.12;

    /**
     * How wide the impaling tail is {@code t} along it (0 its root .. 1 its point), {@code length} blocks long, of its thickness, as
     * {@code RinkakuRibbon.profile} gives the rest: thin where it leaves him, full width {@link #SHAFT} out, a little broader just behind
     * its end, and from there drawn to a point over {@link #POINT_LENGTH}. 0 at its very end.
     */
    public static double spearWidth(double t, double length, double peak, double rootWidth) {
        double c = clamp01(t);
        double total = Math.max(1.0E-3, length);
        double tip = Math.min(0.45, POINT_LENGTH / total);
        double broadest = 1.0 - tip;
        double grow = Math.min(broadest, SHAFT / total);
        if (c <= grow) {
            return peak * (rootWidth + (1.0 - rootWidth) * Math.pow(c / Math.max(1.0E-9, grow), 0.9));
        }
        if (c <= broadest) {
            double s = (c - grow) / Math.max(1.0E-9, broadest - grow);
            // full width along the shaft, swelling into the spearhead over its last stretch
            double head = s < 0.8 ? 1.0 : 1.0 + (HEAD - 1.0) * Math.sin((s - 0.8) / 0.2 * Math.PI * 0.5);
            return peak * head;
        }
        double x = (c - broadest) / tip;
        if (c >= 1.0 || x >= 1.0 - 1.0E-12) {
            return 0.0; // its very point
        }
        return peak * HEAD * Math.pow(1.0 - x, 1.25);
    }

    private static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double[] unit(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return l < 1.0E-12 ? new double[] {0.0, 0.0, 1.0} : new double[] {v[0] / l, v[1] / l, v[2] / l};
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
