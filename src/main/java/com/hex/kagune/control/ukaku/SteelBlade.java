package com.hex.kagune.control.ukaku;

import com.hex.kagune.control.npc.BladeArm;
import com.hex.kagune.control.npc.BladeArmModel;
import java.util.ArrayList;
import java.util.List;

/**
 * The final kakuja's Steel Blade (pure, tested; 1.3.1 rebuilt on the ghouls' arm blade, the owner: "use the arm blade of the wild
 * ghouls for a base", then "make it the armor's texture like the last blade, a bit slimmer too"). C closes his steel wings round his
 * right arm: the steel tentacles draw back into his back while two broad ribbons of steel wind down the arm from the shoulder to the
 * wrist and flow into one great single-edged blade past the fist ({@link BladeArmModel}, the CCG ghoul division's shape), a little
 * slimmer than theirs ({@link #WIDTH}). It is the old Steel Blade's steel: dark, clouded with white, its grooves inked, a bright line
 * down its edge, blood on it after it cuts. C again plays it backwards. While it is out he is far faster and jumps far higher, and M1
 * swings it; its reach is the arm and the blade ({@link #REACH}).
 *
 * <p>{@link #build} lays it out in the right arm's own pixels (x across the arm, toward the body +, y down the arm from the shoulder, z
 * backward), so it rides every pose of the arm: the blade runs on past the fist, its edge on the back of the arm (z +), so it faces down
 * with the arm held out before him (the ghouls' "blade turned over").
 */
public final class SteelBlade {
    /** Ticks the wings take to close into the blade (and to open again). */
    public static final int FORM_TICKS = 24;
    /** Model pixels per block (the player model is drawn at 15/16). */
    public static final double PIXELS = 16.0 / 0.9375;
    /** The blade's length past the fist (blocks): the ghouls' great blade. */
    public static final double LENGTH = BladeArm.BLADE_LENGTH / PIXELS;
    public static final double ARM = 0.75;
    /** His reach with it: the arm, the blade, and the swing's sweep past its point (a ghoul's is 3.2). */
    public static final double REACH = ARM + LENGTH + 0.9;
    /** Extra levels of Speed and Jump Boost while it is out. */
    public static final int SPEED = 3;
    public static final int JUMP = 2;
    /** The shoulder line, where the blade grows out of the forearm, the fist's end and the point (arm pixels down). */
    public static final double BASE_Y = -2.0;
    public static final double GRIP_Y = 5.5;
    public static final double HAND_Y = 10.0;
    public static final double TIP_Y = HAND_Y + BladeArm.BLADE_LENGTH;
    /** How broad the blade is from its spine to its edge, and how thick across, against the ghouls' (1): slim and thin. */
    public static final double WIDTH = 0.66;
    public static final double THICKNESS = 0.5;
    /** How far up each face from the edge its bright line runs (the ghouls' 0.16), and how keen its point (the ghouls' 1.7). */
    static final double GLEAM = 0.3;
    static final double POINT = 2.6;

    // ---- The old Steel Blade's steel: dark, clouded with white, inked grooves, a bright edge; blood fresh and dried.
    static final int STEEL = 0x5C6069;
    static final int STEEL_DARK = 0x24262D;
    static final int STEEL_LIGHT = 0xA5ABB5;
    static final int CLOUD = 0xE4E8EE;
    static final int EDGE = 0xF6F8FB;
    static final int INK = 0x141519;
    static final int BLOOD_FRESH = 0x9C070D;
    static final int BLOOD_DRY = 0x3A0909;
    /** The plain grey the arm blade is built in, so each corner's own shading can be read back off it ({@link #retint}). */
    static final int GREY = 0x808080;

    private SteelBlade() {
    }

    /** Takes the blade's quads, four corners in a row each: where (arm pixels), its colour (ARGB), its normal (unit, outward). */
    public interface Sink {
        void vertex(double x, double y, double z, int argb, double nx, double ny, double nz);
    }

    /** One quad: four corners (arm pixels), a colour each (ARGB) and a normal each (unit, outward). */
    public record Quad(double[][] corners, int[] argb, double[][] normals) {
    }

    /** A quad's normal straight up marks it glowing (drawn full bright): the gleam down the edge. */
    public static boolean glows(double nx, double ny, double nz) {
        return nx == 0.0 && ny == 1.0 && nz == 0.0;
    }

    // ------------------------------------------------------------------ the forming

    /** How closed into the blade the wings are, {@code since} ticks after C was pressed ({@code on}: closing), 0..1. */
    public static double formed(boolean on, double since) {
        double t = clamp01(since / FORM_TICKS);
        return on ? t : 1.0 - t;
    }

