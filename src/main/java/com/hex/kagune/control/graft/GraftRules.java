package com.hex.kagune.control.graft;

import com.hex.kagune.control.DamageBalance;

/**
 * Graft DNA's numbers (pure, tested; 7.0.0): how hard each blow lands, the kakuja's price and limits, how his body heals, and the
 * timings both the server and every client read.
 */
public final class GraftRules {
    /** A click within this many ticks of a strike ending goes on to the next strike of the string. */
    public static final int CHAIN_WINDOW = 9;
    /** A key pressed while a move is still committed waits this long for it (then it is dropped). */
    public static final int BUFFER = 6;
    /**
     * 7.0.2 (PvP): the share of a blow's damage another player takes (a player has 20 health; a mob's blow at the kakuja's mastery
     * would take most of it). Stuns that last too long are broken by the player (StunRules.PLAYER_CHAIN).
     */
    public static final float PVP_SHARE = 0.45F;
    /** The Final Performance's claws: the fewest ticks between two (the click rate it answers). */
    public static final int PERF_CLAW_EVERY = 4;

    /** Stuns: a blow, a heavy one. */
    public static final int STUN = 10;
    public static final int HEAVY_STUN = 24;

    /** The kakuja: mastery, the RC it takes to transform, what it drains a tick, and the cooldown after it ends. */
    public static final int KAKUJA_MASTERY = 900;
    public static final float KAKUJA_COST = 25.0F;
    public static final float KAKUJA_DRAIN = 0.04F;
    public static final int KAKUJA_COOLDOWN = 200;
    /** Ticks to transform in (the kagune drawn back, the hide spreading, the colossi out) and back out. */
    public static final int KAKUJA_IN = 52;
    public static final int KAKUJA_OUT = 40;
    /** No move until the transformation has got this far. */
    public static final int KAKUJA_READY = 42;
    /** In the kakuja he takes this share of any blow. */
    public static final float KAKUJA_TAKEN = 0.55F;

    /** Hold K this long and it changes the base stage instead of transforming. */
    public static final int STAGE_HOLD = 10;

    /** The dash (M2): speed (blocks a tick) and lift, and the kakuja's longer one. */
    public static final double STEP_SPEED = 1.55;
    public static final double STEP_LIFT = 0.28;
    /** The kakuhou refuses to die once in this long (ticks), at this price in RC: a killing blow leaves him at one health. */
    public static final int SURVIVE_COOLDOWN = 3600;
    public static final float SURVIVE_COST = 30.0F;
    /** A blow at least this heavy (health) is healed back in part, fast: this share (the kakuja's), over this long, at this RC a health. */
    public static final float HEAVY_BLOW = 4.0F;
    public static final float RECOVER_SHARE = 0.35F;
    public static final float RECOVER_SHARE_KAKUJA = 0.65F;
    public static final int RECOVER_TICKS = 30;
    public static final float RECOVER_COST = 0.9F;

    // --- 7.0.2, the two tails' four (the user's rework) -----------------------------------------------------------------------

    /** Mockingbird: how near an attacker must be to be countered (blocks), and how hard he slips aside (blocks a tick). */
    public static final double COUNTER_RANGE = 6.5;
    public static final double COUNTER_DODGE = 1.15;
    /** The counter's dodge: blows at him this long into it miss (ticks). */
    public static final int COUNTER_DODGE_TICKS = 5;
    /** A stance nobody struck at: its cooldown is cut to this (ticks). */
    public static final int STANCE_WHIFF_COOLDOWN = 60;

    /** Owl's Execution: how far it looks for its prey, how close he stops, the circle he cuts it from, its sweep. */
    public static final double EXECUTION_SEEK = 11.0;
    public static final double EXECUTION_RADIUS = 2.6;
    /** The rush in (ticks from the start) and the circle round it (from, to), and how far round he goes (degrees). */
    public static final int EXECUTION_RUSH = 5;
    public static final int EXECUTION_CIRCLE_END = 22;
    public static final double EXECUTION_TURN = 270.0;
    /** The fastest he is carried, rushing or circling (blocks a tick). */
    public static final double EXECUTION_SPEED = 1.6;

