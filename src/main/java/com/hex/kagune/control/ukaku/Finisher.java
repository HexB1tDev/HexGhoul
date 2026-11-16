package com.hex.kagune.control.ukaku;

/**
 * 4.2.9 (the owner, quoted exactly: "Has a finisher if the entity / player dies, they are stunned for 2 seconds and then their top half will
 * fall face forward on the ground, a blood splashes everywhere when this happens, the enemy player's kagune is also cut in perfect half at
 * the waist, if it's a bikaku or rinkaku, then it shatters. while the entity is stunned a perfect wound like (can be from the cut wounds we
 * already have in the game) really shallow will slowly appear around the entity's waist, when it reaches it's start thats when the top half
 * slides off."; then: "Up more around here, the hands/arms get chopped off too and land on the floor (the entire thing is meant to be an
 * adaptive ragdoll that adapts to any terrain, can roll, etc.", "just make a really thin red (bright and glowing) line around the whole area of
 * the cut", "the line should honestly now start in the very center, and the line flows from both sides at once", "the cut height is a fixed
 * thing"): the Steel Blade's finisher's rules (pure, tested). How long the line takes ({@link #STUN}, {@link #round}), the line round a body
 * not built like a player ({@link #line}), and what each cut shows ({@link #section}: a person's trunk, a person's limb, meat). The body's
 * pieces and the line round a player are {@code Carcass}'s; their fall is {@link Ragdoll}'s.
 */
public final class Finisher {
    /** How long the wound takes to go round (ticks): the two seconds it stands stunned. */
    public static final int STUN = 40;
    private Finisher() {
    }

    /** How far round the wound has gone {@code age} ticks into the stun (0..1): it creeps in, runs steady, and meets its start at {@link #STUN}. */
    public static double round(double age) {
        double t = clamp(age / STUN);
        return t * t * (3.0 - 2.0 * t) * 0.35 + t * 0.65;
    }

    /**
     * The point of a square-cornered band {@code u} (0..1) of the way round, walked at an even pace from its front middle toward its right, round
     * its back and its left and home: {x, z}, {@code halfWidth} by {@code halfDepth} from its middle.
     */
    public static double[] box(double u, double halfWidth, double halfDepth) {
        double w = halfWidth;
        double d = halfDepth;
        double[] legs = {w, 2.0 * d, 2.0 * w, 2.0 * d, w};
        double total = 4.0 * (w + d);
        double left = (u - Math.floor(u)) * total;
        for (int k = 0; k < legs.length; k++) {
            if (left <= legs[k] || k == legs.length - 1) {
                double t = legs[k] < 1.0E-12 ? 0.0 : Math.min(1.0, left / legs[k]);
                return switch (k) {
                    case 0 -> new double[] {w * t, d};
                    case 1 -> new double[] {w, d - 2.0 * d * t};
                    case 2 -> new double[] {w - 2.0 * w * t, -d};
                    case 3 -> new double[] {-w, -d + 2.0 * d * t};
                    default -> new double[] {-w + w * t, d};
                };
            }
            left -= legs[k];
        }
        return new double[] {0.0, d};
    }

    /**
     * The finisher's line round a body that isn't built like a player (the same thin glowing line, at the same fixed height): round its box
     * {@code halfWidth} by {@code halfDepth} at {@code height} (blocks; x its left, y up, z its front), drawn in from the middle of its front both
     * ways at once, {@code amount} (0..1) of the way to the middle of its back; {@code thick} its half thickness (blocks). Lit from straight above
     * (full bright).
     */
    public static void line(UkakuModel.Sink sink, double amount, double halfWidth, double halfDepth, double height, double thick, int argb) {
        if (amount <= 0.0) {
            return;
        }
        int steps = 96;
        double[] up = {0.0, 1.0, 0.0};
        for (int i = 0; i < steps; i++) {
            double[] a = box(i / (double)steps, halfWidth, halfDepth);
            double[] b = box((i + 1) / (double)steps, halfWidth, halfDepth);
            double f = Math.abs(Math.atan2((a[0] + b[0]) * 0.5, (a[1] + b[1]) * 0.5)) / Math.PI;
            if (f > amount) {
                continue;
            }
            double w = thick * Math.min(1.0, (amount - f) / 0.05 + (amount >= 1.0 ? 1.0 : 0.0));
            sink.quad(new double[] {-a[0], height - w, a[1]}, new double[] {-b[0], height - w, b[1]}, new double[] {-b[0], height + w, b[1]},
                new double[] {-a[0], height + w, a[1]}, up, argb, argb, argb, argb);
        }
    }

