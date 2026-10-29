package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.DragonBody;
import com.hex.kagune.control.awakening.DragonRig;
import com.hex.kagune.control.awakening.S2CDragonFrame;
import com.hex.kagune.control.awakening.V3;

/**
 * The Dragon interpolated between two server frames. Positions are relative to {@link #anchor} so the
 * mesh keeps float precision far from the world origin.
 */
final class DragonPose {
    /** The rendered body bulges past its radius with plates, ridges and spines. */
    static final double BODY_BULGE = 1.25;
    final double anchorX;
    final double anchorY;
    final double anchorZ;
    final float tick;
    final V3[] segments = new V3[DragonRig.SEGMENTS];
    final double[] radii = new double[DragonRig.SEGMENTS];
    final V3[] headTips = new V3[DragonRig.HEADS];
    final double[] jaws = new double[DragonRig.HEADS];
    final int[] headStates = new int[DragonRig.HEADS];
    final V3[] tentacleTips = new V3[DragonRig.TENTACLES];
    final int[] tentacleStates = new int[DragonRig.TENTACLES];
    final V3 emergence;
    /** 1.0.32: steps its legs have taken (cycles, from how far it has really crawled) and how fast it crawls (blocks a tick). */
    double steps;
    double pace;
    /** 1.0.32: the ground its feet stand on. */
    Floor floor = Floor.NONE;

    /**
     * 1.0.32: the top of the ground at (x, z) near height y (all relative to the anchor): minus infinity where there is none within
     * reach below, NaN where it cannot tell.
     */
    interface Floor {
        Floor NONE = (x, y, z) -> Double.NaN;

        double at(double x, double y, double z);
    }

    /**
     * 1.0.32: its legs' walk, kept per Dragon from frame to frame: they step by how far the body has really crawled, one cycle per
     * {@link #STEP} of its girth, and stand still when it does.
     */
    static final class Gait {
        static final double STEP = 2.0;
        private double stepsBefore;
        private double steps;
        private double paceBefore;
        private double pace;

        void advance(S2CDragonFrame from, S2CDragonFrame to) {
            stepsBefore = steps;
            paceBefore = pace;
            if (from == null || to == null) return;
            double crawled = crawl(from, to);
            steps += crawled / (STEP * Math.max(0.5, widest(to)));
            pace += (crawled - pace) * 0.35;
        }

        void hold() {
            stepsBefore = steps;
            paceBefore = pace;
        }

        void apply(DragonPose pose, float t) {
            pose.steps = stepsBefore + (steps - stepsBefore) * t;
            pose.pace = paceBefore + (pace - paceBefore) * t;
        }
    }

