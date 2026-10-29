package com.hex.kagune.control;

import com.hex.kagune.control.client.ControlView;
import com.hex.kagune.control.client.animation.IdlePose;
import com.hex.kagune.control.client.animation.RigPose;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free tests for the ambient idle poses: smooth easing in and out, and stances that settle and stay put. */
public final class IdlePoseTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    /**
     * Height of a leg's sole centre above the ground in model pixels (0 = planted), through the game's transforms: the leg pivot,
     * vanilla's Z*Y*X rotation, bendy-lib's knee bend about the leg's middle (the centre line has no bulge), and the body drop.
     */
    static double soleHeight(RigPose pose, int side) {
        boolean right = side < 0;
        float[] rot = pose.transform(right ? "rightLeg" : "leftLeg", RigPose.ROTATION, 0, 0, 0);
        float knee = pose.transform(right ? "rightLeg" : "leftLeg", RigPose.BEND, 0, 0, 0)[1];
        // Shin: from the knee (y 6) down 6 pixels, turned about x by the knee bend.
        double[] p = {0.0, 6.0 + 6.0 * Math.cos(knee), 6.0 * Math.sin(knee)};
        double y1 = p[1] * Math.cos(rot[0]) - p[2] * Math.sin(rot[0]);
        double z1 = p[1] * Math.sin(rot[0]) + p[2] * Math.cos(rot[0]);
        double x2 = p[0] * Math.cos(rot[1]) + z1 * Math.sin(rot[1]);
        double y3 = x2 * Math.sin(rot[2]) + y1 * Math.cos(rot[2]);
        double drop = -pose.transform("body", RigPose.POSITION, 0, 0, 0)[1] * 16.0;
        return 24.0 - (12.0 + y3) - drop;
    }

    /** {@code p} (a part's own pixels) placed by that part's transform from {@code pose}, as ModelPart.translateAndRotate does. */
    static double[] onPart(RigPose pose, String part, double[] vanillaPivot, double[] p) {
        float[] pos = pose.transform(part, RigPose.POSITION, (float)vanillaPivot[0], (float)vanillaPivot[1], (float)vanillaPivot[2]);
        float[] rot = pose.transform(part, RigPose.ROTATION, 0, 0, 0);
        double x = p[0], y = p[1], z = p[2];
        double y1 = y * Math.cos(rot[0]) - z * Math.sin(rot[0]), z1 = y * Math.sin(rot[0]) + z * Math.cos(rot[0]);
        double x2 = x * Math.cos(rot[1]) + z1 * Math.sin(rot[1]), z2 = -x * Math.sin(rot[1]) + z1 * Math.cos(rot[1]);
        double x3 = x2 * Math.cos(rot[2]) - y1 * Math.sin(rot[2]), y3 = x2 * Math.sin(rot[2]) + y1 * Math.cos(rot[2]);
        return new double[] {pos[0] + x3, pos[1] + y3, pos[2] + z2};
    }

    /** {@code p} (model pixels from the neck) where the lean the kakuja's tentacles ride puts it: turned about the hips. */
    static double[] leaned(RigPose pose, double[] p) {
        double[] b = com.hex.kagune.control.ukaku.KakujaRig.bendModel(new double[] {p[0], p[1] - RigPose.HIP_Y, p[2]}, pose.lean, pose.side);
        return new double[] {b[0], b[1] + RigPose.HIP_Y, b[2]};
    }

    static boolean same(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < 1.0E-3 && Math.abs(a[1] - b[1]) < 1.0E-3 && Math.abs(a[2] - b[2]) < 1.0E-3;
    }

    /**
     * 5.0.19: a lean never goes through PlayerAnimator's body bend. That bend draws the head and arms under a matrix their
     * model parts never carry, so whatever else draws on those parts (the skin's second layer under 3D Skin Layers, other mods'
     * armor, a launcher's own renderer) was left behind, drifting off the head. Now the torso, head and arms are each turned
     * about the hips through their own pivot and rotation: every renderer that draws on the parts draws on the leaned body.
     */
    static void upperTurn() {
        RigPose upright = new RigPose();
        float[] none = upright.transform("body", RigPose.BEND, 0, 0, 0);
        check(RigPose.upperTurn(none[0], none[1]) == null, "an upright body turns nothing");
        check(RigPose.UPPER_PIVOT == 0.375F, "another mod's bend is matched about mid-torso, as IBendHelper.rotateMatrixStack (0.375 blocks)");
        double[] eye = {-2.0, -3.0, -4.0};
        double[] shoulder = {-1.0, 1.0, -2.0};
        double[] chest = {2.0, 3.0, -2.0};
        java.util.Random random = new java.util.Random(13);
        for (int i = 0; i < 400; i++) {
            RigPose pose = new RigPose();
            pose.lean = (random.nextFloat() - 0.3F) * 0.9F;
            pose.side = (random.nextFloat() - 0.5F) * 0.5F;
            float[] bend = pose.transform("body", RigPose.BEND, 0, 0, 0);
            float[] torsoBend = pose.transform("torso", RigPose.BEND, 0, 0, 0);
            check(bend[0] == 0.0F && bend[1] == 0.0F && torsoBend[1] == 0.0F, "a lean is never a bend (lean " + pose.lean + ")");
            // The head, an arm and the chest, each placed by its own part, sit where the tentacles and chest armor ride.
            check(same(onPart(pose, "head", new double[] {0, 0, 0}, eye), leaned(pose, eye)),
                "the head leans with lean " + pose.lean + ", side " + pose.side + " as the tentacles and chest armor ride it");
            check(same(onPart(pose, "rightArm", new double[] {-5, 2, 0}, shoulder), leaned(pose, new double[] {-5 + shoulder[0], 2 + shoulder[1],
                shoulder[2]})), "the arm stays on the shoulder through the lean (" + pose.lean + ", " + pose.side + ")");
            check(same(onPart(pose, "torso", new double[] {0, 0, 0}, chest), leaned(pose, chest)), "the torso leans with them (" + pose.lean + ")");
        }
        // The kakugan and the masks are drawn on the head part's own transform, the same one the head (and its second layer) is
        // drawn with: a hunch moves the face forward, and everything drawn on it moves with it.
        double glowLift = 5 * com.hex.kagune.control.kakugan.KakuganModel.layerFor(1.0E9);
        for (IdlePose.Style style : new IdlePose.Style[] {IdlePose.Style.UKAKU_KAKUJA, IdlePose.Style.RINKAKU_FERAL}) {
            for (double time = 0.0; time < 20.0; time += 0.37) {
                RigPose stance = IdlePose.pose(style, 1.0F, time);
                double forward = eye[2] - onPart(stance, "head", new double[] {0, 0, 0}, eye)[2];
                check(forward > 3.0 * glowLift, style + ": the hunched face is " + forward + " px forward, and the layers on it with it");
            }
        }
        RigPose relaxed = IdlePose.pose(IdlePose.Style.UKAKU_RELAXED, 1.0F, 0.0);
        check(eye[2] - onPart(relaxed, "head", new double[] {0, 0, 0}, eye)[2] < -glowLift, "Ukaku's relaxed stance leans the face back");
    }

    /**
     * 5.0.19: through RigPose, as the game and the previews pose it: the Steel Blade's arm drops vanilla's own swing for its carry,
     * and whatever the arm, the lean, the drop or the crouch, no part of the blade goes into the ground.
     */
    private static void bladeCarry() {
        boolean clear = true;
        boolean own = true;
        String failure = "";
        for (float lean : new float[] {-0.3F, 0.0F, 0.5F, 1.0F}) {
            for (float drop : new float[] {0.0F, 3.0F}) {
                for (boolean crouch : new boolean[] {false, true}) {
                    for (float armX = -1.2F; armX <= 1.5F; armX += 0.15F) {
                        for (float armZ = -0.6F; armZ <= 1.6F; armZ += 0.4F) {
                            RigPose pose = new RigPose();
                            pose.lean = lean;
                            pose.drop = drop;
                            pose.rightArmHold = 1.0F;
                            pose.rightArmX = armX;
                            pose.rightArmZ = armZ;
                            pose.blade = 1.0F;
                            pose.ground = crouch ? com.hex.kagune.control.ukaku.BladeCarry.CROUCH_GROUND : com.hex.kagune.control.ukaku.BladeCarry.GROUND;
                            pose.shoulderY = crouch ? com.hex.kagune.control.ukaku.BladeCarry.CROUCH_SHOULDER : com.hex.kagune.control.ukaku.BladeCarry.SHOULDER;
                            // Vanilla's walking swing and crouch on the arm: dropped.
                            float[] r = pose.transform("rightArm", RigPose.ROTATION, 0.8F, 0.1F, 0.05F);
                            float[] still = pose.transform("rightArm", RigPose.ROTATION, -0.6F, 0.0F, -0.1F);
                            own &= r[0] == still[0] && r[1] == still[1] && r[2] == still[2];
                            float[] pivot = pose.transform("rightArm", RigPose.POSITION, -5.0F, pose.shoulderY, 0.0F);
                            double low = com.hex.kagune.control.ukaku.BladeCarry.lowest(com.hex.kagune.control.ukaku.BladeCarry.zyx(r[0], r[1], r[2]),
                                pivot, 1.0, false);
                            double ground = pose.ground - drop / 0.9375F;
                            if (low > ground - com.hex.kagune.control.ukaku.BladeCarry.CLEARANCE + 1.0E-3) {
                                clear = false;
                                failure = lean + "/" + drop + "/" + crouch + "/" + armX + "/" + armZ + ": " + low + " vs " + ground;
                            }
                        }
                    }
                }
            }
        }
        check(own, "the blade's arm holds its own pose, whatever vanilla's swing");
        // 6.0.4: resting (bladeFit 1), a blade hanging steeply at his side draws in to clear the ground instead of the arm being lifted.
        RigPose rest = new RigPose();
        rest.rightArmHold = 1.0F;
        rest.rightArmZ = 0.55F;
        rest.blade = 1.0F;
        rest.bladeFit = 1.0F;
        float[] hung = rest.transform("rightArm", RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        double hungLow = com.hex.kagune.control.ukaku.BladeCarry.lowest(com.hex.kagune.control.ukaku.BladeCarry.zyx(hung[0], hung[1], hung[2]),
            new float[] {-5.0F, rest.shoulderY, 0.0F}, rest.bladeReach, false);
        check(Math.abs(hung[2] - 0.55F) < 1.0E-4F && rest.bladeReach < 0.8F && rest.bladeReach > 0.3F
            && hungLow <= com.hex.kagune.control.ukaku.BladeCarry.GROUND - com.hex.kagune.control.ukaku.BladeCarry.CLEARANCE + 1.0E-3,
            "resting, the hanging blade draws in to clear the ground and the arm stays where it hangs (reach " + rest.bladeReach + ")");
        check(clear, "the blade never goes into the ground, whatever the arm, lean, drop or crouch (" + failure + ")");
        // And without the blade the arm is vanilla's plus the offsets, as ever.
        RigPose plain = new RigPose();
        plain.rightArmX = -0.3F;
        float[] r = plain.transform("rightArm", RigPose.ROTATION, 0.5F, 0.0F, 0.0F);
        check(Math.abs(r[0] - 0.2F) < 1.0E-6F, "without the blade nothing changes");
    }

    public static void main(String[] args) {
        bladeCarry();
        // Easing: no snap at either end, monotonic in between.
        check(IdlePose.ease(0.0F) == 0.0F && IdlePose.ease(1.0F) == 1.0F, "the pose eases from none to full");
        check(IdlePose.ease(0.02F) < 0.001F && IdlePose.ease(0.98F) > 0.999F, "it starts and ends gently (no snap in or out)");
        float previous = 0.0F;
        for (float b = 0.0F; b <= 1.0001F; b += 0.01F) {
            float e = IdlePose.ease(b);
            check(e >= previous - 1.0E-6F, "easing never goes backwards");
            previous = e;
        }
        // The in-game bug: the server keeps the last combo stage after the swing ends, so "has a combo" stayed true forever and the
        // pose never started after the first attack. Only a swing that is actually playing may hold the pose off.
        ControlView swung = new ControlView(1.0F, 0.0F, 0.0F, UrgeType.NONE, 0L, 0, 0, 0.0F, false, ComboStyle.WHIP, 2, 1000L, 12, 1, 0.0F, false);
        check(swung.hasCombo(), "the last stage is still reported after the swing");
        check(swung.comboPlaying(1005.0), "mid-swing counts as attacking");
        check(!swung.comboPlaying(1000.0 + 12 + ControlView.COMBO_SETTLE_TICKS + 1), "a finished swing no longer holds the pose off");
        check(!swung.comboPlaying(5000.0), "minutes later it certainly does not");
        check(!ControlView.NEUTRAL.comboPlaying(0.0), "no combo at all never blocks the pose");
        // Timing: a second of stillness first, then about a second and a half in, and under half a second out.
        check(IdlePose.STILL_TICKS == 20, "the pose waits for a second of standing still");
        int in = 0;
        for (float b = 0.0F; b < 1.0F; b = IdlePose.step(b, true)) {
            in++;
        }
        int out = 0;
        for (float b = 1.0F; b > 0.0F; b = IdlePose.step(b, false)) {
            out++;
        }
        check(in >= 20 && in <= 40, "easing in takes one to two seconds (" + in + " ticks)");
        check(out >= 4 && out <= 10, "easing out is quick when the player moves (" + out + " ticks)");
        // The stances: nothing at weight 0, a real pose at full weight, and every channel grows smoothly with the weight.
        for (IdlePose.Style style : IdlePose.Style.values()) {
            check(IdlePose.pose(style, 0.0F, 3.0).isZero(), "no weight, no pose: " + style);
        }
        RigPose ukaku = IdlePose.pose(IdlePose.Style.UKAKU_RELAXED, 1.0F, 0.0);
        RigPose rinkaku = IdlePose.pose(IdlePose.Style.RINKAKU_FERAL, 1.0F, 0.0);
        // Ukaku (Jotaro's stance): leaning back, the right hand in the pocket (upper arm back, elbow folded), the left forearm up.
        check(ukaku.lean < 0.0F, "Ukaku leans back a little, never hunches");
        check(ukaku.rightArmX > 0.2F && ukaku.rightElbow > 0.4F, "Ukaku: the right arm hangs back with the forearm folded into the pocket");
        check(ukaku.leftElbow > 0.5F, "Ukaku: the left forearm is raised");
        check(ukaku.rightKnee < 0.2F && ukaku.leftKnee < 0.3F, "Ukaku stands tall (no crouch)");
        // Rinkaku (Bakugo's crouch): hunched, head up, elbows out and bent, hands forward, wide bent-knee stance.
        check(rinkaku.lean > 0.2F && rinkaku.headX < -0.2F, "Rinkaku: hunched forward, head up to watch");
        check(rinkaku.rightArmZ > 0.3F && rinkaku.leftArmZ < -0.3F && rinkaku.rightElbow > 1.0F && rinkaku.leftElbow > 1.0F,
            "Rinkaku: elbows out to the sides, forearms bent forward like claws");
        check(Math.abs(rinkaku.rightArmZ + rinkaku.leftArmZ) < 0.05F, "Rinkaku's arms are symmetrical");
        check(rinkaku.rightLegZ > 0.2F && rinkaku.leftLegZ < -0.2F && rinkaku.rightKnee > 0.5F && rinkaku.leftKnee > 0.5F && rinkaku.drop > 1.0F,
            "Rinkaku: a wide stance on bent knees, the body sunk");
        // Bent legs and the body drop agree: both soles stay on the ground (within less than half a pixel).
        for (RigPose pose : new RigPose[] {ukaku, rinkaku}) {
            for (int side = -1; side <= 1; side += 2) {
                double sole = soleHeight(pose, side);
                check(Math.abs(sole) < 0.4, "the soles stay on the ground (" + sole + " px)");
            }
        }
        // 6.0.2: where limbs can't be drawn bent (3D Skin Layers, no bendy-lib), a stance built for straight limbs: no bends at all,
        // still hunched and watching, arms reaching forward and out like claws, feet wide, soles on the ground.
        for (double t = 0.0; t < 12.0; t += 0.41) {
            RigPose straight = IdlePose.pose(IdlePose.Style.RINKAKU_FERAL, 1.0F, t, false);
            check(straight.rightKnee == 0.0F && straight.leftKnee == 0.0F && straight.rightElbow == 0.0F && straight.leftElbow == 0.0F,
                "Rinkaku on straight limbs: nothing bent");
            check(straight.lean > 0.15F && straight.headX < -0.2F, "Rinkaku on straight limbs: hunched forward, head up to watch");
            check(straight.rightArmX < -0.3F && straight.leftArmX < -0.3F && straight.rightArmZ > 0.2F && straight.leftArmZ < -0.2F
                && Math.abs(straight.rightArmZ + straight.leftArmZ) < 0.05F, "Rinkaku on straight limbs: both arms reach forward and out, alike");
            check(straight.rightLegZ > 0.15F && straight.leftLegZ < -0.15F && straight.leftLegX < straight.rightLegX,
                "Rinkaku on straight limbs: feet wide, the left a little ahead");
            for (int side = -1; side <= 1; side += 2) {
                double sole = soleHeight(straight, side);
                check(Math.abs(sole) < 0.4, "Rinkaku on straight limbs: the soles stay on the ground (" + sole + " px)");
            }
        }
        for (IdlePose.Style style : new IdlePose.Style[] {IdlePose.Style.UKAKU_RELAXED, IdlePose.Style.UKAKU_KAKUJA}) {
            RigPose straight = IdlePose.pose(style, 1.0F, 0.0, false);
            boolean planted = true;
            for (int side = -1; side <= 1; side += 2) {
                double sole = soleHeight(straight, side);
                planted &= Math.abs(sole) < 0.4;
            }
            check(planted && straight.rightKnee == 0.0F && straight.leftKnee == 0.0F && straight.rightElbow == 0.0F && straight.leftElbow == 0.0F,
                style + " on straight limbs: nothing bent, both soles on the ground");
        }
        RigPose half = IdlePose.pose(IdlePose.Style.RINKAKU_FERAL, 0.5F, 0.0);
        check(Math.abs(half.lean - rinkaku.lean * 0.5F) < 1.0E-5F, "halfway in, halfway posed");
        for (double t = 0.0; t < 30.0; t += 0.37) {
            RigPose a = IdlePose.pose(IdlePose.Style.UKAKU_RELAXED, 1.0F, t);
            RigPose k = IdlePose.pose(IdlePose.Style.RINKAKU_FERAL, 1.0F, t);
            check(Math.abs(a.lean - ukaku.lean) < 0.05F && Math.abs(k.lean - rinkaku.lean) < 0.06F, "the stances breathe but stay put");
        }

        // PlayerAnimator conventions (the 1.2.2 poses got every one of these backwards):
        RigPose flex = new RigPose();
        flex.rightElbow = 1.0F;
        flex.leftKnee = 0.8F;
        check(flex.transform("rightArm", RigPose.BEND, 0, 0, 0)[1] == -1.0F,
            "an elbow flex is a negative bendy-lib bend (a positive one folds the forearm backwards)");
        check(flex.transform("leftLeg", RigPose.BEND, 0, 0, 0)[1] == 0.8F, "a knee bend is a positive bendy-lib bend");
        RigPose lean = new RigPose();
        lean.lean = 0.4F;
        float[] torso = lean.transform("torso", RigPose.ROTATION, 0, 0, 0);
        float[] head = lean.transform("head", RigPose.ROTATION, 0, 0, 0);
        float[] arm = lean.transform("rightArm", RigPose.ROTATION, 0, 0, 0);
        check(Math.abs(torso[0] - 0.4F) < 1.0E-5F && Math.abs(head[0] - 0.4F) < 1.0E-5F && Math.abs(arm[0] - 0.4F) < 1.0E-5F,
            "a forward lean tips the torso, the head and the arms together (never the torso alone)");
        float[] neck = lean.transform("head", RigPose.POSITION, 0, 0, 0);
        check(neck[2] < -4.0F && neck[1] > 0.5F, "the neck comes forward and down over the hips");
        RigPose back = new RigPose();
        back.lean = -0.1F;
        check(back.transform("torso", RigPose.ROTATION, 0, 0, 0)[0] < 0.0F && back.transform("head", RigPose.POSITION, 0, 0, 0)[2] > 0.0F,
            "leaning back tips them the other way");
        RigPose tilt = new RigPose();
        tilt.side = 0.2F;
        check(Math.abs(tilt.transform("torso", RigPose.ROTATION, 0, 0, 0)[2]) > 0.15F && Math.abs(tilt.transform("head", RigPose.POSITION, 0, 0, 0)[0]) > 1.5F,
            "a sideways tilt tips them about the forward axis");
        RigPose twist = new RigPose();
        twist.twist = 0.3F;
        float[] shoulder = twist.transform("rightArm", RigPose.POSITION, -5, 2, 0);
        check(Math.abs(shoulder[0] - (-5.0 * Math.cos(0.3))) < 1.0E-4 && Math.abs(shoulder[2] - 5.0 * Math.sin(0.3)) < 1.0E-4,
            "a chest twist carries the shoulder pivots round (like vanilla's attack swing)");
        check(twist.transform("rightArm", RigPose.ROTATION, 0, 0, 0)[1] == 0.3F && twist.transform("torso", RigPose.ROTATION, 0, 0, 0)[1] == 0.3F,
            "the arms turn with the chest");
        RigPose crouch = new RigPose();
        crouch.drop = 1.6F;
        check(crouch.transform("body", RigPose.POSITION, 0, 0, 0)[1] == -0.1F, "the drop lowers the whole body (blocks, y up)");
        upperTurn();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Idle pose tests: " + passed + " checks passed");
    }
}
