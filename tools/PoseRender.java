import com.hex.kagune.control.client.animation.RigPose;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Off-game renderer of the player model exactly as PlayerAnimator 1.0.2 and bendy-lib 4.0.0 pose it in 1.20.1, so a pose
 * can be judged before it ships. Faithful to the game in the parts that matter for posing:
 * <ul>
 *   <li>vanilla PlayerModel cuboids and pivots (classic 4-pixel arms), PlayerAnimator's default pivots, vanilla's idle
 *       arm sway offset;</li>
 *   <li>{@code AnimationApplier.updatePart}: position, then rotation (ZYX like {@code ModelPart.translateAndRotate}),
 *       then the cuboid bend; the torso bends by torso + body bend;</li>
 *   <li>bendy-lib's {@code IBendable.applyBend} per vertex, including the bulge term and its sign quirks (arms and legs
 *       bend from the UP side, the torso from the DOWN side), on cuboids cut into one-pixel strips as bendy-lib does;</li>
 *   <li>{@code BipedEntityModelMixin}: head and arms ("upper parts") rotated by the body bend about the torso's middle;</li>
 *   <li>{@code PlayerRendererMixin}: the body position offset.</li>
 * </ul>
 * Not modelled: the second skin layer, held items, lighting from the world. The skin is a stand-in in the colours of
 * the user's (black suit, white shirt and tie, black hair, kakugan).
 */
public final class PoseRender {
    /** Vanilla's default arm sway (AnimationUtils.bobArms at rest) that every frame starts from. */
    static final float BOB = 0.05F;

    record Tri(double[][] v, int rgb, double[] n) {
    }

    enum Face {FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM}

    interface Painter {
        int paint(Face face, int u, int v);
    }

    /** A bendable cuboid: its box in the part's own space, how it bends, and its skin. */
    record Cube(double[] min, double[] max, int bendDir, Painter painter) {
        // bendDir: +1 = Direction.UP (arms, legs), -1 = Direction.DOWN (torso), 0 = never bends (head).
    }

    // ------------------------------------------------------------------ skin

    static final int HAIR = 0x17161B, HAIR_HI = 0x2A2830, SKIN = 0xE9D2C4, SKIN_SH = 0xD4B6A6, JACKET = 0x1C1C22, JACKET_HI = 0x2A2A33,
        SHIRT = 0xE8E6E2, TIE = 0x111114, PANTS = 0x202027, SHOE = 0x101012, EYE_BLACK = 0x0A0A0C, EYE_RED = 0xE0141E;

    static int head(Face f, int u, int v) {
        switch (f) {
            case TOP: return (u + v) % 3 == 0 ? HAIR_HI : HAIR;
            case BOTTOM: return SKIN_SH;
            case BACK: return v < 7 ? ((u * 7 + v) % 4 == 0 ? HAIR_HI : HAIR) : SKIN_SH;
            case LEFT: case RIGHT: {
                boolean front = f == Face.RIGHT ? u >= 5 : u <= 2; // the half toward the face
                if (v < 3 || !front && v < 7) return (u + v) % 3 == 0 ? HAIR_HI : HAIR;
                return v == 7 ? SKIN_SH : SKIN;
            }
            default: {
                // Face, seen from the front: u 0 is the player's right side.
                if (v <= 1) return (u + v) % 3 == 0 ? HAIR_HI : HAIR;
                if (v == 2 && (u != 2 && u != 5)) return HAIR;
                if (v <= 3 && (u == 0 || u == 7)) return HAIR;
                if (v == 4) {
                    if (u == 1 || u == 6) return EYE_BLACK;
                    if (u == 2 || u == 5) return EYE_RED;
                }
                if (v == 6 && (u == 3 || u == 4)) return SKIN_SH;
                return SKIN;
            }
        }
    }

