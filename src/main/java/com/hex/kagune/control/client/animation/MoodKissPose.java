package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.MoodKiss;

/**
 * 2.2.4 (pure): the kiss as the mood has it ({@link MoodKiss}), over the usual one ({@link EtoPose}): while they wait ({@code KISS}),
 * blush ({@code BLUSH}) and pull them in ({@code PULL}). Sweet is the usual, untouched.
 *
 * <p>2.3.5 (the owner: "you keep drawing animations with the arms closed in front ... like touching each other. What do you expect that is
 * going to happen to the player's first-person view? ... It's going to glitch ... the angry animations don't even look angry ... you were
 * meant to adapt the animation to the character. You just copied it"): each companion has their own way of every mood (Eto's theatrical,
 * Touka's guarded and one-handed, Ken's leading, his hands on them), and no arm is ever raised to face height turned in toward the middle:
 * hands go to their collar (below the face), their shoulders and waist (out to the sides), behind the back, on the hips, or stay down
 * ({@link #clear}: tested on every pose), so the partner's face and camera, right in front of hers, stay clear.
 */
public final class MoodKissPose {
    private MoodKissPose() {
    }

    /** The persona styles (Persona.style): Eto's, Ken's, Touka's. */
    public static final int ETO = 0;
    public static final int KEN = 1;
    public static final int TOUKA = 2;

    // Arm placings, as the right arm has them {x, y, z} (the left mirrored): y + turns it outward, z + swings it out to the side.
    /** Behind the back. */
    static final float[] BEHIND = {0.55F, 0.0F, -0.22F};
    /** A fist on the hip. */
    static final float[] HIP = {0.3F, 0.0F, 0.6F};
    /** Down at the side, a clenched fist. */
    static final float[] FIST = {0.1F, 0.0F, 0.16F};
    /** Hanging loose. */
    static final float[] LIMP = {0.12F, 0.0F, 0.07F};
    /** A fist in their collar: forward and down, at their chest, under both faces. */
    static final float[] COLLAR = {-1.1F, 0.0F, 0.04F};
    /** A hand on their shoulder: forward and out, beside their head. */
    static final float[] SHOULDER = {-1.45F, 0.28F, 0.18F};
    /** Over their shoulder, round the back of their neck from the side (never across the front). */
    static final float[] AROUND = {-1.85F, 0.22F, 0.24F};
    /** At their waist, out wide. */
    static final float[] WAIST = {-0.9F, 0.2F, 0.32F};
    /** Round their middle, holding on (out wide, low). */
    static final float[] HOLD = {-0.95F, 0.12F, 0.5F};
    /** Up by his own head, sheepish (high and out to the side, never in front of the face). */
    static final float[] SHEEPISH = {-2.7F, 0.1F, 0.3F};

    /** {@code own} with the kiss's mood on it ({@code persona}: Persona.style), or {@code own} if the kiss is sweet or this is no kiss. */
    public static RigPose apply(RigPose own, MoodKiss.Style style, EtoGesture gesture, double age, int persona) {
        if (style == null || style == MoodKiss.Style.SWEET || gesture == null) {
            return own;
        }
        int phase = switch (gesture) {
            case KISS -> 0;
            case BLUSH -> 1;
            case PULL -> 2;
            default -> -1;
        };
        if (phase < 0) {
            return own;
        }
        float t = (float)age;
        int who = Math.floorMod(persona, 3);
        RigPose now = pose(style, who, phase, t);
        if (phase == 0) {
            return now.scale(EtoPose.ease(t / 6.0F));
        }
        // 4.1.15 (the owner: "better the animation"): held, or pulled in, from the pose they were already in, never from standing straight
        // (each phase's clock starts again at 0: before, the body snapped upright and leant in all over again).
        float k = EtoPose.ease(t / BLEND);
        return k >= 1.0F ? now : pose(style, who, phase - 1, t).scale(1.0F - k).add(now, k);
    }

    /** 4.1.15: ticks to go from one phase of a kiss to the next (waiting, held, pulled in). */
    static final float BLEND = 8.0F;

