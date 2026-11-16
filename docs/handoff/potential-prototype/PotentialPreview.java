import com.hex.kagune.control.client.animation.Reattach;
import com.hex.kagune.control.client.animation.Remains;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Potential Unlock, the tearing-off and the strands (preview before building; the owner: "Show me a preview before you build it"): his
 * body cut from his own skin (Pale's revive pieces), a part torn off whole, thrown, lying there; strands of his kagune in his kagune colour
 * out of the wound, reaching it, wrapping its torn end, hauling it back up and turning it upright; tissue between; whole at 5 s.
 */
public class PotentialPreview {
    static final double[] HOME = {1.0, 0.0, 0.0, 0.0};

    public static void main(String[] args) throws Exception {
        File out = new File(args[0], "potential-reattach.png");
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] ages = {2, 8, 18, 30, 44, 60, 78, 96};
        double[] eye = {4.4, 3.2, 4.8};
        double[] look = {0.6, 0.6, 0.3};
        row(shots, "right arm", EnumSet.of(Reattach.Part.RIGHT_ARM), new double[][] {{1.0, 0.0, -0.3}}, 0xFFA50F37, ages, eye, look);
        row(shots, "head", EnumSet.of(Reattach.Part.HEAD), new double[][] {{0.3, 0.0, -1.0}}, 0xFFA50F37, ages, new double[] {4.2, 3.0, 2.6},
            new double[] {0.2, 0.9, -0.9});
        row(shots, "both arms (a blue kagune)", EnumSet.of(Reattach.Part.RIGHT_ARM, Reattach.Part.LEFT_ARM), new double[][] {{1.0, 0.0, 0.2},
            {-1.0, 0.0, -0.2}}, 0xFF2F6BFF, ages, new double[] {0.6, 3.6, 6.2}, new double[] {0.0, 0.7, 0.0});
        // Up close: reaching, taking hold, hauling, the tissue closing.
        double[][] blow = {{1.0, 0.0, -0.3}};
        Set<Reattach.Part> arm = EnumSet.of(Reattach.Part.RIGHT_ARM);
        Reattach.Flight flight = Reattach.thrown(Reattach.Part.RIGHT_ARM, 7L, blow[0], 0.0, false);
        for (double age : new double[] {34, 46, 64, 84}) {
            Remains.Placed p = Reattach.place(Reattach.Part.RIGHT_ARM, flight, age, false);
            double[] w = Reattach.wound(Reattach.Part.RIGHT_ARM, false)[0];
            double[] mid = lerp(p.at(), w, 0.5);
            List<WingPreview.Tri> scene = scene(arm, blow, 0xFFA50F37, age);
            shots.add(new ArtPreview.Shot(String.format("right arm, close, %.0ft: %s", age, age < 40 ? "reaching" : age < 48 ? "taking hold" : age < 80
                ? "hauling it in" : "tissue closing"), null, ArtPreview.NO_WINGS, new double[] {mid[0] + 1.4, mid[1] + 1.1, mid[2] + 1.8}, mid, scene));
        }
        ArtPreview.sheet(out, "Potential Unlock (preview): torn off whole, thrown, lying there; his kagune's strands (his kagune colour) reach it, "
            + "wrap its torn end, haul it back and turn it upright; tissue closes; whole at 5 s (100t). Each part on its own.", shots, 8, 300, 300);
        System.out.println(out);
    }

    static void row(List<ArtPreview.Shot> shots, String name, Set<Reattach.Part> parts, double[][] blows, int tint, double[] ages, double[] eye,
                    double[] look) {
        for (double age : ages) {
            String beat = age < Reattach.REACH_AT ? "thrown" : age < Reattach.GRIP_AT ? "strands reaching" : age < Reattach.HAUL_AT ? "taking hold"
                : age < Reattach.HOME_AT ? "hauled back" : age < Reattach.WHOLE ? "closing" : "whole";
            shots.add(new ArtPreview.Shot(String.format("%s %.0ft: %s", name, age, beat), null, ArtPreview.NO_WINGS, eye, look, scene(parts, blows, tint, age)));
        }
    }

    /** His body (what is on him), each torn part where it is, its strands, the tissue, the ground. */
    static List<WingPreview.Tri> scene(Set<Reattach.Part> parts, double[][] blows, int tint, double age) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        Set<Remains.Piece> off = EnumSet.noneOf(Remains.Piece.class);
        int b = 0;
        for (Reattach.Part part : parts) {
            Reattach.Flight flight = Reattach.thrown(part, 7L + part.ordinal(), blows[Math.min(b, blows.length - 1)], 0.0, false);
            b++;
            if (!Reattach.onHim(age)) {
                for (Remains.Piece piece : part.pieces()) {
                    off.add(piece);
                }
                Remains.Placed placed = Reattach.place(part, flight, age, false);
                for (Remains.Piece piece : part.pieces()) {
                    double[] local = Reattach.local(part, piece, new double[3], false);
                    piece(tris, piece, placed, local, true);
                }
                // The wound on him: raw.
                double[][] wound = Reattach.wound(part, false);
                disc(tris, wound[0], wound[1], part == Reattach.Part.HEAD ? 0.14 : 0.11, 0xFFB2222E, 0xFF5A0A12);
            }
            double[][][] strands = Reattach.strands(part, 7L + part.ordinal(), flight, age, age * 1.0, false);
            if (strands != null) {
                for (int s = 0; s < strands.length; s++) {
                    double width = Reattach.STRAND_RADIUS * Reattach.strandWidth(s, age, age);
                    if (width > 0.002 && strands[s].length >= 2) {
                        PalePreview.tubeTris(tris, strands[s], width, s % 2 == 0 ? tint : darker(tint));
                    }
                }
            }
            double[][] tissue = Reattach.tissue(part, flight, age, false);
            if (tissue != null && tissue[2][0] > 0.01) {
                PalePreview.tubeTris(tris, new double[][] {tissue[0], lerp(tissue[0], tissue[1], 0.5), tissue[1]}, 0.1 * tissue[2][0], mix(0xFFB2222E, tint));
            }
        }
        for (Remains.Piece piece : Remains.Piece.values()) {
            if (!off.contains(piece)) {
                piece(tris, piece, new Remains.Placed(Remains.centre(piece, false), HOME), new double[3], false);
            }
        }
        for (double gx = -6.0; gx < 6.0; gx += 2.0) {
            for (double gz = -6.0; gz < 6.0; gz += 2.0) {
                PalePreview.box(tris, new double[] {gx, -0.2, gz}, new double[] {gx + 2.0, 0.0, gz + 2.0}, ((int)(gx + gz) & 2) == 0 ? 0xFF4E5A44 : 0xFF48543F);
            }
        }
        return tris;
    }

    /** A piece placed at {@code placed} (its own frame offset by {@code offset} first): its skin; raw ends only if {@code raw}. */
    static void piece(List<WingPreview.Tri> tris, Remains.Piece piece, Remains.Placed placed, double[] offset, boolean raw) {
        java.util.function.Function<double[], double[]> at = own -> {
            double[] r = Remains.rotate(placed.turn(), new double[] {own[0] + offset[0], own[1] + offset[1], own[2] + offset[2]});
            return new double[] {placed.at()[0] + r[0], placed.at()[1] + r[1], placed.at()[2] + r[2]};
        };
        Remains.mesh(piece, false, 7, new Remains.Sink() {
            @Override
            public void skin(double[] a, double[] b, double[] c, double[] d, double[] normal, float[] uv, boolean layer) {
                if (layer) {
                    return;
                }
                double v = (uv[1] + uv[3] + uv[5] + uv[7]) * 16.0;
                double u = (uv[0] + uv[2] + uv[4] + uv[6]) * 16.0;
                int colour = PalePreview.skinColour(piece, u, v);
                double[] n = Remains.rotate(placed.turn(), normal);
                tris.add(new WingPreview.Tri(new double[][] {at.apply(a), at.apply(b), at.apply(c)}, new int[] {colour, colour, colour}, n, true));
                tris.add(new WingPreview.Tri(new double[][] {at.apply(a), at.apply(c), at.apply(d)}, new int[] {colour, colour, colour}, n, true));
            }

            @Override
            public void flesh(double[][] polygon, double[] normal, int[] argb) {
                if (!raw) {
                    return;
                }
                double[] n = Remains.rotate(placed.turn(), normal);
                for (int i = 1; i + 1 < polygon.length; i++) {
                    tris.add(new WingPreview.Tri(new double[][] {at.apply(polygon[0]), at.apply(polygon[i]), at.apply(polygon[i + 1])},
                        new int[] {argb[0], argb[i], argb[i + 1]}, n, true));
                }
            }
        });
    }

    static void disc(List<WingPreview.Tri> tris, double[] at, double[] out, double radius, int rim, int heart) {
        double[] a = normalize(out);
        double[] side = normalize(cross(a, Math.abs(a[1]) > 0.9 ? new double[] {1, 0, 0} : new double[] {0, 1, 0}));
        double[] up = cross(side, a);
        double[] centre = {at[0] + a[0] * 0.01, at[1] + a[1] * 0.01, at[2] + a[2] * 0.01};
        for (int i = 0; i < 12; i++) {
            double a0 = Math.PI * 2 * i / 12;
            double a1 = Math.PI * 2 * (i + 1) / 12;
            double[] p0 = {centre[0] + (side[0] * Math.cos(a0) + up[0] * Math.sin(a0)) * radius, centre[1] + (side[1] * Math.cos(a0) + up[1] * Math.sin(a0)) * radius,
                centre[2] + (side[2] * Math.cos(a0) + up[2] * Math.sin(a0)) * radius};
            double[] p1 = {centre[0] + (side[0] * Math.cos(a1) + up[0] * Math.sin(a1)) * radius, centre[1] + (side[1] * Math.cos(a1) + up[1] * Math.sin(a1)) * radius,
                centre[2] + (side[2] * Math.cos(a1) + up[2] * Math.sin(a1)) * radius};
            tris.add(new WingPreview.Tri(new double[][] {centre, p0, p1}, new int[] {heart, rim, rim}, a, true));
        }
    }

    static int darker(int argb) {
        return 0xFF000000 | ((argb >> 16 & 255) * 3 / 5) << 16 | ((argb >> 8 & 255) * 3 / 5) << 8 | (argb & 255) * 3 / 5;
    }

    static int mix(int a, int b) {
        return 0xFF000000 | ((a >> 16 & 255) + (b >> 16 & 255)) / 2 << 16 | ((a >> 8 & 255) + (b >> 8 & 255)) / 2 << 8 | ((a & 255) + (b & 255)) / 2;
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double[] normalize(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return l < 1e-12 ? new double[] {0, 1, 0} : new double[] {v[0] / l, v[1] / l, v[2] / l};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
