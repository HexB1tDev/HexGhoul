package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.animation.WarPose;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.KingRules;
import com.hex.kagune.control.king.PeaceMotion;
import com.hex.kagune.control.king.PeaceStrike;
import com.hex.kagune.control.king.WarCloth;
import com.hex.kagune.control.king.WarModel;
import java.util.ArrayList;
import java.util.List;

/** 1.0.5: the final kakujas: the crown, the choice, the growth, the night, War's body and War's shapes. 1.0.14: War's M1. */
public final class KingTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        crown();
        night();
        pose();
        model();
        abilities();
        strikes();
        combo();
        peace();
        peaceBlades();
        peaceArms();
        cloth();
        coming();
        turning();
        eyes();
        warpath();
        held();
        driven();
        clock();
        if (!FAILURES.isEmpty()) {
            for (String failure : FAILURES) {
                System.err.println("FAILED: " + failure);
            }
            throw new AssertionError(FAILURES.size() + " final kakuja checks failed");
        }
        System.out.println("Final kakuja tests: " + passed + " checks passed");
    }

    /** 1.0.27: the warpath's RMB cone, the blitz, Black Sun: their layouts, their timelines, his body through them. */
    private static void warpath() {
        double wasHalf = 0.0;
        for (int row = 0; row < com.hex.kagune.control.king.WarRules.CONE_ROWS; row++) {
            double half = com.hex.kagune.control.king.WarRules.coneHalf(row);
            int n = com.hex.kagune.control.king.WarRules.coneSpikes(row);
            check(half > wasHalf && n >= 1, "the cone widens row by row (" + row + ")");
            check(com.hex.kagune.control.king.WarRules.coneAlong(row) > (row == 0 ? 0.5 : com.hex.kagune.control.king.WarRules.coneAlong(row - 1)),
                "each row of the cone stands further out (" + row + ")");
            double last = -Double.MAX_VALUE;
            for (int k = 0; k < n; k++) {
                for (double jitter : new double[] {-1.0, 0.0, 1.0}) {
                    double across = com.hex.kagune.control.king.WarRules.coneAcross(row, k, jitter);
                    check(Math.abs(across) <= half + 0.3, "a spike of row " + row + " stays inside its row");
                }
                double mid = com.hex.kagune.control.king.WarRules.coneAcross(row, k, 0.0);
                check(mid > last, "a row's spikes run across it in order (" + row + ")");
                last = mid;
            }
            wasHalf = half;
        }
        check(com.hex.kagune.control.king.WarRules.coneSpikes(0) <= 2 && com.hex.kagune.control.king.WarRules.coneHalf(0) < 0.6,
            "the cone starts small at his hand");
        check(com.hex.kagune.control.king.WarRules.coneHalf(com.hex.kagune.control.king.WarRules.CONE_ROWS - 1) >= 4.0,
            "the cone ends wide");
        // The blitz: the body moved only in small steps a tick (held there, never thrown about), up off the ground and away from him.
        double[] was = com.hex.kagune.control.king.WarRules.blitzBody(0.0);
        for (double t = 0.0; t < com.hex.kagune.control.king.WarRules.BLITZ_GUT; t += 0.25) {
            double[] at = com.hex.kagune.control.king.WarRules.blitzBody(t);
            double step = Math.sqrt(Math.pow(at[0] - was[0], 2) + Math.pow(at[1] - was[1], 2) + Math.pow(at[2] - was[2], 2));
            check(step < 0.75 && Double.isFinite(at[1]), "the blitz carries the body smoothly (" + t + ")");
            was = at;
        }
        check(com.hex.kagune.control.king.WarRules.blitzBody(com.hex.kagune.control.king.WarRules.BLITZ_WIND)[1] > 3.0, "the blitz has it high in the air");
        for (double t = 0.0; t < com.hex.kagune.control.king.WarRules.BLITZ_GUT; t += 0.5) {
            check(com.hex.kagune.control.king.WarRules.blitzBody(t)[1] <= com.hex.kagune.control.king.WarRules.BLITZ_PEAK + 1.0E-9,
                "the blitz never has it higher than the room it looks for over it (" + t + ")");
        }
        check(com.hex.kagune.control.king.WarRules.BLITZ_PUNCH < com.hex.kagune.control.king.WarRules.BLITZ_CUT
            && com.hex.kagune.control.king.WarRules.BLITZ_CUT < com.hex.kagune.control.king.WarRules.BLITZ_KICK
            && com.hex.kagune.control.king.WarRules.BLITZ_KICK < com.hex.kagune.control.king.WarRules.BLITZ_WIND
            && com.hex.kagune.control.king.WarRules.BLITZ_GUT - com.hex.kagune.control.king.WarRules.BLITZ_WIND >= 10,
            "punch, cut, kick, then the last one slowed");
        double rise = -1.0;
        double leap = -1.0;
        for (double t = 0.0; t <= com.hex.kagune.control.king.WarRules.SUNDER_PUNCH; t += 0.5) {
            double r = com.hex.kagune.control.king.WarRules.sunderRise(t);
            double l = com.hex.kagune.control.king.WarRules.sunderLeap(t);
            check(r >= rise && r <= com.hex.kagune.control.king.WarRules.SUNDER_UP && l >= leap && l <= 1.0, "Black Sun rises without falling back (" + t + ")");
            rise = r;
            leap = l;
        }
        check(Math.abs(com.hex.kagune.control.king.WarRules.sunderRise(com.hex.kagune.control.king.WarRules.SUNDER_APEX)
            - com.hex.kagune.control.king.WarRules.SUNDER_UP) < 1.0E-9 && leap == 1.0, "it is at its height and he beside it before the punch");
        // His body: sane and smooth all the way through each, and the beats where they should be.
        com.hex.kagune.control.king.KingMove[] moves = {com.hex.kagune.control.king.KingMove.CONE, com.hex.kagune.control.king.KingMove.RISE,
            com.hex.kagune.control.king.KingMove.BLITZ, com.hex.kagune.control.king.KingMove.SUNDER};
        for (com.hex.kagune.control.king.KingMove move : moves) {
            RigPose before = null;
            double worst = 0.0;
            for (double age = 0.0; age <= move.ticks() + 2.0; age += 0.1) {
                RigPose p = new RigPose();
                WarPose.act(p, move, age, 0, 1.0F);
                check(Float.isFinite(p.rightArmX) && Float.isFinite(p.leftArmX) && Math.abs(p.rightArmX) < 3.5F && Math.abs(p.twist) < 1.6F,
                    move + "'s body is sane at " + age);
                if (before != null) {
                    float[] a = {p.rightArmX, p.rightArmY, p.rightArmZ, p.leftArmX, p.leftArmY, p.leftArmZ, p.twist, p.lean, p.rightLegX, p.leftLegX};
                    float[] b = {before.rightArmX, before.rightArmY, before.rightArmZ, before.leftArmX, before.leftArmY, before.leftArmZ, before.twist,
                        before.lean, before.rightLegX, before.leftLegX};
                    for (int i = 0; i < a.length; i++) {
                        worst = Math.max(worst, Math.abs(a[i] - b[i]));
                    }
                }
                before = p;
            }
            check(worst < 0.6, move + "'s body never snaps (worst " + worst + " a tenth of a tick)");
            RigPose end = new RigPose();
            WarPose.act(end, move, move.ticks() + 1.0, 0, 1.0F);
            check(Math.abs(end.rightArmX) < 0.05F && Math.abs(end.leftArmX) < 0.05F && Math.abs(end.armHold) < 0.05F, move + " lets the body go at its end");
        }
        RigPose punch = new RigPose();
        WarPose.act(punch, com.hex.kagune.control.king.KingMove.BLITZ, com.hex.kagune.control.king.WarRules.BLITZ_PUNCH, 0, 1.0F);
        RigPose wound = new RigPose();
        WarPose.act(wound, com.hex.kagune.control.king.KingMove.BLITZ, com.hex.kagune.control.king.WarRules.BLITZ_GUT - 2.0, 0, 1.0F);
        RigPose gut = new RigPose();
        WarPose.act(gut, com.hex.kagune.control.king.KingMove.BLITZ, com.hex.kagune.control.king.WarRules.BLITZ_GUT, 0, 1.0F);
        RigPose kick = new RigPose();
        WarPose.act(kick, com.hex.kagune.control.king.KingMove.BLITZ, com.hex.kagune.control.king.WarRules.BLITZ_KICK + 0.6, 0, 1.0F);
        check(punch.rightArmX < -1.3F && wound.rightArmX > 0.7F && gut.rightArmX < -1.0F && kick.rightLegX < -1.5F,
            "the blitz: punched out, the kick up, the last one drawn back, then driven in");
        RigPose upper = new RigPose();
        WarPose.act(upper, com.hex.kagune.control.king.KingMove.SUNDER, com.hex.kagune.control.king.WarRules.SUNDER_UPPER, 0, 1.0F);
        RigPose cocked = new RigPose();
        WarPose.act(cocked, com.hex.kagune.control.king.KingMove.SUNDER, com.hex.kagune.control.king.WarRules.SUNDER_PUNCH - 2.0, 0, 1.0F);
        RigPose loose = new RigPose();
        WarPose.act(loose, com.hex.kagune.control.king.KingMove.SUNDER, com.hex.kagune.control.king.WarRules.SUNDER_PUNCH, 0, 1.0F);
        check(upper.rightArmX < -2.3F && cocked.leftArmX > 1.2F && loose.leftArmX < -1.2F, "Black Sun: the uppercut, the (bare left) fist cocked, the punch");
        RigPose swipe = new RigPose();
        WarPose.act(swipe, com.hex.kagune.control.king.KingMove.CONE, com.hex.kagune.control.king.WarRules.CONE_SWIPE + 0.5, 0, 1.0F);
        RigPose raised = new RigPose();
        WarPose.act(raised, com.hex.kagune.control.king.KingMove.RISE, com.hex.kagune.control.king.WarRules.RISE_SPIKES + 2.0, 0, 1.0F);
        check(swipe.leftArmX < -0.8F && swipe.leftArmY > 0.3F && raised.rightArmX < -2.3F && raised.leftArmX < -2.3F,
            "the cone's hand swept out low and wide; both hands raised for the spikes");
    }

    /** 1.0.28: War's held moves: their timelines, their shapes, his body through them (no snap from one phase into the next). */
    private static void held() {
        double was = -1.0;
        for (double t = 0.0; t <= com.hex.kagune.control.king.WarRules.REAPER_REACH + 1; t += 0.25) {
            double r = com.hex.kagune.control.king.WarRules.reaperReach(t, false);
            check(r >= was && r >= 0.0 && r <= 1.0, "Black Reaper's centipedes go out without coming back (" + t + ")");
            was = r;
        }
        check(was == 1.0 && com.hex.kagune.control.king.WarRules.reaperReach(0.0, true) == 1.0
            && com.hex.kagune.control.king.WarRules.reaperReach(com.hex.kagune.control.king.WarRules.REAPER_RETRACT, true) == 0.0,
            "out all the way, back all the way");
        check(com.hex.kagune.control.king.WarRules.defyClosed(com.hex.kagune.control.king.WarRules.DEFY_CLOSE, false) == 1.0
            && com.hex.kagune.control.king.WarRules.defyClosed(0.0, true) == 1.0
            && com.hex.kagune.control.king.WarRules.defyClosed(com.hex.kagune.control.king.WarRules.DEFY_OPEN, true) == 0.0, "the cocoon closes and opens");
        check(com.hex.kagune.control.king.WarRules.defySpike(-1.0) == 0.0 && com.hex.kagune.control.king.WarRules.defySpike(3.0) == 1.0
            && com.hex.kagune.control.king.WarRules.defySpike(com.hex.kagune.control.king.WarRules.DEFY_SPIKE_TICKS) == 0.0, "a spike out of it, and back");
        check(Math.abs(com.hex.kagune.control.king.WarRules.spireReach(1.0) - 25.0) < 1.0E-9
            && com.hex.kagune.control.king.WarRules.spireReach(0.0) < com.hex.kagune.control.king.WarRules.spireReach(0.5)
            && com.hex.kagune.control.king.WarRules.spireSize(0.0) < com.hex.kagune.control.king.WarRules.spireSize(1.0)
            && com.hex.kagune.control.king.WarRules.spireArms(0.0) < com.hex.kagune.control.king.WarRules.spireArms(1.0)
            && com.hex.kagune.control.king.WarRules.spireCharge(1000.0) == 1.0, "the Spire Wave grows the longer it is held, to 25 blocks");
        for (double charge : new double[] {0.0, 0.5, 1.0}) {
            double reach = com.hex.kagune.control.king.WarRules.spireReach(charge);
            double speed = com.hex.kagune.control.king.WarRules.spireSpeed(charge);
            int arrive = com.hex.kagune.control.king.WarRules.spireArrive(reach, speed);
            check(arrive >= 10 && speed <= 1.0, "the Spire Wave goes out slow (" + charge + ")");
            double front = 0.0;
            for (double t = 0.0; t <= arrive; t += 0.25) {
                double f = com.hex.kagune.control.king.WarRules.spireFront(t, reach, speed);
                check(f >= front - 1.0E-9 && f <= reach + 1.0E-9 && f - front < speed * 0.6, "the mass goes on steadily (" + charge + ", " + t + ")");
                front = f;
            }
            check(Math.abs(com.hex.kagune.control.king.WarRules.spireFront(arrive + 2.0, reach, speed) - reach) < 1.0E-9
                && com.hex.kagune.control.king.WarRules.spireFront(com.hex.kagune.control.king.WarRules.spireTicks(reach, speed), reach, speed) < 1.0E-9,
                "it holds at the end through the crush, then comes all the way back (" + charge + ")");
            List<com.hex.kagune.control.king.WarHoldModel.Arm> mass = com.hex.kagune.control.king.WarHoldModel.mass(new double[] {0, 1.2, 0.7},
                new double[] {0, 0, 1}, new double[] {-0.3, 1.2, 0.5}, reach, com.hex.kagune.control.king.WarRules.spireSize(charge), speed, charge,
                com.hex.kagune.control.king.WarRules.spireArms(charge), 3, arrive * 0.5, 100.0);
            check(mass.size() == com.hex.kagune.control.king.WarRules.spireArms(charge), "every centipede of the mass is drawn (" + charge + ")");
            for (com.hex.kagune.control.king.WarHoldModel.Arm arm : mass) {
                double[] head = arm.path()[arm.path().length - 1];
                double f = com.hex.kagune.control.king.WarRules.spireFront(arrive * 0.5, reach, speed);
                check(Double.isFinite(head[0]) && head[2] <= f + 3.0 && head[2] >= f - 3.0, "the mass's heads are at its front (" + charge + ")");
                double[] tail = arm.path()[0];
                check(Math.abs(tail[0] + 0.3) < 1.0E-6 && Math.abs(tail[2] - 0.5) < 1.0E-6, "its tails stay on his hand");
            }
        }
        double[][] roots = new double[com.hex.kagune.control.king.WarRules.REAPER_ARMS][];
        for (int k = 0; k < roots.length; k++) {
            roots[k] = new double[] {(k - 1) * 0.15, 1.3, -0.15};
        }
        double[] prey = {0.0, 1.0, 6.0};
        List<com.hex.kagune.control.king.WarHoldModel.Arm> out = com.hex.kagune.control.king.WarHoldModel.reaper(roots, new double[] {0, 0, 1},
            new double[] {1, 0, 0}, prey, 0.3, com.hex.kagune.control.king.WarRules.REAPER_DRAG, 5.0, 100.0);
        check(out.size() == roots.length, "Black Reaper: a centipede out of each root");
        for (com.hex.kagune.control.king.WarHoldModel.Arm arm : out) {
            double[] head = arm.path()[arm.path().length - 1];
            double off = Math.sqrt(Math.pow(head[0] - prey[0], 2) + Math.pow(head[1] - prey[1], 2) + Math.pow(head[2] - prey[2], 2));
            check(off < 0.8 && arm.jaw() < 0.2, "each bites into what it drags, jaws shut on it");
        }
        List<com.hex.kagune.control.king.WarHoldModel.Arm> early = com.hex.kagune.control.king.WarHoldModel.reaper(roots, new double[] {0, 0, 1},
            new double[] {1, 0, 0}, prey, 0.3, com.hex.kagune.control.king.WarRules.REAPER_OUT, 1.0, 100.0);
        double[] reaching = early.get(0).path()[early.get(0).path().length - 1];
        check(reaching[2] < prey[2] - 1.0 && early.get(0).jaw() > 0.9, "on the way out their heads are short of it, jaws open");
        check(com.hex.kagune.control.king.WarHoldModel.coils(new double[] {0, 1.3, 0.2}, new double[] {0, 1.3, 0.8}, 30.0, 100.0).size()
            == com.hex.kagune.control.king.WarRules.spireArms(com.hex.kagune.control.king.WarRules.spireCharge(30.0)), "the Spire Wave's coils, as many as it has");
        // His body: no snap from one phase of Black Reaper into the next.
        int[][] steps = {{com.hex.kagune.control.king.WarRules.REAPER_OUT, com.hex.kagune.control.king.WarRules.REAPER_DRAG, com.hex.kagune.control.king.WarRules.REAPER_REACH},
            {com.hex.kagune.control.king.WarRules.REAPER_DRAG, com.hex.kagune.control.king.WarRules.REAPER_THROUGH, 20},
            {com.hex.kagune.control.king.WarRules.REAPER_THROUGH, com.hex.kagune.control.king.WarRules.REAPER_BACK, com.hex.kagune.control.king.WarRules.REAPER_IMPALE},
            {com.hex.kagune.control.king.WarRules.REAPER_DRAG, com.hex.kagune.control.king.WarRules.REAPER_BACK, 40}};
        for (int[] step : steps) {
            RigPose end = new RigPose();
            WarPose.reaper(end, step[0], step[2], -1, 1.0);
            RigPose start = new RigPose();
            WarPose.reaper(start, step[1], 0.0, step[0], 1.0);
            float jump = Math.max(Math.abs(end.rightArmX - start.rightArmX), Math.max(Math.abs(end.leftArmX - start.leftArmX), Math.abs(end.twist - start.twist)));
            check(jump < 0.45, "Black Reaper's body goes from phase " + step[0] + " into " + step[1] + " without a snap (" + jump + ")");
        }
        RigPose done = new RigPose();
        WarPose.reaper(done, com.hex.kagune.control.king.WarRules.REAPER_BACK, com.hex.kagune.control.king.WarRules.REAPER_RETRACT, com.hex.kagune.control.king.WarRules.REAPER_THROUGH, 1.0);
        check(Math.abs(done.rightArmX) < 0.02 && Math.abs(done.twist) < 0.02, "and back to himself at the end");
        RigPose stab = new RigPose();
        WarPose.reaper(stab, com.hex.kagune.control.king.WarRules.REAPER_THROUGH, 3.0, com.hex.kagune.control.king.WarRules.REAPER_DRAG, 1.0);
        RigPose cover = new RigPose();
        WarPose.defy(cover, com.hex.kagune.control.king.WarRules.DEFY_HELD, 20.0, 1.0);
        RigPose open = new RigPose();
        WarPose.defy(open, com.hex.kagune.control.king.WarRules.DEFY_OPENING, com.hex.kagune.control.king.WarRules.DEFY_OPEN, 1.0);
        RigPose aiming = new RigPose();
        WarPose.spireAim(aiming, 30.0, 1.0);
        check(stab.rightArmX < -1.3F && cover.rightArmX < -1.7F && cover.rightElbow > 1.3F && Math.abs(open.rightArmX) < 0.05F && aiming.leftArmX < -1.4F,
            "the blade thrust out; the arm over his face; open again; the arm pointed out");
        check(WarModel.Motion.orbed(1.0).orb() == 1.0 && WarModel.Motion.of(com.hex.kagune.control.king.KingMove.COCOON, 0.5).orb() == 0.0,
            "the cocoon's sphere is King's Defiance's alone");
    }

    /** 1.0.29: Centipede Dominion driven: its lunge, slam and leap, out and back without a jump; its keys apart. */
    private static void dominion() {
        double was = 0.0;
        double most = 0.0;
        for (double t = 0.0; t <= com.hex.kagune.control.king.WarRules.WYRM_LUNGE_TICKS + 1; t += 0.25) {
            double v = com.hex.kagune.control.king.WarRules.wyrmLunge(t);
            check(v >= 0.0 && v <= 1.0 && Math.abs(v - was) < 0.3, "the lunge goes out and back smoothly (" + t + ")");
            most = Math.max(most, v);
            was = v;
        }
        check(most > 0.99 && was == 0.0, "the lunge reaches all the way out and comes all the way back");
        double rearMost = 0.0;
        for (double t = 0.0; t <= com.hex.kagune.control.king.WarRules.WYRM_SLAM_TICKS; t += 0.5) {
            rearMost = Math.max(rearMost, com.hex.kagune.control.king.WarRules.wyrmRear(t));
        }
        check(rearMost > 3.0 && com.hex.kagune.control.king.WarRules.wyrmRear(com.hex.kagune.control.king.WarRules.WYRM_SLAM_LANDS) == 0.0,
            "reared up high, down on the ground as it lands");
        check(com.hex.kagune.control.king.WarRules.wyrmLeap(com.hex.kagune.control.king.WarRules.WYRM_LEAP_TICKS / 2.0) > 4.0
            && com.hex.kagune.control.king.WarRules.wyrmLeap(com.hex.kagune.control.king.WarRules.WYRM_LEAP_TICKS) < 1.0E-9, "the leap goes up and comes down");
        int[] bits = {com.hex.kagune.control.king.WarRules.WYRM_FORWARD, com.hex.kagune.control.king.WarRules.WYRM_BACK, com.hex.kagune.control.king.WarRules.WYRM_LEFT,
            com.hex.kagune.control.king.WarRules.WYRM_RIGHT, com.hex.kagune.control.king.WarRules.WYRM_JUMP, com.hex.kagune.control.king.WarRules.WYRM_SPRINT,
            com.hex.kagune.control.king.WarRules.WYRM_SNEAK};
        int all = 0;
        for (int b : bits) {
            check((all & b) == 0 && Integer.bitCount(b) == 1, "each driving key its own bit");
            all |= b;
        }
        check(com.hex.kagune.control.king.WarRules.WYRM_RUSH > com.hex.kagune.control.king.WarRules.WYRM_SPEED
            && com.hex.kagune.control.king.WarRules.WYRM_TURN_MOVING > com.hex.kagune.control.king.WarRules.WYRM_TURN, "it rushes faster and turns harder going");
    }

    /** 1.0.30: Centipede Dominion driven: its lunge, slam and leap, out and back without a jump; its keys apart. */
    private static void driven() {
        double was = 0.0;
        double most = 0.0;
        for (double t = 0.0; t <= com.hex.kagune.control.king.WarRules.WYRM_LUNGE_TICKS + 1; t += 0.25) {
            double v = com.hex.kagune.control.king.WarRules.wyrmLunge(t);
            check(v >= 0.0 && v <= 1.0 && Math.abs(v - was) < 0.3, "the lunge goes out and back smoothly (" + t + ")");
            most = Math.max(most, v);
            was = v;
        }
        check(most > 0.99 && was == 0.0, "the lunge reaches all the way out and comes all the way back");
        double rearMost = 0.0;
        for (double t = 0.0; t <= com.hex.kagune.control.king.WarRules.WYRM_SLAM_TICKS; t += 0.5) {
            rearMost = Math.max(rearMost, com.hex.kagune.control.king.WarRules.wyrmRear(t));
        }
        check(rearMost > 3.0 && com.hex.kagune.control.king.WarRules.wyrmRear(com.hex.kagune.control.king.WarRules.WYRM_SLAM_LANDS) == 0.0,
            "reared up high, down on the ground as it lands");
        check(com.hex.kagune.control.king.WarRules.wyrmLeap(com.hex.kagune.control.king.WarRules.WYRM_LEAP_TICKS / 2.0) > 4.0
            && com.hex.kagune.control.king.WarRules.wyrmLeap(com.hex.kagune.control.king.WarRules.WYRM_LEAP_TICKS) < 1.0E-9, "the leap goes up and comes down");
        int[] bits = {com.hex.kagune.control.king.WarRules.WYRM_FORWARD, com.hex.kagune.control.king.WarRules.WYRM_BACK, com.hex.kagune.control.king.WarRules.WYRM_LEFT,
            com.hex.kagune.control.king.WarRules.WYRM_RIGHT, com.hex.kagune.control.king.WarRules.WYRM_JUMP, com.hex.kagune.control.king.WarRules.WYRM_SPRINT,
            com.hex.kagune.control.king.WarRules.WYRM_SNEAK, com.hex.kagune.control.king.WarRules.WYRM_KEY_BITE, com.hex.kagune.control.king.WarRules.WYRM_KEY_SLAM};
        int all = 0;
        for (int b : bits) {
            check((all & b) == 0 && Integer.bitCount(b) == 1, "each driving key its own bit");
            all |= b;
        }
        check(com.hex.kagune.control.king.WarRules.WYRM_RUSH > com.hex.kagune.control.king.WarRules.WYRM_SPEED
            && com.hex.kagune.control.king.WarRules.WYRM_TURN_MOVING > com.hex.kagune.control.king.WarRules.WYRM_TURN, "it rushes faster and turns harder going");
        // 1.0.33: driven as he would walk, round where he looks (here, looking along +z: his left is +x).
        double[] ahead = com.hex.kagune.control.king.WarRules.wyrmWay(0.0, 2.0, true, 0);
        double[] left = com.hex.kagune.control.king.WarRules.wyrmWay(0.0, 2.0, false, 1);
        double[] right = com.hex.kagune.control.king.WarRules.wyrmWay(0.0, 2.0, false, -1);
        double[] veer = com.hex.kagune.control.king.WarRules.wyrmWay(0.0, 2.0, true, 1);
        check(Math.abs(ahead[0]) < 1.0E-9 && Math.abs(ahead[1] - 1.0) < 1.0E-9 && Math.abs(left[0] - 1.0) < 1.0E-9 && Math.abs(right[0] + 1.0) < 1.0E-9
            && Math.abs(veer[0] - veer[1]) < 1.0E-9 && veer[0] > 0.0, "W where he looks, A to his left, D to his right, both between");
        check(com.hex.kagune.control.king.WarRules.wyrmWay(0.0, 1.0, false, 0) == null && com.hex.kagune.control.king.WarRules.wyrmWay(0.0, 0.0, true, 0) == null,
            "no key, no way (it comes to a stop)");
        // 1.0.39: his aim goes with the keys and comes back out as the view Minecraft would make of it, every way round.
        check(com.hex.kagune.control.king.WarRules.wyrmLook(0) == null, "no aim sent, none read");
        double aimWorst = 0.0;
        for (float yaw = -720.0F; yaw <= 720.0F; yaw += 7.3F) {
            for (float pitch = -90.0F; pitch <= 90.0F; pitch += 4.5F) {
                double[] seen = com.hex.kagune.control.king.WarRules.wyrmLook(com.hex.kagune.control.king.WarRules.wyrmAim(yaw, pitch));
                double yr = Math.toRadians(yaw);
                double pr = Math.toRadians(pitch);
                double[] want = {-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr)};
                double d = Math.sqrt((seen[0] - want[0]) * (seen[0] - want[0]) + (seen[1] - want[1]) * (seen[1] - want[1]) + (seen[2] - want[2]) * (seen[2] - want[2]));
                aimWorst = Math.max(aimWorst, d);
            }
        }
        check(aimWorst < 1.0E-3, "the aim sent is the aim read, to a thousandth (worst " + aimWorst + ")");
        // 1.0.33: it rises out of him smoothly: up over him to the roar, held, then down before him onto the ground.
        double[] last = com.hex.kagune.control.king.WarRules.wyrmRise(0.0);
        check(Math.abs(last[0]) < 1.0E-9 && Math.abs(last[1]) < 1.0E-9, "it starts in his back");
        for (double t = 0.0; t <= com.hex.kagune.control.king.WarRules.WYRM_RISE; t += 0.25) {
            double[] at = com.hex.kagune.control.king.WarRules.wyrmRise(t);
            check(Math.abs(at[0] - last[0]) < 0.8 && Math.abs(at[1] - last[1]) < 0.8 && at[0] >= last[0] - 1.0E-9, "it rises out of him smoothly (" + t + ")");
            last = at;
        }
        double[] top = com.hex.kagune.control.king.WarRules.wyrmRise(com.hex.kagune.control.king.WarRules.WYRM_RISE_TOP);
        check(Math.abs(top[1] - com.hex.kagune.control.king.WarRules.WYRM_RISE_HEIGHT) < 1.0E-9 && Math.abs(last[1]) < 1.0E-9
            && Math.abs(last[0] - com.hex.kagune.control.king.WarRules.WYRM_RISE_REACH) < 1.0E-9, "reared high for the roar, down before him at the end");
        check(com.hex.kagune.control.king.WarRules.WYRM_PRELUDE + com.hex.kagune.control.king.WarRules.WYRM_RISE <= 50, "the little turning stays short (2.5 s at most)");
    }

    /** 1.0.27: the clock things are drawn by: smooth and on the server's time whatever the server does to the game time. */
    private static void clock() {
        // {name, server ticks a second, how far off each second's time packet finds the client (ticks)}
        double[][] cases = {{20.0, 1.0}, {20.0, -1.0}, {15.0, 0.0}, {10.0, 0.0}};
        for (double[] c : cases) {
            com.hex.kagune.control.client.SmoothClock clock = new com.hex.kagune.control.client.SmoothClock();
            double game = 1000.0;
            double server = 1000.0;
            clock.reset(game, 0L);
            double last = clock.time(0.0);
            double worstJump = 0.0;
            double worstOff = 0.0;
            for (long tick = 1; tick <= 600; tick++) {
                // Each client tick: four frames through it, then the tick (the client's game time on by one, the server's at its own rate).
                for (double partial = 0.25; partial < 1.0; partial += 0.25) {
                    double t = clock.time(partial);
                    check(t >= last, "the clock never goes back (" + c[0] + " TPS)");
                    worstJump = Math.max(worstJump, t - last);
                    last = t;
                }
                game += 1.0;
                server += c[0] / 20.0;
                if (tick % 20 == 0) {
                    game = Math.floor(server) + c[1];
                }
                clock.at(game, tick);
                double t = clock.time(0.0);
                check(t >= last, "the clock never goes back across a tick (" + c[0] + " TPS)");
                worstJump = Math.max(worstJump, t - last);
                last = t;
                if (tick > 200) {
                    worstOff = Math.max(worstOff, Math.abs(t - server));
                }
            }
            check(worstJump < 0.45, "the clock runs on without a jump (" + c[0] + " TPS, worst " + worstJump + ")");
            check(worstOff < (c[0] >= 20.0 ? 2.5 : 14.0), "the clock stays on the server's time (" + c[0] + " TPS, worst " + worstOff + ")");
        }
        com.hex.kagune.control.client.SmoothClock clock = new com.hex.kagune.control.client.SmoothClock();
        clock.reset(50.0, 10L);
        clock.at(90000.0, 11L);
        check(Math.abs(clock.time(0.0) - 90000.0) < 1.0E-9, "a clock far off jumps onto the game time");
    }

    /** 1.0.25: Peace's arms alive (PeaceArms) and its blows' own arcs. */
    private static void peaceArms() {
        com.hex.kagune.control.king.PeaceArms.Source rest = new com.hex.kagune.control.king.PeaceArms.Source() {
            @Override
            public PeaceMotion.State state(long tick) {
                return PeaceMotion.State.REST;
            }

            @Override
            public double grow(long tick) {
                return 1.0;
            }
        };
        com.hex.kagune.control.king.PeaceArms arms = new com.hex.kagune.control.king.PeaceArms(77L);
        com.hex.kagune.control.king.PeaceArms twin = new com.hex.kagune.control.king.PeaceArms(77L);
        double[][][] first = arms.lines(0L, rest, null);
        boolean held = true;
        boolean finite = true;
        double moved = 0.0;
        double offCurve = 0.0;
        double thinnest = Double.MAX_VALUE;
        double[][][] at150 = null;
        for (long t = 1; t <= 200; t++) {
            double[][][] lines = arms.lines(t, rest, null);
            if (t == 150) {
                at150 = lines;
            }
            for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
                PeaceMotion.Tip tip = PeaceMotion.tip(blade, (double)t, 1.0, PeaceMotion.State.REST);
                double[][] still = com.hex.kagune.control.king.PeaceModel.armCurve(blade, tip);
                double[][] line = lines[blade];
                held &= gap(line[0], still[0]) < 1.0E-6 && gap(line[line.length - 1], still[still.length - 1]) < 1.0E-6;
                for (int i = 0; i < line.length; i++) {
                    for (double c : line[i]) {
                        finite &= Double.isFinite(c);
                    }
                    thinnest = Math.min(thinnest, line[i][3]);
                    offCurve = Math.max(offCurve, gap(line[i], still[i]));
                }
                if (t == 120) {
                    moved = Math.max(moved, gap(line[line.length / 2], first[blade][line.length / 2]));
                }
            }
        }
        check(held, "every arm still roots in his back and still holds its blade where the blade is");
        check(finite && thinnest > 0.5, "the arms stay finite and never thinner than a cord");
        check(offCurve > 2.0, "alive: the arms leave the shape they would hold still (" + offCurve + " px)");
        check(moved > 1.5, "at rest the arms move on their own (" + moved + " px over six seconds)");
        double[][][] a = at150;
        for (long t = 0; t < 150; t++) {
            twin.lines(t, rest, null);
        }
        double[][][] b = twin.lines(150L, rest, null);
        check(gap(a[2][12], b[2][12]) < 1.0E-9, "the same seed and the same moves, the same life");
        double[][][] half = twin.lines(150.5, rest, null);
        double[][][] next = twin.lines(151L, rest, null);
        check(gap(half[2][12], new double[] {(a[2][12][0] + next[2][12][0]) / 2, (a[2][12][1] + next[2][12][1]) / 2, (a[2][12][2] + next[2][12][2]) / 2})
            < 1.0E-9, "between two ticks, between them");
        // His turning leaves the arms behind, and they swing back.
        com.hex.kagune.control.king.PeaceArms turned = new com.hex.kagune.control.king.PeaceArms(77L);
        com.hex.kagune.control.king.PeaceArms calm = new com.hex.kagune.control.king.PeaceArms(77L);
        turned.lines(0L, rest, null);
        calm.lines(0L, rest, null);
        double dragged = 0.0;
        for (long t = 1; t <= 10; t++) {
            double[][][] x = turned.lines(t, rest, new double[] {0.15, 0.0, 0.0, 0.25});
            double[][][] y = calm.lines(t, rest, null);
            dragged = Math.max(dragged, gap(x[3][12], y[3][12]));
        }
        double after = 0.0;
        for (long t = 11; t <= 70; t++) {
            double[][][] x = turned.lines(t, rest, null);
            double[][][] y = calm.lines(t, rest, null);
            after = gap(x[3][12], y[3][12]);
        }
        check(dragged > 2.0, "running and turning drag the arms behind him (" + dragged + " px)");
        check(after < 0.5, "and they swing back to him (" + after + " px left after three seconds)");
        // The blows' arcs: begun at another moment, the same blow goes another way; at the hit it is where it always was.
        PeaceMotion.Tip one = PeaceMotion.pose(PeaceMotion.MR, 1004.0, KingMove.BLADES, 0, 4.0, 0.0, 0.0, 0.0);
        PeaceMotion.Tip two = PeaceMotion.pose(PeaceMotion.MR, 2007.0, KingMove.BLADES, 0, 4.0, 0.0, 0.0, 0.0);
        PeaceMotion.Tip hitOne = PeaceMotion.pose(PeaceMotion.MR, 1005.0, KingMove.BLADES, 0, 5.0, 0.0, 0.0, 0.0);
        PeaceMotion.Tip hitTwo = PeaceMotion.pose(PeaceMotion.MR, 2008.0, KingMove.BLADES, 0, 5.0, 0.0, 0.0, 0.0);
        check(gap(one.at(), two.at()) > 3.0, "no two Lances go the same way (" + gap(one.at(), two.at()) + " px apart on the way)");
        check(gap(hitOne.at(), hitTwo.at()) < 1.0E-6, "but each lands where the Lance always lands");
        PeaceMotion.Tip again = PeaceMotion.pose(PeaceMotion.MR, 1004.0, KingMove.BLADES, 0, 4.0, 0.0, 0.0, 0.0);
        check(gap(one.at(), again.at()) < 1.0E-9, "the server and every client draw the same arc from when it began");
    }

    private static double gap(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void crown() {
        check(!KingRules.awake("") && KingRules.chosen("") == KingForm.NONE, "before the Dragon: no crown");
        check(KingRules.awake(KingRules.AWAKE) && KingRules.chosen(KingRules.AWAKE) == KingForm.NONE, "after the Dragon: a crown to choose");
        check(KingRules.choose("", KingForm.WAR).isEmpty(), "nothing is chosen before the Dragon");
        String peace = KingRules.choose(KingRules.AWAKE, KingForm.PEACE);
        check(KingRules.chosen(peace) == KingForm.PEACE && KingRules.awake(peace), "Peace chosen");
        check(KingRules.choose(peace, KingForm.WAR).equals(peace), "Peace is for good too");
        String war = KingRules.choose(KingRules.AWAKE, KingForm.WAR);
        check(KingRules.chosen(war) == KingForm.WAR && KingRules.awake(war), "War chosen");
        check(KingRules.choose(war, KingForm.PEACE).equals(war), "the choice is for good");
        for (KingForm form : KingForm.values()) {
            check(KingForm.byId(form.id()) == form && KingForm.byOrdinal(form.ordinal()) == form, "round trip " + form);
        }
        check(KingForm.byOrdinal(99) == KingForm.NONE && KingForm.byId("nonsense") == KingForm.NONE, "unknown forms are none");
        double[] war5 = KingRules.boosts(KingForm.WAR);
        check(war5[0] == 0.45 && war5[1] == 0.35 && war5[2] == 0.30 && war5[3] == 0.80 && KingRules.reduction(KingForm.WAR) == 0.25, "War's numbers");
        check(KingRules.reduction(KingForm.NONE) == 0.0 && KingRules.boosts(KingForm.NONE)[3] == 0.0, "nothing worn gives nothing");
        check(KingRules.grow(true, 0) == 0.0 && KingRules.grow(true, KingRules.GROW_TICKS) == 1.0, "it grows over him");
        check(KingRules.grow(false, 0) == 1.0 && KingRules.grow(false, KingRules.GROW_TICKS * 2) == 0.0, "and draws back");
        double last = -1.0;
        boolean rising = true;
        for (int t = 0; t <= KingRules.GROW_TICKS; t++) {
            double g = KingRules.grow(true, t);
            rising &= g >= last;
            last = g;
        }
        check(rising, "growing never shrinks");
    }

    private static void night() {
        check(KingRules.night(6000L) == 0.0 && KingRules.night(1000L) == 0.0, "noon and morning are day");
        check(KingRules.night(18000L) == 1.0, "midnight is the dead of night");
        check(KingRules.night(13000L) > 0.0 && KingRules.night(13000L) < 1.0, "dusk eases in");
        check(KingRules.night(23000L) > 0.0 && KingRules.night(23000L) < 1.0, "dawn eases out");
        check(KingRules.night(18000L + 24000L * 7) == 1.0 && KingRules.night(-6000L) == 1.0, "every day, any day count");
    }

    private static void pose() {
        boolean bounded = true;
        boolean tilts = false;
        double maxRoll = 0.0;
        double bow = 0.0;
        int frames = 0;
        for (int t = 0; t < 20 * 120; t += 3) {
            RigPose p = new RigPose();
            WarPose.apply(p, t, 42, 1.0F, 0.0F);
            bounded &= Float.isFinite(p.headX) && Float.isFinite(p.headY) && Float.isFinite(p.headZ) && Math.abs(p.headZ) < 1.4F
                && Math.abs(p.headY) < 1.4F && Math.abs(p.headX) < 1.0F && p.lean < 0.0F && p.rightArmZ > 0.2F && p.leftArmZ < -0.2F;
            maxRoll = Math.max(maxRoll, Math.abs(p.headZ));
            tilts |= Math.abs(p.headZ) > 0.3F;
            bow += p.headX + p.lean; // the head rides the lean: what it looks down by, all told
            frames++;
        }
        check(bounded, "War's head stays on his shoulders (bounded, finite), leaning back, arms open (the god's stance)");
        double down = bow / frames;
        check(down > 0.2 && down < 0.5, "and bowed, glaring up from under the brow (about twenty degrees down): " + down);
        check(tilts, "and goes far over now and then: " + maxRoll);
        RigPose none = new RigPose();
        WarPose.apply(none, 100.0, 42, 0.0F, 0.0F);
        check(none.headZ == 0.0F && none.lean == 0.0F && none.rightArmZ == 0.0F, "nothing without War");
        RigPose a = new RigPose();
        RigPose b = new RigPose();
        WarPose.apply(a, 777.0, 42, 1.0F, 0.0F);
        WarPose.apply(b, 777.0, 42, 1.0F, 0.0F);
        check(a.headZ == b.headZ && a.headY == b.headY, "the same moment poses the same on every client");
    }

    /**
     * 1.0.16: Peace's four blades in every move, frame by frame: never inside his body (chain or weapon), the roots never leaving his
     * back, every point finite, each blade striking where its move says, the hand-over between moves never jumping.
     */
    private static void peaceBlades() {
        com.hex.kagune.control.king.KingMove[] moves = {com.hex.kagune.control.king.KingMove.NONE, com.hex.kagune.control.king.KingMove.BLADES,
            com.hex.kagune.control.king.KingMove.WALTZ, com.hex.kagune.control.king.KingMove.AEGIS, com.hex.kagune.control.king.KingMove.AEGIS_OPEN,
            com.hex.kagune.control.king.KingMove.ASCENT, com.hex.kagune.control.king.KingMove.DIVE, com.hex.kagune.control.king.KingMove.LANDING,
            com.hex.kagune.control.king.KingMove.PIERCE, com.hex.kagune.control.king.KingMove.REBIRTH, com.hex.kagune.control.king.KingMove.HORIZON};
        int checked = 0;
        String worst = null;
        double deepest = 0.0;
        boolean finite = true;
        for (com.hex.kagune.control.king.KingMove move : moves) {
            int levels = move == com.hex.kagune.control.king.KingMove.BLADES ? com.hex.kagune.control.king.PeaceStrike.values().length : 1;
            for (int level = 0; level < levels; level++) {
                double length = move == com.hex.kagune.control.king.KingMove.BLADES ? move.ticks(level)
                    : move.ticks(level) > 0 ? move.ticks(level) : 40.0;
                for (double pitch : new double[] {0.0, -0.6, 0.5}) {
                    for (double age = 0.0; age <= length; age += 0.5) {
                        com.hex.kagune.control.king.PeaceMotion.State state = new com.hex.kagune.control.king.PeaceMotion.State(move, level, age, pitch, null);
                        for (int blade = 0; blade < com.hex.kagune.control.king.PeaceMotion.BLADES; blade++) {
                            com.hex.kagune.control.king.PeaceMotion.Tip tip = com.hex.kagune.control.king.PeaceMotion.tip(blade, 200.0 + age, 1.0, state);
                            double[][] line = com.hex.kagune.control.king.PeaceModel.armLine(blade, tip, 200.0 + age, 1.0);
                            for (int i = 0; i < line.length; i++) {
                                double t = (double)i / (line.length - 1);
                                double[] q = line[i];
                                finite &= Double.isFinite(q[0]) && Double.isFinite(q[1]) && Double.isFinite(q[2]);
                                if (t < 0.12) {
                                    continue; // leaving his back
                                }
                                double depth = inside(q, q[3] * 0.85);
                                checked++;
                                if (depth > deepest) {
                                    deepest = depth;
                                    worst = move + "/" + level + " blade " + blade + " age " + age + " pitch " + pitch + " at " + t;
                                }
                            }
                            double[] point = com.hex.kagune.control.king.PeaceMotion.point(tip, blade);
                            for (double[] q : new double[][] {tip.at(), point}) {
                                finite &= Double.isFinite(q[0]) && Double.isFinite(q[1]) && Double.isFinite(q[2]);
                                double depth = inside(q, 0.0);
                                checked++;
                                if (depth > deepest) {
                                    deepest = depth;
                                    worst = move + "/" + level + " blade " + blade + "'s weapon, age " + age + " pitch " + pitch;
                                }
                            }
                        }
                    }
                }
            }
        }
        check(finite, "every blade finite in every move");
        check(deepest < 0.75, "no blade ever inside his body (" + checked + " points; deepest " + deepest + " px: " + worst + ")");
        // The hand-over: a move cut off mid-way gives way to the next without a jump.
        com.hex.kagune.control.king.PeaceMotion.State before = new com.hex.kagune.control.king.PeaceMotion.State(com.hex.kagune.control.king.KingMove.BLADES, 0,
            5.0, 0.0, null);
        com.hex.kagune.control.king.PeaceMotion.State after = new com.hex.kagune.control.king.PeaceMotion.State(com.hex.kagune.control.king.KingMove.BLADES, 1,
            0.0, 0.0, null, com.hex.kagune.control.king.KingMove.BLADES, 0, 5.0, 0.0, 0.0);
        // (the same move, a new blow: no hand-over by move; check across moves instead)
        com.hex.kagune.control.king.PeaceMotion.State waltz = new com.hex.kagune.control.king.PeaceMotion.State(com.hex.kagune.control.king.KingMove.WALTZ, 0, 0.0,
            0.0, null, com.hex.kagune.control.king.KingMove.BLADES, 0, 5.0, 0.0, 0.0);
        double[] a = com.hex.kagune.control.king.PeaceMotion.tip(com.hex.kagune.control.king.PeaceMotion.MR, 100.0, 1.0, before).at();
        double[] b = com.hex.kagune.control.king.PeaceMotion.tip(com.hex.kagune.control.king.PeaceMotion.MR, 100.0, 1.0, waltz).at();
        check(Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2)) < 1.0E-6,
            "a move cut off is where the next begins: the Lance's blade hands over to the Waltz without a jump");
        // Each blow strikes with its own blades, out in front of him.
        for (com.hex.kagune.control.king.PeaceStrike blow : com.hex.kagune.control.king.PeaceStrike.values()) {
            if (blow == com.hex.kagune.control.king.PeaceStrike.HALO) {
                continue; // all round him, not before him
            }
            com.hex.kagune.control.king.PeaceMotion.State at = new com.hex.kagune.control.king.PeaceMotion.State(com.hex.kagune.control.king.KingMove.BLADES,
                blow.ordinal(), blow.impact, 0.0, null);
            boolean ahead = true;
            for (int blade : blow.blades()) {
                double[] heart = com.hex.kagune.control.king.PeaceMotion.tip(blade, 100.0, 1.0, at).at();
                ahead &= heart[2] < -18.0; // over a block before him
            }
            check(ahead, blow + ": its blades are out before him on its impact tick");
        }
        com.hex.kagune.control.king.PeaceMotion.State lance = new com.hex.kagune.control.king.PeaceMotion.State(com.hex.kagune.control.king.KingMove.BLADES, 0,
            com.hex.kagune.control.king.PeaceStrike.LANCE.impact, 0.0, null);
        com.hex.kagune.control.king.PeaceMotion.Tip spear = com.hex.kagune.control.king.PeaceMotion.tip(com.hex.kagune.control.king.PeaceMotion.MR, 100.0, 1.0, lance);
        check(spear.dir()[2] < -0.95, "the Lance's point goes straight ahead along its thrust");
        // What the server cuts with (PeaceKit sweeps the same paths): each blow reaches a body before him.
        for (PeaceStrike blow : PeaceStrike.values()) {
            check(peaceReach(KingMove.BLADES, blow.ordinal(), 0.0, 2.5) < 1.2, blow + " reaches a body 2.5 blocks before him");
        }
        peaceAbilities();
    }

    /** 1.0.34: Peace's reworked six, by their numbers (PeaceRules) and the shapes every client draws of them (PeaceFxModel). */
    private static void peaceAbilities() {
        java.util.Set<Integer> sent = new java.util.HashSet<>();
        boolean inTime = true;
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            int at = com.hex.kagune.control.king.PeaceRules.waltzSent(blade);
            sent.add(at);
            inTime &= at >= 0 && at + com.hex.kagune.control.king.PeaceRules.WALTZ_REACH <= com.hex.kagune.control.king.PeaceRules.WALTZ_IN;
        }
        check(sent.size() == PeaceMotion.BLADES && inTime, "the Waltz sends every blade, one at a time, all in before they tear out");
        check(com.hex.kagune.control.king.PeaceRules.WALTZ_RIP < com.hex.kagune.control.king.PeaceRules.WALTZ_TICKS
            && KingMove.WALTZ.ticks(0) == com.hex.kagune.control.king.PeaceRules.WALTZ_TICKS, "the Waltz tears out before it ends");
        check(com.hex.kagune.control.king.PeaceRules.AEGIS_TICKS == 300, "the Aegis stands 15 seconds");
        check(com.hex.kagune.control.king.PeaceRules.shieldFor(0.0, 3.0, 0.5) == com.hex.kagune.control.king.PeaceRules.SHIELD_TOP
            && com.hex.kagune.control.king.PeaceRules.shieldFor(2.0, 0.2, 0.5) == com.hex.kagune.control.king.PeaceRules.SHIELD_RIGHT
            && com.hex.kagune.control.king.PeaceRules.shieldFor(-2.0, 0.2, -0.5) == com.hex.kagune.control.king.PeaceRules.SHIELD_LEFT,
            "the Aegis meets a blow from over him, his right, his left with the shield there");
        double h = com.hex.kagune.control.king.PeaceRules.ASCENT_HEIGHT;
        check(h >= 15.0 && h <= 20.0 && Math.abs(com.hex.kagune.control.king.PeaceRules.lifted(com.hex.kagune.control.king.PeaceRules.ASCENT_RISE, h) - h) < 1.0E-9
            && com.hex.kagune.control.king.PeaceRules.lifted(0.0, h) == 0.0, "the Ascent lifts him 15 to 20 blocks, from where he stood");
        check(com.hex.kagune.control.king.PeaceRules.lowered(0.0, h) == h
            && Math.abs(com.hex.kagune.control.king.PeaceRules.lowered(com.hex.kagune.control.king.PeaceRules.ASCENT_LOWER, h)) < 1.0E-9,
            "left alone, it lets him down all the way, from where it held him");
        double[][] layout = com.hex.kagune.control.king.PeaceFxModel.eruption(1234L, com.hex.kagune.control.king.PeaceRules.ERUPT_COUNT,
            com.hex.kagune.control.king.PeaceRules.ERUPT_RADIUS);
        double[][] again = com.hex.kagune.control.king.PeaceFxModel.eruption(1234L, com.hex.kagune.control.king.PeaceRules.ERUPT_COUNT,
            com.hex.kagune.control.king.PeaceRules.ERUPT_RADIUS);
        double[][] other = com.hex.kagune.control.king.PeaceFxModel.eruption(99L, com.hex.kagune.control.king.PeaceRules.ERUPT_COUNT,
            com.hex.kagune.control.king.PeaceRules.ERUPT_RADIUS);
        boolean within = true;
        boolean same = true;
        double spread = 0.0;
        for (int i = 0; i < layout.length; i++) {
            within &= Math.hypot(layout[i][0], layout[i][1]) <= com.hex.kagune.control.king.PeaceRules.ERUPT_RADIUS + 1.0E-9;
            same &= java.util.Arrays.equals(layout[i], again[i]);
            spread += Math.abs(layout[i][0] - other[i][0]);
            for (double v : layout[i]) {
                within &= Double.isFinite(v);
            }
        }
        check(layout.length == com.hex.kagune.control.king.PeaceRules.ERUPT_COUNT && within, "the eruption's tendrils all within 30 blocks");
        check(same && spread > 10.0, "the eruption is the same on every client, and different every time");
        double longest = 0.0;
        boolean grows = true;
        for (int t = 0; t <= com.hex.kagune.control.king.PeaceRules.PIERCE_CHARGE; t += 5) {
            double l = com.hex.kagune.control.king.PeaceRules.lance(com.hex.kagune.control.king.PeaceRules.charge(t));
            grows &= l >= longest;
            longest = l;
        }
        check(grows && longest > com.hex.kagune.control.king.PeaceRules.LANCE_SHORT, "the lance grows the longer it is held");
        check(com.hex.kagune.control.king.PeaceRules.PIERCE_DRAG == 30.0 && com.hex.kagune.control.king.PeaceRules.PIERCE_PIN == 80,
            "the lance drags up to 30 blocks, pins 4 seconds");
        check(com.hex.kagune.control.king.PeaceRules.thralls(0) == 2 && com.hex.kagune.control.king.PeaceRules.thralls(80) == 3
            && com.hex.kagune.control.king.PeaceRules.thralls(140) == 4 && com.hex.kagune.control.king.PeaceRules.REBIRTH_CHARGE == 140
            && com.hex.kagune.control.king.PeaceRules.THRALL_LIFE == 300, "Rebirth: two to four, four at seven seconds, for fifteen");
        java.util.Set<Integer> horizon = new java.util.HashSet<>();
        int last = -1;
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            int at = com.hex.kagune.control.king.PeaceRules.horizonSent(blade);
            horizon.add(at);
            if (blade != PeaceMotion.GREAT) {
                last = Math.max(last, at);
            }
        }
        check(horizon.size() == PeaceMotion.BLADES && !horizon.contains(-1)
            && com.hex.kagune.control.king.PeaceRules.horizonSent(PeaceMotion.GREAT) > last, "the Horizon's blades go in one at a time, the great one's blow last");
        check(com.hex.kagune.control.king.PeaceRules.horizonRise(com.hex.kagune.control.king.PeaceRules.HORIZON_APEX)
            == com.hex.kagune.control.king.PeaceRules.HORIZON_UP, "the uppercut throws it all the way up by the apex");
    }

    /** 1.0.17: the transformations: War held in until its burst then out at once, Peace growing over him, both done in their time. */
    private static void coming() {
        KingForm war = KingForm.WAR;
        KingForm peace = KingForm.PEACE;
        int burst = com.hex.kagune.control.king.KingTransform.WAR_BURST;
        check(com.hex.kagune.control.king.KingTransform.grow(war, true, burst - 1) == 0.0, "War stays in him until the burst");
        check(com.hex.kagune.control.king.KingTransform.grow(war, true, burst + 2) == 1.0, "War is all out of him two ticks after it");
        double last = -1.0;
        boolean rising = true;
        for (int t = 0; t <= com.hex.kagune.control.king.KingTransform.PEACE_TICKS; t++) {
            double g = com.hex.kagune.control.king.KingTransform.grow(peace, true, t);
            rising &= g >= last;
            last = g;
        }
        check(rising && last == 1.0, "Peace grows over him steadily, all of it by its end");
        check(!com.hex.kagune.control.king.KingTransform.coming(war, true, com.hex.kagune.control.king.KingTransform.WAR_TICKS)
            && !com.hex.kagune.control.king.KingTransform.coming(peace, true, com.hex.kagune.control.king.KingTransform.PEACE_TICKS)
            && !com.hex.kagune.control.king.KingTransform.coming(war, false, 3.0), "each is done coming in its time (and taking it off is not coming)");
        check(com.hex.kagune.control.king.KingTransform.grow(war, false, 0.0) == 1.0 && com.hex.kagune.control.king.KingTransform.grow(war, false, 100.0) == 0.0,
            "taken off, it draws back into him as before");
        double[] warOut = com.hex.kagune.control.king.KingTransform.growth(war, true, burst + 3);
        check(warOut != null && warOut[0] >= com.hex.kagune.control.king.KingTransform.REACH && warOut[1] > 0.1, "War's burst reaches all of him, swollen past itself");
        double[] peaceEnd = com.hex.kagune.control.king.KingTransform.growth(peace, true, com.hex.kagune.control.king.KingTransform.PEACE_ARMOUR_TO);
        check(peaceEnd != null && peaceEnd[0] >= com.hex.kagune.control.king.KingTransform.REACH - 1.0E-9, "Peace's armour has grown over all of him by its end");
        check(com.hex.kagune.control.king.KingTransform.growth(peace, true, com.hex.kagune.control.king.KingTransform.PEACE_TICKS + 1) == null, "after, nothing is held in");
        // The growth: a plate on the chest comes out of the skin as the front passes it, and is all there after.
        List<double[][]> quads = new ArrayList<>();
        com.hex.kagune.control.ayato.UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> quads.add(new double[][] {a, b, c, d});
        double[] plate = {0.5, 5.0, -4.0}; // torso pixels: 2 px out in front of the chest
        com.hex.kagune.control.king.KingGrowth.wrap(sink, WarModel.Part.TORSO, new double[] {0.0, 0.0, 0.0}).quad(plate, plate, plate, plate, new double[] {0, 0, -1}, 0, 0, 0, 0);
        check(quads.isEmpty(), "before the growth reaches it, nothing of a plate is drawn");
        com.hex.kagune.control.king.KingGrowth.wrap(sink, WarModel.Part.TORSO, new double[] {40.0, 0.0, 0.0}).quad(plate, plate, plate, plate, new double[] {0, 0, -1}, 0, 0, 0, 0);
        check(quads.size() == 1 && Math.abs(quads.get(0)[0][2] - plate[2]) < 1.0E-9, "once the growth is past it, a plate is all where it is");
        quads.clear();
        com.hex.kagune.control.king.KingGrowth.wrap(sink, WarModel.Part.TORSO, new double[] {2.0, 0.0, 0.0}).quad(plate, plate, plate, plate, new double[] {0, 0, -1}, 0, 0, 0, 0);
        check(quads.size() == 1 && quads.get(0)[0][2] > plate[2] && quads.get(0)[0][2] < -2.0, "the growth reaching it, a plate is part way out of the skin");
        // What each leaves in the world: finite at every moment.
        boolean finite = true;
        int drawn = 0;
        for (int t = 0; t <= com.hex.kagune.control.king.KingComingFx.WAR_LASTS; t += 2) {
            double[] count = {0};
            boolean[] ok = {true};
            com.hex.kagune.control.ayato.UkakuModel.Sink check = (a, b, c, d, n, ca, cb, cc, cd) -> {
                count[0]++;
                for (double[] p : new double[][] {a, b, c, d, n}) {
                    ok[0] &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
                }
            };
            com.hex.kagune.control.king.KingComingFx.war(check, new double[] {0, 64, 0}, t, 99L, new double[] {3, 66, 4}, t);
            com.hex.kagune.control.king.KingComingFx.peace(check, new double[] {0, 64, 0}, t, 99L, new double[] {3, 66, 4}, t);
            finite &= ok[0];
            drawn += (int)count[0];
        }
        check(finite && drawn > 1000, "the comings' shapes are finite at every moment (" + drawn + " quads)");
        // Peace's blades unfolding as wings: never through him, finite throughout.
        boolean clear = true;
        for (double t = 0.0; t <= com.hex.kagune.control.king.KingTransform.PEACE_TICKS; t += 0.5) {
            for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
                PeaceMotion.Tip tip = PeaceMotion.tip(blade, 100.0 + t, 1.0, new PeaceMotion.State(KingMove.EMERGE, 0, t, 0.0, null, KingMove.NONE, 0, 0.0, 0.0, 0.0));
                clear &= Double.isFinite(tip.at()[0]) && Double.isFinite(tip.at()[1]) && Double.isFinite(tip.at()[2]);
                double[] heart = tip.at();
                clear &= !(Math.abs(heart[0]) < 9.6 && heart[1] > 0.0 && heart[1] < 34.0 && heart[2] > -4.6 && heart[2] < 4.0);
            }
        }
        check(clear, "unfolding as wings, Peace's blades are never inside him");
        // 1.0.19: leaving him.
        for (KingForm form : new KingForm[] {war, peace}) {
            int leave = com.hex.kagune.control.king.KingTransform.leave(form);
            check(com.hex.kagune.control.king.KingTransform.grow(form, false, 0.0) == 1.0 && com.hex.kagune.control.king.KingTransform.grow(form, false, leave - 1) == 1.0
                && com.hex.kagune.control.king.KingTransform.grow(form, false, leave) == 0.0 && com.hex.kagune.control.king.KingTransform.going(form, false, 1.0)
                && !com.hex.kagune.control.king.KingTransform.going(form, true, 1.0), form + " stays whole while it leaves him its own way, then is gone");
        }
        check(com.hex.kagune.control.king.KingShed.gone(com.hex.kagune.control.king.KingTransform.WAR_LEAVE), "all of War has crumbled away by the end of its leaving");
        check(com.hex.kagune.control.king.KingShed.release(new double[] {30.0, 30.0, 20.0}, 0.0) < com.hex.kagune.control.king.KingShed.release(new double[] {2.0, 20.0, -2.0}, 0.0),
            "War's rinkaku crumble before his chest");
        float[] piece = {1, 1, -3, 2, 1, -3, 2, 2, -3, 1, 2, -3, 0, 0, -1, 0xFF202020, 0xFF202020, 0xFF202020, 0xFF202020, 0};
        float[] shed = new float[20];
        boolean whole = com.hex.kagune.control.king.KingShed.shed(piece, WarModel.Part.TORSO, 0.0, shed) && shed[0] == piece[0];
        boolean crumbled = !com.hex.kagune.control.king.KingShed.shed(piece, WarModel.Part.TORSO, com.hex.kagune.control.king.KingTransform.WAR_LEAVE, shed);
        check(whole && crumbled, "a piece of War is on him as it is taken off and gone by the end");
        double[] recede = com.hex.kagune.control.king.KingTransform.growth(peace, false, com.hex.kagune.control.king.KingTransform.PEACE_RECEDE_TO);
        check(recede != null && recede[0] < 1.0E-9, "Peace's armour has receded into his heart by the end");
        boolean sunk = true;
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            PeaceMotion.Tip tip = PeaceMotion.tip(blade, 100.0, 1.0, new PeaceMotion.State(KingMove.RECEDE, 0, com.hex.kagune.control.king.KingTransform.PEACE_LEAVE - 1.0,
                0.0, null));
            sunk &= tip.size() < 0.1 * PeaceMotion.size(blade);
        }
        check(sunk, "Peace's blades have sunk back into him by the end of its leaving");
    }

    /** 1.0.17: War's cloth hangs, trails when he runs, swings when he turns, and never goes through him or stretches like rubber. */
    private static void cloth() {
        double block = 16.0 / 0.9375;
        WarCloth cloth = new WarCloth();
        cloth.reset(clothPose(0.0, 0.0, 0.0, 0.0, false));
        for (int k = 0; k < 240; k++) {
            cloth.step(clothPose(0.0, 0.0, 0.0, 0.0, false), clothPose(0.0, 0.0, 0.0, 0.0, false), 1, WarCloth.Wind.STILL);
        }
        double[] still = clothCheck(cloth, clothPose(0.0, 0.0, 0.0, 0.0, false));
        check(still[0] == 0.0, "the cloth stays finite at rest");
        check(still[1] < 1.12, "at rest no strip is stretched (" + still[1] + ")");
        check(still[2] < 0.05, "at rest it is clear of his body (deepest " + still[2] + " px)");
        check(still[3] * block < 6.5 && still[3] >= -0.01, "the cape hangs to his shins, never through the ground (lowest " + still[3] * block + " px)");
        double restTrail = still[4];
        // Sprinting along his front, legs swinging, for two seconds.
        double speed = 5.6;
        double[] run = null;
        double worstStretch = 0.0;
        double deepest = 0.0;
        for (int tick = 0; tick < 40; tick++) {
            // The stride grows in as the walk animation's does (it eases its speed toward how fast he goes).
            WarCloth.Pose from = clothPose(speed * tick / 20.0, 0.0, tick, Math.min(1.0, tick / 6.0), false);
            WarCloth.Pose to = clothPose(speed * (tick + 1) / 20.0, 0.0, tick + 1, Math.min(1.0, (tick + 1) / 6.0), false);
            cloth.step(from, to, WarCloth.SUBSTEPS_PER_TICK, WarCloth.Wind.STILL);
            run = clothCheck(cloth, to);
            worstStretch = Math.max(worstStretch, run[1]);
            deepest = Math.max(deepest, run[2]);
        }
        check(run[0] == 0.0, "the cloth stays finite while he runs");
        check(worstStretch < 1.3, "running, no strip stretches like rubber (" + worstStretch + ")");
        check(deepest < 0.6, "running, no cloth goes through his legs or body (deepest " + deepest + " px)");
        check((run[4] - restTrail) * block > 2.5, "running, the cape trails out behind him (" + (run[4] - restTrail) * block + " px more)");
        // Spun round half a turn in five ticks, then still.
        double spinWorst = 0.0;
        double x = speed * 2.0;
        for (int tick = 0; tick < 30; tick++) {
            double yawFrom = Math.PI * Math.min(1.0, tick / 5.0);
            double yawTo = Math.PI * Math.min(1.0, (tick + 1) / 5.0);
            cloth.step(clothPose(x, yawFrom, 40 + tick, 0.0, false), clothPose(x, yawTo, 41 + tick, 0.0, false), WarCloth.SUBSTEPS_PER_TICK,
                WarCloth.Wind.breeze(tick));
            double[] c = clothCheck(cloth, clothPose(x, yawTo, 41 + tick, 0.0, false));
            spinWorst = Math.max(spinWorst, c[1]);
            check(c[0] == 0.0 || tick > 0, "the cloth stays finite through a turn");
        }
        check(spinWorst < 1.3, "spun round, it swings without tearing loose (" + spinWorst + ")");
        // Crouched: the torso leans in and the cloth falls with it.
        for (int k = 0; k < 120; k++) {
            cloth.step(clothPose(x, Math.PI, 70, 0.0, true), clothPose(x, Math.PI, 70, 0.0, true), 1, WarCloth.Wind.STILL);
        }
        double[] crouch = clothCheck(cloth, clothPose(x, Math.PI, 70, 0.0, true));
        check(crouch[0] == 0.0 && crouch[2] < 0.05, "crouched, the cloth is clear of him (deepest " + crouch[2] + " px)");
    }

    /**
     * A rigid body for the cloth: feet {@code forward} blocks along +z (his front at yaw 0), turned {@code yaw}, legs and arms
     * swinging with {@code tick} as far as {@code stride} (0 still, 1 a sprint); {@code crouch}ed or not.
     */
    private static WarCloth.Pose clothPose(double forward, double yaw, double tick, double stride, boolean crouch) {
        double block = 16.0 / 0.9375;
        double[] feet = {0.0, 0.0, forward};
        double swing = Math.cos(tick * 0.6662) * 1.0 * stride; // a sprint's stride, as the walk animation swings the legs
        double lean = crouch ? 0.5 : 0.0;
        return new WarCloth.Pose(
            clothPart(feet, yaw, 0.0, crouch ? 3.2 : 0.0, 0.0, lean, block),
            clothPart(feet, yaw, -5.0, 2.0 + (crouch ? 3.2 : 0.0), 0.0, -swing * 0.8 + lean * 0.8, block),
            clothPart(feet, yaw, 5.0, 2.0 + (crouch ? 3.2 : 0.0), 0.0, swing * 0.8 + lean * 0.8, block),
            clothPart(feet, yaw, -1.9, crouch ? 12.2 : 12.0, crouch ? 4.0 : 0.1, swing, block),
            clothPart(feet, yaw, 1.9, crouch ? 12.2 : 12.0, crouch ? 4.0 : 0.1, -swing, block),
            0.0, 1.0 / block);
    }

    /** A model part (pivot in model pixels, turned {@code xRot} as ModelPart does) out in the world on feet {@code feet}, facing {@code yaw}. */
    private static WarCloth.Frame clothPart(double[] feet, double yaw, double pivotX, double pivotY, double pivotZ, double xRot, double block) {
        double[] left = {Math.cos(yaw), 0.0, Math.sin(yaw)};
        double[] forward = {-Math.sin(yaw), 0.0, Math.cos(yaw)};
        double cos = Math.cos(xRot);
        double sin = Math.sin(xRot);
        return (px, py, pz) -> {
            double mx = pivotX + px;
            double my = pivotY + py * cos - pz * sin;
            double mz = pivotZ + py * sin + pz * cos;
            double up = 24.0 - my;
            return new double[] {feet[0] + (left[0] * mx - forward[0] * mz) / block, feet[1] + up / block, feet[2] + (left[2] * mx - forward[2] * mz) / block};
        };
    }

    /** {non-finite particles, worst stretch, deepest inside (px), lowest point above the ground (blocks), how far the lowest cape row is behind his feet (blocks)}. */
    private static double[] clothCheck(WarCloth cloth, WarCloth.Pose pose) {
        double[][] points = cloth.points();
        double bad = 0.0;
        double stretch = 0.0;
        double inside = 0.0;
        double lowest = Double.MAX_VALUE;
        double[] feet = pose.leftLeg().at(0.0, 12.0, 0.0);
        double[] back = WarClothProbe.back(pose);
        double trail = -Double.MAX_VALUE;
        for (int i = 0; i < points.length; i++) {
            double[] p = points[i];
            if (!Double.isFinite(p[0]) || !Double.isFinite(p[1]) || !Double.isFinite(p[2])) {
                bad++;
                continue;
            }
            stretch = Math.max(stretch, cloth.stretch(i, pose.scale()));
            if (!cloth.pinned(i)) {
                inside = Math.max(inside, WarCloth.inside(pose, p, cloth.garment(i)));
            }
            lowest = Math.min(lowest, p[1] - pose.ground());
            if (cloth.garment(i) == WarCloth.CAPE) {
                trail = Math.max(trail, (p[0] - feet[0]) * back[0] + (p[2] - feet[2]) * back[2]);
            }
        }
        return new double[] {bad, stretch, inside, lowest, trail};
    }

    /** Which way is behind him on {@code pose} (horizontal, unit). */
    private static final class WarClothProbe {
        static double[] back(WarCloth.Pose pose) {
            double[] a = pose.torso().at(0.0, 6.0, 0.0);
            double[] b = pose.torso().at(0.0, 6.0, 4.0);
            double dx = b[0] - a[0];
            double dz = b[2] - a[2];
            double l = Math.hypot(dx, dz);
            return new double[] {dx / l, 0.0, dz / l};
        }
    }

    /** The nearest any cutting blade of {@code move} comes to a body's middle {@code side} blocks to his left and {@code ahead} before him. */
    private static double peaceReach(KingMove move, int level, double side, double ahead) {
        double k = 1.0 / PeaceMotion.BLOCK;
        double[] c = {side, 0.9, ahead};
        double best = Double.MAX_VALUE;
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            double[] was = null;
            for (int age = 0; age <= Math.max(move.ticks(level), 60); age++) {
                if (!PeaceMotion.cutting(blade, move, level, age)) {
                    was = null;
                    continue;
                }
                PeaceMotion.Tip tip = PeaceMotion.clear(blade, PeaceMotion.pose(blade, 1000.0 + age, move, level, age, 0.0, 0.0, 0.0));
                double[] heart = {tip.at()[0] * k, tip.at()[1] * k, -tip.at()[2] * k};
                double[] p = PeaceMotion.point(tip, blade);
                double[] point = {p[0] * k, p[1] * k, -p[2] * k};
                best = Math.min(best, segment(c, heart, point));
                if (was != null) best = Math.min(best, segment(c, was, point));
                if (move == KingMove.PIERCE) {
                    double[] r = PeaceMotion.root(blade);
                    best = Math.min(best, segment(c, new double[] {r[0] * k, r[1] * k, -r[2] * k}, heart));
                }
                was = point;
            }
        }
        return best;
    }

    private static double segment(double[] c, double[] a, double[] b) {
        double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double l = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        double t = l < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((c[0] - a[0]) * ab[0] + (c[1] - a[1]) * ab[1] + (c[2] - a[2]) * ab[2]) / l));
        double x = a[0] + ab[0] * t - c[0], y = a[1] + ab[1] * t - c[1], z = a[2] + ab[2] * t - c[2];
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** How far {@code q} (body pixels) with {@code r} round it is inside his body (0: clear): his torso, arms, legs and head. */
    private static double inside(double[] q, double r) {
        double best = 0.0;
        // Torso with the arms beside it, the legs under it, the head over it (body pixels: x left, y up, z back).
        double[][] boxes = {{-8.6, 8.6, 12.0, 24.5, -2.6, 2.6}, {-4.2, 4.2, 0.0, 12.0, -2.4, 2.4}, {-4.3, 4.3, 24.0, 32.3, -4.3, 4.3}};
        for (double[] b : boxes) {
            double dx = Math.min(q[0] - b[0], b[1] - q[0]);
            double dy = Math.min(q[1] - b[2], b[3] - q[1]);
            double dz = Math.min(q[2] - b[4], b[5] - q[2]);
            double depth = Math.min(dx, Math.min(dy, dz)) + r;
            best = Math.max(best, depth);
        }
        return best;
    }

    /** 1.0.20: the fighting grammar: what an M1 and a heavy are, by what came before and how they were clicked. */
    private static void combo() {
        int jump = com.hex.kagune.control.king.KingCombo.JUMP;
        check(com.hex.kagune.control.king.KingCombo.blow(0, 0, jump, false) == com.hex.kagune.control.king.KingCombo.Blow.CHAIN,
            "jump held on a first blow is no launcher (it needs a chain before it)");
        check(com.hex.kagune.control.king.KingCombo.blow(2, 0, jump, false) == com.hex.kagune.control.king.KingCombo.Blow.LAUNCH,
            "jump held after two chained blows launches");
        check(com.hex.kagune.control.king.KingCombo.blow(4, 0, 0, false) == com.hex.kagune.control.king.KingCombo.Blow.CHAIN, "without jump the chain goes on");
        check(com.hex.kagune.control.king.KingCombo.blow(0, 0, 0, true) == com.hex.kagune.control.king.KingCombo.Blow.AIR
            && com.hex.kagune.control.king.KingCombo.blow(0, com.hex.kagune.control.king.KingCombo.AIR_HITS, 0, true) == com.hex.kagune.control.king.KingCombo.Blow.SPIKE,
            "in the air: air blows, then the spike");
        check(com.hex.kagune.control.king.KingCombo.heavy(true, true, 0, 99, 0) == com.hex.kagune.control.king.KingCombo.Heavy.SPIKE, "the heavy in the air spikes");
        check(com.hex.kagune.control.king.KingCombo.heavy(true, false, 3, 5, 0) == com.hex.kagune.control.king.KingCombo.Heavy.ENDER, "the heavy right after a chain ends it");
        check(com.hex.kagune.control.king.KingCombo.heavy(true, false, 3, 40, 0) == com.hex.kagune.control.king.KingCombo.Heavy.CHARGING
            && com.hex.kagune.control.king.KingCombo.heavy(true, false, 0, 0, 0) == com.hex.kagune.control.king.KingCombo.Heavy.CHARGING,
            "a heavy on its own charges");
        check(com.hex.kagune.control.king.KingCombo.heavy(false, false, 0, -1, 3) == com.hex.kagune.control.king.KingCombo.Heavy.TAP
            && com.hex.kagune.control.king.KingCombo.heavy(false, false, 0, -1, com.hex.kagune.control.king.KingCombo.CHARGE_TICKS)
                == com.hex.kagune.control.king.KingCombo.Heavy.CHARGED, "let go quick it is the tap; held long enough, the charged one");
        check(com.hex.kagune.control.king.KingCombo.ender(4) > com.hex.kagune.control.king.KingCombo.ender(1), "the ender is worth more for a longer chain");
        check(com.hex.kagune.control.king.WarStrike.air(0) != com.hex.kagune.control.king.WarStrike.DOWNFALL
            && com.hex.kagune.control.king.WarStrike.air(com.hex.kagune.control.king.KingCombo.AIR_HITS) == com.hex.kagune.control.king.WarStrike.DOWNFALL
            && com.hex.kagune.control.king.PeaceStrike.air(com.hex.kagune.control.king.KingCombo.AIR_HITS) == com.hex.kagune.control.king.PeaceStrike.JUDGMENT,
            "each form's air blows end in its spike");
        check(com.hex.kagune.control.king.WarStrike.next(com.hex.kagune.control.king.WarStrike.ASCENSION) == com.hex.kagune.control.king.WarStrike.JAB
            && com.hex.kagune.control.king.PeaceStrike.next(com.hex.kagune.control.king.PeaceStrike.HALO) == com.hex.kagune.control.king.PeaceStrike.LANCE,
            "after a grammar blow the chain begins again");
        check(com.hex.kagune.control.king.WarStrike.CLEAVER.breaksGuard() && com.hex.kagune.control.king.PeaceStrike.CROSS_CLEAVE.breaksGuard()
            && !com.hex.kagune.control.king.WarStrike.JAB.breaksGuard(), "the heavies break a guard, the light blows do not");
        check(com.hex.kagune.control.king.WarStrike.ASCENSION.up > 1.0 && com.hex.kagune.control.king.WarStrike.DOWNFALL.up < -1.0,
            "the launcher throws up, the spike drives down");
        // Peace's Seraph Thrust reaches far down its column.
        check(peaceReach(KingMove.BLADES, com.hex.kagune.control.king.PeaceStrike.SERAPH.ordinal(), 0.0, 5.0) < 1.2, "the Seraph Thrust runs through a body 5 blocks out");
        check(peaceReach(KingMove.BLADES, com.hex.kagune.control.king.PeaceStrike.HALO.ordinal(), 2.0, -1.5) < 1.2
            && peaceReach(KingMove.BLADES, com.hex.kagune.control.king.PeaceStrike.HALO.ordinal(), -2.0, -1.5) < 1.2, "Halo Rend cuts behind him on both sides");
    }

    /** 1.0.14: War's M1: the chain, its timings, what each blow catches, its poses and its shapes. */
    private static void strikes() {
        com.hex.kagune.control.king.WarStrike[] all = com.hex.kagune.control.king.WarStrike.values();
        com.hex.kagune.control.king.WarStrike[] chain = java.util.Arrays.stream(all).filter(b -> !b.special()).toArray(com.hex.kagune.control.king.WarStrike[]::new);
        check(chain.length == 6 && chain[chain.length - 1].finisher(), "six blows, the Verdict last");
        com.hex.kagune.control.king.WarStrike blow = null;
        for (int i = 0; i < chain.length; i++) {
            blow = com.hex.kagune.control.king.WarStrike.next(blow);
            check(blow == chain[i], "the chain runs in order: " + chain[i]);
        }
        check(com.hex.kagune.control.king.WarStrike.next(blow) == com.hex.kagune.control.king.WarStrike.JAB, "after the Verdict, the jab");
        check(com.hex.kagune.control.king.WarStrike.after(com.hex.kagune.control.king.WarStrike.REAP, 5) == com.hex.kagune.control.king.WarStrike.SIDE_KICK,
            "a click soon after carries the chain on");
        check(com.hex.kagune.control.king.WarStrike.after(com.hex.kagune.control.king.WarStrike.REAP, com.hex.kagune.control.king.WarStrike.CHAIN_WINDOW + 1)
            == com.hex.kagune.control.king.WarStrike.JAB, "a click later starts over");
        check(com.hex.kagune.control.king.WarStrike.after(null, 0) == com.hex.kagune.control.king.WarStrike.JAB, "the first blow is the jab");
        for (com.hex.kagune.control.king.WarStrike b : all) {
            check(b.impact >= 2 && b.impact < b.chain && b.chain <= b.duration, b + ": wind-up, impact, chain, end in order");
            check(b.hit() > 0.0F && b.hit() <= (b.special() ? 16.0F : 15.0F), b + ": damage in bounds: " + b.hit());
            check(b.reach > 3.0 && b.reach < (b.special() ? 6.0 : 5.0), b + ": reach");
            check(b.stand(0.6) < b.reach, b + ": he stops stepping in within his reach");
            check(com.hex.kagune.control.king.KingMove.STRIKE.ticks(b.ordinal()) == b.duration, b + ": the body takes as long as the blow");
        }
        check(com.hex.kagune.control.king.WarStrike.VERDICT.hit() > com.hex.kagune.control.king.WarStrike.HEEL.hit(), "the Verdict hits hardest");
        check(com.hex.kagune.control.king.WarStrike.JAB.catches(1.0) && !com.hex.kagune.control.king.WarStrike.JAB.catches(0.3),
            "the jab catches what is in front, not beside");
        check(com.hex.kagune.control.king.WarStrike.REAP.catches(0.3) && !com.hex.kagune.control.king.WarStrike.REAP.catches(-0.2),
            "the reap's arc is wide, not behind");
        check(com.hex.kagune.control.king.WarStrike.HEEL.catches(-1.0), "the heel catches all round");
        check(com.hex.kagune.control.king.WarStrike.REAP.sideways < 0.0 && com.hex.kagune.control.king.WarStrike.RISING.up > 0.5,
            "the reap throws them to his left; the rising slash lifts them");
        // The poses: finite and bounded through every blow; the blow's hit is reached on its impact tick; let go by the end.
        boolean bounded = true;
        for (com.hex.kagune.control.king.WarStrike b : all) {
            for (double age = 0.0; age <= b.duration; age += 0.25) {
                RigPose p = new RigPose();
                WarPose.act(p, com.hex.kagune.control.king.KingMove.STRIKE, age, b.ordinal(), 1.0F);
                float[] values = {p.rightArmX, p.rightArmY, p.leftArmX, p.leftLegZ, p.rightLegZ, p.twist, p.lean, p.drop, p.spin, p.roll};
                for (float v : values) {
                    bounded &= Float.isFinite(v) && Math.abs(v) < 7.0F;
                }
            }
            RigPose end = new RigPose();
            WarPose.act(end, com.hex.kagune.control.king.KingMove.STRIKE, b.duration, b.ordinal(), 1.0F);
            double spin = Math.IEEEremainder(end.spin, Math.PI * 2.0);
            check(Math.abs(end.rightArmX) < 1.0E-3 && Math.abs(end.leftLegZ) < 1.0E-3 && Math.abs(end.lean) < 1.0E-3 && Math.abs(spin) < 1.0E-3,
                b + ": let go by its end");
        }
        check(bounded, "every blow's pose stays bounded and finite");
        RigPose jab = new RigPose();
        WarPose.act(jab, com.hex.kagune.control.king.KingMove.STRIKE, com.hex.kagune.control.king.WarStrike.JAB.impact, 0, 1.0F);
        check(jab.leftArmX < -1.4F, "the jab's fist is out on its impact tick: " + jab.leftArmX);
        // 1.0.15: in first person, the jab's fist is out in front of his eyes, where he looks (the arms drawn on the pose third person shows).
        double[] shift = com.hex.kagune.control.client.animation.EtoOwnLeg.shift(jab);
        double[] fist = com.hex.kagune.control.client.animation.BodyFrame.world(jab,
            com.hex.kagune.control.client.animation.BodyFrame.model(jab, "leftArm", new double[] {1.0, 10.0, 0.0}));
        double[] eyes = com.hex.kagune.control.client.animation.BodyFrame.world(new RigPose(),
            com.hex.kagune.control.client.animation.BodyFrame.model(new RigPose(), "head", new double[] {0.0, -4.0, 0.0}));
        double[] seen = com.hex.kagune.control.client.animation.FirstPersonArms.adjust(new double[] {fist[0] + shift[0], fist[1] + shift[1],
            fist[2] + shift[2]}, eyes, 0.0);
        double ahead = seen[2] - eyes[2];
        double across = seen[0] - eyes[0];
        double below = eyes[1] - seen[1];
        // In view: within about 35 degrees across and 30 down of the line of the eyes.
        check(ahead > 0.4 && Math.abs(across) / ahead < 0.7 && below / ahead < 0.58 && below > -0.1,
            "first person: the jab's fist is out ahead of his eyes, in view: ahead " + ahead + ", across " + across + ", below " + below);
        double[] down = com.hex.kagune.control.client.animation.FirstPersonArms.adjust(new double[] {eyes[0], eyes[1], eyes[2] + 1.0}, eyes, 0.6);
        check(down[1] < eyes[1] - 0.4 && down[2] > 0.6 + eyes[2], "first person: the arms turn down with the look");
        RigPose kick = new RigPose();
        WarPose.act(kick, com.hex.kagune.control.king.KingMove.STRIKE, com.hex.kagune.control.king.WarStrike.SIDE_KICK.impact, 2, 1.0F);
        check(kick.rightLegZ > 0.9F && kick.spin > 1.0F, "the side kick's foot is out, side-on, on its impact tick");
        RigPose verdict = new RigPose();
        WarPose.act(verdict, com.hex.kagune.control.king.KingMove.STRIKE, 4.0, 5, 1.0F);
        check(verdict.rightArmX < -2.0F, "the Verdict's blade is high overhead in its wind-up: " + verdict.rightArmX);
        // A chain flows: the blow cut off is let go, not dropped, as the next winds up.
        RigPose cut = new RigPose();
        WarPose.strikes(cut, 1, 0.0, 0, com.hex.kagune.control.king.WarStrike.JAB.chain, 1.0F);
        check(cut.leftArmX < -1.0F, "the next blow takes over from where the jab was: " + cut.leftArmX);
        RigPose heel = new RigPose();
        WarPose.strikes(heel, 5, 1.0, 4, com.hex.kagune.control.king.WarStrike.HEEL.chain, 1.0F);
        check(Math.abs(heel.spin) < 1.0F, "the heel's turn is never unwound handing off: " + heel.spin);
        // The shapes: the arcs and shocks are drawn, finite.
        List<double[]> quads = new ArrayList<>();
        com.hex.kagune.control.ayato.UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            quads.add(a);
            quads.add(c);
        };
        double[] chest = {0.0, 65.0, 0.0};
        double[] facing = {0.0, 0.0, 1.0};
        for (com.hex.kagune.control.king.WarStrike b : all) {
            quads.clear();
            for (double age = 0.0; age < b.duration; age += 1.0) {
                com.hex.kagune.control.king.KingFxModel.swing(sink, b, chest, facing, age);
                com.hex.kagune.control.king.KingFxModel.impact(sink, b, new double[] {0.0, 65.0, 2.0}, facing, age);
            }
            check(!quads.isEmpty(), b + ": it has a shape out in the world");
            boolean finite = true;
            for (double[] q : quads) {
                finite &= Double.isFinite(q[0]) && Double.isFinite(q[1]) && Double.isFinite(q[2]) && Math.abs(q[0]) < 10.0 && Math.abs(q[2]) < 10.0;
            }
            check(finite, b + ": its shapes are finite and near him");
        }
        quads.clear();
        com.hex.kagune.control.king.KingFxModel.verdict(sink, new double[] {0.0, 64.0, 3.0}, facing, 4.0, 10.0);
        check(quads.size() > 40, "the Verdict's rinkaku come up out of the ground");
    }

    private static void abilities() {
        check(com.hex.kagune.control.king.WarRules.Ability.values().length == 6, "six abilities");
        String[] names = {"King's Execution", "Black Reaper", "Centipede Dominion", "Spire Wave", "King's Defiance", "Black Sun"};
        String[] keys = {"Z", "J", "G", "B", "C", "V"};
        for (com.hex.kagune.control.king.WarRules.Ability a : com.hex.kagune.control.king.WarRules.Ability.values()) {
            check(a.displayName().equals(names[a.ordinal()]) && a.key().equals(keys[a.ordinal()]), "1.0.13's " + a.displayName() + " on " + a.key());
            check(a.cooldown() > 0 && com.hex.kagune.control.king.WarRules.Ability.bySlot(a.ordinal()) == a, "slot round trip " + a);
        }
        check(com.hex.kagune.control.king.WarRules.Ability.bySlot(6) == null && com.hex.kagune.control.king.WarRules.Ability.bySlot(-1) == null, "no seventh");
        check(com.hex.kagune.control.king.WarRules.Ability.SUN.cooldown() >= 1200, "the Black Sun waits the longest");
        // King's Execution, Black Reaper, Spire Wave: the numbers the spec asked for.
        check(com.hex.kagune.control.king.WarRules.GRASP_RANGE == 15.0 && com.hex.kagune.control.king.WarRules.GRASP_ARMS >= 4
            && com.hex.kagune.control.king.WarRules.GRASP_ARMS <= 5, "Z: four or five centipedes, fifteen blocks");
        check(com.hex.kagune.control.king.WarRules.GRASP_REACH < com.hex.kagune.control.king.WarRules.GRASP_COIL
            && com.hex.kagune.control.king.WarRules.GRASP_COIL < com.hex.kagune.control.king.WarRules.GRASP_CRUSH
            && com.hex.kagune.control.king.WarRules.GRASP_CRUSH < com.hex.kagune.control.king.WarRules.GRASP_TICKS, "Z: it reaches, coils, crushes, in that order");
        check(com.hex.kagune.control.king.WarRules.DASH_RANGE == 10.0, "J: ten blocks");
        check(com.hex.kagune.control.king.WarRules.SPIRE_LENGTH == 20, "B: twenty blocks ahead");
        check(com.hex.kagune.control.king.WarRules.SUN_RANGE == 40.0 && com.hex.kagune.control.king.WarRules.SUN_RADIUS == 12.0
            && Math.abs(com.hex.kagune.control.king.WarRules.SUN_EXECUTE - 0.3) < 1.0E-9, "V: set forty blocks off, seizes twelve round, kills under 30 %");
        check(com.hex.kagune.control.king.WarRules.inSweep(0.0, 3.0, 0.0, 1.0, 5.0, com.hex.kagune.control.king.WarRules.CLEAVE_ARC), "in front is cleaved");
        check(!com.hex.kagune.control.king.WarRules.inSweep(0.0, -3.0, 0.0, 1.0, 5.0, com.hex.kagune.control.king.WarRules.CLEAVE_ARC), "behind is not");
        check(!com.hex.kagune.control.king.WarRules.inSweep(0.0, 6.0, 0.0, 1.0, 5.0, com.hex.kagune.control.king.WarRules.CLEAVE_ARC), "out of reach is not");
        check(com.hex.kagune.control.king.WarRules.inSweep(0.2, -0.3, 0.0, 1.0, 5.0, com.hex.kagune.control.king.WarRules.CLEAVE_ARC), "pressed against him is");
        check(com.hex.kagune.control.king.WarRules.executes(5.9F, 20.0F) && !com.hex.kagune.control.king.WarRules.executes(6.1F, 20.0F)
            && !com.hex.kagune.control.king.WarRules.executes(10.0F, 1000.0F), "the slam kills at 30 % and below, never what is too big");
        check(com.hex.kagune.control.king.WarRules.burst(0.0) == com.hex.kagune.control.king.WarRules.BURST_BASE
            && com.hex.kagune.control.king.WarRules.burst(10.0) > com.hex.kagune.control.king.WarRules.burst(2.0)
            && com.hex.kagune.control.king.WarRules.burst(1.0E6) == com.hex.kagune.control.king.WarRules.BURST_CAP, "C: the cocoon bursts with what it took, to a cap");
        double turned = com.hex.kagune.control.king.WarRules.turn(0.0, Math.PI);
        check(Math.abs(Math.abs(turned) - com.hex.kagune.control.king.WarRules.WYRM_TURN) < 1.0E-9
            && Math.abs(com.hex.kagune.control.king.WarRules.turn(0.0, 0.05) - 0.05) < 1.0E-9, "G: the small Dragon turns toward the aim, no faster than it can");
        // 1.0.15: the Black Sun lifts what it seized from the ground, holds it high, slams it down.
        check(com.hex.kagune.control.king.WarRules.lift(0.0) == 0.0 && com.hex.kagune.control.king.WarRules.lift(com.hex.kagune.control.king.WarRules.SUN_OPEN) == 0.0,
            "V: nothing lifted before the centipedes come up");
        check(com.hex.kagune.control.king.WarRules.lift((com.hex.kagune.control.king.WarRules.SUN_LIFT + com.hex.kagune.control.king.WarRules.SUN_HANG) / 2.0) == 1.0,
            "V: held at the top");
        check(com.hex.kagune.control.king.WarRules.lift(com.hex.kagune.control.king.WarRules.SUN_SLAM) == 0.0
            && com.hex.kagune.control.king.WarRules.lift(com.hex.kagune.control.king.WarRules.SUN_TICKS) == 0.0, "V: slammed down into the ground");
        boolean climbs = true;
        boolean falls = true;
        for (double a = com.hex.kagune.control.king.WarRules.SUN_OPEN; a < com.hex.kagune.control.king.WarRules.SUN_LIFT; a += 0.5) {
            climbs &= com.hex.kagune.control.king.WarRules.lift(a + 0.5) >= com.hex.kagune.control.king.WarRules.lift(a);
        }
        double fallFirst = 0.0;
        double fallLast = 0.0;
        for (double a = com.hex.kagune.control.king.WarRules.SUN_HANG; a < com.hex.kagune.control.king.WarRules.SUN_SLAM; a += 0.5) {
            double drop = com.hex.kagune.control.king.WarRules.lift(a) - com.hex.kagune.control.king.WarRules.lift(a + 0.5);
            falls &= drop >= 0.0;
            if (a == com.hex.kagune.control.king.WarRules.SUN_HANG) {
                fallFirst = drop;
            }
            fallLast = drop;
        }
        check(climbs && falls && fallLast > fallFirst, "V: it rises steadily and comes down faster and faster");
        List<double[]> seizeQuads = new ArrayList<>();
        double[][] coil = com.hex.kagune.control.king.KingFxModel.seize(new double[] {1.0, 64.0, 0.0}, new double[] {0.0, 80.9, 0.0}, 0.3, 1.8, 1.0, 5.0, 3);
        check(coil.length > 8 && coil[0][1] < 64.0 && Math.abs(coil[coil.length - 1][1] - 80.9) < 1.6, "V: each centipede comes up out of the ground to coil round what it holds");
        double[][] half = com.hex.kagune.control.king.KingFxModel.seize(new double[] {1.0, 64.0, 0.0}, new double[] {0.0, 80.9, 0.0}, 0.3, 1.8, 0.3, 5.0, 3);
        check(half[half.length - 1][1] < 75.0, "V: and its point leads it up");
        check(Boolean.TRUE.equals(com.hex.kagune.control.king.WarRules.lashes(3.0)) && Boolean.FALSE.equals(com.hex.kagune.control.king.WarRules.lashes(12.0))
            && com.hex.kagune.control.king.WarRules.lashes(30.0) == null, "the instinct lashes near, shoots far, leaves the rest");
        for (int i = 0; i < 6; i++) {
            double[] tip = WarModel.tendrilTip(i);
            check(Math.signum(tip[0]) == (i % 2 == 0 ? -1.0 : 1.0) && Math.abs(tip[0]) > 10.0, "rinkaku " + i + "'s point is out on its side");
        }
        for (com.hex.kagune.control.king.KingMove move : com.hex.kagune.control.king.KingMove.values()) {
            check(com.hex.kagune.control.king.KingMove.byOrdinal(move.ordinal()) == move, "move round trip " + move);
            double end = move.progress(move.ticks(0) + 5);
            check(move.ticks(0) == 0 ? end == 0.0 : end == 1.0, "a move's progress ends where it should: " + move);
            RigPose p = new RigPose();
            WarPose.act(p, move, move.ticks() * 0.4 + 3.0, 0, 1.0F);
            check(Float.isFinite(p.rightArmX) && Float.isFinite(p.spin) && Math.abs(p.rightArmX) < 4.0F && Math.abs(p.leftArmX) < 4.0F,
                "War's body in " + move + " is sane");
            WarModel.Motion m = WarModel.Motion.of(move, 0.5);
            check(m.reach() >= 0.0 && m.reach() <= 1.0 && m.spread() >= 0.0 && m.spread() <= 1.0 && m.cocoon() >= 0.0 && m.cocoon() <= 1.0,
                "the rinkaku in " + move + " stay in range");
        }
        RigPose grasp = new RigPose();
        WarPose.act(grasp, com.hex.kagune.control.king.KingMove.GRASP, 10.0, 0, 1.0F);
        check(grasp.leftArmX < -1.2F, "Z: the free hand thrown out in front");
        RigPose cleave = new RigPose();
        WarPose.act(cleave, com.hex.kagune.control.king.KingMove.CLEAVE, 3.4, 0, 1.0F);
        RigPose landed = new RigPose();
        WarPose.act(landed, com.hex.kagune.control.king.KingMove.CLEAVE, com.hex.kagune.control.king.WarRules.CLEAVE_LANDS + 0.5, 0, 1.0F);
        check(cleave.rightArmX < -2.0F && landed.rightArmX > -1.0F && landed.lean > 0.4F, "J: the bladed arm raised high, then brought down with his weight");
        check(WarModel.Motion.of(com.hex.kagune.control.king.KingMove.COCOON, 0.5).cocoon() > 0.99, "C: the rinkaku wrapped all round him");
        check(com.hex.kagune.control.client.AbilityHudView.seconds(30) .equals("1.5s") && com.hex.kagune.control.client.AbilityHudView.seconds(400).equals("20s"),
            "the board's seconds");
        // 1.0.22: War's and Peace's panels are the other forms' card: the bound keys, the six abilities in order, K and R to turn back.
        java.util.Map<AbilityGuide.Key, String> bound = new java.util.EnumMap<>(AbilityGuide.Key.class);
        String[][] boundNames = {{"ATTACK", "LMB"}, {"USE", "RMB"}, {"TOGGLE", "R"}, {"CYCLE", "K"}, {"RAPID", "Ctrl+F"}, {"SLINGSHOT", "J"},
            {"GRAB", "G"}, {"SLAM", "B"}, {"GUARD", "C"}, {"LIFT", "V"}, {"UNLEASH", "LCtrl"}};
        for (String[] name : boundNames) {
            bound.put(AbilityGuide.Key.valueOf(name[0]), name[1]);
        }
        for (boolean peace : new boolean[] {false, true}) {
            AbilityGuide.Panel panel = AbilityGuide.king(bound, peace);
            java.util.List<AbilityGuide.Row> rows = panel.rows();
            int six = com.hex.kagune.control.king.WarRules.Ability.values().length;
            check(rows.size() == AbilityGuide.KING_FIRST + six + 3, (peace ? "Peace" : "War") + ": M1, air, heavy, six abilities, K, R, Ctrl + R");
            check(rows.get(AbilityGuide.KING_FIRST).keys().equals("Ctrl+F") && rows.get(AbilityGuide.KING_FIRST + six - 1).keys().equals("V"),
                "the abilities on the keys bound in Controls, in order");
            check(rows.get(AbilityGuide.KING_FIRST).name().equals(peace ? com.hex.kagune.control.king.PeaceRules.Ability.WALTZ.displayName()
                : com.hex.kagune.control.king.WarRules.Ability.EXECUTION.displayName()), "the first ability is its own");
            // 1.2.0: K turns it back, R to kakugan mode (the eyes kept), Ctrl + R puts it all away.
            check(rows.get(rows.size() - 1).keys().equals("LCtrl+R") && rows.get(rows.size() - 2).keys().equals("R")
                && rows.get(rows.size() - 3).keys().startsWith("K"), "K turns it back, R to kakugan mode, Ctrl + R all away");
            check(rows.stream().noneMatch(AbilityGuide.Row::locked), "nothing locked in a final kakuja");
        }
        // 1.0.24: the kagune rides the torso as drawn: the carry is nothing at rest, and as one transform it is the point-by-point one.
        RigPose rest = new RigPose();
        double[] still = com.hex.kagune.control.client.animation.BodyFrame.carry(rest, "torso");
        double[] identity = {1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0};
        double off = 0.0;
        for (int i = 0; i < 12; i++) {
            off = Math.max(off, Math.abs(still[i] - identity[i]));
        }
        check(off < 1.0E-6, "standing still, nothing carries the kagune off its roots");
        RigPose tumbling = new RigPose();
        tumbling.flip = (float)Math.PI;
        tumbling.lean = 0.4F;
        tumbling.roll = 0.3F;
        tumbling.lunge = 3.0F;
        tumbling.drop = 2.0F;
        double[] carry = com.hex.kagune.control.client.animation.BodyFrame.carry(tumbling, "torso");
        double[] root = {0.1, 0.77, -0.18};
        double[] direct = com.hex.kagune.control.client.animation.BodyFrame.carried(tumbling, "torso", root);
        double[] affine = {carry[0] * root[0] + carry[1] * root[1] + carry[2] * root[2] + carry[9],
            carry[3] * root[0] + carry[4] * root[1] + carry[5] * root[2] + carry[10], carry[6] * root[0] + carry[7] * root[1] + carry[8] * root[2] + carry[11]};
        check(Math.abs(affine[0] - direct[0]) + Math.abs(affine[1] - direct[1]) + Math.abs(affine[2] - direct[2]) < 1.0E-6,
            "the torso's carry as one transform");
        check(Math.abs(direct[0] - root[0]) + Math.abs(direct[1] - root[1]) + Math.abs(direct[2] - root[2]) > 0.3,
            "flipped, leaning and lunging, the kagune's root goes with the hips");
        double[][] tail = {root, {0.1, 1.2, -1.2}, {0.1, 1.6, -2.4}, {0.1, 1.9, -3.6}};
        double[][] ridden = com.hex.kagune.control.client.animation.FurutaPose.ride(tumbling, tail);
        check(Math.abs(ridden[0][1] - direct[1]) < 1.0E-6 && Math.abs(ridden[3][1] - tail[3][1]) < 1.0E-6,
            "the root rides the body, the far end stays where it is");
        // The shapes out in the world: finite, and the centipedes black where the Centipede Form is plum.
        check(com.hex.kagune.control.king.KingFxModel.blacken(0xFF563A5E) != 0xFF563A5E && (com.hex.kagune.control.king.KingFxModel.blacken(0xFF563A5E) & 0xFF) < 0x30,
            "Z: the plum plates go black");
        boolean finite = true;
        int[] count = {0};
        double[] hand = {0.0, 1.3, 0.6};
        double[] target = {1.0, 0.9, 9.0};
        for (int i = 0; i < 5; i++) {
            double[][] path = com.hex.kagune.control.king.KingFxModel.grasp(hand, target, 0.3, 1.8, i, 5, 1.0, 1.0, 0.5, 10.0);
            boolean[] ok = {true};
            com.hex.kagune.control.king.KingFxModel.centipede((a, b, c, d, n, ca, cb, cc, cd) -> {
                count[0]++;
                for (double[] v : new double[][] {a, b, c, d}) {
                    ok[0] &= Double.isFinite(v[0]) && Double.isFinite(v[1]) && Double.isFinite(v[2]) && Math.abs(v[2]) < 20.0;
                }
            }, path, 0.2, i, 0.4, true, 1.0, 0.0);
            finite &= ok[0];
            double end = Math.hypot(path[path.length - 1][0] - target[0], path[path.length - 1][2] - target[2]);
            finite &= end < 1.0;
        }
        check(finite && count[0] > 500, "Z: the centipedes reach it and coil round it: " + count[0] + " quads");
        for (int step = 0; step < com.hex.kagune.control.king.WarRules.SPIRE_LENGTH; step++) {
            check(com.hex.kagune.control.king.KingFxModel.spireAt(step, 0) != null, "B: a spike at every step");
        }
    }

    /** 1.0.8: Peace's shapes: finite, near the body (the wings out to about two and a half blocks), within budget, burning cores. */
    private static void peace() {
        int total = 0;
        boolean finite = true;
        boolean glows = false;
        double widest = 0.0;
        for (double time : new double[] {0.0, 77.0, 5000.25}) {
            int quads = 0;
            for (WarModel.Part part : WarModel.Part.values()) {
                int[] count = {0};
                boolean[] ok = {true};
                boolean[] lit = {false};
                double[] wide = {0.0};
                com.hex.kagune.control.king.PeaceModel.build((a, b, c, d, n, ca, cb, cc, cd) -> {
                    count[0]++;
                    for (double[] v : new double[][] {a, b, c, d, n}) {
                        ok[0] &= Double.isFinite(v[0]) && Double.isFinite(v[1]) && Double.isFinite(v[2]) && Math.abs(v[0]) < 60 && Math.abs(v[1]) < 60
                            && Math.abs(v[2]) < 60;
                    }
                    wide[0] = Math.max(wide[0], Math.abs(a[0]));
                    lit[0] |= n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
                }, part, time, 1.0);
                quads += count[0];
                finite &= ok[0];
                glows |= lit[0];
                widest = Math.max(widest, wide[0]);
            }
            total = Math.max(total, quads);
        }
        check(finite, "Peace's shapes are finite and stay near the body");
        check(glows, "the cross blades' cores burn");
        check(widest > 30.0 && widest < 50.0, "the wings reach about two and a half blocks out: " + widest);
        check(total > 5000 && total < 24000, "Peace's quads within budget: " + total);
        int[] none = {0};
        for (WarModel.Part part : WarModel.Part.values()) {
            com.hex.kagune.control.king.PeaceModel.build((a, b, c, d, n, ca, cb, cc, cd) -> none[0]++, part, 0.0, 0.0);
        }
        check(none[0] == 0, "nothing of Peace before it grows");
        // 1.0.16: every blade is held: its chain runs from its root on his back right to its hardened end, which runs into its heart.
        boolean held = true;
        for (double time : new double[] {0.0, 311.5}) {
            for (int blade = 0; blade < com.hex.kagune.control.king.PeaceMotion.BLADES; blade++) {
                com.hex.kagune.control.king.PeaceMotion.Tip tip = com.hex.kagune.control.king.PeaceMotion.tip(blade, time, 1.0, null);
                double[][] line = com.hex.kagune.control.king.PeaceModel.armLine(blade, tip, time, 1.0);
                double[] root = com.hex.kagune.control.king.PeaceMotion.root(blade);
                double[] first = line[0];
                double[] last = line[line.length - 1];
                double toRoot = Math.sqrt(Math.pow(first[0] - root[0], 2) + Math.pow(first[1] - root[1], 2) + Math.pow(first[2] - root[2], 2));
                double toHeart = Math.sqrt(Math.pow(last[0] - tip.at()[0], 2) + Math.pow(last[1] - tip.at()[1], 2) + Math.pow(last[2] - tip.at()[2], 2));
                held &= toRoot < 1.0E-6 && toHeart < 25.0;
            }
        }
        check(held, "every blade is held by its arm from his back, none floats");
        // 1.0.18: the living kagune is drawn between two builds a tick apart: its shapes keep their count from tick to tick.
        int pairs = 0;
        int changed = 0;
        for (KingMove move : new KingMove[] {KingMove.NONE, KingMove.BLADES, KingMove.WALTZ, KingMove.AEGIS, KingMove.AEGIS_OPEN, KingMove.ASCENT,
            KingMove.DIVE, KingMove.LANDING, KingMove.PIERCE, KingMove.REBIRTH, KingMove.HORIZON}) {
            for (int level = 0; level < (move == KingMove.BLADES ? PeaceStrike.values().length : 1); level++) {
                int last = -1;
                for (int age = 0; age <= Math.max(60, move.ticks(level)); age += 3) {
                    int[] count = {0};
                    com.hex.kagune.control.king.PeaceModel.build((a, b, c, d, n, ca, cb, cc, cd) -> count[0]++, WarModel.Part.TORSO, 100.0 + age, 1.0,
                        new PeaceMotion.State(move, level, age, 0.0, null), false, null, com.hex.kagune.control.king.PeaceModel.KAGUNE);
                    if (last >= 0) {
                        pairs++;
                        changed += last == count[0] ? 0 : 1;
                    }
                    last = count[0];
                }
            }
        }
        check(changed == 0, "Peace's living kagune keeps its shape count tick to tick, so it is drawn smoothly between builds (" + changed + " of " + pairs
            + " changed)");
        check(com.hex.kagune.control.king.PeaceModel.point(0.5) == 1.0 && com.hex.kagune.control.king.PeaceModel.point(1.0) == 0.0
            && com.hex.kagune.control.king.PeaceModel.point(0.9) > 0.0 && com.hex.kagune.control.king.PeaceModel.point(0.9) < 1.0,
            "every band comes to a point, none ends cut off");
        RigPose p = new RigPose();
        WarPose.peace(p, 300.0, 1.0F);
        check(p.rightArmZ > 0.1F && p.leftArmZ < -0.1F && Math.abs(p.headZ) < 1.0E-6F, "Peace stands serene: arms a little open, the head level, no twitch");
    }

    /** 1.0.11: Kaneki turning into the Dragon: its beats, its shapes (finite, near him, within budget), his body flung about by it. */
    private static void turning() {
        int ticks = com.hex.kagune.control.king.TurningModel.PRELUDE_TICKS;
        java.util.function.DoubleUnaryOperator at = tick -> tick / ticks;
        check(com.hex.kagune.control.king.TurningModel.chest(0.0) == 0.0
            && com.hex.kagune.control.king.TurningModel.chest(at.applyAsDouble(com.hex.kagune.control.king.TurningModel.CHEST_RIP + 6)) >= 0.6,
            "the kagune tears out of his chest at once at the rip, most of it in six ticks");
        check(com.hex.kagune.control.king.TurningModel.head(at.applyAsDouble(com.hex.kagune.control.king.TurningModel.HEAD_RIP - 1)) == 0.0
            && com.hex.kagune.control.king.TurningModel.head(1.0) > 0.99, "out of his head only from its rip, all of it by the end");
        boolean screams = true;
        for (int[] scream : com.hex.kagune.control.king.TurningModel.SCREAMS) {
            screams &= com.hex.kagune.control.king.TurningModel.scream(at.applyAsDouble(scream[0] + 8)) > 0.99;
        }
        check(screams && com.hex.kagune.control.king.TurningModel.scream(at.applyAsDouble(5)) == 0.0, "he screams on every scream, and not before");
        int[] last = com.hex.kagune.control.king.TurningModel.BURSTS[com.hex.kagune.control.king.TurningModel.BURSTS.length - 1];
        check(Math.abs(com.hex.kagune.control.king.TurningModel.burst(at.applyAsDouble(last[0])) - 1.0) < 1.0E-9 && last[0] < ticks,
            "the last burst is the hardest, before the Dragon breaks out");
        check(com.hex.kagune.control.king.TurningModel.front(1.0) > 25.0 && com.hex.kagune.control.king.TurningModel.front(0.05) == 0.0,
            "the plates spread from the wound over all of him");
        int total = 0;
        boolean finite = true;
        for (double p : new double[] {0.15, 0.5, 0.95}) {
            int quads = 0;
            for (WarModel.Part part : WarModel.Part.values()) {
                int[] count = {0};
                boolean[] ok = {true};
                com.hex.kagune.control.king.TurningModel.build((a, b, c, d, n, ca, cb, cc, cd) -> {
                    count[0]++;
                    for (double[] v : new double[][] {a, b, c, d, n}) {
                        ok[0] &= Double.isFinite(v[0]) && Double.isFinite(v[1]) && Double.isFinite(v[2]) && Math.abs(v[0]) < 50 && Math.abs(v[1]) < 50
                            && Math.abs(v[2]) < 50;
                    }
                }, part, p * ticks, p);
                quads += count[0];
                finite &= ok[0];
            }
            total = Math.max(total, quads);
        }
        check(finite, "the turning's shapes are finite and stay near him");
        check(total > 1500 && total < 14000, "the turning's quads within budget: " + total);
        int[] none = {0};
        for (WarModel.Part part : WarModel.Part.values()) {
            com.hex.kagune.control.king.TurningModel.build((a, b, c, d, n, ca, cb, cc, cd) -> none[0]++, part, 0.0, 0.0);
        }
        check(none[0] == 0, "nothing of the turning before it starts");
        RigPose rip = new RigPose();
        WarPose.turning(rip, 0.13, 23.4, 1.0, 0.6);
        RigPose head = new RigPose();
        WarPose.turning(head, 0.40, 72.0, 1.0, 0.7);
        check(rip.headX < -0.5 && rip.lean < -0.2 && rip.rightArmZ > 0.8 && rip.leftArmZ < -0.8,
            "as it tears out of his chest he is flung back screaming, arms out");
        check(head.rightArmX < -2.0 && head.leftArmX < -2.0 && head.rightElbow > 1.0, "as it tears out of his head he claws at it");
    }

    /** 1.0.11: War's one eye right on the kakugan as placed, the mask mirrored for a right eye. */
    private static void eyes() {
        double[] usual = WarModel.eyeOn(com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT);
        check(Math.abs(usual[0] - 2.0) < 1.0E-9 && Math.abs(usual[1] - 27.0) < 1.0E-9, "on Steve's eyes War's eye is the left one, rows 4 and 5");
        double[] right = WarModel.eyeOn(com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT.withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT));
        check(Math.abs(right[0] + 2.0) < 1.0E-9 && Math.abs(right[1] - 27.0) < 1.0E-9, "one-eyed on the right, it is on the right eye");
        double[] high = WarModel.eyeOn(new com.hex.kagune.control.kakugan.KakuganEyes(0, 2, 6, 2, 1, true, false));
        check(Math.abs(high[0] - 3.0) < 1.0E-9 && Math.abs(high[1] - 29.5) < 1.0E-9, "wherever the kakugan is placed, there");
        double[][] sums = new double[2][1];
        double[][] eyesAt = {usual, right};
        for (int k = 0; k < 2; k++) {
            int side = k;
            WarModel.build((a, b, c, d, n, ca, cb, cc, cd) -> sums[side][0] += a[0] + b[0] + c[0] + d[0], WarModel.Part.HEAD, 40.0, 1.0, 0.0,
                WarModel.Motion.STILL, eyesAt[k]);
        }
        check(Math.abs(sums[0][0] + sums[1][0]) < 1.0E-6 * Math.max(1.0, Math.abs(sums[0][0])), "on the right eye the mask is its own mirror image");
        check(WarModel.eye(1.0, right)[0] < -1.0 && WarModel.eye(1.0, usual)[0] > 1.0, "its white point (and its bloom) goes with it");
    }

    private static void model() {
        int total = 0;
        boolean finite = true;
        boolean glows = false;
        for (double night : new double[] {0.0, 1.0}) {
            for (double time : new double[] {0.0, 137.0, 4000.5}) {
                int quads = 0;
                for (WarModel.Part part : WarModel.Part.values()) {
                    int[] count = {0};
                    boolean[] ok = {true};
                    boolean[] lit = {false};
                    WarModel.build((a, b, c, d, n, ca, cb, cc, cd) -> {
                        count[0]++;
                        for (double[] v : new double[][] {a, b, c, d, n}) {
                            ok[0] &= Double.isFinite(v[0]) && Double.isFinite(v[1]) && Double.isFinite(v[2]) && Math.abs(v[0]) < 40 && Math.abs(v[1]) < 40
                                && Math.abs(v[2]) < 40;
                        }
                        lit[0] |= n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
                    }, part, time, 1.0, night);
                    quads += count[0];
                    finite &= ok[0];
                    glows |= lit[0];
                }
                total = Math.max(total, quads);
            }
        }
        check(finite, "War's shapes are finite and stay near the body");
        check(glows, "something of War burns");
        check(total > 3000 && total < 16000, "War's quads within budget: " + total);
        int[] none = {0};
        for (WarModel.Part part : WarModel.Part.values()) {
            WarModel.build((a, b, c, d, n, ca, cb, cc, cd) -> none[0]++, part, 0.0, 0.0);
        }
        check(none[0] == 0, "nothing before it grows");
        double[] eye = WarModel.eye(1.0);
        check(eye[0] > 0.0 && eye[1] < -0.5 && eye[2] < -4.0, "the eye is on the face, on the left");
    }
}
