package com.hex.kagune.control.client.animation;

/**
 * Where the parts of a posed player model are (6.1.0; pure, tested): the forward kinematics PlayerAnimator 1.0.2 and bendy-lib
 * 4.0.0 apply to a {@link RigPose} (vanilla PlayerModel pivots and cuboids, the part's position then its rotation Rz Ry Rx as
 * {@code ModelPart.translateAndRotate} does, the elbow or knee bend as bendy-lib bends a cuboid), the same math the off-game
 * previewer draws with. Two uses: a pose can be checked for limbs passing through the body ({@link #clipping}), and anything
 * drawn on the body (Pale's flesh out of her upper back, round her leg) is placed exactly where the posed part is.
 *
 * <p>Model space: pixels, x to the player's left, y down from the neck, z backward (PlayerModel's own). Rig space: blocks, x to
 * the player's right, y up from the feet, z forward ({@link #rig}).
 */
public final class BodyFrame {
    /** Vanilla's default arm sway at rest (AnimationUtils.bobArms), which every frame starts from. */
    static final float BOB = 0.05F;
    /** The player renderer's scale. */
    public static final double SCALE = 0.9375;

    /** A model part as posed: its pivot (pixels), rotation (radians, applied Rz Ry Rx), and bend (axis angle, amount). */
    public record Part(double px, double py, double pz, double rx, double ry, double rz, double bendAxis, double bend) {
    }

    /** A part's box in its own pixels and which way bendy-lib bends it (+1 from the top: arms and legs; 0: never). */
    record Box(double[] min, double[] max, int bendDir) {
    }

    static final Box HEAD = new Box(new double[] {-4, -8, -4}, new double[] {4, 0, 4}, 0);
    static final Box TORSO = new Box(new double[] {-4, 0, -2}, new double[] {4, 12, 2}, 0);
    static final Box RIGHT_ARM = new Box(new double[] {-3, -2, -2}, new double[] {1, 10, 2}, 1);
    static final Box LEFT_ARM = new Box(new double[] {-1, -2, -2}, new double[] {3, 10, 2}, 1);
    static final Box LEG = new Box(new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1);
    /** The elbow: the middle of an arm's box (pixels down from the shoulder pivot), where bendy-lib bends it. */
    static final double ELBOW = 4.0;
    /** How much deeper (pixels) the upper arm may sink into the torso than the forearm, at the shoulder joint. */
    static final double JOINT_SLACK = 0.75;

    private BodyFrame() {
    }

    /** The posed part named as PlayerAnimator names it ("head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"). */
    public static Part part(RigPose pose, String name) {
        double px = 0;
        double py = 0;
        double pz = 0;
        double rz = 0;
        switch (name) {
            case "rightArm" -> { px = -5; py = 2; rz = BOB; }
            case "leftArm" -> { px = 5; py = 2; rz = -BOB; }
            case "rightLeg" -> { px = -1.9; py = 12; pz = 0.1; }
            case "leftLeg" -> { px = 1.9; py = 12; pz = 0.1; }
            default -> {
            }
        }
        float[] pos = pose.transform(name, RigPose.POSITION, (float)px, (float)py, (float)pz);
        float[] rot = pose.transform(name, RigPose.ROTATION, 0.0F, 0.0F, (float)rz);
        float[] bend = pose.transform(name, RigPose.BEND, 0.0F, 0.0F, 0.0F);
        return new Part(pos[0], pos[1], pos[2], rot[0], rot[1], rot[2], bend[0], bend[1]);
    }

    static Box box(String name) {
        return switch (name) {
            case "head" -> HEAD;
            case "torso" -> TORSO;
            case "rightArm" -> RIGHT_ARM;
            case "leftArm" -> LEFT_ARM;
            default -> LEG;
        };
    }

    /** A point of a part (its own pixels, before the bend) in model space, bent and posed as the game draws it. */
    public static double[] model(RigPose pose, String name, double[] local) {
        Part part = part(pose, name);
        return place(bend(local, box(name), part.bendAxis(), part.bend()), part);
    }

    /**
     * 7.0.0: a point of an arm or leg (its own pixels) bent as bendy-lib bends that part in {@code pose}, not placed: what a layer
     * draws on the part (the kakuja's hide, its claws) folds at the elbow and knee with it.
     */
    public static double[] bent(RigPose pose, String name, double[] local) {
        Part part = part(pose, name);
        return bend(local, box(name), part.bendAxis(), part.bend());
    }

