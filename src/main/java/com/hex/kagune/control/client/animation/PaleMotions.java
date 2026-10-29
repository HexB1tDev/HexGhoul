package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.PaleStrike;

/**
 * Pale's moves on her body (6.1.0; pure, tested): every client poses her from the move the server began and how long ago, so all
 * of them see the same blow land on the same tick.
 *
 * <p>A strike ({@link #strike}) is thrown the way a fighter throws one, with the whole body: a coil (the weight sat back, the chest
 * wound away, the fist chambered), a drive that accelerates into contact on the server's impact tick (the whole body pivoting on
 * the feet, the lead foot stepping into a lunge, the rear leg straightening, the shoulder thrown forward, the arm fully out, the
 * other hand pulled hard back), an overshoot a hair past full extension, a short hold, and the recoil to her guard. The keys are
 * choreographed in tools/PalePoseDesign.java, their arms solved onto where the fists and elbows go ({@link PaleStrikePoses}); the
 * left-hand strikes are the right-hand keys mirrored. A strike starts from exactly the pose she was in (the last strike's
 * extension is the next one's coil). A flip ({@link #flip}) turns her whole body over once about its middle.
 *
 * <p>Every frame keeps her arms out of her torso and head ({@link BodyFrame#clipping}; the tests sweep them all, from every pose a
 * strike can begin in).
 */
public final class PaleMotions {
    private PaleMotions() {
    }

    /** How the motion eases into a key: evenly in and out, accelerating into it (a blow's contact), decelerating into it. */
    enum Ease {
        SMOOTH, IN, OUT;

        float apply(float u) {
            return switch (this) {
                case SMOOTH -> u * u * (3.0F - 2.0F * u);
                // Fastest at contact: the fist is still speeding up when it lands.
                case IN -> (float)Math.pow(u, 2.2);
                case OUT -> 1.0F - (1.0F - u) * (1.0F - u);
            };
        }
    }

    /** A pose {@code at} ticks into a move, eased into as {@code ease} says; a null pose is the one the move began from. */
    record Key(float at, RigPose pose, Ease ease) {
        static final Key START = new Key(0.0F, null, Ease.SMOOTH);
    }

    // ------------------------------------------------------------------ strikes

    /**
     * A strike's pose {@code age} ticks in, out of {@code from} (the pose she was in when it began, as drawn: the last strike's
     * extension, which is this one's coil; null: her guard). Left-hand strikes are the right-hand keys mirrored. The keys are
     * choreographed in tools/PalePoseDesign.java ({@link PaleStrikePoses}): coil, the drive accelerating into contact on the server's
     * impact tick, the overshoot, the hold, the recoil to the guard.
     */
    public static RigPose strike(PaleStrike strike, double age, RigPose from) {
        RigPose start = from == null ? PaleStrikePoses.guard() : strike.right ? copy(from) : mirror(from);
        // Whole turns are no turn: a strike that ended spun round starts the next from facing ahead, not a turn back.
        start.spin = wrap(start.spin);
        start.flip = wrap(start.flip);
        start.roll = wrap(start.roll);
        Key[] keys = PaleStrikePoses.keys(strike);
        RigPose pose = sample(keys, (float)age, start);
        if (age < keys[1].at()) {
            overhead(pose, start, keys[1].pose(), (float)(age / keys[1].at()));
        }
        grounded(pose);
        return strike.right ? pose : mirror(pose);
    }

    /**
     * An arm over her head when a strike begins (she was stretching) comes down outside it: its turn in toward her middle (the
     * arm's Y and Z) waits until it is down, so it never sweeps past her face. {@code u} is how far (0..1) into the first key.
     */
    static void overhead(RigPose pose, RigPose start, RigPose key, float u) {
        float eased = u * u * (3.0F - 2.0F * u);
        // 6.2.0: out of a cut, from just above level too (an arm can be anywhere up there mid-move).
        float from = start.cut > 0.5F ? RAISED : 1.9F;
        float span = start.cut > 0.5F ? RAISED_SPAN : 0.7F;
        float right = smooth01((-start.rightArmX - from) / span);
        if (right > 0.0F) {
            float late = (float)Math.pow(eased, 1.0 + 3.5 * right);
            pose.rightArmY = start.rightArmY + (key.rightArmY - start.rightArmY) * late;
            pose.rightArmZ = start.rightArmZ + (key.rightArmZ - start.rightArmZ) * late;
        }
        float left = smooth01((-start.leftArmX - from) / span);
        if (left > 0.0F) {
            float late = (float)Math.pow(eased, 1.0 + 3.5 * left);
            pose.leftArmY = start.leftArmY + (key.leftArmY - start.leftArmY) * late;
            pose.leftArmZ = start.leftArmZ + (key.leftArmZ - start.leftArmZ) * late;
        }
    }

    // ------------------------------------------------------------------ 6.2.0: Witness This

    /**
     * 6.2.0, Witness This {@code age} ticks after the full-charge punch landed ({@link com.hex.kagune.control.pale.WitnessRules}): the
     * punch held out as it is driven back; behind it, the uppercut; then a blow a tick, each a strike of hers or a kick centred on the
     * tick it lands (she blinks between them: her pose jumps with her), her whole body tipped to aim it up or down; above it, tucked
     * tight, the stomp driven straight down onto it and ridden down into the ground; off it into a deep crouch, and up into her guard.
     * Always turned to face it ({@code WitnessRules.facing}).
     */
    public static RigPose witness(double age, RigPose from) {
        RigPose pose;
        int hit = com.hex.kagune.control.pale.WitnessRules.hit(age);
        if (age < com.hex.kagune.control.pale.WitnessRules.BEHIND) {
            pose = punch(PaleRules.PUNCH_IMPACT + age, null);
        } else if (age < com.hex.kagune.control.pale.WitnessRules.hitAt(0) - com.hex.kagune.control.pale.WitnessRules.windUp(0)) {
            pose = strike(PaleStrike.UPPERCUT, age - com.hex.kagune.control.pale.WitnessRules.UPPER + PaleStrike.UPPERCUT.impact, null);
        } else if (hit >= 0) {
            pose = witnessBlow(com.hex.kagune.control.pale.WitnessRules.blow(hit), hit, age - com.hex.kagune.control.pale.WitnessRules.hitAt(hit));
        } else if (age < com.hex.kagune.control.pale.WitnessRules.DROP) {
            // Above it, tucked tight, then the leg driven down.
            double u = (age - com.hex.kagune.control.pale.WitnessRules.ABOVE) / (com.hex.kagune.control.pale.WitnessRules.DROP - com.hex.kagune.control.pale.WitnessRules.ABOVE);
            float e = (float)Math.max(0.0, Math.min(1.0, u));
            pose = new RigPose().add(stompTuck(), 1.0F - e * e * e).add(stompOut(), e * e * e);
        } else {
            // 6.2.1: the stomp driven through it, she springs off it and falls out of the sky: from the kick into a fall, arms out.
            double u = (age - com.hex.kagune.control.pale.WitnessRules.DROP - 1.0) / 5.0;
            float e = smooth01((float)u);
            pose = new RigPose().add(stompOut(), 1.0F - e).add(falling(), e);
        }
        // 6.2.3: her turn to face it, as the nearest turn: summed up blow after blow it ran on past a whole turn, which a pose
        // can't carry (settle holds spins to one turn), and she was drawn facing the wrong way from the fourth blow on.
        float turn = wrap((float)com.hex.kagune.control.pale.WitnessRules.facing(age));
        pose.spin += turn;
        if (hit >= 0 && com.hex.kagune.control.pale.WitnessRules.blow(hit) == com.hex.kagune.control.pale.WitnessRules.Blow.SPIN_KICK) {
            // The spinning heel kick: a whole turn into it, the nearer way round, done as the heel lands.
            double since = age - com.hex.kagune.control.pale.WitnessRules.hitAt(hit);
            double w = com.hex.kagune.control.pale.WitnessRules.windUp(hit);
            float left = 1.0F - smooth01((float)((since + w) / w));
            pose.spin -= Math.signum(turn == 0.0F ? 1.0F : turn) * (float)(Math.PI * 2.0) * left;
        }
        // Aimed up or down at it (from below, from above), her whole body tipped.
        pose.flip += (float)com.hex.kagune.control.pale.WitnessRules.tilt(age);
        // 6.2.2: her head is the move's, square on her body and eyes on it (the camera's own turn on top threw it about), eased
        // back to her own as she falls free.
        float free = age < com.hex.kagune.control.pale.WitnessRules.FREE ? 0.0F
            : smooth01((float)((age - com.hex.kagune.control.pale.WitnessRules.FREE) / 6.0));
        pose.headHold = 1.0F - free;
        return pose;
    }

