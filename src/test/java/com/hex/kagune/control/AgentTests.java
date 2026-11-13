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

    /** 2.1.9: before Peace or War: Rank 0-4 and the weaker wild ghouls run, Rank 5-7 and the strongest shake, Rank 8 and above stand. */
    static void dread() {
        com.hex.kagune.control.npc.FinalSight war = com.hex.kagune.control.npc.FinalSight.WAR;
        check(war.kingly() && com.hex.kagune.control.npc.FinalSight.PEACE.kingly() && !com.hex.kagune.control.npc.FinalSight.DRAGON.kingly()
            && !com.hex.kagune.control.npc.FinalSight.UKAKU.kingly(), "Peace and War are the king's");
        for (int rank = 0; rank <= 10; rank++) {
            com.hex.kagune.control.npc.FinalSight.Nerve nerve = com.hex.kagune.control.npc.FinalSight.investigator(rank);
            check(nerve == (rank <= 4 ? com.hex.kagune.control.npc.FinalSight.Nerve.RUN : rank <= 7 ? com.hex.kagune.control.npc.FinalSight.Nerve.SHAKE
                : com.hex.kagune.control.npc.FinalSight.Nerve.STAND), "Rank " + rank + ": " + nerve);
        }
        int tiers = com.hex.kagune.control.npc.NpcIdentity.TIERS;
        for (int tier = 0; tier < tiers; tier++) {
            // 4.0.0: ranks D to SS: the S and the SS shake before Peace or War, the rest run.
            check(com.hex.kagune.control.npc.FinalSight.ghoul(tier, tiers) == (tier < tiers - 2 ? com.hex.kagune.control.npc.FinalSight.Nerve.RUN
                : com.hex.kagune.control.npc.FinalSight.Nerve.SHAKE), "wild ghoul rank " + tier);
        }
        int breaks = 0;
        for (long t = 0; t < 900; t++) {
            if (com.hex.kagune.control.npc.FinalSight.breaks(17, t)) {
                breaks++;
            }
        }
        check(breaks >= 90 && breaks <= 110, "a shaker's nerve gives about once every few seconds: " + breaks + " ticks of 900");
        float most = 0.0F;
        for (double t = 0.0; t < 200.0; t += 0.37) {
            most = Math.max(most, Math.abs(com.hex.kagune.control.npc.FinalSight.tremble(5, t)));
        }
        check(most > 0.015F && most < 0.05F, "a tremble, small: " + most);
    }

    public static void main(String[] args) throws IOException {
        looks();
        dread();
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
        spoils();
        reaperTactics();
        ranks();
        tendril();
        headquarters();
        shedding();
        reaperBlade();
        reaperClose();
        reaperEnd();
        reaperSlowed();
        reaperFallen();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Agent tests: " + passed + " checks passed");
    }

    /**
     * 4.1.31 (the owner: "After the fight is done, he breaks the weapon. The blade shatters at the end, and it's just broken. And he accepts his
     * death a little before he dies actually, and then he just falls on his knees and dies after the weapon shatters"): his end.
     */
    static void reaperEnd() throws IOException {
        check(AgentRules.endClamp(10.0F, 50.0F) == 9.0F && AgentRules.endClamp(40.0F, 5.0F) == 5.0F && AgentRules.endClamp(1.0F, 5.0F) == 0.0F,
            "the blow that brings his end leaves him at 1: his end takes him, not the blow");
        check(com.hex.kagune.control.npc.CcgAgent.END == com.hex.kagune.control.npc.CcgAgent.BLADE + com.hex.kagune.control.npc.ReaperBladeRules.STEPS
            && com.hex.kagune.control.npc.CcgAgent.END < com.hex.kagune.control.npc.ArmRules.STEP, "his end's code clear of every other");
        com.hex.kagune.control.quinque.ArmMesh.Mesh stub = com.hex.kagune.control.quinque.ReaperBladeMesh.broken();
        double far = -1.0E9;
        for (int i = 0; i < stub.pos.length; i += 3) {
            far = Math.max(far, stub.pos[i]);
        }
        check(far < com.hex.kagune.control.quinque.ReaperBladeMesh.SNAPS + 1.5 && far > com.hex.kagune.control.quinque.ReaperBladeMesh.SNAPS - 1.5,
            "broken: the hilt and a jagged stub, nothing past where it snapped (" + far + ")");
        // The shards: out from where the blade was, down about him, never far.
        for (int i = 0; i < com.hex.kagune.control.npc.ReaperBladeRules.SHARDS; i++) {
            double[][] at = com.hex.kagune.control.client.animation.ReaperBladePose.shard(i, 0.0);
            double[][] later = com.hex.kagune.control.client.animation.ReaperBladePose.shard(i, 6.0);
            double moved = Math.hypot(later[0][0] - at[0][0], later[0][2] - at[0][2]);
            check(moved > 2.0 && Math.hypot(later[0][0], later[0][2]) < 64.0, "shard " + i + " flies out, and lands about him (" + moved + ")");
        }
        RigPose after = com.hex.kagune.control.client.animation.ReaperBladePose.end(AgentRules.REAPER_END_TICKS + 40.0);
        check(samePose(after, com.hex.kagune.control.client.animation.ReaperBladePose.end(AgentRules.REAPER_END_TICKS)), "held as he dies");
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        int act = brain.indexOf("private static void act(CcgAgent agent, Mind mind, ServerLevel level, long now) {");
        check(within(brain, act, "if (ending(agent, mind, level, now)) {", 120), "his end before anything else");
        check(brain.contains("event.setAmount(AgentRules.endClamp(agent.m_21223_(), event.getAmount()));") && brain.contains("agent.mind.endSource = event.getSource();")
            && brain.contains("agent.mind.shedAt != Long.MIN_VALUE || agent.mind.endAt != Long.MIN_VALUE") && brain.contains("if (agent.mind.finishing) {"),
            "begun by the blow that brings him low, nothing landing after, his own last blow let through");
        check(brain.contains("agent.m_6469_(mind.endSource != null ? mind.endSource : agent.m_269291_().m_269333_(agent), Float.MAX_VALUE);")
            && brain.contains("\"block.glass.break\""), "it shatters with a sound, and he dies by the blow that brought it (his killer gets Slain Reaper)");
        String client = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/AgentClient.java"));
        String layer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/ReaperBladeLayer.java"));
        check(client.contains("ReaperBladePose.end(")
            && layer.contains("age >= AgentRules.REAPER_END_SHATTER ? ReaperBladeMesh.broken() : ReaperBladeMesh.of()")
            && layer.contains("shards(pose, out, age, posed, light);") && layer.contains("double dt = age - AgentRules.REAPER_END_SHATTER;"),
            "drawn: the whole blade, then all at once the stub in his hand and its shards");
    }

    /** True if {@code what} is in {@code text} at or after {@code from} (itself found: not -1), within {@code within} characters of it. */
    private static boolean within(String text, int from, String what, int within) {
        if (from < 0) {
            return false;
        }
        int at = text.indexOf(what, from);
        return at >= 0 && at - from < within;
    }

    /** The world (blocks about him: y up from the ground, z forward) of a box's corners, model pixels {x0..x1, y0..y1, z0..z1}, under pose p. */
    private static double[][] corners(RigPose p, double x0, double x1, double y0, double y1, double z0, double z1) {
        double[][] out = new double[8][];
        int k = 0;
        for (double x : new double[] {x0, x1}) {
            for (double y : new double[] {y0, y1}) {
                for (double z : new double[] {z0, z1}) {
                    out[k++] = com.hex.kagune.control.client.animation.ReaperBladePose.place(p, new double[] {x, y, z}, false);
                }
            }
        }
        return out;
    }

    private static double lowest(double[][] points) {
        double low = Double.MAX_VALUE;
        for (double[] w : points) {
            low = Math.min(low, w[1]);
        }
        return low;
    }

    private static double highest(double[][] points) {
        double high = -Double.MAX_VALUE;
        for (double[] w : points) {
            high = Math.max(high, w[1]);
        }
        return high;
    }

    /**
     * 4.1.75 (the owner: "make him actually lay on the floor face down like falling on his face and lay there a bit"): knelt and bowed a
     * moment, he falls forward on his face, faster and faster, about his knees; lands face down, flat, on the ground (not in it, not over
     * it); lies there still till he dies, and stays so as he dies (never tipped on his side); the shards lying about him stay where they lie.
     */
    static void reaperFallen() throws IOException {
        int falls = AgentRules.REAPER_END_FALLS;
        int lands = falls + AgentRules.REAPER_END_FALLING;
        check(AgentRules.REAPER_END_FALLING >= 10 && AgentRules.REAPER_END_FALLING <= 20, "a fall from his feet (0.5 to 1 s), not a slide or a drop");
        check(lands + 40 <= AgentRules.REAPER_END_TICKS, "he lies there a while (2 s or more) before he dies");
        // 4.1.76 (the owner: "why tf does he hold off on that michael jackson ah pose????? and why does he hold his weapon like that when it's
        // breaking?? just make the blade instantly shatter while he's holding it on one hand, and the fall straight on his face"): in his
        // one-handed guard the blade shatters at once; straight after, he falls; nothing held on the way, no kneel, no lowering.
        check(AgentRules.REAPER_END_SHATTER <= 3 && falls > AgentRules.REAPER_END_SHATTER && falls - AgentRules.REAPER_END_SHATTER <= 6,
            "it shatters at once, and straight after he falls");
        for (double age = 0.0; age <= AgentRules.REAPER_END_SHATTER; age += 0.5) {
            check(samePose(com.hex.kagune.control.client.animation.ReaperBladePose.end(age),
                com.hex.kagune.control.client.animation.ReaperBladePose.guard(0.0F)), "end tick " + age + ": the blade in his one hand, his guard");
        }
        for (int age = AgentRules.REAPER_END_SHATTER; age < lands; age++) {
            check(!samePose(com.hex.kagune.control.client.animation.ReaperBladePose.end(age), com.hex.kagune.control.client.animation.ReaperBladePose.end(age + 1)),
                "end tick " + age + ": nothing held between the shatter and the ground");
        }
        RigPose guard = com.hex.kagune.control.client.animation.ReaperBladePose.guard(0.0F);
        for (double age = 0.0; age <= lands; age += 0.5) {
            RigPose p = com.hex.kagune.control.client.animation.ReaperBladePose.end(age);
            check(Math.abs(p.rightLegX) <= Math.abs(guard.rightLegX) + 1.0E-6 && Math.abs(p.leftLegX) <= Math.abs(guard.leftLegX) + 1.0E-6
                && Math.abs(p.lean) <= Math.abs(guard.lean) + 1.0E-6, "end tick " + age + ": straight from his feet, never knelt or bent back");
        }
        // Faster and faster: each tick of the fall turns him further than the one before.
        double lastStep = 0.0;
        boolean faster = true;
        for (int age = falls; age < lands; age++) {
            double step = com.hex.kagune.control.client.animation.ReaperBladePose.end(age).flip
                - com.hex.kagune.control.client.animation.ReaperBladePose.end(age + 1).flip;
            faster &= step > lastStep;
            lastStep = step;
        }
        check(faster, "he falls faster and faster (nothing holding him up)");
        check(com.hex.kagune.control.client.animation.ReaperBladePose.end(falls).flip == 0.0F, "upright on his knees until he falls");
        // Forward: his head goes out before him, his feet behind.
        RigPose down = com.hex.kagune.control.client.animation.ReaperBladePose.end(lands);
        double[] crown = com.hex.kagune.control.client.animation.ReaperBladePose.place(down, new double[] {0.0, -8.0, 0.0}, false);
        double[] heel = com.hex.kagune.control.client.animation.ReaperBladePose.place(down, new double[] {-1.9, 24.0, 0.0}, false);
        check(crown[2] > 1.8 && Math.abs(heel[2]) < 0.3, "fallen forward his whole length: his head before him, his feet where he stood ("
            + crown[2] + ", " + heel[2] + ")");
        check(Math.abs(down.flip + com.hex.kagune.control.client.animation.ReaperBladePose.FACE_DOWN) < 1.0E-4
            && Math.abs(down.rightLegX) < 1.0E-3 && Math.abs(down.leftLegX) < 1.0E-3 && Math.abs(down.lean) < 1.0E-3
            && Math.abs(down.headX) < 1.0E-3 && Math.abs(down.headY) < 1.0E-3, "face down, flat: his legs, his back and his head in line");
        // On the ground, face to it: his chest a pixel over it, his head at most a pixel into it, his legs on it, his back no higher than
        // his body is deep, all of him along it.
        double px = 1.0 / 16.0;
        double[][] torso = corners(down, -4, 4, 0, 12, -2, 2);
        double[][] head = corners(down, -4, 4, -8, 0, -4, 4);
        double[][] legs = corners(down, -3.9, 3.9, 12, 24, -2, 2);
        check(lowest(torso) > -0.01 && lowest(torso) < 1.5 * px, "his chest on the ground (" + lowest(torso) * 16 + " px)");
        check(lowest(head) > -1.5 * px, "his face to the ground, at most a pixel into it (" + lowest(head) * 16 + " px)");
        check(lowest(legs) > -0.01 && lowest(legs) < 1.5 * px, "his legs on the ground (" + lowest(legs) * 16 + " px)");
        check(highest(torso) < 6.0 * px && highest(head) < 8.0 * px && highest(legs) < 6.0 * px, "lying flat, nothing of him raised");
        // The broken hilt in his slack hand lies flat beside him, its stub out to his right (held as it hung, it stood straight up out of him).
        double[][] grip = com.hex.kagune.control.client.animation.ReaperBladePose.inArm(down);
        double[] inArm = {grip[0][0], grip[1][0], grip[2][0]};
        float[] armTurn = down.transform("rightArm", RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        double[][] arm = RigPose.zyx(armTurn[0], armTurn[1], armTurn[2]);
        double[] inModel = {arm[0][0] * inArm[0] + arm[0][1] * inArm[1] + arm[0][2] * inArm[2],
            arm[1][0] * inArm[0] + arm[1][1] * inArm[1] + arm[1][2] * inArm[2], arm[2][0] * inArm[0] + arm[2][1] * inArm[1] + arm[2][2] * inArm[2]};
        double[] stub = com.hex.kagune.control.client.animation.ReaperBladePose.place(down, inModel, true);
        double len = Math.sqrt(stub[0] * stub[0] + stub[1] * stub[1] + stub[2] * stub[2]);
        check(Math.abs(stub[1]) / len < 0.3 && stub[0] / len < -0.8,
            "lying, the broken hilt flat beside him, its stub out to his right, not standing up (" + java.util.Arrays.toString(stub) + ")");
        // The front of his feet stays where it stands through the fall (he falls over it), rising only the pixel his lying chest is up.
        double[] rest = com.hex.kagune.control.client.animation.ReaperBladePose.toes(com.hex.kagune.control.client.animation.ReaperBladePose.end(falls), 0.0);
        check(rest[1] > -1.0 * px && rest[1] < 0.5 * px, "standing, his feet on the ground (" + rest[1] * 16 + " px)");
        double lastY = rest[1];
        for (double age = falls; age <= lands; age += 0.25) {
            RigPose p = com.hex.kagune.control.client.animation.ReaperBladePose.end(age);
            double[] toes = com.hex.kagune.control.client.animation.ReaperBladePose.toes(p, -p.flip);
            check(Math.abs(toes[2] - rest[2]) < 1.0E-5 && toes[1] >= lastY - 1.0E-6
                && toes[1] <= com.hex.kagune.control.client.animation.ReaperBladePose.LYING - 2.0 * px + 1.0E-5,
                "end tick " + age + ": he falls over the front of his feet, kept where they stand");
            lastY = toes[1];
            check(lowest(corners(p, -4, 4, 0, 12, -2, 2)) > -0.5 * px && lowest(corners(p, -4, 4, -8, 0, -4, 4)) > -1.5 * px,
                "end tick " + age + ": nothing of him through the ground as he falls");
        }
        // Lying still, till he dies and after (the second vanilla takes over him).
        for (double age = lands; age <= AgentRules.REAPER_END_TICKS + 40; age += 0.5) {
            check(samePose(com.hex.kagune.control.client.animation.ReaperBladePose.end(age), down), "end tick " + age + ": he lies still");
        }
        // Carried between his body's frames: a point keeps its place in the world, whatever the pose (the shards on the ground).
        Random random = new Random(175);
        for (int i = 0; i < 400; i++) {
            RigPose a = com.hex.kagune.control.client.animation.ReaperBladePose.end(random.nextDouble() * AgentRules.REAPER_END_TICKS);
            RigPose b = new RigPose();
            b.flip = (float)(random.nextDouble() * 4.0 - 2.0);
            b.spin = (float)(random.nextDouble() * 4.0 - 2.0);
            b.roll = (float)(random.nextDouble() * 4.0 - 2.0);
            b.drop = (float)(random.nextDouble() * 12.0);
            b.lunge = (float)(random.nextDouble() * 8.0 - 4.0);
            double[] q = {random.nextDouble() * 40 - 20, random.nextDouble() * 40 - 10, random.nextDouble() * 40 - 20};
            double[] there = com.hex.kagune.control.client.animation.ReaperBladePose.place(a, q, false);
            double[] carried = com.hex.kagune.control.client.animation.ReaperBladePose.place(b,
                com.hex.kagune.control.client.animation.ReaperBladePose.carried(a, b, q, false), false);
            double[] way = com.hex.kagune.control.client.animation.ReaperBladePose.place(a, q, true);
            double[] wayCarried = com.hex.kagune.control.client.animation.ReaperBladePose.place(b,
                com.hex.kagune.control.client.animation.ReaperBladePose.carried(a, b, q, true), true);
            check(Math.hypot(Math.hypot(there[0] - carried[0], there[1] - carried[1]), there[2] - carried[2]) < 1.0E-6
                && Math.hypot(Math.hypot(way[0] - wayCarried[0], way[1] - wayCarried[1]), way[2] - wayCarried[2]) < 1.0E-6,
                "carried " + i + ": the same place in the world (and way) in the other pose's frame");
        }
        // The renderer: never tipped on his side as he dies; the shards carried; the thud as he lands.
        String renderer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/AgentRenderer.java"));
        int flipDegrees = renderer.indexOf("protected float m_6441_(CcgAgent agent) {");
        check(within(renderer, flipDegrees, "return agent.swing() == CcgAgent.END ? 0.0F : 90.0F;", 100)
            && Files.readString(Path.of("stubs/net/minecraft/client/renderer/entity/LivingEntityRenderer.java")).contains("protected float m_6441_(T entity)")
            && Files.readString(Path.of("verify/api-allowlist.txt")).contains("M net/minecraft/client/renderer/entity/LivingEntityRenderer m_6441_ (Lnet/minecraft/world/entity/LivingEntity;)F"),
            "he dies lying on his face (vanilla's tip onto his side off for him)");
        String layer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/ReaperBladeLayer.java"));
        int shards = layer.indexOf("static void shards(PoseStack pose, VertexConsumer out, double age, RigPose now, int light) {");
        check(within(layer, shards, "RigPose broke = ReaperBladePose.shatterPose();", 800) && within(layer, shards, "double ground = 24.0 - broke.drop - 0.05;", 900)
            && within(layer, shards, "q[k] = ReaperBladePose.carried(broke, now, q[k], false);", 3000)
            && within(layer, shards, "ReaperBladePose.carried(broke, now, ReaperBladePose.normal(l, w), true)", 3000),
            "the shards worked out where the blade broke and carried into his body's frame (they stay where they lie as he falls)");
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        int thud = brain.indexOf("if (age == AgentRules.REAPER_END_FALLS + AgentRules.REAPER_END_FALLING) {");
        check(thud > brain.indexOf("private static boolean ending(") && within(brain, thud, "\"entity.generic.big_fall\"", 300),
            "the thud of him landing on his face");
    }

    /** Every primitive field of the two poses the same (nothing of him moves between them). */
    private static boolean samePose(RigPose a, RigPose b) {
        try {
            for (java.lang.reflect.Field f : RigPose.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || !f.getType().isPrimitive()) {
                    continue;
                }
                f.setAccessible(true);
                if (!f.get(a).equals(f.get(b))) {
                    return false;
                }
            }
            return true;
        } catch (ReflectiveOperationException failure) {
            return false;
        }
    }

    /** Where the blade's mesh ends (its farthest vertex along it). */
    private static double meshEnd(com.hex.kagune.control.quinque.ArmMesh.Mesh mesh) {
        double far = -1.0E9;
        for (int i = 0; i < mesh.pos.length; i += 3) {
            far = Math.max(far, mesh.pos[i]);
        }
        return far;
    }

    /**
     * 4.1.74 (the owner: "bug found, while transforming arima keeps holding the old quinque instead of getting rid of it, the death animation
     * is too fast, during his death he should remain immortal and the blade slowly shatter, then he holds still for a bit then dies. during
     * this death animation he's 100% immortal, ungrabbable and all effects like bleed etc are removed from him." Then: "I think same for the
     * transformation animation, it goes by too quick, can barely see it"): Dawn out of his hand through the change; the change and his end
     * slowed; the blade breaking a piece at a time from its point; held still, then on his knees, still, then dead; nothing on him, nothing
     * holding him.
     */
    static void reaperSlowed() throws IOException {
        // The change: the same keys, played REAPER_CHANGE_SLOW times slower, ending exactly as its keys end (no frozen tail, nothing cut).
        check(AgentRules.REAPER_CHANGE_SLOW >= 1.5, "the change clearly slower (the owner: \"can barely see it\")");
        check(AgentRules.REAPER_CHANGE_REAL == (int)Math.ceil(AgentRules.REAPER_CHANGE_TICKS * AgentRules.REAPER_CHANGE_SLOW)
            && AgentRules.changeAge(AgentRules.REAPER_CHANGE_REAL) >= AgentRules.REAPER_CHANGE_TICKS
            && AgentRules.changeAge(AgentRules.REAPER_CHANGE_REAL - 1) < AgentRules.REAPER_CHANGE_TICKS,
            "the change lasts in the world exactly as long as its keys take played slower (" + AgentRules.REAPER_CHANGE_REAL + " ticks)");
        check(AgentRules.changeAge(0.0) == 0.0 && AgentRules.REAPER_CHANGE_REAL / 20.0 >= 9.0
            && AgentRules.REAPER_CHANGE_REAL >= 1.5 * AgentRules.REAPER_CHANGE_TICKS, "9.5 seconds of it, not 5.4");
        double previous = -1.0;
        boolean rising = true;
        for (double real = 0.0; real <= AgentRules.REAPER_CHANGE_REAL; real += 0.25) {
            double keys = AgentRules.changeAge(real);
            rising &= keys > previous;
            previous = keys;
        }
        check(rising, "the change's keys only ever go forward");

        // (4.1.74's slowed end, lowered, broken a piece at a time, on his knees: replaced by 4.1.76's, reaperFallen.)

        // The server: Dawn out of his hand as the change begins and every tick of it; everything off him every tick of his end; a crack as
        // each piece goes, the whole blade's sound with the last; nothing holding him.
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        int shed = brain.indexOf("private static boolean shed(CcgAgent agent, Mind mind, ServerLevel level, long now) {");
        int shedEnd = brain.indexOf("\n    }\n", shed);
        int start = brain.indexOf("agent.swing(CcgAgent.SHED, now);", shed);
        int firstLetGo = brain.indexOf("letGo(agent);", shed);
        int tickPin = brain.indexOf("pin(agent, mind, level);", start);
        check(shed > 0 && start > shed && firstLetGo > start && firstLetGo < brain.indexOf("return true;", start),
            "Dawn out of his hand the tick the change begins");
        check(tickPin > start && within(brain, tickPin, "letGo(agent);", 400) && brain.indexOf("letGo(agent);", tickPin) < shedEnd,
            "and every tick it lasts");
        int letGo = brain.indexOf("private static void letGo(CcgAgent agent) {");
        check(within(brain, letGo, "agent.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);", 200),
            "put by (his hand emptied, not dropped: no free Dawn mid-fight)");
        check(brain.contains("AgentRules.changeAge(age) >= AgentRules.REAPER_SHED_SWAP"), "his look turns bare at the slowed key");
        int ending = brain.indexOf("private static boolean ending(CcgAgent agent, Mind mind, ServerLevel level, long now) {");
        int endPin = brain.indexOf("pin(agent, mind, level);", ending);
        check(ending > 0 && endPin > ending && within(brain, endPin, "cleanse(agent, level, now);", 400), "everything taken off him every tick of his end");
        int cleanse = brain.indexOf("static void cleanse(CcgAgent agent, ServerLevel level, long now) {");
        String body = cleanse > 0 ? brain.substring(cleanse, brain.indexOf("\n    }\n", cleanse)) : "";
        check(body.contains("agent.m_21219_();") && body.contains("agent.m_20095_();") && body.contains("UkakuCrystalCombat.stanch(agent, level, now);")
            && body.contains("PaleStun.clear(agent);") && body.contains("PaleKnockdown.clear(agent);"),
            "every effect, his fire, the bleeding and spikes in him, a stun, a knockdown");
        int crash = brain.indexOf("if (!mind.shattered && age >= AgentRules.REAPER_END_SHATTER) {");
        check(within(brain, crash, "\"block.glass.break\"", 300) && within(brain, crash, "\"hexkagune:sfx.blade_heavy\"", 400)
            && !brain.contains("ReaperBladeRules.breaking("), "4.1.76: one crash as it shatters all at once, no cracks piece by piece");
        String crystals = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/UkakuCrystalCombat.java"));
        int stanch = crystals.indexOf("public static void stanch(LivingEntity body, ServerLevel level, long now) {");
        check(within(crystals, stanch, "BLEEDS.remove(body.m_20148_())", 200) && within(crystals, stanch, "STUCK.remove(body.m_19879_())", 300)
            && within(crystals, stanch, "body.m_21195_(KaguneEffects.BLEEDING.get());", 400)
            && within(crystals, stanch, "dirty(level, body.m_19879_());", 500), "his bleeding stopped, the spikes out of him, clients told");
        check(Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/PaleStun.java")).contains("STUNNED.remove(body.m_19879_());")
            && Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/PaleKnockdown.java")).contains("DOWN.remove(body.m_19879_());"),
            "a Pale's stun and knockdown let go of him");
        String owl = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/EtoOwl.java"));
        String child = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/ChildKagune.java"));
        String ken = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/KenPullOut.java"));
        String eto = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/EtoBrain.java"));
        int owlFair = owl.indexOf("static boolean fair(EtoNpc npc, EtoBrain.Mind mind, LivingEntity body) {");
        int childFair = child.indexOf("static boolean fair(LivingEntity living, LivingEntity threat) {");
        int foe = eto.indexOf("public static boolean foe(EtoNpc npc, LivingEntity body) {");
        check(within(owl, owlFair, "AgentBrain.untouchable(body)", 400), "a companion's arm never takes hold of him");
        check(within(child, childFair, "AgentBrain.untouchable(living)", 500), "nor a child's kagune");
        check(ken.contains("&& !AgentBrain.untouchable(e)"), "nor Ken's burst pushes him");
        check(within(eto, foe, "if (AgentBrain.untouchable(body)) {", 300),
            "no official NPC's foe: their worms and tendrils (Roma's impale) never hold him");
        // Drawn slowed: the pose, the case and blade, the coat, all on the same slowed clock.
        String client = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/AgentClient.java"));
        String layer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/ReaperBladeLayer.java"));
        String coatLayer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/ReaperCoatLayer.java"));
        check(client.contains("double shedAge = AgentRules.changeAge(CcgAgent.since(now, agent.swingAt()) + partial);")
            && layer.contains("AgentRules.changeAge(CcgAgent.since(agent.m_9236_().m_46467_(), agent.swingAt()) + partial) - AgentRules.REAPER_SHED_TICKS")
            && coatLayer.contains("age = AgentRules.changeAge(age);"), "his pose, the case and blade, and the coat all played on the slowed clock");
        check(Files.readString(Path.of("verify/api-allowlist.txt")).contains("M net/minecraft/world/entity/LivingEntity m_21219_ ()Z")
            && Files.readString(Path.of("stubs/net/minecraft/world/entity/LivingEntity.java")).contains("public boolean m_21219_()"),
            "removeAllEffects proven (SRG) and stubbed");
    }

    /**
     * 4.1.30 (the owner: "He can dash more precisely. He's way more precise ... he's more into a melee close combat type ... He will always try
     * to, get into range and kill you. He'll dodge moves"): his rush stops where his blade reaches, never past it; his sidestep leaves the line
     * of what is coming; nothing of Dawn's ranged moves with his blade.
     */
    static void reaperClose() throws IOException {
        // A rush: never further a tick than its speed, never past where it stops.
        check(com.hex.kagune.control.npc.ReaperBladeRules.rushStep(10.0) == com.hex.kagune.control.npc.ReaperBladeRules.RUSH_SPEED
            && com.hex.kagune.control.npc.ReaperBladeRules.rushStep(0.4) == 0.4 && com.hex.kagune.control.npc.ReaperBladeRules.rushStep(-1.0) == 0.0,
            "his rush never overshoots where it stops");
        check(com.hex.kagune.control.npc.ReaperBladeRules.RUSH_STOP < AgentRules.REAPER_BLADE_REACH
            && com.hex.kagune.control.npc.ReaperBladeRules.RUSH_SPEED * com.hex.kagune.control.npc.ReaperBladeRules.RUSH_LONGEST
            >= com.hex.kagune.control.npc.ReaperBladeRules.RUSH_FROM - com.hex.kagune.control.npc.ReaperBladeRules.RUSH_STOP,
            "a rush from as far as he starts one ends in his blade's reach, in time");
        // A sidestep: square to the way it comes, to the side of its line he already stands on.
        java.util.Random r = new java.util.Random(77);
        for (int i = 0; i < 500; i++) {
            double vx = r.nextGaussian();
            double vz = r.nextGaussian();
            double dx = r.nextGaussian() * 3.0;
            double dz = r.nextGaussian() * 3.0;
            double[] w = com.hex.kagune.control.npc.ReaperBladeRules.dodge(vx, vz, dx, dz);
            double n = Math.hypot(vx, vz);
            double ux = vx / n;
            double uz = vz / n;
            double across = ux * dz - uz * dx;
            double after = ux * (dz + w[1]) - uz * (dx + w[0]);
            check(Math.abs(Math.hypot(w[0], w[1]) - 1.0) < 1.0E-9 && Math.abs(w[0] * ux + w[1] * uz) < 1.0E-9
                && Math.abs(after) > Math.abs(across) + 0.99, "a sidestep leaves the line of what comes (" + i + ")");
        }
        check(com.hex.kagune.control.npc.ReaperBladeRules.committed(0, com.hex.kagune.control.npc.ReaperBladeRules.blow(0).impact() - 2)
            && !com.hex.kagune.control.npc.ReaperBladeRules.committed(0, 2), "a blow in its chamber can be given up for a dodge; one about to land cannot");
        check(com.hex.kagune.control.npc.ReaperBladeRules.DODGE_SPEED * com.hex.kagune.control.npc.ReaperBladeRules.DODGE_TICKS > 3.0
            && com.hex.kagune.control.npc.ReaperBladeRules.DODGE_EVERY > com.hex.kagune.control.npc.ReaperBladeRules.DODGE_TICKS * 3,
            "a sidestep clears a body's width and more, and not every tick");
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        int blade = brain.indexOf("private static void blade(");
        int end = brain.indexOf("private static void startBlade(", blade);
        String body = brain.substring(blade, end);
        check(body.contains("rush(agent, mind, prey, level, now);") && body.contains("dodge(agent, mind, prey, level, now)")
            && brain.contains("UkakuFire.shots(shots);") && brain.contains("UkakuCrystalCombat.shots(shots);") && brain.contains("KaguneClash.threat(agent, now)"),
            "he rushes in and sidesteps arrows, thrown things, Ukaku's fire and crystal, and blows");
        check(!body.contains("lightning(") && !body.contains("soar") && !body.contains("ground(") && !body.contains("shoot("),
            "close combat only: none of Dawn's ranged moves with his blade");
    }

    /**
     * 4.1.29 (the owner: "a heavy weapon, dual-handed, that deals massive damage and, yeah, just causes bleed whenever hit", "pulls out a
     * random briefcase from his back like a golden looking briefcase", "Just keep it only for himself"): his blade, drawn from the golden case
     * after the jacket; both hands on its grip through every frame of his guard and his string, never through him or into the ground.
     */
    static void reaperBlade() throws IOException {
        com.hex.kagune.control.quinque.ArmMesh.Mesh mesh = com.hex.kagune.control.quinque.ReaperBladeMesh.of();
        double least = Double.MAX_VALUE;
        double most = -Double.MAX_VALUE;
        for (int i = 0; i < mesh.pos.length; i += 3) {
            least = Math.min(least, mesh.pos[i]);
            most = Math.max(most, mesh.pos[i]);
        }
        check(mesh.quads > 2000 && least <= com.hex.kagune.control.quinque.ReaperBladeMesh.BUTT + 0.5
            && most >= com.hex.kagune.control.quinque.ReaperBladeMesh.TIP - 0.5, "a smooth mesh from its red tip to its point: " + least + " .. " + most);
        check(com.hex.kagune.control.quinque.ReaperBladeMesh.TIP - com.hex.kagune.control.quinque.ReaperBladeMesh.BUTT > 40.0, "massive: longer than he is tall");
        check(com.hex.kagune.control.quinque.ReaperBladeMesh.goldHalf(1).quads > 100 && com.hex.kagune.control.quinque.ReaperBladeMesh.goldHalf(-1).quads > 100
            && com.hex.kagune.control.quinque.ReaperBladeMesh.goldHandle().quads > 10, "the golden case: two halves and its handle");
        // Split open on its hinge (its bottom edge stays put, its halves swing apart) and the blade slid out of it: nothing shrinks.
        double[] corner = {1.85, com.hex.kagune.control.quinque.ReaperBladeMesh.CASE_TOP, 0.0};
        double[] swung = com.hex.kagune.control.quinque.ReaperBladeMesh.opened(corner, 1, com.hex.kagune.control.client.animation.ReaperBladePose.CASE_OPEN);
        double[] hinge = com.hex.kagune.control.quinque.ReaperBladeMesh.opened(new double[] {0.0, com.hex.kagune.control.quinque.ReaperBladeMesh.CASE_BOTTOM, 3.0}, 1, 1.0);
        double[] other = com.hex.kagune.control.quinque.ReaperBladeMesh.opened(new double[] {-1.85, com.hex.kagune.control.quinque.ReaperBladeMesh.CASE_TOP, 0.0}, -1,
            com.hex.kagune.control.client.animation.ReaperBladePose.CASE_OPEN);
        check(swung[0] > 7.0 && other[0] < -7.0 && Math.abs(hinge[0]) < 1.0E-9 && Math.abs(hinge[1] - com.hex.kagune.control.quinque.ReaperBladeMesh.CASE_BOTTOM) < 1.0E-9
            && Math.abs(Math.hypot(swung[0], swung[1] - com.hex.kagune.control.quinque.ReaperBladeMesh.CASE_BOTTOM)
            - Math.hypot(1.85, com.hex.kagune.control.quinque.ReaperBladeMesh.CASE_TOP - com.hex.kagune.control.quinque.ReaperBladeMesh.CASE_BOTTOM)) < 1.0E-9,
            "its halves swing apart on the hinge, each its own size");
        check(com.hex.kagune.control.quinque.ReaperBladeMesh.slid(5.0, 0.0) == 5.0 && com.hex.kagune.control.quinque.ReaperBladeMesh.slid(30.0, 0.0) < 10.0
            && com.hex.kagune.control.quinque.ReaperBladeMesh.slid(30.0, 1.0) == 30.0 && com.hex.kagune.control.quinque.ReaperBladeMesh.slid(-12.0, 1.0) == -12.0,
            "the blade slides out of the case: its guard as it is, its blade and rod drawn out to their length");
        // Every frame of his guard, his string and the draw once the blade is out: the fists on the grip, the blade clear of him.
        java.util.List<double[]> frames = new java.util.ArrayList<>();
        frames.add(new double[] {-1, 0});
        for (int step = 0; step < com.hex.kagune.control.npc.ReaperBladeRules.STEPS; step++) {
            for (double age = 0.0; age <= com.hex.kagune.control.npc.ReaperBladeRules.blow(step).total(); age += 0.5) {
                frames.add(new double[] {step, age});
            }
        }
        for (double[] f : frames) {
            RigPose p = f[0] < 0 ? com.hex.kagune.control.client.animation.ReaperBladePose.guard(0.0F)
                : com.hex.kagune.control.client.animation.ReaperBladePose.strike((int)f[0], f[1]);
            String at = f[0] < 0 ? "guard" : "blow " + ((int)f[0] + 1) + " tick " + f[1];
            double[][] d = com.hex.kagune.control.client.animation.ReaperBladePose.drawn(p);
            double[] left = com.hex.kagune.control.client.animation.ReaperBladePose.leftFist(p);
            double[] grip = {d[0][0] + d[1][0] * com.hex.kagune.control.quinque.ReaperBladeMesh.LEFT_HAND,
                d[0][1] + d[1][1] * com.hex.kagune.control.quinque.ReaperBladeMesh.LEFT_HAND, d[0][2] + d[1][2] * com.hex.kagune.control.quinque.ReaperBladeMesh.LEFT_HAND};
            double miss = Math.sqrt(Math.pow(left[0] - grip[0], 2) + Math.pow(left[1] - grip[1], 2) + Math.pow(left[2] - grip[2], 2));
            // 4.1.37: his guard in his right hand alone, at his side (the owner's picture); both hands on the grip through each blow, the
            // left coming on over its first ticks and letting go over its last.
            float both = f[0] < 0 ? 0.0F : com.hex.kagune.control.client.animation.ReaperBladePose.bothHands(
                com.hex.kagune.control.npc.ReaperBladeRules.blow((int)f[0]).total(), f[1]);
            if (both >= 0.999F) {
                check(miss < 0.6, at + ": his left hand on the grip (" + miss + " px off)");
            }
            check(d[0][2] < -1.5 || both < 0.999F && d[0][0] < -5.0, at + ": his hands before him, or his fist at his side (" + d[0][0] + ", "
                + d[0][2] + ")");
            for (double u = 9.6; u <= com.hex.kagune.control.quinque.ReaperBladeMesh.TIP; u += 0.5) {
                double[] c = {d[0][0] + d[1][0] * u, d[0][1] + d[1][1] * u, d[0][2] + d[1][2] * u};
                boolean torso = Math.abs(c[0]) < 4.6 && c[1] > 0.0 && c[1] < 12.0 && c[2] > -2.6 && c[2] < 2.2;
                boolean head = Math.abs(c[0]) < 4.2 && c[1] > -8.4 && c[1] < 0.0 && Math.abs(c[2]) < 4.2;
                check(!torso && !head, at + ": the blade clear of his body and head at " + u + ": " + java.util.Arrays.toString(c));
                double[] model = com.hex.kagune.control.client.animation.ReaperBladePose.toModel(p, c);
                check(model[1] < 24.0 - p.drop + 1.0, at + ": the blade never into the ground at " + u + " (" + model[1] + ")");
            }
        }
        // The draw: his hand behind his back as the case comes into it, the case gone and the blade out by the end, in his guard.
        RigPose behind = com.hex.kagune.control.client.animation.ReaperBladePose.draw(AgentRules.REAPER_CASE_FROM);
        double[] hand = com.hex.kagune.control.client.animation.QuinqueGrip.fist(behind, true, false);
        check(hand[2] > 1.5, "the case taken from behind his back: " + java.util.Arrays.toString(hand));
        check(!com.hex.kagune.control.client.animation.ReaperBladePose.caseHeld(AgentRules.REAPER_CASE_FROM - 0.5)
            && com.hex.kagune.control.client.animation.ReaperBladePose.caseHeld(AgentRules.REAPER_CASE_OPENS)
            && !com.hex.kagune.control.client.animation.ReaperBladePose.caseHeld(AgentRules.REAPER_CASE_DROPS)
            && com.hex.kagune.control.client.animation.ReaperBladePose.caseOpen(AgentRules.REAPER_CASE_OPENS) == 0.0
            && com.hex.kagune.control.client.animation.ReaperBladePose.extended(AgentRules.REAPER_CASE_OPENS + 1.0) == 0.0F
            && com.hex.kagune.control.client.animation.ReaperBladePose.extended(AgentRules.REAPER_CASE_DROPS) == 1.0F
            && com.hex.kagune.control.client.animation.ReaperBladePose.caseOpen(AgentRules.REAPER_CASE_DROPS)
            == com.hex.kagune.control.client.animation.ReaperBladePose.CASE_OPEN, "the case in his hand, split open, the blade slid out, then dropped");
        // The dropped case falls to his feet and is gone (sunk) by the time the draw ends.
        double landed = 20.0;
        check(com.hex.kagune.control.client.animation.ReaperBladePose.sunk(AgentRules.REAPER_DRAW_TICKS - AgentRules.REAPER_CASE_DROPS, landed) > 4.0
            && com.hex.kagune.control.client.animation.ReaperBladePose.sunk(2.0, landed) == 0.0, "the empty case lies a moment at his feet, then sinks away");
        check(AgentRules.REAPER_CASE_FROM < AgentRules.REAPER_CASE_OPENS && AgentRules.REAPER_CASE_DROPS < AgentRules.REAPER_DRAW_TICKS
            && AgentRules.REAPER_CHANGE_TICKS == AgentRules.REAPER_SHED_TICKS + AgentRules.REAPER_DRAW_TICKS, "the draw's times in order");
        RigPose end = com.hex.kagune.control.client.animation.ReaperBladePose.draw(AgentRules.REAPER_DRAW_TICKS);
        RigPose guard = com.hex.kagune.control.client.animation.ReaperBladePose.guard(0.0F);
        // 4.1.37 (the owner: "The blade is facing up instead of down"; "You should be holding that upper bone thing on the blade always"):
        // in his guard the point is well below his fist, his fist on the bone grip behind the guard, the rod rising back past his hip,
        // clear of his side; standing and on his run.
        for (float run : new float[] {0.0F, 1.0F}) {
            RigPose g = com.hex.kagune.control.client.animation.ReaperBladePose.guard(run);
            double[][] d = com.hex.kagune.control.client.animation.ReaperBladePose.drawn(g);
            double[] tip = com.hex.kagune.control.client.animation.ReaperBladePose.toModel(g, new double[] {d[0][0] + d[1][0]
                * com.hex.kagune.control.quinque.ReaperBladeMesh.TIP, d[0][1] + d[1][1] * com.hex.kagune.control.quinque.ReaperBladeMesh.TIP,
                d[0][2] + d[1][2] * com.hex.kagune.control.quinque.ReaperBladeMesh.TIP});
            double[] fist = com.hex.kagune.control.client.animation.ReaperBladePose.toModel(g, d[0]);
            check(tip[1] > fist[1] + 8.0, "his guard (run " + run + "): the blade's point down, below his fist (" + tip[1] + " vs " + fist[1] + ")");
            check(d[1][1] > 0.2 && d[1][2] < -0.7, "his guard (run " + run + "): the blade ahead of him and down: " + java.util.Arrays.toString(d[1]));
            for (double u = com.hex.kagune.control.quinque.ReaperBladeMesh.BUTT; u <= 0.0; u += 0.5) {
                double[] c = {d[0][0] + d[1][0] * u, d[0][1] + d[1][1] * u, d[0][2] + d[1][2] * u};
                check(c[0] < -4.8 || c[2] > 2.4 || c[2] < -2.6, "his guard (run " + run + "): the rod clear of him at " + u + ": "
                    + java.util.Arrays.toString(c));
            }
            double[] left = com.hex.kagune.control.client.animation.ReaperBladePose.leftFist(g);
            check(left[0] > 5.0, "his guard: his left hand loose at his side, off the grip: " + java.util.Arrays.toString(left));
        }
        check(com.hex.kagune.control.client.animation.ReaperBladePose.bothHands(16.0, 0.0) == 0.0F
            && com.hex.kagune.control.client.animation.ReaperBladePose.bothHands(16.0, 8.0) == 1.0F
            && com.hex.kagune.control.client.animation.ReaperBladePose.bothHands(16.0, 16.0) == 0.0F, "each blow: the left hand on, then off");
        check(Math.abs(end.rightArmX - guard.rightArmX) < 1.0E-4 && Math.abs(end.leftArmX - guard.leftArmX) < 1.0E-4
            && Math.abs(end.weaponX - guard.weaponX) < 1.0E-4, "the draw ends in his guard (no jump into his fight)");
        // The string's timing is the server's; codes clear of the others.
        check(com.hex.kagune.control.client.animation.ReaperBladePose.STRING.length == com.hex.kagune.control.npc.ReaperBladeRules.STEPS
            && AgentRules.REAPER_BLADE_WEIGHT.length == com.hex.kagune.control.npc.ReaperBladeRules.STEPS
            && AgentRules.REAPER_BLADE_KNOCK.length == com.hex.kagune.control.npc.ReaperBladeRules.STEPS, "one key set, weight and throw per blow");
        for (int step = 0; step < com.hex.kagune.control.npc.ReaperBladeRules.STEPS; step++) {
            float[][] keys = com.hex.kagune.control.client.animation.ReaperBladePose.STRING[step];
            com.hex.kagune.control.npc.ReaperBladeRules.Blow blow = com.hex.kagune.control.npc.ReaperBladeRules.blow(step);
            check(keys[keys.length - 1][0] == blow.total() && blow.impact() > 4 && blow.impact() < blow.total() - 4,
                "blow " + (step + 1) + ": its keys as long as the blow, its impact after a chamber and before its recovery");
        }
        check(com.hex.kagune.control.npc.CcgAgent.BLADE > com.hex.kagune.control.npc.CcgAgent.SHED
            && com.hex.kagune.control.npc.CcgAgent.BLADE + com.hex.kagune.control.npc.ReaperBladeRules.STEPS <= com.hex.kagune.control.npc.ArmRules.STEP,
            "his blade's codes clear of every other");
        check(AgentRules.REAPER_BLADE_DAMAGE * AgentRules.power(AgentRules.Rank.RANK_10, false) > AgentRules.damage(Quinque.DAWN, 0)
            * AgentRules.power(AgentRules.Rank.RANK_10, false) && AgentRules.REAPER_BLADE_BLEED >= 1, "massive damage, and bleed on every hit");
        // The server and the client wired: his blade alone in his second phase, his hand empty (no item: never dropped), the layer drawn.
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        int fight = brain.indexOf("private static void fight(");
        check(brain.indexOf("blade(agent, mind, prey, level, now);", fight) > 0
            && brain.indexOf("blade(agent, mind, prey, level, now);", fight) < brain.indexOf("tactics(agent, mind, prey, level, now)", fight),
            "his blade before any of Dawn's moves");
        check(brain.contains("if (mind.shedAt != Long.MIN_VALUE || mind.bare || mind.dressAt != Long.MIN_VALUE) {") && brain.contains("UkakuCrystalCombat.agentBleed(body, agent, AgentRules.REAPER_BLADE_BLEED, now);"),
            "his hand empty in his second phase; every hit bleeds");
        check(brain.contains("if (age >= AgentRules.REAPER_CHANGE_REAL || age < 0L) {"), "nothing lands on him through the jacket and the draw (4.1.74: its slower length)");
        String renderer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/AgentRenderer.java"));
        String client = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/AgentClient.java"));
        check(renderer.contains("new ReaperBladeLayer(this)") && client.contains("ReaperBladePose.draw(shedAge - AgentRules.REAPER_SHED_TICKS)")
            && client.contains("return bladed(agent, limbSwing, partial, now);"), "drawn: the draw, his string, his guard");
    }

    /**
     * 4.1.28 (the owner: "he pulls out his jacket, he just removes his jacket, revealing a new skin ... During this next phase, he gets way
     * faster"): at half health the White Reaper takes his jacket off, once, nothing landing on him while he does, and is faster after.
     */
    /**
     * 4.1.37: the coat he takes off (ReaperCoatPose): worn where his parts are; off his shoulders and arms, every piece behind him by the
     * time it is free; never through the ground; flat on it behind him once landed, and kept there through his draw; gone (sunk) as his
     * change ends; and the layer that draws it wired in.
     */
    static void coat() throws IOException {
        com.hex.kagune.control.outfit.OutfitModels.Model model;
        try (java.io.InputStream in = Files.newInputStream(Path.of("src/main/resources" + com.hex.kagune.control.outfit.OutfitModels.RESOURCE))) {
            model = com.hex.kagune.control.outfit.OutfitModels.read(in).get("formal");
        }
        check(model != null, "the Formal Overcoat's model is there to draw his coat from");
        int[] bones = com.hex.kagune.control.client.animation.ReaperCoatPose.BONE;
        int[] slots = com.hex.kagune.control.client.animation.ReaperCoatPose.SLOT;
        for (int piece = 0; piece < com.hex.kagune.control.client.animation.ReaperCoatPose.PIECES; piece++) {
            check(!model.on(bones[piece], slots[piece]).isEmpty(), "coat piece " + piece + ": the overcoat has cubes for it");
        }
        // Worn at first: the body piece exactly on his torso (nothing shows where his look turned bare).
        RigPose start = AgentPose.shed(AgentRules.REAPER_SHED_SWAP);
        double[][][] worn = com.hex.kagune.control.client.animation.ReaperCoatPose.frame(
            com.hex.kagune.control.client.animation.ReaperCoatPose.BODY, AgentRules.REAPER_SHED_SWAP, 24.0);
        double[] collar = com.hex.kagune.control.client.animation.ReaperCoatPose.place(worn, new double[] {0.0, 0.0, 0.0});
        float[] torso = start.transform("torso", RigPose.POSITION, 0.0F, 0.0F, 0.0F);
        double[] neck = {torso[0], torso[1], torso[2]};
        check(Math.abs(collar[0] - neck[0]) + Math.abs(collar[1] - neck[1]) + Math.abs(collar[2] - neck[2]) < 1.0E-6,
            "the coat on him as his look turns bare under it: " + java.util.Arrays.toString(collar));
        // Free of him: every piece's middle behind his back; landed: flat on the ground behind him, and never through it on the way.
        double[][] middles = {{0.0, 6.0, 0.0}, {-1.0, 4.0, 0.0}, {1.0, 4.0, 0.0}, {0.0, 4.5, 0.0}, {0.0, 4.5, 0.0}};
        for (int piece = 0; piece < com.hex.kagune.control.client.animation.ReaperCoatPose.PIECES; piece++) {
            double[][][] off = com.hex.kagune.control.client.animation.ReaperCoatPose.frame(piece,
                com.hex.kagune.control.client.animation.ReaperCoatPose.OFF, 24.0);
            double[] mid = com.hex.kagune.control.client.animation.ReaperCoatPose.place(off, middles[piece]);
            check(mid[2] > 2.5, "coat piece " + piece + " behind his back as it comes free: " + java.util.Arrays.toString(mid));
            for (double age = com.hex.kagune.control.client.animation.ReaperCoatPose.LANDS + 0.5; age < AgentRules.REAPER_CHANGE_TICKS
                - com.hex.kagune.control.client.animation.ReaperCoatPose.SINKS; age += 4.0) {
                double[][][] lying = com.hex.kagune.control.client.animation.ReaperCoatPose.frame(piece, age, 24.0);
                double[] at = com.hex.kagune.control.client.animation.ReaperCoatPose.place(lying, middles[piece]);
                check(at[1] > 22.5 && at[1] < 24.0 && at[2] > 3.0, "tick " + age + ": coat piece " + piece + " lying on the ground behind him: "
                    + java.util.Arrays.toString(at));
                double depth = Math.sqrt(lying[0][0][2] * lying[0][0][2] + lying[0][1][2] * lying[0][1][2] + lying[0][2][2] * lying[0][2][2]);
                check(Math.abs(depth - com.hex.kagune.control.client.animation.ReaperCoatPose.FLAT) < 1.0E-6, "an empty coat lies flat");
            }
        }
        double end = AgentRules.REAPER_CHANGE_TICKS - 0.01;
        check(com.hex.kagune.control.client.animation.ReaperCoatPose.sunk(end) > 6.0
            && com.hex.kagune.control.client.animation.ReaperCoatPose.sunk(AgentRules.REAPER_SHED_TICKS + 20.0) == 0.0
            && !com.hex.kagune.control.client.animation.ReaperCoatPose.drawn(AgentRules.REAPER_CHANGE_TICKS),
            "it lies there through his draw and is sunk away as his change ends");
        String renderer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/AgentRenderer.java"));
        String layer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/ReaperCoatLayer.java"));
        check(renderer.contains("new ReaperCoatLayer(this)") && layer.contains("OutfitModels.get(\"formal\")")
            && layer.contains("textures/entity/ccg/reaper_coat.png") && layer.contains("FELL")
            && Files.exists(Path.of("src/main/resources/assets/hexkagune/textures/entity/ccg/reaper_coat.png")),
            "drawn by its own layer, the overcoat's model in white, kept where it fell");
    }

    /**
     * 4.1.37 (the owner: "whenever he's transforming, you can still grab him"; "he's still very hittable ... make him completely immortal
     * while he's transforming"; "his moves are currently not clashable"): untouchable through his change and his end (the attack itself
     * cancelled, not only its damage; nobody's target; held by nothing, pinned where he stands), his change never waiting for ever to land,
     * and his blade's and his hands' blows on the clash board.
     */
    static void guarded() throws IOException {
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        int rule = brain.indexOf("public static boolean untouchable(Entity body) {");
        check(rule > 0 && within(brain, rule, "&& !mind.finishing", 300)
            && within(brain, rule, "&& (mind.shedAt != Long.MIN_VALUE || mind.endAt != Long.MIN_VALUE || mind.dressAt != Long.MIN_VALUE);", 400),
            "untouchable while he changes or ends (his end's own last blow let through)");
        int attacked = brain.indexOf("public static void attacked(LivingAttackEvent event) {");
        check(attacked > 0 && within(brain, attacked, "if (untouchable(event.getEntity()) && !com.hex.kagune.control.GhoulTank.bypasses(", 400),
            "the attack itself cancelled (no knockback, red flash or sound), /kill and the void still taking him");
        int shed = brain.indexOf("private static boolean shed(CcgAgent agent, Mind mind, ServerLevel level, long now) {");
        int end = brain.indexOf("private static boolean ending(CcgAgent agent, Mind mind, ServerLevel level, long now) {");
        check(within(brain, shed, "pin(agent, mind, level);", 2600) && within(brain, end, "pin(agent, mind, level);", 400),
            "pinned where he stands, every tick of his change and of his end");
        int free = brain.indexOf("private static void free(CcgAgent agent, ServerLevel level) {");
        check(free > 0 && brain.indexOf("KaguneServerController.releaseHeldSlot(level, tails, slot)", free) > 0
            && brain.indexOf("BikakuMoves.release(player, bikaku, false)", free) > 0 && brain.indexOf("PaleFlesh.release(", free) > 0
            && brain.indexOf("GraftCombat.release(", free) > 0 && brain.indexOf("PlayerArms.unbind(player, id)", free) > 0,
            "let go by every hold: the kagune's tails, a Bikaku's, a Pale's flesh, a Graft's limbs, a bound arm");
        check(brain.contains("ago(now, mind.shedPendingAt) >= AgentRules.REAPER_SHED_WAIT") && AgentRules.REAPER_SHED_WAIT <= 100,
            "his change never waits for ever for him to land (in water he would never be bare, never below 90)");
        String eto = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/EtoBrain.java"));
        String arms = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/PlayerArms.java"));
        String patch = Files.readString(Path.of("patch/KagunePatch.java"));
        int spared = eto.indexOf("public static boolean spared(ServerPlayer player, LivingEntity body) {");
        check(spared > 0 && within(eto, spared, "if (AgentBrain.untouchable(body)) {", 200)
            && patch.contains("patchSpared(controller, \"validSwipeTarget\");") && patch.contains("patchSpared(controller, \"isHostileTo\");"),
            "nobody's target: the original kagune's own checks (every grab, tail and swipe that asks them) say no");
        check(arms.contains("if (body == player || !body.m_6084_() || AgentBrain.untouchable(body)) {")
            && arms.contains("!AgentBrain.untouchable(target) && target.m_20280_(player) <= length * length"), "nor the quinque arms' (a Bind)");
        int blade = brain.indexOf("private static void startBlade(");
        int lands = brain.indexOf("private static void bladeLands(");
        int strike = brain.indexOf("private static void beginStrike(");
        int brawl = brain.indexOf("private static void brawlLands(");
        check(within(brain, blade, "KaguneClash.swing(agent, now, ReaperBladeRules.blow(step).impact()", 900)
            && within(brain, lands, "KaguneClash.meets(agent, prey, now)", 500)
            && within(brain, strike, "KaguneClash.swing(agent, now, strike.impact", 900)
            && within(brain, brawl, "KaguneClash.meets(agent, target, now)", 400),
            "his blade's blows and his hands' strikes can be met (KaguneClash): set out on the board, met at impact");
    }

    /**
     * 4.1.38 (the owner: "after the fight ends, I should note, let's say 2 minutes without fighting, Arima puts back his jacket in his
     * normal"; they chose it reversed, in place): two minutes calm; from his guard with no jump, the case fetched and brought to his fist,
     * the blade back in it, the case away, the coat back on (worn whole when his look turns back under it), at rest at the end; untouchable,
     * pinned; his first phase back, whole.
     */
    static void dressing() throws IOException {
        check(AgentRules.REAPER_REDRESS_CALM == 2400 && AgentRules.REAPER_DRESS_TICKS == AgentRules.REAPER_STOW_TICKS + AgentRules.REAPER_DON_TICKS
            && AgentRules.REAPER_DRESS_SWAP < AgentRules.REAPER_DRESS_TICKS && AgentRules.REAPER_DRESS_SWAP > AgentRules.REAPER_STOW_TICKS,
            "two minutes out of a fight; the look turns back near the end, under the coat");
        check(com.hex.kagune.control.npc.CcgAgent.DRESS > com.hex.kagune.control.npc.CcgAgent.END
            && com.hex.kagune.control.npc.CcgAgent.DRESS < com.hex.kagune.control.npc.ArmRules.STEP, "its swing code clear of every other");
        RigPose start = com.hex.kagune.control.client.animation.ReaperDressPose.pose(0.0);
        RigPose guard = com.hex.kagune.control.client.animation.ReaperBladePose.guard(0.0F);
        check(Math.abs(start.rightArmX - guard.rightArmX) + Math.abs(start.weaponX - guard.weaponX) + Math.abs(start.leftArmX - guard.leftArmX) < 0.02,
            "it starts from his guard, no jump");
        // The case: in his left hand from behind his back, at his right fist (as the draw has it) once brought, shut, put away.
        check(com.hex.kagune.control.client.animation.ReaperDressPose.held(start, 0.0) == null, "no case before he reaches for it");
        double[] hand = com.hex.kagune.control.client.animation.QuinqueGrip.fist(
            com.hex.kagune.control.client.animation.ReaperDressPose.pose(com.hex.kagune.control.client.animation.ReaperDressPose.REACH), false, false);
        check(hand[2] > 1.5, "the case taken from behind his back with his left hand: " + java.util.Arrays.toString(hand));
        double bring = com.hex.kagune.control.client.animation.ReaperDressPose.BRING;
        com.hex.kagune.control.client.animation.ReaperDressPose.Held before = com.hex.kagune.control.client.animation.ReaperDressPose.held(
            com.hex.kagune.control.client.animation.ReaperDressPose.pose(bring - 0.01), bring - 0.01);
        com.hex.kagune.control.client.animation.ReaperDressPose.Held after = com.hex.kagune.control.client.animation.ReaperDressPose.held(
            com.hex.kagune.control.client.animation.ReaperDressPose.pose(bring), bring);
        double jump = Math.abs(before.origin()[0] - after.origin()[0]) + Math.abs(before.origin()[1] - after.origin()[1])
            + Math.abs(before.origin()[2] - after.origin()[2]);
        double turn = 0.0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                turn += Math.abs(before.turn()[r][c] - after.turn()[r][c]);
            }
        }
        check(jump < 0.2 && turn < 0.05 && Math.abs(after.open() - com.hex.kagune.control.client.animation.ReaperBladePose.CASE_OPEN) < 1.0E-6,
            "carried to his right fist open, and set there with no jump (" + jump + ", " + turn + ")");
        check(com.hex.kagune.control.client.animation.ReaperDressPose.bladeOut(bring) == 1.0F
            && com.hex.kagune.control.client.animation.ReaperDressPose.bladeOut(com.hex.kagune.control.client.animation.ReaperDressPose.IN) < 0.0F
            && com.hex.kagune.control.client.animation.ReaperDressPose.held(com.hex.kagune.control.client.animation.ReaperDressPose.pose(
                AgentRules.REAPER_STOW_TICKS - 0.5), AgentRules.REAPER_STOW_TICKS - 0.5) == null,
            "the blade back in the case, and the case put away before the coat");
        // The coat: not on him until his hands have it, worn whole by the time his look turns back under it.
        check(com.hex.kagune.control.client.animation.ReaperDressPose.coatAge(AgentRules.REAPER_STOW_TICKS) < 0.0
            && com.hex.kagune.control.client.animation.ReaperDressPose.coatAge(AgentRules.REAPER_DRESS_SWAP)
            < com.hex.kagune.control.client.animation.ReaperCoatPose.PEEL - 3.0, "the coat on him whole before his look turns back");
        RigPose end = com.hex.kagune.control.client.animation.ReaperDressPose.pose(AgentRules.REAPER_DRESS_TICKS - 0.01);
        check(Math.abs(end.rightArmX) < 0.02 && Math.abs(end.leftArmX) < 0.02 && end.armHold < 0.05, "at rest at the end, into his first phase");
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        int dress = brain.indexOf("private static boolean dress(CcgAgent agent, Mind mind, ServerLevel level, long now) {");
        check(dress > 0 && within(brain, dress, "ago(now, mind.foughtAt) < AgentRules.REAPER_REDRESS_CALM", 600)
            && within(brain, dress, "ago(now, mind.hurtAt) < AgentRules.REAPER_REDRESS_CALM", 700)
            && within(brain, dress, "mind.prey != 0", 500) && within(brain, dress, "pin(agent, mind, level);", 1300)
            && within(brain, dress, "agent.getPersistentData().m_128473_(BARE);", 1700)
            && within(brain, dress, "agent.m_21153_(agent.m_21233_());", 2100)
            && brain.contains("if (dress(agent, mind, level, now)) {")
            && brain.contains("|| mind.dressAt != Long.MIN_VALUE);"), "calm two minutes, pinned and untouchable, his first phase back and whole");
        String client = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/AgentClient.java"));
        String blade = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/ReaperBladeLayer.java"));
        String coatLayer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/ReaperCoatLayer.java"));
        check(client.contains("ReaperDressPose.pose(dressAge)") && blade.contains("agent.swing() == CcgAgent.DRESS")
            && coatLayer.contains("ReaperDressPose.coatAge(age)"), "drawn: his pose, the blade and case, the coat");
    }

    static void shedding() throws IOException {
        check(AgentRules.reaperLook(AgentRules.REAPER_LOOK) && AgentRules.reaperLook(AgentRules.REAPER_BARE_LOOK) && !AgentRules.reaperLook(0)
            && !AgentRules.reaperLook(AgentRules.REGULAR_LOOKS - 1), "both of his looks are his, no investigator's");
        check(AgentRules.REAPER_SHED_HEALTH == 90.0F && AgentRules.Rank.RANK_10.health > 200.0,
            "4.1.29 (the owner: \"trigger when he reaches about 90 HP\"): the jacket comes off at 90 of his health");
        check(AgentRules.shedClamp(280.0F, 500.0F) == 190.0F && AgentRules.shedClamp(150.0F, 20.0F) == 20.0F
            && AgentRules.shedClamp(90.0F, 30.0F) == 0.0F && AgentRules.shedClamp(110.0F, 30.0F) == 20.0F,
            "with the jacket on no blow takes him past 90, however big (the second phase always comes)");
        check(AgentRules.REAPER_BARE_SPEED > 1.2 && AgentRules.REAPER_BARE_PACE < 0.8, "faster with it off: his speed and the rest between blows");
        // 4.1.37 (the owner: "he should actually like take off an actual jacket, before switching to the other skin"): the look turns bare
        // at once, under the real coat (worn exactly where his parts are then), which then comes off as a thing of its own.
        check(AgentRules.REAPER_SHED_SWAP >= 1 && AgentRules.REAPER_SHED_SWAP < com.hex.kagune.control.client.animation.ReaperCoatPose.PEEL - 5,
            "the look turns bare under the coat, well before it starts coming off");
        coat();
        guarded();
        dressing();
        check(com.hex.kagune.control.npc.CcgAgent.SHED > com.hex.kagune.control.npc.CcgAgent.GROUND
            && com.hex.kagune.control.npc.CcgAgent.SHED < com.hex.kagune.control.npc.ArmRules.STEP, "the shed's swing code is clear of every other");
        // The move: hands to his lapels in front of his chest, then swept back behind him by the time the look switches, back at rest at the end.
        RigPose grip = AgentPose.shed(7.0);
        for (boolean right : new boolean[] {true, false}) {
            double[] fist = com.hex.kagune.control.client.animation.QuinqueGrip.fist(grip, right, true);
            check(fist[2] < -3.0 && Math.abs(fist[0]) < 4.5 && fist[1] > -1.0 && fist[1] < 9.0,
                (right ? "right" : "left") + " hand on his lapels: before his chest, near its middle, at its height: " + java.util.Arrays.toString(fist));
            double[] off = com.hex.kagune.control.client.animation.QuinqueGrip.fist(
                AgentPose.shed(com.hex.kagune.control.client.animation.ReaperCoatPose.OFF - 1.0), right, false);
            check(off[2] > 2.0, (right ? "right" : "left") + " hand behind his back as the coat comes off: " + java.util.Arrays.toString(off));
        }
        for (double age = 0.0; age < AgentRules.REAPER_SHED_TICKS; age += 0.5) {
            RigPose p = AgentPose.shed(age);
            for (boolean right : new boolean[] {true, false}) {
                double[] fist = com.hex.kagune.control.client.animation.QuinqueGrip.fist(p, right, age >= AgentRules.REAPER_SHED_SWAP);
                check(!(Math.abs(fist[0]) < 3.5 && fist[1] > 0.5 && fist[1] < 11.5 && Math.abs(fist[2]) < 1.5),
                    "tick " + age + ": " + (right ? "right" : "left") + " hand never inside his body");
            }
        }
        RigPose end = AgentPose.shed(AgentRules.REAPER_SHED_TICKS - 0.01);
        check(Math.abs(end.rightArmX) < 0.01 && Math.abs(end.leftArmX) < 0.01 && Math.abs(end.headZ) < 0.01 && end.armHold < 0.01,
            "back at rest as it ends (into his stance with no jump)");
        // The server: before anything else, once; nothing lands while he does it; the jacket's state kept with him; faster.
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        int act = brain.indexOf("private static void act(CcgAgent agent, Mind mind, ServerLevel level, long now) {");
        check(act > 0 && within(brain, act, "if (shed(agent, mind, level, now)) {", 400)
            && brain.indexOf("if (shed(agent, mind, level, now)) {", act) < brain.indexOf("if (BikakuDevour.held(agent)) {", act),
            "the shed comes before anything else he does (but his end, 4.1.31)");
        check(brain.contains("event.setCanceled(true); // 4.1.28") && brain.contains("AgentRules.shedClamp(health, event.getAmount())")
            && brain.contains("agent.mind.shedPending = true;"), "struck to half it starts; struck while it plays, nothing lands");
        check(brain.contains("agent.getPersistentData().m_128405_(BARE, 1);") && brain.contains("mind.bare = saved.m_128441_(BARE);")
            && brain.contains("mind.look = mind.bare ? AgentRules.REAPER_BARE_LOOK : AgentRules.REAPER_LOOK;"), "the jacket stays off, saved with him");
        int swap = brain.indexOf("mind.look = AgentRules.REAPER_BARE_LOOK;");
        check(swap > 0 && within(brain, swap, "agent.dress(mind.look, mind.kind);", 200)
            && within(brain, swap, ".m_22100_(speed(mind));", 400), "the look sent to every client as it comes off, and his speed with it");
        String client = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/AgentClient.java"));
        String renderer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/AgentRenderer.java"));
        check(client.contains("return AgentPose.shed(shedAge);") && client.contains("AgentRules.reaperLook(agent.lookIndex())")
            && renderer.contains("AgentModel.brawny = AgentRules.reaperLook(agent.lookIndex());") && renderer.contains("!AgentRules.reaperLook(agent.lookIndex())")
            && !client.contains("== com.hex.kagune.control.npc.AgentRules.REAPER_LOOK") && !renderer.contains("AgentRules.REAPER_LOOK"),
            "drawn as him in either look: his build, his idle");
    }

    /**
     * 4.1.4 (the owner: "CCG building, tons of ccg agents guard and patrol the area, sit in their offices"): the building's investigators at
     * their posts and their desks (HqRules, AgentDesk), and how one sits.
     */
    static void headquarters() throws IOException {
        // The building turns as a whole: a yaw of its own frame in the world.
        check(com.hex.kagune.control.npc.HqRules.yaw(0, 0) == 0.0F && com.hex.kagune.control.npc.HqRules.yaw(0, 1) == 90.0F
            && com.hex.kagune.control.npc.HqRules.yaw(0, 2) == 180.0F && com.hex.kagune.control.npc.HqRules.yaw(0, 3) == -90.0F
            && com.hex.kagune.control.npc.HqRules.yaw(-90, 1) == 0.0F && com.hex.kagune.control.npc.HqRules.yaw(180, 2) == 0.0F
            && com.hex.kagune.control.npc.HqRules.yaw(90, 1) == 180.0F && com.hex.kagune.control.npc.HqRules.yaw(-90, 3) == 180.0F
            && com.hex.kagune.control.npc.HqRules.yaw(0, 5) == 90.0F, "a watch's way turned with the building, kept within half a turn");
        for (int turns = 0; turns < 4; turns++) {
            for (int yaw : new int[] {0, 90, 180, -90}) {
                // The way it looks as a step ({-sin, cos}) is the template's step turned as the game turns a template's blocks.
                int[] step = {(int)Math.round(-Math.sin(Math.toRadians(yaw))), (int)Math.round(Math.cos(Math.toRadians(yaw)))};
                int[] turned = com.hex.kagune.control.village.VillageRules.turn(step[0], step[1], turns);
                float world = com.hex.kagune.control.npc.HqRules.yaw(yaw, turns);
                check((int)Math.round(-Math.sin(Math.toRadians(world))) == turned[0] && (int)Math.round(Math.cos(Math.toRadians(world))) == turned[1],
                    "yaw " + yaw + " turned " + turns + ": the way the building's blocks turn");
            }
        }
        // Where it sits: on the chair's low step, toward its open side, whichever way the chair faces.
        for (float yaw : new float[] {0.0F, 90.0F, 180.0F, -90.0F}) {
            double[] at = com.hex.kagune.control.npc.HqRules.seat(10, 64, -5, yaw);
            double fx = at[0] - 10.5;
            double fz = at[2] + 4.5;
            check(Math.abs(at[1] - 64.5) < 1.0E-9 && Math.abs(Math.hypot(fx, fz) - com.hex.kagune.control.npc.HqRules.SEAT_FORWARD) < 1.0E-9
                && Math.abs(fx - -Math.sin(Math.toRadians(yaw)) * 0.2) < 1.0E-9 && Math.abs(fz - Math.cos(Math.toRadians(yaw)) * 0.2) < 1.0E-9,
                "yaw " + yaw + ": sat half a block up, a fifth of a block toward the way it faces");
            check(com.hex.kagune.control.npc.HqRules.sits(at[0] - 10.5, at[1] - 64.0, at[2] + 4.5), "placed in its chair's block, it sits down at once");
        }
        check(com.hex.kagune.control.npc.HqRules.sits(1.5, 0.5, 0.0) && !com.hex.kagune.control.npc.HqRules.sits(1.7, 0.0, 0.0)
            && !com.hex.kagune.control.npc.HqRules.sits(0.0, 1.5, 0.0) && !com.hex.kagune.control.npc.HqRules.sits(0.0, -8.0, 0.0),
            "it sits down from beside its chair, never from across the room or another floor");
        check(com.hex.kagune.control.npc.HqRules.posted(0.5, 0.5) && !com.hex.kagune.control.npc.HqRules.posted(1.0, 1.0), "at its post within a step of it");
        // How one sits: thigh and shin are 6 sixteenths each; the hips just over the seat, the knees past the chair's edge, the feet off
        // the floor but never under it (the seat is 8 sixteenths over the floor).
        double hips = 12.0 - com.hex.kagune.control.npc.HqRules.SEAT_DROP;
        double knees = com.hex.kagune.control.npc.HqRules.SEAT_FORWARD * 16.0 + 6.0;
        double feet = hips - 6.0;
        check(hips >= 1.0 && hips <= 3.0 && knees > 8.0 && feet > -8.0 && feet <= 0.0, "hips " + hips + " over the seat, knees " + knees
            + " from the chair's middle (its edge at 8), feet " + feet + " from the seat's top (the floor at -8)");
        float least = Float.MAX_VALUE;
        float most = -Float.MAX_VALUE;
        float glance = 0.0F;
        for (double age = 0.0; age < 600.0; age += 3.0) {
            RigPose p = new RigPose();
            AgentPose.seated(p, age, 4242);
            least = Math.min(least, p.lean);
            most = Math.max(most, p.lean);
            glance = Math.max(glance, Math.abs(p.headY));
            check(p.drop == com.hex.kagune.control.npc.HqRules.SEAT_DROP && p.legHold == 1.0F && p.leftArmHold == 1.0F, "sat down, its legs and free arm the seat's");
            check(Math.abs(p.rightLegX + Math.PI / 2.0) < 1.0E-6 && Math.abs(p.leftLegX + Math.PI / 2.0) < 1.0E-6
                && Math.abs(p.rightKnee - Math.PI / 2.0) < 1.0E-6 && Math.abs(p.leftKnee - Math.PI / 2.0) < 1.0E-6,
                "its thighs out level, its shins straight down");
            check(p.rightLegZ > 0.0F && p.leftLegZ < 0.0F && p.lean > 0.0F && p.lean < 0.12F && p.headX >= 0.0F && p.headX < 0.2F,
                "its knees a little apart, its back near straight, its eyes on its desk");
        }
        check(most - least > 0.015F && glance > 0.3F, "it breathes, and now and then looks up to one side");
        RigPose one = new RigPose();
        RigPose other = new RigPose();
        AgentPose.seated(one, 100.0, 1);
        AgentPose.seated(other, 100.0, 9001);
        check(one.headY != other.headY || one.lean != other.lean, "two of them never as one");
        // The sources: what keeps one sat, and what gets it up.
        String brain = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
        check(brain.contains("mind.seatKept = false;\n        act(agent, mind, level, now);\n        if (!mind.seatKept && agent.seated()) {\n            AgentDesk.stand(agent);")
            && brain.contains("if (!AgentDesk.idle(agent, mind, place, level, now)) {\n                village(agent, mind, place, now);"),
            "sat only while its desk is all it does (a fight, a fright, a stun: up); the building's others walk its patrol");
        String desk = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/AgentDesk.java"));
        check(desk.contains("data.m_128471_(HqRules.SEAT)") && desk.contains("data.m_128471_(HqRules.POST)") && desk.contains("HqRules.yaw(data.m_128451_(HqRules.YAW), place.turns())")
            && desk.contains("agent.seated(true);\n                mind.seatKept = true;") && desk.contains(".m_60795_()"),
            "a seat, a post and the way to face read from its saved data; sat only on a chair still there");
        String client = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/AgentClient.java"));
        check(client.contains("if (agent.seated()) {") && client.contains("AgentPose.seated(p, agent.f_19797_ + partial, agent.m_19879_());"),
            "every client draws it sat");
        String agent = Files.readString(Path.of("src/main/java/com/hex/kagune/control/npc/CcgAgent.java"));
        check(agent.contains("this.f_19804_.m_135372_(SEAT, 0);") && agent.indexOf("ARM_TARGET = Synched") < agent.indexOf("SEAT = Synched"),
            "its seat rides on its entity data, after the older ones (their places kept)");
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
        check(AgentRules.LOOKS.length == 11, "nine investigators' skins and the White Reaper's two (4.1.28: his jacket off)");
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
            check(look.texture().matches("ccg_[fm][0-9]|ccg_reaper(_bare)?"), look.texture() + " has a plain name (no file names from the web)");
            women += look.female() ? 1 : 0;
            slimMen += !look.female() && look.slim() ? 1 : 0;
        }
        check(women == 4 && slimMen == 3, "four women; six men, three of them slim armed (the Reaper one of them)");
        AgentRules.Look reaper = AgentRules.LOOKS[AgentRules.REAPER_LOOK];
        check(reaper.texture().equals("ccg_reaper") && !reaper.female() && reaper.slim(), "the White Reaper: his skin, a man's body on the slim arms");
        AgentRules.Look bare = AgentRules.LOOKS[AgentRules.REAPER_BARE_LOOK];
        check(AgentRules.REAPER_BARE_LOOK == 10 && bare.texture().equals("ccg_reaper_bare") && !bare.female() && !bare.slim(),
            "4.1.28: his jacket off, appended last: the owner's Reaper_Phase2.png on the wide arms it is painted for");
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
        // 3.0.2: its case, the commoner the likelier: the regular quinques by how many carry them, then the higher ranks' weapons.
        com.hex.kagune.control.quinque.Arm whip = com.hex.kagune.control.quinque.Arm.WHIP;
        com.hex.kagune.control.quinque.Arm spearArm = com.hex.kagune.control.quinque.Arm.CRYSTAL_SPEAR;
        com.hex.kagune.control.quinque.Arm hammer = com.hex.kagune.control.quinque.Arm.FLESH_HAMMER;
        com.hex.kagune.control.quinque.Arm sword = com.hex.kagune.control.quinque.Arm.BONE_SWORD;
        double[] ladder = {AgentRules.caseChance(Quinque.BLADE, null, false, 0), AgentRules.caseChance(Quinque.KATANA, null, false, 0),
            AgentRules.caseChance(Quinque.SPEAR, null, false, 0), AgentRules.caseChance(Quinque.MACE, null, false, 0),
            AgentRules.caseChance(Quinque.LAUNCHER, null, false, 0), AgentRules.caseChance(whip.carried, whip, false, 0),
            AgentRules.caseChance(hammer.carried, hammer, false, 0), AgentRules.caseChance(sword.carried, sword, true, 0)};
        boolean falling = true;
        for (int i = 1; i < ladder.length; i++) {
            falling &= ladder[i] < ladder[i - 1] && ladder[i] > 0.0;
        }
        check(falling && ladder[0] == 0.10 && ladder[ladder.length - 1] == 0.02, "the case: the commoner the likelier (a blade one in ten ... a masterwork one in fifty)");
        check(AgentRules.caseChance(whip.carried, whip, false, 0) == AgentRules.caseChance(spearArm.carried, spearArm, false, 0)
            && AgentRules.caseChance(hammer.carried, hammer, false, 0) == AgentRules.caseChance(sword.carried, sword, false, 0)
            && AgentRules.caseChance(Quinque.DAWN, null, false, 3) == 0.0, "as common, as likely; Dawn is the Reaper's own");
        check(Math.abs(AgentRules.caseChance(Quinque.BLADE, null, false, 3) - 0.175) < 1.0E-9 && AgentRules.caseChance(Quinque.BLADE, null, false, 99)
            == AgentRules.caseChance(Quinque.BLADE, null, false, 3), "Looting: a quarter more a level, to three");
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

    /** 2.2.9: what a fallen investigator leaves: mastery always, a ration now and then, Dawn rarely. */
    static void spoils() throws IOException {
        int last = 0;
        for (com.hex.kagune.control.npc.AgentRules.Rank rank : com.hex.kagune.control.npc.AgentRules.Rank.values()) {
            int mastery = com.hex.kagune.control.npc.AgentRules.mastery(rank);
            check(mastery > last && mastery <= 60, rank + ": more mastery the higher the rank (" + mastery + ")");
            last = mastery;
        }
        check(com.hex.kagune.control.npc.AgentRules.mastery(com.hex.kagune.control.npc.AgentRules.Rank.RANK_10) == 60, "the Reaper: 60");
        // 4.1.25 (the owner: "use both styles of fighting that we got. Both the Kickmaster and the normal one, along with the flips"): his
        // hand-to-hand, either string half the time; drawn as a player's strike with Dawn carried clear of him (never through his legs or
        // the ground: every strike of both strings, every tick); its swing codes clear of every other.
        check(AgentRules.brawlStyle(0.2) == com.hex.kagune.control.pale.PaleStrike.Style.KICKS
            && AgentRules.brawlStyle(0.7) == com.hex.kagune.control.pale.PaleStrike.Style.BLITZ, "the White Reaper: Kick Master's string or the Speed Blitz");
        check(com.hex.kagune.control.npc.CcgAgent.STRIKE > com.hex.kagune.control.npc.CcgAgent.GROUND
            && com.hex.kagune.control.npc.CcgAgent.STRIKE > com.hex.kagune.control.npc.CcgAgent.SOARING, "a strike's swing code is no other move's");
        com.hex.kagune.control.client.animation.QuinqueHolds.Hold dawnHold = com.hex.kagune.control.client.animation.QuinqueHolds.of(Quinque.DAWN);
        for (com.hex.kagune.control.pale.PaleStrike strike : com.hex.kagune.control.pale.PaleStrike.values()) {
            double lowest = Double.MAX_VALUE;
            for (double age = 0.0; age <= strike.duration; age += 0.5) {
                com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.AgentPose.brawl(strike, age);
                // Dawn's point, its butt (its model's ends along its length) where the body draws them, against its feet (24 below the neck).
                for (float[] end : new float[][] {{-5.7F, 8.0F, 8.0F}, {21.7F, 8.0F, 8.0F}, {-20.0F, 8.0F, 8.0F}}) {
                    double[] at = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, dawnHold, false, end);
                    lowest = Math.min(lowest, 24.0 - p.drop - at[1]);
                }
            }
            check(lowest > 2.0, "the White Reaper's " + strike + ": Dawn carried clear of the ground (" + Math.round(lowest) + " pixels above it at its lowest)");
        }
        // 4.1.27 (the owner: "it shouldn't be a tentacle. It should be like a smooth part of the blade, as if the blade grew out of the
        // ground"): what his ground strike raises is Dawn's blades, the great one as tall as the strike reaches, the rest round it within what
        // it catches, each breaking the ground in turn; drawn as Dawn's blade mesh, no tendril tube.
        double[][] blades = com.hex.kagune.control.npc.ReaperTendril.blades(11);
        boolean inside = true;
        for (int i = 0; i < blades.length; i++) {
            inside &= Math.hypot(blades[i][0], blades[i][1]) < com.hex.kagune.control.npc.ReaperTendril.RADIUS && blades[i][4] > 1.0
                && blades[i][4] <= com.hex.kagune.control.npc.ReaperTendril.HEIGHT && (i == 0 || blades[i][6] > blades[i - 1][6]);
        }
        check(blades.length == 4 && blades[0][4] == com.hex.kagune.control.npc.ReaperTendril.HEIGHT && inside,
            "the ground strike: Dawn's blades grow out of its mark, inside what it catches, one after another");
        com.hex.kagune.control.quinque.ArmMesh.Mesh rising = com.hex.kagune.control.quinque.QuinqueDawnMesh.risingBlade();
        float top = -Float.MAX_VALUE;
        float bottom = Float.MAX_VALUE;
        for (int i = 1; i < rising.pos.length; i += 3) {
            top = Math.max(top, rising.pos[i]);
            bottom = Math.min(bottom, rising.pos[i]);
        }
        check(rising.quads > 1000 && Math.abs(top - 1.0F) < 1.0E-4F && Math.abs(bottom) < 1.0E-4F, "Dawn's rising blade: smooth, ground to point in its unit frame");
        try {
            String drawn = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/render/ReaperTendrils.java"));
            check(drawn.contains("QuinqueDawnMesh.risingBlade()") && !drawn.contains("emitTubeLegacy"), "the ground strike drawn as Dawn's blades, not a tendril");
        } catch (java.io.IOException e) {
            check(false, "ReaperTendrils.java can be read");
        }
        // 4.1.23 (the owner: "he drops Dawn 100% guaranteed"): every time; everyone who struck him in the last five minutes shares his fall.
        check(com.hex.kagune.control.npc.AgentRules.dawnChance(0) == 1.0 && com.hex.kagune.control.npc.AgentRules.dawnChance(3) == 1.0,
            "Dawn: every time the White Reaper falls");
        check(com.hex.kagune.control.npc.AgentRules.REAPER_SHARE_TICKS == 6000L
            && com.hex.kagune.control.npc.AgentRules.sharesReaper(1000L, 1000L) && com.hex.kagune.control.npc.AgentRules.sharesReaper(1000L, 7000L)
            && !com.hex.kagune.control.npc.AgentRules.sharesReaper(1000L, 7001L) && !com.hex.kagune.control.npc.AgentRules.sharesReaper(5000L, 4000L),
            "Slain Reaper: a blow in the last five minutes (6000 ticks) shares his fall, an older one doesn't");
        try {
            String brain = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java"));
            String json = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/hexkagune/advancements/slain_reaper.json"));
            check(brain.contains("m_135988_(slain, \"slain\")") && json.contains("\"slain\"") && json.contains("minecraft:impossible")
                && json.contains("Slain Reaper"), "Slain Reaper: awarded by code on its one criterion, which nothing else grants");
            check(!brain.contains("if (byPlayer && agent.m_217043_().m_188500_() < AgentRules.dawnChance(looting))"),
                "Dawn: dropped however he falls, not only by a player's hand");
        } catch (java.io.IOException e) {
            check(false, "Slain Reaper: its files can be read");
        }
        check(com.hex.kagune.control.npc.AgentRules.rationChance(0) > com.hex.kagune.control.npc.AgentRules.caseChance(Quinque.BLADE, null, false, 0)
            && com.hex.kagune.control.npc.AgentRules.rationChance(3) < 0.25, "a ration: uncommon, though likelier than any case");
        java.util.Set<String> cookbook = new java.util.HashSet<>();
        try (java.util.stream.Stream<java.nio.file.Path> files = Files.list(RESOURCES.resolve("data/hexkagune/recipes/kitchen"))) {
            for (java.nio.file.Path file : (Iterable<java.nio.file.Path>)files::iterator) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"item\"\\s*:\\s*\"([^\"]+)\"").matcher(Files.readString(file));
                while (m.find()) {
                    cookbook.add(m.group(1));
                }
            }
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            com.hex.kagune.control.npc.AgentRules.Ration r = com.hex.kagune.control.npc.AgentRules.ration(i / 1000.0);
            seen.add(r.item());
            for (double roll : new double[] {0.0, 0.5, 0.999, 1.0}) {
                int n = com.hex.kagune.control.npc.AgentRules.rations(r, roll);
                check(n >= r.least() && n <= r.most(), r.item() + ": " + n);
            }
        }
        for (com.hex.kagune.control.npc.AgentRules.Ration r : com.hex.kagune.control.npc.AgentRules.RATIONS) {
            check(seen.contains(r.item()), r.item() + ": comes up");
            check(cookbook.contains(r.item()), r.item() + ": something the cookbook's recipes take");
            check(!r.item().contains("meat") && r.least() >= 1 && r.most() <= 4, r.item() + ": a ration, not a feast");
        }
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
        // 2.2.5: a male player's build, slimmer than his (the owner: "a little too large ... for the player. Keep the same for Arima").
        double[][] player = com.hex.kagune.control.npc.ReaperBuild.PLAYER;
        check(rows[1][1] == 4.68 && rows[2][2] == 3.05 && com.hex.kagune.control.npc.ReaperBuild.ARM_OUT == 0.75F, "the Reaper's own build unchanged");
        check(player.length == rows.length, "a player's: the same rows");
        double playerWidest = 0.0;
        double playerFurthest = 0.0;
        for (int i = 0; i < rows.length; i++) {
            check(player[i][0] == rows[i][0] && player[i][1] <= rows[i][1] && player[i][2] <= rows[i][2] && player[i][3] <= rows[i][3],
                "a player's no bigger than his anywhere (row " + i + ")");
            playerWidest = Math.max(playerWidest, player[i][1]);
            playerFurthest = Math.max(playerFurthest, player[i][2]);
        }
        check(playerWidest > 4.2 && playerWidest < widest - 0.2, "a player's a little less broad (" + playerWidest + ")");
        check(playerFurthest - 2.0 > 0.4 && playerFurthest - 2.0 < (furthest - 2.0) * 0.75, "a player's chest out less far (" + playerFurthest + ")");
        check(player[1][1] > player[player.length - 1][1] + 0.3 && player[1][2] > player[player.length - 1][2] + 0.4, "still a V");
        check(java.util.Arrays.equals(player[player.length - 1], waist), "the same at the waist");
        check(4.0 + com.hex.kagune.control.npc.ReaperBuild.PLAYER_ARM_OUT >= playerWidest
            && com.hex.kagune.control.npc.ReaperBuild.PLAYER_ARM_OUT < com.hex.kagune.control.npc.ReaperBuild.ARM_OUT, "a player's arms clear of it, closer in");
        check(com.hex.kagune.control.npc.ReaperBuild.back(player, 3.0, true) < com.hex.kagune.control.npc.ReaperBuild.back(3.0, true)
            && com.hex.kagune.control.npc.ReaperBuild.back(player, 3.0, true) > 2.25, "a player's back: less far out, still outside the torso's jacket");
        int[] quads = new int[1];
        com.hex.kagune.control.npc.ReaperBuild.chest(player, true, (corners, uv, normal) -> quads[0]++);
        check(quads[0] == (rows.length - 1) * 4 + 1, "a player's chest drawn whole");
        // 2.3.3: a form's armour fitted to the bare torso, widened to sit over his chest (the chest is kept in every form).
        float fx = com.hex.kagune.control.npc.ReaperBuild.PLAYER_FIT_X;
        float fz = com.hex.kagune.control.npc.ReaperBuild.PLAYER_FIT_Z;
        check(fx > 1.0F && fx < 1.2F && fz > 1.0F && fz < 1.4F, "armour widened a little over his chest (" + fx + ", " + fz + ")");
        check((4.0 + com.hex.kagune.control.npc.ReaperBuild.JACKET) * fx >= playerWidest + com.hex.kagune.control.npc.ReaperBuild.JACKET - 1.0E-6
            && (2.0 + com.hex.kagune.control.npc.ReaperBuild.JACKET) * fz >= playerFurthest + com.hex.kagune.control.npc.ReaperBuild.JACKET - 1.0E-6,
            "the widened armour covers the chest, jacket and all");
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
        // 4.1.24: never in the wild (he is at home in his mansion, the world's one); weights still summed for the rank table's checks.
        check(AgentRules.REAPER_CHANCE == 0.0 && weights > 0.0 && AgentRules.REAPER_HOME_STROLL > 0.0
            && AgentRules.REAPER_HOME_LEASH > AgentRules.REAPER_HOME_STROLL, "4.1.24: the White Reaper never turns up in the wild; he keeps to his garden");
        try {
            String spawner = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/AgentSpawner.java"));
            check(!spawner.contains("prepareReaper"), "4.1.24: no wild appearance makes a White Reaper");
        } catch (java.io.IOException e) {
            check(false, "4.1.24: AgentSpawner.java can be read");
        }
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
