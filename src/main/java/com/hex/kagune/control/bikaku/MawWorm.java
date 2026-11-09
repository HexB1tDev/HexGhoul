package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.KaguneTint;
import com.hex.kagune.control.graft.GraftMesh;
import com.hex.kagune.control.ukaku.CentipedeTail;
import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * 3.4.9, the Bikaku path's kagune (the owner's: "a massive, thick, fleshy, worm-like appendage emerging from the player's lower back
 * ... an enormous predatory parasite or lamprey ... At the end of the appendage is a circular, expandable jaw lined with multiple rows of
 * sharp, curved teeth. The mouth should physically open, close, stretch, and bite"): one worm as a mesh (pure, tested, previewed).
 *
 * <p>Its body is a thick muscular tube along its spine (by length: swelling out of the back, a neck, a swollen head), ringed with
 * irregular fleshy ridges and folded along its length, glowing veins wandering down it and grey bone spurs raked back along its top. Its
 * head ends in the mouth: a rolled lip round a round hole that opens and shuts, a crown of great curved fangs on the lip (shut, they close
 * over the hole like a bud; open, they splay out and the whole head flares), three rings of hooked teeth down a throat that goes dark.
 *
 * <p>Built in a red palette, as every kagune here is ({@link KaguneTint}): its flesh, lip and veins are drawn in the owner's own kagune
 * colour; its teeth, spurs and throat keep their own (bone and blood), so the mouth reads on any colour.
 */
public final class MawWorm {
    private MawWorm() {
    }

    /**
     * One worm this frame: its spine's control points (blocks, root first, the mouth last), the way its back faces at the root, how far it
     * has grown out (0 .. 1), how wide its mouth is (0 shut .. 1 thrown wide), how thick it is (1 the kagune's own), its seed (each worm's
     * ridges, spurs and teeth its own) and the time (ticks) its flesh moves by.
     */
    public record Pose(double[][] path, double[] dorsal, double grown, double aperture, double girth, int seed, double time, Feed feed, boolean leg) {
        public Pose(double[][] path, double[] dorsal, double grown, double aperture, double girth, int seed, double time) {
            this(path, dorsal, grown, aperture, girth, seed, time, null, false);
        }

        public Pose(double[][] path, double[] dorsal, double grown, double aperture, double girth, int seed, double time, Feed feed) {
            this(path, dorsal, grown, aperture, girth, seed, time, feed, false);
        }

        /** 4.0.0: one of The Walker's legs ({@link #legRadius}: thick at the hip, tapering to its foot). */
        public Pose leg(boolean leg) {
            return new Pose(this.path, this.dorsal, this.grown, this.aperture, this.girth, this.seed, this.time, this.feed, leg);
        }
    }

    /**
     * 3.6.3, Grand Devouring (the owner: "dramatically expanding in thickness, length, and overall mass. Its mouth becomes enormous,
     * stretching its jaws unnaturally wide and exposing multiple rows of sharp teeth"): a worm swollen to feed. Its body thickened to
     * {@code body} (radius, blocks) {@code swell} of the way; its head grown {@code head} of the way into a great bell of a mouth round a
     * cavity {@code hole} wide (radius) and {@code depth} deep, rows of teeth down its walls; a bulge {@code bulge} thick (radius) and
     * {@code bulgeLength} long at {@code bulgeAt} (0 the root .. 1 the mouth, by length), the flesh stretched over it, {@code bulgeShown} of
     * the way formed (0 none, 1 all of it; it forms as it is swallowed and goes as it sinks into him). All in blocks.
     */
    public record Feed(double hole, double depth, double body, double swell, double head, double bulgeAt, double bulge, double bulgeLength,
                       double bulgeShown) {
        /** The wall of the bell round its cavity. */
        public double wall() {
            return 0.16 + 0.14 * this.hole;
        }

        /** The bell's outer radius. */
        public double headRadius() {
            return this.hole + this.wall();
        }
    }

    // ---------------------------------------------------------------- its palette (the red one; the flesh is recoloured)

    static final int FLESH = 0xFF62112C;
    static final int FLESH_DEEP = 0xFF22040F;
    static final int FLESH_LIGHT = 0xFF8C2140;
    static final int BELLY = 0xFF7A1D36;
    static final int LIP = 0xFF9A2E46;
    static final int LIP_INNER = 0xFF6E1222;
    static final int VEIN = 0xFFFF2630;
    static final int VEIN_HOT = 0xFFFF8A70;
    static final int VEIN_DIM = 0xFF9A0E1A;
    /** Kept as they are on every kagune: the gums, the throat, bone. */
    static final int GUM = 0xFF5C0B18;
    static final int THROAT = 0xFF2A0408;
    static final int THROAT_DEEP = 0xFF070102;
    static final int FANG_ROOT = 0xFF5E5A58;
    static final int FANG_TIP = 0xFFD9D3C9;
    static final int TOOTH_ROOT = 0xFFB2A691;
    static final int TOOTH_TIP = 0xFFF4EEE2;
    static final int SPUR_ROOT = 0xFF4E4A48;
    static final int SPUR_TIP = 0xFFB9B4AC;

