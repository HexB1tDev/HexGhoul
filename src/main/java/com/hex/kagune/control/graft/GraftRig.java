package com.hex.kagune.control.graft;

import com.hex.kagune.control.ukaku.CentipedeMotion;
import java.util.List;

/**
 * Poses Graft DNA's limbs (pure, tested; 7.0.0). Rig space as every kagune rig in the mod: blocks, x to the player's right, y up
 * from his feet, z forward, turned with his body. Each limb takes a pose: a handful of control points from its root in his lower
 * back to its tip, the side its back faces, how wide its mouth is open, how far its spikes stand out and how fast it gets there.
 * {@link GraftMotion} follows the poses with springs, so a limb never snaps from one to the next.
 *
 * <p>At rest each stage has its own silhouette (the transplant's tails arched over the shoulders like a scorpion's, the
 * centipedes hooked forward with the mouth reared over his head, the four thorns spread round him like a spider's legs, the
 * many-eyed crescents towering in a stack on his right, the kakuja's two colossi curled over and beside him), breathing, writhing on their
 * own slow rhythms, turning a little toward where he looks, streaming back when he runs, tucking in when he crouches and on the
 * way up a jump. In a move each limb answers its {@link GraftMove.Beat}: wound up, then at its point on the landing tick.
 */
public final class GraftRig {
    /** Where the strikes come from: his chest (blocks above his feet). */
    public static final double CHEST = 1.35;

    /**
     * How his body moves this frame, and the prey his mouths are after (the middle of the nearest living thing within
     * {@link #PREY_RANGE}, rig space; null for none).
     */
    public record Body(double speed, boolean crouching, double vertical, boolean onGround, double lookYaw, double lookPitch, double time,
                       double[] prey) {
        public static final Body STILL = new Body(0.0, false, 0.0, true, 0.0, 0.0, 0.0);

        public Body(double speed, boolean crouching, double vertical, boolean onGround, double lookYaw, double lookPitch, double time) {
            this(speed, crouching, vertical, onGround, lookYaw, lookPitch, time, null);
        }
    }

    /** How near a living thing must be for his mouths to go after it (blocks). */
    public static final double PREY_RANGE = 10.0;

    /**
     * One limb this frame.
     *
     * @param stage   the kagune out
     * @param limbs   how many limbs it has
     * @param limb    which one
     * @param length  its length at rest (blocks)
     * @param body    how he moves
     * @param move    the move his kagune is making (NONE at rest)
     * @param age     ticks since the move began
     * @param aim     the move's point (rig space): its target's middle, or where he aimed; null for none
     * @param target  this limb's own target (rig space; a move that sends each limb after its own), or null
     * @param held    the body this limb holds (its middle, rig space), or null
     * @param heldRadius the held body's half-width
     * @param job     7.0.2: the job running alongside the move ({@link GraftMove#job}; NONE for none)
     * @param jobAge  ticks since the job began
     * @param jobHeld the body the job holds (its middle, rig space), or null
     */
    public record Input(GraftStage stage, int limbs, int limb, double length, Body body, GraftMove move, double age, double[] aim, double[] target,
                        double[] held, double heldRadius, GraftMove job, double jobAge, double[] jobHeld) {
        /** An input with no job running. */
        public Input(GraftStage stage, int limbs, int limb, double length, Body body, GraftMove move, double age, double[] aim, double[] target,
                     double[] held, double heldRadius) {
            this(stage, limbs, limb, length, body, move, age, aim, target, held, heldRadius, GraftMove.NONE, 0.0, null);
        }

        /** 7.0.2: the job as the move this limb makes (its own clock, what it holds). */
        Input asJob() {
            // What the job holds is also what it goes after (the King's Game's prey).
            return new Input(this.stage, this.limbs, this.limb, this.length, this.body, this.job, this.jobAge, this.jobHeld, null, this.jobHeld,
                this.heldRadius);
        }
    }

    /**
     * A pose to follow: the points (rig space, root first), its back's direction, the mouth (0 shut, 1 wide), the spikes (1 at rest,
     * more bristling), the length it is drawn out to (an extended strike reaches past its rest length), how quickly to get there
     * (ticks to cover most of the way), and how wide its eyes are (0 resting, 1 staring).
     */
    public record Pose(double[][] points, double[] dorsal, double jaw, double flare, double length, double response, double stare) {
    }

    private GraftRig() {
    }

    /** +1 for a limb on his right, -1 on his left (the centre ones count as right). */
    public static int side(GraftStage stage, int limbs, int limb) {
        return switch (stage) {
            case TRANSPLANT, KAKUJA -> limb == 0 ? 1 : -1;
            case CENTIPEDE -> limb == 2 ? -1 : 1;
            case THORNS -> limb % 2 == 0 ? 1 : -1;
            // The many-eyed kagune towers to his right, every layer on the one side.
            case DRAGON -> 1;
        };
    }

    /** True for a limb that rises from the middle of his back (the centipedes' mouth, a lone tail). */
    public static boolean central(GraftStage stage, int limbs, int limb) {
        return limb == 0 && (stage == GraftStage.CENTIPEDE || stage == GraftStage.TRANSPLANT && limbs == 1);
    }

    /** True if this limb carries the mouth (the centipedes' first; both of the kakuja's colossi). */
    public static boolean maw(GraftStage stage, int limb) {
        return stage == GraftStage.CENTIPEDE && limb == 0 || stage == GraftStage.KAKUJA;
    }

    /** The limb's root in his back (rig space), lower when he crouches. */
    public static double[] root(GraftStage stage, int limbs, int limb, boolean crouching) {
        int s = side(stage, limbs, limb);
        double drop = crouching ? 0.22 : 0.0;
        double x = central(stage, limbs, limb) ? 0.0 : 0.1 * s;
        return switch (stage) {
            case TRANSPLANT, KAKUJA -> new double[] {x, 0.95 - drop, -0.17};
            case CENTIPEDE -> new double[] {x, (limb == 0 ? 1.0 : 0.88) - drop, -0.17};
            case THORNS -> new double[] {0.12 * s, (limb < 2 ? 1.02 : 0.84) - drop, -0.17};
            // The many-eyed crescents leave his back one over another, from between his shoulder blades down to the small of it.
            case DRAGON -> {
                double[] start = LAYERS[Math.min(LAYERS.length - 1, limb)][0];
                yield new double[] {start[0], start[1] - drop, start[2]};
            }
        };
    }

    // The rest designs, drawn for the right side (the left mirrors them), root at DESIGN_ROOT, unit-free: each is scaled to the
    // limb's length about its root.
    static final double[] DESIGN_ROOT = {0.1, 0.95, -0.17};
    /** One tail: out of the small of the back, up and out round his side in a wide arc, its point hooked forward at his shoulder. */
    static final double[][] TAIL = {{0.1, 0.95, -0.17}, {0.35, 1.15, -0.62}, {0.95, 1.65, -1.0}, {1.65, 2.35, -0.85}, {2.0, 2.85, -0.15},
        {1.85, 2.85, 0.65}, {1.4, 2.45, 1.15}};
    /** The centipedes' mouth: reared high over his head, arching forward, its head looking down. */
    static final double[][] MAW = {{0.0, 1.0, -0.17}, {0.0, 1.45, -0.8}, {0.1, 2.6, -1.25}, {0.15, 3.65, -0.8}, {0.12, 4.15, 0.2},
        {0.05, 3.8, 1.05}, {0.0, 3.2, 1.4}};
    /** A side centipede: out to the side, low, hooked forward round him. */
    static final double[][] HOOK = {{0.1, 0.88, -0.17}, {0.45, 1.05, -0.7}, {1.35, 1.6, -1.0}, {2.35, 2.0, -0.6}, {2.95, 1.85, 0.3},
        {2.95, 1.25, 1.05}, {2.6, 0.95, 1.45}};
    /** An upper thorn: high over the shoulders and arching forward, a spider's front leg. */
    static final double[][] THORN_HIGH = {{0.12, 1.02, -0.17}, {0.5, 1.5, -0.8}, {1.6, 3.15, -1.2}, {3.0, 4.5, -0.6}, {4.35, 4.35, 0.6},
        {5.0, 3.0, 1.6}, {5.1, 1.9, 2.1}};
    /** A lower thorn: out wide to the side, its point near the ground ahead of him. */
    static final double[][] THORN_LOW = {{0.12, 0.84, -0.17}, {0.6, 1.0, -0.75}, {2.0, 1.6, -1.2}, {3.8, 1.8, -0.8}, {5.2, 1.2, 0.0},
        {5.6, 0.3, 0.8}, {5.5, 0.1, 1.3}};
    /**
     * The many-eyed kagune (the manga's "let's play house"): long thick crescents laid one over another a band's breadth apart (so
     * each reads as its own), near parallel, as a wall
     * behind him from his left shoulder to his right, each running out level to his right and keeping its height to its end, coming
     * a little forward there (the mass as tall at its far edge as behind him); the top one's end hooked down, the next one's
     * curling up under it; the lowest a blade of a tail down along the ground in front of
     * him on his right. Each is drawn about the stage's length (8 blocks). The top one: up behind his left shoulder, over him.
     */
    static final double[][] LAYER_0 = {{-0.12, 1.75, -0.2}, {-0.4, 2.3, -0.62}, {-0.5, 3.2, -0.9}, {-0.2, 4.05, -1.0}, {0.6, 4.55, -1.0},
        {1.8, 4.7, -0.85}, {3.0, 4.75, -0.55}, {3.9, 4.8, -0.2}, {4.45, 4.6, 0.2}, {4.55, 4.15, 0.45}, {4.3, 3.9, 0.55}};
    /** The second: up behind his head, out level under the top one and round, its end curling up. */
    static final double[][] LAYER_1 = {{0.0, 1.65, -0.2}, {-0.05, 2.2, -0.7}, {-0.05, 2.95, -0.95}, {0.6, 3.55, -1.0}, {1.8, 3.7, -0.85},
        {3.0, 3.7, -0.6}, {4.0, 3.6, -0.25}, {4.7, 3.55, 0.15}, {5.05, 3.8, 0.45}, {5.0, 4.1, 0.6}};
    /** The third: behind his right shoulder, out level and round, its end drooping. */
    static final double[][] LAYER_2 = {{0.1, 1.55, -0.2}, {0.35, 1.95, -0.68}, {0.5, 2.45, -0.95}, {1.3, 2.7, -0.95}, {2.5, 2.75, -0.75},
        {3.7, 2.75, -0.45}, {4.7, 2.7, -0.05}, {5.4, 2.55, 0.35}, {5.75, 2.3, 0.65}};
    /** The fourth: at his chest, out furthest and round, its end curling down. */
    static final double[][] LAYER_3 = {{0.13, 1.4, -0.2}, {0.45, 1.55, -0.7}, {0.8, 1.7, -0.95}, {1.8, 1.75, -0.9}, {3.0, 1.75, -0.65},
        {4.1, 1.7, -0.3}, {5.1, 1.6, 0.15}, {5.7, 1.35, 0.6}, {5.85, 0.95, 0.95}};
    /** The lowest: from his hip down to the ground and along it in front of him on his right, its blade of a point lifting. */
    static final double[][] LAYER_4 = {{0.13, 1.2, -0.2}, {0.55, 0.95, -0.7}, {1.4, 0.6, -0.85}, {2.4, 0.3, -0.65}, {3.4, 0.12, -0.2},
        {4.2, 0.1, 0.5}, {4.6, 0.15, 1.35}, {4.6, 0.3, 2.2}, {4.3, 0.6, 2.85}};
    static final double[][][] LAYERS = {LAYER_0, LAYER_1, LAYER_2, LAYER_3, LAYER_4};

