package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.graft.GraftMesh;

/**
 * 3.4.9: where the Bikaku worm is, each frame (pure: the renderer, the previews and the tests pose it the same way). All in the body's own
 * frame: blocks from the feet, x to its right, y up, z ahead.
 *
 * <p>At rest it rises out of the lower back, arches up behind him and over his right shoulder, its head hanging there looking about: it
 * sways, its body undulating, its head turning in short predatory snaps from one place to the next, and now and then its mouth opens,
 * showing its teeth, and shuts. A bite pulls it back, throws it out at what it bites with its mouth gaping, snaps the mouth shut on it,
 * shakes it, and draws it back.
 */
public final class MawMotion {
    private MawMotion() {
    }

    /** Where the worm leaves the body: the lower back, a little in. */
    public static final double[] ROOT = {0.0, 0.84, -0.13};
    /**
     * 3.4.9 (the owner: "this [kagune] should be longer than all the others. It should like have zigzags and all ... make sure it's
     * long"): where its head rests, towering up over the right shoulder, and the zigzag of coils it climbs there by behind him (side to
     * side, back and up). About eight blocks of it at rest.
     */
    public static final double[] HEAD = {1.15, 2.95, 0.25};
    static final double[][] ZIGZAG = {{0.95, 1.05, -1.05}, {-0.75, 1.5, -1.75}, {1.05, 2.15, -2.25}, {-0.55, 2.85, -2.2}, {0.75, 3.45, -1.45}};
    /** Over the right shoulder: where a worm thrown out ahead passes him (never through him). */
    static final double[] SHOULDER = {0.62, 2.15, -0.25};

    /** 3.4.9: {@link #SHOULDER}, for the server (where the worm's reach is measured from) and the renderer. */
    public static double[] shoulder() {
        return SHOULDER.clone();
    }

    /** The worm this frame: its control points (root first, the mouth last), the way its back faces at the root, how wide its mouth is. */
    public record Shape(double[][] path, double[] dorsal, double aperture) {
    }

    // ---------------------------------------------------------------- at rest

    /** At rest, {@code time} ticks into the world; {@code seed} gives each worm its own rhythm. */
    public static Shape idle(double time, int seed) {
        double[] head = idleHead(time, seed);
        double[] look = idleLook(time, seed);
        return new Shape(path(head, look, time, seed, 0.0), dorsal(), idleMouth(time, seed));
    }

    /** Its head's place at rest: towering over the right shoulder, swaying. */
    static double[] idleHead(double time, int seed) {
        double t = time + seed * 97.0;
        return new double[] {HEAD[0] + 0.24 * Math.sin(t * 0.031) + 0.07 * Math.sin(t * 0.083 + 1.0),
            HEAD[1] + 0.16 * Math.sin(t * 0.047 + 0.5) + 0.05 * Math.sin(t * 0.13),
            HEAD[2] + 0.2 * Math.sin(t * 0.027 + 2.0)};
    }

