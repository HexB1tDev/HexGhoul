import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.PaleStrike;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Pale's speed blitz, choreographed (6.1.0): every key of every strike as a fighter would throw it, written down as what the body
 * does (the pivot of the whole body on the feet, the chest's extra turn, the lean, the lunge of the hips over a stepping foot, the
 * legs) and where each fist and elbow goes in the world; the arms' angles are then solved for (inverse kinematics on the exact
 * model: {@link BodyFrame}), keeping every part of each arm clear of the torso and head, and written out as
 * {@code PaleStrikePoses.java}. Edit the choreography here and run {@code tools/pale-pose-design.sh}; never edit the generated class.
 *
 * <p>Strikes are designed right-handed and mirrored for the left ones. World space: blocks, x to her right, y up from the feet,
 * z forward (the target is ahead, about 1.6 blocks off). Key times are ticks (the impact key on the server's impact tick).
 *
 * <p>What makes a punch land: a visible coil (the weight sat back on the rear leg, the chest wound away, the striking shoulder
 * pulled back, the fist chambered by the jaw or the hip), then a drive that accelerates into contact (the whole body pivoting
 * through, the lead foot stepping into a lunge, the rear leg straightening, the shoulder thrown forward, the arm fully out, the
 * other hand pulled hard back), an overshoot a hair past full extension, a short hold, and the recoil.
 */
public final class PalePoseDesign {
    /** One key: when, how the motion eases into it, the body, and where the hands go (null: hanging, untouched). */
    static final class Spec {
        final double at;
        final String ease;
        final RigPose body = new RigPose();
        double[] rightFist, rightElbow, leftFist, leftElbow;
        /** The hands placed from their own shoulders instead (see {@link #rightFrom}): null when given in the move's frame. */
        double[] rightFrom, rightElbowFrom, leftFrom, leftElbowFrom;
        boolean guard;
        /** The left arm is the right one mirrored (a square stance: the guard, both palms). */
        boolean mirrored;
        RigPose solved;
        Spec copyArmsFrom;
        float elbowAdd;
        boolean airborne;
        boolean guardArms;
        boolean fresh;

        Spec(double at, String ease) {
            this.at = at;
            this.ease = ease;
            // The strike owns her limbs: none of vanilla's swing or bob under it (and the arms are solved for exactly this turn).
            this.body.armHold = 1.0F;
            this.body.legHold = 1.0F;
        }

        /** The whole body's pivot on the feet (+ to her left), the chest's own turn (+ pulls the right shoulder back), lean, lunge. */
        Spec body(double spin, double twist, double lean, double lunge) {
            this.body.spin = (float)spin;
            this.body.twist = (float)twist;
            this.body.lean = (float)lean;
            this.body.lunge = (float)lunge;
            // Her eyes stay on the target whatever the body does: the head turns back against the pivot, and lifts against the lean.
            this.body.headY = (float)wrap(spin);
            this.body.headX = (float)(-0.6 * lean);
            return this;
        }

        /** The lead (left) leg and the rear (right) leg: thigh swing (- forward) and knee bend. */
        Spec legs(double frontX, double frontKnee, double rearX, double rearKnee) {
            this.body.leftLegX = (float)frontX;
            this.body.leftKnee = (float)frontKnee;
            this.body.rightLegX = (float)rearX;
            this.body.rightKnee = (float)rearKnee;
            this.body.rightLegZ = 0.08F;
            this.body.leftLegZ = -0.08F;
            return this;
        }

        Spec right(double[] fist, double[] elbow) {
            this.rightFist = fist;
            this.rightElbow = elbow;
            return this;
        }

        Spec left(double[] fist, double[] elbow) {
            this.leftFist = fist;
            this.leftElbow = elbow;
            return this;
        }

        /**
         * 6.2.0: the right hand placed from her right shoulder wherever the body has put it (blocks, in her turned body's frame: x
         * to her right, y up, z ahead of her), for a body leaned or spun so far that a point in the move's frame is out of reach.
         */
        Spec rightFrom(double[] fist, double[] elbow) {
            this.rightFrom = fist;
            this.rightElbowFrom = elbow;
            return this;
        }

        /** ...and the left hand from her left shoulder. */
        Spec leftFrom(double[] fist, double[] elbow) {
            this.leftFrom = fist;
            this.leftElbowFrom = elbow;
            return this;
        }

        /** The shoulders' own drive (pixels; z - forward, y - up). */
        Spec shoulders(double rightY, double rightZ, double leftY, double leftZ) {
            this.body.rightShoulderY = (float)rightY;
            this.body.rightShoulderZ = (float)rightZ;
            this.body.leftShoulderY = (float)leftY;
            this.body.leftShoulderZ = (float)leftZ;
            return this;
        }

        /** The arms as {@code other} has them, the elbows bent {@code bend} more (a recoil out of full extension). */
        Spec armsOf(Spec other, double bend) {
            this.copyArmsFrom = other;
            this.elbowAdd = (float)bend;
            return this;
        }

        Spec mirrored() {
            this.mirrored = true;
            return this;
        }

        Spec head(double x) {
            this.body.headX += (float)x;
            return this;
        }

        /** The head's own turn (radians, + to her right), whatever the body's pivot (she looks over her shoulder). */
        Spec looking(double y) {
            this.body.headY = (float)y;
            return this;
        }

        /** The whole body tipped back ({@code x} +) or forward, about its middle (a kick's lean away from the leg). */
        Spec tilt(double x) {
            this.body.flip = (float)x;
            return this;
        }

        /** 6.2.0: the whole body rolled about the line ahead of the move (+ rolls her to her left: her right side goes up). */
        Spec roll(double z) {
            this.body.roll = (float)z;
            return this;
        }

        /** Off the ground: the legs lift nothing, the body is not lowered to keep a foot down. */
        Spec airborne() {
            this.airborne = true;
            return this;
        }

        /** Her right leg on its own: thigh swing (- forward), out to her right side (+), and knee bend. */
        Spec rightLeg(double x, double z, double knee) {
            this.body.rightLegX = (float)x;
            this.body.rightLegZ = (float)z;
            this.body.rightKnee = (float)knee;
            return this;
        }

        /** Her left leg on its own: thigh swing (- forward), out to her left side (+), and knee bend. */
        Spec leftLeg(double x, double z, double knee) {
            this.body.leftLegX = (float)x;
            this.body.leftLegZ = (float)-z;
            this.body.leftKnee = (float)knee;
            return this;
        }

        /** Her upper body leaned to her left (+) or right about the hips. */
        Spec side(double s) {
            this.body.side = (float)s;
            return this;
        }

        /** The plane each knee folds in, turned about the leg's length (a roundhouse's chamber folds the shin out to the side). */
        Spec kneeTurn(double right, double left) {
            this.body.rightKneeTurn = (float)right;
            this.body.leftKneeTurn = (float)left;
            return this;
        }

        /** Solved from her guard rather than from the key before (a new start: the arms come at it fresh). */
        Spec fresh() {
            this.fresh = true;
            return this;
        }

        Spec guardArms() {
            this.guardArms = true;
            return this;
        }
    }

    static Spec key(double at, String ease) {
        return new Spec(at, ease);
    }

    /** The guard (the last key of every strike: the generated {@code guard()}). */
    static Spec guardKey(double at) {
        Spec spec = new Spec(at, "SMOOTH");
        spec.guard = true;
        return spec;
    }

    static double[] v(double x, double y, double z) {
        return new double[] {x, y, z};
    }

    /**
     * A point given as her turned body sees it (x to its right, y up, z ahead of it) in the move's frame, the body turned
     * {@code spin} (+ to her left) from the way the move faces.
     */
    static double[] w(double spin, double x, double y, double z) {
        return new double[] {x * Math.cos(spin) - z * Math.sin(spin), y, x * Math.sin(spin) + z * Math.cos(spin)};
    }

    /** {@code p} carried along with a lunge of {@code lunge} pixels (the whole body moved ahead, toward what she strikes). */
    static double[] ahead(double[] p, double lunge) {
        return new double[] {p[0], p[1], p[2] + lunge / 16.0};
    }

    /** Far along a line, for an arm thrown all the way out at it. */
    static double[] at(double x, double y) {
        return new double[] {x, y, 2.2};
    }

    // ------------------------------------------------------------------ the choreography

    /** Square and loose, knees soft, both fists up in front of the chest, the elbows down and out by her ribs. */
    static Spec guard() {
        return key(0, "SMOOTH").body(0.0, 0.0, 0.12, 0.0).legs(-0.36, 0.72, -0.36, 0.72)
            .right(v(0.12, 1.25, 0.46), v(0.4, 1.08, 0.24)).left(v(-0.12, 1.25, 0.46), v(-0.4, 1.08, 0.24)).mirrored();
    }

    /** A straight right (power 1: the cross; 0.62: the jab, the same punch from the other hand, shorter and quicker). */
    static List<Spec> straight(PaleStrike strike, double k) {
        int i = strike.impact;
        Spec coil = key(i - (k < 1 ? 0.85 : 1.15), "SMOOTH").body(-0.3 * k, 0.42 * k, 0.06, -1.3 * k).legs(-0.42, 0.62, -0.2, 0.95)
            .right(v(0.3, 1.18, 0.12), v(0.38, 0.98, -0.12)).left(v(-0.05, 1.3, 0.52), v(-0.26, 1.1, 0.32));
        Spec hit = key(i, "IN").body(0.4 * k, -0.8 * k, 0.12 + 0.18 * k, 3.6 * k).legs(-0.45 - 0.35 * k, 0.55 + 0.35 * k, 0.45 * k, 0.05)
            .shoulders(-0.4 * k, -2.0 * k, 0.0, 0.0).right(at(0.0, 1.34), null).left(v(-0.3, 1.16, 0.16), v(-0.38, 1.0, -0.06));
        Spec over = key(i + 0.55, "OUT").body(0.5 * k, -0.92 * k, 0.15 + 0.21 * k, 4.3 * k).legs(-0.5 - 0.38 * k, 0.6 + 0.35 * k, 0.5 * k, 0.03)
            .shoulders(-0.4 * k, -2.0 * k, 0.0, 0.0).right(at(-0.02, 1.34), null).left(v(-0.3, 1.16, 0.16), v(-0.38, 1.0, -0.06));
        Spec hold = key(i + 1.6, "SMOOTH").body(0.36 * k, -0.74 * k, 0.12 + 0.15 * k, 3.0 * k).legs(-0.45 - 0.3 * k, 0.55 + 0.32 * k, 0.4 * k, 0.1)
            .shoulders(-0.3 * k, -1.4 * k, 0.0, 0.0).armsOf(hit, 0.3);
        return List.of(coil, hit, over, hold, guardKey(strike.duration));
    }

    /** A right hook to the head: the elbow up and out, the forearm level, swung round by the pivot and the chest. */
    static List<Spec> hook(PaleStrike strike) {
        int i = strike.impact;
        Spec coil = key(i - 1.2, "SMOOTH").body(-0.42, 0.5, 0.16, -0.9).legs(-0.5, 0.95, -0.3, 1.0)
            .right(v(0.46, 1.18, 0.16), v(0.5, 1.1, -0.12)).left(v(-0.12, 1.25, 0.4), v(-0.3, 1.0, 0.24));
        Spec hit = key(i, "IN").body(0.32, -0.6, 0.22, 2.6).legs(-0.7, 0.9, 0.32, 0.22).shoulders(-0.6, -1.4, 0.0, 0.0)
            .right(v(0.1, 1.36, 0.9), v(0.4, 1.33, 0.66)).left(v(-0.3, 1.16, 0.18), v(-0.38, 1.0, -0.04));
        Spec over = key(i + 0.55, "OUT").body(0.42, -0.7, 0.25, 3.0).legs(-0.72, 0.9, 0.36, 0.18).shoulders(-0.6, -1.6, 0.0, 0.0)
            .right(v(-0.02, 1.36, 0.9), v(0.32, 1.34, 0.74)).left(v(-0.3, 1.16, 0.18), v(-0.38, 1.0, -0.04));
        Spec hold = key(i + 1.6, "SMOOTH").body(0.34, -0.6, 0.2, 2.4).legs(-0.66, 0.88, 0.3, 0.25).shoulders(-0.4, -1.0, 0.0, 0.0)
            .right(v(0.1, 1.3, 0.8), v(0.38, 1.26, 0.6)).left(v(-0.3, 1.16, 0.18), v(-0.38, 1.0, -0.04));
        return List.of(coil, hit, over, hold, guardKey(strike.duration));
    }

    /** Dipping under, a right hook into the body: deep in the knees, the forearm level at the belt, driven by the legs and pivot. */
    static List<Spec> liver(PaleStrike strike) {
        int i = strike.impact;
        Spec coil = key(i - 1.2, "SMOOTH").body(-0.36, 0.46, 0.32, -0.6).legs(-0.72, 1.32, -0.55, 1.25)
            .right(v(0.42, 1.02, 0.2), v(0.44, 0.95, -0.08)).left(v(-0.12, 1.18, 0.42), v(-0.3, 0.96, 0.28));
        Spec hit = key(i, "IN").body(0.42, -0.78, 0.42, 2.8).legs(-0.9, 1.25, 0.28, 0.7).shoulders(0.0, -1.4, 0.0, 0.0)
            .right(v(0.04, 0.98, 0.86), v(0.34, 0.96, 0.62)).left(v(-0.32, 1.1, 0.16), v(-0.38, 0.96, -0.06));
        Spec over = key(i + 0.55, "OUT").body(0.54, -0.88, 0.45, 3.2).legs(-0.92, 1.25, 0.32, 0.66).shoulders(0.0, -1.6, 0.0, 0.0)
            .right(v(-0.06, 0.98, 0.84), v(0.26, 0.96, 0.7)).left(v(-0.32, 1.1, 0.16), v(-0.38, 0.96, -0.06));
        Spec hold = key(i + 1.6, "SMOOTH").body(0.44, -0.76, 0.38, 2.6).legs(-0.86, 1.2, 0.26, 0.72).shoulders(0.0, -1.0, 0.0, 0.0)
            .armsOf(over, 0.15);
        return List.of(coil, hit, over, hold, guardKey(strike.duration));
    }

    /** From deep in the knees, an uppercut: the legs drive her up, the chest rises and turns, the fist comes up close in front. */
    static List<Spec> uppercut(PaleStrike strike) {
        int i = strike.impact;
        Spec coil = key(i - 1.5, "SMOOTH").body(-0.36, 0.42, 0.38, -0.7).legs(-0.78, 1.42, -0.6, 1.36).shoulders(1.0, 0.0, 0.0, 0.0)
            .right(v(0.34, 0.86, 0.22), v(0.42, 0.97, -0.08)).left(v(-0.12, 1.16, 0.42), v(-0.3, 0.94, 0.28));
        Spec hit = key(i, "IN").body(0.34, -0.62, -0.08, 2.4).legs(-0.32, 0.3, 0.2, 0.05).shoulders(-0.8, -1.2, 0.0, 0.0)
            .right(v(0.1, 1.52, 0.8), v(0.22, 1.22, 0.66)).left(v(-0.3, 1.18, 0.16), v(-0.38, 1.0, -0.06)).head(-0.1);
        Spec over = key(i + 0.55, "OUT").body(0.4, -0.7, -0.14, 2.8).legs(-0.3, 0.22, 0.24, 0.02).shoulders(-1.0, -1.2, 0.0, 0.0)
            .right(v(0.08, 1.62, 0.8), v(0.2, 1.3, 0.68)).left(v(-0.3, 1.18, 0.16), v(-0.38, 1.0, -0.06)).head(-0.14);
        Spec hold = key(i + 1.6, "SMOOTH").body(0.32, -0.6, -0.06, 2.2).legs(-0.32, 0.36, 0.18, 0.1).shoulders(-0.6, -0.8, 0.0, 0.0)
            .right(v(0.12, 1.46, 0.76), v(0.24, 1.18, 0.62)).left(v(-0.3, 1.18, 0.16), v(-0.38, 1.0, -0.06));
        return List.of(coil, hit, over, hold, guardKey(strike.duration));
    }

    /**
     * The blink's palm: she vanishes already coiled (low, the palm chambered at the hip), is there coiled, and drives the palm into
     * the target's ribs from a long low lunge, the other hand pulled back to the hip.
     */
    static List<Spec> palm(PaleStrike strike) {
        int i = strike.impact;
        Spec coil = key(PaleStrike.BLINK_TICK - 0.1, "SMOOTH").body(-0.46, 0.46, 0.34, -1.0).legs(-0.86, 1.36, -0.1, 1.12)
            .right(v(0.42, 0.97, -0.05), v(0.42, 1.08, -0.25)).left(v(-0.04, 1.18, 0.5), v(-0.26, 1.0, 0.32));
        Spec gather = key(i - 1.0, "SMOOTH").body(-0.5, 0.5, 0.36, -1.4).legs(-0.9, 1.42, -0.14, 1.18)
            .right(v(0.42, 0.96, -0.07), v(0.42, 1.07, -0.27)).left(v(-0.04, 1.17, 0.5), v(-0.26, 0.99, 0.32));
        Spec hit = key(i, "IN").body(0.44, -0.8, 0.46, 5.0).legs(-1.05, 1.35, 0.62, 0.05).shoulders(0.0, -2.0, 0.0, 0.0)
            .right(at(0.0, 1.08), null).left(v(-0.42, 0.97, -0.05), v(-0.42, 1.08, -0.25));
        Spec over = key(i + 0.6, "OUT").body(0.52, -0.9, 0.5, 5.7).legs(-1.08, 1.35, 0.68, 0.03).shoulders(0.0, -2.0, 0.0, 0.0)
            .right(at(-0.02, 1.08), null).left(v(-0.42, 0.97, -0.05), v(-0.42, 1.08, -0.25));
        Spec hold = key(i + 2.0, "SMOOTH").body(0.4, -0.76, 0.42, 4.6).legs(-1.0, 1.3, 0.56, 0.1).shoulders(0.0, -1.4, 0.0, 0.0)
            .armsOf(hit, 0.2);
        return List.of(coil, gather, hit, over, hold, guardKey(strike.duration));
    }

    /**
     * The spinning backfist: a little wind the other way, then a full turn to her right on the spot, the arm folded across the
     * chest until the last instant and whipped out straight, the back of the fist leading, meeting the target as she comes round.
     */
    static List<Spec> backfist(PaleStrike strike) {
        int i = strike.impact;
        double turn = -Math.PI * 2.0;
        Spec wind = key(1.3, "SMOOTH").body(0.45, -0.3, 0.12, -0.5).legs(-0.45, 0.9, -0.45, 0.9)
            .right(v(-0.12, 1.22, 0.34), v(0.16, 1.18, 0.38)).left(v(-0.12, 1.2, 0.4), v(-0.3, 0.98, 0.24));
        Spec hit = key(i, "IN").body(turn + 0.55, -0.25, 0.08, 1.6).legs(-0.4, 0.8, -0.3, 0.7)
            .right(v(0.06, 1.42, 2.0), null).left(v(-0.2, 1.22, 0.2), v(-0.34, 0.98, 0.02));
        Spec over = key(i + 0.6, "OUT").body(turn + 0.18, -0.2, 0.06, 1.8).legs(-0.4, 0.8, -0.3, 0.7)
            .right(v(1.2, 1.4, 1.7), null).left(v(-0.2, 1.22, 0.2), v(-0.34, 0.98, 0.02));
        Spec hold = key(i + 2.0, "SMOOTH").body(turn, -0.1, 0.1, 1.0).legs(-0.4, 0.8, -0.36, 0.76)
            .right(v(0.62, 1.28, 0.45), v(0.5, 1.2, 0.2)).left(v(-0.12, 1.2, 0.38), v(-0.3, 0.98, 0.22));
        Spec end = guardKey(strike.duration);
        end.body.spin = (float)turn;
        return List.of(wind, hit, over, hold, end);
    }

    /** The finisher: square, sunk deep, both hands chambered at the hips, then both palms driven out from a long low lunge. */
    static List<Spec> twinPalm(PaleStrike strike) {
        int i = strike.impact;
        Spec coil = key(PaleStrike.BLINK_TICK, "SMOOTH").body(0.0, 0.0, 0.22, -1.6).legs(-0.76, 1.46, -0.76, 1.46).head(0.12)
            .right(v(0.42, 1.0, 0.04), v(0.46, 1.06, -0.2)).left(v(-0.42, 1.0, 0.04), v(-0.46, 1.06, -0.2)).mirrored();
        Spec gather = key(i - 1.2, "SMOOTH").body(0.0, 0.0, 0.26, -2.1).legs(-0.8, 1.52, -0.8, 1.52).head(0.12)
            .right(v(0.43, 0.99, 0.02), v(0.47, 1.05, -0.22)).left(v(-0.43, 0.99, 0.02), v(-0.47, 1.05, -0.22)).mirrored();
        Spec hit = key(i, "IN").body(0.0, 0.0, 0.46, 5.2).legs(-1.05, 1.3, 0.6, 0.05).shoulders(0.0, -2.0, 0.0, -2.0)
            .right(at(0.09, 1.1), null).left(at(-0.09, 1.1), null).mirrored();
        Spec over = key(i + 0.6, "OUT").body(0.0, 0.0, 0.52, 6.0).legs(-1.08, 1.3, 0.66, 0.03).shoulders(0.0, -2.0, 0.0, -2.0)
            .right(at(0.09, 1.1), null).left(at(-0.09, 1.1), null).mirrored();
        Spec hold = key(i + 3.0, "SMOOTH").body(0.0, 0.0, 0.42, 4.8).legs(-1.0, 1.28, 0.55, 0.1).shoulders(0.0, -1.4, 0.0, -1.4)
            .armsOf(hit, 0.15);
        return List.of(coil, gather, hit, over, hold, guardKey(strike.duration));
    }

    /**
     * Big Punch held: sunk deep and wound right round, the right fist drawn back by the ribs, the left hand out at the target,
     * deeper and further round the longer it charges ({@code c}: 0 when the key goes down, 1 fully charged).
     */
    static Spec punchCoil(double c) {
        return key(0, "SMOOTH").body(-0.55 - 0.25 * c, 0.5 + 0.15 * c, 0.06 + 0.06 * c, -1.2 - 1.0 * c)
            .legs(-0.62 - 0.14 * c, 1.15 + 0.25 * c, -0.48 - 0.12 * c, 1.25 + 0.25 * c)
            .right(v(0.43, 1.0 - 0.02 * c, -0.1 - 0.08 * c), v(0.44, 1.1, -0.3 - 0.05 * c))
            .left(v(-0.04, 1.28, 0.6 + 0.04 * c), v(-0.22, 1.18, 0.34));
    }

    /**
     * Big Punch let go: she vanishes and is in front of the target already coiled, then drives the right fist into its gut from the
     * longest, lowest lunge she has, everything behind it, and holds the extension a moment while the body flies.
     */
    static List<Spec> bigPunch() {
        int i = PaleRules.PUNCH_IMPACT;
        Spec coil = punchCoil(1.0);
        Spec appear = key(PaleRules.PUNCH_BLINK - 0.1, "SMOOTH");
        appear.body.clear().add(coil.body, 1.0F);
        appear.right(coil.rightFist, coil.rightElbow).left(coil.leftFist, coil.leftElbow);
        Spec hit = key(i, "IN").body(0.5, -0.9, 0.5, 6.5).legs(-1.12, 1.4, 0.72, 0.03).shoulders(0.0, -2.0, 0.0, 0.0)
            .right(at(0.0, 1.0), null).left(v(-0.42, 0.97, -0.05), v(-0.42, 1.08, -0.25));
        Spec over = key(i + 0.7, "OUT").body(0.6, -1.0, 0.56, 7.4).legs(-1.15, 1.4, 0.78, 0.02).shoulders(0.0, -2.0, 0.0, 0.0)
            .right(at(-0.02, 1.0), null).left(v(-0.42, 0.97, -0.05), v(-0.42, 1.08, -0.25));
        Spec hold = key(i + 4.0, "SMOOTH").body(0.52, -0.92, 0.5, 6.8).legs(-1.1, 1.38, 0.7, 0.05).shoulders(0.0, -1.8, 0.0, 0.0)
            .armsOf(over, 0.05);
        return List.of(appear, hit, over, hold, guardKey(PaleRules.PUNCH_TICKS));
    }

    /**
     * The flesh swipe: as it bursts out of her back she shudders, head up; turned to her right she lifts her right hand to
     * conduct it, then turns through with the sweep, the hand drawn across to her left; and settles. Arms low and in front, out of
     * the flesh's way over her shoulder.
     */
    static List<Spec> swipe() {
        Spec shudder = key(2.0, "SMOOTH").body(-0.2, 0.25, 0.24, -0.6).legs(-0.42, 0.85, -0.32, 0.85).head(-0.18)
            .right(v(0.4, 1.0, 0.24), v(0.44, 1.05, 0.0)).left(v(-0.4, 1.0, 0.24), v(-0.44, 1.05, 0.0));
        Spec ready = key(PaleRules.SWIPE_ERUPT, "SMOOTH").body(-0.38, 0.35, 0.15, -0.3).legs(-0.45, 0.85, -0.3, 0.85)
            .right(v(0.52, 1.18, 0.42), v(0.46, 1.08, 0.14)).left(v(-0.12, 1.22, 0.44), v(-0.36, 1.06, 0.22));
        Spec swept = key(PaleRules.SWIPE_SWEEP_END, "SMOOTH").body(0.45, -0.45, 0.18, 0.8).legs(-0.5, 0.85, -0.2, 0.75)
            .right(v(-0.12, 1.16, 0.58), v(0.24, 1.1, 0.42)).left(v(-0.36, 1.06, 0.12), v(-0.42, 1.0, -0.06));
        Spec settle = key(PaleRules.SWIPE_SWEEP_END + 4.0, "OUT").body(0.38, -0.36, 0.14, 0.5).legs(-0.48, 0.82, -0.24, 0.78)
            .right(v(0.0, 1.14, 0.52), v(0.3, 1.06, 0.34)).left(v(-0.34, 1.08, 0.18), v(-0.42, 1.02, -0.02));
        return List.of(shudder, ready, swept, settle, guardKey(PaleRules.SWIPE_TICKS));
    }

    /** Reaching: her right hand out at what the flesh flies to, leaning after it. */
    static List<Spec> grabReach() {
        return List.of(key(3.0, "SMOOTH").body(0.15, -0.25, 0.2, 1.2).legs(-0.5, 0.8, -0.1, 0.5).shoulders(0.0, -1.2, 0.0, 0.0)
            .right(at(0.06, 1.38), null).left(v(-0.3, 1.12, 0.2), v(-0.4, 1.0, -0.02)));
    }

    /** Holding what it caught up in front of her: standing tall, the right hand closed up before her as if it were the hand that holds. */
    static List<Spec> grabHold() {
        return List.of(key(4.0, "SMOOTH").body(0.05, -0.1, -0.04, 0.0).legs(-0.3, 0.55, -0.24, 0.5).head(-0.12)
            .right(v(0.22, 1.42, 0.5), v(0.4, 1.18, 0.3)).left(v(-0.36, 1.0, 0.14), v(-0.42, 1.04, -0.08)));
    }

    /**
     * The throw: she hauls it up over her shoulder, leaning right back into the pull and sitting on her back leg, her right hand
     * dragged up and back by her ear as if it hauled the flesh; hangs there, straining, wound round; then everything goes into
     * the whip: she pivots and steps through, the hand flung out and down after it, and follows through.
     */
    static List<Spec> grabThrow() {
        int pull = PaleRules.GRAB_PULL;
        int hang = pull + PaleRules.GRAB_HANG;
        int release = PaleRules.grabRelease();
        Spec haul = key(pull * 0.5, "SMOOTH").body(-0.35, 0.4, -0.12, -1.2).legs(-0.45, 0.35, -0.4, 0.9)
            .right(v(0.48, 1.5, 0.1), v(0.5, 1.22, -0.12)).left(v(-0.16, 1.22, 0.46), v(-0.34, 1.06, 0.26));
        Spec back = key(pull, "SMOOTH").body(-0.6, 0.65, -0.26, -2.4).legs(-0.6, 0.2, -0.55, 1.15)
            .right(v(0.5, 1.56, -0.2), v(0.52, 1.28, -0.32)).left(v(-0.08, 1.26, 0.5), v(-0.3, 1.08, 0.3));
        Spec strain = key(hang, "SMOOTH").body(-0.7, 0.72, -0.3, -2.8).legs(-0.62, 0.18, -0.6, 1.2)
            .right(v(0.5, 1.58, -0.26), v(0.52, 1.3, -0.36)).left(v(-0.06, 1.28, 0.52), v(-0.28, 1.1, 0.32));
        Spec whip = key(release, "IN").body(0.6, -0.9, 0.42, 4.6).legs(-0.95, 1.05, 0.5, 0.08).shoulders(0.0, -2.0, 0.0, 0.0)
            .right(at(0.0, 1.25), null).left(v(-0.42, 0.98, -0.06), v(-0.42, 1.06, -0.26));
        Spec follow = key(release + 1.2, "OUT").body(0.72, -1.0, 0.5, 5.2).legs(-1.0, 1.1, 0.56, 0.06).shoulders(0.0, -2.0, 0.0, 0.0)
            .right(at(-0.04, 1.1), null).left(v(-0.42, 0.98, -0.06), v(-0.42, 1.06, -0.26));
        Spec recover = key(release + 5.0, "SMOOTH").body(0.5, -0.7, 0.32, 3.8).legs(-0.85, 1.0, 0.4, 0.15).shoulders(0.0, -1.2, 0.0, 0.0)
            .armsOf(follow, 0.4);
        return List.of(haul, back, strain, whip, follow, recover, guardKey(release + PaleRules.GRAB_RETRACT));
    }

    /**
     * The Speed Kick's lunge into the dash (V tapped, 6.2.0: the user's stick figure): thrown far over forward about her hips, her eyes
     * on it; the right arm reaching straight out ahead of her, the left swept back level behind her shoulder with the forearm hanging;
     * the left knee driven up under her chest, the shin folded back under it; the right leg stretched out long and straight behind
     * her. She is gone a moment after (the dash is unseen); the pose is what is left behind as she goes.
     */
    static List<Spec> kickRush() {
        return List.of(key(1.5, "OUT").body(0.0, 0.0, 0.2, 0.0).tilt(-0.62).head(-0.72).leftLeg(-1.7, 0.04, 1.8).rightLeg(0.5, 0.04, 0.12)
            .rightFrom(v(0.04, -0.04, 0.55), v(0.02, -0.01, 0.25)).leftFrom(v(-0.06, -0.27, -0.16), v(-0.04, 0.0, -0.25)));
    }

    /**
     * The Speed Kick's chamber (6.2.0): what she is in the instant she is there in front of it, already thrown back off her left foot
     * into the kick, rolled a little onto her left side, her right knee drawn up hard toward it with the shin folded tight under the
     * thigh; the right fist cocked out ahead, the left arm down and back.
     */
    static Spec kickChamber() {
        return key(0.6, "SMOOTH").body(0.0, 0.0, -0.2, 0.0).tilt(0.55).roll(0.45).head(0.55).leftLeg(-0.35, 0.04, 0.9)
            .rightLeg(-1.45, 0.06, 2.35).rightFrom(v(0.04, 0.12, 0.42), v(0.06, 0.02, 0.2)).leftFrom(v(-0.1, -0.38, -0.3), v(-0.06, -0.24, -0.08));
    }

    /**
     * The Speed Kick itself (6.2.0: the user's second stick figure, Kizaru's kick with everything behind it), from the chamber (ticks
     * from when she is there): THE BLOW, a flying side kick: her whole body thrown out flat in the air behind the leg, rolled onto its
     * left side (the right hip on top), the right leg locked out dead straight and level into its middle, the left knee tucked up
     * toward it with the foot folded back under her, her head tucked to keep her eyes on it; the right arm reaching on toward it over
     * her, the left hanging under her with the elbow out behind her head. A hair further through; held out flat while it flies;
     * then she rebounds off it: a backflip (unrolling as she goes over, tucked tight upside down), opening out with her feet coming
     * under her, down in a deep crouch, and up into her guard.
     */
    static List<Spec> kick() {
        double flat = 1.42;
        double side = 1.3;
        Spec contact = key(PaleRules.KICK_CONTACT, "IN").body(0.0, 0.0, 0.0, 0.0).tilt(flat).roll(side).airborne().head(1.2)
            .rightLeg(-0.14, 0.02, 0.0).leftLeg(-0.22, -0.1, 2.2)
            .rightFrom(v(0.0, 0.14, 0.53), v(0.02, 0.1, 0.24)).leftFrom(v(-0.15, -0.12, 0.15), v(-0.05, -0.25, -0.05));
        Spec through = key(PaleRules.KICK_CONTACT + 0.6, "OUT").body(0.0, 0.0, 0.0, 0.0).tilt(flat + 0.06).roll(side + 0.04).airborne().head(1.22)
            .rightLeg(-0.2, 0.02, 0.0).leftLeg(-0.24, -0.1, 2.25)
            .rightFrom(v(0.0, 0.13, 0.54), v(0.02, 0.1, 0.25)).leftFrom(v(-0.15, -0.12, 0.14), v(-0.05, -0.25, -0.06));
        Spec hold = key(PaleRules.KICK_HOLD, "SMOOTH").body(0.0, 0.0, 0.0, 0.0).tilt(flat).roll(side).airborne().head(1.15)
            .rightLeg(-0.12, 0.02, 0.06).leftLeg(-0.2, -0.1, 2.15)
            .rightFrom(v(0.0, 0.15, 0.52), v(0.02, 0.1, 0.23)).leftFrom(v(-0.15, -0.12, 0.16), v(-0.05, -0.25, -0.04));
        // The rebound: over backwards, unrolled, tucked tight upside down.
        Spec tuck = key(PaleRules.KICK_TUCK, "SMOOTH").body(0.0, 0.0, 0.35, 0.0).tilt(Math.PI).airborne().head(0.3)
            .legs(-1.9, 2.3, -1.8, 2.3).rightFrom(v(0.08, -0.28, 0.32), v(0.12, -0.08, 0.08)).leftFrom(v(-0.08, -0.28, 0.32), v(-0.12, -0.08, 0.08));
        // Opening out, the feet coming down under her.
        Spec open = key(PaleRules.KICK_OPEN, "SMOOTH").body(0.0, 0.0, 0.2, 0.0).tilt(Math.PI * 2.0 - 0.55).airborne().head(-0.2)
            .legs(-0.9, 1.3, -0.7, 1.2).rightFrom(v(0.28, 0.12, 0.12), v(0.18, -0.06, 0.02)).leftFrom(v(-0.28, 0.12, 0.12), v(-0.18, -0.06, 0.02));
        // Down in a deep crouch, arms out to the sides.
        Spec land = key(PaleRules.KICK_LAND, "OUT").body(0.0, 0.0, 0.42, 0.0).tilt(Math.PI * 2.0).legs(-0.95, 1.7, -0.6, 1.55)
            .rightFrom(v(0.3, -0.3, 0.34), v(0.2, -0.16, 0.12)).leftFrom(v(-0.3, -0.3, 0.34), v(-0.2, -0.16, 0.12));
        Spec rise = key(PaleRules.KICK_LAND + 2.5, "SMOOTH").body(0.0, 0.0, 0.2, 0.0).tilt(Math.PI * 2.0).legs(-0.5, 0.95, -0.4, 0.9).guardArms();
        Spec end = guardKey(PaleRules.KICK_TICKS);
        end.body.flip = (float)(Math.PI * 2.0);
        return List.of(contact, through, hold, tuck, open, land, rise, end);
    }

    /**
     * Watch This (B, 6.2.0): she gathers for a beat (hunched, knees in, fists drawn in to her chest), then flings her arms out wide
     * into a cross as the two tentacles burst out of her back, her chest thrown out and her chin up, her feet planted wide; holds
     * it, savouring it; and settles back into her guard while they hunt.
     */
    static List<Spec> watch() {
        Spec gather = key(3.0, "SMOOTH").body(0.0, 0.0, 0.32, -0.5).legs(-0.42, 0.95, -0.38, 0.95).head(0.18)
            .right(v(0.16, 1.06, 0.3), v(0.36, 1.0, 0.14)).mirrored();
        Spec burst = key(6.0, "OUT").body(0.0, 0.0, -0.2, 0.3).legs(-0.3, 0.45, -0.3, 0.45).head(-0.28)
            .right(v(0.98, 1.36, 0.05), v(0.64, 1.36, 0.03)).mirrored();
        Spec held = key(16.0, "SMOOTH").body(0.0, 0.0, -0.12, 0.2).legs(-0.32, 0.5, -0.32, 0.5).head(-0.18)
            .right(v(0.95, 1.3, 0.1), v(0.62, 1.33, 0.06)).mirrored();
        Spec settle = key(22.0, "SMOOTH").body(0.0, 0.0, 0.12, 0.0).legs(-0.36, 0.72, -0.36, 0.72).guardArms();
        return List.of(gather, burst, held, settle, guardKey(com.hex.kagune.control.pale.WatchRules.POSE_TICKS));
    }

    /**
     * The Flesh Hammer held: up on her left leg, she swings the right one up straight in front of her, all the way up, her whole
     * body tipping back under it, until the leg stands nearly upright over her; the club grows out of the foot on up into the sky.
     * Arms out to balance: the right swept back, the left out ahead. The charge holds this, tipping a little further back.
     */
    static List<Spec> hammerRaise() {
        int up = PaleRules.HAMMER_RAISE_TICKS;
        Spec lifting = key(up * 0.45, "SMOOTH").body(0.0, 0.1, -0.05, -0.4).tilt(0.14).legs(0.02, 0.3, -1.45, 0.15)
            .right(v(0.66, 1.06, 0.04), v(0.62, 1.17, -0.16)).left(v(-0.5, 1.12, 0.26), v(-0.54, 1.08, 0.04));
        Spec raised = key(up, "SMOOTH").body(0.0, 0.12, -0.24, -1.1).tilt(0.42).legs(0.12, 0.32, -2.35, 0.04)
            .right(v(0.55, 0.98, -0.32), v(0.5, 1.2, -0.24)).left(v(-0.5, 1.16, 0.1), v(-0.5, 1.1, -0.14));
        Spec sunk = key(PaleRules.HAMMER_GROW_TICKS, "SMOOTH").body(0.0, 0.14, -0.3, -1.4).tilt(0.5).legs(0.16, 0.42, -2.45, 0.03)
            .right(v(0.55, 0.96, -0.34), v(0.5, 1.18, -0.26)).left(v(-0.5, 1.14, 0.06), v(-0.5, 1.09, -0.17));
        return List.of(lifting, raised, sunk);
    }

    /**
     * The Flesh Hammer let go: the axe kick. First the last bit of wind-up (the leg and the body cocked further back, the arms
     * thrown up), then everything comes down over the leg at once (the body pitched forward over it, the standing knee sinking,
     * the arms thrown down), the club swinging through its whole arc into the ground ahead; she stays down over it while it lies
     * there, then the leg comes back to her and she rises into her guard.
     */
    static List<Spec> hammerSlam() {
        int i = PaleRules.HAMMER_IMPACT;
        Spec cock = key(PaleRules.HAMMER_COCK, "OUT").body(0.0, 0.16, -0.36, -1.7).tilt(0.62).legs(0.24, 0.5, -2.5, 0.0)
            .right(v(0.56, 1.08, -0.3), v(0.5, 1.24, -0.2)).left(v(-0.52, 1.24, -0.02), v(-0.5, 1.12, -0.22));
        Spec slam = key(i, "IN").body(0.0, -0.1, 0.38, 2.4).tilt(-0.3).legs(-0.4, 1.0, -1.0, 0.0)
            .right(v(0.36, 0.72, 0.34), v(0.43, 0.98, 0.2)).left(v(-0.36, 0.74, 0.32), v(-0.43, 0.99, 0.18));
        Spec over = key(i + 0.8, "OUT").body(0.0, -0.12, 0.42, 2.7).tilt(-0.36).legs(-0.42, 1.08, -1.0, 0.0)
            .right(v(0.36, 0.68, 0.36), v(0.43, 0.96, 0.22)).left(v(-0.36, 0.7, 0.34), v(-0.43, 0.97, 0.2));
        Spec down = key(PaleRules.HAMMER_RETRACT_AT, "SMOOTH").body(0.0, -0.08, 0.32, 2.1).tilt(-0.2).legs(-0.32, 0.85, -1.0, 0.05)
            .right(v(0.44, 0.86, -0.06), v(0.46, 1.05, -0.26)).left(v(-0.44, 0.9, 0.18), v(-0.47, 1.06, -0.04));
        Spec back = key(PaleRules.HAMMER_RETRACT_AT + 8.0, "SMOOTH").body(0.0, 0.0, 0.14, 0.4).legs(-0.32, 0.6, -0.28, 0.6)
            .right(v(0.4, 1.02, 0.18), v(0.46, 1.06, -0.06)).left(v(-0.4, 1.02, 0.18), v(-0.46, 1.06, -0.06));
        return List.of(cock, slam, over, down, back, guardKey(PaleRules.HAMMER_TICKS));
    }

    static List<Spec> choreography(PaleStrike strike) {
        return switch (strike) {
            case JAB -> straight(strike, 0.62);
            case CROSS -> straight(strike, 1.0);
            case HOOK -> hook(strike);
            case LIVER -> liver(strike);
            case UPPERCUT -> uppercut(strike);
            case BLINK_PALM -> palm(strike);
            case BACKFIST -> backfist(strike);
            case TWIN_PALM -> twinPalm(strike);
        };
    }

    // ------------------------------------------------------------------ solving

    /** How far (pixels) short of the body an arm is kept (the tests allow a quarter pixel into it; the blends between keys need room). */
    static final double SPARE = -0.4;

    static float drop(RigPose p) {
        return Math.max(0.0F, Math.min(RigPose.legDrop(p.rightLegX, p.rightLegZ, p.rightKnee), RigPose.legDrop(p.leftLegX, p.leftLegZ, p.leftKnee)));
    }

    static double[] world(RigPose p, boolean right, double y) {
        return BodyFrame.world(p, BodyFrame.model(p, right ? "rightArm" : "leftArm", new double[] {right ? -1 : 1, y, 0}));
    }

    static void setArm(RigPose p, boolean right, double[] v) {
        if (right) {
            p.rightArmX = (float)v[0];
            p.rightArmY = (float)v[1];
            p.rightArmZ = (float)v[2];
            p.rightElbow = (float)v[3];
        } else {
            p.leftArmX = (float)v[0];
            p.leftArmY = (float)v[1];
            p.leftArmZ = (float)v[2];
            p.leftElbow = (float)v[3];
        }
    }

    static double d2(double[] a, double[] b) {
        double x = a[0] - b[0];
        double y = a[1] - b[1];
        double z = a[2] - b[2];
        return x * x + y * y + z * z;
    }

    /** {@code p} with only one arm (the other hanging, which never touches the body), settled as the game settles it. */
    static RigPose oneArm(RigPose p, boolean right) {
        RigPose q = new RigPose().add(p, 1.0F);
        q.drop = p.drop;
        setArm(q, !right, new double[] {0, 0, 0, 0});
        q.settle(1.0F);
        return q;
    }

    /**
     * The arm's angles in the key before (null: none): a key is solved starting from them and held near them, so one strike's keys
     * are all the same shape of arm (never the same fist reached with the arm twisted the other way round, which a blend between
     * the two would swing through the body).
     */
    static double[] near;
    static final double NEAR_WEIGHT = 0.012;

    static double cost(RigPose p, boolean right, double[] v, double[] fist, double[] elbow, boolean clip) {
        setArm(p, right, v);
        double c = d2(world(p, right, 9), fist) + (elbow == null ? 0.0 : 0.4 * d2(world(p, right, 4), elbow));
        if (near != null) {
            c += NEAR_WEIGHT * ((v[0] - near[0]) * (v[0] - near[0]) + (v[1] - near[1]) * (v[1] - near[1]) + (v[2] - near[2]) * (v[2] - near[2])
                + (v[3] - near[3]) * (v[3] - near[3]));
        }
        // Joints as joints bend: an elbow folds one way, an arm turns only so far about itself or out to the side.
        // (An elbow folded past about 105 degrees, or an upper arm swung behind her, reaches the point but reads as a broken wing.)
        c += excess(-v[3], 0.0) + excess(v[3], 1.85) + excess(Math.abs(v[1]), 1.3) + excess(v[2], 1.4) + excess(-v[2], 0.9) + excess(-v[0], 3.1)
            + excess(v[0], 1.0);
        if (clip) {
            // Never into the body, with a hair to spare (an arm resting against the torso's side is how arms hang: that is fine).
            double into = BodyFrame.clipping(oneArm(p, right), SPARE);
            c += 4.0 * into * into;
        }
        return c;
    }

    static double excess(double value, double limit) {
        return value > limit ? (value - limit) * (value - limit) * 4.0 : 0.0;
    }

    /** Solves one arm of {@code p} in place onto the targets; returns {fist miss (blocks), clearance shortfall (pixels)}. */
    static double[] solve(RigPose p, boolean right, double[] fist, double[] elbow) {
        Random random = new Random(right ? 7 : 11);
        List<double[]> found = new ArrayList<>();
        for (int start = 0; start < 40; start++) {
            double[] v = start == 0 && near != null ? near.clone()
                : new double[] {-3.0 * random.nextDouble(), (random.nextDouble() - 0.5) * 2.4, (random.nextDouble() - 0.5) * 2.4, 2.0 * random.nextDouble()};
            v = nelder(p, right, v, fist, elbow, false);
            found.add(v);
        }
        found.sort((a, b) -> Double.compare(cost(p, right, a, fist, elbow, true), cost(p, right, b, fist, elbow, true)));
        double[] best = null;
        double bestCost = Double.MAX_VALUE;
        for (int k = 0; k < 4; k++) {
            double[] v = nelder(p, right, found.get(k), fist, elbow, true);
            double c = cost(p, right, v, fist, elbow, true);
            if (c < bestCost) {
                bestCost = c;
                best = v;
            }
        }
        setArm(p, right, best);
        return new double[] {Math.sqrt(d2(world(p, right, 9), fist)), BodyFrame.clipping(oneArm(p, right), 0.0)};
    }

    static double[] nelder(RigPose p, boolean right, double[] start, double[] fist, double[] elbow, boolean clip) {
        int n = 4;
        double[][] s = new double[n + 1][];
        double[] f = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            s[i] = start.clone();
            if (i > 0) {
                s[i][i - 1] += 0.2;
            }
            f[i] = cost(p, right, s[i], fist, elbow, clip);
        }
        for (int iteration = 0; iteration < (clip ? 260 : 500); iteration++) {
            for (int a = 0; a <= n; a++) {
                for (int b = a + 1; b <= n; b++) {
                    if (f[b] < f[a]) {
                        double t = f[a];
                        f[a] = f[b];
                        f[b] = t;
                        double[] u = s[a];
                        s[a] = s[b];
                        s[b] = u;
                    }
                }
            }
            if (f[n] - f[0] < 1.0E-13) {
                break;
            }
            double[] c = new double[n];
            for (int i = 0; i < n; i++) {
                for (int k = 0; k < n; k++) {
                    c[k] += s[i][k] / n;
                }
            }
            double[] xr = along(c, s[n], -1.0);
            double fr = cost(p, right, xr, fist, elbow, clip);
            if (fr < f[0]) {
                double[] xe = along(c, s[n], -2.0);
                double fe = cost(p, right, xe, fist, elbow, clip);
                s[n] = fe < fr ? xe : xr;
                f[n] = Math.min(fe, fr);
            } else if (fr < f[n - 1]) {
                s[n] = xr;
                f[n] = fr;
            } else {
                double[] xc = along(c, s[n], 0.5);
                double fc = cost(p, right, xc, fist, elbow, clip);
                if (fc < f[n]) {
                    s[n] = xc;
                    f[n] = fc;
                } else {
                    for (int i = 1; i <= n; i++) {
                        s[i] = along(s[0], s[i], 0.5);
                        f[i] = cost(p, right, s[i], fist, elbow, clip);
                    }
                }
            }
        }
        int best = 0;
        for (int i = 1; i <= n; i++) {
            if (f[i] < f[best]) {
                best = i;
            }
        }
        return s[best];
    }

    /** {@code from + t (to - from)}. */
    static double[] along(double[] from, double[] to, double t) {
        double[] out = new double[from.length];
        for (int k = 0; k < out.length; k++) {
            out[k] = from[k] + t * (to[k] - from[k]);
        }
        return out;
    }

    static double wrap(double angle) {
        double turn = Math.PI * 2.0;
        return angle - turn * Math.floor((angle + Math.PI) / turn);
    }

    /** A point {@code offset} from a shoulder of {@code p} (blocks, in her turned body's frame), in the move's frame. */
    static double[] fromShoulder(RigPose p, boolean right, double[] offset) {
        double[] shoulder = BodyFrame.world(p, BodyFrame.model(p, right ? "rightArm" : "leftArm", new double[] {right ? -1 : 1, 0, 0}));
        double[] turned = w(p.spin, offset[0], 0.0, offset[2]);
        return new double[] {shoulder[0] + turned[0], shoulder[1] + offset[1], shoulder[2] + turned[2]};
    }

    /** Solves a key's arms (the guard's for a guard key, the other key's for a copy). */
    static RigPose pose(Spec spec, RigPose guard, RigPose previous, StringBuilder report, String name) {
        if (spec.guard) {
            RigPose p = new RigPose().add(guard, 1.0F);
            p.spin = spec.body.spin;
            p.flip = spec.body.flip;
            return p;
        }
        RigPose p = new RigPose().add(spec.body, 1.0F);
        p.drop = spec.airborne ? 0.0F : drop(p);
        if (spec.guardArms) {
            p.rightArmX = guard.rightArmX;
            p.rightArmY = guard.rightArmY;
            p.rightArmZ = guard.rightArmZ;
            p.rightElbow = guard.rightElbow;
            p.leftArmX = guard.leftArmX;
            p.leftArmY = guard.leftArmY;
            p.leftArmZ = guard.leftArmZ;
            p.leftElbow = guard.leftElbow;
        } else if (spec.copyArmsFrom != null) {
            RigPose from = spec.copyArmsFrom.solved;
            p.rightArmX = from.rightArmX;
            p.rightArmY = from.rightArmY;
            p.rightArmZ = from.rightArmZ;
            p.rightElbow = from.rightElbow + spec.elbowAdd;
            p.leftArmX = from.leftArmX;
            p.leftArmY = from.leftArmY;
            p.leftArmZ = from.leftArmZ;
            p.leftElbow = from.leftElbow + (from.leftElbow > 0.3F ? 0.0F : spec.elbowAdd);
        } else {
            if (spec.rightFrom != null) {
                spec.rightFist = fromShoulder(p, true, spec.rightFrom);
                spec.rightElbow = spec.rightElbowFrom == null ? null : fromShoulder(p, true, spec.rightElbowFrom);
            }
            if (spec.leftFrom != null) {
                spec.leftFist = fromShoulder(p, false, spec.leftFrom);
                spec.leftElbow = spec.leftElbowFrom == null ? null : fromShoulder(p, false, spec.leftElbowFrom);
            }
            for (boolean right : new boolean[] {true, false}) {
                double[] fist = right ? spec.rightFist : spec.leftFist;
                if (fist == null) {
                    continue;
                }
                near = previous == null ? null : right ? new double[] {previous.rightArmX, previous.rightArmY, previous.rightArmZ, previous.rightElbow}
                    : new double[] {previous.leftArmX, previous.leftArmY, previous.leftArmZ, previous.leftElbow};
                double[] result = solve(p, right, fist, right ? spec.rightElbow : spec.leftElbow);
                near = null;
                double[] f = world(p, right, 9);
                double[] e = world(p, right, 4);
                report.append(String.format(Locale.ROOT, "  %-12s %s fist (%5.2f %5.2f %5.2f) off %.2f, elbow (%5.2f %5.2f %5.2f)%s%n", name,
                    right ? "R" : "L", f[0], f[1], f[2], result[0], e[0], e[1], e[2],
                    result[1] > 0.0 ? String.format(Locale.ROOT, ", %.2f px INTO the body", result[1]) : ""));
                if (right && spec.mirrored) {
                    // Both arms alike: the left is the right one's mirror image.
                    p.leftArmX = p.rightArmX;
                    p.leftArmY = -p.rightArmY;
                    p.leftArmZ = -p.rightArmZ;
                    p.leftElbow = p.rightElbow;
                    break;
                }
            }
        }
        p.drop = 0.0F;
        return p;
    }

    // ------------------------------------------------------------------ output

    static final String[] CHANNELS = {"headX", "headY", "headZ", "lean", "side", "twist", "lunge", "spin", "flip", "roll", "rightArmX", "rightArmY", "rightArmZ",
        "rightElbow", "rightShoulderY", "rightShoulderZ", "leftArmX", "leftArmY", "leftArmZ", "leftElbow", "leftShoulderY", "leftShoulderZ",
        "rightLegX", "rightLegY", "rightLegZ", "rightKnee", "leftLegX", "leftLegY", "leftLegZ", "leftKnee", "rightKneeTurn", "leftKneeTurn",
        "armHold", "legHold"};

    static float channel(RigPose p, String name) {
        try {
            return RigPose.class.getField(name).getFloat(p);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static String method(String name, RigPose p) {
        StringBuilder out = new StringBuilder();
        out.append("    static RigPose ").append(name).append("() {\n        RigPose p = new RigPose();\n");
        StringBuilder line = new StringBuilder("       ");
        for (String channel : CHANNELS) {
            float value = channel(p, channel);
            if (Math.abs(value) < 5.0E-4F) {
                continue;
            }
            String part = String.format(Locale.ROOT, " p.%s = %.3fF;", channel, value);
            if (line.length() + part.length() > 128) {
                out.append(line).append('\n');
                line = new StringBuilder("       ");
            }
            line.append(part);
        }
        if (line.length() > 7) {
            out.append(line).append('\n');
        }
        out.append("        return p;\n    }\n");
        return out.toString();
    }

    /**
     * One move's keys as Java ({@code new PaleMotions.Key[] {...}}), each key's pose solved (seeded from the one before, the first
     * from {@code first}) and written out as a method named {@code base} and its number.
     */
    static String sequence(String base, List<Spec> specs, RigPose guard, RigPose first, StringBuilder methods, StringBuilder report) {
        StringBuilder out = new StringBuilder("new PaleMotions.Key[] {PaleMotions.Key.START");
        RigPose previous = first;
        for (int k = 0; k < specs.size(); k++) {
            Spec spec = specs.get(k);
            String name = base + (k + 1);
            spec.solved = pose(spec, guard, spec.fresh ? guard : previous, report, name);
            previous = spec.solved;
            String poseCall;
            if (spec.guard) {
                poseCall = spec.body.flip != 0.0F ? String.format(Locale.ROOT, "flipped(guard(), %.6fF)", spec.body.flip)
                    : spec.body.spin == 0.0F ? "guard()" : String.format(Locale.ROOT, "spun(guard(), %.6fF)", spec.body.spin);
            } else {
                methods.append('\n').append(method(name, spec.solved));
                poseCall = name + "()";
            }
            out.append(",\n                new PaleMotions.Key(").append(String.format(Locale.ROOT, "%.2fF", spec.at)).append(", ").append(poseCall)
                .append(", PaleMotions.Ease.").append(spec.ease).append(')');
        }
        return out.append('}').toString();
    }

    public static void main(String[] args) throws Exception {
        File target = new File(args.length > 0 ? args[0] : "src/main/java/com/hex/kagune/control/client/animation/PaleStrikePoses.java");
        StringBuilder report = new StringBuilder();
        Spec guardSpec = guard();
        RigPose guard = pose(guardSpec, null, null, report, "guard");
        StringBuilder methods = new StringBuilder(method("guard", guard));
        StringBuilder table = new StringBuilder();
        for (PaleStrike strike : PaleStrike.values()) {
            table.append("            case ").append(strike.name()).append(" -> ");
            table.append(sequence(strike.name().toLowerCase(Locale.ROOT).replace("_", ""), choreography(strike), guard, guard, methods, report))
                .append(";\n");
        }
        // Big Punch: its charge (the coil at no charge and at full) and the punch out of it.
        Spec coil0 = punchCoil(0.0);
        Spec coil1 = punchCoil(1.0);
        coil0.solved = pose(coil0, guard, guard, report, "punchCoil0");
        coil1.solved = pose(coil1, guard, coil0.solved, report, "punchCoil1");
        methods.append('\n').append(method("punchCoil0", coil0.solved)).append('\n').append(method("punchCoil1", coil1.solved));
        String punch = sequence("bigpunch", bigPunch(), guard, coil1.solved, methods, report);
        String swipe = sequence("swipe", swipe(), guard, guard, methods, report);
        String reach = sequence("grabreach", grabReach(), guard, guard, methods, report);
        String hold = sequence("grabhold", grabHold(), guard, guard, methods, report);
        String flingKeys = sequence("grabthrow", grabThrow(), guard, guard, methods, report);
        List<Spec> rush = kickRush();
        String rushKeys = sequence("kickrush", rush, guard, guard, methods, report);
        Spec chamber = kickChamber();
        chamber.solved = pose(chamber, guard, rush.get(rush.size() - 1).solved, report, "kickChamber");
        methods.append('\n').append(method("kickChamber", chamber.solved));
        String kickKeys = sequence("kick", kick(), guard, chamber.solved, methods, report);
        String watchKeys = sequence("watch", watch(), guard, guard, methods, report);
        List<Spec> raise = hammerRaise();
        String raiseKeys = sequence("hammerraise", raise, guard, guard, methods, report);
        String slamKeys = sequence("hammerslam", hammerSlam(), guard, raise.get(raise.size() - 1).solved, methods, report);
        String source = "package com.hex.kagune.control.client.animation;\n\n"
            + "import com.hex.kagune.control.pale.PaleStrike;\n\n"
            + "/**\n"
            + " * GENERATED by tools/PalePoseDesign.java (tools/pale-pose-design.sh): Pale's strike keys, the arms solved onto the fist and elbow\n"
            + " * targets choreographed there. Edit the choreography there, never this file. Strikes are right-handed here; {@link PaleMotions}\n"
            + " * mirrors the left ones.\n"
            + " */\n"
            + "final class PaleStrikePoses {\n"
            + "    private PaleStrikePoses() {\n    }\n\n"
            + "    /** A strike's keys after the pose it began from: tick, pose, how the motion eases into it. */\n"
            + "    static PaleMotions.Key[] keys(PaleStrike strike) {\n"
            + "        return switch (strike) {\n" + table + "        };\n    }\n\n"
            + "    /** Big Punch's keys after the pose it was let go from (its charge). */\n"
            + "    static PaleMotions.Key[] bigPunch() {\n        return " + punch + ";\n    }\n\n"
            + "    /** The flesh swipe's keys. */\n"
            + "    static PaleMotions.Key[] swipe() {\n        return " + swipe + ";\n    }\n\n"
            + "    /** The flesh grab reaching out (held once reached). */\n"
            + "    static PaleMotions.Key[] grabReach() {\n        return " + reach + ";\n    }\n\n"
            + "    /** Holding what the flesh caught. */\n"
            + "    static PaleMotions.Key[] grabHold() {\n        return " + hold + ";\n    }\n\n"
            + "    /** The throw. */\n"
            + "    static PaleMotions.Key[] grabThrow() {\n        return " + flingKeys + ";\n    }\n\n"
            + "    /** The Speed Kick's lunge into the dash. */\n"
            + "    static PaleMotions.Key[] kickRush() {\n        return " + rushKeys + ";\n    }\n\n"
            + "    /** The Speed Kick, after the chamber she is in when she is there. */\n"
            + "    static PaleMotions.Key[] kick() {\n        return " + kickKeys + ";\n    }\n\n"
            + "    /** Watch This: her cross as the tentacles burst out. */\n"
            + "    static PaleMotions.Key[] watch() {\n        return " + watchKeys + ";\n    }\n\n"
            + "    /** The Flesh Hammer held: the leg raised, the body tipped back under it. */\n"
            + "    static PaleMotions.Key[] hammerRaise() {\n        return " + raiseKeys + ";\n    }\n\n"
            + "    /** The Flesh Hammer let go: the axe kick down. */\n"
            + "    static PaleMotions.Key[] hammerSlam() {\n        return " + slamKeys + ";\n    }\n\n"
            + "    /** {@code pose} turned {@code spin} about the vertical (a turn she ends a strike with). */\n"
            + "    static RigPose spun(RigPose pose, float spin) {\n        pose.spin = spin;\n        return pose;\n    }\n\n"
            + "    /** {@code pose} turned {@code flip} head over heels (a flip she ends a move with). */\n"
            + "    static RigPose flipped(RigPose pose, float flip) {\n        pose.flip = flip;\n        return pose;\n    }\n\n"
            + methods + "}\n";
        Files.writeString(target.toPath(), source, StandardCharsets.UTF_8);
        System.out.print(report);
        System.out.println("wrote " + target);
    }
}
