package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.eto.EtoMove;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.eto.EtoStrike;

/**
 * Eto's moves on one player's body (6.1.0), for {@link KaguneBodyAnimation}: while the server says she is striking or flipping,
 * the move's pose ({@link EtoMotions}) takes the body over, starting from exactly the pose drawn the frame before (so a strike
 * thrown out of the last one's extension, out of a walk or out of a gesture never jumps), turned to face the way she faced when
 * it began. When a strike is over she keeps her guard up for a moment, her legs her own again, and then eases back into her
 * stance; a flip eases straight back.
 */
final class EtoAnimator {
    /** Ticks to blend out of a move into the guard, how long the guard is kept, and the blend from it back to her stance. */
    static final float INTO_GUARD = 3.0F;
    static final float GUARD_KEPT = 20.0F;
    static final float OUT_OF_GUARD = 8.0F;
    /** A flip's blend in from the pose before it (ticks) and back out after. */
    static final float FLIP_IN = 1.5F;
    static final float FLIP_OUT = 5.0F;
    /** 6.2.0: ticks her legs take to be her own again once she lands out of an arm move thrown in the air. */
    static final float AIR_LEGS_OUT = 3.0F;

    private EtoMove move = EtoMove.NONE;
    private int detail;
    private long startedAt = Long.MIN_VALUE;
    /** The pose drawn last frame, and when the current move began, the pose it began from (null: none drawn yet). */
    private final RigPose shown = new RigPose();
    private boolean anyShown;
    private RigPose from;
    /** 6.2.0: the pose drawn the frame before last (how the body was turning), and whether there is one. */
    private final RigPose shownBefore = new RigPose();
    private boolean anyShownBefore;
    /** 6.2.0: the move this one cut off, its turns concluded on top of it (null: it began from rest, or chained out of a strike). */
    private EtoMotions.Cut cut;
    /**
     * 6.2.0: an arm move thrown in the air out of a flip: the flip carries on under it in her legs until she lands (null: none); how
     * far into it the flip was when it was cut, and whether it soared.
     */
    private EtoRules.Flip airFlip;
    private double airFlipAge;
    private boolean airSoar;
    /** The current move's age and charge last frame (what a cut-off flip had got to). */
    private double lastSince;
    private float lastCharge;
    /** The layer's age when the current move began (a flip's touchdown must come after it). */
    private float startAge;
    /** The last move pose drawn, what ended, and when (on EtoClient's clock; NaN: nothing to blend out of). */
    private RigPose last;
    private EtoMove ended = EtoMove.NONE;
    private double endedAt = Double.NaN;

    /** The move posed on the body (NONE once it is over, while it is blended out). */
    EtoMove posing() {
        return this.move;
    }

    /** 6.2.0: true while one of the Owl's own moves has her body, or is being blended back out of. */
    boolean owlPosing(double time) {
        return this.move.owl() || this.ended.owl() && !Double.isNaN(this.endedAt) && time - this.endedAt < OwlMotions.BODY_OUT;
    }

    /** True while a move or its guard has the body (her idle gestures wait). */
    boolean busy(double time) {
        if (this.move != EtoMove.NONE) {
            return true;
        }
        float out = this.ended == EtoMove.FLIP ? FLIP_OUT : this.ended.owl() ? OwlMotions.BODY_OUT : INTO_GUARD + GUARD_KEPT + OUT_OF_GUARD;
        return !Double.isNaN(this.endedAt) && time - this.endedAt < out;
    }

    /**
     * 6.2.0: true while her arms are a move's (a strike or the Big Punch, its charge included) or are still in the guard after one,
     * until they are down again: in first person they are drawn then, as here.
     */
    boolean arms(double time) {
        if (this.move == EtoMove.STRIKE || this.move == EtoMove.PUNCH_CHARGE || this.move == EtoMove.PUNCH || this.move == EtoMove.OWL_PUNCH_CHARGE
            || this.move == EtoMove.OWL_PUNCH || this.move == EtoMove.OWL_STRIKE) {
            return true;
        }
        if (this.ended == EtoMove.OWL_PUNCH || this.ended == EtoMove.OWL_STRIKE) {
            return !Double.isNaN(this.endedAt) && time - this.endedAt < OwlMotions.BODY_OUT;
        }
        return (this.ended == EtoMove.STRIKE || this.ended == EtoMove.PUNCH) && !Double.isNaN(this.endedAt)
            && time - this.endedAt < INTO_GUARD + GUARD_KEPT + OUT_OF_GUARD;
    }

