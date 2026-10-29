package com.hex.kagune.control.client.animation;

/**
 * 1.0.5: War's body (pure, so the previewer poses it as the game does). 1.0.7: a god's stance, the way the fused Zamasu stands: upright
 * and leaning back from the hips, chest out, the arms hanging open and away from his sides, the legs a little apart, breathing slow;
 * 1.0.9: and the head always bowed, glaring up from under the brow as Kid Buu does. And wrong: his head is never still. It settles at a new angle every few seconds, snapping
 * there; it jerks in small violent twitches between; a fine tremor runs through it all. Walking takes some of it out of him, never
 * all.
 */
public final class WarPose {
    /** Seconds a head angle holds before the next, and how fast it snaps there (share of the slot). */
    static final double TILT_SLOT = 3.2;
    static final double TILT_SNAP = 0.07;
    /** The farthest the head goes over (radians about the face) and round (about the neck). */
    static final double TILT_ROLL = 0.5;
    static final double TILT_TURN = 0.35;
    /**
     * The stance: how far back he leans (radians) and how far out the arms hang; 1.0.9: and the head always bowed (radians on the neck,
     * over the lean back: about twenty degrees down in all), the way Kid Buu glares up from under his brow.
     */
    static final double STANCE_LEAN = 0.14;
    static final double STANCE_BOW = 0.46;
    static final double STANCE_ARMS = 0.38;
    /** Twitch checks a second, the chance of one at each, and how fast one dies away (per second). */
    static final double TWITCH_RATE = 7.0;
    static final double TWITCH_CHANCE = 0.13;
    static final double TWITCH_DECAY = 22.0;

    private WarPose() {
    }

    /** War's pose onto {@code p}: {@code time} in ticks, {@code seed} his own, at {@code weight} (0..1), {@code moving} how much he walks. */
    public static void apply(RigPose p, double time, int seed, float weight, float moving) {
        if (weight <= 0.0F) {
            return;
        }
        double w = weight;
        double still = 1.0 - 0.5 * Math.max(0.0, Math.min(1.0, moving));
        double t = time / 20.0;
        // The stance: leaning back, the head bowed, glaring up, the arms open and away from his sides, the palms turned out, the feet apart.
        double breathe = Math.sin(t * 0.9 + seed) * 0.5 + 0.5;
        double god = w * still;
        p.lean -= (float)(god * (STANCE_LEAN + 0.025 * breathe));
        p.headX += (float)(w * STANCE_BOW);
        p.rightArmZ += (float)(w * (STANCE_ARMS + 0.03 * breathe));
        p.leftArmZ -= (float)(w * (STANCE_ARMS + 0.03 * breathe));
        p.rightArmX -= (float)(w * 0.08);
        p.leftArmX -= (float)(w * 0.08);
        p.rightArmY -= (float)(w * 0.35);
        p.leftArmY += (float)(w * 0.35);
        p.rightLegZ += (float)(god * 0.07);
        p.leftLegZ -= (float)(god * 0.07);
        // The tilt: a new angle every slot, snapped to.
        long slot = (long)Math.floor(t / TILT_SLOT);
        double into = (t - slot * TILT_SLOT) / TILT_SLOT;
        double snap = smooth(Math.min(1.0, into / TILT_SNAP));
        double roll = lerp(roll(seed, slot - 1), roll(seed, slot), snap);
        double turn = lerp(turn(seed, slot - 1), turn(seed, slot), snap);
        p.headZ += (float)(w * still * roll);
        p.headY += (float)(w * still * turn);
        // Twitches: a jolt now and then, gone in a fraction of a second.
        long beat = (long)Math.floor(t * TWITCH_RATE);
        for (long b = beat - 3; b <= beat; b++) {
            if (hash(seed, b, 1) >= TWITCH_CHANCE) {
                continue;
            }
            double age = t - b / TWITCH_RATE;
            if (age < 0.0) {
                continue;
            }
            double jolt = Math.exp(-age * TWITCH_DECAY) * w;
            double kind = hash(seed, b, 2);
            double sign = hash(seed, b, 3) < 0.5 ? -1.0 : 1.0;
            if (kind < 0.55) {
                p.headY += (float)(jolt * sign * 0.55);
                p.headZ += (float)(jolt * -sign * 0.35);
            } else if (kind < 0.8) {
                p.headX += (float)(jolt * sign * 0.4);
            } else {
                // A shoulder jerks up and the arm with it.
                p.leftArmZ -= (float)(jolt * 0.35);
                p.leftArmX -= (float)(jolt * sign * 0.25);
                p.headZ += (float)(jolt * 0.2);
            }
        }
        // The tremor.
        p.headY += (float)(w * 0.018 * Math.sin(t * 41.0 + seed));
        p.headX += (float)(w * 0.014 * Math.sin(t * 37.0 + seed * 2.0));
        p.leftArmX += (float)(w * 0.012 * Math.sin(t * 33.0 + seed * 3.0));
    }

