package com.hex.kagune.control.wound;

import java.util.ArrayList;
import java.util.List;

/**
 * What lies under the skin of each part of the player model (pure, tested; 5.0.17), in the part's own pixels (x to the model's
 * left, y down, z backward: {@link Wound.Part#box}): the bones, and in the belly the guts. A deep wound shows whatever of this
 * its opening reaches ({@link WoundShape}): ribs and the breastbone through the chest, the spine and the shoulder blades through
 * the back, the arm and leg bones along the limbs, the kneecap, the skull under the scalp, the pelvis at the hips.
 *
 * <p>Each piece is a long box ({@link Piece}) from {@code a} to {@code b}, its flat side facing {@code facing}, sized by its two
 * half widths. Everything stays inside its part's box, so it is only ever seen through an opening (the skin hides the rest).
 */
public final class Anatomy {
    /** What a piece is: bone, the cartilage that joins the ribs to the breastbone, or gut. */
    public enum Tissue {
        BONE, CARTILAGE, GUT
    }

    /**
     * A long box from {@code a} to {@code b} (part pixels): {@code wide} is its half size across (perpendicular to both its length
     * and {@code facing}), {@code flat} its half size along {@code facing}.
     */
    public record Piece(String name, Tissue tissue, double[] a, double[] b, double[] facing, double wide, double flat) {
        /** The piece's eight corners: the four round {@code a}, then the four round {@code b}, in the same order. */
        public double[][] corners() {
            double[] t = sub(this.b, this.a);
            double length = norm(t);
            t = length < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : scale(t, 1.0 / length);
            double[] n = sub(this.facing, scale(t, dot(this.facing, t)));
            n = norm(n) < 1.0E-6 ? perpendicular(t) : scale(n, 1.0 / norm(n));
            double[] w = cross(t, n);
            double[][] out = new double[8][];
            double[][] offsets = {add(scale(w, this.wide), scale(n, this.flat)), add(scale(w, -this.wide), scale(n, this.flat)),
                add(scale(w, -this.wide), scale(n, -this.flat)), add(scale(w, this.wide), scale(n, -this.flat))};
            for (int k = 0; k < 4; k++) {
                out[k] = add(this.a, offsets[k]);
                out[k + 4] = add(this.b, offsets[k]);
            }
            return out;
        }

        /** The same piece cut down to the share of its length from {@code t0} to {@code t1}. */
        public Piece cut(double t0, double t1) {
            return new Piece(this.name, this.tissue, lerp(this.a, this.b, t0), lerp(this.a, this.b, t1), this.facing, this.wide, this.flat);
        }
    }

    static final double[] X = {1.0, 0.0, 0.0};
    static final double[] Y = {0.0, 1.0, 0.0};
    static final double[] Z = {0.0, 0.0, 1.0};

    private Anatomy() {
    }

    /** Everything under the skin of {@code part} ({@code slim}: the three-pixel arms). */
    public static List<Piece> of(Wound.Part part, boolean slim) {
        List<Piece> out = new ArrayList<>();
        switch (part) {
            case BODY -> torso(out);
            case RIGHT_ARM, LEFT_ARM -> arm(out, part.box(slim), slim);
            case RIGHT_LEG -> leg(out, -1.0);
            case LEFT_LEG -> leg(out, 1.0);
            case HEAD -> skull(out);
        }
        return out;
    }

