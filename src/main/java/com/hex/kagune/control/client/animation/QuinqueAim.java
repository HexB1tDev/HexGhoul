package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 4.1.17 (pure, tested; the owner: "We need to improve every single M1 of every single weapon. They're bad ... the animations always have
 * to be top tier, no excuses whatsoever"): a swing written by what it does, not by arm angles. Each key says which way the blade points
 * and where the right fist is (in the chest's frame, as a fencer would describe it: right, up, forward, in pixels from the neck), and the
 * body's own channels; the right arm's pitch, yaw and roll are solved for it on the weapon's own held model (QuinqueGrip's kinematics),
 * the left hand kept where it can reach its grip, each key's turn kept near the one before it (a wrist never flips over between them).
 *
 * <p>Arm angles alone made a level cut point at the ground half way round: the same roll lays a blade flat with the arm out to the side
 * and tips it down with the arm in front. Solved, the blade goes where the key says, all the way.
 */
public final class QuinqueAim {
    private QuinqueAim() {
    }

    /**
     * A key by intent: when (0..1); the blade's direction and the right fist's place (right, up, forward: pixels from the neck, the chest's
     * frame); the body's twist, lean, lunge, drop and spin as {@link QuinquePose.Key} has them.
     */
    public record Aim(double t, double[] blade, double[] hand, double twist, double lean, double lunge, double drop, double spin) {
    }

    static Aim a(double t, double br, double bu, double bf, double hr, double hu, double hf, double twist, double lean, double lunge, double drop) {
        return a(t, br, bu, bf, hr, hu, hf, twist, lean, lunge, drop, 0.0);
    }

    static Aim a(double t, double br, double bu, double bf, double hr, double hu, double hf, double twist, double lean, double lunge, double drop,
                 double spin) {
        return new Aim(t, new double[] {br, bu, bf}, new double[] {hr, hu, hf}, twist, lean, lunge, drop, spin);
    }

    /**
     * 4.1.21: the hands come first. A pixel of the fist off its place weighs as much as the blade some 20 degrees off its line, so the
     * hands go where the key puts them (in front of him, the arms out) and the blade comes as near its line as the grip and the wrists
     * allow. 4.1.18 weighed the blade a thousand times the hand: to point it, the solver swung the stiff arm back through his body.
     */
    static final double HAND = 3.0;
    /** How much the blade off its line weighs (1 - cos: 40 is 5.4 at 30 degrees off). */
    static final double BLADE = 40.0;
    static final double LEFT = 2.0;
    /** How much a turn away from the key before costs (radians squared): enough to choose between equal answers, no more. */
    static final double KEEP = 1.0;
    /** How much the wrists' bend costs (radians squared): used where the arm alone can't lay the blade on its line, no more. */
    static final double WRIST = 6.0;
    /**
     * How far the wrists bend (radians, each way), in the arm's own frame: about its x (the blade tipped toward the line of the forearm or
     * cocked back from it, a wrist's flexion: a cut ends with the blade nearly in line with the arms) and about its z (the blade swung
     * aside, a wrist's deviation: less).
     */
    static final double WRIST_X = 0.8;
    static final double WRIST_Z = 0.35;

    /**
     * {@code q}'s keys for {@code aims}, solved: the first and last at rest in the stance; each between, the arm turned (as offsets from
     * the stance's) and the wrists bent so the fist and blade are where the aim says.
     */
    public static QuinquePose.Key[] keys(Quinque q, Aim[] aims) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        QuinquePose.Key[] out = new QuinquePose.Key[aims.length];
        double[] prev = {h.pitch(), h.yaw(), h.roll(), 0.0, 0.0};
        for (int i = 0; i < aims.length; i++) {
            Aim aim = aims[i];
            double[] at = aim.blade() == null ? new double[] {h.pitch(), h.yaw(), h.roll(), 0.0, 0.0} : solve(q, aim.blade(), aim.hand(), prev);
            // The roll unwound to the nearest of its turns to the key before (never the long way round).
            while (at[2] - prev[2] > Math.PI) {
                at[2] -= 2.0 * Math.PI;
            }
            while (at[2] - prev[2] < -Math.PI) {
                at[2] += 2.0 * Math.PI;
            }
            out[i] = new QuinquePose.Key((float)aim.t(), (float)(at[0] - h.pitch()), (float)(at[1] - h.yaw()), (float)(at[2] - h.roll()),
                (float)aim.twist(), (float)aim.lean(), (float)aim.lunge(), (float)aim.drop(), (float)aim.spin(), (float)at[3], (float)at[4]);
            prev = at;
        }
        return out;
    }

    /** The stance (a swing's first and last key). */
    static Aim rest(double t) {
        return new Aim(t, null, null, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    /** The stance after a whole turn (a spinning finisher's last key: the same pose, the body round once). */
    static Aim restTurned(double t, double spin) {
        return new Aim(t, null, null, 0.0, 0.0, 0.0, 0.0, spin);
    }

    /** A body with its right arm and wrists at {@code at} (pitch, yaw, roll as {@link QuinqueGrip#aimRight} takes them; wrists x, z). */
    public static RigPose posed(double[] at) {
        RigPose p = new RigPose();
        QuinqueGrip.aimRight(p, at[0], at[1], at[2]);
        if (at.length > 4) {
            p.weaponX = (float)at[3];
            p.weaponZ = (float)at[4];
        }
        return p;
    }

    /**
     * The right arm's pitch, yaw and roll (as {@link QuinqueGrip#aimRight} takes them) and the wrists' x and z putting the fist at
     * {@code hand} (right, up, forward) and {@code q}'s blade along {@code blade}, the left hand able to reach its grip, near {@code near}:
     * a search over the arm's whole range (the wrists straight), then narrowed with the wrists in.
     */
    public static double[] solve(Quinque q, double[] blade, double[] hand, double[] near) {
        double[] want = unit(new double[] {-blade[0], -blade[1], -blade[2]}); // right, up, forward to the model's (x left, y down, z back)
        double[] place = {-hand[0], -hand[1], -hand[2]};
        double[] from = near.length > 4 ? near : new double[] {near[0], near[1], near[2], 0.0, 0.0};
        // A coarse search first, the left hand's reach left out (the dearest part); the best few then narrowed with it in.
        java.util.List<double[]> found = new java.util.ArrayList<>();
        java.util.List<Double> scores = new java.util.ArrayList<>();
        for (double pitch = -3.3; pitch <= 1.2; pitch += 0.2) {
            for (double yaw = -1.8; yaw <= 1.8; yaw += 0.2) {
                for (double roll = -Math.PI; roll < Math.PI; roll += 0.25) {
                    double[] at = {pitch, yaw, roll, 0.0, 0.0};
                    double c = cost(q, want, place, from, at, false);
                    int i = 0;
                    while (i < scores.size() && scores.get(i) < c) {
                        i++;
                    }
                    if (i < SEEDS) {
                        found.add(i, at);
                        scores.add(i, c);
                        if (found.size() > SEEDS) {
                            found.remove(SEEDS);
                            scores.remove(SEEDS);
                        }
                    }
                }
            }
        }
        found.add(from.clone());
        double[] best = from.clone();
        double score = Double.MAX_VALUE;
        for (double[] seed : found) {
            double[] at = narrow(q, want, place, from, seed);
            double c = cost(q, want, place, from, at, true);
            if (c < score) {
                score = c;
                best = at;
            }
        }
        return best;
    }

    /** How many of the coarse search's best are narrowed. */
    static final int SEEDS = 6;

    /** {@code from} narrowed down to the nearest best (each angle stepped while it helps, the step halved; the wrists kept in range). */
    static double[] narrow(Quinque q, double[] want, double[] place, double[] near, double[] from) {
        double[] best = from.clone();
        double score = cost(q, want, place, near, best, true);
        for (double step = 0.16; step > 0.004; step *= 0.5) {
            boolean moved = true;
            while (moved) {
                moved = false;
                for (int i = 0; i < 5; i++) {
                    for (double sign : new double[] {-1.0, 1.0}) {
                        double[] at = best.clone();
                        at[i] += sign * step;
                        if (Math.abs(at[3]) > WRIST_X || Math.abs(at[4]) > WRIST_Z) {
                            continue;
                        }
                        double c = cost(q, want, place, near, at, true);
                        if (c < score) {
                            score = c;
                            best = at;
                            moved = true;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** How far {@code at} is from what is wanted (see the weights). */
    static double cost(Quinque q, double[] want, double[] place, double[] near, double[] at, boolean left) {
        RigPose p = posed(at);
        double[] dir = blade(p, q);
        double off = 1.0 - (dir[0] * want[0] + dir[1] * want[1] + dir[2] * want[2]);
        double[] fist = QuinqueGrip.fist(p, true, false);
        double dx = fist[0] - place[0];
        double dy = fist[1] - place[1];
        double dz = fist[2] - place[2];
        double c = BLADE * off + HAND * (dx * dx + dy * dy + dz * dz) + WRIST * (at[3] * at[3] + at[4] * at[4]);
        if (left && q.twoHanded) {
            float miss = QuinqueGrip.reach(p, q, false);
            c += LEFT * miss * miss;
        }
        double turn = 0.0;
        for (int i = 0; i < 5; i++) {
            double d = at[i] - near[i];
            if (i == 2) {
                d = Math.IEEEremainder(d, 2.0 * Math.PI);
            }
            turn += d * d;
        }
        return c + KEEP * turn;
    }

    /** The way {@code q}'s blade points as drawn on {@code p} (the model's frame, a unit vector): from its left grip toward its right. */
    public static double[] blade(RigPose p, Quinque q) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        double[] r = QuinqueGrip.held(p, h, false, h.rightGrip());
        double[] l = QuinqueGrip.held(p, h, false, h.leftGrip());
        return unit(new double[] {r[0] - l[0], r[1] - l[1], r[2] - l[2]});
    }

    public static double[] unit(double[] v) {
        double n = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return n < 1.0E-9 ? new double[] {0.0, 0.0, -1.0} : new double[] {v[0] / n, v[1] / n, v[2] / n};
    }
}
