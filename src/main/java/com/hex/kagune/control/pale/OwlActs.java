package com.hex.kagune.control.pale;

/**
 * 6.2.4: what the One-Eyed Owl's creature does with itself through each of its moves (pure, tested): where each of its great hands
 * goes, how its body rears, lunges and turns, how its head bows and lunges, how wide its jaw opens, how its crest flares, what its
 * kagune does; timed on the moves' own ticks ({@link OwlRules}, {@link OwlShards}, {@link PaleRules}) so every blow lands as the
 * server has it. Null between moves: it crawls and idles ({@link OwlCreature}).
 *
 * <p>The moves: its M1 a maul (its right claw raked across, its left, its kagune's four points driven in, then its jaws); J held,
 * spikes fired off its crest (its head lowered under them); N, its great hand seizing what it looks at, holding it up before its
 * jaws, slamming it down; J tapped, both arms swept round it; its Pounce, Lunge, Fissure, giant fist and Watch This.
 *
 * <p>Places are the creature model's (pixels: x its left, y up from the ground under it, z back; its front is -z).
 */
public final class OwlActs {
    private OwlActs() {
    }

    /** How the kagune moves through an act: as the move swings it, held at rest, driven in point first ({@link Act#stab}), spread wide. */
    public enum Kagune { FOLLOW, REST, STAB, SPREAD }

    /**
     * One moment of an act: each hand's wrist (null: it crawls) and how much of the way there it is, the body's lean forward, turn to
     * its right and roll (radians), how far it is lifted and pushed ahead (pixels); the head's bow and turn (radians), how far it lunges
     * out (pixels); the jaw open (0..1), the crest flared (0..1); the kagune, and how far driven in (-0.35 drawn back .. 1) when it
     * stabs or spread (0..1) when it spreads; how far its acting hands' fingers are closed (0 open .. 1 a fist); how far round (radians,
     * + to its right) the whole of it is turned, its cloak and all.
     */
    public record Act(double[] right, double[] left, double rightWeight, double leftWeight, double pitch, double turn, double roll, double lift,
                      double ahead, double bow, double headTurn, double lunge, double jaw, double crest, Kagune kagune, double stab, double grip,
                      double spin) {
        public Act(double[] right, double[] left, double rightWeight, double leftWeight, double pitch, double turn, double roll, double lift,
                   double ahead, double bow, double headTurn, double lunge, double jaw, double crest, Kagune kagune, double stab) {
            this(right, left, rightWeight, leftWeight, pitch, turn, roll, lift, ahead, bow, headTurn, lunge, jaw, crest, kagune, stab, 0.0, 0.0);
        }

        public Act(double[] right, double[] left, double rightWeight, double leftWeight, double pitch, double turn, double roll, double lift,
                   double ahead, double bow, double headTurn, double lunge, double jaw, double crest, Kagune kagune, double stab, double grip) {
            this(right, left, rightWeight, leftWeight, pitch, turn, roll, lift, ahead, bow, headTurn, lunge, jaw, crest, kagune, stab, grip, 0.0);
        }
    }

    /** Where it holds what its great hand has seized (the creature's pixels: the middle of the body it holds, before its jaws). */
    public static final double[] HOLD = {-6.0, 40.0, -52.0};
    /** Its right wrist round it, holding it. */
    static final double[] HOLD_WRIST = {-8.0, 33.0, -49.0};

    // ------------------------------------------------------------------ building blocks

    /**
     * A track of keys ({time, x, y, z} each) flowed through: where it is at {@code t}. Each way (x, y, z) is carried on through a key
     * it keeps going the same way through (no stop there), and comes to rest at one it turns back at, and at the first and last
     * (a cubic through them that never overshoots a key).
     */
    static double[] track(double t, double[][] keys) {
        if (t <= keys[0][0]) {
            return new double[] {keys[0][1], keys[0][2], keys[0][3]};
        }
        for (int i = 0; i + 1 < keys.length; i++) {
            double[] a = keys[i];
            double[] b = keys[i + 1];
            if (t <= b[0]) {
                double h = Math.max(1.0E-6, b[0] - a[0]);
                double u = (t - a[0]) / h;
                double[] out = new double[3];
                for (int k = 1; k <= 3; k++) {
                    double ma = slope(keys, i, k) * h;
                    double mb = slope(keys, i + 1, k) * h;
                    double u2 = u * u;
                    double u3 = u2 * u;
                    out[k - 1] = (2.0 * u3 - 3.0 * u2 + 1.0) * a[k] + (u3 - 2.0 * u2 + u) * ma + (-2.0 * u3 + 3.0 * u2) * b[k] + (u3 - u2) * mb;
                }
                return out;
            }
        }
        double[] z = keys[keys.length - 1];
        return new double[] {z[1], z[2], z[3]};
    }