    /**
     * 1.0.6: War's moves on the body ({@code KingMove}), {@code age} ticks in, at {@code weight}: the scythe drawn back, swept round,
     * trailing through a dash, held across him, raised overhead and brought down; the hands that seize and crush; the storm he
     * spins in, the leap, the eye he looks up with.
     */
    public static void act(RigPose p, com.hex.kagune.control.king.KingMove move, double age, int level, float weight) {
        if (weight <= 0.0F || move == null) {
            return;
        }
        double w = weight;
        double u = move.progress(age);
        double hold = smooth(Math.min(1.0, age / 5.0));
        // 1.0.13: the moves of the six abilities as they are now.
        switch (move) {
            case GRASP -> {
                // The free hand thrown out at it, the centipedes pouring from the arm; a clench as it is crushed; drawn back in.
                double out = smooth(Math.min(1.0, age / 3.0)) * (1.0 - smooth(Math.max(0.0, (u - 0.85) / 0.15)));
                double crush = Math.max(0.0, 1.0 - Math.abs(age - com.hex.kagune.control.king.WarRules.GRASP_CRUSH) / 4.0);
                p.leftArmX -= (float)(w * (1.55 - 0.25 * crush) * out);
                p.leftArmY += (float)(w * 0.15 * out);
                p.leftArmZ -= (float)(w * 0.05 * out);
                p.leftElbow += (float)(w * 0.5 * crush);
                p.twist += (float)(w * 0.22 * out);
                p.lean += (float)(w * (0.1 + 0.12 * crush) * out);
                p.headX += (float)(w * 0.15 * crush);
            }
            case DASH -> {
                double env = Math.sin(Math.PI * Math.min(1.0, u));
                p.lean += (float)(w * 0.55 * env);
                p.drop += (float)(w * 1.6 * env);
                p.rightArmX += (float)(w * 1.2 * env);
                p.rightArmZ += (float)(w * 0.5 * env);
                p.leftArmX += (float)(w * 0.6 * env);
                p.headX -= (float)(w * 0.35 * env);
            }
            case CLEAVE -> {
                // Raised high over the head and brought down with all his weight, then up out of it.
                double land = com.hex.kagune.control.king.WarRules.CLEAVE_LANDS / (double)com.hex.kagune.control.king.WarRules.CLEAVE_TICKS;
                double up = smooth(Math.min(1.0, u / (land * 0.7)));
                double down = smooth(Math.max(0.0, Math.min(1.0, (u - land * 0.7) / (land * 0.3))));
                double back = 1.0 - smooth(Math.max(0.0, (u - 0.7) / 0.3));
                p.rightArmX += (float)(w * lerp(-2.9 * up, -0.15, down) * back);
                p.rightArmZ += (float)(w * lerp(0.15, -0.25, down) * back);
                p.twist += (float)(w * lerp(-0.45 * up, 0.5, down) * back);
                p.lean += (float)(w * lerp(-0.25 * up, 0.62, down) * back);
                p.drop += (float)(w * 2.4 * down * back);
                p.leftArmX -= (float)(w * 0.6 * up * back);
                p.headX += (float)(w * lerp(-0.3 * up, 0.25, down) * back);
            }
            case SPIRE -> {
                // The bladed arm raised, then driven down into the ground before him, low over it, the free arm out.
                double slam = com.hex.kagune.control.king.WarRules.SPIRE_SLAM / (double)com.hex.kagune.control.king.WarRules.SPIRE_TICKS;
                double up = smooth(Math.min(1.0, u / (slam * 0.75)));
                double down = smooth(Math.max(0.0, Math.min(1.0, (u - slam * 0.75) / (slam * 0.25))));
                double back = 1.0 - smooth(Math.max(0.0, (u - 0.75) / 0.25));
                p.rightArmX += (float)(w * lerp(-2.9 * up, -0.35, down) * back);
                p.lean += (float)(w * lerp(-0.2 * up, 0.78, down) * back);
                p.drop += (float)(w * 3.0 * down * back);
                p.leftArmZ -= (float)(w * 0.9 * down * back);
                p.leftArmX += (float)(w * 0.3 * down * back);
                p.headX += (float)(w * lerp(-0.35 * up, 0.15, down) * back);
            }
            case DEFIANCE -> {
                p.rightArmX -= (float)(w * 1.4 * hold);
                p.rightArmY += (float)(w * 0.7 * hold);
                p.rightArmZ -= (float)(w * 0.3 * hold);
                p.lean += (float)(w * 0.12 * hold);
                p.drop += (float)(w * 1.2 * hold);
            }
            case COUNTER -> {
                // From behind him: the bladed arm swept across, fast, then back.
                double cut = smooth(Math.min(1.0, u / 0.35));
                double back = 1.0 - smooth(Math.max(0.0, (u - 0.6) / 0.4));
                p.rightArmX += (float)(w * lerp(1.1, -1.5, cut) * back);
                p.rightArmY += (float)(w * 0.7 * cut * back);
                p.rightArmZ += (float)(w * lerp(0.55, -0.2, cut) * back);
                p.twist += (float)(w * lerp(-0.45, 0.7, cut) * back);
                p.lean += (float)(w * 0.2 * back);
                p.drop += (float)(w * 1.0 * back);
            }
            case COCOON -> {
                // Curled in under the coils: arms crossed over his chest, the head bowed.
                p.rightArmX -= (float)(w * 1.25 * hold);
                p.rightArmZ -= (float)(w * 0.55 * hold);
                p.leftArmX -= (float)(w * 1.25 * hold);
                p.leftArmZ += (float)(w * 0.55 * hold);
                p.rightElbow += (float)(w * 1.4 * hold);
                p.leftElbow += (float)(w * 1.4 * hold);
                p.headX += (float)(w * 0.45 * hold);
                p.lean += (float)(w * 0.3 * hold);
                p.drop += (float)(w * 1.2 * hold);
            }
            case BURST -> {
                double open = smooth(Math.min(1.0, u / 0.25)) * (1.0 - smooth(Math.max(0.0, (u - 0.7) / 0.3)));
                p.rightArmZ += (float)(w * 1.4 * open);
                p.leftArmZ -= (float)(w * 1.4 * open);
                p.rightArmX -= (float)(w * 0.6 * open);
                p.leftArmX -= (float)(w * 0.6 * open);
                p.headX -= (float)(w * 0.6 * open);
                p.lean -= (float)(w * 0.3 * open);
            }
            case SUN -> {
                // 1.0.15: the hand driven down at the ground he opens; both arms lifting as they rise, held high, brought down hard
                // with the slam, crouched in it; then up out of it.
                double open = com.hex.kagune.control.king.WarRules.SUN_OPEN;
                double down = smooth(Math.min(1.0, age / (open * 0.6))) * (1.0 - smooth(Math.max(0.0, Math.min(1.0, (age - open) / 6.0))));
                double lift = com.hex.kagune.control.king.WarRules.lift(age);
                double slam = com.hex.kagune.control.king.WarRules.SUN_SLAM;
                double crash = Math.max(0.0, 1.0 - Math.abs(age - slam) / 6.0);
                double raised = age < slam ? smooth(Math.min(1.0, Math.max(0.0, (age - open) / 10.0))) : 0.0;
                double back = 1.0 - smooth(Math.max(0.0, Math.min(1.0, (age - slam - 4.0) / 12.0)));
                p.rightArmX += (float)(w * (-1.1 * down + lerp(-0.6, -2.7, lift) * raised - 0.9 * crash * back));
                p.leftArmX += (float)(w * (lerp(-0.6, -2.7, lift) * raised - 0.9 * crash * back));
                p.rightArmZ += (float)(w * (0.25 * raised + 0.2 * crash));
                p.leftArmZ -= (float)(w * (0.25 * raised + 0.2 * crash));
                p.lean += (float)(w * (0.45 * down - 0.2 * lift * raised + 0.6 * crash * back));
                p.drop += (float)(w * (1.6 * down + 2.4 * crash * back));
                p.headX += (float)(w * (0.35 * down - 0.6 * lift * raised + 0.3 * crash * back));
            }
            case STRIKE -> strike(p, com.hex.kagune.control.king.WarStrike.byOrdinal(level), age, w);
            case CONE -> cone(p, age, w);
            case RISE -> rise(p, age, w);
            case BLITZ -> blitz(p, age, w);
            case SUNDER -> sunder(p, age, w);
            case WYRM -> dominion(p, age, w);
            case REAPER -> reaper(p, level, age, level == com.hex.kagune.control.king.WarRules.REAPER_BACK ? com.hex.kagune.control.king.WarRules.REAPER_DRAG : -1, w);
            case DEFY -> defy(p, level, age, w);
            case SPIRE_AIM -> spireAim(p, age, w);
            case SPIRE_SEND -> spireSend(p, age, 26.0, 48.0, w);
            case CHARGE -> {
                // 1.0.20: the heavy charging: low, braced on the free arm, the blade dragged back along the ground behind him, shaking
                // harder the longer it is held.
                double full = com.hex.kagune.control.king.KingCombo.charge(age);
                double shake = 0.02 + 0.06 * full;
                double t = age / 20.0;
                p.drop += (float)(w * (1.3 + 0.5 * full) * hold);
                p.lean += (float)(w * (0.35 + 0.1 * full) * hold);
                p.twist -= (float)(w * (0.35 + 0.15 * full) * hold);
                p.rightArmX += (float)(w * (0.55 + shake * Math.sin(t * 37.0)) * hold);
                p.rightArmZ += (float)(w * 0.4 * hold);
                p.leftArmX -= (float)(w * (0.85 + shake * Math.sin(t * 31.0 + 1.0)) * hold);
                p.leftElbow += (float)(w * 0.4 * hold);
                p.headX += (float)(w * (0.1 + shake * Math.sin(t * 43.0)) * hold);
                p.rightLegZ += (float)(w * 0.18 * hold);
                p.leftLegZ -= (float)(w * 0.18 * hold);
                p.armHold += (float)(w * hold);
            }
            default -> {
            }
        }
    }

    /** 1.0.27: one beat of a move: {@code from} eased into {@code to} over ticks {@code a..b} ({@link #smooth}). */
    static double beat(double age, double a, double b) {
        return smooth(clamp01((age - a) / Math.max(1.0E-3, b - a)));
    }

    /** 1.0.27: a pose run through key poses, {@code at[i]} the tick it is wholly {@code keys[i]}, eased between them. */
    static void keyed(RigPose p, double age, double w, double[] at, RigPose... keys) {
        if (age <= at[0]) {
            p.add(keys[0], (float)(w * smooth(clamp01(age / Math.max(1.0, at[0])))));
            return;
        }
        for (int i = 1; i < at.length; i++) {
            if (age < at[i]) {
                double t = beat(age, at[i - 1], at[i]);
                p.add(keys[i - 1], (float)(w * (1.0 - t)));
                p.add(keys[i], (float)(w * t));
                return;
            }
        }
        p.add(keys[keys.length - 1], (float)w);
    }

    /**
     * 1.0.27: the RMB's swipe: the free hand across to his right shoulder, the chest turned with it; swept out low and wide to his left
     * at the ground before him as the spikes break it, the weight down behind it; held there, palm to the ground, while they run on.
     */
    static void cone(RigPose p, double age, double w) {
        int swipe = com.hex.kagune.control.king.WarRules.CONE_SWIPE;
        RigPose coil = new RigPose();
        coil.leftArmX = -1.5F; coil.leftArmY = -0.95F; coil.leftElbow = 1.1F; coil.twist = -0.42F; coil.drop = 0.5F; coil.lean = 0.05F;
        coil.rightArmZ = 0.35F; coil.rightArmX = 0.25F; coil.headY = -0.15F;
        RigPose sweep = new RigPose();
        sweep.leftArmX = -0.95F; sweep.leftArmY = 0.55F; sweep.leftArmZ = -0.35F; sweep.leftElbow = 0.1F; sweep.twist = 0.38F; sweep.lean = 0.42F;
        sweep.drop = 1.3F; sweep.lunge = 1.1F; sweep.headX = 0.25F; sweep.rightArmZ = 0.55F; sweep.rightArmX = 0.35F;
        RigPose held = new RigPose().add(sweep, 1.0F);
        held.leftArmX = -1.1F; held.leftArmY = 0.25F; held.lean = 0.35F; held.drop = 1.1F; held.twist = 0.2F;
        double end = com.hex.kagune.control.king.WarRules.CONE_TICKS;
        keyed(p, age, w * (1.0 - beat(age, end - 4.0, end + 0.5)), new double[] {swipe - 1.0, swipe + 0.5, swipe + 3.0}, coil, sweep, held);
        double hold = w * (1.0 - beat(age, end - 3.0, end + 0.5));
        p.armHold += (float)hold;
    }

