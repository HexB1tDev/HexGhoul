package com.hex.kagune.control.quinque;

/**
 * 3.0.0: where the tendril whip's tendril lies (pure): drawn as a Rinkaku tail is (RinkakuRibbon), from its handle's knot of flesh out.
 * Every path is {@link #POINTS} points in the investigator's own frame, from its fist: {forward, left, up} (blocks).
 *
 * <p>At rest it curls ahead of the hand, clear of the ground ({@link #rest}). A lash ({@link #lash}) throws its tip round on an arc (each blow
 * of the string its own: forehand, backhand, overhead, low, a double, a full turn of the body), the tendril reaching out to its whole length as it
 * lands and the middle trailing behind the tip, so it curls as a whip does; a wave runs down it. The Flay is six quick lashes
 * ({@link #flay}); the Bind ({@link #bind}) flies out to its foe, coils round them while it drags them in, pulls taut for the slam, and comes
 * back.
 */
public final class WhipPath {
    private WhipPath() {
    }

    public static final int POINTS = 28;

    /** Its tendril's length (blocks): a masterwork's longer. */
    public static double length(boolean master) {
        return master ? 7.0 : 6.0;
    }

    /** How thick it is where it leaves the handle (blocks; it tapers to a point). */
    public static float thickness(boolean master) {
        return master ? 0.085F : 0.072F;
    }

    /**
     * 3.1.0: a loose raised curl, from the handle's knot, dipping then turning up into its tip. {@code drop} is the knot's height:
     * the curl stays clear of the floor, even with a low hand. A little sway keeps it alive without moving the anchored root.
     */
    public static double[][] rest(double length, double drop, double time) {
        double[][] p = new double[POINTS][];
        double reach = length * 0.27;
        double depth = Math.max(0.04, Math.min(0.65, drop - 0.22));
        for (int i = 0; i < POINTS; i++) {
            double s = i / (double)(POINTS - 1);
            double angle = s * Math.PI * 1.6;
            double sway = 0.06 * Math.sin(time * 0.08 + s * 4.0) * Math.sin(Math.PI * s);
            double along = reach * 0.5 * (1.0 - Math.cos(angle)) + 0.04 * Math.sin(time * 0.08) * s * s;
            double up = -depth * Math.sin(angle);
            p[i] = new double[] {along, -0.35 * s + sway, up};
        }
        return p;
    }

    /**
     * Its {@code step}th lash, {@code t} through its swing (0..1), landing at {@code land} (0..1): its tip thrown along that blow's arc
     * ({@link #arc}), out to its whole length as it lands, its middle trailing.
     */
    public static double[][] lash(int step, double t, double land, double length, double time) {
        double p = progress(t, land);
        double e = reach(t, land);
        double[] tip = arc(step, p);
        double[] mid = arc(step, Math.max(0.0, p - 0.16));
        return curve(tip, mid, e, length, time, step);
    }

    /**
     * 3.0.1: how far through its arc a lash is (0..1), {@code t} through its swing: quickening into the blow, which lands (0.5, the arc's
     * point on its foe) as the swing does ({@code land}), slowing through its follow-through.
     */
    static double progress(double t, double land) {
        double k = Math.max(0.0, Math.min(1.0, t));
        double l = Math.max(0.05, Math.min(0.95, land));
        if (k < l) {
            return 0.5 * Math.pow(k / l, 1.6);
        }
        double y = (k - l) / (1.0 - l);
        return 0.5 + 0.5 * (1.0 - (1.0 - y) * (1.0 - y));
    }

    /** How far out the tendril reaches (0..1): drawn back at the start, its whole length as the blow lands, half back after. */
    static double reach(double t, double land) {
        double k = Math.max(0.0, Math.min(1.0, t));
        double l = Math.max(0.05, Math.min(0.95, land));
        return k < l ? 0.3 + 0.7 * ease(k / l) : 1.0 - 0.45 * ease((k - l) / (1.0 - l));
    }

