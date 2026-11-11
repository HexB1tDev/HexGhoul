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

    /** How much a pixel of the fist off its place weighs against the blade off its line, and the left hand off its grip. */
    static final double HAND = 0.1;
    /** How much the blade off its line weighs (1 - cos: 120 is 18 at 30 degrees off). */
    static final double BLADE = 120.0;
    static final double LEFT = 2.0;
    /** How much a turn away from the key before costs (radians squared): enough to choose between equal answers, no more. */
    static final double KEEP = 4.0;

    /**
     * {@code q}'s keys for {@code aims}, solved: the first and last at rest in the stance; each between, the arm turned (as offsets from
     * the stance's) so the blade and fist are where the aim says.
     */
    public static QuinquePose.Key[] keys(Quinque q, Aim[] aims) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        QuinquePose.Key[] out = new QuinquePose.Key[aims.length];
        double[] prev = {h.pitch(), h.yaw(), h.roll()};
        for (int i = 0; i < aims.length; i++) {
            Aim aim = aims[i];
            double[] at = aim.blade() == null ? prev.clone() : solve(q, aim.blade(), aim.hand(), prev);
            if (aim.blade() == null) {
                at = new double[] {h.pitch(), h.yaw(), h.roll()};
            }
            // The roll unwound to the nearest of its turns to the key before (never the long way round).
            while (at[2] - prev[2] > Math.PI) {
                at[2] -= 2.0 * Math.PI;
            }
            while (at[2] - prev[2] < -Math.PI) {
                at[2] += 2.0 * Math.PI;
            }
            out[i] = new QuinquePose.Key((float)aim.t(), (float)(at[0] - h.pitch()), (float)(at[1] - h.yaw()), (float)(at[2] - h.roll()),
                (float)aim.twist(), (float)aim.lean(), (float)aim.lunge(), (float)aim.drop(), (float)aim.spin());
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

    /**
     * The right arm's pitch, yaw and roll (as {@link QuinqueGrip#aimRight} takes them) pointing {@code q}'s blade along {@code blade} with
     * the fist near {@code hand} (right, up, forward), the left hand able to reach its grip, near {@code near}: a search over the arm's
     * whole range, then narrowed.
     */
    public static double[] solve(Quinque q, double[] blade, double[] hand, double[] near) {
        double[] want = unit(new double[] {-blade[0], -blade[1], -blade[2]}); // right, up, forward to the model's (x left, y down, z back)
        double[] place = {-hand[0], -hand[1], -hand[2]};
        // A coarse search first, the left hand's reach left out (the dearest part); the best few then narrowed with it in.
        java.util.List<double[]> found = new java.util.ArrayList<>();
        java.util.List<Double> scores = new java.util.ArrayList<>();
        for (double pitch = -3.3; pitch <= 1.2; pitch += 0.3) {
            for (double yaw = -1.8; yaw <= 1.8; yaw += 0.3) {
                for (double roll = -Math.PI; roll < Math.PI; roll += 0.35) {
                    double[] at = {pitch, yaw, roll};
                    double c = cost(q, want, place, near, at, false);
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
        found.add(near.clone());
        double[] best = near.clone();
        double score = Double.MAX_VALUE;
        for (double[] seed : found) {
            double[] at = narrow(q, want, place, near, seed);
            double c = cost(q, want, place, near, at, true);
            if (c < score) {
                score = c;
                best = at;
            }
        }
        return best;
    }

    /** How many of the coarse search's best are narrowed. */
    static final int SEEDS = 4;

    /** {@code from} narrowed down to the nearest best (each angle stepped while it helps, the step halved). */
    static double[] narrow(Quinque q, double[] want, double[] place, double[] near, double[] from) {
        double[] best = from.clone();
        double score = cost(q, want, place, near, best, true);
        for (double step = 0.16; step > 0.004; step *= 0.5) {
            boolean moved = true;
            while (moved) {
                moved = false;
                for (int i = 0; i < 3; i++) {
                    for (double sign : new double[] {-1.0, 1.0}) {
                        double[] at = best.clone();
                        at[i] += sign * step;
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
        RigPose p = new RigPose();
        QuinqueGrip.aimRight(p, at[0], at[1], at[2]);
        double[] dir = blade(p, q);
        double off = 1.0 - (dir[0] * want[0] + dir[1] * want[1] + dir[2] * want[2]);
        double[] fist = QuinqueGrip.fist(p, true, false);
        double dx = fist[0] - place[0];
        double dy = fist[1] - place[1];
        double dz = fist[2] - place[2];
        double c = BLADE * off + HAND * (dx * dx + dy * dy + dz * dz);
        if (left && q.twoHanded) {
            float miss = QuinqueGrip.reach(p, q, false);
            c += LEFT * miss * miss;
        }
        double turn = 0.0;
        for (int i = 0; i < 3; i++) {
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
