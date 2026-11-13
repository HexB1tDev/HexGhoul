package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.quinque.ReaperBladeMesh;

/**
 * 4.1.29 (the owner: "a heavy weapon, dual-handed, that deals massive damage ... Arima can be super fast while using it"): the White Reaper
 * with his second-phase blade (pure): its guard, and its own heavy string, each blow by intent, hands first. Every key says where both hands
 * are (together on the grip, in front of his chest: how high, how far round to a side), where the blade points from them (its pitch, its
 * yaw, its edge's roll) and what his body does (twist, lean, step, sink); each frame his straight arms (a mob: no elbows) are aimed so his
 * fists are on the grip, the right at the guard and the left {@link ReaperBladeMesh#LEFT_HAND} behind it, and his right wrist turns the
 * blade to where the key points it (RigPose's weapon channels, drawn by {@code client.render.ReaperBladeLayer}). Nothing is solved by
 * search: each frame is a few turns.
 *
 * <p>Frames: model pixels in the chest's own frame (x to his left, y down, z to his back), as {@link QuinqueGrip}.
 */
public final class ReaperBladePose {
    private ReaperBladePose() {
    }

    /** A wide arm's fist (its own frame, pixels): the middle of the hand a pixel above the arm's end. */
    static final double[] FIST_RIGHT = {-1.0, 9.0, 0.0};
    static final double[] FIST_LEFT = {1.0, 9.0, 0.0};
    static final double ARM = Math.sqrt(1.0 + 81.0);
    /** Between the two hands along the grip (pixels). */
    static final double GAP = -ReaperBladeMesh.LEFT_HAND;
    /** The blade's frame in the right arm's own at rest (columns: along it, toward its edge, through it): forward of the fist, edge down. */
    public static final double[][] HELD = {{0, 0, 1}, {0, 1, 0}, {-1, 0, 0}};
    /** Where both hands' middle circles round (between the shoulders). */
    static final double[] CENTRE = {0.0, 2.0, 0.0};

    /**
     * The keys' channels: tick; body twist (+ to his right), lean (+ forward), lunge (pixels), sink (pixels); the hands' height (radians, +
     * up from his shoulders' level) and how far round to his left (radians); the blade's pitch (+ up), yaw (+ to his left), its edge's roll
     * (0: down); his right leg's and left leg's swing (- forward); his head's pitch (+ down).
     */
    static final int T = 0;
    static final int TWIST = 1;
    static final int LEAN = 2;
    static final int LUNGE = 3;
    static final int SINK = 4;
    static final int HIGH = 5;
    static final int ROUND = 6;
    static final int PITCH = 7;
    static final int YAW = 8;
    static final int ROLL = 9;
    static final int RIGHT_LEG = 10;
    static final int LEFT_LEG = 11;
    static final int HEAD = 12;

    /**
     * His guard. 4.1.37 (the owner: "[Arima] is actually holding the blade upside down, bro. The blade is facing up instead of down"): as he
     * holds it in the owner's picture (art/reaper/Reaper_Quinque1.jpg): his fist low at his right hip and the blade's point down toward the
     * ground ahead of him along his side, its edge down, the rod and its prong back past his hip: in his right hand alone ({@link #HANG_X}),
     * the left loose; the right foot forward. (4.1.29 had it raised, point up, both hands: now {@link #PRESENT}, as it leaves the case.)
     */
    static final float[] GUARD = {0, 0.08F, 0.02F, 0.0F, 0.6F, -0.78F, -0.42F, -0.3F, -0.18F, 0.0F, -0.22F, 0.26F, 0.1F};
    /** 4.1.29's guard, kept for the draw: both hands before him, the blade up at an angle ahead, as it leaves the case. */
    static final float[] PRESENT = {0, 0.0F, 0.08F, 0.0F, 0.6F, -0.45F, -0.22F, 0.55F, -0.3F, 0.0F, -0.22F, 0.26F, 0.06F};

    /** {@code key} at tick {@code t}. */
    static float[] at(float[] key, float t) {
        float[] k = key.clone();
        k[T] = t;
        return k;
    }

    /**
     * His string's keys (each blow's length and impact: npc.ReaperBladeRules): a cleave from over his head; a rising cut from low on his left;
     * a sweep across; a thrust, all his weight behind it.
     */
    public static final float[][][] STRING = {
        {
            GUARD,
            {5, 0.0F, -0.14F, -0.5F, 0.0F, 0.72F, 0.0F, 2.3F, 0.0F, 0.0F, -0.1F, 0.3F, -0.12F},
            {8, 0.0F, 0.34F, 3.0F, 1.6F, -0.4F, 0.0F, 0.05F, 0.0F, 0.0F, -0.6F, 0.48F, 0.22F},
            {10, 0.0F, 0.4F, 3.2F, 2.0F, -0.46F, 0.0F, 0.1F, 0.0F, 0.0F, -0.62F, 0.5F, 0.26F},
            at(GUARD, 16)},
        {
            GUARD,
            {4, -0.6F, 0.16F, 0.0F, 1.6F, -0.72F, 0.32F, -0.55F, 2.2F, 3.1416F, -0.1F, 0.4F, 0.15F},
            {7, 0.5F, -0.08F, 2.2F, 0.4F, 0.36F, -0.25F, 0.45F, -0.65F, 3.1416F, -0.5F, 0.3F, -0.1F},
            {9, 0.66F, -0.12F, 2.4F, 0.2F, 0.58F, -0.3F, 1.05F, -1.15F, 3.1416F, -0.5F, 0.3F, -0.14F},
            at(GUARD, 15)},
        {
            GUARD,
            {5, 0.95F, 0.06F, -0.4F, 1.8F, -0.18F, -0.2F, 0.05F, -1.9F, 1.5708F, -0.05F, 0.35F, 0.05F},
            {8, -0.5F, 0.12F, 2.6F, 1.4F, -0.18F, 0.1F, 0.0F, 0.6F, 1.5708F, -0.55F, 0.3F, 0.08F},
            {10, -0.85F, 0.14F, 2.8F, 1.4F, -0.2F, 0.15F, -0.02F, 1.35F, 1.5708F, -0.55F, 0.3F, 0.08F},
            at(GUARD, 17)},
        {
            GUARD,
            {5, 0.5F, -0.16F, -1.5F, 1.2F, -0.3F, -0.12F, -0.1F, -0.12F, 1.5708F, 0.12F, 0.42F, 0.0F},
            {8, -0.15F, 0.3F, 6.0F, 2.0F, 0.0F, 0.0F, 0.26F, 0.0F, 1.5708F, -0.75F, 0.55F, 0.12F},
            {11, -0.15F, 0.3F, 6.0F, 2.0F, -0.04F, 0.0F, 0.22F, 0.0F, 1.5708F, -0.75F, 0.55F, 0.12F},
            at(GUARD, 18)}};