    /**
     * One blow of Witness This's blitz ({@code hit}), {@code since} ticks from its landing: a strike of hers, or a kick. 6.2.3:
     * each at its own pace (wound up over its own {@code windUp}, recovering as fast as the gap to the next allows), frozen on
     * contact for its hit stop, and her whole body thrown into it: the chest driven through the punches, the hips through the kicks.
     */
    static RigPose witnessBlow(com.hex.kagune.control.pale.WitnessRules.Blow blow, int hit, double since) {
        double w = com.hex.kagune.control.pale.WitnessRules.windUp(hit);
        double stop = com.hex.kagune.control.pale.WitnessRules.hitStop(hit);
        double t;
        if (since < 0.0) {
            t = since * (WITNESS_COIL / w);
        } else if (since < stop) {
            t = 0.0;
        } else {
            double window = Math.max(1.0, com.hex.kagune.control.pale.WitnessRules.gap(hit) - com.hex.kagune.control.pale.WitnessRules.windUp(hit + 1) - stop);
            t = (since - stop) * Math.max(0.6, Math.min(1.8, 3.0 / window));
        }
        RigPose pose = witnessPose(blow, t);
        // Into it: snapped in over the last of the wind-up, held through the stop, eased off after.
        float drive = since < 0.0 ? smooth01((float)((since + 0.6) / 0.6)) : since < stop ? 1.0F : (float)Math.exp(-(since - stop) / 1.3);
        if (!blow.kick()) {
            pose.lean += 0.28F * drive;
            pose.twist *= 1.0F + 0.4F * drive;
            pose.lunge += 1.5F * drive;
        } else {
            pose.lean -= 0.12F * drive;
        }
        // The heavy ones jar her too: her head snaps with the blow.
        if (com.hex.kagune.control.pale.WitnessRules.heavy(hit)) {
            pose.headX += 0.18F * drive;
        }
        return pose;
    }

    /** How far (ticks of a blow's own keys) before contact each Witness This blow is picked up: the same point of every wind-up. */
    static final double WITNESS_COIL = 1.2;

    /** A Witness This blow's pose {@code t} ticks (of its own keys) from contact. */
    static RigPose witnessPose(com.hex.kagune.control.pale.WitnessRules.Blow blow, double t) {
        double since = t;
        return switch (blow) {
            case CROSS -> strike(PaleStrike.CROSS, since + PaleStrike.CROSS.impact, null);
            case JAB -> strike(PaleStrike.JAB, since + PaleStrike.JAB.impact, null);
            case HOOK -> strike(PaleStrike.HOOK, since + PaleStrike.HOOK.impact, null);
            case LIVER -> strike(PaleStrike.LIVER, since + PaleStrike.LIVER.impact, null);
            case UPPERCUT -> strike(PaleStrike.UPPERCUT, since + PaleStrike.UPPERCUT.impact, null);
            case TWIN_PALM -> strike(PaleStrike.TWIN_PALM, since + PaleStrike.TWIN_PALM.impact, null);
            // The Speed Kick's flying side kick, out of its chamber.
            case SIDE_KICK -> kick(PaleRules.kickArrive(PaleRules.KICK_DASH_MIN) + PaleRules.KICK_CONTACT + since, PaleRules.KICK_DASH_MIN, null);
            // The stomp from above: tucked, the leg driven straight down.
            case STOMP -> {
                float e = (float)Math.max(0.0, Math.min(1.0, since + 0.5));
                e = e * e * (3.0F - 2.0F * e);
                yield new RigPose().add(stompTuck(), 1.0F - e).add(stompOut(), e);
            }
            // 6.2.3: her new kicks, each out of its chamber at contact (t 0), held, and drawn back into the air guard.
            case ROUNDHOUSE -> sample(ROUNDHOUSE_KEYS, (float)(t + WITNESS_COIL), roundhouseChamber());
            case SPIN_KICK -> kick(PaleRules.kickArrive(PaleRules.KICK_DASH_MIN) + PaleRules.KICK_CONTACT + since, PaleRules.KICK_DASH_MIN, null);
            case AXE_KICK -> sample(AXE_KEYS, (float)(t + WITNESS_COIL), axeRaised());
            case FLYING_KNEE -> sample(KNEE_KEYS, (float)(t + WITNESS_COIL), kneeChamber());
        };
    }

    // 6.2.3, Witness This: her new kicks (right leg; all in the air). Key times from the chamber, contact at WITNESS_COIL.

    static final Key[] ROUNDHOUSE_KEYS = {Key.START,
        new Key(1.2F, roundhouseHit(), Ease.IN),
        new Key(2.0F, roundhouseFollow(), Ease.OUT),
        new Key(5.0F, airGuard(), Ease.SMOOTH)};
    static final Key[] AXE_KEYS = {Key.START,
        new Key(1.2F, axeHit(), Ease.IN),
        new Key(2.0F, axeFollow(), Ease.OUT),
        new Key(5.0F, airGuard(), Ease.SMOOTH)};
    static final Key[] KNEE_KEYS = {Key.START,
        new Key(1.2F, kneeHit(), Ease.IN),
        new Key(2.0F, kneeFollow(), Ease.OUT),
        new Key(5.0F, airGuard(), Ease.SMOOTH)};

    /** In the air between blows: hands up, knees drawn up a little under her. */
    static RigPose airGuard() {
        RigPose p = PaleStrikePoses.guard();
        p.rightLegX = -0.7F;
        p.rightKnee = 1.3F;
        p.leftLegX = -0.4F;
        p.leftKnee = 1.0F;
        return p;
    }

    /** The roundhouse chambered: wound up the other way, the right knee up and turned out, the right arm swung back. */
    static RigPose roundhouseChamber() {
        RigPose p = airGuard();
        p.spin = -0.45F;
        p.roll = 0.25F;
        p.twist = 0.3F;
        p.lean = -0.1F;
        p.rightLegX = -1.5F;
        p.rightLegZ = 0.65F;
        p.rightKnee = 2.4F;
        p.leftLegX = -0.6F;
        p.leftKnee = 1.5F;
        return p;
    }

    /** ...the shin whipped round flat into it at its middle, hips turned over, the body laid back away from the leg. */
    static RigPose roundhouseHit() {
        RigPose p = airGuard();
        p.spin = 1.25F;
        p.roll = 0.6F;
        p.twist = -0.35F;
        p.lean = -0.4F;
        p.headX = -0.1F;
        p.rightLegX = -0.3F;
        p.rightLegZ = 0.95F;
        p.rightKnee = 0.05F;
        p.leftLegX = -0.35F;
        p.leftKnee = 1.3F;
        p.rightArmX = 0.5F;
        p.rightArmY = 0.0F;
        p.rightArmZ = 0.55F;
        p.rightElbow = 0.35F;
        p.leftArmX = -1.1F;
        p.leftArmY = 0.2F;
        p.leftArmZ = -0.5F;
        p.leftElbow = 1.1F;
        return p;
    }

