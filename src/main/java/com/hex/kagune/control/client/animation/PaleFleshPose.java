package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.pale.PaleFleshModel;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;

/**
 * Where Pale's flesh is this frame (6.1.0; pure, tested): its root, the way out of her back and the top of her right shoulder taken
 * from her body as it is drawn ({@link BodyFrame}), and for each move how far it has come out and what its tip is doing. Rig space
 * throughout (blocks; x to her right, y up from her feet, z forward).
 */
public final class PaleFleshPose {
    /** Where it comes out: on her back, between the shoulder blades and a little to the right (torso pixels). */
    static final double[] ROOT = {-1.4, 4.5, 2.0};
    /** A pixel further out of the back, and a pixel up the back, from the root. */
    static final double[] OUT = {-1.4, 4.5, 3.0};
    static final double[] UP = {-1.4, 3.5, 2.0};
    /** The top of the torso at her right shoulder. */
    static final double[] SHOULDER = {-4.0, 0.0, 0.0};
    /** Where the swipe's rake sweeps: this high off the ground (chest height on most things). */
    public static final double SWIPE_HEIGHT = 1.15;
    /** How far (blocks) the blade reaches past the trunk's tip: the trunk stops this short, so the blade's point cuts out to the reach. */
    static final double RAKE_PAST = 2.5;
    /**
     * How far (blocks) the trunk keeps off her body past its own lumps ({@link #LUMPS}: the most they swell it), and how it is
     * checked: at this many points along it, a few times over.
     */
    static final double MARGIN = 0.04;
    static final double LUMPS = 1.36;
    static final int CHECKS = 32;
    static final int DENSE = 96;
    static final int ROUNDS = 14;
    /** The most (blocks) one round moves a control point, and the most in all (the one out of the back; any other). */
    static final double STEP = 0.2;
    static final double MOST_OUT = 0.3;
    static final double MOST_MOVED = 1.6;
    /** Near its root (blocks) the trunk is still coming out of her back, which it may. */
    static final double EMERGING = 0.34;

    private PaleFleshPose() {
    }

    /** Where the flesh comes out of {@code body}'s back (rig space) and the way out of the back there. */
    public static double[][] rootOf(RigPose body) {
        double[] root = BodyFrame.world(body, BodyFrame.model(body, "torso", ROOT));
        return new double[][] {root, normalize(sub(BodyFrame.world(body, BodyFrame.model(body, "torso", OUT)), root))};
    }

    /** The swipe's trunk tip {@code age} ticks in, in the frame she faced when it began (fully reached: the reach is in its growth). */
    public static double[] swipeTip(double age) {
        double bearing = Math.toRadians(PaleRules.swipeBearing(age));
        double reach = PaleRules.SWIPE_REACH - RAKE_PAST;
        return new double[] {Math.sin(bearing) * reach, SWIPE_HEIGHT, Math.cos(bearing) * reach};
    }

    /**
     * A point given in the frame she faced when the move began, in rig space: that frame is {@code turn} radians to the right of
     * where her body faces now.
     */
    public static double[] turned(double[] point, double turn) {
        double c = Math.cos(turn);
        double s = Math.sin(turn);
        return new double[] {point[0] * c + point[2] * s, point[1], -point[0] * s + point[2] * c};
    }

