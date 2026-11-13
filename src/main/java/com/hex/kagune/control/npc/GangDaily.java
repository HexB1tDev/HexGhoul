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
    /**
     * 4.1.78 (the owner's to-do: "better the quests ... daily ... so it's more fun, less boring"): what a job asks for. {@link #KILL} kills
     * carrying every one of its tags ({@link Job#all}: the victim's, the player's as they killed, the gangs': {@link #CCG_TAG} ...) and none
     * of {@link Job#none}; {@link #BRING} things handed over in the hand ({@link Job#items}, any of them); {@link #WATCH} seconds at night
     * near a place (its official NPC's home: {@link Job#place}); {@link #STREAK} kills in a row (its best today).
     */
    public enum Kind {
        KILL, BRING, WATCH, STREAK
    }

    /** 4.1.78: the gangs' own tags on a kill (GangServer: who the victim was to the player's gang). */
    public static final String CCG_TAG = "d:ccg";
    public static final String RIVAL_TAG = "d:rival";
    public static final String GAS_TAG = "d:gas";
    public static final String CLOWN_TAG = "d:clowns";
    public static final String GHOUL_TAG = "d:ghoul";
    public static final String GANGED_TAG = "d:ganged";

    /**
     * A job: whose it is, what it asks ("Kill %d investigators": %d its goal, %s an "s" past one), how many at least and at most, its
     * {@link Kind} and what counts for it ({@code what}: KILL tags joined by "+", "!" before one it must not carry; BRING item ids joined
     * by "|"; WATCH the place's official NPC's key). Kept by its place here (saved as its ordinal): new ones only ever appended.
     */
    public enum Job {
        NONE(GangPlayer.NONE, "", 0, 0, Kind.KILL, ""),
        AOGIRI_CCG(GangPlayer.AOGIRI, "Kill %d investigators", 3, 5, Kind.KILL, CCG_TAG),
        AOGIRI_RIVALS(GangPlayer.AOGIRI, "Kill %d Clowns or Gas Masks", 3, 5, Kind.KILL, RIVAL_TAG),
        AOGIRI_MEAT(GangPlayer.AOGIRI, "Bring Noro %d human meat", 4, 8, Kind.BRING, MEAT),
        CLOWNS_GAS(GangPlayer.CLOWNS, "Kill %d Gas Masks", 3, 5, Kind.KILL, GAS_TAG),
        CLOWNS_CCG(GangPlayer.CLOWNS, "Kill %d investigators", 2, 4, Kind.KILL, CCG_TAG),
        CLOWNS_FLOWERS(GangPlayer.CLOWNS, "Bring Roma %d alliums", 4, 8, Kind.BRING, "minecraft:allium"),
        // 4.1.54: Banjou's (appended: a job is kept by its place here).
        GAS_CLOWNS(GangPlayer.GAS_MASKS, "Kill %d Clowns", 3, 5, Kind.KILL, CLOWN_TAG),
        GAS_CCG(GangPlayer.GAS_MASKS, "Kill %d investigators", 2, 4, Kind.KILL, CCG_TAG),
        GAS_CHARCOAL(GangPlayer.GAS_MASKS, "Bring Banjou %d charcoal", 6, 12, Kind.BRING, CHARCOAL),
        // 4.1.71: the General's (the owner: "Join through the general (quest) and now daily quests as well.").
        CCG_GHOULS(GangPlayer.CCG, "Kill %d ghouls", 4, 7, Kind.KILL, GHOUL_TAG),
        CCG_GANGS(GangPlayer.CCG, "Kill %d of Aogiri, the Clowns or the Gas Masks", 2, 4, Kind.KILL, GANGED_TAG),
        CCG_PAPER(GangPlayer.CCG, "Bring the General %d paper", 8, 16, Kind.BRING, PAPER),
        // 4.1.78: every giver's own work, eight each, three offered a day; Ken's for Anteiku (it had none).
        AOGIRI_NIGHT_RAID(GangPlayer.AOGIRI, "Kill %d investigators at night", 3, 4, Kind.KILL, CCG_TAG + "+night"),
        AOGIRI_EYES(GangPlayer.AOGIRI, "Kill %d investigators with only your eyes", 2, 3, Kind.KILL, CCG_TAG + "+eyes"),
        AOGIRI_SCOUT(GangPlayer.AOGIRI, "Watch the CCG building at night for %d seconds", 60, 90, Kind.WATCH, "general"),
        AOGIRI_CASE(GangPlayer.AOGIRI, "Bring Noro a quinque case", 1, 1, Kind.BRING, QUINQUE_CASES),
        AOGIRI_PEARLS(GangPlayer.AOGIRI, "Bring Noro %d ender pearls", 3, 6, Kind.BRING, "minecraft:ender_pearl"),
        CLOWNS_SHOW(GangPlayer.CLOWNS, "Put on a show: %d kills in a row", 4, 6, Kind.STREAK, ""),
        CLOWNS_ACROBAT(GangPlayer.CLOWNS, "Kill %d things in mid-air", 3, 5, Kind.KILL, "airborne"),
        CLOWNS_FIREWORKS(GangPlayer.CLOWNS, "Bring Roma %d fireworks", 8, 16, Kind.BRING, "minecraft:firework_rocket"),
        CLOWNS_CAKE(GangPlayer.CLOWNS, "Bring Roma %d cake%s", 1, 2, Kind.BRING, "minecraft:cake"),
        CLOWNS_GAS_NIGHT(GangPlayer.CLOWNS, "Kill %d Gas Masks at night", 2, 4, Kind.KILL, GAS_TAG + "+night"),
        GAS_GUARD(GangPlayer.GAS_MASKS, "Keep the night off the den: kill %d monsters", 10, 16, Kind.KILL, "hostile+night"),
        GAS_WATCH(GangPlayer.GAS_MASKS, "Stand guard at the den at night for %d seconds", 60, 90, Kind.WATCH, "banjou"),
        GAS_WOOL(GangPlayer.GAS_MASKS, "Bring Banjou %d white wool", 6, 10, Kind.BRING, "minecraft:white_wool"),
        GAS_BREAD(GangPlayer.GAS_MASKS, "Bring Banjou %d bread", 8, 16, Kind.BRING, "minecraft:bread"),
        GAS_CLOWNS_NIGHT(GangPlayer.GAS_MASKS, "Kill %d Clowns at night", 2, 4, Kind.KILL, CLOWN_TAG + "+night"),
        ANTEIKU_BLEND(GangPlayer.ANTEIKU, "Brew %d Anteiku Blend for the regulars", 2, 3, Kind.BRING, "hexkagune:anteiku_blend"),
        ANTEIKU_FEED(GangPlayer.ANTEIKU, "Bring Ken %d Ghoul Meat, for the ones who won't kill", 3, 5, Kind.BRING, GHOUL_MEAT),
        ANTEIKU_NIGHT(GangPlayer.ANTEIKU, "Keep the ward safe: kill %d monsters at night", 8, 12, Kind.KILL, "hostile+night"),
        ANTEIKU_FERAL(GangPlayer.ANTEIKU, "Stop %d feral ghoul%s", 1, 2, Kind.KILL, "temper:feral"),
        ANTEIKU_DOVES(GangPlayer.ANTEIKU, "Drive off %d investigators sniffing round", 2, 3, Kind.KILL, CCG_TAG),
        ANTEIKU_WATCH(GangPlayer.ANTEIKU, "Watch over Anteiku at night for %d seconds", 60, 90, Kind.WATCH, "ken"),
        ANTEIKU_BEANS(GangPlayer.ANTEIKU, "Bring Ken %d coffee beans", 8, 16, Kind.BRING, "minecraft:cocoa_beans"),
        ANTEIKU_RESTRAINT(GangPlayer.ANTEIKU, "Kill %d monsters with only your eyes", 6, 10, Kind.KILL, "hostile+eyes"),
        CCG_NIGHT(GangPlayer.CCG, "Night patrol: kill %d ghouls", 3, 5, Kind.KILL, GHOUL_TAG + "+night"),
        CCG_FERAL(GangPlayer.CCG, "Put down %d feral ghoul%s", 1, 2, Kind.KILL, "temper:feral"),
        CCG_STAKEOUT(GangPlayer.CCG, "Stake out Aogiri's hideout at night for %d seconds", 60, 90, Kind.WATCH, "ayato"),
        CCG_LAB(GangPlayer.CCG, "Bring the quinque lab %d Ghoul Meat", 2, 4, Kind.BRING, GHOUL_MEAT),
        CCG_WARD(GangPlayer.CCG, "Keep the ward safe: kill %d monsters at night", 10, 16, Kind.KILL, "hostile+night");

        public final int gang;
        public final String asks;
        public final int least;
        public final int most;
        public final boolean brought;
        public final Kind kind;
        /** 4.1.78: KILL: the tags a kill must carry (all of them) and must not; BRING: what may be handed over; WATCH: the place's NPC. */
        public final java.util.Set<String> all;
        public final java.util.Set<String> none;
        public final List<String> items;
        public final String place;

        Job(int gang, String asks, int least, int most, Kind kind, String what) {
            this.gang = gang;
            this.asks = asks;
            this.least = least;
            this.most = most;
            this.kind = kind;
            this.brought = kind == Kind.BRING;
            java.util.Set<String> all = new java.util.LinkedHashSet<>();
            java.util.Set<String> none = new java.util.LinkedHashSet<>();
            if (kind == Kind.KILL && !what.isEmpty()) {
                for (String tag : what.split("\\+")) {
                    (tag.startsWith("!") ? none : all).add(tag.startsWith("!") ? tag.substring(1) : tag);
                }
            }
            this.all = java.util.Set.copyOf(all);
            this.none = java.util.Set.copyOf(none);
            this.items = kind == Kind.BRING ? List.of(what.split("\\|")) : List.of();
            this.place = kind == Kind.WATCH ? what : null;
        }

        /** What it asks, with its goal ("Kill 4 investigators", "Bring Roma 2 cakes"; one: "Bring Roma a cake", "Stop a feral ghoul"). */
        public String text(int goal) {
            String asks = this.asks;
            int at = asks.indexOf("%d ");
            if (goal == 1 && at >= 0) {
                char next = at + 3 < asks.length() ? Character.toLowerCase(asks.charAt(at + 3)) : 'x';
                asks = asks.substring(0, at) + ("aeiou".indexOf(next) >= 0 ? "an " : "a ") + asks.substring(at + 3);
                return String.format(java.util.Locale.ROOT, asks, ""); // the number gone: only its plural left to fill
            }
            return String.format(java.util.Locale.ROOT, asks, goal, goal == 1 ? "" : "s");
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

        /** 4.1.78: true if a kill carrying {@code tags} counts for it (a KILL job: every one of its tags, none of the others). */
        public boolean counts(java.util.Set<String> tags) {
            return this.kind == Kind.KILL && tags.containsAll(this.all) && java.util.Collections.disjoint(tags, this.none);
        }

        /** 4.1.78: true if only a ghoul can do it (it asks for the eyes alone, or the kagune). */
        public boolean ghoulOnly() {
            return this.all.contains("eyes") || this.all.contains("kagune");
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
                case "hexkagune:anteiku_blend" -> "Anteiku Blend";
                case "hexkagune:blood_latte" -> this.count == 1 ? "blood latte" : "blood lattes";
                case "minecraft:cocoa_beans" -> "coffee beans";
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
    /** 4.1.78: any investigator's quinque case (Noro's job: "bring me a quinque case"). */
    public static final String QUINQUE_CASES = "hexkagune:quinque_case_bikaku|hexkagune:quinque_case_bone_greatsword|hexkagune:quinque_case_crystal_spear"
        + "|hexkagune:quinque_case_dawn|hexkagune:quinque_case_flesh_hammer|hexkagune:quinque_case_katana|hexkagune:quinque_case_koukaku"
        + "|hexkagune:quinque_case_launcher|hexkagune:quinque_case_mace|hexkagune:quinque_case_tendril_whip";
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

    /** 4.1.78: how many jobs the giver offers a day (the player takes one). */
    public static final int OFFERED = 3;

    /**
     * 4.1.78: the jobs {@code gang}'s giver offers on game day {@code today} to the player seeded {@code seed}: {@link #OFFERED} of the ones
     * they {@code can} do (a place known to the world, a ghoul's eyes), shuffled the same all day, anew each day.
     */
    public static List<Job> offers(int gang, long today, long seed, java.util.function.Predicate<Job> can) {
        List<Job> pool = new java.util.ArrayList<>(Job.of(gang));
        pool.removeIf(job -> !can.test(job));
        java.util.Collections.shuffle(pool, new Random(seed * 1000003L + today * 7919L + gang * 31L));
        return List.copyOf(pool.subList(0, Math.min(OFFERED, pool.size())));
    }

    /** 4.1.78: {@code job} taken on game day {@code today}: its goal rolled in its range (the same if asked again that day). */
    public static GangDaily take(Job job, long today, long seed) {
        Random random = new Random(seed * 7L + today * 131L + job.ordinal() * 1009L);
        int goal = job.kind == Kind.WATCH ? job.least + 10 * random.nextInt((job.most - job.least) / 10 + 1) // whole tens of seconds
            : job.least + random.nextInt(job.most - job.least + 1);
        return new GangDaily(job.ordinal(), goal, 0, today, false);
    }

    /** {@code gang}'s first job offered on game day {@code today} (any it has), taken (tests; the board is {@link #offers}). */
    public static GangDaily give(int gang, long today, long seed) {
        List<Job> jobs = offers(gang, today, seed, job -> true);
        return jobs.isEmpty() ? NONE : take(jobs.get(0), today, seed);
    }

    /** 4.1.78: a STREAK job's best run of kills today ({@code run} long now), kept, up to its goal. */
    public GangDaily best(int run) {
        return this.claimed || run <= this.count ? this : new GangDaily(this.kind, this.goal, Math.min(this.goal, run), this.day, false);
    }

    /** 4.1.78: days in a row with a daily done: one more if the last was yesterday, the same if today, else a fresh start. */
    public static int streak(int before, long last, long today) {
        return today == last ? Math.max(1, before) : today == last + 1 ? before + 1 : 1;
    }

    /** 4.1.78: what a streak of {@code days} adds: {@link #POINT} every seventh day in a row, {@link #EXTRA} every third, else nothing. */
    public static int bonus(int days) {
        return days > 0 && days % 7 == 0 ? POINT : days > 0 && days % 3 == 0 ? EXTRA : 0;
    }

    public static final int EXTRA = 1;
    public static final int POINT = 2;
    /** 4.1.78: now and then a mutation point with the pay (a ghoul's: a kagune to spend it on). */
    public static final double POINT_CHANCE = 0.05;

    /** 4.1.78: the mastery it pays a ghoul: a kill's or a fetch's 30, more for a night's watch or a show. */
    public int mastery() {
        return switch (this.job().kind) {
            case KILL -> 40;
            case BRING -> 30;
            case WATCH, STREAK -> 50;
        };
    }

    /** 4.1.78: true if this daily's pay comes with a mutation point (rolled from the day and the player). */
    public boolean point(long seed) {
        return new Random(seed * 17L + this.day * 337L + this.kind).nextDouble() < POINT_CHANCE;
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
            // 4.1.78: Anteiku's (Ken's): the cafe's blend and lattes, Ghoul Meat for the ones who can't hunt, emeralds, a golden apple.
            case GangPlayer.ANTEIKU -> List.of(new Pay("hexkagune:anteiku_blend", 2 + random.nextInt(2)), new Pay(GHOUL_MEAT, 3 + random.nextInt(3)),
                new Pay("hexkagune:blood_latte", 1 + random.nextInt(2)), new Pay("minecraft:emerald", 3 + random.nextInt(3)),
                new Pay("minecraft:golden_apple", 1), new Pay("minecraft:cocoa_beans", 6 + random.nextInt(5)));
            default -> List.of(new Pay("minecraft:cake", 1), new Pay("minecraft:firework_rocket", 6 + random.nextInt(5)), new Pay(CLOWN_MASK, 1),
                new Pay(GHOUL_MEAT, 3 + random.nextInt(4)), new Pay("minecraft:allium", 4 + random.nextInt(5)), new Pay("minecraft:gold_ingot",
                2 + random.nextInt(3)));
        };
        int first = random.nextInt(choices.size());
        int second = (first + 1 + random.nextInt(choices.size() - 1)) % choices.size();
        return List.of(choices.get(first), choices.get(second));
    }
}
