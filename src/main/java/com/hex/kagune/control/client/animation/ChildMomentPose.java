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
        return stage != ChildStage.TEEN;
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
                    case TEEN -> -1.75F;
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
                p.rightArmX = (stage == ChildStage.TEEN ? -2.5F : -2.05F) * up - 0.2F * slap;
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
            default -> {
            }
        }
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
        p.spin = (float)ChildMoment.spinAngle(age);
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
        boolean teen = stage == ChildStage.TEEN;
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
    }
}
