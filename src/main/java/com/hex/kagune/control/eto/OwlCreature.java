package com.hex.kagune.control.eto;

import com.hex.kagune.control.ayato.UkakuModel;

import static com.hex.kagune.control.eto.OwlModel.add;
import static com.hex.kagune.control.eto.OwlModel.dot;
import static com.hex.kagune.control.eto.OwlModel.mul;
import static com.hex.kagune.control.eto.OwlModel.normalize;
import static com.hex.kagune.control.eto.OwlModel.smooth;
import static com.hex.kagune.control.eto.OwlModel.sub;

/**
 * 6.2.4: the One-Eyed Owl as a whole creature (pure, tested), as the user built it in Blockbench (art/one_eyed_owl.bbmodel, baked
 * smooth by tools/owl_smooth.py into {@link OwlMesh}): the horned skull with its one red eye, its jaw, the hood and the dark cloak,
 * the red pads on its shoulders, the crest of spikes, the long bone arms planted on their clawed hands. In the Owl the player's own
 * body is not drawn: this is, in its place, moved by everything that moves her. Our own smooth kagune limbs grow out of its shoulders
 * where its blades were.
 *
 * <p>It is posed off her ({@link #rig}, from a {@link Source}): turned the way her torso faces and leaning as it leans, its head
 * looking where she looks, its hands reaching where her arms carry them; it drags itself along on its two long arms as she walks
 * (each reaching ahead, planting and hauling back in turn, its body lurching with each haul); idle, it breathes, its crest stirs,
 * and every so often its jaw drops wide open.
 *
 * <p>Rig space throughout (blocks: x to her right, y up from her feet, z forward). Its kagune limbs are drawn {@link Rig#size} times
 * as big about their own roots ({@link OwlModel#limb}), so they stay attached.
 */
public final class OwlCreature {
    /** How much bigger than her it stands at its full size (it swells up out of her from her own). */
    public static final double SCALE = 2.2;
    /** The size its kagune limbs are given at (they are drawn {@code scale / BASE} times as big), and how much bigger again at its size. */
    static final double BASE = 1.45;
    static final double KAGUNE = 2.1;
    /** How many of their segments its kagune limbs are drawn in (of hers: the creature has a great deal else to draw). */
    static final double KAGUNE_DETAIL = 0.72;
    /** How long (blocks) one of the model's pixels is at its full size (sixteen a block, as Blockbench builds it). */
    public static final double PIXEL = 1.0 / 16.0;
    /** The frame the kagune is swung off: how long (blocks) one of its pixels is (her torso's pixels are 0.0586). */
    public static final double KAGUNE_PIXEL = 0.046;
    /**
     * Her Owl stance (OwlPose.body): how far (radians) her torso leans forward in it, how far down she looks, how her right arm hangs
     * (her pixels: her upper arm four of them, her forearm six; her left the same across). The creature is built standing so: it
     * follows only her moving off it.
     */
    static final double STANCE_LEAN = 0.34;
    static final double STANCE_LOOK = -0.2;
    static final double[] STANCE_ARM = {1.28, -9.44, -2.69};
    /** How much of her arms' moving off her stance its hands follow (the moves lift them). */
    static final double ARM_FOLLOW = 0.6;
    /** Its crawl (the model's pixels): how far a hand reaches ahead of and behind its place at a run, how high it is lifted. */
    static final double REACH = 9.0;
    static final double LIFT = 11.0;
    /** How much of each stride a hand is planted, hauling (the rest it is lifted and swung ahead again). */
    static final double PLANTED = 0.6;
    /** The idle's yawn (ticks): how long from one to the next; how wide (radians) its jaw drops. */
    static final double YAWN = 230.0;
    static final double JAW = Math.toRadians(64.0);

    private OwlCreature() {
    }

    /**
     * Her body as posed, in rig space, for the creature to be carried over from: her torso's and head's frames (her own pixels),
     * each upper arm's and forearm's way, each hand's frame, each foot's sole; how fast she walks (0 still .. 1 a run) and how far
     * into her stride (radians), whether she is off the ground, the time (ticks), and how grown the Owl is (0..1); 6.2.4: how much
     * of her walk is a sprint (0..1: it gallops), how long (ticks) she has been off the ground and since she last landed (a great
     * many: long ago), and what it is doing in one of its moves ({@link OwlActs}; null: none).
     */
    public record Source(OwlModel.Frame torso, OwlModel.Frame head, double[] rightUpper, double[] rightFore, double[] leftUpper, double[] leftFore,
                         OwlModel.Frame rightHand, OwlModel.Frame leftHand, double[] rightFoot, double[] leftFoot, double walk, double stride,
                         boolean airborne, double time, double grown, double run, double air, double landed, OwlActs.Act act) {
        public Source(OwlModel.Frame torso, OwlModel.Frame head, double[] rightUpper, double[] rightFore, double[] leftUpper, double[] leftFore,
                      OwlModel.Frame rightHand, OwlModel.Frame leftHand, double[] rightFoot, double[] leftFoot, double walk, double stride,
                      boolean airborne, double time, double grown) {
            this(torso, head, rightUpper, rightFore, leftUpper, leftFore, rightHand, leftHand, rightFoot, leftFoot, walk, stride, airborne, time, grown,
                0.0, airborne ? LONG_AGO : 0.0, LONG_AGO, null);
        }

        /** The same, sprinting {@code run}, off the ground {@code air} ticks, landed {@code landed} ticks ago, acting {@code act}. */
        public Source with(double run, double air, double landed, OwlActs.Act act) {
            return new Source(this.torso, this.head, this.rightUpper, this.rightFore, this.leftUpper, this.leftFore, this.rightHand, this.leftHand,
                this.rightFoot, this.leftFoot, this.walk, this.stride, this.airborne, this.time, this.grown, run, air, landed, act);
        }
    }

