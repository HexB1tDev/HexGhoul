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

    /**
     * One key of a swing: when (0..1), the right arm's pitch, yaw and roll from the stance's, and the body's twist, lean, lunge, drop; 4.1.17
     * and its spin (the whole body turned about its middle: + to its left, a spinning cut's whole turn); 4.1.21 and the wrists (the weapon
     * turned in the fist about its right grip: RigPose.weaponX and weaponZ, the arm's own frame).
     */
    record Key(float t, float pitch, float yaw, float roll, float twist, float lean, float lunge, float drop, float spin, float wristX,
               float wristZ) {
        Key(float t, float pitch, float yaw, float roll, float twist, float lean, float lunge, float drop, float spin) {
            this(t, pitch, yaw, roll, twist, lean, lunge, drop, spin, 0.0F, 0.0F);
        }
    }

    private static Key k(double t, double pitch, double yaw, double roll, double twist, double lean, double lunge, double drop) {
        return k(t, pitch, yaw, roll, twist, lean, lunge, drop, 0.0);
    }

    private static Key k(double t, double pitch, double yaw, double roll, double twist, double lean, double lunge, double drop, double spin) {
        return new Key((float)t, (float)pitch, (float)yaw, (float)roll, (float)twist, (float)lean, (float)lunge, (float)drop, (float)spin);
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
            case KATANA -> katana(step);
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

    /** 4.1.17: {@code q}'s {@code step}th swing written by intent (QuinqueAim), or null where it is still written as arm angles. */
    static QuinqueAim.Aim[] aims(Quinque q, int step) {
        return q == Quinque.KATANA ? katanaAims(Math.floorMod(step, q.steps())) : null;
    }

    /**
     * 4.1.17 (the owner: "We need to improve every single M1 of every single weapon. They're bad ... the katana looks like shit"): the
     * katana's five, written by where the blade points and where the hands are (right, up, forward from the neck; QuinqueAim solves the
     * arm): a breath of anticipation, the windup, the cut at its fastest through the impact (0.5), a follow through that carries on past it,
     * the body settling, back into chudan. The feet step in under each (the lunge), the hips drive it (the twist), the whole body turns
     * round once behind the last.
     */
    static Key[] katana(int step) {
        // Solved once, by tools/QuinqueAimBake.java (QuinqueKeys; QuinqueTests checks they still match their aims).
        return QuinqueKeys.KEYS[Quinque.KATANA.ordinal()][Math.floorMod(step, Quinque.KATANA.steps())];
    }

    /**
     * 4.1.21: the hands first (each key's fist where its arm, nine pixels long, puts it: out in front of him, never back through him), the
     * blade where the grip and the wrists can lay it (QuinqueAim): solved on renders from the front, the side and above.
     */
    static QuinqueAim.Aim[] katanaAims(int step) {
        return switch (step) {
            // Kesa-giri: up into jodan over the right shoulder, cut down across the front (the blade pointing at the target half way) to low on the
            // left, the wrists turning the blade into the line of the arms as it lands.
            case 0 -> new QuinqueAim.Aim[] {QuinqueAim.rest(0),
                QuinqueAim.a(0.12, 0.204, 0.865, 0.458, 1.397, -2.901, 8.198, 0.15, -0.03, 0, 0.05),
                QuinqueAim.a(0.3, 0.451, 0.35, -0.821, 4.098, 5.936, 4.148, 0.55, -0.08, -0.6, 0),
                QuinqueAim.a(0.4, 0.605, 0.756, -0.252, 2.755, 2.041, 7.722, 0.25, 0.06, 1.5, 0.3),
                QuinqueAim.a(0.45, 0.553, 0.553, 0.623, 1.568, -1.548, 8.308, 0.02, 0.13, 2.3, 0.55),
                QuinqueAim.a(0.49, -0.4, -0.3, 0.87, 0.943, -3.262, 7.934, -0.15, 0.17, 2.55, 0.7),
                QuinqueAim.a(0.525, -0.55, -0.6, 0.58, 0.491, -4.705, 7.304, -0.3, 0.2, 2.7, 0.8),
                QuinqueAim.a(0.58, -0.631, -0.733, 0.254, -0.405, -6.054, 5.946, -0.5, 0.24, 2.7, 0.9),
                QuinqueAim.a(0.66, -0.723, -0.641, -0.258, -0.864, -6.961, 4.691, -0.62, 0.25, 2.6, 0.95),
                QuinqueAim.a(0.78, -0.302, 0.302, 0.905, 0.053, -6.497, 6.026, -0.32, 0.15, 1.8, 0.6),
                QuinqueAim.a(0.9, 0, 0.6, 0.8, 0.321, -6.139, 6.479, -0.08, 0.05, 0.8, 0.25),
                QuinqueAim.rest(1)};
            // Gyaku-kesa: gathered low on the left, the blade back by the left hip, rising across the front to high on the right.
            case 1 -> new QuinqueAim.Aim[] {QuinqueAim.rest(0),
                QuinqueAim.a(0.15, -0.505, -0.303, 0.808, 0.053, -6.497, 6.026, -0.3, 0.1, 0, 0.4),
                QuinqueAim.a(0.3, -0.722, -0.619, -0.309, -0.864, -6.961, 4.691, -0.6, 0.14, 0.3, 0.6),
                QuinqueAim.a(0.4, -0.609, -0.508, 0.609, 0.313, -4.884, 7.121, -0.25, 0.08, 1.8, 0.5),
                QuinqueAim.a(0.5, 0.45, 0.45, 0.771, 1.86, -2, 8.434, 0.1, 0.02, 2.6, 0.3),
                QuinqueAim.a(0.58, 0.608, 0.71, 0.355, 3.192, 1.798, 7.957, 0.4, -0.04, 2.4, 0.1),
                QuinqueAim.a(0.66, 0.56, 0.611, -0.56, 4.102, 4.287, 6.377, 0.55, -0.07, 2, 0),
                QuinqueAim.a(0.8, 0.303, 0.808, 0.505, 1.846, -1.099, 8.381, 0.3, -0.02, 1, 0),
                QuinqueAim.rest(1)};
            // Yoko-giri: the chest turned right with the blade out level to its right, swept flat across the front to the left, the hips whipping round.
            case 2 -> new QuinqueAim.Aim[] {QuinqueAim.rest(0),
                QuinqueAim.a(0.15, 0.597, 0.398, 0.697, 1.857, -4.694, 7.992, 0.35, -0.03, 0, 0.2),
                QuinqueAim.a(0.3, 0.954, 0, -0.301, 3.202, -4.517, 8.452, 0.85, -0.05, -0.4, 0.35),
                QuinqueAim.a(0.42, 0.862, 0, 0.507, 2.286, -4.714, 8.141, 0.4, 0.06, 1.6, 0.4),
                QuinqueAim.a(0.5, -0.1, 0, 0.995, 0.665, -5.161, 7.226, -0.05, 0.12, 2.6, 0.4),
                QuinqueAim.a(0.58, -0.8, 0, 0.6, -0.566, -4.873, 6.463, -0.5, 0.14, 2.6, 0.4),
                QuinqueAim.a(0.68, -0.954, 0, -0.301, -1.292, -4.697, 5.843, -0.85, 0.14, 2.2, 0.4),
                QuinqueAim.a(0.82, -0.298, 0.596, 0.745, 0.043, -5.785, 6.489, -0.4, 0.06, 1, 0.2),
                QuinqueAim.rest(1)};
            // Tsuki: drawn back by the right hip on the line, then driven straight out along it with a long lunge.
            case 3 -> new QuinqueAim.Aim[] {QuinqueAim.rest(0),
                QuinqueAim.a(0.15, -0.292, 0.148, 0.945, 2.754, -7.84, 6.469, 0.3, -0.05, -0.8, 0.3),
                QuinqueAim.a(0.32, -0.296, 0, 0.955, 4.549, -9.668, 4.691, 0.3, -0.08, -1.5, 0.5),
                QuinqueAim.a(0.5, 0.198, -0.05, 0.979, 0.946, -4.703, 7.567, -0.2, 0.2, 6.5, 0.5),
                QuinqueAim.a(0.62, 0.218, -0.05, 0.975, 0.946, -4.703, 7.567, -0.22, 0.21, 6.5, 0.5),
                QuinqueAim.a(0.82, 0, 0.507, 0.862, 0.5, -6.05, 6.66, -0.05, 0.1, 3, 0.3),
                QuinqueAim.rest(1)};
            // Kaiten-giri, the finisher: coiled with the blade out to the right, the whole body spun round once with it level out to its left, stepping
            // out of it into chudan.
            default -> new QuinqueAim.Aim[] {QuinqueAim.rest(0),
                QuinqueAim.a(0.18, 0.954, 0, -0.301, 3.202, -4.517, 8.452, 0.8, -0.05, -0.5, 0.6),
                QuinqueAim.a(0.4, -0.862, 0, 0.507, -0.406, -4.703, 6.668, -0.35, 0.08, 0.8, 0.45, 2.2),
                QuinqueAim.a(0.6, -0.979, 0, 0.206, -0.935, -4.698, 6.205, -0.55, 0.12, 2, 0.35, 4.87),
                QuinqueAim.a(0.75, -0.768, -0.549, -0.329, -0.933, -6.315, 5.214, -0.6, 0.14, 2.2, 0.45, Math.PI * 2.0),
                QuinqueAim.a(0.88, 0, 0.6, 0.8, 0.321, -6.139, 6.479, -0.1, 0.05, 1, 0.2, Math.PI * 2.0),
                QuinqueAim.restTurned(1, Math.PI * 2.0)};
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
        return new float[] {k.pitch(), k.yaw(), k.roll(), k.twist(), k.lean(), k.lunge(), k.drop(), k.spin(), k.wristX(), k.wristZ()};
    }

    /** How far into its swing the impact is (0..1): where the cut lands. */
    public static final double IMPACT = 0.5;

    /**
     * Its {@code step}th swing {@code t} through (0..1 over {@link Quinque#swing} ticks): its stance with the right arm and the body on
     * its keys; for a two-handed one the left is left for QuinqueGrip.reach to put on its grip.
     */
    public static RigPose swing(Quinque q, int step, double t) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        float[] v = curve(keys(q, Math.floorMod(step, q.steps())), t);
        RigPose p = hold(q);
        QuinqueGrip.aimRight(p, h.pitch() + v[0], h.yaw() + v[1], h.roll() + v[2]);
        p.twist += v[3];
        p.lean += v[4];
        p.lunge += v[5];
        p.drop += v[6];
        p.spin += v[7]; // 4.1.17: the whole body turning (the head with it: it is looking along the cut)
        // 4.1.21: the wrists, the blade turned in the fist (drawn by QuinqueItemRenderer, followed by QuinqueGrip.held).
        // (The curve can overshoot between keys: never further than a wrist bends.)
        p.weaponX += (float)Math.max(-QuinqueAim.WRIST_X, Math.min(QuinqueAim.WRIST_X, v[8]));
        p.weaponZ += (float)Math.max(-QuinqueAim.WRIST_Z, Math.min(QuinqueAim.WRIST_Z, v[9]));
        step(p, q, v[5]);
        return p;
    }

    /** 3.3.2: Dawn's idle: how long a loop is, when it starts after he stops (ticks still), and its beats (ticks into the loop). */
    public static final int DAWN_LOOP = 260;
    public static final int DAWN_SETTLE = 30;
    static final double TWIRL_FROM = 60.0;
    static final double TWIRL_TO = 92.0;
    static final double UP_FROM = 130.0;
    static final double UP_TO = 150.0;
    static final double DOWN_FROM = 222.0;
    static final double DOWN_TO = 244.0;
    /**
     * Dawn carried aside at ease: the arm's pitch, yaw and roll from the stance's (found against its real held model: the hand at the
     * chest out to his right, the lance raised some 30 degrees and turned out to his right). A stiff arm can't rest a lance this long on
     * the shoulder (its hand would be over his head) nor point it down (it would be in the ground).
     */
    static final float SHOULDER_PITCH = -0.55F;
    static final float SHOULDER_YAW = 0.38F;
    static final float SHOULDER_ROLL = 0.3F;

    /**
     * 3.3.2 (the owner: Dawn needs an idle, an ambient): Dawn held at rest {@code still} ticks after he stopped moving (or swinging, or
     * aiming), a slow loop every {@link #DAWN_LOOP}: breathing in the stance; the lance turned to and fro in his fingers, its point
     * circling a little and lifting; back in the stance a while; then carried up and aside at ease, its point raised out to his right, his
     * left hand on his hip, the head tilted, leaning back a little, the point tapping lazily, for a few seconds, before it comes back down
     * into the stance. Relaxed: he knows he doesn't need to be ready. The other quinques keep their stance ({@link #hold}).
     */
    public static RigPose idle(Quinque q, double still) {
        RigPose p = hold(q);
        if (q != Quinque.DAWN || p == null || still < 0.0) {
            return p;
        }
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        double in = Math.max(0.0, still - DAWN_SETTLE);
        double ease = smooth01(still / DAWN_SETTLE);
        double t = in % DAWN_LOOP;
        double breath = Math.sin(still * 0.09);
        float pitch = (float)(0.035 * breath * ease);
        float yaw = 0.0F;
        float roll = 0.0F;
        float lean = (float)(0.012 * breath * ease);
        double shoulder = 0.0;
        if (in > 0.0) {
            if (t >= TWIRL_FROM && t < TWIRL_TO) {
                double u = (t - TWIRL_FROM) / (TWIRL_TO - TWIRL_FROM);
                // Turned to and fro in the fingers (a whole turn would sweep its three blocks down through the ground).
                double envelope = Math.sin(Math.PI * u);
                roll += (float)(0.85 * Math.sin(Math.PI * 4.0 * u) * envelope);
                pitch -= (float)(0.22 * envelope);
            }
            shoulder = t < UP_FROM ? 0.0 : t < UP_TO ? smooth01((t - UP_FROM) / (UP_TO - UP_FROM))
                : t < DOWN_FROM ? 1.0 : t < DOWN_TO ? 1.0 - smooth01((t - DOWN_FROM) / (DOWN_TO - DOWN_FROM)) : 0.0;
        }
        if (shoulder > 0.0) {
            double tap = t >= UP_TO && t < DOWN_FROM ? Math.max(0.0, Math.sin((t - UP_TO) * 0.22)) : 0.0;
            pitch += (float)(shoulder * SHOULDER_PITCH + 0.07 * tap * tap);
            yaw += (float)(shoulder * SHOULDER_YAW);
            roll += (float)(shoulder * SHOULDER_ROLL);
            lean -= (float)(0.06 * shoulder);
            p.headZ += (float)(0.12 * shoulder);
            p.headX -= (float)(0.06 * shoulder);
            // The free left hand on the hip.
            p.leftArmX += (float)(-0.25 * shoulder);
            p.leftArmY += (float)(0.1 * shoulder);
            p.leftArmZ += (float)(-0.55 * shoulder);
            p.leftElbow += (float)(0.9 * shoulder);
            p.leftArmHold = Math.max(p.leftArmHold, (float)shoulder);
        }
        QuinqueGrip.aimRight(p, h.pitch() + pitch, h.yaw() + yaw, h.roll() + roll);
        p.lean += lean;
        return p;
    }

    /** How far through its shoulder rest Dawn's idle is at {@code still} (0 in the stance .. 1 on the shoulder): for tests and the renderer. */
    public static double dawnShoulder(double still) {
        double in = Math.max(0.0, still - DAWN_SETTLE);
        if (in <= 0.0) {
            return 0.0;
        }
        double t = in % DAWN_LOOP;
        return t < UP_FROM ? 0.0 : t < UP_TO ? smooth01((t - UP_FROM) / (UP_TO - UP_FROM))
            : t < DOWN_FROM ? 1.0 : t < DOWN_TO ? 1.0 - smooth01((t - DOWN_FROM) / (DOWN_TO - DOWN_FROM)) : 0.0;
    }

    static double smooth01(double x) {
        double u = Math.max(0.0, Math.min(1.0, x));
        return u * u * (3.0 - 2.0 * u);
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