    /**
     * The way its tip goes {@code p} through blow {@code step}'s arc: {yaw (+ to its left), pitch (+ up)} as a unit way {forward, left, up}.
     * 3.0.1: every arc passes through its foe (yaw and pitch near 0) halfway, as the blow lands.
     */
    static double[] arc(int step, double p) {
        double yaw;
        double pitch;
        switch (Math.floorMod(step, 6)) {
            case 0 -> { // forehand: from its right round to its left, at the chest
                yaw = Math.toRadians(-100.0 + 200.0 * p);
                pitch = Math.toRadians(10.0 - 8.0 * p);
            }
            case 1 -> { // backhand: from its left back to its right
                yaw = Math.toRadians(95.0 - 190.0 * p);
                pitch = Math.toRadians(8.0 - 6.0 * p);
            }
            case 2 -> { // overhead: up and back over its head, cracked straight at them, falling to the ground before it
                yaw = 0.0;
                pitch = Math.toRadians(p < 0.5 ? 125.0 - 245.0 * p : 2.5 - 75.0 * (p - 0.5));
            }
            case 3 -> { // low: along the ground, right to left
                yaw = Math.toRadians(-95.0 + 190.0 * p);
                pitch = Math.toRadians(-18.0);
            }
            case 4 -> { // double: across to its left (through them), wrapped round, and back across through them again
                double swing = p < 0.62 ? ease(p / 0.62) : 1.0 - 0.7 * ease((p - 0.62) / 0.38);
                yaw = Math.toRadians(-95.0 + 190.0 * swing);
                pitch = Math.toRadians(10.0);
            }
            default -> { // a whole turn round: the body turns it (its spin), the tendril out to its right, trailing the hand
                yaw = Math.toRadians(-125.0 + 20.0 * p);
                pitch = Math.toRadians(3.0);
            }
        }
        return way(yaw, pitch);
    }

    /** The Flay, {@code age} ticks in: six quick lashes, each across and back the other way. */
    public static double[][] flay(double age, double length, double time) {
        int[] hits = Arm.Ability.FLAY.hits;
        int k = 0;
        while (k < hits.length - 1 && age > hits[k] + 2.5) {
            k++;
        }
        double start = hits[k] - 4.0;
        double t = Math.max(0.0, Math.min(1.0, (age - start) / 6.5));
        double p = t < 0.62 ? ease(t / 0.62) : 1.0;
        boolean back = k % 2 == 1;
        double yaw = Math.toRadians(back ? 60.0 - 120.0 * p : -60.0 + 120.0 * p);
        double lag = Math.toRadians(back ? 60.0 - 120.0 * Math.max(0.0, p - 0.18) : -60.0 + 120.0 * Math.max(0.0, p - 0.18));
        double pitch = Math.toRadians(10.0 - 14.0 * (k % 3) / 2.0);
        double e = 0.55 + 0.45 * Math.sin(Math.min(1.0, t * 1.4) * Math.PI * 0.5);
        double end = Arm.Ability.FLAY.total;
        if (age > end - 6.0) {
            e *= Math.max(0.35, (end - age) / 6.0);
        }
        return curve(way(yaw, pitch), way(lag, pitch * 0.6), e, length, time, k);
    }

