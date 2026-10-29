package com.hex.kagune.control.king;

/**
 * 1.0.6: War's six abilities (pure, tested): their keys, cooldowns and numbers. 1.0.13: all six made again.
 * <ul>
 *   <li>Z, King's Execution: the free hand thrown forward, and five black centipedes pour out of the arm, as far as fifteen blocks,
 *       seize what is there, coil round it, squeeze, and crush it.</li>
 *   <li>J, Black Reaper: a dash ten blocks where he looks, and at its end a cleave of the bladed arm with all his weight in it: it
 *       bleeds what it cuts, and what it kills it cuts in half.</li>
 *   <li>G, Centipede Dominion: he becomes a small Dragon for seven seconds: it goes on forward on its own, where he aims, biting and
 *       crushing what it meets and ploughing through the ground.</li>
 *   <li>B, Spire Wave: the bladed arm driven into the ground, and a wave of black centipede spikes bursts up in a line twenty blocks
 *       ahead: what it meets is impaled, thrown up and pinned (and ready for Z).</li>
 *   <li>C, King's Defiance: the bladed arm raised; a blow within a moment of it is countered from behind the striker (cut, stunned,
 *       bled); if none comes, the rinkaku cocoon him, take everything that strikes him, and burst outward with all of it.</li>
 *   <li>V, Black Sun (1.0.15): the ground cracks open where he looks and centipedes burst up out of it, seize what is round that
 *       spot, lift it high into the sky and slam it back down (killing what is left weak); darkness falls on everything near.</li>
 * </ul>
 * 1.0.7: the final form is meant to be broken: every number went up, and the six rinkaku hunt on their own (the instinct): each
 * lashes the hostile bodies near him and fires spikes of RC at the ones farther off, and whatever strikes him becomes their prey.
 */
public final class WarRules {
    public enum Ability {
        EXECUTION("King's Execution", "Z", EXECUTION_COOLDOWN),
        REAPER("Black Reaper", "J", 300),
        DOMINION("Centipede Dominion", "G", 800),
        SPIRE("Spire Wave", "B", SPIRE_COOLDOWN),
        DEFIANCE("King's Defiance", "C", 320),
        SUN("Black Sun", "V", 1800);

        private final String displayName;
        private final String key;
        private final int cooldown;

        Ability(String displayName, String key, int cooldown) {
            this.displayName = displayName;
            this.key = key;
            this.cooldown = cooldown;
        }

        public String displayName() {
            return this.displayName;
        }

        /** The key it is on by default (the kagune's ability keys, free while War has the body). */
        public String key() {
            return this.key;
        }

        /** Ticks before it can be used again. */
        public int cooldown() {
            return this.cooldown;
        }

        public static Ability bySlot(int slot) {
            Ability[] all = values();
            return slot >= 0 && slot < all.length ? all[slot] : null;
        }
    }

    // ================================================================== 1.0.27: the warpath (the rework)

    // RMB: the spike cone
    /** His swipe (ticks), and the tick his hand lands on (the first row of spikes breaks the ground then, a row a tick after). */
    public static final int CONE_TICKS = 10;
    public static final int CONE_SWIPE = 3;
    /** Rows of spikes, how far the first stands from him, how far apart they are (blocks), how wide (half) the first, how much wider each. */
    public static final int CONE_ROWS = 13;
    public static final double CONE_FIRST = 1.4;
    public static final double CONE_STEP = 1.0;
    public static final double CONE_HALF = 0.35;
    public static final double CONE_WIDEN = 0.4;
    /** What a spike does to what stands over it; how long the spikes stand; the wait before the next swipe. */
    public static final float CONE_DAMAGE = 9.0F;
    public static final int CONE_BLEED = 60;
    public static final int CONE_SLOW = 40;
    public static final double CONE_LIFT = 0.35;
    public static final int CONE_STAND = 16;
    public static final int CONE_COOLDOWN = 36;

    /** Row {@code row}'s distance out (blocks), half its width, and how many spikes it has. */
    public static double coneAlong(int row) {
        return CONE_FIRST + CONE_STEP * row;
    }

    public static double coneHalf(int row) {
        return CONE_HALF + CONE_WIDEN * row;
    }

    public static int coneSpikes(int row) {
        return 1 + (int)Math.floor(coneHalf(row) * 2.0);
    }

    /** Spike {@code k} of row {@code row} across it (blocks, + his right), {@code jitter} (-1..1) shifting it a little in its place. */
    public static double coneAcross(int row, int k, double jitter) {
        int n = coneSpikes(row);
        double half = coneHalf(row);
        double slot = 2.0 * half / n;
        return n == 1 ? jitter * 0.25 : -half + slot * (k + 0.5) + jitter * 0.3 * slot;
    }

    // Z: King's Execution, at nothing near his aim: the dash, both hands raised, the ground's spikes round him
    /** His hands up (ticks); the spikes break the ground this long after the dash; how far round him; what they do. */
    public static final int RISE_TICKS = 18;
    public static final int RISE_SPIKES = 3;
    public static final double RISE_RADIUS = 5.0;
    public static final float RISE_DAMAGE = 8.0F;
    public static final int RISE_SLOW = 60;
    public static final int RISE_BLEED = 100;

