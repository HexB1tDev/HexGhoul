package com.hex.kagune.control.ukaku;

import java.util.ArrayList;
import java.util.List;

/**
 * Poses Ukaku's first kakuja in 3D (pure, tested): six armored tentacles ({@link KakujaModel}), three from each shoulder
 * blade. Player-local blocks like {@link WingRig}: x to the player's right, y up from the feet, z forward.
 *
 * <p>At rest they spread like the anime's armored wings and frame him like the kakuja art: the upper pair rises high over
 * the shoulders and arches out and down, the middle pair reaches out to the sides like wings, the lower pair sweeps back
 * and low and hooks forward at the claws like pincers. Plates on top, the spike rows on the belly facing down and in.
 *
 * <p>They are alive. Each tentacle writhes on its own slow, never-repeating rhythm (several drifting sways at once, a wave
 * running out to the claw), now and then stretches out long and curls back in with its spikes bristling, and the spikes
 * ripple. They answer the body: they ride the torso as it leans and turns ({@link Body}), stream back as he runs and bob
 * with his stride, tuck in on the way up a jump and spread like wings on the way down, and fold close when he crouches.
 * On top of this designed pose the client runs a spring chain per tentacle ({@link KakujaMotion}) whose lag is handed back
 * here as offsets, so they trail, swing and whip like living things.
 */
public final class KakujaRig {
    public static final int TENTACLES = 6;
    /** Tiers from top to bottom. */
    public static final int UPPER = 0;
    public static final int MIDDLE = 1;
    public static final int LOWER = 2;
    /** Points per tentacle the spring chain follows (and the root). */
    public static final int NODES = 6;

    /**
     * Rest layout of the right side's three tentacles (the left mirrors it), one row per tier: root x, y, z (blocks, a
     * standing player), elevation and sweep back (degrees), roll of the plates outward (degrees), length and root half-width
     * (blocks), curl toward the belly and curl forward over the length (degrees).
     */
    static final double[][] REST = {
        {0.11, 1.4, -0.2, 54.0, 24.0, 28.0, 3.7, 0.34, 104.0, 39.0},
        {0.18, 1.22, -0.21, 31.5, 26.5, 6.5, 3.3, 0.31, 73.0, 32.5},
        {0.14, 1.04, -0.2, 6.5, 35.5, -4.0, 2.9, 0.28, 20.5, 65.0}};
    static final int ROOT_X = 0;
    static final int ROOT_Y = 1;
    static final int ROOT_Z = 2;
    static final int ELEVATION = 3;
    static final int SWEEP = 4;
    static final int ROLL = 5;
    static final int LENGTH = 6;
    static final int WIDTH = 7;
    static final int CURL_DOWN = 8;
    static final int CURL_FORWARD = 9;
    /** Spine samples per tentacle. */
    static final int SAMPLES = 32;
    /** The upper body leans about this height above the feet: the hips (5.0.19; it was mid-torso while it leaned by bendy-lib). */
    static final double BEND_PIVOT = 1.501 - 12.0 / 16.0;
    /**
     * The ground under a standing body (y 0): no part of a tentacle's centre line goes below this height (blocks), the
     * claw's at the tip, more toward the root (the spike rows hang below the centre line); a tentacle that would go lower is
     * pressed along the floor instead, softly over {@link #FLOOR_SOFT}.
     */
    static final double FLOOR_TIP = 0.16;
    static final double FLOOR_ROOT = 0.5;
    static final double FLOOR_SOFT = 0.1;
    /** Each tier comes out this much later in the deploy (the upper pair first), the left a touch after the right. */
    static final float DEPLOY_STAGGER = 0.1F;

    /**
     * What the body is doing, for the tentacles to ride and answer.
     *
     * @param lean     the upper body's bend forward (radians, the body layer's lean), {@code side} sideways, {@code twist} the chest's turn
     * @param drop     how far the body is lowered (blocks)
     * @param run      0 standing .. 1 at a full run
     * @param stride   the walk cycle's phase (radians)
     * @param vertical vertical speed (blocks a tick, + up), meaningful while {@code airborne}
     */
    public record Body(float lean, float side, float twist, float drop, float run, float stride, float vertical, boolean airborne) {
        public static final Body STILL = new Body(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false);
    }

    /**
     * One tentacle's pose this frame: angles in degrees and length in blocks as offsets on top of {@link #REST}, its own
     * deploy (0..1), the wave running out along it, its spikes (scale, and how hard a ripple runs along them), and how far
     * (degrees) it is turned about its own line toward its leading edge ({@code aimRoll}: at -90 its spike rows face forward,
     * the way he fires).
     */
    public record Limb(int side, int tier, double elevation, double sweep, double roll, double curlDown, double curlForward, double length,
                       double wave, double wavePhase, double deploy, double spikes, double bristle, double bristlePhase, double aimRoll) {
        public Limb(int side, int tier, double elevation, double sweep, double roll, double curlDown, double curlForward, double length, double wave,
                    double wavePhase, double deploy, double spikes, double bristle, double bristlePhase) {
            this(side, tier, elevation, sweep, roll, curlDown, curlForward, length, wave, wavePhase, deploy, spikes, bristle, bristlePhase, 0.0);
        }

        Limb plus(double elevation, double sweep, double roll, double curlDown, double curlForward, double length) {
            return new Limb(this.side, this.tier, this.elevation + elevation, this.sweep + sweep, this.roll + roll, this.curlDown + curlDown,
                this.curlForward + curlForward, this.length + length, this.wave, this.wavePhase, this.deploy, this.spikes, this.bristle,
                this.bristlePhase, this.aimRoll);
        }
    }