    /**
     * The flesh for {@code move}, {@code age} ticks in, on {@code body}; {@code tip} where its tip goes (rig space), {@code grip} how
     * wide what it holds is, {@code reach} the ticks the grab takes to get there. Null when the move has no flesh out.
     */
    public static PaleFleshModel.Pose of(RigPose body, PaleMove move, double age, double[] tip, double grip, double reach, double time, int seed) {
        double grown;
        PaleFleshModel.Shape shape;
        switch (move) {
            case SWIPE -> {
                grown = Math.min(1.0, PaleRules.swipeExtent(age) / 0.92);
                shape = PaleFleshModel.Shape.BLADE;
            }
            case GRAB_REACH -> {
                grown = smooth(age / Math.max(1.0, reach));
                shape = PaleFleshModel.Shape.HAND_OPEN;
            }
            case GRAB_HOLD -> {
                grown = 1.0;
                shape = PaleFleshModel.Shape.HAND_CLOSED;
            }
            case GRAB_THROW -> {
                double flung = age - PaleRules.grabRelease();
                grown = flung <= 0.0 ? 1.0 : 1.0 - smooth(flung / PaleRules.GRAB_RETRACT);
                shape = flung <= 0.0 ? PaleFleshModel.Shape.HAND_CLOSED : PaleFleshModel.Shape.LASH;
            }
            case GRAB_MISS -> {
                grown = Math.sin(Math.PI * Math.min(1.0, age / PaleRules.GRAB_MISS_TICKS));
                shape = PaleFleshModel.Shape.HAND_OPEN;
            }
            default -> {
                return null;
            }
        }
        if (grown <= 0.0) {
            return null;
        }
        double[] root = BodyFrame.world(body, BodyFrame.model(body, "torso", ROOT));
        double[] back = normalize(sub(BodyFrame.world(body, BodyFrame.model(body, "torso", OUT)), root));
        double[] up = normalize(sub(BodyFrame.world(body, BodyFrame.model(body, "torso", UP)), root));
        double[] shoulder = BodyFrame.world(body, BodyFrame.model(body, "torso", SHOULDER));
        BodyFrame.Solid solid = new BodyFrame.Solid(body);
        boolean hand = shape == PaleFleshModel.Shape.HAND_OPEN || shape == PaleFleshModel.Shape.HAND_CLOSED;
        // The grabbing end comes down onto what it holds from above and stops short of it: its knot and tendrils go round it.
        double hold = hand ? PaleFleshModel.holdOffset(grip) : 0.0;
        // Near the tip, the knot there (for the rake and the grabbing end) is what must pass clear.
        double tipNeed = shape == PaleFleshModel.Shape.LASH ? 0.0 : PaleFleshModel.knotRadius(1.0) * LUMPS + MARGIN;
        double[][] path = PaleFleshModel.path(root, back, shoulder, up, tip, hand);
        if (hand) {
            path = PaleFleshModel.path(root, back, shoulder, up, shorten(tip, path[path.length - 2], hold), true);
        }
        // Its end, and where it comes down onto it from, never in her (what it grabs may be right up against her).
        int last = path.length - 1;
        double end = Math.max(tipNeed, PaleFleshModel.tipRadius(1.0) * LUMPS + MARGIN);
        path[last] = solid.pushOut(path[last], end);
        if (hand) {
            path[last - 1] = solid.pushOut(path[last - 1], end);
        }
        path = clear(solid, path, up, root, tipNeed);
        double[][] grownPath = PaleFleshModel.grownAlong(path, up, grown);
        return grownPath == null ? null : new PaleFleshModel.Pose(grownPath, up, solid.axis(), grown, shape, grip, time, seed, keep(solid));
    }

    /**
     * The Flesh Hammer's club on {@code body} ({@code move} the hammer held or let go, {@code age} ticks in, grown to {@code charge}):
     * out of her right shin, round the foot and on along the line of the shin, bent a little its own way; it sprouts as the leg
     * rises, lengthens with the charge, and after the blow draws back into the leg. Null when none is out.
     */
    public static PaleFleshModel.Pose club(RigPose body, PaleMove move, double age, float charge, double time, int seed) {
        return club(body, move, age, charge, time, seed, false);
    }

    /** ...6.2.0: {@code spiked}, True Anonymity's club, bristling with bone spikes. */
    public static PaleFleshModel.Pose club(RigPose body, PaleMove move, double age, float charge, double time, int seed, boolean spiked) {
        double grown = switch (move) {
            case HAMMER_CHARGE -> smooth((age - PaleRules.HAMMER_SPROUT) / (PaleRules.HAMMER_RAISE_TICKS - PaleRules.HAMMER_SPROUT));
            case HAMMER -> 1.0 - smooth((age - PaleRules.HAMMER_RETRACT_AT) / PaleRules.HAMMER_RETRACT);
            default -> 0.0;
        };
        if (grown <= 0.0) {
            return null;
        }
        double length = PaleRules.hammerLength(charge);
        double girth = PaleRules.hammerGirth(charge);
        double[] shin = BodyFrame.world(body, BodyFrame.model(body, "rightLeg", PaleMotions.CLUB_START));
        double[] foot = BodyFrame.world(body, BodyFrame.model(body, "rightLeg", PaleMotions.SOLE));
        double[] dir = normalize(sub(foot, shin));
        if (move == PaleMove.HAMMER_CHARGE) {
            // Raised, it only grows once the leg points up: on up into the sky, never down under her.
            grown *= smooth((dir[1] - UP_FROM) / (UP_FULL - UP_FROM));
            if (grown <= 0.0) {
                return null;
            }
        }
        double[] up = {0.0, 1.0, 0.0};
        double[] across = normalize(new double[] {dir[2], 0.0, -dir[0]});
        double bendA = Math.sin(seed * 1.7) * 0.07 * length;
        double bendB = Math.cos(seed * 2.3) * 0.06 * length;
        double[][] path = {shin, foot, add(foot, add(mul(dir, 0.33 * length), mul(across, bendA))), add(foot, add(mul(dir, 0.67 * length), mul(across, -bendB))),
            add(foot, mul(dir, length))};
        double[][] grownPath = PaleFleshModel.grownAlong(path, up, grown);
        if (grownPath == null) {
            return null;
        }
        BodyFrame.Solid solid = new BodyFrame.Solid(body);
        return new PaleFleshModel.Pose(grownPath, up, solid.axis(), grown, spiked ? PaleFleshModel.Shape.CLUB_SPIKED : PaleFleshModel.Shape.CLUB, girth, time,
            seed, keep(solid));
    }