    // Z: King's Execution at a body within reach of his aim: the blitz
    /** How far off it may be (blocks). */
    public static final double BLITZ_RANGE = 7.0;
    /**
     * Its moments (ticks): there; 1.2.8: the jabs on the ground (from, every, how many); the punch that sends it up; he blinks up after
     * it; the cut in the air; the kick; 1.2.8: the flurry in the air after it (four seconds of blows from every side, from, every, how
     * long); the wind-up of the last (slowed); the blow to the gut; when it is all over at the latest.
     */
    public static final int BLITZ_ARRIVE = 2;
    public static final int BLITZ_JABS_FROM = 3;
    public static final int BLITZ_JAB_EVERY = 2;
    public static final int BLITZ_JABS = 4;
    public static final int BLITZ_PUNCH = 11;
    public static final int BLITZ_BLINK = 13;
    public static final int BLITZ_CUT = 15;
    public static final int BLITZ_KICK = 19;
    public static final int BLITZ_FLURRY = 22;
    public static final int BLITZ_FLURRY_EVERY = 3;
    public static final int BLITZ_FLURRY_TICKS = 81;
    public static final int BLITZ_WIND = BLITZ_FLURRY + BLITZ_FLURRY_TICKS;
    public static final int BLITZ_GUT = BLITZ_WIND + 14;
    public static final int BLITZ_TICKS = BLITZ_GUT + 21;
    /**
     * What each blow does (1.2.8: the many small, the last the most of it by far: the gut and the crash are most of the whole); how fast
     * it is driven down; the crater's size round where it lands, what that does, how far the ground shakes.
     */
    public static final float BLITZ_JAB_DAMAGE = 1.0F;
    public static final float BLITZ_PUNCH_DAMAGE = 3.0F;
    public static final float BLITZ_CUT_DAMAGE = 3.0F;
    public static final float BLITZ_KICK_DAMAGE = 2.0F;
    public static final float BLITZ_FLURRY_DAMAGE = 0.5F;
    public static final float BLITZ_GUT_DAMAGE = 30.0F;
    public static final float BLITZ_CRASH_DAMAGE = 10.0F;
    public static final float CRATER_DAMAGE = 10.0F;
    public static final double BLITZ_CRASH_SPEED = 3.2;
    public static final double CRATER_RADIUS = 4.5;
    public static final double SHAKE_RADIUS = 25.0;
    public static final int EXECUTION_COOLDOWN = 300;

    /**
     * Where the blitz has the body {@code t} ticks in (blocks from where it stood, his frame: x his right, y up, z away from him): lifted
     * by the punch, held for the cut, kicked higher, held (barely drifting) through the slowed wind-up. From {@link #BLITZ_GUT} on it is
     * driven down (the server's).
     */
    public static double[] blitzBody(double t) {
        if (t < BLITZ_PUNCH) {
            return new double[] {0.0, 0.0, 0.0};
        }
        if (t < BLITZ_CUT) {
            double u = ease((t - BLITZ_PUNCH) / (BLITZ_CUT - BLITZ_PUNCH));
            return new double[] {0.0, 3.2 * u, 1.6 * u};
        }
        if (t < BLITZ_KICK) {
            double u = (t - BLITZ_CUT) / (BLITZ_KICK - BLITZ_CUT);
            return new double[] {0.0, 3.2 + 0.3 * u, 1.6 + 0.2 * u};
        }
        if (t < BLITZ_FLURRY) {
            double u = ease((t - BLITZ_KICK) / (BLITZ_FLURRY - BLITZ_KICK));
            return new double[] {0.6 * u, 3.5 + 1.1 * u, 1.8 + 1.2 * u};
        }
        if (t < BLITZ_WIND) {
            // 1.2.8: held up there through the flurry, rocked a little by every blow, away from the side it came from.
            double k = (t - BLITZ_FLURRY) / BLITZ_FLURRY_EVERY;
            int n = (int)Math.floor(k);
            double rock = 0.1 * Math.exp(-3.0 * (k - n)) * Math.min(1.0, (k - n) * 6.0);
            double[] from = flurrySide(n);
            double l = Math.max(1.0E-6, Math.sqrt(from[0] * from[0] + from[1] * from[1] + from[2] * from[2]));
            double rise = 0.08 * (t - BLITZ_FLURRY) / BLITZ_FLURRY_TICKS;
            return new double[] {0.6 - from[0] / l * rock, 4.6 + rise - from[1] / l * rock * 0.5, 3.0 - from[2] / l * rock};
        }
        double u = Math.min(1.0, (t - BLITZ_WIND) / (BLITZ_GUT - BLITZ_WIND));
        return new double[] {0.6, 4.68 + 0.12 * u, 3.0 + 0.1 * u};
    }

    /**
     * 1.2.8: which side the flurry's {@code n}th blow comes from (blocks from the body, his frame: x his right, y up, z away from him):
     * round its front from his far left to his far right, from under it and over it, never twice alike in a row.
     */
    public static double[] flurrySide(int n) {
        double a = Math.toRadians(75.0 * Math.sin(n * 2.4));
        double y = -0.7 + 1.2 * (n * 0.6180339887 - Math.floor(n * 0.6180339887));
        double r = 1.45;
        return new double[] {r * Math.sin(a), y, -r * Math.cos(a)};
    }