    /** 1.0.27: King's Execution at nothing: down out of the dash, then both hands thrown up, the ground's spikes rising with them. */
    static void rise(RigPose p, double age, double w) {
        int spikes = com.hex.kagune.control.king.WarRules.RISE_SPIKES;
        double end = com.hex.kagune.control.king.WarRules.RISE_TICKS;
        RigPose low = new RigPose();
        low.drop = 1.8F; low.lean = 0.45F; low.rightArmX = 0.35F; low.leftArmX = 0.35F; low.rightArmZ = 0.45F; low.leftArmZ = -0.45F;
        low.headX = 0.25F;
        RigPose up = new RigPose();
        up.rightArmX = -2.75F; up.leftArmX = -2.75F; up.rightArmZ = 0.35F; up.leftArmZ = -0.35F; up.lean = -0.3F; up.drop = -0.3F;
        up.headX = -0.45F; up.rightElbow = 0.15F; up.leftElbow = 0.15F;
        RigPose held = new RigPose().add(up, 1.0F);
        held.rightArmX = -2.5F; held.leftArmX = -2.5F; held.rightArmZ = 0.55F; held.leftArmZ = -0.55F; held.lean = -0.18F; held.drop = 0.1F;
        keyed(p, age, w * (1.0 - beat(age, end - 5.0, end)), new double[] {spikes - 1.0, spikes + 1.0, spikes + 6.0}, low, up, held);
        p.armHold += (float)(w * (1.0 - beat(age, end - 3.0, end)));
    }

    /**
     * 1.0.27: King's Execution's blitz, beat for beat with the server ({@code WarRules.BLITZ_*}): in low off the dash, a straight right
     * into it; the blade swept across it in the air; a rising kick; then the last one, slow: drawn all the way back, the body coiled
     * round it, held, held; and driven into its gut, through it and down. In the air the whole way (his legs are the move's).
     */
    static void blitz(RigPose p, double age, double w) {
        RigPose in = new RigPose();
        in.drop = 1.2F; in.lean = 0.6F; in.rightArmX = 0.7F; in.rightArmZ = 0.3F; in.leftArmX = -0.6F; in.leftElbow = 1.0F; in.headX = -0.2F;
        in.twist = 0.2F;
        RigPose punch = new RigPose();
        punch.rightArmX = -1.65F; punch.rightArmY = -0.1F; punch.twist = -0.5F; punch.lean = 0.3F; punch.lunge = 1.8F; punch.drop = 0.6F;
        punch.leftArmX = -0.4F; punch.leftElbow = 1.3F; punch.leftArmZ = -0.3F;
        RigPose draw = new RigPose();
        draw.rightArmX = -1.4F; draw.rightArmY = -1.05F; draw.rightArmZ = 0.4F; draw.twist = -0.6F; draw.lean = 0.1F; draw.leftArmX = -0.5F;
        draw.leftElbow = 0.8F; draw.rightKnee = 0.8F; draw.leftKnee = 1.1F; draw.leftLegX = -0.5F;
        RigPose cut = new RigPose();
        cut.rightArmX = -1.45F; cut.rightArmY = 1.05F; cut.rightArmZ = -0.25F; cut.twist = 0.65F; cut.lean = 0.25F; cut.lunge = 1.0F;
        cut.leftArmZ = -0.6F; cut.rightKnee = 0.9F; cut.leftKnee = 1.2F; cut.leftLegX = -0.4F;
        RigPose chamber = new RigPose();
        chamber.rightLegX = -1.1F; chamber.rightKnee = 1.9F; chamber.lean = -0.25F; chamber.leftArmX = -0.6F; chamber.leftArmZ = -0.4F;
        chamber.rightArmZ = 0.6F; chamber.leftKnee = 0.5F; chamber.headX = -0.2F;
        RigPose kick = new RigPose();
        kick.rightLegX = -2.0F; kick.rightKnee = 0.15F; kick.lean = -0.55F; kick.leftLegX = 0.35F; kick.leftKnee = 0.4F; kick.rightArmZ = 0.9F;
        kick.leftArmZ = -0.9F; kick.headX = -0.4F; kick.lunge = 0.6F;
        // The last one: wound all the way back (the shoulder turned away, the fist at the hip), held through the slow beat.
        RigPose wind = new RigPose();
        wind.rightArmX = 0.85F; wind.rightArmZ = 0.35F; wind.rightElbow = 1.5F; wind.twist = -0.95F; wind.lean = -0.1F; wind.leftArmX = -1.35F;
        wind.leftArmY = 0.2F; wind.leftElbow = 0.25F; wind.rightKnee = 1.3F; wind.leftKnee = 0.8F; wind.leftLegX = -0.7F; wind.rightLegX = 0.3F;
        wind.headX = 0.15F; wind.headY = 0.25F;
        RigPose cocked = new RigPose().add(wind, 1.0F);
        cocked.rightArmX = 1.05F; cocked.twist = -1.1F; cocked.rightElbow = 1.7F; cocked.leftArmX = -1.5F;
        RigPose gut = new RigPose();
        gut.rightArmX = -1.25F; gut.rightArmY = -0.1F; gut.rightElbow = 0.0F; gut.twist = 0.7F; gut.lean = 0.85F; gut.lunge = 2.2F; gut.drop = 0.4F;
        gut.leftArmX = 0.6F; gut.leftArmZ = -0.4F; gut.leftElbow = 0.6F; gut.leftLegX = 0.6F; gut.rightLegX = -0.5F; gut.rightKnee = 0.5F;
        gut.headX = 0.45F;
        RigPose through = new RigPose().add(gut, 1.0F);
        through.rightArmX = -0.6F; through.lean = 1.05F; through.headX = 0.6F;
        double arrive = com.hex.kagune.control.king.WarRules.BLITZ_ARRIVE;
        double hit = com.hex.kagune.control.king.WarRules.BLITZ_PUNCH;
        double blink = com.hex.kagune.control.king.WarRules.BLITZ_BLINK;
        double slash = com.hex.kagune.control.king.WarRules.BLITZ_CUT;
        double kicked = com.hex.kagune.control.king.WarRules.BLITZ_KICK;
        double winding = com.hex.kagune.control.king.WarRules.BLITZ_WIND;
        double driven = com.hex.kagune.control.king.WarRules.BLITZ_GUT;
        double end = com.hex.kagune.control.king.WarRules.BLITZ_TICKS;
        keyed(p, age, w * (1.0 - beat(age, end - 12.0, end)),
            new double[] {arrive - 0.5, hit, blink, slash, slash + 1.5, kicked - 0.8, kicked + 0.6, winding + 2.0, driven - 1.5, driven, driven + 6.0},
            in, punch, draw, cut, cut, chamber, kick, wind, cocked, gut, through);
        // The slow beat shakes with what it holds back.
        double strain = beat(age, winding, driven - 3.0) * (1.0 - beat(age, driven - 1.0, driven));
        p.rightArmX += (float)(w * 0.05 * strain * Math.sin(age * 4.7));
        p.headX += (float)(w * 0.04 * strain * Math.sin(age * 5.9 + 1.0));
        double held = w * (1.0 - beat(age, end - 10.0, end));
        p.armHold += (float)held;
        p.legHold += (float)held;
    }

