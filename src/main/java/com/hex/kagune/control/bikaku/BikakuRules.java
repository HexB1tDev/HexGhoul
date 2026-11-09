package com.hex.kagune.control.bikaku;

import com.hex.kagune.control.DamageBalance;
import com.hex.kagune.control.graft.GraftMesh;

/**
 * 3.4.9, the Bikaku path's numbers (pure, tested): its M1 bites (a string of three, each timed as {@link MawMotion#bite} draws it), how far
 * the worm reaches, what a bite does (its damage by mastery, a player's share, the bleeding, the third bite's stun), Predatory Feeding
 * (a bite that lands heals a little, a kill with the worm more), and the mastery each of the path's moves unlocks at.
 */
public final class BikakuRules {
    private BikakuRules() {
    }

    // ---------------------------------------------------------------- the M1 string

    /** The bites of the M1 string, in order: two quick bites from different sides, then a heavy one that shakes what it caught. */
    public enum Bite {
        NONE(0, 0, 0.0F, 0.0F),
        /** Thrown out low from over the shoulder. */
        SNAP(12, 6, 1.0F, 0.4F),
        /** Reared up high and brought down on it. */
        LUNGE(13, 7, 1.0F, 0.4F),
        /** Drawn back further, thrown wide, clamped, shaken: harder, it stuns. */
        MAUL(18, 8, 1.5F, 0.6F),
        /** 3.5.4: in the kakuja, M1 is one of its tentacles thrown far out at what he aims at, biting it. */
        TENTACLE(18, 9, 1.8F, 0.5F);

        private final int ticks;
        private final int land;
        private final float power;
        private final float cost;

        Bite(int ticks, int land, float power, float cost) {
            this.ticks = ticks;
            this.land = land;
            this.power = power;
            this.cost = cost;
        }

        /** How long the whole bite takes (ticks): drawn back, thrown, shut, back to rest. */
        public int ticks() {
            return this.ticks;
        }

        /** The tick its mouth snaps shut on what it bites. */
        public int land() {
            return this.land;
        }

        /** Its share of a bite's damage. */
        public float power() {
            return this.power;
        }

        /** The RC it costs. */
        public float cost() {
            return this.cost;
        }

        /** The heavy bite: it stuns, and is shaken. */
        public boolean heavy() {
            return this == MAUL;
        }

        /** The next bite of the string after this one (after the last, the first again). */
        public Bite next() {
            return switch (this) {
                case SNAP -> LUNGE;
                case LUNGE -> MAUL;
                case TENTACLE -> TENTACLE;
                default -> SNAP;
            };
        }

        /** The bite with ordinal {@code ordinal} (none for anything unknown). */
        public static Bite byOrdinal(int ordinal) {
            Bite[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }

        /** Which way of drawing back {@link MawMotion#bite} gives it (0 low, 1 high, 2 heavy). */
        public int style() {
            return this == LUNGE ? 1 : this == MAUL ? 2 : 0;
        }
    }

    /** A click within this many ticks of a bite's end goes on with the string; later, it starts again. */
    public static final int CHAIN_WINDOW = 9;
    /** A click while a bite is still out waits this long for it (then it is dropped). */
    public static final int BUFFER = 6;

    /** How far the mouth reaches from over the right shoulder (blocks): the worm is long. */
    public static final double REACH = 6.5;
    /** 3.5.0: how far along the ground Predatory Drag reaches (blocks from his feet), and Devouring Maw's reach (from over his shoulder). */
    public static final double DRAG_REACH = 10.0;
    /** 3.5.0: the grabs' damage shares: Devouring Maw's bite, Predatory Drag's bite and its slam. */
    public static final float MAW_POWER = 2.2F;
    public static final float DRAG_BITE_POWER = 0.8F;
    public static final float DRAG_SLAM_POWER = 1.8F;
    /** 3.5.0: what a grab's catch is stunned for after it is let go, and the slam's stun (ticks). */
    public static final int GRAB_STUN_AFTER = 8;
    public static final int SLAM_STUN = 24;
    /** 3.5.0: Devouring Maw heals more (its spec: "minor healing from successful bites"). */
    public static final float MAW_HEAL = 2.0F;

    /** 3.5.1: Ravenous Pursuit: how far its worms reach (from his eyes), each bite's share, how thick each worm is. */
    public static final double PURSUIT_REACH = 7.0;
    public static final float PURSUIT_POWER = 1.1F;
    public static final double PURSUIT_GIRTH = 0.62;
    /** 3.5.1: Carrion Eruption: how far it tunnels (along the ground from his feet), how wide it bursts up, its share, its throw up. */
    public static final double ERUPTION_REACH = 12.0;
    public static final double ERUPTION_RADIUS = 1.6;
    public static final float ERUPTION_POWER = 1.6F;
    public static final double ERUPTION_LAUNCH = 0.85;