    /** Scorpion Vault: the launch (blocks a tick up, and ahead along his look), when it fires, how long the dive can follow. */
    public static final double VAULT_UP = 1.0;
    public static final double VAULT_AHEAD = 0.55;
    public static final int VAULT_LAUNCH = 2;
    public static final int VAULT_WINDOW = 60;
    /** The dive: how fast (blocks a tick), the least it points down (the y of its direction), and the impact's reach and shares. */
    public static final double DIVE_SPEED = 2.0;
    public static final double DIVE_DOWN = 0.45;
    public static final double DIVE_RADIUS = 2.8;
    public static final float DIVE_DIRECT = 2.0F;
    public static final float DIVE_SPLASH = 0.8F;

    /** Human Puppet: how near he hauls himself to the pinned puppet (blocks) and how fast (blocks a tick). */
    public static final double PULL_CLOSE = 1.4;
    public static final double PULL_SPEED = 1.4;
    /** The throw: how hard it is hurled (blocks a tick, along his look) and the lift with it. */
    public static final double THROW_SPEED = 2.2;
    public static final double THROW_LIFT = 0.35;

    // --- 7.0.2, the centipedes' four --------------------------------------------------------------------------------------------

    /** Hungry Maw: how fast the mouth goes out after its prey (blocks a tick), how far from him it can (blocks), how often it snaps. */
    public static final double MAW_SPEED = 1.1;
    public static final double MAW_RANGE = 16.0;
    public static final int MAW_SNAP = 5;
    /** Where the mouth drags what it caught: low at his right side, a little ahead (blocks: right, up from his feet, ahead). */
    public static final double[] DRAG_POINT = {1.7, 0.45, 0.5};
    /** How often the dragging mouth chews (ticks), how hard the fling is (blocks a tick along his look) and its lift. */
    public static final int CHEW = 20;
    public static final double FLING_SPEED = 1.8;
    public static final double FLING_LIFT = 0.35;

    /** Centipede Locomotion: the haul each stride gives him (blocks a tick, along his look), and the stride's length (ticks). */
    public static final double CRAWL_PUSH = 0.95;
    public static final int CRAWL_STRIDE = 8;
    /** 7.0.2 how far out to his side a stride plants (blocks, at a centipede five long; it scales with the length). */
    public static final double CRAWL_SPLAY = 1.6;
    /** The crawl's bite: the lunge behind it (blocks a tick) and the tick it comes. */
    public static final double CRAWL_LUNGE = 0.9;
    public static final int CRAWL_LUNGE_AT = 2;

    /** Living Carapace: how near the attacker must be for the coils to spring at it (blocks), and the bite's window after. */
    public static final double CARAPACE_RANGE = 5.0;

    // --- 7.0.2, the four thorns' four ------------------------------------------------------------------------------------------

    /** Crucifixion: the tick it is hurled, how hard (blocks a tick along his look), and the lift. */
    public static final int CRUCIFY_THROW = 37;
    public static final double CRUCIFY_SPEED = 2.6;
    public static final double CRUCIFY_LIFT = 0.5;
    /** Unstoppable Breach: how fast he is thrown on (blocks a tick), until when, what he runs into is struck within (blocks). */
    public static final double BREACH_SPEED = 1.25;
    public static final int BREACH_TICKS = 20;
    public static final double BREACH_CONTACT = 1.7;
    /** Dragon's Dragnet: where it drags what it catches (blocks ahead of him), and until when. */
    public static final double DRAGNET_POINT = 3.5;
    public static final int DRAGNET_HOLD = 19;
    /** Living Catapult: the launch (blocks a tick up and ahead), when it fires, how hard he steers in the air (a tick). */
    public static final double CATAPULT_UP = 1.8;
    public static final double CATAPULT_AHEAD = 0.9;
    public static final int CATAPULT_LAUNCH = 3;
    public static final double CATAPULT_STEER = 0.11;