    /**
     * How fast way {@code k} goes through key {@code i}: none at the first and last and where it turns back; else the harmonic mean
     * of the two either side, so it never overshoots (Fritsch and Carlson's).
     */
    static double slope(double[][] keys, int i, int k) {
        if (i == 0 || i == keys.length - 1) {
            return 0.0;
        }
        double before = (keys[i][k] - keys[i - 1][k]) / Math.max(1.0E-6, keys[i][0] - keys[i - 1][0]);
        double after = (keys[i + 1][k] - keys[i][k]) / Math.max(1.0E-6, keys[i + 1][0] - keys[i][0]);
        if (before * after <= 0.0) {
            return 0.0;
        }
        return 2.0 / (1.0 / before + 1.0 / after);
    }

    /** A value eased through keys ({time, value} each). */
    static double curve(double t, double[][] keys) {
        if (t <= keys[0][0]) {
            return keys[0][1];
        }
        for (int i = 0; i + 1 < keys.length; i++) {
            double[] a = keys[i];
            double[] b = keys[i + 1];
            if (t <= b[0]) {
                return a[1] + (b[1] - a[1]) * smooth((t - a[0]) / Math.max(1.0E-6, b[0] - a[0]));
            }
        }
        return keys[keys.length - 1][1];
    }

    /** In over {@code a} ticks from 0, out over the last {@code b} of {@code end}: 0..1. */
    static double inOut(double t, double a, double end, double b) {
        return smooth(t / a) * (1.0 - smooth((t - (end - b)) / b));
    }

    static double[] mirror(double[] p) {
        return p == null ? null : new double[] {-p[0], p[1], p[2]};
    }

    static double smooth(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return c * c * (3.0 - 2.0 * c);
    }

    /** An act of only its body and head (its hands crawl, its kagune as the move swings it). */
    static Act body(double pitch, double turn, double lift, double ahead, double bow, double lunge, double jaw, double crest, Kagune kagune, double stab) {
        return new Act(null, null, 0.0, 0.0, pitch, turn, 0.0, lift, ahead, bow, 0.0, lunge, jaw, crest, kagune, stab);
    }

    // ------------------------------------------------------------------ the acts

    /**
     * The creature's act {@code age} ticks into {@code move} ({@code detail} and {@code charge} as the move has them; {@code aim}, the
     * creature's own pixels, where what it reaches for is, or null): null when it is not one of its moves.
     */
    public static Act of(PaleMove move, double age, int detail, float charge, double[] aim) {
        return switch (move) {
            case OWL_STRIKE -> maul(age, Math.max(0, Math.min(OwlRules.REND_STAGES - 1, detail)));
            case OWL_WHIRL -> whirl(age);
            case OWL_LEAP -> pounce(age, charge);
            case OWL_FISSURE_CHARGE -> rear(age);
            case OWL_FISSURE -> slam(age);
            case OWL_PUNCH_CHARGE -> windUp(age);
            case OWL_PUNCH -> fist(age);
            case OWL_KICK -> lunge(age, Math.max(1, detail));
            case OWL_WATCH -> roar(age);
            case OWL_VOLLEY -> barrage(age, detail == 1, charge);
            case OWL_GRAB_REACH -> reach(age, Math.max(1.0, charge), aim);
            case OWL_GRAB_HOLD -> hold(age, aim);
            case OWL_GRAB_SLAM -> slamDown(age, aim);
            case OWL_GRAB_MISS -> miss(age, aim);
            default -> null;
        };
    }

