package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 1.3.26: how a body holds and swings a quinque (players and the CCG's investigators alike; pure).
 *
 * <p>1.3.33, rebuilt. Each two-handed one has a stance solved on its real model with its third person transform
 * ({@link QuinqueHolds}, tools/quinque_models.py): the spear low and level at the hip, the katana in chudan, the mace on the right
 * shoulder, the launcher a rifle (the body bladed, the stock in the right shoulder, the barrel along the look). The right arm carries the
 * weapon; the left is put on its grip every frame by {@link QuinqueGrip#reach} (the caller's last step, after everything else has moved
 * the body), so both hands stay on it through every swing. The one-handed blade is held as a sword is.
 *
 * <p>Each swing is a string of keys on the right arm (where it points: pitch, yaw and roll about its own length, as offsets from the
 * stance) and on the body (the chest's twist, the lean, a step in, a drop), drawn through on a smooth curve: a windup, the cut through
 * its impact at speed, the follow through, back to the stance. Three to a string ({@link Quinque#next}), the third the finisher. The
 * owner: "the combos for it are pretty bad... the M1 animations with those weapons are pretty bad".
 *
 * <p>A stance's bladed turn is shared between the body (its spin: the feet turned with it, the head turned back to the look) and the
 * chest (its twist), so a rifle stance never twists the waist half round.
 */
public final class QuinquePose {
    private QuinquePose() {
    }

    /** The one-handed blade's arm at rest (a held item's: vanilla's). */
    static final float[] HELD = {-0.31F, 0.0F, 0.0F};
    /** How much of a stance's turn the whole body takes (the rest is the chest's), at most. */
    static final float BODY_TURN = 0.4F;

    /** The share of {@code twist} the body takes (its spin, the other way about: RigPose's spin turns it to its left). */
    static float bodyTurn(float twist) {
        return Math.max(-BODY_TURN, Math.min(BODY_TURN, twist * 0.45F));
    }

    /**
     * Its stance: a two-handed one's both arms held (the left as a hint, put on its grip by QuinqueGrip.reach); 1.3.33 the one-handed
     * blade en garde (its right arm alone).
     */
    public static RigPose hold(Quinque q) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        if (h == null) {
            return null;
        }
        RigPose p = new RigPose();
        stance(p, h, 1.0F);
        QuinqueGrip.aimRight(p, h.pitch(), h.yaw(), h.roll());
        if (q.twoHanded) {
            p.leftArmX = h.left()[0];
            p.leftArmY = h.left()[1];
            p.leftArmZ = h.left()[2];
            p.armHold = 1.0F;
        } else {
            p.rightArmHold = 1.0F;
        }
        if (q == Quinque.LAUNCHER) {
            // The cheek down on the stock, looking along it.
            p.headX += 0.1F;
            p.headZ -= 0.12F;
            p.lean += 0.05F;
        }
        return p;
    }

    /** The stance's turn on {@code p} ({@code k} of it): the body's share as a spin (the head kept on the look), the rest the chest's. */
    static void stance(RigPose p, QuinqueHolds.Hold h, float k) {
        float body = bodyTurn(h.twist()) * k;
        p.spin -= body;
        p.headY -= body;
        p.twist += (h.twist() * k - body);
    }

    /** One key of a swing: when (0..1), the right arm's pitch, yaw and roll from the stance's, and the body's twist, lean, lunge, drop. */
    record Key(float t, float pitch, float yaw, float roll, float twist, float lean, float lunge, float drop) {
    }

    private static Key k(double t, double pitch, double yaw, double roll, double twist, double lean, double lunge, double drop) {
        return new Key((float)t, (float)pitch, (float)yaw, (float)roll, (float)twist, (float)lean, (float)lunge, (float)drop);
    }

    /** At rest in the stance (a swing's first and last key). */
    private static Key rest(double t) {
        return k(t, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Its {@code step}th swing's keys. A windup by 0.3, the impact at 0.5 (the cut at its fastest there), the follow through by 0.68,
     * back in the stance at 1. Written against each stance (see the class note), checked in tools/QuinquePreview.java.
     */
    static Key[] keys(Quinque q, int step) {
        return switch (q) {
            case BLADE -> switch (step) {
                // A flat cut from the right across to the left: the arm out to the right, the thumb turned back so the blade lies level
                // out to the right, then swept round in front and on to the left, its point trailing the hand, the chest turning
                // through it.
                case 0 -> new Key[] {rest(0), k(0.3, -0.75, 0.95, 1.57, 0.4, 0, 0, 0), k(0.5, -0.7, 0.05, 1.57, -0.15, 0.1, 1.5, 0.2),
                    k(0.68, -0.65, -0.7, 1.57, -0.45, 0.12, 1.2, 0.2), rest(1)};
                // Back the other way, rising: low on the left, the thumb turned out to the left, up across to high on the right.
                case 1 -> new Key[] {rest(0), k(0.3, -0.2, -0.55, -1.57, -0.4, 0.05, 0, 0.3), k(0.5, -0.9, 0.45, -1.57, 0.15, 0.05, 1.5, 0),
                    k(0.68, -1.25, 0.9, -1.57, 0.4, -0.05, 1.0, 0), rest(1)};
                // The finisher: up over the head (the blade back behind it) and straight down through it, stepping in.
                default -> new Key[] {rest(0), k(0.3, -1.9, 0.25, 0, 0.1, -0.12, -0.5, 0), k(0.5, -0.3, 0.0, 0, -0.1, 0.3, 3.0, 0.4),
                    k(0.68, 0.2, -0.05, 0, -0.15, 0.35, 3.0, 0.5), rest(1)};
            };
            // 2.0.31 (the owner: "We need proper M1 combat on that ... weapon"): Dawn's own, a three-block lance in one hand swung as a
            // sword from its stance at the hip (it borrowed the katana's two-handed chops before, every one ending its point in the ground).
            case DAWN -> switch (step) {
                // A wide flat sweep at chest height: drawn out to the right, the chest turned with it, then carried round in front and on to
                // the left, its length level all the way.
                case 0 -> new Key[] {rest(0), k(0.3, 0.05, 1.05, 0, 0.55, -0.05, -0.5, 0), k(0.5, -0.05, 0.05, 0, -0.2, 0.12, 2.0, 0.3),
                    k(0.68, 0.0, -0.85, 0, -0.55, 0.15, 1.6, 0.3), rest(1)};
                // Back the other way, rising: low on the left, up across the front to high on the right.
                case 1 -> new Key[] {rest(0), k(0.3, 0.55, -0.75, 0, -0.45, 0.1, 0.0, 0.5), k(0.5, -0.35, 0.25, 0, 0.2, 0.0, 2.2, 0.2),
                    k(0.68, -0.95, 0.75, 0, 0.45, -0.08, 1.8, 0.0), rest(1)};
                // The finisher: drawn back with the chest (the arm turning against it so the lance stays on the line), sunk, then driven
                // out its whole length with a long lunge.
                default -> new Key[] {rest(0), k(0.3, 0.08, -0.55, 0, 0.6, -0.12, -2.5, 0.8), k(0.5, -0.38, 0.45, 0, -0.5, 0.3, 7.5, 0.9),
                    k(0.68, -0.38, 0.47, 0, -0.52, 0.32, 7.5, 1.0), rest(1)};
            };
            case KATANA -> switch (step) {
                // Kesa-giri: from over the right shoulder down across to the left hip.
                case 0 -> new Key[] {rest(0), k(0.3, -1.0, 0.45, -0.35, 0.35, -0.05, -0.5, 0), k(0.5, 0.15, -0.25, 0.35, -0.3, 0.2, 2.5, 0.8),
                    k(0.68, 0.3, -0.4, 0.45, -0.45, 0.25, 2.2, 1.0), rest(1)};
                // Gyaku-kesa: from low on the left, rising across to high on the right.
                case 1 -> new Key[] {rest(0), k(0.3, 0.3, -0.45, 0.6, -0.4, 0.15, 0, 0.6), k(0.5, -0.6, 0.25, -0.2, 0.25, 0.05, 2.5, 0.2),
                    k(0.68, -1.0, 0.4, -0.35, 0.4, -0.05, 2.0, 0), rest(1)};
                // Shomen, the finisher: straight up over the head and down the middle with a long step.
                default -> new Key[] {rest(0), k(0.3, -1.45, 0.0, 0, 0, -0.15, -1.0, 0), k(0.5, 0.05, 0.0, 0, 0, 0.3, 4.0, 0.6),
                    k(0.68, 0.25, 0.0, 0, 0, 0.38, 4.0, 0.8), rest(1)};
            };
            case SPEAR -> switch (step) {
                // A straight thrust, driven along its own length: the chest turns back to draw it (the arm turning against it so it
                // stays on the line), then squares up and the hips go in behind it.
                case 0 -> new Key[] {rest(0), k(0.3, 0.05, -0.35, 0, 0.35, -0.05, -1.5, 0), k(0.5, -0.15, 0.4, 0, -0.4, 0.15, 5.0, 0.3),
                    k(0.68, -0.16, 0.42, 0, -0.42, 0.16, 5.0, 0.3), rest(1)};
                // A second, higher, at the face.
                case 1 -> new Key[] {rest(0), k(0.3, -0.05, -0.3, 0, 0.3, -0.05, -1.0, 0), k(0.5, -0.26, 0.4, 0, -0.4, 0.1, 4.5, 0),
                    k(0.68, -0.27, 0.42, 0, -0.42, 0.1, 4.5, 0), rest(1)};
                // The finisher: sunk low, drawn far back, then lunging out its whole length.
                default -> new Key[] {rest(0), k(0.3, 0.05, -0.5, 0, 0.5, -0.1, -2.5, 1.0), k(0.5, -0.32, 0.55, 0, -0.55, 0.3, 8.0, 0.8),
                    k(0.68, -0.34, 0.55, 0, -0.55, 0.32, 8.0, 0.9), rest(1)};
            };
            case MACE -> switch (step) {
                // Raised over the right shoulder and brought down across the front.
                case 0 -> new Key[] {rest(0), k(0.3, -1.1, 0.3, -0.35, 0.5, -0.1, -0.5, 0), k(0.5, 0.45, -0.15, 0.35, -0.45, 0.3, 2.0, 0.5),
                    k(0.68, 0.6, -0.25, 0.45, -0.6, 0.35, 2.0, 0.6), rest(1)};
                // Back the other way: laid flat out to the left and swung across to the right, the whole chest behind it.
                case 1 -> new Key[] {rest(0), k(0.3, -0.3, -0.3, -1.3, -0.75, 0, 0, 0.3), k(0.5, -0.3, 0.1, -1.3, 0.65, 0.1, 1.5, 0.4),
                    k(0.68, -0.25, 0.25, -1.3, 0.95, 0.12, 1.5, 0.4), rest(1)};
                // The finisher: straight up and slammed down into the ground before it.
                default -> new Key[] {rest(0), k(0.3, -1.45, 0.0, 0, 0, -0.2, -0.5, 0), k(0.5, 0.45, 0.0, 0, 0, 0.6, 2.5, 0.8),
                    k(0.68, 0.55, 0.0, 0, 0, 0.65, 2.5, 1.0), rest(1)};
            };
            // The launcher clubs with its muzzle brake: drawn in, the whole body driven in behind it.
            case LAUNCHER -> new Key[] {rest(0), k(0.3, -0.2, -0.3, 0, 0.35, -0.08, -1.5, 0), k(0.5, 0.12, 0.25, 0, -0.25, 0.2, 4.0, 0.3),
                k(0.68, 0.15, 0.25, 0, -0.26, 0.22, 4.0, 0.3), rest(1)};
        };
    }

    static float ease(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return (float)(c * c * (3.0 - 2.0 * c));
    }

    /** The keys' values at {@code t} on a smooth curve through them (Catmull-Rom, still at the first and last). */
    static float[] curve(Key[] keys, double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        int i = 0;
        while (i < keys.length - 2 && c > keys[i + 1].t()) {
            i++;
        }
        Key a = keys[i];
        Key b = keys[i + 1];
        double span = Math.max(1.0E-6, b.t() - a.t());
        double u = (c - a.t()) / span;
        float[] va = values(a);
        float[] vb = values(b);
        float[] out = new float[va.length];
        for (int j = 0; j < va.length; j++) {
            double ma = tangent(keys, i, j) * span;
            double mb = tangent(keys, i + 1, j) * span;
            double u2 = u * u;
            double u3 = u2 * u;
            out[j] = (float)((2 * u3 - 3 * u2 + 1) * va[j] + (u3 - 2 * u2 + u) * ma + (-2 * u3 + 3 * u2) * vb[j] + (u3 - u2) * mb);
        }
        return out;
    }

    private static double tangent(Key[] keys, int i, int j) {
        if (i == 0 || i == keys.length - 1) {
            return 0.0;
        }
        return (values(keys[i + 1])[j] - values(keys[i - 1])[j]) / Math.max(1.0E-6, keys[i + 1].t() - keys[i - 1].t());
    }

    private static float[] values(Key k) {
        return new float[] {k.pitch(), k.yaw(), k.roll(), k.twist(), k.lean(), k.lunge(), k.drop()};
    }

    /** How far into its swing the impact is (0..1): where the cut lands. */
    public static final double IMPACT = 0.5;

    /**
     * Its {@code step}th swing {@code t} through (0..1 over {@link Quinque#swing} ticks): its stance with the right arm and the body on
     * its keys; for a two-handed one the left is left for QuinqueGrip.reach to put on its grip.
     */
    public static RigPose swing(Quinque q, int step, double t) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        float[] v = curve(keys(q, Math.floorMod(step, 3)), t);
        RigPose p = hold(q);
        QuinqueGrip.aimRight(p, h.pitch() + v[0], h.yaw() + v[1], h.roll() + v[2]);
        p.twist += v[3];
        p.lean += v[4];
        p.lunge += v[5];
        p.drop += v[6];
        step(p, q, v[5]);
        return p;
    }

    /**
     * A step in as the body lunges ({@code lunge} pixels): the leading foot forward, the other back (the right leading the blade and the
     * katana, the left the bladed stances), the hips lowered just as far as the split legs lower them (straight legs: an investigator's).
     */
    static void step(RigPose p, Quinque q, float lunge) {
        float s = Math.max(0.0F, Math.min(0.5F, lunge * 0.075F));
        if (s <= 0.01F) {
            return;
        }
        boolean right = q == Quinque.BLADE || q == Quinque.KATANA || q == Quinque.DAWN;
        p.rightLegX = right ? -s : 0.8F * s;
        p.leftLegX = right ? 0.8F * s : -s;
        p.legHold = Math.max(p.legHold, Math.min(1.0F, s / 0.12F));
        p.drop += 12.0F * (1.0F - (float)Math.cos(s));
    }

    /**
     * Its stance as it opens out of its case ({@code open} 0 the case in the hand .. 1 out): the arms come up from hanging into it; as it
     * shuts, the same backwards. The left hand comes onto its grip as it gets there (no reach yet: the weapon is still unfolding).
     */
    public static RigPose unfolding(Quinque q, float open) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        if (h == null) {
            return null;
        }
        float k = ease(open);
        RigPose p = new RigPose();
        stance(p, h, k);
        QuinqueGrip.aimRight(p, HELD[0] + (h.pitch() - HELD[0]) * k, h.yaw() * k, h.roll() * k);
        p.rightArmHold = 1.0F;
        if (q.twoHanded) {
            p.leftArmX = h.left()[0] * k;
            p.leftArmY = h.left()[1] * k;
            p.leftArmZ = h.left()[2] * k;
            p.leftArmHold = k;
        }
        return p;
    }

    /** True once {@code open} of the way out the left hand is on its grip (QuinqueGrip.reach may put it there). */
    public static boolean gripped(float open) {
        return open >= 1.0F;
    }

    /** How long the launcher's kick is drawn after a shot (ticks): 1.3.33 it fires every two, so each kick is short and sharp. */
    public static final double KICK_TICKS = 3.0;

    /**
     * The launcher's kick as a shard leaves ({@code kick} 1 at the shot, dying away): the muzzle jumps up, the shoulder rocks back. For
     * any other quinque nothing.
     */
    public static void recoil(RigPose p, Quinque q, float kick) {
        if (kick <= 0.0F || q != Quinque.LAUNCHER) {
            return;
        }
        pitchBy(p, q, -0.16F * kick);
        p.twist += 0.05F * kick;
        p.lunge -= 0.8F * kick;
        p.headX -= 0.04F * kick;
    }

    /**
     * {@code p}'s right arm turned {@code pitch} about the level line across its stance's look (- raises what it holds, + lowers it):
     * a two-handed weapon raised or lowered whole (its left hand follows on its grip).
     */
    public static void pitchBy(RigPose p, Quinque q, float pitch) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        double turn = h == null ? 0.0 : h.twist();
        double[] axis = {Math.cos(turn), 0.0, Math.sin(turn)};
        double[][] m = RigPose.zyx(p.rightArmX, p.rightArmY, p.rightArmZ);
        float[] a = QuinqueGrip.angles(RigPose.multiply(QuinqueGrip.about(axis, pitch), m));
        p.rightArmX = a[0];
        p.rightArmY = a[1];
        p.rightArmZ = a[2];
    }
}