    /** ...carried on through it, the knee folding back in. */
    static RigPose roundhouseFollow() {
        RigPose p = roundhouseHit();
        p.spin = 1.75F;
        p.roll = 0.45F;
        p.rightLegX = -0.7F;
        p.rightLegZ = 0.7F;
        p.rightKnee = 1.5F;
        return p;
    }

    /** The axe kick raised: the right leg straight up over it, leaning back under it, arms out wide, eyes down on it. */
    static RigPose axeRaised() {
        RigPose p = airGuard();
        p.lean = -0.35F;
        p.headX = 0.6F;
        p.rightLegX = -2.5F;
        p.rightLegZ = 0.1F;
        p.rightKnee = 0.1F;
        p.leftLegX = -0.3F;
        p.leftKnee = 1.0F;
        p.rightArmX = -0.6F;
        p.rightArmY = 0.0F;
        p.rightArmZ = 1.1F;
        p.rightElbow = 0.4F;
        p.leftArmX = -0.6F;
        p.leftArmY = 0.0F;
        p.leftArmZ = -1.1F;
        p.leftElbow = 0.4F;
        return p;
    }

    /** ...the heel brought straight down through it, her whole body folding over after it, arms flung back. */
    static RigPose axeHit() {
        RigPose p = axeRaised();
        p.lean = 0.5F;
        p.headX = 0.75F;
        p.rightLegX = -0.55F;
        p.rightKnee = 0.05F;
        p.leftLegX = -1.0F;
        p.leftKnee = 1.7F;
        p.rightArmX = 0.5F;
        p.rightArmZ = 0.7F;
        p.leftArmX = 0.5F;
        p.leftArmZ = -0.7F;
        return p;
    }

    /** ...followed through down, the knee giving. */
    static RigPose axeFollow() {
        RigPose p = axeHit();
        p.lean = 0.35F;
        p.rightLegX = -0.25F;
        p.rightKnee = 0.6F;
        return p;
    }

    /** The flying knee chambered: the right leg drawn back, both hands thrown up and out for its head. */
    static RigPose kneeChamber() {
        RigPose p = airGuard();
        p.lean = -0.2F;
        p.headX = 0.15F;
        p.rightLegX = 0.35F;
        p.rightKnee = 1.7F;
        p.leftLegX = -0.8F;
        p.leftKnee = 1.2F;
        p.rightArmX = -1.2F;
        p.rightArmY = 0.0F;
        p.rightArmZ = 1.35F;
        p.rightElbow = 0.5F;
        p.leftArmX = -1.2F;
        p.leftArmY = 0.0F;
        p.leftArmZ = -1.35F;
        p.leftElbow = 0.5F;
        return p;
    }

    /** ...the knee driven up into its middle, the hips through it, its head pulled down onto it. */
    static RigPose kneeHit() {
        RigPose p = kneeChamber();
        p.lean = 0.4F;
        p.lunge = 3.0F;
        p.headX = 0.45F;
        p.rightLegX = -1.85F;
        p.rightKnee = 2.45F;
        p.leftLegX = 0.45F;
        p.leftKnee = 1.4F;
        p.rightArmX = -1.3F;
        p.rightArmZ = 0.8F;
        p.rightElbow = 0.9F;
        p.leftArmX = -1.3F;
        p.leftArmZ = -0.8F;
        p.leftElbow = 0.9F;
        return p;
    }

    /** ...held up in it a moment. */
    static RigPose kneeFollow() {
        RigPose p = kneeHit();
        p.lean = 0.25F;
        p.lunge = 1.5F;
        p.rightLegX = -1.6F;
        p.rightKnee = 2.2F;
        return p;
    }

    /** 6.2.1, Witness This: falling out of the sky after the stomp, looking down at the crater, arms out wide, knees a little up. */
    static RigPose falling() {
        RigPose p = new RigPose();
        p.armHold = 1.0F;
        p.legHold = 1.0F;
        p.flip = -0.1F;
        p.headX = 0.45F;
        p.rightLegX = -0.5F;
        p.rightKnee = 0.9F;
        p.leftLegX = -0.2F;
        p.leftKnee = 0.6F;
        p.rightArmX = -0.4F;
        p.rightArmZ = 1.2F;
        p.rightElbow = 0.3F;
        p.leftArmX = -0.4F;
        p.leftArmZ = -1.2F;
        p.leftElbow = 0.3F;
        return p;
    }

    /** 6.2.0, Witness This: above it, tucked tight (both knees up), looking down at it, her arms out wide, like a pounce. */
    static RigPose stompTuck() {
        RigPose p = new RigPose();
        p.armHold = 1.0F;
        p.legHold = 1.0F;
        p.flip = -0.35F;
        p.headX = 0.5F;
        p.rightLegX = -1.9F;
        p.rightKnee = 2.4F;
        p.leftLegX = -1.7F;
        p.leftKnee = 2.4F;
        p.rightArmX = -1.0F;
        p.rightArmZ = 1.4F;
        p.rightElbow = 0.6F;
        p.leftArmX = -1.0F;
        p.leftArmZ = -1.4F;
        p.leftElbow = 0.6F;
        return p;
    }

    /** ...the stomp: her right leg driven straight down onto it, the left kept tucked, both arms flung out wide. */
    static RigPose stompOut() {
        RigPose p = new RigPose();
        p.armHold = 1.0F;
        p.legHold = 1.0F;
        p.flip = -0.2F;
        p.lean = -0.15F;
        p.headX = 0.8F;
        p.rightLegX = 0.1F;
        p.leftLegX = -1.7F;
        p.leftKnee = 2.4F;
        p.rightArmX = -1.3F;
        p.rightArmZ = 1.3F;
        p.rightElbow = 0.2F;
        p.leftArmX = -1.3F;
        p.leftArmZ = -1.3F;
        p.leftElbow = 0.15F;
        return p;
    }

    /** ...landed beside the crater: down in a deep crouch, her arms out wide and low for balance. */
    static RigPose crouch() {
        RigPose p = guard();
        p.lean = 0.42F;
        p.leftLegX = -0.95F;
        p.leftKnee = 1.7F;
        p.rightLegX = -0.6F;
        p.rightKnee = 1.55F;
        p.rightArmX = -0.5F;
        p.rightArmY = 0.0F;
        p.rightArmZ = 1.0F;
        p.rightElbow = 0.35F;
        p.leftArmX = -0.5F;
        p.leftArmY = 0.0F;
        p.leftArmZ = -1.0F;
        p.leftElbow = 0.35F;
        grounded(p);
        return p;
    }

    // ------------------------------------------------------------------ Big Punch