    /** 1.2.8: the flurry's blow at {@code age}, or -1: one every {@link #BLITZ_FLURRY_EVERY} ticks from {@link #BLITZ_FLURRY} on. */
    public static int flurryHit(long age) {
        if (age < BLITZ_FLURRY || age >= BLITZ_WIND || (age - BLITZ_FLURRY) % BLITZ_FLURRY_EVERY != 0) {
            return -1;
        }
        return (int)((age - BLITZ_FLURRY) / BLITZ_FLURRY_EVERY);
    }

    /** 1.2.8: the jab at {@code age} on the ground, or -1. */
    public static int jab(long age) {
        if (age < BLITZ_JABS_FROM || (age - BLITZ_JABS_FROM) % BLITZ_JAB_EVERY != 0 || (age - BLITZ_JABS_FROM) / BLITZ_JAB_EVERY >= BLITZ_JABS) {
            return -1;
        }
        return (int)((age - BLITZ_JABS_FROM) / BLITZ_JAB_EVERY);
    }

    /** 1.0.28: the highest the blitz has the body (blocks; {@link #blitzBody}'s height at its top), for the room it needs over it. */
    public static final double BLITZ_PEAK = 4.8;

    /** Where he is against the body (blocks, the same frame) once he has blinked up to it: just before it, facing it. */
    public static double[] blitzBeside(double t) {
        if (t >= BLITZ_FLURRY - 1 && t < BLITZ_WIND - 1) {
            // 1.2.8: the flurry: blinked to its own side a tick before each blow.
            double[] side = flurrySide((int)Math.floor((t - (BLITZ_FLURRY - 1)) / BLITZ_FLURRY_EVERY));
            return new double[] {side[0], side[1] - 0.25, side[2]};
        }
        return t < BLITZ_KICK ? new double[] {-0.2, -0.4, -1.4} : new double[] {-0.4, -0.25, -1.5};
    }

    /** 1.2.8: whether he blinks to the next side of the flurry at {@code age} (a tick before each of its blows). */
    public static boolean flurryBlink(long age) {
        return age >= BLITZ_FLURRY - 1 && age < BLITZ_WIND - 1 && (age - (BLITZ_FLURRY - 1)) % BLITZ_FLURRY_EVERY == 0;
    }

    // V: Black Sun (1.0.27: the ultimate, at a body within reach)
    /** How far off it may be (blocks). */
    public static final double SUNDER_RANGE = 5.0;
    /**
     * Its moments (ticks): there; the uppercut; the body at its height; he leaps after it, beside it; the punch (1.0.33: drawn back
     * near twice as long); it hangs on his fist, feeling it, till it is sent flying; the latest it ends.
     */
    public static final int SUNDER_ARRIVE = 2;
    public static final int SUNDER_UPPER = 3;
    public static final int SUNDER_APEX = 13;
    public static final int SUNDER_LEAP = 5;
    public static final int SUNDER_BESIDE = 14;
    public static final int SUNDER_PUNCH = 50;
    public static final int SUNDER_FELT = 14;
    public static final int SUNDER_LAUNCH = SUNDER_PUNCH + SUNDER_FELT;
    public static final int SUNDER_TICKS = 180;
    /** 1.0.33: how far the fist drives the body in while it feels it (blocks), and how hard it shudders there. */
    public static final double SUNDER_SINK = 0.7;
    public static final double SUNDER_SHUDDER = 0.12;
    /** How high it is sent up (blocks); what the uppercut, the punch and its landing do. */
    public static final double SUNDER_UP = 14.0;
    public static final float SUNDER_UPPER_DAMAGE = 14.0F;
    public static final float SUNDER_PUNCH_DAMAGE = 42.0F;
    public static final float SUNDER_CRASH_DAMAGE = 16.0F;
    /** The fall: how steeply it comes down (radians below level) and how fast (blocks a tick); the skid's start and its slowing. */
    // 1.0.36: a long line down and forward (the user's call: as a body punched from the sky goes), never straight down into the ground:
    // it slams into the ground far ahead and ploughs on along it through whatever stands there.
    public static final double SUNDER_DIVE = Math.toRadians(22.0);
    public static final double SUNDER_FLY = 2.4;
    public static final double SUNDER_SKID = 2.0;
    public static final double SUNDER_DRAG = 0.9;
    /** 1.0.33: how long he hangs in the air after it is sent flying (ticks), then falls on his own, no fall hurting him. */
    public static final int SUNDER_HANG = 50;
    /** The ring of air the punch throws out (1.0.33: standing up across the sky, square to the punch): how far, how long it takes; how far the ground shakes. */
    public static final double RING_RADIUS = 30.0;
    public static final int RING_TICKS = 24;
    public static final double SUNDER_SHAKE = 40.0;