    // ---------------------------------------------------------------- its shape (blocks, at girth 1)

    /** Its radius where it leaves the back, along its body, at its neck, round its head, at its lip. */
    public static final double ROOT_RADIUS = 0.1;
    public static final double BODY_RADIUS = 0.245;
    public static final double NECK_RADIUS = 0.205;
    public static final double HEAD_RADIUS = 0.31;
    public static final double LIP_RADIUS = 0.27;
    /** How many great fangs crown the lip, and how long they are (shut: they meet over the hole). */
    public static final int FANGS = 6;
    public static final double FANG_LENGTH = 0.42;
    /** The hooked teeth down its throat: how many in each ring. */
    static final int[] TEETH = {14, 12, 9};
    /** 3.6.3: rows of teeth down a feeding worm's cavity. */
    static final int FEED_TEETH_ROWS = 6;
    /** How far apart its ridges are along it (blocks). */
    static final double RIDGE = 0.16;

    /** The whole worm into {@code sink} ({@code tint}: the owner's kagune colour, 0xRRGGBB); {@code fine} close up (more of everything). */
    public static void build(UkakuModel.Sink sink, Pose pose, int tint, boolean fine) {
        build(sink, pose, tint, fine, true);
    }

    /** 3.5.4: as above; {@code mouth} false: no mouth, its end tapering to a blunt tip (one of the kakuja's tentacles). */
    public static void build(UkakuModel.Sink sink, Pose pose, int tint, boolean fine, boolean mouth) {
        if (pose.grown() <= 0.002 || pose.path().length < 2) {
            return;
        }
        // A ring every tenth of a block or so close up (its ridges are a sixth apart), half as many far off.
        double rough = 0.0;
        for (int i = 1; i < pose.path().length; i++) {
            rough += GraftMesh.length3(GraftMesh.sub(pose.path()[i], pose.path()[i - 1]));
        }
        KakujaModel.Spine spine = spine(pose.path(), pose.dorsal(), fine ? Math.max(48, Math.min(150, (int)Math.ceil(rough * 11.0)))
            : Math.max(24, Math.min(70, (int)Math.ceil(rough * 5.0))));
        if (spine.length() < 1.0E-3) {
            return;
        }
        UkakuModel.Sink flesh = (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, n, KaguneTint.recolour(ca, tint), KaguneTint.recolour(cb, tint),
            KaguneTint.recolour(cc, tint), KaguneTint.recolour(cd, tint));
        double g = pose.girth() * (0.35 + 0.65 * GraftMesh.smooth(pose.grown()));
        double open = GraftMesh.clamp01(pose.aperture());
        body(flesh, spine, pose, g, open, fine, mouth);
        veins(flesh, spine, pose, g, open, fine, mouth);
        spurs(sink, spine, pose, g, fine, mouth);
        if (mouth && pose.grown() > 0.35) {
            mouth(sink, flesh, spine, pose, g, open, fine);
        }
    }

    /**
     * A spine through {@code controls} with its rings spaced evenly by length ({@code samples} segments), its back starting toward
     * {@code dorsal} and carried along without twisting.
     */
    public static KakujaModel.Spine spine(double[][] controls, double[] dorsal, int samples) {
        int dense = samples * 4;
        double[][] ps = new double[dense + 1][];
        double[] at = new double[dense + 1];
        for (int i = 0; i <= dense; i++) {
            ps[i] = CentipedeTail.catmullRom(controls, i / (double)dense);
            if (i > 0) {
                at[i] = at[i - 1] + GraftMesh.length3(GraftMesh.sub(ps[i], ps[i - 1]));
            }
        }
        double total = at[dense];
        double[][] even = new double[samples + 1][];
        int j = 0;
        for (int i = 0; i <= samples; i++) {
            double want = total * i / samples;
            while (j < dense - 1 && at[j + 1] < want) {
                j++;
            }
            double span = Math.max(1.0E-9, at[j + 1] - at[j]);
            even[i] = GraftMesh.lerp(ps[j], ps[j + 1], GraftMesh.clamp01((want - at[j]) / span));
        }
        return CentipedeTail.through(even, samples, 1, dorsal);
    }

    // ---------------------------------------------------------------- the body

