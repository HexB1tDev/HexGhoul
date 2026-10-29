package com.hex.kagune.control.awakening.geo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The generated kakuja models load through the runtime, carry the player-model proxy bones and the
 * articulated parts the forms need, stay inside the texture, and fit a per-player polygon budget.
 */
public final class KakujaAssetsTest {
    private static final int QUAD_BUDGET = 6_000;
    private static final String[] PROXIES = {"root", "head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path assets = Path.of("awakening/assets");
        int checked = 0;
        for (String form : new String[] {"war", "peace"}) {
            Path file = assets.resolve("geo/" + form + ".geo.json");
            if (!Files.exists(file)) {
                System.out.println("Not built yet: " + form);
                continue;
            }
            GeoModel model = GeoModel.read(Files.readString(file));
            for (String proxy : PROXIES) check(model.bone(proxy) >= 0, form + " lacks proxy bone " + proxy);
            check(model.quadCount() < QUAD_BUDGET, form + " exceeds the quad budget: " + model.quadCount());
            for (GeoModel.Bone bone : model.bones) {
                for (GeoModel.Cube cube : bone.cubes) {
                    for (GeoModel.Quad quad : cube.quads) {
                        if (quad == null) continue;
                        for (float uv : quad.uvs) check(uv >= 0.0F && uv <= 1.0F, form + " UV outside the texture in " + bone.name);
                        for (float p : quad.positions) check(Float.isFinite(p) && Math.abs(p) < 4.0F, form + " geometry out of bounds in " + bone.name);
                    }
                }
            }
            File texture = assets.resolve("textures/kakuja/" + form + ".png").toFile();
            File glow = assets.resolve("textures/kakuja/" + form + "_glowmask.png").toFile();
            check(texture.isFile() && glow.isFile(), form + " needs its texture and glow mask");
            if (form.equals("war")) {
                for (int k = 1; k <= 4; k++) check(model.bone("blade" + k) >= 0, "the scythe has four bones");
                check(model.bones[model.bone("blade1")].parent == model.bone("right_arm"), "the scythe is fused to the right arm");
                int tendrils = 0;
                for (GeoModel.Bone bone : model.bones) if (bone.name.startsWith("tendril_") && bone.name.endsWith("_1")) tendrils++;
                check(tendrils >= 4 && tendrils <= 6, "4 to 6 back tendrils: " + tendrils);
            }
            new GeoPose(model).matrices();
            checked++;
            System.out.println(form + ": " + model.bones.length + " bones, " + model.quadCount() + " quads");
        }
        check(checked > 0, "no kakuja model found");
        System.out.println("Verified: " + checked + " kakuja model(s) load with proxy bones, articulated parts, valid UVs and within " + QUAD_BUDGET + " quads.");
    }
}
