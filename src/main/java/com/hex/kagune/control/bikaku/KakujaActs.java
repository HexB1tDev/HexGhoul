package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.graft.GraftMesh;

/**
 * 3.5.7: the Bikaku kakuja's own moves (the owner's spec: Thousand Jaws, Endless Hunger, Flesh Cataclysm, Undying Abomination) and its
 * Autonomous Predation, as what the kakuja does each frame ({@link MawKakuja.Act}; pure: the renderer, the previews, the tests and the
 * server's timing all read them here). Body frame, as {@link MawKakuja}: blocks from his feet, x right, y up, z ahead.
 */
public final class KakujaActs {
    private KakujaActs() {
    }

    /** The mouthless tentacle (it coils round Endless Hunger's catch). */
    public static final int WRAPPER = 3;
    /** Where Endless Hunger's catch is held in the great maw as it shuts (its middle). */
    public static final double[] MAW_GRIP = {0.85, 1.6, 2.75};
    /** How far round Flesh Cataclysm's tentacles are swept (radians). */
    public static final double SWEEP = Math.toRadians(110.0);
    /** Thousand Jaws with no body for a tentacle: how far out it lashes at the air. */
    static final double LASH = 8.5;

    /**
     * What the kakuja does {@code age} ticks into {@code move} ({@code time} ticks into the world): {@code targets[i]} where tentacle
     * {@code i}'s body is (null: none), {@code held} where Endless Hunger's catch is (null: as {@link #hungerHeld} puts it),
     * {@code point} where the move was aimed (body frame), {@code caught} whether Endless Hunger caught it. Past its end, or no kakuja
     * move, nothing.
     */
    public static MawKakuja.Act act(BikakuMove move, double age, double time, double[][] targets, double[] held, double[] point, boolean caught) {
        MawKakuja.Act act = MawKakuja.Act.still();
        if (move == null || !move.kakuja() || !(age >= 0.0)) {
            return act;
        }
        return switch (move) {
            case JAWS -> age < move.ticks() ? jaws(act, age, time, targets) : act;
            case HUNGER -> age < (caught ? move.ticks() : move.missedTicks()) ? hunger(act, age, time, held, point, caught) : act;
            case CATACLYSM -> age < move.ticks() ? cataclysm(act, age, time) : act;
            case UNDYING -> age < move.ticks() ? withMend(act, mend(age)) : act;
            default -> act;
        };
    }

    /** True if {@code move} uses tentacle {@code i} (aimed at {@code point}): nothing else of his moves it meanwhile. */
    public static boolean uses(BikakuMove move, int i, double[] point) {
        if (move == null) {
            return false;
        }
        return switch (move) {
            case JAWS -> MawKakuja.mouthed(i);
            case CATACLYSM -> true;
            case HUNGER -> i == WRAPPER || isBiter(hungerBiters(point), i);
            default -> false;
        };
    }

    // ---------------------------------------------------------------- Thousand Jaws

    private static MawKakuja.Act jaws(MawKakuja.Act act, double age, double time, double[][] targets) {
        for (int i = 0; i < MawKakuja.tentacles(); i++) {
            if (!MawKakuja.mouthed(i)) {
                continue;
            }
            int start = -1;
            int cycle = 0;
            for (int at : BikakuMove.jawsStarts(i)) {
                if (age >= at) {
                    start = at;
                    cycle++;
                }
            }
            double into = start < 0 ? -1.0 : age - start;
            double reach = jawsReach(into, age);
            double[] target = targets != null && i < targets.length && targets[i] != null ? targets[i] : lash(i, cycle);
            act = act.lunging(i, target, reach, jawsMouth(into));
        }
        // Its body's grins snap at whatever is close.
        double open = 0.0;
        for (int snap : BikakuMove.JAWS_SNAPS) {
            if (age >= snap - 5 && age < snap) {
                open = Math.max(open, GraftMesh.smooth((age - (snap - 5)) / 5.0));
            }
        }
        return new MawKakuja.Act(act.squeeze(), act.straight(), act.turn(), open, act.maw(), act.lunge(), act.reach(), act.bite(), act.wrapper(),
            act.wrapAt(), act.wrap(), act.mend());
    }