    static int body(Face f, int u, int v) {
        if (f == Face.FRONT) {
            if (v >= 10) return v == 10 ? 0x141417 : JACKET;
            if (u == 3 || u == 4) return u == 4 && v >= 1 && v <= 8 ? TIE : SHIRT;
            if ((u == 2 || u == 5) && v <= 2) return SHIRT;
            return JACKET;
        }
        return (u + v) % 5 == 0 ? JACKET_HI : JACKET;
    }

    static int arm(Face f, int u, int v) {
        if (f == Face.BOTTOM) return SKIN_SH;
        if (f == Face.TOP) return JACKET;
        if (v >= 10) return v == 11 ? SKIN_SH : SKIN;
        if (v == 9) return SHIRT; // the shirt cuff
        return (u + v) % 6 == 0 ? JACKET_HI : JACKET;
    }

    static int leg(Face f, int u, int v) {
        if (f == Face.BOTTOM) return SHOE;
        if (f == Face.TOP) return PANTS;
        if (v >= 10) return SHOE;
        return (u + 2 * v) % 7 == 0 ? 0x2A2A33 : PANTS;
    }

    // ------------------------------------------------------------------ posing

    /** The transforms PlayerAnimator ends up with for one part, starting from vanilla's standing values. */
    static final class Part {
        double px, py, pz, rx, ry, rz, bendAxis, bend;
    }

    static Part part(RigPose pose, String name, double px, double py, double pz, double rx, double ry, double rz) {
        Part p = new Part();
        float[] pos = pose.transform(name, RigPose.POSITION, (float)px, (float)py, (float)pz);
        float[] rot = pose.transform(name, RigPose.ROTATION, (float)rx, (float)ry, (float)rz);
        float[] bend = pose.transform(name, RigPose.BEND, 0.0F, 0.0F, 0.0F);
        p.px = pos[0];
        p.py = pos[1];
        p.pz = pos[2];
        p.rx = rot[0];
        p.ry = rot[1];
        p.rz = rot[2];
        p.bendAxis = bend[0];
        p.bend = stiff ? 0.0 : bend[1];
        return p;
    }

    /** bendy-lib IBendable.applyBend on one cuboid-local point (pixels). */
    static double[] bendPoint(double[] q, Cube cube, double axisAngle, double bendValue) {
        if (cube.bendDir == 0 || Math.abs(bendValue) < 1.0E-4) {
            return q.clone();
        }
        boolean up = cube.bendDir > 0;
        double[] dir = up ? new double[] {0, 1, 0} : new double[] {0, -1, 0};
        // axis = (cos a, 0, sin a) turned by Direction.getRotation(): identity for UP, X by pi for DOWN.
        double[] axis = up ? new double[] {Math.cos(axisAngle), 0, Math.sin(axisAngle)} : new double[] {Math.cos(axisAngle), 0, -Math.sin(axisAngle)};
        double[] centre = {(cube.min[0] + cube.max[0]) / 2, (cube.min[1] + cube.max[1]) / 2, (cube.min[2] + cube.max[2]) / 2};
        double[] v1 = cube.min, v7 = cube.max;
        // aPlane through vertex7, bPlane through vertex1, both with the direction as normal; UP (inverted) uses a as base.
        double distA = dot(dir, q) - dot(dir, v7);
        double distB = dot(dir, q) - dot(dir, v1);
        double distFromBase = up ? distA : distB;
        double distFromOther = up ? distB : distA;
        double fullSize = -dot(dir, v1) + dot(dir, v7);
        double halfSize = fullSize / 2;
        double[] bendNormal = norm(cross(dir, axis));
        double distFromBend = dot(bendNormal, q) - dot(bendNormal, centre);
        if (up) {
            distFromBend = -distFromBend;
        }
        double s = Math.tan(bendValue / 2) * distFromBend;
        double[] p = q.clone();
        if (Math.abs(distFromBase) < Math.abs(distFromOther)) {
            double k = -distFromBase / halfSize * s;
            p = add(p, scale(dir, k));
            p = add(centre, rotate(sub(p, centre), axis, bendValue));
        } else {
            double k = -distFromOther / halfSize * s;
            p = add(p, scale(dir, k));
        }
        return p;
    }

