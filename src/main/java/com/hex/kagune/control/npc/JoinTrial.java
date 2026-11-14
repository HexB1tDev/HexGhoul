package com.hex.kagune.control.npc;

/**
 * 4.1.79 (the plan the owner approved, after it was shown: "whatever you think is best"; the plan: "the gangs' joining. Each chain 3 to 4
 * steps, each a different kind, a trial of its own, a scene with the leader at the end ... Aogiri: a sparring duel with Ayato, an ambush on
 * a CCG patrol; the Clowns: "make me laugh" (keep), a show in the circus ring, a job done in the clown mask; the Gas Masks: walk through
 * their gas to Banjou, hold the gate against a Clown raid; Anteiku: a shift at the café (brew and serve), defend it from a CCG raid without
 * killing a human; the CCG: an academy test, a ghoul hunted with an assigned partner"): the trials of the gangs' ways in (pure, tested).
 * Which trial each step is ({@link #of}), what winning it moves the way on to ({@link #after}), and each one's rules, as numbers and
 * checks, for {@code JoinServer} to hold them to. The steps themselves are {@link GangPlayer}'s.
 *
 * <p>Each trial is held live (the server's, never saved: logged out, dead or gone too far, it is lost and asked for again); only the step
 * is kept. The ones fought with someone are against marked bodies of its own (made for it, gone with it), never the world's.
 */
public final class JoinTrial {
    private JoinTrial() {
    }

    /** The live trials. */
    public enum Kind {
        /** Ayato: a duel that ends at low health, never a death. */
        SPAR,
        /** Aogiri: a CCG pair on its beat, both down within {@link #AMBUSH_CLOCK} of the first blow. */
        AMBUSH,
        /** Roma's ring: {@link #RING_ACTS} volunteers, one at a time, each down within {@link #RING_ACT}, never off the stage. */
        RING,
        /** Banjou: from past his gate back to him through the gas, no mask. */
        GAS,
        /** Banjou's gate held against {@link #GATE_WAVES} waves of Clowns. */
        GATE,
        /** Anteiku: a shift: {@link #SHIFT_CUSTOMERS} regulars served a cup each, brewed by the player. */
        SHIFT,
        /** Anteiku: a CCG raid driven off, nobody killed. */
        CAFE,
        /** The CCG: a ghoul hunted down with an assigned partner, who comes back alive. */
        HUNT
    }

    // ---------------------------------------------------------------- which trial each step is

