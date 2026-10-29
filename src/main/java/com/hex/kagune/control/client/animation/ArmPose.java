package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.pale.ArmRules;
import com.hex.kagune.control.pale.PaleFleshModel;

/**
 * 6.2.0, the Mutated Arm (pure, tested): where her right arm of flesh is this frame. Rig space throughout (blocks; x to her right,
 * y up from her feet, z forward). Her own right arm stays hers: she points it where it goes ({@link #point}), and the flesh bursts
 * out of her hand ({@link #root}) and runs on out the way it points (bowed down a little by its own weight) as far as it is out
 * (measured from her shoulder, as the server measures it: {@link #shoulder}), as thick as it has swollen, its great hand at the
 * end; what it holds is where its hand is.
 */
public final class ArmPose {
    /**
     * Where its reach is measured from (as the server measures it): the top of her right arm, where it joins the shoulder (torso
     * pixels: her own arm's pivot is 5 out and 2 down; this is a little out from it and under it).
     */
    static final double[] SHOULDER = {-6.5, 2.5, 0.0};
    /**
     * Where it bursts out of her (arm pixels): all round her right arm from just above her elbow ({@link PaleFleshModel#ARM_SHEATH})
     * down to the end of her fist, and on out of it; her elbow (the way her forearm points).
     */
    static final double[] SHEATH = {-1.0, 3.0, 0.0};
    static final double[] HAND = {-1.0, 10.0, 0.0};
    static final double[] ELBOW = {-1.0, 4.0, 0.0};
    static final double[] ARM_TOP = {-1.0, 0.0, 0.0};
    /** How many points it runs through, and how much (of its length) its weight bows it down. */
    static final int POINTS = 9;
    static final double SAG = 0.035;
    /** The most (blocks) it bows down in the middle, however far out it is: held up, never dragging on the ground. */
    static final double MOST_SAG = 0.55;
    /** How far (blocks) it carries on out of her hand the way her forearm points as it curves round to its end. */
    static final double BEND = 0.7;

    private ArmPose() {
    }

    /** How far out it is ({@code length}, blocks), how thick ({@code girth}), and its hand (open, shut on what it holds, a fist). */
    public record Reach(double length, double girth, PaleFleshModel.Shape hand) {
    }

    /**
     * The arm in {@code phase}, {@code age} ticks into it: it was out {@code from} as this began, its charge then {@code charge}, and
     * it had been held {@code held} ticks as this began; {@code holding} whether it holds anything; {@code afterSphere} whether it
     * draws back in after the sphere (slowly) or after a drag (quickly).
     */
    public static Reach reach(ArmRules.Phase phase, double age, double from, double charge, double held, boolean holding, boolean afterSphere) {
        return switch (phase) {
            case SHOOT -> {
                // Held, it shoots out as far as the charge reaches as it gets there (from the charge's own girth); tapped, a tap's.
                double c = held > 0.0 ? ArmRules.charge(held + age) : 0.0;
                yield new Reach(ArmRules.shot(age, ArmRules.shootTo(held)), ArmRules.girth(c), hand(holding));
            }
            case HOLD -> {
                double c = ArmRules.charge(held + age);
                yield new Reach(ArmRules.reach(c), ArmRules.girth(c), hand(holding));
            }
            case DRAG -> new Reach(ArmRules.dragged(age, from, charge), ArmRules.girth(charge), hand(holding));
            case SPHERE -> new Reach(from, ArmRules.girth(1.0), PaleFleshModel.Shape.ARM_FIST); // its claws folded in, inside it
            case RETRACT -> {
                double ticks = afterSphere ? ArmRules.RETRACT_TICKS : ArmRules.RETRACT_QUICK;
                double was = ArmRules.girth(afterSphere ? 1.0 : charge);
                // Its claws folded back in as it goes.
                yield new Reach(ArmRules.retracted(age, from, ticks), ArmRules.unswollen(age, was, ticks), PaleFleshModel.Shape.ARM_FIST);
            }
            default -> new Reach(ArmRules.ARM, ArmRules.girth(0.0) * 0.6, PaleFleshModel.Shape.ARM_FIST);
        };
    }