    /** Ticks enough to be long ago (a landing so long past it is over). */
    public static final double LONG_AGO = 1.0E6;

    /**
     * The creature posed: its bones ({@code pose}, the model's pixels), where the model's origin is (rig space, on the ground under her)
     * and how long one of its pixels is now ({@code pixel}); its head's frame (x its left, y down, z back; a pixel each), the frame
     * the kagune is swung off (turned as her torso is), the roots its four limbs grow out of (out of the masses on its shoulders, where
     * its blades were) and the crest's; how far grown (0..1), how much bigger its kagune is drawn than at {@link #BASE}, the time; how
     * far its crest is flared (0..1: it burns brighter).
     */
    public record Rig(OwlMesh.Pose pose, double[] origin, double pixel, OwlModel.Frame head, OwlModel.Frame kagune, double[] rootRight,
                      double[] rootLeft, double[] innerRight, double[] innerLeft, double[] crest, double grown, double size, double time, double walk,
                      double flare) {
        /** Where the rest point {@code q} (the model's pixels) of bone {@code b} is, in rig space. */
        public double[] at(int b, double[] q) {
            return place(this.pose.apply(b, q));
        }

        /** The model's posed point {@code m} in rig space (the model faces -z, its right -x: turned half round, a pixel {@code pixel} long). */
        public double[] place(double[] m) {
            return new double[] {this.origin[0] - m[0] * this.pixel, this.origin[1] + m[1] * this.pixel, this.origin[2] - m[2] * this.pixel};
        }

        /** The model's direction {@code d} in rig space. */
        public static double[] way(double[] d) {
            return new double[] {-d[0], d[1], -d[2]};
        }
    }

    /** The bones it moves by name, found once. */
    static final class Bones {
        final int model;
        final int torso;
        final int mantle;
        final int neck;
        final int skull;
        final int jaw;
        final int tongue;
        final int eye;
        final int iris;
        final int hump;
        final int[] segments;
        final int[] crown;
        final int[] upper = new int[2];
        final int[] fore = new int[2];
        final int[] hand = new int[2];
        final int[] shoulder = new int[2];
        final int[][] fingers = new int[2][];
        final int[] blade = new int[2];
        final int[] innerBlade = new int[2];
        final boolean[] head;
        /** Whether each bone is one of its crest's crystals (drawn shining of themselves). */
        final boolean[] crest;
        /** Each crystal of its crest (its bone), where its point is (the model's pixels), and whether it is one of the great ones. */
        final int[] crystal;
        final double[][] point;
        final boolean[] great;

        Bones(OwlMesh m) {
            this.model = 0;
            this.torso = m.bone("torso");
            this.mantle = m.bone("mantle");
            this.neck = m.bone("neck_cowl");
            this.skull = m.bone("head") >= 0 ? m.bone("head") : m.bone("skull_mask");
            this.jaw = m.bone("mask_jaw");
            this.tongue = m.bone("mask_tongue");
            this.eye = m.bone("mask_single_eye");
            this.iris = m.bone("mask_iris");
            this.hump = m.bone("dorsal_hump");
            java.util.List<Integer> seg = new java.util.ArrayList<>();
            for (int k = 2; m.bone("tongue_" + k) >= 0; k++) {
                seg.add(m.bone("tongue_" + k));
            }
            this.segments = seg.stream().mapToInt(Integer::intValue).toArray();
            java.util.List<Integer> crest = new java.util.ArrayList<>();
            this.head = new boolean[m.names.length];
            this.crest = new boolean[m.names.length];
            for (int b = 0; b < m.names.length; b++) {
                String name = m.names[b];
                if (name.startsWith("crown_primary") || name.startsWith("crown_red_branch")) {
                    crest.add(b);
                    this.crest[b] = true;
                }
                this.head[b] = name.startsWith("mask_") || name.equals("head") || name.equals("skull_mask") || name.startsWith("tongue_");
            }
            this.crown = crest.stream().mapToInt(Integer::intValue).toArray();
            this.crystal = this.crown;
            this.point = new double[this.crown.length][];
            this.great = new boolean[this.crown.length];
            for (int i = 0; i < this.crown.length; i++) {
                int b = this.crown[i];
                double[] pv = m.pivot[b];
                double far = -1.0;
                double[] tip = pv;
                for (int q = 0; q < m.quads(); q++) {
                    if (m.bone[q] != b) {
                        continue;
                    }
                    for (int k = 0; k < 4; k++) {
                        double[] c = {m.corners[q * 12 + k * 3], m.corners[q * 12 + k * 3 + 1], m.corners[q * 12 + k * 3 + 2]};
                        double d = dot(sub(c, pv), sub(c, pv));
                        if (d > far) {
                            far = d;
                            tip = c;
                        }
                    }
                }
                this.point[i] = tip;
                this.great[i] = m.names[b].startsWith("crown_primary");
            }
            String[] sides = {"right", "left"};
            for (int i = 0; i < 2; i++) {
                String s = sides[i];
                this.upper[i] = m.bone(s + "_upper_arm");
                this.fore[i] = m.bone(s + "_forearm");
                this.hand[i] = m.bone(s + "_hand");
                this.shoulder[i] = m.bone(s + "_shoulder");
                this.fingers[i] = new int[] {m.bone(s + "_finger_1"), m.bone(s + "_finger_2"), m.bone(s + "_finger_3"), m.bone(s + "_finger_4"),
                    m.bone(s + "_thumb")};
                this.blade[i] = m.bone(s + "_blade_upper");
                this.innerBlade[i] = m.bone(s + "_blade_lower");
            }
        }

