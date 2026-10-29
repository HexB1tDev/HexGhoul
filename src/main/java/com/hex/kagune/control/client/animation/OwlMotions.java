package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.eto.EtoMove;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.eto.OwlRules;
import com.hex.kagune.control.eto.OwlShards;
import com.hex.kagune.control.eto.OwlSpikes;

/**
 * 6.2.0: the Owl's own moves (Eto's first kakuja; pure, tested): her body ({@link #body}) and the two giant arms ({@link #swing}),
 * every client posing them from the move the server began and how long ago, as her base moves are ({@link EtoMotions}).
 *
 * <p>The giant arms are heavy: each is keyed where its upper arm and its blade go, and what is drawn follows the keys with its
 * weight ({@link #heavy}): the upper arm a little behind what drives it, the blade, heavier at its end, a touch further behind, so
 * as the arm swings the blade trails a little and settles when it stops; and the faster the blade swings, the more it bows under its
 * own weight. The red muscle does the work: it swings the arm, and the blade rides on it, only ever bent so far off it at the elbow
 * ({@link #carried}), never flailing round on its own. Every move starts from the arms exactly as they were drawn ({@code start}),
 * and they are eased back into her idle after it ({@link OwlArms}).
 */
public final class OwlMotions {
    private OwlMotions() {
    }

    /** How far (ticks) the upper arm lags what drives it, and the blade (its heavy end, carried on it). */
    static final double UPPER_LAG = 0.35;
    static final double BLADE_LAG = 0.5;
    /** How far (blocks) the blade bows for each radian a tick it swings, and the most it bows. */
    static final double BOW_PER_SPEED = 0.08;
    static final double BOW_MOST = 0.2;
    /**
     * How far (radians) a blade is ever bent off the line of its upper arm at the elbow, least and most (at rest it is bent about
     * 0.75): the muscle swings the arm, the blade rides on it.
     */
    public static final double ELBOW_LEAST = 0.35;
    public static final double ELBOW_MOST = 0.87;
    /** After one of its moves, how long (ticks) her body takes to ease back into the Owl's stance. */
    public static final float BODY_OUT = 6.0F;

    /** A giant arm's key: {@code at} ticks into the move, swung {@code swing} (null: as it began), eased into as {@code ease} says. */
    record ArmKey(float at, OwlPose.Swing swing, EtoMotions.Ease ease) {
        static final ArmKey START = new ArmKey(0.0F, null, EtoMotions.Ease.SMOOTH);
    }

    /** True for the moves whose giant arms are keyed here (the rest leave them to her idle). */
    public static boolean armed(EtoMove move) {
        return move == EtoMove.OWL_PUNCH_CHARGE || move == EtoMove.OWL_PUNCH || move == EtoMove.OWL_KICK || move == EtoMove.OWL_SPIKES
            || move == EtoMove.OWL_STRIKE || move == EtoMove.OWL_WHIRL || move == EtoMove.OWL_LEAP || move == EtoMove.OWL_FISSURE_CHARGE
            || move == EtoMove.OWL_FISSURE || move == EtoMove.OWL_WATCH || move == EtoMove.OWL_VOLLEY;
    }

    /**
     * How much (0..1) the giant arms turn with her whole body's own turns in {@code move}, {@code age} ticks in: the kick's spin, once
     * it begins (the arms whirl round with her; before it, the blade stays in what it skewered whatever her hips do); everything
     * else stays keyed to the way she faces (so the punch lands where she aims).
     */
    static double followsSpin(EtoMove move, double age, int detail) {
        if (move == EtoMove.OWL_WHIRL) {
            return 1.0; // round with her, all of it
        }
        if (move != EtoMove.OWL_KICK) {
            return 0.0;
        }
        return OwlPose.smooth((age - Math.max(1, detail) - KICK_FOLLOW_FROM) / KICK_FOLLOW_OVER);
    }

    /**
     * Through the kick's skewer and until it is drawn out (ticks after the dash) the right blade is held aimed at this height
     * (blocks), let go over {@link #KICK_AIM_OUT} ticks (the whole arm swings it out: no snap).
     */
    static final double KICK_AIM_FROM = 1.3;
    static final double KICK_AIM_UNTIL = 6.1;
    static final double KICK_AIM_OUT = 1.1;
    static final double KICK_AIM_HEIGHT = 1.0;

    /** From when (ticks after the dash) and over how long the kick's arms come to turn with her spin. */
    static final double KICK_FOLLOW_FROM = 6.6;
    static final double KICK_FOLLOW_OVER = 1.2;

    // ------------------------------------------------------------------ her body

    /**
     * Her body in the Owl's {@code move}, {@code age} ticks in (its detail and charge as sent), out of {@code from} (the pose she was
     * drawn in when it began; null: the Owl's stance): bulked up under the kakuja throughout. Null for any other move.
     */
    public static RigPose body(EtoMove move, double age, int detail, float charge, RigPose from) {
        RigPose pose = switch (move) {
            case OWL_PUNCH_CHARGE -> punchCharge(age, from);
            case OWL_PUNCH -> punch(age, from);
            case OWL_KICK -> kick(age, detail, from);
            case OWL_SPIKES -> spikes(age, detail, charge, from);
            case OWL_STRIKE -> rend(age, detail, from);
            case OWL_WHIRL -> whirl(age, from);
            case OWL_LEAP -> pounce(age, charge, from);
            case OWL_FISSURE_CHARGE -> fissureCharge(age, from);
            case OWL_FISSURE -> fissure(age, from);
            case OWL_WATCH -> watch(age, from);
            case OWL_VOLLEY -> volley(age, detail == 1, charge, from);
            case OWL_GRAB_REACH, OWL_GRAB_HOLD, OWL_GRAB_SLAM, OWL_GRAB_MISS -> grab(age, from);
            default -> null;
        };
        if (pose != null) {
            pose.bulk = 1.0F;
        }
        return pose;
    }

    /**
     * Her body in the Owl's {@code move} as the move alone poses it (out of the Owl's stance), turned {@code turn} as
     * {@link EtoMotions#fleshBody} is, settled: what is put on her when her body isn't drawn this frame (first person). Null for any
     * other move.
     */
    public static RigPose anyBody(EtoMove move, double age, int detail, float charge, float turn) {
        RigPose pose = body(move, age, detail, charge, null);
        if (pose == null) {
            return null;
        }
        if (!EtoMotions.facesLook(move)) {
            pose.spin -= turn;
            pose.headY -= turn;
        }
        pose.settle(1.0F);
        return pose;
    }

    /** The Owl's stance as a move starts from it when there is nothing drawn to start from. */
    static RigPose stance() {
        RigPose p = OwlPose.stance();
        OwlPose.body(p, true, 1000.0, 0.0);
        return p;
    }

    /** A move's start: what was drawn (its whole-body turns as the nearest angle), or the Owl's stance. */
    static RigPose start(RigPose from) {
        RigPose start = from == null ? stance() : EtoMotions.copy(from);
        start.spin = EtoMotions.wrap(start.spin);
        start.flip = EtoMotions.wrap(start.flip);
        start.roll = EtoMotions.wrap(start.roll);
        return start;
    }

    // ------------------------------------------------------------------ its Big Punch (Z)

    /** Ticks her body takes to sink out of what she was doing into the coil. */
    static final double PUNCH_INTO = 2.5;

    /**
     * The coil of the Owl's Big Punch, {@code charge} of the way full: the Big Punch's own coil sunk far deeper under the kakuja's
     * weight, hunched over her knees, wound further round.
     */
    static RigPose punchCoil(float charge) {
        RigPose coil = EtoStrikePoses.punchCoil0().scale(1.0F - charge);
        coil.add(EtoStrikePoses.punchCoil1(), charge);
        coil.lean += 0.12F + 0.12F * charge;
        coil.headX -= 0.08F + 0.06F * charge;
        coil.twist += 0.1F * charge;
        coil.rightLegX -= 0.15F + 0.15F * charge;
        coil.leftLegX -= 0.15F + 0.15F * charge;
        coil.rightKnee += 0.3F + 0.25F * charge;
        coil.leftKnee += 0.3F + 0.25F * charge;
        return coil;
    }

    /**
     * Z held {@code age} ticks in the Owl, out of {@code from}: she sinks into the coil as it charges, deeper and further round; nearly
     * full, the whole of her trembles with it.
     */
    static RigPose punchCharge(double age, RigPose from) {
        float charge = EtoMotions.smooth01((float)(age / EtoRules.PUNCH_CHARGE_TICKS));
        RigPose coil = punchCoil(charge);
        float tremble = EtoMotions.smooth01((float)((age - EtoRules.PUNCH_CHARGE_TICKS * 0.6) / (EtoRules.PUNCH_CHARGE_TICKS * 0.4)));
        if (tremble > 0.0F) {
            float a = 0.04F * tremble;
            coil.rightArmX += a * (float)Math.sin(age * 7.3);
            coil.twist += a * 0.6F * (float)Math.sin(age * 5.3 + 1.1);
            coil.lean += a * 0.5F * (float)Math.sin(age * 8.9 + 2.3);
            coil.headY += a * 0.4F * (float)Math.sin(age * 6.7 + 0.4);
        }
        if (age < PUNCH_INTO) {
            RigPose start = start(from);
            float u = EtoMotions.smooth01((float)(age / PUNCH_INTO));
            RigPose blended = EtoMotions.copy(start).scale(1.0F - u);
            blended.add(coil, u);
            EtoMotions.overhead(blended, start, punchCoil(0.0F), u);
            coil = blended;
        }
        EtoMotions.grounded(coil);
        return coil;
    }

    /**
     * The Owl's Big Punch {@code age} ticks after it was let go, out of {@code from} (her coil as drawn; null: full): the Big Punch's
     * drive, heavier, the lunge longer, the weight thrown in behind it.
     */
    static RigPose punch(double age, RigPose from) {
        RigPose pose = EtoMotions.punch(age, from == null ? punchCoil(1.0F) : from);
        // The weight behind it: sunk a little deeper, leaning further into the blow, then easing up out of it.
        float into = EtoMotions.smooth01((float)(age / EtoRules.PUNCH_IMPACT)) * (1.0F - EtoMotions.smooth01((float)((age - 9.0) / 9.0)));
        pose.lean += 0.05F * into;
        pose.lunge += 1.0F * into;
        pose.leftKnee += 0.1F * into;
        EtoMotions.grounded(pose);
        return pose;
    }

    // ------------------------------------------------------------------ its Speed Kick (V tapped)

    /** A pose built from the Owl's stance by {@code edit}. */
    static RigPose key(java.util.function.Consumer<RigPose> edit) {
        RigPose p = new RigPose();
        p.armHold = 1.0F;
        p.legHold = 1.0F;
        p.bulk = 1.0F;
        edit.accept(p);
        return p;
    }

    /** Shooting along at the dash: low, leaning hard into it, her arms swept back. */
    static RigPose kickRush() {
        return key(p -> {
            p.lean = 0.6F; p.headX = -0.45F;
            p.rightLegX = -0.7F; p.rightKnee = 1.1F; p.leftLegX = 0.55F; p.leftKnee = 0.5F;
            p.rightArmX = 0.75F; p.rightArmZ = 0.25F; p.rightElbow = 0.35F;
            p.leftArmX = 0.75F; p.leftArmZ = -0.25F; p.leftElbow = 0.35F;
        });
    }