    /** True if a kagune of {@code path} (its name: RINKAKU, BIKAKU, UKAKU...) shatters when the body is cut through, rather than being cut with it. */
    public static boolean shatters(String path) {
        return "RINKAKU".equals(path) || "BIKAKU".equals(path);
    }

    /** The cut faces' grid (cells across and front to back) and the colours of what is inside ({@code WoundShape}'s and the meat's). */
    static final int CELLS_ACROSS = 24;
    static final int CELLS_DEEP = 12;
    static final int SKIN = 0xC9897C;
    static final int FAT = 0xE4C47C;
    static final int MUSCLE = 0x8E1822;
    static final int MUSCLE_LIGHT = 0xD9525A;
    static final int MUSCLE_DARK = 0x44060C;
    static final int BONE = 0xEADFC6;
    static final int CANAL = 0x3A0A10;
    static final int GUT = 0xD27884;
    static final int GUT_DARK = 0x8A3646;
    static final int FLESH = 0xB2222E;
    static final int FLESH_DEEP = 0x4A0810;

    /** What a cut face shows: a person's trunk (skin, fat, muscle, the spine, the guts), a person's limb (skin, muscle, the bone), or meat. */
    public enum Inside {
        BODY, LIMB, MEAT
    }

    /** One cell of a cut face: its colour and how far it stands out of the cut (blocks). */
    record Cell(int colour, double height) {
    }

