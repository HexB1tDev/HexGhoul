package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.family.ChildCue;
import com.hex.kagune.control.family.ChildStage;

/**
 * 1.3.57: a child's body as it says a line in a talk ({@link ChildCue}; chosen by {@code TalkCues}): arms folded, a stamp of the foot, a
 * pointing finger, hands on hips, a shrug, a sorry with its head down, a cheer, a giggle behind its hand, a whisper, a "bleh", a pat on the
 * other's shoulder, a sulk, a hand to its chin, a wave, a plea. In the child model's terms ({@code ChildRenderer.Model.turn}: held arms and
 * legs take the pose, the head adds it to where it looks; the body's hop as {@code drop}, its sway as {@code roll}, a tip as {@code flip}).
 * Carried in someone's arms, its legs are left as they sit.
 */
public final class ChildTalkPose {
    private ChildTalkPose() {
    }

    /** {@code cue}'s pose {@code age} ticks in, at {@code stage}, carried or on its feet; null for none. */
    public static RigPose pose(ChildCue cue, double age, ChildStage stage, boolean carried) {
        if (cue == null || cue == ChildCue.NONE) {
            return null;
        }
        RigPose p = new RigPose();
        float t = (float)age;
        boolean little = stage != null && stage.young;
        switch (cue) {
            case CROSS -> {
                folded(p, 0.0F);
                p.headX = -0.12F;
                p.headY = 0.3F * smooth(t / 10.0);
            }
            case STAMP -> {
                // Fists down and a little back, a stamp of the right foot at tick 8 (and a smaller one at 20), its head down with it.
                p.rightArmX = 0.28F;
                p.leftArmX = 0.28F;
                p.rightArmZ = 0.16F;
                p.leftArmZ = -0.16F;
                p.armHold = 1.0F;
                float stomp = bump(t, 3.0F, 8.0F) + 0.6F * bump(t, 15.0F, 20.0F);
                if (!carried) {
                    p.rightLegX = -0.38F * stomp;
                    p.rightLegZ = 0.06F * stomp;
                    p.legHold = 1.0F;
                }
                float hit = Math.max(0.0F, 1.0F - Math.abs(t - 8.5F) / 2.0F) + 0.6F * Math.max(0.0F, 1.0F - Math.abs(t - 20.5F) / 2.0F);
                p.drop = 0.5F * hit;
                p.headX = 0.25F * hit + 0.08F;
            }
            case POINT -> {
                // The right arm straight out at the other, jabbing with each word; the left fist on its hip.
                float up = smooth(t / 5.0);
                p.rightArmX = (-1.62F + 0.1F * (float)Math.sin(t * 0.9)) * up;
                p.rightArmY = 0.05F * up;
                p.rightArmZ = 0.0F;
                p.rightArmHold = up;
                p.leftArmX = 0.3F;
                p.leftArmZ = -0.55F;
                p.leftArmHold = 1.0F;
                p.headX = -0.05F;
                p.flip = 0.06F * up;
            }
            case HIPS -> {
                hips(p);
                p.headX = -0.22F;
                p.flip = -0.05F;
                p.drop = -0.3F * (float)Math.abs(Math.sin(t * 0.25));
            }
            case SHRUG -> {
                float up = bump(t, 2.0F, 12.0F) + bump(t, 22.0F, 32.0F) * 0.5F;
                p.rightArmX = -0.55F;
                p.leftArmX = -0.55F;
                p.rightArmY = 0.35F;
                p.leftArmY = -0.35F;
                p.rightArmZ = 0.35F + 0.25F * up;
                p.leftArmZ = -0.35F - 0.25F * up;
                p.armHold = 1.0F;
                p.drop = -0.7F * up;
                p.headZ = 0.22F * smooth(t / 6.0);
            }
            case SORRY -> {
                // Head down, hands behind its back, a toe scuffing the ground.
                p.rightArmX = 0.42F;
                p.leftArmX = 0.42F;
                p.rightArmZ = -0.18F;
                p.leftArmZ = 0.18F;
                p.armHold = 1.0F;
                p.headX = 0.55F;
                p.headZ = 0.06F * (float)Math.sin(t * 0.15);
                if (!carried) {
                    p.rightLegX = -0.18F + 0.14F * (float)Math.sin(t * 0.35);
                    p.rightLegZ = -0.08F;
                    p.legHold = 1.0F;
                }
            }
            case CHEER -> {
                p.rightArmX = -2.9F;
                p.leftArmX = -2.9F;
                p.rightArmZ = 0.3F;
                p.leftArmZ = -0.3F;
                p.armHold = 1.0F;
                p.headX = -0.35F;
                p.drop = carried ? 0.0F : -1.6F * (float)Math.abs(Math.sin(t * 0.32));
            }
            case GIGGLE -> {
                // A hand over its mouth, shoulders shaking, head down a little.
                p.rightArmX = -1.85F;
                p.rightArmY = -0.9F;
                p.rightArmZ = -0.4F;
                p.rightArmHold = 1.0F;
                p.headX = 0.15F;
                p.roll = 0.05F * (float)Math.sin(t * 1.7);
                p.drop = -0.35F * (float)Math.abs(Math.sin(t * 1.3));
            }
            case WHISPER -> {
                // Leaning in toward the other, a hand by its mouth.
                p.rightArmX = -1.8F;
                p.rightArmY = -0.75F;
                p.rightArmZ = -0.35F;
                p.rightArmHold = 1.0F;
                p.headX = 0.12F;
                p.headZ = -0.12F;
                p.flip = 0.16F;
            }
            case BLEH -> {
                // Head waggling side to side, leaning back, hands up by its ears.
                p.rightArmX = -2.6F;
                p.leftArmX = -2.6F;
                p.rightArmZ = 0.85F;
                p.leftArmZ = -0.85F;
                p.armHold = 1.0F;
                p.headZ = 0.3F * (float)Math.sin(t * 0.55);
                p.headX = -0.15F;
                p.flip = -0.08F;
            }
            case PAT -> {
                // Its hand out to the other's shoulder, patting.
                float out = smooth(t / 6.0);
                p.rightArmX = (-1.35F + 0.12F * (float)Math.sin(t * 0.5)) * out;
                p.rightArmZ = -0.1F * out;
                p.rightArmHold = out;
                p.headZ = 0.15F * out;
                p.headX = 0.08F;
                p.flip = 0.05F * out;
            }
            case SULK -> {
                folded(p, 0.25F);
                p.headX = 0.42F;
                p.headY = 0.45F * smooth(t / 12.0);
                p.roll = -0.04F;
            }
            case THINK -> {
                // A hand to its chin, the other arm across under its elbow, head tipped.
                p.rightArmX = -1.65F;
                p.rightArmY = -0.85F;
                p.rightArmZ = -0.5F;
                p.leftArmX = -0.75F;
                p.leftArmY = 0.35F;
                p.leftArmZ = 0.55F;
                p.armHold = 1.0F;
                p.headZ = 0.2F * smooth(t / 8.0);
                p.headX = -0.1F;
            }
            case WAVE -> {
                float up = smooth(t / 5.0) * (1.0F - smooth((t - 28.0) / 6.0));
                p.rightArmX = -2.6F * up;
                p.rightArmZ = (0.35F + 0.35F * (float)Math.sin(t * 0.85)) * up;
                p.headZ = 0.12F * up;
                p.rightArmHold = up;
            }
            case PLEAD -> {
                // Hands together under its chin, bouncing on its toes.
                p.rightArmX = -1.75F;
                p.leftArmX = -1.75F;
                p.rightArmY = -0.25F;
                p.leftArmY = 0.25F;
                p.rightArmZ = -0.42F;
                p.leftArmZ = 0.42F;
                p.armHold = 1.0F;
                p.headZ = 0.18F;
                p.headX = -0.2F;
                p.drop = carried ? 0.0F : -0.7F * (float)Math.abs(Math.sin(t * 0.45));
            }
            case HIDE -> {
                // Crouched down small (its legs folded forward under it, as near as a model without knees comes), arms round its knees,
                // peeking about.
                if (!carried) {
                    p.rightLegX = -1.3F;
                    p.leftLegX = -1.3F;
                    p.rightLegY = 0.28F;
                    p.leftLegY = -0.28F;
                    p.legHold = 1.0F;
                    p.drop = little ? 4.3F : 8.6F; // a little one's legs are half as long
                }
                p.rightArmX = -1.15F;
                p.leftArmX = -1.15F;
                p.rightArmZ = -0.22F;
                p.leftArmZ = 0.22F;
                p.armHold = 1.0F;
                p.headX = -0.1F;
                p.headY = 0.35F * (float)Math.sin(t * 0.05);
            }
            case COUNT -> {
                // Both hands over its eyes, head down a little, rocking as it counts (a little one's short arms only reach its cheeks: its
                // hands pressed to its face, its big head bowed into them).
                p.rightArmX = little ? -1.9F : -2.15F;
                p.leftArmX = little ? -1.9F : -2.15F;
                p.rightArmY = little ? -0.3F : -0.75F;
                p.leftArmY = little ? 0.3F : 0.75F;
                p.rightArmZ = little ? -0.35F : -0.3F;
                p.leftArmZ = little ? 0.35F : 0.3F;
                p.armHold = 1.0F;
                p.headX = little ? 0.45F : 0.15F;
                p.roll = 0.03F * (float)Math.sin(t * 0.3);
            }
            default -> {
                return null;
            }
        }
        if (little && cue != ChildCue.COUNT) {
            // A little one's big head: a touch less of a nod.
            p.headX *= 0.8F;
        }
        return p;
    }