    /**
     * 1.0.27: Black Sun: in low off the dash; an uppercut that throws it into the sky, rising up on his toes with it; crouched and
     * sprung up after it; over it at the top, the fist (1.0.33: his bare left) drawn back behind him, the whole body bent back like a bow (the slow beat;
     * 1.0.33: drawn on and on, coiling tighter, shaking under it); the punch unleashed, everything behind it, and the fist left buried
     * in it while it feels it; arms flung wide after as it flies; then down, the knees drawn up, falling.
     */
    static void sunder(RigPose p, double age, double w) {
        RigPose in = new RigPose();
        in.drop = 1.6F; in.lean = 0.55F; in.rightArmX = 0.8F; in.rightArmZ = 0.25F; in.rightElbow = 1.0F; in.leftArmX = -0.5F; in.leftElbow = 1.0F;
        in.twist = -0.35F;
        RigPose upper = new RigPose();
        upper.rightArmX = -2.9F; upper.rightArmZ = 0.15F; upper.rightElbow = 0.7F; upper.twist = 0.35F; upper.lean = -0.25F; upper.drop = -0.9F;
        upper.headX = -0.55F; upper.leftArmX = 0.4F; upper.leftArmZ = -0.5F;
        RigPose crouch = new RigPose();
        crouch.drop = 2.0F; crouch.lean = 0.3F; crouch.rightArmX = 0.6F; crouch.leftArmX = 0.6F; crouch.rightArmZ = 0.4F; crouch.leftArmZ = -0.4F;
        crouch.headX = -0.6F;
        RigPose spring = new RigPose();
        spring.rightArmX = -2.6F; spring.leftArmX = 0.5F; spring.leftArmZ = -0.3F; spring.lean = -0.15F; spring.headX = -0.7F; spring.rightLegX = -0.5F;
        spring.rightKnee = 1.3F; spring.leftLegX = 0.2F; spring.leftKnee = 0.4F;
        // 1.0.33: the punch is his bare left fist (the right is the scythe's): drawn back on the left, the right thrown out before him to
        // aim, the body bent back like a bow the other way round; then driven down into it from over it.
        RigPose bow = new RigPose();
        bow.leftArmX = 1.25F; bow.leftArmZ = -0.5F; bow.leftElbow = 1.8F; bow.twist = 1.2F; bow.lean = -0.45F; bow.rightArmX = -1.6F;
        bow.rightArmY = -0.15F; bow.rightArmZ = 0.2F; bow.leftKnee = 1.5F; bow.rightKnee = 0.9F; bow.leftLegX = 0.4F; bow.rightLegX = -0.8F;
        bow.headX = 0.3F; bow.headY = -0.3F;
        RigPose drawn = new RigPose().add(bow, 1.0F);
        drawn.leftArmX = 1.45F; drawn.twist = 1.35F; drawn.lean = -0.55F; drawn.rightArmX = -1.75F;
        RigPose coiled = new RigPose().add(drawn, 1.0F);
        coiled.leftArmX = 1.7F; coiled.leftArmZ = -0.62F; coiled.leftElbow = 2.0F; coiled.twist = 1.48F; coiled.lean = -0.68F;
        coiled.rightArmX = -1.95F; coiled.headX = 0.38F; coiled.leftKnee = 1.75F; coiled.rightKnee = 1.05F;
        RigPose loose = new RigPose();
        loose.leftArmX = -1.4F; loose.leftArmZ = -0.05F; loose.leftElbow = 0.0F; loose.twist = -0.85F; loose.lean = 0.8F; loose.lunge = 2.0F;
        loose.rightArmX = 0.7F; loose.rightArmZ = 0.9F; loose.rightLegX = 0.8F; loose.leftLegX = -0.6F; loose.leftKnee = 0.4F; loose.headX = 0.55F;
        RigPose after = new RigPose();
        after.rightArmZ = 1.2F; after.leftArmZ = -1.2F; after.rightArmX = -0.6F; after.leftArmX = -0.4F; after.lean = 0.25F; after.headX = 0.45F;
        after.rightKnee = 1.0F; after.leftKnee = 1.4F; after.rightLegX = -0.5F; after.leftLegX = -0.8F;
        double arrive = com.hex.kagune.control.king.WarRules.SUNDER_ARRIVE;
        double up = com.hex.kagune.control.king.WarRules.SUNDER_UPPER;
        double leap = com.hex.kagune.control.king.WarRules.SUNDER_LEAP;
        double beside = com.hex.kagune.control.king.WarRules.SUNDER_BESIDE;
        double punch = com.hex.kagune.control.king.WarRules.SUNDER_PUNCH;
        double launch = com.hex.kagune.control.king.WarRules.SUNDER_LAUNCH;
        double end = launch + 30.0;
        keyed(p, age, w * (1.0 - beat(age, end - 10.0, end)),
            new double[] {arrive - 0.5, up, leap - 0.5, leap + 2.0, beside, (beside + punch) * 0.5, punch - 2.0, punch, launch, launch + 10.0},
            in, upper, crouch, spring, bow, drawn, coiled, loose, loose, after);
        // The draw shakes him, harder the longer it is held; the blow's own shock runs back up the arm while it is in it.
        double strain = beat(age, beside, punch - 4.0) * (1.0 - beat(age, punch - 1.5, punch));
        double shake = 0.05 + 0.08 * beat(age, beside, punch - 2.0);
        p.leftArmX += (float)(w * shake * strain * Math.sin(age * 4.3));
        p.headX += (float)(w * 0.8 * shake * strain * Math.sin(age * 6.1 + 0.6));
        p.twist += (float)(w * 0.5 * shake * strain * Math.sin(age * 3.1 + 1.3));
        double shock = beat(age, punch, punch + 1.0) * (1.0 - beat(age, punch + 2.0, launch));
        p.leftArmX += (float)(w * 0.07 * shock * Math.sin(age * 5.3));
        p.lean += (float)(w * 0.04 * shock * Math.sin(age * 4.1 + 0.4));
        double held = w * (1.0 - beat(age, end - 8.0, end));
        p.armHold += (float)held;
        p.legHold += (float)held;
    }

    /**
     * 1.0.33: Centipede Dominion's little turning ({@code WarRules.WYRM_PRELUDE}; after it his body is the small Dragon's, not drawn):
     * doubled over, clawing at the air, shaking harder and harder, then thrown back, arms flung out, as it tears out of his back.
     */
    static void dominion(RigPose p, double age, double w) {
        double prelude = com.hex.kagune.control.king.WarRules.WYRM_PRELUDE;
        if (age > prelude + 2.0) {
            return;
        }
        double torn = beat(age, prelude - 3.0, prelude);
        double hunch = beat(age, 0.0, 4.0) * (1.0 - torn);
        double shake = Math.sin(age * 5.7) * (0.04 + 0.08 * beat(age, 0.0, prelude - 3.0)) * hunch;
        p.lean += (float)(w * (0.8 * hunch - 0.5 * torn));
        p.drop += (float)(w * 1.5 * hunch);
        p.headX += (float)(w * (0.3 * hunch - 0.75 * torn + shake));
        p.rightArmX += (float)(w * (-0.7 * hunch + 0.5 * torn + shake));
        p.leftArmX += (float)(w * (-0.7 * hunch + 0.5 * torn - shake));
        p.rightArmZ += (float)(w * (0.35 * hunch + 1.15 * torn));
        p.leftArmZ -= (float)(w * (0.35 * hunch + 1.15 * torn));
        p.rightElbow += (float)(w * 1.0 * hunch);
        p.leftElbow += (float)(w * 1.0 * hunch);
        p.rightKnee += (float)(w * 1.1 * hunch);
        p.leftKnee += (float)(w * 1.1 * hunch);
        p.twist += (float)(w * shake * 1.5);
        p.armHold += (float)(w * Math.max(hunch, torn));
        p.legHold += (float)(w * hunch);
    }

    /** 1.0.28: Black Reaper's poses: sending them out, hauling it in, the blade through it. */
    static RigPose reaperOut() {
        RigPose o = new RigPose();
        o.leftArmX = -1.45F; o.leftArmY = -0.05F; o.leftElbow = 0.15F; o.twist = -0.3F; o.lean = -0.08F; o.drop = 0.6F; o.headX = -0.1F;
        o.rightArmX = 0.45F; o.rightArmZ = 0.35F; o.rightElbow = 0.7F;
        return o;
    }

    static RigPose reaperDrag() {
        RigPose d = new RigPose();
        d.leftArmX = -1.0F; d.leftArmY = 0.15F; d.leftElbow = 1.25F; d.twist = -0.45F; d.lean = -0.28F; d.drop = 0.9F; d.headX = 0.1F;
        d.rightArmX = 0.75F; d.rightArmZ = 0.3F; d.rightElbow = 1.05F;
        return d;
    }

    static RigPose reaperStab() {
        RigPose t = new RigPose();
        t.rightArmX = -1.55F; t.rightArmY = 0.1F; t.rightElbow = 0.0F; t.twist = 0.5F; t.lean = 0.35F; t.lunge = 1.6F; t.drop = 0.6F;
        t.leftArmX = 0.25F; t.leftArmZ = -0.35F; t.leftElbow = 0.5F; t.headX = 0.2F;
        return t;
    }

