package com.hex.kagune.control.client.animation;

/**
 * Ambient idle stances (pure, tested): when a player with their kagune out stands still for a second, their body settles
 * into their path's stance, easing in slowly and out quickly the moment they move.
 *
 * <ul>
 *   <li><b>Ukaku, relaxed</b> (reference: Jotaro's stance): tall and leaning back a little, chin up, the right hand
 *       in the trouser pocket with the elbow out behind, the left forearm raised loosely in front at the waist, weight on
 *       the right leg with the left foot set out to the side.</li>
 *   <li><b>Ukaku's first kakuja</b> (reference: the masked, horned Ukaku of the anime): hunched forward, shoulders up,
 *       the arms hanging loose a little out from the body with the elbows soft, the chin down and the eyes up under the
 *       brow, feet planted wide with the left a little ahead, breathing slow and heavy, the head tilting now and then.</li>
 *   <li><b>Rinkaku, animalistic</b> (reference: Bakugo's fighting crouch): a wide, low stance on bent knees, the upper
 *       body hunched forward, shoulders up, both elbows bent out to the sides with the hands held forward like claws,
 *       the head pushed forward and up to watch, breathing hard.</li>
 * </ul>
 * Only Rinkaku's path is feral; Ukaku's is never animalistic, whatever his mastery.
 *
 * <p>6.0.2: where the limbs can't be drawn bent (3D Skin Layers, whose voxel sleeves and trousers are straight boxes on the
 * parts, or no bendy-lib; {@code BodyAnimations.limbsBend}), Rinkaku's crouch, all bent knees and elbows, came out as stiff
 * splayed legs in a broken lunge. He then stands in a feral stance built for straight limbs ({@link #rinkakuStraight}), and the
 * other stances drop their small bends with the body set so the soles stay on the ground.
 */
public final class IdlePose {
    /** Ticks a player must stand still before the pose begins. */
    public static final int STILL_TICKS = 20;
    /** Blend per tick: slow in (about 1.5 s), quick out (under half a second). */
    public static final float EASE_IN_PER_TICK = 1.0F / 30.0F;
    public static final float EASE_OUT_PER_TICK = 1.0F / 8.0F;

    public enum Style {
        NONE, UKAKU_RELAXED, UKAKU_KAKUJA, RINKAKU_FERAL,
        /** 6.1.0: Pale's base form. Its stance and gestures are {@link PaleIdle}'s; this style only carries the blend weight. */
        PALE_PLAYFUL
    }

    private IdlePose() {
    }

    /** The next blend value (0..1): rises while idle, falls otherwise. */
    public static float step(float blend, boolean idle) {
        return idle ? Math.min(1.0F, blend + EASE_IN_PER_TICK) : Math.max(0.0F, blend - EASE_OUT_PER_TICK);
    }

