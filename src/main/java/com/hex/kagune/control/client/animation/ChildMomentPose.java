package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.family.ChildMoment;
import com.hex.kagune.control.family.ChildRules;
import com.hex.kagune.control.family.ChildStage;

/**
 * 1.3.20: a child and its parent in a moment together (pure), both bodies on the same clock ({@link ChildMoment}): the parent's (a
 * player's, or an NPC parent's: Eto, Touka, Ken) and the child's. The parent bends down to a little one (a player crouches, knees bent;
 * an NPC, whose model bends no knees, leans further instead); the child looks up at them, hops, squirms, reaches. Also how a child rides on
 * their shoulders (its legs down their front, its hands on their head; their hands up holding its shins) and how it flies held out at arm's
 * length as they spin. Angles in radians as {@link RigPose} has them: an arm's X negative raises it forward, the right arm's Z positive
 * takes it out to the side (the left arm's negative); the head's X positive looks down.
 */
public final class ChildMomentPose {
    private ChildMomentPose() {
    }

    static float sin(double x) {
        return (float)Math.sin(x);
    }

    static float smooth(double x) {
        double t = Math.max(0.0, Math.min(1.0, x));
        return (float)(t * t * (3.0 - 2.0 * t));
    }

    /** A little one's (baby, toddler, child): the parent bends down to it. */
    static boolean little(ChildStage stage) {
        return !stage.older();
    }

    // ---------------------------------------------------------------- the parent