    /** ModelPart.translateAndRotate: pivot + Rz Ry Rx q. */
    static double[] partPoint(double[] q, Part part) {
        double x = q[0], y = q[1], z = q[2];
        double y1 = y * Math.cos(part.rx) - z * Math.sin(part.rx), z1 = y * Math.sin(part.rx) + z * Math.cos(part.rx);
        double x2 = x * Math.cos(part.ry) + z1 * Math.sin(part.ry), z2 = -x * Math.sin(part.ry) + z1 * Math.cos(part.ry);
        double x3 = x2 * Math.cos(part.rz) - y1 * Math.sin(part.rz), y3 = x2 * Math.sin(part.rz) + y1 * Math.cos(part.rz);
        return new double[] {part.px + x3, part.py + y3, part.pz + z2};
    }

    /**
     * 6.2.0: how bulked up the body is drawn (0 not at all, 1 fully), as HexHaki's haki physique does it: the arms thicker
     * (x and z scaled by 1 + {@link #ARM_GAIN} times it) and pushed out from the body, and a second chest over the top six pixels of
     * the torso in the same skin, broader and deeper (x by 1 + {@link #CHEST_GAIN}, z by 1 + {@link #CHEST_DEPTH} times it).
     */
    static double bulk = 0.0;
    /** 6.2.0: her right arm left out (the Mutated Arm is in its place). */
    static boolean noRightArm = false;
    /**
     * 7.0.2: a real 64x64 skin (wide arms) to paint the body with instead of the stand-in suit, its second layer drawn half a pixel
     * out (the head's a pixel), as PlayerModel does: the ghoul NPCs wear vanilla's default skins. Null: the stand-in.
     */
    static BufferedImage skin = null;
    /** 1.0.41: {@link #skin} is painted for the slim arms (3 pixels wide, half a pixel lower, as PlayerModel's slim). */
    static boolean slim = false;
    /** 1.0.41: no elbows or knees bent (a mob on vanilla's player model, as a ghoul NPC is drawn): its limbs swing whole. */
    static boolean stiff = false;
    /** What {@link #textured} returns for a see-through texel of the second layer: no cell drawn there. */
    static final int SEE_THROUGH = -1;

    /** A painter reading {@link #skin} for a box of {@code w x h x d} pixels whose texture starts at {@code u0, v0} (vanilla's layout). */
    static Painter textured(int u0, int v0, int w, int h, int d, boolean outer) {
        return (face, u, v) -> {
            int tu;
            int tv;
            switch (face) {
                case FRONT -> { tu = u0 + d + u; tv = v0 + d + v; }
                case BACK -> { tu = u0 + 2 * d + w + u; tv = v0 + d + v; }
                case RIGHT -> { tu = u0 + u; tv = v0 + d + v; }
                case LEFT -> { tu = u0 + d + w + u; tv = v0 + d + v; }
                case TOP -> { tu = u0 + d + u; tv = v0 + d - 1 - v; }
                default -> { tu = u0 + d + w + u; tv = v0 + d - 1 - v; }
            }
            int argb = skin.getRGB(Math.max(0, Math.min(63, tu)), Math.max(0, Math.min(63, tv)));
            if (outer && (argb >>> 24) < 128) {
                return SEE_THROUGH;
            }
            return argb & 0xFFFFFF;
        };
    }
    static final double ARM_GAIN = 0.4;
    static final double CHEST_GAIN = 0.55;
    static final double CHEST_DEPTH = 0.3575;

    /** How far (pixels) each arm is pushed out from the body at {@code bulk} (HexHaki's armSeparation). */
    static double armSeparation(double bulk) {
        return 2.2 * bulk + ARM_GAIN * bulk + 0.35 * bulk;
    }

