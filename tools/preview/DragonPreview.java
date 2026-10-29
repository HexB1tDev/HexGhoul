package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.DragonRig;
import com.hex.kagune.control.awakening.DragonTimeline;
import com.hex.kagune.control.awakening.S2CDragonFrame;
import com.hex.kagune.control.awakening.V3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Offline stills of the Dragon: runs the real rig and the real sculptor and rasterizes the mesh in
 * software, with the hills it crawls on. A development aid, not part of the mod or the build.
 *
 * <p>Build the mod once ({@code python3 build.py}), then:
 * <pre>
 *   javac -cp build/awakening-mojang:build/stub-classes -d build/preview tools/preview/DragonPreview.java
 *   java -cp build/preview:build/awakening-mojang:build/stub-classes com.hex.kagune.control.awakening.client.DragonPreview out-dir [seed] [ticks...]
 * </pre>
 * Each tick is written as {@code dragon-<tick>-side.png} and {@code dragon-<tick>-top.png}.
 */
public final class DragonPreview {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;

    static final DragonRig.Ground HILLS = new DragonRig.Ground() {
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

    /** Collects every emitted quad in world coordinates relative to the pose anchor. */
    private static final class Collector implements VertexConsumer {
        final List<double[]> quads = new ArrayList<>();
        final double[] current = new double[4 * 3];
        final int[] colors = new int[4];
        final boolean[] bright = new boolean[4];
        int index;

        @Override
        public VertexConsumer vertex(Matrix4f pose, float x, float y, float z) {
            current[index * 3] = x;
            current[index * 3 + 1] = y;
            current[index * 3 + 2] = z;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            colors[index] = r << 16 | g << 8 | b;
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
            bright[index] = light == DragonMesh.FULL_BRIGHT;
            return this;
        }

        @Override
        public VertexConsumer normal(Matrix3f normal, float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
            if (++index == 4) {
                double[] quad = new double[12 + 2];
                System.arraycopy(current, 0, quad, 0, 12);
                quad[12] = colors[0];
                quad[13] = bright[0] ? 1.0 : 0.0;
                quads.add(quad);
                index = 0;
            }
        }
    }

    private static final class Raster {
        final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        final double[] depth = new double[WIDTH * HEIGHT];
        final V3 eye;
        final V3 forward;
        final V3 right;
        final V3 up;
        final double focal;

        Raster(V3 eye, V3 target, double fovDegrees) {
            this.eye = eye;
            this.forward = target.sub(eye).normalize();
            V3 r = forward.cross(V3.UP).normalize();
            if (r.length() < 1.0E-6) r = new V3(1.0, 0.0, 0.0);
            this.right = r;
            this.up = right.cross(forward).normalize();
            this.focal = (HEIGHT / 2.0) / Math.tan(Math.toRadians(fovDegrees) / 2.0);
            java.util.Arrays.fill(depth, Double.MAX_VALUE);
            for (int y = 0; y < HEIGHT; y++) {
                int sky = mix(0x9FB8D0, 0xD8E2EA, y / (double) HEIGHT);
                for (int x = 0; x < WIDTH; x++) image.setRGB(x, y, sky);
            }
        }

        double[] project(V3 world) {
            V3 d = world.sub(eye);
            double z = d.dot(forward);
            if (z < 0.2) return null;
            return new double[] {WIDTH / 2.0 + d.dot(right) * focal / z, HEIGHT / 2.0 - d.dot(up) * focal / z, z};
        }

        void triangle(V3 a, V3 b, V3 c, int rgb, boolean bright) {
            double[] pa = project(a);
            double[] pb = project(b);
            double[] pc = project(c);
            if (pa == null || pb == null || pc == null) return;
            V3 n = b.sub(a).cross(c.sub(a)).normalize();
            if (n.length() < 1.0E-9) return;
            double light = bright ? 1.0 : 0.35 + 0.65 * Math.abs(n.dot(new V3(0.35, 0.85, 0.4).normalize()));
            int shaded = scale(rgb, bright ? 1.0 : light);
            int minX = (int) Math.max(0, Math.floor(Math.min(pa[0], Math.min(pb[0], pc[0]))));
            int maxX = (int) Math.min(WIDTH - 1, Math.ceil(Math.max(pa[0], Math.max(pb[0], pc[0]))));
            int minY = (int) Math.max(0, Math.floor(Math.min(pa[1], Math.min(pb[1], pc[1]))));
            int maxY = (int) Math.min(HEIGHT - 1, Math.ceil(Math.max(pa[1], Math.max(pb[1], pc[1]))));
            double area = (pb[0] - pa[0]) * (pc[1] - pa[1]) - (pc[0] - pa[0]) * (pb[1] - pa[1]);
            if (Math.abs(area) < 1.0E-9) return;
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double px = x + 0.5;
                    double py = y + 0.5;
                    double w0 = ((pb[0] - px) * (pc[1] - py) - (pc[0] - px) * (pb[1] - py)) / area;
                    double w1 = ((pc[0] - px) * (pa[1] - py) - (pa[0] - px) * (pc[1] - py)) / area;
                    double w2 = 1.0 - w0 - w1;
                    if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                    double z = 1.0 / (w0 / pa[2] + w1 / pb[2] + w2 / pc[2]);
                    int i = y * WIDTH + x;
                    if (z >= depth[i]) continue;
                    depth[i] = z;
                    image.setRGB(x, y, shaded);
                }
            }
        }

