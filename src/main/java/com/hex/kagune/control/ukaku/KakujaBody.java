package com.hex.kagune.control.ukaku;

/**
 * How Ukaku's body moves in his first kakuja (pure, tested): realistic locomotion added on top of vanilla's walk cycle,
 * in the shared body layer's channels (radians, PlayerModel signs: arm and leg x- swing forward, the right limbs' z+ swing
 * out, torso twist + turns the chest right; knee and elbow + flex; drop in model pixels, + down).
 *
 * <ul>
 *   <li><b>Walking and running</b>: the swing leg's knee folds high as it comes through and the thigh lifts, the stance
 *       leg stays soft; both arms pump with the elbows bent near a right angle; the chest counter-turns against the hips;
 *       the upper body leans into the run and the head stays level. The body rises and falls with every step by exactly
 *       the bend of the leg it stands on, so the planted foot stays on the ground.</li>
 *   <li><b>Jumping</b>: on the way up the legs tuck under him (the lead knee high) and the arms drive up; at the top and
 *       on the way down the legs reach down for the ground and the arms spread out for balance.</li>
 *   <li><b>Landing</b>: knees and hips fold to soak it up, the body sinks and leans in, then rises back, harder the
 *       faster he came down.</li>
 * </ul>
 */
public final class KakujaBody {
    /** One frame of offsets, in the body layer's channels. */
    public static final class Pose {
        public float lean;
        public float twist;
        public float drop;
        public float headX;
        public float rightArmX;
        public float rightArmZ;
        public float rightElbow;
        public float leftArmX;
        public float leftArmZ;
        public float leftElbow;
        public float rightLegX;
        public float rightLegZ;
        public float rightKnee;
        public float leftLegX;
        public float leftLegZ;
        public float leftKnee;
    }

    /**
     * What the body is doing.
     *
     * @param stride     vanilla's walk cycle phase (radians: its limb swing position times 0.6662)
     * @param swing      vanilla's walk amount (0 still .. 1, its limb swing amount)
     * @param run        0 walking pace or slower .. 1 a full sprint (from the real ground speed)
     * @param airborne   off the ground
     * @param vertical   vertical speed, blocks a tick (+ up)
     * @param sinceLand  ticks since he last touched down (large when long ago)
     * @param landSpeed  how fast he was falling when he touched down (blocks a tick, positive)
     */
    public record Motion(float stride, float swing, float run, boolean airborne, float vertical, float sinceLand, float landSpeed) {
        public static final Motion STILL = new Motion(0.0F, 0.0F, 0.0F, false, 0.0F, 1000.0F, 0.0F);
    }

    /** Vanilla's own leg swing amplitude (radians at a full walk amount), which these offsets ride on. */
    static final float VANILLA_LEG = 1.4F;
    /** Peak knee fold of the swing leg at a walk and at a sprint, and the stance leg's softness. */
    static final float WALK_KNEE = 0.55F;
    static final float RUN_KNEE = 1.55F;
    static final float STANCE_KNEE = 0.22F;
    /** Lean into a full sprint, and how bent the pumping elbows are. */
    static final float RUN_LEAN = 0.3F;
    static final float RUN_ELBOW = 1.35F;
    /** Ticks a landing takes to soak up. */
    public static final float LAND_TICKS = 9.0F;

    private KakujaBody() {
    }

    /** The locomotion pose for this frame. */
    public static Pose pose(Motion m) {
        Pose p = new Pose();
        if (m.airborne()) {
            jump(p, m);
        } else {
            stride(p, m);
            land(p, m);
        }
        return p;
    }

