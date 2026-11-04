package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.quinque.Quinque;

/**
 * 1.3.27: how a CCG investigator moves (pure): its own run, the same for every one of them; its parry; its dash. Its swings, its stance
 * and its case opening are the quinque's ({@link QuinquePose}), its flips a player's ({@code PaleMotions.flip}).
 *
 * <p>The run: an investigator's, trained: the body pitched forward from the hips, the head up against it to keep its eyes on what it
 * runs at, a long stride with the back foot kicked high, the shoulders turning against the hips, a small bounce at each footfall. The
 * free arm pumps, close in; the other carries what it holds: the case low at its side (heavy: it swings little), a one-handed blade
 * trailed low behind it and out from the body, a two-handed quinque kept in its stance a little lowered (its legs and body still the
 * run's).
 */
public final class AgentPose {
    private AgentPose() {
    }

    /** Bring the tip to the target and hold it there through the windup. */
    public static RigPose aimDawn(double age, float pitch) {
        RigPose pose = QuinquePose.hold(Quinque.DAWN);
        float raise = QuinquePose.ease(age / 5.0);
        float targetPitch = (float)Math.toRadians(Math.max(-55.0F, Math.min(55.0F, pitch)));
        QuinquePose.pitchBy(pose, Quinque.DAWN, 0.22F * (1.0F - raise) + targetPitch * raise);
        pose.leftArmZ = -0.30F * raise;
        pose.leftArmX = -0.42F * raise;
        pose.leftArmHold = raise;
        return pose;
    }

    /**
     * 2.0.33: the White Reaper's ground tendril (npc.ReaperTendril): Dawn raised point up, then driven point first into the ground before
     * him with both hands, his weight sunk on it, held there while the tendril rises, then pulled out back into his stance.
     */
    public static RigPose ground(double age) {
        RigPose p = QuinquePose.hold(Quinque.DAWN);
        float raise = QuinquePose.ease(age / 6.0);
        float drive = QuinquePose.ease((age - 6.0) / (com.hex.kagune.control.npc.ReaperTendril.PLANT - 6.0));
        float out = QuinquePose.ease((age - com.hex.kagune.control.npc.ReaperTendril.HOLD)
            / (com.hex.kagune.control.npc.ReaperTendril.DURATION - com.hex.kagune.control.npc.ReaperTendril.HOLD));
        float down = drive * (1.0F - out);
        float up = raise * (1.0F - drive);
        QuinquePose.pitchBy(p, Quinque.DAWN, -1.2F * up + 0.85F * down);
        p.lean += 0.22F * down - 0.08F * up;
        p.lunge += 1.5F * down - 1.0F * up;
        p.headX += 0.15F * down;
        // Sunk on it in a wide stance: the right foot forward, the left back, the hips lowered as far as the split legs lower them.
        float split = 0.45F * down;
        p.rightLegX = -split;
        p.leftLegX = 0.8F * split;
        p.legHold = Math.max(p.legHold, down);
        p.drop += 12.0F * (1.0F - (float)Math.cos(split));
        // The left hand comes onto the shaft above the right as it goes in.
        float both = Math.max(up, down);
        p.leftArmX = -1.05F * both;
        p.leftArmY = 0.35F * both;
        p.leftArmZ = 0.1F * both;
        p.leftArmHold = both;
        return p;
    }

    /** A loaded knee, a quick front kick, then the foot goes back under the hips. */
    public static RigPose kick(double age) {
        float windup = (float)Math.min(1.0, age / AgentRules.REAPER_KICK_WINDUP);
        float recover = (float)Math.max(0.0, 1.0 - (age - AgentRules.REAPER_KICK_WINDUP)
            / (AgentRules.REAPER_KICK_DURATION - AgentRules.REAPER_KICK_WINDUP));
        float extension = (float)Math.pow(windup, 2.5) * Math.min(1.0F, recover);
        RigPose p = QuinquePose.hold(Quinque.DAWN);
        p.rightLegX = -1.48F * extension;
        p.leftLegX = 0.22F * extension;
        p.rightLegZ = 0.08F * extension;
        p.legHold = 1.0F;
        p.lean -= 0.27F * extension;
        p.lunge += 2.0F * extension;
        p.leftArmX = -0.35F * extension;
        p.leftArmZ = -0.55F * extension;
        p.leftArmHold = extension;
        return p;
    }

