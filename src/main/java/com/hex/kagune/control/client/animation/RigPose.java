package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.ayato.BladeCarry;

/**
 * One frame of body pose offsets, and the single place that turns them into PlayerAnimator transforms (pure, so the
 * offline previewer applies exactly what the game applies).
 *
 * <p>PlayerAnimator and bendy-lib behave in ways that are easy to get backwards, so the channels here are physical:
 * <ul>
 *   <li><b>lean / side</b> tip the whole upper body forward or sideways about the hips (5.0.19): the torso, the head and
 *       both arms are turned together about {@link #HIP_Y}, each through its own pivot and rotation, the way vanilla's crouch
 *       leans the body. Never through PlayerAnimator's {@code body} bend: that bends the torso mesh and draws the head and arms
 *       under a matrix of its own that their model parts never carry, so anything else drawn on them from the parts (the
 *       skin's second layer under 3D Skin Layers, armor from other mods) was left behind, drifting off the head.</li>
 *   <li><b>elbow / knee</b> are flexion: positive folds the forearm forward and the shin back, the way joints bend. bendy-lib bends
 *       the lower half of an arm or leg toward model +z (backwards) for a positive value, so the elbows are written negated and
 *       the knees as they are.</li>
 *   <li><b>twist</b> turns the chest (torso yaw). The arms do not follow a torso rotation either, so their pivots and
 *       yaw are carried round here, as vanilla does for its attack swing.</li>
 *   <li><b>drop</b> lowers the whole body (model pixels, + down) for crouches; bent knees take up the difference.</li>
 *   <li><b>lunge</b> (6.1.0) moves the whole body forward (model pixels, + forward; - back): the hips carried over a stepping
 *       foot, a punch with the weight behind it. The legs, angled, keep the feet where they were.</li>
 *   <li><b>flip</b> turns the whole body head-over-heels about the hips (radians, + backwards: a backflip runs 0 to 2 pi; 6.1.0:
 *       - forwards, a front flip runs 0 to -2 pi).</li>
 *   <li><b>roll</b> (6.1.0) turns the whole body about its front-to-back line through the same point (radians, + rolls it to its
 *       left: the right side goes up; a side flip runs a full turn), and <b>spin</b> about the vertical (radians, + turns it to its
 *       left, as seen from above), without turning the player's facing.</li>
 * </ul>
 * Other angles are offsets in radians on top of vanilla's (PlayerModel conventions): head x+ looks down; arm and leg x+
 * swing backwards, x- forwards; the right arm and right leg z+ swing out to the side, the left ones z-; arm y turns the
 * arm about its own length (with a raised arm, it swings it across the body).
 */
public final class RigPose {
    /** Distance from the torso's centre line to each shoulder pivot (model pixels). */
    public static final float ARM_PIVOT = 5.0F;

    public static final int POSITION = 0;
    public static final int ROTATION = 1;
    public static final int BEND = 2;
    /** Where PlayerAnimator turns the head and arms with the body bend: mid-torso, 6 model pixels below the neck (blocks). */
    public static final float UPPER_PIVOT = 0.375F;
    /** Where the upper body leans from: the hips, model pixels down from the neck (the bottom of the torso). */
    public static final float HIP_Y = 12.0F;

