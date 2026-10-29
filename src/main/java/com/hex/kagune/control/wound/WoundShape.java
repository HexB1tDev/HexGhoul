package com.hex.kagune.control.wound;

import java.util.ArrayList;
import java.util.List;

/**
 * A wound's geometry on its part (pure, tested; 3.0.0, rebuilt in 5.0.17), in the part's own pixels. A wound is carved, not
 * stamped: the renderer draws its {@link Role#WALL walls}, {@link Role#FLOOR floor} and the {@link Role#BONE bones and guts} it
 * reaches inside the body first, then writes its {@link Role#OPENING opening} into the depth buffer just nearer than the skin's
 * outer layer, so when the body is drawn its skin inside the opening fails the depth test and the inside shows through (the hole
 * technique of Hex God of Time's Scepter). Whatever is drawn inside but outside the opening stays hidden under the skin, so a
 * rib crossing a gash shows only where the gash is. The {@link Role#RIM rim}, {@link Role#BLOOD blood} and {@link Role#DECAL
 * marks on the skin} (bruising, scorching, smeared blood) are laid over the skin after.
 *
 * <p>The walls show the layers a wound cuts through: the skin, a band of yellow fat, then red muscle down to the floor. What
 * the floor shows depends on where it is ({@link Anatomy}): the ribs over the dark lungs through the chest, the guts through the
 * belly, the spine and shoulder blades through the back, the long bones along the limbs, the skull under the scalp. A fracture's
 * broken bone stands out of the skin. As a wound closes ({@code open} toward 0) it narrows and its floor rises, so the flesh
 * grows back over the bone first, turning the pink of new tissue, until it is gone; a bone out of the skin sinks back in first.
 */
public final class WoundShape {
    /** What a polygon is for. */
    public enum Role {
        WALL, FLOOR, OPENING, RIM, BLOOD, BONE, DECAL
    }

    /** Receives polygons (fans from their first corner, in order round it) in part pixels, with a colour per corner (ARGB). */
    public interface Sink {
        void polygon(Role role, double[][] points, int[] argb);
    }

    /** The cut skin under the outer layer (pale dermis), the fat under it, then the flesh. */
    static final int DERMIS = 0xC9897C;
    static final int FAT = 0xE4C47C;
    static final int FAT_DEEP = 0xC49A55;
    static final int FLESH = 0xB2222E;
    static final int FLESH_DEEP = 0x4A0810;
    static final int MUSCLE = 0x8E1822;
    static final int MUSCLE_LIGHT = 0xD9525A;
    static final int MUSCLE_DARK = 0x44060C;
    static final int HOLE_DEEP = 0x0A0204;
    static final int NEW_TISSUE = 0xE59AA0;
    static final int WET = 0x5C0610;
    static final int BLOOD = 0x6E0812;
    static final int LUNG = 0x7E2434;
    static final int LUNG_DARK = 0x3A0C16;
    static final int BELLY = 0x6A1622;
    static final int BONE = 0xEADFC6;
    static final int CARTILAGE = 0xCBD3D6;
    static final int GUT = 0xD27884;
    static final int CHAR = 0x1C110C;
    static final int CHAR_BROWN = 0x4E2C17;
    static final int INFLAMED = 0xB0453E;
    static final int RAW = 0xC23A44;
    static final int BLISTER = 0xE8D7A0;
    static final int BRUISE = 0x4A1838;
    static final int BRUISE_OLD = 0x8C8A42;
    /** Room left between a wound and the edge of its side (pixels). */
    static final double MARGIN = 0.12;
    /** How far a broken bone stands out of the skin, fully open (pixels). */
    public static final double SHARD_OUT = 1.9;

    private WoundShape() {
    }

    /** How deep a wound of this kind goes, fully open, into {@code part} this thick under it (pixels; never past the middle). */
    public static double depth(Wound.Kind kind, double thickness, Wound.Part part) {
        double half = thickness * 0.48;
        double d = switch (kind) {
            case CUT -> 0.45;
            case TEAR -> Math.min(1.1, thickness * 0.32);
            case GASH, CAVITY -> Math.min(2.0, half);
            case HOLE -> Math.min(1.9, thickness * 0.46);
            case FRACTURE -> Math.min(1.6, thickness * 0.42);
            case BURN -> 0.24;
            case BRUISE -> 0.0;
        };
        // Under the scalp the skull: a head wound goes down to it, no further.
        return part == Wound.Part.HEAD && kind != Wound.Kind.BURN ? Math.min(d, kind == Wound.Kind.CUT ? 0.35 : 0.62) : d;
    }