    static PaleFleshModel.Shape hand(boolean holding) {
        return holding ? PaleFleshModel.Shape.ARM_SHUT : PaleFleshModel.Shape.ARM_OPEN;
    }

    /**
     * Her body under the arm's weight, added onto {@code p}: the arm {@code reach} out in {@code phase}, {@code age} ticks in, pointing
     * {@code aim} (rig space, a unit vector). Thrown out, the right shoulder drives after it, the hips behind it; held, she carries
     * it, its weight dragging her right shoulder forward and down, the rest of her leaning away and her left arm out behind to
     * balance it, the heavier the more it swells (and straining at the full charge); the drag back is a haul, her whole body
     * turning and sitting back into it; the sphere's squeezes go through her; drawn back in, she comes back to herself.
     */
    public static void body(RigPose p, ArmRules.Phase phase, double age, Reach reach, double[] aim, boolean afterSphere, double time) {
        double out = clamp01((reach.length() - ArmRules.ARM) / 3.0);
        double swell = clamp01((reach.girth() - ArmRules.girth(0.0)) / (ArmRules.girth(1.0) - ArmRules.girth(0.0)));
        double heft = 0.6 + 0.4 * swell;
        // Carrying it where it points: the right shoulder after it, the body leaning away from its weight.
        double toward = Math.atan2(aim[0], aim[2]);
        double yaw = Math.max(-0.6, Math.min(0.6, toward * 0.45));
        // Her whole body turns after it a little (spin + is to her left; the arm is out to her right of where she faces).
        double spin = -Math.max(-0.5, Math.min(0.5, toward * 0.35));
        double down = Math.max(-0.4, Math.min(0.6, -aim[1]));
        double twist = (-0.32 + yaw) * heft;
        double lean = (0.1 + 0.22 * down) * heft;
        double side = 0.13 * heft;
        double drop = 1.0 * heft;
        double lunge = 0.0;
        double balance = heft;
        switch (phase) {
            case SHOOT -> {
                // Thrown: the shoulder snaps after it in two ticks and drives on past, the weight lunging in behind.
                double e = smooth(age / 2.0);
                double kick = Math.sin(Math.PI * clamp01(age / 4.0));
                twist = twist * e - 0.14 * kick;
                lean = lean * e + 0.12 * kick;
                side *= e;
                drop *= e;
                lunge = 1.6 * Math.sin(Math.PI * clamp01(age / 5.0));
                balance *= e;
            }
            case HOLD -> {
                double strain = clamp01((swell - 0.6) / 0.4);
                twist += 0.015 * strain * Math.sin(time * 2.3);
                drop += 0.6 * strain;
            }
            case DRAG -> {
                // Hauled in: she turns and sits back into it, a step back as it comes, flinging it out past her right side.
                double u = clamp01(age / ArmRules.DRAG_TICKS);
                double h = smooth(u);
                twist += (0.36 - twist) * h;
                lean += (-0.09 - lean) * h;
                side += (-0.05 - side) * h;
                drop += 0.9 * Math.sin(Math.PI * u);
                lunge = -1.3 * Math.sin(Math.PI * u);
            }
            case SPHERE -> {
                // Braced, the arm out like a bough under its fruit; each squeeze goes through her.
                double squeeze = ArmRules.sphere(age)[1];
                twist -= 0.07 * squeeze;
                lean += 0.07 * squeeze;
                drop += 0.8 * squeeze;
                side += 0.03 * squeeze;
            }
            case RETRACT -> {
                double ticks = afterSphere ? ArmRules.RETRACT_TICKS : ArmRules.RETRACT_QUICK;
                double r = 1.0 - smooth(age / ticks);
                if (!afterSphere) {
                    twist = 0.36;
                    lean = -0.09;
                    side = -0.05;
                    drop = heft;
                }
                twist *= r;
                lean *= r;
                side *= r;
                drop *= r;
                balance *= r;
            }
            default -> {
                return;
            }
        }
        double carried = phase == ArmRules.Phase.SHOOT || phase == ArmRules.Phase.RETRACT ? 1.0 : Math.max(0.35, out);
        double turned = phase == ArmRules.Phase.SHOOT ? smooth(age / 2.0) : phase == ArmRules.Phase.RETRACT
            ? 1.0 - smooth(age / (afterSphere ? ArmRules.RETRACT_TICKS : ArmRules.RETRACT_QUICK)) : 1.0;
        p.spin += (float)(spin * turned);
        p.twist += (float)(twist * carried);
        p.lean += (float)(lean * carried);
        p.side += (float)(side * carried);
        p.drop += (float)(drop * carried);
        p.lunge += (float)lunge;
        // Her left arm out behind her to balance it.
        p.leftArmX += (float)(0.35 * balance * carried);
        p.leftArmZ -= (float)(0.3 * balance * carried);
        p.leftElbow += (float)(0.35 * balance * carried);
        // Her own right arm pointed out the way it goes, the flesh bursting out round it; drawn back in, it comes down level ahead of
        // her as the flesh comes home.
        double[] pointed = aim;
        if (phase == ArmRules.Phase.RETRACT) {
            double home = smooth(age / ((afterSphere ? ArmRules.RETRACT_TICKS : ArmRules.RETRACT_QUICK) * 0.5));
            double facing = -p.spin;
            pointed = PaleFleshPose.normalize(new double[] {aim[0] + (Math.sin(facing) - aim[0]) * home, aim[1] * (1.0 - home),
                aim[2] + (Math.cos(facing) - aim[2]) * home});
        }
        point(p, pointed, pointing(phase, age, afterSphere));
    }

