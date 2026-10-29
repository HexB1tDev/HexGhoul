package com.hex.kagune.control.furuta;

/**
 * The kakuja's living skin (pure, tested; 7.0.0): a black hide of hardened kagune grown tight over every part of his body, laid on
 * each part as a box a little larger than the part (as armour is), painted from one texture ({@link #TEXTURE}) whose layout is
 * defined here and nowhere else: the layer that draws it and {@code tools/FurutaTextures} that paints it both read it, so a texel
 * always lands where it was painted for.
 *
 * <p>After the anime's final form: black flesh, red whorls like fingerprints over the chest, shoulders, back and thighs, and three
 * red slashes down the abdomen that burn (the {@link #GLOW} texture, drawn again at full brightness over the first).
 *
 * <p>Part pixels as every layer: x to the player's left, y down, z backward. Each box's net in the texture is laid out as a skin's
 * (top and bottom in a row above right, front, left and back), at its own origin, {@link #TEXELS} texels to a pixel.
 */
public final class FurutaSkin {
    public static final String TEXTURE = "textures/entity/furuta_kakuja.png";
    public static final String GLOW = "textures/entity/furuta_kakuja_glow.png";
    /** Texels per model pixel. */
    public static final int TEXELS = 8;
    /** The texture's size in model pixels (its texels: x {@link #TEXELS}). */
    public static final int WIDTH = 64;
    public static final int HEIGHT = 32;

    /** A body part's box: its corner and size in the part's own pixels, how far the hide stands off it, and its net's origin. */
    public enum Box {
        HEAD(-4, -8, -4, 8, 8, 8, 0.6, 0, 0),
        TORSO(-4, 0, -2, 8, 12, 4, 0.42, 32, 0),
        RIGHT_ARM(-3, -2, -2, 4, 12, 4, 0.4, 0, 16),
        LEFT_ARM(-1, -2, -2, 4, 12, 4, 0.4, 16, 16),
        RIGHT_LEG(-2, 0, -2, 4, 12, 4, 0.38, 32, 16),
        LEFT_LEG(-2, 0, -2, 4, 12, 4, 0.38, 48, 16);

        public final double x0;
        public final double y0;
        public final double z0;
        public final double w;
        public final double h;
        public final double d;
        public final double inflate;
        public final int u;
        public final int v;

