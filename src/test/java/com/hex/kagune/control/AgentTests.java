package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.AgentPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.quinque.Quinque;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.3.27: the CCG's investigators: their skins and bodies (a woman a little taller and skinnier than Eto), the pairs they come in and
 * how rarely (a little more often than a wild ghoul, which is rarer now), who they go for (a ghoul only once they show their kagune),
 * how they fight, their case all but never left behind, the 32-bit clocks their moves ride on, and their run.
 *
 * <p>1.3.28: the two of them on one prey (one holds the front, the other goes round to its back), their case carried by the handle
 * (its carried frame through vanilla's own hand layer: in the fist, hanging plumb, clear of the leg), and their idle.
 */
public final class AgentTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();
    static final Path RESOURCES = Path.of("src/main/resources");

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) throws IOException {
        looks();
        bodies();
        spawning();
        targets();
        fighting();
        clocks();
        running();
        together();
        carrying();
        idling();
        reaperIdling();
        reaperBuild();
        reaperTactics();
        ranks();
        tendril();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Agent tests: " + passed + " checks passed");
    }

    /** 2.0.33: the White Reaper's ground tendril: telegraphed (time to get off the mark), big, and it rises, whips and sinks. */
    static void tendril() {
        check(com.hex.kagune.control.npc.ReaperTendril.erupts() == com.hex.kagune.control.npc.ReaperTendril.PLANT + com.hex.kagune.control.npc.ReaperTendril.WARN
            && com.hex.kagune.control.npc.ReaperTendril.WARN >= 12, "the mark shows long enough to step off it (at least 0.6 s)");
        check(com.hex.kagune.control.npc.ReaperTendril.erupts() < com.hex.kagune.control.npc.ReaperTendril.HOLD
            && com.hex.kagune.control.npc.ReaperTendril.HOLD < com.hex.kagune.control.npc.ReaperTendril.DURATION,
            "the lance stays in the ground until after it rises, then comes out");
        check(com.hex.kagune.control.npc.ReaperTendril.hits(0.0, 0.0, 0.0) && com.hex.kagune.control.npc.ReaperTendril.hits(1.5, 0.0, 0.0)
            && com.hex.kagune.control.npc.ReaperTendril.hits(0.0, 3.0, 0.0) && !com.hex.kagune.control.npc.ReaperTendril.hits(1.7, 0.0, 0.0)
            && !com.hex.kagune.control.npc.ReaperTendril.hits(1.2, 0.0, 1.2) && !com.hex.kagune.control.npc.ReaperTendril.hits(0.0, 6.0, 0.0),
            "it catches whoever stands on the mark (and above it), not beside it");
        check(com.hex.kagune.control.npc.ReaperTendril.RADIUS >= 1.5 && com.hex.kagune.control.npc.ReaperTendril.HEIGHT >= 5.0
            && com.hex.kagune.control.npc.ReaperTendril.THICKNESS >= 0.5F, "a big tendril");
        check(com.hex.kagune.control.npc.ReaperTendril.out(-1.0) == 0.0 && com.hex.kagune.control.npc.ReaperTendril.out(0.0) == 0.0
            && com.hex.kagune.control.npc.ReaperTendril.out(com.hex.kagune.control.npc.ReaperTendril.RISE) == 1.0
            && com.hex.kagune.control.npc.ReaperTendril.out(com.hex.kagune.control.npc.ReaperTendril.LIFE) == 0.0
            && com.hex.kagune.control.npc.ReaperTendril.out(com.hex.kagune.control.npc.ReaperTendril.LIFE - 1) < 0.2,
            "it bursts out fast and sinks back at the end");
        for (double age = 0.0; age < com.hex.kagune.control.npc.ReaperTendril.LIFE; age += 0.5) {
            double[][] line = com.hex.kagune.control.npc.ReaperTendril.line(age, 1234, 14);
            double top = line[line.length - 1][1];
            boolean finite = true;
            double far = 0.0;
            for (double[] p : line) {
                finite &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
                far = Math.max(far, Math.hypot(p[0], p[2]));
            }
            check(finite && line[0][1] < 0.0 && top <= com.hex.kagune.control.npc.ReaperTendril.HEIGHT && far < 2.0,
                "its line leaves the ground and stays over the mark: " + age);
        }
        check(com.hex.kagune.control.npc.ReaperTendril.line(10.0, 1, 14)[13][1] > 4.5, "full height while it whips");
        check(com.hex.kagune.control.npc.ReaperTendril.CHANCE > 0.0 && com.hex.kagune.control.npc.ReaperTendril.COOLDOWN >= 100L
            && com.hex.kagune.control.npc.ReaperTendril.NEAREST > AgentRules.REAPER_KICK_REACH, "now and then, on prey out of a kick's reach");
    }

    /**
     * The owner's nine skins, each in place (a 64x64 skin), four women and five men, each on the arms it is painted for; 2.0.28: and the
     * White Reaper's own, last, slim armed, worn by him alone.
     */
    static void looks() throws IOException {
        check(AgentRules.LOOKS.length == 10, "nine investigators' skins and the White Reaper's");
        check(AgentRules.REGULAR_LOOKS == 9 && AgentRules.REAPER_LOOK == 9, "the Reaper's skin is the one appended last");
        int women = 0;
        int slimMen = 0;
        for (AgentRules.Look look : AgentRules.LOOKS) {
            Path png = RESOURCES.resolve("assets/hexkagune/" + look.path());
            check(Files.exists(png), look.texture() + " is in place");
            if (Files.exists(png)) {
                byte[] head = Files.readAllBytes(png);
                int width = (head[16] & 0xFF) << 24 | (head[17] & 0xFF) << 16 | (head[18] & 0xFF) << 8 | head[19] & 0xFF;
                int height = (head[20] & 0xFF) << 24 | (head[21] & 0xFF) << 16 | (head[22] & 0xFF) << 8 | head[23] & 0xFF;
                check(width == 64 && height == 64, look.texture() + " is a 64x64 skin");
                check(look.slim() == slimArms(png), look.texture() + " is drawn on the arms it is painted for");
            }
            check(!look.female() || look.slim(), look.texture() + ": a woman on the slim arms");
            check(look.texture().matches("ccg_[fm][0-9]|ccg_reaper"), look.texture() + " has a plain name (no file names from the web)");
            women += look.female() ? 1 : 0;
            slimMen += !look.female() && look.slim() ? 1 : 0;
        }
        check(women == 4 && slimMen == 3, "four women; six men, three of them slim armed (the Reaper one of them)");
        AgentRules.Look reaper = AgentRules.LOOKS[AgentRules.REAPER_LOOK];
        check(reaper.texture().equals("ccg_reaper") && !reaper.female() && reaper.slim(), "the White Reaper: his skin, a man's body on the slim arms");
        check(AgentRules.look(-1) == AgentRules.LOOKS[AgentRules.LOOKS.length - 1] && AgentRules.look(AgentRules.LOOKS.length) == AgentRules.LOOKS[0],
            "any look number wraps onto the table");
        java.util.Set<Integer> rolled = new java.util.HashSet<>();
        for (int i = 0; i <= 1000; i++) {
            int look = AgentRules.regularLook(i / 1000.0);
            check(look >= 0 && look < AgentRules.REGULAR_LOOKS, "an ordinary investigator never wears the Reaper's skin: " + i);
            rolled.add(look);
        }
        check(rolled.size() == AgentRules.REGULAR_LOOKS, "every ordinary skin can be rolled");
    }

    /** True if the skin's right arm's outer column (x 54..55, y 20..31) is empty: a slim skin. Reads the PNG's pixels with ImageIO. */
    static boolean slimArms(Path png) throws IOException {
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(png.toFile());
        for (int x = 54; x <= 55; x++) {
            for (int y = 20; y <= 31; y++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A woman: Eto's body, a little taller and a little skinnier, a touch less curvy; a man a player's size. */
    static void bodies() {
        float eto = FemaleBody.SIZE;
        check(AgentRules.FEMALE_SIZE > eto && AgentRules.FEMALE_SIZE < eto * 1.12F, "a woman a little taller than Eto: " + AgentRules.FEMALE_SIZE);
        check(AgentRules.FEMALE_SIZE < AgentRules.MALE_SIZE, "still shorter than a man");
        check(AgentRules.FEMALE_BUILD < 1.0F && AgentRules.FEMALE_BUILD > 0.85F, "and skinnier, just a little");
        check(AgentRules.FEMALE_CURVES < 1.0F && AgentRules.FEMALE_CURVES > 0.6F && AgentRules.FEMALE_LEGS <= AgentRules.FEMALE_CURVES,
            "a touch less of the figure");
        AgentRules.Look woman = AgentRules.LOOKS[0];
        AgentRules.Look man = AgentRules.LOOKS[4];
        check(AgentRules.size(woman) == AgentRules.FEMALE_SIZE && AgentRules.build(woman) == AgentRules.FEMALE_BUILD, "a woman drawn as one");
        check(AgentRules.size(man) == AgentRules.MALE_SIZE && AgentRules.build(man) == 1.0F, "a man a player's size");
    }

    /** Pairs: about one a day (2.0.1; one every two days before), more often than a wild ghoul (one every two or three nights). */
    static void spawning() {
        double agentTries = 24000.0 / AgentRules.SPAWN_EVERY;
        double pairsADay = agentTries * AgentRules.SPAWN_CHANCE;
        double ghoulTries = (NpcRules.NIGHT_TO - NpcRules.NIGHT_FROM) / (double)NpcRules.SPAWN_EVERY;
        double ghoulsADay = ghoulTries * NpcRules.SPAWN_CHANCE;
        check(pairsADay >= 0.8 && pairsADay <= 1.2, "about a pair a day: " + pairsADay + " pairs a day");
        check(pairsADay > ghoulsADay * 1.5, "more often than a wild ghoul: " + pairsADay + " vs " + ghoulsADay);
        check(ghoulsADay < 0.6, "a wild ghoul is rare now: " + ghoulsADay + " a night");
        check(!AgentRules.spaced(AgentRules.ALONE - 1.0) && AgentRules.spaced(AgentRules.ALONE + 1.0) && AgentRules.spaced(Double.MAX_VALUE),
            "never near another pair");
        check(AgentRules.SPAWN_NEAREST > AgentRules.CLEAR_OF_PLAYERS && AgentRules.SPAWN_FARTHEST > AgentRules.SPAWN_NEAREST,
            "they turn up out of sight, never on top of anyone");
        check(AgentRules.PAIR_APART > 1.0 && AgentRules.PAIR_APART < 3.0, "the two side by side");
        // Every quinque turns up, the launcher least; a pair never carries two of a kind, nor two launchers.
        Map<Quinque, Integer> seen = new EnumMap<>(Quinque.class);
        Random random = new Random(5);
        for (int i = 0; i < 20000; i++) {
            Quinque first = AgentRules.kind(random.nextDouble());
            Quinque second = AgentRules.partnerKind(first, random.nextDouble());
            seen.merge(first, 1, Integer::sum);
            check(first != Quinque.DAWN && second != Quinque.DAWN, "regular investigators never carry Dawn");
            check(second != first, "a pair carries two different quinques (" + first + ")");
        }
        for (Quinque q : Quinque.values()) {
            if (q == Quinque.DAWN) continue;
            check(seen.getOrDefault(q, 0) > 1000, q + " is carried: " + seen.get(q));
        }
        check(seen.get(Quinque.LAUNCHER) < seen.get(Quinque.BLADE) && seen.get(Quinque.LAUNCHER) < seen.get(Quinque.KATANA), "the launcher least");
        check(AgentRules.kind(0.0) == Quinque.BLADE && AgentRules.kind(0.999999) == Quinque.LAUNCHER, "the rolls cover the table");
        // Really rare to get its case.
        check(AgentRules.dropChance(0) <= 0.03 && AgentRules.dropChance(0) > 0.0, "the case: really rare");
        check(AgentRules.dropChance(3) > AgentRules.dropChance(0) && AgentRules.dropChance(3) <= 0.06 && AgentRules.dropChance(99) == AgentRules.dropChance(3),
            "a little less rare with Looting, never common");
    }

    /** A ghoul is let be until their kagune is out in front of them; monsters only up close; wild ghouls on sight. */
    static void targets() {
        check(!AgentRules.revealed(true, false, 3.0, true), "a ghoul with their kagune in: just another face");
        check(!AgentRules.revealed(false, true, 3.0, true), "a human: never");
        check(AgentRules.revealed(true, true, 10.0, true), "a kagune out in sight: revealed");
        check(!AgentRules.revealed(true, true, 10.0, false), "unseen: not revealed");
        check(!AgentRules.revealed(true, true, AgentRules.REVEALED_RANGE + 1.0, true), "too far to see it: not revealed");
        check(AgentRules.tooClose(AgentRules.MOB_RANGE) && !AgentRules.tooClose(AgentRules.MOB_RANGE + 0.5), "a monster only when it comes too close");
        check(AgentRules.GHOUL_RANGE > AgentRules.MOB_RANGE * 2.0, "a wild ghoul is hunted from much further");
        check(AgentRules.MARKED >= 1200L && AgentRules.MARKED > AgentRules.GRUDGE, "a revealed ghoul is kept after a while, longer than a grudge");
        check(AgentRules.LEASH > AgentRules.REVEALED_RANGE, "a fight is followed past where it was seen");
    }

    /** Their weapons' reach, their strings, parries and dodges, their feet. */
    static void fighting() {
        for (Quinque q : Quinque.values()) {
            check(AgentRules.reach(q) > 2.0 && AgentRules.reach(q) < 5.0, q + ": a reach a body can swing at");
            check(AgentRules.lands(q) >= 2 && AgentRules.lands(q) < q.swing, q + ": its blow lands inside its swing");
            check(AgentRules.between(q) > 0, q + ": a breath between blows");
            for (int step = 0; step < 3; step++) {
                check(AgentRules.damage(q, step) > 0.0F && AgentRules.damage(q, step) <= q.attackDamage() * q.weight(step), q + " step " + step + ": hurts, no more than a player's");
            }
            check(AgentRules.damage(q, Quinque.FINISHER) >= AgentRules.damage(q, 0), q + ": the third blow the heaviest");
            check(AgentRules.parry(q) >= 0.0 && AgentRules.parry(q) < 0.75, q + ": parries often, never always");
        }
        check(AgentRules.reach(Quinque.SPEAR) > AgentRules.reach(Quinque.KATANA) && AgentRules.reach(Quinque.KATANA) > AgentRules.reach(Quinque.BLADE),
            "the spear reaches furthest");
        check(AgentRules.parry(Quinque.LAUNCHER) == 0.0, "the launcher can't parry a blow");
        check(AgentRules.parry(Quinque.KATANA) > AgentRules.parry(Quinque.MACE), "the katana is quicker to parry than the mace");
        check(AgentRules.SPEED > NpcRules.speed(0) && AgentRules.SPEED * AgentRules.FIGHT_PACE > NpcRules.speed(4), "fast: faster than a wild ghoul in a fight");
        check(AgentRules.FLIP_EVERY > NpcRules.FLIP_BUSY && AgentRules.DASH_EVERY > AgentRules.DASH_TICKS, "a flip or a dash finishes before the next");
        check(AgentRules.SHOOT_NEAREST > AgentRules.reach(Quinque.LAUNCHER) && AgentRules.SHOOT_FARTHEST < Quinque.SHARD_RANGE, "the launcher keeps its distance, in range");
        check(AgentRules.BURST * AgentRules.SHOT_GAP < AgentRules.BURST_GAP * 2, "bursts with a breath between");
        check(AgentRules.CALM > Quinque.UNFOLD * 4, "the case shuts a while after the fight");
        check(AgentRules.HEALTH > 20.0 && AgentRules.HEALTH < 80.0, "tougher than a player, not a boss");
    }

    /** The 32-bit clocks their moves ride on: right across a wrap, never overflowing, "never" long ago. */
    static void clocks() {
        check(AgentRules.since(1000L, 990) == 10L, "ten ticks ago");
        check(AgentRules.since(1000L, AgentRules.NEVER) > 1_000_000L, "never: long ago");
        long now = (1L << 32) + 5L;
        check(AgentRules.since(now, (int)((1L << 32) - 3L)) == 8L, "across the 32-bit wrap");
        check(AgentRules.since(Long.MAX_VALUE, (int)(Long.MAX_VALUE - 4L)) == 4L, "at the far end of time");
        check(AgentRules.since(100L, 104) == -4L, "a little ahead (a lagging client): negative, not huge");
    }

    /** Their run: none at a walk, all of it at a sprint; the legs and arms opposite; pitched forward, the head up. */
    static void running() {
        check(AgentPose.running(0.1) == 0.0F && AgentPose.running(0.4) == 1.0F && AgentPose.running(0.22) > 0.0F, "a walk is vanilla's, a sprint the run");
        RigPose still = AgentPose.run(1.0, 0.0F, AgentPose.Carry.CASE);
        check(still.isZero(), "no run: nothing on top of the walk");
        for (double phase = 0.0; phase < Math.PI * 2.0; phase += 0.3) {
            RigPose p = AgentPose.run(phase, 1.0F, AgentPose.Carry.CASE);
            check(p.lean > 0.2F && p.headX < 0.0F, "pitched forward, the head up (" + phase + ")");
            check(p.rightLegX * p.leftLegX <= 1.0E-6F || Math.abs(Math.sin(phase)) < 0.05, "the legs go opposite ways (" + phase + ")");
            check(Math.abs(p.rightLegX) <= 1.5F && Math.abs(p.leftLegX) <= 1.5F, "a long stride, never a split (" + phase + ")");
            check(p.leftArmX * p.leftLegX <= 1.0E-6F || Math.abs(Math.sin(phase)) < 0.2, "the free arm swings with the other leg (" + phase + ")");
            check(p.drop >= 0.0F && p.drop <= 1.0F, "a little bounce (" + phase + ")");
            check(p.armHold == 1.0F && p.legHold == 1.0F, "all the run at a sprint");
            RigPose blade = AgentPose.run(phase, 1.0F, AgentPose.Carry.BLADE);
            // 1.3.33: the blade (held off the forearm's line) trails level behind, its point clear of the ground.
            com.hex.kagune.control.client.animation.QuinqueHolds.Hold h = com.hex.kagune.control.client.animation.QuinqueHolds.of(Quinque.BLADE);
            double[] grip = com.hex.kagune.control.client.animation.QuinqueGrip.held(blade, h, false, standing(5.0));
            double[] tip = com.hex.kagune.control.client.animation.QuinqueGrip.held(blade, h, false, standing(31.0));
            check(tip[2] > grip[2] + 10.0 && tip[1] < 23.0, "a blade trailed behind, off the ground (" + phase + ")");
        }
        RigPose half = AgentPose.run(1.0, 0.5F, AgentPose.Carry.CASE);
        RigPose full = AgentPose.run(1.0, 1.0F, AgentPose.Carry.CASE);
        check(Math.abs(half.lean - full.lean / 2.0F) < 1.0E-5F && half.legHold == 0.5F, "blended in by how fast it goes");
        // A two-handed stance carried on the run, lowered (both hands on it); a parry snaps in and out; a dash leans into it.
        RigPose stance = com.hex.kagune.control.client.animation.QuinquePose.hold(Quinque.KATANA);
        RigPose carried = AgentPose.run(0.7, 1.0F, AgentPose.Carry.STANCE);
        AgentPose.stanceRun(carried, Quinque.KATANA, 0.7, 1.0F);
        double[] held = com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, false);
        double[] low = com.hex.kagune.control.client.animation.QuinqueGrip.fist(carried, true, false);
        float miss = com.hex.kagune.control.client.animation.QuinqueGrip.reach(carried, Quinque.KATANA, false);
        check(low[1] > held[1] + 1.0 && Math.abs(miss) < 2.5F, "the katana kept in both hands on the run, lowered (" + miss + ")");
        for (Quinque q : Quinque.values()) {
            RigPose start = AgentPose.parry(q, 0.0);
            RigPose mid = AgentPose.parry(q, 0.45);
            double[] from = com.hex.kagune.control.client.animation.QuinqueGrip.fist(start, true, false);
            double[] up = com.hex.kagune.control.client.animation.QuinqueGrip.fist(mid, true, false);
            check(up[1] < from[1] - 2.0 && mid.twist < start.twist, q + ": the parry whips the weapon up and across");
            double[] back = com.hex.kagune.control.client.animation.QuinqueGrip.fist(AgentPose.parry(q, 0.999), true, false);
            check(Math.abs(back[1] - from[1]) < 0.3, q + ": and back");
            if (q.twoHanded) {
                float off = com.hex.kagune.control.client.animation.QuinqueGrip.reach(mid, q, false);
                check(Math.abs(off) < 3.0F && mid.leftArmHold == 1.0F, q + ": both hands on a two-handed one through the parry (" + off + ")");
            }
        }
        RigPose dash = new RigPose();
        AgentPose.dash(dash, 0, 0.4);
        check(dash.lean > 0.3F && dash.drop > 1.0F && dash.legHold > 0.9F, "a dash forward: low, leaning in");
        RigPose side = new RigPose();
        AgentPose.dash(side, 2, 0.4);
        RigPose other = new RigPose();
        AgentPose.dash(other, 3, 0.4);
        check(side.side > 0.0F && other.side < 0.0F && side.rightLegZ > 0.0F, "a side step leans its way, the legs apart");
        RigPose over = new RigPose();
        AgentPose.dash(over, 0, 1.0);
        check(over.isZero(), "a dash done leaves nothing");
    }

    // ---------------------------------------------------------------- 1.3.28

    /** One holds the front, the other goes round wide to the back, never across the one in front; they swap when the prey turns. */
    static void together() {
        // Always one of each, the launcher in front.
        for (Quinque a : Quinque.values()) {
            for (Quinque b : Quinque.values()) {
                if (a == b) {
                    continue;
                }
                for (boolean swapped : new boolean[] {false, true}) {
                    boolean leadersFront = AgentRules.front(true, a, b, swapped);
                    boolean followersFront = AgentRules.front(false, b, a, swapped);
                    check(leadersFront != followersFront, a + "/" + b + (swapped ? " swapped" : "") + ": one in front, one round the back");
                    if (a.ranged) {
                        check(leadersFront, a + " leading: the launcher holds the front from range");
                    } else if (b.ranged) {
                        check(!leadersFront, b + " following: the launcher holds the front, the leader goes round");
                    } else {
                        check(leadersFront != swapped, a + "/" + b + ": the leader in front until they swap");
                    }
                }
            }
        }
        // Round: the short way, on its own side of the one in front, a step at a time, wide of the prey, and in from behind.
        double front = 0.7;
        double goal = AgentRules.back(front);
        check(Math.abs(AgentRules.wrap(goal - front)) > Math.PI - 1.0E-9, "the back is straight across from the front");
        for (double start : new double[] {0.05, -0.05, 0.4, -0.4, 1.2, -1.2, 2.0, -2.5, 0.0}) {
            double at = AgentRules.wrap(front + start);
            double way = AgentRules.sideOf(at, front, -1.0);
            check(start == 0.0 ? way == -1.0 : way == Math.signum(start), "on its own side of the one in front (" + start + ")");
            int steps = 0;
            double left = Math.abs(AgentRules.wrap(goal - at));
            while (!AgentRules.behind(at, goal) && steps < 8) {
                double next = AgentRules.round(at, goal, AgentRules.sideOf(at, front, -1.0));
                double step = Math.abs(AgentRules.wrap(next - at));
                check(step <= AgentRules.ROUND_STEP + 1.0E-9 && step > 0.1, "a step round at a time (" + start + ")");
                check(Math.abs(AgentRules.wrap(next - front)) >= Math.abs(AgentRules.wrap(at - front)) - 1.0E-9,
                    "always further round from the one in front (" + start + ")");
                check(AgentRules.sideOf(next, front, way) == way || AgentRules.behind(next, goal), "never across the one in front (" + start + ")");
                double now = Math.abs(AgentRules.wrap(goal - next));
                check(now < left, "closer to the prey's back each step (" + start + ")");
                left = now;
                at = next;
                steps++;
            }
            check(AgentRules.behind(at, goal) && steps <= 4, "round to the back in a few steps (" + start + ": " + steps + ")");
        }
        check(AgentRules.behind(AgentRules.wrap(goal + AgentRules.BEHIND * 0.9), goal) && !AgentRules.behind(AgentRules.wrap(goal + AgentRules.BEHIND * 1.2), goal),
            "behind: within its angle of the back");
        check(AgentRules.kept(AgentRules.wrap(goal + AgentRules.BEHIND * 1.5), goal) && !AgentRules.kept(front, goal) && AgentRules.BEHIND_KEPT > AgentRules.BEHIND
            && AgentRules.BEHIND_KEPT < Math.PI / 2.0, "once in from behind it stays in a while, never from the front");
        for (Quinque q : Quinque.values()) {
            check(AgentRules.ROUND_RADIUS * Math.cos(AgentRules.ROUND_STEP / 2.0) > AgentRules.reach(q) + 0.3,
                q + ": going round, even cutting between its spots, it stays out of its own reach of the prey");
        }
        check(AgentRules.ROUND_RADIUS * Math.cos(AgentRules.ROUND_STEP / 2.0) >= 4.5, "wide of a kagune's reach as it goes round");
        // The one in front waits for the other, not for ever; it swaps when the prey turns on the one behind.
        check(AgentRules.holding(false, 0L) && AgentRules.holding(false, AgentRules.HOLD_TICKS - 1L), "the front holds while the other goes round");
        check(!AgentRules.holding(true, 5L), "and goes in once the other is behind");
        check(!AgentRules.holding(false, AgentRules.HOLD_TICKS), "or once it has waited long enough");
        check(AgentRules.HOLD_TICKS >= 40 && AgentRules.HOLD_TICKS <= 100 && AgentRules.HOLD_OFF > 0.5 && AgentRules.HOLD_OFF < 2.0,
            "squared up just out of reach, for a few seconds at most");
        double frontAt = 0.0;
        double backAt = Math.PI;
        check(AgentRules.turned(Math.PI - 0.2, frontAt, backAt), "the prey turned on the one behind: they swap (after a while)");
        check(!AgentRules.turned(0.1, frontAt, backAt), "facing the one in front: no swap");
        check(!AgentRules.turned(Math.PI / 2.0, frontAt, backAt), "looking off to a side: no swap");
        check(AgentRules.SWAP_AFTER >= 20 && AgentRules.NEW_FIGHT > 20, "no swapping on a glance; a new fight sets the sides again");
        check(Math.abs(AgentRules.wrap(3.0 * Math.PI) - Math.PI) < 1.0E-9 || Math.abs(AgentRules.wrap(3.0 * Math.PI) + Math.PI) < 1.0E-9,
            "angles wrap");
        check(AgentRules.wrap(Double.NaN) == 0.0, "a broken angle is none");
    }

    /**
     * The case carried by the handle: its carried frame (the model JSON's own hand transform) through vanilla's ItemInHandLayer and
     * ItemTransform, on the arm AgentPose gives it: the handle bar in the fist, the case hanging plumb with its latches out and its
     * length along the stride, clear of the leg, under any lean of the body; only an investigator's case is ever drawn in that frame.
     */
    static void carrying() throws IOException {
        // Which frame: the carried ones for an investigator's shut case alone.
        check(AgentPose.caseFrame(0.0F, true, false) == AgentPose.CARRIED_WIDE && AgentPose.caseFrame(0.0F, true, true) == AgentPose.CARRIED_SLIM,
            "an investigator's shut case: its carried frame for its arms");
        check(AgentPose.caseFrame(0.0F, false, false) == 0.0F && AgentPose.caseFrame(0.1F, false, true) == 0.0F && AgentPose.caseFrame(0.15F, false, false) == 0.0F,
            "a player's case is never drawn in an investigator's frame");
        check(AgentPose.caseFrame(0.05F, true, true) == 0.0F, "an investigator's case starting to open: the plain frame (its arms in the opening pose)");
        for (float out = AgentPose.OPENING_FRAME; out <= 1.0F; out += 0.1F) {
            check(AgentPose.caseFrame(out, true, false) == out && AgentPose.caseFrame(out, false, false) == out, "opening: its opening frames (" + out + ")");
        }
        check(AgentPose.CARRIED_WIDE > 0.0F && AgentPose.CARRIED_WIDE < AgentPose.CARRIED_SLIM && AgentPose.CARRIED_SLIM < AgentPose.OPENING_FRAME,
            "the carried frames below the first opening one");
        for (Quinque q : Quinque.values()) {
            String item = Files.readString(RESOURCES.resolve("assets/hexkagune/models/item/" + q.caseId + ".json"));
            check(item.matches("(?s).*\\{\\s*\"hexkagune:deploy\"\\s*:\\s*" + AgentPose.CARRIED_WIDE + "\\s*}\\s*,\\s*\"model\"\\s*:\\s*\"hexkagune:item/quinque/"
                + q.model + "_case_carried\".*"), q + ": its case's wide-armed carried frame at the value AgentPose gives");
            check(item.matches("(?s).*\\{\\s*\"hexkagune:deploy\"\\s*:\\s*" + AgentPose.CARRIED_SLIM + "\\s*}\\s*,\\s*\"model\"\\s*:\\s*\"hexkagune:item/quinque/"
                + q.model + "_case_carried_slim\".*"), q + ": its slim-armed one too");
            check(item.matches("(?s).*\\{\\s*\"hexkagune:deploy\"\\s*:\\s*" + AgentPose.OPENING_FRAME + "\\s*}.*"), q + ": its first opening frame where AgentPose expects it");
            for (boolean slim : new boolean[] {false, true}) {
                double[] display = display(q.model + (slim ? "_case_carried_slim" : "_case_carried"));
                check(display != null, q + (slim ? " slim" : " wide") + ": its carried frame has a hand transform");
                if (display == null) {
                    continue;
                }
                String what = q + (slim ? " (slim arms)" : " (wide arms)");
                // Standing straight: right in the fist, plumb.
                RigPose straight = new RigPose();
                AgentPose.carry(straight, 0.0F);
                Hang hang = hang(straight, slim, display);
                check(hang.gripOff < 0.35, what + ": the handle bar in the middle of the fist (" + hang.gripOff + " px off)");
                check(hang.tilt < Math.toRadians(1.0), what + ": the case hangs plumb (" + Math.toDegrees(hang.tilt) + " deg)");
                check(hang.out > Math.cos(Math.toRadians(3.0)), what + ": its latches face out");
                check(Math.abs(hang.along) > Math.cos(Math.toRadians(3.0)), what + ": its length along the stride");
                double leg = slim ? 2.0 * (1.0 + (FemaleBody.HIPS - 1.0) * AgentRules.FEMALE_LEGS) - 0.1 + 2.0 * (1.0 + (FemaleBody.HIPS - 1.0) * AgentRules.FEMALE_LEGS) : 4.0;
                check(hang.inner > leg + 0.3, what + ": clear of the leg (" + hang.inner + " px out, the leg " + leg + ")");
                check(hang.bottom > 3.0 && hang.top < hang.fistY, what + ": below the fist, off the ground (" + hang.bottom + ".." + hang.top + ")");
                // Leaning back at its ease, pitched into its run: the case still hangs from the fist as the arm does.
                RigPose idle = new RigPose();
                AgentPose.idle(idle, 37.0, 5, 1.0F);
                AgentPose.carry(idle, 0.0F);
                check(hang(idle, slim, display).tilt < Math.toRadians(2.0), what + ": leaning back, the case still hangs plumb");
                for (double phase = 0.0; phase < Math.PI * 2.0; phase += 0.5) {
                    RigPose run = AgentPose.run(phase, 1.0F, AgentPose.Carry.CASE);
                    float sway = AgentPose.sway(phase, 1.0F, 1.0F);
                    AgentPose.carry(run, sway);
                    Hang moving = hang(run, slim, display);
                    check(Math.abs(moving.tilt - Math.abs(sway)) < Math.toRadians(2.5), what + ": on the run it swings with the arm, the lean taken out ("
                        + Math.toDegrees(moving.tilt) + " vs " + Math.toDegrees(sway) + ")");
                    check(moving.inner > leg, what + ": clear of the leg on the run");
                }
            }
        }
        // The swing: none standing, small walking, a little more running; always against the right leg.
        check(AgentPose.sway(1.0, 0.0F, 0.0F) == 0.0F, "standing: no swing");
        for (double phase = 0.0; phase < Math.PI * 2.0; phase += 0.25) {
            check(Math.abs(AgentPose.sway(phase, 1.0F, 0.0F)) <= 0.1F + 1.0E-6F && Math.abs(AgentPose.sway(phase, 1.0F, 1.0F)) <= 0.18F + 1.0E-6F,
                "a heavy case swings little (" + phase + ")");
            RigPose run = AgentPose.run(phase, 1.0F, AgentPose.Carry.CASE);
            check(AgentPose.sway(phase, 1.0F, 1.0F) * run.rightLegX <= 1.0E-6F, "the case's arm swings against its leg (" + phase + ")");
        }
    }

    /** Standing about: leaning back, the weight on one leg, breathing, a glance now and then; none of it once it walks. */
    static void idling() {
        check(AgentPose.still(0.0) == 1.0F && AgentPose.still(0.1) == 0.0F && AgentPose.still(0.04) > 0.0F && AgentPose.still(0.04) < 1.0F,
            "idle standing, none at a walk, blended between");
        float minLean = Float.MAX_VALUE;
        float maxLean = -Float.MAX_VALUE;
        float maxGlance = 0.0F;
        for (double age = 0.0; age < 600.0; age += 3.0) {
            RigPose p = new RigPose();
            AgentPose.idle(p, age, 12345, 1.0F);
            minLean = Math.min(minLean, p.lean);
            maxLean = Math.max(maxLean, p.lean);
            maxGlance = Math.max(maxGlance, Math.abs(p.headY));
            check(p.lean < -0.05F && p.lean > -0.2F, "leaning back a little (" + age + ")");
            check(p.legHold == 1.0F && p.leftArmHold == 1.0F, "its legs and its free arm are the idle's");
            check(Math.abs(p.rightLegX) < 0.1F && p.leftLegX < -0.05F && p.leftLegZ < 0.0F, "its weight on the right leg, the left loose and out");
            check(Math.abs(p.rightShoulderY) <= 0.2F + 1.0E-6F, "a breath, not a heave");
            check(p.headX == 0.0F && Math.abs(p.headY) <= 0.38F + 1.0E-6F, "its eyes still where it looks, now and then off to a side");
        }
        check(maxLean - minLean > 0.015F && maxLean - minLean < 0.03F, "it breathes: " + (maxLean - minLean));
        check(maxGlance > 0.3F, "it looks off to a side now and then: " + maxGlance);
        RigPose a = new RigPose();
        RigPose b = new RigPose();
        AgentPose.idle(a, 100.0, 1, 1.0F);
        AgentPose.idle(b, 100.0, 1 + 64 * 7 + 13, 1.0F);
        check(a.lean != b.lean || a.headY != b.headY, "two of them don't breathe and look about as one");
        RigPose none = new RigPose();
        AgentPose.idle(none, 100.0, 1, 0.0F);
        check(none.isZero(), "walking: no idle");
    }

    /** 2.0.49: the White Reaper's tactics: the play he makes on what he reads, and his soar's flight. */
    static void reaperTactics() {
        com.hex.kagune.control.npc.ReaperTactics.Read calm = read(8.0, false, false, false, false, 1.0, 1.0, false, true, true, true, true, false);
        check(com.hex.kagune.control.npc.ReaperTactics.choose(calm, 0.99) == com.hex.kagune.control.npc.ReaperTactics.Play.NONE,
            "nothing to read: the usual moves");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(4.0, false, false, false, true, 1.0, 1.0, false, true, true, true, true, false), 0.99)
            == com.hex.kagune.control.npc.ReaperTactics.Play.VAULT, "a guard up close: over it");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(8.0, false, false, false, true, 1.0, 1.0, false, true, true, true, true, false), 0.99)
            != com.hex.kagune.control.npc.ReaperTactics.Play.VAULT, "not from too far off");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(12.0, true, false, false, false, 1.0, 1.0, false, true, true, true, true, false), 0.1)
            == com.hex.kagune.control.npc.ReaperTactics.Play.SKY_SHOT, "a quarry in the air, far enough: shot where it can't step aside");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(7.0, false, true, false, false, 1.0, 1.0, false, true, true, true, true, false), 0.99)
            == com.hex.kagune.control.npc.ReaperTactics.Play.PUNISH, "stunned: in at once");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(6.0, false, false, true, false, 1.0, 1.0, false, true, true, true, true, false), 0.99)
            == com.hex.kagune.control.npc.ReaperTactics.Play.PUNISH, "a blow spent on nothing: in at once");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(2.0, false, true, false, false, 1.0, 1.0, false, true, true, true, true, false), 0.99)
            == com.hex.kagune.control.npc.ReaperTactics.Play.NONE, "already in reach: his string, not a lunge");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(9.0, false, false, false, false, 0.2, 1.0, false, true, true, true, true, false), 0.99)
            == com.hex.kagune.control.npc.ReaperTactics.Play.PUNISH, "nearly dead: he goes in for it");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(3.0, false, false, false, false, 1.0, 0.3, false, true, true, true, true, false), 0.99)
            == com.hex.kagune.control.npc.ReaperTactics.Play.SOAR, "hurt and pressed: up and away, and the volley down");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(2.5, false, false, false, false, 1.0, 1.0, true, true, true, true, true, false), 0.3)
            == com.hex.kagune.control.npc.ReaperTactics.Play.SOAR, "his string landed: often a soar after it");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(6.0, false, false, false, false, 1.0, 1.0, false, true, true, true, true, false), 0.01)
            == com.hex.kagune.control.npc.ReaperTactics.Play.SOAR, "now and then a soar to keep them guessing");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(3.0, false, true, false, true, 0.1, 0.1, true, true, true, true, true, true), 0.0)
            == com.hex.kagune.control.npc.ReaperTactics.Play.NONE, "in the middle of something: nothing new");
        com.hex.kagune.control.npc.ReaperTactics.Read air = new com.hex.kagune.control.npc.ReaperTactics.Read(3.0, 3.0, true, false, true, false,
            false, 1.0, 0.3, true, false, true, true, true, true, false);
        check(com.hex.kagune.control.npc.ReaperTactics.choose(air, 0.0) == com.hex.kagune.control.npc.ReaperTactics.Play.NONE,
            "off his feet: no new play");
        check(com.hex.kagune.control.npc.ReaperTactics.choose(read(3.0, false, false, false, false, 1.0, 0.3, false, true, true, true, false, false), 0.0)
            != com.hex.kagune.control.npc.ReaperTactics.Play.SOAR, "no soar before it is ready again");
        // The soar's flight, as a mob flies (moved by its speed, then gravity 0.08 and drag: 0.98 up and down, 0.91 along, in the air).
        double y = 0.0;
        double along = 0.0;
        double vy = 0.0;
        double va = 0.0;
        int apexTick = -1;
        double apex = 0.0;
        for (int tick = 0; tick < 60; tick++) {
            if (tick < com.hex.kagune.control.npc.ReaperTactics.RAMP) {
                double[] launch = com.hex.kagune.control.npc.ReaperTactics.soar(tick);
                va = launch[0];
                vy = launch[1];
            } else if (apexTick < 0 && vy <= com.hex.kagune.control.npc.ReaperTactics.APEX) {
                apexTick = tick;
                apex = y;
            }
            if (apexTick >= 0 && tick < apexTick + com.hex.kagune.control.npc.ReaperTactics.AIR_AIM) {
                vy = Math.max(vy, com.hex.kagune.control.npc.ReaperTactics.HANG_FALL);
                va *= com.hex.kagune.control.npc.ReaperTactics.HANG_DRIFT;
            }
            y += vy;
            along += va;
            vy = (vy - 0.08) * 0.98;
            va *= 0.91;
            if (y <= 0.0 && tick > 2) {
                break;
            }
        }
        check(apex > 4.0 && apex < 6.5, "it climbs about two and a half times a flip (" + apex + " blocks)");
        double spun = com.hex.kagune.control.npc.ReaperTactics.SPIN_FROM + com.hex.kagune.control.npc.ReaperTactics.SPIN_TICKS;
        check(apexTick > 8 && Math.abs(apexTick - spun) <= 4.0, "turned over by the top of it, where he aims (top " + apexTick + ", turned " + spun + ")");
        check(along > 4.0 && along < 10.0, "back far enough for the volley (" + along + " blocks)");
        double[] led = com.hex.kagune.control.npc.ReaperTactics.lead(new double[] {0, 0, 0}, new double[] {0.2, 0, 0}, 15.0, 1.5);
        check(Math.abs(led[0] - 0.2 * com.hex.kagune.control.npc.ReaperTactics.LEAD * 10.0) < 1.0E-9 && led[2] == 0.0,
            "a moving quarry led along its way");
        double[] still = com.hex.kagune.control.npc.ReaperTactics.lead(new double[] {1, 2, 3}, new double[] {0, 0, 0}, 15.0, 1.5);
        check(still[0] == 1 && still[1] == 2 && still[2] == 3, "a still one shot where it stands");
    }

    static com.hex.kagune.control.npc.ReaperTactics.Read read(double distance, boolean airborne, boolean stunned, boolean whiffed, boolean guarding,
                                                            double preyHealth, double ownHealth, boolean afterString, boolean flipReady,
                                                            boolean dashReady, boolean shotReady, boolean soarReady, boolean busy) {
        return new com.hex.kagune.control.npc.ReaperTactics.Read(distance, 3.0, true, airborne, stunned, whiffed, guarding, preyHealth, ownHealth,
            afterString, true, flipReady, dashReady, shotReady, soarReady, busy);
    }

    /** 2.0.45: the White Reaper's build: his upper torso broader than his waist, his chest out in front of his belly, in his own skin. */
    static void reaperBuild() {
        double[][] rows = com.hex.kagune.control.npc.ReaperBuild.PROFILE;
        double[] top = rows[1];
        double[] waist = rows[rows.length - 1];
        double widest = 0.0;
        double furthest = 0.0;
        for (double[] row : rows) {
            widest = Math.max(widest, row[1]);
            furthest = Math.max(furthest, row[2]);
        }
        check(widest > 4.4 && widest < 5.0, "his upper torso a little broader than a player's (" + widest + " of 4 pixels a side)");
        check(furthest - 2.0 > 0.7 && furthest - 2.0 < 1.3, "his chest a little out in front of his belly (" + (furthest - 2.0) + " pixels)");
        check(top[1] > waist[1] + 0.5 && top[2] > waist[2] + 0.7, "broader and deeper up top than at the waist (a V)");
        check(waist[1] < 4.0 && waist[2] < 2.0 && waist[3] < 2.0, "at the waist it is just inside the torso, which takes over there");
        boolean down = true;
        for (int i = 1; i < rows.length; i++) {
            down &= rows[i][0] > rows[i - 1][0];
        }
        check(down && rows[0][0] < 0.0 && rows[rows.length - 1][0] < 12.0, "its rows run down the torso from just over its top");
        check(4.0 + com.hex.kagune.control.npc.ReaperBuild.ARM_OUT >= widest, "his arms hang clear of it");
        // 2.0.46: its back, for what lies on a man's back (the Rinkaku mark).
        check(com.hex.kagune.control.npc.ReaperBuild.back(3.0, false) == 2.35 && com.hex.kagune.control.npc.ReaperBuild.back(3.0, true) == 2.6,
            "its back up the chest, and its jacket layer's");
        check(com.hex.kagune.control.npc.ReaperBuild.back(9.5, true) == 0.0 && com.hex.kagune.control.npc.ReaperBuild.back(-1.0, true) == 0.0
            && com.hex.kagune.control.npc.ReaperBuild.back(Double.NaN, true) == 0.0, "none below his waist nor over his shoulders");
        check(com.hex.kagune.control.npc.ReaperBuild.back(8.5, true) < 2.25, "at the waist it is back inside the torso's jacket layer");
        for (boolean jacket : new boolean[] {false, true}) {
            int[] count = new int[1];
            boolean[] ok = {true, true, true};
            com.hex.kagune.control.npc.ReaperBuild.chest(jacket, (corners, uv, normal) -> {
                count[0]++;
                for (int k = 0; k < 4; k++) {
                    double u = uv[2 * k];
                    double v = uv[2 * k + 1];
                    ok[0] &= u >= 16.0 && u <= 40.0 && v >= (jacket ? 32.0 : 16.0) && v <= (jacket ? 48.0 : 32.0);
                }
                double[] mid = {(corners[0][0] + corners[2][0]) / 2, (corners[0][1] + corners[2][1]) / 2, (corners[0][2] + corners[2][2]) / 2};
                double l = Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
                ok[1] &= Math.abs(l - 1.0) < 1.0E-9;
                // Outward: away from the torso's middle line (a top's up, model -y).
                double out = normal[0] * mid[0] + normal[2] * mid[2] + (Math.abs(normal[1]) > 0.9 ? -normal[1] : 0.0);
                ok[2] &= out > 0.0;
            });
            String layer = jacket ? "its jacket layer" : "the chest";
            check(count[0] == (rows.length - 1) * 4 + 1, layer + ": four sides a band, and its top");
            check(ok[0], layer + ": painted from the torso's own skin (and its jacket layer's)");
            check(ok[1] && ok[2], layer + ": every face turned outward");
        }
    }

    /** 2.0.43: the White Reaper standing about: leaning well back, hip cocked, his free hand in his pocket, his eyes on you, barely moving. */
    static void reaperIdling() {
        float minLean = Float.MAX_VALUE;
        float maxLean = -Float.MAX_VALUE;
        for (double age = 0.0; age < 600.0; age += 3.0) {
            RigPose reaper = new RigPose();
            RigPose other = new RigPose();
            AgentPose.reaperIdle(reaper, age, 12345, 1.0F);
            AgentPose.idle(other, age, 12345, 1.0F);
            minLean = Math.min(minLean, reaper.lean);
            maxLean = Math.max(maxLean, reaper.lean);
            check(reaper.lean < other.lean - 0.1F && reaper.lean > -0.35F, "he leans back further than any investigator (" + age + ")");
            check(reaper.side > other.side && reaper.twist < other.twist, "his hip cocked, his chest turned open");
            check(reaper.headY == 0.0F && reaper.headX > 0.0F && reaper.headX < -reaper.lean, "no glancing about; his chin a touch up");
            check(reaper.legHold == 1.0F && reaper.leftArmHold == 1.0F && reaper.leftLegX < -0.2F && reaper.leftLegY < -0.3F,
                "his weight on his right leg, the left loose in front");
            check(reaper.leftElbow == 0.0F && reaper.leftKnee == 0.0F && reaper.leftArmY > 0.3F && reaper.leftArmZ + reaper.side > 0.2F,
                "his free arm straight in to his pocket (an investigator's model bends no elbow)");
        }
        check(maxLean - minLean > 0.005F && maxLean - minLean < 0.02F, "a slow breath, hardly moving: " + (maxLean - minLean));
        RigPose none = new RigPose();
        AgentPose.reaperIdle(none, 100.0, 1, 0.0F);
        check(none.isZero(), "walking: no idle");
        RigPose half = new RigPose();
        AgentPose.reaperIdle(half, 100.0, 1, 0.5F);
        RigPose full = new RigPose();
        AgentPose.reaperIdle(full, 100.0, 1, 1.0F);
        check(Math.abs(half.side - full.side * 0.5F) < 1.0E-6F, "it blends in and out");
    }

    /** Where its case hangs for a pose: in model pixels of the entity (x its right, y up from its feet, z its back). */
    record Hang(double gripOff, double tilt, double out, double along, double inner, double bottom, double top, double fistY) {
    }

    /** The case's carried frame ({@code display}: rotation, translation, scale) on {@code p}'s right arm, as vanilla draws it. */
    static Hang hang(RigPose p, boolean slim, double[] display) {
        float armY = slim ? 2.5F : 2.0F;
        float[] pivot = p.transform("rightArm", RigPose.POSITION, -5.0F, armY, 0.0F);
        float[] turn = p.transform("rightArm", RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        double[] body = mul(scale(-1, -1, 1), translate(0, -1.501, 0));
        double[] arm = mul(body, mul(translate(pivot[0] / 16.0, pivot[1] / 16.0, pivot[2] / 16.0), mul(rz(turn[2]), mul(ry(turn[1]), rx(turn[0])))));
        double[] item = mul(body, mul(translate((pivot[0] + (slim ? 0.5 : 0.0)) / 16.0, pivot[1] / 16.0, pivot[2] / 16.0),
            mul(rz(turn[2]), mul(ry(turn[1]), rx(turn[0])))));
        item = mul(item, mul(rx(Math.toRadians(-90.0)), mul(ry(Math.toRadians(180.0)), translate(1.0 / 16.0, 0.125, -0.625))));
        item = mul(item, translate(display[3] / 16.0, display[4] / 16.0, display[5] / 16.0));
        item = mul(item, mul(rx(Math.toRadians(display[0])), mul(ry(Math.toRadians(display[1])), rz(Math.toRadians(display[2])))));
        item = mul(item, mul(scale(display[6], display[6], display[6]), mul(translate(-0.5, -0.5, -0.5), scale(1.0 / 16.0, 1.0 / 16.0, 1.0 / 16.0))));
        double[] fist = at(arm, (slim ? -0.5 : -1.0) / 16.0, 9.0 / 16.0, 0.0);
        double[] grip = at(item, 8.0, 13.95, 8.0);
        double[] up = unit(sub(at(item, 8.0, 12.0, 8.0), at(item, 8.0, 2.0, 8.0)));
        double[] out = unit(sub(at(item, 8.0, 7.0, 9.5), at(item, 8.0, 7.0, 6.5)));
        double[] along = unit(sub(at(item, 15.0, 7.0, 8.0), at(item, 1.0, 7.0, 8.0)));
        double inner = Double.MAX_VALUE;
        double bottom = Double.MAX_VALUE;
        double top = -Double.MAX_VALUE;
        for (double x : new double[] {1.0, 15.0}) {
            for (double y : new double[] {1.8, 12.0}) {
                for (double z : new double[] {6.35, 9.65}) {
                    double[] c = at(item, x, y, z);
                    inner = Math.min(inner, c[0] * 16.0);
                    bottom = Math.min(bottom, c[1] * 16.0);
                    top = Math.max(top, c[1] * 16.0);
                }
            }
        }
        double gripOff = Math.sqrt(sq(grip[0] - fist[0]) + sq(grip[1] - fist[1]) + sq(grip[2] - fist[2])) * 16.0;
        return new Hang(gripOff, Math.acos(Math.max(-1.0, Math.min(1.0, up[1]))), out[0], along[2], inner, bottom, top, at(arm, 0.0, 10.0 / 16.0, 0.0)[1] * 16.0 + 2.0);
    }

    /** The case model's third person hand transform (rotation, translation, scale x) from its JSON, or null. */
    static double[] display(String model) throws IOException {
        Path file = RESOURCES.resolve("assets/hexkagune/models/item/quinque/" + model + ".json");
        if (!Files.exists(file)) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"thirdperson_righthand\"\\s*:\\s*\\{\\s*\"rotation\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,"
            + "\\s*\"translation\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,\\s*\"scale\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(Files.readString(file));
        if (!m.find()) {
            return null;
        }
        double[] out = new double[7];
        int i = 0;
        for (int g = 1; g <= 3; g++) {
            String[] parts = m.group(g).split(",");
            for (int k = 0; k < (g == 3 ? 1 : 3); k++) {
                out[i++] = Double.parseDouble(parts[k].trim());
            }
        }
        return out;
    }

    // 4x4 matrices, row major.
    static double[] mul(double[] a, double[] b) {
        double[] out = new double[16];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                double v = 0.0;
                for (int k = 0; k < 4; k++) {
                    v += a[r * 4 + k] * b[k * 4 + c];
                }
                out[r * 4 + c] = v;
            }
        }
        return out;
    }

    static double[] translate(double x, double y, double z) {
        return new double[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1};
    }

    static double[] scale(double x, double y, double z) {
        return new double[] {x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1};
    }

    static double[] rx(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[] {1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1};
    }

    static double[] ry(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[] {c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1};
    }

    static double[] rz(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[] {c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    static double[] at(double[] m, double x, double y, double z) {
        return new double[] {m[0] * x + m[1] * y + m[2] * z + m[3], m[4] * x + m[5] * y + m[6] * z + m[7], m[8] * x + m[9] * y + m[10] * z + m[11]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] unit(double[] v) {
        double n = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return n == 0.0 ? v : new double[] {v[0] / n, v[1] / n, v[2] / n};
    }

    static double sq(double v) {
        return v * v;
    }

    /** A point on a standing quinque's line at height {@code y}, where its -45 degree turn puts it (model pixels). */
    static float[] standing(double y) {
        double k = Math.sqrt(0.5) * (y - 8.0);
        return new float[] {(float)(8.0 + k), (float)(8.0 + k), 8.0F};
    }

    /**
     * 1.3.70: ranks by chance, the lowest most common; the higher, the more health and the faster; a pair's leader the better. 2.0.17: Rank
     * 0 to 10, each harder (health, speed, armour, power, a shorter rest); 8, 9 and 10 rare, dangerous, with a lot of health.
     */
    static void ranks() {
        AgentRules.Rank[] all = AgentRules.Rank.values();
        check(all.length == 11 && all[0].level() == 0 && all[10].level() == 10, "Rank 0 to Rank 10");
        int[] seen = new int[all.length];
        for (int i = 0; i < 100000; i++) {
            seen[AgentRules.Rank.roll(i / 100000.0).ordinal()]++;
        }
        boolean falling = true;
        for (int k = 1; k < all.length; k++) {
            falling &= seen[k] < seen[k - 1] && seen[k] > 0;
            check(all[k].health > all[k - 1].health && all[k].speed > all[k - 1].speed && all[k].armor > all[k - 1].armor
                && all[k].power > all[k - 1].power && all[k].pace <= all[k - 1].pace, all[k] + ": harder in every way than " + all[k - 1]);
        }
        check(falling && seen[0] > 15000, "the lowest rank the most common, each higher rarer, every one possible");
        int dangerous = seen[8] + seen[9] + seen[10];
        check(dangerous > 1000 && dangerous < 6000, "8, 9 and 10 rare (about 4 in 100): " + dangerous);
        check(AgentRules.Rank.RANK_8.dangerous() && !AgentRules.Rank.RANK_7.dangerous() && AgentRules.Rank.RANK_8.health >= 150.0
            && AgentRules.Rank.RANK_10.health >= 250.0 && AgentRules.Rank.RANK_8.power >= 2.0F && AgentRules.Rank.RANK_10.power >= 3.0F
            && AgentRules.Rank.RANK_10.pace <= 0.6, "the top three really dangerous: a lot of health, two to three times as hard, far quicker");
        check(AgentRules.Rank.roll(0.0) == AgentRules.Rank.RANK_0 && AgentRules.Rank.roll(1.0) == AgentRules.Rank.RANK_10
            && AgentRules.Rank.roll(-3.0) == AgentRules.Rank.RANK_0, "the ends of the roll");
        boolean leads = true;
        for (double a = 0.0; a < 1.0; a += 0.07) {
            for (double b = 0.0; b < 1.0; b += 0.07) {
                leads &= AgentRules.Rank.of(true, a, b).ordinal() >= AgentRules.Rank.of(false, a, b).ordinal();
            }
        }
        check(leads, "with the same two chances, the one leading never ranks below the other");
        check(AgentRules.Rank.byOrdinal(99) == AgentRules.Rank.RANK_0 && AgentRules.Rank.byOrdinal(10) == AgentRules.Rank.RANK_10, "saved ranks read back");
        check(AgentRules.Rank.fromOld(0) == AgentRules.Rank.RANK_1 && AgentRules.Rank.fromOld(5) == AgentRules.Rank.RANK_8
            && AgentRules.Rank.fromOld(-1) == AgentRules.Rank.RANK_0, "agents saved with the old six ranks keep their place");
        // 2.0.20: a pair's leader (its superior) always the stronger, whatever the two rolled and whichever woke first.
        boolean superiorStronger = true;
        boolean ranked = true;
        for (AgentRules.Rank l : all) {
            if (l == AgentRules.Rank.RANK_10) continue;
            for (AgentRules.Rank p : all) {
                if (p == AgentRules.Rank.RANK_10) continue;
                AgentRules.Rank partnerAfter = AgentRules.subordinate(p, l);
                AgentRules.Rank leaderAfter = AgentRules.superior(l, p);
                superiorStronger &= AgentRules.health(l, true) > AgentRules.health(partnerAfter, false) && AgentRules.power(l, true) > AgentRules.power(partnerAfter, false)
                    && AgentRules.health(leaderAfter, true) > AgentRules.health(p, false) && AgentRules.power(leaderAfter, true) > AgentRules.power(p, false);
                ranked &= partnerAfter.level() <= l.level() && leaderAfter.level() >= p.level() && (l.level() == 0 || partnerAfter.level() < l.level())
                    && (p.level() == 9 || leaderAfter.level() > p.level());
            }
        }
        check(superiorStronger, "whatever their ranks and whoever woke first, the superior has more health and hits harder");
        check(ranked, "the superior ranks above its partner (but at Rank 0 and Rank 9, where its edge alone makes it the stronger)");
        // 2.0.18: dashes chained in zigzags, more and faster by rank, more often, sooner again.
        check(AgentRules.dashes(AgentRules.Rank.RANK_0) == 1 && AgentRules.dashes(AgentRules.Rank.RANK_2) == 2 && AgentRules.dashes(AgentRules.Rank.RANK_5) == 3
            && AgentRules.dashes(AgentRules.Rank.RANK_8) == 4 && AgentRules.dashes(AgentRules.Rank.RANK_10) == 8, "more dashes in a row the higher the rank");
        boolean quicker = true;
        for (int k = 1; k < all.length; k++) {
            quicker &= AgentRules.dashSpeed(all[k]) > AgentRules.dashSpeed(all[k - 1]) && AgentRules.dashEvery(all[k]) <= AgentRules.dashEvery(all[k - 1])
                && AgentRules.dashOften(all[k]) > AgentRules.dashOften(all[k - 1]) && AgentRules.dashes(all[k]) >= AgentRules.dashes(all[k - 1]);
        }
        check(quicker && Math.abs(AgentRules.dashSpeed(AgentRules.Rank.RANK_0) - AgentRules.DASH_SPEED) < 1.0E-9
            && AgentRules.dashSpeed(AgentRules.Rank.RANK_10) > AgentRules.DASH_SPEED * 2.0, "faster, likelier and sooner dashes the higher the rank");
        double[] side = AgentRules.zig(1.0, 0.0, 0.0, 1.0, false);
        double[] in = AgentRules.zig(1.0, 0.0, 0.0, 1.0, true);
        double[] back = AgentRules.zig(side[0], side[1], 0.0, 1.0, false);
        check(Math.abs(side[0] + 1.0) < 1.0E-9 && Math.abs(side[1]) < 1.0E-9 && in[0] < 0.0 && in[1] > 0.0
            && Math.abs(Math.hypot(in[0], in[1]) - 1.0) < 1.0E-9 && Math.abs(back[0] - 1.0) < 1.0E-9, "each dash of a zigzag the other side; leaning in on prey out of reach");
        AgentRules.Rank reaper = AgentRules.Rank.RANK_10;
        check(reaper.title.equals("White Reaper") && AgentRules.regularRank(reaper) == AgentRules.Rank.RANK_9
            && AgentRules.superior(reaper, reaper) == AgentRules.Rank.RANK_9, "the White Reaper is outside the regular ranks");
        double weights = 0.0;
        for (AgentRules.Rank rank : AgentRules.Rank.values()) {
            weights += rank.weight;
        }
        check(AgentRules.REAPER_CHANCE > 0.0 && AgentRules.REAPER_CHANCE < AgentRules.Rank.RANK_9.weight / weights,
            "2.0.29: the White Reaper turns up, rarer than a Rank 9");
        check(AgentRules.dawnBlastDamage(0.0) == 26.0F
            && AgentRules.dawnBlastDamage(1.25) > AgentRules.dawnBlastDamage(2.5)
            && Math.abs(AgentRules.dawnBlastDamage(2.5) - 10.4F) < 0.01F, "Dawn's blast falls off from 26 to 10.4 raw damage");
        RigPose kick = AgentPose.kick(AgentRules.REAPER_KICK_WINDUP);
        check(kick.rightLegX < -1.4F && kick.legHold == 1.0F
            && AgentPose.kick(AgentRules.REAPER_KICK_DURATION).rightLegX == 0.0F, "the front kick extends and recovers");
        var slot = new com.hex.kagune.control.npc.UniqueNpc();
        var first = new java.util.UUID(0L, 1L);
        var second = new java.util.UUID(0L, 2L);
        check(slot.claimExclusive(first, 0L) && !slot.claimExclusive(second, Long.MAX_VALUE)
            && slot.claimExclusive(first, 1L), "the same Reaper still owns the slot after unloading");
        slot.died(first, 2L);
        check(slot.claimExclusive(second, 3L) && !slot.claimExclusive(first, 4L), "only death frees the Reaper slot");
        for (AgentRules.Rank rank : all) {
            if (rank == AgentRules.Rank.RANK_10) continue;
            check(rank.title.endsWith("Rank " + rank.level() + " Investigator") && rank.title.startsWith("\u00a7c") == rank.dangerous()
                && !rank.title.contains("Kaneki"), "its name: " + rank.title);
        }
    }
}
