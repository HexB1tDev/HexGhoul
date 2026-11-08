package com.hex.kagune.control.pale;

import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * 3.4.8: the One-Eyed Owl's mask, built out of blocks as a Blockbench model is (the owner: "You're meant to build it with blocks ...
 * You're meant to make an actual thing, not a face with an image"), to the owner's reference frames; the first piece of the Owl's
 * rebuild (the owner chose to approve it a piece at a time). As the anime draws it: a bone-white mask, its top a rounded dome with two
 * horns curving up off it like a crescent, two long prongs swept out and up off its cheeks, a black zigzag brow over its one red eye,
 * wrinkles round the eye, a grin of square teeth in red gums curling up at its corners, and a long narrow chin post with a line down
 * it. Every feature is a block of its own standing on the face: the face itself is stacked slabs, stepped back at their sides so it
 * rounds off; the horns and prongs are chains of blocks, each turned along the curve and thinner than the last.
 *
 * <p>Its jaw (its lower teeth and gums and the chin post) is a group of its own, hinged behind the corners of its grin, dropping open on
 * the dark red of its mouth.
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
    /** The top of its dome; where its dome begins; where its face begins narrowing to its chin; where its chin post begins; its end. */
    public static final double TOP = 1.05;
    static final double DOME = 0.62;
    static final double JOWL = -0.25;
    static final double POST = -0.62;
    public static final double CHIN = -1.3;
    /** Its bite (where its teeth meet at the middle; the jaw parts from the face along it), how far its corners curl up, how wide its grin. */
    static final double BITE = -0.33;
    static final double CURL = 0.11;
    static final double GRIN_HALF = 0.26;
    /** Its teeth: how many a row, how tall the upper and the lower; its gums. */
    public static final int TEETH = 7;
    static final double UPPER_TOOTH = 0.1;
    static final double LOWER_TOOTH = 0.09;
    static final double GUM = 0.026;
    /** How far out its face stands at its eye; how deep it is (to its back); how thick each slab of its face. */
    static final double FACE = 0.3;
    static final double DEPTH = 0.3;
    static final double SLAB = 0.07;
    /** How far (radians) its jaw drops wide open. */
    public static final double JAW_OPEN = 0.85;

    static final int BONE = 0xFFF6F3EE;
    static final int BONE_MID = 0xFFE6E1DA;
    static final int BONE_SIDE = 0xFFD0CAC2;
    static final int INK = 0xFF141012;
    static final int TOOTH = 0xFFF7F4EE;
    static final int GUM_RED = 0xFFB3172B;
    static final int MOUTH = 0xFF9A1020;
    static final int MOUTH_DEEP = 0xFF4E0610;
    static final int SOCKET = 0xFF0E0809;
    static final int IRIS_DARK = 0xFF7A0812;

    /** How wide (half, mask units) it is {@code y} up: a round dome, its face, narrowing under its grin to its chin post, flared at its end. */
    public static double width(double y) {
        if (y >= DOME) {
            double t = Math.min(1.0, (y - DOME) / (TOP - DOME));
            return 0.5 * Math.sqrt(Math.max(0.0, 1.0 - t * t));
        }
        if (y >= JOWL) {
            return 0.5 - 0.05 * (DOME - y) / (DOME - JOWL);
        }
        if (y >= POST) {
            return 0.45 - 0.28 * smooth((JOWL - y) / (JOWL - POST));
        }
        return 0.17 + 0.05 * smooth((CHIN + 0.12 - y) / 0.12);
    }

    /** How far out of its face its middle stands {@code y} up: its dome rounding back over its top, its chin post a little back. */
    static double middle(double y) {
        if (y > 0.3) {
            double t = (y - 0.3) / (TOP - 0.3);
            return FACE - 0.34 * t * t;
        }
        return y < POST ? FACE - 0.04 : FACE;
    }

    /** Its face's front at ({@code x}, {@code y}), stepped back toward its sides as its slabs are. */
    static double front(double x, double y) {
        double w = Math.max(0.02, width(y));
        double a = Math.abs(x) / w;
        return middle(y) - (a <= STEP[0] ? 0.0 : a <= STEP[1] ? INSET[0] : INSET[1]);
    }

    /** Where each slab steps back toward its sides (of its half width), and how far. */
    static final double[] STEP = {0.62, 0.86};
    static final double[] INSET = {0.06, 0.15};

    /** Where its upper and lower teeth meet {@code x} across, curling up at its corners. */
    public static double bite(double x) {
        double f = Math.min(1.0, Math.abs(x) / GRIN_HALF);
        return BITE + CURL * f * f;
    }

    /** Where its jaw is hinged (behind the corners of its grin). */
    static final double[] HINGE = {0.0, BITE + 0.05, FACE - 0.32};

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

    /** How a group of its blocks is moved (its face: not at all; its jaw: dropped open). */
    interface Move {
        double[] at(double[] p);
    }

    /** It, where {@code f} puts it, its jaw {@code jaw} (0 shut .. 1 wide) open. */
    public static void draw(UkakuModel.Sink sink, Frame f, double jaw) {
        double angle = JAW_OPEN * Math.max(0.0, Math.min(1.0, jaw));
        Move still = p -> p;
        Move dropped = p -> dropped(p, angle);
        Blocks face = new Blocks(sink, f, still);
        Blocks chin = new Blocks(sink, f, dropped);
        slabs(face, chin);
        // The dark of its mouth, behind its teeth (hidden shut, seen as its jaw drops).
        // (Inside its face's front: none of it shows past the face's sides, shut.)
        face.box(new double[] {0.0, BITE + 0.03, FACE - 0.16}, X, Y, Z, GRIN_HALF * 0.95, 0.15, 0.08, MOUTH_DEEP);
        face.box(new double[] {0.0, BITE + 0.06, FACE - 0.06}, X, Y, Z, GRIN_HALF * 0.98, 0.055, 0.03, MOUTH);
        chin.box(new double[] {0.0, BITE - 0.06, FACE - 0.07}, X, Y, Z, GRIN_HALF * 0.95, 0.055, 0.04, MOUTH);
        grin(face, chin);
        eye(face);
        brow(face);
        chinPost(chin);
        // Its horns, curving up off its dome and in at their points like a crescent; its prongs, swept out and up off its cheeks.
        for (int s = -1; s <= 1; s += 2) {
            double hy = 0.84;
            double hx = s * width(hy) * 0.72;
            chain(face, new double[] {hx, hy, middle(hy) - 0.13}, new double[] {s * 0.42, 1.0, -0.1}, 0.9, 0.13, 0.09, new double[] {-s * 0.3, 0.02, -0.04},
                9, s);
            double cy = 0.28;
            double cx = s * width(cy) * 0.9;
            chain(face, new double[] {cx, cy, middle(cy) - 0.17}, new double[] {s * 1.0, 0.72, -0.3}, 1.0, 0.1, 0.07, new double[] {s * 0.02, 0.2, 0.0}, 9, s);
        }
    }

    /**
     * Its face: slabs stacked from the end of its chin post to the top of its dome, each as wide as its face is there, its middle out
     * to its face's front and stepped back twice toward its sides; the slabs under its bite (and, where its grin curls up, their ends)
     * its jaw's.
     */
    static void slabs(Blocks face, Blocks chin) {
        int n = (int)Math.ceil((TOP - CHIN) / SLAB);
        for (int i = 0; i < n; i++) {
            double y0 = CHIN + i * SLAB;
            double y1 = Math.min(TOP, y0 + SLAB);
            double y = (y0 + y1) * 0.5;
            double w = width(y);
            if (w < 0.02) {
                continue;
            }
            double back = middle(y) - DEPTH;
            // The x where its bite runs through this slab: inside it (the middle) its face's, outside (its corners) its jaw's.
            double split = y >= BITE + CURL ? 2.0 : y <= BITE ? -1.0 : GRIN_HALF * Math.sqrt((y - BITE) / CURL);
            double[] edges = {-w, -STEP[1] * w, -STEP[0] * w, STEP[0] * w, STEP[1] * w, w};
            for (int k = 0; k + 1 < edges.length; k++) {
                double a = edges[k];
                double b = edges[k + 1];
                double mid = Math.abs((a + b) * 0.5) / w;
                double z = middle(y) - (mid <= STEP[0] ? 0.0 : mid <= STEP[1] ? INSET[0] : INSET[1]);
                int colour = mid <= STEP[0] ? BONE : mid <= STEP[1] ? BONE_MID : BONE_SIDE;
                // Cut where its bite runs through it.
                double[] cuts = {a, Math.max(a, Math.min(b, -split)), Math.max(a, Math.min(b, split)), b};
                for (int c = 0; c < 3; c++) {
                    double l = cuts[c];
                    double r = cuts[c + 1];
                    if (r - l < 1.0E-4) {
                        continue;
                    }
                    boolean inside = c == 1;
                    Blocks to = y >= BITE + CURL || (y > BITE && inside) ? face : chin;
                    to.span(l, r, y0, y1, back, z, colour);
                }
            }
        }
    }

    /**
     * Its grin: a black backing between its teeth, its upper teeth hanging in a row in their red gum along its lip and its lower teeth
     * standing in theirs on its jaw, both rows curling up at its corners (the middle teeth the biggest); black lips over the gums, and
     * the creases of its grin curling up at its corners.
     */
    static void grin(Blocks face, Blocks chin) {
        double half = GRIN_HALF;
        double tw = 2.0 * half / TEETH;
        for (int i = 0; i < TEETH; i++) {
            double x = -half + tw * (i + 0.5);
            double big = 1.0 - 0.28 * Math.pow(Math.abs(x) / half, 2.0);
            double b = bite(x);
            double z = middle(b) + 0.02;
            // The black between them.
            face.box(new double[] {x, b + UPPER_TOOTH * 0.5, z - 0.03}, X, Y, Z, tw * 0.5, UPPER_TOOTH * 0.55, 0.02, INK);
            chin.box(new double[] {x, b - LOWER_TOOTH * 0.5, z - 0.03}, X, Y, Z, tw * 0.5, LOWER_TOOTH * 0.55, 0.02, INK);
            // The teeth: square, a gap between each.
            double hu = UPPER_TOOTH * big;
            double hl = LOWER_TOOTH * big;
            face.box(new double[] {x, b + 0.006 + hu * 0.5, z}, X, Y, Z, tw * 0.42, hu * 0.5, 0.03, TOOTH);
            chin.box(new double[] {x, b - 0.006 - hl * 0.5, z}, X, Y, Z, tw * 0.42, hl * 0.5, 0.03, TOOTH);
            // Their gums, and the black of its lips over them.
            face.box(new double[] {x, b + UPPER_TOOTH + GUM * 0.5, z - 0.005}, X, Y, Z, tw * 0.53, GUM * 0.5, 0.03, GUM_RED);
            chin.box(new double[] {x, b - LOWER_TOOTH - GUM * 0.5, z - 0.005}, X, Y, Z, tw * 0.53, GUM * 0.5, 0.03, GUM_RED);
            face.box(new double[] {x, b + UPPER_TOOTH + GUM + 0.011, z - 0.01}, X, Y, Z, tw * 0.55, 0.012, 0.03, INK);
            chin.box(new double[] {x, b - LOWER_TOOTH - GUM - 0.011, z - 0.01}, X, Y, Z, tw * 0.55, 0.012, 0.03, INK);
        }
        // The creases at its corners, curling up toward its eye.
        for (int s = -1; s <= 1; s += 2) {
            double cy = bite(half);
            line(face, new double[][] {{s * half, cy}, {s * (half + 0.045), cy + 0.05}, {s * (half + 0.06), cy + 0.13}}, 0.016, 0.02, INK);
        }
    }

    /** Its one eye: a black socket, rounded, a red iris standing out of it darker round its edge, a hot heart and a glint (burning). */
    static void eye(Blocks face) {
        double z = FACE;
        // (Each round thing two crossed blocks: an octagon from the front.)
        round(face, 0.0, 0.0, z + 0.012, 0.125, 0.012, SOCKET);
        round(face, 0.0, 0.0, z + 0.03, 0.1, 0.02, IRIS_DARK);
        round(face, 0.0, 0.004, z + 0.05, 0.082, 0.02, OwlModel.glow(OwlModel.IRIS));
        round(face, -0.01, 0.012, z + 0.064, 0.04, 0.01, OwlModel.glow(OwlModel.IRIS_HOT));
        face.box(new double[] {-0.036, 0.04, z + 0.074}, X, Y, Z, 0.017, 0.017, 0.006, OwlModel.glow(0xFFFFFFFF));
    }

    /** A round thing (from the front) at ({@code x}, {@code y}), {@code r} across, {@code t} thick, its front at {@code z}: two crossed blocks. */
    static void round(Blocks b, double x, double y, double z, double r, double t, int colour) {
        b.box(new double[] {x, y, z - t}, X, Y, Z, r, r * 0.42, t, colour);
        b.box(new double[] {x, y, z - t}, X, Y, Z, r * 0.42, r, t, colour);
        double d = r * 0.82;
        double[] diag = norm(new double[] {1.0, 1.0, 0.0});
        double[] anti = norm(new double[] {-1.0, 1.0, 0.0});
        b.box(new double[] {x, y, z - t}, diag, anti, Z, d, d * 0.5, t * 0.98, colour);
        b.box(new double[] {x, y, z - t}, anti, diag, Z, d, d * 0.5, t * 0.98, colour);
    }

    /**
     * Its brow and lines: a black zigzag right across its forehead over its eye (three sharp peaks each side, dipping to the bridge
     * between); two slashes up each temple; three wrinkles each side sweeping out and down from its eye.
     */
    static void brow(Blocks face) {
        int n = 12;
        double[][] zig = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double fx = -1.0 + 2.0 * i / n;
            double peak = i % 2 == 1 ? 0.065 : -0.02;
            zig[i] = new double[] {fx * 0.4, 0.33 + peak - 0.05 * (1.0 - Math.abs(fx)) + 0.04 * fx * fx};
        }
        line(face, zig, 0.03, 0.022, INK);
        for (int s = -1; s <= 1; s += 2) {
            for (int k = 0; k < 2; k++) {
                double y0 = 0.5 + 0.1 * k;
                line(face, new double[][] {{s * (0.24 + 0.03 * k), y0}, {s * (0.33 + 0.03 * k), y0 + 0.08}, {s * (0.4 + 0.02 * k), y0 + 0.17}}, 0.024, 0.02, INK);
            }
            for (int k = 0; k < 3; k++) {
                line(face, new double[][] {{s * 0.15, 0.08 - 0.07 * k}, {s * (0.25 + 0.01 * k), 0.07 - 0.09 * k}, {s * (0.34 + 0.02 * k), -0.01 - 0.1 * k}},
                    0.018 - 0.002 * k, 0.018, INK);
            }
        }
    }

    /** Its chin post's lines: a bar across under its lip, a line down its middle, two notches each side near its end. */
    static void chinPost(Blocks chin) {
        double z = FACE - 0.04;
        chin.box(new double[] {0.0, POST - 0.04, z + 0.008}, X, Y, Z, 0.1, 0.01, 0.012, INK);
        chin.box(new double[] {0.0, (POST - 0.06 + CHIN + 0.08) * 0.5, z + 0.008}, X, Y, Z, 0.01, (POST - 0.06 - CHIN - 0.08) * 0.5, 0.012, INK);
        for (double y : new double[] {CHIN + 0.16, CHIN + 0.1}) {
            for (int s = -1; s <= 1; s += 2) {
                chin.box(new double[] {s * 0.08, y, z + 0.008}, X, Y, Z, 0.03, 0.008, 0.012, INK);
            }
        }
    }

    /** A line of black blocks laid on its face through {@code pts} ({x, y}), {@code w} thick, tapering to {@code tip} at its end, each turned along it. */
    static void line(Blocks b, double[][] pts, double w, double tip, int colour) {
        int n = pts.length - 1;
        for (int i = 0; i < n; i++) {
            double[] a = pts[i];
            double[] c = pts[i + 1];
            double t = (i + 0.5) / n;
            double thick = w + (tip - w) * t;
            double mx = (a[0] + c[0]) * 0.5;
            double my = (a[1] + c[1]) * 0.5;
            double[] along = norm(new double[] {c[0] - a[0], c[1] - a[1], 0.0});
            double[] up = {-along[1], along[0], 0.0};
            double length = Math.hypot(c[0] - a[0], c[1] - a[1]) * 0.5 + thick * 0.5;
            b.box(new double[] {mx, my, front(mx, my) + 0.006}, along, up, Z, length, thick * 0.5, 0.012, colour);
        }
    }

    /**
     * A horn or prong: a chain of blocks from {@code root} along {@code dir} ({@code length} long), {@code r0} wide and {@code r1} deep
     * at its root, each turned along its curve (curling {@code curl} at its point) and thinner than the last; bone white, each block a
     * shade greyer than the one before it toward the point ({@code side}: which side, its flat faces turned to face out).
     */
    static void chain(Blocks b, double[] root, double[] dir, double length, double r0, double r1, double[] curl, int count, int side) {
        double[] d = norm(dir);
        double[][] spine = new double[count + 1][];
        for (int i = 0; i <= count; i++) {
            double u = i / (double)count;
            spine[i] = add(root, add(scale(d, length * u), scale(curl, u * u)));
        }
        for (int i = 0; i < count; i++) {
            double u = (i + 0.5) / count;
            double[] a = spine[i];
            double[] c = spine[i + 1];
            double[] along = norm(sub(c, a));
            double[] across = norm(cross(along, Z));
            if (dot(across, across) < 1.0E-6) {
                across = X;
            }
            double[] depth = cross(across, along);
            double taper = Math.pow(1.0 - u, 0.85) + 0.06;
            double seg = Math.sqrt(dot(sub(c, a), sub(c, a)));
            int colour = mix(BONE, BONE_SIDE, 0.5 * u);
            b.box(scale(add(a, c), 0.5), along, across, depth, seg * 0.56, r0 * taper, r1 * taper, colour);
        }
    }

    // ------------------------------------------------------------------ blocks

    static final double[] X = {1.0, 0.0, 0.0};
    static final double[] Y = {0.0, 1.0, 0.0};
    static final double[] Z = {0.0, 0.0, 1.0};

    /** Puts blocks (mask units), moved as their group is, where the frame says, to the sink. */
    static final class Blocks {
        final UkakuModel.Sink sink;
        final Frame f;
        final Move move;

        Blocks(UkakuModel.Sink sink, Frame f, Move move) {
            this.sink = sink;
            this.f = f;
            this.move = move;
        }

        /** An upright block from {@code l} to {@code r} across, {@code y0} to {@code y1} up, {@code back} to {@code front} out. */
        void span(double l, double r, double y0, double y1, double back, double front, int colour) {
            box(new double[] {(l + r) * 0.5, (y0 + y1) * 0.5, (back + front) * 0.5}, X, Y, Z, (r - l) * 0.5, (y1 - y0) * 0.5, (front - back) * 0.5, colour);
        }

        /** A block: its middle, its axes (unit), its half sizes along them; one colour (each face lit as it faces). */
        void box(double[] c, double[] ax, double[] ay, double[] az, double hx, double hy, double hz, int colour) {
            double[][] k = new double[8][];
            for (int i = 0; i < 8; i++) {
                double sx = (i & 1) == 0 ? -1.0 : 1.0;
                double sy = (i & 2) == 0 ? -1.0 : 1.0;
                double sz = (i & 4) == 0 ? -1.0 : 1.0;
                k[i] = this.f.at(this.move.at(add(c, add(scale(ax, sx * hx), add(scale(ay, sy * hy), scale(az, sz * hz))))));
            }
            double[] o = this.move.at(c);
            double[][] axes = {ax, ay, az};
            // Its six faces: -x +x -y +y -z +z, each wound to face out.
            int[][] faces = {{0, 4, 6, 2}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 6, 7, 3}, {0, 2, 3, 1}, {4, 5, 7, 6}};
            for (int i = 0; i < 6; i++) {
                double sgn = (i & 1) == 0 ? -1.0 : 1.0;
                double[] n = this.f.way(sub(this.move.at(add(c, scale(axes[i / 2], sgn))), o));
                int[] q = faces[i];
                this.sink.quad(k[q[0]], k[q[1]], k[q[2]], k[q[3]], n, colour, colour, colour, colour);
            }
        }
    }

    // ------------------------------------------------------------------ vectors

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
