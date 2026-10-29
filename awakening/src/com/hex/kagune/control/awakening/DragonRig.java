package com.hex.kagune.control.awakening;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Server-authoritative kinematics and autonomous behaviour of the Dragon Kakuja.
 *
 * <p>The rig is deliberately free of Minecraft types: the session feeds it the prey in range and a
 * terrain probe, and applies the {@link Strike strikes} it returns. Clients only render the state the
 * server sends, so what players see is exactly what deals damage.
 *
 * <p>The Dragon crawls like a colossal centipede: the head leads along a heading that can only turn
 * so fast, the body follows the head's exact path pressed into the ground, and where the path
 * crosses itself the newer part climbs over the older one.
 */
public final class DragonRig {
    public static final int SEGMENTS = 84;
    public static final double SPACING = 2.4;
    public static final double BODY_LENGTH = SEGMENTS * SPACING;
    public static final int HEADS = 5;
    public static final int TENTACLES = 10;
    /** Body segments that carry a head; head 0 is the front of the body. */
    public static final int[] HEAD_ROOTS = {0, 16, 30, 45, 62};
    public static final int[] TENTACLE_ROOTS = {4, 10, 21, 25, 36, 40, 51, 55, 69, 76};
    /** The front never strays further than this (horizontally) from where Rinkaku fell. */
    public static final double LEASH = 78.0;
    /** Everything alive this close to the body is hunted. */
    public static final double DETECTION = 96.0;

    /** Fastest turn of the heading, in radians per tick: a turning radius of about 17 blocks when cruising. */
    public static final double MAX_YAW_RATE = 0.042;
    public static final double CRUISE_SPEED = 0.72;
    public static final double CHARGE_SPEED = 1.05;
    /** Heading weave that makes the path an S-shaped slither. */
    static final double WEAVE = 0.32;
    /** Steepest step between neighbouring segments where the body climbs over itself. */
    static final double RAMP = 0.9 * SPACING;
    /** Centre-to-centre height, as a fraction of both radii, at which one part of the body passes over another. */
    static final double CLEARANCE = 0.8;
    /** The body starts this deep under the eruption point and bursts out in an arch. */
    static final double BURROW_DEPTH = 7.0;
    static final double ARCH_HEIGHT = 9.0;
    static final int ARCH_TICKS = 48;
    /** Ground probe span: obstacles narrower than this are ploughed through, not climbed. */
    static final double GROUND_SPAN = 4.5;
    static final double CLIMB_RATE = 0.6;
    static final double SINK_RATE = 0.7;

    public static final float ERUPTION_DAMAGE = 22.0F;
    public static final float BITE_DAMAGE = 34.0F;
    public static final float LASH_DAMAGE = 18.0F;
    public static final float SQUEEZE_DAMAGE = 6.0F;
    public static final float SLAM_DAMAGE = 30.0F;
    public static final float SLAM_SPLASH_DAMAGE = 14.0F;
    public static final float SHOCKWAVE_DAMAGE = 12.0F;

    /** Top of the original terrain at a column, and the vertical limits of the world. */
    public interface Ground {
        double surface(double x, double z);

        /** Top of the land the body crawls on, ignoring trees; liquids still count. */
        default double terrain(double x, double z) {
            return surface(x, z);
        }

        double minY();

        double maxY();
    }

    /** A living target the session found near the body this tick. */
    public record Prey(int id, V3 position, double width, double height, boolean player, boolean grabbable) {
        V3 center() {
            return position.add(0.0, height * 0.5, 0.0);
        }
    }

    public enum StrikeKind { ERUPTION, BITE, LASH, GRAB, SQUEEZE, SLAM, RELEASE, SHOCKWAVE }

    /**
     * Something the session must apply in the world. {@code serial} identifies one attack so a lash
     * damages each entity once even though it is reported on every tick of the sweep.
     */
    public record Strike(StrikeKind kind, int source, V3 position, double radius, float damage, int targetId, int serial) {
    }

    public enum HeadState { IDLE, WINDUP, STRIKE, RECOVER, LIMP }

    public enum TentacleState { IDLE, REACH, HOLD, SLAM, LASH, RECOVER, LIMP }

    public static final class Head {
        public final int index;
        public final int root;
        final double restLength;
        final double reach;
        public V3 tip = V3.ZERO;
        public double jaw;
        public HeadState state = HeadState.IDLE;
        int timer;
        int cooldown;
        int targetId;
        V3 aim = V3.ZERO;
        V3 from = V3.ZERO;

