package com.hex.kagune.control.ayato;

import com.hex.kagune.control.StrikePattern;

/**
 * Body poses for Ayato's close-range combo (pure, tested), added on top of the shared kagune body layer. Ayato fights
 * light and upright, on his toes (docs/AYATO_PATH.md, "Body language"): kicks are high roundhouses with the arms up in
 * guard, wing blades pull the body around after the cut, the shard burst throws the chest open and recoils, and the
 * One-Winged Kagune sweeps turn the whole torso, the beheading cut rising high before it drives through.
 *
 * <p>Angles are radians in vanilla PlayerModel signs (arm and leg x- swing forward, right limb z+ moves outward,
 * torso y+ turns the chest right). Every channel is zero at the start and end of the strike and eases through three
 * keys: wind-up, the impact tick (full extension) and the follow-through.
 */
public final class AyatoBody {
    /** One frame of offsets. Arm "across" is converted to vanilla yaw per side; "out" to vanilla roll per side. */
    public static final class Pose {
        public float twist;
        public float headX;
        public float headY;
        public float torsoX;
        public float torsoBend;
        public float rightArmX;
        public float rightArmY;
        public float rightArmZ;
        public float rightArmBend;
        public float leftArmX;
        public float leftArmY;
        public float leftArmZ;
        public float leftArmBend;
        public float rightLegX;
        public float rightLegZ;
        public float leftLegX;
        public float leftLegZ;
        /** Knee flexion (+ folds the shin back), for the hop's and the flip's tuck. */
        public float rightKnee;
        public float leftKnee;
        /** Whole-body backflip, radians (0..2 pi), about the hips. */
        public float flip;

        public float[] channels() {
            return new float[] {this.twist, this.headX, this.headY, this.torsoX, this.torsoBend, this.rightArmX, this.rightArmY,
                this.rightArmZ, this.rightArmBend, this.leftArmX, this.leftArmY, this.leftArmZ, this.leftArmBend, this.rightLegX,
                this.rightLegZ, this.leftLegX, this.leftLegZ, this.rightKnee, this.leftKnee, this.flip};
        }
    }

    /** Largest leg swing used (the high kick); the body layer widens its leg limits to this for Ayato strikes. */
    public static final float MAX_LEG_X = 1.5F;
    public static final float MAX_LEG_Z = 0.6F;

    private AyatoBody() {
    }

    /** True for the patterns this class poses. */
    public static boolean poses(StrikePattern pattern) {
        return pattern.compareTo(StrikePattern.KICK) >= 0;
    }