        /** Whether every bone it moves is in the mesh (an empty mesh, unread: nothing to pose). */
        boolean whole() {
            return this.torso >= 0 && this.skull >= 0 && this.jaw >= 0 && this.upper[0] >= 0 && this.upper[1] >= 0 && this.fore[0] >= 0 && this.fore[1] >= 0
                && this.hand[0] >= 0 && this.hand[1] >= 0 && this.blade[0] >= 0 && this.blade[1] >= 0 && this.innerBlade[0] >= 0 && this.innerBlade[1] >= 0;
        }
    }

    private static Bones bones;

    static synchronized Bones bones() {
        if (bones == null) {
            bones = new Bones(OwlMesh.get());
        }
        return bones;
    }

    static final double[] X = {1.0, 0.0, 0.0};
    static final double[] Y = {0.0, 1.0, 0.0};
    static final double[] Z = {0.0, 0.0, 1.0};

    // ------------------------------------------------------------------ posed

    /** How much bigger than her (1 her own size .. {@link #SCALE}) it is as it grows out of her ({@code grown} 0..1). */
    public static double scale(double grown) {
        return 1.0 + (SCALE - 1.0) * smooth(Math.max(0.0, Math.min(1.0, grown)) / 0.7);
    }

    /**
     * The rig-space point {@code r} as the creature's own pixels at {@code grown} (x its left, y up, z back, from the ground under her;
     * as it faces her way: where its moves reach, {@link OwlActs}).
     */
    public static double[] model(double[] r, double grown) {
        double pixel = PIXEL * scale(grown) / SCALE;
        return new double[] {-r[0] / pixel, r[1] / pixel, -r[2] / pixel};
    }

    /**
     * How the act {@code a} turns and tips the whole of it and its body (spun round, turned, rolled, leant), as a turn of rig space (the
     * creature facing {@code yaw} in it).
     */
    static double[][] acted(OwlActs.Act a, double yaw) {
        double[][] d = OwlMesh.times(OwlMesh.about(Y, a.spin() + a.turn()), OwlMesh.times(OwlMesh.about(Z, a.roll()), OwlMesh.about(X, -a.pitch())));
        double[][] w = {{-1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, -1.0}};
        double[][] facing = OwlMesh.about(Y, yaw);
        double[][] back = OwlMesh.about(Y, -yaw);
        return OwlMesh.times(w, OwlMesh.times(facing, OwlMesh.times(d, OwlMesh.times(back, w))));
    }

    /** The creature posed off {@code s}. */
    public static Rig rig(Source s) {
        OwlMesh mesh = OwlMesh.get();
        Bones b = bones();
        OwlModel.Frame t = s.torso();
        double px = t.pixel();
        double[] left = mul(t.x(), 1.0 / px);
        double[] up = mul(t.y(), -1.0 / px);
        double[] fwd = mul(t.z(), -1.0 / px);
        double g = Math.max(0.0, Math.min(1.0, s.grown()));
        double k = scale(g);
        double size = k / BASE * (1.0 + (KAGUNE - 1.0) * smooth(g / 0.7));
        double pixel = PIXEL * k / SCALE;
        double walk = Math.max(0.0, Math.min(1.0, s.walk()));
        double phase = s.stride();
        double time = s.time();
        // On the ground under her (where her pelvis is, carried out as she is scaled), turned the way her torso faces.
        double[] pelvis = mul(t.at(0.0, 12.0, 0.0), k);
        double[] origin = {pelvis[0], 0.0, pelvis[2]};
        double[] flat = {fwd[0], 0.0, fwd[2]};
        double[] flatFwd = dot(flat, flat) < 1.0E-6 ? new double[] {0.0, 0.0, 1.0} : normalize(flat);
        double[] flatRight = {flatFwd[2], 0.0, -flatFwd[0]};
        double yaw = Math.atan2(flatFwd[0], flatFwd[2]);
        OwlMesh.Pose p = new OwlMesh.Pose(mesh);
        p.turn(b.model, OwlMesh.about(Y, yaw));
        if (b.whole()) {
            pose(p, b, s, up, fwd, flatFwd, flatRight, walk, phase, time, px * k / pixel);
        } else {
            p.solve();
        }
        double flare = flare(s, walk);
        Rig bare = new Rig(p, origin, pixel, null, null, null, null, null, null, null, g, size, time, walk, flare);
        // The kagune out of the masses on its shoulders, where its blades were; swung off a frame turned as her torso is.
        double[] rootRight = b.whole() ? bare.at(b.blade[0], mesh.pivot[b.blade[0]]) : origin;
        double[] rootLeft = b.whole() ? bare.at(b.blade[1], mesh.pivot[b.blade[1]]) : origin;
        double[] innerRight = b.whole() ? bare.at(b.innerBlade[0], mesh.pivot[b.innerBlade[0]]) : origin;
        double[] innerLeft = b.whole() ? bare.at(b.innerBlade[1], mesh.pivot[b.innerBlade[1]]) : origin;
        double[] crest = b.hump >= 0 ? bare.at(b.hump, new double[] {0.0, 70.0, 14.0}) : origin;
        double kp = KAGUNE_PIXEL;
        double[] kx = mul(left, kp);
        double[] ky = mul(up, -kp);
        double[] kz = mul(fwd, -kp);
        if (s.act() != null) {
            // Through its own moves the frame is turned and tipped with its body as the move turns and tips it.
            double[][] moved = acted(s.act(), yaw);
            kx = OwlMesh.apply(moved, kx);
            ky = OwlMesh.apply(moved, ky);
            kz = OwlMesh.apply(moved, kz);
        }
        double[] ko = sub(crest, add(mul(ky, OwlModel.CREST_ROOT[1]), mul(kz, OwlModel.CREST_ROOT[2])));
        OwlModel.Frame kagune = new OwlModel.Frame(ko, kx, ky, kz);
        OwlModel.Frame head;
        if (b.skull >= 0) {
            head = new OwlModel.Frame(bare.at(b.skull, mesh.pivot[b.skull]), mul(Rig.way(p.turned(b.skull, X)), pixel),
                mul(Rig.way(p.turned(b.skull, mul(Y, -1.0))), pixel), mul(Rig.way(p.turned(b.skull, Z)), pixel));
        } else {
            head = new OwlModel.Frame(origin, mul(flatRight, -pixel), new double[] {0.0, -pixel, 0.0}, mul(flatFwd, -pixel));
        }
        return new Rig(p, origin, pixel, head, kagune, rootRight, rootLeft, innerRight, innerLeft, crest, g, size, time, walk, flare);
    }