    /** What the right hand carries as it runs. */
    public enum Carry {
        /** Its case shut (or nothing). */
        CASE,
        /** A one-handed quinque out (the blade). */
        BLADE,
        /** A two-handed one in its stance (the spear, the katana, the mace, the launcher). */
        STANCE
    }

    /** The run's lean forward, its head's lift against it, its stride, its arm pump, its bounce (model pixels). */
    static final float LEAN = 0.3F;
    static final float HEAD = -0.24F;
    static final float STRIDE = 1.1F;
    static final float KICK = 0.18F;
    static final float PUMP = 1.0F;
    static final float BOUNCE = 0.6F;
    static final float SHOULDERS = 0.14F;
    /** How far a two-handed stance is lowered as it runs (radians on both arms). */
    static final float STANCE_LOWER = 0.35F;

    /**
     * Its run {@code amount} of the way in (0: the vanilla walk, 1: all the run), {@code phase} into its stride (radians: the walk's own
     * limb swing times vanilla's 0.6662, so the feet keep pace with the ground), carrying {@code carry}.
     */
    public static RigPose run(double phase, float amount, Carry carry) {
        RigPose p = new RigPose();
        float k = Math.max(0.0F, Math.min(1.0F, amount));
        if (k <= 0.0F) {
            return p;
        }
        float s = (float)Math.sin(phase);
        float c = (float)Math.cos(phase);
        // The legs: forward is X-; the back leg kicked a little higher behind than the front one reaches.
        p.rightLegX = (STRIDE * s + KICK * Math.max(0.0F, s)) * k;
        p.leftLegX = (-STRIDE * s + KICK * Math.max(0.0F, -s)) * k;
        p.rightLegZ = 0.04F * k;
        p.leftLegZ = -0.04F * k;
        p.legHold = k;
        // The arms: each pumps with the opposite leg, a little out from the body, further forward than back.
        p.leftArmX = (PUMP * s - 0.12F * Math.abs(s) - 0.1F) * k;
        p.leftArmZ = -0.1F * k;
        p.rightArmX = (-PUMP * s - 0.12F * Math.abs(s) - 0.1F) * k;
        p.rightArmZ = 0.1F * k;
        switch (carry) {
            case CASE -> {
                p.rightArmX = (0.12F - 0.22F * s) * k;
                p.rightArmZ = 0.08F * k;
            }
            case BLADE -> {
                // 1.3.33: swung back and down, the thumb turned back, so the blade (held off the forearm's line) trails level behind.
                QuinqueGrip.aimRight(p, (0.55F + 0.08F * s) * k, 0.15F * k, (float)Math.PI * k);
            }
            case STANCE -> {
                // (the stance's arms are put on by whoever asked: see stanceRun)
            }
        }
        p.armHold = k;
        // The body: pitched forward, the head lifted against it, the shoulders turning against the stride, a bounce at each footfall.
        p.lean = LEAN * k;
        p.headX = HEAD * k;
        p.twist = SHOULDERS * s * k;
        p.drop = BOUNCE * Math.abs(c) * k;
        return p;
    }

    /**
     * A two-handed stance carried on the run ({@code q}'s, put on {@code run}): the chest turned half its stance's way (the weapon carried
     * across the body, as one runs with a rifle), lowered, bobbing with each stride; the left hand is put back on its grip by
     * QuinqueGrip.reach after everything else (AgentClient).
     */
    public static void stanceRun(RigPose run, Quinque q, double phase, float amount) {
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        if (h == null || amount <= 0.0F) {
            return;
        }
        float k = Math.max(0.0F, Math.min(1.0F, amount));
        float bob = 0.06F * (float)Math.sin(phase * 2.0) * k;
        run.twist *= 0.4F; // the weapon steadies the shoulders
        run.twist += h.twist() * (1.0F - 0.5F * k);
        QuinqueGrip.aimRight(run, h.pitch(), h.yaw(), h.roll());
        // Lowered about the level line across the chest as it now faces.
        QuinquePose.pitchBy(run, q, STANCE_LOWER * k + bob);
        run.leftArmX = h.left()[0];
        run.leftArmY = h.left()[1];
        run.leftArmZ = h.left()[2];
        run.armHold = 1.0F;
    }