    public float headX, headY, headZ;
    public float lean, side, twist, drop, lunge, flip, roll, spin;
    public float rightArmX, rightArmY, rightArmZ, rightElbow, rightShoulderY, rightShoulderZ;
    public float leftArmX, leftArmY, leftArmZ, leftElbow, leftShoulderY, leftShoulderZ;
    public float rightLegX, rightLegY, rightLegZ, rightKnee;
    public float leftLegX, leftLegY, leftLegZ, leftKnee;
    /**
     * 6.2.0: the plane each knee folds in, turned about the leg's own length (radians; bendy-lib's bend axis): 0 folds the shin
     * straight back, about a quarter turn folds it out to a side (a roundhouse kick's chamber, the shin level behind the knee).
     */
    public float rightKneeTurn, leftKneeTurn;
    /**
     * 5.0.19, the Steel Blade: how much of the right arm's own (vanilla) rotation is dropped for the pose here (1: the arm is all
     * the blade's carry, no walking swing or crouch), and the blade's ground guard ({@link BladeCarry#guard}): how far the blade
     * is driven out (0: no guard), whether the arm is slim, where the ground is below the model (model y, before {@link #drop})
     * and the shoulder's own pivot height. Set directly, not added or scaled.
     */
    public float rightArmHold;
    /**
     * 6.1.0: how much of both arms' and both legs' own (vanilla) rotation is dropped for the pose here (1: the limbs are all the
     * move's: no walking swing, no attack swing, no bob), as {@link #rightArmHold} does for the right arm alone. Eto's strikes and
     * flips hold all four: their keys are solved for exactly the turn they make. Unlike the blade's hold, these are added and
     * scaled like any channel, so a move's limbs blend in and out.
     */
    public float armHold, legHold;
    /**
     * 6.2.2: how much of the head's own (vanilla, camera) turn is dropped (1: the head is the move's alone, square on her body).
     * Witness This turns and tips her whole body every blow; the camera's look on top of that threw her head about.
     */
    public float headHold;
    /**
     * 6.2.0: how bulked up her body is (0..1), as HexHaki's haki physique draws it (the Owl): her arms pushed out from her body by
     * {@link #BULK_SPREAD} pixels at the full bulk (drawn thicker by {@link #BULK_ARMS} and with a broader chest over her torso by
     * the renderer). Added and scaled like any channel.
     */
    public float bulk;
    /** At the full bulk: how far (pixels) each arm is pushed out, how much thicker it is, how much broader and deeper the chest. */
    public static final float BULK_SPREAD = 2.95F;
    public static final float BULK_ARMS = 0.4F;
    public static final float BULK_CHEST = 0.55F;
    public static final float BULK_CHEST_DEPTH = 0.3575F;
    public float blade;
    public boolean bladeSlim;
    public float ground = BladeCarry.GROUND;
    public float shoulderY = BladeCarry.SHOULDER;
    /**
     * 6.0.4: how much the blade draws in to rest off the ground (0 full length, the guard lifting the arm if it must; 1 drawn in
     * as far as it must, {@link BladeCarry#fit}); and, set as the arm's turn is worked out, how far out it is (of {@link #blade}):
     * what the blade is drawn with.
     */
    public float bladeFit;
    public float bladeReach = 1.0F;
    /**
     * 6.2.0: 1 on the pose a move starts from when it cut another off mid-way (EtoMotions.Cut): a raised arm, wherever it was, is
     * brought down outside her head. Drawn as nothing.
     */
    public float cut;

    public RigPose clear() {
        this.headX = this.headY = this.headZ = 0.0F;
        this.lean = this.side = this.twist = this.drop = this.lunge = this.flip = this.roll = this.spin = 0.0F;
        this.rightArmX = this.rightArmY = this.rightArmZ = this.rightElbow = this.rightShoulderY = this.rightShoulderZ = 0.0F;
        this.leftArmX = this.leftArmY = this.leftArmZ = this.leftElbow = this.leftShoulderY = this.leftShoulderZ = 0.0F;
        this.rightLegX = this.rightLegY = this.rightLegZ = this.rightKnee = 0.0F;
        this.leftLegX = this.leftLegY = this.leftLegZ = this.leftKnee = 0.0F;
        this.rightKneeTurn = this.leftKneeTurn = 0.0F;
        this.rightArmHold = this.blade = 0.0F;
        this.armHold = this.legHold = 0.0F;
        this.headHold = 0.0F;
        this.bulk = 0.0F;
        this.cut = 0.0F;
        this.bladeFit = 0.0F;
        this.bladeReach = 1.0F;
        this.bladeSlim = false;
        this.ground = BladeCarry.GROUND;
        this.shoulderY = BladeCarry.SHOULDER;
        return this;
    }