    /**
     * The parent's pose in {@code moment} {@code age} ticks in, with a child at {@code stage}; {@code knees}: its model bends its knees (a
     * player's) so it crouches down to a little one, else it leans further. Arms and legs as absolutes (with their holds set), the rest as
     * offsets: blend it in with {@link #blend}.
     */
    public static RigPose parent(ChildMoment moment, double age, ChildStage stage, boolean knees) {
        RigPose p = new RigPose();
        boolean small = little(stage);
        float t = (float)age;
        switch (moment) {
            case HUG -> {
                if (small) {
                    bend(p, knees, stage);
                    p.rightArmX = -1.05F;
                    p.leftArmX = -1.05F;
                    p.rightArmZ = -0.38F;
                    p.leftArmZ = 0.38F;
                    p.headX = 0.45F;
                } else {
                    p.rightArmX = -1.45F;
                    p.leftArmX = -1.45F;
                    p.rightArmY = -0.16F;
                    p.leftArmY = 0.16F;
                    p.rightArmZ = -0.08F;
                    p.leftArmZ = 0.08F;
                    p.lean = 0.12F;
                    p.headX = 0.3F;
                }
                p.roll = 0.05F * sin(t * 0.16); // a slow rock
                p.armHold = 1.0F;
            }
            case PAT -> {
                // The hand on top of its head, wherever its head is: a baby's low (bent right down to it), a teen's near their own.
                float reach = switch (stage) {
                    case BABY -> -0.7F;
                    case TODDLER -> -1.0F;
                    case CHILD -> -1.3F;
                    case TEEN, ADULT -> -1.75F;
                };
                if (stage == ChildStage.BABY || stage == ChildStage.TODDLER) {
                    bend(p, knees, stage);
                } else if (stage == ChildStage.CHILD) {
                    p.lean += 0.1F;
                }
                p.rightArmX = reach + 0.13F * sin(t * 0.7);
                p.rightArmZ = -0.12F;
                p.rightArmY = -0.1F;
                p.headX = 0.42F;
                p.headZ = 0.1F;
                p.rightArmHold = 1.0F;
            }
            case HIGH_FIVE -> {
                // Up, the slap at 12, and back down.
                float up = smooth(t / 9.0) * (1.0F - smooth((t - 16.0) / 10.0));
                float slap = Math.max(0.0F, 1.0F - Math.abs(t - 12.0F) / 3.0F);
                p.rightArmX = (stage.older() ? -2.5F : -2.05F) * up - 0.2F * slap;
                p.rightArmZ = -0.18F * up;
                p.headX = small ? 0.25F : 0.0F;
                p.lean = small ? 0.12F : 0.0F;
                p.rightArmHold = up;
            }
            case TICKLE -> {
                if (small) {
                    bend(p, knees, stage);
                }
                float wiggle = sin(t * 1.5);
                p.rightArmX = (small ? -0.85F : -1.05F) + 0.08F * wiggle;
                p.leftArmX = (small ? -0.85F : -1.05F) - 0.08F * wiggle;
                p.rightArmZ = -0.22F;
                p.leftArmZ = 0.22F;
                p.rightArmY = 0.18F * wiggle;
                p.leftArmY = 0.18F * wiggle;
                p.headX = 0.4F;
                p.armHold = 1.0F;
            }
            case PEEKABOO -> {
                bend(p, knees, stage);
                float open = peek(t);
                // Hands over the face, then flung open: boo!
                p.rightArmX = -2.15F + 0.6F * open;
                p.leftArmX = -2.15F + 0.6F * open;
                p.rightArmZ = -0.55F + 1.15F * open;
                p.leftArmZ = 0.55F - 1.15F * open;
                p.headX = 0.25F - 0.2F * open;
                p.armHold = 1.0F;
            }
            case TOSS -> {
                // From the cradle up over the head to throw, back down to catch.
                float up = (float)ChildMoment.tossArms(age);
                double lift = ChildMoment.tossLift(age);
                p.rightArmX = -0.95F - 1.6F * up;
                p.leftArmX = -0.95F - 1.6F * up;
                p.rightArmY = -0.45F * (1.0F - up);
                p.leftArmY = 0.45F * (1.0F - up);
                p.headX = (float)(-0.6 * lift);
                p.drop = 0.8F * Math.max(0.0F, 0.5F - up); // a little give at the knees to catch
                p.armHold = 1.0F;
            }
            case SPIN -> spinning(p, age);
            case TUCK -> {
                // Bent over its bed: the covers smoothed (both hands, side to side), then a hand on its head, a last look.
                bend(p, knees, ChildStage.TODDLER);
                float smooth = 1.0F - smooth((t - 46.0) / 8.0);
                p.rightArmX = -0.85F - 0.15F * (1.0F - smooth);
                p.leftArmX = -0.85F * smooth;
                p.rightArmY = 0.3F * sin(t * 0.5) * smooth;
                p.leftArmY = -0.3F * sin(t * 0.5 + 1.2) * smooth;
                p.rightArmZ = -0.15F;
                p.leftArmZ = 0.15F * smooth;
                p.headX = 0.55F;
                p.armHold = 1.0F;
            }
            case FEED -> {
                // The food held out to it (down low to a little one), a little patient bob.
                float reach = switch (stage) {
                    case BABY -> -0.7F;
                    case TODDLER -> -0.95F;
                    case CHILD -> -1.2F;
                    case TEEN, ADULT -> -1.35F;
                };
                if (stage == ChildStage.BABY || stage == ChildStage.TODDLER) {
                    bend(p, knees, stage);
                }
                float give = smooth(t / 6.0) * (1.0F - smooth((t - 18.0) / 8.0));
                p.rightArmX = reach * give;
                p.rightArmZ = -0.1F * give;
                p.headX = 0.35F;
                p.rightArmHold = give;
            }
            case NUZZLE -> {
                // 4.1.15: a nuzzle to the top of its head (bent down to a little one), both hands at its shoulders, a moment held, back.
                float in = smooth(t / 12.0) * (1.0F - smooth((t - 32.0) / 12.0));
                float reach = shoulderReach(stage);
                if (small) {
                    bend(p, knees, stage);
                }
                p.lean += 0.22F * in;
                p.headX = 0.3F + 0.4F * in;
                p.rightArmX = reach * in;
                p.leftArmX = reach * in;
                p.rightArmZ = -0.24F * in;
                p.leftArmZ = 0.24F * in;
                p.armHold = in;
            }
            case BOOP -> {
                // 4.1.15: a finger out to its nose, the boop at 12, back.
                float out = smooth(t / 9.0) * (1.0F - smooth((t - 18.0) / 12.0));
                float jab = Math.max(0.0F, 1.0F - Math.abs(t - 12.0F) / 2.5F);
                if (small) {
                    bend(p, knees, stage);
                }
                p.rightArmX = (headReach(stage) + 0.12F) * out - 0.14F * jab;
                p.rightArmY = 0.12F * out;
                p.rightArmZ = -0.05F * out;
                p.headX = 0.35F;
                p.headZ = -0.1F * out;
                p.rightArmHold = out;
            }
            case FIST_BUMP -> {
                // 4.1.15: a fist out at its height, bumped at 10, then thrown up and open: the explosion.
                float out = smooth(t / 7.0) * (1.0F - smooth((t - 12.0) / 4.0));
                float boom = smooth((t - 11.0) / 4.0) * (1.0F - smooth((t - 20.0) / 9.0));
                float bump = Math.max(0.0F, 1.0F - Math.abs(t - 10.0F) / 2.0F);
                float level = stage.older() ? -1.35F : -1.1F;
                p.rightArmX = level * out - 0.12F * bump - 1.9F * boom;
                p.rightArmZ = -0.08F * out - 0.55F * boom;
                p.lean = small ? 0.12F : 0.0F;
                p.headX = small ? 0.22F : 0.05F;
                p.rightArmHold = Math.max(out, boom);
            }
            case PATTY_CAKE -> {
                // 4.1.15: four rounds of a clap of their own and both palms out to its; bent down to it.
                bend(p, knees, stage);
                float in = smooth(t / 6.0) * (1.0F - smooth((t - 72.0) / 8.0));
                float own = pattyOwn(t);
                float level = stage == ChildStage.TODDLER ? -0.95F : -1.2F;
                p.rightArmX = (level - 0.25F * (1.0F - own)) * in;
                p.leftArmX = (level - 0.25F * (1.0F - own)) * in;
                p.rightArmZ = (-0.08F - 0.3F * own + 0.12F * (1.0F - own)) * in;
                p.leftArmZ = (0.08F + 0.3F * own - 0.12F * (1.0F - own)) * in;
                p.headX = 0.3F;
                p.armHold = in;
            }
            case DANCE -> {
                // 4.1.15: both its hands in theirs, low to a toddler, swaying side to side and stepping, a bounce on the beat.
                float in = smooth(t / 8.0) * (1.0F - smooth((t - 80.0) / 10.0));
                float sway = sin(t * 0.21);
                float level = switch (stage) {
                    case TODDLER -> -0.8F;
                    case CHILD -> -1.0F;
                    default -> -1.15F;
                };
                if (stage == ChildStage.TODDLER) {
                    p.lean += 0.3F * in;
                }
                p.rightArmX = level * in;
                p.leftArmX = level * in;
                p.rightArmZ = -0.22F * in;
                p.leftArmZ = 0.22F * in;
                p.roll = 0.1F * sway * in;
                p.headZ = -0.08F * sway * in;
                p.headX = stage == ChildStage.TEEN ? 0.1F : 0.3F;
                p.rightLegX = 0.18F * sway * in;
                p.leftLegX = -0.18F * sway * in;
                p.armHold = in;
                p.legHold = in;
            }
            default -> {
            }
        }
        return p;
    }