    /**
     * Its bones posed: its body leaning as her torso leans and lurching as it hauls itself along (at a sprint galloping, both arms
     * hauling together, heaving up into each bound), breathing, shifting its weight; reared up as it flies, squashed down as it lands;
     * its head looking where she looks, now and then cocked aside as an owl's; its jaw dropping and its tongue rolling out now and
     * then; its crest stirring; its arms reaching for where its hands are planted ({@link #reach}), its claws flexing. Through one of
     * its moves, the move's act ({@link OwlActs}) on all of it. {@code herPixel}: how many of the model's pixels one of hers is
     * carried over to.
     */
    static void pose(OwlMesh.Pose p, Bones b, Source s, double[] up, double[] fwd, double[] flatFwd, double[] flatRight, double walk, double phase,
                     double time, double herPixel) {
        OwlActs.Act a = s.act();
        double run = Math.max(0.0, Math.min(1.0, s.run())) * walk;
        double idle = a == null ? 1.0 - walk : 0.0;
        // (Its own moves leap and land as they do: OwlActs.)
        double air = s.airborne() && a == null ? smooth(s.air() / 4.0) : 0.0;
        double land = a == null ? landing(s.landed()) : 0.0;
        // Leaning as she leans (forward tips its top toward its front, -z: a turn the wrong way about its x), into its crawl as it
        // walks and further into its gallop, rolling toward the arm hauling it (not at a gallop: both haul), heaving with each haul
        // (twice a stride crawling, once a bound galloping: up as its arms haul it on, down as they reach ahead), breathing. Its own
        // moves are theirs: her lean under them is not carried over.
        double lean = a != null ? 0.0 : Math.max(-0.3, Math.min(0.3, 0.5 * (Math.atan2(dot(up, flatFwd), Math.max(0.2, up[1])) - STANCE_LEAN)));
        double breath = Math.sin(time * 0.09);
        double bound = Math.sin(phase);
        double crawl = 0.5 - 0.5 * Math.cos(phase * 2.0);
        double pitch = lean + 0.07 * walk + 0.08 * run - 0.07 * run * bound + 0.012 * breath - 0.12 * air + 0.2 * land;
        double roll = 0.045 * walk * (1.0 - run) * Math.sin(phase) + 0.03 * idle * Math.sin(time * 0.023);
        double turn = 0.0;
        double lift = -1.1 * walk * (1.0 - run) * crawl + 3.2 * run * (0.5 + 0.5 * bound) + 2.0 * air - 5.0 * land;
        double ahead = 0.0;
        if (a != null) {
            pitch += a.pitch();
            turn += a.turn();
            roll += a.roll();
            lift += a.lift();
            ahead += a.ahead();
        }
        double[][] body = OwlMesh.times(OwlMesh.about(Y, turn), OwlMesh.times(OwlMesh.about(Z, roll), OwlMesh.about(X, -pitch)));
        p.turn(b.torso, body);
        if (a != null && a.spin() != 0.0) {
            p.turn(b.model, OwlMesh.about(Y, a.spin())); // (the whole of it turned round: its whirl)
        }
        p.shift(b.torso, new double[] {0.0, lift, ahead});
        // Its mantle mostly held where it lies on the ground (the body turning in it).
        p.turn(b.mantle, OwlMesh.times(OwlMesh.about(X, pitch * 0.75), OwlMesh.times(OwlMesh.about(Z, -roll * 0.75), OwlMesh.about(Y, -turn * 0.6))));
        // Its shoulders heaving with its breath, rolling as they haul.
        for (int i = 0; i < 2; i++) {
            double side = i == 0 ? 1 : -1;
            p.turn(b.shoulder[i], OwlMesh.times(OwlMesh.about(Z, side * 0.015 * breath), OwlMesh.about(X, 0.05 * walk * Math.sin(phase + (i == 0 ? Math.PI : 0.0)))));
        }
        // Its head looking where she looks (a little of it in its neck), within what its heavy skull allows; idle, its eye flicking
        // about and now and then its head cocked aside a long moment, as an owl's; bobbing as it gallops; bowed, turned and lunged
        // out as its move has it.
        OwlModel.Frame h = s.head();
        double[] face = normalize(mul(h.z(), -1.0));
        double look = Math.max(-0.6, Math.min(0.45, Math.asin(Math.max(-1.0, Math.min(1.0, face[1]))) - STANCE_LOOK + pitch));
        double headTurn = Math.max(-0.9, Math.min(0.9, Math.atan2(dot(face, flatRight), dot(face, flatFwd)))) + idle * glance(time);
        double cock = idle * tilt(time);
        // (A huff between: its head tossed up a moment, its crest bristling.)
        double huff = idle * bump(time - Math.floor(time / HUFF) * HUFF, 6.0, 16.0);
        double bow = 0.08 * run * bound + 0.1 * air - 0.12 * land - 0.08 * huff;
        double lunge = 0.0;
        if (a != null) {
            bow += a.bow();
            headTurn += a.headTurn();
            lunge = a.lunge();
        }
        p.turn(b.neck, OwlMesh.times(OwlMesh.about(Y, 0.3 * headTurn), OwlMesh.about(X, 0.3 * look - 0.5 * bow)));
        p.shift(b.neck, new double[] {0.0, -0.35 * lunge, -lunge});
        p.turn(b.skull, OwlMesh.times(OwlMesh.about(Y, 0.7 * headTurn), OwlMesh.times(OwlMesh.about(X, 0.7 * look - 0.5 * bow), OwlMesh.about(Z, cock))));
        // Now and then its jaw drops wide and its tongue rolls out, writhing, and back in (not in a move: its jaws are the move's).
        double c = time - Math.floor(time / YAWN) * YAWN;
        double quiet = a == null ? 1.0 : 0.0;
        double open = quiet * smooth(c / 14.0) * (1.0 - smooth((c - 74.0) / 14.0));
        double out = quiet * smooth((c - 12.0) / 22.0) * (1.0 - smooth((c - 58.0) / 16.0));
        double jaw = Math.max(Math.max(open, 0.25 * air), a == null ? 0.0 : a.jaw());
        p.turn(b.jaw, OwlMesh.about(X, -JAW * jaw));
        if (b.tongue >= 0 && b.segments.length > 0) {
            // (A long jointed tongue rolls out of it; the user's short one lies in its jaw, riding it.)
            p.scale(b.tongue, 0.06 + 0.94 * out);
            for (int i = 0; i < b.segments.length; i++) {
                p.turn(b.segments[i], OwlMesh.euler(0.08 * out * Math.sin(time * 0.31 - i * 0.7), 0.1 * out * Math.sin(time * 0.19 - i * 0.55), 0.0));
            }
        }
        // Its crest stirring; flared out and back (bristling, shivering) as far as the move flares it.
        double flare = flare(s, walk);
        OwlMesh m = p.mesh;
        for (int i = 0; i < b.crown.length; i++) {
            double side = Math.signum(m.pivot[b.crown[i]][0]);
            double shiver = 0.035 * flare * Math.sin(time * 2.7 + i * 1.3);
            p.turn(b.crown[i], OwlMesh.times(OwlMesh.about(Z, -side * 0.22 * flare), OwlMesh.about(X, 0.02 * Math.sin(time * 0.07 + i * 1.7) + 0.12 * flare + shiver)));
        }
        p.solve();
        for (int i = 0; i < 2; i++) {
            reach(p, b, s, i, walk, run, phase, air, idle, herPixel);
        }
        p.solve();
    }