    /** Adds {@code other} scaled by {@code weight}. */
    public RigPose add(RigPose other, float weight) {
        this.headX += other.headX * weight;
        this.headY += other.headY * weight;
        this.headZ += other.headZ * weight;
        this.lean += other.lean * weight;
        this.side += other.side * weight;
        this.twist += other.twist * weight;
        this.drop += other.drop * weight;
        this.lunge += other.lunge * weight;
        this.flip += other.flip * weight;
        this.roll += other.roll * weight;
        this.spin += other.spin * weight;
        this.rightArmX += other.rightArmX * weight;
        this.rightArmY += other.rightArmY * weight;
        this.rightArmZ += other.rightArmZ * weight;
        this.rightElbow += other.rightElbow * weight;
        this.rightShoulderY += other.rightShoulderY * weight;
        this.rightShoulderZ += other.rightShoulderZ * weight;
        this.leftArmX += other.leftArmX * weight;
        this.leftArmY += other.leftArmY * weight;
        this.leftArmZ += other.leftArmZ * weight;
        this.leftElbow += other.leftElbow * weight;
        this.leftShoulderY += other.leftShoulderY * weight;
        this.leftShoulderZ += other.leftShoulderZ * weight;
        this.rightLegX += other.rightLegX * weight;
        this.rightLegY += other.rightLegY * weight;
        this.rightLegZ += other.rightLegZ * weight;
        this.rightKnee += other.rightKnee * weight;
        this.leftLegX += other.leftLegX * weight;
        this.leftLegY += other.leftLegY * weight;
        this.leftLegZ += other.leftLegZ * weight;
        this.leftKnee += other.leftKnee * weight;
        this.rightKneeTurn += other.rightKneeTurn * weight;
        this.leftKneeTurn += other.leftKneeTurn * weight;
        this.armHold += other.armHold * weight;
        this.headHold += other.headHold * weight;
        this.legHold += other.legHold * weight;
        this.bulk += other.bulk * weight;
        this.cut += other.cut * weight;
        return this;
    }

    /**
     * 1.0.26: a move's pose with its legs ({@code keep} of them: 0 none) and what carries the whole body (a drop, a lunge, a flip, a
     * roll, a spin) let go, so the legs go on running under it; what it turns the whole body by is kept as a turn of the chest instead
     * (at most {@code maxTwist} radians).
     */
    public RigPose legs(float keep, float maxTwist) {
        float let = 1.0F - keep;
        this.twist -= clamp(this.spin * let, maxTwist);
        this.spin *= keep;
        this.drop *= keep;
        this.lunge *= keep;
        this.flip *= keep;
        this.roll *= keep;
        this.rightLegX *= keep;
        this.rightLegY *= keep;
        this.rightLegZ *= keep;
        this.rightKnee *= keep;
        this.leftLegX *= keep;
        this.leftLegY *= keep;
        this.leftLegZ *= keep;
        this.leftKnee *= keep;
        this.rightKneeTurn *= keep;
        this.leftKneeTurn *= keep;
        this.legHold *= keep;
        return this;
    }

    public RigPose scale(float weight) {
        RigPose copy = new RigPose();
        return copy.add(this, weight);
    }