    /**
     * The pose at {@code progress} (0..1 through the strike) with the impact at {@code impact}. {@code rightMain} says
     * which side strikes (the kicking leg, the cutting wing; the :re One-Winged Kagune is on the left); {@code both}
     * is set when both wings strike together (cross cut, twin shard burst).
     */
    public static Pose pose(StrikePattern pattern, boolean rightMain, boolean both, float progress, float impact) {
        Keys keys = new Keys(progress, impact);
        Side side = new Side(rightMain ? 1.0F : -1.0F);
        Pose pose = new Pose();
        switch (bodyOf(pattern)) {
            case KICK -> {
                pose.twist = side.mirror(keys.at(0.15F, -0.3F, -0.28F));
                pose.headY = side.mirror(keys.at(-0.12F, 0.24F, 0.22F));
                pose.torsoX = keys.at(-0.06F, -0.2F, -0.18F);
                pose.headX = keys.at(0.0F, 0.08F, 0.06F);
                side.leg(pose, true, keys.at(-0.6F, -1.45F, -1.35F), keys.at(0.15F, 0.55F, 0.5F));
                side.leg(pose, false, keys.at(0.05F, 0.22F, 0.2F), keys.at(0.0F, 0.05F, 0.05F));
                // Kicking-side arm drops back for balance, the other stays up in guard.
                side.arm(pose, true, keys.at(-0.55F, 0.4F, 0.35F), keys.at(0.2F, -0.1F, -0.1F), keys.at(0.1F, 0.45F, 0.4F),
                    keys.at(0.7F, 0.1F, 0.1F));
                side.arm(pose, false, keys.at(-0.6F, -0.95F, -0.9F), keys.at(0.25F, 0.35F, 0.3F), keys.at(0.0F, -0.05F, -0.05F),
                    keys.at(0.7F, 0.9F, 0.85F));
            }
            case WING_SLASH -> sweep(pose, keys, side, 1.0F);
            case ONE_WING_SLASH -> sweep(pose, keys, side, 1.35F);
            case WING_CROSS -> {
                pose.torsoX = keys.at(0.22F, -0.12F, -0.1F);
                pose.torsoBend = keys.at(0.3F, 0.0F, 0.0F);
                pose.headX = keys.at(0.12F, -0.08F, -0.06F);
                // Arms fold across the chest, then fling outward as the blades cross.
                for (boolean main : new boolean[] {true, false}) {
                    side.arm(pose, main, keys.at(-1.0F, -0.45F, -0.4F), keys.at(0.6F, -0.35F, -0.3F), keys.at(-0.1F, 0.9F, 0.85F),
                        keys.at(0.8F, 0.05F, 0.05F));
                    side.leg(pose, main, keys.at(0.0F, main ? 0.12F : -0.12F, main ? 0.1F : -0.1F), keys.at(0.05F, 0.15F, 0.15F));
                }
            }
            case SHARD_BURST -> {
                // Chest thrown open as the wings fire, then the recoil.
                pose.torsoX = keys.at(-0.15F, -0.28F, -0.22F);
                pose.headX = keys.at(-0.1F, -0.15F, -0.1F);
                side.arm(pose, true, keys.at(0.35F, 0.55F, 0.5F), keys.at(-0.1F, -0.25F, -0.2F), keys.at(0.35F, 0.65F, 0.6F),
                    keys.at(0.1F, 0.2F, 0.2F));
                if (both) {
                    side.arm(pose, false, keys.at(0.35F, 0.55F, 0.5F), keys.at(-0.1F, -0.25F, -0.2F), keys.at(0.35F, 0.65F, 0.6F),
                        keys.at(0.1F, 0.2F, 0.2F));
                } else {
                    // One wing (Rabbit Guise): the free hand points the shards at the target.
                    pose.twist = side.mirror(keys.at(0.15F, 0.1F, 0.1F));
                    side.arm(pose, false, keys.at(-0.2F, -0.5F, -0.45F), keys.at(0.0F, 0.1F, 0.1F), keys.at(0.0F, 0.0F, 0.0F),
                        keys.at(0.1F, 0.0F, 0.0F));
                }
                side.leg(pose, true, keys.at(0.0F, 0.2F, 0.18F), keys.at(0.0F, 0.0F, 0.0F));
                side.leg(pose, false, keys.at(0.0F, -0.15F, -0.12F), keys.at(0.0F, 0.0F, 0.0F));
            }
            case ONE_WING_RETURN -> {
                // Back from the far side to the wing's side.
                pose.twist = side.mirror(keys.at(-0.45F, 0.5F, 0.5F));
                pose.headY = side.mirror(keys.at(0.2F, -0.25F, -0.25F));
                pose.torsoX = keys.at(-0.05F, 0.2F, 0.18F);
                side.arm(pose, true, keys.at(-1.0F, -1.15F, -1.05F), keys.at(0.8F, -0.55F, -0.6F), keys.at(0.05F, 0.5F, 0.5F),
                    keys.at(0.15F, 0.0F, 0.0F));
                side.arm(pose, false, keys.at(0.25F, 0.1F, 0.1F), keys.at(0.0F, 0.0F, 0.0F), keys.at(0.4F, 0.2F, 0.2F),
                    keys.at(0.4F, 0.3F, 0.3F));
                side.leg(pose, true, keys.at(-0.2F, 0.15F, 0.12F), keys.at(0.0F, 0.0F, 0.0F));
                side.leg(pose, false, keys.at(0.15F, -0.2F, -0.18F), keys.at(0.0F, 0.0F, 0.0F));
            }
            case BEHEADING -> {
                // Rises high, then drives a level cut through at neck height with a lunge.
                pose.twist = side.mirror(keys.at(0.7F, -0.75F, -0.8F));
                pose.headY = side.mirror(keys.at(-0.25F, 0.3F, 0.3F));
                pose.torsoX = keys.at(-0.28F, 0.35F, 0.3F);
                pose.torsoBend = keys.at(0.0F, 0.3F, 0.25F);
                pose.headX = keys.at(-0.25F, 0.1F, 0.08F);
                side.arm(pose, true, keys.at(-2.2F, -1.45F, -1.3F), keys.at(-0.4F, 0.95F, 1.0F), keys.at(0.5F, 0.1F, 0.05F),
                    keys.at(0.1F, 0.0F, 0.0F));
                side.arm(pose, false, keys.at(-0.4F, 0.45F, 0.4F), keys.at(0.0F, 0.0F, 0.0F), keys.at(0.6F, 0.5F, 0.45F),
                    keys.at(0.4F, 0.3F, 0.3F));
                side.leg(pose, true, keys.at(0.25F, 0.35F, 0.3F), keys.at(0.0F, 0.0F, 0.0F));
                side.leg(pose, false, keys.at(0.0F, -0.45F, -0.4F), keys.at(0.0F, 0.0F, 0.0F));
            }
            case FLAME_SHOT -> {
                // The hand on the wing's side cocks back, then thrusts forward, palm out, as the wing spits its fire; the chest
                // turns into it and the other hand stays tucked at the ribs.
                pose.twist = side.mirror(keys.at(0.2F, -0.3F, -0.26F));
                pose.headY = side.mirror(keys.at(-0.08F, 0.14F, 0.12F));
                pose.torsoX = keys.at(-0.03F, 0.07F, 0.05F);
                side.arm(pose, true, keys.at(0.3F, -1.5F, -1.38F), keys.at(-0.2F, 0.22F, 0.24F), keys.at(0.28F, 0.04F, 0.0F),
                    keys.at(1.2F, 0.08F, 0.2F));
                side.arm(pose, false, keys.at(0.1F, 0.25F, 0.25F), keys.at(0.1F, 0.15F, 0.15F), keys.at(0.08F, 0.12F, 0.12F),
                    keys.at(0.9F, 1.0F, 0.95F));
                side.leg(pose, true, keys.at(0.0F, 0.1F, 0.08F), keys.at(0.0F, 0.0F, 0.0F));
                side.leg(pose, false, keys.at(0.0F, -0.16F, -0.14F), keys.at(0.0F, 0.0F, 0.0F));
            }
            case FLARE -> {
                // Both palms drawn back to the hips, then thrust out together as the fireball leaves.
                pose.torsoX = keys.at(-0.1F, 0.16F, 0.14F);
                pose.headX = keys.at(-0.05F, 0.05F, 0.05F);
                for (boolean main : new boolean[] {true, false}) {
                    side.arm(pose, main, keys.at(0.25F, -1.5F, -1.38F), keys.at(-0.25F, 0.3F, 0.26F), keys.at(0.3F, 0.0F, 0.0F),
                        keys.at(1.2F, 0.06F, 0.12F));
                }
                side.leg(pose, false, keys.at(0.0F, -0.28F, -0.25F), keys.at(0.0F, 0.0F, 0.0F));
                side.leg(pose, true, keys.at(0.0F, 0.14F, 0.12F), keys.at(0.0F, 0.0F, 0.0F));
            }
            case RISING_FLAME -> {
                // Crouched, hands low and back, then both arms sweep up past the head as the pillar goes up.
                pose.torsoX = keys.at(0.32F, -0.2F, -0.16F);
                pose.headX = keys.at(0.15F, -0.3F, -0.25F);
                // The arms are still rising at the impact and go on overhead after it.
                for (boolean main : new boolean[] {true, false}) {
                    side.arm(pose, main, keys.at(0.3F, -1.4F, -2.3F), keys.at(0.0F, 0.1F, 0.1F), keys.at(0.2F, 0.35F, 0.3F),
                        keys.at(0.4F, 0.2F, 0.2F));
                }
                pose.rightKnee = keys.at(0.9F, 0.1F, 0.1F);
                pose.leftKnee = keys.at(0.9F, 0.1F, 0.1F);
                side.leg(pose, true, keys.at(-0.45F, 0.0F, 0.0F), keys.at(0.15F, 0.1F, 0.1F));
                side.leg(pose, false, keys.at(-0.45F, 0.0F, 0.0F), keys.at(0.15F, 0.1F, 0.1F));
            }
            case FIRE_FAN -> {
                // One arm sweeps wide from the far side out to its own as the fan sprays.
                pose.twist = side.mirror(keys.at(-0.35F, 0.4F, 0.4F));
                pose.headY = side.mirror(keys.at(0.15F, -0.2F, -0.2F));
                side.arm(pose, true, keys.at(-1.4F, -1.45F, -1.3F), keys.at(0.9F, -0.7F, -0.75F), keys.at(0.0F, 0.2F, 0.2F),
                    keys.at(0.4F, 0.05F, 0.05F));
                side.arm(pose, false, keys.at(0.1F, 0.3F, 0.3F), keys.at(0.0F, 0.0F, 0.0F), keys.at(0.1F, 0.2F, 0.2F),
                    keys.at(0.8F, 0.8F, 0.8F));
                side.leg(pose, false, keys.at(0.0F, -0.2F, -0.18F), keys.at(0.0F, 0.0F, 0.0F));
            }
            case RABBIT_DASH -> {
                // Low and flat out: chest down, arms trailing back like the wings, legs striding.
                float run = smooth(progress / 0.12F) * (1.0F - smooth((progress - 0.72F) / 0.28F));
                float stride = (float)Math.sin(progress * Math.PI * 6.0) * run;
                pose.torsoX = 0.62F * run;
                pose.headX = -0.4F * run;
                side.arm(pose, true, 0.95F * run, 0.0F, 0.25F * run, 0.15F * run);
                side.arm(pose, false, 0.95F * run, 0.0F, 0.25F * run, 0.15F * run);
                side.leg(pose, true, -0.6F * stride, 0.0F);
                side.leg(pose, false, 0.6F * stride, 0.0F);
                pose.rightKnee = 0.6F * run * Math.max(0.0F, stride);
                pose.leftKnee = 0.6F * run * Math.max(0.0F, -stride);
            }
            case RABBIT_HOP -> {
                // A quick crouch, then thrown back: arms flung forward for balance, knees tucked.
                pose.torsoX = keys.at(0.25F, -0.15F, -0.1F);
                pose.headX = keys.at(0.1F, -0.15F, -0.1F);
                for (boolean main : new boolean[] {true, false}) {
                    side.arm(pose, main, keys.at(0.5F, -1.1F, -0.8F), keys.at(0.0F, 0.0F, 0.0F), keys.at(0.15F, 0.35F, 0.3F),
                        keys.at(0.3F, 0.4F, 0.3F));
                    side.leg(pose, main, keys.at(-0.35F, -0.7F, -0.5F), keys.at(0.05F, 0.12F, 0.1F));
                }
                pose.rightKnee = keys.at(0.8F, 1.2F, 0.9F);
                pose.leftKnee = keys.at(0.8F, 1.2F, 0.9F);
            }
            case BLACK_RABBIT -> blackRabbit(pose, side, progress);
            case RUSHING_DEATH -> rushingDeath(pose, side, progress);
            case SPIKE_BARRAGE -> {
                // Both palms out at the target and held there while the wings machine-gun, the body braced against it.
                float hold = smooth(progress / 0.15F) * (1.0F - smooth((progress - 0.85F) / 0.15F));
                float judder = 0.04F * hold * (float)Math.sin(progress * Math.PI * 40.0);
                pose.torsoX = 0.1F * hold;
                pose.headX = -0.05F * hold;
                for (boolean main : new boolean[] {true, false}) {
                    side.arm(pose, main, -1.45F * hold + judder, 0.22F * hold, 0.05F * hold, 0.1F * hold);
                }
                side.leg(pose, false, -0.25F * hold, 0.0F);
                side.leg(pose, true, 0.15F * hold, 0.0F);
            }
            case CRYSTAL_BLOOM -> {
                // Arms pulled in tight to the chest, then flung wide as every spike bursts.
                pose.torsoX = keys.at(0.2F, -0.25F, -0.2F);
                pose.headX = keys.at(0.15F, -0.2F, -0.15F);
                for (boolean main : new boolean[] {true, false}) {
                    side.arm(pose, main, keys.at(-0.9F, 0.25F, 0.2F), keys.at(0.55F, -0.2F, -0.15F), keys.at(-0.1F, 1.1F, 1.0F),
                        keys.at(1.4F, 0.1F, 0.1F));
                    side.leg(pose, main, keys.at(0.0F, main ? 0.15F : -0.15F, main ? 0.12F : -0.12F), keys.at(0.05F, 0.2F, 0.18F));
                }
            }
            default -> {
            }
        }
        return pose;
    }

