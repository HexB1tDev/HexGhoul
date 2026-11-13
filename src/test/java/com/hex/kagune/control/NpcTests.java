package com.hex.kagune.control;

import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.npc.GangPlayer;
import com.hex.kagune.control.npc.JoinTrial;
import com.hex.kagune.control.npc.JoinVoice;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcLook;
import com.hex.kagune.control.npc.NpcNames;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.npc.NpcTemper;
import com.hex.kagune.control.quest.Favours;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestEvent;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 7.0.2: the ghoul NPCs: who they are, who they go for, when and where they turn up, how hard they hit. */
public final class NpcTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    /** 4.1.79: true if {@code what} is in {@code text} at or after {@code from} (itself found: not -1), within {@code within} characters of it. */
    static boolean within(String text, int from, String what, int within) {
        if (from < 0) {
            return false;
        }
        int at = text.indexOf(what, from);
        return at >= 0 && at - from < within;
    }

    public static void main(String[] args) {
        village();
        lives();
        romance214();
        approach226();
        errands247();
        identity();
        blades();
        bubbles();
        tempers();
        spawning();
        strength();
        look();
        moves();
        bonds();
        favours();
        talk();
        attraction();
        mansion();
        dryLand();
        ccgBuilding();
        hotel();
        cityFixes();
        worldgen();
        sitePrep();
        onePerWorld();
        gangs();
        joining();
        reputation();
        dailies();
        division();
        footing();
        lag173();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Ghoul NPC tests: " + passed + " checks passed");
    }

    /** A made-up column for Footing: each block's collision top (NaN: none), and the body's room (1.8 tall) against those tops and solids. */
    static com.hex.kagune.control.Footing.Column column(java.util.Map<Integer, Double> tops, double height) {
        return new com.hex.kagune.control.Footing.Column() {
            @Override
            public double top(int y) {
                return tops.getOrDefault(y, Double.NaN);
            }

            @Override
            public boolean free(double feet) {
                for (java.util.Map.Entry<Integer, Double> block : tops.entrySet()) {
                    double from = block.getKey();
                    double to = block.getKey() + block.getValue();
                    if (to > feet + 1.0E-9 && from < feet + height - 1.0E-9) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * 4.1.73 (the owner: "Make sure NPCS dont spawn in the ground at any point, especially official NPCS."): a body put down stands on its
     * floor (the top of its collision: a slab's half, a carpet's sixteenth), never at the whole block its home was kept as; every place the mod
     * puts one down goes by it; one found in the blocks is stood out of them; every NPC written into a template stands clear.
     */
    static void footing() {
        // The ground's top block at 63 (a full block): a body wanted at 64 stands at 64.
        java.util.Map<Integer, Double> ground = new java.util.HashMap<>();
        for (int y = 50; y <= 63; y++) {
            ground.put(y, 1.0);
        }
        check(com.hex.kagune.control.Footing.stand(64.0, column(ground, 1.8)) == 64.0, "on flat ground, at its top");
        // A slab built at its home's block (64): on the slab (64.5), not half in it.
        java.util.Map<Integer, Double> slab = new java.util.HashMap<>(ground);
        slab.put(64, 0.5);
        check(com.hex.kagune.control.Footing.stand(64.0, column(slab, 1.8)) == 64.5, "a slab where it was put: on the slab, never half in it");
        java.util.Map<Integer, Double> carpet = new java.util.HashMap<>(ground);
        carpet.put(64, 1.0 / 16.0);
        check(com.hex.kagune.control.Footing.stand(64.0, column(carpet, 1.8)) == 64.0625, "a carpet: on it (a sixteenth up), not in it");
        // Ground risen over its home since (built up two blocks): out of it, onto the new top.
        java.util.Map<Integer, Double> risen = new java.util.HashMap<>(ground);
        risen.put(64, 1.0);
        risen.put(65, 1.0);
        check(com.hex.kagune.control.Footing.stand(64.0, column(risen, 1.8)) == 66.0, "the ground built up two blocks over its home: on top of it");
        // A floor a block lower (its home's slab broken since): down onto it rather than in the air (within DOWN).
        java.util.Map<Integer, Double> lower = new java.util.HashMap<>();
        for (int y = 50; y <= 62; y++) {
            lower.put(y, 1.0);
        }
        check(com.hex.kagune.control.Footing.stand(64.0, column(lower, 1.8)) == 63.0, "its floor a block lower now: down onto it");
        // No room on the nearest floor (a ceiling a block over it): the next floor up with room.
        java.util.Map<Integer, Double> low = new java.util.HashMap<>(ground);
        low.put(65, 1.0);
        low.put(66, 1.0);
        check(com.hex.kagune.control.Footing.stand(64.0, column(low, 1.8)) == 67.0, "no room under a ceiling: the floor over it");
        check(Double.isNaN(com.hex.kagune.control.Footing.stand(64.0, column(new java.util.HashMap<>(), 1.8))), "nothing near to stand on: left as it was");
        java.util.Map<Integer, Double> fence = new java.util.HashMap<>(ground);
        fence.put(64, 1.5);
        check(com.hex.kagune.control.Footing.stand(64.0, column(fence, 1.8)) == 65.5, "a fence (1.5 high to a body) under it: on the fence");
        java.util.Map<Integer, Double> deep = new java.util.HashMap<>();
        for (int y = 50; y <= 70; y++) {
            deep.put(y, 1.0);
        }
        check(Double.isNaN(com.hex.kagune.control.Footing.stand(64.0, column(deep, 1.8))) && com.hex.kagune.control.Footing.UP == 3
            && com.hex.kagune.control.Footing.DOWN == 2, "buried deeper than it may be lifted (3): left (Homeward puts an official one home)");
        check(!com.hex.kagune.control.Footing.stuck(1) && com.hex.kagune.control.Footing.stuck(2) && com.hex.kagune.control.Footing.INSIDE == 0.15,
            "in the blocks twice running, half a second apart, more than 0.15 deep: stood out (a door shut on it once, a shove: never)");
        // Every place the mod puts a body down at a kept height goes by it.
        String unique = read("src/main/java/com/hex/kagune/control/server/UniqueNpcs.java");
        String eto = read("src/main/java/com/hex/kagune/control/npc/EtoNpc.java");
        String child = read("src/main/java/com/hex/kagune/control/npc/ChildNpc.java");
        String home = read("src/main/java/com/hex/kagune/control/server/Homeward.java");
        String ghoul = read("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java");
        String footing = read("src/main/java/com/hex/kagune/control/server/NpcFooting.java");
        check(unique.contains("body.m_7678_(spawn[0] + 0.5, NpcFooting.feet(level, body, spawn[0] + 0.5, spawn[1] - 1.0, spawn[2] + 0.5), spawn[2] + 0.5, 0.0F, 0.0F);"),
            "an official NPC (or a child) coming back at its home: on its floor there");
        check(eto.contains("com.hex.kagune.control.server.NpcFooting.feet(level, this, origin.m_123341_() + 0.5, origin.m_123342_() - 1.0, origin.m_123343_() + 0.5)")
            && eto.contains("this.m_6021_(origin.m_123341_() + 0.5, feet, origin.m_123343_() + 0.5);")
            && child.contains("com.hex.kagune.control.server.NpcFooting.feet(level, this, origin.m_123341_() + 0.5, origin.m_123342_() - 1.0, origin.m_123343_() + 0.5)")
            && !eto.contains("this.m_6021_(origin.m_123341_() + 0.5, origin.m_123342_() - 1.0") && !child.contains("this.m_6021_(origin.m_123341_() + 0.5, origin.m_123342_() - 1.0"),
            "an official NPC or a child sent home: on its floor there");
        check(home.contains("NpcFooting.put(level, mob, home.f_82479_, home.f_82480_, home.f_82481_);")
            && read("src/main/java/com/hex/kagune/control/server/EtoBrain.java").contains("Homeward.put(level, npc, homeAt);")
            && read("src/main/java/com/hex/kagune/control/server/ChildBrain.java").contains("Homeward.put(level, npc, homeAt);"),
            "one put home stuck in a wall or stalled: on its floor at home");
        check(ghoul.contains("double feet = NpcFooting.feet(level, npc, behind.f_82479_, owner.m_20186_(), behind.f_82481_);")
            && ghoul.contains("npc.m_7678_(owner.m_20185_(), owner.m_20186_(), owner.m_20189_(), owner.m_146908_(), 0.0F);"),
            "an ally ghoul brought up behind its player: on the floor there with room, else at their feet");
        check(footing.contains("@SubscribeEvent\n    public static void tick(LivingEvent.LivingTickEvent event) {") && footing.contains("|| !NpcSwim.ours(mob)")
            && footing.contains("% EVERY != 0") && footing.contains("mob.f_19794_ || mob.m_20159_() || mob.m_5803_()")
            && footing.contains("agent.seated()") && footing.contains("level.m_186434_(mob, mob.m_20191_().m_82400_(-Footing.INSIDE))")
            && footing.contains("Safety.caught(failure, \"an NPC stood out of the blocks\")"),
            "every NPC of the mod's looked at every half second, never one held, riding, asleep or at its desk; an error never stops the game");
        // Every NPC written into a structure template stands clear (tools/npc_spots.py: its body's box and what holds it up).
        try {
            Process run = new ProcessBuilder("python3", "tools/npc_spots.py", "--check").redirectErrorStream(true).start();
            String said = new String(run.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int code = run.waitFor();
            check(code == 0 && said.contains("117 NPCs in the templates, 0 wrong") && said.contains("ccg_building: 53 NPCs (29 at their desks' chairs), 0 wrong")
                && said.contains("gas_hideout.nbt: 5 NPCs (0 at their desks' chairs), 0 wrong"),
                "every NPC in every template on its floor, none in a block, none over nothing: " + said.replace('\n', ' '));
        } catch (java.io.IOException | InterruptedException failure) {
            check(false, "the templates' NPCs could not be checked: " + failure);
        }
        check(read("tools/exploration_structures.py").contains("feet = py + rest(out.get((px, py, pz)), out.get((px, py - 1, pz)))")
            && read("tools/companion_structures.py").contains("wy_out, feet = wy_out - 1, wy_out - 1 + 1.0 / 16.0"),
            "the templates' makers put them on a carpet's top (not in it), down onto it (not a block over it)");
    }

    /** 4.1.73 (the owner: "Run a lag prevention check"): a widowed investigator stops looking round 72 blocks every second for its partner. */
    static void lag173() {
        check(com.hex.kagune.control.npc.AgentRules.look(0L) == 20L && com.hex.kagune.control.npc.AgentRules.look(399L) == 20L
            && com.hex.kagune.control.npc.AgentRules.look(400L) == 600L && com.hex.kagune.control.npc.AgentRules.look(Long.MAX_VALUE) == 600L
            && com.hex.kagune.control.npc.AgentRules.PARTNER_LOST == 400L,
            "its partner looked for each second while it may be about, each half minute once it is lost");
        check(read("src/main/java/com/hex/kagune/control/server/AgentBrain.java").contains("long every = AgentRules.look(ago(now, mind.partnerSeen));\n"
            + "        if (now % every != Math.floorMod(agent.m_19879_(), every)) {"), "the investigator's brain goes by it");
    }

    /** 1.0.41: its flips, heavies and ways of fighting: the stronger the quicker, a flip never slower or faster than one goes. */
    static void moves() {
        for (int tier = 0; tier + 1 < NpcIdentity.TIERS; tier++) {
            check(NpcRules.flipEvery(tier + 1) < NpcRules.flipEvery(tier) && NpcRules.heavyEvery(tier + 1) < NpcRules.heavyEvery(tier)
                && NpcRules.counter(tier + 1) > NpcRules.counter(tier) && NpcRules.dodge(tier + 1) > NpcRules.dodge(tier), "the stronger, the quicker: " + tier);
        }
        for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
            check(NpcRules.flipEvery(tier) > NpcRules.FLIP_POSED && NpcRules.heavyEvery(tier) > NpcRules.flipEvery(tier)
                && NpcRules.counter(tier) < 1.0 && NpcRules.dodge(tier) < 1.0, "it is not flipping or heaving every moment: " + tier);
        }
        for (double gap = -2.0; gap < 40.0; gap += 0.25) {
            double speed = NpcRules.flipSpeed(gap);
            check(speed >= NpcRules.FLIP_FORWARD_LEAST && speed <= NpcRules.FLIP_FORWARD_MOST, "a front flip's speed for " + gap + ": " + speed);
        }
        check(NpcRules.flipSpeed(4.0) < NpcRules.flipSpeed(8.0), "a front flip carries it farther the farther off its prey");
        int lancers = 0;
        int snipers = 0;
        for (int seed = 0; seed < 0x10000; seed++) {
            lancers += NpcRules.lancer(seed) ? 1 : 0;
            snipers += NpcRules.sniper(seed) ? 1 : 0;
        }
        check(lancers > 0x10000 / 3 && lancers < 0x10000 * 2 / 3, "about half the rinkaku fight from their tails' full length: " + lancers);
        check(snipers > 0x10000 / 4 && snipers < 0x10000 / 2, "about a third of the ukaku keep far off: " + snipers);
        check(NpcRules.SNIPE_NEAREST < NpcRules.SNIPE_FARTHEST && NpcRules.SNIPE_FARTHEST < NpcRules.SNIPE_RANGE
            && NpcRules.CROWDED < NpcRules.CLOSE_IN && NpcRules.CLOSE_IN < NpcRules.CLOSE_IN_MOST, "the ranges it keeps are in order");
    }

    /** Who one is survives the round trip through its saved number, whatever it is. */
    static void identity() {
        for (NpcIdentity.Kind kind : NpcIdentity.Kind.values()) {
            for (NpcTemper temper : NpcTemper.values()) {
                for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
                    for (int skin = 0; skin < NpcLook.SKINS.length; skin++) {
                        NpcIdentity who = new NpcIdentity(kind, temper, tier, skin, (skin * 7 + tier) % NpcNames.count(), skin * 9001 + tier * 77);
                        check(NpcIdentity.unpack(who.pack()).equals(who), "round trip: " + who);
                    }
                }
            }
        }
        // 1.0.41: one saved before its own skins (no flag): given one of them, the same every time, in range.
        long old = 1L | 2L << 1 | 1L << 4 | 3L << 6 | 17L << 10 | 4242L << 18;
        NpcIdentity before = NpcIdentity.unpack(old);
        check(before.skin() >= 0 && before.skin() < NpcLook.SKINS.length && before.equals(NpcIdentity.unpack(old)) && before.name() == 17
            && before.seed() == 4242 && NpcIdentity.unpack(before.pack()).equals(before), "an old save's ghoul gets one of the new skins, for good");
        NpcIdentity wild = new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.ANTEIKU, 9, -1, 999, 0x7FFFFFFF);
        check(wild.tier() == NpcIdentity.TIERS - 1 && wild.skin() == NpcLook.SKINS.length - 1 && wild.name() < NpcNames.count() && wild.seed() <= 0xFFFF,
            "numbers out of range are brought in: " + wild);
        Random random = new Random(7);
        Map<NpcTemper, Integer> seen = new EnumMap<>(NpcTemper.class);
        int ukaku = 0;
        int blades = 0;
        for (int i = 0; i < 20000; i++) {
            NpcIdentity who = NpcIdentity.roll(500, random::nextDouble);
            seen.merge(who.temper(), 1, Integer::sum);
            ukaku += who.kind() == NpcIdentity.Kind.UKAKU ? 1 : 0;
            blades += who.kind() == NpcIdentity.Kind.KOUKAKU ? 1 : 0;
            check(NpcIdentity.unpack(who.pack()).equals(who), "a rolled one round trips");
        }
        int total = 0;
        for (NpcTemper temper : NpcTemper.values()) {
            total += temper.weight();
        }
        for (NpcTemper temper : NpcTemper.values()) {
            double share = seen.getOrDefault(temper, 0) / 20000.0;
            check(Math.abs(share - temper.weight() / (double)total) < 0.02, temper + " turns up by its weight: " + share);
        }
        // 1.3.36: the arm blade (an NPC-only koukaku) a little over a quarter, ukaku and rinkaku about even in the rest.
        check(Math.abs(ukaku / 20000.0 - 0.35) < 0.02 && Math.abs(blades / 20000.0 - 0.28) < 0.02, "rinkaku, ukaku and the arm blade by their odds: "
            + ukaku + ", " + blades);
        check(NpcIdentity.Kind.values()[2] == NpcIdentity.Kind.KOUKAKU && NpcIdentity.Kind.byOrdinal(2) == NpcIdentity.Kind.KOUKAKU
            && NpcIdentity.Kind.byOrdinal(9) == NpcIdentity.Kind.RINKAKU, "the arm blade appended (sent by ordinal)");
        // A ghoul saved before 1.3.36 (bit 36 clear) reads as it was.
        long older = 1L | 2L << 1 | 1L << 4 | 3L << 6 | 17L << 10 | 4242L << 18 | 1L << 35;
        check(NpcIdentity.unpack(older).kind() == NpcIdentity.Kind.UKAKU && NpcIdentity.unpack(older & ~1L).kind() == NpcIdentity.Kind.RINKAKU,
            "an older save keeps its kagune");
        check(NpcTemper.pick(0.0) == NpcTemper.FERAL && NpcTemper.pick(0.999999) == NpcTemper.ANTEIKU && NpcTemper.pick(1.5) == NpcTemper.ANTEIKU,
            "the ends of the roll");
    }

    /** Who each temper goes for, leaves be, or will not touch first. */
    static void tempers() {
        NpcTemper.Attitude hostile = NpcTemper.Attitude.HOSTILE;
        NpcTemper.Attitude wary = NpcTemper.Attitude.WARY;
        NpcTemper.Attitude friendly = NpcTemper.Attitude.FRIENDLY;
        String[] sides = {null, "anteiku", "pack"};
        for (String side : sides) {
            for (boolean ghoul : new boolean[] {false, true}) {
                check(NpcTemper.FERAL.toward(ghoul, side) == hostile, "a feral goes for anyone");
                check(NpcTemper.DRIFTER.toward(ghoul, side) == wary && NpcTemper.SCAVENGER.toward(ghoul, side) == wary, "drifters and scavengers leave people be");
                check(NpcTemper.GOURMET.toward(ghoul, side) == (ghoul ? hostile : wary), "a gourmet goes for ghouls only");
            }
        }
        check(NpcTemper.PACK.toward(false, null) == hostile, "the Pack goes for humans");
        check(NpcTemper.PACK.toward(true, "anteiku") == hostile, "the Pack goes for Anteiku's ghouls");
        check(NpcTemper.PACK.toward(true, null) == wary, "the Pack sizes up a ghoul of no side");
        check(NpcTemper.PACK.toward(true, "pack") == friendly, "the Pack never touches its own");
        check(NpcTemper.ANTEIKU.toward(false, null) == friendly && NpcTemper.ANTEIKU.toward(true, "anteiku") == friendly, "Anteiku is friendly");
        check(NpcTemper.ANTEIKU.toward(true, "pack") == wary, "Anteiku is wary of the Pack");
        for (NpcTemper other : NpcTemper.values()) {
            check(NpcTemper.GOURMET.hunts(other) == (other != NpcTemper.GOURMET), "a gourmet eats every other kind: " + other);
            check(NpcTemper.ANTEIKU.hunts(other) == (other == NpcTemper.FERAL), "Anteiku only puts down ferals: " + other);
            check(!NpcTemper.DRIFTER.hunts(other) && !NpcTemper.SCAVENGER.hunts(other), "drifters and scavengers hunt no one: " + other);
            check(NpcTemper.PACK.hunts(other) == (other == NpcTemper.ANTEIKU), "the Pack hunts Anteiku: " + other);
        }
        check(NpcTemper.SCAVENGER.coward() && !NpcTemper.FERAL.coward(), "only the scavenger runs");
        check(NpcTemper.FERAL.hostile() && NpcTemper.GOURMET.hostile() && NpcTemper.PACK.hostile() && !NpcTemper.ANTEIKU.hostile()
            && !NpcTemper.DRIFTER.hostile() && !NpcTemper.SCAVENGER.hostile(), "which are hostile");
        check(NpcTemper.DRIFTER.grudge() > NpcTemper.SCAVENGER.grudge() && NpcTemper.FERAL.grudge() == Integer.MAX_VALUE, "grudges");
    }

    /** Only at night, a ring round the player, never near another. */
    static void spawning() {
        check(!NpcRules.night(0L) && !NpcRules.night(6000L) && !NpcRules.night(12999L), "not by day");
        check(NpcRules.night(13000L) && NpcRules.night(18000L) && NpcRules.night(22999L) && NpcRules.night(24000L * 5 + 18000L), "at night, any day");
        check(!NpcRules.night(23000L) && !NpcRules.night(23999L), "not at dawn");
        check(!NpcRules.spaced(NpcRules.ALONE - 1.0, 0), "never near another");
        check(NpcRules.spaced(NpcRules.ALONE + 1.0, 1), "one other far off is fine");
        check(!NpcRules.spaced(NpcRules.ALONE + 1.0, 2), "not a third in one stretch");
        check(NpcRules.spaced(Double.MAX_VALUE, 0), "none at all: fine");
        Random random = new Random(3);
        for (int i = 0; i < 2000; i++) {
            double[] at = NpcRules.around(random.nextDouble(), random.nextDouble());
            double distance = Math.hypot(at[0], at[1]);
            check(distance >= NpcRules.SPAWN_NEAREST - 1.0E-9 && distance <= NpcRules.SPAWN_FARTHEST + 1.0E-9, "a ring round the player: " + distance);
            check(distance > NpcRules.CLEAR_OF_PLAYERS, "never on top of them");
        }
        // 1.3.27: rare now: about one every two or three nights near someone out in it (a night's tries times the chance).
        double tries = (NpcRules.NIGHT_TO - NpcRules.NIGHT_FROM) / (double)NpcRules.SPAWN_EVERY;
        double expected = tries * NpcRules.SPAWN_CHANCE;
        check(expected >= 0.25 && expected <= 0.6, "rare: " + expected + " a night before spacing");
    }

    /**
     * 4.0.0: ranks D to SS. The stronger the player, the stronger the ghoul (an S or SS rare even then); each rank tougher, quicker and
     * keener; a D only its basic string; the string hurts but does not shred; an old save's three tiers are C, B and A.
     */
    static void strength() {
        Random random = new Random(11);
        for (int mastery : new int[] {0, 500, 1000}) {
            int[] ranks = new int[NpcIdentity.TIERS];
            for (int i = 0; i < 40000; i++) {
                ranks[NpcRules.tier(mastery, random.nextDouble())]++;
            }
            double top = (ranks[NpcRules.S] + ranks[NpcRules.SS]) / 40000.0;
            check(Math.abs(top - NpcRules.atLeast(mastery, NpcRules.S)) < 0.01, "S or SS at mastery " + mastery + ": " + top);
            check(top < 0.2, "an S or an SS stays rare: " + top);
            check(ranks[NpcRules.SS] < ranks[NpcRules.S], "SS rarer than S at mastery " + mastery);
            check(mastery < 1000 || ranks[NpcRules.D] == 0, "no D for the strongest");
            check(mastery > 0 || ranks[NpcRules.D] > ranks[NpcRules.A], "a beginner meets Ds more than As");
        }
        check(NpcRules.RANKS.length == NpcIdentity.TIERS && NpcRules.rankName(0).equals("D") && NpcRules.rankName(5).equals("SS"), "D to SS");
        for (int tier = 1; tier < NpcIdentity.TIERS; tier++) {
            check(NpcRules.health(tier) > NpcRules.health(tier - 1) && NpcRules.damage(tier) > NpcRules.damage(tier - 1)
                && NpcRules.armor(tier) > NpcRules.armor(tier - 1) && NpcRules.speed(tier) > NpcRules.speed(tier - 1), "rank " + tier + " is tougher");
            check(NpcRules.string(tier) > NpcRules.string(tier - 1), "rank " + tier + " strings longer");
            check(NpcRules.regenEvery(tier) < NpcRules.regenEvery(tier - 1) && NpcRules.sight(tier) > NpcRules.sight(tier - 1),
                "rank " + tier + " knits faster and sees further");
            check(NpcRules.pause(tier, 0.5) < NpcRules.pause(tier - 1, 0.5), "rank " + tier + " pauses less between strings");
        }
        check(NpcRules.health(NpcRules.SS) >= 6.0F * NpcRules.health(NpcRules.D), "an SS is a wall next to a D");
        check(!NpcRules.flips(NpcRules.D) && !NpcRules.dodges(NpcRules.D) && !NpcRules.counters(NpcRules.D) && !NpcRules.heavies(NpcRules.D),
            "a D has only its basic string");
        check(NpcRules.flips(NpcRules.C) && !NpcRules.heavies(NpcRules.C) && NpcRules.heavies(NpcRules.B) && NpcRules.counters(NpcRules.B),
            "flips from C, heavies and clashes from B");
        check(!NpcRules.frenzies(NpcRules.A) && NpcRules.frenzies(NpcRules.S) && NpcRules.frenzies(NpcRules.SS), "an S and an SS frenzy");
        check(NpcRules.health(0) > 20.0F, "a ghoul outlasts a player's health");
        for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
            GraftStage stage = NpcRules.graftStage(tier);
            int limbs = stage.limbs(NpcRules.tailMastery(tier));
            int want = tier <= NpcRules.C ? 1 : tier <= NpcRules.A ? 2 : tier == NpcRules.S ? 3 : 4;
            check(limbs == want, "rank " + tier + ": " + limbs + " tails");
            // One string and the pause after it: the damage a second it averages (before armour and difficulty).
            GraftMove move = GraftMove.firstStrike(stage);
            double power = 0.0;
            int ticks = 0;
            for (int i = 0; i < NpcRules.string(tier); i++) {
                for (GraftMove.Beat beat : move.beats(limbs)) {
                    power += beat.power();
                }
                ticks += move.ticks();
                move = GraftMove.nextStrike(move, stage);
            }
            ticks += NpcRules.pause(tier, 0.5);
            double perSecond = power * NpcRules.damage(tier) * 20.0 / ticks;
            check(perSecond >= 1.5 && perSecond <= 22.0, "rank " + tier + " lashes for " + perSecond + " a second");
            double shots = NpcRules.string(tier) * NpcRules.damage(tier) * 20.0
                / (NpcRules.string(tier) * NpcRules.SHOT_EVERY + 6 + NpcRules.pause(tier, 0.5));
            check(shots >= 1.0 && shots <= 14.0, "rank " + tier + " shoots for " + shots + " a second (and burns)");
            for (double r : new double[] {0.0, 0.49, 0.5, 0.99}) {
                int meat = NpcRules.meat(tier, r);
                check(meat >= 1 + tier / 2 && meat <= 2 + tier / 2, "rank " + tier + " leaves " + meat + " Ghoul Meat");
            }
        }
        check(NpcRules.wingMastery(NpcRules.SS) < 450, "the wings stay the plain twin wings (no crystal, no embers)");
        // An old save's ghoul (three tiers, no rank flag): its tier one up, no mask; and back the same.
        for (int old = 0; old < 3; old++) {
            long packed = 1L | 2L << 1 | (long)old << 4 | 3L << 6 | 17L << 10 | 4242L << 18 | 1L << 35;
            NpcIdentity before = NpcIdentity.unpack(packed);
            check(before.tier() == old + 1 && before.mask() == 0 && !before.aogiri(), "an old tier " + old + " is " + before.rank() + "-rank now");
            check(NpcIdentity.unpack(before.pack()).equals(before), "and stays so once saved again");
        }
        for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
            for (int mask = 0; mask <= com.hex.kagune.control.outfit.Outfits.GENERIC_MASKS.size(); mask++) {
                for (boolean aogiri : new boolean[] {false, true}) {
                    NpcIdentity who = new NpcIdentity(NpcIdentity.Kind.KOUKAKU, NpcTemper.PACK, tier, 5, 9, 1234, mask, aogiri);
                    check(NpcIdentity.unpack(who.pack()).equals(who), "rank, mask and faction saved and read back: " + who);
                }
            }
        }
        NpcIdentity masked = new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.FERAL, NpcRules.S, 1, 2, 3, 2, false);
        check("mask_gas".equals(masked.maskItem()) && masked.tagName().endsWith("S-rank"), "a wild one's mask is generic: " + masked.maskItem());
        NpcIdentity member = masked.member(1, NpcRules.B);
        check(member.aogiri() && "mask_bin_brothers".equals(member.maskItem()) && member.tagName().contains("Aogiri"), "a member's mask is its faction's");
    }

    /**
     * 4.1.45 (the owner: "roaming gangs, such as the clowns, 3-5 roaming ghouls walking together ... Clowns (all ghouls wear different clown
     * masks (nothing else) and protect eachother) (Non-hostile unless provoked) ... Gas Masks"): three to five of them, each in a different
     * mask of their gang's (its own paint on its base's model), kept in the identity every client is told; they start nothing and stand
     * together.
     */
    /**
     * 4.1.50 (the owner: Aogiri "dialog with Ayato and with a quest", "Kill 5 CCG"; the Clowns through Roma, "A short chain": make her laugh,
     * kill 3 Gas Masks, bring her an investigator's quinque case; "(symbol) (space) (name)", Aogiri dark red with a tree, the Clowns blue
     * with a clown face; "the gang will never attack the player again, even when hit, they will however help the player and defend him
     * against hostiles. IF the player kills over 10 members of his own gang, he will be banned from joining, tag removed and will be
     * attacked on sight."): a player's gang.
     */
    /**
     * 4.1.52 (the owner: "New Reputation bar when talking to fellow gang members, high reputation means you're feared, they react to it,
     * some stepping back to let you pass (just a little and not the leaders, they do not step back this also applies for official NPCS). /
     * When the reputation bar reaches 0 the player is warned to leave the area (if within the hideout), the bar starts at 50%."; "They
     * attack after a warning").
     */
    static void reputation() {
        com.hex.kagune.control.npc.GangStanding fresh = com.hex.kagune.control.npc.GangStanding.joined(7L);
        check(fresh.reputation() == 50 && com.hex.kagune.control.npc.GangStanding.START == 50 && fresh.day() == 7L && fresh.warnedAt() < 0L
            && com.hex.kagune.control.npc.GangStanding.NEW.reputation() == 50, "the bar starts at 50% (a member from before 4.1.52 too)");
        check(fresh.plus(80, 7L).reputation() == 100 && fresh.plus(-90, 7L).reputation() == 0 && fresh.plus(3, 7L).reputation() == 53,
            "the bar runs from 0 to 100");
        check(com.hex.kagune.control.npc.GangStanding.KILL_CCG > 0 && com.hex.kagune.control.npc.GangStanding.KILL_RIVAL > 0
            && com.hex.kagune.control.npc.GangStanding.DAILY > 0 && com.hex.kagune.control.npc.GangStanding.HIT_OWN > 0
            && com.hex.kagune.control.npc.GangStanding.KILL_OWN >= 5 * com.hex.kagune.control.npc.GangStanding.KILL_CCG,
            "up for the CCG, rivals and dailies; down a little for a blow on your own, a lot for killing one");
        com.hex.kagune.control.npc.GangStanding high = new com.hex.kagune.control.npc.GangStanding(90, 10L, -1L);
        com.hex.kagune.control.npc.GangStanding low = new com.hex.kagune.control.npc.GangStanding(10, 10L, -1L);
        check(high.drifted(10L).reputation() == 90 && high.drifted(12L).reputation() == 80 && high.drifted(40L).reputation() == 50
            && low.drifted(13L).reputation() == 25 && low.drifted(100L).reputation() == 50 && high.drifted(12L).day() == 12L,
            "back toward half over the days nothing happens, never past it");
        check(high.feared() && !fresh.feared() && com.hex.kagune.control.npc.GangStanding.FEARED == 80, "feared from 80");
        com.hex.kagune.control.npc.GangStanding empty = new com.hex.kagune.control.npc.GangStanding(0, 10L, -1L);
        com.hex.kagune.control.npc.GangStanding warned = empty.warn(1000L);
        long grace = com.hex.kagune.control.npc.GangStanding.GRACE;
        check(empty.empty() && warned.warnedAt() == 1000L && warned.warn(5000L).warnedAt() == 1000L && grace == 600L
            && !warned.turned(1000L + grace - 1L, true) && warned.turned(1000L + grace, true) && !warned.turned(1000L + grace, false)
            && !empty.turned(99999L, true) && warned.secondsLeft(1000L) == 30 && warned.secondsLeft(1000L + grace) == 0,
            "at 0 in the hideout: told to leave, turned on half a minute later; never outside it, never unwarned");
        check(warned.calm().warnedAt() < 0L && warned.plus(5, 10L).warnedAt() < 0L && warned.plus(5, 10L).reputation() == 5
            && !new com.hex.kagune.control.npc.GangStanding(5, 10L, -1L).turned(99999L, true), "out of it, or the bar up again: let go (not banned: it recovers)");
        int yielding = 0;
        for (int seed = 0; seed < 1000; seed++) {
            yielding += com.hex.kagune.control.npc.GangStanding.yields(seed) ? 1 : 0;
        }
        check(yielding == 600, "some of a gang step back for a feared member, not all: " + yielding + " of 1000");
        String[] bands = {"Unwanted", "Despised", "Doubted", "Known", "Respected", "Feared"};
        int[] at = {0, 10, 30, 50, 70, 90};
        boolean worded = true;
        for (int i = 0; i < bands.length; i++) {
            worded &= bands[i].equals(com.hex.kagune.control.npc.GangStanding.band(at[i]));
            String[] lines = com.hex.kagune.control.npc.GangTalk.lines(at[i]);
            worded &= lines.length >= 3;
            for (String line : lines) {
                worded &= !line.isBlank() && line.length() <= 140;
            }
            worded &= java.util.Arrays.asList(lines).contains(com.hex.kagune.control.npc.GangTalk.greet(at[i], 12345));
        }
        check(worded, "every band has its word and at least three lines of its own, the greeting one of them");
        com.hex.kagune.control.network.S2CNpcTalk page = new com.hex.kagune.control.network.S2CNpcTalk(1, "a", "b", 0, false, 0, "c", new int[0], new String[0],
            72 | com.hex.kagune.control.npc.GangPlayer.CLOWNS << 8);
        com.hex.kagune.control.network.S2CNpcTalk none = new com.hex.kagune.control.network.S2CNpcTalk(1, "a", "b", 0, false, 0, "c", new int[0], new String[0]);
        check(page.reputation() == 72 && page.gang() == com.hex.kagune.control.npc.GangPlayer.CLOWNS && none.reputation() == -1 && none.gang() == 0
            && none.standing() == com.hex.kagune.control.network.S2CNpcTalk.NO_STANDING, "the talk page carries the bar only for one of the player's own gang");
        String brain = read("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java");
        String server = read("src/main/java/com/hex/kagune/control/server/GangServer.java");
        String talk = read("src/main/java/com/hex/kagune/control/server/GhoulNpcTalk.java");
        String eto = read("src/main/java/com/hex/kagune/control/server/EtoServer.java");
        check(brain.contains("return GangServer.turned(player);") && brain.contains("GangServer.struckOwn(member, now);")
            && brain.contains("if (yields(npc, who, mind, level, now)) {") && server.contains("s.turned(now, inside)")
            && server.contains("inHideout(player, g.gang())") && talk.contains("GangServer.shown(player, com.hex.kagune.control.npc.GangPlayer.of(who))")
            && eto.contains("GangServer.shown(player, GangServer.memberOf(npc.persona()))"),
            "turned on only after the warning, in the hideout; blows on their own counted; feared: some step back (ghoul NPCs only, never the "
                + "leaders); the bar on the talk screen with their own gang and its leaders");
    }

    /**
     * 4.1.53 (the owner: "Noro gives Daily quests (missions with item rewards such as ores, ghoul meat,  human meat, reputation etc...)";
     * the Clowns' leader "gives daily quests as well with clown related rewards, and reputation"; the gangs' traits; "Joining gives the
     * mask").
     */
    /**
     * 4.1.78 (the owner's to-do: "better the quests, to include everything in the mod + minecraft, daily and romance quest betterment, so
     * it's more fun, less boring"): the daily board. Every giver (Noro, Roma, Banjou, the General, and now Ken for Anteiku) has eight jobs
     * of at least three kinds; three are offered a day (the same all day, others the next), the ones the player can do (a ghoul's eyes, a
     * place the world knows); one is taken. Kills count by their tags; a show by its best run; a watch by its seconds; things by the hand.
     */
    static void board() {
        int[] gangs = {com.hex.kagune.control.npc.GangPlayer.AOGIRI, com.hex.kagune.control.npc.GangPlayer.CLOWNS, com.hex.kagune.control.npc.GangPlayer.GAS_MASKS, com.hex.kagune.control.npc.GangPlayer.ANTEIKU, com.hex.kagune.control.npc.GangPlayer.CCG};
        for (int gang : gangs) {
            java.util.List<com.hex.kagune.control.npc.GangDaily.Job> jobs = com.hex.kagune.control.npc.GangDaily.Job.of(gang);
            java.util.Set<com.hex.kagune.control.npc.GangDaily.Kind> kinds = java.util.EnumSet.noneOf(com.hex.kagune.control.npc.GangDaily.Kind.class);
            for (com.hex.kagune.control.npc.GangDaily.Job job : jobs) {
                kinds.add(job.kind);
                check(job.gang == gang && job.least >= 1 && job.most >= job.least && !job.text(job.least).contains("%")
                    && !job.text(job.least).isBlank() && job.text(1).equals(job.text(1).replace("1 cakes", "")), "job " + job + ": its words and range");
                switch (job.kind) {
                    case KILL -> check(!job.all.isEmpty() && job.items.isEmpty() && job.place == null, "job " + job + ": a kill asks for something");
                    case BRING -> {
                        check(!job.items.isEmpty() && job.brought && job.all.isEmpty(), "job " + job + ": things brought");
                        for (String item : job.items) {
                            check(item.matches("(minecraft|hexkagune):[a-z_]+"), "job " + job + ": a real item id: " + item);
                        }
                    }
                    case WATCH -> check(job.place != null && java.util.Set.of("general", "banjou", "ken", "ayato", "roma", "noro", "eto").contains(job.place)
                        && job.least >= 30, "job " + job + ": a night's watch at an official NPC's place");
                    case STREAK -> check(job.least >= 3, "job " + job + ": a run worth calling a show");
                }
            }
            check(jobs.size() >= 8 && kinds.size() >= 3, "giver of gang " + gang + ": eight jobs or more, of three kinds or more (" + jobs.size() + ", " + kinds + ")");
            java.util.Set<com.hex.kagune.control.npc.GangDaily.Job> seen = new java.util.HashSet<>();
            boolean daily = true;
            for (long day = 0; day < 120; day++) {
                for (long seed = 0; seed < 4; seed++) {
                    java.util.List<com.hex.kagune.control.npc.GangDaily.Job> offered = com.hex.kagune.control.npc.GangDaily.offers(gang, day, seed, job -> true);
                    seen.addAll(offered);
                    daily &= offered.size() == 3 && new java.util.HashSet<>(offered).size() == 3
                        && offered.equals(com.hex.kagune.control.npc.GangDaily.offers(gang, day, seed, job -> true)) && jobs.containsAll(offered);
                    for (com.hex.kagune.control.npc.GangDaily.Job job : offered) {
                        com.hex.kagune.control.npc.GangDaily taken = com.hex.kagune.control.npc.GangDaily.take(job, day, seed);
                        daily &= (job.kind != com.hex.kagune.control.npc.GangDaily.Kind.WATCH || taken.goal() % 10 == 0)
                            && !job.text(taken.goal()).matches(".*\\b1 [a-z].*") // "a quinque case", never "1 quinque case"
                            && taken.job() == job && taken.goal() >= job.least && taken.goal() <= job.most && taken.count() == 0 && taken.today(day)
                            && !taken.today(day + 1) && !taken.done() && !taken.claimed() && taken.equals(com.hex.kagune.control.npc.GangDaily.take(job, day, seed));
                        java.util.List<com.hex.kagune.control.npc.GangDaily.Pay> pay = taken.pay(seed);
                        daily &= pay.size() == 2 && !pay.get(0).item().equals(pay.get(1).item()) && pay.get(0).count() > 0 && !pay.get(0).text().contains("_");
                    }
                }
            }
            check(com.hex.kagune.control.npc.GangDaily.OFFERED == 3, "the owner's pick 1 of 3: three on the board");
            check(daily, "gang " + gang + ": three different jobs offered, the same all day; taken, a goal in range; two things paid, named plainly");
            check(seen.containsAll(jobs), "gang " + gang + ": every job comes up on the board");
            boolean changes = false;
            for (long day = 1; day < 20; day++) {
                changes |= !com.hex.kagune.control.npc.GangDaily.offers(gang, day, 7L, job -> true).equals(com.hex.kagune.control.npc.GangDaily.offers(gang, day - 1, 7L, job -> true));
            }
            check(changes, "gang " + gang + ": a new board other days");
            // Only what the player can do: no watch where the world has no place, nothing for a ghoul's eyes for a human.
            for (long day = 0; day < 40; day++) {
                for (com.hex.kagune.control.npc.GangDaily.Job job : com.hex.kagune.control.npc.GangDaily.offers(gang, day, 3L, j -> j.kind != com.hex.kagune.control.npc.GangDaily.Kind.WATCH && !j.ghoulOnly())) {
                    check(job.kind != com.hex.kagune.control.npc.GangDaily.Kind.WATCH && !job.ghoulOnly(), "gang " + gang + " day " + day + ": only jobs they can do offered");
                }
            }
        }
        check(com.hex.kagune.control.npc.GangDaily.Job.ANTEIKU_BLEND.gang == com.hex.kagune.control.npc.GangPlayer.ANTEIKU && com.hex.kagune.control.npc.GangDaily.Job.of(com.hex.kagune.control.npc.GangPlayer.ANTEIKU).size() >= 8, "Ken has Anteiku's dailies (it had none)");
        // The jobs kept from before keep their places (saved by ordinal): new ones only after them.
        check(com.hex.kagune.control.npc.GangDaily.Job.CCG_PAPER.ordinal() == 12 && com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_CCG.ordinal() == 1 && com.hex.kagune.control.npc.GangDaily.Job.GAS_CHARCOAL.ordinal() == 9
            && com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_NIGHT_RAID.ordinal() == 13, "the jobs from before keep their saved places");
        // Kills by their tags: the old jobs' meaning kept, the new ones' conditions.
        check(com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_CCG.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.CCG_TAG)) && !com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_CCG.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.GAS_TAG))
            && com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_GAS.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.GAS_TAG, "night")) && com.hex.kagune.control.npc.GangDaily.Job.GAS_CLOWNS.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.CLOWN_TAG))
            && com.hex.kagune.control.npc.GangDaily.Job.CCG_GHOULS.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.GHOUL_TAG)) && com.hex.kagune.control.npc.GangDaily.Job.CCG_GANGS.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.GHOUL_TAG, com.hex.kagune.control.npc.GangDaily.GANGED_TAG))
            && !com.hex.kagune.control.npc.GangDaily.Job.CCG_GANGS.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.GHOUL_TAG)) && com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_RIVALS.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.RIVAL_TAG)),
            "the jobs from before count what they always did");
        check(com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_NIGHT_RAID.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.CCG_TAG, "night")) && !com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_NIGHT_RAID.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.CCG_TAG))
            && com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_EYES.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.CCG_TAG, "eyes")) && !com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_EYES.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.CCG_TAG, "kagune"))
            && com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_EYES.ghoulOnly() && com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_ACROBAT.counts(java.util.Set.of("airborne", "mob:minecraft:zombie"))
            && !com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_ACROBAT.counts(java.util.Set.of("mob:minecraft:zombie")) && com.hex.kagune.control.npc.GangDaily.Job.ANTEIKU_FERAL.counts(java.util.Set.of("temper:feral"))
            && !com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_SCOUT.counts(java.util.Set.of(com.hex.kagune.control.npc.GangDaily.CCG_TAG)) && !com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_SHOW.counts(java.util.Set.of("hostile")),
            "kills count by what they carry: at night, eyes alone, in mid-air, a feral ghoul; a watch or a show counts no kill");
        com.hex.kagune.control.npc.GangDaily show = com.hex.kagune.control.npc.GangDaily.take(com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_SHOW, 3L, 9L);
        check(show.best(2).count() == 2 && show.best(2).best(1).count() == 2 && show.best(99).done() && show.best(99).count() == show.goal(),
            "a show counts its best run, never less, up to its goal");
        com.hex.kagune.control.npc.GangDaily watch = com.hex.kagune.control.npc.GangDaily.take(com.hex.kagune.control.npc.GangDaily.Job.ANTEIKU_WATCH, 3L, 9L);
        com.hex.kagune.control.npc.GangDaily seconds = watch;
        for (int i = 0; i < watch.goal(); i++) {
            seconds = seconds.counted();
        }
        check(!watch.counted().done() && seconds.done() && seconds.counted().count() == watch.goal(), "a watch counts its seconds to its goal");
        com.hex.kagune.control.npc.GangDaily cake = com.hex.kagune.control.npc.GangDaily.take(com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_CAKE, 3L, 9L);
        check(!cake.brought(cake.goal() - 1).done() && cake.brought(cake.goal()).done() && com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_CAKE.text(1).equals("Bring Roma a cake") && com.hex.kagune.control.npc.GangDaily.Job.ANTEIKU_FERAL.text(1).equals("Stop a feral ghoul")
            && com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_CAKE.text(2).equals("Bring Roma 2 cakes") && com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_CASE.items.size() == 10,
            "things brought: enough of them, in hand; one or many named right; any quinque case for Noro");
        // Streaks: days in a row; every third a little more, every seventh a mutation point.
        check(com.hex.kagune.control.npc.GangDaily.streak(4, 9L, 10L) == 5 && com.hex.kagune.control.npc.GangDaily.streak(4, 10L, 10L) == 4 && com.hex.kagune.control.npc.GangDaily.streak(4, 7L, 10L) == 1 && com.hex.kagune.control.npc.GangDaily.streak(0, Long.MIN_VALUE / 2, 10L) == 1
            && com.hex.kagune.control.npc.GangDaily.bonus(3) == com.hex.kagune.control.npc.GangDaily.EXTRA && com.hex.kagune.control.npc.GangDaily.bonus(6) == com.hex.kagune.control.npc.GangDaily.EXTRA && com.hex.kagune.control.npc.GangDaily.bonus(7) == com.hex.kagune.control.npc.GangDaily.POINT && com.hex.kagune.control.npc.GangDaily.bonus(14) == com.hex.kagune.control.npc.GangDaily.POINT
            && com.hex.kagune.control.npc.GangDaily.bonus(1) == 0 && com.hex.kagune.control.npc.GangDaily.bonus(2) == 0 && com.hex.kagune.control.npc.GangDaily.bonus(0) == 0, "days in a row: each third a little more, each seventh a mutation point");
        int points = 0;
        for (long seed = 0; seed < 4000; seed++) {
            points += com.hex.kagune.control.npc.GangDaily.take(com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_CCG, seed, seed).point(seed) ? 1 : 0;
        }
        check(points > 100 && points < 320 && com.hex.kagune.control.npc.GangDaily.take(com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_SCOUT, 1L, 1L).mastery() > com.hex.kagune.control.npc.GangDaily.take(com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_PEARLS, 1L, 1L).mastery(),
            "now and then a mutation point (about one in twenty); a night's watch pays more mastery than an errand (" + points + ")");
        java.util.Set<String> anteiku = new java.util.HashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            for (com.hex.kagune.control.npc.GangDaily.Pay pay : com.hex.kagune.control.npc.GangDaily.take(com.hex.kagune.control.npc.GangDaily.Job.ANTEIKU_BLEND, seed, seed).pay(seed)) {
                anteiku.add(pay.item());
            }
        }
        check(anteiku.containsAll(java.util.List.of("hexkagune:anteiku_blend", com.hex.kagune.control.npc.GangDaily.GHOUL_MEAT, "hexkagune:blood_latte")), "Ken pays in the café's things");
        // The server: the board in the talk, every giver's voice, kills as tags, a show's run, a night's watch, the pay.
        String server = read("src/main/java/com/hex/kagune/control/server/GangServer.java");
        String eto = read("src/main/java/com/hex/kagune/control/server/EtoServer.java");
        String quests = read("src/main/java/com/hex/kagune/control/server/QuestServer.java");
        check(server.contains(": persona == Persona.KEN ? GangPlayer.ANTEIKU : GangPlayer.NONE;") && server.contains("\"Anything I can do for the café today?\""),
            "Ken gives Anteiku's dailies");
        check(eto.contains("if (key.equals(\"daily\") || key.startsWith(\"daily:\")) {") && eto.contains("board.add(new EtoTalk.Option(slots[i], said.jokes().get(i)[0], said.jokes().get(i)[1]));"),
            "the board on a page of its own: three jobs to choose from");
        check(server.contains("List<GangDaily.Job> offered = GangDaily.offers(gang, today, mine, job -> can(player, job));")
            && server.contains("if (job != null && offered.contains(job)) {") && server.contains("board.add(new String[] {job.text(GangDaily.take(job, today, mine).goal()), \"daily:take:\" + job.ordinal()});"),
            "only what is offered today, and the player can do, can be taken");
        check(server.contains("if (job.ghoulOnly() && !com.hex.kagune.data.KaguneProfile.isUnlocked(player)) {")
            && server.contains("return job.kind != GangDaily.Kind.WATCH || home(player.m_284548_(), job.place) != null;")
            && server.contains("UniqueNpcs.known(level, key);"), "a human is never offered a ghoul's job; a watch only where the world has the place (asking makes no NPC)");
        check(server.contains("tags.addAll(QuestServer.victim(dead));") && server.contains("com.hex.kagune.data.KaguneProfile.isUnlocked(player) ? QuestServer.context(player)\n                    : QuestServer.bare(player));")
            && !quests.substring(quests.indexOf("static Set<String> bare(ServerPlayer player) {"), quests.indexOf("static boolean night(ServerPlayer player) {")).contains("PathServer")
            && server.contains("dailyKill(player, tags, run(player, player.m_9236_().m_46467_()));"), "a kill's tags from the quest log's own, and the gangs'");
        check(server.contains("watch(player); // 4.1.78") && server.contains("if (!QuestServer.night(player)) {") && server.contains("> WATCH_REACH * WATCH_REACH"),
            "a watch counts each second near the place, at night only");
        check(server.contains("String ghoul = QuestServer.payGhoul(player, d.mastery(), d.point(mine) || bonus == GangDaily.POINT ? 1 : 0);")
            && quests.contains("cached.log.grant(points);") && quests.contains("mastery += points * Reward.RINKAKU_POINT_MASTERY;"),
            "a ghoul paid mastery and now and then a mutation point (a Rinkaku's or Bikaku's as mastery, as a quest's)");
        for (int gang : gangs) {
            for (String moment : new String[] {"board", "took", "going", "short", "paid", "rested", "nothing", "outsider"}) {
                for (int seed = 0; seed < 3; seed++) {
                    String line = com.hex.kagune.control.npc.DailyVoice.voice(gang, moment, seed, "kill 4 investigators",
                        com.hex.kagune.control.npc.GangDaily.take(com.hex.kagune.control.npc.GangDaily.Job.of(gang).get(0), 1L, 1L));
                    check(line != null && !line.isBlank() && !line.contains("null"), "gang " + gang + " " + moment + ": said in their voice: " + line);
                }
            }
        }
    }

    static void dailies() {
        board();

        com.hex.kagune.control.npc.GangDaily d = new com.hex.kagune.control.npc.GangDaily(com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_CCG.ordinal(), 3, 0, 4L, false);
        com.hex.kagune.control.npc.GangDaily three = d.counted().counted().counted();
        check(three.done() && three.count() == 3 && three.counted().count() == 3 && !three.claimed() && three.paid().claimed()
            && three.paid().counted().equals(three.paid()), "kills counted to the goal, no further; paid once");
        com.hex.kagune.control.npc.GangDaily meat = new com.hex.kagune.control.npc.GangDaily(com.hex.kagune.control.npc.GangDaily.Job.AOGIRI_MEAT.ordinal(), 5, 0, 4L, false);
        check(!meat.brought(4).done() && meat.brought(5).done() && meat.brought(9).done() && meat.job().brought, "things brought: enough of them, in hand");
        java.util.Set<String> aogiri = new java.util.HashSet<>();
        java.util.Set<String> clowns = new java.util.HashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            for (com.hex.kagune.control.npc.GangDaily.Pay pay : com.hex.kagune.control.npc.GangDaily.give(com.hex.kagune.control.npc.GangPlayer.AOGIRI, seed, seed).pay(seed)) {
                aogiri.add(pay.item());
            }
            for (com.hex.kagune.control.npc.GangDaily.Pay pay : com.hex.kagune.control.npc.GangDaily.give(com.hex.kagune.control.npc.GangPlayer.CLOWNS, seed, seed).pay(seed)) {
                clowns.add(pay.item());
            }
        }
        // 4.1.54: Banjou pays in the Gas Masks' things: a gas mask, the filters' charcoal, iron, emeralds, Ghoul Meat, now and then a golden apple.
        java.util.Set<String> gas = new java.util.HashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            for (com.hex.kagune.control.npc.GangDaily.Pay pay : com.hex.kagune.control.npc.GangDaily.give(com.hex.kagune.control.npc.GangPlayer.GAS_MASKS, seed, seed).pay(seed)) {
                gas.add(pay.item());
                check(!pay.text().contains("_"), "a reward named plainly: " + pay.text());
            }
        }
        check(gas.containsAll(java.util.List.of(com.hex.kagune.control.npc.GangDaily.GAS_MASK, com.hex.kagune.control.npc.GangDaily.CHARCOAL,
            "minecraft:iron_ingot", "minecraft:golden_apple")) && com.hex.kagune.control.npc.GangDaily.Job.GAS_CLOWNS.ordinal()
            > com.hex.kagune.control.npc.GangDaily.Job.CLOWNS_FLOWERS.ordinal() && com.hex.kagune.control.npc.GangDaily.Job.GAS_CHARCOAL.brought,
            "Banjou's jobs (appended: a kept job keeps its place) and pay");
        check(aogiri.containsAll(java.util.List.of("minecraft:raw_iron", "minecraft:raw_gold", "minecraft:diamond", com.hex.kagune.control.npc.GangDaily.GHOUL_MEAT,
                com.hex.kagune.control.npc.GangDaily.MEAT)) && clowns.containsAll(java.util.List.of("minecraft:cake", "minecraft:firework_rocket",
                com.hex.kagune.control.npc.GangDaily.CLOWN_MASK)) && com.hex.kagune.control.npc.GangStanding.DAILY == 10,
            "Noro pays in ores, Ghoul Meat, human meat; Roma in clown things (a cake, fireworks, a clown mask); both in reputation (+10%)");
        String server = read("src/main/java/com/hex/kagune/control/server/GangServer.java");
        String brain = read("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java");
        String eto = read("src/main/java/com/hex/kagune/control/server/EtoServer.java");
        String layers = read("src/main/java/com/hex/kagune/control/client/render/PlayerLayers.java");
        check(server.contains("persona == Persona.NORO ? GangPlayer.AOGIRI : persona == Persona.ROMA ? GangPlayer.CLOWNS")
            && eto.contains("page = withDaily(player, npc, page);") && eto.contains("GangServer.daily(player, npc.persona(), key, c.seed())")
            && server.contains("reputation(player, com.hex.kagune.control.npc.GangStanding.DAILY);"),
            "Noro gives Aogiri's, Roma the Clowns', from their first page; done, it pays its items and reputation");
        // 4.1.54 (the owner, the Gas Masks' "Gang Bonuses": "While wearing the gas Mask, gain night vision. / Gain speed 2 when at low HP.").
        check(server.contains(": persona == Persona.BANJOU\n            ? GangPlayer.GAS_MASKS") && server.contains("MobEffects.f_19611_, MASK_SIGHT_TICKS, 0")
            && server.contains("item.piece().id().startsWith(\"mask_gas\")") && server.contains("MobEffects.f_19596_, 40, 1")
            && com.hex.kagune.control.server.GangServer.GAS_SPEED_AT <= 0.35F && server.contains("gasTraits(player, g); // 4.1.54")
            && server.contains("tags.add(GangDaily.CLOWN_TAG);") && server.contains(": gang == GangPlayer.GAS_MASKS ? GangDaily.GAS_MASK // 4.1.54"),
            "the Gas Masks: Banjou's dailies; night vision while their gas mask is worn (taken back when it comes off), Speed II at low health; "
                + "joining gives the gas mask");
        check(brain.contains("who.gang() == 0 && !who.aogiri() && GangServer.of(player).member(com.hex.kagune.control.npc.GangPlayer.AOGIRI)")
            && brain.indexOf("Long until = mind.grudges.get(body.m_20148_());") < brain.indexOf("GangServer.of(player).member(com.hex.kagune.control.npc.GangPlayer.AOGIRI)"),
            "Aogiri: wild ghouls no longer attack unless provoked (a grudge still holds)");
        check(server.contains("MobEffects.f_19600_, FIGHT_BOOST, 0") && server.contains("MobEffects.f_19603_, FIGHT_BOOST, 1")
            && server.contains("g.member(GangPlayer.AOGIRI) && (ccg || gang)") && server.contains("g.member(GangPlayer.CLOWNS) && ccg"),
            "Aogiri gains strength fighting the CCG and other gangs; the Clowns jump boost fighting the CCG");
        check(server.contains("MobEffects.f_19609_, VANISH, 0") && com.hex.kagune.control.server.GangServer.VANISH == 100
            && com.hex.kagune.control.server.GangServer.VANISH_REST == 6000L && com.hex.kagune.control.server.GangServer.VANISH_AT <= 0.35F
            && layers.contains("if (vanished(player) || clownVanished(player)) {"),
            "the Clowns: fully invisible (kagune, body, armour) for 5 seconds at low health, every 5 minutes at most");
        check(server.contains("\"hexkagune:mask_bin_brothers\"") && server.contains("giveMask(player, gang); // 4.1.53")
            && server.contains("giveMask(player, g.gang()); // 4.1.53: a member from before it got theirs"), "joining gives the gang's mask (once a gang)");
    }

    /**
     * 4.1.70 (the owner: "Anteikou - Joinable through Ken (quest) / Traits: / All ghouls and factions are neutral to the player but the CCG.
     * / Enegy bar increases size and increases maximum capacity. / Looks: Light brown name tag with a coffee symbol."): Ken's chain step by
     * step, the name, his words, the neutrality (where it sits in a ghoul's choice of whom to go for), the capacity on both sides, the longer
     * bar as drawn.
     */
    static void anteiku() {
        com.hex.kagune.control.npc.GangPlayer k = com.hex.kagune.control.npc.GangPlayer.NEW;
        int anteiku = com.hex.kagune.control.npc.GangPlayer.ANTEIKU;
        check(anteiku == 4 && com.hex.kagune.control.npc.GangPlayer.title(anteiku).equals("Anteiku"), "Anteiku: gang 4");
        // 4.1.79: Ken's way in: the answer, then the shift, the raid, back to him (each step moved on only from the one before it).
        check(k.advance(anteiku, GangPlayer.ANTEIKU_SHIFT, GangPlayer.ANTEIKU_RAID) == k && k.advance(anteiku, GangPlayer.ANTEIKU_NOT_YET, GangPlayer.ANTEIKU_SHIFT).anteikuStep() == GangPlayer.ANTEIKU_SHIFT,
            "nothing moves on before Ken has asked why; the answer, the shift next");
        k = k.advance(anteiku, GangPlayer.ANTEIKU_NOT_YET, GangPlayer.ANTEIKU_SHIFT);
        check(k.advance(anteiku, GangPlayer.ANTEIKU_NOT_YET, GangPlayer.ANTEIKU_SHIFT) == k && k.advance(anteiku, GangPlayer.ANTEIKU_RAID, GangPlayer.ANTEIKU_DONE) == k,
            "answered once; the raid can't be won before the shift");
        k = k.advance(anteiku, GangPlayer.ANTEIKU_SHIFT, GangPlayer.ANTEIKU_RAID).advance(anteiku, GangPlayer.ANTEIKU_RAID, GangPlayer.ANTEIKU_DONE);
        check(k.anteikuStep() == GangPlayer.ANTEIKU_DONE && k.step(anteiku) == GangPlayer.ANTEIKU_DONE && !k.member(anteiku), "the shift, the raid: back to Ken");
        check(GangPlayer.ANTEIKU_SHIFT == 1 && GangPlayer.ANTEIKU_RAID == 2 && GangPlayer.ANTEIKU_DONE == 3, "the saved steps keep their numbers (the beans' 1 is the shift, the doves' 2 the raid)");
        k = k.join(anteiku);
        check(k.member(anteiku) && k.anteikuStep() == GangPlayer.ANTEIKU_NOT_YET && k.advance(anteiku, GangPlayer.ANTEIKU_NOT_YET, GangPlayer.ANTEIKU_SHIFT) == k
            && !k.member(GangPlayer.AOGIRI), "joined: Anteiku's (its way in done with; a member's never moves)");
        GangPlayer left = k.quit();
        check(left.gang() == GangPlayer.NONE && !left.banned(anteiku) && left.anteikuStep() == 0
            && left.advance(anteiku, GangPlayer.ANTEIKU_NOT_YET, GangPlayer.ANTEIKU_SHIFT).anteikuStep() == GangPlayer.ANTEIKU_SHIFT, "out of Anteiku, never banned; the way in again from the start");
        GangPlayer other = GangPlayer.NEW.advance(anteiku, GangPlayer.ANTEIKU_NOT_YET, GangPlayer.ANTEIKU_SHIFT).advance(anteiku, GangPlayer.ANTEIKU_SHIFT, GangPlayer.ANTEIKU_RAID);
        check(other.join(GangPlayer.CLOWNS).anteikuStep() == other.anteikuStep() && other.advance(GangPlayer.AOGIRI, GangPlayer.AOGIRI_NOT_YET, GangPlayer.AOGIRI_SPAR).anteikuStep() == GangPlayer.ANTEIKU_RAID
            && other.advance(GangPlayer.AOGIRI, GangPlayer.AOGIRI_NOT_YET, GangPlayer.AOGIRI_SPAR).aogiriStep() == GangPlayer.AOGIRI_SPAR, "another gang's way in leaves Ken's alone, and his leaves theirs");
        check(com.hex.kagune.control.npc.GangPlayer.named(anteiku, "Ken").equals("§f §6Ken§r") && com.hex.kagune.control.npc.GangPlayer.COFFEE.equals("")
            && com.hex.kagune.control.npc.GangPlayer.ANTEIKU_BROWN == 0xC89B6D, "the coffee cup, a space, the name (plain text's nearest gold; the tag itself light brown)");
        int r = com.hex.kagune.control.npc.GangPlayer.ANTEIKU_BROWN >> 16 & 255;
        int g = com.hex.kagune.control.npc.GangPlayer.ANTEIKU_BROWN >> 8 & 255;
        int b = com.hex.kagune.control.npc.GangPlayer.ANTEIKU_BROWN & 255;
        check(r > g && g > b && r - b > 70 && r > 160 && b > 70, "Anteiku's colour is a light brown (red over green over blue, light, not orange)");
        NpcIdentity rolled = NpcIdentity.roll(300, new java.util.Random(7)::nextDouble);
        for (com.hex.kagune.control.npc.Gang gang : com.hex.kagune.control.npc.Gang.values()) {
            check(com.hex.kagune.control.npc.GangPlayer.of(rolled.ganged(gang, 1)) != anteiku, "no roaming ghoul is Anteiku's (Ken and Touka are)");
        }
        for (int seed = 0; seed < 6; seed++) {
            check(!com.hex.kagune.control.npc.GangWords.asked(anteiku).equals(com.hex.kagune.control.npc.GangWords.asked(com.hex.kagune.control.npc.GangPlayer.CLOWNS))
                && com.hex.kagune.control.npc.GangWords.demand(anteiku, com.hex.kagune.control.npc.GangPlayer.AOGIRI, seed).contains("Aogiri"),
                "Ken asks in his own words, and names the gang a spouse should quit");
        }
        try {
            String server = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GangServer.java"));
            String brain = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java"));
            String bar = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/SharedBar.java"));
            String client = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/BikakuClient.java"));
            String hud = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/ReserveHud.java"));
            check(server.contains("|| persona == Persona.KEN || persona == Persona.GENERAL;") && server.contains(": persona == Persona.KEN ? GangPlayer.ANTEIKU : persona == Persona.GENERAL")
                && server.contains("persona == Persona.KEN || persona == Persona.TOUKA ? GangPlayer.ANTEIKU"), "Ken leads Anteiku; he and Touka are its own (their talk shows the bar)");
            check(server.contains("case GangPlayer.ANTEIKU -> ken(player, npc, g, key, seed); // 4.1.70")
                && within(server, server.indexOf("return quit(player, g, gang, key, seed);"), "case GangPlayer.ANTEIKU -> ken(", 1400),
                "talking with Ken about Anteiku goes through his way in, after the way out");
            int ken = server.indexOf("static Said ken(ServerPlayer player");
            check(ken > 0 && within(server, ken, "return welcome(player, npc, GangPlayer.ANTEIKU, g, seed);", 300)
                && within(server, ken, "moved(player, g, GangPlayer.ANTEIKU, GangPlayer.ANTEIKU_NOT_YET, GangPlayer.ANTEIKU_SHIFT);", 900),
                "Ken's way in: done, the scene; the right answer, the shift");
            check(com.hex.kagune.control.npc.GangWords.KEN_RIGHT == 2 && com.hex.kagune.control.npc.GangWords.KEN_WHY[2].startsWith("So I don't turn into a monster.")
                && server.contains("if (said == com.hex.kagune.control.npc.GangWords.KEN_RIGHT) {\n                moved(player, g, GangPlayer.ANTEIKU,"),
                "the reason Anteiku takes is the third, and it is Anteiku's own");
            check(server.contains(".m_7220_(Component.m_237113_(name).m_6270_(") && server.contains("net.minecraft.network.chat.Style.f_131099_.m_178520_(GangPlayer.ANTEIKU_BROWN)")
                && server.contains("event.setDisplayname(nameTag(gang, event.getDisplayname().getString()));"), "the tag: the cup, then the name in Anteiku's own light brown");
            check(server.contains("if (gang != GangPlayer.AOGIRI && gang != GangPlayer.CLOWNS && gang != GangPlayer.GAS_MASKS"),
                "Anteiku has no hideout to be turned out of (no ghoul of its own to turn on them)");
            check(server.contains("!was.member(GangPlayer.ANTEIKU)) {\n                    by = GangStanding.KILL_RIVAL;"), "Anteiku rewards no gang killing (it is no one's rival)");
            int grudge = brain.indexOf("Long until = mind.grudges.get(body.m_20148_());");
            int neutral = brain.indexOf("if (body instanceof ServerPlayer player && !who.ccg() && GangServer.neutral(player)) {");
            int ccg = brain.indexOf("if (who.ccg()) {\n            return division(");
            int aogiri = brain.indexOf("return aogiriFoe(body) || body instanceof Player player && AogiriGuard.unwelcome(npc, player, now);");
            int wild = brain.indexOf("return attitude(npc, who, player) == NpcTemper.Attitude.HOSTILE;");
            check(grudge > 0 && neutral > grudge && neutral < ccg && neutral < aogiri && neutral < wild && brain.indexOf("return GangServer.turned(player);") < grudge,
                "one of Anteiku: gone for on a grudge (they started it), a ban or a threat to a gang's own, then never by a ghoul, a gang or Aogiri's gate; the CCG's ghouls apart");
            int fair = brain.indexOf("static boolean fair(");
            check(fair > 0 && brain.indexOf("GangServer.neutral(player)) {\n            return false; // 4.1.70: nor on one of Anteiku in the way", fair) > fair,
                "nor hit standing in the way");
            check(bar.contains("+ GangServer.capacity(player);") && server.contains("return of(player).member(GangPlayer.ANTEIKU) ? GangPlayer.ANTEIKU_CAPACITY : 0;")
                && client.contains("return potential(player) + (GangClient.anteiku(player) ? com.hex.kagune.control.npc.GangPlayer.ANTEIKU_CAPACITY : 0);"),
                "the bar holds more for one of Anteiku, on the server (what everything costs) and on the client (what the menus read)");
            check(hud.contains("if (BikakuClient.potential(minecraft.f_91074_) > 1) {"), "the bar is purple for Potential Unlock only");
        } catch (java.io.IOException failure) {
            check(false, "read Anteiku's files: " + failure);
        }
        // The owner, of a first try whose bar ran past the hotbar: "naw man just make the energy bar smaller but longer to fit the bar, dont
        // make it past it wtf bruh"; of two ways shown, "B: shrunk into its spot". So one of Anteiku's is drawn on half pixels in the usual
        // bar's own spot: exactly as wide, half as tall and centred in its rows, its tail about twice as long in its own pixels.
        check(com.hex.kagune.control.npc.GangPlayer.ANTEIKU_CAPACITY == 1 && com.hex.kagune.control.client.ReserveBarView.FINE == 2
            && com.hex.kagune.control.client.ReserveBarView.ANTEIKU_TAIL >= 2 * com.hex.kagune.control.client.ReserveBarView.TAIL,
            "one more bar's worth; a tail about twice as long, on half pixels");
        double[][] spans = new double[2][];
        for (int which = 0; which < 2; which++) {
            double[] seen = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
            double[] at = {0.0, 0.0, 1.0};
            java.util.Deque<double[]> stack = new java.util.ArrayDeque<>();
            com.hex.kagune.control.client.ui.UiCanvas probe = new com.hex.kagune.control.client.ui.UiCanvas() {
                public void fill(int x0, int y0, int x1, int y1, int argb) {
                    seen[0] = Math.min(seen[0], at[0] + x0 * at[2]);
                    seen[1] = Math.max(seen[1], at[0] + x1 * at[2]);
                    seen[2] = Math.min(seen[2], at[1] + y0 * at[2]);
                    seen[3] = Math.max(seen[3], at[1] + y1 * at[2]);
                }

                public void gradient(int x0, int y0, int x1, int y1, int top, int bottom) {
                }

                public void text(String text, int x, int y, int argb, boolean shadow) {
                }

                public void centered(String text, int centerX, int y, int argb) {
                }

                public int width(String text) {
                    return text.length() * 6;
                }

                public void face(int x, int y, int size) {
                }

                public void hat(int x, int y, int size, int[] hole) {
                }

                public void push() {
                    stack.push(at.clone());
                }

                public void translate(float x, float y) {
                    at[0] += x * at[2];
                    at[1] += y * at[2];
                }

                public void scale(float factor) {
                    at[2] *= factor;
                }

                public void pop() {
                    System.arraycopy(stack.pop(), 0, at, 0, 3);
                }

                public void batch(Runnable draw) {
                    draw.run();
                }
            };
            for (float fill : new float[] {0.0F, 0.4F, 1.0F}) {
                for (double time = 0.0; time < 400.0; time += 7.0) {
                    com.hex.kagune.control.client.ReserveBarView.Frame frame = new com.hex.kagune.control.client.ReserveBarView.Frame(fill, fill, 0.0F, 0.0F,
                        0.9F, time);
                    if (which == 0) {
                        com.hex.kagune.control.client.ReserveBarView.draw(probe, 0, 0, frame);
                    } else {
                        com.hex.kagune.control.client.ReserveBarView.drawFine(probe, 0, 0, frame);
                    }
                }
            }
            check(stack.isEmpty() && at[0] == 0.0 && at[2] == 1.0, "the bar leaves the canvas as it found it");
            spans[which] = seen;
        }
        int wide = com.hex.kagune.control.client.ReserveBarView.WIDTH;
        int tall = com.hex.kagune.control.client.ReserveBarView.HEIGHT;
        check(spans[0][0] == 0.0 && spans[0][1] == wide && spans[0][2] >= 0.0 && spans[0][3] <= tall, "the usual bar as it was: the food bar's footprint");
        check(spans[1][0] == 0.0 && spans[1][1] == wide && spans[1][2] >= 0.0 && spans[1][3] <= tall,
            "one of Anteiku's in exactly the same spot, never past the hotbar (" + spans[1][0] + " to " + spans[1][1] + ", rows " + spans[1][2] + " to " + spans[1][3] + ")");
        check(spans[1][3] - spans[1][2] <= (spans[0][3] - spans[0][2]) / 2.0 + 0.01 && Math.abs((spans[1][2] + spans[1][3]) / 2.0 - tall / 2.0) <= 0.75,
            "half as tall, centred in its rows");
        try {
            String hud = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/ReserveHud.java"));
            check(hud.contains("if (GangClient.anteiku(minecraft.f_91074_)) {") && hud.contains("ReserveBarView.drawFine(canvas, x, y, frame);")
                && hud.contains("int x = event.getWindow().m_85445_() / 2 + 10;"), "the HUD draws one of Anteiku's the finer way, where every bar is");
        } catch (java.io.IOException failure) {
            check(false, "read the reserve HUD: " + failure);
        }
    }

    /**
     * 4.1.71 (the owner: "CCG Faction: / Join through the general (quest) and now daily quests as well. / Traits: / CCG no longer attacks
     * the player. / All ghouls but official NPCS are hostile towards the player. / Damage increased when fighting ghouls. / Looks:  Cyan Blue
     * with a brief cafe symbol."): the General's chain step by step, the name, the division's membership, where the investigators' and the
     * ghouls' choices sit, the damage, the hideout, the issued case, the dailies.
     */
    static void ccg() {
        int ccg = com.hex.kagune.control.npc.GangPlayer.CCG;
        com.hex.kagune.control.npc.GangPlayer g = com.hex.kagune.control.npc.GangPlayer.NEW;
        check(ccg == 5 && com.hex.kagune.control.npc.GangPlayer.title(ccg).equals("the CCG"), "the CCG: gang 5");
        // 4.1.79: the oath, the academy's test, the hunt with a partner, sworn in (2 stays "done", as saved; the hunt, new, is 3).
        check(g.advance(ccg, GangPlayer.CCG_EXAM, GangPlayer.CCG_HUNT) == g, "no test before the General has asked why");
        g = g.advance(ccg, GangPlayer.CCG_NOT_YET, GangPlayer.CCG_EXAM);
        check(g.generalStep() == GangPlayer.CCG_EXAM && g.advance(ccg, GangPlayer.CCG_NOT_YET, GangPlayer.CCG_EXAM) == g
            && g.advance(ccg, GangPlayer.CCG_HUNT, GangPlayer.CCG_DONE) == g, "answered as the bureau would: the test next (once); no hunt before it");
        g = g.advance(ccg, GangPlayer.CCG_EXAM, GangPlayer.CCG_HUNT).advance(ccg, GangPlayer.CCG_HUNT, GangPlayer.CCG_DONE);
        check(g.generalStep() == GangPlayer.CCG_DONE && GangPlayer.CCG_DONE == 2 && GangPlayer.CCG_EXAM == 1 && GangPlayer.CCG_HUNT == 3,
            "the test, the hunt, then sworn in (a player saved done stays done)");
        com.hex.kagune.control.npc.GangPlayer in = g.join(ccg);
        check(in.member(ccg) && in.generalStep() == 0 && !in.member(com.hex.kagune.control.npc.GangPlayer.ANTEIKU), "sworn in: the CCG's");
        com.hex.kagune.control.npc.GangPlayer out = in.quit();
        check(out.gang() == 0 && !out.banned(ccg) && out.generalStep() == 0, "resigned: never banned; the way in from the start");
        com.hex.kagune.control.npc.GangPlayer banned = in;
        for (int i = 0; i < 11; i++) {
            banned = banned.killedOwn();
        }
        check(banned.gang() == 0 && banned.banned(ccg), "past ten of their own (investigators or the division), banned as every gang");
        GangPlayer half = GangPlayer.NEW.advance(GangPlayer.ANTEIKU, 0, GangPlayer.ANTEIKU_SHIFT).advance(GangPlayer.ANTEIKU, GangPlayer.ANTEIKU_SHIFT,
            GangPlayer.ANTEIKU_RAID).advance(ccg, 0, GangPlayer.CCG_EXAM);
        check(half.anteikuStep() == GangPlayer.ANTEIKU_RAID && half.generalStep() == GangPlayer.CCG_EXAM && half.join(ccg).anteikuStep() == half.anteikuStep()
            && half.join(GangPlayer.ANTEIKU).generalStep() == GangPlayer.CCG_EXAM, "Ken's way in and the General's keep apart");
        check(com.hex.kagune.control.npc.GangPlayer.named(ccg, "Ken").equals("§f §bKen§r") && com.hex.kagune.control.npc.GangPlayer.BRIEFCASE.equals(""),
            "the quinque case, a space, the name (plain text's nearest aqua; the tag itself cyan blue)");
        int r = com.hex.kagune.control.npc.GangPlayer.CCG_CYAN >> 16 & 255;
        int gr = com.hex.kagune.control.npc.GangPlayer.CCG_CYAN >> 8 & 255;
        int b = com.hex.kagune.control.npc.GangPlayer.CCG_CYAN & 255;
        check(b > gr && gr > r && b > 200 && gr > 140 && r < 90, "the CCG's colour is a cyan blue (blue over green over red, bright)");
        NpcIdentity rolled = NpcIdentity.roll(300, new java.util.Random(13)::nextDouble);
        NpcIdentity division = rolled.ganged(com.hex.kagune.control.npc.Gang.CCG_DIVISION, 1);
        check(division.ccg() && com.hex.kagune.control.npc.GangPlayer.of(division) == ccg && com.hex.kagune.control.npc.GangPlayer.of(rolled) == 0,
            "the CCG's ghoul division is the CCG's own (its stance, its reputation); a wild ghoul is no one's");
        check(com.hex.kagune.control.npc.GangPlayer.CCG_DAMAGE > 1.2F && com.hex.kagune.control.npc.GangPlayer.CCG_DAMAGE <= 1.5F, "a member's blows on ghouls land harder");
        check(com.hex.kagune.control.npc.GangWords.GENERAL_RIGHT == 2 && com.hex.kagune.control.npc.GangWords.GENERAL_WHY[2].startsWith("To protect people"),
            "the reason the bureau takes is the third: to protect people");
        java.util.Set<String> pays = new java.util.HashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            for (com.hex.kagune.control.npc.GangDaily.Pay pay : com.hex.kagune.control.npc.GangDaily.give(ccg, seed, seed).pay(seed)) {
                pays.add(pay.item());
                check(!pay.text().contains("_"), "a reward named plainly: " + pay.text());
            }
        }
        check(pays.contains("minecraft:iron_ingot") && pays.contains("minecraft:bread") && pays.contains("minecraft:diamond") && pays.size() == 6,
            "the General pays in the bureau's things");
        check(com.hex.kagune.control.npc.GangDaily.Job.CCG_PAPER.brought && com.hex.kagune.control.npc.GangDaily.Job.CCG_PAPER.asked(10).equals("bring me 10 sheets of paper, for the reports")
            && com.hex.kagune.control.npc.GangDaily.Job.CCG_GHOULS.ordinal() > com.hex.kagune.control.npc.GangDaily.Job.GAS_CHARCOAL.ordinal(),
            "the General's jobs appended (a kept job is kept by its place), his paperwork brought in hand");
        try {
            String server = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GangServer.java"));
            String agents = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
            String brain = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java"));
            check(server.contains("|| persona == Persona.GENERAL;") && server.contains(": persona == Persona.GENERAL ? GangPlayer.CCG // 4.1.71"),
                "the General leads the CCG and gives its dailies");
            int general = server.indexOf("static Said general(ServerPlayer player");
            check(general > 0 && within(server, general, "return welcome(player, npc, GangPlayer.CCG, g, seed);", 300)
                && within(server, general, "return exam(player, g, key, seed);", 500)
                && within(server, general, "moved(player, g, GangPlayer.CCG, GangPlayer.CCG_NOT_YET, GangPlayer.CCG_EXAM);", 1000),
                "the General's way in: done, sworn in; the test; the oath, then the test");
            check(server.contains("dead instanceof com.hex.kagune.control.npc.CcgAgent && was.member(GangPlayer.CCG)"), "an investigator killed is one of a member's own");
            check(server.contains("by = AgentBrain.ccg(dead) ? -GangStanding.KILL_OWN : who != null ? GangStanding.KILL_RIVAL : 0;"),
                "a member's reputation: up for ghouls, down a lot for the CCG");
            check(server.contains("net.minecraft.network.chat.Style.f_131099_.m_178520_(GangPlayer.CCG_CYAN)"), "the tag: the case, then the name in cyan blue");
            check(server.contains("event.setAmount(event.getAmount() * GangPlayer.CCG_DAMAGE);") && server.contains("if (of(player).member(GangPlayer.CCG) && ghoul(hurt)) {")
                && server.contains("return npc.identity() == null || !npc.identity().ccg();") && server.contains("&& !named.persona().human();"),
                "a member's blows land harder on any ghoul (not the division's, not the General)");
            check(server.contains(": gang == GangPlayer.CCG ? \"ccg_building\" : \"gas_hideout\");") && server.contains("gang == GangPlayer.CCG ? GangDaily.ISSUED_CASE"),
                "the tower is the CCG's hideout (an empty bar there turns it on them); a new member is issued a quinque");
            int wanted = agents.indexOf("static boolean wanted(CcgAgent agent");
            int stance = agents.indexOf("int stance = GangServer.ccgStance(player);", wanted);
            int grudge = agents.indexOf("Long until = mind.grudges.get(body.m_20148_());", wanted);
            check(wanted > 0 && stance > wanted && stance < grudge && agents.indexOf("return GangServer.turned(player);", stance) < grudge,
                "an investigator never goes for one of the CCG, marked or struck, unless turned; one it banned, on sight");
            check(agents.contains("|| GangServer.ccgStance(player) == GangServer.OWN_GANG) {\n                continue; // 4.1.71")
                && agents.contains("GangServer.struckOwn(member, now); // 4.1.71"), "a member's kagune never marks them; their blows cost reputation, not a fight");
            int hunted = brain.indexOf("if (body instanceof ServerPlayer player && !who.ccg() && GangServer.hunted(player)) {");
            check(hunted > brain.indexOf("GangServer.neutral(player)) {") && hunted < brain.indexOf("if (who.ccg()) {\n            return division(")
                && hunted > brain.indexOf("return GangServer.turned(player);"), "every ghoul NPC but the division goes for one of the CCG");
        } catch (java.io.IOException failure) {
            check(false, "read the CCG's files: " + failure);
        }
    }

    /**
     * 4.1.79 (the plan the owner approved: "the gangs' joining. Each chain 3 to 4 steps, each a different kind, a trial of its own, a scene with
     * the leader at the end"): the trials' rules (JoinTrial), their words (JoinVoice), and how the server holds them (JoinServer, the brains,
     * the talk).
     */
    static void trials() {
        int[][] orders = {{GangPlayer.AOGIRI, 0, GangPlayer.AOGIRI_SPAR, GangPlayer.AOGIRI_AMBUSH, GangPlayer.AOGIRI_DONE},
            {GangPlayer.CLOWNS, 0, GangPlayer.CLOWN_RING, GangPlayer.CLOWN_JOB, GangPlayer.CLOWN_CASE},
            {GangPlayer.GAS_MASKS, 0, GangPlayer.GAS_WALK, GangPlayer.GAS_RAID, GangPlayer.GAS_DONE},
            {GangPlayer.ANTEIKU, 0, GangPlayer.ANTEIKU_SHIFT, GangPlayer.ANTEIKU_RAID, GangPlayer.ANTEIKU_DONE},
            {GangPlayer.CCG, 0, GangPlayer.CCG_EXAM, GangPlayer.CCG_HUNT, GangPlayer.CCG_DONE}};
        java.util.Set<JoinTrial.Kind> seen = java.util.EnumSet.noneOf(JoinTrial.Kind.class);
        for (int[] order : orders) {
            int gang = order[0];
            check(JoinTrial.afterTalk(gang) == order[2], "the first answer (or Ayato's ask) moves " + GangPlayer.title(gang) + " on to its second step");
            java.util.Set<Object> kinds = new java.util.HashSet<>();
            int trials = 0;
            for (int i = 1; i < order.length; i++) {
                JoinTrial.Kind kind = JoinTrial.of(gang, order[i]);
                kinds.add(kind == null ? "step " + order[i] : kind);
                check(!JoinTrial.goal(gang, order[i]).isEmpty() || order[i] == 0, "every step past the first says what it asks: " + GangPlayer.title(gang) + " " + order[i]);
                if (kind != null) {
                    trials++;
                    seen.add(kind);
                    check(JoinTrial.gang(kind) == gang && JoinTrial.step(kind) == order[i] && i + 1 < order.length && JoinTrial.after(kind) == order[i + 1],
                        kind + ": " + GangPlayer.title(gang) + "'s, held at its step, won moves on to the next");
                }
            }
            check(kinds.size() == order.length - 1 && trials >= 1, GangPlayer.title(gang) + ": each step a different kind, at least one a live trial");
            check(JoinTrial.of(gang, 0) == null, "the first ask is a talk, never a trial");
        }
        check(seen.size() == JoinTrial.Kind.values().length, "every trial is some gang's step");
        check(JoinTrial.of(GangPlayer.AOGIRI, GangPlayer.AOGIRI_SPAR) == JoinTrial.Kind.SPAR && JoinTrial.of(GangPlayer.CLOWNS, GangPlayer.CLOWN_RING) == JoinTrial.Kind.RING
            && JoinTrial.of(GangPlayer.GAS_MASKS, GangPlayer.GAS_RAID) == JoinTrial.Kind.GATE && JoinTrial.of(GangPlayer.ANTEIKU, GangPlayer.ANTEIKU_RAID) == JoinTrial.Kind.CAFE
            && JoinTrial.of(GangPlayer.CCG, GangPlayer.CCG_HUNT) == JoinTrial.Kind.HUNT && JoinTrial.of(GangPlayer.CCG, GangPlayer.CCG_EXAM) == null,
            "the plan: Ayato's spar, Roma's ring, the gate, the café's raid, the hunt; the academy's test is talk");
        // Ayato's spar: a duel that ends at low health, never a death.
        check(JoinTrial.clamp(20.0F, 20.0F, 5.0F, 0.25F) == 5.0F && JoinTrial.clamp(8.0F, 20.0F, 5.0F, 0.25F) == 3.0F
            && JoinTrial.clamp(5.0F, 20.0F, 3.0F, 0.25F) == 0.0F && JoinTrial.clamp(40.0F, 40.0F, 100.0F, 0.5F) == 20.0F,
            "a blow lands whole above the floor; one that would go under it lands to the floor only");
        float hp = 20.0F;
        for (int blow = 0; blow < 40; blow++) {
            hp -= JoinTrial.clamp(hp, 20.0F, 1.0F + blow % 7, JoinTrial.SPAR_FLOOR);
            check(hp >= 20.0F * JoinTrial.SPAR_FLOOR - 0.001F && hp > 0.0F, "no run of Ayato's blows takes them under a quarter (never dead)");
        }
        check(JoinTrial.reached(hp, 20.0F, JoinTrial.SPAR_FLOOR) && JoinTrial.reached(5.0F, 20.0F, 0.25F) && !JoinTrial.reached(5.1F, 20.0F, 0.25F),
            "brought to the floor: it is reached");
        check(JoinTrial.SPAR_FLOOR == 0.25F && JoinTrial.SPAR_WIN == 0.5F && JoinTrial.SPAR_WIN > JoinTrial.SPAR_FLOOR && JoinTrial.SPAR_COUNTDOWN == 60
            && JoinTrial.SPAR_TICKS == 2400, "half of him before a quarter of them; three seconds' count, two minutes at most");
        // The ambush.
        check(!JoinTrial.calledIn(Long.MIN_VALUE, 1000000L) && !JoinTrial.calledIn(100L, 700L) && JoinTrial.calledIn(100L, 701L) && JoinTrial.AMBUSH_CLOCK == 600,
            "no clock before the first blow; both down within thirty seconds of it, or they call it in");
        check(JoinTrial.AMBUSH_SIZE == 2 && JoinTrial.AMBUSH_NEAR >= 32.0 && JoinTrial.AMBUSH_FAR > JoinTrial.AMBUSH_NEAR, "the patrol: the CCG's pair, out of sight");
        // The ring.
        check(JoinTrial.showtime(13000L) && !JoinTrial.showtime(12999L) && JoinTrial.showtime(22999L) && !JoinTrial.showtime(23000L)
            && JoinTrial.showtime(24000L * 5L + 18000L) && !JoinTrial.showtime(24000L * 5L + 6000L), "shows are at night");
        check(JoinTrial.RING_ACTS == 3 && JoinTrial.RING_ACT == 600 && JoinTrial.RING_IN_FAR < JoinTrial.RING_REACH && JoinTrial.RING_TOP_TIER <= 1,
            "three volunteers, thirty seconds each, stepping in on the stage; none over C rank");
        // The gas.
        check(!JoinTrial.gasStarts(15.9) && JoinTrial.gasStarts(16.0) && JoinTrial.gasStarts(48.0) && !JoinTrial.gasStarts(48.1),
            "the gas starts out past the gate, in reach of it");
        check(JoinTrial.GAS_REACHED < JoinTrial.GAS_START && JoinTrial.GAS_TICKS == 1200, "to Banjou within a minute of it starting");
        // The gate.
        check(JoinTrial.nextWave(0, 0, 0L) && !JoinTrial.nextWave(1, 3, 899L) && JoinTrial.nextWave(1, 0, 10L) && JoinTrial.nextWave(1, 2, 900L)
            && !JoinTrial.nextWave(2, 0, 5000L), "the first wave at once; the next when it is down or after 45 s; never a third");
        check(JoinTrial.GATE_WAVES == 2 && JoinTrial.GATE_WAVE == 3 && JoinTrial.GATE_NEAR > Math.hypot(28.0, 19.0) / 2.0
            && JoinTrial.GATE_ZONE >= JoinTrial.GATE_FAR, "two waves of three, coming from outside the walls; the gate's ground takes them in");
        // The shift.
        check(!JoinTrial.shiftLost(1) && JoinTrial.shiftLost(2) && JoinTrial.SHIFT_CUSTOMERS == 3 && JoinTrial.SHIFT_PATIENCE == 1200,
            "three regulars served; two walking out loses the shift; a minute each");
        com.hex.kagune.control.kitchen.Dish blend = null;
        for (com.hex.kagune.control.kitchen.Dish dish : com.hex.kagune.control.kitchen.Menu.DISHES) {
            if (("hexkagune:" + dish.id()).equals(JoinTrial.CUP)) {
                blend = dish;
            }
        }
        check(blend != null && blend.kind() == com.hex.kagune.control.kitchen.Dish.Kind.DRINK && blend.makes() == 1, "the cup served is the cookbook's Anteiku Blend");
        if (blend != null) {
            Map<String, Integer> need = new java.util.HashMap<>();
            for (String ingredient : blend.recipe().ingredients()) {
                need.merge(ingredient, JoinTrial.SHIFT_CUSTOMERS, Integer::sum);
            }
            Map<String, Integer> given = new java.util.HashMap<>();
            for (String[] making : JoinTrial.MAKINGS) {
                given.put(making[0], Integer.parseInt(making[1]));
            }
            check(need.equals(given), "Ken's makings brew exactly three blends, by the cookbook's own recipe: " + need + " / " + given);
        }
        // The café's raid.
        check(!JoinTrial.breaks(0.0F, 20.0F) && !JoinTrial.breaks(-3.0F, 20.0F) && JoinTrial.breaks(8.0F, 20.0F) && !JoinTrial.breaks(8.1F, 20.0F)
            && JoinTrial.breaks(1.0F, 20.0F) && JoinTrial.CAFE_BREAK == 0.4F && JoinTrial.CAFE_RAIDERS == 3,
            "an investigator left alive at two fifths or under breaks off; a dead one never did");
        // The hunt.
        check(JoinTrial.HUNT_NEAR >= 40.0 && JoinTrial.HUNT_FAR > JoinTrial.HUNT_NEAR && JoinTrial.HUNT_TOP_TIER <= 3 && JoinTrial.PARTNER_CLOSE < 6.0,
            "the reported ghoul out of sight, A rank at most; the partner at their side");
        // The academy's test.
        java.util.Set<Integer> asked = new java.util.HashSet<>();
        java.util.Set<String> firsts = new java.util.HashSet<>();
        for (long day = 0; day < 200; day++) {
            for (long mine = -3; mine < 3; mine++) {
                int[] drawn = JoinTrial.draw(day, mine * 7919L);
                check(drawn.length == JoinTrial.EXAM_ASKED && drawn[0] != drawn[1] && drawn[1] != drawn[2] && drawn[0] != drawn[2],
                    "three questions, all different");
                for (int q : drawn) {
                    check(q >= 0 && q < JoinTrial.QUESTIONS.length, "a question of the academy's");
                    asked.add(q);
                }
                firsts.add(drawn[0] + "," + drawn[1] + "," + drawn[2]);
            }
        }
        check(asked.size() == JoinTrial.QUESTIONS.length && firsts.size() > 100 && JoinTrial.QUESTIONS.length >= 3 * JoinTrial.EXAM_ASKED,
            "every question comes up; the sittings differ day to day and player to player");
        check(java.util.Arrays.equals(JoinTrial.draw(9L, 4L), JoinTrial.draw(9L, 4L)), "the same sitting all day (a page closed and opened again)");
        for (JoinTrial.Question q : JoinTrial.QUESTIONS) {
            check(!q.asked().isEmpty() && !q.right().equals(q.wrong1()) && !q.right().equals(q.wrong2()) && !q.wrong1().equals(q.wrong2())
                && q.answer(0).equals(q.right()), "a question with three different answers, the right one first: " + q.asked());
        }
        check(JoinTrial.right(0) && !JoinTrial.right(1) && !JoinTrial.right(2) && !JoinTrial.right(-1), "only the right one is right");
        check(!JoinTrial.maySit(40L, 40L) && JoinTrial.maySit(41L, 40L) && JoinTrial.maySit(40L, Long.MIN_VALUE), "one wrong and it is sat again tomorrow");
        check(JoinTrial.way(0.0, -42.0).equals("42 blocks north") && JoinTrial.way(30.0, 30.0).equals("42 blocks south-east") && JoinTrial.way(2.0, 2.0).equals("here")
            && JoinTrial.way(-10.0, 0.0).equals("10 blocks west") && JoinTrial.way(20.0, -20.0).equals("28 blocks north-east"), "the way said as the game's compass has it");
        check(JoinTrial.clock(1300L).equals("1:05") && JoinTrial.clock(0L).equals("0:00") && JoinTrial.clock(-5L).equals("0:00") && JoinTrial.clock(19L).equals("0:01"),
            "the time left, minutes and seconds");
        // Their words.
        String[] moments = {"offer", "start", "on", "won", "lost", "draw", "fled", "died", "called", "slow", "booed", "masked", "walked", "killed", "partner", "night"};
        java.util.Set<String> offers = new java.util.HashSet<>();
        java.util.Set<String> buttons = new java.util.HashSet<>();
        for (JoinTrial.Kind kind : JoinTrial.Kind.values()) {
            for (String moment : moments) {
                for (int seed = 0; seed < 4; seed++) {
                    String line = JoinVoice.say(kind, moment, seed);
                    check(line != null && line.length() > 8 && !line.contains("null"), "a line for " + kind + " " + moment);
                }
            }
            offers.add(JoinVoice.say(kind, "offer", 0));
            buttons.add(JoinVoice.start(kind));
            check(!JoinVoice.say(kind, "offer", 0).equals(JoinVoice.say(kind, "start", 0)) && !JoinVoice.say(kind, "won", 0).equals(JoinVoice.say(kind, "died", 0)),
                kind + ": offered, begun, won and lost each in their own words");
            check(JoinVoice.ask(kind).endsWith("...") && JoinVoice.start(kind).length() < 40, kind + ": its buttons short");
        }
        check(offers.size() == JoinTrial.Kind.values().length && buttons.size() == JoinTrial.Kind.values().length, "every trial put and begun in words of its own");
        check(JoinVoice.say(JoinTrial.Kind.SPAR, "start", 0).contains("Three... two...") && JoinVoice.say(JoinTrial.Kind.CAFE, "offer", 0).contains("don't kill")
            && JoinVoice.say(JoinTrial.Kind.GAS, "offer", 0).contains("No mask") && JoinVoice.say(JoinTrial.Kind.HUNT, "offer", 0).contains("partner back alive"),
            "each trial's rule said by its leader");
        java.util.Set<String> scenes = new java.util.HashSet<>();
        for (int gang = 1; gang <= 5; gang++) {
            scenes.add(JoinVoice.scene(gang, 0));
        }
        check(scenes.size() == 5 && JoinVoice.scene(GangPlayer.AOGIRI, 0).contains("Aogiri") && JoinVoice.scene(GangPlayer.ANTEIKU, 0).contains("Anteiku")
            && JoinVoice.scene(GangPlayer.CCG, 0).contains("Investigator"), "a scene of their own for each leader");
        for (String moment : new String[] {"job", "job_on", "case"}) {
            check(!JoinVoice.roma(moment, 1).isEmpty(), "Roma's job: " + moment);
        }
        for (String moment : new String[] {"exam", "next", "failed", "passed", "today"}) {
            check(!JoinVoice.general(moment, 1).isEmpty(), "the General's test: " + moment);
        }
        check(JoinVoice.roma("job", 0).contains("mask") && JoinVoice.general("failed", 0).contains("omorrow"), "the job in her face; tomorrow after a fail");
        {
            String join = read("src/main/java/com/hex/kagune/control/server/JoinServer.java");
            String gangs = read("src/main/java/com/hex/kagune/control/server/GangServer.java");
            String eto = read("src/main/java/com/hex/kagune/control/server/EtoBrain.java");
            String agents = read("src/main/java/com/hex/kagune/control/server/AgentBrain.java");
            String ghouls = read("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java");
            String talk = read("src/main/java/com/hex/kagune/control/server/GhoulNpcTalk.java");
            String etoServer = read("src/main/java/com/hex/kagune/control/server/EtoServer.java");
            // The spar.
            int hurt = join.indexOf("public static void hurt(LivingHurtEvent event) {");
            check(join.contains("@SubscribeEvent(priority = EventPriority.LOWEST)\n    public static void hurt(LivingHurtEvent event) {"),
                "the spar's blows are held last of all (what lands, as every other handler leaves it)");
            check(within(join, hurt, "JoinTrial.clamp(player.m_21223_(), player.m_21233_(), event.getAmount(), JoinTrial.SPAR_FLOOR);", 700)
                && within(join, hurt, "end(player, live, false, \"lost\");", 1000), "Ayato's blows never take them under the floor; the one that would, loses it");
            check(within(join, hurt, "JoinTrial.clamp(npc.m_21223_(), npc.m_21233_(), event.getAmount(), JoinTrial.SPAR_WIN);", 1700)
                && within(join, hurt, "end(player, live, true, \"won\");", 2000), "theirs never take him under half; the one that would, wins it");
            check(within(join, hurt, "event.setAmount(0.0F); // not yet", 1500), "no blow lands on him during the count");
            check(eto.contains("partnerOf(mind, player) && !JoinServer.sparring(player, npc)))) {")
                && eto.contains("(!partnerOf(mind, player) || JoinServer.sparring(player, npc)) && !spared(player, npc)")
                && eto.contains("if (attacker instanceof ServerPlayer player && !JoinServer.sparring(player, npc)) {"),
                "a spar: fought though his partner; no regard lost, no grudge kept for its blows");
            check(join.contains("leader.m_21153_(leader.m_21233_()); // a fair fight") && join.contains("if (player.m_21223_() < player.m_21233_() * 0.5F) {"),
                "he starts it whole; not with someone half dead");
            int over = join.indexOf("static void end(ServerPlayer player, Live live, boolean won, String moment) {");
            check(within(join, over, "ayato.mind.grudges.remove(player.m_20148_());", 1200) && within(join, over, "ayato.mind.target = 0;", 1200),
                "the spar over: he lets them go, nothing held against them");
            check(within(join, over, "g.advance(gang, JoinTrial.step(live.kind), JoinTrial.after(live.kind));", 1800)
                && within(join, over, "clear(level, live, now);", 600), "won, the way in moves on (only from its own step); either way its bodies go");
            // The ambush.
            int ambush = join.indexOf("static void ambush(ServerPlayer player, Live live, ServerLevel level, long now) {");
            check(within(join, ambush, "if (JoinTrial.calledIn(live.first, now)) {\n            end(player, live, false, \"called\");", 400)
                && within(join, hurt, "live.first = player.m_9236_().m_46467_(); // the first blow", 3500)
                && within(join, ambush, "if (live.marked.isEmpty()) {\n            end(player, live, true, \"won\");", 200),
                "the first blow starts the clock; both down, won; too slow, they call it in");
            // The ring.
            int start = join.indexOf("static String start(ServerPlayer player, EtoNpc leader, JoinTrial.Kind kind, int seed) {");
            check(within(join, start, "if (!JoinTrial.showtime(level.m_46468_())) {\n                    return JoinVoice.say(kind, \"night\", seed);", 2400),
                "no show by day");
            int ring = join.indexOf("static void ring(ServerPlayer player, Live live, ServerLevel level, long now) {");
            check(within(join, ring, "distance(player, live.anchor) > JoinTrial.RING_REACH, JoinTrial.RING_OUT)", 300)
                && within(join, ring, "if (now - live.currentAt > JoinTrial.RING_ACT) {\n                end(player, live, false, \"booed\");", 1000)
                && within(join, ring, "Math.min(rolled.tier(), JoinTrial.RING_TOP_TIER)", 2600), "off the stage, over; an act too long, booed; volunteers C rank at most");
            // The gas.
            int gas = join.indexOf("static void gas(ServerPlayer player, Live live, ServerLevel level, long now) {");
            check(within(join, gas, "if (JoinTrial.gasStarts(from)) {", 500) && within(join, gas, "if (GangServer.gasMaskOn(player)) {\n            end(player, live, false, \"masked\");", 1200)
                && within(join, gas, "if (from <= JoinTrial.GAS_REACHED) {", 1400) && within(join, gas, "MobEffects.f_19614_, 50, 0", 2400),
                "the gas starts past the gate; a gas mask on loses it; reaching him wins; poison while in it");
            // The gate.
            int gate = join.indexOf("static void gate(ServerPlayer player, Live live, ServerLevel level, long now) {");
            check(within(join, gate, "if (JoinTrial.nextWave(live.wave, standing, now - live.waveAt)) {", 500)
                && within(join, gate, "if (live.wave >= JoinTrial.GATE_WAVES && standing == 0) {", 1300), "the waves; all down, the gate held");
            check(join.contains("NpcIdentity.roll(mastery, () -> player.m_217043_().m_188500_()).ganged(gang, faces[i]);")
                && join.contains("com.hex.kagune.control.npc.Gang gang = com.hex.kagune.control.npc.Gang.CLOWNS;"), "the raid is the Clowns (rivals of the Gas Masks on sight)");
            // The shift.
            int serve = join.indexOf("static boolean serve(ServerPlayer player, GhoulNpc npc) {");
            check(within(join, serve, "held.m_41720_() != cup", 600) && within(join, serve, "held.m_41774_(1);", 1000) && within(join, serve, "live.done++;", 1300),
                "a regular served only a cup of the blend, taken from the hand");
            check(talk.contains("if (JoinServer.serve(player, npc)) {\n                return;") && talk.indexOf("JoinServer.serve(player, npc)") < talk.indexOf("open(player, npc);\n        }"),
                "a right click on a waiting regular serves before any talk opens");
            check(within(join, join.indexOf("case SHIFT -> {"), "makings(player);", 80) && join.contains("kept.m_128454_(MAKINGS_DAY) == today"),
                "Ken hands over the makings, once a day");
            check(ghouls.contains("calm(npc, mind, now, false);\n        // 4.1.79: a regular of Anteiku's shift waits at their seat for their cup.\n        if (JoinServer.held(npc, level, now)) {"),
                "a regular waits at their seat");
            // The café's raid.
            int died = join.indexOf("public static void died(LivingDeathEvent event) {");
            check(within(join, died, "case CAFE -> {\n                    if (theirs) {\n                        end(player, live, false, \"killed\");", 3000),
                "an investigator of the raid killed by them loses it");
            check(within(join, hurt, "JoinTrial.breaks(hurt.m_21223_() - event.getAmount(), hurt.m_21233_())", 3000)
                && join.contains("if (OWNER.containsKey(event.getEntity().m_20148_()) && event.getEntity().getPersistentData().m_128471_(BROKE)) {\n            event.setCanceled(true);"),
                "one left low breaks off, and nothing lands on him as he goes");
            // The hunt.
            check(within(join, died, "if (id.equals(live.partner)) {\n                        end(player, live, false, \"partner\");", 3600)
                && within(join, died, "} else if (id.equals(live.target)) {", 3800), "the partner dead, lost; the target dead, won");
            int single = join.indexOf("static CcgAgent single(ServerLevel level, double x, double z, ServerPlayer player) {");
            check(within(join, single, "saveAlone(agent, player);", 300) && join.contains("AgentBrain.save(agent, com.hex.kagune.control.npc.AgentRules.regularLook(") && join.contains(".m_188500_()), true, null);"),
                "a lone investigator is saved before he wakes (no partner of his own made for him)");
            int wanted = agents.indexOf("static boolean wanted(CcgAgent agent");
            check(within(agents, wanted, "if (body instanceof ServerPlayer player && JoinServer.partnered(agent, player)) {\n            return false;", 800)
                && agents.indexOf("JoinServer.partnered(agent, player)", wanted) < agents.indexOf("Long until = mind.grudges.get(body.m_20148_());", wanted),
                "the partner never goes for the one he was assigned to (before any mark or grudge)");
            check(agents.contains("if (JoinServer.partnerWalk(agent, level, now)) {\n            return;"), "the partner keeps at their side");
            // The bodies.
            check(ghouls.contains("&& JoinServer.orphan(npc)) {\n            npc.m_146870_();") && agents.contains("&& JoinServer.orphan(agent)) {\n            agent.m_146870_();"),
                "a trial's body with no trial live is taken away (a restart, a trial lost while it lay unloaded)");
            check(ghouls.contains("who.gang() == 0 && !JoinServer.marked(npc);") && ghouls.contains("|| npc.m_21532_() || JoinServer.marked(npc)) {"),
                "a volunteer or the reported ghoul neither hides by day nor slips away at dawn");
            check(join.contains("return marked(body) && !OWNER.containsKey(body.m_20148_());"), "an orphan: marked, and no trial owns it");
            // The talk.
            check(gangs.contains("return trial(player, npc, kind, key, seed); // 4.1.79") && etoServer.contains("GangServer.talk(player, npc, npc.persona(), key, c.seed());"),
                "a step that is a trial is offered, begun and asked about through the leader's talk");
            int exam = gangs.indexOf("static Said exam(ServerPlayer player, GangPlayer g, String key, int seed) {");
            check(within(gangs, exam, "if (!com.hex.kagune.control.npc.JoinTrial.right(answer)) {\n                JoinServer.failed(player);", 600)
                && within(gangs, exam, "if (state[3] >= com.hex.kagune.control.npc.JoinTrial.EXAM_ASKED) {", 1200)
                && within(gangs, exam, "moved(player, g, GangPlayer.CCG, GangPlayer.CCG_EXAM, GangPlayer.CCG_HUNT);", 1400),
                "a wrong answer fails the test till tomorrow; three right, the hunt");
            check(gangs.contains("if (investigator && clownMaskOn(player)) {\n                // 4.1.79: the Clowns' job")
                && gangs.contains("now = now.advance(GangPlayer.CLOWNS, GangPlayer.CLOWN_JOB, GangPlayer.CLOWN_CASE);"),
                "Roma's job counts only in one of the Clowns' masks");
            check(gangs.contains("int aogiri = k.m_128441_(AOGIRI_STEP) ? k.m_128451_(AOGIRI_STEP) : GangPlayer.aogiriFrom(k.m_128471_(AOGIRI_QUEST), k.m_128451_(CCG));")
                && gangs.contains("k.m_128405_(AOGIRI_STEP, g.aogiriStep());"), "Aogiri's step kept, and read from 4.1.50's quest for a player saved before");
            int welcome = gangs.indexOf("static Said welcome(ServerPlayer player, EtoNpc npc, int gang, GangPlayer g, int seed) {");
            check(within(gangs, welcome, "set(player, g.join(gang));", 200) && within(gangs, welcome, "EtoBrain.gesture(npc.mind, sceneGesture(gang),", 600)
                && within(gangs, welcome, "return new Said(com.hex.kagune.control.npc.JoinVoice.scene(gang, seed), null);", 1200),
                "the end of every way in: the leader's scene (their move, their words) as they join");
            check(gangs.split("return welcome\\(player, npc, GangPlayer\\.", -1).length == 6, "all five ways in end in the scene");
            check(join.contains("NpcTemper.DRIFTER, Math.min(rolled.tier(), JoinTrial.RING_TOP_TIER)"),
                "a volunteer is a drifter: it hunts no other ghoul (never Roma's Clowns about the tent)");
            int march = join.indexOf("static void march(ServerLevel level, Live live, ServerPlayer player) {");
            check(within(join, march, "ghoul.mind.prey == 0 && ghoul.mind.leavingAt == Long.MIN_VALUE", 500) && within(join, march, "agent.mind.prey == 0", 900)
                && within(join, gate, "march(level, live, player);", 2200) && within(join, join.indexOf("static void cafe(ServerPlayer player"), "march(level, live, player);", 1500),
                "the raid's bodies with nothing to fight march on them (the gate's Clowns, the café's investigators)");
            int huntAt = join.indexOf("static boolean hunt(ServerLevel level, ServerPlayer player, Live live) {");
            check(within(join, huntAt, "BlockPos beside = floor(level, feet, 1.5, 3.5, false, false, player);", 1800)
                && within(join, huntAt, "partner = AgentSpawner.placeAt(level, beside.m_123341_(), beside.m_123342_(), beside.m_123343_());", 2000),
                "the partner made on their own floor (the General's office is up in the tower)");
            check(gangs.contains("kept(player).m_128473_(JoinServer.LENT);") && within(gangs, gangs.indexOf("private static void joined("), "!kept(player).m_128471_(JoinServer.LENT)", 300),
                "Roma's lent mask is theirs once in (no second one), and the loan forgotten for a way in begun again");
            check(gangs.contains("\"A DOVE? In my circus?") && gangs.contains("\"Anteiku? The coffee people? Cute."),
                "Roma answers one of Anteiku or the CCG as what they are (not as a Gas Mask)");
            check(!join.isEmpty() && !gangs.isEmpty() && !eto.isEmpty() && !agents.isEmpty() && !ghouls.isEmpty() && !talk.isEmpty() && !etoServer.isEmpty(),
                "read the trials' files");
        }
    }

    static void joining() {
        // 4.1.79: Ayato's way in: the spar, the ambush, back to him; a player saved mid 4.1.50's "Kill 5 CCG" read sensibly.
        GangPlayer g = GangPlayer.NEW;
        int aogiri = GangPlayer.AOGIRI;
        check(!g.member(aogiri) && g.advance(aogiri, GangPlayer.AOGIRI_SPAR, GangPlayer.AOGIRI_AMBUSH) == g, "nothing asked, nothing moves");
        g = g.advance(aogiri, GangPlayer.AOGIRI_NOT_YET, GangPlayer.AOGIRI_SPAR);
        check(g.aogiriStep() == GangPlayer.AOGIRI_SPAR && g.advance(aogiri, GangPlayer.AOGIRI_AMBUSH, GangPlayer.AOGIRI_DONE) == g, "asked: the spar first");
        g = g.advance(aogiri, GangPlayer.AOGIRI_SPAR, GangPlayer.AOGIRI_AMBUSH).advance(aogiri, GangPlayer.AOGIRI_AMBUSH, GangPlayer.AOGIRI_DONE);
        check(g.aogiriStep() == GangPlayer.AOGIRI_DONE && !g.member(aogiri), "the spar, the ambush: back to Ayato");
        check(GangPlayer.aogiriFrom(false, 3) == GangPlayer.AOGIRI_NOT_YET && GangPlayer.aogiriFrom(true, 2) == GangPlayer.AOGIRI_AMBUSH
            && GangPlayer.aogiriFrom(true, 5) == GangPlayer.AOGIRI_DONE && GangPlayer.aogiriFrom(true, 9) == GangPlayer.AOGIRI_DONE,
            "4.1.50's quest read: never begun, nothing; begun, the ambush; five doves, done");
        g = g.join(aogiri);
        check(g.member(aogiri) && g.aogiriStep() == GangPlayer.AOGIRI_NOT_YET && g.ownKills() == 0, "joined: Aogiri's");
        for (int i = 0; i < 10; i++) {
            g = g.killedOwn();
        }
        check(g.member(aogiri) && g.ownKills() == 10, "ten of their own: still theirs (\"over 10\")");
        g = g.killedOwn();
        check(!g.member(aogiri) && g.gang() == GangPlayer.NONE && g.banned(aogiri) && !g.banned(GangPlayer.CLOWNS), "the eleventh: banned, out, its tag gone");
        // Roma's: the joke (kept), the ring, the job in her face, the case.
        GangPlayer c = GangPlayer.NEW;
        int clowns = GangPlayer.CLOWNS;
        check(c.advance(clowns, GangPlayer.CLOWN_RING, GangPlayer.CLOWN_JOB) == c, "no show before she has laughed");
        c = c.advance(clowns, GangPlayer.CLOWN_NOT_YET, GangPlayer.CLOWN_RING);
        check(c.clownStep() == GangPlayer.CLOWN_RING && c.advance(clowns, GangPlayer.CLOWN_JOB, GangPlayer.CLOWN_CASE) == c, "she laughed: the ring next");
        c = c.advance(clowns, GangPlayer.CLOWN_RING, GangPlayer.CLOWN_JOB).advance(clowns, GangPlayer.CLOWN_JOB, GangPlayer.CLOWN_CASE);
        check(c.clownStep() == GangPlayer.CLOWN_CASE && GangPlayer.CLOWN_RING == 1 && GangPlayer.CLOWN_JOB == 2,
            "the ring, the job, then the case (the saved 1 and 2 keep their places)");
        c = c.join(clowns);
        check(c.member(clowns) && c.clownStep() == GangPlayer.CLOWN_NOT_YET, "joined: the Clowns'");
        // 4.1.54 (the owner: the Gas Masks joined by "Short chain, like Roma"): Banjou taken seriously; 4.1.79: the gas, the gate.
        GangPlayer m = GangPlayer.NEW;
        int gas = GangPlayer.GAS_MASKS;
        check(m.advance(gas, GangPlayer.GAS_WALK, GangPlayer.GAS_RAID) == m, "no gas before Banjou has been taken seriously");
        m = m.advance(gas, GangPlayer.GAS_NOT_YET, GangPlayer.GAS_WALK);
        check(m.gasStep() == GangPlayer.GAS_WALK && m.advance(gas, GangPlayer.GAS_NOT_YET, GangPlayer.GAS_WALK) == m, "taken seriously: the gas next (once)");
        m = m.advance(gas, GangPlayer.GAS_WALK, GangPlayer.GAS_RAID).advance(gas, GangPlayer.GAS_RAID, GangPlayer.GAS_DONE);
        check(m.gasStep() == GangPlayer.GAS_DONE && GangPlayer.GAS_WALK == 1 && GangPlayer.GAS_RAID == 2, "the gas, the gate, back to Banjou");
        m = m.join(gas);
        check(m.member(gas) && m.gasStep() == GangPlayer.GAS_NOT_YET && !m.member(clowns), "joined: the Gas Masks'");
        for (int i = 0; i < 11; i++) {
            m = m.killedOwn();
        }
        check(m.gang() == GangPlayer.NONE && m.banned(gas) && !m.banned(clowns), "the Gas Masks ban past ten of their own, as every gang");
        GangPlayer all = GangPlayer.NEW;
        for (int gang = 1; gang <= 5; gang++) {
            all = all.advance(gang, 0, JoinTrial.afterTalk(gang));
        }
        check(all.aogiriStep() == GangPlayer.AOGIRI_SPAR && all.clownStep() == GangPlayer.CLOWN_RING && all.gasStep() == GangPlayer.GAS_WALK
            && all.anteikuStep() == GangPlayer.ANTEIKU_SHIFT && all.generalStep() == GangPlayer.CCG_EXAM, "five ways in at once, each its own");
        check(all.join(GangPlayer.CCG).aogiriStep() == GangPlayer.AOGIRI_SPAR && all.join(GangPlayer.CCG).generalStep() == 0
            && all.killedOwn() == all, "joining one leaves the others where they were; no gang, no own to kill");
        GangPlayer turned = all.join(GangPlayer.CCG);
        for (int i = 0; i < 11; i++) {
            turned = turned.killedOwn();
        }
        check(turned.gang() == GangPlayer.NONE && turned.aogiriStep() == 0 && turned.clownStep() == 0 && turned.gasStep() == 0 && turned.anteikuStep() == 0
            && turned.generalStep() == 0, "banned: every way in begins again");
        anteiku();
        ccg();
        trials();
        // 4.1.67 (the owner: "Add a new Quit the gang button to each leader whenever the player joins it."; "kicked from the gang if the
        // player marries another gang leader, but can still join if the player was already married before joining."): out, never banned.
        com.hex.kagune.control.npc.GangPlayer q = GangPlayer.NEW.advance(GangPlayer.CLOWNS, 0, GangPlayer.CLOWN_RING).join(GangPlayer.CLOWNS)
            .killedOwn().killedOwn();
        com.hex.kagune.control.npc.GangPlayer out = q.quit();
        check(out.gang() == com.hex.kagune.control.npc.GangPlayer.NONE && out.ownKills() == 0 && !out.banned(com.hex.kagune.control.npc.GangPlayer.CLOWNS)
            && out.banned() == q.banned(), "quitting: out of the Clowns, their tag gone, not banned");
        check(out.clownStep() == com.hex.kagune.control.npc.GangPlayer.CLOWN_NOT_YET && out.aogiriStep() == 0
            && out.join(com.hex.kagune.control.npc.GangPlayer.CLOWNS).member(com.hex.kagune.control.npc.GangPlayer.CLOWNS),
            "quitting: the way in begins again, and they may come back by it");
        check(com.hex.kagune.control.npc.GangPlayer.NEW.quit() == com.hex.kagune.control.npc.GangPlayer.NEW, "no gang: nothing to quit");
        int[] leaders = {com.hex.kagune.control.npc.GangPlayer.AOGIRI, com.hex.kagune.control.npc.GangPlayer.CLOWNS, com.hex.kagune.control.npc.GangPlayer.GAS_MASKS,
            com.hex.kagune.control.npc.GangPlayer.ANTEIKU, com.hex.kagune.control.npc.GangPlayer.CCG};
        java.util.Set<String> said = new java.util.HashSet<>();
        int demanded = 0;
        for (int leads : leaders) {
            check(com.hex.kagune.control.npc.GangWords.wantOut(leads).contains(com.hex.kagune.control.npc.GangPlayer.title(leads)), "asking out names the gang");
            said.add(com.hex.kagune.control.npc.GangWords.asked(leads));
            said.add(com.hex.kagune.control.npc.GangWords.stay(leads));
            said.add(com.hex.kagune.control.npc.GangWords.gone(leads));
            for (int gang : leaders) {
                if (gang == leads) {
                    continue;
                }
                java.util.Set<String> lines = new java.util.HashSet<>();
                for (int seed = 0; seed < 9; seed++) {
                    String line = com.hex.kagune.control.npc.GangWords.demand(leads, gang, seed);
                    lines.add(line);
                    check(line.toLowerCase(java.util.Locale.ROOT).contains(com.hex.kagune.control.npc.GangPlayer.title(gang).toLowerCase(java.util.Locale.ROOT))
                        && line.toLowerCase(java.util.Locale.ROOT).contains("quit") || line.contains("Leave") || line.contains("leave"),
                        "the spouse names the gang to quit: " + line);
                }
                check(lines.size() == 3, "three demands a leader, in turn");
            }
        }
        for (int seed = 0; seed < 300; seed++) {
            demanded += com.hex.kagune.control.npc.GangWords.demands(seed) ? 1 : 0;
        }
        check(said.size() == 15 && demanded == 200, "each leader their own words to go and to stay (4.1.70: Ken too; 4.1.71: the General); the "
            + "spouse brings it up two times in three");
        com.hex.kagune.control.npc.GangPlayer banned = m.join(com.hex.kagune.control.npc.GangPlayer.AOGIRI).quit();
        check(banned.banned(com.hex.kagune.control.npc.GangPlayer.GAS_MASKS), "quitting never lifts a ban from before");
        try {
            String gangs = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GangServer.java"));
            String eto = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/EtoServer.java"));
            String commands = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/ControlCommands.java"));
            int memberLines = gangs.indexOf("static String memberLine(int gang, int seed) {");
            check(gangs.contains("return quit(player, g, gang, key, seed); // 4.1.67") && gangs.contains("return new Said(memberLine(gang, seed), wayOut(gang));")
                && within(gangs, memberLines, "case GangPlayer.AOGIRI ->", 200) && within(gangs, memberLines, "case GangPlayer.CLOWNS ->", 400)
                && within(gangs, memberLines, "case GangPlayer.GAS_MASKS ->", 800) && within(gangs, memberLines, "case GangPlayer.ANTEIKU ->", 1200)
                && within(gangs, memberLines, "default -> pick(seed, \"The bureau's holding.", 1800)
                && gangs.contains("\"gang:quit:yes\"") && gangs.contains("\"gang:stay\"") && gangs.contains("set(player, g.quit());"),
                "each leader's member talk offers the way out (Ayato, Roma, Banjou, 4.1.70: Ken, 4.1.71: the General), asked twice, then out");
            check(eto.contains("UniqueNpcs.pair(level, npc.persona().key(), player, Romance.Stage.MARRIED);\n        GangServer.married(player, npc.persona());")
                && commands.contains("GangServer.married(player, persona);"), "a wedding to another gang's leader takes them out of theirs (by ring or by command)");
            check(gangs.contains("if (!leads(spouse) || g.gang() == GangPlayer.NONE || g.gang() == theirs) {"),
                "married to a leader first, joining another gang after is never stopped");
            check(eto.contains("GangServer.spouseDemand(player, npc.persona(), context.seed())") && gangs.contains("nag(player, g, now); // 4.1.67")
                && gangs.contains("static final long NAG_EVERY = 20L * 150L;"), "a leader's spouse in another gang hears it on greeting and, near, every few minutes");
        } catch (java.io.IOException failure) {
            check(false, "the gang server's source: " + failure);
        }
        check(com.hex.kagune.control.npc.GangPlayer.title(com.hex.kagune.control.npc.GangPlayer.GAS_MASKS).equals("the Gas Masks")
            && com.hex.kagune.control.npc.GangPlayer.GAS_MASKS == 3, "the Gas Masks: gang 3");
        check(com.hex.kagune.control.npc.GangPlayer.named(com.hex.kagune.control.npc.GangPlayer.AOGIRI, "Ken").equals("\u00a7f\ue000 \u00a74Ken\u00a7r")
            && com.hex.kagune.control.npc.GangPlayer.named(com.hex.kagune.control.npc.GangPlayer.CLOWNS, "Ken").equals("\u00a7f\ue001 \u00a79Ken\u00a7r")
            && com.hex.kagune.control.npc.GangPlayer.named(com.hex.kagune.control.npc.GangPlayer.GAS_MASKS, "Ken").equals("\u00a7f\ue002 \u00a7aKen\u00a7r")
            && com.hex.kagune.control.npc.GangPlayer.named(com.hex.kagune.control.npc.GangPlayer.NONE, "Ken").equals("Ken"),
            "(symbol) (space) (name): the emblem in its own colours, Aogiri's name dark red, the Clowns' blue, the Gas Masks' green (4.1.54: "
                + "\"Toxic logo, green name\"); a gangless name as it is");
        NpcIdentity rolled = NpcIdentity.roll(300, new java.util.Random(11)::nextDouble);
        check(com.hex.kagune.control.npc.GangPlayer.of(rolled.ganged(com.hex.kagune.control.npc.Gang.CLOWNS, 2)) == com.hex.kagune.control.npc.GangPlayer.CLOWNS
            && com.hex.kagune.control.npc.GangPlayer.of(rolled.ganged(com.hex.kagune.control.npc.Gang.GAS_MASKS, 2)) == com.hex.kagune.control.npc.GangPlayer.GAS_MASKS
            && com.hex.kagune.control.npc.GangPlayer.of(rolled) == com.hex.kagune.control.npc.GangPlayer.NONE, "a Clown is the Clowns'; 4.1.54: a Gas Mask the Gas Masks'");
        try {
            String font = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/assets/minecraft/font/default.json")));
            check(font.contains("\"hexkagune:font/aogiri_tree.png\"") && font.contains("\"hexkagune:font/clown_face.png\"") && font.contains("\\ue000")
                && font.contains("\\ue001") && font.contains("\"hexkagune:font/gas_toxic.png\"") && font.contains("\\ue002"), "the emblems in the game's own font");
            for (String glyph : new String[] {"aogiri_tree", "clown_face", "gas_toxic"}) {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new java.io.File("src/main/resources/assets/hexkagune/textures/font/" + glyph + ".png"));
                int drawn = 0;
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        drawn += image.getRGB(x, y) >>> 24 == 255 ? 1 : 0;
                    }
                }
                check(image.getWidth() == 16 && image.getHeight() == 16 && drawn > 60, glyph + ": a 16 x 16 emblem (" + drawn + " pixels)");
            }
            String brain = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java")));
            int own = brain.indexOf("if (stance == GangServer.OWN_GANG) {");
            int grudge = brain.indexOf("Long until = mind.grudges.get(body.m_20148_());");
            check(own > 0 && own < grudge && brain.indexOf("return GangServer.turned(player);", own) < grudge
                && brain.indexOf("if (GangServer.threatens(npc, who, body)) {") < grudge,
                "its own never gone for, grudge or not (4.1.52: unless the bar is empty and they stayed in the hideout past the warning); the banned "
                    + "on sight; what goes for its own gone for");
            check(brain.contains("GangServer.stance(member, who) == GangServer.OWN_GANG && !GangServer.turned(member)) {\n            GangServer.struckOwn(member, now);")
                && brain.contains("GangServer.struckOwn(member, now); // 4.1.52: their reputation down a little\n            return;"),
                "its own's blows never held against them (4.1.52: they cost reputation)");
            String server = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GangServer.java")));
            check(server.contains("public static void name(PlayerEvent.NameFormat event) {") && server.contains("player.refreshDisplayName();")
                && server.contains("k.m_128405_(GANG, g.gang());") && server.contains("root.m_128469_(\"PlayerPersisted\")"),
                "the name everyone sees carries the gang; kept with the player through death");
        } catch (java.io.IOException failure) {
            check(false, "read the gangs' files: " + failure);
        }
    }

    static void gangs() {
        com.hex.kagune.control.npc.Gang clowns = com.hex.kagune.control.npc.Gang.CLOWNS;
        com.hex.kagune.control.npc.Gang gas = com.hex.kagune.control.npc.Gang.GAS_MASKS;
        check(com.hex.kagune.control.npc.Gang.values().length == 4 && com.hex.kagune.control.npc.Gang.byOrdinal(0) == com.hex.kagune.control.npc.Gang.NONE
            && com.hex.kagune.control.npc.Gang.byOrdinal(1) == clowns && com.hex.kagune.control.npc.Gang.byOrdinal(2) == gas
            && com.hex.kagune.control.npc.Gang.byOrdinal(3) == com.hex.kagune.control.npc.Gang.CCG_DIVISION
            && com.hex.kagune.control.npc.Gang.byOrdinal(4) == com.hex.kagune.control.npc.Gang.NONE
            && com.hex.kagune.control.npc.Gang.byOrdinal(-1) == com.hex.kagune.control.npc.Gang.NONE, "none, the clowns, the gas masks, the CCG's division (by ordinal: the identity's two bits)");
        check(com.hex.kagune.control.npc.Gang.roaming(0.0) == clowns && com.hex.kagune.control.npc.Gang.roaming(0.49) == clowns
            && com.hex.kagune.control.npc.Gang.roaming(0.5) == gas && com.hex.kagune.control.npc.Gang.roaming(0.999) == gas, "either gang, even odds");
        int[] sizes = new int[6];
        java.util.Random random = new java.util.Random(4145);
        for (int i = 0; i < 3000; i++) {
            int size = com.hex.kagune.control.npc.Gang.size(random.nextDouble());
            check(size >= 3 && size <= 5, "a gang of " + size);
            sizes[Math.max(0, Math.min(5, size))]++;
        }
        check(sizes[3] > 800 && sizes[4] > 800 && sizes[5] > 800, "three, four and five about as often: " + java.util.Arrays.toString(sizes));
        check(com.hex.kagune.control.npc.Gang.size(0.0) == 3 && com.hex.kagune.control.npc.Gang.size(0.9999) == 5
            && com.hex.kagune.control.npc.Gang.size(1.0) == 5 && com.hex.kagune.control.npc.Gang.size(-1.0) == 3, "never fewer than three nor more than five");
        int[] first = new int[6];
        for (com.hex.kagune.control.npc.Gang gang : new com.hex.kagune.control.npc.Gang[] {clowns, gas}) {
            check(gang.masks().size() == 5 && new java.util.HashSet<>(gang.masks()).size() == 5, gang.title() + ": five masks, all different");
            check(gang.mask(0) == null && gang.mask(6) == null && gang.masks().get(0).equals(gang.mask(1)), gang.title() + ": its masks counted from one");
            for (int i = 0; i < 400; i++) {
                int size = com.hex.kagune.control.npc.Gang.size(random.nextDouble());
                int[] faces = gang.faces(size, random::nextDouble);
                java.util.Set<Integer> seen = new java.util.HashSet<>();
                for (int face : faces) {
                    check(face >= 1 && face <= 5, gang.title() + ": a mask of its own: " + face);
                    seen.add(face);
                }
                check(faces.length == size && seen.size() == size, gang.title() + ": every one of " + size + " in a different mask " + java.util.Arrays.toString(faces));
                if (gang == clowns) {
                    first[faces[0]]++;
                }
            }
        }
        check(first[1] > 30 && first[2] > 30 && first[3] > 30 && first[4] > 30 && first[5] > 30, "any of them leads in any mask: " + java.util.Arrays.toString(first));
        // Kept in the identity (every client is told which gang and which mask): bits 44 and 45, under the rank's flag.
        for (com.hex.kagune.control.npc.Gang gang : com.hex.kagune.control.npc.Gang.values()) {
            for (int mask = 0; mask <= 5; mask++) {
                if (gang.ccg() && mask > 0) {
                    continue;
                }
                for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
                    NpcIdentity who = new NpcIdentity(NpcIdentity.Kind.RINKAKU, NpcTemper.DRIFTER, tier, 13, 21, 0xBEEF, mask, false, gang.ordinal());
                    check(NpcIdentity.unpack(who.pack()).equals(who) && NpcIdentity.unpack(who.pack()).gangOf() == gang, "a gang's own saved and read back: " + who);
                }
            }
        }
        NpcIdentity rolled = NpcIdentity.roll(300, new java.util.Random(7)::nextDouble);
        NpcIdentity clown = rolled.ganged(clowns, 3);
        check(clown.gangOf() == clowns && clown.temper() == NpcTemper.DRIFTER && !clown.aogiri() && clown.tier() == rolled.tier()
            && clown.kind() == rolled.kind() && clown.skin() == rolled.skin() && clown.name() == rolled.name(), "one of a gang: the ghoul rolled, a drifter (starts nothing)");
        check("mask_clown_v3".equals(clown.maskItem()) && clown.tagName().endsWith("-rank (Clowns)"), "its mask its gang's, its name tag says so: " + clown.tagName());
        check("mask_gas".equals(rolled.ganged(gas, 1).maskItem()) && "mask_gas_v5".equals(rolled.ganged(gas, 5).maskItem())
            && rolled.ganged(gas, 2).tagName().endsWith("(Gas Masks)"), "the gas masks' own");
        check(clown.ranked(NpcRules.S).gangOf() == clowns && clown.ranked(NpcRules.S).mask() == 3, "ranked again, still of its gang");
        // 4.1.48 (the owner chose "Yes, rivals"): the Clowns and the Gas Masks fight on sight, either way round; nobody else is anyone's rival.
        for (com.hex.kagune.control.npc.Gang a : com.hex.kagune.control.npc.Gang.values()) {
            for (com.hex.kagune.control.npc.Gang b : com.hex.kagune.control.npc.Gang.values()) {
                check(a.rival(b) == (a == clowns && b == gas || a == gas && b == clowns), a + " and " + b + ": rivals only if the Clowns and the Gas Masks");
            }
        }
        check(new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.FERAL, NpcRules.S, 1, 2, 3, 2, false).gang() == 0, "every one before is of no gang");
        for (int old = 0; old < 3; old++) {
            long packed = 1L | 2L << 1 | (long)old << 4 | 3L << 6 | 17L << 10 | 4242L << 18 | 1L << 35 | 1L << 44 | 1L << 45;
            check(NpcIdentity.unpack(packed).gang() == 0, "an old save's ghoul (no rank flag) is of no gang, whatever its high bits");
        }
        check(new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.DRIFTER, 1, 1, 1, 1, 1, false, 9).gang() == 3
            && new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.DRIFTER, 1, 1, 1, 1, 1, false, -2).gang() == 0, "its gang held to the two bits");
        // Each mask its own paint on its base's model (drawn on the face: no item, nothing else on it).
        check("mask_clown".equals(com.hex.kagune.control.outfit.Outfits.geometryOf("mask_clown_v3"))
            && "mask_gas".equals(com.hex.kagune.control.outfit.Outfits.geometryOf("mask_gas_v5"))
            && "mask_clown".equals(com.hex.kagune.control.outfit.Outfits.geometryOf("mask_clown"))
            && "formal".equals(com.hex.kagune.control.outfit.Outfits.geometryOf("formal"))
            && "mask_v".equals(com.hex.kagune.control.outfit.Outfits.geometryOf("mask_v"))
            && "mask_clown_vx".equals(com.hex.kagune.control.outfit.Outfits.geometryOf("mask_clown_vx"))
            && com.hex.kagune.control.outfit.Outfits.geometryOf(null) == null, "a variant drawn with its base's model, any other mask with its own");
        java.util.Map<String, com.hex.kagune.control.outfit.OutfitModels.Model> models = java.util.Map.of();
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/main/resources" + com.hex.kagune.control.outfit.OutfitModels.RESOURCE))) {
            models = com.hex.kagune.control.outfit.OutfitModels.read(in);
        } catch (java.io.IOException failure) {
            check(false, "read the outfit models: " + failure);
        }
        java.io.File paints = new java.io.File("src/main/resources/assets/hexkagune/textures/outfit");
        for (com.hex.kagune.control.npc.Gang gang : new com.hex.kagune.control.npc.Gang[] {clowns, gas}) {
            String base = gang.masks().get(0);
            com.hex.kagune.control.outfit.OutfitModels.Model model = models.get(base);
            check(model != null && !model.on(com.hex.kagune.control.outfit.OutfitModels.HEAD, -1).isEmpty(), base + ": its model, on the head");
            java.util.Set<String> looks = new java.util.HashSet<>();
            for (String mask : gang.masks()) {
                check(!models.containsKey(mask) || mask.equals(base), mask + ": no model of its own (its base's)");
                check(com.hex.kagune.control.outfit.Outfits.geometryOf(mask).equals(base), mask + ": drawn with " + base + "'s model");
                try {
                    java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new java.io.File(paints, mask + ".png"));
                    java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(new java.io.File(paints, base + ".png"));
                    check(image != null && original != null && image.getWidth() == original.getWidth() && image.getHeight() == original.getHeight(),
                        mask + ": its paint, the size of its base's");
                    if (image == null || original == null) {
                        continue;
                    }
                    StringBuilder print = new StringBuilder();
                    int changed = 0;
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            int a = image.getRGB(x, y);
                            int b = original.getRGB(x, y);
                            check((a >>> 24 == 0) == (b >>> 24 == 0), mask + ": clear where its base is clear (" + x + ", " + y + ")");
                            changed += a != b ? 1 : 0;
                            print.append(Integer.toHexString(a));
                        }
                    }
                    check(mask.equals(base) || changed > 300, mask + ": a paint of its own (" + changed + " texels differ from " + base + "'s)");
                    looks.add(print.toString());
                } catch (java.io.IOException failure) {
                    check(false, mask + ": read its paint: " + failure);
                }
            }
            check(looks.size() == 5, gang.title() + ": five different looks");
        }
        try {
            String brain = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java")));
            String spawner = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcSpawner.java")));
            String layer = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/render/OutfitLayer.java")));
            String models2 = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/outfit/OutfitModels.java")));
            check(brain.contains("return body instanceof Mob mob && mob.m_5448_() instanceof GhoulNpc target && mates(npc, target)")
                && brain.indexOf("if (who.gang() != 0) {\n            // 4.1.45") < brain.indexOf("if (who.aogiri()) {\n            // 4.0.0 (the owner: \"Its members must ONLY"),
                "a gang goes for nothing but what goes for one of them (before any other rule: no prey, no player)");
            check(brain.contains("if (who.aogiri() || who.gang() != 0) {\n            return false;"), "and its blows land only on what it is fighting");
            check(brain.contains("mate.mind.grudges.put(attacker.m_20148_(), now + GANG_GRUDGE);") && brain.contains("holds = !own;\n                grudge = GANG_GRUDGE;")
                && brain.contains("boolean own = attacker instanceof GhoulNpc kin && mates(npc, kin) || who.ccg() && AgentBrain.ccg(attacker);")
                && brain.contains("holds = !own;"), "whoever hurts one of them has every one near on him (4.1.46: a stray blow of their own's is nothing)");
            check(brain.contains("if (who.gang() != 0 && roam(npc, mind, level, now)) {") && brain.contains("mind.wanderAt = Long.MAX_VALUE; // never its own way while it follows")
                && brain.contains("&& a.getPersistentData().m_128451_(GANG_ID) == b.getPersistentData().m_128451_(GANG_ID);"),
                "they walk together: one leads, the rest keep up (only with their own gang)");
            check(brain.contains("boolean wild = mind.owner == null && !who.aogiri() && who.gang() == 0 && !JoinServer.marked(npc);"),
                "a gang neither prowls nor hides: it walks the night");
            // 4.1.48: a rival on sight (prey looks only at what it sees), before the rule that a gang starts nothing.
            check(brain.indexOf("if (who.gang() != 0 && body instanceof GhoulNpc other && other.identity() != null && who.gangOf().rival(other.identity().gangOf())) {")
                    < brain.indexOf("if (who.gang() != 0) {\n            // 4.1.45")
                && brain.indexOf("if (who.gang() != 0 && body instanceof GhoulNpc other") > brain.indexOf("if (who.ccg()) {\n            return division("),
                "a Clown goes for a Gas Mask it sees (and back), the CCG's division as it was");
            // 4.1.48 (the owner chose "One per world + clowns"): the circus's Clowns: their template's flag made a Clown in its own mask,
            // its home where it woke; keeping to the tent (strolling a straight open way on its floor), never roaming with the others.
            check(brain.contains("} else if (saved.m_128471_(CIRCUS)) {") && brain.contains("who = circus(who, saved.m_128451_(CIRCUS_MASK), npc.m_217043_().m_188500_());")
                && brain.contains("saved.m_128405_(CIRCUS_HOME_X, (int)Math.floor(npc.m_20185_()));")
                && brain.indexOf("if (atCircus(npc, mind, level, now)) {") < brain.indexOf("if (who.gang() != 0 && roam(npc, mind, level, now)) {")
                && brain.indexOf("if (atCircus(npc, mind, level, now)) {") > brain.indexOf("if (who.aogiri() && AogiriGuard.idle(npc, mind, level, now)) {"),
                "a circus Clown is made one by its template, and keeps to the tent instead of roaming");
            check(brain.contains("} else if (who.gang() == 0 && who.maskItem() != null) {"), "nothing put on a gang's own: its mask is drawn on its face");
            check(spawner.contains("if (player.m_217043_().m_188500_() < GANG_CHANCE) {\n                    gang(level, player);")
                && spawner.contains(".ganged(gang, faces[i]);") && spawner.contains("npc.getPersistentData().m_128405_(GhoulNpcBrain.GANG_ID, gang);")
                && spawner.contains("if (!clear(level, x, z)) {\n                continue;"), "now and then a gang at night, where one ghoul could be, its number on each");
            check(layer.contains("(view.identity().aogiri() || view.identity().gang() != 0) ? view.identity().maskItem() : null;")
                && models2.contains("loaded.get(Outfits.geometryOf(id))"), "every client draws a gang's mask on its face");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
    }

    /**
     * 4.1.46 (the owner: "New Re CCG Ghoul division (Normal ccg agents (HUMANS not ghouls), they have RC cells which means they get a random
     * kagune, they are not ghouls and are members of the CCG, they are never attacked by the ccg, have no kakugan, they are basically wild
     * ghouls with CCG armor and a Kagune (NO kakugan), they spawn in trios"; asked how they behave: "Like CCG agents"): the division kept in
     * the identity, no kakugan, the Formal Outfit, three at a time with the CCG's spawns, the CCG's on both sides of every rule.
     */
    static void division() {
        com.hex.kagune.control.npc.Gang division = com.hex.kagune.control.npc.Gang.CCG_DIVISION;
        check(division.ccg() && !com.hex.kagune.control.npc.Gang.CLOWNS.ccg() && !com.hex.kagune.control.npc.Gang.GAS_MASKS.ccg()
            && !com.hex.kagune.control.npc.Gang.NONE.ccg() && division.masks().isEmpty() && division.mask(1) == null
            && com.hex.kagune.control.npc.Gang.TRIO == 3, "the CCG's division: its own, no masks, three");
        check(java.util.Arrays.equals(division.faces(3, () -> 0.5), new int[3]), "no masks to hand out (and no division by zero)");
        java.util.Random random = new java.util.Random(4146);
        boolean never = true;
        for (int i = 0; i < 2000; i++) {
            never &= !com.hex.kagune.control.npc.Gang.roaming(random.nextDouble()).ccg();
        }
        check(never, "never one of the roaming gangs");
        java.util.Set<NpcIdentity.Kind> kinds = java.util.EnumSet.noneOf(NpcIdentity.Kind.class);
        for (int i = 0; i < 600; i++) {
            NpcIdentity rolled = NpcIdentity.roll(i % 800, random::nextDouble);
            NpcIdentity one = rolled.ganged(division, 0);
            kinds.add(one.kind());
            check(one.ccg() && !one.kakugan() && one.gangOf() == division && one.maskItem() == null && !one.aogiri() && one.kind() == rolled.kind()
                && one.tier() == rolled.tier(), "one of the division: the CCG's, no kakugan, no mask, the kagune rolled: " + one);
            check(NpcIdentity.unpack(one.pack()).equals(one) && NpcIdentity.unpack(one.pack()).ccg(), "kept in its saved data, told to every client: " + one);
            check(one.tagName().endsWith("-rank (CCG Ghoul Division)"), "its name tag says so: " + one.tagName());
            check(rolled.kakugan() && rolled.ganged(com.hex.kagune.control.npc.Gang.CLOWNS, 1).kakugan(), "every other ghoul has its kakugan");
        }
        check(kinds.size() == NpcIdentity.Kind.values().length, "a random kagune: every kind turns up " + kinds);
        try {
            String agent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java")));
            String arms = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/AgentArms.java")));
            String eto = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/EtoBrain.java")));
            String brain = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java")));
            String ghoulSpawner = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcSpawner.java")));
            String agentSpawner = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/AgentSpawner.java")));
            String renderer = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/render/GhoulNpcRenderer.java")));
            String talk = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcTalk.java")));
            check(agent.contains("return body instanceof CcgAgent || body instanceof GhoulNpc ghoul && ghoul.identity() != null && ghoul.identity().ccg();")
                && agent.contains("if (body == agent || !body.m_6084_() || ccg(body) || body.m_5833_()") && !agent.contains("|| body instanceof CcgAgent ||")
                && agent.contains("&& shared.m_20280_(agent) <= AgentRules.LEASH * AgentRules.LEASH && !ccg(shared)) {")
                && agent.contains("if (by instanceof LivingEntity living && living.m_6084_() && !ccg(living)")
                && agent.contains("b -> b != agent && b.m_6084_() && !ccg(b))) {")
                && arms.contains("&& !AgentBrain.ccg(body) && (body == prey"), "an investigator never goes for one of the division, nor lands a blow on one");
            check(agent.contains("if (ccg(event.getEntity()) && ccg(event.getSource().m_7639_())) {\n            event.setCanceled(true);")
                && agent.contains("|| ccg(attacker) || attacker == agent) {"), "nothing of the CCG's hurts the CCG's (either way), nor is held against it");
            check(agent.contains("GhoulNpcBrain.called(attacker, agent, until);") && brain.contains("AgentBrain.called(attacker, npc, now + GANG_GRUDGE);"),
                "strike an investigator and the division near comes for you; strike one of the division and the investigators near do");
            check(brain.contains("if (who.ccg()) {\n            return division(npc, mind, body, now);") && brain.indexOf("return division(npc, mind, body, now);") > 0
                && brain.indexOf("return division(npc, mind, body, now);") < brain.indexOf("// 4.1.45 (the owner: \"protect eachother)"),
                "the division goes for what an investigator goes for (before a gang's rule)");
            check(brain.contains("if (AgentBrain.ccg(body) || body instanceof com.hex.kagune.control.npc.HumanNpc")
                && brain.contains("boolean shows = ghoul.mind != null && (ghoul.mind.out || ghoul.mind.eyes);\n            return current || shows && distance <= com.hex.kagune.control.npc.AgentRules.GHOUL_RANGE;")
                && brain.contains("if (AgentBrain.marked(npc, player, now)) {") && brain.contains("AgentRules.revealed(KaguneProfile.isUnlocked(player), AgentBrain.shows(player), distance, seen)"),
                "never the CCG's or a human; a ghoul once it shows itself; a player who shows what they are or whom the CCG holds something against");
            check(agent.contains("boolean shown = shows(player); // 1.3.70: their eyes on too"), "the investigators tell it the same way");
            check(brain.contains("if (AgentBrain.ccg(body)) {\n            return true; // 1.3.27") && brain.contains("static boolean aogiriFoe(LivingEntity body) {\n        if (AgentBrain.ccg(body)) {")
                && eto.contains("if (npc.persona().human() && AgentBrain.ccg(current)) {") && eto.contains("|| AgentBrain.ccg(body)))) {"),
                "a wild ghoul, Aogiri and a ghoul's home take it for the CCG; the CCG's own people don't go for it");
            check(brain.contains("if (!who.ccg() && dawn(npc, level, now)) {") && brain.contains("OutfitServer.dress(npc, com.hex.kagune.control.outfit.Outfits.FORMAL_SET);")
                && brain.contains("if (who != null && who.ccg()) {\n            HumanBrain.drop(npc, 0);"), "it keeps the day, wears the Formal Outfit, leaves a human's meat");
            check(ghoulSpawner.contains("npc -> npc.m_6084_() && !AgentBrain.ccg(npc));") && agentSpawner.contains("if (player.m_217043_().m_188500_() < DIVISION_CHANCE) {\n                        division(level, player);")
                && agentSpawner.contains(".ganged(com.hex.kagune.control.npc.Gang.CCG_DIVISION, 0);") && agentSpawner.contains("if (made.size() < com.hex.kagune.control.npc.Gang.TRIO) {")
                && agentSpawner.contains("g -> g.m_6084_() && AgentBrain.ccg(g))) {"), "three at a time with the CCG's own, spaced as the CCG, never fewer");
            check(renderer.contains("if (view != null && !view.identity().kakugan()) {\n                    return;"), "every client: no kakugan on it, ever");
            check(talk.contains("if (who.ccg()) {") && talk.indexOf("if (who.ccg()) {") < talk.indexOf("if (!GhoulNpcBrain.talks(npc, who, mind, player, now)) {"),
                "no ghoul's talk with it (no friend made of it): a word as the CCG's");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
    }

    /**
     * 1.0.41: their own faces, twelve boys and twelve girls, every one's eyes where the kakugan goes (read off the very textures: the
     * white, the iris toward the nose, nothing of the second layer over them; and the game's own reader finds them there); names for
     * every temper, a boy's for a boy and a girl's for a girl.
     */
    static void look() {
        check(NpcLook.SKINS.length == 24 && NpcLook.BOYS == 12, "twelve boys, twelve girls");
        check(!NpcLook.girl(0) && !NpcLook.girl(11) && NpcLook.girl(12) && NpcLook.girl(23) && NpcLook.girl(-1), "the girls after the boys");
        check(NpcLook.texture(4).equals("textures/entity/ghoul/boy_05.png") && NpcLook.texture(-1).endsWith("girl_12.png"), "skin textures");
        java.io.File dir = new java.io.File("src/main/resources/assets/hexkagune/textures/entity/ghoul");
        for (int skin = 0; skin < NpcLook.SKINS.length; skin++) {
            com.hex.kagune.control.kakugan.KakuganEyes eyes = NpcLook.eyes(skin);
            check(eyes.validated().equals(eyes), NpcLook.SKINS[skin] + ": its eyes are valid as they are");
            check(eyes.height() == (NpcLook.girl(skin) ? 2 : 1), NpcLook.SKINS[skin] + ": a girl's eyes two rows tall, a boy's one");
            java.awt.image.BufferedImage image;
            try {
                image = javax.imageio.ImageIO.read(new java.io.File(dir, NpcLook.SKINS[skin] + ".png"));
            } catch (java.io.IOException failure) {
                image = null;
            }
            check(image != null && image.getWidth() == 64 && image.getHeight() == 64, NpcLook.SKINS[skin] + ": a 64 x 64 skin");
            if (image == null) {
                continue;
            }
            boolean patched = skin == NpcLook.PATCHED;
            for (int side = 0; side < 2; side++) {
                int x = side == 0 ? eyes.rightX() : eyes.leftX();
                int irisX = side == 0 ? x + 1 : x;
                int whiteX = side == 0 ? x : x + 1;
                boolean turns = eyes.ghoul() == com.hex.kagune.control.kakugan.KakuganEyes.BOTH
                    || eyes.ghoul() == (side == 0 ? com.hex.kagune.control.kakugan.KakuganEyes.RIGHT : com.hex.kagune.control.kakugan.KakuganEyes.LEFT);
                for (int row = 0; row < eyes.height(); row++) {
                    int y = (side == 0 ? eyes.rightY() : eyes.leftY()) + row;
                    int white = image.getRGB(8 + whiteX, 8 + y);
                    int iris = image.getRGB(8 + irisX, 8 + y);
                    check(brightness(white) > 200 && brightness(iris) < brightness(white) - 60, NpcLook.SKINS[skin] + ": an eye (white, iris to the nose) at "
                        + whiteX + "," + y);
                    boolean covered = (image.getRGB(40 + whiteX, 8 + y) >>> 24) > 0 || (image.getRGB(40 + irisX, 8 + y) >>> 24) > 0;
                    check(covered == (patched && !turns), NpcLook.SKINS[skin] + (turns ? ": nothing over the eye that turns" : ": the patch over the eye that does not"));
                }
            }
            if (!patched) {
                int[] pixels = new int[64 * 64];
                image.getRGB(0, 0, 64, 64, pixels, 0, 64);
                com.hex.kagune.control.kakugan.KakuganEyes found = com.hex.kagune.control.kakugan.KakuganSkin.detect(pixels, 64);
                check(found != null && found.rightX() == eyes.rightX() && found.leftX() == eyes.leftX() && found.rightY() == eyes.rightY(),
                    NpcLook.SKINS[skin] + ": the game's own reader finds the eyes where the kakugan goes (" + found + ")");
            }
        }
        check(NpcLook.eyes(NpcLook.PATCHED).ghoul() == com.hex.kagune.control.kakugan.KakuganEyes.RIGHT, "the boy in the eyepatch: only his right eye turns");
        for (NpcTemper temper : NpcTemper.values()) {
            for (boolean girl : new boolean[] {false, true}) {
                String name = NpcNames.name(3, temper, girl);
                check(name.startsWith(NpcNames.given(3, girl)) && name.length() > NpcNames.given(3, girl).length(), "named: " + name);
            }
        }
        check(NpcNames.name(20, NpcTemper.DRIFTER, false).equals("Ren the Drifter") && NpcNames.name(20, NpcTemper.ANTEIKU, false).equals("Ren of Anteiku")
            && NpcNames.name(11, NpcTemper.PACK, true).equals("Mio of the Pack"), "how the names read");
        java.util.Set<String> given = new java.util.HashSet<>();
        for (boolean girl : new boolean[] {false, true}) {
            for (int i = 0; i < NpcNames.count(); i++) {
                check(given.add(NpcNames.given(i, girl)), "no name twice: " + NpcNames.given(i, girl));
            }
        }
        check(NpcNames.count() <= 256, "names fit in the saved number");
        NpcIdentity boy = new NpcIdentity(NpcIdentity.Kind.RINKAKU, NpcTemper.DRIFTER, 0, 3, 20, 1);
        NpcIdentity girl = new NpcIdentity(NpcIdentity.Kind.RINKAKU, NpcTemper.DRIFTER, 0, 15, 11, 1);
        check(!boy.girl() && boy.displayName().equals("Ren the Drifter") && girl.girl() && girl.displayName().equals("Mio the Drifter")
            && girl.given().equals("Mio"), "a boy goes by a boy's name, a girl by a girl's");
    }

    private static int brightness(int argb) {
        return ((argb >> 16 & 255) + (argb >> 8 & 255) + (argb & 255)) / 3;
    }

    /** Regard: how it reads, what it does to a temper's attitude, a stray blow forgiven, a betrayal not. */
    static void bonds() {
        check(NpcBonds.band(-100) == NpcBonds.Band.ENEMY && NpcBonds.band(NpcBonds.ENEMY) == NpcBonds.Band.ENEMY, "enemy at and below -40");
        check(NpcBonds.band(-1) == NpcBonds.Band.COLD && NpcBonds.band(0) == NpcBonds.Band.STRANGER && NpcBonds.band(NpcBonds.FRIENDLY) == NpcBonds.Band.FRIENDLY
            && NpcBonds.band(NpcBonds.FRIEND) == NpcBonds.Band.FRIEND, "the bands");
        for (NpcTemper.Attitude base : NpcTemper.Attitude.values()) {
            check(NpcBonds.attitude(base, NpcBonds.ENEMY) == NpcTemper.Attitude.HOSTILE, "an enemy is gone for, whatever the temper: " + base);
            check(NpcBonds.attitude(base, 0) == base, "a stranger is as its temper says: " + base);
        }
        check(NpcBonds.attitude(NpcTemper.Attitude.WARY, NpcBonds.FRIENDLY) == NpcTemper.Attitude.FRIENDLY, "a friendly one won't fight first");
        check(NpcBonds.attitude(NpcTemper.Attitude.HOSTILE, NpcBonds.MOST) == NpcTemper.Attitude.HOSTILE, "regard never tames a hunter");
        check(NpcBonds.struck(NpcBonds.FRIEND) == NpcBonds.FRIEND + NpcBonds.BLOW && !NpcBonds.betrayal(NpcBonds.FRIEND), "a friend forgives one stray blow");
        check(NpcBonds.betrayal(NpcBonds.FRIENDLY) && NpcBonds.struck(NpcBonds.FRIENDLY) == NpcBonds.BETRAYED, "the blow that turns a friend is a betrayal");
        check(NpcBonds.struck(0) == NpcBonds.BLOW && !NpcBonds.betrayal(0), "a stranger struck only cools");
        check(NpcBonds.struck(-90) == NpcBonds.LEAST, "never below the least");
        check(NpcBonds.joins(NpcTemper.DRIFTER, null) && NpcBonds.joins(NpcTemper.ANTEIKU, "anteiku") && !NpcBonds.joins(NpcTemper.ANTEIKU, "pack")
            && NpcBonds.joins(NpcTemper.PACK, "pack") && !NpcBonds.joins(NpcTemper.PACK, null) && !NpcBonds.joins(NpcTemper.GOURMET, null)
            && !NpcBonds.joins(NpcTemper.FERAL, null), "who walks with whom");
    }

    /** Every temper but the feral asks something fitting, the same until it is done; favours and bonds keep through a save. */
    static void favours() {
        for (NpcTemper temper : NpcTemper.values()) {
            for (int seed = 0; seed < 200; seed++) {
                NpcIdentity who = new NpcIdentity(NpcIdentity.Kind.RINKAKU, temper, 1, 2, seed % NpcNames.count(), seed * 337);
                Favours.Spec spec = Favours.offer(who, seed % 4);
                if (temper == NpcTemper.FERAL) {
                    check(spec == null, "a feral asks nothing");
                    continue;
                }
                check(spec != null && spec.equals(Favours.offer(who, seed % 4)), temper + ": the same ask until it is done");
                check(Favours.Spec.load(spec.save()).equals(spec), "a favour's spec round trips");
                Quest quest = Favours.quest(spec, "1b4e28ba-2fa1-11d2-883f-0016d3cca427", who, seed % 4);
                check(quest.line() == Quest.Line.FAVOUR && quest.objectives().size() == 1 && quest.title().contains(who.given()),
                    "a favour is a quest of the favour line: " + quest.title());
                check(!quest.id().contains(";") && !quest.id().contains("=") && !quest.id().contains(","), "a favour's id fits the save: " + quest.id());
                String asking = Favours.asking(spec, who);
                check(asking.contains("pays"), "it says what it pays: " + asking);
                boolean violent = quest.objectives().get(0).all().contains(Tags.temper("anteiku"));
                check(!violent || temper == NpcTemper.PACK, "only the Pack asks for Anteiku's blood");
            }
        }
        NpcIdentity ren = new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.DRIFTER, 1, 4, 36, 4242);
        String giver = "1b4e28ba-2fa1-11d2-883f-0016d3cca427";
        QuestLog log = new QuestLog();
        log.bond(giver, log.bond(giver).regard(NpcBonds.FRIENDLY).with(NpcBonds.MET, true));
        Favours.Spec spec = Favours.offer(ren, 0);
        check(log.startFavour(spec, giver, ren), "a favour taken on");
        check(!log.startFavour(spec, giver, ren), "one favour at a time for the same ghoul");
        for (int i = 0; i < 4; i++) {
            log.startFavour(Favours.offer(new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.ANTEIKU, 0, 0, i, i * 11), 0), "other-" + i,
                new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.ANTEIKU, 0, 0, i, i * 11));
        }
        check(log.favours().size() == NpcBonds.MOST_FAVOURS, "no more than three on");
        QuestLog read = QuestLog.load(log.save());
        check(read.favours().size() == log.favours().size() && read.favourFrom(giver) != null
            && read.favourFrom(giver).quest().id().equals(log.favourFrom(giver).quest().id()), "favours keep through a save");
        check(read.bond(giver).regard() == NpcBonds.FRIENDLY && read.bond(giver).has(NpcBonds.MET), "bonds keep through a save");
        Quest favour = read.favourFrom(giver).quest();
        check(read.quests().contains(favour), "a favour counts like any quest");
        com.hex.kagune.control.quest.Objective asked = favour.objectives().get(0);
        for (int i = 0; i < 40; i++) {
            read.apply(new QuestEvent(asked.signal(), 1, new java.util.HashSet<>(asked.all())), com.hex.kagune.control.KagunePath.RINKAKU, 500);
        }
        if (read.met(favour)) {
            read.complete(favour);
            check(read.favourFrom(giver) == null, "a favour done is taken off");
            check(read.bond(giver).regard() == NpcBonds.FRIENDLY + NpcBonds.FAVOUR_DONE && read.bond(giver).favours() == 1, "and the ghoul thinks better of you");
            check(!Favours.offer(ren, 1).equals(spec) || Favours.offer(ren, 1).equals(Favours.offer(ren, 1)), "the next ask is rolled afresh");
        } else {
            check(false, "a favour can be done: " + favour.objectives().get(0).text());
        }
        QuestLog crowded = new QuestLog();
        for (int i = 0; i < NpcBonds.REMEMBERED + 10; i++) {
            crowded.bond("npc-" + i, QuestLog.Bond.NONE.regard(i % 7 == 0 ? NpcBonds.FRIEND : 1));
        }
        check(crowded.bonds().size() == NpcBonds.REMEMBERED, "only so many bonds remembered: " + crowded.bonds().size());
        check(crowded.bond("npc-0").regard() == NpcBonds.FRIEND && crowded.bond("npc-" + (NpcBonds.REMEMBERED + 9)).regard() == 1,
            "the faintest forgotten first, the newest kept");
        check(QuestBook.get("a_friend") != null && QuestBook.get("cold_blood") != null && "trust".equals(QuestBook.get("a_friend").exclusive())
            && "trust".equals(QuestBook.get("cold_blood").exclusive()), "a friend or cold blood: one closes the other");
    }

    /** Every temper talks in its own voice at every standing, offers what fits, and never talks when it would fight. */
    static void talk() {
        String[] sides = {null, "anteiku", "pack"};
        java.util.Set<String> greetings = new java.util.HashSet<>();
        for (NpcTemper temper : NpcTemper.values()) {
            for (String side : sides) {
                for (boolean ghoul : new boolean[] {false, true}) {
                    NpcTemper.Attitude base = temper.toward(ghoul, side);
                    for (int regard : new int[] {-30, 0, 30, 70}) {
                        NpcTemper.Attitude attitude = NpcBonds.attitude(base, regard);
                        boolean talks = NpcTalk.talks(temper, attitude);
                        check(!talks || temper != NpcTemper.FERAL && attitude != NpcTemper.Attitude.HOSTILE, temper + " talks only when it would not fight");
                        if (!talks) {
                            continue;
                        }
                        NpcIdentity who = new NpcIdentity(NpcIdentity.Kind.RINKAKU, temper, 1, 0, 5, regard + 100);
                        Favours.Spec spec = Favours.offer(who, 0);
                        for (boolean ally : new boolean[] {false, true}) {
                            NpcTalk.Context c = new NpcTalk.Context(who, regard, ghoul, side, ally, ally, false, false, true, false, true,
                                spec == null ? null : Favours.asking(spec, who), true);
                            NpcTalk.Page page = NpcTalk.open(c);
                            greetings.add(page.line());
                            check(!page.line().isBlank() && page.choices().contains(NpcTalk.Choice.LEAVE) && page.choices().size() <= 8,
                                temper + " greets and lets you leave");
                            check(page.choices().contains(NpcTalk.Choice.PART) == ally && page.choices().contains(NpcTalk.Choice.INSULT) == !ally,
                                "an ally can be parted with, a stranger insulted");
                            check(!page.choices().contains(NpcTalk.Choice.GIFT) || temper != NpcTemper.ANTEIKU, "Anteiku takes no ghoul's flesh");
                            for (NpcTalk.Choice choice : page.choices()) {
                                NpcTalk.Reply reply = NpcTalk.answer(c, choice);
                                check(choice == NpcTalk.Choice.LEAVE || !reply.line().isBlank(), temper + " answers " + choice);
                                if (reply.effect() == NpcTalk.Effect.ALLY) {
                                    check(regard >= NpcBonds.FRIEND && !ally && NpcBonds.joins(temper, side), "joins only a friend it would walk with");
                                }
                            }
                        }
                    }
                }
            }
        }
        check(greetings.size() >= 25, "many different greetings: " + greetings.size());
        NpcIdentity scav = new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.SCAVENGER, 0, 0, 0, 1);
        NpcTalk.Context c = new NpcTalk.Context(scav, 0, true, null, false, false, true, true, false, false, true, "Eat raw flesh (4). It pays 20 mastery.",
            true);
        check(NpcTalk.answer(c, NpcTalk.Choice.CHAT).regard() == 0, "one chat a night counts");
        check(NpcTalk.answer(c, NpcTalk.Choice.WHO).regard() == 0, "being told twice who it is counts once");
        check(NpcTalk.answer(c, NpcTalk.Choice.FAVOUR).effect() == NpcTalk.Effect.OFFER, "it offers its favour");
        check(NpcTalk.answer(c, NpcTalk.Choice.ACCEPT).effect() == NpcTalk.Effect.START_FAVOUR, "and it is taken on");
        check(NpcTalk.answer(c, NpcTalk.Choice.INSULT).effect() == NpcTalk.Effect.FLEE, "an insulted scavenger runs");
        check(NpcTalk.answer(c, NpcTalk.Choice.GIFT).effect() == NpcTalk.Effect.NONE, "no gift without meat in hand");
    }

    /**
     * 1.3.36: the arm blade (an NPC-only koukaku): three cuts to a string, chained only close together; each lands before it ends; the
     * thrust reaches furthest and hits hardest; the spiral winds shoulder to wrist before the blade grows; its shape stays on the arm; its
     * poses hold the blade arm and keep the free hand on the forearm while it winds.
     */
    /**
     * 4.1.47: the great blade's lowest point in {@code p} (blocks above the feet; below 0 it is in the ground), from the game's own transform
     * of the right arm (its pivot and turn, the chest's twist and lean carried) and the body's drop.
     */
    static double bladeLow(com.hex.kagune.control.client.animation.RigPose p) {
        float[] turn = p.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.ROTATION, 0, 0, 0);
        float[] pivot = p.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.POSITION, -5, 2, 0);
        double cx = Math.cos(turn[0]), sx = Math.sin(turn[0]), cy = Math.cos(turn[1]), sy = Math.sin(turn[1]), cz = Math.cos(turn[2]), sz = Math.sin(turn[2]);
        double[] lowest = {99.0};
        com.hex.kagune.control.npc.BladeArmModel.build(1.0, false, 0x7A1E5C, (a, b, c, d, n, ca, cb, cc, cd) -> {
            for (double[] local : new double[][] {a, b, c, d}) {
                double y1 = local[1] * cx - local[2] * sx;
                double z1 = local[1] * sx + local[2] * cx;
                double x2 = local[0] * cy + z1 * sy;
                double y = pivot[1] + x2 * sz + y1 * cz;
                lowest[0] = Math.min(lowest[0], (24.0 - y - p.drop) / 16.0);
            }
        });
        return lowest[0];
    }

    static void blades() {
        check(com.hex.kagune.control.npc.BladeArm.STEPS == 4 && com.hex.kagune.control.npc.BladeArm.next(-1, 0L) == 0
            && com.hex.kagune.control.npc.BladeArm.next(0, 3L) == 1 && com.hex.kagune.control.npc.BladeArm.next(1, 3L) == 2
            && com.hex.kagune.control.npc.BladeArm.next(2, 3L) == 3 && com.hex.kagune.control.npc.BladeArm.next(3, 3L) == 0
            && com.hex.kagune.control.npc.BladeArm.next(1, com.hex.kagune.control.npc.BladeArm.CHAIN + 1) == 0
            && com.hex.kagune.control.npc.BladeArm.next(0, -5L) == 0, "4.1.47: four slashes to a string, chained only close together");
        for (int step = 0; step < com.hex.kagune.control.npc.BladeArm.STEPS; step++) {
            check(com.hex.kagune.control.npc.BladeArm.sweeps(step) == (step == com.hex.kagune.control.npc.BladeArm.SWEEP), "only the sweep cuts all before it: " + step);
        }
        for (int step = 0; step < com.hex.kagune.control.npc.BladeArm.STEPS; step++) {
            check(com.hex.kagune.control.npc.BladeArm.lands(step) > 1 && com.hex.kagune.control.npc.BladeArm.lands(step)
                < com.hex.kagune.control.npc.BladeArm.ticks(step), "cut " + step + " lands partway through");
        }
        check(com.hex.kagune.control.npc.BladeArm.reach(1, 3) > com.hex.kagune.control.npc.BladeArm.reach(1, 2)
            && com.hex.kagune.control.npc.BladeArm.reach(1, 2) > com.hex.kagune.control.npc.BladeArm.reach(1, 0)
            && com.hex.kagune.control.npc.BladeArm.weight(3) > com.hex.kagune.control.npc.BladeArm.weight(2)
            && com.hex.kagune.control.npc.BladeArm.weight(2) > com.hex.kagune.control.npc.BladeArm.weight(0)
            && com.hex.kagune.control.npc.BladeArm.reach(2) > com.hex.kagune.control.npc.BladeArm.reach(0)
            && com.hex.kagune.control.npc.BladeArm.reach(0) >= 3.2, "the cleave furthest and hardest, the sweep next; stronger, longer; the great blade's reach");
        double last = 1.0;
        for (int i = 0; i < com.hex.kagune.control.npc.BladeArm.PLATES; i++) {
            double at = com.hex.kagune.control.npc.BladeArm.plate(i, 0.3);
            check(at <= last + 1.0E-9, "the spiral winds from the shoulder down (" + i + ")");
            last = at;
            check(com.hex.kagune.control.npc.BladeArm.plate(i, 0.0) == 0.0 && com.hex.kagune.control.npc.BladeArm.plate(i, 0.8) == 1.0,
                "plate " + i + " none at first, grown before the blade is done");
        }
        check(com.hex.kagune.control.npc.BladeArm.blade(0.5) == 0.0 && com.hex.kagune.control.npc.BladeArm.blade(1.0) == 1.0
            && com.hex.kagune.control.npc.BladeArm.blade(0.8) > 0.0, "the blade grows once the spiral reaches the wrist");
        int[] quads = {0};
        int[] gleams = {0};
        double[] most = {0.0, -99.0, 0.0, 99.0, 0.0, 0.0};
        for (boolean slim : new boolean[] {false, true}) {
            com.hex.kagune.control.npc.BladeArmModel.build(1.0, slim, 0x7A1E5C, (a, b, c, d, n, ca, cb, cc, cd) -> {
                quads[0]++;
                gleams[0] += n == com.hex.kagune.control.npc.BladeArmModel.GLOW ? 1 : 0;
                for (double[] p : new double[][] {a, b, c, d}) {
                    most[0] = Math.max(most[0], Math.max(Math.abs(p[0] + 1.0), Math.abs(p[2])));
                    most[1] = Math.max(most[1], p[1]);
                    most[3] = Math.min(most[3], p[1]);
                    if (p[1] > 20.0 && p[1] < 28.0) {
                        most[4] = Math.max(most[4], p[2]);
                        most[5] = Math.min(most[5], p[2]);
                    }
                    check(Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]) && (ca >>> 24) == 0xFF, "a finite, opaque corner");
                }
            });
        }
        // 4.1.47 (the owner: "it looks way too small, not sharp"; then a picture): the ribbons round the arm, one great blade past the fist,
        // its point more than twice the arm's length from the shoulder.
        check(quads[0] > 400 && gleams[0] > 50 && most[0] < 9.5 && most[1] > 33.0 && most[1] < 38.0 && most[3] > -4.5,
            "the great blade on the arm (" + quads[0] + " quads, " + gleams[0] + " glowing, " + most[0] + ", " + most[3] + ".." + most[1] + ")");
        check(most[4] > 5.0 && most[4] > -most[5] + 1.0, "the owner chose \"B: blade turned over\": its edge (the widest side) on the back of the arm (z +"
            + most[4] + " against " + most[5] + "), facing down with the arm held out");
        int[] none = {0};
        com.hex.kagune.control.npc.BladeArmModel.build(0.0, false, 0x7A1E5C, (a, b, c, d, n, ca, cb, cc, cd) -> none[0]++);
        check(none[0] == 0, "nothing before it winds out");
        com.hex.kagune.control.client.animation.RigPose guard = com.hex.kagune.control.client.animation.BladeArmPose.guard();
        check(guard.rightArmHold == 1.0F && guard.twist < 0.0F, "its guard: the blade arm held, the right shoulder forward");
        for (int step = 0; step < com.hex.kagune.control.npc.BladeArm.STEPS; step++) {
            for (double t = 0.0; t <= 1.0; t += 0.05) {
                com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.BladeArmPose.swing(step, t);
                check(Float.isFinite(p.rightArmX) && Float.isFinite(p.rightArmY) && Float.isFinite(p.rightArmZ) && Math.abs(p.lunge) <= 8.0F,
                    "cut " + step + " at " + t + " is a sane pose");
                double low = bladeLow(p);
                check(low > -0.05, "4.1.47: cut " + step + " at " + t + " keeps the great blade out of the ground (" + low + " blocks)");
            }
        }
        for (double time = 0.0; time < 320.0; time += 3.0) {
            com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.BladeArmPose.stand(time, 31);
            check(p.rightArmHold == 1.0F && p.legHold == 1.0F && bladeLow(p) > -0.05, "4.1.47: its stance at " + time + ": held, feet set, the blade off the ground");
        }
        check(com.hex.kagune.control.client.animation.BladeArmPose.stand(10.0, 31, 0.0F).legHold == 0.0F, "walking, its legs its walk's");
        boolean alive = false;
        com.hex.kagune.control.client.animation.RigPose first = com.hex.kagune.control.client.animation.BladeArmPose.stand(0.0, 31);
        for (double time = 4.0; time < 200.0; time += 4.0) {
            com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.BladeArmPose.stand(time, 31);
            alive |= Math.abs(p.rightArmX - first.rightArmX) + Math.abs(p.rightArmZ - first.rightArmZ) + Math.abs(p.lean - first.lean) > 0.05;
        }
        check(alive, "4.1.47: the stance is alive (it breathes, the blade drifts), not a still pose");
        try {
            String client = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/GhoulNpcClient.java")));
            String combat = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcCombat.java")));
            check(client.contains("return com.hex.kagune.control.client.animation.BladeArmPose.stand(clock, view.identity().seed(), still);")
                && !client.contains("return com.hex.kagune.control.client.animation.BladeArmPose.guard();"), "every client draws its living stance, not a still guard");
            check(combat.contains("if (BladeArm.sweeps(step)) {") && combat.contains("|| !GhoulNpcBrain.fair(npc, who, mind, other, now)) {")
                && combat.contains("WoundHits.expect(body, WoundBook.Blow.SLASH,"), "the sweep cuts all before it it may (never one it would spare); every cut a slash");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
        com.hex.kagune.control.client.animation.RigPose winding = com.hex.kagune.control.client.animation.BladeArmPose.wrap(0.5);
        double[] left = com.hex.kagune.control.client.animation.QuinqueGrip.fist(winding, false, false);
        float[] turn = winding.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.ROTATION, 0, 0, 0);
        float[] pivot = winding.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.POSITION, -5, 2, 0);
        double cx = Math.cos(turn[0]), sx = Math.sin(turn[0]), cy = Math.cos(turn[1]), sy = Math.sin(turn[1]), cz = Math.cos(turn[2]), sz = Math.sin(turn[2]);
        double[] local = {-1.0, 6.5, -0.5};
        // Rz Ry Rx of the forearm point.
        double y1 = local[1] * cx - local[2] * sx;
        double z1 = local[1] * sx + local[2] * cx;
        double x2 = local[0] * cy + z1 * sy;
        double z2 = -local[0] * sy + z1 * cy;
        double[] forearm = {pivot[0] + x2 * cz - y1 * sz, pivot[1] + x2 * sz + y1 * cz, pivot[2] + z2};
        double off = Math.sqrt(Math.pow(left[0] - forearm[0], 2) + Math.pow(left[1] - forearm[1], 2) + Math.pow(left[2] - forearm[2], 2));
        check(off < 3.0, "winding out, its free hand grips the forearm (" + off + " px)");
        com.hex.kagune.control.client.animation.RigPose run = com.hex.kagune.control.client.animation.BladeArmPose.run(0.5, 1.0F);
        check(run.lean > 0.3F && run.rightArmHold == 1.0F, "its sprint: pitched forward, the blade arm swept back");
    }

    /**
     * 1.3.38: the speech bubble and the thought cloud: every quad above the speaker's head but the tail's tip or the trail's last puff;
     * the words inside the box; the outline behind the body, the body behind the words; actions make a cloud, words a bubble; words
     * inked black, thoughts dark grey and slanted; it pops in and settles; a sleeper's z's grow.
     */
    static void bubbles() {
        for (boolean thought : new boolean[] {false, true}) {
            for (int lines = 1; lines <= 4; lines++) {
                for (int width : new int[] {6, 40, 180}) {
                    com.hex.kagune.control.npc.BubbleShape shape = com.hex.kagune.control.npc.BubbleShape.of(width, lines, thought);
                    int[] quads = {0};
                    float[] lowest = {-99.0F};
                    boolean[] layered = {true};
                    shape.fill((a, b, c, d, argb, depth) -> {
                        quads[0]++;
                        for (float[] p : new float[][] {a, b, c, d}) {
                            lowest[0] = Math.max(lowest[0], p[1]);
                        }
                        layered[0] &= depth == com.hex.kagune.control.npc.BubbleShape.OUTLINE_DEPTH
                            ? argb == (thought ? com.hex.kagune.control.npc.BubbleShape.CLOUD_INK : com.hex.kagune.control.npc.BubbleShape.INK)
                            : depth == com.hex.kagune.control.npc.BubbleShape.BODY_DEPTH && (argb >>> 24) == 0xFF;
                    });
                    String what = (thought ? "cloud " : "bubble ") + lines + "x" + width;
                    check(quads[0] > 20 && lowest[0] <= 2.0F && lowest[0] >= -0.1F, what + ": over the head, its point at it (" + lowest[0] + ")");
                    check(layered[0] && com.hex.kagune.control.npc.BubbleShape.OUTLINE_DEPTH > com.hex.kagune.control.npc.BubbleShape.BODY_DEPTH,
                        what + ": outline behind body, body behind the words");
                    check(shape.x1 - shape.x0 >= width + 2 * com.hex.kagune.control.npc.BubbleShape.PAD && shape.lineY(0) > shape.y0
                        && shape.lineY(lines - 1) + 9 <= shape.y1 && shape.y1 < 0.0F, what + ": the words inside the box");
                }
            }
        }
        check(com.hex.kagune.control.npc.BubbleShape.thought(java.util.List.of("§7§o*stirs*")) && !com.hex.kagune.control.npc.BubbleShape.thought(
            java.util.List.of("§fHey.")) && !com.hex.kagune.control.npc.BubbleShape.thought(java.util.List.of()), "an action is a cloud, words a bubble");
        check(com.hex.kagune.control.npc.BubbleShape.ink("§fHey.", false).equals("§0Hey.") && com.hex.kagune.control.npc.BubbleShape.ink("§7§o*sighs*",
            true).equals("§8§o*sighs*"), "words inked black, thoughts dark grey and slanted");
        check(com.hex.kagune.control.npc.BubbleShape.pop(0.0) < 0.7F && com.hex.kagune.control.npc.BubbleShape.pop(2.6) > 1.0F
            && com.hex.kagune.control.npc.BubbleShape.pop(10.0) == 1.0F && com.hex.kagune.control.npc.BubbleShape.pop(1.0E9) == 1.0F, "it pops in and settles");
        check(com.hex.kagune.control.npc.BubbleShape.zees(0).equals("z") && com.hex.kagune.control.npc.BubbleShape.zees(20).equals("z Z")
            && com.hex.kagune.control.npc.BubbleShape.zees(50).equals("z Z z"), "a sleeper's z's grow");
        turns();
        layout();
    }

    /**
     * 1.3.52: a talk's turns: a line answered by someone else comes down as the answer goes up (never both at once), shrinking away; the
     * same speaker's next line takes its place anyway; a lone line stays as long as it takes to read.
     */
    static void turns() {
        String line = "You always get the bigger half and it's not fair.";
        check(com.hex.kagune.control.npc.Speech.turn(4, 5, 70L) == 70 && com.hex.kagune.control.npc.Speech.turn(4, 4, 70L) == 0
            && com.hex.kagune.control.npc.Speech.turn(4, 5, 3L) == 20 && com.hex.kagune.control.npc.Speech.turn(4, 5, 99999L)
            == com.hex.kagune.control.npc.Speech.MOST, "a line is up until someone else answers (at least a second)");
        check(com.hex.kagune.control.npc.Speech.life(line, 0) == com.hex.kagune.control.npc.Speech.ticks(line)
            && com.hex.kagune.control.npc.Speech.life(line, 70) == 70 && com.hex.kagune.control.npc.Speech.life(line, 2) == 10
            && com.hex.kagune.control.npc.Speech.life(line, 9999) == com.hex.kagune.control.npc.Speech.ticks(line), "sent for a while, up that long");
        boolean readable = true;
        for (int n = 0; n <= 120; n++) {
            String text = "x".repeat(n);
            int pause = com.hex.kagune.control.family.Parenting.pause(text);
            readable &= pause < com.hex.kagune.control.npc.Speech.ticks(text) && pause >= 40 && pause <= 140 && pause >= 20 + n;
        }
        check(readable, "each turn up long enough to read (a tick or more a letter), the next before it would have come down anyway");
        check(com.hex.kagune.control.npc.BubbleShape.pop(10.0, 100.0) == 1.0F && com.hex.kagune.control.npc.BubbleShape.pop(10.0, 0.0) == 0.0F
            && com.hex.kagune.control.npc.BubbleShape.pop(10.0, 1.5) > 0.3F && com.hex.kagune.control.npc.BubbleShape.pop(10.0, 1.5) < 1.0F
            && com.hex.kagune.control.npc.BubbleShape.pop(1.0, 100.0) == com.hex.kagune.control.npc.BubbleShape.pop(1.0), "it shrinks away at its end");
        // Moved aside or up: the box goes, its point stays at the head; the tail (a cloud's trail) leans down to it.
        for (boolean thought : new boolean[] {false, true}) {
            com.hex.kagune.control.npc.BubbleShape home = com.hex.kagune.control.npc.BubbleShape.of(120, 2, thought);
            com.hex.kagune.control.npc.BubbleShape same = com.hex.kagune.control.npc.BubbleShape.of(120, 2, thought, 0.0F, 0.0F);
            check(same.home() && same.x0 == home.x0 && same.y1 == home.y1, "unmoved, the same shape as before");
            for (float[] move : new float[][] {{90.0F, 0.0F}, {-150.0F, 0.0F}, {0.0F, 40.0F}, {60.0F, 25.0F}}) {
                com.hex.kagune.control.npc.BubbleShape moved = com.hex.kagune.control.npc.BubbleShape.of(120, 2, thought, move[0], move[1]);
                float[] lowest = {-99.0F, 0.0F};
                int[] below = {0};
                moved.fill((a, b, c, d, argb, depth) -> {
                    for (float[] pt : new float[][] {a, b, c, d}) {
                        if (pt[1] > lowest[0]) {
                            lowest[0] = pt[1];
                            lowest[1] = pt[0];
                        }
                        below[0] += pt[1] > moved.y1 + 2.0F ? 1 : 0;
                    }
                });
                String what = (thought ? "cloud" : "bubble") + " moved " + move[0] + "," + move[1];
                check(Math.abs(moved.x0 - (home.x0 + move[0])) < 0.01F && Math.abs(moved.y1 - (home.y1 - move[1])) < 0.01F, what + ": its box moved");
                check(lowest[0] <= 2.6F && lowest[0] >= -2.0F && Math.abs(lowest[1] + 5.0F) < 4.5F, what + ": its point still at the head (" + lowest[0]
                    + " at " + lowest[1] + ")");
                check(below[0] > 0, what + ": something reaches down from the box to the head");
                check(moved.lineY(0) > moved.y0 && moved.lineY(1) + 9 <= moved.y1, what + ": the words still inside");
            }
        }
    }

    /** 1.3.52: bubbles kept apart: two that would overlap spread apart, each to its own speaker's side; boxed in, the newer goes up. */
    static void layout() {
        double k = 0.025 / 6.0; // radians a pixel six blocks off
        com.hex.kagune.control.npc.BubbleShape wide = com.hex.kagune.control.npc.BubbleShape.of(200, 2, false);
        com.hex.kagune.control.npc.BubbleShape small = com.hex.kagune.control.npc.BubbleShape.of(60, 1, false);
        java.util.function.BiFunction<Integer, Double, com.hex.kagune.control.npc.BubbleLayout.Seen> at = (id, x) -> new com.hex.kagune.control.npc.BubbleLayout.Seen(
            id, 100.0 - id, x, 0.0, k, wide.x0, wide.y0, wide.x1, wide.y1, wide.rim());
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> one = com.hex.kagune.control.npc.BubbleLayout.arrange(java.util.List.of(at.apply(1, 0.0)));
        check(one.get(1).equals(com.hex.kagune.control.npc.BubbleLayout.Place.HOME), "alone, over its head");
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> apart = com.hex.kagune.control.npc.BubbleLayout.arrange(java.util.List.of(
            at.apply(1, 0.0), at.apply(2, 1.2)));
        check(apart.get(1).equals(com.hex.kagune.control.npc.BubbleLayout.Place.HOME) && apart.get(2).equals(com.hex.kagune.control.npc.BubbleLayout.Place.HOME),
            "far enough apart, both over their own heads");
        // Two speakers a block and a half apart, six blocks off: spread apart sideways, each to its own speaker's side, never stacked.
        double near = 1.5 / 6.0;
        for (double side : new double[] {1.0, -1.0}) {
            java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> two = java.util.List.of(at.apply(1, 0.0), at.apply(2, side * near));
            java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> placed = com.hex.kagune.control.npc.BubbleLayout.arrange(two);
            check(placed.get(2).shift() * side > 0.0F && placed.get(1).shift() * side < 0.0F && placed.get(1).lift() == 0.0F && placed.get(2).lift() == 0.0F,
                "spread apart, each to its own side: " + placed);
            check(clear(two, placed), "and clear of each other");
            for (com.hex.kagune.control.npc.BubbleLayout.Seen b : two) {
                double[] box = box(b, placed.get(b.id()));
                check(b.x() > box[0] && b.x() < box[2], "each still over its own speaker, its tail short");
            }
        }
        // Right on top of each other (a little one on its parent's hip): split to either side, the point between them.
        java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> stacked = java.util.List.of(at.apply(1, 0.0), new com.hex.kagune.control.npc.BubbleLayout.Seen(
            2, 1.0, 0.0, 0.0004, k * 1.06, small.x0, small.y0, small.x1, small.y1, small.rim()));
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> hip = com.hex.kagune.control.npc.BubbleLayout.arrange(stacked);
        check(clear(stacked, hip) && hip.get(1).shift() < 0.0F && hip.get(2).shift() > 0.0F && hip.get(2).lift() <= com.hex.kagune.control.npc.BubbleLayout.TILT,
            "a little one's bubble on its parent's: split either side, " + hip);
        // A cloud (its puffs reach past its box) beside a bubble: what is drawn kept clear, not just the boxes.
        com.hex.kagune.control.npc.BubbleShape cloud = com.hex.kagune.control.npc.BubbleShape.of(190, 2, true);
        java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> puffs = java.util.List.of(com.hex.kagune.control.npc.BubbleLayout.Seen.of(1, 30.0, 0.0, 0.0,
            k, cloud), com.hex.kagune.control.npc.BubbleLayout.Seen.of(2, 5.0, 0.0, 0.0004, k * 1.06, small));
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> drawn = com.hex.kagune.control.npc.BubbleLayout.arrange(puffs);
        double cloudRight = (cloud.x1 + cloud.rim() + drawn.get(1).shift()) * k;
        double bubbleLeft = 0.0004 + (small.x0 - small.rim() + drawn.get(2).shift()) * k * 1.06;
        check(cloud.rim() > 4.0F && bubbleLeft >= cloudRight, "a cloud's puffs kept clear of the bubble beside it");
        // Three in a row: nobody covers anybody.
        java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> three = java.util.List.of(at.apply(1, 0.0), at.apply(2, near), at.apply(3, -near));
        check(clear(three, com.hex.kagune.control.npc.BubbleLayout.arrange(three)), "three talking at once: none covers another");
        // Five crowded together: no room aside for all, so some go up; still none covers another.
        java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> crowd = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            crowd.add(at.apply(i + 1, (i - 2) * 0.1));
        }
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> lifted = com.hex.kagune.control.npc.BubbleLayout.arrange(crowd);
        boolean up = false;
        for (com.hex.kagune.control.npc.BubbleLayout.Place place : lifted.values()) {
            up |= place.lift() > 0.0F;
        }
        check(clear(crowd, lifted) && up, "a crowd: some go up, none covers another");
        check(com.hex.kagune.control.npc.BubbleLayout.arrange(java.util.List.of(at.apply(2, near), at.apply(1, 0.0))).equals(
            com.hex.kagune.control.npc.BubbleLayout.arrange(java.util.List.of(at.apply(1, 0.0), at.apply(2, near)))), "the same whatever order they come in");
    }

    private static boolean clear(java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> seen,
                                 java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> placed) {
        for (com.hex.kagune.control.npc.BubbleLayout.Seen a : seen) {
            for (com.hex.kagune.control.npc.BubbleLayout.Seen b : seen) {
                if (a.id() < b.id()) {
                    double[] p = box(a, placed.get(a.id()));
                    double[] q = box(b, placed.get(b.id()));
                    if (p[0] < q[2] && q[0] < p[2] && p[1] < q[3] && q[1] < p[3]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static double[] box(com.hex.kagune.control.npc.BubbleLayout.Seen b, com.hex.kagune.control.npc.BubbleLayout.Place place) {
        double k = b.perPixel();
        double r = b.rim();
        return new double[] {b.x() + (b.x0() - r + place.shift()) * k, b.y() + (-b.y1() - r + place.lift()) * k, b.x() + (b.x1() + r + place.shift()) * k,
            b.y() + (-b.y0() + r + place.lift()) * k};
    }

    /** 1.3.71: the Brick Village: its map, and its people's sense of where they are once it is placed turned. */
    /**
     * 2.0.35: who each villager falls for (its own for life): most the other sex, a fair share both, some their own; a player who isn't who
     * it falls for is turned down kindly; every line filled, in character, never a banned name.
     */
    static void attraction() {
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura|\\{|\\}|null");
        for (boolean female : new boolean[] {true, false}) {
            int[] counts = new int[3];
            for (long seed = 0; seed < 20000; seed++) {
                counts[com.hex.kagune.control.village.HumanPersona.attraction(seed * 7919L + 13L, female).ordinal()]++;
            }
            int other = counts[(female ? com.hex.kagune.control.village.HumanPersona.Attraction.MEN : com.hex.kagune.control.village.HumanPersona.Attraction.WOMEN).ordinal()];
            int own = counts[(female ? com.hex.kagune.control.village.HumanPersona.Attraction.WOMEN : com.hex.kagune.control.village.HumanPersona.Attraction.MEN).ordinal()];
            int both = counts[com.hex.kagune.control.village.HumanPersona.Attraction.BOTH.ordinal()];
            check(other > 11500 && other < 13300 && both > 3800 && both < 5000 && own > 2700 && own < 3700,
                (female ? "women" : "men") + ": most fall for the other sex, a fair share both, some their own: " + java.util.Arrays.toString(counts));
        }
        long seed = 123456789L;
        check(com.hex.kagune.control.village.HumanPersona.attraction(seed, true) == com.hex.kagune.control.village.HumanPersona.attraction(seed, true)
            && com.hex.kagune.control.village.HumanPersona.taste(seed) == com.hex.kagune.control.village.HumanPersona.taste(seed), "its own for life");
        java.util.Set<com.hex.kagune.control.village.HumanPersona.Taste> tastes = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanPersona.Taste.class);
        for (long s = 0; s < 2000; s++) {
            tastes.add(com.hex.kagune.control.village.HumanPersona.taste(s));
        }
        check(tastes.size() == com.hex.kagune.control.village.HumanPersona.Taste.values().length, "every taste turns up");
        var MEN = com.hex.kagune.control.village.HumanPersona.Attraction.MEN;
        var WOMEN = com.hex.kagune.control.village.HumanPersona.Attraction.WOMEN;
        var BOTH = com.hex.kagune.control.village.HumanPersona.Attraction.BOTH;
        check(com.hex.kagune.control.village.HumanPersona.attracted(MEN, false) && !com.hex.kagune.control.village.HumanPersona.attracted(MEN, true)
            && com.hex.kagune.control.village.HumanPersona.attracted(WOMEN, true) && !com.hex.kagune.control.village.HumanPersona.attracted(WOMEN, false)
            && com.hex.kagune.control.village.HumanPersona.attracted(BOTH, true) && com.hex.kagune.control.village.HumanPersona.attracted(BOTH, false)
            && com.hex.kagune.control.village.HumanPersona.attracted(MEN, null) && com.hex.kagune.control.village.HumanPersona.attracted(WOMEN, null),
            "who may date whom (a player who never said may date anyone)");
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int roll = 0; roll < 6; roll++) {
                for (var a : com.hex.kagune.control.village.HumanPersona.Attraction.values()) {
                    for (var t : com.hex.kagune.control.village.HumanPersona.Taste.values()) {
                        String line = com.hex.kagune.control.village.HumanPersona.type(temper, a, t, roll);
                        check(!line.isBlank() && !banned.matcher(line).find() && line.contains(a == MEN ? "men" : a == WOMEN ? "women" : "person")
                            || a == BOTH && line.contains("Anyone"), "its type, in its own words: " + temper + " " + a + " " + t);
                    }
                }
                for (var a : new com.hex.kagune.control.village.HumanPersona.Attraction[] {MEN, WOMEN}) {
                    String no = com.hex.kagune.control.village.HumanPersona.notMyType(temper, a, roll);
                    check(!no.isBlank() && !banned.matcher(no).find() && no.toLowerCase(java.util.Locale.ROOT).contains(a == MEN ? "men" : "women")
                        && !no.toLowerCase(java.util.Locale.ROOT).contains(a == MEN ? "women" : "zzz"), "a kind no that says who it falls for: " + temper + " " + a);
                }
            }
        }
        check(com.hex.kagune.control.village.HumanPersona.trusts(com.hex.kagune.control.village.HumanPersona.Taste.DARING) > 0.0
            && com.hex.kagune.control.village.HumanPersona.trusts(com.hex.kagune.control.village.HumanPersona.Taste.FUNNY) == 0.0,
            "someone who likes a little danger takes a ghoul's confession more readily");
        check(com.hex.kagune.control.village.HumanPersona.NOT_TYPE != com.hex.kagune.control.village.HumanPersona.PARTNER
            && com.hex.kagune.control.village.HumanPersona.NOT_TYPE != com.hex.kagune.control.village.HumanPersona.CONFESSED
            && (com.hex.kagune.control.village.HumanPersona.NOT_TYPE & (com.hex.kagune.control.npc.NpcBonds.MET | com.hex.kagune.control.npc.NpcBonds.BEFRIENDED
            | com.hex.kagune.control.npc.NpcBonds.ASKED | com.hex.kagune.control.npc.NpcBonds.ALLY)) == 0, "its own bond flag");
    }

    /**
     * 2.1.3: every villager is someone (HumanLife, HumanVoice): a woman's or a man's name, a life of its own for life, every temper able to
     * say every part of it cleanly, the page offering what it may be asked as it warms, gifts landing as they should, its village's people
     * each given a different trade.
     */
    static void lives() {
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura|Touka|Hinami|Nishiki|Amon|Juuzou"
            + "|Arima|Rize|Uta\\b|Yomo|Shuu|Tsukiyama|Akira|Mutsuki|Urie|Saiko|Haise|Hide\\b|Hideyoshi|Kimi\\b|Chie\\b|Yoriko|Banjou");
        java.util.Set<String> names = new java.util.HashSet<>();
        for (boolean woman : new boolean[] {true, false}) {
            for (int i = 0; i < com.hex.kagune.control.village.HumanLife.names(woman); i++) {
                String name = com.hex.kagune.control.village.HumanLife.name(woman, i);
                check(names.add(name) && !banned.matcher(name).find() && name.matches("[A-Z][a-z]+")
                    && !name.toLowerCase(java.util.Locale.ROOT).matches(".*(kaneki|ayato|furuta).*"), "a name of its own (none banned from the jar): " + name);
            }
        }
        check(com.hex.kagune.control.village.HumanLife.names(true) >= 40 && com.hex.kagune.control.village.HumanLife.names(false) >= 40,
            "plenty of women's names and men's");
        java.util.Set<String> lives = new java.util.HashSet<>();
        java.util.EnumSet<com.hex.kagune.control.village.HumanLife.Job> jobs = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanLife.Job.class);
        java.util.EnumSet<com.hex.kagune.control.village.HumanLife.View> views = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanLife.View.class);
        for (long seed = 0; seed < 400; seed++) {
            com.hex.kagune.control.village.HumanLife.Life life = com.hex.kagune.control.village.HumanLife.of(seed * 104729L + 7L, null);
            check(life.equals(com.hex.kagune.control.village.HumanLife.of(seed * 104729L + 7L, null)), "its own for life");
            lives.add(life.job() + life.family() + life.hobby() + life.fear() + life.dream() + life.secret());
            jobs.add(life.job());
            views.add(life.view());
            check(com.hex.kagune.control.village.HumanLife.gift(life, life.loves()) == com.hex.kagune.control.village.HumanLife.Gift.LOVED
                && com.hex.kagune.control.village.HumanLife.gift(life, "minecraft:rotten_flesh") == com.hex.kagune.control.village.HumanLife.Gift.DISLIKED
                && com.hex.kagune.control.village.HumanLife.gift(life, "minecraft:cobblestone") == com.hex.kagune.control.village.HumanLife.Gift.FINE,
                "a gift lands as it should");
        }
        check(lives.size() >= 395, "no two lives alike: " + lives.size() + " of 400");
        check(jobs.size() == com.hex.kagune.control.village.HumanLife.Job.values().length && views.size() == com.hex.kagune.control.village.HumanLife.View.values().length,
            "every trade and every view of ghouls turns up");
        java.util.Set<com.hex.kagune.control.village.HumanLife.Job> village = new java.util.HashSet<>();
        for (int slot = 0; slot < 12; slot++) {
            village.add(com.hex.kagune.control.village.HumanLife.villageJob(320, -1088, slot));
        }
        check(village.size() == 12, "a village's people each have their own trade");
        // Every temper says every part of every life, cleanly.
        boolean clean = true;
        String bad = "";
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 60; seed++) {
                com.hex.kagune.control.village.HumanLife.Life life = com.hex.kagune.control.village.HumanLife.of(seed * 31L + temper.ordinal(), null);
                java.util.List<String> lines = new java.util.ArrayList<>();
                for (com.hex.kagune.control.village.HumanVoice.Topic topic : com.hex.kagune.control.village.HumanVoice.Topic.values()) {
                    if (topic != com.hex.kagune.control.village.HumanVoice.Topic.TYPE) {
                        lines.add(com.hex.kagune.control.village.HumanVoice.answer(temper, life, topic, seed * 13, seed % 2 == 0 ? null
                            : com.hex.kagune.control.village.HumanVoice.gossip("Hana", "baker", seed)));
                    }
                }
                lines.add(com.hex.kagune.control.village.HumanVoice.chat(temper, life, seed * 7));
                lines.add(com.hex.kagune.control.village.HumanVoice.caught(life, seed * 32) + "Hi.");
                for (com.hex.kagune.control.village.HumanLife.Gift how : com.hex.kagune.control.village.HumanLife.Gift.values()) {
                    lines.add(com.hex.kagune.control.village.HumanVoice.gift(temper, how, life.lovesName(), seed % 3 == 0, seed));
                }
                for (String line : lines) {
                    boolean ok = line != null && !line.isBlank() && !line.contains("{") && !line.contains("null") && line.length() <= 420
                        && !banned.matcher(line).find();
                    if (!ok && bad.isEmpty()) {
                        bad = temper + ": " + line;
                    }
                    clean &= ok;
                }
            }
        }
        check(clean, "every temper says every part of every life cleanly: " + bad);
        // The page: what may be asked, as it warms; what is not yet known first.
        java.util.List<com.hex.kagune.control.village.HumanVoice.Topic> stranger = com.hex.kagune.control.village.HumanVoice.topics(0, false, 0, 3);
        java.util.List<com.hex.kagune.control.village.HumanVoice.Topic> friend = com.hex.kagune.control.village.HumanVoice.topics(90, false, 0, 3);
        check(!stranger.contains(com.hex.kagune.control.village.HumanVoice.Topic.SECRET) && !stranger.contains(com.hex.kagune.control.village.HumanVoice.Topic.FEAR)
            && stranger.contains(com.hex.kagune.control.village.HumanVoice.Topic.WORK) && stranger.size() <= com.hex.kagune.control.village.HumanVoice.ROOM,
            "a stranger can ask what anyone could");
        check(friend.contains(com.hex.kagune.control.village.HumanVoice.Topic.SECRET) || friend.size() == com.hex.kagune.control.village.HumanVoice.ROOM,
            "a close friend can ask more");
        int known = 0;
        for (com.hex.kagune.control.village.HumanVoice.Topic topic : com.hex.kagune.control.village.HumanVoice.Topic.values()) {
            if (topic != com.hex.kagune.control.village.HumanVoice.Topic.SECRET) {
                known |= topic.flag();
            }
        }
        check(com.hex.kagune.control.village.HumanVoice.topics(90, false, known, 5).get(0) == com.hex.kagune.control.village.HumanVoice.Topic.SECRET,
            "what isn't known yet comes first");
        check((com.hex.kagune.control.village.HumanVoice.Topic.WORK.flag() & (com.hex.kagune.control.npc.NpcBonds.MET | com.hex.kagune.control.npc.NpcBonds.ASKED
            | com.hex.kagune.control.village.HumanPersona.CONFESSED | com.hex.kagune.control.village.HumanPersona.PARTNER
            | com.hex.kagune.control.village.HumanPersona.NOT_TYPE)) == 0, "learned flags past the bond's own");
    }

    /** 2.1.4: romance for every temper (HumanRomance): flirting at every warmth, a kind no, a partner's talk, more kisses, hugs and love. */
    static void romance214() {
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura");
        boolean clean = true;
        String bad = "";
        int distinct = 0;
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            java.util.Set<String> said = new java.util.HashSet<>();
            for (int seed = 0; seed < 24; seed++) {
                java.util.List<String> lines = new java.util.ArrayList<>();
                for (int regard : new int[] {0, 30, 70}) {
                    lines.add(com.hex.kagune.control.village.HumanRomance.flirt(temper, regard, true, seed));
                    lines.add(com.hex.kagune.control.village.HumanRomance.flirt(temper, regard, false, seed));
                }
                com.hex.kagune.control.village.HumanLife.Life life = com.hex.kagune.control.village.HumanLife.of(seed * 977L, null);
                for (com.hex.kagune.control.village.HumanRomance.Partner topic : com.hex.kagune.control.village.HumanRomance.Partner.values()) {
                    for (com.hex.kagune.control.village.HumanPersona.Taste taste : com.hex.kagune.control.village.HumanPersona.Taste.values()) {
                        lines.add(com.hex.kagune.control.village.HumanRomance.partner(temper, topic, taste, life.dream(), seed));
                    }
                }
                lines.add(com.hex.kagune.control.village.HumanRomance.kissed(temper, seed));
                lines.add(com.hex.kagune.control.village.HumanRomance.hugged(temper, seed));
                lines.add(com.hex.kagune.control.village.HumanRomance.loved(temper, seed));
                for (String line : lines) {
                    boolean ok = line != null && !line.isBlank() && !line.contains("{") && !line.contains("null") && line.length() <= 420
                        && !banned.matcher(line).find() && !line.contains("like I want");
                    if (!ok && bad.isEmpty()) {
                        bad = temper + ": " + line;
                    }
                    clean &= ok;
                    said.add(line);
                }
            }
            distinct += said.size();
            check(said.size() >= 60, temper + " has plenty to say in love: " + said.size());
        }
        check(clean, "every temper's romance is said cleanly: " + bad);
        check(com.hex.kagune.control.village.HumanRomance.flirted(com.hex.kagune.control.village.HumanPersona.Temper.FLIRTY, true)
            > com.hex.kagune.control.village.HumanRomance.flirted(com.hex.kagune.control.village.HumanPersona.Temper.STOIC, true)
            && com.hex.kagune.control.village.HumanRomance.flirted(com.hex.kagune.control.village.HumanPersona.Temper.KIND, false) == 0,
            "a flirt warms the flirty most, and one that doesn't land warms nobody");
    }

    /** 2.2.6: villagers come over by themselves (HumanApproach): how often by temper and warmth, why, and every temper's words. */
    static void approach226() {
        com.hex.kagune.control.village.HumanPersona.Temper[] tempers = com.hex.kagune.control.village.HumanPersona.Temper.values();
        int warm = com.hex.kagune.control.village.HumanPersona.WARM;
        int close = com.hex.kagune.control.village.HumanPersona.CLOSE;
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura");
        boolean clean = true;
        String bad = "";
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : tempers) {
            long stranger = com.hex.kagune.control.village.HumanApproach.every(temper, warm - 1, false);
            long friend = com.hex.kagune.control.village.HumanApproach.every(temper, warm, false);
            long dear = com.hex.kagune.control.village.HumanApproach.every(temper, close, false);
            long partner = com.hex.kagune.control.village.HumanApproach.every(temper, 0, true);
            check(stranger == 0L && friend > dear && dear > partner && partner >= 1200L && friend <= 30000L,
                temper + ": never to a stranger; more often the closer (" + friend + ", " + dear + ", " + partner + ")");
            java.util.EnumMap<com.hex.kagune.control.village.HumanApproach.Why, Integer> seen = new java.util.EnumMap<>(com.hex.kagune.control.village.HumanApproach.Why.class);
            for (int roll = 0; roll < 2000; roll++) {
                com.hex.kagune.control.village.HumanApproach.Why friendly = com.hex.kagune.control.village.HumanApproach.pick(temper, close, false, false, false, false, roll);
                check(!friendly.partners(), temper + ": a friend never comes for a partner's reasons");
                com.hex.kagune.control.village.HumanApproach.Why loved = com.hex.kagune.control.village.HumanApproach.pick(temper, 0, true, false, roll % 2 == 0, false, roll);
                seen.merge(loved, 1, Integer::sum);
                seen.merge(friendly, 1, Integer::sum);
                check(com.hex.kagune.control.village.HumanApproach.pick(temper, warm, false, false, false, false, roll)
                    != com.hex.kagune.control.village.HumanApproach.Why.GIFT, temper + ": only one close to them brings a gift");
                check(com.hex.kagune.control.village.HumanApproach.pick(temper, 100, true, false, true, true, roll)
                    != com.hex.kagune.control.village.HumanApproach.Why.GIFT, temper + ": one gift a day");
                check(com.hex.kagune.control.village.HumanApproach.pick(temper, warm, false, true, false, false, roll)
                    == com.hex.kagune.control.village.HumanApproach.Why.WORRIED, temper + ": worried when they're hurt");
            }
            for (com.hex.kagune.control.village.HumanApproach.Why why : com.hex.kagune.control.village.HumanApproach.Why.values()) {
                check(why == com.hex.kagune.control.village.HumanApproach.Why.WORRIED || seen.getOrDefault(why, 0) > 0, temper + ": comes over for " + why);
                for (int roll = 0; roll < 4; roll++) {
                    String line = com.hex.kagune.control.village.HumanApproach.line(temper, why, "honey bottles", "The fog's back.", roll);
                    boolean ok = why == com.hex.kagune.control.village.HumanApproach.Why.WALK ? line.isEmpty()
                        : line.length() > 3 && !line.contains("{") && !banned.matcher(line).find() && line.length() <= 300
                        && (why != com.hex.kagune.control.village.HumanApproach.Why.GIFT || line.contains("oney bottles"))
                        && (why != com.hex.kagune.control.village.HumanApproach.Why.NEWS && why != com.hex.kagune.control.village.HumanApproach.Why.GOSSIP
                            || line.endsWith("The fog's back."));
                    if (!ok && bad.isEmpty()) {
                        bad = temper + " " + why + ": " + line;
                    }
                    clean &= ok;
                }
            }
        }
        check(clean, "every temper's words for coming over: " + bad);
        check(com.hex.kagune.control.village.HumanApproach.every(com.hex.kagune.control.village.HumanPersona.Temper.FLIRTY, close, false)
            < com.hex.kagune.control.village.HumanApproach.every(com.hex.kagune.control.village.HumanPersona.Temper.SHY, close, false),
            "the flirty come over more often than the shy");
        // The gift, as said.
        check(com.hex.kagune.control.village.HumanApproach.thing("cookies", 4).equals("cookies")
            && com.hex.kagune.control.village.HumanApproach.thing("iron ingot", 1).equals("an iron ingot")
            && com.hex.kagune.control.village.HumanApproach.thing("lantern", 1).equals("a lantern")
            && com.hex.kagune.control.village.HumanApproach.thing("bread", 2).equals("bread")
            && com.hex.kagune.control.village.HumanApproach.thing("honeycomb", 1).equals("honeycomb")
            && com.hex.kagune.control.village.HumanApproach.line(com.hex.kagune.control.village.HumanPersona.Temper.STOIC,
                com.hex.kagune.control.village.HumanApproach.Why.GIFT, "an iron ingot", "", 0).startsWith("An iron ingot"),
            "a gift said rightly (\"an iron ingot\", \"cookies\", \"bread\")");
        check(com.hex.kagune.control.village.HumanApproach.gives(12) == 4 && com.hex.kagune.control.village.HumanApproach.gives(1) == 1,
            "a third of a ware's lot, at least one");
        // 2.3.4: a human partner's ring and a kiss left hanging, every temper; married past every other flag of a human's bond.
        int topicFlags = 0;
        for (com.hex.kagune.control.village.HumanVoice.Topic topic : com.hex.kagune.control.village.HumanVoice.Topic.values()) {
            topicFlags |= topic.flag();
        }
        check((com.hex.kagune.control.village.HumanPersona.MARRIED & (topicFlags | com.hex.kagune.control.village.HumanPersona.CONFESSED
            | com.hex.kagune.control.village.HumanPersona.PARTNER | com.hex.kagune.control.village.HumanPersona.NOT_TYPE
            | com.hex.kagune.control.village.HumanPersona.LEGACY | 7)) == 0, "married: a flag of its own");
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : tempers) {
            for (int roll = 0; roll < 4; roll++) {
                check(com.hex.kagune.control.village.HumanRomance.proposed(temper, roll).length() > 3
                    && com.hex.kagune.control.village.HumanRomance.hanging(temper, roll).length() > 3, temper + ": a ring's answer, a kiss left hanging");
            }
        }
        // Every trade's wares can be given, and said.
        for (com.hex.kagune.control.village.HumanLife.Job job : com.hex.kagune.control.village.HumanLife.Job.values()) {
            check(!job.wares.isEmpty(), job + ": something to give");
            for (com.hex.kagune.control.village.VillageShop.Offer ware : job.wares) {
                String thing = com.hex.kagune.control.village.HumanApproach.thing(ware.name(), com.hex.kagune.control.village.HumanApproach.gives(ware.count()));
                check(!thing.isBlank() && !thing.startsWith("a a") && !thing.startsWith("a e") && !thing.startsWith("a i"), job + ": " + thing);
            }
        }
    }

    /** 2.4.7: villagers' favours: what they ask, kept with the player, every temper's words. */
    static void errands247() {
        java.util.EnumMap<com.hex.kagune.control.village.HumanErrand.Kind, Integer> kinds = new java.util.EnumMap<>(com.hex.kagune.control.village.HumanErrand.Kind.class);
        for (long roll = 0; roll < 4000; roll++) {
            com.hex.kagune.control.village.HumanErrand.Errand errand = com.hex.kagune.control.village.HumanErrand.roll(roll * 7919L + (roll << 9), "Hana");
            kinds.merge(errand.kind(), 1, Integer::sum);
            check(errand.count() >= 1 && errand.emeralds() >= 2 && errand.emeralds() <= 8, "a favour pays in emeralds: " + errand);
            if (errand.kind() == com.hex.kagune.control.village.HumanErrand.Kind.FETCH) {
                check(errand.item().startsWith("minecraft:") && !errand.name().isEmpty(), "a thing to fetch: " + errand);
            }
            String line = com.hex.kagune.control.village.HumanErrand.save("u-1", "Mai", errand, 1, 500L);
            com.hex.kagune.control.village.HumanErrand.Open back = com.hex.kagune.control.village.HumanErrand.load(line);
            check(back != null && back.errand().equals(errand) && back.done() == 1 && back.until() == 500L && back.giverName().equals("Mai"),
                "kept with the player and back: " + line);
        }
        check(kinds.size() == 3, "fetching, hunting and visiting all come up: " + kinds);
        check(com.hex.kagune.control.village.HumanErrand.roll(5L, null).kind() != com.hex.kagune.control.village.HumanErrand.Kind.VISIT,
            "no visit with no neighbour");
        check(com.hex.kagune.control.village.HumanErrand.load("") == null && com.hex.kagune.control.village.HumanErrand.load("junk") == null, "none, or junk, is none");
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura");
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (com.hex.kagune.control.village.HumanErrand.Kind kind : com.hex.kagune.control.village.HumanErrand.Kind.values()) {
                for (int seed = 0; seed < 4; seed++) {
                    com.hex.kagune.control.village.HumanErrand.Errand errand = new com.hex.kagune.control.village.HumanErrand.Errand(kind, "minecraft:coal",
                        "coal", 6, "Hana", 3);
                    String ask = com.hex.kagune.control.village.HumanErrand.ask(temper, errand, seed);
                    check(ask.length() > 10 && !ask.contains("{") && !banned.matcher(ask).find(), temper + " " + kind + ": " + ask);
                }
            }
            check(com.hex.kagune.control.village.HumanErrand.thanks(temper, 1).length() > 3 && com.hex.kagune.control.village.HumanErrand.dropped(temper, 1).length() > 3,
                temper + ": thanks and a let-down");
        }
        check(com.hex.kagune.control.village.HumanErrand.visited("Mai", 2).contains("Mai"), "the neighbour knows who sent them");
        check(com.hex.kagune.control.village.HumanErrand.OFFER_EVERY >= 6000L && com.hex.kagune.control.village.HumanErrand.CHANCE <= 0.05, "rare");
    }

    static void village() {
        // 2.0.13: trading with village humans: seven wares each (2.0.14: a staple, two more foods, two farming goods, two basic goods), never meat or fish; every
        // non-meat ingredient the cookbook needs is sold by someone; what they buy, and the page's words.
        {
            boolean six = true;
            boolean noMeat = true;
            java.util.Set<String> sold = new java.util.HashSet<>();
            java.util.Set<String> staples = new java.util.HashSet<>();
            for (com.hex.kagune.control.village.VillageShop.Offer o : com.hex.kagune.control.village.VillageShop.STAPLES) {
                staples.add(o.item());
            }
            for (long seed = 0; seed < 600; seed++) {
                java.util.List<com.hex.kagune.control.village.VillageShop.Offer> stock = com.hex.kagune.control.village.VillageShop.stock(seed * 7919L);
                com.hex.kagune.control.village.HumanLife.Job job = com.hex.kagune.control.village.HumanLife.of(seed * 7919L, null).job();
                six &= stock.size() == 7 && stock.stream().map(com.hex.kagune.control.village.VillageShop.Offer::item).distinct().count() == 7
                    && stock.get(0).equals(job.wares.get(0)) && stock.equals(com.hex.kagune.control.village.VillageShop.stock(seed * 7919L))
                    && stock.subList(0, com.hex.kagune.control.village.VillageShop.TRADE_OFFERS).stream().allMatch(job.wares::contains)
                    && stock.size() <= com.hex.kagune.control.npc.NpcTalk.Choice.SAYS.length - 1;
                for (com.hex.kagune.control.village.VillageShop.Offer o : stock) {
                    sold.add(o.item());
                    noMeat &= !java.util.regex.Pattern.compile("beef|porkchop|chicken|mutton|rabbit|cod|salmon|tropical_fish|pufferfish|hexkagune:|flesh")
                        .matcher(o.item()).find() && o.count() > 0 && o.price() > 0 && o.count() <= 64;
                }
            }
            check(six, "each sells seven things for life (2.1.3: five of its trade's own, its trade's first first), none twice, one slot left to sell");
            for (com.hex.kagune.control.village.HumanLife.Job job : com.hex.kagune.control.village.HumanLife.Job.values()) {
                check(job.wares.size() >= com.hex.kagune.control.village.VillageShop.TRADE_OFFERS + 1
                    && job.wares.stream().map(com.hex.kagune.control.village.VillageShop.Offer::item).distinct().count() == job.wares.size()
                    && job.wares.stream().allMatch(o -> o.item().matches("minecraft:[a-z_]+")), job + ": enough wares of its own, none twice");
            }
            check(noMeat, "no meat, no fish, sensible lots");
            java.util.Set<String> needed = new java.util.TreeSet<>();
            for (com.hex.kagune.control.kitchen.Dish dish : com.hex.kagune.control.kitchen.Menu.DISHES) {
                if (dish.recipe() != null) {
                    for (String item : dish.recipe().ingredients()) {
                        if (item.startsWith("minecraft:") && !item.equals("minecraft:cod")) {
                            needed.add(item);
                        }
                    }
                }
            }
            needed.removeAll(sold);
            check(needed.isEmpty(), "every non-meat ingredient of the cookbook is sold in some village: missing " + needed);
            com.hex.kagune.control.village.VillageShop.Offer wheat = com.hex.kagune.control.village.VillageShop.buys("minecraft:wheat");
            check(wheat != null && com.hex.kagune.control.village.VillageShop.buys("minecraft:diamond") == null
                && com.hex.kagune.control.village.VillageShop.lots(wheat, 45) == 2
                && com.hex.kagune.control.village.VillageShop.sellLabel(wheat, 45).equals("Sell 40 wheat: 2 emeralds")
                && com.hex.kagune.control.village.VillageShop.STAPLES[0].label().equals("12 wheat: 1 emerald"), "what it buys and how the page says it");
            boolean said = true;
            for (int r = 0; r < 12; r++) {
                for (String line : new String[] {com.hex.kagune.control.village.VillageShop.hello(r), com.hex.kagune.control.village.VillageShop.done(r),
                    com.hex.kagune.control.village.VillageShop.short_(wheat, 0, r), com.hex.kagune.control.village.VillageShop.short_(wheat, 3, r),
                    com.hex.kagune.control.village.VillageShop.sold(wheat, r), com.hex.kagune.control.village.VillageShop.bought(wheat, 2, r),
                    com.hex.kagune.control.village.VillageShop.purse(0), com.hex.kagune.control.village.VillageShop.purse(5)}) {
                    said &= line != null && !line.isBlank() && !line.contains("null");
                }
            }
            check(said, "the trade's words are all there");
        }
        // 1.3.75: the villagers' own skins exist, are painted for a player's body, and every new look maps to one of them.
        boolean skins = com.hex.kagune.control.village.HumanSkins.ALL.length >= 8;
        for (com.hex.kagune.control.village.HumanSkins.Skin s : com.hex.kagune.control.village.HumanSkins.ALL) {
            skins &= new java.io.File("src/main/resources/assets/hexkagune/" + s.path()).isFile();
        }
        for (int roll = -50; roll < 50; roll++) {
            int look = com.hex.kagune.control.npc.HumanRules.pick(roll * 7919);
            skins &= look >= com.hex.kagune.control.npc.HumanRules.CUTE && "hexkagune".equals(com.hex.kagune.control.npc.HumanRules.namespace(look))
                && com.hex.kagune.control.npc.HumanRules.skin(look).startsWith("textures/entity/human/human_");
        }
        skins &= "minecraft".equals(com.hex.kagune.control.npc.HumanRules.namespace(3))
            && com.hex.kagune.control.npc.HumanRules.skin(3).startsWith("textures/entity/player/");
        check(skins, "the villagers' own skins: files there, new looks map to them, old looks keep vanilla's");
        // 1.3.79: children's looks: every version's texture there, its own look first, the nearest look picked by hair and eyes.
        int versions = 0;
        boolean looked = true;
        for (com.hex.kagune.control.family.ChildSkins.Skin s : com.hex.kagune.control.family.ChildSkins.ALL) {
            com.hex.kagune.control.family.ChildLooks.Look[] looks = com.hex.kagune.control.family.ChildLooks.of(s.key());
            looked &= looks.length >= 1 && looks[0].texture().equals(s.key());
            for (com.hex.kagune.control.family.ChildLooks.Look look : looks) {
                looked &= new java.io.File("src/main/resources/assets/hexkagune/textures/entity/child/skins/" + look.texture() + ".png").isFile();
                looked &= com.hex.kagune.control.family.ChildLooks.pick(s.key(), look.hair(), look.eyes()) == look;
            }
            versions += looks.length - 1;
        }
        check(looked && versions >= 100, "children's looks: " + versions + " versions with other hair and eyes, each picked by its own colours");
        // 2.0: a village's people never share a look or a name (16 of them: two in each of its 8 houses), whatever village it is.
        boolean unique = com.hex.kagune.control.village.HumanSkins.ALL.length >= 16;
        java.util.Set<Integer> firsts = new java.util.HashSet<>();
        for (int v = 0; v < 200; v++) {
            int mx = v * 977 - 5000;
            int mz = v * -613 + 300;
            java.util.Set<Integer> looks = new java.util.HashSet<>();
            java.util.Set<Integer> names = new java.util.HashSet<>();
            for (int slot = 0; slot < 16; slot++) {
                int look = com.hex.kagune.control.npc.HumanRules.villageLook(mx, mz, slot);
                unique &= look >= com.hex.kagune.control.npc.HumanRules.CUTE && looks.add(look);
                unique &= names.add(com.hex.kagune.control.npc.HumanRules.villageName(mx, mz, slot, com.hex.kagune.control.village.HumanPersona.names()));
            }
            firsts.add(com.hex.kagune.control.npc.HumanRules.villageLook(mx, mz, 0));
        }
        check(unique && firsts.size() >= 10, "no two of a village's 16 people share a look or a name; villages shuffle differently");
        // 1.3.76: blinks: every skin that blinks has its shut eyes; villagers blink with their child skin's; vanilla's looks have none.
        int blinking = 0;
        boolean shut = com.hex.kagune.control.npc.HumanRules.closed(3) == null;
        for (com.hex.kagune.control.family.ChildSkins.Skin s : com.hex.kagune.control.family.ChildSkins.ALL) {
            if (com.hex.kagune.control.family.ChildBlinks.has(s.key())) {
                blinking++;
                shut &= new java.io.File("src/main/resources/assets/hexkagune/" + com.hex.kagune.control.family.ChildBlinks.closed(s.key())).isFile();
            }
        }
        for (int i = 0; i < com.hex.kagune.control.village.HumanSkins.ALL.length; i++) {
            String path = com.hex.kagune.control.npc.HumanRules.closed(com.hex.kagune.control.npc.HumanRules.CUTE + i);
            shut &= (path != null) == com.hex.kagune.control.family.ChildBlinks.has(com.hex.kagune.control.village.HumanSkins.ALL[i].child());
        }
        check(shut && blinking >= 25, "shut eyes for blinks: " + blinking + " child skins, the villagers' from theirs");
        int[][] streets = com.hex.kagune.control.village.VillageMap.STREETS;
        int[][] route = com.hex.kagune.control.village.VillageMap.ROUTE;
        int[][] homes = com.hex.kagune.control.village.VillageMap.HOMES;
        int perimeter = com.hex.kagune.control.village.VillageMap.PERIMETER;
        check(streets.length > 100 && route.length >= 8 && homes.length >= 6, "a village of streets, a patrol loop and homes ("
            + streets.length + ", " + route.length + ", " + homes.length + ")");
        boolean in = true;
        for (int[][] set : new int[][][] {streets, route, homes}) {
            for (int[] p : set) {
                in &= com.hex.kagune.control.village.VillageRules.inside(p[0], p[2], perimeter) && p.length == 3;
            }
        }
        check(in, "every street spot, patrol spot and home within the perimeter");
        boolean ground = true;
        for (int[] p : route) {
            ground &= p[1] >= -4 && p[1] <= 4;
        }
        check(ground, "the patrol stays near the ground (never a roof)");
        check(homes.length == 13 && perimeter <= 128, "13 homes (26 villagers, 26 looks), the village within 128 of its middle");
        // 2.0.4: the world's own ground cleared over the village, from just over its own highest block, never below its ground.
        boolean clears = com.hex.kagune.control.village.VillageTops.clearFrom(-1, 5) == -1
            && com.hex.kagune.control.village.VillageTops.clearFrom(5, com.hex.kagune.control.village.VillageTops.SIZE) == -1;
        for (int x = 0; x < com.hex.kagune.control.village.VillageTops.SIZE; x += 7) {
            for (int z = 0; z < com.hex.kagune.control.village.VillageTops.SIZE; z += 7) {
                int from = com.hex.kagune.control.village.VillageTops.clearFrom(x, z);
                clears &= from > com.hex.kagune.control.village.VillageTops.GROUND && from > com.hex.kagune.control.village.VillageTops.top(x, z);
            }
        }
        check(clears && com.hex.kagune.control.village.VillageTops.MIDDLE == 102 && com.hex.kagune.control.village.VillageTops.top(128, 128) >= 0,
            "the world's ground cleared over the village, never into it");
        // 2.0.1: only on dry, fairly flat land.
        int n = com.hex.kagune.control.village.VillageGround.SAMPLES.length;
        int[] flat = new int[n];
        java.util.Arrays.fill(flat, 70);
        int[] wet = flat.clone();
        wet[12] = 72;
        int[] hill = flat.clone();
        hill[3] = 70 + com.hex.kagune.control.village.VillageGround.FLAT + 1;
        // 2.0.2: a little water at the rim (sample 0 is a corner, 1 and 2 on the edge): a river or a pond, never the sea.
        int[] rim = flat.clone();
        rim[0] = 72;
        rim[1] = 72;
        int[] wider = rim.clone();
        wider[2] = 72;
        int[] low = flat.clone();
        low[0] = 40;
        check(n == 25 && com.hex.kagune.control.village.VillageGround.suits(flat, flat)
            && !com.hex.kagune.control.village.VillageGround.suits(wet, flat) && !com.hex.kagune.control.village.VillageGround.suits(hill, hill)
            && com.hex.kagune.control.village.VillageGround.suits(rim, low) && !com.hex.kagune.control.village.VillageGround.suits(wider, flat)
            && !com.hex.kagune.control.village.VillageGround.suits(new int[0], new int[0]), "a village only on dry, fairly flat land");
        for (int i = 0; i < route.length; i++) {
            int[] a = route[i];
            int[] b = route[(i + 1) % route.length];
            check(Math.hypot(a[0] - b[0], a[2] - b[2]) < 24.0, "the patrol's steps are short (" + i + ")");
        }
        // Turns: as the game turns a template (CLOCKWISE_90: x, z to -z, x), and a yaw-0 body's yaw with it (+90 a quarter turn).
        check(com.hex.kagune.control.village.VillageRules.turns(0.0F) == 0 && com.hex.kagune.control.village.VillageRules.turns(90.0F) == 1
            && com.hex.kagune.control.village.VillageRules.turns(180.0F) == 2 && com.hex.kagune.control.village.VillageRules.turns(-90.0F) == 3
            && com.hex.kagune.control.village.VillageRules.turns(269.0F) == 3 && com.hex.kagune.control.village.VillageRules.turns(-180.0F) == 2,
            "the village's turn read off a body placed facing south");
        int[] q = com.hex.kagune.control.village.VillageRules.turn(3, 5, 1);
        check(q[0] == -5 && q[1] == 3, "a quarter turn clockwise: x, z to -z, x");
        boolean back = true;
        for (int t = 0; t < 4; t++) {
            int[] r = com.hex.kagune.control.village.VillageRules.unturn(com.hex.kagune.control.village.VillageRules.turn(7, -2, t)[0],
                com.hex.kagune.control.village.VillageRules.turn(7, -2, t)[1], t);
            back &= r[0] == 7 && r[1] == -2;
        }
        check(back, "turned and turned back, the same spot");
        double[] c = com.hex.kagune.control.village.VillageRules.clamp(200.0, -3.0, perimeter, 2.0);
        check(c[0] == perimeter - 2.0 && c[1] == -3.0 && !com.hex.kagune.control.village.VillageRules.inside(perimeter + 1.0, 0.0, perimeter),
            "running, never past the perimeter");
        check(com.hex.kagune.control.village.VillageRules.next(route.length - 1, route.length, true) == 0
            && com.hex.kagune.control.village.VillageRules.next(0, route.length, false) == route.length - 1
            && com.hex.kagune.control.village.VillageRules.nearest(route, route[3][0], route[3][2]) == 3, "the loop walked round, either way");
        // 1.3.72: trading in the streets, and a word in passing.
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura|\\{|\\}|null");
        java.util.Set<String> said = new java.util.HashSet<>();
        boolean trades = true;
        java.util.Set<String> known = java.util.Set.of("minecraft:bread", "minecraft:apple", "minecraft:carrot", "minecraft:baked_potato",
            "minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:cod", "minecraft:sweet_berries", "minecraft:honey_bottle", "minecraft:poppy",
            "minecraft:paper", "minecraft:candle", "minecraft:emerald");
        for (com.hex.kagune.control.village.VillageTalk.Goods goods : com.hex.kagune.control.village.VillageTalk.GOODS) {
            trades &= known.contains(goods.item());
            for (int seed = 0; seed < 600; seed += 7) {
                java.util.List<com.hex.kagune.control.village.VillageTalk.Line> lines = com.hex.kagune.control.village.VillageTalk.trade(goods, seed);
                int hands = 0;
                for (com.hex.kagune.control.village.VillageTalk.Line line : lines) {
                    hands += line.hands() ? 1 : 0;
                    trades &= !line.text().isBlank() && !banned.matcher(line.text()).find();
                    said.add(line.text());
                }
                trades &= lines.size() >= 4 && hands == 1 && lines.get(0).seller() && !lines.get(1).seller() && !lines.get(3).seller()
                    && lines.get(3).hands() && lines.get(0).text().toLowerCase(java.util.Locale.ROOT).contains(goods.name().split(" ")[0]);
            }
        }
        check(trades && known.contains(com.hex.kagune.control.village.VillageTalk.COIN), "every trade: goods 1.20.1 has, the seller first, "
            + "the coin and the goods changing hands once, the goods named, nothing that must stay out");
        for (int seed = 0; seed < 40; seed++) {
            String greet = com.hex.kagune.control.village.VillageTalk.greet(seed);
            String dusk = com.hex.kagune.control.village.VillageTalk.dusk(seed);
            check(!greet.isBlank() && !dusk.isBlank() && !banned.matcher(greet + dusk).find(), "a word in passing, and at dusk");
            said.add(greet);
            said.add(dusk);
        }
        check(said.size() > 100, "plenty of different words in the streets (" + said.size() + ")");
        // 1.3.73: talking with them: a name and a temper each, every word in its temper's way.
        java.util.EnumSet<com.hex.kagune.control.village.HumanPersona.Temper> tempers = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanPersona.Temper.class);
        java.util.Set<String> names = new java.util.HashSet<>();
        // 2.0.36: a villager placed before keeps one of the first eight (by its seed); one placed since rolls any of them.
        java.util.EnumSet<com.hex.kagune.control.village.HumanPersona.Temper> rolled = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanPersona.Temper.class);
        boolean legacy = true;
        for (long seed = 0; seed < 4000; seed += 13) {
            com.hex.kagune.control.village.HumanPersona.Temper old = com.hex.kagune.control.village.HumanPersona.temper(seed * 7919L);
            legacy &= old.ordinal() < com.hex.kagune.control.village.HumanPersona.LEGACY;
            tempers.add(old);
            rolled.add(com.hex.kagune.control.village.HumanPersona.rolled(seed * 7919L));
            names.add(com.hex.kagune.control.village.HumanPersona.name(seed * 104729L));
        }
        check(legacy && tempers.size() == com.hex.kagune.control.village.HumanPersona.LEGACY && names.size() > 12,
            "every old temper and plenty of names turn up, a villager placed before keeping its own (" + tempers.size() + ", " + names.size() + ")");
        check(rolled.size() == com.hex.kagune.control.village.HumanPersona.Temper.values().length && com.hex.kagune.control.village.HumanPersona.Temper.values().length == 12,
            "2.0.36: a new villager may have any of the twelve tempers (" + rolled.size() + ")");
        for (String name : names) {
            check(!banned.matcher(name).find() && !java.util.Set.of("Eto", "Touka", "Ken").contains(name), "a name of its own: " + name);
        }
        java.util.Set<String> talk = new java.util.HashSet<>();
        boolean spoken = true;
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            spoken &= com.hex.kagune.control.village.HumanPersona.liking(temper) > 0;
            for (int regard : new int[] {-50, 0, 30, 80}) {
                for (int seed = 0; seed < 6; seed++) {
                    for (String line : new String[] {com.hex.kagune.control.village.HumanPersona.hello(temper, regard, seed),
                        com.hex.kagune.control.village.HumanPersona.chat(temper, seed), com.hex.kagune.control.village.HumanPersona.about(temper, seed),
                        com.hex.kagune.control.village.HumanPersona.news(seed), com.hex.kagune.control.village.HumanPersona.compliment(temper, regard, seed),
                        com.hex.kagune.control.village.HumanPersona.bye(temper, regard, seed)}) {
                        spoken &= line != null && !line.isBlank() && !banned.matcher(line).find();
                        talk.add(line);
                    }
                }
            }
        }
        check(spoken, "every temper says something to everything, warm or not, and likes a compliment");
        check(talk.size() >= 120, "plenty of different words when talked to (" + talk.size() + ")");
        check(!com.hex.kagune.control.village.HumanPersona.hello(com.hex.kagune.control.village.HumanPersona.Temper.SHY, 0, 1)
            .equals(com.hex.kagune.control.village.HumanPersona.hello(com.hex.kagune.control.village.HumanPersona.Temper.SHY, 90, 1)),
            "a friend is greeted otherwise than a stranger");
        // 1.3.77: gestures: every temper moves with everything it says, real moves (no props), coded and decoded whole.
        boolean moves = true;
        java.util.Set<Integer> kinds = new java.util.HashSet<>();
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 12; seed++) {
                java.util.List<Integer> codes = new java.util.ArrayList<>(java.util.List.of(
                    com.hex.kagune.control.village.HumanGestures.hello(temper, false, seed), com.hex.kagune.control.village.HumanGestures.hello(temper, true, seed),
                    com.hex.kagune.control.village.HumanGestures.chat(temper, seed), com.hex.kagune.control.village.HumanGestures.about(temper, seed),
                    com.hex.kagune.control.village.HumanGestures.news(seed), com.hex.kagune.control.village.HumanGestures.compliment(temper, seed),
                    com.hex.kagune.control.village.HumanGestures.yes(temper), com.hex.kagune.control.village.HumanGestures.love(temper, seed),
                    com.hex.kagune.control.village.HumanGestures.bye(temper, seed), com.hex.kagune.control.village.HumanGestures.greet(seed),
                    com.hex.kagune.control.village.HumanGestures.dusk(seed), com.hex.kagune.control.village.HumanGestures.idle(temper, seed)));
                for (com.hex.kagune.control.village.HumanPersona.Answer answer : com.hex.kagune.control.village.HumanPersona.Answer.values()) {
                    codes.add(com.hex.kagune.control.village.HumanGestures.confessed(answer));
                }
                for (int code : codes) {
                    boolean cue = com.hex.kagune.control.village.HumanGestures.cue(code);
                    com.hex.kagune.control.npc.EtoGesture g = com.hex.kagune.control.village.HumanGestures.gesture(code);
                    moves &= code > 0 && com.hex.kagune.control.village.HumanGestures.ticks(code) > 0
                        && (cue ? com.hex.kagune.control.village.HumanGestures.childCue(code) != com.hex.kagune.control.family.ChildCue.NONE
                            : g != com.hex.kagune.control.npc.EtoGesture.NONE && g.mainHand == null && g.offHand == null);
                    int packed = com.hex.kagune.control.village.HumanGestures.pack(code, com.hex.kagune.control.village.HumanGestures.style(temper), seed * 977 + 5);
                    moves &= com.hex.kagune.control.village.HumanGestures.code(packed) == code
                        && com.hex.kagune.control.village.HumanGestures.style(packed) == com.hex.kagune.control.village.HumanGestures.style(temper)
                        && com.hex.kagune.control.village.HumanGestures.count(packed) == seed * 977 + 5;
                    kinds.add(code);
                }
                for (String line : new String[] {com.hex.kagune.control.village.HumanPersona.kissed(temper, seed),
                    com.hex.kagune.control.village.HumanPersona.hugged(temper, seed)}) {
                    moves &= line != null && !line.isBlank() && !banned.matcher(line).find();
                }
            }
        }
        int kiss = com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.KISS);
        check(moves && kinds.size() >= 25, "villagers move with their words: " + kinds.size() + " different moves, none with a prop");
        check(com.hex.kagune.control.village.HumanGestures.ticks(kiss) == com.hex.kagune.control.village.HumanGestures.HELD
            && com.hex.kagune.control.village.HumanGestures.eyesClosed(kiss)
            && com.hex.kagune.control.village.HumanGestures.blush(com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.KISSED))
            && com.hex.kagune.control.village.HumanGestures.still(kiss), "a kiss: held a while, eyes shut, standing still; then a blush");
        boolean pink = true;
        for (int i = 0; i < com.hex.kagune.control.village.HumanSkins.ALL.length; i++) {
            String path = com.hex.kagune.control.npc.HumanRules.blush(com.hex.kagune.control.npc.HumanRules.CUTE + i);
            pink &= path == null || new java.io.File("src/main/resources/assets/hexkagune/" + path).isFile();
        }
        check(pink && com.hex.kagune.control.npc.HumanRules.blush(2) == null, "villagers' blushes are there");
        // 1.3.78: dates: hand in hand, standing so the two hands meet; something to say for each moment, by every temper.
        boolean dated = true;
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 6; seed++) {
                for (String line : new String[] {com.hex.kagune.control.village.HumanPersona.walk(temper, seed),
                    com.hex.kagune.control.village.HumanPersona.stroll(temper, seed), com.hex.kagune.control.village.HumanPersona.letGo(temper, seed),
                    com.hex.kagune.control.village.HumanPersona.edge(temper, seed)}) {
                    dated &= line != null && !line.isBlank() && !banned.matcher(line).find();
                }
            }
        }
        double apart = com.hex.kagune.control.village.HumanHand.out(false, false);
        double[] right = com.hex.kagune.control.village.HumanHand.offset(0.0F, apart);
        check(dated && apart > 1.0 && apart < 1.3 && com.hex.kagune.control.village.HumanHand.out(true, true) < apart
            && Math.abs(right[0] + apart) < 1.0E-9 && Math.abs(right[1]) < 1.0E-9, "a date: hand in hand at its partner's right, a word for each moment");
        // 1.3.74: romance: a ghoul's confession first (may take convincing), then together.
        com.hex.kagune.control.village.HumanPersona.Temper kind = com.hex.kagune.control.village.HumanPersona.Temper.KIND;
        com.hex.kagune.control.village.HumanPersona.Temper nervous = com.hex.kagune.control.village.HumanPersona.Temper.NERVOUS;
        int[] yes = new int[2];
        boolean shape = true;
        for (int i = 0; i < 1000; i++) {
            double roll = i / 1000.0;
            com.hex.kagune.control.village.HumanPersona.Answer first = com.hex.kagune.control.village.HumanPersona.confession(kind, 60, false, roll);
            com.hex.kagune.control.village.HumanPersona.Answer plea = com.hex.kagune.control.village.HumanPersona.confession(nervous, 60, true, roll);
            shape &= first != com.hex.kagune.control.village.HumanPersona.Answer.NO && plea != com.hex.kagune.control.village.HumanPersona.Answer.DOUBT;
            yes[0] += com.hex.kagune.control.village.HumanPersona.confession(kind, 60, false, roll) == com.hex.kagune.control.village.HumanPersona.Answer.YES ? 1 : 0;
            yes[1] += com.hex.kagune.control.village.HumanPersona.confession(nervous, 60, false, roll) == com.hex.kagune.control.village.HumanPersona.Answer.YES ? 1 : 0;
        }
        check(shape, "a first try never a flat no (a doubt: they may plead), a plea never another doubt");
        check(yes[0] > yes[1] * 2 && yes[1] > 0, "the kind believe a ghoul far sooner than the nervous (" + yes[0] + " to " + yes[1] + " in 1000)");
        check(com.hex.kagune.control.village.HumanPersona.confession(nervous, 100, true, 0.8) == com.hex.kagune.control.village.HumanPersona.Answer.YES,
            "close enough and pleading, even the nervous may believe");
        check(com.hex.kagune.control.village.HumanPersona.CONFESSED != com.hex.kagune.control.village.HumanPersona.PARTNER
            && (com.hex.kagune.control.village.HumanPersona.CONFESSED & 15) == 0 && (com.hex.kagune.control.village.HumanPersona.PARTNER & 15) == 0,
            "the romance's flags clear of the bond's own (met, befriended, asked, ally)");
        boolean romance = true;
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 4; seed++) {
                for (com.hex.kagune.control.village.HumanPersona.Answer answer : com.hex.kagune.control.village.HumanPersona.Answer.values()) {
                    String line = com.hex.kagune.control.village.HumanPersona.confessed(temper, answer, seed);
                    romance &= !line.isBlank() && !banned.matcher(line).find();
                }
                for (String line : new String[] {com.hex.kagune.control.village.HumanPersona.yes(temper, seed),
                    com.hex.kagune.control.village.HumanPersona.love(temper, seed), com.hex.kagune.control.village.HumanPersona.hello(temper, 90, true, seed)}) {
                    romance &= !line.isBlank() && !banned.matcher(line).find();
                }
            }
        }
        check(romance, "every temper has its words for a confession (yes, doubt, no), a yes, love, and a partner's hello");
    }

    /**
     * 4.1.1 (the owner: "bug, make sure NONE of the structures spawns in water or collides with water bodies, the mansion is currently happily
     * resting at the very bottom of the ocean..."): every place on dry land, none in or beside water; the world's one hideout at the first
     * dry offer, the same however it is asked.
     */
    static void dryLand() {
        int margin = com.hex.kagune.control.village.DryLand.MARGIN;
        int step = com.hex.kagune.control.village.DryLand.STEP;
        int fine = com.hex.kagune.control.village.DryLand.BIOME_STEP;
        check(step <= 16 && fine <= 4 && margin >= 8, "the ground looked at every 16 blocks, its biome every 4 (the biomes' own grain), 8 out round it");
        // The columns looked at: all over the footprint and its margin, never further apart than their step, the coarse ones first.
        int[][] sizes = {{1, 1}, {7, 5}, {33, 17}, {77, 130}, {272, 263}, {73, 73}};
        for (int[] size : sizes) {
            for (int every : new int[] {step, fine}) {
                int minX = -37;
                int minZ = 1201;
                int[][] columns = com.hex.kagune.control.village.DryLand.columns(minX, minZ, size[0], size[1], every);
                java.util.TreeSet<Integer> xs = new java.util.TreeSet<>();
                java.util.TreeSet<Integer> zs = new java.util.TreeSet<>();
                java.util.Set<Long> seen = new java.util.HashSet<>();
                boolean inside = true;
                for (int[] c : columns) {
                    xs.add(c[0]);
                    zs.add(c[1]);
                    seen.add(((long)c[0] << 32) ^ (c[1] & 0xffffffffL));
                    inside &= c[0] >= minX - margin && c[0] <= minX + size[0] - 1 + margin && c[1] >= minZ - margin && c[1] <= minZ + size[1] - 1 + margin;
                }
                boolean close = true;
                for (java.util.TreeSet<Integer> line : List.of(xs, zs)) {
                    Integer last = null;
                    for (int at : line) {
                        close &= last == null || at - last <= every;
                        last = at;
                    }
                }
                check(inside && close && seen.size() == columns.length && columns.length == xs.size() * zs.size()
                    && xs.first() == minX - margin && xs.last() == minX + size[0] - 1 + margin && zs.first() == minZ - margin
                    && zs.last() == minZ + size[1] - 1 + margin && columns[0][0] == minX - margin && columns[0][1] == minZ - margin,
                    "every " + every + " blocks over " + size[0] + " x " + size[1] + " and 8 out round it, from a corner");
            }
        }
        // A lake 16 blocks across, or a wet biome's patch (a quart, 4 x 4), anywhere under a place or at its edge: not there. Further
        // off, it may stand.
        int minX = 100;
        int minZ = -50;
        int sizeX = 37;
        int sizeZ = 29;
        boolean lakes = true;
        boolean patches = true;
        int lakeTries = 0;
        for (int lx = minX - margin - 24; lx <= minX + sizeX + margin + 8; lx++) {
            for (int lz = minZ - margin - 24; lz <= minZ + sizeZ + margin + 8; lz++) {
                int ax = lx;
                int az = lz;
                boolean touches = ax + 15 >= minX - margin && ax <= minX + sizeX - 1 + margin && az + 15 >= minZ - margin && az <= minZ + sizeZ - 1 + margin;
                com.hex.kagune.control.village.DryLand.Ground lake = ground((x, z) -> x >= ax && x < ax + 16 && z >= az && z < az + 16, (x, z) -> false);
                lakes &= com.hex.kagune.control.village.DryLand.dry(lake, 70, minX, minZ, sizeX, sizeZ) == !touches;
                // A biome's patch lies on its grain (4 x 4, from a multiple of 4).
                int qx = Math.floorDiv(lx, 4) * 4;
                int qz = Math.floorDiv(lz, 4) * 4;
                boolean near = qx + 3 >= minX - margin && qx <= minX + sizeX - 1 + margin && qz + 3 >= minZ - margin && qz <= minZ + sizeZ - 1 + margin;
                com.hex.kagune.control.village.DryLand.Ground patch = ground((x, z) -> false, (x, z) -> x >= qx && x < qx + 4 && z >= qz && z < qz + 4);
                patches &= com.hex.kagune.control.village.DryLand.dry(patch, 70, minX, minZ, sizeX, sizeZ) == !near;
                lakeTries++;
            }
        }
        check(lakes && lakeTries > 1000, "a lake (16 across) under it or at its edge, anywhere: not there; further off: it may stand");
        check(patches, "a sea, a river, a beach or a swamp under it or at its edge (its biome, on its 4-block grain): not there");
        check(com.hex.kagune.control.village.DryLand.dry(ground((x, z) -> false, (x, z) -> false), 70, minX, minZ, sizeX, sizeZ),
            "all dry: it stands");
        // A river 4 blocks wide (its biome) through it, any way and anywhere: not there.
        boolean rivers = true;
        for (int at = minX - margin; at <= minX + sizeX - 1 + margin; at++) {
            int a = Math.floorDiv(at, 4) * 4;
            rivers &= !com.hex.kagune.control.village.DryLand.dry(ground((x, z) -> false, (x, z) -> x >= a && x < a + 4), 70, minX, minZ, sizeX, sizeZ);
        }
        for (int at = minZ - margin; at <= minZ + sizeZ - 1 + margin; at++) {
            int a = Math.floorDiv(at, 4) * 4;
            rivers &= !com.hex.kagune.control.village.DryLand.dry(ground((x, z) -> false, (x, z) -> z >= a && z < a + 4), 70, minX, minZ, sizeX, sizeZ);
        }
        check(rivers, "a river through it, any way: not there");
        // The sea at once: its first column is wet, nothing more is looked at.
        int[] looks = {0};
        com.hex.kagune.control.village.DryLand.Ground sea = new com.hex.kagune.control.village.DryLand.Ground() {
            @Override
            public int surface(int x, int z) {
                looks[0]++;
                return 63;
            }

            @Override
            public int floor(int x, int z) {
                return 40;
            }

            @Override
            public boolean wet(int x, int y, int z) {
                looks[0]++;
                return true;
            }
        };
        check(!com.hex.kagune.control.village.DryLand.dry(sea, 63, 0, 0, 272, 272) && looks[0] == 1, "the sea is seen at its first column");

        // The game's spread (RandomSpreadStructurePlacement, linear): its legacy random is java.util.Random's own.
        long[] seeds = {0L, 1L, -1L, 42L, 1234567890123L, Long.MIN_VALUE + 7, 8675309L};
        boolean same = true;
        boolean within = true;
        for (long seed : seeds) {
            for (int rx = -6; rx <= 6; rx++) {
                for (int rz = -6; rz <= 6; rz++) {
                    int[] chunk = com.hex.kagune.control.village.DryLand.spreadChunk(seed, rx, rz, 40, 20, com.hex.kagune.control.village.DryLand.SALT);
                    long[] legacy = {((long)rx * 341873128712L + (long)rz * 132897987541L + seed + (long)com.hex.kagune.control.village.DryLand.SALT
                        ^ 0x5DEECE66DL) & ((1L << 48) - 1)};
                    int x = legacyInt(legacy, 20);
                    int z = legacyInt(legacy, 20);
                    same &= chunk[0] == rx * 40 + x && chunk[1] == rz * 40 + z;
                    within &= Math.floorDiv(chunk[0], 40) == rx && Math.floorDiv(chunk[1], 40) == rz && chunk[0] - rx * 40 < 20 && chunk[1] - rz * 40 < 20;
                }
            }
        }
        check(same, "the offers are the game's: its legacy random (seeded by the region and the salt) taken step by step");
        check(within, "each offer in its own region, its separation kept");
        // The hideout's offers: nearest the world's middle first, none too near it; chosen the same way however often it is asked.
        for (long seed : seeds) {
            int[][] offers = com.hex.kagune.control.village.DryLand.hideoutOffers(seed);
            boolean order = offers.length > 40 && offers.length <= 4 * com.hex.kagune.control.village.DryLand.REGIONS * com.hex.kagune.control.village.DryLand.REGIONS;
            long last = -1L;
            for (int[] offer : offers) {
                long d = (long)offer[0] * offer[0] + (long)offer[1] * offer[1];
                order &= d >= last && d >= (long)com.hex.kagune.control.village.DryLand.NEAREST * com.hex.kagune.control.village.DryLand.NEAREST;
                int[] again = com.hex.kagune.control.village.DryLand.spreadChunk(seed, Math.floorDiv(offer[0], com.hex.kagune.control.village.DryLand.SPACING),
                    Math.floorDiv(offer[1], com.hex.kagune.control.village.DryLand.SPACING), com.hex.kagune.control.village.DryLand.SPACING,
                    com.hex.kagune.control.village.DryLand.SEPARATION, com.hex.kagune.control.village.DryLand.SALT);
                order &= again[0] == offer[0] && again[1] == offer[1];
                last = d;
            }
            check(order && java.util.Arrays.deepEquals(offers, com.hex.kagune.control.village.DryLand.hideoutOffers(seed)),
                "seed " + seed + ": the hideout's offers are the game's own start chunks, nearest first, none too near the middle");
        }
        int[][] offers = {{40, 3}, {41, 50}, {-60, 2}, {7, -80}, {90, 90}};
        java.util.Set<Integer> wet = java.util.Set.of(0);
        java.util.Set<Integer> steep = java.util.Set.of(1, 3);
        java.util.Set<Integer> badBiome = java.util.Set.of(2);
        java.util.Set<Integer> pond = new java.util.HashSet<>();
        com.hex.kagune.control.village.DryLand.Judge judge = new com.hex.kagune.control.village.DryLand.Judge() {
            int index(int x, int z) {
                for (int i = 0; i < offers.length; i++) {
                    if (offers[i][0] == x && offers[i][1] == z) {
                        return i;
                    }
                }
                return -1;
            }

            @Override
            public int[] grid(int x, int z) {
                int i = index(x, z);
                if (wet.contains(i)) {
                    return null;
                }
                int[] floor = new int[25];
                java.util.Arrays.fill(floor, 70 + i);
                if (steep.contains(i)) {
                    floor[3] = 70 + i + com.hex.kagune.control.village.VillageGround.FLAT + 5;
                }
                return floor;
            }

            @Override
            public boolean biome(int x, int z, int y) {
                return !badBiome.contains(index(x, z));
            }

            @Override
            public boolean dry(int x, int z, int y) {
                return !pond.contains(index(x, z));
            }

            @Override
            public int[] wetness(int x, int z, int most) {
                int i = index(x, z);
                // Offer 0's middle is under water; offer 2 may not stand in its biome; the rest: the further out, the less water.
                return i < 0 || i == 0 || badBiome.contains(i) ? null : new int[] {10 - 2 * i, 70 + i};
            }
        };
        int[] chosen = com.hex.kagune.control.village.DryLand.choose(offers, judge, com.hex.kagune.control.village.VillageGround.FLAT);
        check(chosen != null && chosen[0] == 90 && chosen[1] == 90 && chosen[2] == 74, "the first dry, fairly flat offer in a biome it may stand in: "
            + java.util.Arrays.toString(chosen));
        pond.add(4);
        chosen = com.hex.kagune.control.village.DryLand.choose(offers, judge, com.hex.kagune.control.village.VillageGround.FLAT);
        check(chosen != null && chosen[0] == 41 && chosen[1] == 50 && chosen[2] == 71, "a pond at the flat one: the first dry steep one instead");
        pond.add(1);
        pond.add(3);
        check(com.hex.kagune.control.village.DryLand.choose(offers, judge, com.hex.kagune.control.village.VillageGround.FLAT) == null,
            "no dry land among them: no hideout in the water");
        // 4.1.6 (the owner: "I cant get the aogiri hideout to spawn or the ccg building at all"): none of the near offers dry, so the
        // further ones are weighed; none of those either, so the driest near one (its middle on dry land, in a biome it may stand in).
        int[][] further = {{200, 3}, {-210, 40}};
        java.util.Set<Integer> ponds = new java.util.HashSet<>(pond);
        com.hex.kagune.control.village.DryLand.Judge wide = new com.hex.kagune.control.village.DryLand.Judge() {
            boolean far(int x) {
                return Math.abs(x) >= 200;
            }

            @Override
            public int[] grid(int x, int z) {
                return far(x) ? new int[] {80, 80, 81} : judge.grid(x, z);
            }

            @Override
            public boolean biome(int x, int z, int y) {
                return far(x) || judge.biome(x, z, y);
            }

            @Override
            public boolean dry(int x, int z, int y) {
                return far(x) ? !ponds.contains(x) : judge.dry(x, z, y);
            }

            @Override
            public int[] wetness(int x, int z, int most) {
                return far(x) ? new int[] {50, 80} : judge.wetness(x, z, most);
            }
        };
        int[] picked = com.hex.kagune.control.village.DryLand.pick(offers, further, wide, com.hex.kagune.control.village.VillageGround.FLAT);
        check(picked != null && picked[0] == 200 && picked[1] == 3 && picked[2] == 80, "no near offer dry: the first dry further one "
            + java.util.Arrays.toString(picked));
        ponds.add(200);
        ponds.add(-210);
        picked = com.hex.kagune.control.village.DryLand.pick(offers, further, wide, com.hex.kagune.control.village.VillageGround.FLAT);
        check(picked != null && picked[0] == 90 && picked[1] == 90 && picked[2] == 74,
            "nothing dry anywhere: still a hideout, at the driest near offer standing on land in its biomes " + java.util.Arrays.toString(picked));
        pond.clear();
        picked = com.hex.kagune.control.village.DryLand.pick(offers, further, wide, com.hex.kagune.control.village.VillageGround.FLAT);
        check(picked != null && picked[0] == 40 + 50 && java.util.Arrays.equals(picked, com.hex.kagune.control.village.DryLand.choose(offers, judge,
            com.hex.kagune.control.village.VillageGround.FLAT)), "a world whose near offers held a dry one keeps the same spot as before 4.1.6");
        check(com.hex.kagune.control.village.DryLand.driest(new int[][] {{40, 3}, {-60, 2}}, judge) == null,
            "an offer under water, or in a biome it may not stand in, is never the driest");
        // The wetness counted: a wet biome column 1, water over the ground the 16 biome columns' worth it stands for; it stops past most.
        com.hex.kagune.control.village.DryLand.Ground soaked = new com.hex.kagune.control.village.DryLand.Ground() {
            public int surface(int x, int z) {
                return x >= 50 ? 64 : 70;
            }

            public int floor(int x, int z) {
                return x >= 50 ? 50 : 70;
            }

            public boolean wet(int x, int y, int z) {
                return x >= 50;
            }
        };
        int dryCount = com.hex.kagune.control.village.DryLand.wetness(soaked, 70, -200, 0, 60, 60, Integer.MAX_VALUE);
        int wetAll = com.hex.kagune.control.village.DryLand.wetness(soaked, 70, 100, 0, 60, 60, Integer.MAX_VALUE);
        int biomeColumns = com.hex.kagune.control.village.DryLand.columns(100, 0, 60, 60, com.hex.kagune.control.village.DryLand.BIOME_STEP).length;
        int waterColumns = com.hex.kagune.control.village.DryLand.columns(100, 0, 60, 60, com.hex.kagune.control.village.DryLand.STEP).length;
        check(dryCount == 0 && wetAll == biomeColumns + 16 * waterColumns, "the wetness of dry ground and of the sea: " + dryCount + ", " + wetAll);
        int capped = com.hex.kagune.control.village.DryLand.wetness(soaked, 70, 100, 0, 60, 60, 5);
        check(capped == 6, "it stops counting once past the most it is asked for: " + capped);
        // Its further offers: the regions out to FAR, never a near one, nearest first; the building's also kept from the others' spots.
        for (long seed : new long[] {0L, 99L, -12345L}) {
            int[][] near = com.hex.kagune.control.village.DryLand.hideoutOffers(seed);
            int[][] far = com.hex.kagune.control.village.DryLand.hideoutFurther(seed);
            java.util.Set<String> nearSet = new java.util.HashSet<>();
            for (int[] o : near) {
                nearSet.add(o[0] + "," + o[1]);
            }
            boolean apart = far.length > near.length;
            long last = -1L;
            for (int[] o : far) {
                long d = (long)o[0] * o[0] + (long)o[1] * o[1];
                int rx = Math.floorDiv(o[0], com.hex.kagune.control.village.DryLand.SPACING);
                int rz = Math.floorDiv(o[1], com.hex.kagune.control.village.DryLand.SPACING);
                int far8 = com.hex.kagune.control.village.DryLand.FAR;
                int in4 = com.hex.kagune.control.village.DryLand.REGIONS;
                apart &= !nearSet.contains(o[0] + "," + o[1]) && d >= last && rx >= -far8 && rx < far8 && rz >= -far8 && rz < far8
                    && !(rx >= -in4 && rx < in4 && rz >= -in4 && rz < in4);
                last = d;
            }
            check(apart, "seed " + seed + ": the further offers lie beyond the near ones, nearest first (" + far.length + ")");
            int[][] ccgFar = com.hex.kagune.control.village.DryLand.ccgFurther(seed);
            boolean kept = ccgFar.length > 0;
            for (int[] o : ccgFar) {
                kept &= !com.hex.kagune.control.village.DryLand.near(seed, o[0], o[1], com.hex.kagune.control.village.DryLand.CCG_APART, 40, 20,
                    com.hex.kagune.control.village.DryLand.SALT);
            }
            check(kept, "seed " + seed + ": the building's further offers are kept from the mansion's spots too");
        }
        // The spots a place may move to, and the start chunks a jigsaw place may move by: nearest first, all within reach.
        int[][] shifts = com.hex.kagune.control.village.DryLand.shifts(5);
        check(shifts.length == 121 && shifts[0][0] == 0 && shifts[0][1] == 0 && shifts[1][0] * shifts[1][0] + shifts[1][1] * shifts[1][1] == 1
            && shifts[120][0] * shifts[120][0] + shifts[120][1] * shifts[120][1] == 50, "a jigsaw place moved a chunk at a time, nearest first");
        for (int size : new int[] {1, 37, 77, 130, 183, 263, 272}) {
            for (int corner : new int[] {-160, 0, 48, 1600}) {
                int want = corner + 8 - size / 2;
                int[] runs = com.hex.kagune.control.village.DryLand.runs(want, corner, size, 32);
                boolean ok = runs.length >= 1 && runs[0] == ControlStructuresProbe.clampMin(want, corner, size);
                for (int run : runs) {
                    ok &= com.hex.kagune.control.village.DryLand.reachable(corner, corner, run, run, run + size - 1, run + size - 1)
                        && Math.floorMod(run - runs[0], 32) == 0;
                }
                ok &= size < 272 - 32 || runs.length == 1;
                check(ok, "a " + size + "-block place's spots from a chunk at " + corner + ": the middle first, all within its reach");
            }
        }
        int[][] spots = com.hex.kagune.control.village.DryLand.spots(new int[] {10, -22, 42}, new int[] {5, -27}, 9);
        check(spots.length == 6 && spots[0][0] == 10 && spots[0][1] == 5, "the middle spot first, then the nearest");
        check(com.hex.kagune.control.village.DryLand.REACH_BACK == 128 && com.hex.kagune.control.village.DryLand.REACH_ON == 143,
            "a place reaches 8 chunks either way of its start chunk, no further");

        // Every structure: one of the expansion's dry-land types; their biomes never wet; the wet biomes listed.
        try {
            java.nio.file.Path data = java.nio.file.Path.of("src/main/resources/data/hexkagune");
            String wetTag = new String(java.nio.file.Files.readAllBytes(data.resolve("tags/worldgen/biome/wet.json")));
            check(wetTag.contains("\"#minecraft:is_ocean\"") && wetTag.contains("\"#minecraft:is_river\"") && wetTag.contains("\"#minecraft:is_beach\"")
                && wetTag.contains("\"minecraft:swamp\"") && wetTag.contains("\"minecraft:mangrove_swamp\""), "the wet biomes: seas, rivers, beaches, swamps");
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(data.resolve("worldgen/structure"))) {
                for (java.nio.file.Path file : files.sorted().toList()) {
                    String json = new String(java.nio.file.Files.readAllBytes(file));
                    String name = file.getFileName().toString();
                    check(json.contains("\"type\": \"hexkagune:dry_place\"") || json.contains("\"type\": \"hexkagune:site_")
                        || name.equals("small_city.json") && json.contains("\"type\": \"hexkagune:small_city\""), name + ": built only on dry land");
                }
            }
            for (String place : List.of("anteiku", "library", "reaper_mansion", "circus", "gas_hideout")) {
                String json = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure/" + place + ".json")));
                check(json.contains("\"type\": \"hexkagune:dry_place\"") && json.contains("\"start_pool\": \"hexkagune:" + place + "/start\""),
                    place + ": moved near its start onto dry land");
                String pool = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/template_pool/" + place + "/start.json")));
                check(pool.split("\"element_type\"").length == 2 && pool.contains("minecraft:single_pool_element"),
                    place + ": one template (its pieces come out the same each time they are looked at)");
            }
            // 4.1.9 (the owner: "it should be able to spawn literally in any biome ... It should also be fully compatible with Biomes of
            // Plenty"): no place has a list of biomes any more (a mod's biome missing from it was no place at all: in a world of Regions
            // Unexplored's biomes the hideout, the CCG building and Ken's tower were nowhere, /locate and all). Every structure's biomes are
            // #hexkagune:overworld (only the overworld's: a set runs only where its biomes can be made), and the land is judged in code.
            String overworld = new String(java.nio.file.Files.readAllBytes(data.resolve("tags/worldgen/biome/overworld.json")));
            check(overworld.contains("\"#minecraft:is_overworld\"") && overworld.split("\"#?[a-z_]+:[a-z_/]+\"").length == 2,
                "#hexkagune:overworld is the overworld's biomes and nothing else");
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(data.resolve("worldgen/structure"))) {
                for (java.nio.file.Path file : files.sorted().toList()) {
                    String json = new String(java.nio.file.Files.readAllBytes(file));
                    check(json.contains("\"biomes\": \"#hexkagune:overworld\""), file.getFileName() + ": any biome of the overworld");
                }
            }
            try (java.util.stream.Stream<java.nio.file.Path> tags = java.nio.file.Files.walk(data.resolve("tags/worldgen/biome"))) {
                java.util.List<String> left = tags.filter(java.nio.file.Files::isRegularFile).map(f -> data.resolve("tags/worldgen/biome").relativize(f).toString())
                    .sorted().toList();
                check(left.equals(List.of("overworld.json", "plains.json", "steep.json", "wet.json")),
                    "no place's own biome list is left (4.1.42: the steep ones kept out; 4.1.72: the hotel's plains): " + left);
            }
            check(wetTag.contains("\"#forge:is_swamp\"") && wetTag.contains("\"#forge:is_water\"") && wetTag.contains("\"#c:is_swamp\"")
                && wetTag.contains("\"biomesoplenty:marsh\"") && wetTag.split("\"required\": false").length > 10,
                "a mod's swamps and waters are wet too (Forge's and the common tags, Biomes O' Plenty's own), each only if it is there");
            String structures = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java")));
            check(structures.contains("TYPES.register(\"dry_place\", () -> DRY_PLACE);")
                && structures.contains("&& DryLand.dry(ground, middle, at[0], at[1], sizeX, sizeZ)")
                && structures.contains("&& DryLand.dry(ground, at.m_123342_(), minX, minZ, maxX - minX + 1, maxZ - minZ + 1)")
                && structures.contains("DryLand.reachable(home.m_45604_(), home.m_45605_(), minX, minZ, maxX, maxZ)")
                && !structures.contains("WET_EDGE") && structures.contains("m_203656_(WET)")
                && structures.contains("ResourceLocation.m_214293_(\"hexkagune\", \"wet\")"),
                "every place checks the dry land under it and round it, wet biomes and water both, within its start chunk's reach");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
    }

    /** The game's legacy random (LegacyRandomSource: next(bits), nextInt(bound) by BitRandomSource), written out on its own. */
    private static int legacyInt(long[] seed, int bound) {
        int r;
        int v;
        do {
            seed[0] = seed[0] * 0x5DEECE66DL + 0xBL & ((1L << 48) - 1);
            r = (int)(seed[0] >> 17);
            v = r % bound;
        } while (r - v + (bound - 1) < 0);
        return v;
    }

    /** A world's ground for {@link #dryLand}: water over the ground where {@code lake}, a wet biome where {@code wet}. */
    private static com.hex.kagune.control.village.DryLand.Ground ground(java.util.function.BiPredicate<Integer, Integer> lake,
                                                                        java.util.function.BiPredicate<Integer, Integer> wet) {
        return new com.hex.kagune.control.village.DryLand.Ground() {
            @Override
            public int surface(int x, int z) {
                return lake.test(x, z) ? 72 : 70;
            }

            @Override
            public int floor(int x, int z) {
                return lake.test(x, z) ? 66 : 70;
            }

            @Override
            public boolean wet(int x, int y, int z) {
                return wet.test(x, z);
            }
        };
    }

    /** ControlStructures.Site.clampMin's sum, written out (the class itself needs the game). */
    private static final class ControlStructuresProbe {
        static int clampMin(int want, int corner, int size) {
            int lo = corner - 128;
            int hi = corner + 143 - size + 1;
            return hi < lo ? lo : Math.max(lo, Math.min(hi, want));
        }
    }

    /**
     * 4.1.4 (the owner: "CCG building, tons of ccg agents guard and patrol the area, sit in their offices"; "Also I forgot to send you the
     * CCG building, this is it, also like the others only 1 of them spawns"): the building as a place of its own, one in a world, on dry
     * land away from the mansion and the towns; its people at their posts, on their patrol and at their desks.
     */
    @SuppressWarnings("unchecked")
    /**
     * 4.1.9 (the owner: "Cant find CCG building, Aogiri Hideout or Kaneki's building AT ALL not even with locate"; "it should be able to
     * spawn literally in any biome as long as there's only one of each structure aside from the villages, of course"): the hideout, the
     * CCG building, Anteiku, the library and Ken's tower are the world's one each, in any land biome, a mod's too.
     */
    static void onePerWorld() {
        com.hex.kagune.control.village.DryLand.Probe everywhere = new com.hex.kagune.control.village.DryLand.Probe() {
            @Override
            public boolean land(int chunkX, int chunkZ) {
                return true;
            }

            @Override
            public int[] placed(int chunkX, int chunkZ) {
                return new int[] {chunkX + 1, chunkZ};
            }
        };
        int[][] near = {{40, 3}, {-50, 7}};
        int[][] further = {{200, 9}};
        int[] first = com.hex.kagune.control.village.DryLand.firstPlaced(near, further, everywhere, at -> true, 24);
        check(first != null && first[0] == 40 && first[1] == 3 && first[2] == 41 && first[3] == 3, "the first offer that stands, with where its middle stands");
        int[] skipped = com.hex.kagune.control.village.DryLand.firstPlaced(near, further, everywhere, at -> at[0] != 41, 24);
        check(skipped != null && skipped[0] == -50, "an offer that would stand too near another place is passed over");
        int[] further1 = com.hex.kagune.control.village.DryLand.firstPlaced(near, further, everywhere, at -> at[0] > 100, 24);
        check(further1 != null && further1[0] == 200, "the further offers once the near ones are all passed over");
        int[] laid = {0};
        com.hex.kagune.control.village.DryLand.Probe sea = new com.hex.kagune.control.village.DryLand.Probe() {
            @Override
            public boolean land(int chunkX, int chunkZ) {
                return chunkX != -50;
            }

            @Override
            public int[] placed(int chunkX, int chunkZ) {
                laid[0]++;
                return null;
            }
        };
        check(com.hex.kagune.control.village.DryLand.firstPlaced(near, further, sea, at -> true, 24) == null && laid[0] == 2,
            "an offer in the sea is never laid out; none that stands: none at all");
        laid[0] = 0;
        int[][] many = new int[100][];
        for (int i = 0; i < many.length; i++) {
            many[i] = new int[] {i * 40, 0};
        }
        check(com.hex.kagune.control.village.DryLand.firstPlaced(many, new int[0][], sea, at -> true, com.hex.kagune.control.village.DryLand.PLACE_TRIES) == null
            && laid[0] == com.hex.kagune.control.village.DryLand.PLACE_TRIES, "no more than PLACE_TRIES offers laid out in full (a world of sea gives up quickly)");
        check(com.hex.kagune.control.village.DryLand.apart(0, 0, List.of(new int[] {12, 0}), 12)
            && !com.hex.kagune.control.village.DryLand.apart(0, 0, List.of(new int[] {11, 3}), 12)
            && com.hex.kagune.control.village.DryLand.apart(5, 5, List.of(), 12), "apart: at least that many chunks from each");
        // Their numbers, each its own salt, the same as their sets'.
        java.util.Set<Integer> salts = new java.util.HashSet<>(List.of(com.hex.kagune.control.village.DryLand.SALT, com.hex.kagune.control.village.DryLand.CCG_SALT,
            com.hex.kagune.control.village.DryLand.HOTEL_SALT));
        check(List.of(com.hex.kagune.control.village.DryLand.PLACES).equals(List.of("anteiku", "library", "reaper_mansion", "circus",
            "gas_hideout")), "the six jigsaw places, in the order chosen (4.1.24: the White Reaper's mansion; 4.1.48: the circus tent; 4.1.54: the "
            + "Gas Masks' hideout last)");
        // The offers are read from the set as the world has it (a mod may change every set's spacing): with the file's numbers they are
        // exactly the hideout's and the CCG building's as before; with other numbers, that spread's own offers.
        for (long seed : new long[] {0L, 42L, -7_000_000_000L}) {
            com.hex.kagune.control.village.DryLand.Spread hideoutFile = com.hex.kagune.control.village.DryLand.fixed(seed, 40, 20,
                com.hex.kagune.control.village.DryLand.SALT);
            check(java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.offers(hideoutFile, 0, com.hex.kagune.control.village.DryLand.REGIONS,
                new com.hex.kagune.control.village.DryLand.Spread[0], 0), oldOffers(seed, com.hex.kagune.control.village.DryLand.SALT, 0, 4, false)),
                "the hideout's offers from its set's numbers are the same as 4.1.8's (seed " + seed + ")");
            check(java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.ccgOffers(seed), oldOffers(seed, com.hex.kagune.control.village.DryLand.CCG_SALT, 0, 4, true))
                && java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.hideoutFurther(seed), oldOffers(seed, com.hex.kagune.control.village.DryLand.SALT, 4, 8, false)),
                "the CCG building's and the hideout's further offers are the same as 4.1.8's (seed " + seed + ")");
            com.hex.kagune.control.village.DryLand.Spread[] keep = {hideoutFile};
            check(java.util.Arrays.deepEquals(java.util.Arrays.stream(com.hex.kagune.control.village.DryLand.offers(com.hex.kagune.control.village.DryLand.fixed(seed, 40, 20,
                    com.hex.kagune.control.village.DryLand.CCG_SALT), com.hex.kagune.control.village.DryLand.REGIONS, com.hex.kagune.control.village.DryLand.FAR,
                    keep, com.hex.kagune.control.village.DryLand.CCG_APART)).toArray(int[][]::new),
                    com.hex.kagune.control.village.DryLand.ccgFurther(seed)),
                "the CCG building's further offers from its set's numbers: clear of the hideout's (seed " + seed + ")");
            check(java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.ccgFurther(seed), oldOffers(seed, com.hex.kagune.control.village.DryLand.CCG_SALT, 4, 8, true)),
                "the CCG building's further offers, against 4.1.8's own way (seed " + seed + ")");
            // Twice the spacing (a pack that spreads every set out): every offer is the start chunk its region offers, regions twice as far.
            com.hex.kagune.control.village.DryLand.Spread wide = com.hex.kagune.control.village.DryLand.fixed(seed, 80, 40,
                com.hex.kagune.control.village.DryLand.SALT);
            int[][] wideOffers = com.hex.kagune.control.village.DryLand.offers(wide, 0, com.hex.kagune.control.village.DryLand.REGIONS,
                new com.hex.kagune.control.village.DryLand.Spread[0], 0);
            boolean own = wideOffers.length > 0;
            for (int[] offer : wideOffers) {
                int[] again = wide.chunk(offer[0], offer[1]);
                own &= again[0] == offer[0] && again[1] == offer[1];
            }
            check(own, "a wider spread's offers are each the start chunk the game offers in its region (seed " + seed + ")");
        }
        try {
            java.nio.file.Path data = java.nio.file.Path.of("src/main/resources/data/hexkagune");
            for (String place : com.hex.kagune.control.village.DryLand.PLACES) {
                int salt = com.hex.kagune.control.village.DryLand.placeSalt(place);
                check(salt >= 0 && salts.add(salt), place + ": its own salt");
                String set = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure_set/" + place + ".json")));
                check(set.contains("\"minecraft:random_spread\"") && set.contains("\"spacing\": " + com.hex.kagune.control.village.DryLand.SPACING + ",")
                    && set.contains("\"separation\": " + com.hex.kagune.control.village.DryLand.SEPARATION + ",") && set.contains("\"salt\": " + salt)
                    && set.contains("\"structure\": \"hexkagune:" + place + "\"") && !set.contains("concentric") && !set.contains("\"count\"")
                    && !set.contains("frequency") && !set.contains("exclusion_zone") && !set.contains("\"spread_type\""),
                    place + ": one set of offers, the game's own numbers as DryLand's (no ring of three)");
                int[][] offers = com.hex.kagune.control.village.DryLand.placeOffers(42L, place);
                boolean sorted = offers.length > 10;
                for (int i = 1; i < offers.length; i++) {
                    long a = (long)offers[i - 1][0] * offers[i - 1][0] + (long)offers[i - 1][1] * offers[i - 1][1];
                    long b = (long)offers[i][0] * offers[i][0] + (long)offers[i][1] * offers[i][1];
                    sorted &= a <= b && b >= (long)com.hex.kagune.control.village.DryLand.NEAREST * com.hex.kagune.control.village.DryLand.NEAREST;
                }
                check(sorted && com.hex.kagune.control.village.DryLand.placeFurther(42L, place).length > offers.length, place + ": its offers nearest the middle first");
            }
            check(com.hex.kagune.control.village.DryLand.placeSalt("hotel") < 0 && com.hex.kagune.control.village.DryLand.placeSalt(null) < 0,
                "4.1.72: the hotels are not one of a kind");
            try (java.util.stream.Stream<java.nio.file.Path> sets = java.nio.file.Files.list(data.resolve("worldgen/structure_set"))) {
                for (java.nio.file.Path file : sets.sorted().toList()) {
                    String json = new String(java.nio.file.Files.readAllBytes(file));
                    check(!json.contains("concentric_rings") && !json.contains("\"count\""), file.getFileName() + ": no ring of several");
                }
            }
            String structures = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java")));
            int overrides = structures.split("public Optional<Structure.GenerationStub> m_262864_\\(Structure.GenerationContext context\\) \\{\n            return this.m_214086_\\(context\\);").length - 1;
            check(overrides == 3, "the jigsaw village, every site (4.1.72: the hotels too) and every jigsaw place leave the game's biome-list test out ("
                + overrides + ")");
            check(structures.contains("return !biome.m_203656_(WET) && !biome.m_203656_(STEEP);") && !structures.contains("return context.f_226630_().test(biome);"),
                "a place stands in any biome but a wet one (4.1.42: or a steep one)");
            // 4.1.14 (the owner: "Fix structures still not spawning at all or able to be located (still not spawning)"): no one chunk a world
            // any more: every spot of its set where it lays out on dry land may hold it, until the world makes one (PlaceClaims).
            check(structures.contains("if (Site.onPurpose(context)) {\n                // /place: where it is asked, but (4.1.43) only while the world has none yet")
                && structures.contains("return claimed(id, context, () -> this.here(context));")
                && structures.contains("return claimed(id, context, () -> {")
                && !structures.contains("if (!offered(context, placeSpread(context, id))) {")
                && structures.contains("Optional<Structure.GenerationStub> stub = this.flattest(context);"),
                "a jigsaw place stands at any spot of its set where it lays out on dry, flat land (4.1.42: the flattest), until one is built (4.1.43: /place too, never a second)");
            check(structures.contains("if (inHotel(context, x, z) || DryLand.near(hideoutSpread(context), x, z, DryLand.PLACE_APART)")
                && structures.contains("x, z, DryLand.PLACE_APART)) {") && structures.contains("if (!other.equals(id) && DryLand.near(placeSpread(context, other), x, z, DryLand.PLACE_APART)) {"),
                "kept clear of the hideout's, the CCG building's, the other places' spots and the hotels (4.1.72)");
            check(structures.contains("if (id == null) {\n                return Optional.empty();"), "a place whose name can't be read is never built many times over");
            // 4.1.14: nothing worked out once a world any more (whatever stopped the world's one chunk left the world without it).
            check(!structures.contains("chosen(context, id)") && !structures.contains("= hideout(context)") && !structures.contains("= ccg(context)")
                && !structures.contains("HIDEOUTS") && !structures.contains("BUILDINGS"), "no place hangs on one chunk worked out once a world");
            // Taken as the world makes the chunk (its heights are the chunk being made), never as /locate only asks; a taken spot turns every
            // other away; the first to be made wins, under one lock.
            // 4.1.17: asking is only /locate's own (the level's heights); anything else makes (a mod's height wrapper never builds every spot).
            // 4.1.43 (the owner: "Structures are still spawning doubles, they're not unique"; "eto spawned 2 of them"): no more guessing who asks:
            // the spot is taken as its pieces are made (the world's making and /place always make them, /locate never does), and a spot that
            // can't take it makes no pieces at all.
            int only = structures.indexOf("static Structure.GenerationStub onlyIfClaimed(String id, ChunkPos chunk, Structure.GenerationStub stub) {");
            check(!structures.contains("making(context)") && only > 0
                && structures.indexOf("if (!PlaceClaims.claim(id, x, z)) {\n                return; // not this world's one: nothing of it here", only) - only < 400
                && structures.contains("return stub.isEmpty() ? stub : Optional.of(onlyIfClaimed(id, chunk, stub.get()));")
                && structures.contains("if (taken != null && (taken[0] != chunk.f_45578_ || taken[1] != chunk.f_45579_)) {\n            return Optional.empty();"),
                "a spot is taken as the place is really built, by whatever builds it; every other spot builds nothing from then on");
            // 4.1.17 (the owner: "there's multiple structures of each, which cannot happen"): a place found standing is the world's one if
            // none is taken (a world from before the claims); a second is told in the log; every one-of-a-kind place is looked for.
            String adopt = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/PlaceClaims.java")));
            check(adopt.contains("if (taken == null) {\n                    claim(place, at.f_45578_, at.f_45579_);")
                && adopt.contains("ONE_OF_A_KIND = {\"aogiri_hideout\", \"ccg_building\", \"anteiku\", \"library\", \"reaper_mansion\", \"circus\", \"gas_hideout\"}"),
                "a one-of-a-kind place already standing is the world's one");
            // 4.1.17 (the owner: "they are spawning under the ground, which cannot happen ... always at the top max area"): no place under
            // the ground; 4.1.57 (the owner: "CLEAR ALL BLOCKS OR TREES, AND THEN PLACE IT DIRECTLY ON THE FLOOR, NOT FLYING, NOT HOVERING
            // OVER THE FLOOR"): it stands on its footprint's own floor, moved there, everything over that floor cleared before it (so never
            // under the ground either), no longer lifted onto its highest ground (a plinth over its low side). The tower starts on the
            // ground, not at y 63.
            check(structures.contains("return Optional.of(settled(best, floor - ground.surface(bestAt[0], bestAt[1]), context.f_226625_(),")
                && structures.contains("made.m_192781_(shift);") && !structures.contains("DryLand.lift("),
                "a place stands on its footprint's own floor, its site cleared over it: never under the ground, never on a plinth");
            // 4.1.68 (the owner: "Ken's cybertower is now fully removed, he is to spawn on top of Anteikou."): no tower anywhere.
            for (String gone : new String[] {"worldgen/structure/cyber_tower.json", "worldgen/structure_set/cyber_tower.json",
                "worldgen/template_pool/cyber_tower/start.json", "structures/cyber_tower.nbt"}) {
                check(!java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/" + gone)), "Ken's tower gone: " + gone);
            }
            for (String place : new String[] {"anteiku", "library", "reaper_mansion", "circus", "gas_hideout"}) {
                String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure/" + place + ".json")));
                check(json.contains("\"project_start_to_heightmap\": \"WORLD_SURFACE_WG\""), place + " starts on the ground's height");
            }
            // The lift: the highest of the footprint's ground, the edges and every LIFT_STEP between.
            com.hex.kagune.control.village.DryLand.Ground hill = new com.hex.kagune.control.village.DryLand.Ground() {
                @Override
                public int surface(int x, int z) {
                    return 64 + Math.max(0, x) / 2 + (z == 37 ? 30 : 0);
                }

                @Override
                public int floor(int x, int z) {
                    return this.surface(x, z);
                }

                @Override
                public boolean wet(int x, int y, int z) {
                    return false;
                }
            };
            check(com.hex.kagune.control.village.DryLand.lift(hill, 0, 0, -20, -20, 20, 20) == 10, "lifted onto the hill's top across its footprint");
            check(com.hex.kagune.control.village.DryLand.lift(hill, 20, 0, -20, -20, 20, 20) == 0, "already at the top: not lifted");
            check(com.hex.kagune.control.village.DryLand.lift(hill, 0, 0, -20, 30, 0, 37) == 30, "a peak at its far edge is found");
            String claims = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/PlaceClaims.java")));
            check(claims.contains("public static void load(LevelEvent.Load event) {") && claims.contains("level.m_8895_().m_164861_(Data::load,")
                && claims.contains("public static void chunk(ChunkEvent.Load event) {") && claims.contains("!ControlStructures.standsIn(level, chunk, id)")
                && claims.contains("@Mod.EventBusSubscriber(modid = \"hexkagune\")"),
                "the taken spots are kept with the world, ready before its ground is made; a taken spot whose chunk holds nothing is given up");
            com.hex.kagune.control.village.TakenPlaces taken = new com.hex.kagune.control.village.TakenPlaces();
            check(taken.at("aogiri_hideout") == null && taken.take("aogiri_hideout", 40, -12) && !taken.take("aogiri_hideout", 80, 3)
                && taken.take("aogiri_hideout", 40, -12) && taken.take("ccg_building", 80, 3) && "ccg_building".equals(taken.whose(80, 3)),
                "the first spot made takes a place; any other is refused; the same spot again is still its own; each place its own");
            check(!taken.release("aogiri_hideout", 80, 3) && taken.release("aogiri_hideout", 40, -12) && taken.take("aogiri_hideout", 80, 3),
                "a spot given up lets the next made take it (and only its own spot gives it up)");
            com.hex.kagune.control.village.TakenPlaces back = com.hex.kagune.control.village.TakenPlaces.read(taken.write());
            check(java.util.Arrays.equals(back.at("aogiri_hideout"), new int[] {80, 3}) && java.util.Arrays.equals(back.at("ccg_building"), new int[] {80, 3})
                && com.hex.kagune.control.village.TakenPlaces.read("anteiku:1,x;library:-5,7;junk;").at("anteiku") == null
                && java.util.Arrays.equals(com.hex.kagune.control.village.TakenPlaces.read("anteiku:1,x;library:-5,7;junk;").at("library"), new int[] {-5, 7})
                && com.hex.kagune.control.village.TakenPlaces.read(null).at("library") == null, "kept and read back as saved; a broken entry is as if not built");
            String commands = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/ControlCommands.java")));
            // (Read in the source: ControlStructures registers the game's own registries as it loads, and there is no game here.)
            check(commands.contains("Commands.m_82127_(\"places\")") && commands.contains("ControlStructures.report()")
                && structures.contains("ONE_OF_A_KIND = java.util.List.of(\"aogiri_hideout\", \"ccg_building\", \"anteiku\", \"library\", \"reaper_mansion\",\n        \"circus\", \"gas_hideout\");"),
                "/kagune places lists the eight places (4.1.54: the Gas Masks' hideout)");
            // 4.1.13: from a player it makes each place's start chunk and looks in it: built there, or stopped (one chunk a world: anything that
            // stops it there leaves the world without it).
            check(commands.contains("ControlStructures.report(player.m_284548_())") && structures.contains("if (standsIn(level, level.m_6325_(at[0], at[1]), id)) {")
                && structures.contains("found != null && found.m_73603_()") && structures.contains("PlaceClaims.release(id, at[0], at[1]);"),
                "/kagune places says where each place was built and looks in its chunk; one stopped there is given up for the next spot");
        } catch (java.io.IOException failure) {
            check(false, "read the sets: " + failure);
        }
    }

    /** 4.1.8's DryLand.offers and near, kept here as they were, to hold 4.1.9's (read from a Spread) to the same offers. */
    private static int[][] oldOffers(long seed, int salt, int inner, int outer, boolean apart) {
        java.util.List<int[]> offers = new java.util.ArrayList<>();
        for (int rx = -outer; rx < outer; rx++) {
            for (int rz = -outer; rz < outer; rz++) {
                if (rx >= -inner && rx < inner && rz >= -inner && rz < inner) {
                    continue;
                }
                int[] chunk = com.hex.kagune.control.village.DryLand.spreadChunk(seed, rx, rz, 40, 20, salt);
                if ((long)chunk[0] * chunk[0] + (long)chunk[1] * chunk[1] < 32L * 32L) {
                    continue;
                }
                if (apart && oldNear(seed, chunk[0], chunk[1], 12, 40, 20, com.hex.kagune.control.village.DryLand.SALT)) {
                    continue;
                }
                offers.add(chunk);
            }
        }
        offers.sort(java.util.Comparator.<int[]>comparingLong(c -> (long)c[0] * c[0] + (long)c[1] * c[1]).thenComparingInt(c -> c[0]).thenComparingInt(c -> c[1]));
        return offers.toArray(new int[0][]);
    }

    private static String read(String path) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)));
        } catch (java.io.IOException e) {
            return "";
        }
    }

    private static boolean oldNear(long seed, int x, int z, int apart, int spacing, int separation, int salt) {
        int rx = Math.floorDiv(x, spacing);
        int rz = Math.floorDiv(z, spacing);
        int reach = apart / spacing + 1;
        for (int i = rx - reach; i <= rx + reach; i++) {
            for (int j = rz - reach; j <= rz + reach; j++) {
                int[] other = com.hex.kagune.control.village.DryLand.spreadChunk(seed, i, j, spacing, separation, salt);
                long dx = other[0] - x;
                long dz = other[1] - z;
                if (dx * dx + dz * dz < (long)apart * apart) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 4.1.35: the owner's Small City, cut into four 272 x 272 quarters on one ground, every tile of each on disk. */
    /**
     * 4.1.39: the city's ground worked out once per city (its four quarters and every place asking inCity got the same 740 heights each
     * time); the humans a villager brings keep about its village (a homeless random walk took them off for good while its young brought more).
     */
    /** 4.1.42: made-up land: heights by column, and which columns are sea or steep. */
    record Land(java.util.function.IntBinaryOperator height, java.util.function.IntBinaryOperator water,
                java.util.function.BiPredicate<Integer, Integer> sea, java.util.function.BiPredicate<Integer, Integer> steep)
        implements com.hex.kagune.control.village.DryLand.Ground {
        @Override
        public int surface(int x, int z) {
            return Math.max(this.height.applyAsInt(x, z), this.water.applyAsInt(x, z));
        }

        @Override
        public int floor(int x, int z) {
            return this.height.applyAsInt(x, z);
        }

        @Override
        public boolean wet(int x, int y, int z) {
            return this.sea.test(x, z);
        }

        @Override
        public boolean sea(int x, int y, int z) {
            return this.sea.test(x, z);
        }

        @Override
        public boolean steep(int x, int y, int z) {
            return this.steep.test(x, z);
        }

        static Land of(java.util.function.IntBinaryOperator height) {
            return new Land(height, (x, z) -> Integer.MIN_VALUE, (x, z) -> false, (x, z) -> false);
        }
    }

    /**
     * 4.1.42 (the owner: "How does this generation pass your bug test, bro?"; "The structures are spawning multiple times ... And they're all
     * fucking flying"): the city's land judged on made-up land (a valley the old samples stepped over, a hillside, the sea, a steep biome),
     * the foundation's column on made-up columns (a floor over air filled down to the ground, a floor on the ground left), places kept apart,
     * and the wiring: /place alone builds outside the world's one, every structure of ours on a foundation.
     */
    /**
     * 4.1.57 (the owner: "FIX THE FUCKING STRUCTURE SPAWNING PLACEMENT, i asked multiple times to fix it, CLEAR THE FUCKING AREA OF SPAWN,
     * CLEAR ALL BLOCKS OR TREES, AND THEN PLACE IT DIRECTLY ON THE FLOOR, NOT FLYING, NOT HOVERING OVER THE FLOOR, I SAID ON IT."): a place
     * stands on its footprint's own floor (the middle of its ground, never the highest: no plinth); its clearing's tiles take every block
     * over that floor across the footprint, and over the bank's top round it, never the floor itself, never past the start's reach; the bank
     * round it is flat by the place, then a block a block; every place is built after the trees (the last step) and its clearing laid before
     * its own pieces; the clearing template is a cube of air.
     */
    static void sitePrep() {
        int clear = com.hex.kagune.control.village.SitePrep.CLEAR;
        java.util.Random random = new java.util.Random(4157);
        boolean covered = true;
        boolean within = true;
        boolean few = true;
        for (int trial = 0; trial < 400; trial++) {
            int minX = random.nextInt(400) - 200;
            int minZ = random.nextInt(400) - 200;
            int maxX = minX + random.nextInt(trial < 50 ? 20 : 120);
            int maxZ = minZ + random.nextInt(trial < 50 ? 20 : 120);
            int fromY = 40 + random.nextInt(40);
            int topY = fromY + random.nextInt(70);
            java.util.List<com.hex.kagune.control.village.SitePrep.Tile> tiles = com.hex.kagune.control.village.SitePrep.tiles(minX, minZ, maxX, maxZ,
                fromY, topY);
            for (com.hex.kagune.control.village.SitePrep.Tile t : tiles) {
                within &= t.x() >= minX && t.z() >= minZ && t.y() >= fromY;
            }
            int across = ((maxX - minX) / clear + 2) * ((maxZ - minZ) / clear + 2) * ((topY - fromY) / clear + 2);
            few &= tiles.size() <= across;
            for (int k = 0; k < 60; k++) {
                int x = minX + random.nextInt(maxX - minX + 1);
                int z = minZ + random.nextInt(maxZ - minZ + 1);
                int y = fromY + random.nextInt(topY - fromY + 1);
                boolean in = false;
                for (com.hex.kagune.control.village.SitePrep.Tile t : tiles) {
                    in |= x >= t.x() && x < t.x() + clear && z >= t.z() && z < t.z() + clear && y >= t.y() && y < t.y() + clear;
                }
                covered &= in;
            }
        }
        check(covered, "the clearing's tiles take every block of what they clear");
        check(within, "no tile starts before what it clears (never under a floor, never past a footprint's near edge)");
        check(few, "no more tiles than the run needs");

        // A place's clearing: its footprint from over its floor, the whole site from over its bank; never its floor; within its start's reach.
        int floor = 63;
        int top = 120;
        int minX = 1000;
        int minZ = 2000;
        int maxX = 1027;
        int maxZ = 2018;
        int margin = com.hex.kagune.control.village.SitePrep.MARGIN;
        int bank = com.hex.kagune.control.village.SitePrep.bank();
        java.util.List<com.hex.kagune.control.village.SitePrep.Tile> site = com.hex.kagune.control.village.SitePrep.clearing(minX, minZ, maxX, maxZ,
            1008, 2000, floor, top);
        java.util.function.Predicate<int[]> taken = p -> {
            for (com.hex.kagune.control.village.SitePrep.Tile t : site) {
                if (p[0] >= t.x() && p[0] < t.x() + clear && p[1] >= t.y() && p[1] < t.y() + clear && p[2] >= t.z() && p[2] < t.z() + clear) {
                    return true;
                }
            }
            return false;
        };
        boolean footprint = true;
        boolean ring = true;
        boolean keepsFloor = true;
        for (int x = minX - margin; x <= maxX + margin; x++) {
            for (int z = minZ - margin; z <= maxZ + margin; z++) {
                boolean in = x >= minX && x <= maxX && z >= minZ && z <= maxZ;
                keepsFloor &= !taken.test(new int[] {x, floor, z});
                for (int y = floor + 1; y <= top; y++) {
                    if (in) {
                        footprint &= taken.test(new int[] {x, y, z});
                    } else if (y > floor + bank) {
                        ring &= taken.test(new int[] {x, y, z});
                    }
                }
            }
        }
        check(footprint, "everything over the floor across the footprint cleared before the place: hills, trunks, leaves");
        check(ring, "everything over the bank's top round it cleared too (trees round it)");
        check(keepsFloor, "the floor itself never cleared: the place stands on the ground");
        java.util.List<com.hex.kagune.control.village.SitePrep.Tile> far = com.hex.kagune.control.village.SitePrep.clearing(1000, 2000, 1271, 2262,
            1008 + 64, 2000 + 64, floor, top);
        boolean reach = true;
        for (com.hex.kagune.control.village.SitePrep.Tile t : far) {
            reach &= t.x() >= 1072 - com.hex.kagune.control.village.SitePrep.REACH_BACK && t.x() + clear - 1 <= 1072 + com.hex.kagune.control.village.SitePrep.REACH_ON
                && t.z() >= 2064 - com.hex.kagune.control.village.SitePrep.REACH_BACK && t.z() + clear - 1 <= 2064 + com.hex.kagune.control.village.SitePrep.REACH_ON;
        }
        check(reach && !far.isEmpty(), "a big place's clearing never reaches past what its start chunk places (the hideout's)");

        // The floor: the middle of the footprint's ground, never its highest (no plinth over the low side) nor its lowest.
        com.hex.kagune.control.village.DryLand.Ground slope = Land.of((x, z) -> 64 + (x - minX) / 3);
        int settled = com.hex.kagune.control.village.SitePrep.floor(slope, minX, minZ, maxX, maxZ);
        int highest = com.hex.kagune.control.village.SitePrep.highest(slope, minX, minZ, maxX, maxZ);
        check(settled >= 67 && settled <= 70 && highest == 73, "on a slope from 64 to 73 it stands at its middle (" + settled
            + "), under its highest ground (" + highest + "): no plinth");
        check(com.hex.kagune.control.village.SitePrep.floor(Land.of((x, z) -> 70), minX, minZ, maxX, maxZ) == 70, "on flat land, on it");
        check(com.hex.kagune.control.village.SitePrep.samples(0, 0, 271, 262).length <= 30 * 30, "a big footprint's samples kept few");

        // The bank: flat by the place, then a block a block, to the margin's edge; the fill and the apron.
        int flat = com.hex.kagune.control.village.SitePrep.FLAT;
        boolean banked = true;
        for (int d = 0; d <= margin; d++) {
            int rise = com.hex.kagune.control.village.SitePrep.rise(d);
            banked &= rise == (d <= flat ? 0 : d - flat) && com.hex.kagune.control.village.SitePrep.cut(floor, d) == floor + 1 + rise
                && com.hex.kagune.control.village.SitePrep.fill(floor, d) == floor - rise;
        }
        check(banked && bank == margin - flat, "flat by the place, then a block up or down a block out: never a cliff");
        check(java.util.Arrays.equals(com.hex.kagune.control.village.SitePrep.apron(floor, 0, floor - 5, 24), new int[] {floor - 4, floor}),
            "a yard over lower ground filled up to the floor");
        check(com.hex.kagune.control.village.SitePrep.apron(floor, 0, floor, 24) == null, "ground at the floor: nothing to fill");
        check(java.util.Arrays.equals(com.hex.kagune.control.village.SitePrep.apron(floor, 5, floor - 9, 24), new int[] {floor - 8, floor - 3}),
            "the bank built up to its own height out there");
        check(com.hex.kagune.control.village.SitePrep.apron(floor, 0, floor - 30, 24) == null, "a ravine left as it is");
        boolean grounded = true;
        for (com.hex.kagune.control.village.SitePrep.Tile t : site) {
            int delta = com.hex.kagune.control.village.SitePrep.groundDelta(floor, t.y());
            grounded &= delta <= 0 && t.y() + delta == floor + 1;
        }
        check(grounded, "every clearing tile's ground the place's floor (the game's beard carves the site down to it, never builds a rim up)");
        check(com.hex.kagune.control.village.SitePrep.distance(minX - 3, minZ + 4, minX, minZ, maxX, maxZ) == 3
            && com.hex.kagune.control.village.SitePrep.distance(minX + 3, minZ + 4, minX, minZ, maxX, maxZ) == 0
            && com.hex.kagune.control.village.SitePrep.distance(maxX + 2, maxZ + 5, minX, minZ, maxX, maxZ) == 5, "how far out of the footprint");

        try {
            String code = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java"));
            int stub = code.indexOf("Structure.GenerationStub stub(Structure.GenerationContext context, int turns");
            check(stub > 0 && code.indexOf("clearing(templates,", stub) > 0 && code.indexOf("clearing(templates,", stub)
                < code.indexOf("for (int i = 0; i < site.across(); i++)", stub), "the CCG building's and the hideout's site cleared before their tiles");
            int settledAt = code.indexOf("static Structure.GenerationStub settled(");
            check(settledAt > 0 && code.indexOf("clearing(templates,", settledAt) < code.indexOf("for (StructurePiece piece : made.m_192780_()", settledAt),
                "every other place's site cleared before its pieces");
            check(!code.contains("DryLand.lift(") && code.contains("SitePrep.floor(ground,"), "no place lifted onto its highest ground any more");
            check(code.contains("new PoolElementStructurePiece(templates, element, at, com.hex.kagune.control.village.SitePrep.groundDelta(floor, tile.y()),")
                && code.contains("pool.m_72647_() > 0"), "the clearing's tiles laid with their ground at the floor, and told apart by it");
            check(code.split("apron\\(level, box, pieces\\)", -1).length == 3, "every place graded round after it is built");
            int apron = code.indexOf("static void apron(WorldGenLevel level, BoundingBox chunk, PiecesContainer pieces) {");
            check(apron > 0 && code.indexOf("if (inside(own, x, z)) {\n                    // Under the place itself only its foundation fills", apron) > 0,
                "the grading never fills under the place itself (its pool, its stairwell kept)");
            for (String name : new String[] {"anteiku", "aogiri_hideout", "ccg_building", "circus", "gas_hideout", "library",
                "reaper_mansion"}) {
                String json = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure/" + name + ".json"));
                check(json.contains("\"step\": \"top_layer_modification\""), name + ": built after the trees (the last step), so none grows through it");
            }
            byte[] raw;
            try (java.util.zip.GZIPInputStream in = new java.util.zip.GZIPInputStream(java.nio.file.Files.newInputStream(
                java.nio.file.Path.of("src/main/resources/data/hexkagune/structures/clearing.nbt")))) {
                raw = in.readAllBytes();
            }
            String text = new String(raw, java.nio.charset.StandardCharsets.ISO_8859_1);
            int names = text.split("Name", -1).length - 1;
            int positions = text.split("\u0003pos", -1).length - 1;
            check(names == 1 && text.contains("minecraft:air") && positions == clear * clear * clear,
                "the clearing template a cube of air " + clear + " on a side (" + names + " block, " + positions + " positions)");
        } catch (java.io.IOException failure) {
            check(false, "the places' sources: " + failure);
        }
        // 4.1.57 (the owner: "Be careful with the glass placement on the structures, the CCG building has all glass pannels unconnected and
        // not attached to eachother!"): every pane and bar in every template joined to what it touches, as the game joins them
        // (tools/join_panes.py, one rule for the fix and this check).
        try {
            Process check = new ProcessBuilder("python3", "tools/join_panes.py", "--check").redirectErrorStream(true).start();
            String said = new String(check.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int code = check.waitFor();
            check(code == 0 && said.contains("ccg_building: 3134 panes and bars, 0 to join"),
                "every pane in every template joined to its neighbours, the CCG building's 3134 too: " + said.replace('\n', ' '));
        } catch (java.io.IOException | InterruptedException failure) {
            check(false, "the panes' check could not run: " + failure);
        }
    }

    static void worldgen() {
        int minX = 1000;
        int minZ = -2000;
        com.hex.kagune.control.village.DryLand.Ground flat = Land.of((x, z) -> 70);
        // 4.1.72: the Small City's own judge (CityPlan) is gone with it; a hotel's land is judged as the hideout's (DryLand.land over its
        // 191 x 196, 9 x 9 levels, ControlStructures.HIDEOUT_FLAT 14, DryLand.DEEP 8: ControlStructures.Site.hotelAt).
        int hx = 191;
        int hz = 196;
        int deep = com.hex.kagune.control.village.DryLand.DEEP;
        check(deep == 8, "a big place's water no deeper than 8 (a river, a swamp's pools), moved to DryLand with the city gone: " + deep);
        check(com.hex.kagune.control.village.DryLand.land(flat, minX, minZ, hx, hz, 9, 14, deep) == 70, "flat land holds a hotel, on it");
        // A valley 25 deep and 40 wide across it: the 9 levels a side are 23 blocks apart, so one always falls in it.
        java.util.function.IntBinaryOperator valley = (x, z) -> x >= minX + 90 && x < minX + 130 ? 45 : 70;
        check(com.hex.kagune.control.village.DryLand.land(Land.of(valley), minX, minZ, hx, hz, 9, 14, deep) == Integer.MIN_VALUE,
            "a valley across it: no hotel there");
        check(com.hex.kagune.control.village.DryLand.land(Land.of((x, z) -> 70 + (x - minX) * 30 / hx), minX, minZ, hx, hz, 9, 14, deep)
            == Integer.MIN_VALUE, "a hillside rising 30 over it: no hotel");
        int gentle = com.hex.kagune.control.village.DryLand.land(Land.of((x, z) -> 70 + (x - minX) * 8 / hx), minX, minZ, hx, hz, 9, 14, deep);
        check(gentle >= 70 && gentle <= 78, "a gentle rise of 8 over it holds one, on its middle: " + gentle);
        check(com.hex.kagune.control.village.DryLand.land(new Land((x, z) -> 70, (x, z) -> Integer.MIN_VALUE, (x, z) -> x > minX + 170,
            (x, z) -> false), minX, minZ, hx, hz, 9, 14, deep) == Integer.MIN_VALUE, "the sea at its edge: no hotel");
        check(com.hex.kagune.control.village.DryLand.land(new Land((x, z) -> 70, (x, z) -> Integer.MIN_VALUE, (x, z) -> false,
            (x, z) -> z < minZ + 40), minX, minZ, hx, hz, 9, 14, deep) == Integer.MIN_VALUE, "a steep biome in it: no hotel");
        java.util.function.IntBinaryOperator riverBed = (x, z) -> z >= minZ + 60 && z < minZ + 72 ? 66 : 70;
        java.util.function.IntBinaryOperator riverTop = (x, z) -> z >= minZ + 60 && z < minZ + 72 ? 69 : Integer.MIN_VALUE;
        check(com.hex.kagune.control.village.DryLand.land(new Land(riverBed, riverTop, (x, z) -> false, (x, z) -> false), minX, minZ, hx, hz, 9, 14, deep)
            == 70, "a river 3 deep across it: a hotel over it (its terrain fit fills the bed up)");
        check(com.hex.kagune.control.village.DryLand.land(new Land((x, z) -> 50, (x, z) -> 64, (x, z) -> false, (x, z) -> false), minX, minZ, hx, hz,
            9, 14, deep) == Integer.MIN_VALUE, "a lake 14 deep under it: no hotel");
        check(com.hex.kagune.control.village.DryLand.roughness(flat, 0, 0, 40, 40, 6) == 0
            && com.hex.kagune.control.village.DryLand.roughness(Land.of((x, z) -> 60 + x), 0, 0, 40, 40, 6) == 39,
            "a place's ground: its rise and fall over its footprint, edges and all");
        // 4.1.44 (the owner: "Im 99% sure the Aogiri place IS not spawning at all"): the hideout's ground (272 x 263) judged as the city's:
        // a stream through it and a rise of 12 hold it (its foundation and terrain fit meet them); a hillside of 30 or a lake don't.
        java.util.function.IntBinaryOperator rolling = (x, z) -> 66 + (x - minX) * 12 / 272;
        com.hex.kagune.control.village.DryLand.Ground stream = new Land(rolling, (x, z) -> z >= minZ + 100 && z < minZ + 104 ? 70 + 3 : Integer.MIN_VALUE,
            (x, z) -> false, (x, z) -> false);
        int hide = com.hex.kagune.control.village.DryLand.land(stream, minX, minZ, 272, 263, 9, 14, 8);
        check(hide >= 66 && hide <= 78, "the hideout on rolling land with a stream through it: built, on its middle (" + hide + ")");
        check(com.hex.kagune.control.village.DryLand.land(Land.of((x, z) -> 66 + (x - minX) * 30 / 272), minX, minZ, 272, 263, 9, 14, 8) == Integer.MIN_VALUE,
            "a hillside of 30 under it: no hideout there");
        check(com.hex.kagune.control.village.DryLand.land(new Land((x, z) -> 50, (x, z) -> 63, (x, z) -> false, (x, z) -> false), minX, minZ, 272, 263, 9, 14, 8)
            == Integer.MIN_VALUE, "a lake under it: no hideout there");
        // The foundation's column: 2 solid, 1 a plant, 0 air.
        java.util.function.IntUnaryOperator floating = y -> y == 80 ? 2 : y <= 60 ? 2 : y == 61 ? 1 : 0;
        int[] fill = com.hex.kagune.control.village.DryLand.footing(floating, 80, 1, 16);
        check(fill != null && fill[0] == 60 && fill[1] == 80, "a floor over air: filled from the ground under it (through a plant) up to it");
        check(com.hex.kagune.control.village.DryLand.footing(y -> y <= 80 ? 2 : 0, 80, 1, 16) == null, "a floor on the ground: left as it is");
        check(com.hex.kagune.control.village.DryLand.footing(y -> y <= 60 ? 2 : 0, 80, 7, 16) == null, "no floor in its first layers: nothing filled");
        check(com.hex.kagune.control.village.DryLand.footing(y -> y == 80 || y <= 10 ? 2 : 0, 80, 1, 16) == null,
            "ground deeper than a foundation goes: nothing filled (no place stands on ground that steep)");
        java.util.function.IntUnaryOperator layered = y -> y == 84 ? 2 : y <= 70 ? 2 : 0;
        int[] high = com.hex.kagune.control.village.DryLand.footing(layered, 80, 7, 16);
        check(high != null && high[0] == 70 && high[1] == 84, "the city's platform found in its first layers, filled under");
        // Places apart.
        com.hex.kagune.control.village.TakenPlaces taken = com.hex.kagune.control.village.TakenPlaces.read("");
        check(taken.takeApart("anteiku", 0, 0, 32) && !taken.takeApart("library", 10, 10, 32) && taken.crowded("circus", 20, 5, 32)
            && taken.takeApart("library", 40, 0, 32) && taken.takeApart("anteiku", 0, 0, 32) && !taken.takeApart("anteiku", 90, 90, 32),
            "one-of-a-kind places never within 32 chunks of each other; each keeps its one spot");
        try {
            String structures = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java"));
            int purpose = structures.indexOf("static boolean onPurpose(Structure.GenerationContext context) {");
            check(purpose > 0 && structures.indexOf("if (!(context.f_226629_() instanceof net.minecraft.world.level.Level)) {", purpose) - purpose < 900,
                "only /place (asking on the live world) builds outside the world's one: never the world's own making, whatever a mod does to the biome test");
            check(structures.split("foundation\\(level, box, pieces\\);").length - 1 == 2, "the sites (4.1.72: the hotels among them) and the jigsaw places all on a foundation");
            String eto = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/EtoBrain.java"));
            check(eto.contains("if (slot.alive && slot.body != null && !slot.body.equals(npc.m_20148_()) && slot.seen != Long.MIN_VALUE && now - slot.seen < 400L) {"),
                "4.1.43: a second body of Eto (or Touka, Ken, Ayato, Noro) leaves at once while hers is loaded: never two of her");
            String claims = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/PlaceClaims.java"));
            check(claims.contains("data.taken.takeApart(id, x, z, APART)") && structures.contains("PlaceClaims.crowded(id, chunk.f_45578_, chunk.f_45579_)"),
                "a spot too near another place is never taken, nor pointed at by /locate");
            String steep = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/hexkagune/tags/worldgen/biome/steep.json"));
            check(steep.contains("#minecraft:is_mountain") && steep.contains("#minecraft:is_hill") && steep.contains("biomesoplenty:crag")
                && steep.contains("\"required\": false"), "mountains and hills kept out, vanilla's and the biome mods' (only if they are there)");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
    }

    static void cityFixes() {
        // 4.1.40: the old towns' maps (3, 4) still found for their people in older worlds, never built.
        check(com.hex.kagune.control.village.SiteMaps.byMap(3) != null && com.hex.kagune.control.village.SiteMaps.byMap(3).id().equals("village_1")
            && com.hex.kagune.control.village.SiteMaps.byMap(4) != null && com.hex.kagune.control.village.SiteMaps.byMap(4).id().equals("village_2")
            && com.hex.kagune.control.village.SiteMaps.of("village_1") == null
            && java.util.Arrays.stream(com.hex.kagune.control.village.SiteMaps.ALL).noneMatch(site -> site.map() == 3 || site.map() == 4),
            "an older world's townspeople keep their town's streets and patrol; the towns are never built again");
        check(com.hex.kagune.control.village.SiteMaps.byMap(3).homes().length > 0 && com.hex.kagune.control.village.SiteMaps.byMap(4).route().length > 0,
            "their homes and patrol are there");
        try {
            String structures = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java"));
            // 4.1.72 (the owner: "from now on the small city is gone, erased."): its four quarters' maps (7 to 10) kept for its people in a
            // world made before, never built again.
            boolean retired = true;
            for (int k = 0; k < 4; k++) {
                com.hex.kagune.control.village.SiteMaps.Site q = com.hex.kagune.control.village.SiteMaps.byMap(7 + k);
                retired &= q != null && q.id().equals("small_city_" + k) && q.width() == 272 && q.homes().length == 12 && q.route().length >= 8
                    && com.hex.kagune.control.village.SiteMaps.of("small_city_" + k) == null;
            }
            check(retired && java.util.Arrays.stream(com.hex.kagune.control.village.SiteMaps.ALL).noneMatch(site -> site.map() >= 7 && site.map() <= 10)
                && !structures.contains("class City ") && !structures.contains("CityPlacement"),
                "4.1.72: the Small City is never built again; an older world's city people keep their quarters' streets, homes and patrol");
            String humans = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/VillageHumans.java"));
            String brain = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/HumanBrain.java"));
            check(humans.contains("m_128405_(HOME + \"X\"") && humans.contains("m_128405_(HOME, 1)")
                && brain.contains("saved.m_128441_(VillageHumans.HOME)") && brain.contains("VillageHumans.HOME_RANGE * VillageHumans.HOME_RANGE")
                && com.hex.kagune.control.server.VillageHumans.HOME_RANGE <= com.hex.kagune.control.server.VillageHumans.CROWD,
                "a villager's human keeps about its village, within the crowd it is counted in");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
    }

    /**
     * 4.1.72 (the owner, with humanhotel.schem: "from now on the small city is gone, erased. / From now on close to a village will be an
     * hotel where humans work, live and walk around doing their own thing. / CCG patrols around the hotel at all times."; they chose
     * "Beside some villages"): the hotel's site, its tiles, its people, where hotels stand and how they face, the city's files gone, the
     * owner's file untouched.
     */
    static void hotel() {
        com.hex.kagune.control.village.SiteMaps.Site site = com.hex.kagune.control.village.SiteMaps.of("hotel");
        check(site != null && site.width() == 191 && site.depth() == 196 && site.height() == 194 && site.ground() == 11 && site.map() == 12
            && com.hex.kagune.control.village.SiteMaps.byMap(12) == site, "the hotel: the owner's 191 x 196, 194 tall (its empty top left off), its ground its slab's top (11), map 12");
        check(site != null && site.homes().length == 10 && site.route().length >= 8, "humans at home in it (10 homes, two each), a patrol round it");
        boolean tiles = site != null && site.across() == 3 && site.down() == 4;
        for (int i = 0; site != null && i < site.across(); i++) {
            for (int j = 0; j < site.down(); j++) {
                tiles &= java.nio.file.Files.isRegularFile(java.nio.file.Path.of("src/main/resources/data/hexkagune/structures/hotel/t_" + i + "_" + j + ".nbt"));
            }
        }
        check(tiles, "every tile of the hotel written (3 x 4)");
        // Its people: the humans who live in it and the investigators who patrol round it, written into its tiles.
        int humans = 0;
        int agents = 0;
        for (java.nio.file.Path tile : java.util.List.of(java.nio.file.Path.of("src/main/resources/data/hexkagune/structures/hotel"))) {
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(tile)) {
                for (java.nio.file.Path file : files.toList()) {
                    String text = new String(new java.util.zip.GZIPInputStream(java.nio.file.Files.newInputStream(file)).readAllBytes(),
                        java.nio.charset.StandardCharsets.ISO_8859_1);
                    humans += text.split("hexkagune:human(?![a-z_])", -1).length - 1; // the entity's id, never a longer id it begins
                    agents += text.split("hexkagune:ccg_investigator", -1).length - 1;
                }
            } catch (java.io.IOException failure) {
                check(false, "read the hotel's tiles: " + failure);
            }
        }
        check(humans == 2 * site.homes().length && agents == 6, "people: two humans living at each of its homes (" + humans
            + "), six investigators on patrol round it (" + agents + ")");
        // Where hotels stand (HotelPlan, pure): one village in four, on the first side (from its own, by the seed, round clockwise) clear of
        // every other village's offer, DISTANCE chunks out, never overlapping another hotel, turned to face its village.
        int chosen = 0;
        for (int x = -60; x < 60; x++) {
            for (int z = -60; z < 60; z++) {
                chosen += com.hex.kagune.control.village.HotelPlan.chosen(1234L, x, z) ? 1 : 0;
            }
        }
        check(chosen > 14400 * 20 / 100 && chosen < 14400 * 30 / 100 && com.hex.kagune.control.village.HotelPlan.ONE_IN == 4,
            "about one village in four is chosen for a hotel (" + chosen + " of 14400)");
        // Its near edge, on its nearest sides (north and west: the footprint centres on its chunk's middle), from the village's start corner.
        int d16 = com.hex.kagune.control.village.HotelPlan.DISTANCE * 16;
        int deepest = Math.max(site.width(), site.depth());
        int nearEdge = Math.min(d16 - (8 - deepest / 2 + deepest - 1), d16 + 8 - deepest / 2);
        check(nearEdge == 119 && nearEdge - com.hex.kagune.control.village.SitePrep.MARGIN - 12 >= 80 + 16
            && com.hex.kagune.control.village.HotelPlan.VILLAGE_REACH * 16 >= 80 + 16,
            "a hotel's near edge " + nearEdge + " blocks out of its village's start: its clearing (8) and its terrain fit (12) short of where a "
                + "village's pieces may reach (80 from its start piece's middle, that piece up to 16 off: 96)");
        check(com.hex.kagune.control.village.HotelPlan.REACH * 16 >= deepest / 2 + 8 && com.hex.kagune.control.village.HotelPlan.REACH * 16 <= 128
            && com.hex.kagune.control.village.HotelPlan.CLEAR_OF == com.hex.kagune.control.village.HotelPlan.REACH
                + com.hex.kagune.control.village.HotelPlan.VILLAGE_REACH + 1
            && com.hex.kagune.control.village.HotelPlan.APART == 2 * com.hex.kagune.control.village.HotelPlan.REACH + 1,
            "a hotel within a start's reach (8 chunks); no other village within its reach and a village's; two hotels never within each other's");
        for (int reach : new int[] {13, 14, 27, 28}) {
            int[] scan = com.hex.kagune.control.village.HotelPlan.scan(reach);
            boolean even = scan[0] == -reach && scan[scan.length - 1] == reach;
            for (int k = 1; k < scan.length; k++) {
                even &= scan[k] > scan[k - 1] && scan[k] - scan[k - 1] <= com.hex.kagune.control.village.HotelPlan.SCAN_GAP;
            }
            // 4.1.72: a first scan (-13 by 6s) never asked 12 or 13 chunks out.
            check(even, "the offers asked from " + -reach + " to " + reach + " chunks, none more than 6 apart: " + java.util.Arrays.toString(scan));
        }
        int villagesSeen = 0;
        int chosenSeen = 0;
        int hotelsSeen = 0;
        int[] sides = new int[4];
        boolean placed = true;
        boolean apart = true;
        boolean clear = true;
        boolean same = true;
        int nearCount = 0;
        int nearWrong = 0;
        for (long seed : new long[] {1234L, -99L, 8675309L, 42L}) {
            // Vanilla's villages: a linear spread, spacing 34, separation 8, salt 10387312 (data/minecraft/worldgen/structure_set/villages.json).
            java.util.Map<Long, int[]> memo = new java.util.HashMap<>();
            com.hex.kagune.control.village.HotelPlan.Offers villages = (cx, cz) -> {
                int rx = Math.floorDiv(cx, 34);
                int rz = Math.floorDiv(cz, 34);
                return memo.computeIfAbsent((long)rx << 32 ^ rz & 0xFFFFFFFFL,
                    k -> com.hex.kagune.control.village.DryLand.spreadChunk(seed, rx, rz, 34, 8, 10387312));
            };
            java.util.List<int[]> offers = new java.util.ArrayList<>();
            java.util.List<int[]> hotels = new java.util.ArrayList<>();
            for (int rx = -8; rx <= 7; rx++) {
                for (int rz = -8; rz <= 7; rz++) {
                    offers.add(com.hex.kagune.control.village.DryLand.spreadChunk(seed, rx, rz, 34, 8, 10387312));
                }
            }
            for (int[] v : offers) {
                boolean inner = Math.abs(v[0]) < 200 && Math.abs(v[1]) < 200;
                villagesSeen += inner ? 1 : 0;
                chosenSeen += inner && com.hex.kagune.control.village.HotelPlan.chosen(seed, v[0], v[1]) ? 1 : 0;
                int[] h = com.hex.kagune.control.village.HotelPlan.hotelOf(seed, villages, v[0], v[1]);
                same &= java.util.Arrays.equals(h, com.hex.kagune.control.village.HotelPlan.hotelOf(seed, villages, v[0], v[1]));
                if (h == null) {
                    continue;
                }
                hotels.add(h);
                if (!inner) {
                    continue;
                }
                hotelsSeen++;
                sides[h[2]]++;
                int[] back = com.hex.kagune.control.village.HotelPlan.village(h[0], h[1], h[2]);
                placed &= back[0] == v[0] && back[1] == v[1] && Math.abs(h[0] - v[0]) + Math.abs(h[1] - v[1]) == com.hex.kagune.control.village.HotelPlan.DISTANCE;
                for (int[] o : offers) {
                    if (o != v) {
                        clear &= Math.max(Math.abs(o[0] - h[0]), Math.abs(o[1] - h[1])) > com.hex.kagune.control.village.HotelPlan.CLEAR_OF;
                    }
                }
            }
            for (int[] a : hotels) {
                for (int[] b : hotels) {
                    if (a != b) {
                        apart &= Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1])) >= com.hex.kagune.control.village.HotelPlan.APART;
                    }
                }
            }
            // HotelPlan.near (what keeps every other place clear of the hotels): right at each hotel's margin, inside and out, and all round.
            java.util.List<int[]> asked = new java.util.ArrayList<>();
            int edge = com.hex.kagune.control.village.HotelPlan.REACH + com.hex.kagune.control.village.HotelPlan.MARGIN;
            for (int[] h : hotels) {
                if (Math.abs(h[0]) > 150 || Math.abs(h[1]) > 150) {
                    continue;
                }
                for (int r = edge; r <= edge + 1; r++) {
                    for (int k = -r; k <= r; k++) {
                        asked.add(new int[] {h[0] + k, h[1] - r});
                        asked.add(new int[] {h[0] + k, h[1] + r});
                        asked.add(new int[] {h[0] - r, h[1] + k});
                        asked.add(new int[] {h[0] + r, h[1] + k});
                    }
                }
            }
            for (int x = -150; x < 150; x += 3) {
                for (int z = -150; z < 150; z += 3) {
                    asked.add(new int[] {x, z});
                }
            }
            for (int[] c : asked) {
                boolean want = false;
                for (int[] h : hotels) {
                    want |= com.hex.kagune.control.village.HotelPlan.covers(h[0], h[1], c[0], c[1], com.hex.kagune.control.village.HotelPlan.MARGIN);
                }
                nearCount += want ? 1 : 0;
                nearWrong += want != com.hex.kagune.control.village.HotelPlan.near(seed, villages, c[0], c[1]) ? 1 : 0;
            }
        }
        System.out.println("  hotels: " + villagesSeen + " village offers, " + chosenSeen + " chosen, " + hotelsSeen + " hotels (sides "
            + java.util.Arrays.toString(sides) + ")");
        check(hotelsSeen * 100 >= chosenSeen * 75 && hotelsSeen * 100 <= villagesSeen * 30,
            "most chosen villages find a side clear of the rest (" + hotelsSeen + " of " + chosenSeen + " chosen, " + villagesSeen + " villages)");
        check(sides[0] > hotelsSeen / 8 && sides[1] > hotelsSeen / 8 && sides[2] > hotelsSeen / 8 && sides[3] > hotelsSeen / 8 && placed,
            "on every side, " + com.hex.kagune.control.village.HotelPlan.DISTANCE + " chunks out, each hotel's village found back from it: "
                + java.util.Arrays.toString(sides));
        check(clear, "no hotel within reach of another village's offer (a village of its own's never under a hotel)");
        check(apart, "no two hotels within each other's reach (the one beside the first village stands)");
        check(same, "the same world makes the same choice every time (by its seed and the villages' offers)");
        check(nearCount > 10000 && nearWrong == 0, "every chunk a hotel's margin reaches is known near one, and no other: "
            + nearCount + " chunks near them, " + nearWrong + " judged wrong");
        boolean faces = true;
        int[][] step = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int side = 0; side < 4; side++) {
            int[] front = com.hex.kagune.control.village.HotelPlan.front(com.hex.kagune.control.village.HotelPlan.turns(side));
            faces &= front[0] == -step[side][0] && front[1] == -step[side][1];
        }
        check(faces, "turned so its forecourt (its file's south) faces its village");
        check(com.hex.kagune.control.village.HotelPlan.covers(0, 0, 7, -7, 0) && !com.hex.kagune.control.village.HotelPlan.covers(0, 0, 8, 0, 0)
            && com.hex.kagune.control.village.HotelPlan.covers(0, 0, 13, 0, com.hex.kagune.control.village.HotelPlan.MARGIN), "a hotel's chunks, and the margin kept round them");
        // 4.1.72 (the owner: "Only spawns on plains or biomes o plenty equivallent biomes, we cant have that shit spawning on mountains"):
        // every column of its footprint and round it a plains biome (hexkagune:plains).
        String plainsTag = read("src/main/resources/data/hexkagune/tags/worldgen/biome/plains.json");
        java.util.List<String> plainsIds = new java.util.ArrayList<>();
        java.util.regex.Matcher id = java.util.regex.Pattern.compile("\"(?:id\": \")?([a-z_]+:[a-z_]+)\"").matcher(plainsTag);
        while (id.find()) {
            plainsIds.add(id.group(1));
        }
        check(plainsIds.equals(java.util.List.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains", "biomesoplenty:field",
                "biomesoplenty:grassland", "biomesoplenty:pasture", "biomesoplenty:prairie", "biomesoplenty:lavender_field"))
            && plainsTag.split("\"required\": false").length == 6 && !plainsTag.contains("#") && !plainsTag.contains("meadow")
            && !plainsTag.contains("highland") && !plainsTag.contains("rocky_shrubland"),
            "a hotel's biomes: vanilla's plains and Biomes O' Plenty's open grasslands (each only if it is there); never forge:is_plains (the meadow, "
                + "BOP's highland and rocky shrubland are in it): " + plainsIds);
        int px = 1000;
        int pz = -400;
        check(com.hex.kagune.control.village.HotelPlan.onPlains(px, pz, 196, 191, (x, z) -> true), "all plains: a hotel");
        check(!com.hex.kagune.control.village.HotelPlan.onPlains(px, pz, 196, 191, (x, z) -> !(x > px + 150 && z > pz + 150)),
            "a mountain in one corner of it: no hotel");
        boolean strips = com.hex.kagune.control.village.HotelPlan.PLAINS_STEP <= 16;
        for (int at = -16; at < 191; at++) { // every strip a chunk wide that touches its footprint (196 x 191)
            int fromX = px + at;
            int fromZ = pz + at;
            strips &= !com.hex.kagune.control.village.HotelPlan.onPlains(px, pz, 196, 191, (x, z) -> !(x >= fromX && x < fromX + 17))
                && !com.hex.kagune.control.village.HotelPlan.onPlains(px, pz, 196, 191, (x, z) -> !(z >= fromZ && z < fromZ + 17));
        }
        check(strips, "a strip of forest a chunk wide anywhere across it, either way: no hotel");
        check(!com.hex.kagune.control.village.HotelPlan.onPlains(px, pz, 196, 191, (x, z) -> z >= pz),
            "plains under all of it but a hill just past its north edge: no hotel (looked at round it too)");
        check(read("src/main/java/com/hex/kagune/control/ControlStructures.java").contains("if (!asked && !com.hex.kagune.control.village.HotelPlan.onPlains("
            + "minX, minZ, sizeX, sizeZ, (x, z) -> plains(context, x, y, z))) {"), "the hotel asks the world's biomes so before it is built (/place builds it anywhere)");
        check(java.util.Arrays.asList(com.hex.kagune.control.village.HotelPlan.VILLAGE_TAGS).equals(java.util.List.of("village_plains", "village_desert",
            "village_savanna", "village_snowy", "village_taiga")), "a village stands where vanilla's village biome tags say (a pack's biomes in them too)");
        String structures = read("src/main/java/com/hex/kagune/control/ControlStructures.java");
        check(structures.contains("if (HOTEL.equals(this.site.id())) {\n                return this.hotel(context, chunk); // 4.1.72")
            && structures.contains(".m_7745_(net.minecraft.resources.ResourceLocation.m_214293_(\"minecraft\", \"villages\"));")
            && structures.contains("int[] hotel = com.hex.kagune.control.village.HotelPlan.hotelOf(seed, offers, at[0], at[1]);\n"
                + "                if (hotel == null || hotel[0] != chunk.f_45578_ || hotel[1] != chunk.f_45579_) {")
            && structures.contains("if (!village(context, at[0], at[1]) || holdsPlace(chunk)) {")
            && structures.contains("return this.hotelAt(context, chunk, com.hex.kagune.control.village.HotelPlan.turns(hotel[2]), false);"),
            "a hotel starts only beside a village that is really there (the world's own village placement and biomes), clear of an older world's place");
        check(structures.contains("int middle = DryLand.land(ground, minX, minZ, sizeX, sizeZ, 9, HIDEOUT_FLAT, DryLand.DEEP);")
            && structures.contains("|| !asked && !biome(context, minX + sizeX / 2, middle, minZ + sizeZ / 2)) {"), "on land as the hideout judges its own: dry, fairly flat, no sea");
        check(structures.contains("|| inHotel(context, chunk.f_45578_, chunk.f_45579_)) {") && structures.contains("if (inHotel(context, chunk.f_45578_, chunk.f_45579_)) {")
            && structures.contains("if (inHotel(context, x, z) ||")
            && structures.contains("return com.hex.kagune.control.village.HotelPlan.near(seed, villageOffers(spread, seed), x, z);")
            && structures.contains("ChunkPos offer = spread.m_227008_(seed, cx, cz);"),
            "the hideout, the CCG building and every jigsaw place stay clear of the hotels (judged by HotelPlan.near on the world's own village offers)");
        String set = read("src/main/resources/data/hexkagune/worldgen/structure_set/hotel.json");
        String structure = read("src/main/resources/data/hexkagune/worldgen/structure/hotel.json");
        check(set.contains("\"structure\": \"hexkagune:hotel\"") && set.contains("\"spacing\": 1,") && set.contains("\"separation\": 0,")
            && set.contains("\"salt\": " + com.hex.kagune.control.village.DryLand.HOTEL_SALT) && structure.contains("\"type\": \"hexkagune:site_hotel\"")
            && structure.contains("\"terrain_adaptation\": \"beard_box\"") && structure.contains("\"step\": \"top_layer_modification\""),
            "its set offers every chunk (the hotel decides); its structure its own type, the ground fitted to it, after the trees");
        check(!java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure_set/small_city.json"))
            && !java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure/small_city.json"))
            && !java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/structures/small_city_0"))
            && !java.nio.file.Files.exists(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/village/CityPlan.java")),
            "4.1.72: the Small City's set, structure, tiles and plan are gone");
        try {
            byte[] owners = java.nio.file.Files.readAllBytes(java.nio.file.Path.of("art/exploration/humanhotel.schem"));
            String sum = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(owners));
            check(sum.equals("e8ec48539030baf6116bedbad8dece19908489ac5a171d0068dc51ab77d127ea"), "the owner's humanhotel.schem is as they sent it");
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException failure) {
            check(false, "read the owner's hotel: " + failure);
        }
        String humansSrc = read("src/main/java/com/hex/kagune/control/server/VillageHumans.java");
        check(humansSrc.contains("instanceof net.minecraft.world.entity.npc.Villager") && humansSrc.contains(".m_128441_(KEY)")
            && humansSrc.contains(">= MOST") && com.hex.kagune.control.server.VillageHumans.MOST > 0 && com.hex.kagune.control.server.VillageHumans.ODDS > 1,
            "humans in vanilla villages too: each villager looked at once, a few humans, never a crowd, none by a wandering trader");
    }

    static void ccgBuilding() {
        com.hex.kagune.control.village.SiteMaps.Site site = com.hex.kagune.control.village.SiteMaps.of("ccg_building");
        check(site != null && site.width() == 31 && site.depth() == 46 && site.height() == 65 && site.ground() == 3 && site.map() == 11
            && site.across() == 1 && site.down() == 1 && com.hex.kagune.control.village.SiteMaps.byMap(11) == site,
            "4.1.51: the building is the owner's furnished tower (31 x 46, 65 tall, its ground 3), one tile, its own map (11)");
        com.hex.kagune.control.village.SiteMaps.Site old = com.hex.kagune.control.village.SiteMaps.byMap(6);
        check(old != null && old != site && "ccg_building".equals(old.id()) && old.width() == 39 && old.depth() == 39 && old.route().length == 15
            && !java.util.Arrays.asList(com.hex.kagune.control.village.SiteMaps.ALL).contains(old),
            "4.1.51: the old building's map (6) is kept for its people in a world made before (their patrol), never built again");
        check(com.hex.kagune.control.village.SiteMaps.ALL.length == 3 && com.hex.kagune.control.village.SiteMaps.of("aogiri_hideout").map() == 5
            && com.hex.kagune.control.village.SiteMaps.of("village_1") == null && com.hex.kagune.control.village.SiteMaps.of("village_2") == null
            && com.hex.kagune.control.village.SiteMaps.byMap(3) != null && com.hex.kagune.control.village.SiteMaps.byMap(4) != null,
            "the hideout keeps its map; the two towns are gone (4.1.36), their maps kept for an older world's people (4.1.40); 4.1.72: the hideout, the "
                + "building and the hotel are all the sites built");
        // Its patrol: a loop round the tower, two blocks out from its walls (the world's ground, levelled by its terrain adaptation).
        int[][] route = site.route();
        boolean round = route.length >= 12;
        int west = 0;
        int east = 0;
        int north = 0;
        int south = 0;
        for (int i = 0; i < route.length; i++) {
            int[] a = route[i];
            int[] b = route[(i + 1) % route.length];
            round &= Math.hypot(a[0] - b[0], a[2] - b[2]) <= 24.0 && a[1] == 0 && Math.abs(a[0]) <= site.perimeter() && Math.abs(a[2]) <= site.perimeter();
            west += a[0] <= -17 ? 1 : 0;
            east += a[0] >= 17 ? 1 : 0;
            north += a[2] <= -25 ? 1 : 0;
            south += a[2] >= 24 ? 1 : 0;
        }
        check(round && west >= 3 && east >= 3 && north >= 3 && south >= 3, "its patrol goes round it: stops on every side, none far from the next, at street level");
        // Its offers: the game's own for its set, none near a spot of the mansion's; never the hideout's own offers.
        long[] seeds = {0L, 1L, -7L, 123456789L, 8675309L, Long.MIN_VALUE + 5L, 4242424242L};
        int fewest = Integer.MAX_VALUE;
        for (long seed : seeds) {
            int[][] offers = com.hex.kagune.control.village.DryLand.ccgOffers(seed);
            fewest = Math.min(fewest, offers.length);
            boolean ok = true;
            long last = -1L;
            int[][] hideouts = com.hex.kagune.control.village.DryLand.hideoutOffers(seed);
            for (int[] offer : offers) {
                long d = (long)offer[0] * offer[0] + (long)offer[1] * offer[1];
                ok &= d >= last && d >= (long)com.hex.kagune.control.village.DryLand.NEAREST * com.hex.kagune.control.village.DryLand.NEAREST;
                last = d;
                int[] again = com.hex.kagune.control.village.DryLand.spreadChunk(seed, Math.floorDiv(offer[0], 40), Math.floorDiv(offer[1], 40), 40, 20,
                    com.hex.kagune.control.village.DryLand.CCG_SALT);
                ok &= again[0] == offer[0] && again[1] == offer[1];
                for (int[] hideout : hideouts) {
                    ok &= Math.hypot(hideout[0] - offer[0], hideout[1] - offer[1]) >= com.hex.kagune.control.village.DryLand.CCG_APART;
                }
                // 4.1.36: the towns' spots are gone (the Small City is kept clear where it is built: ControlStructures.inCity).
            }
            check(ok && java.util.Arrays.deepEquals(offers, com.hex.kagune.control.village.DryLand.ccgOffers(seed)),
                "seed " + seed + ": the building's offers are the game's own, nearest first, none within " + com.hex.kagune.control.village.DryLand.CCG_APART
                    + " chunks of a spot of the mansion's");
        }
        check(fewest >= 6, "every world has several spots to try (the fewest of these seeds: " + fewest + ")");
        // 12 chunks: the mansion and the towns reach 136 blocks from their middle, the tower and its patrol 27 (4.1.51).
        check(site.perimeter() <= 28 && com.hex.kagune.control.village.DryLand.CCG_APART * 16 >= 136 + 28 + 16 && com.hex.kagune.control.village.DryLand.CCG_SALT != com.hex.kagune.control.village.DryLand.SALT
            && com.hex.kagune.control.village.DryLand.CCG_SALT != com.hex.kagune.control.village.DryLand.HOTEL_SALT, "far enough that they never touch; its own salt");
        check(com.hex.kagune.control.village.DryLand.near(5L, 100, 100, 1000, 40, 20, 77) && !com.hex.kagune.control.village.DryLand.near(5L, 100, 100, 0, 40, 20, 77),
            "near: anything within a great reach, nothing within none");
        try {
            java.nio.file.Path data = java.nio.file.Path.of("src/main/resources/data/hexkagune");
            String set = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure_set/ccg_building.json")));
            check(set.contains("\"minecraft:random_spread\"") && set.contains("\"spacing\": " + com.hex.kagune.control.village.DryLand.SPACING + ",")
                && set.contains("\"separation\": " + com.hex.kagune.control.village.DryLand.SEPARATION + ",")
                && set.contains("\"salt\": " + com.hex.kagune.control.village.DryLand.CCG_SALT) && set.contains("\"structure\": \"hexkagune:ccg_building\"")
                && !set.contains("\"spread_type\"") && !set.contains("frequency") && !set.contains("exclusion_zone"),
                "its offers are the game's own (a linear random spread, the same numbers as DryLand's)");
            check(!java.nio.file.Files.exists(data.resolve("worldgen/structure_set/villages.json")), "the towns' set is gone (4.1.36)");
            String structure = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure/ccg_building.json")));
            check(structure.contains("\"type\": \"hexkagune:site_ccg_building\"") && structure.contains("\"biomes\": \"#hexkagune:overworld\"")
                && structure.contains("\"terrain_adaptation\": \"beard_box\""), "its structure: its own type, any biome of the overworld, the ground fitted to it");
            String structures = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java")));
            check(structures.contains("return claimed(CCG, context, () -> {")
                && structures.contains("DryLand.near(hideoutSpread(context), chunk.f_45578_, chunk.f_45579_, DryLand.CCG_APART)")
                && structures.contains("|| inHotel(context, chunk.f_45578_, chunk.f_45579_)) {"),
                "the world's one building, at the first of its set's spots on dry land the world makes, clear of the hideout's and the hotels");
            // The template itself: its people, and what each does.
            java.util.Map<String, Object> template;
            try (java.io.DataInputStream in = new java.io.DataInputStream(new java.util.zip.GZIPInputStream(
                java.nio.file.Files.newInputStream(data.resolve("structures/ccg_building/t_0_0.nbt"))))) {
                in.readByte();
                in.readUTF();
                template = (java.util.Map<String, Object>)nbt(in, 10);
            }
            List<Object> size = (List<Object>)template.get("size");
            check(size.equals(List.of(31, 65, 46)), "the template's size is the site's " + size);
            List<Object> palette = (List<Object>)template.get("palette");
            java.util.Map<List<Object>, String> blocks = new java.util.HashMap<>();
            for (Object block : (List<Object>)template.get("blocks")) {
                java.util.Map<String, Object> b = (java.util.Map<String, Object>)block;
                blocks.put((List<Object>)b.get("pos"), (String)((java.util.Map<String, Object>)palette.get((Integer)b.get("state"))).get("Name"));
            }
            int seats = 0;
            int posts = 0;
            int walkers = 0;
            int generals = 0;
            int staff = 0;
            int paintings = 0;
            int frames = 0;
            boolean sound = true;
            boolean hung = true;
            int street = site.ground() + 1;
            java.util.Set<Integer> seatFloors = new java.util.HashSet<>();
            java.util.Set<Integer> postFloors = new java.util.HashSet<>();
            java.util.Set<List<Object>> spots = new java.util.HashSet<>();
            List<Object> entities = (List<Object>)template.get("entities");
            for (Object entity : entities) {
                java.util.Map<String, Object> e = (java.util.Map<String, Object>)entity;
                java.util.Map<String, Object> tag = (java.util.Map<String, Object>)e.get("nbt");
                List<Object> at = (List<Object>)e.get("blockPos");
                int x = (Integer)at.get(0);
                int y = (Integer)at.get(1);
                int z = (Integer)at.get(2);
                if ("minecraft:painting".equals(tag.get("id")) || "minecraft:item_frame".equals(tag.get("id"))) {
                    // 4.1.51: the owner's paintings and item frames, each in the block it hung in (its position that block's middle), on
                    // a wall (a solid block behind it).
                    List<Object> pos = (List<Object>)e.get("pos");
                    boolean painting = "minecraft:painting".equals(tag.get("id"));
                    paintings += painting ? 1 : 0;
                    frames += painting ? 0 : 1;
                    int face = ((Number)tag.get(painting ? "facing" : "Facing")).intValue();
                    // A painting's facing is 2D (0 south, 1 west, 2 north, 3 east), a frame's 3D (2 north, 3 south, 4 west, 5 east).
                    int[] back = painting ? new int[][] {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}[face]
                        : face == 2 ? new int[] {0, 1} : face == 3 ? new int[] {0, -1} : face == 4 ? new int[] {1, 0} : new int[] {-1, 0};
                    String wall = blocks.get(List.of(x + back[0], y, z + back[1]));
                    hung &= ((Number)pos.get(0)).doubleValue() == x + 0.5 && ((Number)pos.get(1)).doubleValue() == y + 0.5
                        && ((Number)pos.get(2)).doubleValue() == z + 0.5 && wall != null && !"minecraft:air".equals(wall)
                        && (painting ? tag.get("variant") != null : tag.get("Item") != null) && !tag.containsKey("ForgeData");
                    continue;
                }
                java.util.Map<String, Object> forge = (java.util.Map<String, Object>)tag.get("ForgeData");
                java.util.Map<String, Object> village = (java.util.Map<String, Object>)forge.get("HexVillage");
                String here = blocks.get(at);
                String over = blocks.get(List.of(x, y + 1, z));
                String under = blocks.get(List.of(x, y - 1, z));
                boolean free = (here == null || "minecraft:air".equals(here) || here.endsWith("_carpet")) && (over == null || "minecraft:air".equals(over))
                    && under != null && !"minecraft:air".equals(under);
                if ("hexkagune:general".equals(tag.get("id"))) {
                    // 4.1.5: the General; 4.1.51: in the top floor's boss's office.
                    generals++;
                    sound &= y == street + 9 * 6 && spots.add(at) && free
                        && !forge.containsKey(com.hex.kagune.control.npc.HqRules.SEAT) && !forge.containsKey(com.hex.kagune.control.npc.HqRules.POST);
                    continue;
                }
                sound &= Integer.valueOf(11).equals(village.get("Map")) && spots.add(at) && Byte.valueOf((byte)1).equals(tag.get("PersistenceRequired"))
                    && (Integer)village.get("CentreX") == site.middleX() - x && (Integer)village.get("CentreZ") == site.middleZ() - z
                    && (Integer)village.get("CentreY") == street - y;
                if ("hexkagune:human".equals(tag.get("id"))) {
                    // 4.1.51 (the owner chose "Restaurant staff humans"): in the lobby, where its villagers stood, each its own home.
                    staff++;
                    sound &= y == street && free && (Integer)village.get("Home") >= 0 && !forge.containsKey(com.hex.kagune.control.npc.HqRules.SEAT);
                    continue;
                }
                sound &= "hexkagune:ccg_investigator".equals(tag.get("id"));
                boolean seat = forge.containsKey(com.hex.kagune.control.npc.HqRules.SEAT);
                boolean post = forge.containsKey(com.hex.kagune.control.npc.HqRules.POST);
                if (seat) {
                    seats++;
                    seatFloors.add((y - street) / 6);
                    int yaw = (Integer)forge.get(com.hex.kagune.control.npc.HqRules.YAW);
                    // On a chair (a stair) with nothing over it, a floor's own height; before it open floor, a carpet, or a desk or a table
                    // the legs go under.
                    double[] looks = {-Math.sin(Math.toRadians(yaw)), Math.cos(Math.toRadians(yaw))};
                    String front = blocks.get(List.of(x + (int)Math.round(looks[0]), y, z + (int)Math.round(looks[1])));
                    sound &= !post && here != null && here.endsWith("_stairs") && (over == null || over.endsWith("_trapdoor")) && (y - street) % 6 == 0
                        && yaw % 90 == 0 && (front == null || "minecraft:air".equals(front) || front.endsWith("_carpet") || front.endsWith("_fence")
                            || front.endsWith("_planks") || front.endsWith("_slab") || "minecraft:piston_head".equals(front))
                        && blocks.get(List.of(x, y + 2, z)) == null;
                } else {
                    sound &= free;
                    if (post) {
                        posts++;
                        postFloors.add((y - street) / 6);
                        sound &= forge.containsKey(com.hex.kagune.control.npc.HqRules.YAW);
                    } else {
                        walkers++;
                        sound &= y == street; // out of doors, between its hedges and its glass
                    }
                }
            }
            check(sound, "every one of its people: of this building (its map), kept, alone on its spot, a seat on a chair with room for its knees, "
                + "the rest with a floor under them and room to stand");
            check(generals == 1, "4.1.5: the General, once, on the top floor");
            check(seats == 29 && posts == 13 && walkers == 6 && staff == 4, "4.1.51: 48 investigators: 29 seated (the two receptionists, three on each "
                + "office floor), 13 at posts (two at the door, two at the lift, one by the lift on each floor), 6 on patrol; the restaurant's "
                + "4 staff (" + seats + ", " + posts + ", " + walkers + ", " + staff + ")");
            java.util.Set<Integer> floors = new java.util.HashSet<>();
            for (int f = 0; f < 10; f++) {
                floors.add(f);
            }
            check(seatFloors.equals(floors) && postFloors.equals(floors), "someone at a desk and someone on guard on every one of its ten floors");
            check(hung && paintings == 26 && frames == 30 && entities.size() == 48 + 1 + 4 + 26 + 30,
                "4.1.51: the owner's 26 paintings and 30 item frames, each on its wall (" + paintings + ", " + frames + ")");
            int signs = 0;
            int buttons = 0;
            for (java.util.Map.Entry<List<Object>, String> block : blocks.entrySet()) {
                signs += block.getValue().endsWith("_sign") ? 1 : 0;
            }
            for (int f = 0; f < 10; f++) {
                buttons += "minecraft:stone_button".equals(blocks.get(List.of(16, street + 1 + f * com.hex.kagune.control.village.LiftRules.STOREY, 14))) ? 1 : 0;
            }
            check(signs == 145, "the owner's file's signs are all there: " + signs);
            check(buttons == 10, "4.1.51: a lift button in every floor's cabin, one over the other, a storey apart: " + buttons);
            check(com.hex.kagune.control.village.LiftRules.ride(1, 0) == 6 && com.hex.kagune.control.village.LiftRules.ride(8, 1) == 6
                && com.hex.kagune.control.village.LiftRules.ride(0, 9) == -54 && com.hex.kagune.control.village.LiftRules.ride(0, 0) == 0
                && com.hex.kagune.control.village.LiftRules.floor(0) == 1 && com.hex.kagune.control.village.LiftRules.floor(9) == 10
                && com.hex.kagune.control.village.LiftRules.aboard(1.5, 1.5) && !com.hex.kagune.control.village.LiftRules.aboard(3.0, 0.0),
                "4.1.51: the lift does what its signs say: one floor up, from the top back to the lobby; a lone button goes nowhere; ridden from its cabin");
            String lift = read("src/main/java/com/hex/kagune/control/server/CcgLift.java");
            check(lift.contains("PlaceClaims.claimed(\"ccg_building\")") && lift.contains("instanceof ButtonBlock") && lift.contains("LiftRules.aboard(dx, dz)")
                && lift.contains("event.getHand() != InteractionHand.MAIN_HAND"), "the lift only in the world's CCG building, only for one in its cabin, once a press");
        } catch (java.io.IOException failure) {
            check(false, "read the building's files: " + failure);
        }
    }

    /** A named binary tag's payload (type {@code type}) read as plain Java: maps, lists, numbers, strings. */
    private static Object nbt(java.io.DataInputStream in, int type) throws java.io.IOException {
        switch (type) {
            case 1:
                return in.readByte();
            case 2:
                return in.readShort();
            case 3:
                return in.readInt();
            case 4:
                return in.readLong();
            case 5:
                return in.readFloat();
            case 6:
                return in.readDouble();
            case 7: {
                byte[] bytes = new byte[in.readInt()];
                in.readFully(bytes);
                return bytes;
            }
            case 8:
                return in.readUTF();
            case 9: {
                int of = in.readByte();
                int n = in.readInt();
                List<Object> list = new java.util.ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    list.add(nbt(in, of));
                }
                return list;
            }
            case 10: {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                for (int kind = in.readByte(); kind != 0; kind = in.readByte()) {
                    String name = in.readUTF();
                    map.put(name, nbt(in, kind));
                }
                return map;
            }
            case 11: {
                List<Object> ints = new java.util.ArrayList<>();
                for (int n = in.readInt(); n > 0; n--) {
                    ints.add(in.readInt());
                }
                return ints;
            }
            case 12: {
                List<Object> longs = new java.util.ArrayList<>();
                for (int n = in.readInt(); n > 0; n--) {
                    longs.add(in.readLong());
                }
                return longs;
            }
            default:
                throw new java.io.IOException("tag " + type);
        }
    }

    /**
     * 4.0.2 (the owner: "we're gonna replace the Ougiri hideout with another schematic ... While Aogiri Tree only has one hideout, there's
     * multiple villages that can spawn, although they're as rare as a normal village ... at least two guarding the entrances, always in the
     * same spot ... a little message with multiple choices. This only appears once ... If he doesn't like it, they'll attack you ... wait
     * let's say 5 MC days"): the mansion as the hideout, the world's one; the villages as rare as Minecraft's; the gate's question.
     */
    static void mansion() {
        com.hex.kagune.control.village.SiteMaps.Site hideout = com.hex.kagune.control.village.SiteMaps.of("aogiri_hideout");
        check(hideout != null && hideout.width() == 272 && hideout.depth() == 263 && hideout.route().length >= 40,
            "the hideout is the owner's mansion (272 x 263, all a start chunk reaches), its patrol a loop round the house");
        try {
            String set = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure_set/aogiri_hideout.json")));
            // 4.1.1 (the owner: "the mansion is currently happily resting at the very bottom of the ocean"): a spread of offers, the
            // world's one hideout at the first on dry land (DryLand.hideoutOffers reads the same numbers as the game).
            check(set.contains("\"minecraft:random_spread\"") && set.contains("\"spacing\": " + com.hex.kagune.control.village.DryLand.SPACING + ",")
                && set.contains("\"separation\": " + com.hex.kagune.control.village.DryLand.SEPARATION + ",")
                && set.contains("\"salt\": " + com.hex.kagune.control.village.DryLand.SALT) && !set.contains("\"spread_type\"")
                && !set.contains("frequency") && !set.contains("exclusion_zone"), "the hideout's offers are the game's own (a linear random spread, the same numbers)");
            // 4.0.5 (the owner: "also game crash upon creating world", "Failed to load registries"): every structure set's placement has
            // every field the game's codec requires (the hideout's ring had no salt: no world could be made), names structures that exist
            // and biome tags that exist.
            java.nio.file.Path data = java.nio.file.Path.of("src/main/resources/data/hexkagune");
            try (java.util.stream.Stream<java.nio.file.Path> sets = java.nio.file.Files.list(data.resolve("worldgen/structure_set"))) {
                for (java.nio.file.Path file : sets.sorted().toList()) {
                    String json = new String(java.nio.file.Files.readAllBytes(file));
                    String name = file.getFileName().toString();
                    java.util.List<String> needs = new java.util.ArrayList<>(List.of("\"salt\":", "\"structures\":", "\"structure\":", "\"weight\":"));
                    if (json.contains("\"minecraft:concentric_rings\"")) {
                        needs.addAll(List.of("\"distance\":", "\"spread\":", "\"count\":", "\"preferred_biomes\":"));
                    } else if (json.contains("\"hexkagune:city\"")) {
                        needs.remove("\"salt\":"); // 4.1.36: the Small City's own placement: its numbers are its own (DryLand.CITY_*)
                    } else {
                        check(json.contains("\"minecraft:random_spread\""), name + ": a placement the game knows");
                        needs.addAll(List.of("\"spacing\":", "\"separation\":"));
                    }
                    for (String need : needs) {
                        check(json.contains(need), name + ": its placement has " + need);
                    }
                    java.util.regex.Matcher made = java.util.regex.Pattern.compile("\"structure\": \"hexkagune:([a-z0-9_]+)\"").matcher(json);
                    while (made.find()) {
                        check(java.nio.file.Files.exists(data.resolve("worldgen/structure/" + made.group(1) + ".json")), name + ": its structure " + made.group(1) + " exists");
                    }
                    java.util.regex.Matcher tag = java.util.regex.Pattern.compile("\"#hexkagune:([a-z0-9_/]+)\"").matcher(json);
                    while (tag.find()) {
                        check(java.nio.file.Files.exists(data.resolve("tags/worldgen/biome/" + tag.group(1) + ".json")), name + ": its biome tag " + tag.group(1) + " exists");
                    }
                }
            }
            check(!java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure_set/small_city.json"))
                && !java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure_set/villages.json"))
                && !java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure/village_1.json"))
                && !java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure/village_2.json"))
                && !java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure/village.json")),
                "4.1.36: the villages are gone; 4.1.72: the Small City too, the hotels in its place");
            String brain = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java")));
            check(brain.contains("AogiriGuard.idle(npc, mind, level, now)") && brain.contains("AogiriGuard.unwelcome(npc, player, now)"),
                "the members at their work, and a player kept out is gone for");
            String structures = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java")));
            check(structures.contains("return claimed(HIDEOUT, context, () -> {") && structures.contains("if (inHotel(context, chunk.f_45578_, chunk.f_45579_)) {")
                && structures.contains("int middle = DryLand.land(ground(context), minX, minZ, sizeX, sizeZ, 9, HIDEOUT_FLAT, DryLand.DEEP);")
                && structures.contains("if ((CCG.equals(this.site.id()) || unique(this.site)) && onPurpose(context)) {")
                && structures.contains("return claimed(CCG.equals(this.site.id()) ? CCG : HIDEOUT, context, () -> Optional.of(this.stub(")
                && structures.contains("return context.f_226630_().test(null);"),
                "the world's one hideout, at the first spot the world makes where its land holds it (4.1.44: judged as the city's; 4.1.43: /place only while there is none)");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
        check(com.hex.kagune.control.server.AogiriGuard.BAN == 5L * 24000L, "a wrong answer keeps you out five days");
        java.util.UUID one = new java.util.UUID(1L, 2L);
        java.util.UUID two = new java.util.UUID(3L, 4L);
        java.util.List<com.hex.kagune.control.npc.AogiriGate.Answer> a = com.hex.kagune.control.npc.AogiriGate.answers(one);
        check(a.size() == 6 && a.stream().filter(com.hex.kagune.control.npc.AogiriGate.Answer::welcome).count() == 3
            && a.equals(com.hex.kagune.control.npc.AogiriGate.answers(one)) && !a.equals(com.hex.kagune.control.npc.AogiriGate.answers(two)),
            "at the gate: six answers, three he takes and three he doesn't, the same each time for the same player, another's in another order");
        check(com.hex.kagune.control.npc.AogiriGate.ANSWERS.stream().allMatch(x -> !x.label().isEmpty() && !x.reply().isEmpty()),
            "every answer has its words and his reply");
    }
}
