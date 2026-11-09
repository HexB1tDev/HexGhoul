package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.graft.GraftMesh;

/**
 * 3.6.3, Grand Devouring (pure, tested; the owner's words in {@code docs/handoff/owner-words-mutations.md}): one worm's feeding on a body
 * at one heart or less, from the moment it is chosen to the moment it is a plain worm again.
 *
 * <ol>
 *   <li>{@link #RISE}: it rears up above its place, swelling (thicker, its head growing into a great bell of a mouth sized to its victim),
 *   its jaws thrown wider and wider.</li>
 *   <li>{@link #REACH}: it arches over, its mouth coming round to hang open over its victim (the owner's drawing).</li>
 *   <li>{@link #ENGULF}: it comes down over it, the whole body inside the bell, to the ground.</li>
 *   <li>{@link #TRAPPED}: its jaws slowly close under it while it is drawn up into the cavity.</li>
 *   <li>{@link #SWALLOW}: shut, the gulp; the body goes deeper and dies at its end ({@link #KILL}).</li>
 *   <li>{@link #DIGEST}: a bulge the size of the body runs down the whole worm to his back, the flesh stretched over it.</li>
 *   <li>{@link #SINK}: it sinks into his back: fed ({@link #FED}: the bar refilled).</li>
 *   <li>{@link #RETURN}: back to its own size and place ({@link #END}); until then it does nothing else.</li>
 * </ol>
 *
 * <p>Body frame (x right, y up, z ahead; blocks); the victim's feet {@code victim} where it was caught (held there), its size
 * {@code width} by {@code height}: the mouth, the cavity, the teeth, the swelling and the bulge all scale to it.
 */
public final class GrandDevour {
    public static final int RISE = 50;
    public static final int REACH = 30;
    public static final int ENGULF = 30;
    public static final int TRAPPED = 80;
    public static final int SWALLOW = 30;
    public static final int DIGEST = 180;
    public static final int SINK = 30;
    public static final int RETURN = 30;
    /** When its mouth is down over its victim (caught), when its jaws are shut, when the victim dies, when he is fed, when it is done. */
    public static final int CAUGHT = RISE + REACH + ENGULF;
    public static final int SHUT = CAUGHT + TRAPPED;
    public static final int KILL = SHUT + SWALLOW;
    public static final int FED = KILL + DIGEST + SINK;
    public static final int END = FED + RETURN;
    /** Points along its arch, and control points of its whole spine (the root, out of his back, the arch, its head's three). */
    static final int ARCH = 14;
    static final int POINTS = ARCH + 5;
    /**
     * The owner: "it's not progressively getting bigger at all it jumps into a massive size  insantly". It swells on through its rise and
     * while it arches over: its body all the way by {@link #GROWN}, its mouth (the bell round the cavity) by the time it is over its victim.
     */
    public static final int GROWN = RISE + REACH / 2;
    /**
     * Its arch's crest, where it hangs its mouth down over its victim (the owner's drawing): this far along the way to its head, and as
     * high over the back of its head as this share of the rest of the way (most of a quarter turn down into it).
     */
    static final double CREST_AT = 0.4;
    static final double CREST_RISE = 0.55;

    private GrandDevour() {
    }

    /** One feeding worm this frame: its shape and its swelling (3.6.3: kept clear of his body by its own feeding radii, {@code MawMotion.around}). */
    public record Feeding(MawMotion.Shape shape, MawWorm.Feed feed) {
    }

    // ---------------------------------------------------------------- its size, from its victim's

    /** The cavity's radius: round the victim's widest (its corners) and wide enough for its height (a bell, not a pipe). */
    public static double hole(double width, double height) {
        return Math.max(width * 0.71 + 0.2, height * 0.42);
    }

    /** The cavity's depth: the whole of the victim's height and some. */
    public static double depth(double width, double height) {
        return height + 0.3 + 0.15 * hole(width, height);
    }

    /** The swollen body's radius. */
    public static double body(double width, double height) {
        return Math.max(0.5, 0.5 * hole(width, height));
    }

