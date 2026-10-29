package com.hex.kagune.control.king;

import static com.hex.kagune.control.graft.GraftMesh.add;
import static com.hex.kagune.control.graft.GraftMesh.clamp01;
import static com.hex.kagune.control.graft.GraftMesh.cross;
import static com.hex.kagune.control.graft.GraftMesh.dot;
import static com.hex.kagune.control.graft.GraftMesh.mul;
import static com.hex.kagune.control.graft.GraftMesh.smooth;
import static com.hex.kagune.control.graft.GraftMesh.sub;

import com.hex.kagune.control.ukaku.UkakuModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.0.28: the shapes of War's held moves (pure; points in any frame with y up: the world in game, the rig in the previews):
 * Black Reaper's centipedes out of his back to what they drag, the Spire Wave's round his arm while it charges and the mass of them
 * sent out along it, King's Defiance's spikes out of the cocoon. Each centipede is drawn by {@link KingFxModel#centipede}.
 */
public final class WarHoldModel {
    /** One centipede: the way it lies (root to head), how thick, its jaw (0 shut, 1 wide), its look's seed, its legs' wave. */
    public record Arm(double[][] path, double width, double jaw, int seed, double phase) {
    }

    /** Where Black Reaper's centipedes come out of his back (torso pixels), and his left elbow and hand (left arm pixels). */
    public static double[] reaperRoot(int k) {
        double side = k - (WarRules.REAPER_ARMS - 1) * 0.5;
        return new double[] {side * 2.4, 3.0 + Math.abs(side) * 1.2, 2.4};
    }

    public static final double[] ELBOW = {1.0, 3.5, 0.0};
    public static final double[] HAND = {1.0, 10.5, 0.0};

    private WarHoldModel() {
    }

    /** {@code arms} drawn. */
    public static void draw(UkakuModel.Sink sink, List<Arm> arms, boolean fine) {
        for (Arm arm : arms) {
            KingFxModel.centipede(sink, arm.path(), arm.width(), arm.seed(), arm.jaw(), fine, 1.0, arm.phase());
        }
    }

    /**
     * Black Reaper: a centipede out of each of {@code roots} (his upper back), up and over his shoulder ({@code forward}, {@code right}:
     * which way he faces) and out at {@code prey} (its middle, {@code radius} round), each biting its own side of it; their heads go
     * out along the way first ({@code phase} {@code WarRules.REAPER_*}, {@code age} ticks in); taut and trembling as they drag it; back
     * the same way into him.
     */
    public static List<Arm> reaper(double[][] roots, double[] forward, double[] right, double[] prey, double radius, int phase, double age, double now) {
        List<Arm> out = new ArrayList<>();
        double reach = phase == WarRules.REAPER_OUT ? WarRules.reaperReach(age, false)
            : phase == WarRules.REAPER_BACK ? WarRules.reaperReach(age, true) : 1.0;
        if (reach <= 0.01) {
            return out;
        }
        boolean taut = phase == WarRules.REAPER_DRAG || phase == WarRules.REAPER_THROUGH;
        double[] up = {0.0, 1.0, 0.0};
        for (int k = 0; k < roots.length; k++) {
            double side = k - (roots.length - 1) * 0.5;
            double[] root = roots[k];
            double[] lift = add(add(add(root, mul(up, 0.7)), mul(forward, -0.35)), mul(right, side * 0.45));
            double[] over = add(add(add(root, mul(up, 1.25 + 0.15 * Math.abs(side))), mul(forward, 0.35)), mul(right, side * 0.8));
            double[] to = sub(prey, over);
            double distance = Math.sqrt(dot(to, to));
            double[] flat = unit(new double[] {to[0], 0.0, to[2]}, forward);
            double[] across = {-flat[2], 0.0, flat[0]};
            double a = Math.PI * 2.0 * k / roots.length + 0.6;
            double[] bite = add(add(add(prey, mul(across, Math.cos(a) * (radius + 0.05))), mul(up, Math.sin(a) * 0.35)), mul(flat, -radius * 0.5));
            List<double[]> path = new ArrayList<>();
            path.add(root);
            path.add(lift);
            path.add(over);
            int mids = 4;
            for (int j = 1; j <= mids; j++) {
                double t = (double)j / (mids + 1);
                double[] q = add(over, mul(sub(bite, over), t));
                double arc = Math.sin(Math.PI * t) * (taut ? 0.25 + 0.02 * distance : 0.6 + 0.06 * distance);
                double wave = taut ? 0.06 * Math.sin(now * 1.7 + k * 2.0 + j * 1.3) : 0.35 * Math.sin(now * 0.3 + k * 2.1 + j * 1.1) * (1.0 - t * 0.5);
                q = add(add(q, mul(up, arc)), mul(across, wave + side * 0.35 * Math.sin(Math.PI * t)));
                path.add(q);
            }
            path.add(bite);
            double jaw = phase == WarRules.REAPER_OUT ? (reach > 0.85 ? 1.0 - (reach - 0.85) / 0.15 * 0.9 : 1.0) : taut ? 0.06 : 0.6;
            out.add(new Arm(upTo(path, reach), 0.17, Math.max(0.0, jaw), 41 + k, now * (taut ? 1.5 : 1.0) + k * 1.7));
        }
        return out;
    }

    /**
     * The Spire Wave held ({@code age} ticks): centipedes winding round his forearm from {@code elbow} past {@code hand}, writhing,
     * their heads fanned out round where he points; more of them, longer and thicker the longer it is held; they grow out of the arm
     * over its first ticks.
     */
    public static List<Arm> coils(double[] elbow, double[] hand, double age, double now) {
        List<Arm> out = new ArrayList<>();
        double c = WarRules.spireCharge(age);
        double grown = Math.min(1.0, age / 6.0);
        if (grown <= 0.01) {
            return out;
        }
        int n = WarRules.spireArms(c);
        double[] d = unit(sub(hand, elbow), new double[] {0.0, 0.0, 1.0});
        double[] u1 = unit(Math.abs(d[1]) > 0.95 ? new double[] {1.0, 0.0, 0.0} : cross(d, new double[] {0.0, 1.0, 0.0}), new double[] {1.0, 0.0, 0.0});
        double[] u2 = cross(u1, d);
        double[] end = add(hand, mul(d, 0.3 + 1.1 * c));
        for (int k = 0; k < n; k++) {
            int turn = k % 2 == 0 ? 1 : -1;
            List<double[]> path = new ArrayList<>();
            int points = 10;
            for (int j = 0; j <= points; j++) {
                double s = (double)j / points;
                double[] along = add(elbow, mul(sub(end, elbow), s));
                double flare = s < 0.72 ? 1.0 : 1.0 + (s - 0.72) / 0.28 * (1.4 + 1.6 * c);
                double radius = (0.1 + 0.07 * c) * flare;
                double ang = Math.PI * 2.0 * k / n + turn * s * Math.PI * 2.4 + now * 0.12 * (1.0 + c) * turn + 0.3 * Math.sin(now * 0.3 + k);
                path.add(add(along, add(mul(u1, Math.cos(ang) * radius), mul(u2, Math.sin(ang) * radius))));
            }
            out.add(new Arm(upTo(path, grown), 0.045 + 0.07 * c, 0.5 + 0.5 * Math.sin(now * 0.35 + k * 1.9), 61 + k, now * 1.3 + k));
        }
        return out;
    }

    /**
     * The Spire Wave let go ({@code age} ticks): its {@code arms} centipedes out along {@code dir} from {@code from} (their tails kept on
     * his {@code hand}) to the head of the mass ({@code WarRules.spireFront}), each its own way round the others and writhing, {@code size}
     * round, heads open going out; at the end they close in on one another round what they carry and crush it; then back.
     */
    public static List<Arm> mass(double[] from, double[] dir, double[] hand, double reach, double size, double speed, double charge, int arms, int seed,
                                 double age, double now) {
        List<Arm> out = new ArrayList<>();
        double front = WarRules.spireFront(age, reach, speed);
        if (front <= 0.05) {
            return out;
        }
        int arrive = WarRules.spireArrive(reach, speed);
        double crush = clamp01((age - arrive) / WarRules.SPIRE_CRUSH) * (1.0 - clamp01((age - arrive - WarRules.SPIRE_CRUSH) / 6.0));
        double[] r1 = unit(Math.abs(dir[1]) > 0.95 ? new double[] {1.0, 0.0, 0.0} : cross(dir, new double[] {0.0, 1.0, 0.0}), new double[] {1.0, 0.0, 0.0});
        double[] r2 = cross(r1, dir);
        for (int k = 0; k < arms; k++) {
            double own = random(seed, k);
            int turn = k % 2 == 0 ? 1 : -1;
            double lead = (k % 3) * 0.3 * size * (1.0 - crush);
            double head = Math.max(0.3, front - lead);
            int points = Math.max(4, Math.min(18, (int)Math.round(head / 1.4)));
            List<double[]> path = new ArrayList<>();
            for (int j = 0; j <= points; j++) {
                double t = (double)j / points;
                double x = head * t;
                double[] axis = add(add(from, mul(dir, x)), mul(sub(hand, from), Math.max(0.0, 1.0 - x / 3.0)));
                double close = crush * smooth(clamp01((t - 0.5) / 0.5));
                double radius = size * 0.6 * Math.sqrt(t) * (1.0 - 0.9 * close);
                double wob = size * 0.18 * Math.sin(x * 0.8 - now * 0.45 + k * 1.7) * t * (1.0 - close);
                double ang = Math.PI * 2.0 * k / arms + turn * x * 0.32 + now * 0.07 * turn + 0.5 * Math.sin(now * 0.17 + k * 1.3) + own;
                path.add(add(axis, add(mul(r1, Math.cos(ang) * (radius + wob)), mul(r2, Math.sin(ang) * (radius + wob)))));
            }
            double jaw = crush > 0.01 ? 1.0 - smooth(clamp01(crush * 2.0)) : 0.9;
            out.add(new Arm(path.toArray(new double[0][]), (0.16 + 0.22 * charge) * (0.85 + 0.3 * own), jaw, 71 + k + Math.floorMod(seed, 13),
                now * 1.6 + k * 2.1));
        }
        return out;
    }

    /** King's Defiance: a spike out of the cocoon at {@code from} toward {@code tip}, {@code age} ticks after the blow ({@code WarRules.defySpike}). */
    public static void defySpike(UkakuModel.Sink sink, double[] from, double[] tip, double age, int seed, double now) {
        double out = WarRules.defySpike(age);
        if (out <= 0.01) {
            return;
        }
        double[] d = sub(tip, from);
        double[] dir = unit(d, new double[] {0.0, 1.0, 0.0});
        double[][] points = {sub(from, mul(dir, 0.45)), add(from, mul(d, 0.3 * out)), add(from, mul(d, 0.65 * out)), add(from, mul(d, out))};
        KingFxModel.spike(sink, points, 0.24, seed, now, 1.0);
    }

    /** The first {@code share} of the way along {@code points} (the rest cut off where it ends). */
    public static double[][] upTo(List<double[]> points, double share) {
        if (share >= 0.999 || points.size() < 2) {
            return points.toArray(new double[0][]);
        }
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += length(sub(points.get(i), points.get(i - 1)));
        }
        double want = total * Math.max(0.0, share);
        List<double[]> out = new ArrayList<>();
        out.add(points.get(0));
        double gone = 0.0;
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            double step = length(sub(b, a));
            if (gone + step >= want) {
                double t = step <= 1.0E-9 ? 0.0 : (want - gone) / step;
                out.add(add(a, mul(sub(b, a), t)));
                break;
            }
            out.add(b);
            gone += step;
        }
        if (out.size() < 2) {
            out.add(add(points.get(0), mul(sub(points.get(1), points.get(0)), 0.01)));
        }
        return out.toArray(new double[0][]);
    }

    static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    /** {@code a} made a unit long ({@code fallback} if it has no length). */
    static double[] unit(double[] a, double[] fallback) {
        double l = length(a);
        return l < 1.0E-9 ? fallback : mul(a, 1.0 / l);
    }

    /** A number in [0, 1) from {@code seed} and {@code k}, the same every time. */
    static double random(long seed, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + k * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}