    /** The model's triangles in world space (blocks; the player stands at the origin facing +z). */
    static List<Tri> model(RigPose pose) {
        List<Tri> tris = new ArrayList<>();
        Part head = part(pose, "head", 0, 0, 0, 0, 0, 0);
        Part torso = part(pose, "torso", 0, 0, 0, 0, 0, 0);
        Part body = part(pose, "body", 0, 0, 0, 0, 0, 0);
        // The torso's own bend is torso + body (AnimationApplier.updatePart); the upper parts turn with the body bend.
        torso.bendAxis += body.bendAxis;
        torso.bend += body.bend;
        // 6.2.0: a pose bulked up of its own (RigPose.bulk) pushes its arms out itself.
        double bulk = Math.max(PoseRender.bulk, pose.bulk);
        double out = armSeparation(bulk) - RigPose.BULK_SPREAD * pose.bulk;
        Part rightArm = part(pose, "rightArm", -5 - out, 2, 0, 0, 0, BOB);
        Part leftArm = part(pose, "leftArm", 5 + out, 2, 0, 0, 0, -BOB);
        Part rightLeg = part(pose, "rightLeg", -1.9, 12, 0.1, 0, 0, 0);
        Part leftLeg = part(pose, "leftLeg", 1.9, 12, 0.1, 0, 0, 0);
        double upperAxisAngle = -body.bendAxis;
        double[] upperAxis = {Math.cos(upperAxisAngle), 0, Math.sin(upperAxisAngle)};
        double upperBend = body.bend;
        java.util.function.UnaryOperator<double[]> upper = q -> Math.abs(upperBend) < 1.0E-4 ? q
            : add(new double[] {0, 6, 0}, rotate(sub(q, new double[] {0, 6, 0}), upperAxis, upperBend));
        java.util.function.UnaryOperator<double[]> world = q -> world(pose, q);

        if (skin != null) {
            // A real skin: each part and its second layer in vanilla's texture layout (wide arms, or slim).
            Object[][] parts = {
                {new double[] {-4, -8, -4}, new double[] {4, 0, 4}, 0, head, upper, 0, 0, 32, 0, 0.5},
                {new double[] {-4, 0, -2}, new double[] {4, 12, 2}, -1, torso, null, 16, 16, 16, 32, 0.25},
                {new double[] {slim ? -2 : -3, slim ? -1.5 : -2, -2}, new double[] {1, slim ? 10.5 : 10, 2}, 1, rightArm, upper, 40, 16, 40, 32, 0.25},
                {new double[] {-1, slim ? -1.5 : -2, -2}, new double[] {slim ? 2 : 3, slim ? 10.5 : 10, 2}, 1, leftArm, upper, 32, 48, 48, 48, 0.25},
                {new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1, rightLeg, null, 0, 16, 0, 32, 0.25},
                {new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1, leftLeg, null, 16, 48, 0, 48, 0.25}};
            for (Object[] p : parts) {
                double[] mn = (double[])p[0];
                double[] mx = (double[])p[1];
                int w = (int)Math.round(mx[0] - mn[0]);
                int h = (int)Math.round(mx[1] - mn[1]);
                int d = (int)Math.round(mx[2] - mn[2]);
                @SuppressWarnings("unchecked")
                java.util.function.UnaryOperator<double[]> carried = p[4] == null ? q -> q : (java.util.function.UnaryOperator<double[]>)p[4];
                cube(tris, new Cube(mn, mx, (Integer)p[2], textured((Integer)p[5], (Integer)p[6], w, h, d, false)), (Part)p[3], carried, world);
                cube(tris, new Cube(mn, mx, (Integer)p[2], textured((Integer)p[7], (Integer)p[8], w, h, d, true)), (Part)p[3], carried, world, 1.0, 1.0,
                    (Double)p[9]);
            }
            return tris;
        }
        cube(tris, new Cube(new double[] {-4, -8, -4}, new double[] {4, 0, 4}, 0, PoseRender::head), head, upper, world);
        cube(tris, new Cube(new double[] {-4, 0, -2}, new double[] {4, 12, 2}, -1, PoseRender::body), torso, q -> q, world);
        double arms = 1.0 + ARM_GAIN * bulk;
        if (!noRightArm) {
            cube(tris, new Cube(new double[] {-3, -2, -2}, new double[] {1, 10, 2}, 1, PoseRender::arm), rightArm, upper, world, arms, arms);
        }
        cube(tris, new Cube(new double[] {-1, -2, -2}, new double[] {3, 10, 2}, 1, PoseRender::arm), leftArm, upper, world, arms, arms);
        if (bulk > 0.001) {
            cube(tris, new Cube(new double[] {-4, 0, -2}, new double[] {4, 6, 2}, -1, PoseRender::body), torso, q -> q, world, 1.0 + CHEST_GAIN * bulk,
                1.0 + CHEST_DEPTH * bulk);
        }
        cube(tris, new Cube(new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1, PoseRender::leg), rightLeg, q -> q, world);
        cube(tris, new Cube(new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1, PoseRender::leg), leftLeg, q -> q, world);
        return tris;
    }