    /**
     * What the tentacles are doing for a move this frame: where he is looking ({@code aim}, rig space, unit), how far each
     * tentacle (by {@link #index}) is turned to fire ({@code amount}, 0..1), the kick of a needle just leaving it
     * ({@code kick}, 0..1), how far all six are pulled in tight against his back ({@code tuck}, 0..1: through a flip), and where
     * the left three aim when it isn't where the right three do ({@code aimLeft}, null: the same; the final kakuja's fire
     * tendrils sweep and beat on their own).
     */
    public record Action(double[] aim, float[] amount, float[] kick, float tuck, double[] aimLeft) {
        public static final Action NONE = new Action(WingRig.FORWARD, new float[TENTACLES], new float[TENTACLES], 0.0F);

        public Action(double[] aim, float[] amount, float[] kick) {
            this(aim, amount, kick, 0.0F, null);
        }

        public Action(double[] aim, float[] amount, float[] kick, float tuck) {
            this(aim, amount, kick, tuck, null);
        }

        /** Where tentacle {@code index} aims. */
        double[] aimOf(int index) {
            return sideOf(index) < 0 && this.aimLeft != null ? this.aimLeft : this.aim;
        }

        float amount(int index) {
            return this.amount == null || index >= this.amount.length ? 0.0F : this.amount[index];
        }

        float kick(int index) {
            return this.kick == null || index >= this.kick.length ? 0.0F : this.kick[index];
        }
    }

    private KakujaRig() {
    }

    /** The tentacle's index (0..5): right upper, middle, lower, then the left's. */
    public static int index(int side, int tier) {
        return (side > 0 ? 0 : 3) + tier;
    }

    public static int sideOf(int index) {
        return index < 3 ? 1 : -1;
    }

    public static int tierOf(int index) {
        return index % 3;
    }

    /** Root of a tentacle in torso pixels (x to the player's left, y down from the neck, z backward), where its socket goes. */
    public static double[] rootPixels(int side, int tier) {
        double[] rest = REST[tier];
        return new double[] {-side * rest[ROOT_X] * 16.0, (1.501 - rest[ROOT_Y]) * 16.0};
    }

    // ------------------------------------------------------------------ building

    /** All quads for one frame, a still body and no spring lag (tests, previews). */
    public static List<WingRig.Quad> build(WingRig.Input in, double[] eye) {
        return build(in, eye, Body.STILL, null);
    }

    /** All quads for one frame: the body to ride, and the spring chain's lag ({@link #offsets}, rig space; null for none). */
    public static List<WingRig.Quad> build(WingRig.Input in, double[] eye, Body body, double[][][] offsets) {
        List<WingRig.Quad> out = new ArrayList<>();
        if (in.deploy() <= 0.001F || in.alpha() <= 0.003F) {
            return out;
        }
        return build(in, eye, body, Action.NONE, offsets);
    }

    /** All quads for one frame, the tentacles also turning to fire as {@code action} says. */
    public static List<WingRig.Quad> build(WingRig.Input in, double[] eye, Body body, Action action, double[][][] offsets) {
        return build(in, eye, body, action, offsets, false);
    }

    /** {@link #build(WingRig.Input, double[], Body, Action, double[][][])}; {@code finalForm} draws the final kakuja. */
    public static List<WingRig.Quad> build(WingRig.Input in, double[] eye, Body body, Action action, double[][][] offsets, boolean finalForm) {
        List<WingRig.Quad> out = new ArrayList<>();
        if (in.deploy() <= 0.001F || in.alpha() <= 0.003F) {
            return out;
        }
        Body b = body == null ? Body.STILL : body;
        Limb[] limbs = pose(in, b, action);
        KakujaModel.Spine[] spines = spines(in, b, limbs);
        draw(out, in, spines, limbs, offsets, !b.airborne(), eye, finalForm);
        return out;
    }

    /**
     * Draws already placed spines, each bent by its spring lag; {@code grounded} keeps them off the floor under his feet (in
     * the air there is no floor at his feet to keep them off).
     */
    public static void draw(List<WingRig.Quad> out, WingRig.Input in, KakujaModel.Spine[] spines, Limb[] limbs, double[][][] offsets,
                            boolean grounded) {
        draw(out, in, spines, limbs, offsets, grounded, null, false);
    }

    /**
     * {@link #draw(List, WingRig.Input, KakujaModel.Spine[], Limb[], double[][][], boolean)}; with {@code finalForm} it is the
     * final kakuja: the right three stay armored spike tentacles and the left three are slim fire tendrils
     * ({@link FireTendril}) on the same spines, their gas shaded from the camera at {@code eye} (rig space; null for none).
     */
    public static void draw(List<WingRig.Quad> out, WingRig.Input in, KakujaModel.Spine[] spines, Limb[] limbs, double[][][] offsets,
                            boolean grounded, double[] eye, boolean finalForm) {
        UkakuModel.Sink sink = sink(out);
        boolean fine = in.detail() >= 0.6F;
        KakujaModel.Spine[] placed = new KakujaModel.Spine[TENTACLES];
        for (int i = 0; i < TENTACLES; i++) {
            if (spines[i] != null) {
                placed[i] = offsets == null || offsets[i] == null ? spines[i] : offset(spines[i], limbs[i].side(), offsets[i]);
            }
        }
        // Never through each other (after the spring lag, which moves each on its own), then never through the floor.
        separate(placed, limbs);
        for (int i = 0; i < TENTACLES; i++) {
            if (placed[i] == null) {
                continue;
            }
            Limb limb = limbs[i];
            KakujaModel.Spine spine = grounded ? floor(placed[i], limb.side()) : placed[i];
            double width = REST[limb.tier()][WIDTH] * widthOut(limb.deploy());
            if (finalForm && limb.side() < 0) {
                FireTendril.tendril(sink, spine, new KakujaModel.Look(width, 0, 0, fine, in.alpha(), in.time(), i + 1, 0.0), eye);
                continue;
            }
            // Spikes sprout last, once the plates are out.
            double spikes = spikesOut(limb.deploy()) * limb.spikes();
            KakujaModel.tentacle(sink, spine, new KakujaModel.Look(width, fine ? 9 : 6, fine ? 2 : 1, fine, in.alpha(), in.time(), i + 1, spikes,
                limb.bristle(), limb.bristlePhase()));
        }
    }