    /** Eto's (2.2.4's callers). */
    public static RigPose apply(RigPose own, MoodKiss.Style style, EtoGesture gesture, double age) {
        return apply(own, style, gesture, age, ETO);
    }

    /** The pose for {@code style}, {@code persona}'s way, waiting (0), blushing (1) or pulling them in (2), {@code t} ticks in. */
    static RigPose pose(MoodKiss.Style style, int persona, int phase, float t) {
        RigPose p = new RigPose();
        boolean blush = phase == 1;
        boolean pull = phase == 2;
        float slow = (float)Math.sin(t * 0.05);
        switch (style) {
            case ANGRY -> angry(p, persona, phase, t);
            case POSSESSIVE -> {
                switch (persona) {
                    case TOUKA -> {
                        // A fist twisted in their collar, dragging them down; the other a fist; glaring straight at them.
                        lean(p, pull ? 0.6F : 0.55F, 0.06F);
                        right(p, pull ? -1.0F : -1.2F, 0.0F, 0.04F);
                        left(p, FIST);
                        p.headZ = 0.05F;
                    }
                    case KEN -> {
                        // Both hands on their shoulders, gripping, holding them where they are.
                        lean(p, pull ? 0.6F : 0.54F, 0.06F);
                        both(p, pull ? WAIST : SHOULDER);
                        p.headZ = 0.14F;
                    }
                    default -> {
                        // On her toes: one fist in their collar pulling them down, the other hand round the back of their neck.
                        lean(p, pull ? 0.62F : 0.58F, 0.08F);
                        right(p, COLLAR);
                        left(p, AROUND);
                        p.headZ = 0.22F;
                        p.drop = pull ? -0.5F : -0.7F;
                    }
                }
            }
            case GIDDY -> {
                float bounce = (float)Math.abs(Math.sin(t * 0.35));
                switch (persona) {
                    case TOUKA -> {
                        // Hands behind her back, trying not to bounce and bouncing anyway, looking away and back.
                        lean(p, blush ? 0.55F : 0.5F, 0.04F);
                        both(p, BEHIND);
                        p.drop = -0.5F * bounce;
                        p.headY = 0.25F * (float)Math.sin(t * 0.07);
                        p.headZ = 0.12F;
                    }
                    case KEN -> {
                        // A hand up behind his own head, grinning, a little bounce.
                        lean(p, blush ? 0.52F : 0.48F, 0.04F);
                        right(p, SHEEPISH);
                        left(p, LIMP);
                        p.drop = -0.4F * bounce;
                        p.headZ = -0.15F;
                    }
                    default -> {
                        // Bouncing on her toes, arms flapping at her sides, her head bobbing.
                        float flap = (float)Math.abs(Math.sin(t * 0.5));
                        lean(p, blush ? EtoPose.BLUSH_LEAN : EtoPose.KISS_LEAN, 0.04F);
                        right(p, -0.35F, 0.0F, 0.55F + 0.45F * flap);
                        left(p, -0.35F, 0.0F, 0.55F + 0.45F * flap);
                        p.headZ = 0.16F * (float)Math.sin(t * 0.3);
                        p.drop = -(blush ? 0.7F : 1.3F) * bounce;
                    }
                }
                if (pull) {
                    // Pulled in, still bouncing: arms over their shoulders, round from the sides.
                    both(p, AROUND);
                    p.lean = EtoPose.PULL_LEAN;
                    p.headX = -EtoPose.PULL_LEAN - 0.06F;
                }
            }
            case TENDER -> {
                switch (persona) {
                    case TOUKA -> {
                        // One hand on their shoulder, the other at her side; her head tipped, eyes shut, still.
                        lean(p, blush ? 0.55F : 0.5F, 0.04F);
                        right(p, pull ? AROUND : SHOULDER);
                        left(p, pull ? SHOULDER : LIMP);
                        p.headZ = -0.18F;
                    }
                    case KEN -> {
                        // His hands at their waist, gentle, swaying a little.
                        lean(p, blush ? 0.52F : 0.48F, 0.04F);
                        both(p, WAIST);
                        if (pull) {
                            both(p, -1.0F, 0.2F, 0.38F);
                        }
                        p.headZ = 0.18F + 0.05F * slow;
                        p.roll = 0.02F * slow;
                    }
                    default -> {
                        // Both hands on their shoulders, slowly, swaying, her head tipped.
                        lean(p, blush ? EtoPose.BLUSH_LEAN : EtoPose.KISS_LEAN, 0.04F);
                        both(p, pull ? AROUND : SHOULDER);
                        p.headZ = 0.2F + 0.06F * slow;
                        p.roll = 0.02F * slow;
                    }
                }
            }
            case DESPERATE -> {
                float shake = 0.015F * (float)Math.sin(t * 1.3);
                switch (persona) {
                    case TOUKA -> {
                        // A fist in their shirt, the other arm round their side; her head down against them, trembling.
                        lean(p, 0.6F, -0.08F);
                        right(p, COLLAR);
                        left(p, HOLD);
                        p.headZ = 0.12F;
                    }
                    case KEN -> {
                        // Both arms round their back, low; his head bowed onto their shoulder.
                        lean(p, 0.62F, -0.04F);
                        both(p, HOLD);
                        p.headZ = 0.28F;
                    }
                    default -> {
                        // Thrown round them, holding on with her whole weight, on her toes.
                        lean(p, 0.62F, 0.02F);
                        both(p, HOLD);
                        p.headZ = 0.2F;
                        p.drop = -0.4F;
                    }
                }
                p.roll = shake;
            }
            case SLEEPY -> {
                switch (persona) {
                    case TOUKA -> {
                        // Drooping forward onto them, catching herself with a jerk now and then, a hand holding their sleeve.
                        float nod = (float)Math.max(0.0, Math.sin(t * 0.08));
                        lean(p, (blush ? 0.46F : 0.42F) + 0.1F * nod, -0.1F - 0.12F * nod);
                        right(p, -0.85F, 0.0F, 0.06F);
                        left(p, LIMP);
                        p.headZ = 0.1F;
                    }
                    case KEN -> {
                        // Leaning his weight on them, head on one side, slow nods; his hands in his pockets.
                        lean(p, blush ? 0.5F : 0.46F, -0.08F - 0.05F * slow);
                        both(p, 0.24F, 0.0F, -0.02F);
                        p.headZ = 0.3F;
                        p.roll = 0.03F * slow;
                    }
                    default -> {
                        // Her head lolling side to side, swaying, drooping in and out.
                        lean(p, (blush ? 0.46F : 0.4F) + 0.07F * (float)Math.sin(t * 0.06), -0.1F - 0.07F * (float)Math.sin(t * 0.05));
                        both(p, LIMP);
                        p.headZ = 0.28F * (float)Math.sin(t * 0.04);
                        p.roll = 0.05F * (float)Math.sin(t * 0.04);
                    }
                }
                if (pull) {
                    both(p, WAIST);
                }
            }
            case HUNGRY -> {
                switch (persona) {
                    case TOUKA -> {
                        // Fists clenched at her sides, holding herself back, shaking; her head tipped.
                        lean(p, blush ? 0.55F : 0.5F, 0.02F);
                        both(p, FIST);
                        p.headZ = -0.25F;
                        p.roll = 0.012F * (float)Math.sin(t * 1.7);
                    }
                    case KEN -> {
                        // His hands on their shoulders, holding them at arm's length; not leaning in all the way; shaking.
                        lean(p, 0.36F, 0.0F);
                        both(p, SHOULDER);
                        p.headY = 0.15F;
                        p.roll = 0.012F * (float)Math.sin(t * 1.7);
                    }
                    default -> {
                        // Her head tipped as if to bite, a hand at their jaw from the side, the other on her hip; swaying like a stalker.
                        lean(p, blush ? 0.58F : 0.54F, 0.02F);
                        right(p, -1.4F, 0.3F, 0.14F);
                        left(p, HIP);
                        p.headZ = -0.32F;
                        p.roll = 0.03F * (float)Math.sin(t * 0.07);
                    }
                }
                if (pull) {
                    p.lean = Math.max(p.lean, 0.56F);
                    p.headX = -p.lean - 0.04F;
                }
            }
            case TEASING -> {
                float bob = (float)Math.sin(t * 0.15);
                switch (persona) {
                    case TOUKA -> {
                        // Hands on her hips, leaning in and pulling back, a smirk to one side.
                        lean(p, 0.4F + 0.1F * bob, 0.04F);
                        both(p, HIP);
                        p.headZ = -0.22F;
                    }
                    case KEN -> {
                        // Hands down at his sides, leaning in and away, grinning, his head on one side.
                        lean(p, 0.38F + 0.1F * bob, 0.04F);
                        both(p, -0.05F, 0.0F, 0.1F);
                        p.headZ = 0.2F;
                    }
                    default -> {
                        // Hands behind her back, bobbing in and out of reach, a sly tilt.
                        lean(p, 0.38F + 0.12F * bob, 0.04F);
                        both(p, BEHIND);
                        p.headZ = 0.24F;
                        p.headY = -0.14F * (float)Math.sin(t * 0.1);
                        p.roll = -0.04F;
                    }
                }
                if (pull) {
                    both(p, AROUND);
                    p.lean = EtoPose.PULL_LEAN;
                    p.headX = -EtoPose.PULL_LEAN - 0.06F;
                }
            }
            case SHY -> shy(p, persona, phase, t);
            case MORNING -> morning(p, persona, phase, t);
            case RAIN -> rain(p, persona, phase, t);
            case MOVIE -> movie(p, persona, phase, t);
            case NOSE -> nose(p, persona, phase, t);
            case GALLANT -> gallant(p, persona, phase, t);
            default -> {
            }
        }
        return p;
    }

