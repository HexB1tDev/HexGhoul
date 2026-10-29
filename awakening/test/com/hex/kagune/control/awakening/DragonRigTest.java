package com.hex.kagune.control.awakening;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/** Plain-Java checks of the Dragon's schedule and kinematics; runs during the build without Minecraft. */
public final class DragonRigTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final DragonRig.Ground HILLS = new DragonRig.Ground() {
        @Override
        public double surface(double x, double z) {
            return 64.0 + 6.0 * Math.sin(x * 0.05) * Math.cos(z * 0.04);
        }

        @Override
        public double minY() {
            return -64.0;
        }

        @Override
        public double maxY() {
            return 320.0;
        }
    };

    private static final DragonRig.Ground FLAT = new DragonRig.Ground() {
        @Override
        public double surface(double x, double z) {
            return 64.0;
        }

        @Override
        public double minY() {
            return -64.0;
        }

        @Override
        public double maxY() {
            return 320.0;
        }
    };

    /** Flat land with a 6x6 hut five blocks tall at the origin and a 2x2 pit at (20, 7). */
    private static final DragonRig.Ground HUT_AND_PIT = new DragonRig.Ground() {
        @Override
        public double surface(double x, double z) {
            if (Math.abs(x) < 3.0 && Math.abs(z) < 3.0) return 69.0;
            if (x >= 19.0 && x < 21.0 && z >= 6.0 && z < 8.0) return 61.0;
            return 64.0;
        }

        @Override
        public double minY() {
            return -64.0;
        }

        @Override
        public double maxY() {
            return 320.0;
        }
    };

    /** Pairs checked for passing over each other in the last run. */
    private static int lastCrossings;

    private static V3[] body(DragonRig rig) {
        V3[] segments = new V3[DragonRig.SEGMENTS];
        for (int i = 0; i < segments.length; i++) segments[i] = rig.segment(i);
        return segments;
    }

    /** One run of the whole sequence with the given prey per tick; returns the strike counts. */
    private static Map<DragonRig.StrikeKind, Integer> run(String name, long seed, DragonRig.Ground ground, IntFunction<List<DragonRig.Prey>> preyAt, boolean hugging) {
        V3 anchor = new V3(0.5, ground.surface(0.5, 0.5), 0.5);
        DragonRig rig = new DragonRig(anchor, seed);
        Map<DragonRig.StrikeKind, Integer> counts = new EnumMap<>(DragonRig.StrikeKind.class);
        double maxLeash = 0.0;
        double worstHug = 1.0;
        double worstTurn = 1.0;
        int crossings = 0;
        int mainBites = 0;
        V3[] previousForward = null;
        for (int tick = 0; tick < DragonTimeline.END_TICK; tick++) {
            List<DragonRig.Prey> prey = preyAt.apply(tick);
            List<DragonRig.Strike> strikes = rig.step(tick, prey, ground);
            V3[] s = body(rig);
            V3 front = DragonBody.flatForward(s, 0);
            for (DragonRig.Strike strike : strikes) {
                counts.merge(strike.kind(), 1, Integer::sum);
                check(Double.isFinite(strike.position().x()) && Double.isFinite(strike.position().y()), name + ": finite strike");
                if (strike.kind() == DragonRig.StrikeKind.BITE && strike.source() == 0) {
                    mainBites++;
                    check(strike.position().sub(s[0]).dot(front) > 0.0, name + ": the main head bit behind the body at tick " + tick);
                }
            }
            V3 lead = rig.lead();
            check(Double.isFinite(lead.x()) && Double.isFinite(lead.y()) && Double.isFinite(lead.z()), name + ": finite lead");
            maxLeash = Math.max(maxLeash, lead.sub(anchor).horizontalLength());
            if (tick == 0) check(lead.y() < ground.terrain(lead.x(), lead.z()) - 4.0, name + ": the Dragon starts underground");
            if (tick == DragonRig.ARCH_TICKS / 2) check(lead.y() > ground.terrain(lead.x(), lead.z()) + 4.0, name + ": the head bursts out in an arch");
            boolean rampage = tick >= DragonTimeline.RAMPAGE_START && tick < DragonTimeline.RAMPAGE_END;
            if (tick >= DragonRig.ARCH_TICKS + 20 && rampage) {
                check(lead.y() <= DragonRig.groundBelow(ground, lead.x(), lead.z()) + 1.5, name + ": the head crawls instead of flying at tick " + tick);
            }
            if (rampage) {
                check(rig.heads[0].tip.sub(s[0]).dot(front) > 0.0, name + ": the main head swung behind the front of the body at tick " + tick);
                for (int i = 1; i < DragonRig.SEGMENTS; i++) {
                    if (rig.radius(i) < 0.05) continue;
                    double horizontal = s[i].sub(s[i - 1]).horizontalLength();
                    check(horizontal <= DragonRig.SPACING + 0.05, name + ": segments stretched apart horizontally: " + horizontal);
                    check(s[i].distance(s[i - 1]) <= 1.6 * DragonRig.SPACING, name + ": segments stretched apart: " + s[i].distance(s[i - 1]));
                }
                for (int h = 1; h < DragonRig.HEADS; h++) {
                    int root = DragonRig.HEAD_ROOTS[h];
                    check(rig.headRoot(rig.heads[h]).y() >= s[root].y(), name + ": side heads grow from the upper body");
                }
                for (DragonRig.Tentacle t : rig.tentacles) check(rig.tentacleRoot(t).y() >= s[t.root].y(), name + ": tentacles grow from the upper body");
                // Where the body crosses itself, the newer part passes over the older part.
                for (int i = 0; i < DragonRig.SEGMENTS; i++) {
                    if (rig.radius(i) < 0.05) continue;
                    for (int j = i + 5; j < DragonRig.SEGMENTS && rig.radius(j) >= 0.05; j++) {
                        double reach = rig.radius(i) + rig.radius(j);
                        if ((j - i) * DragonRig.SPACING < 1.5 * reach) continue;
                        double d = s[i].sub(s[j]).horizontalLength();
                        if (d >= 0.5 * reach) continue;
                        crossings++;
                        double clearance = DragonRig.CLEARANCE * reach * (1.0 - (d / reach) * (d / reach));
                        check(s[i].y() - s[j].y() >= clearance - 1.0E-6, name + ": the body passed through itself at tick " + tick);
                    }
                }
            }
            if (tick > 360 && rampage) {
                int hugging10 = 0;
                int counted = 0;
                for (int i = 0; i < DragonRig.SEGMENTS; i++) check(rig.radius(i) > 0.5, name + ": full body has emerged by 18 s");
                for (int i = 10; i < DragonRig.SEGMENTS; i++) {
                    double r = rig.radius(i);
                    // A segment lying across another part of the body is over (or under) it by design: not counted.
                    boolean crossing = false;
                    for (int j = 0; j < DragonRig.SEGMENTS && !crossing; j++) {
                        double reach = r + rig.radius(j);
                        if (Math.abs(j - i) * DragonRig.SPACING < 1.5 * reach) continue;
                        crossing = s[i].sub(s[j]).horizontalLength() < reach;
                    }
                    if (crossing) continue;
                    counted++;
                    double land = ground.terrain(s[i].x(), s[i].z());
                    double bottom = s[i].y() - r;
                    if (bottom >= land - 0.6 * r && bottom <= land + 0.5) hugging10++;
                }
                double share = counted == 0 ? 1.0 : hugging10 / (double) counted;
                worstHug = Math.min(worstHug, share);
                if (hugging) check(share >= 0.80, name + ": the body left the ground at tick " + tick + " (" + share + " hugging)");
            }
            if (tick < DragonTimeline.SHATTER_TICK) {
                V3[] forward = new V3[DragonRig.SEGMENTS];
                for (int i = 0; i < forward.length; i++) forward[i] = DragonBody.forward(s, i);
                if (previousForward != null) {
                    for (int i = 0; i < forward.length; i++) {
                        if (rig.radius(i) < 0.05) continue;
                        double dot = forward[i].dot(previousForward[i]);
                        worstTurn = Math.min(worstTurn, dot);
                        check(dot > 0.5, name + ": segment " + i + " flipped at tick " + tick + " (dot " + dot + ")");
                    }
                }
                previousForward = forward;
            }
            if (tick >= DragonTimeline.SHATTER_TICK) {
                for (int i = 0; i < DragonRig.SEGMENTS; i++) check(rig.radius(i) == 0.0, name + ": body gone after shatter");
            }
        }
        check(maxLeash <= DragonRig.LEASH + 10.0, name + ": lead escaped the leash: " + maxLeash);
        check(counts.getOrDefault(DragonRig.StrikeKind.ERUPTION, 0) == 1, name + ": one eruption");
        check(counts.getOrDefault(DragonRig.StrikeKind.SHOCKWAVE, 0) == 1, name + ": one shockwave");
        V3 emergence = rig.emergence(ground);
        check(emergence.sub(anchor).horizontalLength() < DragonRig.LEASH + DragonRig.BODY_LENGTH, name + ": emergence near the fall");
        lastCrossings = crossings;
        System.out.println(name + ": leash " + String.format("%.1f", maxLeash) + ", worst hug " + String.format("%.2f", worstHug)
                + ", worst turn " + String.format("%.2f", worstTurn) + ", crossing checks " + crossings + ", main-head bites " + mainBites + ", " + counts);
        return counts;
    }

    public static void main(String[] args) {
        check(DragonTimeline.RAMPAGE_TICKS == 1200, "rampage must last exactly 60 seconds");
        check(DragonTimeline.RAMPAGE_END - DragonTimeline.RAMPAGE_START == 1200, "rampage window must be 1200 ticks");
        check(DragonTimeline.phaseAt(DragonTimeline.RAMPAGE_END - 1) == DragonTimeline.Phase.RAMPAGE, "last rampage tick");
        check(DragonTimeline.phaseAt(DragonTimeline.RAMPAGE_END) == DragonTimeline.Phase.DESTABILIZE, "destabilizing starts at 60 s");
        check(DragonTimeline.phaseAt(DragonTimeline.END_TICK) == DragonTimeline.Phase.DONE, "sequence ends");
        check(!DragonTimeline.attacking(DragonTimeline.RAMPAGE_END), "no new attacks once destabilizing");

        // The ground under the body: exact on slopes, blind to small obstacles and ditches.
        check(Math.abs(DragonRig.groundBelow(HILLS, 30.5, -12.5) - HILLS.surface(30.5, -12.5)) < 0.15, "ground estimate follows hills");
        check(DragonRig.groundBelow(HUT_AND_PIT, 0.5, 0.5) == 64.0, "the body ploughs through a hut instead of climbing it");
        check(DragonRig.groundBelow(HUT_AND_PIT, 20.0, 7.0) == 64.0, "the body bridges a small pit");

        for (long seed = 1; seed <= 6; seed++) {
            Map<DragonRig.StrikeKind, Integer> counts = run("hills " + seed, seed * 7919L, HILLS, tick -> {
                List<DragonRig.Prey> prey = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                    double angle = i * 0.52 + tick * 0.004;
                    double distance = 14.0 + (i % 4) * 11.0;
                    double x = 0.5 + Math.cos(angle) * distance;
                    double z = 0.5 + Math.sin(angle) * distance;
                    prey.add(new DragonRig.Prey(100 + i, new V3(x, HILLS.surface(x, z), z), 0.6, 1.8, i < 2, true));
                }
                return prey;
            }, true);
            check(counts.getOrDefault(DragonRig.StrikeKind.BITE, 0) > 40, "heads bite during the rampage: " + counts);
            check(counts.getOrDefault(DragonRig.StrikeKind.SLAM, 0) > 5, "tentacles slam during the rampage: " + counts);
            check(counts.getOrDefault(DragonRig.StrikeKind.LASH, 0) > 20, "tentacles lash during the rampage: " + counts);
        }
        for (long seed = 1; seed <= 3; seed++) run("empty hills " + seed, seed * 104729L, HILLS, tick -> List.of(), true);
        // A player circling close to the fall drags the head round and round, so the body must climb over itself.
        for (int direction = -1; direction <= 1; direction += 2) {
            int sense = direction;
            run("orbiting prey " + sense, 5L + sense, FLAT, tick -> {
                double angle = tick * 0.02 * sense;
                double radius = 6.0 + 4.0 * Math.sin(tick * 0.01);
                return List.of(new DragonRig.Prey(1, new V3(0.5 + Math.cos(angle) * radius, 64.0, 0.5 + Math.sin(angle) * radius), 0.6, 1.8, true, true));
            }, false);
            check(lastCrossings > 100, "the orbiting prey should make the body cross itself: " + lastCrossings);
        }
        System.out.println("Verified: exact 60 s rampage, a ground-hugging crawl that leads with the head, never flips, climbs over itself and stays leashed; heads and tentacles attack; collapse and shatter.");
    }
}
