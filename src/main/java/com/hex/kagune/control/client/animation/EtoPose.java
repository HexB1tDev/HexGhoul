package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoHand;
import com.hex.kagune.control.npc.EtoRules;

/**
 * 1.3.0: Eto's body language (pure, previewed by {@code tools/ArtPreview} sheet {@code eto}): her goofy idle gestures, the kiss (hands
 * behind her back, her top half leaning in, on her toes), the blush, the hug, the cross arms of a girl left hanging, and her moves in a
 * fight (1.3.4: her fight is a Pale player's own moves, posed by their animator, EtoFight). Only what vanilla's player model can show: the head, the arms and the legs turned whole, and the whole body turned ({@code flip},
 * {@code roll}, {@code spin}) and lifted ({@code drop}, - up) by her renderer. A lean of her top half is the whole body tipped forward
 * ({@code flip} -) with the legs swung back under her by as much ({@code legX} the same), so only what is above them leans.
 */
public final class EtoPose {
    /**
     * How far her top half leans in for a kiss, and blushing (radians). 1.3.3: down to the height of a crouching player's face (the
     * shift held for a kiss crouches them), so she never stands over them.
     */
    static final float KISS_LEAN = 0.5F;
    static final float BLUSH_LEAN = 0.55F;
    static final float PULL_LEAN = 0.52F;

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
            // 1.3.2: her idle life, her clinging, the pull of a long kiss, the ring, a puppy, her laugh.
            case READ -> {
                // Both hands round an open book at her chest, head bowed over it; a page turned every couple of seconds.
                float turn = page(t, 46.0F);
                p.rightArmX = -0.85F - 0.12F * turn;
                p.leftArmX = -0.85F;
                p.rightArmY = -0.38F + 0.3F * turn;
                p.leftArmY = 0.38F;
                p.rightArmZ = -0.08F;
                p.leftArmZ = 0.08F;
                p.headX = 0.45F;
                p.headZ = 0.06F * (float)Math.sin(t * 0.05);
            }
            case WRITE -> {
                // The pocket book low in her left hand, the quill's small loops in her right, head down.
                p.leftArmX = -0.8F;
                p.leftArmY = 0.5F;
                p.leftArmZ = 0.1F;
                p.rightArmX = -0.95F + 0.09F * (float)Math.sin(t * 2.6);
                p.rightArmY = -0.45F + 0.16F * (float)Math.sin(t * 0.9);
                p.rightArmZ = -0.05F + 0.07F * (float)Math.cos(t * 2.6);
                p.headX = 0.52F;
                p.headY = 0.08F;
                // A pause to think now and then: the quill to her lips.
                float think = page(t, 60.0F);
                p.rightArmX -= 0.7F * think;
                p.rightArmY += 0.55F * think;
                p.headX -= 0.4F * think;
            }
            case SWEEP -> {
                // Bent a little over the brush, sweeping side to side.
                float stroke = (float)Math.sin(t * 0.3);
                p.lean = 0.16F;
                p.rightArmX = -0.62F;
                p.leftArmX = -0.5F;
                p.rightArmY = -0.25F + 0.45F * stroke;
                p.leftArmY = 0.35F + 0.45F * stroke;
                p.rightArmZ = -0.12F;
                p.leftArmZ = 0.12F;
                p.headX = 0.3F;
                p.headY = 0.12F * stroke;
                p.roll = 0.03F * stroke;
            }
            case HAIR -> {
                // A lock of hair round her finger, head tipped into it.
                p.rightArmX = -2.45F;
                p.rightArmY = 0.18F;
                p.rightArmZ = -0.12F + 0.14F * (float)Math.sin(t * 0.55);
                p.headZ = -0.24F;
                p.headX = 0.06F;
                p.leftArmZ = -0.1F;
                p.roll = -0.03F;
            }
            case HUM -> {
                // Hands behind her back, swaying to a song only she knows.
                float sway = (float)Math.sin(t * 0.17);
                p.rightArmX = 0.45F;
                p.leftArmX = 0.45F;
                p.rightArmZ = -0.2F;
                p.leftArmZ = 0.2F;
                p.roll = 0.07F * sway;
                p.headZ = 0.2F * (float)Math.sin(t * 0.17 + 0.5);
                p.headX = -0.06F;
                p.drop = 0.25F * (float)Math.abs(Math.sin(t * 0.34));
            }
            case LOOK -> {
                // A hand shading her eyes, looking one way, then the other.
                p.rightArmX = -2.2F;
                p.rightArmY = -0.55F;
                p.rightArmZ = -0.12F;
                p.headY = 0.65F * (float)Math.sin(t * 0.1);
                p.headX = -0.06F;
                p.leftArmZ = -0.2F;
            }
            case DANCE -> {
                // Arms up in turn, a bounce on every beat.
                int beat = (int)Math.floor(t / 8.0F) % 4;
                float up = (float)Math.abs(Math.sin(t * Math.PI / 8.0));
                p.rightArmX = beat % 2 == 0 ? -2.7F * up : -0.3F;
                p.leftArmX = beat % 2 == 1 ? -2.7F * up : -0.3F;
                p.rightArmZ = 0.25F;
                p.leftArmZ = -0.25F;
                p.drop = -1.1F * up;
                p.roll = 0.08F * (float)Math.sin(t * Math.PI / 16.0);
                p.headZ = beat % 2 == 0 ? 0.2F : -0.2F;
                p.rightLegX = beat == 1 ? -0.35F * up : 0.0F;
                p.leftLegX = beat == 3 ? -0.35F * up : 0.0F;
            }
            case POUT -> {
                // Arms crossed, chin down, turned half away. No mark: just a sulk.
                p.rightArmX = -1.05F;
                p.leftArmX = -1.05F;
                p.rightArmZ = -0.85F;
                p.leftArmZ = 0.85F;
                p.rightArmY = 0.3F;
                p.leftArmY = -0.3F;
                p.headX = 0.28F;
                p.headY = -0.5F;
                p.roll = 0.02F * (float)Math.sin(t * 0.09);
            }
            case YAWN -> {
                // A hand to her mouth, head back; then a shudder of a stretch.
                float yawn = (float)Math.sin(Math.min(1.0, t / 24.0) * Math.PI);
                p.rightArmX = -1.85F;
                p.rightArmY = 0.55F;
                p.rightArmZ = -0.2F;
                p.headX = -0.32F * yawn;
                p.leftArmX = -2.4F * (t > 20.0F ? ease((t - 20.0F) / 8.0F) : 0.0F);
                p.leftArmZ = -0.25F;
                p.drop = -0.4F * yawn;
            }
            case LICK -> {
                // A finger at her lips, head tipped, the other hand on her hip.
                p.rightArmX = -1.75F;
                p.rightArmY = 0.5F;
                p.rightArmZ = -0.25F;
                p.headZ = 0.2F;
                p.headX = -0.08F;
                p.leftArmZ = -0.45F;
                p.leftArmX = 0.2F;
            }
            case CLING -> {
                // Arms wound round her partner's arm, cheek on their shoulder.
                p.rightArmX = -0.95F;
                p.leftArmX = -1.15F;
                p.rightArmY = -0.6F;
                p.leftArmY = 0.5F;
                p.rightArmZ = -0.25F;
                p.leftArmZ = 0.3F;
                p.headZ = 0.32F;
                p.headX = 0.1F;
                p.lean = 0.08F;
                p.roll = -0.04F + 0.015F * (float)Math.sin(t * 0.08);
            }
            case PULL -> {
                // 1.3.3: the kiss held: her arms over their shoulders (level with them, a little round their neck), pulling them in, her face up to theirs.
                p.lean = PULL_LEAN;
                p.rightArmX = -1.95F;
                p.leftArmX = -1.95F;
                p.rightArmY = -0.32F;
                p.leftArmY = 0.32F;
                p.rightArmZ = 0.04F;
                p.leftArmZ = -0.04F;
                p.headX = -PULL_LEAN - 0.06F;
                p.headZ = 0.2F + 0.05F * (float)Math.sin(t * 0.1);
            }
            case RING -> {
                if (t < 30.0F) {
                    // Hands over her mouth.
                    p.rightArmX = -1.85F;
                    p.leftArmX = -1.85F;
                    p.rightArmY = 0.55F;
                    p.leftArmY = -0.55F;
                    p.rightArmZ = -0.2F;
                    p.leftArmZ = 0.2F;
                    p.headX = 0.12F;
                } else {
                    // Then she jumps.
                    float hop = (float)Math.sin(Math.min(1.0, (t - 30.0) / 14.0) * Math.PI);
                    p.rightArmX = -2.8F * hop;
                    p.leftArmX = -2.8F * hop;
                    p.rightArmZ = 0.3F;
                    p.leftArmZ = -0.3F;
                    p.drop = -3.0F * hop;
                    p.headX = -0.25F * hop;
                }
            }
            case PET -> {
                // Something small cuddled to her chest, rocked side to side.
                float rock = (float)Math.sin(t * 0.14);
                p.rightArmX = -1.0F;
                p.leftArmX = -1.0F;
                p.rightArmY = -0.75F;
                p.leftArmY = 0.75F;
                p.headX = 0.35F;
                p.headZ = 0.15F * rock;
                p.roll = 0.05F * rock;
            }
            case HAND -> {
                // 1.3.3: her left hand out to the one at her left, holding theirs, a little shy. 1.3.4: just out from her side, to meet theirs
                // where it hangs (EtoHand), not reaching past it.
                p.leftArmZ = -EtoHand.ARM;
                p.leftArmX = -0.12F;
                p.leftArmY = 0.1F;
                p.headX = 0.08F;
                p.rightArmZ = 0.06F;
            }
            case LAUGH -> {
                // Head back, arms wide, shaking with it.
                float shake = (float)Math.sin(t * 2.2);
                p.headX = -0.5F + 0.05F * shake;
                p.rightArmZ = 1.1F;
                p.leftArmZ = -1.1F;
                p.rightArmX = -0.4F;
                p.leftArmX = -0.4F;
                p.drop = 0.3F * shake;
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
        // 1.3.1: bent at the hips, her feet flat where she stands (tipping her whole body over lifted her feet off the ground). 1.3.3: her
        // face level with theirs (the head turned back up by the lean, a touch more).
        p.lean = lean;
        p.headX = -lean - 0.04F;
        p.headZ = 0.06F * (float)Math.sin(t * 0.12);
    }

    /** A short motion once every {@code every} ticks (a page turned, a pause to think): 0, up to 1 and back over 10 ticks. */
    static float page(float t, float every) {
        float into = t % every;
        return into > every - 10.0F ? (float)Math.sin((into - (every - 10.0F)) / 10.0F * Math.PI) : 0.0F;
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