    /** How deep the floor is at {@code open} (it rises as the wound closes, the flesh growing back over the bone). */
    static double floorDepth(Wound w, double open, double[] box) {
        double d = depth(w.kind(), w.face().thickness(box), w.part());
        return w.kind() == Wound.Kind.CUT ? d * (0.5 + 0.5 * open) : d * (0.08 + 0.92 * Math.pow(open, 1.8));
    }

    /** True for the kinds the skin is opened by (a bruise is only a mark on it). */
    public static boolean opens(Wound.Kind kind) {
        return kind != Wound.Kind.BRUISE;
    }

    /** The wound's outline on its side ({u, v} pairs round it), {@code open} wide, kept inside the side. */
    public static double[][] outline(Wound w, double open, double[] box) {
        double[] side = w.face().size(box);
        double o = clamp(open, 0.0, 1.0);
        int n = switch (w.kind()) {
            case CUT -> 14;
            case HOLE -> 12;
            case GASH, CAVITY -> 22;
            default -> 16;
        };
        double c = Math.cos(w.angle());
        double s = Math.sin(w.angle());
        double half = w.length() * 0.5;
        double across = w.width() * 0.5;
        double[][] out = new double[n][];
        for (int k = 0; k < n; k++) {
            double t = 2.0 * Math.PI * k / n;
            double jag = hash(w.seed() * 31 + k) - 0.5;
            double a;
            double b;
            switch (w.kind()) {
                case CUT -> {
                    // A lens: long and pointed, opening in the middle.
                    a = Math.cos(t) * half * (0.75 + 0.25 * o);
                    b = Math.sin(t) * across * o * Math.pow(Math.abs(Math.sin(t)), 0.15);
                }
                case GASH -> {
                    // A long lens laid wide open, its lips torn ragged.
                    double ragged = 1.0 + 0.2 * jag;
                    a = Math.cos(t) * half * (0.82 + 0.18 * o) * (1.0 + 0.04 * jag);
                    b = Math.sin(t) * across * Math.pow(o, 0.85) * Math.pow(Math.abs(Math.sin(t)), 0.12) * ragged;
                }
                case TEAR, FRACTURE -> {
                    // A ragged oval: its edge torn in and out.
                    double ragged = 1.0 + 0.34 * jag + 0.1 * Math.sin(t * 5.0 + w.seed());
                    a = Math.cos(t) * half * (0.6 + 0.4 * o) * ragged;
                    b = Math.sin(t) * across * o * ragged;
                }
                case CAVITY -> {
                    // Flesh torn out: a big ragged crater, its edge bitten in and out.
                    double ragged = 1.0 + 0.3 * jag + 0.12 * Math.sin(t * 3.0 + w.seed()) + 0.07 * Math.sin(t * 7.0 + w.seed() * 2.0);
                    double grow = 0.4 + 0.6 * o;
                    a = Math.cos(t) * half * grow * ragged;
                    b = Math.sin(t) * across * grow * ragged;
                }
                case BURN, BRUISE -> {
                    double blob = 1.0 + 0.12 * Math.sin(t * 3.0 + w.seed()) + 0.1 * jag;
                    double grow = w.kind() == Wound.Kind.BURN ? 0.55 + 0.45 * o : 1.0;
                    a = Math.cos(t) * half * grow * blob;
                    b = Math.sin(t) * across * grow * blob;
                }
                default -> {
                    double r = across * Math.sqrt(o) * (1.0 + 0.1 * jag);
                    a = Math.cos(t) * r;
                    b = Math.sin(t) * r;
                }
            }
            double u = w.u() + a * c - b * s;
            double v = w.v() + a * s + b * c;
            out[k] = new double[] {clamp(u, MARGIN, side[0] - MARGIN), clamp(v, MARGIN, side[1] - MARGIN)};
        }
        return out;
    }