    /** 3.5.3: Monstrous Regeneration heals this much every so many ticks while it lasts (32 health over its 8 s). */
    public static final float REGEN_HEAL = 1.0F;
    public static final int REGEN_EVERY = 5;
    /**
     * 3.5.3, the passives while the worm is out: Abnormal Regeneration (a ghoul's regeneration starts after this share of its usual wait,
     * and heals in this share of its usual time), Pain Suppression (knockback resistance added, the share of a stun he still takes).
     */
    public static final double REGEN_WAIT_SHARE = 0.4;
    public static final double REGEN_HEAL_SHARE = 0.5;
    public static final double KNOCKBACK_RESIST = 0.6;
    public static final double STUN_SHARE = 0.5;

    /**
     * 3.5.4, the kakuja: the RC to go into it and what it drains a tick, its cooldown after it ends, the share of every blow he still takes
     * in it (Abnormal Flesh), how far its tentacles reach (from its middle), its regeneration (1 health every so many ticks), how slow it
     * is (Slowness, its amplifier: Immovable Horror).
     */
    public static final float KAKUJA_COST = 25.0F;
    public static final float KAKUJA_DRAIN = 0.05F;
    public static final int KAKUJA_COOLDOWN = 1200;
    public static final float KAKUJA_TAKEN = 0.45F;
    public static final double KAKUJA_REACH = 14.0;
    public static final int KAKUJA_REGEN_EVERY = 8;
    public static final int KAKUJA_SLOW = 5;
    /**
     * 3.6.3, Grand Devouring's cooldown (the owner: "Grand Devour should have a cooldown of 1 minute"), from when its worm is itself again
     * (fed, or let go before it swallowed).
     */
    public static final int DEVOUR_COOLDOWN = 1200;
    /**
     * 3.6.7, Potential Unlock (the owner: "The maximum energy capacity is also permanently multiplied by 3"; "Upon unlocking Potential
     * Unlock, the player's energy bar permanently changes color"; then "Should be Purple, the bar, my bad"): how many times the energy he holds,
     * and his bar's colour. Energy stays on the bar's own scale (the original's clamps, sync and rest-time recovery all assume it); every
     * price is that much less of it, and it reads that many times more.
     */
    public static final int POTENTIAL_CAPACITY = 3;
    public static final int POTENTIAL_PURPLE = 0x8A2BE2;

    /** 3.6.7: how many times the energy a Bikaku with {@code mutations} (BikakuMutation bits) holds. */
    public static int capacity(int mutations) {
        return BikakuMutation.POTENTIAL_UNLOCK.in(mutations) ? POTENTIAL_CAPACITY : 1;
    }
    public static final double KAKUJA_STEADY = 1.0;
    /** Its middle (the body frame): where its tentacles' reach is measured from. */
    public static final double[] KAKUJA_CENTRE = {0.0, 2.4, 0.0};

    /**
     * 3.5.7, the kakuja's moves (shares of a bite's damage, heals in health). Thousand Jaws: each tentacle's bite and what it heals; its
     * grins snap at what is within {@link #GRIN_REACH} of its middle (a big bite, a stun).
     */
    public static final float JAWS_POWER = 0.4F;
    public static final float JAWS_HEAL = 0.5F;
    public static final double GRIN_REACH = 4.4;
    public static final float GRIN_POWER = 1.0F;
    public static final int GRIN_STUN = 16;
    /** Endless Hunger: each bite while it holds its catch and what it heals; the maw's crushing bite and what it heals; its stun after. */
    public static final float HUNGER_POWER = 0.8F;
    public static final float HUNGER_HEAL = 2.0F;
    public static final float HUNGER_CRUSH_POWER = 3.0F;
    public static final float HUNGER_CRUSH_HEAL = 6.0F;
    public static final int HUNGER_STUN = 30;
    /**
     * Flesh Cataclysm: what is within {@link #CATA_NEAR} of its middle as it bursts is hurled out ({@link #CATA_NEAR_POWER}); what a
     * sweeping tentacle passes (within {@link #CATA_ARC} of its way, as far as its mouth and a little more) is hit hard ({@link #CATA_POWER});
     * both stunned; how hard they are thrown (out, up).
     */
    public static final double CATA_NEAR = 5.0;
    public static final float CATA_NEAR_POWER = 1.4F;
    public static final float CATA_POWER = 2.4F;
    public static final int CATA_STUN = 30;
    public static final double CATA_ARC = Math.toRadians(16.0);
    public static final double CATA_THROW_NEAR = 1.4;
    public static final double CATA_THROW = 1.0;
    /**
     * Undying Abomination: 1 health every {@link #UNDYING_EVERY} ticks over the kakuja's own; the share of a blow it still takes over the
     * kakuja's; a blow of {@link #UNDYING_HEAVY} or more slows it to every {@link #UNDYING_SLOW_EVERY} ticks for {@link #UNDYING_SLOWED}.
     */
    public static final float UNDYING_HEAL = 1.0F;
    public static final int UNDYING_EVERY = 4;
    public static final float UNDYING_TAKEN = 0.6F;
    public static final float UNDYING_HEAVY = 8.0F;
    public static final int UNDYING_SLOW_EVERY = 12;
    public static final int UNDYING_SLOWED = 40;
    /**
     * Autonomous Predation: its mouthed tentacles bite, on their own, hostile bodies within {@link #AUTO_REACH} of its middle: each at most
     * every {@link #AUTO_EVERY} ticks (and a little more for each), never two starting within {@link #AUTO_GAP} ticks of each other.
     */
    public static final double AUTO_REACH = 12.0;
    public static final float AUTO_POWER = 0.8F;
    public static final float AUTO_HEAL = 0.5F;
    public static final int AUTO_EVERY = 45;
    public static final int AUTO_GAP = 8;

