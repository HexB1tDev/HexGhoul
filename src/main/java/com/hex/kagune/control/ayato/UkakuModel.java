package com.hex.kagune.control.ayato;

/**
 * Ayato's Ukaku kagune as real 3D geometry (pure, tested), matched to anime and manga frames. This class is the entry
 * point and the shared toolkit; each state of the kagune is its own part:
 * <ul>
 *   <li>{@link UkakuFlame}: the gaseous wings (at rest): RC flame pouring out of the shoulder blades.</li>
 *   <li>{@link UkakuCrystal}: the crystallized wings (to strike or fire): hard, sharp RC crystal, folding into blades
 *       to cut and firing shards from the feather tips.</li>
 *   <li>{@link UkakuOneWing}: the :re One-Winged Kagune, a single giant crystal blade.</li>
 * </ul>
 * A twin wing crystallizes from the root up: {@link #wing} hands the crystal and the fire a front (how far up each tongue
 * the crystal has climbed); the crystal is drawn below it, the fire above it.
 *
 * <p>Geometry is built in a wing-local frame (u outward from the spine, v up, w toward the dorsal side, in units of the
 * wing's length) and placed by a {@link Frame}. Glowing parts report an upward normal so Minecraft's entity lighting
 * leaves them at full brightness ({@link #glowQuad}); solid crystal reports its true normals so it shades ({@link #facet}).
 */
public final class UkakuModel {
    /** Receives one quad: four corners (triangles repeat a corner), a normal and four ARGB colours. */
    public interface Sink {
        void quad(double[] a, double[] b, double[] c, double[] d, double[] normal, int argbA, int argbB, int argbC, int argbD);
    }

    /**
     * Wing-local frame placed in player space: origin and unit axes (outward, up, dorsal), scaled by the wing length, and
     * optionally bent ({@link Bend}): a wing that aims curls forward along its length instead of turning stiffly.
     */
    public record Frame(double[] origin, double[] out, double[] up, double[] back, double scale, Bend bend) {
        public Frame(double[] origin, double[] out, double[] up, double[] back, double scale) {
            this(origin, out, up, back, scale, null);
        }

        double[] at(double u, double v, double w) {
            if (this.bend != null) {
                return this.bend.place(this, u, v, w);
            }
            return new double[] {
                this.origin[0] + (this.out[0] * u + this.up[0] * v + this.back[0] * w) * this.scale,
                this.origin[1] + (this.out[1] * u + this.up[1] * v + this.back[1] * w) * this.scale,
                this.origin[2] + (this.out[2] * u + this.up[2] * v + this.back[2] * w) * this.scale};
        }
    }

    /**
     * A wing bent like a limb: its fan first folds toward its centre line ({@code fold} 0..1, or spreads from it below 0;
     * about the wing-local angle {@code centre}, radians from outward toward up), then everything beyond {@code start} blocks along {@code axis} from
     * the root curls toward {@code toward} on a circular arc, through {@code angle} radians over the next
     * {@code length - start} blocks (a beam bend: the centre line follows the arc and every point keeps its offset from it,
     * so the tongues curve with it instead of shearing).
     */
    public record Bend(double fold, double centre, double[] axis, double[] toward, double start, double angle, double length) {
        double[] place(Frame frame, double u, double v, double w) {
            double radius = Math.sqrt(u * u + v * v);
            double polar = Math.atan2(v, u);
            double folded = this.centre + (polar - this.centre) * (1.0 - this.fold);
            double fu = radius * Math.cos(folded);
            double fv = radius * Math.sin(folded);
            double fw = w * (1.0 - 0.5 * this.fold);
            double[] p = {
                (frame.out()[0] * fu + frame.up()[0] * fv + frame.back()[0] * fw) * frame.scale(),
                (frame.out()[1] * fu + frame.up()[1] * fv + frame.back()[1] * fw) * frame.scale(),
                (frame.out()[2] * fu + frame.up()[2] * fv + frame.back()[2] * fw) * frame.scale()};
            double[] a = this.axis;
            double[] b = this.toward;
            double s = p[0] * a[0] + p[1] * a[1] + p[2] * a[2];
            double[] o = frame.origin();
            if (s <= this.start || Math.abs(this.angle) < 1.0E-4) {
                return new double[] {o[0] + p[0], o[1] + p[1], o[2] + p[2]};
            }
            double[] c = {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
            double[] q = {p[0] - s * a[0], p[1] - s * a[1], p[2] - s * a[2]};
            double qb = q[0] * b[0] + q[1] * b[1] + q[2] * b[2];
            double qc = q[0] * c[0] + q[1] * c[1] + q[2] * c[2];
            double curvature = this.angle / Math.max(0.05, this.length - this.start);
            double theta = curvature * (s - this.start);
            double along = this.start + Math.sin(theta) / curvature;
            double across = (1.0 - Math.cos(theta)) / curvature;
            double[] n = {-Math.sin(theta) * a[0] + Math.cos(theta) * b[0], -Math.sin(theta) * a[1] + Math.cos(theta) * b[1],
                -Math.sin(theta) * a[2] + Math.cos(theta) * b[2]};
            return new double[] {
                o[0] + along * a[0] + across * b[0] + qb * n[0] + qc * c[0],
                o[1] + along * a[1] + across * b[1] + qb * n[1] + qc * c[1],
                o[2] + along * a[2] + across * b[2] + qb * n[2] + qc * c[2]};
        }
    }