    // ------------------------------------------------------------------ the pose

    /** The six tentacles' poses this frame: alive at rest, answering the body, turning to fire. */
    public static Limb[] pose(WingRig.Input in, Body body) {
        return pose(in, body, Action.NONE);
    }

    /** {@link #pose(WingRig.Input, Body)} with the tentacles also turning to fire as {@code action} says. */
    public static Limb[] pose(WingRig.Input in, Body body, Action action) {
        Limb[] limbs = new Limb[TENTACLES];
        Body b = body == null ? Body.STILL : body;
        Action act = action == null ? Action.NONE : action;
        for (int i = 0; i < TENTACLES; i++) {
            int side = sideOf(i);
            int tier = tierOf(i);
            Limb limb = alive(side, tier, in.time(), deployOf(in.deploy(), side, tier));
            limb = moving(limb, b, in.crouching());
            limb = tucked(limb, act.tuck());
            limb = aiming(limb, act.amount(i), act.aimOf(i), act.kick(i));
            limbs[i] = limb;
        }
        return limbs;
    }

    /**
     * {@code limb} drawn back into his back by {@code fold} (0 as it is .. 1 all the way in): 5.0.19, the final kakuja's steel
     * tentacles folding away as they close round his arm into the Steel Blade, length, width and spikes going in together.
     */
    public static Limb folded(Limb limb, double fold) {
        double keep = Math.max(0.0, Math.min(1.0, 1.0 - fold));
        if (keep >= 1.0) {
            return limb;
        }
        return new Limb(limb.side(), limb.tier(), limb.elevation(), limb.sweep(), limb.roll(), limb.curlDown(), limb.curlForward(), limb.length(),
            limb.wave(), limb.wavePhase(), limb.deploy() * keep, limb.spikes(), limb.bristle(), limb.bristlePhase(), limb.aimRoll());
    }

    /** A tentacle's own deploy: the upper pair comes out first, then the middle, then the lower; the left a beat behind. */
    public static double deployOf(float deploy, int side, int tier) {
        double delay = DEPLOY_STAGGER * tier + (side < 0 ? 0.04 : 0.0);
        double span = 1.0 - DEPLOY_STAGGER * 2.0 - 0.04;
        return Math.max(0.0, Math.min(1.0, (deploy - delay) / span));
    }

    /** How much of a tentacle's length is out of his back at its own {@code deploy} (0..1). */
    public static double lengthOut(double deploy) {
        return UkakuModel.smooth(deploy);
    }

    /** Its width over its rest width at {@code deploy}: it swells to full as it comes out. */
    public static double widthOut(double deploy) {
        return 0.45 + 0.55 * lengthOut(deploy);
    }

    /** How far its spikes have sprouted at {@code deploy}: last, once the plates are out. */
    public static double spikesOut(double deploy) {
        return UkakuModel.smooth((deploy - 0.55) / 0.45);
    }

    /**
     * A tentacle at rest, alive: several slow sways drifting against each other (so it never visibly repeats), a wave
     * running out to the claw, the spikes rippling, and every few seconds a stretch: it reaches out long, uncurls and
     * bristles, then curls back in.
     */
    public static Limb alive(int side, int tier, double time, double deploy) {
        int i = index(side, tier);
        double[] phase = new double[10];
        for (int k = 0; k < phase.length; k++) {
            phase[k] = UkakuModel.hash(i * 37 + k * 11 + 5) * Math.PI * 2.0;
        }
        // The three on a side breathe together like one wing (the wave rippling down from the upper one); each only adds a
        // little of its own, so neighbours never sway into each other.
        double[] wing = new double[4];
        for (int k = 0; k < wing.length; k++) {
            wing[k] = UkakuModel.hash((side > 0 ? 401 : 809) + k * 13) * Math.PI * 2.0;
        }
        double t = time;
        double shared = t - WING_RIPPLE * tier;
        double elevation = 3.0 * Math.sin(0.53 * shared + wing[0]) + 1.1 * Math.sin(1.31 * t + phase[1]);
        double sweep = 3.5 * Math.sin(0.41 * shared + wing[1]) + 0.9 * Math.sin(1.07 * t + phase[3]);
        double roll = 2.5 * Math.sin(0.6 * shared + wing[2]) + 1.5 * Math.sin(0.93 * t + phase[4]);
        double curlDown = 6.0 * Math.sin(0.67 * shared + wing[3]) + 2.0 * Math.sin(1.83 * t + phase[6]);
        double curlForward = 3.0 * Math.sin(0.47 * shared + wing[1] + 1.0) + 1.0 * Math.sin(1.21 * t + phase[7]);
        // A stretch reaches out along the tentacle's own line and away from its neighbours: the upper one up, the middle one
        // straight out, the lower one out and down.
        double stretch = stretch(i, t);
        elevation += new double[] {6.0, 1.0, -5.0}[tier] * stretch;
        curlDown -= new double[] {24.0, 14.0, 6.0}[tier] * stretch;
        curlForward += 8.0 * stretch;
        double length = 0.16 * stretch;
        return new Limb(side, tier, elevation, sweep, roll, curlDown, curlForward, length, 0.13, 1.7 * t + phase[8], deploy, 1.0,
            0.05 + 0.3 * stretch, 3.0 * t + phase[9]);
    }