    /**
     * How far (0..1) her own right arm is pointed out the way the flesh goes, {@code age} ticks into {@code phase}: thrown up at
     * once as it shoots, held out the whole time it is out, lowered once the flesh is nearly back in her hand.
     */
    public static double pointing(ArmRules.Phase phase, double age, boolean afterSphere) {
        return switch (phase) {
            case SHOOT -> smooth(age / RAISE);
            case HOLD, DRAG, SPHERE -> 1.0;
            case RETRACT -> 1.0 - smooth((age / (afterSphere ? ArmRules.RETRACT_TICKS : ArmRules.RETRACT_QUICK) - 0.6) / 0.4);
            default -> 0.0;
        };
    }

    /** How long (ticks) she takes to throw her arm up as it shoots. */
    static final double RAISE = 1.5;

    /**
     * Her right arm on {@code p} pointed along {@code aim} (rig space, a unit vector), {@code weight} of the way from where it is
     * (and as much of vanilla's own swing of it dropped): its turn found by a few steps of a small solve on the exact model
     * ({@link BodyFrame}), so it points there however her body is turned, leaned or bent, the arm straight.
     */
    public static void point(RigPose p, double[] aim, double weight) {
        if (weight <= 0.0) {
            return;
        }
        aim = reachable(aim, p.spin, p.twist);
        float[] was = {p.rightArmX, p.rightArmY, p.rightArmZ, p.rightElbow};
        float hold = p.rightArmHold;
        p.rightArmHold = 1.0F;
        p.rightArmZ = 0.0F;
        p.rightElbow = 0.0F;
        // Straight out ahead, then a damped Gauss-Newton on the arm's swing (X) and its turn about itself (Y): a raised arm's Y
        // swings it across, so the two between them point it anywhere ahead of her (her hand kept off her face as it does).
        double[] v = {-Math.PI / 2.0 - Math.asin(Math.max(-1.0, Math.min(1.0, aim[1]))), 0.0};
        double[] head = BodyFrame.world(p, BodyFrame.model(p, "head", HEAD));
        BodyFrame.Solid solid = new BodyFrame.Solid(p, false);
        for (int step = 0; step < SOLVE_STEPS; step++) {
            double[] r = miss(p, v, aim, head, solid);
            if (dot(r, r) < 1.0E-8) {
                break;
            }
            double[][] j = new double[2][];
            for (int k = 0; k < 2; k++) {
                double[] nudged = v.clone();
                nudged[k] += 1.0E-3;
                double[] rk = miss(p, nudged, aim, head, solid);
                j[k] = new double[r.length];
                for (int i = 0; i < r.length; i++) {
                    j[k][i] = (rk[i] - r[i]) / 1.0E-3;
                }
            }
            double a = dot(j[0], j[0]) + DAMP;
            double b = dot(j[0], j[1]);
            double d = dot(j[1], j[1]) + DAMP;
            double g0 = -dot(j[0], r);
            double g1 = -dot(j[1], r);
            double det = a * d - b * b;
            if (Math.abs(det) < 1.0E-12) {
                break;
            }
            v[0] += Math.max(-0.6, Math.min(0.6, (d * g0 - b * g1) / det));
            v[1] += Math.max(-0.6, Math.min(0.6, (a * g1 - b * g0) / det));
            v[1] = Math.max(-1.5, Math.min(1.5, v[1]));
        }
        float w = (float)Math.min(1.0, weight);
        p.rightArmX = was[0] + ((float)v[0] - was[0]) * w;
        p.rightArmY = was[1] + ((float)v[1] - was[1]) * w;
        p.rightArmZ = was[2] * (1.0F - w);
        p.rightElbow = was[3] * (1.0F - w);
        p.rightArmHold = Math.max(hold, w);
    }

