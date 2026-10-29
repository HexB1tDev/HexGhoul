package com.hex.kagune.control;

import com.hex.kagune.control.ukaku.UkakuModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Rinkaku's kakuja coming out of his body and breaking apart (5.0.0), the pure part: the timing of the burst (each centipede leg
 * tears out of his back and overshoots its length before it settles, each red tentacle erupts after it, the mask creeps over
 * his face from the lens outward) and the shatter (every leg, tentacle and the mask break into pieces that fly off his body,
 * spin, bounce once on the ground and fade). Ages are in ticks since the burst or the shatter.
 */
public final class KakujaBurst {
    /** Ticks between one leg tearing out and the next. */
    public static final double LEG_STAGGER = 6.0;
    /** Ticks a leg takes to come all the way out of his back. */
    public static final double LEG_TICKS = 16.0;
    /** Ticks between one tentacle and the next. */
    public static final double TENTACLE_STAGGER = 4.0;
    /** Ticks a tentacle takes to come all the way out. */
    public static final double TENTACLE_TICKS = 12.0;
    /** Ticks the mask takes to cover his face. */
    public static final double MASK_TICKS = 50.0;
    /** How far the mask's growing edge has to go from the lens to cover all of it (head pixels: the beak's tip is the farthest). */
    public static final double MASK_REACH = 12.5;
    /** How wide the mask's growing edge is (head pixels): quads inside it are still swelling into place. */
    public static final double MASK_EDGE = 1.8;
    /** The flesh colour of the mask's growing edge. */
    public static final int MASK_FLESH = 0xB3182A;
    /** Ticks after a shatter before the form he goes back to starts growing its tentacles. */
    public static final double REGROW_DELAY = 6.0;
    /** Ticks a regrowing tentacle takes, and between one and the next. */
    public static final double REGROW_TICKS = 8.0;
    public static final double REGROW_STAGGER = 2.0;

    /** How long a piece lives (ticks), and over how many of its last ticks it shrinks and fades. */
    public static final int SHARD_LIFE = 44;
    public static final int SHARD_FADE = 18;
    /** Pull down and air drag per tick. */
    public static final double GRAVITY = 0.05;
    public static final double DRAG = 0.96;
    /** The bounce off the ground: what is kept of the fall and of the slide. */
    public static final double BOUNCE = 0.35;
    public static final double FRICTION = 0.55;

    private KakujaBurst() {
    }

    // ---------------------------------------------------------------------------------------------------------------- burst

    /** When the {@code order}th leg starts tearing out. */
    static double legStart(int order) {
        return 1.0 + order * LEG_STAGGER;
    }

    /** How much of the {@code order}th leg is out of his back, 0..1: fast out of the wound, slowing as the last plates come free. */
    public static double legOut(double age, int order) {
        double t = (age - legStart(order)) / LEG_TICKS;
        if (t <= 0.0) {
            return 0.0;
        }
        if (t >= 1.0) {
            return 1.0;
        }
        double rest = 1.0 - t;
        return 1.0 - rest * rest * rest;
    }

    /** The leg's length over its rest length: it overshoots by a tenth as it whips out, then pulls back in. */
    public static double legStretch(double age, int order) {
        double u = (age - legStart(order) - 10.0) / 14.0;
        return u <= 0.0 || u >= 1.0 ? 1.0 : 1.0 + 0.1 * Math.sin(Math.PI * u);
    }

    /** The leg's width over its rest width: swollen with blood as it tears out, settling. */
    public static double legSwell(double age, int order) {
        double t = Math.max(0.0, age - legStart(order));
        return 1.0 + 0.35 * (1.0 - smooth(t / 26.0));
    }

    /** How wide the jaw gapes while the leg comes out (it roars out, then closes to rest). */
    public static double legJaw(double age, int order) {
        double t = age - legStart(order);
        return t <= 0.0 ? 0.0 : t < 8.0 ? t / 8.0 : 1.0 - smooth((t - 14.0) / 8.0);
    }