    /** All of a wound's geometry at {@code open} (0 closed .. 1 fully open), on a part whose box is {@code box}. */
    public static void build(Wound w, double open, double[] box, Sink sink) {
        if (open <= 0.02) {
            return;
        }
        double o = clamp(open, 0.0, 1.0);
        double heal = 1.0 - o;
        double[][] rim2 = outline(w, o, box);
        double[] centre = centre(rim2);
        if (w.kind() == Wound.Kind.BRUISE) {
            bruise(w, rim2, centre, o, box, 1.0, sink);
            return;
        }
        Wound.Face face = w.face();
        double outer = w.part().outer();
        double deep = floorDepth(w, o, box);
        int n = rim2.length;
        double inset = switch (w.kind()) {
            case HOLE -> 0.72;
            case CUT -> 0.8;
            case BURN -> 0.9;
            default -> 0.84;
        };
        double[][] floor2 = new double[n][];
        for (int k = 0; k < n; k++) {
            floor2[k] = toward(centre, rim2[k], inset);
        }
        walls(w, rim2, floor2, centre, deep, heal, box, sink);
        floor(w, floor2, centre, deep, heal, box, sink);
        if (w.kind() != Wound.Kind.CUT && w.kind() != Wound.Kind.BURN) {
            inside(w, rim2, floor2, deep, box, sink);
        }
        if (w.kind() == Wound.Kind.FRACTURE) {
            shard(w, o, box, sink);
        }
        // The opening: where the skin is cut away, on the outer layer's surface (the renderer brings it just nearer).
        double[][] opening = new double[n + 2][];
        opening[0] = face.point(box, centre[0], centre[1], outer);
        for (int k = 0; k <= n; k++) {
            opening[k + 1] = face.point(box, rim2[k % n][0], rim2[k % n][1], outer);
        }
        sink.polygon(Role.OPENING, opening, new int[n + 2]);
        if (w.kind() == Wound.Kind.BURN) {
            scorch(w, rim2, centre, o, box, sink);
            return;
        }
        rim(w, rim2, centre, o, box, outer, sink);
        if (w.kind() == Wound.Kind.GASH || w.kind() == Wound.Kind.CAVITY || w.kind() == Wound.Kind.FRACTURE || w.kind() == Wound.Kind.TEAR) {
            // The skin round a wound this brutal is bruised dark.
            bruise(w, ring(w.face(), rim2, centre, 1.0, 0.9, box), centre, o, box, w.kind() == Wound.Kind.TEAR ? 0.3 : 0.55, sink);
        }
        blood(w, rim2, o, box, outer, sink);
    }

    /** The walls, layer by layer down from the outer layer: the cut skin, the fat, then the muscle to the floor. */
    private static void walls(Wound w, double[][] rim2, double[][] floor2, double[] centre, double deep, double heal, double[] box, Sink sink) {
        Wound.Face face = w.face();
        double outer = w.part().outer();
        int n = rim2.length;
        boolean hole = w.kind() == Wound.Kind.HOLE;
        boolean burn = w.kind() == Wound.Kind.BURN;
        double fat = w.kind() == Wound.Kind.CUT || burn ? 0.0 : Math.min(0.24, deep * 0.2);
        int dermis = opaque(shade(burn ? CHAR_BROWN : DERMIS, heal * 0.3));
        int fatTop = opaque(shade(FAT, heal * 0.4));
        int fatBottom = opaque(shade(FAT_DEEP, heal * 0.4));
        int top = shade(hole ? 0x7A101A : burn ? RAW : FLESH, heal);
        int bottom = shade(hole ? HOLE_DEEP : FLESH_DEEP, heal * 0.6);
        double[][] mid = new double[n][];
        for (int k = 0; k < n; k++) {
            mid[k] = toward(rim2[k], floor2[k], deep <= 1.0E-6 ? 0.0 : fat / deep);
        }
        for (int k = 0; k < n; k++) {
            int m = (k + 1) % n;
            double[] a0 = face.point(box, rim2[k][0], rim2[k][1], outer);
            double[] b0 = face.point(box, rim2[m][0], rim2[m][1], outer);
            double[] a1 = face.point(box, rim2[k][0], rim2[k][1], 0.0);
            double[] b1 = face.point(box, rim2[m][0], rim2[m][1], 0.0);
            sink.polygon(Role.WALL, new double[][] {a0, b0, b1, a1}, new int[] {dermis, dermis, dermis, dermis});
            double[] a2 = a1;
            double[] b2 = b1;
            if (fat > 0.02) {
                a2 = face.point(box, mid[k][0], mid[k][1], -fat);
                b2 = face.point(box, mid[m][0], mid[m][1], -fat);
                sink.polygon(Role.WALL, new double[][] {a1, b1, b2, a2}, new int[] {fatTop, fatTop, fatBottom, fatBottom});
            }
            // The muscle, its fibres catching the light in bands.
            int band = w.kind() == Wound.Kind.CUT || hole ? top : mix(top, k % 2 == 0 ? MUSCLE_LIGHT : MUSCLE_DARK, 0.25 * hash(w.seed() * 5 + k));
            double[] a3 = face.point(box, floor2[k][0], floor2[k][1], -deep);
            double[] b3 = face.point(box, floor2[m][0], floor2[m][1], -deep);
            sink.polygon(Role.WALL, new double[][] {a2, b2, b3, a3}, new int[] {opaque(band), opaque(band), opaque(bottom), opaque(bottom)});
        }
    }

    /** What lies at the bottom of the wound there: lungs in the chest, the belly's lining, the skull, else muscle. */
    enum Bottom {
        MUSCLE, LUNG, BELLY, SKULL
    }

