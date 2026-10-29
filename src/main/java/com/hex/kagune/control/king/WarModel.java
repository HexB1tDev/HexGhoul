package com.hex.kagune.control.king;

import static com.hex.kagune.control.furuta.FurutaMesh.add;
import static com.hex.kagune.control.furuta.FurutaMesh.argb;
import static com.hex.kagune.control.furuta.FurutaMesh.clamp01;
import static com.hex.kagune.control.furuta.FurutaMesh.cross;
import static com.hex.kagune.control.furuta.FurutaMesh.dot;
import static com.hex.kagune.control.furuta.FurutaMesh.hash;
import static com.hex.kagune.control.furuta.FurutaMesh.lerp;
import static com.hex.kagune.control.furuta.FurutaMesh.mix;
import static com.hex.kagune.control.furuta.FurutaMesh.mul;
import static com.hex.kagune.control.furuta.FurutaMesh.noise;
import static com.hex.kagune.control.furuta.FurutaMesh.normalize;
import static com.hex.kagune.control.furuta.FurutaMesh.perpendicular;
import static com.hex.kagune.control.furuta.FurutaMesh.sub;

import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.ayato.UkakuModel;
import com.hex.kagune.control.furuta.FurutaMesh;
import com.hex.kagune.control.kakugan.KakuganEyes;
import java.util.ArrayList;
import java.util.List;

/**
 * War, the One-Eyed King (pure; the final evolution's evil path): Kaneki's last kakuja as the mod draws every kagune, smooth
 * living shapes in black and crimson, after the War sheet (tools/kakuja/reference/war.webp), measured on it with a pixel grid.
 *
 * <p>Built in body pixels (x to the player's left, y up from the feet, z backward: the player stands in x -4..4, y 0..32,
 * z -2..2, the head in y 24..32) and handed to the sink in the pixels of the body part it rides on (x left, y down, z back, from
 * that part's pivot), so a layer on the player model can draw each part on its model part through every pose.
 * <ul>
 *   <li>Head: a black mask over the face and crown, a crown of curved shards swept back, the pale ring on the forehead, one white
 *       eye (the left), red cracks; the chin, the mouth and the back of the head stay the player's own.</li>
 *   <li>Right arm: the kakuja that swallows it, a spiked pauldron over a fan of long curved plates with glowing crimson seams that
 *       reaches 13 pixels out, and the scythe: a curved blade with a steel edge facing forward and in, a glowing groove, a
 *       serrated crimson back, its tip on the ground in front of the feet.</li>
 *   <li>Back: six rinkaku from between the shoulder blades (the top pair over the head, the middle pair to the hips, the lower
 *       pair to the knees), living tubes that writhe, black above and glowing crimson beneath; and the cape, a spiked mantle over
 *       two layers of torn strips to the ankles (1.0.17: pure black, and real cloth: {@link WarCloth}).</li>
 *   <li>Torso: the high collar, the coat with its V neck (the upper chest the player's own), plates, the diagonal sash, the belt
 *       (1.0.17: all he wears pure black; the coat's skirt is {@link WarCloth}'s).</li>
 *   <li>Left arm: a pauldron and a ragged sleeve; the forearm and the hand stay bare. Legs: wraps, the knees bare, boots.</li>
 * </ul>
 * {@code time} (ticks) moves what lives (the rinkaku writhe, the cape sways); {@code grow} 0..1 lets it out of the body.
 */
public final class WarModel {
    public static final int BLACK = 0xFF1A1819;
    public static final int BLACK2 = 0xFF242122;
    public static final int CHAR = 0xFF373233;
    public static final int CHAR_L = 0xFF575152;
    public static final int GREY = 0xFF7A7475;
    public static final int STEEL = 0xFF9A9496;
    public static final int STEEL_L = 0xFFC9C3C3;
    public static final int MAROON = 0xFF3B0A10;
    public static final int CRIMSON = 0xFF66101B;
    public static final int RED = 0xFF9E1220;
    public static final int BLOOD = 0xFFEC1A30;
    public static final int BONE = 0xFFEFE9E2;
    public static final int PUPIL = 0xFF0C0A0B;

    /** The body parts War rides on, with their pivots in body pixels. */
    public enum Part {
        HEAD(0, 24),
        TORSO(0, 24),
        RIGHT_ARM(-5, 22),
        LEFT_ARM(5, 22),
        RIGHT_LEG(-1.9, 12),
        LEFT_LEG(1.9, 12);

        final double px;
        final double py;

        Part(double px, double py) {
            this.px = px;
            this.py = py;
        }

        /** A body-pixel point in this part's pixels. */
        public double[] local(double[] body) {
            return new double[] {body[0] - this.px, this.py - body[1], body[2]};
        }
    }

    private WarModel() {
    }

    /** Everything of War that rides on {@code part}, in that part's own pixels. */
    public static void build(UkakuModel.Sink sink, Part part, double time, double grow) {
        build(sink, part, time, grow, 0.0);
    }

    /**
     * {@link #build}, {@code night} (0 by day .. 1 in the dead of night) deep in the night: the mask's pale ring goes dull and the eye
     * burns brighter, a halo round its white point (the layer darkens the rest of him and blooms the eye for the far-off).
     */
    public static void build(UkakuModel.Sink sink, Part part, double time, double grow, double night) {
        build(sink, part, time, grow, night, Motion.STILL);
    }

    /**
     * 1.0.6: what the rinkaku do in War's moves: {@code reach} (0..1) sends their points out in front of him to seize ({@code low}: and
     * down, to impale); {@code spread} fans them out round him (the storm, the eye); {@code frenzy} makes them thrash; 1.0.13:
     * {@code cocoon} wraps them round him, coil over coil, head to foot.
     */
    public record Motion(double reach, double low, double spread, double frenzy, double cocoon, double[][] aims, double orb) {
        public static final Motion STILL = new Motion(0.0, 0.0, 0.0, 0.0);

        public Motion(double reach, double low, double spread, double frenzy) {
            this(reach, low, spread, frenzy, 0.0, null, 0.0);
        }

        public Motion(double reach, double low, double spread, double frenzy, double cocoon, double[][] aims) {
            this(reach, low, spread, frenzy, cocoon, aims, 0.0);
        }

        /**
         * 1.0.28: King's Defiance: the rinkaku grown thick and wound round him into a sphere, {@code closed} (0..1) of the way shut
         * ({@code WarRules.defyClosed}).
         */
        public static Motion orbed(double closed) {
            return new Motion(0.0, 0.0, 0.0, 0.15, 0.0, null, clamp01(closed));
        }

        /**
         * 1.0.7: this motion with each rinkaku's own strike ({@code aims[i]}: {x, y, z, weight} in body pixels, null for none), the
         * rinkaku numbered as {@link #TENDRILS} goes, the right one of each pair first.
         */
        public Motion aiming(double[][] aims) {
            return new Motion(this.reach, this.low, this.spread, this.frenzy, this.cocoon, aims, this.orb);
        }

        /** The rinkaku in {@code move}, {@code progress} (0..1) through it. */
        public static Motion of(KingMove move, double progress) {
            return of(move, 0, progress);
        }

        /**
         * The rinkaku in {@code move} at {@code level}, {@code progress} (0..1) through it. 1.0.14: in his M1 they writhe with every blow,
         * fan out with the heel's turn, and for the Verdict drive down ahead of him into the ground (to come up through what it hits).
         */
        public static Motion of(KingMove move, int level, double progress) {
            if (move == KingMove.STRIKE) {
                double in = FurutaMesh.smooth(clamp01(progress * 2.5));
                double out = 1.0 - FurutaMesh.smooth(clamp01((progress - 0.7) * 3.3));
                return switch (WarStrike.byOrdinal(level)) {
                    case HEEL -> new Motion(0.0, 0.0, 0.45 * in * out, 1.2);
                    case VERDICT -> new Motion(0.9 * in * out, 1.0, 0.0, 1.0);
                    // 1.0.20: the launcher's rinkaku up under them; the spike's and Calamity's down into the ground; the ender's
                    // driven through them; the Cleaver's thrashing.
                    case ASCENSION -> new Motion(0.6 * in * out, 0.0, 0.15, 1.2);
                    case DOWNFALL -> new Motion(0.75 * in * out, 1.0, 0.0, 1.1);
                    case CALAMITY -> new Motion(1.0 * in * out, 1.0, 0.2 * in * out, 1.8);
                    case REND -> new Motion(1.0 * in * out, 0.0, 0.0, 1.4);
                    case CLEAVER -> new Motion(0.0, 0.0, 0.3 * in * out, 1.3);
                    default -> new Motion(0.0, 0.0, 0.1, 0.9);
                };
            }
            double in = FurutaMesh.smooth(clamp01(progress * 3.0));
            double out = 1.0 - FurutaMesh.smooth(clamp01((progress - 0.75) * 4.0));
            return switch (move) {
                case GRASP -> new Motion(0.0, 0.0, 0.1, 0.5);
                case DASH -> new Motion(0.0, 0.0, 0.1, 0.6);
                case CLEAVE -> new Motion(0.0, 0.0, 0.35 * in * out, 1.0);
                // Driven into the ground ahead with the arm: the wave is theirs.
                case SPIRE -> new Motion(0.85 * in * out, 1.0, 0.0, 0.6);
                case DEFIANCE -> new Motion(0.0, 0.0, 0.25, 0.3);
                case COUNTER -> new Motion(0.6 * in * out, 0.0, 0.0, 1.0);
                case COCOON -> new Motion(0.0, 0.0, 0.0, 0.2, FurutaMesh.smooth(clamp01(progress * 6.0)), null);
                case BURST -> new Motion(0.0, 0.0, out, 2.0);
                // 1.0.15: down into the ground with his hand, then thrashing as what they hold is lifted.
                case SUN -> new Motion(0.6 * in * out, 1.0, 0.3 * in * out, 1.4);
                // 1.0.20: the heavy charging: coiled round him, thrashing.
                case CHARGE -> new Motion(0.0, 0.0, 0.0, 1.6, 0.35, null);
                default -> STILL;
            };
        }
    }