    /** How long its parry is drawn (ticks). */
    public static final int PARRY_TICKS = 9;

    /** Its weight through a quick move {@code t} (0..1): in fast, held, out. */
    static float snap(double t) {
        if (t < 0.0 || t >= 1.0) {
            return 0.0F;
        }
        if (t < 0.28) {
            return QuinquePose.ease(t / 0.28);
        }
        if (t < 0.6) {
            return 1.0F;
        }
        return 1.0F - QuinquePose.ease((t - 0.6) / 0.4);
    }

    /**
     * Its parry {@code t} through (0..1 over {@link #PARRY_TICKS}): from its stance (or a held blade) the weapon whipped up and across
     * the body to meet the blow (the right arm raised and swung in, the shoulders turning into it; a two-handed one's left hand follows
     * on its grip by QuinqueGrip.reach).
     */
    public static RigPose parry(Quinque q, double t) {
        float k = snap(t);
        QuinqueHolds.Hold h = QuinqueHolds.of(q);
        RigPose p = QuinquePose.hold(q);
        if (q.twoHanded) {
            QuinqueGrip.aimRight(p, h.pitch() - 0.6F * k, h.yaw() - 0.3F * k, h.roll() - 0.25F * k);
        } else {
            // The blade up and across, its flat turned to take the blow.
            QuinqueGrip.aimRight(p, h.pitch() - 0.85F * k, h.yaw() - 0.5F * k, h.roll() + 1.2F * k);
        }
        p.twist -= 0.35F * k;
        p.lean -= 0.08F * k;
        return p;
    }

    /** How long a dash is drawn (ticks): its push, and the body coming back up. */
    public static final int DASH_TICKS = 11;

    /**
     * A dash ({@code way}: 0 forward, 1 back, 2 left, 3 right) {@code t} through (0..1 over {@link #DASH_TICKS}) put on {@code p}: low,
     * the legs split along it, the body leaning into it.
     */
    public static void dash(RigPose p, int way, double t) {
        float k = snap(t);
        if (k <= 0.0F) {
            return;
        }
        float legs = Math.max(p.legHold, k);
        switch (way) {
            case 0 -> {
                p.lean += 0.42F * k;
                p.rightLegX = p.rightLegX * (1.0F - k) - 0.75F * k;
                p.leftLegX = p.leftLegX * (1.0F - k) + 0.8F * k;
            }
            case 1 -> {
                p.lean -= 0.15F * k;
                p.rightLegX = p.rightLegX * (1.0F - k) + 0.7F * k;
                p.leftLegX = p.leftLegX * (1.0F - k) - 0.55F * k;
            }
            default -> {
                float side = way == 2 ? 1.0F : -1.0F;
                p.side += 0.3F * side * k;
                p.rightLegX *= 1.0F - k;
                p.leftLegX *= 1.0F - k;
                p.rightLegZ = p.rightLegZ * (1.0F - k) + 0.42F * k;
                p.leftLegZ = p.leftLegZ * (1.0F - k) - 0.42F * k;
            }
        }
        p.legHold = legs;
        p.drop += 2.2F * k;
        p.headX -= 0.1F * k;
    }

    /** How much of its run it is in at {@code speed} (blocks a tick along the ground): none at a walk, all of it at a sprint. */
    public static float running(double speed) {
        return (float)Math.max(0.0, Math.min(1.0, (speed - 0.16) / 0.12));
    }

    // ---------------------------------------------------------------- 1.3.28: its case carried by the handle

    /**
     * Its carrying arm's turn in the world (radians: the right arm hanging straight down, a touch out from the body so the case clears
     * its leg). The case's carried frames (tools/quinque_models.py CARRY_X, CARRY_Z) are solved for exactly this arm: the handle bar in
     * the middle of the fist, the case hanging plumb below it, its latches out, its length along the stride.
     */
    public static final float CARRY_X = 0.0F;
    public static final float CARRY_Z = 0.1F;
    /** The deploy values that pick the case's carried frames (wide arms, slim arms), and the first of its opening frames (above both). */
    public static final float CARRIED_WIDE = 0.05F;
    public static final float CARRIED_SLIM = 0.1F;
    public static final float OPENING_FRAME = 0.2F;
    /** How far the carrying arm swings with a walk's stride and a run's (radians each way): a heavy case swings little. */
    static final float SWAY_WALK = 0.1F;
    static final float SWAY_RUN = 0.18F;