        Box(double x0, double y0, double z0, double w, double h, double d, double inflate, int u, int v) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.w = w;
            this.h = h;
            this.d = d;
            this.inflate = inflate;
            this.u = u;
            this.v = v;
        }
    }

    /** One face of a box. */
    public enum Face {
        FRONT, BACK, RIGHT, LEFT, TOP, BOTTOM
    }

    /** A face's rectangle in the texture (model pixels): its corner and size. */
    public static double[] rect(Box box, Face face) {
        double u = box.u;
        double v = box.v;
        return switch (face) {
            case TOP -> new double[] {u + box.d, v, box.w, box.d};
            case BOTTOM -> new double[] {u + box.d + box.w, v, box.w, box.d};
            case RIGHT -> new double[] {u, v + box.d, box.d, box.h};
            case FRONT -> new double[] {u + box.d, v + box.d, box.w, box.h};
            case LEFT -> new double[] {u + box.d + box.w, v + box.d, box.d, box.h};
            case BACK -> new double[] {u + 2.0 * box.d + box.w, v + box.d, box.w, box.h};
        };
    }

    /**
     * The point of the box's surface at {@code (s, t)} pixels into the face's rectangle (s across, t down the texture), pushed
     * {@code out} pixels off it (with the box grown by the same, so the edges meet).
     */
    public static double[] point(Box box, Face face, double s, double t, double out) {
        double x0 = box.x0 - out;
        double y0 = box.y0 - out;
        double z0 = box.z0 - out;
        double w = box.w + 2.0 * out;
        double h = box.h + 2.0 * out;
        double d = box.d + 2.0 * out;
        double fs = s / rect(box, face)[2];
        double ft = t / rect(box, face)[3];
        return switch (face) {
            // Seen from in front the player's left (+x) is on the right of the picture.
            case FRONT -> new double[] {x0 + w * fs, y0 + h * ft, z0};
            case BACK -> new double[] {x0 + w * (1.0 - fs), y0 + h * ft, z0 + d};
            case RIGHT -> new double[] {x0, y0 + h * ft, z0 + d * (1.0 - fs)};
            case LEFT -> new double[] {x0 + w, y0 + h * ft, z0 + d * fs};
            case TOP -> new double[] {x0 + w * fs, y0, z0 + d * (1.0 - ft)};
            case BOTTOM -> new double[] {x0 + w * fs, y0 + h, z0 + d * ft};
        };
    }

    /** The face's outward normal. */
    public static double[] normal(Face face) {
        return switch (face) {
            case FRONT -> new double[] {0.0, 0.0, -1.0};
            case BACK -> new double[] {0.0, 0.0, 1.0};
            case RIGHT -> new double[] {-1.0, 0.0, 0.0};
            case LEFT -> new double[] {1.0, 0.0, 0.0};
            case TOP -> new double[] {0.0, -1.0, 0.0};
            case BOTTOM -> new double[] {0.0, 1.0, 0.0};
        };
    }

    /** Receives one textured quad: corners (part pixels), their texture coordinates (0..1), the normal, and each corner's opacity. */
    public interface Sink {
        void quad(double[][] corners, double[][] uv, double[] normal, double[] alpha);
    }

    /**
     * The hide on {@code box}, spread {@code grow} of the way ({@link FurutaArmor#reveal}): every face cut into cells about
     * {@code cell} pixels square, each corner as opaque as the hide has reached it, so it creeps over the body from where it started
     * ({@link #order}) with a soft edge.
     */
    public static void build(Sink sink, Box box, double grow, double cell) {
        if (grow <= 0.002) {
            return;
        }
        for (Face face : Face.values()) {
            double[] r = rect(box, face);
            int cols = Math.max(1, (int)Math.round(r[2] / cell));
            int rows = Math.max(1, (int)Math.round(r[3] / cell));
            double[] n = normal(face);
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    double[][] corners = new double[4][];
                    double[][] uv = new double[4][];
                    double[] alpha = new double[4];
                    double[][] st = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
                    boolean any = false;
                    for (int k = 0; k < 4; k++) {
                        double s = st[k][0] * r[2] / cols;
                        double t = st[k][1] * r[3] / rows;
                        double[] p = point(box, face, s, t, box.inflate);
                        corners[k] = p;
                        uv[k] = new double[] {(r[0] + s) / WIDTH, (r[1] + t) / HEIGHT};
                        alpha[k] = FurutaArmor.reveal(grow, order(box, point(box, face, s, t, 0.0)));
                        any |= alpha[k] > 0.004;
                    }
                    if (any) {
                        sink.quad(corners, uv, n, alpha);
                    }
                }
            }
        }
    }

    /**
     * When the hide reaches a point of a part (0: the sockets in his lower back, where it starts .. 1: the crown of his head, last):
     * up the torso from the small of the back, out along the arms from the shoulders and down the legs from the hips, up the neck.
     */
    public static double order(Box box, double[] p) {
        return switch (box) {
            case TORSO -> {
                double dy = (10.0 - p[1]) / 12.0;
                double front = p[2] < 0.0 ? 0.12 * (-p[2] / 2.0) : 0.0;
                yield clamp01(0.05 + 0.45 * Math.max(0.0, dy) + front + 0.03 * Math.abs(p[0]) / 4.0);
            }
            case RIGHT_ARM, LEFT_ARM -> clamp01(0.42 + 0.3 * (p[1] + 2.0) / 12.0);
            case RIGHT_LEG, LEFT_LEG -> clamp01(0.38 + 0.32 * p[1] / 12.0);
            // The head goes under from the neck up, the face last.
            case HEAD -> clamp01(0.74 + 0.22 * (-p[1]) / 8.0 + (p[2] < -3.5 ? 0.04 : 0.0));
        };
    }

    // ---------------------------------------------------------------- the paint (read by tools/FurutaTextures)

    /** Where the whorls turn (part pixels on the surface) per box, and how far they reach. */
    static final double[][] TORSO_WHORLS = {{2.3, 3.2, -2.0, 4.2}, {-2.3, 3.2, -2.0, 4.2}, {0.0, 3.5, 2.0, 5.0}, {2.6, 10.0, 2.0, 2.4},
        {-2.6, 10.0, 2.0, 2.4}};
    static final double[][] ARM_WHORLS = {{-1.0, -1.5, 0.0, 4.0}, {-1.0, 6.5, -2.0, 2.6}};
    static final double[][] LEG_WHORLS = {{0.0, 3.0, -2.0, 3.6}, {0.0, 4.0, 2.0, 3.0}};
    static final double[][] HEAD_WHORLS = {{0.0, -8.0, 0.0, 5.0}, {0.0, -4.0, 4.0, 4.5}};

    /**
     * The hide's colour (RGB) at a point of a box's surface (part pixels; {@code n} the face's normal), and {@code glow}: the
     * burning slashes alone (ARGB, transparent elsewhere). Pure paint: black flesh with a sheen of dark crimson where muscle swells,
     * red whorls round the points above, finer lines running along the limbs, and the three slashes down the abdomen.
     */
    public static int paint(Box box, double[] p, double[] n, boolean glow) {
        double slash = box == Box.TORSO && n[2] < -0.5 ? slashes(p[0], p[1]) : 0.0;
        if (glow) {
            if (slash <= 0.0) {
                return 0;
            }
            int a = (int)Math.round(255.0 * Math.min(1.0, slash * 1.3));
            return a << 24 | FurutaMesh.mix(0xB00A10, 0xFF6A4A, slash);
        }
        double[][] whorls = switch (box) {
            case TORSO -> TORSO_WHORLS;
            case RIGHT_ARM, LEFT_ARM -> ARM_WHORLS;
            case RIGHT_LEG, LEFT_LEG -> LEG_WHORLS;
            case HEAD -> HEAD_WHORLS;
        };
        double mirror = box == Box.LEFT_ARM ? -1.0 : 1.0;
        double line = 0.0;
        for (double[] w : whorls) {
            double dx = p[0] - w[0] * mirror;
            double dy = p[1] - w[1];
            double dz = p[2] - w[2];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > w[3]) {
                continue;
            }
            // Fingerprint rings: warped a little so they wander, fading out at the edge.
            double warp = 0.3 * Math.sin(dx * 1.1 + dz * 0.9) + 0.2 * Math.sin(dy * 1.4 + dx * 0.6);
            double ring = Math.abs(Math.sin((d + warp) * Math.PI / 0.8));
            double fade = 1.0 - smooth((d - w[3] * 0.65) / (w[3] * 0.35));
            line = Math.max(line, (ring < 0.3 ? 1.0 - ring / 0.3 : 0.0) * fade);
        }
        // Fine lines down the limbs and the flanks between the whorls.
        boolean limb = box != Box.TORSO && box != Box.HEAD;
        double along = limb ? Math.abs(Math.sin((p[0] * 0.8 + p[2] * 0.7) * Math.PI / 1.0 + 0.9 * Math.sin(p[1] * 0.45 + p[0])))
            : 1.0;
        double fibres = limb ? (along < 0.1 ? 0.5 * (1.0 - along / 0.1) : 0.0) : 0.0;
        // Muscle: a dark crimson sheen over the pecs, the abs and the deltoids.
        double swell = 0.0;
        if (box == Box.TORSO && n[2] < -0.5) {
            swell = Math.max(gauss(p[0] - 2.1, p[1] - 3.4, 1.8, 1.6), gauss(p[0] + 2.1, p[1] - 3.4, 1.8, 1.6)) * 0.6;
        }
        if ((box == Box.RIGHT_ARM || box == Box.LEFT_ARM) && p[1] < 2.0) {
            swell = 0.4 * gauss(p[1] + 1.0, 0.0, 2.0, 1.0);
        }
        int base = FurutaMesh.mix(0x07040A, 0x2A070D, swell + 0.12 * FurutaMesh.noise(box.ordinal() * 7 + 1, p[0] * 0.9 + p[2] * 0.5, p[1] * 0.9));
        int lined = FurutaMesh.mix(base, 0x8E1720, Math.max(line, fibres) * 0.95);
        return FurutaMesh.mix(lined, 0xE02A22, slash);
    }

    /** The three slashes down the abdomen (front of the torso), 0 outside them, 1 at their burning cores. */
    static double slashes(double x, double y) {
        double best = 0.0;
        double[] columns = {-1.55, 0.0, 1.55};
        for (int i = 0; i < 3; i++) {
            double centre = columns[i] + 0.12 * (y - 8.0) * (i - 1) * 0.3;
            double top = 5.7 + (i == 1 ? -0.3 : 0.25);
            double bottom = 10.8 - (i == 1 ? 0.0 : 0.5);
            if (y < top || y > bottom) {
                continue;
            }
            double u = (y - top) / (bottom - top);
            // Tapered at both ends like claw marks, widest a third of the way down.
            double half = 0.42 * Math.sin(Math.PI * Math.pow(u, 0.8));
            double off = Math.abs(x - centre);
            if (off < half) {
                best = Math.max(best, 1.0 - off / Math.max(1.0E-3, half) * 0.55);
            }
        }
        return best;
    }

    static double gauss(double dx, double dy, double sx, double sy) {
        return Math.exp(-(dx * dx) / (sx * sx) - (dy * dy) / (sy * sy));
    }

    static double smooth(double value) {
        double t = clamp01(value);
        return t * t * (3.0 - 2.0 * t);
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
