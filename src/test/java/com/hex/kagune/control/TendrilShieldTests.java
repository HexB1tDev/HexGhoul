package com.hex.kagune.control;

import com.hex.kagune.control.ukaku.UkakuRules;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free tests for Auto Projectile's tentacle guard (5.0.14): its rules, its pose, and that Ukaku still beats it. */
public final class TendrilShieldTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-4;
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static void main(String[] args) {
        rules();
        flight();
        pose();
        balance();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Tendril shield tests: " + passed + " checks passed");
    }

    /**
     * 6.0.9: projectile entities are seen early enough to be stopped. Arrows fly as vanilla's (each tick: hit what lies along this
     * tick's move, move, keep 0.99 of the speed, fall 0.05), shot at a player standing at the origin from every side: a skeleton's
     * (1.6 a tick, aimed a fifth of a block high for every block away, with its spread), a drawn bow's (3.0) and a crossbow's
     * (3.15). Every one that would hit him is seen (within the scan, along its own flight) at least the tentacle's swing-in time
     * before it lands, from wherever that can be done at all; closer than that, the guard goes up against the shooter instead.
     */
    private static void flight() {
        java.util.Random random = new java.util.Random(7L);
        double[] centre = {0.0, 0.9, 0.0};
        double[] still = {0.0, 0.0, 0.0};
        double[][] kinds = {{1.6, 8.0, 6.0}, {3.0, 14.0, 1.0}, {3.15, 14.0, 1.0}}; // speed, nearest blockable, spread
        String[] names = {"skeleton", "bow", "crossbow"};
        for (int kind = 0; kind < kinds.length; kind++) {
            int hits = 0;
            int late = 0;
            String example = "";
            for (double distance = kinds[kind][1]; distance <= 40.0; distance += 2.0) {
                for (int trial = 0; trial < 60; trial++) {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double[] p = {Math.cos(angle) * distance, 1.52, Math.sin(angle) * distance};
                    double dx = -p[0];
                    double dz = -p[2];
                    double flat = Math.sqrt(dx * dx + dz * dz);
                    double dy = kind == 0 ? 0.6 - p[1] + flat * 0.2 : 1.2 - p[1] + flat * (0.012 + 0.0016 * flat);
                    double[] v = shot(dx, dy, dz, kinds[kind][0], kinds[kind][2], random);
                    int seen = -1;
                    for (int tick = 0; tick < 200; tick++) {
                        double[] next = {p[0] + v[0], p[1] + v[1], p[2] + v[2]};
                        if (hitsPlayer(p, next)) {
                            hits++;
                            if (seen < 0 || tick - seen < TendrilShield.RAISE_TICKS) {
                                late++;
                                example = names[kind] + " from " + distance + " (seen at " + seen + ", landed at " + tick + ")";
                            }
                            break;
                        }
                        p = next;
                        v = new double[] {v[0] * TendrilShield.AIR_DRAG, v[1] * TendrilShield.AIR_DRAG - TendrilShield.ARROW_GRAVITY, v[2] * TendrilShield.AIR_DRAG};
                        if (p[1] < -4.0) {
                            break;
                        }
                        boolean inScan = Math.abs(p[0]) <= TendrilShield.FLIGHT_SCAN && Math.abs(p[2]) <= TendrilShield.FLIGHT_SCAN;
                        if (seen < 0 && inScan && TendrilShield.incoming(p, v, TendrilShield.ARROW_GRAVITY, TendrilShield.AIR_DRAG, centre, still,
                            TendrilShield.FLIGHT_LOOKAHEAD, TendrilShield.INTERCEPT)) {
                            seen = tick;
                        }
                    }
                }
            }
            check(hits > 100 && late == 0, names[kind] + " arrows that would hit are seen in time to be stopped (" + late + " of " + hits
                + " late" + (example.isEmpty() ? "" : ", e.g. a " + example) + ")");
        }
        // What isn't coming at him isn't guarded against: wide of him, flying away, or falling short.
        check(!TendrilShield.incoming(new double[] {3.0, 1.5, 20.0}, new double[] {0.0, 0.25, -3.0}, TendrilShield.ARROW_GRAVITY, TendrilShield.AIR_DRAG, centre,
            still, TendrilShield.FLIGHT_LOOKAHEAD, TendrilShield.INTERCEPT), "an arrow passing three blocks wide is not incoming");
        check(!TendrilShield.incoming(new double[] {0.0, 1.5, 6.0}, new double[] {0.0, 0.1, 3.0}, TendrilShield.ARROW_GRAVITY, TendrilShield.AIR_DRAG, centre,
            still, TendrilShield.FLIGHT_LOOKAHEAD, TendrilShield.INTERCEPT), "an arrow flying away is not");
        check(!TendrilShield.incoming(new double[] {0.0, 1.5, 30.0}, new double[] {0.0, 0.0, -0.6}, TendrilShield.ARROW_GRAVITY, TendrilShield.AIR_DRAG, centre,
            still, TendrilShield.FLIGHT_LOOKAHEAD, TendrilShield.INTERCEPT), "a weak arrow that falls short is not");
        check(TendrilShield.incoming(new double[] {0.0, 1.2, 20.0}, new double[] {0.0, 0.0, -1.0}, 0.0, 1.0, centre, still,
            TendrilShield.FLIGHT_LOOKAHEAD, TendrilShield.INTERCEPT), "a fireball flies straight at him: incoming");
        check(TendrilShield.incoming(new double[] {0.0, 1.2, 12.0}, new double[] {0.0, 0.0, -1.0}, 0.0, 1.0, centre, new double[] {0.0, 0.0, 0.2},
            TendrilShield.FLIGHT_LOOKAHEAD, TendrilShield.INTERCEPT), "a shot meets him as he runs into it");
        // A shooter about to loose at him: aimed within fifteen degrees and in range.
        double[] eye = {0.0, 1.62, 20.0};
        double[] at = {0.0, -0.035, -1.0};
        check(TendrilShield.aimedAt(eye, at, centre), "a player drawing straight at him aims at him");
        check(TendrilShield.aimedAt(eye, new double[] {Math.sin(Math.toRadians(10.0)), 0.0, -Math.cos(Math.toRadians(10.0))}, centre),
            "ten degrees off is still at him");
        check(!TendrilShield.aimedAt(eye, new double[] {Math.sin(Math.toRadians(25.0)), 0.0, -Math.cos(Math.toRadians(25.0))}, centre),
            "twenty-five degrees off is not");
        check(!TendrilShield.aimedAt(new double[] {0.0, 1.62, 60.0}, at, centre), "beyond range is not");
    }

    /** A shot along (dx, dy, dz) at {@code speed}, with vanilla's spread (0.0075 a unit of inaccuracy, per axis). */
    private static double[] shot(double dx, double dy, double dz, double speed, double inaccuracy, java.util.Random random) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double x = dx / length + random.nextGaussian() * 0.0075 * inaccuracy;
        double y = dy / length + random.nextGaussian() * 0.0075 * inaccuracy;
        double z = dz / length + random.nextGaussian() * 0.0075 * inaccuracy;
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[] {x / l * speed, y / l * speed, z / l * speed};
    }

    /** True when the move from {@code a} to {@code b} crosses a standing player's box (0.6 wide, 1.8 tall, at the origin). */
    private static boolean hitsPlayer(double[] a, double[] b) {
        double[] lo = {-0.3, 0.0, -0.3};
        double[] hi = {0.3, 1.8, 0.3};
        double t0 = 0.0;
        double t1 = 1.0;
        for (int k = 0; k < 3; k++) {
            double d = b[k] - a[k];
            if (Math.abs(d) < 1.0E-12) {
                if (a[k] < lo[k] || a[k] > hi[k]) {
                    return false;
                }
                continue;
            }
            double u0 = (lo[k] - a[k]) / d;
            double u1 = (hi[k] - a[k]) / d;
            t0 = Math.max(t0, Math.min(u0, u1));
            t1 = Math.min(t1, Math.max(u0, u1));
            if (t0 > t1) {
                return false;
            }
        }
        return true;
    }

    private static void rules() {
        check(near(TendrilShield.cost(0.0F, true, false), 2.0) && near(TendrilShield.cost(0.0F, true, true), 1.36),
            "an arrow costs what the original's deflection did (2, 1.36 with the Reflex Organ)");
        check(near(TendrilShield.cost(4.0F, false, false), 1.0) && near(TendrilShield.cost(0.1F, false, false), 0.15),
            "Ukaku's shots cost by what the guard took, never nothing");
        check(near(TendrilShield.guardHealth(0, false, 1.0), 6.0) && near(TendrilShield.guardHealth(1000, false, 1.0), 12.0)
            && near(TendrilShield.guardHealth(1000, true, 1.0), 18.0) && TendrilShield.guardHealth(1000, false, 0.0) == 0.0F
            && near(TendrilShield.guardHealth(500, false, 2.0), 18.0), "guard health by mastery, organ and the server's strength");
        float[] held = TendrilShield.absorb(4.5F, 12.0F);
        float[] broke = TendrilShield.absorb(4.5F, 2.0F);
        check(held[0] == 4.5F && held[1] == 0.0F && broke[0] == 2.0F && broke[1] == 2.5F, "a guard takes what it can, the rest goes through");
        check(!TendrilShield.inPlace(100L, 103L) && TendrilShield.inPlace(100L, 104L), "nothing is stopped while the tentacle swings in");
        double[] centre = {0.0, 1.0, 0.0};
        check(TendrilShield.incoming(new double[] {0.0, 1.2, 10.0}, new double[] {0.0, 0.0, -2.0}, centre, 10.0, 1.35),
            "a shot flying at him is incoming");
        check(!TendrilShield.incoming(new double[] {2.0, 1.2, 10.0}, new double[] {0.0, 0.0, -2.0}, centre, 10.0, 1.35),
            "a shot passing two blocks wide is not");
        check(!TendrilShield.incoming(new double[] {0.0, 1.2, 10.0}, new double[] {0.0, 0.0, 2.0}, centre, 10.0, 1.35),
            "a shot flying away is not");
        check(!TendrilShield.incoming(new double[] {0.0, 1.2, 40.0}, new double[] {0.0, 0.0, -2.0}, centre, 10.0, 1.35),
            "a shot twenty ticks away is not yet");
        double[] from = TendrilShield.from(new double[] {0.0, 1.0, 5.0}, new double[] {0.0, 0.0, -2.0}, centre);
        check(near(from[0], 0.0) && near(from[1], 0.0) && near(from[2], 1.0), "the shots come from back along their flight");
        double[] ahead = TendrilShield.direction(0.0F, 0.0F);
        check(TendrilShield.covers(ahead, TendrilShield.direction(40.0F, 0.0F)) && !TendrilShield.covers(ahead, TendrilShield.direction(60.0F, 0.0F)),
            "one guard covers fifty degrees round its own direction");
        boolean round = true;
        for (float yaw = -180.0F; yaw < 180.0F; yaw += 17.0F) {
            for (float pitch = -80.0F; pitch <= 80.0F; pitch += 20.0F) {
                double[] d = TendrilShield.direction(yaw, pitch);
                double[] back = TendrilShield.direction(TendrilShield.yaw(d), TendrilShield.pitch(d));
                round &= near(d[0], back[0]) && near(d[1], back[1]) && near(d[2], back[2]) && near(dot(d, d), 1.0);
            }
        }
        check(round, "a direction survives the packet's yaw and pitch");
        double[] stop = TendrilShield.stopPoint(new double[] {0.0, 1.0, 6.0}, new double[] {0.0, 0.0, -2.0}, new double[] {0.0, 1.0, 0.3},
            centre, new double[] {0.0, 0.0, 1.0});
        check(near(stop[2], TendrilShield.DISTANCE), "the shot stops on the guard, in front of him");
        check(TendrilShield.amount(0.0, Double.NaN) == 0.0F && TendrilShield.amount(TendrilShield.RAISE_TICKS, Double.NaN) == 1.0F
            && TendrilShield.amount(20.0, TendrilShield.LOWER_TICKS) == 0.0F && TendrilShield.amount(20.0, 1.0) > 0.0F,
            "it swings in, holds, and folds back");
    }

    private static void pose() {
        double[] centre = {0.0, 1.0, 0.0};
        double[] forward = {0.0, 0.0, 1.0};
        int n = 24;
        for (int side = -1; side <= 1; side += 2) {
            // A tentacle root on his back, to one side (x is his left with this forward).
            double[] root = {0.15 * side, 1.35, -0.2};
            for (int k = 0; k < 16; k++) {
                double angle = Math.PI * 2.0 * k / 16.0;
                double[] dir = {Math.sin(angle), 0.0, Math.cos(angle)};
                double[][] p = TendrilShield.pose(root, centre, dir, forward, n);
                check(p.length == n && near(p[0][0], root[0]) && near(p[0][1], root[1]) && near(p[0][2], root[2]), "the guard grows from the root");
                double[] across = {-dir[2], 0.0, dir[0]};
                int first = (int)Math.ceil(TendrilShield.SPLIT * (n - 1));
                for (int i = first; i < n; i++) {
                    double[] r = {p[i][0] - centre[0], 0.0, p[i][2] - centre[2]};
                    check(dot(r, dir) >= 0.45, "the guard lies between him and the shots (" + k + ", point " + i + ")");
                    check(Math.hypot(r[0], r[2]) >= 0.45, "the guard stays out of his body (" + k + ")");
                }
                double span = Math.abs(dot(new double[] {p[n - 1][0] - p[first][0], 0.0, p[n - 1][2] - p[first][2]}, across));
                check(span >= 1.8 * TendrilShield.HALF_WIDTH, "the guard spans across the line the shots come in on (" + span + ")");
            }
            double[][] rain = TendrilShield.pose(root, centre, new double[] {0.0, 1.0, 0.0}, forward, n);
            check(rain[n - 1][1] >= centre[1] + 0.9 && rain[n / 2][1] >= centre[1] + 0.9, "against rain the guard goes over his head");
        }
        double[][] a = {{0, 0, 0}, {1, 1, 1}};
        double[][] b = {{2, 2, 2}, {3, 3, 3}};
        check(near(TendrilShield.blend(a, b, 0.5)[1][0], 2.0) && TendrilShield.blend(a, b, 0.0)[1][0] == 1.0, "the guard eases in from the usual path");
    }

    /**
     * The user's rule: a good defence against Ukaku, never one that makes him useless. Each of his answers is checked with his
     * own numbers at the same mastery, so changing either side's balance re-checks this.
     */
    private static void balance() {
        for (int mastery : new int[] {250, 500, 750, 1000}) {
            float guard = TendrilShield.guardHealth(mastery, false, 1.0);
            float organGuard = TendrilShield.guardHealth(mastery, true, 1.0);
            float shot = UkakuRules.meleeDamage(mastery) * ComboStyle.UKAKU_FIRE.stage(0).damageMul();
            // A guard holds a few of his ordinary shots, not a whole chain.
            int shots = (int)Math.ceil(guard / shot);
            check(shots >= 2 && shots <= 4, "at " + mastery + " a guard breaks after " + shots + " fire shots (2 to 4)");
            boolean organ = mastery >= 750; // the Projectile Reflex Organ's tier
            check(!organ || Math.ceil(organGuard / shot) <= 6, "at " + mastery + " even the Reflex Organ's guard breaks within six shots");
            // The Gatling (from its mastery) at full spin breaks a guard in about a second: its stream is the answer to a guard.
            double perTick = UkakuRules.GATLING_FULL_RATE * UkakuRules.gatlingDamage(mastery);
            check(mastery < UkakuRules.GATLING_MASTERY || guard / perTick <= 24.0,
                "at " + mastery + " the Gatling breaks a guard in " + guard / perTick + " ticks (about a second)");
            check(!organ || organGuard / perTick <= 30.0, "at " + mastery + " the Gatling breaks the Reflex Organ's guard within 1.5 s");
            // A broken tentacle stays aside longer than it took to break it.
            check(TendrilShield.BREAK_TICKS > UkakuRules.flameCadence(mastery) * (shots - 1),
                "at " + mastery + " a broken tentacle stays aside longer than breaking it took");
            // Close up, his spikes and needles beat a guard that has to come up (it swings in for RAISE_TICKS).
            double spikeReach = UkakuRules.crystalSpeed(mastery) * TendrilShield.RAISE_TICKS;
            double needleReach = UkakuRules.needleSpeed(mastery) * TendrilShield.RAISE_TICKS;
            check(spikeReach >= 10.0 && needleReach >= 16.0, "at " + mastery + " spikes from within " + spikeReach + " and needles from within "
                + needleReach + " blocks land before a fresh guard is up");
        }
        // Spikes a guard stops stay in the tentacle: with Bursting Shards each comes back as a blast beside him, more than a third of
        // the hit it would have been (and Crystal Bloom sets them all off at once).
        for (int mastery : new int[] {250, 500, 750, 1000}) {
            for (float hit : new float[] {UkakuRules.gatlingDamage(mastery), UkakuRules.meleeDamage(mastery) * ComboStyle.UKAKU_CRYSTAL.stage(0).damageMul()}) {
                float blast = UkakuRules.burstDamage(hit) * UkakuRules.BURST_EXPLOSION_SHARE;
                check(blast >= hit / 3.0F, "at " + mastery + " a guarded " + hit + " spike comes back as a " + blast + " blast with Bursting Shards");
            }
        }
        check(TendrilShield.MAX_GUARDS == 2, "two guards at once: shots from a third side get through");
        check(TendrilShield.COVER_DEGREES <= 60.0, "one guard can't cover a whole flank");
    }
}