    /**
     * Its M1, its maul: the right claw raked across before it (reared back out to its right, swept through to its left), the left
     * claw the same the other way, its kagune's points driven in as it lunges, then its jaws: reared back gaping and snapped shut on
     * the blow's tick, its head lunged out and down.
     */
    static Act maul(double t, int stage) {
        double hit = OwlRules.REND_IMPACT[stage];
        double end = OwlRules.REND_TICKS[stage];
        switch (stage) {
            case 0, 1 -> {
                double[] claw = track(t, new double[][] {{0.0, -50.0, 14.0, -36.0}, {hit - 2.0, -66.0, 36.0, -6.0}, {hit - 0.4, -30.0, 30.0, -56.0},
                    {hit + 1.2, 12.0, 20.0, -50.0}, {end, -40.0, 11.8, -24.0}});
                double w = inOut(t, 1.5, end, 3.0);
                double turn = curve(t, new double[][] {{0.0, 0.0}, {hit - 2.0, 0.32}, {hit + 1.0, -0.26}, {end, 0.0}});
                double pitch = curve(t, new double[][] {{0.0, 0.0}, {hit, 0.14}, {end, 0.0}});
                double jaw = curve(t, new double[][] {{0.0, 0.0}, {hit, 0.45}, {end, 0.0}});
                double hook = curve(t, new double[][] {{0.0, 0.0}, {hit - 2.0, 0.15}, {hit, 0.55}, {end, 0.0}});
                if (stage == 0) {
                    return new Act(claw, null, w, 0.0, pitch, turn, 0.0, 0.0, -2.0 * w, 0.0, 0.0, 0.0, jaw, 0.0, Kagune.REST, 0.0, hook);
                }
                return new Act(null, mirror(claw), 0.0, w, pitch, -turn, 0.0, 0.0, -2.0 * w, 0.0, 0.0, 0.0, jaw, 0.0, Kagune.REST, 0.0, hook);
            }
            case 2 -> {
                double pitch = curve(t, new double[][] {{0.0, 0.0}, {hit - 1.5, -0.16}, {hit, 0.26}, {hit + 2.5, 0.22}, {end, 0.0}});
                double lift = curve(t, new double[][] {{0.0, 0.0}, {hit - 1.5, 5.0}, {hit, -2.0}, {end, 0.0}});
                double ahead = curve(t, new double[][] {{0.0, 0.0}, {hit, -6.0}, {hit + 2.5, -6.0}, {end, 0.0}});
                double stab = curve(t, new double[][] {{0.0, 0.0}, {hit - 1.5, -0.35}, {hit - 0.3, 1.0}, {hit + 2.5, 1.0}, {end, 0.0}});
                return body(pitch, 0.0, lift, ahead, 0.0, 0.0, 0.35, 0.0, Kagune.STAB, stab);
            }
            default -> {
                double bow = curve(t, new double[][] {{0.0, 0.0}, {hit - 1.5, -0.2}, {hit, 0.36}, {hit + 2.0, 0.3}, {end, 0.0}});
                double lunge = curve(t, new double[][] {{0.0, 0.0}, {hit - 1.5, -3.0}, {hit, 15.0}, {hit + 2.0, 13.0}, {end, 0.0}});
                double jaw = t < hit ? smooth(t / (hit - 1.5)) : curve(t, new double[][] {{hit, 0.0}, {hit + 2.0, 0.1}, {end, 0.0}});
                double pitch = curve(t, new double[][] {{0.0, 0.0}, {hit - 1.5, -0.1}, {hit, 0.34}, {end, 0.0}});
                double lift = curve(t, new double[][] {{0.0, 0.0}, {hit, -3.0}, {end, 0.0}});
                double[] brace = {-58.0, 4.0, -34.0};
                double w = 0.55 * inOut(t, 2.0, end, 4.0);
                double ahead = curve(t, new double[][] {{0.0, 0.0}, {hit - 1.5, 2.0}, {hit, -7.0}, {hit + 2.0, -6.0}, {end, 0.0}});
                return new Act(brace, mirror(brace), w, w, pitch, 0.0, 0.0, lift, ahead, bow, 0.0, lunge, jaw, 0.0, Kagune.REST, 0.0);
            }
        }
    }

    /**
     * J tapped: coiled to its right, then the whole of it whirled round to its left as the server's blades sweep ({@link
     * OwlRules#whirlTurn}), both arms flung out wide, its jaws open.
     */
    static Act whirl(double t) {
        double w = inOut(t, OwlRules.WHIRL_WIND, OwlRules.WHIRL_TICKS, OwlRules.WHIRL_TICKS - OwlRules.WHIRL_END);
        double[] out = {-62.0, 30.0, -48.0};
        double coil = curve(t, new double[][] {{0.0, 0.0}, {OwlRules.WHIRL_WIND, 0.3}, {OwlRules.WHIRL_START + 2.0, -0.15}, {OwlRules.WHIRL_END, -0.15},
            {OwlRules.WHIRL_TICKS, 0.0}});
        return new Act(out, mirror(out), w, w, 0.05 * w, coil, 0.0, 3.0 * w, 0.0, 0.0, 0.0, 0.0, 0.4 * w, 0.0, Kagune.FOLLOW, 0.0, 0.0,
            -OwlRules.whirlTurn(t));
    }