    // --- 7.0.2, the final kakuja's four ------------------------------------------------------------------------------------------

    /** King's Checkmate: how close his charge brings him to what is hooked (blocks), and its ticks. */
    public static final double CHECKMATE_CLOSE = 1.8;
    public static final int CHECKMATE_CHARGE_FROM = 11;
    public static final int CHECKMATE_CHARGE_TO = 15;
    /** Centipede Hunt: how far it looks (blocks), how fast he is hurled after it (blocks a tick), how near catches it, the drag. */
    public static final double HUNT_RANGE = 20.0;
    public static final double HUNT_SPEED = 1.4;
    public static final double HUNT_CATCH = 2.8;
    public static final double HUNT_DRAG_SPEED = 1.1;
    public static final int HUNT_THROW = 20;
    public static final double HUNT_THROW_SPEED = 2.4;
    /** Monstrous Recovery: the share of his health knit back over the job, the RC a health it costs, the share of blows he takes. */
    public static final float RECOVERY_SHARE = 0.75F;
    public static final float RECOVERY_COST = 1.3F;
    public static final float RECOVERY_TAKEN = 0.5F;
    /** The Final Performance: how far the colossi follow what he looks at (blocks), how often they look again (ticks). */
    public static final double PERFORMANCE_RANGE = 14.0;
    public static final int PERFORMANCE_RETARGET = 10;

    // --- 7.0.2, the mass of eyes' four ------------------------------------------------------------------------------------------

    /** Flesh Bloom: the ticks its waves of spikes burst along it. */
    public static final int[] BLOOM_WAVES = {14, 22, 30};
    /** Black Curtain: how wide the wall stands across his front (blocks either side), how far ahead its foot, its top (blocks up). */
    public static final double CURTAIN_SPAN = 4.2;
    public static final double CURTAIN_FOOT = 1.6;
    public static final double CURTAIN_TOP = 5.6;
    /** How high the fallen curtain drapes over what is under it, at the middle of what it covers (blocks). */
    public static final double CURTAIN_DRAPE = 2.1;
    /** The King's Game: how far the mass follows its prey (blocks), the sword's rush (blocks a tick) and how near it must bring him. */
    public static final double GAME_RANGE = 16.0;
    public static final double GAME_RUSH = 1.4;
    public static final double GAME_REACH = 4.0;

    private GraftRules() {
    }

    /**
     * 7.0.2 Eye of the Storm: crescent {@code limb}'s bearing round him {@code age} ticks in (radians from the way he faced, + to his
     * right): each its own speed, the odd ones the other way round, all wandering off their circles.
     */
    public static double stormAngle(int limb, double age) {
        double way = limb % 2 == 0 ? 1.0 : -1.0;
        return limb * 1.2566 + way * ((0.16 + 0.03 * limb) * age + 0.6 * Math.sin(age * 0.11 + limb * 1.7));
    }

    /** How far out crescent {@code limb} wheels (blocks), grown out over the first ticks, swelling and drawing in as it goes. */
    public static double stormRadius(int limb, double age, double length) {
        return smooth(age / 6.0) * length * (0.55 + 0.25 * Math.sin(age * 0.13 + limb * 2.1));
    }

    /** How high crescent {@code limb} wheels (blocks above his feet), rising and dipping. */
    public static double stormHeight(int limb, double age) {
        return 1.5 + 1.3 * Math.sin(age * 0.09 + limb * 1.3);
    }

    /** 7.0.2 Flesh Bloom: how far the spikes stand out {@code age} ticks in (1 at rest): each wave bursting, easing, the next. */
    public static double bloomFlare(double age) {
        if (age < 6.0 || age > 40.0) {
            return 1.0;
        }
        double out = 1.5;
        for (int wave : BLOOM_WAVES) {
            double d = (age - wave) / 1.8;
            out += 2.4 * Math.exp(-d * d);
        }
        return out * (age > 36.0 ? 1.0 - smooth((age - 36.0) / 4.0) * 0.6 : 1.0);
    }