    /**
     * Big Punch held {@code age} ticks, out of {@code from} (the pose she was in when Z went down): she sinks into the coil as it
     * charges, deeper and further round, and once it is nearly full the whole body trembles with it.
     */
    public static RigPose punchCharge(double age, RigPose from) {
        float charge = smooth01((float)(age / PaleRules.PUNCH_CHARGE_TICKS));
        RigPose coil = PaleStrikePoses.punchCoil0().scale(1.0F - charge);
        coil.add(PaleStrikePoses.punchCoil1(), charge);
        float tremble = smooth01((float)((age - PaleRules.PUNCH_CHARGE_TICKS * 0.6) / (PaleRules.PUNCH_CHARGE_TICKS * 0.4)));
        if (tremble > 0.0F) {
            float a = 0.035F * tremble;
            coil.rightArmX += a * (float)Math.sin(age * 7.3);
            coil.rightElbow += a * 0.6F * (float)Math.sin(age * 6.1 + 0.7);
            coil.twist += a * 0.5F * (float)Math.sin(age * 5.3 + 1.1);
            coil.lean += a * 0.4F * (float)Math.sin(age * 8.9 + 2.3);
            coil.headY += a * 0.4F * (float)Math.sin(age * 6.7 + 0.4);
        }
        if (from != null && age < 2.0) {
            RigPose start = copy(from);
            start.spin = wrap(start.spin);
            start.flip = wrap(start.flip);
            start.roll = wrap(start.roll);
            float u = smooth01((float)(age / 2.0));
            RigPose blended = start.scale(1.0F - u);
            blended.add(coil, u);
            coil = blended;
            overhead(coil, start, PaleStrikePoses.punchCoil0(), u);
        }
        grounded(coil);
        return coil;
    }

    /** Big Punch {@code age} ticks after it was let go, out of {@code from} (her charge as drawn; null: fully charged). */
    public static RigPose punch(double age, RigPose from) {
        RigPose start = from == null ? PaleStrikePoses.punchCoil1() : copy(from);
        start.spin = wrap(start.spin);
        start.flip = wrap(start.flip);
        start.roll = wrap(start.roll);
        Key[] keys = PaleStrikePoses.bigPunch();
        RigPose pose = sample(keys, (float)age, start);
        if (age < keys[1].at()) {
            // 6.2.0: let go out of anything (an arm up over her head after a throw), the arm comes down outside her head.
            overhead(pose, start, keys[1].pose(), (float)(age / keys[1].at()));
        }
        grounded(pose);
        return pose;
    }

    /** True for the moves posed here (the body is theirs while they run). */
    public static boolean animates(PaleMove move) {
        return switch (move) {
            case STRIKE, FLIP, PUNCH_CHARGE, PUNCH, SWIPE, GRAB_REACH, GRAB_HOLD, GRAB_THROW, GRAB_MISS, KICK, HAMMER_CHARGE, HAMMER, WATCH, WITNESS -> true;
            // 6.2.0: the Owl's own moves (OwlMotions).
            default -> move.owl();
        };
    }

    /**
     * True for the moves she makes facing wherever she looks (charging, holding up what she caught, or hurling the Owl's volley at
     * it); every other move is thrown the way she faced when it began, whatever her body has turned to since.
     */
    public static boolean facesLook(PaleMove move) {
        return move == PaleMove.PUNCH_CHARGE || move == PaleMove.GRAB_HOLD || move == PaleMove.HAMMER_CHARGE || move == PaleMove.OWL_PUNCH_CHARGE
            || move == PaleMove.OWL_FISSURE_CHARGE || move == PaleMove.OWL_VOLLEY || move == PaleMove.OWL_GRAB_REACH || move == PaleMove.OWL_GRAB_HOLD
            || move == PaleMove.OWL_GRAB_SLAM;
    }

    /**
     * Her body in a move of the flesh's ({@code move}, {@code age} ticks in, let go at {@code charge}) as the move alone poses it, out
     * of her guard, turned {@code turn} (radians, + to her right: how far the way she faced when it began is from her body's facing
     * now), settled as the game settles it: what the flesh is put on when her body isn't drawn this frame (in first person the model
     * is never set up, so the pose it was last drawn in is stale). Null for any other move.
     */
    public static RigPose fleshBody(PaleMove move, double age, float charge, float turn) {
        RigPose pose = switch (move) {
            case HAMMER_CHARGE -> hammerCharge(age, null);
            // Let go after the charge it was let go at (never before the leg was up).
            case HAMMER -> hammer(age, charge, hammerCharge(Math.max(PaleRules.HAMMER_RAISE_TICKS, charge * PaleRules.HAMMER_GROW_TICKS), null));
            case SWIPE, GRAB_REACH, GRAB_HOLD, GRAB_THROW, GRAB_MISS -> flesh(move, age, null);
            default -> null;
        };
        if (pose == null) {
            return null;
        }
        if (!facesLook(move)) {
            pose.spin -= turn;
            pose.headY -= turn;
        }
        pose.settle(1.0F);
        return pose;
    }

    // ------------------------------------------------------------------ the flesh's moves

    /**
     * Her body while the flesh moves ({@code move} {@code age} ticks in, out of {@code from}): the swipe (she conducts the sweep),
     * the grab reaching (and a miss), holding, and the throw. Null for any other move.
     */
    public static RigPose flesh(PaleMove move, double age, RigPose from) {
        Key[] keys = switch (move) {
            case SWIPE -> PaleStrikePoses.swipe();
            case GRAB_REACH, GRAB_MISS -> PaleStrikePoses.grabReach();
            case GRAB_HOLD -> PaleStrikePoses.grabHold();
            case GRAB_THROW -> PaleStrikePoses.grabThrow();
            default -> null;
        };
        if (keys == null) {
            return null;
        }
        RigPose start = from == null ? PaleStrikePoses.guard() : copy(from);
        start.spin = wrap(start.spin);
        start.flip = wrap(start.flip);
        start.roll = wrap(start.roll);
        RigPose pose = sample(keys, (float)age, start);
        if (age < keys[1].at()) {
            overhead(pose, start, keys[1].pose(), (float)(age / keys[1].at()));
        }
        grounded(pose);
        return pose;
    }

    /** How far out to her side (radians) the left arm swings as it is swept back into the lunge. */
    static final float RUSH_BACK_WIDE = 0.45F;

    /**
     * The Speed Kick {@code age} ticks in, the dash taking {@code dash} ticks, out of {@code from} (null: her guard). 6.2.0, Kizaru's
     * kick: thrown into the sprinter's lunge ({@link PaleStrikePoses#kickRush}) for {@link PaleRules#KICK_BURST}; gone through the dash
     * (unseen: the lunge gives way to the chamber she will be in); there, in the chamber ({@link PaleStrikePoses#kickChamber}: thrown
     * back, the right knee up hard), the leg driven straight out into its middle on {@link PaleRules#KICK_CONTACT} with her whole body
     * thrown out flat behind it, held out, the backflip off it, the crouch, her guard ({@link PaleStrikePoses#kick}). Her left sole is
     * the bottom of her while she stands; in the air ({@link PaleRules#kickAirborne}) her body keeps its own height.
     */
    public static RigPose kick(double age, int dash, RigPose from) {
        RigPose start = from == null ? PaleStrikePoses.guard() : copy(from);
        start.spin = wrap(start.spin);
        start.flip = wrap(start.flip);
        start.roll = wrap(start.roll);
        Key[] rush = PaleStrikePoses.kickRush();
        int arrive = PaleRules.kickArrive(dash);
        RigPose pose;
        if (age < PaleRules.KICK_BURST) {
            pose = sample(rush, (float)age, start);
            if (age < rush[1].at()) {
                float u = (float)(age / rush[1].at());
                overhead(pose, start, rush[1].pose(), u);
                // 6.2.0: a right arm up over her head comes straight down ahead of her (overhead holds its turn until it is down; a
                // swing out to the side there would carry it in over her head); the left, swept back behind her, goes out round her
                // side on its way, never through it.
                pose.leftArmZ -= RUSH_BACK_WIDE * (float)Math.sin(Math.PI * u);
            }
        } else if (age < arrive) {
            // Gone (nothing of her is drawn): the lunge giving way to the chamber she is in when she is there.
            float u = smooth01((float)((age - PaleRules.KICK_BURST) / Math.max(1, dash)));
            pose = new RigPose().add(sample(rush, PaleRules.KICK_BURST, start), 1.0F - u).add(PaleStrikePoses.kickChamber(), u);
        } else {
            // There: out of the chamber, the kick.
            pose = sample(PaleStrikePoses.kick(), (float)(age - arrive), PaleStrikePoses.kickChamber());
        }
        planted(pose);
        // In the air (thrown out flat behind the leg, the backflip off it) her body is held at its own height, not set down on her foot.
        pose.drop *= (float)(1.0 - PaleRules.kickAirborne(dash, age));
        return pose;
    }