    /** Its radius {@code u} of the way along it (by length; {@code length} blocks long) before its ridges, at girth 1. */
    public static double radius(double u, double length, double open) {
        double at = GraftMesh.clamp01(u) * length;
        double fromMouth = Math.max(0.0, length - at);
        double base = ROOT_RADIUS + (BODY_RADIUS - ROOT_RADIUS) * GraftMesh.smooth(at / 0.6);
        double neck = (NECK_RADIUS - BODY_RADIUS) * bump(fromMouth - 0.62, 0.22);
        double head = (HEAD_RADIUS - BODY_RADIUS) * bump(fromMouth - 0.24, 0.2) * (1.0 + 0.25 * open);
        double r = base + neck + head;
        // Its last stretch rounds into the lip (a mouth thrown wide flares it).
        double lip = lipRadius(open);
        double into = GraftMesh.smooth(GraftMesh.clamp01((0.09 - fromMouth) / 0.09));
        return r + (lip - r) * into;
    }

    /**
     * 3.6.3: a feeding worm's radius {@code u} of the way along it ({@link Feed}): out of his back as thin as a plain worm, growing steadily
     * the whole way along it into the bell round the cavity ({@link #horn}), rounding into the lip at the mouth; the bulge where it is, the
     * flesh stretched over it and squeezed in just behind it (pushing it on).
     */
    public static double feedRadius(double u, double length, double open, Feed feed) {
        double normal = radius(u, length, open);
        double at = GraftMesh.clamp01(u) * length;
        double r = normal + (horn(at, length, open, feed) - normal) * GraftMesh.clamp01(feed.swell());
        double shown = GraftMesh.clamp01(feed.bulgeShown());
        if (shown > 0.0 && feed.bulge() > 0.0 && feed.bulgeLength() > 0.0) {
            // The body swallowed, squeezed thinner and longer as the worm narrows toward his back (as a snake's prey), always wider than
            // the worm round it; over its last stretch into his back it is taken in, down to just wider than the worm there.
            double centre = GraftMesh.clamp01(feed.bulgeAt()) * length;
            double round = Math.max(BODY_RADIUS, horn(centre, length, open, feed));
            double fit = Math.pow(GraftMesh.clamp01(round / Math.max(BODY_RADIUS, feed.headRadius())), 0.3);
            double b = Math.min(Math.max(1.45 * round, feed.bulge() * fit), 3.0 * round + 0.2);
            b = (1.3 * round + (b - 1.3 * round) * GraftMesh.smooth(GraftMesh.clamp01(centre / (BULGE_TAKEN_IN + feed.bulge())))) * shown;
            // The body's length, its ends falling off sharply; nowhere more than half again as wide as the worm round it, and none of it
            // in its first half block out of his back (it goes into him there).
            double x = (at - centre) / (0.5 * feed.bulgeLength());
            double room = GraftMesh.smooth(GraftMesh.clamp01((at - 0.45) / 1.2));
            double here = Math.min(b * Math.exp(-x * x * x * x), r * (1.0 + 0.6 * room) + 0.08 * room);
            double squeeze = 1.0 - 0.14 * Math.exp(-(x + 1.7) * (x + 1.7)) * shown;
            r = Math.max(r * squeeze, here) + 0.07 * here;
        }
        return r;
    }

    /** 3.6.3: how far out of his back (blocks) a feeding worm is still a plain worm's size; how much of its head the bell rises over. */
    static final double FEED_PLAIN = 0.6;
    static final double BELL_RISE = 0.35;
    /** 3.6.3: over its last this many blocks into his back (and its own radius more) the bulge is taken in (the owner on its root: "its not meant to be that big"). */
    static final double BULGE_TAKEN_IN = 3.0;

    /** 3.6.3: a feeding worm's bell (outer radius) as far as its head has grown. */
    static double bell(Feed feed) {
        return HEAD_RADIUS + (feed.headRadius() - HEAD_RADIUS) * GraftMesh.clamp01(feed.head());
    }

    /**
     * 3.6.3 (the owner: "why is it not arching and progressively increasing size, it's doing all at once", and on its root: "its not meant
     * to be that big at all, never even close"): a feeding worm's shape {@code at} blocks along it ({@code length} long), swollen: out of
     * his back a plain worm's thickness, then growing steadily all the way along it (slowly at first) to its bell's width a little way
     * into its head, the bell round the cavity to the mouth, rounding into its lip.
     */
    static double horn(double at, double length, double open, Feed feed) {
        double head = GraftMesh.clamp01(feed.head());
        double bell = bell(feed);
        double headLength = 0.62 + (feed.depth() + 0.15 * feed.headRadius() - 0.62) * head;
        double full = Math.max(FEED_PLAIN + 0.5, length - (1.0 - BELL_RISE) * headLength);
        double base = ROOT_RADIUS + (BODY_RADIUS - ROOT_RADIUS) * GraftMesh.smooth(GraftMesh.clamp01(at / 0.6));
        double r = base + (bell - BODY_RADIUS) * GraftMesh.smooth(GraftMesh.clamp01((at - FEED_PLAIN) / (full - FEED_PLAIN)));
        double from = Math.max(0.0, length - at);
        double lip = mouthScale(feed, 1.0) * lipRadius(open);
        double rim = 0.09 + (Math.max(0.12, feed.wall() * 0.8) - 0.09) * head;
        return r + (lip - r) * GraftMesh.smooth(GraftMesh.clamp01((rim - from) / rim));
    }