    /** How far up (the up part of its direction) her shin must point for the raised club to start growing, and to grow all out. */
    static final double UP_FROM = 0.25;
    static final double UP_FULL = 0.75;

    /** Her body, for what grows off the flesh to keep clear of. */
    static PaleFleshModel.Keep keep(BodyFrame.Solid solid) {
        return new PaleFleshModel.Keep() {
            @Override
            public double[] clear(double[] point, double radius) {
                return solid.pushOut(point, radius);
            }

            @Override
            public double depth(double[] point, double radius) {
                return solid.depth(point, radius) * BodyFrame.SCALE / 16.0;
            }
        };
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    /** {@code tip} brought {@code by} blocks back toward {@code from}. */
    static double[] shorten(double[] tip, double[] from, double by) {
        double[] d = sub(tip, from);
        double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        if (by <= 0.0 || l < 1.0E-6) {
            return tip;
        }
        double k = Math.min(by, l * 0.8) / l;
        return new double[] {tip[0] - d[0] * k, tip[1] - d[1] * k, tip[2] - d[2] * k};
    }

    /**
     * The trunk (all out) kept off her body: wherever (past where it comes out of her back) it would pass within its own thickness
     * of her torso, head or an arm, the bend over her shoulder and the arch are pushed away from her middle line, until it passes
     * clear. As it grows along this path, every part of it already out is clear too.
     */
    static double[][] clear(BodyFrame.Solid solid, double[][] path, double[] up, double[] root, double tipNeed) {
        return clear(solid, path, up, root, tipNeed, (s, total) -> PaleFleshModel.radius(s, 1.0, total));
    }

    /** {@link #clear}, the trunk {@code thickness} thick {@code s} of the way along it, {@code total} blocks long (6.2.0: the Mutated Arm). */
    static double[][] clear(BodyFrame.Solid solid, double[][] path, double[] up, double[] root, double tipNeed,
                            java.util.function.DoubleBinaryOperator thickness) {
        double[][] p = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            p[i] = path[i].clone();
        }
        // Moved: every control point but the root, the stem and the tip; each pushes the trunk most round its own place along it.
        int last = path.length - 1;
        double width = 1.25 / last;
        for (int round = 0; round < ROUNDS; round++) {
            // The trunk evenly along its length (for its thickness), each point knowing where it is between the control points.
            KakujaModel.Spine dense = PaleFleshModel.trunk(p, up, DENSE);
            double[] lengths = new double[DENSE + 1];
            for (int i = 1; i <= DENSE; i++) {
                lengths[i] = lengths[i - 1] + distance(dense.p()[i], dense.p()[i - 1]);
            }
            double total = lengths[DENSE];
            double[][] push = new double[path.length][3];
            double[] hardest = new double[path.length];
            boolean clear = true;
            int j = 1;
            for (int i = 1; i <= CHECKS; i++) {
                double s = i / (double)CHECKS;
                while (j < DENSE && lengths[j] < s * total) {
                    j++;
                }
                double[] at = dense.p()[j];
                double u = j / (double)DENSE;
                if (distance(at, root) < EMERGING) {
                    continue; // still coming out of her back
                }
                double need = Math.max(thickness.applyAsDouble(s, total) * LUMPS + MARGIN, tipNeed * clamp01(1.0 - (1.0 - s) * total / 0.5));
                double depth = Math.min(STEP, solid.depth(at, need) * BodyFrame.SCALE / 16.0);
                if (depth <= 0.0) {
                    continue;
                }
                clear = false;
                double[] away = sub(at, solid.nearest(at));
                double l = Math.sqrt(away[0] * away[0] + away[1] * away[1] + away[2] * away[2]);
                away = l < 1.0E-6 ? up : new double[] {away[0] / l, away[1] / l, away[2] / l};
                for (int c = 2; c < last; c++) {
                    double weight = depth * Math.max(0.0, 1.0 - Math.abs(u - c / (double)last) / width);
                    if (weight > hardest[c]) {
                        hardest[c] = weight;
                        push[c] = mul(away, weight);
                    }
                }
            }
            if (clear) {
                break;
            }
            for (int c = 2; c < last; c++) {
                double[] moved = new double[3];
                for (int k = 0; k < 3; k++) {
                    moved[k] = p[c][k] + push[c][k] * 1.4 + 0.01 * up[k] - path[c][k];
                }
                // Each only so far from where it would be: out of the back least (it sets the way the trunk leaves her back).
                double length = Math.sqrt(moved[0] * moved[0] + moved[1] * moved[1] + moved[2] * moved[2]);
                double most = c == 2 ? MOST_OUT : MOST_MOVED;
                for (int k = 0; k < 3; k++) {
                    p[c][k] = path[c][k] + (length > most ? moved[k] * most / length : moved[k]);
                }
            }
        }
        return p;
    }

    static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static double clamp01(double u) {
        return Math.max(0.0, Math.min(1.0, u));
    }

    static double smooth(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return c * c * (3.0 - 2.0 * c);
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