    /**
     * Its Pounce: crouched low on its arms, then in the air its arms reaching out ahead, its jaws gaping; landed, both hands slammed down
     * ahead of it (squashed low under the blow), and up again.
     */
    static Act pounce(double t, float landed) {
        if (t < OwlRules.LEAP_CROUCH) {
            double u = smooth(t / OwlRules.LEAP_CROUCH);
            return body(0.25 * u, 0.0, -6.0 * u, 0.0, 0.0, 0.0, 0.3 * u, 0.0, Kagune.FOLLOW, 0.0);
        }
        double[] reach = {-34.0, 34.0, -42.0};
        double u = smooth((t - OwlRules.LEAP_CROUCH) / 3.0);
        if (landed <= 0.0F || t < landed) {
            return new Act(reach, mirror(reach), u, u, -0.2 * u, 0.0, 0.0, 0.0, 0.0, -0.1 * u, 0.0, 0.0, 0.85 * u, 0.0, Kagune.FOLLOW, 0.0);
        }
        // Landed: out of the air's reach both hands slammed down ahead of it, the weight of it coming down behind them.
        double s = t - landed;
        double end = OwlRules.LEAP_RECOVER;
        double ready = smooth((landed - OwlRules.LEAP_CROUCH) / 3.0);
        double[] hand = track(s, new double[][] {{0.0, reach[0], reach[1], reach[2]}, {OwlRules.LEAP_SLAM_AFTER, -22.0, 3.0, -50.0},
            {OwlRules.LEAP_SLAM_AFTER + 2.0, -22.0, 3.0, -50.0}, {end, -40.0, 11.8, -24.0}});
        double w = ready + (1.0 - ready) * smooth(s / 1.5);
        w *= 1.0 - smooth((s - OwlRules.LEAP_SLAM_AFTER - 2.0) / (end - OwlRules.LEAP_SLAM_AFTER - 2.0));
        double hit = curve(s, new double[][] {{0.0, 0.0}, {OwlRules.LEAP_SLAM_AFTER, 1.0}, {end, 0.0}});
        double air = ready * (1.0 - smooth(s / OwlRules.LEAP_SLAM_AFTER));
        return new Act(hand, mirror(hand), w, w, 0.36 * hit - 0.2 * air, 0.0, 0.0, -5.0 * hit, -3.0 * hit, 0.15 * hit - 0.1 * air, 0.0, 0.0,
            Math.max(0.85 * air, 0.6 * (1.0 - hit)), 0.0, Kagune.FOLLOW, 0.0);
    }

    /** V held, its Fissure charging: reared up high on its haunches, both hands raised over it, its jaws roaring, its crest flaring. */
    static Act rear(double t) {
        double u = smooth(t / 6.0);
        double[] up = {-30.0, 98.0, -26.0};
        double shake = 0.02 * Math.sin(t * 1.7) * u;
        return new Act(up, mirror(up), u, u, -0.36 * u + shake, 0.0, 0.0, 8.0 * u, 0.0, -0.15 * u, 0.0, 0.0, 0.7 * u, 0.6 * u, Kagune.FOLLOW, 0.0);
    }

    /** Let go: both hands brought down together on the ground ahead, its weight behind them; the ground splits from there. */
    static Act slam(double t) {
        double hit = OwlRules.FISSURE_IMPACT;
        double end = OwlRules.FISSURE_TICKS;
        double[] hand = track(t, new double[][] {{0.0, -30.0, 98.0, -26.0}, {hit - 2.5, -28.0, 102.0, -24.0}, {hit, -14.0, 2.0, -46.0}, {end, -40.0, 11.8, -24.0}});
        double w = 1.0 - smooth((t - hit - 4.0) / (end - hit - 4.0));
        double pitch = curve(t, new double[][] {{0.0, -0.36}, {hit - 1.5, -0.4}, {hit, 0.44}, {hit + 4.0, 0.4}, {end, 0.0}});
        double lift = curve(t, new double[][] {{0.0, 8.0}, {hit - 1.5, 9.0}, {hit, -6.0}, {end, 0.0}});
        double jaw = curve(t, new double[][] {{0.0, 0.7}, {hit, 0.9}, {end, 0.0}});
        double ahead = curve(t, new double[][] {{0.0, 0.0}, {hit, -4.0}, {hit + 4.0, -4.0}, {end, 0.0}});
        double bow = curve(t, new double[][] {{0.0, -0.15}, {hit, 0.1}, {end, 0.0}});
        double crest = curve(t, new double[][] {{0.0, 0.6}, {hit, 0.5}, {end, 0.0}});
        return new Act(hand, mirror(hand), w, w, pitch, 0.0, 0.0, lift, ahead, bow, 0.0, 0.0, jaw, crest, Kagune.FOLLOW, 0.0);
    }