    /**
     * 3.6.3: the throat's radius {@code w} of the way in: a plain worm's narrows to its end; a feeding worm's ({@code head} of the way)
     * is a cavity {@code cavity} wide, its walls almost straight and rippling as they work, out of the jaws' hole at its mouth.
     */
    static double throatRadius(double hole, double cavity, double head, double w, double time, int seed) {
        double plain = hole * (1.0 - 0.82 * w) * (1.0 + 0.08 * Math.sin(w * Math.PI * 3.0));
        if (head <= 0.0) {
            return plain;
        }
        double work = 0.045 * Math.sin(w * Math.PI * 5.0 - time * 0.21 + seed) + 0.03 * Math.sin(time * 0.13 + w * 9.0);
        double walls = cavity * (1.0 - 0.14 * w * w) * (1.0 + work);
        // Out of the jaws' hole (closing or wide) into the cavity just behind them.
        double behind = GraftMesh.smooth(GraftMesh.clamp01(w / 0.1));
        double feeding = hole + (walls - hole) * behind;
        return plain + (feeding - plain) * head;
    }

    /**
     * 3.6.3: how much bigger than a plain worm's (girth 1) a feeding worm's mouth is drawn: its hole thrown wide is the cavity's, as far
     * as its head has grown ({@code g} the plain worm's).
     */
    static double mouthScale(Feed feed, double g) {
        return feed == null ? g : g + (feed.hole() / holeRadius(1.0) - g) * GraftMesh.clamp01(feed.head());
    }

    /** Its radius {@code u} of the way along it as {@code pose} has it: feeding, a Walker's leg, a worm's, or a mouthless tentacle's. */
    static double profile(Pose pose, double u, double length, double open, boolean mouth) {
        if (pose.feed() != null) {
            return feedRadius(u, length, open, pose.feed());
        }
        if (!mouth) {
            return tip(u, length);
        }
        return pose.leg() ? legRadius(u, length, open) : radius(u, length, open);
    }

