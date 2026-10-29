package com.hex.kagune.control.eto;

import com.hex.kagune.control.ayato.UkakuModel;

/**
 * 6.2.0, the Owl's bone spikes (J held in it; pure, tested, the same on the server and every client from one seed): hunched over,
 * {@link #COUNT} huge bone spikes burst out of her back and shoulders all round her, one after another in no set order, each its own
 * length, angle and curve; held out while J is, then drawn back into her.
 *
 * <p>Each spike grows out of a place on her back ({@link Spike#px}, her torso's own pixels) and goes {@link Spike#dir} (the frame of
 * the way she faces: x to her right, y up, z ahead). The server, which has no posed body, puts its root where her hunched back is
 * ({@link #root}); every client grows it out of her back as drawn.
 */
public final class OwlSpikes {
    /** How many burst out of her. */
    public static final int COUNT = 14;
    /** Ticks a spike takes to burst out to its full length (fastest at the start), and the little way it overshoots. */
    public static final double GROW = 2.0;
    static final double OVERSHOOT = 0.06;
    /** Ticks a spike takes to draw back in once she lets go. */
    public static final double RETRACT = 7.0;
    /** The first bursts this long after J (she hunches first); each next one this much later (give or take). */
    public static final double FIRST = OwlRules.SPIKES_HUNCH;
    public static final double STAGGER = 0.45;

    /**
     * One spike: where it grows out of her (torso pixels: x to her left, y down from the top of her shoulders, z back), the way it
     * goes (unit, the frame of the way she faces), how long and thick (blocks), when it bursts out (ticks into the move), how far it
     * bows up out of its line (blocks), and its own seed (its knobs and ridges).
     */
    public record Spike(double[] px, double[] dir, double length, double radius, double erupt, double bow, int seed) {
    }

    private OwlSpikes() {
    }

    /** The spikes of one burst, from its seed (the same on every side). */
    public static Spike[] of(int seed) {
        java.util.Random random = new java.util.Random(seed * 0x9E3779B97F4A7C15L + 0x6A09E667F3BCC909L);
        int[] order = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            order[i] = i;
        }
        for (int i = COUNT - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        Spike[] spikes = new Spike[COUNT];
        for (int i = 0; i < COUNT; i++) {
            // All round her: its bearing from straight ahead (+ to her right), a little off an even spread.
            double bearing = (i + 0.5) / COUNT * Math.PI * 2.0 + (random.nextDouble() - 0.5) * 0.32;
            double ahead = Math.cos(bearing);
            double right = Math.sin(bearing);
            boolean front = ahead > 0.35;
            if (front) {
                // Ahead, out of the outside of a shoulder and splayed out past her head, never straight over it.
                double out = right > 0.12 || right >= -0.12 && (i & 1) == 0 ? 1.0 : -1.0;
                right = out * Math.max(Math.abs(right), 0.4);
                double level = Math.sqrt(ahead * ahead + right * right);
                ahead /= level;
                right /= level;
            }
            double side = Math.abs(right);
            // Ahead, they arch up over her shoulders (clear of her head); to her sides they rise off her shoulders; behind, off her back.
            double rise = front ? 0.58 + 0.32 * random.nextDouble() : side > 0.6 ? 0.22 + 0.4 * random.nextDouble() : 0.0 + 0.5 * random.nextDouble();
            if (!front && random.nextDouble() < 0.18) {
                rise += 0.55; // now and then one stands up high
            }
            double[] dir = {right * Math.cos(rise), Math.sin(rise), ahead * Math.cos(rise)};
            double length = front ? 2.8 + 1.7 * random.nextDouble() : 3.2 + 2.3 * random.nextDouble();
            double radius = (0.15 + 0.035 * length + 0.04 * random.nextDouble()) * (front ? 0.78 : 1.0);
            // Out of her back where it points: her right side for those to her right (her right is -x in her pixels), her shoulders for
            // those ahead, down her back for those behind.
            // (Ahead: out of the great masses of her shoulders, where the giant arms join her, well clear of her head.)
            double x = front ? -Math.signum(right) * (6.6 + 1.0 * random.nextDouble()) : -right * (1.6 + 2.2 * random.nextDouble());
            double y = front ? 0.4 + 0.6 * random.nextDouble() : 1.2 + (ahead < -0.4 ? 5.0 : 3.0) * random.nextDouble();
            double z = front ? 0.6 : 1.7;
            double erupt = FIRST + order[i] * STAGGER + random.nextDouble() * 0.25;
            spikes[i] = new Spike(new double[] {x, y, z}, dir, length, radius, erupt, 0.12 + 0.32 * random.nextDouble(), random.nextInt());
        }
        return spikes;
    }