    /** Z held, its giant fist winding up: the right arm drawn far back, twisted round to its right, its jaws bared. */
    static Act windUp(double t) {
        double u = smooth(t / 8.0);
        double[] back = {-52.0, 34.0, 12.0};
        return new Act(back, null, u, 0.0, -0.06 * u, 0.42 * u, 0.0, 2.0 * u, 0.0, 0.0, 0.0, 0.0, 0.3 * u, 0.2 * u, Kagune.REST, 0.0, 0.8 * u);
    }

    /** Let go: the fist driven out ahead with all of it behind it, held out, then drawn back. */
    static Act fist(double t) {
        double hit = PaleRules.PUNCH_IMPACT;
        double end = PaleRules.PUNCH_TICKS;
        double[] hand = track(t, new double[][] {{0.0, -52.0, 34.0, 12.0}, {hit, -8.0, 30.0, -68.0}, {hit + 4.0, -8.0, 30.0, -66.0}, {end, -40.0, 11.8, -24.0}});
        double w = 1.0 - smooth((t - hit - 4.0) / (end - hit - 4.0));
        double turn = curve(t, new double[][] {{0.0, 0.42}, {hit, -0.26}, {hit + 4.0, -0.22}, {end, 0.0}});
        double pitch = curve(t, new double[][] {{0.0, -0.06}, {hit, 0.26}, {end, 0.0}});
        double lift = curve(t, new double[][] {{0.0, 2.0}, {hit, 0.0}});
        double ahead = curve(t, new double[][] {{0.0, 0.0}, {hit, -4.0}, {hit + 4.0, -4.0}, {end, 0.0}});
        double bow = curve(t, new double[][] {{0.0, 0.0}, {hit, 0.05}, {end, 0.0}});
        double jaw = curve(t, new double[][] {{0.0, 0.3}, {hit, 0.6}, {end, 0.0}});
        double crest = curve(t, new double[][] {{0.0, 0.2}, {end, 0.0}});
        return new Act(hand, null, w, 0.0, pitch, turn, 0.0, lift, ahead, bow, 0.0, 0.0, jaw, crest, Kagune.REST, 0.0, 0.8 * w);
    }

