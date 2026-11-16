package com.hex.kagune.control.awakening.geo;

import com.hex.kagune.control.bikaku.GrandDevour;
import com.hex.kagune.control.bikaku.MawMotion;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 3.6.5: the inside of Grand Devouring's mouth (the GeckoLib model the player being eaten sees, {@code devour_cavity.geo.json}, made by
 * {@code tools/devour_cavity.py}), baked as GeckoLib bakes it: every bone the code drives is there; its pulse animates every ring of wall
 * and throat; its UVs inside the texture; its walls closed all round at every height while they pulse (nothing of the outside world shows
 * through the flesh); its jaws hanging out past the rim when wide and lying across the hole when shut; and the player's eyes inside the
 * cavity, as the game places it along the worm's head, from as the mouth comes down round them until they are swallowed.
 */
public final class DevourCavityTest {
    /** As the game draws it (DevourInside): the model's canonical radius and walls' height, and a little inside the worm's cavity. */
    static final double MODEL_RADIUS = 1.0;
    static final double MODEL_DEPTH = 2.25;
    static final double INSIDE = 0.95;
    static final double WIDE = Math.toRadians(-100.0);
    static int passed;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed++;
    }

    public static void main(String[] args) throws Exception {
        Path assets = Path.of("src/main/resources/assets/hexkagune");
        GeoModel model = GeoModel.read(Files.readString(assets.resolve("geo/devour_cavity.geo.json")));
        GeoAnimation animation = GeoAnimation.read(Files.readString(assets.resolve("animations/devour_cavity.animation.json")));
        check(Files.isRegularFile(assets.resolve("textures/geo/devour_cavity.png")), "its texture is there");
        // Every bone the code drives, under its parent.
        for (int k = 0; k < 6; k++) {
            check(model.bone("wall" + k) >= 0 && model.bones[model.bone("tooth" + k)].parent == model.bone("wall" + k), "ring " + k + " and its teeth");
        }
        for (int k = 0; k < 3; k++) {
            check(model.bone("throat" + k) >= 0, "throat ring " + k);
        }
        check(model.bone("cap") >= 0, "the throat ends in the dark");
        for (int j = 0; j < 8; j++) {
            check(model.bones[model.bone("hinge" + j)].parent == model.bone("jaw" + j) && model.bones[model.bone("fang" + j)].parent == model.bone("hinge" + j),
                "jaw " + j + ": its hinge and its fang");
            check(model.bones[model.bone("hinge" + j)].rotX == 0.0, "a hinge has no turn of its own (code sets it outright)");
        }
        GeoAnimation.Clip pulse = animation.clip("animation.hexkagune.devour_cavity.pulse");
        check(pulse != null && pulse.loop == GeoAnimation.Loop.LOOP && pulse.length > 1.0, "its pulse loops");
        for (int k = 0; k < 6; k++) {
            check(pulse.bones.containsKey("wall" + k), "the pulse works ring " + k);
        }
        for (int k = 0; k < 3; k++) {
            check(pulse.bones.containsKey("throat" + k), "the pulse works the throat " + k);
        }
        check(model.quadCount() < 3000, "a sane number of quads: " + model.quadCount());
        for (GeoModel.Bone bone : model.bones) {
            for (GeoModel.Cube cube : bone.cubes) {
                for (GeoModel.Quad quad : cube.quads) {
                    if (quad == null) continue;
                    for (float uv : quad.uvs) check(uv >= 0.0F && uv <= 1.0F, "UV inside the texture in " + bone.name);
                    for (float p : quad.positions) check(Float.isFinite(p) && Math.abs(p) < 4.0F, "inside its bounds in " + bone.name);
                }
            }
        }
        // Closed all round: from its middle, every way round at every height of its walls, a wall within a pixel and a half of its radius,
        // at rest and all through the pulse.
        Molang.Context still = new Molang.Context() {
            public double animTime() {
                return 0.0;
            }

            public double lifeTime() {
                return 0.0;
            }

            public double variable(String name) {
                return 0.0;
            }
        };
        for (double t = 0.0; t < pulse.length; t += 0.2) {
            GeoPose pose = new GeoPose(model);
            pose.apply(pulse, t, 1.0, still, null);
            double[][] m = pose.matrices();
            for (double y = 0.2; y < 6 * 6.0 - 0.2; y += 0.5) {
                for (int a = 0; a < 360; a += 3) {
                    double hit = nearestWall(model, pose, m, y / 16.0, Math.toRadians(a));
                    check(hit < (16.0 + 1.6) / 16.0 * 1.09, "the wall is closed at height " + y + ", " + a + " degrees, " + t + " s (" + hit + ")");
                }
            }
        }
        // The jaws: wide, hanging out past the rim, below the mouth; shut, lying across the hole.
        for (double open : new double[] {1.0, 0.0}) {
            GeoPose pose = new GeoPose(model);
            for (int j = 0; j < 8; j++) {
                pose.rotX[model.bone("hinge" + j)] = WIDE * open;
            }
            double[][] m = pose.matrices();
            for (int j = 0; j < 8; j++) {
                int b = model.bone("hinge" + j);
                for (GeoModel.Cube cube : model.bones[b].cubes) {
                    double[] cm = Mat4.multiply(m[b], GeoPose.cubeMatrix(cube));
                    double lowest = 9.0;
                    double inmost = 9.0;
                    java.util.List<double[]> corners = new java.util.ArrayList<>();
                    for (GeoModel.Quad q : cube.quads) {
                        if (q == null) continue;
                        for (int v = 0; v < 4; v++) {
                            double[] p = Mat4.point(cm, q.positions[v * 3], q.positions[v * 3 + 1], q.positions[v * 3 + 2]);
                            lowest = Math.min(lowest, p[1]);
                            inmost = Math.min(inmost, Math.hypot(p[0], p[2]));
                            corners.add(p);
                        }
                    }
                    // The middle of its inner edge: the corners nearest the middle, averaged.
                    double[] edge = new double[3];
                    int n = 0;
                    for (double[] p : corners) {
                        if (Math.hypot(p[0], p[2]) < inmost + 0.01) {
                            edge[0] += p[0];
                            edge[2] += p[2];
                            n++;
                        }
                    }
                    double reach = Math.hypot(edge[0] / n, edge[2] / n);
                    if (open > 0.5) {
                        check(lowest < -0.6 && inmost > 0.85, "a wide jaw hangs out past the rim (" + lowest + ", " + inmost + ")");
                    } else {
                        check(lowest > -0.15 && reach < 0.1, "a shut jaw lies across the hole, its inner edge at the middle (" + lowest + ", " + reach + ")");
                    }
                }
            }
        }
        // The player's eyes inside the drawn cavity, for every worm of every form, from as the mouth comes down round them until swallowed.
        double width = 0.6;
        double height = 1.8;
        double[] spot = {0.4, 0.0, 4.2};
        for (int worms = 1; worms <= 3; worms++) {
            for (int i = 0; i < worms; i++) {
                for (double age = GrandDevour.CAUGHT - GrandDevour.ENGULF * 0.35; age < GrandDevour.KILL; age += 0.5) {
                    GrandDevour.Feeding f = GrandDevour.at(100 + age, 7, worms, i, spot, width, height, age);
                    double[][] drawn = MawMotion.around(f.shape().path(), 1.0, f.shape().aperture(), f.feed());
                    double[][] axis = GrandDevour.mouthAxis(drawn, f.feed().depth());
                    double[] eye = {spot[0], spot[1] + GrandDevour.lift(age, height) + 1.62, spot[2]};
                    double[] along = sub(axis[1], axis[0]);
                    double length = Math.sqrt(dot(along, along));
                    double[] d = {along[0] / length, along[1] / length, along[2] / length};
                    double[] rel = sub(eye, axis[0]);
                    double up = dot(rel, d);
                    double aside = Math.sqrt(Math.max(0.0, dot(rel, rel) - up * up));
                    double depth = f.feed().depth();
                    check(up > 0.05 && up < depth, "the eyes are within the cavity's depth (" + up + " of " + depth + " at " + age + ")");
                    check(aside < f.feed().hole() * INSIDE * 0.9, "the eyes are inside the cavity's walls (" + aside + " at " + age + ")");
                }
            }
        }
        System.out.println("Devour cavity tests: " + passed + " checks passed (" + model.bones.length + " bones, " + model.quadCount() + " quads)");
    }

    /** How far from the middle (blocks) a level ray at height {@code y} going {@code angle} round first meets a wall ring's quad. */
    static double nearestWall(GeoModel model, GeoPose pose, double[][] m, double y, double angle) {
        double[] o = {0.0, y, 0.0};
        double[] dir = {Math.cos(angle), 0.0, Math.sin(angle)};
        double best = Double.MAX_VALUE;
        for (int b = 0; b < model.bones.length; b++) {
            if (!model.bones[b].name.startsWith("wall") || pose.invisible(b)) continue;
            for (GeoModel.Cube cube : model.bones[b].cubes) {
                double[] cm = Mat4.multiply(m[b], GeoPose.cubeMatrix(cube));
                for (GeoModel.Quad q : cube.quads) {
                    if (q == null) continue;
                    double[][] v = new double[4][];
                    for (int k = 0; k < 4; k++) {
                        v[k] = Mat4.point(cm, q.positions[k * 3], q.positions[k * 3 + 1], q.positions[k * 3 + 2]);
                    }
                    best = Math.min(best, Math.min(ray(o, dir, v[0], v[1], v[2]), ray(o, dir, v[0], v[2], v[3])));
                }
            }
        }
        return best;
    }

    /** Möller–Trumbore: the distance along the ray to the triangle, or the largest double. */
    static double ray(double[] o, double[] d, double[] a, double[] b, double[] c) {
        double[] e1 = sub(b, a);
        double[] e2 = sub(c, a);
        double[] p = cross(d, e2);
        double det = dot(e1, p);
        if (Math.abs(det) < 1e-12) return Double.MAX_VALUE;
        double inv = 1.0 / det;
        double[] s = sub(o, a);
        double u = dot(s, p) * inv;
        if (u < -1e-9 || u > 1 + 1e-9) return Double.MAX_VALUE;
        double[] q = cross(s, e1);
        double v = dot(d, q) * inv;
        if (v < -1e-9 || u + v > 1 + 1e-9) return Double.MAX_VALUE;
        double t = dot(e2, q) * inv;
        return t > 0 ? t : Double.MAX_VALUE;
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