    /**
     * The way she points her right arm for {@code aim} (rig space, a unit vector), her body turned {@code spin} (+ to her left) and her
     * chest {@code twist} on it (+ her right shoulder back): never further across her chest than {@link #ACROSS} to her left (less and less as it goes up past {@link #RAISED}: her hand would end up
     * in front of her face) nor more than {@link #UP} up or {@link #DOWN} down (a right arm pointed further would run across her
     * chest or face); the flesh curves on round from her hand to where it goes.
     */
    public static double[] reachable(double[] aim, double spin, double twist) {
        double level = Math.hypot(aim[0], aim[2]);
        double pitch = Math.atan2(aim[1], level);
        double toward = level < 1.0E-6 ? -spin : Math.atan2(aim[0], aim[2]);
        // Off the way her chest faces (+ to her right).
        double off = wrap(toward + spin - twist);
        double up = Math.max(-DOWN, Math.min(UP, pitch));
        double kept = Math.max(-ACROSS * (1.0 - smooth((up - RAISED) / (UP - RAISED))), off);
        if (kept == off && up == pitch) {
            return aim;
        }
        double yaw = kept - spin + twist;
        return new double[] {Math.sin(yaw) * Math.cos(up), Math.sin(up), Math.cos(yaw) * Math.cos(up)};
    }

    static final double ACROSS = Math.toRadians(20.0);
    static final double RAISED = Math.toRadians(20.0);
    static final double UP = Math.toRadians(70.0);
    static final double DOWN = Math.toRadians(60.0);

    static double wrap(double angle) {
        return angle - Math.PI * 2.0 * Math.floor((angle + Math.PI) / (Math.PI * 2.0));
    }

    static final int SOLVE_STEPS = 8;
    static final double DAMP = 1.0E-3;

    /**
     * How far off {@code aim} her right arm points with its swing and turn {@code v}, and (weighed heavily) how far inside
     * {@link #FACE_CLEAR} of the middle of her head ({@code head}) her fist is, and how deep into the rest of her ({@code solid})
     * the flesh round her forearm would be.
     */
    static double[] miss(RigPose p, double[] v, double[] aim, double[] head, BodyFrame.Solid solid) {
        p.rightArmX = (float)v[0];
        p.rightArmY = (float)v[1];
        double[] top = BodyFrame.world(p, BodyFrame.model(p, "rightArm", ARM_TOP));
        double[] hand = BodyFrame.world(p, BodyFrame.model(p, "rightArm", HAND));
        double[] way = PaleFleshPose.normalize(PaleFleshPose.sub(hand, top));
        double inside = Math.max(0.0, FACE_CLEAR - distance(hand, head));
        double sheathed = sheathDepth(solid, BodyFrame.world(p, BodyFrame.model(p, "rightArm", SHEATH)), hand, 0.0);
        return new double[] {way[0] - aim[0], way[1] - aim[1], way[2] - aim[2], 6.0 * inside, 8.0 * sheathed};
    }

