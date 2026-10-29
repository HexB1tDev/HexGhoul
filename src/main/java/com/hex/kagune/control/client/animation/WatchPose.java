package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.eto.EtoFleshModel;
import com.hex.kagune.control.eto.WatchRules;

/**
 * 6.2.0, Watch This (pure, tested): where one of Eto's two tentacles is this frame. Rig space throughout (blocks; x to her right,
 * y up from her feet, z forward), on the body as it is drawn. A tentacle is poised over its shoulder like a scorpion's tail, its
 * beak down at what is ahead, never still; it rears back to strike (the beak already on its mark) and lunges, faster and faster,
 * driving the beak through the chest; it holds what it impaled on the beak, the beak through it and out of its back, wherever the
 * body goes; after the throw it follows through and swings back to its place.
 */
public final class WatchPose {
    /** Where each tentacle comes out of her upper back, either side of the spine (torso pixels; x is to her left). */
    static final double[] ROOT_RIGHT = {-2.2, 3.4, 2.0};
    /** The top of the torso at each shoulder. */
    static final double[] SHOULDER_RIGHT = {-4.0, 0.0, 0.0};

    private WatchPose() {
    }

    /** Where a tentacle's beak is gripped ({@code knot}), the way the beak points, how far it is out (0..1), how hard its end holds on. */
    public record Tip(double[] knot, double[] dir, double grown, double hold) {
    }

    /**
     * Where a strike aims from: above its shoulder and out in front of her face (a beak comes down onto what is low and close from
     * in front of her, never across her).
     */
    static double[] aimFrom(int side) {
        return add(WatchRules.pivot(side), new double[] {0.0, 0.35, 0.9});
    }

    /** Poised, at rest: over its shoulder and up, the beak angled down at what is ahead, swaying and coiling a little, alive. */
    public static Tip idle(int side, double time) {
        double phase = side > 0 ? 0.0 : 2.1;
        double[] knot = {side * (0.95 + 0.1 * Math.sin(time * 0.13 + phase)), 2.7 + 0.09 * Math.sin(time * 0.21 + phase),
            0.55 + 0.13 * Math.sin(time * 0.17 + 1.3 + phase)};
        double[] dir = normalize(new double[] {side * 0.12 + 0.08 * Math.sin(time * 0.19 + phase), -0.55 + 0.06 * Math.sin(time * 0.23 + phase), 1.0});
        return new Tip(knot, dir, 1.0, 0.0);
    }

    /**
     * The tentacle on {@code side} in {@code phase}, {@code age} ticks into it ({@code lunge}: its strike's lunge ticks): {@code chest}
     * where what it strikes or carries has its chest now (null: nothing), {@code from} where its beak was when this phase began
     * (a miss: where it got to; after a throw: where it let go), {@code time} for its life.
     */
    public static Tip tip(int side, WatchRules.Phase phase, double age, int lunge, double[] chest, double[] from, double time) {
        Tip rest = idle(side, time);
        double[] pivot = WatchRules.pivot(side);
        double beakIn = WatchRules.BEAK - WatchRules.THROUGH;
        switch (phase) {
            case OUT -> {
                return new Tip(rest.knot, rest.dir, smooth(age / WatchRules.ERUPT), 0.0);
            }
            case IN -> {
                return new Tip(rest.knot, rest.dir, 1.0 - smooth(age / WatchRules.RETRACT), 0.0);
            }
            case STRIKE -> {
                if (chest == null) {
                    return rest;
                }
                double[] aim = normalize(sub(chest, aimFrom(side)));
                // Reared back over the shoulder, coiled, the beak already on its mark...
                double[] rear = add(pivot, new double[] {side * 0.5, 0.95, -0.55});
                if (age < WatchRules.REAR) {
                    double u = smooth(age / WatchRules.REAR);
                    return new Tip(lerp(rest.knot, rear, u), normalize(lerp(rest.dir, aim, u)), 1.0, 0.0);
                }
                // ...then the lunge, still speeding up as the beak goes in: an arc up and out round her (never over her head or across
                // her), the beak leading along it and swinging back onto its mark as it closes.
                double v = clamp01((age - WatchRules.REAR) / Math.max(1, lunge));
                double e = Math.pow(v, 2.2);
                double[] in = sub(chest, mul(aim, beakIn));
                double[] mid = lerp(rear, in, 0.5);
                double[] out = {mid[0], 0.0, mid[2]};
                if (Math.hypot(out[0], out[2]) < 0.3) {
                    out = new double[] {in[0], 0.0, in[2]};
                }
                out = normalize(out);
                double[] bend = add(mid, new double[] {out[0] * 0.9, 0.6, out[2] * 0.9});
                double[] knot = bezier(rear, bend, in, e);
                double[] along = normalize(add(mul(sub(bend, rear), 1.0 - e), mul(sub(in, bend), e)));
                double[] dir = normalize(lerp(aim, along, 0.8 * Math.sin(Math.PI * e)));
                return new Tip(knot, dir, 1.0, e);
            }
            case CARRY -> {
                if (chest == null) {
                    return rest;
                }
                // The beak through it and out of its back, pointing away from her, wherever it is (turning from the way it went in
                // as the lift begins).
                double turn = smooth(age / (WatchRules.IMPALE_HOLD + 2.0));
                double[] dir = normalize(lerp(normalize(sub(chest, aimFrom(side))), normalize(sub(chest, pivot)), turn));
                return new Tip(sub(chest, mul(dir, beakIn)), dir, 1.0, 1.0);
            }
            case RECOVER, MISS -> {
                if (from == null) {
                    return rest;
                }
                double ticks = phase == WatchRules.Phase.RECOVER ? WatchRules.RECOVER : WatchRules.MISS_RECOVER;
                double u = clamp01(age / ticks);
                double[] dir = normalize(sub(from, pivot));
                // Carried on a little past where it let go (or got to), then back to its place.
                double[] on = add(from, mul(dir, 0.6 * (1.0 - (1.0 - Math.min(1.0, u / 0.3)) * (1.0 - Math.min(1.0, u / 0.3)))));
                double back = smooth((u - 0.25) / 0.75);
                return new Tip(lerp(sub(on, mul(dir, beakIn)), rest.knot, back), normalize(lerp(dir, rest.dir, back)), 1.0, 0.0);
            }
            default -> {
                return rest;
            }
        }
    }