    /**
     * Firing the Gatling, {@code amount} 0..1 up (it eases in and out): both palms held out at the target, the body braced
     * and juddering with the stream ({@code time} in ticks).
     */
    public static Pose gatling(float amount, double time) {
        float a = Math.max(0.0F, Math.min(1.0F, amount));
        Pose pose = new Pose();
        if (a <= 0.0F) {
            return pose;
        }
        Side side = new Side(1.0F);
        float judder = 0.035F * a * (float)Math.sin(time * 2.7);
        pose.torsoX = 0.12F * a;
        pose.headX = -0.06F * a;
        for (boolean main : new boolean[] {true, false}) {
            side.arm(pose, main, -1.45F * a + (main ? judder : -judder), 0.22F * a, 0.05F * a, 0.12F * a);
        }
        side.leg(pose, false, -0.28F * a, 0.0F);
        side.leg(pose, true, 0.18F * a, 0.0F);
        return pose;
    }

    /** How far past the wing's release (share of the throw) the arm finishes its whip. */
    public static final float VORTEX_FOLLOW = 0.06F;

    /**
     * Throwing a Wing Vortex with the wing on the {@code right} (or left) side, {@code progress} 0..1 through the throw with the
     * wing leaving at {@code release}: the chest turns away and the arm on that side draws back and up as the wing coils, then
     * whips forward and across in a spiral, palm open, the chest turning into it and the weight stepping onto the front foot;
     * a short hold, then back to rest.
     */
    public static Pose vortex(boolean right, float progress, float release) {
        // The arm's whip runs on a little past the moment the wing leaves it.
        Keys keys = new Keys(progress, release + VORTEX_FOLLOW);
        Side side = new Side(right ? 1.0F : -1.0F);
        Pose pose = new Pose();
        pose.twist = side.mirror(keys.at(0.45F, -0.5F, -0.45F));
        pose.headY = side.mirror(keys.at(-0.2F, 0.3F, 0.25F));
        pose.torsoX = keys.at(-0.12F, 0.2F, 0.16F);
        pose.headX = keys.at(-0.05F, 0.1F, 0.08F);
        side.arm(pose, true, keys.at(0.45F, -1.6F, -1.45F), keys.at(-0.35F, 0.5F, 0.45F), keys.at(0.55F, 0.1F, 0.08F), keys.at(0.7F, 0.08F, 0.12F));
        side.arm(pose, false, keys.at(-0.35F, 0.35F, 0.3F), keys.at(0.25F, -0.2F, -0.15F), keys.at(0.15F, 0.3F, 0.25F), keys.at(0.6F, 0.4F, 0.35F));
        side.leg(pose, false, keys.at(0.05F, -0.32F, -0.28F), keys.at(0.0F, 0.0F, 0.0F));
        side.leg(pose, true, keys.at(-0.05F, 0.22F, 0.2F), keys.at(0.0F, 0.0F, 0.0F));
        return pose;
    }