    /**
     * 1.0.28: Black Reaper, {@code phase} ({@code WarRules.REAPER_*}) {@code age} ticks in, coming out of {@code from} (the phase before,
     * -1: none): the free arm thrown out after the centipedes, the blade drawn back by the hip; hauling, leant back, the haul in
     * heaves; the blade driven through what was dragged in, then wrenched; and back to himself out of whichever it was.
     */
    public static void reaper(RigPose p, int phase, double age, int from, double w) {
        if (w <= 0.0) {
            return;
        }
        int out = com.hex.kagune.control.king.WarRules.REAPER_OUT;
        int drag = com.hex.kagune.control.king.WarRules.REAPER_DRAG;
        int through = com.hex.kagune.control.king.WarRules.REAPER_THROUGH;
        if (phase == out) {
            p.add(reaperOut(), (float)(w * beat(age, 0.0, com.hex.kagune.control.king.WarRules.REAPER_REACH * 0.7)));
        } else if (phase == drag) {
            double t = beat(age, 0.0, 4.0);
            p.add(reaperOut(), (float)(w * (1.0 - t)));
            p.add(reaperDrag(), (float)(w * t));
            // The haul, in heaves.
            double heave = Math.max(0.0, Math.sin(age * 0.75)) * t;
            p.leftElbow += (float)(w * 0.35 * heave);
            p.lean -= (float)(w * 0.08 * heave);
            p.leftArmX += (float)(w * 0.2 * heave);
        } else if (phase == through) {
            double t = beat(age, 0.0, 2.0);
            p.add(reaperDrag(), (float)(w * (1.0 - t)));
            p.add(reaperStab(), (float)(w * t));
            // Wrenched in it at the end.
            double wrench = beat(age, com.hex.kagune.control.king.WarRules.REAPER_IMPALE - 4.0, com.hex.kagune.control.king.WarRules.REAPER_IMPALE);
            p.twist += (float)(w * 0.3 * wrench);
            p.rightArmY -= (float)(w * 0.45 * wrench);
            p.rightArmZ += (float)(w * 0.35 * wrench);
        } else {
            RigPose last = from == out ? reaperOut() : from == through ? reaperStab() : reaperDrag();
            p.add(last, (float)(w * (1.0 - beat(age, 0.0, com.hex.kagune.control.king.WarRules.REAPER_RETRACT))));
        }
        p.armHold += (float)w;
    }

    /**
     * 1.0.28: King's Defiance: crouched, the bladed arm folded up over his face, the free one across his body, breathing slow inside the
     * cocoon; then (level 1) thrown open with it and up out of it.
     */
    public static void defy(RigPose p, int level, double age, double w) {
        if (w <= 0.0) {
            return;
        }
        RigPose cover = new RigPose();
        cover.rightArmX = -2.0F; cover.rightArmY = 0.65F; cover.rightArmZ = -0.1F; cover.rightElbow = 1.65F; cover.headX = 0.3F; cover.lean = 0.28F;
        cover.drop = 1.8F; cover.leftArmX = -0.55F; cover.leftArmY = 0.45F; cover.leftElbow = 1.3F; cover.rightKnee = 1.0F; cover.leftKnee = 1.0F;
        cover.rightLegX = -0.45F; cover.leftLegX = -0.45F;
        if (level == com.hex.kagune.control.king.WarRules.DEFY_OPENING) {
            double open = com.hex.kagune.control.king.WarRules.DEFY_OPEN;
            double t = beat(age, 0.0, open * 0.45);
            double back = beat(age, open * 0.45, open);
            RigPose flung = new RigPose();
            flung.rightArmZ = 1.1F; flung.leftArmZ = -1.1F; flung.rightArmX = -0.5F; flung.leftArmX = -0.5F; flung.lean = -0.25F; flung.headX = -0.35F;
            flung.drop = 0.3F;
            p.add(cover, (float)(w * (1.0 - t)));
            p.add(flung, (float)(w * t * (1.0 - back)));
            p.armHold += (float)(w * (1.0 - back));
            p.legHold += (float)(w * (1.0 - t));
            return;
        }
        double t = beat(age, 0.0, com.hex.kagune.control.king.WarRules.DEFY_CLOSE * 0.8);
        p.add(cover, (float)(w * t));
        p.lean += (float)(w * t * 0.03 * Math.sin(age * 0.16));
        p.headX += (float)(w * t * 0.03 * Math.sin(age * 0.16 + 0.8));
        p.armHold += (float)(w * t);
        p.legHold += (float)(w * t);
    }

    /**
     * 1.0.28: the Spire Wave held: the free arm pointed straight out where he looks, the shoulder behind it, the blade drawn back; the
     * longer it is held the lower he braces and the harder the arm shakes with what is winding round it.
     */
    public static void spireAim(RigPose p, double age, double w) {
        if (w <= 0.0) {
            return;
        }
        double c = com.hex.kagune.control.king.WarRules.spireCharge(age);
        double t = beat(age, 0.0, 5.0);
        RigPose aim = new RigPose();
        aim.leftArmX = -1.57F; aim.leftArmY = -0.12F; aim.leftElbow = 0.05F; aim.twist = -0.45F; aim.lean = 0.12F; aim.headX = 0.05F;
        aim.rightArmX = 0.45F; aim.rightArmZ = 0.45F; aim.rightElbow = 0.85F;
        p.add(aim, (float)(w * t));
        p.drop += (float)(w * t * (0.5 + 0.6 * c));
        double shake = 0.012 + 0.05 * c;
        p.leftArmX += (float)(w * t * shake * Math.sin(age * 3.1));
        p.leftArmY += (float)(w * t * shake * 0.7 * Math.sin(age * 2.3 + 1.0));
        p.headX += (float)(w * t * shake * 0.5 * Math.sin(age * 2.7 + 0.4));
        p.armHold += (float)(w * t);
    }

    /**
     * 1.0.28: the Spire Wave let go: the arm thrust out after them, the weight behind it, and held out (straining) while they go; a clench
     * of the fist as they crush ({@code arrive}); eased back as they come back, to himself by {@code total}.
     */
    public static void spireSend(RigPose p, double age, double arrive, double total, double w) {
        if (w <= 0.0) {
            return;
        }
        RigPose aim = new RigPose();
        aim.leftArmX = -1.57F; aim.leftArmY = -0.12F; aim.twist = -0.45F; aim.lean = 0.12F; aim.drop = 0.8F; aim.rightArmX = 0.45F; aim.rightArmZ = 0.45F;
        aim.rightElbow = 0.85F;
        RigPose thrust = new RigPose();
        thrust.leftArmX = -1.68F; thrust.leftArmY = 0.05F; thrust.twist = -0.65F; thrust.lean = 0.32F; thrust.lunge = 1.5F; thrust.drop = 0.9F; thrust.headX = 0.1F;
        thrust.rightArmX = 0.7F; thrust.rightArmZ = 0.55F; thrust.rightElbow = 0.9F;
        double in = beat(age, 0.0, 2.0);
        double back = beat(age, arrive + com.hex.kagune.control.king.WarRules.SPIRE_CRUSH, total);
        p.add(aim, (float)(w * (1.0 - in) * (1.0 - back)));
        p.add(thrust, (float)(w * in * (1.0 - back)));
        double strain = in * (1.0 - beat(age, arrive, arrive + 2.0));
        p.leftArmX += (float)(w * strain * 0.03 * Math.sin(age * 2.9));
        double clench = Math.max(0.0, 1.0 - Math.abs(age - arrive - 1.0) / 3.0) * (1.0 - back);
        p.leftElbow += (float)(w * 0.55 * clench);
        p.lean -= (float)(w * 0.12 * clench);
        p.armHold += (float)(w * (1.0 - back));
    }

    /** 1.0.14: ticks over which a blow cut off by the next one is let go while the next winds up. */
    public static final double HANDOFF = 3.0;

    /**
     * 1.0.14: a blow of his M1 {@code age} ticks in, while the one before it ({@code wasLevel}, cut off {@code cutAge} ticks into it; -1
     * for none) is let go over {@link #HANDOFF}, so a chain flows blow into blow instead of snapping back to the stance between them.
     */
    public static void strikes(RigPose p, int level, double age, int wasLevel, double cutAge, float weight) {
        if (wasLevel >= 0 && age < HANDOFF) {
            strike(p, com.hex.kagune.control.king.WarStrike.byOrdinal(wasLevel), cutAge, weight * (1.0 - smooth(clamp01(age / HANDOFF))));
        }
        strike(p, com.hex.kagune.control.king.WarStrike.byOrdinal(level), age, weight);
    }