    /**
     * Blow {@code step} of his string, {@code age} ticks in. 4.1.37: his left hand comes onto the grip over the blow's first
     * {@link #TAKES} ticks and lets go over its last (his guard is one-handed), the blade where the keys point it all through.
     */
    public static RigPose strike(int step, double age) {
        float[][] keys = STRING[Math.floorMod(step, STRING.length)];
        return pose(AgentPose.keyed(keys, age), 1.0F, bothHands(keys[keys.length - 1][T], age));
    }

    /** 4.1.37: ticks his left hand takes to come onto the grip (and to let go of it). */
    static final double TAKES = 2.0;

    /** 4.1.37: how far onto the grip his left hand is, {@code age} ticks into a blow {@code total} long (1: both hands on it). */
    public static float bothHands(double total, double age) {
        return smooth(age / TAKES) * smooth((total - age) / TAKES);
    }

    /**
     * His guard (standing, or on top of his run: {@code run} 0..1 trails the blade out wider at his side, its point kept off the ground,
     * and leaves his legs to the run).
     */
    public static RigPose guard(float run) {
        float[] k = GUARD.clone();
        k[PITCH] += 0.06F * run;
        k[YAW] -= 0.06F * run;
        return pose(k, 1.0F - run, 0.0F);
    }

    /**
     * 4.1.37: the draw's end: the blade from where it left the case ({@link #PRESENT}, both hands on it) down into his guard (his right hand
     * alone), {@code lower} 0..1 of the way.
     */
    static RigPose presented(float lower) {
        float[] k = new float[GUARD.length];
        for (int c = 0; c < k.length; c++) {
            k[c] = PRESENT[c] + (GUARD[c] - PRESENT[c]) * lower;
        }
        return pose(k, 1.0F, 1.0F - lower);
    }

    /** {@code k}'s pose with both hands on the grip. */
    static RigPose pose(float[] k, float legs) {
        return pose(k, legs, 1.0F);
    }

    /**
     * 4.1.37: his right arm in his one-handed guard: hanging a little forward and out at his side (x - forward, z + out), so the blade
     * runs ahead of him along his right side and its rod back past his hip, clear of him.
     */
    static final double HANG_X = -0.3;
    static final double HANG_Z = 0.12;

    /**
     * {@code k}'s pose: his body, then his arms and his wrist, the blade along the key: {@code both} 1 with both hands on the grip, 0 in his
     * right hand alone at his side (the left arm loose), between the two on the way; his legs {@code legs} of the key's.
     */
    static RigPose pose(float[] k, float legs, float both) {
        RigPose p = new RigPose();
        p.twist = k[TWIST];
        p.lean = k[LEAN];
        p.lunge = k[LUNGE];
        p.drop = k[SINK];
        p.headX = k[HEAD] - 0.6F * k[LEAN];
        p.headY = -k[TWIST] * 0.7F;
        p.headHold = 0.5F;
        p.rightLegX = k[RIGHT_LEG] * legs;
        p.leftLegX = k[LEFT_LEG] * legs;
        p.rightLegZ = 0.06F * legs;
        p.leftLegZ = -0.06F * legs;
        p.legHold = legs;
        if (both >= 1.0F) {
            hands(p, k[HIGH], k[ROUND], k[PITCH], k[YAW], k[ROLL]);
            return p;
        }
        RigPose one = new RigPose();
        oneHand(one, k[PITCH], k[YAW], k[ROLL]);
        if (both > 0.0F) {
            RigPose two = new RigPose();
            hands(two, k[HIGH], k[ROUND], k[PITCH], k[YAW], k[ROLL]);
            float[] r = QuinqueGrip.blendTurn(one.rightArmX, one.rightArmY, one.rightArmZ, two.rightArmX, two.rightArmY, two.rightArmZ, both);
            float[] l = QuinqueGrip.blendTurn(one.leftArmX, one.leftArmY, one.leftArmZ, two.leftArmX, two.leftArmY, two.leftArmZ, both);
            // The blade kept where the key points it: the wrist worked out again for the blended arm.
            double[][] armR = RigPose.zyx(r[0], r[1], r[2]);
            float[] wr = QuinqueGrip.angles(RigPose.multiply(QuinqueGrip.transpose(armR), RigPose.multiply(frame(k[PITCH], k[YAW], k[ROLL]),
                QuinqueGrip.transpose(HELD))));
            one.rightArmX = r[0];
            one.rightArmY = r[1];
            one.rightArmZ = r[2];
            one.leftArmX = l[0];
            one.leftArmY = l[1];
            one.leftArmZ = l[2];
            one.weaponX = wr[0];
            one.weaponY = wr[1];
            one.weaponZ = wr[2];
        }
        p.rightArmX = one.rightArmX;
        p.rightArmY = one.rightArmY;
        p.rightArmZ = one.rightArmZ;
        p.leftArmX = one.leftArmX;
        p.leftArmY = one.leftArmY;
        p.leftArmZ = one.leftArmZ;
        p.weaponX = one.weaponX;
        p.weaponY = one.weaponY;
        p.weaponZ = one.weaponZ;
        p.armHold = 1.0F;
        p.rightArmHold = 1.0F;
        p.leftArmHold = 1.0F;
        return p;
    }