    /**
     * 7.0.0: where a rig-space point fixed to {@code name} as he stands at rest is carried by {@code pose} (the part's own turn and
     * place, then the whole body's turn, drop and lunge): what grows out of his back stays in his back as he hunches and lunges.
     */
    public static double[] carried(RigPose pose, String name, double[] rig) {
        RigPose rest = new RigPose();
        double[] local = local(unworld(rest, rig), part(rest, name));
        return world(pose, model(pose, name, local));
    }

    /**
     * 1.0.24: {@link #carried} for {@code name} as one rigid transform, worked out once for many points: {a00, a01, a02, a10, a11, a12,
     * a20, a21, a22, b0, b1, b2}, a rig-space point p carried to A p + b (every step of the carry is rigid, so it is exactly that).
     */
    public static double[] carry(RigPose pose, String name) {
        double[] b = carried(pose, name, new double[] {0, 0, 0});
        double[] ex = carried(pose, name, new double[] {1, 0, 0});
        double[] ey = carried(pose, name, new double[] {0, 1, 0});
        double[] ez = carried(pose, name, new double[] {0, 0, 1});
        return new double[] {ex[0] - b[0], ey[0] - b[0], ez[0] - b[0], ex[1] - b[1], ey[1] - b[1], ez[1] - b[1], ex[2] - b[2], ey[2] - b[2],
            ez[2] - b[2], b[0], b[1], b[2]};
    }

    /** {@code ModelPart.translateAndRotate}: pivot + Rz Ry Rx q. */
    static double[] place(double[] q, Part part) {
        double x = q[0];
        double y = q[1];
        double z = q[2];
        double y1 = y * Math.cos(part.rx()) - z * Math.sin(part.rx());
        double z1 = y * Math.sin(part.rx()) + z * Math.cos(part.rx());
        double x2 = x * Math.cos(part.ry()) + z1 * Math.sin(part.ry());
        double z2 = -x * Math.sin(part.ry()) + z1 * Math.cos(part.ry());
        double x3 = x2 * Math.cos(part.rz()) - y1 * Math.sin(part.rz());
        double y3 = x2 * Math.sin(part.rz()) + y1 * Math.cos(part.rz());
        return new double[] {part.px() + x3, part.py() + y3, part.pz() + z2};
    }

    /** The inverse of {@link #place}: a model-space point in the part's own pixels (unbent). */
    static double[] local(double[] q, Part part) {
        double x = q[0] - part.px();
        double y = q[1] - part.py();
        double z = q[2] - part.pz();
        // Undo Rz, then Ry, then Rx.
        double x1 = x * Math.cos(-part.rz()) - y * Math.sin(-part.rz());
        double y1 = x * Math.sin(-part.rz()) + y * Math.cos(-part.rz());
        double x2 = x1 * Math.cos(-part.ry()) + z * Math.sin(-part.ry());
        double z2 = -x1 * Math.sin(-part.ry()) + z * Math.cos(-part.ry());
        double y3 = y1 * Math.cos(-part.rx()) - z2 * Math.sin(-part.rx());
        double z3 = y1 * Math.sin(-part.rx()) + z2 * Math.cos(-part.rx());
        return new double[] {x2, y3, z3};
    }

    /** bendy-lib's {@code IBendable.applyBend} on one point of an arm or leg (pixels), bending from the top. */
    static double[] bend(double[] q, Box box, double axisAngle, double amount) {
        if (box.bendDir() == 0 || Math.abs(amount) < 1.0E-4) {
            return q.clone();
        }
        double[] dir = {0, 1, 0};
        double[] axis = {Math.cos(axisAngle), 0, Math.sin(axisAngle)};
        double[] centre = {(box.min()[0] + box.max()[0]) / 2, (box.min()[1] + box.max()[1]) / 2, (box.min()[2] + box.max()[2]) / 2};
        double distA = dot(dir, q) - dot(dir, box.max());
        double distB = dot(dir, q) - dot(dir, box.min());
        double halfSize = (dot(dir, box.max()) - dot(dir, box.min())) / 2;
        double[] normal = norm(cross(dir, axis));
        double fromBend = -(dot(normal, q) - dot(normal, centre));
        double s = Math.tan(amount / 2) * fromBend;
        double[] p = q.clone();
        if (Math.abs(distA) < Math.abs(distB)) {
            double k = -distA / halfSize * s;
            p = new double[] {p[0] + dir[0] * k, p[1] + dir[1] * k, p[2] + dir[2] * k};
            double[] d = {p[0] - centre[0], p[1] - centre[1], p[2] - centre[2]};
            double[] r = rotate(d, axis, amount);
            p = new double[] {centre[0] + r[0], centre[1] + r[1], centre[2] + r[2]};
        } else {
            double k = -distB / halfSize * s;
            p = new double[] {p[0] + dir[0] * k, p[1] + dir[1] * k, p[2] + dir[2] * k};
        }
        return p;
    }

