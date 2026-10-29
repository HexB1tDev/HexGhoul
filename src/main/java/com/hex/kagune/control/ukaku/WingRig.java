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
        RISE
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
    record WingAngles(float sweep, float roll) {
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
            boolean cutting = in.motion() == Motion.CROSS || in.motion() == (side > 0 ? Motion.SLASH_RIGHT : Motion.SLASH_LEFT);
            // A single wing (one small wing, Rabbit Guise) answers both hands.
            float aim = form.rightWing() ? (side > 0 ? in.aimRight() : in.aimLeft()) : Math.max(in.aimRight(), in.aimLeft());
            float shot = form.rightWing() ? (side > 0 ? in.kickRight() : in.kickLeft()) : Math.max(in.kickRight(), in.kickLeft());
            UkakuModel.Frame frame = aim > 0.001F && coil <= 0.0F
                ? aimFrame(side, ROOT_X * side, rootY, rootZ, length, angles.sweep(), angles.roll(), in.aim(), aim, shot)
                : twinFrame(side, ROOT_X * side, rootY, rootZ, length, angles.sweep(), angles.roll());
            if (coil > 0.0F) {
                frame = twirl(frame, side, coil);
            } else if (aim > 0.001F && in.barrel() != 0.0F) {
                // The Gatling: the aiming wing shudders about its own centre line with the stream.
                frame = spin(frame, side * in.barrel() * Math.min(1.0F, aim));
            }
            // A shot only swells the flame a little: the full thrust streams the tips back, through the curl of an aiming wing.
            float thrust = Math.max(Math.max(kick, AIM_SHOT_THRUST * Math.min(1.0F, shot)), coil);
            // A new wing grows out of the shoulder blade the way the kagune first comes out.
            float wingDeploy = deploy * grow;
            UkakuModel.wing(sink, frame, new UkakuModel.Params(in.time() + (side > 0 ? 0.0 : 1.7), crystal, wingDeploy, in.dissolving(), reEra,
                cutting ? strike : 0.0F, recoil, in.detail(), in.alpha(), side > 0 ? 1 : 2, side, in.volley(), thrust, mastery, eye));
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
