package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.AgentRules;

/**
 * 4.1.38 (the owner: "after the fight ends, I should note, let's say 2 minutes without fighting, Arima puts back his jacket in his normal";
 * asked how, they chose it reversed, where he stands): the White Reaper putting his jacket back on (pure), {@code t} ticks in
 * ({@link AgentRules#REAPER_DRESS_TICKS}). First the blade goes away ({@link AgentRules#REAPER_STOW_TICKS}): his left hand goes behind his
 * back and comes out with the golden case ({@link #CASE_IN}), out at his side ({@link #OUT}) while his right raises the blade before him;
 * the left brings the case to the blade, opening it on the way, and sets it at his right fist ({@link #BRING}); the blade slides back into it
 * ({@link #IN}), the case shuts ({@link #SHUT}), and from there it is his draw played backwards: the case swung down to hang from his right
 * hand and put away behind his back. Then the coat ({@link AgentRules#REAPER_DON_TICKS}): his arms sweep back for it ({@link #LEAD}) and it
 * comes back up his arms and onto his shoulders, his taking it off played backwards (ReaperCoatPose), to his hands on his lapels and rest.
 */
public final class ReaperDressPose {
    private ReaperDressPose() {
    }

    /** The left hand behind his back; the case in it; out at his side with it; the blade raised before him by. */
    public static final double REACH = 6.0;
    public static final double CASE_IN = 5.0;
    public static final double OUT = 12.0;
    public static final double RAISED = 14.0;
    /** The case at his right fist, open about the blade; the blade back in it; the case shut. */
    public static final double BRING = 19.0;
    public static final double IN = 26.0;
    public static final double SHUT = 30.0;
    /** The draw's tick the rest of the blade's going away plays backwards from (the end of its lift). */
    static final double FROM_DRAW = ReaperBladePose.LIFTED;
    /** Ticks of the coat's part his arms take to sweep back for it before it comes onto him. */
    public static final double LEAD = 5.0;

    /**
     * His left arm for the case (as the draw's right, mirrored): tick, twist (+ to his right), lean, the left arm's x (- forward), y, z (- out),
     * the head's turn and pitch.
     */
    static final float[][] LEFT_KEYS = {
        {0, 0.0F, 0.0F, 0.06F, 0.0F, -0.08F, 0.0F, 0.0F},
        {6, -0.32F, 0.08F, 0.72F, 0.0F, 0.42F, 0.25F, 0.05F},
        {8, -0.34F, 0.08F, 0.74F, 0.0F, 0.44F, 0.28F, 0.05F},
        {12, 0.08F, 0.0F, -0.2F, 0.0F, -0.62F, -0.15F, 0.0F}};

    /** His pose {@code t} ticks into putting it back on. */
    public static RigPose pose(double t) {
        if (t >= AgentRules.REAPER_STOW_TICKS) {
            return don(t - AgentRules.REAPER_STOW_TICKS);
        }
        if (t >= SHUT) {
            return ReaperBladePose.draw(drawTick(t));
        }
        // His right hand: the blade from his guard up before him (both hands' grip, as it left the case), by RAISED.
        float lower = 1.0F - ReaperBladePose.smooth((t - REACH) / (RAISED - REACH));
        RigPose p = ReaperBladePose.presented(lower);
        RigPose two = ReaperBladePose.presented(0.0F);
        float[] k = AgentPose.keyed(LEFT_KEYS, Math.min(t, OUT));
        float carry = ReaperBladePose.smooth((t - OUT) / (BRING - OUT));
        float off = ReaperBladePose.smooth((t - IN) / (SHUT - IN));
        p.twist += k[1] * (1.0F - carry);
        p.lean += k[2] * (1.0F - carry);
        p.headY += k[6] * (1.0F - carry);
        p.headX += k[7] * (1.0F - carry);
        float[] left;
        if (t < OUT) {
            left = new float[] {k[3], k[4], k[5]};
        } else if (t < IN) {
            left = QuinqueGrip.blendTurn(k[3], k[4], k[5], two.leftArmX, two.leftArmY, two.leftArmZ, carry);
        } else {
            // Off the grip as the case shuts (the draw's own left arm at the end of its lift: loose).
            left = QuinqueGrip.blendTurn(two.leftArmX, two.leftArmY, two.leftArmZ, 0.05F, 0.0F, -0.06F, off);
        }
        p.leftArmX = left[0];
        p.leftArmY = left[1];
        p.leftArmZ = left[2];
        p.leftArmHold = 1.0F;
        p.rightArmHold = 1.0F;
        p.armHold = 1.0F;
        p.headHold = Math.max(p.headHold, 0.5F);
        return p;
    }