    /**
     * A model-space point (pixels) in rig space (blocks) with the body lowered {@code drop} pixels, as the renderer places it
     * (PlayerAnimator moves the whole model by the drop before the player's scale, so the drop is not scaled).
     */
    public static double[] rig(double[] model, double drop) {
        double k = SCALE / 16.0;
        return new double[] {-model[0] * k, (24.016 - model[1]) * k - drop / 16.0, -model[2] * k};
    }

    /** Where PlayerAnimator turns the whole model about (blocks above the feet). */
    public static final double TURN_PIVOT = 0.7;

    /**
     * A model-space point (pixels) in rig space (blocks) as the game draws the posed body: also turned with the whole body's
     * {@link RigPose#flip}, {@link RigPose#spin} and {@link RigPose#roll} about {@link #TURN_PIVOT}, as PlayerAnimator's
     * PlayerRendererMixin turns it (ZP, YP, XP in its own frame, whose z runs backward: here x and y turn the other way), then lowered
     * by the drop and carried forward by the lunge.
     */
    public static double[] world(RigPose pose, double[] model) {
        double[] q = rig(model, 0.0);
        q[1] -= TURN_PIVOT;
        q = rotate(q, new double[] {1, 0, 0}, -pose.flip);
        q = rotate(q, new double[] {0, 1, 0}, -pose.spin);
        q = rotate(q, new double[] {0, 0, 1}, pose.roll);
        q[1] += TURN_PIVOT - pose.drop / 16.0;
        q[2] += pose.lunge / 16.0;
        return q;
    }

    /** The inverse of {@link #world}: a rig-space point (blocks) in model space (pixels), the body's turn, drop and lunge undone. */
    public static double[] unworld(RigPose pose, double[] rig) {
        double[] q = rig.clone();
        q[2] -= pose.lunge / 16.0;
        q[1] -= TURN_PIVOT - pose.drop / 16.0;
        q = rotate(q, new double[] {0, 0, 1}, -pose.roll);
        q = rotate(q, new double[] {0, 1, 0}, pose.spin);
        q = rotate(q, new double[] {1, 0, 0}, pose.flip);
        q[1] += TURN_PIVOT;
        double k = SCALE / 16.0;
        return new double[] {-q[0] / k, 24.016 - q[1] / k, -q[2] / k};
    }

    /**
     * How deep (pixels) a ball of {@code radius} blocks at the rig-space point {@code rig} sinks into her torso, head or either arm
     * (the arms unbent, near enough for clearance); 0 or less when it is clear of them all.
     */
    public static double sphereDepth(RigPose pose, double[] rig, double radius) {
        return new Solid(pose).depth(rig, radius);
    }

    /**
     * Her torso, head and arms as posed, for many questions about one pose (what is drawn round her body asks a thousand a frame):
     * how deep a ball sinks into them, and where her body's middle line is.
     */
    public static final class Solid {
        private static final String[] NAMES = {"torso", "head", "rightArm", "leftArm"};
        private static final Box[] BOXES = {TORSO, HEAD, RIGHT_ARM, LEFT_ARM};
        private final RigPose pose;
        /** Per part: its pivot, then cos and sin of -rz, -ry, -rx (as {@link #local} undoes them). */
        private final double[][] parts = new double[NAMES.length][];
        private final Part[] posed = new Part[NAMES.length];
        /** The body's turn as {@link #unworld} undoes it: cos and sin of -roll, spin, flip. */
        private final double[] turn;
        /** Her middle line (rig space): hips, neck, the top of her head. */
        private final double[][] axis;
        /** 6.2.0: the part left out (-1: none): her right arm, while the Mutated Arm is in its place. */
        private final int skip;

        public Solid(RigPose pose) {
            this(pose, true);
        }

