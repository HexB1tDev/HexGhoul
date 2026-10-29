package com.hex.kagune.control.pale;

import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * 6.2.4: the Owl DNA kakuja, the One-Eyed Owl, as real 3D geometry (pure, tested), rebuilt to the user's reference (a gaunt,
 * hunched creature in a tattered robe):
 * <ul>
 * <li><b>four kagune limbs</b>: two off each shoulder, like a spider's legs or a wing's fingers: an upper of dark crimson flesh,
 * plated like bark, arching up off her back, a knuckle where it bends, then a long glossy navy spike of a blade, tapering to a
 * needle point; the outer pair is the long one every move swings ({@link Look#right}, {@link Look#left}), the inner pair rises
 * higher off her shoulder blades and follows it;</li>
 * <li><b>the mask</b>: long, bone white, tapering to a pointed snout of a chin, a crown of horns over it, one great red eye in the
 * middle of the brow, two black slashes running out from it each side, a grin of teeth across the bottom of it, a dark cowl
 * behind;</li>
 * <li><b>the crest</b>: a burst of glowing red flares and white bone spikes off the nape of her neck;</li>
 * <li><b>her body under it</b>: a long, tattered dark robe from her ribs to her ankles that follows her legs, bone bands round her
 * chest and arms, long curled claws off her hands and bird's talons off her feet.</li>
 * </ul>
 * It grows in stages ({@link Look#grown}, 0..1): the crest ignites, the limbs tear out and unfold, the blades slide out of the
 * knuckles, the robe spills down, the mask closes over her face from the eye outward and the crown and claws come last.
 *
 * <p>Everything is in rig space (blocks: x to her right, y up from the feet, z forward), placed by {@link Look}. It is cel-shaded
 * (a bright, a base and a shadow tone, hard-edged, from a light above and in front of her) and baked into the vertex colours; every
 * quad is given an upward normal so the game's own lighting adds nothing on top.
 */
public final class OwlModel {
    // ---- The limbs' uppers: dark crimson flesh, plated like bark, its ridges lighter, the cracks between them near black.
    public static final int MUSCLE = 0xFF8A1622;
    public static final int MUSCLE_HI = 0xFFC8343F;
    public static final int MUSCLE_DARK = 0xFF35060C;
    public static final int FIBRE = 0xFF1A0306;
    static final int RIDGE = 0xFFE0545A;
    // ---- The blades: glossy navy, near black in shadow, a hard pale gloss where the light glances along them, the point black.
    public static final int BLADE = 0xFF27305A;
    public static final int BLADE_HI = 0xFF6A7CC4;
    public static final int BLADE_DARK = 0xFF0A0C1C;
    public static final int BLADE_TIP = 0xFF040508;
    static final int GLOSS = 0xFFC4D0FF;
    // ---- The crest: burning red.
    public static final int CRYSTAL = 0xFFE8121E;
    public static final int CRYSTAL_HOT = 0xFFFF9070;
    public static final int CRYSTAL_DARK = 0xFF5E0410;
    // ---- Bone: the crest's spikes, the bands.
    static final int BONE = 0xFFE0D9CB;
    static final int BONE_HI = 0xFFFFFFFF;
    static final int BONE_DARK = 0xFF8A8275;
    static final int BONE_TIP = 0xFF2E2A26;
    // ---- The bands round her arms: filthy wrappings.
    static final int WRAP = 0xFF7A6E62;
    static final int WRAP_HI = 0xFFA89A88;
    static final int WRAP_DARK = 0xFF3A322C;
    // ---- The mask: bone white, grey in its hollows, its marks black; the eye burning red.
    public static final int MASK = 0xFFE8E4DA;
    public static final int MASK_HI = 0xFFFFFFFF;
    public static final int MASK_DARK = 0xFF7E786E;
    public static final int MASK_LINE = 0xFF0C0A0C;
    public static final int SOCKET = 0xFF040305;
    public static final int IRIS = 0xFFE8101E;
    public static final int IRIS_HOT = 0xFFFFA088;
    static final int TOOTH = 0xFFF2EEE4;
    static final int TOOTH_DARK = 0xFFA49C8E;
    // ---- The cowl behind the mask.
    static final int HOOD = 0xFF241E22;
    static final int HOOD_HI = 0xFF463E44;
    static final int HOOD_DARK = 0xFF0E0B0D;
    // ---- The robe: near black, its folds lighter, blood dark in it near the hem, a pale ragged trim at its top.
    static final int ROBE = 0xFF2A1F21;
    static final int ROBE_HI = 0xFF4E3F41;
    static final int ROBE_DARK = 0xFF0D090A;
    static final int ROBE_IN = 0xFF140D0E;
    static final int STAIN = 0xFF4E0E14;
    static final int TRIM = 0xFFB8AD9C;
    // ---- The claws and talons: grey hide going to black horn.
    static final int CLAW = 0xFF4A4341;
    static final int CLAW_HI = 0xFF8C807A;
    static final int CLAW_DARK = 0xFF1A1514;
    static final int CLAW_TIP = 0xFF060505;

    /** The outer limbs (blocks): the upper, the blade. */
    public static final double UPPER = 0.9;
    public static final double FORE = 1.5;
    /** The inner limbs (blocks): the upper, the blade. */
    public static final double INNER_UPPER = 0.7;
    public static final double INNER_FORE = 1.2;
    /** The outer upper's half-thickness where it leaves her back, and the outer blade's at its widest (blocks). */
    public static final double ARM_RADIUS = 0.165;
    public static final double BLADE_WIDTH = 0.12;
    /** How high (of its length) an upper arches up off the line from its root to its knuckle. */
    static final double ARCH = 0.13;

    /** A colour drawn at full brightness whatever the light where it is (the eye, the crest): alpha 0xFE. */
    public static int glow(int c) {
        return (c & 0x00FFFFFF) | 0xFE000000;
    }

    /** Whether a colour is one drawn at full brightness ({@link #glow}). */
    public static boolean glowing(int c) {
        return (c >>> 24) == 0xFE;
    }

    /** The light the cel shading is from (rig space, unit): above her, in front, a little to her left. */
    static final double[] LIGHT = normalize(new double[] {-0.5, 0.8, 0.35});
    /** Every quad's normal: straight up, so the game's lighting is the same on all of it (the shading is baked). */
    static final double[] UP = {0.0, 1.0, 0.0};

    private OwlModel() {
    }

    /**
     * A part's own space placed in rig space: {@code o} where its origin is, and how far (blocks) one of its pixels runs along each
     * of its axes (x to the player's left, y down, z backward, as the player model has them).
     */
    public record Frame(double[] o, double[] x, double[] y, double[] z) {
        public double[] at(double px, double py, double pz) {
            return new double[] {this.o[0] + this.x[0] * px + this.y[0] * py + this.z[0] * pz,
                this.o[1] + this.x[1] * px + this.y[1] * py + this.z[1] * pz, this.o[2] + this.x[2] * px + this.y[2] * py + this.z[2] * pz};
        }

        /** A direction given in the part's pixels, in rig space, unit length. */
        public double[] dir(double dx, double dy, double dz) {
            return normalize(new double[] {this.x[0] * dx + this.y[0] * dy + this.z[0] * dz, this.x[1] * dx + this.y[1] * dy + this.z[1] * dz,
                this.x[2] * dx + this.y[2] * dy + this.z[2] * dz});
        }

        /** How long (blocks) one of its pixels is. */
        public double pixel() {
            return Math.sqrt(dot(this.x, this.x));
        }
    }

    /**
     * One limb in rig space: the root, the knuckle (the "elbow"), the point; {@code front} the side its upper faces (unit, square to
     * the upper), {@code bow} how far (blocks) the blade bows out along its length, and which way ({@code bend}, unit).
     */
    public record Arm(double[] shoulder, double[] elbow, double[] tip, double[] front, double bow, double[] bend) {
    }

    /**
     * 6.2.4: her own body as posed, for what the kakuja puts on it: frames at the bottom of each hand (where the claws grow), at each
     * upper arm and wrist (the bands), at each hip and the sole of each foot (the robe, the talons); how much thicker her arms are
     * drawn ({@code armScale}) and how bulked her chest is ({@code bulk}).
     */
    public record Body(Frame rightHand, Frame leftHand, Frame rightUpper, Frame leftUpper, Frame rightWrist, Frame leftWrist, Frame rightHip,
                       Frame leftHip, Frame rightFoot, Frame leftFoot, double armScale, double bulk) {
    }

    /**
     * Everything that places the kakuja: her posed head and torso, the outer limbs, which eye her kakugan is in and where her eyes are
     * (head pixels; the mask's one eye is in the middle of its brow whatever they are), the time (ticks), how far it has grown out
     * (0 none, 1 all), the inner limbs, and her body (null: none of what goes on it).
     */
    public record Look(Frame head, Frame torso, Arm right, Arm left, boolean rightEye, double eyeX, double eyeY, double otherX, double otherY,
                       double time, double grown, Arm rightInner, Arm leftInner, Body body) {
    }

    /** All of it. */
    public static void mesh(UkakuModel.Sink sink, Look look) {
        if (look.grown() <= 0.01) {
            return;
        }
        kagune(sink, look);
        mask(sink, look);
        body(sink, look);
    }

    /** The crest and the four limbs (what first person draws, with the mask; the rest is out of her sight under her). */
    public static void kagune(UkakuModel.Sink sink, Look look) {
        if (look.grown() <= 0.01) {
            return;
        }
        crest(sink, look, true);
        if (look.rightInner() != null) {
            limb(sink, look, look.rightInner(), 1, true);
        }
        if (look.leftInner() != null) {
            limb(sink, look, look.leftInner(), -1, true);
        }
        limb(sink, look, look.right(), 1, false);
        limb(sink, look, look.left(), -1, false);
    }

    /** The mask and everything on her head (asked alone by the tests: it sits on her head, never in it). */
    public static void head(UkakuModel.Sink sink, Look look) {
        if (look.grown() > 0.01) {
            mask(sink, look);
        }
    }

    /** What reaches out from her: the crest's flares and spikes and all four blades (asked alone by the tests: never through her). */
    public static void reaching(UkakuModel.Sink sink, Look look) {
        if (look.grown() <= 0.01) {
            return;
        }
        crest(sink, look, false);
        for (int i = 0; i < 4; i++) {
            Arm arm = i == 0 ? look.right() : i == 1 ? look.left() : i == 2 ? look.rightInner() : look.leftInner();
            if (arm == null) {
                continue;
            }
            boolean inner = i >= 2;
            double gu = upperGrown(look.grown(), inner);
            double gb = bladeGrown(look.grown(), inner);
            if (gb > 0.01) {
                blade(sink, lerp(arm.shoulder(), arm.elbow(), gu), arm, gb, inner);
            }
        }
    }

    /** What goes on her own body: the robe, the bands, the claws and talons (third person only). */
    public static void body(UkakuModel.Sink sink, Look look) {
        Body b = look.body();
        if (b == null || look.grown() <= 0.01) {
            return;
        }
        robe(sink, look);
        bands(sink, look);
        for (int side : new int[] {1, -1}) {
            claws(sink, look, side > 0 ? b.rightHand() : b.leftHand(), side);
            talons(sink, look, side > 0 ? b.rightFoot() : b.leftFoot());
        }
    }

    // ------------------------------------------------------------------ how far each part has grown

    /** How far the crest has ignited (0..1) at {@code g}: first. */
    static double crestGrown(double g) {
        return smooth(g / 0.3);
    }

    /** How far a limb's upper has torn out (0..1), the inner pair a little after the outer. */
    public static double upperGrown(double g, boolean inner) {
        return smooth((g - (inner ? 0.12 : 0.04)) / 0.42);
    }

    /** How far a limb's blade has slid out of its knuckle (0..1), once its upper is most of the way out. */
    public static double bladeGrown(double g, boolean inner) {
        return smooth((g - (inner ? 0.38 : 0.3)) / 0.5);
    }

    /** How far the mask has closed over her face from the eye (0..1). */
    static double maskGrown(double g) {
        return smooth((g - 0.3) / 0.5);
    }

    /** How far the robe has spilled down (0..1). */
    static double robeGrown(double g) {
        return smooth((g - 0.22) / 0.6);
    }

    /** The claws, talons, bands and the crown of the mask: last. */
    static double lastGrown(double g) {
        return smooth((g - 0.62) / 0.36);
    }

    // ------------------------------------------------------------------ shading

    /** A colour cel-shaded for a surface facing {@code n}: its bright, base or shadow tone, hard-edged. */
    static int cel(double[] n, int hi, int base, int dark) {
        double t = dot(n, LIGHT);
        double up = smooth((t - 0.7) / 0.05);
        double down = smooth((0.02 - t) / 0.06);
        int c = mix(base, hi, up * 0.8);
        return mix(c, dark, down);
    }

    static double smooth(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return c * c * (3.0 - 2.0 * c);
    }

    /**
     * A surface given as a grid of points, their outward normals and colours (already shaded): quads between them, each with the
     * upward normal ({@link #UP}); {@code wrap} joins the last column to the first.
     */
    static void grid(UkakuModel.Sink sink, double[][][] p, int[][] c) {
        for (int i = 0; i + 1 < p.length; i++) {
            for (int j = 0; j + 1 < p[i].length; j++) {
                sink.quad(p[i][j], p[i][j + 1], p[i + 1][j + 1], p[i + 1][j], UP, c[i][j], c[i][j + 1], c[i + 1][j + 1], c[i + 1][j]);
            }
        }
    }

    /** The same grid drawn from both sides (a sheet: a spike's blade, the robe). */
    static void sheet(UkakuModel.Sink sink, double[][][] p, int[][] c, int[][] back) {
        grid(sink, p, c);
        for (int i = 0; i + 1 < p.length; i++) {
            for (int j = 0; j + 1 < p[i].length; j++) {
                sink.quad(p[i][j], p[i + 1][j], p[i + 1][j + 1], p[i][j + 1], UP, back[i][j], back[i + 1][j], back[i + 1][j + 1], back[i][j + 1]);
            }
        }
    }

    /** A shading of a surface: its tones, and anything drawn on it, at a point and its place on the surface. */
    interface Paint {
        int at(double[] point, double[] normal, double u, double v);
    }

    /**
     * An ellipsoid (rig space): centre, its three unit axes and radii along them, {@code lat} by {@code lon}, painted; {@code bulge}
     * adds to the radius at each place on it.
     */
    static void ellipsoid(UkakuModel.Sink sink, double[] c, double[] a1, double[] a2, double[] a3, double r1, double r2, double r3, int lat, int lon,
                          Paint paint, java.util.function.DoubleBinaryOperator bulge) {
        blob(sink, c, a1, a2, a3, r1, r2, r3, 2.0, lat, lon, paint, bulge);
    }

    /** The same, squared off by {@code n} (2 an ellipsoid; more, boxier: to cover a box). */
    static void blob(UkakuModel.Sink sink, double[] c, double[] a1, double[] a2, double[] a3, double r1, double r2, double r3, double n, int lat,
                     int lon, Paint paint, java.util.function.DoubleBinaryOperator bulge) {
        double[][][] p = new double[lat + 1][lon + 1][];
        int[][] col = new int[lat + 1][lon + 1];
        for (int i = 0; i <= lat; i++) {
            double u = Math.PI * i / lat;
            for (int j = 0; j <= lon; j++) {
                double v = 2.0 * Math.PI * j / lon;
                double s1 = squash(Math.cos(u), n);
                double s2 = squash(Math.sin(u) * Math.cos(v), n);
                double s3 = squash(Math.sin(u) * Math.sin(v), n);
                double k = 1.0 + bulge.applyAsDouble(u, v);
                p[i][j] = add(c, add(mul(a1, s1 * r1 * k), add(mul(a2, s2 * r2 * k), mul(a3, s3 * r3 * k))));
                // Its normal: the gradient of |x/r|^n along each axis.
                double[] nrm = normalize(add(mul(a1, grad(s1, n) / r1), add(mul(a2, grad(s2, n) / r2), mul(a3, grad(s3, n) / r3))));
                col[i][j] = paint.at(p[i][j], nrm, u, v);
            }
        }
        grid(sink, p, col);
    }

    /**
     * A tube along {@code spine} (rig space), its cross-section an ellipse (half-widths from {@code radii} at each place along it:
     * across {@code side}, and square to it), painted; closed at both ends.
     */
    static void tube(UkakuModel.Sink sink, double[][] spine, double[][] side, java.util.function.DoubleFunction<double[]> radii, int sides, Paint paint) {
        loft(sink, spine, side, (s, angle) -> {
            double[] r = radii.apply(s);
            return new double[] {Math.cos(angle) * r[0], Math.sin(angle) * r[1]};
        }, sides, paint, true, true);
    }

    /** A cross-section of a lofted surface: the point at {@code angle} round it, {@code s} along it, as (across its side, square to it). */
    interface Section {
        double[] at(double s, double angle);
    }

    /**
     * 6.2.4: a surface lofted along {@code spine} (rig space): at each place along it the cross-section {@code section} gives, laid
     * across {@code side} (and square to it and to the spine), its normals from its own neighbours, painted; closed at either end
     * if asked.
     */
    static void loft(UkakuModel.Sink sink, double[][] spine, double[][] side, Section section, int sides, Paint paint, boolean capStart, boolean capEnd) {
        int n = spine.length - 1;
        double[][][] p = new double[n + 1][sides + 1][];
        for (int i = 0; i <= n; i++) {
            double s = i / (double)n;
            double[] t = normalize(sub(spine[Math.min(n, i + 1)], spine[Math.max(0, i - 1)]));
            double[] a = normalize(sub(side[i], mul(t, dot(side[i], t))));
            double[] b = cross(t, a);
            for (int k = 0; k <= sides; k++) {
                double angle = 2.0 * Math.PI * (k % sides) / sides;
                double[] q = section.at(s, angle);
                p[i][k] = add(spine[i], add(mul(a, q[0]), mul(b, q[1])));
            }
        }
        int[][] col = new int[n + 1][sides + 1];
        for (int i = 0; i <= n; i++) {
            double s = i / (double)n;
            for (int k = 0; k <= sides; k++) {
                double angle = 2.0 * Math.PI * (k % sides) / sides;
                double[] du = sub(p[i][(k + 1) % sides], p[i][(k + sides - 1) % sides]);
                double[] dv = sub(p[Math.min(n, i + 1)][k], p[Math.max(0, i - 1)][k]);
                double[] nrm = normalize(cross(du, dv));
                if (dot(nrm, sub(p[i][k], spine[i])) < 0.0) {
                    nrm = mul(nrm, -1.0);
                }
                col[i][k] = paint.at(p[i][k], nrm, s, angle);
            }
        }
        grid(sink, p, col);
        if (capStart) {
            capOff(sink, p[0], spine[0], col[0][0]);
        }
        if (capEnd) {
            capOff(sink, p[n], spine[n], col[n][0]);
        }
    }

    static double squash(double x, double n) {
        return Math.signum(x) * Math.pow(Math.abs(x), 2.0 / n);
    }

    static double grad(double x, double n) {
        return Math.signum(x) * Math.pow(Math.abs(x), n - 1.0);
    }

    static void capOff(UkakuModel.Sink sink, double[][] ring, double[] centre, int colour) {
        for (int k = 0; k + 1 < ring.length; k++) {
            sink.quad(centre, ring[k], ring[k + 1], ring[k + 1], UP, colour, colour, colour, colour);
            sink.quad(centre, ring[k + 1], ring[k], ring[k], UP, colour, colour, colour, colour);
        }
    }

    /** Points along a quadratic curve from {@code a} through the pull of {@code m} to {@code b}, {@code n} segments. */
    static double[][] curve(double[] a, double[] m, double[] b, int n) {
        double[][] out = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double u = i / (double)n;
            out[i] = add(add(mul(a, (1.0 - u) * (1.0 - u)), mul(m, 2.0 * u * (1.0 - u))), mul(b, u * u));
        }
        return out;
    }

    /** The same direction at every point of a spine. */
    static double[][] same(double[] d, int count) {
        double[][] out = new double[count][];
        java.util.Arrays.fill(out, d);
        return out;
    }

    /** Some direction square to {@code d} (unit). */
    static double[] square(double[] d) {
        double[] a = Math.abs(d[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        return normalize(sub(a, mul(d, dot(a, d))));
    }

    /**
     * 6.2.4: a horn, claw or spike: a curved cone from {@code root} along {@code dir} ({@code length} blocks), curling toward
     * {@code curl} (blocks at its point), {@code r} its radius at the root, tapering to a point; {@code paint} along it (u 0..1).
     */
    static void spike(UkakuModel.Sink sink, double[] root, double[] dir, double length, double r, double[] curl, int rings, int sides, Paint paint) {
        if (length < 0.005) {
            return;
        }
        double[] d = normalize(dir);
        double[][] spine = new double[rings + 1][];
        for (int i = 0; i <= rings; i++) {
            double u = i / (double)rings;
            spine[i] = add(root, add(mul(d, length * u), mul(curl, u * u)));
        }
        loft(sink, spine, same(square(d), rings + 1), (s, angle) -> {
            double rr = r * Math.pow(1.0 - s, 0.85) + 0.002;
            return new double[] {Math.cos(angle) * rr, Math.sin(angle) * rr};
        }, sides, paint, true, false);
    }

    // ------------------------------------------------------------------ the limbs

    /**
     * One limb ({@code side} 1 her right, -1 her left; {@code inner} the shorter pair): its upper arching up off its root to the
     * knuckle, plated dark crimson flesh with quills along its top, the knuckle, and the blade out of it.
     */
    static void limb(UkakuModel.Sink sink, Look look, Arm arm, int side, boolean inner) {
        limb(sink, look, arm, side, inner, 1.0);
    }

    /** The same, in {@code detail} as many segments along and round it (6.2.4: the Owl creature's four drawn a little coarser). */
    static void limb(UkakuModel.Sink sink, Look look, Arm arm, int side, boolean inner, double detail) {
        double g = look.grown();
        double gu = upperGrown(g, inner);
        if (gu <= 0.01) {
            return;
        }
        double scale = inner ? 0.8 : 1.0;
        Frame t = look.torso();
        double[] s = arm.shoulder();
        double[] e = lerp(s, arm.elbow(), gu);
        double[][] spine = upper(t, s, e, side, (int)Math.round(14 * detail));
        double[] top = archWay(t, s, e, side);
        double[] across = normalize(cross(normalize(sub(e, s)), top));
        double r = ARM_RADIUS * scale * (0.45 + 0.55 * gu);
        // Wide across, flatter top to bottom: a wing's bone more than an arm; swelling where it leaves her back and at the knuckle.
        loft(sink, spine, same(across, spine.length), (q, angle) -> {
            double root = 1.0 + 0.45 * Math.pow(Math.max(0.0, 1.0 - q / 0.14), 2.0);
            double knuckle = 0.16 * Math.exp(-Math.pow((q - 0.95) / 0.07, 2.0));
            double rr = r * (1.04 - 0.3 * q + knuckle) * root;
            // Plates standing a little proud of it, as bark does.
            double plate = 1.0 + 0.045 * Math.sin(q * 19.0 + angle * 2.0) * Math.sin(angle * 3.0 + q * 7.0);
            return new double[] {Math.cos(angle) * rr * 1.22 * plate, Math.sin(angle) * rr * 0.92 * plate};
        }, (int)Math.round((inner ? 12 : 14) * detail), (p, n, q, angle) -> bark(n, q, angle), true, false);
        // Quills along its top, raked toward the knuckle.
        double[] along = normalize(sub(e, s));
        double[] up = normalize(cross(across, along));
        if (dot(up, top) < 0.0) {
            up = mul(up, -1.0);
        }
        double[] q0 = {0.3, 0.48, 0.66};
        for (int i = 0; i < q0.length; i++) {
            double[] at = spine[(int)Math.round(q0[i] * (spine.length - 1))];
            double rr = r * (1.04 - 0.3 * q0[i]) * 0.85;
            double[] root = add(at, mul(up, rr));
            double[] dir = normalize(add(up, mul(along, 0.9)));
            double length = (0.13 + 0.05 * Math.sin(i * 2.3 + side)) * scale * gu;
            spike(sink, root, dir, length, 0.035 * scale, mul(along, 0.03), 3, 4,
                (p, n, u, v) -> mix(cel(n, MUSCLE_HI, MUSCLE, MUSCLE_DARK), BLADE_DARK, smooth((u - 0.35) / 0.5)));
        }
        // The knuckle: dark, going navy where the blade comes out of it.
        double kr = r * 0.92;
        double[] bAxis = normalize(sub(arm.tip(), arm.elbow()));
        ellipsoid(sink, e, bAxis, across, normalize(cross(bAxis, across)), kr, kr, kr, (int)Math.round(7 * detail), (int)Math.round(10 * detail),
            (p, n, u, v) -> mix(cel(n, MUSCLE_HI, MUSCLE, MUSCLE_DARK), cel(n, BLADE_HI, BLADE, BLADE_DARK), smooth((Math.cos(u) + 0.2) / 0.6) * 0.8),
            (u, v) -> 0.06 * Math.sin(u * 5.0 + v * 3.0));
        double gb = bladeGrown(g, inner);
        if (gb > 0.01) {
            blade(sink, e, arm, gb, inner, detail);
        }
    }

    /** The way (unit, square to it) an upper from {@code s} to {@code e} arches: up off her back, a little out to her {@code side}. */
    static double[] archWay(Frame t, double[] s, double[] e, int side) {
        double[] c = normalize(sub(e, s));
        double[] want = add(mul(t.dir(0.0, -1.0, 0.0), 0.85), mul(t.dir(-side, 0.0, 0.0), 0.35));
        double[] w = sub(want, mul(c, dot(want, c)));
        double l = Math.sqrt(dot(w, w));
        return l < 1.0E-6 ? square(c) : mul(w, 1.0 / l);
    }

    /**
     * 6.2.4: an upper's spine (rig space, {@code n} segments) from its root {@code s} to its knuckle {@code e}: arching up off the line
     * between them ({@link #ARCH} of its length at the top of the arch, less as the line itself stands up or hangs straight down).
     */
    public static double[][] upper(Frame t, double[] s, double[] e, int side, int n) {
        double[] c = sub(e, s);
        double length = Math.sqrt(dot(c, c));
        double standing = Math.abs(dot(normalize(c), t.dir(0.0, -1.0, 0.0)));
        double h = ARCH * length * (1.0 - 0.7 * standing * standing);
        double[] m = add(lerp(s, e, 0.5), mul(archWay(t, s, e, side), 2.0 * h));
        return curve(s, m, e, n);
    }

    /** Bark: dark crimson plates, their ridges catching the light, black cracks between them, darker underneath. */
    static int bark(double[] n, double q, double angle) {
        int c = cel(n, MUSCLE_HI, MUSCLE, MUSCLE_DARK);
        double plate = Math.sin(angle * 4.0 + 1.4 * Math.sin(q * 9.0 + angle));
        double crack = Math.sin(q * 23.0 + 2.2 * Math.sin(angle * 3.0 + q * 4.0));
        double lit = Math.max(0.0, dot(n, LIGHT));
        c = mix(c, RIDGE, 0.55 * smooth((plate - 0.45) / 0.4) * lit);
        if (crack > 0.82 || plate < -0.8) {
            c = mix(c, FIBRE, 0.75);
        }
        // Darkening into navy toward the knuckle, where the blade grows out of it.
        return mix(c, BLADE_DARK, 0.5 * smooth((q - 0.8) / 0.2));
    }

    /**
     * A blade grown {@code grow} of the way out of the knuckle {@code e}: a long navy spike, thick out of the knuckle, a lens across
     * (an edge on the outside of its bow, its back rounder), bowed, tapering to a needle point; a ragged collar of red flesh where it
     * leaves the knuckle, a hard gloss along it, the point black.
     */
    static void blade(UkakuModel.Sink sink, double[] e, Arm arm, double grow, boolean inner) {
        blade(sink, e, arm, grow, inner, 1.0);
    }

    static void blade(UkakuModel.Sink sink, double[] e, Arm arm, double grow, boolean inner, double detail) {
        double[] tip = lerp(e, arm.tip(), grow);
        double[] along = normalize(sub(tip, e));
        double[] bend = normalize(sub(arm.bend(), mul(along, dot(arm.bend(), along))));
        double width = BLADE_WIDTH * (inner ? 0.8 : 1.0) * (0.5 + 0.5 * grow);
        int rings = (int)Math.round((inner ? 16 : 20) * detail);
        double[][] spine = new double[rings + 1][];
        for (int i = 0; i <= rings; i++) {
            double v = i / (double)rings;
            spine[i] = add(lerp(e, tip, v), mul(bend, arm.bow() * grow * 3.6 * v * (1.0 - v) * (0.55 + 0.9 * v)));
        }
        loft(sink, spine, same(bend, rings + 1), (v, angle) -> {
            double w = width * (1.0 + 0.4 * Math.exp(-Math.pow((v - 0.1) / 0.1, 2.0))) * Math.pow(Math.max(0.0, 1.0 - v), 0.75) + 0.003;
            double c = Math.cos(angle);
            // The edge (c 1) out on the bow's outside, thin as a wedge; the back rounder and a little thicker.
            double x = c > 0.0 ? w * c : 0.7 * w * c;
            double y = 0.62 * w * Math.sin(angle) * (1.0 - 0.75 * Math.max(0.0, c));
            return new double[] {x, y};
        }, 8, (p, n, v, angle) -> {
            int colour = cel(n, BLADE_HI, BLADE, BLADE_DARK);
            // The hard gloss where the light glances along it.
            colour = mix(colour, GLOSS, 0.85 * smooth((dot(n, LIGHT) - 0.9) / 0.05));
            // The ragged collar of red flesh it slides out of.
            double collar = 0.07 + 0.05 * Math.abs(Math.sin(angle * 2.5 + 1.0));
            colour = mix(cel(n, MUSCLE_HI, MUSCLE, MUSCLE_DARK), colour, smooth((v - collar) / 0.025));
            // The point black.
            return mix(colour, BLADE_TIP, 0.85 * smooth((v - 0.72) / 0.28));
        }, true, true);
    }

    // ------------------------------------------------------------------ the crest

    /** Where (torso pixels) the crest bursts out: the nape of her neck, on her upper back, under the back of her head. */
    static final double[] CREST_ROOT = {0.0, 3.6, 4.1};

    /**
     * The crest's flares (torso pixels: which way, x to her left, y down, z back; how long, blocks; 1 for a bone spike, 0 a flare):
     * a burst all round in the plane of her back and out behind her; the ones that rise lean well back, clear of her head however
     * she holds it.
     */
    static final double[][] CREST = crestRays();

    static double[][] crestRays() {
        java.util.List<double[]> rays = new java.util.ArrayList<>();
        int flares = 15;
        for (int k = 0; k < flares; k++) {
            // Round her back from straight up (0) through her sides, skipping straight down.
            double phi = -2.45 + 4.9 * k / (flares - 1.0) + 0.11 * Math.sin(k * 2.7);
            double upness = Math.cos(phi);
            double back = 0.6 + 0.75 * Math.max(0.0, upness) + 0.15 * Math.sin(k * 1.9);
            double length = (0.5 + 0.33 * Math.max(0.0, upness) + 0.09 * Math.sin(k * 3.1)) * (upness < -0.3 ? 0.65 : 1.0);
            rays.add(new double[] {Math.sin(phi), -upness, back, length, 0.0});
        }
        int bones = 8;
        for (int k = 0; k < bones; k++) {
            double phi = -2.2 + 4.4 * (k + 0.5) / bones;
            double upness = Math.cos(phi);
            rays.add(new double[] {0.8 * Math.sin(phi), -0.8 * upness, 0.95 + 0.4 * Math.max(0.0, upness), 0.38 + 0.16 * Math.max(0.0, upness), 1.0});
        }
        // A tight core of short flares straight out behind.
        for (int k = 0; k < 5; k++) {
            double phi = 2.0 * Math.PI * k / 5.0 + 0.4;
            rays.add(new double[] {0.35 * Math.sin(phi), -0.35 * Math.cos(phi), 1.0, 0.24, 0.0});
        }
        return rays.toArray(new double[0][]);
    }

    /** The crest: the glowing knot it bursts out of on her back ({@code withKnot}), its flares and spikes. */
    static void crest(UkakuModel.Sink sink, Look look, boolean withKnot) {
        Frame t = look.torso();
        double g = crestGrown(look.grown());
        if (g <= 0.01) {
            return;
        }
        double px = t.pixel();
        double[] root = t.at(CREST_ROOT[0], CREST_ROOT[1], CREST_ROOT[2]);
        if (withKnot) {
            ellipsoid(sink, root, t.dir(1, 0, 0), t.dir(0, -1, 0), t.dir(0, 0, 1), 2.6 * px * g, 1.9 * px * g, 1.4 * px * g, 6, 10,
                (p, n, u, v) -> glow(mix(CRYSTAL_DARK, CRYSTAL, 0.45 + 0.45 * Math.max(0.0, dot(n, t.dir(0, 0, 1))))), (u, v) -> 0.1 * Math.sin(u * 4.0 + v * 5.0));
        }
        for (int i = 0; i < CREST.length; i++) {
            double[] q = CREST[i];
            double[] dir = t.dir(q[0], q[1], q[2]);
            double flicker = 1.0 + 0.08 * Math.sin(look.time() * 0.37 + i * 2.1);
            double stagger = smooth((g - 0.25 * i / CREST.length) / 0.75);
            double length = q[3] * stagger * (q[4] > 0.5 ? 1.0 : flicker);
            double[] from = add(root, mul(dir, 1.1 * px));
            if (q[4] > 0.5) {
                spike(sink, from, dir, length, 0.042, mul(t.dir(0, 0, 1), 0.04 * length), 4, 5,
                    (p, n, u, v) -> mix(cel(n, BONE_HI, BONE, BONE_DARK), BONE_TIP, smooth((u - 0.7) / 0.3)));
            } else {
                double heat = 0.5 + 0.5 * Math.sin(look.time() * 0.29 + i * 1.3);
                shard(sink, from, dir, length, 0.055 + 0.035 * q[3], t.dir(0, 0, 1), heat);
            }
        }
    }

    /**
     * A crystal shard: four flat facets from a narrow ring at {@code root}, swelling to {@code r} and more, to a point {@code length}
     * along {@code dir} ({@code roll} sets how it is turned about its length); red, its lit facets burning ({@code heat} 0..1 how hot
     * they are now), all of it glowing. Drawn from both sides.
     */
    static void shard(UkakuModel.Sink sink, double[] root, double[] dir, double length, double r, double[] roll, double heat) {
        if (length < 0.01) {
            return;
        }
        double[] d = normalize(dir);
        double[] a = sub(roll, mul(d, dot(roll, d)));
        if (dot(a, a) < 1.0E-8) {
            a = Math.abs(d[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
            a = sub(a, mul(d, dot(a, d)));
        }
        a = normalize(a);
        double[] b = cross(d, a);
        double[] mid = add(root, mul(d, length * 0.3));
        double[] point = add(root, mul(d, length));
        double[][] low = new double[4][];
        double[][] high = new double[4][];
        for (int k = 0; k < 4; k++) {
            double th = Math.PI / 2.0 * k;
            double[] off = add(mul(a, Math.cos(th)), mul(b, Math.sin(th) * 0.6));
            // Narrow where it breaks out, swelling, then to its point.
            low[k] = add(root, mul(off, r * 0.35));
            high[k] = add(mid, mul(off, r * 1.25));
        }
        int hot = mix(CRYSTAL, CRYSTAL_HOT, heat);
        for (int k = 0; k < 4; k++) {
            int k1 = (k + 1) % 4;
            int lower = crystal(facet(low[k], low[k1], high[k1], root), hot);
            int upper = crystal(facet(high[k], high[k1], point, root), hot);
            int edge = glow(mix(CRYSTAL_DARK, CRYSTAL, 0.6));
            sink.quad(low[k], low[k1], high[k1], high[k], UP, lower, lower, upper, upper);
            sink.quad(high[k], low[k], low[k1], high[k1], UP, upper, lower, lower, upper);
            sink.quad(high[k], high[k1], point, point, UP, upper, upper, edge, edge);
            sink.quad(point, high[k1], high[k], high[k], UP, edge, upper, upper, upper);
        }
    }

    /** A flat facet's outward normal (away from {@code inside}). */
    static double[] facet(double[] p0, double[] p1, double[] p2, double[] inside) {
        double[] n = normalize(cross(sub(p1, p0), sub(p2, p0)));
        return dot(n, sub(p0, inside)) < 0.0 ? mul(n, -1.0) : n;
    }

    /** A crest facet facing {@code n}: burning ({@code hot}) toward the light, red across it, dark red away from it; all glowing. */
    static int crystal(double[] n, int hot) {
        double t = dot(n, LIGHT);
        if (t > 0.35) {
            return glow(hot);
        }
        return t > -0.2 ? glow(mix(CRYSTAL_DARK, CRYSTAL, 0.9)) : glow(mix(CRYSTAL_DARK, CRYSTAL, 0.45));
    }

    // ------------------------------------------------------------------ the mask

    /** The mask's top and its chin (head pixels, y down from her neck): over her crown, down to a point well under her jaw. */
    static final double MASK_TOP = -9.6;
    static final double CHIN = 1.3;
    /** Where (head pixels) its one eye is, in the middle of the brow, and the middle of its grin. */
    static final double EYE_Y = -5.2;
    static final double MOUTH_Y = -1.85;
    /** How round it is across (a superellipse's exponent: 1 an ellipse, less boxier), still outside the head's corners. */
    static final double ROUND = 0.5;
    /** Where (head pixels) the dome over her crown begins. */
    static final double DOME_FROM = -7.6;
    /** Where (head pixels) the face begins to taper to its chin. */
    static final double TAPER_FROM = -2.5;

    /** The mask's half-width (head pixels) at {@code y}: a dome over her crown, broad across the brow, then tapering to a point. */
    static double faceWidth(double y) {
        if (y < DOME_FROM) {
            double u = (DOME_FROM - y) / (DOME_FROM - MASK_TOP);
            return Math.max(0.35, 4.8 * Math.sqrt(Math.max(0.0, 1.0 - u * u)));
        }
        if (y < TAPER_FROM) {
            return 4.8 + 0.12 * Math.exp(-Math.pow((y - EYE_Y) / 1.3, 2.0));
        }
        double s = Math.min(1.0, (y - TAPER_FROM) / (CHIN - TAPER_FROM));
        return 4.8 - (4.8 - 0.25) * Math.pow(s, 1.25);
    }

    /** How far (head pixels) its face stands in front of the head's middle at {@code y}: its snout jutting out toward the chin. */
    static double faceDepth(double y) {
        double u = Math.max(0.0, (DOME_FROM - y) / (DOME_FROM - MASK_TOP));
        return 5.35 + 0.7 * smooth((y + 3.0) / 4.0) - 1.9 * u * u;
    }

    /** How far round the head (radians from the front, each way) it wraps at {@code y}: to the ears, then under the jaw only round the front. */
    static double faceWrap(double y) {
        // (Under her jaw only its front, a keel of a chin: anything wrapped further round there turns into her chest as she looks aside.)
        double wrap = 1.95 - (1.95 - 0.4) * smooth((y + 0.6) / 1.2);
        // Where it narrows under her jaw it covers the front only; over her crown it wraps on round like a helmet.
        return wrap + (Math.min(wrap, 0.8) - wrap) * smooth((4.7 - faceWidth(y)) / 0.5) * smooth((y + 7.4) / 0.6);
    }

    /** The angle round the mask at which it is {@code x} (head pixels) across, at {@code y}. */
    static double faceAngle(double x, double y) {
        return Math.signum(x) * Math.asin(Math.min(1.0, Math.pow(Math.abs(x) / faceWidth(y), 1.0 / ROUND)));
    }

    /** A point of the mask (head pixels) at {@code a} round from the front (+ toward x, her left) and {@code y} down it. */
    static double[] facePoint(double a, double y) {
        double w = faceWidth(y);
        double d = faceDepth(y);
        double ra = Math.abs(a);
        double sa = Math.sin(Math.min(ra, Math.PI / 2.0));
        double ca = Math.cos(Math.min(ra, Math.PI));
        double x = Math.signum(a) * w * Math.pow(sa, ROUND) + Math.signum(a) * Math.max(0.0, ra - Math.PI / 2.0) * 0.2;
        double front = Math.pow(Math.max(0.0, ca), 0.5);
        // Under her jaw a keel of a chin: its sides stay forward, only so far behind its front (round the head above).
        double keel = (d - 0.55) * smooth((y + 0.5) / 0.7);
        double z = -(keel + (d - keel) * Math.signum(ca) * Math.pow(Math.abs(ca), ROUND) + relief(x, y) * front) * (ra < Math.PI / 2.0 ? 1.0 : 0.9);
        return new double[] {x, y, z};
    }

    /**
     * How far (pixels, + out) the face stands off its round at ({@code x}, {@code y}): a keel down its snout from under the eye to the
     * chin, a heavy brow ridge over the eye, cheekbones sweeping down to the chin, the eye's socket dished in, the grin's lips.
     */
    static double relief(double x, double y) {
        double keel = 0.6 * Math.exp(-Math.pow(x / 0.85, 2.0)) * smooth((y - (EYE_Y + 1.8)) / 1.2);
        double brow = 0.5 * Math.exp(-Math.pow(Math.hypot(x / 2.8, (y - (EYE_Y - 2.0)) / 0.6), 2.0));
        double cheeks = 0.0;
        for (int side : new int[] {-1, 1}) {
            double d = segment(x, y, side * 3.9, EYE_Y + 0.8, side * 1.0, CHIN - 0.6);
            cheeks = Math.max(cheeks, 0.4 * Math.exp(-Math.pow(d / 0.6, 2.0)));
        }
        double socket = -0.6 * (1.0 - smooth((Math.hypot(x, (y - EYE_Y) * 0.95) - 1.5) / 0.9));
        double lips = 0.18 * Math.exp(-Math.pow(mouthDistance(x, y) / 0.35, 2.0));
        return keel + brow + cheeks + socket + lips;
    }

    /** The grin's upper and lower lips (head pixels) at {@code x} across it: its corners drawn up. */
    static double upperLip(double x) {
        double u = x / 3.0;
        return MOUTH_Y - 0.7 - 0.4 * u * u;
    }

    static double lowerLip(double x) {
        double u = x / 3.0;
        return MOUTH_Y + 0.75 - 1.15 * u * u;
    }

    /** How far (head pixels) ({@code x}, {@code y}) is from the grin's outline. */
    static double mouthDistance(double x, double y) {
        if (Math.abs(x) > 3.0) {
            return Math.hypot(Math.abs(x) - 3.0, y - (upperLip(3.0) + lowerLip(3.0)) * 0.5);
        }
        return Math.min(Math.abs(y - upperLip(x)), Math.abs(y - lowerLip(x)));
    }

    /** How far (head pixels) ({@code x}, {@code y}) is from the segment ({@code ax}, {@code ay}) to ({@code bx}, {@code by}). */
    static double segment(double x, double y, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double t = Math.max(0.0, Math.min(1.0, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)));
        return Math.hypot(x - ax - dx * t, y - ay - dy * t);
    }

    /** The share of the mask's columns spread evenly across its face; the rest go round its sides, evenly in angle. */
    static final double FACE_COLUMNS = 0.72;
    static final double FACE_SPAN = 0.93;

    /** The angle round the mask of its column at {@code t} (-1 .. 1, her right side to her left) at {@code y}. */
    static double faceColumn(double t, double y) {
        double at = Math.abs(t);
        double w = faceWidth(y);
        if (at <= FACE_COLUMNS) {
            return faceAngle(Math.signum(t) * w * FACE_SPAN * at / FACE_COLUMNS, y);
        }
        double from = Math.abs(faceAngle(w * FACE_SPAN, y));
        double wrap = Math.max(faceWrap(y), from);
        return Math.signum(t) * (from + (wrap - from) * (at - FACE_COLUMNS) / (1.0 - FACE_COLUMNS));
    }

    /** A point of the mask's face at ({@code x}, {@code y}) (head pixels across and down it), {@code lift} pixels off it (rig space). */
    static double[] onFace(Frame h, double x, double y, double lift) {
        // Never past its edge, where there is no mask to lie on.
        double w = faceWidth(Math.max(MASK_TOP, Math.min(CHIN, y))) * FACE_SPAN;
        x = Math.max(-w, Math.min(w, x));
        double[] q = facePoint(faceAngle(x, y), y);
        q[2] -= lift;
        return offFace(h, h.at(q[0], q[1], q[2]));
    }

    /**
     * The mask as far as it has closed over her face ({@code maskGrown}: from round the eye outward), the cowl behind it, its marks,
     * the eye, the grin and the crown of horns.
     */
    static void mask(UkakuModel.Sink sink, Look look) {
        Frame h = look.head();
        double g = maskGrown(look.grown());
        if (g < 0.02) {
            return;
        }
        hood(sink, look, g);
        int across = 26;
        int down = 26;
        double[][][] p = new double[down + 1][across + 1][];
        int[][] col = new int[down + 1][across + 1];
        // How far out from the eye (head pixels) it has closed: points further out are drawn in to that rim.
        double reach = 1.5 + 15.0 * g;
        double[] eye = {0.0, EYE_Y};
        for (int i = 0; i <= down; i++) {
            double y = MASK_TOP + (CHIN - MASK_TOP) * i / down;
            for (int j = 0; j <= across; j++) {
                double a = faceColumn(j / (double)across * 2.0 - 1.0, y);
                double[] q = facePoint(a, y);
                double fromEye = Math.hypot(q[0] - eye[0], q[1] - eye[1]);
                if (fromEye > reach) {
                    double k = reach / fromEye;
                    double[] r = facePoint(faceAngle(q[0] * k, eye[1] + (q[1] - eye[1]) * k), eye[1] + (q[1] - eye[1]) * k);
                    q = r;
                }
                p[i][j] = offFace(h, h.at(q[0], q[1], q[2]));
                int c = cel(faceNormal(a, y, h), MASK_HI, MASK, MASK_DARK);
                // Going grey round its sides and under the chin.
                c = mix(c, MASK_DARK, 0.4 * smooth((Math.abs(a) - 0.95) / 0.8) + 0.2 * smooth((q[1] - 0.5) / 2.5));
                // The fresh rim as it closes: raw red.
                c = mix(c, MUSCLE, smooth((fromEye - reach + 1.2) / 1.2) * (1.0 - smooth((g - 0.97) / 0.03)));
                col[i][j] = c;
            }
        }
        grid(sink, p, col);
        // Its inside (seen past its edges at the sides, top and bottom): the outside brought in toward the head's middle.
        for (int i = 0; i < down; i++) {
            for (int j = 0; j < across; j++) {
                if (j >= 3 && j < across - 3 && i >= 2 && i < down - 2) {
                    continue; // nowhere it can be seen
                }
                double[][] corner = {p[i][j], p[i + 1][j], p[i + 1][j + 1], p[i][j + 1]};
                double[][] in = new double[4][];
                for (int k = 0; k < 4; k++) {
                    double[] axis = h.at(0.0, 0.0, 0.0);
                    double[] c = corner[k];
                    double[] mid = {axis[0], c[1], axis[2]};
                    double[] inward = add(c, mul(normalize(sub(mid, c)), 0.4 * h.pixel()));
                    in[k] = inward == offHead(h, inward) ? inward : c;
                }
                sink.quad(in[0], in[1], in[2], in[3], UP, MASK_DARK, MASK_DARK, MASK_DARK, MASK_DARK);
            }
        }
        // The eye ignites first; the bone spreads out from round it.
        eye(sink, h, look.time());
        if (g < 0.35) {
            return;
        }
        marks(sink, h, smooth((g - 0.35) / 0.4));
        grin(sink, h, smooth((g - 0.55) / 0.35));
        crown(sink, look, h);
    }

    /** The mask's outward normal at ({@code a}, {@code y}), in rig space. */
    static double[] faceNormal(double a, double y, Frame h) {
        double e = 0.01;
        double[] p = facePoint(a, y);
        double[] pa = facePoint(a + e, y);
        double[] pv = facePoint(a, Math.min(CHIN, y + 0.1));
        double[] pv0 = facePoint(a, Math.max(MASK_TOP, y - 0.1));
        double[] n = cross(sub(pv, pv0), sub(pa, p));
        double[] centre = {0.0, p[1], 0.0};
        if (dot(n, sub(p, centre)) < 0.0) {
            n = mul(n, -1.0);
        }
        return normalize(add(mul(h.x(), n[0]), add(mul(h.y(), n[1]), mul(h.z(), n[2]))));
    }

    /**
     * A strip laid on the face (head pixels): along {@code xs}/{@code ys}, {@code width} pixels across at its start tapering to
     * {@code end} at its finish, {@code lift} off it, all {@code colour}.
     */
    static void stroke(UkakuModel.Sink sink, Frame h, double[] xs, double[] ys, double width, double end, double lift, int colour) {
        int n = xs.length - 1;
        for (int i = 0; i < n; i++) {
            double dx = xs[i + 1] - xs[i];
            double dy = ys[i + 1] - ys[i];
            double l = Math.max(1.0E-6, Math.hypot(dx, dy));
            double nx = -dy / l;
            double ny = dx / l;
            double w0 = 0.5 * (width + (end - width) * i / n);
            double w1 = 0.5 * (width + (end - width) * (i + 1) / n);
            double[] a = onFace(h, xs[i] + nx * w0, ys[i] + ny * w0, lift);
            double[] b = onFace(h, xs[i + 1] + nx * w1, ys[i + 1] + ny * w1, lift);
            double[] c = onFace(h, xs[i + 1] - nx * w1, ys[i + 1] - ny * w1, lift);
            double[] d = onFace(h, xs[i] - nx * w0, ys[i] - ny * w0, lift);
            sink.quad(a, b, c, d, UP, colour, colour, colour, colour);
        }
    }

    /** Points along a line on the face from ({@code ax}, {@code ay}) to ({@code bx}, {@code by}), bowed {@code bow} pixels, {@code n} segments. */
    static double[][] line(double ax, double ay, double bx, double by, double bow, int n) {
        double[] xs = new double[n + 1];
        double[] ys = new double[n + 1];
        double l = Math.max(1.0E-6, Math.hypot(bx - ax, by - ay));
        for (int i = 0; i <= n; i++) {
            double u = i / (double)n;
            double b = bow * 4.0 * u * (1.0 - u);
            xs[i] = ax + (bx - ax) * u - (by - ay) / l * b;
            ys[i] = ay + (by - ay) * u + (bx - ax) / l * b;
        }
        return new double[][] {xs, ys};
    }

    /** The black slashes running out from the eye, two each side, to the edges of the brow, and the cracks; {@code g} how far drawn. */
    static void marks(UkakuModel.Sink sink, Frame h, double g) {
        for (int side : new int[] {-1, 1}) {
            double[][] upperSlash = line(side * 2.15, EYE_Y - 0.45, side * (2.15 + 2.75 * g), EYE_Y - 0.45 - 1.25 * g, 0.0, 4);
            stroke(sink, h, upperSlash[0], upperSlash[1], 0.5, 0.12, 0.06, MASK_LINE);
            double[][] lowerSlash = line(side * 2.2, EYE_Y + 0.45, side * (2.2 + 2.65 * g), EYE_Y + 0.45 + 0.55 * g, 0.0, 4);
            stroke(sink, h, lowerSlash[0], lowerSlash[1], 0.45, 0.1, 0.06, MASK_LINE);
        }
        // Thin cracks: off the crown down toward the eye, and from the corner of the grin down its jaw.
        double[][] crown = line(0.5, MASK_TOP + 1.2, 0.25, MASK_TOP + 1.2 + (EYE_Y - 1.9 - MASK_TOP - 1.2) * g, 0.3, 4);
        stroke(sink, h, crown[0], crown[1], 0.16, 0.08, 0.05, MASK_LINE);
        double[][] jaw = line(-2.3, MOUTH_Y + 0.6, -2.3 + 1.2 * g, MOUTH_Y + 0.6 + 1.6 * g, -0.2, 3);
        stroke(sink, h, jaw[0], jaw[1], 0.15, 0.07, 0.05, MASK_LINE);
        double[][] cheek = line(2.3, EYE_Y + 1.5, 2.3 + 1.1 * g, EYE_Y + 1.5 + 1.2 * g, 0.2, 3);
        stroke(sink, h, cheek[0], cheek[1], 0.14, 0.06, 0.05, MASK_LINE);
    }

    /** The grin: black between its lips, a row of teeth hanging from the upper and one standing off the lower, two fangs; {@code g} how far bared. */
    static void grin(UkakuModel.Sink sink, Frame h, double g) {
        if (g <= 0.01) {
            return;
        }
        int n = 12;
        double half = 3.0 * (0.35 + 0.65 * g);
        for (int i = 0; i < n; i++) {
            double x0 = -half + 2.0 * half * i / n;
            double x1 = -half + 2.0 * half * (i + 1) / n;
            sink.quad(onFace(h, x0, upperLip(x0), 0.05), onFace(h, x1, upperLip(x1), 0.05), onFace(h, x1, lowerLip(x1), 0.05),
                onFace(h, x0, lowerLip(x0), 0.05), UP, SOCKET, SOCKET, SOCKET, SOCKET);
        }
        int teeth = 10;
        for (int k = 0; k < teeth; k++) {
            double x = -2.7 + 5.4 * (k + 0.5) / teeth;
            if (Math.abs(x) > half - 0.2) {
                continue;
            }
            double u = x / 3.0;
            boolean fang = k == 2 || k == teeth - 3;
            double longest = (fang ? 1.0 : 0.62) * (1.0 - 0.45 * u * u);
            tooth(sink, h, x, upperLip(x) - 0.05, 0.46, longest, 1.0);
        }
        for (int k = 0; k < teeth - 1; k++) {
            double x = -2.43 + 4.86 * (k + 0.5) / (teeth - 1);
            if (Math.abs(x) > half - 0.25) {
                continue;
            }
            double u = x / 3.0;
            tooth(sink, h, x, lowerLip(x) + 0.05, 0.44, 0.55 * (1.0 - 0.5 * u * u), -1.0);
        }
    }

    /** One tooth on the face at {@code x} from its gum at {@code y}, {@code width} across, {@code length} long, pointing {@code way} (1 down, -1 up). */
    static void tooth(UkakuModel.Sink sink, Frame h, double x, double y, double width, double length, double way) {
        double tipY = y + way * length;
        double lift = 0.12;
        double[] a = onFace(h, x - width * 0.5, y, lift);
        double[] b = onFace(h, x + width * 0.5, y, lift);
        double[] c = onFace(h, x + width * 0.12, tipY, lift);
        double[] d = onFace(h, x - width * 0.12, tipY, lift);
        sink.quad(a, b, c, d, UP, TOOTH, TOOTH, TOOTH_DARK, TOOTH_DARK);
    }

    /** The one eye: a black socket, a dark red ring, the iris burning (its heat breathing a little), a black pupil. */
    static void eye(UkakuModel.Sink sink, Frame h, double time) {
        double[] e = facePoint(0.0, EYE_Y);
        double z = Math.min(e[2] - 0.05, -4.7);
        double[] right = h.dir(1, 0, 0);
        double[] up = h.dir(0, -1, 0);
        double px = h.pixel();
        double pulse = 0.5 + 0.5 * Math.sin(time * 0.17);
        disc(sink, h.at(0.0, EYE_Y, z), right, up, 1.75 * px, 1.65 * px, 16, SOCKET, SOCKET);
        disc(sink, h.at(0.0, EYE_Y, z - 0.06), right, up, 1.3 * px, 1.25 * px, 16, glow(0xFF8A0A14), glow(0xFF420308));
        disc(sink, h.at(0.0, EYE_Y, z - 0.12), right, up, 1.0 * px, 1.0 * px, 16, glow(mix(IRIS_HOT, 0xFFFFD0C0, pulse * 0.4)), glow(IRIS));
        disc(sink, h.at(0.0, EYE_Y, z - 0.18), right, up, 0.36 * px, 0.36 * px, 10, SOCKET, SOCKET);
        // A glint.
        disc(sink, h.at(-0.38, EYE_Y - 0.38, z - 0.22), right, up, 0.16 * px, 0.16 * px, 6, glow(0xFFFFFFFF), glow(0xFFFFE0D8));
    }

    /** A flat disc at {@code c}, spanned by {@code a} and {@code b}: its middle {@code inner}, its rim {@code outer}. */
    static void disc(UkakuModel.Sink sink, double[] c, double[] a, double[] b, double ra, double rb, int sides, int inner, int outer) {
        for (int k = 0; k < sides; k++) {
            double a0 = 2.0 * Math.PI * k / sides;
            double a1 = 2.0 * Math.PI * (k + 1) / sides;
            double[] p0 = add(c, add(mul(a, Math.cos(a0) * ra), mul(b, Math.sin(a0) * rb)));
            double[] p1 = add(c, add(mul(a, Math.cos(a1) * ra), mul(b, Math.sin(a1) * rb)));
            sink.quad(c, p0, p1, p1, UP, inner, outer, outer, outer);
            sink.quad(c, p1, p0, p0, UP, inner, outer, outer, outer);
        }
    }

    /**
     * The crown of horns over the mask (head pixels: where each grows off the dome, which way, x to her left, y down, z back; how long,
     * pixels; how far it curls out): a tall one in the middle, two beside it, the two great horns sweeping up and out, two small ones
     * between.
     */
    static final double[][] CROWN = {
        {0.0, -9.4, -2.2, 0.0, -1.0, 0.3, 6.4, 0.0},
        {1.5, -9.2, -2.0, 0.25, -1.0, 0.4, 4.6, 0.6},
        {-1.5, -9.2, -2.0, -0.25, -1.0, 0.4, 4.6, -0.6},
        {2.9, -9.0, -1.4, 0.85, -0.8, 0.35, 6.2, 1.8},
        {-2.9, -9.0, -1.4, -0.85, -0.8, 0.35, 6.2, -1.8},
        {2.2, -9.1, -0.2, 0.5, -1.0, 0.6, 3.0, 0.5},
        {-2.2, -9.1, -0.2, -0.5, -1.0, 0.6, 3.0, -0.5}};

    static void crown(UkakuModel.Sink sink, Look look, Frame h) {
        double g = lastGrown(look.grown());
        if (g <= 0.01) {
            return;
        }
        double px = h.pixel();
        for (int i = 0; i < CROWN.length; i++) {
            double[] q = CROWN[i];
            double[] root = offHead(h, h.at(q[0], q[1], q[2]));
            double[] dir = h.dir(q[3], q[4], q[5]);
            double grow = smooth((g - 0.06 * i) / 0.7);
            double[] curl = mul(h.dir(Math.signum(q[7] == 0.0 ? 0.0 : q[7]), 0.25, 0.6), Math.abs(q[7]) * px + 0.6 * px);
            spike(sink, root, dir, q[6] * px * grow, (i < 3 || i > 4 ? 0.5 : 0.65) * px, curl, 5, 6,
                (p, n, u, v) -> mix(cel(n, MASK_HI, MASK, MASK_DARK), BONE_TIP, 0.7 * smooth((u - 0.78) / 0.22)));
        }
    }

    /** The cowl over the rest of her head behind the mask: dark, close over the crown and down the back; on her head, never in it nor below it. */
    static void hood(UkakuModel.Sink sink, Look look, double g) {
        Frame h = look.head();
        double px = h.pixel();
        // Behind the mask only (never over the front of its dome).
        UkakuModel.Sink outside = (a, b, c, d, n, ca, cb, cc, cd) -> {
            if (onHead(h, a) && onHead(h, b) && onHead(h, c) && onHead(h, d) && behind(h, a) && behind(h, b) && behind(h, c) && behind(h, d)) {
                sink.quad(a, b, c, d, n, ca, cb, cc, cd);
            }
        };
        double k = 0.9 + 0.1 * g;
        blob(outside, h.at(0.0, -5.6, 1.2), h.dir(0, -1, 0), h.dir(1, 0, 0), h.dir(0, 0, 1), 5.6 * px * k, 5.3 * px * k, 5.3 * px * k, 3.6, 12, 18,
            (p, n, u, v) -> cel(n, HOOD_HI, HOOD, HOOD_DARK), (u, v) -> 0.03 * Math.sin(v * 7.0 + u * 3.0));
    }

    /** Whether a point (rig space) of the cowl is behind the mask's face. */
    static boolean behind(Frame h, double[] p) {
        return headPixels(h, p)[2] > -1.8;
    }

    /** A point (rig space) in her head's own pixels ({@code h} her head's frame). */
    static double[] headPixels(Frame h, double[] p) {
        double[] d = sub(p, h.o());
        double k = dot(h.x(), h.x());
        return new double[] {dot(d, h.x()) / k, dot(d, h.y()) / k, dot(d, h.z()) / k};
    }

    /**
     * Whether a point (rig space) of the cowl is on her head: neither inside it nor down below her jaw (at the back of her head, kept
     * a pixel higher still, clear of her shoulders as she turns it).
     */
    static boolean onHead(Frame h, double[] p) {
        double[] q = headPixels(h, p);
        double lowest = -0.2 - 1.2 * smooth((q[2] - 1.0) / 2.0);
        return q[1] < lowest && !(Math.abs(q[0]) < 4.0 && q[1] > -8.0 && Math.abs(q[2]) < 4.0);
    }

    /** A point (rig space) moved out of her head's box to just past its nearest face, if it is in it. */
    static double[] offHead(Frame h, double[] p) {
        double m = 0.3;
        double[] q = headPixels(h, p);
        double[] room = {4.0 + m - Math.abs(q[0]), q[1] + 8.0 + m, m - q[1], 4.0 + m - Math.abs(q[2])};
        if (!(room[0] > 0.0 && room[1] > 0.0 && room[2] > 0.0 && room[3] > 0.0)) {
            return p;
        }
        int k = 0;
        for (int i = 1; i < 4; i++) {
            if (room[i] < room[k]) {
                k = i;
            }
        }
        switch (k) {
            case 0 -> q[0] = Math.signum(q[0] == 0.0 ? 1.0 : q[0]) * (4.0 + m);
            case 1 -> q[1] = -8.0 - m;
            case 2 -> q[1] = m;
            default -> q[2] = Math.signum(q[2] == 0.0 ? 1.0 : q[2]) * (4.0 + m);
        }
        return h.at(q[0], q[1], q[2]);
    }

    /** A point (rig space) of the mask in her head's box brought out in front of her face (never down past her jaw into her chest). */
    static double[] offFace(Frame h, double[] p) {
        double[] q = headPixels(h, p);
        if (q[2] < 0.0 && Math.abs(q[0]) < 4.3 && q[1] > -8.3 && q[1] < 0.3 && q[2] > -4.3) {
            return h.at(q[0], q[1], -4.6);
        }
        return offHead(h, p);
    }

    // ------------------------------------------------------------------ her body under it

    /** The robe's ring round her waist under her ribs and its hips (torso pixels: half-widths across and front to back). */
    static final double ROBE_TOP = 6.4;
    static final double ROBE_X = 4.45;
    static final double ROBE_Z = 2.45;
    static final double HIP_X = 4.6;
    static final double HIP_Z = 2.65;
    /** How long (blocks) the skirt hangs off her hips, and how far it flares out by its hem. */
    static final double HANG = 0.62;
    static final double FLARE = 0.11;
    /** How close (blocks) to a leg's line the skirt may come: the leg's corners and a hair. */
    public static final double LEG_ROOM = 0.185;
    static final int SKIRT_COLUMNS = 20;
    static final int SKIRT_ROWS = 6;

    /** A point round a rounded box (pixels: half-widths {@code hx}, {@code hz}) at {@code th} (0 her front, + toward her left). */
    static double[] round(double th, double hx, double hz) {
        double s = Math.sin(th);
        double c = Math.cos(th);
        return new double[] {hx * Math.signum(s) * Math.pow(Math.abs(s), 0.6), -hz * Math.signum(c) * Math.pow(Math.abs(c), 0.6)};
    }

    /**
     * The robe ({@code robeGrown}: spilling down from her waist): a bodice round her middle under her ribs, a ragged pale trim at its
     * top; the skirt from her hips to her ankles, its front and sides carried by her legs (split down the front and back, so either
     * leg can kick out through it), its back hanging, flaring out to a tattered hem; kept off her legs, never into the ground.
     */
    public static void robe(UkakuModel.Sink sink, Look look) {
        Body b = look.body();
        if (b == null) {
            return;
        }
        Frame t = look.torso();
        double g = robeGrown(look.grown());
        if (g <= 0.01) {
            return;
        }
        int around = 16;
        // The bodice.
        double[] ys = {ROBE_TOP, 9.2, 12.0};
        double[][][] p = new double[ys.length][around + 1][];
        int[][] col = new int[ys.length][around + 1];
        for (int i = 0; i < ys.length; i++) {
            for (int k = 0; k <= around; k++) {
                double th = 2.0 * Math.PI * k / around;
                double[] r = round(th, ROBE_X, ROBE_Z);
                p[i][k] = t.at(r[0], ys[i], r[1]);
                double[] n = t.dir(Math.sin(th), 0.0, -Math.cos(th));
                col[i][k] = mix(cel(n, ROBE_HI, ROBE, ROBE_DARK), ROBE_DARK, 0.3 * Math.max(0.0, Math.sin(th * 5.0 + i)));
            }
        }
        grid(sink, p, col);
        // The ragged trim along its top.
        for (int k = 0; k < around; k++) {
            double th0 = 2.0 * Math.PI * k / around;
            double th1 = 2.0 * Math.PI * (k + 1) / around;
            double[] r0 = round(th0, ROBE_X + 0.15, ROBE_Z + 0.15);
            double[] r1 = round(th1, ROBE_X + 0.15, ROBE_Z + 0.15);
            double[] rm = round((th0 + th1) * 0.5, ROBE_X + 0.15, ROBE_Z + 0.15);
            double drop = 0.9 + 0.5 * Math.abs(Math.sin(k * 2.3));
            sink.quad(t.at(r0[0], ROBE_TOP - 0.35, r0[1]), t.at(r1[0], ROBE_TOP - 0.35, r1[1]), t.at(rm[0], ROBE_TOP + drop, rm[1]),
                t.at(rm[0], ROBE_TOP + drop, rm[1]), UP, TRIM, TRIM, mix(TRIM, ROBE, 0.6), mix(TRIM, ROBE, 0.6));
        }
        // The skirt.
        double[] hipR = b.rightHip().o();
        double[] hipL = b.leftHip().o();
        double[] footR = b.rightFoot().o();
        double[] footL = b.leftFoot().o();
        double[] legR = normalize(sub(footR, hipR));
        double[] legL = normalize(sub(footL, hipL));
        double[] hang = normalize(add(mul(t.dir(0, 1, 0), 0.6), new double[] {0.0, -0.4, 0.0}));
        int cols = SKIRT_COLUMNS;
        int rows = SKIRT_ROWS;
        double[][][] s = new double[rows + 1][cols + 1][];
        int[][] out = new int[rows + 1][cols + 1];
        int[][] in = new int[rows + 1][cols + 1];
        for (int k = 0; k <= cols; k++) {
            // Split down the front (k = 0 and cols are her right and left of it).
            double th = 2.0 * Math.PI * k / cols;
            double sn = Math.sin(th);
            double cs = Math.cos(th);
            double[] r = round(th, HIP_X, HIP_Z);
            double[] hip = t.at(r[0], 12.0, r[1]);
            double[] outward = t.dir(sn, 0.0, -cs);
            // Which leg carries it (her right for sin < 0: x to her left is +; split down the front, the first column her left
            // leg's, the last her right's), and how much: the front and sides most.
            double right = k == 0 ? 0.0 : k == cols ? 1.0 : smooth(0.5 - 2.5 * sn);
            double carried = 0.55 + 0.35 * Math.max(0.0, cs) + 0.1 * Math.abs(sn);
            double[] leg = normalize(add(mul(legR, right), mul(legL, 1.0 - right)));
            double[] way = normalize(add(mul(leg, carried), mul(hang, 1.0 - carried)));
            double ragged = 0.9 + 0.1 * Math.sin(k * 2.9 + 0.7) - (k % 5 == 2 ? 0.1 : 0.0);
            double length = HANG * ragged * g;
            for (int i = 0; i <= rows; i++) {
                double u = i / (double)rows;
                double[] q = add(hip, add(mul(way, length * u), mul(outward, FLARE * g * Math.pow(u, 1.4))));
                q = offLeg(q, hipR, footR, outward);
                q = offLeg(q, hipL, footL, outward);
                q[1] = Math.max(q[1], 0.012);
                s[i][k] = q;
                double fold = Math.sin(th * 9.0 + u * 1.5);
                int c = mix(cel(outward, ROBE_HI, ROBE, ROBE_DARK), ROBE_DARK, 0.35 * Math.max(0.0, fold));
                double stain = Math.sin(th * 3.0 + 1.1) * Math.sin(u * 5.0 + th * 2.0);
                c = mix(c, STAIN, 0.6 * smooth((stain - 0.55) / 0.3) * smooth((u - 0.45) / 0.3));
                // Frayed paler at its hem.
                out[i][k] = mix(c, ROBE_HI, 0.25 * smooth((u - 0.88) / 0.12));
                in[i][k] = mix(ROBE_IN, ROBE_DARK, 0.5 * u);
            }
        }
        sheet(sink, s, out, in);
    }

    /** {@code q} moved out from the leg running {@code hip} to {@code foot} (and a little past it) to at least {@link #LEG_ROOM}. */
    static double[] offLeg(double[] q, double[] hip, double[] foot, double[] outward) {
        double[] d = sub(foot, hip);
        double l2 = Math.max(1.0E-9, dot(d, d));
        double t = Math.max(-0.05, Math.min(1.1, dot(sub(q, hip), d) / l2));
        double[] near = add(hip, mul(d, t));
        double[] off = sub(q, near);
        double dist = Math.sqrt(dot(off, off));
        if (dist >= LEG_ROOM) {
            return q;
        }
        double[] way = dist < 1.0E-6 ? outward : mul(off, 1.0 / dist);
        return add(near, mul(way, LEG_ROOM));
    }

    /** Bone bands round each upper arm and each wrist. */
    static void bands(UkakuModel.Sink sink, Look look) {
        Body b = look.body();
        double g = lastGrown(look.grown());
        if (g <= 0.01) {
            return;
        }
        double k = b.armScale();
        for (Frame f : new Frame[] {b.rightUpper(), b.leftUpper(), b.rightWrist(), b.leftWrist()}) {
            band(sink, f, 0.0, 0.0, 0.0, 2.0 * k + 0.3, 2.0 * k + 0.3, 0.65 * g, 12);
        }
    }

    /** One band round a box in frame {@code f} (pixels: centre, half-widths), {@code height} pixels tall, {@code around} faces. */
    static void band(UkakuModel.Sink sink, Frame f, double cx, double cy, double cz, double hx, double hz, double height, int around) {
        if (height <= 0.01) {
            return;
        }
        double[][][] p = new double[2][around + 1][];
        int[][] c = new int[2][around + 1];
        for (int k = 0; k <= around; k++) {
            double th = 2.0 * Math.PI * k / around;
            double[] r = round(th, hx, hz);
            p[0][k] = f.at(cx + r[0], cy - height * 0.5, cz + r[1]);
            p[1][k] = f.at(cx + r[0], cy + height * 0.5, cz + r[1]);
            double[] n = f.dir(Math.sin(th), 0.0, -Math.cos(th));
            int base = mix(mix(cel(n, WRAP_HI, WRAP, WRAP_DARK), STAIN, 0.2), WRAP_DARK, 0.35 * Math.max(0.0, Math.sin(th * 6.0)));
            c[0][k] = base;
            c[1][k] = mix(base, BONE_DARK, 0.3);
        }
        grid(sink, p, c);
    }

    /** Four long claws off the bottom of a hand (frame {@code h}: x to her left, y down her arm, z back), curling in toward the palm. */
    static void claws(UkakuModel.Sink sink, Look look, Frame h, int side) {
        double g = lastGrown(look.grown());
        if (g <= 0.01 || h == null) {
            return;
        }
        double px = h.pixel();
        UkakuModel.Sink above = (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(ground(a), ground(b), ground(c), ground(d), n, ca, cb, cc, cd);
        double[] zs = {-1.45, -0.5, 0.5, 1.45};
        for (int i = 0; i < zs.length; i++) {
            double reach = (i == 1 || i == 2 ? 5.2 : 4.4) * g;
            double[] root = h.at(side * 0.4, -0.2, zs[i]);
            double[] dir = h.dir(side * 0.25, 1.0, -0.12 - 0.05 * zs[i]);
            double[] curl = mul(h.dir(side * 1.0, -0.2, -0.35), 1.9 * px * g);
            spike(above, root, dir, reach * px, 0.55 * px, curl, 5, 5, (p, n, u, v) -> {
                int c = cel(n, CLAW_HI, CLAW, CLAW_DARK);
                return mix(c, CLAW_TIP, smooth((u - 0.35) / 0.45));
            });
        }
        // The thumb, off the front of the palm side.
        spike(above, h.at(side * 1.2, -0.8, -1.6), h.dir(side * 0.7, 0.6, -0.6), 3.0 * px * g, 0.5 * px, mul(h.dir(side, 0.3, 0.2), 0.9 * px * g), 4, 5,
            (p, n, u, v) -> mix(cel(n, CLAW_HI, CLAW, CLAW_DARK), CLAW_TIP, smooth((u - 0.35) / 0.45)));
    }

    /** A bird's talons off the sole of a foot (frame {@code f}: x to her left, y down her leg, z back): three forward, a spur behind; on the ground, never in it. */
    static void talons(UkakuModel.Sink sink, Look look, Frame f) {
        double g = lastGrown(look.grown());
        if (g <= 0.01 || f == null) {
            return;
        }
        double px = f.pixel();
        UkakuModel.Sink above = (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(ground(a), ground(b), ground(c), ground(d), n, ca, cb, cc, cd);
        for (double x : new double[] {-1.3, 0.0, 1.3}) {
            double[] root = f.at(x, -0.6, -1.4);
            double[] dir = f.dir(x * 0.3, 0.12, -1.0);
            spike(above, root, dir, (x == 0.0 ? 4.6 : 3.9) * px * g, 0.7 * px, mul(f.dir(0, 1.0, -0.2), 1.3 * px * g), 4, 5,
                (p, n, u, v) -> mix(cel(n, CLAW_HI, CLAW, CLAW_DARK), CLAW_TIP, smooth((u - 0.45) / 0.4)));
        }
        spike(above, f.at(0.0, -0.8, 1.4), f.dir(0.0, 0.25, 1.0), 2.4 * px * g, 0.6 * px, mul(f.dir(0, 1.0, 0.0), 0.8 * px * g), 3, 5,
            (p, n, u, v) -> mix(cel(n, CLAW_HI, CLAW, CLAW_DARK), CLAW_TIP, smooth((u - 0.4) / 0.4)));
    }

    /** A point (rig space) no lower than the ground under her. */
    static double[] ground(double[] p) {
        return p[1] >= 0.004 ? p : new double[] {p[0], 0.004, p[2]};
    }

    // ------------------------------------------------------------------ maths

    /** A point along a smooth curve through {@code p} at {@code t} (0..1), Catmull-Rom. */
    static double[] catmull(double[][] p, double t) {
        int segments = p.length - 1;
        double f = Math.max(0.0, Math.min(1.0, t)) * segments;
        int i = Math.min(segments - 1, (int)Math.floor(f));
        double u = f - i;
        double[] p0 = p[Math.max(0, i - 1)];
        double[] p1 = p[i];
        double[] p2 = p[i + 1];
        double[] p3 = p[Math.min(segments, i + 2)];
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            out[k] = 0.5 * (2.0 * p1[k] + (-p0[k] + p2[k]) * u + (2.0 * p0[k] - 5.0 * p1[k] + 4.0 * p2[k] - p3[k]) * u * u
                + (-p0[k] + 3.0 * p1[k] - 3.0 * p2[k] + p3[k]) * u * u * u);
        }
        return out;
    }

    public static int mix(int a, int b, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int)Math.round(((a >> 16) & 0xFF) * (1.0 - k) + ((b >> 16) & 0xFF) * k);
        int g = (int)Math.round(((a >> 8) & 0xFF) * (1.0 - k) + ((b >> 8) & 0xFF) * k);
        int bl = (int)Math.round((a & 0xFF) * (1.0 - k) + (b & 0xFF) * k);
        int al = (int)Math.round(((a >>> 24) & 0xFF) * (1.0 - k) + ((b >>> 24) & 0xFF) * k);
        return al << 24 | r << 16 | g << 8 | bl;
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double dist2(double[] a, double[] b) {
        double[] d = sub(a, b);
        return dot(d, d);
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / l);
    }
}