    /**
     * V tapped, its Lunge: hauled forward low and fast on its arms, its jaws gaping; landed on it: its kagune's points driven in, its
     * left claw raked across, its right hand seizing and hauling it in, its jaws snapped on it.
     */
    static Act lunge(double t, int dash) {
        if (t < dash) {
            double u = smooth(t / Math.max(1.0, dash * 0.5));
            double[] reach = {-30.0, 10.0, -52.0};
            return new Act(reach, mirror(reach), 0.6 * u, 0.6 * u, 0.3 * u, 0.0, 0.0, -3.0 * u, -4.0 * u, 0.1 * u, 0.0, 0.0, 0.6 * u, 0.0, Kagune.FOLLOW,
                0.0);
        }
        double s = t - dash;
        double end = OwlRules.KICK_TICKS;
        // (Each hand out of the dash's reach, where it was.)
        double stab = curve(s, new double[][] {{0.0, 0.0}, {OwlRules.KICK_SKEWER, 1.0}, {OwlRules.KICK_SKEWER + 1.5, 1.0}, {OwlRules.KICK_SLASH, 0.0}});
        double[] left = track(s, new double[][] {{0.0, 30.0, 10.0, -52.0}, {OwlRules.KICK_SLASH - 2.0, 64.0, 34.0, -8.0}, {OwlRules.KICK_SLASH, 10.0, 24.0, -54.0},
            {OwlRules.KICK_SLASH + 1.5, -12.0, 20.0, -46.0}, {OwlRules.KICK_SLASH + 4.0, 40.0, 11.8, -24.0}});
        double lw = curve(s, new double[][] {{0.0, 0.6}, {OwlRules.KICK_SLASH - 1.5, 1.0}, {OwlRules.KICK_SLASH + 2.0, 1.0}, {OwlRules.KICK_SLASH + 4.0, 0.0}});
        double[] right = track(s, new double[][] {{0.0, -30.0, 10.0, -52.0}, {OwlRules.KICK_PULL - 2.0, -12.0, 28.0, -60.0}, {OwlRules.KICK_PULL, -10.0, 28.0, -58.0},
            {OwlRules.KICK_PULL + 2.0, -26.0, 32.0, -30.0}, {OwlRules.KICK_BLOW + 3.0, -40.0, 11.8, -24.0}});
        double rw = curve(s, new double[][] {{0.0, 0.6}, {OwlRules.KICK_PULL - 1.0, 1.0}, {OwlRules.KICK_BLOW, 1.0}, {OwlRules.KICK_BLOW + 3.0, 0.0}});
        double blow = OwlRules.KICK_BLOW;
        double bow = curve(s, new double[][] {{0.0, 0.1}, {blow - 2.0, 0.0}, {blow - 0.8, -0.3}, {blow, 0.36}, {blow + 2.0, 0.3}, {end, 0.0}});
        double head = curve(s, new double[][] {{blow - 2.0, 0.0}, {blow, 12.0}, {blow + 2.0, 10.0}, {end, 0.0}});
        double jaw = s < blow ? curve(s, new double[][] {{0.0, 0.6}, {blow - 0.8, 1.0}}) : curve(s, new double[][] {{blow, 0.0}, {end, 0.0}});
        double pitch = curve(s, new double[][] {{0.0, 0.3}, {OwlRules.KICK_SKEWER, 0.22}, {blow, 0.32}, {end, 0.0}});
        double turn = curve(s, new double[][] {{OwlRules.KICK_SLASH - 2.0, 0.0}, {OwlRules.KICK_SLASH - 1.0, -0.25}, {OwlRules.KICK_SLASH + 1.0, 0.25},
            {OwlRules.KICK_PULL + 1.0, 0.0}});
        Kagune k = s < OwlRules.KICK_SLASH ? Kagune.STAB : Kagune.REST;
        double ahead = curve(s, new double[][] {{0.0, -4.0}, {end - 4.0, -3.0}, {end, 0.0}});
        return new Act(right, left, rw, lw, pitch, turn, 0.0, -3.0 * (1.0 - smooth(s / end)), ahead, bow, 0.0, head, jaw, 0.0, k, stab, 0.35 * lw);
    }

    /** B, its Watch This: hunched low, then reared up high as its lances burst out of its back, its arms flung wide, roaring. */
    static Act roar(double t) {
        double up = OwlRules.LANCES_ERUPT_AT;
        double end = OwlRules.LANCES_POSE_TICKS;
        double pitch = curve(t, new double[][] {{0.0, 0.0}, {up, 0.3}, {up + 3.0, -0.32}, {end - 6.0, -0.28}, {end, 0.0}});
        double lift = curve(t, new double[][] {{0.0, 0.0}, {up, -4.0}, {up + 3.0, 9.0}, {end - 6.0, 8.0}, {end, 0.0}});
        double rise = curve(t, new double[][] {{up, 0.0}, {up + 3.0, 1.0}, {end - 6.0, 1.0}, {end, 0.0}});
        double[] wide = {-64.0, 46.0, -6.0};
        return new Act(wide, mirror(wide), rise, rise, pitch, 0.0, 0.0, lift, 0.0, -0.2 * rise, 0.0, 0.0, rise, rise, Kagune.FOLLOW, 0.0);
    }

    /**
     * J held, its spikes: its head lowered, its crest flared, its hands braced wide on the ground and its kagune spread behind it, and
     * the spikes fired off its crest one after another (a jolt through it at each); let go, it rises out of it.
     */
    static Act barrage(double t, boolean held, float letGo) {
        double end = OwlShards.ends(held, letGo);
        double u = inOut(t, OwlShards.FIRST_RIGHT * 0.8, end, OwlShards.RECOVER);
        double kick = 0.0;
        int made = OwlShards.throwsMade(held, letGo);
        for (int k = 0; k < made; k++) {
            double p = t - OwlShards.throwAt(k);
            if (p >= 0.0 && p < 2.0) {
                kick = Math.max(kick, 1.0 - p / 2.0);
            }
        }
        double[] brace = {-56.0, 3.0, -36.0};
        return new Act(brace, mirror(brace), u, u, 0.3 * u - 0.06 * kick, 0.0, 0.0, -4.0 * u + 1.0 * kick, -2.0 * u, 0.72 * u, 0.0, 0.0, 0.3 * u,
            u, Kagune.SPREAD, u);
    }