    /**
     * How deep (blocks) the flesh round her forearm, from {@code root} to {@code fist}, as thick as its lumps go and {@code spare} more,
     * is in {@code solid}.
     */
    static double sheathDepth(BodyFrame.Solid solid, double[] root, double[] fist, double spare) {
        double deepest = 0.0;
        double length = distance(root, fist);
        for (int i = 0; i <= 6; i++) {
            double t = i / 6.0;
            double[] at = {root[0] + (fist[0] - root[0]) * t, root[1] + (fist[1] - root[1]) * t, root[2] + (fist[2] - root[2]) * t};
            double need = PaleFleshModel.sheathRadius(t * length) * SHEATH_LUMPS + SHEATH_MARGIN + spare;
            deepest = Math.max(deepest, solid.depth(at, need) * BodyFrame.SCALE / 16.0);
        }
        return deepest;
    }

    /**
     * Whether keeping clear of her face is what stops her pointed arm on {@code p} (her fist at the least it is kept from the middle
     * of her head, or the flesh round her forearm up against the rest of her): then it points as near where it goes as it can.
     */
    public static boolean faceBound(RigPose p) {
        double[] head = BodyFrame.world(p, BodyFrame.model(p, "head", HEAD));
        double[] fist = fist(p);
        return distance(fist, head) < FACE_CLEAR + 0.01 || sheathDepth(new BodyFrame.Solid(p, false), root(p), fist, 0.02) > 0.0;
    }

    /**
     * The middle of her head (head pixels), and how far from it (blocks) her fist and the flesh's root round her forearm are kept:
     * her head and the flesh round them.
     */
    public static final double[] HEAD = {0.0, -4.0, 0.0};
    public static final double FACE_CLEAR = 0.44;
    /** How far out its lumps take the flesh round her forearm (of its thickness), and how much more it is kept off the rest of her. */
    static final double SHEATH_LUMPS = 1.36;
    static final double SHEATH_MARGIN = 0.015;

    /** The way her right arm points (rig space, a unit vector): from its top to her hand. */
    public static double[] way(RigPose p) {
        double[] top = BodyFrame.world(p, BodyFrame.model(p, "rightArm", ARM_TOP));
        double[] hand = BodyFrame.world(p, BodyFrame.model(p, "rightArm", HAND));
        return PaleFleshPose.normalize(PaleFleshPose.sub(hand, top));
    }