    /** 4.1.37: the blade's frame in the chest's (columns: along, toward its edge, through) for {@code pitch}, {@code yaw}, {@code roll}. */
    static double[][] frame(double pitch, double yaw, double roll) {
        double[] u = along(pitch, yaw);
        double[] down = {0.0, 1.0, 0.0};
        if (Math.abs(u[1]) > 0.97) {
            down = new double[] {0.0, 0.0, -Math.signum(u[1])};
        }
        double[] v = norm(sub(down, scale(u, dot(down, u))));
        v = norm(add(scale(v, Math.cos(roll)), scale(cross(u, v), Math.sin(roll))));
        double[] w = cross(u, v);
        return new double[][] {{u[0], v[0], w[0]}, {u[1], v[1], w[1]}, {u[2], v[2], w[2]}};
    }

    /** 4.1.37: the blade in his right hand alone, along {@code pitch} and {@code yaw}, edge rolled {@code roll}: his arm hanging, the left loose. */
    static void oneHand(RigPose p, double pitch, double yaw, double roll) {
        double[][] armR = RigPose.zyx((float)HANG_X, 0.0F, (float)HANG_Z);
        float[] ra = QuinqueGrip.angles(armR);
        float[] wr = QuinqueGrip.angles(RigPose.multiply(QuinqueGrip.transpose(armR), RigPose.multiply(frame(pitch, yaw, roll),
            QuinqueGrip.transpose(HELD))));
        p.rightArmX = ra[0];
        p.rightArmY = ra[1];
        p.rightArmZ = ra[2];
        p.leftArmX = 0.06F;
        p.leftArmY = 0.0F;
        p.leftArmZ = -0.08F;
        p.weaponX = wr[0];
        p.weaponY = wr[1];
        p.weaponZ = wr[2];
        p.armHold = 1.0F;
        p.rightArmHold = 1.0F;
        p.leftArmHold = 1.0F;
    }

    /** The blade's way along (pitch + up, yaw + to his left): a unit vector in the chest's frame. */
    public static double[] along(double pitch, double yaw) {
        return new double[] {Math.cos(pitch) * Math.sin(yaw), -Math.sin(pitch), -Math.cos(pitch) * Math.cos(yaw)};
    }

    /**
     * Both hands on the grip at {@code high} and {@code round}, the blade along {@code pitch} and {@code yaw}, its edge rolled {@code roll}
     * from down: his arms and his right wrist set on {@code p} (held: nothing of vanilla's own swing left in them).
     */
    public static void hands(RigPose p, double high, double round, double pitch, double yaw, double roll) {
        double out = com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
        double[] right = {-5.0 - out, 2.0, 0.0};
        double[] left = {5.0 + out, 2.0, 0.0};
        double[] u = along(pitch, yaw);
        // Its edge: square to it, as near straight down as it can be (forward when it points straight up or down), rolled about it.
        double[] down = {0.0, 1.0, 0.0};
        if (Math.abs(u[1]) > 0.97) {
            down = new double[] {0.0, 0.0, -Math.signum(u[1])};
        }
        double[] v = norm(sub(down, scale(u, dot(down, u))));
        v = norm(add(scale(v, Math.cos(roll)), scale(cross(u, v), Math.sin(roll))));
        double[] w = cross(u, v);
        double[] mid = {Math.sin(round) * Math.cos(high), -Math.sin(high), -Math.cos(round) * Math.cos(high)};
        // Where the hands go: their middle out along {@code mid} and shifted across, as far as puts each fist at the end of its own
        // straight arm (a few Newton steps on the two misses; where both can't, as near as they get).
        double r = 7.0;
        double across = 0.0;
        for (int i = 0; i < 8; i++) {
            double[] f = misses(mid, u, r, across, right, left);
            if (Math.abs(f[0]) + Math.abs(f[1]) < 1.0E-4) {
                break;
            }
            double h = 1.0E-3;
            double[] fr = misses(mid, u, r + h, across, right, left);
            double[] fa = misses(mid, u, r, across + h, right, left);
            double a11 = (fr[0] - f[0]) / h;
            double a12 = (fa[0] - f[0]) / h;
            double a21 = (fr[1] - f[1]) / h;
            double a22 = (fa[1] - f[1]) / h;
            double det = a11 * a22 - a12 * a21;
            if (Math.abs(det) < 1.0E-9) {
                break;
            }
            double dr = (a22 * f[0] - a12 * f[1]) / det;
            double da = (a11 * f[1] - a21 * f[0]) / det;
            r = Math.max(2.0, Math.min(14.0, r - Math.max(-3.0, Math.min(3.0, dr))));
            across = Math.max(-4.0, Math.min(4.0, across - Math.max(-2.0, Math.min(2.0, da))));
        }
        double[] target = fistAt(mid, u, r, across);
        double[][] armR = QuinqueGrip.between(norm(FIST_RIGHT), norm(sub(target, right)));
        double[] fist = add(right, QuinqueGrip.apply(armR, FIST_RIGHT));
        double[][] blade = {{u[0], v[0], w[0]}, {u[1], v[1], w[1]}, {u[2], v[2], w[2]}};
        double[][] wrist = RigPose.multiply(QuinqueGrip.transpose(armR), RigPose.multiply(blade, QuinqueGrip.transpose(HELD)));
        double[] grip = sub(fist, scale(u, GAP));
        double[][] armL = QuinqueGrip.between(norm(FIST_LEFT), norm(sub(grip, left)));
        float[] ra = QuinqueGrip.angles(armR);
        float[] l = QuinqueGrip.angles(armL);
        float[] wr = QuinqueGrip.angles(wrist);
        p.rightArmX = ra[0];
        p.rightArmY = ra[1];
        p.rightArmZ = ra[2];
        p.leftArmX = l[0];
        p.leftArmY = l[1];
        p.leftArmZ = l[2];
        p.weaponX = wr[0];
        p.weaponY = wr[1];
        p.weaponZ = wr[2];
        p.armHold = 1.0F;
        p.rightArmHold = 1.0F;
        p.leftArmHold = 1.0F;
    }