    /** 4.1.15: how far forward an arm goes to a child's shoulders at {@code stage} (radians, as PAT's reach, a little lower). */
    static float shoulderReach(ChildStage stage) {
        return switch (stage) {
            case BABY -> -0.62F;
            case TODDLER -> -0.88F;
            case CHILD -> -1.12F;
            case TEEN, ADULT -> -1.45F;
        };
    }

    /** 4.1.15: how far forward an arm goes to a child's face at {@code stage} (radians). */
    static float headReach(ChildStage stage) {
        return switch (stage) {
            case BABY -> -0.78F;
            case TODDLER -> -1.05F;
            case CHILD -> -1.32F;
            case TEEN, ADULT -> -1.62F;
        };
    }

    /** 4.1.15: PATTY_CAKE: 1 while both clap their own hands, 0 while their palms meet; four rounds of 18 ticks after the first 4. */
    static float pattyOwn(float t) {
        float round = ((t - 4.0F) % 18.0F + 18.0F) % 18.0F / 18.0F;
        return 0.5F + 0.5F * (float)Math.cos(round * Math.PI * 2.0);
    }

    /**
     * 1.3.21: hand in hand: the parent's left arm a little out and forward to its child's hand, held still as they walk. Solved with
     * {@link #handChild} and {@code ChildRules.handSide} on the real models (a player's arm; the child's at its stage's size and layout, a
     * toddler's half size) so the two hands meet: a toddler reaching up to it, a child and a teen down and out to it.
     */
    public static RigPose handParent(ChildStage stage, double size) {
        RigPose p = new RigPose();
        p.leftArmZ = ChildRules.handParentZ(stage, size);
        p.leftArmX = ChildRules.handParentX(stage);
        p.leftArmHold = 1.0F;
        return p;
    }