    /**
     * The deploy value a shut case is drawn at ({@code shown}: how far it is drawn opening, 0 shut): its opening frames as they come;
     * otherwise, in an investigator's hand ({@code slim}: its arms), the carried frame solved for its arms, and anyone else's the plain
     * shut case (fitted to vanilla's holding arm; never a carried frame, which is fitted to an investigator's).
     */
    public static float caseFrame(float shown, boolean investigator, boolean slim) {
        if (shown >= OPENING_FRAME) {
            return shown;
        }
        if (!investigator || shown > 0.0F) {
            // (an investigator's case just starting to open has its arms already in the opening pose, QuinquePose.HELD: the plain frame)
            return 0.0F;
        }
        return slim ? CARRIED_SLIM : CARRIED_WIDE;
    }

    /**
     * Its case carried by the handle, on {@code p}'s right arm: the arm hangs plumb from the shoulder whatever the body is doing (the
     * lean and the sideways tip taken back out of it, so the case hangs straight down from the fist; the shoulders' turn too, so the
     * case stays along the stride and its corners never swing in against the hip), a touch out from the body, swinging {@code sway}
     * (radians) with the stride. Put on after everything else that moves the body.
     */
    public static void carry(RigPose p, float sway) {
        p.rightArmX = CARRY_X + sway - p.lean;
        p.rightArmY = -p.twist;
        p.rightArmZ = CARRY_Z - p.side;
        p.rightArmHold = 1.0F;
    }

    /**
     * The carrying arm's swing at {@code phase} of the stride: {@code walk} (0..1) of a walk's on vanilla's legs (which swing on
     * {@code cos}), {@code run} (0..1) of its run's (whose legs swing on {@code sin}); always against the right leg, as an arm swings.
     */
    public static float sway(double phase, float walk, float run) {
        float r = clamp01(run);
        float w = clamp01(walk) * (1.0F - r);
        return -(SWAY_WALK * w * (float)Math.cos(phase) + SWAY_RUN * r * (float)Math.sin(phase));
    }

    // ---------------------------------------------------------------- 1.3.28: standing about

    /** The lean back of its idle (radians), the tip of its upper body off its weighted leg, its breath's and its glance's periods (ticks). */
    static final float IDLE_LEAN = -0.1F;
    static final float IDLE_SIDE = 0.04F;
    static final double IDLE_BREATH = 74.0;
    static final double IDLE_GLANCE = 230.0;

    /** How still it stands at {@code speed} (blocks a tick along the ground): 1 at rest, none at a walk. */
    public static float still(double speed) {
        return (float)Math.max(0.0, Math.min(1.0, 1.0 - (speed - 0.015) / 0.05));
    }

    /**
     * Standing about ({@code amount}: 0..1 of it, put on {@code p}): relaxed and a little cocky. It leans back from the hips; its weight
     * is on its right leg (the case's side), the left one loose, a little forward, out and turned out; its upper body tips a touch away
     * from the case's weight; its chest is turned a little open, its head cocked; its free arm hangs back, loose. It breathes (the
     * chest and the shoulders rising and falling) and now and then looks off to one side. {@code age}: ticks; {@code seed} (its id)
     * keeps two of them from breathing and looking about as one.
     */
    public static void idle(RigPose p, double age, int seed, float amount) {
        float k = clamp01(amount);
        if (k <= 0.0F) {
            return;
        }
        double breath = Math.sin((age + (seed & 63) * 3.1) * (2.0 * Math.PI / IDLE_BREATH));
        double look = Math.sin((age + (seed >> 6 & 127) * 5.3) * (2.0 * Math.PI / IDLE_GLANCE));
        // Mostly looking ahead, now and then off to a side and back (the cube keeps it near zero most of the way round).
        float glance = (float)(look * look * look);
        p.lean += (IDLE_LEAN + 0.012F * (float)breath) * k;
        p.side += IDLE_SIDE * k;
        p.twist += -0.08F * k;
        p.headY += 0.38F * glance * k;
        p.headZ += 0.07F * k;
        p.rightShoulderY += -0.2F * (float)breath * k;
        p.leftShoulderY += -0.2F * (float)breath * k;
        // The legs: its weight on the right, the left loose.
        p.rightLegX = blend(p.rightLegX, 0.03F, k);
        p.rightLegY = blend(p.rightLegY, 0.0F, k);
        p.rightLegZ = blend(p.rightLegZ, 0.03F, k);
        p.leftLegX = blend(p.leftLegX, -0.15F, k);
        p.leftLegY = blend(p.leftLegY, -0.24F, k);
        p.leftLegZ = blend(p.leftLegZ, -0.09F, k);
        p.legHold = Math.max(p.legHold, k);
        // The free arm: hanging a little back of straight down (its lean taken out), out a little, loose.
        p.leftArmX = blend(p.leftArmX, 0.08F - p.lean + 0.02F * (float)breath, k);
        p.leftArmY = blend(p.leftArmY, 0.12F, k);
        p.leftArmZ = blend(p.leftArmZ, -0.07F - p.side, k);
        p.leftArmHold = Math.max(p.leftArmHold, k);
    }