    /** Where Black Sun has the body {@code t} ticks in (blocks above where the uppercut found it): sent up, slowing to its height, held. */
    public static double sunderRise(double t) {
        if (t < SUNDER_UPPER) {
            return 0.0;
        }
        return SUNDER_UP * ease(Math.min(1.0, (t - SUNDER_UPPER) / (SUNDER_APEX - SUNDER_UPPER)));
    }

    /** How far he has leapt up after it ({@code t} ticks in), 0..1 of the way to beside it. */
    public static double sunderLeap(double t) {
        if (t < SUNDER_LEAP) {
            return 0.0;
        }
        double u = Math.min(1.0, (t - SUNDER_LEAP) / (SUNDER_BESIDE - SUNDER_LEAP));
        return u * u * (3.0 - 2.0 * u);
    }

    // ================================================================== 1.0.28: the held moves (Black Reaper, King's Defiance, Spire Wave)

    // J: Black Reaper: centipedes out of his back at what he aims at, dragging it in while the key is held; close, the blade through it
    /** How far off it may be (blocks); the centipedes' flight out to it and back (ticks); the longest they hold it; how close is close. */
    public static final double REAPER_RANGE = 18.0;
    public static final int REAPER_REACH = 6;
    public static final int REAPER_RETRACT = 9;
    public static final int REAPER_HOLD = 120;
    public static final double REAPER_CLOSE = 2.0;
    /** How fast it is dragged (blocks a tick, at most, and that share of the way left each tick); how long the blade holds it through. */
    public static final double REAPER_PULL = 0.75;
    public static final double REAPER_PULL_SHARE = 0.35;
    public static final int REAPER_IMPALE = 14;
    /** What the bite does as they latch on, and the blade through it: damage, bleeding, slowing, the stun. */
    public static final float REAPER_BITE = 4.0F;
    public static final float REAPER_IMPALE_DAMAGE = 20.0F;
    public static final int REAPER_BLEED = 140;
    public static final int REAPER_SLOW = 100;
    public static final int REAPER_STUN = 40;
    /** The wait after: a whiff's, and a catch's. */
    public static final int REAPER_WHIFF_COOLDOWN = 60;
    /** The centipedes out of his back. */
    public static final int REAPER_ARMS = 3;
    /** The phases (the move's level): out at it, dragging it in, the blade through it, going back. */
    public static final int REAPER_OUT = 0;
    public static final int REAPER_DRAG = 1;
    public static final int REAPER_THROUGH = 2;
    public static final int REAPER_BACK = 3;

    /** How far out the centipedes are {@code age} ticks into a phase that sends them out (0..1), or back in ({@code back}). */
    public static double reaperReach(double age, boolean back) {
        if (back) {
            double u = Math.max(0.0, Math.min(1.0, age / REAPER_RETRACT));
            return 1.0 - u * u * (3.0 - 2.0 * u);
        }
        return ease(Math.max(0.0, Math.min(1.0, age / REAPER_REACH)));
    }

    // C: King's Defiance: the cocoon, held
    /** The longest it holds (ticks); how long it takes to close round him, and to open; the cocoon's size (blocks, round his middle). */
    public static final int DEFY_MOST = 200;
    public static final int DEFY_CLOSE = 8;
    public static final int DEFY_OPEN = 10;
    public static final double DEFY_RADIUS = 1.15;
    /** A spike out of it at what struck it: how far it reaches past the cocoon (blocks), its time out and back, what it does. */
    public static final double DEFY_SPIKE = 2.6;
    public static final int DEFY_SPIKE_TICKS = 12;
    public static final float DEFY_SPIKE_DAMAGE = 9.0F;
    public static final int DEFY_BLEED = 100;
    /** The wait before one striker can be answered again (ticks). */
    public static final int DEFY_EVERY = 6;
    /** The phases: closing round him and holding, opening. */
    public static final int DEFY_HELD = 0;
    public static final int DEFY_OPENING = 1;

    /** How far closed the cocoon is {@code age} ticks into holding (0..1), or into opening ({@code opening}). */
    public static double defyClosed(double age, boolean opening) {
        if (opening) {
            double u = Math.max(0.0, Math.min(1.0, age / DEFY_OPEN));
            return 1.0 - u * u * (3.0 - 2.0 * u);
        }
        return ease(Math.max(0.0, Math.min(1.0, age / DEFY_CLOSE)));
    }

    /** A spike's length out of the cocoon {@code age} ticks after it struck (0..1): out at once, held a moment, drawn back slow. */
    public static double defySpike(double age) {
        if (age < 0.0) {
            return 0.0;
        }
        if (age < 2.0) {
            return ease(age / 2.0);
        }
        if (age < 4.0) {
            return 1.0;
        }
        double u = Math.min(1.0, (age - 4.0) / (DEFY_SPIKE_TICKS - 4.0));
        return 1.0 - u * u * (3.0 - 2.0 * u);
    }