    static Bottom bottom(Wound w) {
        if (w.part() == Wound.Part.HEAD) {
            return Bottom.SKULL;
        }
        if (w.part() == Wound.Part.BODY && w.face() != Wound.Face.BACK) {
            return w.v() < 7.3 ? Bottom.LUNG : w.face() == Wound.Face.FRONT ? Bottom.BELLY : Bottom.MUSCLE;
        }
        return Bottom.MUSCLE;
    }

    /** The floor: dark for a puncture, raw and blistered for a burn, else what lies there, in bands of fibre along the wound. */
    private static void floor(Wound w, double[][] floor2, double[] centre, double deep, double heal, double[] box, Sink sink) {
        Wound.Face face = w.face();
        int n = floor2.length;
        if (w.kind() == Wound.Kind.HOLE || w.kind() == Wound.Kind.CUT || w.kind() == Wound.Kind.BURN) {
            int colour = opaque(shade(w.kind() == Wound.Kind.HOLE ? HOLE_DEEP : w.kind() == Wound.Kind.BURN ? RAW : 0x5A0A14, heal * 0.7));
            sink.polygon(Role.FLOOR, star(face, box, centre, floor2, -deep), fill(n + 2, colour));
            if (w.kind() == Wound.Kind.BURN) {
                blisters(w, floor2, centre, deep, box, sink);
            }
            return;
        }
        Bottom bottom = bottom(w);
        if (bottom == Bottom.SKULL && deep > 0.45) {
            // Down to the skull: bone, split by a dark crack.
            sink.polygon(Role.FLOOR, star(face, box, centre, floor2, -deep), fill(n + 2, opaque(shade(BONE, heal * 0.5))));
            double[] from = floor2[(int)(hash(w.seed() * 43) * n) % n];
            double[] to = toward(from, centre, 1.6);
            double[] crack = {-(to[1] - from[1]), to[0] - from[0]};
            double l = Math.max(1.0E-6, Math.sqrt(crack[0] * crack[0] + crack[1] * crack[1]));
            double half = 0.06 / l;
            int dark = opaque(shade(0x3A1A14, heal * 0.5));
            sink.polygon(Role.FLOOR, new double[][] {face.point(box, from[0] + crack[0] * half, from[1] + crack[1] * half, -deep + 0.01),
                face.point(box, to[0] + crack[0] * half, to[1] + crack[1] * half, -deep + 0.01), face.point(box, to[0] - crack[0] * half,
                to[1] - crack[1] * half, -deep + 0.01), face.point(box, from[0] - crack[0] * half, from[1] - crack[1] * half, -deep + 0.01)},
                fill(4, dark));
            return;
        }
        if ((bottom == Bottom.LUNG || bottom == Bottom.BELLY) && deep <= 0.9) {
            bottom = Bottom.MUSCLE;
        }
        if (bottom == Bottom.LUNG || bottom == Bottom.BELLY) {
            int light = bottom == Bottom.LUNG ? LUNG : BELLY;
            int[] colours = new int[n + 2];
            colours[0] = opaque(shade(mix(light, 0xFFFFFF, 0.08), heal * 0.5));
            for (int k = 1; k < n + 2; k++) {
                colours[k] = opaque(shade(bottom == Bottom.LUNG ? LUNG_DARK : FLESH_DEEP, heal * 0.5));
            }
            sink.polygon(Role.FLOOR, star(face, box, centre, floor2, -deep), colours);
            return;
        }
        // Bands across the wound, the fibres running along it.
        double c = Math.cos(w.angle());
        double s = Math.sin(w.angle());
        double[] across = {-s, c};
        double lo = Double.MAX_VALUE;
        double hi = -Double.MAX_VALUE;
        for (double[] p : floor2) {
            double d = (p[0] - centre[0]) * across[0] + (p[1] - centre[1]) * across[1];
            lo = Math.min(lo, d);
            hi = Math.max(hi, d);
        }
        int bands = 7;
        for (int b = 0; b < bands; b++) {
            double from = lo + (hi - lo) * b / bands;
            double to = lo + (hi - lo) * (b + 1) / bands;
            List<double[]> piece = new ArrayList<>(List.of(floor2));
            piece = clip(piece, across[0], across[1], -(from + centre[0] * across[0] + centre[1] * across[1]), true);
            piece = clip(piece, across[0], across[1], to + centre[0] * across[0] + centre[1] * across[1], false);
            if (piece.size() < 3) {
                continue;
            }
            int base = b % 2 == 0 ? MUSCLE_LIGHT : MUSCLE_DARK;
            base = mix(base, MUSCLE, 0.4 * hash(w.seed() * 7 + b));
            double[][] ring = piece.toArray(new double[0][]);
            sink.polygon(Role.FLOOR, star(face, box, centre(ring), ring, -deep), fill(ring.length + 2, opaque(shade(base, heal))));
        }
    }

