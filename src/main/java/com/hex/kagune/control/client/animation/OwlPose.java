package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.eto.OwlActs;
import com.hex.kagune.control.eto.OwlModel;
import com.hex.kagune.control.eto.OwlRules;
import com.hex.kagune.control.kakugan.KakuganEyes;

/**
 * 6.2.0: the Owl (Eto's first kakuja) placed on her posed body (pure, tested): the mask on her head as the game draws it, the
 * muscle and spikes on her torso, and the two giant arms swung from her shoulders ({@link Swing}).
 */
public final class OwlPose {
    /**
     * One giant arm's swing (radians, in her torso's own frame): the upper arm off the shoulder, how far out to her side and how far
     * forward (+; back -); the forearm-blade off the elbow, the same two; and how far (blocks) the blade bows up out of its line.
     */
    public record Swing(double out, double forward, double bladeOut, double bladeForward, double bow) {
    }


    /** 6.2.4: how far out (radians) and forward each upper arm is raised at rest ({@link #REST}). */
    static final double REST_OUT = 1.2;
    static final double REST_FORWARD = 0.2;

    /** In it, how far (radians) her heavy head turns either way, tips back and nods down: her look past that is held back. */
    public static final double LOOK_YAW = 0.75;
    public static final double LOOK_UP = 0.4;
    public static final double LOOK_DOWN = 0.35;

    /**
     * Her look held within what her heavy head allows in it ({@code yaw} her head turned off her body and {@code pitch} down, radians,
     * as vanilla turns it under the pose): the offsets holding it back put on {@code p}, as much as she is in it ({@code weight});
     * turned aside, it nods down less.
     */
    public static void neck(RigPose p, double yaw, double pitch, double weight) {
        if (!Double.isFinite(yaw) || !Double.isFinite(pitch) || !(weight > 0.0)) {
            return;
        }
        double held = Math.min(1.0, weight);
        double turned = Math.max(-LOOK_YAW, Math.min(LOOK_YAW, yaw));
        // Turned, the chin comes round over her shoulder: it nods down less the further she looks aside.
        double down = LOOK_DOWN * (1.0 - 0.6 * Math.abs(turned) / LOOK_YAW);
        p.headY += (float)((turned - yaw) * held);
        p.headX += (float)((Math.max(-LOOK_UP, Math.min(down, pitch)) - pitch) * held);
    }

    /** Her breathing in it (radians a tick): slow and heavy, the giant arms heaving with it. */
    static final double BREATH = 0.11;
    /** Now and then (every so many ticks) the blades flick out and back, over so many ticks: restless. */
    public static final double FLICK_EVERY = 97.0;
    public static final double FLICK_LENGTH = 9.0;

    /**
     * One giant arm's swing ({@code side} 1 her right, -1 her left) at {@code time} (ticks), walking at {@code walk} (0 still, 1 at
     * a run) {@code stride} into her stride (radians: the game's walk position by its 0.6662): heaving up on each breath, swaying
     * slowly like a predator's tail, and now and then flicking out and back; walking, the arm swinging against the leg on its side
     * (as an arm swings), both lifted clear the faster she goes. The muscle moves it all: the blade rides on the upper arm, only
     * flexing a little at the elbow on its own.
     */
    public static Swing swing(int side, double time, double walk, double stride) {
        double breath = Math.sin(time * BREATH);
        double sway = Math.sin(time * 0.047 + side * 1.9);
        double drift = Math.sin(time * 0.031 + side * 0.7);
        double flex = Math.sin(time * 0.13 + side * 2.3);
        double flick = flick(time + (side > 0 ? 0.0 : FLICK_EVERY * 0.43));
        double w = Double.isFinite(walk) ? Math.max(0.0, Math.min(1.0, walk)) : 0.0;
        // Lifted clear as soon as she moves at all.
        double lift = w * (2.0 - w);
        double arm = side * Math.cos(stride);
        // The blade trails the arm's swing a little at the elbow, and opens a touch with each flick.
        double trail = side * Math.cos(stride - 1.2);
        return carry(REST.out() + 0.06 * breath + 0.03 * sway + 0.2 * flick + 0.25 * lift, REST.forward() + 0.07 * sway + 0.36 * w * arm,
            REST_ELBOW[0] + 0.04 * flex + 0.06 * flick - 0.08 * lift, REST_ELBOW[1] + 0.06 * drift - 0.12 * w * trail,
            REST.bow() + 0.03 * flex + 0.05 * flick, REST.bladeForward());
    }

    /** How far into a flick (0..1) the blades are at {@code time}: out fast, back slower, once every {@link #FLICK_EVERY} ticks. */
    public static double flick(double time) {
        double t = time % FLICK_EVERY;
        if (t < 0.0) {
            t += FLICK_EVERY;
        }
        if (!(t < FLICK_LENGTH)) {
            return 0.0;
        }
        double u = t / FLICK_LENGTH;
        return u < 0.25 ? smooth(u / 0.25) : 1.0 - smooth((u - 0.25) / 0.75);
    }

    /**
     * The shoulder joint each giant arm turns about (torso pixels, her right side; x mirrored for the left): up on top of her own
     * shoulder (bulked and pushed out, as HexHaki's haki physique draws it), clear of the mask. Its x rides out with the bulk
     * ({@link RigPose#bulk}): this is at the full bulk.
     */
    static final double[] SHOULDER = {-9.4, -1.9, 0.7};

    private OwlPose() {
    }

