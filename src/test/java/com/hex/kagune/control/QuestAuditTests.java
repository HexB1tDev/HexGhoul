package com.hex.kagune.control;

import com.hex.kagune.control.quest.Hunts;
import com.hex.kagune.control.quest.Objective;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.Signal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 4.2.1 (the owner: "Check every request to make sure that there's no issues like the Anteco one ... I only tested one quest so far ... check
 * all the other quests, bro."): every quest can be done, and says how. Every objective's signal is one the server sends (FAVOUR never was:
 * A Friend in the Dark, Errands of the Night and Side by Side could never be finished); a night's hunt asks only for prey that lives where
 * the player is; every prey says where it lives; the dishes asked for say how they're made; Ken's daily blends can be brewed and the makings
 * are handed over; the Clowns' "dove's quinque case" falls with the dove.
 */
public final class QuestAuditTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    public static void main(String[] args) throws Exception {
        Set<Signal> sent = sent();
        book(sent);
        hunts(sent);
        favours(sent);
        dishes();
        dailies();
        dove();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.out.println("FAILED: " + failure));
            System.exit(1);
        }
        System.out.println("Quest audit tests: " + passed + " checks passed");
    }

    /** The signals the server sends (QuestServer.signal(..., Signal.X ...) or new QuestEvent(Signal.X ...)) anywhere in its code. */
    static Set<Signal> sent() throws Exception {
        Set<Signal> out = EnumSet.noneOf(Signal.class);
        Pattern call = Pattern.compile("(?:signal\\(\\w+, |new QuestEvent\\()(?:com\\.hex\\.kagune\\.control\\.quest\\.)?Signal\\.([A-Z_]+)");
        try (var files = Files.walk(Path.of("src/main/java/com/hex/kagune/control/server"))) {
            for (Path file : (Iterable<Path>)files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                Matcher m = call.matcher(Files.readString(file));
                while (m.find()) {
                    out.add(Signal.valueOf(m.group(1)));
                }
            }
        }
        check(out.size() >= 15, "the server's signals found (" + out + ")");
        return out;
    }

    /** Every quest of the book: each objective's signal is one the server sends. */
    private static void book(Set<Signal> sent) {
        int objectives = 0;
        for (Quest quest : QuestBook.all()) {
            for (Objective objective : quest.objectives()) {
                objectives++;
                check(sent.contains(objective.signal()), quest.id() + " (" + quest.title() + "): \"" + objective.text() + "\" needs " + objective.signal()
                    + ", which nothing sends: it can never be finished");
            }
        }
        check(objectives >= 60, "the book's objectives all looked at (" + objectives + ")");
        check(sent.contains(Signal.FAVOUR), "a favour done is counted (A Friend in the Dark, Errands of the Night)");
    }

    /** A night's hunt asks only for prey that lives where the player is; every prey says where. */
    private static void hunts(Set<Signal> sent) {
        String[][] biomes = {{"minecraft:plains", ""}, {"minecraft:desert", "husks"}, {"minecraft:snowy_plains", "strays"},
            {"minecraft:swamp", "slimes"}, {"minecraft:ocean", "drowned"}, {"biomesoplenty:dune_beach", "husks"}};
        for (String[] at : biomes) {
            Random random = new Random(at[0].hashCode());
            boolean far = false;
            boolean elsewhere = false;
            boolean local = at[1].isEmpty();
            for (int i = 0; i < 3000; i++) {
                Hunts.Spec spec = Hunts.draw(random, true, at[0]);
                Quest quest = Hunts.quest(spec);
                for (Objective objective : quest.objectives()) {
                    check(sent.contains(objective.signal()), "a hunt's objective is sent: " + objective.text());
                }
                if (spec.template() != 0) {
                    continue;
                }
                String text = quest.objectives().get(0).text();
                far |= text.contains("vindicators") || text.contains("pillagers") || text.contains("iron golems") || text.contains("phantoms")
                    || text.contains("cave spiders");
                if (!at[0].contains("desert") && !at[0].contains("dune")) {
                    elsewhere |= text.contains("husks");
                }
                if (!at[0].contains("snow")) {
                    elsewhere |= text.contains("strays");
                }
                if (!at[0].contains("ocean")) {
                    elsewhere |= text.contains("drowned");
                }
                if (!at[0].contains("swamp")) {
                    elsewhere |= text.contains("slimes") || text.contains("witches");
                }
                local |= !at[1].isEmpty() && text.contains(at[1]);
                check(text.contains("(") && quest.story().contains("They are found "), "a hunt's prey says where it lives: " + text);
            }
            check(!far, at[0] + ": no night's hunt for what lives far off (a mansion, a raid, a mineshaft, a village, a sleepless night)");
            check(!elsewhere, at[0] + ": no night's hunt for a desert's, the snow's, the water's or a swamp's prey elsewhere");
            check(local, at[0] + ": its own prey may be hunted there (" + at[1] + ")");
        }
        check(Hunts.here(Hunts.Where.ANY, "") && !Hunts.here(Hunts.Where.FAR, "minecraft:woodland_mansion") && Hunts.here(Hunts.Where.SNOW, "minecraft:frozen_river")
            && Hunts.here(Hunts.Where.WATER, "minecraft:river") && !Hunts.here(Hunts.Where.DESERT, "minecraft:plains"), "where each lives, read off the biome's id");
    }

    /** Every favour a ghoul can ask: its signal sent, a kill's prey says where it lives. */
    private static void favours(Set<Signal> sent) throws Exception {
        String favours = read("src/main/java/com/hex/kagune/control/quest/Favours.java");
        check(favours.contains("objective = Objective.of(\"Kill \" + prey.name() + \" (\" + prey.where() + \")\", Signal.KILL, count, Tags.mob(prey.id()));"),
            "a favour's prey says where it lives");
        Pattern objectives = Pattern.compile("Objective\\.of\\([^;]*?Signal\\.([A-Z_]+)");
        Matcher m = objectives.matcher(favours);
        int n = 0;
        while (m.find()) {
            n++;
            check(sent.contains(Signal.valueOf(m.group(1))), "a favour's objective is sent: " + m.group(1));
        }
        check(n >= 8, "every favour kind looked at (" + n + ")");
        String hunts = read("src/main/java/com/hex/kagune/control/quest/Hunts.java");
        Matcher prey = Pattern.compile("new Prey\\(\"([a-z_:]+)\", \"[^\"]+\", \\d+, \\d+, Where\\.([A-Z]+), \"([^\"]+)\"\\)").matcher(hunts);
        int kinds = 0;
        while (prey.find()) {
            kinds++;
        }
        check(kinds == 15, "every prey (15, appended only) has its where (" + kinds + ")");
    }

    /** A dish asked for (a wish, a chapter) says how it is made, from the cookbook's own recipe. */
    private static void dishes() throws Exception {
        String blend = com.hex.kagune.control.kitchen.Dishes.how("hexkagune:anteiku_blend");
        check(blend.startsWith("craft 1 glass bottle, 2 cocoa beans and 1 Blood Sugar Cubes together")
            && blend.contains("; Blood Sugar Cubes: craft 1 sugar and 1 Bottled Blood together")
            && blend.contains("; Bottled Blood: craft 1 glass bottle and 1 Human Meat together"), "the blend's recipe all the way down: " + blend);
        check(com.hex.kagune.control.kitchen.Dishes.how("hexkagune:ghouls_pie").contains("3 wheat") && com.hex.kagune.control.kitchen.Dishes.how("hexkagune:ghouls_pie")
            .contains("ghoul meat"), "a ghoul's pie's recipe: " + com.hex.kagune.control.kitchen.Dishes.how("hexkagune:ghouls_pie"));
        check(com.hex.kagune.control.kitchen.Dishes.how("minecraft:bread").isEmpty(), "nothing said of what is no dish of the kitchen's");
        for (java.util.Map.Entry<String, List<String>> wishes : com.hex.kagune.control.npc.Tastes.WISHES.entrySet()) {
            for (String item : wishes.getValue()) {
                String said = com.hex.kagune.control.npc.Tastes.wishSaid(wishes.getKey(), item, 3);
                boolean dish = com.hex.kagune.control.kitchen.Dishes.byItem(item) != null;
                check(!dish || said.contains(com.hex.kagune.control.kitchen.Dishes.how(item)), wishes.getKey() + "'s wish for " + item + " says how it's made");
            }
        }
        for (com.hex.kagune.control.npc.Courtship.Chapter chapter : com.hex.kagune.control.npc.Courtship.of("ayato")) {
            if (chapter.items().contains("hexkagune:ghouls_pie")) {
                check(chapter.ask().contains(com.hex.kagune.control.kitchen.Dishes.how("hexkagune:ghouls_pie")), "Ayato's pies chapter says how they're made");
            }
        }
    }

    /** The gangs' dailies: what a BRING job asks for says where it comes from; Ken's blends are given their makings and can be brewed. */
    private static void dailies() throws Exception {
        for (com.hex.kagune.control.npc.GangDaily.Job job : com.hex.kagune.control.npc.GangDaily.Job.values()) {
            if (!job.brought) {
                continue;
            }
            boolean plain = job.items.stream().allMatch(id -> id.equals("minecraft:bread") || id.equals("minecraft:white_wool") || id.equals("minecraft:cake"));
            check(plain || !job.hint().isEmpty(), job + " says where what it asks for comes from (" + job.items + ")");
        }
        check(com.hex.kagune.control.npc.GangDaily.Job.ANTEIKU_BLEND.hint().contains("Ken hands you the makings")
            && com.hex.kagune.control.npc.GangDaily.Job.ANTEIKU_BLEND.hint().contains("right click in the café"), "Ken's blends: the makings and the brew told");
        String gang = read("src/main/java/com/hex/kagune/control/server/GangServer.java");
        check(gang.contains("if (job == GangDaily.Job.ANTEIKU_BLEND) {\n                        JoinServer.blendMakings(player, taken.goal());")
            && gang.contains("+ (hint.isEmpty() ? \"\" : \" (\" + hint + \")\"), null);"), "the job taken: its hint said, Ken's makings handed over");
        String join = read("src/main/java/com/hex/kagune/control/server/JoinServer.java");
        check(join.contains("if (live == null && hasMakings(player) && brew(player, GangServer.home(player.m_284548_(), \"touka\"), held)) {"),
            "a glass bottle brews in the café with no shift on (the makings in hand)");
        check(join.contains("return beans != null && sugar != null && count(player, beans) >= 2 && count(player, sugar) >= 1;"),
            "off-shift, only with the makings (a bottle filled in the café is never stopped)");
        check(join.contains("String[][] each = {{\"minecraft:glass_bottle\", \"1\"}, {\"minecraft:cocoa_beans\", \"2\"}, {\"hexkagune:blood_sugar\", \"1\"}};"),
            "Ken's makings: a cup's each");
    }

    /** The Clowns' "dove's quinque case" falls with the dove. */
    private static void dove() throws Exception {
        String gang = read("src/main/java/com/hex/kagune/control/server/GangServer.java");
        int job = gang.indexOf("now = now.advance(GangPlayer.CLOWNS, GangPlayer.CLOWN_JOB, GangPlayer.CLOWN_CASE);");
        int dropped = gang.indexOf("doveCase(dead);", job);
        check(job > 0 && dropped > job && dropped - job < 300, "the job done: the dove's case falls");
        check(gang.contains("agent.getPersistentData().m_128379_(DOVE_CASE, true);")
            && gang.contains("dead.m_19983_(new net.minecraft.world.item.ItemStack(com.hex.kagune.control.ControlItems.caseOf(com.hex.kagune.control.quinque.Quinque.BLADE), 1));"),
            "an investigator's own case; a division ghoul's, a Blade's");
        String agent = read("src/main/java/com/hex/kagune/control/server/AgentBrain.java");
        check(agent.contains("boolean dove = agent.getPersistentData().m_128471_(GangServer.DOVE_CASE);")
            && agent.contains("if (!dove && (!byPlayer || mind == null || agent.m_217043_().m_188500_() >= AgentRules.caseChance("),
            "a marked dove leaves its case whatever the odds");
        check(com.hex.kagune.control.npc.JoinTrial.goal(com.hex.kagune.control.npc.GangPlayer.CLOWNS, com.hex.kagune.control.npc.GangPlayer.CLOWN_CASE)
            .contains("it fell where the dove did"), "the step says where the case is");
    }
}