    /**
     * 4.0.0 (the owner: "the legs look like sticks"): a Walker's leg's radius {@code u} of the way along it: out of his hip as the worm, swelling
     * at once into a thick, muscled thigh (three quarters again as wide), tapering down its length to a worm's girth at the shin, a little
     * swell at the bend, its foot the worm's head and mouth.
     */
    public static double legRadius(double u, double length, double open) {
        double at = GraftMesh.clamp01(u) * length;
        double share = GraftMesh.clamp01(u);
        double thigh = 0.75 * GraftMesh.smooth(GraftMesh.clamp01(at / 0.9)) * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((share - 0.3) / 0.55)));
        double bend = 0.12 * bump(share - 0.5, 0.09);
        return radius(u, length, open) * (1.0 + thigh + bend);
    }

    /** 3.5.4: a mouthless tentacle's radius {@code u} of the way along it: out of its root as the worm's, tapering over its last third. */
    static double tip(double u, double length) {
        double at = GraftMesh.clamp01(u) * length;
        double base = ROOT_RADIUS + (BODY_RADIUS - ROOT_RADIUS) * GraftMesh.smooth(at / 0.6);
        return base * (0.12 + 0.88 * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((u - 0.62) / 0.38))));
    }

    /** The lip's radius: the head flares as the mouth is thrown wide. */
    public static double lipRadius(double open) {
        return LIP_RADIUS * (1.0 + 0.42 * GraftMesh.smooth(GraftMesh.clamp01((open - 0.45) / 0.55)));
    }

    /** The hole's radius inside the lip (0 shut .. most of the lip thrown wide). */
    public static double holeRadius(double open) {
        return lipRadius(open) * (0.2 + 0.64 * GraftMesh.smooth(open));
    }

    private static double bump(double x, double width) {
        return Math.exp(-(x * x) / (width * width));
    }

    /** How high a ridge stands at {@code at} blocks along (0 in a groove .. 1 on a crest), irregular. */
    static double ridge(double at, double a, int seed) {
        double wobble = 0.35 * (GraftMesh.noise(seed, at * 2.1, a * 0.6) - 0.5) + 0.12 * Math.sin(a * 2.0 + seed);
        double phase = at / RIDGE + wobble;
        double crest = Math.sin(phase * Math.PI * 2.0);
        return Math.pow(Math.max(0.0, crest), 2.2);
    }

    private static void body(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose, double g, double open, boolean fine, boolean mouth) {
        double length = spine.length();
        int seed = pose.seed();
        double t = pose.time();
        int stations = fine ? spine.samples() : Math.max(12, spine.samples());
        int sides = fine ? 16 : 10;
        // It breathes: a slow swell running down it from the head.
        // Its flesh runs right up to the lip (by length: a share of a long worm would leave a gap behind the mouth).
        double end = Math.max(0.5, (length - 0.004) / length);
        GraftMesh.tube(sink, spine, 0.0, end, stations, sides, new GraftMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double at = s * length;
                double r = profile(pose, s, length, open, mouth);
                double crest = ridge(at, a, seed);
                double fold = 0.035 * Math.sin(a * 5.0 + 2.6 * GraftMesh.noise(seed + 3, at * 1.7, 0.0)) * GraftMesh.smooth(at / 0.4);
                double lump = 0.07 * (GraftMesh.noise(seed + 11, at * 3.2, a * 1.3) - 0.5);
                double breathe = 0.03 * Math.sin(t * 0.12 - at * 2.2);
                double ridged = s > 0.9 ? 0.5 : 1.0;
                return g * r * (1.0 + 0.1 * crest * ridged + fold + lump + breathe);
            }

            @Override
            public int colour(double s, double a) {
                double at = s * length;
                double crest = ridge(at, a, seed);
                double mottle = GraftMesh.noise(seed + 5, at * 4.0, a * 2.0);
                // Its belly (the side away from its back) a little lighter, the grooves dark, the crests catching light.
                double belly = Math.max(0.0, -Math.sin(a));
                int c = GraftMesh.mix(FLESH_DEEP, FLESH, 0.45 + 0.55 * crest);
                c = GraftMesh.mix(c, FLESH_LIGHT, 0.35 * crest * crest);
                c = GraftMesh.mix(c, BELLY, 0.3 * belly);
                c = GraftMesh.mix(c, FLESH_DEEP, 0.25 * mottle);
                // Darker where it tears out of the back.
                return GraftMesh.mix(c, FLESH_DEEP, 0.5 * (1.0 - GraftMesh.smooth(at / 0.35)));
            }
        }, 1.0);
    }

    /** Glowing veins wandering down it, a pulse of RC running along them toward the mouth. */
    private static void veins(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose, double g, double open, boolean fine, boolean mouth) {
        double length = spine.length();
        int count = fine ? 6 : 4;
        int steps = (int)Math.ceil(length * (fine ? 9.0 : 4.0));
        for (int v = 0; v < count; v++) {
            double a0 = Math.PI * 2.0 * v / count + 0.7 * GraftMesh.hash(pose.seed() * 31 + v);
            double from = 0.04 + 0.05 * GraftMesh.hash(pose.seed() + v * 7);
            double to = 0.84 + 0.08 * GraftMesh.hash(pose.seed() + v * 13);
            double[][] prev = null;
            double prevGlow = 0.0;
            for (int i = 0; i <= steps; i++) {
                double s = from + (to - from) * i / steps;
                double at = s * length;
                double a = a0 + 0.55 * Math.sin(at * 2.3 + v * 1.9) + 0.9 * (GraftMesh.noise(pose.seed() + 40 + v, at * 1.6, 0.0) - 0.5)
                    + 0.18 * Math.sin(at * 9.0 + v);
                double r = g * profile(pose, s, length, open, mouth)
                    * (1.0 + 0.1 * ridge(at, a, pose.seed())) + 0.006;
                double[][] f = spine.at(s);
                double[] out = GraftMesh.add(GraftMesh.mul(f[3], Math.cos(a)), GraftMesh.mul(f[2], Math.sin(a)));
                double[] round = GraftMesh.add(GraftMesh.mul(f[3], -Math.sin(a)), GraftMesh.mul(f[2], Math.cos(a)));
                double width = g * 0.0125 * (0.45 + 0.55 * Math.sin(Math.PI * (i / (double)steps))) * (0.6 + 0.8 * GraftMesh.noise(pose.seed() + 90 + v, at * 3.0, 0.0));
                double[] centre = GraftMesh.add(f[0], GraftMesh.mul(out, r));
                double[][] edge = {GraftMesh.sub(centre, GraftMesh.mul(round, width)), GraftMesh.add(centre, GraftMesh.mul(round, width))};
                double glow = 0.55 + 0.45 * Math.pow(0.5 + 0.5 * Math.sin(at * 3.0 - pose.time() * 0.35), 3.0);
                // Sunk in the grooves, it dims; where it crosses a crest it shows.
                glow *= 0.55 + 0.45 * ridge(at, a, pose.seed());
                if (prev != null) {
                    int c0 = GraftMesh.mix(GraftMesh.mix(VEIN_DIM, VEIN, prevGlow), VEIN_HOT, Math.max(0.0, prevGlow - 0.7));
                    int c1 = GraftMesh.mix(GraftMesh.mix(VEIN_DIM, VEIN, glow), VEIN_HOT, Math.max(0.0, glow - 0.7));
                    GraftMesh.glow(sink, prev[0], edge[0], edge[1], prev[1], GraftMesh.argb(c0, 1.0), GraftMesh.argb(c1, 1.0),
                        GraftMesh.argb(c1, 1.0), GraftMesh.argb(c0, 1.0));
                }
                prev = edge;
                prevGlow = glow;
            }
        }
    }

    /** Grey bone spurs raked back along its top, a crown of them round the back of its head. */
    private static void spurs(UkakuModel.Sink sink, KakujaModel.Spine spine, Pose pose, double g, boolean fine, boolean mouth) {
        double length = spine.length();
        int sides = fine ? 6 : 4;
        int stations = fine ? 4 : 2;
        int n = 0;
        for (double at = 0.38; at < length - 0.62; at += 0.27, n++) {
            double s = at / length;
            double a = Math.PI * 0.5 + (n % 2 == 0 ? 0.55 : -0.55) + 0.25 * (GraftMesh.hash(pose.seed() * 17 + n) - 0.5);
            double r = g * profile(pose, s, length, 0.0, mouth);
            double[][] on = GraftMesh.onSurface(spine, s, a, r * 0.86);
            double[] dir = GraftMesh.normalize(GraftMesh.sub(GraftMesh.mul(on[1], 0.85), GraftMesh.mul(on[2], 0.55)));
            double len = g * (0.17 + 0.1 * GraftMesh.hash(pose.seed() * 5 + n));
            GraftMesh.horn(sink, on[0], dir, GraftMesh.mul(on[2], -1.0), len, g * 0.05, sides, stations, SPUR_ROOT, SPUR_TIP, 1.0);
        }
    }

    // ---------------------------------------------------------------- the mouth

    private static void mouth(UkakuModel.Sink sink, UkakuModel.Sink flesh, KakujaModel.Spine spine, Pose pose, double g, double open, boolean fine) {
        double[][] f = spine.at(1.0);
        double[] centre = f[0];
        double[] fwd = f[1];
        double[] dorsal = f[2];
        double[] edge = f[3];
        double grown = GraftMesh.smooth(GraftMesh.clamp01((pose.grown() - 0.35) / 0.65));
        // 3.6.3: a feeding worm's mouth as big as its cavity (its jaws close over it; the cavity stays).
        Feed feed = pose.feed();
        double head = feed == null ? 0.0 : GraftMesh.clamp01(feed.head());
        g = mouthScale(feed, g);
        double lip = g * lipRadius(open);
        double hole = g * holeRadius(open) * (0.4 + 0.6 * grown);
        double cavity = feed == null ? hole : hole + (feed.hole() - hole) * head;
        int sides = fine ? 20 : 12;
        // The lip: rolled forward round the hole, a darker inner edge going in.
        int rows = fine ? 5 : 3;
        double[][][] ring = new double[rows + 1][sides][];
        int[] tone = new int[rows + 1];
        for (int i = 0; i <= rows; i++) {
            double v = i / (double)rows;
            double r = lip + (hole - lip) * v;
            double ahead = g * ((0.05 + 0.04 * (1.0 - open)) * Math.sin(Math.PI * v) * (1.0 - 0.4 * open) - 0.02 * v);
            for (int j = 0; j < sides; j++) {
                double a = Math.PI * 2.0 * j / sides;
                // Shut, drawn tight into creases (a sphincter): deep folds between the fangs, the rim scalloped.
                double crease = Math.pow(Math.abs(Math.sin(a * FANGS * 1.5)), 0.6);
                double pucker = 1.0 + (0.06 * Math.sin(a * FANGS) + 0.12 * v * (crease - 0.6)) * (1.0 - open);
                double[] out = GraftMesh.add(GraftMesh.mul(edge, Math.cos(a)), GraftMesh.mul(dorsal, Math.sin(a)));
                ring[i][j] = GraftMesh.add(GraftMesh.add(centre, GraftMesh.mul(out, r * pucker)), GraftMesh.mul(fwd, ahead));
            }
            tone[i] = GraftMesh.argb(GraftMesh.mix(LIP, LIP_INNER, v), 1.0);
        }
        for (int i = 0; i < rows; i++) {
            double[] mid = GraftMesh.add(centre, GraftMesh.mul(fwd, -g * 0.1));
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                GraftMesh.facet(flesh, ring[i][j], ring[i + 1][j], ring[i + 1][k], ring[i][k], mid, tone[i], tone[i + 1], tone[i + 1], tone[i]);
            }
        }
        // The throat: in from the hole, narrowing and going dark; its walls ridged.
        double depth = g * (0.32 + 0.12 * open);
        if (feed != null) {
            depth += (feed.depth() - depth) * head;
        }
        int deep = fine ? (feed != null ? 10 : 6) : (feed != null ? 5 : 3);
        double[][][] throat = new double[deep + 1][sides][];
        int[] shade = new int[deep + 1];
        double[] lipIn = GraftMesh.add(centre, GraftMesh.mul(fwd, -g * 0.02));
        for (int i = 0; i <= deep; i++) {
            double w = i / (double)deep;
            double r = throatRadius(hole, cavity, head, w, pose.time(), pose.seed());
            double[] c = GraftMesh.add(lipIn, GraftMesh.mul(fwd, -depth * w));
            for (int j = 0; j < sides; j++) {
                double a = Math.PI * 2.0 * j / sides;
                double[] out = GraftMesh.add(GraftMesh.mul(edge, Math.cos(a)), GraftMesh.mul(dorsal, Math.sin(a)));
                throat[i][j] = GraftMesh.add(c, GraftMesh.mul(out, r));
            }
            shade[i] = GraftMesh.argb(w < 0.4 ? GraftMesh.mix(GUM, THROAT, w / 0.4) : GraftMesh.mix(THROAT, THROAT_DEEP, (w - 0.4) / 0.6), 1.0);
        }
        for (int i = 0; i < deep; i++) {
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                // Seen from inside: the centre it faces away from is the throat's own axis further in.
                double[] axis = GraftMesh.add(lipIn, GraftMesh.mul(fwd, -depth * 2.0));
                GraftMesh.facet(sink, throat[i][k], throat[i + 1][k], throat[i + 1][j], throat[i][j], GraftMesh.add(axis, GraftMesh.mul(fwd, depth * 4.0)),
                    shade[i], shade[i + 1], shade[i + 1], shade[i]);
            }
        }
        // A plug of dark at the bottom of the throat.
        double[] bottom = GraftMesh.add(lipIn, GraftMesh.mul(fwd, -depth));
        for (int j = 0; j < sides; j++) {
            int k = (j + 1) % sides;
            GraftMesh.facet(sink, throat[deep][j], throat[deep][k], bottom, bottom, GraftMesh.add(bottom, GraftMesh.mul(fwd, -1.0)),
                shade[deep], shade[deep], GraftMesh.argb(THROAT_DEEP, 1.0), GraftMesh.argb(THROAT_DEEP, 1.0));
        }
        // Rings of hooked teeth down the throat, pointing in and down it (what is bitten goes one way). 3.6.3: a feeding worm's cavity
        // has more rows, all down its walls, and more teeth in each (as many as its wider ring takes).
        int toothSides = fine ? 5 : 4;
        int toothRows = feed != null && head > 0.5 ? FEED_TEETH_ROWS : TEETH.length;
        for (int r = 0; r < toothRows; r++) {
            int plain = TEETH[Math.min(r, TEETH.length - 1)];
            double w = toothRows == TEETH.length ? 0.06 + 0.3 * r : 0.05 + 0.82 * r / (toothRows - 1.0);
            double radius = throatRadius(hole, cavity, head, w, pose.time(), pose.seed()) * 0.98;
            int count = toothRows == TEETH.length ? (fine ? plain : Math.max(6, plain - 4))
                : (int)Math.max(plain, Math.min(fine ? 44 : 24, Math.round(Math.PI * 2.0 * radius / (g * (fine ? 0.085 : 0.15)))));
            double[] c = GraftMesh.add(lipIn, GraftMesh.mul(fwd, -depth * w));
            int rr = toothRows == TEETH.length ? r : Math.min(2, r / 2);
            double len = g * (0.1 - 0.022 * rr) * (0.55 + 0.45 * grown);
            for (int k = 0; k < count; k++) {
                double a = Math.PI * 2.0 * (k + 0.5 * r) / count + 0.2 * GraftMesh.hash(pose.seed() * 13 + r * 41 + k);
                double[] out = GraftMesh.add(GraftMesh.mul(edge, Math.cos(a)), GraftMesh.mul(dorsal, Math.sin(a)));
                double[] base = GraftMesh.add(c, GraftMesh.mul(out, radius));
                double[] dir = GraftMesh.normalize(GraftMesh.sub(GraftMesh.mul(out, -0.72), GraftMesh.mul(fwd, 0.55)));
                double l = len * (0.75 + 0.5 * GraftMesh.hash(pose.seed() * 7 + r * 29 + k));
                GraftMesh.horn(sink, base, dir, GraftMesh.mul(fwd, -1.0), l, g * (0.019 - 0.003 * rr), toothSides, fine ? 3 : 2, TOOTH_ROOT, TOOTH_TIP, 1.0);
            }
        }
        // The crown of great fangs on the lip: shut, they lean in and meet over the hole; open, they splay out, hooked at their points.
        double splay = GraftMesh.smooth(open);
        for (int k = 0; k < FANGS; k++) {
            double a = Math.PI * 2.0 * k / FANGS + Math.PI / FANGS + 0.12 * (GraftMesh.hash(pose.seed() * 3 + k) - 0.5);
            double[] out = GraftMesh.add(GraftMesh.mul(edge, Math.cos(a)), GraftMesh.mul(dorsal, Math.sin(a)));
            double[] round = GraftMesh.add(GraftMesh.mul(edge, -Math.sin(a)), GraftMesh.mul(dorsal, Math.cos(a)));
            double[] base = GraftMesh.add(GraftMesh.add(centre, GraftMesh.mul(out, lip * (0.95 - 0.09 * splay))), GraftMesh.mul(fwd, -g * (0.09 - 0.06 * splay)));
            double[] shut = GraftMesh.add(GraftMesh.sub(GraftMesh.mul(fwd, 0.86), GraftMesh.mul(out, 0.5)), GraftMesh.mul(round, 0.12));
            double[] wide = GraftMesh.add(GraftMesh.mul(fwd, 0.72), GraftMesh.mul(out, 0.7));
            double[] dir = GraftMesh.normalize(GraftMesh.lerp(shut, wide, splay));
            double len = g * FANG_LENGTH * (0.9 + 0.2 * GraftMesh.hash(pose.seed() * 11 + k)) * (0.3 + 0.7 * grown) * (0.8 + 0.35 * open);
            fang(sink, base, dir, GraftMesh.mul(out, -1.0), round, len, g * 0.095, g * 0.036, fine);
        }
    }

    /**
     * A great fang: a broad, curved claw (wide across the lip, thin through it), from {@code base} along {@code dir}, hooked toward
     * {@code hook} at its point.
     */
    static void fang(UkakuModel.Sink sink, double[] base, double[] dir, double[] hook, double[] across, double length, double width, double thick,
                     boolean fine) {
        int stations = fine ? 6 : 3;
        int sides = fine ? 8 : 5;
        double[] d = GraftMesh.normalize(dir);
        double[] h = GraftMesh.sub(hook, GraftMesh.mul(d, GraftMesh.dot(hook, d)));
        h = GraftMesh.dot(h, h) < 1.0E-9 ? GraftMesh.perpendicular(d) : GraftMesh.normalize(h);
        double[][] ctrl = {base, GraftMesh.add(base, GraftMesh.mul(d, length * 0.42)),
            GraftMesh.add(GraftMesh.add(base, GraftMesh.mul(d, length * 0.8)), GraftMesh.mul(h, length * 0.12)),
            GraftMesh.add(GraftMesh.add(base, GraftMesh.mul(d, length * 0.95)), GraftMesh.mul(h, length * 0.38))};
        double[][][] rings = new double[stations + 1][sides][];
        int[] colour = new int[stations + 1];
        for (int i = 0; i <= stations; i++) {
            double u = i / (double)stations;
            double[] p = bezier(ctrl, u);
            double[] t = GraftMesh.normalize(GraftMesh.sub(bezier(ctrl, Math.min(1.0, u + 0.04)), bezier(ctrl, Math.max(0.0, u - 0.04))));
            double[] wide = GraftMesh.normalize(GraftMesh.sub(across, GraftMesh.mul(t, GraftMesh.dot(across, t))));
            double[] flat = GraftMesh.cross(t, wide);
            double taper = Math.pow(1.0 - u, 0.85);
            for (int j = 0; j < sides; j++) {
                double a = Math.PI * 2.0 * j / sides;
                rings[i][j] = GraftMesh.add(p, GraftMesh.add(GraftMesh.mul(wide, Math.cos(a) * width * taper), GraftMesh.mul(flat, Math.sin(a) * thick * taper)));
            }
            colour[i] = GraftMesh.argb(GraftMesh.mix(FANG_ROOT, FANG_TIP, Math.pow(u, 0.7)), 1.0);
        }
        for (int i = 0; i < stations; i++) {
            double[] mid = bezier(ctrl, (i + 0.5) / stations);
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                GraftMesh.facet(sink, rings[i][j], rings[i + 1][j], rings[i + 1][k], rings[i][k], mid, colour[i], colour[i + 1], colour[i + 1], colour[i]);
            }
        }
    }

    private static double[] bezier(double[][] c, double u) {
        double v = 1.0 - u;
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            out[k] = v * v * v * c[0][k] + 3 * v * v * u * c[1][k] + 3 * v * u * u * c[2][k] + u * u * u * c[3][k];
        }
        return out;
    }
}