    /** The right fist for the hands' middle {@code r} out along {@code mid}, {@code across} to his left. */
    static double[] fistAt(double[] mid, double[] u, double r, double across) {
        return add(add(add(CENTRE, scale(mid, r)), new double[] {across, 0.0, 0.0}), scale(u, GAP * 0.5));
    }

    /** How far each fist would be from the end of its arm (pixels: + beyond it). */
    static double[] misses(double[] mid, double[] u, double r, double across, double[] right, double[] left) {
        double[] fist = fistAt(mid, u, r, across);
        double[] grip = sub(fist, scale(u, GAP));
        return new double[] {length(sub(fist, right)) - ARM, length(sub(grip, left)) - ARM};
    }

    /**
     * The blade's frame as drawn for {@code p} (the chest's frame, pixels): {fist, along, toward its edge} from the right arm's turn and the
     * wrist, as the renderer draws it (tests, previews).
     */
    public static double[][] drawn(RigPose p) {
        double out = com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
        double[][] arm = RigPose.zyx(p.rightArmX, p.rightArmY, p.rightArmZ);
        double[][] wrist = RigPose.zyx(p.weaponX, p.weaponY, p.weaponZ);
        double[][] m = RigPose.multiply(arm, RigPose.multiply(wrist, HELD));
        double[] fist = add(new double[] {-5.0 - out, 2.0, 0.0}, QuinqueGrip.apply(arm, FIST_RIGHT));
        return new double[][] {fist, {m[0][0], m[1][0], m[2][0]}, {m[0][1], m[1][1], m[2][1]}};
    }

    /** The blade's frame in the right arm's own (columns: along, toward its edge, through), the wrist's turn in it. */
    public static double[][] inArm(RigPose p) {
        return RigPose.multiply(RigPose.zyx(p.weaponX, p.weaponY, p.weaponZ), HELD);
    }

    /** A point of the chest's frame in the model's (pixels): turned by his twist, then leaned about his hips (tests). */
    public static double[] toModel(RigPose p, double[] chest) {
        double c = Math.cos(p.twist);
        double sn = Math.sin(p.twist);
        double x = c * chest[0] + sn * chest[2];
        double z = -sn * chest[0] + c * chest[2];
        float[] at = p.leanPosition((float)x, (float)chest[1], (float)z);
        return new double[] {at[0], at[1], at[2]};
    }

    /** The left fist as drawn for {@code p} (the chest's frame, pixels). */
    public static double[] leftFist(RigPose p) {
        double out = com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
        return add(new double[] {5.0 + out, 2.0, 0.0}, QuinqueGrip.apply(RigPose.zyx(p.leftArmX, p.leftArmY, p.leftArmZ), FIST_LEFT));
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    static double[] norm(double[] a) {
        double l = length(a);
        return l < 1.0E-12 ? new double[] {0.0, 1.0, 0.0} : scale(a, 1.0 / l);
    }

    /**
     * 4.1.29 (the owner: "pulls out a random briefcase from his back like a golden looking briefcase"; of the first try: "Why can't the blade
     * just, like the briefcase just splits apart open like a normal fucking briefcase and releases the weapon?"): {@code age} ticks after the
     * jacket is off: his right hand behind his back, the golden case in it ({@link AgentRules#REAPER_CASE_FROM}), swung out to his side, then
     * lifted before him, turning end on to where the blade will point ({@link #LIFTED}); it splits open on its hinge
     * ({@link AgentRules#REAPER_CASE_OPENS}), the blade slides out of it ({@link #extended}), his left hand comes onto the grip, the empty case
     * drops from his hand ({@link AgentRules#REAPER_CASE_DROPS}) to the ground.
     */
    public static RigPose draw(double age) {
        float[] k = AgentPose.keyed(DRAW_KEYS, Math.min(age, LIFT_FROM));
        RigPose p = new RigPose();
        p.twist = k[1];
        p.lean = k[2];
        p.rightArmX = k[3];
        p.rightArmY = k[4];
        p.rightArmZ = k[5];
        p.headY = k[6];
        p.headX = k[7];
        p.leftArmX = 0.05F;
        p.leftArmZ = -0.06F;
        p.rightArmHold = 1.0F;
        p.leftArmHold = 1.0F;
        p.armHold = 1.0F;
        p.headHold = 0.5F;
        float lift = smooth((age - LIFT_FROM) / (LIFTED - LIFT_FROM));
        float grip = smooth((age - GRIP_FROM) / (GRIP_AT - GRIP_FROM));
        if (lift <= 0.0F) {
            return p;
        }
        // 4.1.37: lifted to the blade held up before him as it leaves the case; once both hands have it, lowered into his guard.
        RigPose guard = presented(smooth((age - LOWER_FROM) / (LOWERED - LOWER_FROM)));
        RigPose out = new RigPose().add(p, 1.0F - lift).add(guard, lift);
        // His right arm and the turn in his fist his guard's once lifted; his left hand onto the grip once the blade is out.
        float[] right = QuinqueGrip.blendTurn(p.rightArmX, p.rightArmY, p.rightArmZ, guard.rightArmX, guard.rightArmY, guard.rightArmZ, lift);
        out.rightArmX = right[0];
        out.rightArmY = right[1];
        out.rightArmZ = right[2];
        out.weaponX = guard.weaponX;
        out.weaponY = guard.weaponY;
        out.weaponZ = guard.weaponZ;
        float[] left = QuinqueGrip.blendTurn(p.leftArmX, p.leftArmY, p.leftArmZ, guard.leftArmX, guard.leftArmY, guard.leftArmZ, grip);
        out.leftArmX = left[0];
        out.leftArmY = left[1];
        out.leftArmZ = left[2];
        out.rightArmHold = 1.0F;
        out.leftArmHold = 1.0F;
        out.armHold = 1.0F;
        return out;
    }

    /**
     * 4.1.29: the draw's own times (ticks after the jacket): the lift before him begins, ends; his left hand onto the grip. 4.1.37 (the
     * owner: "The taking out the quinque from the case also should take a little longer. It's pretty too fast"): each part about half as
     * slow again, and the blade lowered into his guard at the end ({@link #LOWER_FROM} .. {@link #LOWERED}).
     */
    public static final double LIFT_FROM = 22.0;
    public static final double LIFTED = 30.0;
    static final double GRIP_FROM = 40.0;
    static final double GRIP_AT = 46.0;
    public static final double LOWER_FROM = 50.0;
    public static final double LOWERED = 58.0;
    /** 4.1.29: how far the case's halves swing open on their hinge (radians), how far the blade slides out over (ticks from the opening). */
    public static final double CASE_OPEN = Math.toRadians(112.0);
    static final double SPLITS = 6.0;
    static final double SLIDE_FROM = 4.0;
    static final double SLIDES = 10.0;

    static float smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return (float)(t * t * (3.0 - 2.0 * t));
    }