    /** A burn's floor: yellow-white blisters in the raw flesh. */
    private static void blisters(Wound w, double[][] floor2, double[] centre, double deep, double[] box, Sink sink) {
        int count = 3 + (int)(hash(w.seed() * 3) * 3);
        for (int i = 0; i < count; i++) {
            double[] at = toward(centre, floor2[(int)(hash(w.seed() * 11 + i) * floor2.length) % floor2.length], 0.2 + 0.6 * hash(w.seed() * 13 + i));
            double r = 0.12 + 0.12 * hash(w.seed() * 17 + i);
            double[][] dot = new double[6][];
            for (int k = 0; k < 6; k++) {
                double t = Math.PI * 2.0 * k / 6.0;
                dot[k] = new double[] {at[0] + Math.cos(t) * r, at[1] + Math.sin(t) * r};
            }
            sink.polygon(Role.FLOOR, star(w.face(), box, at, dot, -deep + 0.02), fill(8, opaque(BLISTER)));
        }
    }

    /**
     * The bones and guts the wound reaches ({@link Anatomy}): every piece within the wound's reach, cut down to the stretch of it
     * near the opening (what lies beyond is under the skin anyway), shaded by how it faces out of the wound.
     */
    private static void inside(Wound w, double[][] rim2, double[][] floor2, double deep, double[] box, Sink sink) {
        Wound.Face face = w.face();
        double outer = w.part().outer();
        boolean slim = slim(w.part(), box);
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double[] p : rim2) {
            grow(lo, hi, face.point(box, p[0], p[1], outer));
        }
        for (double[] p : floor2) {
            grow(lo, hi, face.point(box, p[0], p[1], -deep));
        }
        for (int i = 0; i < 3; i++) {
            lo[i] -= 0.9;
            hi[i] += 0.9;
        }
        double[] normal = face.normal();
        double[] origin = face.point(box, 0.0, 0.0, 0.0);
        for (Anatomy.Piece piece : Anatomy.of(w.part(), slim)) {
            double[] span = Anatomy.within(piece, lo, hi);
            if (span == null) {
                continue;
            }
            Anatomy.Piece shown = piece.cut(span[0], span[1]);
            double nearest = Double.MAX_VALUE;
            for (double[] corner : shown.corners()) {
                nearest = Math.min(nearest, -Anatomy.dot(Anatomy.sub(corner, origin), normal));
            }
            if (nearest > deep) {
                continue; // under the floor: unseen
            }
            int colour = switch (piece.tissue()) {
                case BONE -> BONE;
                case CARTILAGE -> CARTILAGE;
                case GUT -> GUT;
            };
            box(shown, colour, normal, w.seed() + piece.name().hashCode(), sink);
        }
    }

    /** One piece as six faces, each lit by how it faces out of the wound, the bone a little bloodied and mottled. */
    static void box(Anatomy.Piece piece, int colour, double[] out, int seed, Sink sink) {
        double[][] c = piece.corners();
        int[][] faces = {{0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}, {0, 3, 2, 1}, {4, 5, 6, 7}};
        double[] middle = {0.0, 0.0, 0.0};
        for (double[] p : c) {
            middle = Anatomy.add(middle, Anatomy.scale(p, 1.0 / 8.0));
        }
        for (int f = 0; f < faces.length; f++) {
            double[][] quad = {c[faces[f][0]], c[faces[f][1]], c[faces[f][2]], c[faces[f][3]]};
            double[] centre = Anatomy.scale(Anatomy.add(Anatomy.add(quad[0], quad[1]), Anatomy.add(quad[2], quad[3])), 0.25);
            double[] facing = Anatomy.sub(centre, middle);
            double length = Anatomy.norm(facing);
            double light = length < 1.0E-9 ? 0.7 : 0.62 + 0.38 * Math.max(0.0, Anatomy.dot(facing, out) / length);
            int[] colours = new int[4];
            for (int k = 0; k < 4; k++) {
                int mottled = mix(colour, BLOOD, 0.03 + 0.1 * hash(seed * 29 + f * 4 + k));
                colours[k] = opaque(scaled(mottled, light));
            }
            sink.polygon(Role.BONE, quad, colours);
        }
    }

    /**
     * A fracture's broken bone: the bone under the wound ({@link Anatomy#under}), snapped, its jagged end standing out of the skin
     * at a slant along it, bloody where it comes out. It sinks back in as the wound closes (the bone set before the skin shuts).
     */
    private static void shard(Wound w, double open, double[] box, Sink sink) {
        boolean slim = slim(w.part(), box);
        Anatomy.Piece bone = Anatomy.under(w.part(), w.face(), w.u(), w.v(), slim);
        if (bone == null) {
            return;
        }
        double[] normal = w.face().normal();
        double[] surface = w.face().point(box, w.u(), w.v(), 0.0);
        double[] base = Anatomy.closest(bone, surface);
        double[] along = Anatomy.sub(bone.b(), bone.a());
        along = Anatomy.scale(along, 1.0 / Math.max(1.0E-9, Anatomy.norm(along)));
        along = Anatomy.sub(along, Anatomy.scale(normal, Anatomy.dot(along, normal)));
        double alongLength = Anatomy.norm(along);
        along = alongLength < 1.0E-6 ? Anatomy.perpendicular(normal) : Anatomy.scale(along, 1.0 / alongLength);
        // Slant toward the middle of the part, so it stays over its own side.
        double[] middle = {(box[0] + box[3]) * 0.5, (box[1] + box[4]) * 0.5, (box[2] + box[5]) * 0.5};
        if (Anatomy.dot(Anatomy.sub(middle, base), along) < 0.0) {
            along = Anatomy.scale(along, -1.0);
        }
        double[] way = Anatomy.add(normal, Anatomy.scale(along, 0.85));
        way = Anatomy.scale(way, 1.0 / Anatomy.norm(way));
        double reach = -Anatomy.dot(Anatomy.sub(base, surface), normal) + w.part().outer() + SHARD_OUT * Math.pow(open, 1.5);
        double length = reach / Math.max(0.3, Anatomy.dot(way, normal));
        double[] tip = Anatomy.add(base, Anatomy.scale(way, length));
        double width = Math.max(0.45, Math.max(bone.wide(), bone.flat()) * 1.1);
        double[] side = Anatomy.cross(way, along);
        side = Anatomy.scale(side, 1.0 / Math.max(1.0E-9, Anatomy.norm(side)));
        double[] across = Anatomy.cross(side, way);
        double[][] ring = {Anatomy.add(base, Anatomy.scale(side, width)), Anatomy.add(base, Anatomy.scale(across, width)),
            Anatomy.add(base, Anatomy.scale(side, -width)), Anatomy.add(base, Anatomy.scale(across, -width))};
        // The snapped end: jagged, one corner standing further out than the rest.
        double[][] end = new double[4][];
        for (int k = 0; k < 4; k++) {
            double[] spread = Anatomy.sub(ring[k], base);
            double keep = 0.25 + 0.35 * hash(w.seed() * 37 + k);
            end[k] = Anatomy.add(Anatomy.add(tip, Anatomy.scale(spread, keep)), Anatomy.scale(way, -0.35 * hash(w.seed() * 41 + k)));
        }
        int bloody = opaque(mix(BONE, BLOOD, 0.55));
        for (int k = 0; k < 4; k++) {
            int m = (k + 1) % 4;
            double[] facing = Anatomy.sub(Anatomy.add(ring[k], ring[m]), Anatomy.scale(base, 2.0));
            double light = 0.55 + 0.45 * Math.max(0.0, Anatomy.dot(facing, normal) / Math.max(1.0E-9, Anatomy.norm(facing)));
            int bone2 = opaque(scaled(mix(BONE, BLOOD, 0.12), light));
            sink.polygon(Role.BONE, new double[][] {ring[k], ring[m], end[m], end[k]}, new int[] {bloody, bloody, bone2, bone2});
        }
        int marrow = opaque(mix(BONE, 0x9A2A2E, 0.45));
        sink.polygon(Role.BONE, new double[][] {end[0], end[1], end[2], end[3]}, new int[] {marrow, marrow, marrow, marrow});
    }

    /** The wet, bruised ring round the opening, fading into the skin. */
    private static void rim(Wound w, double[][] rim2, double[] centre, double open, double[] box, double outer, Sink sink) {
        Wound.Face face = w.face();
        double width = (switch (w.kind()) {
            case CUT -> 0.22;
            case GASH, CAVITY -> 0.5;
            case HOLE -> 0.32;
            default -> 0.4;
        }) * (0.6 + 0.4 * open);
        double[][] outerRing = ring(face, rim2, centre, 1.0, width, box);
        int n = rim2.length;
        int inner = argb(WET, 0.85 * Math.min(1.0, open * 1.5));
        int clear = argb(WET, 0.0);
        double lift = outer + 0.02;
        for (int k = 0; k < n; k++) {
            int m = (k + 1) % n;
            sink.polygon(Role.RIM, new double[][] {face.point(box, rim2[k][0], rim2[k][1], lift), face.point(box, rim2[m][0], rim2[m][1], lift),
                face.point(box, outerRing[m][0], outerRing[m][1], lift), face.point(box, outerRing[k][0], outerRing[k][1], lift)},
                new int[] {inner, inner, clear, clear});
        }
    }

    /**
     * A bruise ({@code strength} 1 for a bruise alone, less round a wound): dark purple where the blow landed, fading out; as it
     * heals it yellows and fades.
     */
    private static void bruise(Wound w, double[][] ring, double[] centre, double open, double[] box, double strength, Sink sink) {
        Wound.Face face = w.face();
        double lift = w.part().outer() + 0.015;
        int colour = mix(BRUISE, BRUISE_OLD, Math.pow(1.0 - open, 0.8));
        double alpha = 0.72 * strength * Math.min(1.0, open * 1.6);
        int n = ring.length;
        if (strength >= 1.0) {
            double[][] points = new double[n + 2][];
            int[] colours = new int[n + 2];
            points[0] = face.point(box, centre[0], centre[1], lift);
            colours[0] = argb(colour, alpha);
            for (int k = 0; k <= n; k++) {
                points[k + 1] = face.point(box, ring[k % n][0], ring[k % n][1], lift);
                colours[k + 1] = argb(colour, 0.0);
            }
            sink.polygon(Role.DECAL, points, colours);
            return;
        }
        // Round a wound: a band from its edge outward.
        double[][] inner = ring(face, ring, centre, 1.0, -0.9, box);
        for (int k = 0; k < n; k++) {
            int m = (k + 1) % n;
            sink.polygon(Role.DECAL, new double[][] {face.point(box, inner[k][0], inner[k][1], lift), face.point(box, inner[m][0], inner[m][1], lift),
                face.point(box, ring[m][0], ring[m][1], lift), face.point(box, ring[k][0], ring[k][1], lift)},
                new int[] {argb(colour, alpha), argb(colour, alpha), argb(colour, 0.0), argb(colour, 0.0)});
        }
    }

    /** A burn's scorched skin: black at the raw edge, brown, then an angry red fading out; paling pink as it heals. */
    private static void scorch(Wound w, double[][] rim2, double[] centre, double open, double[] box, Sink sink) {
        Wound.Face face = w.face();
        double lift = w.part().outer() + 0.015;
        double[][] mid = ring(face, rim2, centre, 1.0, 0.55 + 0.3 * open, box);
        double[][] edge = ring(face, rim2, centre, 1.0, 1.2 + 0.5 * open, box);
        int black = argb(shade(CHAR, (1.0 - open) * 0.8), 0.95 * Math.min(1.0, open * 1.4));
        int brown = argb(shade(CHAR_BROWN, (1.0 - open) * 0.8), 0.85 * Math.min(1.0, open * 1.4));
        int red = argb(INFLAMED, 0.0);
        int n = rim2.length;
        for (int k = 0; k < n; k++) {
            int m = (k + 1) % n;
            sink.polygon(Role.DECAL, new double[][] {face.point(box, rim2[k][0], rim2[k][1], lift), face.point(box, rim2[m][0], rim2[m][1], lift),
                face.point(box, mid[m][0], mid[m][1], lift), face.point(box, mid[k][0], mid[k][1], lift)}, new int[] {black, black, brown, brown});
            sink.polygon(Role.DECAL, new double[][] {face.point(box, mid[k][0], mid[k][1], lift), face.point(box, mid[m][0], mid[m][1], lift),
                face.point(box, edge[m][0], edge[m][1], lift), face.point(box, edge[k][0], edge[k][1], lift)}, new int[] {brown, brown, red, red});
        }
    }

    /** Blood running down from the wound's lowest points (more of it for a worse wound), gone as it heals. */
    private static void blood(Wound w, double[][] rim2, double open, double[] box, double outer, Sink sink) {
        int trickles = switch (w.kind()) {
            case CAVITY -> 3;
            case GASH, FRACTURE -> 2;
            case CUT -> w.length() > 3.0 ? 1 : 0;
            default -> 1;
        };
        if (trickles == 0) {
            return;
        }
        Wound.Face face = w.face();
        double[] side = face.size(box);
        List<double[]> low = new ArrayList<>(List.of(rim2));
        low.sort((a, b) -> Double.compare(b[1], a[1]));
        double lift = outer + 0.03;
        for (int i = 0; i < trickles && i < low.size(); i++) {
            double[] from = low.get(i * Math.max(1, low.size() / (trickles * 3)));
            double length = Math.min(side[1] - 0.05 - from[1], (0.8 + (w.kind() == Wound.Kind.CAVITY ? 3.0 : 1.8) * hash(w.seed() * 13 + i)) * open);
            if (length < 0.15) {
                continue;
            }
            double half = 0.1 + 0.08 * hash(w.seed() * 19 + i);
            double u = clamp(from[0], half, side[0] - half);
            int wet = argb(BLOOD, 0.88 * open);
            int dry = argb(BLOOD, 0.0);
            sink.polygon(Role.BLOOD, new double[][] {face.point(box, u - half, from[1] - 0.05, lift), face.point(box, u + half, from[1] - 0.05, lift),
                face.point(box, u + half * 0.4, from[1] + length, lift), face.point(box, u - half * 0.4, from[1] + length, lift)},
                new int[] {wet, wet, dry, dry});
        }
    }

    /** A polygon fanned from {@code centre} round {@code ring} (closed), at {@code out} along the side's normal. */
    static double[][] star(Wound.Face face, double[] box, double[] centre, double[][] ring, double out) {
        int n = ring.length;
        double[][] points = new double[n + 2][];
        points[0] = face.point(box, centre[0], centre[1], out);
        for (int k = 0; k <= n; k++) {
            points[k + 1] = face.point(box, ring[k % n][0], ring[k % n][1], out);
        }
        return points;
    }

    /**
     * {@code ring} pushed out from {@code centre} by {@code extra} pixels (in, if negative) after scaling by {@code scale}, kept
     * on {@code face}'s side.
     */
    static double[][] ring(Wound.Face face, double[][] ring, double[] centre, double scale, double extra, double[] box) {
        double[] side = face.size(box);
        double[][] out = new double[ring.length][];
        for (int k = 0; k < ring.length; k++) {
            double du = (ring[k][0] - centre[0]) * scale;
            double dv = (ring[k][1] - centre[1]) * scale;
            double l = Math.max(1.0E-6, Math.sqrt(du * du + dv * dv));
            double grown = Math.max(0.05, l + extra) / l;
            out[k] = new double[] {clamp(centre[0] + du * grown, 0.0, side[0]), clamp(centre[1] + dv * grown, 0.0, side[1])};
        }
        return out;
    }

    static double[] centre(double[][] points) {
        double cu = 0.0;
        double cv = 0.0;
        for (double[] p : points) {
            cu += p[0] / points.length;
            cv += p[1] / points.length;
        }
        return new double[] {cu, cv};
    }

    /** The point {@code share} of the way from {@code from} to {@code to}. */
    static double[] toward(double[] from, double[] to, double share) {
        return new double[] {from[0] + (to[0] - from[0]) * share, from[1] + (to[1] - from[1]) * share};
    }

    static boolean slim(Wound.Part part, double[] box) {
        return (part == Wound.Part.RIGHT_ARM || part == Wound.Part.LEFT_ARM) && box[3] - box[0] < 3.5;
    }

    private static void grow(double[] lo, double[] hi, double[] p) {
        for (int i = 0; i < 3; i++) {
            lo[i] = Math.min(lo[i], p[i]);
            hi[i] = Math.max(hi[i], p[i]);
        }
    }

    private static int[] fill(int n, int colour) {
        int[] out = new int[n];
        java.util.Arrays.fill(out, colour);
        return out;
    }

    /** The part of a polygon where {@code nu u + nv v <= d} ({@code flip}: where it is {@code >= -d}). */
    static List<double[]> clip(List<double[]> in, double nu, double nv, double d, boolean flip) {
        List<double[]> out = new ArrayList<>();
        int n = in.size();
        for (int i = 0; i < n; i++) {
            double[] a = in.get(i);
            double[] b = in.get((i + 1) % n);
            double da = side(a, nu, nv, d, flip);
            double db = side(b, nu, nv, d, flip);
            if (da <= 0.0) {
                out.add(a);
            }
            if (da < 0.0 && db > 0.0 || da > 0.0 && db < 0.0) {
                double t = da / (da - db);
                out.add(new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
            }
        }
        return out;
    }

    private static double side(double[] p, double nu, double nv, double d, boolean flip) {
        double value = p[0] * nu + p[1] * nv;
        return flip ? -value - d : value - d;
    }

    /** New tissue: the flesh turns pink as the wound closes. */
    static int shade(int rgb, double heal) {
        return mix(rgb, NEW_TISSUE, 0.55 * Math.max(0.0, Math.min(1.0, heal)));
    }

    static int scaled(int rgb, double k) {
        double s = Math.max(0.0, Math.min(1.0, k));
        return (int)Math.round((rgb >> 16 & 0xFF) * s) << 16 | (int)Math.round((rgb >> 8 & 0xFF) * s) << 8 | (int)Math.round((rgb & 0xFF) * s);
    }

    static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    static int argb(int rgb, double alpha) {
        return (int)Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0) << 24 | rgb & 0xFFFFFF;
    }

    static int mix(int a, int b, double t) {
        double s = Math.max(0.0, Math.min(1.0, t));
        int r = (int)Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * s);
        int g = (int)Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * s);
        int bl = (int)Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * s);
        return r << 16 | g << 8 | bl;
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double)(1L << 53);
    }
}