    /**
     * 6.2.0: her body as {@code move} alone poses it ({@code age} ticks in; its detail and charge as sent), turned {@code turn} as
     * {@link #fleshBody} is, settled; her guard for anything else: what is put on her when her body isn't drawn this frame.
     */
    public static RigPose anyBody(PaleMove move, double age, int detail, float charge, float turn) {
        RigPose flesh = fleshBody(move, age, charge, turn);
        if (flesh != null) {
            return flesh;
        }
        if (move == PaleMove.KICK) {
            return kickBody(age, detail, turn);
        }
        RigPose owl = OwlMotions.anyBody(move, age, detail, charge, turn);
        if (owl != null) {
            return owl;
        }
        RigPose pose = move == PaleMove.WATCH ? watch(age, null) : guard();
        pose.settle(1.0F);
        return pose;
    }

    /**
     * Her body in the Speed Kick ({@code age} ticks in, the dash {@code dash} ticks), turned {@code turn} as {@link #fleshBody} is, and
     * settled: what her own leg is drawn on in first person.
     */
    public static RigPose kickBody(double age, int dash, float turn) {
        RigPose pose = kick(age, dash, null);
        pose.spin -= turn;
        pose.headY -= turn;
        pose.settle(1.0F);
        return pose;
    }

    // ------------------------------------------------------------------ Watch This

    /** Watch This, {@code age} ticks in, out of {@code from}: gathered, the arms flung out into a cross as the tentacles burst, held, settled. */
    public static RigPose watch(double age, RigPose from) {
        Key[] keys = PaleStrikePoses.watch();
        RigPose start = from == null ? PaleStrikePoses.guard() : copy(from);
        start.spin = wrap(start.spin);
        start.flip = wrap(start.flip);
        start.roll = wrap(start.roll);
        RigPose pose = sample(keys, (float)age, start);
        if (age < keys[1].at()) {
            overhead(pose, start, keys[1].pose(), (float)(age / keys[1].at()));
        }
        grounded(pose);
        return pose;
    }

    // ------------------------------------------------------------------ the Flesh Hammer

    /**
     * The Flesh Hammer held, {@code age} ticks in, out of {@code from}: the right leg lifted out straight and her whole body tipped
     * back under it, her left foot planted; held at full charge, a tremor runs through her.
     */
    public static RigPose hammerCharge(double age, RigPose from) {
        Key[] keys = PaleStrikePoses.hammerRaise();
        RigPose start = from == null ? PaleStrikePoses.guard() : copy(from);
        start.spin = wrap(start.spin);
        start.flip = wrap(start.flip);
        start.roll = wrap(start.roll);
        RigPose pose = sample(keys, (float)age, start);
        if (age < keys[1].at()) {
            overhead(pose, start, keys[1].pose(), (float)(age / keys[1].at()));
        }
        double full = smooth01((float)((age - PaleRules.HAMMER_GROW_TICKS) / 6.0));
        pose.lean += (float)(0.014 * full * Math.sin(age * 2.3));
        pose.flip += (float)(0.01 * full * Math.sin(age * 3.1 + 1.0));
        planted(pose);
        return pose;
    }

    /**
     * The axe kick, {@code age} ticks after the hammer was let go at {@code charge}, out of the held pose {@code from}: the last of
     * the wind-up, then the leg and everything with it come down, and at the blow the leg is at just the angle that lays the head of
     * a club this long on the ground ahead; it stays down while the club lies there and comes back to her as it draws in.
     */
    public static RigPose hammer(double age, float charge, RigPose from) {
        Key[] keys = PaleStrikePoses.hammerSlam();
        RigPose start = from == null ? hammerCharge(PaleRules.HAMMER_GROW_TICKS, null) : copy(from);
        start.spin = wrap(start.spin);
        start.flip = wrap(start.flip);
        start.roll = wrap(start.roll);
        RigPose pose = sample(keys, (float)age, start);
        planted(pose);
        // The keys (after the start): cocked, the blow, over it, down, back up, her guard. The leg comes down from the cock.
        double cocked = keys[1].at();
        double down = age < PaleRules.HAMMER_IMPACT ? smooth01((float)((age - cocked) / (PaleRules.HAMMER_IMPACT - cocked)))
            : 1.0 - smooth01((float)((age - PaleRules.HAMMER_RETRACT_AT) / (keys[keys.length - 1].at() - PaleRules.HAMMER_RETRACT_AT)));
        if (down > 0.0) {
            RigPose blow = copy(keys[2].pose());
            planted(blow);
            raise(pose, onGround(blow, charge) * down);
            planted(pose);
        }
        return pose;
    }

    /**
     * How much further (radians, + lowers it) {@code pose}'s right leg must turn for the head of a club grown {@code charge} to
     * rest on the ground (its underside level with her feet).
     */
    static double onGround(RigPose pose, float charge) {
        double length = PaleRules.hammerLength(charge);
        double girth = PaleRules.hammerGirth(charge);
        double delta = 0.0;
        for (int round = 0; round < 8; round++) {
            double gap = clubHead(pose, delta, length, girth);
            if (Math.abs(gap) < 0.005) {
                break;
            }
            double slope = (clubHead(pose, delta + 0.01, length, girth) - gap) / 0.01;
            if (Math.abs(slope) < 1.0E-6) {
                break;
            }
            delta = Math.max(-LEG_UP - TIP_BACK - pose.rightLegX, Math.min(-pose.rightLegX, delta - gap / slope));
        }
        return delta;
    }

    /** How far above the ground (blocks) the underside of the club's head is with the right leg turned {@code delta} more. */
    static double clubHead(RigPose pose, double delta, double length, double girth) {
        RigPose p = copy(pose);
        raise(p, delta);
        planted(p);
        double[] shin = BodyFrame.world(p, BodyFrame.model(p, "rightLeg", CLUB_START));
        double[] foot = BodyFrame.world(p, BodyFrame.model(p, "rightLeg", SOLE));
        double[] dir = {foot[0] - shin[0], foot[1] - shin[1], foot[2] - shin[2]};
        double shinLength = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        double along = (shinLength + length) * CLUB_HEAD - shinLength;
        double headY = foot[1] + dir[1] / shinLength * along;
        return headY - girth * HEAD_SINKS;
    }

    /**
     * Where the club's head is at the blow (rig space: its middle), {@code charge} grown, on {@code pose} (the slam's own pose):
     * where the blow lands.
     */
    public static double[] clubHeadAt(RigPose pose, float charge) {
        RigPose p = copy(pose);
        raise(p, onGround(p, charge));
        planted(p);
        double length = PaleRules.hammerLength(charge);
        double[] shin = BodyFrame.world(p, BodyFrame.model(p, "rightLeg", CLUB_START));
        double[] foot = BodyFrame.world(p, BodyFrame.model(p, "rightLeg", SOLE));
        double[] dir = {foot[0] - shin[0], foot[1] - shin[1], foot[2] - shin[2]};
        double shinLength = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        double along = (shinLength + length) * CLUB_HEAD - shinLength;
        return new double[] {foot[0] + dir[0] / shinLength * along, foot[1] + dir[1] / shinLength * along, foot[2] + dir[2] / shinLength * along};
    }