    /**
     * What a wing is doing this frame.
     *
     * @param time       seconds, for flicker and shimmer
     * @param crystal    0 gaseous .. 1 crystallized
     * @param deploy     0 gone .. 1 fully out; with {@code dissolving} it is breaking down back into the shoulder blade
     * @param reEra      the :re wings (larger and more controlled)
     * @param blade      0..1 the wing folded into one long cutting blade
     * @param recoil     0..1 a shard wave just left the feather tips
     * @param detail     0..1 level of detail (below 0.6: distant or first person, far fewer quads)
     * @param alpha      multiplies all opacity
     * @param seed       varies the flicker between the two wings
     * @param side       +1 the right wing, -1 the left wing
     * @param volley     0..1 the wings flared to fire a Shard Volley
     * @param kick       0..1 a kick or burst thrusting the gas wings
     * @param mastery    the host's mastery (the look matures with it: Season 1 fire, then the √A and :re starry flame)
     * @param eye        the camera in player-local blocks (x right, y up from the feet, z forward), or null if unknown:
     *                   glowing volumes are shaded per vertex by how squarely they face it
     */
    public record Params(double time, float crystal, float deploy, boolean dissolving, boolean reEra, float blade,
                         float recoil, float detail, float alpha, int seed, int side, float volley, float kick, int mastery, double[] eye) {
        boolean fine() {
            return this.detail >= 0.6F;
        }
    }

    private UkakuModel() {
    }

    /**
     * One wing of the twin pair (or the Rabbit Guise's single wing). Crystallizing sweeps up the wing: {@code crystal} is
     * how far the crystal has climbed each tongue from the shoulder blade (0 none .. 1 to the very tip); the crystal is drawn
     * up to that front and the fire only beyond it, both at full strength, with a bright seam where the one turns into the
     * other.
     */
    public static void wing(Sink sink, Frame frame, Params p) {
        float deploy = clamp01(p.deploy());
        if (deploy <= 0.001F || p.alpha() <= 0.003F) {
            return;
        }
        double front = clamp01(p.crystal());
        if (front < UkakuFlame.GONE) {
            UkakuFlame.wing(sink, frame, p, 1.0, front);
        }
        if (front > 0.002) {
            UkakuCrystal.wing(sink, frame, p, 1.0, front);
        }
    }

    /** The :re One-Winged Kagune. */
    public static void oneWing(Sink sink, Frame frame, Params p) {
        UkakuOneWing.wing(sink, frame, p);
    }

    /** Number of crystallized primary feathers per wing (where Shard Volley shards leave from). */
    public static int primaries(boolean reEra) {
        return UkakuCrystal.primaries(reEra);
    }

    /** Wing-local point (u, v, w) on primary feather {@code primary} at {@code along} (0 root .. 1 tip), in the volley pose. */
    public static double[] launchPoint(boolean reEra, int primary, double along) {
        return UkakuCrystal.launchPoint(reEra, primary, along);
    }

    // ------------------------------------------------------------------ primitives

    /** A shaded crystal facet: true normal, oriented away from {@code centre} (the spine), emitted once (unculled). */
    static void facet(Sink sink, Frame frame, double[] a, double[] b, double[] c, double[] d, double[] centre, int ca, int cb, int cc,
                              int cd) {
        double[] pa = frame.at(a[0], a[1], a[2]);
        double[] pb = frame.at(b[0], b[1], b[2]);
        double[] pc = frame.at(c[0], c[1], c[2]);
        double[] pd = frame.at(d[0], d[1], d[2]);
        double[] e1 = {pc[0] - pa[0], pc[1] - pa[1], pc[2] - pa[2]};
        double[] e2 = {pd[0] - pb[0], pd[1] - pb[1], pd[2] - pb[2]};
        double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
        double length = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (length < 1.0E-12) {
            return;
        }
        n[0] /= length;
        n[1] /= length;
        n[2] /= length;
        double[] pcentre = frame.at(centre[0], centre[1], centre[2]);
        double mx = (pa[0] + pb[0] + pc[0] + pd[0]) / 4.0 - pcentre[0];
        double my = (pa[1] + pb[1] + pc[1] + pd[1]) / 4.0 - pcentre[1];
        double mz = (pa[2] + pb[2] + pc[2] + pd[2]) / 4.0 - pcentre[2];
        if (mx * n[0] + my * n[1] + mz * n[2] < 0.0) {
            n[0] = -n[0];
            n[1] = -n[1];
            n[2] = -n[2];
        }
        sink.quad(pa, pb, pc, pd, n, ca, cb, cc, cd);
    }

