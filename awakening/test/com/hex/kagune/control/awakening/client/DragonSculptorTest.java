package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.DragonRig;
import com.hex.kagune.control.awakening.DragonTimeline;
import com.hex.kagune.control.awakening.S2CDragonFrame;
import com.hex.kagune.control.awakening.V3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Sculpts the Dragon at every stage of the sequence into a counting buffer: geometry must stay
 * finite, close to the body, made of whole quads, and inside a polygon budget a client can afford.
 */
public final class DragonSculptorTest {
    private static final int QUAD_BUDGET = 32_000;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Counter implements VertexConsumer {
        int vertices;
        int open;
        double farthest;

        @Override
        public VertexConsumer vertex(Matrix4f pose, float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) throw new AssertionError("non-finite vertex");
            farthest = Math.max(farthest, Math.sqrt(x * x + y * y + z * z));
            open++;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            check(r >= 0 && r < 256 && g >= 0 && g < 256 && b >= 0 && b < 256 && a >= 0 && a < 256, "colour out of range");
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int overlay) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int light) {
            return this;
        }

        @Override
        public VertexConsumer normal(Matrix3f normal, float x, float y, float z) {
            check(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z), "non-finite normal");
            return this;
        }

        @Override
        public void endVertex() {
            check(open == 1, "vertex not terminated exactly once");
            open = 0;
            vertices++;
        }
    }

    /**
     * The camera must never render from inside the body: every point on and inside the body tubes,
     * the necks and the jaws is moved to where {@link DragonPose#inside} no longer holds.
     */
    private static int checkCameraClearance(DragonPose pose, int tick) {
        int tested = 0;
        java.util.SplittableRandom random = new java.util.SplittableRandom(tick);
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            if (!pose.born(i)) continue;
            for (int k = 0; k < 4; k++) {
                V3 offset = new V3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(2.0 * pose.radii[i]);
                V3 world = pose.world(pose.segments[i].add(offset));
                V3 clear = pose.outside(world, 2.0);
                check(Double.isFinite(clear.x()) && Double.isFinite(clear.y()) && Double.isFinite(clear.z()), "finite camera position");
                check(!pose.inside(clear, 1.9), "camera left inside the body at tick " + tick + " segment " + i);
                tested++;
            }
        }
        for (int h = 0; h < DragonRig.HEADS; h++) {
            if (pose.radii[DragonRig.HEAD_ROOTS[h]] < 0.3) continue;
            V3 clear = pose.outside(pose.world(pose.headTips[h]), 2.0);
            check(!pose.inside(clear, 1.9), "camera left inside a head at tick " + tick);
            tested++;
        }
        return tested;
    }

    public static void main(String[] args) {
        DragonRig.Ground hills = new DragonRig.Ground() {
            @Override
            public double surface(double x, double z) {
                return 64.0 + 5.0 * Math.sin(x * 0.04) * Math.cos(z * 0.05);
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
        V3 anchor = new V3(100.5, 64.0, -40.5);
        DragonRig rig = new DragonRig(anchor, 1234L);
        UUID owner = new UUID(3L, 4L);
        int[] lights = new int[DragonRig.SEGMENTS];
        java.util.Arrays.fill(lights, 0xF000F0);
        Matrix4f pose = new Matrix4f();
        Matrix3f normal = new Matrix3f();
        S2CDragonFrame previous = null;
        int peak = 0;
        int sculpted = 0;
        int cleared = 0;
        // 1.0.32: its legs walk by how far it really crawls, on the real ground (here the hills).
        DragonPose.Gait gait = new DragonPose.Gait();
        double steps = 0.0;
        for (int tick = 0; tick < DragonTimeline.END_TICK; tick++) {
            List<DragonRig.Prey> prey = new ArrayList<>();
            prey.add(new DragonRig.Prey(7, anchor.add(18.0, 0.0, 6.0), 0.6, 1.8, true, true));
            prey.add(new DragonRig.Prey(8, anchor.add(-25.0, 0.0, -14.0), 0.9, 1.4, false, true));
            rig.step(tick, prey, hills);
            S2CDragonFrame frame = S2CDragonFrame.capture(owner, rig, tick >= DragonTimeline.SHATTER_TICK ? rig.emergence(hills) : null);
            gait.advance(previous, frame);
            if (previous != null && (tick % 7 == 0 || tick < 50 || tick > DragonTimeline.RAMPAGE_END - 5)) {
                Counter counter = new Counter();
                DragonPose interpolated = new DragonPose(previous, frame, 0.5F);
                gait.apply(interpolated, 0.5F);
                check(interpolated.steps >= steps, "the legs never step backward in time");
                steps = interpolated.steps;
                interpolated.floor = (x, y, z) -> hills.terrain(x + interpolated.anchorX, z + interpolated.anchorZ) - interpolated.anchorY;
                DragonSculptor.sculpt(new DragonMesh(counter, pose, normal), interpolated, tick + 0.5, 20, lights);
                check(counter.vertices % 4 == 0, "geometry must be whole quads");
                check(counter.farthest < DragonRig.LEASH + DragonRig.BODY_LENGTH + 60.0, "geometry flew away from the Dragon: " + counter.farthest);
                peak = Math.max(peak, counter.vertices / 4);
                sculpted++;
                cleared += checkCameraClearance(interpolated, tick);
                if (tick == DragonTimeline.SHATTER_TICK + 5) check(counter.vertices == 0, "nothing of the body remains after it shatters");
            }
            previous = frame;
        }
        check(DragonPose.crawl(previous, previous) == 0.0, "a body that has not moved has not crawled");
        check(steps > 20.0, "its legs should walk it through the rampage: " + steps + " steps");
        check(peak > 5_000, "the Dragon should be a detailed colossus, peak quads " + peak);
        check(peak < QUAD_BUDGET, "polygon budget exceeded: " + peak);
        check(cleared > 10_000, "camera clearance should be exercised: " + cleared);
        System.out.println("Verified: " + sculpted + " sculpted frames across the whole sequence, finite geometry, peak " + peak + " quads (budget " + QUAD_BUDGET + ");"
                + " " + cleared + " camera positions inside or against the body all pushed clear of it.");
    }
}