    /**
     * What a cut face shows at cell {@code c} across (0 at its left, x growing to its right) and {@code r} front to back (0 at its back):
     * for a person's trunk ({@link Inside#BODY}) skin at the rim, fat under it (muscle at the back), the spine in the back's middle standing proud with its canal
     * dark behind it, the back's muscles either side, and the guts' coils bulging out of the front; for anything else meat, red and uneven.
     * {@code scale}: a pixel of the body (blocks), the heights in it.
     */
    static Cell cell(int c, int r, Inside inside, double scale, int seed) {
        boolean person = inside != Inside.MEAT;
        double nx = (c + 0.5) / CELLS_ACROSS * 2.0 - 1.0;
        double nz = (r + 0.5) / CELLS_DEEP * 2.0 - 1.0;
        int edge = Math.min(Math.min(c, CELLS_ACROSS - 1 - c), Math.min(r, CELLS_DEEP - 1 - r));
        double grain = noise(c, r, seed);
        double px = scale;
        if (!person) {
            // Meat: red through and through, darker and lighter in lumps, standing out unevenly; a darker rim.
            double lump = noise(c / 3, r / 3, seed + 17) * 0.6 + grain * 0.4;
            int colour = edge == 0 ? UkakuModel.mixInt(FLESH_DEEP, MUSCLE, grain)
                : lump > 0.7 ? UkakuModel.mixInt(MUSCLE_LIGHT, FLESH, (lump - 0.7) / 0.3)
                : lump > 0.35 ? UkakuModel.mixInt(FLESH, MUSCLE, (0.7 - lump) / 0.35) : UkakuModel.mixInt(MUSCLE, FLESH_DEEP, (0.35 - lump) / 0.35);
            return new Cell(colour, px * (edge == 0 ? 0.05 : 0.08 + 0.42 * lump * lump));
        }
        if (edge == 0) {
            return new Cell(UkakuModel.mixInt(SKIN, MUSCLE_LIGHT, grain * 0.25), px * 0.04);
        }
        if (inside == Inside.LIMB) {
            // A limb: muscle round the bone in its middle (a hard white ring, the marrow red in it).
            double bone = Math.pow(nx / 0.38, 2) + Math.pow(nz / 0.38, 2);
            if (bone < 0.45) {
                return new Cell(UkakuModel.mixInt(0xA8263A, 0xC45A4A, grain), px * 0.4);
            }
            if (bone < 1.0) {
                return new Cell(UkakuModel.mixInt(BONE, 0xCBD3D6, grain * 0.3), px * 0.48);
            }
            return new Cell(UkakuModel.mixInt(MUSCLE, MUSCLE_LIGHT, grain * 0.6), px * (0.14 + 0.08 * grain));
        }
        boolean back = nz < -0.2;
        if (edge == 1) {
            return back ? new Cell(UkakuModel.mixInt(MUSCLE_DARK, MUSCLE, grain), px * 0.1)
                : new Cell(UkakuModel.mixInt(FAT, 0xC49A55, grain * 0.6), px * 0.08);
        }
        if (edge == 2 && !back) {
            return new Cell(UkakuModel.mixInt(MUSCLE, MUSCLE_LIGHT, grain * 0.5), px * 0.14);
        }
        // The vertebra: its round body toward the middle (a harder rim round a spongy heart), the canal behind it, the spine's knob behind that.
        double body = Math.pow(nx / 0.25, 2) + Math.pow((nz + 0.3) / 0.27, 2);
        if (body < 1.0) {
            int bone = body > 0.55 ? UkakuModel.mixInt(BONE, 0xCBD3D6, grain * 0.3) : UkakuModel.mixInt(0xE8C9A8, 0xD49A86, grain * 0.5);
            return new Cell(bone, px * (0.62 - 0.1 * Math.max(0.0, 0.55 - body)));
        }
        double canal = Math.pow(nx / 0.1, 2) + Math.pow((nz + 0.66) / 0.12, 2);
        if (canal < 1.0) {
            return new Cell(CANAL, px * 0.05);
        }
        if (Math.abs(nx) < 0.07 && nz < -0.66) {
            return new Cell(UkakuModel.mixInt(BONE, 0xD8C8A8, grain * 0.4), px * 0.38);
        }
        if (canal < 2.6 && nz < -0.5) {
            return new Cell(UkakuModel.mixInt(BONE, 0xD8C8A8, grain * 0.5), px * 0.3); // its ring of bone round the canal
        }
        if (back) {
            return new Cell(UkakuModel.mixInt(MUSCLE, MUSCLE_DARK, grain * 0.7), px * (0.16 + 0.06 * grain));
        }
        // The guts: tubes coiled side by side, each a ring of gut round a dark middle, bulging out of the cut.
        double ux = nx * 3.4 + (r % 2) * 0.5;
        double fx = ux - Math.floor(ux) - 0.5;
        double fz = (nz + 0.2) * 2.6;
        fz = fz - Math.floor(fz) - 0.5;
        double ring = Math.sqrt(fx * fx + fz * fz * 1.6);
        if (ring < 0.17) {
            return new Cell(GUT_DARK, px * 0.22);
        }
        if (ring < 0.5) {
            return new Cell(UkakuModel.mixInt(GUT, 0xF0A0AA, (0.5 - ring) * 1.2 * (0.6 + 0.4 * grain)), px * (0.3 + 0.35 * (0.5 - ring) / 0.33));
        }
        return new Cell(UkakuModel.mixInt(0x6E0812, GUT_DARK, grain), px * 0.12);
    }

