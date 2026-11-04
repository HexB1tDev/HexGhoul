package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.MoodKiss;

/**
 * 2.2.4 (pure): the kiss as the mood has it ({@link MoodKiss}), over the usual one ({@link EtoPose}): while they wait ({@code KISS}),
 * blush ({@code BLUSH}) and pull them in ({@code PULL}). Sweet is the usual, untouched; anything else is each mood's own body:
 * <ul>
 * <li>angry: a fist in their collar, the other hand clenched at the side, the head turned away, a foot tapping;</li>
 * <li>possessive: both arms round their neck at once, held tight, leaning hard;</li>
 * <li>giddy: hands clasped at the chest, bouncing on the toes, the head bobbing;</li>
 * <li>tender: both hands up to their face, slow, a gentle sway;</li>
 * <li>desperate: arms thrown round them, leaning their whole weight in, trembling;</li>
 * <li>sleepy: arms hanging, the head lolling, drooping in and out;</li>
 * <li>hungry: the head tipped for a bite, a hand at their jaw, the other on the hip;</li>
 * <li>teasing: hands behind the back, bobbing in and out, a sly tilt.</li>
 * </ul>
 */
public final class MoodKissPose {
    private MoodKissPose() {
    }

    /** {@code own} with the kiss's mood on it, or {@code own} unchanged if the kiss is sweet or this is no part of a kiss. */
    public static RigPose apply(RigPose own, MoodKiss.Style style, EtoGesture gesture, double age) {
        if (style == null || style == MoodKiss.Style.SWEET || gesture == null) {
            return own;
        }
        float t = (float)age;
        RigPose p = switch (gesture) {
            case KISS -> waiting(style, t, false);
            case BLUSH -> waiting(style, t, true);
            case PULL -> pulling(style, t);
            default -> null;
        };
        if (p == null) {
            return own;
        }
        return p.scale(EtoPose.ease(t / 6.0F));
    }

    /** Leaning in and waiting ({@code blush}: held a while, flushed). */
    static RigPose waiting(MoodKiss.Style style, float t, boolean blush) {
        RigPose p = new RigPose();
        switch (style) {
            case ANGRY -> {
                // A fist in their collar, the other clenched low, looking anywhere but at them; held a while, the face turns back to theirs.
                float lean = blush ? 0.52F : 0.46F;
                p.lean = lean;
                p.rightArmX = -1.6F;
                p.rightArmY = -0.28F;
                p.rightArmZ = 0.02F;
                p.leftArmX = 0.12F;
                p.leftArmZ = -0.1F;
                p.headX = -lean - 0.02F;
                p.headY = blush ? 0.3F * (1.0F - EtoPose.ease(t / 30.0F)) + 0.06F : 0.3F;
                p.headZ = -0.06F;
                p.rightLegX = blush ? 0.0F : -0.16F * (float)Math.max(0.0, Math.sin(t * 0.55));
            }
            case POSSESSIVE -> {
                // Both arms round their neck at once, gripping, pulling them down to her.
                float lean = blush ? 0.58F : 0.54F;
                p.lean = lean;
                p.rightArmX = -1.95F;
                p.leftArmX = -1.95F;
                p.rightArmY = -0.48F;
                p.leftArmY = 0.48F;
                p.rightArmZ = 0.02F;
                p.leftArmZ = -0.02F;
                p.headX = -lean - 0.06F;
                p.headZ = 0.12F;
            }
            case GIDDY -> {
                // Up on the toes, hands clasped at the chest, bouncing, the head bobbing side to side.
                float lean = blush ? EtoPose.BLUSH_LEAN : EtoPose.KISS_LEAN;
                p.lean = lean;
                p.rightArmX = -1.05F;
                p.leftArmX = -1.05F;
                p.rightArmY = 0.7F;
                p.leftArmY = -0.7F;
                p.rightArmZ = -0.3F;
                p.leftArmZ = 0.3F;
                p.headX = -lean - 0.04F;
                p.headZ = 0.16F * (float)Math.sin(t * 0.3);
                p.drop = -(blush ? 0.7F : 1.3F) * (float)Math.abs(Math.sin(t * 0.35));
            }
            case TENDER -> {
                // Both hands up to their face, slowly, swaying a little.
                float lean = blush ? EtoPose.BLUSH_LEAN : EtoPose.KISS_LEAN;
                float reach = EtoPose.ease(t / 18.0F);
                p.lean = lean;
                p.rightArmX = -(blush ? 1.85F : 1.75F) * reach;
                p.leftArmX = -(blush ? 1.85F : 1.75F) * reach;
                p.rightArmY = -0.4F * reach;
                p.leftArmY = 0.4F * reach;
                p.headX = -lean - 0.04F;
                p.headZ = 0.1F * (float)Math.sin(t * 0.05);
                p.roll = 0.02F * (float)Math.sin(t * 0.05);
            }
            case DESPERATE -> {
                // Thrown round them, holding on with the whole weight, shaking a little.
                float lean = 0.6F;
                p.lean = lean;
                p.rightArmX = -1.35F;
                p.leftArmX = -1.35F;
                p.rightArmZ = -0.6F;
                p.leftArmZ = 0.6F;
                p.headX = -lean - 0.02F;
                p.headZ = 0.18F;
                p.roll = 0.015F * (float)Math.sin(t * 1.3);
            }
            case SLEEPY -> {
                // Arms hanging, the head lolling, drooping in and catching themself.
                float lean = (blush ? 0.46F : 0.4F) + 0.07F * (float)Math.sin(t * 0.06);
                p.lean = lean;
                p.rightArmX = 0.1F;
                p.leftArmX = 0.1F;
                p.rightArmZ = 0.08F;
                p.leftArmZ = -0.08F;
                p.headX = -lean + 0.1F + 0.07F * (float)Math.sin(t * 0.05);
                p.headZ = 0.24F;
                p.roll = 0.05F * (float)Math.sin(t * 0.04);
            }
            case HUNGRY -> {
                // The head tipped as if to bite, a hand at their jaw, the other on the hip, swaying slowly like something stalking.
                float lean = blush ? 0.58F : 0.54F;
                p.lean = lean;
                p.rightArmX = -1.7F;
                p.rightArmY = -0.35F;
                p.rightArmZ = 0.02F;
                p.leftArmX = 0.2F;
                p.leftArmZ = -0.45F;
                p.headX = -lean - 0.02F;
                p.headZ = -0.3F;
                p.roll = 0.03F * (float)Math.sin(t * 0.07);
            }
            case TEASING -> {
                // Hands behind the back, bobbing in and out of reach, a sly tilt.
                float lean = 0.38F + 0.12F * (float)Math.sin(t * 0.15);
                p.lean = lean;
                p.rightArmX = 0.55F;
                p.leftArmX = 0.55F;
                p.rightArmZ = -0.22F;
                p.leftArmZ = 0.22F;
                p.headX = -lean - 0.04F;
                p.headZ = 0.24F;
                p.headY = -0.14F * (float)Math.sin(t * 0.1);
                p.roll = -0.04F;
            }
            default -> {
                return null;
            }
        }
        return p;
    }

