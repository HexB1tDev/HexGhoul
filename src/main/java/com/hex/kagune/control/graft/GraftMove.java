package com.hex.kagune.control.graft;

import java.util.ArrayList;
import java.util.List;

/**
 * Every move of Graft DNA (pure, tested; 7.0.0). A move is a timeline of {@link Beat}s, one per limb strike: when the limb starts
 * winding up, the tick its blow lands (the server hurts on that tick, the client draws the limb arriving on it), and when it
 * lets go and settles back. The same beats drive the server's hits and every client's animation, so what is seen is what hits.
 *
 * <p>M1 is each stage's own five-blow string (the kakuja's six, its claws and jaws with its colossi); 7.0.2, the user's rework: the
 * abilities are four a stage on the ability keys (1 to 4: the original's Z, J, V, B; {@link #of}), with the moves that follow on
 * from them ({@link #followUp}) and the jobs that run alongside whatever else he does ({@link #job}). Right click is the dash.
 */
public enum GraftMove {
    NONE("", Slot.NONE, GraftStage.TRANSPLANT, 0, 0, 0.0F, 0),
    // --- Rize's Kakuhou: the string ---------------------------------------------------------------------------------------
    LASH_1("Rising Lash", Slot.M1, GraftStage.TRANSPLANT, 0, 9, 0.4F, 0),
    LASH_2("Backhand Lash", Slot.M1, GraftStage.TRANSPLANT, 0, 9, 0.4F, 0),
    LASH_3("Twin Skewer", Slot.M1, GraftStage.TRANSPLANT, 0, 11, 0.5F, 0),
    LASH_4("Gutting Upper", Slot.M1, GraftStage.TRANSPLANT, 0, 11, 0.5F, 0),
    LASH_5("Guillotine", Slot.M1, GraftStage.TRANSPLANT, 0, 15, 0.7F, 0),
    /** M1 with a sword or axe in hand: his own blade, then the kagune pierces where it cut. */
    ACCORD("Blade and Kagune", Slot.M1, GraftStage.TRANSPLANT, 0, 9, 0.3F, 0),
    STEP("Kagune Step", Slot.M2, GraftStage.TRANSPLANT, 0, 8, 1.0F, 14),
    // --- the centipedes: the string ---------------------------------------------------------------------------------------
    RIP_1("Hook Rip", Slot.M1, GraftStage.CENTIPEDE, 250, 8, 0.5F, 0),
    RIP_2("Hook Rip", Slot.M1, GraftStage.CENTIPEDE, 250, 8, 0.5F, 0),
    RIP_3("Snapping Lunge", Slot.M1, GraftStage.CENTIPEDE, 250, 11, 0.6F, 0),
    RIP_4("Spiked Impale", Slot.M1, GraftStage.CENTIPEDE, 250, 11, 0.6F, 0),
    RIP_5("Centipede Crash", Slot.M1, GraftStage.CENTIPEDE, 250, 15, 0.8F, 0),
    // --- four thorns: the string ------------------------------------------------------------------------------------------
    THORN_1("Thorn Sweep", Slot.M1, GraftStage.THORNS, 450, 10, 0.6F, 0),
    THORN_2("Thorn Sweep", Slot.M1, GraftStage.THORNS, 450, 10, 0.6F, 0),
    THORN_3("Cross Cleave", Slot.M1, GraftStage.THORNS, 450, 12, 0.7F, 0),
    THORN_4("Fourfold Pierce", Slot.M1, GraftStage.THORNS, 450, 12, 0.7F, 0),
    THORN_5("Quadruple Crush", Slot.M1, GraftStage.THORNS, 450, 16, 0.9F, 0),
    // --- the many-eyed kagune: the string ---------------------------------------------------------------------------------
    CRESCENT_1("Crescent", Slot.M1, GraftStage.DRAGON, 700, 10, 0.7F, 0),
    CRESCENT_2("Crescent", Slot.M1, GraftStage.DRAGON, 700, 10, 0.7F, 0),
    CRESCENT_3("Staring Barrage", Slot.M1, GraftStage.DRAGON, 700, 15, 0.8F, 0),
    CRESCENT_4("Crescent Jaws", Slot.M1, GraftStage.DRAGON, 700, 12, 0.8F, 0),
    CRESCENT_5("Crescent Cyclone", Slot.M1, GraftStage.DRAGON, 700, 18, 1.0F, 0),
    // --- the kakuja: the string, claws and jaws with the colossi ----------------------------------------------------------
    CLAW_1("Rake", Slot.M1, GraftStage.KAKUJA, 900, 7, 0.5F, 0),
    CLAW_2("Rake", Slot.M1, GraftStage.KAKUJA, 900, 7, 0.5F, 0),
    CLAW_3("Centipede Strike", Slot.M1, GraftStage.KAKUJA, 900, 10, 0.7F, 0),
    CLAW_4("Double Rake", Slot.M1, GraftStage.KAKUJA, 900, 10, 0.7F, 0),
    CLAW_5("Devouring Lunge", Slot.M1, GraftStage.KAKUJA, 900, 11, 0.8F, 0),
    CLAW_6("Predator's Finish", Slot.M1, GraftStage.KAKUJA, 900, 16, 1.0F, 0),
    // --- 7.0.2, the user's rework: the two tails' four (Rize's Kakuhou). The vault and the execution work with one tail; the
    // counter and the puppet need both (the second tail grows at mastery 100).
    /** Open, the tails drawn low and poised: whoever strikes at him now is countered ({@link #MOCKINGBIRD_COUNTER}). */
    MOCKINGBIRD("Mockingbird", Slot.Z, GraftStage.TRANSPLANT, 100, 30, 1.6F, 120),
    /** Struck at in the stance: he slips aside, one tail hooks the attacker, the other comes round and runs it through from behind. */
    MOCKINGBIRD_COUNTER("Mockingbird", Slot.NONE, GraftStage.TRANSPLANT, 100, 16, 0.0F, 0),
    /** In on it, then cut after cut as he circles it, the tails switching angles; both driven through it at the end. */
    OWLS_EXECUTION("Owl's Execution", Slot.J, GraftStage.TRANSPLANT, 40, 32, 3.0F, 140),
    /** Both tails stabbed into the ground behind him: he is thrown high ({@link #VAULT_DIVE} on the key again in the air). */
    SCORPION_VAULT("Scorpion Vault", Slot.V, GraftStage.TRANSPLANT, 0, 8, 2.0F, 100),
    /** Down from the vault where he aims, both tails ahead like spears: the impact stuns. */
    VAULT_DIVE("Scorpion Vault", Slot.NONE, GraftStage.TRANSPLANT, 0, 30, 0.6F, 0),
    /** One tail catches it and drags it in, stunned; then his choice: the key again, M1 or right click. */
    HUMAN_PUPPET("Human Puppet", Slot.B, GraftStage.TRANSPLANT, 180, 50, 2.6F, 160),
    /** The puppet hauled up and hurled. */
    PUPPET_THROW("Human Puppet", Slot.NONE, GraftStage.TRANSPLANT, 180, 12, 0.0F, 0),
    /** The puppet held while the second tail rips up through it. */
    PUPPET_STRIKE("Human Puppet", Slot.NONE, GraftStage.TRANSPLANT, 180, 12, 0.0F, 0),
    /** He hauls himself in to the pinned puppet: his claws, then a tail through it. */
    PUPPET_PULL("Human Puppet", Slot.NONE, GraftStage.TRANSPLANT, 180, 18, 0.0F, 0),
    // --- 7.0.2, the user's rework: the centipedes' four (two centipedes and the mouth) ------------------------------------------
    /** The side centipedes feint at its front; the mouth comes round under it and snaps up through its guard. */
    AMBUSH("The Ambush", Slot.V, GraftStage.CENTIPEDE, 250, 16, 2.2F, 120),
    /** The centipedes plant ahead in turn and haul him along, steered where he looks ({@link #job}: he fights as he goes). */
    LOCOMOTION("Centipede Locomotion", Slot.J, GraftStage.CENTIPEDE, 280, 40, 2.0F, 150),
    /** The crawl's end: the mouth lunges and bites what is ahead. */
    CRAWL_BITE("Centipede Locomotion", Slot.NONE, GraftStage.CENTIPEDE, 280, 12, 0.0F, 0),
    /** The mouth goes after its prey, round what is in the way, snapping; caught, it is clamped ({@link #MAW_DRAG}). */
    HUNGRY_MAW("Hungry Maw", Slot.Z, GraftStage.CENTIPEDE, 320, 30, 2.4F, 160),
    /** Clamped in the mouth and dragged along wherever he goes while the centipedes fight on ({@link #job}; the key again flings it). */
    MAW_DRAG("Hungry Maw", Slot.NONE, GraftStage.CENTIPEDE, 320, 100, 0.0F, 0),
    /** The centipedes coiled round him: a blow at him springs them out ({@link #CARAPACE_SPRING}). */
    CARAPACE("Living Carapace", Slot.B, GraftStage.CENTIPEDE, 380, 60, 2.0F, 200),
    /** Struck at in the carapace: the coils spring out at the attacker; the mouth is free to bite after (M1 or the key). */
    CARAPACE_SPRING("Living Carapace", Slot.NONE, GraftStage.CENTIPEDE, 380, 12, 0.0F, 0),
    /** The mouth's bite after the spring. */
    CARAPACE_BITE("Living Carapace", Slot.NONE, GraftStage.CENTIPEDE, 380, 10, 0.0F, 0),
    // --- 7.0.2, the user's rework: the four thorns' four ------------------------------------------------------------------------
    /** The upper pair catch it from either side and hold it up; the lower pair run it through, again and again; then it is hurled. */
    CRUCIFIXION("Crucifixion", Slot.Z, GraftStage.THORNS, 450, 42, 3.0F, 160),
    /** Thrown forward, the four spread ahead of him smashing what stands in the way and running through what lives. */
    BREACH("Unstoppable Breach", Slot.J, GraftStage.THORNS, 500, 26, 3.2F, 160),
    /** The lower pair far out to either side, swept in, dragging what they pass to the middle; the upper pair come down on it all. */
    DRAGNET("Dragon's Dragnet", Slot.V, GraftStage.THORNS, 560, 30, 3.0F, 180),
    /** All four planted, and he is flung high ({@link #CATAPULT_FLIGHT}: steered where he looks; {@link #CATAPULT_LAND} where he comes down). */
    CATAPULT("Living Catapult", Slot.B, GraftStage.THORNS, 620, 10, 3.5F, 220),
    /** The catapult's flight (a job): splayed, reaching for the ground as he falls. */
    CATAPULT_FLIGHT("Living Catapult", Slot.NONE, GraftStage.THORNS, 620, 140, 0.0F, 0),
    /** Down: the four driven into the ground round him one after another. */
    CATAPULT_LAND("Living Catapult", Slot.NONE, GraftStage.THORNS, 620, 12, 0.0F, 0),
    // --- 7.0.2, the user's rework: the mass of eyes' four ------------------------------------------------------------------------
    /** The mass grown wide and wheeling round him, every crescent on its own path, cutting what it passes. */
    STORM("Eye of the Storm", Slot.Z, GraftStage.DRAGON, 700, 60, 4.0F, 200),
    /** Drawn in round him, flung out all round, spikes bursting along it in waves, drawn in again. */
    BLOOM("Flesh Bloom", Slot.J, GraftStage.DRAGON, 740, 44, 4.0F, 200),
    /** Raised into a wall of flesh before him, then folded forward onto what is under it, its ends curled round the sides. */
    CURTAIN("Black Curtain", Slot.V, GraftStage.DRAGON, 780, 40, 4.0F, 220),
    /** The mass after one body, at it from every side ({@link #job}: he goes where he likes; a sword's M1 rushes in on it, bleeding). */
    KINGS_GAME("The King's Game", Slot.B, GraftStage.DRAGON, 820, 100, 4.0F, 260),
    /** His sword's blow in the King's Game, a crescent driven in with it. */
    KINGS_STRIKE("The King's Game", Slot.NONE, GraftStage.DRAGON, 820, 8, 0.0F, 0),
    // --- 7.0.2, the user's rework: the final kakuja's four ------------------------------------------------------------------------
    /** One colossus at its front, the other round behind it, hooking it; he charges in: claws, jaws, a colossus, a last blow. */
    CHECKMATE("King's Checkmate", Slot.Z, GraftStage.KAKUJA, 900, 38, 5.0F, 200),
    /** Hurled after it, one colossus biting at it as he closes, the other striding him on; caught, it is dragged ({@link #HUNT_DRAG}). */
    HUNT("Centipede Hunt", Slot.J, GraftStage.KAKUJA, 920, 40, 5.0F, 220),
    /** Caught in the hunt: dragged across the ground as he runs on, bleeding, then thrown. */
    HUNT_DRAG("Centipede Hunt", Slot.NONE, GraftStage.KAKUJA, 920, 24, 0.0F, 0),
    /** Curled round him healing fast for much RC ({@link #job}: he can move, not strike; the key again stops it). */
    RECOVERY("Monstrous Recovery", Slot.V, GraftStage.KAKUJA, 940, 80, 2.0F, 600),
    /** The colossi at whatever he looks at from every side ({@link #job}), his claws at it as fast as he clicks; the key again: the end. */
    PERFORMANCE("The Final Performance", Slot.B, GraftStage.KAKUJA, 980, 100, 8.0F, 900),
    /** His claws in the performance: one rake, either hand. */
    PERF_CLAW("The Final Performance", Slot.NONE, GraftStage.KAKUJA, 980, 5, 0.0F, 0),
    /** The performance's end: caught by one colossus, hauled to him, the other through it and his last blow. */
    PERF_FINISH("The Final Performance", Slot.NONE, GraftStage.KAKUJA, 980, 26, 0.0F, 0);