    /**
     * N, its great hand reaching for what it looks at ({@code aim}: the middle of it; its wrist this side of it, round it), its body
     * leaning after it, its jaws parting; there at {@code ticks}, closing on it.
     */
    static Act reach(double t, double ticks, double[] aim) {
        double[] to = aim == null ? new double[] {-10.0, 30.0, -66.0} : reachable(wrist(aim));
        double u = smooth(t / ticks);
        double[] hand = {-40.0 + (to[0] + 40.0) * u, 11.8 + (to[1] - 11.8) * u, -24.0 + (to[2] + 24.0) * u};
        return new Act(hand, null, 1.0, 0.0, 0.16 * u, 0.0, 0.0, 0.0, -3.0 * u, 0.1 * u, 0.0, 0.0, 0.5 * u, 0.0, Kagune.REST, 0.0,
            -0.3 * smooth(t / (ticks * 0.6)) + 1.3 * smooth((t - ticks + 1.0) / 1.0));
    }

    /**
     * What it has seized held up before its jaws (round it where it is, {@code aim}, as the server carries it there; null: where it
     * holds it), its head bowed to it, its jaws worrying at it.
     */
    static Act hold(double t, double[] aim) {
        double u = smooth(t / 4.0);
        double chew = 0.5 + 0.5 * Math.sin(t * 0.55);
        double[] round = aim == null ? HOLD_WRIST : reachable(wrist(aim));
        double[] hand = {round[0], round[1] + Math.sin(t * 0.55), round[2]};
        return new Act(hand, null, 1.0, 0.0, 0.12, 0.0, 0.0, 0.0, -2.0, 0.22 * u, 0.0, 4.0 * u, chew * u, 0.0, Kagune.REST, 0.0, 1.0);
    }

    /** Let go: the hand raised high with it (out of where it held it, {@code aim}), then slammed down on the ground ahead, its body behind it. */
    static Act slamDown(double t, double[] aim) {
        double hit = OwlRules.GRAB_SLAM_IMPACT;
        double end = OwlRules.GRAB_SLAM_TICKS;
        double[] from = aim == null ? HOLD_WRIST : reachable(wrist(aim));
        double[] hand = track(t, new double[][] {{0.0, from[0], from[1], from[2]}, {hit - 2.0, -18.0, 72.0, -30.0},
            {hit, -8.0, 4.0, -50.0}, {hit + 3.0, -8.0, 4.0, -50.0}, {end, -40.0, 11.8, -24.0}});
        double w = 1.0 - smooth((t - hit - 3.0) / (end - hit - 3.0));
        double pitch = curve(t, new double[][] {{0.0, 0.12}, {hit - 2.0, -0.2}, {hit, 0.38}, {end, 0.0}});
        double lift = curve(t, new double[][] {{0.0, 0.0}, {hit - 2.0, 4.0}, {hit, -4.0}, {end, 0.0}});
        double turn = curve(t, new double[][] {{0.0, 0.0}, {hit - 2.0, 0.2}, {hit, -0.1}, {end, 0.0}});
        double bow = curve(t, new double[][] {{0.0, 0.22}, {hit - 2.0, 0.0}, {hit, 0.15}, {end, 0.0}});
        return new Act(hand, null, w, 0.0, pitch, turn, 0.0, lift, -3.0 * w, bow, 0.0, 0.0, curve(t, new double[][] {{0.0, 0.6}, {hit, 1.0}, {end, 0.0}}),
            0.0, Kagune.REST, 0.0, curve(t, new double[][] {{0.0, 1.0}, {hit, 1.0}, {hit + 1.5, 0.0}}));
    }