    /** Planted, her right side driven forward behind the skewer. */
    static RigPose kickSkewer() {
        return key(p -> {
            p.lean = 0.32F; p.headX = -0.25F; p.twist = -0.5F; p.spin = 0.15F; p.lunge = 2.0F;
            p.rightLegX = -0.75F; p.rightKnee = 1.05F; p.leftLegX = 0.45F; p.leftKnee = 0.35F;
            p.rightArmX = -1.25F; p.rightArmY = 0.15F; p.rightArmZ = 0.2F; p.rightElbow = 0.35F;
            p.leftArmX = 0.55F; p.leftArmZ = -0.25F; p.leftElbow = 0.9F;
        });
    }

    /** Wound up to her left for the slash, the skewer still in it. */
    static RigPose kickWind() {
        return key(p -> {
            p.lean = 0.3F; p.headX = -0.2F; p.twist = -0.75F; p.spin = 0.25F; p.lunge = 1.6F;
            p.rightLegX = -0.7F; p.rightKnee = 1.0F; p.leftLegX = 0.45F; p.leftKnee = 0.4F;
            p.rightArmX = -1.15F; p.rightArmY = 0.1F; p.rightArmZ = 0.25F; p.rightElbow = 0.45F;
            p.leftArmX = 0.35F; p.leftArmZ = -0.55F; p.leftElbow = 1.1F;
        });
    }

    /** The slash: unwound hard to her right, the left side thrown through with it. */
    static RigPose kickSlash() {
        return key(p -> {
            p.lean = 0.3F; p.headX = -0.2F; p.twist = 0.45F; p.spin = -0.1F; p.lunge = 1.4F;
            p.rightLegX = -0.65F; p.rightKnee = 0.95F; p.leftLegX = 0.4F; p.leftKnee = 0.4F;
            p.rightArmX = -0.9F; p.rightArmY = 0.2F; p.rightArmZ = 0.3F; p.rightElbow = 0.6F;
            p.leftArmX = -1.0F; p.leftArmY = 0.05F; p.leftArmZ = -0.35F; p.leftElbow = 0.5F;
        });
    }

    /** On through, her weight coming over onto her left foot. */
    static RigPose kickFollow() {
        return key(p -> {
            p.lean = 0.25F; p.headX = -0.15F; p.twist = 0.6F; p.spin = -0.25F; p.lunge = 1.0F;
            p.rightLegX = -0.55F; p.rightKnee = 0.9F; p.leftLegX = 0.2F; p.leftKnee = 0.45F;
            p.rightArmX = -0.7F; p.rightArmZ = 0.45F; p.rightElbow = 0.8F;
            p.leftArmX = -0.8F; p.leftArmZ = -0.5F; p.leftElbow = 0.7F;
        });
    }

    /** The blade drawn out, turning away to her right, the right knee chambered up. */
    static RigPose kickChamber() {
        return key(p -> {
            p.lean = 0.1F; p.headX = -0.1F; p.twist = 0.3F; p.spin = -0.6F; p.side = -0.15F;
            p.rightLegX = -1.25F; p.rightLegZ = 0.35F; p.rightKnee = 1.7F; p.leftLegX = -0.15F; p.leftKnee = 0.35F;
            p.rightArmX = -0.5F; p.rightArmZ = 0.9F; p.rightElbow = 1.0F;
            p.leftArmX = -0.5F; p.leftArmZ = -0.9F; p.leftElbow = 1.0F;
        });
    }

    /** Spinning: her back round to it, the leg swinging out. */
    static RigPose kickSpin() {
        return key(p -> {
            p.lean = 0.05F; p.twist = 0.1F; p.spin = -2.6F; p.side = 0.2F; p.headY = 0.6F;
            p.rightLegX = -0.8F; p.rightLegZ = 1.0F; p.rightKnee = 0.9F; p.leftLegX = -0.1F; p.leftKnee = 0.3F;
            p.rightArmX = -0.3F; p.rightArmZ = 1.15F; p.rightElbow = 0.7F;
            p.leftArmX = -0.3F; p.leftArmZ = -1.15F; p.leftElbow = 0.7F;
        });
    }

    /** The heel: her right side round to it, the leg straight out level, leaning away from it, her look on it over her shoulder. */
    static RigPose kickHeel() {
        return key(p -> {
            p.lean = 0.05F; p.spin = (float)(-1.5 * Math.PI); p.side = 0.55F; p.headY = 1.0F;
            p.rightLegX = -0.15F; p.rightLegZ = 2.0F; p.rightKnee = 0.05F; p.leftLegX = -0.05F; p.leftLegZ = -0.18F; p.leftKnee = 0.25F;
            p.rightArmX = -0.2F; p.rightArmZ = 1.2F; p.rightElbow = 0.6F;
            p.leftArmX = -0.2F; p.leftArmZ = -1.0F; p.leftElbow = 0.6F;
        });
    }

    /** On round, the leg coming down. */
    static RigPose kickThrough() {
        return key(p -> {
            p.lean = 0.1F; p.spin = -5.6F; p.side = 0.25F; p.headY = 0.4F;
            p.rightLegX = -0.3F; p.rightLegZ = 0.9F; p.rightKnee = 0.6F; p.leftLegX = -0.1F; p.leftKnee = 0.4F;
            p.rightArmX = -0.2F; p.rightArmZ = 0.9F; p.rightElbow = 0.6F;
            p.leftArmX = -0.2F; p.leftArmZ = -0.9F; p.leftElbow = 0.6F;
        });
    }

    /** Down again, the whole turn round, facing it, sunk under the Owl. */
    static RigPose kickLand() {
        return key(p -> {
            p.lean = 0.2F; p.headX = -0.05F; p.spin = (float)(-2.0 * Math.PI);
            p.rightLegX = -0.3F; p.rightKnee = 0.55F; p.leftLegX = -0.2F; p.leftKnee = 0.5F; p.rightLegZ = 0.12F; p.leftLegZ = -0.12F;
            p.rightArmX = -0.1F; p.rightArmZ = 0.3F; p.rightElbow = 0.35F;
            p.leftArmX = -0.1F; p.leftArmZ = -0.3F; p.leftElbow = 0.35F;
        });
    }

    /** Her Owl stance, the whole turn round (a full turn is none: blended out of, it is just her stance). */
    static RigPose kickEnd() {
        RigPose p = stance();
        p.spin = (float)(-2.0 * Math.PI);
        return p;
    }