    /** How far its crest is flared: as its move flares it, a little at its huffs, idle. */
    static double flare(Source s, double walk) {
        if (s.act() != null) {
            return Math.max(0.0, Math.min(1.0, s.act().crest()));
        }
        return 0.3 * (1.0 - walk) * bump(s.time() - Math.floor(s.time() / HUFF) * HUFF, 6.0, 16.0);
    }

    /** How far squashed down it is {@code landed} ticks after landing (1 the moment it lands, gone in {@link #LANDING} ticks). */
    static double landing(double landed) {
        return landed < 0.0 || landed >= LANDING ? 0.0 : 1.0 - smooth(landed / LANDING);
    }

    /** Idle, its look flicking from one place to the next (radians to its right): a quick turn, then held. */
    static double glance(double time) {
        double k = Math.floor(time / GLANCE);
        double f = smooth((time - k * GLANCE) / 3.0);
        return 0.12 * (noise(k) + (noise(k + 1.0) - noise(k)) * f);
    }

    /** Idle, now and then its head cocked aside (radians, rolled) and held a long moment, as an owl's; one way, then the other. */
    static double tilt(double time) {
        double k = Math.floor(time / TILT);
        return (k % 2.0 == 0.0 ? 1.0 : -1.0) * 0.3 * bump(time - k * TILT, 16.0, 60.0);
    }

    /** 0 up to {@code from}, risen to 1 over a few ticks, held, fallen back by {@code to}. */
    static double bump(double t, double from, double to) {
        return smooth((t - from) / 5.0) * (1.0 - smooth((t - to + 5.0) / 5.0));
    }

    /** A steady random -1..1 for {@code k}. */
    static double noise(double k) {
        double x = Math.sin(k * 12.9898 + 78.233) * 43758.5453;
        return (x - Math.floor(x)) * 2.0 - 1.0;
    }

    /** Idle (ticks): how often its look flicks about, it cocks its head, it huffs; how long (ticks) a landing squashes it. */
    static final double GLANCE = 26.0;
    static final double TILT = 170.0;
    static final double HUFF = 97.0;
    static final double LANDING = 9.0;

