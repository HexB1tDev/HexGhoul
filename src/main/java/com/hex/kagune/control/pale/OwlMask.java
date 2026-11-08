package com.hex.kagune.control.pale;

import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * 3.4.8: the One-Eyed Owl's mask, sculpted to the owner's reference frames; the first piece of the Owl's rebuild (the owner chose to
 * approve it a piece at a time; a face painted on a flat sheet and a mask stacked out of cubes were both turned down: "every other
 * model we got in the game is clean and polished"). A smooth bone-white shell, shaded corner by corner as War's model is: its top a
 * rounded dome with two horns curving up off it like a crescent, two long prongs swept out and up off its cheeks; its brow raised over
 * a deep socket with its one red eye burning in it, a black zigzag crease across the brow and wrinkles round the eye; its cheeks
 * swelling into a grin sunk into the mask, a row of rounded square teeth above and below in red gums; and a long narrow chin post
 * with a groove down it. Its jaw (the lower teeth and gums and the chin post) drops open on a hinge behind the corners of its grin,
 * on the red of its mouth.
 *
 * <p>Pure (tested, and drawn off-game by the preview). Mask units: x to its right, y up, z out of its face; its eye at 0, 0. It is put
 * where its {@link Frame} says, {@link Frame#size} blocks a unit.
 */
public final class OwlMask {
    private OwlMask() {
    }

    /** Where it is: its eye's place behind its face (rig space), its axes (unit: its right, its up, out of its face), blocks a unit. */
    public record Frame(double[] origin, double[] right, double[] up, double[] ahead, double size) {
        double[] at(double[] p) {
            double k = this.size;
            return new double[] {this.origin[0] + (this.right[0] * p[0] + this.up[0] * p[1] + this.ahead[0] * p[2]) * k,
                this.origin[1] + (this.right[1] * p[0] + this.up[1] * p[1] + this.ahead[1] * p[2]) * k,
                this.origin[2] + (this.right[2] * p[0] + this.up[2] * p[1] + this.ahead[2] * p[2]) * k};
        }

        double[] way(double[] d) {
            return norm(new double[] {this.right[0] * d[0] + this.up[0] * d[1] + this.ahead[0] * d[2],
                this.right[1] * d[0] + this.up[1] * d[1] + this.ahead[1] * d[2], this.right[2] * d[0] + this.up[2] * d[1] + this.ahead[2] * d[2]});
        }
    }

    // ---- Its shape (mask units).
    /** The top of its dome; where its chin post begins; its end. */
    public static final double TOP = 1.05;
    static final double POST = -0.55;
    public static final double CHIN = -1.3;
    /** Its bite (where its teeth meet at the middle; its jaw parts from its face along it), how far its corners curl up, how wide its grin. */
    static final double BITE = -0.31;
    static final double CURL = 0.1;
    static final double GRIN_HALF = 0.33;
    /** Its teeth: how many a row, how tall the upper and the lower; its gums. */
    public static final int TEETH = 8;
    static final double UPPER_TOOTH = 0.105;
    static final double LOWER_TOOTH = 0.095;
    static final double GUM = 0.03;
    /** How far out its face stands at its eye; how thick its shell. */
    static final double FACE = 0.3;
    static final double SHELL = 0.09;
    /** How far (radians) its jaw drops wide open. */
    public static final double JAW_OPEN = 0.8;

    static final int BONE = 0xFFF3F0EA;
    static final int BONE_SHADE = 0xFFB9B2AA;
    static final int INK = 0xFF120E10;
    static final int SOCKET = 0xFF140A0C;
    static final int TOOTH = 0xFFFAF7F0;
    static final int TOOTH_ROOT = 0xFFCFC7BC;
    static final int GUM_RED = 0xFFB4182C;
    static final int GUM_DARK = 0xFF6A0A16;
    static final int MOUTH = 0xFF5A0610;
    static final int MOUTH_DEEP = 0xFF2A0207;

    // ------------------------------------------------------------------ its form

    /**
     * How wide (half) it is {@code y} up: egg-shaped, as the reference draws it, widest across its cheekbones ({@link #WIDEST}),
     * rounding up into its dome and tapering down in a V past its grin to its chin post, the post flared at its end.
     */
    public static double width(double y) {
        if (y >= WIDEST) {
            double t = Math.min(1.0, (y - WIDEST) / (TOP - WIDEST));
            return 0.5 * Math.sqrt(Math.max(0.0, 1.0 - t * t));
        }
        if (y >= POST) {
            double t = (WIDEST - y) / (WIDEST - POST);
            return 0.155 + 0.345 * (1.0 - Math.pow(t, 2.6));
        }
        return 0.155 + 0.05 * smooth((CHIN + 0.12 - y) / 0.12);
    }

    /** Where it is widest (across its cheekbones, where its prongs grow out). */
    static final double WIDEST = 0.3;

    /** How far out its middle stands {@code y} up: its dome rounding back over its top, its chin post set back a little. */
    static double middle(double y) {
        double z = FACE;
        if (y > 0.3) {
            double t = (y - 0.3) / (TOP - 0.3);
            z -= 0.42 * t * t;
        }
        if (y < POST + 0.1) {
            z -= 0.06 * smooth((POST + 0.1 - y) / 0.2);
        }
        return z;
    }

    /** Its shell's smooth front (no features) at ({@code x}, {@code y}): round across, back toward its edges. */
    static double base(double x, double y) {
        double w = Math.max(0.02, width(y));
        double a = Math.min(1.0, Math.abs(x) / w);
        return middle(y) - 0.8 * w * (1.0 - Math.sqrt(Math.max(0.0, 1.0 - 0.95 * a * a)));
    }

    /** Where its zigzag brow runs {@code x} across (three sharp peaks each side, dipping to the bridge between). */
    static double brow(double x) {
        double f = Math.max(-1.0, Math.min(1.0, x / BROW_HALF));
        double k = (f + 1.0) * 6.0;
        int i = (int)Math.min(11, Math.floor(k));
        double u = k - i;
        return browAt(i) + (browAt(i + 1) - browAt(i)) * u;
    }

    static final double BROW_HALF = 0.4;

    static double browAt(int i) {
        double f = -1.0 + i / 6.0;
        double peak = i % 2 == 1 ? 0.06 : -0.02;
        return 0.33 + peak - 0.05 * (1.0 - Math.abs(f)) + 0.04 * f * f;
    }

    /** Where its upper and lower teeth meet {@code x} across, curling up at its corners. */
    public static double bite(double x) {
        double f = Math.min(1.0, Math.abs(x) / GRIN_HALF);
        return BITE + CURL * f * f;
    }

    /** How far into its grin {@code (x, y)} is (1 deep inside .. 0 at its lips' edge .. below 0 outside). */
    static double inMouth(double x, double y) {
        double top = bite(x) + UPPER_TOOTH + GUM;
        double bottom = bite(x) - LOWER_TOOTH - GUM;
        double across = 1.0 - Math.abs(x) / (GRIN_HALF + 0.015);
        double down = Math.min(top - y, y - bottom) / 0.02;
        return Math.min(across / 0.08, down);
    }

    /**
     * What is carved into its shell and raised out of it at ({@code x}, {@code y}): its brow standing out over the zigzag, its eye's deep
     * socket with a rim round it, its cheeks swelling either side of its grin, its grin sunk in with its lips rolled round it.
     */
    static double relief(double x, double y) {
        double d = 0.0;
        // Its brow: a heavy ridge arching over its eye, the zigzag crease along its foot.
        double arch = 0.37 - 0.03 * (1.0 - Math.min(1.0, Math.abs(x) / BROW_HALF)) + 0.05 * x * x;
        double fade = 1.0 - smooth((Math.abs(x) - BROW_HALF) / 0.1);
        d += 0.04 * Math.exp(-Math.pow((y - arch - 0.05) / 0.08, 2.0)) * fade;
        // Its socket, and the rim round it.
        double r = Math.hypot(x, y);
        if (r < SOCKET_R) {
            double t = r / SOCKET_R;
            d -= 0.085 * (1.0 - t * t);
        }
        d += 0.022 * Math.exp(-Math.pow((r - SOCKET_R - 0.02) / 0.03, 2.0));
        // Its cheeks, swelling up round the corners of its grin.
        for (int s = -1; s <= 1; s += 2) {
            double cx = x - s * 0.33;
            double cy = y + 0.08;
            d += 0.045 * Math.exp(-(cx * cx + cy * cy) / 0.012);
        }
        // Its grin, sunk into it, its lips rolled round it.
        double m = inMouth(x, y);
        if (m > 0.0) {
            d -= 0.1 * smooth(m);
        } else {
            d += 0.018 * Math.exp(-m * m / 0.5);
        }
        return d;
    }

    /** How far round its eye's socket is (mask units). */
    static final double SOCKET_R = 0.14;

    /** Its face's front (its shell with what is carved into it). */
    static double front(double x, double y) {
        return base(x, y) + relief(x, y);
    }

    /** Its face's outward normal at ({@code x}, {@code y}) (mask units). */
    static double[] normal(double x, double y) {
        double e = 0.006;
        double dx = (front(x + e, y) - front(x - e, y)) / (2.0 * e);
        double dy = (front(x, y + e) - front(x, y - e)) / (2.0 * e);
        return norm(new double[] {-dx, -dy, 1.0});
    }

    /** Its colour at ({@code x}, {@code y}): bone white, shaded in what is carved into it; its socket dark; its grin's hollow red. */
    static int colour(double x, double y) {
        double r = Math.hypot(x, y);
        double m = inMouth(x, y);
        if (m > 0.4) {
            return MOUTH;
        }
        double cut = Math.max(0.0, -relief(x, y)) / 0.06;
        int c = mix(BONE, BONE_SHADE, Math.min(1.0, cut) * 0.8);
        // (Shaded a little toward its socket: its black lining is laid in it, crisp-edged, see eye.)
        c = mix(c, BONE_SHADE, 0.6 * (1.0 - smooth((r - SOCKET_R) / 0.05)));
        // (A little cooler down its sides, where it turns away.)
        double a = Math.abs(x) / Math.max(0.05, width(y));
        return mix(c, BONE_SHADE, 0.25 * a * a);
    }

    /** Where its jaw is hinged (behind the corners of its grin). */
    static final double[] HINGE = {0.0, BITE + 0.07, FACE - 0.34};

    /** {@code p} on its jaw dropped {@code angle} radians open (its chin down and back). */
    static double[] dropped(double[] p, double angle) {
        if (angle == 0.0) {
            return p;
        }
        double ry = p[1] - HINGE[1];
        double rz = p[2] - HINGE[2];
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new double[] {p[0], HINGE[1] + ry * c - rz * s, HINGE[2] + ry * s + rz * c};
    }

    // ------------------------------------------------------------------ drawing it

    /** How a part of it is moved (its face: not at all; its jaw: dropped open). */
    interface Move {
        double[] at(double[] p);
    }

    /** It, where {@code f} puts it, its jaw {@code jaw} (0 shut .. 1 wide) open: its smooth parts to {@code skin}, the rest to {@code sink}. */
    public static void draw(UkakuModel.Sink sink, OwlCreature.Skin skin, Frame f, double jaw) {
        double angle = JAW_OPEN * Math.max(0.0, Math.min(1.0, jaw));
        Move face = p -> p;
        Move chin = p -> dropped(p, angle);
        Out out = new Out(sink, skin, f);
        // The red of its mouth behind its teeth: hidden shut, seen as its jaw drops.
        out.ellipsoid(face, new double[] {0.0, BITE + 0.01, FACE - 0.2}, GRIN_HALF * 1.02, 0.2, 0.13, MOUTH_DEEP, MOUTH);
        shell(out, face, false, angle);
        shell(out, chin, true, angle);
        teeth(out, face, chin);
        eye(out, face);
        lines(out, face, chin);
        // Its horns, curving up off its dome and in at their points like a crescent; its prongs, swept out and up off its cheeks.
        for (int s = -1; s <= 1; s += 2) {
            double hy = 0.83;
            double hx = s * width(hy) * 0.7;
            out.horn(face, new double[] {hx, hy, base(hx, hy) - 0.07}, new double[] {s * 0.42, 1.0, -0.08}, 0.92, 0.12, 0.085,
                new double[] {-s * 0.3, 0.03, -0.05});
            double cy = 0.3;
            double cx = s * width(cy) * 0.88;
            out.horn(face, new double[] {cx, cy, base(cx, cy) - 0.08}, new double[] {s * 1.0, 0.72, -0.3}, 1.02, 0.09, 0.065,
                new double[] {s * 0.02, 0.2, 0.0});
        }
    }

    /**
     * Its shell, the upper (its face, from the top of its dome to its bite) or the lower (its jaw, from its bite to the end of its
     * chin): the carved front, its back (the shell's thickness behind it), the rounded edge between, and where it parts from the other
     * (the red inside of it, seen as its jaw drops).
     */
    static void shell(Out out, Move move, boolean jaw, double angle) {
        int rows = jaw ? 30 : 40;
        int cols = 26;
        double[][][] p = new double[rows + 1][cols + 1][];
        for (int i = 0; i <= rows; i++) {
            double t = i / (double)rows;
            // (Its rows closer together toward its bite and its eye, where most is.)
            double e = jaw ? t * t * 0.4 + t * 0.6 : 1.0 - Math.pow(1.0 - t, 1.6);
            double guess = jaw ? BITE + (CHIN - BITE) * e : TOP + (BITE - TOP) * e;
            double w = width(guess);
            for (int j = 0; j <= cols; j++) {
                double s = -1.0 + 2.0 * j / cols;
                // (Its columns closer together toward its middle.)
                double x = Math.signum(s) * Math.pow(Math.abs(s), 1.15) * w;
                double y = jaw ? bite(x) + (CHIN - bite(x)) * e : TOP + (bite(x) - TOP) * e;
                p[i][j] = new double[] {x, y};
            }
        }
        // Its front.
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[][] q = {p[i][j], p[i][j + 1], p[i + 1][j + 1], p[i + 1][j]};
                double[][] at = new double[4][];
                double[][] n = new double[4][];
                int[] c = new int[4];
                for (int k = 0; k < 4; k++) {
                    double x = q[k][0];
                    double y = q[k][1];
                    at[k] = move.at(new double[] {x, y, front(x, y)});
                    n[k] = turned(move, normal(x, y));
                    c[k] = colour(x, y);
                }
                out.smooth(at, n, c);
            }
        }
        // Its back: the shell's thickness behind its smooth front (none of what is carved in it), facing back.
        int step = 2;
        for (int i = 0; i < rows; i += step) {
            for (int j = 0; j < cols; j += step) {
                int i1 = Math.min(rows, i + step);
                int j1 = Math.min(cols, j + step);
                double[][] q = {p[i][j], p[i1][j], p[i1][j1], p[i][j1]};
                double[][] at = new double[4][];
                double[][] n = new double[4][];
                int[] c = new int[4];
                for (int k = 0; k < 4; k++) {
                    double x = q[k][0];
                    double y = q[k][1];
                    at[k] = move.at(new double[] {x, y, base(x, y) - SHELL});
                    n[k] = turned(move, scale(normal(x, y), -1.0));
                    c[k] = BONE_SHADE;
                }
                out.smooth(at, n, c);
            }
        }
        // Its edge, rounded from its front over to its back: down its left, (its chin's end), up its right.
        java.util.List<double[]> edge = new java.util.ArrayList<>();
        for (int i = 0; i <= rows; i++) {
            edge.add(p[i][0]);
        }
        if (jaw) {
            for (int j = 1; j <= cols; j++) {
                edge.add(p[rows][j]);
            }
        }
        for (int i = jaw ? rows - 1 : rows; i >= (jaw ? 0 : 0); i--) {
            edge.add(p[i][cols]);
        }
        for (int k = 0; k + 1 < edge.size(); k++) {
            double[] a = edge.get(k);
            double[] b = edge.get(k + 1);
            double[] out2 = norm(new double[] {b[1] - a[1], -(b[0] - a[0]), 0.0});
            double[] mid = {(a[0] + b[0]) * 0.5, (a[1] + b[1]) * 0.5};
            double[] centre = {0.0, jaw ? -0.7 : 0.3};
            if (out2[0] * (mid[0] - centre[0]) + out2[1] * (mid[1] - centre[1]) < 0.0) {
                out2 = scale(out2, -1.0);
            }
            // Three steps round its edge: its front, a little out and back, its back.
            double[][] ring = new double[3][];
            double[][] ringB = new double[3][];
            double[][] ringN = new double[3][];
            double[] fa = {a[0], a[1], front(a[0], a[1])};
            double[] fb = {b[0], b[1], front(b[0], b[1])};
            double[] ba = {a[0], a[1], base(a[0], a[1]) - SHELL};
            double[] bb = {b[0], b[1], base(b[0], b[1]) - SHELL};
            ring[0] = fa;
            ringB[0] = fb;
            ring[1] = add(lerp(fa, ba, 0.5), scale(out2, 0.018));
            ringB[1] = add(lerp(fb, bb, 0.5), scale(out2, 0.018));
            ring[2] = ba;
            ringB[2] = bb;
            ringN[0] = norm(add(normal(a[0], a[1]), scale(out2, 1.2)));
            ringN[1] = out2;
            ringN[2] = norm(add(scale(normal(a[0], a[1]), -1.0), scale(out2, 1.2)));
            for (int r = 0; r < 2; r++) {
                double[][] at = {move.at(ring[r]), move.at(ringB[r]), move.at(ringB[r + 1]), move.at(ring[r + 1])};
                double[][] n = {turned(move, ringN[r]), turned(move, ringN[r]), turned(move, ringN[r + 1]), turned(move, ringN[r + 1])};
                out.smooth(at, n, new int[] {BONE_SHADE, BONE_SHADE, BONE_SHADE, BONE_SHADE});
            }
        }
        // Where it parts from the other: the red inside of it (its face's bottom edge, its jaw's top), seen as its jaw drops.
        int row = jaw ? 0 : rows;
        for (int j = 0; j < cols; j++) {
            double[] a = p[row][j];
            double[] b = p[row][j + 1];
            double[] fa = move.at(new double[] {a[0], a[1], front(a[0], a[1])});
            double[] fb = move.at(new double[] {b[0], b[1], front(b[0], b[1])});
            double[] ba = move.at(new double[] {a[0], a[1], base(a[0], a[1]) - SHELL});
            double[] bb = move.at(new double[] {b[0], b[1], base(b[0], b[1]) - SHELL});
            out.flat(fa, fb, bb, ba, MOUTH);
        }
    }

    /**
     * Its teeth: rounded square blocks standing in its grin, their fronts flush with its face, the middle ones the biggest; the upper row
     * hanging from a red gum along its upper lip, the lower standing in its own on its jaw.
     */
    static void teeth(Out out, Move face, Move chin) {
        double tw = 2.0 * GRIN_HALF / TEETH;
        for (int i = 0; i < TEETH; i++) {
            double x = -GRIN_HALF + tw * (i + 0.5);
            double big = 1.0 - 0.25 * Math.pow(Math.abs(x) / GRIN_HALF, 2.0);
            double b = bite(x);
            for (int row = 0; row < 2; row++) {
                boolean upper = row == 0;
                Move m = upper ? face : chin;
                double tall = (upper ? UPPER_TOOTH : LOWER_TOOTH) * big;
                double y = upper ? b + 0.006 + tall * 0.5 : b - 0.006 - tall * 0.5;
                double zf = base(x, y);
                double[] az = norm(new double[] {-(base(x + 0.01, y) - base(x - 0.01, y)) / 0.02, 0.0, 1.0});
                double[] ax = norm(cross(new double[] {0.0, 1.0, 0.0}, az));
                double[] ay = cross(az, ax);
                double[] c = add(new double[] {x, y, zf}, scale(az, -0.045));
                out.rounded(m, c, ax, ay, az, tw * 0.44, tall * 0.5, 0.05, TOOTH, upper ? TOOTH_ROOT : TOOTH_ROOT, upper);
                // Its gum, over (or under) it.
                double gy = upper ? b + (UPPER_TOOTH + GUM * 0.5) : b - (LOWER_TOOTH + GUM * 0.5);
                double[] g = add(new double[] {x, gy, base(x, gy)}, scale(az, -0.055));
                out.rounded(m, g, ax, ay, az, tw * 0.56, GUM * 0.62, 0.05, GUM_RED, GUM_DARK, !upper);
            }
        }
    }

    /** Its one eye in its socket: a burning red ball, darker round its edge, a black heart, a glint. */
    static void eye(Out out, Move face) {
        // Its socket's black lining, laid just over the hollow of it, its edge a clean circle.
        int rings = 5;
        int round = 24;
        double edge = SOCKET_R * 0.97;
        for (int i = 0; i < rings; i++) {
            double r0 = edge * i / rings;
            double r1 = edge * (i + 1) / rings;
            for (int k = 0; k < round; k++) {
                double a0 = 2.0 * Math.PI * k / round;
                double a1 = 2.0 * Math.PI * (k + 1) / round;
                double[][] q = {{r0 * Math.cos(a0), r0 * Math.sin(a0)}, {r1 * Math.cos(a0), r1 * Math.sin(a0)}, {r1 * Math.cos(a1), r1 * Math.sin(a1)},
                    {r0 * Math.cos(a1), r0 * Math.sin(a1)}};
                double[][] at = new double[4][];
                for (int t = 0; t < 4; t++) {
                    at[t] = face.at(add(new double[] {q[t][0], q[t][1], front(q[t][0], q[t][1])}, scale(normal(q[t][0], q[t][1]), 0.004)));
                }
                out.flat(at[0], at[1], at[2], at[3], SOCKET);
            }
        }
        double z = front(0.0, 0.0);
        // (A ball set in its socket, its front standing out a little past the socket's floor.)
        out.ellipsoid(face, new double[] {0.0, 0.0, z - 0.03}, 0.1, 0.1, 0.075, OwlModel.glow(0xFF50040A), OwlModel.glow(0xFFD80E1C));
        out.ellipsoid(face, new double[] {0.0, 0.0, z + 0.03}, 0.056, 0.056, 0.022, OwlModel.glow(0xFF9A0612), OwlModel.glow(0xFFFF2A30));
        out.ellipsoid(face, new double[] {-0.03, 0.034, z + 0.05}, 0.013, 0.013, 0.006, OwlModel.glow(0xFFFFFFFF), OwlModel.glow(0xFFFFFFFF));
    }

    /**
     * Its black lines, each a thin raised ridge laid along its face: the zigzag crease across its brow, two slashes up each temple, three
     * wrinkles each side sweeping out from its eye, the creases of its grin curling up at its corners, the groove down its chin post
     * and the bar across under its lip.
     */
    static void lines(Out out, Move face, Move chin) {
        double[][] zig = new double[13][];
        for (int i = 0; i <= 12; i++) {
            zig[i] = new double[] {-BROW_HALF + 2.0 * BROW_HALF * i / 12.0, browAt(i)};
        }
        out.line(face, zig, 0.03, 0.022);
        for (int s = -1; s <= 1; s += 2) {
            for (int k = 0; k < 2; k++) {
                double y0 = 0.5 + 0.1 * k;
                out.line(face, curve(new double[] {s * (0.25 + 0.03 * k), y0}, new double[] {s * (0.33 + 0.03 * k), y0 + 0.08},
                    new double[] {s * (0.4 + 0.02 * k), y0 + 0.16}), 0.024, 0.006);
            }
            for (int k = 0; k < 3; k++) {
                out.line(face, curve(new double[] {s * (SOCKET_R + 0.04), 0.07 - 0.07 * k}, new double[] {s * (0.26 + 0.01 * k), 0.06 - 0.09 * k},
                    new double[] {s * (0.34 + 0.02 * k), -0.02 - 0.1 * k}), 0.018 - 0.002 * k, 0.005);
            }
            double cy = bite(GRIN_HALF);
            out.line(face, curve(new double[] {s * (GRIN_HALF + 0.01), cy + 0.02}, new double[] {s * (GRIN_HALF + 0.05), cy + 0.07},
                new double[] {s * (GRIN_HALF + 0.06), cy + 0.15}), 0.02, 0.006);
        }
        out.line(chin, new double[][] {{-0.09, POST - 0.03}, {0.09, POST - 0.03}}, 0.018, 0.018);
        out.line(chin, new double[][] {{0.0, POST - 0.06}, {0.0, -0.9}, {0.0, CHIN + 0.08}}, 0.018, 0.012);
        for (double y : new double[] {CHIN + 0.17, CHIN + 0.11}) {
            for (int s = -1; s <= 1; s += 2) {
                out.line(chin, new double[][] {{s * 0.05, y}, {s * 0.11, y}}, 0.013, 0.013);
            }
        }
    }

    static double[][] curve(double[] a, double[] m, double[] b) {
        int n = 8;
        double[][] out = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double u = i / (double)n;
            out[i] = new double[] {(1 - u) * (1 - u) * a[0] + 2 * u * (1 - u) * m[0] + u * u * b[0], (1 - u) * (1 - u) * a[1] + 2 * u * (1 - u) * m[1] + u * u * b[1]};
        }
        return out;
    }

    /** {@code n} (a direction on a part) as {@code move} turns it. */
    static double[] turned(Move move, double[] n) {
        double[] o = move.at(new double[] {0.0, 0.0, 0.0});
        return norm(sub(move.at(n), o));
    }

    // ------------------------------------------------------------------ putting it out

    /** Puts its parts (mask units) where the frame says: its smooth parts to the skin (lit corner by corner), the rest to the sink. */
    static final class Out {
        final UkakuModel.Sink sink;
        final OwlCreature.Skin skin;
        final Frame f;

        Out(UkakuModel.Sink sink, OwlCreature.Skin skin, Frame f) {
            this.sink = sink;
            this.skin = skin;
            this.f = f;
        }

        static final float[] UV = {0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F};

        /** A smooth quad (mask units, as moved), each corner its own normal and colour. */
        void smooth(double[][] p, double[][] n, int[] c) {
            if (OwlModel.glowing(c[0])) {
                this.flat(p[0], p[1], p[2], p[3], c[0]);
                return;
            }
            this.skin.quad(OwlCreature.Tex.PLAIN, this.f.at(p[0]), this.f.at(p[1]), this.f.at(p[2]), this.f.at(p[3]), UV,
                new double[][] {this.f.way(n[0]), this.f.way(n[1]), this.f.way(n[2]), this.f.way(n[3])}, c[0], c[1], c[2], c[3]);
        }

        /** A flat quad, both sides. */
        void flat(double[] a, double[] b, double[] c, double[] d, int colour) {
            double[] n = this.f.way(norm(cross(sub(b, a), sub(d, a))));
            this.sink.quad(this.f.at(a), this.f.at(b), this.f.at(c), this.f.at(d), n, colour, colour, colour, colour);
            this.sink.quad(this.f.at(a), this.f.at(d), this.f.at(c), this.f.at(b), scale(n, -1.0), colour, colour, colour, colour);
        }

        /** An ellipsoid (axes along x, y, z), {@code back} colour toward its back shading to {@code front} toward its front. */
        void ellipsoid(Move m, double[] c, double rx, double ry, double rz, int back, int front) {
            this.rounded(m, c, X, Y, Z, rx, ry, rz, 2.0, front, back, false);
        }

        /** A rounded block (a squared-off ellipsoid, its corners rounded), {@code root} colour toward its root (its top if {@code rootUp}). */
        void rounded(Move m, double[] c, double[] ax, double[] ay, double[] az, double hx, double hy, double hz, int colour, int root, boolean rootUp) {
            this.rounded(m, c, ax, ay, az, hx, hy, hz, 7.0, colour, root, rootUp);
        }

        void rounded(Move m, double[] c, double[] ax, double[] ay, double[] az, double hx, double hy, double hz, double power, int colour, int root,
                     boolean rootUp) {
            int lat = 8;
            int lon = 12;
            double[][][] p = new double[lat + 1][lon + 1][];
            double[][][] n = new double[lat + 1][lon + 1][];
            int[][] col = new int[lat + 1][lon + 1];
            boolean glow = OwlModel.glowing(colour);
            for (int i = 0; i <= lat; i++) {
                double u = Math.PI * i / lat;
                for (int j = 0; j <= lon; j++) {
                    double v = 2.0 * Math.PI * j / lon;
                    // (Its poles out of its front and back: x and y round it, z through it.)
                    double sz = squash(Math.cos(u), power);
                    double sx = squash(Math.sin(u) * Math.cos(v), power);
                    double sy = squash(Math.sin(u) * Math.sin(v), power);
                    p[i][j] = add(c, add(scale(ax, sx * hx), add(scale(ay, sy * hy), scale(az, sz * hz))));
                    n[i][j] = norm(add(scale(ax, grad(sx, power) / hx), add(scale(ay, grad(sy, power) / hy), scale(az, grad(sz, power) / hz))));
                    double toward = glow ? (Math.cos(u) + 1.0) * 0.5 : Math.max(0.0, Math.min(1.0, (rootUp ? sy : -sy) * 0.5 + 0.5));
                    col[i][j] = glow ? mix(root, colour, toward) : mix(colour, root, toward * toward);
                }
            }
            for (int i = 0; i < lat; i++) {
                for (int j = 0; j < lon; j++) {
                    int[][] k = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                    double[][] q = new double[4][];
                    double[][] nn = new double[4][];
                    int[] cc = new int[4];
                    for (int t = 0; t < 4; t++) {
                        q[t] = m.at(p[k[t][0]][k[t][1]]);
                        nn[t] = turned(m, n[k[t][0]][k[t][1]]);
                        cc[t] = col[k[t][0]][k[t][1]];
                    }
                    if (glow) {
                        double[] fn = this.f.way(norm(add(add(nn[0], nn[1]), add(nn[2], nn[3]))));
                        this.sink.quad(this.f.at(q[0]), this.f.at(q[1]), this.f.at(q[2]), this.f.at(q[3]), fn, cc[0], cc[1], cc[2], cc[3]);
                    } else {
                        this.smooth(q, nn, cc);
                    }
                }
            }
        }

        /**
         * A horn or prong: a smooth tapering cone from {@code root} along {@code dir} ({@code length} long), {@code r0} wide and
         * {@code r1} deep at its root, curling {@code curl} at its point; bone white, greying a little toward its point.
         */
        void horn(Move m, double[] root, double[] dir, double length, double r0, double r1, double[] curl) {
            int rings = 12;
            int sides = 10;
            double[] d = norm(dir);
            double[][] spine = new double[rings + 1][];
            for (int i = 0; i <= rings; i++) {
                double u = i / (double)rings;
                spine[i] = add(root, add(scale(d, length * u), scale(curl, u * u)));
            }
            double[][][] p = new double[rings + 1][sides + 1][];
            double[][][] n = new double[rings + 1][sides + 1][];
            for (int i = 0; i <= rings; i++) {
                double u = i / (double)rings;
                double[] t = norm(sub(spine[Math.min(rings, i + 1)], spine[Math.max(0, i - 1)]));
                double[] a = norm(cross(t, Z));
                if (dot(a, a) < 1.0E-6) {
                    a = X;
                }
                double[] b = cross(t, a);
                double taper = Math.pow(1.0 - u, 0.8) + 0.004;
                for (int k = 0; k <= sides; k++) {
                    double ang = 2.0 * Math.PI * k / sides;
                    double ca = Math.cos(ang);
                    double sa = Math.sin(ang);
                    p[i][k] = add(spine[i], add(scale(a, ca * r0 * taper), scale(b, sa * r1 * taper)));
                    n[i][k] = norm(add(scale(a, ca / r0), scale(b, sa / r1)));
                }
            }
            for (int i = 0; i < rings; i++) {
                int c0 = mix(BONE, BONE_SHADE, 0.35 * i / rings);
                int c1 = mix(BONE, BONE_SHADE, 0.35 * (i + 1) / rings);
                for (int k = 0; k < sides; k++) {
                    double[][] q = {m.at(p[i][k]), m.at(p[i + 1][k]), m.at(p[i + 1][k + 1]), m.at(p[i][k + 1])};
                    double[][] nn = {turned(m, n[i][k]), turned(m, n[i + 1][k]), turned(m, n[i + 1][k + 1]), turned(m, n[i][k + 1])};
                    this.smooth(q, nn, new int[] {c0, c1, c1, c0});
                }
            }
        }

        /** A black line laid along its face through {@code pts} ({x, y}): a thin raised ridge, {@code w} wide tapering to {@code tip}. */
        void line(Move m, double[][] pts, double w, double tip) {
            int n = pts.length - 1;
            double[][] l = new double[n + 1][];
            double[][] r = new double[n + 1][];
            double[][] lo = new double[n + 1][];
            double[][] ro = new double[n + 1][];
            for (int i = 0; i <= n; i++) {
                double[] a = pts[Math.max(0, i - 1)];
                double[] b = pts[Math.min(n, i + 1)];
                double[] along = norm(new double[] {b[0] - a[0], b[1] - a[1], 0.0});
                double half = (w + (tip - w) * i / (double)n) * 0.5;
                double x = pts[i][0];
                double y = pts[i][1];
                double[] side = {-along[1] * half, along[0] * half};
                double[] nl = normal(x + side[0], y + side[1]);
                double[] nr = normal(x - side[0], y - side[1]);
                l[i] = add(new double[] {x + side[0], y + side[1], front(x + side[0], y + side[1])}, scale(nl, 0.007));
                r[i] = add(new double[] {x - side[0], y - side[1], front(x - side[0], y - side[1])}, scale(nr, 0.007));
                lo[i] = add(new double[] {x + side[0], y + side[1], front(x + side[0], y + side[1])}, scale(nl, -0.004));
                ro[i] = add(new double[] {x - side[0], y - side[1], front(x - side[0], y - side[1])}, scale(nr, -0.004));
            }
            for (int i = 0; i < n; i++) {
                this.flat(m.at(l[i]), m.at(l[i + 1]), m.at(r[i + 1]), m.at(r[i]), INK);
                this.flat(m.at(l[i]), m.at(lo[i]), m.at(lo[i + 1]), m.at(l[i + 1]), INK);
                this.flat(m.at(r[i]), m.at(r[i + 1]), m.at(ro[i + 1]), m.at(ro[i]), INK);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    static final double[] X = {1.0, 0.0, 0.0};
    static final double[] Y = {0.0, 1.0, 0.0};
    static final double[] Z = {0.0, 0.0, 1.0};

    static double squash(double x, double n) {
        return Math.signum(x) * Math.pow(Math.abs(x), 2.0 / n);
    }

    static double grad(double x, double n) {
        return Math.signum(x) * Math.pow(Math.abs(x), n - 1.0);
    }

    static int mix(int a, int b, double t) {
        double u = Math.max(0.0, Math.min(1.0, t));
        int r = (int)Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * u);
        int g = (int)Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * u);
        int bl = (int)Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * u);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    static double smooth(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return c * c * (3.0 - 2.0 * c);
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] norm(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0.0, 0.0, 1.0} : scale(a, 1.0 / l);
    }
}