    /** How low the club's head comes down: its middle this many of its radii above the ground (the rest sinks into the crater). */
    static final double HEAD_SINKS = 0.6;

    /** As high as the leg lifts (radians), and how much further back her whole body may tip to lift it more. */
    static final double LEG_UP = 1.6;
    static final double TIP_BACK = 0.8;

    /**
     * Her right leg turned {@code delta} more (- lifts it): past as high as it goes, the rest of the lift is her whole body tipping
     * back under it.
     */
    static void raise(RigPose p, double delta) {
        double legX = p.rightLegX + delta;
        if (delta < 0.0 && legX < -LEG_UP) {
            // Only the lift itself goes into the tip: a leg already up past LEG_UP stays as it is.
            double past = Math.min(-delta, -LEG_UP - legX);
            p.flip += (float)past;
            legX += past;
        }
        p.rightLegX = (float)legX;
    }

    /** Where the club starts on her shin, and her sole (the right leg's own pixels), and how far along the club its head is. */
    static final double[] CLUB_START = {0.0, 7.0, 0.0};
    static final double[] SOLE = {0.0, 12.0, 0.0};
    static final double CLUB_HEAD = 0.8;

    /** Her body lowered (or raised) so the lowest corner of her left sole is on the ground, however the whole body is tipped. */
    static void planted(RigPose p) {
        p.drop = 0.0F;
        double lowest = Double.MAX_VALUE;
        for (double x : new double[] {-2.0, 2.0}) {
            for (double z : new double[] {-2.0, 2.0}) {
                lowest = Math.min(lowest, BodyFrame.world(p, BodyFrame.model(p, "leftLeg", new double[] {x, 12.0, z}))[1]);
            }
        }
        p.drop = (float)(lowest * 16.0);
    }

    /** Her guard, as drawn between strikes. */
    public static RigPose guard() {
        RigPose pose = PaleStrikePoses.guard();
        grounded(pose);
        return pose;
    }

    /**
     * Lowers the body by what its legs lift it ({@link RigPose#legDrop}): the lower foot stays on the ground and neither sinks
     * into it, whatever the legs do.
     */
    static void grounded(RigPose p) {
        p.drop = Math.max(0.0F, Math.min(RigPose.legDrop(p.rightLegX, p.rightLegZ, p.rightKnee), RigPose.legDrop(p.leftLegX, p.leftLegZ, p.leftKnee)));
    }

    /**
     * A move's pose as it is blended back out of once the move ends: its whole-body turns as the nearest angle. (A flip ends a
     * full turn round, which is no turn at all; blended out from 360 degrees, the body would spin all the way back the other way.)
     */
    public static RigPose ended(RigPose pose) {
        RigPose out = copy(pose);
        out.spin = wrap(out.spin);
        out.flip = wrap(out.flip);
        out.roll = wrap(out.roll);
        return out;
    }

    /** An angle in [-pi, pi). */
    static float wrap(float angle) {
        double turn = Math.PI * 2.0;
        return (float)(angle - turn * Math.floor((angle + Math.PI) / turn));
    }

    // ------------------------------------------------------------------ 6.2.0: one move cut off by another

    /**
     * A move cut off mid-way by a new one: the pose the new one starts from (exactly what was drawn, its whole-body turns taken off)
     * and those turns ({@link #flip}, {@link #roll}, {@link #spin}: radians, the same turns as drawn) concluded on top of it over
     * {@link #ticks}: on round the way the body was turning when that is not too far (a flip spun on to its end as the punch comes),
     * otherwise back the shortest way, fast at first and slowing into the new move. Never a jump, never a turn spun back.
     */
    public record Cut(RigPose from, double flip, double roll, double spin, double ticks, double rightArmZ, double leftArmZ) {
        /** The turns still to conclude {@code age} ticks into the new move (added to its own). */
        public double[] left(double age) {
            double u = Math.max(0.0, Math.min(1.0, age / this.ticks));
            double k = Math.pow(1.0 - u, 1.6);
            return new double[] {this.flip * k, this.roll * k, this.spin * k};
        }

        /**
         * {@code pose} ({@code age} ticks into the new move) with an arm that was cut off held up and out to her side unwound as it
         * comes down: that same angle on a lowered arm would turn it in across her, so the further down the arm is drawn the more
         * of it goes, until the new move's own angle has taken over.
         */
        public void unwind(RigPose pose, double age) {
            double k = 1.0 - smooth01((float)(age / ARM_UNWIND_FADE));
            if (k <= 0.0) {
                return;
            }
            pose.rightArmZ -= (float)(this.rightArmZ * k * smooth01((pose.rightArmX + LOWERED) / LOWERED_SPAN));
            pose.leftArmZ -= (float)(this.leftArmZ * k * smooth01((pose.leftArmX + LOWERED) / LOWERED_SPAN));
        }

        /**
         * Through the cut ({@code age} ticks in, until a little after its turns are concluded): an arm that would graze her torso or
         * head on its way into the new move is turned aside just enough that it doesn't ({@link #clear}).
         */
        public void clearOf(RigPose pose, double age) {
            if (age <= this.ticks + CLEAR_AFTER) {
                clear(pose);
            }
        }
    }

    /** Ticks after a cut's turns are concluded that its arms are still kept clear of her; the graze allowed (as the tests allow). */
    static final double CLEAR_AFTER = 3.0;
    static final double CLEAR_TOLERANCE = 0.25;
    /** How far (radians) an arm is turned aside each try, and how many tries at most. */
    static final float CLEAR_STEP = 0.08F;
    static final int CLEAR_TRIES = 6;

    /**
     * {@code pose}'s arms kept out of her torso and head, measured exactly as the tests measure it ({@link BodyFrame#worstClip}):
     * while an arm grazes, it is turned aside a little (its sideways angle, its twist, its elbow or its lift), whichever grazes least, and given
     * up on once nothing helps. Only a cut's few ticks pay for it.
     */
    static void clear(RigPose pose) {
        for (int attempt = 0; attempt < CLEAR_TRIES; attempt++) {
            double[] worst = BodyFrame.worstClip(pose, CLEAR_TOLERANCE);
            if (worst[0] <= 0.0) {
                return;
            }
            int side = (int)worst[1];
            double best = worst[0];
            float[] tried = null;
            for (int channel = 0; channel < 4; channel++) {
                for (float step : new float[] {CLEAR_STEP, -CLEAR_STEP}) {
                    float was = arm(pose, side, channel);
                    setArm(pose, side, channel, was + step);
                    double depth = BodyFrame.clipping(pose, CLEAR_TOLERANCE);
                    setArm(pose, side, channel, was);
                    if (depth < best) {
                        best = depth;
                        tried = new float[] {channel, step};
                    }
                }
            }
            if (tried == null) {
                return; // nothing helps from here
            }
            int channel = (int)tried[0];
            setArm(pose, side, channel, arm(pose, side, channel) + tried[1]);
        }
    }

    /** An arm's sideways angle (channel 0), twist (1), elbow (2) or lift (3), of her right arm (side 0) or left. */
    static float arm(RigPose pose, int side, int channel) {
        boolean right = side == 0;
        return switch (channel) {
            case 0 -> right ? pose.rightArmZ : pose.leftArmZ;
            case 1 -> right ? pose.rightArmY : pose.leftArmY;
            case 2 -> right ? pose.rightElbow : pose.leftElbow;
            default -> right ? pose.rightArmX : pose.leftArmX;
        };
    }