    // B: Spire Wave: the arm held out charging, and on letting go the centipedes out along it, carrying all before them, the crush
    /** The longest charge that counts (ticks), and the longest it may be held before it goes on its own. */
    public static final int SPIRE_CHARGE = 60;
    public static final int SPIRE_HOLD_MOST = 100;
    /** Reach: at no charge, at full (blocks); how fat the mass (its radius, blocks); how fast it goes out (blocks a tick). */
    public static final double SPIRE_REACH_LOW = 8.0;
    public static final double SPIRE_REACH_HIGH = 25.0;
    public static final double SPIRE_SIZE_LOW = 1.0;
    public static final double SPIRE_SIZE_HIGH = 2.4;
    public static final double SPIRE_SPEED_LOW = 0.75;
    public static final double SPIRE_SPEED_HIGH = 1.0;
    /** The crush at its end: its time, and after it the centipedes' way back (ticks); what it does (at no charge, more at full). */
    public static final int SPIRE_CRUSH = 6;
    public static final int SPIRE_BACK = 16;
    public static final float SPIRE_CARRY_DAMAGE = 4.0F;
    public static final float SPIRE_CRUSH_LOW = 12.0F;
    public static final float SPIRE_CRUSH_HIGH = 30.0F;
    public static final int SPIRE_COOLDOWN = 300;
    /** The centipedes in the mass, at no charge and at full. */
    public static final int SPIRE_ARMS_LOW = 5;
    public static final int SPIRE_ARMS_HIGH = 8;

    /** How charged it is after {@code held} ticks (0..1). */
    public static double spireCharge(double held) {
        return Math.max(0.0, Math.min(1.0, held / SPIRE_CHARGE));
    }

    public static double spireReach(double charge) {
        return SPIRE_REACH_LOW + (SPIRE_REACH_HIGH - SPIRE_REACH_LOW) * charge;
    }

    public static double spireSize(double charge) {
        return SPIRE_SIZE_LOW + (SPIRE_SIZE_HIGH - SPIRE_SIZE_LOW) * charge;
    }

    public static double spireSpeed(double charge) {
        return SPIRE_SPEED_LOW + (SPIRE_SPEED_HIGH - SPIRE_SPEED_LOW) * charge;
    }

    public static int spireArms(double charge) {
        return SPIRE_ARMS_LOW + (int)Math.round((SPIRE_ARMS_HIGH - SPIRE_ARMS_LOW) * charge);
    }

    /** The tick the mass reaches the end of {@code reach} going {@code speed} (its crush). */
    public static int spireArrive(double reach, double speed) {
        return (int)Math.ceil(reach / speed);
    }

    /**
     * How far out the mass's head is {@code age} ticks after letting go (blocks): out at its speed (a touch slower to start, as
     * something heavy), held at the end through the crush, then drawn back.
     */
    public static double spireFront(double age, double reach, double speed) {
        int arrive = spireArrive(reach, speed);
        if (age <= 0.0) {
            return 0.0;
        }
        if (age < arrive) {
            double u = age / arrive;
            return reach * (0.82 * u + 0.18 * u * u);
        }
        if (age < arrive + SPIRE_CRUSH) {
            return reach;
        }
        double u = Math.min(1.0, (age - arrive - SPIRE_CRUSH) / SPIRE_BACK);
        return reach * (1.0 - u * u * (3.0 - 2.0 * u));
    }

    /** Ticks from letting go to the end of it all. */
    public static int spireTicks(double reach, double speed) {
        return spireArrive(reach, speed) + SPIRE_CRUSH + SPIRE_BACK;
    }

    /** Fast at first, settling at the end. */
    static double ease(double x) {
        double k = 1.0 - Math.max(0.0, Math.min(1.0, x));
        return 1.0 - k * k * k;
    }

    /** What a key did: pressed, let go, or pressed crouched. */
    public static final int PRESS = 0;
    public static final int RELEASE = 1;
    public static final int CROUCH_PRESS = 2;

    // King's Execution
    public static final double GRASP_RANGE = 15.0;
    /** The aim it takes to be seized without being under the crosshair (cosine off the look). */
    public static final double GRASP_AIM = 0.94;
    public static final int GRASP_ARMS = 5;
    /** Ticks: the arms reach it by the first, coiled round it by the second; it squeezes until the crush; all back in by the last. */
    public static final int GRASP_REACH = 5;
    public static final int GRASP_COIL = 8;
    public static final int GRASP_CRUSH = 26;
    public static final int GRASP_TICKS = 34;
    public static final int SQUEEZE_EVERY = 4;
    public static final float SQUEEZE_DAMAGE = 4.0F;
    public static final float CRUSH_DAMAGE = 34.0F;
    public static final int CRUSH_STUN = 30;
    /** Too big to hold (max health above this): it is only crushed where it stands. */
    public static final float HOLD_LIMIT = 150.0F;

    // Black Reaper
    public static final double DASH_RANGE = 10.0;
    public static final int DASH_TICKS = 6;
    public static final int CLEAVE_TICKS = 14;
    /** The tick of the cleave the blade lands on. */
    public static final int CLEAVE_LANDS = 5;
    public static final double CLEAVE_RADIUS = 5.0;
    /** The cleave's half-angle in front (cosine): seventy degrees either side. */
    public static final double CLEAVE_ARC = 0.34;
    public static final float CLEAVE_DAMAGE = 30.0F;
    public static final int CLEAVE_BLEED = 140;

