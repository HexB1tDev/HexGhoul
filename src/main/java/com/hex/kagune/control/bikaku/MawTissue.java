package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.KaguneTint;
import com.hex.kagune.control.graft.GraftMesh;
import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.ukaku.UkakuModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 3.5.3, Monstrous Regeneration seen (the owner's spec: "wounds are visibly repaired by rapidly growing strands of kagune tissue ... organic
 * tissue crawling over damaged body parts and knitting them back together"): thin strands of the kagune's flesh crawling over his whole
 * body while it lasts, each laid on one part of him (his head, torso, arms, legs: their own pixels, so they follow the body as it moves),
 * winding round it, its growing end glowing, a new one starting where the last let go. Pure: the renderer places each strand's points on
 * the posed body and builds them here.
 */
public final class MawTissue {
    private MawTissue() {
    }

    /** One strand: the body part it crawls over (PlayerAnimator's name), its points in that part's own pixels, its thickness (blocks). */
    public record Strand(String part, double[][] local, double radius) {
    }

    /** A part's box in its own pixels (PlayerModel's: x to its left, y down, z back) and how many strands crawl over it. */
    private record Part(String name, double[] min, double[] max, int strands) {
    }

    private static final Part[] PARTS = {
        new Part("torso", new double[] {-4, 0, -2}, new double[] {4, 12, 2}, 9),
        new Part("head", new double[] {-4, -8, -4}, new double[] {4, 0, 4}, 4),
        new Part("rightArm", new double[] {-3, -2, -2}, new double[] {1, 10, 2}, 3),
        new Part("leftArm", new double[] {-1, -2, -2}, new double[] {3, 10, 2}, 3),
        new Part("rightLeg", new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 3),
        new Part("leftLeg", new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 3)};

    /** How far out from the skin a strand lies (pixels), and how many points each is drawn through. */
    static final double OUT = 0.55;
    static final int POINTS = 12;
    /** How long a strand takes to crawl over its part (ticks), and how much of its way shows at once. */
    static final double CRAWL = 26.0;
    static final double SHOWS = 0.6;
    /** How far down an arm or a leg its strands go (a share of it, from the joint). */
    static final double LIMB_REACH = 0.45;

    /** How strongly the strands show {@code age} ticks into Monstrous Regeneration: growing in, holding, withering at its end. */
    public static double shown(double age) {
        int ticks = BikakuMove.REGENERATION.ticks();
        if (age < 0.0 || age >= ticks) {
            return 0.0;
        }
        return GraftMesh.smooth(GraftMesh.clamp01(age / 6.0)) * GraftMesh.smooth(GraftMesh.clamp01((ticks - age) / 24.0));
    }

    /** Every strand {@code age} ticks in ({@code time}: the clock its writhing runs on; {@code seed}: his own). */
    public static List<Strand> strands(double time, double age, int seed) {
        List<Strand> out = new ArrayList<>();
        double shown = shown(age);
        if (shown <= 0.002) {
            return out;
        }
        int n = 0;
        for (Part part : PARTS) {
            for (int k = 0; k < part.strands(); k++, n++) {
                double h = GraftMesh.hash(seed * 31 + n * 7);
                double h2 = GraftMesh.hash(seed * 17 + n * 13);
                double h3 = GraftMesh.hash(seed * 7 + n * 29);
                // Where it is on its way over the part: crawling, a new one starting where the last let go.
                double p = ((age + h * CRAWL) % CRAWL) / CRAWL * (1.0 + SHOWS);
                double from = Math.max(0.0, p - SHOWS);
                double to = Math.min(1.0, p);
                if (to - from < 0.05) {
                    continue;
                }
                double a0 = Math.PI * 2.0 * h2;
                double turns = (0.6 + 0.7 * h3) * (n % 2 == 0 ? 1.0 : -1.0);
                boolean down = h > 0.5;
                double[][] local = new double[POINTS][];
                for (int i = 0; i < POINTS; i++) {
                    double u = from + (to - from) * i / (POINTS - 1.0);
                    double a = a0 + turns * Math.PI * 2.0 * u + 0.25 * Math.sin(time * 0.35 + u * 7.0 + n);
                    double along = down ? u : 1.0 - u;
                    // On an arm or a leg, only near its joint (the upper arm, the thigh): vanilla swings the rest of it as he walks.
                    double reach = part.name().endsWith("Arm") || part.name().endsWith("Leg") ? LIMB_REACH : 1.0;
                    double y = part.min()[1] + 0.5 + (part.max()[1] - part.min()[1] - 1.0) * reach * along;
                    local[i] = onSurface(part, a, y, OUT + 0.25 * Math.sin(time * 0.5 + u * 9.0 + n * 1.7));
                }
                out.add(new Strand(part.name(), local, 0.045 * shown * (0.8 + 0.4 * h3)));
            }
        }
        return out;
    }

    /** The point on {@code part}'s box at angle {@code a} round it (in its x-z section), height {@code y}, {@code out} pixels off its skin. */
    static double[] onSurface(Part part, double a, double y, double out) {
        double cx = (part.min()[0] + part.max()[0]) * 0.5;
        double cz = (part.min()[2] + part.max()[2]) * 0.5;
        double hx = (part.max()[0] - part.min()[0]) * 0.5;
        double hz = (part.max()[2] - part.min()[2]) * 0.5;
        double c = Math.cos(a);
        double s = Math.sin(a);
        double t = 1.0 / Math.max(Math.abs(c) / hx, Math.abs(s) / hz);
        return new double[] {cx + c * (t + out), y, cz + s * (t + out)};
    }

    /**
     * The strands, placed ({@code placed}: each strand's points where they are drawn), into {@code sink} in the kagune colour
     * ({@code tint}): a thin tube of flesh, darker where it began and lighter toward its growing end, and a glowing thread of RC on it.
     */
    public static void build(UkakuModel.Sink sink, List<double[][]> placed, List<Double> radii, int tint) {
        UkakuModel.Sink flesh = (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, n, KaguneTint.recolour(ca, tint), KaguneTint.recolour(cb, tint),
            KaguneTint.recolour(cc, tint), KaguneTint.recolour(cd, tint));
        for (int k = 0; k < placed.size(); k++) {
            double[][] points = placed.get(k);
            double radius = radii.get(k);
            if (points.length < 2 || radius <= 1.0E-4) {
                continue;
            }
            KakujaModel.Spine spine = MawWorm.spine(points, GraftMesh.perpendicular(GraftMesh.normalize(GraftMesh.sub(points[points.length - 1], points[0]))), 14);
            if (spine.length() < 0.02) {
                continue;
            }
            GraftMesh.tube(flesh, spine, 0.0, 1.0, 14, 5, new GraftMesh.Surface() {
                @Override
                public double radius(double s, double a) {
                    return radius * (0.35 + 0.65 * Math.sin(Math.PI * Math.min(1.0, s * 1.15)));
                }

                @Override
                public int colour(double s, double a) {
                    return GraftMesh.mix(MawWorm.FLESH_DEEP, MawWorm.LIP, 0.3 + 0.7 * s);
                }
            }, 1.0);
            // Its thread of RC, brightest at the growing end.
            double[][] prev = null;
            int prevColour = 0;
            for (int i = 0; i <= 10; i++) {
                double s = i / 10.0;
                double[][] f = spine.at(s);
                double w = radius * 0.45;
                double[] centre = GraftMesh.add(f[0], GraftMesh.mul(f[2], radius * 0.8));
                double[][] edge = {GraftMesh.sub(centre, GraftMesh.mul(f[3], w)), GraftMesh.add(centre, GraftMesh.mul(f[3], w))};
                int colour = KaguneTint.recolour(GraftMesh.argb(GraftMesh.mix(MawWorm.VEIN_DIM, MawWorm.VEIN_HOT, s * s), 1.0), tint);
                if (prev != null) {
                    GraftMesh.glow(sink, prev[0], edge[0], edge[1], prev[1], prevColour, colour, colour, prevColour);
                }
                prev = edge;
                prevColour = colour;
            }
        }
    }
}
