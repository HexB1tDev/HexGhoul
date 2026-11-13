package com.hex.kagune.control.npc;

import java.util.List;
import java.util.Random;

/**
 * 4.1.53 (the owner: "Noro gives Daily quests (missions with item rewards such as ores, ghoul meat,  human meat, reputation etc...)"; of
 * the Clowns: "The leader gives daily quests as well with clown related rewards, and reputation."): a member's daily quest (pure, tested).
 * One a game day, asked of their gang's quest giver (Noro for Aogiri, Roma for the Clowns; 4.1.54, the owner: Banjou, "Daily quest giver",
 * for the Gas Masks): a job ({@link Job}) with its goal, counted
 * as they do it (kills) or handed over when they come back (things brought), and paid when it is done: its items and
 * {@link GangStanding#DAILY} reputation. {@link #kind} 0 is none; {@link #day} the game day it was given; {@link #claimed} once paid.
 */
public record GangDaily(int kind, int goal, int count, long day, boolean claimed) {
    /** A job: whose it is, what it asks ("Kill %d investigators."), how many at least and at most, and whether it is things brought. */
    public enum Job {
        NONE(GangPlayer.NONE, "", 0, 0, false),
        AOGIRI_CCG(GangPlayer.AOGIRI, "Kill %d investigators", 3, 5, false),
        AOGIRI_RIVALS(GangPlayer.AOGIRI, "Kill %d Clowns or Gas Masks", 3, 5, false),
        AOGIRI_MEAT(GangPlayer.AOGIRI, "Bring Noro %d human meat", 4, 8, true),
        CLOWNS_GAS(GangPlayer.CLOWNS, "Kill %d Gas Masks", 3, 5, false),
        CLOWNS_CCG(GangPlayer.CLOWNS, "Kill %d investigators", 2, 4, false),
        CLOWNS_FLOWERS(GangPlayer.CLOWNS, "Bring Roma %d alliums", 4, 8, true),
        // 4.1.54: Banjou's (appended: a job is kept by its place here).
        GAS_CLOWNS(GangPlayer.GAS_MASKS, "Kill %d Clowns", 3, 5, false),
        GAS_CCG(GangPlayer.GAS_MASKS, "Kill %d investigators", 2, 4, false),
        GAS_CHARCOAL(GangPlayer.GAS_MASKS, "Bring Banjou %d charcoal", 6, 12, true),
        // 4.1.71: the General's (the owner: "Join through the general (quest) and now daily quests as well.").
        CCG_GHOULS(GangPlayer.CCG, "Kill %d ghouls", 4, 7, false),
        CCG_GANGS(GangPlayer.CCG, "Kill %d of Aogiri, the Clowns or the Gas Masks", 2, 4, false),
        CCG_PAPER(GangPlayer.CCG, "Bring the General %d paper", 8, 16, true);

        public final int gang;
        public final String asks;
        public final int least;
        public final int most;
        public final boolean brought;

        Job(int gang, String asks, int least, int most, boolean brought) {
            this.gang = gang;
            this.asks = asks;
            this.least = least;
            this.most = most;
            this.brought = brought;
        }

        /** What it asks, with its goal ("Kill 4 investigators"). */
        public String text(int goal) {
            return String.format(java.util.Locale.ROOT, this.asks, goal);
        }

        /** 4.1.54: the job as its giver asks it in his own words, mid-sentence (Banjou: "kill 4 Clowns", "bring me 8 charcoal, for the filters"). */
        public String asked(int goal) {
            if (this == GAS_CHARCOAL) {
                return "bring me " + goal + " charcoal, for the filters";
            }
            if (this == CCG_PAPER) {
                return "bring me " + goal + " sheets of paper, for the reports";
            }
            String text = this.text(goal);
            return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
        }

        /** The jobs of {@code gang}'s giver. */
        public static List<Job> of(int gang) {
            return java.util.Arrays.stream(values()).filter(j -> j != NONE && j.gang == gang).toList();
        }
    }

    /** One thing paid: an item's id (or {@link #MEAT}, {@link #GHOUL_MEAT}, {@link #CLOWN_MASK}: the mod's own) and how many. */
    public record Pay(String item, int count) {
        /** What it is called ("6 raw iron", "a clown mask"). */
        public String text() {
            String name = switch (this.item) {
                case "minecraft:raw_iron" -> "raw iron";
                case "minecraft:raw_gold" -> "raw gold";
                case "minecraft:diamond" -> this.count == 1 ? "diamond" : "diamonds";
                case "minecraft:emerald" -> "emeralds";
                case "minecraft:gold_ingot" -> "gold ingots";
                case "minecraft:cake" -> "cake";
                case "minecraft:firework_rocket" -> "fireworks";
                case "minecraft:allium" -> "alliums";
                case "minecraft:iron_ingot" -> "iron ingots";
                case "minecraft:charcoal" -> "charcoal";
                case "minecraft:golden_apple" -> this.count == 1 ? "golden apple" : "golden apples";
                case GHOUL_MEAT -> "Ghoul Meat";
                case MEAT -> "human meat";
                case CLOWN_MASK -> "clown mask";
                case GAS_MASK -> "gas mask";
                case ISSUED_CASE -> "quinque case";
                default -> this.item.substring(this.item.indexOf(':') + 1).replace('_', ' ');
            };
            return this.count == 1 ? (name.equals("cake") ? "a cake" : "a " + name) : this.count + " " + name;
        }
    }