    /** The kick's keys after the dash (ticks after it). */
    static EtoMotions.Key[] kickKeys() {
        return new EtoMotions.Key[] {EtoMotions.Key.START,
            new EtoMotions.Key(1.5F, kickSkewer(), EtoMotions.Ease.IN),
            new EtoMotions.Key(3.6F, kickWind(), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)OwlRules.KICK_SLASH, kickSlash(), EtoMotions.Ease.IN),
            new EtoMotions.Key(6.2F, kickFollow(), EtoMotions.Ease.OUT),
            new EtoMotions.Key(7.6F, kickChamber(), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key(8.9F, kickSpin(), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)OwlRules.KICK_BLOW, kickHeel(), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key(11.6F, kickThrough(), EtoMotions.Ease.OUT),
            new EtoMotions.Key(13.6F, kickLand(), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)OwlRules.KICK_TICKS, kickEnd(), EtoMotions.Ease.SMOOTH)};
    }

    /**
     * The Owl's kick {@code age} ticks in, the dash {@code dash} ticks, out of {@code from}: low into the dash; then planted behind the
     * skewer, wound up to her left and unwound into the slash, the blade drawn out as she turns away, the spinning heel all the way
     * round (three quarters of a turn into it, on round to face it), down and back into the Owl's stance.
     */
    static RigPose kick(double age, int dash, RigPose from) {
        int ticks = Math.max(1, dash);
        RigPose start = start(from);
        EtoMotions.Key[] rush = {EtoMotions.Key.START, new EtoMotions.Key((float)Math.min(1.2, ticks * 0.6), kickRush(), EtoMotions.Ease.SMOOTH)};
        RigPose pose;
        if (age < ticks) {
            pose = EtoMotions.sample(rush, (float)age, start);
            if (age < rush[1].at()) {
                EtoMotions.overhead(pose, start, rush[1].pose(), (float)(age / rush[1].at()));
            }
        } else {
            pose = EtoMotions.sample(kickKeys(), (float)(age - ticks), EtoMotions.sample(rush, ticks, start));
        }
        EtoMotions.grounded(pose);
        return pose;
    }

    // ------------------------------------------------------------------ its Rend (M1)

    /** Her stance between the cuts: low, wide, weight forward, her claws up. */
    static RigPose rendGuard() {
        return key(p -> {
            p.lean = 0.22F; p.headX = -0.1F;
            p.rightLegX = -0.35F; p.rightKnee = 0.7F; p.rightLegZ = 0.12F; p.leftLegX = 0.1F; p.leftKnee = 0.55F; p.leftLegZ = -0.12F;
            p.rightArmX = -0.7F; p.rightArmZ = 0.35F; p.rightElbow = 1.1F;
            p.leftArmX = -0.7F; p.leftArmZ = -0.35F; p.leftElbow = 1.1F;
        });
    }

    /** The keys of cut {@code stage}: the wind-up, the cut (accelerating into the impact tick), the follow-through, back to her stance. */
    static EtoMotions.Key[] rendKeys(int stage) {
        RigPose guard = rendGuard();
        return switch (stage) {
            case 0 -> new EtoMotions.Key[] {EtoMotions.Key.START,
                new EtoMotions.Key(2.0F, key(p -> {
                    // Wound up to her right, the right side reared back.
                    p.lean = 0.15F; p.twist = 0.55F; p.spin = -0.15F; p.headX = -0.15F;
                    p.rightLegX = -0.35F; p.rightKnee = 0.7F; p.rightLegZ = 0.12F; p.leftLegX = 0.2F; p.leftKnee = 0.5F; p.leftLegZ = -0.12F;
                    p.rightArmX = -1.5F; p.rightArmZ = 0.65F; p.rightElbow = 0.9F; p.leftArmX = -0.6F; p.leftArmZ = -0.4F; p.leftElbow = 1.0F;
                }), EtoMotions.Ease.SMOOTH),
                new EtoMotions.Key((float)OwlRules.REND_IMPACT[0], key(p -> {
                    // Down across her: unwound to her left, lunging into it.
                    p.lean = 0.38F; p.twist = -0.45F; p.spin = 0.15F; p.lunge = 1.5F;
                    p.rightLegX = -0.6F; p.rightKnee = 1.0F; p.rightLegZ = 0.12F; p.leftLegX = 0.4F; p.leftKnee = 0.4F; p.leftLegZ = -0.12F;
                    p.rightArmX = -0.45F; p.rightArmZ = 0.3F; p.rightElbow = 0.35F; p.leftArmX = 0.2F; p.leftArmZ = -0.3F; p.leftElbow = 1.2F;
                }), EtoMotions.Ease.IN),
                new EtoMotions.Key(6.0F, key(p -> {
                    p.lean = 0.32F; p.twist = -0.6F; p.spin = 0.2F; p.lunge = 1.0F;
                    p.rightLegX = -0.55F; p.rightKnee = 0.95F; p.rightLegZ = 0.12F; p.leftLegX = 0.35F; p.leftKnee = 0.45F; p.leftLegZ = -0.12F;
                    p.rightArmX = -0.3F; p.rightArmZ = 0.3F; p.rightElbow = 0.5F; p.leftArmX = 0.25F; p.leftArmZ = -0.3F; p.leftElbow = 1.2F;
                }), EtoMotions.Ease.OUT),
                new EtoMotions.Key((float)OwlRules.REND_TICKS[0], guard, EtoMotions.Ease.SMOOTH)};
            case 1 -> new EtoMotions.Key[] {EtoMotions.Key.START,
                new EtoMotions.Key(2.0F, key(p -> {
                    // Wound up to her left, the left side drawn back.
                    p.lean = 0.2F; p.twist = -0.65F; p.spin = 0.15F; p.headX = -0.12F;
                    p.rightLegX = -0.3F; p.rightKnee = 0.75F; p.rightLegZ = 0.12F; p.leftLegX = 0.15F; p.leftKnee = 0.5F; p.leftLegZ = -0.12F;
                    p.rightArmX = -0.8F; p.rightArmZ = 0.4F; p.rightElbow = 1.1F; p.leftArmX = 0.4F; p.leftArmZ = -0.7F; p.leftElbow = 0.8F;
                }), EtoMotions.Ease.SMOOTH),
                new EtoMotions.Key((float)OwlRules.REND_IMPACT[1], key(p -> {
                    // Across her from the left: unwound hard to her right.
                    p.lean = 0.3F; p.twist = 0.5F; p.spin = -0.15F; p.lunge = 1.3F;
                    p.rightLegX = -0.55F; p.rightKnee = 0.95F; p.rightLegZ = 0.12F; p.leftLegX = 0.35F; p.leftKnee = 0.45F; p.leftLegZ = -0.12F;
                    p.rightArmX = -0.5F; p.rightArmZ = 0.35F; p.rightElbow = 1.1F; p.leftArmX = -1.2F; p.leftArmZ = -0.45F; p.leftElbow = 0.4F;
                }), EtoMotions.Ease.IN),
                new EtoMotions.Key(6.0F, key(p -> {
                    p.lean = 0.28F; p.twist = 0.65F; p.spin = -0.2F; p.lunge = 0.9F;
                    p.rightLegX = -0.5F; p.rightKnee = 0.9F; p.rightLegZ = 0.12F; p.leftLegX = 0.3F; p.leftKnee = 0.45F; p.leftLegZ = -0.12F;
                    p.rightArmX = -0.45F; p.rightArmZ = 0.4F; p.rightElbow = 1.1F; p.leftArmX = -0.9F; p.leftArmZ = -0.55F; p.leftElbow = 0.5F;
                }), EtoMotions.Ease.OUT),
                new EtoMotions.Key((float)OwlRules.REND_TICKS[1], guard, EtoMotions.Ease.SMOOTH)};
            case 2 -> new EtoMotions.Key[] {EtoMotions.Key.START,
                new EtoMotions.Key(2.5F, key(p -> {
                    // Opened right up: her chest out, both arms flung wide.
                    p.lean = 0.08F; p.headX = -0.2F;
                    p.rightLegX = -0.3F; p.rightKnee = 0.6F; p.rightLegZ = 0.15F; p.leftLegX = 0.1F; p.leftKnee = 0.5F; p.leftLegZ = -0.15F;
                    p.rightArmX = -0.6F; p.rightArmZ = 1.1F; p.rightElbow = 0.4F; p.leftArmX = -0.6F; p.leftArmZ = -1.1F; p.leftElbow = 0.4F;
                }), EtoMotions.Ease.SMOOTH),
                new EtoMotions.Key((float)OwlRules.REND_IMPACT[2], key(p -> {
                    // Closed: thrown forward into the scissor, her arms swept in.
                    p.lean = 0.36F; p.headX = -0.05F; p.lunge = 1.5F;
                    p.rightLegX = -0.6F; p.rightKnee = 1.0F; p.rightLegZ = 0.12F; p.leftLegX = 0.35F; p.leftKnee = 0.45F; p.leftLegZ = -0.12F;
                    p.rightArmX = -1.0F; p.rightArmZ = 0.3F; p.rightElbow = 0.7F; p.leftArmX = -1.0F; p.leftArmZ = -0.3F; p.leftElbow = 0.7F;
                }), EtoMotions.Ease.IN),
                new EtoMotions.Key(6.6F, key(p -> {
                    p.lean = 0.3F; p.lunge = 1.0F;
                    p.rightLegX = -0.55F; p.rightKnee = 0.95F; p.rightLegZ = 0.12F; p.leftLegX = 0.3F; p.leftKnee = 0.45F; p.leftLegZ = -0.12F;
                    p.rightArmX = -0.8F; p.rightArmZ = 0.35F; p.rightElbow = 0.9F; p.leftArmX = -0.8F; p.leftArmZ = -0.35F; p.leftElbow = 0.9F;
                }), EtoMotions.Ease.OUT),
                new EtoMotions.Key((float)OwlRules.REND_TICKS[2], guard, EtoMotions.Ease.SMOOTH)};
            default -> new EtoMotions.Key[] {EtoMotions.Key.START,
                new EtoMotions.Key(4.0F, key(p -> {
                    // Up on her toes, risen tall and straight, both arms raised over her head with the blades.
                    p.lean = 0.04F; p.headX = -0.08F;
                    p.rightLegX = -0.15F; p.rightKnee = 0.25F; p.rightLegZ = 0.12F; p.leftLegX = 0.05F; p.leftKnee = 0.2F; p.leftLegZ = -0.12F;
                    p.rightArmX = -2.6F; p.rightArmZ = 0.3F; p.rightElbow = 0.5F; p.leftArmX = -2.6F; p.leftArmZ = -0.3F; p.leftElbow = 0.5F;
                }), EtoMotions.Ease.SMOOTH),
                new EtoMotions.Key((float)OwlRules.REND_IMPACT[3], key(p -> {
                    // Brought down with all her weight, sunk deep behind it.
                    p.lean = 0.72F; p.headX = 0.2F; p.lunge = 2.0F;
                    p.rightLegX = -0.65F; p.rightKnee = 1.25F; p.rightLegZ = 0.16F; p.leftLegX = 0.2F; p.leftKnee = 1.1F; p.leftLegZ = -0.16F;
                    p.rightArmX = -0.75F; p.rightArmZ = 0.3F; p.rightElbow = 0.25F; p.leftArmX = -0.75F; p.leftArmZ = -0.3F; p.leftElbow = 0.25F;
                }), EtoMotions.Ease.IN),
                new EtoMotions.Key(10.0F, key(p -> {
                    p.lean = 0.66F; p.headX = 0.12F; p.lunge = 1.8F;
                    p.rightLegX = -0.62F; p.rightKnee = 1.2F; p.rightLegZ = 0.16F; p.leftLegX = 0.2F; p.leftKnee = 1.05F; p.leftLegZ = -0.16F;
                    p.rightArmX = -0.7F; p.rightArmZ = 0.3F; p.rightElbow = 0.3F; p.leftArmX = -0.7F; p.leftArmZ = -0.3F; p.leftElbow = 0.3F;
                }), EtoMotions.Ease.SMOOTH),
                new EtoMotions.Key((float)OwlRules.REND_TICKS[3], stance(), EtoMotions.Ease.SMOOTH)};
        };
    }

    /** Cut {@code stage} of the Rend {@code age} ticks in, out of {@code from} (the last cut's follow-through, as drawn). */
    static RigPose rend(double age, int stage, RigPose from) {
        int at = Math.max(0, Math.min(OwlRules.REND_STAGES - 1, stage));
        EtoMotions.Key[] keys = rendKeys(at);
        RigPose start = start(from);
        RigPose pose = EtoMotions.sample(keys, (float)age, start);
        if (age < keys[1].at()) {
            EtoMotions.overhead(pose, start, keys[1].pose(), (float)(age / keys[1].at()));
        }
        EtoMotions.grounded(pose);
        return pose;
    }

    /** A giant arm held up at her side between the cuts, the blade low and ready. */
    static final OwlPose.Swing REND_GUARD = OwlPose.carry(1.14, 0.47, OwlPose.REST_ELBOW[0], OwlPose.REST_ELBOW[1], 0.24, 1.2);

    /** The giant arms' keys of cut {@code stage}, {@code side}'s. */
    static ArmKey[] rendArm(int stage, int side) {
        boolean right = side > 0;
        return switch (stage) {
            case 0 -> right ? new ArmKey[] {ArmKey.START,
                new ArmKey(2.0F, new OwlPose.Swing(0.79, 3.51, 0.55, 2.86, 0.3), EtoMotions.Ease.SMOOTH),
                new ArmKey(3.9F, new OwlPose.Swing(0.04, 1.62, -0.58, 1.39, 0.3), EtoMotions.Ease.IN),
                new ArmKey(5.6F, new OwlPose.Swing(-0.57, 1.29, -1.06, 0.63, 0.28), EtoMotions.Ease.OUT),
                new ArmKey((float)OwlRules.REND_TICKS[0], REND_GUARD, EtoMotions.Ease.SMOOTH)}
                : new ArmKey[] {ArmKey.START, new ArmKey(3.0F, REND_GUARD, EtoMotions.Ease.SMOOTH)};
            case 1 -> right ? new ArmKey[] {ArmKey.START, new ArmKey(3.0F, REND_GUARD, EtoMotions.Ease.SMOOTH)}
                : new ArmKey[] {ArmKey.START,
                    new ArmKey(1.8F, new OwlPose.Swing(1.21, -0.18, 1.09, 1.28, 0.26), EtoMotions.Ease.SMOOTH),
                    new ArmKey(3.9F, new OwlPose.Swing(0.1, 1.93, -0.4, 1.69, 0.3), EtoMotions.Ease.IN),
                    new ArmKey(5.5F, new OwlPose.Swing(-0.66, 1.98, -1.04, 1.35, 0.3), EtoMotions.Ease.OUT),
                    new ArmKey((float)OwlRules.REND_TICKS[1], REND_GUARD, EtoMotions.Ease.SMOOTH)};
            case 2 -> new ArmKey[] {ArmKey.START,
                new ArmKey(2.5F, new OwlPose.Swing(1.48, 1.21, 0.94, 1.71, 0.3), EtoMotions.Ease.SMOOTH),
                new ArmKey(4.9F, right ? new OwlPose.Swing(-0.4, 1.92, -0.85, 1.51, 0.3) : new OwlPose.Swing(-0.33, 1.69, -0.89, 1.25, 0.3), EtoMotions.Ease.IN),
                new ArmKey(6.5F, right ? new OwlPose.Swing(-0.67, 1.93, -1.06, 1.28, 0.28) : new OwlPose.Swing(-0.67, 1.71, -1.06, 1.07, 0.28), EtoMotions.Ease.OUT),
                new ArmKey((float)OwlRules.REND_TICKS[2], REND_GUARD, EtoMotions.Ease.SMOOTH)};
            default -> new ArmKey[] {ArmKey.START,
                new ArmKey(4.0F, new OwlPose.Swing(0.37, 3.83, 0.22, 3.27, 0.34), EtoMotions.Ease.SMOOTH),
                new ArmKey(6.8F, new OwlPose.Swing(0.29, 1.01, -0.34, 1.08, 0.26), EtoMotions.Ease.IN),
                new ArmKey(10.0F, new OwlPose.Swing(0.3, 0.98, -0.33, 1.07, 0.24), EtoMotions.Ease.SMOOTH),
                new ArmKey((float)OwlRules.REND_TICKS[3], OwlPose.REST, EtoMotions.Ease.SMOOTH)};
        };
    }

    // ------------------------------------------------------------------ its Fissure (V held)

    /** Reared up for the Fissure, {@code charge} of the way: up on her toes, arched back, both arms up over her head with the blades. */
    static RigPose fissureRear(float charge) {
        float c = charge;
        // Risen tall and straight (never arched back: the Owl's crest stands up off her back only on a body held upright or hunched),
        // looking ahead, the height all in her legs and her arms.
        return key(p -> {
            p.lean = 0.06F - 0.04F * c; p.headX = -0.06F;
            p.rightLegX = -0.1F; p.rightKnee = 0.25F - 0.1F * c; p.rightLegZ = 0.15F; p.leftLegX = 0.05F; p.leftKnee = 0.2F - 0.08F * c; p.leftLegZ = -0.15F;
            p.rightArmX = -2.5F - 0.3F * c; p.rightArmZ = 0.28F; p.rightElbow = 0.6F; p.leftArmX = -2.5F - 0.3F * c; p.leftArmZ = -0.28F; p.leftElbow = 0.6F;
        });
    }

    /** V held {@code age} ticks in the Owl, out of {@code from}: rearing up as it charges, trembling once it is nearly full. */
    static RigPose fissureCharge(double age, RigPose from) {
        float charge = EtoMotions.smooth01((float)(age / EtoRules.HAMMER_GROW_TICKS));
        RigPose pose = fissureRear(charge);
        float tremble = EtoMotions.smooth01((float)((age - EtoRules.HAMMER_GROW_TICKS * 0.6) / (EtoRules.HAMMER_GROW_TICKS * 0.4)));
        if (tremble > 0.0F) {
            float a = 0.035F * tremble;
            pose.lean += a * (float)Math.sin(age * 7.9);
            pose.rightArmX += a * (float)Math.sin(age * 6.7 + 0.5);
            pose.leftArmX += a * (float)Math.sin(age * 7.1 + 1.9);
        }
        if (age < PUNCH_INTO) {
            RigPose start = start(from);
            float u = EtoMotions.smooth01((float)(age / PUNCH_INTO));
            RigPose blended = EtoMotions.copy(start).scale(1.0F - u);
            blended.add(pose, u);
            EtoMotions.overhead(blended, start, fissureRear(0.0F), u);
            pose = blended;
        }
        EtoMotions.grounded(pose);
        return pose;
    }

    /** Let go {@code age} ticks ago, out of {@code from} (reared as drawn): the last of the wind-up, everything brought down, held, up again. */
    static RigPose fissure(double age, RigPose from) {
        EtoMotions.Key[] keys = {EtoMotions.Key.START,
            new EtoMotions.Key(2.0F, fissureRear(1.0F), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)OwlRules.FISSURE_IMPACT, key(p -> {
                p.lean = 0.78F; p.headX = 0.2F; p.lunge = 2.2F;
                p.rightLegX = -0.7F; p.rightKnee = 1.3F; p.rightLegZ = 0.18F; p.leftLegX = 0.2F; p.leftKnee = 1.15F; p.leftLegZ = -0.18F;
                p.rightArmX = -0.8F; p.rightArmZ = 0.28F; p.rightElbow = 0.2F; p.leftArmX = -0.8F; p.leftArmZ = -0.28F; p.leftElbow = 0.2F;
            }), EtoMotions.Ease.IN),
            new EtoMotions.Key(12.0F, key(p -> {
                p.lean = 0.72F; p.headX = 0.1F; p.lunge = 2.0F;
                p.rightLegX = -0.66F; p.rightKnee = 1.25F; p.rightLegZ = 0.18F; p.leftLegX = 0.2F; p.leftKnee = 1.1F; p.leftLegZ = -0.18F;
                p.rightArmX = -0.75F; p.rightArmZ = 0.28F; p.rightElbow = 0.25F; p.leftArmX = -0.75F; p.leftArmZ = -0.28F; p.leftElbow = 0.25F;
            }), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)OwlRules.FISSURE_TICKS, stance(), EtoMotions.Ease.SMOOTH)};
        RigPose start = from == null ? fissureRear(1.0F) : start(from);
        RigPose pose = EtoMotions.sample(keys, (float)age, start);
        EtoMotions.grounded(pose);
        return pose;
    }

    /** How far (radians) the blades are bent off the upper arms at the elbow as she rears with them for the Fissure. */
    static final double FISSURE_BENT = 0.66;

    /** Both giant arms reared up over her as she rears (the blades hooked forward over her head), trembling with the charge. */
    static OwlPose.Swing fissureCrossed(int side, double age) {
        float charge = EtoMotions.smooth01((float)(age / EtoRules.HAMMER_GROW_TICKS));
        // Reared up and back like great wings over her, the blades carried up on them, hooked forward over her head.
        OwlPose.Swing crossed = OwlPose.carry(0.3 + 0.08 * charge, 3.45 + 0.12 * charge, FISSURE_BENT, OwlPose.REST_ELBOW[1], 0.34, 2.8);
        float tremble = EtoMotions.smooth01((float)((age - EtoRules.HAMMER_GROW_TICKS * 0.6) / (EtoRules.HAMMER_GROW_TICKS * 0.4)));
        if (tremble > 0.0F) {
            // The muscle shakes with it; the blade, carried on it, shakes with it.
            double a = 0.03 * tremble;
            double out = a * Math.sin(age * 6.3 + side);
            double forward = a * Math.sin(age * 7.3 + side * 2.0);
            double[] elbow = OwlPose.elbow(crossed);
            crossed = OwlPose.carry(crossed.out() + out, crossed.forward() + forward, elbow[0], elbow[1], crossed.bow(), crossed.bladeForward());
        }
        return crossed;
    }

    /** Let go: the arms drawn back the last bit, then both brought down to one place on the ground ahead, held there, back to rest. */
    static ArmKey[] fissureArm(int side) {
        OwlPose.Swing pulled = OwlPose.carry(0.28, 3.75, FISSURE_BENT + 0.05, OwlPose.REST_ELBOW[1], 0.36, 3.0);
        OwlPose.Swing down = new OwlPose.Swing(0.03, 1.07, -0.61, 1.01, 0.26);
        return new ArmKey[] {ArmKey.START,
            new ArmKey(2.0F, pulled, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)OwlRules.FISSURE_IMPACT - 0.2F, down, EtoMotions.Ease.IN),
            new ArmKey(12.0F, down, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)OwlRules.FISSURE_TICKS, OwlPose.REST, EtoMotions.Ease.SMOOTH)};
    }

    // ------------------------------------------------------------------ its Pounce (M2)

    /** The pounce until she lands: sunk down to spring, the bound itself stretched out, then tucked in the air with the blades up. */
    static EtoMotions.Key[] pounceKeys() {
        return new EtoMotions.Key[] {EtoMotions.Key.START,
            new EtoMotions.Key((float)OwlRules.LEAP_CROUCH, key(p -> {
                p.lean = 0.5F; p.headX = -0.3F;
                p.rightLegX = -0.75F; p.rightKnee = 1.5F; p.rightLegZ = 0.15F; p.leftLegX = -0.6F; p.leftKnee = 1.45F; p.leftLegZ = -0.15F;
                p.rightArmX = 0.6F; p.rightArmZ = 0.25F; p.rightElbow = 0.5F; p.leftArmX = 0.6F; p.leftArmZ = -0.25F; p.leftElbow = 0.5F;
            }), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)(OwlRules.LEAP_CROUCH + 1.5), key(p -> {
                p.lean = 0.2F; p.headX = -0.2F;
                p.rightLegX = 0.3F; p.rightKnee = 0.2F; p.rightLegZ = 0.08F; p.leftLegX = 0.5F; p.leftKnee = 0.3F; p.leftLegZ = -0.08F;
                p.rightArmX = -2.2F; p.rightArmZ = 0.35F; p.rightElbow = 0.4F; p.leftArmX = -2.2F; p.leftArmZ = -0.35F; p.leftElbow = 0.4F;
            }), EtoMotions.Ease.OUT),
            new EtoMotions.Key((float)(OwlRules.LEAP_CROUCH + 5.0), key(p -> {
                p.lean = 0.1F; p.headX = -0.15F;
                p.rightLegX = -1.2F; p.rightKnee = 1.6F; p.rightLegZ = 0.12F; p.leftLegX = -0.9F; p.leftKnee = 1.4F; p.leftLegZ = -0.12F;
                p.rightArmX = -2.4F; p.rightArmZ = 0.35F; p.rightElbow = 0.45F; p.leftArmX = -2.4F; p.leftArmZ = -0.35F; p.leftElbow = 0.45F;
            }), EtoMotions.Ease.SMOOTH)};
    }

    /** The slam as she lands (ticks after touchdown): brought down with both blades, sunk deep behind them; up again into the Owl's stance. */
    static EtoMotions.Key[] slamKeys() {
        return new EtoMotions.Key[] {EtoMotions.Key.START,
            new EtoMotions.Key((float)OwlRules.LEAP_SLAM_AFTER, key(p -> {
                p.lean = 0.75F; p.headX = 0.2F; p.lunge = 1.6F;
                p.rightLegX = -0.65F; p.rightKnee = 1.35F; p.rightLegZ = 0.16F; p.leftLegX = 0.2F; p.leftKnee = 1.2F; p.leftLegZ = -0.16F;
                p.rightArmX = -0.75F; p.rightArmZ = 0.3F; p.rightElbow = 0.25F; p.leftArmX = -0.75F; p.leftArmZ = -0.3F; p.leftElbow = 0.25F;
            }), EtoMotions.Ease.IN),
            new EtoMotions.Key(5.0F, key(p -> {
                p.lean = 0.7F; p.headX = 0.12F; p.lunge = 1.5F;
                p.rightLegX = -0.62F; p.rightKnee = 1.3F; p.rightLegZ = 0.16F; p.leftLegX = 0.2F; p.leftKnee = 1.15F; p.leftLegZ = -0.16F;
                p.rightArmX = -0.7F; p.rightArmZ = 0.3F; p.rightElbow = 0.3F; p.leftArmX = -0.7F; p.leftArmZ = -0.3F; p.leftElbow = 0.3F;
            }), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)OwlRules.LEAP_RECOVER, stance(), EtoMotions.Ease.SMOOTH)};
    }

    /**
     * The pounce {@code age} ticks in, landed at {@code landed} ticks (0: still in the air), out of {@code from}: crouched, the bound,
     * tucked in the air; from the landing, the slam out of however she came down.
     */
    static RigPose pounce(double age, float landed, RigPose from) {
        RigPose start = start(from);
        EtoMotions.Key[] keys = pounceKeys();
        boolean down = landed > 0.0F && age >= landed;
        double air = down ? landed : age;
        RigPose pose = EtoMotions.sample(keys, (float)air, start);
        if (air < keys[1].at()) {
            EtoMotions.overhead(pose, start, keys[1].pose(), (float)(air / keys[1].at()));
        }
        if (down) {
            pose = EtoMotions.sample(slamKeys(), (float)(age - landed), pose);
        }
        EtoMotions.grounded(pose);
        return pose;
    }

    /** The pounce's giant arms: reared up behind her, held up over her through the air, driven down as she lands, back to rest. */
    static OwlPose.Swing pounceArm(double t, float landed) {
        OwlPose.Swing reared = new OwlPose.Swing(0.42, 3.88, 0.3, 3.29, 0.32);
        OwlPose.Swing over = new OwlPose.Swing(0.36, 3.35, 0.04, 2.81, 0.34);
        OwlPose.Swing chop = new OwlPose.Swing(0.29, 1.01, -0.34, 1.08, 0.26);
        ArmKey[] air = {ArmKey.START, new ArmKey((float)OwlRules.LEAP_CROUCH, reared, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)(OwlRules.LEAP_CROUCH + 4.0), over, EtoMotions.Ease.SMOOTH)};
        if (!(landed > 0.0F) || t < landed) {
            return sample(air, t, OwlPose.REST);
        }
        OwlPose.Swing atLanding = sample(air, landed, OwlPose.REST);
        ArmKey[] slam = {ArmKey.START, new ArmKey((float)OwlRules.LEAP_SLAM_AFTER - 0.2F, chop, EtoMotions.Ease.IN), new ArmKey(5.0F, chop, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)OwlRules.LEAP_RECOVER, OwlPose.REST, EtoMotions.Ease.SMOOTH)};
        return sample(slam, t - landed, atLanding);
    }

    // ------------------------------------------------------------------ its Reaping Whirl (J tapped)

    /**
     * The whirl {@code age} ticks in, out of {@code from}: coiled up to her right, crouched; then round a whole turn to her left
     * ({@link OwlRules#whirlTurn}, exactly as the server reaps with it), her arms flung out with the blades, her upper body leading
     * into it and dragged after; on round a hair, and back into her stance.
     */
    static RigPose whirl(double age, RigPose from) {
        EtoMotions.Key[] keys = {EtoMotions.Key.START,
            new EtoMotions.Key((float)OwlRules.WHIRL_WIND, key(p -> {
                p.lean = 0.3F; p.twist = 0.75F; p.headX = -0.1F;
                p.rightLegX = -0.4F; p.rightKnee = 1.0F; p.rightLegZ = 0.18F; p.leftLegX = 0.15F; p.leftKnee = 0.9F; p.leftLegZ = -0.18F;
                p.rightArmX = 0.45F; p.rightArmZ = 0.7F; p.rightElbow = 0.4F; p.leftArmX = -1.0F; p.leftArmZ = -0.25F; p.leftElbow = 0.9F;
            }), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)(OwlRules.WHIRL_START + 2.0), key(p -> {
                p.lean = 0.2F; p.twist = -0.1F;
                p.rightLegX = -0.25F; p.rightKnee = 0.65F; p.rightLegZ = 0.15F; p.leftLegX = -0.05F; p.leftKnee = 0.6F; p.leftLegZ = -0.15F;
                p.rightArmX = -0.3F; p.rightArmZ = 1.35F; p.rightElbow = 0.25F; p.leftArmX = -0.3F; p.leftArmZ = -1.35F; p.leftElbow = 0.25F;
            }), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)OwlRules.WHIRL_END, key(p -> {
                p.lean = 0.18F; p.twist = -0.3F;
                p.rightLegX = -0.25F; p.rightKnee = 0.6F; p.rightLegZ = 0.15F; p.leftLegX = -0.05F; p.leftKnee = 0.6F; p.leftLegZ = -0.15F;
                p.rightArmX = -0.3F; p.rightArmZ = 1.3F; p.rightElbow = 0.3F; p.leftArmX = -0.3F; p.leftArmZ = -1.3F; p.leftElbow = 0.3F;
            }), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)(OwlRules.WHIRL_END + 2.0), key(p -> {
                p.lean = 0.2F; p.twist = -0.4F;
                p.rightLegX = -0.3F; p.rightKnee = 0.65F; p.rightLegZ = 0.15F; p.leftLegX = -0.1F; p.leftKnee = 0.6F; p.leftLegZ = -0.15F;
                p.rightArmX = -0.4F; p.rightArmZ = 0.9F; p.rightElbow = 0.6F; p.leftArmX = -0.4F; p.leftArmZ = -0.9F; p.leftElbow = 0.6F;
            }), EtoMotions.Ease.OUT),
            new EtoMotions.Key((float)OwlRules.WHIRL_TICKS, stance(), EtoMotions.Ease.SMOOTH)};
        RigPose start = start(from);
        RigPose pose = EtoMotions.sample(keys, (float)age, start);
        if (age < keys[1].at()) {
            EtoMotions.overhead(pose, start, keys[1].pose(), (float)(age / keys[1].at()));
        }
        pose.spin += (float)OwlRules.whirlTurn(age);
        EtoMotions.grounded(pose);
        return pose;
    }

    /** The whirl's giant arms: swept back to her right in the coil (the left across), then both out wide at full reach, the blades carried out on them. */
    static ArmKey[] whirlArm(int side) {
        boolean right = side > 0;
        OwlPose.Swing coiled = right ? new OwlPose.Swing(0.77, -0.71, 1.18, 0.01, 0.26) : new OwlPose.Swing(0.64, 1.44, 0.08, 1.45, 0.26);
        OwlPose.Swing out = right ? new OwlPose.Swing(1.49, 1.38, 0.93, 1.72, 0.32) : new OwlPose.Swing(1.49, 1.39, 0.94, 1.76, 0.32);
        return new ArmKey[] {ArmKey.START,
            new ArmKey((float)OwlRules.WHIRL_WIND, coiled, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)(OwlRules.WHIRL_START + 2.0), out, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)OwlRules.WHIRL_END, out, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)OwlRules.WHIRL_TICKS, OwlPose.REST, EtoMotions.Ease.SMOOTH)};
    }

    // ------------------------------------------------------------------ its bone spikes (J held)

    /** Hunched over before they burst: head and shoulders down, crouched, her arms drawn in tense under her. */
    static RigPose spikesHunch() {
        return key(p -> {
            p.lean = 0.9F; p.headX = 0.42F;
            p.rightLegX = -0.55F; p.rightKnee = 1.15F; p.rightLegZ = 0.18F; p.leftLegX = -0.55F; p.leftKnee = 1.15F; p.leftLegZ = -0.18F;
            p.rightArmX = -0.45F; p.rightArmZ = 0.3F; p.rightElbow = 1.0F;
            p.leftArmX = -0.45F; p.leftArmZ = -0.3F; p.leftElbow = 1.0F;
        });
    }

    /** As they burst: thrown up a little by them, her arms flung out and down, the claws spread. */
    static RigPose spikesBurst() {
        return key(p -> {
            p.lean = 0.74F; p.headX = 0.12F;
            p.rightLegX = -0.5F; p.rightKnee = 1.0F; p.rightLegZ = 0.2F; p.leftLegX = -0.5F; p.leftKnee = 1.0F; p.leftLegZ = -0.2F;
            p.rightArmX = -0.15F; p.rightArmZ = 0.95F; p.rightElbow = 0.35F;
            p.leftArmX = -0.15F; p.leftArmZ = -0.95F; p.leftElbow = 0.35F;
        });
    }

    /** Held out: hunched under them, her arms still out. */
    static RigPose spikesHeld() {
        return key(p -> {
            p.lean = 0.8F; p.headX = 0.25F;
            p.rightLegX = -0.52F; p.rightKnee = 1.08F; p.rightLegZ = 0.18F; p.leftLegX = -0.52F; p.leftKnee = 1.08F; p.leftLegZ = -0.18F;
            p.rightArmX = -0.3F; p.rightArmZ = 0.75F; p.rightElbow = 0.55F;
            p.leftArmX = -0.3F; p.leftArmZ = -0.75F; p.leftElbow = 0.55F;
        });
    }

    /** Ticks her body takes to straighten up back into the Owl's stance as they draw in. */
    static final double SPIKES_RISE = OwlSpikes.RETRACT + 2.0;

    /**
     * The bone spikes {@code age} ticks in (their seed {@code seed}; let go at {@code letGo} ticks, 0 while held), out of {@code from}:
     * hunched over, jolted by every spike as it tears out of her (a shudder the way it went), held under them breathing hard and
     * trembling; let go, straightening back up into the Owl's stance as they draw in.
     */
    static RigPose spikes(double age, int seed, float letGo, RigPose from) {
        OwlSpikes.Spike[] spikes = OwlSpikes.of(seed);
        double allOut = OwlSpikes.allOut(spikes);
        EtoMotions.Key[] keys = {EtoMotions.Key.START,
            new EtoMotions.Key((float)(OwlRules.SPIKES_HUNCH - 0.5), spikesHunch(), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)(OwlSpikes.FIRST + 1.2), spikesBurst(), EtoMotions.Ease.OUT),
            new EtoMotions.Key((float)(allOut + 1.0), spikesHeld(), EtoMotions.Ease.SMOOTH)};
        double held = letGo > 0.0F ? Math.min(age, letGo) : age;
        RigPose pose = EtoMotions.sample(keys, (float)held, start(from));
        if (held < keys[1].at()) {
            EtoMotions.overhead(pose, start(from), keys[1].pose(), (float)(held / keys[1].at()));
        }
        // Every spike tearing out of her jolts her the way it went.
        for (OwlSpikes.Spike spike : spikes) {
            double since = held - spike.erupt();
            if (since > 0.0 && since < 2.5) {
                double jolt = since / 0.35 * Math.exp(1.0 - since / 0.35);
                pose.lean -= (float)(0.05 * jolt * (0.5 + 0.5 * spike.dir()[1]));
                pose.side -= (float)(0.035 * jolt * spike.dir()[0]);
                pose.twist += (float)(0.03 * jolt * spike.dir()[0] * spike.dir()[2]);
                pose.headX -= (float)(0.03 * jolt);
            }
        }
        if (held > allOut) {
            // Breathing hard under them, trembling with the strain.
            double strain = OwlPose.smooth((held - allOut) / 3.0);
            pose.lean += (float)(strain * (0.03 * Math.sin(held * 0.35) + 0.008 * Math.sin(held * 5.3)));
            pose.rightArmZ += (float)(strain * 0.02 * Math.sin(held * 4.7));
            pose.leftArmZ -= (float)(strain * 0.02 * Math.sin(held * 5.1 + 1.0));
        }
        if (letGo > 0.0F && age > letGo) {
            // Straightening back up as they go back in.
            float u = EtoMotions.smooth01((float)((age - letGo) / SPIKES_RISE));
            RigPose risen = EtoMotions.copy(pose).scale(1.0F - u);
            risen.add(stance(), u);
            pose = risen;
        }
        EtoMotions.grounded(pose);
        return pose;
    }

    // ------------------------------------------------------------------ its Watch This (B): the lances

    /**
     * Her body as the lances burst out of her ({@link com.hex.kagune.control.eto.OwlLances}): hunched over, gathering herself, then
     * reared up tall as they tear out of her back (jolted by it), chest out, her own arms flung out wide, braced; held a moment,
     * then back into the Owl's stance while they hunt on.
     */
    static RigPose watch(double age, RigPose from) {
        EtoMotions.Key[] keys = {EtoMotions.Key.START,
            new EtoMotions.Key(4.0F, key(p -> {
                p.lean = 0.62F; p.headX = 0.3F;
                p.rightLegX = -0.45F; p.rightKnee = 0.9F; p.rightLegZ = 0.16F; p.leftLegX = -0.45F; p.leftKnee = 0.9F; p.leftLegZ = -0.16F;
                p.rightArmX = -0.4F; p.rightArmZ = 0.22F; p.rightElbow = 0.9F; p.leftArmX = -0.4F; p.leftArmZ = -0.22F; p.leftElbow = 0.9F;
            }), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)(OwlRules.LANCES_ERUPT_AT + 1.5), watchReared(), EtoMotions.Ease.OUT),
            new EtoMotions.Key(15.0F, key(p -> {
                p.lean = 0.14F; p.headX = -0.04F;
                p.rightLegX = -0.12F; p.rightKnee = 0.3F; p.rightLegZ = 0.2F; p.leftLegX = -0.08F; p.leftKnee = 0.28F; p.leftLegZ = -0.2F;
                p.rightArmX = -0.1F; p.rightArmZ = 0.85F; p.rightElbow = 0.3F; p.leftArmX = -0.1F; p.leftArmZ = -0.85F; p.leftElbow = 0.3F;
            }), EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key((float)OwlRules.LANCES_POSE_TICKS, stance(), EtoMotions.Ease.SMOOTH)};
        RigPose start = start(from);
        RigPose pose = EtoMotions.sample(keys, (float)age, start);
        // The lances tearing out of her back jolt her forward and down, a beat.
        double since = age - OwlRules.LANCES_ERUPT_AT;
        if (since > 0.0 && since < 3.0) {
            double jolt = since / 0.4 * Math.exp(1.0 - since / 0.4);
            pose.lean += (float)(0.09 * jolt);
            pose.headX += (float)(0.05 * jolt);
        }
        EtoMotions.grounded(pose);
        return pose;
    }

    /** Reared up tall as they tear out of her: chest out, chin up (never thrown back), her own arms flung out wide, braced wide. */
    static RigPose watchReared() {
        return key(p -> {
            p.lean = 0.04F; p.headX = -0.14F;
            p.rightLegX = -0.15F; p.rightKnee = 0.35F; p.rightLegZ = 0.24F; p.leftLegX = -0.1F; p.leftKnee = 0.32F; p.leftLegZ = -0.24F;
            p.rightArmX = 0.05F; p.rightArmZ = 1.2F; p.rightElbow = 0.2F; p.leftArmX = 0.05F; p.leftArmZ = -1.2F; p.leftElbow = 0.2F;
        });
    }

    /**
     * The giant arms through it: drawn in close as she hunches, then flung out wide and up as the lances burst out (wings spread over
     * her, the blades carried on them), held, and back to rest.
     */
    static ArmKey[] watchArm() {
        double bent = OwlPose.REST_ELBOW[0];
        double way = OwlPose.REST_ELBOW[1];
        return new ArmKey[] {ArmKey.START,
            new ArmKey(4.0F, OwlPose.carry(0.72, 0.62, bent - 0.08, way, 0.24, 1.2), EtoMotions.Ease.SMOOTH),
            new ArmKey((float)(OwlRules.LANCES_ERUPT_AT + 1.5), OwlPose.carry(1.22, 2.15, bent - 0.12, way, 0.3, 2.4), EtoMotions.Ease.OUT),
            new ArmKey(15.0F, OwlPose.carry(1.12, 1.85, bent - 0.06, way, 0.28, 2.2), EtoMotions.Ease.SMOOTH),
            new ArmKey((float)OwlRules.LANCES_POSE_TICKS, OwlPose.REST, EtoMotions.Ease.SMOOTH)};
    }

    // ------------------------------------------------------------------ its blade volley (N)

    /**
     * Her body through the volley ({@link OwlShards}): braced wide, leaning into it, the torso wound round after each arm as it rears
     * back and uncoiled the other way as it whips through, every throw in turn; back into the Owl's stance after the last.
     */
    static RigPose volley(double age, boolean held, float letGo, RigPose from) {
        float end = OwlShards.ends(held, letGo);
        float braced = Math.max(3.0F, end - OwlShards.RECOVER + 2.0F);
        RigPose brace = key(p -> {
            p.lean = 0.22F; p.headX = -0.04F;
            p.rightLegX = -0.32F; p.rightKnee = 0.55F; p.rightLegZ = 0.2F; p.leftLegX = 0.12F; p.leftKnee = 0.4F; p.leftLegZ = -0.2F;
            p.rightArmX = -0.35F; p.rightArmZ = 0.45F; p.rightElbow = 0.6F; p.leftArmX = -0.35F; p.leftArmZ = -0.45F; p.leftElbow = 0.6F;
        });
        EtoMotions.Key[] keys = {EtoMotions.Key.START, new EtoMotions.Key(3.0F, brace, EtoMotions.Ease.SMOOTH),
            new EtoMotions.Key(braced, brace, EtoMotions.Ease.SMOOTH), new EtoMotions.Key(Math.max(braced + 0.5F, end), stance(), EtoMotions.Ease.SMOOTH)};
        // (6.2.4: the spikes fly off the creature's crest, OwlActs: her body under it only braced, never twisting after them.)
        RigPose pose = EtoMotions.sample(keys, (float)age, start(from));
        EtoMotions.grounded(pose);
        return pose;
    }

    /** 6.2.4: through its great hand's moves her body is only the Owl's stance (the creature acts, OwlActs): eased into it. */
    static RigPose grab(double age, RigPose from) {
        EtoMotions.Key[] keys = {EtoMotions.Key.START, new EtoMotions.Key(4.0F, stance(), EtoMotions.Ease.SMOOTH)};
        RigPose pose = EtoMotions.sample(keys, (float)age, start(from));
        EtoMotions.grounded(pose);
        return pose;
    }

    /** A giant arm through the volley: raised ready; rearing back over its shoulder; whipped through as it lets go; on through; ready. */
    static final OwlPose.Swing VOLLEY_READY = OwlPose.carry(0.9, 1.9, OwlPose.REST_ELBOW[0] - 0.1, OwlPose.REST_ELBOW[1], 0.26, 2.2);
    static final OwlPose.Swing VOLLEY_WOUND = OwlPose.carry(0.55, 3.45, OwlPose.REST_ELBOW[0] - 0.05, OwlPose.REST_ELBOW[1], 0.3, 3.0);
    static final OwlPose.Swing VOLLEY_RELEASE = OwlPose.carry(0.3, 1.9, OwlPose.REST_ELBOW[0] - 0.15, OwlPose.REST_ELBOW[1], 0.3, 2.2);
    static final OwlPose.Swing VOLLEY_THROUGH = OwlPose.carry(0.08, 1.05, OwlPose.REST_ELBOW[0] - 0.1, OwlPose.REST_ELBOW[1], 0.28, 1.4);

    /** {@code side}'s giant arm {@code t} ticks into the volley: its throw under way, else raised ready. */
    static OwlPose.Swing volleyArm(int side, double t, boolean held, float letGo) {
        int k = OwlShards.throwOf(side, t, held, letGo);
        if (k < 0) {
            return VOLLEY_READY;
        }
        float at = OwlShards.throwAt(k);
        ArmKey[] keys = {new ArmKey((float)(at + OwlShards.WIND_FROM), VOLLEY_READY, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)(at + OwlShards.WOUND), VOLLEY_WOUND, EtoMotions.Ease.SMOOTH),
            new ArmKey(at, VOLLEY_RELEASE, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)(at + OwlShards.THROUGH), VOLLEY_THROUGH, EtoMotions.Ease.OUT),
            new ArmKey((float)(at + OwlShards.READY), VOLLEY_READY, EtoMotions.Ease.SMOOTH)};
        return sample(keys, t, VOLLEY_READY);
    }

    // ------------------------------------------------------------------ the giant arms

    /*
     * The giant arms' keys are laid out in the frame of the way she faces (OwlPose#toTorso): out to her side and forward of where
     * she faces, standing straight up; what is drawn is the same directions off her torso as it is posed, so they point where she
     * aims whatever her body does under them, and ride her twists and lunges.
     */

    /** The right giant arm reared up behind her as the charge begins, and full: the upper arm up and back, the blade curled up over it. */
    static final OwlPose.Swing PUNCH_REAR0 = new OwlPose.Swing(0.67, 3.37, 0.26, 2.61, 0.3);
    static final OwlPose.Swing PUNCH_REAR1 = new OwlPose.Swing(0.48, 3.73, 0.23, 2.93, 0.36);
    /** The left giant arm braced: out ahead of her to her left, the blade's point down on the ground. */
    static final OwlPose.Swing PUNCH_BRACE = new OwlPose.Swing(0.86, 0.69, 0.34, 1.12, 0.2);
    /** Let go, the brace lifting off the ground as she drives forward. */
    static final OwlPose.Swing PUNCH_UNBRACE = new OwlPose.Swing(0.96, 0.62, 0.41, 1.19, 0.22);
    /**
     * Let go: drawn back a hair further (the last of the wind-up), then the whole arm driven over and down at what she faces, the
     * elbow leading, the blade carried with it; held out a moment; and back.
     */
    static final OwlPose.Swing PUNCH_PULL = new OwlPose.Swing(0.38, 3.93, 0.22, 3.14, 0.36);
    static final OwlPose.Swing PUNCH_DRIVE = new OwlPose.Swing(-0.14, 2.03, -0.63, 1.74, 0.3);
    static final OwlPose.Swing PUNCH_THROUGH = new OwlPose.Swing(-0.23, 1.69, -0.72, 1.4, 0.24);
    static final OwlPose.Swing PUNCH_HELD = new OwlPose.Swing(0.08, 1.46, -0.47, 1.32, 0.26);
    /** The left giant arm flung out wide and back against the blow like a wing (as a fighter's other arm), its point kept up off the ground. */
    static final OwlPose.Swing PUNCH_BACK = new OwlPose.Swing(0.74, -0.85, 1.28, -0.28, 0.26);
    /** On its way there, swept out wide to her side (the point arcing round high, never down through the ground under her). */
    static final OwlPose.Swing PUNCH_FLING = new OwlPose.Swing(1.05, -0.28, 1.04, 0.89, 0.26);
    static final OwlPose.Swing PUNCH_SETTLE = new OwlPose.Swing(1.03, 0.1, 0.69, 1.08, 0.26);

    /** Both giant arms held out through the dash like a hunting bird's claws: out wide, the blades raised forward off the ground. */
    static final OwlPose.Swing KICK_SWEPT = new OwlPose.Swing(0.99, 0.04, 0.76, 0.91, 0.2);
    /**
     * The right one cocked as she plants: the elbow up and back over her shoulder, the blade levelled at it over her head (raised
     * forward and over, never swung down under the arm into the ground); then driven down and through it, held, drawn out.
     */
    static final OwlPose.Swing KICK_COCK = new OwlPose.Swing(0.78, 2.8, 0.21, 2.17, 0.24);
    static final OwlPose.Swing KICK_THRUST = new OwlPose.Swing(0.02, 1.4, -0.52, 1.26, 0.18);
    static final OwlPose.Swing KICK_HOLD = new OwlPose.Swing(0.05, 1.38, -0.5, 1.26, 0.2);
    static final OwlPose.Swing KICK_PULLED = new OwlPose.Swing(0.8, 2.21, 0.27, 2.0, 0.3);
    /** The left one wound back out wide behind her, and the slash across its neck, on through. */
    static final OwlPose.Swing KICK_WIND = new OwlPose.Swing(1.14, -0.18, 1.06, 1.11, 0.26);
    static final OwlPose.Swing KICK_WOUND = new OwlPose.Swing(1.24, -0.27, 1.15, 1.42, 0.3);
    static final OwlPose.Swing KICK_CUT = new OwlPose.Swing(0.12, 2.13, -0.37, 1.86, 0.3);
    static final OwlPose.Swing KICK_ACROSS = new OwlPose.Swing(-0.6, 2.15, -1.0, 1.57, 0.3);
    /**
     * Both drawn up over her as she spins (pulled in, as a skater does to spin fast: the blades standing up, never whipping round at
     * full reach), then flung out wide as the spin slows after the heel, the blades carried out on them.
     */
    static final OwlPose.Swing KICK_WHIRL = new OwlPose.Swing(0.55, 3.31, 0.25, 2.8, 0.3);
    static final OwlPose.Swing KICK_WHIRLED = new OwlPose.Swing(1.47, 1.02, 0.93, 1.57, 0.3);

    static ArmKey[] kickRight(int dash) {
        float d = Math.max(1, dash);
        return new ArmKey[] {ArmKey.START,
            new ArmKey(Math.min(1.2F, d * 0.6F), KICK_SWEPT, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 0.3F, KICK_COCK, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 1.7F, KICK_THRUST, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 6.2F, KICK_HOLD, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 7.0F, KICK_PULLED, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 7.9F, KICK_WHIRL, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 10.4F, KICK_WHIRL, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 12.4F, KICK_WHIRLED, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 15.0F, OwlPose.REST, EtoMotions.Ease.SMOOTH)};
    }

    static ArmKey[] kickLeft(int dash) {
        float d = Math.max(1, dash);
        return new ArmKey[] {ArmKey.START,
            new ArmKey(Math.min(1.2F, d * 0.6F), KICK_SWEPT, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 1.2F, KICK_WIND, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 3.3F, KICK_WOUND, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 4.6F, KICK_CUT, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 5.6F, KICK_ACROSS, EtoMotions.Ease.OUT),
            new ArmKey(d + 7.9F, KICK_WHIRL, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 10.4F, KICK_WHIRL, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 12.4F, KICK_WHIRLED, EtoMotions.Ease.SMOOTH),
            new ArmKey(d + 15.0F, OwlPose.REST, EtoMotions.Ease.SMOOTH)};
    }

    /** Both giant arms braced on the ground at her sides through the spikes (a second pair of legs under her), flaring as they burst. */
    static final OwlPose.Swing SPIKES_BRACE = new OwlPose.Swing(0.96, 0.38, 0.56, 1.04, 0.22);
    static final OwlPose.Swing SPIKES_FLARE = new OwlPose.Swing(1.17, 0.57, 0.73, 1.21, 0.3);
    /** Let go: the blades lifted off the ground as she straightens. */
    static final OwlPose.Swing SPIKES_LIFT = new OwlPose.Swing(1.12, 0.85, 0.63, 1.31, 0.26);

    /** A giant arm through the spikes (unweighted), {@code t} ticks in, let go at {@code letGo}: braced, flaring, braced; back to rest as they go. */
    static OwlPose.Swing spikesArm(double t, int seed, float letGo, int side) {
        double allOut = OwlSpikes.allOut(OwlSpikes.of(seed));
        ArmKey[] keys = {ArmKey.START,
            new ArmKey((float)(OwlRules.SPIKES_HUNCH - 1.0), SPIKES_BRACE, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)(OwlSpikes.FIRST + 1.4), SPIKES_FLARE, EtoMotions.Ease.OUT),
            new ArmKey((float)(allOut + 1.5), SPIKES_BRACE, EtoMotions.Ease.SMOOTH)};
        double held = letGo > 0.0F ? Math.min(t, letGo) : t;
        OwlPose.Swing swing = sample(keys, held, OwlPose.REST);
        if (letGo > 0.0F && t > letGo) {
            // Lifted off the ground first (her shoulders are still low), then down to rest as she straightens up.
            double u = (t - letGo) / SPIKES_RISE;
            // (6.2.4: eased at the elbow, so the muscle lifts the blade and the blade only flexes.)
            swing = u < 0.4 ? blend(swing, SPIKES_LIFT, OwlPose.smooth(u / 0.4), side) : blend(SPIKES_LIFT, stoopedRest(side), OwlPose.smooth((u - 0.4) / 0.6), side);
        }
        return swing;
    }

    /**
     * 6.2.4: her idle's rest ({@link OwlPose#REST}, off her torso) as it is in the frame of the way she faces once she stands in the
     * Owl's stoop: a move ending here hands its arms back to her idle as they are (nothing turned round its own arm on the way).
     */
    static OwlPose.Swing stoopedRest(int side) {
        return OwlPose.fromTorso(stance(), OwlPose.REST, side, 0.0);
    }

    /** Ticks the giant arms take to go from as they were drawn into a move's keys (the kick's dash: no time at all). */
    static final double ARMS_IN = 3.0;
    static final double KICK_ARMS_IN = 1.0;

    /**
     * One giant arm ({@code side} 1 her right, -1 her left) in the Owl's {@code move}, {@code age} ticks in, with its weight
     * ({@link #heavy}), off her torso as {@code body} poses it (its facing {@code turn} radians, + to her right, from her body's:
     * what the move was thrown at), eased in from {@code start} (that arm as it was drawn when the move began). Null when the move
     * doesn't key the giant arms.
     */
    public static OwlPose.Swing swing(EtoMove move, int side, double age, int detail, float charge, OwlPose.Swing start, RigPose body, float turn) {
        Track track = track(move, side, detail, charge);
        if (track == null) {
            return null;
        }
        OwlPose.Swing keyed = heavy(track, age, track.at(0.0));
        // Keyed to the way she faces; for a move that spins her, to her whole body's own turn as well.
        double follow = followsSpin(move, age, detail);
        double frameSpin = -turn + (follow > 0.0 ? follow * body(move, Math.max(0.0, age), detail, charge, null).spin : 0.0);
        double in = move == EtoMove.OWL_KICK ? KICK_ARMS_IN : ARMS_IN;
        if (start != null && age < in) {
            // Out of what was drawn (as it is off her torso now, in the frame of the way she faces, where the keys bend the blade):
            // the upper arm turned the shortest way round to where the move has it, the blade carried on it, its bend eased.
            keyed = blend(OwlPose.fromTorso(body, start, side, frameSpin), keyed, OwlPose.smooth(age / in), side);
        }
        OwlPose.Swing posed = OwlPose.toTorso(body, keyed, side, frameSpin);
        if (move == EtoMove.OWL_KICK && side > 0) {
            // Through it and held there: the blade pivots on what it skewered (its middle, where she aimed) as her body twists round.
            double k = age - Math.max(1, detail);
            double held = OwlPose.smooth((k - KICK_AIM_FROM) / 0.5) * (1.0 - OwlPose.smooth((k - KICK_AIM_UNTIL) / KICK_AIM_OUT));
            if (held > 0.0) {
                double cos = Math.cos(-turn);
                double sin = Math.sin(-turn);
                // The middle of what she kicks, where she planted to skewer it, turned as the move faces.
                double[] middle = {-sin * OwlRules.KICK_STAND, KICK_AIM_HEIGHT, cos * OwlRules.KICK_STAND};
                posed = blend(posed, OwlPose.aimAt(body, side, middle, posed), held, side);
            }
        }
        return carried(posed, side);
    }

    /**
     * {@code swing} with its blade bent off its upper arm at the elbow no less than {@link #ELBOW_LEAST} and no more than
     * {@link #ELBOW_MOST}, the way it was bent; the upper arm, the muscle that swings it, as it was.
     */
    public static OwlPose.Swing carried(OwlPose.Swing swing, int side) {
        double[] elbow = OwlPose.elbow(swing);
        double bent = Math.max(ELBOW_LEAST, Math.min(ELBOW_MOST, elbow[0]));
        if (Math.abs(bent - elbow[0]) < 1.0E-9) {
            return swing;
        }
        return OwlPose.carry(swing.out(), swing.forward(), bent, elbow[1], swing.bow(), swing.bladeForward());
    }

    /**
     * {@code t} of the way from {@code a} to {@code b} ({@code side}'s arm, both off the same torso), each part turned evenly the
     * shortest way round from the one direction to the other (never through some third way, as blending the angles would near
     * an arm pointing straight out to her side), its angles the ones nearest {@code b}'s.
     */
    public static OwlPose.Swing blend(OwlPose.Swing a, OwlPose.Swing b, double t, int side) {
        if (t <= 0.0) {
            return a;
        }
        if (t >= 1.0) {
            return b;
        }
        double[] upper = turn(a.out(), a.forward(), b.out(), b.forward(), t, side);
        // The blade carried on it, its bend at the elbow eased from the one to the other.
        double[] bend = bend(OwlPose.elbow(a), OwlPose.elbow(b), t);
        double near = a.bladeForward() + (b.bladeForward() + Math.PI * 2.0 * Math.round((a.bladeForward() - b.bladeForward()) / (Math.PI * 2.0))
            - a.bladeForward()) * t;
        return OwlPose.carry(upper[0], upper[1], bend[0], bend[1], a.bow() + (b.bow() - a.bow()) * t, near);
    }

    /** {@code t} of the way from one bend at the elbow ({how far, which way}) to another, round the shorter way. */
    static double[] bend(double[] a, double[] b, double t) {
        double way = b[1] - a[1];
        way -= Math.PI * 2.0 * Math.round(way / (Math.PI * 2.0));
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + way * t};
    }

    /** One part's direction {@code t} of the way round from (out, forward) {@code a} to {@code b}, as {out, forward}. */
    static double[] turn(double outA, double forwardA, double outB, double forwardB, double t, int side) {
        double[] da = unit(outA, forwardA, side);
        double[] db = unit(outB, forwardB, side);
        double cos = Math.max(-1.0, Math.min(1.0, da[0] * db[0] + da[1] * db[1] + da[2] * db[2]));
        double angle = Math.acos(cos);
        double[] d;
        if (angle < 1.0E-6) {
            d = db;
        } else if (Math.PI - angle < 1.0E-3) {
            // Straight opposite: round by way of the forward angles' own blend.
            return new double[] {outA + (outB - outA) * t, forwardA + (forwardB - forwardA) * t};
        } else {
            double sa = Math.sin((1.0 - t) * angle) / Math.sin(angle);
            double sb = Math.sin(t * angle) / Math.sin(angle);
            d = new double[] {da[0] * sa + db[0] * sb, da[1] * sa + db[1] * sb, da[2] * sa + db[2] * sb};
        }
        double out = Math.asin(Math.max(-1.0, Math.min(1.0, -side * d[0])));
        double near = forwardA + (forwardB + Math.PI * 2.0 * Math.round((forwardA - forwardB) / (Math.PI * 2.0)) - forwardA) * t;
        double forward = Math.abs(d[1]) < 1.0E-9 && Math.abs(d[2]) < 1.0E-9 ? near : Math.atan2(-d[2], d[1]);
        forward += Math.PI * 2.0 * Math.round((near - forward) / (Math.PI * 2.0));
        return new double[] {out, forward};
    }

    /** A part's direction in its torso's own axes (x to her left, y down, z back), as {@link OwlPose#direction} lays it out. */
    static double[] unit(double out, double forward, int side) {
        return new double[] {-side * Math.sin(out), Math.cos(out) * Math.cos(forward), -Math.cos(out) * Math.sin(forward)};
    }

    /** {@code swing} with each of its angles forward a whole number of turns nearer {@code to}'s: the very same directions. */
    public static OwlPose.Swing nearest(OwlPose.Swing swing, OwlPose.Swing to) {
        double turn = Math.PI * 2.0;
        double upper = swing.forward() + turn * Math.round((to.forward() - swing.forward()) / turn);
        double blade = swing.bladeForward() + turn * Math.round((to.bladeForward() - swing.bladeForward()) / turn);
        return new OwlPose.Swing(swing.out(), upper, swing.bladeOut(), blade, swing.bow());
    }

    /** One giant arm's keyed swing in {@code move} at {@code age}, unweighted, in the frame of the way she faces (null: none keyed). */
    public static OwlPose.Swing keyed(EtoMove move, int side, double age, int detail, float charge) {
        Track track = track(move, side, detail, charge);
        return track == null ? null : track.at(Math.max(0.0, age));
    }

    /** The same with its weight ({@link #heavy}), still in the frame of the way she faces. */
    public static OwlPose.Swing weighted(EtoMove move, int side, double age, int detail, float charge) {
        Track track = track(move, side, detail, charge);
        return track == null ? null : heavy(track, age, track.at(0.0));
    }

    /** The keys of one giant arm through {@code move} (unweighted, in the frame of the way she faces); null when it keys none. */
    static Track track(EtoMove move, int side, int detail, float charge) {
        return switch (move) {
            case OWL_PUNCH_CHARGE -> t -> punchChargeArm(side, t);
            case OWL_PUNCH -> {
                ArmKey[] keys = side > 0 ? punchRight() : punchLeft();
                OwlPose.Swing first = punchChargeArm(side, EtoRules.PUNCH_CHARGE_TICKS * Math.max(0.15, charge));
                yield t -> sample(keys, t, first);
            }
            case OWL_KICK -> {
                ArmKey[] keys = side > 0 ? kickRight(detail) : kickLeft(detail);
                yield t -> sample(keys, t, OwlPose.REST);
            }
            case OWL_SPIKES -> t -> spikesArm(t, detail, charge, side);
            case OWL_STRIKE -> {
                ArmKey[] keys = rendArm(Math.max(0, Math.min(OwlRules.REND_STAGES - 1, detail)), side);
                yield t -> sample(keys, t, REND_GUARD);
            }
            case OWL_WHIRL -> {
                ArmKey[] keys = whirlArm(side);
                yield t -> sample(keys, t, OwlPose.REST);
            }
            case OWL_LEAP -> t -> pounceArm(t, charge);
            case OWL_WATCH -> {
                ArmKey[] keys = watchArm();
                yield t -> sample(keys, t, OwlPose.REST);
            }
            case OWL_VOLLEY -> t -> volleyArm(side, t, detail == 1, charge);
            case OWL_FISSURE_CHARGE -> t -> fissureCrossed(side, t);
            case OWL_FISSURE -> {
                ArmKey[] keys = fissureArm(side);
                yield t -> sample(keys, t, fissureCrossed(side, EtoRules.HAMMER_GROW_TICKS));
            }
            default -> null;
        };
    }

    /** A giant arm through the charge, {@code age} ticks in: reared and braced as the charge builds; nearly full, trembling with it. */
    static OwlPose.Swing punchChargeArm(int side, double age) {
        float charge = EtoMotions.smooth01((float)(age / EtoRules.PUNCH_CHARGE_TICKS));
        OwlPose.Swing held = side > 0 ? lerp(PUNCH_REAR0, PUNCH_REAR1, charge) : PUNCH_BRACE;
        float tremble = EtoMotions.smooth01((float)((age - EtoRules.PUNCH_CHARGE_TICKS * 0.6) / (EtoRules.PUNCH_CHARGE_TICKS * 0.4)));
        if (side > 0 && tremble > 0.0F) {
            // The muscle shakes with it; the blade, carried on it, shakes with it.
            double a = 0.035 * tremble;
            double out = a * Math.sin(age * 6.1);
            double forward = a * Math.sin(age * 7.7 + 0.9);
            double[] elbow = OwlPose.elbow(held);
            held = OwlPose.carry(held.out() + out, held.forward() + forward, elbow[0], elbow[1], held.bow(), held.bladeForward());
        }
        return held;
    }

    static ArmKey[] punchRight() {
        return new ArmKey[] {ArmKey.START,
            new ArmKey(1.2F, PUNCH_PULL, EtoMotions.Ease.SMOOTH),
            new ArmKey(4.0F, PUNCH_DRIVE, EtoMotions.Ease.IN),
            new ArmKey(4.8F, PUNCH_THROUGH, EtoMotions.Ease.OUT),
            new ArmKey(9.0F, PUNCH_HELD, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)EtoRules.PUNCH_TICKS, OwlPose.REST, EtoMotions.Ease.SMOOTH)};
    }

    static ArmKey[] punchLeft() {
        return new ArmKey[] {ArmKey.START,
            new ArmKey(2.0F, PUNCH_UNBRACE, EtoMotions.Ease.SMOOTH),
            new ArmKey(3.1F, PUNCH_FLING, EtoMotions.Ease.SMOOTH),
            new ArmKey(4.4F, PUNCH_BACK, EtoMotions.Ease.SMOOTH),
            new ArmKey(10.0F, PUNCH_SETTLE, EtoMotions.Ease.SMOOTH),
            new ArmKey((float)EtoRules.PUNCH_TICKS, OwlPose.REST, EtoMotions.Ease.SMOOTH)};
    }

    // ------------------------------------------------------------------ weight

    /** A giant arm's keyed swing at a moment ({@code t} ticks into the move; never asked before 0). */
    interface Track {
        OwlPose.Swing at(double t);
    }

    /** How many past moments the weight looks back over, and how far apart (in lags). */
    static final int WEIGHT_SAMPLES = 10;
    static final double WEIGHT_STEP = 0.4;

    /**
     * The arm keyed by {@code track}, {@code age} ticks in, with its weight: the upper arm follows where it is driven a moment late
     * (as a heavy thing does: the latest of it most, what came before less and less) by {@link #UPPER_LAG}, and the blade, carried
     * on it, settles into its bend at the elbow a moment later still ({@link #BLADE_LAG}); before the move began, it was where it
     * started ({@code from}). The blade bows with its own speed.
     */
    static OwlPose.Swing heavy(Track track, double age, OwlPose.Swing from) {
        OwlPose.Swing now = age <= 0.0 ? from : track.at(age);
        OwlPose.Swing drawn = followed(track, age, from, now.bladeForward());
        OwlPose.Swing before = followed(track, age - 0.5, from, now.bladeForward());
        double[] blade = OwlPose.along(drawn.bladeOut(), drawn.bladeForward());
        double[] was = OwlPose.along(before.bladeOut(), before.bladeForward());
        double speed = Math.acos(Math.max(-1.0, Math.min(1.0, blade[0] * was[0] + blade[1] * was[1] + blade[2] * was[2]))) / 0.5;
        double bow = now.bow() + Math.min(BOW_MOST, BOW_PER_SPEED * speed);
        return new OwlPose.Swing(drawn.out(), drawn.forward(), drawn.bladeOut(), drawn.bladeForward(), bow);
    }

    /** The arm keyed by {@code track} as its weight draws it at {@code age}: the upper arm a moment late, the blade carried on it. */
    static OwlPose.Swing followed(Track track, double age, OwlPose.Swing from, double near) {
        double[] upper = follow(track, age, from, UPPER_LAG, true);
        double[] bend = follow(track, age, from, BLADE_LAG, false);
        return OwlPose.carry(upper[0], upper[1], bend[0], bend[1], from.bow(), near);
    }

    /**
     * One part ({@code upperArm}: the upper arm's {out, forward}, else the blade's bend off it at the elbow, {how far, which way})
     * following {@code track} {@code lag} ticks behind, at {@code age}.
     */
    static double[] follow(Track track, double age, OwlPose.Swing from, double lag, boolean upperArm) {
        double a = 0.0;
        double b = 0.0;
        double total = 0.0;
        double step = lag * WEIGHT_STEP;
        double first = Double.NaN;
        for (int i = 0; i < WEIGHT_SAMPLES; i++) {
            double back = i * step;
            double w = Math.exp(-back / lag);
            double t = age - back;
            OwlPose.Swing s = t <= 0.0 ? from : track.at(t);
            if (upperArm) {
                a += w * s.out();
                b += w * s.forward();
            } else {
                double[] elbow = OwlPose.elbow(s);
                if (Double.isNaN(first)) {
                    first = elbow[1];
                }
                a += w * elbow[0];
                // Which way, the nearest round to the latest (never averaged across the half turn).
                b += w * (elbow[1] + Math.PI * 2.0 * Math.round((first - elbow[1]) / (Math.PI * 2.0)));
            }
            total += w;
        }
        return new double[] {a / total, b / total};
    }

    /** The swing {@code t} ticks into {@code keys}, the first (null) key being {@code start}. */
    static OwlPose.Swing sample(ArmKey[] keys, double t, OwlPose.Swing start) {
        OwlPose.Swing previous = start;
        float previousAt = keys[0].at();
        for (int i = 1; i < keys.length; i++) {
            ArmKey key = keys[i];
            if (t <= key.at()) {
                float span = key.at() - previousAt;
                float u = span <= 0.0F ? 1.0F : (float)Math.max(0.0, (t - previousAt) / span);
                return lerp(previous, key.swing(), key.ease().apply(Math.min(1.0F, u)));
            }
            previous = key.swing();
            previousAt = key.at();
        }
        return previous;
    }

    /** {@code t} of the way from {@code a} to {@code b}: the upper arm swung between them, the blade carried on it, its bend eased. */
    static OwlPose.Swing lerp(OwlPose.Swing a, OwlPose.Swing b, double t) {
        double[] bend = bend(OwlPose.elbow(a), OwlPose.elbow(b), t);
        return OwlPose.carry(a.out() + (b.out() - a.out()) * t, a.forward() + (b.forward() - a.forward()) * t, bend[0], bend[1],
            a.bow() + (b.bow() - a.bow()) * t, a.bladeForward() + (b.bladeForward() - a.bladeForward()) * t);
    }
}