    /** Its reach closed on nothing: the hand clutches at the air ahead and comes back. */
    static Act miss(double t, double[] aim) {
        double[] to = aim == null ? new double[] {-10.0, 30.0, -66.0} : reachable(aim);
        double end = OwlRules.GRAB_MISS_TICKS;
        double[] hand = track(t, new double[][] {{0.0, -40.0, 11.8, -24.0}, {4.0, to[0], to[1], to[2]}, {6.0, to[0], to[1] - 2.0, to[2] + 3.0},
            {end, -40.0, 11.8, -24.0}});
        double w = 1.0 - smooth((t - 6.0) / (end - 6.0));
        return new Act(hand, null, w, 0.0, 0.16 * w, 0.0, 0.0, 0.0, -3.0 * w, 0.0, 0.0, 0.0, 0.3 * w, 0.0, Kagune.REST, 0.0,
            curve(t, new double[][] {{0.0, 0.0}, {3.0, -0.3}, {5.0, 1.0}, {end, 0.0}}));
    }

    /** Its wrist round the middle {@code aim} of what it seizes (this side of it, below it: its claws round it). */
    static double[] wrist(double[] aim) {
        return new double[] {aim[0] + HOLD_WRIST[0] - HOLD[0], aim[1] + HOLD_WRIST[1] - HOLD[1], aim[2] + HOLD_WRIST[2] - HOLD[2]};
    }

    /**
     * {@code a} going over into {@code b} ({@code u} 0..1 of the way; either null: the creature as it is between its moves): every
     * turn, lean and reach eased across, a hand only one of them moves eased in or out of its crawl, its whole turn the short way round.
     */
    public static Act blend(Act a, Act b, double u) {
        if (u >= 1.0 || a == null && b == null) {
            return b;
        }
        if (u <= 0.0) {
            return a;
        }
        Act x = a == null ? NONE : a;
        Act y = b == null ? NONE : b;
        double[][] hands = new double[2][];
        double[] weights = new double[2];
        double[][] from = {x.right(), x.left()};
        double[][] to = {y.right(), y.left()};
        double[] wa = {x.rightWeight(), x.leftWeight()};
        double[] wb = {y.rightWeight(), y.leftWeight()};
        for (int i = 0; i < 2; i++) {
            if (from[i] != null && to[i] != null) {
                hands[i] = new double[] {from[i][0] + (to[i][0] - from[i][0]) * u, from[i][1] + (to[i][1] - from[i][1]) * u, from[i][2] + (to[i][2] - from[i][2]) * u};
                weights[i] = wa[i] + (wb[i] - wa[i]) * u;
            } else if (from[i] != null) {
                hands[i] = from[i];
                weights[i] = wa[i] * (1.0 - u);
            } else if (to[i] != null) {
                hands[i] = to[i];
                weights[i] = wb[i] * u;
            }
        }
        double spin = y.spin() - x.spin();
        spin -= Math.PI * 2.0 * Math.round(spin / (Math.PI * 2.0));
        Kagune kagune = b == null ? x.kagune() : y.kagune();
        return new Act(hands[0], hands[1], weights[0], weights[1], mix(x.pitch(), y.pitch(), u), mix(x.turn(), y.turn(), u), mix(x.roll(), y.roll(), u),
            mix(x.lift(), y.lift(), u), mix(x.ahead(), y.ahead(), u), mix(x.bow(), y.bow(), u), mix(x.headTurn(), y.headTurn(), u), mix(x.lunge(), y.lunge(), u),
            mix(x.jaw(), y.jaw(), u), mix(x.crest(), y.crest(), u), kagune, kagune == x.kagune() && kagune == y.kagune() ? mix(x.stab(), y.stab(), u)
                : b == null ? x.stab() * (1.0 - u) : y.stab() * u, mix(x.grip(), y.grip(), u), x.spin() + spin * u);
    }

    /** The creature as it is between its moves, as an act (nothing moved). */
    static final Act NONE = new Act(null, null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Kagune.FOLLOW, 0.0);

    static double mix(double a, double b, double u) {
        return a + (b - a) * u;
    }

    /** {@code aim} brought within the right arm's reach (from its shoulder), and off the ground. */
    static double[] reachable(double[] aim) {
        double[] shoulder = {-18.0, 49.5, -12.0};
        double[] d = {aim[0] - shoulder[0], aim[1] - shoulder[1], aim[2] - shoulder[2]};
        double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        double most = 84.0;
        double k = l > most ? most / l : 1.0;
        return new double[] {shoulder[0] + d[0] * k, Math.max(4.0, shoulder[1] + d[1] * k), shoulder[2] + d[2] * k};
    }
}