    /** The eased weight for a blend value: smooth at both ends, so the pose never snaps in or out. */
    public static float ease(float blend) {
        float t = Math.max(0.0F, Math.min(1.0F, blend));
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    /** The stance at {@code weight} (0 none .. 1 full), with a little life in it ({@code time} in seconds). */
    public static RigPose pose(Style style, float weight, double time) {
        return pose(style, weight, time, true);
    }

    /** The stance, {@code bends} false where the limbs can't be drawn bent: straight-limbed stances then. */
    public static RigPose pose(Style style, float weight, double time, boolean bends) {
        RigPose pose = new RigPose();
        if (style == Style.NONE || style == Style.PALE_PLAYFUL || weight <= 0.0F) {
            return pose;
        }
        if (style == Style.UKAKU_RELAXED) {
            ukaku(pose, time);
        } else if (style == Style.UKAKU_KAKUJA) {
            kakuja(pose, time);
        } else if (bends) {
            rinkaku(pose, time);
        } else {
            rinkakuStraight(pose, time);
        }
        if (!bends) {
            straighten(pose);
        }
        return pose.scale(Math.min(1.0F, weight));
    }

    /** No elbow or knee bends, and the body set (from the straight legs) so both soles stay on the ground. */
    static void straighten(RigPose p) {
        p.rightElbow = 0.0F;
        p.leftElbow = 0.0F;
        p.rightKnee = 0.0F;
        p.leftKnee = 0.0F;
        p.drop = Math.max(0.0F, Math.max(RigPose.legDrop(p.rightLegX, p.rightLegZ, 0.0F), RigPose.legDrop(p.leftLegX, p.leftLegZ, 0.0F)));
    }

    /** Jotaro's stance: unbothered. */
    private static void ukaku(RigPose p, double time) {
        float breath = (float)Math.sin(time * 1.6);
        float sway = (float)Math.sin(time * 0.55);
        // Upper body: a little back and toward the standing (right) leg, chest turned a touch to his left.
        p.lean = -0.09F + 0.012F * breath;
        p.side = -0.035F + 0.01F * sway;
        p.twist = -0.12F + 0.02F * sway;
        // The head goes back with the lean; lift the chin a little more and look slightly off to his right.
        p.headX = 0.02F - 0.01F * breath;
        p.headY = -0.16F + 0.03F * sway;
        p.headZ = 0.04F;
        // Right hand in the front trouser pocket: the upper arm hangs back and out, the forearm folds forward into the pocket.
        p.rightArmX = 0.44F;
        p.rightArmY = -0.19F;
        p.rightArmZ = -0.3F;
        p.rightElbow = 0.66F;
        // Left forearm up loosely in front at the waist, the hand a little out to the side (the "pointing" hand).
        p.leftArmX = 0.0F + 0.02F * breath;
        p.leftArmY = -0.55F;
        p.leftArmZ = -0.11F;
        p.leftElbow = 0.82F;
        // Weight on the right leg; the left set out to the side and slightly forward, its knee soft.
        p.rightLegZ = 0.02F;
        p.leftLegX = -0.08F;
        p.leftLegZ = -0.21F;
        p.leftKnee = 0.12F;
        p.drop = Math.max(RigPose.legDrop(p.rightLegX, p.rightLegZ, p.rightKnee), 0.0F);
    }

    /** The kakuja: a monster in no hurry. */
    private static void kakuja(RigPose p, double time) {
        float breath = (float)Math.sin(time * 1.9);
        float tilt = (float)Math.sin(time * 0.37);
        // Hunched forward, the shoulders up and heaving slowly.
        p.lean = 0.16F + 0.015F * breath;
        p.rightShoulderY = -0.3F - 0.08F * breath;
        p.leftShoulderY = -0.3F - 0.08F * breath;
        // Chin down, eyes up under the brow (the head rides the lean down; a little more), tilting now and then.
        p.headX = -0.08F - 0.01F * breath;
        p.headZ = 0.06F * tilt;
        p.headY = 0.05F * tilt;
        // Arms hanging loose, a little forward and out, elbows soft, the palms turned in.
        p.rightArmX = -0.12F + 0.02F * breath;
        p.rightArmY = -0.15F;
        p.rightArmZ = 0.16F;
        p.rightElbow = 0.3F;
        p.leftArmX = -0.1F + 0.02F * breath;
        p.leftArmY = 0.15F;
        p.leftArmZ = -0.16F;
        p.leftElbow = 0.34F;
        // Feet planted wide, the left a little ahead, knees soft; the body sinks so the soles stay on the ground.
        p.rightLegX = 0.06F;
        p.rightLegZ = 0.1F;
        p.rightKnee = 0.14F;
        p.leftLegX = -0.14F;
        p.leftLegZ = -0.1F;
        p.leftKnee = 0.2F;
        p.drop = Math.max(RigPose.legDrop(p.rightLegX, p.rightLegZ, p.rightKnee), RigPose.legDrop(p.leftLegX, p.leftLegZ, p.leftKnee));
    }

    /**
     * The feral stance on straight limbs (where they can't be drawn bent): hunched forward over wide-set feet, the left a little
     * ahead and the toes turned out, shoulders up and heaving, the head pushed up to watch, both arms reaching forward and out
     * with the hands open in front of him like claws, twitching now and then.
     */
    static void rinkakuStraight(RigPose p, double time) {
        float breath = (float)Math.sin(time * 3.4);
        float flex = (float)Math.pow(Math.max(0.0, Math.sin(time * 0.9)), 6.0);
        p.lean = 0.24F + 0.025F * breath;
        p.headX = -0.42F - 0.02F * breath;
        p.rightShoulderY = -0.55F - 0.25F * breath;
        p.leftShoulderY = -0.55F - 0.25F * breath;
        p.rightArmX = -0.5F - 0.03F * breath - 0.08F * flex;
        p.rightArmY = -0.35F;
        p.rightArmZ = 0.3F + 0.04F * flex;
        p.leftArmX = -0.5F - 0.03F * breath - 0.08F * flex;
        p.leftArmY = 0.35F;
        p.leftArmZ = -0.3F - 0.04F * flex;
        p.rightLegX = 0.08F;
        p.rightLegY = 0.3F;
        p.rightLegZ = 0.24F;
        p.leftLegX = -0.16F;
        p.leftLegY = -0.3F;
        p.leftLegZ = -0.24F;
    }

    /** Bakugo's fighting crouch: a predator about to spring. */
    private static void rinkaku(RigPose p, double time) {
        // Heavy, quick breathing in the shoulders; the hands flex now and then.
        float breath = (float)Math.sin(time * 3.4);
        float flex = (float)Math.pow(Math.max(0.0, Math.sin(time * 0.9)), 6.0);
        p.lean = 0.3F + 0.025F * breath;
        // Head forward and up, eyes on the prey (it rides the lean down, so lift it back and then some).
        p.headX = -0.5F - 0.02F * breath;
        p.rightShoulderY = -0.6F - 0.25F * breath;
        p.leftShoulderY = -0.6F - 0.25F * breath;
        // Elbows out to the sides, forearms bent forward, hands held out in front like claws.
        p.rightArmX = -0.19F - 0.03F * breath;
        p.rightArmY = -0.11F;
        p.rightArmZ = 0.56F;
        p.rightElbow = 1.21F + 0.15F * flex;
        p.leftArmX = -0.19F - 0.03F * breath;
        p.leftArmY = 0.11F;
        p.leftArmZ = -0.56F;
        p.leftElbow = 1.22F + 0.15F * flex;
        // Wide and low: feet well apart (the left a little ahead), knees bent and pushed out over the toes; the body sinks
        // so the soles stay planted.
        p.rightLegX = -0.35F;
        p.rightLegY = 0.37F;
        p.rightLegZ = 0.4F;
        p.rightKnee = 0.835F;
        p.leftLegX = -0.524F;
        p.leftLegY = -0.433F;
        p.leftLegZ = -0.311F;
        p.leftKnee = 0.747F;
        p.drop = 1.8F;
    }
}