    // ---------------------------------------------------------------- 4.1.15: the new ones (the owner: "add more kiss types")

    /**
     * Shy: Eto's, hands behind her back, rocking on her heels, her cheek turned to be kissed; Touka's, fists at her sides, her face turned
     * hard away, trembling; Ken's, a hand behind his head, his head bowed. Held, a hand finds their shoulder (Ken's, their waist).
     */
    static void shy(RigPose p, int persona, int phase, float t) {
        boolean blush = phase == 1;
        boolean pull = phase == 2;
        switch (persona) {
            case TOUKA -> {
                lean(p, pull ? 0.52F : blush ? 0.48F : 0.38F, -0.04F);
                if (blush || pull) {
                    right(p, pull ? AROUND : SHOULDER);
                } else {
                    right(p, FIST);
                }
                left(p, FIST);
                p.headY = blush || pull ? -0.18F : -0.45F;
                p.roll = 0.01F * (float)Math.sin(t * 2.0);
            }
            case KEN -> {
                lean(p, pull ? 0.5F : blush ? 0.45F : 0.36F, -0.12F); // head bowed
                if (blush || pull) {
                    both(p, pull ? HOLD : WAIST);
                } else {
                    right(p, SHEEPISH);
                    left(p, LIMP);
                }
                p.headZ = 0.1F;
            }
            default -> {
                lean(p, pull ? EtoPose.PULL_LEAN : blush ? 0.5F : 0.42F, 0.04F);
                if (pull) {
                    both(p, AROUND);
                } else {
                    right(p, blush ? SHOULDER : BEHIND);
                    left(p, BEHIND);
                }
                p.headY = blush || pull ? 0.12F : 0.3F; // the cheek offered
                p.drop = -0.3F * (float)Math.abs(Math.sin(t * 0.12));
                p.rightLegX = blush || pull ? 0.0F : -0.15F * (float)Math.sin(t * 0.12); // a toe scuffing the floor
            }
        }
    }

