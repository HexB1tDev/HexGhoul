package com.hex.kagune.control.ukaku;

import com.hex.kagune.control.UkakuStage;
import java.util.ArrayList;
import java.util.List;

/**
 * Poses Ukaku's kagune in 3D (pure, tested). Coordinates are player-local blocks: x to the player's right, y up from
 * the feet, z forward. The renderer only rotates the result by the body's yaw and moves it to the player.
 *
 * <p>Twin wings sit on the shoulder blades and spread to the sides like the anime frames (swept back a little); the
 * left wing alone is the Rabbit Guise; the One-Winged Kagune rises from the left shoulder blade. The geometry is the
 * solid, feathered {@link UkakuModel}; this class places each wing and turns motions into poses: a striking wing folds
 * into a blade and swings across the front, a volley flares the wings forward and flashes at the feather tips.
 */
public final class WingRig {
    /** Shoulder-blade roots (x right, y up, z forward) for a standing player. */
    public static final double ROOT_X = 0.21;
    public static final double ROOT_Y = 1.24;
    public static final double ROOT_Z = -0.17;
    public static final double CROUCH_DROP = 0.3;
    /** Resting sweep of the wings behind the shoulders, degrees. */
    public static final float REST_SWEEP = 22.0F;
    /** Resting lift of the One-Winged Kagune (tip raised up and back), degrees. */
    public static final float ONE_WING_REST_PITCH = 38.0F;
    /** Twin-wing pose while firing a Shard Volley: swept forward to aim over the shoulders, slightly rolled down. */
    public static final float VOLLEY_SWEEP = -12.0F;
    public static final float VOLLEY_ROLL = -8.0F;

    /**
     * What the kagune is doing; strikes come from the server-timed combo stage. Aiming (the M1 shots, the enders that fire)
     * is not a motion but the {@link Input} aim amounts, so it blends with any of these.
     */
    public enum Motion {
        NONE, KICK, SLASH_RIGHT, SLASH_LEFT, CROSS, BURST, ONE_SLASH, ONE_RETURN, ONE_BEHEAD,
        /** Rabbit Dash: both wings swept far back, streaming fire behind. */
        DASH,
        /** Rabbit Hop: one heavy downbeat of the wings that throws him back. */
        HOP,
        /** Black Rabbit: the wings fold in tight through the backflip, then spread again. */
        FLIP,
        /** Rising Flame: both wings sweep up from low as the pillar erupts. */
        RISE,
        // 4.1.2: the Twin Wings' slams in the fire, and their guard.
        /** A quick slap across the front, flat, at chest height (one wing). */
        SLAP_RIGHT, SLAP_LEFT,
        /** A wide swing: drawn far back and up, swung all the way across (one wing). */
        SWING_RIGHT, SWING_LEFT,
        /** Low and forward, then swept up high through the target (one wing). */
        RISE_RIGHT, RISE_LEFT,
        /** Raised high over the shoulder, then driven straight down in front (one wing). */
        SLAM_RIGHT, SLAM_LEFT,
        /** Both raised high together, then slammed down in front as one. */
        TWIN_SLAM,
        /** The guard: both wings grown out and swung forward round him, meeting in front, a little raised ({@code progress}: how far closed). */
        GUARD
    }