    // Centipede Dominion
    /** 1.0.29: longer, now that it is his to drive (sneaking ends it sooner). */
    public static final int WYRM_TICKS = 240;
    /**
     * 1.0.29: driven: its pace on W, rushing (sprinting), how fast it gets up to pace and comes to a stop (blocks a tick, each tick),
     * how much faster it turns while it goes and for A and D (radians a tick).
     */
    public static final double WYRM_RUSH = 0.85;
    public static final double WYRM_SPEED_UP = 0.06;
    public static final double WYRM_SLOW_DOWN = 0.1;
    public static final double WYRM_TURN_MOVING = 0.2;
    public static final double WYRM_TURN_KEYS = 0.12;
    /** 1.0.29: M1: the head lunged out at what he looks at and its bite (how far, its ticks out and back, how wide, its wait). */
    public static final double WYRM_LUNGE = 4.0;
    public static final int WYRM_LUNGE_TICKS = 8;
    public static final double WYRM_LUNGE_RADIUS = 2.2;
    public static final int WYRM_LUNGE_EVERY = 12;
    /** 1.0.29: RMB: reared up and slammed down before it (its ticks, the tick it lands, how far round, what it does, its wait). */
    public static final int WYRM_SLAM_TICKS = 16;
    public static final int WYRM_SLAM_LANDS = 11;
    public static final double WYRM_SLAM_RADIUS = 4.5;
    public static final float WYRM_SLAM_DAMAGE = 14.0F;
    public static final int WYRM_SLAM_EVERY = 40;
    /** 1.0.29: Space: the leap (its ticks, how high, how fast, its wait). */
    public static final int WYRM_LEAP_TICKS = 12;
    public static final double WYRM_LEAP_HEIGHT = 4.5;
    public static final double WYRM_LEAP_SPEED = 1.3;
    public static final int WYRM_LEAP_EVERY = 36;
    /** 1.0.29: the driving input's bits ({@code C2SKingAction.WYRM}). */
    public static final int WYRM_FORWARD = 1;
    public static final int WYRM_BACK = 2;
    public static final int WYRM_LEFT = 4;
    public static final int WYRM_RIGHT = 8;
    public static final int WYRM_JUMP = 16;
    public static final int WYRM_SPRINT = 32;
    public static final int WYRM_SNEAK = 64;
    /** 1.0.33: M1 and RMB go with the keys too (the chase camera has the clicks): the bite, the slam. */
    public static final int WYRM_KEY_BITE = 128;
    public static final int WYRM_KEY_SLAM = 256;
    /**
     * 1.0.33: its little turning first (a short one of the Dragon's): doubled over and shaking for {@link #WYRM_PRELUDE} ticks, then it
     * tears out of his back and rises out of him for {@link #WYRM_RISE} (reared up {@link #WYRM_RISE_HEIGHT} over him, roaring, then
     * down onto the ground {@link #WYRM_RISE_REACH} before him), his to drive from then on.
     */
    public static final int WYRM_PRELUDE = 14;
    public static final int WYRM_RISE = 26;
    public static final double WYRM_RISE_HEIGHT = 3.4;
    public static final double WYRM_RISE_REACH = 6.5;
    /** The rise's beats (ticks into it): at its height (the roar), and when it starts down. */
    public static final int WYRM_RISE_TOP = 9;
    public static final int WYRM_RISE_DOWN = 15;

    /** 1.0.33: where its head is {@code t} ticks into its rise: {ahead of his chest, over it} (blocks). */
    public static double[] wyrmRise(double t) {
        double up;
        double ahead;
        if (t < WYRM_RISE_TOP) {
            double k = ease(Math.max(0.0, t) / WYRM_RISE_TOP);
            up = WYRM_RISE_HEIGHT * k;
            ahead = 0.8 * k;
        } else if (t < WYRM_RISE_DOWN) {
            up = WYRM_RISE_HEIGHT + 0.15 * Math.sin((t - WYRM_RISE_TOP) * 1.3);
            ahead = 0.8;
        } else {
            double k = ease(Math.min(1.0, (t - WYRM_RISE_DOWN) / (double)(WYRM_RISE - WYRM_RISE_DOWN)));
            up = WYRM_RISE_HEIGHT * (1.0 - k);
            ahead = 0.8 + (WYRM_RISE_REACH - 0.8) * k;
        }
        return new double[] {ahead, up};
    }

    /**
     * 1.0.33: the way he drives it, as he would walk: W ahead where he looks, A and D to his left and right of that, both together
     * between; null when none is held (S is the brake). {@code lookX}, {@code lookZ} his aim across the ground.
     */
    public static double[] wyrmWay(double lookX, double lookZ, boolean forward, int left) {
        double l = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (l < 1.0E-6 || (!forward && left == 0)) {
            return null;
        }
        double fx = lookX / l;
        double fz = lookZ / l;
        // His left, seen from above: the aim turned a quarter to the left (x, z) -> (z, -x).
        double x = (forward ? fx : 0.0) + left * fz;
        double z = (forward ? fz : 0.0) - left * fx;
        double n = Math.sqrt(x * x + z * z);
        return new double[] {x / n, z / n};
    }