    /** Scales everything by {@code damp} and keeps each channel within a sane range (and finite). */
    public void settle(float damp) {
        this.headX = clamp(this.headX * damp, 1.1F);
        this.headY = clamp(this.headY * damp, 1.3F);
        this.headZ = clamp(this.headZ * damp, 0.6F);
        this.lean = clamp(this.lean * damp, 1.0F);
        // 6.2.0: as far over as the side kick leans her, away from the leg.
        this.side = clamp(this.side * damp, 0.6F);
        this.twist = clamp(this.twist * damp, 1.2F);
        this.drop = clamp(this.drop * damp, 4.0F);
        this.lunge = clamp(this.lunge * damp, 8.0F);
        // A flip is all or nothing (a partial turn would leave him lying in the air); so are a roll and a spin.
        float turn = (float)(Math.PI * 2.0);
        this.flip = damp >= 0.5F && Float.isFinite(this.flip) ? Math.max(-turn, Math.min(turn, this.flip)) : 0.0F;
        this.roll = damp >= 0.5F && Float.isFinite(this.roll) ? Math.max(-turn, Math.min(turn, this.roll)) : 0.0F;
        this.spin = damp >= 0.5F && Float.isFinite(this.spin) ? Math.max(-turn, Math.min(turn, this.spin)) : 0.0F;
        this.rightArmX = clamp(this.rightArmX * damp, 3.3F);
        this.rightArmY = clamp(this.rightArmY * damp, 1.6F);
        this.rightArmZ = clamp(this.rightArmZ * damp, 1.6F);
        this.rightElbow = clamp(this.rightElbow * damp, 2.2F);
        this.rightShoulderY = clamp(this.rightShoulderY * damp, 2.0F);
        this.rightShoulderZ = clamp(this.rightShoulderZ * damp, 2.0F);
        this.leftArmX = clamp(this.leftArmX * damp, 3.3F);
        this.leftArmY = clamp(this.leftArmY * damp, 1.6F);
        this.leftArmZ = clamp(this.leftArmZ * damp, 1.6F);
        this.leftElbow = clamp(this.leftElbow * damp, 2.2F);
        this.leftShoulderY = clamp(this.leftShoulderY * damp, 2.0F);
        this.leftShoulderZ = clamp(this.leftShoulderZ * damp, 2.0F);
        // 6.1.0: a leg may go well past level (a high kick, an axe kick's raise), never round to the back.
        this.rightLegX = clamp(this.rightLegX * damp, 2.6F);
        this.rightLegY = clamp(this.rightLegY * damp, 0.8F);
        // 6.2.0: out to the side and on up past level (a side kick's leg, straight out of the hip, riding on up after the blow).
        this.rightLegZ = clamp(this.rightLegZ * damp, 2.2F);
        this.rightKnee = clamp(this.rightKnee * damp, 2.0F);
        this.leftLegX = clamp(this.leftLegX * damp, 2.6F);
        this.leftLegY = clamp(this.leftLegY * damp, 0.8F);
        this.leftLegZ = clamp(this.leftLegZ * damp, 2.2F);
        this.leftKnee = clamp(this.leftKnee * damp, 2.0F);
        this.rightKneeTurn = clamp(this.rightKneeTurn * damp, 1.7F);
        this.leftKneeTurn = clamp(this.leftKneeTurn * damp, 1.7F);
        // A knee can't bend backwards and an elbow can only straighten a little past straight.
        this.rightKnee = Math.max(0.0F, this.rightKnee);
        this.leftKnee = Math.max(0.0F, this.leftKnee);
        this.rightElbow = Math.max(-0.1F, this.rightElbow);
        this.leftElbow = Math.max(-0.1F, this.leftElbow);
        this.rightArmHold = Float.isFinite(this.rightArmHold) ? Math.max(0.0F, Math.min(1.0F, this.rightArmHold)) : 0.0F;
        this.armHold = Float.isFinite(this.armHold) ? Math.max(0.0F, Math.min(1.0F, this.armHold * damp)) : 0.0F;
        this.legHold = Float.isFinite(this.legHold) ? Math.max(0.0F, Math.min(1.0F, this.legHold * damp)) : 0.0F;
        this.headHold = Float.isFinite(this.headHold) ? Math.max(0.0F, Math.min(1.0F, this.headHold * damp)) : 0.0F;
        this.bulk = Float.isFinite(this.bulk) ? Math.max(0.0F, Math.min(1.0F, this.bulk)) : 0.0F;
        this.blade = Float.isFinite(this.blade) ? Math.max(0.0F, Math.min(1.1F, this.blade)) : 0.0F;
    }