    /**
     * Everything the rig needs for one frame.
     *
     * @param crystalRight 0..1 smoothed crystallization of the right wing (the left for one-wing forms uses crystalLeft)
     * @param progress     0..1 progress of {@code motion}; {@code impact} is the fraction at which it connects
     * @param volley       0..1 eased volley flare; {@code recoil} 0..1 pulses with each wave
     * @param detail       0..1 level of detail; {@code alpha} multiplies every polygon's opacity
     * @param dissolving   the kagune is retracting: it shrinks back into the shoulder blades, shedding crystal chips
     * @param coilRight    0..1 the right wing twirling up to be thrown as a Wing Vortex ({@code coilLeft} the left)
     * @param growRight    0..1 how much of the right wing is on his back (0 thrown, growing back to 1; {@code growLeft} the left)
     * @param barrel       radians the aiming wings are turned about their centre lines (the Gatling's shudder; 0 none)
     */
    public record Input(WingForm form, int mastery, float deploy, double time, float crystalRight, float crystalLeft, Motion motion,
                        float progress, float impact, float volley, float recoil, boolean crouching, float detail,
                        float alpha, boolean dissolving, float aimRight, float aimLeft, double[] aim, float kickRight, float kickLeft,
                        float coilRight, float coilLeft, float growRight, float growLeft, float barrel) {
        /** No Gatling spinning. */
        public Input(WingForm form, int mastery, float deploy, double time, float crystalRight, float crystalLeft, Motion motion, float progress,
                     float impact, float volley, float recoil, boolean crouching, float detail, float alpha, boolean dissolving,
                     float aimRight, float aimLeft, double[] aim, float kickRight, float kickLeft, float coilRight, float coilLeft, float growRight,
                     float growLeft) {
            this(form, mastery, deploy, time, crystalRight, crystalLeft, motion, progress, impact, volley, recoil, crouching, detail, alpha,
                dissolving, aimRight, aimLeft, aim, kickRight, kickLeft, coilRight, coilLeft, growRight, growLeft, 0.0F);
        }

        /** Both wings on his back, neither thrown. */
        public Input(WingForm form, int mastery, float deploy, double time, float crystalRight, float crystalLeft, Motion motion, float progress,
                     float impact, float volley, float recoil, boolean crouching, float detail, float alpha, boolean dissolving,
                     float aimRight, float aimLeft, double[] aim, float kickRight, float kickLeft) {
            this(form, mastery, deploy, time, crystalRight, crystalLeft, motion, progress, impact, volley, recoil, crouching, detail, alpha,
                dissolving, aimRight, aimLeft, aim, kickRight, kickLeft, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
        }

        /** No aiming: both wings in their motion's pose. */
        public Input(WingForm form, int mastery, float deploy, double time, float crystalRight, float crystalLeft, Motion motion, float progress,
                     float impact, float volley, float recoil, boolean crouching, float detail, float alpha, boolean dissolving) {
            this(form, mastery, deploy, time, crystalRight, crystalLeft, motion, progress, impact, volley, recoil, crouching, detail, alpha,
                dissolving, 0.0F, 0.0F, FORWARD, 0.0F, 0.0F);
        }
    }

    /** Straight ahead in rig space. */
    public static final double[] FORWARD = {0.0, 0.0, 1.0};
    /**
     * Of the turn from the wing's rest line to the target, the part done by tipping the whole wing forward at the root
     * (the rest curls the wing along its length), so the bend runs smoothly from root to tip instead of all at the end.
     */
    static final double AIM_TURN = 0.3;
    /** Blocks of the wing that stay straight before it starts to curl. */
    static final double AIM_STRAIGHT = 0.2;
    /** Where along the wing's centre line (wing lengths) a shot leaves: just inside the visible flame tip. */
    static final double MUZZLE = 0.86;
    /** How much a wing swells as a shot leaves it (fraction of its length at the kick's peak), like a flame flaring. */
    static final double AIM_FLARE = 0.05;
    /** The flame's own thrust as a shot leaves (0..1 of a kick's): hotter, a little longer, barely streaming back. */
    static final float AIM_SHOT_THRUST = 0.3F;
    /** The fan's centre line in the wing plane: the main tongue's direction (radians from outward toward up). */
    static final double FAN_CENTRE = Math.toRadians(44.0);

    /** One quad (triangles repeat a corner): corners, a normal, and one ARGB colour per corner (gradients and glows). */
    public record Quad(float[] xyz, float nx, float ny, float nz, int[] argb) {
        /** The most opaque corner's alpha, 0..1. */
        public float alpha() {
            int max = 0;
            for (int colour : this.argb) {
                max = Math.max(max, colour >>> 24);
            }
            return max / 255.0F;
        }
    }

    /** Angles for one twin wing, degrees. */
    public record WingAngles(float sweep, float roll) {
    }

    private WingRig() {
    }

    private static float smooth(float t) {
        float c = Math.max(0.0F, Math.min(1.0F, t));
        return c * c * (3.0F - 2.0F * c);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** The swing starts this long (in progress) before the impact, and ends this long after it. */
    static final float SWING_LEAD = 0.12F;
    static final float SWING_TAIL = 0.05F;
    /** After the swing, the pose holds this long before settling back. */
    static final float FOLLOW_HOLD = 0.08F;

    /**
     * Wind-up, a fast swing that lands on the impact, a short hold, then settle back: returns {windup 0..1, swing 0..1,
     * settle 0..1}. The server applies the hit at {@code impact}, so the swing is already most of the way through there
     * (about 0.8) instead of just starting.
     */
    static float[] phases(float progress, float impact) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        float i = Math.max(0.15F, Math.min(0.9F, impact));
        float swingStart = i - SWING_LEAD;
        float swingEnd = i + SWING_TAIL;
        float settleStart = Math.min(0.97F, swingEnd + FOLLOW_HOLD);
        return new float[] {smooth(p / swingStart), smooth((p - swingStart) / (swingEnd - swingStart)),
            smooth((p - settleStart) / (1.0F - settleStart))};
    }

    /** Pose of a striking twin wing: pull back and up, then swing across the front with the blade flat, then settle. */
    static WingAngles slash(float progress, float impact) {
        float[] ph = phases(progress, impact);
        float sweep = lerp(REST_SWEEP, 60.0F, ph[0]);
        float roll = lerp(0.0F, 25.0F, ph[0]);
        // Swing level across the front of the chest (a lower roll would drive the blade into the ground).
        sweep = lerp(sweep, -105.0F, ph[1]);
        roll = lerp(roll, -8.0F, ph[1]);
        sweep = lerp(sweep, REST_SWEEP, ph[2]);
        roll = lerp(roll, 0.0F, ph[2]);
        return new WingAngles(sweep, roll);
    }

    /** 4.1.10: the guard's beats by its amount: {flared open, slammed shut past where it holds}, then held (degrees for the flare and the past). */
    static final float GUARD_OPEN = 0.2F;
    static final float GUARD_PAST = 0.82F;
    static final float GUARD_FLARE_SWEEP = 40.0F;
    static final float GUARD_FLARE_ROLL = -6.0F;
    static final float GUARD_OVER = 0.0F;

    /** 4.1.10: the guarding wings' angles at {@code amount} (0 at rest, 1 held closed). */
    public static WingAngles guard(float amount) {
        float a = Math.max(0.0F, Math.min(1.0F, amount));
        if (a < GUARD_OPEN) {
            float k = smooth(a / GUARD_OPEN);
            return new WingAngles(lerp(REST_SWEEP, GUARD_FLARE_SWEEP, k), lerp(0.0F, GUARD_FLARE_ROLL, k));
        }
        if (a < GUARD_PAST) {
            float k = smooth((a - GUARD_OPEN) / (GUARD_PAST - GUARD_OPEN));
            return new WingAngles(lerp(GUARD_FLARE_SWEEP, GUARD_SWEEP - GUARD_OVER, k), lerp(GUARD_FLARE_ROLL, GUARD_ROLL, k));
        }
        float k = smooth((a - GUARD_PAST) / (1.0F - GUARD_PAST));
        return new WingAngles(lerp(GUARD_SWEEP - GUARD_OVER, GUARD_SWEEP, k), GUARD_ROLL);
    }

    /**
     * 4.1.13 (the owner: "I asked for a block animation where the wings like clamp together like hands in front of the player. And I get
     * this": 4.1.2's guard swung both wings forward and up, into a cone over his head, his body open): each wing comes forward along his
     * side (its root angles, {@link #guard}) and is bent across the front of his body at chest height (a curl toward the other side, a
     * little forward: {@link #GUARD_ACROSS}), its broad face out, the two meeting and overlapping in the middle as two hands close over
     * each other. By the guard's amount, so it opens the same way back.
     */
    static UkakuModel.Frame guardFrame(int side, double rootX, double rootY, double rootZ, double length, WingAngles angles, float amount) {
        float a = smooth(Math.max(0.0F, Math.min(1.0F, amount)));
        double[][] axes = twinAxes(side, angles.sweep(), angles.roll());
        double[] out = axes[0];
        double[] up = axes[1];
        double[] back = mul(cross(axes[1], axes[0]), side);
        double[] origin = {rootX, rootY, rootZ};
        double[] main = normalize(add(mul(out, Math.cos(FAN_CENTRE)), mul(up, Math.sin(FAN_CENTRE))));
        // Wrapped round toward the other side, level (the curl turns about the vertical, whatever the wing's own tilt: tied to its tilt, the
        // wrap rose over his head or dug into the ground): a curved shield round his front at the height its tilt sets, not a sail ahead.
        double curl = Math.toRadians(GUARD_WRAP) * a;
        double flat = Math.sqrt(main[0] * main[0] + main[2] * main[2]);
        if (curl < 1.0E-3 || flat < 1.0E-6) {
            return new UkakuModel.Frame(origin, out, up, back, length);
        }
        double[] across = rotateY(new double[] {main[0] / flat, 0.0, main[2] / flat}, -side * Math.PI * 0.5);
        double[] toward = sub(across, mul(main, dot(across, main)));
        if (dot(toward, toward) < 1.0E-8) {
            return new UkakuModel.Frame(origin, out, up, back, length);
        }
        // Its feathers folded by its length, so the shield spans GUARD_HALF either side of its line whatever the wing's size (knee to head).
        double spread = Math.toDegrees(Math.atan(GUARD_HALF / Math.max(0.3, length)));
        float fold = (float)Math.max(0.0, Math.min(GUARD_FOLD_MOST, 1.0 - spread / 45.0)) * a;
        UkakuModel.Bend bend = new UkakuModel.Bend(fold, FAN_CENTRE, main, normalize(toward), GUARD_STRAIGHT, curl, length);
        return new UkakuModel.Frame(origin, out, up, back, length, bend);
    }

    /** 4.1.13: how far round his front a guarding wing wraps (degrees), how far it runs straight first (blocks), how tight its feathers fold. */
    static final double GUARD_WRAP = 205.0;
    static final double GUARD_STRAIGHT = 0.35;
    /** 4.1.13: how far the closed shield reaches above and below its line (blocks), and the most its feathers fold. */
    static final double GUARD_HALF = 2.0;
    static final double GUARD_FOLD_MOST = 0.85;

    /** 4.1.2: the guard's wings, swung forward round him till they meet in front (degrees). */
    public static final float GUARD_SWEEP = -55.0F;
    public static final float GUARD_ROLL = -36.0F;
    /** 4.1.2: how much longer the guarding wings grow, fully closed (a share of their length). */
    public static final float GUARD_GROWTH = 0.12F;

    /** 4.1.2: which wing strikes in a one-wing slam (+1 right, -1 left; 0 neither). */
    public static int striking(Motion motion) {
        return switch (motion) {
            case SLAP_RIGHT, SWING_RIGHT, RISE_RIGHT, SLAM_RIGHT -> 1;
            case SLAP_LEFT, SWING_LEFT, RISE_LEFT, SLAM_LEFT -> -1;
            default -> 0;
        };
    }

    /**
     * 4.1.10 (the owner: "study complex fighting moves, and combos and make it look smooth and beautiful"): one slam of a striking wing, as a
     * fighter throws a blow. Its keys ({sweep, roll}, degrees): the CHAMBER (drawn back and cocked), the IMPACT (where it lands), the
     * OVERSHOOT (carried on past it), the RECOIL (the rebound off the hit); and its timing: the drive's share of the move (the chamber's load
     * to the impact), the overshoot's, the recoil's, and the move's length in ticks (ComboStyle.UKAKU_WINGS: the whip's spring runs on
     * them). 4.1.2's were a wind-up, a swing that slowed to a stop as it landed, and a hold. 4.1.11: and the stage's range (ComboStage.rangeMul:
     * how far it hits, {@link UkakuRules#slamReach}).
     */
    public record Blow(float[] chamber, float[] impact, float[] over, float[] recoil, float drive, float overshoot, float settle, float ticks,
                       float rangeMul) {
    }

    /** 4.1.10: each slam's blow ({@link Blow}). */
    public static Blow blow(Motion motion) {
        return switch (motion) {
            // A slap: a short cock back, snapped flat across the chest, carried well past, a little rebound. Quick.
            case SLAP_RIGHT, SLAP_LEFT -> new Blow(new float[] {50.0F, 12.0F}, new float[] {-70.0F, -4.0F}, new float[] {-110.0F, -9.0F},
                new float[] {-90.0F, -3.0F}, 0.28F, 0.12F, 0.12F, 16.0F, 1.0F);
            // The wide swing: wound far back and up, unwound all the way across the front, its weight carrying it round to the far side.
            case SWING_RIGHT, SWING_LEFT -> new Blow(new float[] {88.0F, 36.0F}, new float[] {-74.0F, -10.0F}, new float[] {-134.0F, -16.0F},
                new float[] {-116.0F, -9.0F}, 0.30F, 0.14F, 0.14F, 20.0F, 1.05F);
            // The rising wing: low and forward, then up through the target and on up past the head.
            case RISE_RIGHT, RISE_LEFT -> new Blow(new float[] {-18.0F, -32.0F}, new float[] {-46.0F, 54.0F}, new float[] {-38.0F, 82.0F},
                new float[] {-43.0F, 64.0F}, 0.28F, 0.12F, 0.12F, 20.0F, 1.0F);
            // Both raised higher still and held a beat longer (the drive is the same share of a longer move), then down together.
            case TWIN_SLAM -> new Blow(new float[] {4.0F, 86.0F}, new float[] {-68.0F, -8.0F}, new float[] {-96.0F, -14.0F},
                new float[] {-78.0F, -3.0F}, 0.28F, 0.08F, 0.10F, 28.0F, 1.05F);
            // The overhead slam: high over the shoulder, then driven down in front, level with the waist (never into the ground), a short
            // carry on, the bounce off what it hit.
            default -> new Blow(new float[] {8.0F, 80.0F}, new float[] {-68.0F, -8.0F}, new float[] {-94.0F, -14.0F},
                new float[] {-76.0F, -3.0F}, 0.28F, 0.10F, 0.12F, 22.0F, 1.0F);
        };
    }

    /** 4.1.10: how much further than the chamber the wing is drawn while it loads (a share of the way from the impact back to the chamber). */
    static final float LOAD = 0.1F;
    /**
     * 4.1.10: how hard the drive accelerates into the impact (the power of its ease-in: 2 is a constant push, more a later snap). 4.1.12: 1.6
     * (4.1.10's 2.4 crammed the swing into its last instant; the owner could barely see it).
     */
    static final float DRIVE_SNAP = 1.6F;

    /**
     * 4.1.10: the slam's moments as shares of the move: {chamber reached, the load ends (the drive starts), the impact, the overshoot's end,
     * the recoil's end}; the rest settles back by 1.
     */
    public static float[] beats(Blow blow, float impact) {
        float hit = Math.max(0.2F, Math.min(0.85F, impact));
        float load = Math.max(0.06F, hit - blow.drive());
        float chamber = 0.7F * load;
        float over = Math.min(0.96F, hit + blow.overshoot());
        float recoil = Math.min(0.985F, over + blow.settle());
        return new float[] {chamber, load, hit, over, recoil};
    }

    /**
     * 4.1.10: a striking wing's root angles at {@code progress} through {@code motion}: rest, eased into the chamber; loaded a touch further
     * back; DRIVEN into the impact on an ease-in (slow out of the load, fastest as it lands, never braking into the target); carried on past
     * on an ease-out (the impact took some of its speed); rebounding; settled back to rest.
     */
    public static WingAngles slam(Motion motion, float progress, float impact) {
        Blow blow = blow(motion);
        float[] at = beats(blow, impact);
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        float[] rest = {REST_SWEEP, 0.0F};
        float[] chamber = blow.chamber();
        float[] loaded = {chamber[0] + LOAD * (chamber[0] - blow.impact()[0]), chamber[1] + LOAD * (chamber[1] - blow.impact()[1])};
        float[] from;
        float[] to;
        float k;
        if (p < at[0]) {
            from = rest;
            to = chamber;
            k = smooth(p / at[0]);
        } else if (p < at[1]) {
            from = chamber;
            to = loaded;
            k = smooth((p - at[0]) / (at[1] - at[0]));
        } else if (p < at[2]) {
            from = loaded;
            to = blow.impact();
            k = (float)Math.pow((p - at[1]) / (at[2] - at[1]), DRIVE_SNAP);
        } else if (p < at[3]) {
            from = blow.impact();
            to = blow.over();
            float s = (p - at[2]) / (at[3] - at[2]);
            k = 1.0F - (1.0F - s) * (1.0F - s);
        } else if (p < at[4]) {
            from = blow.over();
            to = blow.recoil();
            k = smooth((p - at[3]) / (at[4] - at[3]));
        } else {
            from = blow.recoil();
            to = rest;
            k = smooth((p - at[4]) / (1.0F - at[4]));
        }
        return new WingAngles(lerp(from[0], to[0], k), lerp(from[1], to[1], k));
    }

    /**
     * 4.1.10: how the feathers of a slamming wing fold ({@link UkakuModel.Bend}'s fold: + toward its centre line, - spread): tucked in tight
     * as it loads and drives (a blade cutting the air), thrown open on the impact, easing back as it rebounds.
     */
    public static float slamFold(Motion motion, float progress, float impact) {
        float[] at = beats(blow(motion), impact);
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        if (p < at[1]) {
            return SLAM_TUCK * smooth((p - at[0] * 0.5F) / (at[1] - at[0] * 0.5F));
        }
        if (p < at[2]) {
            return SLAM_TUCK;
        }
        if (p < at[4]) {
            return lerp(-SLAM_FLARE, 0.0F, smooth((p - at[2]) / (at[4] - at[2])));
        }
        return 0.0F;
    }

    static final float SLAM_TUCK = 0.3F;
    static final float SLAM_FLARE = 0.22F;

    /** 4.1.10: the fire streaming back off a slamming wing (its thrust, 0..1): building through the drive, gone by the end of the overshoot. */
    static float slamThrust(Motion motion, float progress, float impact) {
        float[] at = beats(blow(motion), impact);
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        if (p < at[1] || p >= at[3]) {
            return 0.0F;
        }
        if (p < at[2]) {
            return SLAM_THRUST * (p - at[1]) / (at[2] - at[1]);
        }
        return SLAM_THRUST * (1.0F - smooth((p - at[2]) / (at[3] - at[2])));
    }

    static final float SLAM_THRUST = 0.55F;

    /** 4.1.10: how hard a slamming wing is (UkakuModel's strike: its RC hardens), from the chamber to the end of its recoil. */
    static float slamHard(Motion motion, float progress, float impact) {
        float[] at = beats(blow(motion), impact);
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        return smooth(p / at[0]) * (1.0F - smooth((p - at[4]) / (1.0F - at[4])));
    }

    /**
     * 4.1.10: the other wing's pull while one slams (degrees, {sweep, roll}): drawn back as the striking wing drives, as a fighter's other
     * hand pulls back to the hip, released through the overshoot.
     */
    public static float[] counterPull(Motion motion, float progress, float impact) {
        float[] at = beats(blow(motion), impact);
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        float k = smooth((p - at[1]) / (at[2] - at[1])) * (1.0F - smooth((p - at[3]) / (1.0F - at[3])));
        return new float[] {COUNTER_SWEEP * k, COUNTER_ROLL * k};
    }

    static final float COUNTER_SWEEP = 20.0F;
    static final float COUNTER_ROLL = 9.0F;

    /**
     * 4.1.10: the whip. The wing's tip follows its root through a damped spring (in ticks: {@link Blow#ticks}), so it trails as the drive
     * speeds the root up, snaps on past it as the root is stopped by the impact, and settles: {sweep, roll} where the tip points. Worked out
     * from the start of the move every time (no state: the same frame comes out of the same progress).
     */
    public static float[] tip(Motion motion, float progress, float impact) {
        Blow blow = blow(motion);
        double end = Math.max(0.0F, Math.min(1.0F, progress)) * blow.ticks();
        int steps = Math.max(1, (int)Math.ceil(end / WHIP_STEP));
        double dt = end / steps;
        // 4.1.11: the spring as quick as the blow (a share of its drive's ticks): a slap's tip trails it by a beat, never a quarter turn.
        double w = 2.0 * Math.PI / Math.max(WHIP_PERIOD_LEAST, WHIP_PER_DRIVE * blow.drive() * blow.ticks());
        double sweep = REST_SWEEP;
        double roll = 0.0;
        double vSweep = 0.0;
        double vRoll = 0.0;
        for (int i = 1; i <= steps; i++) {
            WingAngles root = slam(motion, (float)(i * dt / blow.ticks()), impact);
            vSweep += (w * w * (root.sweep() - sweep) - 2.0 * WHIP_DAMPING * w * vSweep) * dt;
            vRoll += (w * w * (root.roll() - roll) - 2.0 * WHIP_DAMPING * w * vRoll) * dt;
            sweep += vSweep * dt;
            roll += vRoll * dt;
        }
        return new float[] {(float)sweep, (float)roll};
    }

    /**
     * 4.1.10: the whip's spring: its damping (1 none overshoots; less, a snap past and back), its step (ticks). 4.1.11: its period is
     * {@link #WHIP_PER_DRIVE} of the blow's drive (ticks), at least {@link #WHIP_PERIOD_LEAST} (4.1.10's 3.2 ticks left a slap's tip a
     * quarter turn behind at the impact).
     */
    static final double WHIP_PER_DRIVE = 1.4;
    static final double WHIP_PERIOD_LEAST = 1.2;
    static final double WHIP_DAMPING = 0.45;
    static final double WHIP_STEP = 0.1;
    /** 4.1.10: the most the wing bends root to tip in the whip (degrees). */
    static final float WHIP_MOST = 55.0F;

    /**
     * 4.1.11 (the owner: "You can make the wing stretch further forward, because it currently just feels like a little chicken wings, bro.
     * Whenever they stretch to slap something, they can stretch to be larger in size and length as well. You don't have to be limited by
     * such small range."): how far out a slamming wing is lashed (0..1): shooting out through the drive, all the way at the impact, held
     * through the overshoot, drawn back in through the recoil and a little after.
     */
    public static float lash(Motion motion, float progress, float impact) {
        float[] at = beats(blow(motion), impact);
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        if (p < at[1]) {
            return 0.0F;
        }
        if (p < at[2]) {
            return smooth((p - at[1]) / (at[2] - at[1]));
        }
        if (p < at[3]) {
            return 1.0F;
        }
        float back = at[4] + LASH_BACK * (1.0F - at[4]);
        return 1.0F - smooth((p - at[3]) / (back - at[3]));
    }

    /** 4.1.11: how far past the recoil's end (a share of what is left of the move) the lashed wing is drawn back in. */
    static final float LASH_BACK = 0.45F;

    /**
     * 4.1.11: how many times its own length a slamming wing grows to at the impact: as far as the slam hits ({@link UkakuRules#slamReach}),
     * its tip on it, at most {@link #STRETCH_MOST} (the smallest wings stop short of it, never a thread).
     */
    public static float stretchMost(Motion motion, int mastery, float length) {
        double reach = UkakuRules.slamReach(mastery, blow(motion).rangeMul());
        return (float)Math.max(1.0, Math.min(STRETCH_MOST, reach / (TIP_SHARE * Math.max(0.5, length))));
    }

    public static final float STRETCH_MOST = 3.2F;
    /** 4.1.11: how far from its shoulder blade a lashed wing's furthest flame tip lies (its fire streaming), in its lengths (measured: UkakuTests). */
    public static final float TIP_SHARE = 1.4F;

    /** 4.1.11: how much of the way the lashed tip points: level ahead (a little up), up through the target for the rising wing (degrees). */
    static float lashElevation(Motion motion) {
        return switch (motion) {
            case RISE_RIGHT, RISE_LEFT -> 22.0F;
            default -> 5.0F;
        };
    }

    /** 4.1.11: the feathers of a lashed wing folded in tight (a long tongue of fire, not a sheet); of the turn to the lash, the share at the root. */
    static final float LASH_FOLD = 0.78F;
    /** 4.1.11: how much of the whip's trail the lash's heading keeps (the rest is where the blow is: its tip lands on the impact). */
    static final float LASH_TRAIL = 0.3F;
    static final float LASH_TURN = 0.4F;
    /** 4.1.11: the most the lashed wing bends root to tip (degrees). */
    static final float LASH_MOST = 120.0F;

    /**
     * 4.1.10: a slamming twin wing's frame: placed at its root angles, its fan folded ({@link #slamFold}), and everything past the first
     * {@link #AIM_STRAIGHT} blocks curled toward where its tip points ({@link #tip}), a smooth arc root to tip, as a whip bends. 4.1.11: and
     * lashed out ({@link #lash}): grown to {@link #stretchMost} its length, its tip pulled level toward where it sweeps (or up, rising),
     * turned part of the way at the root and curled the rest, its feathers folded in tight but for the flare of the hit.
     */
    static UkakuModel.Frame slamFrame(int side, double rootX, double rootY, double rootZ, double length, Motion motion, float progress, float impact,
                                      WingAngles root, int mastery) {
        float[] tipAngles = tip(motion, progress, impact);
        float lash = lash(motion, progress, impact);
        // 4.1.12: the whip fades out after the recoil, so a long move ends on the wing's own shape whatever the spring is still doing.
        float[] beat = beats(blow(motion), impact);
        float settled = 1.0F - smooth((Math.max(0.0F, Math.min(1.0F, progress)) - beat[4]) / (1.0F - beat[4]));
        tipAngles = new float[] {root.sweep() + (tipAngles[0] - root.sweep()) * settled, root.roll() + (tipAngles[1] - root.roll()) * settled};
        double[][] axes = twinAxes(side, root.sweep(), root.roll());
        double[] out = axes[0];
        double[] up = axes[1];
        double[] back = mul(cross(axes[1], axes[0]), side);
        double[] main = normalize(add(mul(out, Math.cos(FAN_CENTRE)), mul(up, Math.sin(FAN_CENTRE))));
        double[][] tipAxes = twinAxes(side, tipAngles[0], tipAngles[1]);
        double[] whip = normalize(add(mul(tipAxes[0], Math.cos(FAN_CENTRE)), mul(tipAxes[1], Math.sin(FAN_CENTRE))));
        // The lash: level (or rising) along where the blow sweeps (the root's, trailed a little by the whip).
        float sweepTo = root.sweep() + LASH_TRAIL * (tipAngles[0] - root.sweep());
        double[] heading = twinAxes(side, sweepTo, 0.0F)[0];
        double flat = Math.sqrt(heading[0] * heading[0] + heading[2] * heading[2]);
        double[] target = whip;
        if (lash > 0.0F && flat > 1.0E-6) {
            double e = Math.toRadians(lashElevation(motion));
            double[] level = {heading[0] / flat * Math.cos(e), Math.sin(e), heading[2] / flat * Math.cos(e)};
            target = normalize(add(mul(whip, 1.0 - lash), mul(level, lash)));
        }
        double total = Math.acos(Math.max(-1.0, Math.min(1.0, dot(main, target))));
        double[] turnAxis = cross(main, target);
        if (lash > 0.0F && dot(turnAxis, turnAxis) > 1.0E-12) {
            turnAxis = normalize(turnAxis);
            double turn = total * LASH_TURN * lash;
            out = rotate(out, turnAxis, turn);
            up = rotate(up, turnAxis, turn);
            back = rotate(back, turnAxis, turn);
            main = normalize(rotate(main, turnAxis, turn));
        }
        double most = Math.toRadians(WHIP_MOST + (LASH_MOST - WHIP_MOST) * lash);
        double curl = Math.min(most, Math.acos(Math.max(-1.0, Math.min(1.0, dot(main, target)))));
        double[] toward = sub(target, mul(main, dot(target, main)));
        float flare = slamFold(motion, progress, impact);
        float fold = flare + (LASH_FOLD - flare) * lash + (flare < 0.0F ? flare * lash : 0.0F);
        if (dot(toward, toward) < 1.0E-8 || curl < 1.0E-3) {
            toward = up;
            curl = 0.0;
        }
        double scale = length * (1.0 + (stretchMost(motion, mastery, (float)length) - 1.0) * lash);
        UkakuModel.Bend bend = new UkakuModel.Bend(fold, FAN_CENTRE, main, normalize(toward), AIM_STRAIGHT, curl, scale);
        return new UkakuModel.Frame(new double[] {rootX, rootY, rootZ}, out, up, back, scale, bend);
    }

    /** Twin-wing angles for one side (+1 right, -1 left). */
    static WingAngles twinAngles(Input in, int side) {
        float sweep = REST_SWEEP;
        float roll = 0.0F;
        float window = (float)Math.sin(Math.PI * Math.max(0.0F, Math.min(1.0F, in.progress())));
        switch (in.motion()) {
            case KICK -> {
                sweep += 13.0F * window;
                roll += 12.0F * window;
            }
            case SLASH_RIGHT -> {
                if (side > 0) {
                    WingAngles slash = slash(in.progress(), in.impact());
                    sweep = slash.sweep();
                    roll = slash.roll();
                }
            }
            case SLASH_LEFT -> {
                if (side < 0) {
                    WingAngles slash = slash(in.progress(), in.impact());
                    sweep = slash.sweep();
                    roll = slash.roll();
                }
            }
            case CROSS -> {
                WingAngles slash = slash(in.progress(), in.impact());
                sweep = slash.sweep();
                roll = slash.roll();
            }
            case DASH -> {
                // Swept far back and a little down: two jets streaming behind him.
                float hold = smooth(in.progress() / 0.15F) * (1.0F - smooth((in.progress() - 0.7F) / 0.3F));
                sweep = lerp(REST_SWEEP, 78.0F, hold);
                roll = lerp(0.0F, -18.0F, hold);
            }
            case HOP -> {
                // Raised, then one hard downbeat forward that throws him back, then settle.
                float[] ph = phases(in.progress(), in.impact());
                sweep = lerp(lerp(lerp(REST_SWEEP, 40.0F, ph[0]), -35.0F, ph[1]), REST_SWEEP, ph[2]);
                roll = lerp(lerp(lerp(0.0F, 38.0F, ph[0]), -30.0F, ph[1]), 0.0F, ph[2]);
            }
            case FLIP -> {
                // Folded in tight through the flip (the first third), spread again for the barrage.
                float fold = smooth(in.progress() / 0.08F) * (1.0F - smooth((in.progress() - 0.3F) / 0.12F));
                sweep = lerp(REST_SWEEP, 82.0F, fold);
                roll = lerp(0.0F, 30.0F, fold);
            }
            case RISE -> {
                // Low and forward, then sweeping up past the head as the pillar goes up.
                float[] ph = phases(in.progress(), in.impact());
                sweep = lerp(lerp(lerp(REST_SWEEP, -20.0F, ph[0]), 5.0F, ph[1]), REST_SWEEP, ph[2]);
                roll = lerp(lerp(lerp(0.0F, -35.0F, ph[0]), 50.0F, ph[1]), 0.0F, ph[2]);
            }
            case BURST -> {
                float[] ph = phases(in.progress(), in.impact());
                sweep = lerp(lerp(lerp(REST_SWEEP, 40.0F, ph[0]), -15.0F, ph[1]), REST_SWEEP, ph[2]);
                roll = lerp(lerp(lerp(0.0F, 15.0F, ph[0]), -8.0F, ph[1]), 0.0F, ph[2]);
            }
            case SLAP_RIGHT, SLAP_LEFT, SWING_RIGHT, SWING_LEFT, RISE_RIGHT, RISE_LEFT, SLAM_RIGHT, SLAM_LEFT -> {
                if (side == striking(in.motion())) {
                    WingAngles blow = slam(in.motion(), in.progress(), in.impact());
                    sweep = blow.sweep();
                    roll = blow.roll();
                } else {
                    // 4.1.10: the other wing pulls back as the striking one drives (a fighter's other hand to the hip), ready for its turn.
                    float[] pull = counterPull(in.motion(), in.progress(), in.impact());
                    sweep += pull[0];
                    roll += pull[1];
                }
            }
            case TWIN_SLAM -> {
                WingAngles blow = slam(Motion.TWIN_SLAM, in.progress(), in.impact());
                sweep = blow.sweep();
                roll = blow.roll();
            }
            case GUARD -> {
                // Grown and swung forward round him till they meet in front of his chest, raised a little, overlapping. 4.1.10: flared open and
                // back first (the chamber), slammed shut (the drive), closing a little past where they hold (the overshoot) and easing open
                // to it (the recoil): by the guard's amount (RinkakuBlock.amount), so lowered it plays back the other way.
                WingAngles guard = guard(in.progress());
                sweep = guard.sweep();
                roll = guard.roll();
            }
            default -> {
            }
        }
        float volley = Math.max(0.0F, Math.min(1.0F, in.volley()));
        if (volley > 0.0F) {
            sweep = lerp(sweep, VOLLEY_SWEEP + 10.0F * in.recoil(), volley);
            roll = lerp(roll, VOLLEY_ROLL, volley);
        }
        return new WingAngles(sweep, roll);
    }

    /** {yaw, pitch} of the One-Winged Kagune, degrees: yaw swings the blade to the left (+) or right (-), pitch raises it. */
    static float[] oneWingAngles(Input in) {
        float yaw = 0.0F;
        float pitch = ONE_WING_REST_PITCH;
        float[] ph = phases(in.progress(), in.impact());
        switch (in.motion()) {
            case ONE_SLASH -> {
                yaw = lerp(lerp(lerp(0.0F, 70.0F, ph[0]), -85.0F, ph[1]), 0.0F, ph[2]);
                pitch = lerp(lerp(lerp(ONE_WING_REST_PITCH, 10.0F, ph[0]), -12.0F, ph[1]), ONE_WING_REST_PITCH, ph[2]);
            }
            case ONE_RETURN -> {
                yaw = lerp(lerp(lerp(0.0F, -60.0F, ph[0]), 80.0F, ph[1]), 0.0F, ph[2]);
                pitch = lerp(lerp(lerp(ONE_WING_REST_PITCH, 12.0F, ph[0]), -10.0F, ph[1]), ONE_WING_REST_PITCH, ph[2]);
            }
            case ONE_BEHEAD -> {
                yaw = lerp(lerp(lerp(0.0F, 40.0F, ph[0]), -100.0F, ph[1]), 0.0F, ph[2]);
                pitch = lerp(lerp(lerp(ONE_WING_REST_PITCH, 70.0F, ph[0]), -30.0F, ph[1]), ONE_WING_REST_PITCH, ph[2]);
            }
            default -> {
            }
        }
        float volley = Math.max(0.0F, Math.min(1.0F, in.volley()));
        pitch = lerp(pitch, 15.0F - 8.0F * in.recoil(), volley);
        return new float[] {yaw, pitch};
    }

    /** All quads for one frame, in player-local blocks, seen from an unknown camera. */
    public static List<Quad> build(Input in) {
        return build(in, null);
    }

    /**
     * All quads for one frame, in player-local blocks. {@code eye} is the camera in the same space (or null): the
     * glowing gas volumes are shaded by how squarely each part faces it.
     */
    public static List<Quad> build(Input in, double[] eye) {
        List<Quad> out = new ArrayList<>();
        float deploy = Math.max(0.0F, Math.min(1.0F, in.deploy()));
        if (deploy <= 0.001F || in.alpha() <= 0.003F) {
            return out;
        }
        UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> out.add(new Quad(new float[] {
            (float)a[0], (float)a[1], (float)a[2], (float)b[0], (float)b[1], (float)b[2], (float)c[0], (float)c[1], (float)c[2],
            (float)d[0], (float)d[1], (float)d[2]}, (float)n[0], (float)n[1], (float)n[2], new int[] {ca, cb, cc, cd}));
        int mastery = Math.max(0, in.mastery());
        WingForm form = in.form().usable(mastery);
        double rootY = ROOT_Y - (in.crouching() ? CROUCH_DROP : 0.0);
        double rootZ = ROOT_Z - (in.crouching() ? 0.05 : 0.0);
        boolean reEra = UkakuStage.forMastery(mastery).reEra();
        float recoil = Math.max(0.0F, Math.min(1.0F, in.recoil())) * Math.max(0.0F, Math.min(1.0F, in.volley()));
        if (form.kakuja()) {
            return KakujaRig.build(in, eye, KakujaRig.Body.STILL, KakujaRig.Action.NONE, null, form.finalKakuja());
        }
        if (form == WingForm.ONE_WING) {
            float[] angles = oneWingAngles(in);
            float scale = UkakuRules.wingLength(mastery, form);
            UkakuModel.oneWing(sink, oneWingFrame(-0.2, rootY + 0.12, rootZ + 0.01, scale, angles[0], angles[1]),
                new UkakuModel.Params(in.time(), 1.0F, deploy, in.dissolving(), true, 0.0F, 0.0F, in.detail(), in.alpha(), 0, -1,
                    in.volley(), 0.0F, mastery, eye));
            return out;
        }
        float length = UkakuRules.wingLength(mastery, form);
        if (in.motion() == Motion.GUARD) {
            // 4.1.2 (the owner: "wings grow and protect the player"): the guarding wings grow as they close.
            length *= 1.0F + GUARD_GROWTH * smooth(in.progress());
        }
        float[] ph = phases(in.progress(), in.impact());
        float strike = ph[0] * (1.0F - ph[2]);
        float kick = kick(in);
        for (int side = -1; side <= 1; side += 2) {
            if (side > 0 && !form.rightWing()) {
                continue;
            }
            float crystal = side > 0 ? in.crystalRight() : in.crystalLeft();
            float grow = Math.max(0.0F, Math.min(1.0F, side > 0 ? in.growRight() : in.growLeft()));
            if (grow <= 0.001F) {
                continue; // thrown as a Wing Vortex: nothing on this shoulder blade until the new wing grows
            }
            float coil = Math.max(0.0F, Math.min(1.0F, side > 0 ? in.coilRight() : in.coilLeft()));
            WingAngles angles = twinAngles(in, side);
            if (coil > 0.0F) {
                // Twirling up to be thrown: swung forward over the shoulder.
                float c = smooth(coil);
                angles = new WingAngles(lerp(angles.sweep(), VORTEX_SWEEP, c), lerp(angles.roll(), VORTEX_ROLL, c));
            }
            boolean slamming = in.motion() == Motion.TWIN_SLAM || striking(in.motion()) == side;
            boolean cutting = in.motion() == Motion.CROSS || in.motion() == (side > 0 ? Motion.SLASH_RIGHT : Motion.SLASH_LEFT)
                || slamming; // 4.1.2: a slamming wing hardens as a blade's does
            // A single wing (one small wing, Rabbit Guise) answers both hands.
            float aim = form.rightWing() ? (side > 0 ? in.aimRight() : in.aimLeft()) : Math.max(in.aimRight(), in.aimLeft());
            float shot = form.rightWing() ? (side > 0 ? in.kickRight() : in.kickLeft()) : Math.max(in.kickRight(), in.kickLeft());
            UkakuModel.Frame frame = in.motion() == Motion.GUARD && coil <= 0.0F
                ? guardFrame(side, ROOT_X * side, rootY, rootZ, length, angles, in.progress()) // 4.1.13: clamped together in front, like hands
                : aim > 0.001F && coil <= 0.0F
                ? aimFrame(side, ROOT_X * side, rootY, rootZ, length, angles.sweep(), angles.roll(), in.aim(), aim, shot)
                : slamming && coil <= 0.0F && in.volley() <= 0.0F
                ? slamFrame(side, ROOT_X * side, rootY, rootZ, length, in.motion(), in.progress(), in.impact(), angles, mastery) // 4.1.10: the whip; 4.1.11: the lash
                : twinFrame(side, ROOT_X * side, rootY, rootZ, length, angles.sweep(), angles.roll());
            if (coil > 0.0F) {
                frame = twirl(frame, side, coil);
            } else if (aim > 0.001F && in.barrel() != 0.0F) {
                // The Gatling: the aiming wing shudders about its own centre line with the stream.
                frame = spin(frame, side * in.barrel() * Math.min(1.0F, aim));
            }
            // A shot only swells the flame a little: the full thrust streams the tips back, through the curl of an aiming wing.
            float thrust = Math.max(Math.max(kick, AIM_SHOT_THRUST * Math.min(1.0F, shot)), coil);
            float hard = strike;
            if (slamming) {
                // 4.1.10: the fire streams off it through the drive; it hardens from the chamber to the end of its recoil.
                Motion blow = in.motion() == Motion.TWIN_SLAM ? Motion.TWIN_SLAM : in.motion();
                thrust = Math.max(thrust, slamThrust(blow, in.progress(), in.impact()));
                hard = slamHard(blow, in.progress(), in.impact());
            }
            // A new wing grows out of the shoulder blade the way the kagune first comes out.
            float wingDeploy = deploy * grow;
            UkakuModel.wing(sink, frame, new UkakuModel.Params(in.time() + (side > 0 ? 0.0 : 1.7), crystal, wingDeploy, in.dissolving(), reEra,
                cutting ? hard : 0.0F, recoil, in.detail(), in.alpha(), side > 0 ? 1 : 2, side, in.volley(), thrust, mastery, eye));
        }
        return out;
    }

    /** Pose of a wing twirling up to be thrown (Wing Vortex): swept forward over the shoulder, rolled up. */
    static final float VORTEX_SWEEP = -40.0F;
    static final float VORTEX_ROLL = 20.0F;
    /** Turns the wing spins about its own centre line as it coils to be thrown. */
    static final double VORTEX_TURNS = 1.25;

    /**
     * A wing coiling to be thrown ({@code coil} 0..1): its fan turned about its own centre line, faster and faster, like a
     * drill winding up, and drawn in a little.
     */
    static UkakuModel.Frame twirl(UkakuModel.Frame frame, int side, float coil) {
        double c = Math.max(0.0, Math.min(1.0, coil));
        double[] main = normalize(add(mul(frame.out(), Math.cos(FAN_CENTRE)), mul(frame.up(), Math.sin(FAN_CENTRE))));
        double angle = side * Math.PI * 2.0 * VORTEX_TURNS * c * c;
        double[] out = rotate(frame.out(), main, angle);
        double[] up = rotate(frame.up(), main, angle);
        double[] back = rotate(frame.back(), main, angle);
        return new UkakuModel.Frame(frame.origin(), out, up, back, frame.scale() * (1.0 - 0.15 * c), frame.bend());
    }

    /** A wing's fan turned {@code angle} radians about its own centre line (the Gatling's shudder; keep it small, a curled wing squashes as it turns). */
    static UkakuModel.Frame spin(UkakuModel.Frame frame, double angle) {
        double[] main = normalize(add(mul(frame.out(), Math.cos(FAN_CENTRE)), mul(frame.up(), Math.sin(FAN_CENTRE))));
        return new UkakuModel.Frame(frame.origin(), rotate(frame.out(), main, angle), rotate(frame.up(), main, angle), rotate(frame.back(), main, angle),
            frame.scale(), frame.bend());
    }

    /** 0..1 thrust of the gas wings during a kick or a burst: builds through the windup, peaks just after impact, fades. */
    static float kick(Input in) {
        if (in.motion() == Motion.DASH) {
            return 1.0F;
        }
        if (in.motion() != Motion.KICK && in.motion() != Motion.BURST && in.motion() != Motion.HOP) {
            return 0.0F;
        }
        float[] ph = phases(in.progress(), in.impact());
        return Math.max(0.0F, Math.min(1.0F, 0.45F * ph[0] + 0.55F * ph[1] - ph[2]));
    }

    /** The placed frame of a twin wing: outward, up and dorsal (behind the wing) axes from the pose angles. */
    static UkakuModel.Frame twinFrame(int side, double rootX, double rootY, double rootZ, double length, float sweep, float roll) {
        double[][] axes = twinAxes(side, sweep, roll);
        double[] cross = cross(axes[1], axes[0]);
        double[] back = {cross[0] * side, cross[1] * side, cross[2] * side};
        return new UkakuModel.Frame(new double[] {rootX, rootY, rootZ}, axes[0], axes[1], back, length);
    }

    /** The placed frame of the One-Winged Kagune: {@code out} along the blade's plane forward, {@code up} its back edge. */
    static UkakuModel.Frame oneWingFrame(double rootX, double rootY, double rootZ, double scale, float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double[] forward = {-Math.sin(yaw), 0.0, Math.cos(yaw)};
        // Pitch rotates the crescent in its own plane: u (forward) and v (up) turn together.
        double[] u = {forward[0] * Math.cos(pitch), Math.sin(pitch), forward[2] * Math.cos(pitch)};
        double[] v = {-forward[0] * Math.sin(pitch), Math.cos(pitch), -forward[2] * Math.sin(pitch)};
        return new UkakuModel.Frame(new double[] {rootX, rootY, rootZ}, u, v, cross(u, v), scale);
    }

    /**
     * Where a Shard Volley shard leaves a twin wing: the rig-local point (x right, y up from the feet, z forward) near
     * the tip of a primary feather of wing {@code side} (+1 right, -1 left) with the wings in the volley pose. {@code t}
     * (0..1) picks the feather, lowest to highest, so a wave fans out along the whole wing as in the anime.
     */
    public static double[] launchPoint(int mastery, WingForm form, int side, double t, boolean crouching) {
        int clamped = Math.max(0, mastery);
        WingForm usable = form.usable(clamped);
        float length = UkakuRules.wingLength(clamped, usable);
        boolean reEra = UkakuStage.forMastery(clamped).reEra();
        int s = side >= 0 ? 1 : -1;
        double rootY = ROOT_Y - (crouching ? CROUCH_DROP : 0.0);
        double rootZ = ROOT_Z - (crouching ? 0.05 : 0.0);
        UkakuModel.Frame frame = twinFrame(s, ROOT_X * s, rootY, rootZ, length, VOLLEY_SWEEP, VOLLEY_ROLL);
        int primaries = UkakuModel.primaries(reEra);
        int feather = (int)Math.round(Math.max(0.0, Math.min(1.0, t)) * (primaries - 1));
        double[] local = UkakuModel.launchPoint(reEra, feather, 0.85);
        return frame.at(local[0], local[1], local[2]);
    }

    /**
     * A twin wing aiming at {@code aim} (rig space, unit): the same wing, full size and full spread, bent so its tip points
     * along {@code aim}. By {@code amount} (0..1) it tips forward a little at the root ({@link #AIM_TURN}) and curls the rest
     * of the way along its length, one smooth arc from the shoulder blade to the tip. {@code kick} (0..1, just after a shot)
     * flares it: a little longer and wider, the tip thrown back a touch as the fire leaves it.
     */
    static UkakuModel.Frame aimFrame(int side, double rootX, double rootY, double rootZ, double length, float sweep, float roll, double[] aim,
                                     float amount, float kick) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        double flare = Math.max(0.0, Math.min(1.0, kick));
        double[][] axes = twinAxes(side, sweep, roll);
        double[] out = axes[0];
        double[] up = axes[1];
        double[] back = mul(cross(axes[1], axes[0]), side);
        double[] main = normalize(add(mul(out, Math.cos(FAN_CENTRE)), mul(up, Math.sin(FAN_CENTRE))));
        double[] target = normalize(aim == null ? FORWARD : aim);
        double total = Math.acos(Math.max(-1.0, Math.min(1.0, dot(main, target))));
        // Tip the whole wing part of the way toward the target at the root.
        double[] turnAxis = cross(main, target);
        if (dot(turnAxis, turnAxis) > 1.0E-12) {
            turnAxis = normalize(turnAxis);
            double turn = total * AIM_TURN * t;
            out = rotate(out, turnAxis, turn);
            up = rotate(up, turnAxis, turn);
            back = rotate(back, turnAxis, turn);
            main = normalize(rotate(main, turnAxis, turn));
        }
        // Curl the rest of the way along the wing, in the plane of its main line and the target.
        double[] toward = sub(target, mul(main, dot(target, main)));
        if (dot(toward, toward) < 1.0E-8) {
            toward = sub(FORWARD, mul(main, dot(FORWARD, main)));
        }
        toward = normalize(toward);
        double curl = Math.acos(Math.max(-1.0, Math.min(1.0, dot(main, target)))) * t - 0.12 * flare;
        double scale = length * (1.0 + AIM_FLARE * flare);
        UkakuModel.Bend bend = new UkakuModel.Bend(-0.12 * flare, FAN_CENTRE, main, toward, AIM_STRAIGHT, curl, scale);
        return new UkakuModel.Frame(new double[] {rootX, rootY, rootZ}, out, up, back, scale, bend);
    }