    /**
     * 1.0.39: where he looks as he drives it, sent with the keys every tick: the server never hears his aim otherwise while the chase
     * camera has the view (the game only sends a player's turning while the camera is his own body), so it steered by the way he faced
     * when it began. Yaw in the top 16 bits, pitch in the next 15, and the lowest bit set (0: no aim sent).
     */
    public static int wyrmAim(float yaw, float pitch) {
        double turned = ((yaw % 360.0) + 360.0) % 360.0;
        int y = (int) Math.round(turned / 360.0 * 65536.0) & 0xFFFF;
        int p = (int) Math.round((Math.max(-90.0, Math.min(90.0, pitch)) + 90.0) / 180.0 * 32767.0);
        return y << 16 | p << 1 | 1;
    }

    /** 1.0.40: {yaw, pitch} (degrees) of {@code aim} ({@link #wyrmAim}); null if none was sent. */
    public static double[] wyrmAngles(int aim) {
        if ((aim & 1) == 0) {
            return null;
        }
        return new double[] {(aim >>> 16) / 65536.0 * 360.0, ((aim >>> 1) & 0x7FFF) / 32767.0 * 180.0 - 90.0};
    }

    /** 1.0.39: the way {@code aim} ({@link #wyrmAim}) looks, as Minecraft turns yaw and pitch into a view: {x, y, z}; null if none was sent. */
    public static double[] wyrmLook(int aim) {
        double[] angles = wyrmAngles(aim);
        if (angles == null) {
            return null;
        }
        double yaw = Math.toRadians(angles[0]);
        double pitch = Math.toRadians(angles[1]);
        return new double[] {-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
    }

    /** 1.0.29: how far the lunging head is out ({@code age} ticks in, 0..1): out fast, back slower. */
    public static double wyrmLunge(double age) {
        if (age < 0.0 || age > WYRM_LUNGE_TICKS) {
            return 0.0;
        }
        double out = 3.0;
        return age < out ? ease(age / out) : 1.0 - ((age - out) / (WYRM_LUNGE_TICKS - out)) * ((age - out) / (WYRM_LUNGE_TICKS - out));
    }

    /** 1.0.29: how high the head is reared for the slam ({@code age} ticks in, blocks over where it was), down hard at its landing. */
    public static double wyrmRear(double age) {
        if (age < 0.0 || age > WYRM_SLAM_TICKS) {
            return 0.0;
        }
        if (age < WYRM_SLAM_LANDS - 2) {
            return 3.5 * ease(age / (WYRM_SLAM_LANDS - 2));
        }
        if (age < WYRM_SLAM_LANDS) {
            return 3.5 * (1.0 - (age - (WYRM_SLAM_LANDS - 2)) / 2.0);
        }
        return 0.0;
    }

    /** 1.0.29: the leap's height ({@code age} ticks in, blocks over the ground). */
    public static double wyrmLeap(double age) {
        if (age < 0.0 || age > WYRM_LEAP_TICKS) {
            return 0.0;
        }
        double u = age / WYRM_LEAP_TICKS;
        return WYRM_LEAP_HEIGHT * 4.0 * u * (1.0 - u);
    }
    /** Blocks a tick it goes, and the fastest it turns toward his aim (radians a tick). */
    public static final double WYRM_SPEED = 0.45;
    public static final double WYRM_TURN = 0.12;
    /** 1.0.15: how fast it climbs a wall's face and eases down off an edge (blocks a tick), and how high its head rears looking up. */
    public static final double WYRM_CLIMB = 0.55;
    public static final double WYRM_FALL = 0.9;
    public static final double WYRM_REAR = 3.0;
    /** Its size against the great Dragon's. */
    public static final double WYRM_SCALE = 0.24;
    public static final float WYRM_BITE = 16.0F;
    public static final float WYRM_SNAP = 8.0F;
    public static final float WYRM_CRUSH = 4.0F;
    /** Ticks before the same body is bitten (or crushed) again. */
    public static final int WYRM_AGAIN = 10;

    // Spire Wave
    public static final int SPIRE_TICKS = 22;
    /** The tick of the move the arm hits the ground on; the wave runs a block a tick from there. */
    public static final int SPIRE_SLAM = 8;
    public static final int SPIRE_LENGTH = 20;
    public static final double SPIRE_RADIUS = 1.6;
    public static final float SPIRE_DAMAGE = 18.0F;
    public static final double SPIRE_LAUNCH = 1.0;
    public static final int SPIRE_PIN = 24;
    /** How long the spikes stand before they sink back (ticks). */
    public static final int SPIRE_STAND = 20;

    // King's Defiance
    public static final int PARRY_TICKS = 12;
    public static final int COUNTER_TICKS = 10;
    public static final float COUNTER_DAMAGE = 20.0F;
    public static final int COUNTER_STUN = 30;
    public static final int COUNTER_BLEED = 100;
    /** How far behind the striker he comes out. */
    public static final double BLINK_BEHIND = 1.6;
    public static final int COCOON_TICKS = 60;
    public static final int BURST_TICKS = 12;
    public static final double BURST_RADIUS = 7.0;
    public static final float BURST_BASE = 10.0F;
    public static final double BURST_STORED = 1.5;
    public static final float BURST_CAP = 60.0F;

    // Black Sun (1.0.15: out of the ground where he looks)
    /** How far off he can set it (blocks), how far round that spot its centipedes seize (and how many), how far its darkness falls. */
    public static final double SUN_RANGE = 40.0;
    public static final double SUN_RADIUS = 12.0;
    public static final int SUN_SEIZE = 10;
    public static final double SUN_DARK = 30.0;
    /** Ticks: the ground cracks open, the centipedes burst up and seize by the first; lifted high by the next; held up there; slammed
     *  down by the next; sunk back into the ground by the end. */
    public static final int SUN_OPEN = 14;
    public static final int SUN_LIFT = 46;
    public static final int SUN_HANG = 60;
    public static final int SUN_SLAM = 67;
    public static final int SUN_TICKS = 86;
    /** How high they are lifted (blocks); the squeeze while held (every so many ticks); the slam, its stun, and what it splashes. */
    public static final double SUN_HEIGHT = 16.0;
    public static final int SQUEEZE_HELD = 10;
    public static final float HELD_DAMAGE = 3.0F;
    public static final float SLAM_DAMAGE = 26.0F;
    public static final int SLAM_STUN = 30;
    public static final double SLAM_SPLASH = 3.0;
    public static final float SPLASH_DAMAGE = 8.0F;
    /** The slam kills what is left at this share of its health or less (not what is bigger than {@link #SUN_LIMIT}). */
    public static final double SUN_EXECUTE = 0.3;
    public static final float SUN_LIMIT = 300.0F;

    // 1.0.7: the rinkaku's instinct, always on while War is worn. 1.0.35: cut right down (the user's call: it killed everything before
    // it got near him): a body must come close to be lashed, one lash at a time between all six, and one spike every twenty seconds.
    /** How far the rinkaku hunt (blocks), and how near a body must be to be lashed rather than shot. */
    public static final double INSTINCT_RANGE = 14.0;
    public static final double LASH_RANGE = 4.0;
    public static final float LASH_DAMAGE = 3.0F;
    public static final float SPIKE_DAMAGE = 2.0F;
    /** Ticks a rinkaku waits after a lash; ticks between any two lashes of the six; ticks between two spikes (of all six). */
    public static final int LASH_EVERY = 60;
    public static final int LASH_GAP = 24;
    public static final int SPIKE_COOLDOWN = 400;
    /** A spike's speed (blocks a tick), and how long it flies before it is spent. */
    public static final double SPIKE_SPEED = 2.4;
    public static final int SPIKE_LIFE = 12;
    /** The share of what War's blows take that comes back to him as health. */
    public static final double LIFESTEAL = 0.15;
    /** Ticks a body that struck War stays his prey. */
    public static final int PREY_MEMORY = 200;

    private WarRules() {
    }

    /** True if a body at {@code dx, dz} (horizontal, from him) is in a sweep facing {@code fx, fz} (unit) out to {@code range}, {@code arc} wide (cosine). */
    public static boolean inSweep(double dx, double dz, double fx, double fz, double range, double arc) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > range) {
            return false;
        }
        return d < 0.8 || (dx * fx + dz * fz) / d >= arc;
    }

    /** True if the Black Sun's slam kills a body left at {@code health} of {@code max}. */
    public static boolean executes(float health, float max) {
        return max > 0.0F && max <= SUN_LIMIT && health <= max * SUN_EXECUTE;
    }

    /** The instinct's choice for a body {@code distance} off: lashed (true) or shot (false); beyond its range, neither (null). */
    public static Boolean lashes(double distance) {
        return distance > INSTINCT_RANGE ? null : distance <= LASH_RANGE;
    }

    /** What the cocoon bursts out with, having taken {@code stored}. */
    public static float burst(double stored) {
        return (float)Math.min(BURST_CAP, BURST_BASE + Math.max(0.0, stored) * BURST_STORED);
    }

    /** The heading (radians) the small Dragon turns to from {@code heading} toward {@code wanted} in one tick, no faster than it can. */
    public static double turn(double heading, double wanted) {
        double d = Math.atan2(Math.sin(wanted - heading), Math.cos(wanted - heading));
        return heading + Math.max(-WYRM_TURN, Math.min(WYRM_TURN, d));
    }

    /**
     * 1.0.15: how high (share of {@link #SUN_HEIGHT}) the Black Sun's centipedes hold what they seized, {@code age} ticks in: up from
     * the ground once seized, held high, then driven down faster and faster into the slam.
     */
    public static double lift(double age) {
        if (age <= SUN_OPEN || age >= SUN_SLAM) {
            return 0.0;
        }
        if (age < SUN_LIFT) {
            double u = (age - SUN_OPEN) / (double)(SUN_LIFT - SUN_OPEN);
            return u * u * (3.0 - 2.0 * u);
        }
        if (age < SUN_HANG) {
            return 1.0;
        }
        double u = (age - SUN_HANG) / (double)(SUN_SLAM - SUN_HANG);
        return 1.0 - u * u;
    }
}