    public boolean isZero() {
        return this.headX == 0 && this.headY == 0 && this.headZ == 0 && this.lean == 0 && this.side == 0 && this.twist == 0 && this.drop == 0
            && this.lunge == 0
            && this.flip == 0 && this.roll == 0 && this.spin == 0
            && this.rightArmX == 0 && this.rightArmY == 0 && this.rightArmZ == 0 && this.rightElbow == 0 && this.rightShoulderY == 0
            && this.rightShoulderZ == 0 && this.leftArmX == 0 && this.leftArmY == 0 && this.leftArmZ == 0 && this.leftElbow == 0
            && this.leftShoulderY == 0 && this.leftShoulderZ == 0 && this.rightLegX == 0 && this.rightLegY == 0 && this.rightLegZ == 0
            && this.rightKnee == 0 && this.leftLegX == 0 && this.leftLegY == 0 && this.leftLegZ == 0 && this.leftKnee == 0
            && this.rightKneeTurn == 0 && this.leftKneeTurn == 0
            && this.armHold == 0 && this.legHold == 0 && this.headHold == 0 && this.bulk == 0;
    }

    /**
     * The transform PlayerAnimator should use for {@code part} ("head", "torso", "body", "rightArm", "leftArm", "rightLeg",
     * "leftLeg"), given the value from the layers below ({@code x, y, z}). Rotations in radians, positions in model pixels
     * except "body" (blocks, y up); a bend is (axis, amount, unused).
     */
    public float[] transform(String part, int type, float x, float y, float z) {
        switch (part) {
            case "head":
                if (type == ROTATION && this.headHold > 0.0F) {
                    float own = 1.0F - Math.min(1.0F, this.headHold);
                    return this.leanRotation(x * own + this.headX, y * own + this.headY, z * own + this.headZ);
                }
                return type == ROTATION ? this.leanRotation(x + this.headX, y + this.headY, z + this.headZ)
                    : type == POSITION ? this.leanPosition(x, y, z) : new float[] {x, y, z};
            case "torso":
                return type == ROTATION ? this.leanRotation(x, y + this.twist, z) : type == POSITION ? this.leanPosition(x, y, z) : new float[] {x, y, z};
            case "body":
                if (type == ROTATION) {
                    // PlayerRendererMixin turns the whole model about a point 0.7 blocks up; x+ tips the head backwards.
                    return new float[] {x + this.flip, y + this.spin, z + this.roll};
                }
                // Blocks, in the mixin's frame: y up, z backward (so a lunge forward is -z).
                return type == POSITION ? new float[] {x, y - this.drop / 16.0F, z - this.lunge / 16.0F} : new float[] {x, y, z};
            case "rightArm":
                return this.arm(type, x, y, z, true);
            case "leftArm":
                return this.arm(type, x, y, z, false);
            case "rightLeg":
                return type == ROTATION ? new float[] {x * this.legOwn() + this.rightLegX, y * this.legOwn() + this.rightLegY, z * this.legOwn() + this.rightLegZ}
                    : type == BEND ? new float[] {x + this.rightKneeTurn, y + this.rightKnee, z} : new float[] {x, y, z};
            case "leftLeg":
                return type == ROTATION ? new float[] {x * this.legOwn() + this.leftLegX, y * this.legOwn() + this.leftLegY, z * this.legOwn() + this.leftLegZ}
                    : type == BEND ? new float[] {x + this.leftKneeTurn, y + this.leftKnee, z} : new float[] {x, y, z};
            default:
                return new float[] {x, y, z};
        }
    }

    /**
     * 5.0.13: the turn PlayerAnimator draws the head and arms with for a {@code body} bend of ({@code axis}, {@code amount}) (the
     * BEND transform of "body" from every layer, as its {@code IBendHelper.rotateMatrixStack} reads it): {angle, x, y, z}, a turn by
     * angle (radians) about the unit axis (x, y, z) in model space (x to the player's left, y down, z backward) through
     * {@link #UPPER_PIVOT}; null when there is none. Anything drawn on the head or an arm by a layer of its own (the kakugan, the
     * masks, wounds) must take the same turn, or a lean leaves it behind.
     */
    public static float[] upperTurn(float axis, float amount) {
        if (!(Math.abs(amount) >= 1.0E-5F) || !Float.isFinite(axis)) {
            return null;
        }
        return new float[] {amount, (float)Math.cos(-axis), 0.0F, (float)Math.sin(-axis)};
    }