    static void setArm(RigPose pose, int side, int channel, float value) {
        boolean right = side == 0;
        switch (channel) {
            case 0 -> {
                if (right) {
                    pose.rightArmZ = value;
                } else {
                    pose.leftArmZ = value;
                }
            }
            case 1 -> {
                if (right) {
                    pose.rightArmY = value;
                } else {
                    pose.leftArmY = value;
                }
            }
            case 2 -> {
                if (right) {
                    pose.rightElbow = Math.max(0.0F, value);
                } else {
                    pose.leftElbow = Math.max(0.0F, value);
                }
            }
            default -> {
                if (right) {
                    pose.rightArmX = value;
                } else {
                    pose.leftArmX = value;
                }
            }
        }
    }

    /**
     * How far up (radians, arm X) an arm counts as raised on a cut: brought down outside her head (over {@link #RAISED_SPAN}), and
     * held out to her side there, that angle unwound as it comes down ({@link Cut#unwind}).
     */
    static final float RAISED = 1.3F;
    static final float RAISED_SPAN = 0.6F;
    /** By when (ticks) a cut-off raised arm's unwinding has handed over to the new move; how far down (arm X) it starts, and is whole. */
    static final double ARM_UNWIND_FADE = 3.5;
    static final float LOWERED = 1.3F;
    static final float LOWERED_SPAN = 0.8F;

    /** How fast (radians a tick) a cut-off turn is concluded, the least and most ticks it takes, and the furthest on round it goes. */
    static final double CONCLUDE_RATE = 1.1;
    static final double CONCLUDE_MIN = 2.0;
    static final double CONCLUDE_MAX = 5.0;
    static final double CONCLUDE_MOST = Math.PI * 1.25;

    /**
     * The new move's start out of {@code drawn} (the pose drawn when it began), {@code before} the pose drawn the frame before (how the
     * body was turning; null: still).
     */
    public static Cut cut(RigPose drawn, RigPose before) {
        RigPose from = copy(drawn);
        double flip = conclude(drawn.flip, before == null ? 0.0 : drawn.flip - before.flip);
        double roll = conclude(drawn.roll, before == null ? 0.0 : drawn.roll - before.roll);
        double spin = conclude(drawn.spin, before == null ? 0.0 : drawn.spin - before.spin);
        from.flip = 0.0F;
        from.roll = 0.0F;
        from.spin = 0.0F;
        from.cut = 1.0F;
        double most = Math.max(Math.abs(flip), Math.max(Math.abs(roll), Math.abs(spin)));
        // A raised arm held out to her side (-Z on her right, +Z on her left).
        double right = -drawn.rightArmX > RAISED && drawn.rightArmZ < 0.0F ? drawn.rightArmZ : 0.0;
        double left = -drawn.leftArmX > RAISED && drawn.leftArmZ > 0.0F ? drawn.leftArmZ : 0.0;
        return new Cut(from, flip, roll, spin, Math.max(CONCLUDE_MIN, Math.min(CONCLUDE_MAX, most / CONCLUDE_RATE)), right, left);
    }

    /**
     * The same turn as {@code angle}, as the start of its way back to none: on round the way it was turning ({@code rate}, + or -)
     * when that is at most {@link #CONCLUDE_MOST}, else the shortest way.
     */
    static double conclude(double angle, double rate) {
        double shortest = wrap((float)angle);
        if (Math.abs(rate) < 1.0E-4) {
            return shortest;
        }
        // Going from the start to none turns it the way of minus the start: on round means the start's sign against the rate's.
        double on = rate > 0.0 ? (shortest < 0.0 ? shortest : shortest - Math.PI * 2.0) : (shortest > 0.0 ? shortest : shortest + Math.PI * 2.0);
        return Math.abs(on) <= CONCLUDE_MOST ? on : shortest;
    }

    /**
     * True when {@code now} carries on from {@code was} as one move (the punch let go out of its charge, the slam out of the hammer's
     * raise, the grab's hold out of its reach, its throw out of the hold, a reach that missed): not a cut, its own keys follow on.
     */
    public static boolean continues(PaleMove was, PaleMove now) {
        return was == PaleMove.PUNCH_CHARGE && now == PaleMove.PUNCH || was == PaleMove.HAMMER_CHARGE && now == PaleMove.HAMMER
            || was == PaleMove.GRAB_REACH && (now == PaleMove.GRAB_HOLD || now == PaleMove.GRAB_MISS || now == PaleMove.GRAB_THROW)
            || was == PaleMove.GRAB_HOLD && now == PaleMove.GRAB_THROW || was == PaleMove.STRIKE && now == PaleMove.STRIKE
            || was == PaleMove.OWL_PUNCH_CHARGE && now == PaleMove.OWL_PUNCH || was == PaleMove.OWL_STRIKE && now == PaleMove.OWL_STRIKE
            || was == PaleMove.OWL_FISSURE_CHARGE && now == PaleMove.OWL_FISSURE;
    }

    /**
     * True for the moves of her arms and upper body (her blows, the flesh, the cross): thrown in the air, her legs carry on with what
     * they were doing ({@link #legsFrom}) until she lands; the rest (the punch, the kick, the hammer, a flip) take her whole body.
     */
    public static boolean upperBody(PaleMove move) {
        return switch (move) {
            case STRIKE, SWIPE, GRAB_REACH, GRAB_HOLD, GRAB_THROW, GRAB_MISS, WATCH, OWL_STRIKE -> true;
            default -> false;
        };
    }

    /** {@code pose} with {@code legs}' legs (and how they hold) put on it by {@code weight}: the rest of it is its own. */
    public static void legsFrom(RigPose pose, RigPose legs, float weight) {
        if (weight <= 0.0F) {
            return;
        }
        pose.rightLegX += (legs.rightLegX - pose.rightLegX) * weight;
        pose.rightLegY += (legs.rightLegY - pose.rightLegY) * weight;
        pose.rightLegZ += (legs.rightLegZ - pose.rightLegZ) * weight;
        pose.rightKnee += (legs.rightKnee - pose.rightKnee) * weight;
        pose.leftLegX += (legs.leftLegX - pose.leftLegX) * weight;
        pose.leftLegY += (legs.leftLegY - pose.leftLegY) * weight;
        pose.leftLegZ += (legs.leftLegZ - pose.leftLegZ) * weight;
        pose.leftKnee += (legs.leftKnee - pose.leftKnee) * weight;
        pose.rightKneeTurn += (legs.rightKneeTurn - pose.rightKneeTurn) * weight;
        pose.leftKneeTurn += (legs.leftKneeTurn - pose.leftKneeTurn) * weight;
        pose.legHold += (legs.legHold - pose.legHold) * weight;
        pose.drop += (legs.drop - pose.drop) * weight;
        pose.lunge += (legs.lunge - pose.lunge) * weight;
    }

    // ------------------------------------------------------------------ flips

    /**
     * A flip {@code age} ticks in (the push is at once): the takeoff (arms swung from behind her up over her head, the legs driving
     * straight), the body turning over once about its middle, fastest in the middle, while she tucks tight (knees to the chest,
     * hands to the shins), then opening out (legs reaching for the ground, arms out for balance). A side flip goes over spread
     * out, legs split and arms wide. {@code landed} ticks since she touched down again (negative: still in the air): the legs soak
     * it up.
     */
    public static RigPose flip(PaleRules.Flip flip, double age, double landed) {
        return flip(flip, age, landed, 1.0, PaleRules.FLIP_SPIN_TICKS);
    }