    /**
     * The morning after: Eto's, a slow stretch with one arm up high at her side, swaying, her head heavy; Touka's, arms wrapped round
     * herself (the blanket), grumpy; Ken's, a mug held at his chest, a hand at their waist.
     */
    static void morning(RigPose p, int persona, int phase, float t) {
        boolean blush = phase == 1;
        boolean pull = phase == 2;
        float sway = (float)Math.sin(t * 0.05);
        switch (persona) {
            case TOUKA -> {
                lean(p, pull ? 0.5F : blush ? 0.46F : 0.4F, -0.02F);
                right(p, blush ? SHOULDER : pull ? AROUND : new float[] {-0.9F, 0.3F, -0.7F});
                left(p, pull ? WAIST : new float[] {-0.9F, 0.3F, -0.7F}); // arms wrapped round herself (the blanket)
                p.headZ = -0.1F;
                p.roll = 0.02F * sway;
            }
            case KEN -> {
                lean(p, pull ? 0.5F : blush ? 0.48F : 0.42F, 0.0F);
                right(p, pull ? new float[] {-1.0F, 0.2F, 0.38F} : new float[] {-0.9F, -0.25F, 0.05F});
                left(p, pull ? new float[] {-1.0F, 0.2F, 0.38F} : WAIST);
                p.headZ = 0.14F + 0.04F * sway;
                p.roll = 0.02F * sway;
            }
            default -> {
                lean(p, (pull ? EtoPose.PULL_LEAN : blush ? 0.5F : 0.44F) + 0.03F * sway, -0.02F);
                if (blush || pull) {
                    both(p, pull ? AROUND : WAIST);
                } else {
                    float down = EtoPose.ease((t - 26.0F) / 18.0F); // the stretch, then the arm comes down
                    right(p, -2.9F + (LIMP[0] + 2.9F) * down, 0.1F * (1.0F - down), 0.4F + (LIMP[2] - 0.4F) * down);
                    left(p, LIMP);
                }
                p.headZ = 0.22F + 0.05F * sway;
            }
        }
    }

