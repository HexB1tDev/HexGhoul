package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.ukaku.Finisher;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.2.9 (the owner: "Up more around here, the hands/arms get chopped off too and land on the floor"): a body the Steel Blade's finisher cut
 * through (pure, tested). The cut runs across the middle of the trunk and through both arms at the same height, so it falls apart in four:
 * the top (head, chest, upper arms), the bottom (belly and legs), and each forearm with its hand. Each piece is the body's own boxes, textured
 * as vanilla textures them (the same corners, the same UV of the same skin), cut where the line crosses them with what is inside showing on each
 * cut ({@link Finisher#section}), and the boxes it is made of for its fall ({@code Ragdoll}).
 *
 * <p>Space: blocks, x to the body's left, y up from its feet, z its front (right-handed: a turn about y by minus its yaw puts it in the world).
 */
public final class Carcass {
    /** Where the cut runs (blocks above the feet): the player's chest, the same for every body (the owner: "the cut height is a fixed thing"). */
    public static double height() {
        return place(new RigPose(), "torso", new double[] {0.0, CUT, 0.0}, 1.0)[1];
    }

    /** The pieces. */
    public enum Kind {
        TOP, BOTTOM, RIGHT_HAND, LEFT_HAND
    }

    /** How a skin is laid out: a player's (wide or slim arms, with the second layer), a zombie's (64 square, a hat only), a skeleton's (64 by 32, thin limbs). */
    public enum Layout {
        PLAYER_WIDE(64, 64, 4, true), PLAYER_SLIM(64, 64, 3, true), HUMANOID(64, 64, 4, false), THIN(64, 32, 2, false);

        final int width;
        final int height;
        final int arm;
        final boolean layers;

        Layout(int width, int height, int arm, boolean layers) {
            this.width = width;
            this.height = height;
            this.arm = arm;
            this.layers = layers;
        }
    }

    /** Where the trunk is cut (model pixels down from the neck: its middle) and so where the arms are (pixels down from the shoulder's pivot). */
    public static final double CUT = 6.0;
    static final double SHOULDER = 2.0;

    /** A textured quad of a piece: its corners (blocks), their texture coordinates (0..1) and its facing. */
    public record Face(double[][] p, double[][] uv, double[] n) {
    }

    /** A coloured quad of a cut (what is inside). */
    public record Cap(double[][] p, int[] argb, double[] n) {
    }

    /** A piece: its textured quads, its cuts, and the boxes it falls as (blocks: {min x, y, z, max x, y, z}). */
    public static final class Piece {
        public final Kind kind;
        public final List<Face> faces = new ArrayList<>();
        public final List<Cap> caps = new ArrayList<>();
        public final List<double[]> boxes = new ArrayList<>();

        Piece(Kind kind) {
            this.kind = kind;
        }
    }

    /** One cube of a part: its corner and size (part pixels), where its texture starts, how much it is blown up (the second layer), mirrored. */
    record Cube(String part, double x, double y, double z, double w, double h, double d, int u, int v, double grow, boolean mirror, boolean solid) {
    }

    private Carcass() {
    }

    /**
     * The body in {@code pose} cut through: its pieces, laid out as {@code layout}, {@code scale} its size against a player's ({@code 1} a player,
     * 1/0.9375 a zombie), {@code inside} what its trunk shows (a person's or meat; its limbs then show a limb's or meat).
     */
    public static List<Piece> cut(RigPose pose, Layout layout, double scale, Finisher.Inside inside) {
        Piece top = new Piece(Kind.TOP);
        Piece bottom = new Piece(Kind.BOTTOM);
        Piece right = new Piece(Kind.RIGHT_HAND);
        Piece left = new Piece(Kind.LEFT_HAND);
        Finisher.Inside limb = inside == Finisher.Inside.MEAT ? Finisher.Inside.MEAT : Finisher.Inside.LIMB;
        for (Cube cube : cubes(layout)) {
            switch (cube.part()) {
                case "head" -> add(top, cube, pose, layout, scale, Double.NaN, null, null);
                case "torso" -> add(top, cube, pose, layout, scale, CUT, bottom, inside);
                case "rightArm", "leftArm" -> {
                    // Cut where the line crosses it; held up out of it (a zombie reaching), it goes whole with the top (or below it, the legs).
                    double at = armCut(pose, cube, scale);
                    Piece hand = cube.part().equals("rightArm") ? right : left;
                    if (!Double.isNaN(at)) {
                        add(top, cube, pose, layout, scale, at, hand, limb);
                    } else {
                        add(above(pose, cube, scale) ? top : bottom, cube, pose, layout, scale, Double.NaN, null, null);
                    }
                }
                default -> add(bottom, cube, pose, layout, scale, Double.NaN, null, null);
            }
        }
        List<Piece> out = new ArrayList<>();
        for (Piece piece : List.of(top, bottom, right, left)) {
            if (!piece.faces.isEmpty()) {
                out.add(piece);
            }
        }
        return out;
    }

    /** The cubes of a body laid out as {@code layout}, vanilla's: its parts and (a player's) their second layer. */
    static List<Cube> cubes(Layout layout) {
        List<Cube> out = new ArrayList<>();
        int a = layout.arm;
        out.add(new Cube("head", -4, -8, -4, 8, 8, 8, 0, 0, 0.0, false, true));
        out.add(new Cube("head", -4, -8, -4, 8, 8, 8, 32, 0, 0.5, false, false));
        out.add(new Cube("torso", -4, 0, -2, 8, 12, 4, 16, 16, 0.0, false, true));
        if (layout == Layout.THIN) {
            out.add(new Cube("rightArm", -1, -2, -1, 2, 12, 2, 40, 16, 0.0, false, true));
            out.add(new Cube("leftArm", -1, -2, -1, 2, 12, 2, 40, 16, 0.0, true, true));
            out.add(new Cube("rightLeg", -1, 0, -1, 2, 12, 2, 0, 16, 0.0, false, true));
            out.add(new Cube("leftLeg", -1, 0, -1, 2, 12, 2, 0, 16, 0.0, true, true));
            return out;
        }
        out.add(new Cube("rightArm", a == 3 ? -2 : -3, -2, -2, a, 12, 4, 40, 16, 0.0, false, true));
        if (layout == Layout.HUMANOID) {
            // Vanilla's plain humanoid (a zombie's): its left limbs wear its right ones' texture, mirrored (only a player's skin has its own).
            out.add(new Cube("leftArm", -1, -2, -2, a, 12, 4, 40, 16, 0.0, true, true));
            out.add(new Cube("rightLeg", -2, 0, -2, 4, 12, 4, 0, 16, 0.0, false, true));
            out.add(new Cube("leftLeg", -2, 0, -2, 4, 12, 4, 0, 16, 0.0, true, true));
            return out;
        }
        out.add(new Cube("leftArm", -1, -2, -2, a, 12, 4, 32, 48, 0.0, false, true));
        out.add(new Cube("rightLeg", -2, 0, -2, 4, 12, 4, 0, 16, 0.0, false, true));
        out.add(new Cube("leftLeg", -2, 0, -2, 4, 12, 4, 16, 48, 0.0, false, true));
        if (layout.layers) {
            out.add(new Cube("torso", -4, 0, -2, 8, 12, 4, 16, 32, 0.25, false, false));
            out.add(new Cube("rightArm", a == 3 ? -2 : -3, -2, -2, a, 12, 4, 40, 32, 0.25, false, false));
            out.add(new Cube("leftArm", -1, -2, -2, a, 12, 4, 48, 48, 0.25, false, false));
            out.add(new Cube("rightLeg", -2, 0, -2, 4, 12, 4, 0, 32, 0.25, false, false));
            out.add(new Cube("leftLeg", -2, 0, -2, 4, 12, 4, 0, 48, 0.25, false, false));
        }
        return out;
    }

    /**
     * Cube {@code cube} into {@code into}, or (with a cut at part pixel {@code at}, down from its pivot) its part above the cut into {@code into}
     * and below it into {@code under}, the cut faced with {@code inside} on both (its solid cube only).
     */
    static void add(Piece into, Cube cube, RigPose pose, Layout layout, double scale, double at, Piece under, Finisher.Inside inside) {
        double g = cube.grow();
        double x0 = cube.x() - g;
        double y0 = cube.y() - g;
        double z0 = cube.z() - g;
        double x1 = cube.x() + cube.w() + g;
        double y1 = cube.y() + cube.h() + g;
        double z1 = cube.z() + cube.d() + g;
        boolean cut = !Double.isNaN(at) && at > y0 && at < y1;
        for (double[][][] face : faces(cube, x0, y0, z0, x1, y1, z1, layout)) {
            if (!cut) {
                emit(into, face, cube.part(), pose, scale);
                continue;
            }
            emit(into, clip(face, at, true), cube.part(), pose, scale);
            emit(under, clip(face, at, false), cube.part(), pose, scale);
        }
        if (!cube.solid()) {
            return;
        }
        if (!cut) {
            box(into, cube.part(), pose, scale, x0, y0, z0, x1, y1, z1);
            return;
        }
        box(into, cube.part(), pose, scale, x0, y0, z0, x1, at, z1);
        box(under, cube.part(), pose, scale, x0, at, z0, x1, y1, z1);
        // The cut, both sides: what is inside, standing out of each (model y runs down: the upper piece's faces down, the lower's up).
        double cx = (x0 + x1) * 0.5;
        double cz = (z0 + z1) * 0.5;
        double hw = (x1 - x0) * 0.5;
        double hd = (z1 - z0) * 0.5;
        int seed = cube.part().hashCode();
        Finisher.section(capSink(into, cube.part(), pose, scale, cx, at, cz), hw, hd, 0.0, false, inside, 1.0, seed);
        Finisher.section(capSink(under, cube.part(), pose, scale, cx, at, cz), hw, hd, 0.0, true, inside, 1.0, seed + 1);
    }

    /** A sink placing a cut's quads (its own frame: x the body's right, y out of the cut upward, z its front; pixels) on the part, posed. */
    static com.hex.kagune.control.ukaku.UkakuModel.Sink capSink(Piece piece, String part, RigPose pose, double scale, double cx, double at, double cz) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> {
            double[][] corners = {a, b, c, d};
            double[][] out = new double[4][];
            for (int k = 0; k < 4; k++) {
                out[k] = place(pose, part, new double[] {cx - corners[k][0], at - corners[k][1], cz - corners[k][2]}, scale);
            }
            double[] tip = place(pose, part, new double[] {cx - a[0] - n[0] * 0.1, at - a[1] - n[1] * 0.1, cz - a[2] - n[2] * 0.1}, scale);
            piece.caps.add(new Cap(out, new int[] {ca, cb, cc, cd}, unit(sub(tip, out[0]))));
        };
    }

    /**
     * Where the cut (a level plane at the trunk's cut, {@link #height}, the same for every body) crosses an arm: part pixels down it from its
     * shoulder; NaN if it doesn't (the arm wholly above or below it, or lying along it).
     */
    static double armCut(RigPose pose, Cube cube, double scale) {
        double cx = cube.x() + cube.w() * 0.5;
        double cz = cube.z() + cube.d() * 0.5;
        double[] top = place(pose, cube.part(), new double[] {cx, cube.y(), cz}, scale);
        double[] end = place(pose, cube.part(), new double[] {cx, cube.y() + cube.h(), cz}, scale);
        double cut = place(pose, "torso", new double[] {0.0, CUT, 0.0}, scale)[1];
        double drop = top[1] - end[1];
        double length = Math.sqrt(Math.pow(top[0] - end[0], 2) + drop * drop + Math.pow(top[2] - end[2], 2));
        if ((top[1] - cut) * (end[1] - cut) > 0.0 || Math.abs(drop) < 0.5 * length) {
            return Double.NaN;
        }
        return cube.y() + cube.h() * (top[1] - cut) / drop;
    }

    /** True if an arm (uncut) lies above the cut: its middle higher. */
    static boolean above(RigPose pose, Cube cube, double scale) {
        double[] middle = place(pose, cube.part(), new double[] {cube.x() + cube.w() * 0.5, cube.y() + cube.h() * 0.5, cube.z() + cube.d() * 0.5}, scale);
        return middle[1] >= place(pose, "torso", new double[] {0.0, CUT, 0.0}, scale)[1];
    }

    /** A box of a part (part pixels) as the piece's collision box: the posed corners' bounds (blocks). */
    static void box(Piece piece, String part, RigPose pose, double scale, double x0, double y0, double z0, double x1, double y1, double z1) {
        double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int k = 0; k < 8; k++) {
            double[] p = place(pose, part, new double[] {(k & 1) == 0 ? x0 : x1, (k & 2) == 0 ? y0 : y1, (k & 4) == 0 ? z0 : z1}, scale);
            for (int i = 0; i < 3; i++) {
                b[i] = Math.min(b[i], p[i]);
                b[i + 3] = Math.max(b[i + 3], p[i]);
            }
        }
        piece.boxes.add(b);
    }

    /** A part's point (its own pixels) where the posed body has it, in the carcass's space (blocks: x its left, y up, z its front). */
    public static double[] place(RigPose pose, String part, double[] local, double scale) {
        double[] rig = BodyFrame.world(pose, BodyFrame.model(pose, part, local));
        double k = scale;
        return new double[] {-rig[0] * k, rig[1] * k, rig[2] * k};
    }

    static void emit(Piece piece, double[][][] face, String part, RigPose pose, double scale) {
        if (face == null) {
            return;
        }
        double[][] corners = face[0];
        double[][] uv = face[1];
        double[][] p = new double[corners.length][];
        for (int k = 0; k < corners.length; k++) {
            p[k] = place(pose, part, corners[k], scale);
        }
        double[] n = unit(cross(sub(p[1], p[0]), sub(p[2], p[0])));
        if (corners.length == 4) {
            piece.faces.add(new Face(p, uv, n));
            return;
        }
        // A cut face of five corners (never more): as a fan of quads, each a triangle with its last corner twice.
        for (int k = 1; k + 1 < corners.length; k++) {
            piece.faces.add(new Face(new double[][] {p[0], p[k], p[k + 1], p[k + 1]}, new double[][] {uv[0], uv[k], uv[k + 1], uv[k + 1]}, n));
        }
    }

    /**
     * The six faces of a cube as vanilla builds them ({@code ModelPart.Cube}: its corners in part pixels and their texture coordinates, 0..1),
     * each {corners, uvs}.
     */
    static List<double[][][]> faces(Cube cube, double x0, double y0, double z0, double x1, double y1, double z1, Layout layout) {
        if (cube.mirror()) {
            double t = x0;
            x0 = x1;
            x1 = t;
        }
        double[] v0 = {x0, y0, z0};
        double[] v1 = {x1, y0, z0};
        double[] v2 = {x1, y1, z0};
        double[] v3 = {x0, y1, z0};
        double[] v4 = {x0, y0, z1};
        double[] v5 = {x1, y0, z1};
        double[] v6 = {x1, y1, z1};
        double[] v7 = {x0, y1, z1};
        double u = cube.u();
        double v = cube.v();
        double w = cube.w();
        double h = cube.h();
        double d = cube.d();
        double f4 = u + d;
        double f5 = u + d + w;
        double f6 = u + d + w + w;
        double f7 = u + d + w + d;
        double f8 = u + d + w + d + w;
        double g1 = v + d;
        double g2 = v + d + h;
        List<double[][][]> out = new ArrayList<>();
        out.add(polygon(new double[][] {v5, v4, v0, v1}, f4, v, f5, g1, layout, cube.mirror()));
        out.add(polygon(new double[][] {v2, v3, v7, v6}, f5, g1, f6, v, layout, cube.mirror()));
        out.add(polygon(new double[][] {v0, v4, v7, v3}, u, g1, f4, g2, layout, cube.mirror()));
        out.add(polygon(new double[][] {v1, v0, v3, v2}, f4, g1, f5, g2, layout, cube.mirror()));
        out.add(polygon(new double[][] {v5, v1, v2, v6}, f5, g1, f7, g2, layout, cube.mirror()));
        out.add(polygon(new double[][] {v4, v5, v6, v7}, f7, g1, f8, g2, layout, cube.mirror()));
        return out;
    }

    /** One face as vanilla's {@code Polygon}: its corners take (u1, v0), (u0, v0), (u0, v1), (u1, v1); mirrored, in the other order. */
    static double[][][] polygon(double[][] corners, double u0, double v0, double u1, double v1, Layout layout, boolean mirror) {
        double tw = layout.width;
        double th = layout.height;
        double[][] uv = {{u1 / tw, v0 / th}, {u0 / tw, v0 / th}, {u0 / tw, v1 / th}, {u1 / tw, v1 / th}};
        double[][] p = corners.clone();
        if (mirror) {
            double[][] rp = new double[4][];
            double[][] ruv = new double[4][];
            for (int k = 0; k < 4; k++) {
                rp[k] = p[3 - k];
                ruv[k] = uv[3 - k];
            }
            p = rp;
            uv = ruv;
        }
        return new double[][][] {p, uv};
    }

    /**
     * The part of a face above ({@code above}: part y less than {@code at}, model y running down) or below the cut, its texture coordinates
     * cut with it; null if none of it is there.
     */
    static double[][][] clip(double[][][] face, double at, boolean above) {
        double[][] p = face[0];
        double[][] uv = face[1];
        List<double[]> outP = new ArrayList<>();
        List<double[]> outUv = new ArrayList<>();
        int n = p.length;
        for (int k = 0; k < n; k++) {
            double[] a = p[k];
            double[] b = p[(k + 1) % n];
            boolean ina = above ? a[1] <= at : a[1] >= at;
            boolean inb = above ? b[1] <= at : b[1] >= at;
            if (ina) {
                outP.add(a);
                outUv.add(uv[k]);
            }
            if (ina != inb) {
                double t = (at - a[1]) / (b[1] - a[1]);
                outP.add(new double[] {a[0] + (b[0] - a[0]) * t, at, a[2] + (b[2] - a[2]) * t});
                double[] ua = uv[k];
                double[] ub = uv[(k + 1) % n];
                outUv.add(new double[] {ua[0] + (ub[0] - ua[0]) * t, ua[1] + (ub[1] - ua[1]) * t});
            }
        }
        if (outP.size() < 3) {
            return null;
        }
        return new double[][][] {outP.toArray(new double[0][]), outUv.toArray(new double[0][])};
    }

    /**
     * How each piece starts moving the moment the cut meets its start ({speed, spin}, blocks and radians a tick, the carcass's space): the top
     * slides forward off the cut and tips over its front, landing clear of the legs (the owner: "the top half is clearly falling in the legs, it
     * should fall forward a bit more"); the forearms drop away to either side, turning; the belly and legs rock back.
     */
    public static double[][] launch(Kind kind) {
        return switch (kind) {
            case TOP -> new double[][] {{0.0, 0.06, 0.2}, {0.11, 0.0, 0.0}};
            case RIGHT_HAND -> new double[][] {{-0.035, 0.03, 0.02}, {0.16, 0.05, 0.22}};
            case LEFT_HAND -> new double[][] {{0.035, 0.03, 0.02}, {0.14, -0.05, -0.24}};
            // The cut's recoil: the legs rock back, away from the top falling forward, and go down behind it.
            default -> new double[][] {{0.0, 0.0, -0.015}, {-0.035, 0.0, 0.0}};
        };
    }

    /** The wound's line: how thick (pixels each side of the cut) and its colour, bright red, drawn glowing (lit from straight above: full bright). */
    public static final double LINE = 0.07;
    public static final int GLOW = 0xFFFF2A2A;
    /** How far out of the skin the wound is drawn (pixels): over a skin's second layer, under nothing. */
    static final double WOUND_OUT = 0.32;
    /** How many steps go round each cut box. */
    static final int WOUND_STEPS = 96;

    /**
     * The wound the finisher draws round the body before it falls apart (the owner: "a perfect wound ... really shallow will slowly appear";
     * then: "just make a really thin red (bright and glowing) line around the whole area of the cut", "no stripes, an actual line, and even
     * thinner"): one line round the trunk and both arms where {@link #cut} will part them (where an arm lies against the trunk it runs on across
     * both, unbroken), drawn in from the very middle of its front both ways at once, left and right (the owner: "the line should honestly now start
     * in the very center, and the line flows from both sides at once, left and right"), {@code amount} (0..1) of the way round to where the two
     * ends meet in the middle of its back, each end thinning to nothing. Quads into {@code sink} (the carcass's space, blocks; lit from straight
     * above: full bright).
     */
    public static void wound(RigPose pose, Layout layout, double scale, double amount, com.hex.kagune.control.ukaku.UkakuModel.Sink sink) {
        wound(pose, layout, scale, amount, true, FLAT_LAYER, sink);
    }

    /** How far a skin's second layer stands off the body (pixels): vanilla's flat layer, and 3D Skin Layers' voxels. */
    public static final double FLAT_LAYER = 0.25;
    public static final double VOXEL_LAYER = 1.15;

    /**
     * As above; {@code arms} false: round the trunk only (a mob's arms are not where a player's hang); {@code layer} how far the skin's second
     * layer stands off it (pixels: 0.25 flat, more as 3D Skin Layers' voxels), the line just past it (the owner: "why does the line stop on the
     * overlayer?").
     */
    public static void wound(RigPose pose, Layout layout, double scale, double amount, boolean arms, double layer,
                             com.hex.kagune.control.ukaku.UkakuModel.Sink sink) {
        if (amount <= 0.0) {
            return;
        }
        double out = WOUND_OUT + (layout.layers ? Math.max(0.25, layer) : 0.0);
        List<Cube> cut = new ArrayList<>();
        List<double[]> bounds = new ArrayList<>();
        for (Cube cube : cubes(layout)) {
            if (cube.solid() && (cube.part().equals("torso") || arms && cube.part().endsWith("Arm"))) {
                cut.add(cube);
                // What it covers (blocks), blown up by how far the line stands off it: a line inside another's is pressed between them, unseen.
                double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
                for (int k = 0; k < 8; k++) {
                    double[] p = place(pose, cube.part(), new double[] {(k & 1) == 0 ? cube.x() - out : cube.x() + cube.w() + out,
                        (k & 2) == 0 ? cube.y() : cube.y() + cube.h(), (k & 4) == 0 ? cube.z() - out : cube.z() + cube.d() + out}, scale);
                    for (int q = 0; q < 3; q++) {
                        b[q] = Math.min(b[q], p[q]);
                        b[q + 3] = Math.max(b[q + 3], p[q]);
                    }
                }
                bounds.add(b);
            }
        }
        double hair = 0.06 / 16.0 * scale;
        for (int c = 0; c < cut.size(); c++) {
            Cube cube = cut.get(c);
            double at = cube.part().equals("torso") ? CUT : armCut(pose, cube, scale);
            if (Double.isNaN(at)) {
                continue; // an arm held up out of the cut: no line round it
            }
            double cx = cube.x() + cube.w() * 0.5;
            double cz = cube.z() + cube.d() * 0.5;
            double hw = cube.w() * 0.5 + out;
            double hd = cube.d() * 0.5 + out;
            double[] middle = place(pose, cube.part(), new double[] {cx, at, cz}, scale);
            for (int i = 0; i < WOUND_STEPS; i++) {
                double[] a = square(i / (double)WOUND_STEPS, cx, at, cz, hw, hd);
                double[] b = square((i + 1) / (double)WOUND_STEPS, cx, at, cz, hw, hd);
                double[] mid = place(pose, cube.part(), new double[] {(a[0] + b[0]) * 0.5, at, (a[2] + b[2]) * 0.5}, scale);
                // How far round from the front's middle, either way (0 there, 1 at the back's middle).
                double f = Math.abs(Math.atan2(-mid[0], mid[2])) / Math.PI;
                if (f > amount || hidden(mid, bounds, c, hair)) {
                    continue;
                }
                // Its newest end thins to nothing as it is drawn in.
                double width = LINE * Math.min(1.0, (amount - f) / TIP + (amount >= 1.0 ? 1.0 : 0.0));
                strip(sink, pose, cube.part(), scale, middle, a, b, -width, width, GLOW, GLOW);
            }
        }
    }

    /** How much of the way round the line's end thins over (0..1 of the sweep). */
    static final double TIP = 0.05;

    /**
     * The point {@code u} (0..1) of the way round a box's cut, square-cornered (so where an arm lies against the trunk their lines meet straight
     * on), from its front middle toward its right: part pixels, at the cut.
     */
    static double[] square(double u, double cx, double at, double cz, double hw, double hd) {
        double[] b = com.hex.kagune.control.ukaku.Finisher.box(u, hw, hd);
        // The band's x runs to the body's right and its z to the front: the part's own x is its left, z its back.
        return new double[] {cx - b[0], at, cz - b[1]};
    }

    /** True if {@code p} lies inside any of {@code bounds} but the {@code own}th (by more than {@code hair}): pressed between two parts. */
    static boolean hidden(double[] p, List<double[]> bounds, int own, double hair) {
        for (int i = 0; i < bounds.size(); i++) {
            double[] b = bounds.get(i);
            if (i != own && p[0] > b[0] + hair && p[0] < b[3] - hair && p[1] > b[1] + hair && p[1] < b[4] - hair && p[2] > b[2] + hair
                && p[2] < b[5] - hair) {
                return true;
            }
        }
        return false;
    }

    /** A strip of the wound between two points of its band (part pixels), from {@code lo} to {@code hi} pixels along the part (model y, down). */
    static void strip(com.hex.kagune.control.ukaku.UkakuModel.Sink sink, RigPose pose, String part, double scale, double[] middle, double[] a,
                      double[] b, double lo, double hi, int cLo, int cHi) {
        double[] p0 = place(pose, part, new double[] {a[0], a[1] + lo, a[2]}, scale);
        double[] p1 = place(pose, part, new double[] {b[0], b[1] + lo, b[2]}, scale);
        double[] p2 = place(pose, part, new double[] {b[0], b[1] + hi, b[2]}, scale);
        double[] p3 = place(pose, part, new double[] {a[0], a[1] + hi, a[2]}, scale);
        // It glows: lit from straight above, the game's way of drawing a quad at full brightness ({@code KakujaArmorLayer.emit}).
        sink.quad(p0, p1, p2, p3, new double[] {0.0, 1.0, 0.0}, cLo, cLo, cHi, cHi);
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] unit(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