    /** 1.0.35: body pixels to the block (as the rinkaku's aims are given). */
    static final double PIXELS = 16.0 / 0.9375;

    /**
     * 1.0.35: the rinkaku in War's M1 (the user's call: his blows were his fists alone): each blow of it thrown with some of the six too,
     * at what it hits ({@code at}, body pixels: x his left, y up, z back; null: ahead of him at the blow's reach), {@code age} ticks into
     * blow {@code level}. The jab with the upper pair stabbing past his fist; the reap with one swept across before him, the next after
     * it; the side kick with the lower pair driven in low; the rising cut with the middle pair torn up under it; the cleaver with the
     * upper pair brought down on it; the launcher with the lower pair hooked up under it. Per rinkaku {x, y, z, weight} or null (as
     * {@link Motion#aiming} takes them); null if the blow throws none.
     */
    public static double[][] strikeAims(int level, double age, double[] at) {
        WarStrike blow = WarStrike.byOrdinal(level);
        double[] t = at != null ? at : v(0.0, 22.0, -blow.reach * 0.85 * PIXELS);
        double impact = blow.impact;
        double lead = Math.max(2.0, impact);
        double w = age <= impact ? FurutaMesh.smooth(clamp01((age - (impact - lead)) / lead))
            : 1.0 - FurutaMesh.smooth(clamp01((age - impact - 1.5) / 4.0));
        if (w <= 0.0) {
            return null;
        }
        double sweep = FurutaMesh.smooth(clamp01((age - (impact - 2.0)) / 4.0));
        double[][] aims = new double[TENDRILS.length * 2][];
        switch (blow) {
            case JAB -> {
                aims[0] = new double[] {t[0] - 3.0, t[1] + 2.0, t[2], w};
                aims[1] = new double[] {t[0] + 3.0, t[1] + 1.0, t[2], w};
            }
            case REAP -> {
                // From his right across to his left (x is his left), the second a beat behind the first.
                aims[2] = new double[] {t[0] - 30.0 + 60.0 * sweep, t[1], t[2] + 6.0, w};
                double late = FurutaMesh.smooth(clamp01((age - (impact - 1.0)) / 4.0));
                aims[0] = new double[] {t[0] - 26.0 + 52.0 * late, t[1] + 6.0, t[2] + 8.0, 0.8 * w};
            }
            case SIDE_KICK -> {
                aims[4] = new double[] {t[0] - 3.0, t[1] - 10.0, t[2], w};
                aims[5] = new double[] {t[0] + 3.0, t[1] - 12.0, t[2], w};
            }
            case RISING -> {
                aims[2] = new double[] {t[0] - 6.0, t[1] - 24.0 + 42.0 * sweep, t[2] + 2.0, w};
                aims[3] = new double[] {t[0] + 6.0, t[1] - 26.0 + 42.0 * sweep, t[2] + 2.0, w};
            }
            case ASCENSION -> {
                aims[4] = new double[] {t[0] - 4.0, t[1] - 22.0 + 44.0 * sweep, t[2], w};
                aims[5] = new double[] {t[0] + 4.0, t[1] - 22.0 + 44.0 * sweep, t[2], w};
            }
            case CLEAVER -> {
                aims[0] = new double[] {t[0] - 5.0, t[1] + 40.0 - 40.0 * sweep, t[2], w};
                aims[1] = new double[] {t[0] + 5.0, t[1] + 40.0 - 40.0 * sweep, t[2], w};
            }
            default -> {
                return null;
            }
        }
        return aims;
    }

    /** 1.0.35: {@code first}'s aims with {@code over}'s laid over them where it has one (either may be null). */
    public static double[][] overlay(double[][] first, double[][] over) {
        if (first == null) {
            return over;
        }
        if (over == null) {
            return first;
        }
        double[][] out = first.clone();
        for (int i = 0; i < out.length && i < over.length; i++) {
            if (over[i] != null) {
                out[i] = over[i];
            }
        }
        return out;
    }

    /** {@link #build} with the rinkaku in {@code motion}. */
    public static void build(UkakuModel.Sink sink, Part part, double time, double grow, double night, Motion motion) {
        build(sink, part, time, grow, night, motion, null);
    }

    /**
     * 1.0.11: {@link #build} with the one eye where the player's kakugan is ({@code eye}, from {@link #eyeOn}; null: the usual left
     * eye). On the right eye the whole mask is the mirror image of itself.
     */
    public static void build(UkakuModel.Sink sink, Part part, double time, double grow, double night, Motion motion, double[] eye) {
        build(sink, part, time, grow, night, motion, eye, false);
    }

    /** 1.0.17: {@link #build}, the cape left to {@link WarCloth} ({@code cloth}: it hangs and moves as real cloth, drawn on its own). */
    public static void build(UkakuModel.Sink sink, Part part, double time, double grow, double night, Motion motion, double[] eye, boolean cloth) {
        Body body = new Body(sink, part);
        switch (part) {
            case HEAD -> {
                double[] at = eye == null ? DEFAULT_EYE : eye;
                mask(at[0] < 0.0 ? new Body(mirrored(sink), part) : body, time, grow, night, Math.abs(at[0]), at[1]);
            }
            case TORSO -> {
                coat(body, grow);
                collar(body, grow);
                mantle(body, time, grow);
                if (!cloth) {
                    cape(body, time, grow);
                }
                tendrils(body, time, grow, motion == null ? Motion.STILL : motion);
            }
            case RIGHT_ARM -> rightArm(body, time, grow);
            case LEFT_ARM -> leftArm(body, time, grow);
            case RIGHT_LEG, LEFT_LEG -> leg(body, part == Part.RIGHT_LEG ? -1 : 1, grow);
        }
    }

    /**
     * 1.0.26: War's rinkaku alone (in the torso's pixels, as {@link #build} draws them on it), nothing of his armour: what first person
     * draws of his kagune, reaching out past his eyes.
     */
    public static void rinkaku(UkakuModel.Sink sink, double time, double grow, Motion motion) {
        tendrils(new Body(sink, Part.TORSO), time, grow, motion == null ? Motion.STILL : motion);
    }

    // ================================================================== drawing in body pixels

    /** A sink in body pixels: converts corners and normals to the part's pixels (y flipped) and keeps the glow marker. */
    static final class Body {
        final UkakuModel.Sink out;
        final Part part;
        int quads;

        Body(UkakuModel.Sink out, Part part) {
            this.out = out;
            this.part = part;
        }

        void shaded(double[] a, double[] b, double[] c, double[] d, double[] centre, int ca, int cb, int cc, int cd) {
            double[] n = normalize(cross(sub(c, a), sub(d, b)));
            if (dot(n, n) < 0.5) {
                n = normalize(cross(sub(b, a), sub(c, a)));
                if (dot(n, n) < 0.5) {
                    return;
                }
            }
            double[] mid = mul(add(add(a, b), add(c, d)), 0.25);
            if (dot(n, sub(mid, centre)) < 0.0) {
                n = mul(n, -1.0);
            }
            double[] ln = {n[0], -n[1], n[2]};
            if (ln[0] == 0.0 && ln[1] == 1.0 && ln[2] == 0.0) {
                ln = normalize(new double[] {1.0E-4, 1.0, 1.0E-4});
            }
            this.out.quad(this.part.local(a), this.part.local(b), this.part.local(c), this.part.local(d), ln, solid(ca), solid(cb), solid(cc), solid(cd));
            this.quads++;
        }

        void glow(double[] a, double[] b, double[] c, double[] d, int ca, int cb, int cc, int cd) {
            this.out.quad(this.part.local(a), this.part.local(b), this.part.local(c), this.part.local(d), new double[] {0.0, 1.0, 0.0}, solid(ca), solid(cb),
                solid(cc), solid(cd));
            this.quads++;
        }

        /** {@link FurutaMesh#mix} gives colours without alpha: War is opaque. */
        static int solid(int colour) {
            return colour >>> 24 == 0 ? colour | 0xFF000000 : colour;
        }
    }

    static double[] v(double x, double y, double z) {
        return new double[] {x, y, z};
    }