    /** The kakuja's right colossus: up over him and round, its head looming above his right shoulder, looking down at what he faces. */
    static final double[][] COLOSSUS_HIGH = {{0.1, 1.0, -0.2}, {0.5, 1.4, -0.9}, {1.4, 2.8, -1.8}, {2.4, 4.6, -1.65}, {2.85, 6.0, -0.4},
        {2.25, 6.65, 1.15}, {1.05, 6.0, 2.4}, {0.55, 4.85, 2.95}};
    /** The kakuja's left colossus: lower and wider, curled round beside him, its head raised. */
    static final double[][] COLOSSUS_LOW = {{0.1, 1.0, -0.2}, {0.6, 1.2, -1.0}, {2.0, 1.8, -2.0}, {3.8, 2.6, -1.8}, {5.0, 3.6, -0.6},
        {5.2, 4.4, 1.0}, {4.2, 4.85, 2.2}, {3.0, 4.45, 2.85}};

    /** The rest design for this limb (right-side coordinates). */
    static double[][] design(GraftStage stage, int limbs, int limb) {
        return switch (stage) {
            case TRANSPLANT -> TAIL;
            case CENTIPEDE -> limb == 0 ? MAW : HOOK;
            case THORNS -> limb < 2 ? THORN_HIGH : THORN_LOW;
            case DRAGON -> LAYERS[Math.min(LAYERS.length - 1, limb)];
            case KAKUJA -> limb == 0 ? COLOSSUS_HIGH : COLOSSUS_LOW;
        };
    }

    /** The pose for {@code in}, never through his own body. */
    public static Pose pose(Input in) {
        int s = side(in.stage(), in.limbs(), in.limb());
        double[] root = root(in.stage(), in.limbs(), in.limb(), in.body().crouching());
        GraftMove.Beat beat = beat(in.move(), in.limbs(), in.limb(), in.age());
        // 7.0.2: a job's limb is its own where the move leaves it free, and always while it holds something.
        GraftMove.Beat job = in.job() == null || in.job() == GraftMove.NONE ? null : beat(in.job(), in.limbs(), in.limb(), in.jobAge());
        Pose pose;
        if (job != null && (beat == null || job.has(GraftMove.CARRY))) {
            pose = act(in.asJob(), s, root, job);
        } else {
            pose = beat == null ? rest(in, s, root) : act(in, s, root, beat);
        }
        return new Pose(clear(pose.points(), s, in.body().crouching()), pose.dorsal(), pose.jaw(), pose.flare(), pose.length(), pose.response(),
            pose.stare());
    }

    /** His body's half-width for keeping limbs clear (blocks round the vertical through his middle) and its top. */
    public static final double BODY_RADIUS = 0.5;
    public static final double BODY_TOP = 2.05;

