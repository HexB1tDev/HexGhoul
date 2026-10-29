package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRig;
import com.hex.kagune.control.graft.GraftStage;

/**
 * 7.0.0: Graft DNA on his own body (pure, for {@link KaguneBodyAnimation}). In the kakuja he stands as a predator: hunched, knees
 * bent, clawed hands hanging ready, breathing, the claws flexing, the head now and then snapping to a crooked tilt and easing back.
 * Through every move his body follows the beats the kagune strikes on (the same beats the server hurts on): the kakuja's claws
 * wound up over the shoulder and raked across, its lunges behind the jaws, both arms brought down with an overhead slam; on every
 * stage his weight thrown behind a thrust, his chest turned through a sweep, crouched under a rising rip, flung open as the limbs
 * burst out, curled round when they wrap him.
 */
public final class GraftPose {
    /** Ticks a beat's pose takes to ease back out after it lets go. */
    static final double OUT = 4.0;
    /** How hard the body follows the kagune's own blows (its arms' and the kakuja's jaws' are wholly his). */
    static final float FOLLOW = 0.6F;
    /** The head's crooked twitch: how often (ticks), and how long it lasts. */
    static final double TWITCH_EVERY = 70.0;
    static final double TWITCH_LONG = 14.0;

    /** How much of a limb's length rides his torso (the rest stays where the rig put it, so its blows land where they were aimed). */
    static final double RIDE = 0.45;

    private GraftPose() {
    }

    /**
     * 7.0.0: a limb's points (rig space, root first) carried with his torso as {@code pose} hunches, leans and lunges it: the root
     * stays in the sockets of his back, the carry fading out along the first {@link #RIDE} of the limb. Unchanged when nothing poses
     * him.
     */
    public static double[][] ride(RigPose pose, double[][] points) {
        if (pose == null || points.length == 0) {
            return points;
        }
        double[] root = points[0];
        double[] to = BodyFrame.carried(pose, "torso", root);
        double[] shift = {to[0] - root[0], to[1] - root[1], to[2] - root[2]};
        if (!(Math.abs(shift[0]) + Math.abs(shift[1]) + Math.abs(shift[2]) > 1.0E-5)) {
            return points;
        }
        double total = 0.0;
        for (int i = 1; i < points.length; i++) {
            total += Math.sqrt(sq(points[i][0] - points[i - 1][0]) + sq(points[i][1] - points[i - 1][1]) + sq(points[i][2] - points[i - 1][2]));
        }
        double reach = Math.max(1.0E-6, total * RIDE);
        double[][] out = new double[points.length][];
        double along = 0.0;
        for (int i = 0; i < points.length; i++) {
            if (i > 0) {
                along += Math.sqrt(sq(points[i][0] - points[i - 1][0]) + sq(points[i][1] - points[i - 1][1]) + sq(points[i][2] - points[i - 1][2]));
            }
            double w = 1.0 - smooth(along / reach);
            out[i] = new double[] {points[i][0] + shift[0] * w, points[i][1] + shift[1] * w, points[i][2] + shift[2] * w};
        }
        return out;
    }

    private static double sq(double v) {
        return v * v;
    }

    /**
     * Puts him on {@code p}: the kakuja's stance at {@code hide} (0 none .. 1 the hide all over him) and {@code move} at {@code age}
     * ticks ({@link GraftMove#NONE} for none), its beats for {@code limbs} limbs of {@code stage}; {@code clock} drives the breathing.
     */
    public static void body(RigPose p, GraftStage stage, int limbs, GraftMove move, double age, double hide, double clock) {
        body(p, stage, limbs, move, age, GraftMove.NONE, 0.0, hide, clock);
    }

    /** 7.0.2: as {@link #body}, with the job running alongside ({@code job} at {@code jobAge} ticks): its body under the move's. */
    public static void body(RigPose p, GraftStage stage, int limbs, GraftMove move, double age, GraftMove job, double jobAge, double hide,
                            double clock) {
        if (job != null && job != GraftMove.NONE && jobAge <= job.ticks() + OUT) {
            for (GraftMove.Beat beat : job.beats(Math.max(1, limbs))) {
                beat(p, stage, limbs, job, beat, jobAge);
            }
        }
        boolean moving = move != GraftMove.NONE && age <= move.ticks() + OUT;
        if (hide > 0.01) {
            stance(p, (float)hide, clock, moving);
        }
        if (!moving) {
            return;
        }
        for (GraftMove.Beat beat : move.beats(Math.max(1, limbs))) {
            beat(p, stage, limbs, move, beat, age);
        }
    }