    /**
     * 1.0.14: one of War's M1 blows on the body ({@code WarStrike}), {@code age} ticks in: wound up into its coil, snapped through to
     * the blow on the impact tick, carried into its follow-through, then let go. The bladed arm is the right; the free hand the left.
     */
    static void strike(RigPose p, com.hex.kagune.control.king.WarStrike blow, double age, double w) {
        if (w <= 0.0) {
            return;
        }
        RigPose coil = new RigPose();
        RigPose hit = new RigPose();
        RigPose follow = new RigPose();
        double spin = 0.0;
        switch (blow) {
            case JAB -> {
                // The left shoulder back, the fist folded at the chin; then out straight, the weight behind it.
                coil.leftArmX = -0.55F; coil.leftElbow = 1.4F; coil.twist = 0.18F; coil.drop = 0.4F; coil.lean = 0.05F;
                coil.rightArmX = -0.35F; coil.rightElbow = 0.7F;
                hit.leftArmX = -1.62F; hit.leftArmY = 0.1F; hit.twist = -0.38F; hit.lean = 0.2F; hit.lunge = 1.6F; hit.drop = 0.5F;
                hit.rightArmX = -0.4F; hit.rightElbow = 0.9F; hit.rightArmZ = 0.2F;
                follow.add(hit, 0.85F);
            }
            case REAP -> {
                // The blade drawn out wide on his right, the chest turned with it; swept flat across in front; through to his left.
                coil.rightArmX = -1.3F; coil.rightArmY = -1.0F; coil.rightArmZ = 0.35F; coil.rightElbow = 0.3F; coil.twist = -0.55F;
                coil.lean = 0.05F; coil.drop = 0.5F; coil.leftArmX = -0.4F; coil.leftElbow = 0.6F;
                hit.rightArmX = -1.5F; hit.rightArmY = 0.05F; hit.twist = 0.1F; hit.lean = 0.15F; hit.lunge = 1.4F; hit.drop = 0.6F;
                hit.leftArmZ = -0.3F;
                follow.rightArmX = -1.35F; follow.rightArmY = 1.0F; follow.rightArmZ = -0.25F; follow.twist = 0.6F; follow.lean = 0.2F;
                follow.lunge = 1.2F; follow.drop = 0.5F; follow.leftArmZ = -0.5F;
            }
            case SIDE_KICK -> {
                // Turned side-on, the right knee chambered high; the foot driven straight out flat, the body tipped away from it.
                coil.spin = 0.9F; coil.headY = -0.9F; coil.rightLegX = -0.95F; coil.rightKnee = 1.7F; coil.roll = 0.12F; coil.drop = 1.0F;
                coil.leftArmX = -0.6F; coil.leftElbow = 0.9F; coil.rightArmX = -0.4F; coil.rightElbow = 0.8F;
                hit.spin = 1.25F; hit.headY = -1.2F; hit.rightLegZ = 1.1F; hit.rightLegX = -0.15F; hit.roll = 0.45F; hit.drop = 0.6F;
                hit.rightArmZ = 0.5F; hit.leftArmX = -0.9F; hit.leftElbow = 0.9F; hit.lean = -0.1F;
                follow.spin = 1.05F; follow.headY = -1.0F; follow.rightLegX = -0.8F; follow.rightKnee = 1.5F; follow.roll = 0.15F;
                follow.drop = 0.8F; follow.leftArmX = -0.6F; follow.leftElbow = 0.9F;
            }
            case RISING -> {
                // Crouched, the blade low across on his left; ripped up through them; high over his right, rising with it.
                coil.rightArmX = 0.2F; coil.rightArmY = 0.9F; coil.rightArmZ = -0.2F; coil.twist = 0.5F; coil.drop = 1.4F; coil.lean = 0.35F;
                coil.headX = 0.1F; coil.leftArmX = -0.3F;
                hit.rightArmX = -1.6F; hit.twist = 0.0F; hit.drop = 0.4F; hit.lean = 0.05F; hit.lunge = 1.0F;
                follow.rightArmX = -2.8F; follow.rightArmY = -0.5F; follow.rightArmZ = 0.5F; follow.twist = -0.5F; follow.lean = -0.2F;
                follow.drop = -0.4F; follow.headX = -0.3F; follow.leftArmZ = -0.5F;
            }
            case HEEL -> {
                // Low; round on the spot, the left heel swung out level through everything round him, the body tipped away from it.
                coil.drop = 0.8F; coil.leftLegX = -0.3F; coil.leftKnee = 0.8F; coil.rightArmZ = 0.4F; coil.leftArmZ = -0.4F;
                hit.leftLegZ = -1.2F; hit.leftLegX = 0.2F; hit.leftKnee = 0.1F; hit.roll = -0.4F; hit.drop = 0.5F; hit.rightArmZ = 0.9F;
                hit.leftArmZ = -0.5F;
                follow.leftLegZ = -0.4F; follow.roll = -0.1F; follow.drop = 0.6F; follow.rightArmZ = 0.5F;
                // The turn: once round, to his right, landing past half way; a full turn is no turn, so it is kept at the end.
                spin = -Math.PI * 2.0 * smooth(clamp01(age / (blow.impact + 3.0)));
            }
            case ASCENSION -> {
                // 1.0.20: crouched low, the blade down and back on his right; hooked in under them and torn up high, rising off his feet
                // with it.
                coil.rightArmX = 0.5F; coil.rightArmZ = 0.3F; coil.drop = 2.0F; coil.lean = 0.45F; coil.twist = -0.4F; coil.leftArmX = -0.5F;
                coil.headX = 0.1F;
                hit.rightArmX = -2.4F; hit.rightArmZ = 0.1F; hit.drop = -0.6F; hit.lean = -0.15F; hit.twist = 0.2F; hit.lunge = 1.2F;
                hit.leftArmZ = -0.6F; hit.headX = -0.35F;
                follow.rightArmX = -3.0F; follow.drop = -1.2F; follow.lean = -0.25F; follow.headX = -0.5F; follow.leftArmZ = -0.8F;
                follow.rightKnee = 1.2F; follow.leftKnee = 0.9F;
            }
            case DOWNFALL -> {
                // 1.0.20: in the air, the knees drawn up, the blade raised over his head; brought down on them, folding over it.
                coil.rightArmX = -3.0F; coil.rightArmZ = 0.2F; coil.lean = -0.35F; coil.twist = -0.3F; coil.headX = -0.2F; coil.rightKnee = 1.0F;
                coil.leftKnee = 1.3F; coil.leftArmZ = -0.6F;
                hit.rightArmX = -0.1F; hit.lean = 0.75F; hit.twist = 0.4F; hit.headX = 0.4F; hit.lunge = 1.0F; hit.leftArmX = 0.3F;
                hit.leftArmZ = -0.9F; hit.rightKnee = 0.6F; hit.leftKnee = 0.6F;
                follow.add(hit, 1.0F);
                follow.lean = 0.6F;
            }
            case CLEAVER -> {
                // 1.0.20: the free arm out before him, the blade raised high behind his head, the chest turned with it; brought down with
                // all his weight, crouched deep in it.
                coil.rightArmX = -3.1F; coil.rightArmY = -0.2F; coil.rightArmZ = 0.3F; coil.leftArmX = -0.9F; coil.leftArmZ = -0.4F; coil.lean = -0.35F;
                coil.drop = -0.4F; coil.twist = -0.55F; coil.headX = -0.25F;
                hit.rightArmX = -0.4F; hit.rightArmZ = -0.1F; hit.lean = 0.75F; hit.drop = 2.4F; hit.twist = 0.55F; hit.leftArmZ = -1.0F;
                hit.leftArmX = 0.25F; hit.headX = 0.35F; hit.lunge = 2.2F;
                follow.add(hit, 1.0F);
                follow.rightArmX = -0.25F;
                follow.lean = 0.7F;
            }
            case CALAMITY -> {
                // 1.0.20: up on his toes, both arms over his head; brought down together into the ground before him, low over it.
                coil.rightArmX = -3.1F; coil.leftArmX = -3.0F; coil.rightArmZ = 0.4F; coil.leftArmZ = -0.4F; coil.lean = -0.45F; coil.drop = -1.4F;
                coil.headX = -0.5F;
                hit.rightArmX = -0.2F; hit.leftArmX = -0.3F; hit.rightArmZ = 0.1F; hit.leftArmZ = -0.1F; hit.lean = 0.95F; hit.drop = 3.4F;
                hit.headX = 0.4F; hit.lunge = 1.4F;
                follow.add(hit, 1.0F);
                follow.lean = 0.85F;
            }
            case REND -> {
                // 1.0.20: turned, the free hand drawn back; thrust out flat into them as the rinkaku drive through; swept out wide,
                // flinging them away.
                coil.leftArmX = -0.4F; coil.leftElbow = 1.4F; coil.twist = 0.5F; coil.drop = 0.9F; coil.lean = 0.1F; coil.rightArmX = 0.3F;
                coil.rightArmZ = 0.5F;
                hit.leftArmX = -1.65F; hit.leftArmY = 0.2F; hit.twist = -0.45F; hit.lean = 0.35F; hit.lunge = 2.2F; hit.drop = 0.8F;
                hit.rightArmZ = 0.6F; hit.rightArmX = 0.2F;
                follow.leftArmX = -1.2F; follow.leftArmZ = -0.6F; follow.twist = -0.8F; follow.lean = 0.25F; follow.drop = 0.6F;
                follow.rightArmZ = 0.7F;
            }
            case VERDICT -> {
                // Rising tall, arched back, the blade high over his head; brought down on them with all his weight, crouched low in it.
                coil.rightArmX = -3.0F; coil.rightArmZ = 0.15F; coil.leftArmZ = -0.8F; coil.leftArmX = -0.5F; coil.lean = -0.3F;
                coil.drop = -1.0F; coil.twist = -0.4F; coil.headX = -0.3F;
                hit.rightArmX = -0.3F; hit.twist = 0.5F; hit.lean = 0.7F; hit.drop = 2.8F; hit.leftArmZ = -0.9F; hit.leftArmX = 0.2F;
                hit.headX = 0.3F; hit.lunge = 2.0F;
                follow.add(hit, 1.0F);
                follow.rightArmX = -0.2F;
                follow.lean = 0.65F;
            }
        }
        double impact = blow.impact;
        double c = smooth(clamp01(age / Math.max(1.0, impact - 1.5)));
        double h = smooth(clamp01((age - (impact - 1.5)) / 1.5));
        double f = smooth(clamp01((age - impact) / 2.5));
        double o = smooth(clamp01((age - impact - 2.5) / Math.max(1.0, blow.duration - impact - 2.5)));
        p.add(coil, (float)(w * c * (1.0 - h) * (1.0 - o)));
        p.add(hit, (float)(w * h * (1.0 - f) * (1.0 - o)));
        p.add(follow, (float)(w * h * f * (1.0 - o)));
        // Let go part way (handed off to the next blow), a turn is the turn it is short of a whole one, never unwound backwards.
        p.spin += (float)(w * (w < 0.999 ? Math.IEEEremainder(spin, Math.PI * 2.0) : spin));
        // The limbs are the blow's: no walking swing through it.
        float held = (float)(w * (1.0 - o));
        p.armHold += held;
        p.legHold += held;
    }