    /** Arms folded across its chest ({@code low}: lower, for a sulk). */
    private static void folded(RigPose p, float low) {
        p.rightArmX = -0.95F + low;
        p.leftArmX = -0.95F + low;
        p.rightArmY = -0.15F;
        p.leftArmY = 0.15F;
        p.rightArmZ = -0.78F;
        p.leftArmZ = 0.78F;
        p.armHold = 1.0F;
    }

    /**
     * Hands on its hips, as near as a model without elbows comes to it: arms out from its sides and a little back, turned in at the
     * hands, chest out.
     */
    private static void hips(RigPose p) {
        p.rightArmX = 0.32F;
        p.leftArmX = 0.32F;
        p.rightArmY = -0.25F;
        p.leftArmY = 0.25F;
        p.rightArmZ = 0.48F;
        p.leftArmZ = -0.48F;
        p.armHold = 1.0F;
    }

    /** Up from {@code from} to its top at {@code to}, back down as fast after (0..1). */
    private static float bump(float t, float from, float to) {
        float half = to - from;
        if (t < from || t > to + half) {
            return 0.0F;
        }
        return t <= to ? smooth((t - from) / half) : 1.0F - smooth((t - to) / half);
    }

    private static float smooth(double x) {
        double c = Math.max(0.0, Math.min(1.0, x));
        return (float)(c * c * (3.0 - 2.0 * c));
    }
}
