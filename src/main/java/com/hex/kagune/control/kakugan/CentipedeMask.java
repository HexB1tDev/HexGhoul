package com.hex.kagune.control.kakugan;

import com.hex.kagune.control.ayato.UkakuModel;

/**
 * Kaneki's kakuja mask (3.1.0) in the centipede legs' colours: a crumpled, glossy plum shell over the forehead and his right
 * eye, its edge rippling in petals; a round black lens with a red pupil where his right eye is; his left eye left bare in a
 * window of skin; and a long ridged beak from the bridge of the nose down past his chin. Same space as {@link KanekiMask}
 * (head pixels: x to the player's left, y down from the neck, z backward, the face at z = -4); the lens and the window follow
 * the player's own eyes, and a player whose one ghoul eye is the right one wears it mirrored, so the window is always round
 * the eye that turns.
 */
public final class CentipedeMask {
    // The centipede legs' palette (CentipedeTail).
    static final int SHADOW = 0x22142A;
    static final int DARK = 0x36213C;
    static final int PLUM = 0x563A5E;
    static final int HI = 0x6F5078;
    static final int GLOSS = 0xD8C2E2;
    static final int LENS = 0x0B090E;
    static final int LENS_SHEEN = 0x5E5670;
    static final int PUPIL = 0x8E141A;
    static final int PUPIL_HOT = 0xD62C2A;

    /** The shell's back sits on the skin's outer layer. */
    static final double SKIN = 4.6;
    /** The default eyes' centres (head pixels). */
    static final double RIGHT_X = -2.0;
    static final double LEFT_X = 2.0;
    static final double EYE_Y = -3.0;
    /** The beak's tip below the chin, and how far it stands out at its deepest. */
    static final double BEAK_TIP = 6.4;
    static final double BEAK_DEPTH = 2.5;
    /** The window round the bare eye. */
    static final double WINDOW = 1.55;
    static final double LENS_RADIUS = 1.55;
    /** The light the gloss is baked for: from above, in front, a little to his left. */
    static final double[] SHINE = normalize(new double[] {0.3, -0.62, -0.72});

    private CentipedeMask() {
    }

    /** Where the eyes are on this player's face: {right x, right y, left x, left y} in head pixels. */
    static double[] eyes(KakuganEyes eyes) {
        KakuganEyes e = eyes.validated();
        double half = e.height() * 0.5;
        return new double[] {-4.0 + e.rightX() + 1.0, -8.0 + e.rightY() + half, -4.0 + e.leftX() + 1.0, -8.0 + e.leftY() + half};
    }

    /** Where the lens sits on this player's face (head pixels {x, y}): over the eye that stays human. Where the mask grows from. */
    public static double[] lens(KakuganEyes eyes) {
        KakuganEyes e = eyes.validated();
        double[] at = eyes(e);
        return e.ghoul() == KakuganEyes.RIGHT ? new double[] {at[2], at[3]} : new double[] {at[0], at[1]};
    }