    /** The kakuja's predator stance and its life. {@code moving}: a move's own pose takes most of it over. */
    static void stance(RigPose p, float w, double clock, boolean moving) {
        float k = w * (moving ? 0.45F : 1.0F);
        float breath = (float)Math.sin(clock * 0.09);
        p.lean += k * (0.24F + 0.03F * breath);
        p.drop += k * 1.3F;
        p.headX -= k * 0.22F;
        p.rightArmX -= k * 0.4F;
        p.leftArmX -= k * 0.4F;
        p.rightArmZ += k * (0.24F + 0.04F * breath);
        p.leftArmZ -= k * (0.24F + 0.04F * breath);
        p.rightElbow += k * 0.6F;
        p.leftElbow += k * 0.6F;
        p.rightLegX -= k * 0.14F;
        p.leftLegX -= k * 0.14F;
        p.rightKnee += k * 0.32F;
        p.leftKnee += k * 0.32F;
        // The claws flexing, never quite still.
        float flex = (float)(0.16 * Math.max(0.0, Math.sin(clock * 0.21)) + 0.05 * Math.sin(clock * 0.53)) * k;
        p.rightElbow += flex;
        p.leftElbow += flex * 0.8F;
        // Now and then his head snaps to a crooked tilt, holds, and eases back.
        if (!moving) {
            double cycle = clock % TWITCH_EVERY;
            int which = (int)Math.floorMod((long)Math.floor(clock / TWITCH_EVERY), 3L);
            if (cycle < TWITCH_LONG) {
                float jerk = (float)(cycle < 1.5 ? cycle / 1.5 : Math.exp(-(cycle - 1.5) * 0.3)) * w;
                switch (which) {
                    case 0 -> p.headZ += 0.5F * jerk;
                    case 1 -> {
                        p.headY += 0.4F * jerk;
                        p.headZ -= 0.3F * jerk;
                    }
                    default -> p.headX += 0.28F * jerk;
                }
            }
        }
    }