    /**
     * A cut face: the body {@code halfWidth} by {@code halfDepth} cut through at {@code height}, standing out of it upward ({@code up}: the legs' top)
     * or downward (the top half's underside): each cell of {@link #cell} a raised block, its sides down to the cut where it stands above its
     * neighbour. {@code scale}: a pixel of the body (blocks).
     */
    public static void section(UkakuModel.Sink sink, double halfWidth, double halfDepth, double height, boolean up, Inside inside, double scale,
                               int seed) {
        double sign = up ? 1.0 : -1.0;
        double cw = halfWidth * 2.0 / CELLS_ACROSS;
        double cd = halfDepth * 2.0 / CELLS_DEEP;
        double[][] tall = new double[CELLS_ACROSS][CELLS_DEEP];
        int[][] colour = new int[CELLS_ACROSS][CELLS_DEEP];
        for (int c = 0; c < CELLS_ACROSS; c++) {
            for (int r = 0; r < CELLS_DEEP; r++) {
                Cell cell = cell(c, r, inside, scale, seed);
                tall[c][r] = cell.height();
                colour[c][r] = cell.colour();
            }
        }
        double[] n = {0.0, sign, 0.0};
        for (int c = 0; c < CELLS_ACROSS; c++) {
            for (int r = 0; r < CELLS_DEEP; r++) {
                double x0 = -halfWidth + c * cw;
                double x1 = x0 + cw;
                double z0 = -halfDepth + r * cd;
                double z1 = z0 + cd;
                double y = height + sign * tall[c][r];
                int top = UkakuModel.argb(colour[c][r], 1.0);
                int side = UkakuModel.argb(UkakuModel.mixInt(colour[c][r], 0x000000, 0.35), 1.0);
                if (up) {
                    sink.quad(new double[] {x0, y, z0}, new double[] {x0, y, z1}, new double[] {x1, y, z1}, new double[] {x1, y, z0}, n, top, top, top, top);
                } else {
                    sink.quad(new double[] {x0, y, z0}, new double[] {x1, y, z0}, new double[] {x1, y, z1}, new double[] {x0, y, z1}, n, top, top, top, top);
                }
                // Its sides, down to whichever is lower beside it (the cut itself at the edge).
                double right = c + 1 < CELLS_ACROSS ? tall[c + 1][r] : 0.0;
                double left = c > 0 ? tall[c - 1][r] : 0.0;
                double front = r + 1 < CELLS_DEEP ? tall[c][r + 1] : 0.0;
                double behind = r > 0 ? tall[c][r - 1] : 0.0;
                side(sink, new double[] {x1, 0, z0}, new double[] {x1, 0, z1}, height, sign, tall[c][r], right, new double[] {1, 0, 0}, side);
                side(sink, new double[] {x0, 0, z1}, new double[] {x0, 0, z0}, height, sign, tall[c][r], left, new double[] {-1, 0, 0}, side);
                side(sink, new double[] {x1, 0, z1}, new double[] {x0, 0, z1}, height, sign, tall[c][r], front, new double[] {0, 0, 1}, side);
                side(sink, new double[] {x0, 0, z0}, new double[] {x1, 0, z0}, height, sign, tall[c][r], behind, new double[] {0, 0, -1}, side);
            }
        }
    }

    private static void side(UkakuModel.Sink sink, double[] a, double[] b, double height, double sign, double tall, double beside, double[] n, int colour) {
        if (tall <= beside + 1.0E-6) {
            return;
        }
        double hi = height + sign * tall;
        double lo = height + sign * beside;
        sink.quad(new double[] {a[0], lo, a[2]}, new double[] {b[0], lo, b[2]}, new double[] {b[0], hi, b[2]}, new double[] {a[0], hi, a[2]}, n, colour,
            colour, colour, colour);
    }

    /** A steady hash of a cell (0..1): the same face every frame. */
    static double noise(int c, int r, int seed) {
        long h = c * 73856093L ^ r * 19349663L ^ seed * 83492791L;
        h = (h ^ (h >>> 13)) * 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535.0;
    }

    /** Opaque {@code from} mixed {@code amount} (0..1) of the way to {@code to} (RGB). */
    public static int mix(int from, int to, double amount) {
        return UkakuModel.argb(UkakuModel.mixInt(from, to, clamp(amount)), 1.0);
    }

    static double clamp(double x) {
        return x < 0.0 ? 0.0 : x > 1.0 ? 1.0 : x;
    }

}
