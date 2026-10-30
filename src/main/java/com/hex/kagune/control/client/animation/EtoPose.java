package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoRules;

/**
 * 1.3.0: Eto's body language (pure, previewed by {@code tools/ArtPreview} sheet {@code eto}): her goofy idle gestures, the kiss (hands
 * behind her back, her top half leaning in, on her toes), the blush, the hug, the cross arms of a girl left hanging, and her moves in a
 * fight. Only what vanilla's player model can show: the head, the arms and the legs turned whole, and the whole body turned ({@code flip},
 * {@code roll}, {@code spin}) and lifted ({@code drop}, - up) by her renderer. A lean of her top half is the whole body tipped forward
 * ({@code flip} -) with the legs swung back under her by as much ({@code legX} the same), so only what is above them leans.
 */
public final class EtoPose {
    /** How far her top half leans in for a kiss, and blushing (radians). */
    static final float KISS_LEAN = 0.34F;
    static final float BLUSH_LEAN = 0.4F;

    private EtoPose() {
    }

    /** Her pose for {@code gesture} {@code age} ticks in, or null for none. */
    public static RigPose gesture(EtoGesture gesture, double age) {
        if (gesture == null || gesture == EtoGesture.NONE) {
            return null;
        }
        RigPose p = new RigPose();
        float t = (float)age;
        float in = gesture.held() ? ease(t / 6.0F) : envelope(t, gesture.ticks, 5.0F, 6.0F);
        switch (gesture) {
            case WAVE -> {
                p.rightArmX = -2.75F;
                p.rightArmZ = 0.2F + 0.38F * (float)Math.sin(t * 0.62);
                p.headZ = 0.16F;
                p.headY = -0.12F;
            }
            case STRETCH -> {
                // Up on her toes, both arms high, head back: a big yawn.
                float reach = (float)Math.sin(Math.min(1.0, t / 30.0) * Math.PI * 0.5);
                p.rightArmX = -2.95F * reach;
                p.leftArmX = -2.95F * reach;
                p.rightArmZ = 0.22F;
                p.leftArmZ = -0.22F;
                p.headX = -0.38F * reach;
                p.drop = -0.8F * reach;
                p.roll = 0.05F * (float)Math.sin(t * 0.18);
            }
            case TWIRL -> {
                float k = ease(Math.min(1.0F, t / 24.0F));
                p.spin = (float)(Math.PI * 2.0) * k;
                p.rightArmZ = 1.15F;
                p.leftArmZ = -1.15F;
                p.headZ = 0.18F;
                p.drop = -1.2F * (float)Math.sin(Math.PI * k);
            }
            case HOP -> {
                p.drop = -3.0F * (float)Math.abs(Math.sin(t * Math.PI / 12.0));
                p.rightArmZ = 0.45F;
                p.leftArmZ = -0.45F;
                p.rightLegX = -0.25F * (float)Math.abs(Math.sin(t * Math.PI / 12.0));
                p.leftLegX = 0.25F * (float)Math.abs(Math.sin(t * Math.PI / 12.0));
            }
            case PEACE -> {
                // A peace sign by her face, head tipped to it.
                p.rightArmX = -2.35F;
                p.rightArmY = -0.45F;
                p.rightArmZ = -0.35F;
                p.headZ = -0.24F;
                p.leftArmZ = -0.12F;
            }
            case LEAN -> {
                // Chin on her hand, hip out, bored and cute.
                p.rightArmX = -1.95F;
                p.rightArmY = 0.55F;
                p.rightArmZ = -0.25F;
                p.headZ = 0.28F;
                p.headX = 0.12F;
                p.roll = -0.06F;
                p.leftArmZ = -0.35F;
                p.leftArmX = 0.15F;
            }
            case GIGGLE -> {
                // Hands over her mouth, shoulders shaking; then a shrug.
                float shake = (float)Math.sin(t * 1.7);
                if (t < 22.0F) {
                    p.rightArmX = -1.85F;
                    p.leftArmX = -1.85F;
                    p.rightArmY = 0.55F;
                    p.leftArmY = -0.55F;
                    p.rightArmZ = -0.2F;
                    p.leftArmZ = 0.2F;
                    p.headX = 0.18F + 0.08F * shake;
                    p.drop = 0.35F * shake;
                } else {
                    p.rightArmZ = 0.55F;
                    p.leftArmZ = -0.55F;
                    p.rightArmX = -0.45F;
                    p.leftArmX = -0.45F;
                    p.headZ = 0.22F;
                    p.drop = -0.5F;
                }
            }
            case KISS -> kiss(p, KISS_LEAN, t);
            case BLUSH -> {
                kiss(p, BLUSH_LEAN, t);
                p.headZ = 0.12F * (float)Math.sin(t * 0.08);
            }
            case KISSED -> {
                // Hands clasped at her chest, a happy bounce.
                p.rightArmX = -1.05F;
                p.leftArmX = -1.05F;
                p.rightArmY = 0.7F;
                p.leftArmY = -0.7F;
                p.rightArmZ = -0.3F;
                p.leftArmZ = 0.3F;
                p.drop = t < 16.0F ? -2.4F * (float)Math.sin(Math.PI * t / 16.0) : 0.0F;
                p.headZ = 0.2F;
            }
            case HUG -> {
                p.rightArmX = -1.4F;
                p.leftArmX = -1.4F;
                p.rightArmZ = -0.55F;
                p.leftArmZ = 0.55F;
                p.lean = 0.12F;
                p.headX = -0.1F;
                p.headZ = 0.2F;
            }
            case ANGRY -> {
                // Arms crossed, nose up, looking away. Hmph.
                p.rightArmX = -1.05F;
                p.leftArmX = -1.05F;
                p.rightArmZ = -0.85F;
                p.leftArmZ = 0.85F;
                p.rightArmY = 0.3F;
                p.leftArmY = -0.3F;
                p.headY = 0.6F;
                p.headX = -0.18F;
                p.drop = t < 6.0F ? -1.0F * (float)Math.sin(Math.PI * t / 6.0) : 0.0F;
            }
            default -> {
            }
        }
        RigPose out = p.scale(in);
        if (gesture == EtoGesture.TWIRL) {
            // 1.3.1: the turn is never faded with the rest (scaled back to 0 it spun her backwards a whole turn as it ended).
            out.spin = p.spin;
        }
        return out;
    }

