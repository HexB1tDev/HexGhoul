// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / GeckoLib 4.8.4, bernie-g/geckolib branch 1.20.1). Never packaged; see verify/api-allowlist.txt.
package software.bernie.geckolib.renderer;

/** Forge/src/main/java/software/bernie/geckolib/renderer/GeoRenderer.java */
public interface GeoRenderer<T extends software.bernie.geckolib.core.animatable.GeoAnimatable> {
    default software.bernie.geckolib.core.object.Color getRenderColor(T animatable, float partialTick, int packedLight) {
        throw new UnsupportedOperationException();
    }

    default void renderCubesOfBone(com.mojang.blaze3d.vertex.PoseStack poseStack, software.bernie.geckolib.cache.object.GeoBone bone,
                                   com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green,
                                   float blue, float alpha) {
        throw new UnsupportedOperationException();
    }
}