    /** How far the steel tentacles have drawn back into his back at {@code formed} (0 out .. 1 gone). */
    public static double fold(double formed) {
        return smoother(formed / 0.42);
    }

    /** How far the blade has grown out past the fist (0..1): once the ribbons have wound down to the wrist. */
    public static double extend(double formed) {
        return BladeArm.blade(formed);
    }

    // ------------------------------------------------------------------ the shape

    /** Where station {@code s} (0 the shoulder line .. 1 the point) is down the arm. */
    public static double station(double s) {
        return BASE_Y + s * (TIP_Y - BASE_Y);
    }

    /** The blade's whole length from the shoulder line to the point (pixels). */
    public static double fullLength() {
        return TIP_Y - BASE_Y;
    }

    /** The arm's middle (x): the ribbons are round it, the blade through it. */
    public static double axis(boolean slim) {
        return slim ? -0.5 : -1.0;
    }

    /** The blade and its ribbons, {@code formed} of the way closed (0..1), as quads. */
    public static List<Quad> build(double formed, double time, boolean slim) {
        List<Quad> out = new ArrayList<>();
        build(formed, time, slim, true, collect(out));
        return out;
    }

    /** A sink that gathers quads (four corners in a row). */
    static Sink collect(List<Quad> out) {
        double[][] corners = new double[4][];
        int[] colours = new int[4];
        double[][] normals = new double[4][];
        int[] n = {0};
        return (x, y, z, argb, nx, ny, nz) -> {
            int k = n[0]++;
            corners[k] = new double[] {x, y, z};
            colours[k] = argb;
            normals[k] = new double[] {nx, ny, nz};
            if (n[0] == 4) {
                out.add(new Quad(corners.clone(), colours.clone(), normals.clone()));
                n[0] = 0;
            }
        };
    }

    /** The blade, {@code formed} of the way closed, into {@code sink} ({@code shoulder}: kept for the callers; it has no pauldron). */
    public static void build(double formed, double time, boolean slim, boolean shoulder, Sink sink) {
        build(formed, time, slim, shoulder, 0.0, 0.0, sink);
    }

    /** As above, bloodied {@code blood} (0..1), the blood {@code dry} (0..1). */
    public static void build(double formed, double time, boolean slim, boolean shoulder, double blood, double dry, Sink sink) {
        build(formed, time, slim, shoulder, blood, dry, 1.0, sink);
    }

    /** As above, {@code reach} (0..1) of its grown length out of the forearm (drawn in, resting near the ground). */
    public static void build(double formed, double time, boolean slim, boolean shoulder, double blood, double dry, double reach, Sink sink) {
        if (formed <= 0.005) {
            return;
        }
        double out = Double.isFinite(reach) ? clamp01(reach) : 1.0;
        double bloodied = clamp01(blood);
        double dried = clamp01(dry);
        double glint = ((time * 0.36) % 1.0) * 1.6 - 0.3;
        BladeArmModel.build(formed, slim, GREY, out, WIDTH, THICKNESS, GLEAM, POINT, (a, b, c, d, n, ca, cb, cc, cd) -> {
            boolean glow = n == BladeArmModel.GLOW;
            double nx = glow ? 0.0 : n[0];
            double ny = glow ? 1.0 : n[1];
            double nz = glow ? 0.0 : n[2];
            if (!glow && nx == 0.0 && ny == 1.0 && nz == 0.0) {
                nz = 1.0E-4; // a lit face that happens to face straight up is not the gleam
            }
            double[][] q = {a, b, c, d};
            int[] cs = {ca, cb, cc, cd};
            for (int k = 0; k < 4; k++) {
                int argb = retint(cs[k], glow, q[k][0], q[k][1], q[k][2], bloodied, dried, glint);
                sink.vertex(q[k][0], q[k][1], q[k][2], argb, nx, ny, nz);
            }
        });
    }