    /** The rib cage, the breastbone, the collarbones, the spine, the shoulder blades, the pelvis, and the guts below the ribs. */
    private static void torso(List<Piece> out) {
        for (int i = 0; i < 6; i++) {
            double y = 1.7 + i * 1.0;
            for (int s = -1; s <= 1; s += 2) {
                // Each rib leaves the breastbone as cartilage, runs out and down across the chest, round the flank and back up
                // to the spine.
                double[] sternal = {s * 0.55, y + 0.05, -1.28};
                double[] costal = {s * 1.35, y + 0.2, -1.24};
                double[] front = {s * 3.5, y + 0.55, -1.0};
                double[] flank = {s * 3.55, y + 0.35, 1.0};
                double[] back = {s * 0.75, y - 0.3, 1.2};
                out.add(new Piece("rib cartilage", Tissue.CARTILAGE, sternal, costal, Z, 0.21, 0.14));
                out.add(new Piece("rib", Tissue.BONE, costal, front, Z, 0.24, 0.15));
                out.add(new Piece("rib", Tissue.BONE, front, flank, X, 0.24, 0.15));
                out.add(new Piece("rib", Tissue.BONE, flank, back, Z, 0.24, 0.15));
            }
        }
        for (int i = 0; i < 2; i++) {
            // The floating ribs: from the spine only part of the way round.
            double y = 7.6 + i * 0.85;
            for (int s = -1; s <= 1; s += 2) {
                out.add(new Piece("floating rib", Tissue.BONE, new double[] {s * 0.75, y - 0.2, 1.2}, new double[] {s * 2.7, y + 0.25, 1.0}, Z, 0.2,
                    0.13));
            }
        }
        out.add(new Piece("breastbone", Tissue.BONE, new double[] {0.0, 0.85, -1.35}, new double[] {0.0, 6.4, -1.35}, Z, 0.55, 0.22));
        out.add(new Piece("breastbone", Tissue.CARTILAGE, new double[] {0.0, 6.4, -1.33}, new double[] {0.0, 7.1, -1.3}, Z, 0.25, 0.14));
        for (int s = -1; s <= 1; s += 2) {
            out.add(new Piece("collarbone", Tissue.BONE, new double[] {s * 0.5, 0.6, -1.45}, new double[] {s * 3.55, 0.35, -0.85}, Y, 0.17, 0.2));
            // The shoulder blades: flat plates high on the back, with their spine across the top.
            out.add(new Piece("shoulder blade", Tissue.BONE, new double[] {s * 2.15, 1.25, 1.5}, new double[] {s * 2.35, 4.7, 1.45}, Z, 1.0, 0.12));
            out.add(new Piece("shoulder blade", Tissue.BONE, new double[] {s * 1.25, 1.75, 1.66}, new double[] {s * 3.35, 1.35, 1.62}, Z, 0.16, 0.12));
            // The pelvis: the hip bones' wings at the sides, their crests round to the front.
            out.add(new Piece("hip bone", Tissue.BONE, new double[] {s * 3.15, 9.35, 0.05}, new double[] {s * 3.05, 11.75, 0.0}, X, 1.05, 0.17));
            out.add(new Piece("hip bone", Tissue.BONE, new double[] {s * 3.05, 9.5, -0.95}, new double[] {s * 1.5, 11.55, -1.3}, Z, 0.24, 0.16));
        }
        out.add(new Piece("pubis", Tissue.BONE, new double[] {-1.5, 11.55, -1.3}, new double[] {1.5, 11.55, -1.3}, Z, 0.24, 0.16));
        out.add(new Piece("sacrum", Tissue.BONE, new double[] {0.0, 9.6, 1.3}, new double[] {0.0, 11.7, 1.2}, Z, 1.0, 0.22));
        for (int i = 0; i < 12; i++) {
            // The spine: a vertebra every pixel down the back, each with its spinous process standing out behind it.
            double y = 0.25 + i * 0.97;
            out.add(new Piece("vertebra", Tissue.BONE, new double[] {0.0, y, 0.9}, new double[] {0.0, y + 0.7, 0.9}, Z, 0.6, 0.45));
            out.add(new Piece("vertebra", Tissue.BONE, new double[] {0.0, y + 0.05, 1.5}, new double[] {0.0, y + 0.6, 1.62}, Z, 0.2, 0.2));
        }
        for (int i = 0; i < 4; i++) {
            // The guts: loops of bowel packed below the ribs, behind the belly.
            double y = 7.85 + i * 0.85;
            double reach = i == 0 ? 2.5 : 2.9;
            out.add(new Piece("bowel", Tissue.GUT, new double[] {-reach, y, -1.0}, new double[] {reach, y + (i % 2 == 0 ? 0.12 : -0.12), -1.0}, Z, 0.36,
                0.36));
        }
        for (int s = -1; s <= 1; s += 2) {
            out.add(new Piece("bowel", Tissue.GUT, new double[] {s * 2.85, 7.9, -0.95}, new double[] {s * 2.95, 10.6, -0.95}, Z, 0.33, 0.33));
        }
    }

    /** The upper arm bone, its head in the shoulder and its knuckles at the elbow; the two forearm bones; the wrist. */
    private static void arm(List<Piece> out, double[] box, boolean slim) {
        double cx = (box[0] + box[3]) * 0.5;
        double thick = slim ? 0.38 : 0.45;
        double fore = slim ? 0.2 : 0.24;
        double apart = slim ? 0.28 : 0.34;
        out.add(new Piece("upper arm bone", Tissue.BONE, new double[] {cx, -1.25, 0.0}, new double[] {cx, 4.0, 0.1}, Z, thick, thick));
        out.add(new Piece("upper arm bone", Tissue.BONE, new double[] {cx, -1.85, 0.0}, new double[] {cx, -0.65, 0.0}, Z, thick + 0.28, thick + 0.25));
        out.add(new Piece("elbow", Tissue.BONE, new double[] {cx, 3.75, 0.15}, new double[] {cx, 4.35, 0.2}, Z, thick + 0.3, thick));
        out.add(new Piece("forearm bone", Tissue.BONE, new double[] {cx - apart, 4.6, -0.15}, new double[] {cx - apart, 9.35, -0.2}, Z, fore, fore));
        out.add(new Piece("forearm bone", Tissue.BONE, new double[] {cx + apart, 4.4, 0.35}, new double[] {cx + apart, 9.3, 0.3}, Z, fore, fore));
        out.add(new Piece("wrist", Tissue.BONE, new double[] {cx, 9.45, 0.05}, new double[] {cx, 9.85, 0.05}, Z, slim ? 0.5 : 0.6, 0.35));
    }