    /**
     * Where a point of one part ends up in the world (blocks): {@code local} is in the part's own pixels (arms: the box
     * runs y -2..10 from the shoulder pivot; legs: 0..12 from the hip), exactly as {@link #model} places it.
     */
    static double[] point(RigPose pose, String name, double[] local) {
        Part body = part(pose, "body", 0, 0, 0, 0, 0, 0);
        double[] upperAxis = {Math.cos(-body.bendAxis), 0, Math.sin(-body.bendAxis)};
        boolean upperPart;
        Part part;
        Cube cube;
        switch (name) {
            case "rightArm" -> { part = part(pose, name, -5, 2, 0, 0, 0, BOB); cube = new Cube(new double[] {-3, -2, -2}, new double[] {1, 10, 2}, 1, PoseRender::arm); upperPart = true; }
            case "leftArm" -> { part = part(pose, name, 5, 2, 0, 0, 0, -BOB); cube = new Cube(new double[] {-1, -2, -2}, new double[] {3, 10, 2}, 1, PoseRender::arm); upperPart = true; }
            case "rightLeg" -> { part = part(pose, name, -1.9, 12, 0.1, 0, 0, 0); cube = new Cube(new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1, PoseRender::leg); upperPart = false; }
            case "leftLeg" -> { part = part(pose, name, 1.9, 12, 0.1, 0, 0, 0); cube = new Cube(new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 1, PoseRender::leg); upperPart = false; }
            case "torso" -> {
                part = part(pose, "torso", 0, 0, 0, 0, 0, 0);
                part.bendAxis += body.bendAxis;
                part.bend += body.bend;
                cube = new Cube(new double[] {-4, 0, -2}, new double[] {4, 12, 2}, -1, PoseRender::body);
                upperPart = false;
            }
            default -> { part = part(pose, "head", 0, 0, 0, 0, 0, 0); cube = new Cube(new double[] {-4, -8, -4}, new double[] {4, 0, 4}, 0, PoseRender::head); upperPart = true; }
        }
        double[] q = partPoint(bendPoint(local, cube, part.bendAxis, part.bend), part);
        if (upperPart && Math.abs(body.bend) > 1.0E-4) {
            q = add(new double[] {0, 6, 0}, rotate(sub(q, new double[] {0, 6, 0}), upperAxis, body.bend));
        }
        return world(pose, q);
    }

