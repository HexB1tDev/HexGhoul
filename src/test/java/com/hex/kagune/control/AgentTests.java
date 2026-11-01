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
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Agent tests: " + passed + " checks passed");
    }

    /** The owner's nine skins, each in place (a 64x64 skin), four women and five men, each on the arms it is painted for. */
    static void looks() throws IOException {
        check(AgentRules.LOOKS.length == 9, "nine investigators' skins");
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
            check(look.texture().matches("ccg_[fm][0-9]"), look.texture() + " has a plain name (no file names from the web)");
            women += look.female() ? 1 : 0;
            slimMen += !look.female() && look.slim() ? 1 : 0;
        }
        check(women == 4 && slimMen == 2, "four women; five men, two of them slim armed");
        check(AgentRules.look(-1) == AgentRules.LOOKS[8] && AgentRules.look(9) == AgentRules.LOOKS[0], "any look number wraps onto the table");
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

    /** Pairs, rare: about one every two days, a little more often than a wild ghoul (now one every two or three nights). */
    static void spawning() {
        double agentTries = 24000.0 / AgentRules.SPAWN_EVERY;
        double pairsADay = agentTries * AgentRules.SPAWN_CHANCE;
        double ghoulTries = (NpcRules.NIGHT_TO - NpcRules.NIGHT_FROM) / (double)NpcRules.SPAWN_EVERY;
        double ghoulsADay = ghoulTries * NpcRules.SPAWN_CHANCE;
        check(pairsADay >= 0.3 && pairsADay <= 0.7, "not an everyday find: " + pairsADay + " pairs a day");
        check(pairsADay > ghoulsADay && pairsADay < ghoulsADay * 2.0, "a little more often than a wild ghoul: " + pairsADay + " vs " + ghoulsADay);
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
            check(second != first, "a pair carries two different quinques (" + first + ")");
        }
        for (Quinque q : Quinque.values()) {
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
        check(AgentRules.flank(true) == 0.0 && AgentRules.flank(false) > Math.PI / 2.0, "the second comes from the far side");
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
            check(blade.rightArmX > 0.4F && blade.rightArmZ > 0.2F, "a blade trailed low behind, out from the body (" + phase + ")");
        }
        RigPose half = AgentPose.run(1.0, 0.5F, AgentPose.Carry.CASE);
        RigPose full = AgentPose.run(1.0, 1.0F, AgentPose.Carry.CASE);
        check(Math.abs(half.lean - full.lean / 2.0F) < 1.0E-5F && half.legHold == 0.5F, "blended in by how fast it goes");
        // A two-handed stance carried on the run, lowered; a parry snaps in and out; a dash leans into it.
        RigPose stance = com.hex.kagune.control.client.animation.QuinquePose.hold(Quinque.KATANA);
        RigPose carried = AgentPose.run(0.7, 1.0F, AgentPose.Carry.STANCE);
        AgentPose.stanceRun(carried, stance, 0.7, 1.0F);
        check(carried.rightArmX > stance.rightArmX && carried.leftArmX > stance.leftArmX && carried.leftArmZ == stance.leftArmZ, "the katana kept in both hands, lowered");
        for (Quinque q : Quinque.values()) {
            RigPose start = AgentPose.parry(q, 0.0);
            RigPose mid = AgentPose.parry(q, 0.45);
            check(mid.rightArmX < start.rightArmX - 0.5F && mid.twist < 0.0F, q + ": the parry whips the weapon up and across");
            check(Math.abs(AgentPose.parry(q, 0.999).rightArmX - start.rightArmX) < 0.05F, q + ": and back");
            check(q.twoHanded == (mid.leftArmHold > 0.0F), q + ": both hands on a two-handed one");
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
}