    /**
     * The Bind, {@code age} ticks in, its foe's middle at {@code target} ({forward, left, up} from the fist) and {@code girth} round
     * (blocks: half its width): thrown out to them, coiled round them while they are dragged in, pulled taut for the slam, back.
     */
    public static double[][] bind(double age, double[] target, double girth, double length, double time) {
        int[] hits = Arm.Ability.BIND.hits;
        double total = Arm.Ability.BIND.total;
        double[][] p = new double[POINTS][];
        double far = Math.max(0.5, Math.sqrt(dot(target, target)));
        double[] to = far > length ? scale(target, length / far) : target;
        if (age < hits[0]) {
            // Thrown: the tip flies out to them on a rising arc, the rest following.
            double k = ease(age / hits[0]);
            for (int i = 0; i < POINTS; i++) {
                double s = i / (double)(POINTS - 1) * k;
                double[] at = scale(to, s);
                at[2] += 0.6 * Math.sin(Math.PI * s) * (1.0 - 0.5 * k);
                at[1] += 0.25 * Math.sin(Math.PI * s * 2.0 + time * 0.3) * (1.0 - k);
                p[i] = at;
            }
            return p;
        }
        if (age > total - 4.0) {
            // Let go: it whips back to the hand.
            double k = ease((total - age) / 4.0);
            for (int i = 0; i < POINTS; i++) {
                double s = i / (double)(POINTS - 1);
                double[] at = scale(to, s * k);
                at[2] += 0.3 * Math.sin(Math.PI * s) * k;
                p[i] = at;
            }
            return p;
        }
        // Bound: straight out to them (taut for the slam, slack and waving while it drags), its last third coiled round them.
        boolean taut = age >= hits[1] - 2;
        int coil = POINTS / 3;
        int straight = POINTS - coil;
        double[] axisUp = {0.0, 0.0, 1.0};
        double[] side = norm(cross(to, axisUp));
        double[] fwd = norm(new double[] {to[0], to[1], 0.0});
        for (int i = 0; i < straight; i++) {
            double s = i / (double)(straight - 1);
            double[] at = scale(to, s * (1.0 - girth / Math.max(girth + 0.01, far)));
            double slack = taut ? 0.0 : 0.18 * Math.sin(Math.PI * s);
            at[2] += slack * Math.sin(time * 0.4) - slack;
            p[i] = add(at, scale(side, slack * Math.cos(time * 0.37 + s * 4.0)));
        }
        double[] last = p[straight - 1];
        for (int i = 0; i < coil; i++) {
            double s = (i + 1) / (double)coil;
            double a = s * Math.PI * 2.0 * 2.25;
            double r = girth * 1.05;
            double down = 0.45 - 0.9 * s;
            double[] ring = add(scale(fwd, -Math.cos(a) * r), scale(side, Math.sin(a) * r));
            double[] at = add(to, add(ring, new double[] {0.0, 0.0, down}));
            p[straight + i] = i == 0 ? midpoint(last, at) : at;
        }
        return p;
    }

    /** A whip from the fist out to {@code tip} (a unit way) at {@code e} of its {@code length}, bowed through {@code mid}'s way, a wave down it. */
    static double[][] curve(double[] tip, double[] mid, double e, double length, double time, int seed) {
        double reach = length * Math.max(0.05, e);
        double[] end = scale(tip, reach);
        end[2] -= 0.35 * (1.0 - e); // slack: its tip sags
        double[] bow = scale(mid, reach * 0.55);
        bow[2] -= 0.25 * (1.0 - e);
        double[] across = norm(cross(tip, new double[] {0.0, 0.0, 1.0}));
        double[][] p = new double[POINTS][];
        for (int i = 0; i < POINTS; i++) {
            double s = i / (double)(POINTS - 1);
            double a = (1.0 - s) * (1.0 - s);
            double b = 2.0 * (1.0 - s) * s;
            double c = s * s;
            double[] at = {b * bow[0] + c * end[0], b * bow[1] + c * end[1], b * bow[2] + c * end[2]};
            double wave = 0.07 * Math.sin(Math.PI * s) * Math.sin(s * 7.0 - time * 0.6 + seed);
            p[i] = add(at, add(scale(across, wave), new double[] {0.0, 0.0, wave * 0.5}));
            if (a == 1.0) {
                p[i] = new double[] {0.0, 0.0, 0.0};
            }
        }
        return p;
    }

    /** A unit way {forward, left, up} from a yaw (+ to its left) and a pitch (+ up). */
    static double[] way(double yaw, double pitch) {
        return new double[] {Math.cos(pitch) * Math.cos(yaw), Math.cos(pitch) * Math.sin(yaw), Math.sin(pitch)};
    }

    static double ease(double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        return k * k * (3.0 - 2.0 * k);
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] scale(double[] a, double s) {
        return new double[] {a[0] * s, a[1] * s, a[2] * s};
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] midpoint(double[] a, double[] b) {
        return scale(add(a, b), 0.5);
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] norm(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : scale(a, 1.0 / l);
    }
}