    /**
     * 1.3.21: hand in hand: the child's right arm to its parent's hand ({@link #handParent}), looking up at them. A toddler's arm is
     * short and its head big: its arm straight up to the hand by its head, the head tipped away and up (so it clears their hand), not
     * turned; a child and a teen turn their heads to them (to its right: y+).
     */
    public static RigPose handChild(ChildStage stage, double parent, boolean slim) {
        RigPose p = new RigPose();
        ChildRules.Hand hand = ChildRules.hand(stage, parent, slim);
        p.rightArmZ = hand.armZ();
        p.rightArmX = hand.armX();
        switch (stage) {
            case BABY, TODDLER -> {
                p.headX = -0.2F;
                p.headZ = 0.4F;
            }
            case CHILD -> {
                p.headX = -0.15F;
                p.headY = 0.3F;
            }
            case TEEN, ADULT -> {
                p.headX = -0.05F;
                p.headY = 0.25F;
            }
        }
        p.rightArmHold = 1.0F;
        p.headHold = 1.0F;
        return p;
    }

    /** Down to a little one: a crouch on bent knees (a player's), or a deeper lean (a model without knees). */
    private static void bend(RigPose p, boolean knees, ChildStage stage) {
        float low = stage == ChildStage.BABY || stage == ChildStage.TODDLER ? 1.0F : 0.6F;
        if (knees) {
            p.drop += 5.0F * low;
            p.rightLegX = -0.75F * low;
            p.leftLegX = -0.55F * low;
            p.rightKnee = 1.4F * low;
            p.leftKnee = 1.2F * low;
            p.lean += 0.38F * low;
            p.legHold = 1.0F;
        } else {
            p.lean += 0.55F * low;
        }
    }

    /** PEEKABOO: how open the hands are (0 over the face .. 1 flung open), twice. */
    static float peek(float t) {
        float first = smooth((t - 22.0) / 3.0) * (1.0F - smooth((t - 34.0) / 4.0));
        float second = smooth((t - 58.0) / 3.0) * (1.0F - smooth((t - 70.0) / 2.0));
        return Math.max(first, second);
    }

    /** SPIN: arms straight out holding it, leaning back against its weight, turning round. */
    private static void spinning(RigPose p, double age) {
        p.rightArmX = -1.42F;
        p.leftArmX = -1.42F;
        p.rightArmY = -0.18F;
        p.leftArmY = 0.18F;
        p.lean = -0.14F;
        p.headX = 0.1F;
        // 1.3.24: to their right, the way the child is carried round (a pose's spin + turns it left: it turned the other way).
        p.spin = (float)-ChildMoment.spinTurn(age);
        p.armHold = 1.0F;
    }

    /** Carrying it on the shoulders: both hands up holding its shins. */
    public static RigPose shouldersParent() {
        RigPose p = new RigPose();
        p.rightArmX = -2.0F;
        p.leftArmX = -2.0F;
        p.rightArmZ = 0.62F;
        p.leftArmZ = -0.62F;
        p.headX = -0.06F;
        p.armHold = 1.0F;
        return p;
    }