    /** The live trial {@code gang}'s way in holds at {@code step}, or null (a talk, a thing to bring, a kill to make, or none). */
    public static Kind of(int gang, int step) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> step == GangPlayer.AOGIRI_SPAR ? Kind.SPAR : step == GangPlayer.AOGIRI_AMBUSH ? Kind.AMBUSH : null;
            case GangPlayer.CLOWNS -> step == GangPlayer.CLOWN_RING ? Kind.RING : null;
            case GangPlayer.GAS_MASKS -> step == GangPlayer.GAS_WALK ? Kind.GAS : step == GangPlayer.GAS_RAID ? Kind.GATE : null;
            case GangPlayer.ANTEIKU -> step == GangPlayer.ANTEIKU_SHIFT ? Kind.SHIFT : step == GangPlayer.ANTEIKU_RAID ? Kind.CAFE : null;
            case GangPlayer.CCG -> step == GangPlayer.CCG_HUNT ? Kind.HUNT : null;
            default -> null;
        };
    }

    /** The gang whose way in {@code kind} is a step of. */
    public static int gang(Kind kind) {
        return switch (kind) {
            case SPAR, AMBUSH -> GangPlayer.AOGIRI;
            case RING -> GangPlayer.CLOWNS;
            case GAS, GATE -> GangPlayer.GAS_MASKS;
            case SHIFT, CAFE -> GangPlayer.ANTEIKU;
            case HUNT -> GangPlayer.CCG;
        };
    }

    /** The step {@code kind} is held at. */
    public static int step(Kind kind) {
        return switch (kind) {
            case SPAR -> GangPlayer.AOGIRI_SPAR;
            case AMBUSH -> GangPlayer.AOGIRI_AMBUSH;
            case RING -> GangPlayer.CLOWN_RING;
            case GAS -> GangPlayer.GAS_WALK;
            case GATE -> GangPlayer.GAS_RAID;
            case SHIFT -> GangPlayer.ANTEIKU_SHIFT;
            case CAFE -> GangPlayer.ANTEIKU_RAID;
            case HUNT -> GangPlayer.CCG_HUNT;
        };
    }

    /** The step winning {@code kind} moves its way in on to. */
    public static int after(Kind kind) {
        return switch (kind) {
            case SPAR -> GangPlayer.AOGIRI_AMBUSH;
            case AMBUSH -> GangPlayer.AOGIRI_DONE;
            case RING -> GangPlayer.CLOWN_JOB;
            case GAS -> GangPlayer.GAS_RAID;
            case GATE -> GangPlayer.GAS_DONE;
            case SHIFT -> GangPlayer.ANTEIKU_RAID;
            case CAFE -> GangPlayer.ANTEIKU_DONE;
            case HUNT -> GangPlayer.CCG_DONE;
        };
    }

    /** The step a talk with the leader moves on from by itself (the answer given, the exam passed), to the next one: {@code gang}'s. */
    public static int afterTalk(int gang) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> GangPlayer.AOGIRI_SPAR;
            case GangPlayer.CLOWNS -> GangPlayer.CLOWN_RING;
            case GangPlayer.GAS_MASKS -> GangPlayer.GAS_WALK;
            case GangPlayer.ANTEIKU -> GangPlayer.ANTEIKU_SHIFT;
            case GangPlayer.CCG -> GangPlayer.CCG_EXAM;
            default -> 0;
        };
    }

    /** What {@code gang}'s way in asks at {@code step}, as the action bar and the reminders say it (empty for none). */
    public static String goal(int gang, int step) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> switch (step) {
                case GangPlayer.AOGIRI_SPAR -> "Spar with Ayato (bring him to half; never fall under a quarter)";
                case GangPlayer.AOGIRI_AMBUSH -> "Ambush the CCG patrol (both down within 30 s of your first blow)";
                case GangPlayer.AOGIRI_DONE -> "Go back to Ayato";
                default -> "";
            };
            case GangPlayer.CLOWNS -> switch (step) {
                case GangPlayer.CLOWN_RING -> "Put on a show in Roma's ring (three volunteers, 30 s each, at night)";
                case GangPlayer.CLOWN_JOB -> "Kill an investigator in a Clown's mask";
                case GangPlayer.CLOWN_CASE -> "Bring Roma the dove's quinque case";
                default -> "";
            };
            case GangPlayer.GAS_MASKS -> switch (step) {
                case GangPlayer.GAS_WALK -> "Walk back to Banjou through the gas (from past his gate, no mask)";
                case GangPlayer.GAS_RAID -> "Hold Banjou's gate against the Clowns";
                case GangPlayer.GAS_DONE -> "Go back to Banjou";
                default -> "";
            };
            case GangPlayer.ANTEIKU -> switch (step) {
                case GangPlayer.ANTEIKU_SHIFT -> "Work a shift at Anteiku (brew Anteiku Blends, serve three regulars)";
                case GangPlayer.ANTEIKU_RAID -> "Drive the CCG off from Anteiku (kill nobody)";
                case GangPlayer.ANTEIKU_DONE -> "Go back to Ken";
                default -> "";
            };
            case GangPlayer.CCG -> switch (step) {
                case GangPlayer.CCG_EXAM -> "Sit the academy's test (three questions, the General's office)";
                case GangPlayer.CCG_HUNT -> "Hunt the reported ghoul with your partner (bring him back alive)";
                case GangPlayer.CCG_DONE -> "Report to the General";
                default -> "";
            };
            default -> "";
        };
    }

    // ---------------------------------------------------------------- Ayato's spar

    /** Ticks of "3, 2, 1" before he comes at them. */
    public static final int SPAR_COUNTDOWN = 60;
    /** How long it may go on (ticks): past it, a draw (asked again). */
    public static final int SPAR_TICKS = 2400;
    /** Ayato brought to this share of his health: won. His health never goes under it from their blows in a spar. */
    public static final float SPAR_WIN = 0.5F;
    /** They brought to this share of theirs: lost. His blows never take them under it in a spar (a spar, not a death). */
    public static final float SPAR_FLOOR = 0.25F;
    /** How far from where it began they may go (blocks), and for how long past it (ticks) before it counts as running away. */
    public static final double SPAR_RING = 18.0;
    public static final int SPAR_OUT = 100;

    /**
     * What a blow of {@code amount} on a body at {@code health} of {@code max} may take in a spar that stops at {@code floor} (a share of
     * {@code max}): all of it, if it leaves the body above the floor; else just enough to bring it to the floor (none, at it already).
     */
    public static float clamp(float health, float max, float amount, float floor) {
        float least = floor * max;
        if (health - amount > least) {
            return amount;
        }
        return Math.max(0.0F, health - least);
    }

    /** True if {@code health} of {@code max} is at or under {@code floor} (a share of {@code max}). */
    public static boolean reached(float health, float max, float floor) {
        return health <= floor * max + 0.001F;
    }

    // ---------------------------------------------------------------- the ambush

    /** How far off the patrol is made (blocks, on the ground). */
    public static final double AMBUSH_NEAR = 40.0;
    public static final double AMBUSH_FAR = 70.0;
    /** From the first blow on either, both down within this (ticks), or they call it in. */
    public static final int AMBUSH_CLOCK = 600;
    /** How long to get to them before they walk on out of it (ticks). */
    public static final int AMBUSH_FIND = 6000;
    /** The patrol: the CCG's pair. */
    public static final int AMBUSH_SIZE = 2;

    /** True if the patrol has called it in: struck first at {@code first} (Long.MIN_VALUE: not yet), not finished by {@code now}. */
    public static boolean calledIn(long first, long now) {
        return first != Long.MIN_VALUE && now - first > AMBUSH_CLOCK;
    }

    // ---------------------------------------------------------------- Roma's ring

    public static final int RING_ACTS = 3;
    /** Each act: its volunteer down within this (ticks) of stepping into the ring, or the crowd boos it off. */
    public static final int RING_ACT = 600;
    /** Between acts (ticks). */
    public static final int RING_GAP = 80;
    /** The stage (blocks round Roma's spot), and how long off it (ticks) before the show is over. */
    public static final double RING_REACH = 14.0;
    public static final int RING_OUT = 60;
    /** Where a volunteer steps in (blocks from Roma's spot). */
    public static final double RING_IN_NEAR = 5.0;
    public static final double RING_IN_FAR = 9.0;
    /** The highest rank (NpcRules.RANKS: C) a volunteer may be: a show, not a slaughter. */
    public static final int RING_TOP_TIER = 1;

    /** True if a show is held at {@code dayTime} (the world's day time): at night (Roma: "Shows are at night"). */
    public static boolean showtime(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= 13000L && time < 23000L;
    }

    // ---------------------------------------------------------------- Banjou's gas

    /** Out past his gate: at least this far from him (blocks), and within {@link #GAS_OUTER}, the gas starts. */
    public static final double GAS_START = 16.0;
    public static final double GAS_OUTER = 48.0;
    /** To him within this (ticks) of the gas starting. */
    public static final int GAS_TICKS = 1200;
    /** Reached him: this close (blocks). */
    public static final double GAS_REACHED = 3.5;

    /** True if {@code distance} from Banjou is where the gas starts (past the gate, still in reach of it). */
    public static boolean gasStarts(double distance) {
        return distance >= GAS_START && distance <= GAS_OUTER;
    }

    // ---------------------------------------------------------------- the gate

    public static final int GATE_WAVES = 2;
    public static final int GATE_WAVE = 3;
    /** The whole raid (ticks). */
    public static final int GATE_TICKS = 3600;
    /** The next wave comes when this one is down, or this long (ticks) after it came, whichever is first. */
    public static final int GATE_NEXT = 900;
    /** Where the Clowns come from (blocks from Banjou). */
    public static final double GATE_NEAR = 22.0;
    public static final double GATE_FAR = 30.0;
    /** The gate's ground (blocks from Banjou), and how long off it (ticks) before the gate is given up. */
    public static final double GATE_ZONE = 50.0;
    public static final int GATE_OUT = 200;

    /** True if the next wave comes now: {@code wave} of them come so far, {@code standing} of it up, it came {@code since} ticks ago. */
    public static boolean nextWave(int wave, int standing, long since) {
        return wave < GATE_WAVES && (wave == 0 || standing == 0 || since >= GATE_NEXT);
    }

    // ---------------------------------------------------------------- the shift

    public static final int SHIFT_CUSTOMERS = 3;
    /** How long a regular waits for a cup (ticks), and how many walk out before the shift is lost. */
    public static final int SHIFT_PATIENCE = 1200;
    public static final int SHIFT_STRIKES = 2;
    /** Between regulars (ticks). */
    public static final int SHIFT_GAP = 100;
    /** Where a regular sits down (blocks from the counter), and the café's floor (blocks) they must stay on. */
    public static final double SHIFT_NEAR = 3.0;
    public static final double SHIFT_FAR = 7.0;
    public static final double SHIFT_ZONE = 24.0;
    /** What is served: the cookbook's Anteiku Blend, brewed (crafted) by the player. */
    public static final String CUP = "hexkagune:anteiku_blend";
    /**
     * 4.1.86 (the owner: "quests per player. thats the main thing"): what another player is told on their action bar when they hit or
     * talk to one of a trial's bodies that isn't theirs ({@code owner}: whose it is, or null if they aren't about).
     */
    public static String notYours(String owner) {
        return "That one is " + (owner == null || owner.isBlank() ? "another player" : owner) + "'s quest, not yours.";
    }

    /** 4.1.86: how often (ticks) one player is told so, however many blows. */
    public static final long NOT_YOURS_EVERY = 40L;

    /** 4.1.82: how near (blocks) the waiting regular a right click with the cup serves them, aimed at them or not. */
    public static final double SERVE_NEAR = 4.0;
    /** 4.1.82: the seats in front of the counter: this far out from Touka's spot, the way she faces, and this far to either side. */
    public static final int COUNTER_NEAR = 2;
    public static final int COUNTER_FAR = 6;
    public static final int COUNTER_SIDE = 3;

    /**
     * 4.1.82 (the owner: "clients are spawning in areas that are not infront of the reception"): where a regular may sit, as offsets {dx, dz}
     * from Touka's spot behind the counter: {@link #COUNTER_NEAR} to {@link #COUNTER_FAR} blocks the way she faces ({@code yaw}, Minecraft's:
     * 0 south, 90 west), the tables' side, and up to {@link #COUNTER_SIDE} to either side; the nearest rows first.
     */
    public static java.util.List<int[]> counter(float yaw) {
        double fx = -Math.sin(Math.toRadians(yaw));
        double fz = Math.cos(Math.toRadians(yaw));
        java.util.List<int[]> out = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int ahead = COUNTER_NEAR; ahead <= COUNTER_FAR; ahead++) {
            for (int side = -COUNTER_SIDE; side <= COUNTER_SIDE; side++) {
                int dx = (int)Math.round(fx * ahead - fz * side);
                int dz = (int)Math.round(fz * ahead + fx * side);
                if (seen.add((long)dx << 32 ^ dz & 0xFFFFFFFFL)) {
                    out.add(new int[] {dx, dz});
                }
            }
        }
        return out;
    }
    /** What Ken hands over for it, once: the blend's makings, three cups' worth (item id, count). */
    public static final String[][] MAKINGS = {{"minecraft:glass_bottle", "3"}, {"minecraft:cocoa_beans", "6"}, {"hexkagune:blood_sugar", "3"}};

    /** True if the shift is lost: {@code strikes} regulars walked out. */
    public static boolean shiftLost(int strikes) {
        return strikes >= SHIFT_STRIKES;
    }

    // ---------------------------------------------------------------- the café's raid

    public static final int CAFE_RAIDERS = 3;
    /** An investigator beaten to this share of his health (alive) breaks off and runs: driven off. */
    public static final float CAFE_BREAK = 0.4F;
    public static final int CAFE_TICKS = 3600;
    public static final double CAFE_NEAR = 18.0;
    public static final double CAFE_FAR = 26.0;
    public static final double CAFE_ZONE = 50.0;
    public static final int CAFE_OUT = 200;

    /** True if an investigator left at {@code health} of {@code max} by a blow breaks off (alive, and low enough). */
    public static boolean breaks(float health, float max) {
        return health > 0.0F && health <= max * CAFE_BREAK + 0.001F;
    }

    // ---------------------------------------------------------------- the hunt

    public static final double HUNT_NEAR = 50.0;
    public static final double HUNT_FAR = 90.0;
    public static final int HUNT_TICKS = 12000;
    /** The reported ghoul: a rank over what one met by them would be, at most this (NpcRules.RANKS: A). */
    public static final int HUNT_TOP_TIER = 3;
    /** How close (blocks) the partner keeps. */
    public static final double PARTNER_CLOSE = 3.5;

    // ---------------------------------------------------------------- the academy's test

    /** One question: asked, and its answers, the right one first (shown turned round, as Roma's jokes are). */
    public record Question(String asked, String right, String wrong1, String wrong2) {
        public String answer(int i) {
            return i == 0 ? this.right : i == 1 ? this.wrong1 : this.wrong2;
        }
    }

    /** The academy's questions (the mod's own: the kagune, the kakuhou, the ranks, the CCG's ways). Append only (asked by index). */
    public static final Question[] QUESTIONS = {
        new Question("Where does a kagune come from? What do you cut out of a dead ghoul?", "The kakuhou. The organ its kagune grows from.",
            "Its heart. Ghouls have two.", "Its eyes. The kakugan is the kagune's root."),
        new Question("What is a quinque made from?", "A ghoul's kakuhou.", "Steel, folded with silver.", "A ghoul's bones, ground down."),
        new Question("A ghoul in a crowd. What gives it away first?", "Its eyes. Black and red: the kakugan.", "Its smell. They smell of iron.",
            "Its shadow. It doesn't cast one."),
        new Question("How do investigators work?", "In pairs. Always.", "Alone. It covers more ground.", "In squads of ten, or not at all."),
        new Question("What can a ghoul eat?", "People. And coffee; nothing else.", "Anything, as long as it's raw.", "Nothing. They live on blood alone."),
        new Question("Which kagune grows from the shoulders and fires shards?", "Ukaku.", "Rinkaku.", "Bikaku."),
        new Question("Which kagune grows from the lower back, in tendrils?", "Rinkaku.", "Ukaku.", "Koukaku."),
        new Question("The bureau rates ghouls from D up. The worst are...", "SS.", "D.", "A."),
        new Question("A ghoul that eats ghouls can grow...", "A kakuja. Kagune armour over its body.", "Wings of light.", "A second head."),
        new Question("A ghoul in a crowd. You...", "Get the people clear first. Then the ghoul.", "Engage at once, whatever it costs.",
            "Let it go. Report it tomorrow.")};
    /** How many are asked at a sitting. */
    public static final int EXAM_ASKED = 3;

    /** The {@link #EXAM_ASKED} questions asked on {@code day} of a player of {@code mine} (indexes, all different). */
    public static int[] draw(long day, long mine) {
        return draw(QUESTIONS.length, EXAM_ASKED, day, mine);
    }

    /** 4.1.80: {@code asked} of {@code n} questions drawn for {@code day} and a player of {@code mine} (indexes, all different; Touka's homework too). */
    public static int[] draw(int n, int asked, long day, long mine) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        long seed = day * 0x9E3779B97F4A7C15L ^ mine * 0xC2B2AE3D27D4EB4FL;
        for (int i = n - 1; i > 0; i--) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int j = (int)Math.floorMod(seed >>> 33, (long)(i + 1));
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        int[] out = new int[asked];
        System.arraycopy(order, 0, out, 0, asked);
        return out;
    }

    /** True if answer {@code answer} (its index in the question: 0 the right one) is right. */
    public static boolean right(int answer) {
        return answer == 0;
    }

    /** True if a test may be sat on {@code day}: never on the day of a failed one ({@code failedOn}; Long.MIN_VALUE for none). */
    public static boolean maySit(long day, long failedOn) {
        return day != failedOn;
    }

    // ---------------------------------------------------------------- where it is

    /**
     * The way to go from where they stand ({@code dx}, {@code dz}: the thing's offset, blocks) as said in the action bar: "42 blocks
     * north-east" (Minecraft's north is -z, east +x); "here" when within 4.
     */
    public static String way(double dx, double dz) {
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 4.0) {
            return "here";
        }
        String[] names = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
        double angle = Math.atan2(dz, dx); // 0 east, pi/2 south (+z)
        int at = (int)Math.floorMod(Math.round(angle / (Math.PI / 4.0)), 8L);
        return Math.round(distance) + " blocks " + names[at];
    }

    /** "1:05": {@code ticks} as minutes and seconds (never under 0). */
    public static String clock(long ticks) {
        long seconds = Math.max(0L, (ticks + 19L) / 20L);
        return seconds / 60L + ":" + (seconds % 60L < 10L ? "0" : "") + seconds % 60L;
    }
}