    // ---------------------------------------------------------------- 2.0.43: the White Reaper standing about

    /** His lean back (radians), the tip of his upper body off his weighted leg, his chest turned open, and his slow breath (ticks). */
    static final float REAPER_LEAN = -0.26F;
    static final float REAPER_SIDE = 0.09F;
    static final float REAPER_TWIST = -0.15F;
    static final double REAPER_BREATH = 110.0;

    /**
     * 2.0.43 (the owner: "a small ambient animation to the White Reaper, making sure that he's just like really edgy and as if he knows
     * that he can kill you at any point. Like super relaxed overall, lean back and all ... doesn't have to have any movement, just a
     * pose"): the White Reaper's own idle in place of {@link #idle}. He leans well back from the hips, his weight sunk on his right leg
     * (the case's side) and his hip cocked, the left leg loose and turned out in front; his chest turned open, his chin a touch up so he
     * looks down at whoever is in front of him, his head cocked; his free hand in his coat's pocket. He hardly moves: a slow breath, no
     * glancing about (his eyes stay on you).
     */
    public static void reaperIdle(RigPose p, double age, int seed, float amount) {
        float k = clamp01(amount);
        if (k <= 0.0F) {
            return;
        }
        double breath = Math.sin((age + (seed & 63) * 3.1) * (2.0 * Math.PI / REAPER_BREATH));
        p.lean += (REAPER_LEAN + 0.008F * (float)breath) * k;
        p.side += REAPER_SIDE * k;
        p.twist += REAPER_TWIST * k;
        // The lean takes the head back with it: brought most of the way forward again, so his chin is only a little up.
        p.headX += 0.13F * k;
        p.headZ += 0.14F * k;
        p.rightShoulderY += -0.12F * (float)breath * k;
        p.leftShoulderY += -0.12F * (float)breath * k;
        // The legs: his weight sunk on the right, the left loose, out in front and turned out.
        p.rightLegX = blend(p.rightLegX, 0.06F, k);
        p.rightLegY = blend(p.rightLegY, 0.08F, k);
        p.rightLegZ = blend(p.rightLegZ, 0.04F, k);
        p.leftLegX = blend(p.leftLegX, -0.26F, k);
        p.leftLegY = blend(p.leftLegY, -0.42F, k);
        p.leftLegZ = blend(p.leftLegZ, -0.13F, k);
        p.legHold = Math.max(p.legHold, k);
        // The free hand in his pocket (solved through the game's transforms, tools/ArmSolve's way, on an investigator's straight arms:
        // vanilla's player model bends no elbow): the arm in to his hip, its hand sunk in the pocket at its side. Its lean and tip
        // taken out, so it stays there.
        p.leftArmX = blend(p.leftArmX, -0.327F - p.lean, k);
        p.leftArmY = blend(p.leftArmY, 0.392F, k);
        p.leftArmZ = blend(p.leftArmZ, 0.284F - p.side, k);
        p.leftArmHold = Math.max(p.leftArmHold, k);
    }

    static float blend(float from, float to, float k) {
        return from + (to - from) * k;
    }

    static float clamp01(float v) {
        return Float.isFinite(v) ? Math.max(0.0F, Math.min(1.0F, v)) : 0.0F;
    }
}