    /** Her own body standing in the Owl: her arms hanging a little out from her side. */
    /**
     * 6.2.4: her body as the creature is carried over from through one of its own moves (its act moves it, OwlActs; never her own
     * old choreography under it): the Owl's stance, breathing, at {@code time}, her look ({@code lookYaw}, {@code lookPitch}, as
     * vanilla turns her head) on it, all of it thrown {@code turn} (radians, + to her right) the way the move was, settled.
     */
    public static RigPose acting(double time, double lookYaw, double lookPitch, double turn) {
        RigPose p = stance();
        body(p, true, 1000.0, time);
        neck(p, lookYaw, lookPitch, 1.0);
        p.headY += (float)lookYaw;
        p.headX += (float)lookPitch;
        p.spin -= (float)turn;
        p.headY -= (float)turn;
        p.settle(1.0F);
        return p;
    }

    public static RigPose stance() {
        RigPose pose = new RigPose();
        pose.rightArmZ = ARMS_OUT;
        pose.leftArmZ = -ARMS_OUT;
        pose.rightElbow = 0.25F;
        pose.leftElbow = 0.25F;
        pose.bulk = 1.0F;
        return pose;
    }

    // ------------------------------------------------------------------ growing it, turning back

    /** A key of her body as it grows or goes: offsets onto her pose. */
    record Key(double lean, double drop, double headX, double armX, double armZ, double elbow) {
        Key lerp(Key b, double t) {
            return new Key(this.lean + (b.lean - this.lean) * t, this.drop + (b.drop - this.drop) * t, this.headX + (b.headX - this.headX) * t,
                this.armX + (b.armX - this.armX) * t, this.armZ + (b.armZ - this.armZ) * t, this.elbow + (b.elbow - this.elbow) * t);
        }
    }