    // ---------------------------------------------------------------- the child

    /**
     * The child's pose in {@code moment} {@code age} ticks in at {@code stage}, as its own model takes it (arms, legs and head; the body's
     * hop as {@code drop} (negative up, model pixels), its sway as {@code roll}, a tip forward or back as {@code flip}). Null for none.
     */
    public static RigPose child(ChildMoment moment, double age, ChildStage stage) {
        RigPose p = new RigPose();
        float t = (float)age;
        boolean teen = stage.older();
        switch (moment) {
            case HUG -> {
                float hop = t < 10.0F ? -2.0F * sin(Math.PI * t / 10.0) : 0.0F;
                p.rightArmX = teen ? -1.5F : -1.85F;
                p.leftArmX = teen ? -1.5F : -1.85F;
                p.rightArmZ = -0.32F;
                p.leftArmZ = 0.32F;
                p.headX = teen ? 0.2F : -0.35F;
                p.drop = hop;
                p.roll = 0.05F * sin(t * 0.16);
                p.armHold = 1.0F;
            }
            case PAT -> {
                p.headX = teen ? 0.15F : -0.32F;
                p.headZ = 0.14F * sin(t * 0.35);
                p.rightArmZ = 0.18F;
                p.leftArmZ = -0.18F;
                p.drop = teen ? 0.0F : -0.6F * Math.abs(sin(t * 0.3));
            }
            case HIGH_FIVE -> {
                float up = smooth(t / 8.0) * (1.0F - smooth((t - 16.0) / 10.0));
                float slap = Math.max(0.0F, 1.0F - Math.abs(t - 12.0F) / 3.0F);
                p.leftArmX = -2.7F * up - 0.2F * slap;
                p.leftArmZ = 0.12F * up;
                p.headX = teen ? -0.05F : -0.3F;
                p.drop = -1.6F * slap;
                p.leftArmHold = up;
            }
            case TICKLE -> {
                p.rightArmX = -0.6F + 0.7F * sin(t * 1.1);
                p.leftArmX = -0.6F - 0.7F * sin(t * 1.3);
                p.rightArmZ = 0.45F + 0.3F * sin(t * 1.7);
                p.leftArmZ = -0.45F - 0.3F * sin(t * 1.5);
                p.headZ = 0.28F * sin(t * 0.9);
                p.headX = -0.15F;
                p.roll = 0.14F * sin(t * 1.2);
                p.drop = -0.8F * Math.abs(sin(t * 0.8));
                p.armHold = 1.0F;
                if (stage != ChildStage.BABY) {
                    p.rightLegX = 0.25F * sin(t * 1.4);
                    p.leftLegX = -0.25F * sin(t * 1.4);
                }
            }
            case PEEKABOO -> {
                float open = peek(t);
                // Still and staring while the face is hidden; at "boo!" arms up, a squeal, a bounce.
                p.headZ = 0.18F * (1.0F - open) * sin(t * 0.2);
                p.rightArmX = -0.4F - 1.3F * open;
                p.leftArmX = -0.4F - 1.3F * open;
                p.rightArmZ = 0.3F * open;
                p.leftArmZ = -0.3F * open;
                p.headX = -0.25F * open;
                p.drop = -1.2F * open * Math.abs(sin(t * 0.9));
                p.armHold = 1.0F;
            }
            case TOSS -> {
                double lift = ChildMoment.tossLift(age);
                float air = (float)Math.min(1.0, lift / 0.4);
                p.rightArmX = -0.5F - 2.3F * air;
                p.leftArmX = -0.5F - 2.3F * air;
                p.rightArmZ = 0.5F * air;
                p.leftArmZ = -0.5F * air;
                p.headX = -0.3F * air;
                p.rightLegX = 0.9F * air + 0.25F * air * sin(t * 0.9);
                p.leftLegX = 0.9F * air - 0.25F * air * sin(t * 0.9);
                p.armHold = 1.0F;
            }
            case SPIN -> {
                // Flying: arms out like wings, legs swung out behind it, its whole body tipped out flatter the faster it goes.
                double speed = Math.abs(ChildMoment.spinAngle(age + 1.0) - ChildMoment.spinAngle(age));
                float fly = (float)Math.min(1.0, speed / 0.3);
                p.rightArmX = -0.4F;
                p.leftArmX = -0.4F;
                p.rightArmZ = 1.2F;
                p.leftArmZ = -1.2F;
                p.rightLegX = 0.45F * fly + 0.1F * sin(t * 1.3);
                p.leftLegX = 0.45F * fly - 0.1F * sin(t * 1.3);
                p.rightLegZ = 0.15F;
                p.leftLegZ = -0.15F;
                p.headX = -0.4F;
                p.flip = -1.05F * fly;
                p.armHold = 1.0F;
                p.legHold = 1.0F;
            }
            case ARMS_UP -> {
                p.rightArmX = -2.85F;
                p.leftArmX = -2.85F;
                p.rightArmZ = 0.18F + 0.08F * sin(t * 0.7);
                p.leftArmZ = -0.18F - 0.08F * sin(t * 0.7);
                p.headX = -0.45F;
                p.drop = -1.0F * Math.abs(sin(t * 0.32));
                p.armHold = 1.0F;
            }
            case WAVE -> {
                float up = smooth(t / 5.0) * (1.0F - smooth((t - 30.0) / 6.0));
                p.rightArmX = -2.6F * up;
                p.rightArmZ = (0.35F + 0.35F * sin(t * 0.85)) * up;
                p.headZ = 0.12F * up;
                p.rightArmHold = up;
            }
            case CLAP -> {
                float meet = 0.5F + 0.5F * sin(t * 1.25);
                p.rightArmX = -1.15F;
                p.leftArmX = -1.15F;
                p.rightArmZ = -0.1F - 0.32F * meet;
                p.leftArmZ = 0.1F + 0.32F * meet;
                p.headX = -0.2F;
                p.drop = -0.7F * Math.abs(sin(t * 0.62));
                p.armHold = 1.0F;
            }
            case FEED -> {
                // Taken from their hand, then up to its mouth (in, across its face, as a horn is held) and munched, its head bobbing as it chews.
                float bite = 0.5F + 0.5F * sin(t * 0.8);
                float up = smooth((t - 8.0) / 5.0) * (1.0F - smooth((t - 50.0) / 8.0));
                p.rightArmX = (-1.4F - 0.22F * bite) * up;
                p.rightArmY = -0.55F * up;
                p.rightArmZ = 0.0F;
                p.headX = 0.12F + 0.1F * up * sin(t * 1.6);
                p.rightArmHold = up;
            }
            case TUCK -> {
                return null; // asleep: lying in its bed as the game lays it
            }
            case NUZZLE -> {
                // 4.1.15: its head ducked for the nuzzle, then up beaming, a little hop.
                float in = smooth(t / 12.0) * (1.0F - smooth((t - 32.0) / 12.0));
                float after = smooth((t - 32.0) / 6.0) * (1.0F - smooth((t - 42.0) / 6.0));
                p.headX = (teen ? 0.25F : 0.35F) * in - 0.35F * after;
                p.rightArmZ = 0.12F + 0.1F * after;
                p.leftArmZ = -0.12F - 0.1F * after;
                p.drop = teen ? 0.0F : -0.9F * after;
            }
            case BOOP -> {
                // 4.1.15: still, eyes on the finger; at the boop its head jerks back; then it wobbles, giggling.
                float jab = Math.max(0.0F, 1.0F - Math.abs(t - 12.5F) / 3.0F);
                float giggle = smooth((t - 14.0) / 3.0) * (1.0F - smooth((t - 28.0) / 6.0));
                p.headX = -0.2F - 0.3F * jab;
                p.headZ = 0.2F * giggle * sin(t * 1.3);
                p.rightArmX = -0.5F * giggle;
                p.leftArmX = -0.5F * giggle;
                p.rightArmZ = 0.3F * giggle;
                p.leftArmZ = -0.3F * giggle;
                p.drop = -0.7F * giggle * Math.abs(sin(t * 0.9));
                p.armHold = giggle;
            }
            case FIST_BUMP -> {
                // 4.1.15: its own fist out to meet theirs at 10, then up and open: boom.
                float out = smooth(t / 7.0) * (1.0F - smooth((t - 12.0) / 4.0));
                float boom = smooth((t - 11.0) / 4.0) * (1.0F - smooth((t - 20.0) / 9.0));
                p.leftArmX = (teen ? -1.3F : -1.6F) * out - 1.8F * boom;
                p.leftArmZ = 0.08F * out + 0.6F * boom;
                p.headX = teen ? 0.0F : -0.25F;
                p.drop = teen ? 0.0F : -1.0F * boom;
                p.leftArmHold = Math.max(out, boom);
            }
            case PATTY_CAKE -> {
                // 4.1.15: as theirs, mirrored: its own clap, then both palms up and out to theirs.
                float in = smooth(t / 6.0) * (1.0F - smooth((t - 72.0) / 8.0));
                float own = pattyOwn(t);
                p.rightArmX = (-1.25F - 0.35F * (1.0F - own)) * in;
                p.leftArmX = (-1.25F - 0.35F * (1.0F - own)) * in;
                p.rightArmZ = (0.08F + 0.3F * own - 0.1F * (1.0F - own)) * in;
                p.leftArmZ = (-0.08F - 0.3F * own + 0.1F * (1.0F - own)) * in;
                p.headX = -0.3F;
                p.drop = -0.5F * Math.abs(sin(t * 0.35)) * in;
                p.armHold = in;
            }
            case DANCE -> {
                // 4.1.15: both hands up in theirs, swaying the other way, stepping, bouncing.
                float in = smooth(t / 8.0) * (1.0F - smooth((t - 80.0) / 10.0));
                float sway = sin(t * 0.21);
                float level = switch (stage) {
                    case TODDLER -> -2.2F;
                    case CHILD -> -1.55F;
                    default -> -1.2F;
                };
                p.rightArmX = level * in;
                p.leftArmX = level * in;
                p.rightArmZ = 0.2F * in;
                p.leftArmZ = -0.2F * in;
                p.roll = -0.14F * sway * in;
                p.headZ = 0.12F * sway * in;
                p.headX = teen ? 0.0F : -0.35F;
                p.drop = -0.8F * Math.abs(sin(t * 0.42)) * in;
                p.rightLegX = -0.3F * sway * in;
                p.leftLegX = 0.3F * sway * in;
                p.armHold = in;
                p.legHold = in;
            }
            case FLOWER -> {
                // Held up to them, proud, then given (the arm stays out a moment, then down).
                float out = smooth(t / 8.0) * (1.0F - smooth((t - 44.0) / 10.0));
                p.rightArmX = -1.5F * out;
                p.rightArmZ = -0.08F * out;
                p.headX = -0.35F * out;
                p.rightArmHold = out;
                p.drop = t > 30.0F && t < 40.0F ? -0.8F * sin(Math.PI * (t - 30.0) / 10.0) : 0.0F;
            }
            default -> {
                return null;
            }
        }
        return p;
    }