    /** Which key a move answers to. */
    public enum Slot {
        NONE, M1, M2, Z, J, V, B, N, H, G, C
    }

    /** What a limb does in a beat. */
    public enum Kind {
        /** Coiled back, then shot straight at the point like a spear, held, drawn back. */
        THRUST,
        /** Out at {@code reach}, swept round from {@code from} to {@code to} degrees at {@code height}. */
        SWEEP,
        /** Reared up overhead, then driven down into the ground at the point. */
        SLAM,
        /** Reared back, the mouth gaping, then snapped shut on the point. */
        BITE,
        /** Shot at the target and closed round it; it is carried at the limb's end until let go. */
        SEIZE,
        /** Every limb out round him, spun from {@code from} to {@code to} degrees. */
        SPIN,
        /** Curled round his own body, a living shield. */
        CURL,
        /** Out of the curl, flung straight out all round. */
        BURST,
        /** Reared back over the shoulder and whipped forward: what it held flies. */
        WHIP,
        /** Planted on the ground behind him and pushing: a dash's thrust. */
        PLANT,
        /** His own arm, clawing (the limbs only ready themselves). */
        CLAW,
        /** Driven low under the point, then ripped up through it: what it catches is thrown into the air. */
        UPPER,
        /** 7.0.2: drawn low behind him, the points raised over his shoulders like a scorpion's: poised (Mockingbird's stance). */
        LURK,
        /** 7.0.2: shot past the side of what struck at him and wrapped round its back: hooked, held where it stands. */
        HOOK,
        /** 7.0.2: curved out round the side of its point and driven into it from behind. */
        FLANK,
        /** 7.0.2: both out ahead of him along his dive like spears (the impact is the server's, where he comes down). */
        DIVE,
        /** 7.0.2: the mouth sent after its prey along a route round what is in the way (by its point), snapping as it goes. */
        CHASE,
        /** 7.0.2: its jaws shut on what it caught, dragging it along at his side. */
        CLAMP,
        /** 7.0.2: a crawling stride: reached ahead to his side, planted, and pulled past as it hauls him on. */
        CRAWL,
        /** 7.0.2: splayed out round him in the air like a spider's legs, reaching down for the ground as he falls. */
        SOAR,
        /** 7.0.2: out round him, each on its own wheeling, wandering path (Eye of the Storm, {@link GraftRules#stormAngle}). */
        STORM,
        /** 7.0.2: raised into a wall across his front, folded forward onto the ground ahead, its ends curling round the sides. */
        CURTAIN
    }