    /**
     * Puts the move on {@code p} (the stance composed so far): the move's pose replaces it while the move runs; after it the guard
     * and the blend back. {@code time} is EtoClient's clock (never set back), {@code age} the layer's age (ticks), {@code landed} the layer's last touchdown (its age then), {@code turn}
     * how far (radians, + to her right) the way she faced when the move began is from her body's facing now.
     */
    void apply(RigPose p, EtoClient.EtoView view, double time, float age, float landed, float turn) {
        EtoMove now = view.move();
        boolean animated = EtoMotions.animates(now);
        if (EtoMotions.facesLook(now)) {
            turn = 0.0F; // charging, or holding up what she caught, she faces wherever she looks
        }
        if (!animated) {
            if (this.move != EtoMove.NONE) {
                this.ended = this.move;
                this.endedAt = time;
                this.move = EtoMove.NONE;
            }
            this.blendOut(p, time);
            return;
        }
        if (now != this.move || view.startedAt() != this.startedAt || view.detail() != this.detail) {
            EtoMove was = this.move;
            int wasDetail = this.detail;
            float wasStartAge = this.startAge;
            this.move = now;
            this.detail = view.detail();
            this.startedAt = view.startedAt();
            this.startAge = age;
            this.endedAt = Double.NaN;
            this.from = this.anyShown ? EtoMotions.copy(this.shown) : null;
            this.cut = null;
            this.airFlip = null;
            if (this.from != null) {
                // The new move's own facing is put on at the end: take it off what it starts from, so the first frame is the last.
                this.from.spin += turn;
                this.from.headY += turn;
                if (was != EtoMove.NONE && !EtoMotions.continues(was, now)) {
                    // 6.2.0: cut off mid-way (a move carrying on is its own): the whole-body turns concluded on top, the way they went.
                    RigPose before = this.anyShownBefore ? EtoMotions.copy(this.shownBefore) : null;
                    if (before != null) {
                        before.spin += turn;
                    }
                    this.cut = EtoMotions.cut(this.from, before);
                    this.from = this.cut.from();
                }
                if (was == EtoMove.FLIP && EtoMotions.upperBody(now) && !(landed > wasStartAge + 1.0F)) {
                    // An arm move out of a flip in the air: her legs carry on with the flip until she lands.
                    this.airFlip = EtoRules.Flip.byOrdinal(wasDetail);
                    this.airFlipAge = this.lastSince;
                    this.airSoar = this.lastCharge > 0.5F;
                }
            }
        }
        double since = view.age(time);
        RigPose pose;
        if (now == EtoMove.STRIKE) {
            pose = EtoMotions.strike(EtoStrike.byOrdinal(this.detail), since, this.from);
        } else if (now == EtoMove.PUNCH_CHARGE) {
            pose = EtoMotions.punchCharge(since, this.from);
        } else if (now == EtoMove.PUNCH) {
            pose = EtoMotions.punch(since, this.from);
        } else if (now == EtoMove.KICK) {
            pose = EtoMotions.kick(since, view.detail(), this.from);
        } else if (now == EtoMove.WITNESS) {
            pose = EtoMotions.witness(since, this.from);
        } else if (now == EtoMove.HAMMER_CHARGE) {
            pose = EtoMotions.hammerCharge(since, this.from);
        } else if (now == EtoMove.HAMMER) {
            pose = EtoMotions.hammer(since, view.charge(), this.from);
        } else if (now == EtoMove.WATCH) {
            pose = EtoMotions.watch(since, this.from);
        } else if (now.flesh()) {
            pose = EtoMotions.flesh(now, since, this.from);
        } else if (now.owl()) {
            // 6.2.0: the Owl's own moves.
            pose = OwlMotions.body(now, since, view.detail(), view.charge(), this.from);
        } else {
            double touchdown = landed > this.startAge + 1.0F ? age - landed : -1.0;
            // 6.2.0: an Up We Go flip (charge 1) soars: stretched through the ramp, turning over across the longer climb.
            EtoRules.Flip flip = EtoRules.Flip.byOrdinal(this.detail);
            pose = view.charge() > 0.5F ? EtoMotions.soar(flip, since, touchdown) : EtoMotions.flip(flip, since, touchdown);
            if (this.from != null && since < FLIP_IN) {
                float u = smooth((float)(since / FLIP_IN));
                RigPose blended = EtoMotions.copy(this.from).scale(1.0F - u);
                blended.add(pose, u);
                pose = blended;
            }
        }
        if (this.cut != null) {
            double[] left = this.cut.left(since);
            pose.flip += (float)left[0];
            pose.roll += (float)left[1];
            pose.spin += (float)left[2];
            this.cut.unwind(pose, since);
            this.cut.clearOf(pose, since);
        }
        if (this.airFlip != null) {
            // Her legs still in the flip until she lands, then her own again over a few ticks.
            boolean down = landed > this.startAge + 1.0F;
            double flipAge = this.airFlipAge + since;
            double touchdown = down ? age - landed : -1.0;
            RigPose legs = this.airSoar ? EtoMotions.soar(this.airFlip, flipAge, touchdown) : EtoMotions.flip(this.airFlip, flipAge, touchdown);
            float weight = down ? 1.0F - smooth((float)((age - landed) / AIR_LEGS_OUT)) : 1.0F;
            EtoMotions.legsFrom(pose, legs, weight);
        }
        this.lastSince = since;
        this.lastCharge = view.charge();
        // Thrown the way she faced when it began, whatever her body has turned to since.
        pose.spin -= turn;
        pose.headY -= turn;
        this.last = EtoMotions.ended(pose);
        copyInto(pose, p);
    }

