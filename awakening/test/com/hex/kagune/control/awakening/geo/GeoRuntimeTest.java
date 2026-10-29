package com.hex.kagune.control.awakening.geo;

import java.util.List;
import java.util.Map;

/** The model and animation runtime against hand-computed GeckoLib results. */
public final class GeoRuntimeTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0E-6, message + ": expected " + expected + ", got " + actual);
    }

    private static final String ARM_MODEL = """
            {"format_version": "1.12.0", "minecraft:geometry": [{
              "description": {"identifier": "geometry.test", "texture_width": 64, "texture_height": 64},
              "bones": [
                {"name": "hand", "parent": "right_arm", "pivot": [-6, 12, 0], "cubes": [
                  {"origin": [-7, 10, -1], "size": [2, 2, 2], "uv": {"north": {"uv": [0, 0], "uv_size": [8, 8]}}}]},
                {"name": "right_arm", "pivot": [-5, 22, 0], "cubes": [{"origin": [-8, 12, -2], "size": [4, 12, 4], "uv": [40, 16]}]}
              ]}]}
            """;

    private static final String CLIPS = """
            {"format_version": "1.8.0", "animations": {
              "animation.test.raise": {"loop": true, "animation_length": 2.0, "bones": {
                "right_arm": {"rotation": {"0.0": [0, 0, 0], "1.0": [-90, 0, 0], "2.0": [0, 0, 0]}}}},
              "animation.test.smooth": {"bones": {
                "right_arm": {"position": {"0.0": {"vector": [0, 0, 0], "lerp_mode": "catmullrom"},
                                           "1.0": {"vector": [4, 0, 0], "lerp_mode": "catmullrom"},
                                           "2.0": {"vector": [4, 8, 0], "lerp_mode": "catmullrom"}}}}},
              "animation.test.jump": {"loop": "hold_on_last_frame", "bones": {
                "right_arm": {"scale": {"0.0": 1, "1.0": {"pre": [1, 1, 1], "post": [2, 2, 2]}, "1.5": 2}}}},
              "animation.test.molang": {"loop": true, "bones": {
                "right_arm": {"rotation": ["math.sin(query.anim_time * 90) * 30", "variable.charge * 10", 0]}}}
            }}
            """;

    public static void main(String[] args) {
        // JSON
        Map<String, Object> parsed = Json.map(Json.parse("{\"a\": [1, -2.5e1, \"x\\u0041\\n\"], \"b\": {\"c\": true, \"d\": null}}"), "test");
        List<Object> a = Json.list(parsed.get("a"), "a");
        check(a.get(0).equals(1.0) && a.get(1).equals(-25.0) && a.get(2).equals("xA\n"), "JSON values");
        check(Json.map(parsed.get("b"), "b").get("c").equals(Boolean.TRUE) && Json.map(parsed.get("b"), "b").containsKey("d"), "JSON nested object");
        for (String bad : new String[] {"{", "[1,]x", "{\"a\" 1}", "\"open", "nul"}) {
            boolean failed = false;
            try {
                Json.parse(bad);
            } catch (IllegalArgumentException expected) {
                failed = true;
            }
            check(failed, "malformed JSON must be rejected: " + bad);
        }

        // Molang
        Molang.Context context = new Molang.Context() {
            @Override
            public double animTime() {
                return 0.5;
            }

            @Override
            public double lifeTime() {
                return 7.0;
            }

            @Override
            public double variable(String name) {
                return name.equals("charge") ? 3.0 : 0.0;
            }
        };
        near(Molang.compile("math.sin(90)").eval(context), 1.0, "Molang trigonometry takes degrees");
        near(Molang.compile("Query.Anim_Time * 2 + v.charge").eval(context), 4.0, "Molang is case-insensitive and reads queries and variables");
        near(Molang.compile("-2 * (1 + 2) / 4").eval(context), -1.5, "Molang arithmetic precedence");
        near(Molang.compile("q.life_time > 5 ? math.clamp(10, 0, 4) : 1;").eval(context), 4.0, "Molang comparison, ternary, clamp, trailing semicolon");
        check(Molang.compile("math.cos(0) * 3").constant(), "constant expressions fold");
        boolean rejected = false;
        try {
            Molang.compile("math.random(1, 2)");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "unknown Molang functions are rejected, not silently zero");

        // Model baking
        GeoModel model = GeoModel.read(ARM_MODEL);
        check(model.bones.length == 2 && model.bones[0].name.equals("right_arm"), "parents are baked before children");
        int arm = model.bone("right_arm");
        int hand = model.bone("hand");
        check(model.bones[hand].parent == arm, "hand hangs from the arm");
        near(model.bones[arm].pivotX, 5.0, "bone pivot x is negated");
        GeoModel.Quad[] armQuads = model.bones[arm].cubes[0].quads;
        // North face (index 2) of a 4x12x4 box at uv [40, 16]: u 44..48, v 20..32, u mirrored by default.
        GeoModel.Quad north = armQuads[2];
        near(north.positions[0], 4.0 / 16.0, "cube origin x becomes -(origin.x + size.x)");
        near(north.positions[3], 8.0 / 16.0, "second north vertex is the far x edge");
        near(north.uvs[0], 48.0 / 64.0, "box UV north face starts on the right");
        near(north.uvs[2], 44.0 / 64.0, "box UV north face ends on the left");
        near(north.uvs[1], 20.0 / 64.0, "box UV north face top");
        near(north.uvs[5], 32.0 / 64.0, "box UV north face bottom");
        near(north.normalZ, -1.0, "north faces -z");
        check(model.bones[hand].cubes[0].quads[0] == null && model.bones[hand].cubes[0].quads[2] != null, "per-face UV keeps only the faces it lists");

        // Forward kinematics: an arm raised by -90 degrees about X points forwards (-z).
        GeoAnimation clips = GeoAnimation.read(CLIPS);
        GeoPose pose = new GeoPose(model);
        GeoAnimation.Clip raise = clips.clip("animation.test.raise");
        pose.apply(raise, 1.0, 1.0, Molang.NONE, null);
        near(pose.rotX[arm], Math.PI / 2.0, "keyframe rotation x is negated into radians");
        double[][] matrices = pose.matrices();
        double[] fist = Mat4.point(matrices[arm], 6.0 / 16.0, 12.0 / 16.0, 0.0);
        near(fist[0], 6.0 / 16.0, "raised arm keeps its side");
        near(fist[1], 22.0 / 16.0, "raised arm is level with its shoulder");
        near(fist[2], -10.0 / 16.0, "raised arm points forwards");
        double[] knuckle = Mat4.point(matrices[hand], 6.0 / 16.0, 10.0 / 16.0, 0.0);
        near(knuckle[2], -12.0 / 16.0, "child bones follow their parent");

        // Interpolation, looping, holding and Molang keyframes.
        pose.reset();
        pose.apply(raise, 2.5, 1.0, Molang.NONE, null);
        near(pose.rotX[arm], Math.toRadians(45.0), "looping clip wraps and interpolates linearly");
        GeoAnimation.Clip smooth = clips.clip("animation.test.smooth");
        pose.reset();
        pose.apply(smooth, 1.0, 1.0, Molang.NONE, null);
        near(pose.posX[arm], 4.0, "Catmull-Rom passes through its keyframes");
        pose.reset();
        pose.apply(smooth, 1.5, 1.0, Molang.NONE, null);
        near(pose.posX[arm], GeoAnimation.catmullRom(0.5, 0.0, 4.0, 4.0, 4.0), "Catmull-Rom between keyframes");
        check(pose.posX[arm] > 4.0, "Catmull-Rom overshoots towards the next keyframe");
        check(smooth.finished(2.5) && !smooth.finished(1.0), "a clip without loop finishes");
        GeoAnimation.Clip jump = clips.clip("animation.test.jump");
        pose.reset();
        pose.apply(jump, 0.999, 1.0, Molang.NONE, null);
        check(pose.scaleX[arm] < 1.01, "before a pre/post keyframe the value arrives at pre");
        pose.reset();
        pose.apply(jump, 1.001, 1.0, Molang.NONE, null);
        near(pose.scaleX[arm], 2.0, "after a pre/post keyframe the value leaves from post");
        pose.reset();
        pose.apply(jump, 9.0, 1.0, Molang.NONE, null);
        check(!jump.finished(9.0) && pose.scaleX[arm] == 2.0, "hold_on_last_frame holds");
        GeoAnimation.Clip molang = clips.clip("animation.test.molang");
        pose.reset();
        pose.apply(molang, 1.0, 1.0, context, null);
        near(pose.rotX[arm], Math.toRadians(-30.0), "Molang keyframes see the clip's own time");
        near(pose.rotY[arm], Math.toRadians(-30.0), "Molang keyframes see game variables");

        // Blending and skipped bones.
        pose.reset();
        pose.apply(raise, 1.0, 0.5, Molang.NONE, null);
        near(pose.rotX[arm], Math.PI / 4.0, "half weight blends halfway");
        pose.reset();
        pose.apply(raise, 1.0, 1.0, Molang.NONE, bone -> bone == arm);
        near(pose.rotX[arm], 0.0, "skipped bones keep their pose");
        System.out.println("Verified: JSON and Molang readers, GeckoLib bone/cube/UV baking, forward kinematics, keyframe interpolation, looping, holding and blending.");
    }
}