        void quads(List<double[]> quads, V3 origin) {
            for (double[] q : quads) {
                V3 a = new V3(q[0], q[1], q[2]).add(origin);
                V3 b = new V3(q[3], q[4], q[5]).add(origin);
                V3 c = new V3(q[6], q[7], q[8]).add(origin);
                V3 d = new V3(q[9], q[10], q[11]).add(origin);
                triangle(a, b, c, (int) q[12], q[13] > 0.5);
                triangle(a, c, d, (int) q[12], q[13] > 0.5);
            }
        }

        void ground(DragonRig.Ground ground, V3 center, double half, double step) {
            for (double x = center.x() - half; x < center.x() + half; x += step) {
                for (double z = center.z() - half; z < center.z() + half; z += step) {
                    V3 a = new V3(x, ground.surface(x, z), z);
                    V3 b = new V3(x + step, ground.surface(x + step, z), z);
                    V3 c = new V3(x + step, ground.surface(x + step, z + step), z + step);
                    V3 d = new V3(x, ground.surface(x, z + step), z + step);
                    boolean check = ((int) Math.floor(x / 8.0) + (int) Math.floor(z / 8.0)) % 2 == 0;
                    int rgb = check ? 0x5E8C3A : 0x55813A;
                    triangle(a, b, c, rgb, false);
                    triangle(a, c, d, rgb, false);
                }
            }
        }
    }

    private static int scale(int rgb, double k) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * k);
        int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * k);
        int b = (int) Math.min(255, (rgb & 0xFF) * k);
        return r << 16 | g << 8 | b;
    }

    private static int mix(int a, int b, double t) {
        return DragonPalette.mix(a, b, t);
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview-out");
        out.mkdirs();
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 7919L;
        List<Integer> wanted = new ArrayList<>();
        for (int i = 2; i < args.length; i++) wanted.add(Integer.parseInt(args[i]));
        if (wanted.isEmpty()) wanted = List.of(10, 24, 40, 120, 400, 800, 1250, 1300);
        V3 anchor = new V3(0.5, HILLS.surface(0.5, 0.5), 0.5);
        DragonRig rig = new DragonRig(anchor, seed);
        UUID owner = new UUID(1L, 2L);
        int[] lights = new int[DragonRig.SEGMENTS];
        java.util.Arrays.fill(lights, 0xF00000);
        S2CDragonFrame previous = null;
        DragonPose.Gait gait = new DragonPose.Gait();
        int last = wanted.stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int tick = 0; tick <= last; tick++) {
            List<DragonRig.Prey> prey = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                double angle = i * 0.52 + tick * 0.004;
                double distance = 14.0 + (i % 4) * 11.0;
                double x = anchor.x() + Math.cos(angle) * distance;
                double z = anchor.z() + Math.sin(angle) * distance;
                prey.add(new DragonRig.Prey(100 + i, new V3(x, HILLS.surface(x, z), z), 0.6, 1.8, i < 2, true));
            }
            rig.step(tick, prey, HILLS);
            S2CDragonFrame frame = S2CDragonFrame.capture(owner, rig, tick >= DragonTimeline.SHATTER_TICK ? rig.emergence(HILLS) : null);
            gait.advance(previous, frame);
            if (wanted.contains(tick)) {
                DragonPose pose = new DragonPose(previous == null ? frame : previous, frame, 1.0F);
                gait.apply(pose, 1.0F);
                pose.floor = (x, y, z) -> HILLS.terrain(x + pose.anchorX, z + pose.anchorZ) - pose.anchorY;
                // -Djaw=1 holds every mouth that far open (0 shut, 1 gaping), to see the bite without hunting for the frame.
                if (System.getProperty("jaw") != null) java.util.Arrays.fill(pose.jaws, Double.parseDouble(System.getProperty("jaw")));
                Collector collector = new Collector();
                DragonSculptor.sculpt(new DragonMesh(collector, new Matrix4f(), new Matrix3f()), pose, tick, 20, lights);
                V3 origin = new V3(pose.anchorX, pose.anchorY, pose.anchorZ);
                V3 focus = pose.world(pose.centroid().lerp(pose.headTips[0], 0.55));
                V3 heading = rig.heading();
                V3 sideways = new V3(-heading.z(), 0.0, heading.x());
                double distance = tick < DragonTimeline.RAMPAGE_START ? 40.0 : 95.0;
                V3 sideEye = focus.add(sideways.scale(distance)).add(0.0, distance * 0.45, 0.0);
                render(new Raster(sideEye, focus, 60.0), collector, origin, focus, new File(out, "dragon-" + tick + "-side.png"));
                V3 topEye = focus.add(0.0, 150.0, 0.01);
                render(new Raster(topEye, focus, 70.0), collector, origin, focus, new File(out, "dragon-" + tick + "-top.png"));
                // 1.0.29: the heads close: the main one from in front and to its side, a side one beside it.
                for (int h = 0; h < 2; h++) {
                    V3 tip = pose.world(pose.headTips[h]);
                    V3 root = pose.world(pose.headRoot(h));
                    V3 ahead = tip.sub(root).normalize();
                    V3 across = new V3(-ahead.z(), 0.0, ahead.x()).normalize();
                    double r = pose.headRadius(h);
                    V3 headEye = tip.add(ahead.scale(r * 5.5)).add(across.scale(r * 4.0)).add(0.0, r * 2.2, 0.0);
                    Raster close = new Raster(headEye, tip.add(ahead.scale(r * 0.9)), 50.0);
                    close.ground(HILLS, tip, 40.0, 1.0);
                    close.quads(collector.quads, origin);
                    ImageIO.write(close.image, "png", new File(out, "dragon-" + tick + "-head" + h + ".png"));
                    if (h == 0) {
                        V3 faceEye = tip.add(ahead.scale(r * 7.5)).add(across.scale(r * 1.2)).add(0.0, r * 1.2, 0.0);
                        Raster face = new Raster(faceEye, tip.add(ahead.scale(r * 0.5)), 50.0);
                        face.ground(HILLS, tip, 40.0, 1.0);
                        face.quads(collector.quads, origin);
                        ImageIO.write(face.image, "png", new File(out, "dragon-" + tick + "-face.png"));
                        V3 backEye = tip.sub(ahead.scale(r * 1.5)).add(across.scale(r * 5.0)).add(0.0, r * 4.5, 0.0);
                        Raster top = new Raster(backEye, tip.add(ahead.scale(r * 0.8)), 55.0);
                        top.ground(HILLS, tip, 40.0, 1.0);
                        top.quads(collector.quads, origin);
                        ImageIO.write(top.image, "png", new File(out, "dragon-" + tick + "-crown.png"));
                    }
                }
                System.out.println("tick " + tick + ": " + collector.quads.size() + " quads");
            }
            previous = frame;
        }
    }

    private static void render(Raster raster, Collector collector, V3 origin, V3 focus, File file) throws Exception {
        raster.ground(HILLS, focus, 110.0, 2.0);
        raster.quads(collector.quads, origin);
        ImageIO.write(raster.image, "png", file);
    }
}