        Head(int index) {
            this.index = index;
            this.root = HEAD_ROOTS[index];
            this.restLength = index == 0 ? 6.0 : 9.0;
            this.reach = index == 0 ? 24.0 : 22.0;
        }
    }

    public static final class Tentacle {
        public final int index;
        public final int root;
        public final double side;
        final double restLength = 13.0;
        final double reach = 28.0;
        public V3 tip = V3.ZERO;
        public TentacleState state = TentacleState.IDLE;
        int timer;
        int cooldown;
        int targetId;
        public int heldId;
        int serial;
        V3 aim = V3.ZERO;
        V3 from = V3.ZERO;
        V3 sweep = V3.ZERO;

        Tentacle(int index) {
            this.index = index;
            this.root = TENTACLE_ROOTS[index];
            this.side = DragonBody.tentacleSide(index);
        }
    }

    public final V3 anchor;
    private final SplittableRandom random;
    private final DragonTrail trail;
    private final V3[] segments = new V3[SEGMENTS];
    private final double[] radii = new double[SEGMENTS];
    private final double[] fallen = new double[SEGMENTS];
    /** Height of the ground the spine rests on under each segment, taken from the trail. */
    private final double[] bases = new double[SEGMENTS];
    private final double[] xs = new double[SEGMENTS];
    private final double[] ys = new double[SEGMENTS];
    private final double[] zs = new double[SEGMENTS];
    public final Head[] heads = new Head[HEADS];
    public final Tentacle[] tentacles = new Tentacle[TENTACLES];
    private V3 lead;
    private double heading;
    private double speed = 0.25;
    private int turning;
    private V3 goal;
    private int goalPrey;
    private int goalTimer;
    /** How high the fore-body rears, driven by the main head. */
    private double rear;
    private int serials;
    private int tick = -1;
    private V3 emergence;