    /** One beat's pose on him at {@code age}. */
    static void beat(RigPose p, GraftStage stage, int limbs, GraftMove move, GraftMove.Beat beat, double age) {
        if (age < beat.start() || age > beat.end() + OUT) {
            return;
        }
        boolean winding = age < beat.land();
        float w = (float)smooth(beat.windup(age));
        float t = (float)smooth(beat.through(age));
        // Easing back out once it lets go.
        float weight = age <= beat.end() ? 1.0F : (float)(1.0 - smooth((age - beat.end()) / OUT));
        if (beat.limb() == GraftMove.ARM) {
            arm(p, beat, winding, w, t, weight);
            return;
        }
        int side = beat.limb() == GraftMove.ALL ? 0 : GraftRig.side(stage, Math.max(1, limbs), Math.min(beat.limb(), Math.max(0, limbs - 1)));
        float k = FOLLOW * weight;
        switch (beat.kind()) {
            case THRUST, BITE -> {
                float at = winding ? -0.3F * w : 1.0F - 0.5F * t;
                p.lean += k * 0.14F * at;
                p.lunge += k * 1.8F * Math.max(0.0F, at);
                pointArm(p, side == 0 ? 1 : side, k * (winding ? 0.4F * w : 1.0F - 0.6F * t));
            }
            case SWEEP -> {
                float dir = (float)Math.signum(beat.to() - beat.from());
                float turn = winding ? 0.32F * w : 0.32F - 0.7F * t;
                p.twist += k * dir * turn;
                p.side += k * dir * 0.06F * (winding ? w : 1.0F - 2.0F * t);
                float swing = winding ? -0.9F * w : -0.9F + 0.6F * t;
                if (side >= 0) {
                    p.rightArmX += k * swing;
                    p.rightArmZ += k * 0.4F * (winding ? w : 1.0F - t);
                } else {
                    p.leftArmX += k * swing;
                    p.leftArmZ -= k * 0.4F * (winding ? w : 1.0F - t);
                }
            }
            case SLAM -> {
                if (winding) {
                    p.lean -= k * 0.15F * w;
                    raiseArms(p, side, k * w, 2.5F);
                } else {
                    p.lean += k * 0.42F * (1.0F - 0.6F * t);
                    p.drop += k * 2.4F * (1.0F - 0.6F * t);
                    raiseArms(p, side, k * (1.0F - t), 0.6F);
                }
            }
            case UPPER -> {
                if (winding) {
                    p.drop += k * 2.0F * w;
                    p.lean += k * 0.25F * w;
                } else {
                    p.drop += k * (2.0F - 2.6F * t);
                    p.lean += k * (0.25F - 0.4F * t);
                    raiseArms(p, side == 0 ? 1 : side, k * t, 2.6F);
                }
            }
            case SPIN -> {
                double turns = (beat.to() - beat.from()) / 360.0;
                float swirl = (float)Math.sin(Math.PI * 2.0 * turns * t);
                p.twist += k * 0.4F * swirl;
                p.side += k * 0.08F * swirl;
                p.rightArmZ += k * 0.9F;
                p.leftArmZ -= k * 0.9F;
                p.drop += k * 0.8F;
            }
            case CURL -> {
                float c = winding ? w : 1.0F;
                p.lean += k * 0.3F * c;
                p.drop += k * 1.4F * c;
                p.headX += k * 0.22F * c;
                p.rightArmX -= k * 1.1F * c;
                p.leftArmX -= k * 1.1F * c;
                p.rightArmY -= k * 0.5F * c;
                p.leftArmY += k * 0.5F * c;
                p.rightElbow += k * 0.9F * c;
                p.leftElbow += k * 0.9F * c;
            }
            case BURST -> {
                float c = winding ? -0.4F * w : 1.0F - 0.5F * t;
                p.lean -= k * 0.2F * c;
                p.headX -= k * 0.35F * c;
                p.rightArmZ += k * 1.4F * Math.max(0.0F, c);
                p.leftArmZ -= k * 1.4F * Math.max(0.0F, c);
                p.rightArmX -= k * 0.6F * Math.max(0.0F, c);
                p.leftArmX -= k * 0.6F * Math.max(0.0F, c);
            }
            case PLANT -> {
                if (move == GraftMove.SCORPION_VAULT || move == GraftMove.CATAPULT) {
                    vault(p, age - (move == GraftMove.CATAPULT ? 1.0 : 0.0), weight);
                    return;
                }
                float c = winding ? w : 1.0F - t;
                p.lean += k * 0.4F * c;
                p.drop += k * 1.0F * c;
                p.rightArmX += k * 0.9F * c;
                p.leftArmX += k * 0.9F * c;
            }
            case STORM -> {
                // 7.0.2 Eye of the Storm: arms out from his sides in the wheeling mass, his chest swaying with it.
                float c = winding ? w : 1.0F - 0.5F * t;
                p.rightArmZ += k * 1.0F * c;
                p.leftArmZ -= k * 1.0F * c;
                p.twist += k * 0.25F * c * (float)Math.sin(age * 0.3);
                p.headX -= k * 0.15F * c;
            }
            case CURTAIN -> {
                // 7.0.2 Black Curtain: both arms raised as the wall rises, brought down with it as it falls.
                double fold = com.hex.kagune.control.graft.GraftRules.curtainFold(age, beat.land());
                float raise = (float)(smooth(age / 10.0) * (1.0 - fold));
                float down = (float)(fold * (1.0 - smooth((age - beat.land() - 6.0) / 8.0)));
                raiseArms(p, 0, k * raise, 2.6F);
                raiseArms(p, 0, k * down, 1.2F);
                p.lean += k * (0.35F * down - 0.12F * raise);
                p.drop += k * 1.2F * down;
            }
            case SOAR -> {
                // 7.0.2 the catapult's flight: arms out for balance, legs drawn up, then reaching down for the ground.
                float c = winding ? w : 1.0F;
                p.rightArmZ += 0.6F * c * 1.1F;
                p.leftArmZ -= 0.6F * c * 1.1F;
                p.rightArmX -= 0.6F * c * 0.3F;
                p.leftArmX -= 0.6F * c * 0.3F;
                p.rightKnee += 0.6F * c * 0.5F;
                p.leftKnee += 0.6F * c * 0.7F;
                p.leftLegX -= 0.6F * c * 0.4F;
                p.headX += 0.6F * c * 0.25F;
            }
            case CRAWL -> {
                // 7.0.2 the crawl: sunk low, leaning into it, the head up on where he goes, his weight thrown to each stride's side.
                float c = age > beat.end() ? weight : 1.0F;
                float stride = (float)Math.sin(Math.PI * smooth(beat.through(age)));
                p.drop += 0.6F * c * 2.2F;
                p.lean += 0.6F * c * 0.5F;
                p.headX -= 0.6F * c * 0.35F;
                p.twist += 0.6F * c * (side >= 0 ? 0.14F : -0.14F) * stride;
                p.lunge += 0.6F * c * 1.5F * stride;
                p.rightArmX -= 0.6F * c * 0.9F;
                p.leftArmX -= 0.6F * c * 0.9F;
                p.rightElbow += 0.6F * c * 0.6F;
                p.leftElbow += 0.6F * c * 0.6F;
                p.rightKnee += 0.6F * c * 0.6F;
                p.leftKnee += 0.6F * c * 0.6F;
            }
            case CHASE -> {
                // 7.0.2 Hungry Maw sent out: his hand thrown after it, leaning on.
                float c = winding ? w : 1.0F - 0.3F * t;
                p.lean += k * 0.2F * c;
                pointArm(p, 1, k * c);
            }
            case CLAMP -> {
                // 7.0.2 dragging what the mouth holds: leaning away from its weight at his side.
                p.side -= 0.6F * weight * 0.12F;
                p.lean += 0.6F * weight * 0.08F;
            }
            case LURK -> {
                // 7.0.2 Mockingbird: open, exposed on purpose: chest up, arms hanging low and out from his sides, weight sunk into
                // his knees, swaying a little, his head down on what is in front of him.
                float c = winding ? w : 1.0F;
                float sway = (float)Math.sin(age * 0.35) * 0.07F;
                p.lean -= k * 0.12F * c;
                p.drop += k * 1.0F * c;
                p.headX += k * 0.1F * c;
                p.twist += k * sway * c;
                p.rightArmZ += k * 0.45F * c;
                p.leftArmZ -= k * 0.45F * c;
                p.rightArmX -= k * 0.15F * c;
                p.leftArmX -= k * 0.15F * c;
                p.rightElbow += k * 0.3F * c;
                p.leftElbow += k * 0.3F * c;
                p.rightKnee += k * 0.3F * c;
                p.leftKnee += k * 0.3F * c;
                p.rightLegX -= k * 0.15F * c;
                p.leftLegX -= k * 0.15F * c;
            }
            case HOOK -> {
                // 7.0.2 Mockingbird's slip: dropped low and aside out of the blow's way, the chest turned back on the attacker, an
                // arm up across it.
                float c = winding ? w : 1.0F - 0.7F * t;
                p.drop += k * 1.8F * c;
                p.side += k * 0.3F * c;
                p.twist -= k * 0.35F * c;
                p.lean += k * 0.18F * c;
                p.leftArmX -= k * 1.1F * c;
                p.leftArmY += k * 0.4F * c;
                p.rightArmZ += k * 0.5F * c;
                p.rightKnee += k * 0.5F * c;
                p.leftKnee += k * 0.3F * c;
            }
            case FLANK -> {
                // 7.0.2: the counterstrike: his chest turned with it as it comes round, his hand thrown after it.
                float c = winding ? 0.4F * w : 1.0F - 0.6F * t;
                p.twist += k * (side >= 0 ? 0.3F : -0.3F) * c;
                pointArm(p, side == 0 ? -1 : side, k * c);
            }
            case DIVE -> {
                // 7.0.2 the vault's dive: pitched forward over his tails, arms swept back, legs trailing.
                float c = winding ? w : 1.0F;
                p.lean += k * 0.95F * c;
                p.headX -= k * 0.5F * c;
                p.rightArmX += k * 1.2F * c;
                p.leftArmX += k * 1.2F * c;
                p.rightArmZ += k * 0.25F * c;
                p.leftArmZ -= k * 0.25F * c;
                p.rightLegX += k * 0.45F * c;
                p.leftLegX += k * 0.3F * c;
                p.rightKnee += k * 0.3F * c;
            }
            case WHIP -> {
                // 7.0.2 the puppet hurled: the chest wound back with the hoist, then thrown round after it, the arm flung out.
                float c = winding ? w : 1.0F - t;
                int hand = side == 0 ? 1 : side;
                p.twist += k * hand * (winding ? 0.4F * w : 0.4F - 1.0F * t);
                p.lean += k * (winding ? -0.15F * w : 0.3F * (1.0F - t));
                if (hand > 0) {
                    p.rightArmX += k * (winding ? -2.4F * w : -2.4F + 1.6F * t);
                } else {
                    p.leftArmX += k * (winding ? -2.4F * w : -2.4F + 1.6F * t);
                }
                p.lunge += k * (winding ? 0.0F : 2.0F * c);
            }
            case SEIZE -> {
                pointArm(p, side == 0 ? 1 : side, k * (winding ? w : 1.0F));
                if (!winding) {
                    raiseArms(p, side == 0 ? 1 : side, k * (1.0F - 0.3F * t), 0.8F);
                }
            }
            default -> {
            }
        }
    }