    /** Held long enough, pulling them in: the usual arms round their neck, each mood its own way. */
    static RigPose pulling(MoodKiss.Style style, float t) {
        RigPose p = new RigPose();
        p.lean = EtoPose.PULL_LEAN;
        p.rightArmX = -1.95F;
        p.leftArmX = -1.95F;
        p.rightArmY = -0.32F;
        p.leftArmY = 0.32F;
        p.rightArmZ = 0.04F;
        p.leftArmZ = -0.04F;
        p.headX = -EtoPose.PULL_LEAN - 0.06F;
        p.headZ = 0.2F + 0.05F * (float)Math.sin(t * 0.1);
        switch (style) {
            case ANGRY -> {
                // Still a fist in their collar on one side.
                p.rightArmX = -1.6F;
                p.rightArmY = -0.25F;
                p.rightArmZ = 0.02F;
                p.headY = 0.08F;
            }
            case POSSESSIVE -> {
                // Tighter, and further in.
                p.rightArmY = -0.52F;
                p.leftArmY = 0.52F;
                p.lean += 0.05F;
                p.headX -= 0.05F;
            }
            case GIDDY -> p.drop = -0.6F * (float)Math.abs(Math.sin(t * 0.4));
            case TENDER -> {
                // Hands on their face, not round their neck.
                p.rightArmX = -1.85F;
                p.leftArmX = -1.85F;
                p.rightArmY = -0.42F;
                p.leftArmY = 0.42F;
                p.roll = 0.02F * (float)Math.sin(t * 0.05);
            }
            case DESPERATE -> {
                // Arms round them, all the weight in.
                p.rightArmX = -1.35F;
                p.leftArmX = -1.35F;
                p.rightArmY = 0.0F;
                p.leftArmY = 0.0F;
                p.rightArmZ = -0.6F;
                p.leftArmZ = 0.6F;
                p.lean = 0.6F;
                p.headX = -0.64F;
                p.roll = 0.015F * (float)Math.sin(t * 1.3);
            }
            case SLEEPY -> {
                p.rightArmX = -1.6F;
                p.leftArmX = -1.6F;
                p.headZ = 0.26F;
                p.roll = 0.04F * (float)Math.sin(t * 0.04);
            }
            case HUNGRY -> p.headZ = -0.32F;
            case TEASING -> p.headZ = 0.22F;
            default -> {
                return null;
            }
        }
        return p;
    }
}