    /**
     * A model-space point (pixels) in the world (blocks; x to the player's left, z forward), as PlayerAnimator's
     * PlayerRendererMixin places it: the whole model turned about a point 0.7 blocks up by the body's rotation (ZP roll, YP spin,
     * XP flip, in its own frame: x to the player's right, z backward; x+ tips the head backwards, y+ turns her to her left, z+ lifts
     * her right side), then moved by the body's position (the drop).
     */
    static double[] world(RigPose pose, double[] q) {
        float[] bodyPos = pose.transform("body", RigPose.POSITION, 0, 0, 0);
        float[] bodyRot = pose.transform("body", RigPose.ROTATION, 0, 0, 0);
        double[] w = {q[0] / 16.0, 1.501 - q[1] / 16.0 - 0.7, -q[2] / 16.0};
        // This world is the mixin's frame turned half round (x and z the other way): its x and z turns run backwards here.
        w = rotate(w, new double[] {1, 0, 0}, -bodyRot[0]);
        w = rotate(w, new double[] {0, 1, 0}, bodyRot[1]);
        w = rotate(w, new double[] {0, 0, 1}, -bodyRot[2]);
        // The body's position is in the mixin's frame too (x to her right, z backward).
        return new double[] {w[0] - bodyPos[0], w[1] + 0.7 + bodyPos[1], w[2] - bodyPos[2]};
    }

    /** Tessellates a cuboid into one-pixel cells, bends, poses and places each corner, and paints each cell. */
    static void cube(List<Tri> tris, Cube c, Part part, java.util.function.UnaryOperator<double[]> upper,
                     java.util.function.UnaryOperator<double[]> world) {
        cube(tris, c, part, upper, world, 1.0, 1.0);
    }

    /** The same, the part scaled {@code sx} across and {@code sz} front to back after its bend (ModelPart.xScale, zScale). */
    static void cube(List<Tri> tris, Cube c, Part part, java.util.function.UnaryOperator<double[]> upper,
                     java.util.function.UnaryOperator<double[]> world, double scaleX, double scaleZ) {
        cube(tris, c, part, upper, world, scaleX, scaleZ, 0.0);
    }