    public DragonRig(V3 anchor, long seed) {
        this.anchor = anchor;
        this.random = new SplittableRandom(seed);
        V3 origin = anchor.add(0.0, -BURROW_DEPTH, 0.0);
        this.trail = new DragonTrail(origin, BODY_LENGTH + 12.0);
        this.lead = origin;
        this.goal = anchor;
        this.heading = random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < SEGMENTS; i++) {
            segments[i] = origin;
            bases[i] = origin.y();
        }
        for (int i = 0; i < HEADS; i++) {
            heads[i] = new Head(i);
            heads[i].tip = origin;
            heads[i].cooldown = 10 + i * 7;
        }
        for (int i = 0; i < TENTACLES; i++) {
            tentacles[i] = new Tentacle(i);
            tentacles[i].tip = origin;
            tentacles[i].cooldown = 14 + i * 5;
        }
    }

    public int tick() {
        return tick;
    }

    public V3 segment(int index) {
        return segments[index];
    }

    /** Current body radius at a segment, including growth, emergence from the ground and collapse. */
    public double radius(int index) {
        return radii[index];
    }

    public V3 lead() {
        return lead;
    }

    /** Horizontal direction the head is travelling in. */
    public V3 heading() {
        return new V3(Math.cos(heading), 0.0, Math.sin(heading));
    }

    /** Base girth profile: a swollen fore-body tapering to a long tail. */
    public static double baseRadius(int index) {
        double u = index / (double) (SEGMENTS - 1);
        if (u < 0.28) return 3.6 + 2.2 * DragonTimeline.smooth(u / 0.28);
        return 5.8 - 4.5 * DragonTimeline.smooth((u - 0.28) / 0.72);
    }

    /**
     * Height of the ground the body rests on: the second lowest of the centre and four opposite-pair
     * averages around it. On an even slope every pair average equals the centre, so the estimate is
     * exact; a tree, hut or wall under a few of the samples is ignored, so the body ploughs through it
     * instead of climbing it, and it bridges a small pit under the centre.
     */
    public static double groundBelow(Ground ground, double x, double z) {
        double[] values = new double[5];
        values[0] = ground.terrain(x, z);
        for (int k = 0; k < 4; k++) {
            double angle = k * Math.PI / 4.0;
            double dx = Math.cos(angle) * GROUND_SPAN;
            double dz = Math.sin(angle) * GROUND_SPAN;
            values[k + 1] = 0.5 * (ground.terrain(x + dx, z + dz) + ground.terrain(x - dx, z - dz));
        }
        Arrays.sort(values);
        return values[1];
    }

    /** Where Rinkaku emerges from the collapsed body; fixed once the Dragon shatters. */
    public V3 emergence(Ground ground) {
        if (emergence != null) return emergence;
        V3 centroid = centroid();
        V3 nearest = anchor;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < SEGMENTS; i++) {
            double distance = segments[i].sub(centroid).horizontalLength();
            if (radii[i] > 0.05 && distance < best) {
                best = distance;
                nearest = segments[i];
            }
        }
        return new V3(nearest.x(), ground.surface(nearest.x(), nearest.z()), nearest.z());
    }

    /** Axis-aligned bounds of everything the Dragon can touch this tick, padded by its reach. */
    public double[] bounds(double padding) {
        double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int i = 0; i < SEGMENTS; i++) include(box, segments[i], radii[i]);
        for (Head h : heads) include(box, h.tip, 4.0);
        for (Tentacle t : tentacles) include(box, t.tip, 3.0);
        for (int i = 0; i < 3; i++) {
            box[i] -= padding;
            box[i + 3] += padding;
        }
        return box;
    }

    private static void include(double[] box, V3 p, double r) {
        box[0] = Math.min(box[0], p.x() - r);
        box[1] = Math.min(box[1], p.y() - r);
        box[2] = Math.min(box[2], p.z() - r);
        box[3] = Math.max(box[3], p.x() + r);
        box[4] = Math.max(box[4], p.y() + r);
        box[5] = Math.max(box[5], p.z() + r);
    }

    public V3 centroid() {
        V3 sum = V3.ZERO;
        int count = 0;
        for (int i = 0; i < SEGMENTS; i++) {
            if (radii[i] > 0.05) {
                sum = sum.add(segments[i]);
                count++;
            }
        }
        return count == 0 ? anchor : sum.scale(1.0 / count);
    }

    /** Advances the rig to the given session tick and returns what must happen in the world. */
    public List<Strike> step(int now, List<Prey> prey, Ground ground) {
        tick = now;
        List<Strike> strikes = new ArrayList<>();
        DragonTimeline.Phase phase = DragonTimeline.phaseAt(now);
        if (now == 0) {
            strikes.add(new Strike(StrikeKind.ERUPTION, -1, anchor, 9.0, ERUPTION_DAMAGE, 0, ++serials));
        }
        if (now == DragonTimeline.SHATTER_TICK && emergence == null) {
            // Fix the emergence point from the last intact pose, before the body bursts apart.
            emergence = emergence(ground);
        }
        if (phase == DragonTimeline.Phase.DESTABILIZE || phase == DragonTimeline.Phase.EMERGENCE || phase == DragonTimeline.Phase.DONE) {
            collapse(now);
        } else {
            crawl(now, prey, ground);
            layBody(now);
        }
        boolean attacking = DragonTimeline.attacking(now);
        for (Head head : heads) tickHead(head, now, prey, attacking, ground, strikes);
        for (Tentacle tentacle : tentacles) tickTentacle(tentacle, now, prey, attacking, ground, strikes);
        if (now == DragonTimeline.SHATTER_TICK) {
            strikes.add(new Strike(StrikeKind.SHOCKWAVE, -1, emergence, 34.0, SHOCKWAVE_DAMAGE, 0, ++serials));
        }
        return strikes;
    }

    // ---- Locomotion -----------------------------------------------------------------------------

    private void crawl(int now, List<Prey> prey, Ground ground) {
        if (now == 0) {
            // Burst out towards the nearest living thing, if there is one.
            Prey first = nearest(prey, anchor, DETECTION, false, null, 0.0);
            if (first != null && first.position().sub(anchor).horizontalLength() > 1.0) {
                heading = Math.atan2(first.position().z() - anchor.z(), first.position().x() - anchor.x());
            }
        }
        if (now < DragonTimeline.RAMPAGE_START) {
            // Eruption: the head tears out of the ground in a straight arch, picking up speed.
            speed = 0.25 + 0.6 * now / (double) DragonTimeline.ERUPTION_TICKS;
        } else {
            steer(now, prey);
        }
        double nx = lead.x() + Math.cos(heading) * speed;
        double nz = lead.z() + Math.sin(heading) * speed;
        double target = groundBelow(ground, nx, nz);
        if (now < ARCH_TICKS) target += ARCH_HEIGHT * Math.sin(Math.PI * now / ARCH_TICKS);
        double y = lead.y() + Math.max(-SINK_RATE, Math.min(CLIMB_RATE, target - lead.y()));
        y = Math.max(ground.minY() + 4.0, Math.min(ground.maxY() - 6.0, y));
        lead = new V3(nx, y, nz);
        trail.record(lead);
    }

    private void steer(int now, List<Prey> prey) {
        if (goalPrey != 0) {
            Prey hunted = find(prey, goalPrey);
            if (hunted == null) {
                goalTimer = 0;
            } else {
                goal = hunted.position();
            }
        }
        if (--goalTimer <= 0 || flatDistance(lead, goal) < 6.0) chooseGoal(prey);
        V3 toward = goal;
        boolean prowling = goalPrey != 0;
        // While the main head hunts, the whole body turns after its quarry.
        Head main = heads[0];
        if (main.state == HeadState.WINDUP || main.state == HeadState.STRIKE) {
            Prey quarry = find(prey, main.targetId);
            if (quarry != null) {
                toward = quarry.position();
                prowling = true;
            }
        }
        // Soft leash: once the head is about to pass 80 % of the leash it turns back inwards. Looking a
        // turning radius ahead keeps the wide turn itself inside the leash.
        V3 fromAnchor = lead.sub(anchor).withY(0.0);
        V3 projected = fromAnchor.add(heading().scale(speed / MAX_YAW_RATE * 0.8));
        if (fromAnchor.horizontalLength() > LEASH * 0.8 || projected.horizontalLength() > LEASH * 0.8) {
            toward = anchor.add(fromAnchor.normalize().scale(LEASH * 0.3));
            prowling = false;
            if (flatDistance(goal, anchor) > LEASH * 0.8) goalTimer = 0;
        }
        double offCourse = Math.IEEEremainder(Math.atan2(toward.z() - lead.z(), toward.x() - lead.x()) - heading, Math.PI * 2.0);
        // The slither weave only while roughly on course, so it never decides which way a hard turn goes.
        double weave = WEAVE * Math.sin(now * 0.07) * Math.max(0.0, 1.0 - Math.abs(offCourse) / 1.2);
        double delta = offCourse + weave;
        // Keep turning the same way through a U-turn instead of dithering around the reverse heading.
        if (Math.abs(offCourse) > 2.8 && turning != 0) delta = turning * Math.abs(delta);
        delta = Math.max(-MAX_YAW_RATE, Math.min(MAX_YAW_RATE, delta));
        turning = Math.abs(delta) >= MAX_YAW_RATE * 0.99 ? (delta > 0.0 ? 1 : -1) : 0;
        heading = Math.IEEEremainder(heading + delta, Math.PI * 2.0);
        V3 ahead = toward.sub(lead).withY(0.0);
        boolean charge = prowling && ahead.horizontalLength() < 40.0 && ahead.normalize().dot(heading()) > 0.75;
        double want = charge ? CHARGE_SPEED : CRUISE_SPEED;
        speed += Math.max(-0.02, Math.min(0.02, want - speed));
    }

    private static double flatDistance(V3 a, V3 b) {
        return a.sub(b).horizontalLength();
    }

    private void chooseGoal(List<Prey> prey) {
        goalTimer = 60 + random.nextInt(50);
        goalPrey = 0;
        double total = 0.0;
        for (Prey p : prey) if (flatDistance(p.position(), anchor) <= LEASH) total += p.player() ? 3.0 : 1.0;
        if (total > 0.0) {
            double pick = random.nextDouble() * total;
            for (Prey p : prey) {
                if (flatDistance(p.position(), anchor) > LEASH) continue;
                pick -= p.player() ? 3.0 : 1.0;
                if (pick <= 0.0) {
                    goal = p.position();
                    goalPrey = p.id();
                    return;
                }
            }
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(random.nextDouble()) * LEASH * 0.75;
        goal = anchor.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
    }

    private void layBody(int now) {
        double girth = DragonTimeline.girth(now);
        double emerged = trail.length();
        double rearTarget = switch (heads[0].state) {
            case WINDUP -> 8.5;
            case STRIKE -> 0.6;
            default -> 1.6 + 0.5 * Math.sin(now * 0.11);
        };
        rear += (rearTarget - rear) * 0.2;
        for (int i = 0; i < SEGMENTS; i++) {
            double distance = i * SPACING;
            V3 p = trail.behind(distance);
            double born = Math.max(0.0, Math.min(1.0, (emerged - distance) / (SPACING * 2.0)));
            radii[i] = baseRadius(i) * girth * born;
            bases[i] = p.y();
            double lift = i < 9 ? rear * Math.pow(1.0 - i / 9.0, 2.0) * born : 0.0;
            xs[i] = p.x();
            ys[i] = p.y() + DragonBody.RIDE * radii[i] + lift;
            zs[i] = p.z();
        }
        climb();
        for (int i = 0; i < SEGMENTS; i++) segments[i] = new V3(xs[i], ys[i], zs[i]);
    }

    /**
     * Where the path crosses itself the newer, forward part of the body climbs over the older part,
     * and the climb is ramped so neighbouring segments never step more than {@link #RAMP} apart.
     */
    private void climb() {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = SEGMENTS - 1; i >= 0; i--) {
                if (radii[i] < 0.05) continue;
                double need = ys[i];
                for (int j = i + 5; j < SEGMENTS && radii[j] >= 0.05; j++) {
                    // Neighbours along a bend (or over the eruption arch) overlap naturally; only parts
                    // of the body that are far apart along it can cross.
                    if ((j - i) * SPACING < 1.5 * (radii[i] + radii[j])) continue;
                    int k = Math.min(SEGMENTS - 1, j + 1);
                    double abx = xs[k] - xs[j];
                    double abz = zs[k] - zs[j];
                    double lengthSquared = abx * abx + abz * abz;
                    double t = lengthSquared < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((xs[i] - xs[j]) * abx + (zs[i] - zs[j]) * abz) / lengthSquared));
                    double dx = xs[j] + abx * t - xs[i];
                    double dz = zs[j] + abz * t - zs[i];
                    double reach = radii[i] + radii[j] + (radii[k] - radii[j]) * t;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d >= reach) continue;
                    double under = ys[j] + (ys[k] - ys[j]) * t;
                    // A parabolic hump: the climb starts gently at the edge and is full over the middle.
                    need = Math.max(need, under + CLEARANCE * reach * (1.0 - (d / reach) * (d / reach)));
                }
                ys[i] = need;
            }
            int born = 0;
            while (born < SEGMENTS && radii[born] >= 0.05) born++;
            for (int i = 1; i < born; i++) ys[i] = Math.max(ys[i], ys[i - 1] - RAMP);
            for (int i = born - 2; i >= 0; i--) ys[i] = Math.max(ys[i], ys[i + 1] - RAMP);
        }
    }

    private void collapse(int now) {
        double girth = Math.max(DragonTimeline.girth(Math.min(now, DragonTimeline.SHATTER_TICK - 1)), 0.0);
        int elapsed = now - DragonTimeline.RAMPAGE_END;
        lead = segments[0];
        for (int i = 0; i < SEGMENTS; i++) {
            double t = Math.max(0.0, elapsed - i * 0.6);
            double drop = 0.5 * 0.08 * t * t;
            V3 top = segments[i].add(0.0, fallen[i], 0.0);
            double rest = bases[i] + 0.3 * baseRadius(i) * girth;
            double applied = Math.min(drop, Math.max(0.0, top.y() - rest));
            segments[i] = top.add(0.0, -applied, 0.0);
            fallen[i] = applied;
            // The mass sags and swells unevenly as it tears itself apart.
            double throb = 1.0 + 0.08 * Math.sin(now * 0.9 + i * 1.7) * DragonTimeline.instability(now);
            radii[i] = now >= DragonTimeline.SHATTER_TICK ? 0.0 : baseRadius(i) * girth * throb * (radii[i] > 0.0 ? 1.0 : 0.0);
        }
    }

    // ---- Body frame -----------------------------------------------------------------------------

    private V3 forwardAt(int index) {
        return DragonBody.forward(segments, index);
    }

    private V3 sideAt(int index) {
        return DragonBody.side(segments, index);
    }

    private V3 upAt(int index) {
        return DragonBody.up(segments, index);
    }

    public V3 headRoot(Head head) {
        return DragonBody.headRoot(segments, radii, head.index);
    }

    public V3 tentacleRoot(Tentacle tentacle) {
        return DragonBody.tentacleRoot(segments, radii, tentacle.index);
    }

    private double scaleAt(int root) {
        return radii[root] / Math.max(0.1, baseRadius(root));
    }

    private V3 headRest(Head head, int now) {
        V3 root = headRoot(head);
        double scale = Math.max(0.2, scaleAt(head.root));
        double sway = now * 0.06 + head.index * 1.9;
        if (head.index == 0) {
            // The main head leads: it rests in front of the body, held a little off the ground.
            V3 direction = forwardAt(0).add(0.0, 0.12, 0.0).normalize();
            V3 wobble = sideAt(0).scale(Math.sin(sway) * 0.7).add(0.0, Math.sin(sway * 1.3) * 0.4, 0.0);
            return root.add(direction.scale(head.restLength * scale)).add(wobble.scale(scale));
        }
        double sign = DragonBody.headSide(head.index);
        V3 direction = upAt(head.root).scale(0.85).add(sideAt(head.root).scale(sign * 0.5)).add(forwardAt(head.root).scale(0.3)).normalize();
        V3 wobble = new V3(Math.sin(sway) * 1.6, Math.sin(sway * 1.3) * 0.9, Math.cos(sway * 0.8) * 1.6);
        return root.add(direction.scale(head.restLength * scale)).add(wobble.scale(scale));
    }

    /** Raised high before a bite: the main head stays in front of the body, side heads lift over their flank. */
    private V3 headCocked(Head head) {
        V3 root = headRoot(head);
        double length = head.restLength * Math.max(0.2, scaleAt(head.root));
        if (head.index == 0) return root.add(DragonBody.flatForward(segments, 0).scale(length * 0.4)).add(0.0, length * 0.85, 0.0);
        double sign = DragonBody.headSide(head.index);
        return root.add(upAt(head.root).scale(length * 0.95)).add(sideAt(head.root).scale(sign * length * 0.25));
    }

    /** The main head never swings behind the front of the body: at least 0.35 of its neck points forwards. */
    private V3 clampAhead(V3 tip) {
        V3 root = segments[0];
        V3 forward = DragonBody.flatForward(segments, 0);
        V3 neck = tip.sub(root);
        double length = neck.length();
        if (length < 1.0E-6) return root.add(forward.scale(0.1));
        double along = neck.dot(forward);
        if (along >= 0.35 * length) return tip;
        V3 across = neck.sub(forward.scale(along));
        V3 bend = across.length() < 1.0E-6 ? V3.UP : across.normalize();
        return root.add(forward.scale(0.35 * length)).add(bend.scale(Math.sqrt(1.0 - 0.35 * 0.35) * length));
    }

    private V3 tentacleRest(Tentacle tentacle, int now) {
        V3 root = tentacleRoot(tentacle);
        // Arched up and out over the flank.
        V3 direction = sideAt(tentacle.root).scale(tentacle.side * 0.6).add(upAt(tentacle.root).scale(0.65)).add(forwardAt(tentacle.root).scale(-0.2)).normalize();
        double w = now * 0.11 + tentacle.index * 2.3;
        V3 writhe = new V3(Math.sin(w) * 3.4, Math.sin(w * 1.7) * 2.6, Math.cos(w * 1.2) * 3.4);
        double scale = Math.max(0.15, scaleAt(tentacle.root));
        return root.add(direction.scale(tentacle.restLength * scale)).add(writhe.scale(scale));
    }

    // ---- Targets --------------------------------------------------------------------------------

    private static Prey find(List<Prey> prey, int id) {
        if (id == 0) return null;
        for (Prey p : prey) if (p.id() == id) return p;
        return null;
    }

    private int attackers(int id) {
        int count = 0;
        for (Head h : heads) if (h.targetId == id && h.state != HeadState.IDLE) count++;
        for (Tentacle t : tentacles) if ((t.targetId == id || t.heldId == id) && t.state != TentacleState.IDLE) count++;
        return count;
    }

    /**
     * Best target within reach. With a {@code facing} direction, only prey whose horizontal
     * direction from {@code from} has a dot product above {@code minDot} with it counts.
     */
    private Prey nearest(List<Prey> prey, V3 from, double reach, boolean grabOnly, V3 facing, double minDot) {
        Prey best = null;
        double bestScore = Double.MAX_VALUE;
        for (Prey p : prey) {
            if (grabOnly && !p.grabbable()) continue;
            if (isHeld(p.id())) continue;
            double distance = p.center().distance(from);
            if (distance > reach) continue;
            if (facing != null && p.center().sub(from).withY(0.0).normalize().dot(facing) <= minDot) continue;
            double score = distance + attackers(p.id()) * 9.0 - (p.player() ? 6.0 : 0.0);
            if (score < bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private boolean isHeld(int id) {
        for (Tentacle t : tentacles) if (t.heldId == id) return true;
        return false;
    }

    /** Heads and tentacles on a flank only go for prey on their own side of the body. */
    private V3 facing(Head head) {
        return head.index == 0 ? DragonBody.flatForward(segments, 0) : sideAt(head.root).scale(DragonBody.headSide(head.index));
    }

    // ---- Heads ----------------------------------------------------------------------------------

    private void tickHead(Head head, int now, List<Prey> prey, boolean attacking, Ground ground, List<Strike> strikes) {
        if (DragonTimeline.phaseAt(now).ordinal() >= DragonTimeline.Phase.DESTABILIZE.ordinal()) {
            head.state = HeadState.LIMP;
            double twitch = DragonTimeline.instability(now);
            V3 root = headRoot(head);
            V3 droop = root.add(0.0, -head.restLength * 1.25, 0.0)
                    .add(Math.sin(now * 1.3 + head.index) * twitch * 1.5, 0.0, Math.cos(now * 1.1 + head.index) * twitch * 1.5);
            if (head.index == 0) droop = droop.add(DragonBody.flatForward(segments, 0).scale(head.restLength * 0.6));
            droop = droop.withY(Math.max(droop.y(), ground.terrain(droop.x(), droop.z()) + 0.6));
            head.tip = head.tip.lerp(droop, 0.2);
            head.jaw += (0.65 - head.jaw) * 0.2;
            return;
        }
        Prey target = find(prey, head.targetId);
        switch (head.state) {
            case IDLE -> {
                head.tip = head.tip.lerp(headRest(head, now), 0.25);
                head.jaw += (0.12 + 0.08 * Math.sin(now * 0.2 + head.index) - head.jaw) * 0.3;
                if (attacking && --head.cooldown <= 0) {
                    Prey pick = nearest(prey, headRoot(head), head.reach, false, facing(head), head.index == 0 ? 0.4 : -0.25);
                    if (pick != null) {
                        head.targetId = pick.id();
                        head.aim = pick.center();
                        head.state = HeadState.WINDUP;
                        head.timer = 8;
                    } else {
                        head.cooldown = 6;
                    }
                }
            }
            case WINDUP -> {
                if (target != null) head.aim = target.center();
                head.tip = head.tip.lerp(headCocked(head), 0.3);
                head.jaw += (1.0 - head.jaw) * 0.35;
                if (--head.timer <= 0) {
                    head.from = head.tip;
                    head.state = HeadState.STRIKE;
                    head.timer = 5;
                }
            }
            case STRIKE -> {
                if (target != null) head.aim = target.center();
                double k = 1.0 - (head.timer - 1) / 5.0;
                head.tip = head.from.lerp(head.aim, k * k);
                head.jaw = 1.0;
                if (--head.timer <= 0) {
                    if (head.index == 0) head.tip = clampAhead(head.tip);
                    strikes.add(new Strike(StrikeKind.BITE, head.index, head.tip, 3.4, BITE_DAMAGE, head.targetId, ++serials));
                    head.state = HeadState.RECOVER;
                    head.timer = 12;
                    head.jaw = 0.0;
                }
            }
            case RECOVER -> {
                head.tip = head.tip.lerp(headRest(head, now), 0.18);
                head.jaw += (0.1 - head.jaw) * 0.25;
                if (--head.timer <= 0) {
                    head.state = HeadState.IDLE;
                    head.targetId = 0;
                    head.cooldown = 8 + random.nextInt(16);
                }
            }
            default -> head.state = HeadState.IDLE;
        }
        if (head.index == 0) head.tip = clampAhead(head.tip);
    }

    // ---- Tentacles ------------------------------------------------------------------------------

    private void tickTentacle(Tentacle t, int now, List<Prey> prey, boolean attacking, Ground ground, List<Strike> strikes) {
        if (DragonTimeline.phaseAt(now).ordinal() >= DragonTimeline.Phase.DESTABILIZE.ordinal()) {
            if (t.heldId != 0) {
                strikes.add(new Strike(StrikeKind.RELEASE, t.index, t.tip, 0.0, 0.0F, t.heldId, ++serials));
                t.heldId = 0;
            }
            t.state = TentacleState.LIMP;
            V3 root = tentacleRoot(t);
            V3 droop = root.add(sideAt(t.root).scale(t.side * t.restLength * 0.6));
            double floor = ground.terrain(droop.x(), droop.z()) + 0.5;
            droop = droop.withY(Math.max(floor, droop.y() - t.restLength));
            double twitch = DragonTimeline.instability(now) * 2.0;
            t.tip = t.tip.lerp(droop.add(Math.sin(now * 1.7 + t.index) * twitch, 0.0, Math.cos(now * 1.5 + t.index) * twitch), 0.15);
            return;
        }
        Prey target = find(prey, t.state == TentacleState.HOLD || t.state == TentacleState.SLAM ? t.heldId : t.targetId);
        switch (t.state) {
            case IDLE -> {
                t.tip = t.tip.lerp(tentacleRest(t, now), 0.22);
                if (attacking && --t.cooldown <= 0) {
                    V3 root = tentacleRoot(t);
                    V3 flank = sideAt(t.root).scale(t.side);
                    boolean grab = random.nextDouble() < 0.55;
                    Prey pick = nearest(prey, root, t.reach, grab, flank, -0.25);
                    if (pick == null && grab) {
                        grab = false;
                        pick = nearest(prey, root, t.reach, false, flank, -0.25);
                    }
                    if (pick != null) {
                        t.targetId = pick.id();
                        t.aim = pick.center();
                        t.from = t.tip;
                        t.serial = ++serials;
                        if (grab) {
                            t.state = TentacleState.REACH;
                            t.timer = 9;
                        } else {
                            t.state = TentacleState.LASH;
                            t.timer = 10;
                            V3 flat = pick.center().sub(root).withY(0.0).normalize();
                            t.sweep = new V3(-flat.z(), 0.0, flat.x()).scale(9.0);
                        }
                    } else {
                        t.cooldown = 8;
                    }
                }
            }
            case REACH -> {
                if (target != null) t.aim = target.center();
                double k = 1.0 - (t.timer - 1) / 9.0;
                t.tip = t.from.lerp(t.aim, Math.sqrt(k));
                if (--t.timer <= 0) {
                    if (target != null && target.center().distance(t.tip) <= 3.2 + target.width()) {
                        t.heldId = target.id();
                        t.state = TentacleState.HOLD;
                        t.timer = 22;
                        t.from = t.tip;
                        strikes.add(new Strike(StrikeKind.GRAB, t.index, t.tip, 0.0, 0.0F, t.heldId, t.serial));
                    } else {
                        t.state = TentacleState.RECOVER;
                        t.timer = 12;
                        t.targetId = 0;
                    }
                }
            }
            case HOLD -> {
                if (target == null) {
                    releaseHeld(t, strikes);
                    break;
                }
                V3 root = tentacleRoot(t);
                V3 lifted = root.add(0.0, 15.0, 0.0).add(sideAt(t.root).scale(t.side * 5.0));
                t.tip = t.tip.lerp(lifted, 0.12);
                if (t.timer % 8 == 0) strikes.add(new Strike(StrikeKind.SQUEEZE, t.index, t.tip, 0.0, SQUEEZE_DAMAGE, t.heldId, ++serials));
                if (--t.timer <= 0) {
                    t.state = TentacleState.SLAM;
                    t.timer = 6;
                    t.from = t.tip;
                    V3 down = root.add(sideAt(t.root).scale(t.side * 9.0));
                    t.aim = down.withY(ground.surface(down.x(), down.z()) + 0.5);
                }
            }
            case SLAM -> {
                double k = 1.0 - (t.timer - 1) / 6.0;
                t.tip = t.from.lerp(t.aim, k * k);
                if (--t.timer <= 0) {
                    strikes.add(new Strike(StrikeKind.SLAM, t.index, t.aim, 5.5, SLAM_DAMAGE, t.heldId, ++serials));
                    t.heldId = 0;
                    t.targetId = 0;
                    t.state = TentacleState.RECOVER;
                    t.timer = 14;
                }
            }
            case LASH -> {
                double k = 1.0 - (t.timer - 1) / 10.0;
                V3 point = t.aim.add(t.sweep.scale(2.0 * k - 1.0)).add(0.0, 1.5 * Math.sin(k * Math.PI), 0.0);
                t.tip = t.tip.lerp(point, 0.65);
                strikes.add(new Strike(StrikeKind.LASH, t.index, t.tip, 2.6, LASH_DAMAGE, 0, t.serial));
                if (--t.timer <= 0) {
                    t.state = TentacleState.RECOVER;
                    t.timer = 12;
                    t.targetId = 0;
                }
            }
            case RECOVER -> {
                t.tip = t.tip.lerp(tentacleRest(t, now), 0.18);
                if (--t.timer <= 0) {
                    t.state = TentacleState.IDLE;
                    t.cooldown = 10 + random.nextInt(20);
                }
            }
            default -> t.state = TentacleState.IDLE;
        }
    }

    private void releaseHeld(Tentacle t, List<Strike> strikes) {
        if (t.heldId != 0) strikes.add(new Strike(StrikeKind.RELEASE, t.index, t.tip, 0.0, 0.0F, t.heldId, ++serials));
        t.heldId = 0;
        t.targetId = 0;
        t.state = TentacleState.RECOVER;
        t.timer = 12;
    }

    /** Called when the session loses a held entity (it died, left, or became invalid). */
    public void dropHeld(int entityId) {
        for (Tentacle t : tentacles) {
            if (t.heldId == entityId) {
                t.heldId = 0;
                t.targetId = 0;
                if (t.state == TentacleState.HOLD) {
                    t.state = TentacleState.RECOVER;
                    t.timer = 12;
                }
            }
        }
    }
}