    /** Seconds the shared sway takes to ripple from one tier down to the next. */
    static final double WING_RIPPLE = 0.35;
    /** Seconds between a tentacle's chances to stretch, and how many of them it takes. */
    static final double STRETCH_PERIOD = 5.5;
    static final double STRETCH_CHANCE = 0.7;
    /** A stretch's length (seconds): out over the first part, held, then curled back in. */
    static final double STRETCH_LENGTH = 1.9;

    /** 0..1 how far tentacle {@code index} is through a stretch at {@code time} seconds (0 most of the time). */
    public static double stretch(int index, double time) {
        double period = STRETCH_PERIOD + 2.5 * UkakuModel.hash(index * 7 + 1);
        double shifted = time + UkakuModel.hash(index * 13 + 3) * period;
        long window = (long)Math.floor(shifted / period);
        if (UkakuModel.hash((int)(index * 1009 + window * 31)) > STRETCH_CHANCE) {
            return 0.0;
        }
        double start = UkakuModel.hash((int)(index * 2003 + window * 17)) * (period - STRETCH_LENGTH);
        double u = shifted - window * period - start;
        if (u <= 0.0 || u >= STRETCH_LENGTH) {
            return 0.0;
        }
        return UkakuModel.smooth(u / 0.55) * (1.0 - UkakuModel.smooth((u - 0.95) / (STRETCH_LENGTH - 0.95)));
    }

    /**
     * The tentacles answering the body: streaming back as he runs and bobbing with each step, tucked in on the way up a
     * jump and spread like wings on the way down, folded close while he crouches.
     */
    public static Limb moving(Limb limb, Body body, boolean crouching) {
        int tier = limb.tier();
        double run = clamp01(body.run());
        double bob = Math.sin(2.0 * body.stride() + tier * 0.6 + (limb.side() > 0 ? 0.0 : 0.4));
        // Every response moves the three on a side together, like one wing, so the spacing between them never closes.
        // Running: they stream back and dip a little, bobbing with each step.
        Limb out = limb.plus(-4.0 * run + 3.0 * run * bob, 24.0 * run, 0.0, 4.0 * run, -14.0 * run, 0.0);
        if (body.airborne()) {
            double rising = clamp01(body.vertical() / 0.42);
            double falling = clamp01(-body.vertical() / 0.5);
            // On the way up they tuck in and lift clear of the ground he leaves, on the way down they spread like wings.
            out = out.plus(8.0 * rising + 8.0 * falling, 10.0 * rising - 10.0 * falling, 4.0 * falling, 6.0 * rising - 20.0 * falling,
                -10.0 * falling, 0.0);
        }
        if (crouching) {
            // Crouching: they fold back closer over him.
            out = out.plus(-3.0, 8.0, 0.0, 6.0, 0.0, 0.0);
        }
        return out;
    }

    /** All six pulled in tight against his back ({@code tuck} 0..1): swept far back, curled close, a little shorter. */
    public static Limb tucked(Limb limb, float tuck) {
        double t = UkakuModel.smooth(clamp01(tuck));
        if (t <= 0.0) {
            return limb;
        }
        return limb.plus(-10.0 * t, 40.0 * t, 0.0, 45.0 * t, 20.0 * t, -0.5 * t);
    }

    /** The firing pose, absolute per tier: elevation, curl toward the belly (now facing forward: the tentacle cups forward). */
    static final double[] AIM_ELEVATION = {36.0, 10.0, -16.0};
    static final double[] AIM_CURL_DOWN = {58.0, 46.0, 36.0};
    static final double AIM_SWEEP = 4.0;
    static final double AIM_CURL_FORWARD = 6.0;
    /** Turned about its own line so the spike rows face forward. */
    static final double AIM_ROLL = -90.0;