    /**
     * 6.2.0, Up We Go: the flip as she soars ({@link PaleRules#soar}): stretched long, arms up, rising through the launch's ramp like
     * a dart, then turning over (slower, across the longer climb) from the top of it.
     */
    public static RigPose soar(PaleRules.Flip flip, double age, double landed) {
        return flip(flip, age, landed, PaleRules.SOAR_RAMP - 1.0, PaleRules.SOAR_SPIN_TICKS);
    }

    /** A flip that starts turning over {@code from} ticks in and takes {@code spinTicks} to. */
    static RigPose flip(PaleRules.Flip flip, double age, double landed, double from, double spinTicks) {
        RigPose p = new RigPose();
        // In the air the limbs are all the flip's: no walking swing under it.
        p.armHold = 1.0F;
        p.legHold = 1.0F;
        float takeoff = smooth01((float)(age / 1.5));
        double spin = Math.max(0.0, Math.min(1.0, (age - from) / spinTicks));
        // Smootherstep: slow off the ground, whipping over in the middle, slowing to open out.
        double turn = spin * spin * spin * (spin * (spin * 6.0 - 15.0) + 10.0);
        float full = (float)(Math.PI * 2.0);
        switch (flip) {
            case FORWARD -> p.flip = -full * (float)turn;
            case BACK -> p.flip = full * (float)turn;
            // A side flip toward her left rolls her over to the left (+roll lifts the right side), and the other way to the right.
            case LEFT -> p.roll = full * (float)turn;
            case RIGHT -> p.roll = -full * (float)turn;
        }
        // One phase after the other: off the ground, into the tuck (held), opened out for the landing.
        float tuck = smooth01((float)((spin - 0.03) / 0.3));
        float open = smooth01((float)((spin - 0.68) / 0.3));
        if (landed >= 0.0) {
            // Touching down (early, on a ledge, or on time) opens her out at once: she lands on her feet, not tucked.
            open = Math.max(open, smooth01((float)((landed + 1.0) / 1.2)));
        }
        boolean side = flip == PaleRules.Flip.LEFT || flip == PaleRules.Flip.RIGHT;
        // Arms: swung up from behind to over her head (out a little: on a raised arm that is -Z), then (a front or back flip) in
        // to the shins or (a side flip) out wide, then out for balance. Z comes round late, so a raised arm never turns in at her head.
        float armX = lerp(0.7F, -2.7F, takeoff);
        // (Soaring, 6.2.0, she rises like a dart: her arms nearly together over her head.)
        float armZ = lerp(0.15F, from > 1.5 ? -0.23F : -0.3F, takeoff);
        float elbow = lerp(0.3F, 0.1F, takeoff);
        armX = lerp(armX, side ? -0.35F : -1.15F, tuck);
        armZ = lerp(armZ, side ? 1.3F : 0.4F, tuck * tuck);
        elbow = lerp(elbow, side ? 0.25F : 0.95F, tuck);
        armX = lerp(armX, -0.45F, open);
        armZ = lerp(armZ, 1.05F, open);
        elbow = lerp(elbow, 0.35F, open);
        // Legs: crouched to drive, straight off the ground, tucked (or split), reaching down.
        float legX = lerp(-0.5F, 0.0F, takeoff);
        float knee = lerp(1.0F, 0.08F, takeoff);
        float split = 0.0F;
        legX = lerp(legX, side ? -0.25F : -1.55F, tuck);
        knee = lerp(knee, side ? 0.9F : 2.0F, tuck);
        split = lerp(split, side ? 0.75F : 0.12F, tuck);
        legX = lerp(legX, -0.3F, open);
        knee = lerp(knee, 0.45F, open);
        split = lerp(split, 0.1F, open);
        float lean = lerp(lerp(lerp(0.35F, -0.1F, takeoff), side ? 0.05F : 0.3F, tuck), 0.12F, open);
        float headX = lerp(lerp(lerp(0.1F, -0.3F, takeoff), side ? 0.0F : 0.16F, tuck), 0.05F, open);
        p.rightArmX = armX;
        p.leftArmX = armX;
        p.rightArmZ = armZ;
        p.leftArmZ = -armZ;
        p.rightElbow = elbow;
        p.leftElbow = elbow;
        p.rightLegX = legX;
        p.leftLegX = legX;
        p.rightKnee = knee;
        p.leftKnee = knee;
        p.rightLegZ = split;
        p.leftLegZ = -split;
        p.lean = lean;
        p.headX = headX;
        if (landed >= 0.0) {
            // Touchdown: the knees give and spring back, the arms settle.
            float soak = (float)Math.max(0.0, 1.0 - landed / 6.0) * (float)Math.sin(Math.PI * Math.min(1.0, landed / 6.0 + 0.25));
            p.rightKnee += 0.9F * soak;
            p.leftKnee += 0.9F * soak;
            p.rightLegX -= 0.4F * soak;
            p.leftLegX -= 0.4F * soak;
            p.lean += 0.25F * soak;
            p.rightArmX -= 0.25F * soak;
            p.leftArmX -= 0.25F * soak;
            grounded(p);
        }
        return p;
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float smooth01(float u) {
        float c = Math.max(0.0F, Math.min(1.0F, u));
        return c * c * (3.0F - 2.0F * c);
    }

    // ------------------------------------------------------------------ helpers

    /** The pose {@code t} ticks into {@code keys}, the first (null) key being {@code start}. */
    static RigPose sample(Key[] keys, float t, RigPose start) {
        RigPose previous = start;
        float previousAt = keys[0].at();
        for (int i = 1; i < keys.length; i++) {
            Key key = keys[i];
            if (t <= key.at()) {
                float span = key.at() - previousAt;
                float u = span <= 0.0F ? 1.0F : Math.max(0.0F, (t - previousAt) / span);
                u = key.ease().apply(u);
                RigPose out = new RigPose();
                out.add(previous, 1.0F - u);
                out.add(key.pose(), u);
                return out;
            }
            previous = key.pose();
            previousAt = key.at();
        }
        return copy(previous);
    }

    static RigPose copy(RigPose pose) {
        return new RigPose().add(pose, 1.0F);
    }

    /** Left for right: the arms and legs swap, everything sideways turns the other way. */
    public static RigPose mirror(RigPose p) {
        RigPose m = new RigPose();
        m.headX = p.headX;
        m.headY = -p.headY;
        m.headZ = -p.headZ;
        m.lean = p.lean;
        m.side = -p.side;
        m.twist = -p.twist;
        m.drop = p.drop;
        m.lunge = p.lunge;
        m.armHold = p.armHold;
        m.legHold = p.legHold;
        m.cut = p.cut;
        m.flip = p.flip;
        m.roll = -p.roll;
        m.spin = -p.spin;
        m.rightArmX = p.leftArmX;
        m.leftArmX = p.rightArmX;
        m.rightArmY = -p.leftArmY;
        m.leftArmY = -p.rightArmY;
        m.rightArmZ = -p.leftArmZ;
        m.leftArmZ = -p.rightArmZ;
        m.rightElbow = p.leftElbow;
        m.leftElbow = p.rightElbow;
        m.rightShoulderY = p.leftShoulderY;
        m.leftShoulderY = p.rightShoulderY;
        m.rightShoulderZ = p.leftShoulderZ;
        m.leftShoulderZ = p.rightShoulderZ;
        m.rightLegX = p.leftLegX;
        m.leftLegX = p.rightLegX;
        m.rightLegY = -p.leftLegY;
        m.leftLegY = -p.rightLegY;
        m.rightLegZ = -p.leftLegZ;
        m.leftLegZ = -p.rightLegZ;
        m.rightKnee = p.leftKnee;
        m.leftKnee = p.rightKnee;
        m.rightKneeTurn = -p.leftKneeTurn;
        m.leftKneeTurn = -p.rightKneeTurn;
        return m;
    }
}