    /** Up on a parent's shoulders: its legs down their front either side of the neck, its hands on their head, looking about. */
    public static RigPose shouldersChild(double time) {
        RigPose p = new RigPose();
        p.rightLegX = -0.95F;
        p.leftLegX = -0.95F;
        p.rightLegY = 0.3F;
        p.leftLegY = -0.3F;
        p.rightLegZ = 0.62F;
        p.leftLegZ = -0.62F;
        p.rightArmX = -2.15F;
        p.leftArmX = -2.15F;
        p.rightArmZ = -0.12F;
        p.leftArmZ = 0.12F;
        p.headY = 0.35F * sin(time * 0.045);
        p.headX = -0.1F;
        p.armHold = 1.0F;
        p.legHold = 1.0F;
        return p;
    }

    /** How a child held {@code ride} is drawn ({@code ChildRules.RIDE_*}) when no moment poses it: null for in the arms (its own sitting). */
    public static RigPose ride(int ride, double time) {
        return ride == ChildRules.RIDE_SHOULDERS ? shouldersChild(time) : null;
    }

    /** 1.3.21: as above, knowing its stage and its parent's size (hand in hand: its arm to their hand), and its skin's arms. */
    public static RigPose ride(int ride, ChildStage stage, double time, double parent, boolean slim) {
        return ride == ChildRules.RIDE_HAND ? handChild(stage, parent, slim) : ride(ride, time);
    }