    /**
     * A tentacle turning to fire ({@code amount} 0..1): it swings out to the side and forward (the side he turns toward
     * further forward), rises or dips with where he looks, rolls a quarter turn so its spike rows face where he is looking and
     * cups forward around the aim, steadier as it goes. A needle leaving it ({@code kick}) snaps it open a little and ripples
     * its spikes.
     */
    public static Limb aiming(Limb limb, float amount, double[] aim, float kick) {
        double a = UkakuModel.smooth(clamp01(amount));
        double k = clamp01(kick);
        if (a <= 0.0 && k <= 0.0) {
            return limb;
        }
        int tier = limb.tier();
        double[] rest = REST[tier];
        double[] look = normalize(aim == null ? WingRig.FORWARD : aim);
        double yaw = Math.toDegrees(Math.atan2(look[0], look[2]));
        double pitch = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, look[1]))));
        double toElevation = AIM_ELEVATION[tier] + 0.5 * pitch - rest[ELEVATION] - limb.elevation();
        double toSweep = AIM_SWEEP - limb.side() * 0.6 * yaw - rest[SWEEP] - limb.sweep();
        double toRoll = -rest[ROLL] - limb.roll();
        double toDown = AIM_CURL_DOWN[tier] - rest[CURL_DOWN] - limb.curlDown();
        double toForward = AIM_CURL_FORWARD - rest[CURL_FORWARD] - limb.curlForward();
        Limb out = limb.plus(toElevation * a + 4.0 * k, toSweep * a, toRoll * a, toDown * a - 12.0 * k, toForward * a, 0.1 * a);
        return new Limb(out.side(), out.tier(), out.elevation(), out.sweep(), out.roll(), out.curlDown(), out.curlForward(), out.length(),
            out.wave() * (1.0 - 0.6 * a), out.wavePhase(), out.deploy(), out.spikes(), out.bristle() + 0.6 * k, out.bristlePhase(),
            out.aimRoll() + AIM_ROLL * a);
    }

    // ------------------------------------------------------------------ spines

    /** Every tentacle's centre line this frame, riding the torso (null where a tentacle is not out yet). */
    public static KakujaModel.Spine[] spines(WingRig.Input in, Body body, Limb[] limbs) {
        KakujaModel.Spine[] spines = new KakujaModel.Spine[TENTACLES];
        Body b = body == null ? Body.STILL : body;
        for (int i = 0; i < TENTACLES; i++) {
            if (limbs[i].deploy() <= 0.001) {
                continue;
            }
            spines[i] = ride(spine(limbs[i], in.crouching(), lengthOut(limbs[i].deploy())), b, in.crouching());
        }
        return spines;
    }

    /**
     * The tentacle's centre line for {@code limb} on a standing body: from its root out along its elevation and sweep,
     * curling toward its belly and forward along its length (most of it toward the claw), {@code grow} of its length out.
     * A tentacle still coming out is the first {@code grow} of its own full curve (5.0.9): it pushes out of his shoulder blade
     * along its own line, straight at first (the curl is mostly toward the claw), and curls into place as the rest comes free.
     */
    public static KakujaModel.Spine spine(Limb limb, boolean crouching, double grow) {
        double[] rest = REST[limb.tier()];
        int side = limb.side() >= 0 ? 1 : -1;
        double full = Math.max(0.05, rest[LENGTH] + limb.length());
        double length = Math.max(0.05, (rest[LENGTH] + limb.length()) * grow);
        // How much of the whole curve is out: the bends below follow the full tentacle's, cut where it leaves the body.
        double share = Math.min(1.0, length / full);
        double[] root = {side * rest[ROOT_X], rest[ROOT_Y], rest[ROOT_Z] - (crouching ? 0.05 : 0.0)};
        double elevation = Math.toRadians(rest[ELEVATION] + limb.elevation());
        double sweep = Math.toRadians(rest[SWEEP] + limb.sweep());
        double[] t = {side * Math.cos(elevation) * Math.cos(sweep), Math.sin(elevation), -Math.cos(elevation) * Math.sin(sweep)};
        double[] up = {0.0, 1.0, 0.0};
        double[] n = Math.abs(t[1]) > 0.98 ? new double[] {0.0, 0.0, -1.0} : normalize(sub(up, mul(t, dot(up, t))));
        // Roll the plates outward about the tentacle's own line.
        double[] outward = {side, 0.0, 0.0};
        double[] q = sub(sub(outward, mul(t, dot(outward, t))), mul(n, dot(outward, n)));
        if (dot(q, q) > 1.0E-8) {
            q = normalize(q);
            double roll = Math.toRadians(rest[ROLL] + limb.roll());
            n = normalize(add(mul(n, Math.cos(roll)), mul(q, Math.sin(roll))));
        }
        double[] b = mul(cross(t, n), side);
        if (Math.abs(limb.aimRoll()) > 1.0E-4) {
            // Turned about its own line toward its leading edge (at -90 the belly faces the way the edge did: forward).
            double r = Math.toRadians(limb.aimRoll());
            n = normalize(add(mul(n, Math.cos(r)), mul(b, Math.sin(r))));
            b = mul(cross(t, n), side);
        }
        double down = Math.toRadians(rest[CURL_DOWN] + limb.curlDown());
        double forward = Math.toRadians(rest[CURL_FORWARD] + limb.curlForward());
        int samples = SAMPLES;
        double ds = 1.0 / samples;
        double[][] ps = new double[samples + 1][];
        double[][] ts = new double[samples + 1][];
        double[][] ns = new double[samples + 1][];
        double[][] bs = new double[samples + 1][];
        double[] p = root;
        for (int i = 0; i <= samples; i++) {
            ps[i] = p;
            ts[i] = t;
            ns[i] = n;
            bs[i] = b;
            if (i == samples) {
                break;
            }
            // Along the full tentacle (s) and the step there (du): only the share that is out is walked.
            double s = (i + 0.5) * ds * share;
            double du = ds * share;
            // Curvature grows toward the claw; the wave grows with it.
            double bendDown = down * (0.4 + 1.2 * s) * du + limb.wave() * Math.sin(2.0 * Math.PI * 0.8 * s - limb.wavePhase()) * s * du * 2.0;
            double bendForward = forward * (0.2 + 1.6 * s) * du;
            // Half the turn, the step, the other half: the centre line follows the arc.
            double[][] r = turn(t, n, b, bendDown * 0.5, bendForward * 0.5);
            p = add(p, mul(r[0], length * ds));
            r = turn(r[0], r[1], r[2], bendDown * 0.5, bendForward * 0.5);
            t = r[0];
            n = r[1];
            b = r[2];
        }
        return new KakujaModel.Spine(ps, ts, ns, bs, length);
    }

    /** A standing body's spine carried with the torso: lowered with the body, bent with its lean, turned with the chest. */
    public static KakujaModel.Spine ride(KakujaModel.Spine spine, Body body, boolean crouching) {
        double drop = body.drop() + (crouching ? WingRig.CROUCH_DROP : 0.0);
        boolean still = Math.abs(body.lean()) < 1.0E-4 && Math.abs(body.side()) < 1.0E-4 && Math.abs(body.twist()) < 1.0E-4 && drop == 0.0;
        if (still) {
            return spine;
        }
        int count = spine.p().length;
        double[][] ps = new double[count][];
        double[][] ts = new double[count][];
        double[][] ns = new double[count][];
        double[][] bs = new double[count][];
        for (int i = 0; i < count; i++) {
            ps[i] = torsoPoint(spine.p()[i], body, drop);
            ts[i] = torsoDirection(spine.t()[i], body);
            ns[i] = torsoDirection(spine.n()[i], body);
            bs[i] = torsoDirection(spine.b()[i], body);
        }
        return new KakujaModel.Spine(ps, ts, ns, bs, spine.length());
    }

    /**
     * A point fixed to the upper torso (rig space, standing) where it is with the body lowered by {@code drop}, the upper
     * body leaned forward and sideways about the hips (5.0.19: as the body layer leans the torso, head and arms, RigPose) and
     * the chest turned by its twist.
     */
    public static double[] torsoPoint(double[] p, Body body, double drop) {
        double[] q = {p[0], p[1] - BEND_PIVOT, p[2]};
        q = bend(q, body);
        q = twist(q, body.twist());
        return new double[] {q[0], q[1] + BEND_PIVOT - drop, q[2]};
    }

    /** A direction fixed to the upper torso, turned with it. */
    public static double[] torsoDirection(double[] v, Body body) {
        return twist(bend(v, body), body.twist());
    }

    /**
     * The upper body's bend (the body layer's lean and sideways bend) in rig space, as bendy-lib carries the head and arms:
     * a turn by {@code sqrt(lean^2 + side^2)} about the horizontal axis that is (lean, 0, side) in the model's own space.
     */
    static double[] bend(double[] v, Body body) {
        // Model space: x to the player's left, y down, z backward.
        double[] r = bendModel(new double[] {-v[0], -v[1], -v[2]}, body.lean(), body.side());
        return new double[] {-r[0], -r[1], -r[2]};
    }

    /**
     * The upper body's lean on a vector in model space (x to the player's left, y down, z backward; relative to the lean's
     * pivot at the hips): what {@link #bend} does in rig space.
     */
    public static double[] bendModel(double[] m, double lean, double side) {
        double amount = Math.sqrt(lean * lean + side * side);
        if (amount < 1.0E-5) {
            return m;
        }
        return rotate(m, new double[] {lean / amount, 0.0, side / amount}, amount);
    }

    /** The chest's turn (RigPose twist, torso yaw: + turns the chest to the right) in rig space. */
    static double[] twist(double[] v, double twist) {
        if (Math.abs(twist) < 1.0E-5) {
            return v;
        }
        double c = Math.cos(twist);
        double s = Math.sin(twist);
        return new double[] {v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c};
    }

    /** The points the spring chain follows on each tentacle: its root and {@link #NODES} points out to the claw (rig space). */
    public static double[][][] nodes(KakujaModel.Spine[] spines) {
        double[][][] out = new double[TENTACLES][][];
        for (int i = 0; i < TENTACLES; i++) {
            if (spines[i] == null) {
                continue;
            }
            out[i] = new double[NODES + 1][];
            for (int k = 0; k <= NODES; k++) {
                out[i][k] = spines[i].at(k / (double)NODES)[0];
            }
        }
        return out;
    }

    /**
     * {@code spine} bent by the spring chain's lag: {@code offsets} (rig space) at the root and each node, blended along the
     * length; the frames follow the new centre line (the plates keep their dorsal side up as it bends).
     */
    public static KakujaModel.Spine offset(KakujaModel.Spine spine, int side, double[][] offsets) {
        if (offsets == null || offsets.length < NODES + 1) {
            return spine;
        }
        int count = spine.p().length;
        double[][] ps = new double[count][];
        for (int i = 0; i < count; i++) {
            double s = i / (double)(count - 1) * NODES;
            int k = Math.min(NODES - 1, (int)Math.floor(s));
            double f = s - k;
            double[] o = lerp(offsets[k], offsets[k + 1], f);
            ps[i] = add(spine.p()[i], o);
        }
        return reshape(spine, side, ps);
    }

    /** Along each tentacle, from where to where its needles leave (share of its length): the long spikes of its middle. */
    static final double MUZZLE_FROM = 0.36;
    static final double MUZZLE_TO = 0.8;

    /**
     * Where needles leave tentacle {@code index} fully turned to fire toward {@code aim} (rig space, unit), for a still,
     * standing (or crouching) body: the points of {@code count} spikes spread along its belly, both rows in turn, from
     * {@link #MUZZLE_FROM} to {@link #MUZZLE_TO} of its length. Rig space (x right, y up from the feet, z forward).
     */
    public static double[][] muzzles(int index, double[] aim, boolean crouching, int count) {
        int side = sideOf(index);
        int tier = tierOf(index);
        Limb steady = new Limb(side, tier, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0);
        Limb aimed = aiming(steady, 1.0F, aim, 0.0F);
        KakujaModel.Spine spine = ride(spine(aimed, crouching, 1.0), Body.STILL, crouching);
        int n = Math.max(1, count);
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            double s = n == 1 ? 0.5 * (MUZZLE_FROM + MUZZLE_TO) : MUZZLE_FROM + (MUZZLE_TO - MUZZLE_FROM) * i / (n - 1.0);
            out[i] = KakujaModel.spikeTip(spine, REST[tier][WIDTH], i % 2 == 0 ? 1 : -1, s)[0];
        }
        return out;
    }

    /** Along a fire tendril, where its fire leaves (share of its length): just short of the tip. */
    static final double MOUTH_AT = 0.96;

    /**
     * Where fire leaves fire tendril {@code index} (the final kakuja's left three) fully turned toward {@code aim} (rig space, unit),
     * for a still, standing (or crouching) body: just short of its tip. Rig space (x right, y up from the feet, z forward).
     */
    public static double[] mouth(int index, double[] aim, boolean crouching) {
        Limb steady = new Limb(sideOf(index), tierOf(index), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0);
        Limb aimed = aiming(steady, 1.0F, aim, 0.0F);
        KakujaModel.Spine spine = ride(spine(aimed, crouching, 1.0), Body.STILL, crouching);
        return spine.at(MOUTH_AT)[0];
    }

    /** Separation passes per frame, and how far up from the roots it starts to apply (share of the length). */
    static final int SEPARATION_PASSES = 3;
    static final double SEPARATION_FROM = 0.1;
    static final double SEPARATION_RAMP = 0.25;
    /** Of the upper neighbour's spikes, the share that must clear the lower one (the tips may brush, the rows never cross). */
    static final double SPIKE_CLEARANCE = 0.7;

    /**
     * The clearance two neighbouring tentacles' centre lines need at {@code sa} along the upper one and {@code sb} along the
     * lower one: the upper's belly and most of its spikes, and the lower's ridge, nothing near the roots (they grow from one
     * place on his back) and the full clearance from a quarter of the way out.
     */
    public static double clearance(int upperTier, double sa, int lowerTier, double sb) {
        return clearance(upperTier, sa, 1.0, lowerTier, sb, 1.0);
    }

    /**
     * {@link #clearance(int, double, int, double)} for two tentacles still coming out, at their own deploys {@code deployA} and
     * {@code deployB} (5.0.9): only what they are as drawn (their width so far, the spikes once sprouted), and nothing within
     * the same distance of his back as when fully out. Two stubs just pushing out of his shoulder blade sit side by side
     * at their sockets; the full clearance there flung the newer one a block away (down to his feet).
     */
    public static double clearance(int upperTier, double sa, double deployA, int lowerTier, double sb, double deployB) {
        double ramp = UkakuModel.smooth((Math.min(sa * lengthOut(deployA), sb * lengthOut(deployB)) - SEPARATION_FROM) / SEPARATION_RAMP);
        if (ramp <= 0.0) {
            return 0.0;
        }
        double wa = REST[upperTier][WIDTH] * widthOut(deployA) * KakujaModel.widthAt(sa);
        double wb = REST[lowerTier][WIDTH] * widthOut(deployB) * KakujaModel.widthAt(sb);
        double spikes = REST[upperTier][WIDTH] * 1.9 * KakujaModel.spikeAt(sa) * spikesOut(deployA);
        return ramp * (0.5 * KakujaModel.THICKNESS * wa + SPIKE_CLEARANCE * spikes + 1.15 * KakujaModel.THICKNESS * wb);
    }

    /**
     * Keeps neighbouring tentacles on each side (upper and middle, middle and lower) from passing through each other:
     * wherever two come closer than {@link #clearance}, both are pushed apart along the line between them, the push spread
     * smoothly along each so neither kinks, nothing at the roots. A few passes, every frame, after everything else moved them.
     */
    public static void separate(KakujaModel.Spine[] spines, Limb[] limbs) {
        for (int pass = 0; pass < SEPARATION_PASSES; pass++) {
            boolean moved = false;
            for (int side = 1; side >= -1; side -= 2) {
                for (int tier = 0; tier < 2; tier++) {
                    int a = index(side, tier);
                    int b = index(side, tier + 1);
                    if (spines[a] == null || spines[b] == null) {
                        continue;
                    }
                    KakujaModel.Spine[] pair = apart(spines[a], tier, deployOf(limbs, a), spines[b], tier + 1, deployOf(limbs, b), side);
                    if (pair != null) {
                        spines[a] = pair[0];
                        spines[b] = pair[1];
                        moved = true;
                    }
                }
            }
            if (!moved) {
                return;
            }
        }
    }

    /** Tentacle {@code index}'s own deploy, fully out when there is no pose to say otherwise. */
    static double deployOf(Limb[] limbs, int index) {
        return limbs == null || limbs[index] == null ? 1.0 : limbs[index].deploy();
    }

    /** One separation pass on a pair (at their own deploys): the two pushed apart, or null if they were clear. */
    static KakujaModel.Spine[] apart(KakujaModel.Spine upper, int upperTier, double upperDeploy, KakujaModel.Spine lower, int lowerTier,
                                     double lowerDeploy, int side) {
        int na = upper.p().length;
        int nb = lower.p().length;
        double[][] pushA = new double[na][3];
        double[][] pushB = new double[nb][3];
        boolean any = false;
        for (int i = 0; i < na; i++) {
            double sa = i / (double)(na - 1);
            if (sa < SEPARATION_FROM) {
                continue;
            }
            // The closest point of the lower one to this point of the upper one.
            int best = -1;
            double bestGap = Double.MAX_VALUE;
            for (int j = 0; j < nb; j++) {
                double sb = j / (double)(nb - 1);
                double need = clearance(upperTier, sa, upperDeploy, lowerTier, sb, lowerDeploy);
                if (need <= 0.0) {
                    continue;
                }
                double[] d = sub(upper.p()[i], lower.p()[j]);
                double gap = Math.sqrt(dot(d, d)) - need;
                if (gap < bestGap) {
                    bestGap = gap;
                    best = j;
                }
            }
            if (best < 0 || bestGap >= 0.0) {
                continue;
            }
            double[] d = sub(upper.p()[i], lower.p()[best]);
            double length = Math.sqrt(dot(d, d));
            // Apart along the line between them; if they meet head on, the upper one goes its dorsal way.
            double[] away = length > 1.0E-6 ? mul(d, 1.0 / length) : upper.n()[i];
            double half = -bestGap * 0.5;
            pushA[i] = add(pushA[i], mul(away, half));
            pushB[best] = add(pushB[best], mul(away, -half));
            any = true;
        }
        if (!any) {
            return null;
        }
        return new KakujaModel.Spine[] {pushed(upper, side, pushA), pushed(lower, side, pushB)};
    }

    /** {@code spine} moved by {@code push} per sample, blurred along it so it bends smoothly, the roots held. */
    static KakujaModel.Spine pushed(KakujaModel.Spine spine, int side, double[][] push) {
        int count = push.length;
        double[][] smooth = push;
        for (int pass = 0; pass < 3; pass++) {
            double[][] next = new double[count][];
            for (int i = 0; i < count; i++) {
                double[] sum = mul(smooth[i], 0.4);
                double weight = 0.4;
                for (int k = 1; k <= 2; k++) {
                    double w = k == 1 ? 0.2 : 0.1;
                    if (i - k >= 0) {
                        sum = add(sum, mul(smooth[i - k], w));
                        weight += w;
                    }
                    if (i + k < count) {
                        sum = add(sum, mul(smooth[i + k], w));
                        weight += w;
                    }
                }
                next[i] = mul(sum, 1.0 / weight);
            }
            smooth = next;
        }
        // The blur spreads the push thinner: scale it back up so it still clears fully where it was needed (smooth everywhere).
        double gain = 1.0;
        for (int i = 0; i < count; i++) {
            double need = Math.sqrt(dot(push[i], push[i]));
            double got = Math.sqrt(dot(smooth[i], smooth[i]));
            if (need > 1.0E-6 && got > 1.0E-9) {
                gain = Math.max(gain, Math.min(3.0, need / got));
            }
        }
        double[][] ps = new double[count][];
        for (int i = 0; i < count; i++) {
            double s = i / (double)(count - 1);
            double hold = UkakuModel.smooth((s - SEPARATION_FROM * 0.5) / SEPARATION_RAMP);
            ps[i] = add(spine.p()[i], mul(smooth[i], gain * hold));
        }
        return reshape(spine, side, ps);
    }

    /** Lowest a point {@code s} (0 root .. 1 claw) along a tentacle's centre line may go over a floor at y 0. */
    static double floorAt(double s) {
        return FLOOR_ROOT + (FLOOR_TIP - FLOOR_ROOT) * clamp01(s);
    }

    /**
     * {@code spine} kept off the floor under his feet: any part that would go below {@link #floorAt} is pressed up along the
     * floor (a soft fold, so a claw resting on the ground slides along it instead of sinking or kinking).
     */
    public static KakujaModel.Spine floor(KakujaModel.Spine spine, int side) {
        int count = spine.p().length;
        boolean clear = true;
        for (int i = 0; i < count && clear; i++) {
            clear = spine.p()[i][1] - floorAt(i / (double)(count - 1)) > 4.0 * FLOOR_SOFT;
        }
        if (clear) {
            return spine;
        }
        double[][] ps = new double[count][];
        for (int i = 0; i < count; i++) {
            double[] p = spine.p()[i];
            double f = floorAt(i / (double)(count - 1));
            double above = (p[1] - f) / FLOOR_SOFT;
            double lifted = f + FLOOR_SOFT * (above > 30.0 ? above : Math.log1p(Math.exp(above)));
            ps[i] = new double[] {p[0], lifted, p[2]};
        }
        return reshape(spine, side, ps);
    }

    /** {@code spine} moved to new centre-line points {@code ps}: the frames follow (the plates keep their dorsal side up as it bends). */
    static KakujaModel.Spine reshape(KakujaModel.Spine spine, int side, double[][] ps) {
        int count = ps.length;
        double[][] ts = new double[count][];
        double[][] ns = new double[count][];
        double[][] bs = new double[count][];
        int sign = side >= 0 ? 1 : -1;
        for (int i = 0; i < count; i++) {
            double[] ahead = ps[Math.min(count - 1, i + 1)];
            double[] behind = ps[Math.max(0, i - 1)];
            double[] t = sub(ahead, behind);
            t = dot(t, t) < 1.0E-12 ? spine.t()[i] : normalize(t);
            double[] n = sub(spine.n()[i], mul(t, dot(spine.n()[i], t)));
            n = dot(n, n) < 1.0E-10 ? spine.n()[i] : normalize(n);
            ts[i] = t;
            ns[i] = n;
            bs[i] = mul(cross(t, n), sign);
        }
        double length = 0.0;
        for (int i = 1; i < count; i++) {
            double[] d = sub(ps[i], ps[i - 1]);
            length += Math.sqrt(dot(d, d));
        }
        return new KakujaModel.Spine(ps, ts, ns, bs, length);
    }

    /** The frame turned toward the belly by {@code down} and toward the leading edge by {@code forward} (radians). */
    static double[][] turn(double[] t, double[] n, double[] b, double down, double forward) {
        double cd = Math.cos(down);
        double sd = Math.sin(down);
        double[] t1 = sub(mul(t, cd), mul(n, sd));
        double[] n1 = add(mul(n, cd), mul(t, sd));
        double cf = Math.cos(forward);
        double sf = Math.sin(forward);
        double[] t2 = add(mul(t1, cf), mul(b, sf));
        double[] b2 = sub(mul(b, cf), mul(t1, sf));
        return new double[][] {normalize(t2), normalize(n1), normalize(b2)};
    }

    static UkakuModel.Sink sink(List<WingRig.Quad> out) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> out.add(new WingRig.Quad(new float[] {
            (float)a[0], (float)a[1], (float)a[2], (float)b[0], (float)b[1], (float)b[2], (float)c[0], (float)c[1], (float)c[2],
            (float)d[0], (float)d[1], (float)d[2]}, (float)n[0], (float)n[1], (float)n[2], new int[] {ca, cb, cc, cd}));
    }

    static double clamp01(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
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

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return normalize(new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]});
    }

    static double[] normalize(double[] a) {
        double length = Math.sqrt(dot(a, a));
        return length < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : mul(a, 1.0 / length);
    }

    /** {@code v} turned about unit {@code k} by {@code angle} (Rodrigues). */
    static double[] rotate(double[] v, double[] k, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double[] kxv = {k[1] * v[2] - k[2] * v[1], k[2] * v[0] - k[0] * v[2], k[0] * v[1] - k[1] * v[0]};
        double kv = dot(k, v);
        return new double[] {v[0] * c + kxv[0] * s + k[0] * kv * (1 - c), v[1] * c + kxv[1] * s + k[1] * kv * (1 - c),
            v[2] * c + kxv[2] * s + k[2] * kv * (1 - c)};
    }
}