    /** The draw's tick that {@code t} (from {@link #SHUT}) plays backwards: its lift's end down to its start. */
    public static double drawTick(double t) {
        return Math.max(0.0, FROM_DRAW * (1.0 - (t - SHUT) / (AgentRules.REAPER_STOW_TICKS - SHUT)));
    }

    /** His coat's part ({@code u} ticks of it): his arms swept back for the coat, then his taking it off backwards. */
    static RigPose don(double u) {
        double age = coatAge(u + AgentRules.REAPER_STOW_TICKS);
        if (age >= 0.0) {
            return AgentPose.shed(age);
        }
        float w = ReaperBladePose.smooth(u / LEAD);
        return new RigPose().add(ReaperBladePose.draw(0.0), 1.0F - w).add(AgentPose.shed(ReaperCoatPose.OFF), w);
    }

    /**
     * The tick of his taking the coat off that {@code t} shows backwards (the coat drawn as it was then: ReaperCoatPose), or -1 while the coat
     * is not on him yet.
     */
    public static double coatAge(double t) {
        double u = t - AgentRules.REAPER_STOW_TICKS - LEAD;
        if (u < 0.0) {
            return -1.0;
        }
        double span = AgentRules.REAPER_DON_TICKS - LEAD;
        return Math.max(0.0, ReaperCoatPose.OFF * (1.0 - u / span));
    }

    /** How far the blade is out of its case {@code t} ticks in (1 all out), or -1 once it is in it (not drawn). */
    public static float bladeOut(double t) {
        if (t >= IN) {
            return -1.0F;
        }
        return 1.0F - ReaperBladePose.smooth((t - BRING) / (IN - BRING));
    }

    /** Where the case is {@code t} ticks in, in his right arm's frame: its handle's place, its turn, how far its halves are open. */
    public record Held(double[] origin, double[][] turn, double open, boolean handle) {
    }

    /** The case {@code t} ticks in ({@code p}: his pose then), or null while it is not in either hand. */
    public static Held held(RigPose p, double t) {
        if (t < CASE_IN || t >= AgentRules.REAPER_STOW_TICKS) {
            return null;
        }
        if (t >= SHUT) {
            double d = drawTick(t);
            return ReaperBladePose.caseHeld(d) ? new Held(ReaperBladePose.FIST_RIGHT, ReaperBladePose.caseTurn(p, d), 0.0, true) : null;
        }
        double[][] along = ReaperBladePose.caseTurn(p, ReaperBladePose.LIFTED);
        if (t >= BRING) {
            double open = ReaperBladePose.CASE_OPEN * (1.0 - ReaperBladePose.smooth((t - IN) / (SHUT - IN)));
            return new Held(ReaperBladePose.FIST_RIGHT, along, open, false);
        }
        // In his left hand, hanging plumb from it, then carried to his right fist (turned end on along the blade, opened on the way).
        double out = com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
        double[] rs = {-5.0 - out, 2.0, 0.0};
        double[] ls = {5.0 + out, 2.0, 0.0};
        double[][] armR = RigPose.zyx(p.rightArmX, p.rightArmY, p.rightArmZ);
        double[][] armL = RigPose.zyx(p.leftArmX, p.leftArmY, p.leftArmZ);
        double[] inLeft = ReaperBladePose.add(ls, QuinqueGrip.apply(armL, ReaperBladePose.FIST_LEFT));
        double[] atRight = ReaperBladePose.add(rs, QuinqueGrip.apply(armR, ReaperBladePose.FIST_RIGHT));
        double[][] plumb = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        double[][] onBlade = RigPose.multiply(armR, along);
        double w = t < OUT ? 0.0 : ReaperBladePose.smooth((t - OUT) / (BRING - OUT));
        double[] o = {inLeft[0] + (atRight[0] - inLeft[0]) * w, inLeft[1] + (atRight[1] - inLeft[1]) * w, inLeft[2] + (atRight[2] - inLeft[2]) * w};
        float[] a = QuinqueGrip.angles(plumb);
        float[] b = QuinqueGrip.angles(onBlade);
        float[] m = QuinqueGrip.blendTurn(a[0], a[1], a[2], b[0], b[1], b[2], (float)w);
        double[][] turn = RigPose.multiply(QuinqueGrip.transpose(armR), RigPose.zyx(m[0], m[1], m[2]));
        double[] origin = QuinqueGrip.apply(QuinqueGrip.transpose(armR), ReaperBladePose.sub(o, rs));
        return new Held(origin, turn, ReaperBladePose.CASE_OPEN * w, w < 0.15);
    }
}