        /** Her body, with her right arm or without it ({@code rightArm} false: the Mutated Arm bursts out of it, so it may lie in it). */
        public Solid(RigPose pose, boolean rightArm) {
            this.pose = pose;
            this.skip = rightArm ? -1 : 2;
            for (int i = 0; i < NAMES.length; i++) {
                Part part = BodyFrame.part(pose, NAMES[i]);
                this.posed[i] = part;
                this.parts[i] = new double[] {part.px(), part.py(), part.pz(), Math.cos(-part.rz()), Math.sin(-part.rz()), Math.cos(-part.ry()),
                    Math.sin(-part.ry()), Math.cos(-part.rx()), Math.sin(-part.rx())};
            }
            this.turn = new double[] {Math.cos(-pose.roll), Math.sin(-pose.roll), Math.cos(pose.spin), Math.sin(pose.spin), Math.cos(pose.flip),
                Math.sin(pose.flip)};
            this.axis = new double[][] {world(pose, model(pose, "torso", new double[] {0, 12, 0})), world(pose, model(pose, "torso", new double[] {0, 0, 0})),
                world(pose, model(pose, "head", new double[] {0, -8, 0}))};
        }

        /** As {@link BodyFrame#sphereDepth}: torso, head and arms. */
        public double depth(double[] rig, double radius) {
            return this.depth(rig, radius, 0);
        }

        /** As {@link #depth}, her head and arms alone. */
        public double beyondTorso(double[] rig, double radius) {
            return this.depth(rig, radius, 1);
        }

        /** As {@link #depth}, her torso alone. */
        public double torso(double[] rig, double radius) {
            return this.part(rig, radius, 0);
        }

        /** As {@link #depth}, one part alone: 0 torso, 1 head, 2 right arm, 3 left arm. */
        public double part(double[] rig, double radius, int index) {
            return outside(this.local(this.unturned(rig), index), BOXES[index], radius * 16.0 / SCALE);
        }

        private double depth(double[] rig, double radius, int from) {
            double[] model = this.unturned(rig);
            double r = radius * 16.0 / SCALE;
            double deepest = -Double.MAX_VALUE;
            for (int i = from; i < NAMES.length; i++) {
                if (i != this.skip) {
                    deepest = Math.max(deepest, outside(this.local(model, i), BOXES[i], r));
                }
            }
            return deepest;
        }

        /** {@link #unworld}, its turn worked out once. */
        private double[] unturned(double[] rig) {
            double[] t = this.turn;
            double x = rig[0];
            double y = rig[1] - (TURN_PIVOT - this.pose.drop / 16.0);
            double z = rig[2] - this.pose.lunge / 16.0;
            double x1 = x * t[0] - y * t[1];
            double y1 = x * t[1] + y * t[0];
            double x2 = x1 * t[2] + z * t[3];
            double z2 = z * t[2] - x1 * t[3];
            double y3 = y1 * t[4] - z2 * t[5];
            double z3 = z2 * t[4] + y1 * t[5];
            double k = SCALE / 16.0;
            return new double[] {-x2 / k, 24.016 - (y3 + TURN_PIVOT) / k, -z3 / k};
        }

        /** {@link BodyFrame#local}, the part's turn worked out once. */
        private double[] local(double[] q, int index) {
            double[] p = this.parts[index];
            double x = q[0] - p[0];
            double y = q[1] - p[1];
            double z = q[2] - p[2];
            double x1 = x * p[3] - y * p[4];
            double y1 = x * p[4] + y * p[3];
            double x2 = x1 * p[5] + z * p[6];
            double z2 = -x1 * p[6] + z * p[5];
            return new double[] {x2, y1 * p[7] - z2 * p[8], y1 * p[8] + z2 * p[7]};
        }

        /**
         * {@code rig} moved the shortest way out of her torso, head and arms to {@code radius} blocks clear of each (itself when it
         * is clear already).
         */
        public double[] pushOut(double[] rig, double radius) {
            double[] p = rig;
            double r = radius * 16.0 / SCALE;
            for (int round = 0; round < 4; round++) {
                double[] model = this.unturned(p);
                int worst = -1;
                double deepest = 0.0;
                double[] at = null;
                for (int i = 0; i < NAMES.length; i++) {
                    if (i == this.skip) {
                        continue;
                    }
                    double[] local = this.local(model, i);
                    double depth = outside(local, BOXES[i], r);
                    if (depth > deepest) {
                        deepest = depth;
                        worst = i;
                        at = local;
                    }
                }
                if (worst < 0) {
                    return p;
                }
                p = world(this.pose, place(clearOf(at, BOXES[worst], r + 0.05), this.posed[worst]));
            }
            return p;
        }