    static double dot(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /** The most (degrees) her body is turned from the way the arm points: it squares her up to it, a little at a time. */
    public static final double SQUARE = 25.0;
    /** How much (of the way, a tick) her body turns after the arm. */
    public static final double SQUARING = 0.35;

    /**
     * Her body's facing (degrees, Minecraft's yaw) a tick on from {@code bodyYaw}, the arm pointing along {@code aim} (the world):
     * turned after it, and never more than {@link #SQUARE} from it.
     */
    public static float squared(float bodyYaw, double[] aim) {
        if (aim[0] * aim[0] + aim[2] * aim[2] < 1.0E-6) {
            return bodyYaw;
        }
        double toward = Math.toDegrees(Math.atan2(-aim[0], aim[2]));
        double off = wrapDegrees(toward - bodyYaw) * (1.0 - SQUARING);
        off = Math.max(-SQUARE, Math.min(SQUARE, off));
        return (float)(bodyYaw + wrapDegrees(toward - bodyYaw) - off);
    }

    static double wrapDegrees(double d) {
        double w = d % 360.0;
        if (w >= 180.0) {
            w -= 360.0;
        }
        if (w < -180.0) {
            w += 360.0;
        }
        return w;
    }

    /** Where its reach is measured from on {@code body} (rig space): the top of her right arm, as the server has it. */
    public static double[] shoulder(RigPose body) {
        return BodyFrame.world(body, BodyFrame.model(body, "torso", SHOULDER));
    }

    /** The way her right forearm points on {@code body} (rig space, a unit vector): the way the flesh leaves her hand. */
    public static double[] forearm(RigPose body) {
        double[] elbow = BodyFrame.world(body, BodyFrame.model(body, "rightArm", ELBOW));
        return PaleFleshPose.normalize(PaleFleshPose.sub(fist(body), elbow));
    }

    /** Where it bursts out of {@code body} (rig space): round her right arm, just above her elbow. */
    public static double[] root(RigPose body) {
        return BodyFrame.world(body, BodyFrame.model(body, "rightArm", SHEATH));
    }

    /** The end of her right fist on {@code body} (rig space), out of which it runs on. */
    public static double[] fist(RigPose body) {
        return BodyFrame.world(body, BodyFrame.model(body, "rightArm", HAND));
    }

    /** How far behind what it holds (blocks, along it) the end of the arm is: its hand is round it ({@link ArmRules#handBack}). */
    public static double handBack(double girth) {
        return ArmRules.handBack(girth);
    }

    /** As thick as it is, no thicker than its hand fits {@code out} blocks from her shoulder ({@link ArmRules#fits}). */
    public static double girth(double girth, double out) {
        return Math.min(girth, ArmRules.fits(out));
    }

    /** Where the arm ends holding what has its middle at {@code at}: its hand round it, from {@code root} ({@code girth} thick). */
    public static double[] holding(double[] root, double[] at, double girth) {
        double[] way = PaleFleshPose.normalize(PaleFleshPose.sub(at, root));
        double back = handBack(girth);
        return new double[] {at[0] - way[0] * back, at[1] - way[1] * back, at[2] - way[2] * back};
    }

    /**
     * Where the arm ends out of {@code root} along {@code aim} (a unit vector), {@code length} out to the middle of its hand (where
     * the server catches what it meets, and where what it holds is): the arm itself ends {@link #handBack} short of that.
     */
    public static double[] out(double[] root, double[] aim, double length, double girth) {
        double to = Math.max(0.3, length - handBack(girth));
        return new double[] {root[0] + aim[0] * to, root[1] + aim[1] * to, root[2] + aim[2] * to};
    }

    /** Her own arm hanging at her side (rig space, a unit vector): the way the arm turns to as it goes back into her. */
    static final double[] HANGING = {0.12, -0.9855, 0.12};

    /** How far out (blocks, past her arm's length) it points all the way where it is aimed; shorter, it hangs toward her side. */
    static final double SWINGS = 2.0;

    /**
     * The way it points this frame, {@code length} out: where it is aimed ({@code aim}); as short as her own arm, hanging at her side
     * where her arm is, swinging between the two as it shoots out of her and draws back into her.
     */
    public static double[] aimNow(double[] aim, double length) {
        return aimNow(aim, length, HANGING);
    }

    /** {@link #aimNow}, as short as her own arm pointing {@code along} (rig space, a unit vector): her arm's own way. */
    public static double[] aimNow(double[] aim, double length, double[] along) {
        double u = 1.0 - smooth((length - ArmRules.ARM) / SWINGS);
        if (u <= 0.0) {
            return aim;
        }
        return PaleFleshPose.normalize(new double[] {aim[0] + (along[0] - aim[0]) * u, aim[1] + (along[1] - aim[1]) * u,
            aim[2] + (along[2] - aim[2]) * u});
    }

    public static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    static double clamp01(double u) {
        return Math.max(0.0, Math.min(1.0, u));
    }

    static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3.0 - 2.0 * c);
    }

    /** The way back to {@code root} from the sphere's middle {@code centre} (a unit vector): where the arm joins it. */
    public static double[] back(double[] root, double[] centre) {
        return PaleFleshPose.normalize(PaleFleshPose.sub(root, centre));
    }

    /** Where the arm ends on the sphere round {@code centre}, {@code radius} big: just inside it, the flesh it grew into. */
    public static double[] onSphere(double[] root, double[] centre, double radius) {
        double[] back = back(root, centre);
        return new double[] {centre[0] + back[0] * radius * 0.9, centre[1] + back[1] * radius * 0.9, centre[2] + back[2] * radius * 0.9};
    }

    /**
     * The arm on {@code body} out to {@code end} (rig space: where the arm ends, its hand beyond), {@code girth} thick, with its
     * {@code hand}.
     */
    public static PaleFleshModel.Pose pose(RigPose body, double[] end, double girth, PaleFleshModel.Shape hand, double time, int seed) {
        double[] root = root(body);
        double[] fist = fist(body);
        double[] exit = forearm(body);
        double[] d = {end[0] - fist[0], end[1] - fist[1], end[2] - fist[2]};
        double length = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        if (length < 0.2 || dot(d, exit) < 0.0) {
            return null; // back inside her arm (or not yet out past her fist)
        }
        // It bursts out all round her forearm, down it and out of her fist the way her forearm points, and curves round to its end,
        // bowed down by its weight: its points evenly along that.
        double bend = Math.min(BEND, length * 0.3);
        double[] control = {fist[0] + exit[0] * bend, fist[1] + exit[1] * bend, fist[2] + exit[2] * bend};
        int sheath = 8;
        int dense = 128;
        double[][] curve = new double[sheath + dense + 1][];
        double[] along = new double[sheath + dense + 1];
        for (int i = 0; i < sheath; i++) {
            double t = i / (double)sheath;
            curve[i] = new double[] {root[0] + (fist[0] - root[0]) * t, root[1] + (fist[1] - root[1]) * t, root[2] + (fist[2] - root[2]) * t};
            along[i] = i == 0 ? 0.0 : along[i - 1] + distance(curve[i], curve[i - 1]);
        }
        for (int i = 0; i <= dense; i++) {
            double t = i / (double)dense;
            double a = (1.0 - t) * (1.0 - t);
            double b = 2.0 * t * (1.0 - t);
            double c = t * t;
            double sag = Math.min(SAG * length, MOST_SAG) * 4.0 * t * (1.0 - t);
            int k = sheath + i;
            curve[k] = new double[] {fist[0] * a + control[0] * b + end[0] * c, fist[1] * a + control[1] * b + end[1] * c - sag,
                fist[2] * a + control[2] * b + end[2] * c};
            along[k] = along[k - 1] + distance(curve[k], curve[k - 1]);
        }
        dense = sheath + dense;
        // Her arm's own line held (the root above her elbow, her fist), then evenly on along the curve.
        int points = Math.max(POINTS, Math.min(24, (int)Math.ceil(along[dense] / 1.5) + 6));
        double[][] path = new double[points][];
        path[0] = root;
        path[1] = fist;
        double from = along[sheath];
        int j = sheath + 1;
        for (int i = 2; i < points; i++) {
            double want = from + (along[dense] - from) * (i - 1) / (points - 2.0);
            while (j < dense && along[j] < want) {
                j++;
            }
            double span = Math.max(1.0E-9, along[j] - along[j - 1]);
            double f = clamp01((want - along[j - 1]) / span);
            path[i] = new double[] {curve[j - 1][0] + (curve[j][0] - curve[j - 1][0]) * f, curve[j - 1][1] + (curve[j][1] - curve[j - 1][1]) * f,
                curve[j - 1][2] + (curve[j][2] - curve[j - 1][2]) * f};
        }
        BodyFrame.Solid solid = new BodyFrame.Solid(body, false);
        double[] up = {0.0, 1.0, 0.0};
        path = PaleFleshPose.clear(solid, path, up, root, girth * 1.5, (s, total) -> PaleFleshModel.armRadius(s, girth, total));
        // Evenly along it again: its thickness is laid along it by length.
        path = PaleFleshModel.grownAlong(path, up, 1.0);
        if (path == null) {
            return null;
        }
        return new PaleFleshModel.Pose(path, up, solid.axis(), 1.0, hand, girth, time, seed,
            PaleFleshPose.keep(solid));
    }
}