    // ---------------------------------------------------------------- blending

    /**
     * {@code moment}'s pose laid onto {@code p} by {@code w}: arms and legs it holds taken toward it (and held from vanilla's swing), the
     * head, lean, hop, sway, spin added.
     */
    public static void blend(RigPose p, RigPose m, float w) {
        if (m == null || w <= 0.0F) {
            return;
        }
        float arms = Math.max(m.armHold, Math.max(m.rightArmHold, m.leftArmHold));
        if (m.armHold > 0.0F || m.rightArmHold > 0.0F) {
            float k = w * Math.max(m.armHold, m.rightArmHold);
            p.rightArmX += (m.rightArmX - p.rightArmX) * k;
            p.rightArmY += (m.rightArmY - p.rightArmY) * k;
            p.rightArmZ += (m.rightArmZ - p.rightArmZ) * k;
            p.rightArmHold = Math.max(p.rightArmHold, k);
            // 3.4.8: a kagune-made weapon turned in that fist (WeaponPose) goes with it.
            p.weaponX += (m.weaponX - p.weaponX) * k;
            p.weaponY += (m.weaponY - p.weaponY) * k;
            p.weaponZ += (m.weaponZ - p.weaponZ) * k;
        }
        if (m.armHold > 0.0F || m.leftArmHold > 0.0F) {
            float k = w * Math.max(m.armHold, m.leftArmHold);
            p.leftArmX += (m.leftArmX - p.leftArmX) * k;
            p.leftArmY += (m.leftArmY - p.leftArmY) * k;
            p.leftArmZ += (m.leftArmZ - p.leftArmZ) * k;
            p.leftArmHold = Math.max(p.leftArmHold, k);
        }
        if (arms <= 0.0F) {
            p.rightArmX += m.rightArmX * w;
            p.rightArmY += m.rightArmY * w;
            p.rightArmZ += m.rightArmZ * w;
            p.leftArmX += m.leftArmX * w;
            p.leftArmY += m.leftArmY * w;
            p.leftArmZ += m.leftArmZ * w;
        }
        if (m.legHold > 0.0F) {
            float k = w * m.legHold;
            p.rightLegX += (m.rightLegX - p.rightLegX) * k;
            p.leftLegX += (m.leftLegX - p.leftLegX) * k;
            p.rightLegY += (m.rightLegY - p.rightLegY) * k;
            p.leftLegY += (m.leftLegY - p.leftLegY) * k;
            p.rightLegZ += (m.rightLegZ - p.rightLegZ) * k;
            p.leftLegZ += (m.leftLegZ - p.leftLegZ) * k;
            p.rightKnee += (m.rightKnee - p.rightKnee) * k;
            p.leftKnee += (m.leftKnee - p.leftKnee) * k;
            p.legHold = Math.max(p.legHold, k);
        } else {
            p.rightLegX += m.rightLegX * w;
            p.leftLegX += m.leftLegX * w;
        }
        p.headX += m.headX * w;
        p.headY += m.headY * w;
        p.headZ += m.headZ * w;
        p.lean += m.lean * w;
        p.drop += m.drop * w;
        p.roll += m.roll * w;
        p.flip += m.flip * w;
        p.spin += m.spin * w;
        p.twist += m.twist * w; // 1.3.26: a quinque's swing turns the chest and steps in
        p.lunge += m.lunge * w;
    }
}