    /** When the last of them is all the way out (ticks into the move). */
    public static double allOut(Spike[] spikes) {
        double last = 0.0;
        for (Spike spike : spikes) {
            last = Math.max(last, spike.erupt() + GROW);
        }
        return last;
    }

    /**
     * How far out {@code spike} is (0 in, 1 all out, a hair more as it overshoots) {@code age} ticks into the move, let go at
     * {@code letGo} ticks (0: still held): bursting out fast (fastest at the start), a little past and back; drawn in smoothly once
     * let go, never before it is all the way out.
     */
    public static double out(Spike spike, double age, double letGo) {
        double since = age - spike.erupt();
        if (since <= 0.0) {
            return 0.0;
        }
        double grown;
        if (since < GROW) {
            double u = since / GROW;
            // Out fast, a hair past its length at the end, settling back.
            grown = 1.0 - Math.pow(1.0 - u, 3.0) + OVERSHOOT * Math.sin(Math.PI * u) * u;
        } else {
            double settle = Math.min(1.0, (since - GROW) / 1.5);
            grown = 1.0 + OVERSHOOT * 0.4 * (1.0 - settle) * Math.cos(settle * Math.PI * 0.5);
        }
        if (letGo > 0.0) {
            double from = Math.max(letGo, spike.erupt() + GROW);
            double back = (age - from) / RETRACT;
            if (back > 0.0) {
                double u = Math.min(1.0, back);
                grown *= 1.0 - u * u * (3.0 - 2.0 * u);
            }
        }
        return Math.max(0.0, grown);
    }

    /** When every spike is back in, let go at {@code letGo} (ticks into the move). */
    public static double allIn(Spike[] spikes, double letGo) {
        return Math.max(letGo, allOut(spikes)) + RETRACT;
    }

    /**
     * A spike's barbs: one or two short hooked bone thorns jutting out of its sides partway along, raked back toward its root, as its
     * own bone grows out of it (they come out with it).
     */
    static void barbs(UkakuModel.Sink sink, Spike spike, double[][] spine, double[] dir, double[] bend, double[] side, double radius, double out) {
        int count = 1 + (spike.seed() >>> 3 & 1);
        for (int b = 0; b < count; b++) {
            double u = 0.34 + 0.22 * b + 0.06 * ((spike.seed() >>> (5 + b)) & 3) / 3.0;
            double grown = Math.max(0.0, Math.min(1.0, (out - u * 0.8) / 0.35));
            if (grown <= 0.05) {
                continue;
            }
            int at = (int)Math.round(u * RINGS);
            double[] base = spine[Math.max(0, Math.min(RINGS, at))];
            // Out of one side or the other, a little up, raked back toward the root.
            double around = ((spike.seed() >>> (9 + b)) & 1) == 0 ? 1.0 : -1.0;
            double[] way = OwlModel.normalize(OwlModel.add(OwlModel.add(OwlModel.mul(side, around), OwlModel.mul(bend, 0.45)), OwlModel.mul(dir, -0.5)));
            double length = spike.length() * 0.16 * grown;
            double thick = radius * (1.0 - u) * 0.7;
            double[][] barb = new double[BARB_RINGS + 1][];
            double[][] faces = new double[BARB_RINGS + 1][];
            double[] across = OwlModel.normalize(OwlModel.cross(way, dir));
            for (int i = 0; i <= BARB_RINGS; i++) {
                double v = i / (double)BARB_RINGS;
                // Hooked: curling forward again toward its point.
                barb[i] = OwlModel.add(OwlModel.add(base, OwlModel.mul(way, length * v)), OwlModel.mul(dir, length * 0.35 * v * v));
                faces[i] = across;
            }
            OwlModel.tube(sink, barb, faces, v -> {
                double r = thick * Math.pow(Math.max(0.0, 1.0 - v), 1.1) + 0.003;
                return new double[] {r, r * 0.8};
            }, BARB_SIDES, (p, n, v, w) -> OwlModel.mix(OwlModel.cel(n, BONE_HI, BONE, BONE_DARK), BONE_TIP, Math.max(0.0, (v - 0.7) / 0.3)));
        }
    }

    // ------------------------------------------------------------------ where they are, for the server

    /**
     * Her hunched back as the server takes it (the frame her torso's pixels are in at the full hunch, rig blocks, the frame of the
     * way she faces): its origin and how far each of its pixels runs; checked against the posed body by the tests.
     */
    static final double[] HUNCH_O = {0.0, 1.082, 0.5};
    static final double[] HUNCH_X = {-0.0586, 0.0, 0.0};
    static final double[] HUNCH_Y = {0.0, -0.0414, -0.0415};
    static final double[] HUNCH_Z = {0.0, 0.0415, -0.0414};