        /** Her middle line: hips, neck, head top (rig space). */
        public double[][] axis() {
            return this.axis;
        }

        /** The point of her middle line nearest {@code rig}. */
        public double[] nearest(double[] rig) {
            return nearestOn(this.axis, rig);
        }
    }

    /** A point in a box's own pixels moved the shortest way to {@code r} pixels clear of it. */
    static double[] clearOf(double[] q, Box box, double r) {
        double[] nearest = new double[3];
        double gap = 0.0;
        for (int k = 0; k < 3; k++) {
            nearest[k] = Math.max(box.min()[k], Math.min(box.max()[k], q[k]));
            gap += (q[k] - nearest[k]) * (q[k] - nearest[k]);
        }
        gap = Math.sqrt(gap);
        if (gap > 1.0E-9) {
            return new double[] {nearest[0] + (q[0] - nearest[0]) * r / gap, nearest[1] + (q[1] - nearest[1]) * r / gap,
                nearest[2] + (q[2] - nearest[2]) * r / gap};
        }
        // Inside: out through the nearest face.
        int axis = 0;
        boolean low = true;
        double best = Double.MAX_VALUE;
        for (int k = 0; k < 3; k++) {
            if (q[k] - box.min()[k] < best) {
                best = q[k] - box.min()[k];
                axis = k;
                low = true;
            }
            if (box.max()[k] - q[k] < best) {
                best = box.max()[k] - q[k];
                axis = k;
                low = false;
            }
        }
        double[] out = q.clone();
        out[axis] = low ? box.min()[axis] - r : box.max()[axis] + r;
        return out;
    }