    /**
     * Arm {@code i} (0 its right, 1 its left) reaching for its hand's place: planted on the ground where it rests, out wide; as it walks,
     * hauling back along the ground under it and lifted and swung ahead again in turn with the other (at a sprint both together,
     * further); in the air, both reaching ahead to land on; lifted as her arm is moved off hanging (her moves carry it); its hand flat
     * on the ground (its fingers drooping as it lifts, flexing idle). Through one of its moves, its hand where the act has it, turned
     * along its forearm, its fingers closed as the act grips.
     */
    static void reach(OwlMesh.Pose p, Bones b, Source s, int i, double walk, double run, double phase, double air, double idle, double herPixel) {
        OwlMesh m = p.mesh;
        int up = b.upper[i];
        int fo = b.fore[i];
        int ha = b.hand[i];
        double side = i == 0 ? 1.0 : -1.0;
        double[] s0 = m.pivot[up];
        double[] e0 = m.pivot[fo];
        double[] w0 = m.pivot[ha];
        double upperLength = Math.sqrt(dot(sub(e0, s0), sub(e0, s0)));
        double foreLength = Math.sqrt(dot(sub(w0, e0), sub(w0, e0)));
        // Its crawl: planted and hauled back, then lifted and swung ahead, half a stride from the other (galloping, with it).
        double u = (phase + (i == 0 ? Math.PI * (1.0 - run) : 0.0)) / (Math.PI * 2.0);
        u -= Math.floor(u);
        double reach = REACH * walk * (1.0 + 0.5 * run);
        double ahead;
        double lift;
        if (u < PLANTED) {
            ahead = reach * (1.0 - 2.0 * u / PLANTED);
            lift = 0.0;
        } else {
            double t = (u - PLANTED) / (1.0 - PLANTED);
            ahead = reach * (-1.0 + 2.0 * smooth(t));
            lift = LIFT * walk * (1.0 + 0.3 * run) * Math.sin(Math.PI * t);
        }
        ahead = ahead + (REACH * 1.4 - ahead) * air;
        lift = lift + (9.0 - lift) * air;
        double[] forward = p.turned(b.model, new double[] {0.0, 0.0, -1.0});
        double[] wrist = add(p.apply(b.model, w0), add(mul(forward, ahead), new double[] {0.0, lift, 0.0}));
        // Her arm off her stance, carried over (not through its own moves: its hands are theirs).
        OwlActs.Act a = s.act();
        double[] herArm = add(mul(i == 0 ? s.rightUpper() : s.leftUpper(), 4.0), mul(i == 0 ? s.rightFore() : s.leftFore(), 6.0));
        double[] stance = {STANCE_ARM[0] * side, STANCE_ARM[1], STANCE_ARM[2]};
        double[] off = Rig.way(sub(herArm, stance));
        wrist = add(wrist, mul(off, herPixel * (a == null ? ARM_FOLLOW : 0.0)));
        wrist[1] = Math.max(wrist[1], w0[1]);
        double w = a == null ? 0.0 : Math.max(0.0, Math.min(1.0, i == 0 ? a.rightWeight() : a.leftWeight()));
        double[] to = a == null ? null : i == 0 ? a.right() : a.left();
        if (to == null) {
            w = 0.0;
        }
        if (w > 0.0) {
            wrist = OwlModel.lerp(wrist, p.apply(b.model, to), w);
            wrist[1] = Math.max(wrist[1], w0[1] - 2.0);
        }
        double[] shoulder = p.at[up];
        // (Bending the way its arm bends at rest, as the model holds it: its elbow out and down, half way to the ground.)
        double[] reachRest = normalize(sub(w0, s0));
        double[] bendRest = sub(sub(e0, s0), mul(reachRest, dot(sub(e0, s0), reachRest)));
        double[] pole = OwlMesh.apply(p.rot[m.parent[up]], normalize(bendRest));
        double[] elbow = knee(shoulder, wrist, upperLength, foreLength, pole);
        double[][] parent = p.rot[m.parent[up]];
        double[][] upper = OwlMesh.times(OwlMesh.arc(OwlMesh.apply(parent, sub(e0, s0)), sub(elbow, shoulder)), parent);
        double[][] fore = OwlMesh.times(OwlMesh.arc(OwlMesh.apply(upper, sub(w0, e0)), sub(wrist, elbow)), upper);
        double lifted = smooth(lift / 6.0);
        p.world(up, upper);
        p.world(fo, fore);
        p.world(ha, toward(OwlMesh.times(p.rot[b.model], OwlMesh.about(X, -0.3 * lifted)), fore, w));
        // Idle, its claws flexing (their points lifted off the ground and set down again), raking at it now and then.
        double flex = idle * (0.06 + 0.06 * Math.sin(s.time() * 0.05 + i * 2.1) + 0.3 * bump(s.time() - Math.floor(s.time() / RAKE) * RAKE - i * 40.0, 0.0, 14.0));
        double grip = a == null ? 0.0 : a.grip();
        double curl = (0.45 * lifted - flex) * (1.0 - w) + 1.35 * grip * w;
        for (int f : b.fingers[i]) {
            p.turn(f, OwlMesh.about(X, -curl));
        }
    }

    /** How often (ticks) its idle claws rake at the ground. */
    static final double RAKE = 140.0;

