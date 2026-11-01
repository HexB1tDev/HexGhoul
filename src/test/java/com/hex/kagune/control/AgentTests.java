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
}