    /**
     * The way its head looks at rest: ahead and about, held on one spot and then snapping to the next (a predator scanning), a quiver on
     * top. Unit, in the body's frame.
     */
    static double[] idleLook(double time, int seed) {
        double t = time + seed * 61.0;
        double hold = 46.0 + 14.0 * GraftMesh.hash(seed * 7);
        int beat = (int)Math.floor(t / hold);
        double into = (t - beat * hold) / 7.0;
        double w = GraftMesh.smooth(GraftMesh.clamp01(into));
        double yaw = lerp(scanYaw(seed, beat - 1), scanYaw(seed, beat), w) + 0.04 * Math.sin(t * 0.9);
        double pitch = lerp(scanPitch(seed, beat - 1), scanPitch(seed, beat), w) + 0.03 * Math.sin(t * 1.1 + 2.0);
        return new double[] {-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
    }

    /** Where it looks on the {@code beat}th spot: anywhere from well to the left of ahead to well round to the right. */
    private static double scanYaw(int seed, int beat) {
        return -0.35 + 1.25 * (GraftMesh.hash(seed * 131 + beat * 17) - 0.3);
    }

    private static double scanPitch(int seed, int beat) {
        return -0.35 + 0.45 * GraftMesh.hash(seed * 53 + beat * 29);
    }

    /**
     * How wide its mouth is at rest: nearly shut, breathing; now and then it gapes, showing its teeth (open over half a second, held,
     * shut), and in between it tastes the air (a little open).
     */
    public static double idleMouth(double time, int seed) {
        double t = time + seed * 211.0;
        double period = 150.0 + 60.0 * GraftMesh.hash(seed * 19);
        double phase = t - Math.floor(t / period) * period;
        double rest = 0.05 + 0.03 * Math.sin(t * 0.2);
        double gape = phase < 12.0 ? GraftMesh.smooth(phase / 12.0) : phase < 26.0 ? 1.0 : phase < 38.0 ? 1.0 - GraftMesh.smooth((phase - 26.0) / 12.0) : 0.0;
        double taste = phase > 80.0 && phase < 96.0 ? Math.sin(Math.PI * (phase - 80.0) / 16.0) : 0.0;
        return Math.max(rest, Math.max(0.78 * gape + 0.06 * gape * Math.sin(t * 1.3), 0.3 * taste));
    }

    // ---------------------------------------------------------------- 3.4.9: this frame, from what the server says

    /**
     * The worm this frame for a player whose worm is making {@code bite} ({@code age} ticks into it, at {@code target} in the body's frame,
     * held to its reach), or at rest when it makes none or the bite is over. The renderer and the previews both pose it here.
     */
    public static Shape of(double time, int seed, BikakuRules.Bite bite, double age, double[] target) {
        if (bite == null || bite == BikakuRules.Bite.NONE || target == null || !(age >= 0.0) || age >= bite.ticks()) {
            return idle(time, seed);
        }
        return bite(time, seed, BikakuRules.reach(target), age, bite.ticks(), bite.land(), bite.style());
    }

    /** First person: how far the near part of the worm is kept to the right and up (blocks, at the eyes), and how far out that fades. */
    static final double[] FIRST_PERSON_ASIDE = {0.8, 0.35, -0.1};
    static final double FIRST_PERSON_FADE = 3.4;
    /** First person: the worm's line kept this far from the eyes. */
    public static final double FIRST_PERSON_CLEAR = 1.25;

    /**
     * 3.4.9, his own first person (the body's frame, his eyes at {@code eye}): the worm's points near him moved aside to the right and up,
     * fading out over a few blocks, then kept {@link #FIRST_PERSON_CLEAR} from the eyes, so a bite comes in past the top right of his view
     * and never covers what it bites. What it bites, further out, is where it is. Only he sees it so; everyone else sees it as it is.
     */
    public static double[][] firstPerson(double[][] points, double[] eye) {
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double d = GraftMesh.length3(GraftMesh.sub(points[i], eye));
            double fade = GraftMesh.smooth(GraftMesh.clamp01(1.0 - d / FIRST_PERSON_FADE));
            out[i] = GraftMesh.add(points[i], GraftMesh.mul(FIRST_PERSON_ASIDE, fade));
        }
        return clearOfEyes(out, eye, FIRST_PERSON_CLEAR);
    }