    /** 4.1.29: how far the case's halves have swung open on their hinge (radians), {@code age} ticks after the jacket. */
    public static double caseOpen(double age) {
        return CASE_OPEN * smooth((age - AgentRules.REAPER_CASE_OPENS) / SPLITS);
    }

    /** 4.1.29: how far the blade has slid out of the case (0 inside .. 1 all out), {@code age} ticks after the jacket. */
    public static float extended(double age) {
        return smooth((age - AgentRules.REAPER_CASE_OPENS - SLIDE_FROM) / SLIDES);
    }

    /** 4.1.29: whether the case is in his hand ({@code age} ticks after the jacket): from when it comes into it until it drops. */
    public static boolean caseHeld(double age) {
        return age >= AgentRules.REAPER_CASE_FROM && age < AgentRules.REAPER_CASE_DROPS;
    }

    /**
     * 4.1.29: the case's turn in his right arm's frame (its own: x across, y down from the handle, z along): hanging plumb from his fist
     * until the lift, then turned end on along the blade (its depth along the blade, its length across the blade's flat, its thickness
     * through it: the blade's guard lies in it), the shortest way.
     */
    public static double[][] caseTurn(RigPose p, double age) {
        double[][] hanging = QuinqueGrip.transpose(RigPose.zyx(p.rightArmX, p.rightArmY, p.rightArmZ));
        double[][] along = RigPose.multiply(inArm(p), new double[][] {{0, 1, 0}, {0, 0, 1}, {1, 0, 0}});
        float lift = smooth((age - LIFT_FROM) / (LIFTED - LIFT_FROM));
        if (lift <= 0.0F) {
            return hanging;
        }
        if (lift >= 1.0F) {
            return along;
        }
        float[] a = QuinqueGrip.angles(hanging);
        float[] b = QuinqueGrip.angles(along);
        float[] m = QuinqueGrip.blendTurn(a[0], a[1], a[2], b[0], b[1], b[2], lift);
        return RigPose.zyx(m[0], m[1], m[2]);
    }

    /** 4.1.29: the dropped case's fall (pixels a tick, each tick), how long it lies before it sinks, how fast it sinks (pixels a tick). */
    public static final double CASE_FALL = 2.4;
    static final double LIES = 2.0;
    static final double SINKS = 2.2;

    /** 4.1.29: how far the dropped case has sunk into the ground, {@code dt} ticks after it dropped ({@code landed}: how far it fell). */
    public static double sunk(double dt, double landed) {
        double down = Math.sqrt(2.0 * Math.max(0.0, landed) / CASE_FALL);
        return Math.max(0.0, dt - down - LIES) * SINKS;
    }

    /**
     * 4.1.29: a point (or a way, {@code way}) of a half of the dropped case ({@code side}), {@code dt} ticks after it left his fist, in the model's
     * frame (pixels): open as it was, placed as his fist held it at the drop, tipped over as it falls (about its middle, forward), before its
     * fall down (the renderer's, to the ground).
     */
    public static double[] dropped(double[] v, int side, double dt, boolean way) {
        Release r = release();
        double[] p;
        if (way) {
            double[] o = ReaperBladeMesh.opened(new double[] {0.0, 0.0, 0.0}, side, CASE_OPEN);
            double[] t = ReaperBladeMesh.opened(v, side, CASE_OPEN);
            p = QuinqueGrip.apply(r.arm, QuinqueGrip.apply(r.turn, sub(t, o)));
            p = new double[] {r.cos * p[0] + r.sin * p[2], p[1], -r.sin * p[0] + r.cos * p[2]};
        } else {
            p = r.place(ReaperBladeMesh.opened(v, side, CASE_OPEN));
        }
        double tip = Math.min(1.0, Math.max(0.0, dt) / 4.0) * 1.35;
        double[] centre = way ? new double[] {0.0, 0.0, 0.0} : r.centre;
        double dy = p[1] - centre[1];
        double dz = p[2] - centre[2];
        return new double[] {p[0], centre[1] + Math.cos(tip) * dy - Math.sin(tip) * dz, centre[2] + Math.sin(tip) * dy + Math.cos(tip) * dz};
    }