    /** The point of the polyline {@code line} nearest {@code p}. */
    public static double[] nearestOn(double[][] line, double[] p) {
        double[] best = line[0];
        double bestGap = Double.MAX_VALUE;
        for (int i = 0; i + 1 < line.length; i++) {
            double[] a = line[i];
            double[] ab = {line[i + 1][0] - a[0], line[i + 1][1] - a[1], line[i + 1][2] - a[2]};
            double[] ap = {p[0] - a[0], p[1] - a[1], p[2] - a[2]};
            double t = Math.max(0.0, Math.min(1.0, dot(ap, ab) / Math.max(1.0E-12, dot(ab, ab))));
            double[] q = {a[0] + ab[0] * t, a[1] + ab[1] * t, a[2] + ab[2] * t};
            double gap = (p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]) + (p[2] - q[2]) * (p[2] - q[2]);
            if (gap < bestGap) {
                bestGap = gap;
                best = q;
            }
        }
        return best;
    }

    /** How far a ball of radius {@code r} round {@code q} reaches into {@code box} (pixels; 0 or less: clear). */
    static double outside(double[] q, Box box, double r) {
        double dx = Math.max(box.min()[0] - q[0], Math.max(0.0, q[0] - box.max()[0]));
        double dy = Math.max(box.min()[1] - q[1], Math.max(0.0, q[1] - box.max()[1]));
        double dz = Math.max(box.min()[2] - q[2], Math.max(0.0, q[2] - box.max()[2]));
        double gap = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (gap > 0.0) {
            return r - gap;
        }
        return r + depth(q, box);
    }

    /** One face of the posed body: four corners (rig space, blocks, counter-clockwise seen from outside) and its outward normal. */
    public interface QuadSink {
        void quad(double[] a, double[] b, double[] c, double[] d, double[] normal);
    }

    static final String[] PARTS = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};

    /**
     * The whole posed body's surface as the game draws it ({@link #world}: turned, lowered and carried forward with the body), face by
     * face: each part's box, the arms and legs in two halves (above and below the elbow or knee) so a bend shows. For whatever is
     * drawn as her body without being her model: Pale's afterimages.
     */
    public static void surface(RigPose pose, QuadSink sink) {
        for (String name : PARTS) {
            Box box = box(name);
            Part part = part(pose, name);
            double[] cuts = box.bendDir() == 0 ? new double[] {box.min()[1], box.max()[1]}
                : new double[] {box.min()[1], (box.min()[1] + box.max()[1]) / 2.0, box.max()[1]};
            for (int slab = 0; slab + 1 < cuts.length; slab++) {
                double[][] c = new double[8][];
                for (int i = 0; i < 8; i++) {
                    double[] local = {(i & 1) == 0 ? box.min()[0] : box.max()[0], (i & 2) == 0 ? cuts[slab] : cuts[slab + 1],
                        (i & 4) == 0 ? box.min()[2] : box.max()[2]};
                    c[i] = world(pose, place(bend(local, box, part.bendAxis(), part.bend()), part));
                }
                boolean top = slab == 0;
                boolean bottom = slab + 2 == cuts.length;
                // Faces of the box by corner (bit 0: x, 1: y, 2: z); model y runs down, so "top" is y min.
                face(sink, c, 0, 4, 6, 2);
                face(sink, c, 1, 3, 7, 5);
                face(sink, c, 0, 1, 5, 4);
                face(sink, c, 2, 6, 7, 3);
                if (top) {
                    face(sink, c, 0, 2, 3, 1);
                }
                if (bottom) {
                    face(sink, c, 4, 5, 7, 6);
                }
            }
        }
    }

    private static void face(QuadSink sink, double[][] c, int a, int b, int d, int e) {
        double[] centre = {0, 0, 0};
        for (double[] corner : c) {
            centre[0] += corner[0] / 8.0;
            centre[1] += corner[1] / 8.0;
            centre[2] += corner[2] / 8.0;
        }
        double[] n = norm(cross(new double[] {c[b][0] - c[a][0], c[b][1] - c[a][1], c[b][2] - c[a][2]},
            new double[] {c[e][0] - c[a][0], c[e][1] - c[a][1], c[e][2] - c[a][2]}));
        double[] mid = {(c[a][0] + c[d][0]) / 2.0 - centre[0], (c[a][1] + c[d][1]) / 2.0 - centre[1], (c[a][2] + c[d][2]) / 2.0 - centre[2]};
        if (dot(n, mid) < 0.0) {
            // Wound the other way: flip it so it faces out.
            sink.quad(c[a], c[e], c[d], c[b], new double[] {-n[0], -n[1], -n[2]});
        } else {
            sink.quad(c[a], c[b], c[d], c[e], n);
        }
    }

    /**
     * How deep (pixels) any arm goes into the torso or the head in {@code pose}: 0 when the arms stay outside, give or take
     * {@code tolerance} pixels (an arm's top turning at the shoulder always grazes the torso, as vanilla's own swing and raise do,
     * so the shoulder joint, the box's top three pixels, is not counted, and the upper arm below it may sink {@link #JOINT_SLACK}
     * more: raised and turned in, its inner corner meets the torso's inside the shoulder; the forearm and fist get no slack).
     * Samples each arm's box, bent and posed, on a half-pixel grid.
     */
    public static double clipping(RigPose pose, double tolerance) {
        return worstClip(pose, tolerance)[0];
    }

    /** {@link #clipping}, the arms sampled from {@code below} pixels under the top of their box (the forearm alone: 6). */
    public static double clipping(RigPose pose, double tolerance, double below) {
        return worstClip(pose, tolerance, below)[0];
    }

    /**
     * The deepest point of an arm inside the torso or the head: {depth (pixels, past {@code tolerance}; 0: none), which arm (0
     * right, 1 left), the point in the arm's own pixels (x, y, z), what it is in (0 torso, 1 head)}.
     */
    public static double[] worstClip(RigPose pose, double tolerance) {
        return worstClip(pose, tolerance, 3.0);
    }

    static double[] worstClip(RigPose pose, double tolerance, double below) {
        Part torso = part(pose, "torso");
        Part head = part(pose, "head");
        double[] worst = {0.0, -1, 0, 0, 0, -1};
        String[] arms = {"rightArm", "leftArm"};
        for (int index = 0; index < arms.length; index++) {
            String arm = arms[index];
            Box box = box(arm);
            Part part = part(pose, arm);
            // From a pixel below the shoulder pivot down: above that is the joint itself, whose corners turn against the
            // torso's top whenever an arm is raised (vanilla's own arm raise does the same).
            for (double y = box.min()[1] + below; y <= box.max()[1]; y += 0.5) {
                for (double x = box.min()[0]; x <= box.max()[0]; x += 0.5) {
                    for (double z = box.min()[2]; z <= box.max()[2]; z += 0.5) {
                        boolean surface = x == box.min()[0] || x == box.max()[0] || z == box.min()[2] || z == box.max()[2] || y == box.max()[1];
                        if (!surface) {
                            continue;
                        }
                        double[] q = place(bend(new double[] {x, y, z}, box, part.bendAxis(), part.bend()), part);
                        // The upper arm by the shoulder may sink a little more: its inner corner turns against the torso's corner
                        // whenever the arm is raised and turned in, inside the shoulder, out of sight. Below the elbow, never.
                        double allowed = tolerance + (y < ELBOW ? JOINT_SLACK : 0.0);
                        double inTorso = depth(local(q, torso), TORSO) - allowed;
                        double inHead = depth(local(q, head), HEAD) - allowed;
                        double deeper = Math.max(inTorso, inHead);
                        if (deeper > worst[0]) {
                            worst = new double[] {deeper, index, x, y, z, inHead > inTorso ? 1 : 0};
                        }
                    }
                }
            }
        }
        return worst;
    }

    /**
     * 6.2.0: {@link #clipping} with her body bulked up as {@code pose.bulk} has it ({@link RigPose#bulk}, as the game draws it): each
     * arm drawn thicker about its own middle line (bent first, then scaled, as the model's own scale does it), the broader, deeper
     * chest over the top of her torso as well as the torso and head (pixels past {@code tolerance}; 0: none).
     */
    public static double bulkClipping(RigPose pose, double tolerance) {
        double b = Float.isFinite(pose.bulk) ? Math.max(0.0, Math.min(1.0, pose.bulk)) : 0.0;
        double k = 1.0 + RigPose.BULK_ARMS * b;
        Box chest = new Box(new double[] {-4.0 * (1.0 + RigPose.BULK_CHEST * b), 0.0, -2.0 * (1.0 + RigPose.BULK_CHEST_DEPTH * b)},
            new double[] {4.0 * (1.0 + RigPose.BULK_CHEST * b), 6.0, 2.0 * (1.0 + RigPose.BULK_CHEST_DEPTH * b)}, 0);
        Part torso = part(pose, "torso");
        Part head = part(pose, "head");
        double worst = 0.0;
        for (String arm : new String[] {"rightArm", "leftArm"}) {
            Box box = box(arm);
            Part part = part(pose, arm);
            for (double y = box.min()[1] + 3.0; y <= box.max()[1]; y += 0.5) {
                for (double x = box.min()[0]; x <= box.max()[0]; x += 0.5) {
                    for (double z = box.min()[2]; z <= box.max()[2]; z += 0.5) {
                        boolean surface = x == box.min()[0] || x == box.max()[0] || z == box.min()[2] || z == box.max()[2] || y == box.max()[1];
                        if (!surface) {
                            continue;
                        }
                        double[] bent = bend(new double[] {x, y, z}, box, part.bendAxis(), part.bend());
                        double[] q = place(new double[] {bent[0] * k, bent[1], bent[2] * k}, part);
                        double allowed = tolerance + (y < ELBOW ? JOINT_SLACK : 0.0);
                        double[] inTorso = local(q, torso);
                        double deeper = Math.max(Math.max(depth(inTorso, TORSO), depth(inTorso, chest)), depth(local(q, head), HEAD)) - allowed;
                        worst = Math.max(worst, deeper);
                    }
                }
            }
        }
        return worst;
    }

    /** How far inside {@code box} the point is (pixels; 0 or less when outside). */
    static double depth(double[] q, Box box) {
        double inside = Double.MAX_VALUE;
        for (int k = 0; k < 3; k++) {
            inside = Math.min(inside, Math.min(q[k] - box.min()[k], box.max()[k] - q[k]));
        }
        return inside;
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] norm(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0, 0, 0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }

    /** Rodrigues: {@code v} turned by {@code angle} about the unit {@code axis}. */
    static double[] rotate(double[] v, double[] axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double[] k = norm(axis);
        double[] kxv = cross(k, v);
        double kv = dot(k, v);
        return new double[] {v[0] * c + kxv[0] * s + k[0] * kv * (1 - c), v[1] * c + kxv[1] * s + k[1] * kv * (1 - c),
            v[2] * c + kxv[2] * s + k[2] * kv * (1 - c)};
    }
}