    /**
     * In the rain: Eto's, arms out wide catching it, her face tipped up into it, then down to them; Ken's, his hands at their waist, his head
     * back laughing at the sky, then in; Touka's, hands in her pockets, scowling at the rain, then giving up on it.
     */
    static void rain(RigPose p, int persona, int phase, float t) {
        boolean blush = phase == 1;
        boolean pull = phase == 2;
        float up = 0.3F * (1.0F - EtoPose.ease((t - 18.0F) / 20.0F)); // the face up into the rain, then down to them
        switch (persona) {
            case TOUKA -> {
                lean(p, pull ? 0.52F : blush ? 0.48F : 0.4F, 0.02F);
                right(p, pull ? AROUND : blush ? SHOULDER : new float[] {0.2F, 0.0F, -0.02F});
                left(p, pull ? WAIST : new float[] {0.2F, 0.0F, -0.02F});
                p.headY = blush || pull ? 0.0F : -0.25F * (1.0F - EtoPose.ease((t - 10.0F) / 20.0F));
                p.headZ = -0.08F;
            }
            case KEN -> {
                lean(p, pull ? 0.52F : blush ? 0.46F : 0.4F, blush || pull ? 0.02F : up);
                both(p, pull ? new float[] {-1.0F, 0.2F, 0.38F} : WAIST);
                p.headZ = 0.12F;
            }
            default -> {
                lean(p, pull ? EtoPose.PULL_LEAN : blush ? 0.52F : 0.46F, blush || pull ? 0.04F : up + 0.04F);
                if (blush || pull) {
                    both(p, pull ? AROUND : SHOULDER);
                    p.drop = pull ? -0.5F : 0.0F;
                } else {
                    both(p, -0.55F, 0.0F, 0.95F); // out wide, palms to the rain
                    p.headY = 0.1F * (float)Math.sin(t * 0.08);
                }
                p.headZ = 0.18F;
            }
        }
    }