    /** After a move: into the guard and out of it again (a strike), or straight back to her stance (a flip). */
    private void blendOut(RigPose p, double time) {
        if (this.last == null || Double.isNaN(this.endedAt)) {
            return;
        }
        float t = (float)(time - this.endedAt);
        if (this.ended == EtoMove.FLIP || this.ended.owl()) {
            // A flip, and (6.2.0) the Owl's moves: straight back into her stance (the Owl's, under it).
            float out = this.ended == EtoMove.FLIP ? FLIP_OUT : OwlMotions.BODY_OUT;
            if (t >= out) {
                this.last = null;
                return;
            }
            mix(p, this.last, 1.0F - smooth(t / out));
            return;
        }
        if (t >= INTO_GUARD + GUARD_KEPT + OUT_OF_GUARD) {
            this.last = null;
            return;
        }
        RigPose guard = guardKept(p);
        RigPose held;
        if (t < INTO_GUARD) {
            float u = smooth(t / INTO_GUARD);
            held = EtoMotions.copy(this.last).scale(1.0F - u);
            held.add(guard, u);
        } else {
            held = guard;
        }
        float out = t < INTO_GUARD + GUARD_KEPT ? 1.0F : 1.0F - smooth((t - INTO_GUARD - GUARD_KEPT) / OUT_OF_GUARD);
        mix(p, held, out);
    }

    /**
     * Her guard kept after a strike: the arms up as between strikes; the legs her own again (she can walk with it), the stance
     * under it what she was doing anyway.
     */
    private static RigPose guardKept(RigPose stance) {
        RigPose guard = EtoMotions.guard();
        guard.legHold = 0.0F;
        guard.rightLegX = stance.rightLegX;
        guard.rightLegY = stance.rightLegY;
        guard.rightLegZ = stance.rightLegZ;
        guard.rightKnee = stance.rightKnee;
        guard.leftLegX = stance.leftLegX;
        guard.leftLegY = stance.leftLegY;
        guard.leftLegZ = stance.leftLegZ;
        guard.leftKnee = stance.leftKnee;
        guard.drop = stance.drop;
        guard.lunge = 0.0F;
        return guard;
    }

    /** The pose as drawn this frame (after everything, settled): the next move starts from it. */
    void shown(RigPose drawn) {
        if (this.anyShown) {
            copyInto(this.shown, this.shownBefore);
            this.anyShownBefore = true;
        }
        copyInto(drawn, this.shown);
        this.anyShown = true;
    }

    /** {@code p} becomes {@code (1 - w) p + w over}. */
    private static void mix(RigPose p, RigPose over, float w) {
        if (w <= 0.0F) {
            return;
        }
        RigPose mixed = EtoMotions.copy(p).scale(1.0F - w);
        mixed.add(over, w);
        copyInto(mixed, p);
    }

    private static void copyInto(RigPose source, RigPose target) {
        target.clear();
        target.add(source, 1.0F);
    }

    private static float smooth(float u) {
        float c = Math.max(0.0F, Math.min(1.0F, u));
        return c * c * (3.0F - 2.0F * c);
    }
}