    /** 4.1.29: his pose as the case leaves his fist, worked out once: the case's turn in his arm, his arm's, his twist, the case's middle. */
    static final class Release {
        final RigPose at;
        final double[][] turn;
        final double[][] arm;
        final double cos;
        final double sin;
        final double[] centre;

        Release() {
            double drop = AgentRules.REAPER_CASE_DROPS;
            this.at = draw(drop);
            this.turn = caseTurn(this.at, drop);
            this.arm = RigPose.zyx(this.at.rightArmX, this.at.rightArmY, this.at.rightArmZ);
            this.cos = Math.cos(this.at.twist);
            this.sin = Math.sin(this.at.twist);
            this.centre = this.place(new double[] {0.0, (ReaperBladeMesh.CASE_TOP + ReaperBladeMesh.CASE_BOTTOM) * 0.5, 0.0});
        }

        /** A point of the case's own frame in the model's (pixels), as his fist held it then. */
        double[] place(double[] c) {
            double out = com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
            double[] a = add(FIST_RIGHT, QuinqueGrip.apply(this.turn, c));
            return toModel(this.at, add(new double[] {-5.0 - out, 2.0, 0.0}, QuinqueGrip.apply(this.arm, a)));
        }
    }

    private static Release released;

    static synchronized Release release() {
        if (released == null) {
            released = new Release();
        }
        return released;
    }

    /**
     * 4.1.31 (the owner: "he breaks the weapon. The blade shatters at the end, and it's just broken. And he accepts his death a little before he
     * dies actually, and then he just falls on his knees and dies after the weapon shatters"): {@code age} ticks into his end: both hands
     * lower the blade, its point to the ground before him ({@link #END_LOW}); it shatters ({@link AgentRules#REAPER_END_SHATTER}), his arms
     * fall to his sides, the broken hilt in his right hand; he goes down on his knees ({@link AgentRules#REAPER_END_KNEELS}: his legs folded
     * back, his hips sunk till his feet are on the ground: he has no knees to bend), leans forward, bows his head; held as he dies.
     * 4.1.74 (the owner: "the blade slowly shatter, then he holds still for a bit then dies"): lowered more slowly; he holds it there, still,
     * the whole time it breaks away piece by piece ({@link AgentRules#REAPER_END_SHATTER} .. {@link AgentRules#REAPER_END_SHATTERED}); only
     * then his arms fall; he goes down slowly ({@link AgentRules#REAPER_END_KNEELING}) and holds still on his knees till he dies.
     * 4.1.75 (the owner: "make him actually lay on the floor face down like falling on his face and lay there a bit"): knelt and bowed a
     * moment, then he falls on his face ({@link #fall}) and lies there, still, till he dies.
     */
    public static RigPose end(double age) {
        float lower = smooth(age / (AgentRules.REAPER_END_SHATTER - 1.0));
        float[] k = new float[GUARD.length];
        for (int c = 0; c < k.length; c++) {
            k[c] = GUARD[c] + (END_LOW[c] - GUARD[c]) * lower;
        }
        RigPose p = pose(k, 1.0F - lower, lower); // 4.1.37: from his one-handed guard, both hands on it as he lowers it
        float drop = smooth((age - AgentRules.REAPER_END_SHATTERED) / 8.0);
        if (drop > 0.0F) {
            float[] right = QuinqueGrip.blendTurn(p.rightArmX, p.rightArmY, p.rightArmZ, -0.22F, 0.0F, 0.12F, drop);
            float[] left = QuinqueGrip.blendTurn(p.leftArmX, p.leftArmY, p.leftArmZ, 0.08F, 0.0F, -0.1F, drop);
            p.rightArmX = right[0];
            p.rightArmY = right[1];
            p.rightArmZ = right[2];
            p.leftArmX = left[0];
            p.leftArmY = left[1];
            p.leftArmZ = left[2];
            // The broken hilt held slack in his hanging hand, its stub ahead of it.
            p.weaponX = p.weaponX * (1.0F - drop);
            p.weaponY = p.weaponY * (1.0F - drop);
            p.weaponZ = p.weaponZ * (1.0F - drop);
        }
        float kneel = smooth((age - AgentRules.REAPER_END_KNEELS) / (double)AgentRules.REAPER_END_KNEELING);
        float bow = smooth((age - AgentRules.REAPER_END_KNEELS - 14.0) / 24.0); // 4.1.75: bowed before he falls (30 ticks before)
        p.rightLegX = p.rightLegX * (1.0F - kneel) + KNEEL * kneel;
        p.leftLegX = p.leftLegX * (1.0F - kneel) + KNEEL * kneel;
        p.rightLegZ *= 1.0F - kneel;
        p.leftLegZ *= 1.0F - kneel;
        p.rightLegZ += 0.08F * kneel;
        p.leftLegZ -= 0.08F * kneel;
        p.legHold = Math.max(p.legHold, kneel);
        p.drop = p.drop * (1.0F - kneel) + KNEEL_DROP * kneel;
        p.lunge *= 1.0F - kneel;
        p.twist *= 1.0F - kneel;
        p.lean = p.lean * (1.0F - kneel) + 0.2F * kneel + 0.1F * bow;
        p.headX = p.headX * (1.0F - kneel) + 0.25F * kneel + 0.45F * bow;
        p.headY *= 1.0F - kneel;
        p.headHold = Math.max(p.headHold, 0.8F * kneel);
        double falling = (age - AgentRules.REAPER_END_FALLS) / (double)AgentRules.REAPER_END_FALLING;
        if (falling > 0.0) {
            fall(p, Math.min(1.0, falling));
        }
        return p;
    }