    /**
     * The upper body's lean as a turn in model space (x to the player's left, y down, z backward): {@link #lean} forward and
     * {@link #side} sideways, about the axis (lean, 0, side) by their length; null when there is none.
     */
    double[][] leanTurn() {
        double amount = Math.sqrt((double)this.lean * this.lean + (double)this.side * this.side);
        if (!(amount >= 1.0E-5)) {
            return null;
        }
        double ux = this.lean / amount;
        double uz = this.side / amount;
        double c = Math.cos(amount);
        double s = Math.sin(amount);
        double t = 1.0 - c;
        // Rodrigues' rotation about the unit axis (ux, 0, uz).
        return new double[][] {{c + ux * ux * t, -uz * s, ux * uz * t}, {uz * s, c, -ux * s}, {ux * uz * t, ux * s, c + uz * uz * t}};
    }

    /** A part's pivot (model pixels) where the lean carries it: turned about the hips. */
    float[] leanPosition(float x, float y, float z) {
        double[][] r = this.leanTurn();
        if (r == null) {
            return new float[] {x, y, z};
        }
        double dy = y - HIP_Y;
        return new float[] {(float)(r[0][0] * x + r[0][1] * dy + r[0][2] * z), (float)(HIP_Y + r[1][0] * x + r[1][1] * dy + r[1][2] * z),
            (float)(r[2][0] * x + r[2][1] * dy + r[2][2] * z)};
    }

    /**
     * A part's rotation (ModelPart's x, y, z: applied as Rz Ry Rx) with the lean's turn put before it, as the same three angles: the
     * part turned with the upper body.
     */
    float[] leanRotation(float x, float y, float z) {
        if (this.leanTurn() == null) {
            return new float[] {x, y, z};
        }
        return this.leanRotation(zyx(x, y, z));
    }

    /** A part's rotation given as a matrix (Rz Ry Rx), with the lean's turn put before it, as ModelPart's three angles. */
    float[] leanRotation(double[][] turn) {
        double[][] r = this.leanTurn();
        double[][] m = r == null ? turn : multiply(r, turn);
        double sinY = Math.max(-1.0, Math.min(1.0, -m[2][0]));
        double ry = Math.asin(sinY);
        double rx;
        double rz;
        if (Math.abs(sinY) < 0.999999) {
            rx = Math.atan2(m[2][1], m[2][2]);
            rz = Math.atan2(m[1][0], m[0][0]);
        } else {
            // Straight up or down its own y: only the sum of the other two is defined; keep it all in x.
            rz = 0.0;
            rx = Math.atan2(-m[1][2], m[1][1]);
        }
        return new float[] {(float)rx, (float)ry, (float)rz};
    }

    /** ModelPart's rotation as a matrix: Rz(z) Ry(y) Rx(x). */
    static double[][] zyx(double x, double y, double z) {
        double cx = Math.cos(x);
        double sx = Math.sin(x);
        double cy = Math.cos(y);
        double sy = Math.sin(y);
        double cz = Math.cos(z);
        double sz = Math.sin(z);
        double[][] rx = {{1, 0, 0}, {0, cx, -sx}, {0, sx, cx}};
        double[][] ry = {{cy, 0, sy}, {0, 1, 0}, {-sy, 0, cy}};
        double[][] rz = {{cz, -sz, 0}, {sz, cz, 0}, {0, 0, 1}};
        return multiply(rz, multiply(ry, rx));
    }