    /**
     * His own arm in a beat: a claw wound up over the shoulder and raked down across (from his right, his right hand; from his
     * left, his left; straight on, both brought down from overhead), or the jaws' lunge (thrown forward, arms swept back).
     */
    static void arm(RigPose p, GraftMove.Beat beat, boolean winding, float w, float t, float weight) {
        p.armHold += weight * 0.85F;
        if (beat.kind() == GraftMove.Kind.BITE) {
            float lunge = winding ? -0.3F * w : 1.0F - 0.6F * t;
            p.lean += weight * (winding ? -0.12F * w : 0.45F * (1.0F - 0.6F * t));
            p.lunge += weight * 4.5F * Math.max(0.0F, lunge);
            p.headX -= weight * 0.3F * (winding ? w : 1.0F - t);
            p.rightArmX += weight * 0.6F * (winding ? w : 1.0F - t);
            p.leftArmX += weight * 0.6F * (winding ? w : 1.0F - t);
            p.rightArmZ += weight * 0.5F * (winding ? w : 1.0F - t);
            p.leftArmZ -= weight * 0.5F * (winding ? w : 1.0F - t);
            return;
        }
        if (Math.abs(beat.from()) < 1.0E-3) {
            // Both hands from overhead, brought down together.
            float raise = winding ? w : 1.0F - t;
            float down = winding ? 0.0F : (float)Math.sin(Math.PI * t);
            p.rightArmX += weight * (-2.8F * raise - 0.5F * down);
            p.leftArmX += weight * (-2.8F * raise - 0.5F * down);
            p.rightElbow += weight * 0.6F * raise;
            p.leftElbow += weight * 0.6F * raise;
            p.lean += weight * (winding ? -0.12F * w : 0.45F * down);
            p.drop += weight * 1.8F * down;
            return;
        }
        int hand = beat.from() > 0.0 ? 1 : -1;
        float raisedX = -2.7F;
        float armX = winding ? raisedX * w : raisedX + 2.2F * t;
        float armZ = winding ? 0.55F * w : 0.55F - 0.95F * t;
        float armY = winding ? 0.0F : 0.65F * t;
        float elbow = winding ? 0.85F * w : 0.85F - 0.6F * t;
        if (hand > 0) {
            p.rightArmX += weight * armX;
            p.rightArmZ += weight * armZ;
            p.rightArmY += weight * armY;
            p.rightElbow += weight * elbow;
        } else {
            p.leftArmX += weight * armX;
            p.leftArmZ -= weight * armZ;
            p.leftArmY -= weight * armY;
            p.leftElbow += weight * elbow;
        }
        p.twist += weight * hand * (winding ? 0.35F * w : 0.35F - 0.8F * t);
        p.lean += weight * (winding ? 0.0F : 0.2F * (float)Math.sin(Math.PI * t));
        p.lunge += weight * (winding ? 0.0F : 2.0F * (float)Math.sin(Math.PI * t));
    }