    /**
     * Like a film: Eto's, on her toes, her arms over their shoulders, a foot popped up behind her; Touka's the same, smaller, grudging, one
     * hand on their shoulder; Ken's, the dip: leaning right over them, an arm low round their back, a step forward.
     */
    static void movie(RigPose p, int persona, int phase, float t) {
        boolean blush = phase == 1;
        boolean pull = phase == 2;
        float pop = EtoPose.ease(t / 14.0F);
        switch (persona) {
            case TOUKA -> {
                lean(p, pull ? 0.52F : blush ? 0.5F : 0.48F, 0.04F);
                right(p, pull ? AROUND : SHOULDER);
                left(p, LIMP);
                p.rightLegX = (blush || pull ? 0.55F : 0.4F * pop);
                p.drop = -0.3F;
                p.headZ = -0.12F;
            }
            case KEN -> {
                lean(p, pull ? 0.74F : blush ? 0.72F : 0.68F, -0.15F);
                right(p, HOLD);
                left(p, pull ? HOLD : WAIST);
                p.rightLegX = -0.35F;
                p.leftLegX = 0.3F;
                p.headZ = 0.2F;
            }
            default -> {
                lean(p, pull ? EtoPose.PULL_LEAN : blush ? 0.54F : 0.5F, 0.06F);
                both(p, AROUND);
                p.leftLegX = blush || pull ? 0.95F : 0.75F * pop; // the foot pop
                p.drop = -0.6F;
                p.headZ = 0.2F;
            }
        }
    }

    /**
     * Nose to nose, rubbing side to side: Eto's, her hands in theirs low between them, bobbing, giggling; Ken's, his hands on their shoulders;
     * Touka's, hands behind her back, quick little rubs. Held, slower.
     */
    static void nose(RigPose p, int persona, int phase, float t) {
        boolean blush = phase == 1;
        boolean pull = phase == 2;
        float rub = (float)Math.sin(t * (blush || pull ? 0.25 : 0.45));
        switch (persona) {
            case TOUKA -> {
                lean(p, pull ? 0.54F : 0.5F, 0.0F);
                both(p, pull ? AROUND : BEHIND);
                p.headY = 0.18F * (float)Math.sin(t * 0.55);
                p.headZ = -0.1F;
            }
            case KEN -> {
                lean(p, pull ? 0.54F : 0.5F, -0.04F);
                both(p, pull ? WAIST : SHOULDER);
                p.headY = 0.22F * rub;
            }
            default -> {
                lean(p, pull ? EtoPose.PULL_LEAN : 0.56F, 0.0F);
                if (pull) {
                    both(p, AROUND);
                } else {
                    both(p, -0.75F, 0.0F, 0.08F); // her hands in theirs, low between them
                }
                p.headY = 0.28F * rub;
                p.drop = -0.3F - 0.3F * (float)Math.abs(Math.sin(t * 0.3));
            }
        }
    }

    /**
     * A bow over their hand: Eto's, a theatrical curtsy, one foot back, her other hand out at her side holding her skirt; Ken's, a deep bow,
     * his other hand behind his back; Touka's, quick, grudging, glancing aside. Held, the face comes up to them; pulled in, up into it.
     */
    static void gallant(RigPose p, int persona, int phase, float t) {
        boolean blush = phase == 1;
        boolean pull = phase == 2;
        float down = blush ? -0.1F : 0.0F; // held: looking up at them now, not at the hand
        switch (persona) {
            case TOUKA -> {
                lean(p, 0.5F, pull ? 0.04F : blush ? down : -0.25F);
                right(p, pull ? WAIST : new float[] {-1.05F, 0.05F, 0.06F});
                left(p, pull ? SHOULDER : FIST);
                p.headY = pull || blush ? 0.0F : -0.25F;
            }
            case KEN -> {
                lean(p, pull ? 0.5F : 0.7F, pull ? 0.04F : blush ? down : -0.45F);
                right(p, pull ? WAIST : new float[] {-1.0F, 0.0F, 0.06F});
                left(p, pull ? WAIST : BEHIND);
            }
            default -> {
                lean(p, pull ? EtoPose.PULL_LEAN : 0.62F, pull ? 0.06F : blush ? down : -0.35F);
                right(p, pull ? WAIST : new float[] {-0.95F, 0.0F, 0.08F});
                left(p, pull ? AROUND : new float[] {-0.25F, 0.0F, 0.75F}); // her skirt held out
                p.leftLegX = pull ? 0.0F : 0.35F; // a foot back
                p.drop = pull ? 0.0F : 0.8F; // the dip of a curtsy
            }
        }
    }