    /** Flags on a beat. */
    public static final int BLEED = 1;
    public static final int STUN = 2;
    public static final int PIERCE = 4;
    public static final int CRATER = 8;
    /** Each limb of the beat goes after its own target ({@link GraftRules#targets}). */
    public static final int MULTI = 16;
    public static final int KNOCK_UP = 32;
    /** Spikes stand out on the limb (its thorns are the edge). */
    public static final int SPIKED = 64;
    /** Heavy: a long stun and a big knock. */
    public static final int HEAVY = 128;
    /** 7.0.2: breaks through defences (a raised shield is knocked down before it lands). */
    public static final int BREAK = 256;
    /** 7.0.2: keeps holding what an earlier move of the same hold caught (no new catch). */
    public static final int CARRY = 512;
    /** 7.0.2: a sweep that drags what it passes in to the move's point instead of throwing it (Dragon's Dragnet). */
    public static final int GATHER = 1024;

    /**
     * One limb's strike. {@code limb} indexes the stage's limbs ({@link #ALL} every one, {@link #ARM} his own arm); ticks are
     * from the move's start: {@code start} the wind-up begins, {@code land} the blow lands (a sweep or spin sweeps from there to
     * {@code end}), {@code end} it lets go. {@code reach} in blocks; {@code from} and {@code to} in degrees from the way the move
     * faces, plus to his right; {@code height} above his feet; {@code power} the blow's share of a full hit.
     */
    public record Beat(int limb, Kind kind, int start, int land, int end, double reach, double from, double to, double height, float power,
                       int flags) {
        public boolean has(int flag) {
            return (this.flags & flag) != 0;
        }

        /** True while this beat holds its limb at {@code age} (wind-up through to letting go). */
        public boolean holds(double age) {
            return age >= this.start && age <= this.end;
        }

        /** 0..1 through the wind-up at {@code age}. */
        public double windup(double age) {
            return this.land <= this.start ? 1.0 : clamp01((age - this.start) / (double)(this.land - this.start));
        }

        /** 0..1 from the blow landing to letting go at {@code age}. */
        public double through(double age) {
            return this.end <= this.land ? 1.0 : clamp01((age - this.land) / (double)(this.end - this.land));
        }

        public boolean involves(int limb) {
            return this.limb == ALL || this.limb == limb;
        }
    }

    public static final int ALL = -1;
    public static final int ARM = -2;

    private final String displayName;
    private final Slot slot;
    private final GraftStage stage;
    private final int mastery;
    private final int ticks;
    private final float cost;
    private final int cooldown;