    private static double clamp01(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }

    /**
     * 1.0.8: Peace's body: serene and upright, the chin level, the arms a little open with the palms down, breathing slow, the head
     * turning gently now and then. Nothing twitches: that is War's.
     */
    public static void peace(RigPose p, double time, float weight) {
        if (weight <= 0.0F) {
            return;
        }
        double w = weight;
        double t = time / 20.0;
        double breathe = Math.sin(t * 0.8) * 0.5 + 0.5;
        // 1.0.18: his own stance back (1.0.16's angelic one gone): upright, the arms a little open, the head level and calm.
        p.lean -= (float)(w * (0.04 + 0.02 * breathe));
        p.rightArmZ += (float)(w * (0.22 + 0.03 * breathe));
        p.leftArmZ -= (float)(w * (0.22 + 0.03 * breathe));
        p.rightArmX -= (float)(w * 0.06);
        p.leftArmX -= (float)(w * 0.06);
        p.headY += (float)(w * 0.12 * Math.sin(t * 0.21));
        p.headX -= (float)(w * 0.04);
    }

    /**
     * 1.0.17: War coming over him (KingTransform), {age, lean, headX, headY, headZ, rightArmX, rightArmZ, leftArmX, leftArmZ, rightElbow,
     * leftElbow, twist, legs apart, drop}: clutching his chest as the darkness pours in; doubled over; wrenched back; coiled tight;
     * thrown open at the burst, roaring up; held; let go into his stance.
     */
    static final double[][] WAR_COMING = {
        {0.0, 0.25, 0.4, 0.0, 0.0, -1.1, -0.35, -1.1, 0.35, 1.4, 1.4, 0.0, 0.1, 0.0},
        {6.0, 0.55, 0.7, 0.0, 0.1, -0.6, -0.2, -0.7, 0.25, 1.6, 1.5, 0.15, 0.18, 1.0},
        {11.0, -0.2, -0.5, 0.15, 0.25, 0.2, 0.5, 0.25, -0.5, 0.6, 0.6, -0.2, 0.2, 0.0},
        {14.5, 0.45, 0.6, 0.0, 0.0, -1.3, -0.6, -1.3, 0.6, 1.7, 1.7, 0.0, 0.22, 1.5},
        {16.0, -0.45, -0.8, 0.0, 0.0, -0.4, 1.5, -0.4, -1.5, 0.2, 0.2, 0.0, 0.3, -0.5},
        {22.0, -0.35, -0.7, 0.0, 0.1, -0.3, 1.3, -0.3, -1.3, 0.25, 0.25, 0.0, 0.3, 0.0},
        {40.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    };

    /**
     * 1.0.17: Peace coming over him: head bowed, still; rising, weightless, the arms opening; spread wide and high, his face to the sky
     * as the wings unfold; the arms coming down, still open; into his angelic stance.
     */
    static final double[][] PEACE_COMING = {
        {0.0, -0.05, 0.3, 0.0, 0.0, 0.0, 0.05, 0.0, -0.05, 0.0, 0.0, 0.0, 0.0, 0.0},
        {10.0, -0.08, -0.1, 0.0, 0.0, -0.3, 0.6, -0.3, -0.6, 0.15, 0.15, 0.0, 0.04, -4.0},
        {24.0, -0.15, -0.45, 0.0, 0.0, -0.5, 1.25, -0.5, -1.25, 0.1, 0.1, 0.0, 0.06, -6.0},
        {38.0, -0.1, -0.2, 0.0, 0.0, -0.3, 0.8, -0.3, -0.8, 0.15, 0.15, 0.0, 0.04, -4.0},
        {50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0},
        {56.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    };

    /**
     * 1.0.19: War leaving him, {age, lean, headX, headY, headZ, rightArmX, rightArmZ, leftArmX, leftArmZ, rightElbow, leftElbow, twist,
     * legs apart, drop}: a shudder through him as it dies, doubled over clutching his chest; hunched, heaving, as it crumbles off him;
     * straightening, spent, the arms hanging; himself again.
     */
    static final double[][] WAR_GOING = {
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {3.0, 0.42, 0.55, 0.0, 0.1, -0.9, -0.3, -1.0, 0.3, 1.5, 1.4, 0.1, 0.16, 1.2},
        {12.0, 0.36, 0.45, -0.1, 0.0, -0.4, -0.15, -0.5, 0.2, 0.9, 0.8, -0.05, 0.14, 0.9},
        {22.0, 0.18, 0.3, 0.0, 0.0, 0.05, 0.08, 0.0, -0.08, 0.25, 0.2, 0.0, 0.08, 0.4},
        {32.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    };

    /**
     * 1.0.19: Peace leaving him: his arms opening a little as the blades fold, the head bowed as it is drawn in, a breath out at the
     * last light, himself again.
     */
    static final double[][] PEACE_GOING = {
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {8.0, -0.05, -0.15, 0.0, 0.0, -0.2, 0.45, -0.2, -0.45, 0.1, 0.1, 0.0, 0.03, -1.5},
        {20.0, 0.08, 0.35, 0.0, 0.0, -0.1, 0.12, -0.1, -0.12, 0.25, 0.25, 0.0, 0.0, 0.0},
        {30.0, 0.04, 0.25, 0.0, 0.0, 0.0, 0.05, 0.0, -0.05, 0.1, 0.1, 0.0, 0.0, 0.0},
        {36.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    };

    /** 1.0.19: a final kakuja leaving him, {@code age} ticks after it was taken off: War's death throes and heaving, Peace's breath out. */
    public static void going(RigPose p, boolean war, double age, double time) {
        double[] k = beat(war ? WAR_GOING : PEACE_GOING, age);
        double t = time / 20.0;
        double tremor = war ? 0.12 * Math.exp(-age / 6.0) : 0.0;
        double heave = war ? 0.06 * Math.max(0.0, Math.sin(Math.PI * Math.min(1.0, age / 26.0))) * Math.sin(t * 9.0) : 0.0;
        p.lean += (float)(k[1] + tremor * Math.sin(t * 41.0) + heave);
        p.headX += (float)(k[2] + tremor * Math.sin(t * 37.0 + 1.0) + heave);
        p.headY += (float)(k[3] + tremor * 0.8 * Math.sin(t * 29.0));
        p.headZ += (float)k[4];
        p.rightArmX += (float)(k[5] + tremor * Math.sin(t * 29.0 + 0.4));
        p.rightArmZ += (float)k[6];
        p.leftArmX += (float)(k[7] + tremor * Math.sin(t * 27.0 + 2.4));
        p.leftArmZ += (float)k[8];
        p.rightElbow += (float)k[9];
        p.leftElbow += (float)k[10];
        p.twist += (float)k[11];
        p.rightLegZ += (float)k[12];
        p.leftLegZ -= (float)k[12];
        p.drop += (float)k[13];
    }

    /** Where {@code beats} have it at {@code age}: the two beats either side, eased between. */
    static double[] beat(double[][] beats, double age) {
        double[] a = beats[0];
        double[] b = beats[beats.length - 1];
        for (int i = 0; i + 1 < beats.length; i++) {
            if (age >= beats[i][0] && age <= beats[i + 1][0]) {
                a = beats[i];
                b = beats[i + 1];
                break;
            }
        }
        double f = b[0] > a[0] ? smooth((age - a[0]) / (b[0] - a[0])) : 1.0;
        double[] k = new double[a.length];
        for (int i = 1; i < a.length; i++) {
            k[i] = lerp(a[i], b[i], f);
        }
        return k;
    }

    /**
     * 1.0.17: a final kakuja coming over him, {@code age} ticks in (on top of his stance, which it gives way to at the end): War
     * shaking harder and harder toward the burst, then roaring; Peace swaying gently, weightless.
     */
    public static void coming(RigPose p, boolean war, double age, double time) {
        double[][] beats = war ? WAR_COMING : PEACE_COMING;
        double[] a = beats[0];
        double[] b = beats[beats.length - 1];
        for (int i = 0; i + 1 < beats.length; i++) {
            if (age >= beats[i][0] && age <= beats[i + 1][0]) {
                a = beats[i];
                b = beats[i + 1];
                break;
            }
        }
        double f = b[0] > a[0] ? smooth((age - a[0]) / (b[0] - a[0])) : 1.0;
        double[] k = new double[a.length];
        for (int i = 1; i < a.length; i++) {
            k[i] = lerp(a[i], b[i], f);
        }
        double t = time / 20.0;
        double burst = com.hex.kagune.control.king.KingTransform.WAR_BURST;
        double tremor = !war ? 0.0 : age < burst ? 0.03 + 0.14 * age / burst : 0.17 * Math.exp(-(age - burst) / 5.0);
        double sway = war ? 0.0 : 0.04 * Math.sin(t * 1.7);
        p.lean += (float)(k[1] + tremor * Math.sin(t * 41.0));
        p.headX += (float)(k[2] + tremor * Math.sin(t * 37.0 + 1.0));
        p.headY += (float)(k[3] + tremor * 0.8 * Math.sin(t * 29.0) + sway);
        p.headZ += (float)(k[4] + tremor * Math.sin(t * 33.0 + 2.0));
        p.rightArmX += (float)(k[5] + tremor * 1.5 * Math.sin(t * 29.0 + 0.4) + sway);
        p.rightArmZ += (float)(k[6] + tremor * Math.sin(t * 31.0 + 1.4));
        p.leftArmX += (float)(k[7] + tremor * 1.5 * Math.sin(t * 27.0 + 2.4) - sway);
        p.leftArmZ += (float)(k[8] - tremor * Math.sin(t * 35.0 + 0.9));
        p.rightElbow += (float)k[9];
        p.leftElbow += (float)k[10];
        p.twist += (float)(k[11] + tremor * 0.6 * Math.sin(t * 23.0));
        p.rightLegZ += (float)k[12];
        p.leftLegZ -= (float)k[12];
        p.drop += (float)k[13];
    }

    /**
     * 1.0.11: the turning's beats, {p, lean, headX, headY, headZ, rightArmX, rightArmZ, leftArmX, leftArmZ, rightElbow, leftElbow, twist,
     * legs apart}: clutching his chest; flung back screaming as it tears out of him; doubled over; clawing at his head as it tears out
     * of that; thrown about; clawing; the last scream, arched, arms up; curled in on himself; bursting open.
     */
    static final double[][] TURNING = {
        {0.00, 0.06, 0.15, 0.0, 0.0, -0.2, 0.05, -0.2, -0.05, 0.2, 0.2, 0.0, 0.08},
        {0.08, 0.30, 0.60, 0.0, 0.0, -1.25, -0.42, -1.25, 0.42, 1.35, 1.35, 0.0, 0.12},
        {0.13, -0.42, -0.90, 0.0, 0.0, 0.55, 1.25, 0.55, -1.25, 0.2, 0.2, 0.0, 0.2},
        {0.27, -0.34, -0.72, 0.1, 0.0, 0.4, 1.1, 0.45, -1.1, 0.25, 0.25, 0.08, 0.2},
        {0.34, 0.45, 0.65, 0.0, 0.1, -0.35, 0.22, -0.3, -0.22, 0.45, 0.45, 0.0, 0.16},
        {0.40, -0.15, -0.45, 0.0, 0.35, -2.7, 0.55, -2.7, -0.55, 1.6, 1.6, 0.0, 0.2},
        {0.53, -0.05, -0.2, -0.15, -0.3, -2.5, 0.6, -2.6, -0.5, 1.5, 1.6, 0.25, 0.2},
        {0.63, -0.22, -0.3, 0.2, 0.15, -0.4, 1.5, 0.3, -1.2, 0.3, 0.5, -0.3, 0.24},
        {0.74, 0.50, 0.2, 0.0, -0.1, -1.4, 0.5, -1.6, -0.4, 0.8, 0.9, 0.12, 0.2},
        {0.83, -0.55, -1.0, 0.0, 0.0, -2.2, 1.4, -2.2, -1.4, 0.15, 0.15, 0.0, 0.26},
        {0.92, 0.75, 0.9, 0.0, 0.0, -0.9, -0.5, -0.9, 0.5, 1.8, 1.8, 0.0, 0.26},
        {1.00, -0.30, -0.6, 0.0, 0.0, -1.0, 1.6, -1.0, -1.6, 0.2, 0.2, 0.0, 0.3},
    };

    /**
     * 1.0.11: Kaneki turning into the Dragon ({@code TurningModel}), {@code progress} (0..1) into it at {@code time} (ticks): from beat to
     * beat of {@link #TURNING}, with a tremor that builds as it takes him, his head shaking as he screams and jolting with every burst.
     */
    public static void turning(RigPose p, double progress, double time, double scream, double burst) {
        double[] a = TURNING[0];
        double[] b = TURNING[TURNING.length - 1];
        for (int i = 0; i + 1 < TURNING.length; i++) {
            if (progress >= TURNING[i][0] && progress <= TURNING[i + 1][0]) {
                a = TURNING[i];
                b = TURNING[i + 1];
                break;
            }
        }
        double f = b[0] > a[0] ? smooth((progress - a[0]) / (b[0] - a[0])) : 1.0;
        double[] k = new double[a.length];
        for (int i = 1; i < a.length; i++) {
            k[i] = lerp(a[i], b[i], f);
        }
        double t = time / 20.0;
        double tremor = 0.02 + 0.06 * progress + 0.1 * scream + 0.2 * burst;
        p.lean += (float)(k[1] + tremor * Math.sin(t * 41.0));
        p.headX += (float)(k[2] + tremor * Math.sin(t * 37.0 + 1.0));
        p.headY += (float)(k[3] + 0.12 * scream * Math.sin(t * 26.0));
        p.headZ += (float)(k[4] + tremor * Math.sin(t * 33.0 + 2.0));
        p.rightArmX += (float)(k[5] + tremor * 1.5 * Math.sin(t * 29.0 + 0.4));
        p.rightArmZ += (float)(k[6] + tremor * Math.sin(t * 31.0 + 1.4));
        p.leftArmX += (float)(k[7] + tremor * 1.5 * Math.sin(t * 27.0 + 2.4));
        p.leftArmZ += (float)(k[8] - tremor * Math.sin(t * 35.0 + 0.9));
        p.rightElbow += (float)k[9];
        p.leftElbow += (float)k[10];
        p.twist += (float)(k[11] + tremor * 0.6 * Math.sin(t * 23.0));
        p.rightLegZ += (float)k[12];
        p.leftLegZ -= (float)k[12];
    }

    /** The head's roll in slot {@code slot}: mostly far over, to either side, now and then nearly upright. */
    static double roll(int seed, long slot) {
        double h = hash(seed, slot, 4);
        double magnitude = h < 0.15 ? 0.1 : 0.55 + 0.45 * hash(seed, slot, 5);
        return (hash(seed, slot, 6) < 0.5 ? -1.0 : 1.0) * magnitude * TILT_ROLL;
    }

    static double turn(int seed, long slot) {
        return (hash(seed, slot, 7) - 0.5) * 2.0 * TILT_TURN;
    }

    static double hash(int seed, long n, int channel) {
        long x = n * 0x9E3779B97F4A7C15L + seed * 0xC2B2AE3D27D4EB4FL + channel * 0x165667B19E3779F9L;
        x ^= x >>> 33;
        x *= 0xFF51AFD7ED558CCDL;
        x ^= x >>> 33;
        x *= 0xC4CEB9FE1A85EC53L;
        x ^= x >>> 33;
        return (x >>> 11) * 0x1.0p-53;
    }

    static double smooth(double x) {
        return x * x * (3.0 - 2.0 * x);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