    /** 4.1.75: face down: how far (radians) the whole of him is turned forward once he has fallen. */
    public static final float FACE_DOWN = (float)(Math.PI / 2.0);
    /**
     * 4.1.75: lying face down, how high (blocks) the middle of his body is: his chest a pixel over the ground, his head (deeper than his
     * chest) a pixel into it, his face to the ground.
     */
    public static final double LYING = 3.0 / 16.0;
    /** 4.1.75: his arms lying (a little back, out from his sides) and his head (in line with him, face to the ground). */
    static final float LIE_ARM_X = 0.06F;
    static final float LIE_ARM_Z = 0.2F;
    /** 4.1.75: lying, his wrist turned so the broken hilt lies flat beside him, its stub out to his right (HELD's way along his arm's front). */
    static final float LIE_WRIST = (float)(Math.PI / 2.0);

    /**
     * 4.1.75: {@code t} (0 to 1) of his fall on his face, from his knees: the whole of him turned forward faster and faster (nothing holds
     * him up: {@code t} squared) about his knees, which stay where they rest ({@link #knee}); his legs straighten out behind him, his back
     * in line with them, his head too, his arms limp along his sides; at the end he lies flat, face down ({@link #FACE_DOWN},
     * {@link #LYING}).
     */
    static void fall(RigPose p, double t) {
        double theta = FACE_DOWN * t * t;
        float straight = smooth(t);
        float legs = KNEEL * (1.0F - straight);
        p.rightLegX = legs;
        p.leftLegX = legs;
        p.rightLegZ *= 1.0F - straight;
        p.leftLegZ *= 1.0F - straight;
        p.lean *= 1.0F - straight;
        p.headX *= 1.0F - straight;
        p.headY *= 1.0F - straight;
        float[] right = QuinqueGrip.blendTurn(p.rightArmX, p.rightArmY, p.rightArmZ, LIE_ARM_X, 0.0F, LIE_ARM_Z, straight);
        float[] left = QuinqueGrip.blendTurn(p.leftArmX, p.leftArmY, p.leftArmZ, LIE_ARM_X, 0.0F, -LIE_ARM_Z, straight);
        p.rightArmX = right[0];
        p.rightArmY = right[1];
        p.rightArmZ = right[2];
        p.leftArmX = left[0];
        p.leftArmY = left[1];
        p.leftArmZ = left[2];
        // The broken hilt in his slack hand turned with it: its stub out to his right, flat on the ground beside him (held as it hung, it
        // would stand straight up out of him lying on his face).
        p.weaponX *= 1.0F - straight;
        p.weaponY = p.weaponY * (1.0F - straight) + LIE_WRIST * straight;
        p.weaponZ *= 1.0F - straight;
        p.flip = (float)-theta; // forward (x+ tips his head back)
        // His knees kept where they rest (their middle rising to the middle of his lying legs as he comes down flat): the drop and the
        // lunge that put them there, his body turned theta about the point BodyFrame.TURN_PIVOT up.
        double[] rest = knee(KNEEL, 0.0, KNEEL_DROP, 0.0);
        double y = rest[1] + (LYING - rest[1]) * t * t;
        double[] w = turned(kneeModel(legs), theta);
        p.drop = (float)(16.0 * (BodyFrame.TURN_PIVOT - (y - w[1])));
        p.lunge = (float)(16.0 * (rest[2] - w[2]));
    }

    /** 4.1.75: the middle of his right knee (halfway down his leg, swung back {@code legs}) in the model's frame (pixels). */
    static double[] kneeModel(float legs) {
        return new double[] {-1.9, 12.0 + 6.0 * Math.cos(legs), 6.0 * Math.sin(legs)};
    }

    /** 4.1.75: a model point (pixels) about the body's turning point (blocks: y up, z forward), turned forward {@code theta}. */
    static double[] turned(double[] q, double theta) {
        double y = 1.501 - q[1] / 16.0 - BodyFrame.TURN_PIVOT;
        double z = -q[2] / 16.0;
        return new double[] {q[0] / 16.0, y * Math.cos(theta) - z * Math.sin(theta), y * Math.sin(theta) + z * Math.cos(theta)};
    }

    /** 4.1.75: where his knee is (blocks: y up from the ground, z forward), his legs swung back {@code legs}, turned, dropped, lunged. */
    public static double[] knee(float legs, double theta, double drop, double lunge) {
        double[] w = turned(kneeModel(legs), theta);
        return new double[] {w[0], w[1] + BodyFrame.TURN_PIVOT - drop / 16.0, w[2] + lunge / 16.0};
    }

    /**
     * 4.1.75: a point (or, {@code way}, a direction) in the model's frame under pose {@code from}, given in the model's frame under pose
     * {@code to} at the same place in the world. AgentRenderer turns the whole model, its layers with it, by the pose's flip, spin and roll
     * about the point BodyFrame.TURN_PIVOT up, then drops and lunges it: what keeps the shards lying where they fell while he falls on his
     * face. A null pose: none of that.
     */
    public static double[] carried(RigPose from, RigPose to, double[] q, boolean way) {
        return local(to, place(from, q, way), way);
    }