    /**
     * 7.0.2 Black Curtain: how far over the wall has fallen {@code age} ticks in (0 standing, 1 lying on the ground ahead): it stands
     * from its rise until four ticks before the move's land tick {@code land}, then comes down onto the ground on it.
     */
    public static double curtainFold(double age, int land) {
        return smooth((age - (land - 4.0)) / 4.0);
    }

    /** 7.0.2 Hungry Maw: how far the mouth has gone out after its prey {@code age} ticks in (a limb {@code length} long). */
    public static double mawReach(double age, double length) {
        return length * 0.9 + MAW_SPEED * Math.max(0.0, age);
    }

    /**
     * 7.0.2 Centipede Locomotion: how far a stride has hauled him {@code ticks} after it planted (blocks; the push along the ground
     * with vanilla's friction, 0.546 a tick).
     */
    public static double crawled(double ticks) {
        double out = 0.0;
        double speed = CRAWL_PUSH;
        int whole = (int)Math.min(60, Math.floor(Math.max(0.0, ticks)));
        for (int i = 0; i < whole; i++) {
            out += speed;
            speed *= 0.546;
        }
        return out + speed * (Math.max(0.0, ticks) - whole);
    }

    /**
     * 7.0.2: how hard a cut makes a body bleed (stacks of the original's bleeding: its strength and length, UkakuRules), by the
     * blow's share of a full hit: the heavy blows open deeper wounds.
     */
    public static int bleedStacks(float power) {
        return power >= 2.0F ? 5 : power >= 1.2F ? 4 : power >= 0.6F ? 3 : 2;
    }

    /**
     * 7.0.2: a cut of {@code hit} stacks on a body bleeding {@code stacks} until {@code until} ({@code running}: still bleeding):
     * refreshed, never stacked. It bleeds as hard as the harder of the two and for at least the cut's own length from now, never
     * longer than either alone would have it. {stacks, until}.
     */
    public static long[] refreshBleed(int stacks, long until, boolean running, long now, int hit) {
        int strength = running ? Math.max(stacks, hit) : hit;
        long fresh = now + com.hex.kagune.control.ukaku.UkakuRules.bleedDuration(hit);
        return new long[] {strength, running ? Math.max(until, fresh) : fresh};
    }

    /**
     * 7.0.2: the dive's direction from where he looks ({@code x, y, z}, any frame with y up): along it, but never flatter than
     * {@link #DIVE_DOWN} down (looking up, he still comes down ahead of himself). Unit length.
     */
    public static double[] diveDirection(double x, double y, double z) {
        double flat = Math.hypot(x, z);
        double length = Math.sqrt(flat * flat + y * y);
        double down = length < 1.0E-9 ? -1.0 : y / length;
        if (down > -DIVE_DOWN) {
            down = -DIVE_DOWN;
        }
        double level = Math.sqrt(Math.max(0.0, 1.0 - down * down));
        if (flat < 1.0E-9) {
            return new double[] {0.0, -1.0, 0.0}; // straight up or down: no heading, so straight down
        }
        return new double[] {x / flat * level, down, z / flat * level};
    }

    /** 7.0.2: how high the vault has thrown him {@code ticks} after it fired (blocks; Minecraft's gravity and drag on {@link #VAULT_UP}). */
    public static double vaultRise(double ticks) {
        return rise(VAULT_UP, ticks);
    }

    /** 7.0.2: how high the catapult has flung him {@code ticks} after it fired (blocks; {@link #CATAPULT_UP}). */
    public static double catapultRise(double ticks) {
        return rise(CATAPULT_UP, ticks);
    }

    /** How high a body thrown up at {@code up} blocks a tick is {@code ticks} later (Minecraft's gravity and drag). */
    static double rise(double up, double ticks) {
        if (ticks <= 0.0) {
            return 0.0;
        }
        double y = 0.0;
        double v = up;
        int whole = (int)Math.min(80, Math.floor(ticks));
        for (int i = 0; i < whole; i++) {
            y += v;
            v = (v - 0.08) * 0.98;
        }
        return Math.max(0.0, y + v * (ticks - whole));
    }

