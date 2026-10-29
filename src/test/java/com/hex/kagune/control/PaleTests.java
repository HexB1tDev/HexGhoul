package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.PaleFleshPose;
import com.hex.kagune.control.client.animation.PaleIdle;
import com.hex.kagune.control.client.animation.PaleMotions;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.pale.PaleFleshModel;
import com.hex.kagune.control.pale.PaleKey;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.PaleSteamRules;
import com.hex.kagune.control.pale.PaleStrike;
import com.hex.kagune.control.pale.PaleUnlock;
import com.hex.kagune.control.pale.KickTumble;
import com.hex.kagune.control.pale.ReviveRules;
import com.hex.kagune.control.pale.StunRules;
import com.hex.kagune.control.client.animation.Remains;
import com.hex.kagune.control.pale.WatchRules;
import com.hex.kagune.control.client.animation.WatchPose;
import com.hex.kagune.control.client.animation.ArmPose;
import com.hex.kagune.control.pale.ArmRules;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.pale.OwlRules;
import com.hex.kagune.control.client.animation.OwlLancePose;
import com.hex.kagune.control.pale.OwlLances;
import com.hex.kagune.control.pale.OwlShards;
import com.hex.kagune.control.pale.OwlModel;
import com.hex.kagune.control.pale.OwlCreature;
import com.hex.kagune.control.pale.OwlActs;
import com.hex.kagune.control.pale.OwlMesh;
import com.hex.kagune.control.pale.OwlSpikes;
import com.hex.kagune.control.client.animation.OwlBulk;
import com.hex.kagune.control.client.animation.OwlArms;
import com.hex.kagune.control.client.animation.OwlMotions;
import com.hex.kagune.control.client.animation.OwlPose;
import com.hex.kagune.control.kakugan.KakuganEyes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 6.1.0: the Pale path: her rules and track, her idle (never a limb through the body), her steam. */
public final class PaleTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();
    /** How far (pixels) an arm may graze the torso or head: less than a third of a pixel, unseen. */
    static final double CLEARANCE = 0.25;

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        track();
        clearance();
        idle();
        strikes();
        stun();
        ownArms();
        flips();
        soar();
        cuts();
        bigPunch();
        flesh();
        kick();
        hammer();
        trueAnonymity();
        witness();
        watch();
        arm();
        owl();
        owlCreature();
        owlCreatureFrames();
        owlCreatureGaits();
        owlActs();
        owlMoves();
        owlLances();
        owlVolley();
        owlDna();
        revive();
        physics();
        keys();
        steam();
        eyes();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Pale tests: " + passed + " checks passed");
    }

    /**
     * 7.0.1: the shared eye-mode fight (EyeRules): how hard the eyes alone hit grows with mastery and stays under any kagune's own
     * sustained M1; an M1 over a block strikes only a body right by the crosshair; the panel shows the kit with the eyes open.
     */
    static void eyes() {
        check(Math.abs(com.hex.kagune.control.pale.EyeRules.power(0) - com.hex.kagune.control.pale.EyeRules.POWER_NEW) < 1.0E-6
            && Math.abs(com.hex.kagune.control.pale.EyeRules.power(1000) - com.hex.kagune.control.pale.EyeRules.POWER_MASTERED) < 1.0E-6,
            "eyes: power runs from new to mastered");
        check(com.hex.kagune.control.pale.EyeRules.power(-50) == com.hex.kagune.control.pale.EyeRules.power(0)
            && com.hex.kagune.control.pale.EyeRules.power(5000) == com.hex.kagune.control.pale.EyeRules.power(1000), "eyes: power is clamped");
        float last = 0.0F;
        boolean rising = true;
        for (int m = 0; m <= 1000; m += 10) {
            float power = com.hex.kagune.control.pale.EyeRules.power(m);
            rising &= power >= last;
            last = power;
        }
        check(rising, "eyes: more mastery never hits softer");
        double fresh = eyeBlitzRate(0);
        double master = eyeBlitzRate(1000);
        check(master > fresh * 2.0, "eyes: a master's blitz is over twice a new ghoul's a second (" + fresh + " -> " + master + ")");
        check(master < 18.0, "eyes: the eyes alone stay under a kagune's sustained M1 at 1000 (DamageBalance: 18..24/s): " + master);
        check(fresh > 3.0, "eyes: a new ghoul's fists still count: " + fresh);
        check(com.hex.kagune.control.pale.EyeRules.strikesOverBlock(2.0, 0.98), "eyes: a body by the crosshair over a block is struck");
        check(!com.hex.kagune.control.pale.EyeRules.strikesOverBlock(2.0, 0.5), "eyes: a body off to the side: the block is mined");
        check(!com.hex.kagune.control.pale.EyeRules.strikesOverBlock(6.0, 1.0), "eyes: a body out of reach: the block is mined");
        for (KagunePath path : new KagunePath[] {KagunePath.RINKAKU, KagunePath.UKAKU, KagunePath.GRAFT}) {
            for (int m : new int[] {0, PaleRules.KICK_MASTERY - 1, PaleRules.KICK_MASTERY, 1000}) {
                AbilityGuide.Panel panel = AbilityGuide.panel(new AbilityGuide.Input(path, AbilityGuide.State.EYES, m,
                    com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, ControlTests.defaultKeys()));
                AbilityGuide.Row blitz = ControlTests.find(panel, "Speed Blitz");
                AbilityGuide.Row kick = ControlTests.find(panel, "Speed Kick");
                check(blitz != null && "LMB".equals(blitz.keys()) && blitz.name().contains(PaleStrike.count(m) + " strikes")
                    && ControlTests.find(panel, "Flips") != null && ControlTests.find(panel, "Big Punch") != null,
                    "eyes: " + path + " at " + m + ": the blitz, the flips and the Big Punch on the panel");
                check(kick != null && "V".equals(kick.keys()) && kick.locked() == (m < PaleRules.KICK_MASTERY),
                    "eyes: " + path + " at " + m + ": the Speed Kick on V, dim below its mastery");
            }
            AbilityGuide.Panel closed = AbilityGuide.panel(new AbilityGuide.Input(path, AbilityGuide.State.CLOSED, 1000,
                com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, ControlTests.defaultKeys()));
            check(ControlTests.find(closed, "Speed Blitz") == null, "eyes: " + path + ": eyes closed, no fight on the panel");
        }
    }

    /** The eyes alone's blitz sustained as the server chains it (each strike from the last one's chain tick; the finisher, its recovery): damage a second. */
    static double eyeBlitzRate(int mastery) {
        double damage = 0.0;
        double ticks = 0.0;
        PaleStrike strike = null;
        for (int i = 0; i < 64; i++) {
            strike = PaleStrike.next(strike, mastery);
            damage += PaleRules.fist(mastery) * strike.damage * com.hex.kagune.control.pale.EyeRules.power(mastery);
            ticks += strike.finisher() ? strike.duration + PaleRules.FINISH_RECOVERY : strike.chain;
        }
        return damage / ticks * 20.0;
    }

    static void track() {
        int previous = -1;
        for (PaleUnlock unlock : PaleUnlock.values()) {
            check(unlock.mastery() >= previous, "the track runs in mastery order: " + unlock);
            previous = unlock.mastery();
        }
        check(PaleUnlock.KAKUJA_V1.reached(PaleRules.KAKUJA_V1_MASTERY) && !PaleUnlock.KAKUJA_V1.reached(PaleRules.KAKUJA_V1_MASTERY - 1),
            "the Owl at its mastery");
        check(PaleUnlock.next(1000) == null && PaleUnlock.values()[PaleUnlock.values().length - 1] != PaleUnlock.KAKUJA_V1
            && java.util.Arrays.stream(PaleUnlock.values()).noneMatch(u -> u.name().contains("V2") || u.text().contains("V2")),
            "6.2.0: the Owl is her only kakuja, no sealed V2 on the track; at 1000 every step is reached");
        check(PaleUnlock.BIG_PUNCH.reached(0) && PaleUnlock.BLITZ.reached(0) && PaleUnlock.FLIPS.reached(0), "fists, flips and Big Punch from the start");
        check(PaleStrike.count(0) == 4 && PaleStrike.count(PaleRules.BLITZ_BLINK_MASTERY) == 6 && PaleStrike.count(1000) == 8,
            "the blitz grows from four strikes to eight");
        PaleStrike strike = null;
        for (int i = 0; i < 4; i++) {
            strike = PaleStrike.next(strike, 0);
        }
        check(PaleStrike.next(strike, 0) == PaleStrike.JAB, "past her last strike the blitz starts over");
        check(PaleRules.fist(0) == 1.8F && Math.abs(PaleRules.fist(1000) - 4.5F) < 1.0E-4F, "a fist hits for 1.8, 4.5 at mastery 1000");
        check(PaleRules.punchCharge(0) > 0.0F && PaleRules.punchCharge(1000) == 1.0F, "Big Punch: a quick release still punches, the charge tops out");
        check(PaleRules.formRegen(0) > 0.0F && PaleRules.formRegen(500) > PaleRules.formRegen(0) && Math.abs(PaleRules.formRegen(1000) * 20.0F - 2.4F) < 1.0E-4F
            && PaleRules.formRegen(3000) == PaleRules.formRegen(1000), "her RC comes back in form, faster with mastery, 2.4 a second at 1000");
        check(PaleRules.swipeBearing(0) > 0 && PaleRules.swipeBearing(100) < 0, "the swipe goes from her right to her left");
        double last = Double.MAX_VALUE;
        for (double age = 0; age <= PaleRules.SWIPE_TICKS; age += 0.25) {
            double bearing = PaleRules.swipeBearing(age);
            check(bearing <= last + 1.0E-9, "the sweep never swings back");
            last = bearing;
        }
        check(PaleRules.swipeExtent(PaleRules.SWIPE_TICKS) < 0.01, "the flesh is back in by the end of the swipe");
        check(PaleRules.Flip.from(1, 0) == PaleRules.Flip.FORWARD && PaleRules.Flip.from(-1, 0) == PaleRules.Flip.BACK
            && PaleRules.Flip.from(0, 1) == PaleRules.Flip.LEFT && PaleRules.Flip.from(0, -1) == PaleRules.Flip.RIGHT
            && PaleRules.Flip.from(0, 0) == PaleRules.Flip.FORWARD, "W S A D pick the flip, nothing held flips forward");
    }

    static void clearance() {
        RigPose rest = new RigPose();
        rest.settle(1.0F);
        check(BodyFrame.clipping(rest, CLEARANCE) == 0.0, "standing still, nothing clips");
        // The pose a review caught: a fist pulled in to the mouth runs the forearm through the chest and face.
        RigPose caught = new RigPose();
        caught.rightArmX = -1.6F;
        caught.rightArmY = -1.4F;
        caught.rightArmZ = -0.6F;
        caught.rightElbow = 2.0F;
        caught.settle(1.0F);
        check(BodyFrame.clipping(caught, CLEARANCE) > 1.0, "the arm through the chest is caught");
        // A forearm laid across the chest (the arm turned in, the elbow folded): the forearm gets no slack at the shoulder.
        RigPose crossed = new RigPose();
        crossed.rightArmX = -0.6F;
        crossed.rightArmY = -1.3F;
        crossed.rightElbow = 1.8F;
        crossed.settle(1.0F);
        check(BodyFrame.clipping(crossed, CLEARANCE) > 1.0, "a forearm folded across the chest is caught");
        // The upper arm swung in across the body sinks into the chest at the shoulder, out of sight; only so far.
        RigPose swung = new RigPose();
        swung.rightArmX = -1.0F;
        swung.rightArmZ = -0.9F;
        swung.rightElbow = 2.0F;
        swung.settle(1.0F);
        check(BodyFrame.clipping(swung, CLEARANCE) < 0.05, "an arm swung in sinks no more than its joint allows at the shoulder");
        RigPose through = new RigPose();
        through.rightArmZ = -0.6F;
        through.settle(1.0F);
        check(BodyFrame.clipping(through, CLEARANCE) > 1.0, "a hanging arm swung in through the body is caught");
        // The model's rig: feet on the ground, head on top.
        double[] foot = BodyFrame.rig(BodyFrame.model(rest, "rightLeg", new double[] {0, 12, 0}), 0.0);
        double[] crown = BodyFrame.rig(BodyFrame.model(rest, "head", new double[] {0, -8, 0}), 0.0);
        check(Math.abs(foot[1]) < 0.01 && foot[0] > 0.05, "the right foot is on the ground at her right: " + foot[0] + ", " + foot[1]);
        check(Math.abs(crown[1] - 1.876) < 0.02, "the crown of the head is 1.88 blocks up: " + crown[1]);
    }

    static void idle() {
        for (PaleIdle.Gesture gesture : PaleIdle.Gesture.values()) {
            double worst = 0.0;
            String where = "";
            for (float side : new float[] {1.0F, -1.0F, 0.0F, 0.5F}) {
                for (int hand : new int[] {1, -1}) {
                    for (float t = 0.0F; t <= 1.0001F; t += 0.01F) {
                        for (double time : new double[] {0.0, 1.7, 3.3, 5.9}) {
                            double clip = BodyFrame.clipping(PaleIdle.compose(side, time, gesture, t, hand), CLEARANCE);
                            if (clip > worst) {
                                worst = clip;
                                where = " (side " + side + ", hand " + hand + ", t " + t + ", time " + time + ")";
                            }
                        }
                    }
                }
            }
            check(worst == 0.0, "her arms stay out of her body all through " + gesture + ": " + worst + " px" + where);
        }
        for (float side = -1.0F; side <= 1.0F; side += 0.1F) {
            for (double time = 0.0; time < 20.0; time += 0.37) {
                check(BodyFrame.clipping(PaleIdle.compose(side, time, null, 0.0F, 1), CLEARANCE) == 0.0, "the stance never clips (side " + side + ")");
            }
        }
        // The scheduler: never the last two again, everything turns up, rests in between.
        PaleIdle idle = new PaleIdle(42L);
        PaleIdle.Gesture last = null;
        PaleIdle.Gesture beforeLast = null;
        Set<PaleIdle.Gesture> seen = EnumSet.noneOf(PaleIdle.Gesture.class);
        int started = 0;
        int lastEnd = -1;
        int quietest = Integer.MAX_VALUE;
        PaleIdle.Gesture previous = null;
        for (int tick = 0; tick < 40000; tick++) {
            idle.tick(true, false);
            PaleIdle.Gesture now = idle.current();
            if (now != null && now != previous) {
                check(now != last && now != beforeLast, "a gesture never repeats either of the last two: " + now);
                beforeLast = last;
                last = now;
                seen.add(now);
                started++;
                if (lastEnd >= 0) {
                    quietest = Math.min(quietest, tick - lastEnd);
                }
            }
            if (now == null && previous != null) {
                lastEnd = tick;
            }
            previous = now;
        }
        check(seen.size() == PaleIdle.Gesture.values().length, "every gesture turns up: " + seen);
        check(started > 150, "she does something every few seconds: " + started);
        check(quietest >= PaleIdle.REST_MIN - 1, "and rests between them: " + quietest);
        // On the move (or in a flip, a strike): nothing starts, and whatever was going is gone within a few ticks.
        PaleIdle walking = new PaleIdle(7L);
        for (int tick = 0; tick < 20000; tick++) {
            walking.tick(false, true);
            check(walking.current() == null, "nothing plays on the move");
        }
        PaleIdle interrupted = new PaleIdle(9L);
        int guard = 0;
        while (interrupted.current() == null && guard++ < 2000) {
            interrupted.tick(true, false);
        }
        check(interrupted.current() != null, "standing still, she does something");
        int gone = 0;
        while (interrupted.current() != null && gone < 40) {
            interrupted.tick(false, true);
            gone++;
        }
        check(gone <= 10, "moving, the gesture going fades out and is dropped within half a second: " + gone + " ticks");
    }

    /** Where (rig space, blocks) the striking fist is in {@code pose}. */
    static double[] fist(RigPose pose, boolean right) {
        return BodyFrame.world(pose, BodyFrame.model(pose, right ? "rightArm" : "leftArm", new double[] {right ? -1 : 1, 9, 0}));
    }

    /**
     * Every pose a strike can begin from in a real blitz: her guard; the strike before it in the chain, anywhere from its chain tick
     * to its end (a click queued during it, or a late one during its recovery); and for the opening jab, anything she might be
     * doing first: the end of a blitz cut short at any strike her mastery stops at, the finisher, any idle gesture at any moment,
     * a flip's landing.
     */
    static List<RigPose> starts(PaleStrike strike) {
        List<RigPose> starts = new ArrayList<>();
        starts.add(null);
        List<PaleStrike> befores = new ArrayList<>();
        if (strike == PaleStrike.JAB) {
            befores.add(PaleStrike.LIVER);
            befores.add(PaleStrike.BLINK_PALM);
            befores.add(PaleStrike.TWIN_PALM);
        } else {
            befores.add(PaleStrike.values()[strike.ordinal() - 1]);
        }
        for (PaleStrike before : befores) {
            for (double age = before.chain; age <= before.duration + 0.001; age += 0.5) {
                starts.add(PaleMotions.strike(before, age, null));
            }
        }
        if (strike == PaleStrike.JAB) {
            for (PaleIdle.Gesture gesture : PaleIdle.Gesture.values()) {
                for (float t = 0.0F; t <= 1.0001F; t += 0.125F) {
                    for (float side : new float[] {1.0F, -1.0F}) {
                        for (int hand : new int[] {1, -1}) {
                            starts.add(PaleIdle.compose(side, 1.7, gesture, t, hand));
                        }
                    }
                }
            }
            for (PaleRules.Flip flip : PaleRules.Flip.values()) {
                starts.add(PaleMotions.flip(flip, PaleRules.FLIP_SPIN_TICKS + 2.0, 1.0));
            }
        }
        return starts;
    }

    static void strikes() {
        for (PaleStrike strike : PaleStrike.values()) {
            List<RigPose> starts = starts(strike);
            check(strike.impact >= 2 && strike.chain > strike.impact && strike.duration >= strike.chain, "a strike coils, lands, then chains: " + strike);
            double worst = 0.0;
            String where = "";
            for (int s = 0; s < starts.size(); s++) {
                // Past the impact the start no longer shows; the keys themselves are swept from the guard.
                double until = s == 0 ? strike.duration : strike.impact + 1.0;
                for (double age = 0.0; age <= until + 0.001; age += 0.05) {
                    RigPose pose = PaleMotions.strike(strike, age, starts.get(s));
                    pose.settle(1.0F);
                    double clip = BodyFrame.clipping(pose, CLEARANCE);
                    if (clip > worst) {
                        worst = clip;
                        where = " (from start " + s + ", tick " + String.format(java.util.Locale.ROOT, "%.2f", age) + ")";
                    }
                    check(pose.drop >= 0.0F && Math.min(RigPose.legDrop(pose.rightLegX, pose.rightLegZ, pose.rightKnee),
                        RigPose.legDrop(pose.leftLegX, pose.leftLegZ, pose.leftKnee)) - pose.drop < 0.01F, "her lower foot stays on the ground: " + strike);
                }
            }
            check(worst == 0.0, "her arms stay out of her body all through the " + strike + ": " + worst + " px" + where);
            // The first frame is the pose it began from: a strike never jumps.
            RigPose from = PaleMotions.strike(PaleStrike.HOOK, 4.0, null);
            RigPose first = PaleMotions.strike(strike, 0.0, from);
            check(Math.abs(first.rightArmX - from.rightArmX) < 1.0E-4 && Math.abs(first.leftElbow - from.leftElbow) < 1.0E-4
                && Math.abs(first.twist - from.twist) < 1.0E-4 && Math.abs(first.lunge - from.lunge) < 1.0E-4, "a strike starts where the last one was: " + strike);
            RigPose held = PaleMotions.strike(strike, strike.impact, null);
            check(held.armHold == 1.0F && held.legHold == 1.0F, "a strike owns her limbs: " + strike);
        }
        // Heft: at the impact the blow is out where it lands, the body behind it.
        for (PaleStrike strike : new PaleStrike[] {PaleStrike.JAB, PaleStrike.CROSS, PaleStrike.BLINK_PALM, PaleStrike.TWIN_PALM}) {
            RigPose hit = PaleMotions.strike(strike, strike.impact, null);
            hit.settle(1.0F);
            double reach = fist(hit, strike.right)[2];
            double wanted = strike == PaleStrike.JAB ? 0.9 : 1.15;
            check(reach >= wanted, "the " + strike + " lands a long way out (" + String.format(java.util.Locale.ROOT, "%.2f", reach) + " blocks)");
            check(hit.lunge > 2.0F, "the hips go into the " + strike);
        }
        for (PaleStrike strike : new PaleStrike[] {PaleStrike.CROSS, PaleStrike.HOOK, PaleStrike.LIVER, PaleStrike.UPPERCUT}) {
            RigPose coil = PaleMotions.strike(strike, strike.impact - 1.2, null);
            RigPose hit = PaleMotions.strike(strike, strike.impact, null);
            // Turned away in the coil, through and past square at the impact (+ spin and - twist bring the right side round; a left
            // hand's strike turns the other way).
            double hand = strike.right ? 1.0 : -1.0;
            double coiled = hand * (coil.spin - coil.twist);
            double through = hand * (hit.spin - hit.twist);
            check(through - coiled > 1.4, "the body turns through the " + strike + ": " + (through - coiled) + " rad");
        }
    }

    static void bigPunch() {
        // The charge starts from anything she may be doing: her guard, a strike past its blow, any idle gesture, a flip's landing.
        List<RigPose> starts = new ArrayList<>(starts(PaleStrike.JAB));
        for (PaleStrike strike : PaleStrike.values()) {
            for (double age = strike.impact + 1.0; age <= strike.duration + 0.001; age += 1.0) {
                starts.add(PaleMotions.strike(strike, age, null));
            }
        }
        double worst = 0.0;
        String where = "";
        for (int s = 0; s < starts.size(); s++) {
            for (double age = 0.0; age <= (s == 0 ? PaleRules.PUNCH_MAX_HOLD : 2.5); age += 0.05) {
                RigPose pose = PaleMotions.punchCharge(age, starts.get(s));
                pose.settle(1.0F);
                double clip = BodyFrame.clipping(pose, CLEARANCE);
                if (clip > worst) {
                    worst = clip;
                    where = " (from start " + s + ", tick " + String.format(java.util.Locale.ROOT, "%.2f", age) + ")";
                }
            }
        }
        check(worst == 0.0, "her arms stay out of her body all through Big Punch's charge: " + worst + " px" + where);
        worst = 0.0;
        for (double held = 0.0; held <= PaleRules.PUNCH_MAX_HOLD; held += 2.5) {
            RigPose charge = PaleMotions.punchCharge(held, null);
            for (double age = 0.0; age <= PaleRules.PUNCH_TICKS + 0.001; age += 0.05) {
                RigPose pose = PaleMotions.punch(age, charge);
                pose.settle(1.0F);
                double clip = BodyFrame.clipping(pose, CLEARANCE);
                if (clip > worst) {
                    worst = clip;
                    where = " (held " + held + ", tick " + String.format(java.util.Locale.ROOT, "%.2f", age) + ")";
                }
            }
        }
        check(worst == 0.0, "her arms stay out of her body all through Big Punch: " + worst + " px" + where);
        RigPose hit = PaleMotions.punch(PaleRules.PUNCH_IMPACT, null);
        hit.settle(1.0F);
        check(fist(hit, true)[2] >= 1.4, "Big Punch lands further out than any strike: " + fist(hit, true)[2]);
        RigPose coiled = PaleMotions.punchCharge(PaleRules.PUNCH_CHARGE_TICKS, null);
        check(coiled.spin - coiled.twist < -1.0 && hit.spin - hit.twist > 1.2, "charged, she is wound right round; the punch turns her through");
        check(PaleRules.PUNCH_BLINK < PaleRules.PUNCH_IMPACT && PaleRules.PUNCH_IMPACT < PaleRules.PUNCH_TICKS, "blink, then the blow, then the hold");
    }

    /** One frame of the flesh to check: her body, where its tip goes, how wide its grip, the grab's reach. */
    record FleshCase(PaleMove move, double age, RigPose body, double[] tip, double grip, double reach, String label) {
    }

    static void flesh() {
        // Out of her back, over her right shoulder, never through her: every vertex of the mass (trunk, tendrils, rake, hand) stays
        // out of her torso, head and arms, but where it grows out of her back (close to the root, never towards her chest).
        List<FleshCase> cases = fleshCases();
        double[] hold = {0.0, PaleRules.GRAB_HOLD_UP, PaleRules.GRAB_HOLD_AHEAD};
        int whip = PaleRules.grabRelease();
        double worst = 0.0;
        String where = "";
        int shown = 0;
        int[] grown = new int[PaleMove.values().length];
        for (FleshCase c : cases) {
            RigPose body = c.body();
            body.settle(1.0F);
            for (double time : new double[] {0.0, 37.4}) {
                PaleFleshModel.Pose pose = PaleFleshPose.of(body, c.move(), c.age(), c.tip(), c.grip(), c.reach(), time, 41);
                if (pose == null) {
                    continue;
                }
                grown[c.move().ordinal()]++;
                double[] root = PaleFleshPose.rootOf(body)[0];
                double[] out = PaleFleshPose.rootOf(body)[1];
                BodyFrame.Solid solid = new BodyFrame.Solid(body);
                double[] deepest = {0.0};
                PaleFleshModel.mesh((a, b, cc, d, n, ca, cb, ccc, cd) -> {
                    for (double[] v : new double[][] {a, b, cc, d}) {
                        double other = solid.beyondTorso(v, 0.0);
                        if (other > CLEARANCE) {
                            deepest[0] = Math.max(deepest[0], other);
                        }
                        double torso = solid.torso(v, 0.0);
                        if (torso <= CLEARANCE) {
                            continue;
                        }
                        double[] r = {v[0] - root[0], v[1] - root[1], v[2] - root[2]};
                        double fromRoot = Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
                        double outward = r[0] * out[0] + r[1] * out[1] + r[2] * out[2];
                        if (fromRoot <= 0.32 && outward >= -0.09) {
                            continue; // growing out of her back
                        }
                        deepest[0] = Math.max(deepest[0], torso);
                    }
                }, pose);
                if (deepest[0] > worst) {
                    worst = deepest[0];
                    where = " (" + c.move() + " tick " + c.age() + ", " + c.label() + ")";
                }
                if (deepest[0] > 0.0 && shown++ < 6) {
                    System.err.println("flesh into her: " + deepest[0] + " px, " + c.move() + " tick " + c.age() + ", " + c.label());
                }
            }
        }
        check(worst == 0.0, "the flesh never goes through her (torso, head, arms): " + worst + " px" + where);
        // The body as the flesh sees it: the middle of her torso two pixels in (it is four deep), of her head four.
        for (RigPose from : fleshStarts()) {
            RigPose body = PaleMotions.flesh(PaleMove.GRAB_HOLD, 6.0, from);
            body.settle(1.0F);
            BodyFrame.Solid solid = new BodyFrame.Solid(body);
            double[] chest = BodyFrame.world(body, BodyFrame.model(body, "torso", new double[] {0, 6, 0}));
            double[] head = BodyFrame.world(body, BodyFrame.model(body, "head", new double[] {0, -4, 0}));
            check(Math.abs(solid.torso(chest, 0.0) - 2.0) < 1.0E-6 && Math.abs(solid.beyondTorso(head, 0.0) - 4.0) < 1.0E-6
                && Math.abs(solid.depth(head, 0.1) - BodyFrame.sphereDepth(body, head, 0.1)) < 1.0E-9, "her body, posed, as the flesh sees it");
        }
        for (PaleMove move : new PaleMove[] {PaleMove.SWIPE, PaleMove.GRAB_REACH, PaleMove.GRAB_HOLD, PaleMove.GRAB_THROW, PaleMove.GRAB_MISS}) {
            check(grown[move.ordinal()] > 0, "the flesh is out during " + move);
        }
        // It comes out and goes back in: nothing at the swipe's start and end, all out at its sweep; the throw draws it back.
        check(PaleFleshPose.of(new RigPose(), PaleMove.SWIPE, 0.0, PaleFleshPose.swipeTip(0.0), 0.5, 0.0, 0.0, 1) == null, "the swipe starts in her back");
        check(PaleFleshPose.of(new RigPose(), PaleMove.SWIPE, PaleRules.SWIPE_TICKS, PaleFleshPose.swipeTip(PaleRules.SWIPE_TICKS), 0.5, 0.0, 0.0, 1) == null
            || PaleFleshPose.of(new RigPose(), PaleMove.SWIPE, PaleRules.SWIPE_TICKS, PaleFleshPose.swipeTip(PaleRules.SWIPE_TICKS), 0.5, 0.0, 0.0, 1).grown() < 0.02,
            "the swipe ends with it back in");
        PaleFleshModel.Pose sweeping = PaleFleshPose.of(new RigPose(), PaleMove.SWIPE, 8.0, PaleFleshPose.swipeTip(8.0), 0.5, 0.0, 0.0, 1);
        double[] farthest = {0.0};
        PaleFleshModel.mesh((a, b, cc, d, n, ca, cb, ccc, cd) -> {
            for (double[] v : new double[][] {a, b, cc, d}) {
                farthest[0] = Math.max(farthest[0], Math.hypot(v[0], v[2]));
            }
        }, sweeping);
        check(sweeping.grown() > 0.99 && farthest[0] > PaleRules.SWIPE_REACH - 0.5 && farthest[0] < PaleRules.SWIPE_REACH + 0.5,
            "at its sweep the rake reaches as far as it hits: " + farthest[0]);
        // Grabbing, the tendrils go round what they hold: its middle is inside them, past the knot.
        PaleFleshModel.Pose holding = PaleFleshPose.of(new RigPose(), PaleMove.GRAB_HOLD, 4.0, hold, 0.6, 0.0, 0.0, 1);
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        double[] nearest = {Double.MAX_VALUE};
        PaleFleshModel.mesh((a, b, cc, d, n, ca, cb, ccc, cd) -> {
            for (double[] v : new double[][] {a, b, cc, d}) {
                double[] r = {v[0] - hold[0], v[1] - hold[1], v[2] - hold[2]};
                double gap = Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
                nearest[0] = Math.min(nearest[0], gap);
                if (gap < 1.4) {
                    for (int k = 0; k < 3; k++) {
                        lo[k] = Math.min(lo[k], r[k]);
                        hi[k] = Math.max(hi[k], r[k]);
                    }
                }
            }
        }, holding);
        check(nearest[0] > 0.3, "the tendrils hold a body, not go through its middle: " + nearest[0]);
        check(lo[0] < -0.3 && hi[0] > 0.3 && lo[1] < -0.3 && hi[1] > 0.3 && hi[2] > 0.25, "the tendrils close round it on every side and over its far side");
        PaleFleshModel.Pose gone = PaleFleshPose.of(new RigPose(), PaleMove.GRAB_THROW, whip + PaleRules.GRAB_RETRACT, hold, 0.5, 0.0, 0.0, 1);
        check(gone == null || gone.grown() < 0.02, "the throw ends with the flesh back in");
        // The throw's path: from where it was held, all the way back behind her, over her (never through her) and out ahead, let
        // go at its fastest.
        double[] start = PaleRules.throwAt(0.0, hold);
        double[] wound = PaleRules.throwAt(PaleRules.GRAB_PULL + PaleRules.GRAB_HANG, hold);
        double[] letGo = PaleRules.throwAt(whip, hold);
        check(Math.abs(start[2] - hold[2]) < 1.0E-9 && wound[2] < -3.5 && Math.abs(letGo[2] - PaleRules.GRAB_FLING[2]) < 1.0E-9,
            "the throw starts where it held it, hauls it all the way back, and lets go out ahead");
        double fastest = 0.0;
        double closest = Double.MAX_VALUE;
        double[] head = {0.0, 1.6, 0.0};
        for (double age = 0.25; age <= whip + 1.0E-9; age += 0.25) {
            double[] a = PaleRules.throwAt(age - 0.25, hold);
            double[] b = PaleRules.throwAt(age, hold);
            double speed = Math.sqrt(Math.pow(b[0] - a[0], 2) + Math.pow(b[1] - a[1], 2) + Math.pow(b[2] - a[2], 2)) * 4.0;
            check(age < whip - 0.3 || speed >= fastest - 1.0E-9, "the whip is fastest as it lets go");
            fastest = Math.max(fastest, speed);
            closest = Math.min(closest, Math.sqrt(Math.pow(b[0] - head[0], 2) + Math.pow(b[1] - head[1], 2) + Math.pow(b[2] - head[2], 2)));
        }
        check(fastest > 4.0, "the whip is violent: " + fastest + " blocks a tick at its fastest");
        check(closest > 1.6, "the thrown body goes over her, never through her: " + closest);
        // The swipe sweeps the frame she faced: turned, its tip turns with it.
        double[] straight = PaleFleshPose.swipeTip(8.5);
        double[] right = PaleFleshPose.turned(straight, Math.PI / 2.0);
        check(Math.abs(right[0] - straight[2]) < 1.0E-9 && Math.abs(right[2] + straight[0]) < 1.0E-9, "a quarter turn right puts ahead on her right");
    }

    /** Where she may be when the flesh comes out: her guard, a strike past its blow, an idle gesture, a flip's landing. */
    static List<RigPose> fleshStarts() {
        List<RigPose> froms = new ArrayList<>();
        froms.add(null);
        List<RigPose> all = starts(PaleStrike.JAB);
        for (int i = 7; i < all.size(); i += 23) {
            froms.add(all.get(i));
        }
        return froms;
    }

    /** Every frame of the flesh the tests look at: each move through, from where she may start it, reaching wherever. */
    static List<FleshCase> fleshCases() {
        List<RigPose> froms = fleshStarts();
        List<FleshCase> cases = new ArrayList<>();
        for (RigPose from : froms) {
            for (double age = 0.0; age <= PaleRules.SWIPE_TICKS + 0.001; age += 0.5) {
                for (double turn : new double[] {-0.6, 0.0, 0.6}) {
                    cases.add(new FleshCase(PaleMove.SWIPE, age, PaleMotions.flesh(PaleMove.SWIPE, age, from),
                        PaleFleshPose.turned(PaleFleshPose.swipeTip(age), turn), 0.5, 0.0, "swipe turned " + turn));
                }
            }
        }
        // Whatever she reaches for, wherever it is (it may be anywhere round her by the time the flesh gets there).
        for (double yaw = -180.0; yaw < 180.0; yaw += 20.0) {
            for (double pitch : new double[] {-55.0, -25.0, 0.0, 30.0, 60.0}) {
                for (double distance : new double[] {1.6, 4.0, 11.0}) {
                    double y = Math.toRadians(yaw);
                    double p = Math.toRadians(pitch);
                    double[] tip = {Math.sin(y) * Math.cos(p) * distance, 1.5 + Math.sin(p) * distance, Math.cos(y) * Math.cos(p) * distance};
                    if (tip[1] < 0.2) {
                        continue;
                    }
                    String where = " at yaw " + yaw + ", pitch " + pitch + ", " + distance;
                    double reach = PaleRules.grabReachTicks(distance);
                    for (double u : new double[] {0.3, 0.7, 1.0}) {
                        cases.add(new FleshCase(PaleMove.GRAB_REACH, reach * u, PaleMotions.flesh(PaleMove.GRAB_REACH, reach * u, null), tip, 0.5, reach,
                            "reaching" + where));
                    }
                    cases.add(new FleshCase(PaleMove.GRAB_MISS, PaleRules.GRAB_MISS_TICKS * 0.5,
                        PaleMotions.flesh(PaleMove.GRAB_MISS, PaleRules.GRAB_MISS_TICKS * 0.5, null), tip, 0.5, 0.0, "missing" + where));
                    if (distance < 11.0) {
                        for (double grip : new double[] {0.3, 0.9}) {
                            cases.add(new FleshCase(PaleMove.GRAB_HOLD, 10.0, PaleMotions.flesh(PaleMove.GRAB_HOLD, 10.0, null), tip, grip, 0.0,
                                "holding" + where));
                        }
                    }
                }
            }
        }
        // The throw: from where she held it (up before her, or off to a side), hauled over her shoulder and all the way back, hung
        // there, whipped over her and out, then the flesh drawn back in.
        int release = PaleRules.grabRelease();
        for (double[] from : new double[][] {{0.0, PaleRules.GRAB_HOLD_UP, PaleRules.GRAB_HOLD_AHEAD}, {2.2, 1.6, 3.0}, {-2.0, 2.8, 2.6}}) {
            for (double age = 0.0; age <= release + PaleRules.GRAB_RETRACT + 0.001; age += 0.5) {
                double[] at = PaleRules.throwAt(age, from);
                double[] tip = age <= release ? new double[] {at[0], at[1], at[2]} : PaleRules.GRAB_FLING.clone();
                for (double grip : new double[] {0.3, 0.9}) {
                    cases.add(new FleshCase(PaleMove.GRAB_THROW, age, PaleMotions.flesh(PaleMove.GRAB_THROW, age, null), tip, grip, 0.0, "throwing"));
                }
            }
        }
        return cases;
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** A straight length of flesh 4 blocks long out of the origin, turned {@code bearing} radians round from straight ahead. */
    static double[][] rod(double bearing) {
        double[][] path = new double[9][];
        for (int i = 0; i < 9; i++) {
            double r = i * 0.5;
            path[i] = new double[] {Math.sin(bearing) * r, 1.0, Math.cos(bearing) * r};
        }
        return path;
    }

    static void physics() {
        // A rod swept round its root through a right angle in 6 ticks, then held there.
        java.util.function.DoubleFunction<double[][]> sweep = t -> rod(Math.PI * 0.5 * Math.min(1.0, t / 6.0));
        com.hex.kagune.control.pale.FleshPhysics.Feel whip = com.hex.kagune.control.pale.FleshPhysics.WHIP;
        double[][] mid = com.hex.kagune.control.pale.FleshPhysics.simulate(sweep, 3.0, whip);
        double[][] want = com.hex.kagune.control.pale.FleshPhysics.resample(sweep.apply(3.0), whip.points());
        check(Math.abs(mid[0][0] - want[0][0]) < 1.0E-9 && Math.abs(mid[0][2] - want[0][2]) < 1.0E-9, "the root stays on the root");
        double tipAngle = Math.atan2(mid[mid.length - 1][0], mid[mid.length - 1][2]);
        double nearAngle = Math.atan2(mid[3][0], mid[3][2]);
        check(tipAngle < Math.PI * 0.25 - 0.15 && nearAngle > tipAngle + 0.1, "the heavy tip trails the thin flesh near the root: tip at "
            + tipAngle + ", near the root " + nearAngle + ", the path at " + Math.PI * 0.25);
        // Stopped, the tip carries on past where it should be, then swings back and settles.
        double most = -1.0;
        for (double t = 6.0; t <= 12.0; t += 0.25) {
            double[][] at = com.hex.kagune.control.pale.FleshPhysics.simulate(sweep, t, whip);
            most = Math.max(most, Math.atan2(at[at.length - 1][0], at[at.length - 1][2]));
        }
        check(most > Math.PI * 0.5 + 0.05, "it overshoots when the swing stops: " + most);
        double[][] settled = com.hex.kagune.control.pale.FleshPhysics.simulate(sweep, 24.0, whip);
        double[] end = settled[settled.length - 1];
        check(Math.hypot(end[0] - 4.0, end[2]) < 0.06, "and settles where it should be: " + java.util.Arrays.toString(end));
        // It bends, it does not stretch.
        double worst = 0.0;
        for (double t = 0.0; t <= 12.0; t += 0.5) {
            double[][] at = com.hex.kagune.control.pale.FleshPhysics.simulate(sweep, t, whip);
            for (int k = 1; k < at.length; k++) {
                double seg = Math.sqrt(Math.pow(at[k][0] - at[k - 1][0], 2) + Math.pow(at[k][1] - at[k - 1][1], 2) + Math.pow(at[k][2] - at[k - 1][2], 2));
                worst = Math.max(worst, Math.abs(seg / (4.0 / (whip.points() - 1)) - 1.0));
            }
        }
        check(worst <= 0.1201, "its segments keep their length: " + worst);
        // Held, its tip stays on what it holds, wherever that goes.
        double[][] held = com.hex.kagune.control.pale.FleshPhysics.simulate(sweep, 3.0, com.hex.kagune.control.pale.FleshPhysics.REACH.held(true));
        double[] heldTip = held[held.length - 1];
        check(Math.hypot(heldTip[0] - want[want.length - 1][0], heldTip[2] - want[want.length - 1][2]) < 1.0E-9, "a held tip stays on what it holds");
        // A blink (the root jumps blocks at once) sets it straight onto the path there.
        com.hex.kagune.control.pale.FleshPhysics.Chain chain = new com.hex.kagune.control.pale.FleshPhysics.Chain();
        chain.step(rod(0.0), 0.0, whip);
        chain.step(rod(0.0), 1.0, whip);
        double[][] moved = rod(0.0);
        for (double[] q : moved) {
            q[0] += 10.0;
        }
        double[][] after = chain.step(moved, 1.0, whip);
        check(Math.abs(after[after.length - 1][0] - 10.0) < 1.0E-9, "a blink sets it straight onto its path");
    }

    static void kick() {
        // From anything she may be doing, every frame of the Speed Kick keeps her arms out of her body, however long the dash.
        double worst = 0.0;
        String where = "";
        List<RigPose> froms = starts(PaleStrike.JAB);
        for (int dash : new int[] {PaleRules.KICK_DASH_MIN, 3, PaleRules.KICK_DASH}) {
            for (int s = 0; s < froms.size(); s += 3) {
                for (double age = 0.0; age <= PaleRules.kickArrive(dash) + PaleRules.KICK_TICKS + 0.001; age += 0.1) {
                    RigPose pose = PaleMotions.kick(age, dash, froms.get(s));
                    pose.settle(1.0F);
                    double clip = BodyFrame.clipping(pose, CLEARANCE);
                    if (clip > worst) {
                        worst = clip;
                        where = " (dash " + dash + ", from start " + s + ", tick " + String.format(java.util.Locale.ROOT, "%.1f", age) + ")";
                    }
                }
            }
        }
        check(worst == 0.0, "her arms stay out of her body all through the Speed Kick: " + worst + " px" + where);
        int dash = 3;
        int arrive = PaleRules.kickArrive(dash);
        // 6.2.0, the lunge (the user's stick figure): thrown far over forward, the right leg stretched out behind, the left knee up
        // under her, the right arm reaching out ahead.
        RigPose lunge = PaleMotions.kick(1.5, dash, null);
        lunge.settle(1.0F);
        double[] lungeHip = legPoint(lunge, 0.0);
        double[] crownLunge = BodyFrame.world(lunge, BodyFrame.model(lunge, "head", new double[] {0.0, -8.0, 0.0}));
        double[] backFoot = legPoint(lunge, 12.0);
        double[] reach = BodyFrame.world(lunge, BodyFrame.model(lunge, "rightArm", new double[] {-1.0, 9.0, 0.0}));
        check(crownLunge[2] > lungeHip[2] + 0.5 && backFoot[2] < lungeHip[2] - 0.45 && reach[2] > crownLunge[2] - 0.2,
            "the lunge: over forward, the leg out behind, the arm out ahead: crown " + java.util.Arrays.toString(crownLunge) + ", back foot "
                + java.util.Arrays.toString(backFoot) + ", fist " + java.util.Arrays.toString(reach));
        // Gone through the dash, nothing can hurt her from the lunge until she is there.
        check(!PaleRules.kickGone(dash, PaleRules.KICK_BURST - 0.01) && PaleRules.kickGone(dash, PaleRules.KICK_BURST)
            && PaleRules.kickGone(dash, arrive - 0.01) && !PaleRules.kickGone(dash, arrive), "gone from the end of the lunge until she is there");
        check(PaleRules.kickUntouchable(dash, 0.0) && PaleRules.kickUntouchable(dash, arrive - 0.01) && !PaleRules.kickUntouchable(dash, arrive),
            "untouchable from the lunge until she is there");
        // The blow (the user's second stick figure): her body thrown out flat in the air behind the leg, the leg dead straight and
        // level, the sole where the server has it, the left knee tucked up toward it.
        RigPose on = kickAt(dash, PaleRules.KICK_CONTACT);
        double[] foot = legPoint(on, 12.0);
        double[] hip = legPoint(on, 0.0);
        double rising = Math.toDegrees(Math.atan2(foot[1] - hip[1], Math.hypot(foot[0] - hip[0], foot[2] - hip[2])));
        check(on.rightKnee < 0.05 && Math.abs(rising) < 10.0, "the leg out dead straight and level at the blow: " + rising + " deg");
        check(Math.abs(foot[0] - PaleRules.KICK_FOOT_X) < 0.06 && Math.abs(foot[1] - PaleRules.KICK_FOOT_Y) < 0.06 && Math.abs(foot[2] - PaleRules.KICK_FOOT_Z) < 0.06,
            "the sole meets its middle where the server has it: " + java.util.Arrays.toString(foot));
        double[] crown = BodyFrame.world(on, BodyFrame.model(on, "head", new double[] {0.0, -8.0, 0.0}));
        check(crown[2] < hip[2] - 0.6 && Math.hypot(crown[0] - hip[0], crown[2] - hip[2]) > 0.9 && Math.abs(crown[1] - hip[1]) < 0.45,
            "laid out flat behind the leg: crown " + java.util.Arrays.toString(crown) + ", hip " + java.util.Arrays.toString(hip));
        double[] knee = BodyFrame.world(on, BodyFrame.model(on, "leftLeg", new double[] {0.0, 6.0, 0.0}));
        check(knee[2] > hip[2] + 0.15 && on.leftKnee > 1.8, "the left knee tucked up toward it: " + java.util.Arrays.toString(knee));
        // Driven out of the chamber, still speeding up as it lands.
        double before = distance(legPoint(kickAt(dash, 0.5), 12.0), legPoint(kickAt(dash, 0.75), 12.0));
        double into = distance(legPoint(kickAt(dash, 0.75), 12.0), foot);
        check(into > before, "the foot speeds up into the blow: " + before + " then " + into);
        // The rebound: a backflip, over backwards one way, ending upright.
        double flipped = kickAt(dash, PaleRules.KICK_CONTACT).flip;
        for (double since = PaleRules.KICK_HOLD; since <= PaleRules.KICK_LAND + 1.0E-6; since += 0.1) {
            double flip = kickAt(dash, since).flip;
            check(flip >= flipped - 1.0E-4, "over backwards one way (" + since + "): " + flip);
            flipped = flip;
        }
        check(Math.abs(flipped - Math.PI * 2.0) < 1.0E-3 && Math.abs(kickAt(dash, PaleRules.KICK_TUCK).flip - Math.PI) < 0.05,
            "upside down at the tuck, all the way round at the landing: " + flipped);
        RigPose done = PaleMotions.kick(arrive + PaleRules.KICK_TICKS, dash, null);
        check(Math.abs(PaleMotions.ended(done).flip) < 0.01 && Math.abs(PaleMotions.ended(done).spin) < 0.05, "she ends upright and square");
        // Never into the ground (a toe's corner may graze it), even kicking something so low she needs no lift; on her left sole while
        // she stands.
        double deepest = 0.0;
        for (int d : new int[] {PaleRules.KICK_DASH_MIN, PaleRules.KICK_DASH}) {
            for (double age = 0.0; age <= PaleRules.kickArrive(d) + PaleRules.KICK_TICKS; age += 0.25) {
                RigPose pose = PaleMotions.kick(age, d, null);
                pose.settle(1.0F);
                deepest = Math.min(deepest, lowestPoint(pose) + PaleRules.kickHeight(d, 0.0, age));
                if (PaleRules.kickAirborne(d, age) == 0.0 && !PaleRules.kickGone(d, age)) {
                    check(Math.abs(lowestLeftSole(pose)) < 0.01, "on her left sole while she stands (" + age + ")");
                }
            }
        }
        check(deepest > -0.08, "never into the ground: " + deepest);
        check(PaleRules.kickAirborne(dash, arrive) == 0.0 && PaleRules.kickAirborne(dash, arrive + PaleRules.KICK_CONTACT) == 1.0
            && PaleRules.kickAirborne(dash, arrive + PaleRules.KICK_OPEN) == 1.0 && PaleRules.kickAirborne(dash, arrive + PaleRules.KICK_LAND) == 0.0,
            "in the air from the blow until her feet come back under her");
        // Up off the ground through the blow, the rebound's hop, down by the landing.
        for (int ticks : new int[] {PaleRules.KICK_DASH_MIN, 3, PaleRules.KICK_DASH}) {
            int there = PaleRules.kickArrive(ticks);
            double rise = 0.0;
            for (double age = 0.0; age <= there + 1.0E-6; age += 0.1) {
                double now = PaleRules.kickRise(ticks, age);
                check(now >= rise - 1.0E-9, "she only goes up in the dash");
                check(age > PaleRules.KICK_BURST + ticks * (1.0 - PaleRules.KICK_SPRING) + 1.0E-6 || now < 1.0E-9, "on the ground for most of the dash (" + age + ")");
                rise = now;
            }
            check(rise == 1.0 && PaleRules.kickRise(ticks, there + PaleRules.KICK_CONTACT) == 1.0 && PaleRules.kickRise(ticks, there + PaleRules.KICK_LAND) == 0.0,
                "up at the end of the dash, held through the blow, then down");
            check(PaleRules.kickHop(ticks, there + PaleRules.KICK_HOLD) == 0.0 && PaleRules.kickHop(ticks, there + PaleRules.KICK_LAND) == 0.0
                && Math.abs(PaleRules.kickHop(ticks, there + (PaleRules.KICK_HOLD + PaleRules.KICK_LAND) / 2.0) - PaleRules.KICK_REBOUND) < 1.0E-9,
                "the rebound's hop: from the hold, highest midway, down at the landing");
        }
        check(Math.abs(PaleRules.kickLift(1.75) - (1.75 - PaleRules.KICK_FOOT_Y)) < 1.0E-9 && PaleRules.kickLift(0.3) == 0.0
            && PaleRules.kickLift(9.0) == PaleRules.KICK_LIFT_MAX, "she rises as high as puts the foot at its middle, never below the ground, never too far");
        // Where she kicks from: straight in front of it, the sole on its front.
        double half = 0.3;
        double[] stand = PaleRules.kickStand(half);
        check(Math.abs(stand[0] + PaleRules.KICK_FOOT_X) < 1.0E-9 && Math.abs(stand[1] + PaleRules.KICK_FOOT_Z + half) < 1.0E-9,
            "there, the sole meets the front of its middle");
        double[] flight = PaleRules.kickFlight();
        check(flight[0] == 0.0 && flight[1] == 1.0, "it flies straight away from her");
        check(PaleRules.kickCarried(PaleRules.KICK_SPEED, 10, 0) < PaleRules.KICK_SPEED && PaleRules.kickCarried(PaleRules.KICK_SPEED, 10, 0) > PaleRules.KICK_SPEED * 0.75
            && PaleRules.kickCarried(PaleRules.KICK_SPEED, 10, 20) < PaleRules.kickCarried(PaleRules.KICK_SPEED, 10, 0), "carried far, slowed by what it breaks");
        // The dash: from where she was to where she kicks from, smooth, slowing as she gets there, never faster than a server allows.
        check(PaleRules.KICK_RANGE == 20.0, "the Speed Kick reaches 20 blocks");
        double fastest = 0.0;
        for (double distance = 0.0; distance <= PaleRules.KICK_RANGE + 1.0; distance += 0.5) {
            int ticks = PaleRules.kickDash(distance);
            check(ticks >= PaleRules.KICK_DASH_MIN && ticks <= PaleRules.KICK_DASH, "a dash takes a few ticks, and never none");
            double[] to = {distance * 0.6, 0.0, distance * 0.8};
            double[] last = PaleRules.kickPlace(to, ticks, 1.0, 0.0);
            check(last[0] == 0.0 && last[2] == 0.0 && PaleRules.kickPlace(to, ticks, 1.0, PaleRules.KICK_BURST)[2] == 0.0, "where she was through the lunge");
            double lastStep = Double.MAX_VALUE;
            for (int tick = PaleRules.KICK_BURST + 1; tick <= PaleRules.kickArrive(ticks); tick++) {
                double[] at = PaleRules.kickPlace(to, ticks, 1.0, tick);
                double level = Math.hypot(at[0] - last[0], at[2] - last[2]);
                double step = Math.sqrt(level * level + (at[1] - last[1]) * (at[1] - last[1]));
                fastest = Math.max(fastest, step);
                check(level <= lastStep + 1.0E-6, "the dash slows as she gets there");
                lastStep = level;
                last = at;
            }
            check(Math.abs(last[0] - to[0]) < 1.0E-9 && Math.abs(last[2] - to[2]) < 1.0E-9 && Math.abs(last[1] - 1.0) < 1.0E-9,
                "the dash ends where she kicks from, risen");
        }
        check(fastest < 9.8, "never faster than a server lets a player move: " + fastest + " blocks a tick");
        check(PaleRules.kickPlace(new double[] {1.0, 0.0, 1.0}, 3, 1.0, PaleRules.kickArrive(3) + PaleRules.KICK_LAND) == null, "landed, she is her own again");
        check(Math.abs(PaleRules.kickPlace(new double[] {1.0, 0.0, 1.0}, 3, 1.2, PaleRules.kickArrive(3) + PaleRules.KICK_CONTACT)[1] - 1.2) < 1.0E-9, "up at the blow");
        // What it does to the body: the head snaps first, then the body goes round after it and over, tumbling; it comes down flat
        // and lies there, then gets up.
        KickTumble.Frame snap = KickTumble.at(0.7, Double.NaN);
        check(snap.head() > 0.9 * KickTumble.HEAD_SNAP && Math.abs(snap.twist()) < 0.25 && snap.tumble() < 0.05,
            "the head snaps round before the body follows: " + snap);
        check(KickTumble.at(0.0, Double.NaN).head() == 0.0, "nothing before the foot lands");
        double tumbled = -1.0;
        for (double t = 0.0; t <= 14.0; t += 0.25) {
            KickTumble.Frame frame = KickTumble.at(t, Double.NaN);
            check(frame.tumble() >= tumbled - 1.0E-9, "it tumbles over one way");
            tumbled = frame.tumble();
        }
        check(tumbled > Math.PI, "over and over in the air: " + tumbled);
        for (double landed : new double[] {1.0, 4.0, 9.0, 13.0, 20.0}) {
            KickTumble.Frame flat = KickTumble.at(landed + KickTumble.THUMP + 1.0, landed);
            check(flat.flat() > 0.999 && Math.abs(Math.sin(flat.tumble())) > 0.999, "it comes down flat (landed " + landed + "): " + flat);
            KickTumble.Frame still = KickTumble.at(landed + PaleRules.KICK_DOWN_TICKS - 0.5, landed);
            check(Math.abs(still.tumble() - flat.tumble()) < 1.0E-9, "and lies there");
            double end = landed + PaleRules.KICK_DOWN_TICKS + PaleRules.KICK_GETUP_TICKS;
            KickTumble.Frame up = KickTumble.at(end, landed);
            double turns = up.tumble() / (Math.PI * 2.0);
            double spins = up.twist() / (Math.PI * 2.0);
            check(Math.abs(turns - Math.round(turns)) < 1.0E-6 && Math.abs(spins - Math.round(spins)) < 1.0E-6 && up.head() == 0.0 && up.flat() < 1.0E-6,
                "then it is up on its feet as it was (landed " + landed + "): " + up);
            check(KickTumble.over(end - landed + landed, landed) && !KickTumble.over(end - 1.0, landed), "and done once up");
        }
    }

    /** 6.2.0, True Anonymity: the club grows on past its full size, spiked; reaches further, a wider stomp, a bigger crater. */
    static void trueAnonymity() {
        float full = PaleRules.hammerCharge(PaleRules.HAMMER_GROW_TICKS);
        float huge = PaleRules.hammerCharge(PaleRules.HAMMER_GROW_TICKS, true);
        check(full == 1.0F && Math.abs(huge - PaleRules.HAMMER_HUGE) < 1.0E-6 && PaleRules.hammerCharge(PaleRules.HAMMER_GROW_TICKS, false) == full
            && PaleRules.hammerCharge(9999.0, true) <= PaleRules.HAMMER_HUGE, "the club grows on to " + PaleRules.HAMMER_HUGE + " with it, no further");
        check(PaleRules.hammerLength(huge) > PaleRules.hammerLength(full) * 1.3 && PaleRules.hammerGirth(huge) > PaleRules.hammerGirth(full) * 1.3
            && PaleRules.hammerRadius(huge) > PaleRules.hammerRadius(full) * 1.25 && PaleRules.hammerCrater(huge) > PaleRules.hammerCrater(full) * 1.3
            && PaleRules.hammerReach(huge) > PaleRules.hammerReach(full) + 0.8 && PaleRules.hammerDamage(1000, huge) > PaleRules.hammerDamage(1000, full),
            "longer, thicker, a wider stomp, a bigger crater, further, harder");
        check(PaleRules.hammerCost(huge) == PaleRules.hammerCost(full), "no dearer");
        // Spiked: the same club with bone spikes all over it (more of it), every spike outside the flesh.
        RigPose raised = PaleMotions.hammerCharge(PaleRules.HAMMER_GROW_TICKS, null);
        PaleFleshModel.Pose plain = PaleFleshPose.club(raised, PaleMove.HAMMER_CHARGE, PaleRules.HAMMER_GROW_TICKS, huge, 30.0, 11, false);
        PaleFleshModel.Pose spiked = PaleFleshPose.club(raised, PaleMove.HAMMER_CHARGE, PaleRules.HAMMER_GROW_TICKS, huge, 30.0, 11, true);
        int[] quads = new int[2];
        PaleFleshModel.mesh((a, b, c, d, n, ca, cb, cc, cd) -> quads[0]++, plain);
        PaleFleshModel.mesh((a, b, c, d, n, ca, cb, cc, cd) -> quads[1]++, spiked);
        check(spiked.shape() == PaleFleshModel.Shape.CLUB_SPIKED && quads[1] >= quads[0] + 150 && quads[1] < 3600,
            "spikes all over the club, the huge club near the quad budget: " + quads[0] + " then " + quads[1]);
    }

    /** 6.2.0, Witness This: the full-charge punch's combo, laid out as every client and the server work it out. */
    static void witness() {
        check(com.hex.kagune.control.pale.WitnessRules.BEHIND < com.hex.kagune.control.pale.WitnessRules.UPPER
            && com.hex.kagune.control.pale.WitnessRules.UPPER < com.hex.kagune.control.pale.WitnessRules.FIRST_HIT
            && com.hex.kagune.control.pale.WitnessRules.ABOVE < com.hex.kagune.control.pale.WitnessRules.DROP
            && com.hex.kagune.control.pale.WitnessRules.DROP < com.hex.kagune.control.pale.WitnessRules.IMPACT
            && com.hex.kagune.control.pale.WitnessRules.DROP < com.hex.kagune.control.pale.WitnessRules.FREE && com.hex.kagune.control.pale.WitnessRules.FREE < com.hex.kagune.control.pale.WitnessRules.IMPACT
            && com.hex.kagune.control.pale.WitnessRules.IMPACT < com.hex.kagune.control.pale.WitnessRules.TICKS && com.hex.kagune.control.pale.WitnessRules.TICKS < com.hex.kagune.control.pale.WitnessRules.LONGEST, "behind it, the uppercut, the blitz, above it, down, she falls");
        // It goes up at least six blocks, hangs up there through the blitz, comes down onto the ground, never jumping a long way at once.
        double top = 0.0;
        double[] last = com.hex.kagune.control.pale.WitnessRules.target(0.0, 0.0);
        for (double age = 0.0; age <= com.hex.kagune.control.pale.WitnessRules.IMPACT; age += 0.25) {
            double[] at = com.hex.kagune.control.pale.WitnessRules.target(age, 0.0);
            top = Math.max(top, at[1]);
            check(distance(at, last) < 3.5, "it moves on its path, batted hard but never a jump (" + age + ")");
            last = at;
            check(com.hex.kagune.control.pale.WitnessRules.frozen(age) || age >= com.hex.kagune.control.pale.WitnessRules.IMPACT, "frozen all through");
        }
        check(top >= 6.0, "the uppercut throws it at least six blocks up: " + top);
        check(Math.abs(com.hex.kagune.control.pale.WitnessRules.target(com.hex.kagune.control.pale.WitnessRules.IMPACT, -0.5)[1] + 0.5) < 1.0E-9
            && !com.hex.kagune.control.pale.WitnessRules.frozen(com.hex.kagune.control.pale.WitnessRules.IMPACT), "down on the ground at the crater, and free");
        // She: where she punched until she is behind it; at every blitz blow beside it, each from the other side to the last; above it
        // for the kick; her own again once she lands. Always facing it.
        check(com.hex.kagune.control.pale.WitnessRules.her(com.hex.kagune.control.pale.WitnessRules.BEHIND - 0.5, 0.0) == null
            && com.hex.kagune.control.pale.WitnessRules.her(com.hex.kagune.control.pale.WitnessRules.FREE, 0.0) == null, "her own before and after");
        // 6.2.1: she kicks off it high in the air and falls out of the sky (never put back down on the ground).
        check(com.hex.kagune.control.pale.WitnessRules.her(com.hex.kagune.control.pale.WitnessRules.FREE - 0.01, 0.0)[1] > 5.0, "she is high in the air when she is her own again: " + com.hex.kagune.control.pale.WitnessRules.her(com.hex.kagune.control.pale.WitnessRules.FREE - 0.01, 0.0)[1]);
        // The blitz: a storm of punches and kicks from all round a sphere about it, above and below too, each from about the far side
        // of the last; what she strikes with meets its middle; turned to face it.
        double[] previous = null;
        Set<com.hex.kagune.control.pale.WitnessRules.Blow> kinds = EnumSet.noneOf(com.hex.kagune.control.pale.WitnessRules.Blow.class);
        boolean above = false;
        boolean below = false;
        for (int hit = 0; hit < com.hex.kagune.control.pale.WitnessRules.HITS; hit++) {
            double tick = com.hex.kagune.control.pale.WitnessRules.hitAt(hit);
            double[] her = com.hex.kagune.control.pale.WitnessRules.her(tick, 0.0);
            double[] it = com.hex.kagune.control.pale.WitnessRules.target(tick - 1.0E-6, 0.0);
            com.hex.kagune.control.pale.WitnessRules.Blow blow = com.hex.kagune.control.pale.WitnessRules.blow(hit);
            kinds.add(blow);
            double[] striking = {her[0], her[1] + blow.reach(), her[2]};
            double[] middle = {it[0], it[1] + com.hex.kagune.control.pale.WitnessRules.MID, it[2]};
            check(Math.abs(distance(striking, middle) - com.hex.kagune.control.pale.WitnessRules.STAND) < 1.0E-4, "she strikes its middle for blow " + hit);
            double[] side = com.hex.kagune.control.pale.WitnessRules.around(hit);
            above |= side[1] > 0.7;
            below |= side[1] < -0.7;
            double level = Math.hypot(side[0], side[2]);
            if (level > 0.3) {
                double facing = com.hex.kagune.control.pale.WitnessRules.facing(tick);
                check((-side[0] * -Math.sin(facing) + -side[2] * Math.cos(facing)) / level > 0.99, "facing it for blow " + hit);
            }
            if (previous != null) {
                check(side[0] * previous[0] + side[1] * previous[1] + side[2] * previous[2] < 0.0, "each blow from about the far side of the last (" + hit + ")");
            }
            previous = side;
            check(com.hex.kagune.control.pale.WitnessRules.blinks((int)Math.ceil(tick - com.hex.kagune.control.pale.WitnessRules.windUp(hit))), "she blinks to each blow");
            check(com.hex.kagune.control.pale.WitnessRules.windUp(hit) < (hit == 0 ? com.hex.kagune.control.pale.WitnessRules.FIRST_HIT - com.hex.kagune.control.pale.WitnessRules.UPPER : com.hex.kagune.control.pale.WitnessRules.gap(hit - 1)) - 0.5
                && com.hex.kagune.control.pale.WitnessRules.hit(tick - com.hex.kagune.control.pale.WitnessRules.windUp(hit) + 0.01) == hit && com.hex.kagune.control.pale.WitnessRules.hit(tick - 0.01) == hit, "she is there for each blow, its own wind-up before it lands (" + hit + ")");
            // 6.2.3: stopped dead on the blow, then it flies, there before the next.
            double[] struck = com.hex.kagune.control.pale.WitnessRules.target(tick + com.hex.kagune.control.pale.WitnessRules.hitStop(hit) - 0.01, 0.0);
            double[] there = com.hex.kagune.control.pale.WitnessRules.target(tick + com.hex.kagune.control.pale.WitnessRules.gap(hit) - 0.01, 0.0);
            double[] settled = com.hex.kagune.control.pale.WitnessRules.target(tick + com.hex.kagune.control.pale.WitnessRules.gap(hit) - 0.02, 0.0);
            check(tick < com.hex.kagune.control.pale.WitnessRules.UPPER + com.hex.kagune.control.pale.WitnessRules.CLIMB || distance(struck, com.hex.kagune.control.pale.WitnessRules.target(tick + 0.01, 0.0)) < 1.0E-6 && distance(there, settled) < 1.0E-6
                && distance(struck, there) > 2.0, "a hit stop, then knocked far, at rest before the next (" + hit + ")");
            double[] knock = com.hex.kagune.control.pale.WitnessRules.knock(hit);
            check(knock[0] * side[0] + knock[1] * side[1] + knock[2] * side[2] < -0.999, "each blow knocks it away from her");
        }
        Set<Integer> paces = new java.util.HashSet<>();
        int kicks = 0;
        for (int hit = 0; hit < com.hex.kagune.control.pale.WitnessRules.HITS; hit++) {
            paces.add(com.hex.kagune.control.pale.WitnessRules.gap(hit));
            kicks += com.hex.kagune.control.pale.WitnessRules.blow(hit).kick() ? 1 : 0;
            check(com.hex.kagune.control.pale.WitnessRules.gap(hit) >= 2 && com.hex.kagune.control.pale.WitnessRules.gap(hit) <= 9 && (hit == 0 || com.hex.kagune.control.pale.WitnessRules.gap(hit) != com.hex.kagune.control.pale.WitnessRules.gap(hit - 1)), "never the same pace twice running (" + hit + ")");
            check(com.hex.kagune.control.pale.WitnessRules.heavy(hit) == com.hex.kagune.control.pale.WitnessRules.hitStop(hit) > 1.0 && com.hex.kagune.control.pale.WitnessRules.power(hit) >= 0.5, "the heavy blows stop dead longest (" + hit + ")");
        }
        check(paces.size() >= 6 && kicks >= 8 && kinds.contains(com.hex.kagune.control.pale.WitnessRules.Blow.ROUNDHOUSE) && kinds.contains(com.hex.kagune.control.pale.WitnessRules.Blow.SPIN_KICK)
            && kinds.contains(com.hex.kagune.control.pale.WitnessRules.Blow.AXE_KICK) && kinds.contains(com.hex.kagune.control.pale.WitnessRules.Blow.FLYING_KNEE), "every pace, and kicks of every kind: " + paces + ", " + kicks + " kicks");
        check(com.hex.kagune.control.pale.WitnessRules.HITS >= 20 && kinds.size() >= 6
            && kinds.contains(com.hex.kagune.control.pale.WitnessRules.Blow.SIDE_KICK) && kinds.contains(com.hex.kagune.control.pale.WitnessRules.Blow.STOMP)
            && above && below, "a storm: twenty blows and more, one every few ticks (each one seen), punches and kicks, from above and below too");
        check(com.hex.kagune.control.pale.WitnessRules.FIRST_HIT - com.hex.kagune.control.pale.WitnessRules.UPPER <= 3, "no pause after the uppercut: after it at once");
        double[] lo = {1.0E9, 1.0E9};
        double[] hi = {-1.0E9, -1.0E9};
        for (double age = com.hex.kagune.control.pale.WitnessRules.FIRST_HIT; age < com.hex.kagune.control.pale.WitnessRules.DROP; age += 0.25) {
            double[] at = com.hex.kagune.control.pale.WitnessRules.target(age, 0.0);
            check(Math.abs(at[0]) < 30.0 && Math.abs(at[2] - com.hex.kagune.control.pale.WitnessRules.KNOCK_BACK) < 30.0 && at[1] > 5.0
                && at[1] < com.hex.kagune.control.pale.WitnessRules.RISE + 20.0, "batted all about the sky, never off out of it nor into the ground (" + age + ")");
            lo[0] = Math.min(lo[0], at[0]);
            lo[1] = Math.min(lo[1], at[2]);
            hi[0] = Math.max(hi[0], at[0]);
            hi[1] = Math.max(hi[1], at[2]);
        }
        // 6.2.1: a big sphere: it is knocked about blocks and blocks, not a nudge.
        check(hi[0] - lo[0] > 14.0 && hi[1] - lo[1] > 18.0, "batted all over a big sphere: " + (hi[0] - lo[0]) + " by " + (hi[1] - lo[1]));
        double[] over = com.hex.kagune.control.pale.WitnessRules.her(com.hex.kagune.control.pale.WitnessRules.DROP, 0.0);
        double[] under = com.hex.kagune.control.pale.WitnessRules.target(com.hex.kagune.control.pale.WitnessRules.DROP, 0.0);
        check(over[1] > under[1] + 1.5 && Math.hypot(over[0] - under[0], over[2] - under[2]) < 1.0E-6, "right above it for the kick down");
        // Her arms never into her body through any of it.
        double worst = 0.0;
        for (double age = 0.0; age <= com.hex.kagune.control.pale.WitnessRules.TICKS; age += 0.25) {
            RigPose pose = PaleMotions.witness(age, null);
            pose.settle(1.0F);
            worst = Math.max(worst, BodyFrame.clipping(pose, CLEARANCE));
        }
        check(worst == 0.0, "her arms stay out of her body all through Witness This: " + worst + " px");
        check(com.hex.kagune.control.pale.PaleMutation.WITNESS_THIS.ordinal() == 3 && com.hex.kagune.control.pale.PaleMutation.earned(1000) == 4
            && !PaleMove.WITNESS.owl() && PaleMove.OWL_VOLLEY.owl(), "a fourth mutation for a fourth point; the combo is not one of the Owl's moves");
    }

    static void watch() {
        // Her cross: from anything she may be doing, her arms stay out of her body; at the burst they are flung out wide.
        double worst = 0.0;
        String where = "";
        List<RigPose> froms = starts(PaleStrike.JAB);
        for (int s = 0; s < froms.size(); s += 3) {
            for (double age = 0.0; age <= WatchRules.POSE_TICKS + 0.001; age += 0.1) {
                RigPose pose = PaleMotions.watch(age, froms.get(s));
                pose.settle(1.0F);
                double clip = BodyFrame.clipping(pose, CLEARANCE);
                if (clip > worst) {
                    worst = clip;
                    where = " (from start " + s + ", tick " + String.format(java.util.Locale.ROOT, "%.1f", age) + ")";
                }
            }
        }
        check(worst == 0.0, "her arms stay out of her body all through her cross: " + worst + " px" + where);
        RigPose cross = PaleMotions.watch(8.0, null);
        cross.settle(1.0F);
        double[] rightFist = BodyFrame.world(cross, BodyFrame.model(cross, "rightArm", new double[] {-1.0, 9.0, 0.0}));
        double[] leftFist = BodyFrame.world(cross, BodyFrame.model(cross, "leftArm", new double[] {1.0, 9.0, 0.0}));
        check(rightFist[0] > 0.7 && leftFist[0] < -0.7 && rightFist[1] > 1.05 && leftFist[1] > 1.05,
            "her arms are flung out into a cross: " + java.util.Arrays.toString(rightFist) + " " + java.util.Arrays.toString(leftFist));
        check(WatchRules.ACTIVE == 200 && WatchRules.ERUPT_AT + WatchRules.ERUPT < WatchRules.POSE_TICKS, "out ten seconds, bursting out as she throws her arms wide");
        // The tentacles never go through her (trunk, knot, cords, beak), whatever they do, on her stance or her cross.
        RigPose stance = PaleMotions.guard();
        stance.settle(1.0F);
        worst = 0.0;
        where = "";
        double[][] marks = {{0.0, 1.1, 3.0}, {2.5, 0.9, 1.5}, {-2.5, 0.9, 1.5}, {4.0, 1.4, -1.0}, {-4.0, 1.4, -1.0}, {0.5, 0.4, 1.2}, {-0.6, 2.4, 2.0},
            {1.5, 1.0, -2.5}, {0.0, 1.0, 7.0}};
        for (RigPose body : new RigPose[] {stance, cross}) {
            for (int side : new int[] {1, -1}) {
                for (double time = 0.0; time < 60.0; time += 7.0) {
                    worst = Math.max(worst, depth(body, side, WatchPose.tip(side, WatchRules.Phase.IDLE, 0.0, 3, null, null, time), time));
                }
                for (double age = 0.0; age <= WatchRules.ERUPT; age += 1.0) {
                    worst = Math.max(worst, depth(body, side, WatchPose.tip(side, WatchRules.Phase.OUT, age, 3, null, null, 5.0), 5.0));
                }
                for (double[] mark : marks) {
                    int lunge = WatchRules.lungeTicks(distance(mark, WatchRules.pivot(side)));
                    for (double age = 0.0; age <= WatchRules.strikeTicks(lunge); age += 0.5) {
                        double clip = depth(body, side, WatchPose.tip(side, WatchRules.Phase.STRIKE, age, lunge, mark, null, age), age);
                        if (clip > worst) {
                            worst = clip;
                            where = " (striking at " + java.util.Arrays.toString(mark) + ", side " + side + ", tick " + age + ")";
                        }
                    }
                    for (double u = 0.0; u <= 1.0; u += 0.1) {
                        double[] carried = WatchRules.carry(u, mark, side);
                        double clip = depth(body, side, WatchPose.tip(side, WatchRules.Phase.CARRY, u * 10.0, lunge, carried, null, u), u);
                        if (clip > worst) {
                            worst = clip;
                            where = " (carrying from " + java.util.Arrays.toString(mark) + ", side " + side + ", u " + u + ")";
                        }
                    }
                    double[] end = WatchRules.carry(1.0, mark, side);
                    for (double age = 0.0; age <= WatchRules.RECOVER; age += 1.0) {
                        worst = Math.max(worst, depth(body, side, WatchPose.tip(side, WatchRules.Phase.RECOVER, age, lunge, null, end, age), age));
                    }
                }
            }
        }
        check(worst == 0.0, "the tentacles never go through her: " + worst + " px" + where);
        // The beak goes in through the chest and out of the back by THROUGH, pointing away from her.
        double[] chest = {0.8, 1.2, 2.4};
        WatchPose.Tip held = WatchPose.tip(1, WatchRules.Phase.CARRY, 4.0, 3, chest, null, 0.0);
        double[] point = {held.knot()[0] + held.dir()[0] * WatchRules.BEAK, held.knot()[1] + held.dir()[1] * WatchRules.BEAK,
            held.knot()[2] + held.dir()[2] * WatchRules.BEAK};
        check(Math.abs(distance(point, chest) - WatchRules.THROUGH) < 1.0E-9 && held.hold() == 1.0, "the beak is through the chest and out of the back");
        // The lunge: a coil back (away from what it strikes), then faster and faster into it.
        double[] mark = {0.0, 1.1, 3.0};
        int lunge = WatchRules.lungeTicks(distance(mark, WatchRules.pivot(1)));
        double[] rested = WatchPose.idle(1, 0.0).knot();
        double[] reared = WatchPose.tip(1, WatchRules.Phase.STRIKE, WatchRules.REAR, lunge, mark, null, 0.0).knot();
        check(distance(reared, mark) > distance(rested, mark), "it rears back before it strikes");
        double[] a = WatchPose.tip(1, WatchRules.Phase.STRIKE, WatchRules.REAR + lunge * 0.3, lunge, mark, null, 0.0).knot();
        double[] b = WatchPose.tip(1, WatchRules.Phase.STRIKE, WatchRules.REAR + lunge * 0.6, lunge, mark, null, 0.0).knot();
        double[] c = WatchPose.tip(1, WatchRules.Phase.STRIKE, WatchRules.REAR + lunge * 0.9, lunge, mark, null, 0.0).knot();
        check(distance(b, c) > distance(a, b) * 1.3, "the lunge speeds up into the blow");
        // The carry: up over her and out to the tentacle's own side, never down through the ground or her body; thrown on at the
        // sweep's own speed, out to that side.
        for (int side : new int[] {1, -1}) {
            for (double[] from : marks) {
                double lowest = Double.MAX_VALUE;
                double nearest = Double.MAX_VALUE;
                for (double u = 0.0; u <= 1.0; u += 0.02) {
                    double[] at = WatchRules.carry(u, from, side);
                    lowest = Math.min(lowest, at[1]);
                    if (u > 0.15) {
                        nearest = Math.min(nearest, Math.hypot(at[0], at[2]));
                    }
                }
                check(lowest > Math.min(0.35, from[1] - 1.0E-9), "carried up, never down into the ground: " + lowest);
                check(nearest > 0.8, "carried round her, never through her: " + nearest);
                double[] v = WatchRules.throwVelocity(from, side);
                double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
                check(v[0] * side > 0.8 && speed > 1.2 && speed < 3.5 && v[1] > -0.6, "thrown out to the side (" + side + ") at the sweep's speed: "
                    + java.util.Arrays.toString(v));
            }
        }
    }

    static void arm() {
        // 6.2.0: the hand sweeps everything it went past since last tick: tick by tick, the sweeps leave no gap from her shoulder out
        // to the full reach (a body close to her, between two ticks' reach, is never missed), tapped or charged.
        for (double to : new double[] {ArmRules.TAP_REACH, ArmRules.reach(1.0)}) {
            double covered = 0.0;
            double last = 0.0;
            for (int age = 0; age <= ArmRules.SHOOT_TICKS; age++) {
                double now = ArmRules.shot(age, to);
                check((age <= 1 ? 0.0 : last) <= covered + 1.0E-9, "the sweep starts where the last one ended (reach " + to + ", tick " + age + ")");
                covered = Math.max(covered, now);
                last = now;
            }
            check(Math.abs(covered - to) < 1.0E-9, "the sweeps cover the whole way out: " + covered + " of " + to);
        }
        double[] shoulder = {0.33, 1.32, 0.0};
        double[] ahead = {0.0, 0.0, 1.0};
        double[] close = {0.0, 0.9, 2.2};
        double swept = ArmRules.along(shoulder, ahead, close, 0.0, ArmRules.shot(1, ArmRules.TAP_REACH));
        check(Math.abs(swept - 2.2) < 1.0E-9 && ArmRules.off(shoulder, ahead, close, swept) < ArmRules.CATCH + 0.3 + 0.54,
            "a body standing close in front of her is caught on the first tick: " + swept + " out, " + ArmRules.off(shoulder, ahead, close, swept) + " off");
        check(ArmRules.along(shoulder, ahead, new double[] {0.0, 1.0, 30.0}, 6.0, 11.0) == 11.0
            && ArmRules.along(shoulder, ahead, new double[] {0.0, 1.0, -3.0}, 6.0, 11.0) == 6.0, "only what the hand swept this tick");
        // Its rules: a tap's reach out to the full charge's; it swells as it is held; every move ends where the next begins.
        check(ArmRules.reach(0.0) == ArmRules.TAP_REACH && ArmRules.reach(1.0) == ArmRules.MAX_REACH && ArmRules.TAP_REACH == 20.0
            && ArmRules.MAX_REACH == 35.0, "a tap reaches 20 blocks, the full charge 35");
        check(ArmRules.charge(ArmRules.CHARGE_TICKS) == 1.0 && ArmRules.charge(0.0) == 0.0, "charged over three seconds");
        boolean swelling = true;
        for (double c = 0.0; c < 1.0; c += 0.05) {
            swelling &= ArmRules.girth(c + 0.05) > ArmRules.girth(c);
        }
        check(swelling && ArmRules.girth(1.0) > ArmRules.girth(0.0) * 1.8, "held, it keeps on swelling: " + ArmRules.girth(0.0) + " to " + ArmRules.girth(1.0));
        check(Math.abs(ArmRules.shot(ArmRules.SHOOT_TICKS, 20.0) - 20.0) < 1.0E-9 && ArmRules.shot(0.0, 20.0) == ArmRules.ARM,
            "shot out from her arm's length to its reach");
        check(Math.abs(ArmRules.dragged(ArmRules.DRAG_TICKS, 17.0, 0.0) - ArmRules.DRAG_TO) < 1.0E-9 && ArmRules.dragged(0.0, 17.0, 0.0) == 17.0,
            "dragged back from where it got to, up to her");
        check(Math.abs(ArmRules.retracted(ArmRules.RETRACT_TICKS, 30.0, ArmRules.RETRACT_TICKS) - ArmRules.ARM) < 1.0E-9,
            "drawn all the way back in to her arm's length");
        double[] start = ArmRules.sphere(0.0);
        double[] end = ArmRules.sphere(ArmRules.SPHERE_TICKS);
        check(start[0] == 0.0 && end[0] == 0.0 && ArmRules.sphere(ArmRules.SPHERE_FORM)[0] == 1.0, "the sphere folds shut, then opens");
        for (int i = 0; i < ArmRules.SQUEEZES; i++) {
            int tick = ArmRules.squeezeTick(i);
            double[] at = ArmRules.sphere(tick);
            check(at[0] == 1.0 && at[1] > 0.85, "squeeze " + i + " hurts as it slams shut (tick " + tick + "): " + at[1]);
        }
        double[] d = {0.0, 0.0, 1.0};
        double[] look = {1.0, 0.0, 0.0};
        double[] first = ArmRules.follow(d, look);
        for (int t = 0; t < 20; t++) {
            d = ArmRules.follow(d, look);
        }
        check(first[0] < 0.6 && d[0] > 0.999, "it swings after where she looks, lagging with its weight");
        // As she sees it: each move carries on from where the last one ended (how far out, how thick), never a jump.
        for (double heldBefore : new double[] {0.0, PaleRules.HOLD_TICKS}) {
            ArmPose.Reach shot = ArmPose.reach(ArmRules.Phase.SHOOT, ArmRules.SHOOT_TICKS, ArmRules.ARM, 0.0, heldBefore, false, false);
            if (heldBefore > 0.0) {
                ArmPose.Reach hold = ArmPose.reach(ArmRules.Phase.HOLD, 0.0, shot.length(), 0.0, heldBefore + ArmRules.SHOOT_TICKS, false, false);
                check(Math.abs(hold.length() - shot.length()) < 1.0E-9 && Math.abs(hold.girth() - shot.girth()) < 1.0E-9,
                    "held, the shot carries straight on into the hold: " + shot + " " + hold);
            } else {
                ArmPose.Reach drag = ArmPose.reach(ArmRules.Phase.DRAG, 0.0, shot.length(), 0.0, 0.0, true, false);
                check(Math.abs(drag.length() - shot.length()) < 1.0E-9 && Math.abs(drag.girth() - shot.girth()) < 1.0E-9,
                    "tapped, the shot turns straight into the drag: " + shot + " " + drag);
            }
        }
        for (double charge : new double[] {0.0, 0.4, 0.9}) {
            double held = charge * ArmRules.CHARGE_TICKS;
            ArmPose.Reach hold = ArmPose.reach(ArmRules.Phase.HOLD, 3.0, 0.0, 0.0, held - 3.0, true, false);
            ArmPose.Reach drag = ArmPose.reach(ArmRules.Phase.DRAG, 0.0, hold.length(), ArmRules.charge(held), 0.0, true, false);
            ArmPose.Reach dragged = ArmPose.reach(ArmRules.Phase.DRAG, ArmRules.DRAG_TICKS, hold.length(), ArmRules.charge(held), 0.0, true, false);
            ArmPose.Reach back = ArmPose.reach(ArmRules.Phase.RETRACT, 0.0, dragged.length(), ArmRules.charge(held), 0.0, false, false);
            check(Math.abs(drag.length() - hold.length()) < 1.0E-9 && Math.abs(drag.girth() - hold.girth()) < 1.0E-9
                && Math.abs(back.length() - dragged.length()) < 1.0E-9 && Math.abs(back.girth() - dragged.girth()) < 1.0E-9,
                "let go at charge " + charge + ": hold, drag and drawing in run on from each other");
        }
        ArmPose.Reach full = ArmPose.reach(ArmRules.Phase.HOLD, 0.0, 0.0, 0.0, ArmRules.CHARGE_TICKS, true, false);
        ArmPose.Reach sphere = ArmPose.reach(ArmRules.Phase.SPHERE, 0.0, full.length(), 1.0, 0.0, true, true);
        ArmPose.Reach in = ArmPose.reach(ArmRules.Phase.RETRACT, ArmRules.RETRACT_TICKS, sphere.length(), 1.0, 0.0, false, true);
        check(Math.abs(sphere.girth() - full.girth()) < 1.0E-9 && full.length() == ArmRules.MAX_REACH, "at the full charge it is out 35 blocks, as thick as it gets");
        check(Math.abs(in.length() - ArmRules.ARM) < 1.0E-9 && in.girth() < ArmRules.girth(0.0), "drawn back in, the mutation is gone: " + in);
        double[] hanging = ArmPose.aimNow(new double[] {0.0, 0.0, 1.0}, in.length());
        double[] shotOut = ArmPose.aimNow(new double[] {0.0, 0.0, 1.0}, ArmRules.shot(1.0, ArmRules.TAP_REACH));
        check(hanging[1] < -0.98 && shotOut[2] > 0.999, "as short as her arm it hangs at her side where hers is; shot out, it points where she aims: "
            + java.util.Arrays.toString(hanging));
        double[] root = {0.3, 1.4, 0.0};
        double[] body = {0.5, 1.2, 9.0};
        check(Math.abs(distance(ArmPose.holding(root, body, 0.4), body) - ArmPose.handBack(0.4)) < 1.0E-9,
            "its hand is round what it holds");
        double[] outEnd = ArmPose.out(root, new double[] {0.0, 0.0, 1.0}, 9.0, 0.4);
        check(Math.abs(distance(outEnd, new double[] {0.3, 1.4, 9.0}) - ArmPose.handBack(0.4)) < 1.0E-9,
            "reaching, its hand is where it catches (nothing jumps as it closes on something)");
        check(ArmRules.dragTo(0.0) == ArmRules.DRAG_TO && ArmRules.fits(ArmRules.dragTo(0.95)) >= ArmRules.girth(0.95) - 1.0E-9
            && ArmRules.fits(ArmRules.dragTo(0.0)) >= ArmRules.girth(0.0) - 1.0E-9, "dragged in, its hand stops short of her however swollen");
        check(ArmRules.fits(ArmRules.sphereOut(1.0, 1.8)) > 0.2 && ArmRules.sphereOut(30.0, 1.8) == 30.0, "the sphere is never on top of her");
        // Its weight squares her up to it: her body turns after it, never more than SQUARE from it.
        float facing = 0.0F;
        double[] left = {1.0, 0.0, 0.0};
        float firstTurn = ArmPose.squared(facing, left);
        for (int t = 0; t < 30; t++) {
            facing = ArmPose.squared(facing, left);
        }
        check(Math.abs(firstTurn - (-90.0F)) <= ArmPose.SQUARE + 1.0E-3 && Math.abs(facing - (-90.0F)) < 0.5F, "her body turns after the arm: " + firstTurn + ", " + facing);
        // Her own arm stays hers: pointed out the way the flesh goes while it is out (thrown up as it shoots, lowered as it comes back
        // in), and the flesh bursts out all round it from above her elbow, down her forearm and on out of her fist.
        for (double yaw = -ArmPose.SQUARE * 2.0; yaw <= ArmPose.SQUARE * 2.0; yaw += ArmPose.SQUARE) {
            for (double pitch = -60.0; pitch <= 60.0; pitch += 30.0) {
                double y = Math.toRadians(yaw);
                double x = Math.toRadians(pitch);
                double[] sight = {Math.sin(y) * Math.cos(x), -Math.sin(x), Math.cos(y) * Math.cos(x)};
                for (ArmRules.Phase phase : new ArmRules.Phase[] {ArmRules.Phase.HOLD, ArmRules.Phase.DRAG, ArmRules.Phase.SPHERE}) {
                    ArmPose.Reach reach = ArmPose.reach(phase, 4.0, 18.0, 0.5, 20.0, phase == ArmRules.Phase.DRAG, false);
                    RigPose pose = PaleMotions.guard();
                    ArmPose.body(pose, phase, 4.0, reach, sight, false, 4.0);
                    pose.settle(1.0F);
                    double[] way = ArmPose.way(pose);
                    double[] meant = ArmPose.reachable(sight, pose.spin, pose.twist);
                    double off = Math.toDegrees(Math.acos(Math.min(1.0, way[0] * meant[0] + way[1] * meant[1] + way[2] * meant[2])));
                    // (Unless pointing there would bring her fist or her forearm across her face: then as near as it can.)
                    boolean face = ArmPose.faceBound(pose);
                    check((off < 3.0 || face && off < 30.0) && pose.rightArmHold == 1.0F, "her arm points where the flesh goes (" + phase + ", yaw " + yaw
                        + ", pitch " + pitch + "): " + off + " deg off");
                    check(Math.abs(yaw) > 1.0 || pitch < 0.0 || pitch > 31.0 || off < 3.0, "straight ahead, level or a little down, right where it goes (" + phase + ", pitch " + pitch + "): "
                        + off + " deg off");
                    check(Math.abs(yaw) > ArmPose.SQUARE + 1.0 || pitch < -20.0 || meant == sight, "within her reach it points right where it goes");
                    double[] from = ArmPose.shoulder(pose);
                    double girth = ArmPose.girth(reach.girth(), reach.length());
                    PaleFleshModel.Pose flesh = ArmPose.pose(pose, ArmPose.out(from, sight, reach.length(), girth), girth, reach.hand(), 4.0, 7);
                    check(flesh != null && distance(flesh.path()[0], ArmPose.root(pose)) < 0.05, "the flesh bursts out round her arm above the elbow");
                    if (flesh != null) {
                        // Down her forearm, round her fist, on out of it.
                        com.hex.kagune.control.ukaku.KakujaModel.Spine spine = PaleFleshModel.trunk(flesh);
                        double[] fist = ArmPose.fist(pose);
                        double nearest = Double.MAX_VALUE;
                        for (double[] at : spine.p()) {
                            nearest = Math.min(nearest, distance(at, fist));
                        }
                        check(nearest < 0.08, "it runs down round her forearm and out of her fist: " + nearest);
                    }
                }
            }
        }
        check(ArmPose.pointing(ArmRules.Phase.SHOOT, 0.0, false) == 0.0 && ArmPose.pointing(ArmRules.Phase.SHOOT, 2.0, false) == 1.0
            && ArmPose.pointing(ArmRules.Phase.HOLD, 30.0, false) == 1.0 && ArmPose.pointing(ArmRules.Phase.RETRACT, ArmRules.RETRACT_QUICK, false) == 0.0
            && ArmPose.pointing(ArmRules.Phase.RETRACT, ArmRules.RETRACT_TICKS * 0.5, true) == 1.0 && ArmPose.pointing(ArmRules.Phase.NONE, 0.0, false) == 0.0,
            "thrown up as it shoots, held out, lowered as it comes back in");
        // Never through her: out round her arm, everything it does, every way she can point it (her body squared up to it: as much as it
        // may be off, twice over).
        double worst = 0.0;
        String where = "";
        List<double[]> states = new ArrayList<>();
        // {phase, age, from, charge, held before, after the sphere}
        for (double age = 0.0; age <= ArmRules.SHOOT_TICKS; age += 1.0) {
            states.add(new double[] {ArmRules.Phase.SHOOT.ordinal(), age, ArmRules.ARM, 0.0, 0.0, 0.0});
            states.add(new double[] {ArmRules.Phase.SHOOT.ordinal(), age, ArmRules.ARM, 0.0, PaleRules.HOLD_TICKS, 0.0});
        }
        for (double age = 0.0; age <= ArmRules.CHARGE_TICKS; age += 8.0) {
            states.add(new double[] {ArmRules.Phase.HOLD.ordinal(), age, 0.0, 0.0, PaleRules.HOLD_TICKS + ArmRules.SHOOT_TICKS, 0.0});
        }
        for (double charge : new double[] {0.0, 0.5, 0.95}) {
            for (double age = 0.0; age <= ArmRules.DRAG_TICKS; age += 1.5) {
                states.add(new double[] {ArmRules.Phase.DRAG.ordinal(), age, 18.0, charge, 0.0, 0.0});
            }
            for (double age = 0.0; age <= ArmRules.RETRACT_QUICK; age += 1.0) {
                states.add(new double[] {ArmRules.Phase.RETRACT.ordinal(), age, ArmRules.dragTo(charge), charge, 0.0, 0.0});
            }
        }
        for (double age = 0.0; age <= ArmRules.RETRACT_TICKS; age += 1.5) {
            states.add(new double[] {ArmRules.Phase.RETRACT.ordinal(), age, ArmRules.MAX_REACH, 1.0, 0.0, 1.0});
            states.add(new double[] {ArmRules.Phase.RETRACT.ordinal(), age, ArmRules.sphereOut(1.0, 1.2), 1.0, 0.0, 1.0});
        }
        for (double[] state : states) {
            ArmRules.Phase phase = ArmRules.Phase.byOrdinal((int)state[0]);
            boolean afterSphere = state[5] > 0.0;
            ArmPose.Reach reach = ArmPose.reach(phase, state[1], state[2], state[3], state[4], phase == ArmRules.Phase.DRAG, afterSphere);
            for (double yaw = -ArmPose.SQUARE * 2.0; yaw <= ArmPose.SQUARE * 2.0; yaw += ArmPose.SQUARE) {
                for (double pitch = -60.0; pitch <= 60.0; pitch += 30.0) {
                    double y = Math.toRadians(yaw);
                    double x = Math.toRadians(pitch);
                    double[] sight = {Math.sin(y) * Math.cos(x), -Math.sin(x), Math.cos(y) * Math.cos(x)};
                    RigPose pose = PaleMotions.guard();
                    ArmPose.body(pose, phase, state[1], reach, sight, afterSphere, state[1]);
                    pose.settle(1.0F);
                    double[] way = ArmPose.aimNow(sight, reach.length(), ArmPose.way(pose));
                    double[] from = ArmPose.shoulder(pose);
                    double girth = ArmPose.girth(reach.girth(), reach.length());
                    PaleFleshModel.Pose flesh = ArmPose.pose(pose, ArmPose.out(from, way, reach.length(), girth), girth, reach.hand(), state[1], 7);
                    double clip = armDepth(pose, flesh);
                    if (clip > worst) {
                        worst = clip;
                        where = " (" + phase + " " + java.util.Arrays.toString(state) + ", yaw " + yaw + ", pitch " + pitch + ")";
                    }
                }
            }
        }
        // The sphere, wherever it is (right in front of her, a wall behind it, or far out).
        for (double out : new double[] {ArmRules.sphereOut(1.0, 1.8), ArmRules.sphereOut(1.0, 0.9), 12.0}) {
            for (double yaw = -ArmPose.SQUARE * 2.0; yaw <= ArmPose.SQUARE * 2.0; yaw += ArmPose.SQUARE) {
                double y = Math.toRadians(yaw);
                double[] way = {Math.sin(y), 0.0, Math.cos(y)};
                RigPose pose = PaleMotions.guard();
                ArmPose.body(pose, ArmRules.Phase.SPHERE, 10.0, new ArmPose.Reach(out, ArmRules.girth(1.0), PaleFleshModel.Shape.ARM_SHUT), way, true, 10.0);
                pose.settle(1.0F);
                double[] from = ArmPose.shoulder(pose);
                double[] centre = {from[0] + way[0] * out, from[1], from[2] + way[2] * out};
                double girth = ArmPose.girth(ArmRules.girth(1.0), out);
                double clip = armDepth(pose, ArmPose.pose(pose, ArmPose.onSphere(ArmPose.root(pose), centre, out > 5.0 ? 1.8 : out - 1.2), girth, PaleFleshModel.Shape.ARM_SHUT, 10.0, 7));
                if (clip > worst) {
                    worst = clip;
                    where = " (the sphere " + out + " out, yaw " + yaw + ")";
                }
            }
        }
        check(worst == 0.0, "the arm never goes through her: " + worst + " px" + where);
        // Her body under it: never a limb through her, whatever the arm is doing.
        worst = 0.0;
        where = "";
        for (ArmRules.Phase phase : new ArmRules.Phase[] {ArmRules.Phase.SHOOT, ArmRules.Phase.HOLD, ArmRules.Phase.DRAG, ArmRules.Phase.SPHERE,
            ArmRules.Phase.RETRACT}) {
            for (double age = 0.0; age <= 30.0; age += 0.5) {
                for (double yaw = -75.0; yaw <= 75.0; yaw += 50.0) {
                    double y = Math.toRadians(yaw);
                    RigPose pose = PaleMotions.guard();
                    ArmPose.body(pose, phase, age, ArmPose.reach(phase, age, 20.0, 1.0, 40.0, true, true), new double[] {Math.sin(y), -0.2, Math.cos(y)}, true,
                        age);
                    pose.settle(1.0F);
                    double clip = BodyFrame.clipping(pose, CLEARANCE);
                    if (clip > worst) {
                        worst = clip;
                        where = " (" + phase + ", tick " + age + ", yaw " + yaw + ")";
                    }
                }
            }
        }
        check(worst == 0.0, "her arms stay out of her body under the arm's weight: " + worst + " px" + where);
        // The sphere: closed all round what it holds as it shuts, squeezed in hard, nothing at all before it begins to form.
        double[] centre = {0.0, 1.5, 12.0};
        double[] backWay = {0.0, 0.0, -1.0};
        double[] nearest = {Double.MAX_VALUE};
        double[] farthest = {0.0};
        int[] quads = {0};
        PaleFleshModel.cocoon((a, b, c, e, n, ca, cb, cc, cd) -> {
            quads[0]++;
            for (double[] v : new double[][] {a, b, c, e}) {
                nearest[0] = Math.min(nearest[0], distance(v, centre));
                farthest[0] = Math.max(farthest[0], distance(v, centre));
            }
        }, centre, backWay, 1.2, 1.0, 1.0, 0.0, 3);
        check(quads[0] > 100 && nearest[0] > 1.2 * 0.6 && farthest[0] < 1.2 * 1.45, "the sphere is whole round it, squeezed in: " + nearest[0] + " .. " + farthest[0]);
        double[] tip = {Double.MAX_VALUE};
        PaleFleshModel.cocoon((a, b, c, e, n, ca, cb, cc, cd) -> {
            for (double[] v : new double[][] {a, b, c, e}) {
                tip[0] = Math.min(tip[0], v[2]);
            }
        }, centre, backWay, 1.2, 1.0, 0.0, 0.0, 3);
        check(tip[0] < centre[2] - 1.0, "shut, it closes right over the far side");
        int[] none = {0};
        PaleFleshModel.cocoon((a, b, c, e, n, ca, cb, cc, cd) -> none[0]++, centre, backWay, 1.2, 0.0, 0.0, 0.0, 3);
        check(none[0] == 0, "nothing of it before it begins to form");
        check(ArmRules.sphereRadius(1.8) >= 1.8 * 0.55 && ArmRules.sphereRadius(0.5) >= 0.9, "the sphere is big enough for what it holds");
    }

    static void revive() {
        // Her mutations: a point at mastery 500, spent on the one there is; had, nothing left to spend.
        check(com.hex.kagune.control.pale.PaleMutation.earned(499) == 0 && com.hex.kagune.control.pale.PaleMutation.earned(500) == 1,
            "a mutation point at mastery 500");
        com.hex.kagune.control.pale.PaleMutation revive = com.hex.kagune.control.pale.PaleMutation.NOT_THAT_EASILY;
        check(!com.hex.kagune.control.pale.PaleMutation.canUnlock(499, 0, revive) && com.hex.kagune.control.pale.PaleMutation.canUnlock(500, 0, revive)
            && !com.hex.kagune.control.pale.PaleMutation.canUnlock(1000, revive.bit(), revive) && revive.in(revive.bit()),
            "unlocked with the point, once");
        check(PaleUnlock.MUTATION_POINT.mastery() == com.hex.kagune.control.pale.PaleMutation.MILESTONES[0], "the track shows where the point comes");
        // 6.2.0: Up We Go, a second mutation for a second point at 800; the first point still buys either.
        com.hex.kagune.control.pale.PaleMutation soar = com.hex.kagune.control.pale.PaleMutation.UP_WE_GO;
        check(PaleUnlock.MUTATION_POINT_2.mastery() == com.hex.kagune.control.pale.PaleMutation.MILESTONES[1] && PaleUnlock.MUTATION_POINT_2.mastery() == 800,
            "a second point at mastery 800, on the track");
        check(com.hex.kagune.control.pale.PaleMutation.earned(799) == 1 && com.hex.kagune.control.pale.PaleMutation.earned(800) == 2,
            "two points by 800");
        check(com.hex.kagune.control.pale.PaleMutation.canUnlock(500, 0, soar) && !com.hex.kagune.control.pale.PaleMutation.canUnlock(799, revive.bit(), soar)
            && com.hex.kagune.control.pale.PaleMutation.canUnlock(800, revive.bit(), soar)
            && !com.hex.kagune.control.pale.PaleMutation.canUnlock(1000, revive.bit() | soar.bit(), revive), "either one with the first point, both by 800");
        check(revive.bit() != soar.bit() && soar.in(com.hex.kagune.control.pale.PaleMutation.all()), "each its own bit");
        // The timings: five seconds in pieces, the lines, the pull; whole after; ready again five minutes on.
        check(ReviveRules.SCATTER == 220 && ReviveRules.COOLDOWN == 6000L && ReviveRules.WHOLE == ReviveRules.SCATTER + ReviveRules.LINES + ReviveRules.PULL,
            "five seconds in pieces, five minutes before it takes again");
        check(ReviveRules.ready(100L, 100L) && !ReviveRules.ready(99L, 100L) && ReviveRules.ready(0L, 0L) && ReviveRules.ready(5L, 5L + ReviveRules.COOLDOWN + 1L),
            "ready once the cooldown is up (a ready time further off than the cooldown is forgotten)");
        double pulled = 0.0;
        for (double age = 0.0; age <= ReviveRules.WHOLE + 1.0; age += 0.25) {
            double now = ReviveRules.pull(age);
            check(now >= pulled - 1.0E-12, "the pull only ever brings it in");
            check(age > ReviveRules.SCATTER + ReviveRules.LINES || now == 0.0, "nothing is pulled before the lines are out");
            pulled = now;
        }
        check(ReviveRules.pull(ReviveRules.WHOLE) == 1.0 && ReviveRules.apart(0.0) && ReviveRules.apart(ReviveRules.WHOLE - 0.01)
            && !ReviveRules.apart(ReviveRules.WHOLE), "pulled all the way in, then whole");
        // Her remains, the same on every client from the same seed.
        for (long seed : new long[] {1L, 12345L, -77L, 987654321L}) {
            for (double ground : new double[] {0.0, -0.6, -2.5}) {
                Remains.Scatter scatter = Remains.scatter(seed, ground, seed < 0);
                Remains.Scatter again = Remains.scatter(seed, ground, seed < 0);
                check(java.util.Arrays.deepEquals(scatter.track(Remains.Piece.HEAD).at(), again.track(Remains.Piece.HEAD).at()), "the same seed, the same remains");
                for (Remains.Piece piece : Remains.Piece.values()) {
                    double[] home = Remains.centre(piece, scatter.slim());
                    Remains.Placed start = Remains.place(scatter, piece, 0.0);
                    check(distance(start.at(), home) < 1.0E-9, "it comes apart from where it was in her (" + piece + ")");
                    Remains.Placed lying = Remains.place(scatter, piece, ReviveRules.SCATTER);
                    double[][] corners = Remains.corners(piece, scatter.slim());
                    double low = Double.MAX_VALUE;
                    for (double[] c : corners) {
                        low = Math.min(low, lying.at()[1] + Remains.rotate(lying.turn(), c)[1]);
                    }
                    check(Math.abs(low - ground) < 1.0E-6, "it lies on the ground (" + piece + ", ground " + ground + "): " + low);
                    double spread = Math.hypot(lying.at()[0], lying.at()[2]);
                    check(spread < 4.5, "it lands near where she stood (" + piece + "): " + spread);
                    // Flat: one side of its box square to the ground.
                    double square = 0.0;
                    for (int k : new int[] {1, 2, 4}) {
                        double[] e = Remains.rotate(lying.turn(), new double[] {corners[k][0] - corners[0][0], corners[k][1] - corners[0][1], corners[k][2] - corners[0][2]});
                        square = Math.max(square, Math.abs(e[1]) / Math.sqrt(e[0] * e[0] + e[1] * e[1] + e[2] * e[2]));
                    }
                    check(square > 0.999, "it lies flat on a side (" + piece + "): " + square);
                    if (piece != Remains.Piece.HEAD) {
                        // A limb on its length (never standing on its torn end), the halves of her torso on front or back.
                        double[] length = Remains.rotate(lying.turn(), new double[] {corners[2][0] - corners[0][0], corners[2][1] - corners[0][1],
                            corners[2][2] - corners[0][2]});
                        double level = Math.abs(length[1]) / Math.sqrt(length[0] * length[0] + length[1] * length[1] + length[2] * length[2]);
                        check(level < 0.01, "it lies along its length, not on its end (" + piece + "): " + level);
                    }
                    Remains.Placed whole = Remains.place(scatter, piece, ReviveRules.WHOLE);
                    check(distance(whole.at(), home) < 1.0E-9 && Math.abs(whole.turn()[0] - 1.0) < 1.0E-9, "pulled back to where it belongs (" + piece + ")");
                    double[] before = Remains.place(scatter, piece, ReviveRules.WHOLE - 0.05).at();
                    check(distance(before, home) < 0.2, "it is right there as she is whole (" + piece + ")");
                    Remains.Line line = Remains.line(scatter, piece, ReviveRules.SCATTER + ReviveRules.LINES);
                    check(piece == Remains.Piece.CHEST ? line == null : line != null && line.reach() == 1.0, "a line out to every piece but the chest itself");
                }
                check(Remains.line(scatter, Remains.Piece.HEAD, ReviveRules.SCATTER - 1.0) == null && Remains.line(scatter, Remains.Piece.HEAD, ReviveRules.WHOLE) == null,
                    "no lines before the five seconds are up, nor once she is whole");
                for (int rope = 0; rope < Remains.ROPES; rope++) {
                    double[][] guts = Remains.guts(scatter, rope, 60.0);
                    double lowest = Double.MAX_VALUE;
                    for (int i = 1; i < guts.length; i++) {
                        lowest = Math.min(lowest, guts[i][1]);
                    }
                    check(lowest >= ground + Remains.GUT_RADIUS - 1.0E-6, "her guts lie on the ground, never in it (out of where they hang)");
                    check(Remains.guts(scatter, rope, ReviveRules.WHOLE) == null, "reeled back in");
                }
            }
        }
        // 6.2.0, the gathering: what is left of her chest stands first, her head is pulled on last; every piece is dragged home in its
        // own turn, turned upright before it gets there, never jumping, and home (its line and guts reeled in with it) by the time
        // she is whole, so her body takes over exactly where the pieces came together.
        double firstStart = Remains.Piece.CHEST.haul()[0];
        double lastEnd = Remains.Piece.HEAD.haul()[1];
        for (Remains.Piece piece : Remains.Piece.values()) {
            double[] when = piece.haul();
            check(when[0] >= 0.0 && when[1] <= 1.0 && when[0] < when[1] && when[0] >= firstStart && when[1] <= lastEnd,
                "each piece is hauled in its turn, the chest first, the head last (" + piece + ")");
        }
        check(lastEnd == 1.0, "the last piece is home the moment she is whole");
        for (long seed : new long[] {7L, 4242L}) {
            Remains.Scatter scatter = Remains.scatter(seed, -0.4, false);
            for (Remains.Piece piece : Remains.Piece.values()) {
                double[] when = piece.haul();
                double start = ReviveRules.SCATTER + ReviveRules.LINES + when[0] * ReviveRules.PULL;
                double end = ReviveRules.SCATTER + ReviveRules.LINES + when[1] * ReviveRules.PULL;
                double[] home = Remains.centre(piece, false);
                check(!Remains.home(piece, end - 0.05) && Remains.home(piece, end), "home exactly when its haul is done (" + piece + ")");
                Remains.Placed in = Remains.place(scatter, piece, end);
                check(distance(in.at(), home) < 1.0E-9 && Math.abs(in.turn()[0] - 1.0) < 1.0E-9, "home and upright (" + piece + ")");
                double upright = ReviveRules.upright(start + (end - start) * 0.72, when[0], when[1]);
                check(upright == 1.0, "upright before it is home, never turning as it slots in (" + piece + "): " + upright);
                double[] was = Remains.place(scatter, piece, ReviveRules.SCATTER).at();
                double worst = 0.0;
                for (double age = ReviveRules.SCATTER; age <= ReviveRules.WHOLE + 1.0; age += 0.25) {
                    double[] now = Remains.place(scatter, piece, age).at();
                    worst = Math.max(worst, distance(now, was));
                    was = now;
                    Remains.Line line = Remains.line(scatter, piece, age);
                    check(!Remains.home(piece, age) || line == null, "no line to a piece already home (" + piece + ")");
                }
                check(worst < 0.5, "dragged home, never jumping (" + piece + "): " + worst + " blocks in a quarter tick");
            }
            for (int rope = 0; rope < Remains.ROPES; rope++) {
                check(Remains.guts(scatter, rope, ReviveRules.WHOLE - 1.0) == null, "her guts are back in before she is whole");
            }
        }
        // Where each piece is torn open (where the blood pumps out of): every torn end, facing out of it.
        for (Remains.Piece piece : Remains.Piece.values()) {
            double[][][] open = Remains.openings(piece, false);
            int ends = (piece.ordinal() == Remains.Piece.HEAD.ordinal() ? 1 : 0);
            check(open.length >= 1, "torn open somewhere (" + piece + ")");
            for (double[][] end : open) {
                check(Math.abs(Math.sqrt(end[1][0] * end[1][0] + end[1][1] * end[1][1] + end[1][2] * end[1][2]) - 1.0) < 1.0E-9,
                    "which way it bleeds out is a direction (" + piece + ")");
                double[] middle = new double[3];
                check(end[0][0] * end[1][0] + end[0][1] * end[1][1] + end[0][2] * end[1][2] > 0.0 || distance(end[0], middle) < 1.0E-9,
                    "it bleeds out of the end, away from its middle (" + piece + ")");
            }
            if (ends == 1) {
                check(open.length == 1 && open[0][1][1] < -0.99, "her head bleeds out of her neck, downward");
            }
        }
        // Each piece in her skin, its torn ends raw: the head torn at the neck, a forearm at the elbow, the hand whole.
        for (Remains.Piece piece : Remains.Piece.values()) {
            int[] counts = new int[3];
            Remains.mesh(piece, false, 3, new Remains.Sink() {
                @Override
                public void skin(double[] a, double[] b, double[] c, double[] d, double[] normal, float[] uv, boolean layer) {
                    counts[layer ? 1 : 0]++;
                    for (float f : uv) {
                        check(f >= 0.0F && f <= 1.0F, "its skin is on the skin (" + piece + ")");
                    }
                }

                @Override
                public void flesh(double[][] polygon, double[] normal, int[] argb) {
                    counts[2]++;
                }
            });
            check(counts[0] >= 4 && counts[1] >= 4 && counts[2] > 30, "a piece in her skin, raw where it is torn (" + piece + "): " + java.util.Arrays.toString(counts));
        }
    }

    static void owl() {
        // Its rules: nothing while she doubles over, then it bursts out and fills in; drawn back in all the way; her body bulked
        // only once it is well out.
        check(OwlRules.grown(0.0) == 0.0 && OwlRules.grown(OwlRules.BURST) == 0.0 && OwlRules.grown(OwlRules.GROW_TICKS) == 1.0,
            "it bursts out at the burst and is all out when it has grown");
        boolean rising = true;
        boolean falling = true;
        for (double age = 0.0; age < OwlRules.GROW_TICKS + 5.0; age += 0.5) {
            rising &= OwlRules.grown(age + 0.5) >= OwlRules.grown(age);
            falling &= OwlRules.shed(age + 0.5) <= OwlRules.shed(age);
        }
        check(rising && falling && OwlRules.shed(0.0) == 1.0 && OwlRules.shed(OwlRules.SHED_TICKS) == 0.0, "it grows and goes smoothly, all the way");
        check(OwlRules.bulk(0.0) == 0.0 && OwlRules.bulk(1.0) == 1.0, "bulked up only as it fills in");
        check(OwlRules.MASTERY == PaleRules.KAKUJA_V1_MASTERY && !OwlRules.changing(true, OwlRules.GROW_TICKS) && OwlRules.changing(false, 0.0),
            "K is heard again once it has grown or gone");
        // Her body as it tears out of her, in it, and as it goes: from anything she may be doing, never an arm through her, her
        // arms as thick as the bulk draws them and her chest as broad. (Growing, whatever she was doing is under it whole; going,
        // nothing else can be going on, and her idle comes back under it as it lets go, as KaguneBodyAnimation puts it on.)
        double worst = 0.0;
        double worstBulk = 0.0;
        String where = "";
        List<RigPose> froms = starts(PaleStrike.JAB);
        for (boolean on : new boolean[] {true, false}) {
            for (int f = 0; f < froms.size(); f += 3) {
                RigPose from = froms.get(f) == null ? new RigPose() : froms.get(f);
                for (double age = 0.0; age <= OwlRules.GROW_TICKS + 6.0; age += age < OwlRules.BURST + 6.0 ? 0.5 : 2.0) {
                    RigPose pose = new RigPose().add(from, on ? 1.0F : (float)(1.0 - OwlPose.weight(false, age)));
                    OwlPose.body(pose, on, age, age * 1.3);
                    pose.settle(1.0F);
                    double clip = BodyFrame.clipping(pose, CLEARANCE);
                    double bulked = BodyFrame.bulkClipping(pose, CLEARANCE);
                    if (Math.max(clip, bulked) > Math.max(worst, worstBulk)) {
                        where = " (" + (on ? "growing" : "going") + ", start " + f + ", tick " + age + ")";
                    }
                    worst = Math.max(worst, clip);
                    worstBulk = Math.max(worstBulk, bulked);
                }
            }
        }
        check(worst == 0.0 && worstBulk == 0.0, "her arms stay out of her body as the Owl grows and goes: " + worst + " / bulked " + worstBulk + " px" + where);
        // In it, walking: her bulked arms swinging past her broad chest.
        RigPose in = new RigPose();
        OwlPose.body(in, true, OwlRules.GROW_TICKS + 20.0, 3.0);
        double swing = 0.0;
        for (double a = -0.8; a <= 0.8; a += 0.1) {
            RigPose walking = new RigPose().add(in, 1.0F);
            walking.rightArmX += (float)a;
            walking.leftArmX -= (float)a;
            walking.settle(1.0F);
            swing = Math.max(swing, BodyFrame.bulkClipping(walking, CLEARANCE));
        }
        check(in.bulk == 1.0F && swing == 0.0, "in it, her thick arms swing past her broad chest as she walks: " + swing + " px");
        // 6.2.4: its limbs torn up out of her back folded tight, unfolding as it grows (folding up again as it goes): all four blades
        // and the crest never through her, out of nothing to her idle; what it puts on her (the robe, the claws, the talons) never in
        // the ground; the robe never through her legs.
        check(OwlPose.unfurl(OwlPose.REST, 1, 1.0) == OwlPose.REST && OwlPose.unfurl(OwlPose.REST, 1, 0.0).forward() > 2.0,
            "the limbs come up out of her back folded and are all the way down in her idle once it has grown");
        worst = 0.0;
        double sunkGrowing = Double.MAX_VALUE;
        double legs = 0.0;
        where = "";
        for (boolean on : new boolean[] {true, false}) {
            for (double age = 0.0; age <= OwlRules.GROW_TICKS + 2.0; age += 0.5) {
                double g = OwlRules.grown(on, age);
                if (g <= 0.01) {
                    continue;
                }
                RigPose pose = new RigPose();
                OwlPose.body(pose, on, age, age);
                pose.settle(1.0F);
                OwlModel.Look look = OwlPose.look(pose, KakuganEyes.DEFAULT, OwlPose.unfurl(OwlPose.swing(1, age, 0.0, 0.0), 1, g),
                    OwlPose.unfurl(OwlPose.swing(-1, age, 0.0, 0.0), -1, g), age, g);
                BodyFrame.Solid solid = new BodyFrame.Solid(pose);
                double[] deepest = {0.0};
                OwlModel.reaching((a, b, c, d, n, ca, cb, cc, cd) -> {
                    for (double[] v : new double[][] {a, b, c, d}) {
                        deepest[0] = Math.max(deepest[0], Math.max(solid.depth(v, 0.0), chestDepth(look.torso(), v, pose.bulk)) - CLEARANCE);
                    }
                }, look);
                if (deepest[0] > worst) {
                    worst = deepest[0];
                    where = " (" + (on ? "growing" : "going") + ", tick " + age + ")";
                }
                double[] low = {Double.MAX_VALUE};
                OwlModel.body((a, b, c, d, n, ca, cb, cc, cd) -> {
                    for (double[] v : new double[][] {a, b, c, d}) {
                        low[0] = Math.min(low[0], v[1]);
                    }
                }, look);
                sunkGrowing = Math.min(sunkGrowing, low[0]);
                legs = Math.max(legs, robeInLegs(pose, look));
            }
        }
        check(worst <= 0.0, "its limbs and crest never go through her as they unfold out of her and fold back in: " + worst + " px" + where);
        check(sunkGrowing >= 0.0, "the robe, claws and talons never sink into the ground as it grows and goes: " + sunkGrowing);
        check(legs <= 0.0, "the robe never goes through her legs as it grows and goes: " + legs + " px");
        // Her broad chest: over the top of her torso, all of it, and in her skin.
        RigPose bulked = new RigPose();
        bulked.bulk = 1.0F;
        int[] faces = {0};
        boolean[] mapped = {true};
        double[] widest = {0.0};
        OwlBulk.chest(bulked, (a, b, c, d, n, uv, jacket) -> {
            faces[0]++;
            for (float u : uv) {
                mapped[0] &= u >= 0.0F && u <= 1.0F;
            }
            for (double[] v : new double[][] {a, b, c, d}) {
                widest[0] = Math.max(widest[0], Math.abs(v[0]));
            }
        });
        check(faces[0] == 12 && mapped[0] && widest[0] > 0.3, "her chest: six faces and the jacket over them, in her skin, broad: " + widest[0]);
        // Alive: standing, the giant arms heave with her breath and flick out now and then; walking, they swing against each other
        // and the blades are lifted clear.
        check(OwlPose.flick(OwlPose.FLICK_LENGTH * 0.25) > 0.99 && OwlPose.flick(OwlPose.FLICK_LENGTH + 1.0) == 0.0 && OwlPose.flick(-1.0) == 0.0
            && OwlPose.flick(OwlPose.FLICK_EVERY + 1.0) > 0.0, "the blades flick out fast, come back, then are still a while");
        double heaveLow = Double.MAX_VALUE;
        double heaveHigh = -Double.MAX_VALUE;
        for (double time = 0.0; time < 120.0; time += 1.0) {
            heaveLow = Math.min(heaveLow, OwlPose.swing(1, time, 0.0, 0.0).out());
            heaveHigh = Math.max(heaveHigh, OwlPose.swing(1, time, 0.0, 0.0).out());
        }
        check(heaveHigh - heaveLow > 0.08, "standing, the arms heave with her breath: " + (heaveHigh - heaveLow));
        check(OwlPose.swing(1, 10.0, 1.0, 0.0).forward() - OwlPose.swing(1, 10.0, 1.0, Math.PI).forward() > 0.5
            && OwlPose.swing(-1, 10.0, 1.0, Math.PI).forward() - OwlPose.swing(-1, 10.0, 1.0, 0.0).forward() > 0.5,
            "walking, each arm swings forward and back, against the other");
        // Running, the blades are lifted clear (the whole arm lifts them, the blade carried on it): every point of her stride, each
        // blade's point well up off where it hangs standing.
        RigPose standing = OwlPose.stance();
        double lifted = Double.MAX_VALUE;
        for (int side : new int[] {1, -1}) {
            double still = OwlPose.arm(standing, OwlPose.swing(side, 10.0, 0.0, 0.0), side).tip()[1];
            for (double stride = 0.0; stride < Math.PI * 2.0; stride += 0.25) {
                lifted = Math.min(lifted, OwlPose.arm(standing, OwlPose.swing(side, 10.0, 1.0, stride), side).tip()[1] - still);
            }
        }
        check(lifted > 0.3, "running, the blades are lifted clear: their points at least " + lifted + " blocks up");
        // Whatever it does (breathing, swaying, flicking, standing, walking, running), never a blade or a crystal through her (her
        // bulked chest, her head, her arms, her legs), nothing of it into the ground, and the mask and its feathers on her head,
        // never in it.
        double through = 0.0;
        double sunk = Double.MAX_VALUE;
        double inHead = 0.0;
        where = "";
        for (double walk : new double[] {0.0, 0.5, 1.0}) {
            for (double time = 0.0; time < OwlPose.FLICK_EVERY * 2.0; time += 1.5) {
                double stride = time * 0.6662 * 1.4 * walk;
                RigPose body = new RigPose();
                OwlPose.body(body, true, OwlRules.GROW_TICKS + 30.0, time);
                body.rightLegX = (float)(0.7 * walk * Math.cos(stride));
                body.leftLegX = (float)(-0.7 * walk * Math.cos(stride));
                body.rightArmX += (float)(-0.5 * walk * Math.cos(stride));
                body.leftArmX += (float)(0.5 * walk * Math.cos(stride));
                body.settle(1.0F);
                OwlModel.Look look = OwlPose.look(body, KakuganEyes.DEFAULT, OwlPose.swing(1, time, walk, stride), OwlPose.swing(-1, time, walk, stride),
                    time, 1.0);
                BodyFrame.Solid solid = new BodyFrame.Solid(body);
                double[] deepest = {0.0};
                OwlModel.reaching((a, b, c, d, n, ca, cb, cc, cd) -> {
                    for (double[] v : new double[][] {a, b, c, d}) {
                        deepest[0] = Math.max(deepest[0], Math.max(solid.depth(v, 0.0), chestDepth(look.torso(), v, body.bulk)) - CLEARANCE);
                        // Her legs: under her hips, never more than a few pixels out from her middle.
                        if (v[1] < 0.8 && Math.abs(v[0]) < 0.45 && Math.abs(v[2]) < 0.6) {
                            deepest[0] = Math.max(deepest[0], 1.0);
                        }
                    }
                }, look);
                double[] low = {Double.MAX_VALUE};
                OwlModel.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                    for (double[] v : new double[][] {a, b, c, d}) {
                        low[0] = Math.min(low[0], v[1]);
                    }
                }, look);
                double[] head = {0.0};
                OwlModel.head((a, b, c, d, n, ca, cb, cc, cd) -> {
                    for (double[] v : new double[][] {a, b, c, d}) {
                        head[0] = Math.max(head[0], solid.part(v, 0.0, 1));
                    }
                }, look);
                if (deepest[0] > through) {
                    where = String.format(java.util.Locale.ROOT, " (walk %.1f, tick %.1f)", walk, time);
                }
                through = Math.max(through, deepest[0]);
                sunk = Math.min(sunk, low[0]);
                inHead = Math.max(inHead, head[0]);
            }
        }
        check(through <= 0.0, "never a blade or a crystal through her: " + through + " px" + where);
        check(sunk >= 0.0, "nothing of it sinks into the ground: " + sunk);
        check(inHead <= 0.0, "the mask and its feathers sit on her head, never in it: " + inHead + " px");
        // Her look (the mask turns with her head, as far as her heavy head goes in it): the mask never deeper into her chest or her
        // arms than her own head goes (nodding, the top of her bulked chest meets her jaw; level, nothing), her head never into the
        // crystals; a look past the limit is held back to it.
        double worstLook = 0.0;
        where = "";
        for (double yaw : new double[] {-1.5, -0.45, 0.0, 0.45, 1.5}) {
            for (double pitch : new double[] {-1.5, 0.0, 1.5}) {
                for (double time : new double[] {0.0, 20.0, 41.0}) {
                    // As the game draws it: her look held back by her heavy head, under the pose.
                    RigPose body = new RigPose();
                    OwlPose.body(body, true, OwlRules.GROW_TICKS + 30.0, time);
                    OwlPose.neck(body, yaw, pitch, 1.0);
                    body.settle(1.0F);
                    body.headY += (float)yaw;
                    body.headX += (float)pitch;
                    OwlModel.Look look = OwlPose.look(body, KakuganEyes.DEFAULT, OwlPose.swing(1, time, 0.0, 0.0), OwlPose.swing(-1, time, 0.0, 0.0), time, 1.0);
                    BodyFrame.Solid solid = new BodyFrame.Solid(body);
                    double own = pitch == 0.0 ? 0.0 : ownHead(look, solid, body.bulk);
                    double[] deepest = {0.0};
                    OwlModel.head((a, b, c, d, n, ca, cb, cc, cd) -> {
                        for (double[] v : new double[][] {a, b, c, d}) {
                            double into = Math.max(Math.max(solid.part(v, 0.0, 0), chestDepth(look.torso(), v, body.bulk)),
                                Math.max(solid.part(v, 0.0, 2), solid.part(v, 0.0, 3)));
                            deepest[0] = Math.max(deepest[0], into - own - CLEARANCE);
                        }
                    }, look);
                    OwlModel.reaching((a, b, c, d, n, ca, cb, cc, cd) -> {
                        for (double[] v : new double[][] {a, b, c, d}) {
                            deepest[0] = Math.max(deepest[0], solid.part(v, 0.0, 1) - CLEARANCE);
                        }
                    }, look);
                    if (deepest[0] > worstLook) {
                        worstLook = deepest[0];
                        where = String.format(java.util.Locale.ROOT, " (yaw %.2f, pitch %.2f, tick %.0f)", yaw, pitch, time);
                    }
                }
            }
        }
        check(worstLook <= 0.0, "however she holds her head in it, the mask stays off her body and her head off the crystals: " + worstLook + " px" + where);
        RigPose held = new RigPose();
        OwlPose.neck(held, 1.4, -1.2, 1.0);
        check(Math.abs(held.headY - (OwlPose.LOOK_YAW - 1.4)) < 1.0E-6 && Math.abs(held.headX - (-OwlPose.LOOK_UP + 1.2)) < 1.0E-6,
            "a look past what her heavy head allows is held back to it");
        RigPose free = new RigPose();
        OwlPose.neck(free, 0.3, 0.2, 1.0);
        check(free.headY == 0.0F && free.headX == 0.0F, "within it, her look is her own");
    }

    /**
     * 6.2.4: the Owl as a whole creature drawn in her place (the user's model, baked): it is there to draw; it bursts out of her at
     * her own size and swells up to its own, its horns some five blocks up; it stands on the ground (nothing of it under it)
     * standing, crawling and running; its one eye in front of its head (first person sees out of it); cheap enough to draw every frame.
     */
    static void owlCreature() {
        OwlMesh mesh = OwlMesh.get();
        check(mesh.quads() > 3000 && mesh.bone("head") >= 0 && mesh.bone("mask_jaw") >= 0 && mesh.bone("right_blade_upper") >= 0
            && mesh.bone("mask_tongue") >= 0 && mesh.bone("mask_iris") >= 0, "its baked mesh is there, every bone it moves in it: " + mesh.quads() + " quads");
        check(OwlCreature.scale(0.0) == 1.0 && Math.abs(OwlCreature.scale(1.0) - OwlCreature.SCALE) < 1.0E-9, "it grows from her size to its own");
        boolean swelling = true;
        for (double g = 0.0; g < 1.0; g += 0.02) {
            swelling &= OwlCreature.scale(g + 0.02) >= OwlCreature.scale(g);
        }
        check(swelling, "it only ever swells as it grows");
        double lowest = Double.MAX_VALUE;
        double headTop = 0.0;
        boolean finite = true;
        int most = 0;
        String where = "";
        int head = mesh.bone("head");
        for (double walk : new double[] {0.0, 0.5, 1.0}) {
            for (double time = 0.0; time < 240.0; time += 2.5) {
                double stride = time * 0.6662 * 1.4 * walk;
                RigPose body = new RigPose();
                OwlPose.body(body, true, OwlRules.GROW_TICKS + 30.0, time);
                body.settle(1.0F);
                OwlCreature.Rig rig = OwlCreature.rig(OwlPose.source(body, walk, stride, false, time, 1.0));
                OwlModel.Look look = OwlPose.look(rig, OwlPose.swing(1, time, walk, stride), OwlPose.swing(-1, time, walk, stride), time, 1.0);
                double[] low = {Double.MAX_VALUE};
                int[] quads = {0};
                boolean[] ok = {true};
                com.hex.kagune.control.ukaku.UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
                    quads[0]++;
                    for (double[] v : new double[][] {a, b, c, d}) {
                        low[0] = Math.min(low[0], v[1]);
                        ok[0] &= Double.isFinite(v[0]) && Double.isFinite(v[1]) && Double.isFinite(v[2]);
                    }
                };
                OwlCreature.mesh(sink, painted(sink), rig);
                OwlCreature.kagune(sink, rig, look);
                if (low[0] < lowest) {
                    where = String.format(java.util.Locale.ROOT, " (walk %.1f, tick %.1f)", walk, time);
                }
                lowest = Math.min(lowest, low[0]);
                finite &= ok[0];
                most = Math.max(most, quads[0]);
                for (int i = 0; i < mesh.quads(); i++) {
                    if (mesh.bone[i] == head) {
                        headTop = Math.max(headTop, rig.at(head, new double[] {mesh.corners[i * 12], mesh.corners[i * 12 + 1], mesh.corners[i * 12 + 2]})[1]);
                    }
                }
            }
        }
        check(finite, "every point of it is a real place");
        check(lowest >= -1.0E-9, "nothing of it under the ground, standing, crawling or running: " + lowest + where);
        double eyeUp = OwlCreature.eye(standing(0.0, 0.0, 30.0))[1];
        check(headTop > 4.4 && headTop < 5.6 && eyeUp > 3.0 && eyeUp < 4.0, "its head (its horns) some five blocks up, its eye three and a half: " + headTop
            + ", " + eyeUp);
        // (The sheet's creature, its cloak, crest and glow, at its full detail: twelve thousand quads at most, a phone's GPU's play.)
        check(most <= 12000, "cheap enough to draw every frame: " + most + " quads, its glow and its kagune with it");
        // Bursting out of her and shrinking back into her: a real place throughout, never under the ground.
        double growingLow = Double.MAX_VALUE;
        for (boolean on : new boolean[] {true, false}) {
            for (double age = 0.0; age <= OwlRules.GROW_TICKS; age += 0.5) {
                double g = OwlRules.grown(on, age);
                if (g <= 0.01) {
                    continue;
                }
                RigPose body = new RigPose();
                OwlPose.body(body, on, age, age);
                body.settle(1.0F);
                OwlCreature.Rig rig = OwlCreature.rig(OwlPose.source(body, 0.0, 0.0, false, age, g));
                double[] low = {Double.MAX_VALUE};
                com.hex.kagune.control.ukaku.UkakuModel.Sink under = (a, b, c, d, n, ca, cb, cc, cd) -> {
                    for (double[] v : new double[][] {a, b, c, d}) {
                        low[0] = Math.min(low[0], Double.isFinite(v[1]) ? v[1] : -1.0);
                    }
                };
                OwlCreature.mesh(under, painted(under), rig);
                growingLow = Math.min(growingLow, low[0]);
            }
        }
        check(growingLow >= -1.0E-9, "it bursts out of her and shrinks back into her above the ground: " + growingLow);
        // First person sees out of its one eye: in front of its head and up where its head is.
        OwlCreature.Rig rig = standing(0.0, 0.0, 0.0);
        double[] eye = OwlCreature.eye(rig);
        double[] skull = rig.head().o();
        double[] forward = unit(new double[] {-rig.head().z()[0], -rig.head().z()[1], -rig.head().z()[2]});
        check(dot3(sub3(eye, skull), forward) > 0.2 && eye[1] > 3.0 && dot3(forward, new double[] {0.0, 0.0, 1.0}) > 0.7,
            "its eye in front of its head, high up, looking ahead: " + java.util.Arrays.toString(eye));
    }

    /** 6.2.4: the creature standing (its Owl grown), crawling {@code walk} at {@code stride}, at {@code time}. */
    static OwlCreature.Rig standing(double walk, double stride, double time) {
        RigPose body = new RigPose();
        OwlPose.body(body, true, OwlRules.GROW_TICKS + 30.0, time);
        body.settle(1.0F);
        return OwlCreature.rig(OwlPose.source(body, walk, stride, false, time, 1.0));
    }

    /** 6.2.4: the creature standing (its Owl grown) at {@code time}, through its move's {@code act}. */
    static OwlCreature.Rig acting(OwlActs.Act act, double time) {
        RigPose body = new RigPose();
        OwlPose.body(body, true, OwlRules.GROW_TICKS + 30.0, time);
        body.settle(1.0F);
        return OwlCreature.rig(OwlPose.source(body, 0.0, 0.0, false, time, 1.0).with(0.0, 0.0, OwlCreature.LONG_AGO, act));
    }

    /** 6.2.4: the creature at {@code walk} and {@code stride}, sprinting {@code run}, off the ground {@code air} ticks, landed {@code landed} ago. */
    static OwlCreature.Rig moving(double walk, double stride, double run, double air, double landed, double time) {
        RigPose body = new RigPose();
        OwlPose.body(body, true, OwlRules.GROW_TICKS + 30.0, time);
        body.settle(1.0F);
        return OwlCreature.rig(OwlPose.source(body, walk, stride, air > 0.0, time, 1.0).with(run, air, landed, null));
    }

    /** 6.2.4: where bone {@code name}'s pivot is as {@code rig} poses it. */
    static double[] pivot(OwlCreature.Rig rig, String name) {
        int b = OwlMesh.get().bone(name);
        return rig.at(b, OwlMesh.get().pivot[b]);
    }

    /** 6.2.4: the lowest of the creature's own mesh (not its kagune) as {@code rig} poses it, and how many quads it is. */
    static double[] lowest(OwlCreature.Rig rig) {
        double[] low = {Double.MAX_VALUE, 0.0};
        com.hex.kagune.control.ukaku.UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            low[1]++;
            for (double[] v : new double[][] {a, b, c, d}) {
                low[0] = Math.min(low[0], Double.isFinite(v[1]) ? v[1] : -1.0E9);
            }
        };
        OwlCreature.mesh(sink, painted(sink), rig);
        return low;
    }

    /**
     * 6.2.4: the creature's gaits: sprinting, it gallops (both hands lifted and swung ahead together, its body heaving up into each
     * bound), never into the ground; in the air its hands reach ahead to land on; landed, it is squashed down under its weight and
     * up again; idle, now and then its head cocked aside as an owl's.
     */
    static void owlCreatureGaits() {
        OwlCreature.Rig rest = standing(0.0, 0.0, 30.0);
        double restHand = span(rest, "right_hand")[0];
        boolean together = false;
        double low = Double.MAX_VALUE;
        double top = -Double.MAX_VALUE;
        double bottom = Double.MAX_VALUE;
        for (double stride = 0.0; stride < Math.PI * 2.0; stride += 0.1) {
            OwlCreature.Rig r = moving(1.0, stride, 1.0, 0.0, OwlCreature.LONG_AGO, 30.0);
            together |= span(r, "right_hand")[0] - restHand > 0.3 && span(r, "left_hand")[0] - restHand > 0.3;
            low = Math.min(low, lowest(r)[0]);
            double y = pivot(r, "torso")[1];
            top = Math.max(top, y);
            bottom = Math.min(bottom, y);
        }
        check(together && top - bottom > 0.12 && low >= -1.0E-9, "sprinting, it gallops: both hands swung ahead together, its body heaving "
            + (top - bottom) + ", nothing under the ground " + low);
        OwlCreature.Rig flying = moving(0.0, 0.0, 0.0, 8.0, OwlCreature.LONG_AGO, 30.0);
        double[] hand = pivot(flying, "right_hand");
        double[] restWrist = pivot(rest, "right_hand");
        check(hand[2] - restWrist[2] > 0.5 && hand[1] - restWrist[1] > 0.3, "in the air its hands reach ahead to land on: "
            + java.util.Arrays.toString(sub3(hand, restWrist)));
        double restBody = pivot(rest, "torso")[1];
        double squashed = pivot(moving(0.0, 0.0, 0.0, 0.0, 1.0, 30.0), "torso")[1];
        double after = pivot(moving(0.0, 0.0, 0.0, 0.0, 12.0, 30.0), "torso")[1];
        check(restBody - squashed > 0.15 && Math.abs(after - restBody) < 1.0E-6 && lowest(moving(0.0, 0.0, 0.0, 0.0, 1.0, 30.0))[0] >= -1.0E-9,
            "landed, squashed down under its weight (" + (restBody - squashed) + ") above the ground, and up again");
        int skull = OwlMesh.get().bone("head");
        double cocked = 0.0;
        for (double time = 0.0; time < 340.0; time += 4.0) {
            cocked = Math.max(cocked, Math.abs(standing(0.0, 0.0, time).pose().turned(skull, new double[] {1.0, 0.0, 0.0})[1]));
        }
        check(cocked > 0.2, "idle, now and then its head cocked aside as an owl's: " + cocked);
    }

    /**
     * 6.2.4: the creature through each of its moves ({@link OwlActs}): a real act throughout, its hands within its arms' reach and
     * above the ground, never jumping, nothing of it under the ground, cheap as ever; its M1's claws raked across before it, its
     * kagune's points driven in ahead, its jaws gaping before the bite and shut on its tick, its head lunged out; its great hand
     * reaching to what it reaches for, round what it holds where the server holds it, slammed down on the ground ahead.
     */
    static void owlActs() {
        Object[][] moves = {{PaleMove.OWL_STRIKE, 0, 0.0F, (double)OwlRules.REND_TICKS[0]}, {PaleMove.OWL_STRIKE, 1, 0.0F, (double)OwlRules.REND_TICKS[1]},
            {PaleMove.OWL_STRIKE, 2, 0.0F, (double)OwlRules.REND_TICKS[2]}, {PaleMove.OWL_STRIKE, 3, 0.0F, (double)OwlRules.REND_TICKS[3]},
            {PaleMove.OWL_WHIRL, 0, 0.0F, (double)OwlRules.WHIRL_TICKS}, {PaleMove.OWL_LEAP, 0, 0.0F, 12.0},
            {PaleMove.OWL_LEAP, 0, 8.0F, 8.0 + OwlRules.LEAP_RECOVER}, {PaleMove.OWL_FISSURE_CHARGE, 0, 0.0F, 30.0},
            {PaleMove.OWL_FISSURE, 0, 0.0F, (double)OwlRules.FISSURE_TICKS}, {PaleMove.OWL_PUNCH_CHARGE, 0, 0.0F, 30.0},
            {PaleMove.OWL_PUNCH, 0, 0.0F, (double)PaleRules.PUNCH_TICKS}, {PaleMove.OWL_KICK, 4, 0.0F, 4.0 + OwlRules.KICK_TICKS},
            {PaleMove.OWL_WATCH, 0, 0.0F, (double)OwlRules.LANCES_POSE_TICKS}, {PaleMove.OWL_VOLLEY, 1, 0.0F, 40.0},
            {PaleMove.OWL_VOLLEY, 1, 20.0F, (double)OwlShards.ends(true, 20.0F)}, {PaleMove.OWL_GRAB_REACH, 0, 5.0F, 5.0},
            {PaleMove.OWL_GRAB_HOLD, 0, 0.0F, 60.0}, {PaleMove.OWL_GRAB_SLAM, 0, 0.0F, (double)OwlRules.GRAB_SLAM_TICKS},
            {PaleMove.OWL_GRAB_MISS, 0, 0.0F, (double)OwlRules.GRAB_MISS_TICKS}};
        double[] aim = {-6.0, 30.0, -90.0};
        double[][] shoulders = {{-18.0, 49.5, -12.0}, {18.0, 49.5, -12.0}};
        boolean real = true;
        boolean reach = true;
        double jump = 0.0;
        String jumpAt = "";
        double low = Double.MAX_VALUE;
        int most = 0;
        String where = "";
        for (Object[] m : moves) {
            PaleMove move = (PaleMove)m[0];
            int detail = (Integer)m[1];
            float charge = (Float)m[2];
            double end = (Double)m[3];
            // (Its hands followed a twentieth of a tick at a time: however fast a blow, it never jumps from one place to the next.)
            double[][] last = null;
            for (double t = 0.0; t <= end; t += 0.05) {
                OwlCreature.Rig rig = acting(OwlActs.of(move, t, detail, charge, aim), 30.0 + t);
                double[][] now = {pivot(rig, "right_hand"), pivot(rig, "left_hand")};
                if (last != null && Math.max(length3(sub3(now[0], last[0])), length3(sub3(now[1], last[1]))) > jump) {
                    jump = Math.max(length3(sub3(now[0], last[0])), length3(sub3(now[1], last[1])));
                    jumpAt = move + " " + detail + " at " + String.format(java.util.Locale.ROOT, "%.2f", t);
                }
                last = now;
            }
            for (double t = 0.0; t <= end; t += 0.5) {
                OwlActs.Act act = OwlActs.of(move, t, detail, charge, aim);
                if (act == null || !Double.isFinite(act.pitch() + act.turn() + act.lift() + act.ahead() + act.bow() + act.lunge() + act.jaw() + act.crest())) {
                    real = false;
                    where = move + " " + t;
                    continue;
                }
                double[][] hands = {act.right(), act.left()};
                double[] weights = {act.rightWeight(), act.leftWeight()};
                for (int i = 0; i < 2; i++) {
                    if (hands[i] != null && weights[i] > 0.0 && (length3(sub3(hands[i], shoulders[i])) > 88.5 || hands[i][1] < 0.0 || weights[i] > 1.0)) {
                        reach = false;
                        where = move + " " + detail + " at " + t + ": " + java.util.Arrays.toString(hands[i]);
                    }
                }
                OwlCreature.Rig rig = acting(act, 30.0 + t);
                double[] l = lowest(rig);
                if (l[0] < low) {
                    low = l[0];
                    if (low < 0.0) {
                        where = move + " " + detail + " at " + t;
                    }
                }
                most = Math.max(most, (int)l[1]);
            }
        }
        check(real, "a real act through each of its moves " + where);
        check(reach, "its hands within its arms' reach and above the ground " + where);
        check(jump < 0.4, "its hands never jump: " + jump + " blocks in a twentieth of a tick at the most (" + jumpAt + ")");
        check(low >= -1.0E-9, "nothing of it under the ground through its moves: " + low + " " + where);
        check(most <= 12000, "its moves as cheap as its idle: " + most + " quads");
        // Its M1: the right claw raked across before it, out of its right, through to its left.
        double hit = OwlRules.REND_IMPACT[0];
        double[] wound = pivot(acting(OwlActs.of(PaleMove.OWL_STRIKE, hit - 2.0, 0, 0.0F, null), 30.0), "right_hand");
        double[] struck = pivot(acting(OwlActs.of(PaleMove.OWL_STRIKE, hit, 0, 0.0F, null), 30.0), "right_hand");
        double[] through = pivot(acting(OwlActs.of(PaleMove.OWL_STRIKE, hit + 1.2, 0, 0.0F, null), 30.0), "right_hand");
        check(wound[0] > 3.0 && through[0] < 0.0 && struck[2] > 2.0, "its right claw raked across before it: from " + wound[0] + " through "
            + struck[2] + " ahead to " + through[0]);
        // Its kagune's points driven in ahead.
        OwlCreature.Rig still = standing(0.0, 0.0, 30.0);
        OwlModel.Look restLook = OwlPose.look(still, OwlPose.REST, OwlPose.REST, 30.0, 1.0);
        double stabAt = OwlRules.REND_IMPACT[2] + 1.0;
        OwlActs.Act stab = OwlActs.of(PaleMove.OWL_STRIKE, stabAt, 2, 0.0F, null);
        OwlCreature.Rig stabbing = acting(stab, 30.0);
        OwlPose.Swing thrust = OwlPose.acted(OwlPose.REST, OwlPose.REST, stabAt, stab, 1);
        OwlPose.Swing thrustLeft = OwlPose.acted(OwlPose.REST, OwlPose.REST, stabAt, stab, -1);
        OwlModel.Look stabLook = OwlPose.look(stabbing, thrust, thrustLeft, 30.0, 1.0, stab);
        double gained = Math.min(Math.min(stabLook.right().tip()[2] - restLook.right().tip()[2], stabLook.left().tip()[2] - restLook.left().tip()[2]),
            Math.min(stabLook.rightInner().tip()[2] - restLook.rightInner().tip()[2], stabLook.leftInner().tip()[2] - restLook.leftInner().tip()[2]));
        double tipLow = Math.min(Math.min(stabLook.right().tip()[1], stabLook.left().tip()[1]), Math.min(stabLook.rightInner().tip()[1], stabLook.leftInner().tip()[1]));
        check(gained > 1.0 && tipLow > 0.0, "its kagune's four points driven in ahead: " + gained + " blocks further at the least, " + tipLow + " up");
        // Its bite: its jaws gaping before it, shut on its tick, its head lunged out.
        int jawBone = OwlMesh.get().bone("mask_jaw");
        int skullBone = OwlMesh.get().bone("head");
        double bite = OwlRules.REND_IMPACT[3];
        double[] gape = new double[2];
        double[] reachOut = new double[2];
        for (int i = 0; i < 2; i++) {
            OwlCreature.Rig r = acting(OwlActs.of(PaleMove.OWL_STRIKE, bite - 1.5 + 1.5 * i, 3, 0.0F, null), 30.0);
            double[] front = r.pose().turned(jawBone, new double[] {0.0, 0.0, -1.0});
            double[] face = r.pose().turned(skullBone, new double[] {0.0, 0.0, -1.0});
            gape[i] = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot3(front, face)))));
            reachOut[i] = pivot(r, "head")[2] - pivot(still, "head")[2];
        }
        check(gape[0] > 50.0 && gape[1] < 6.0 && reachOut[1] > 0.6, "its jaws gape before the bite (" + gape[0] + " degrees), shut on it (" + gape[1]
            + "), its head lunged out " + reachOut[1]);
        // Its great hand: reaching for what it reaches for; round what it holds, where the server holds it; slammed down ahead.
        double[] reached = pivot(acting(OwlActs.of(PaleMove.OWL_GRAB_REACH, 5.0, 0, 5.0F, new double[] {-10.0, 30.0, -60.0}), 30.0), "right_hand");
        check(length3(sub3(reached, new double[] {10.0 / 16.0, 30.0 / 16.0, 60.0 / 16.0})) < 0.6, "its great hand reaches what it reaches for: "
            + java.util.Arrays.toString(reached));
        double[] holding = pivot(acting(OwlActs.of(PaleMove.OWL_GRAB_HOLD, 20.0, 0, 0.0F, null), 50.0), "right_hand");
        check(length3(sub3(holding, OwlRules.GRAB_HOLD)) < 0.8, "it holds what it seized where the server holds it: " + java.util.Arrays.toString(holding)
            + " for " + java.util.Arrays.toString(OwlRules.GRAB_HOLD));
        double[] slammed = pivot(acting(OwlActs.of(PaleMove.OWL_GRAB_SLAM, OwlRules.GRAB_SLAM_IMPACT, 0, 0.0F, null), 30.0), "right_hand");
        double[] raised = pivot(acting(OwlActs.of(PaleMove.OWL_GRAB_SLAM, OwlRules.GRAB_SLAM_IMPACT - 2.0, 0, 0.0F, null), 30.0), "right_hand");
        check(raised[1] > 3.5 && slammed[1] < 0.8 && slammed[2] > 2.5, "raised high (" + raised[1] + ") and slammed down on the ground ahead: "
            + java.util.Arrays.toString(slammed));
        // Its spikes: its head lowered under its flared crest.
        OwlCreature.Rig firing = acting(OwlActs.of(PaleMove.OWL_VOLLEY, 12.0, 1, 0.0F, null), 30.0);
        check(pivot(still, "head")[1] - pivot(firing, "head")[1] > 0.25 && firing.flare() > 0.9,
            "firing its spikes, its head lowered under its flared crest: " + (pivot(still, "head")[1] - pivot(firing, "head")[1]));
    }

    /** 6.2.4: the creature's painted quads put to {@code sink} as plain ones (where they are is all these tests look at). */
    static OwlCreature.Skin painted(com.hex.kagune.control.ukaku.UkakuModel.Sink sink) {
        return (t, a, b, c, d, uv, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, new double[] {0.0, 1.0, 0.0}, ca, cb, cc, cd);
    }

    /** 6.2.4: the lowest and highest of bone {@code name}'s quads as {@code rig} poses them ({low y, high y, and the same for z}). */
    static double[] span(OwlCreature.Rig rig, String name) {
        OwlMesh mesh = OwlMesh.get();
        int b = mesh.bone(name);
        double[] out = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int i = 0; i < mesh.quads(); i++) {
            if (mesh.bone[i] != b) {
                continue;
            }
            for (int k = 0; k < 4; k++) {
                int o = i * 12 + k * 3;
                double[] p = rig.at(b, new double[] {mesh.corners[o], mesh.corners[o + 1], mesh.corners[o + 2]});
                out[0] = Math.min(out[0], p[1]);
                out[1] = Math.max(out[1], p[1]);
                out[2] = Math.min(out[2], p[2]);
                out[3] = Math.max(out[3], p[2]);
            }
        }
        return out;
    }

    /** 6.2.4: whether {@code p} is within {@code margin} (blocks) of the box round bone {@code name}'s quads as {@code rig} poses them. */
    static boolean within(OwlCreature.Rig rig, String name, double[] p, double margin) {
        OwlMesh mesh = OwlMesh.get();
        int b = mesh.bone(name);
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int i = 0; i < mesh.quads(); i++) {
            if (mesh.bone[i] != b) {
                continue;
            }
            for (int k = 0; k < 4; k++) {
                int o = i * 12 + k * 3;
                double[] q = rig.at(b, new double[] {mesh.corners[o], mesh.corners[o + 1], mesh.corners[o + 2]});
                for (int a = 0; a < 3; a++) {
                    lo[a] = Math.min(lo[a], q[a]);
                    hi[a] = Math.max(hi[a], q[a]);
                }
            }
        }
        for (int a = 0; a < 3; a++) {
            if (p[a] < lo[a] - margin || p[a] > hi[a] + margin) {
                return false;
            }
        }
        return true;
    }

    /**
     * 6.2.4: the creature hangs together and moves as it should: its right on her right; its kagune out of the red masses on its
     * shoulders (where its blades were), the outer roots above the inner, behind its eye; its hands planted on the ground at rest,
     * each hauled back along it and lifted and swung ahead again as it crawls; its jaw dropping wide open now and then and shut again,
     * its tongue in its mouth throughout, riding its jaw; its head turning the way she looks.
     */
    static void owlCreatureFrames() {
        String worst = "";
        boolean sides = true;
        boolean rooted = true;
        for (double walk : new double[] {0.0, 1.0}) {
            for (double time = 0.0; time < 60.0; time += 5.0) {
                double stride = time * 0.6662 * 1.4 * walk;
                RigPose body = new RigPose();
                OwlPose.body(body, true, OwlRules.GROW_TICKS + 30.0, time);
                body.settle(1.0F);
                OwlCreature.Source src = OwlPose.source(body, walk, stride, false, time, 1.0);
                OwlCreature.Rig r = OwlCreature.rig(src);
                double[] herLeft = unit(src.torso().x());
                double[] herFront = unit(new double[] {-src.torso().z()[0], 0.0, -src.torso().z()[2]});
                double[] eye = OwlCreature.eye(r);
                boolean sd = dot3(sub3(r.rootRight(), r.rootLeft()), herLeft) < 0.0 && dot3(sub3(r.innerRight(), r.innerLeft()), herLeft) < 0.0
                    && dot3(sub3(r.rootRight(), eye), herFront) < 0.0 && dot3(sub3(r.rootLeft(), eye), herFront) < 0.0 && r.rootRight()[1] > r.innerRight()[1];
                sides &= sd;
                double[][] roots = {r.rootRight(), r.rootLeft(), r.innerRight(), r.innerLeft()};
                String[] masses = {"right_shoulder", "left_shoulder", "right_shoulder", "left_shoulder"};
                OwlModel.Look look = OwlPose.look(r, OwlPose.swing(1, time, walk, stride), OwlPose.swing(-1, time, walk, stride), time, 1.0);
                OwlModel.Arm[] limbs = {look.right(), look.left(), look.rightInner(), look.leftInner()};
                for (int i = 0; i < 4; i++) {
                    if (!within(r, masses[i], roots[i], 0.05)) {
                        rooted = false;
                        worst = "root " + i + " off its shoulder's mass";
                    }
                    if (!same3(limbs[i].shoulder(), roots[i])) {
                        rooted = false;
                        worst = "limb " + i + " not out of its root";
                    }
                }
            }
        }
        check(sides, "its right on her right, its outer roots above its inner, all behind its eye");
        check(rooted, "its kagune out of the masses on its shoulders, where its blades were " + worst);
        // At rest its hands are planted on the ground, out wide.
        OwlCreature.Rig rest = standing(0.0, 0.0, 30.0);
        boolean planted = true;
        for (String side : new String[] {"right", "left"}) {
            double low = Double.MAX_VALUE;
            for (String part : new String[] {"_finger_1", "_finger_2", "_finger_3", "_finger_4", "_thumb", "_hand"}) {
                low = Math.min(low, span(rest, side + part)[0]);
            }
            planted &= low < 0.08;
        }
        check(planted, "its hands planted on the ground at rest");
        // Crawling: each hand hauled back along the ground and lifted and swung ahead again, the two half a stride apart.
        double[] rise = new double[2];
        double[] fore = {Double.MAX_VALUE, Double.MAX_VALUE};
        double[] aft = {-Double.MAX_VALUE, -Double.MAX_VALUE};
        boolean both = false;
        double restHand = span(rest, "right_hand")[0];
        for (double stride = 0.0; stride < Math.PI * 2.0; stride += 0.1) {
            OwlCreature.Rig r = standing(1.0, stride, 30.0);
            double[][] hands = {span(r, "right_hand"), span(r, "left_hand")};
            for (int i = 0; i < 2; i++) {
                rise[i] = Math.max(rise[i], hands[i][0] - restHand);
                fore[i] = Math.min(fore[i], hands[i][2]);
                aft[i] = Math.max(aft[i], hands[i][2]);
            }
            both |= hands[0][0] - restHand > 0.2 && hands[1][0] - restHand > 0.2;
        }
        check(rise[0] > 0.25 && rise[1] > 0.25 && aft[0] - fore[0] > 0.7 && aft[1] - fore[1] > 0.7 && !both,
            "crawling, each hand hauls back along the ground and is lifted and swung ahead, in turn: lifted " + rise[0] + ", " + rise[1] + ", swept "
                + (aft[0] - fore[0]) + ", " + (aft[1] - fore[1]));
        // Its jaw drops wide now and then (turned down off its skull), its tongue out of it; closed, the tongue is hidden in its mouth.
        int jawBone = OwlMesh.get().bone("mask_jaw");
        int skullBone = OwlMesh.get().bone("head");
        double widest = 0.0;
        double reached = -Double.MAX_VALUE;
        double shut = 0.0;
        double hidden = -Double.MAX_VALUE;
        for (double time = 0.0; time < 230.0; time += 2.0) {
            OwlCreature.Rig r = standing(0.0, 0.0, time);
            double[] jaw = span(r, "mask_jaw");
            double[] front = r.pose().turned(jawBone, new double[] {0.0, 0.0, -1.0});
            double[] face = r.pose().turned(skullBone, new double[] {0.0, 0.0, -1.0});
            double open = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot3(front, face)))));
            double tongue = span(r, "mask_tongue")[3];
            widest = Math.max(widest, open);
            reached = Math.max(reached, tongue - jaw[3]);
            if (time > 110.0) {
                shut = Math.max(shut, open);
                hidden = Math.max(hidden, tongue - jaw[3]);
            }
        }
        check(widest > 50.0, "its jaw drops wide open now and then: " + widest + " degrees");
        check(reached < 0.0, "its tongue in its mouth, riding its jaw, open or shut: " + reached + " blocks past its jaw at the most");
        check(shut < 1.0 && hidden < 0.0, "closed again, its jaw shut and its tongue in its mouth: " + shut + " degrees, " + hidden);
        // Its head turns the way she looks.
        RigPose looking = new RigPose();
        OwlPose.body(looking, true, OwlRules.GROW_TICKS + 30.0, 30.0);
        looking.headY -= 0.6F;
        looking.settle(1.0F);
        OwlCreature.Rig turned = OwlCreature.rig(OwlPose.source(looking, 0.0, 0.0, false, 30.0, 1.0));
        double[] face = unit(new double[] {-turned.head().z()[0], 0.0, -turned.head().z()[2]});
        double[] ahead = unit(new double[] {-rest.head().z()[0], 0.0, -rest.head().z()[2]});
        check(Math.abs(face[0] - ahead[0]) > 0.2, "its head turns the way she looks: " + java.util.Arrays.toString(face) + " from " + java.util.Arrays.toString(ahead));
    }

    static boolean same3(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]) < 1.0E-9;
    }

    static double length3(double[] a) {
        return Math.sqrt(dot3(a, a));
    }

    /** How deep (pixels) her own head goes into her torso, her bulked chest or her arms (nodding, the chest's top meets her jaw). */
    /**
     * 6.2.0: the Owl's own moves. Its Big Punch: the struck body flies dead level exactly as far as the charge sends it, slowing
     * evenly from the blow's full force; her body and both giant arms never through her through the charge and the punch (out of her
     * idle in it, at any charge), nothing of it into the ground but the braced blade's point resting on it, the giant arms never
     * jumping (into the move, through it, back into her idle), the blade trailing its upper arm through the drive and bowing with it.
     */
    /**
     * 6.2.0, the Owl's Watch This: six lances fanned round her, each diving down through what it hunts into the ground, hoisting it
     * up over her (clear of her head) and slamming it down on her far side; never through her or into the ground in the crown; cheap.
     */
    static void owlLances() {
        check(OwlLances.Phase.byOrdinal(0) == OwlLances.Phase.NONE && OwlLances.Phase.byOrdinal(8) == OwlLances.Phase.IN
            && OwlLances.Phase.byOrdinal(99) == OwlLances.Phase.NONE && PaleMove.OWL_WATCH.owl() && OwlMotions.armed(PaleMove.OWL_WATCH),
            "the lances' phases are sent by ordinal; its body move is the Owl's own, the giant arms keyed through it");
        double closest = Double.MAX_VALUE;
        for (int i = 0; i < OwlLances.COUNT; i++) {
            for (int j = i + 1; j < OwlLances.COUNT; j++) {
                closest = Math.min(closest, turned(OwlLances.bearing(i) - OwlLances.bearing(j)));
            }
            check(OwlLances.side(i) * Math.sin(OwlLances.bearing(i)) > 0.0 && OwlLances.px(i)[0] * OwlLances.side(i) < 0.0,
                "each lance grows out of its own side of her back and hunts on that side: " + i);
        }
        check(closest > 0.6, "the six hunt all round her, each its own way: " + closest);
        // The dive: steeply down through the chest and on into the ground a little, whatever its height and wherever it stands.
        double worstThrough = 0.0;
        double worstGround = Double.MAX_VALUE;
        double shallowest = -1.0;
        for (double chestUp : new double[] {0.35, 0.8, 1.24, 1.8}) {
            for (double angle = 0.0; angle < Math.PI * 2.0; angle += 0.4) {
                for (double distance : new double[] {1.5, 4.0, 9.0}) {
                    double[] chest = {Math.sin(angle) * distance, chestUp, Math.cos(angle) * distance};
                    double[] dir = OwlLances.diveDir(chest);
                    shallowest = Math.max(shallowest, dir[1]);
                    double[] knot = OwlLances.through(chest, dir, chestUp);
                    double[] point = {knot[0] + dir[0] * OwlLances.SPEAR, knot[1] + dir[1] * OwlLances.SPEAR, knot[2] + dir[2] * OwlLances.SPEAR};
                    worstThrough = Math.max(worstThrough, segmentDistance(knot, point, chest));
                    worstGround = Math.min(worstGround, -point[1]);
                }
            }
        }
        check(shallowest < -0.85 && worstThrough < 1.0E-6 && worstGround > 0.0, "the spear comes down steeply through the chest and on into the ground: "
            + shallowest + ", " + worstThrough + " off the chest, its point " + worstGround + " under the ground at the least");
        // The hoist: from where it was pinned, up over her (its feet clear above her head), down on her far side; smooth, heavy then fast.
        double worstStep = 0.0;
        double lowestOver = Double.MAX_VALUE;
        double highest = 0.0;
        boolean far = true;
        boolean onward = true;
        double previousU = -1.0;
        for (double t = 0.0; t <= OwlRules.LANCE_HOIST + 1.0; t += 0.25) {
            double u = OwlLances.hoistU(t);
            onward &= u >= previousU && u >= 0.0 && u <= 1.0;
            previousU = u;
        }
        for (double angle = 0.0; angle < Math.PI * 2.0; angle += 0.5) {
            for (double distance : new double[] {1.2, 3.0, 6.0, 10.0}) {
                double chestUp = 1.24;
                double[] pinned = {Math.sin(angle) * distance, chestUp, Math.cos(angle) * distance};
                double[] slam = OwlLances.slamPoint(pinned, chestUp, 0.0);
                double flat = Math.hypot(slam[0], slam[2]);
                far &= slam[0] * pinned[0] + slam[2] * pinned[2] < 0.0 && flat >= 2.5 - 1.0E-9 && flat <= 4.5 + 1.0E-9 && Math.abs(slam[1] - chestUp) < 1.0E-9;
                double[] last = OwlLances.hoist(0.0, pinned, slam);
                for (double u = 0.01; u <= 1.0 + 1.0E-9; u += 0.01) {
                    double[] p = OwlLances.hoist(u, pinned, slam);
                    worstStep = Math.max(worstStep, Math.sqrt(Math.pow(p[0] - last[0], 2) + Math.pow(p[1] - last[1], 2) + Math.pow(p[2] - last[2], 2)));
                    highest = Math.max(highest, p[1]);
                    // Crossing over her (its feet clear over her head, and over her bulked shoulders on the way).
                    double from = Math.hypot(p[0], p[2]);
                    if (from < 0.7) {
                        lowestOver = Math.min(lowestOver, p[1] - chestUp - 2.3);
                    } else if (from < 1.3 && u > 0.15 && u < 0.85) {
                        lowestOver = Math.min(lowestOver, p[1] - chestUp - 1.9);
                    }
                    last = p;
                }
                double[] end = OwlLances.hoist(1.0, pinned, slam);
                far &= Math.abs(end[0] - slam[0]) + Math.abs(end[1] - slam[1]) + Math.abs(end[2] - slam[2]) < 1.0E-9;
            }
        }
        check(onward && OwlLances.hoistU(0.0) == 0.0 && OwlLances.hoistU(OwlRules.LANCE_HOIST) == 1.0, "the hoist runs once, only ever on");
        check(far, "slammed down on her far side, 2.5 to 4.5 blocks out, its chest at its height over the ground there");
        check(worstStep < 0.5 && highest > 4.0 && lowestOver > 0.0, "hoisted smoothly well up over her, its feet clear over her head and shoulders: "
            + worstStep + " a step, " + highest + " high, feet " + lowestOver + " clear of her passing over");
        // In the crown (and bursting out, and going in), every lance clear of her body and her bulked chest, off the ground; each cheap.
        RigPose stance = OwlMotions.body(PaleMove.OWL_WATCH, OwlRules.LANCES_POSE_TICKS, 0, 0.0F, null);
        stance.settle(1.0F);
        double deepest = 0.0;
        double lowest = Double.MAX_VALUE;
        int most = 0;
        String where = "";
        for (double age = 0.0; age <= OwlRules.LANCES_POSE_TICKS + 40.0; age += 1.5) {
            RigPose body = age < OwlRules.LANCES_POSE_TICKS ? OwlMotions.body(PaleMove.OWL_WATCH, age, 0, 0.0F, null) : stance;
            if (body != stance) {
                body.settle(1.0F);
            }
            BodyFrame.Solid solid = new BodyFrame.Solid(body);
            OwlModel.Look look = OwlPose.look(body, KakuganEyes.DEFAULT, OwlPose.REST, OwlPose.REST, age, 1.0);
            for (int i = 0; i < OwlLances.COUNT; i++) {
                double since = age - OwlRules.LANCES_ERUPT_AT;
                if (since < 0.0) {
                    continue;
                }
                OwlLances.Tip tip = since < OwlRules.LANCES_ERUPT ? OwlLances.tip(i, OwlLances.Phase.OUT, since, 0, null, 1.0, null, age)
                    : OwlLances.crown(i, age);
                double[][] path = OwlLancePose.path(body, i, tip);
                double[] root = path[0];
                double[] worst = {0.0, Double.MAX_VALUE};
                int[] quads = {0};
                OwlLances.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                    quads[0]++;
                    for (double[] v : new double[][] {a, b, c, d}) {
                        if (Math.sqrt(Math.pow(v[0] - root[0], 2) + Math.pow(v[1] - root[1], 2) + Math.pow(v[2] - root[2], 2)) < 0.45) {
                            continue; // where it tears out of her
                        }
                        worst[0] = Math.max(worst[0], Math.max(solid.depth(v, 0.0), chestDepth(look.torso(), v, body.bulk)) - CLEARANCE);
                        worst[1] = Math.min(worst[1], v[1]);
                    }
                }, path, tip.dir(), tip.grown(), 7 * i, age);
                most = Math.max(most, quads[0]);
                if (worst[0] > deepest) {
                    deepest = worst[0];
                    where = " (lance " + i + ", tick " + age + ")";
                }
                lowest = Math.min(lowest, worst[1]);
            }
        }
        check(deepest <= 0.0, "never a lance through her: " + deepest + " px" + where);
        check(lowest > 0.5, "the crown stands up off the ground: " + lowest);
        check(most > 60 && most <= 400, "each lance is cheap: " + most + " quads at the most");
        // Through the dive the spear comes down onto its mark and is through it by the end; pinned, it stays through it.
        double[] mark = {2.8, 1.24, 2.8};
        int dive = OwlLances.diveTicks(4.0);
        OwlLances.Tip struck = OwlLances.tip(0, OwlLances.Phase.STRIKE, OwlLances.strikeTicks(dive), dive, mark, 1.24, null, 0.0);
        OwlLances.Tip pinned = OwlLances.tip(0, OwlLances.Phase.PIN, 3.0, dive, mark, 1.24, null, 0.0);
        check(segmentDistance(struck.knot(), OwlLances.point(struck), mark) < 0.05 && segmentDistance(pinned.knot(), OwlLances.point(pinned), mark) < 0.05,
            "the spear through its mark as the dive ends, and held there pinned");
        double jumpLance = 0.0;
        double[] lastPoint = null;
        for (double age = 0.0; age <= OwlLances.strikeTicks(dive); age += 0.25) {
            double[] point = OwlLances.point(OwlLances.tip(0, OwlLances.Phase.STRIKE, age, dive, mark, 1.24, null, 0.0));
            if (lastPoint != null) {
                jumpLance = Math.max(jumpLance, Math.sqrt(Math.pow(point[0] - lastPoint[0], 2) + Math.pow(point[1] - lastPoint[1], 2) + Math.pow(point[2] - lastPoint[2], 2)));
            }
            lastPoint = point;
        }
        check(jumpLance < 2.0, "the dive never jumps: " + jumpLance + " blocks in a quarter tick");
        // Her body and the giant arms through it, as every Owl move.
        owlSequence("the Owl's Watch This", new Object[][] {{PaleMove.OWL_WATCH, (double)OwlRules.LANCES_POSE_TICKS, 0, 0.0F}});
    }

    /**
     * 6.2.0, the Owl's spikes (6.2.4: J held, off its crest): two bursts first, one after another while held; the spikes go where she
     * aims and leave from the creature's crest, its head lowered under it; her body and the giant arms through it as every Owl move;
     * each spike cheap.
     */
    static void owlVolley() {
        check(PaleMove.OWL_VOLLEY.owl() && OwlMotions.armed(PaleMove.OWL_VOLLEY) && PaleMotions.facesLook(PaleMove.OWL_VOLLEY),
            "the volley is the Owl's own, the giant arms keyed through it, thrown where she looks");
        check(OwlShards.throwAt(0) == OwlShards.FIRST_RIGHT && OwlShards.throwAt(1) == OwlShards.FIRST_LEFT
            && OwlShards.throwAt(3) - OwlShards.throwAt(2) == OwlShards.STORM_EVERY && OwlShards.side(0) == 1 && OwlShards.side(1) == -1
            && OwlShards.side(2) == 1, "the first burst, the next, then one after another");
        check(OwlShards.throwsMade(false, 0.0F) == 2 && OwlShards.throwsMade(true, 12.0F) == 3
            && OwlShards.throwAt(OwlShards.throwsMade(true, 0.0F) - 1) <= OwlShards.STORM_MOST
            && OwlShards.ends(false, 0.0F) == OwlShards.FIRST_LEFT + OwlShards.RECOVER, "tapped, two throws; held, as long as it is (and no longer than the most)");
        boolean apart = true;
        for (int side : new int[] {1, -1}) {
            for (double age = 0.0; age < 80.0; age += 0.25) {
                int k = OwlShards.throwOf(side, age, true, 0.0F);
                if (k >= 0) {
                    apart &= OwlShards.side(k) == side && age - OwlShards.throwAt(k) >= OwlShards.WIND_FROM && age - OwlShards.throwAt(k) < OwlShards.READY;
                }
            }
        }
        check(apart, "each arm's throws come one after another, never two at once");
        // Where she aims: the tap's middle spears get there (their drop and their lift no more than a body's breadth off); every spear
        // of the storm lands within its scatter of it.
        double worstAim = 0.0;
        double worstScatter = 0.0;
        for (int k = 0; k < 8; k++) {
            double[] from = OwlShards.release(k);
            for (double distance : new double[] {5.0, 10.0, 16.0, 22.0}) {
                double[] target = {0.4, 1.0, distance};
                for (int j = 0; j < OwlShards.spears(k); j++) {
                    double[] d = OwlShards.direction(from, target, k, j);
                    double[] v = {d[0] * OwlShards.SPEED, d[1] * OwlShards.SPEED, d[2] * OwlShards.SPEED};
                    double best = Double.MAX_VALUE;
                    for (double t = 0.0; t <= OwlShards.LIFE; t += 0.05) {
                        double[] p = OwlShards.at(from, v, t);
                        best = Math.min(best, Math.sqrt(Math.pow(p[0] - target[0], 2) + Math.pow(p[1] - target[1], 2) + Math.pow(p[2] - target[2], 2)));
                    }
                    if (k < 2 && j == OwlShards.spears(k) / 2) {
                        worstAim = Math.max(worstAim, best);
                    } else {
                        worstScatter = Math.max(worstScatter, best / distance);
                    }
                }
            }
        }
        check(worstAim < 0.6 && worstScatter < 0.14, "the spears go where she aims: the tap's middle ones " + worstAim
            + " off at the worst, the rest fanned and scattered " + worstScatter + " a block out at the most");
        // They leave from the creature's crest as it fires them (its head lowered under it): each burst's release within a block of it.
        double worstRelease = 0.0;
        for (int k = 0; k < 6; k++) {
            double age = OwlShards.throwAt(k);
            OwlCreature.Rig rig = acting(OwlActs.of(PaleMove.OWL_VOLLEY, age, 1, 0.0F, null), age);
            double[] release = OwlShards.release(k);
            worstRelease = Math.max(worstRelease, length3(sub3(release, rig.crest())));
        }
        check(worstRelease < 1.0, "the spikes leave from its crest: " + worstRelease + " blocks off at the worst");
        int[] quads = {0};
        OwlShards.mesh((a, b, c, d, n, ca, cb, cc, cd) -> quads[0]++, new double[] {0.0, 1.0, 0.0}, new double[] {0.0, 0.0, 1.0}, 1.0, 7);
        check(quads[0] >= 8 && quads[0] <= 150, "a spike is cheap (the storm fires many): " + quads[0] + " quads");
        owlSequence("the Owl's blade volley, tapped", new Object[][] {{PaleMove.OWL_VOLLEY, (double)OwlShards.ends(false, 0.0F), 0, 0.0F}});
        owlSequence("the Owl's blade volley, held", new Object[][] {{PaleMove.OWL_VOLLEY, (double)OwlShards.ends(true, 24.0F), 1, 24.0F}});
    }

    /**
     * 6.2.0, Owl DNA: the path is not named for a character: its id is owl, and nothing a player can see names her aliases or her
     * gang: no lang value, no text in the mod's code (every string literal of it, ids and keys aside), no stage name. 1.2.5: no
     * character of the show is named anywhere in the mod's jar (the build checks every entry); Owl DNA and Graft DNA are dormant,
     * never pickable, so their ids read as none (the player chooses again).
     */
    static void owlDna() {
        check(KagunePath.PALE.id().equals("owl") && KagunePath.PALE.displayName().equals("Owl DNA") && KagunePath.fromId("owl") == KagunePath.NONE
            && KagunePath.fromId(" OWL ") == KagunePath.NONE && KagunePath.fromId("graft") == KagunePath.NONE && KagunePath.fromId("ito") == KagunePath.NONE
            && KagunePath.fromId("rinkaku") == KagunePath.RINKAKU && KagunePath.fromId("ukaku") == KagunePath.UKAKU
            && KagunePath.choosable().equals(List.of(KagunePath.RINKAKU, KagunePath.UKAKU)),
            "the dormant paths are never pickable and their ids read as none; Rinkaku and Ukaku are the paths to choose");
        java.util.regex.Pattern named = java.util.regex.Pattern.compile("(?i)\\b(takatsuki|aogiri|yoshimura)\\b");
        for (int mastery : new int[] {0, 250, 500, 750, 1000}) {
            check(!named.matcher(com.hex.kagune.control.pale.PaleUnlock.stageName(mastery)).find(), "its stage names are its own: " + com.hex.kagune.control.pale.PaleUnlock.stageName(mastery));
        }
        List<String> seen = new java.util.ArrayList<>();
        try {
            String lang = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/assets/hexkagune/lang/en_us.json")),
                java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher pair = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(lang);
            int values = 0;
            while (pair.find()) {
                values++;
                if (named.matcher(pair.group(2)).find()) {
                    seen.add("lang " + pair.group(1) + ": " + pair.group(2));
                }
            }
            check(values > 20, "the lang file was read: " + values + " values");
            int literals = 0;
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(java.nio.file.Paths.get("src/main/java"))) {
                for (java.nio.file.Path file : (Iterable<java.nio.file.Path>)files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                    for (String literal : literals(new String(java.nio.file.Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8))) {
                        literals++;
                        // (An id or a key, lower case and unspaced, is the code's own: a saved path's old id, a lang key.)
                        if (!literal.matches("[a-z0-9_.:/-]*") && named.matcher(literal).find()) {
                            seen.add(file.getFileName() + ": \"" + literal + "\"");
                        }
                    }
                }
            }
            check(literals > 1000, "the mod's code was read: " + literals + " string literals");
        } catch (java.io.IOException e) {
            check(false, "the sources could not be read: " + e);
        }
        check(seen.isEmpty(), "nothing a player sees names her aliases or her gang: " + seen);
    }

    /** Every string literal in Java source {@code java} (comments and character literals skipped), unescaped only as far as quotes. */
    static List<String> literals(String java) {
        List<String> out = new java.util.ArrayList<>();
        int i = 0;
        int n = java.length();
        while (i < n) {
            char c = java.charAt(i);
            if (c == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int end = java.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
            } else if (c == '\'') {
                i++;
                while (i < n && java.charAt(i) != '\'') {
                    i += java.charAt(i) == '\\' ? 2 : 1;
                }
                i++;
            } else if (c == '"') {
                StringBuilder literal = new StringBuilder();
                i++;
                while (i < n && java.charAt(i) != '"') {
                    if (java.charAt(i) == '\\' && i + 1 < n) {
                        literal.append(java.charAt(i + 1));
                        i += 2;
                    } else {
                        literal.append(java.charAt(i++));
                    }
                }
                i++;
                out.add(literal.toString());
            } else {
                i++;
            }
        }
        return out;
    }

    static void owlMoves() {
        // 6.2.1: in the Owl she tanks it: a 20-damage blow reaches her as 3.
        check(Math.abs(OwlRules.taken(20.0F) - 3.0F) < 1.0E-4F && OwlRules.TAKEN <= 0.2F, "the Owl takes only a sliver of any hurt");
        check(OwlRules.punchDistance(0.0F) == OwlRules.PUNCH_NEAR && OwlRules.punchDistance(1.0F) == OwlRules.PUNCH_FAR
            && OwlRules.punchDistance(0.5F) > OwlRules.punchDistance(0.15F) && OwlRules.PUNCH_NEAR >= 15.0 && OwlRules.PUNCH_SMASH_REACH == 10.0,
            "the Owl's punch: a tap already sends it far, full charge enormously far; ten blocks of terrain broken");
        double lastDistance = 0.0;
        for (float charge : new float[] {0.15F, 0.5F, 1.0F}) {
            double distance = OwlRules.punchDistance(charge);
            double speed = OwlRules.punchSpeed(charge);
            double decel = OwlRules.punchDecel(speed, distance);
            check(OwlRules.punchStep(speed, 0.0, distance) == speed, "off the blow at its full force: charge " + charge);
            double travelled = 0.0;
            double previous = Double.MAX_VALUE;
            boolean slowing = true;
            int ticks = 0;
            while (ticks < 400) {
                double step = OwlRules.punchStep(speed, travelled, distance);
                if (step < 0.05) {
                    break;
                }
                slowing &= step <= previous + 1.0E-9;
                previous = step;
                travelled += step;
                speed = OwlRules.slowed(speed, decel);
                ticks++;
            }
            check(Math.abs(travelled - distance) < 0.06, "it flies as far as the charge sends it: " + travelled + " of " + distance);
            check(slowing && ticks < OwlRules.PUNCH_FLIGHT_TICKS && ticks > 8, "slowing evenly all the way, watched to its end: " + ticks + " ticks");
            check(distance > lastDistance, "further the more it is charged");
            lastDistance = distance;
        }
        check(PaleMove.OWL_PUNCH_CHARGE.owl() && PaleMove.OWL_PUNCH.owl() && !PaleMove.WATCH.owl() && !PaleMove.NONE.owl() && !PaleMove.PUNCH.owl(),
            "the Owl's moves are its own");
        check(PaleMove.byOrdinal(PaleMove.OWL_PUNCH.ordinal()) == PaleMove.OWL_PUNCH && PaleMotions.animates(PaleMove.OWL_PUNCH)
            && PaleMotions.facesLook(PaleMove.OWL_PUNCH_CHARGE) && PaleMotions.continues(PaleMove.OWL_PUNCH_CHARGE, PaleMove.OWL_PUNCH)
            && !PaleMotions.upperBody(PaleMove.OWL_PUNCH), "its punch charges facing her look, carries on into the blow, takes her whole body");
        // Every frame, as the game draws it: out of her idle in the Owl, the charge held as long as it is, let go; the kick from
        // dashes short and long.
        for (float charge : new float[] {0.15F, 0.5F, 1.0F}) {
            double held = PaleRules.PUNCH_CHARGE_TICKS * charge;
            owlSequence("the Owl's punch, charge " + charge, new Object[][] {{PaleMove.OWL_PUNCH_CHARGE, held, 0, 0.0F}, {PaleMove.OWL_PUNCH,
                (double)PaleRules.PUNCH_TICKS, 0, charge}});
        }
        check(OwlRules.nextRend(-1) == 0 && OwlRules.nextRend(0) == 1 && OwlRules.nextRend(2) == 3 && OwlRules.nextRend(3) == 0,
            "the Rend: four cuts, then the first again");
        for (int stage = 0; stage < OwlRules.REND_STAGES; stage++) {
            check(OwlRules.REND_IMPACT[stage] < OwlRules.REND_TICKS[stage] && OwlRules.REND_IMPACT[stage] < OwlRules.REND_CHAIN[stage]
                && OwlRules.REND_REACH[stage] > OwlRules.REND_STAND && OwlRules.REND_DAMAGE[stage] > 1.0F,
                "each cut lands before it can be chained, reaching past where she stands to it: cut " + stage);
        }
        check(OwlRules.REND_DAMAGE[3] > OwlRules.REND_DAMAGE[0] * 1.5, "the overhead finisher the heaviest");
        // The Pounce: a bound further and flatter than a flip (higher still with Up We Go), landing short and long, the slam after.
        for (PaleRules.Flip way : PaleRules.Flip.values()) {
            double[] plain = OwlRules.leap(way, false);
            double[] soared = OwlRules.leap(way, true);
            check(plain[0] > way.speed && plain[1] < way.up && soared[0] > plain[0] && soared[1] > plain[1],
                "the pounce: further and flatter than a flip, more again with Up We Go: " + way);
            check(PaleRules.Flip.byOrdinal(OwlRules.leapDetail(way, true) & 15) == way && (OwlRules.leapDetail(way, true) & 16) != 0
                && (OwlRules.leapDetail(way, false) & 16) == 0, "its detail carries its way and whether it soars");
        }
        for (double landedAt : new double[] {OwlRules.LEAP_CROUCH + 5.0, 14.0, 24.0}) {
            owlSequence("the Owl's pounce, landing at " + landedAt, new Object[][] {{PaleMove.OWL_LEAP, landedAt + OwlRules.LEAP_RECOVER, 0, (float)landedAt}});
        }
        // The Fissure: longer the more it is charged, running out from where the blades come down; the charge and the slam clean.
        check(OwlRules.fissureLength(0.0F) >= 5.0 && OwlRules.fissureLength(1.0F) > OwlRules.fissureLength(0.5F)
            && OwlRules.fissureRun(OwlRules.FISSURE_IMPACT - 1.0, 1.0F) < 0.0 && OwlRules.fissureRun(OwlRules.FISSURE_IMPACT, 1.0F) == 0.0
            && OwlRules.fissureRun(100.0, 0.5F) == OwlRules.fissureLength(0.5F), "the Fissure: from where the blades land, as long as the charge sends it");
        check(OwlRules.FISSURE_IMPACT + OwlRules.fissureLength(1.0F) / OwlRules.FISSURE_SPEED < OwlRules.FISSURE_TICKS,
            "the split runs all the way out before she is herself again");
        for (double held : new double[] {4.0, 18.0, PaleRules.HAMMER_GROW_TICKS + 12.0}) {
            float charge = PaleRules.hammerCharge(held);
            owlSequence("the Owl's Fissure, held " + held, new Object[][] {{PaleMove.OWL_FISSURE_CHARGE, held, 0, 0.0F},
                {PaleMove.OWL_FISSURE, (double)OwlRules.FISSURE_TICKS, 0, charge}});
        }
        // The Reaping Whirl: a whole turn, speeding up and slowing smoothly, the blades over everything round her once.
        double previousTurn = 0.0;
        boolean onward = true;
        for (double t = 0.0; t <= OwlRules.WHIRL_TICKS; t += 0.25) {
            double turn = OwlRules.whirlTurn(t);
            onward &= turn >= previousTurn - 1.0E-12 && turn - previousTurn < 0.45;
            previousTurn = turn;
        }
        check(onward && OwlRules.whirlTurn(OwlRules.WHIRL_START) == 0.0 && Math.abs(OwlRules.whirlTurn(OwlRules.WHIRL_END) - Math.PI * 2.0) < 1.0E-9,
            "the whirl: a whole turn, only ever on round, never a jolt");
        RigPose whirling = OwlMotions.body(PaleMove.OWL_WHIRL, 8.0, 0, 0.0F, null);
        check(Math.abs(whirling.spin - OwlRules.whirlTurn(8.0)) < 1.0E-5, "her body turns exactly as the server reaps: " + whirling.spin);
        owlSequence("the Owl's whirl", new Object[][] {{PaleMove.OWL_WHIRL, (double)OwlRules.WHIRL_TICKS, 0, 0.0F}});
        // The Rend, all four cuts chained (each out of the last), and each alone out of her idle.
        owlSequence("the Owl's Rend, chained", new Object[][] {{PaleMove.OWL_STRIKE, (double)OwlRules.REND_CHAIN[0], 0, 0.0F},
            {PaleMove.OWL_STRIKE, (double)OwlRules.REND_CHAIN[1], 1, 0.0F}, {PaleMove.OWL_STRIKE, (double)OwlRules.REND_CHAIN[2], 2, 0.0F},
            {PaleMove.OWL_STRIKE, (double)OwlRules.REND_TICKS[3], 3, 0.0F}});
        for (int stage = 0; stage < OwlRules.REND_STAGES; stage++) {
            owlSequence("the Owl's Rend, cut " + stage, new Object[][] {{PaleMove.OWL_STRIKE, (double)OwlRules.REND_TICKS[stage], stage, 0.0F}});
        }
        for (int dash : new int[] {OwlRules.KICK_DASH_MIN, 4, OwlRules.KICK_DASH_MAX}) {
            owlSequence("the Owl's kick, dash " + dash, new Object[][] {{PaleMove.OWL_KICK, (double)(dash + OwlRules.KICK_TICKS), dash, 0.0F}});
        }
        // Its Speed Kick: the dash quick (never past the server's ten blocks a tick), held there, stepping in through the spin; the
        // skewer through what she kicks, the slash across its neck, the heel into it, each where the server lands it.
        check(OwlRules.kickDash(0.0) == OwlRules.KICK_DASH_MIN && OwlRules.kickDash(1000.0) == OwlRules.KICK_DASH_MAX
            && OwlRules.kickDash(12.0) >= OwlRules.kickDash(6.0), "the dash: quick, longer the further");
        check(OwlRules.KICK_SKEWER < OwlRules.KICK_SLASH && OwlRules.KICK_SLASH < OwlRules.KICK_PULL && OwlRules.KICK_PULL < OwlRules.KICK_BLOW
            && OwlRules.KICK_BLOW < OwlRules.KICK_TICKS, "skewer, slash, the blade drawn out, the heel, in that order");
        for (double distance : new double[] {1.0, 6.0, 15.0, PaleRules.KICK_RANGE}) {
            int dash = OwlRules.kickDash(distance);
            double[] to = {0.0, 0.0, distance};
            double[] last = {0.0, 0.0, 0.0};
            double fastest = 0.0;
            double age = 0.0;
            for (; age < 100.0; age += 1.0) {
                double[] at = OwlRules.kickPlace(to, dash, 0.0, age);
                if (at == null) {
                    break;
                }
                fastest = Math.max(fastest, Math.sqrt(Math.pow(at[0] - last[0], 2) + Math.pow(at[1] - last[1], 2) + Math.pow(at[2] - last[2], 2)));
                last = at;
            }
            check(fastest < 10.0, "the dash never faster than a server lets her move: " + fastest + " over " + distance);
            check(age == dash + OwlRules.KICK_TICKS, "held through it all, her own again at its end");
            double[] planted = OwlRules.kickPlace(to, dash, 0.0, dash);
            double[] stepped = OwlRules.kickPlace(to, dash, 0.0, dash + OwlRules.KICK_BLOW);
            check(Math.abs(planted[2] - distance) < 1.0E-9 && Math.abs(stepped[2] - distance - OwlRules.KICK_STEP) < 1.0E-9,
                "planted where she aimed, stepped in at it by the heel");
        }
        int kickDash = 3;
        OwlArms kickArms = new OwlArms();
        OwlModel.Look skewered = null;
        OwlModel.Look slashed = null;
        double clock = 50.0;
        for (double age = 0.0; age <= kickDash + OwlRules.KICK_PULL; age += 0.25) {
            clock += 0.25;
            RigPose kicking = OwlMotions.body(PaleMove.OWL_KICK, age, kickDash, 0.0F, null);
            kicking.settle(1.0F);
            OwlPose.Swing[] swung = kickArms.swing(PaleMove.OWL_KICK, 1L, kickDash, age, 0.0F, clock, OwlPose.REST, OwlPose.REST, kicking, 0.0F);
            OwlModel.Look looked = OwlPose.look(kicking, KakuganEyes.DEFAULT, swung[0], swung[1], clock, 1.0);
            if (Math.abs(age - (kickDash + OwlRules.KICK_SKEWER + 0.5)) < 1.0E-9) {
                skewered = looked;
            }
            if (Math.abs(age - (kickDash + OwlRules.KICK_SLASH)) < 1.0E-9) {
                slashed = looked;
            }
            if (age >= kickDash + OwlRules.KICK_SKEWER + 0.5 && age <= kickDash + OwlRules.KICK_PULL - 1.0) {
                // Held through it all until it is drawn out, whatever her body does meanwhile.
                double held = segmentDistance(looked.right().elbow(), looked.right().tip(), new double[] {0.0, 1.0, OwlRules.KICK_STAND});
                check(held < 0.45 && looked.right().tip()[2] > OwlRules.KICK_STAND, "the right blade stays through it: " + held + " off at " + age);
            }
        }
        double[] middle = {0.0, 1.0, OwlRules.KICK_STAND};
        double skewerOff = segmentDistance(skewered.right().elbow(), skewered.right().tip(), middle);
        check(skewered.right().tip()[2] > OwlRules.KICK_STAND && skewerOff < 0.45,
            "the right blade through the middle of what she kicks: tip " + java.util.Arrays.toString(skewered.right().tip()) + ", " + skewerOff + " off its middle");
        double[] neck = {0.0, 1.55, OwlRules.KICK_STAND};
        double slashOff = segmentDistance(slashed.left().elbow(), slashed.left().tip(), neck);
        check(slashOff < 0.5, "the left blade across its neck: " + slashOff + " off it");
        RigPose heel = OwlMotions.body(PaleMove.OWL_KICK, kickDash + OwlRules.KICK_BLOW, kickDash, 0.0F, null);
        heel.settle(1.0F);
        double[] foot = BodyFrame.world(heel, BodyFrame.model(heel, "rightLeg", new double[] {0.0, 12.0, 0.0}));
        double reach = foot[2] + OwlRules.KICK_STEP;
        check(reach > OwlRules.KICK_STAND - 0.6 && reach < OwlRules.KICK_STAND + 0.2 && foot[1] > 0.8 && Math.abs(foot[0]) < 0.4,
            "the heel lands on it, high, stepping in through the spin: " + java.util.Arrays.toString(foot) + " + " + OwlRules.KICK_STEP);
        // Its bone spikes: the same burst from the same seed on every side, all round her, staggered in no set order, never down;
        // each out fast and back smoothly; where the server puts them is where they are drawn; never through her.
        OwlSpikes.Spike[] burst = OwlSpikes.of(424242);
        OwlSpikes.Spike[] again = OwlSpikes.of(424242);
        check(burst.length == OwlSpikes.COUNT && java.util.Arrays.equals(burst[3].dir(), again[3].dir()) && burst[7].erupt() == again[7].erupt(),
            "the same spikes from the same seed, everywhere");
        double[] bearings = new double[burst.length];
        double first = Double.MAX_VALUE;
        double last = 0.0;
        boolean sweep = true;
        for (int i = 0; i < burst.length; i++) {
            double[] d = burst[i].dir();
            check(Math.abs(d[0] * d[0] + d[1] * d[1] + d[2] * d[2] - 1.0) < 1.0E-9 && d[1] >= 0.0, "a spike's way: unit, never down into the ground");
            bearings[i] = Math.atan2(d[0], d[2]);
            first = Math.min(first, burst[i].erupt());
            last = Math.max(last, burst[i].erupt());
            if (i > 0 && burst[i].erupt() < burst[i - 1].erupt()) {
                sweep = false;
            }
        }
        java.util.Arrays.sort(bearings);
        double gap = bearings[0] + Math.PI * 2.0 - bearings[bearings.length - 1];
        for (int i = 1; i < bearings.length; i++) {
            gap = Math.max(gap, bearings[i] - bearings[i - 1]);
        }
        check(gap < Math.toRadians(55.0), "all round her, no wide gap: " + Math.toDegrees(gap) + " deg");
        check(!sweep && last - first > (OwlSpikes.COUNT - 1) * OwlSpikes.STAGGER * 0.8 && first >= OwlRules.SPIKES_HUNCH,
            "staggered one after another in no set order, once she has hunched: " + first + " to " + last);
        OwlSpikes.Spike one = burst[0];
        check(OwlSpikes.out(one, one.erupt(), 0.0F) == 0.0 && OwlSpikes.out(one, one.erupt() + OwlSpikes.GROW * 0.5, 0.0F) > 0.75
            && OwlSpikes.out(one, one.erupt() + OwlSpikes.GROW, 0.0F) >= 0.99 && OwlSpikes.out(one, one.erupt() + 30.0, 0.0F) == 1.0,
            "a spike bursts out fast and stays out");
        double peak = 0.0;
        for (double t = 0.0; t < 40.0; t += 0.05) {
            peak = Math.max(peak, OwlSpikes.out(one, t, 0.0F));
        }
        check(peak <= 1.07, "it overshoots its length only a hair: " + peak);
        double letGo = one.erupt() + 0.5;
        check(OwlSpikes.out(one, one.erupt() + OwlSpikes.GROW, letGo) >= 0.99, "never drawn in before it is all the way out");
        double previousOut = 2.0;
        boolean shrinking = true;
        for (double t = one.erupt() + OwlSpikes.GROW + 1.0; t <= one.erupt() + OwlSpikes.GROW + OwlSpikes.RETRACT; t += 0.25) {
            double now = OwlSpikes.out(one, t, letGo);
            shrinking &= now <= previousOut + 1.0E-9;
            previousOut = now;
        }
        check(shrinking && previousOut < 1.0E-6, "let go, drawn back in smoothly, all of it");
        double worstRoot = 0.0;
        double worstSpike = 0.0;
        String whereSpike = "";
        for (int seed : new int[] {1, 77, 424242, -9001, 31337}) {
            OwlSpikes.Spike[] spikes = OwlSpikes.of(seed);
            double allOut = OwlSpikes.allOut(spikes);
            for (double age : new double[] {OwlSpikes.FIRST + 1.0, OwlSpikes.FIRST + 3.0, allOut, allOut + 6.0}) {
                RigPose hunched = OwlMotions.body(PaleMove.OWL_SPIKES, age, seed, 0.0F, null);
                hunched.settle(1.0F);
                BodyFrame.Solid solid = new BodyFrame.Solid(hunched);
                for (OwlSpikes.Spike spike : spikes) {
                    double[][] placed = OwlPose.spike(hunched, spike, 0.0);
                    if (age >= allOut) {
                        double[] server = OwlSpikes.root(spike);
                        worstRoot = Math.max(worstRoot, Math.sqrt(Math.pow(server[0] - placed[0][0], 2) + Math.pow(server[1] - placed[0][1], 2)
                            + Math.pow(server[2] - placed[0][2], 2)));
                    }
                    double extended = OwlSpikes.out(spike, age, 0.0F);
                    double[] deepest = {0.0};
                    double[] root = placed[0];
                    OwlSpikes.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                        for (double[] v : new double[][] {a, b, c, d}) {
                            double fromRoot = Math.sqrt(Math.pow(v[0] - root[0], 2) + Math.pow(v[1] - root[1], 2) + Math.pow(v[2] - root[2], 2));
                            if (fromRoot > 0.45) {
                                deepest[0] = Math.max(deepest[0], solid.depth(v, 0.0) - CLEARANCE);
                            }
                        }
                    }, spike, placed[0], placed[1], new double[] {0.0, 1.0, 0.0}, extended);
                    if (deepest[0] > worstSpike) {
                        worstSpike = deepest[0];
                        whereSpike = " (seed " + seed + ", tick " + age + ", out of " + java.util.Arrays.toString(spike.px()) + " going "
                            + java.util.Arrays.toString(spike.dir()) + ")";
                    }
                }
            }
            double held = OwlSpikes.allOut(spikes) + 8.0;
            owlSequence("the Owl's spikes, seed " + seed, new Object[][] {{PaleMove.OWL_SPIKES, OwlSpikes.allIn(spikes, held) + 0.5, seed, (float)held}});
        }
        check(worstRoot < 0.3, "the server's spikes grow out of her where they are drawn: " + worstRoot + " blocks off");
        check(worstSpike <= 0.0, "never a spike through her (but where it tears out of her back): " + worstSpike + " px" + whereSpike);
        // A raised blade's point hooks forward (it bows back over the top), never back behind her; hanging or level, it bows up.
        RigPose upright = OwlPose.stance();
        upright.settle(1.0F);
        double worstHook = 1.0;
        String whereHook = "";
        for (int side : new int[] {1, -1}) {
            for (double out = -0.6; out <= 1.4; out += 0.2) {
                for (double forward = -0.8; forward <= 4.2; forward += 0.2) {
                    OwlModel.Arm limb = OwlPose.arm(upright, new OwlPose.Swing(0.6, 1.0, out, forward, 0.3), side);
                    double[] blade = {limb.tip()[0] - limb.elbow()[0], limb.tip()[1] - limb.elbow()[1], limb.tip()[2] - limb.elbow()[2]};
                    double length = Math.sqrt(blade[0] * blade[0] + blade[1] * blade[1] + blade[2] * blade[2]);
                    double rising = blade[1] / length;
                    double[] bend = limb.bend();
                    if (rising > 0.5) {
                        // Bulging back (or at least not forward) of its line: its point curls forward.
                        double ahead = bend[2] - blade[2] / length * (bend[0] * blade[0] + bend[1] * blade[1] + bend[2] * blade[2]) / length;
                        if (-ahead < worstHook) {
                            worstHook = -ahead;
                            whereHook = String.format(java.util.Locale.ROOT, " (side %d, out %.1f, forward %.1f)", side, out, forward);
                        }
                    } else if (rising < -0.2) {
                        check(bend[1] > 0.3, "a hanging blade bows up: side " + side + ", out " + out + ", forward " + forward);
                    }
                }
            }
        }
        check(worstHook >= -0.05, "a raised blade's point hooks forward, never back behind her: " + worstHook + whereHook);
        // Its weight: through the drive the blade is further behind in its swing than its upper arm is in its own (the heavy end
        // trailing the root), each behind where it is driven; and the blade bows the faster it swings.
        OwlPose.Swing pulled = OwlMotions.keyed(PaleMove.OWL_PUNCH, 1, 1.2, 0, 1.0F);
        OwlPose.Swing driven = OwlMotions.keyed(PaleMove.OWL_PUNCH, 1, PaleRules.PUNCH_IMPACT, 0, 1.0F);
        OwlPose.Swing key = OwlMotions.keyed(PaleMove.OWL_PUNCH, 1, 3.4, 0, 1.0F);
        OwlPose.Swing heavy = OwlMotions.weighted(PaleMove.OWL_PUNCH, 1, 3.4, 0, 1.0F);
        double upperWay = (pulled.forward() - heavy.forward()) / (pulled.forward() - driven.forward());
        double bladeWay = (pulled.bladeForward() - heavy.bladeForward()) / (pulled.bladeForward() - driven.bladeForward());
        double keyWay = (pulled.forward() - key.forward()) / (pulled.forward() - driven.forward());
        check(keyWay > upperWay && upperWay > bladeWay, "the blade trails its upper arm through the drive: driven " + keyWay + ", upper " + upperWay
            + ", blade " + bladeWay + " of the way");
        check(heavy.bow() > key.bow() + 0.05, "and bows with its speed: " + heavy.bow() + " over " + key.bow());
        // Aimed where she faces, however her body is turned under it: at the blow the right arm's elbow is out in front of her.
        RigPose blow = OwlMotions.body(PaleMove.OWL_PUNCH, PaleRules.PUNCH_IMPACT + 1.0, 0, 1.0F, null);
        blow.settle(1.0F);
        OwlModel.Look struck = OwlPose.look(blow, KakuganEyes.DEFAULT, OwlMotions.swing(PaleMove.OWL_PUNCH, 1, PaleRules.PUNCH_IMPACT + 1.0, 0, 1.0F, null, blow, 0.0F),
            OwlPose.REST, 0.0, 1.0);
        check(struck.right().elbow()[2] > 1.5 && Math.abs(struck.right().elbow()[0]) < 1.0 && struck.right().elbow()[1] > 0.5,
            "at the blow the giant elbow is driven out in front of her: " + java.util.Arrays.toString(struck.right().elbow()));
        RigPose turned = OwlMotions.body(PaleMove.OWL_PUNCH, PaleRules.PUNCH_IMPACT + 1.0, 0, 1.0F, null);
        turned.spin -= 0.8F;
        turned.settle(1.0F);
        OwlModel.Look aimed = OwlPose.look(turned, KakuganEyes.DEFAULT, OwlMotions.swing(PaleMove.OWL_PUNCH, 1, PaleRules.PUNCH_IMPACT + 1.0, 0, 1.0F, null, turned,
            0.8F), OwlPose.REST, 0.0, 1.0);
        double[] e = aimed.right().elbow();
        double[] s0 = aimed.right().shoulder();
        double yaw = Math.atan2(e[0] - s0[0], e[2] - s0[2]);
        double[] e0 = struck.right().elbow();
        double[] s1 = struck.right().shoulder();
        double yaw0 = Math.atan2(e0[0] - s1[0], e0[2] - s1[2]);
        double offYaw = yaw - yaw0 - 0.8;
        offYaw -= Math.PI * 2.0 * Math.round(offYaw / (Math.PI * 2.0));
        check(Math.abs(offYaw) < 1.0E-3, "thrown the way the move faced, the giant arm turns with it exactly: " + offYaw);
    }

    /**
     * The Owl's moves {@code moves} ({move, ticks it runs, detail, charge}, one after another, each out of the last as drawn) played
     * as the game plays them out of her idle in it and back into it, every quarter tick: her own arms never through her, never a
     * giant arm's blade through her, nothing of it into the ground (a braced point resting on it), the giant arms never jumping.
     */
    static void owlSequence(String name, Object[][] moves) {
        double worstArms = 0.0;
        double worstBlade = 0.0;
        double lowest = Double.MAX_VALUE;
        double jump = 0.0;
        String where = "";
        String whereBlade = "";
        String whereJump = "";
        String whereLow = "";
        double worstLean = Double.MAX_VALUE;
        double worstTilt = Double.MAX_VALUE;
        String whereLean = "";
        String whereTilt = "";
        OwlArms arms = new OwlArms();
        double[][] previous = null;
        // The red muscle does the work: how far each blade's point travels off its shoulder, how much of that is the blade moving on
        // its own (not carried by its upper arm), the most of it in a quarter tick, and the blade's bend at the elbow.
        double[][] lastUpper = new double[2][];
        double[][] lastBlade = new double[2][];
        double[] travel = new double[2];
        double[] own = new double[2];
        double flick = 0.0;
        String whereFlick = "";
        double bentLeast = Double.MAX_VALUE;
        double bentMost = 0.0;
        String whereBent = "";
        double time = 100.0;
        double total = 0.0;
        for (Object[] m : moves) {
            total += (Double)m[1];
        }
        RigPose lastDrawn = null;
        RigPose from = null;
        int current = -1;
        for (double t = -3.0; t <= total + 10.0; t += 0.25) {
            time += 0.25;
            PaleMove move = PaleMove.NONE;
            double age = 0.0;
            int detail = 0;
            float charge = 0.0F;
            int index = -1;
            double begun = 0.0;
            for (int i = 0; i < moves.length && t >= 0.0; i++) {
                double length = (Double)moves[i][1];
                if (t < begun + length) {
                    index = i;
                    move = (PaleMove)moves[i][0];
                    age = t - begun;
                    detail = (Integer)moves[i][2];
                    charge = (Float)moves[i][3];
                    break;
                }
                begun += length;
            }
            if (index != current) {
                from = lastDrawn;
                current = index;
            }
            RigPose body;
            if (move == PaleMove.NONE) {
                body = OwlPose.stance();
                OwlPose.body(body, true, 1000.0, time);
            } else {
                body = OwlMotions.body(move, age, detail, charge, index == 0 ? null : from);
            }
            body.settle(1.0F);
            lastDrawn = PaleMotions.ended(body);
            OwlPose.Swing[] swung = arms.swing(move, index + 1L, detail, age, charge, time, OwlPose.swing(1, time, 0.0, 0.0), OwlPose.swing(-1, time, 0.0, 0.0),
                body, 0.0F);
            String at = String.format(java.util.Locale.ROOT, " (%s, %s tick %.2f)", name, move, age);
            OwlModel.Look look = OwlPose.look(body, KakuganEyes.DEFAULT, swung[0], swung[1], time, 1.0);
            double[][] ends = {look.right().elbow(), look.right().tip(), look.left().elbow(), look.left().tip()};
            if (previous != null) {
                for (int i = 0; i < ends.length; i++) {
                    double d = Math.sqrt(Math.pow(ends[i][0] - previous[i][0], 2) + Math.pow(ends[i][1] - previous[i][1], 2) + Math.pow(ends[i][2] - previous[i][2], 2));
                    if (d > jump) {
                        jump = d;
                        whereJump = at + (i < 2 ? " right " : " left ") + (i % 2 == 0 ? "elbow" : "point");
                    }
                }
            }
            previous = ends;
            OwlModel.Arm[] giant = {look.right(), look.left()};
            for (int i = 0; i < 2; i++) {
                double[] upper = unit(sub3(giant[i].elbow(), giant[i].shoulder()));
                double[] blade = unit(sub3(giant[i].tip(), giant[i].elbow()));
                double bent = Math.acos(Math.max(-1.0, Math.min(1.0, dot3(upper, blade))));
                if (bent < bentLeast || bent > bentMost) {
                    whereBent = at + (i == 0 ? " right" : " left");
                }
                bentLeast = Math.min(bentLeast, bent);
                bentMost = Math.max(bentMost, bent);
                if (lastUpper[i] != null) {
                    double[] carried = carriedBy(lastUpper[i], upper, lastBlade[i]);
                    double moved = OwlModel.FORE * Math.sqrt(dot3(sub3(blade, carried), sub3(blade, carried)));
                    double[] point = sub3(add3(scale3(upper, OwlModel.UPPER), scale3(blade, OwlModel.FORE)),
                        add3(scale3(lastUpper[i], OwlModel.UPPER), scale3(lastBlade[i], OwlModel.FORE)));
                    travel[i] += Math.sqrt(dot3(point, point));
                    own[i] += moved;
                    if (moved > flick) {
                        flick = moved;
                        whereFlick = at + (i == 0 ? " right" : " left");
                    }
                }
                lastUpper[i] = upper;
                lastBlade[i] = blade;
            }
            double clip = BodyFrame.clipping(body, CLEARANCE);
            if (clip > worstArms) {
                worstArms = clip;
                where = at;
            }
            // Never arched back, her head never thrown back: the crest on her back and the mantle off her mask stand up only on a body
            // held upright or hunched over (arched back, they flop out flat behind her and her head sinks out of sight behind her bulk).
            if (body.lean < worstLean) {
                worstLean = body.lean;
                whereLean = at;
            }
            if (body.lean + body.headX < worstTilt) {
                worstTilt = body.lean + body.headX;
                whereTilt = at;
            }
            BodyFrame.Solid solid = new BodyFrame.Solid(body);
            double[] deepest = {0.0};
            OwlModel.reaching((a, b, c, d, n, ca, cb, cc, cd) -> {
                for (double[] v : new double[][] {a, b, c, d}) {
                    deepest[0] = Math.max(deepest[0], Math.max(solid.depth(v, 0.0), chestDepth(look.torso(), v, body.bulk)) - CLEARANCE);
                }
            }, look);
            if (deepest[0] > worstBlade) {
                worstBlade = deepest[0];
                whereBlade = at;
            }
            double[] low = {Double.MAX_VALUE};
            OwlModel.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                for (double[] v : new double[][] {a, b, c, d}) {
                    low[0] = Math.min(low[0], v[1]);
                }
            }, look);
            if (low[0] < lowest) {
                lowest = low[0];
                whereLow = at;
            }
        }
        check(worstArms == 0.0, "her own arms never through her: " + worstArms + " px" + where);
        check(worstLean >= -0.02, "never arched back (her crest stays up off her back): lean " + worstLean + whereLean);
        check(worstTilt >= -0.15, "her head never thrown back out of sight: " + worstTilt + whereTilt);
        check(worstBlade <= 0.0, "never a giant arm's blade through her: " + worstBlade + " px" + whereBlade);
        check(lowest >= -0.08, "nothing of it into the ground (a braced point rests on it): " + lowest + whereLow);
        // (The fastest a blade's point ever whips through a blow is about four blocks a tick; anything more is a jump.)
        check(jump < 1.2, "the giant arms never jump (into it, through it, back into her idle): " + jump + " blocks in a quarter tick" + whereJump);
        // The red muscle does the work, the blade rides on it: never bent far off it or folded straight, most of the way its point
        // goes carried there by the upper arm, never flicking round on its own.
        check(bentLeast >= OwlMotions.ELBOW_LEAST - 1.0E-6 && bentMost <= OwlMotions.ELBOW_MOST + 1.0E-6,
            "each blade bent off its upper arm only so far: " + Math.toDegrees(bentLeast) + " to " + Math.toDegrees(bentMost) + " degrees (" + name + ")" + whereBent);
        for (int i = 0; i < 2; i++) {
            check(own[i] <= 0.35 * travel[i], "the " + (i == 0 ? "right" : "left") + " blade carried by its upper arm, not moving on its own: "
                + own[i] + " of its " + travel[i] + " blocks its own (" + name + ")");
        }
        check(flick < 0.5, "a blade never flicks round its upper arm: " + flick + " blocks in a quarter tick" + whereFlick);
    }

    /** {@code v} turned by the least turn taking {@code from} to {@code to}: where a blade would point had its arm only carried it. */
    static double[] carriedBy(double[] from, double[] to, double[] v) {
        double[] axis = {from[1] * to[2] - from[2] * to[1], from[2] * to[0] - from[0] * to[2], from[0] * to[1] - from[1] * to[0]};
        double sin = Math.sqrt(dot3(axis, axis));
        double cos = dot3(from, to);
        if (sin < 1.0E-9) {
            return v;
        }
        double[] k = scale3(axis, 1.0 / sin);
        double[] kv = {k[1] * v[2] - k[2] * v[1], k[2] * v[0] - k[0] * v[2], k[0] * v[1] - k[1] * v[0]};
        return add3(add3(scale3(v, cos), scale3(kv, sin)), scale3(k, dot3(k, v) * (1.0 - cos)));
    }

    static double[] sub3(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] add3(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] scale3(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double dot3(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] unit(double[] a) {
        return scale3(a, 1.0 / Math.max(1.0E-12, Math.sqrt(dot3(a, a))));
    }

    /** How far {@code p} is from the segment {@code a}-{@code b}. */
    static double segmentDistance(double[] a, double[] b, double[] p) {
        double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double[] ap = {p[0] - a[0], p[1] - a[1], p[2] - a[2]};
        double t = Math.max(0.0, Math.min(1.0, (ab[0] * ap[0] + ab[1] * ap[1] + ab[2] * ap[2]) / Math.max(1.0E-12, ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2])));
        double[] q = {a[0] + ab[0] * t - p[0], a[1] + ab[1] * t - p[1], a[2] + ab[2] * t - p[2]};
        return Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2]);
    }

    /** An angle's size, a whole turn round counting as none. */
    static double turned(double angle) {
        return Math.abs(angle - Math.PI * 2.0 * Math.round(angle / (Math.PI * 2.0)));
    }

    static double ownHead(OwlModel.Look look, BodyFrame.Solid solid, double bulk) {
        double deepest = 0.0;
        for (double x = -4.0; x <= 4.0; x += 1.0) {
            for (double y = -8.0; y <= 0.0; y += 1.0) {
                for (double z = -4.0; z <= 4.0; z += 1.0) {
                    if (Math.abs(x) < 4.0 && y > -8.0 && y < 0.0 && Math.abs(z) < 4.0) {
                        continue; // its surface
                    }
                    double[] v = look.head().at(x, y, z);
                    deepest = Math.max(deepest, Math.max(Math.max(solid.part(v, 0.0, 0), chestDepth(look.torso(), v, bulk)),
                        Math.max(solid.part(v, 0.0, 2), solid.part(v, 0.0, 3))));
                }
            }
        }
        return deepest;
    }

    /** How deep (pixels) a point is inside her chest bulked up by {@code bulk} (OwlBulk's box over the top of her torso). */
    /**
     * 6.2.4: how deep (pixels) the Owl's robe goes into either of her legs (each as a round of its box's half-width about the line
     * from its hip to its foot).
     */
    static double robeInLegs(RigPose pose, OwlModel.Look look) {
        double[][][] legs = new double[2][][];
        int i = 0;
        for (String leg : new String[] {"rightLeg", "leftLeg"}) {
            legs[i++] = new double[][] {BodyFrame.world(pose, BodyFrame.model(pose, leg, new double[] {0.0, 0.0, 0.0})),
                BodyFrame.world(pose, BodyFrame.model(pose, leg, new double[] {0.0, 12.0, 0.0}))};
        }
        double px = BodyFrame.SCALE / 16.0;
        double[] deepest = {0.0};
        OwlModel.robe((a, b, c, d, n, ca, cb, cc, cd) -> {
            for (double[] v : new double[][] {a, b, c, d}) {
                for (double[][] leg : legs) {
                    deepest[0] = Math.max(deepest[0], 2.0 - segmentDistance(leg[0], leg[1], v) / px);
                }
            }
        }, look);
        return deepest[0];
    }

    static double chestDepth(OwlModel.Frame torso, double[] v, double bulk) {
        double[] d = {v[0] - torso.o()[0], v[1] - torso.o()[1], v[2] - torso.o()[2]};
        double[] q = new double[3];
        double[][] axes = {torso.x(), torso.y(), torso.z()};
        for (int k = 0; k < 3; k++) {
            double[] a = axes[k];
            q[k] = (d[0] * a[0] + d[1] * a[1] + d[2] * a[2]) / (a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        }
        double b = Math.max(0.0, Math.min(1.0, bulk));
        double halfX = 4.0 * (1.0 + RigPose.BULK_CHEST * b);
        double halfZ = 2.0 * (1.0 + RigPose.BULK_CHEST_DEPTH * b);
        return Math.min(Math.min(halfX - Math.abs(q[0]), halfZ - Math.abs(q[2])), Math.min(q[1], OwlBulk.HEIGHT - q[1]));
    }

    /** How deep (pixels) the Mutated Arm goes into her torso, head or left arm (her right arm is not drawn: the arm is in its place). */
    static double armDepth(RigPose body, PaleFleshModel.Pose arm) {
        if (arm == null) {
            return 0.0;
        }
        BodyFrame.Solid solid = new BodyFrame.Solid(body, false);
        double[] deepest = {0.0};
        PaleFleshModel.mesh((a, b, cc, d, n, ca, cb, ccc, cd) -> {
            for (double[] v : new double[][] {a, b, cc, d}) {
                double depth = solid.depth(v, 0.0);
                if (depth > CLEARANCE) {
                    deepest[0] = Math.max(deepest[0], depth);
                }
            }
        }, arm);
        return deepest[0];
    }

    /** How deep (pixels) the tentacle on {@code side} at {@code tip} goes into {@code body}'s torso, head or arms. */
    static double depth(RigPose body, int side, WatchPose.Tip tip, double time) {
        return clubDepth(body, WatchPose.pose(body, side, tip, time, 5));
    }

    /** The Speed Kick's pose {@code since} ticks after she is there (6.2.0: the lunge, then the dash), the dash {@code dash} ticks, settled. */
    static RigPose kickAt(int dash, double since) {
        RigPose pose = PaleMotions.kick(PaleRules.kickArrive(dash) + since, dash, null);
        pose.settle(1.0F);
        return pose;
    }

    /** A point down her right leg ({@code down} pixels from the hip), in rig space. */
    static double[] legPoint(RigPose pose, double down) {
        return BodyFrame.world(pose, BodyFrame.model(pose, "rightLeg", new double[] {0.0, down, 0.0}));
    }

    static void hammer() {
        // Her arms stay out of her body all through the hammer: the raise from anything she may be doing, the slam out of any hold.
        double worst = 0.0;
        String where = "";
        List<RigPose> froms = starts(PaleStrike.JAB);
        for (int s = 0; s < froms.size(); s += 4) {
            for (double age = 0.0; age <= PaleRules.HAMMER_MAX_HOLD; age += age < 12.0 ? 0.1 : 2.0) {
                RigPose pose = PaleMotions.hammerCharge(age, froms.get(s));
                pose.settle(1.0F);
                double clip = BodyFrame.clipping(pose, CLEARANCE);
                if (clip > worst) {
                    worst = clip;
                    where = " (raising, from start " + s + ", tick " + String.format(java.util.Locale.ROOT, "%.1f", age) + ")";
                }
            }
        }
        // (Let go before the leg is up, she raises it first: the slam always starts from it raised.)
        for (double held : new double[] {PaleRules.HAMMER_RAISE_TICKS, 12.0, 20.0, 30.0, 55.0, PaleRules.HAMMER_MAX_HOLD}) {
            RigPose charged = PaleMotions.hammerCharge(held, null);
            float charge = PaleRules.hammerCharge(held);
            for (double age = 0.0; age <= PaleRules.HAMMER_TICKS + 0.001; age += 0.1) {
                RigPose pose = PaleMotions.hammer(age, charge, charged);
                pose.settle(1.0F);
                double clip = BodyFrame.clipping(pose, CLEARANCE);
                if (clip > worst) {
                    worst = clip;
                    where = " (the slam after " + held + " ticks held, tick " + String.format(java.util.Locale.ROOT, "%.1f", age) + ")";
                }
            }
        }
        check(worst == 0.0, "her arms stay out of her body all through the Flesh Hammer: " + worst + " px" + where);
        // She stands on her left foot however her body tips: its sole stays on the ground.
        double floating = 0.0;
        for (double age = 0.0; age <= 40.0; age += 1.0) {
            floating = Math.max(floating, Math.abs(lowestLeftSole(PaleMotions.hammerCharge(age, null))));
        }
        for (double age = 0.0; age <= PaleRules.HAMMER_TICKS; age += 1.0) {
            floating = Math.max(floating, Math.abs(lowestLeftSole(PaleMotions.hammer(age, 1.0F, null))));
        }
        check(floating < 0.01, "her standing foot stays on the ground: " + floating);
        // The club never goes through her, raised or slammed, at any charge.
        worst = 0.0;
        where = "";
        for (float charge : new float[] {0.1F, 0.5F, 1.0F}) {
            for (double age = 0.0; age <= 40.0; age += 1.0) {
                RigPose body = PaleMotions.hammerCharge(age, null);
                body.settle(1.0F);
                double depth = clubDepth(body, PaleFleshPose.club(body, PaleMove.HAMMER_CHARGE, age, Math.min(charge, PaleRules.hammerCharge(age)), age, 11));
                if (depth > worst) {
                    worst = depth;
                    where = " (raised, charge " + charge + ", tick " + age + ")";
                }
            }
            for (double age = 0.0; age <= PaleRules.HAMMER_TICKS; age += 0.5) {
                RigPose body = PaleMotions.hammer(age, charge, null);
                body.settle(1.0F);
                double depth = clubDepth(body, PaleFleshPose.club(body, PaleMove.HAMMER, age, charge, age, 11));
                if (depth > worst) {
                    worst = depth;
                    where = " (slammed, charge " + charge + ", tick " + age + ")";
                }
            }
        }
        check(worst == 0.0, "the club never goes through her: " + worst + " px" + where);
        // At the blow the club's head comes down on the ground (into the crater it knocks out: never floating above it, never
        // buried deep) right where the server has the blow land.
        for (float charge = 0.1F; charge <= 1.0001F; charge += 0.15F) {
            RigPose body = PaleMotions.hammer(PaleRules.HAMMER_IMPACT, charge, null);
            body.settle(1.0F);
            double[] low = {Double.MAX_VALUE};
            PaleFleshModel.mesh((a, b, cc, d, n, ca, cb, ccc, cd) -> {
                for (double[] v : new double[][] {a, b, cc, d}) {
                    low[0] = Math.min(low[0], v[1]);
                }
            }, PaleFleshPose.club(body, PaleMove.HAMMER, PaleRules.HAMMER_IMPACT, charge, 0.0, 11));
            check(low[0] > -1.0 && low[0] < 0.05, "at charge " + charge + " the club comes down on the ground: its lowest point at " + low[0]);
            double[] head = PaleMotions.clubHeadAt(body, charge);
            check(Math.abs(Math.hypot(head[0], head[2]) - PaleRules.hammerReach(charge)) < 0.15 && Math.abs(head[0]) < 0.3,
                "at charge " + charge + " it lands where the blow is: " + java.util.Arrays.toString(head) + " vs " + PaleRules.hammerReach(charge));
            check(head[1] > 0.0 && head[1] < PaleRules.hammerGirth(charge), "its head's middle just off the ground: " + head[1]);
        }
        check(PaleFleshPose.club(new RigPose(), PaleMove.HAMMER, PaleRules.HAMMER_RETRACT_AT + PaleRules.HAMMER_RETRACT, 1.0F, 0.0, 1) == null,
            "the club is back in her leg before the hammer is over");
        // When her body isn't drawn this frame (first person), the club goes on the body as the move alone poses it: held, the leg
        // is up and the club grows on up into the sky, bigger as it charges; let go, it comes down on the ground ahead of her (the
        // way she faced when it began), never under or behind her.
        double grownBefore = 0.0;
        for (double age = 0.0; age <= PaleRules.HAMMER_MAX_HOLD; age += 1.0) {
            float charge = PaleRules.hammerCharge(age);
            RigPose body = PaleMotions.fleshBody(PaleMove.HAMMER_CHARGE, age, 0.0F, 0.7F);
            check(body != null && body.spin == 0.0F, "held, she faces where she looks (tick " + age + ")");
            PaleFleshModel.Pose club = PaleFleshPose.club(body, PaleMove.HAMMER_CHARGE, age, charge, age, 11);
            if (age >= PaleRules.HAMMER_RAISE_TICKS) {
                check(body.rightLegX < -2.0F, "held, her right leg is up (tick " + age + "): " + body.rightLegX);
                check(club != null && club.grown() >= grownBefore, "held, the club is out and only grows (tick " + age + ")");
                double[] tip = club == null ? new double[3] : club.path()[club.path().length - 1];
                check(tip[1] > 2.4 && tip[2] > -1.0, "held, the club stands up into the sky over her (tick " + age + "): " + java.util.Arrays.toString(tip));
                grownBefore = club == null ? 0.0 : club.grown();
            }
        }
        for (float turn : new float[] {0.0F, 0.7F, -1.2F}) {
            for (float charge : new float[] {0.34F, 1.0F}) {
                RigPose body = PaleMotions.fleshBody(PaleMove.HAMMER, PaleRules.HAMMER_IMPACT + 2.0, charge, turn);
                PaleFleshModel.Pose club = PaleFleshPose.club(body, PaleMove.HAMMER, PaleRules.HAMMER_IMPACT + 2.0, charge, 0.0, 11);
                double[] tip = club == null ? new double[] {0.0, 9.0, -9.0} : club.path()[club.path().length - 1];
                double[] ahead = PaleFleshPose.turned(new double[] {0.0, 0.0, PaleRules.hammerReach(charge)}, turn);
                check(club != null && club.grown() > 0.99, "let go, the club is all out on the blow (turn " + turn + ", charge " + charge + ")");
                check(tip[1] < 0.8 && tip[0] * ahead[0] + tip[2] * ahead[2] > 0.6 * PaleRules.hammerReach(charge) * PaleRules.hammerReach(charge),
                    "let go, the club comes down on the ground ahead of her (turn " + turn + ", charge " + charge + "): " + java.util.Arrays.toString(tip)
                        + " vs " + java.util.Arrays.toString(ahead));
            }
        }
        check(PaleMotions.fleshBody(PaleMove.KICK, 3.0, 0.0F, 0.0F) == null && PaleMotions.fleshBody(PaleMove.SWIPE, 3.0, 0.0F, 0.4F) != null,
            "the flesh's moves alone have a body for the flesh");
        // In first person she sees her own leg (PaleOwnLeg) and the club round it from where her head has gone: standing, from her eyes;
        // raised, the leg and the club out in front of the camera, never on it.
        double[] standing = com.hex.kagune.control.client.animation.PaleOwnLeg.shift(new RigPose());
        check(Math.abs(standing[0]) + Math.abs(standing[1]) + Math.abs(standing[2]) < 1.0E-9, "standing, she sees from her own eyes");
        double[] eye = {0.0, 1.62, 0.0};
        int[] faces = {0, 0};
        boolean[] mapped = {true};
        double[] nearest = {Double.MAX_VALUE, Double.MAX_VALUE};
        for (double age = PaleRules.HAMMER_RAISE_TICKS; age <= PaleRules.HAMMER_MAX_HOLD; age += 5.0) {
            RigPose body = PaleMotions.fleshBody(PaleMove.HAMMER_CHARGE, age, 0.0F, 0.0F);
            double[] shift = com.hex.kagune.control.client.animation.PaleOwnLeg.shift(body);
            faces[0] = 0;
            faces[1] = 0;
            com.hex.kagune.control.client.animation.PaleOwnLeg.mesh(body, shift, (a, b, c, d, n, uv, trousers) -> {
                faces[trousers ? 1 : 0]++;
                for (int i = 0; i < 8; i += 2) {
                    double u = uv[i] * 64.0;
                    double v = uv[i + 1] * 64.0 - (trousers ? 32.0 : 16.0);
                    mapped[0] &= u >= -1.0E-6 && u <= 16.0 + 1.0E-6 && v >= -1.0E-6 && v <= 16.0 + 1.0E-6;
                }
                for (double[] p : new double[][] {a, b, c, d}) {
                    nearest[0] = Math.min(nearest[0], distance(p, eye));
                }
            });
            PaleFleshModel.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                for (double[] p : new double[][] {a, b, c, d}) {
                    nearest[1] = Math.min(nearest[1], distance(new double[] {p[0] + shift[0], p[1] + shift[1], p[2] + shift[2]}, eye));
                }
            }, PaleFleshPose.club(body, PaleMove.HAMMER_CHARGE, age, PaleRules.hammerCharge(age), age, 11));
        }
        check(faces[0] == 18 && faces[1] == 18, "her leg and its trouser layer: four sides in four rows, and both ends: " + faces[0] + ", " + faces[1]);
        check(mapped[0], "her leg is mapped onto the skin's leg and trouser squares");
        check(nearest[0] > 0.3, "raised, her own leg is out in front of the camera, never on it: " + nearest[0]);
        check(nearest[1] > 0.4, "raised, the club round it is out in front of the camera, never on it: " + nearest[1]);
        check(PaleRules.HAMMER_RETRACT_AT + PaleRules.HAMMER_RETRACT <= PaleRules.HAMMER_TICKS && PaleRules.hammerReach(1.0F) > PaleRules.hammerReach(0.1F),
            "a bigger club lands further out");
    }

    static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    /** 6.2.0: the lowest point of her whole body (blocks; negative: below her feet' level), every box's corners. */
    static double lowestPoint(RigPose p) {
        double lowest = Double.MAX_VALUE;
        for (String part : new String[] {"head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"}) {
            double half = part.equals("body") ? 4.0 : part.equals("head") ? 4.0 : 2.0;
            double[] ys = part.equals("head") ? new double[] {0.0, -8.0} : new double[] {0.0, 12.0};
            for (double y : ys) {
                for (double x : new double[] {-half, half}) {
                    for (double z : new double[] {part.equals("head") ? -4.0 : -2.0, part.equals("head") ? 4.0 : 2.0}) {
                        lowest = Math.min(lowest, BodyFrame.world(p, BodyFrame.model(p, part, new double[] {x, y, z}))[1]);
                    }
                }
            }
        }
        return lowest;
    }

    /** How far (pixels) below the ground the lowest corner of her left sole is (negative: above it). */
    static double lowestLeftSole(RigPose p) {
        double lowest = Double.MAX_VALUE;
        for (double x : new double[] {-2.0, 2.0}) {
            for (double z : new double[] {-2.0, 2.0}) {
                lowest = Math.min(lowest, BodyFrame.world(p, BodyFrame.model(p, "leftLeg", new double[] {x, 12.0, z}))[1]);
            }
        }
        return lowest;
    }

    /** How deep (pixels) the club goes into her torso, head or arms; 0 when it is clear. */
    static double clubDepth(RigPose body, PaleFleshModel.Pose club) {
        if (club == null) {
            return 0.0;
        }
        BodyFrame.Solid solid = new BodyFrame.Solid(body);
        double[] deepest = {0.0};
        PaleFleshModel.mesh((a, b, cc, d, n, ca, cb, ccc, cd) -> {
            for (double[] v : new double[][] {a, b, cc, d}) {
                double depth = solid.depth(v, 0.0);
                if (depth > CLEARANCE) {
                    deepest[0] = Math.max(deepest[0], depth);
                }
            }
        }, club);
        return deepest[0];
    }

    static void keys() {
        // J and V: a tap, or a hold from the sixth tick down, reminded every few ticks, let go once.
        PaleKey tap = new PaleKey(false);
        check(tap.tick(true, true) == PaleKey.NONE && tap.tick(false, true) == PaleKey.NONE && tap.tick(false, false) == PaleKey.TAP,
            "a short press is a tap, told when it comes up");
        check(tap.tick(true, false) == PaleKey.TAP, "a press and release within one tick is a tap");
        PaleKey hold = new PaleKey(false);
        int held = 0;
        int kept = 0;
        hold.tick(true, true);
        for (int t = 1; t <= 30; t++) {
            int what = hold.tick(false, true);
            if (what == PaleKey.HOLD) {
                held = t;
            }
            kept += what == PaleKey.KEEP ? 1 : 0;
        }
        check(held == PaleRules.HOLD_TICKS, "a hold starts on its sixth tick: " + held);
        check(kept >= 5 && kept <= 7, "a hold is reminded every few ticks: " + kept);
        check(hold.tick(false, false) == PaleKey.LET_GO && hold.tick(false, false) == PaleKey.NONE, "a hold is let go once");
        // Z: held at once; even the quickest tap is a hold and a let-go.
        PaleKey punch = new PaleKey(true);
        check(punch.tick(true, false) == PaleKey.HOLD && punch.tick(false, false) == PaleKey.LET_GO, "the quickest Z is a punch at the least charge");
        check(punch.tick(true, true) == PaleKey.HOLD, "Z charges the moment it goes down");
        int reminders = 0;
        for (int t = 0; t < 40; t++) {
            reminders += punch.tick(false, true) == PaleKey.KEEP ? 1 : 0;
        }
        check(reminders >= 9 && 40 / Math.max(1, reminders) < PaleRules.HOLD_TIMEOUT, "a charge is reminded well inside the server's timeout: " + reminders);
    }

    static void stun() {
        check(StunRules.TICKS >= 10, "a strike stuns for at least half a second: " + StunRules.TICKS);
        check(StunRules.HEAVY_TICKS >= StunRules.TICKS && StunRules.CAP >= StunRules.HEAVY_TICKS, "heavy strikes stun longer, the cap above both");
        long until = StunRules.renewed(Long.MIN_VALUE, 100, StunRules.TICKS);
        check(until == 100 + StunRules.TICKS, "a fresh stun runs its full length: " + until);
        check(StunRules.holds(until, 100) && StunRules.holds(until, 100 + StunRules.TICKS - 1) && !StunRules.holds(until, 100 + StunRules.TICKS),
            "it holds for exactly its length");
        long renewed = StunRules.renewed(until, 105, StunRules.TICKS);
        check(renewed == 105 + StunRules.TICKS, "every hit renews it from that hit: " + renewed);
        long heavy = StunRules.renewed(Long.MIN_VALUE, 200, StunRules.HEAVY_TICKS);
        check(StunRules.renewed(heavy, 201, StunRules.TICKS) == heavy, "a light hit never cuts a heavy stun short");
        long piled = Long.MIN_VALUE;
        for (int t = 0; t < 200; t++) {
            piled = StunRules.renewed(piled, 300 + t / 20, StunRules.HEAVY_TICKS * 4);
        }
        check(piled <= 300 + 200 / 20 + StunRules.CAP, "a pile of hits never stuns past the cap: " + piled);
        // A combo: a strike every 6 to 9 ticks keeps it stunned all through.
        until = Long.MIN_VALUE;
        long now = 0;
        boolean gap = false;
        for (int[] strike : new int[][] {{0, 0}, {7, 0}, {14, 0}, {22, 1}, {30, 0}, {37, 1}}) {
            for (long t = now; t < strike[0]; t++) {
                gap |= t > 0 && !StunRules.holds(until, t);
            }
            now = strike[0];
            until = StunRules.renewed(until, now, strike[1] == 1 ? StunRules.HEAVY_TICKS : StunRules.TICKS);
        }
        check(!gap, "a combo keeps it helpless from the first hit to the last");
    }

    /** 6.2.0: her own two arms in first person through the Speed Blitz (PaleOwnArm.both). */
    static void ownArms() {
        for (PaleStrike strike : PaleStrike.values()) {
            for (double age : new double[] {1.0, strike.impact, strike.chain}) {
                // The left arm is the right one mirrored across her: on the mirrored body it covers the same ground, x flipped.
                RigPose body = PaleMotions.strike(strike, age, null);
                RigPose mirrored = PaleMotions.mirror(body);
                body.settle(1.0F);
                mirrored.settle(1.0F);
                double[] right = bounds(sink -> com.hex.kagune.control.client.animation.PaleOwnArm.mesh(body, new double[3], false, sink), false);
                double[] left = bounds(sink -> com.hex.kagune.control.client.animation.PaleOwnArm.left(mirrored, new double[3], false, sink), false);
                boolean same = Math.abs(right[0] + left[3]) < 1.0E-6 && Math.abs(right[3] + left[0]) < 1.0E-6;
                for (int k = 1; k < 3; k++) {
                    same &= Math.abs(right[k] - left[k]) < 1.0E-6 && Math.abs(right[k + 3] - left[k + 3]) < 1.0E-6;
                }
                check(same, "her left arm is her right mirrored: " + strike + " tick " + age);
            }
            // She sees her blow land: at impact the fist that throws it is in front of her eyes, in view.
            RigPose body = PaleMotions.strike(strike, strike.impact, null);
            body.settle(1.0F);
            double[] shift = com.hex.kagune.control.client.animation.PaleOwnLeg.shift(body);
            double[] eye = BodyFrame.world(new RigPose(), BodyFrame.model(new RigPose(), "head", new double[] {0.0, -4.0, 0.0}));
            double[] ahead = {0.0, 0.0, -1.0E9};
            java.util.function.Consumer<com.hex.kagune.control.client.animation.PaleOwnLeg.Sink> arm = strike.right
                ? sink -> com.hex.kagune.control.client.animation.PaleOwnArm.mesh(body, shift, false, sink)
                : sink -> com.hex.kagune.control.client.animation.PaleOwnArm.left(body, shift, false, sink);
            arm.accept((a, b, c, d, n, uv, sleeve) -> {
                for (double[] q : new double[][] {a, b, c, d}) {
                    if (q[2] > ahead[2]) {
                        System.arraycopy(q, 0, ahead, 0, 3);
                    }
                }
            });
            double dx = ahead[0] - eye[0];
            double dy = ahead[1] - eye[1];
            double dz = ahead[2] - eye[2];
            double off = Math.toDegrees(Math.acos(dz / Math.sqrt(dx * dx + dy * dy + dz * dz)));
            check(dz > 0.4 && off < 60.0, "in first person she sees the " + strike + " land: its fist " + dz + " ahead, " + off + " deg off");
        }
    }

    /** The box round every corner a mesh gives ({min x, y, z, max x, y, z}); {@code sleeves} counts the overlay too. */
    static double[] bounds(java.util.function.Consumer<com.hex.kagune.control.client.animation.PaleOwnLeg.Sink> mesh, boolean sleeves) {
        double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        mesh.accept((a, b, c, d, n, uv, sleeve) -> {
            if (sleeve && !sleeves) {
                return;
            }
            for (double[] q : new double[][] {a, b, c, d}) {
                for (int k = 0; k < 3; k++) {
                    box[k] = Math.min(box[k], q[k]);
                    box[k + 3] = Math.max(box[k + 3], q[k]);
                }
            }
        });
        return box;
    }

    static void flips() {
        for (PaleRules.Flip flip : PaleRules.Flip.values()) {
            double worst = 0.0;
            for (double age = 0.0; age <= PaleRules.FLIP_SPIN_TICKS + 4.0; age += 0.05) {
                for (double landed : new double[] {-1.0, 0.0, 1.0, 2.5, 4.0, 6.0}) {
                    RigPose pose = PaleMotions.flip(flip, age, landed);
                    pose.settle(1.0F);
                    worst = Math.max(worst, BodyFrame.clipping(pose, CLEARANCE));
                }
            }
            check(worst == 0.0, "her arms stay out of her body all through the " + flip + " flip: " + worst + " px");
            RigPose done = PaleMotions.flip(flip, PaleRules.FLIP_SPIN_TICKS + 1.0, -1.0);
            check(Math.abs(Math.abs(done.flip + done.roll) - Math.PI * 2.0) < 1.0E-3 || Math.abs(done.flip + done.roll) < 1.0E-3,
                "a flip turns her over exactly once: " + flip);
            // Once it is over she blends back into her stance from it: from no turn at all, never spinning back round.
            for (double landed : new double[] {0.0, 2.0, 6.0}) {
                RigPose end = PaleMotions.ended(PaleMotions.flip(flip, PaleRules.FLIP_SPIN_TICKS + 4.0, landed));
                check(Math.abs(end.flip) < 0.05 && Math.abs(end.roll) < 0.05 && Math.abs(end.spin) < 0.05,
                    "a landed " + flip + " flip is blended out of from no turn: " + end.flip + ", " + end.roll + ", " + end.spin);
            }
        }
    }

    /** A move as the animator draws it ({@code PaleAnimator}): {@code age} ticks in, started out of {@code from}. */
    static RigPose drawnMove(PaleMove move, int detail, float charge, double age, RigPose from) {
        RigPose pose;
        switch (move) {
            case STRIKE -> pose = PaleMotions.strike(PaleStrike.byOrdinal(detail), age, from);
            case PUNCH_CHARGE -> pose = PaleMotions.punchCharge(age, from);
            case PUNCH -> pose = PaleMotions.punch(age, from);
            case KICK -> pose = PaleMotions.kick(age, detail, from);
            case HAMMER_CHARGE -> pose = PaleMotions.hammerCharge(age, from);
            case HAMMER -> pose = PaleMotions.hammer(age, charge, from);
            case WATCH -> pose = PaleMotions.watch(age, from);
            case FLIP -> {
                PaleRules.Flip flip = PaleRules.Flip.byOrdinal(detail);
                pose = charge > 0.5F ? PaleMotions.soar(flip, age, -1.0) : PaleMotions.flip(flip, age, -1.0);
                if (from != null && age < 1.5) {
                    double u = age / 1.5;
                    float k = (float)(u * u * (3.0 - 2.0 * u));
                    RigPose blended = from.scale(1.0F - k);
                    blended.add(pose, k);
                    pose = blended;
                }
            }
            case OWL_PUNCH_CHARGE, OWL_PUNCH -> pose = OwlMotions.body(move, age, detail, charge, from);
            default -> pose = PaleMotions.flesh(move, age, from);
        }
        return pose;
    }

    /** What a move that only carries another on starts from, as in the game (the hold out of the reach, the throw out of the hold). */
    static RigPose carriedOn(PaleMove move) {
        RigPose reached = PaleMotions.flesh(PaleMove.GRAB_REACH, 4.0, null);
        return switch (move) {
            case GRAB_HOLD -> reached;
            case GRAB_THROW -> PaleMotions.flesh(PaleMove.GRAB_HOLD, 10.0, reached);
            default -> null;
        };
    }

    /** {move, detail, charge, how long it runs}: every move she makes, as a start for cuts and as the move cutting in. */
    static final Object[][] EVERY_MOVE = {
        {PaleMove.STRIKE, PaleStrike.JAB.ordinal(), 0.0F, (double)PaleStrike.JAB.duration},
        {PaleMove.STRIKE, PaleStrike.HOOK.ordinal(), 0.0F, (double)PaleStrike.HOOK.duration},
        {PaleMove.STRIKE, PaleStrike.UPPERCUT.ordinal(), 0.0F, (double)PaleStrike.UPPERCUT.duration},
        {PaleMove.STRIKE, PaleStrike.BACKFIST.ordinal(), 0.0F, (double)PaleStrike.BACKFIST.duration},
        {PaleMove.STRIKE, PaleStrike.TWIN_PALM.ordinal(), 0.0F, (double)PaleStrike.TWIN_PALM.duration},
        {PaleMove.PUNCH_CHARGE, 0, 0.0F, (double)PaleRules.PUNCH_CHARGE_TICKS},
        {PaleMove.PUNCH, 0, 1.0F, (double)PaleRules.PUNCH_TICKS},
        {PaleMove.KICK, 4, 1.0F, (double)(PaleRules.kickArrive(4) + PaleRules.KICK_TICKS)},
        {PaleMove.HAMMER_CHARGE, 0, 0.0F, (double)PaleRules.HAMMER_GROW_TICKS},
        {PaleMove.HAMMER, 0, 1.0F, (double)PaleRules.HAMMER_TICKS},
        {PaleMove.WATCH, 0, 0.0F, (double)WatchRules.POSE_TICKS},
        {PaleMove.SWIPE, 0, 0.0F, (double)PaleRules.SWIPE_TICKS},
        {PaleMove.GRAB_REACH, 0, 4.0F, 4.0},
        {PaleMove.GRAB_HOLD, 0, 0.0F, 10.0},
        {PaleMove.GRAB_THROW, 0, 0.0F, (double)(PaleRules.grabRelease() + PaleRules.GRAB_RETRACT)},
        {PaleMove.GRAB_MISS, 0, 0.0F, (double)PaleRules.GRAB_MISS_TICKS},
        {PaleMove.FLIP, PaleRules.Flip.FORWARD.ordinal(), 0.0F, PaleRules.FLIP_SPIN_TICKS + 2.0},
        {PaleMove.FLIP, PaleRules.Flip.LEFT.ordinal(), 0.0F, PaleRules.FLIP_SPIN_TICKS + 2.0},
        {PaleMove.FLIP, PaleRules.Flip.BACK.ordinal(), 1.0F, PaleRules.SOAR_RAMP + PaleRules.SOAR_SPIN_TICKS + 2.0},
    };

    /**
     * 6.2.0, cancelling: every move cut off at many points by every other move. The new one starts exactly where the old one was drawn
     * (no jump), a turn cut off mid-way is concluded on round the way it went (a flip spun on, never spun back), and through the cut
     * her arms never pass through her body; an arm move thrown in the air out of a flip keeps the flip in her legs.
     */
    static void cuts() {
        double worst = 0.0;
        String where = "";
        for (Object[] a : EVERY_MOVE) {
            PaleMove was = (PaleMove)a[0];
            double length = (Double)a[3];
            for (double at = 1.0; at < length; at += Math.max(1.5, length / 6.0)) {
                RigPose drawn = drawnMove(was, (Integer)a[1], (Float)a[2], at, carriedOn(was));
                drawn.settle(1.0F);
                RigPose before = drawnMove(was, (Integer)a[1], (Float)a[2], at - 0.05, carriedOn(was));
                before.settle(1.0F);
                for (Object[] b : EVERY_MOVE) {
                    PaleMove now = (PaleMove)b[0];
                    if (now == PaleMove.FLIP || now == PaleMove.PUNCH || now == PaleMove.HAMMER || now == PaleMove.GRAB_HOLD || now == PaleMove.GRAB_THROW
                        || was == PaleMove.STRIKE && now == PaleMove.STRIKE) {
                        continue; // a flip only starts on the ground (its own blend is swept with the flips); the rest only carry on a move
                    }
                    PaleMotions.Cut cut = PaleMotions.cut(drawn, before);
                    for (double t = 0.0; t <= 5.0; t += 0.5) {
                        RigPose pose = drawnMove(now, (Integer)b[1], (Float)b[2], t, cut.from());
                        double[] left = cut.left(t);
                        pose.flip += (float)left[0];
                        pose.roll += (float)left[1];
                        pose.spin += (float)left[2];
                        cut.unwind(pose, t);
                        cut.clearOf(pose, t);
                        pose.settle(1.0F);
                        double clip = BodyFrame.clipping(pose, CLEARANCE);
                        if (clip > worst) {
                            worst = clip;
                            where = " (" + was + " " + a[1] + " at " + at + " cut by " + now + " " + b[1] + ", tick " + t + ")";
                        }
                        if (t == 0.0) {
                            for (float[] pair : new float[][] {{pose.flip, drawn.flip}, {pose.roll, drawn.roll}, {pose.spin, drawn.spin}}) {
                                double off = pair[0] - pair[1];
                                off -= Math.PI * 2.0 * Math.round(off / (Math.PI * 2.0));
                                check(Math.abs(off) < 1.0E-3, "a cut never jumps (its turns, a whole turn the same): " + was + " at " + at + " into " + now + ": " + off);
                            }
                        }
                    }
                }
            }
        }
        check(worst == 0.0, "her arms stay out of her body through every cut: " + worst + " px" + where);
        // A forward flip cut off two thirds round by a punch spins on round to its end, never back.
        RigPose drawn = PaleMotions.flip(PaleRules.Flip.FORWARD, 1.0 + PaleRules.FLIP_SPIN_TICKS * 0.6, -1.0);
        RigPose before = PaleMotions.flip(PaleRules.Flip.FORWARD, 1.0 + PaleRules.FLIP_SPIN_TICKS * 0.6 - 0.05, -1.0);
        PaleMotions.Cut cut = PaleMotions.cut(drawn, before);
        double previous = Double.MAX_VALUE;
        boolean onward = true;
        for (double t = 0.0; t <= cut.ticks() + 0.5; t += 0.1) {
            double turn = PaleMotions.strike(PaleStrike.JAB, t, cut.from()).flip + cut.left(t)[0];
            onward &= turn <= previous + 1.0E-6;
            previous = turn;
        }
        check(onward && Math.abs(previous) < 1.0E-3, "a flip cut off mid-way spins on to its end, never back: ends at " + previous);
        check(cut.ticks() <= 5.0, "and quickly: " + cut.ticks() + " ticks");
        // Barely begun, it is undone the short way instead of going all the way round.
        RigPose early = PaleMotions.flip(PaleRules.Flip.FORWARD, 2.0, -1.0);
        RigPose earlier = PaleMotions.flip(PaleRules.Flip.FORWARD, 1.95, -1.0);
        PaleMotions.Cut back = PaleMotions.cut(early, earlier);
        check(Math.abs(back.flip()) <= Math.PI, "a flip barely begun is undone the short way: " + back.flip());
        // Thrown in the air, an arm move keeps the flip in her legs: legs as the flip has them, arms the strike's.
        RigPose strike = PaleMotions.strike(PaleStrike.JAB, 2.0, null);
        RigPose tuck = PaleMotions.flip(PaleRules.Flip.FORWARD, 8.0, -1.0);
        RigPose layered = new RigPose().add(strike, 1.0F);
        PaleMotions.legsFrom(layered, tuck, 1.0F);
        check(layered.rightKnee == tuck.rightKnee && layered.leftLegX == tuck.leftLegX && layered.rightArmX == strike.rightArmX,
            "in the air: the flip's legs under the strike's arms");
        check(PaleMotions.continues(PaleMove.PUNCH_CHARGE, PaleMove.PUNCH) && PaleMotions.continues(PaleMove.GRAB_HOLD, PaleMove.GRAB_THROW)
            && !PaleMotions.continues(PaleMove.FLIP, PaleMove.STRIKE) && !PaleMotions.continues(PaleMove.KICK, PaleMove.PUNCH_CHARGE),
            "a move carrying on is not a cut; one move into another is");
        check(PaleMotions.upperBody(PaleMove.STRIKE) && PaleMotions.upperBody(PaleMove.SWIPE) && !PaleMotions.upperBody(PaleMove.KICK)
            && !PaleMotions.upperBody(PaleMove.PUNCH), "her blows and the flesh are arm moves; the kick and the punch take her whole body");
    }

    /** 6.2.0, Up We Go: a flip's launch built up smoothly, about two and a half times as high, turning over across the climb. */
    static void soar() {
        double previous = 0.0;
        for (int tick = 0; tick < PaleRules.SOAR_RAMP; tick++) {
            double k = PaleRules.soarRamp(tick);
            check(k > previous && k <= 1.0, "the launch only ever builds up: tick " + tick + ": " + k);
            check(tick == 0 ? k >= 0.35 : k - previous <= 0.3, "smooth: off the ground at once, then no jolt: tick " + tick);
            previous = k;
        }
        check(PaleRules.soarRamp(PaleRules.SOAR_RAMP - 1) == 1.0, "at full speed by the top of the ramp");
        for (PaleRules.Flip flip : PaleRules.Flip.values()) {
            double base = apex(new double[] {flip.up, flip.up}, 0);
            double[] ramp = new double[PaleRules.SOAR_RAMP];
            for (int tick = 0; tick < ramp.length; tick++) {
                ramp[tick] = PaleRules.soar(flip, tick)[1];
                check(PaleRules.soar(flip, tick)[0] <= flip.speed * PaleRules.SOAR_ALONG + 1.0E-9, "never faster along than it should be");
            }
            double high = apex(ramp, 0);
            check(high > base * 2.2 && high < 14.0, "Up We Go flies far higher (" + flip + "): " + high + " blocks against " + base);
            double worst = 0.0;
            for (double age = 0.0; age <= PaleRules.SOAR_RAMP + PaleRules.SOAR_SPIN_TICKS + 4.0; age += 0.1) {
                for (double landed : new double[] {-1.0, 0.0, 2.5, 6.0}) {
                    RigPose pose = PaleMotions.soar(flip, age, landed);
                    pose.settle(1.0F);
                    worst = Math.max(worst, BodyFrame.clipping(pose, CLEARANCE));
                }
            }
            check(worst == 0.0, "her arms stay out of her body all through the " + flip + " soar: " + worst + " px");
            RigPose rising = PaleMotions.soar(flip, PaleRules.SOAR_RAMP - 1.5, -1.0);
            check(Math.abs(rising.flip) < 1.0E-6 && Math.abs(rising.roll) < 1.0E-6 && rising.rightArmX < -2.0,
                "stretched through the ramp, arms up, not turning yet (" + flip + ")");
            RigPose done = PaleMotions.soar(flip, PaleRules.SOAR_RAMP - 1.0 + PaleRules.SOAR_SPIN_TICKS + 0.5, -1.0);
            check(Math.abs(Math.abs(done.flip + done.roll) - Math.PI * 2.0) < 1.0E-3, "a soar turns her over exactly once: " + flip);
            RigPose end = PaleMotions.ended(PaleMotions.soar(flip, PaleRules.SOAR_RAMP + PaleRules.SOAR_SPIN_TICKS + 4.0, 2.0));
            check(Math.abs(end.flip) < 0.05 && Math.abs(end.roll) < 0.05, "a landed soar is blended out of from no turn (" + flip + ")");
        }
    }

    /**
     * How high (blocks) a body goes when its upward speed is set to {@code ramp} on its first ticks (as the server sets it), then flies
     * on under the game's gravity and air drag.
     */
    static double apex(double[] ramp, int unused) {
        double y = 0.0;
        double v = 0.0;
        for (int tick = 0; tick < 200; tick++) {
            if (tick < ramp.length) {
                v = ramp[tick];
            }
            y += v;
            v = (v - 0.08) * 0.98;
            if (tick >= ramp.length && v <= 0.0) {
                return y;
            }
        }
        return y;
    }

    static void steam() {
        double total = 0.0;
        for (PaleSteamRules.Point point : PaleSteamRules.POINTS) {
            total += point.weight();
        }
        int legs = 0;
        int samples = 10000;
        for (int i = 0; i < samples; i++) {
            PaleSteamRules.Point point = PaleSteamRules.pick(i / (double)samples, false);
            legs += point.upper() ? 0 : 1;
            check(PaleSteamRules.pick(i / (double)samples, true).firstPerson(), "in first person, only points away from her face");
        }
        check(legs > 0 && legs < samples * 0.1, "the legs steam now and then, not much: " + legs);
        check(PaleSteamRules.rate(1.0F, 0.3, false) < PaleSteamRules.rate(1.0F, 0.0, false), "fewer wisps on the move: no trail");
        check(PaleSteamRules.rate(1.0F, 0.0, true) < PaleSteamRules.rate(1.0F, 0.0, false), "far fewer in first person");
        check(PaleSteamRules.rate(0.0F, 0.0, false) == 0.0F, "none once the form has faded");
        float strength = 0.0F;
        int ticks = 0;
        while (strength < 1.0F && ticks < 100) {
            strength = PaleSteamRules.ease(strength, true);
            ticks++;
        }
        check(ticks >= 15 && ticks <= 25, "it wakes over about a second: " + ticks);
        check(PaleSteamRules.opacity(0.0F, 0) == 0.0F && PaleSteamRules.opacity(1.0F, 50) == 0.0F && PaleSteamRules.opacity(0.3F, 12) > 0.4F,
            "a wisp opens, then fades to nothing");
        check(PaleSteamRules.size(1.0F) > PaleSteamRules.size(0.0F) * 3.0F, "a wisp spreads as it rises");
        java.util.Random random = new java.util.Random(3);
        for (int i = 0; i < 2000; i++) {
            float[] axes = PaleSteamRules.axes(random.nextFloat() * 7.0F, random.nextFloat() * 7.0F, random.nextFloat() * 7.0F);
            double[] view = {random.nextGaussian(), random.nextGaussian(), random.nextGaussian()};
            double length = Math.sqrt(view[0] * view[0] + view[1] * view[1] + view[2] * view[2]);
            double sum = 0.0;
            for (int a = 0; a < 3; a++) {
                double dot = (axes[a * 3] * view[0] + axes[a * 3 + 1] * view[1] + axes[a * 3 + 2] * view[2]) / length;
                sum += dot * dot;
                for (int b = 0; b < 3; b++) {
                    double ab = axes[a * 3] * axes[b * 3] + axes[a * 3 + 1] * axes[b * 3 + 1] + axes[a * 3 + 2] * axes[b * 3 + 2];
                    check(Math.abs(ab - (a == b ? 1.0 : 0.0)) < 1.0E-5, "a wisp's planes are square to each other");
                }
            }
            check(Math.abs(sum - 1.0) < 1.0E-4, "a wisp is as faint from every angle (its planes' shares add to one)");
        }
        double[] still = PaleSteamRules.place(PaleSteamRules.POINTS.get(0), 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        double[] leant = PaleSteamRules.place(PaleSteamRules.POINTS.get(0), 0.5, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        check(leant[2] > still[2] + 0.2 && leant[1] < still[1], "a shoulder's steam follows her lean forward");
    }
}