    /** Where {@code spike} grows out of her as the server takes it (rig blocks from her feet, the frame of the way she faces). */
    public static double[] root(Spike spike) {
        double[] p = spike.px();
        return new double[] {HUNCH_O[0] + HUNCH_X[0] * p[0] + HUNCH_Y[0] * p[1] + HUNCH_Z[0] * p[2],
            HUNCH_O[1] + HUNCH_X[1] * p[0] + HUNCH_Y[1] * p[1] + HUNCH_Z[1] * p[2],
            HUNCH_O[2] + HUNCH_X[2] * p[0] + HUNCH_Y[2] * p[1] + HUNCH_Z[2] * p[2]};
    }

    /** Its point, {@code out} of the way out from {@code root} (rig blocks). */
    public static double[] tip(Spike spike, double[] root, double out) {
        double reach = spike.length() * out;
        return new double[] {root[0] + spike.dir()[0] * reach, root[1] + spike.dir()[1] * reach, root[2] + spike.dir()[2] * reach};
    }

    // ------------------------------------------------------------------ how they look

    public static final int BONE = 0xFFE9E3D6;
    public static final int BONE_HI = 0xFFFFFFFF;
    public static final int BONE_DARK = 0xFF8A8276;
    public static final int BONE_TIP = 0xFF4E4740;
    public static final int FLESH = 0xFFC40F27;
    public static final int FLESH_DARK = 0xFF66050F;

    /** Rings along one spike and corners round it (a spike is cheap: fourteen of them stay well under the quad budget). */
    static final int RINGS = 7;
    static final int SIDES = 6;
    /** Each spike's barbs: how many (one or two, by its seed), and their rings and corners. */
    static final int BARB_RINGS = 3;
    static final int BARB_SIDES = 5;

    /**
     * One spike drawn {@code out} of the way out of {@code root} (rig space) along {@code dir} (rig space, unit; its bow bends it up,
     * {@code up} unit): a wicked bone horn, ridged and knobbed along its length, raw red flesh where it tears out of her, tapering to a
     * dark needle point; cel-shaded.
     */
    public static void mesh(UkakuModel.Sink sink, Spike spike, double[] root, double[] dir, double[] up, double out) {
        if (out <= 0.02) {
            return;
        }
        double length = spike.length() * out;
        double[] bend = OwlModel.normalize(OwlModel.sub(up, OwlModel.mul(dir, OwlModel.dot(up, dir))));
        double[] side = OwlModel.normalize(OwlModel.cross(dir, bend));
        double[][] spine = new double[RINGS + 1][];
        double[][] sides = new double[RINGS + 1][];
        for (int i = 0; i <= RINGS; i++) {
            double u = i / (double)RINGS;
            double arch = spike.bow() * out * 4.0 * u * (1.0 - u) + spike.bow() * out * u * u * 0.6;
            spine[i] = OwlModel.add(OwlModel.add(root, OwlModel.mul(dir, length * u)), OwlModel.mul(bend, arch));
            sides[i] = side;
        }
        double radius = spike.radius() * Math.min(1.0, 0.45 + 0.55 * out);
        int seed = spike.seed();
        barbs(sink, spike, spine, dir, bend, side, radius, out);
        OwlModel.tube(sink, spine, sides, u -> {
            // Thick out of her, a knob a little way up, ridged, tapering to a needle.
            double taper = Math.pow(Math.max(0.0, 1.0 - u), 0.9);
            double knob = 0.22 * Math.exp(-Math.pow((u - 0.18) / 0.07, 2.0));
            double ridge = 1.0 + 0.1 * Math.sin(u * 11.0 + (seed & 7));
            double r = radius * (taper * ridge + knob * (1.0 - u)) + 0.004;
            return new double[] {r, r * 0.82};
        }, SIDES, (p, n, u, v) -> {
            int c = OwlModel.cel(n, BONE_HI, BONE, BONE_DARK);
            if (u < 0.1) {
                // Where it tears out of her: raw.
                c = OwlModel.mix(OwlModel.cel(n, FLESH, FLESH, FLESH_DARK), c, u / 0.1);
            }
            if (u > 0.82) {
                c = OwlModel.mix(c, BONE_TIP, (u - 0.82) / 0.18);
            }
            // Ridges running down it, darker.
            double groove = Math.sin(v * 3.0 + u * 2.0 + (seed & 3));
            return groove > 0.75 ? OwlModel.mix(c, BONE_DARK, 0.35) : c;
        });
    }
}