    /** The mod's own things paid (ControlItems' dish "human_meat", its Ghoul Meat, a clown's mask). */
    public static final String MEAT = "hexkagune:human_meat";
    public static final String GHOUL_MEAT = "hexkagune:ghoul_meat";
    public static final String CLOWN_MASK = "hexkagune:mask_clown";
    /** 4.1.54: the Gas Masks' own mask (the one a member is given, and one of Banjou's pay). */
    public static final String GAS_MASK = "hexkagune:mask_gas";
    /** 4.1.71: what the General's job of things brought asks for (reports), and the quinque a new investigator is issued. */
    public static final String PAPER = "minecraft:paper";
    public static final String ISSUED_CASE = "hexkagune:quinque_case_bikaku";
    /** 4.1.54: what Banjou's job of things brought asks for (his men's filters). */
    public static final String CHARCOAL = "minecraft:charcoal";

    public static final GangDaily NONE = new GangDaily(0, 0, 0, -1L, false);

    public Job job() {
        return this.kind > 0 && this.kind < Job.values().length ? Job.values()[this.kind] : Job.NONE;
    }

    /** True if one is given for game day {@code today} (one a day: tomorrow's is a new one). */
    public boolean today(long today) {
        return this.kind != 0 && this.day == today;
    }

    /** True once its goal is reached (kills counted, or the things brought). */
    public boolean done() {
        return this.kind != 0 && this.count >= this.goal;
    }

    /** {@code gang}'s job for game day {@code today} for the player seeded {@code seed}: rolled the same all day, a new one each day. */
    public static GangDaily give(int gang, long today, long seed) {
        List<Job> jobs = Job.of(gang);
        if (jobs.isEmpty()) {
            return NONE;
        }
        Random random = new Random(seed * 1000003L + today * 7919L + gang);
        Job job = jobs.get(random.nextInt(jobs.size()));
        int goal = job.least + random.nextInt(job.most - job.least + 1);
        return new GangDaily(job.ordinal(), goal, 0, today, false);
    }

    /** One more of what it counts (a kill of the kind it asks), up to its goal. */
    public GangDaily counted() {
        return this.claimed || this.done() ? this : new GangDaily(this.kind, this.goal, this.count + 1, this.day, false);
    }

    /** {@code have} of the things it asks brought: the goal met if there are enough. */
    public GangDaily brought(int have) {
        return have >= this.goal && !this.claimed ? new GangDaily(this.kind, this.goal, this.goal, this.day, false) : this;
    }

    /** Paid. */
    public GangDaily paid() {
        return new GangDaily(this.kind, this.goal, this.count, this.day, true);
    }

    /** What it pays (besides {@link GangStanding#DAILY} reputation), rolled from the day and the player: two things of the gang's. */
    public List<Pay> pay(long seed) {
        Random random = new Random(seed * 31L + this.day * 104729L + this.kind * 13L + this.goal);
        List<Pay> choices = switch (this.job().gang) {
            case GangPlayer.AOGIRI -> List.of(new Pay("minecraft:raw_iron", 6 + random.nextInt(5)), new Pay("minecraft:raw_gold", 3 + random.nextInt(4)),
                new Pay("minecraft:diamond", 1 + random.nextInt(2)), new Pay(GHOUL_MEAT, 4 + random.nextInt(5)), new Pay(MEAT, 4 + random.nextInt(5)),
                new Pay("minecraft:emerald", 3 + random.nextInt(4)));
            // 4.1.54: the Gas Masks' (Banjou's): a mask, the filters' charcoal, iron, meat for the hungry, emeralds, now and then a golden apple.
            case GangPlayer.GAS_MASKS -> List.of(new Pay(GAS_MASK, 1), new Pay(CHARCOAL, 8 + random.nextInt(9)), new Pay("minecraft:iron_ingot",
                4 + random.nextInt(5)), new Pay(GHOUL_MEAT, 3 + random.nextInt(4)), new Pay("minecraft:emerald", 2 + random.nextInt(4)),
                new Pay("minecraft:golden_apple", 1));
            // 4.1.71: the CCG's (the General's): the bureau's iron and pay, field rations, now and then a diamond or a golden apple.
            case GangPlayer.CCG -> List.of(new Pay("minecraft:iron_ingot", 6 + random.nextInt(5)), new Pay("minecraft:emerald", 4 + random.nextInt(4)),
                new Pay("minecraft:bread", 6 + random.nextInt(5)), new Pay("minecraft:gold_ingot", 2 + random.nextInt(3)), new Pay("minecraft:diamond", 1),
                new Pay("minecraft:golden_apple", 1));
            default -> List.of(new Pay("minecraft:cake", 1), new Pay("minecraft:firework_rocket", 6 + random.nextInt(5)), new Pay(CLOWN_MASK, 1),
                new Pay(GHOUL_MEAT, 3 + random.nextInt(4)), new Pay("minecraft:allium", 4 + random.nextInt(5)), new Pay("minecraft:gold_ingot",
                2 + random.nextInt(3)));
        };
        int first = random.nextInt(choices.size());
        int second = (first + 1 + random.nextInt(choices.size() - 1)) % choices.size();
        return List.of(choices.get(first), choices.get(second));
    }
}
