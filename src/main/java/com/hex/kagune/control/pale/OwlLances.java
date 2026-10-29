package com.hex.kagune.control.pale;

import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * 6.2.0, the Owl's Watch This (B in it; pure, tested, the same on the server and every client): {@link #COUNT} lances of her red
 * muscle, each tipped with a long barbed spear of bone, burst out of her back into a crown over her and for ten seconds hunt on their
 * own ({@link OwlRules#LANCES_ACTIVE}). Each favours its own side of her ({@link #bearing}): it rears up high and dives onto what it
 * hunts from above, the bone driven through it and on into the ground ({@link #through}); pinned there a moment, it is hoisted up
 * over her on the barbs and slammed into the ground on her far side ({@link #hoist}, {@link #slamPoint}); the lance tears free and
 * goes back to the crown. Watch This's two tentacles, grown into six, hunting from above.
 *
 * <p>Points are in her frame: blocks to her right (x), up from her feet (y), ahead of her (z); on a client, the frame her body is
 * drawn in.
 */
public final class OwlLances {
    /** What a lance is doing (sent by ordinal: new ones go at the end). */
    public enum Phase {
        /** Not out. */
        NONE,
        /** Bursting out of her back. */
        OUT,
        /** Raised in the crown over her, hunting. */
        IDLE,
        /** Rearing up high ({@link OwlRules#LANCE_REAR} ticks), then the dive at its target from above. */
        STRIKE,
        /** Through it and into the ground: pinned there. */
        PIN,
        /** Hoisted up over her on the barbs and slammed down on her far side. */
        HOIST,
        /** Torn free after the slam, back into the crown. */
        RECOVER,
        /** The dive found nothing there: out of the ground, back into the crown. */
        MISS,
        /** Drawing back into her back. */
        IN;

        public static Phase byOrdinal(int ordinal) {
            Phase[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }
    }

    /** How many burst out of her. */
    public static final int COUNT = 6;
    /** The way round her each lance hunts (radians from ahead, + to her right): ahead to either side, out to her sides, behind her. */
    static final double[] BEARING = {0.8, -0.8, 1.75, -1.75, 2.6, -2.6};
    /** The bone spear: its length (blocks) from where the muscle grips it to its point. */
    public static final double SPEAR = 2.3;
    /** How far past the middle of what it dives through the point goes on into the ground (blocks, along the spear). */
    static final double INTO_GROUND = 0.3;
    /** Where on a body it goes through (a share of its height up from its feet: the chest), as Watch This's beak does. */
    public static final double CHEST = WatchRules.CHEST;

    private OwlLances() {
    }

    /** Which side of her lance {@code i} grows out of (1 her right, -1 her left). */
    public static int side(int i) {
        return (i & 1) == 0 ? 1 : -1;
    }

    /** The way round her lance {@code i} hunts (radians from ahead, + to her right). */
    public static double bearing(int i) {
        return BEARING[Math.floorMod(i, COUNT)];
    }

    /**
     * Where lance {@code i} grows out of her back (torso pixels: x to her left, y down from the top of her shoulders, z back): in
     * three pairs either side of her spine, high, out over her shoulder blades, low.
     */
    public static double[] px(int i) {
        int row = Math.floorMod(i, COUNT) / 2;
        double out = row == 0 ? 1.5 : row == 1 ? 2.7 : 1.4;
        double down = row == 0 ? 0.9 : row == 1 ? 2.5 : 4.2;
        return new double[] {-side(i) * out, down, 1.8};
    }

    /**
     * Where lance {@code i} rears from and swings what it hoists about: up over her back and out toward its own way round her (the six
     * fanned out round her like a crown, never bunched over her head).
     */
    public static double[] pivot(int i) {
        double b = bearing(i);
        return new double[] {Math.sin(b) * 1.25, 2.85, Math.cos(b) * 1.25 - 0.45};
    }

    // ------------------------------------------------------------------ the strike

    /** The dive at something {@code distance} blocks from the lance's {@link #pivot}: the further, the longer (ticks). */
    public static int diveTicks(double distance) {
        return (int)Math.max(2, Math.min(5, Math.round(1.2 + distance * 0.28)));
    }

    /** Ticks from the start of a strike to the spear going in. */
    public static int strikeTicks(int dive) {
        return OwlRules.LANCE_REAR + dive;
    }

    /**
     * The way the spear goes into what it dives on, its chest at {@code chest}: from above, a little from her side of it (steeply
     * down, never flat: it pins it to the ground).
     */
    public static double[] diveDir(double[] chest) {
        double[] in = {-chest[0], 0.0, -chest[2]};
        double flat = Math.hypot(in[0], in[2]);
        if (flat > 1.0E-6) {
            in = new double[] {in[0] / flat, 0.0, in[2] / flat};
        }
        // Down at it from above, leaning out from her side.
        return normalize(new double[] {-in[0] * 0.38, -1.0, -in[2] * 0.38});
    }

    /**
     * Where the muscle grips the spear (its knot) when the spear is through a chest at {@code chest} along {@code dir}, the chest
     * {@code chestUp} blocks above the ground under it: the point on through it into the ground a little.
     */
    public static double[] through(double[] chest, double[] dir, double chestUp) {
        double down = Math.max(0.3, -dir[1]);
        double beyond = Math.max(0.5, Math.min(SPEAR - 0.3, chestUp / down + INTO_GROUND));
        return sub(chest, mul(dir, SPEAR - beyond));
    }

    // ------------------------------------------------------------------ the hoist and the slam

    /**
     * Where what was pinned with its chest at {@code pinned} is slammed down: on her far side, as far out from her as it was (2.5 to
     * 4.5 blocks), its chest {@code chestUp} above the ground there ({@code ground}: the ground's height there, her frame).
     */
    public static double[] slamPoint(double[] pinned, double chestUp, double ground) {
        double flat = Math.hypot(pinned[0], pinned[2]);
        double[] away = flat < 1.0E-6 ? new double[] {0.0, 0.0, -1.0} : new double[] {-pinned[0] / flat, 0.0, -pinned[2] / flat};
        double out = Math.max(2.5, Math.min(4.5, flat));
        return new double[] {away[0] * out, ground + chestUp, away[2] * out};
    }

    /** How high (blocks) over the pin and over the slam the hoist's curve is drawn: it carries the body well up over her. */
    static final double HOIST_UP = 5.0;
    static final double SLAM_UP = 5.6;

    /**
     * Where the chest of what was pinned at {@code pinned} is {@code u} of the way through the hoist (0 pinned, 1 slammed down at
     * {@code slam}): torn up out of the ground, carried up over her and brought down on her far side.
     */
    public static double[] hoist(double u, double[] pinned, double[] slam) {
        double t = Math.max(0.0, Math.min(1.0, u));
        double[] c1 = add(pinned, new double[] {0.0, HOIST_UP, 0.0});
        double[] c2 = add(slam, new double[] {0.0, SLAM_UP, 0.0});
        double s = 1.0 - t;
        return add(add(mul(pinned, s * s * s), mul(c1, 3.0 * s * s * t)), add(mul(c2, 3.0 * s * t * t), mul(slam, t * t * t)));
    }

    /** How far through the hoist it is {@code ticks} after it began: heavy up off the ground, then brought down hard and fast. */
    public static double hoistU(double ticks) {
        double t = Math.max(0.0, Math.min(1.0, ticks / OwlRules.LANCE_HOIST));
        return Math.pow(t, 1.7);
    }

    // ------------------------------------------------------------------ where a lance is

    /** Where a lance's spear is gripped ({@code knot}), the way it points, how far out the lance is (0..1). */
    public record Tip(double[] knot, double[] dir, double grown) {
    }

    /** Raised in the crown, at rest: up over her back, the spear angled out and down toward its own way round her, alive. */
    public static Tip crown(int i, double time) {
        double b = bearing(i);
        double phase = i * 1.37;
        double[] out = {Math.sin(b), 0.0, Math.cos(b)};
        double[] knot = add(pivot(i), new double[] {out[0] * (0.45 + 0.06 * Math.sin(time * 0.15 + phase)), 0.3 + 0.08 * Math.sin(time * 0.19 + phase),
            out[2] * (0.45 + 0.06 * Math.sin(time * 0.15 + phase))});
        double[] dir = normalize(new double[] {out[0] + 0.08 * Math.sin(time * 0.21 + phase), -0.55 + 0.07 * Math.sin(time * 0.17 + phase),
            out[2] + 0.08 * Math.cos(time * 0.23 + phase)});
        return new Tip(knot, dir, 1.0);
    }

    /**
     * Lance {@code i} in {@code phase}, {@code age} ticks into it ({@code dive}: its strike's dive ticks): {@code chest} where what it
     * strikes, pins or hoists has its chest now (null: nothing), {@code chestUp} how far that chest is above its feet, {@code from}
     * where its point was when this phase began (after a slam or a miss), {@code time} for its life.
     */
    public static Tip tip(int i, Phase phase, double age, int dive, double[] chest, double chestUp, double[] from, double time) {
        Tip rest = crown(i, time);
        double[] pivot = pivot(i);
        switch (phase) {
            case OUT -> {
                return new Tip(rest.knot(), rest.dir(), smooth(age / OwlRules.LANCES_ERUPT));
            }
            case IN -> {
                return new Tip(rest.knot(), rest.dir(), 1.0 - smooth(age / OwlRules.LANCES_RETRACT));
            }
            case STRIKE -> {
                if (chest == null) {
                    return rest;
                }
                double[] dir = diveDir(chest);
                double[] in = through(chest, dir, chestUp);
                // Reared up high over its side of her, the spear already turned down at its mark...
                double[] toward = {chest[0] - pivot[0], 0.0, chest[2] - pivot[2]};
                double flat = Math.max(1.0E-6, Math.hypot(toward[0], toward[2]));
                double[] high = add(pivot, new double[] {toward[0] / flat * 0.5, 1.7, toward[2] / flat * 0.5});
                double[] aim = normalize(sub(chest, high));
                if (age < OwlRules.LANCE_REAR) {
                    double u = smooth(age / OwlRules.LANCE_REAR);
                    return new Tip(lerp(rest.knot(), high, u), normalize(lerp(rest.dir(), aim, u)), 1.0);
                }
                // ...then the dive, still speeding up as it goes in: over the top onto it, the spear coming down along its line.
                double v = clamp01((age - OwlRules.LANCE_REAR) / Math.max(1, dive));
                double e = Math.pow(v, 2.0);
                double[] over = add(lerp(high, in, 0.45), new double[] {0.0, 0.9, 0.0});
                double[] knot = bezier(high, over, in, e);
                return new Tip(knot, normalize(lerp(aim, dir, smooth(v))), 1.0);
            }
            case PIN -> {
                if (chest == null) {
                    return rest;
                }
                double[] dir = diveDir(chest);
                return new Tip(through(chest, dir, chestUp), dir, 1.0);
            }
            case HOIST -> {
                if (chest == null) {
                    return rest;
                }
                // Through it from above and out under it, the muscle hauling from over her back.
                double[] dir = normalize(add(normalize(sub(chest, pivot)), new double[] {0.0, -0.7, 0.0}));
                double turn = smooth(age / 2.0);
                dir = normalize(lerp(diveDir(chest), dir, turn));
                return new Tip(sub(chest, mul(dir, SPEAR * 0.38)), dir, 1.0);
            }
            case RECOVER, MISS -> {
                if (from == null) {
                    return rest;
                }
                double ticks = phase == Phase.RECOVER ? OwlRules.LANCE_RECOVER : OwlRules.LANCE_MISS;
                double u = clamp01(age / ticks);
                // Torn up out of the ground first (straight up the way it went in), then back into the crown.
                double[] dir = normalize(sub(from, pivot));
                double[] knot = sub(from, mul(dir, SPEAR));
                double[] torn = add(knot, new double[] {0.0, 1.2 * smooth(u / 0.35), 0.0});
                double back = smooth((u - 0.2) / 0.8);
                return new Tip(lerp(torn, rest.knot(), back), normalize(lerp(dir, rest.dir(), back)), 1.0);
            }
            default -> {
                return rest;
            }
        }
    }

    /** The point of a lance at {@code tip} (where the spear ends). */
    public static double[] point(Tip tip) {
        return add(tip.knot(), mul(tip.dir(), SPEAR * spearOut(tip.grown())));
    }

    /** How much of the spear is out: it tears out of her first, leading the muscle, all out by the time the muscle is half out. */
    public static double spearOut(double grown) {
        return 0.35 + 0.65 * smooth(grown / 0.5);
    }

    // ------------------------------------------------------------------ how they look

    public static final int MUSCLE = OwlModel.MUSCLE;
    public static final int MUSCLE_HI = OwlModel.MUSCLE_HI;
    public static final int MUSCLE_DARK = OwlModel.MUSCLE_DARK;
    public static final int FIBRE = OwlModel.FIBRE;
    /** Rings a block along the muscle (the fewest and most), corners round it; the spear's rings and corners; each barb's. */
    static final double RINGS_A_BLOCK = 4.5;
    static final int RINGS_LEAST = 10;
    static final int RINGS_MOST = 28;
    static final int SIDES = 7;
    static final int SPEAR_RINGS = 8;
    static final int SPEAR_SIDES = 6;
    static final int BARBS = 3;
    static final int BARB_RINGS = 2;
    static final int BARB_SIDES = 4;

    /**
     * One lance into {@code sink}: its muscle along {@code path} (rig space, root in her back first, the knot that grips the spear
     * last), as far out as {@code grown} (0..1, root first), the spear on out of the knot along {@code dir}: red muscle, corded and
     * swelling out of her, gripping the spear in a knot; the spear bone white, ridged, three barbs swept back off it, a dark needle
     * point; cel-shaded.
     */
    public static void mesh(UkakuModel.Sink sink, double[][] path, double[] dir, double grown, int seed, double time) {
        if (grown <= 0.01 || path == null || path.length < 2) {
            return;
        }
        double[][] smooth = resample(path, grown);
        if (smooth == null) {
            return;
        }
        double run = 0.0;
        for (int i = 1; i < smooth.length; i++) {
            run += length(sub(smooth[i], smooth[i - 1]));
        }
        double length = run;
        int rings = (int)Math.max(RINGS_LEAST, Math.min(RINGS_MOST, Math.ceil(length * RINGS_A_BLOCK)));
        double[][] spine = even(smooth, rings);
        double[][] sides = transported(spine);
        double fat = 0.55 + 0.45 * clamp01(grown);
        double knotted = clamp01((grown - 0.6) / 0.4);
        OwlModel.tube(sink, spine, sides, u -> {
            // Narrow out of her, swelling a little way out, corded, tapering, then the knot gripping the spear.
            double swell = 0.11 + 0.065 * Math.sin(Math.PI * Math.min(1.0, u * 1.6)) * (u < 0.62 ? 1.0 : 1.0 - smooth((u - 0.62) / 0.3));
            double knot = 0.07 * knotted * Math.exp(-Math.pow((u - 0.97) / 0.06, 2.0));
            double cord = 1.0 + 0.07 * Math.sin(u * length * 6.0 + (seed & 7) + time * 0.12);
            double r = fat * (swell + knot) * cord;
            return new double[] {r, r * 0.9};
        }, SIDES, (p, n, u, v) -> {
            int c = OwlModel.cel(n, MUSCLE_HI, MUSCLE, MUSCLE_DARK);
            // Fibres running along it, darker.
            double fibre = Math.sin(v * 4.0 + u * 3.0 + (seed & 3));
            return fibre > 0.7 ? OwlModel.mix(c, FIBRE, 0.5) : c;
        });
        double out = spearOut(grown);
        if (out <= 0.02) {
            return;
        }
        // The spear leads: while it is still coming out (or going back in) it is at the end of what is out, the way the muscle goes
        // there, turning to the way it will point as it comes all the way out.
        int last = spine.length - 1;
        double[] along = normalize(sub(spine[last], spine[Math.max(0, last - 2)]));
        double[] way = normalize(lerp(along, normalize(dir), smooth((grown - 0.55) / 0.45)));
        spear(sink, spine[last], way, sides[last], out, seed);
    }

    /** The bone spear out of the knot at {@code from} along {@code dir}, {@code out} of it out. */
    static void spear(UkakuModel.Sink sink, double[] from, double[] dir, double[] across, double out, int seed) {
        double length = SPEAR * out;
        double[] side = normalize(sub(across, mul(dir, dot(across, dir))));
        double[] other = cross(dir, side);
        double[][] spine = new double[SPEAR_RINGS + 1][];
        double[][] sides = new double[SPEAR_RINGS + 1][];
        for (int i = 0; i <= SPEAR_RINGS; i++) {
            double u = i / (double)SPEAR_RINGS;
            // A hair of a hook toward its point.
            spine[i] = add(add(from, mul(dir, length * u)), mul(other, -0.06 * length * u * u * u));
            sides[i] = side;
        }
        OwlModel.tube(sink, spine, sides, u -> {
            double socket = 0.105 + 0.03 * Math.exp(-Math.pow((u - 0.1) / 0.07, 2.0));
            double taper = Math.pow(Math.max(0.0, 1.0 - u), 0.85);
            double r = socket * taper * Math.min(1.0, 0.5 + 0.5 * out) + 0.004;
            return new double[] {r, r * 0.8};
        }, SPEAR_SIDES, (p, n, u, v) -> {
            int c = OwlModel.cel(n, OwlSpikes.BONE_HI, OwlSpikes.BONE, OwlSpikes.BONE_DARK);
            if (u < 0.06) {
                c = OwlModel.mix(OwlModel.cel(n, MUSCLE, MUSCLE, MUSCLE_DARK), c, u / 0.06);
            }
            if (u > 0.8) {
                c = OwlModel.mix(c, OwlSpikes.BONE_TIP, (u - 0.8) / 0.2);
            }
            return Math.sin(v * 3.0 + u * 2.5 + (seed & 3)) > 0.78 ? OwlModel.mix(c, OwlSpikes.BONE_DARK, 0.35) : c;
        });
        if (out < 0.6) {
            return;
        }
        // Barbs swept back off it toward the knot, a third of the way round from each other.
        for (int b = 0; b < BARBS; b++) {
            double at = 0.48 + 0.12 * b;
            double angle = b * Math.PI * 2.0 / BARBS + (seed & 7) * 0.4;
            double[] round = add(mul(side, Math.cos(angle)), mul(other, Math.sin(angle)));
            double radius = 0.105 * Math.pow(1.0 - at, 0.85);
            double[] base = add(spine[0], mul(dir, length * at));
            base = add(base, mul(round, radius * 0.6));
            double[] way = normalize(add(mul(round, 1.0), mul(dir, -1.1)));
            double barb = 0.24 * out * (1.0 - 0.2 * b);
            double[][] line = {base, add(base, mul(way, barb * 0.5)), add(base, mul(way, barb))};
            double[][] across2 = {dir, dir, dir};
            OwlModel.tube(sink, line, across2, u -> {
                double r = 0.045 * (1.0 - u) + 0.003;
                return new double[] {r, r * 0.7};
            }, BARB_SIDES, (p, n, u, v) -> OwlModel.mix(OwlModel.cel(n, OwlSpikes.BONE_HI, OwlSpikes.BONE, OwlSpikes.BONE_DARK), OwlSpikes.BONE_TIP, u * 0.7));
        }
    }

    /** {@code path} as a smooth curve through its points (Catmull-Rom), only as far as {@code grown} of its length from the root. */
    static double[][] resample(double[][] path, double grown) {
        int dense = 64;
        double[][] curve = new double[dense + 1][];
        int n = path.length - 1;
        for (int k = 0; k <= dense; k++) {
            double t = k / (double)dense * n;
            int i = Math.min(n - 1, (int)Math.floor(t));
            double f = t - i;
            double[] p0 = path[Math.max(0, i - 1)];
            double[] p1 = path[i];
            double[] p2 = path[i + 1];
            double[] p3 = path[Math.min(n, i + 2)];
            curve[k] = catmull(p0, p1, p2, p3, f);
        }
        double[] lengths = new double[dense + 1];
        for (int k = 1; k <= dense; k++) {
            lengths[k] = lengths[k - 1] + length(sub(curve[k], curve[k - 1]));
        }
        double end = lengths[dense] * clamp01(grown);
        if (end < 0.03) {
            return null;
        }
        int last = 1;
        while (last < dense && lengths[last] < end) {
            last++;
        }
        double[][] out = new double[last + 1][];
        System.arraycopy(curve, 0, out, 0, last);
        double span = Math.max(1.0E-9, lengths[last] - lengths[last - 1]);
        out[last] = lerp(curve[last - 1], curve[last], clamp01((end - lengths[last - 1]) / span));
        return out;
    }

    /** {@code curve} as {@code rings + 1} points evenly spaced along it. */
    static double[][] even(double[][] curve, int rings) {
        double[] lengths = new double[curve.length];
        for (int k = 1; k < curve.length; k++) {
            lengths[k] = lengths[k - 1] + length(sub(curve[k], curve[k - 1]));
        }
        double total = lengths[curve.length - 1];
        double[][] out = new double[rings + 1][];
        int j = 1;
        for (int i = 0; i <= rings; i++) {
            double want = total * i / rings;
            while (j < curve.length - 1 && lengths[j] < want) {
                j++;
            }
            double span = Math.max(1.0E-9, lengths[j] - lengths[j - 1]);
            out[i] = lerp(curve[j - 1], curve[j], clamp01((want - lengths[j - 1]) / span));
        }
        return out;
    }

    /** A way square to {@code spine} at each of its points, carried along it without twisting (so the muscle never wrings). */
    static double[][] transported(double[][] spine) {
        int n = spine.length;
        double[][] sides = new double[n][];
        double[] t0 = normalize(sub(spine[1], spine[0]));
        double[] side = Math.abs(t0[1]) < 0.9 ? cross(t0, new double[] {0.0, 1.0, 0.0}) : cross(t0, new double[] {1.0, 0.0, 0.0});
        side = normalize(side);
        sides[0] = side;
        double[] last = t0;
        for (int i = 1; i < n; i++) {
            double[] t = normalize(sub(spine[Math.min(n - 1, i + 1)], spine[i - 1]));
            double[] axis = cross(last, t);
            double s = Math.sqrt(dot(axis, axis));
            if (s > 1.0E-9) {
                double c = dot(last, t);
                double[] k = mul(axis, 1.0 / s);
                side = add(add(mul(side, c), mul(cross(k, side), s)), mul(k, dot(k, side) * (1.0 - c)));
            }
            side = normalize(sub(side, mul(t, dot(side, t))));
            sides[i] = side;
            last = t;
        }
        return sides;
    }

    static double[] catmull(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double[] r = new double[3];
        for (int k = 0; k < 3; k++) {
            r[k] = 0.5 * (2.0 * p1[k] + (-p0[k] + p2[k]) * t + (2.0 * p0[k] - 5.0 * p1[k] + 4.0 * p2[k] - p3[k]) * t2
                + (-p0[k] + 3.0 * p1[k] - 3.0 * p2[k] + p3[k]) * t3);
        }
        return r;
    }

    /** The quadratic curve from {@code a} through toward {@code b} to {@code c}, {@code t} of the way. */
    static double[] bezier(double[] a, double[] b, double[] c, double t) {
        return add(add(mul(a, (1.0 - t) * (1.0 - t)), mul(b, 2.0 * t * (1.0 - t))), mul(c, t * t));
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

    static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-9 ? new double[] {0.0, -1.0, 0.0} : mul(a, 1.0 / l);
    }

    static double clamp01(double u) {
        return Math.max(0.0, Math.min(1.0, u));
    }

    static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3.0 - 2.0 * c);
    }
}