    GraftMove(String displayName, Slot slot, GraftStage stage, int mastery, int ticks, float cost, int cooldown) {
        this.displayName = displayName;
        this.slot = slot;
        this.stage = stage;
        this.mastery = mastery;
        this.ticks = ticks;
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public String displayName() {
        return this.displayName;
    }

    public Slot slot() {
        return this.slot;
    }

    /** The stage the move belongs to (it is the first it can be used in). */
    public GraftStage stage() {
        return this.stage;
    }

    /** The mastery it unlocks at. */
    public int mastery() {
        return this.mastery;
    }

    /** How long it lasts (ticks; a hold is let go at the latest then). */
    public int ticks() {
        return this.ticks;
    }

    /** Its RC cost (the original's scale, before mastery and the limbs out). */
    public float cost() {
        return this.cost;
    }

    /** Ticks before its key is heard again, counted from its start. */
    public int cooldown() {
        return this.cooldown;
    }

    /** True for the M1 strings (a click within the chain window goes on to the next). */
    public boolean combo() {
        return this.slot == Slot.M1 && this != ACCORD;
    }

    /** True for the moves that carry a body (the puppet and what follows it, the counter's hook, the hunt's drag, the performance's end). */
    public boolean holds() {
        return this == HUMAN_PUPPET || this == PUPPET_THROW || this == PUPPET_STRIKE
            || this == PUPPET_PULL || this == MOCKINGBIRD_COUNTER || this == HUNT_DRAG || this == PERF_FINISH;
    }

    /** 7.0.2: true for a move only ever started by another (a counter, a dive, a puppet's follow-up): no key of its own. */
    public boolean followUp() {
        return this != NONE && this.slot == Slot.NONE;
    }

    /**
     * 7.0.2: true for the moves whose point is their target wherever it goes: the server strikes at its middle each tick, and every
     * client draws the limbs at it there too (not where it stood when the move began).
     */
    public boolean follows() {
        return this == MOCKINGBIRD_COUNTER || this == OWLS_EXECUTION || this == HUMAN_PUPPET || this == PUPPET_STRIKE || this == PUPPET_PULL
            || this == AMBUSH || this == CARAPACE_SPRING || this == CARAPACE_BITE || this == CRUCIFIXION || this == KINGS_STRIKE || this == CHECKMATE
            || this == HUNT || this == PERF_CLAW || this == PERF_FINISH;
    }

    /** 7.0.2: what the move's key does beyond starting it, for the ability panel ("" for nothing). */
    public String hint() {
        return switch (this) {
            case MOCKINGBIRD -> "counters";
            case SCORPION_VAULT -> "again: dive";
            case HUMAN_PUPPET -> "again, M1 or RMB";
            case HUNGRY_MAW -> "again: fling it";
            case LOCOMOTION -> "again: bite";
            case CARAPACE -> "counters, then M1: bite";
            case CATAPULT -> "steer where you look";
            case KINGS_GAME -> "a sword's M1 rushes in";
            case RECOVERY -> "again: stop";
            case PERFORMANCE -> "M1 claws, again: finish";
            default -> "";
        };
    }

    /** True for the guards (incoming blows are caught while they hold). */
    public boolean guards() {
        return this == CARAPACE;
    }

    /**
     * 7.0.2: true for the moves that run alongside whatever else he does (the crawl, the mouth dragging what it caught): they keep
     * their own clock, every other move goes on over them, and the limbs they hold are theirs until a move's own beat takes one (a
     * held body's limb stays with it).
     */
    public boolean job() {
        return this == LOCOMOTION || this == MAW_DRAG || this == CATAPULT_FLIGHT || this == KINGS_GAME || this == RECOVERY || this == PERFORMANCE;
    }

    /** 7.0.2: true for the moves whose limb goes after its target by its own route (the move's point is the bend in it). */
    public boolean chases() {
        return this == HUNGRY_MAW;
    }

    /** True for the moves that throw him along (the server moves him: they keep the limbs' plant in step with it). */
    public boolean travels() {
        return this == STEP || this == BREACH || this == HUNT;
    }

    /** The first move of the M1 string in {@code stage}. */
    public static GraftMove firstStrike(GraftStage stage) {
        return switch (stage) {
            case TRANSPLANT -> LASH_1;
            case CENTIPEDE -> RIP_1;
            case THORNS -> THORN_1;
            case DRAGON -> CRESCENT_1;
            case KAKUJA -> CLAW_1;
        };
    }

    /** The next strike after {@code last} in its string (the finisher goes back to the first). */
    public static GraftMove nextStrike(GraftMove last, GraftStage stage) {
        GraftMove first = firstStrike(stage);
        if (last == null || last.stage != first.stage || !last.combo()) {
            return first;
        }
        GraftMove next = values()[last.ordinal() + 1];
        return next.combo() && next.stage == first.stage ? next : first;
    }

    /** The move a key starts in {@code stage} (NONE if the key does nothing there). */
    public static GraftMove of(Slot slot, GraftStage stage) {
        if (slot == Slot.M1) {
            return firstStrike(stage);
        }
        if (slot == Slot.M2) {
            return STEP;
        }
        return switch (stage) {
            // 7.0.2: the two tails' own four (the user's rework); the centipede and the thorns keep the old six until theirs come.
            case TRANSPLANT -> switch (slot) {
                case Z -> MOCKINGBIRD;
                case J -> OWLS_EXECUTION;
                case V -> SCORPION_VAULT;
                case B -> HUMAN_PUPPET;
                default -> NONE;
            };
            case CENTIPEDE -> switch (slot) {
                case Z -> HUNGRY_MAW;
                case J -> LOCOMOTION;
                case V -> AMBUSH;
                case B -> CARAPACE;
                default -> NONE;
            };
            case THORNS -> switch (slot) {
                case Z -> CRUCIFIXION;
                case J -> BREACH;
                case V -> DRAGNET;
                case B -> CATAPULT;
                default -> NONE;
            };
            case DRAGON -> switch (slot) {
                case Z -> STORM;
                case J -> BLOOM;
                case V -> CURTAIN;
                case B -> KINGS_GAME;
                default -> NONE;
            };
            case KAKUJA -> switch (slot) {
                case Z -> CHECKMATE;
                case J -> HUNT;
                case V -> RECOVERY;
                case B -> PERFORMANCE;
                default -> NONE;
            };
        };
    }

    /** Every move a player can learn, in the order they unlock (the unlock track; the strings, the toss and NONE left out). */
    public static List<GraftMove> track() {
        List<GraftMove> out = new ArrayList<>();
        for (GraftMove move : values()) {
            if (move == NONE || move == ACCORD || move.followUp() || move.combo() && move != firstStrike(move.stage)) {
                continue;
            }
            out.add(move);
        }
        out.sort(java.util.Comparator.comparingInt(GraftMove::mastery));
        return out;
    }

    public static GraftMove byOrdinal(int ordinal) {
        GraftMove[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    /**
     * The beats of this move for a kagune of {@code limbs} limbs (its stage's count). Limb indices are into the stage's limbs: on
     * the centipede stage 0 is the one with the mouth; on the thorns 0 and 1 are the upper pair; in the kakuja 0 is the right
     * centipede and 1 the left.
     */
    public List<Beat> beats(int limbs) {
        int n = Math.max(1, limbs);
        int other = n > 1 ? 1 : 0;
        List<Beat> b = new ArrayList<>();
        int third = Math.min(2, n - 1);
        switch (this) {
            // Rize's Kakuhou: a rising lash, the other tail backhanded high, both skewering, one ripped up under it, both brought
            // down on it like a guillotine.
            case LASH_1 -> b.add(new Beat(0, Kind.SWEEP, 0, 3, 7, 4.6, 75.0, -70.0, 0.9, 1.0F, STUN));
            case LASH_2 -> b.add(new Beat(other, Kind.SWEEP, 0, 3, 7, 4.6, -80.0, 75.0, 1.9, 1.0F, STUN));
            case LASH_3 -> {
                b.add(new Beat(0, Kind.THRUST, 0, 4, 8, 5.4, 6.0, 6.0, 1.3, n > 1 ? 0.75F : 1.3F, STUN | BLEED));
                if (n > 1) {
                    b.add(new Beat(1, Kind.THRUST, 1, 5, 9, 5.4, -6.0, -6.0, 1.3, 0.75F, STUN | BLEED));
                }
            }
            case LASH_4 -> b.add(new Beat(0, Kind.UPPER, 0, 4, 9, 5.0, 0.0, 0.0, 1.2, 1.2F, KNOCK_UP | STUN | BLEED));
            case LASH_5 -> b.add(new Beat(ALL, Kind.SLAM, 0, 8, 13, 5.0, 0.0, 0.0, 0.0, 1.5F, CRATER | HEAVY | STUN | BLEED));
            case ACCORD -> b.add(new Beat(0, Kind.THRUST, 0, 3, 6, 4.6, 6.0, 6.0, 1.3, 0.5F, 0));
            case STEP -> b.add(new Beat(ALL, Kind.PLANT, 0, 1, 6, 2.6, 0.0, 0.0, 0.0, 0.0F, 0));
            // The centipedes: the flank ones hook in from either side, the mouth lunges and snaps, both flank ones run it through,
            // the mouth rips it up and the flank ones crash down on it.
            case RIP_1 -> b.add(new Beat(1, Kind.SWEEP, 0, 3, 7, 5.2, 85.0, -50.0, 1.2, 1.0F, STUN | SPIKED | BLEED));
            case RIP_2 -> b.add(new Beat(third, Kind.SWEEP, 0, 3, 7, 5.2, -85.0, 50.0, 1.6, 1.0F, STUN | SPIKED | BLEED));
            case RIP_3 -> b.add(new Beat(0, Kind.BITE, 0, 5, 10, 7.0, 0.0, 0.0, 1.4, 1.5F, BLEED | STUN));
            case RIP_4 -> {
                b.add(new Beat(1, Kind.THRUST, 0, 4, 8, 7.0, 8.0, 8.0, 1.3, 0.8F, PIERCE | SPIKED | BLEED | STUN));
                b.add(new Beat(third, Kind.THRUST, 1, 5, 9, 7.0, -8.0, -8.0, 1.3, 0.8F, PIERCE | SPIKED | BLEED | STUN));
            }
            case RIP_5 -> {
                b.add(new Beat(0, Kind.UPPER, 0, 3, 7, 6.0, 0.0, 0.0, 1.3, 1.0F, KNOCK_UP | STUN | BLEED));
                b.add(new Beat(1, Kind.SLAM, 3, 10, 14, 4.8, 12.0, 0.0, 0.0, 1.0F, CRATER | HEAVY | STUN | SPIKED));
                b.add(new Beat(third, Kind.SLAM, 4, 11, 15, 4.8, -12.0, 0.0, 0.0, 1.0F, CRATER | HEAVY | STUN | SPIKED));
            }
            // The thorns: a pair sweeps from one side, the other pair from the other, the upper pair cleaves crossed, all four run
            // it through one after another, all four crush it.
            case THORN_1 -> {
                b.add(new Beat(0, Kind.SWEEP, 0, 4, 8, 6.5, 85.0, -60.0, 2.0, 1.0F, STUN | SPIKED));
                b.add(new Beat(Math.min(2, n - 1), Kind.SWEEP, 1, 5, 9, 6.5, 70.0, -50.0, 0.8, 0.8F, STUN | SPIKED));
            }
            case THORN_2 -> {
                b.add(new Beat(other, Kind.SWEEP, 0, 4, 8, 6.5, -85.0, 60.0, 2.0, 1.0F, STUN | SPIKED));
                b.add(new Beat(Math.min(3, n - 1), Kind.SWEEP, 1, 5, 9, 6.5, -70.0, 50.0, 0.8, 0.8F, STUN | SPIKED));
            }
            case THORN_3 -> {
                b.add(new Beat(0, Kind.SWEEP, 0, 5, 10, 7.0, 100.0, -45.0, 2.3, 1.1F, HEAVY | SPIKED | BLEED));
                b.add(new Beat(other, Kind.SWEEP, 1, 6, 11, 7.0, -100.0, 45.0, 1.3, 1.1F, HEAVY | SPIKED | BLEED));
            }
            case THORN_4 -> {
                for (int i = 0; i < n; i++) {
                    b.add(new Beat(i, Kind.THRUST, i, 4 + i, 7 + i, 7.5, (i % 2 == 0 ? 9.0 : -9.0) * (1 + i / 2), 0.0, 1.3, 0.6F,
                        PIERCE | SPIKED | STUN));
                }
            }
            case THORN_5 -> b.add(new Beat(ALL, Kind.SLAM, 0, 8, 12, 5.5, 0.0, 0.0, 0.0, 1.5F, CRATER | KNOCK_UP | STUN | HEAVY | SPIKED));
            // The many-eyed kagune (its stack towering on his right): the top layers sweep across, the low ones sweep back, every
            // layer stabs in turn, the stack closes on it like jaws, they spin round him.
            case CRESCENT_1 -> {
                b.add(new Beat(0, Kind.SWEEP, 0, 4, 8, 7.5, 95.0, -55.0, 2.6, 1.0F, STUN | SPIKED));
                b.add(new Beat(Math.min(1, n - 1), Kind.SWEEP, 1, 5, 9, 7.5, 80.0, -45.0, 1.5, 0.8F, STUN | SPIKED));
            }
            case CRESCENT_2 -> {
                b.add(new Beat(Math.min(3, n - 1), Kind.SWEEP, 0, 4, 8, 7.5, -95.0, 55.0, 1.9, 1.0F, STUN | SPIKED));
                b.add(new Beat(Math.min(4, n - 1), Kind.SWEEP, 1, 5, 9, 7.5, -80.0, 45.0, 0.9, 0.8F, STUN | SPIKED));
            }
            case CRESCENT_3 -> {
                for (int i = 0; i < n; i++) {
                    int land = 3 + 2 * i;
                    b.add(new Beat(i, Kind.THRUST, land - 3, land, land + 2, 9.0, (i - (n - 1) * 0.5) * 7.0, 0.0, 1.3, 0.45F, STUN | SPIKED));
                }
            }
            case CRESCENT_4 -> {
                // The jaws: the top layers come down on it, the middle one runs straight in, the lowest rip up from under it, all
                // closing on the one tick.
                for (int i = 0; i < n; i++) {
                    double layer = n == 1 ? 0.5 : i / (double)(n - 1);
                    Kind kind = layer < 0.35 ? Kind.SLAM : layer > 0.65 ? Kind.UPPER : Kind.THRUST;
                    b.add(new Beat(i, kind, 0, 6, 10, 7.5, 0.0, 0.0, 1.2, kind == Kind.THRUST ? 0.5F : 0.35F, HEAVY | STUN | SPIKED | BLEED));
                }
            }
            case CRESCENT_5 -> b.add(new Beat(ALL, Kind.SPIN, 2, 5, 17, 7.0, 0.0, 720.0, 1.2, 0.9F, BLEED | STUN | SPIKED));
            // The kakuja: every blow his and his colossi's together. He rakes and the colossus on that side snaps at it a beat later,
            // rakes the other way and the other snaps; a colossus spears it; both claws and a colossus sweep; his jaws lunge with
            // both colossi biting beside them; the colossi crash down on it.
            case CLAW_1 -> {
                b.add(new Beat(ARM, Kind.CLAW, 0, 2, 4, 3.4, 45.0, -45.0, 1.4, 0.5F, STUN));
                b.add(new Beat(0, Kind.BITE, 0, 4, 7, 7.0, 6.0, 6.0, 1.4, 0.45F, STUN | BLEED));
            }
            case CLAW_2 -> {
                b.add(new Beat(ARM, Kind.CLAW, 0, 2, 4, 3.4, -45.0, 45.0, 1.4, 0.5F, STUN));
                b.add(new Beat(other, Kind.BITE, 0, 4, 7, 7.0, -6.0, -6.0, 1.4, 0.45F, STUN | BLEED));
            }
            case CLAW_3 -> b.add(new Beat(0, Kind.THRUST, 0, 4, 8, 9.0, 8.0, 8.0, 1.4, 1.0F, BLEED | STUN | SPIKED));
            case CLAW_4 -> {
                b.add(new Beat(ARM, Kind.CLAW, 0, 2, 4, 3.4, 40.0, -40.0, 1.4, 0.5F, STUN));
                b.add(new Beat(ARM, Kind.CLAW, 3, 5, 7, 3.4, -40.0, 40.0, 1.4, 0.5F, STUN));
                b.add(new Beat(other, Kind.SWEEP, 1, 5, 9, 7.5, -75.0, 75.0, 1.5, 0.7F, SPIKED | STUN));
            }
            case CLAW_5 -> {
                b.add(new Beat(ARM, Kind.BITE, 0, 5, 9, 3.8, 0.0, 0.0, 1.6, 0.85F, BLEED | STUN));
                b.add(new Beat(0, Kind.BITE, 0, 5, 9, 8.0, 10.0, 10.0, 1.4, 0.4F, BLEED | STUN));
                b.add(new Beat(other, Kind.BITE, 1, 6, 10, 8.0, -10.0, -10.0, 1.4, 0.4F, BLEED | STUN));
            }
            case CLAW_6 -> {
                b.add(new Beat(ARM, Kind.CLAW, 0, 2, 4, 3.6, 0.0, 0.0, 1.4, 0.6F, STUN));
                b.add(new Beat(ALL, Kind.SLAM, 2, 10, 15, 5.0, 0.0, 0.0, 0.0, 1.2F, CRATER | KNOCK_UP | HEAVY | BLEED | SPIKED));
            }
            // 7.0.2 Mockingbird: open, the tails low and poised; struck at, he slips aside, one tail hooks the attacker and the other
            // comes round it and runs it through from behind.
            case MOCKINGBIRD -> b.add(new Beat(ALL, Kind.LURK, 0, 4, 30, 2.0, 0.0, 0.0, 0.6, 0.0F, 0));
            case MOCKINGBIRD_COUNTER -> {
                b.add(new Beat(0, Kind.HOOK, 0, 3, 12, 6.0, 0.0, 0.0, 1.3, 0.4F, STUN));
                b.add(new Beat(other, Kind.FLANK, 1, 6, 10, 7.0, 0.0, 0.0, 1.3, 2.2F, STUN | HEAVY));
            }
            // Owl's Execution: in on it, then eight cuts as he circles it, the tails taking turns, each tail back and forth (forehand,
            // backhand) at a new height every time, one cut's end the next one's start; both driven through it together. One tail
            // alone makes every other cut.
            case OWLS_EXECUTION -> {
                double[][] cuts = {{75.0, -70.0, 1.7}, {-80.0, 70.0, 1.3}, {-65.0, 80.0, 0.8}, {65.0, -80.0, 2.0}, {85.0, -55.0, 2.2},
                    {-70.0, 60.0, 0.6}, {-60.0, 75.0, 1.2}, {70.0, -85.0, 1.6}};
                for (int i = 0; i < cuts.length; i++) {
                    if (n == 1 && i % 2 == 1) {
                        continue;
                    }
                    int land = 6 + 2 * i;
                    b.add(new Beat(i % 2 == 0 ? 0 : other, Kind.SWEEP, land - 1, land, land + 3, 4.0, cuts[i][0], cuts[i][1], cuts[i][2], 0.34F, BLEED));
                }
                b.add(new Beat(0, Kind.THRUST, 21, 27, 31, 4.6, n > 1 ? 5.0 : 0.0, 0.0, 1.3, 1.3F, BLEED | BREAK | STUN));
                if (n > 1) {
                    b.add(new Beat(other, Kind.THRUST, 23, 27, 31, 4.6, -5.0, 0.0, 1.3, 1.3F, BLEED | BREAK | STUN));
                }
            }
            // Scorpion Vault: both tails stabbed into the ground behind him, and he is thrown; the dive, both tails out ahead.
            case SCORPION_VAULT -> b.add(new Beat(ALL, Kind.PLANT, 0, 2, 8, 3.0, 0.0, 0.0, 0.0, 0.0F, 0));
            case VAULT_DIVE -> b.add(new Beat(ALL, Kind.DIVE, 0, 3, 30, 3.6, 0.0, 0.0, 0.0, 2.0F, STUN | HEAVY));
            // Human Puppet: caught and dragged in; then thrown, ripped by the other tail, or he hauls himself in on it.
            case HUMAN_PUPPET -> b.add(new Beat(0, Kind.SEIZE, 0, 5, 50, 9.0, 0.0, 0.0, 1.4, 0.5F, STUN));
            case PUPPET_THROW -> b.add(new Beat(0, Kind.WHIP, 0, 5, 9, 4.0, 0.0, 0.0, 1.4, 1.1F, STUN | HEAVY));
            case PUPPET_STRIKE -> {
                b.add(new Beat(0, Kind.SEIZE, 0, 0, 5, 9.0, 0.0, 0.0, 1.4, 0.0F, CARRY));
                b.add(new Beat(other, Kind.UPPER, 0, 4, 9, 4.0, 0.0, 0.0, 1.2, 1.8F, STUN | HEAVY | KNOCK_UP));
            }
            case PUPPET_PULL -> {
                b.add(new Beat(0, Kind.SEIZE, 0, 0, 16, 9.0, 0.0, 0.0, 1.4, 0.0F, CARRY));
                b.add(new Beat(ARM, Kind.CLAW, 4, 6, 8, 2.8, 45.0, -45.0, 1.4, 0.6F, STUN));
                b.add(new Beat(ARM, Kind.CLAW, 7, 9, 11, 2.8, -45.0, 45.0, 1.4, 0.6F, STUN));
                b.add(new Beat(other, Kind.THRUST, 9, 13, 16, 3.6, -6.0, -6.0, 1.3, 1.2F, STUN | HEAVY));
            }
            // 7.0.2 The Ambush: the side centipedes run at its front, plain to see; the mouth goes low round under it and snaps up
            // through it, through its guard.
            case AMBUSH -> {
                b.add(new Beat(Math.min(1, n - 1), Kind.THRUST, 0, 4, 8, 6.0, 12.0, 0.0, 1.3, 0.35F, STUN | SPIKED));
                b.add(new Beat(third, Kind.THRUST, 1, 5, 9, 6.0, -12.0, 0.0, 1.3, 0.35F, STUN | SPIKED));
                b.add(new Beat(0, Kind.UPPER, 2, 10, 15, 7.0, 0.0, 0.0, 1.2, 1.7F, STUN | HEAVY | KNOCK_UP | BREAK | BLEED));
            }
            // Centipede Locomotion: the side centipedes in turn, a stride every eight ticks (the server hauls him on each plant).
            case LOCOMOTION -> {
                for (int i = 0; i < 5; i++) {
                    int start = 8 * i;
                    b.add(new Beat(i % 2 == 0 ? Math.min(1, n - 1) : third, Kind.CRAWL, start, start + 2, Math.min(40, start + 12), 4.2, 0.0, 0.0, 0.0, 0.0F, 0));
                }
            }
            case CRAWL_BITE -> b.add(new Beat(0, Kind.BITE, 0, 5, 10, 7.0, 0.0, 0.0, 1.4, 1.6F, STUN | HEAVY | BLEED));
            // Hungry Maw: the mouth after its prey, snapping (the server bites what its head meets every few ticks); caught, dragged.
            case HUNGRY_MAW -> b.add(new Beat(0, Kind.CHASE, 0, 1, 30, 16.0, 0.0, 0.0, 1.3, 0.3F, BLEED | STUN));
            case MAW_DRAG -> b.add(new Beat(0, Kind.CLAMP, 0, 0, 100, 4.0, 0.0, 0.0, 0.6, 0.25F, BLEED | STUN | CARRY));
            // Living Carapace: the centipedes coiled round him (the mouth free); sprung out at what struck at him; the mouth's bite.
            case CARAPACE -> {
                b.add(new Beat(Math.min(1, n - 1), Kind.CURL, 0, 3, 58, 1.2, 0.0, 0.0, 1.2, 0.0F, 0));
                b.add(new Beat(third, Kind.CURL, 0, 3, 58, 1.2, 0.0, 0.0, 1.2, 0.0F, 0));
            }
            case CARAPACE_SPRING -> {
                b.add(new Beat(Math.min(1, n - 1), Kind.BURST, 0, 2, 10, 4.5, 0.0, 0.0, 1.2, 1.1F, STUN | HEAVY | SPIKED));
                b.add(new Beat(third, Kind.BURST, 0, 2, 10, 4.5, 0.0, 0.0, 1.2, 1.1F, STUN | HEAVY | SPIKED));
            }
            case CARAPACE_BITE -> b.add(new Beat(0, Kind.BITE, 0, 4, 9, 7.0, 0.0, 0.0, 1.4, 1.4F, BLEED | STUN));
            // 7.0.2 Crucifixion: the upper pair catch it from either side and hold it up; the lower pair run it through in turn from
            // new angles; then the upper pair hurl it.
            case CRUCIFIXION -> {
                int fourth = Math.min(3, n - 1);
                b.add(new Beat(0, Kind.SEIZE, 0, 6, 35, 7.5, 0.0, 0.0, 1.5, 0.5F, STUN));
                if (n > 1) {
                    b.add(new Beat(other, Kind.SEIZE, 0, 6, 35, 7.5, 0.0, 0.0, 1.5, 0.0F, CARRY));
                }
                double[] angles = {18.0, -24.0, 30.0, -18.0, 24.0, -30.0, 12.0, -12.0};
                for (int i = 0; i < angles.length; i++) {
                    int land = 10 + 3 * i;
                    b.add(new Beat(i % 2 == 0 ? third : fourth, Kind.THRUST, land - 3, land, land + 3, 6.5, angles[i], 0.0, 1.3, 0.32F,
                        BLEED | PIERCE | SPIKED | STUN));
                }
                b.add(new Beat(0, Kind.WHIP, 35, 37, 42, 5.0, 0.0, 0.0, 1.5, 1.0F, STUN | HEAVY));
                if (n > 1) {
                    b.add(new Beat(other, Kind.WHIP, 35, 37, 42, 5.0, 0.0, 0.0, 1.5, 0.0F, 0));
                }
            }
            // Unstoppable Breach: the four out ahead in turn as he is thrown on; each runs through what lives where it lands, or
            // smashes what stands there.
            case BREACH -> {
                double[] angles = {14.0, -14.0, 26.0, -26.0, 10.0, -10.0};
                for (int i = 0; i < angles.length; i++) {
                    int land = 4 + 3 * i;
                    b.add(new Beat(i % Math.max(1, Math.min(4, n)), Kind.THRUST, land - 3, land, land + 3, 5.0, angles[i], 0.0, 1.3, 0.5F,
                        BLEED | STUN | SPIKED | CRATER));
                }
            }
            // Dragon's Dragnet: the lower pair far out to either side and swept in, dragging what they pass; the upper pair come down
            // together on the middle.
            case DRAGNET -> {
                int fourth = Math.min(3, n - 1);
                b.add(new Beat(third, Kind.SWEEP, 2, 7, 13, 9.0, 115.0, 5.0, 0.9, 0.25F, STUN | GATHER | SPIKED));
                b.add(new Beat(fourth, Kind.SWEEP, 2, 7, 13, 9.0, -115.0, -5.0, 0.9, 0.25F, STUN | GATHER | SPIKED));
                b.add(new Beat(0, Kind.SLAM, 8, 18, 24, 4.0, 0.0, 0.0, 0.0, 1.6F, CRATER | HEAVY | STUN | SPIKED | BLEED));
                if (n > 1) {
                    b.add(new Beat(other, Kind.SLAM, 8, 18, 24, 4.0, 0.0, 0.0, 0.0, 1.6F, CRATER | HEAVY | STUN | SPIKED | BLEED));
                }
            }
            // Living Catapult: all four planted round him; the flight; the four into the ground round him one after another.
            case CATAPULT -> b.add(new Beat(ALL, Kind.PLANT, 0, 3, 10, 2.0, 0.0, 0.0, 0.0, 0.0F, 0));
            case CATAPULT_FLIGHT -> b.add(new Beat(ALL, Kind.SOAR, 0, 3, 140, 3.0, 0.0, 0.0, 0.0, 0.0F, 0));
            case CATAPULT_LAND -> {
                double[] angles = {40.0, -40.0, 140.0, -140.0};
                for (int i = 0; i < 4; i++) {
                    b.add(new Beat(Math.min(i, n - 1), Kind.SLAM, 0, 2 + 2 * i, 7 + 2 * i, 3.2, angles[i], 0.0, 0.0, 0.7F, STUN | CRATER | SPIKED));
                }
            }
            // 7.0.2 Eye of the Storm: every crescent out round him on its own path (the server cuts what each passes).
            case STORM -> b.add(new Beat(ALL, Kind.STORM, 0, 6, 56, 7.0, 0.0, 0.0, 1.5, 0.3F, BLEED | STUN | SPIKED));
            // Flesh Bloom: drawn in, flung out all round, spikes bursting along it in waves (the server's), drawn in again.
            case BLOOM -> {
                b.add(new Beat(ALL, Kind.CURL, 0, 5, 6, 1.6, 0.0, 0.0, 1.3, 0.0F, 0));
                b.add(new Beat(ALL, Kind.BURST, 6, 10, 36, 7.0, 0.0, 0.0, 1.4, 0.4F, BLEED | STUN | SPIKED));
                b.add(new Beat(ALL, Kind.CURL, 36, 40, 44, 1.6, 0.0, 0.0, 1.3, 0.0F, 0));
            }
            // Black Curtain: raised into a wall, folded forward onto what is under it (the server's), its ends curled round.
            case CURTAIN -> b.add(new Beat(ALL, Kind.CURTAIN, 0, 14, 38, 7.5, 0.0, 0.0, 0.0, 1.4F, STUN | HEAVY | SPIKED));
            // The King's Game: the crescents at it in turn from every side: straight in, round behind it, up from under, down on it.
            case KINGS_GAME -> {
                Kind[] ways = {Kind.THRUST, Kind.FLANK, Kind.UPPER, Kind.SLAM, Kind.FLANK, Kind.THRUST};
                for (int i = 0; i < 12; i++) {
                    int land = 6 + 7 * i;
                    Kind kind = ways[i % ways.length];
                    b.add(new Beat(i % Math.max(1, n), kind, land - 5, land, land + 2, 14.0, kind == Kind.THRUST ? (i % 4 < 2 ? 10.0 : -10.0) : 0.0, 0.0,
                        1.3, 0.3F, STUN | SPIKED));
                }
            }
            case KINGS_STRIKE -> b.add(new Beat(Math.min(2, n - 1), Kind.THRUST, 0, 3, 6, 9.0, 6.0, 0.0, 1.3, 0.6F, STUN | SPIKED | BLEED));
            // 7.0.2 King's Checkmate: one colossus at its front; the other round behind it, hooking it where it stands; he charges in:
            // both claws, his jaws, the first colossus through it, a last blow up through it.
            case CHECKMATE -> {
                b.add(new Beat(0, Kind.THRUST, 0, 5, 9, 10.0, 6.0, 0.0, 1.4, 0.5F, STUN | SPIKED));
                b.add(new Beat(other, Kind.HOOK, 2, 10, 34, 11.0, 0.0, 0.0, 1.4, 0.5F, STUN | BREAK));
                b.add(new Beat(ARM, Kind.CLAW, 14, 16, 18, 3.4, 45.0, -45.0, 1.4, 0.6F, STUN | BLEED));
                b.add(new Beat(ARM, Kind.CLAW, 18, 20, 22, 3.4, -45.0, 45.0, 1.4, 0.6F, STUN | BLEED));
                b.add(new Beat(ARM, Kind.BITE, 21, 25, 28, 3.8, 0.0, 0.0, 1.6, 0.9F, BLEED | STUN));
                b.add(new Beat(0, Kind.THRUST, 24, 28, 32, 6.0, 8.0, 0.0, 1.4, 1.4F, HEAVY | STUN | BLEED | SPIKED));
                b.add(new Beat(ARM, Kind.CLAW, 31, 33, 36, 3.6, 0.0, 0.0, 1.4, 1.2F, HEAVY | KNOCK_UP | STUN));
            }
            // Centipede Hunt: one colossus biting at it as he closes, the other striding him on and round; caught, dragged, thrown.
            case HUNT -> {
                for (int i = 0; i < 6; i++) {
                    int land = 6 + 6 * i;
                    b.add(new Beat(0, Kind.BITE, land - 4, land, land + 2, 9.0, 0.0, 0.0, 1.4, 0.3F, BLEED | STUN));
                }
                for (int i = 0; i < 5; i++) {
                    b.add(new Beat(other, Kind.CRAWL, 8 * i, 8 * i + 2, Math.min(40, 8 * i + 8), 4.0, 0.0, 0.0, 0.0, 0.0F, 0));
                }
            }
            case HUNT_DRAG -> {
                b.add(new Beat(0, Kind.CLAMP, 0, 0, 20, 6.0, 0.0, 0.0, 0.6, 0.25F, BLEED | STUN | CARRY));
                for (int i = 0; i < 3; i++) {
                    b.add(new Beat(other, Kind.CRAWL, 8 * i, 8 * i + 2, 8 * i + 8, 4.0, 0.0, 0.0, 0.0, 0.0F, 0));
                }
            }
            // Monstrous Recovery: the colossi curled round him while his body knits.
            case RECOVERY -> b.add(new Beat(ALL, Kind.CURL, 0, 4, 80, 1.6, 0.0, 0.0, 1.3, 0.0F, 0));
            // The Final Performance: the colossi at whatever he looks at, in turn, from every side.
            case PERFORMANCE -> {
                Kind[] ways = {Kind.THRUST, Kind.FLANK, Kind.SWEEP, Kind.UPPER, Kind.SLAM, Kind.THRUST, Kind.FLANK, Kind.UPPER};
                for (int i = 0; i < 16; i++) {
                    int land = 6 + 6 * i;
                    Kind kind = ways[i % ways.length];
                    double from = kind == Kind.SWEEP ? (i % 2 == 0 ? 70.0 : -70.0) : kind == Kind.THRUST ? (i % 2 == 0 ? 8.0 : -8.0) : 0.0;
                    double to = kind == Kind.SWEEP ? -from : 0.0;
                    b.add(new Beat(i % 2 == 0 ? 0 : other, kind, land - 4, land, land + 2, 12.0, from, to, 1.4, 0.3F, STUN | BLEED | SPIKED));
                }
            }
            case PERF_CLAW -> b.add(new Beat(ARM, Kind.CLAW, 0, 2, 4, 3.4, 40.0, -40.0, 1.4, 0.4F, STUN | BLEED));
            case PERF_FINISH -> {
                b.add(new Beat(0, Kind.SEIZE, 0, 5, 19, 12.0, 0.0, 0.0, 1.5, 0.6F, STUN));
                b.add(new Beat(other, Kind.THRUST, 10, 14, 18, 6.0, -6.0, 0.0, 1.4, 2.2F, HEAVY | BLEED | STUN | SPIKED));
                b.add(new Beat(ARM, Kind.CLAW, 16, 19, 22, 3.6, 0.0, 0.0, 1.4, 1.6F, HEAVY | KNOCK_UP | STUN));
            }
            default -> {
            }
        }
        // Clamp limb indices to the kagune actually out (a beat for a limb it lacks goes to the last one).
        List<Beat> out = new ArrayList<>();
        for (Beat beat : b) {
            int limb = beat.limb() >= n ? n - 1 : beat.limb();
            out.add(limb == beat.limb() ? beat : new Beat(limb, beat.kind(), beat.start(), beat.land(), beat.end(), beat.reach(), beat.from(), beat.to(),
                beat.height(), beat.power(), beat.flags()));
        }
        return out;
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