    /**
     * The arm blade's corner colour (built in {@link #GREY}) as the old Steel Blade's steel at (x, y, z): its gleam the bright edge (a glint
     * running down it now and then), its veins inked grooves, everything else mottled steel as light as the arm blade shaded that corner;
     * blood on the blade past the fist after it cuts.
     */
    static int retint(int argb, boolean glow, double x, double y, double z, double blood, double dry, double glint) {
        int alpha = argb & 0xFF000000;
        double along = clamp01((y - HAND_Y) / (TIP_Y - HAND_Y));
        if (glow) {
            // The edge: a bright white line all down it, a glint running along it now and then.
            double shine = Math.exp(-Math.pow((along - glint) / 0.06, 2.0));
            return alpha | mix(EDGE, 0xFFFFFF, shine);
        }
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int bl = argb & 0xFF;
        int colour;
        if (r > g + 30 && r > bl + 30) {
            // The arm blade's red veins: here the steel's inked grooves.
            colour = mix(INK, STEEL_DARK, 0.4);
        } else {
            double shade = (r + g + bl) / 3.0 / (GREY & 0xFF);
            // A touch darker than the old blade's, so its edge stands out against it.
            colour = steel(x, y, z, 0.85 * clamp01((shade - 0.5) / 0.85));
        }
        if (blood > 0.0 && y > HAND_Y) {
            double n = 0.5 + 0.5 * Math.sin(x * 1.3 + y * 0.53 + z * 0.9) * Math.sin(y * 0.29 - z * 1.1 + 2.1) + 0.3 * Math.sin(y * 1.7 + x * 2.3);
            double mid = smoother(along / 0.15) * (1.0 - smoother((along - 0.9) / 0.1));
            double threshold = 1.05 - blood * 0.85 * mid;
            colour = mix(colour, mix(BLOOD_FRESH, BLOOD_DRY, dry), 0.92 * smoother((n - threshold) / 0.2));
        }
        return alpha | colour;
    }

    /** Mottled steel at (x, y, z): dark steel clouded with white, specked darker; {@code light} 0..1 how bright that corner is shaded. */
    static int steel(double x, double y, double z, double light) {
        double cloud = 0.5 + 0.5 * Math.sin(x * 0.9 + y * 0.37 + z * 0.7) * Math.sin(y * 0.21 - x * 0.6 + z * 0.45 + 1.3)
            + 0.25 * Math.sin(y * 0.83 + x * 1.7 - z * 1.1);
        double blotch = smoother((cloud - 0.55) / 0.35);
        double speck = smoother((0.18 - cloud) / 0.2);
        int base = mix(STEEL_DARK, STEEL, 0.3 + 0.7 * clamp01(light));
        base = mix(base, CLOUD, 0.62 * blotch * (0.4 + 0.6 * light));
        return mix(base, STEEL_DARK, 0.55 * speck);
    }

    /**
     * How far from the blade's axis ({@link #axis}) any part of it reaches at {@code s} (0 the shoulder line .. 1 the point; pixels),
     * fully formed, with a little room: for keeping it off the ground.
     */
    public static double extent(double s) {
        double[] table = Extents.TABLE;
        double at = clamp01(s) * (table.length - 1);
        int i = (int)Math.min(table.length - 2, Math.floor(at));
        double t = at - i;
        return table[i] + (table[i + 1] - table[i]) * t;
    }

    /** {@link #extent}'s table, from the built blade (once). */
    private static final class Extents {
        static final double[] TABLE = compute();

        static double[] compute() {
            int bins = 64;
            double[] most = new double[bins + 1];
            double ax = axis(false);
            for (Quad q : build(1.0, 0.0, false)) {
                for (double[] c : q.corners()) {
                    double s = clamp01((c[1] - BASE_Y) / fullLength());
                    int i = (int)Math.round(s * bins);
                    most[i] = Math.max(most[i], Math.hypot(c[0] - ax, c[2]));
                }
            }
            double[] out = new double[bins + 1];
            for (int i = 0; i <= bins; i++) {
                double m = most[i];
                for (int j = Math.max(0, i - 2); j <= Math.min(bins, i + 2); j++) {
                    m = Math.max(m, most[j]);
                }
                out[i] = m + 0.5;
            }
            return out;
        }
    }

    /** The point of the blade's edge at {@code s} (0 the shoulder line .. 1 the point), {@code formed} of the way closed (arm pixels). */
    public static double[] edgePoint(double s, double formed, boolean slim) {
        return edgePoint(s, formed, slim, 1.0);
    }

    /** {@link #edgePoint(double, double, boolean)} with {@code reach} of it out. */
    public static double[] edgePoint(double s, double formed, boolean slim, double reach) {
        double out = extend(formed) * (Double.isFinite(reach) ? clamp01(reach) : 1.0);
        double y = station(s);
        double root = BladeArmModel.root();
        double length = (TIP_Y - root) * out;
        double u = length <= 1.0E-6 ? 0.0 : clamp01((y - root) / length);
        return BladeArmModel.edge(u, slim, out, WIDTH, POINT);
    }

    // ------------------------------------------------------------------ easing and colour

    static double clamp01(double t) {
        return t < 0.0 ? 0.0 : t > 1.0 ? 1.0 : t;
    }

    /** Smootherstep: no jerk at either end. */
    static double smoother(double t) {
        double x = clamp01(t);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    static int mix(int a, int b, double t) {
        double s = clamp01(t);
        int r = (int)Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * s);
        int g = (int)Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * s);
        int bl = (int)Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * s);
        return r << 16 | g << 8 | bl;
    }
}