    /** Waiting for a kiss: hands behind her back, her top half leaning in by {@code lean}, chin up, on her toes, swaying a little. */
    private static void kiss(RigPose p, float lean, float t) {
        p.rightArmX = 0.55F;
        p.leftArmX = 0.55F;
        p.rightArmZ = -0.22F;
        p.leftArmZ = 0.22F;
        // 1.3.1: bent at the hips, her feet flat where she stands (tipping her whole body over lifted her feet off the ground).
        p.lean = lean;
        p.headX = -0.32F;
        p.headZ = 0.06F * (float)Math.sin(t * 0.12);
    }

    /** Her pose for {@code move} {@code age} ticks in, or null for none. */
    public static RigPose move(EtoRules.Move move, double age) {
        if (move == null || move == EtoRules.Move.NONE || move == EtoRules.Move.FLIP_FORWARD || move == EtoRules.Move.FLIP_BACK) {
            return null;
        }
        RigPose p = new RigPose();
        float t = (float)age;
        switch (move) {
            case STRIKE -> {
                float k = envelope(t, 6, 1.5F, 3.0F);
                p.rightArmX = -1.55F * k;
                p.rightArmY = 0.2F * k;
                p.flip = -0.1F * k;
                p.rightLegX = -0.1F * k;
                p.leftLegX = -0.1F * k;
            }
            case BLITZ -> {
                float k = envelope(t, EtoRules.BLITZ_TICKS, 1.0F, 2.0F);
                p.rightArmX = -1.65F * k;
                p.leftArmX = 0.6F * k;
                p.flip = -0.22F * k;
                p.rightLegX = -0.6F * k;
                p.leftLegX = 0.35F * k;
            }
            case PUNCH -> {
                if (t < EtoRules.PUNCH_WINDUP) {
                    float k = ease(t / EtoRules.PUNCH_WINDUP);
                    p.rightArmX = 0.95F * k;
                    p.rightArmZ = 0.35F * k;
                    p.leftArmX = -0.8F * k;
                    p.flip = 0.08F * k;
                    p.rightLegX = 0.08F * k;
                    p.leftLegX = 0.08F * k;
                } else {
                    float k = envelope(t - EtoRules.PUNCH_WINDUP, EtoRules.PUNCH_TICKS - EtoRules.PUNCH_WINDUP, 1.0F, 4.0F);
                    p.rightArmX = -1.72F * k;
                    p.leftArmX = 0.7F * k;
                    p.flip = -0.26F * k;
                    p.rightLegX = -0.5F * k;
                    p.leftLegX = 0.24F * k;
                }
            }
            case KICK -> {
                float k = envelope(t, EtoRules.KICK_TICKS, 2.0F, 3.0F);
                p.rightLegX = -1.65F * k;
                p.rightArmZ = 0.7F * k;
                p.leftArmZ = -0.7F * k;
                p.flip = 0.12F * k;
                p.leftLegX = 0.12F * k;
            }
            default -> {
                return null;
            }
        }
        return p;
    }

    /** 0 to 1 over {@code rise} ticks, held, back to 0 over the last {@code fall} of {@code length}. */
    static float envelope(float t, int length, float rise, float fall) {
        if (t < 0.0F || t >= length) {
            return 0.0F;
        }
        float up = ease(t / rise);
        float down = ease((length - t) / fall);
        return Math.min(up, down);
    }

    static float ease(float t) {
        float c = Math.max(0.0F, Math.min(1.0F, t));
        return c * c * (3.0F - 2.0F * c);
    }
}