    /** How far its body went between two frames: the mean of its segments' moves across the ground (blocks). */
    static double crawl(S2CDragonFrame from, S2CDragonFrame to) {
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            if (from.radii()[i] <= 0.05F || to.radii()[i] <= 0.05F) continue;
            double dx = to.anchorX() + to.segments()[i * 3] - from.anchorX() - from.segments()[i * 3];
            double dz = to.anchorZ() + to.segments()[i * 3 + 2] - from.anchorZ() - from.segments()[i * 3 + 2];
            sum += Math.sqrt(dx * dx + dz * dz);
            count++;
        }
        return count == 0 ? 0.0 : Math.min(4.0, sum / count);
    }

    static double widest(S2CDragonFrame frame) {
        double widest = 0.0;
        for (float r : frame.radii()) widest = Math.max(widest, r);
        return widest;
    }

    DragonPose(S2CDragonFrame from, S2CDragonFrame to, float t) {
        anchorX = to.anchorX();
        anchorY = to.anchorY();
        anchorZ = to.anchorZ();
        // Re-anchor the older frame if the anchor ever differed so interpolation stays continuous.
        double ox = from.anchorX() - to.anchorX();
        double oy = from.anchorY() - to.anchorY();
        double oz = from.anchorZ() - to.anchorZ();
        tick = from.tick() + (to.tick() - from.tick()) * t;
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            segments[i] = lerp(from.segments(), to.segments(), i * 3, t, ox, oy, oz);
            radii[i] = from.radii()[i] + (to.radii()[i] - from.radii()[i]) * t;
        }
        for (int i = 0; i < DragonRig.HEADS; i++) {
            headTips[i] = lerp(from.heads(), to.heads(), i * 4, t, ox, oy, oz);
            jaws[i] = from.heads()[i * 4 + 3] + (to.heads()[i * 4 + 3] - from.heads()[i * 4 + 3]) * t;
            headStates[i] = to.headStates()[i];
        }
        for (int i = 0; i < DragonRig.TENTACLES; i++) {
            tentacleTips[i] = lerp(from.tentacles(), to.tentacles(), i * 3, t, ox, oy, oz);
            tentacleStates[i] = to.tentacleStates()[i];
        }
        float[] e = to.emergence();
        emergence = new V3(e[0], e[1], e[2]);
    }

    private static V3 lerp(float[] a, float[] b, int i, float t, double ox, double oy, double oz) {
        return new V3(a[i] + ox + (b[i] - a[i] - ox) * t, a[i + 1] + oy + (b[i + 1] - a[i + 1] - oy) * t, a[i + 2] + oz + (b[i + 2] - a[i + 2] - oz) * t);
    }

    V3 world(V3 relative) {
        return relative.add(anchorX, anchorY, anchorZ);
    }

    V3 relative(V3 world) {
        return world.sub(new V3(anchorX, anchorY, anchorZ));
    }

    boolean born(int i) {
        return radii[i] > 0.05;
    }

    /** Average of the emerged segments, relative to the anchor. */
    V3 centroid() {
        V3 sum = V3.ZERO;
        int count = 0;
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            if (born(i)) {
                sum = sum.add(segments[i]);
                count++;
            }
        }
        return count == 0 ? V3.ZERO : sum.scale(1.0 / count);
    }

    /** Distance from the centroid to the outermost surface of the body. */
    double extent(V3 centroid) {
        double extent = 0.0;
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            if (born(i)) extent = Math.max(extent, segments[i].distance(centroid) + radii[i]);
        }
        for (V3 tip : headTips) extent = Math.max(extent, tip.distance(centroid));
        return extent;
    }

    // The body frame and the attachment points are the server's own formulas (DragonBody), so heads
    // and tentacles sprout exactly where they strike from.

    V3 forwardAt(int index) {
        return DragonBody.forward(segments, index);
    }

    V3 sideAt(int index) {
        return DragonBody.side(segments, index);
    }

    V3 upAt(int index) {
        return DragonBody.up(segments, index);
    }

    V3 headRoot(int head) {
        return DragonBody.headRoot(segments, radii, head);
    }

    V3 tentacleRoot(int tentacle) {
        return DragonBody.tentacleRoot(segments, radii, tentacle);
    }

    /** How fully grown the body is, from the rig's girth curve. */
    double girth() {
        double base = DragonRig.baseRadius(20);
        return base <= 0.0 ? 0.0 : radii[20] / base;
    }

    /** Radius of a head's jaws as the sculptor draws them; the jaws reach 2.1 times this past the tip. */
    double headRadius(int head) {
        double scale = Math.max(0.15, Math.min(1.2, girth()));
        return head == 0 ? Math.max(3.4 * scale, radii[DragonRig.HEAD_ROOTS[0]] * 1.12) : 2.4 * scale;
    }

    /** Direction the jaws of a head point in: away from the neck at the tip. */
    V3 headDirection(int head) {
        V3 direction = headTips[head].sub(headRoot(head)).normalize();
        return direction.length() < 1.0E-6 ? forwardAt(DragonRig.HEAD_ROOTS[head]) : direction;
    }

    /**
     * The nearest point at least {@code margin} outside the body, the necks and the jaws, as world
     * coordinates. Used to keep the camera out of the near-black inside of the mesh.
     */
    V3 outside(V3 world, double margin) {
        V3 p = relative(world);
        for (int pass = 0; pass < 4; pass++) {
            boolean moved = false;
            for (int i = 0; i + 1 < DragonRig.SEGMENTS; i++) {
                if (!born(i) && !born(i + 1)) continue;
                V3 next = pushOut(p, segments[i], segments[i + 1], Math.max(radii[i], radii[i + 1]) * BODY_BULGE + margin);
                moved |= next != p;
                p = next;
            }
            for (int h = 0; h < DragonRig.HEADS; h++) {
                if (radii[DragonRig.HEAD_ROOTS[h]] < 0.3) continue;
                double r = headRadius(h);
                V3 tip = headTips[h];
                V3 neck = pushOut(p, headRoot(h), tip, Math.max(r, radii[DragonRig.HEAD_ROOTS[h]] * 0.95) + margin);
                V3 jaws = pushOut(neck, tip, tip.add(headDirection(h).scale(r * 2.1)), r * 1.1 + margin);
                moved |= jaws != p;
                p = jaws;
            }
            if (!moved) return world(p);
        }
        // Still wedged between coils: rise until clear of all of it.
        V3 lifted = world(p);
        for (int step = 0; step < 512 && inside(lifted, margin * 0.99); step++) lifted = lifted.add(0.0, 0.5, 0.0);
        return lifted;
    }

    /** True if the point (world coordinates) is inside the body, a neck or the jaws, widened by {@code margin}. */
    boolean inside(V3 world, double margin) {
        V3 p = relative(world);
        for (int i = 0; i + 1 < DragonRig.SEGMENTS; i++) {
            if (!born(i) && !born(i + 1)) continue;
            if (p.distanceToSegment(segments[i], segments[i + 1]) < Math.max(radii[i], radii[i + 1]) * BODY_BULGE + margin) return true;
        }
        for (int h = 0; h < DragonRig.HEADS; h++) {
            if (radii[DragonRig.HEAD_ROOTS[h]] < 0.3) continue;
            double r = headRadius(h);
            V3 tip = headTips[h];
            if (p.distanceToSegment(headRoot(h), tip) < Math.max(r, radii[DragonRig.HEAD_ROOTS[h]] * 0.95) + margin) return true;
            if (p.distanceToSegment(tip, tip.add(headDirection(h).scale(r * 2.1))) < r * 1.1 + margin) return true;
        }
        return false;
    }

    private static V3 pushOut(V3 p, V3 a, V3 b, double r) {
        V3 ab = b.sub(a);
        double lengthSquared = ab.dot(ab);
        double t = lengthSquared < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, p.sub(a).dot(ab) / lengthSquared));
        V3 closest = a.add(ab.scale(t));
        V3 away = p.sub(closest);
        double distance = away.length();
        if (distance >= r) return p;
        V3 direction = distance < 1.0E-6 ? V3.UP : away.scale(1.0 / distance);
        return closest.add(direction.scale(r));
    }
}
