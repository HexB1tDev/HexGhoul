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

    /**
     * 1.3.14: the pose for {@code gesture}, {@code leads}: a man's way of holding someone (Ken's: {@link #led}) where he has one, else the
     * same as everyone's.
     */
    public static RigPose gesture(EtoGesture gesture, double age, boolean leads) {
        return gesture(gesture, age, leads ? 1 : 0, 0);
    }

    /**
     * 1.3.29: the pose for {@code gesture} in {@code style} (Persona.style: 0 Eto's, 1 Ken's {@link #led}, 2 Touka's {@link #touka}),
     * {@code variant} (0..2: which of its ways this time, so a kiss is not the same kiss every time).
     */
    public static RigPose gesture(EtoGesture gesture, double age, int style, int variant) {
        if (gesture != null) {
            RigPose own = style == 1 ? led(gesture, (float)age) : style == 2 ? touka(gesture, (float)age, variant) : null;
            if (own != null) {
                return own;
            }
        }
        return gesture(gesture, age);
    }

    /**
     * 1.3.29: Touka's, not Eto's (the owner: "she's more serious"): for a kiss she reaches up with ONE hand, to their head, their cheek or
     * the back of their neck (by {@code variant}), the other hand stiff at her side; a quick kiss is on the cheek, her head turned, a hand
     * on their shoulder; a kiss she steals turns their face to hers with a hand on their cheek, then she pulls back, arms crossed, looking
     * anywhere else; after a kiss she doesn't bounce, she crosses her arms and looks away; a hug is a proper one, stiff, her face turned
     * aside. Null for anything else (everyone's).
     */
    static RigPose touka(EtoGesture gesture, float t, int variant) {
        RigPose p = new RigPose();
        float in = gesture.held() ? ease(t / 6.0F) : envelope(t, gesture.ticks, 5.0F, 6.0F);
        int v = Math.floorMod(variant, 3);
        switch (gesture) {
            case KISS -> {
                // 1.3.30 (the owner: no arm thrown forward the moment a kiss is asked): waiting for it she only leans in, eyes shut, her
                // arms as they are, loose at her sides.
                p.lean = 0.4F;
                p.headX = -0.46F;
                p.headZ = (v == 2 ? -0.1F : 0.08F) + 0.04F * (float)Math.sin(t * 0.1);
                p.rightArmX = 0.08F;
                p.leftArmX = 0.08F;
                p.rightArmZ = 0.05F;
                p.leftArmZ = -0.05F;
            }
            case BLUSH -> {
                // Kissed: only now does one hand come up to them (their head, their cheek, the back of their neck by the variant), over
                // a moment; the lean she already had is kept (never dropped and leant into again).
                toukaKissed(p, v, t, ease(t / 8.0F));
                return p;
            }
            case PULL -> {
                // Pulling them in: the hand she had up goes behind their head, the other flat against their chest; still only the one
                // reaching up. From the kiss she was in, never from nothing.
                float k = ease(t / 8.0F);
                toukaKissed(p, v, t, 1.0F - k);
                p.lean = 0.5F;
                p.headX = -0.56F;
                p.headZ = 0.16F + 0.05F * (float)Math.sin(t * 0.09);
                p.rightArmX += -2.4F * k;
                p.rightArmY += -0.5F * k;
                p.rightArmZ += 0.1F * k;
                p.leftArmX = 0.12F + (-1.25F - 0.12F) * k;
                p.leftArmY = 0.2F * k;
                p.leftArmZ = -0.06F + 0.01F * k;
                p.rightArmHold = 1.0F;
                p.leftArmHold = Math.max(k, 0.0F);
                return p;
            }
            case PECK -> {
                float close = (float)Math.sin(Math.min(1.0, t / (double)EtoGesture.PECK.ticks) * Math.PI);
                p.lean = 0.36F * close;
                p.headX = -0.2F * close;
                p.headY = (v == 1 ? -0.55F : 0.55F) * close; // to their cheek, not their lips
                p.drop = -0.6F * close;
                p.rightArmX = -1.35F;
                p.rightArmY = -0.3F;
                p.rightArmZ = 0.05F;
                p.leftArmX = 0.1F;
            }
            case STEAL -> {
                float steal = (float)Math.min(1.0, t / 8.0);
                float back = t < 26.0F ? 0.0F : ease((t - 26.0F) / 10.0F);
                float kiss = steal * (1.0F - back);
                p.lean = 0.46F * kiss;
                p.headX = -0.5F * kiss + 0.18F * back;
                p.drop = -0.7F * kiss;
                p.rightArmX = -1.95F * kiss + -1.05F * back;
                p.rightArmY = -0.45F * kiss + -0.85F * back;
                p.rightArmZ = 0.08F * kiss + 0.85F * back;
                p.leftArmX = 0.12F * kiss + -1.05F * back;
                p.leftArmY = 0.85F * back;
                p.leftArmZ = -0.85F * back;
                p.headY = 0.6F * back;
                p.rightArmHold = 1.0F;
                p.leftArmHold = 1.0F;
                return p; // its own envelope (no fade: the cross is held to the end)
            }
            case KISSED -> {
                // Arms crossed, chin tucked, looking away. Flustered, not giddy.
                p.rightArmX = -1.05F;
                p.leftArmX = -1.05F;
                p.rightArmZ = -0.85F;
                p.leftArmZ = 0.85F;
                p.rightArmY = 0.3F;
                p.leftArmY = -0.3F;
                p.headY = v == 1 ? -0.55F : 0.55F;
                p.headX = 0.18F;
            }
            case HUG -> {
                // A proper hug, stiff; her face turned aside so they can't see it.
                p.rightArmX = -1.45F;
                p.leftArmX = -1.45F;
                p.rightArmZ = -0.5F;
                p.leftArmZ = 0.5F;
                p.lean = 0.08F;
                p.headY = v == 2 ? 0.5F : -0.5F;
                p.headX = 0.1F;
            }
            default -> {
                return null;
            }
        }
        return p.scale(in);
    }

    /**
     * 1.3.30: Touka kissed and holding it: leant in (fully, as she was), her one hand {@code up} of the way to them (0 at her side, 1 there:
     * their head, cheek or neck by {@code v}), the other stiff at her side.
     */
    private static void toukaKissed(RigPose p, int v, float t, float up) {
        p.lean = 0.5F;
        p.headX = -0.56F;
        p.headZ = (v == 2 ? -0.1F : 0.08F) + 0.04F * (float)Math.sin(t * 0.1);
        float x = v == 0 ? -2.55F : v == 1 ? -1.95F : -2.25F;
        float y = v == 0 ? -0.2F : v == 1 ? -0.42F : -0.6F;
        p.rightArmX = 0.08F + (x - 0.08F) * up;
        p.rightArmY = y * up;
        p.rightArmZ = 0.05F + 0.03F * up;
        p.rightArmHold = 1.0F;
        p.leftArmX = 0.12F;
        p.leftArmZ = -0.06F;
    }

    /**
     * 1.3.14: Ken's: he leans down to a kiss, a hand cupping their cheek and the other at their waist (held, a little further in); making
     * out, both arms round their waist pulling them in, head tilted; his own quick kiss on their forehead, hands on their shoulders; a hug
     * that wraps them up, his head resting on theirs; an arm round their shoulders where she would cling to an arm; and after a kiss, a
     * sheepish hand to the back of his neck. Null for anything else (the same as everyone's).
     */
    static RigPose led(EtoGesture gesture, float t) {
        RigPose p = new RigPose();
        float in = gesture.held() ? ease(t / 6.0F) : envelope(t, gesture.ticks, 5.0F, 6.0F);
        float sway = (float)Math.sin(t * 0.08);
        switch (gesture) {
            case KISS, BLUSH -> {
                float lean = gesture == EtoGesture.BLUSH ? 0.27F : 0.18F;
                p.lean = lean;
                p.headX = 0.22F;
                p.headZ = 0.12F;
                p.rightArmX = -1.75F;
                p.rightArmY = -0.38F;
                p.rightArmZ = 0.02F;
                p.leftArmX = -1.0F;
                p.leftArmY = 0.18F;
                p.leftArmZ = -0.04F;
                p.roll = 0.015F * sway;
            }
            case PULL -> {
                // Round their waist, not his own: the arms reach forward and stay a body's width apart, the hands at their back.
                p.lean = 0.16F;
                p.headX = 0.26F;
                p.headZ = 0.18F;
                p.rightArmX = -1.3F;
                p.leftArmX = -1.3F;
                p.rightArmY = -0.1F;
                p.leftArmY = 0.1F;
                p.rightArmZ = 0.05F;
                p.leftArmZ = -0.05F;
                p.roll = 0.02F * sway;
            }
            case PECK -> {
                float close = (float)Math.sin(Math.min(1.0, t / (double)EtoGesture.PECK.ticks) * Math.PI);
                p.rightArmX = -1.45F;
                p.leftArmX = -1.45F;
                p.rightArmY = -0.25F;
                p.leftArmY = 0.25F;
                p.lean = 0.33F * close;
                p.headX = 0.45F * close;
            }
            case HUG -> {
                // Round their shoulders, a body's width apart, his head resting on theirs.
                p.rightArmX = -1.5F;
                p.leftArmX = -1.5F;
                p.rightArmY = -0.14F;
                p.leftArmY = 0.14F;
                p.rightArmZ = 0.05F;
                p.leftArmZ = -0.05F;
                p.lean = 0.1F;
                p.headX = 0.32F;
                p.headZ = 0.25F;
            }
            case CLING -> {
                // His arm round their shoulders (they are at his left), his head tipped toward theirs.
                p.leftArmX = -0.55F;
                p.leftArmZ = -1.3F;
                p.leftArmY = 0.6F;
                p.headZ = -0.22F;
                p.headX = 0.08F;
                p.rightArmZ = 0.05F;
            }
            case KISSED -> {
                // A hand to the back of his neck, head down, a small smile: sheepish.
                p.rightArmX = -3.3F;
                p.rightArmY = 0.0F;
                p.rightArmZ = -0.55F;
                p.headX = 0.22F;
                p.headZ = -0.12F;
                p.leftArmZ = -0.06F;
            }
            default -> {
                return null;
            }
        }
        return p.scale(in);
    }

    /** Her pose for {@code gesture} {@code age} ticks in, or null for none. */
    /**
     * 1.3.30: a child held in the arms, the whole time it is held: the cradle (PET's arms) at full, easing in over its first moments once it
     * is picked up ({@code since}: ticks since; anything long ago is held), rocked on its own clock ({@code clock}).
     */
    public static RigPose cradle(double since, double clock) {
        RigPose p = new RigPose();
        float rock = (float)Math.sin(clock * 0.14);
        p.rightArmX = -1.0F;
        p.leftArmX = -1.0F;
        p.rightArmY = -0.75F;
        p.leftArmY = 0.75F;
        p.headX = 0.35F;
        p.headZ = 0.15F * rock;
        p.roll = 0.05F * rock;
        float in = since >= 5.0 || !Double.isFinite(since) ? 1.0F : ease((float)Math.max(0.0, since) / 5.0F);
        return p.scale(in);
    }

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
                // Chin on her hand, hip out, bored and cute. (2.0: the arm turned in to the face; it was turned out, an arm held out sideways.)
                p.rightArmX = -2.0F;
                p.rightArmY = -0.55F;
                p.rightArmZ = 0.1F;
                p.headZ = 0.28F;
                p.headX = 0.12F;
                p.roll = -0.06F;
                p.leftArmZ = -0.35F;
                p.leftArmX = 0.15F;
            }
            case GIGGLE -> {
                // A hand over her mouth, the other across her tummy, shoulders shaking; then a shrug. (2.0: both arms were turned out, held
                // straight ahead like a sleepwalker's: a stiff arm can't fold to the mouth, so one turns in to it, the other lies across.)
                float shake = (float)Math.sin(t * 1.7);
                if (t < 22.0F) {
                    p.rightArmX = -2.1F;
                    p.leftArmX = -0.55F;
                    p.rightArmY = -0.55F;
                    p.leftArmY = 0.45F;
                    p.rightArmZ = 0.0F;
                    p.leftArmZ = 0.0F;
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
                // A hand to her mouth, head back; then a shudder of a stretch. (2.0: the hand turned in to the mouth.)
                float yawn = (float)Math.sin(Math.min(1.0, t / 24.0) * Math.PI);
                p.rightArmX = -2.15F;
                p.rightArmY = -0.5F;
                p.rightArmZ = 0.0F;
                p.headX = -0.32F * yawn;
                p.leftArmX = -2.4F * (t > 20.0F ? ease((t - 20.0F) / 8.0F) : 0.0F);
                p.leftArmZ = -0.25F;
                p.drop = -0.4F * yawn;
            }
            case LICK -> {
                // A finger at her lips, head tipped, the other hand on her hip. (2.0: the hand turned in to the lips.)
                p.rightArmX = -2.0F;
                p.rightArmY = -0.45F;
                p.rightArmZ = 0.05F;
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
                    // Hands over her mouth. (2.0: turned in to it; they were turned out.)
                    p.rightArmX = -1.85F;
                    p.leftArmX = -1.85F;
                    p.rightArmY = -0.55F;
                    p.leftArmY = 0.55F;
                    p.rightArmZ = 0.2F;
                    p.leftArmZ = -0.2F;
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
                // where it hangs (EtoHand), not reaching past it. 1.3.5: held still as she walks (none of the walk's swing on it).
                p.leftArmZ = -EtoHand.ARM;
                p.leftArmX = -EtoHand.FORWARD;
                p.leftArmHold = 1.0F;
                p.leftArmY = 0.1F;
                p.headX = 0.08F;
                p.rightArmZ = 0.06F;
            }
            case LAUGH -> {
                // Head back, hands on her belly, shaking with it. (2.0: the arms were flung straight out, a stiff T on a mob's arms.)
                float shake = (float)Math.sin(t * 2.2);
                p.headX = -0.45F + 0.05F * shake;
                p.rightArmZ = 0.05F;
                p.leftArmZ = -0.05F;
                p.rightArmX = -0.75F + 0.06F * shake;
                p.leftArmX = -0.75F + 0.06F * shake;
                p.rightArmY = -0.45F;
                p.leftArmY = 0.45F;
                p.drop = 0.3F * shake;
            }
            // 1.3.14: Touka's (strict, pouty) and Ken's (quiet, a little eerie).
            case CROSS -> {
                // Arms folded tight, weight on one leg (a hip out), chin turned a little away; now and then a glance back.
                float glance = (float)Math.max(0.0, Math.sin(t * 0.07 - 1.2));
                folded(p);
                p.headY = 0.45F - 0.5F * glance;
                p.headX = -0.08F;
                p.roll = 0.05F;
                p.rightLegZ = 0.06F;
                p.leftLegZ = 0.02F;
            }
            case TAP -> {
                // Arms folded tight, one foot tapping, looking right at them, head tipped: well?
                folded(p);
                float tap = (float)Math.abs(Math.sin(t * Math.PI / 7.0));
                p.rightLegX = -0.22F * tap;
                p.legHold = 1.0F;
                p.headX = 0.1F;
                p.headZ = -0.12F;
                p.roll = 0.03F;
            }
            case HUFF -> {
                // Shoulders up on the breath in, dropped on the huff, head snapped away.
                float breath = t < 10.0F ? ease(t / 10.0F) : 1.0F - ease(Math.min(1.0F, (t - 10.0F) / 6.0F));
                folded(p);
                p.drop = -0.6F * breath;
                p.headY = t < 10.0F ? 0.0F : -0.7F * ease(Math.min(1.0F, (t - 10.0F) / 5.0F));
                p.headX = -0.15F;
            }
            case SIP -> {
                // The cup in both hands at her chest; up to her lips every few seconds.
                float sip = (float)Math.max(0.0, Math.sin(t * Math.PI / 50.0 - 0.6));
                p.rightArmX = -1.05F - 0.55F * sip;
                p.leftArmX = -0.95F - 0.4F * sip;
                p.rightArmY = -0.42F;
                p.leftArmY = 0.48F;
                p.rightArmZ = -0.05F;
                p.leftArmZ = 0.05F;
                p.headX = 0.22F - 0.32F * sip;
            }
            case WIPE -> {
                // The cup in her left hand, the cloth in her right going round and round it.
                float round = t * 0.42F;
                p.leftArmX = -0.95F;
                p.leftArmY = 0.45F;
                p.rightArmX = -1.0F + 0.12F * (float)Math.sin(round);
                p.rightArmY = -0.35F + 0.14F * (float)Math.cos(round);
                p.rightArmZ = -0.06F;
                p.headX = 0.38F;
            }
            case KICKS -> {
                // A practice kick: weight back, the right leg snapped up high, held a beat, down; arms out for balance; a shrug after.
                float kick = t < 6.0F ? ease(t / 6.0F) : t < 14.0F ? 1.0F : 1.0F - ease(Math.min(1.0F, (t - 14.0F) / 8.0F));
                p.rightLegX = -1.45F * kick;
                p.leftLegX = 0.12F * kick;
                p.rightArmZ = 0.55F * kick;
                p.leftArmZ = -0.75F * kick;
                p.rightArmX = 0.35F * kick;
                p.leftArmX = -0.35F * kick;
                p.lean = -0.18F * kick;
                p.legHold = kick;
                float shrug = t > 26.0F ? (float)Math.sin(Math.min(1.0, (t - 26.0) / 10.0) * Math.PI) : 0.0F;
                p.drop = -0.4F * shrug;
            }
            case SCOLD -> {
                // Leaning in, a finger wagged at them, the other arm stiff at her side.
                p.leftArmX = 0.2F;
                p.leftArmZ = -0.1F;
                p.leftArmY = 0.0F;
                p.rightArmX = -1.55F;
                p.rightArmY = 0.12F * (float)Math.sin(t * 1.3);
                p.rightArmZ = -0.08F;
                p.lean = 0.14F;
                p.headX = 0.08F;
                p.headZ = 0.06F * (float)Math.sin(t * 1.3);
            }
            case SHY -> {
                // Turned half away, arms crossed, cheeks pink, a glance back and away again.
                folded(p);
                float glance = (float)Math.max(0.0, Math.sin(t * 0.09 - 0.8));
                p.twist = -0.35F;
                p.headY = -0.55F + 0.6F * glance;
                p.headX = 0.2F;
                p.roll = -0.03F;
            }
            case CHIN -> {
                // A hand to the chin, the other arm across under it, head tipped: thinking (or hiding something).
                p.rightArmX = -2.05F;
                p.rightArmY = -0.6F;
                p.rightArmZ = 0.12F;
                p.leftArmX = -0.8F;
                p.leftArmY = 0.7F;
                p.leftArmZ = -0.05F;
                p.headZ = 0.12F;
                p.headX = 0.1F + 0.05F * (float)Math.sin(t * 0.06);
            }
            case CRACK -> crack(p, t);
            case PECK -> {
                // In fast, up on her toes, hands behind her back; a beat; back down.
                float close = (float)Math.sin(Math.min(1.0, t / (double)EtoGesture.PECK.ticks) * Math.PI);
                p.lean = 0.32F * close;
                p.headX = -0.22F * close;
                p.drop = -0.7F * close;
                p.rightArmX = 0.55F;
                p.leftArmX = 0.55F;
                p.rightArmZ = -0.22F;
                p.leftArmZ = 0.22F;
            }
            case WATER -> {
                // 1.3.29: the bucket held out over the flowers and tipped, slowly, side to side.
                float tip = (float)Math.sin(t * 0.09);
                p.rightArmX = -1.2F + 0.15F * tip;
                p.rightArmY = -0.2F;
                p.rightArmZ = 0.25F + 0.12F * tip;
                p.leftArmX = -0.4F;
                p.lean = 0.22F;
                p.headX = 0.42F;
                p.headY = 0.12F * tip;
            }
            case COOK -> {
                // 1.3.31: at the stove: the bowl held low in the left hand, the right stirring round and round, her head down to the fire.
                float round = (float)(t * 0.35);
                p.lean = 0.25F;
                p.headX = 0.45F;
                p.rightArmX = -1.05F + 0.18F * (float)Math.sin(round);
                p.rightArmY = -0.25F + 0.2F * (float)Math.cos(round);
                p.rightArmZ = 0.08F;
                p.leftArmX = -0.85F;
                p.leftArmY = 0.35F;
                p.leftArmZ = -0.05F;
            }
            case STOKE -> {
                // 2.4.9: bent to the stove's mouth, a lump of coal in the right hand pushed in, a poke, the hand drawn back; the left braced
                // on a knee; her head down to the fire.
                float push = Math.max(0.0F, (float)Math.sin(t * 0.3));
                p.lean = 0.5F;
                p.headX = 0.38F;
                p.rightArmX = -0.7F - 0.4F * push;
                p.rightArmY = -0.08F;
                p.rightArmZ = 0.06F;
                p.leftArmX = -0.35F;
                p.leftArmZ = -0.16F;
            }
            case SEASON -> {
                // 2.4.9: the bowl held low in the left hand (as when stirring), the right hand high over it shaking a pinch in, quick and
                // small; her head down, watching it fall.
                float shake = (float)Math.sin(t * 1.7);
                p.lean = 0.2F;
                p.headX = 0.42F;
                p.rightArmX = -1.45F + 0.08F * shake;
                p.rightArmY = -0.15F;
                p.rightArmZ = 0.12F + 0.06F * shake;
                p.leftArmX = -0.85F;
                p.leftArmY = 0.35F;
                p.leftArmZ = -0.05F;
            }
            case TASTE -> {
                // 2.4.9: a taste off the spoon: brought up to the mouth, blown on, tasted (the head up a little), a nod, the spoon back
                // down to the pot; the bowl kept low in the other hand.
                float up = ease(t / 18.0F) * (1.0F - ease((t - 50.0F) / 16.0F));
                float nod = t >= 36.0F && t < 50.0F ? 0.18F * (float)Math.sin((t - 36.0F) / 14.0F * Math.PI * 2.0) : 0.0F;
                p.rightArmX = -1.0F - 0.72F * up;
                p.rightArmY = -0.42F * up;
                p.rightArmZ = 0.05F;
                p.leftArmX = -0.85F + 0.15F * up;
                p.leftArmY = 0.35F;
                p.leftArmZ = -0.05F;
                p.headX = 0.3F - 0.38F * up + nod;
                p.headZ = 0.06F * up;
            }
            case SERVE -> {
                // 2.4.9: what it made held out to its spouse in both hands at the chest (never up at the face), a little bob, the head
                // tipped to them.
                float bob = 0.04F * (float)Math.sin(t * 0.25);
                p.lean = 0.12F;
                p.headX = 0.12F;
                p.headZ = 0.08F;
                p.rightArmX = -1.2F + bob;
                p.leftArmX = -1.2F + bob;
                p.rightArmY = -0.22F;
                p.leftArmY = 0.22F;
            }
            case TIDY -> {
                // 1.3.29: bent over a chest, one hand then the other putting things where they go.
                float a = (float)Math.sin(t * 0.22);
                float b = (float)Math.sin(t * 0.22 + Math.PI);
                p.lean = 0.55F;
                p.headX = 0.35F;
                p.rightArmX = -1.15F + 0.35F * a;
                p.leftArmX = -1.15F + 0.35F * b;
                p.rightArmZ = 0.12F;
                p.leftArmZ = -0.12F;
                p.headY = 0.15F * (float)Math.sin(t * 0.05);
            }
            case FLINCH -> {
                // 1.3.65 (a final kakuja, War above all): a step back from it (the right foot behind), the right arm drawn in across her
                // body, the left hand thrown up in front of her, palm out, to keep it off; her head pulled back and turned a little aside; a
                // tremble that settles as she holds her ground.
                float back = ease(Math.min(1.0F, t / 7.0F));
                float shake = 0.05F * (float)Math.sin(t * 2.1) * Math.max(0.0F, 1.0F - t / 50.0F);
                p.lean = -0.16F * back;
                p.drop = 0.35F * back;
                p.rightArmX = -1.0F * back + shake;
                p.rightArmY = 0.25F * back;
                p.rightArmZ = -0.75F * back;
                p.leftArmX = -1.95F * back - shake;
                p.leftArmY = 0.0F;
                p.leftArmZ = 0.08F * back;
                p.headX = -0.15F * back;
                p.headY = 0.35F * back;
                p.rightLegX = 0.38F * back;
                p.leftLegX = -0.1F * back;
                p.legHold = back;
            }
            case AWE -> {
                // 1.3.65 (Peace): her hands come together low on her chest, her head tipped back to look up at it, a slow breath in and out.
                float up = ease(Math.min(1.0F, t / 12.0F));
                float breath = (float)Math.sin(t * 0.09) * 0.5F + 0.5F;
                p.rightArmX = -0.75F * up;
                p.leftArmX = -0.75F * up;
                p.rightArmY = 0.25F * up;
                p.leftArmY = -0.25F * up;
                p.rightArmZ = -0.6F * up;
                p.leftArmZ = 0.6F * up;
                p.headX = (-0.5F - 0.08F * breath) * up;
                p.lean = -0.06F * up;
                p.drop = -0.12F * breath * up;
            }
            case BOW -> {
                // A small, polite bow from the hips, hands at the sides.
                float bow = (float)Math.sin(Math.min(1.0, t / (double)EtoGesture.BOW.ticks) * Math.PI);
                p.lean = 0.48F * bow;
                p.headX = 0.25F * bow;
                p.rightArmZ = 0.05F;
                p.leftArmZ = -0.05F;
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

    /** 1.3.14: arms folded tight across the chest. */
    private static void folded(RigPose p) {
        p.rightArmX = -1.05F;
        p.leftArmX = -1.05F;
        p.rightArmZ = -0.85F;
        p.leftArmZ = 0.85F;
        p.rightArmY = 0.3F;
        p.leftArmY = -0.3F;
    }

    /**
     * 2.4.8 (the owner: "better [Ken]'s crack finger animation so only one hand (arm) goes forward"; "the arm should have a little bend and
     * be lower"): the right arm alone out in front, about 50 degrees, turned a little in toward the chest, the elbow bent so the hand comes
     * up; the left arm hanging loose at the side, never brought up. 2.5.1 (the owner: "anything but smooth, its an instant snap to that
     * position"): one flowing movement, never a jump: the arm eases up ({@link #CRACK_RISE}), the thumb presses into a finger, the crack
     * ({@link #CRACK_AT}: a small flick of the hand, the head tipping with it), a moment held, then it eases back down to nothing
     * ({@link #CRACK_DOWN}). Shared with a player Ken taught a kakuja ({@link #crackOver}).
     */
    public static void crack(RigPose p, float t) {
        float w = crackWeight(t);
        float press = crackPress(t);
        float snap = crackSnap(t);
        p.rightArmX = (CRACK_ARM_X - 0.08F * press + 0.12F * snap) * w;
        p.rightArmY = (CRACK_ARM_Y - 0.08F * press) * w;
        p.rightArmZ = 0.03F * w;
        p.rightElbow = (CRACK_ELBOW + 0.1F * snap) * w;
        p.leftArmX = 0.06F * w;
        p.leftArmY = 0.0F;
        p.leftArmZ = -0.06F * w;
        p.headX = 0.14F * w;
        p.headZ = 0.3F * snap;
    }

    /**
     * 2.5.1: the crack laid over what the body is already doing (a player's stance, the kagune's), never replacing it outright: the right
     * arm drawn from wherever it was to the crack's and back again by {@link #crackWeight}, the head's tip at the crack added.
     */
    public static void crackOver(RigPose p, float t) {
        float w = crackWeight(t);
        if (w <= 0.0F) {
            return;
        }
        float press = crackPress(t);
        float snap = crackSnap(t);
        p.rightArmX += (CRACK_ARM_X - 0.08F * press + 0.12F * snap - p.rightArmX) * w;
        p.rightArmY += (CRACK_ARM_Y - 0.08F * press - p.rightArmY) * w;
        p.rightArmZ += (0.03F - p.rightArmZ) * w;
        p.rightElbow += (CRACK_ELBOW + 0.1F * snap - p.rightElbow) * w;
        p.headZ += 0.3F * snap;
    }

    /** 2.5.1: how much of the crack is on the body {@code t} ticks in: 0, eased up to 1 by {@link #CRACK_RISE}, held, eased back to 0. */
    public static float crackWeight(float t) {
        if (t <= 0.0F || t >= CRACK_END) {
            return 0.0F;
        }
        float up = ease(t / CRACK_RISE);
        float down = ease((t - CRACK_DOWN) / (CRACK_END - CRACK_DOWN));
        return up * (1.0F - down);
    }

    /** 2.5.1: the thumb pressing into the finger before the crack (0 to 1, eased). */
    static float crackPress(float t) {
        return ease((t - (CRACK_AT - 6.0F)) / 6.0F);
    }

    /** 2.5.1: the crack itself: a small flick and back over six ticks (0 outside it). */
    static float crackSnap(float t) {
        return t >= CRACK_AT && t < CRACK_AT + 6.0F ? (float)Math.sin((t - CRACK_AT) / 6.0F * Math.PI) : 0.0F;
    }

    /** When the finger cracks (ticks into the gesture): the same moment its sound is played (EtoGesture.CRACK_SNAP). */
    public static final float CRACK_AT = com.hex.kagune.control.npc.EtoGesture.CRACK_SNAP;
    /** 2.5.1: the arm all the way up by here; easing down from here; at rest again by here (ticks). */
    public static final float CRACK_RISE = 18.0F;
    public static final float CRACK_DOWN = 34.0F;
    public static final float CRACK_END = 52.0F;
    /** The arm at the crack: out about 50 degrees, a little in toward the chest, the elbow bent (a player's arm shows it). */
    static final float CRACK_ARM_X = -0.9F;
    static final float CRACK_ARM_Y = -0.3F;
    static final float CRACK_ELBOW = 0.75F;

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