    /**
     * How far into its lunge a Thousand Jaws tentacle is, {@code into} ticks into its bite (negative: not begun), {@code age} into the move:
     * thrown out as it shuts at {@link BikakuMove#JAWS_BITE}, drawn half back, thrown again; all the way back over the last ticks.
     */
    static double jawsReach(double into, double age) {
        double out = GraftMesh.smooth(GraftMesh.clamp01(age / 4.0)) * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - (BikakuMove.JAWS.ticks() - 8)) / 8.0)));
        if (into < 0.0) {
            return 0.35 * out;
        }
        int bite = BikakuMove.JAWS_BITE;
        double w;
        if (into < bite) {
            double f = into / bite;
            w = 0.35 + 0.65 * f * f * (3.0 - 2.0 * f);
        } else if (into < bite + 2) {
            w = 1.0;
        } else {
            w = 1.0 - 0.65 * GraftMesh.smooth(GraftMesh.clamp01((into - bite - 2) / (BikakuMove.JAWS_CYCLE - bite - 2.0)));
        }
        return w * out;
    }

    /** Its mouth: thrown wide on the way, snapped shut as it bites, slack after. */
    static double jawsMouth(double into) {
        int bite = BikakuMove.JAWS_BITE;
        if (into < 0.0) {
            return 0.3;
        }
        if (into < bite - 1) {
            return GraftMesh.clamp01(0.35 + into / (bite - 1.0));
        }
        if (into < bite) {
            return 1.0 - (into - (bite - 1));
        }
        return 0.04 + 0.4 * GraftMesh.clamp01((into - bite - 2) / 3.0);
    }

    /** Where tentacle {@code i} lashes with no body to bite: out its own way, a little further round each time. */
    static double[] lash(int i, int cycle) {
        double[] way = MawKakuja.turned(GraftMesh.add(MawKakuja.BODY, MawKakuja.sweepWay(i)), 0.35 * Math.sin(cycle * 2.1 + i));
        double[] flat = GraftMesh.sub(way, MawKakuja.BODY);
        return new double[] {MawKakuja.BODY[0] + flat[0] * LASH, 1.2 + 0.8 * Math.sin(cycle * 1.3 + i), MawKakuja.BODY[2] + flat[2] * LASH};
    }

    // ---------------------------------------------------------------- Endless Hunger

    /** The three mouthed tentacles that go after a body at {@code point} (body frame): those facing nearest its way. */
    public static int[] hungerBiters(double[] point) {
        double[] to = point == null ? new double[] {0.0, 0.0, 1.0} : new double[] {point[0] - MawKakuja.BODY[0], 0.0, point[2] - MawKakuja.BODY[2]};
        double flat = Math.hypot(to[0], to[2]);
        to = flat < 1.0E-6 ? new double[] {0.0, 0.0, 1.0} : new double[] {to[0] / flat, 0.0, to[2] / flat};
        int[] best = {-1, -1, -1};
        double[] score = {-9.0, -9.0, -9.0};
        for (int i = 0; i < MawKakuja.tentacles(); i++) {
            if (!MawKakuja.mouthed(i)) {
                continue;
            }
            double d = GraftMesh.dot(MawKakuja.sweepWay(i), to);
            for (int k = 0; k < 3; k++) {
                if (d > score[k]) {
                    for (int m = 2; m > k; m--) {
                        score[m] = score[m - 1];
                        best[m] = best[m - 1];
                    }
                    score[k] = d;
                    best[k] = i;
                    break;
                }
            }
        }
        java.util.Arrays.sort(best);
        return best;
    }

    private static boolean isBiter(int[] biters, int i) {
        for (int b : biters) {
            if (b == i) {
                return true;
            }
        }
        return false;
    }

    /** Endless Hunger's catch, {@code age} ticks in, caught at {@code at}: held where it was, then pulled into the great maw and held there. */
    public static double[] hungerHeld(double[] at, double age) {
        if (age < BikakuMove.HUNGER_PULL) {
            return at.clone();
        }
        double w = GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.HUNGER_PULL) / (double)(BikakuMove.HUNGER_CRUSH - BikakuMove.HUNGER_PULL)));
        return GraftMesh.lerp(at, MAW_GRIP, w);
    }

    /** The tick of Endless Hunger's {@code n}th bite and which of its three mouths makes it ({@code n} 0 ..). */
    public static int hungerBiter(int n) {
        return n % 3;
    }

    private static MawKakuja.Act hunger(MawKakuja.Act act, double age, double time, double[] held, double[] point, boolean caught) {
        int[] biters = hungerBiters(point);
        int land = BikakuMove.HUNGER.land();
        double[] at = point == null ? new double[] {0.0, 1.0, 6.0} : point;
        double[] catchAt = held != null ? held : hungerHeld(at, age);
        if (!caught) {
            // Thrown at it, shut on nothing, drawn back.
            double reach = age < land ? GraftMesh.smooth(age / land) : 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - land - 2) / 12.0));
            double mouth = age < land - 1 ? GraftMesh.clamp01(0.4 + age / (land - 1.0)) : age < land ? 1.0 - (age - (land - 1)) : 0.06;
            for (int b : biters) {
                act = act.lunging(b, at, reach, mouth);
            }
            return act;
        }
        double end = BikakuMove.HUNGER.ticks();
        double away = 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.HUNGER_RELEASE) / (end - BikakuMove.HUNGER_RELEASE - 1.0)));
        for (int k = 0; k < biters.length; k++) {
            // Each bites at its own beats, drawn a little back and thrown again between.
            double reach;
            double mouth;
            if (age < land) {
                reach = GraftMesh.smooth(age / land);
                mouth = age < land - 1 ? GraftMesh.clamp01(0.4 + age / (land - 1.0)) : 1.0 - (age - (land - 1));
            } else {
                // Held on from the grab, easing a little back, thrown in again for each of its bites.
                double reachNow = 1.0 - 0.15 * GraftMesh.smooth(GraftMesh.clamp01((age - land) / 4.0));
                double mouthNow = 0.04 + 0.2 * GraftMesh.smooth(GraftMesh.clamp01((age - land - 1.0) / 3.0));
                for (int n = 0; n < BikakuMove.HUNGER_BITES.length; n++) {
                    if (hungerBiter(n) != k) {
                        continue;
                    }
                    double since = age - BikakuMove.HUNGER_BITES[n];
                    double bump = since < -3.0 || since > 4.0 ? 0.0 : since < 0.0 ? GraftMesh.smooth((since + 3.0) / 3.0) : 1.0 - GraftMesh.smooth(since / 4.0);
                    reachNow = Math.max(reachNow, 0.85 + 0.15 * bump);
                    if (since >= -3.0 && since < -0.5) {
                        mouthNow = Math.max(mouthNow, GraftMesh.smooth(GraftMesh.clamp01((since + 3.0) / 1.5)));
                    } else if (since >= -0.5 && since < 0.5) {
                        mouthNow = 0.04;
                    }
                }
                reach = reachNow * away;
                mouth = mouthNow;
            }
            // From its own side round the catch.
            double[] off = GraftMesh.mul(GraftMesh.normalize(GraftMesh.sub(MawKakuja.TENTACLES[biters[k]].root(), catchAt)), 0.35);
            act = act.lunging(biters[k], GraftMesh.add(catchAt, off), reach, mouth);
        }
        double wrap = GraftMesh.smooth(GraftMesh.clamp01((age - 4.0) / (BikakuMove.HUNGER_COILED - 4.0))) * away;
        double mawOwn = MawKakuja.mawOpen(time);
        double maw;
        if (age < BikakuMove.HUNGER_PULL) {
            maw = mawOwn;
        } else if (age < BikakuMove.HUNGER_CRUSH - 1) {
            maw = mawOwn + (1.0 - mawOwn) * GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.HUNGER_PULL) / 4.0));
        } else if (age < BikakuMove.HUNGER_CRUSH) {
            maw = 1.0 - (age - (BikakuMove.HUNGER_CRUSH - 1));
        } else if (age < BikakuMove.HUNGER_RELEASE) {
            maw = 0.02;
        } else {
            maw = 0.02 + (mawOwn - 0.02) * GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.HUNGER_RELEASE) / 8.0));
        }
        return new MawKakuja.Act(act.squeeze(), act.straight(), act.turn(), act.grins(), maw, act.lunge(), act.reach(), act.bite(), WRAPPER, catchAt, wrap,
            act.mend());
    }

    // ---------------------------------------------------------------- Flesh Cataclysm

    /** How drawn in or burst out it is, {@code age} ticks in (1 as it is). */
    public static double squeeze(double age) {
        int burst = BikakuMove.CATACLYSM.land();
        if (age < BikakuMove.CATA_DRAWN) {
            return 1.0 - 0.22 * GraftMesh.smooth(age / BikakuMove.CATA_DRAWN);
        }
        if (age < burst) {
            double f = (age - BikakuMove.CATA_DRAWN) / (double)(burst - BikakuMove.CATA_DRAWN);
            return 0.78 + 0.36 * f * f;
        }
        return 1.14 - 0.14 * GraftMesh.smooth(GraftMesh.clamp01((age - burst) / 14.0));
    }

    /** How far its tentacles are thrown out straight. */
    public static double thrown(double age) {
        int burst = BikakuMove.CATACLYSM.land();
        if (age < BikakuMove.CATA_DRAWN) {
            return 0.0;
        }
        if (age < burst) {
            return GraftMesh.smooth((age - BikakuMove.CATA_DRAWN) / (double)(burst - BikakuMove.CATA_DRAWN));
        }
        if (age < BikakuMove.CATA_SWEPT) {
            return 1.0;
        }
        return 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.CATA_SWEPT) / (BikakuMove.CATACLYSM.ticks() - BikakuMove.CATA_SWEPT - 2.0)));
    }

    /** How far round it they are swept (radians). */
    public static double turn(double age) {
        int burst = BikakuMove.CATACLYSM.land();
        if (age < burst) {
            return 0.0;
        }
        if (age < BikakuMove.CATA_SWEPT) {
            return SWEEP * GraftMesh.smooth((age - burst) / (double)(BikakuMove.CATA_SWEPT - burst));
        }
        return SWEEP * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.CATA_SWEPT) / (BikakuMove.CATACLYSM.ticks() - BikakuMove.CATA_SWEPT - 2.0))));
    }

    /** How wide every mouth on it is thrown open: the roar. */
    public static double roar(double age) {
        int burst = BikakuMove.CATACLYSM.land();
        if (age < BikakuMove.CATA_DRAWN) {
            return 0.0;
        }
        if (age < burst) {
            return GraftMesh.smooth((age - BikakuMove.CATA_DRAWN) / (double)(burst - BikakuMove.CATA_DRAWN));
        }
        return 1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - BikakuMove.CATA_SWEPT - 2) / 10.0));
    }

    private static MawKakuja.Act cataclysm(MawKakuja.Act act, double age, double time) {
        double roar = roar(age);
        double own = MawKakuja.mawOpen(time);
        return new MawKakuja.Act(squeeze(age), thrown(age), turn(age), roar, own + (1.0 - own) * roar, act.lunge(), act.reach(), act.bite(), act.wrapper(),
            act.wrapAt(), act.wrap(), act.mend());
    }

    /** Where tentacle {@code i}'s mouth is {@code age} ticks into Flesh Cataclysm (the server sweeps its blow along it). */
    public static double[] cataclysmHead(int i, double age, double time) {
        double[][] line = MawKakuja.tentacle(i, time, null, 0.0, squeeze(age), thrown(age), turn(age));
        return line[line.length - 1];
    }

    // ---------------------------------------------------------------- Undying Abomination

    /** How far its mending shows, {@code age} ticks in: in over half a second, out over its last second and a half. */
    public static double mend(double age) {
        double end = BikakuMove.UNDYING.ticks();
        return GraftMesh.smooth(GraftMesh.clamp01(age / 10.0)) * (1.0 - GraftMesh.smooth(GraftMesh.clamp01((age - (end - 30.0)) / 30.0)));
    }

    /** {@code act} with its flesh mending ({@code mend}). */
    public static MawKakuja.Act withMend(MawKakuja.Act act, double mend) {
        return new MawKakuja.Act(act.squeeze(), act.straight(), act.turn(), act.grins(), act.maw(), act.lunge(), act.reach(), act.bite(), act.wrapper(),
            act.wrapAt(), act.wrap(), Math.max(act.mend(), mend));
    }

    // ---------------------------------------------------------------- Autonomous Predation

    /** How long one of its own bites lasts (as the tentacle bite of M1), and the tick it shuts. */
    public static int autoTicks() {
        return BikakuRules.Bite.TENTACLE.ticks();
    }

    /** {@code act} with tentacle {@code i} biting on its own at {@code target}, {@code age} ticks into the bite (past its end, nothing). */
    public static MawKakuja.Act withAuto(MawKakuja.Act act, int i, double[] target, double age) {
        if (target == null || !(age >= 0.0) || age >= autoTicks() || act.lunge()[i] != null || i == act.wrapper()) {
            return act;
        }
        return act.lunging(i, target, MawKakuja.lunge(age), MawKakuja.lungeMouth(age));
    }
}