    /** A bite at nothing in particular takes what is within this of its mouth when it shuts. */
    public static final double MOUTH_RADIUS = 1.1;

    /** A bite's damage at {@code mastery}, before its share. */
    public static float damage(int mastery) {
        int m = Math.max(0, Math.min(1000, mastery));
        return (5.5F + m * 0.0065F) * DamageBalance.scale(m);
    }

    /** The share of a bite another player takes (a player is not a mob; as Graft DNA's). */
    public static final float PVP_SHARE = 0.45F;
    /** The heavy bite's stun (ticks). */
    public static final int STUN = 12;

    /** Bleeding stacks a bite leaves. */
    public static int bleedStacks(Bite bite) {
        return bite.heavy() ? 4 : 2;
    }

    // ---------------------------------------------------------------- Predatory Feeding

    /** Health a bite that lands gives back. */
    public static final float BITE_HEAL = 1.0F;
    /** Health a kill with the worm gives back (on top of the bite's). */
    public static final float KILL_HEAL = 4.0F;

    // ---------------------------------------------------------------- what unlocks when (the plan's gates; only M1 is built so far)

    public static final int MAW = 0;
    public static final int DRAG = 150;
    public static final int PURSUIT = 300;
    public static final int ERUPTION = 450;
    public static final int REGENERATION = 600;
    public static final int KAKUJA = 1000;

    // ---------------------------------------------------------------- where the bite goes

    /**
     * Where a bite at {@code target} goes, in the body's frame: the target itself, or, past the worm's reach from over the shoulder, as far
     * toward it as the worm reaches.
     */
    public static double[] reach(double[] target) {
        return reach(target, REACH);
    }

    /** 3.5.8: {@code target} held to {@code reach} from over his shoulder (a bite thrown as far as it was held for). */
    public static double[] reach(double[] target, double reach) {
        double[] from = MawMotion.SHOULDER;
        double[] to = GraftMesh.sub(target, from);
        double distance = GraftMesh.length3(to);
        if (distance <= reach || distance < 1.0E-9) {
            return target;
        }
        return GraftMesh.add(from, GraftMesh.mul(to, reach / distance));
    }

    /**
     * 3.5.8 (the owner: "it should be a held move that the longer you hold it for, the longer it goes for ... this specific kagune should
     * have a lot of range, like covering up to like 30 blocks. Of course, attacks are not able to be performed until the kagune is pulled
     * fully back"): held, a bite or a move reaches from its own reach on a tap to {@link #HELD_REACH} after {@link #CHARGE_FULL} ticks; held
     * past {@link #CHARGE_GIVE_UP}, it goes by itself.
     */
    public static final double HELD_REACH = 30.0;
    public static final int CHARGE_FULL = 40;
    public static final int CHARGE_GIVE_UP = 240;
    /** 3.5.8: how much longer (a share of its time) a throw takes for each block it reaches past its own reach: out and back. */
    public static final double SLOWER_PER_BLOCK = 0.05;

    /** 3.5.8: how far a bite or move whose own reach is {@code base} goes after a hold of {@code held} ticks. */
    public static double heldReach(double base, double held) {
        return base + (HELD_REACH - base) * Math.max(0.0, Math.min(1.0, held / CHARGE_FULL));
    }

    /** 3.5.8: how many times longer a throw to {@code reach} takes than one to its own {@code base} reach (every beat of it). */
    public static double slower(double reach, double base) {
        return 1.0 + Math.max(0.0, reach - base) * SLOWER_PER_BLOCK;
    }

    /** Where a bite at nothing goes: ahead along the look, as far as it reaches (body frame, from the eyes). */
    public static double[] ahead(double eyeHeight, double pitchRadians) {
        double reach = REACH * 0.85;
        return reach(new double[] {0.0, eyeHeight - Math.sin(pitchRadians) * reach, Math.cos(pitchRadians) * reach});
    }

    /** The worm's look for a player: its seed (ridges, spurs, teeth, rhythm), the same on every client. */
    public static int seed(long most, long least) {
        long mixed = most * 31L + least;
        return (int)((mixed ^ (mixed >>> 32)) & 0x7FFF);
    }
}
