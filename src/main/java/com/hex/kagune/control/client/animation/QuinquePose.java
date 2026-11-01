package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 1.3.26: how a body holds and swings a quinque (players and the CCG's investigators alike; pure).
 *
 * <p>The two-handed ones each have a stance, solved on the real models with their third person hold (tools/quinque_models.py): the
 * right hand on the grip where the item is held, the left on the lower grip (the spear's back, the katana's and the mace's pommel end,
 * the launcher's fore grip under its magazine), the weapon up and forward (the katana in a kamae; the launcher level at the hip). The
 * one-handed blade is held as a sword is. Each swings three ways, a string of M1s ({@link Quinque#next}): a windup, the cut, the
 * recovery; the third the finisher. A two-handed swing moves both arms together (the left after the right).
 */
public final class QuinquePose {
    private QuinquePose() {
    }

    /** The stance's right arm and left arm (x, y, z: RigPose's), or null for a one-handed quinque. */
    static float[][] stance(Quinque q) {
        return switch (q) {
            case SPEAR -> new float[][] {{-1.4F, 0.15F, 0.1F}, {-0.2F, -0.6F, 0.9F}};
            case KATANA -> new float[][] {{-1.4F, -0.1F, -0.15F}, {-0.65F, 0.4F, 0.55F}};
            case MACE -> new float[][] {{-1.4F, 0.15F, 0.1F}, {-0.35F, 0.0F, 0.95F}};
            case LAUNCHER -> new float[][] {{-0.75F, -0.35F, 0.0F}, {-0.85F, -0.3F, 1.0F}};
            default -> null;
        };
    }

    /** The one-handed blade's arm at rest (a held item's). */
    static final float[] HELD = {-0.31F, 0.0F, 0.0F};

    /** Its stance (both arms held), or null (the blade: vanilla holds it). */
    public static RigPose hold(Quinque q) {
        float[][] s = stance(q);
        if (s == null) {
            return null;
        }
        RigPose p = new RigPose();
        p.rightArmX = s[0][0];
        p.rightArmY = s[0][1];
        p.rightArmZ = s[0][2];
        p.leftArmX = s[1][0];
        p.leftArmY = s[1][1];
        p.leftArmZ = s[1][2];
        p.armHold = 1.0F;
        return p;
    }

    /** One swing's shape: the right arm's windup and cut (x, y, z on top of its stance), the body's twist, lean, lunge and drop at each. */
    record Shape(float[] windup, float[] cut, float twistUp, float twistCut, float leanCut, float lungeUp, float lungeCut, float dropCut) {
    }

    static Shape shape(Quinque q, int step) {
        return switch (q) {
            case BLADE -> switch (step) {
                case 0 -> new Shape(f(-1.2F, 0.9F, 0.35F), f(-1.25F, -1.0F, 0.0F), 0.35F, -0.45F, 0.0F, 0.0F, 1.0F, 0.0F);
                case 1 -> new Shape(f(-1.3F, -0.9F, 0.0F), f(-1.1F, 0.9F, 0.3F), -0.35F, 0.45F, 0.0F, 0.0F, 1.0F, 0.0F);
                default -> new Shape(f(-2.6F, 0.0F, 0.1F), f(-0.4F, -0.1F, 0.0F), 0.0F, 0.0F, 0.35F, 0.0F, 2.0F, 0.0F);
            };
            case KATANA -> switch (step) {
                case 0 -> new Shape(f(-0.9F, 0.35F, 0.0F), f(0.95F, -0.45F, 0.0F), 0.3F, -0.45F, 0.15F, 0.0F, 1.5F, 0.0F);
                case 1 -> new Shape(f(0.7F, -0.35F, 0.0F), f(-0.9F, 0.45F, 0.0F), -0.3F, 0.45F, 0.0F, 0.0F, 1.5F, 0.0F);
                default -> new Shape(f(0.15F, 0.9F, 0.0F), f(0.25F, -1.0F, 0.0F), 0.8F, -1.0F, 0.1F, -1.0F, 3.0F, 1.0F);
            };
            case SPEAR -> switch (step) {
                case 0 -> new Shape(f(0.35F, 0.0F, 0.0F), f(-0.15F, 0.0F, 0.0F), 0.1F, -0.1F, 0.1F, -1.0F, 4.0F, 0.0F);
                case 1 -> new Shape(f(0.3F, 0.15F, 0.0F), f(-0.1F, -0.1F, 0.0F), -0.1F, 0.1F, 0.1F, -1.0F, 3.0F, 0.0F);
                default -> new Shape(f(0.15F, 0.7F, 0.0F), f(-0.05F, -0.65F, 0.0F), 0.5F, -0.5F, 0.15F, -1.5F, 6.0F, 0.5F);
            };
            case MACE -> switch (step) {
                case 0 -> new Shape(f(0.2F, 1.0F, 0.0F), f(0.35F, -1.0F, 0.0F), 0.8F, -0.9F, 0.1F, 0.0F, 1.0F, 0.5F);
                case 1 -> new Shape(f(0.2F, -0.9F, 0.0F), f(0.35F, 0.9F, 0.0F), -0.8F, 0.9F, 0.1F, 0.0F, 1.0F, 0.5F);
                default -> new Shape(f(-1.3F, 0.0F, 0.0F), f(1.25F, 0.0F, 0.0F), 0.0F, 0.0F, 0.45F, -1.0F, 2.0F, 3.0F);
            };
            case LAUNCHER -> new Shape(f(0.25F, 0.0F, 0.0F), f(-0.45F, 0.0F, 0.0F), 0.0F, 0.0F, 0.1F, 0.0F, 2.0F, 0.0F);
        };
    }

    private static float[] f(float x, float y, float z) {
        return new float[] {x, y, z};
    }

    static float ease(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return (float)(c * c * (3.0 - 2.0 * c));
    }

    /** How far through its windup, cut and recovery a swing {@code t} through is: the delta's weight on (windup, cut). */
    static float[] phase(double t) {
        if (t < 0.35) {
            return new float[] {ease(t / 0.35), 0.0F};
        }
        if (t < 0.55) {
            float k = ease((t - 0.35) / 0.2);
            return new float[] {1.0F - k, k};
        }
        return new float[] {0.0F, 1.0F - ease((t - 0.55) / 0.45)};
    }

    /**
     * Its {@code step}th swing {@code t} through (0..1 over {@link Quinque#swing} ticks): its stance (or a held blade) with the windup
     * and the cut on the arms (both, for a two-handed one) and the body.
     */
    public static RigPose swing(Quinque q, int step, double t) {
        float[][] s = stance(q);
        Shape shape = shape(q, Math.floorMod(step, 3));
        float[] w = phase(t);
        float[] d = new float[3];
        for (int i = 0; i < 3; i++) {
            d[i] = shape.windup()[i] * w[0] + shape.cut()[i] * w[1];
        }
        RigPose p = new RigPose();
        float[] right = s == null ? HELD : s[0];
        p.rightArmX = right[0] + d[0];
        p.rightArmY = right[1] + d[1];
        p.rightArmZ = right[2] + d[2];
        p.rightArmHold = 1.0F;
        if (s != null) {
            // The left hand goes with the right (on the same grip).
            p.leftArmX = s[1][0] + d[0];
            p.leftArmY = s[1][1] + d[1];
            p.leftArmZ = s[1][2];
            p.leftArmHold = 1.0F;
        }
        p.twist = shape.twistUp() * w[0] + shape.twistCut() * w[1];
        p.lean = shape.leanCut() * w[1];
        p.lunge = shape.lungeUp() * w[0] + shape.lungeCut() * w[1];
        p.drop = shape.dropCut() * w[1];
        return p;
    }

    /**
     * Its stance as it opens out of its case ({@code open} 0 the case in the hand .. 1 out): the arms come up from hanging into it; as it
     * shuts, the same backwards.
     */
    public static RigPose unfolding(Quinque q, float open) {
        RigPose hold = hold(q);
        if (hold == null) {
            return null;
        }
        float k = ease(open);
        RigPose p = new RigPose();
        p.rightArmX = HELD[0] + (hold.rightArmX - HELD[0]) * k;
        p.rightArmY = hold.rightArmY * k;
        p.rightArmZ = hold.rightArmZ * k;
        p.leftArmX = hold.leftArmX * k;
        p.leftArmY = hold.leftArmY * k;
        p.leftArmZ = hold.leftArmZ * k;
        p.rightArmHold = 1.0F;
        p.leftArmHold = k;
        return p;
    }

    /** The launcher's kick as a shard leaves ({@code kick} 1 at the shot, dying away): the barrel jumps, both arms with it. */
    public static void recoil(RigPose p, float kick) {
        if (kick <= 0.0F) {
            return;
        }
        p.rightArmX += 0.14F * kick;
        p.leftArmX += 0.14F * kick;
        p.lunge -= 0.8F * kick;
    }
}