    /**
     * A leg tearing out: its rest {@code points} (root first) stretched about the root by {@code stretch}, cut to the first
     * {@code out} of their length (the rest is still inside him) and resampled to as many points as they had.
     */
    public static double[][] extrude(double[][] points, double out, double stretch) {
        int n = points.length;
        double[][] stretched = new double[n][];
        for (int i = 0; i < n; i++) {
            stretched[i] = new double[] {points[0][0] + (points[i][0] - points[0][0]) * stretch, points[0][1] + (points[i][1] - points[0][1]) * stretch,
                points[0][2] + (points[i][2] - points[0][2]) * stretch};
        }
        double[] along = new double[n];
        for (int i = 1; i < n; i++) {
            along[i] = along[i - 1] + distance(stretched[i - 1], stretched[i]);
        }
        double total = along[n - 1] * Math.max(0.0, Math.min(1.0, out));
        double[][] result = new double[n][];
        int segment = 1;
        for (int i = 0; i < n; i++) {
            double target = total * i / (n - 1);
            while (segment < n - 1 && along[segment] < target) {
                segment++;
            }
            double span = along[segment] - along[segment - 1];
            double f = span < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (target - along[segment - 1]) / span));
            double[] a = stretched[segment - 1];
            double[] b = stretched[segment];
            result[i] = new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
        }
        return result;
    }

    static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** True while the {@code legs} legs of a burst are still coming out or settling (their springs take over after). */
    public static boolean legsBursting(double age, int legs) {
        return age >= 0.0 && age < burstEnd(legs);
    }

    /** When the last of {@code legs} legs has settled. */
    public static double burstEnd(int legs) {
        return legStart(Math.max(0, legs - 1)) + 30.0;
    }

    /** When the {@code order}th tentacle starts coming out. */
    public static double tentacleStart(int order) {
        return 6.0 + order * TENTACLE_STAGGER;
    }

    /** How much of the {@code order}th tentacle is out, 0..1 (exponential: it erupts, then the tip lands). */
    public static float tentacleOut(double age, int order) {
        double t = (age - tentacleStart(order)) / TENTACLE_TICKS;
        if (t <= 0.0) {
            return 0.0F;
        }
        if (t >= 1.0) {
            return 1.0F;
        }
        return (float)((1.0 - Math.pow(2.0, -10.0 * t)) / (1.0 - Math.pow(2.0, -10.0)));
    }

    /** How much of a tentacle of the form he goes back to has grown out again, {@code age} ticks after the kakuja broke. */
    public static float regrowOut(double age, int order) {
        return (float)smooth((age - REGROW_DELAY - order * REGROW_STAGGER) / REGROW_TICKS);
    }

    /** True once every one of {@code limbs} tentacles has grown back. */
    public static boolean regrown(double age, int limbs) {
        return age >= REGROW_DELAY + Math.max(0, limbs - 1) * REGROW_STAGGER + REGROW_TICKS;
    }

    /** How far from the lens the mask has grown (head pixels), {@code age} ticks after the burst. */
    public static double maskFront(double age) {
        double u = Math.max(0.0, Math.min(1.0, age / MASK_TICKS));
        // Slow to take hold, quicker across the face, slow again down the beak.
        return (MASK_REACH + MASK_EDGE) * smooth(u);
    }

    /**
     * How much of a mask quad {@code distance} pixels from the lens has grown in when the edge is at {@code front}: 0 not yet
     * (not drawn), 1 fully in place.
     */
    public static double maskGrown(double distance, double front) {
        return Math.max(0.0, Math.min(1.0, (front - distance) / MASK_EDGE));
    }

    /**
     * The mask growing over his face ({@code sink} gets what is there): only what the edge, {@code front} pixels from the lens,
     * has reached is drawn; at the edge each piece swells up out of its own middle, raw red, darkening to the mask's plum as it
     * sets.
     */
    public static UkakuModel.Sink growingMask(UkakuModel.Sink sink, double[] lens, double front) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> {
            double x = (a[0] + b[0] + c[0] + d[0]) * 0.25;
            double y = (a[1] + b[1] + c[1] + d[1]) * 0.25;
            double z = (a[2] + b[2] + c[2] + d[2]) * 0.25;
            double grown = maskGrown(Math.hypot(x - lens[0], y - lens[1]), front);
            if (grown <= 0.0) {
                return;
            }
            if (grown >= 1.0) {
                sink.quad(a, b, c, d, n, ca, cb, cc, cd);
                return;
            }
            double s = 0.25 + 0.75 * grown;
            double[] middle = {x, y, z};
            sink.quad(toward(middle, a, s), toward(middle, b, s), toward(middle, c, s), toward(middle, d, s), n, flesh(ca, grown),
                flesh(cb, grown), flesh(cc, grown), flesh(cd, grown));
        };
    }

    private static double[] toward(double[] middle, double[] corner, double s) {
        return new double[] {middle[0] + (corner[0] - middle[0]) * s, middle[1] + (corner[1] - middle[1]) * s, middle[2] + (corner[2] - middle[2]) * s};
    }

    /** The mask's colour {@code argb} as it sets: raw flesh at first, its own colour once grown in. */
    static int flesh(int argb, double grown) {
        double k = grown * grown;
        int r = (int)Math.round(((MASK_FLESH >> 16) & 0xFF) + ((((argb >> 16) & 0xFF) - ((MASK_FLESH >> 16) & 0xFF)) * k));
        int g = (int)Math.round(((MASK_FLESH >> 8) & 0xFF) + ((((argb >> 8) & 0xFF) - ((MASK_FLESH >> 8) & 0xFF)) * k));
        int b = (int)Math.round((MASK_FLESH & 0xFF) + (((argb & 0xFF) - (MASK_FLESH & 0xFF)) * k));
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    // -------------------------------------------------------------------------------------------------------------- shatter

    /** One quad of the kakuja (world space): corners, the normal it was lit with, a colour per corner, glowing or lit. */
    public record Quad(double[] a, double[] b, double[] c, double[] d, double[] normal, int ca, int cb, int cc, int cd, boolean glow) {
        double[] centre() {
            return new double[] {(this.a[0] + this.b[0] + this.c[0] + this.d[0]) * 0.25, (this.a[1] + this.b[1] + this.c[1] + this.d[1]) * 0.25,
                (this.a[2] + this.b[2] + this.c[2] + this.d[2]) * 0.25};
        }
    }

    /** A broken-off piece: its quads relative to its centre, where it is, how it flies and spins. */
    public static final class Shard {
        final double[] local;
        final double[] normals;
        final int[] colours;
        final boolean[] glow;
        final double[] position;
        final double[] previous;
        final double[] velocity;
        final double[] axis;
        double spin;
        double angle;
        double previousAngle;
        int age;
        final double floor;

        Shard(List<Quad> quads, double[] centre, double[] velocity, double[] axis, double spin, double floor) {
            int n = quads.size();
            this.local = new double[n * 12];
            this.normals = new double[n * 3];
            this.colours = new int[n * 4];
            this.glow = new boolean[n];
            for (int q = 0; q < n; q++) {
                Quad quad = quads.get(q);
                double[][] corners = {quad.a(), quad.b(), quad.c(), quad.d()};
                for (int k = 0; k < 4; k++) {
                    for (int axisIndex = 0; axisIndex < 3; axisIndex++) {
                        this.local[q * 12 + k * 3 + axisIndex] = corners[k][axisIndex] - centre[axisIndex];
                    }
                }
                System.arraycopy(quad.normal(), 0, this.normals, q * 3, 3);
                this.colours[q * 4] = quad.ca();
                this.colours[q * 4 + 1] = quad.cb();
                this.colours[q * 4 + 2] = quad.cc();
                this.colours[q * 4 + 3] = quad.cd();
                this.glow[q] = quad.glow();
            }
            this.position = centre.clone();
            this.previous = centre.clone();
            this.velocity = velocity;
            this.axis = axis;
            this.spin = spin;
            this.floor = floor;
        }

        /** One tick of flight: gravity, drag, spin; one low bounce off the ground, then it slides and settles. */
        public void step() {
            System.arraycopy(this.position, 0, this.previous, 0, 3);
            this.previousAngle = this.angle;
            this.velocity[1] -= GRAVITY;
            for (int i = 0; i < 3; i++) {
                this.velocity[i] *= DRAG;
                this.position[i] += this.velocity[i];
            }
            this.angle += this.spin;
            if (this.position[1] < this.floor) {
                this.position[1] = this.floor;
                if (this.velocity[1] < 0.0) {
                    this.velocity[1] = -this.velocity[1] * BOUNCE;
                }
                this.velocity[0] *= FRICTION;
                this.velocity[2] *= FRICTION;
                this.spin *= FRICTION;
            }
            this.age++;
        }

        public boolean dead() {
            return this.age >= SHARD_LIFE;
        }

        /** 1 while it flies, shrinking to 0 over its last {@link #SHARD_FADE} ticks. */
        public double fade(double partial) {
            double left = SHARD_LIFE - (this.age + partial);
            return Math.max(0.0, Math.min(1.0, left / SHARD_FADE));
        }

        public int quads() {
            return this.glow.length;
        }

        public double[] position(double partial) {
            return new double[] {this.previous[0] + (this.position[0] - this.previous[0]) * partial,
                this.previous[1] + (this.position[1] - this.previous[1]) * partial,
                this.previous[2] + (this.position[2] - this.previous[2]) * partial};
        }

        /** Corner {@code corner} of quad {@code quad} relative to the piece's centre, turned and shrunk for this frame. */
        public double[] corner(int quad, int corner, double[] rotation, double scale) {
            int i = quad * 12 + corner * 3;
            return rotate(rotation, this.local[i], this.local[i + 1], this.local[i + 2], scale);
        }

        public double[] normal(int quad, double[] rotation) {
            int i = quad * 3;
            return rotate(rotation, this.normals[i], this.normals[i + 1], this.normals[i + 2], 1.0);
        }

        public int colour(int quad, int corner) {
            return this.colours[quad * 4 + corner];
        }

        public boolean glow(int quad) {
            return this.glow[quad];
        }

        /** This frame's rotation (a row-major 3x3 matrix about the piece's spin axis). */
        public double[] rotation(double partial) {
            return axisAngle(this.axis, this.previousAngle + (this.angle - this.previousAngle) * partial);
        }

        double[] velocity() {
            return this.velocity;
        }
    }

    /**
     * Breaks {@code quads} into pieces about {@code cell} blocks across (the quads whose centres fall in the same cell make one
     * piece) and throws them off a body whose middle is {@code body}: outward from it, up, a little at random, each spinning on
     * its own axis. {@code floor} is where they land. The same seed breaks the same way.
     */
    public static List<Shard> shatter(List<Quad> quads, double cell, double[] body, double floor, double force, long seed) {
        Map<Long, List<Quad>> cells = new HashMap<>();
        List<Long> order = new ArrayList<>();
        for (Quad quad : quads) {
            double[] c = quad.centre();
            long key = cellKey(Math.floor(c[0] / cell), Math.floor(c[1] / cell), Math.floor(c[2] / cell));
            List<Quad> members = cells.get(key);
            if (members == null) {
                members = new ArrayList<>();
                cells.put(key, members);
                order.add(key);
            }
            members.add(quad);
        }
        SplittableRandom random = new SplittableRandom(seed);
        List<Shard> shards = new ArrayList<>(order.size());
        for (long key : order) {
            List<Quad> members = cells.get(key);
            double[] centre = new double[3];
            for (Quad quad : members) {
                double[] c = quad.centre();
                for (int i = 0; i < 3; i++) {
                    centre[i] += c[i] / members.size();
                }
            }
            double dx = centre[0] - body[0];
            double dy = centre[1] - body[1];
            double dz = centre[2] - body[2];
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-4) {
                dx = random.nextDouble() - 0.5;
                dz = random.nextDouble() - 0.5;
                dy = 0.0;
                length = Math.max(1.0E-4, Math.hypot(dx, dz));
            }
            // Nearer the body, the harder the blast; far tips mostly drop where they were.
            double near = 1.0 / (1.0 + 0.35 * length);
            double speed = force * (0.08 + 0.22 * near) * (0.7 + 0.6 * random.nextDouble());
            double[] velocity = {dx / length * speed + (random.nextDouble() - 0.5) * 0.08 * force,
                dy / length * speed * 0.5 + (0.1 + 0.16 * random.nextDouble()) * force,
                dz / length * speed + (random.nextDouble() - 0.5) * 0.08 * force};
            double[] axis = unit(new double[] {random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5});
            double spin = (0.15 + 0.4 * random.nextDouble()) * (random.nextBoolean() ? 1.0 : -1.0);
            shards.add(new Shard(members, centre, velocity, axis, spin, floor));
        }
        return shards;
    }

    private static long cellKey(double x, double y, double z) {
        long ix = (long)x & 0x1FFFFF;
        long iy = (long)y & 0x1FFFFF;
        long iz = (long)z & 0x1FFFFF;
        return ix << 42 | iy << 21 | iz;
    }

    // ------------------------------------------------------------------------------------------------------------ the head

    /**
     * A point in head pixels (x to his left, y down from the neck, z backward) in the world, relative to his feet: the vanilla
     * player model's transform (body turned by {@code bodyYaw}, flipped, scaled 0.9375, raised 1.501, the head at its pivot
     * turned by {@code headYaw} relative to the body and pitched by {@code pitch}; degrees). Crouching lowers the head.
     */
    public static double[] head(double[] pixel, double bodyYaw, double headYaw, double pitch, boolean crouching) {
        double x = pixel[0] / 16.0;
        double y = pixel[1] / 16.0;
        double z = pixel[2] / 16.0;
        double a = Math.toRadians(pitch);
        double y1 = y * Math.cos(a) - z * Math.sin(a);
        double z1 = y * Math.sin(a) + z * Math.cos(a);
        double b = Math.toRadians(headYaw);
        double x2 = x * Math.cos(b) + z1 * Math.sin(b);
        double z2 = -x * Math.sin(b) + z1 * Math.cos(b);
        double qy = -1.501 + (crouching ? 4.2 / 16.0 : 0.0) + y1;
        double sx = -x2 * 0.9375;
        double sy = -qy * 0.9375;
        double sz = z2 * 0.9375;
        double t = Math.toRadians(180.0 - bodyYaw);
        return new double[] {sx * Math.cos(t) + sz * Math.sin(t), sy - (crouching ? 0.125 : 0.0), -sx * Math.sin(t) + sz * Math.cos(t)};
    }

    // ------------------------------------------------------------------------------------------------------------- helpers

    static double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }

    static double[] unit(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return length < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : new double[] {v[0] / length, v[1] / length, v[2] / length};
    }

    static double[] axisAngle(double[] axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double t = 1.0 - c;
        double x = axis[0];
        double y = axis[1];
        double z = axis[2];
        return new double[] {t * x * x + c, t * x * y - s * z, t * x * z + s * y, t * x * y + s * z, t * y * y + c, t * y * z - s * x,
            t * x * z - s * y, t * y * z + s * x, t * z * z + c};
    }

    static double[] rotate(double[] m, double x, double y, double z, double scale) {
        return new double[] {(m[0] * x + m[1] * y + m[2] * z) * scale, (m[3] * x + m[4] * y + m[5] * z) * scale,
            (m[6] * x + m[7] * y + m[8] * z) * scale};
    }
}