    /**
     * 3.4.9, first person: the worm's points (camera-relative, the eyes at {@code eye}) kept at least {@code radius} from the eyes (pushed
     * straight out), so nothing of it comes into the camera; what passes him still shows, round him.
     */
    public static double[][] clearOfEyes(double[][] points, double[] eye, double radius) {
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double[] from = GraftMesh.sub(points[i], eye);
            double d = GraftMesh.length3(from);
            out[i] = d >= radius ? points[i] : GraftMesh.add(eye, GraftMesh.mul(d < 1.0E-6 ? new double[] {0.0, -1.0, 0.0} : GraftMesh.mul(from, 1.0 / d), radius));
        }
        return out;
    }

    /**
     * 3.4.9: the worm while it grows out of his back ({@code grown} 0 .. 1): its points cut where that share of its length (along the line
     * through them) ends, so its first part shows, mouth first, and at 1 it is exactly the whole worm.
     */
    public static double[][] grown(double[][] points, double grown) {
        if (grown >= 1.0 || points.length < 2) {
            return points;
        }
        double total = 0.0;
        for (int i = 1; i < points.length; i++) {
            total += GraftMesh.length3(GraftMesh.sub(points[i], points[i - 1]));
        }
        double want = Math.max(0.15, total * Math.max(0.0, grown));
        java.util.List<double[]> kept = new java.util.ArrayList<>();
        kept.add(points[0]);
        double along = 0.0;
        for (int i = 1; i < points.length; i++) {
            double step = GraftMesh.length3(GraftMesh.sub(points[i], points[i - 1]));
            if (along + step >= want) {
                double f = step < 1.0E-9 ? 1.0 : (want - along) / step;
                kept.add(GraftMesh.lerp(points[i - 1], points[i], GraftMesh.clamp01(f)));
                break;
            }
            kept.add(points[i]);
            along += step;
        }
        return kept.toArray(new double[0][]);
    }

    // ---------------------------------------------------------------- a bite

    /** A bite of style 0 (the M1 string's first, low over the shoulder): {@link #bite(double, int, double[], double, double, double, int)}. */
    public static Shape bite(double time, int seed, double[] target, double age, double total, double land) {
        return bite(time, seed, target, age, total, land, 0);
    }

    /**
     * A bite at {@code target} (body frame), {@code age} ticks into one {@code total} long that lands at {@code land}: drawn back with its
     * mouth opening, thrown out at the target gaping, snapped shut on it, shaken, drawn back to rest. 3.4.9: {@code style} is the way the
     * M1 string's bites come: 0 drawn back a little and thrown low over the shoulder, 1 reared up high and brought down on it, 2 drawn
     * far back and thrown in a wide hook from the side, held and shaken harder.
     */
    public static Shape bite(double time, int seed, double[] target, double age, double total, double land, int style) {
        double[] restHead = idleHead(time, seed);
        double[] restLook = idleLook(time, seed);
        double[] toward = GraftMesh.normalize(GraftMesh.sub(target, SHOULDER));
        // His right, square to the way it goes (the heavy bite hooks out that way, round the outside, never across his face).
        double[] side = GraftMesh.cross(new double[] {0.0, 1.0, 0.0}, toward);
        side = GraftMesh.dot(side, side) < 1.0E-9 ? new double[] {1.0, 0.0, 0.0} : GraftMesh.normalize(side);
        double[] back = style == 1 ? new double[] {-0.1, 0.8, -0.5} : style == 2 ? new double[] {0.3, 0.45, -0.85} : new double[] {0.08, 0.22, -0.32};
        double pull = style == 2 ? 0.4 : style == 1 ? 0.2 : 0.1;
        double[] coiled = GraftMesh.add(GraftMesh.add(restHead, back), GraftMesh.mul(toward, -pull));
        double[] at = GraftMesh.sub(target, GraftMesh.mul(toward, 0.08));
        double windup = Math.max(1.0, land * 0.42);
        double hold = style == 2 ? 6.0 : 3.0;
        double shakeBy = style == 2 ? 0.24 : 0.14;
        double[] head;
        double[] look;
        double mouth;
        if (age < windup) {
            double w = GraftMesh.smooth(age / windup);
            head = GraftMesh.lerp(restHead, coiled, w);
            look = GraftMesh.normalize(GraftMesh.lerp(restLook, toward, w));
            mouth = lerp(idleMouth(time, seed), 0.45, w);
        } else if (age < land) {
            double w = (age - windup) / Math.max(1.0E-6, land - windup);
            double fast = w * w * (3.0 - 2.0 * w);
            head = GraftMesh.lerp(coiled, at, fast);
            // The way it comes: over the top (1), in a hook from the side (2); straight (0).
            double arc = Math.sin(Math.PI * fast);
            if (style == 1) {
                head = GraftMesh.add(head, new double[] {0.0, 0.7 * arc, 0.0});
            } else if (style == 2) {
                head = GraftMesh.add(head, GraftMesh.add(GraftMesh.mul(side, 0.8 * arc), new double[] {0.0, 0.25 * arc, 0.0}));
            }
            look = style == 0 ? toward : GraftMesh.normalize(GraftMesh.sub(at, GraftMesh.lerp(coiled, at, Math.max(0.0, fast - 0.25))));
            if (GraftMesh.dot(look, look) < 1.0E-9 || !Double.isFinite(look[0])) {
                look = toward;
            }
            look = GraftMesh.normalize(GraftMesh.lerp(look, toward, w));
            mouth = age < land - 1.0 ? lerp(0.45, 1.0, GraftMesh.smooth(w * 1.4)) : lerp(1.0, 0.04, age - (land - 1.0));
        } else {
            double after = Math.max(1.0, total - land - hold);
            if (age < land + hold) {
                double shake = Math.sin((age - land) * 2.6) * shakeBy * (1.0 - (age - land) / hold);
                head = GraftMesh.add(at, GraftMesh.mul(side, shake));
                look = GraftMesh.normalize(GraftMesh.add(toward, GraftMesh.mul(side, shake * 1.5)));
                mouth = 0.04;
            } else {
                double w = GraftMesh.smooth(GraftMesh.clamp01((age - land - hold) / after));
                head = GraftMesh.lerp(at, restHead, w);
                look = GraftMesh.normalize(GraftMesh.lerp(toward, restLook, w));
                mouth = lerp(0.04, idleMouth(time, seed), w);
            }
        }
        if (age < land - 1.0 || age > land + hold) {
            head = clear(head); // in flight, never through his own head or body (on its target, it is where its target is)
        }
        double reach = GraftMesh.smooth(GraftMesh.clamp01(GraftMesh.length3(GraftMesh.sub(head, restHead)) / 2.0));
        return new Shape(path(head, look, time, seed, reach), dorsal(), GraftMesh.clamp01(mouth));
    }

    // ---------------------------------------------------------------- its body between root and head

    /** Its back faces up out of his back (its spurs up, its belly down). */
    static double[] dorsal() {
        return new double[] {0.0, 1.0, 0.0};
    }

    /**
     * 3.4.9: how far along the worm each coil is from its head at rest (blocks, along the line through its points): a bite uncoils the
     * worm from the head back, only as far as it needs to reach.
     */
    static final double[] ARC = arcs();

    private static double[] arcs() {
        double[] neck = {HEAD[0], HEAD[1], HEAD[2] - 0.6};
        double[] curl = GraftMesh.lerp(ZIGZAG[ZIGZAG.length - 1], neck, 0.5);
        double[] out = new double[ZIGZAG.length];
        double along = 0.6 + GraftMesh.length3(GraftMesh.sub(curl, neck));
        double[] before = curl;
        for (int k = ZIGZAG.length - 1; k >= 0; k--) {
            along += GraftMesh.length3(GraftMesh.sub(ZIGZAG[k], before));
            out[k] = along;
            before = ZIGZAG[k];
        }
        return out;
    }

    /**
     * Its control points from the root to {@code head}, the mouth coming at it along {@code look}: straight out of the back, up behind him
     * in a zigzag of coils (a wave running up them, so it slithers), round over the right shoulder to the head. Thrown out ({@code reach}
     * 1), 3.4.9 (the owner: "make sure it's long"): it uncoils from the head back only as far as the bite needs; the coils it needs go out
     * over his shoulder along the way to the head, spread out and still swinging from side to side, and the rest stay coiled up behind him,
     * drawn in a little (they gave the front its length). Never into his own head or body.
     */
    static double[][] path(double[] head, double[] look, double time, int seed, double reach) {
        double t = time + seed * 43.0;
        double wave = 1.0 - 0.6 * reach;
        double[] out = add(ROOT, new double[] {0.02, 0.05, -0.34});
        double neckLength = 0.6 + 0.3 * reach;
        double[] neck = clear(GraftMesh.sub(head, GraftMesh.mul(GraftMesh.normalize(look), neckLength)));
        int n = ZIGZAG.length;
        double straighten = GraftMesh.smooth(reach);
        double need = GraftMesh.length3(GraftMesh.sub(neck, SHOULDER)) + 0.4;
        double[] way = GraftMesh.sub(neck, SHOULDER);
        way = GraftMesh.dot(way, way) < 1.0E-9 ? new double[] {0.0, 0.0, 1.0} : GraftMesh.normalize(way);
        double[] sideways = GraftMesh.cross(way, new double[] {0.0, 1.0, 0.0});
        sideways = GraftMesh.dot(sideways, sideways) < 0.04 ? new double[] {1.0, 0.0, 0.0} : GraftMesh.normalize(sideways);
        double[][] coils = new double[n][];
        for (int k = 0; k < n; k++) {
            double phase = t * 0.07 - k * 1.1;
            double sign = k % 2 == 0 ? 1.0 : -1.0;
            double[] wobble = {0.22 * Math.sin(phase) * sign * wave, 0.12 * Math.sin(phase * 0.8 + 1.0) * wave, 0.16 * Math.sin(phase * 0.9 + 2.0) * wave};
            double[] rest = add(ZIGZAG[k], wobble);
            if (straighten <= 1.0E-6) {
                coils[k] = rest;
                continue;
            }
            // Uncoiled if the bite needs it (from the head back), placed along the way over the shoulder by how far it is from the head.
            double w = GraftMesh.smooth(GraftMesh.clamp01((need + 0.9 - ARC[k]) / 1.8)) * straighten;
            double f = Math.max(0.06, Math.min(0.94, 1.0 - ARC[k] / need));
            double taper = Math.sqrt(Math.sin(Math.PI * f));
            double[] on = GraftMesh.add(GraftMesh.lerp(SHOULDER, neck, f), GraftMesh.add(GraftMesh.mul(sideways, 0.32 * taper * sign),
                new double[] {0.0, -0.1 * taper * sign, 0.0}));
            double[] stay = GraftMesh.lerp(rest, add(rest, new double[] {0.0, -0.3, 0.45}), 0.5 * straighten);
            coils[k] = clear(GraftMesh.lerp(stay, on, w));
        }
        double[] curl = GraftMesh.lerp(coils[n - 1], neck, 0.5);
        curl = clear(add(curl, new double[] {0.1 * Math.sin(t * 0.09 + 1.3) * wave, 0.08 * Math.sin(t * 0.08) * wave, 0.0}));
        double[][] path = new double[n + 5][];
        path[0] = ROOT;
        path[1] = out;
        System.arraycopy(coils, 0, path, 2, n);
        path[n + 2] = curl;
        path[n + 3] = neck;
        path[n + 4] = head;
        return path;
    }

    /** Where his head is (its middle) and how far the worm's middle keeps from it; his torso's half-width with the worm's thickness. */
    static final double[] HEAD_CENTRE = {0.0, 1.72, 0.0};
    static final double HEAD_CLEAR = 0.62;
    static final double TORSO_HALF = 0.72;
    static final double TORSO_BACK = -0.4;
    static final double TORSO_FRONT = 0.4;

    /** 3.4.9: {@code p} kept out of his head (pushed out from its middle) and his torso (pushed out behind him, or in front). */
    static double[] clear(double[] p) {
        double[] out = p;
        double[] from = GraftMesh.sub(out, HEAD_CENTRE);
        double d = GraftMesh.length3(from);
        if (d < HEAD_CLEAR) {
            double[] way = d < 1.0E-6 ? new double[] {0.0, 0.0, -1.0} : GraftMesh.mul(from, 1.0 / d);
            out = GraftMesh.add(HEAD_CENTRE, GraftMesh.mul(way, HEAD_CLEAR));
        }
        if (out[1] > 0.6 && out[1] < 1.55 && Math.abs(out[0]) < TORSO_HALF && out[2] > TORSO_BACK && out[2] < TORSO_FRONT) {
            out = new double[] {out[0], out[1], out[2] < 0.0 ? TORSO_BACK : TORSO_FRONT};
        }
        return out;
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