    /** The mask's quads for a player whose eyes are {@code eyes}. */
    public static void build(UkakuModel.Sink sink, KakuganEyes eyes) {
        KakuganEyes e = eyes.validated();
        if (e.ghoul() == KakuganEyes.RIGHT) {
            // One eyed on the right: the mask built for the mirrored face, then mirrored back (x flipped, corners reversed).
            KakuganEyes mirrored = new KakuganEyes(KakuganEyes.FACE - KakuganEyes.WIDTH - e.leftX(), e.leftY(),
                KakuganEyes.FACE - KakuganEyes.WIDTH - e.rightX(), e.rightY(), e.height(), e.irisInside(), e.clearOuter());
            shape((a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(flip(d), flip(c), flip(b), flip(a), flip(n), cd, cc, cb, ca), mirrored);
            return;
        }
        shape(sink, e);
    }

    private static double[] flip(double[] v) {
        return new double[] {-v[0], v[1], v[2]};
    }

    private static void shape(UkakuModel.Sink sink, KakuganEyes eyes) {
        double[] at = eyes(eyes);
        Shape shape = new Shape(at[0] - RIGHT_X, at[1] - EYE_Y, at[2], at[3]);
        shell(sink, shape);
        lens(sink, shape);
        beak(sink, shape);
    }

    /** The shell's outline and surface for one face. */
    record Shape(double dx, double dy, double windowX, double windowY) {
        /** Inside the shell (positive) or out of it (negative), roughly in pixels. */
        double inside(double x, double y) {
            double u = x - this.dx;
            double v = y - this.dy;
            // Round the lens, the forehead, and the right cheek; the edge rippling in petals.
            double eye = 1.0 - Math.hypot((u + 1.9) / 3.15, (v + 3.4) / 3.5);
            double brow = 1.0 - Math.hypot((u - 0.1) / 5.0, (v + 6.8) / 2.6);
            double body = Math.max(eye * 3.2, brow * 2.4);
            double angle = Math.atan2(v + 4.6, u + 1.0);
            body += 0.42 * Math.sin(angle * 8.0 + 0.6) + 0.22 * Math.sin(angle * 13.0 + 2.1);
            // The bare eye's window.
            double window = Math.hypot(x - this.windowX, y - this.windowY) - WINDOW;
            double top = (y - (-8.4 + this.dy - 0.3 * Math.sin(x * 1.9 + 0.4) - 0.18 * Math.sin(x * 3.7 + 1.3))) * 2.0;
            double chin = (0.3 + this.dy - y) * 2.0;
            return Math.min(Math.min(body, window * 1.4), Math.min(top, chin));
        }

        /** How far the shell stands out of the skin at (x, y) (pixels), crumples included. */
        double depth(double x, double y) {
            double u = x - this.dx;
            double v = y - this.dy;
            double r = Math.hypot(u + 1.4, v + 4.8);
            double dome = 0.75 * Math.max(0.0, 1.0 - (r / 5.2) * (r / 5.2));
            return 0.12 + dome + crumple(u, v);
        }

        /** Crumples: folds radiating from the lens like petals, and a few running across. */
        double crumple(double u, double v) {
            double lu = u - RIGHT_X;
            double lv = v - EYE_Y;
            double r = Math.hypot(lu, lv);
            double a = Math.atan2(lv, lu);
            // The petals twist as they run out, and crumples run every way across them.
            double petals = Math.sin(a * 7.0 + r * 1.3 + 0.6 * Math.sin(a * 3.0)) * 0.22 * smooth(Math.min(1.0, Math.max(0.0, (r - LENS_RADIUS) / 1.6)));
            double across = 0.2 * Math.sin(1.1 * u + 1.9 * v + 0.7) * Math.sin(1.7 * u - 0.6 * v + 1.9)
                + 0.14 * Math.sin(2.6 * u - 1.3 * v + 2.2) * Math.sin(0.9 * u + 2.4 * v + 0.3);
            return petals + across;
        }
    }

    /** A point of the shell's surface at (x, y): bent back round the right temple and over the top of the head. */
    static double[] surface(Shape shape, double x, double y) {
        double z = -(SKIN + shape.depth(x, y));
        double px = x;
        double py = y;
        if (x < -3.5) {
            double d = -3.5 - x;
            px = -3.5 - d * 0.6;
            z += d * 0.9;
        }
        if (y < -7.9) {
            double d = -7.9 - y;
            py = -7.9 - d * 0.3;
            z += d * 1.4;
        }
        return new double[] {px, py, z};
    }

    /**
     * The shell as rings round the lens out to its rippling edge (the crumples radiate from the lens, so the mesh does too):
     * smooth edges, per-corner gloss, and a rolled lip with a wall down to the skin.
     */
    static void shell(UkakuModel.Sink sink, Shape shape) {
        int segments = 48;
        int rings = 7;
        double cx = RIGHT_X + shape.dx();
        double cy = EYE_Y + shape.dy();
        double inner = LENS_RADIUS + 0.35;
        double[][][] grid = new double[segments + 1][rings + 1][];
        int[][] colour = new int[segments + 1][rings + 1];
        double[] reach = new double[segments + 1];
        for (int k = 0; k <= segments; k++) {
            double angle = Math.PI * 2.0 * k / segments;
            double ca = Math.cos(angle);
            double sa = Math.sin(angle);
            // Out along the ray to where the shell first ends.
            double r = inner;
            while (r < 8.0 && shape.inside(cx + ca * (r + 0.05), cy + sa * (r + 0.05)) > 0.0) {
                r += 0.05;
            }
            reach[k] = r;
            for (int j = 0; j <= rings; j++) {
                // Rings packed toward the edge, where the petals ripple.
                double f = 1.0 - (1.0 - j / (double)rings) * (1.0 - j / (double)rings);
                double rr = inner + (Math.max(inner, r) - inner) * f;
                double x = cx + ca * rr;
                double y = cy + sa * rr;
                double[] point = surface(shape, x, y);
                if (j == rings) {
                    // The lip rolls back toward the skin.
                    point[2] += 0.18;
                }
                grid[k][j] = point;
                colour[k][j] = gloss(shape, x, y, j == rings);
            }
        }
        for (int k = 0; k < segments; k++) {
            if (reach[k] <= inner + 0.05 && reach[k + 1] <= inner + 0.05) {
                continue;
            }
            for (int j = 0; j < rings; j++) {
                double[] a = grid[k][j];
                double[] b = grid[k][j + 1];
                double[] c = grid[k + 1][j + 1];
                double[] d = grid[k + 1][j];
                double[] n = facing(normal(a, b, c, d), new double[] {0.0, 0.0, -1.0});
                sink.quad(a, b, c, d, n, colour[k][j], colour[k][j + 1], colour[k + 1][j + 1], colour[k + 1][j]);
            }
            wall(sink, shape, grid[k][rings], grid[k + 1][rings]);
        }
    }

    /** The shell's colour at (x, y): plum, darker in the folds, a pale gloss where the surface faces the light. */
    static int gloss(Shape shape, double x, double y, boolean lip) {
        double e = 0.08;
        double[] p = surface(shape, x, y);
        double[] px = surface(shape, x + e, y);
        double[] py = surface(shape, x, y + e);
        double[] n = facing(cross(sub(px, p), sub(py, p)), new double[] {0.0, 0.0, -1.0});
        double shine = Math.pow(Math.max(0.0, dot(n, SHINE)), 7.0);
        double fold = shape.crumple(x - shape.dx(), y - shape.dy());
        int base = mix(DARK, HI, clamp01(0.45 + fold * 1.5));
        int colour = mix(base, GLOSS, Math.min(1.0, shine * (lip ? 1.1 : 0.9)));
        return 0xFF000000 | colour;
    }

    static void wall(UkakuModel.Sink sink, Shape shape, double[] from, double[] to) {
        double[] backFrom = {from[0], from[1], -SKIN + 0.05};
        double[] backTo = {to[0], to[1], -SKIN + 0.05};
        double[] away = {(from[0] + to[0]) * 0.5 - (-1.0 + shape.dx()), (from[1] + to[1]) * 0.5 - (-4.6 + shape.dy()), 0.0};
        double[] n = facing(normal(from, to, backTo, backFrom), away);
        int argb = 0xFF000000 | SHADOW;
        sink.quad(from, to, backTo, backFrom, n, argb, argb, argb, argb);
    }

    /** The lens over his right eye: a raised plum socket, a glossy black dome, a red pupil with a black heart. */
    static void lens(UkakuModel.Sink sink, Shape shape) {
        double cx = RIGHT_X + shape.dx();
        double cy = EYE_Y + shape.dy();
        double base = -(SKIN + shape.depth(cx, cy)) - 0.1;
        int segments = 14;
        // Rings out from the centre: {radius, how far forward, colour}.
        double[][] rings = {
            {0.0, -1.0, 0}, {0.24, -0.99, 0}, {0.36, -0.98, 1}, {0.66, -0.93, 1}, {0.78, -0.9, 2}, {1.15, -0.7, 2},
            {LENS_RADIUS - 0.06, -0.22, 2}, {LENS_RADIUS + 0.22, -0.55, 3}, {LENS_RADIUS + 0.62, -0.15, 3}};
        for (int r = 0; r + 1 < rings.length; r++) {
            for (int k = 0; k < segments; k++) {
                double a0 = Math.PI * 2.0 * k / segments;
                double a1 = Math.PI * 2.0 * (k + 1) / segments;
                double[] p0 = ringPoint(cx, cy, base, rings[r], a0);
                double[] p1 = ringPoint(cx, cy, base, rings[r], a1);
                double[] q1 = ringPoint(cx, cy, base, rings[r + 1], a1);
                double[] q0 = ringPoint(cx, cy, base, rings[r + 1], a0);
                double[] n = facing(normal(p0, q0, q1, p1), new double[] {0.0, 0.0, -1.0});
                int colour = lensColour((int)rings[r + 1][2], n, (int)rings[r][2]);
                int argb = 0xFF000000 | colour;
                sink.quad(p0, q0, q1, p1, n, argb, argb, argb, argb);
            }
        }
    }

    static double[] ringPoint(double cx, double cy, double base, double[] ring, double angle) {
        return new double[] {cx + Math.cos(angle) * ring[0], cy + Math.sin(angle) * ring[0], base + ring[1]};
    }

    /** 0 the pupil's black heart, 1 the red pupil, 2 the black glass, 3 the plum socket. */
    static int lensColour(int part, double[] n, int inner) {
        double shine = Math.pow(Math.max(0.0, dot(n, SHINE)), 14.0);
        return switch (part) {
            case 0 -> mix(LENS, LENS_SHEEN, shine * 0.5);
            case 1 -> inner == 1 ? mix(PUPIL, PUPIL_HOT, 0.35 + 0.5 * shine) : mix(PUPIL, LENS, 0.4);
            case 2 -> inner == 1 ? mix(PUPIL, LENS, 0.7) : mix(LENS, LENS_SHEEN, shine);
            default -> mix(mix(DARK, HI, 0.5), GLOSS, shine * 0.6);
        };
    }

    /**
     * The beak: from the bridge of his nose down past the chin, a keeled ridge with two faces sloping back to the face, its
     * left side flaring over his left cheek below the bare eye; it narrows to a point that hangs in front of his chest.
     */
    static void beak(UkakuModel.Sink sink, Shape shape) {
        int stations = 14;
        double top = EYE_Y + 0.6 + shape.dy();
        double tip = BEAK_TIP + shape.dy();
        double[][][] rings = new double[stations + 1][][];
        for (int i = 0; i <= stations; i++) {
            double u = i / (double)stations;
            double y = top + (tip - top) * u;
            double x = 0.05 + shape.dx() * 0.5 + 0.45 * u * u;
            // Wide on the face, narrowing below the chin to the point.
            double below = Math.max(0.0, y / tip);
            double half = 1.55 * (1.0 - 0.15 * u) * Math.pow(Math.max(0.0, 1.0 - below), 0.75) + 0.03;
            double flare = 1.0 + 0.75 * Math.exp(-Math.pow((y + 0.9) / 1.0, 2.0));
            double out = BEAK_DEPTH * Math.sin(Math.PI * Math.min(1.0, 0.18 + 0.82 * u)) * 0.7 + 0.6 + 0.9 * u;
            double wrinkle = 0.14 * Math.sin(u * 17.0 + 0.8);
            double ridgeZ = -(SKIN + out + wrinkle);
            // The back: on the face, under the skin; below the chin it comes forward to close the beak.
            double backZ = y < 0.0 ? -SKIN + 0.2 : -(SKIN + Math.min(out - 0.35, 0.3 + 1.6 * below));
            double sideZ = (ridgeZ + backZ) * 0.5 - 0.35;
            if (i == stations) {
                double[] point = {x, y, ridgeZ + 0.3};
                rings[i] = new double[][] {point, point, point, point};
            } else {
                rings[i] = new double[][] {{x, y, ridgeZ}, {x + half * flare, y, sideZ}, {x, y, backZ}, {x - half, y, sideZ}};
            }
        }
        for (int i = 0; i < stations; i++) {
            for (int k = 0; k < 4; k++) {
                int j = (k + 1) % 4;
                double[] a = rings[i][k];
                double[] b = rings[i + 1][k];
                double[] c = rings[i + 1][j];
                double[] d = rings[i][j];
                // Away from the beak's own middle at this station.
                double[] middle = {(rings[i][0][0] + rings[i][2][0]) * 0.5, rings[i][0][1], (rings[i][0][2] + rings[i][2][2]) * 0.5};
                double[] face = {(a[0] + b[0] + c[0] + d[0]) * 0.25 - middle[0], 0.0, (a[2] + b[2] + c[2] + d[2]) * 0.25 - middle[2]};
                double[] n = facing(normal(a, b, c, d), face);
                double shine = Math.pow(Math.max(0.0, dot(n, SHINE)), 9.0);
                // The ridge's two faces lit, the underside dark; gloss down the ridge.
                int base = k == 0 ? HI : k == 3 ? PLUM : DARK;
                int colour = mix(base, GLOSS, shine * 0.85);
                int argb = 0xFF000000 | colour;
                sink.quad(a, b, c, d, n, argb, argb, argb, argb);
            }
        }
    }

    static double[] normal(double[] a, double[] b, double[] c, double[] d) {
        double[] e1 = {c[0] - a[0], c[1] - a[1], c[2] - a[2]};
        double[] e2 = {d[0] - b[0], d[1] - b[1], d[2] - b[2]};
        double[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
        double length = Math.sqrt(dot(n, n));
        if (length < 1.0E-12) {
            return new double[] {0.0, 0.0, -1.0};
        }
        return new double[] {n[0] / length, n[1] / length, n[2] / length};
    }

    /** {@code n}, turned to face the side {@code toward} points to. */
    static double[] facing(double[] n, double[] toward) {
        return dot(n, toward) < 0.0 ? new double[] {-n[0], -n[1], -n[2]} : n;
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    static double smooth(double value) {
        double t = clamp01(value);
        return t * t * (3.0 - 2.0 * t);
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] cross(double[] a, double[] b) {
        double[] n = {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
        double length = Math.sqrt(dot(n, n));
        return length < 1.0E-12 ? new double[] {0.0, 0.0, -1.0} : new double[] {n[0] / length, n[1] / length, n[2] / length};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return new double[] {a[0] / length, a[1] / length, a[2] / length};
    }

    static int mix(int from, int to, double amount) {
        double t = clamp01(amount);
        int r = (int)Math.round((from >> 16 & 0xFF) + t * ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)));
        int g = (int)Math.round((from >> 8 & 0xFF) + t * ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)));
        int b = (int)Math.round((from & 0xFF) + t * ((to & 0xFF) - (from & 0xFF)));
        return r << 16 | g << 8 | b;
    }
}