    /** The turn {@code t} (0..1) of the way from {@code a} to {@code b} (both turns of the model). */
    static double[][] toward(double[][] a, double[][] b, double t) {
        if (t <= 0.0) {
            return a;
        }
        if (t >= 1.0) {
            return b;
        }
        // a^T b, its angle and axis; a turned that share of it.
        double[][] d = new double[3][3];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                d[r][c] = a[0][r] * b[0][c] + a[1][r] * b[1][c] + a[2][r] * b[2][c];
            }
        }
        double cos = Math.max(-1.0, Math.min(1.0, (d[0][0] + d[1][1] + d[2][2] - 1.0) * 0.5));
        double angle = Math.acos(cos);
        double[] axis = {d[2][1] - d[1][2], d[0][2] - d[2][0], d[1][0] - d[0][1]};
        double l = Math.sqrt(dot(axis, axis));
        if (l < 1.0E-9) {
            return angle < 1.0 ? a : b; // (none to speak of, or half way round: no way to ease it)
        }
        return OwlMesh.times(a, OwlMesh.about(mul(axis, 1.0 / l), angle * t));
    }

    /** Two bones {@code a} and {@code b} long from {@code from} to {@code to}, the joint bending toward {@code pole} (stretched straight past their reach). */
    static double[] knee(double[] from, double[] to, double a, double b, double[] pole) {
        double[] d = sub(to, from);
        double l = Math.sqrt(dot(d, d));
        double[] dir = l < 1.0E-6 ? new double[] {0.0, -1.0, 0.0} : mul(d, 1.0 / l);
        double reach = Math.max(1.0E-6, Math.min(l, (a + b) * 0.999));
        double along = (a * a - b * b + reach * reach) / (2.0 * reach);
        double out = Math.sqrt(Math.max(0.0, a * a - along * along));
        double[] side = sub(pole, mul(dir, dot(pole, dir)));
        double sl = Math.sqrt(dot(side, side));
        side = sl < 1.0E-6 ? OwlModel.square(dir) : mul(side, 1.0 / sl);
        return add(from, add(mul(dir, along), mul(side, out)));
    }

    // ------------------------------------------------------------------ the mesh

    /** Its textures (tools/owl_smooth.py bakes them out of the model): all of it, and what of it burns (drawn again, adding light). */
    public enum Tex {
        OWL("one_eyed_owl", false, false), CREST("one_eyed_owl", false, true), OWL_GLOW("one_eyed_owl_glow", true, true),
        HALO("owl_halo", true, true);

        public final String file;
        /** Drawn adding light (the crest's red, the eye, the slashes). */
        public final boolean glows;
        /** Drawn at full brightness, whatever the light (its crest's crystals shine of themselves). */
        public final boolean bright;

        Tex(String file, boolean glows, boolean bright) {
            this.file = file;
            this.glows = glows;
            this.bright = bright;
        }
    }

    /**
     * Takes the creature's textured quads: their corners, the texture's place at each ({@code uv}: u, v for a, b, c, d), which way each
     * corner faces ({@code n}: four unit normals, out of it), each corner's tint.
     */
    public interface Skin {
        void quad(Tex tex, double[] a, double[] b, double[] c, double[] d, float[] uv, double[][] n, int ca, int cb, int cc, int cd);
    }

    /** Its body as posed (the kagune limbs are {@link #kagune}) to {@code skin}. */
    public static void mesh(UkakuModel.Sink sink, Skin skin, Rig r) {
        mesh(sink, skin, r, true);
    }

    /** The same, its head left out if not {@code head} (first person: seen out of it). */
    public static void mesh(UkakuModel.Sink sink, Skin skin, Rig r, boolean head) {
        draw(grounded(skin), r, head ? 2 : 1);
    }

    /** Its head alone, as drawn (for the tests). */
    public static void head(UkakuModel.Sink sink, Skin skin, Rig r) {
        draw(skin, r, 0);
    }

    /** Its quads posed to {@code skin}: {@code which} 0 its head's alone, 1 all but its head's, 2 all. */
    static void draw(Skin skin, Rig r, int which) {
        OwlMesh m = OwlMesh.get();
        Bones b = bones();
        OwlMesh.Pose p = r.pose();
        float[] c = m.corners;
        for (int i = 0; i < m.quads(); i++) {
            int bone = m.bone[i];
            if ((which == 0 && !b.head[bone]) || (which == 1 && b.head[bone])) {
                continue;
            }
            int o = i * 12;
            double[] qa = r.at(bone, new double[] {c[o], c[o + 1], c[o + 2]});
            double[] qb = r.at(bone, new double[] {c[o + 3], c[o + 4], c[o + 5]});
            double[] qc = r.at(bone, new double[] {c[o + 6], c[o + 7], c[o + 8]});
            double[] qd = r.at(bone, new double[] {c[o + 9], c[o + 10], c[o + 11]});
            float[] w = m.normals;
            double[][] nn = new double[4][];
            for (int k = 0; k < 4; k++) {
                nn[k] = Rig.way(p.turned(bone, new double[] {w[o + k * 3], w[o + k * 3 + 1], w[o + k * 3 + 2]}));
            }
            float[] uv = m.uv(i);
            skin.quad(b.crest[bone] ? Tex.CREST : Tex.OWL, qa, qb, qc, qd, uv, nn, -1, -1, -1, -1);
            if (m.burns[i]) {
                skin.quad(Tex.OWL_GLOW, qa, qb, qc, qd, uv, nn, -1, -1, -1, -1);
            }
        }
    }

    /**
     * Its glow, as the sheet's burns: a soft halo of red light facing {@code camera} (rig space) at the root of each crystal of its
     * crest and half way up the great ones (wider and redder as its crest flares), and round its eye (not if {@code eye} is false:
     * first person, seen out of it).
     */
    public static void halos(Skin skin, Rig r, double[] camera, boolean eye) {
        Bones b = bones();
        OwlMesh m = OwlMesh.get();
        int red = 0xFFFF3030;
        int deep = 0xFFC81424;
        double k = 1.0 + 0.55 * r.flare();
        for (int i = 0; i < b.crystal.length; i++) {
            int bone = b.crystal[i];
            double[] root = m.pivot[bone];
            double[] tip = b.point[i];
            if (b.great[i]) {
                halo(skin, r.at(bone, OwlModel.lerp(root, tip, 0.14)), 18.0 * k * r.pixel(), red, camera);
                halo(skin, r.at(bone, OwlModel.lerp(root, tip, 0.45)), 10.0 * k * r.pixel(), r.flare() > 0.5 ? red : deep, camera);
            } else {
                halo(skin, r.at(bone, OwlModel.lerp(root, tip, 0.25)), 8.0 * k * r.pixel(), deep, camera);
            }
        }
        if (eye && b.iris >= 0) {
            halo(skin, r.at(b.iris, m.pivot[b.iris]), 2.6 * r.pixel(), red, camera);
        }
    }

    /** One halo: a square {@code radius} each way about {@code at}, facing {@code camera}. */
    static void halo(Skin skin, double[] at, double radius, int colour, double[] camera) {
        double[] look = sub(camera, at);
        double length = Math.sqrt(dot(look, look));
        if (length < 1.0E-6) {
            return;
        }
        double[] n = mul(look, 1.0 / length);
        double[] u = normalize(OwlModel.cross(Math.abs(n[1]) < 0.95 ? Y : X, n));
        double[] v = OwlModel.cross(n, u);
        u = mul(u, radius);
        v = mul(v, radius);
        double[][] nn = {n, n, n, n};
        skin.quad(Tex.HALO, sub(sub(at, u), v), sub(add(at, u), v), add(add(at, u), v), add(sub(at, u), v), HALO_UV, nn, colour, colour, colour, colour);
    }

    static final float[] HALO_UV = {0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F};

    /** Where its one eye is (rig space): first person sees out of it. */
    public static double[] eye(Rig r) {
        Bones b = bones();
        return b.iris >= 0 ? r.at(b.iris, OwlMesh.get().pivot[b.iris]) : r.head().o();
    }

    /** The four kagune limbs ({@code look}, swung off {@link Rig#kagune}), each to the creature's size about its own root (so it stays attached). */
    public static void kagune(UkakuModel.Sink sink, Rig r, OwlModel.Look look) {
        if (look.grown() <= 0.01) {
            return;
        }
        OwlModel.Arm[] arms = {look.rightInner(), look.leftInner(), look.right(), look.left()};
        for (int i = 0; i < 4; i++) {
            if (arms[i] != null) {
                OwlModel.limb(scaled(sink, arms[i].shoulder(), r.size()), look, arms[i], i % 2 == 0 ? 1 : -1, i < 2, KAGUNE_DETAIL);
            }
        }
    }

    /** A point of a limb ({@code arm}, as swung) as it is drawn to the creature's size. */
    public static double[] limbAt(Rig r, OwlModel.Arm arm, double[] p) {
        return add(arm.shoulder(), mul(sub(p, arm.shoulder()), r.size()));
    }

    /**
     * The place in its back (just under the hump of it) for a place on hers ({@code px}, her torso's pixels: x to her left, y down from
     * her neck, z back): the spikes' and the lances' roots.
     */
    public static double[] backAt(Rig r, double[] px) {
        Bones b = bones();
        double x = Math.max(-0.9, Math.min(0.9, px[0] / 4.0)) * 22.0;
        double y = 66.0 - Math.max(0.0, Math.min(12.0, px[1])) * 2.0;
        double over = 1.0 - Math.pow(x / 26.0, 2.0) - Math.pow((y - 58.0) / 18.0, 2.0);
        double z = 9.0 + 16.0 * Math.sqrt(Math.max(0.0, over)) - 1.5;
        return r.at(b.torso >= 0 ? b.torso : 0, new double[] {x, y, z});
    }

    /** Which way (unit) its back faces there. */
    public static double[] backWay(Rig r, double[] px) {
        Bones b = bones();
        double x = Math.max(-0.9, Math.min(0.9, px[0] / 4.0));
        return normalize(Rig.way(r.pose().turned(b.torso >= 0 ? b.torso : 0, normalize(new double[] {x * 0.6, 0.45, 1.0}))));
    }

    /** {@code sink} with everything put to it scaled {@code k} about {@code o}. */
    static UkakuModel.Sink scaled(UkakuModel.Sink sink, double[] o, double k) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(add(o, mul(sub(a, o), k)), add(o, mul(sub(b, o), k)), add(o, mul(sub(c, o), k)),
            add(o, mul(sub(d, o), k)), n, ca, cb, cc, cd);
    }

    /** {@code skin} with nothing put to it lower than the ground. */
    static Skin grounded(Skin skin) {
        return (t, a, b, c, d, uv, n, ca, cb, cc, cd) -> skin.quad(t, OwlModel.ground(a), OwlModel.ground(b), OwlModel.ground(c), OwlModel.ground(d), uv, n,
            ca, cb, cc, cd);
    }

    /** How far {@code q} is from the bone from {@code a} to {@code b}. */
    public static double distance(double[] q, double[] a, double[] b) {
        double[] d = sub(b, a);
        double t = Math.max(0.0, Math.min(1.0, dot(sub(q, a), d) / Math.max(1.0E-9, dot(d, d))));
        double[] off = sub(q, add(a, mul(d, t)));
        return Math.sqrt(dot(off, off));
    }
}