    /**
     * 7.0.2 Scorpion Vault: sunk low as the tails go in, sprung up off them with his arms thrown up, then tucked, knees drawn up,
     * as he is carried high.
     */
    static void vault(RigPose p, double age, float weight) {
        float crouch = (float)(age < 2.0 ? smooth(age / 2.0) : 1.0 - smooth((age - 2.0) / 1.5));
        float tuck = (float)(smooth((age - 2.5) / 2.0) * (1.0 - smooth((age - 8.0) / 4.0)));
        p.drop += weight * 2.2F * crouch;
        p.lean += weight * (0.3F * crouch + 0.2F * tuck);
        p.rightKnee += weight * (0.6F * crouch + 1.3F * tuck);
        p.leftKnee += weight * (0.6F * crouch + 1.3F * tuck);
        p.rightLegX -= weight * (0.3F * crouch + 0.9F * tuck);
        p.leftLegX -= weight * (0.3F * crouch + 0.9F * tuck);
        p.rightArmX += weight * (0.6F * crouch - 1.9F * tuck);
        p.leftArmX += weight * (0.6F * crouch - 1.9F * tuck);
        p.rightArmZ += weight * 0.35F * tuck;
        p.leftArmZ -= weight * 0.35F * tuck;
    }

    /** The arm on {@code side} (+1 his right, -1 his left) thrust out toward where the kagune strikes. */
    static void pointArm(RigPose p, int side, float k) {
        if (side > 0) {
            p.rightArmX -= 1.35F * k;
            p.rightArmZ -= 0.1F * k;
        } else {
            p.leftArmX -= 1.35F * k;
            p.leftArmZ += 0.1F * k;
        }
    }

    /** Arms raised {@code by} (radians, forward and up): both for {@code side} 0, else that side's. */
    static void raiseArms(RigPose p, int side, float k, float by) {
        if (side >= 0) {
            p.rightArmX -= by * k;
        }
        if (side <= 0) {
            p.leftArmX -= by * k;
        }
    }

    static double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }
}