    /** The bulge's radius and length: the body swallowed, squeezed long. */
    public static double bulge(double width, double height) {
        return Math.max(1.6 * body(width, height), 0.6 * width + 0.2 * height + 0.2);
    }

    public static double bulgeLength(double width, double height) {
        return 0.6 * height + width + 0.4;
    }

    // ---------------------------------------------------------------- the victim

    /** How far up (blocks) the victim's feet have been drawn into the cavity, {@code age} ticks in. */
    public static double lift(double age, double height) {
        double trapped = GraftMesh.smooth(GraftMesh.clamp01((age - CAUGHT) / TRAPPED)) * 0.3 * height;
        double swallowed = GraftMesh.smooth(GraftMesh.clamp01((age - SHUT) / SWALLOW)) * (0.35 * height + 0.3);
        return trapped + swallowed;
    }

    /** True while the victim is still drawn: until a little into the gulp (shut in by then, out of sight). */
    public static boolean shown(double age) {
        return age < SHUT + SWALLOW * 0.3;
    }

    /** True while its worm can do nothing else. */
    public static boolean busy(double age) {
        return age >= 0.0 && age < END;
    }

    // ---------------------------------------------------------------- the worm

    /**
     * Worm {@code i} of {@code worms} feeding on its victim ({@code victim}: its feet, body frame; {@code width} by {@code height}),
     * {@code age} ticks in, {@code time} ticks into the world, {@code seed} its own.
     */
    public static Feeding at(double time, int seed, int worms, int i, double[] victim, double width, double height, double age) {
        double side = worms <= 1 || i == 0 ? 1.0 : i == 1 ? -1.0 : 0.0;
        double hole = hole(width, height);
        double depth = depth(width, height);
        double bodyR = body(width, height);
        MawMotion.Shape rest = MawMotion.framed(worms, i, MawMotion.idle(time, seed));
        double[][] idle = resample(rest.path(), POINTS);
        double swell = age < GROWN ? GraftMesh.smooth(GraftMesh.clamp01(age / GROWN)) : age < FED ? 1.0
            : 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - FED) / RETURN));
        // Swollen until the bulge is in him (the owner: "Once the digestion animation finishes, the tentacle gradually returns to its normal
        // size and position").
        double head = age < RISE + REACH ? GraftMesh.smooth(GraftMesh.clamp01((age - 5.0) / (RISE + REACH - 5.0)))
            : age < FED ? 1.0
            : 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - FED) / RETURN));
        MawWorm.Feed probe = new MawWorm.Feed(hole, depth, bodyR, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0);
        double headLen = depth + 0.3 * probe.headRadius();
        // Its poses, each one smooth arch out of his back, leaning back as it leaves it and curving over into its head (the owner's
        // drawing, and: "when I said arch smoothly the image describes exactly what I mean"): reared up over him, hanging open over its
        // victim, down over it, and shut with it inside, lifted a little, while it digests.
        double[][] raised = raised(side, victim, headLen);
        double[][] over = hang(side, victim, headLen, height + 0.3 + 0.1 * height);
        double[][] down = hang(side, victim, headLen, -0.05 + lift(age, height));
        double[][] carry = carry(side, victim, height, headLen);
        double[][] path;
        double open;
        if (age < RISE) {
            double k = GraftMesh.smooth(GraftMesh.clamp01(age / RISE));
            path = mix(idle, raised, k);
            open = rest.aperture() + (1.0 - rest.aperture()) * GraftMesh.smooth(GraftMesh.clamp01(age / (RISE * 0.85)));
        } else if (age < RISE + REACH) {
            path = mix(raised, over, GraftMesh.smooth((age - RISE) / REACH));
            open = 1.0;
        } else if (age < CAUGHT) {
            path = mix(over, down, GraftMesh.smooth((age - RISE - REACH) / ENGULF));
            open = 1.0;
        } else if (age < SHUT) {
            path = down;
            // The jaws close slowly under it (its feet drawn up past them).
            open = 1.0 - 0.92 * GraftMesh.smooth((age - CAUGHT) / TRAPPED);
        } else if (age < KILL) {
            // The gulp: it stays down over it until it is in out of sight, then lifts.
            double k = GraftMesh.smooth(GraftMesh.clamp01((age - SHUT - SWALLOW * 0.35) / (SWALLOW * 0.65)));
            path = mix(down, carry, k);
            open = 0.08 * (1.0 - GraftMesh.smooth((age - SHUT) / SWALLOW));
        } else if (age < FED) {
            // Shut, held in its arch while the bulge runs down it, swaying a little as it works (eased in).
            path = sway(carry, time, seed, 0.12 * GraftMesh.smooth(GraftMesh.clamp01((age - KILL) / 20.0)));
            open = 0.0;
        } else {
            path = mix(sway(carry, time, seed, 0.12), idle, GraftMesh.smooth(GraftMesh.clamp01((age - FED) / RETURN)));
            open = rest.aperture() * GraftMesh.smooth(GraftMesh.clamp01((age - FED) / RETURN));
        }
        // The bulge: forming at the back of its cavity as it gulps, down the whole worm, into his back.
        double bulgeShown = 0.0;
        double bulgeAt = 1.0;
        double length = length(path);
        double inHead = GraftMesh.clamp01(1.0 - (0.85 * depth) / Math.max(0.5, length));
        if (age >= KILL - SWALLOW * 0.5 && age < FED) {
            bulgeShown = GraftMesh.smooth(GraftMesh.clamp01((age - (KILL - SWALLOW * 0.5)) / (SWALLOW * 0.5)));
            double run = GraftMesh.smooth(GraftMesh.clamp01((age - KILL) / DIGEST));
            bulgeAt = inHead + (0.03 - inHead) * run;
            if (age >= KILL + DIGEST) {
                // Into his back: it shrinks away into him.
                double sunk = GraftMesh.smooth(GraftMesh.clamp01((age - KILL - DIGEST) / SINK));
                bulgeShown *= 1.0 - sunk;
                bulgeAt = 0.03 * (1.0 - sunk);
            }
        }
        MawWorm.Feed feed = new MawWorm.Feed(hole, depth, bodyR, swell, head, bulgeAt, bulge(width, height), bulgeLength(width, height), bulgeShown);
        return new Feeding(new MawMotion.Shape(path, MawMotion.dorsal(), GraftMesh.clamp01(open)), feed);
    }

    /** Out of his back, its arch's foot: just out of the root to its side, a little up and back. */
    static double[] exit(double side) {
        return new double[] {MawMotion.ROOT[0] + side * 0.3, MawMotion.ROOT[1] + 0.3, MawMotion.ROOT[2] - 0.32};
    }

    /** The way its arch leaves his back (the owner, on its going straight up: "the kagune is literally VERTICAL from the point of origin"): back and up, leaning out to its side. */
    static double[] rising(double side) {
        return GraftMesh.normalize(new double[] {side * 0.4, 0.62, -0.68});
    }

    /**
     * Reared up over him: the back of its head over and a little behind his back, its head turned up and out toward its victim, gaping
     * (its arch leaning back out of his back and curling up into it).
     */
    static double[][] raised(double side, double[] victim, double headLen) {
        double[] from = exit(side);
        double[] back = {from[0] + side * 0.4, from[1] + 1.6 + 0.35 * headLen, from[2] - 0.4 - 0.15 * headLen};
        double[] dir = GraftMesh.normalize(GraftMesh.add(GraftMesh.mul(level(GraftMesh.sub(victim, back)), 0.75), new double[] {0.0, 0.65, 0.0}));
        double[] mouth = GraftMesh.add(back, GraftMesh.mul(dir, headLen));
        return build(side, back, dir, mouth, false);
    }

    /** Its mouth hanging open straight down, {@code rim} blocks over its victim's feet (over its head, or down over it to the ground). */
    static double[][] hang(double side, double[] victim, double headLen, double rim) {
        double[] mouth = {victim[0], victim[1] + rim, victim[2]};
        double[] back = GraftMesh.add(mouth, new double[] {0.0, headLen, 0.0});
        return build(side, back, new double[] {0.0, -1.0, 0.0}, mouth, true);
    }

    /** Shut with its victim inside: its head lifted a little off the ground where it caught it, tilted a little out, its arch over to it. */
    static double[][] carry(double side, double[] victim, double height, double headLen) {
        double[] mouth = {victim[0] * 0.92, victim[1] + 0.2 * height + 0.6, victim[2] * 0.92};
        double[] dir = GraftMesh.normalize(GraftMesh.add(new double[] {0.0, -1.0, 0.0}, GraftMesh.mul(level(mouth), 0.25)));
        double[] back = GraftMesh.sub(mouth, GraftMesh.mul(dir, headLen));
        return build(side, back, dir, mouth, true);
    }

    /**
     * Its spine: the root, out of his back, one smooth arch to the back of its head {@code back} (arriving along {@code dir}), then its head
     * straight on to its mouth. The arch is made of circular arcs meeting without a corner: leaving his back leaning back and up
     * ({@link #rising}) and curving over; {@code crest}: through a crest on the way, level there, before it turns down into its head
     * (a mouth hanging down over its victim), or else straight round into its head (a head turned toward its victim).
     */
    static double[][] build(double side, double[] back, double[] dir, double[] mouth, boolean crest) {
        double[] from = exit(side);
        double[] up = rising(side);
        java.util.List<double[]> dense = new java.util.ArrayList<>();
        dense.add(from.clone());
        if (crest) {
            double[] flat = level(GraftMesh.sub(back, from));
            double span = Math.hypot(back[0] - from[0], back[2] - from[2]);
            double[] top = GraftMesh.add(from, GraftMesh.mul(flat, CREST_AT * span));
            top[1] = Math.max(back[1] + CREST_RISE * (1.0 - CREST_AT) * span + 0.25, from[1] + 1.0 + 0.25 * span);
            biarc(dense, from, up, top, flat, 40);
            biarc(dense, top, flat, back, dir, 40);
        } else {
            biarc(dense, from, up, back, dir, 60);
        }
        double[][] arch = resample(dense.toArray(new double[0][]), ARCH + 1);
        double[][] out = new double[POINTS][];
        out[0] = MawMotion.ROOT.clone();
        for (int k = 0; k <= ARCH; k++) {
            out[1 + k] = arch[k];
        }
        out[2 + ARCH] = GraftMesh.lerp(back, mouth, 1.0 / 3.0);
        out[3 + ARCH] = GraftMesh.lerp(back, mouth, 2.0 / 3.0);
        out[4 + ARCH] = mouth.clone();
        return out;
    }

    /**
     * Two circular arcs from {@code p0} (leaving along {@code t0}) to {@code p1} (arriving along {@code t1}; both unit), meeting without a
     * corner: {@code n} points each after {@code p0} into {@code out}, the last {@code p1}.
     */
    static void biarc(java.util.List<double[]> out, double[] p0, double[] t0, double[] p1, double[] t1, int n) {
        double[] v = GraftMesh.sub(p1, p0);
        double vv = GraftMesh.dot(v, v);
        if (vv < 1.0E-12) {
            out.add(p1.clone());
            return;
        }
        double denom = 2.0 * (1.0 - GraftMesh.dot(t0, t1));
        double d;
        if (denom < 1.0E-6) {
            double vt = GraftMesh.dot(v, t1);
            d = Math.abs(vt) < 1.0E-9 ? 0.5 * Math.sqrt(vv) : vv / (4.0 * vt);
        } else {
            double vt = GraftMesh.dot(v, GraftMesh.add(t0, t1));
            d = (-vt + Math.sqrt(vt * vt + denom * vv)) / denom;
        }
        double[] joint = GraftMesh.mul(GraftMesh.add(GraftMesh.add(p0, p1), GraftMesh.mul(GraftMesh.sub(t0, t1), d)), 0.5);
        arc(out, p0, t0, joint, n);
        // The second from its far end backward, then turned round.
        java.util.List<double[]> back = new java.util.ArrayList<>();
        arc(back, p1, GraftMesh.mul(t1, -1.0), joint, n);
        for (int k = back.size() - 2; k >= 0; k--) {
            out.add(back.get(k));
        }
        out.add(p1.clone());
    }

    /** A circular arc from {@code a} (leaving along {@code t}, unit) to {@code b}: {@code n} points after {@code a} into {@code out}, the last {@code b}. */
    static void arc(java.util.List<double[]> out, double[] a, double[] t, double[] b, int n) {
        double[] c = GraftMesh.sub(b, a);
        double len = GraftMesh.length3(c);
        double along = GraftMesh.dot(c, t);
        double[] aside = GraftMesh.sub(c, GraftMesh.mul(t, along));
        double off = GraftMesh.length3(aside);
        if (len < 1.0E-9 || off < 1.0E-6 * len) {
            for (int k = 1; k <= n; k++) {
                out.add(GraftMesh.lerp(a, b, k / (double)n));
            }
            return;
        }
        double[] normal = GraftMesh.mul(aside, 1.0 / off);
        double half = Math.atan2(off / len, along / len);
        double radius = len / (2.0 * (off / len));
        double[] centre = GraftMesh.add(a, GraftMesh.mul(normal, radius));
        for (int k = 1; k <= n; k++) {
            double phi = 2.0 * half * k / n;
            out.add(k == n ? b.clone() : GraftMesh.add(centre, GraftMesh.add(GraftMesh.mul(normal, -radius * Math.cos(phi)),
                GraftMesh.mul(t, radius * Math.sin(phi)))));
        }
    }

    /** A slow sway through its arch (its head and its middle, never its root), {@code amount} blocks. */
    static double[][] sway(double[][] path, double time, int seed, double amount) {
        double[][] out = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            double k = Math.sin(Math.PI * 0.5 * i / (path.length - 1.0));
            out[i] = GraftMesh.add(path[i], new double[] {amount * k * Math.sin(time * 0.07 + seed), 0.5 * amount * k * Math.sin(time * 0.11 + seed * 2.0),
                amount * k * Math.sin(time * 0.05 + seed * 3.0)});
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    static double[] level(double[] v) {
        double[] flat = {v[0], 0.0, v[2]};
        return GraftMesh.dot(flat, flat) < 1.0E-9 ? new double[] {0.0, 0.0, 1.0} : GraftMesh.normalize(flat);
    }

    static double[][] mix(double[][] a, double[][] b, double t) {
        double[][] out = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            out[i] = GraftMesh.lerp(a[i], b[i], t);
        }
        return out;
    }

    static double length(double[][] path) {
        double total = 0.0;
        for (int i = 1; i < path.length; i++) {
            total += GraftMesh.length3(GraftMesh.sub(path[i], path[i - 1]));
        }
        return total;
    }

    /** {@code path} resampled to {@code n} points evenly along its length (its ends kept). */
    static double[][] resample(double[][] path, int n) {
        double total = length(path);
        double[][] out = new double[n][];
        int j = 0;
        double done = 0.0;
        for (int k = 0; k < n; k++) {
            double want = total * k / (n - 1.0);
            while (j < path.length - 2 && done + GraftMesh.length3(GraftMesh.sub(path[j + 1], path[j])) < want) {
                done += GraftMesh.length3(GraftMesh.sub(path[j + 1], path[j]));
                j++;
            }
            double seg = Math.max(1.0E-9, GraftMesh.length3(GraftMesh.sub(path[j + 1], path[j])));
            out[k] = GraftMesh.lerp(path[j], path[j + 1], GraftMesh.clamp01((want - done) / seg));
        }
        return out;
    }
}