    /** Walking and running, in step with vanilla's leg swing. */
    static void stride(Pose p, Motion m) {
        float amount = clamp01(m.swing());
        if (amount <= 0.001F) {
            return;
        }
        float run = clamp01(m.run()) * amount;
        float phase = m.stride();
        float sin = (float)Math.sin(phase);
        float cos = (float)Math.cos(phase);
        // Vanilla swings the right leg by 1.4 cos(phase) (+ back); it comes forward while sin(phase) > 0.
        float knee = WALK_KNEE + (RUN_KNEE - WALK_KNEE) * run;
        float rightSwing = (float)Math.pow(Math.max(0.0F, sin), 1.4);
        float leftSwing = (float)Math.pow(Math.max(0.0F, -sin), 1.4);
        p.rightKnee = amount * (STANCE_KNEE * (1.0F - rightSwing) + knee * rightSwing);
        p.leftKnee = amount * (STANCE_KNEE * (1.0F - leftSwing) + knee * leftSwing);
        // Running, the thigh lifts high as the knee comes through.
        p.rightLegX = -0.45F * run * rightSwing;
        p.leftLegX = -0.45F * run * leftSwing;
        // Arms pump with the elbows bent, a little out from the body.
        p.rightElbow = amount * (0.25F + (RUN_ELBOW - 0.25F) * run);
        p.leftElbow = p.rightElbow;
        p.rightArmZ = 0.08F * run;
        p.leftArmZ = -0.08F * run;
        // The chest counter-turns: the right shoulder comes forward with the right arm (as the left leg does).
        p.twist = -0.14F * (0.4F + 0.6F * run) * amount * cos;
        // Lean into the run; the head stays level.
        p.lean = amount * (0.04F + RUN_LEAN * run);
        p.headX = -0.75F * p.lean;
        // Sink by the bend this adds to the leg he stands on, so that foot stays planted (the swing leg is off the ground).
        // Vanilla's own stiff swing is left as it is: only the knees' shortening of the leg counts.
        float rightVanilla = VANILLA_LEG * cos * amount;
        float leftVanilla = -rightVanilla;
        float rightDrop = legDrop(rightVanilla + p.rightLegX, p.rightKnee) - legDrop(rightVanilla, 0.0F);
        float leftDrop = legDrop(leftVanilla + p.leftLegX, p.leftKnee) - legDrop(leftVanilla, 0.0F);
        p.drop = Math.max(0.0F, Math.min(rightDrop, leftDrop));
    }

    /** In the air: tucked on the way up, reaching down and balancing on the way down. */
    static void jump(Pose p, Motion m) {
        float rising = clamp01(m.vertical() / 0.3F);
        float falling = clamp01(-m.vertical() / 0.45F);
        float floating = 1.0F - Math.max(rising, falling);
        float tuck = rising + 0.6F * floating;
        // Legs: the lead (right) knee drives up, the other trails; then both reach for the ground, a little apart.
        p.rightLegX = -0.75F * tuck - 0.2F * falling;
        p.leftLegX = -0.25F * tuck + 0.15F * falling;
        p.rightKnee = 1.35F * tuck + 0.25F * falling;
        p.leftKnee = 0.9F * tuck + 0.3F * falling;
        p.rightLegZ = 0.12F * falling;
        p.leftLegZ = -0.12F * falling;
        // Arms drive up on the way up, spread out on the way down.
        p.rightArmX = -1.1F * rising - 0.35F * falling;
        p.leftArmX = -0.8F * rising - 0.35F * falling;
        p.rightArmZ = 0.15F * rising + 0.85F * falling;
        p.leftArmZ = -0.15F * rising - 0.85F * falling;
        p.rightElbow = 0.6F * rising + 0.25F * falling;
        p.leftElbow = 0.7F * rising + 0.25F * falling;
        p.lean = 0.12F * rising + 0.06F * floating - 0.04F * falling;
        p.headX = -0.75F * p.lean - 0.1F * falling;
    }

    /** Touching down: fold and sink, then rise back, harder the faster he came down. */
    static void land(Pose p, Motion m) {
        float since = m.sinceLand();
        if (since < 0.0F || since >= LAND_TICKS || m.landSpeed() <= 0.12F) {
            return;
        }
        float hard = clamp01((m.landSpeed() - 0.12F) / 0.6F);
        // A quick fold (2 ticks), then a slower rise.
        float t = since / LAND_TICKS;
        float soak = since < 2.0F ? smooth(since / 2.0F) : 1.0F - smooth((since - 2.0F) / (LAND_TICKS - 2.0F));
        float k = soak * (0.35F + 0.65F * hard);
        p.rightLegX -= 0.55F * k;
        p.leftLegX -= 0.35F * k;
        p.rightKnee += 1.25F * k;
        p.leftKnee += 1.05F * k;
        p.lean += 0.32F * k;
        p.headX -= 0.24F * k;
        p.rightArmX -= 0.35F * k;
        p.leftArmX -= 0.35F * k;
        p.rightArmZ += 0.3F * k;
        p.leftArmZ -= 0.3F * k;
        p.rightElbow += 0.4F * k;
        p.leftElbow += 0.4F * k;
        p.drop = Math.max(p.drop, Math.min(legDrop(p.rightLegX, p.rightKnee), legDrop(p.leftLegX, p.leftKnee)));
        if (t >= 1.0F) {
            p.drop = 0.0F;
        }
    }

    /** How far (model pixels) a leg swung by {@code legX} with its knee bent by {@code knee} lifts its foot (RigPose.legDrop). */
    static float legDrop(float legX, float knee) {
        double thigh = 6.0 * Math.cos(legX);
        double shin = 6.0 * Math.cos(legX + knee);
        return (float)Math.max(0.0, 12.0 - thigh - shin);
    }

    static float clamp01(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }

    static float smooth(float value) {
        float t = clamp01(value);
        return t * t * (3.0F - 2.0F * t);
    }
}