    /** A glowing quad: lit from straight above so the entity lighting leaves it at full brightness. */
    static void glowQuad(Sink sink, Frame frame, double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
        sink.quad(frame.at(a[0], a[1], a[2]), frame.at(b[0], b[1], b[2]), frame.at(c[0], c[1], c[2]), frame.at(d[0], d[1], d[2]),
            new double[] {0.0, 1.0, 0.0}, ca, cb, cc, cd);
    }

    /** A soft round disc of gas in one of the three local planes (0: u-v, 1: v-w, 2: u-w): hot centre, clear rim. */
    static void disc(Sink sink, Frame frame, double cx, double cy, double cz, double radius, int plane, double strength) {
        int segments = 12;
        int core = argb(UkakuFeathers.PUFF_CORE, strength);
        int rim = argb(UkakuFeathers.PUFF_EDGE, 0.0);
        int mid = argb(mixInt(UkakuFeathers.PUFF_CORE, UkakuFeathers.PUFF_EDGE, 0.6), strength * 0.75);
        double[] centre = {cx, cy, cz};
        for (int s = 0; s < segments; s++) {
            double a0 = 2.0 * Math.PI * s / segments;
            double a1 = 2.0 * Math.PI * (s + 1) / segments;
            double[] m0 = ring(cx, cy, cz, radius * 0.55, a0, plane);
            double[] m1 = ring(cx, cy, cz, radius * 0.55, a1, plane);
            double[] r0 = ring(cx, cy, cz, radius, a0, plane);
            double[] r1 = ring(cx, cy, cz, radius, a1, plane);
            glowQuad(sink, frame, centre, m0, m1, m1, core, mid, mid, mid);
            glowQuad(sink, frame, m0, r0, r1, m1, mid, rim, rim, mid);
        }
    }

    static double[] ring(double cx, double cy, double cz, double radius, double angle, int plane) {
        double c = Math.cos(angle) * radius;
        double s = Math.sin(angle) * radius;
        return switch (plane) {
            case 0 -> new double[] {cx + c, cy + s, cz};
            case 1 -> new double[] {cx, cy + c, cz + s};
            default -> new double[] {cx + c, cy, cz + s};
        };
    }

    /** A four-point star flash in two crossed planes. */
    static void star(Sink sink, Frame frame, double cx, double cy, double cz, double size, double strength) {
        int core = argb(UkakuFeathers.FLASH, Math.min(1.0, strength));
        int point = argb(UkakuFeathers.RIM_TIP, 0.0);
        for (int plane = 0; plane < 2; plane++) {
            for (int arm = 0; arm < 4; arm++) {
                double angle = Math.PI / 2.0 * arm;
                double[] tip = starPoint(cx, cy, cz, size * (arm % 2 == 0 ? 1.0 : 0.7), angle, plane);
                double[] left = starPoint(cx, cy, cz, size * 0.18, angle + Math.PI / 4.0, plane);
                double[] right = starPoint(cx, cy, cz, size * 0.18, angle - Math.PI / 4.0, plane);
                glowQuad(sink, frame, new double[] {cx, cy, cz}, right, tip, left, core, core, point, core);
            }
        }
    }

    static double[] starPoint(double cx, double cy, double cz, double r, double angle, int plane) {
        return plane == 0 ? new double[] {cx + Math.cos(angle) * r, cy + Math.sin(angle) * r, cz}
            : new double[] {cx, cy + Math.sin(angle) * r, cz + Math.cos(angle) * r};
    }

    /** {@code edge} pushed away from {@code spine} within the feather's plane by {@code amount}. */
    static double[] outward(double[] edge, double[] spine, double amount) {
        double dx = edge[0] - spine[0];
        double dy = edge[1] - spine[1];
        double l = Math.sqrt(dx * dx + dy * dy);
        if (l < 1.0E-9) {
            return edge.clone();
        }
        return new double[] {edge[0] + dx / l * amount, edge[1] + dy / l * amount, edge[2]};
    }


    // ------------------------------------------------------------------ helpers

    static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double)(1L << 53);
    }

    static int argb(int rgb, double alpha) {
        int a = (int)Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0);
        return a << 24 | (rgb & 0xFFFFFF);
    }

    static int mix(int from, int to, double amount) {
        return mixInt(from, to, amount);
    }

    static int mixInt(int from, int to, double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        int r = (int)Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int)Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int)Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return r << 16 | g << 8 | b;
    }

    static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    static double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }

    static float clamp01(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }

    static double clamp01(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }
}
