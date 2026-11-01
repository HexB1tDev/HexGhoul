package com.hex.kagune.control.npc;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 1.3.27: the CCG's investigators (pure): who they are, how they are built, when they turn up, who they go for and how they fight.
 *
 * <p>They walk in pairs and nothing else: two of them turn up together ({@link #SPAWN_CHANCE}: rare, a little more often than a wild
 * ghoul, any time of day), walk the world together, and a pair is never seen near another. They are human and keep the peace: a
 * monster that comes too close ({@link #MOB_RANGE}) is cut down, a wild ghoul in sight ({@link #GHOUL_RANGE}) is hunted, and a player
 * is let be until they show what they are: a kagune out in front of them ({@link #revealed}) and the pair goes for them, and keeps
 * after them a while ({@link #MARKED}) even if it goes back in. Whoever strikes either of them is fought.
 *
 * <p>Each carries a quinque shut in its case ({@link #kind}), opened as a fight finds it and shut again after; fast on their feet
 * ({@link #SPEED}, faster still in a fight), flipping in and out, dashing past a blow, meeting a kagune's blow with their own (the two
 * thrown apart) or slapping it aside, and stringing their weapon's three M1s ({@link Quinque#next}). The case is all but never left
 * behind when one falls ({@link #dropChance}).
 */
public final class AgentRules {
    private AgentRules() {
    }

    /** One investigator's look: its skin ({@code textures/entity/ccg/<texture>.png}), a woman's body or a man's, and its arms. */
    public record Look(String texture, boolean female, boolean slim) {
        public String path() {
            return "textures/entity/ccg/" + this.texture + ".png";
        }
    }

    /** The owner's skins: four women (all slim armed), five men (three wide, two slim). */
    public static final Look[] LOOKS = {
        new Look("ccg_f0", true, true),
        new Look("ccg_f1", true, true),
        new Look("ccg_f2", true, true),
        new Look("ccg_f3", true, true),
        new Look("ccg_m0", false, false),
        new Look("ccg_m1", false, false),
        new Look("ccg_m2", false, false),
        new Look("ccg_m3", false, true),
        new Look("ccg_m4", false, true),
    };

    /** The look numbered {@code index} (any int: wrapped onto the table). */
    public static Look look(int index) {
        return LOOKS[Math.floorMod(index, LOOKS.length)];
    }

    // ---------------------------------------------------------------- their bodies

    /**
     * A woman: Eto's own body (the female figure), "a little taller and skinnier": this much taller than Eto ({@code FemaleBody.SIZE}),
     * drawn this much narrower for her height, with a little less of the figure's curves and leg width.
     */
    public static final float TALLER = 1.07F;
    public static final float FEMALE_SIZE = com.hex.kagune.control.FemaleBody.SIZE * TALLER;
    public static final float FEMALE_BUILD = 0.93F;
    public static final float FEMALE_CURVES = 0.85F;
    public static final float FEMALE_LEGS = 0.8F;
    /** A man: a player's size as a player is drawn. */
    public static final float MALE_SIZE = 0.9375F;

    /** How tall it is drawn against a player's model. */
    public static float size(Look look) {
        return look.female() ? FEMALE_SIZE : MALE_SIZE;
    }

    /** How wide it is drawn against its height. */
    public static float build(Look look) {
        return look.female() ? FEMALE_BUILD : 1.0F;
    }

    // ---------------------------------------------------------------- what it is

    /** Health, armour, movement (faster than a wild ghoul's lowest), how far it follows. */
    public static final double HEALTH = 44.0;
    public static final double ARMOR = 6.0;
    public static final double SPEED = 0.34;
    public static final double FOLLOW = 40.0;
    /** How much faster it moves in a fight (on top of its speed). */
    public static final double FIGHT_PACE = 1.35;
    /** Its walk about with its partner (slow, a patrol). */
    public static final double PATROL_PACE = 0.62;

    /**
     * The quinque a new one carries, for a roll {@code r} in 0..1: a blade or a katana most often, a spear, the heavy mace now and then,
     * the launcher least (one of a pair at most: {@link #partnerKind}).
     */
    public static Quinque kind(double r) {
        if (r < 0.26) {
            return Quinque.BLADE;
        }
        if (r < 0.50) {
            return Quinque.KATANA;
        }
        if (r < 0.72) {
            return Quinque.SPEAR;
        }
        if (r < 0.88) {
            return Quinque.MACE;
        }
        return Quinque.LAUNCHER;
    }

    /** Its partner's quinque for a roll {@code r}: never the same as {@code first}, never two launchers (one keeps the other's back). */
    public static Quinque partnerKind(Quinque first, double r) {
        Quinque kind = kind(r);
        if (kind == first) {
            Quinque[] all = Quinque.values();
            kind = all[(kind.ordinal() + 1 + (int)Math.floor(r * 3.0)) % all.length];
        }
        if (kind == first || kind == Quinque.LAUNCHER && first == Quinque.LAUNCHER) {
            kind = first == Quinque.KATANA ? Quinque.SPEAR : Quinque.KATANA;
        }
        return kind;
    }

    /** The case's chance of being left when one falls: really rare (and a little less rare with Looting). */
    public static double dropChance(int looting) {
        return 0.025 + 0.01 * Math.max(0, Math.min(3, looting));
    }

    // ---------------------------------------------------------------- when they turn up

    /** Every this many ticks each player in the overworld has this chance of a pair turning up: about one pair every two days. */
    public static final int SPAWN_EVERY = 200;
    public static final double SPAWN_CHANCE = 0.0042;
    /** How far from the player (blocks), no player closer than this to the spot, the pair this far apart. */
    public static final double SPAWN_NEAREST = 28.0;
    public static final double SPAWN_FARTHEST = 52.0;
    public static final double CLEAR_OF_PLAYERS = 22.0;
    public static final double PAIR_APART = 1.6;
    /** No other investigators within this of where a pair turns up. */
    public static final double ALONE = 128.0;

    /** True if a new pair may turn up with the nearest other investigator {@code nearest} away. */
    public static boolean spaced(double nearest) {
        return nearest >= ALONE;
    }

    // ---------------------------------------------------------------- who they go for

    /** A monster this close is cut down; a wild ghoul this close is hunted; a revealed ghoul this close is gone for. */
    public static final double MOB_RANGE = 6.0;
    public static final double GHOUL_RANGE = 20.0;
    public static final double REVEALED_RANGE = 24.0;
    /** A fight is given up past this (blocks from it). */
    public static final double LEASH = 40.0;
    /** A revealed ghoul is kept after this long (ticks: two minutes); one who struck them, this long. */
    public static final long MARKED = 2400L;
    public static final long GRUDGE = 900L;

    /**
     * True if a player shows what they are: a ghoul with their kagune out, within {@link #REVEALED_RANGE} and in sight. A ghoul who keeps
     * it in is just another face in the crowd.
     */
    public static boolean revealed(boolean ghoul, boolean kaguneOut, double distance, boolean seen) {
        return ghoul && kaguneOut && seen && distance <= REVEALED_RANGE;
    }

    /** True if a monster {@code distance} away is close enough to be cut down. */
    public static boolean tooClose(double distance) {
        return distance <= MOB_RANGE;
    }

    // ---------------------------------------------------------------- how they fight

    /** How far its weapon reaches it at (blocks between them): where it stands to swing, and the most a blow lands at. */
    public static double reach(Quinque q) {
        return switch (q) {
            case BLADE -> 2.6;
            case KATANA -> 3.0;
            case SPEAR -> 4.4;
            case MACE -> 3.1;
            case LAUNCHER -> 2.4;
        };
    }

    /** Ticks between the start of a swing and its blow landing (its windup: the cut is at {@link #LANDS_AT} of its swing). */
    public static final double LANDS_AT = 0.45;

    public static int lands(Quinque q) {
        return Math.max(2, (int)Math.round(q.swing * LANDS_AT));
    }

    /** The pause after a blow before the next of its string (ticks), and after a finisher before it starts again. */
    public static int between(Quinque q) {
        return switch (q) {
            case SPEAR -> 3;
            case BLADE -> 4;
            case KATANA -> 5;
            case MACE -> 9;
            case LAUNCHER -> 6;
        };
    }

    public static final int AFTER_STRING = 18;

    /** Its blow's damage: the weapon's own (as a player's would be), its string's weight; a ghoul is cut deeper by the quinque itself. */
    public static float damage(Quinque q, int step) {
        return q.attackDamage() * q.weight(step) * 0.85F;
    }

    /** The chance it meets a blow coming at it with its own (the two thrown apart: a kagune slapped aside), by its weapon. */
    public static double parry(Quinque q) {
        return switch (q) {
            case BLADE -> 0.5;
            case KATANA -> 0.62;
            case SPEAR -> 0.48;
            case MACE -> 0.3;
            case LAUNCHER -> 0.0;
        };
    }

    /** The chance it flips or dashes clear of a blow it doesn't meet. */
    public static final double DODGE = 0.55;
    /** The chance each tick it is ready that it flips in on prey too far off (front flip), or clear of prey too close. */
    public static final double FLIP_CHANCE = 0.18;
    /** Not another flip or dash for this long after one (ticks). */
    public static final int FLIP_EVERY = 34;
    public static final int DASH_EVERY = 22;
    /**
     * 1.3.33: the chance each tick it may that it slips aside, left or right, mid-fight (between blows, or as it shoots): about one
     * every two seconds. The owner: "not as agile as expected... a dash animation, like dash left, right".
     */
    public static final double SIDE_DASH = 0.05;
    /** 1.3.33: the chance each tick that it dashes in on prey just out of its reach. */
    public static final double DASH_IN = 0.06;
    /** A dash: how hard it pushes off (blocks a tick), and how long the push lasts (renewed against the ground's grip). */
    public static final double DASH_SPEED = 0.8;
    public static final int DASH_TICKS = 4;
    /** Flips: how fast along and up. */
    public static final double FLIP_SPEED = 0.7;
    public static final double FLIP_UP = 0.62;
    public static final double FLIP_BACK_SPEED = 0.66;
    public static final double FLIP_BACK_UP = 0.6;

    /** The launcher: where it likes to stand from its prey (blocks), a burst of shards, the gap between shards and between bursts. */
    public static final double SHOOT_NEAREST = 7.0;
    public static final double SHOOT_FARTHEST = 16.0;
    public static final int BURST = 8;
    /** 1.3.33: as fast as a player's (Quinque.SHOT_EVERY). */
    public static final int SHOT_GAP = Quinque.SHOT_EVERY;
    public static final int BURST_GAP = 30;
    /** Its shard's damage against a player's (a little less: a pair of them fires a lot). */
    public static final float SHARD_DAMAGE = Quinque.SHARD_DAMAGE * 0.8F;

    /** How long its case takes to come open before it can swing (its unfold), and how long after its last fight it shuts it. */
    public static final int CALM = 160;

    // ---------------------------------------------------------------- 1.3.28: the two of them together

    /*
     * A pair on one prey never comes at it in a line. One holds the front: it comes straight on and squares up just out of its prey's
     * reach (HOLD_OFF past its own), its eyes on it, and strikes when the prey comes to it, once the other is round, or after HOLD_TICKS.
     * The other goes round: wide (ROUND_RADIUS, past a kagune's reach), the way round on its own side of the one in front (never across
     * it), and comes in from the prey's back (within BEHIND of straight opposite the one in front). The launcher always holds the front
     * (it fights from range); otherwise the leader does, until the prey turns its back on it to face the other for SWAP_AFTER ticks: then
     * the two swap (the one now behind it goes in, the one now in front holds).
     *
     * Angles are round the prey, atan2(dz, dx) of a position less the prey's, radians.
     */

    /** How far from its prey the one going round keeps (blocks), and how far round it heads at a time (radians). */
    public static final double ROUND_RADIUS = 5.5;
    public static final double ROUND_STEP = Math.toRadians(55.0);
    /** Within this of the prey's back (radians) the one going round goes in; once in, it stays in while within the wider one. */
    public static final double BEHIND = Math.toRadians(40.0);
    public static final double BEHIND_KEPT = Math.toRadians(75.0);
    /** The one in front: how far past its reach it squares up while the other goes round (blocks), and the longest it waits (ticks). */
    public static final double HOLD_OFF = 1.2;
    public static final int HOLD_TICKS = 60;
    /** The prey turned on the one behind (its face within FACING of it, the one in front past TURNED_AWAY) this long: the two swap. */
    public static final int SWAP_AFTER = 30;
    public static final double FACING = Math.toRadians(50.0);
    public static final double TURNED_AWAY = Math.toRadians(100.0);
    /** A fight is a new one (its sides chosen again) after this long out of one (ticks). */
    public static final int NEW_FIGHT = 40;

    /**
     * True if one of a pair holds the front against their prey ({@code mine}, {@code theirs}: the two quinques; {@code swapped}: the
     * pair's sides swapped since the fight began); false, it goes round to the back. Never both: the pair always has one of each.
     */
    public static boolean front(boolean leader, Quinque mine, Quinque theirs, boolean swapped) {
        if (mine.ranged != theirs.ranged) {
            return mine.ranged;
        }
        return leader != swapped;
    }

    /** {@code a} wrapped onto -pi..pi. */
    public static double wrap(double a) {
        double w = Math.IEEEremainder(a, 2.0 * Math.PI);
        return Double.isFinite(w) ? w : 0.0;
    }

    /** The prey's back: straight opposite the one in front ({@code front}: its angle round the prey). */
    public static double back(double front) {
        return wrap(front + Math.PI);
    }

    /** True if {@code at} (an angle round the prey) is within {@link #BEHIND} of {@code goal}. */
    public static boolean behind(double at, double goal) {
        return Math.abs(wrap(goal - at)) <= BEHIND;
    }

    /** True if the one already in from behind stays in: {@code at} within {@link #BEHIND_KEPT} of {@code goal} (no going back out for a step). */
    public static boolean kept(double at, double goal) {
        return Math.abs(wrap(goal - at)) <= BEHIND_KEPT;
    }

    /**
     * Where the one going round heads next (an angle round the prey) from {@code at} toward {@code goal} (the prey's back): the short way
     * round, at most {@link #ROUND_STEP} on; {@code way} (+1 or -1: the side of the one in front it is on) when it stands about straight
     * across from it (next to the one in front), so it never crosses in front of it.
     */
    public static double round(double at, double goal, double way) {
        double d = wrap(goal - at);
        if (Math.abs(d) > Math.PI - 0.35) {
            d = (way >= 0.0 ? 1.0 : -1.0) * Math.abs(d);
        }
        return wrap(at + Math.max(-ROUND_STEP, Math.min(ROUND_STEP, d)));
    }

    /** The side ({@code +1}/{@code -1}) of the one in front ({@code front}) that {@code at} is on, {@code otherwise} if right on it. */
    public static double sideOf(double at, double front, double otherwise) {
        double d = wrap(at - front);
        return Math.abs(d) < 1.0E-3 ? (otherwise >= 0.0 ? 1.0 : -1.0) : Math.signum(d);
    }

    /**
     * True if the prey, facing {@code look} (an angle), has turned on the one behind it ({@code backAt}) and its back on the one in
     * front ({@code frontAt}): the one behind in front of its face, the one in front out of its sight.
     */
    public static boolean turned(double look, double frontAt, double backAt) {
        return Math.abs(wrap(backAt - look)) <= FACING && Math.abs(wrap(frontAt - look)) >= TURNED_AWAY;
    }

    /** True while the one in front still waits for the other to come round: not yet behind, and not waited {@link #HOLD_TICKS}. */
    public static boolean holding(boolean otherBehind, long waited) {
        return !otherBehind && waited < HOLD_TICKS;
    }

    /** "Never", on an investigator's 32-bit clocks (its entity data keeps the low 32 bits of the game tick). */
    public static final int NEVER = Integer.MIN_VALUE;

    /** Ticks since {@code at} (a 32-bit clock) on the game clock {@code now}: wraps safely; "never" is very long ago. */
    public static long since(long now, int at) {
        return at == NEVER ? Long.MAX_VALUE / 4 : (long)((int)now - at);
    }

    /** True if a pair member is lost: its partner gone this long (ticks), with no player near to see it go. */
    public static final long PARTNER_LOST = 400L;
}
