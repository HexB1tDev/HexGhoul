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
    static final int ARCH = 10;
    static final int POINTS = ARCH + 5;

    private GrandDevour() {
    }

    /** One feeding worm this frame: its shape, its swelling, the girth it is kept clear of his body by. */
    public record Feeding(MawMotion.Shape shape, MawWorm.Feed feed, double clearGirth) {
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
        double swell = age < RISE ? GraftMesh.smooth(GraftMesh.clamp01(age / RISE)) : age < FED ? 1.0
            : 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - FED) / RETURN));
        double head = age < RISE ? GraftMesh.smooth(GraftMesh.clamp01((age - 5.0) / (RISE - 5.0)))
            : age < KILL ? 1.0
            // Emptied as the bulge leaves it, the bell shrinks back (a little of it kept until he is fed).
            : age < FED ? 1.0 - 0.75 * GraftMesh.smooth(GraftMesh.clamp01((age - KILL) / (DIGEST * 0.4)))
            : 0.25 * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - FED) / RETURN)));
        MawWorm.Feed probe = new MawWorm.Feed(hole, depth, bodyR, swell, head, 0.0, 0.0, 0.0);
        double headLen = depth + 0.3 * probe.headRadius();
        // Its poses, each one smooth arch out of his back and over into its head (the owner's drawing): reared up, hanging open over its
        // victim, down over it, and carried up shut with it inside while it digests.
        double[][] raised = raised(side, victim, hole, depth, headLen);
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
            double k = GraftMesh.smooth((age - SHUT) / SWALLOW);
            path = mix(down, carry, k);
            open = 0.08 * (1.0 - k);
        } else if (age < FED) {
            // Shut, held up in its arch while the bulge runs down it, swaying a little as it works.
            path = sway(carry, time, seed, 0.12);
            open = 0.0;
        } else {
            path = mix(sway(carry, time, seed, 0.12), idle, GraftMesh.smooth(GraftMesh.clamp01((age - FED) / RETURN)));
            open = rest.aperture() * GraftMesh.smooth(GraftMesh.clamp01((age - FED) / RETURN));
        }
        // The bulge: forming in the head as it gulps, down the whole worm, into his back.
        double bulgeR = bulge(width, height);
        double bulge = 0.0;
        double bulgeAt = 1.0;
        double length = length(path);
        double inHead = GraftMesh.clamp01(1.0 - (0.5 * depth) / Math.max(0.5, length));
        if (age >= KILL - SWALLOW * 0.5 && age < FED) {
            bulge = bulgeR * GraftMesh.smooth(GraftMesh.clamp01((age - (KILL - SWALLOW * 0.5)) / (SWALLOW * 0.5)));
            double run = GraftMesh.smooth(GraftMesh.clamp01((age - KILL) / DIGEST));
            bulgeAt = inHead + (0.03 - inHead) * run;
            if (age >= KILL + DIGEST) {
                // Into his back: it shrinks away into him.
                bulge *= 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - KILL - DIGEST) / SINK));
                bulgeAt = 0.03 * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - KILL - DIGEST) / SINK)));
            }
        }
        MawWorm.Feed feed = new MawWorm.Feed(hole, depth, bodyR, swell, head, bulgeAt, bulge, bulgeLength(width, height));
        double clear = 1.0 + (bodyR / MawWorm.BODY_RADIUS - 1.0) * swell;
        return new Feeding(new MawMotion.Shape(path, MawMotion.dorsal(), GraftMesh.clamp01(open)), feed, clear);
    }

    /** Out of his back, its arch's foot: just out of the root to its side, a little up and back. */
    static double[] exit(double side) {
        return new double[] {MawMotion.ROOT[0] + side * 0.3, MawMotion.ROOT[1] + 0.3, MawMotion.ROOT[2] - 0.32};
    }

    /** The way its arch leaves his back: up, out to its side and back. */
    static double[] rising(double side) {
        return GraftMesh.normalize(new double[] {side * 0.35, 1.0, -0.45});
    }

    /** Reared up high over its own side, its head turned toward its victim and a little up, gaping. */
    static double[][] raised(double side, double[] victim, double hole, double depth, double headLen) {
        double[] crest = {side * 0.9, 3.6 + 0.5 * hole + 0.35 * depth, -0.3};
        double[] dir = GraftMesh.normalize(GraftMesh.add(GraftMesh.mul(level(GraftMesh.sub(victim, crest)), 0.85), new double[] {0.0, 0.3, 0.0}));
        double[] mouth = GraftMesh.add(crest, GraftMesh.mul(dir, 0.3 * headLen));
        double[] back = GraftMesh.sub(mouth, GraftMesh.mul(dir, headLen));
        return build(side, back, dir, mouth, 0.55);
    }

    /** Its mouth hanging open straight down, {@code rim} blocks over its victim's feet (over its head, or down over it to the ground). */
    static double[][] hang(double side, double[] victim, double headLen, double rim) {
        double[] mouth = {victim[0], victim[1] + rim, victim[2]};
        double[] back = GraftMesh.add(mouth, new double[] {0.0, headLen, 0.0});
        return build(side, back, new double[] {0.0, -1.0, 0.0}, mouth, 0.0);
    }

    /** Shut with its victim inside, its head lifted off the ground and drawn a little back toward him, hanging down and forward. */
    static double[][] carry(double side, double[] victim, double height, double headLen) {
        double[] mouth = {victim[0] * 0.75 + side * 0.25, victim[1] + 0.45 * height + 1.5, victim[2] * 0.75};
        double[] dir = GraftMesh.normalize(GraftMesh.add(new double[] {0.0, -1.0, 0.0}, GraftMesh.mul(level(mouth), 0.45)));
        double[] back = GraftMesh.sub(mouth, GraftMesh.mul(dir, headLen));
        return build(side, back, dir, mouth, 0.0);
    }

    /**
     * Its spine: the root, out of his back, one smooth arch (a curve with no corner: up out of his back, over, and coming round into its
     * head along {@code dir}) to the back of its head {@code back}, then its head straight on to its mouth. {@code reach} 0: the arch
     * goes high over (a head hanging down); more: it comes in from behind (a head turned forward).
     */
    static double[][] build(double side, double[] back, double[] dir, double[] mouth, double reach) {
        double[] from = exit(side);
        double[] up = rising(side);
        double span = Math.hypot(back[0] - from[0], back[2] - from[2]);
        // As high over as it is long (the owner: "dont be afraid to make it stretch arch longer"): the arch's top well over both its feet.
        double top = Math.max(from[1], back[1]) + 1.1 + 0.35 * span;
        double a = Math.max(0.8, (top - from[1]) * 1.25);
        double b = reach > 0.0 ? Math.max(0.8, reach * GraftMesh.length3(GraftMesh.sub(back, from))) : Math.max(0.6, (top - back[1]) * 1.25);
        double[] c1 = GraftMesh.add(from, GraftMesh.mul(up, a));
        double[] c2 = GraftMesh.sub(back, GraftMesh.mul(dir, b));
        double[][] arch = bezier(from, c1, c2, back, ARCH);
        double[][] out = new double[POINTS][];
        out[0] = MawMotion.ROOT.clone();
        out[1] = from;
        for (int k = 0; k < ARCH; k++) {
            out[2 + k] = arch[k];
        }
        out[2 + ARCH] = GraftMesh.lerp(back, mouth, 1.0 / 3.0);
        out[3 + ARCH] = GraftMesh.lerp(back, mouth, 2.0 / 3.0);
        out[4 + ARCH] = mouth.clone();
        return out;
    }

    /** A cubic curve from {@code p0} to {@code p3}, {@code n} points along it evenly by length (the first after {@code p0}, the last {@code p3}). */
    static double[][] bezier(double[] p0, double[] p1, double[] p2, double[] p3, int n) {
        int fine = 96;
        double[][] ps = new double[fine + 1][];
        double[] at = new double[fine + 1];
        for (int i = 0; i <= fine; i++) {
            double u = i / (double)fine;
            double v = 1.0 - u;
            ps[i] = new double[3];
            for (int k = 0; k < 3; k++) {
                ps[i][k] = v * v * v * p0[k] + 3 * v * v * u * p1[k] + 3 * v * u * u * p2[k] + u * u * u * p3[k];
            }
            if (i > 0) {
                at[i] = at[i - 1] + GraftMesh.length3(GraftMesh.sub(ps[i], ps[i - 1]));
            }
        }
        double[][] out = new double[n][];
        int j = 0;
        for (int k = 1; k <= n; k++) {
            double want = at[fine] * k / n;
            while (j < fine - 1 && at[j + 1] < want) {
                j++;
            }
            double seg = Math.max(1.0E-9, at[j + 1] - at[j]);
            out[k - 1] = GraftMesh.lerp(ps[j], ps[j + 1], GraftMesh.clamp01((want - at[j]) / seg));
        }
        return out;
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