    /** The tentacle on {@code side} out of {@code body}'s back to {@code tip}, kept off her body; null when none is out. */
    public static EtoFleshModel.Pose pose(RigPose body, int side, Tip tip, double time, int seed) {
        if (tip.grown() <= 0.0) {
            return null;
        }
        double[] rootPx = {ROOT_RIGHT[0] * side, ROOT_RIGHT[1], ROOT_RIGHT[2]};
        double[] root = BodyFrame.world(body, BodyFrame.model(body, "torso", rootPx));
        double[] back = EtoFleshPose.normalize(EtoFleshPose.sub(BodyFrame.world(body, BodyFrame.model(body, "torso", new double[] {rootPx[0], rootPx[1], rootPx[2] + 1.0})), root));
        double[] up = EtoFleshPose.normalize(EtoFleshPose.sub(BodyFrame.world(body, BodyFrame.model(body, "torso", new double[] {rootPx[0], rootPx[1] - 1.0, rootPx[2]})), root));
        double[] shoulder = BodyFrame.world(body, BodyFrame.model(body, "torso", new double[] {SHOULDER_RIGHT[0] * side, SHOULDER_RIGHT[1], SHOULDER_RIGHT[2]}));
        BodyFrame.Solid solid = new BodyFrame.Solid(body);
        double[][] path = EtoFleshModel.tentaclePath(root, back, shoulder, up, tip.knot(), tip.dir());
        double need = EtoFleshModel.knotRadius(1.0) * 0.5 * EtoFleshPose.LUMPS + EtoFleshPose.MARGIN;
        int last = path.length - 1;
        path[last] = solid.pushOut(path[last], need);
        path = EtoFleshPose.clear(solid, path, up, root, need);
        double[][] grown = EtoFleshModel.grownAlong(path, up, tip.grown());
        return grown == null ? null : new EtoFleshModel.Pose(grown, up, solid.axis(), tip.grown(), EtoFleshModel.Shape.BEAK, 0.0, time, seed,
            EtoFleshPose.keep(solid));
    }

    /** The quadratic curve from {@code a} through toward {@code b} to {@code c}, {@code t} of the way. */
    static double[] bezier(double[] a, double[] b, double[] c, double t) {
        return add(add(mul(a, (1.0 - t) * (1.0 - t)), mul(b, 2.0 * t * (1.0 - t))), mul(c, t * t));
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-9 ? new double[] {0.0, 0.0, 1.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }

    static double clamp01(double u) {
        return Math.max(0.0, Math.min(1.0, u));
    }

    static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3.0 - 2.0 * c);
    }
}
