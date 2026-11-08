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

    // ---------------------------------------------------------------- a bite

    /**
     * A bite at {@code target} (body frame), {@code age} ticks into one {@code total} long that lands at {@code land}: drawn back and up with
     * its mouth opening, thrown out at the target gaping, snapped shut on it, shaken, drawn back to rest.
     */
    public static Shape bite(double time, int seed, double[] target, double age, double total, double land) {
        double[] restHead = idleHead(time, seed);
        double[] restLook = idleLook(time, seed);
        double[] shoulder = SHOULDER;
        double[] toward = GraftMesh.normalize(GraftMesh.sub(target, shoulder));
        double[] coiled = GraftMesh.add(GraftMesh.add(restHead, new double[] {0.08, 0.22, -0.32}), GraftMesh.mul(toward, -0.1));
        double[] at = GraftMesh.sub(target, GraftMesh.mul(toward, 0.08));
        double windup = Math.max(1.0, land * 0.42);
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
            look = toward;
            mouth = age < land - 1.0 ? lerp(0.45, 1.0, GraftMesh.smooth(w * 1.4)) : lerp(1.0, 0.04, age - (land - 1.0));
        } else {
            double hold = 3.0;
            double back = Math.max(1.0, total - land - hold);
            if (age < land + hold) {
                double shake = Math.sin((age - land) * 2.6) * 0.14 * (1.0 - (age - land) / hold);
                double[] side = GraftMesh.normalize(GraftMesh.cross(toward, new double[] {0.0, 1.0, 0.0}));
                head = GraftMesh.add(at, GraftMesh.mul(side, shake));
                look = toward;
                mouth = 0.04;
            } else {
                double w = GraftMesh.smooth(GraftMesh.clamp01((age - land - hold) / back));
                head = GraftMesh.lerp(at, restHead, w);
                look = GraftMesh.normalize(GraftMesh.lerp(toward, restLook, w));
                mouth = lerp(0.04, idleMouth(time, seed), w);
            }
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
     * Its control points from the root to {@code head}, the mouth coming at it along {@code look}: straight out of the back, up behind him
     * in a zigzag of coils (a wave running up them, so it slithers), round over the right shoulder to the head. Thrown out ({@code reach}
     * 1), the coils pull most of the way straight toward the line over his shoulder to the head, as a spring does, the wave nearly still.
     */
    static double[][] path(double[] head, double[] look, double time, int seed, double reach) {
        double t = time + seed * 43.0;
        double wave = 1.0 - 0.7 * reach;
        double[] out = add(ROOT, new double[] {0.02, 0.05, -0.34});
        double neckLength = 0.6 + 0.3 * reach;
        double[] neck = GraftMesh.sub(head, GraftMesh.mul(GraftMesh.normalize(look), neckLength));
        int n = ZIGZAG.length;
        double[][] coils = new double[n][];
        for (int k = 0; k < n; k++) {
            double phase = t * 0.07 - k * 1.1;
            double side = k % 2 == 0 ? 1.0 : -1.0;
            double[] wobble = {0.22 * Math.sin(phase) * side * wave, 0.12 * Math.sin(phase * 0.8 + 1.0) * wave, 0.16 * Math.sin(phase * 0.9 + 2.0) * wave};
            double[] rest = add(ZIGZAG[k], wobble);
            // Its place on the straightened line: out of the back to over the shoulder, then on to the neck; still a little zigzag.
            double f = (k + 1.0) / (n + 1.0);
            double[] line = f < 0.5 ? GraftMesh.lerp(out, SHOULDER, f / 0.5) : GraftMesh.lerp(SHOULDER, neck, (f - 0.5) / 0.5);
            double[] across = {0.28 * side, 0.12, 0.0};
            coils[k] = GraftMesh.lerp(rest, add(line, across), 0.82 * GraftMesh.smooth(reach));
        }
        double[] curl = GraftMesh.lerp(coils[n - 1], neck, 0.5);
        curl = add(curl, new double[] {0.1 * Math.sin(t * 0.09 + 1.3) * wave, 0.08 * Math.sin(t * 0.08) * wave, 0.0});
        double[][] path = new double[n + 5][];
        path[0] = ROOT;
        path[1] = out;
        System.arraycopy(coils, 0, path, 2, n);
        path[n + 2] = curl;
        path[n + 3] = neck;
        path[n + 4] = head;
        return path;
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
