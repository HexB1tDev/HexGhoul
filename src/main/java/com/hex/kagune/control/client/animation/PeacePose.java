package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.PeaceRules;
import com.hex.kagune.control.king.PeaceStrike;

/**
 * 1.0.16: Peace's body through its moves (pure): minimal, controlled, sure. The blades do the work; his hands only direct them, a
 * gesture at the moment each strikes, as a conductor's do. He never turns his body round under them (they are on his back, and turned
 * with him they would strike somewhere else than where they are seen).
 */
public final class PeacePose {
    private PeacePose() {
    }

    static double smooth(double x) {
        x = Math.max(0.0, Math.min(1.0, x));
        return x * x * (3.0 - 2.0 * x);
    }

    /** Up into it over {@code from}..{@code to}, held, out of it over {@code off}..{@code end}. */
    static double hold(double age, double from, double to, double off, double end) {
        return smooth((age - from) / Math.max(1.0E-6, to - from)) * (1.0 - smooth((age - off) / Math.max(1.0E-6, end - off)));
    }

    public static void act(RigPose p, KingMove move, double age, int level, float weight) {
        if (weight <= 0.0F || move == null) {
            return;
        }
        double w = weight;
        switch (move) {
            case BLADES -> {
                PeaceStrike blow = PeaceStrike.byOrdinal(level);
                double k = hold(age, blow.impact - 2.5, blow.impact, blow.impact + 1.5, blow.duration);
                switch (blow) {
                    case LANCE -> {
                        // Two fingers of the right hand toward it as the lance goes.
                        p.rightArmX -= (float)(w * 1.35 * k);
                        p.rightArmY += (float)(w * 0.15 * k);
                        p.lean += (float)(w * 0.06 * k);
                    }
                    case SWEEP -> {
                        // The left hand carried across, as the scythe goes.
                        double across = smooth((age - (blow.impact - 2.0)) / 4.0);
                        p.leftArmX -= (float)(w * 1.2 * k);
                        p.leftArmY += (float)(w * (-0.7 + 1.5 * across) * k);
                        p.twist -= (float)(w * 0.18 * (across - 0.5) * k);
                    }
                    case SCISSOR -> {
                        // Both hands low and closed across each other.
                        p.rightArmX -= (float)(w * 0.75 * k);
                        p.leftArmX -= (float)(w * 0.75 * k);
                        p.rightArmY += (float)(w * 0.55 * k);
                        p.leftArmY -= (float)(w * 0.55 * k);
                        p.drop += (float)(w * 0.6 * k);
                    }
                    case CRUCIFIX -> {
                        // Arms opened to the quarters, then brought in before him together.
                        double open = hold(age, 1.0, blow.impact - 3.0, blow.impact - 2.0, blow.impact);
                        double in = hold(age, blow.impact - 2.0, blow.impact, blow.impact + 2.0, blow.duration);
                        p.rightArmZ += (float)(w * 1.1 * open);
                        p.leftArmZ -= (float)(w * 1.1 * open);
                        p.rightArmX -= (float)(w * (0.4 * open + 1.3 * in));
                        p.leftArmX -= (float)(w * (0.4 * open + 1.3 * in));
                        p.headX -= (float)(w * 0.1 * open);
                    }
                    case ASCEND -> {
                        // 1.0.20: both hands low and turned up, then lifted as the blades scoop up, rising onto his toes.
                        double low = hold(age, 0.0, blow.impact - 2.5, blow.impact - 1.0, blow.impact);
                        double up = hold(age, blow.impact - 1.0, blow.impact + 1.0, blow.impact + 3.0, blow.duration);
                        p.rightArmX -= (float)(w * (0.3 * low + 2.2 * up));
                        p.leftArmX -= (float)(w * (0.3 * low + 2.2 * up));
                        p.rightArmZ += (float)(w * 0.3 * up);
                        p.leftArmZ -= (float)(w * 0.3 * up);
                        p.drop += (float)(w * (1.2 * low - 0.6 * up));
                        p.headX -= (float)(w * 0.35 * up);
                    }
                    case JUDGMENT -> {
                        // 1.0.20: in the air, both arms raised, then brought down before him, folding over them.
                        double raised = hold(age, 0.0, blow.impact - 2.0, blow.impact - 1.0, blow.impact);
                        double down = hold(age, blow.impact - 1.0, blow.impact, blow.impact + 2.0, blow.duration);
                        p.rightArmX -= (float)(w * (2.8 * raised + 0.6 * down));
                        p.leftArmX -= (float)(w * (2.8 * raised + 0.6 * down));
                        p.lean += (float)(w * (-0.2 * raised + 0.55 * down));
                        p.headX += (float)(w * 0.35 * down);
                        p.rightKnee += (float)(w * 0.9 * k);
                        p.leftKnee += (float)(w * 1.1 * k);
                    }
                    case CROSS_CLEAVE -> {
                        // 1.0.20: both arms raised wide, then crossed down before him as the blades cross.
                        double wide = hold(age, 0.0, blow.impact - 3.0, blow.impact - 1.0, blow.impact);
                        double crossed = hold(age, blow.impact - 1.0, blow.impact, blow.impact + 2.0, blow.duration);
                        p.rightArmZ += (float)(w * 1.2 * wide);
                        p.leftArmZ -= (float)(w * 1.2 * wide);
                        p.rightArmX -= (float)(w * (1.6 * wide + 0.9 * crossed));
                        p.leftArmX -= (float)(w * (1.6 * wide + 0.9 * crossed));
                        p.rightArmY += (float)(w * 0.8 * crossed);
                        p.leftArmY -= (float)(w * 0.8 * crossed);
                        p.lean += (float)(w * 0.25 * crossed);
                        p.drop += (float)(w * 0.9 * crossed);
                    }
                    case SERAPH -> {
                        // 1.0.20: drawn back as charged, then the right hand driven straight out along the thrust, the left behind it.
                        double out = hold(age, blow.impact - 1.0, blow.impact, blow.impact + 5.0, blow.duration);
                        double back = 1.0 - smooth(age / Math.max(1.0, blow.impact - 1.0));
                        p.rightArmX -= (float)(w * 1.6 * out);
                        p.leftArmX -= (float)(w * 0.9 * out);
                        p.leftArmY -= (float)(w * 0.3 * out);
                        p.lean += (float)(w * (0.2 * back + 0.3 * out));
                        p.drop += (float)(w * (1.0 * back + 0.5 * out));
                        p.twist += (float)(w * 0.2 * out);
                    }
                    case HALO -> {
                        // 1.0.20: the arms opened wide and drawn round as the blades go round him, the head held still.
                        double open = hold(age, 0.0, blow.impact - 3.0, blow.impact + 3.0, blow.duration);
                        double turn = smooth((age - (blow.impact - 3.0)) / 6.0) - 0.5;
                        p.rightArmZ += (float)(w * 1.35 * open);
                        p.leftArmZ -= (float)(w * 1.35 * open);
                        p.rightArmY += (float)(w * 0.4 * turn * open);
                        p.leftArmY -= (float)(w * 0.4 * turn * open);
                        p.drop += (float)(w * 0.7 * open);
                    }
                }
            }
            case CHARGE -> {
                // 1.0.20: the heavy charging: low, the right hand drawn back to his shoulder, the left out before him toward what the
                // blades will go at.
                double k = smooth(age / 5.0);
                double full = com.hex.kagune.control.king.KingCombo.charge(age);
                p.drop += (float)(w * (0.9 + 0.4 * full) * k);
                p.lean += (float)(w * 0.2 * k);
                p.rightArmX -= (float)(w * 0.4 * k);
                p.rightElbow += (float)(w * 1.5 * k);
                p.leftArmX -= (float)(w * 1.25 * k);
                p.twist -= (float)(w * 0.25 * k);
                p.armHold += (float)(w * k);
            }
            case WALTZ -> {
                // 1.0.34: his left hand out at it, sending them in one by one (a flick of it each time); flung open as they tear out of it.
                double out = hold(age, 1.0, 4.0, PeaceRules.WALTZ_RIP + 2.0, PeaceRules.WALTZ_TICKS);
                p.leftArmX -= (float)(w * 1.35 * out);
                p.leftArmY -= (float)(w * 0.1 * out);
                double flick = 0.0;
                for (int blade = 0; blade < com.hex.kagune.control.king.PeaceMotion.BLADES; blade++) {
                    double sent = PeaceRules.waltzSent(blade);
                    flick = Math.max(flick, hold(age, sent - 1.0, sent, sent + 0.5, sent + 3.0));
                }
                p.leftElbow += (float)(w * 0.35 * flick);
                p.leftArmX -= (float)(w * 0.15 * flick);
                double tear = hold(age, PeaceRules.WALTZ_RIP - 1.0, PeaceRules.WALTZ_RIP, PeaceRules.WALTZ_RIP + 3.0, PeaceRules.WALTZ_RIP + 9.0);
                p.rightArmZ += (float)(w * 1.2 * tear);
                p.leftArmZ -= (float)(w * 1.2 * tear);
                p.leftArmX += (float)(w * 0.6 * tear);
                p.headX -= (float)(w * 0.15 * tear);
                p.twist += (float)(w * 0.1 * out);
            }
            case ASCENT -> {
                // Carried up: arms a little out and back, legs together, the toes pointed.
                double k = smooth(age / 6.0);
                p.rightArmZ += (float)(w * 0.55 * k);
                p.leftArmZ -= (float)(w * 0.55 * k);
                p.rightArmX += (float)(w * 0.25 * k);
                p.leftArmX += (float)(w * 0.25 * k);
                p.rightLegX += (float)(w * 0.15 * k);
                p.leftLegX += (float)(w * 0.25 * k);
                p.headX -= (float)(w * 0.1 * k);
            }
            case DIVE -> {
                double k = smooth(age / 4.0);
                p.lean += (float)(w * 0.9 * k);
                p.rightArmX += (float)(w * 0.6 * k);
                p.leftArmX += (float)(w * 0.6 * k);
                p.rightArmZ += (float)(w * 0.2 * k);
                p.leftArmZ -= (float)(w * 0.2 * k);
                p.headX -= (float)(w * 0.5 * k);
            }
            case LANDING -> {
                // 1.0.34: the superhero landing: down on his right knee, the left foot planted before him, the right fist driven into
                // the ground, the left arm flung out behind, the head up; held a beat, then he rises out of it.
                double k = hold(age, 0.0, 1.0, PeaceRules.LAND_TICKS - 10.0, PeaceRules.LAND_TICKS);
                p.drop += (float)(w * 5.0 * k);
                p.lean += (float)(w * 0.55 * k);
                p.leftLegX -= (float)(w * 1.25 * k);
                p.leftKnee += (float)(w * 1.6 * k);
                p.rightLegX += (float)(w * 0.35 * k);
                p.rightKnee += (float)(w * 2.0 * k);
                p.rightArmX -= (float)(w * 0.35 * k);
                p.rightArmZ += (float)(w * 0.15 * k);
                p.leftArmX += (float)(w * 0.7 * k);
                p.leftArmZ -= (float)(w * 1.0 * k);
                p.headX -= (float)(w * 0.55 * k);
                p.legHold += (float)(w * k);
            }
            case PIERCE -> {
                // 1.0.34: held, the right hand drawn back by his shoulder, the left out before him along the lance as it gathers; let go
                // (level 1), the right thrust out after it, held out while it is out. 1.0.40: his chest kept square (no twist, no lean):
                // the lance hangs off it, and the twist swung it as much as 26 degrees off where he aimed it.
                double k = smooth(Math.min(1.0, age / 5.0));
                if (level == 0) {
                    double full = PeaceRules.charge(age);
                    p.rightArmX -= (float)(w * 0.3 * k);
                    p.rightElbow += (float)(w * 1.4 * k);
                    p.leftArmX -= (float)(w * 1.4 * k);
                    p.drop += (float)(w * (0.4 + 0.4 * full) * k);
                } else {
                    double thrust = smooth(Math.min(1.0, age / 2.0));
                    p.rightArmX -= (float)(w * 1.55 * thrust);
                }
            }
            case REBIRTH -> {
                // 1.0.34: held, crouched low, both hands spread down toward the ground the blades go into; let go (level 1), up out of it.
                double k = level == 0 ? smooth(Math.min(1.0, age / 6.0)) : 1.0 - smooth(Math.min(1.0, age / 10.0));
                p.drop += (float)(w * 2.2 * k);
                p.lean += (float)(w * 0.35 * k);
                p.rightArmX -= (float)(w * 0.45 * k);
                p.leftArmX -= (float)(w * 0.45 * k);
                p.rightArmZ += (float)(w * 0.6 * k);
                p.leftArmZ -= (float)(w * 0.6 * k);
                p.rightKnee += (float)(w * 1.1 * k);
                p.leftKnee += (float)(w * 1.1 * k);
                p.headX += (float)(w * 0.3 * k);
                p.legHold += (float)(w * k);
            }
            case HORIZON -> {
                // 1.0.34: in low off the dash; the right arm swung up with the uppercut; up under it, arms spread, directing the blades in;
                // the right arm brought down with the last blade's blow; spread wide, holding them as they drive it under.
                double in = hold(age, 0.0, 1.5, PeaceRules.HORIZON_UPPER, PeaceRules.HORIZON_UPPER + 2.0);
                p.drop += (float)(w * 1.5 * in);
                p.lean += (float)(w * 0.5 * in);
                double upper = hold(age, PeaceRules.HORIZON_UPPER - 1.0, PeaceRules.HORIZON_UPPER, PeaceRules.HORIZON_UPPER + 3.0, PeaceRules.HORIZON_UPPER + 9.0);
                p.rightArmX -= (float)(w * 2.7 * upper);
                p.headX -= (float)(w * 0.4 * upper);
                double direct = hold(age, PeaceRules.HORIZON_UNDER, PeaceRules.HORIZON_FIRST, PeaceRules.HORIZON_STRIKE - 4.0, PeaceRules.HORIZON_STRIKE - 2.0);
                p.rightArmZ += (float)(w * 0.9 * direct);
                p.leftArmZ -= (float)(w * 0.9 * direct);
                p.rightArmX -= (float)(w * 1.6 * direct);
                p.leftArmX -= (float)(w * 1.6 * direct);
                p.headX -= (float)(w * 0.5 * direct);
                double down = hold(age, PeaceRules.HORIZON_STRIKE - 3.0, PeaceRules.HORIZON_STRIKE - 1.0, PeaceRules.HORIZON_STRIKE + 2.0, PeaceRules.HORIZON_STRIKE + 8.0);
                p.rightArmX -= (float)(w * (2.6 - 2.2 * smooth(Math.min(1.0, Math.max(0.0, (age - PeaceRules.HORIZON_STRIKE + 1.0) / 3.0)))) * down);
                p.lean += (float)(w * 0.35 * down);
                p.headX += (float)(w * 0.5 * down);
                double spread = hold(age, PeaceRules.HORIZON_STRIKE + 4.0, PeaceRules.HORIZON_STRIKE + 10.0, PeaceRules.HORIZON_TICKS - 14.0, PeaceRules.HORIZON_TICKS);
                p.rightArmZ += (float)(w * 1.1 * spread);
                p.leftArmZ -= (float)(w * 1.1 * spread);
                p.rightArmX += (float)(w * 0.3 * spread);
                p.leftArmX += (float)(w * 0.3 * spread);
                p.headX += (float)(w * 0.45 * spread);
            }
            default -> {
            }
        }
    }
}
