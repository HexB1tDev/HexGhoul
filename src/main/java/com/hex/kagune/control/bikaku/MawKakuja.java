package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.KaguneTint;
import com.hex.kagune.control.graft.GraftMesh;
import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * 3.5.4, the Bikaku kakuja (the owner's spec: "an enormous, grotesque, irregular mass of living kagune flesh ... a gigantic, pulsating
 * biological growth, covered in monstrous mouths and surrounded by enormous tentacles ... visibly asymmetrical ... its main body
 * approximately 4-5 Minecraft blocks tall and its tentacles extending considerably farther"), as a mesh in his body's frame (pure:
 * the renderer, the previews and the tests build it the same way).
 *
 * <p>Its body is a heap of fleshy lumps of different sizes piled round him, lopsided, each breathing on its own rhythm, glowing seams of RC
 * running over them; he is inside it. Across it, lipped mouths grin with rows of big flat teeth, each opening and shutting on its own;
 * low on its front one huge round maw (the worm's own mouth, swollen: its crown of fangs, its rings of hooked teeth, its dark throat).
 * Six huge tentacles grow out of it every way, curling and swaying, five ending in the worm's toothed mouth, one tapering. Everything of
 * its flesh is in the owner's kagune colour; teeth, fangs and throats keep their own.
 *
 * <p>Its transformation ({@link #growth}): the worm swells and thrashes; thinner tendrils wrap round him; the lumps swell up out of them
 * and fuse into the mass that swallows him; the mouths open across it; the tentacles burst out. Leaving it, the reverse, faster.
 */
public final class MawKakuja {
    private MawKakuja() {
    }

    /** How long growing into it and coming out of it take (ticks). */
    public static final int IN = 60;
    public static final int OUT = 30;

    /** One lump of its body: its middle and radii (blocks, body frame), its own seed. */
    record Lump(double[] centre, double[] radii, int seed) {
    }

    /** Piled round him, heaped higher to his left and back, swollen out over his right shoulder, a lip of flesh low in front. */
    static final Lump[] LUMPS = {
        new Lump(new double[] {0.1, 1.9, -0.15}, new double[] {1.75, 1.95, 1.55}, 3),
        new Lump(new double[] {-0.55, 3.55, -0.45}, new double[] {1.25, 1.05, 1.15}, 11),
        new Lump(new double[] {1.45, 2.6, 0.05}, new double[] {0.95, 1.0, 0.9}, 19),
        new Lump(new double[] {-1.35, 1.1, 0.35}, new double[] {0.95, 0.9, 1.0}, 23),
        new Lump(new double[] {0.4, 2.3, -1.4}, new double[] {1.2, 1.25, 0.9}, 29),
        new Lump(new double[] {0.2, 0.5, 0.6}, new double[] {1.4, 0.55, 1.1}, 37),
        new Lump(new double[] {0.6, 4.3, -0.1}, new double[] {0.7, 0.55, 0.65}, 41)};

    /** A grinning mouth: the lump it opens on, which way from its middle, how wide (blocks), how often it opens. */
    record Grin(int lump, double[] way, double width, double rate, double phase) {
    }

    static final Grin[] GRINS = {
        new Grin(0, new double[] {0.05, 0.5, 1.0}, 2.1, 0.09, 0.0),
        new Grin(0, new double[] {-0.95, 0.05, 0.45}, 1.4, 0.12, 1.7),
        new Grin(2, new double[] {0.8, 0.3, 0.55}, 1.2, 0.1, 3.1),
        new Grin(1, new double[] {-0.25, 0.5, 0.85}, 1.3, 0.14, 4.4),
        new Grin(3, new double[] {-0.5, 0.2, 0.85}, 1.0, 0.11, 2.2),
        new Grin(4, new double[] {0.7, 0.25, -0.7}, 1.4, 0.08, 5.3),
        new Grin(4, new double[] {-0.6, 0.35, -0.75}, 1.1, 0.13, 0.9),
        new Grin(1, new double[] {0.6, 0.45, -0.65}, 1.0, 0.1, 2.7)};

    /** The great maw low on its front: out of its body from inside, its girth (the worm's mouth that many times its size). */
    static final double[] MAW_ROOT = {0.45, 1.35, 0.4};
    static final double[] MAW_HEAD = {0.75, 1.55, 1.95};
    static final double MAW_GIRTH = 3.2;

    /** A tentacle: where it grows out (inside its body), the way it starts, the axis it curls round and how far, its length and girth. */
    record Tentacle(double[] root, double[] way, double[] curl, double bend, double length, double girth, boolean mouth, double rate) {
    }

    /**
     * Out every way and curling round its body: out to his right and arching down to the ground; over the top from his left and down
     * over the front; out behind, low, rearing up; round its base on his left along the ground (no mouth, tapering); out ahead low along
     * the ground and rising; up from its crest and over backward.
     */
    static final Tentacle[] TENTACLES = {
        new Tentacle(new double[] {2.0, 2.7, 0.3}, new double[] {1.0, 0.45, 0.2}, new double[] {0.0, 0.0, 1.0}, -2.2, 10.0, 2.0, true, 0.05),
        new Tentacle(new double[] {-1.1, 4.0, -0.5}, new double[] {-0.45, 1.0, 0.25}, new double[] {1.0, 0.0, -0.25}, 2.4, 11.0, 2.1, true, 0.043),
        new Tentacle(new double[] {0.4, 2.0, -2.2}, new double[] {0.2, 0.15, -1.0}, new double[] {1.0, 0.0, 0.0}, 1.6, 12.0, 2.2, true, 0.038),
        new Tentacle(new double[] {-2.2, 0.8, 0.4}, new double[] {-0.8, -0.2, 0.55}, new double[] {0.0, 1.0, 0.0}, -2.6, 10.0, 1.8, false, 0.06),
        new Tentacle(new double[] {1.0, 0.7, 1.5}, new double[] {0.35, -0.05, 1.0}, new double[] {1.0, 0.0, -0.35}, -1.4, 10.0, 1.9, true, 0.055),
        new Tentacle(new double[] {0.6, 4.6, 0.0}, new double[] {0.1, 1.0, -0.2}, new double[] {1.0, 0.0, 0.0}, 2.0, 9.0, 1.8, true, 0.065)};

    /** Where its lumps grow out of and shrink back into: his middle. */
    static final double[] BODY = {0.0, 1.2, -0.1};

    /** How many tentacles it has. */
    public static int tentacles() {
        return TENTACLES.length;
    }

    /** True if tentacle {@code i} ends in a mouth. */
    public static boolean mouthed(int i) {
        return TENTACLES[i].mouth();
    }

    /** 3.5.7: how much tighter its tentacles curl for each share it is drawn in; how much longer one thrown out straight is than its own length. */
    static final double SQUEEZE_CURL = 3.0;
    static final double STRAIGHT_LONGER = 1.15;
    /** 3.5.7: how high a tentacle thrown out straight ends (it slopes there from its root: it sweeps at a body's height round him). */
    static final double SWEEP_HEIGHT = 1.1;

    /** 3.5.7: the level way tentacle {@code i} is thrown out straight: out from its middle through its root. */
    public static double[] sweepWay(int i) {
        double[] root = TENTACLES[i].root();
        double[] way = {root[0] - BODY[0] + 0.3 * TENTACLES[i].way()[0], 0.0, root[2] - BODY[2] + 0.3 * TENTACLES[i].way()[2]};
        double flat = Math.hypot(way[0], way[2]);
        return flat < 1.0E-6 ? new double[] {1.0, 0.0, 0.0} : new double[] {way[0] / flat, 0.0, way[2] / flat};
    }

    /** 3.5.7: {@code p} turned round the kakuja's upright middle by {@code turn} (radians, from his right toward ahead). */
    public static double[] turned(double[] p, double turn) {
        double c = Math.cos(turn);
        double sn = Math.sin(turn);
        double x = p[0] - BODY[0];
        double z = p[2] - BODY[2];
        return new double[] {BODY[0] + x * c - z * sn, p[1], BODY[2] + x * sn + z * c};
    }

    /**
     * 3.5.7: tentacle {@code i} coiled round what it holds ({@code at}, its middle, body frame), {@code amount} 0 (as it is) .. 1 (wound
     * round it, Endless Hunger): out from the mass to it and round and round it from its feet to its shoulders, restraining it.
     */
    public static double[][] wrapped(int i, double time, double[] at, double amount) {
        double[][] rest = resample(tentacle(i, time, null, 0.0), WRAP_POINTS);
        if (amount <= 0.0 || at == null) {
            return rest;
        }
        double[][] coiled = new double[WRAP_POINTS][];
        int approach = 7;
        coiled[0] = rest[0];
        coiled[1] = rest[1];
        coiled[2] = rest[2];
        double[] start = GraftMesh.add(at, new double[] {0.0, -WRAP_HIGH, 0.0});
        double[] from = rest[2];
        double[] to = GraftMesh.sub(at, start);
        double startAngle = Math.atan2(from[2] - at[2], from[0] - at[0]);
        double[] coilStart = {at[0] + Math.cos(startAngle) * WRAP_RADIUS, start[1], at[2] + Math.sin(startAngle) * WRAP_RADIUS};
        for (int k = 3; k < approach; k++) {
            double f = (k - 2) / (double)(approach - 2);
            double[] line = GraftMesh.lerp(from, coilStart, f);
            line[1] += 1.2 * Math.sin(Math.PI * f);
            coiled[k] = line;
        }
        int turns = WRAP_POINTS - approach;
        for (int k = 0; k < turns; k++) {
            double f = k / (double)(turns - 1);
            double a = startAngle + f * WRAP_TURNS * Math.PI * 2.0 + 0.05 * Math.sin(time * 0.4 + k);
            double r = WRAP_RADIUS * (1.0 - 0.18 * f) + 0.04 * Math.sin(time * 0.5 + k * 1.7);
            coiled[approach + k] = new double[] {at[0] + Math.cos(a) * r, start[1] + f * 2.0 * WRAP_HIGH, at[2] + Math.sin(a) * r};
        }
        double w = GraftMesh.smooth(GraftMesh.clamp01(amount));
        double[][] out = new double[WRAP_POINTS][];
        for (int k = 0; k < WRAP_POINTS; k++) {
            out[k] = GraftMesh.lerp(rest[k], coiled[k], w);
        }
        return out;
    }

    /** 3.5.7: how many points a coiling tentacle's line has; how far round its catch, how far up and down it winds, how many turns. */
    static final int WRAP_POINTS = 22;
    static final double WRAP_RADIUS = 0.62;
    static final double WRAP_HIGH = 0.8;
    static final double WRAP_TURNS = 2.6;

    /** {@code line} as {@code count} points along the curve through it (for the tests: a coiling tentacle compared frame to frame). */
    public static double[][] resampleForTests(double[][] line, int count) {
        return resample(line, count);
    }

    /** {@code line} as {@code count} points along the curve through it. */
    static double[][] resample(double[][] line, int count) {
        double[][] out = new double[count][];
        for (int k = 0; k < count; k++) {
            out[k] = com.hex.kagune.control.ukaku.CentipedeTail.catmullRom(line, k / (double)(count - 1));
        }
        return out;
    }

    // ---------------------------------------------------------------- its growth

    /**
     * How far each part of it has grown, {@code age} ticks after going into it ({@code kakuja} true) or after coming out of it: how much
     * the worm has swollen and how much of it still shows, the tendrils wrapping him, each lump, the mouths, the tentacles.
     */
    public record Growth(double swell, double worm, double wrap, double[] lumps, double mouths, double tentacles) {
        /** True if anything of the kakuja shows. */
        public boolean shows() {
            for (double lump : this.lumps) {
                if (lump > 0.002) {
                    return true;
                }
            }
            return this.wrap > 0.002 || this.tentacles > 0.002;
        }
    }

    public static Growth growth(boolean kakuja, double age) {
        double[] lumps = new double[LUMPS.length];
        if (kakuja) {
            double swell = GraftMesh.smooth(GraftMesh.clamp01(age / 18.0));
            double worm = 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - 22.0) / 14.0));
            double wrap = GraftMesh.smooth(GraftMesh.clamp01((age - 8.0) / 16.0)) * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - 34.0) / 10.0)));
            for (int i = 0; i < lumps.length; i++) {
                double w = GraftMesh.clamp01((age - 18.0 - 3.0 * i) / 14.0);
                lumps[i] = GraftMesh.smooth(w) * (1.0 + 0.1 * Math.sin(Math.PI * w));
            }
            double mouths = GraftMesh.smooth(GraftMesh.clamp01((age - 32.0) / 16.0));
            double t = GraftMesh.clamp01((age - 42.0) / 16.0);
            double tentacles = t <= 0.0 ? 0.0 : 1.0 + 2.2 * Math.pow(t - 1.0, 3) + 1.2 * Math.pow(t - 1.0, 2); // bursting out, overshooting
            return new Growth(swell, worm, wrap, lumps, mouths, Math.max(0.0, tentacles));
        }
        double swell = 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - 14.0) / 14.0));
        double worm = GraftMesh.smooth(GraftMesh.clamp01((age - 16.0) / 12.0));
        for (int i = 0; i < lumps.length; i++) {
            lumps[i] = 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - 6.0 - i) / 16.0));
        }
        double mouths = 1.0 - GraftMesh.smooth(GraftMesh.clamp01(age / 8.0));
        double tentacles = 1.0 - GraftMesh.smooth(GraftMesh.clamp01(age / 10.0));
        return new Growth(age >= OUT ? 0.0 : swell, age >= OUT ? 1.0 : worm, 0.0, lumps, mouths, tentacles);
    }

    // ---------------------------------------------------------------- its tentacles' motion

    /**
     * Tentacle {@code i}'s line this frame (body frame, root first): out of its body the way it starts, curling round its axis, swaying
     * and writhing; lunging ({@code lunge} its target, {@code reach} 0 .. 1 how far into the lunge) its last part pulled straight at it.
     */
    public static double[][] tentacle(int i, double time, double[] lunge, double reach) {
        return tentacle(i, time, lunge, reach, 1.0, 0.0, 0.0);
    }

    /**
     * 3.5.7: as above, the whole kakuja drawn in ({@code squeeze} under 1: each tentacle shorter and curled tighter round it) or burst out
     * (over 1), the tentacle thrown out straight and level from its root ({@code straight} 0 .. 1: Flesh Cataclysm), all of it turned
     * round its middle by {@code turn} (radians, + from his right toward ahead: the sweep). ({@code thrown}: how far thrown out straight.)
     */
    public static double[][] tentacle(int i, double time, double[] lunge, double reach, double squeeze, double thrown, double turn) {
        Tentacle t = TENTACLES[i];
        int n = 9;
        double[][] points = new double[n][];
        points[0] = GraftMesh.lerp(BODY, t.root(), squeeze);
        double[] d = GraftMesh.normalize(t.way());
        double[] axis = GraftMesh.normalize(t.curl());
        double tight = squeeze < 1.0 ? 1.0 + (1.0 - squeeze) * SQUEEZE_CURL : 1.0;
        double step = t.length() * Math.min(1.0, squeeze) / (n - 1);
        for (int k = 1; k < n; k++) {
            double writhe = 0.22 * Math.sin(time * t.rate() * 6.0 + k * 0.9 + i * 1.7);
            d = GraftMesh.rotate(d, axis, t.bend() * tight / (n - 1) + writhe * 0.35);
            d = GraftMesh.normalize(GraftMesh.rotate(d, new double[] {0.0, 1.0, 0.0}, 0.09 * Math.sin(time * t.rate() * 4.0 + k * 1.3 + i)));
            points[k] = GraftMesh.add(points[k - 1], GraftMesh.mul(d, step));
        }
        if (thrown > 0.0) {
            // Thrown out straight and level, its full length and more, the way its root faces out of the mass.
            double[] out = sweepWay(i);
            double longer = t.length() * STRAIGHT_LONGER / (n - 1);
            for (int k = 1; k < n; k++) {
                double[] line = GraftMesh.add(points[0], GraftMesh.mul(out, longer * k));
                line[1] = points[0][1] + (SWEEP_HEIGHT - points[0][1]) * k / (n - 1.0);
                line[1] += 0.12 * Math.sin(time * 0.9 + k * 1.3 + i);
                points[k] = GraftMesh.lerp(points[k], line, thrown);
            }
        }
        if (turn != 0.0) {
            for (int k = 0; k < n; k++) {
                points[k] = turned(points[k], turn);
            }
        }
        if (lunge != null && reach > 0.0) {
            // Thrown at it: its line from its third point pulled straight at the target, the mouth on it.
            int from = 2;
            for (int k = from + 1; k < n; k++) {
                double f = (k - from) / (double)(n - 1 - from);
                double[] straight = GraftMesh.lerp(points[from], lunge, f);
                points[k] = GraftMesh.lerp(points[k], straight, reach * (0.4 + 0.6 * f));
            }
        }
        return points;
    }

    /** 3.5.4: how far into its lunge a tentacle is, {@code age} ticks into its bite: thrown out to it as it shuts, held, drawn back. */
    public static double lunge(double age) {
        com.hex.kagune.control.bikaku.BikakuRules.Bite bite = com.hex.kagune.control.bikaku.BikakuRules.Bite.TENTACLE;
        int land = bite.land();
        if (age < land) {
            double w = GraftMesh.clamp01(age / land);
            return w * w * (3.0 - 2.0 * w);
        }
        if (age < land + 3) {
            return 1.0;
        }
        return 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - land - 3) / (double)(bite.ticks() - land - 3)));
    }

    /** How wide the lunging tentacle's mouth is: thrown wide on the way, snapped shut as it lands, slack after. */
    public static double lungeMouth(double age) {
        int land = com.hex.kagune.control.bikaku.BikakuRules.Bite.TENTACLE.land();
        if (age < land - 1) {
            return GraftMesh.clamp01(0.3 + age / (land - 1.0));
        }
        if (age < land) {
            return 1.0 - (age - (land - 1));
        }
        return 0.04 + 0.2 * GraftMesh.clamp01((age - land - 3) / 6.0);
    }

    /** The mouthed tentacle that goes after {@code target} (body frame): the one whose mouth is nearest the way to it. */
    public static int biter(double[] target, double time) {
        double[] centre = {0.0, 2.4, 0.0};
        double[] to = GraftMesh.normalize(GraftMesh.sub(target, centre));
        int best = 0;
        double bestDot = -2.0;
        for (int i = 0; i < TENTACLES.length; i++) {
            if (!TENTACLES[i].mouth()) {
                continue;
            }
            double[][] line = tentacle(i, time, null, 0.0);
            double[] head = GraftMesh.normalize(GraftMesh.sub(line[line.length - 1], centre));
            double dot = GraftMesh.dot(head, to);
            if (dot > bestDot) {
                bestDot = dot;
                best = i;
            }
        }
        return best;
    }

    /** How many tendrils wrap round him as it grows. */
    static final int WRAPS = 4;

    /** Wrapping tendril {@code k}: out of his lower back and round and round him, climbing to his shoulders, writhing. */
    static double[][] wrap(int k, double time) {
        int n = 10;
        double[][] points = new double[n][];
        points[0] = MawMotion.ROOT;
        double a0 = Math.PI * 1.5 + k * Math.PI * 0.5;
        int way = k % 2 == 0 ? 1 : -1;
        for (int j = 1; j < n; j++) {
            double a = a0 + way * j * 0.85 + 0.15 * Math.sin(time * 0.3 + j + k);
            double r = 0.5 + 0.06 * Math.sin(time * 0.4 + j * 1.3 + k);
            points[j] = new double[] {Math.cos(a) * r, 0.75 + j * 0.17, Math.sin(a) * r * 0.75};
        }
        return points;
    }

    /** How wide grin {@code i} is open this frame (0 .. 1): mostly a clenched grin, now and then thrown open. */
    static double grinOpen(int i, double time) {
        Grin grin = GRINS[i];
        double wave = Math.sin(time * grin.rate() + grin.phase());
        return 0.06 + 0.94 * Math.pow(Math.max(0.0, wave), 3.0);
    }

    /** How wide the great maw is open this frame. */
    static double mawOpen(double time) {
        double wave = Math.sin(time * 0.07 + 0.8);
        return 0.15 + 0.85 * Math.pow(Math.max(0.0, wave), 2.0);
    }

    // ---------------------------------------------------------------- the mesh

    /**
     * 3.5.7: what the kakuja is doing this frame, over what it always does: drawn in or burst out ({@code squeeze}, 1 as it is), its
     * tentacles thrown out straight ({@code straight}) and turned round it ({@code turn}, radians), its grins thrown at least
     * {@code grins} open (-1: as they are), its maw exactly {@code maw} open (-1: as it is), each tentacle's lunge ({@code lunge[i]} its target or null, {@code reach[i]} how far into
     * it, {@code bite[i]} its mouth, -1 its own), one tentacle coiled round a catch ({@code wrapper}, -1 none, at {@code wrapAt}, by
     * {@code wrap}), and its flesh mending ({@code mend} 0 .. 1).
     */
    public record Act(double squeeze, double straight, double turn, double grins, double maw, double[][] lunge, double[] reach, double[] bite, int wrapper,
                      double[] wrapAt, double wrap, double mend) {
        /** Nothing over what it always does. */
        public static Act still() {
            int n = TENTACLES.length;
            double[] none = new double[n];
            java.util.Arrays.fill(none, -1.0);
            return new Act(1.0, 0.0, 0.0, -1.0, -1.0, new double[n][], new double[n], none, -1, null, 0.0, 0.0);
        }

        /** As this, tentacle {@code i} lunging at {@code target}, {@code reach} into it, its mouth {@code bite}. */
        public Act lunging(int i, double[] target, double reach, double bite) {
            double[][] lunges = this.lunge.clone();
            double[] reaches = this.reach.clone();
            double[] bites = this.bite.clone();
            lunges[i] = target;
            reaches[i] = reach;
            bites[i] = bite;
            return new Act(this.squeeze, this.straight, this.turn, this.grins, this.maw, lunges, reaches, bites, this.wrapper, this.wrapAt, this.wrap, this.mend);
        }
    }

    /**
     * The kakuja into {@code sink} (body frame) as grown ({@code growth}), {@code time} ticks into the world, {@code seed} his, in
     * {@code tint}; {@code biter} the tentacle lunging ({@code -1} none) at {@code lunge} ({@code reach} how far into its lunge, its mouth
     * {@code bite} open).
     */
    public static void build(UkakuModel.Sink sink, Growth growth, double time, int seed, int tint, boolean fine, int biter, double[] lunge, double reach,
                             double bite) {
        Act act = Act.still();
        if (biter >= 0 && lunge != null) {
            act = act.lunging(biter, lunge, reach, bite);
        }
        build(sink, growth, time, seed, tint, fine, act);
    }

    /** 3.5.7: the kakuja into {@code sink} as grown, doing {@code act}. */
    public static void build(UkakuModel.Sink sink, Growth growth, double time, int seed, int tint, boolean fine, Act act) {
        UkakuModel.Sink flesh = (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, n, KaguneTint.recolour(ca, tint), KaguneTint.recolour(cb, tint),
            KaguneTint.recolour(cc, tint), KaguneTint.recolour(cd, tint));
        double squeeze = act.squeeze();
        // Its body: the lumps, each breathing (3.5.7: drawn in toward its middle, or burst out).
        for (int i = 0; i < LUMPS.length; i++) {
            double grown = growth.lumps()[i];
            if (grown <= 0.002) {
                continue;
            }
            Lump lump = squeezed(place(LUMPS[i], grown), squeeze);
            double breathe = 1.0 + 0.055 * Math.sin(time * 0.12 + i * 1.9) + 0.02 * Math.sin(time * 0.31 + i);
            double k = grown * breathe;
            GraftMesh.blob(flesh, lump.centre(), new double[] {1.0, 0.0, 0.0}, new double[] {0.0, 1.0, 0.0}, new double[] {0.0, 0.0, 1.0},
                lump.radii()[0] * k, lump.radii()[1] * k, lump.radii()[2] * k, fine ? 12 : 7, fine ? 18 : 11, 0.22, seed + lump.seed(),
                v -> 0.45 + 0.4 * v, MawWorm.FLESH_LIGHT, MawWorm.FLESH_DEEP, 1.0);
            seams(flesh, lump, k, time, seed, fine);
        }
        // Its mouths, opening across it as it grows.
        if (growth.mouths() > 0.002) {
            for (int i = 0; i < GRINS.length; i++) {
                Grin grin = GRINS[i];
                double lumpGrown = growth.lumps()[grin.lump()];
                if (lumpGrown <= 0.05) {
                    continue;
                }
                double open = act.grins() >= 0.0 ? Math.max(grinOpen(i, time), act.grins()) : grinOpen(i, time);
                grin(sink, flesh, grin, lumpGrown, growth.mouths(), open, fine, squeeze);
            }
            double[] mawRoot = squeezedPoint(MAW_ROOT, squeeze);
            double[] mawHead = squeezedPoint(MAW_HEAD, squeeze);
            double[][] maw = {mawRoot, GraftMesh.lerp(mawRoot, mawHead, 0.55), mawHead};
            MawWorm.build(sink, new MawWorm.Pose(maw, new double[] {0.0, 1.0, 0.0}, growth.mouths(), act.maw() >= 0.0 ? act.maw() : mawOpen(time),
                MAW_GIRTH * growth.mouths(), seed + 5, time), tint, fine);
        }
        // Growing into it: thinner tendrils out of his back wrapping round him, before the mass swallows them.
        if (growth.wrap() > 0.002) {
            for (int k = 0; k < WRAPS; k++) {
                double[][] line = MawMotion.grown(wrap(k, time), growth.wrap());
                MawWorm.build(sink, new MawWorm.Pose(line, new double[] {0.0, 1.0, 0.0}, growth.wrap(), 0.0, 0.5, seed + 211 * (k + 1), time), tint, fine,
                    false);
            }
        }
        // Its tentacles, bursting out.
        java.util.List<double[][]> mending = new java.util.ArrayList<>();
        java.util.List<Double> mendRadii = new java.util.ArrayList<>();
        if (growth.tentacles() > 0.002) {
            for (int i = 0; i < TENTACLES.length; i++) {
                Tentacle t = TENTACLES[i];
                boolean lunging = act.lunge()[i] != null && act.reach()[i] > 0.0;
                double[][] line = i == act.wrapper() && act.wrap() > 0.0 ? wrapped(i, time, act.wrapAt(), act.wrap())
                    : tentacle(i, time, lunging ? act.lunge()[i] : null, lunging ? act.reach()[i] : 0.0, squeeze, act.straight(), act.turn());
                double shown = Math.min(1.0, growth.tentacles());
                line = MawMotion.grown(line, shown);
                double own = 0.12 + 0.5 * Math.pow(Math.max(0.0, Math.sin(time * 0.06 + i * 2.3)), 4.0);
                double open = act.bite()[i] >= 0.0 ? act.bite()[i] : act.grins() >= 0.0 ? Math.max(own, act.grins()) : own;
                MawWorm.build(sink, new MawWorm.Pose(line, new double[] {0.0, 1.0, 0.0}, shown, open, t.girth() * Math.max(0.3, growth.tentacles()),
                    seed + 61 * (i + 1), time), tint, fine, t.mouth());
                if (act.mend() > 0.002) {
                    tentacleMending(mending, mendRadii, line, t.girth(), time, i, act.mend());
                }
            }
        }
        // 3.5.7, Undying Abomination: strands of new flesh crawling over its lumps and down its tentacles, knitting them.
        if (act.mend() > 0.002) {
            for (int i = 0; i < LUMPS.length; i++) {
                if (growth.lumps()[i] > 0.05) {
                    lumpMending(mending, mendRadii, squeezed(place(LUMPS[i], growth.lumps()[i]), squeeze), time, i, act.mend());
                }
            }
            mendStrands(sink, mending, mendRadii, tint, time);
        }
    }

    /**
     * 3.5.7: Undying Abomination's strands into {@code sink}: a tube of new, lighter flesh, swelling toward its growing end, a thick thread
     * of RC glowing along it, its growing end glowing hottest (made to show on the mass, where {@link MawTissue}'s darker strands did not).
     */
    private static void mendStrands(UkakuModel.Sink sink, java.util.List<double[][]> placed, java.util.List<Double> radii, int tint, double time) {
        UkakuModel.Sink flesh = (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, n, KaguneTint.recolour(ca, tint), KaguneTint.recolour(cb, tint),
            KaguneTint.recolour(cc, tint), KaguneTint.recolour(cd, tint));
        for (int k = 0; k < placed.size(); k++) {
            double[][] points = placed.get(k);
            double radius = radii.get(k);
            if (points.length < 2 || radius <= 1.0E-4) {
                continue;
            }
            com.hex.kagune.control.ukaku.KakujaModel.Spine spine = MawWorm.spine(points,
                GraftMesh.perpendicular(GraftMesh.normalize(GraftMesh.sub(points[points.length - 1], points[0]))), 16);
            if (spine.length() < 0.02) {
                continue;
            }
            GraftMesh.tube(flesh, spine, 0.0, 1.0, 16, 6, new GraftMesh.Surface() {
                @Override
                public double radius(double s, double a) {
                    return radius * (0.45 + 0.55 * Math.sin(Math.PI * Math.min(1.0, s * 1.1)));
                }

                @Override
                public int colour(double s, double a) {
                    return GraftMesh.mix(MawWorm.LIP, MawWorm.FLESH_LIGHT, 0.4 + 0.6 * s);
                }
            }, 1.0);
            double[][] prev = null;
            int prevColour = 0;
            for (int i = 0; i <= 12; i++) {
                double s = i / 12.0;
                double[][] f = spine.at(s);
                double w = radius * 0.75;
                double[] centre = GraftMesh.add(f[0], GraftMesh.mul(f[2], radius * 0.85));
                double[][] edge = {GraftMesh.sub(centre, GraftMesh.mul(f[3], w)), GraftMesh.add(centre, GraftMesh.mul(f[3], w))};
                double pulse = 0.6 + 0.4 * Math.sin(time * 0.5 - s * 6.0 + k);
                int colour = KaguneTint.recolour(GraftMesh.argb(GraftMesh.mix(MawWorm.VEIN, MawWorm.VEIN_HOT, Math.min(1.0, s * s * pulse + 0.25)), 1.0), tint);
                if (prev != null) {
                    GraftMesh.glow(sink, prev[0], edge[0], edge[1], prev[1], prevColour, colour, colour, prevColour);
                }
                prev = edge;
                prevColour = colour;
            }
        }
    }

    /** 3.5.7: lump {@code lump} drawn in toward its middle ({@code squeeze} under 1) or burst out (over 1). */
    static Lump squeezed(Lump lump, double squeeze) {
        if (squeeze == 1.0) {
            return lump;
        }
        double size = 1.0 + (squeeze - 1.0) * 0.45;
        return new Lump(squeezedPoint(lump.centre(), squeeze), new double[] {lump.radii()[0] * size, lump.radii()[1] * size, lump.radii()[2] * size},
            lump.seed());
    }

    /** 3.5.7: a point of its body drawn in toward its middle by {@code squeeze}. */
    static double[] squeezedPoint(double[] p, double squeeze) {
        return squeeze == 1.0 ? p : GraftMesh.lerp(BODY, p, squeeze);
    }

    /** 3.5.7: how many mending strands crawl over each lump; how thick they are. */
    static final int MEND_STRANDS = 6;
    static final double MEND_RADIUS = 0.14;

    /** 3.5.7: strands of new flesh crawling over a lump: each a spiral laid on its skin, growing along over its time and starting again. */
    private static void lumpMending(java.util.List<double[][]> out, java.util.List<Double> radii, Lump lump, double time, int index, double mend) {
        for (int k = 0; k < MEND_STRANDS; k++) {
            double cycle = (time * 0.045 + GraftMesh.hash(index * 17 + k * 5) ) % 1.0;
            double grown = GraftMesh.smooth(GraftMesh.clamp01(cycle * 1.6)) * mend;
            if (grown <= 0.02) {
                continue;
            }
            double a0 = Math.PI * 2.0 * GraftMesh.hash(index * 31 + k * 7);
            double tilt = 0.5 + 0.8 * GraftMesh.hash(index * 13 + k);
            int n = 14;
            double[][] line = new double[n][];
            for (int j = 0; j < n; j++) {
                double u = j / (n - 1.0) * grown;
                double a = a0 + u * Math.PI * 2.2;
                double y = -0.75 + 1.5 * u * tilt;
                double ring = Math.sqrt(Math.max(0.05, 1.0 - y * y));
                line[j] = new double[] {lump.centre()[0] + Math.cos(a) * ring * lump.radii()[0] * 1.04, lump.centre()[1] + y * lump.radii()[1] * 1.04,
                    lump.centre()[2] + Math.sin(a) * ring * lump.radii()[2] * 1.04};
            }
            out.add(line);
            radii.add(MEND_RADIUS * (0.6 + 0.4 * mend));
        }
    }

    /** 3.5.7: a strand of new flesh winding down a tentacle from its root, crawling out along it. */
    private static void tentacleMending(java.util.List<double[][]> out, java.util.List<Double> radii, double[][] line, double girth, double time, int i,
                                        double mend) {
        if (line.length < 3) {
            return;
        }
        double cycle = (time * 0.05 + 0.37 * i) % 1.0;
        double grown = GraftMesh.smooth(GraftMesh.clamp01(cycle * 1.5)) * mend;
        if (grown <= 0.02) {
            return;
        }
        KakujaModelSpine spine = new KakujaModelSpine(MawWorm.spine(line, new double[] {0.0, 1.0, 0.0}, 24));
        int n = 16;
        double[][] strand = new double[n][];
        double r = MawWorm.BODY_RADIUS * girth * 1.05;
        for (int j = 0; j < n; j++) {
            double s = 0.05 + 0.75 * grown * j / (n - 1.0);
            strand[j] = GraftMesh.onSurface(spine.spine, s, j * 0.9 + i, r)[0];
        }
        out.add(strand);
        radii.add(MEND_RADIUS * girth * 0.55);
    }

    /** A spine held for {@link #tentacleMending}. */
    private record KakujaModelSpine(com.hex.kagune.control.ukaku.KakujaModel.Spine spine) {
    }

    /** Lump {@code lump} as far as it has grown: swelling out of his middle to its own place (and back into it). */
    static Lump place(Lump lump, double grown) {
        return new Lump(GraftMesh.lerp(BODY, lump.centre(), Math.min(1.0, grown)), lump.radii(), lump.seed());
    }

    /** Glowing seams of RC wandering over a lump. */
    private static void seams(UkakuModel.Sink sink, Lump lump, double k, double time, int seed, boolean fine) {
        int lines = fine ? 3 : 2;
        int steps = fine ? 22 : 12;
        for (int v = 0; v < lines; v++) {
            double tilt = Math.PI * GraftMesh.hash(seed * 7 + lump.seed() * 3 + v);
            double spin = Math.PI * 2.0 * GraftMesh.hash(seed * 13 + lump.seed() + v * 5);
            double[] prev0 = null;
            double[] prev1 = null;
            int prevColour = 0;
            for (int i = 0; i <= steps; i++) {
                double a = Math.PI * 2.0 * i / steps * 0.75;
                double wobble = 0.25 * (GraftMesh.noise(seed + lump.seed() + v, i * 0.6, 0.0) - 0.5);
                double[] dir = {Math.cos(a), Math.sin(a) * Math.cos(tilt) + wobble, Math.sin(a) * Math.sin(tilt)};
                dir = GraftMesh.normalize(GraftMesh.rotate(dir, new double[] {0.0, 1.0, 0.0}, spin));
                double[] out = {dir[0] * lump.radii()[0] * k * 1.015, dir[1] * lump.radii()[1] * k * 1.015, dir[2] * lump.radii()[2] * k * 1.015};
                double[] p = GraftMesh.add(lump.centre(), out);
                double[] along = GraftMesh.normalize(GraftMesh.cross(dir, new double[] {0.0, 1.0, 0.0}));
                if (!Double.isFinite(along[0])) {
                    along = new double[] {1.0, 0.0, 0.0};
                }
                double width = 0.035 * (0.6 + 0.8 * Math.sin(Math.PI * i / steps));
                double[] across = GraftMesh.normalize(GraftMesh.cross(dir, GraftMesh.sub(p, prev0 == null ? p : GraftMesh.mul(GraftMesh.add(prev0, prev1), 0.5))));
                if (!Double.isFinite(across[0]) || GraftMesh.dot(across, across) < 0.5) {
                    across = along;
                }
                double[] e0 = GraftMesh.sub(p, GraftMesh.mul(across, width));
                double[] e1 = GraftMesh.add(p, GraftMesh.mul(across, width));
                double pulse = 0.55 + 0.45 * Math.pow(0.5 + 0.5 * Math.sin(i * 0.5 - time * 0.3 + v), 3.0);
                int colour = GraftMesh.argb(GraftMesh.mix(MawWorm.VEIN_DIM, MawWorm.VEIN, pulse), 1.0);
                if (prev0 != null) {
                    GraftMesh.glow(sink, prev0, e0, e1, prev1, prevColour, colour, colour, prevColour);
                }
                prev0 = e0;
                prev1 = e1;
                prevColour = colour;
            }
        }
    }

    /**
     * A grinning mouth on a lump: thick lips round a wide opening ({@code open} 0 a clenched grin .. 1 thrown open), a row of big flat teeth
     * along each lip, a dark mouth behind them.
     */
    private static void grin(UkakuModel.Sink sink, UkakuModel.Sink flesh, Grin grin, double lumpGrown, double grown, double open, boolean fine,
                             double squeeze) {
        Lump lump = squeezed(place(LUMPS[grin.lump()], lumpGrown), squeeze);
        double[] w = GraftMesh.normalize(grin.way());
        double[] r = {lump.radii()[0] * lumpGrown, lump.radii()[1] * lumpGrown, lump.radii()[2] * lumpGrown};
        double[] at = GraftMesh.add(lump.centre(), new double[] {w[0] * r[0], w[1] * r[1], w[2] * r[2]});
        double[] n = GraftMesh.normalize(new double[] {w[0] / r[0], w[1] / r[1], w[2] / r[2]});
        double[] u = GraftMesh.cross(new double[] {0.0, 1.0, 0.0}, n);
        u = GraftMesh.dot(u, u) < 1.0E-4 ? new double[] {1.0, 0.0, 0.0} : GraftMesh.normalize(u);
        double[] v = GraftMesh.cross(n, u);
        double width = grin.width() * grown;
        double a = width * 0.5;
        double b = width * (0.06 + 0.22 * open);
        int sides = fine ? 18 : 10;
        // Its lips: from the opening out over a raised crest down into the flesh round it.
        double[][] scales = {{1.0, 0.05}, {1.18, 0.11}, {1.45, 0.0}};
        double[][][] rings = new double[scales.length][sides][];
        for (int s = 0; s < scales.length; s++) {
            for (int j = 0; j < sides; j++) {
                double ang = Math.PI * 2.0 * j / sides;
                double x = Math.cos(ang) * a * scales[s][0];
                double y = Math.sin(ang) * (b * scales[s][0] + (s == 0 ? 0.0 : 0.06 * width * s));
                rings[s][j] = GraftMesh.add(GraftMesh.add(at, GraftMesh.add(GraftMesh.mul(u, x), GraftMesh.mul(v, y))), GraftMesh.mul(n, scales[s][1] * width));
            }
        }
        int[] tones = {GraftMesh.argb(MawWorm.LIP_INNER, 1.0), GraftMesh.argb(MawWorm.LIP, 1.0), GraftMesh.argb(MawWorm.FLESH, 1.0)};
        double[] behind = GraftMesh.sub(at, GraftMesh.mul(n, width));
        for (int s = 0; s + 1 < scales.length; s++) {
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                GraftMesh.facet(flesh, rings[s][j], rings[s + 1][j], rings[s + 1][k], rings[s][k], behind, tones[s], tones[s + 1], tones[s + 1], tones[s]);
            }
        }
        // The dark of the mouth behind the teeth.
        double[] deep = GraftMesh.sub(at, GraftMesh.mul(n, 0.35 * width));
        int dark = GraftMesh.argb(MawWorm.THROAT_DEEP, 1.0);
        int gum = GraftMesh.argb(MawWorm.THROAT, 1.0);
        for (int j = 0; j < sides; j++) {
            int k = (j + 1) % sides;
            double[] i0 = GraftMesh.lerp(rings[0][j], deep, 0.7);
            double[] i1 = GraftMesh.lerp(rings[0][k], deep, 0.7);
            GraftMesh.facet(sink, rings[0][k], i1, i0, rings[0][j], GraftMesh.add(at, GraftMesh.mul(n, width)), gum, dark, dark, gum);
        }
        // Its teeth: big, flat, square, a row hanging from the upper lip and one standing on the lower, meeting when it is shut.
        int count = (int)Math.round(4 + 3 * grin.width());
        double toothLength = Math.min(b * 0.98, 0.07 + 0.08 * grin.width()) * grown;
        double toothWide = 1.7 * a / count * 0.86;
        int root = GraftMesh.argb(MawWorm.TOOTH_ROOT, 1.0);
        int tip = GraftMesh.argb(MawWorm.TOOTH_TIP, 1.0);
        for (int row = -1; row <= 1; row += 2) {
            for (int i = 0; i < count; i++) {
                double x = -0.85 * a + (i + 0.5) * (1.7 * a / count);
                double edge = b * Math.sqrt(Math.max(0.0, 1.0 - (x * x) / (a * a)));
                double[] base = GraftMesh.add(GraftMesh.add(at, GraftMesh.add(GraftMesh.mul(u, x), GraftMesh.mul(v, row * edge))), GraftMesh.mul(n, 0.01 * width));
                double[] end = GraftMesh.sub(base, GraftMesh.mul(v, row * toothLength));
                double[] half = GraftMesh.mul(u, toothWide * 0.5);
                double[] thick = GraftMesh.mul(n, -0.035 * width);
                double[] b0 = GraftMesh.sub(base, half);
                double[] b1 = GraftMesh.add(base, half);
                double[] e0 = GraftMesh.sub(end, half);
                double[] e1 = GraftMesh.add(end, half);
                double[] out = GraftMesh.add(at, GraftMesh.mul(n, -width));
                GraftMesh.facet(sink, b0, b1, e1, e0, out, root, root, tip, tip);
                GraftMesh.facet(sink, e0, e1, GraftMesh.add(e1, thick), GraftMesh.add(e0, thick), GraftMesh.add(base, GraftMesh.mul(v, row * width)), tip, tip,
                    tip, tip);
            }
        }
    }
}