    /** A centre line through control points (Catmull-Rom), framed by parallel transport from {@code up}. */
    static KakujaModel.Spine spine(List<double[]> control, int samples, double[] up) {
        int n = control.size();
        double[][] p = new double[samples + 1][];
        for (int i = 0; i <= samples; i++) {
            double u = (double)i / samples * (n - 1);
            int k = Math.min(n - 2, (int)Math.floor(u));
            double f = u - k;
            double[] p0 = control.get(Math.max(0, k - 1));
            double[] p1 = control.get(k);
            double[] p2 = control.get(k + 1);
            double[] p3 = control.get(Math.min(n - 1, k + 2));
            p[i] = catmull(p0, p1, p2, p3, f);
        }
        double[][] t = new double[samples + 1][];
        double[][] nn = new double[samples + 1][];
        double[][] b = new double[samples + 1][];
        double length = 0.0;
        for (int i = 0; i <= samples; i++) {
            double[] d = sub(p[Math.min(samples, i + 1)], p[Math.max(0, i - 1)]);
            t[i] = normalize(d);
            if (i > 0) {
                length += Math.sqrt(dot(sub(p[i], p[i - 1]), sub(p[i], p[i - 1])));
            }
        }
        double[] ref = sub(up, mul(t[0], dot(up, t[0])));
        nn[0] = dot(ref, ref) < 1.0E-8 ? perpendicular(t[0]) : normalize(ref);
        for (int i = 1; i <= samples; i++) {
            double[] q = sub(nn[i - 1], mul(t[i], dot(nn[i - 1], t[i])));
            nn[i] = dot(q, q) < 1.0E-8 ? perpendicular(t[i]) : normalize(q);
        }
        for (int i = 0; i <= samples; i++) {
            b[i] = normalize(cross(t[i], nn[i]));
        }
        return new KakujaModel.Spine(p, t, nn, b, length);
    }