    /** 4.1.75: a model point (pixels) of pose {@code p} in the world about the body (blocks: x to his left, y up from the ground, z forward). */
    public static double[] place(RigPose p, double[] q, boolean way) {
        float[] at = p == null ? new float[3] : p.transform("body", RigPose.POSITION, 0.0F, 0.0F, 0.0F);
        float[] rot = p == null ? new float[3] : p.transform("body", RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        double[] w = {q[0] / 16.0, way ? -q[1] / 16.0 : 1.501 - q[1] / 16.0 - BodyFrame.TURN_PIVOT, -q[2] / 16.0};
        w = about(w, 0, -rot[0]);
        w = about(w, 1, rot[1]);
        w = about(w, 2, -rot[2]);
        return way ? w : new double[] {w[0] - at[0], w[1] + BodyFrame.TURN_PIVOT + at[1], w[2] - at[2]};
    }

    /** 4.1.75: {@link #place} undone: a world point (or direction) about the body in the model's frame of pose {@code p}. */
    public static double[] local(RigPose p, double[] w, boolean way) {
        float[] at = p == null ? new float[3] : p.transform("body", RigPose.POSITION, 0.0F, 0.0F, 0.0F);
        float[] rot = p == null ? new float[3] : p.transform("body", RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        double[] r = way ? w.clone() : new double[] {w[0] + at[0], w[1] - BodyFrame.TURN_PIVOT - at[1], w[2] + at[2]};
        r = about(r, 2, rot[2]);
        r = about(r, 1, -rot[1]);
        r = about(r, 0, rot[0]);
        return new double[] {16.0 * r[0], way ? -16.0 * r[1] : 16.0 * (1.501 - BodyFrame.TURN_PIVOT - r[1]), -16.0 * r[2]};
    }

    /** {@code v} turned {@code angle} (right-handed) about axis {@code axis} (0 x, 1 y, 2 z). */
    static double[] about(double[] v, int axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return switch (axis) {
            case 0 -> new double[] {v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c};
            case 1 -> new double[] {v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c};
            default -> new double[] {v[0] * c - v[1] * s, v[0] * s + v[1] * c, v[2]};
        };
    }

    /** 4.1.75: his pose as his blade broke (the frame its shards are worked out in). */
    public static RigPose shatterPose() {
        return shatter().at;
    }

    /**
     * 4.1.31: on his knees: his legs swung back (radians) and his hips sunk (pixels): his thighs seen going down and back from the front, the
     * rest of each leg (his shins, folded under him) below the ground; and the blade lowered before the end, its point resting on the
     * ground ahead (a key).
     */
    public static final float KNEEL = 0.55F;
    public static final float KNEEL_DROP = 6.0F;
    static final float[] END_LOW = {0, 0.0F, 0.12F, 0.0F, 0.4F, -0.85F, -0.05F, -0.32F, -0.1F, 0.0F, -0.05F, 0.1F, 0.4F};

    /**
     * 4.1.31: shard {@code i} of the shattered blade, {@code dt} ticks after it broke, in the model's frame (pixels, before its fall to the
     * ground: the renderer's): {centre, along its length, across it} where the blade was as it broke, flown out and spun.
     */
    public static double[][] shard(int i, double dt) {
        Shatter s = shatter();
        com.hex.kagune.control.npc.ReaperBladeRules.Shard d = com.hex.kagune.control.npc.ReaperBladeRules.shard(i);
        double across = ReaperBladeMesh.drawnV(ReaperBladeMesh.edge(d.u()) * d.v() + ReaperBladeMesh.spine(d.u()) * (1.0 - d.v()));
        double[] at = add(add(s.fist, scale(s.along, d.u())), scale(s.edge, across));
        double[] flat = cross(s.along, s.edge);
        double[] start = toModel(s.at, at);
        double[] modelFlat = sub(toModel(s.at, add(at, flat)), start);
        double[] modelAlong = sub(toModel(s.at, add(at, s.along)), start);
        double t = Math.max(0.0, dt);
        double[] fly = add(add(scale(modelFlat, d.out() * t), scale(modelAlong, d.along() * t)), new double[] {0.0, -d.up() * t, 0.0});
        double spin = d.spin() * t;
        double[] length = norm(add(scale(modelAlong, Math.cos(spin)), scale(modelFlat, Math.sin(spin))));
        double[] width = norm(cross(length, norm(add(modelFlat, new double[] {0.0, 0.3 * Math.sin(spin * 1.7), 0.0}))));
        return new double[][] {add(start, fly), scale(length, d.size()), scale(width, d.size() * 0.38)};
    }

    /** 4.1.31: the unit normal of a sliver laid along {@code l} and {@code w}. */
    public static double[] normal(double[] l, double[] w) {
        return norm(cross(l, w));
    }

    /** 4.1.31: his pose as the blade breaks (worked out once): the blade's fist, its way along and to its edge (the chest's frame). */
    static final class Shatter {
        final RigPose at = end(AgentRules.REAPER_END_SHATTER);
        final double[][] drawn = drawn(this.at);
        final double[] fist = this.drawn[0];
        final double[] along = this.drawn[1];
        final double[] edge = this.drawn[2];
    }

    private static Shatter shattered;

    static synchronized Shatter shatter() {
        if (shattered == null) {
            shattered = new Shatter();
        }
        return shattered;
    }

    /**
     * The draw's keys: tick; twist (+ to his right), lean; the right arm's x (- forward), y, z (+ out); the head's turn and pitch. His hand
     * goes round behind his back at his waist and comes out at his side with the case.
     */
    static final float[][] DRAW_KEYS = {
        {0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
        {9, 0.32F, 0.08F, 0.72F, 0.0F, -0.42F, -0.25F, 0.05F},
        {14, 0.34F, 0.08F, 0.74F, 0.0F, -0.44F, -0.28F, 0.05F},
        {22, -0.08F, 0.0F, -0.2F, 0.0F, 0.62F, 0.15F, 0.0F}};
}