    /** The same, the box grown {@code grow} pixels out on every side (a skin's second layer), its texels as the box's own. */
    static void cube(List<Tri> tris, Cube c, Part part, java.util.function.UnaryOperator<double[]> upper,
                     java.util.function.UnaryOperator<double[]> world, double scaleX, double scaleZ, double grow) {
        double[] mn = c.min, mx = c.max;
        int sx = (int)Math.round(mx[0] - mn[0]), sy = (int)Math.round(mx[1] - mn[1]), sz = (int)Math.round(mx[2] - mn[2]);
        java.util.function.Function<double[], double[]> place = flat -> {
            double[] q = flat;
            if (grow > 0.0) {
                q = new double[3];
                for (int i = 0; i < 3; i++) {
                    double mid = (mn[i] + mx[i]) * 0.5;
                    double half = (mx[i] - mn[i]) * 0.5;
                    q[i] = mid + (flat[i] - mid) * (half + grow) / half;
                }
            }
            double[] bent = bendPoint(q, c, part.bendAxis, part.bend);
            return world.apply(upper.apply(partPoint(new double[] {bent[0] * scaleX, bent[1], bent[2] * scaleZ}, part)));
        };
        for (Face f : Face.values()) {
            int nu, nv;
            switch (f) {
                case FRONT, BACK -> { nu = sx; nv = sy; }
                case LEFT, RIGHT -> { nu = sz; nv = sy; }
                default -> { nu = sx; nv = sz; }
            }
            double[][][] grid = new double[nu + 1][nv + 1][];
            for (int u = 0; u <= nu; u++) {
                for (int v = 0; v <= nv; v++) {
                    double[] q = switch (f) {
                        case FRONT -> new double[] {mn[0] + u, mn[1] + v, mn[2]};
                        case BACK -> new double[] {mx[0] - u, mn[1] + v, mx[2]};
                        case LEFT -> new double[] {mx[0], mn[1] + v, mn[2] + u};
                        case RIGHT -> new double[] {mn[0], mn[1] + v, mx[2] - u};
                        case TOP -> new double[] {mn[0] + u, mn[1], mn[2] + v};
                        case BOTTOM -> new double[] {mn[0] + u, mx[1], mn[2] + v};
                    };
                    grid[u][v] = place.apply(q);
                }
            }
            double[] inside = place.apply(new double[] {(mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2, (mn[2] + mx[2]) / 2});
            for (int u = 0; u < nu; u++) {
                for (int v = 0; v < nv; v++) {
                    int rgb = c.painter.paint(f, u, v);
                    if (rgb == SEE_THROUGH) {
                        continue;
                    }
                    double[] a = grid[u][v], b = grid[u + 1][v], d = grid[u][v + 1], e = grid[u + 1][v + 1];
                    double[] n = norm(cross(sub(b, a), sub(d, a)));
                    double[] mid = {(a[0] + e[0]) / 2 - inside[0], (a[1] + e[1]) / 2 - inside[1], (a[2] + e[2]) / 2 - inside[2]};
                    if (dot(n, mid) < 0) {
                        n = scale(n, -1);
                    }
                    tris.add(new Tri(new double[][] {a, b, e}, rgb, n));
                    tris.add(new Tri(new double[][] {a, e, d}, rgb, n));
                }
            }
        }
    }

    // ------------------------------------------------------------------ rasterizing

    /** A perspective view: eye and target in world blocks, vertical field of view in degrees. */
    record Camera(double[] eye, double[] target, double fov) {
    }

    static final class Canvas {
        final int w, h, ss;
        final int[] rgb;
        final double[] depth;
        final double[] eye, fwd, right, up;
        final double focal;

        Canvas(int w, int h, int ss, Camera cam, int background) {
            this.w = w * ss;
            this.h = h * ss;
            this.ss = ss;
            this.rgb = new int[this.w * this.h];
            this.depth = new double[this.w * this.h];
            java.util.Arrays.fill(this.rgb, background);
            java.util.Arrays.fill(this.depth, Double.POSITIVE_INFINITY);
            this.eye = cam.eye;
            this.fwd = norm(sub(cam.target, cam.eye));
            double[] r = norm(cross(this.fwd, new double[] {0, 1, 0}));
            this.right = r;
            this.up = cross(r, this.fwd);
            this.focal = (this.h / 2.0) / Math.tan(Math.toRadians(cam.fov / 2));
        }

        double[] project(double[] p) {
            double[] d = sub(p, this.eye);
            double z = dot(d, this.fwd);
            if (z < 0.05) return null;
            return new double[] {this.w / 2.0 + dot(d, this.right) * this.focal / z, this.h / 2.0 - dot(d, this.up) * this.focal / z, z};
        }

        void tri(Tri t, boolean lit) {
            double[] a = project(t.v[0]), b = project(t.v[1]), c = project(t.v[2]);
            if (a == null || b == null || c == null) return;
            int color = t.rgb;
            if (lit) {
                // Minecraft's two entity lights plus ambient, softly.
                double[] l0 = norm(new double[] {0.2, 1.0, -0.7}), l1 = norm(new double[] {-0.2, 1.0, 0.7});
                double light = Math.min(1.0, 0.45 + 0.45 * Math.max(0, dot(t.n, l0)) + 0.45 * Math.max(0, dot(t.n, l1)));
                color = shade(color, light);
            }
            int minX = (int)Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
            int maxX = (int)Math.min(this.w - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
            int minY = (int)Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
            int maxY = (int)Math.min(this.h - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
            double area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
            if (Math.abs(area) < 1.0E-9) return;
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double px = x + 0.5, py = y + 0.5;
                    double w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) / area;
                    double w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) / area;
                    double w2 = 1 - w0 - w1;
                    if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
                    double z = 1.0 / (w0 / a[2] + w1 / b[2] + w2 / c[2]);
                    int i = y * this.w + x;
                    if (z < this.depth[i]) {
                        this.depth[i] = z;
                        this.rgb[i] = color;
                    }
                }
            }
        }