    static double[] catmull(double[] p0, double[] p1, double[] p2, double[] p3, double f) {
        double f2 = f * f;
        double f3 = f2 * f;
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) {
            r[i] = 0.5 * (2.0 * p1[i] + (-p0[i] + p2[i]) * f + (2.0 * p0[i] - 5.0 * p1[i] + 4.0 * p2[i] - p3[i]) * f2
                + (-p0[i] + 3.0 * p1[i] - 3.0 * p2[i] + p3[i]) * f3);
        }
        return r;
    }

    /**
     * A blade along a curve: a diamond section {@code width} across {@code side} (its edges) and {@code thick} through it,
     * tapering to a point; shaded from {@code root} to {@code tip}. {@code seam} (if not 0) glows along its {@code +side} edge.
     */
    static void shard(Body body, double[] base, double[] dir, double[] bend, double length, double width, double thick, double[] side, int root, int tip,
                      int seam, int stations) {
        shard(body, base, dir, bend, length, width, thick, side, root, tip, seam, stations, false);
    }

    /** {@code blunt}: a plate (a scale) that keeps its width down most of its length before it comes to a point. */
    static void shard(Body body, double[] base, double[] dir, double[] bend, double length, double width, double thick, double[] side, int root, int tip,
                      int seam, int stations, boolean blunt) {
        if (length < 0.05) {
            return;
        }
        double[] d = normalize(dir);
        double[] bb = sub(bend, mul(d, dot(bend, d)));
        bb = dot(bb, bb) < 1.0E-10 ? perpendicular(d) : normalize(bb);
        double[] c1 = add(base, mul(d, length * 0.5));
        double[] c2 = add(add(base, mul(d, length)), mul(bb, length * 0.22));
        double[][][] ring = new double[stations + 1][][];
        double[][] centre = new double[stations + 1][];
        int[] colour = new int[stations + 1];
        for (int i = 0; i <= stations; i++) {
            double u = (double)i / stations;
            double[] p = add(add(mul(base, (1 - u) * (1 - u)), mul(c1, 2 * u * (1 - u))), mul(c2, u * u));
            double[] tng = normalize(add(mul(sub(c1, base), 2 * (1 - u)), mul(sub(c2, c1), 2 * u)));
            double[] s = sub(side, mul(tng, dot(side, tng)));
            s = dot(s, s) < 1.0E-10 ? perpendicular(tng) : normalize(s);
            double[] nrm = cross(tng, s);
            double k = blunt ? Math.min(1.0, 0.8 + u * 0.8) * Math.min(1.0, (1.0 - u) / 0.45) : Math.pow(1.0 - u, 0.85);
            double w = width * 0.5 * k;
            double h = thick * 0.5 * Math.max(0.25, k);
            ring[i] = new double[][] {add(p, mul(s, w)), add(p, mul(nrm, h)), sub(p, mul(s, w)), sub(p, mul(nrm, h))};
            centre[i] = p;
            colour[i] = mix(root, tip, Math.pow(u, 0.9));
        }
        for (int i = 0; i < stations; i++) {
            double[] mid = lerp(centre[i], centre[i + 1], 0.5);
            for (int j = 0; j < 4; j++) {
                int k = (j + 1) % 4;
                body.shaded(ring[i][j], ring[i + 1][j], ring[i + 1][k], ring[i][k], mid, colour[i], colour[i + 1], colour[i + 1], colour[i]);
            }
        }
        if (seam != 0) {
            for (int i = 0; i < stations - 1; i++) {
                double[] a = ring[i][0];
                double[] b = ring[i + 1][0];
                double[] in0 = lerp(ring[i][0], ring[i][1], 0.18);
                double[] in1 = lerp(ring[i + 1][0], ring[i + 1][1], 0.18);
                double[] lift0 = mul(normalize(sub(a, centre[i])), 0.04);
                double[] lift1 = mul(normalize(sub(b, centre[i + 1])), 0.04);
                body.glow(add(a, lift0), add(b, lift1), add(in1, lift1), add(in0, lift0), seam, seam, seam, seam);
            }
        }
    }

    /**
     * A rounded box's skin round {@code centre} ({@code radii} half sizes, squareness {@code power}), where {@code keep} says
     * (with the point and its outward direction), coloured by {@code colour}.
     */
    static void shell(Body body, double[] centre, double[] radii, double power, int bands, int segments, java.util.function.Predicate<double[]> keep,
                      java.util.function.ToIntFunction<double[]> colour) {
        double[][][] grid = new double[bands + 1][segments + 1][];
        for (int i = 0; i <= bands; i++) {
            double theta = Math.PI * i / bands;
            for (int j = 0; j <= segments; j++) {
                double phi = Math.PI * 2.0 * j / segments;
                double sx = Math.sin(theta) * Math.cos(phi);
                double sy = Math.cos(theta);
                double sz = Math.sin(theta) * Math.sin(phi);
                double e = 2.0 / power;
                grid[i][j] = add(centre, v(radii[0] * sgnpow(sx, e), radii[1] * sgnpow(sy, e), radii[2] * sgnpow(sz, e)));
            }
        }
        for (int i = 0; i < bands; i++) {
            for (int j = 0; j < segments; j++) {
                double[] a = grid[i][j];
                double[] b = grid[i + 1][j];
                double[] c = grid[i + 1][j + 1];
                double[] d = grid[i][j + 1];
                double[] mid = mul(add(add(a, b), add(c, d)), 0.25);
                if (!keep.test(mid)) {
                    continue;
                }
                body.shaded(a, b, c, d, centre, colour.applyAsInt(a), colour.applyAsInt(b), colour.applyAsInt(c), colour.applyAsInt(d));
            }
        }
    }

    static double sgnpow(double v, double e) {
        return Math.signum(v) * Math.pow(Math.abs(v), e);
    }

    /** A flat disc (or ring: {@code inner} > 0) facing {@code normal}, as shaded quads or glowing ones. */
    static void disc(Body body, double[] centre, double[] normal, double[] up, double inner, double outer, double ry, int colourIn, int colourOut, int sides,
                     boolean glow) {
        double[] n = normalize(normal);
        double[] u = normalize(sub(up, mul(n, dot(up, n))));
        double[] w = cross(n, u);
        double scale = ry / outer;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2.0 * i / sides;
            double a1 = Math.PI * 2.0 * (i + 1) / sides;
            double[] o0 = add(centre, add(mul(w, Math.cos(a0) * outer), mul(u, Math.sin(a0) * outer * scale)));
            double[] o1 = add(centre, add(mul(w, Math.cos(a1) * outer), mul(u, Math.sin(a1) * outer * scale)));
            double[] i0 = add(centre, add(mul(w, Math.cos(a0) * inner), mul(u, Math.sin(a0) * inner * scale)));
            double[] i1 = add(centre, add(mul(w, Math.cos(a1) * inner), mul(u, Math.sin(a1) * inner * scale)));
            if (glow) {
                body.glow(i0, o0, o1, i1, colourIn, colourOut, colourOut, colourIn);
            } else {
                body.shaded(i0, o0, o1, i1, sub(centre, n), colourIn, colourOut, colourOut, colourIn);
            }
        }
    }

    /** A thin glowing line on the mask from {@code a} to {@code b}, facing forward. */
    static void vein(Body body, double[] a, double[] b, double width, int ca, int cb) {
        double[] across = mul(normalize(cross(sub(b, a), v(0, 0, 1))), width);
        body.glow(add(a, across), add(b, across), sub(b, across), sub(a, across), ca, cb, cb, ca);
    }

    /** Mottled armour: {@code base} with darker and lighter patches by position, so the plates read as grown, not painted. */
    static int mottle(double[] p, int base, int light, int seed) {
        double n = noise(seed, p[0] * 0.55 + p[2] * 0.35, p[1] * 0.55);
        return n > 0.55 ? mix(base, light, (n - 0.55) * 2.4) : mix(base, BLACK, (0.55 - n) * 0.8);
    }

    // ================================================================== head
    /** 1.0.11: where the eye is on the usual placement (Steve's eyes, the left one), body pixels. */
    static final double[] DEFAULT_EYE = eyeOn(KakuganEyes.DEFAULT);

    /** Where the eye's white point is, in the head's own pixels, for a mask grown {@code grow}. */
    public static double[] eye(double grow) {
        return eye(grow, null);
    }

    /** 1.0.11: and with the eye on the player's kakugan ({@code at}, from {@link #eyeOn}). */
    public static double[] eye(double grow, double[] at) {
        double out = 0.5 + 0.2 * grow;
        double[] e = at == null ? DEFAULT_EYE : at;
        return Part.HEAD.local(v(e[0] + 0.06 * Math.signum(e[0]), e[1] + 0.02, -4.0 - out - 0.32));
    }

    /**
     * 1.0.11: where War's one eye goes (body pixels {x, y}): right on the player's kakugan as they placed it ({@code KakuganEyes}): the
     * ghoul eye of a one-eyed placement, else the left one. x below 0 is on the player's right (the mask mirrored there).
     */
    public static double[] eyeOn(KakuganEyes eyes) {
        KakuganEyes e = eyes == null ? KakuganEyes.DEFAULT : eyes.validated();
        boolean right = e.ghoul() == KakuganEyes.RIGHT;
        int column = right ? e.rightX() : e.leftX();
        int row = right ? e.rightY() : e.leftY();
        // Face columns run from the player's right (x -4) to his left (x 4); rows from the top of the head (y 32) down.
        return new double[] {column + KakuganEyes.WIDTH / 2.0 - 4.0, 32.0 - row - e.height() / 2.0};
    }

    /** How far {@code p} is from the segment {@code a}..{@code b} across the face (x and y only). */
    static double flatDistance(double[] p, double[] a, double[] b) {
        double vx = b[0] - a[0];
        double vy = b[1] - a[1];
        double t = clamp01(((p[0] - a[0]) * vx + (p[1] - a[1]) * vy) / Math.max(1.0E-9, vx * vx + vy * vy));
        return Math.hypot(p[0] - (a[0] + vx * t), p[1] - (a[1] + vy * t));
    }

    /** {@code sink} seen in a mirror across the middle of the head: x turned over, each quad wound the other way so it still faces out. */
    static UkakuModel.Sink mirrored(UkakuModel.Sink sink) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(v(-a[0], a[1], a[2]), v(-d[0], d[1], d[2]), v(-c[0], c[1], c[2]), v(-b[0], b[1], b[2]),
            v(-n[0], n[1], n[2]), ca, cd, cc, cb);
    }

    /** The mask, its one eye at ({@code ex}, {@code ey}) on the left of the face (body pixels; 1.0.11: on the kakugan). */
    static void mask(Body body, double time, double grow, double night, double ex, double ey) {
        if (grow <= 0.02) {
            return;
        }
        // 1.0.12: the mask keeps its own shape wherever the kakugan is; only the eye moves (and what it would land on makes way).
        double[] eyeAt = v(ex, ey, 0.0);
        double[] centre = v(0.0, 28.0, 0.0);
        double out = 0.5 + 0.2 * grow;
        double[] radii = v(4.0 + out, 4.0 + out, 4.0 + out);
        // A deep helmet over the front of the head and the crown; the back of the head and the mouth (the character's left of
        // it, under the eye) stay the player's own. On the right it runs down over the jaw.
        shell(body, centre, radii, 6.5, 22, 40, p -> {
            if (p[2] > 1.6 && p[1] < 31.2) {
                return false;
            }
            if (p[2] > 3.2) {
                return false;
            }
            // The mouth: never higher than usual, lower when the eye sits low.
            if (p[2] < -2.0 && p[1] < Math.min(26.3, ey - 1.6) && p[0] > -0.6 && p[0] < 3.6) {
                return false;
            }
            return p[1] > (p[0] < -0.6 ? 23.6 : 24.4);
        }, p -> mottle(p, BLACK, GREY, 11));
        // Raised plates over the face, swept up and out from the nose, grey on their edges.
        for (int k = 0; k < 9; k++) {
            int sx = k % 2 == 0 ? -1 : 1;
            if (sx > 0 && 25.4 + 0.75 * k < 28.9) {
                continue; // the mouth, under the eye
            }
            double[] a = v(sx * (0.5 + 0.2 * k), 25.4 + 0.75 * k, -4.0 - out - 0.05);
            double[] dir = v(sx * (2.4 + 0.25 * k), 1.0 + 0.2 * k, 0.5);
            if (flatDistance(eyeAt, a, add(a, dir)) < 2.0) {
                continue; // the eye
            }
            shard(body, a, dir, v(0, 0, 1), Math.sqrt(dot(dir, dir)) + 0.6, 1.4, 0.6, v(0, 1, 0), k % 3 == 0 ? CHAR : BLACK, k % 2 == 0 ? GREY : CHAR_L, 0,
                3);
        }
        // Jagged drips off the jaw on the right.
        for (int k = 0; k < 5; k++) {
            double x = -4.1 + 0.85 * k;
            shard(body, v(x, 24.2, -4.0 - out + 0.3), v(0.1, -1.0, 0.15), v(0, 0, 1), 1.2 + hash(k + 140) * 1.3, 1.0, 0.6, v(1, 0, 0), BLACK, CHAR, 0, 2);
        }
        // Red cracks: jagged, splitting across the right cheek and over the brow.
        double z = -4.0 - out - 0.1;
        double[][][] cracks = {
            {v(-4.3, 25.2, z), v(-3.5, 26.1, z), v(-3.7, 26.9, z), v(-2.8, 27.6, z), v(-2.1, 28.9, z), v(-1.3, 29.3, z)},
            {v(-3.7, 26.9, z), v(-4.3, 27.9, z), v(-4.1, 28.8, z)},
            {v(-3.9, 31.6, z), v(-3.0, 31.2, z), v(-2.4, 31.9, z), v(-1.5, 31.5, z)},
            {v(4.3, 29.0, z), v(3.6, 29.9, z), v(3.8, 30.6, z), v(3.0, 31.5, z)},
        };
        for (double[][] c : cracks) {
            for (int k = 0; k + 1 < c.length; k++) {
                if (flatDistance(eyeAt, c[k], c[k + 1]) < 1.6) {
                    continue;
                }
                double w = 0.17 * (1.0 - 0.5 * k / (c.length - 1.0));
                vein(body, c[k], c[k + 1], w, k == 0 ? BLOOD : RED, RED);
            }
        }
        // The ring on the forehead: a raised black rim round a white disc.
        // (1.0.12: where it always is, unless the eye is set right under it: then across the brow, on the far side.)
        double ringX = Math.hypot(ex - 0.5, ey - 29.7) < 3.0 ? Math.max(-2.1, ex - 3.9) : 0.5;
        double[] ring = v(ringX, 29.7, -4.0 - out - 0.2);
        disc(body, ring, v(0, 0, -1), v(0, 1, 0), 1.15, 1.85, 1.85, BLACK, BLACK2, 24, false);
        // By night it goes dull, and the eye is all that shines.
        disc(body, add(ring, v(0, 0, -0.04)), v(0, 0, -1), v(0, 1, 0), 0.0, 1.2, 1.2, mix(0xF4F0EA, 0x3A3836, night), mix(0xD9D3CB, 0x2A2826, night), 24,
            night < 0.5);
        // The left eye: sunk in a socket, all black, with one glowing white point staring out of it; red veins crawl out over
        // the mask and a line of blood runs down the cheek.
        double[] eye = v(ex, ey, -4.0 - out - 0.2);
        disc(body, add(eye, v(0, 0, 0.04)), v(0, 0, -1), v(0, 1, 0), 0.0, 1.45, 1.2, PUPIL, BLACK2, 18, false);
        disc(body, eye, v(0, 0, -1), v(0, 1, 0), 0.0, 1.05, 0.66, 0xFF050405, PUPIL, 20, false);
        disc(body, add(eye, v(0.0, -0.18, -0.05)), v(0, 0, -1), v(0, 1, 0), 0.0, 0.62, 0.5, 0xFF2A0306, 0xFF050405, 14, false);
        disc(body, add(eye, v(0.06, 0.02, -0.09)), v(0, 0, -1), v(0, 1, 0), 0.24, 0.42, 0.42, 0xFF4A4A4A, 0xFF050405, 14, true);
        disc(body, add(eye, v(0.06, 0.02, -0.11)), v(0, 0, -1), v(0, 1, 0), 0.0, 0.24, 0.24, 0xFFFFFFFF, 0xFFF4F4F4, 12, true);
        if (night > 0.01) {
            int halo = mix(0x000000, 0x9A9A9A, night);
            disc(body, add(eye, v(0.06, 0.02, -0.13)), v(0, 0, -1), v(0, 1, 0), 0.2, 0.9, 0.9, halo, 0x000000, 16, true);
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2.0 * k / 8 + (hash(k + 210) - 0.5) * 0.5;
            double r0 = 0.9;
            double r1 = 1.5 + hash(k + 220) * 0.9;
            double[] from = add(eye, v(Math.cos(a) * r0 * 1.05, Math.sin(a) * r0 * 0.72, -0.02));
            double[] knee = add(eye, v(Math.cos(a + 0.25) * (r0 + r1) * 0.55, Math.sin(a + 0.25) * (r0 + r1) * 0.42, 0.02));
            double[] to = add(eye, v(Math.cos(a - 0.15) * r1, Math.sin(a - 0.15) * r1 * 0.8, 0.08));
            vein(body, from, knee, 0.09, RED, BLOOD);
            vein(body, knee, to, 0.06, BLOOD, CRIMSON);
        }
        double[] tear = v(ex - 0.3, ey - 1.4, -4.0 - out - 0.15);
        vein(body, add(eye, v(-0.15, -0.65, -0.02)), tear, 0.13, BLOOD, RED);
        vein(body, tear, v(ex - 0.1, Math.max(24.2, ey - 2.8), -4.0 - out - 0.1), 0.1, RED, CRIMSON);
        // The crown: shards from the top and the sides of the helmet, up, out and back.
        double reach = FurutaMesh.smooth(clamp01(grow * 1.4 - 0.2));
        List<double[]> crown = crownSpikes();
        for (int k = 0; k < crown.size(); k++) {
            double[] s = crown.get(k);
            double[] base = v(s[0], s[1], s[2]);
            double[] from = sub(base, centre);
            double top = clamp01((s[1] - 29.0) / 3.5);
            double[] dir = normalize(add(normalize(v(from[0], Math.max(0.0, from[1]), from[2] * 0.4)), v(0.0, 0.5 + 0.7 * top, 0.55)));
            double sway = 0.05 * Math.sin(time * 0.05 + k * 1.7);
            dir = normalize(add(dir, v(sway, 0, sway)));
            double[] sideAxis = normalize(cross(dir, v(0, 0, 1)));
            int tone = (int)Math.floor(hash(k * 13 + 5) * 6);
            int rootCol = tone < 3 ? BLACK : CHAR;
            int tipCol = tone == 5 ? GREY : tone >= 3 ? CHAR_L : CHAR;
            shard(body, sub(base, mul(dir, 0.6)), dir, v(0, 0.4, 1.0), s[3] * reach + 0.6, s[4], s[5], sideAxis, rootCol, tipCol, k % 3 == 1 ? RED : 0, 4);
        }
    }

    /** Crown shards: {x, y, z, length, width, thickness} in body pixels (fixed, from a seed). */
    static List<double[]> crownSpikes() {
        List<double[]> out = new ArrayList<>();
        java.util.Random r = new java.util.Random(5);
        // Over the top, densest at the front.
        for (int i = 0; i < 16; i++) {
            double x = -4.0 + 8.0 * (i + 0.5) / 16 + (r.nextDouble() - 0.5) * 0.6;
            double z = -4.3 + Math.pow(r.nextDouble(), 1.6) * 6.0;
            double middle = 1.0 - Math.abs(x) / 4.6;
            out.add(new double[] {x, 32.5, z, 1.6 + 2.0 * middle + r.nextDouble() * 1.0, 1.5 + r.nextDouble() * 0.8, 0.7 + r.nextDouble() * 0.3});
        }
        // Round the sides of the helmet.
        for (int i = 0; i < 14; i++) {
            int sx = i % 2 == 0 ? -1 : 1;
            double y = 26.5 + 5.6 * r.nextDouble();
            double z = -4.3 + r.nextDouble() * 5.2;
            out.add(new double[] {sx * 4.55, y, z, 1.2 + 2.0 * (y - 26.0) / 6.0 + r.nextDouble() * 0.7, 1.3 + r.nextDouble() * 0.6, 0.6 + r.nextDouble() * 0.3});
        }
        // Along the brow.
        for (int i = 0; i < 6; i++) {
            out.add(new double[] {-3.8 + 7.6 * (i + 0.5) / 6, 31.9, -4.6, 1.6 + r.nextDouble() * 1.2, 1.3 + r.nextDouble() * 0.5, 0.6});
        }
        return out;
    }

    // ================================================================== torso
    static void coat(Body body, double grow) {
        if (grow <= 0.02) {
            return;
        }
        double[] centre = v(0.0, 18.1, 0.0);
        shell(body, centre, v(4.62, 6.25, 2.62), 8.0, 20, 36, p -> {
            boolean vee = p[2] < -1.0 && p[1] > 19.4 && Math.abs(p[0] - 0.2) < (p[1] - 19.4) * 0.62;
            return !vee && p[1] < 24.4 && p[1] > 11.9;
        }, p -> mottle(p, WarCloth.CLOTH_DEEP, WarCloth.CLOTH_SHEEN, 21));
        // Lapels down the V.
        for (int sx = -1; sx <= 1; sx += 2) {
            double[] a = v(0.2 + sx * 0.15, 19.4, -2.75);
            double[] b = v(0.2 + sx * 2.9, 24.3, -2.75);
            shard(body, a, sub(b, a), v(0, 0, -1), Math.sqrt(dot(sub(b, a), sub(b, a))) + 0.6, 1.2, 0.7, v(sx, -0.5, 0), WarCloth.CLOTH_SHEEN, WarCloth.CLOTH, 0, 3);
        }
        // Layered plates across the chest and belly.
        double[] rows = {19.1, 17.3, 15.7};
        for (int k = 0; k < rows.length; k++) {
            double[] a = v(-3.7 + 0.4 * k, rows[k], -2.75);
            shard(body, a, v(1, -0.08, 0), v(0, 0, -1), 7.2 - 0.6 * k, 1.1, 0.6, v(0, 1, 0), k % 2 == 0 ? CHAR_L : CHAR, CHAR, 0, 4);
        }
        // The sash, left shoulder to right hip (1.0.17: black as all he wears).
        double[] s0 = v(4.4, 24.0, -2.85);
        double[] s1 = v(-4.4, 13.8, -2.85);
        shard(body, s0, sub(s1, s0), v(0, 0, -1), Math.sqrt(dot(sub(s1, s0), sub(s1, s0))) + 0.8, 1.7, 0.7, v(0.6, 0.6, 0), WarCloth.CLOTH_SHEEN, WarCloth.CLOTH_DEEP,
            0, 6);
        // The belt.
        shell(body, v(0.0, 13.2, 0.0), v(4.85, 0.8, 2.85), 10.0, 6, 32, p -> true, p -> mottle(p, WarCloth.CLOTH_DEEP, WarCloth.CLOTH, 22));
    }

    static void collar(Body body, double grow) {
        if (grow <= 0.02) {
            return;
        }
        // The high collar: upright plates round the neck, flaring out past the helmet, the front corners highest.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int k = 0; k < 4; k++) {
                double[] base = v(sx * (2.6 + 0.75 * k), 23.2, -2.4 + 0.9 * k);
                double[] dir = v(sx * (0.5 + 0.1 * k), 1.0, -0.15 + 0.08 * k);
                shard(body, base, dir, v(sx, 0, -0.3), (3.6 - 0.45 * k) * grow + 0.4, 2.6, 0.8, v(0, 0, 1), k == 1 ? CHAR : BLACK, k == 0 ? GREY : CHAR_L, 0, 4,
                    true);
            }
        }
        for (int k = 0; k < 7; k++) {
            double a = Math.toRadians(-60 + 120 * k / 6.0);
            double[] base = v(Math.sin(a) * 3.8, 23.4, 2.3 + Math.cos(a) * 0.4);
            shard(body, base, v(Math.sin(a) * 0.4, 1.0, 0.3), v(0, 0, 1), (2.4 + 0.6 * Math.cos(a)) * grow + 0.3, 2.2, 0.7, v(Math.cos(a), 0, -Math.sin(a)),
                BLACK, CHAR, 0, 3, true);
        }
    }

    static void mantle(Body body, double time, double grow) {
        if (grow <= 0.02) {
            return;
        }
        shell(body, v(0.0, 22.6, 2.4), v(6.6, 2.6, 1.5), 4.0, 10, 36, p -> p[2] > 2.0 && p[1] > 20.6, p -> mottle(p, WarCloth.CLOTH_DEEP, WarCloth.CLOTH_SHEEN, 31));
        // Spiked plates jutting out at the shoulders.
        for (int k = 0; k < 8; k++) {
            int sx = k % 2 == 0 ? -1 : 1;
            double[] base = v(sx * (3.0 + 0.75 * k), 23.0 - 0.15 * k, 3.1);
            shard(body, base, v(sx, 0.2 + 0.05 * k, 0.2), v(0, 1, 0), (2.4 + 0.3 * k) * grow, 1.8, 0.7, v(0, 1, 0), BLACK, CHAR, 0, 3);
        }
    }

    /** The cape's strips: {x at the top, y top, z top, width, length, layer (0 back, 1 outer, 2 sides), flare}. */
    static List<double[]> capeStrips() {
        List<double[]> out = new ArrayList<>();
        java.util.Random r = new java.util.Random(37);
        for (int k = 0; k < 18; k++) {
            double x = -7.0 + 14.0 * k / 17.0;
            double edge = Math.abs(x) / 7.0;
            out.add(new double[] {x * 0.8, 22.2, 3.1, 1.8 + r.nextDouble() * 0.7, 17.0 + r.nextDouble() * 4.0 - 2.0 * edge, 0, x / 7.0});
        }
        for (int k = 0; k < 14; k++) {
            double x = -6.2 + 12.4 * k / 13.0;
            out.add(new double[] {x * 0.82, 21.9, 3.8, 1.6 + r.nextDouble() * 0.6, 10.5 + r.nextDouble() * 7.0, 1, x / 6.2});
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int k = 0; k < 4; k++) {
                out.add(new double[] {sx * (4.6 + 0.3 * k), 21.4 - 0.5 * k, 2.0 - 1.1 * k, 1.8, 14.5 + r.nextDouble() * 4.0 - k, 2, sx});
            }
        }
        return out;
    }

    static void cape(Body body, double time, double grow) {
        if (grow <= 0.02) {
            return;
        }
        List<double[]> strips = capeStrips();
        for (int i = 0; i < strips.size(); i++) {
            double[] s = strips.get(i);
            int layer = (int)s[5];
            double length = s[4] * (0.3 + 0.7 * grow);
            double flare = s[6];
            List<double[]> pts = new ArrayList<>();
            int points = 5;
            for (int k = 0; k < points; k++) {
                double u = (double)k / (points - 1);
                double sway = Math.sin(time * 0.07 + i * 0.9 + u * 2.2) * 0.9 * u * u + Math.sin(time * 0.031 + i * 2.3) * 0.5 * u;
                double out = layer == 2 ? 4.6 * u : Math.abs(flare) * 1.6 * u;
                double x = s[0] + (layer == 2 ? flare * out : flare * 3.4 * u) + sway * 0.3;
                double y = s[1] - length * u;
                double z = s[2] + (layer == 2 ? -0.8 * u : 1.0 + 2.4 * u * u) * (layer == 0 ? 1.0 : 1.1) + sway * (layer == 2 ? 0.4 : 1.0) + (layer == 2 ? 0 : 0.0);
                pts.add(v(x, y, z));
            }
            double[] across = layer == 2 ? v(0, 0, 1) : v(1, 0, 0);
            ribbon(body, pts, s[3], 0.45, across, i, layer);
        }
    }

    /** A cape strip: a thin band along the points, widening a little, ending in a torn point; dark above, red at the end. */
    static void ribbon(Body body, List<double[]> control, double width, double thick, double[] across, int seed, int layer) {
        int stations = 12;
        KakujaModel.Spine sp = spine(control, stations, v(0, 0, 1));
        double[][][] ring = new double[stations + 1][][];
        int[] colour = new int[stations + 1];
        for (int i = 0; i <= stations; i++) {
            double u = (double)i / stations;
            double[][] f = sp.at(u);
            double[] t = f[1];
            double[] s = sub(across, mul(t, dot(across, t)));
            s = dot(s, s) < 1.0E-10 ? f[2] : normalize(s);
            double[] n = cross(t, s);
            double tear = u > 0.78 ? 1.0 - (u - 0.78) / 0.22 : 1.0;
            double w = width * 0.5 * (1.0 + 0.25 * u) * Math.max(0.0, tear);
            double skew = u > 0.78 ? (hash(seed * 7 + 3) - 0.5) * width * 0.6 * (u - 0.78) / 0.22 : 0.0;
            double[] p = add(f[0], mul(s, skew));
            ring[i] = new double[][] {add(p, mul(s, w)), add(p, mul(n, thick * 0.5)), sub(p, mul(s, w)), sub(p, mul(n, thick * 0.5))};
            // 1.0.17: pure black, deepest at the torn end.
            int c = mix(WarCloth.CLOTH, WarCloth.CLOTH_SHEEN, hash(seed * 11 + 4) * 0.6 * (1.0 - u));
            c = mix(c, WarCloth.CLOTH_DEEP, clamp01((u - 0.55) / 0.45) * 0.8);
            colour[i] = mix(c, WarCloth.CLOTH_DEEP, 0.3 * noise(seed, u * 7.0, 1.0));
        }
        for (int i = 0; i < stations; i++) {
            double[] mid = lerp(sp.at((double)i / stations)[0], sp.at((double)(i + 1) / stations)[0], 0.5);
            for (int j = 0; j < 4; j++) {
                int k = (j + 1) % 4;
                body.shaded(ring[i][j], ring[i + 1][j], ring[i + 1][k], ring[i][k], mid, colour[i], colour[i + 1], colour[i + 1], colour[i]);
            }
        }
    }

    // ================================================================== rinkaku
    /** The six rinkaku's control points (the right side; the left mirrors x), from between the shoulder blades. */
    static final double[][][] TENDRILS = {
        {{1.0, 22.5, 2.6}, {2.6, 30.0, 5.2}, {4.6, 37.0, 3.6}, {9.6, 36.6, 0.6}, {12.4, 31.0, -2.4}, {12.8, 25.0, -4.6}},
        {{1.2, 21.5, 2.6}, {5.4, 26.4, 5.6}, {9.8, 28.4, 4.4}, {12.8, 24.0, 2.0}, {13.6, 18.6, 0.6}, {12.8, 14.0, -0.6}},
        {{1.2, 20.5, 2.6}, {6.0, 21.6, 5.8}, {10.8, 20.0, 5.0}, {13.6, 14.8, 3.0}, {13.8, 9.0, 1.6}, {12.8, 5.0, 0.6}},
    };
    /** Armoured segments along each rinkaku. */
    static final int SEGMENTS = 7;

    /** 1.0.7: how much bigger the rinkaku are than their control points (out from the root), and how thick at the root. */
    static final double TENDRIL_SCALE = 1.3;

    /** 1.0.7: where rinkaku {@code index}'s point is at rest, in body pixels (the right one of each pair first, as the aims go). */
    public static double[] tendrilTip(int index) {
        double[][] cp = TENDRILS[Math.max(0, Math.min(TENDRILS.length - 1, index / 2))];
        int sx = index % 2 == 0 ? -1 : 1;
        double[] root = v(sx * cp[0][0], cp[0][1], cp[0][2]);
        double[] last = cp[cp.length - 1];
        return add(root, mul(sub(v(sx * last[0], last[1], last[2]), root), TENDRIL_SCALE));
    }
    static final double TENDRIL_ROOT = 2.35;

    /** A rinkaku's radius at {@code s}: thick at the root, each armoured segment flaring at its start and narrowing to the next. */
    /** 1.0.28: the cocoon's sphere: its middle's height and its radius (body pixels: as {@code WarRules.DEFY_RADIUS}). */
    static final double ORB_CENTRE = 17.9;
    static final double ORB_RADIUS = 19.6;

    static double tendrilRadius(double s) {
        double seg = (s * SEGMENTS) % 1.0;
        return (TENDRIL_ROOT * Math.pow(1.0 - s, 0.65) + 0.12) * (1.1 - 0.24 * FurutaMesh.smooth(seg));
    }

    static void tendrils(Body body, double time, double grow, Motion motion) {
        if (grow <= 0.02) {
            return;
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int level = 0; level < TENDRILS.length; level++) {
                double[][] cp = TENDRILS[level];
                List<double[]> pts = new ArrayList<>();
                int seed = level * 2 + (sx > 0 ? 1 : 0);
                // Now and then a rinkaku jerks, as if it smelled something.
                double jolt = Math.pow(Math.max(0.0, Math.sin(time * 0.23 + seed * 1.7)), 12) * 1.4;
                for (int k = 0; k < cp.length; k++) {
                    double u = (double)k / (cp.length - 1);
                    // Out of the back as it grows; then alive: a writhe that grows toward the tip.
                    double[] root = v(sx * cp[0][0], cp[0][1], cp[0][2]);
                    double[] p = add(root, mul(sub(v(sx * cp[k][0], cp[k][1], cp[k][2]), root), TENDRIL_SCALE));
                    p = lerp(root, p, FurutaMesh.smooth(clamp01(grow * 1.3 - u * 0.3)));
                    double w = Math.pow(u, 1.3) * (1.3 + jolt) * (1.0 + 1.6 * motion.frenzy());
                    double pace = 1.0 + 1.5 * motion.frenzy();
                    p = add(p, v(Math.sin(time * 0.07 * pace + seed * 1.3 + u * 3.0) * w, Math.sin(time * 0.06 * pace + seed * 2.1 + u * 2.4) * w * 0.8,
                        Math.cos(time * 0.065 * pace + seed * 0.7 + u * 2.7) * w));
                    // The moves: out in front to seize (and down to impale), or fanned out round him.
                    double pull = Math.pow(u, 1.5);
                    if (motion.reach() > 0.0) {
                        double[] prey = v(sx * (1.2 + 0.7 * level), 22.0 - 2.0 * level - 14.0 * motion.low(), -13.0 + 1.5 * level + 6.0 * motion.low());
                        p = lerp(p, prey, motion.reach() * pull);
                    }
                    if (motion.spread() > 0.0) {
                        double a = Math.toRadians(sx * (40.0 + 32.0 * level)) + time * 0.12 * motion.frenzy();
                        double[] fan = v(Math.sin(a) * 15.0, 30.0 - 6.0 * level, Math.cos(a) * 9.0);
                        p = lerp(p, fan, motion.spread() * pull);
                    }
                    // 1.0.13: the cocoon: wound round him, each rinkaku its own coil, from over his head down to his feet.
                    if (motion.cocoon() > 0.0 && k > 0) {
                        double a = sx * (u * Math.PI * 6.0 + level * 2.1) + time * 0.03;
                        double radius = 5.4 + 0.5 * level + 0.3 * Math.sin(u * 9.0 + seed);
                        double[] coil = v(Math.sin(a) * radius, 35.0 - u * 34.0 + level * 0.9, Math.cos(a) * radius * 0.8);
                        p = lerp(p, coil, motion.cocoon() * FurutaMesh.smooth(clamp01(u * 1.6)));
                    }
                    // 1.0.28: King's Defiance: wound round him into a sphere, each rinkaku a band of it from its crown to under his
                    // feet, the bands crossing (the right ones one way, the left ones the other), breathing slowly.
                    if (motion.orb() > 0.0 && k > 0) {
                        double lat = Math.PI * (0.05 + 0.9 * clamp01(u * 1.05 + level * 0.012));
                        double a = sx * (u * Math.PI * 2.6 + level * 1.05) + sx * time * 0.015 + (sx > 0 ? 0.5 : 0.0);
                        double r = ORB_RADIUS * (1.0 + 0.03 * Math.sin(time * 0.11 + seed) + 0.04 * Math.sin(u * 11.0 + seed * 2.0));
                        double[] ball = v(Math.sin(a) * Math.sin(lat) * r, ORB_CENTRE + Math.cos(lat) * r, Math.cos(a) * Math.sin(lat) * r);
                        p = lerp(p, ball, motion.orb() * FurutaMesh.smooth(clamp01(u * 2.2)));
                    }
                    // 1.0.7: its own strike, at what it hunts.
                    double[][] aims = motion.aims();
                    if (aims != null && seed < aims.length && aims[seed] != null && k > 0) {
                        double[] aim = aims[seed];
                        p = lerp(p, v(aim[0], aim[1], aim[2]), aim[3] * Math.pow(u, 1.2));
                    }
                    pts.add(p);
                }
                KakujaModel.Spine sp = spine(pts, 56, v(0, 1, 0));
                FurutaMesh.tube(new SinkAdapter(body), sp, 0.0, 0.97, 56, 6, new FurutaMesh.Surface() {
                    @Override
                    public double radius(double s, double a) {
                        // A blade-like ridge down the back, flatter across.
                        double ridge = Math.pow(Math.max(0.0, Math.sin(a)), 6) * 0.45;
                        // (1.0.28: grown thick to close the cocoon's sphere.)
                        return tendrilRadius(s) * (1.0 + ridge - 0.15 * Math.abs(Math.cos(a))) * (1.0 + 1.3 * motion.orb());
                    }

                    @Override
                    public int colour(double s, double a) {
                        double under = Math.max(0.0, -Math.sin(a));
                        double seg = (s * SEGMENTS) % 1.0;
                        int c = mix(BLACK, CRIMSON, clamp01(under * 1.2 + s * 0.25));
                        c = mix(c, seg < 0.25 ? GREY : CHAR_L, 0.45 * Math.max(0.0, Math.sin(a)) * (1.0 - s * 0.7));
                        return mix(c, RED, clamp01(under * under * 1.2 - 0.35 + s * 0.4));
                    }
                }, 1.0);
                // A glowing vein along the underside.
                for (int i = 0; i < 40; i++) {
                    double s0 = 0.03 + 0.92 * i / 40.0;
                    double s1 = 0.03 + 0.92 * (i + 1) / 40.0;
                    double[][] f0 = sp.at(s0);
                    double[][] f1 = sp.at(s1);
                    double r0 = tendrilRadius(s0) * 0.86 + 0.04;
                    double r1 = tendrilRadius(s1) * 0.86 + 0.04;
                    double[] d0 = mul(f0[2], -r0);
                    double[] d1 = mul(f1[2], -r1);
                    double[] e0 = mul(f0[3], r0 * 0.18);
                    double[] e1 = mul(f1[3], r1 * 0.18);
                    body.glow(add(add(f0[0], d0), e0), add(add(f1[0], d1), e1), sub(add(f1[0], d1), e1), sub(add(f0[0], d0), e0), RED, RED, BLOOD, BLOOD);
                }
                // Barbs: a hooked spike off the ridge at the start of every segment, raking back toward the body, and thorns off the
                // edges, alternating.
                for (int k = 1; k < SEGMENTS; k++) {
                    double s = (double)k / SEGMENTS + 0.01;
                    double[][] f = sp.at(s);
                    double r = tendrilRadius(s);
                    double fade = 1.0 - s * 0.55;
                    double[] ridge = add(f[0], mul(f[2], r * 1.25));
                    shard(body, sub(ridge, mul(f[2], 0.4)), add(f[2], mul(f[1], -0.55)), mul(f[1], -1), (1.6 + hash(seed * 31 + k) * 1.0) * fade * grow + 0.3,
                        r * 0.9, 0.5, f[3], BLACK, k % 2 == 0 ? CHAR_L : CRIMSON, RED, 3);
                    int edge = k % 2 == 0 ? 1 : -1;
                    double[] flank = add(f[0], mul(f[3], edge * r * 0.9));
                    shard(body, flank, add(mul(f[3], edge), mul(f[1], -0.35)), mul(f[1], -1), (1.0 + hash(seed * 37 + k) * 0.7) * fade * grow + 0.2, r * 0.6, 0.4,
                        f[2], BLACK, CHAR, 0, 2);
                }
                // The point: a long hooked claw, curling under.
                double[][] tip = sp.at(0.97);
                double[][] before = sp.at(0.9);
                shard(body, before[0], sub(tip[0], before[0]), mul(tip[2], -1), 3.8 * grow + 0.4, tendrilRadius(0.9) * 2.2, 0.55, tip[3], BLACK, BLOOD, RED, 5);
            }
        }
    }

    /** {@link FurutaMesh}'s shapes drawn into a body-pixel sink. */
    static final class SinkAdapter implements UkakuModel.Sink {
        final Body body;

        SinkAdapter(Body body) {
            this.body = body;
        }

        @Override
        public void quad(double[] a, double[] b, double[] c, double[] d, double[] normal, int ca, int cb, int cc, int cd) {
            if (normal[0] == 0.0 && normal[1] == 1.0 && normal[2] == 0.0) {
                this.body.glow(a, b, c, d, ca, cb, cc, cd);
            } else {
                double[] mid = mul(add(add(a, b), add(c, d)), 0.25);
                this.body.shaded(a, b, c, d, sub(mid, normal), ca, cb, cc, cd);
            }
        }
    }

    // ================================================================== right arm
    static void rightArm(Body body, double time, double grow) {
        if (grow <= 0.02) {
            return;
        }
        double g = FurutaMesh.smooth(clamp01(grow));
        // The arm's own sleeve of plates, so no skin shows under the kakuja.
        shell(body, v(-6.0, 18.2, 0.0), v(2.55, 6.3, 2.55), 8.0, 16, 24, p -> p[1] > 11.6, p -> mottle(p, BLACK, CHAR_L, 41));
        // The pauldron: plates rising up and out over the shoulder.
        for (int k = 0; k < 7; k++) {
            double[] base = v(-5.0 - 1.1 * k, 24.6 - 0.25 * k, -2.0 + (k % 3) * 1.6);
            double[] dir = v(-0.7 - 0.1 * k, 1.0 - 0.06 * k, 0.12 * ((k % 3) - 1));
            shard(body, base, dir, v(-1, 0.2, 0), (3.2 + hash(k * 3) * 2.0) * g + 0.5, 2.6, 1.0, v(0, 0, 1), k % 2 == 0 ? BLACK : CHAR, k % 3 == 0 ? GREY : CHAR_L,
                k % 2 == 1 ? RED : 0, 4);
        }
        crescent(body, time, g);
        // The drape: broad plates hanging down and back off the back of the arm, layered like feathers.
        for (int k = 0; k < 9; k++) {
            double[] root = v(-6.8 - 1.0 * (k % 5), 24.0 - 1.6 * k, 2.0 + 0.3 * (k % 3));
            double[] dir = v(-0.25 - 0.1 * (k % 3), -1.0, 0.55 + 0.1 * (k % 2));
            double breathe = 0.04 * Math.sin(time * 0.07 + k * 0.9);
            shard(body, root, add(dir, v(0, 0, breathe)), v(0, 0, 1), (6.5 - 0.3 * k + hash(k + 190) * 1.2) * g, 4.4, 0.9, v(1, 0, 0), k % 2 == 0 ? BLACK : BLACK2,
                k % 3 == 0 ? CRIMSON : CHAR, k % 3 == 2 ? 0 : RED, 5, true);
        }
    }

    /**
     * The arm kakuja after the sheet's detail: one crescent from the shoulder to the blade's point on the ground, its outer
     * (convex) edge jagged with plates and spikes, its inner edge the steel of the scythe; layered plates on both faces point
     * down it toward the tip, with a glowing crimson line down the middle.
     */
    static final double[][] CRESCENT = {{-6.6, 25.2, 0.8}, {-11.4, 24.0, 1.4}, {-15.8, 19.6, 1.6}, {-17.6, 13.0, 0.8}, {-17.0, 6.2, -2.6}, {-15.6, -0.2,
        -7.6}};
    static final double[] CRESCENT_WIDTH = {6.2, 8.6, 8.4, 7.0, 4.6, 0.2};
    /** Where the crescent curls round: its inner edge faces this point. */
    static final double[] CURL = {-8.0, 11.0, -2.0};

    static double crescentWidth(double u) {
        double x = clamp01(u) * (CRESCENT_WIDTH.length - 1);
        int k = Math.min(CRESCENT_WIDTH.length - 2, (int)Math.floor(x));
        return CRESCENT_WIDTH[k] + (CRESCENT_WIDTH[k + 1] - CRESCENT_WIDTH[k]) * (x - k);
    }

    static void crescent(Body body, double time, double g) {
        List<double[]> cp = new ArrayList<>();
        for (int i = 0; i < CRESCENT.length; i++) {
            double breathe = 0.25 * Math.sin(time * 0.06 + i * 0.8) * i / (CRESCENT.length - 1.0);
            double[] p = add(CRESCENT[i], v(breathe, 0, breathe * 0.5));
            cp.add(lerp(CRESCENT[0], p, g));
        }
        int stations = 30;
        KakujaModel.Spine sp = spine(cp, stations, v(0, 0, 1));
        double[][][] ring = new double[stations + 1][][];
        double[][] centres = new double[stations + 1][];
        double[][] ins = new double[stations + 1][];
        double[][] faces = new double[stations + 1][];
        double[] widths = new double[stations + 1];
        double[] thick = new double[stations + 1];
        for (int i = 0; i <= stations; i++) {
            double u = (double)i / stations;
            double[][] f = sp.at(u);
            double[] t = f[1];
            double[] toward = sub(CURL, f[0]);
            double[] in = sub(toward, mul(t, dot(toward, t)));
            in = dot(in, in) < 1.0E-8 ? f[2] : normalize(in);
            double[] n = normalize(cross(t, in));
            double w = crescentWidth(u) * g;
            double th = 1.7 * Math.pow(1.0 - u, 0.6) + 0.22;
            double[] p = f[0];
            // The section: the outer edge, its shoulders, the faces, the inner (steel) edge.
            ring[i] = new double[][] {p, add(add(p, mul(in, w * 0.16)), mul(n, th)), add(add(p, mul(in, w * 0.6)), mul(n, th * 0.75)), add(p, mul(in, w)),
                sub(add(p, mul(in, w * 0.6)), mul(n, th * 0.75)), sub(add(p, mul(in, w * 0.16)), mul(n, th))};
            centres[i] = add(p, mul(in, w * 0.4));
            ins[i] = in;
            faces[i] = n;
            widths[i] = w;
            thick[i] = th;
        }
        for (int i = 0; i < stations; i++) {
            double u = (i + 0.5) / stations;
            double[] mid = lerp(centres[i], centres[i + 1], 0.5);
            double blade = clamp01((u - 0.38) / 0.2);
            int back = mix(mix(BLACK, CRIMSON, 0.55), MAROON, u);
            int plate = mottle(centres[i], BLACK, CHAR, 43);
            int inner = mix(mottle(centres[i], BLACK2, CHAR_L, 44), STEEL, blade);
            int edge = mix(CHAR_L, STEEL_L, blade);
            int[] colour = {back, plate, inner, edge, inner, plate};
            for (int j = 0; j < 6; j++) {
                int k = (j + 1) % 6;
                body.shaded(ring[i][j], ring[i + 1][j], ring[i + 1][k], ring[i][k], mid, colour[j], colour[j], colour[k], colour[k]);
            }
            // The glowing line down the middle of both faces.
            if (u > 0.04 && u < 0.9) {
                for (int side = -1; side <= 1; side += 2) {
                    double lift = 0.06;
                    double[] a0 = surface(ring[i], side, 0.28, faces[i], lift * side);
                    double[] a1 = surface(ring[i + 1], side, 0.28, faces[i + 1], lift * side);
                    double[] b0 = surface(ring[i], side, 0.42, faces[i], lift * side);
                    double[] b1 = surface(ring[i + 1], side, 0.42, faces[i + 1], lift * side);
                    body.glow(a0, a1, b1, b0, RED, RED, BLOOD, BLOOD);
                }
            }
        }
        // Layered plates on both faces, pointing down the crescent toward the tip, overlapping like scales.
        for (int k = 0; k < 12; k++) {
            double u = 0.03 + 0.042 * k;
            int i = (int)Math.round(u * stations);
            double[][] f = sp.at(u);
            for (int side = -1; side <= 1; side += 2) {
                for (int row = 0; row < 2; row++) {
                    double across = row == 0 ? 0.12 : 0.5 + 0.08 * hash(k * 7 + row);
                    double[] root = add(add(f[0], mul(ins[i], widths[i] * across)), mul(faces[i], side * thick[i] * (row == 0 ? 0.95 : 0.8)));
                    double[] dir = add(add(f[1], mul(ins[i], row == 0 ? -0.3 : -0.08)), mul(faces[i], side * 0.22));
                    double length = (6.4 - 3.2 * u + hash(k * 11 + side + row * 3) * 1.6) * g;
                    double width = (row == 0 ? 3.8 : 4.4) * (1.0 - 0.4 * u);
                    int seed = k * 4 + row * 2 + (side + 1) / 2;
                    int base = seed % 3 == 0 ? BLACK2 : BLACK;
                    int tip = seed % 5 == 0 ? GREY : seed % 3 == 2 ? CRIMSON : CHAR;
                    shard(body, root, dir, mul(faces[i], -side), length * 0.85, width, 0.85, ins[i], base, tip, seed % 3 == 2 ? 0 : RED, 4, true);
                }
            }
        }
        // Jagged spikes along the outer edge, out and down.
        for (int k = 0; k < 12; k++) {
            double u = 0.04 + 0.055 * k;
            int i = (int)Math.round(u * stations);
            double[][] f = sp.at(u);
            double[] dir = add(mul(ins[i], -1.0), mul(f[1], 0.9 + 0.4 * hash(k + 160)));
            shard(body, f[0], dir, f[1], (1.6 + hash(k + 170) * 2.2) * g, 1.7, 0.8, faces[i], BLACK, k % 3 == 0 ? CRIMSON : CHAR, 0, 3);
        }
    }

    /** A point on a crescent face: {@code across} of the way from the outer edge to the inner one, on face {@code side}, lifted. */
    static double[] surface(double[][] ring, int side, double across, double[] face, double lift) {
        double[] outer = side > 0 ? ring[1] : ring[5];
        double[] inner = side > 0 ? ring[2] : ring[4];
        double[] p = across < 0.16 ? lerp(ring[0], outer, across / 0.16) : lerp(outer, inner, (across - 0.16) / 0.44);
        return add(p, mul(face, lift));
    }

    // ================================================================== left arm and legs
    static void leftArm(Body body, double time, double grow) {
        if (grow <= 0.02) {
            return;
        }
        double g = FurutaMesh.smooth(clamp01(grow));
        for (int k = 0; k < 5; k++) {
            double[] base = v(4.6 + 0.9 * k, 25.0 - 0.75 * k, -2.4 + 0.2 * k);
            shard(body, base, v(1.0, -0.55 - 0.15 * k, 0.0), v(0, -1, 0), (4.2 - 0.3 * k) * g, 5.4 - 0.4 * k, 0.95, v(0, 0, 1), k % 2 == 0 ? BLACK : CHAR, CHAR_L,
                0, 4);
        }
        // The sleeve over the upper arm, then ragged strips with red ends to mid forearm.
        shell(body, v(6.0, 19.4, 0.0), v(2.55, 3.1, 2.55), 8.0, 10, 24, p -> p[1] > 16.4, p -> mottle(p, WarCloth.CLOTH_DEEP, WarCloth.CLOTH_SHEEN, 51));
        for (int k = 0; k < 6; k++) {
            double x = 4.1 + 0.8 * k;
            double length = 1.6 + hash(k + 90) * 2.4;
            double sway = 0.25 * Math.sin(time * 0.08 + k);
            List<double[]> pts = List.of(v(x, 16.8, -2.4), v(x + sway * 0.3, 16.8 - length * 0.5, -2.5), v(x + sway, 16.8 - length, -2.55));
            ribbon(body, new ArrayList<>(pts), 0.8, 0.3, v(1, 0, 0), k + 100, 1);
        }
    }

    static void leg(Body body, int sx, double grow) {
        if (grow <= 0.02) {
            return;
        }
        double cx = sx * 1.95;
        // Wraps round the thigh and the shin; the knee and a patch of the shin stay bare.
        shell(body, v(cx, 9.75, 0.0), v(2.5, 2.3, 2.5), 8.0, 10, 24, p -> true, p -> mottle(p, WarCloth.CLOTH_DEEP, WarCloth.CLOTH_SHEEN, 61 + sx));
        shell(body, v(cx, 3.9, 0.0), v(2.5, 1.6, 2.5), 8.0, 8, 24, p -> !(p[2] < -1.2 && Math.abs(p[0] - cx - 0.6 * sx) < 0.9), p -> mottle(p,
            WarCloth.CLOTH_DEEP, WarCloth.CLOTH_SHEEN, 63 + sx));
        // The boot, its wider cuff, blood run down from the knee.
        shell(body, v(cx, 1.1, -0.1), v(2.6, 1.15, 2.75), 8.0, 8, 24, p -> true, p -> mottle(p, WarCloth.CLOTH_DEEP, WarCloth.CLOTH, 65 + sx));
        shell(body, v(cx, 2.4, 0.0), v(2.85, 0.45, 2.85), 10.0, 4, 24, p -> true, p -> mottle(p, WarCloth.CLOTH, WarCloth.CLOTH_SHEEN, 67 + sx));
        for (int k = 0; k < 3; k++) {
            double x = cx + (hash(k + sx * 10 + 20) - 0.5) * 2.8;
            double y0 = 7.6;
            double y1 = 7.6 - 1.0 - hash(k + sx * 10 + 30) * 1.6;
            double z = -2.5;
            body.shaded(v(x - 0.22, y0, z), v(x + 0.22, y0, z), v(x + 0.18, y1, z), v(x - 0.18, y1, z), v(x, (y0 + y1) / 2, 0), RED, RED, CRIMSON, CRIMSON);
        }
    }
}