    /**
     * Angry: Eto's, a fist in their collar, the other on her hip, her nose up and turned away, stamping; Touka's, both fists clenched at
     * her sides, her head turned hard away, then a fist in their collar (held, pulling); Ken's, fists at his sides, his head bowed and
     * turned away, then his hands on their shoulders.
     */
    static void angry(RigPose p, int persona, int phase, float t) {
        boolean blush = phase == 1;
        boolean pull = phase == 2;
        float stamp = (float)Math.max(0.0, Math.sin(t * 0.9));
        float back = blush ? EtoPose.ease(t / 30.0F) : pull ? 1.0F : 0.0F; // held, the face comes back round
        switch (persona) {
            case TOUKA -> {
                lean(p, pull ? 0.52F : 0.36F, 0.0F);
                if (blush || pull) {
                    right(p, COLLAR);
                    left(p, FIST);
                } else {
                    both(p, FIST);
                }
                p.headY = -0.55F * (1.0F - back) - 0.12F;
                p.headX += 0.08F; // glaring from under her brows
                p.headZ = -0.06F;
                p.roll = 0.012F * (float)Math.sin(t * 2.4);
            }
            case KEN -> {
                lean(p, pull ? 0.5F : 0.32F, 0.0F);
                if (blush || pull) {
                    both(p, SHOULDER);
                } else {
                    both(p, FIST);
                }
                p.headY = 0.35F * (1.0F - back) + 0.05F;
                p.headX += 0.16F * (1.0F - back); // head bowed
            }
            default -> {
                lean(p, pull ? 0.52F : 0.4F, 0.0F);
                right(p, COLLAR);
                left(p, HIP);
                p.headY = 0.5F * (1.0F - back) + 0.08F;
                p.headX -= 0.14F * (1.0F - back); // nose in the air
                p.rightLegX = pull ? 0.0F : -0.28F * stamp;
                p.roll = 0.015F * (float)Math.sin(t * 2.2);
            }
        }
    }

    /**
     * True if {@code p} keeps the partner's face clear: no arm raised to face height ({@code x} under -1.25) turned in toward the middle,
     * where the partner's face (and a first-person camera) is.
     */
    public static boolean clear(RigPose p) {
        boolean right = p.rightArmX >= -1.25F || p.rightArmY >= -0.05F && p.rightArmZ >= -0.05F;
        boolean left = p.leftArmX >= -1.25F || p.leftArmY <= 0.05F && p.leftArmZ <= 0.05F;
        return right && left;
    }

    private static void lean(RigPose p, float lean, float headUp) {
        p.lean = lean;
        p.headX = -lean - headUp;
    }

    private static void right(RigPose p, float x, float y, float z) {
        p.rightArmX = x;
        p.rightArmY = y;
        p.rightArmZ = z;
    }

    private static void right(RigPose p, float[] a) {
        right(p, a[0], a[1], a[2]);
    }

    /** The left arm, given as the right arm would have it (mirrored). */
    private static void left(RigPose p, float x, float y, float z) {
        p.leftArmX = x;
        p.leftArmY = -y;
        p.leftArmZ = -z;
    }

    private static void left(RigPose p, float[] a) {
        left(p, a[0], a[1], a[2]);
    }

    private static void both(RigPose p, float[] a) {
        right(p, a);
        left(p, a);
    }

    private static void both(RigPose p, float x, float y, float z) {
        right(p, x, y, z);
        left(p, x, y, z);
    }
}