    static final Key NONE = new Key(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    /** Doubled over as it tears its way out: hunched over her middle, head down, arms pulled in tight, knees giving. */
    static final Key HUNCH = new Key(0.55, 2.2, 0.35, -0.45, -0.02, 1.25);
    /** The burst: thrown back by it, head back, arms flung out and down behind her. */
    static final Key BURST = new Key(-0.3, 0.0, -0.45, 0.35, 0.75, 0.1);
    /** In it (6.2.4, as the reference stoops): hunched well forward under its weight, her head held up to look ahead, arms hanging. */
    static final Key OWL = new Key(0.34, 0.7, -0.14, 0.12, OwlPose.ARMS_OUT, 0.3);
    /** Turning back: bent over as it draws in. */
    static final Key DRAWN = new Key(0.35, 1.4, 0.25, -0.25, 0.0, 0.7);

    /** Her body as the Owl grows on her ({@code on}) or goes, {@code age} ticks in. */
    static Key key(boolean on, double age) {
        if (on) {
            double a = smooth(age / 7.0);
            double b = smooth((age - (OwlRules.BURST - 1.0)) / 3.0);
            // The roar held while the limbs tear out and unfold, then settling into its stoop.
            double c = smooth((age - (OwlRules.BURST + 7.0)) / 12.0);
            return NONE.lerp(HUNCH, a).lerp(BURST, b).lerp(OWL, c);
        }
        double a = smooth(age / 6.0);
        double b = smooth((age - 8.0) / (OwlRules.SHED_TICKS - 8.0));
        return OWL.lerp(DRAWN, a).lerp(NONE, b);
    }

    /** How much of her own playful idle is gone under it (0..1): all of it in it, back as it goes. */
    public static double weight(boolean on, double age) {
        return on ? smooth(age / 2.5) : 1.0 - smooth((age - 8.0) / (OwlRules.SHED_TICKS - 8.0));
    }

    /**
     * Her body as the Owl grows on her ({@code on}) or goes, {@code age} ticks in, at {@code time}: onto {@code p}, whatever else she
     * was doing, her arms taken over by it (held still while it tears out of her or draws in, swinging as she walks once it is
     * out), her body bulked up under it as it fills in, breathing heavily in it; the shaking as it tears its way out.
     */
    public static void body(RigPose p, boolean on, double age, double time) {
        Key k = key(on, age);
        double t = weight(on, age);
        double held = t * (on ? 1.0 - smooth((age - (OwlRules.GROW_TICKS - 6.0)) / 6.0) : 1.0);
        double in = on ? smooth((age - OwlRules.BURST) / 6.0) : 1.0 - smooth((age - 8.0) / (OwlRules.SHED_TICKS - 8.0));
        double tear = on ? smooth(age / 3.0) * (1.0 - smooth((age - OwlRules.BURST) / 2.0)) : 0.0;
        double breath = Math.sin(time * BREATH);
        p.lean += (float)(k.lean() + in * 0.025 * breath);
        p.drop += (float)(k.drop() + in * 0.25 * breath);
        p.headX += (float)k.headX();
        p.twist += (float)(0.06 * tear * Math.sin(age * 2.9));
        p.side += (float)(0.04 * tear * Math.sin(age * 3.7 + 1.0));
        p.rightArmX = mix(p.rightArmX, k.armX() + 0.05 * tear * Math.sin(age * 4.1), t);
        p.leftArmX = mix(p.leftArmX, k.armX() + 0.05 * tear * Math.sin(age * 3.3 + 2.0), t);
        p.rightArmY = mix(p.rightArmY, 0.0, t);
        p.leftArmY = mix(p.leftArmY, 0.0, t);
        // Taken over round her sides, never in across her (from arms up over her head, say).
        double round = SWING_ROUND * Math.sin(Math.PI * t);
        p.rightArmZ = mix(p.rightArmZ, k.armZ(), t) + (float)round;
        p.leftArmZ = mix(p.leftArmZ, -k.armZ(), t) - (float)round;
        p.rightElbow = mix(p.rightElbow, k.elbow(), t);
        p.leftElbow = mix(p.leftElbow, k.elbow(), t);
        p.rightShoulderY = mix(p.rightShoulderY, 0.0, t);
        p.rightShoulderZ = mix(p.rightShoulderZ, 0.0, t);
        p.leftShoulderY = mix(p.leftShoulderY, 0.0, t);
        p.leftShoulderZ = mix(p.leftShoulderZ, 0.0, t);
        p.armHold = mix(p.armHold, 1.0, held);
        p.bulk = (float)OwlRules.bulk(OwlRules.grown(on, age));
    }

    /** How far out (radians) her arms swing round as it takes them over. */
    static final double SWING_ROUND = 0.35;

    static float mix(float a, double b, double t) {
        return (float)(a + (b - a) * t);
    }

    /** How far (radians) her own arms hang out from her side in the Owl. */
    static final float ARMS_OUT = 0.08F;

    /**
     * The Owl on {@code pose}, the mask's socket over the eye her kakugan is in ({@code eyes}; both eyes reads as her right), the
     * arms swung {@code right} and {@code left}, at {@code time} (ticks), grown out {@code grown} (0..1).
     */
    public static OwlModel.Look look(RigPose pose, KakuganEyes eyes, Swing right, Swing left, double time, double grown) {
        OwlModel.Frame head = frame(pose, "head");
        OwlModel.Frame torso = frame(pose, "torso");
        boolean rightEye = eyes.ghoul() != KakuganEyes.LEFT;
        double bulk = Float.isFinite(pose.bulk) ? Math.max(0.0, Math.min(1.0, pose.bulk)) : 0.0;
        return new OwlModel.Look(head, torso, arm(torso, right, 1, bulk), arm(torso, left, -1, bulk), rightEye, eyeX(eyes, rightEye),
            eyeY(eyes, rightEye), eyeX(eyes, !rightEye), eyeY(eyes, !rightEye), time, grown, inner(torso, right, 1, bulk, time),
            inner(torso, left, -1, bulk, time), body(pose, bulk));
    }

    // ------------------------------------------------------------------ 6.2.4: the inner limbs, her body under it

    /**
     * Where each inner limb grows out (torso pixels, her right side; x mirrored for the left): off her shoulder blade, behind and
     * inside the outer limb's root and higher. Its x rides out with the bulk as the outer's does.
     */
    static final double[] INNER_SHOULDER = {-6.8, -2.2, 2.5};
    /** At rest the inner upper rises up and out off her back, a little behind her, its blade hanging straight down off its knuckle. */
    static final double[] INNER_UP = {-0.62, -0.62, 0.72};
    static final double[] INNER_DOWN = {-0.22, 1.0, 0.18};
    /** How much of the outer limb's motion off its rest each inner one follows. */
    public static final double INNER_FOLLOW = 0.35;

    /**
     * 6.2.4: one inner limb ({@code side}), following {@link #INNER_FOLLOW} of what the outer one swung {@code outer} does off its rest
     * (its upper turned by that share of the outer upper's turn, its blade by that share of the outer blade's), breathing on its own.
     */
    static OwlModel.Arm inner(OwlModel.Frame torso, Swing outer, int side, double bulk, double time) {
        double x = INNER_SHOULDER[0] + RigPose.BULK_SPREAD * (1.0 - bulk) * 0.6;
        return inner(torso, outer, side, torso.at(x * side, INNER_SHOULDER[1], INNER_SHOULDER[2]), time);
    }

    /** The same grown out of {@code shoulder} (rig space; the creature's root on its yoke). */
    static OwlModel.Arm inner(OwlModel.Frame torso, Swing outer, int side, double[] shoulder, double time) {
        double breath = Math.sin(time * BREATH + 1.4);
        double[] restUpper = torso.dir(INNER_UP[0] * side, INNER_UP[1] - 0.06 * breath, INNER_UP[2]);
        double[] restBlade = torso.dir(INNER_DOWN[0] * side, INNER_DOWN[1], INNER_DOWN[2] + 0.05 * breath);
        double[] upper = turnedBy(restUpper, direction(torso, REST.out(), REST.forward(), side), direction(torso, outer.out(), outer.forward(), side),
            INNER_FOLLOW);
        double[] blade = turnedBy(restBlade, direction(torso, REST.bladeOut(), REST.bladeForward(), side),
            direction(torso, outer.bladeOut(), outer.bladeForward(), side), INNER_FOLLOW);
        double[] elbow = add(shoulder, mul(upper, OwlModel.INNER_UPPER));
        double[] tip = add(elbow, mul(blade, OwlModel.INNER_FORE));
        return new OwlModel.Arm(shoulder, elbow, tip, torso.dir(0.0, 0.0, -1.0), 0.16 + 0.1 * Math.abs(outer.bow() - REST.bow()), bend(torso, blade, side));
    }

    /**
     * 6.2.4: her body as posed ({@code pose}, settled), as the creature is carried over from it ({@link com.hex.kagune.control.eto.OwlCreature}):
     * her torso and head, the way each upper arm and forearm goes, each hand, each foot; walking {@code walk} (0..1) {@code stride}
     * into her stride, off the ground or not, at {@code time}, the Owl grown {@code grown}.
     */
    public static com.hex.kagune.control.eto.OwlCreature.Source source(RigPose pose, double walk, double stride, boolean airborne, double time,
                                                                       double grown) {
        double[][] arms = new double[4][];
        int i = 0;
        for (String part : new String[] {"rightArm", "leftArm"}) {
            double cx = part.equals("rightArm") ? -1.0 : 1.0;
            double[] shoulder = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {cx, 0.0, 0.0}));
            double[] elbow = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {cx, 4.0, 0.0}));
            double[] hand = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {cx, 10.0, 0.0}));
            arms[i++] = normalize(sub(elbow, shoulder));
            arms[i++] = normalize(sub(hand, elbow));
        }
        return new com.hex.kagune.control.eto.OwlCreature.Source(frame(pose, "torso"), frame(pose, "head"), arms[0], arms[1], arms[2], arms[3],
            frameAt(pose, "rightArm", -1.0, 10.0, 0.0, 1.0), frameAt(pose, "leftArm", 1.0, 10.0, 0.0, 1.0),
            BodyFrame.world(pose, BodyFrame.model(pose, "rightLeg", new double[] {0.0, 12.0, 0.0})),
            BodyFrame.world(pose, BodyFrame.model(pose, "leftLeg", new double[] {0.0, 12.0, 0.0})), walk, stride, airborne, time, grown);
    }

    /**
     * 6.2.4: the kagune and the mask on the creature ({@code rig}): the four limbs swung {@code right} and {@code left} the ways
     * they are off her torso (the frame turned as hers is, {@link com.hex.kagune.control.eto.OwlCreature.Rig#kagune}), each out of
     * its own root on the creature's yoke; the mask on its head. Nothing of her own body: the creature is drawn in its place.
     */
    public static OwlModel.Look look(com.hex.kagune.control.eto.OwlCreature.Rig rig, Swing right, Swing left, double time, double grown) {
        return look(rig, right, left, time, grown, null);
    }

    /** The same through the creature's act ({@code act}, null: none): driving its points in, its inner limbs drive in with the outer. */
    public static OwlModel.Look look(com.hex.kagune.control.eto.OwlCreature.Rig rig, Swing right, Swing left, double time, double grown,
                                     OwlActs.Act act) {
        OwlModel.Frame k = rig.kagune();
        OwlModel.Arm outerRight = held(k, arm(k, right, 1, rig.rootRight()), right, 1, false);
        OwlModel.Arm outerLeft = held(k, arm(k, left, -1, rig.rootLeft()), left, -1, false);
        OwlModel.Arm innerRight = held(k, inner(k, right, 1, rig.innerRight(), time), right, 1, true);
        OwlModel.Arm innerLeft = held(k, inner(k, left, -1, rig.innerLeft(), time), left, -1, true);
        if (act != null && act.kagune() == OwlActs.Kagune.STAB && act.stab() > 0.0) {
            double d = Math.min(1.0, act.stab());
            innerRight = drive(innerRight, outerRight, d, k, 1);
            innerLeft = drive(innerLeft, outerLeft, d, k, -1);
        }
        return new OwlModel.Look(rig.head(), k, outerRight, outerLeft, true, 0.0, -4.0, 0.0, -4.0, time, grown, innerRight, innerLeft, null);
    }

    /** Inner limb {@code a} turned {@code d} of the way to point as the outer one {@code b} does (driven in alongside it), its lengths kept. */
    static OwlModel.Arm drive(OwlModel.Arm a, OwlModel.Arm b, double d, OwlModel.Frame k, int side) {
        double[] au = sub(a.elbow(), a.shoulder());
        double[] ab = sub(a.tip(), a.elbow());
        double upperLength = Math.sqrt(dot(au, au));
        double bladeLength = Math.sqrt(dot(ab, ab));
        double[] upper = normalize(add(mul(normalize(au), 1.0 - d), mul(normalize(sub(b.elbow(), b.shoulder())), d)));
        double[] blade = normalize(add(mul(normalize(ab), 1.0 - d), mul(normalize(sub(b.tip(), b.elbow())), d)));
        double[] elbow = add(a.shoulder(), mul(upper, upperLength));
        return new OwlModel.Arm(a.shoulder(), elbow, add(elbow, mul(blade, bladeLength)), a.front(), a.bow(), bend(k, blade, side));
    }

    /**
     * 6.2.4: the creature's kagune through its act: as the move swings it ({@link OwlActs.Kagune#FOLLOW}); else eased over from how it
     * was drawn as the move began ({@code start}, {@code age} ticks in) into its rest, and from there drawn back and driven in point
     * first ({@link #THRUST}) or spread and braced into the ground ({@link #SPREAD}) as far as the act has it.
     */
    public static Swing acted(Swing swung, Swing start, double age, OwlActs.Act act, int side) {
        if (act == null || act.kagune() == OwlActs.Kagune.FOLLOW) {
            return swung;
        }
        Swing base = OwlMotions.blend(start == null ? swung : start, REST, smooth(age / 4.0), side);
        return switch (act.kagune()) {
            case STAB -> act.stab() >= 0.0 ? OwlMotions.blend(base, THRUST, Math.min(1.0, act.stab()), side)
                : OwlMotions.blend(base, COCKED, Math.min(1.0, -act.stab() / 0.35), side);
            case SPREAD -> OwlMotions.blend(base, SPREAD, Math.max(0.0, Math.min(1.0, act.stab())), side);
            default -> base;
        };
    }

    /** Its points driven in: the uppers raised forward, the blades driven on ahead and a little down, in toward what is before it. */
    static final Swing THRUST = new Swing(0.6, 2.0, -0.2, 1.3, 0.05);
    /** Drawn back to drive in: the uppers raised high, the blades cocked up and back over it like a scorpion's. */
    static final Swing COCKED = new Swing(0.9, 2.6, 0.5, 2.9, 0.15);
    /** Braced: the uppers thrown out and forward, the blades driven down into the ground ahead of it like legs, against the recoil. */
    static final Swing SPREAD = new Swing(1.25, 1.1, 0.45, 0.15, 0.1);

    /**
     * 6.2.4: how the creature holds its kagune at rest, as the sheet has its blades (torso directions: x her left, y down, z back, for
     * her right; her left the same across): out of the masses on its shoulders, each upper only a short way out (the rest of it in
     * the mass), the outer blades swept out and down and back, the inner ones lower.
     */
    static final double[] HELD_UPPER = {-0.95, -0.15, 0.2};
    static final double[] HELD_BLADE = {-0.9, 0.42, 0.18};
    static final double[] HELD_INNER_UPPER = {-0.9, 0.2, 0.3};
    static final double[] HELD_INNER_BLADE = {-0.66, 0.72, 0.25};
    /** How much of its upper's own length shows out of its mass, held so. */
    static final double HELD_REACH = 0.55;

    /**
     * 6.2.4: the creature's limb {@code a} (swung {@code swing}): held as {@link #HELD_UPPER} has it while the swing is at rest (idle,
     * walking), going over into the swing's own aim as it is swung off its rest (a move: its strikes land where they are aimed).
     */
    static OwlModel.Arm held(OwlModel.Frame k, OwlModel.Arm a, Swing swing, int side, boolean inner) {
        double[] restUpper = direction(k, REST.out(), REST.forward(), side);
        double[] restBlade = direction(k, REST.bladeOut(), REST.bladeForward(), side);
        double off = Math.acos(Math.max(-1.0, Math.min(1.0, dot(restUpper, direction(k, swing.out(), swing.forward(), side)))))
            + Math.acos(Math.max(-1.0, Math.min(1.0, dot(restBlade, direction(k, swing.bladeOut(), swing.bladeForward(), side)))));
        double w = 1.0 - smooth(off / 0.9);
        if (w <= 0.0) {
            return a;
        }
        // (Held as the creature stands, upright, whatever her own lean: across her and back from her, flat; down, down.)
        double[] hu = inner ? HELD_INNER_UPPER : HELD_UPPER;
        double[] hb = inner ? HELD_INNER_BLADE : HELD_BLADE;
        double[] left = normalize(new double[] {k.x()[0], 0.0, k.x()[2]});
        double[] back = normalize(new double[] {k.z()[0], 0.0, k.z()[2]});
        double[] down = {0.0, -1.0, 0.0};
        double[] heldUpper = normalize(add(add(mul(left, hu[0] * side), mul(down, hu[1])), mul(back, hu[2])));
        double[] heldBlade = normalize(add(add(mul(left, hb[0] * side), mul(down, hb[1])), mul(back, hb[2])));
        double upperLength = Math.sqrt(dot(sub(a.elbow(), a.shoulder()), sub(a.elbow(), a.shoulder())));
        double bladeLength = Math.sqrt(dot(sub(a.tip(), a.elbow()), sub(a.tip(), a.elbow())));
        double[] upper = normalize(add(mul(normalize(sub(a.elbow(), a.shoulder())), 1.0 - w), mul(heldUpper, w)));
        double[] blade = normalize(add(mul(normalize(sub(a.tip(), a.elbow())), 1.0 - w), mul(heldBlade, w)));
        double[] elbow = add(a.shoulder(), mul(upper, upperLength * (1.0 - (1.0 - HELD_REACH) * w)));
        double[] tip = add(elbow, mul(blade, bladeLength));
        return new OwlModel.Arm(a.shoulder(), elbow, tip, a.front(), a.bow() * (1.0 - 0.75 * w), bend(k, blade, side));
    }

    /** {@code v} turned by {@code share} of the least turn taking {@code from} to {@code to} (all unit). */
    static double[] turnedBy(double[] v, double[] from, double[] to, double share) {
        double[] axis = cross(from, to);
        double sin = Math.sqrt(dot(axis, axis));
        double cos = dot(from, to);
        if (sin < 1.0E-9) {
            return v;
        }
        double angle = Math.atan2(sin, cos) * share;
        double[] k = mul(axis, 1.0 / sin);
        return add(add(mul(v, Math.cos(angle)), mul(cross(k, v), Math.sin(angle))), mul(k, dot(k, v) * (1.0 - Math.cos(angle))));
    }

    /** 6.2.4: her own body as posed, for the claws, talons, bands and robe the kakuja puts on it ({@link OwlModel.Body}). */
    static OwlModel.Body body(RigPose pose, double bulk) {
        double k = 1.0 + RigPose.BULK_ARMS * bulk;
        return new OwlModel.Body(frameAt(pose, "rightArm", -1.0 * k, 10.0, 0.0, k), frameAt(pose, "leftArm", 1.0 * k, 10.0, 0.0, k),
            frameAt(pose, "rightArm", -1.0 * k, 0.8, 0.0, k), frameAt(pose, "leftArm", 1.0 * k, 0.8, 0.0, k), frameAt(pose, "rightArm", -1.0 * k, 8.3, 0.0, k),
            frameAt(pose, "leftArm", 1.0 * k, 8.3, 0.0, k), frameAt(pose, "rightLeg", 0.0, 0.0, 0.0, 1.0), frameAt(pose, "leftLeg", 0.0, 0.0, 0.0, 1.0),
            frameAt(pose, "rightLeg", 0.0, 12.0, 0.0, 1.0), frameAt(pose, "leftLeg", 0.0, 12.0, 0.0, 1.0), k, bulk);
    }

    /**
     * A frame on a posed part at its own pixel ({@code x}, {@code y}, {@code z}), bent and posed as the game draws it: its axes one
     * pixel each way there (across and front to back {@code thick} times as long, as the part is drawn thicker).
     */
    static OwlModel.Frame frameAt(RigPose pose, String part, double x, double y, double z, double thick) {
        double[] o = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {x, y, z}));
        double[] ax = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {x + thick, y, z}));
        double[] ay = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {x, y + 1.0, z}));
        double[] az = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {x, y, z + thick}));
        return new OwlModel.Frame(o, sub(ax, o), sub(ay, o), sub(az, o));
    }

    /** An eye's middle across her face (head pixels: x to her left), from its column on the face texture. */
    static double eyeX(KakuganEyes eyes, boolean right) {
        return -4.0 + eyes.column(right) + 1.0;
    }

    /** An eye's middle up her face (head pixels: y down from the neck). */
    static double eyeY(KakuganEyes eyes, boolean right) {
        return -8.0 + eyes.row(right) + eyes.height() * 0.5;
    }

    /** A part of the posed body as a frame: its origin and how far (blocks) each of its pixels runs, as the game draws it. */
    static OwlModel.Frame frame(RigPose pose, String part) {
        double[] o = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {0.0, 0.0, 0.0}));
        double[] x = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {1.0, 0.0, 0.0}));
        double[] y = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {0.0, 1.0, 0.0}));
        double[] z = BodyFrame.world(pose, BodyFrame.model(pose, part, new double[] {0.0, 0.0, 1.0}));
        return new OwlModel.Frame(o, sub(x, o), sub(y, o), sub(z, o));
    }

    /** 6.2.0: one giant arm ({@code side}) swung {@code swing} off {@code body}'s torso, fully bulked (for the tests). */
    public static OwlModel.Arm arm(RigPose body, Swing swing, int side) {
        return arm(frame(body, "torso"), swing, side, 1.0);
    }

    /** One giant arm ({@code side} 1 her right, -1 her left) swung {@code swing} off her torso. */
    static OwlModel.Arm arm(OwlModel.Frame torso, Swing swing, int side, double bulk) {
        double x = SHOULDER[0] + RigPose.BULK_SPREAD * (1.0 - bulk);
        return arm(torso, swing, side, torso.at(x * side, SHOULDER[1], SHOULDER[2]));
    }

    /** The same grown out of {@code shoulder} (rig space; the creature's root on its yoke). */
    static OwlModel.Arm arm(OwlModel.Frame torso, Swing swing, int side, double[] shoulder) {
        double[] upper = direction(torso, swing.out(), swing.forward(), side);
        double[] elbow = add(shoulder, mul(upper, OwlModel.UPPER));
        double[] blade = direction(torso, swing.bladeOut(), swing.bladeForward(), side);
        double[] tip = add(elbow, mul(blade, OwlModel.FORE));
        double[] front = torso.dir(0.0, 0.0, -1.0);
        return new OwlModel.Arm(shoulder, elbow, tip, front, swing.bow(), bend(torso, blade, side));
    }

    /**
     * The way a blade pointing {@code blade} (rig space, unit) bows out of its line (6.2.0): hanging or held level, up and a little out
     * (over, then down to its point: its point hooks down at what it is held at); raised, the bow turns with it, back over the top (its
     * point hooks forward, a claw about to come down), never flipping round to hook its point back behind her as the old fixed bow did
     * once a blade stood up along it.
     */
    static double[] bend(OwlModel.Frame torso, double[] blade, int side) {
        double[] up = torso.dir(0.0, -1.0, 0.0);
        double[] out = torso.dir(-side, 0.0, 0.0);
        double[] hanging = normalize(add(up, mul(out, 0.35)));
        double rise = smooth((dot(blade, up) + 0.1) / 0.6);
        if (rise <= 0.0) {
            return hanging;
        }
        // Raised: square to the blade in the plane of her front and up, on the side away from where it is coming from (up turned
        // back, ahead turned up).
        double[] left = normalize(torso.dir(1.0, 0.0, 0.0));
        double[] over = cross(left, blade);
        double length = Math.sqrt(dot(over, over));
        if (length < 1.0E-6) {
            return hanging; // straight out to her side: the old bow holds
        }
        over = mul(over, 1.0 / length);
        double[] flat = sub(hanging, mul(blade, dot(hanging, blade)));
        double flatLength = Math.sqrt(dot(flat, flat));
        flat = flatLength < 1.0E-6 ? over : mul(flat, 1.0 / flatLength);
        return normalize(add(mul(flat, 1.0 - rise), mul(over, rise)));
    }

    /**
     * 6.2.0: a giant arm's swing given in the frame of the way she faces ({@code facing}: turned {@code facingSpin} radians, + to her
     * left, from her body's facing; standing straight up, not leaned, twisted or bent aside) as the same directions in her torso's own
     * frame as {@code body} poses it: an arm keyed to point at what she faces points at it however her body is turned and bent under
     * it. Each angle forward is the one nearest the facing frame's own (so keys blend the way they were laid out).
     */
    public static Swing toTorso(RigPose body, Swing facing, int side, double facingSpin) {
        RigPose upright = new RigPose();
        upright.spin = (float)facingSpin;
        OwlModel.Frame facingFrame = frame(upright, "torso");
        OwlModel.Frame torso = frame(body, "torso");
        double[] upper = local(torso, direction(facingFrame, facing.out(), facing.forward(), side), side, facing.forward());
        double[] blade = local(torso, direction(facingFrame, facing.bladeOut(), facing.bladeForward(), side), side, facing.bladeForward());
        return new Swing(upper[0], upper[1], blade[0], blade[1], facing.bow());
    }

    /** 6.2.0: {@link #toTorso} undone: a giant arm's swing off {@code body}'s torso as the same directions in the frame of the way she faces. */
    public static Swing fromTorso(RigPose body, Swing torsoSwing, int side, double facingSpin) {
        RigPose upright = new RigPose();
        upright.spin = (float)facingSpin;
        OwlModel.Frame facingFrame = frame(upright, "torso");
        OwlModel.Frame torso = frame(body, "torso");
        double[] upper = local(facingFrame, direction(torso, torsoSwing.out(), torsoSwing.forward(), side), side, torsoSwing.forward());
        double[] blade = local(facingFrame, direction(torso, torsoSwing.bladeOut(), torsoSwing.bladeForward(), side), side, torsoSwing.bladeForward());
        return new Swing(upper[0], upper[1], blade[0], blade[1], torsoSwing.bow());
    }

    /**
     * 6.2.0: where one of the Owl's bone spikes grows out of {@code body} as posed and the way it goes ({root, way}, rig space; the way
     * turned as the move faces, {@code facingSpin} radians + to her left from her body's facing): out of her back as drawn.
     */
    public static double[][] spike(RigPose body, com.hex.kagune.control.eto.OwlSpikes.Spike spike, double facingSpin) {
        double[] root = BodyFrame.world(body, BodyFrame.model(body, "torso", spike.px()));
        double[] d = spike.dir();
        double c = Math.cos(facingSpin);
        double s = Math.sin(facingSpin);
        return new double[][] {root, {d[0] * c - d[2] * s, d[1], d[0] * s + d[2] * c}};
    }

    /**
     * 6.2.0: a giant arm ({@code side}) off {@code body}'s torso swung round its shoulder, whole (the blade bent off the upper arm as
     * in {@code near}), until its blade goes through {@code point} (rig space): a thrust held in what it went through, whatever her
     * body does round it. Each angle forward nearest {@code near}'s.
     */
    public static Swing aimAt(RigPose body, int side, double[] point, Swing near) {
        OwlModel.Frame torso = frame(body, "torso");
        double bulk = Float.isFinite(body.bulk) ? Math.max(0.0, Math.min(1.0, body.bulk)) : 0.0;
        double x = SHOULDER[0] + RigPose.BULK_SPREAD * (1.0 - bulk);
        double[] shoulder = torso.at(x * side, SHOULDER[1], SHOULDER[2]);
        double[] upper = direction(torso, near.out(), near.forward(), side);
        double[] blade = direction(torso, near.bladeOut(), near.bladeForward(), side);
        double[] to = sub(point, shoulder);
        for (int i = 0; i < 4; i++) {
            // The nearest the blade's line comes to the point, turned onto it about the shoulder (the whole arm with it).
            double[] elbow = mul(upper, OwlModel.UPPER);
            double along = Math.max(0.0, Math.min(OwlModel.FORE, dot(sub(to, elbow), blade)));
            double[] nearest = add(elbow, mul(blade, along));
            double[][] turned = turnOnto(nearest, to, upper, blade);
            upper = turned[0];
            blade = turned[1];
        }
        double[] u = local(torso, upper, side, near.forward());
        double[] b = local(torso, blade, side, near.bladeForward());
        return new Swing(u[0], u[1], b[0], b[1], near.bow());
    }

    /** {@code parts} turned by the least turn taking the way {@code from} points to the way {@code to} points. */
    static double[][] turnOnto(double[] from, double[] to, double[]... parts) {
        double[] a = normalize(from);
        double[] b = normalize(to);
        double[] axis = cross(a, b);
        double sin = Math.sqrt(dot(axis, axis));
        double cos = dot(a, b);
        double[][] turned = new double[parts.length][];
        for (int i = 0; i < parts.length; i++) {
            double[] v = parts[i];
            if (sin < 1.0E-9) {
                turned[i] = v;
                continue;
            }
            double[] k = mul(axis, 1.0 / sin);
            turned[i] = add(add(mul(v, cos), mul(cross(k, v), sin)), mul(k, dot(k, v) * (1.0 - cos)));
        }
        return turned;
    }

    // ------------------------------------------------------------------ the elbow

    /**
     * 6.2.0: the elbow's hinge. The red muscle swings the arm and the blade rides on it, bent so far off it at the elbow and so far
     * round from the way it naturally bends: it flexes at the elbow, never swinging round the arm on its own. The way it naturally
     * bends follows the arm wherever it points (off the arm's own side: out to her side, up, front): carried round from
     * {@link #HINGE_FROM} by the least turn, so it turns smoothly with the arm everywhere but one way the arm can never point:
     * {@link #HINGE_BLIND}, in, down and back into her own body.
     */
    static final double[] HINGE_BLIND = normalize(new double[] {-0.6, -0.6, -0.5});
    static final double[] HINGE_FROM = mul(HINGE_BLIND, -1.0);
    /** Pointing {@link #HINGE_FROM}, the way the blade bends: toward her front, a little up and in. */
    static final double[] HINGE = normalize(sub(new double[] {-0.3, 0.3, 1.0}, mul(HINGE_FROM, dot(new double[] {-0.3, 0.3, 1.0}, HINGE_FROM))));

    /** A part of a giant arm pointing (out, forward) off its own side as {out to her side, up, front}. */
    static double[] along(double out, double forward) {
        return new double[] {Math.sin(out), -Math.cos(out) * Math.cos(forward), Math.cos(out) * Math.sin(forward)};
    }

    /** The two ways square to an upper arm pointing {@code upper} ({@link #along}) its blade's bend is measured round from ({@link #HINGE} carried to it first). */
    static double[][] hinge(double[] upper) {
        // HINGE carried by the least turn from HINGE_FROM to the arm.
        double c = dot(HINGE_FROM, upper);
        double[] w = cross(HINGE_FROM, upper);
        double[] way = add(add(mul(HINGE, c), cross(w, HINGE)), mul(w, dot(w, HINGE) / Math.max(1.0E-6, 1.0 + c)));
        way = normalize(sub(way, mul(upper, dot(way, upper))));
        return new double[][] {way, cross(upper, way)};
    }

    /**
     * 6.2.0: how {@code swing}'s blade is bent off its upper arm at the elbow: {how far (radians), which way (radians round from
     * {@link #HINGE})}.
     */
    public static double[] elbow(Swing swing) {
        double[] upper = along(swing.out(), swing.forward());
        double[] blade = along(swing.bladeOut(), swing.bladeForward());
        double[][] ways = hinge(upper);
        double bent = Math.acos(Math.max(-1.0, Math.min(1.0, dot(upper, blade))));
        return new double[] {bent, Math.atan2(dot(blade, ways[1]), dot(blade, ways[0]))};
    }

    /**
     * 6.2.0: a giant arm with its upper arm pointing (out, forward) and its blade bent {@code bent} radians off it at the elbow,
     * {@code way} radians round from {@link #HINGE}, bowed {@code bow}; the blade's angle forward nearest {@code near}.
     */
    public static Swing carry(double out, double forward, double bent, double way, double bow, double near) {
        double[] upper = along(out, forward);
        double[][] ways = hinge(upper);
        double[] side = add(mul(ways[0], Math.cos(way)), mul(ways[1], Math.sin(way)));
        double[] blade = add(mul(upper, Math.cos(bent)), mul(side, Math.sin(bent)));
        double bladeOut = Math.asin(Math.max(-1.0, Math.min(1.0, blade[0])));
        double bladeForward = Math.abs(blade[1]) < 1.0E-9 && Math.abs(blade[2]) < 1.0E-9 ? near : Math.atan2(blade[2], -blade[1]);
        bladeForward += Math.PI * 2.0 * Math.round((near - bladeForward) / (Math.PI * 2.0));
        return new Swing(out, forward, bladeOut, bladeForward, bow);
    }

    /** 6.2.0's rest: the upper arms out and down off the shoulders, the blades sweeping on down, out and forward. */
    static final Swing REST_620 = new Swing(1.0, 0.15, 0.6, 1.1, 0.26);

    /**
     * At rest (6.2.4, spread wider as the reference stands): the upper arms raised further out off the shoulders, each blade bent off
     * its upper arm exactly as 6.2.0's was (the moves were keyed round that bend: the muscle carries the blade, the blade never swings
     * on its own going into one), angled on down, out and forward to its point. (Declared after the hinge it is carried on.)
     */
    public static final Swing REST = carry(REST_OUT, REST_FORWARD, elbow(REST_620)[0], elbow(REST_620)[1], REST_620.bow(), REST_620.bladeForward());

    /** How the blade is bent off its upper arm at rest ({@link #elbow} of {@link #REST}). */
    static final double[] REST_ELBOW = elbow(REST);

    /** 6.2.4: the limbs as they tear out of her back (and as they fold back in, turning back): straight up out of it, folded tight. */
    public static final Swing ERUPT = carry(0.45, 2.85, 0.38, REST_ELBOW[1], 0.14, 2.6);

    /**
     * 6.2.4: a limb ({@code side}) as the Owl has grown out {@code grown} (0..1): torn up out of her back folded tight, unfolding down
     * and out into {@code swung} (its idle or its move) as it grows; turning back, folding up again as it is drawn in.
     */
    public static Swing unfurl(Swing swung, int side, double grown) {
        double f = smooth((grown - 0.06) / 0.62);
        return f >= 1.0 ? swung : OwlMotions.blend(ERUPT, swung, f, side);
    }

    /**
     * A direction (rig space, unit) as {out, forward} off {@code torso} for {@code side} ({@link #direction} undone), forward the
     * angle nearest {@code near}.
     */
    static double[] local(OwlModel.Frame torso, double[] dir, int side, double near) {
        double px2 = dot(torso.x(), torso.x());
        double lx = dot(dir, torso.x()) / px2;
        double ly = dot(dir, torso.y()) / px2;
        double lz = dot(dir, torso.z()) / px2;
        double length = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (length < 1.0E-9) {
            return new double[] {0.0, near};
        }
        lx /= length;
        ly /= length;
        lz /= length;
        double out = Math.asin(Math.max(-1.0, Math.min(1.0, -side * lx)));
        double forward = Math.abs(ly) < 1.0E-9 && Math.abs(lz) < 1.0E-9 ? near : Math.atan2(-lz, ly);
        forward += Math.PI * 2.0 * Math.round((near - forward) / (Math.PI * 2.0));
        return new double[] {out, forward};
    }

    /**
     * A direction off her torso (rig space, unit): straight down it, turned out to her {@code side} by {@code out}, then forward by
     * {@code forward} (torso pixels: x to her left, y down, z back).
     */
    static double[] direction(OwlModel.Frame torso, double out, double forward, int side) {
        return torso.dir(-side * Math.sin(out), Math.cos(out) * Math.cos(forward), -Math.cos(out) * Math.sin(forward));
    }

    static double smooth(double u) {
        double c = Math.max(0.0, Math.min(1.0, u));
        return c * c * (3.0 - 2.0 * c);
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

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0.0, -1.0, 0.0} : mul(a, 1.0 / l);
    }
}