        void floor(double size, int a, int b) {
            for (int i = (int)-size; i < size; i++) {
                for (int j = (int)-size; j < size; j++) {
                    int c = ((i + j) & 1) == 0 ? a : b;
                    double[] p0 = {i, 0, j}, p1 = {i + 1, 0, j}, p2 = {i + 1, 0, j + 1}, p3 = {i, 0, j + 1};
                    tri(new Tri(new double[][] {p0, p1, p2}, c, new double[] {0, 1, 0}), false);
                    tri(new Tri(new double[][] {p0, p2, p3}, c, new double[] {0, 1, 0}), false);
                }
            }
        }

        BufferedImage image() {
            BufferedImage out = new BufferedImage(this.w / this.ss, this.h / this.ss, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < out.getHeight(); y++) {
                for (int x = 0; x < out.getWidth(); x++) {
                    int r = 0, g = 0, bl = 0;
                    for (int dy = 0; dy < this.ss; dy++) {
                        for (int dx = 0; dx < this.ss; dx++) {
                            int c = this.rgb[(y * this.ss + dy) * this.w + x * this.ss + dx];
                            r += (c >> 16) & 255;
                            g += (c >> 8) & 255;
                            bl += c & 255;
                        }
                    }
                    int n = this.ss * this.ss;
                    out.setRGB(x, y, (r / n) << 16 | (g / n) << 8 | bl / n);
                }
            }
            return out;
        }
    }

    /** The posed player on a floor, seen by {@code cam}. */
    static BufferedImage render(RigPose pose, Camera cam, int w, int h, List<Tri> extra) {
        Canvas canvas = new Canvas(w, h, 2, cam, 0x3A4450);
        canvas.floor(6, 0x58626C, 0x4E5862);
        for (Tri t : model(pose)) canvas.tri(t, true);
        if (extra != null) for (Tri t : extra) canvas.tri(t, true);
        return canvas.image();
    }

    /** A camera orbiting the player: {@code yaw} 0 looks at the face, 90 at the player's left side, 180 at the back. */
    static Camera orbit(double yawDegrees, double pitchDegrees, double distance, double targetY) {
        double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees);
        double[] target = {0, targetY, 0};
        double[] eye = {Math.sin(yaw) * Math.cos(pitch) * distance, targetY + Math.sin(pitch) * distance, Math.cos(yaw) * Math.cos(pitch) * distance};
        return new Camera(eye, target, 34);
    }

    // ------------------------------------------------------------------ vector helpers

    static double dot(double[] a, double[] b) {return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];}
    static double[] add(double[] a, double[] b) {return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};}
    static double[] sub(double[] a, double[] b) {return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};}
    static double[] scale(double[] a, double k) {return new double[] {a[0] * k, a[1] * k, a[2] * k};}
    static double[] cross(double[] a, double[] b) {return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};}
    static double[] norm(double[] a) {double l = Math.sqrt(dot(a, a)); return l < 1e-12 ? new double[] {0, 1, 0} : scale(a, 1 / l);}

    /** Rodrigues rotation of {@code v} about unit {@code k} by {@code angle} (right-handed, as JOML). */
    static double[] rotate(double[] v, double[] k, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double[] kxv = cross(k, v);
        double kv = dot(k, v);
        return new double[] {v[0] * c + kxv[0] * s + k[0] * kv * (1 - c), v[1] * c + kxv[1] * s + k[1] * kv * (1 - c),
            v[2] * c + kxv[2] * s + k[2] * kv * (1 - c)};
    }

    static int shade(int rgb, double k) {
        int r = (int)Math.min(255, ((rgb >> 16) & 255) * k), g = (int)Math.min(255, ((rgb >> 8) & 255) * k), b = (int)Math.min(255, (rgb & 255) * k);
        return r << 16 | g << 8 | b;
    }
}