    static double[][] multiply(double[][] a, double[][] b) {
        double[][] out = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                out[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return out;
    }

    /** How much of the legs' own (vanilla) rotation is kept. */
    private float legOwn() {
        return 1.0F - (Float.isFinite(this.legHold) ? Math.max(0.0F, Math.min(1.0F, this.legHold)) : 0.0F);
    }

    /** An arm's turn with {@code own} of vanilla's kept: held, the turn is in the chest's own frame (see below). */
    private float[] heldArm(float x, float y, float z, float armX, float armY, float armZ, float own) {
        float twist = this.twist;
        if (own >= 1.0F) {
            return this.leanRotation(x + armX, y + armY + twist, z + armZ);
        }
        // A held arm's turn is in the chest's own frame: the chest's twist carries all of it round (not added to one of its angles,
        // which is only right for an arm hanging straight down).
        double[][] arm = zyx(x * own + armX, y * own + armY + twist * own, z * own + armZ);
        return this.leanRotation(multiply(zyx(0.0, twist * (1.0F - own), 0.0), arm));
    }

    private float[] arm(int type, float x, float y, float z, boolean right) {
        float twist = this.twist;
        if (type == ROTATION) {
            float both = Float.isFinite(this.armHold) ? Math.max(0.0F, Math.min(1.0F, this.armHold)) : 0.0F;
            if (!right) {
                return this.heldArm(x, y, z, this.leftArmX, this.leftArmY, this.leftArmZ, 1.0F - both);
            }
            float own = 1.0F - Math.max(both, Math.max(0.0F, Math.min(1.0F, this.rightArmHold)));
            float[] turned = this.heldArm(x, y, z, this.rightArmX, this.rightArmY, this.rightArmZ, own);
            if (this.blade > 0.0F) {
                // The Steel Blade never goes into the ground: resting, it draws in to clear it (6.0.4); otherwise, or where even
                // drawn in it would not, the arm is lifted just clear of it (the shoulder's pivot as below).
                float[] pivot = this.arm(POSITION, -ARM_PIVOT, this.shoulderY, 0.0F, true);
                double ground = this.ground - this.drop / 0.9375F;
                double ext = this.blade;
                float fit = Float.isFinite(this.bladeFit) ? Math.max(0.0F, Math.min(1.0F, this.bladeFit)) : 0.0F;
                if (fit > 0.0F) {
                    double fitted = BladeCarry.fit(BladeCarry.zyx(turned[0], turned[1], turned[2]), pivot, ground, this.blade, this.bladeSlim);
                    ext = this.blade + (fitted - this.blade) * fit;
                }
                this.bladeReach = (float)(ext / this.blade);
                turned = BladeCarry.guard(turned[0], turned[1], turned[2], pivot, ground, ext, this.bladeSlim);
            }
            return turned;
        }
        if (type == BEND) {
            return new float[] {x, y - (right ? this.rightElbow : this.leftElbow), z};
        }
        // The shoulder pivots ride round with the chest (vanilla's own attack swing does the same), plus a shrug; a bulked-up body
        // carries them further out.
        float cos = (float)Math.cos(twist);
        float sin = (float)Math.sin(twist);
        float out = ARM_PIVOT + BULK_SPREAD * (Float.isFinite(this.bulk) ? Math.max(0.0F, Math.min(1.0F, this.bulk)) : 0.0F);
        if (right) {
            return this.leanPosition(x + ARM_PIVOT - out * cos, y + this.rightShoulderY, z + out * sin + this.rightShoulderZ);
        }
        return this.leanPosition(x + out * cos - ARM_PIVOT, y + this.leftShoulderY, z - out * sin + this.leftShoulderZ);
    }

    /**
     * How far (model pixels) bent legs lift the feet: the body {@link #drop} that keeps them on the ground. Each leg is two
     * 6-pixel halves, the shin turning about the middle by the knee bend.
     */
    public static float legDrop(float legX, float legZ, float knee) {
        double thigh = 6.0 * Math.cos(legX) * Math.cos(legZ);
        double shin = 6.0 * Math.cos(legX + knee) * Math.cos(legZ);
        return (float)(12.0 - thigh - shin);
    }

    private static float clamp(float value, float limit) {
        return Float.isFinite(value) ? Math.max(-limit, Math.min(limit, value)) : 0.0F;
    }
}