    /**
     * 7.0.2: the execution's circle: how far round it he is {@code age} ticks in (degrees from where the rush brought him in; 0
     * until the cuts begin, {@link #EXECUTION_TURN} once they are done), eased in and out.
     */
    public static double executionTurn(double age) {
        return EXECUTION_TURN * smooth((age - EXECUTION_RUSH) / (double)(EXECUTION_CIRCLE_END - EXECUTION_RUSH));
    }

    /** A full blow's damage at {@code mastery} in {@code stage} (before a beat's own share). */
    public static float damage(int mastery, GraftStage stage) {
        int m = Math.max(0, Math.min(1000, mastery));
        return (5.5F + m * 0.0065F) * stage.damage() * DamageBalance.scale(m);
    }

    /** RC a tick while the kagune is out (it is fed by the kakuhou), more with mastery; none in the kakuja. */
    public static float formRegen(int mastery, boolean kakuja) {
        return kakuja ? 0.0F : 0.018F + 0.03F * Math.max(0, Math.min(1000, mastery)) / 1000.0F;
    }

    /**
     * Regeneration ({@code wound/Regeneration} is every ghoul's; this is how much faster his is): the share of the usual wait before
     * his wounds start to close, and of the time they take to close. Faster with mastery, far faster in the kakuja.
     */
    public static double waitShare(int mastery, boolean kakuja) {
        double m = Math.max(0, Math.min(1000, mastery)) / 1000.0;
        return kakuja ? 0.12 : 0.5 - 0.25 * m;
    }

    public static double healShare(int mastery, boolean kakuja) {
        double m = Math.max(0, Math.min(1000, mastery)) / 1000.0;
        return kakuja ? 0.35 : 0.75 - 0.25 * m;
    }

    /** RC each point of health his faster healing costs (the plain ghoul's part of it is free, from the flesh reserve as always). */
    public static final float REGEN_COST = 0.55F;

    /** The base stage a kakuja transformation ({@code in}, from {@code age} ticks into it) draws: before the colossi, the old stage shrinks away. */
    public static double baseRetract(boolean in, double age) {
        return in ? 1.0 - smooth(age / 12.0) : smooth((age - 24.0) / 16.0);
    }

    /** The hide's spread through a transformation (0 none .. 1 whole): in, from the back over the body to the face; out, the reverse. */
    public static double hide(boolean in, double age) {
        return in ? smooth((age - 6.0) / 30.0) : 1.0 - smooth((age - 8.0) / 22.0);
    }

    /** The colossi's growth through a transformation (0 inside him .. 1 unfolded). */
    public static double colossi(boolean in, double age) {
        return in ? smooth((age - 24.0) / 26.0) : 1.0 - smooth(age / 16.0);
    }

    /**
     * Where a held body's middle is carried (rig space: blocks to his right, up from his feet, ahead), {@code since} ticks after the
     * limb closed on it ({@code kind}: the limb's beat): 7.0.2, the puppet held out at the tail's length (hoisted high for the throw),
     * the crucified one held up between the upper thorns, the performance's prey hauled right in to him; else at his side.
     */
    public static double[] heldPoint(GraftMove move, GraftMove.Kind kind, double since) {
        if (move == GraftMove.CRUCIFIXION) {
            return new double[] {0.0, 1.3 + 1.6 * smooth(since / 8.0), 3.6};
        }
        if (move == GraftMove.PERF_FINISH) {
            return new double[] {0.6, 1.4, 2.0};
        }
        if (move == GraftMove.HUMAN_PUPPET || move == GraftMove.PUPPET_STRIKE) {
            return new double[] {0.4, 1.3, 3.4};
        }
        if (move == GraftMove.PUPPET_THROW) {
            return new double[] {0.45, 1.3 + 2.3 * smooth(since / 4.0), 3.4 - 1.3 * smooth(since / 4.0)};
        }
        return new double[] {0.9, 1.4, 2.3};
    }

    static double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }
}