    /**
     * {@code points} with every point past the root's first two pushed out of his body: a point inside the column round him (feet
     * to the top of his head) moves straight out of it, on the side it is on (its limb's side when dead centre). The root and the
     * point leaving his back stay where they are.
     */
    public static double[][] clear(double[][] points, int side, boolean crouching) {
        double top = BODY_TOP - (crouching ? 0.3 : 0.0);
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double[] p = points[i].clone();
            if (i >= 2 && p[1] > -0.2 && p[1] < top) {
                double r = Math.hypot(p[0], p[2]);
                // The head is narrower than the shoulders.
                double radius = p[1] > top - 0.55 ? BODY_RADIUS * 0.75 : BODY_RADIUS;
                if (r < radius) {
                    double ox = r < 1.0E-6 ? side : p[0] / r;
                    double oz = r < 1.0E-6 ? 0.0 : p[2] / r;
                    p[0] = ox * radius;
                    p[2] = oz * radius;
                }
            }
            out[i] = p;
        }
        return out;
    }

    /** The beat holding this limb at {@code age} (the latest begun), or null. */
    public static GraftMove.Beat beat(GraftMove move, int limbs, int limb, double age) {
        if (move == null || move == GraftMove.NONE) {
            return null;
        }
        GraftMove.Beat found = null;
        for (GraftMove.Beat beat : move.beats(limbs)) {
            if (beat.involves(limb) && beat.holds(age) && (found == null || beat.start() >= found.start())) {
                found = beat;
            }
        }
        return found;
    }

    /** At rest: the stage's design, breathing and writhing, turning toward his look, trailing as he runs, tucked as he crouches. */
    static Pose rest(Input in, int s, double[] root) {
        double[][] points = whole(design(in.stage(), in.limbs(), in.limb()), s, in.length(), root);
        Body body = in.body();
        double k = in.length() / curveLength(design(in.stage(), in.limbs(), in.limb()));
        double look = clamp(body.lookYaw(), -1.2, 1.2);
        double pitch = clamp(body.lookPitch(), -1.0, 1.0);
        double run = clamp01(body.speed() / 0.28);
        double rise = clamp(body.vertical() / 0.4, -1.0, 1.0);
        double offset = in.limb() * 1.7 + (s < 0 ? 0.9 : 0.0);
        double crouch = body.crouching() ? 1.0 : 0.0;
        for (int i = 1; i < points.length; i++) {
            double f = i / (double)(points.length - 1);
            double[] p = points[i];
            // Breathing and a slow writhe of its own (two drifting waves, never in step with the others).
            p[1] += k * (0.15 * f * Math.sin(body.time() * 0.07 + offset + f * 1.6) - 0.4 * pitch * f * f * f + 0.1 * run * f);
            p[0] += k * s * (0.12 * f * f * Math.sin(body.time() * 0.045 + 1.7 + offset) + 0.06 * f * Math.sin(body.time() * 0.11 + offset * 2.3));
            p[2] += k * 0.08 * f * Math.sin(body.time() * 0.083 + offset);
            // Every limb twitches now and then, its point flicking like a predator's tail.
            double flick = flick(body.time(), in.limb() + in.stage().ordinal() * 5);
            p[0] += k * s * 0.18 * flick * f * f * f;
            p[1] += k * 0.12 * flick * f * f;
            if (in.stage() == GraftStage.KAKUJA || maw(in.stage(), in.limb())) {
                // The colossi are never still: a heavy sway under the head's own hunting (below).
                p[0] += k * s * 0.22 * f * f * Math.sin(body.time() * 0.05 + in.limb() * 2.4);
                p[1] += k * 0.28 * f * f * f * Math.sin(body.time() * 0.11 + in.limb() * 1.9);
                p[2] += k * 0.18 * f * f * Math.sin(body.time() * 0.07 + in.limb() * 3.1);
            }
            // Streaming back as he runs; tucked in close when he crouches; drawn up on the way up a jump, spread on the way down.
            p[2] -= k * (0.45 * run + 0.25 * crouch) * f;
            p[1] -= k * 0.35 * crouch * f * f;
            if (!body.onGround()) {
                p[1] += k * 0.35 * rise * f;
                p[0] += k * s * 0.3 * Math.max(0.0, -rise) * f;
            }
            // Never into the ground (the many-eyed kagune's lowest layer trails along it).
            p[1] = Math.max(p[1], 0.08);
            points[i] = turn(new double[][] {p}, root, look * 0.35 * f * f)[0];
        }
        double[] dorsal = in.stage() == GraftStage.DRAGON ? new double[] {0.0, 0.35, 1.0}
            : in.stage() == GraftStage.THORNS ? new double[] {0.0, 1.0, 0.0} : new double[] {s * 0.6, 0.8, 0.0};
        if (maw(in.stage(), in.limb())) {
            // A mouth is a predator: it hunts (see hunt), and follows its own head fast.
            Hunt hunt = hunt(in, points, k);
            return new Pose(CentipedeMotion.fit(hunt.points(), in.length(), 1, points.length - 1), dorsal, hunt.jaw(), 1.0, in.length(),
                hunt.response(), hunt.stare());
        }
        return new Pose(CentipedeMotion.fit(points, in.length(), 1, points.length - 1), dorsal, 0.0, 1.0, in.length(), 5.0, 0.0);
    }

    /** What a mouth is doing at rest: its points (the head bent where it goes), its jaw, how fast it follows, how hard it stares. */
    record Hunt(double[][] points, double jaw, double response, double stare) {
    }

    /** Where a mouth's head begins along it (its points from here on are the head and neck it aims). */
    static final double HEAD = 0.55;
    /** A strike at prey: the ticks of each part of it, the last of every cycle (the rest of the cycle it tracks). */
    static final double REAR = 7.0;
    static final double LUNGE = 2.0;
    static final double RECOIL = 7.0;

    /**
     * A mouth at rest, hunting. With prey: the head turned on it and following it, trembling, the jaws working; every so often it
     * rears back, gaping, and lunges at it, snapping shut (or, a feint, stopping short with the jaws still wide and jerking back).
     * With none: it hunts in jerks, the head snapping round to a new bearing and freezing there, sniffing, now and then snapping
     * at nothing. Each mouth keeps its own beat.
     */
    static Hunt hunt(Input in, double[][] rest, double k) {
        int n = rest.length;
        int neck = (int)Math.round((n - 1) * HEAD);
        double[] base = rest[neck];
        double[] tip = rest[n - 1];
        double headLength = Math.max(0.5, length(sub(tip, base)));
        double time = in.body().time();
        int id = in.limb() + in.stage().ordinal() * 7;
        double[] prey = in.body().prey();
        double[] want;
        double jaw;
        double response;
        double stare;
        if (prey != null) {
            double[] way = normalize(sub(prey, base));
            double period = strikePeriod(id);
            double clock = time + 29.0 * id;
            long cycle = (long)Math.floor(clock / period);
            double at = clock - cycle * period;
            boolean snaps = hash((int)(cycle * 31 + id)) < 0.62;
            double lunge = (snaps ? 1.5 : 0.75) * k;
            double strike = period - REAR - LUNGE - RECOIL;
            double along;
            double lift;
            if (at < strike) {
                // Tracking: the jaws working, quivering.
                along = 0.0;
                lift = 0.0;
                jaw = 0.38 + 0.14 * Math.sin(time * 0.9 + id) + 0.06 * Math.sin(time * 2.7 + id * 3.0);
                response = 0.8;
            } else if (at < strike + REAR) {
                // Rearing back, the head drawn up and away, gaping.
                double u = smooth((at - strike) / REAR);
                along = -0.9 * k * u;
                lift = 0.6 * k * u;
                jaw = 0.4 + 0.6 * u;
                response = 0.6;
            } else if (at < strike + REAR + LUNGE) {
                double u = smooth((at - strike - REAR) / LUNGE);
                along = -0.9 * k + (lunge + 0.9 * k) * u;
                lift = 0.6 * k * (1.0 - u);
                // A snap shuts as it lands; a feint stays wide.
                jaw = snaps && u > 0.7 ? 0.0 : 1.0;
                response = 0.22;
            } else {
                double u = smooth((at - strike - REAR - LUNGE) / RECOIL);
                along = lunge * (1.0 - u);
                lift = 0.0;
                jaw = snaps ? 0.4 * u : 1.0 - 0.6 * u;
                response = snaps ? 0.7 : 0.35;
            }
            // Never still: a predator's tremble, finer while it strikes.
            double tremble = at < strike ? 1.0 : 0.4;
            double[] side = normalize(new double[] {way[2], 0.0, -way[0]});
            double[] jitter = add(mul(side, tremble * k * (0.05 * Math.sin(time * 2.3 + id * 1.7) + 0.035 * Math.sin(time * 3.7 + id))),
                new double[] {0.0, tremble * k * 0.04 * Math.sin(time * 2.9 + id * 0.6), 0.0});
            want = add(add(add(base, mul(way, headLength + along)), new double[] {0.0, lift, 0.0}), jitter);
            stare = 1.0;
        } else {
            // Hunting in jerks: a new bearing every so often, snapped round to in a few ticks and held there, frozen.
            double span = 16.0 + 8.0 * hash(id * 7 + 3);
            double clock = time + 23.0 * id;
            long leg = (long)Math.floor(clock / span);
            double at = clock - leg * span;
            double move = smooth(at / 3.0);
            double yaw = lerp1(bearing(leg - 1, id), bearing(leg, id), move);
            double pitch = lerp1(tilt(leg - 1, id), tilt(leg, id), move);
            double[] head = sub(tip, base);
            head = turnVector(head, yaw);
            head = add(head, new double[] {0.0, Math.sin(pitch) * headLength, 0.0});
            want = add(base, mul(normalize(head), headLength));
            // On arriving it sniffs, jaws working; now and then it snaps at nothing.
            boolean lashes = hash((int)(leg * 7 + id * 3)) < 0.25;
            if (lashes && at > 5.0 && at < 9.0) {
                double u = (at - 5.0) / 4.0;
                want = add(want, mul(normalize(head), 0.6 * k * Math.sin(Math.PI * u)));
                jaw = u < 0.5 ? 0.9 : 0.0;
            } else if (at > 3.0 && at < 7.0) {
                jaw = 0.22 + 0.3 * Math.sin(Math.PI * (at - 3.0) / 4.0);
            } else {
                jaw = 0.2 + 0.04 * Math.sin(time * 1.7 + id);
            }
            want = add(want, new double[] {0.0, 0.015 * k * Math.sin(time * 3.1 + id), 0.0});
            response = at < 3.0 ? 0.45 : 0.9;
            stare = 0.3;
        }
        // Bend the neck and head to put the point there (the rest of the limb stays where its rest pose put it).
        double[] shift = sub(want, tip);
        double most = 2.6 * k;
        double size = length(shift);
        if (size > most) {
            shift = mul(shift, most / size);
        }
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            double w = i <= neck ? 0.0 : smooth((i - neck) / (double)(n - 1 - neck));
            out[i] = add(rest[i], mul(shift, w));
        }
        return new Hunt(out, clamp01(jaw), response, stare);
    }

    /** How often (ticks) a mouth with prey strikes at it. */
    static double strikePeriod(int id) {
        return 46.0 + 22.0 * hash(id * 13 + 1);
    }

    /** The first time at or after {@code time} that this mouth, with prey near, begins to rear back for a strike. */
    public static double nextStrike(GraftStage stage, int limb, double time) {
        int id = limb + stage.ordinal() * 7;
        double period = strikePeriod(id);
        double clock = time + 29.0 * id;
        double start = Math.floor(clock / period) * period + period - REAR - LUNGE - RECOIL;
        if (start < clock) {
            start += period;
        }
        return start - 29.0 * id;
    }

    /** The bearing (radians, about the vertical) a hunting mouth turns to on leg {@code leg} of its search. */
    static double bearing(long leg, int id) {
        return (hash((int)(leg * 101 + id * 17)) - 0.5) * 1.7;
    }

    /** The tilt (radians) a hunting mouth's head takes on leg {@code leg}. */
    static double tilt(long leg, int id) {
        return (hash((int)(leg * 53 + id * 29 + 7)) - 0.5) * 0.6;
    }

    /** {@code v} turned about the vertical by {@code angle} (positive toward his right). */
    static double[] turnVector(double[] v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new double[] {v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c};
    }

    static double lerp1(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** 0..1, the same for the same {@code n}. */
    static double hash(int n) {
        int h = n * 0x27D4EB2D;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return (h & 0xFFFFFF) / (double)0x1000000;
    }

    /** -1..1: a limb's point flicking, a quick twitch every few seconds (each limb on its own beat), else 0. */
    static double flick(double time, int limb) {
        double period = 95.0 + 23.0 * (limb % 4);
        double at = (time + limb * 41.0) % period;
        return at < 8.0 ? Math.sin(Math.PI * at / 4.0) * (1.0 - at / 8.0) : 0.0;
    }

    /** In a move: wound up, then at its point. */
    static Pose act(Input in, int s, double[] root, GraftMove.Beat beat) {
        double age = in.age();
        double length = in.length();
        double k = length / 5.0;
        // A blow aimed at a point is let go a moment before it lands, so the limb (on its springs) is there on the very tick the
        // server hurts; a sweep starts on the tick its arc does.
        double lead = lead(beat);
        boolean winding = age < beat.land() - lead;
        double w = smooth(lead <= 0.0 ? beat.windup(age) : clamp01((age - beat.start()) / Math.max(1.0E-6, beat.land() - lead - beat.start())));
        double t = smooth(beat.through(age));
        double facing = facing(in.aim());
        double[] chest = {0.0, CHEST - (in.body().crouching() ? 0.25 : 0.0), 0.0};
        double flare = beat.has(GraftMove.SPIKED) ? (winding ? 1.0 + 0.4 * w : 1.55) : 1.0;
        double stare = winding ? w : 0.6;
        double[] up = {0.0, 1.0, 0.0};
        // Where it sits among the limbs on its side: they rear, coil and arch apart, never as one.
        double fan = fan(in);
        double[] apart = {s * 0.45 * fan * k, -0.5 * fan * k, 0.0};
        switch (beat.kind()) {
            case THRUST, BITE -> {
                double[] target = strikePoint(in, beat, chest, facing);
                boolean bite = beat.kind() == GraftMove.Kind.BITE;
                if (winding) {
                    double[][] coil = {root, add(root, new double[] {s * 0.3, 0.5, -0.5}),
                        add(add(root, new double[] {s * 0.8 * k, 1.9 * k, -1.15 * k}), apart),
                        add(add(root, new double[] {s * 0.65 * k, 2.6 * k, -0.45 * k}), apart), add(add(root, aimed(target, root, 0.55 * k, 2.4 * k)), apart)};
                    // Drawn back hard: the further it will go, the tighter it coils.
                    double[][] blend = blendTo(restPoints(in, s, root), coil, w);
                    double gape = bite ? w : 0.15 * w;
                    if (bite) {
                        // A mouth drawn back to bite trembles with it, its jaws quivering wide.
                        double tremble = k * w * (0.09 * Math.sin(age * 3.1 + in.limb()) + 0.05 * Math.sin(age * 5.3));
                        for (int i = blend.length / 2; i < blend.length; i++) {
                            double f = (i - blend.length / 2) / (double)(blend.length - 1 - blend.length / 2);
                            blend[i] = add(blend[i], new double[] {s * tremble * f, 0.7 * tremble * f, 0.0});
                        }
                        gape = Math.min(1.0, w * (0.92 + 0.08 * Math.sin(age * 4.4)));
                    }
                    return new Pose(CentipedeMotion.fit(blend, length, 1, blend.length - 1), up, gape, flare, length, bite ? 0.9 : 1.6, stare);
                }
                // Up out of the back, over his shoulder on its own side, then straight down the line at the target.
                double[] b1 = add(root, new double[] {s * 0.3, 0.5, -0.38});
                double[] b2 = add(root, new double[] {s * 0.6, 1.3, -0.15});
                double[] over = {s * (0.78 + 0.3 * fan), Math.max(2.05, target[1] + 0.55) - 0.3 * fan, 0.55};
                double[] p4 = add(lerp(over, target, 0.55), new double[] {0.0, 0.15, 0.0});
                double[] tip = beat.has(GraftMove.PIERCE) ? add(target, mul(normalize(sub(target, over)), 0.6)) : target;
                if (bite && age > beat.land()) {
                    // Its teeth in, it shakes its head to tear, hard at first, dying away.
                    double tear = Math.sin((age - beat.land()) * 2.6) * 0.45 * (1.0 - t) * Math.min(1.0, (age - beat.land()) / 0.5);
                    double[] across = normalize(new double[] {target[2] - root[2], 0.0, -(target[0] - root[0])});
                    tip = add(tip, mul(across, tear));
                    p4 = add(p4, mul(across, tear * 0.5));
                }
                double[][] line = {root, b1, b2, over, p4, tip};
                double reach = Math.max(length, curveLength(line));
                // The jaws snap shut on the tick the bite lands.
                double jaw = bite ? (age < beat.land() ? 1.0 : 0.12) : 0.0;
                return new Pose(stretched(line, reach, length), up, jaw, flare, reach, 0.35, stare);
            }
            case SWEEP -> {
                double from = facing + Math.toRadians(beat.from());
                double to = facing + Math.toRadians(beat.to());
                double turn = Math.signum(to - from);
                double angle = winding ? from + turn * Math.toRadians(-28.0) * w : from + (to - from) * t;
                double reach = Math.min(beat.reach(), length * 1.45);
                double[] tip = add(new double[] {chest[0], beat.height(), chest[2]}, direction(angle, reach));
                // The middle trails the sweep, kept outside his body; it leaves his back over the shoulder on its own side.
                double[] lag = add(new double[] {0.0, Math.max(beat.height(), 1.0) + 0.55, 0.0}, direction(angle - turn * 0.55, Math.max(1.5, reach * 0.48)));
                double[] b1 = add(root, new double[] {s * 0.3, 0.5, -0.4});
                double[] b2 = add(root, new double[] {s * 0.65, 1.05, -0.2});
                double[][] line = {root, b1, b2, lag, lerp(lag, tip, 0.6), tip};
                if (winding) {
                    line = blendTo(restPoints(in, s, root), line, w);
                }
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), up, 0.0, flare, drawn, winding ? 1.4 : 0.45, stare);
            }
            case SLAM -> {
                double[] ground = slamPoint(in, beat, chest, facing);
                double[] high = add(root, new double[] {s * (0.9 + 0.6 * fan) * k, (3.3 - 0.55 * fan) * k, -(0.4 + 0.25 * Math.abs(fan)) * k});
                if (winding) {
                    double[][] reared = {root, add(root, new double[] {s * 0.3, 0.8, -0.5}), add(root, new double[] {s * 0.6 * k, 2.4 * k, -0.9 * k}),
                        high, add(high, new double[] {0.0, 0.25 * k, 0.9 * k})};
                    double[][] blend = blendTo(restPoints(in, s, root), reared, w);
                    return new Pose(CentipedeMotion.fit(blend, length, 1, blend.length - 1), up, 0.4 * w, flare, length, 1.8, stare);
                }
                double[] above = add(ground, new double[] {s * 0.35 * fan, 1.8 * k, 0.3 * fan});
                double[][] line = {root, add(root, new double[] {s * 0.35, 0.9, -0.4}), lerp(high, above, 0.4), above, ground};
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), up, 0.0, flare, drawn, 0.4, stare);
            }
            case UPPER -> {
                // Driven low along the ground to under its point, then ripped up through it and on above it.
                double[] target = strikePoint(in, beat, chest, facing);
                double[] under = {target[0], 0.12, target[2]};
                double[] b1 = add(root, new double[] {s * 0.35, 0.1, -0.35});
                double[] low = add(lerp(new double[] {root[0], 0.3, root[2]}, under, 0.45), new double[] {s * 0.6, 0.0, 0.0});
                if (winding) {
                    double[][] dive = {root, b1, low, under};
                    double[][] blend = blendTo(restPoints(in, s, root), dive, w);
                    return new Pose(CentipedeMotion.fit(blend, length, 1, blend.length - 1), up, maw(in.stage(), in.limb()) ? w : 0.0, flare, length, 1.1,
                        stare);
                }
                double[] tip = add(target, new double[] {0.0, 0.6 + 1.7 * t, 0.0});
                double[][] line = {root, b1, low, under, lerp(under, tip, 0.5), tip};
                double drawn = Math.max(length, curveLength(line));
                // 7.0.2 (The Ambush): a mouth comes up gaping and snaps shut as it rips through.
                double jaw = maw(in.stage(), in.limb()) ? (age < beat.land() ? 1.0 : 0.1) : 0.0;
                return new Pose(stretched(line, drawn, length), up, jaw, flare, drawn, 0.3, stare);
            }
            case SEIZE -> {
                double[] target = in.held() != null ? in.held() : strikePoint(in, beat, chest, facing);
                if (winding) {
                    double[][] coil = {root, add(root, new double[] {s * 0.3, 0.6, -0.5}), add(add(root, new double[] {s * 0.7 * k, 2.2 * k, -0.9 * k}), apart),
                        add(add(root, aimed(target, root, 0.6 * k, 2.6 * k)), apart)};
                    double[][] blend = blendTo(restPoints(in, s, root), coil, w);
                    return new Pose(CentipedeMotion.fit(blend, length, 1, blend.length - 1), up, w, flare, length, 1.5, stare);
                }
                double[][] wrap = coil(root, s, k, target, Math.max(0.3, in.heldRadius()), 1.2);
                double drawn = Math.max(length, curveLength(wrap));
                return new Pose(stretched(wrap, drawn, length), up, 0.0, flare, drawn, 0.6, stare);
            }
            case SPIN, BURST -> {
                double base = beat.kind() == GraftMove.Kind.SPIN ? facing + Math.toRadians(beat.from() + (beat.to() - beat.from()) * t)
                    : facing + Math.toRadians(beat.from());
                double share = beat.limb() == GraftMove.ALL ? in.limb() * Math.PI * 2.0 / Math.max(1, in.limbs()) : 0.0;
                if (beat.kind() == GraftMove.Kind.BURST && beat.limb() == GraftMove.ALL) {
                    share += Math.PI / Math.max(1, in.limbs());
                }
                double angle = base + share;
                double reach = Math.min(beat.reach(), length * 1.4);
                if (in.move() == GraftMove.BLOOM && !winding) {
                    // 7.0.2: each wave of spikes heaves the mass out further before it eases back.
                    reach *= 0.82 + 0.18 * clamp01((GraftRules.bloomFlare(age) - 1.5) / 2.4);
                }
                double[] tip = add(new double[] {0.0, beat.height(), 0.0}, direction(angle, reach));
                double[] lag = add(add(root, new double[] {0.0, 0.5, 0.0}), direction(angle - (beat.kind() == GraftMove.Kind.SPIN ? 0.6 : 0.0), reach * 0.45));
                double[][] line = {root, add(root, new double[] {s * 0.25, 0.4, -0.35}), lag, lerp(lag, tip, 0.55), tip};
                if (winding) {
                    double[][] curl = curl(in, s, root, 1.0);
                    line = blendTo(curl, line, beat.kind() == GraftMove.Kind.BURST ? w * w : w);
                }
                double drawn = Math.max(length, curveLength(line));
                // Thousand Thorns: the ribbons burst out bristling with enormous spikes.
                double bristle = in.move() == GraftMove.BLOOM ? GraftRules.bloomFlare(age) : beat.kind() == GraftMove.Kind.BURST ? 1.7 : 1.4;
                return new Pose(stretched(line, drawn, length), up, 0.0, Math.max(flare, bristle), drawn, winding ? 1.0 : 0.4, stare);
            }
            case CURL -> {
                double[][] curl = curl(in, s, root, winding ? w : 1.0);
                // Its flat faces turned out from him all the way round (a sideways hint at the root is carried round the ring radial).
                double[] sideways = cocoon(in.stage()) ? new double[] {s, 0.0, 0.0} : up;
                return new Pose(wrapped(curl, length), sideways, 0.0, 1.35, length, winding ? 1.0 : 1.6, 0.8);
            }
            case WHIP -> {
                if (winding && in.held() != null) {
                    // 7.0.2 (Human Puppet's throw): still wrapped round what it hoists, until it lets it fly.
                    double[][] wrap = coil(root, s, k, in.held(), Math.max(0.3, in.heldRadius()), 1.2);
                    double drawn = Math.max(length, curveLength(wrap));
                    return new Pose(stretched(wrap, drawn, length), up, 0.0, flare, drawn, 0.5, stare);
                }
                double[] target = in.aim() == null ? add(chest, direction(facing, 5.0)) : in.aim();
                double[] over = add(root, new double[] {s * 0.6 * k, 3.4 * k, -0.9 * k});
                double[][] line = winding
                    ? new double[][] {root, add(root, new double[] {s * 0.3, 0.7, -0.6}), add(root, new double[] {s * 0.8 * k, 2.4 * k, -1.4 * k}), over}
                    : new double[][] {root, add(root, new double[] {s * 0.3, 0.8, -0.3}), add(over, new double[] {0.0, -0.4 * k, 1.6 * k}),
                        add(chest, direction(facing, 3.6 * k)), add(target, new double[] {0.0, 0.5, 0.0})};
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), up, 0.0, 1.2, drawn, winding ? 1.0 : 0.4, stare);
            }
            case PLANT -> {
                if (in.move() == GraftMove.SCORPION_VAULT || in.move() == GraftMove.CATAPULT) {
                    return vault(in, s, root, k, age, stare);
                }
                int order = in.limb();
                double spread = in.limbs() <= 1 ? 0.0 : (order / (double)(in.limbs() - 1) - 0.5) * 1.4;
                double[] ground = {s * (0.6 + Math.abs(spread)) + spread * 0.5, 0.0, -2.0 - 0.3 * order};
                double[][] line = {root, add(root, new double[] {s * 0.3, 0.4, -0.6}), lerp(add(root, new double[] {s * 0.6, 0.5, -1.2}), ground, 0.5),
                    ground};
                return new Pose(CentipedeMotion.fit(line, length, 1, line.length - 1), up, 0.0, 1.2, length, 0.35, stare);
            }
            case LURK -> {
                return lurk(in, s, root, k, age, w, winding);
            }
            case CHASE -> {
                // 7.0.2 Hungry Maw: reared up over him, then out along its route (bent at the move's point, round what is in the
                // way) to its prey, drawn out further every tick, the jaws snapping as it goes.
                double[] prey = in.target() != null ? in.target() : strikePoint(in, beat, chest, facing);
                double[] rear = add(root, new double[] {0.0, 2.4 * k, -0.4 * k});
                double[] bend = in.aim() != null ? in.aim() : add(lerp(rear, prey, 0.5), new double[] {0.0, 1.0, 0.0});
                double[][] route = {root, add(root, new double[] {0.0, 0.6, -0.5}), rear, bend, prey};
                double drawn = Math.min(curveLength(route), Math.max(length, GraftRules.mawReach(age, length)));
                double[][] line = CentipedeMotion.resample(route, drawn);
                if (winding) {
                    line = blendTo(restPoints(in, s, root), line, w);
                    line = CentipedeMotion.resample(line, curveLength(line));
                }
                double jaw = 0.5 + 0.5 * Math.sin(age * 1.6);
                return new Pose(line, up, jaw, 1.3, drawn, winding ? 0.8 : 0.35, 1.0);
            }
            case CLAMP -> {
                // 7.0.2 the mouth's catch: up out of his back, round and down onto what it holds, its jaws shut through it, chewing.
                double[] held = in.held() != null ? in.held() : GraftRules.DRAG_POINT.clone();
                double[] rear = add(root, new double[] {0.3, 1.5 * k, -0.2 * k});
                double[] over = add(lerp(rear, held, 0.55), new double[] {0.0, 0.9 * k, 0.0});
                double[] into = add(held, mul(normalize(sub(held, over)), 0.35));
                double[][] line = {root, add(root, new double[] {0.1, 0.6, -0.4}), rear, over, held, into};
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), up, 0.12 + 0.08 * Math.max(0.0, Math.sin(age * 0.6)), 1.2, drawn, 0.5, 0.9);
            }
            case STORM -> {
                // 7.0.2 Eye of the Storm: out round him on its own wheeling path, its body trailing the way it goes, its eyes
                // widening and narrowing on their own.
                double a = facing + GraftRules.stormAngle(in.limb(), age);
                double way = in.limb() % 2 == 0 ? 1.0 : -1.0;
                double r = GraftRules.stormRadius(in.limb(), age, length);
                double h = GraftRules.stormHeight(in.limb(), age);
                double[] tip = add(new double[] {0.0, h, 0.0}, direction(a, Math.max(1.2, r)));
                double[] mid = add(new double[] {0.0, 0.5 * (h + root[1]) + 0.8, 0.0}, direction(a - way * 0.5, Math.max(1.0, r * 0.55)));
                double[][] line = {root, add(root, new double[] {0.4, 0.5, -0.45}), mid, tip};
                if (winding) {
                    line = blendTo(restPoints(in, s, root), line, w);
                }
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), new double[] {0.0, 0.35, 1.0}, 0.0, 1.6, drawn, 0.5,
                    0.5 + 0.5 * Math.sin(age * 0.5 + in.limb() * 1.9));
            }
            case CURTAIN -> {
                return curtain(in, s, root, beat, age, length);
            }
            case SOAR -> {
                // 7.0.2 the catapult's flight: splayed wide round him like a spider's legs, reaching down for the ground as he falls.
                double fall = clamp(-in.body().vertical() / 0.8, 0.0, 1.0);
                double a = Math.toRadians(SPLAY[in.limb() % SPLAY.length]);
                double reach = 3.0 * k;
                double[] knee = add(direction(a, reach * 0.55), new double[] {0.0, 1.7 + 0.6 * (1.0 - fall), 0.0});
                double[] tip = add(direction(a, reach), new double[] {0.0, 0.7 - 2.2 * fall, 0.0});
                double[][] line = {root, add(root, new double[] {s * 0.3, 0.4, -0.3}), knee, tip};
                if (winding) {
                    line = blendTo(restPoints(in, s, root), line, w);
                }
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), up, 0.0, 1.3, drawn, 0.6, stare);
            }
            case CRAWL -> {
                // 7.0.2 a stride: lifted and reached ahead to his side, stabbed down into the ground, then pulled past him as it hauls
                // him on (the ground it holds sliding back under him).
                double slide = age > beat.land() ? GraftRules.crawled(age - beat.land()) : 0.0;
                double[] ground = {s * GraftRules.CRAWL_SPLAY * k, 0.02, beat.reach() - slide};
                double[] high = add(root, new double[] {s * 0.9 * k, 2.0 * k, 1.3 * k});
                double[][] line;
                if (winding) {
                    double[][] lift = {root, add(root, new double[] {s * 0.35, 0.45, -0.35}), high, add(ground, new double[] {-s * 0.2, 2.2 * k, 0.3}),
                        add(ground, new double[] {0.0, 0.25 + 1.4 * k * (1.0 - w), 0.2})};
                    line = blendTo(restPoints(in, s, root), lift, Math.min(1.0, 2.0 * w));
                } else {
                    line = new double[][] {root, add(root, new double[] {s * 0.35, 0.45, -0.35}), high, add(ground, new double[] {s * 0.3, 1.2 * k, -0.3}),
                        ground};
                }
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), up, 0.0, 1.3, drawn, winding ? 0.6 : 0.3, 0.6);
            }
            case HOOK -> {
                // 7.0.2 Mockingbird's hook: shot past the side of what struck at him, then wrapped round its back from that side,
                // holding it where it stands.
                double[] target = in.held() != null ? in.held() : strikePoint(in, beat, chest, facing);
                double[] away = levelWay(chest, target);
                double[] across = {away[2], 0.0, -away[0]};
                double[] over = {s * 0.85, 2.15, 0.15};
                double[] beside = add(add(target, mul(across, s * 1.0)), new double[] {0.0, 0.3, 0.0});
                if (winding) {
                    double[][] out = {root, add(root, new double[] {s * 0.3, 0.55, -0.4}), over, add(lerp(over, beside, 0.55), new double[] {0.0, 0.45, 0.0}),
                        beside};
                    double[][] blend = blendTo(restPoints(in, s, root), out, w);
                    double drawn = Math.max(length, curveLength(blend));
                    return new Pose(stretched(blend, drawn, length), up, 0.0, flare, drawn, 0.8, stare);
                }
                double[][] wrap = hook(root, s, over, target, Math.max(0.3, in.heldRadius()), 1.15);
                double drawn = Math.max(length, curveLength(wrap));
                return new Pose(stretched(wrap, drawn, length), up, 0.0, flare, drawn, 0.45, stare);
            }
            case FLANK -> {
                // 7.0.2 Mockingbird's counterstrike: out wide round the side of what struck at him, behind it, and driven into its
                // back and out of its front.
                double[] target = strikePoint(in, beat, chest, facing);
                double[] away = levelWay(chest, target);
                double[] across = {away[2], 0.0, -away[0]};
                if (winding) {
                    // Drawn back low and wide on its own side, coiled to go round.
                    double[][] coil = {root, add(root, new double[] {s * 0.35, 0.3, -0.5}), add(root, new double[] {s * 1.1 * k, 0.6 * k, -1.0 * k}),
                        add(root, new double[] {s * 1.6 * k, 1.2 * k, -0.4 * k}), add(root, new double[] {s * 1.5 * k, 1.5 * k, 0.5 * k})};
                    double[][] blend = blendTo(restPoints(in, s, root), coil, w);
                    return new Pose(CentipedeMotion.fit(blend, length, 1, blend.length - 1), up, 0.0, flare, length, 1.3, stare);
                }
                double[] over = {s * 0.85, 2.15, 0.1};
                double[] wide = add(add(target, add(mul(across, s * 1.8), mul(away, 0.4))), new double[] {0.0, 0.55, 0.0});
                double[] behind = add(add(target, mul(away, 1.7)), add(mul(across, s * 0.35), new double[] {0.0, 0.2, 0.0}));
                double[] through = sub(target, mul(away, 0.55));
                double[][] line = {root, add(root, new double[] {s * 0.3, 0.5, -0.35}), over, wide, behind, target, through};
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), up, 0.0, flare, drawn, 0.3, stare);
            }
            case DIVE -> {
                // 7.0.2 the vault's dive: both out ahead of him along it like spears, side by side, their points together.
                double[] dir = diveWay(in.body());
                double[] line1 = add(chest, new double[] {s * 0.5, 0.45, 0.1});
                double[] mid = add(add(chest, mul(dir, 1.6 * k)), new double[] {s * 0.3, 0.0, 0.0});
                double[] tip = add(add(chest, mul(dir, 3.4 * k)), new double[] {s * 0.08, 0.0, 0.0});
                double[][] line = {root, add(root, new double[] {s * 0.3, 0.45, -0.2}), add(line1, mul(dir, 0.4)), mid, tip};
                if (winding) {
                    line = blendTo(restPoints(in, s, root), line, w);
                }
                double drawn = Math.max(length, curveLength(line));
                return new Pose(stretched(line, drawn, length), up, 0.0, 1.2, drawn, winding ? 0.9 : 0.35, stare);
            }
            case CLAW -> {
                // His own arm claws; the limbs only rear, ready, a little drawn up.
                double[][] points = restPoints(in, s, root);
                for (int i = 1; i < points.length; i++) {
                    double f = i / (double)(points.length - 1);
                    points[i] = add(points[i], new double[] {0.0, 0.35 * k * f, 0.2 * k * f});
                }
                return new Pose(CentipedeMotion.fit(points, length, 1, points.length - 1), up, 0.3, 1.25, length, 1.2, 0.7);
            }
        }
        return rest(in, s, root);
    }

    /**
     * 7.0.2 Mockingbird's stance: dropped low out of his back (nothing guards him), down along the ground behind him and out to
     * each side, then coiled up in an S beside his hips, the stings at his waist pointing forward: poised, trembling, turned on the
     * nearest thing to him (its prey).
     */
    static Pose lurk(Input in, int s, double[] root, double k, double age, double w, boolean winding) {
        double[][] lurk = {root, add(root, new double[] {s * 0.3, -0.2, -0.45}), add(root, new double[] {s * 0.9 * k, -0.55 * k, -1.0 * k}),
            add(root, new double[] {s * 1.5 * k, -0.75 * k, -1.2 * k}), add(root, new double[] {s * 2.0 * k, -0.6 * k, -0.6 * k}),
            add(root, new double[] {s * 2.1 * k, -0.15 * k, 0.1 * k}), add(root, new double[] {s * 1.75 * k, 0.25 * k, 0.45 * k}),
            add(root, new double[] {s * 1.35 * k, 0.2 * k, 0.75 * k})};
        // Drawn to the tail's own length and resampled as it is: the fit would lift its middle to lengthen it, out of the coil.
        lurk = scaled(lurk, root, in.length());
        double[] prey = in.body().prey();
        double[] sting = lurk[lurk.length - 1];
        double[] toward = prey == null ? new double[] {0.0, 0.0, 0.0} : mul(normalize(sub(prey, sting)), 0.35 * k);
        for (int i = 3; i < lurk.length; i++) {
            double f = (i - 2) / (double)(lurk.length - 3);
            // Poised: a predator's fine tremble, never in step with the other tail.
            double tremble = k * f * f * (0.035 * Math.sin(age * 2.1 + in.limb() * 1.3) + 0.02 * Math.sin(age * 3.7 + in.limb() * 2.9));
            lurk[i] = add(add(lurk[i], new double[] {s * tremble, 0.8 * tremble, 0.0}), mul(toward, f * f));
        }
        double[][] points = winding ? blendTo(restPoints(in, s, root), lurk, w) : lurk;
        return new Pose(CentipedeMotion.resample(points, curveLength(points)), new double[] {s * 0.6, 0.8, 0.0}, 0.0, 1.15, in.length(),
            winding ? 1.2 : 1.0, 0.9);
    }

    /**
     * 7.0.2 Scorpion Vault: both stabbed into the ground behind him; once he is thrown they stay where they went in (the ground
     * falling away below him as he rises), then are torn free and drawn up after him.
     */
    static Pose vault(Input in, int s, double[] root, double k, double age, double stare) {
        boolean catapult = in.move() == GraftMove.CATAPULT;
        double rise = catapult ? GraftRules.catapultRise(age - GraftRules.CATAPULT_LAUNCH) : GraftRules.vaultRise(age - GraftRules.VAULT_LAUNCH);
        // 7.0.2 Living Catapult: all four round him (ahead and behind on either side), the vault's two behind him.
        double[] ground = catapult ? add(direction(Math.toRadians(SPLAY[in.limb() % SPLAY.length]), 1.9), new double[] {0.0, -rise - 0.1, 0.0})
            : new double[] {s * 0.55, -rise - 0.1, -1.7};
        double[] high = add(root, new double[] {s * 0.45, 0.45, -0.55});
        double[][] planted = {root, high, add(lerp(high, ground, 0.5), new double[] {s * 0.25, 0.1, -0.15}), ground};
        double free = smooth((age - (catapult ? 6.0 : 5.0)) / 3.0);
        double[][] line = free > 0.0 ? blendTo(planted, restPoints(in, s, root), free) : planted;
        double drawn = Math.max(in.length(), curveLength(line));
        double[] up = {0.0, 1.0, 0.0};
        return new Pose(stretched(line, drawn, in.length()), up, 0.0, 1.2, drawn, free > 0.0 ? 1.0 : 0.3, stare);
    }

    /**
     * 7.0.2 Black Curtain: the crescent risen as one band of a wall across his front (the top one highest), out round his right and
     * across to his left; folded forward onto the ground ahead (it falls about its foot); then its free end curls on round the left
     * side and in, and the end it came from round the right, closing it in.
     */
    static Pose curtain(Input in, int s, double[] root, GraftMove.Beat beat, double age, double length) {
        int n = Math.max(1, in.limbs());
        double band = n == 1 ? 0.5 : 1.0 - in.limb() / (double)(n - 1);
        double height = 1.0 + (GraftRules.CURTAIN_TOP - 1.0) * band;
        double fold = GraftRules.curtainFold(age, beat.land()) * Math.PI * 0.5;
        double curl = smooth((age - beat.land()) / 6.0);
        double span = GraftRules.CURTAIN_SPAN;
        double foot = GraftRules.CURTAIN_FOOT;
        // The wall's points (x across, its height on the wall), then laid over about its foot.
        double[][] wall = {{span + 0.4, height * 0.8, -0.3 + 1.2 * curl}, {span, height, 0.0}, {span * 0.4, height, 0.0}, {-span * 0.4, height, 0.0},
            {-span, height, 0.0}, {-span - 0.3 + 1.2 * curl, height * 0.9, 1.4 * curl}};
        double[][] line = new double[wall.length + 2][];
        line[0] = root;
        line[1] = add(root, new double[] {0.45, 0.6, -0.4});
        // Fallen, it drapes over whatever stands under it (a body's height in the middle of what it covers) rather than lying flat.
        double laid = GraftRules.curtainFold(age, beat.land());
        for (int i = 0; i < wall.length; i++) {
            double h = wall[i][1];
            double along = wall[i][2];
            double reach = along + h * Math.sin(fold);
            double drape = GraftRules.CURTAIN_DRAPE * Math.sin(Math.PI * clamp01(reach / (GraftRules.CURTAIN_TOP + 0.8))) * laid;
            line[2 + i] = turnVector(new double[] {wall[i][0], Math.max(h * Math.cos(fold), drape) + 0.1, foot + reach}, facing(in.aim()));
        }
        double rise = smooth(age / 10.0);
        if (rise < 1.0) {
            line = blendTo(restPoints(in, s, root), line, rise);
        }
        double drawn = Math.max(length, curveLength(line));
        return new Pose(stretched(line, drawn, length), new double[] {0.0, 0.35, 1.0}, 0.0, age >= beat.land() ? 1.8 : 1.3, drawn, rise < 1.0 ? 0.8 : 0.4,
            fold > 0.1 ? 1.0 : 0.6);
    }

    /** 7.0.2: where each of the four thorns splays round him (degrees from ahead, + to his right): ahead and behind on either side. */
    static final double[] SPLAY = {40.0, -40.0, 135.0, -135.0};

    /** 7.0.2: {@code points} scaled about {@code root} so that the curve through them is {@code length} long. */
    static double[][] scaled(double[][] points, double[] root, double length) {
        double k = length / Math.max(1.0E-6, curveLength(points));
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            out[i] = add(root, mul(sub(points[i], root), k));
        }
        return out;
    }

    /** 7.0.2: the vault's dive from where he looks this frame (rig space, unit), as the server dives him ({@link GraftRules#diveDirection}). */
    static double[] diveWay(Body body) {
        double yaw = body.lookYaw();
        double pitch = body.lookPitch();
        return GraftRules.diveDirection(Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    /** The level way from {@code from} to {@code to} (unit; straight ahead when one is over the other). */
    static double[] levelWay(double[] from, double[] to) {
        double[] level = {to[0] - from[0], 0.0, to[2] - from[2]};
        return length(level) < 1.0E-6 ? new double[] {0.0, 0.0, 1.0} : normalize(level);
    }

    /**
     * 7.0.2: hooked round a body at {@code c} from the side {@code s} of it: over his shoulder ({@code over}) to it, then round its
     * back from that side and on round, {@code turns} half turns, a little lower as it goes.
     */
    static double[][] hook(double[] root, int s, double[] over, double[] c, double r, double turns) {
        double toward = Math.atan2(root[0] - c[0], root[2] - c[2]);
        double a0 = toward - s * Math.PI * 0.5;
        int count = 8;
        double[][] points = new double[3 + count][];
        points[0] = root;
        points[1] = add(root, new double[] {s * 0.3, 0.55, -0.4});
        points[2] = over;
        for (int i = 0; i < count; i++) {
            double f = i / (double)(count - 1);
            double a = a0 - s * turns * Math.PI * f;
            points[3 + i] = new double[] {c[0] + Math.sin(a) * r, c[1] + 0.3 - 0.45 * f, c[2] + Math.cos(a) * r};
        }
        return points;
    }

    /**
     * -1..1: where this limb sits among those on its own side (the first -1, the last +1; 0 for one alone or a central one): the
     * many-eyed stack's top layer -1, its lowest +1; a pair of thorns -1 and +1.
     */
    static double fan(Input in) {
        int s = side(in.stage(), in.limbs(), in.limb());
        int count = 0;
        int index = 0;
        for (int i = 0; i < in.limbs(); i++) {
            if (side(in.stage(), in.limbs(), i) == s && !central(in.stage(), in.limbs(), i)) {
                if (i == in.limb()) {
                    index = count;
                }
                count++;
            }
        }
        return count <= 1 || central(in.stage(), in.limbs(), in.limb()) ? 0.0 : 2.0 * index / (count - 1) - 1.0;
    }

    /** Ticks before it lands that a blow aimed at a point is let go (out of its wind-up): how long its springs take to get there. */
    static final double LEAD = 1.0;

    static double lead(GraftMove.Beat beat) {
        return switch (beat.kind()) {
            case THRUST, BITE, UPPER, SLAM, SEIZE, HOOK, FLANK -> Math.min(LEAD, 0.45 * (beat.land() - beat.start()));
            default -> 0.0;
        };
    }

    /** The rest design's points for this limb (no breathing), for blending a wind-up from. */
    static double[][] restPoints(Input in, int s, double[] root) {
        return whole(design(in.stage(), in.limbs(), in.limb()), s, in.length(), root);
    }

    /** How far out the cocoon stands round him (blocks), and the heights its layers wrap at. */
    static final double COCOON_RADIUS = 1.8;
    static final double COCOON_LOW = 0.35;
    static final double COCOON_HIGH = 2.75;
    /**
     * 7.0.2 the kakuja's two colossi: drawn in tighter and wound on up past his head, each more than once round (a squat barrel
     * five blocks across before), so the two close into a cocoon taller than it is wide.
     */
    static final double KAKUJA_COCOON_RADIUS = 1.3;
    static final double KAKUJA_COCOON_HIGH = 3.3;

    /** True if this stage's curl is the cocoon (Living Fortress, Monstrous Regeneration), not a short coil before a spin. */
    public static boolean cocoon(GraftStage stage) {
        return stage == GraftStage.DRAGON || stage == GraftStage.KAKUJA;
    }

    /**
     * Wrapped round him: out of his back behind him and round, each limb at its own height and the next the other way round, as far
     * as its length goes. The many-eyed kagune and the kakuja's colossi stand {@link #COCOON_RADIUS} out as a wall from his shins to
     * over his head (a cocoon he stands in, clear of him); the other stages draw in closer for the spins they coil before.
     * {@code amount} 0..1 how far round yet.
     */
    static double[][] curl(Input in, int s, double[] root, double amount) {
        int n = Math.max(1, in.limbs());
        boolean wall = cocoon(in.stage());
        boolean colossi = in.stage() == GraftStage.KAKUJA;
        double radius = (colossi ? KAKUJA_COCOON_RADIUS : wall ? COCOON_RADIUS : 1.15) + (wall ? 0.06 : 0.1) * (in.limb() % 2);
        double low = wall ? COCOON_LOW : 0.5;
        double high = colossi ? KAKUJA_COCOON_HIGH : wall ? COCOON_HIGH : 2.1;
        double band = low + (high - low) * (in.limb() + 0.5) / n;
        // Alternate ways round (two limbs from either side go round to meet; a one-sided stack interleaves).
        int way = (in.limb() % 2 == 0 ? 1 : -1) * (n == 2 ? s : 1);
        double start = Math.PI - s * 0.5;
        double[] exit = add(root, new double[] {s * 0.3, 0.15 * Math.signum(band - root[1]), -0.45});
        // As far round as it is long (the ring is cut off at its length; never straightened toward his back).
        double around = in.length() / radius;
        double sweep = way * Math.min(Math.toRadians(colossi ? 470.0 : 330.0), around) * Math.max(0.15, amount);
        int turns = colossi ? 13 : 9;
        double[][] out = new double[turns + 2][];
        out[0] = root;
        out[1] = exit;
        // 7.0.2: a cocoon's limbs spiral up through all of their band, so few thick ones (the kakuja's two) close into one wall.
        double rise = wall ? (high - low) / n * (colossi ? 0.95 : 0.85) : 0.22;
        for (int i = 0; i < turns; i++) {
            double f = (i + 1) / (double)turns;
            double a = start + sweep * f;
            // A slow helix about its band, breathing.
            double y = band + rise * (f - 0.5) * way + 0.06 * Math.sin(in.body().time() * 0.13 + i * 0.9 + in.limb());
            out[2 + i] = new double[] {Math.sin(a) * radius, y, Math.cos(a) * radius};
        }
        return out;
    }

    /** A wrap {@code length} long: cut off where its length runs out, or (still short of it) bowed out to it. */
    static double[][] wrapped(double[][] curl, double length) {
        return curveLength(curl) >= length ? CentipedeMotion.resample(curl, length) : CentipedeMotion.fit(curl, length, 1, curl.length - 1);
    }

    /** Wrapped round a body at {@code c}: over to it, then {@code turns} times round it, the point closing on its front. */
    static double[][] coil(double[] root, int s, double k, double[] c, double r, double turns) {
        double toward = Math.atan2(root[0] - c[0], root[2] - c[2]);
        double[] p1 = add(root, new double[] {s * 0.3, 0.7, -0.5});
        double[] p2 = add(lerp(root, c, 0.5), new double[] {0.0, 1.0 * k, 0.0});
        double a0 = toward + 0.3 * s;
        int count = 8;
        double[][] points = new double[3 + count][];
        points[0] = root;
        points[1] = p1;
        points[2] = p2;
        double sweep = turns * Math.PI * s;
        for (int i = 0; i < count; i++) {
            double f = (i + 1) / (double)count;
            double a = a0 + sweep * f;
            points[3 + i] = new double[] {c[0] + Math.sin(a) * r, c[1] + 0.5 - 0.85 * f, c[2] + Math.cos(a) * r};
        }
        return points;
    }

    /** Where a strike of this beat lands (rig space): its own target, the move's point turned by the beat's angle, held to its reach. */
    static double[] strikePoint(Input in, GraftMove.Beat beat, double[] chest, double facing) {
        double[] point;
        if (in.target() != null) {
            point = in.target();
        } else if (in.aim() != null) {
            point = turn(new double[][] {in.aim()}, chest, Math.toRadians(beat.from()))[0];
        } else {
            point = add(new double[] {0.0, beat.height(), 0.0}, direction(facing + Math.toRadians(beat.from()), beat.reach()));
        }
        double[] offset = sub(point, chest);
        double distance = length(offset);
        if (distance > beat.reach()) {
            point = add(chest, mul(offset, beat.reach() / distance));
        }
        return point;
    }

    /**
     * Where the server hits for {@code beat} (rig space), as {@code GraftCombat} finds it: a thrust's, bite's, rip's, seizure's,
     * hook's or flank's strike point, the ground a slam lands on (this limb's share of it), the point just ahead of his face his own jaws bite; null
     * for the blows that strike everything in an area (sweeps, spins, bursts, claws).
     */
    public static double[] hitPoint(Input in, GraftMove.Beat beat) {
        double[] chest = {0.0, CHEST - (in.body().crouching() ? 0.25 : 0.0), 0.0};
        double facing = facing(in.aim());
        return switch (beat.kind()) {
            case THRUST, UPPER, SEIZE, HOOK, FLANK -> strikePoint(in, beat, chest, facing);
            case BITE -> beat.limb() == GraftMove.ARM ? new double[] {0.0, 1.5, 2.0} : strikePoint(in, beat, chest, facing);
            case SLAM -> slamPoint(in, beat, chest, facing);
            default -> null;
        };
    }

    /** Where a slam of this beat hits the ground (rig space, at his feet's height). */
    static double[] slamPoint(Input in, GraftMove.Beat beat, double[] chest, double facing) {
        double[] point;
        double flat = in.aim() == null ? 0.0 : Math.hypot(in.aim()[0], in.aim()[2]);
        // As the server: the move's point when the beat aims straight at it and it is in reach, else out at the beat's angle.
        if (beat.from() == 0.0 && in.aim() != null && flat <= beat.reach() + 1.0 && flat > 0.5) {
            point = new double[] {in.aim()[0], 0.0, in.aim()[2]};
        } else {
            point = direction(facing + Math.toRadians(beat.from()), beat.reach());
        }
        if (beat.limb() == GraftMove.ALL) {
            int n = Math.max(1, in.limbs());
            double spread = n == 1 ? 0.0 : (in.limb() / (double)(n - 1) - 0.5) * 1.6;
            point = add(point, direction(facing + Math.PI * 0.5, spread));
        }
        return new double[] {point[0], 0.05, point[2]};
    }

    /** The heading of {@code aim} from his chest (radians, 0 straight ahead, + to his right); straight ahead for none. */
    static double facing(double[] aim) {
        if (aim == null || Math.abs(aim[0]) + Math.abs(aim[2]) < 1.0E-6) {
            return 0.0;
        }
        return Math.atan2(aim[0], aim[2]);
    }

    /** A level direction {@code angle} round from straight ahead (radians, + to his right), {@code reach} long. */
    static double[] direction(double angle, double reach) {
        return new double[] {Math.sin(angle) * reach, 0.0, Math.cos(angle) * reach};
    }

    /** A point {@code up} above the root and {@code out} along the way to {@code target}, for a limb's point drawn back aimed at it. */
    static double[] aimed(double[] target, double[] root, double out, double up) {
        double[] level = sub(target, root);
        level[1] = 0.0;
        double[] way = length(level) < 1.0E-6 ? new double[] {0.0, 0.0, 1.0} : normalize(level);
        return new double[] {way[0] * out * 0.4, up, way[2] * out * 0.4 - 0.3};
    }

    /** {@code points} as {@link CentipedeMotion#POINTS} points along their spline, over {@code reach} (or the rest length bowed out). */
    static double[][] stretched(double[][] points, double reach, double rest) {
        if (reach <= rest + 1.0E-6) {
            return CentipedeMotion.fit(points, rest, 1, points.length - 2);
        }
        return CentipedeMotion.resample(points, reach);
    }

    /** Each point moved {@code amount} of the way from {@code from} to {@code to} (resampled to a common count first). */
    static double[][] blendTo(double[][] from, double[][] to, double amount) {
        int count = 8;
        double[][] a = sample(from, count);
        double[][] b = sample(to, count);
        double[][] out = new double[count][];
        for (int i = 0; i < count; i++) {
            out[i] = lerp(a[i], b[i], amount);
        }
        return out;
    }

    /** {@code count} points evenly along the spline through {@code points}. */
    static double[][] sample(double[][] points, int count) {
        double[][] dense = CentipedeMotion.resample(points, curveLength(points));
        double[][] out = new double[count][];
        for (int i = 0; i < count; i++) {
            double x = i * (dense.length - 1) / (double)(count - 1);
            int j = Math.min(dense.length - 2, (int)Math.floor(x));
            out[i] = lerp(dense[j], dense[j + 1], x - j);
        }
        return out;
    }

    /** A whole design for {@code side}, scaled about its root to be {@code length} long and moved to {@code root}. */
    static double[][] whole(double[][] design, int side, double length, double[] root) {
        double k = length / curveLength(design);
        double[] from = design[0];
        double[][] out = new double[design.length][];
        for (int i = 0; i < design.length; i++) {
            out[i] = new double[] {root[0] + (design[i][0] - from[0]) * k * side, root[1] + (design[i][1] - from[1]) * k,
                root[2] + (design[i][2] - from[2]) * k};
        }
        out[0] = root.clone();
        return out;
    }

    static double curveLength(double[][] points) {
        return CentipedeMotion.curveLength(points);
    }

    /** Points turned about the vertical through {@code about} by {@code angle} (positive toward his right). */
    static double[][] turn(double[][] points, double[] about, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double[][] out = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double x = points[i][0] - about[0];
            double z = points[i][2] - about[2];
            out[i] = new double[] {about[0] + x * c + z * s, points[i][1], about[2] - x * s + z * c};
        }
        return out;
    }

    /**
     * Every limb's root and tip at rest (rig space), for tests and previews: {root, tip} per limb.
     */
    public static double[][][] restTips(GraftStage stage, int mastery) {
        int n = stage.limbs(mastery);
        double[][][] out = new double[n][][];
        for (int limb = 0; limb < n; limb++) {
            Pose pose = pose(new Input(stage, n, limb, stage.length(mastery), Body.STILL, GraftMove.NONE, 0.0, null, null, null, 0.3));
            out[limb] = new double[][] {pose.points()[0], pose.points()[pose.points().length - 1]};
        }
        return out;
    }

    /** The beats of {@code move} as a list (a convenience for callers that only have the move). */
    public static List<GraftMove.Beat> beats(GraftMove move, int limbs) {
        return move.beats(limbs);
    }

    static double smooth(double value) {
        double t = clamp01(value);
        return t * t * (3.0 - 2.0 * t);
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] mul(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    static double length(double[] a) {
        return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    }

    static double[] normalize(double[] a) {
        double l = length(a);
        return l < 1.0E-9 ? new double[] {0.0, 0.0, 1.0} : mul(a, 1.0 / l);
    }
}