    /** The thigh bone with its knob at the hip and its knuckles at the knee; the kneecap; the shin bone and the fibula. */
    private static void leg(List<Piece> out, double outer) {
        out.add(new Piece("thigh bone", Tissue.BONE, new double[] {0.0, 0.45, 0.1}, new double[] {0.0, 5.75, 0.0}, Z, 0.5, 0.5));
        out.add(new Piece("thigh bone", Tissue.BONE, new double[] {outer * 0.85, 0.35, 0.1}, new double[] {outer * 0.85, 1.25, 0.1}, Z, 0.42, 0.42));
        out.add(new Piece("knee", Tissue.BONE, new double[] {0.0, 5.7, 0.1}, new double[] {0.0, 6.35, 0.1}, Z, 0.82, 0.62));
        out.add(new Piece("kneecap", Tissue.BONE, new double[] {0.0, 5.75, -1.22}, new double[] {0.0, 6.75, -1.25}, Z, 0.55, 0.2));
        out.add(new Piece("shin bone", Tissue.BONE, new double[] {-outer * 0.12, 6.75, -0.45}, new double[] {-outer * 0.12, 11.5, -0.35}, Z, 0.48, 0.45));
        out.add(new Piece("fibula", Tissue.BONE, new double[] {outer * 0.95, 6.95, 0.45}, new double[] {outer * 0.95, 11.4, 0.4}, Z, 0.22, 0.22));
    }

    /** The skull: a shell of bone half a pixel under the scalp, all round but the face. */
    private static void skull(List<Piece> out) {
        double in = 3.45;
        double t = 0.14;
        out.add(new Piece("skull", Tissue.BONE, new double[] {-in, -7.3, 0.0}, new double[] {-in, -0.9, 0.0}, X, 3.3, t));
        out.add(new Piece("skull", Tissue.BONE, new double[] {in, -7.3, 0.0}, new double[] {in, -0.9, 0.0}, X, 3.3, t));
        out.add(new Piece("skull", Tissue.BONE, new double[] {0.0, -7.3, in}, new double[] {0.0, -0.9, in}, Z, 3.3, t));
        out.add(new Piece("skull", Tissue.BONE, new double[] {0.0, -in - 4.0, -3.3}, new double[] {0.0, -in - 4.0, 3.3}, Y, 3.3, t));
    }

    /**
     * The long bone a fracture breaks at ({@code u}, {@code v}) on {@code face} of {@code part}: the bone of the limb under it (or
     * a rib, the collarbone, the skull), the one nearest the skin there. Null if there is none near.
     */
    public static Piece under(Wound.Part part, Wound.Face face, double u, double v, boolean slim) {
        double[] box = part.box(slim);
        double[] point = face.point(box, u, v, 0.0);
        double[] normal = face.normal();
        Piece best = null;
        double bestScore = Double.MAX_VALUE;
        for (Piece piece : of(part, slim)) {
            if (piece.tissue() != Tissue.BONE) {
                continue;
            }
            double[] closest = closest(piece, point);
            double[] d = sub(closest, point);
            double depth = -dot(d, normal);
            double across = norm(sub(d, scale(normal, -depth)));
            double score = depth + across * 2.0;
            if (depth > -0.2 && across < 1.6 && score < bestScore) {
                bestScore = score;
                best = piece;
            }
        }
        return best;
    }

    /** The point on {@code piece}'s centre line nearest {@code point}. */
    public static double[] closest(Piece piece, double[] point) {
        double[] ab = sub(piece.b(), piece.a());
        double length = dot(ab, ab);
        double t = length < 1.0E-12 ? 0.0 : Math.max(0.0, Math.min(1.0, dot(sub(point, piece.a()), ab) / length));
        return lerp(piece.a(), piece.b(), t);
    }

    /**
     * The share of {@code piece}'s length inside the box {@code lo} to {@code hi} (part pixels), grown by the piece's own size:
     * {t0, t1}, or null if it misses the box.
     */
    public static double[] within(Piece piece, double[] lo, double[] hi) {
        double grow = Math.max(piece.wide(), piece.flat());
        double t0 = 0.0;
        double t1 = 1.0;
        double[] d = sub(piece.b(), piece.a());
        for (int i = 0; i < 3; i++) {
            double min = lo[i] - grow;
            double max = hi[i] + grow;
            if (Math.abs(d[i]) < 1.0E-9) {
                if (piece.a()[i] < min || piece.a()[i] > max) {
                    return null;
                }
                continue;
            }
            double ta = (min - piece.a()[i]) / d[i];
            double tb = (max - piece.a()[i]) / d[i];
            t0 = Math.max(t0, Math.min(ta, tb));
            t1 = Math.min(t1, Math.max(ta, tb));
            if (t0 > t1) {
                return null;
            }
        }
        return new double[] {t0, t1};
    }

    static double[] perpendicular(double[] t) {
        double[] axis = Math.abs(t[0]) < 0.6 ? X : Math.abs(t[1]) < 0.6 ? Y : Z;
        double[] n = sub(axis, scale(t, dot(axis, t)));
        return scale(n, 1.0 / norm(n));
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

    static double norm(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }
}