    /**
     * Where a shot leaves wing {@code side} (+1 right, -1 left; one-wing forms use the left) aiming fully at {@code aim}:
     * {x, y, z} of the tip of the curled wing in rig space (x right, y up from the feet, z forward). The fire flies along the
     * aim from here, out of the point the wing is drawn pointing with.
     */
    public static double[] aimTip(int mastery, WingForm form, int side, double[] aim, boolean crouching) {
        int clamped = Math.max(0, mastery);
        WingForm usable = form.usable(clamped);
        int s = side >= 0 && usable.rightWing() ? 1 : -1;
        double rootY = ROOT_Y - (crouching ? CROUCH_DROP : 0.0);
        double rootZ = ROOT_Z - (crouching ? 0.05 : 0.0);
        UkakuModel.Frame frame = aimFrame(s, ROOT_X * s, rootY, rootZ, UkakuRules.wingLength(clamped, usable), REST_SWEEP, 0.0F, aim, 1.0F,
            0.0F);
        return frame.at(MUZZLE * Math.cos(FAN_CENTRE), MUZZLE * Math.sin(FAN_CENTRE), 0.0);
    }

    /** Axes of a twin wing: {outward, up} after roll, the backward tilt and the sweep about the vertical. */
    static double[][] twinAxes(int side, float sweepDeg, float rollDeg) {
        double roll = Math.toRadians(rollDeg);
        double[] outward = {side * Math.cos(roll), Math.sin(roll), 0.0};
        double[] up = {-side * Math.sin(roll), Math.cos(roll), 0.0};
        double tilt = Math.toRadians(-8.0);
        outward = rotateX(outward, tilt);
        up = rotateX(up, tilt);
        double sweep = Math.toRadians(side * sweepDeg);
        return new double[][] {rotateY(outward, sweep), rotateY(up, sweep)};
    }

    private static double[] rotateX(double[] v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new double[] {v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c};
    }

    private static double[] rotateY(double[] v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new double[] {v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / length);
    }

    /** {@code v} turned about unit {@code k} by {@code angle} (right-handed). */
    private static double[] rotate(double[] v, double[] k, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double[] kxv = {k[1] * v[2] - k[2] * v[1], k[2] * v[0] - k[0] * v[2], k[0] * v[1] - k[1] * v[0]};
        double kv = dot(k, v);
        return new double[] {v[0] * c + kxv[0] * s + k[0] * kv * (1 - c), v[1] * c + kxv[1] * s + k[1] * kv * (1 - c),
            v[2] * c + kxv[2] * s + k[2] * kv * (1 - c)};
    }

    private static double[] cross(double[] a, double[] b) {
        double x = a[1] * b[2] - a[2] * b[1];
        double y = a[2] * b[0] - a[0] * b[2];
        double z = a[0] * b[1] - a[1] * b[0];
        double len = Math.max(1.0E-9, Math.sqrt(x * x + y * y + z * z));
        return new double[] {x / len, y / len, z / len};
    }
}