    /**
     * The crystal moves (and both kakuja's) share the fire's body language: the same hands thrust, sweep and rise. The final
     * kakuja's M1 shots thrust the hand on the side that fires (right for needles, left for fire).
     */
    static StrikePattern bodyOf(StrikePattern pattern) {
        return switch (pattern) {
            case CRYSTAL_SHOT, NEEDLE_VOLLEY, NEEDLE_PAIR, FIRE_LASH, NEEDLE_FAN -> StrikePattern.FLAME_SHOT;
            case HARROW, CROSSFIRE, SEARING_HARROW -> StrikePattern.SHARD_BURST;
            case PIERCING_LINE, FIREBALL, PYRE_LANCE -> StrikePattern.FLARE;
            case THORN_FAN, TWIN_SERPENTS -> StrikePattern.FIRE_FAN;
            case HAIL_OF_THORNS, HELLRAIN -> StrikePattern.RISING_FLAME;
            case SIX_TAIL_BARRAGE -> StrikePattern.SPIKE_BARRAGE;
            case CRIMSON_BLOOM, CRIMSON_PYRE -> StrikePattern.CRYSTAL_BLOOM;
            case EMBER_HOP -> StrikePattern.RABBIT_HOP;
            case CRYSTAL_LANCE -> StrikePattern.FLARE;
            case SPIKE_FAN -> StrikePattern.FIRE_FAN;
            case SPIKE_RAIN -> StrikePattern.RISING_FLAME;
            default -> pattern;
        };
    }

