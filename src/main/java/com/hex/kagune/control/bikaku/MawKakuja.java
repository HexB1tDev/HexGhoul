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
        Tentacle t = TENTACLES[i];
        int n = 9;
        double[][] points = new double[n][];
        points[0] = t.root();
        double[] d = GraftMesh.normalize(t.way());
        double[] axis = GraftMesh.normalize(t.curl());
        double step = t.length() / (n - 1);
        for (int k = 1; k < n; k++) {
            double writhe = 0.22 * Math.sin(time * t.rate() * 6.0 + k * 0.9 + i * 1.7);
            d = GraftMesh.rotate(d, axis, t.bend() / (n - 1) + writhe * 0.35);
            d = GraftMesh.normalize(GraftMesh.rotate(d, new double[] {0.0, 1.0, 0.0}, 0.09 * Math.sin(time * t.rate() * 4.0 + k * 1.3 + i)));
            points[k] = GraftMesh.add(points[k - 1], GraftMesh.mul(d, step));
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
     * The kakuja into {@code sink} (body frame) as grown ({@code growth}), {@code time} ticks into the world, {@code seed} his, in
     * {@code tint}; {@code biter} the tentacle lunging ({@code -1} none) at {@code lunge} ({@code reach} how far into its lunge, its mouth
     * {@code bite} open).
     */
    public static void build(UkakuModel.Sink sink, Growth growth, double time, int seed, int tint, boolean fine, int biter, double[] lunge, double reach,
                             double bite) {
        UkakuModel.Sink flesh = (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, n, KaguneTint.recolour(ca, tint), KaguneTint.recolour(cb, tint),
            KaguneTint.recolour(cc, tint), KaguneTint.recolour(cd, tint));
        // Its body: the lumps, each breathing.
        for (int i = 0; i < LUMPS.length; i++) {
            double grown = growth.lumps()[i];
            if (grown <= 0.002) {
                continue;
            }
            Lump lump = place(LUMPS[i], grown);
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
                grin(sink, flesh, grin, lumpGrown, growth.mouths(), grinOpen(i, time), fine);
            }
            double[][] maw = {MAW_ROOT, GraftMesh.lerp(MAW_ROOT, MAW_HEAD, 0.55), MAW_HEAD};
            MawWorm.build(sink, new MawWorm.Pose(maw, new double[] {0.0, 1.0, 0.0}, growth.mouths(), mawOpen(time), MAW_GIRTH * growth.mouths(), seed + 5,
                time), tint, fine);
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
        if (growth.tentacles() > 0.002) {
            for (int i = 0; i < TENTACLES.length; i++) {
                Tentacle t = TENTACLES[i];
                boolean lunging = i == biter && lunge != null;
                double[][] line = tentacle(i, time, lunging ? lunge : null, lunging ? reach : 0.0);
                double shown = Math.min(1.0, growth.tentacles());
                line = MawMotion.grown(line, shown);
                double open = lunging ? bite : 0.12 + 0.5 * Math.pow(Math.max(0.0, Math.sin(time * 0.06 + i * 2.3)), 4.0);
                MawWorm.build(sink, new MawWorm.Pose(line, new double[] {0.0, 1.0, 0.0}, shown, open, t.girth() * Math.max(0.3, growth.tentacles()),
                    seed + 61 * (i + 1), time), tint, fine, t.mouth());
            }
        }
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
    private static void grin(UkakuModel.Sink sink, UkakuModel.Sink flesh, Grin grin, double lumpGrown, double grown, double open, boolean fine) {
        Lump lump = place(LUMPS[grin.lump()], lumpGrown);
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