    /**
     * The Black Rabbit: a crouch (to 0.1), a tucked backflip through a full turn (0.1 to 0.38), the barrage hanging in the air
     * with the arms flung wide and the head down on the target (0.38 to 0.75), then dropping into a landing crouch.
     */
    /** Where in Rushing Death the needles leave: at the top of the jump, the flip just finished. */
    public static final float RUSH_THROW = 0.42F;

    /**
     * Rushing Death: a crouch (to 0.06), a tucked backflip through a full turn (0.06 to 0.38) up and back, then at the top
     * the throw (arms flung forward as the six tentacles snap forward, about {@link #RUSH_THROW}), then the fall, arms out
     * for balance, legs reaching down, and the landing.
     */
    private static void rushingDeath(Pose pose, Side side, float progress) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        float crouch = smooth(p / 0.06F) * (1.0F - smooth((p - 0.06F) / 0.08F));
        float spin = smooth((p - 0.06F) / 0.32F);
        float tuck = smooth((p - 0.06F) / 0.1F) * (1.0F - smooth((p - 0.28F) / 0.1F));
        float fling = smooth((p - 0.36F) / 0.06F) * (1.0F - smooth((p - 0.55F) / 0.12F));
        float fall = smooth((p - 0.55F) / 0.1F) * (1.0F - smooth((p - 0.86F) / 0.14F));
        // A full turn is the same as none: once he has come round, the flip reads 0 again.
        pose.flip = spin >= 1.0F ? 0.0F : (float)(Math.PI * 2.0) * spin;
        pose.torsoX = 0.3F * crouch + 0.55F * tuck + 0.12F * fling - 0.05F * fall;
        pose.headX = 0.1F * crouch + 0.3F * tuck - 0.1F * fling;
        for (boolean main : new boolean[] {true, false}) {
            side.arm(pose, main, 0.4F * crouch - 1.0F * tuck - 1.5F * fling - 0.35F * fall, 0.15F * tuck - 0.2F * fling,
                0.1F * crouch + 0.35F * fling + 0.95F * fall, 0.3F * crouch + 1.2F * tuck + 0.1F * fling + 0.25F * fall);
            side.leg(pose, main, -0.3F * crouch - 1.25F * tuck - (main ? 0.25F : 0.05F) * fling + 0.1F * fall, 0.08F * crouch + 0.12F * fall);
        }
        float knee = 0.6F * crouch + 1.3F * tuck + 0.3F * fling + 0.35F * fall;
        pose.rightKnee = knee;
        pose.leftKnee = knee;
    }

    private static void blackRabbit(Pose pose, Side side, float progress) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        float crouch = smooth(p / 0.08F) * (1.0F - smooth((p - 0.08F) / 0.12F));
        float spin = smooth((p - 0.1F) / 0.28F);
        float tuck = smooth((p - 0.1F) / 0.12F) * (1.0F - smooth((p - 0.28F) / 0.12F));
        float hang = smooth((p - 0.36F) / 0.1F) * (1.0F - smooth((p - 0.7F) / 0.1F));
        float land = smooth((p - 0.78F) / 0.1F) * (1.0F - smooth((p - 0.88F) / 0.12F));
        // A full turn is the same as none: once he has come round, the flip reads 0 again.
        pose.flip = spin >= 1.0F ? 0.0F : (float)(Math.PI * 2.0) * spin;
        pose.torsoX = 0.3F * crouch + 0.55F * tuck - 0.1F * hang + 0.3F * land;
        pose.headX = 0.1F * crouch + 0.3F * tuck + 0.45F * hang - 0.1F * land;
        for (boolean main : new boolean[] {true, false}) {
            side.arm(pose, main, 0.3F * crouch - 1.0F * tuck - 0.55F * hang - 0.3F * land, 0.15F * tuck, 0.1F * crouch + 1.0F * hang + 0.3F * land,
                0.3F * crouch + 1.2F * tuck + 0.25F * hang + 0.4F * land);
            side.leg(pose, main, -0.3F * crouch - 1.25F * tuck - 0.25F * hang - 0.4F * land, 0.08F * crouch + 0.1F * hang + 0.12F * land);
        }
        float knee = 0.6F * crouch + 1.25F * tuck + 0.35F * hang + 0.8F * land;
        pose.rightKnee = knee;
        pose.leftKnee = knee;
    }

    /** A wing blade swung from its own side across the front ({@code scale} > 1 for the heavier One-Winged Kagune). */
    private static void sweep(Pose pose, Keys keys, Side side, float scale) {
        pose.twist = side.mirror(keys.at(0.35F, -0.4F, -0.45F) * scale);
        pose.headY = side.mirror(keys.at(-0.15F, 0.2F, 0.2F) * scale);
        pose.torsoX = keys.at(-0.05F, 0.2F, 0.18F) * scale;
        pose.torsoBend = keys.at(0.0F, 0.1F, 0.08F) * (scale - 1.0F) * 3.0F;
        pose.headX = keys.at(-0.05F, 0.05F, 0.05F);
        side.arm(pose, true, keys.at(-0.45F, -1.25F, -1.15F), keys.at(-0.5F, 0.75F, 0.8F) * scale, keys.at(0.5F, 0.1F, 0.05F),
            keys.at(0.3F, 0.05F, 0.05F));
        side.arm(pose, false, keys.at(0.0F, 0.3F, 0.3F), keys.at(0.0F, 0.0F, 0.0F), keys.at(0.1F, 0.3F, 0.3F) * scale,
            keys.at(0.2F, 0.5F, 0.45F));
        side.leg(pose, true, keys.at(0.05F, 0.15F, 0.15F) * scale, keys.at(0.0F, 0.0F, 0.0F));
        side.leg(pose, false, keys.at(0.0F, -0.25F, -0.22F) * scale, keys.at(0.0F, 0.0F, 0.0F));
    }

    /** Wind-up, impact and follow-through keys, eased, with rest at both ends. */
    private static final class Keys {
        private final float progress;
        private final float windupAt;
        private final float impactAt;
        private final float followAt;

        Keys(float progress, float impact) {
            this.progress = Float.isFinite(progress) ? Math.max(0.0F, Math.min(1.0F, progress)) : 0.0F;
            this.impactAt = Float.isFinite(impact) ? Math.max(0.1F, Math.min(0.8F, impact)) : 0.4F;
            this.windupAt = 0.55F * this.impactAt;
            this.followAt = this.impactAt + 0.18F * (1.0F - this.impactAt);
        }

        float at(float windup, float strike, float follow) {
            float p = this.progress;
            if (p <= 0.0F || p >= 1.0F) {
                return 0.0F;
            }
            if (p < this.windupAt) {
                return windup * smooth(p / this.windupAt);
            }
            if (p < this.impactAt) {
                return lerp(windup, strike, smooth((p - this.windupAt) / (this.impactAt - this.windupAt)));
            }
            if (p < this.followAt) {
                return lerp(strike, follow, smooth((p - this.impactAt) / (this.followAt - this.impactAt)));
            }
            return follow * (1.0F - smooth((p - this.followAt) / (1.0F - this.followAt)));
        }
    }

    /** Maps "main side / other side" limb values onto the right and left limbs, mirroring yaw and roll. */
    private record Side(float sign) {
        float mirror(float value) {
            return value * this.sign;
        }

        boolean right(boolean main) {
            return main == this.sign > 0.0F;
        }

        /** {@code across}: + swings across the body; {@code out}: + moves the arm outward. */
        void arm(Pose pose, boolean main, float x, float across, float out, float bend) {
            if (this.right(main)) {
                pose.rightArmX += x;
                pose.rightArmY -= across;
                pose.rightArmZ += out;
                pose.rightArmBend += bend;
            } else {
                pose.leftArmX += x;
                pose.leftArmY += across;
                pose.leftArmZ -= out;
                pose.leftArmBend += bend;
            }
        }

        /** {@code x}: - swings the leg forward; {@code out}: + moves it outward. */
        void leg(Pose pose, boolean main, float x, float out) {
            if (this.right(main)) {
                pose.rightLegX += x;
                pose.rightLegZ += out;
            } else {
                pose.leftLegX += x;
                pose.leftLegZ -= out;
            }
        }
    }

    private static float smooth(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }
}
